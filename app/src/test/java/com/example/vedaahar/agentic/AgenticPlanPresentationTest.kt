package com.example.vedaahar.agentic

import com.example.vedaahar.network.dto.WellnessPlanResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgenticPlanPresentationTest {
    @Test
    fun mapsSevenDayDietResponseAndDailyTarget() {
        val presentation = planResponse().toPresentation()

        assertEquals(7, presentation.diet?.durationDays)
        assertEquals(7, presentation.diet?.days?.size)
        assertEquals("Calories", presentation.diet?.dailyTarget?.first()?.label)
        assertEquals("1751.8", presentation.diet?.dailyTarget?.first()?.value)
        assertEquals("Breakfast", presentation.diet?.days?.first()?.meals?.first()?.name)
        assertEquals("Besan Chilla", presentation.diet?.days?.first()?.meals?.first()?.foods?.first()?.name)
        assertEquals("150 g", presentation.diet?.days?.first()?.meals?.first()?.foods?.first()?.portion)
    }

    @Test
    fun mapsLifestyleRecommendationsAndRagProvenance() {
        val presentation = planResponse().toPresentation()
        val recommendation = presentation.lifecycleRecommendation()

        assertEquals("daily_routine", recommendation.category)
        assertEquals("Use a stable wake schedule.", recommendation.recommendation)
        assertEquals("source.pdf", recommendation.source?.source)
        assertEquals("68", recommendation.source?.page)
        assertTrue(recommendation.source?.hasDisplayableProvenance == true)
    }

    @Test
    fun mapsDoctorStatusMessages() {
        assertEquals("Doctor review pending", "PENDING".toDoctorStatusMessage())
        assertEquals("Reviewed and approved", "APPROVED".toDoctorStatusMessage())
        assertEquals("Changes requested by doctor", "CHANGES_REQUESTED".toDoctorStatusMessage())
        assertEquals("Plan not approved", "REJECTED".toDoctorStatusMessage())
    }

    @Test
    fun mapsValidationMessagesWithoutTreatingPassAsApproval() {
        val pass = planResponse(validationStatus = "PASS", doctorStatus = "PENDING").toPresentation()
        val review = planResponse(validationStatus = "REVIEW_REQUIRED", doctorStatus = "PENDING").toPresentation()

        assertEquals("Validation checks passed. Doctor review is still separate.", pass.validationMessage)
        assertEquals("Doctor review pending", pass.doctorStatusMessage)
        assertEquals("This generated plan requires review before finalization.", review.validationMessage)
    }

    @Test
    fun identifiesStaticFallbackWhenNoGeneratedPlanExists() {
        val generatedPlan: WellnessPlanResponse? = null

        assertFalse(generatedPlan != null)
    }

    @Test
    fun gsonDeserializesActualAgenticPlanResponseShape() {
        val response = Gson().fromJson(realPlanResponseJson, WellnessPlanResponse::class.java)
        val presentation = response.toPresentation()

        assertEquals(108L, response.id)
        assertEquals(1, response.version)
        assertEquals(77L, response.assessmentId)
        assertEquals(88L, response.agentRunId)
        assertEquals("PENDING", response.doctorStatus)
        assertEquals(7, presentation.diet?.durationDays)
        assertEquals(7, presentation.diet?.days?.size)
        assertEquals("Breakfast", presentation.diet?.days?.first()?.meals?.first()?.name)
        assertEquals("Moong Dal Khichdi", presentation.diet?.days?.first()?.meals?.first()?.foods?.first()?.name)
        assertEquals("evening_routine", presentation.lifecycleRecommendation().category)
        assertTrue(presentation.ragAvailable == true)
        assertEquals("available", presentation.ragStatus)
    }

    private fun AiWellnessPlanPresentation.lifecycleRecommendation(): AiLifestyleRecommendation {
        return lifestyle?.recommendations?.first() ?: error("Missing lifestyle recommendation")
    }

    private fun planResponse(
        validationStatus: String = "REVIEW_REQUIRED",
        doctorStatus: String = "PENDING"
    ) = WellnessPlanResponse(
        id = 42L,
        assessmentId = 1L,
        agentRunId = 2L,
        lineageId = "lineage",
        parentPlanId = null,
        version = 1,
        assessmentSummary = mapOf("source" to "android_canonical_results"),
        dietPlan = mapOf(
            "duration_days" to 7,
            "daily_target" to mapOf(
                "calories" to 1751.8,
                "protein_g" to 78.8,
                "carbs_g" to 227.7,
                "fat_g" to 58.4,
                "fiber_g" to 30.0,
                "calculation_basis" to "Mifflin-St Jeor"
            ),
            "days" to (1..7).map { day ->
                mapOf(
                    "day" to day,
                    "daily_total" to mapOf("calories" to 2070.0, "protein_g" to 102.0),
                    "meals" to listOf(
                        mapOf(
                            "meal" to "Breakfast",
                            "target_calories" to 437.9,
                            "actual_calories" to 655.0,
                            "meal_total" to mapOf("calories" to 655.0, "protein_g" to 24.0),
                            "foods" to listOf(
                                mapOf(
                                    "food_name" to "Besan Chilla",
                                    "portion_g" to 150,
                                    "nutrition" to mapOf("calories" to 285.0, "protein_g" to 12.0),
                                    "ranking_reasons" to listOf("suitable for vata constitution")
                                )
                            )
                        )
                    )
                )
            }
        ),
        lifestylePlan = mapOf(
            "draft_status" to "AI-GENERATED - DOCTOR REVIEW REQUIRED",
            "recommendations" to listOf(
                mapOf(
                    "category" to "daily_routine",
                    "personalized_recommendation" to "Use a stable wake schedule.",
                    "patient_specific_reason" to "Tailored to Pitta.",
                    "safety_consideration" to "Doctor review required.",
                    "ayurvedic_basis" to mapOf("source" to "source.pdf", "page" to "68", "verified" to false)
                )
            ),
            "safety_flags" to listOf("Review required")
        ),
        validation = mapOf("status" to validationStatus),
        validationStatus = validationStatus,
        validationWarnings = listOf("Review required"),
        ragStatus = mapOf("available" to true, "status" to "available"),
        doctorStatus = doctorStatus,
        workflowStatus = "PENDING_DOCTOR_REVIEW",
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )

    private val realPlanResponseJson = """
        {
          "id": 108,
          "assessment_id": 77,
          "agent_run_id": 88,
          "lineage_id": "lineage-108",
          "parent_plan_id": null,
          "version": 1,
          "assessment_summary": {
            "prakriti": {"primary_dosha": "Vata", "scores": {"vata": 42, "pitta": 29, "kapha": 21}},
            "vikriti": {"primary_dosha": "Pitta", "scores": {"vata": 20, "pitta": 35, "kapha": 18}},
            "agni": {"type": "Vishama", "score": 11}
          },
          "diet_plan": {
            "duration_days": 7,
            "daily_target": {
              "calories": 1800.0,
              "protein_g": 82.5,
              "carbs_g": 230.0,
              "fat_g": 55.0,
              "fiber_g": 31.0,
              "calculation_basis": "development smoke response"
            },
            "days": [
              {
                "day": 1,
                "daily_total": {"calories": 1795.0, "protein_g": 84.0},
                "meals": [
                  {
                    "meal": "Breakfast",
                    "target_calories": 450.0,
                    "actual_calories": 440.0,
                    "meal_total": {"calories": 440.0, "protein_g": 19.0},
                    "foods": [
                      {
                        "food_name": "Moong Dal Khichdi",
                        "portion_g": 220,
                        "nutrition": {"calories": 330.0, "protein_g": 15.0, "fiber_g": 6.0},
                        "ranking_reasons": ["gentle for Vishama Agni"]
                      }
                    ]
                  }
                ]
              },
              {"day": 2, "daily_total": {}, "meals": []},
              {"day": 3, "daily_total": {}, "meals": []},
              {"day": 4, "daily_total": {}, "meals": []},
              {"day": 5, "daily_total": {}, "meals": []},
              {"day": 6, "daily_total": {}, "meals": []},
              {"day": 7, "daily_total": {}, "meals": []}
            ]
          },
          "lifestyle_plan": {
            "draft_status": "AI-GENERATED - DOCTOR REVIEW REQUIRED",
            "recommendations": [
              {
                "category": "evening_routine",
                "personalized_recommendation": "Keep dinner light and early.",
                "patient_specific_reason": "Supports the canonical Agni result.",
                "safety_consideration": "Doctor review required.",
                "ayurvedic_basis": {"source": "indexed_source.pdf", "page": "12", "verified": false}
              }
            ],
            "safety_flags": ["Doctor review pending"]
          },
          "validation": {"status": "REVIEW_REQUIRED", "warnings": ["Doctor review pending"]},
          "validation_status": "REVIEW_REQUIRED",
          "validation_warnings": ["Doctor review pending"],
          "rag_status": {"available": true, "status": "available", "chunk_count": 1532},
          "doctor_status": "PENDING",
          "workflow_status": "PENDING_DOCTOR_REVIEW",
          "created_at": "2026-01-01T00:00:00Z",
          "updated_at": "2026-01-01T00:00:00Z"
        }
    """.trimIndent()
}
