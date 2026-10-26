package com.example.vedaahar.agentic

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.example.vedaahar.network.PatientSession
import com.google.gson.Gson
import java.io.IOException
import java.util.UUID

data class PublicPdfExport(val record: DownloadedDraft, val uri: String, val displayName: String, val inDownloads: Boolean)
data class PendingPdfPicker(val ownerHash: String, val record: DownloadedDraft, val displayName: String)

fun newPdfExportName(record: DownloadedDraft): String {
    require(record.planId > 0 && record.planVersion > 0)
    return "VedAmrit_Diet_Plan_${record.planId}_v${record.planVersion}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().replace("-", "").take(8)}.pdf"
}

fun createPdfDocumentIntent(displayName: String): Intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
    .addCategory(Intent.CATEGORY_OPENABLE).setType("application/pdf").putExtra(Intent.EXTRA_TITLE, displayName)
    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)

/** Public files outlive app/account sessions. Only the in-app references are patient-scoped. */
class PublicPdfDownloads(private val context: Context, private val owner: String,
                         private val currentOwner: () -> String? = { PatientSession.currentKey(context) }) {
    private val resolver get() = context.contentResolver
    private val gson = Gson()
    private fun requireOwner() = check(currentOwner() == owner) { "Patient changed. No report was exported for the new account." }

    fun latest(): PublicPdfExport? {
        requireOwner()
        return runCatching {
            val raw = PatientSession.preferences(context, "public_draft_pdf", owner)?.getString("latest", null) ?: return null
            gson.fromJson(raw, PublicPdfExport::class.java).takeIf { Uri.parse(it.uri).scheme == "content" }
        }.getOrNull()
    }

    fun saveToDownloads(record: DownloadedDraft, data: ByteArray, authorize: () -> Unit): PublicPdfExport {
        check(Build.VERSION.SDK_INT >= 29) { "Use the system Save As picker on this Android version." }
        val destination = object : PendingPdfDestination {
            override fun create(displayName: String): String {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/VedAmrit")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                return (resolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
                    ?: throw IOException("Could not create the Downloads entry.")).toString()
            }
            override fun output(id: String) = resolver.openOutputStream(Uri.parse(id), "w")
                ?: throw IOException("Could not open the public PDF for writing.")
            override fun publish(id: String) {
                val values = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                check(resolver.update(Uri.parse(id), values, null, null) == 1) { "Could not finish publishing the public PDF." }
            }
            override fun delete(id: String) {
                check(resolver.delete(Uri.parse(id), null, null) == 1) { "Could not remove the incomplete PDF." }
            }
        }
        return save(record, data, newPdfExportName(record), destination, true, authorize)
    }

    fun saveToDocument(record: DownloadedDraft, data: ByteArray, name: String, uri: Uri, authorize: () -> Unit): PublicPdfExport {
        require(uri.scheme == "content" && DocumentsContract.isDocumentUri(context, uri)) { "The system picker did not return a document URI." }
        val destination = object : PendingPdfDestination {
            override fun create(displayName: String) = uri.toString()
            override fun output(id: String) = resolver.openOutputStream(uri, "w")
                ?: throw IOException("Could not open the selected document.")
            override fun publish(id: String) {}
            override fun delete(id: String) { check(DocumentsContract.deleteDocument(resolver, uri)) { "Could not remove the incomplete document." } }
        }
        try {
            val exported = save(record, data, name, destination, false, authorize)
            // Preserve read access for reopening; a provider may not offer persistence.
            runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            return exported
        } catch (failure: Throwable) {
            // A source/session failure may have occurred before the transaction took ownership of this fresh document.
            runCatching { DocumentsContract.deleteDocument(resolver, uri) }
            throw failure
        }
    }

    private fun save(record: DownloadedDraft, data: ByteArray, name: String, destination: PendingPdfDestination,
                     inDownloads: Boolean, authorize: () -> Unit): PublicPdfExport {
        val guard = { requireOwner(); authorize() }
        val uri = publishVerifiedPdf(data, name, destination, guard)
        try {
            guard()
            val result = PublicPdfExport(record, uri, name, inDownloads)
            check(PatientSession.preferences(context, "public_draft_pdf", owner)!!.edit()
                .putString("latest", gson.toJson(result)).commit()) { "Could not save the export reference." }
            guard()
            return result
        } catch (failure: Throwable) {
            runCatching { destination.delete(uri) }.onFailure { failure.addSuppressed(it) }
            throw failure
        }
    }

    fun open(exported: PublicPdfExport, authorize: () -> Unit): String {
        requireOwner(); authorize()
        val uri = checkedUri(exported)
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.clipData = ClipData.newRawUri("VedAmrit draft PDF", uri)
        return try {
            requireOwner(); authorize(); context.startActivity(intent)
            "Opened PDF."
        } catch (_: ActivityNotFoundException) { "PDF is saved. No PDF viewer is installed; open it from Files after installing a trusted viewer." }
    }

    fun share(exported: PublicPdfExport, authorize: () -> Unit): String {
        requireOwner(); authorize()
        val uri = checkedUri(exported)
        val send = Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri("VedAmrit draft PDF", uri)
        val chooser = Intent.createChooser(send, "Share PDF")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        chooser.clipData = send.clipData
        return try {
            requireOwner(); authorize(); context.startActivity(chooser)
            "Choose an app to share this public PDF."
        } catch (_: ActivityNotFoundException) { "No sharing app is available. The PDF remains saved." }
    }

    private fun checkedUri(exported: PublicPdfExport): Uri {
        val uri = Uri.parse(exported.uri)
        check(uri.scheme == "content") { "Invalid export URI." }
        val valid = resolver.openInputStream(uri)?.use { input ->
            val prefix = ByteArray(5)
            var offset = 0
            while (offset < prefix.size) {
                val count = input.read(prefix, offset, prefix.size - offset)
                if (count < 0) break
                offset += count
            }
            offset == 5 && prefix.contentEquals("%PDF-".toByteArray())
        } == true
        check(valid) { "Public PDF was moved, deleted or changed. Download it again." }
        return uri
    }
}

