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
        sourceBytes: ByteArray? = null
    ): MedicalDocument {
        val id = UUID.randomUUID().toString()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date())
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
            formattedDate = formattedDate,
            pageCount = pageCount,
            localPath = localPath
        )

        val currentList = getDocuments(context).toMutableList()
        currentList.add(0, newDoc)
        saveDocuments(context, currentList)
        return newDoc
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
                id = "seed-1",
                title = "Prescription",
                fileName = "Prescription_September.pdf",
                fileType = "PDF",
                sizeBytes = 2516582L,
                formattedSize = "2.4 MB",
                uploadTimestamp = System.currentTimeMillis() - 86400000L,
                formattedDate = "09 Sep 2026",
                pageCount = 1
            ),
            MedicalDocument(
                id = "seed-2",
                title = "Blood Test Report",
                fileName = "Blood_Test_Panel.pdf",
                fileType = "PDF",
                sizeBytes = 1887436L,
                formattedSize = "1.8 MB",
                uploadTimestamp = System.currentTimeMillis() - 172800000L,
                formattedDate = "07 Sep 2026",
                pageCount = 2
            ),
            MedicalDocument(
                id = "seed-3",
                title = "Doctor Prescription",
                fileName = "Ayurvedic_Consultation.jpg",
                fileType = "JPG",
                sizeBytes = 943718L,
                formattedSize = "920 KB",
                uploadTimestamp = System.currentTimeMillis() - 604800000L,
                formattedDate = "02 Sep 2026",
                pageCount = 1
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
        }
    }

    private fun JSONObject.toMedicalDocument(): MedicalDocument {
        return MedicalDocument(
            id = optString("id"),
            title = optString("title"),
            fileName = optString("fileName"),
            fileType = optString("fileType", "PDF"),
            sizeBytes = optLong("sizeBytes"),
            formattedSize = optString("formattedSize"),
            uploadTimestamp = optLong("uploadTimestamp"),
            formattedDate = optString("formattedDate"),
            pageCount = optInt("pageCount", 1),
            localPath = optString("localPath").takeIf { it.isNotBlank() }
        )
    }
}
