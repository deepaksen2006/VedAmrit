package com.example.vedaahar.agentic

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import org.junit.Assert.*
import org.junit.Test

class PdfExportTransactionTest {
    private val pdf = "%PDF-1.4\nsynthetic fixture\n%%EOF".toByteArray()
    private val record = DownloadedDraft(42, 1, 7, "a".repeat(64), "b".repeat(64), null)
    private fun name() = newPdfExportName(record)

    private class Destination : PendingPdfDestination {
        val data = ByteArrayOutputStream()
        var created = 0
        var published = false
        var deleted = false
        var closed = false
        var failWrite = false
        var failClose = false
        var failPublish = false
        var writeAction: () -> Unit = {}
        var publishAction: () -> Unit = {}
        override fun create(displayName: String): String { created++; return "new-entry" }
        override fun output(id: String): OutputStream = object : OutputStream() {
            override fun write(value: Int) { data.write(value) }
            override fun write(bytes: ByteArray, offset: Int, count: Int) {
                data.write(bytes, offset, count); writeAction()
                if (failWrite) throw IOException("disk full")
            }
            override fun close() { closed = true; if (failClose) throw IOException("close failed") }
        }
        override fun publish(id: String) {
            check(closed)
            if (failPublish) throw IOException("publish failed")
            published = true; publishAction()
        }
        override fun delete(id: String) { assertEquals("new-entry", id); deleted = true; published = false }
    }

    @Test fun completePdfIsClosedBeforeItBecomesVisible() {
        val destination = Destination()
        assertEquals("new-entry", publishVerifiedPdf(pdf, name(), destination) {})
        assertArrayEquals(pdf, destination.data.toByteArray())
        assertTrue(destination.closed)
        assertTrue(destination.published)
        assertFalse(destination.deleted)
    }

    @Test fun writeCloseAndPublishFailuresDeleteOnlyNewEntry() {
        for (mode in 0..2) {
            val destination = Destination().apply { failWrite = mode == 0; failClose = mode == 1; failPublish = mode == 2 }
            try { publishVerifiedPdf(pdf, name(), destination) {}; fail("Failure not propagated") } catch (_: IOException) {}
            assertTrue(destination.deleted)
            assertFalse(destination.published)
        }
    }

    @Test fun missingPatientAndInvalidBytesCannotCreatePublicFiles() {
        val destination = Destination()
        try { publishVerifiedPdf(pdf, name(), destination) { error("patient changed") }; fail() } catch (_: IllegalStateException) {}
        try { publishVerifiedPdf("{}".toByteArray(), name(), destination) {}; fail() } catch (_: IllegalArgumentException) {}
        assertEquals(0, destination.created)
    }

    @Test fun accountSwitchDuringWriteOrPublicationRemovesExport() {
        for (duringWrite in listOf(true, false)) {
            var active = true
            val destination = Destination().apply {
                if (duringWrite) writeAction = { active = false } else publishAction = { active = false }
            }
            try { publishVerifiedPdf(pdf, name(), destination) { check(active) }; fail() } catch (_: IllegalStateException) {}
            assertTrue(destination.deleted)
            assertFalse(destination.published)
        }
    }

    @Test fun repeatsGetUniqueNonSensitiveNames() {
        val first = name()
        val second = name()
        assertNotEquals(first, second)
        assertTrue(first.startsWith("VedAmrit_Diet_Plan_42_v1_"))
        assertTrue(first.endsWith(".pdf"))
        assertFalse(first.contains("http"))
        assertFalse(first.contains(record.profileVersion))
    }

    @Test fun olderAndroidUsesDocumentPickerWithoutStoragePermission() {
        assertEquals(PublicPdfRoute.CreateDocument, publicPdfRoute(24))
        assertEquals(PublicPdfRoute.CreateDocument, publicPdfRoute(28))
        assertEquals(PublicPdfRoute.MediaStoreDownloads, publicPdfRoute(29))
        assertEquals(PublicPdfRoute.MediaStoreDownloads, publicPdfRoute(35))
    }
}

