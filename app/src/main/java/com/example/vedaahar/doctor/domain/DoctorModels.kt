package com.example.vedaahar.doctor.domain

data class VerifiedDoctor(
    val id: Int,
    val fullName: String,
    val specialization: String,
    val experience: String,
    val consultationFee: String,
    val isOnline: Boolean,
    val clinicName: String,
    val bio: String
)

data class DoctorConsultation(
    val id: Int,
    val patientName: String,
    val doshaType: String,
    val symptoms: String,
    val status: String
)

data class DoctorDietPlanDraft(
    val patientName: String = "",
    val prakriti: String = "",
    val vikriti: String = "",
    val agni: String = "",
    val ama: String = "",
    val breakfast: String = "",
    val lunch: String = "",
    val dinner: String = "",
    val foodsToEat: String = "",
    val foodsToAvoid: String = "",
    val hydrationAdvice: String = "",
    val yogaRecommendation: String = "",
    val sleepAdvice: String = ""
)
