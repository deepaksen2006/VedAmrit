package com.example.vedaahar.agentic

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.AtomicFile
import androidx.core.content.FileProvider
import com.example.vedaahar.BuildConfig
import com.example.vedaahar.network.PatientSession
import com.example.vedaahar.network.dto.WellnessPlanResponse
import com.example.vedaahar.assessment.AssessmentSyncStore
import com.example.vedaahar.profile.PatientProfileStore
import com.google.gson.Gson
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest

data class DownloadedDraft(
    val planId: Long, val planVersion: Int, val assessmentId: Long,
    val assessmentVersion: String, val profileVersion: String, val generatedAt: String?
)

fun reportOwnerHash(owner: String): String = MessageDigest.getInstance("SHA-256")
    .digest(owner.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

class PatientPdfStore(
    private val context: Context,
    private val owner: String,
    private val currentOwner: () -> String? = { PatientSession.currentKey(context) },
    private val matchesLocalInputs: (DownloadedDraft) -> Boolean = { record ->
        val assessment = AssessmentSyncStore(context).read()
        record.assessmentId == assessment?.serverId && record.assessmentVersion == assessment?.serverVersion &&
            assessment?.ready == true && !assessment.localChanges &&
            record.profileVersion == PatientProfileStore.current(context).profileVersion
    }
) {
    private val gson = Gson()
    private fun requireOwner() {
        check(BuildConfig.AGENTIC_DRAFT_PREVIEW && currentOwner() == owner) {
            "Sign in to the patient account that downloaded this draft."
        }
    }
    private fun filename(record: DownloadedDraft): String {
        require(record.planId > 0 && record.planVersion > 0 && record.assessmentId > 0 &&
            record.assessmentVersion.matches(Regex("[0-9a-f]{64}")) &&
            record.profileVersion.matches(Regex("[0-9a-f]{64}"))) { "Invalid saved report reference." }
        val version = reportOwnerHash("${record.assessmentId}:${record.assessmentVersion}:${record.profileVersion}").take(16)
        return "draft-${record.planId}-v${record.planVersion}-$version.pdf"
    }
    private fun file(record: DownloadedDraft) = File(
        File(context.noBackupFilesDir, "patient_draft_reports/${reportOwnerHash(owner)}"), filename(record))

    fun latest(): DownloadedDraft? {
        requireOwner()
        return runCatching {
            val raw = PatientSession.preferences(context, "downloaded_draft_pdf", owner)?.getString("latest", null)
                ?: return null
            val record = gson.fromJson(raw, DownloadedDraft::class.java)
            record.takeIf { file(it).isFile }
        }.getOrNull()
    }

    fun save(plan: WellnessPlanResponse, data: ByteArray): DownloadedDraft {
        requireOwner()
        require(plan.draftPdfAvailable && plan.inputsCurrent && plan.doctorStatus == "PENDING") {
            "This plan is not eligible for draft PDF export. Refresh its validation status."
        }
        require(data.size <= DraftPdfRepository.MAX_BYTES && DraftPdfRepository.isPdf(data)) { "Invalid PDF response." }
        val record = DownloadedDraft(plan.id, plan.version, requireNotNull(plan.assessmentId),
            requireNotNull(plan.assessmentVersion), requireNotNull(plan.profileVersion), plan.createdAt)
        check(matchesLocalInputs(record)) { "Profile or assessments changed during download. Sync and download a current plan." }
        val target = file(record)
        target.parentFile!!.mkdirs()
        val atomic = AtomicFile(target)
        val stream = atomic.startWrite()
        try {
            stream.write(data)
            requireOwner() // A late response never becomes another patient's saved report.
            check(matchesLocalInputs(record)) { "Profile or assessments changed during download. Refresh the plan." }
            atomic.finishWrite(stream)
        } catch (error: Exception) { atomic.failWrite(stream); throw error }
        check(PatientSession.preferences(context, "downloaded_draft_pdf", owner)!!.edit()
            .putString("latest", gson.toJson(record)).commit()) { "Could not save the report reference. Check device storage." }
        return record
    }

    fun contentUri(record: DownloadedDraft): Uri {
        requireOwner()
        check(matchesLocalInputs(record)) { "Saved PDF uses older local profile or assessment versions. Sync and download a current plan." }
        val source = file(record)
        check(source.isFile) { "Saved PDF was not found. Download it again." }
        val target = File(File(context.cacheDir, "patient_draft_reports/${reportOwnerHash(owner)}"), filename(record))
        target.parentFile!!.mkdirs()
        val atomic = AtomicFile(target)
        val stream = atomic.startWrite()
        try {
            source.inputStream().use { it.copyTo(stream) }
            requireOwner()
            atomic.finishWrite(stream)
        } catch (error: Exception) { atomic.failWrite(stream); throw error }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
    }

    fun assertCurrent(record: DownloadedDraft) {
        requireOwner()
        check(matchesLocalInputs(record)) { "Profile or assessment changed. Sync and download a current plan before exporting." }
    }

    fun readPdf(record: DownloadedDraft): ByteArray {
        assertCurrent(record)
        val source = file(record)
        check(source.isFile && source.length() <= DraftPdfRepository.MAX_BYTES) { "Private PDF was not found or is too large. Download it again." }
        val data = source.readBytes()
        check(DraftPdfRepository.isPdf(data)) { "Private PDF is invalid. Download it again." }
        assertCurrent(record)
        return data
    }

    fun open(record: DownloadedDraft): String {
        val uri = contentUri(record)
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.clipData = ClipData.newRawUri("Personalized AI draft PDF", uri)
        return try {
            requireOwner()
            context.startActivity(intent)
            "Opened downloaded draft for plan #${record.planId}."
        } catch (_: ActivityNotFoundException) {
            "PDF saved privately. No PDF viewer is installed; install a trusted viewer and tap Open Downloaded PDF."
        }
    }
}

/** Keep existing camera URI behavior; add an account check for every PDF file open. */
open class PatientPdfProvider : FileProvider() {
    protected open fun patientOwner(): String? = context?.let { PatientSession.currentKey(it) }

    private fun checkAccess(uri: Uri, mode: String = "r") {
        if (uri.pathSegments.firstOrNull() == "patient_draft_reports") {
            val owner = patientOwner()
            if (!BuildConfig.AGENTIC_DRAFT_PREVIEW || owner == null || mode != "r" ||
                uri.pathSegments.size != 3 || uri.pathSegments.getOrNull(1) != reportOwnerHash(owner) ||
                !uri.lastPathSegment.orEmpty().matches(Regex("draft-[1-9][0-9]*-v[1-9][0-9]*-[0-9a-f]{16}\\.pdf"))) {
                throw FileNotFoundException("This report belongs to a different patient session.")
            }
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): android.database.Cursor {
        checkAccess(uri)
        return super.query(uri, projection, selection, selectionArgs, sortOrder)
    }

    override fun getType(uri: Uri): String? {
        checkAccess(uri)
        return super.getType(uri)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        checkAccess(uri, mode)
        return super.openFile(uri, mode)
    }
}

