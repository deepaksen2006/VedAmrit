package com.example.vedaahar.network.dto

import com.google.gson.annotations.SerializedName

data class CarePage<T>(val count: Int, val next: String?, val previous: String?, val results: List<T>)

data class CareHospitalDto(
    val id: Long,
    val name: String,
    val description: String,
    val address: String,
    val city: String,
    val state: String,
    @SerializedName("postal_code") val postalCode: String,
    // Django decimals arrive as strings. Parse only when a map/number is needed.
    val latitude: String?,
    val longitude: String?,
    val phone: String,
    val email: String,
    val facilities: List<String>,
    @SerializedName("verification_status") val verificationStatus: String,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("location_status") val locationStatus: String? = null,
    @SerializedName("distance_km") val distanceKm: Double? = null,
    @SerializedName("location_attribution") val locationAttribution: String? = null
)

data class CareDoctorDto(
    val id: Long,
    @SerializedName("full_name") val fullName: String,
    val qualifications: List<String>,
    val specialization: String,
    @SerializedName("registration_number") val registrationNumber: String,
    @SerializedName("registration_council") val registrationCouncil: String,
    @SerializedName("experience_years") val experienceYears: Int,
    val bio: String,
    @SerializedName("verification_status") val verificationStatus: String,
    @SerializedName("updated_at") val updatedAt: String?,
    val affiliations: List<CareAffiliationDto>,
    @SerializedName("credentials_reviewed_at") val credentialsReviewedAt: String? = null
)

data class CareAffiliationDto(
    val id: Long,
    @SerializedName("hospital_id") val hospitalId: Long,
    @SerializedName("hospital_name") val hospitalName: String,
    val city: String,
    @SerializedName("consultation_mode") val consultationMode: String,
    val fees: String,
    @SerializedName("verification_status") val verificationStatus: String? = null
)
