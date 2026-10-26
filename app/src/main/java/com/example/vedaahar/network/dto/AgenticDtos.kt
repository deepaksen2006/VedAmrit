package com.example.vedaahar.network.dto

import com.google.gson.annotations.SerializedName

data class AgenticPlanRequest(
    @SerializedName("patient_profile")
    val patientProfile: Map<String, Any?>,
    val assessment: CanonicalAssessmentDto,
    @SerializedName("plan_days")
    val planDays: Int? = null,
    @SerializedName("assessment_id") val assessmentId: Long? = null,
    @SerializedName("assessment_version") val assessmentVersion: String? = null,
    @SerializedName("profile_version") val profileVersion: String? = null,
    @SerializedName("generation_id") val generationId: String? = null
)

data class CanonicalAssessmentDto(
    val prakriti: Map<String, Any?>,
    val vikriti: Map<String, Any?>,
    val agni: Map<String, Any?>
)

data class CanonicalAssessmentRequest(
    @SerializedName("assessment_profile")
    val assessmentProfile: Map<String, Any?>,
    @SerializedName("submission_id") val submissionId: String? = null
)

data class AssessmentResultResponse(
    val id: Long,
    @SerializedName("prakriti_result")
    val prakritiResult: Map<String, Any?>,
    @SerializedName("vikriti_result")
    val vikritiResult: Map<String, Any?>,
    @SerializedName("agni_result")
    val agniResult: Map<String, Any?>,
    @SerializedName("canonical_payload")
    val canonicalPayload: Map<String, Any?>?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    @SerializedName("assessment_version") val assessmentVersion: String? = null,
    @SerializedName("submission_id") val submissionId: String? = null
)

data class WellnessPlanResponse(
    val id: Long,
    @SerializedName("assessment_id")
    val assessmentId: Long?,
    @SerializedName("agent_run_id")
    val agentRunId: Long?,
    @SerializedName("lineage_id")
    val lineageId: String?,
    @SerializedName("parent_plan_id")
    val parentPlanId: Long?,
    val version: Int,
    @SerializedName("assessment_summary")
    val assessmentSummary: Map<String, Any?>?,
    @SerializedName("diet_plan")
    val dietPlan: Map<String, Any?>?,
    @SerializedName("lifestyle_plan")
    val lifestylePlan: Map<String, Any?>?,
    val validation: Map<String, Any?>?,
    @SerializedName("validation_status")
    val validationStatus: String?,
    @SerializedName("validation_warnings")
    val validationWarnings: List<Any?>?,
    @SerializedName("rag_status")
    val ragStatus: Map<String, Any?>?,
    @SerializedName("doctor_status")
    val doctorStatus: String?,
    @SerializedName("workflow_status")
    val workflowStatus: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    @SerializedName("assessment_version") val assessmentVersion: String? = null,
    @SerializedName("profile_version") val profileVersion: String? = null,
    @SerializedName("generation_id") val generationId: String? = null,
    @SerializedName("draft_view_allowed") val draftViewAllowed: Boolean = false,
    @SerializedName("draft_label") val draftLabel: String? = null,
    @SerializedName("draft_pdf_available") val draftPdfAvailable: Boolean = false,
    @SerializedName("draft_pdf_block_reasons") val draftPdfBlockReasons: List<String>? = null,
    @SerializedName("inputs_current") val inputsCurrent: Boolean = false
)

data class WellnessPlanStatusResponse(
    val id: Long,
    val version: Int,
    @SerializedName("agent_run_status")
    val agentRunStatus: String?,
    @SerializedName("validation_status")
    val validationStatus: String?,
    @SerializedName("doctor_status")
    val doctorStatus: String?,
    @SerializedName("workflow_status")
    val workflowStatus: String?,
    @SerializedName("rag_status")
    val ragStatus: Map<String, Any?>?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)

data class FinalReportResponse(
    val id: Long?,
    @SerializedName("wellness_plan_id")
    val wellnessPlanId: Long?,
    @SerializedName("report_reference")
    val reportReference: String?,
    @SerializedName("report_metadata")
    val reportMetadata: Map<String, Any?>?,
    @SerializedName("finalized_at")
    val finalizedAt: String?,
    @SerializedName("created_at")
    val createdAt: String?
)

data class PatientProfileDto(
    @SerializedName("profile_version") val profileVersion: String? = null,
    val id: Long? = null,
    @SerializedName("user_id")
    val userId: Long? = null,
    val name: String? = null,
    val age: Int? = null,
    @SerializedName("date_of_birth")
    val dateOfBirth: String? = null,
    val gender: String? = null,
    @SerializedName("height_cm")
    val heightCm: String? = null,
    @SerializedName("weight_kg")
    val weightKg: String? = null,
    @SerializedName("dietary_preference")
    val dietaryPreference: String? = null,
    val goal: String? = null,
    @SerializedName("activity_level")
    val activityLevel: String? = null,
    val allergies: List<String>? = null,
    val intolerances: List<String>? = null,
    @SerializedName("food_intolerances")
    val foodIntolerances: List<String>? = null,
    @SerializedName("foods_to_avoid")
    val foodsToAvoid: List<String>? = null,
    @SerializedName("health_conditions")
    val healthConditions: List<String>? = null,
    @SerializedName("current_medicines")
    val currentMedicines: List<String>? = null,
    @SerializedName("medicine_allergies")
    val medicineAllergies: List<String>? = null,
    @SerializedName("medication_restrictions")
    val medicationRestrictions: List<String>? = null,
    @SerializedName("doctor_restrictions")
    val doctorRestrictions: List<String>? = null,
    @SerializedName("pregnancy_status")
    val pregnancyStatus: String? = null,
    @SerializedName("pregnancy_information")
    val pregnancyInformation: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)


data class GenerationStatusResponse(
    @SerializedName("generation_id") val generationId: String?,
    val status: String,
    @SerializedName("plan_id") val planId: Long?,
    @SerializedName("assessment_id") val assessmentId: Long?,
    @SerializedName("assessment_version") val assessmentVersion: String?,
    @SerializedName("profile_version") val profileVersion: String?,
    val message: String?
)
