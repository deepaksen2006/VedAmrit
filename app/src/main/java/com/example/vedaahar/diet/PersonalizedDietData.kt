package com.example.vedaahar.diet

import com.example.vedaahar.agentic.*
import com.example.vedaahar.network.dto.WellnessPlanResponse
import com.example.vedaahar.profile.StoredPatientProfile
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

data class PersonalizedDietData(
    val content: AiWellnessPlanPresentation,
    val warnings: List<String>,
    val foodsToEat: List<String>,
    val foodsToAvoid: List<String>,
    val foodsToEatSource: String,
    val foodsToAvoidSource: String,
    val usable: Boolean
) {
    fun day(number: Int) = content.diet?.days?.firstOrNull { it.day == number }
}

fun WellnessPlanResponse.personalizedDietData(profile: StoredPatientProfile? = null): PersonalizedDietData {
    val rendered = toPresentation()
    val profileMatches = profile == null || (profile.profileVersion != null && profile.profileVersion == profileVersion)
    val usable = inputsCurrent && profileMatches && doctorStatus?.uppercase() !in setOf("REJECTED", "CHANGES_REQUESTED") &&
        rendered.diet != null && rendered.lifestyle != null
    val content = if (usable) rendered else rendered.copy(diet = null, lifestyle = null)
    val warnings = (rendered.validationWarnings + strings(validation?.get("issues")) +
        strings((validation?.get("diet_validation") as? Map<*, *>)?.get("issues")) +
        strings(lifestylePlan?.get("safety_flags"))).distinct()
    val explicitEat = if (usable) strings(dietPlan?.get("foods_to_prefer")) + strings(dietPlan?.get("foods_to_eat")) else emptyList()
    val eat = explicitEat.ifEmpty { content.diet?.days.orEmpty().flatMap { it.meals }.flatMap { it.foods }.map { it.name } }.distinct()
    val frozen = (validation?.get("profile") as? Map<*, *>)
    val matchingLocal = profile?.takeIf { profileVersion != null && it.profileVersion == profileVersion }
    val avoid = if (usable) buildList {
        addAll(strings(dietPlan?.get("foods_to_avoid")))
        addAll(strings(frozen?.get("foods_to_avoid") ?: matchingLocal?.foodsToAvoid).map { "Avoid: $it" })
        addAll(strings(frozen?.get("allergies") ?: matchingLocal?.allergies).map { "Allergy: $it" })
        addAll(strings(frozen?.get("food_intolerances") ?: matchingLocal?.foodIntolerances).map { "Intolerance: $it" })
        addAll(strings(frozen?.get("doctor_restrictions") ?: matchingLocal?.doctorRestrictions).map { "Doctor restriction: $it" })
    }.distinct() else emptyList()
    return PersonalizedDietData(content, warnings, eat, avoid,
        if (explicitEat.isNotEmpty()) "Included in the saved plan's food guidance." else "Foods included in your saved meal schedule; follow their recorded portions.",
        if (frozen != null || matchingLocal != null) "From the saved plan and matching patient restrictions."
        else "Only avoidance guidance supplied with this plan is shown.", usable)
}

private fun strings(value: Any?): List<String> = when (value) {
    is String -> listOf(value).filter(String::isNotBlank)
    is List<*> -> value.filterIsInstance<String>().filter(String::isNotBlank)
    else -> emptyList()
}

fun savedDayDate(raw: String?): String? {
    if (raw == null || !Regex("\\d{4}-\\d{2}-\\d{2}").matches(raw)) return null
    val position = ParsePosition(0)
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }.parse(raw, position)
    return raw.takeIf { parsed != null && position.index == raw.length }
}

fun mealCategory(raw: String): String = when (raw.trim().lowercase()) {
    "breakfast" -> "Breakfast"
    "lunch" -> "Lunch"
    "snack", "evening snack" -> "Evening Snack"
    "dinner" -> "Dinner"
    else -> raw
}

fun mealNutrients(meal: AiMeal): List<NutritionMetric> {
    val nutrients = meal.mealTotal.filter { it.label != "Basis" }
    val actual = meal.actualCalories?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    return if (nutrients.none { it.label == "Calories" } && actual != null)
        listOf(NutritionMetric("Calories", meal.actualCalories!!)) + nutrients else nutrients
}

