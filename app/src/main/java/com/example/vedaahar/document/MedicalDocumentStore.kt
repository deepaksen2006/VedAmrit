package com.example.vedaahar.document

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object MedicalDocumentStore {
    private const val PrefsName = "vedamrit_medical_documents"
    private const val DocumentsKey = "documents_list"

    fun getDocuments(context: Context): List<MedicalDocument> {
        val prefs = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        val raw = prefs.getString(DocumentsKey, null)
        if (raw == null) {
            val initial = defaultSeedDocuments()
            saveDocuments(context, initial)
            return initial
        }
        val jsonArray = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return buildList {
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                add(obj.toMedicalDocument())
            }
        }
    }

    fun addDocument(
        context: Context,
        title: String,
        fileName: String,
        fileType: String,
        sizeBytes: Long,
        pageCount: Int = 1,
        sourceBytes: ByteArray? = null,
        subTitle: String = "",
        documentType: String = "Lab Report",
        documentDate: String? = null,
        doctorOrHospital: String = "",
        notes: String = ""
    ): MedicalDocument {
        val id = UUID.randomUUID().toString()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val defaultFormattedDate = dateFormat.format(Date())
        val finalDate = documentDate?.takeIf { it.isNotBlank() } ?: defaultFormattedDate
        val formattedSize = formatFileSize(sizeBytes)

        var localPath: String? = null
        if (sourceBytes != null) {
            val docDir = File(context.filesDir, "medical_documents").apply { mkdirs() }
            val targetFile = File(docDir, "${id}_$fileName")
            runCatching {
                targetFile.writeBytes(sourceBytes)
                localPath = targetFile.absolutePath
            }
        }

        val newDoc = MedicalDocument(
            id = id,
            title = title,
            fileName = fileName,
            fileType = fileType.uppercase(Locale.ROOT),
            sizeBytes = sizeBytes,
            formattedSize = formattedSize,
            uploadTimestamp = System.currentTimeMillis(),
            formattedDate = finalDate,
            pageCount = pageCount,
            localPath = localPath,
            subTitle = subTitle,
            documentType = documentType,
            documentDate = finalDate,
            doctorOrHospital = doctorOrHospital,
            notes = notes,
            sharedWith = emptyList()
        )

        val currentList = getDocuments(context).toMutableList()
        currentList.add(0, newDoc)
        saveDocuments(context, currentList)
        return newDoc
    }

    fun renameDocument(context: Context, id: String, newTitle: String): Boolean {
        val currentList = getDocuments(context).toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = currentList[index]
            currentList[index] = existing.copy(title = newTitle.trim())
            saveDocuments(context, currentList)
            return true
        }
        return false
    }

    fun shareDocument(
        context: Context,
        id: String,
        recipient: String,
        purpose: String = "",
        duration: String = "7 Days"
    ): Boolean {
        val currentList = getDocuments(context).toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = currentList[index]
            val record = "$recipient ($purpose, expires: $duration)"
            val updatedShared = (existing.sharedWith + record).distinct()
            currentList[index] = existing.copy(sharedWith = updatedShared)
            saveDocuments(context, currentList)
            return true
        }
        return false
    }

    fun deleteDocument(context: Context, id: String) {
        val currentList = getDocuments(context).toMutableList()
        val doc = currentList.find { it.id == id }
        if (doc?.localPath != null) {
            runCatching { File(doc.localPath).delete() }
        }
        currentList.removeAll { it.id == id }
        saveDocuments(context, currentList)
    }

    private fun saveDocuments(context: Context, list: List<MedicalDocument>) {
        val prefs = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        val array = JSONArray()
        list.forEach { doc ->
            array.put(doc.toJson())
        }
        prefs.edit().putString(DocumentsKey, array.toString()).apply()
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format(Locale.US, "%d KB", bytes / 1024)
            else -> "$bytes B"
        }
    }

    private fun defaultSeedDocuments(): List<MedicalDocument> {
        return listOf(
            MedicalDocument(
                id = "seed-blood-test",
                title = "Blood Test Report",
                fileName = "Blood_Test_Report.pdf",
                fileType = "PDF",
                sizeBytes = 2516582L, // 2.4 MB
                formattedSize = "2.4 MB",
                uploadTimestamp = 1788854400000L, // 08 Sep 2026
                formattedDate = "08 Sep 2026",
                pageCount = 2,
                subTitle = "Complete Blood Count",
                documentType = "Lab Report",
                documentDate = "08 Sep 2026",
                doctorOrHospital = "Apollo Diagnostics",
                notes = "Routine full blood panel including CBC and hemoglobin."
            ),
            MedicalDocument(
                id = "seed-prescription",
                title = "Prescription",
                fileName = "General_Consultation_Prescription.pdf",
                fileType = "PDF",
                sizeBytes = 1258291L, // 1.2 MB
                formattedSize = "1.2 MB",
                uploadTimestamp = 1788595200000L, // 05 Sep 2026
                formattedDate = "05 Sep 2026",
                pageCount = 1,
                subTitle = "General Consultation",
                documentType = "Prescription",
                documentDate = "05 Sep 2026",
                doctorOrHospital = "Dr. Arvind Vaidya (VedaMrit Ayurveda)",
                notes = "Herbal supplements and dietary guidelines for Vata-Pitta balance."
            ),
            MedicalDocument(
                id = "seed-xray",
                title = "X-Ray Report",
                fileName = "Chest_XRay_Digital.jpg",
                fileType = "JPG",
                sizeBytes = 3250585L, // 3.1 MB
                formattedSize = "3.1 MB",
                uploadTimestamp = 1787904000000L, // 28 Aug 2026
                formattedDate = "28 Aug 2026",
                pageCount = 1,
                subTitle = "Chest X-Ray",
                documentType = "X-Ray / Scan",
                documentDate = "28 Aug 2026",
                doctorOrHospital = "Metro Imaging Center",
                notes = "PA view chest radiograph. Lung fields clear, cardiothoracic ratio normal."
            )
        )
    }

    private fun MedicalDocument.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("fileName", fileName)
            put("fileType", fileType)
            put("sizeBytes", sizeBytes)
            put("formattedSize", formattedSize)
            put("uploadTimestamp", uploadTimestamp)
            put("formattedDate", formattedDate)
            put("pageCount", pageCount)
            put("localPath", localPath ?: "")
            put("subTitle", subTitle)
            put("documentType", documentType)
            put("documentDate", documentDate)
            put("doctorOrHospital", doctorOrHospital)
            put("notes", notes)
            val sharedArray = JSONArray()
            sharedWith.forEach { sharedArray.put(it) }
            put("sharedWith", sharedArray)
        }
    }

    private fun JSONObject.toMedicalDocument(): MedicalDocument {
        val sharedList = mutableListOf<String>()
        val sharedArray = optJSONArray("sharedWith")
        if (sharedArray != null) {
            for (i in 0 until sharedArray.length()) {
                sharedArray.optString(i)?.let { sharedList.add(it) }
            }
        }

        val fDate = optString("formattedDate", "08 Sep 2026")
        return MedicalDocument(
            id = optString("id"),
            title = optString("title"),
            fileName = optString("fileName"),
            fileType = optString("fileType", "PDF"),
            sizeBytes = optLong("sizeBytes"),
            formattedSize = optString("formattedSize"),
            uploadTimestamp = optLong("uploadTimestamp"),
            formattedDate = fDate,
            pageCount = optInt("pageCount", 1),
            localPath = optString("localPath").takeIf { it.isNotBlank() },
            subTitle = optString("subTitle", ""),
            documentType = optString("documentType", "Lab Report"),
            documentDate = optString("documentDate", fDate),
            doctorOrHospital = optString("doctorOrHospital", ""),
            notes = optString("notes", ""),
            sharedWith = sharedList
        )
    }
}

