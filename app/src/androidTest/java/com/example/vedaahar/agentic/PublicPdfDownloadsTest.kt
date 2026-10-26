package com.example.vedaahar.agentic

import android.content.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.UUID

fun publicFixturePdf(): ByteArray {
    val document = PdfDocument()
    val page = document.startPage(PdfDocument.PageInfo.Builder(400, 500, 1).create())
    page.canvas.drawText("VedAmrit synthetic public export test - no patient data", 15f, 30f, android.graphics.Paint())
    document.finishPage(page)
    return ByteArrayOutputStream().use { output -> document.writeTo(output); document.close(); output.toByteArray() }
}

@RunWith(AndroidJUnit4::class)
class PublicPdfDownloadsTest {
    private val real = InstrumentationRegistry.getInstrumentation().targetContext
    private val namespace = "public_pdf_test_${UUID.randomUUID()}"
    private val owner = "$namespace|1"
    private var current: String? = owner
    private val prefs = mutableSetOf<String>()
    private val uris = mutableListOf<android.net.Uri>()
    private var launched: Intent? = null
    private var noViewer = false
    private val context = object : ContextWrapper(real) {
        override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
            val testName = namespace + name; prefs += testName
            return real.getSharedPreferences(testName, mode)
        }
        override fun startActivity(intent: Intent) {
            if (noViewer) throw ActivityNotFoundException("Fixture: no viewer")
            launched = intent
        }
    }
    private val record = DownloadedDraft(9000000000L + (System.nanoTime() and 0xffffff), 1, 7, "a".repeat(64), "b".repeat(64), null)
    private fun downloads() = PublicPdfDownloads(context, owner) { current }

    @After fun cleanup() {
        uris.forEach { real.contentResolver.delete(it, null, null) } // Only this test's freshly returned URIs.
        prefs.forEach { real.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit(); real.deleteSharedPreferences(it) }
    }

    @Test fun publishedPdfHasPublicPathMimeBytesAndSurvivesStoreRecreation() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 29)
        val data = publicFixturePdf()
        val states = mutableListOf<Int>()
        val collection = MediaStore.setIncludePending(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY))
        val saved = downloads().saveToDownloads(record, data) {
            real.contentResolver.query(collection, arrayOf(MediaStore.Downloads.IS_PENDING),
                "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?", arrayOf("VedAmrit_Diet_Plan_${record.planId}_%"), null)?.use {
                while (it.moveToNext()) states += it.getInt(0)
            }
        }
        val uri = android.net.Uri.parse(saved.uri); uris += uri
        assertTrue(states.contains(1))
        assertEquals(0, states.last())
        assertEquals("application/pdf", real.contentResolver.getType(uri))
        real.contentResolver.query(uri, arrayOf(MediaStore.Downloads.RELATIVE_PATH, MediaStore.Downloads.IS_PENDING), null, null, null)!!.use {
            assertTrue(it.moveToFirst())
            assertEquals(Environment.DIRECTORY_DOWNLOADS + "/VedAmrit/", it.getString(0))
            assertEquals(0, it.getInt(1))
        }
        assertArrayEquals(data, real.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
        PdfRenderer(real.contentResolver.openFileDescriptor(uri, "r")!!).use { assertEquals(1, it.pageCount) }
        val recreated = downloads()
        assertEquals(saved, recreated.latest())
        recreated.open(saved) {}
        assertEquals(uri, launched!!.data)
        assertEquals(Intent.ACTION_VIEW, launched!!.action)
        assertTrue(launched!!.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        recreated.share(saved) {}
        val send = launched!!.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("application/pdf", send.type)
        assertEquals(uri, send.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM))
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test fun repeatExportDoesNotOverwriteExistingFileAndAccountSwitchDoesNotCreateAnother() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 29)
        val first = downloads().saveToDownloads(record, publicFixturePdf()) {}
        uris += android.net.Uri.parse(first.uri)
        val second = downloads().saveToDownloads(record, publicFixturePdf()) {}
        uris += android.net.Uri.parse(second.uri)
        assertNotEquals(first.uri, second.uri)
        assertNotEquals(first.displayName, second.displayName)
        assertTrue(real.contentResolver.openInputStream(uris[0])!!.use { it.read() } >= 0)
        current = "$namespace|2"
        assertNull(PublicPdfDownloads(context, current!!) { current }.latest())
        try { downloads().saveToDownloads(record, publicFixturePdf()) {}; fail("Cross-account export allowed") } catch (_: IllegalStateException) {}
        try { downloads().open(first) {}; fail("Cross-account in-app open allowed") } catch (_: IllegalStateException) {}
    }

    @Test fun noViewerKeepsPublicFileAndOlderRouteBuildsSaveAsIntent() {
        val intent = createPdfDocumentIntent(newPdfExportName(record))
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, intent.action)
        assertEquals("application/pdf", intent.type)
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
        assertEquals(PublicPdfRoute.CreateDocument, publicPdfRoute(28))
        if (Build.VERSION.SDK_INT >= 29) {
            val saved = downloads().saveToDownloads(record, publicFixturePdf()) {}
            uris += android.net.Uri.parse(saved.uri)
            noViewer = true
            assertTrue(downloads().open(saved) {}.contains("No PDF viewer"))
            assertTrue(real.contentResolver.openInputStream(uris.last())!!.use { it.read() } >= 0)
        }
    }
}

/** Two-phase synthetic fixture for checking Files/Downloads after the app process exits. */
@RunWith(AndroidJUnit4::class)
class PublicPdfLifecycleCreateTest {
    @Test fun createPublicFixture() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 29)
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("publicPdfLifecycle") == "create")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(!context.getSharedPreferences("public_pdf_lifecycle_fixture", Context.MODE_PRIVATE).contains("uri")) {
            "Clean the previous lifecycle fixture before creating another."
        }
        val record = DownloadedDraft(9000000001, 1, 7, "a".repeat(64), "b".repeat(64), null)
        val owner = "public-pdf-lifecycle-fixture|1"
        val saved = PublicPdfDownloads(context, owner) { owner }.saveToDownloads(record, publicFixturePdf()) {}
        context.getSharedPreferences("public_pdf_lifecycle_fixture", Context.MODE_PRIVATE).edit()
            .putString("uri", saved.uri).putString("name", saved.displayName).commit()
        val results = android.os.Bundle().apply { putString("fixture_uri", saved.uri); putString("fixture_name", saved.displayName) }
        InstrumentationRegistry.getInstrumentation().sendStatus(0, results)
    }
}

@RunWith(AndroidJUnit4::class)
class PublicPdfLifecycleVerifyTest {
    @Test fun verifyAndCleanOnlyCreatedFixture() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 29)
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("publicPdfLifecycle") == "verify")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("public_pdf_lifecycle_fixture", Context.MODE_PRIVATE)
        val uri = android.net.Uri.parse(prefs.getString("uri", null) ?: error("Create the lifecycle fixture first."))
        try {
            assertEquals("application/pdf", context.contentResolver.getType(uri))
            val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            assertTrue(DraftPdfRepository.isPdf(bytes))
            PdfRenderer(context.contentResolver.openFileDescriptor(uri, "r")!!).use { assertEquals(1, it.pageCount) }
        } finally {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)!!.use {
                check(it.moveToFirst() && it.getString(0) == prefs.getString("name", null))
                check(it.getString(0).startsWith("VedAmrit_Diet_Plan_9000000001_"))
            }
            context.contentResolver.delete(uri, null, null)
            val owner = "public-pdf-lifecycle-fixture|1"
            com.example.vedaahar.network.PatientSession.preferences(context, "public_draft_pdf", owner)!!.edit().clear().commit()
            prefs.edit().clear().commit()
        }
    }
}

/** Explicit opt-in interaction with only the synthetic fixture in the public folder. */
@RunWith(AndroidJUnit4::class)
class PublicPdfFilesUiTest {
    @Test fun filesAppListsFixtureAndOpensItInPdfViewer() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("publicPdfLifecycle") == "files")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        Assume.assumeFalse("Unlock the phone before the Files/PDF viewer UI check.", keyguard.isKeyguardLocked)
        val name = context.getSharedPreferences("public_pdf_lifecycle_fixture", Context.MODE_PRIVATE).getString("name", null)
            ?: error("Create the lifecycle fixture first.")
        val folder = android.provider.DocumentsContract.buildDocumentUri("com.android.externalstorage.documents",
            "primary:${Environment.DIRECTORY_DOWNLOADS}/VedAmrit")
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(folder, "vnd.android.document/directory")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val automation = instrumentation.uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        fun find(text: String): android.view.accessibility.AccessibilityNodeInfo? {
            repeat(30) {
                automation.rootInActiveWindow?.findAccessibilityNodeInfosByText(text)?.firstOrNull()?.let { return it }
                Thread.sleep(200)
            }
            return null
        }
        fun click(node: android.view.accessibility.AccessibilityNodeInfo) {
            var target = node
            while (!target.isClickable && target.parent != null) target = target.parent
            check(target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
        }
        val file = find(name) ?: error("The synthetic exported PDF is not visible in Files.")
        click(file)
        Thread.sleep(800)
        if (automation.rootInActiveWindow?.packageName?.toString() == "android") {
            val labels = mutableListOf<String>()
            fun collect(node: android.view.accessibility.AccessibilityNodeInfo) {
                node.text?.toString()?.takeIf { it.isNotBlank() && !it.contains(name) }?.let { labels += it }
                for (index in 0 until node.childCount) node.getChild(index)?.let { collect(it) }
            }
            automation.rootInActiveWindow?.let { collect(it) }
            val label = labels.firstOrNull { it.contains("Drive") || it.contains("PDF", ignoreCase = true) }
                ?: error("PDF chooser needs manual selection. App labels: $labels")
            click(find(label) ?: error("PDF viewer option disappeared."))
            automation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Just once")?.firstOrNull()?.let { click(it) }
        }
        fun viewer(): android.view.accessibility.AccessibilityNodeInfo? {
            repeat(30) {
                val root = automation.rootInActiveWindow
                val hasTitle = root?.findAccessibilityNodeInfosByText(name)?.isNotEmpty() == true
                val hasWpsCanvas = root?.findAccessibilityNodeInfosByViewId("cn.wps.moffice_eng:id/pdf_renderview")?.isNotEmpty() == true
                if (root != null && (hasTitle || hasWpsCanvas) && root.packageName.toString() !in
                    listOf("com.google.android.documentsui", "com.vedamrit", "android")) return root
                Thread.sleep(200)
            }
            return null
        }
        val viewerRoot = viewer() ?: error("External PDF viewer did not display the exported document.")
        Thread.sleep(700)
        automation.takeScreenshot()?.let { bitmap ->
            java.io.File(context.cacheDir, "public_pdf_ui_fixture.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
        instrumentation.sendStatus(0, android.os.Bundle().apply {
            putString("files_visible", "true"); putString("pdf_viewer_package", viewerRoot.packageName.toString())
        })
    }
}


