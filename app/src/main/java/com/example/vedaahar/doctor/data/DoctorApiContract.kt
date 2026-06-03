package com.example.vedaahar.doctor.data

/*
Retrofit contract for the Django module.

Add Retrofit/Hilt dependencies before enabling this interface in production:

interface DoctorApi {
    @POST("/doctor/register")
    suspend fun registerDoctor(@Body request: DoctorRegisterRequest): DoctorAuthResponse

    @POST("/doctor/login")
    suspend fun loginDoctor(@Body request: DoctorLoginRequest): DoctorAuthResponse

    @GET("/doctors/verified")
    suspend fun verifiedDoctors(): List<DoctorResponse>

    @GET("/doctor/profile")
    suspend fun doctorProfile(@Header("Authorization") token: String): DoctorResponse

    @PUT("/doctor/status")
    suspend fun updateStatus(@Header("Authorization") token: String, @Body request: DoctorStatusRequest): DoctorResponse

    @POST("/consultation/create")
    suspend fun createConsultation(@Header("Authorization") token: String, @Body request: CreateConsultationRequest): ConsultationResponse

    @GET("/doctor/consultations")
    suspend fun doctorConsultations(@Header("Authorization") token: String): List<ConsultationResponse>

    @POST("/diet-plan/create")
    suspend fun createDietPlan(@Header("Authorization") token: String, @Body request: CreateDietPlanRequest): DietPlanResponse

    @GET("/patient/diet-plan")
    suspend fun patientDietPlan(@Header("Authorization") token: String): List<DietPlanResponse>
}
*/

data class DoctorRegisterRequest(
    val fullName: String,
    val phone: String,
    val email: String,
    val password: String,
    val bamsRegistrationNumber: String,
    val specialization: String,
    val experience: String,
    val clinicName: String,
    val consultationFee: String,
    val bio: String
)

data class DoctorLoginRequest(val identifier: String, val password: String)
data class DoctorStatusRequest(val isOnline: Boolean, val availableTimings: String)
data class DoctorAuthResponse(val access: String, val refresh: String, val isVerified: Boolean)
