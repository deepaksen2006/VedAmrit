package com.example.vedaahar.agentic

import com.example.vedaahar.network.dto.WellnessPlanResponse
import com.example.vedaahar.BuildConfig

data class AiWellnessPlanPresentation(
    val planId: Long,
    val diet: AiDietPlan?,
    val lifestyle: AiLifestylePlan?,
    val validationStatus: String?,
    val validationMessage: String,
    val validationWarnings: List<String>,
    val doctorStatus: String?,
    val doctorStatusMessage: String,
    val workflowStatus: String?,
    val ragAvailable: Boolean?,
    val ragStatus: String?
)

data class AiDietPlan(
    val durationDays: Int?,
    val dailyTarget: List<NutritionMetric>,
    val days: List<AiDietDay>
)

data class AiDietDay(
    val day: Int?,
    val meals: List<AiMeal>,
    val dailyTotal: List<NutritionMetric>,
    val title: String? = null,
    val description: String? = null,
    val date: String? = null
)

data class AiMeal(
    val name: String,
    val targetCalories: String?,
    val actualCalories: String?,
    val foods: List<AiFood>,
    val mealTotal: List<NutritionMetric>,
    val description: String? = null
)

data class AiFood(
    val name: String,
    val portion: String?,
    val nutrition: List<NutritionMetric>,
    val rankingReasons: List<String>,
    val description: String? = null
)

data class AiLifestylePlan(
    val draftStatus: String?,
    val recommendations: List<AiLifestyleRecommendation>,
    val safetyFlags: List<String>
)

data class AiLifestyleRecommendation(
    val category: String?,
    val recommendation: String?,
    val reason: String?,
    val safetyConsideration: String?,
    val source: AiSource?
)

data class AiSource(
    val source: String?,
    val page: String?
) {
    val hasDisplayableProvenance: Boolean = !source.isNullOrBlank() && source != "Not provided" && source != "unknown"
}

data class NutritionMetric(
    val label: String,
    val value: String
)

fun WellnessPlanResponse.toPresentation(): AiWellnessPlanPresentation {
    val displayable = draftViewAllowed && validationStatus?.uppercase() in setOf("PASS", "REVIEW_REQUIRED") &&
        (doctorStatus?.uppercase() == "APPROVED" || BuildConfig.AGENTIC_DRAFT_PREVIEW)
    return AiWellnessPlanPresentation(
        planId = id,
        diet = if (displayable) dietPlan?.toDietPlan() else null,
        lifestyle = if (displayable) lifestylePlan?.toLifestylePlan() else null,
        validationStatus = validationStatus,
        validationMessage = validationStatus.toValidationMessage(),
        validationWarnings = (validationWarnings.orEmpty() + (validation?.get("failed_checks") as? List<*>).orEmpty()).distinct().mapNotNull { it?.toString()?.takeIf(String::isNotBlank) },
        doctorStatus = doctorStatus,
        doctorStatusMessage = doctorStatus.toDoctorStatusMessage(),
        workflowStatus = workflowStatus,
        ragAvailable = ragStatus?.get("available") as? Boolean,
        ragStatus = ragStatus?.get("status")?.toString()
    )
}

fun String?.toValidationMessage(): String {
    return when (this?.uppercase()) {
        "PASS" -> "Validation checks passed. Doctor review is still separate."
        "REVIEW_REQUIRED" -> "This generated plan requires review before finalization."
        "FAIL" -> "This generated plan did not pass validation and requires review."
        else -> "Validation status is not available yet."
    }
}

fun String?.toDoctorStatusMessage(): String {
    return when (this?.uppercase()) {
        "APPROVED" -> "Reviewed and approved"
        "CHANGES_REQUESTED" -> "Changes requested by doctor"
        "REJECTED" -> "Plan not approved"
        else -> "Doctor review pending"
    }
}

private fun Map<String, Any?>.toDietPlan(): AiDietPlan {
    return AiDietPlan(
        durationDays = number("duration_days")?.toInt(),
        dailyTarget = metrics(map("daily_target")),
        days = list("days").mapNotNull { it.asMapOrNull()?.toDietDay() }
    )
}

private fun Map<String, Any?>.toDietDay(): AiDietDay {
    return AiDietDay(
        day = number("day")?.toInt(),
        meals = list("meals").mapNotNull { it.asMapOrNull()?.toMeal() },
        dailyTotal = metrics(map("daily_total")),
        title = this["title"] as? String,
        description = (this["description"] as? String) ?: (this["notes"] as? String),
        date = this["date"] as? String
    )
}

private fun Map<String, Any?>.toMeal(): AiMeal {
    return AiMeal(
        name = string("meal") ?: "Meal",
        targetCalories = number("target_calories")?.formatNumber(),
        actualCalories = number("actual_calories")?.formatNumber(),
        foods = list("foods").mapNotNull { it.asMapOrNull()?.toFood() },
        mealTotal = metrics(map("meal_total")),
        description = (this["description"] as? String) ?: (this["notes"] as? String)
    )
}

private fun Map<String, Any?>.toFood(): AiFood {
    return AiFood(
        name = string("food_name") ?: "Food",
        portion = number("portion_g")?.formatNumber()?.let { "$it g" },
        nutrition = metrics(map("nutrition")),
        rankingReasons = list("ranking_reasons").mapNotNull { it?.toString()?.takeIf(String::isNotBlank) },
        description = this["description"] as? String
    )
}

private fun Map<String, Any?>.toLifestylePlan(): AiLifestylePlan {
    return AiLifestylePlan(
        draftStatus = string("draft_status"),
        recommendations = list("recommendations").mapNotNull { it.asMapOrNull()?.toLifestyleRecommendation() },
        safetyFlags = list("safety_flags").mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }
    )
}

private fun Map<String, Any?>.toLifestyleRecommendation(): AiLifestyleRecommendation {
    val source = map("ayurvedic_basis")?.let {
        AiSource(
            source = it.string("source"),
            page = it.string("page")
        )
    }?.takeIf { it.hasDisplayableProvenance }
    return AiLifestyleRecommendation(
        category = string("category"),
        recommendation = string("personalized_recommendation"),
        reason = string("patient_specific_reason"),
        safetyConsideration = string("safety_consideration"),
        source = source
    )
}

private fun metrics(values: Map<String, Any?>?): List<NutritionMetric> {
    if (values == null) return emptyList()
    val fields = listOf("calories" to "Calories", "protein_g" to "Protein", "carbs_g" to "Carbs",
        "fat_g" to "Fat", "fiber_g" to "Fiber")
    val result = fields.mapNotNull { (key, label) ->
        val raw = values[key] ?: if (key == "carbs_g") values["carbohydrates_g"] else null
        val number = (raw as? Number)?.toDouble() ?: (raw as? String)?.toDoubleOrNull()
        if (number == null || !number.isFinite() || number < 0) null
        else NutritionMetric(label, number.formatNumber())
    }
    val basis = (values["calculation_basis"] as? String)?.takeIf(String::isNotBlank)
    return if (basis != null) result + NutritionMetric("Basis", basis) else result
}

private fun Map<String, Any?>.string(key: String): String? = this[key]?.toString()?.takeIf(String::isNotBlank)

private fun Map<String, Any?>.number(key: String): Number? {
    return when (val value = this[key]) {
        is Number -> value
        is String -> value.toDoubleOrNull()
        else -> null
    }?.takeIf { it.toDouble().isFinite() }
}

private fun Map<String, Any?>.map(key: String): Map<String, Any?>? = this[key].asMapOrNull()

private fun Map<String, Any?>.list(key: String): List<Any?> = this[key] as? List<Any?> ?: emptyList()

private fun Any?.asMapOrNull(): Map<String, Any?>? {
    @Suppress("UNCHECKED_CAST")
    return this as? Map<String, Any?>
}

private fun Number.formatNumber(): String {
    val value = toDouble()
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        "%.1f".format(value)
    }
}
