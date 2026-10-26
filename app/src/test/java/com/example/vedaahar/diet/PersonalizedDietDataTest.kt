package com.example.vedaahar.diet

import com.example.vedaahar.network.dto.WellnessPlanResponse
import com.example.vedaahar.profile.StoredPatientProfile
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class PersonalizedDietDataTest {
    private fun response(): WellnessPlanResponse {
        val days = (1..7).map { day -> mapOf("day" to day, "meals" to listOf("Breakfast", "Lunch", "Snack", "Dinner").map { meal ->
            mapOf("meal" to meal, "foods" to listOf(mapOf("food_name" to "Saved $meal $day", "portion_g" to 125)),
                "meal_total" to mapOf("calories" to 200.5, "protein_g" to 7, "fiber_g" to 0))
        }) }
        val data = mapOf("id" to 42, "version" to 1, "assessment_id" to 7,
            "assessment_version" to "a".repeat(64), "profile_version" to "b".repeat(64),
            "inputs_current" to true, "draft_view_allowed" to true, "doctor_status" to "PENDING",
            "validation_status" to "REVIEW_REQUIRED", "validation_warnings" to listOf("Saved safety warning"),
            "diet_plan" to mapOf("duration_days" to 7, "days" to days),
            "lifestyle_plan" to mapOf("recommendations" to listOf(mapOf("category" to "routine",
                "personalized_recommendation" to "Saved recommendation", "safety_consideration" to "Saved caution",
                "ayurvedic_basis" to mapOf("source" to "saved-source.pdf", "page" to 12))), "safety_flags" to listOf("Lifestyle caution")))
        return Gson().fromJson(Gson().toJson(data), WellnessPlanResponse::class.java)
    }

    @Test fun mapsEveryActualSavedDayAndFourMealsWithoutUsingSelectionIndexAsData() {
        val response = response()
        val data = response.personalizedDietData()
        assertTrue(data.usable)
        for (day in 1..7) {
            assertEquals(day, data.day(day)?.day)
            assertEquals(4, data.day(day)?.meals?.size)
            assertEquals("Saved Breakfast $day", data.day(day)?.meals?.first()?.foods?.first()?.name)
        }
    }

    @Test fun daySwitchUsesExplicitDayNumberEvenWhenServerOrderDiffers() {
        val original = response()
        val days = original.dietPlan!!["days"] as List<*>
        val data = original.copy(dietPlan = original.dietPlan!! + ("days" to days.reversed())).personalizedDietData()
        assertEquals("Saved Breakfast 7", data.day(7)!!.meals.first().foods.first().name)
        assertEquals("Saved Breakfast 1", data.day(1)!!.meals.first().foods.first().name)
    }

    @Test fun missingMealNutritionIsNotReplacedByZerosOrSummedFoodEstimates() {
        val original = response()
        val diet = mapOf("duration_days" to 7, "days" to listOf(mapOf("day" to 1,
            "meals" to listOf(mapOf("meal" to "Breakfast", "foods" to listOf(mapOf("food_name" to "Saved food",
                "nutrition" to mapOf("protein_g" to 9))))))))
        val meal = original.copy(dietPlan = diet).personalizedDietData().day(1)!!.meals.first()
        assertTrue(mealNutrients(meal).isEmpty())
        assertTrue(meal.foods.first().nutrition.any { it.value == "9" })
    }

    @Test fun explicitZeroIsShownButInvalidAndDuplicateNutrientsAreNot() {
        val original = response()
        val mealData = mapOf("meal" to "Lunch", "target_calories" to "NaN",
            "meal_total" to mapOf("fiber_g" to 0, "protein_g" to "bad", "calories" to "Infinity",
                "carbs_g" to 12, "carbohydrates_g" to 14))
        val diet = mapOf("days" to listOf(mapOf("day" to 1, "meals" to listOf(mealData))))
        val meal = original.copy(dietPlan = diet).personalizedDietData().day(1)!!.meals.first()
        assertNull(meal.targetCalories)
        assertEquals(1, meal.mealTotal.count { it.label == "Carbs" })
        assertEquals("12", meal.mealTotal.first { it.label == "Carbs" }.value)
        assertEquals("0", meal.mealTotal.first { it.label == "Fiber" }.value)
        assertFalse(meal.mealTotal.any { it.label == "Calories" || it.label == "Protein" })
    }

    @Test fun safetyMessagesRemainAvailableWhenMealsAreBlocked() {
        val data = response().copy(validationStatus = "FAIL", validation = mapOf("failed_checks" to listOf("Unsafe portion"),
            "issues" to listOf("Incomplete day"))).personalizedDietData()
        assertFalse(data.usable)
        assertNull(data.day(1))
        assertTrue(data.foodsToEat.isEmpty())
        assertTrue(data.warnings.containsAll(listOf("Unsafe portion", "Incomplete day", "Lifestyle caution")))
    }

    @Test fun staleAndRejectedPlansCannotAppearAsUsableGuidance() {
        for (plan in listOf(response().copy(inputsCurrent = false), response().copy(doctorStatus = "REJECTED"),
            response().copy(doctorStatus = "CHANGES_REQUESTED"), response().copy(draftViewAllowed = false))) {
            assertFalse(plan.personalizedDietData().usable)
        }
    }

    @Test fun localRestrictionsMustMatchExactPlanProfileVersion() {
        val profile = StoredPatientProfile("Fixture", "34", "Female", "164", "58",
            allergies = listOf("peanut"), foodsToAvoid = listOf("brinjal"), profileVersion = "b".repeat(64))
        val own = response().personalizedDietData(profile)
        assertTrue(own.foodsToAvoid.contains("Allergy: peanut"))
        assertTrue(own.foodsToAvoid.contains("Avoid: brinjal"))
        val other = response().personalizedDietData(profile.copy(profileVersion = "c".repeat(64)))
        assertFalse(other.usable)
        assertTrue(other.foodsToAvoid.isEmpty())
    }

    @Test fun preferredFoodsComeFromSavedOutputAndNeverGenericAdvice() {
        val original = response()
        val derived = original.personalizedDietData()
        assertTrue(derived.foodsToEat.contains("Saved Breakfast 1"))
        assertFalse(derived.foodsToEat.contains("Seasonal fruits and vegetables"))
        val explicit = original.copy(dietPlan = original.dietPlan!! + ("foods_to_prefer" to listOf("Explicit saved item"))).personalizedDietData()
        assertEquals(listOf("Explicit saved item"), explicit.foodsToEat)
    }

    @Test fun datesDescriptionsAndSourcesAreOptionalSavedFields() {
        val original = response()
        assertNull(original.personalizedDietData().day(1)?.date)
        assertNull(savedDayDate("2026-02-31"))
        assertNull(savedDayDate("not a date"))
        assertEquals("2026-10-26", savedDayDate("2026-10-26"))
        val source = original.personalizedDietData().content.lifestyle!!.recommendations.first().source
        assertEquals("saved-source.pdf", source?.source)
        assertEquals("Evening Snack", mealCategory("snack"))
    }
}

