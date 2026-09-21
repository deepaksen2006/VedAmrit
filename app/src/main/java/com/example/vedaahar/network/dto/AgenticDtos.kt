package com.example.vedaahar.network.dto

import com.google.gson.annotations.SerializedName

data class AgenticPlanRequest(
    @SerializedName("patient_profile")
    val patientProfile: Map<String, Any?>,
    val assessment: CanonicalAssessmentDto,
    @SerializedName("plan_days")
    val planDays: Int? = null
)

data class CanonicalAssessmentDto(
    val prakriti: Map<String, Any?>,
    val vikriti: Map<String, Any?>,
    val agni: Map<String, Any?>
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
    val updatedAt: String?
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
