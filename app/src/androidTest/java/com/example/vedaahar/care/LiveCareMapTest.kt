package com.example.vedaahar.care

import android.graphics.Bitmap
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vedaahar.BuildConfig
import com.example.vedaahar.CommunityCareScreen
import com.example.vedaahar.network.CareDirectoryClient
import com.example.vedaahar.ui.theme.VedAaharTheme
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Opt-in only: uses configured public APIs/map tiles, no auth/session/data mutations. */
@RunWith(AndroidJUnit4::class)
class LiveCareMapTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun configuredMapRendersWithThePublicHospitalDirectory() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("verifyLiveCareMap") == "true")
        assertTrue("Configure a dedicated Android map key before running live verification", BuildConfig.MAPTILER_API_KEY.isNotBlank())
        rule.runOnUiThread { rule.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        val model = CareDirectoryViewModel(CareDirectoryRepository(CareDirectoryClient.create()))
        rule.setContent { VedAaharTheme { CommunityCareScreen(model = model) } }
        verifyRenderedMap()
        rule.onNodeWithText("Map authorization failed. Check the Android map key configuration.").assertDoesNotExist()
        rule.onNodeWithText("Map tiles could not be loaded. Please retry.").assertDoesNotExist()
        rule.waitUntil(40_000) { !model.state.value.loading }
        assertNull("Public hospital directory must be reachable", model.state.value.error)
        val state = model.state.value
        assertTrue(state.hospitals.all { it.verificationStatus == "VERIFIED" })
        assertEquals("Current hospitals must remain private pending identity verification", 0, state.count)
        println("Live care check: directory count=${state.count}, loaded=${state.hospitals.size}, approved pins=${state.hospitals.count { it.approvedPoint() != null }}")
        screenshot("care-live-map.png")

        scrollTo("No verified hospitals found. Try another name or city.")
        rule.onNodeWithText("No verified hospitals found. Try another name or city.").assertIsDisplayed()
        scrollTo("Hospital name or city")
        rule.onNodeWithText("Hospital name or city").performTextInput("Udaipur")
        rule.onNodeWithText("Search directory").performClick()
        rule.waitUntil(40_000) { model.state.value.search.text == "Udaipur" && !model.state.value.loading }
        assertNull("Live directory city search must succeed", model.state.value.error)
        assertEquals(0, model.state.value.count)
        scrollTo("No verified hospitals found. Try another name or city.")
        screenshot("care-live-directory-empty.png")

        scrollTo("Coordinates")
        rule.onNodeWithText("Coordinates").performClick()
        scrollTo("Latitude (−90 to 90)")
        rule.onNodeWithText("Latitude (−90 to 90)").performTextInput("24.5854")
        scrollTo("Longitude (−180 to 180)")
        rule.onNodeWithText("Longitude (−180 to 180)").performTextInput("73.7125")
        scrollTo("Search near coordinates")
        rule.onNodeWithText("Search near coordinates").performClick()
        rule.waitUntil(40_000) { model.state.value.search.center != null && !model.state.value.loading }
        assertNull("Live nearby search must succeed", model.state.value.error)
        assertEquals(CarePoint(24.5854, 73.7125), model.state.value.search.center)
        assertEquals(0, model.state.value.count)
        scrollTo("No verified hospitals with approved locations were found in this radius.")
        rule.onNodeWithText("No verified hospitals with approved locations were found in this radius.").assertIsDisplayed()
        screenshot("care-live-nearby-empty.png")
        verifyRenderedMap()
        screenshot("care-live-nearby-map.png")
        println("Live care check: Udaipur directory and 20 km coordinate-nearby searches succeeded; both empty states visible; 0 public hospitals/pins")
    }

    private fun scrollTo(text: String) { rule.onNodeWithTag("care-list").performScrollToNode(hasText(text)) }

    private fun verifyRenderedMap() {
        rule.waitUntil(40_000) { rule.onAllNodesWithTag("care-list").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        rule.onNodeWithTag("care-list").performScrollToNode(hasTestTag("care-map"))
        rule.waitUntil(40_000) { rule.onAllNodesWithTag("care-map-ready").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        rule.onNodeWithTag("care-map-ready").assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        // Instrumentation runs under the target UID; use a temporary cache folder,
        // never patient preferences, databases or saved-report directories.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        rule.waitUntil(10_000) {
            ViewCompat.getRootWindowInsets(rule.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) != true
        }
        rule.waitForIdle()
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            val evidence = File(instrumentation.targetContext.cacheDir, "care-live-verification").apply { mkdirs() }
            File(evidence, name).outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }
}
