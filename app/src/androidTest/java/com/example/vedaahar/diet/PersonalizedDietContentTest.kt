package com.example.vedaahar.diet

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vedaahar.agentic.AgenticPlanUiState
import com.example.vedaahar.assessment.AssessmentSyncStatus
import com.example.vedaahar.network.dto.WellnessPlanResponse
import com.example.vedaahar.ui.theme.VedAaharTheme
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PersonalizedDietContentTest {
    @get:Rule val rule = createComposeRule()

    private fun fixture(): WellnessPlanResponse {
        val days = (1..7).map { day -> mapOf("day" to day, "meals" to listOf("Breakfast", "Lunch", "Snack", "Dinner").map { meal ->
            mapOf("meal" to meal, "foods" to listOf(mapOf("food_name" to "Saved day $day $meal", "portion_g" to 125)),
                "meal_total" to mapOf("calories" to 200, "protein_g" to 8, "fiber_g" to 3))
        }) }
        val payload = mapOf("id" to 42, "version" to 1, "assessment_id" to 7,
            "assessment_version" to "a".repeat(64), "profile_version" to "b".repeat(64),
            "inputs_current" to true, "draft_view_allowed" to true, "doctor_status" to "PENDING",
            "validation_status" to "REVIEW_REQUIRED", "validation_warnings" to listOf("Saved safety warning — fixture only."),
            "diet_plan" to mapOf("duration_days" to 7, "days" to days),
            "lifestyle_plan" to mapOf("recommendations" to listOf(mapOf("category" to "routine",
                "personalized_recommendation" to "A saved lifestyle recommendation from the test fixture.",
                "patient_specific_reason" to "Saved assessment context.",
                "safety_consideration" to "Saved clinician caution remains visible.",
                "ayurvedic_basis" to mapOf("source" to "fixture-source.pdf", "page" to "12")))))
        return Gson().fromJson(Gson().toJson(payload), WellnessPlanResponse::class.java)
    }

    private fun screen(state: AgenticPlanUiState = AgenticPlanUiState.Success(fixture()),
                       complete: Boolean = true, width: Int? = null, scale: Float = 1f,
                       generate: () -> Unit = {}, refresh: () -> Unit = {}, pdf: () -> Unit = {}, takeAssessment: () -> Unit = {}) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                VedAaharTheme {
                    Box(if (width != null) Modifier.width(width.dp) else Modifier.fillMaxSize()) {
                        PersonalizedDietContent(state, null, if (complete) AssessmentSyncStatus.SYNCED else AssessmentSyncStatus.PENDING,
                            complete, {}, generate, refresh, takeAssessment,
                            pdfActions = { Button(onClick = pdf, modifier = Modifier.testTag("test-report-download")) { Text("Download Your Diet Report") } },
                            details = { Text("Exact assessment and profile versions", Modifier.testTag("test-exact-versions")) })
                    }
                }
            }
        }
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // Wait for the device compositor to present the frame after a programmatic scroll.
        android.os.SystemClock.sleep(250)
        val image = instrumentation.uiAutomation.takeScreenshot() ?: return
        File(instrumentation.targetContext.cacheDir, name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    @Test fun headerStatusAndSafetyAreVisibleBeforeTechnicalDetails() {
        screen()
        rule.onNodeWithText("My Personalized Diet Plan").assertIsDisplayed()
        rule.onNodeWithText("PENDING", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Saved safety warning", substring = true).assertIsDisplayed()
        rule.onNodeWithTag("test-exact-versions").assertDoesNotExist()
        capture("task3_3_phone_header.png")
    }

    @Test fun sevenDayTabsSwitchToThatDaysSavedMeals() {
        screen()
        rule.onNodeWithTag("personalized-diet-list").performScrollToNode(hasTestTag("diet-day-tabs"))
        rule.onNodeWithTag("diet-day-tabs").performScrollToNode(hasTestTag("diet-day-tab-7"))
        rule.onNodeWithTag("diet-day-tab-7").performClick().assertIsSelected()
        rule.onNodeWithTag("personalized-diet-list").performScrollToNode(hasText("Saved day 7 Breakfast", substring = true))
        rule.onNodeWithText("Saved day 7 Breakfast", substring = true).assertIsDisplayed()
        rule.onNode(hasText("Saved day 1 Breakfast", substring = true) and hasAnyAncestor(hasTestTag("diet-selected-day"))).assertDoesNotExist()
        rule.onNodeWithText("Evening Snack").assertExists()
        rule.onNodeWithTag("personalized-diet-list").performScrollToIndex(5)
        capture("task3_3_phone_meals.png")
    }

    @Test fun generationRefreshDetailsAndPdfActionsKeepTheirBindings() {
        var generated = 0; var refreshed = 0; var downloaded = 0
        screen(generate = { generated++ }, refresh = { refreshed++ }, pdf = { downloaded++ })
        rule.onNodeWithTag("diet-generate").performClick()
        rule.onNodeWithTag("diet-refresh").performClick()
        rule.onNodeWithContentDescription("Jump to diet report download").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("test-report-download").performClick()
        rule.onNodeWithTag("personalized-diet-list").performScrollToNode(hasTestTag("diet-details-toggle"))
        rule.onNodeWithTag("diet-details-toggle").performClick()
        rule.onNodeWithTag("test-exact-versions").assertExists()
        rule.runOnIdle { assertEquals(1, generated); assertEquals(1, refreshed); assertEquals(1, downloaded) }
    }

    @Test fun failedValidationDoesNotShowSampleMeals() {
        screen(AgenticPlanUiState.Success(fixture().copy(validationStatus = "FAIL")))
        rule.onNodeWithTag("personalized-diet-list").performScrollToNode(hasText("This plan is stale", substring = true))
        rule.onNode(hasText("Saved day 1 Breakfast", substring = true) and hasAnyAncestor(hasTestTag("diet-selected-day"))).assertDoesNotExist()
        rule.onNodeWithText("Warm Oats with Nuts and Seeds", substring = true).assertDoesNotExist()
    }

    @Test fun loadingDisablesActionsAndDoesNotInventDoctorStatus() {
        screen(AgenticPlanUiState.Loading)
        rule.onNodeWithTag("diet-generate").assertIsNotEnabled()
        rule.onNodeWithTag("diet-refresh").assertIsNotEnabled()
        rule.onNodeWithText("PENDING").assertDoesNotExist()
    }

    @Test fun missingAssessmentsOfferTheExistingAssessmentAction() {
        var taken = 0
        screen(complete = false, takeAssessment = { taken++ })
        rule.onNodeWithTag("diet-generate").assertIsNotEnabled()
        rule.onNodeWithTag("personalized-diet-list").performScrollToNode(hasText("Take assessments"))
        rule.onNodeWithText("Take assessments").performClick()
        rule.runOnIdle { assertEquals(1, taken) }
        rule.onNodeWithTag("diet-selected-day").assertDoesNotExist()
    }

    @Test fun networkFailureIsVisibleAndRefreshRemainsAvailable() {
        var refreshed = 0
        screen(AgenticPlanUiState.Error("Network unavailable. Retry when online."), refresh = { refreshed++ })
        rule.onNodeWithTag("diet-error").assertIsDisplayed()
        rule.onNodeWithTag("diet-refresh").performClick()
        rule.runOnIdle { assertEquals(1, refreshed) }
        rule.onNodeWithTag("diet-selected-day").assertDoesNotExist()
    }

    @Test fun recommendationSafetyIsVisibleBeforeExpandingSources() {
        screen()
        rule.onNodeWithTag("personalized-diet-list").performScrollToNode(hasText("Saved clinician caution remains visible."))
        rule.onNodeWithText("Saved clinician caution remains visible.").assertIsDisplayed()
        rule.onNodeWithText("Retrieved source:", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Read more & sources").performClick()
        rule.onNodeWithText("Retrieved source: fixture-source.pdf", substring = true).assertExists()
        rule.onNodeWithText("Retrieval does not clinically verify this recommendation.").assertExists()
    }

    @Test fun smallWidthWithLargeFontsRemainsScrollableWithoutLosingControls() {
        screen(width = 320, scale = 1.6f)
        rule.onNodeWithTag("personalized-diet-list").performScrollToNode(hasTestTag("diet-day-tabs"))
        rule.onNodeWithTag("diet-day-tabs").performScrollToNode(hasTestTag("diet-day-tab-4"))
        rule.onNodeWithTag("diet-day-tab-4").performClick()
        rule.onNodeWithTag("personalized-diet-list").performScrollToNode(hasText("Saved day 4 Breakfast", substring = true))
        rule.onNodeWithText("Saved day 4 Breakfast", substring = true).assertIsDisplayed()
        capture("task3_3_phone_large_font.png")
        rule.onNodeWithContentDescription("Jump to diet report download").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("test-report-download").assertIsDisplayed()
    }
}

