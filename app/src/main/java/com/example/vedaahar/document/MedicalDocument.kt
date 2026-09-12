package com.example.vedaahar.document

data class MedicalDocument(
    val id: String,
    val title: String,
    val fileName: String,
    val fileType: String, // "PDF", "JPG", "PNG"
    val sizeBytes: Long,
    val formattedSize: String,
    val uploadTimestamp: Long,
    val formattedDate: String,
    val pageCount: Int = 1,
    val localPath: String? = null,
    val subTitle: String = "",
    val documentType: String = "Lab Report",
    val documentDate: String = formattedDate,
    val doctorOrHospital: String = "",
    val notes: String = "",
    val sharedWith: List<String> = emptyList()
)
