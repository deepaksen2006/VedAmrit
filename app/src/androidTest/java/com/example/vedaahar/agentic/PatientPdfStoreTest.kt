package com.example.vedaahar.agentic

import android.content.*
import android.content.pm.PackageManager
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vedaahar.network.dto.WellnessPlanResponse
import com.google.gson.Gson
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PatientPdfStoreTest {
    private val real = InstrumentationRegistry.getInstrumentation().targetContext
    private val namespace = "pdf_test_${UUID.randomUUID()}"
    private val ownerA = "$namespace|1"
    private val ownerB = "$namespace|2"
    private var current: String? = ownerA
    private var launched: Intent? = null
    private var noViewer = false
    private var inputsCurrent = true
    private val preferences = mutableSetOf<String>()
    private val privateRoot = File(real.noBackupFilesDir, namespace)
    private val context = object : ContextWrapper(real) {
        override fun getApplicationContext(): Context = this
        override fun getNoBackupFilesDir() = privateRoot
        override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
            val testName = namespace + name
            preferences += testName
            return real.getSharedPreferences(testName, mode)
        }
        override fun startActivity(intent: Intent) {
            if (noViewer) throw ActivityNotFoundException("Fixture: no viewer")
            launched = intent
        }
    }
    private fun store(owner: String = ownerA) = PatientPdfStore(context, owner, { current }, { inputsCurrent })
    private fun plan() = Gson().fromJson("""{"id":42,"version":1,"assessment_id":7,"assessment_version":"${"a".repeat(64)}","profile_version":"${"b".repeat(64)}","inputs_current":true,"draft_pdf_available":true,"doctor_status":"PENDING","created_at":"2026-10-26"}""",
        WellnessPlanResponse::class.java)
    private fun bytes(): ByteArray {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(300, 400, 1).create())
        page.canvas.drawText("Synthetic PDF storage test", 20f, 30f, android.graphics.Paint())
        document.finishPage(page)
        return ByteArrayOutputStream().use { output -> document.writeTo(output); document.close(); output.toByteArray() }
    }

    @After fun cleanup() {
        preferences.forEach { real.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit(); real.deleteSharedPreferences(it) }
        privateRoot.deleteRecursively()
        for (owner in listOf(ownerA, ownerB)) File(real.cacheDir, "patient_draft_reports/${reportOwnerHash(owner)}").deleteRecursively()
    }

    @Test fun savesAndReopensThroughSafeContentUriAfterStoreRecreation() {
        val record = store().save(plan(), bytes())
        val reopened = store()
        assertEquals(record, reopened.latest())
        val uri = reopened.contentUri(record)
        assertEquals("content", uri.scheme)
        val info = real.packageManager.resolveContentProvider("${real.packageName}.fileprovider", PackageManager.GET_META_DATA)!!
        val provider = object : PatientPdfProvider() { override fun patientOwner() = current }
        provider.attachInfo(real, info)
        PdfRenderer(provider.openFile(uri, "r")!!).use { assertEquals(1, it.pageCount) }
        reopened.open(record)
        assertEquals(Intent.ACTION_VIEW, launched!!.action)
        assertEquals("application/pdf", launched!!.type)
        assertEquals(uri, launched!!.data)
        assertTrue(launched!!.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertTrue(launched!!.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION == 0)
    }

    @Test fun accountSwitchBlocksSavedReportAndPreviouslyGrantedUri() {
        val record = store().save(plan(), bytes())
        val uri = store().contentUri(record)
        current = ownerB
        assertNull(store(ownerB).latest())
        try { store().contentUri(record); fail("Cross-account open allowed") } catch (_: IllegalStateException) {}
        val info = real.packageManager.resolveContentProvider("${real.packageName}.fileprovider", PackageManager.GET_META_DATA)!!
        val provider = object : PatientPdfProvider() { override fun patientOwner() = current }
        provider.attachInfo(real, info)
        try { provider.openFile(uri, "r"); fail("Cross-account provider access allowed") } catch (_: FileNotFoundException) {}
        current = ownerA
        assertEquals(record, store().latest())
    }

    @Test fun providerRejectsTraversalAndWriteAccess() {
        val record = store().save(plan(), bytes())
        val uri = store().contentUri(record)
        val info = real.packageManager.resolveContentProvider("${real.packageName}.fileprovider", PackageManager.GET_META_DATA)!!
        val provider = object : PatientPdfProvider() { override fun patientOwner() = current }
        provider.attachInfo(real, info)
        try { provider.openFile(uri, "w"); fail("Write allowed") } catch (_: FileNotFoundException) {}
        val traversed = uri.buildUpon().encodedPath("/patient_draft_reports/${reportOwnerHash(ownerA)}/../${reportOwnerHash(ownerB)}/${uri.lastPathSegment}").build()
        try { provider.openFile(traversed, "r"); fail("Traversal allowed") } catch (_: FileNotFoundException) {}
        current = ownerB
        try { provider.query(uri, null, null, null, null); fail("Foreign metadata allowed") } catch (_: FileNotFoundException) {}
    }

    @Test fun missingViewerIsActionableAndKeepsSavedFile() {
        val record = store().save(plan(), bytes())
        noViewer = true
        assertTrue(store().open(record).contains("No PDF viewer"))
        assertEquals(record, store().latest())
    }

    @Test fun changedLocalInputsBlockReopeningDownloadedGuidance() {
        val record = store().save(plan(), bytes())
        inputsCurrent = false
        try { store().contentUri(record); fail("Stale report opened") } catch (_: IllegalStateException) {}
        inputsCurrent = true
        assertEquals(record, store().latest())
    }

    @Test fun lateDownloadAndInvalidResponseAreNotSaved() {
        current = ownerB
        try { store().save(plan(), bytes()); fail("Late save allowed") } catch (_: IllegalStateException) {}
        current = ownerA
        try { store().save(plan(), "{}".toByteArray()); fail("Invalid PDF saved") } catch (_: IllegalArgumentException) {}
        assertNull(store().latest())
    }
}

