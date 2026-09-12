package com.example.vedaahar

import com.example.vedaahar.document.MedicalDocument
import com.example.vedaahar.document.MedicalDocumentStore
import com.example.vedaahar.document.ui.DocumentTypeOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MedicalDocumentTest {

    @Test
    fun testMedicalDocumentModel_creationAndDefaults() {
        val doc = MedicalDocument(
            id = "test-doc-1",
            title = "Blood Test Report",
            fileName = "Blood_Test_Report.pdf",
            fileType = "PDF",
            sizeBytes = 2516582L,
            formattedSize = "2.4 MB",
            uploadTimestamp = 1788854400000L,
            formattedDate = "08 Sep 2026",
            subTitle = "Complete Blood Count",
            documentType = "Lab Report",
            documentDate = "08 Sep 2026",
            doctorOrHospital = "Apollo Diagnostics",
            notes = "Routine CBC"
        )

        assertEquals("test-doc-1", doc.id)
        assertEquals("Blood Test Report", doc.title)
        assertEquals("Complete Blood Count", doc.subTitle)
        assertEquals("Lab Report", doc.documentType)
        assertEquals("PDF", doc.fileType)
        assertEquals(2516582L, doc.sizeBytes)
        assertEquals("2.4 MB", doc.formattedSize)
        assertEquals("08 Sep 2026", doc.documentDate)
        assertEquals("Apollo Diagnostics", doc.doctorOrHospital)
        assertEquals(1, doc.pageCount)
        assertTrue(doc.sharedWith.isEmpty())
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("500 B", MedicalDocumentStore.formatFileSize(500L))
        assertEquals("1023 B", MedicalDocumentStore.formatFileSize(1023L))
        assertEquals("1 KB", MedicalDocumentStore.formatFileSize(1024L))
        assertEquals("50 KB", MedicalDocumentStore.formatFileSize(51200L))
        assertEquals("1.0 MB", MedicalDocumentStore.formatFileSize(1048576L))
        assertEquals("2.4 MB", MedicalDocumentStore.formatFileSize(2516582L))
        assertEquals("10.0 MB", MedicalDocumentStore.formatFileSize(10485760L))
    }

    @Test
    fun testDocumentTypeOptions() {
        val expectedOptions = listOf(
            "Lab Report",
            "Prescription",
            "Doctor's Report",
            "X-Ray / Scan",
            "Discharge Summary",
            "Medical Certificate",
            "Vaccination Record",
            "Other"
        )

        assertEquals(expectedOptions.size, DocumentTypeOptions.size)
        expectedOptions.forEach { expected ->
            assertTrue("Options should contain $expected", DocumentTypeOptions.contains(expected))
        }
    }

    @Test
    fun testFileValidationRules() {
        val maxAllowedSize = 10L * 1024 * 1024 // 10 MB

        val validSize = 2516582L // 2.4 MB
        val edgeSize = 10L * 1024 * 1024 // Exactly 10 MB
        val overflowSize = 10L * 1024 * 1024 + 1 // > 10 MB

        assertFalse("2.4 MB should not be too large", validSize > maxAllowedSize)
        assertFalse("10 MB exact should not exceed limit", edgeSize > maxAllowedSize)
        assertTrue(">10 MB must trigger too large", overflowSize > maxAllowedSize)

        val allowedExtensions = setOf("pdf", "jpg", "jpeg", "png")
        assertTrue("PDF is supported", "pdf" in allowedExtensions)
        assertTrue("JPG is supported", "jpg" in allowedExtensions)
        assertTrue("JPEG is supported", "jpeg" in allowedExtensions)
        assertTrue("PNG is supported", "png" in allowedExtensions)
        assertFalse("DOCX is not supported", "docx" in allowedExtensions)
        assertFalse("ZIP is not supported", "zip" in allowedExtensions)
        assertFalse("EXE is not supported", "exe" in allowedExtensions)
    }

    @Test
    fun testDocumentSearchAndFilteringLogic() {
        val doc1 = MedicalDocument(
            id = "doc-1",
            title = "Blood Test Report",
            subTitle = "Complete Blood Count",
            fileName = "Blood_Test_Report.pdf",
            fileType = "PDF",
            sizeBytes = 2516582L,
            formattedSize = "2.4 MB",
            uploadTimestamp = 1788854400000L,
            formattedDate = "08 Sep 2026",
            documentType = "Lab Report",
            doctorOrHospital = "Apollo Diagnostics",
            notes = "Routine panel"
        )

        val doc2 = MedicalDocument(
            id = "doc-2",
            title = "Prescription",
            subTitle = "General Consultation",
            fileName = "Prescription.pdf",
            fileType = "PDF",
            sizeBytes = 1258291L,
            formattedSize = "1.2 MB",
            uploadTimestamp = 1788595200000L,
            formattedDate = "05 Sep 2026",
            documentType = "Prescription",
            doctorOrHospital = "Dr. Arvind Vaidya",
            notes = "Triphala and Ashwagandha"
        )

        val doc3 = MedicalDocument(
            id = "doc-3",
            title = "X-Ray Report",
            subTitle = "Chest X-Ray",
            fileName = "Chest_XRay.jpg",
            fileType = "JPG",
            sizeBytes = 3250585L,
            formattedSize = "3.1 MB",
            uploadTimestamp = 1787904000000L,
            formattedDate = "28 Aug 2026",
            documentType = "X-Ray / Scan",
            doctorOrHospital = "Metro Imaging Center",
            notes = "Clear lungs"
        )

        val docList = listOf(doc1, doc2, doc3)

        // Search test: by title
        val bloodSearch = docList.filter { it.title.contains("Blood", ignoreCase = true) }
        assertEquals(1, bloodSearch.size)
        assertEquals("Blood Test Report", bloodSearch.first().title)

        // Search test: by doctor
        val doctorSearch = docList.filter { it.doctorOrHospital.contains("Arvind", ignoreCase = true) }
        assertEquals(1, doctorSearch.size)
        assertEquals("Prescription", doctorSearch.first().title)

        // Filter test: Lab Reports
        val labReports = docList.filter { it.documentType.equals("Lab Report", ignoreCase = true) }
        assertEquals(1, labReports.size)
        assertEquals("Blood Test Report", labReports.first().title)

        // Filter test: Prescriptions
        val prescriptions = docList.filter { it.documentType.equals("Prescription", ignoreCase = true) }
        assertEquals(1, prescriptions.size)
        assertEquals("Prescription", prescriptions.first().title)

        // Filter test: X-Ray / Scan
        val scans = docList.filter { it.documentType.contains("X-Ray", ignoreCase = true) || it.documentType.contains("Scan", ignoreCase = true) }
        assertEquals(1, scans.size)
        assertEquals("X-Ray Report", scans.first().title)

        // Sorting test: Newest First
        val newestFirst = docList.sortedByDescending { it.uploadTimestamp }
        assertEquals("Blood Test Report", newestFirst[0].title)
        assertEquals("Prescription", newestFirst[1].title)
        assertEquals("X-Ray Report", newestFirst[2].title)

        // Sorting test: Oldest First
        val oldestFirst = docList.sortedBy { it.uploadTimestamp }
        assertEquals("X-Ray Report", oldestFirst[0].title)
        assertEquals("Prescription", oldestFirst[1].title)
        assertEquals("Blood Test Report", oldestFirst[2].title)
    }

    @Test
    fun testShareAndRenameSimulation() {
        val original = MedicalDocument(
            id = "doc-1",
            title = "Blood Test Report",
            fileName = "Blood_Test_Report.pdf",
            fileType = "PDF",
            sizeBytes = 2516582L,
            formattedSize = "2.4 MB",
            uploadTimestamp = 1788854400000L,
            formattedDate = "08 Sep 2026",
            documentType = "Lab Report"
        )

        // Renaming
        val renamed = original.copy(title = "Annual CBC Report 2026")
        assertEquals("Annual CBC Report 2026", renamed.title)
        assertEquals(original.id, renamed.id)

        // Sharing
        val sharedRecord = "Dr. Arvind Vaidya (Consultation, expires: 7 Days)"
        val sharedDoc = renamed.copy(sharedWith = listOf(sharedRecord))
        assertEquals(1, sharedDoc.sharedWith.size)
        assertTrue(sharedDoc.sharedWith.first().contains("Dr. Arvind Vaidya"))
    }
}
