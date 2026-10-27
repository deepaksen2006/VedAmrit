package com.example.vedaahar.care

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vedaahar.BuildConfig
import com.example.vedaahar.CommunityCareScreen
import com.example.vedaahar.network.CareDirectoryClient
import com.example.vedaahar.ui.theme.VedAaharTheme
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.concurrent.TimeUnit

/** All hospital responses are isolated HTTP fixtures, never live Django writes. */
@RunWith(AndroidJUnit4::class)
class VerifiedHospitalFixtureMapTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun onlyVerifiedApprovedFixtureHasNativeMarkerAndSelectableDetails() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("verifyFixtureCareMap") == "true")
        assertTrue("Configure Android maps for the native marker check", BuildConfig.MAPTILER_API_KEY.isNotBlank())
        rule.runOnUiThread { rule.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(json("""{"count":2,"next":null,"previous":null,"results":[
              {"id":9000000001,"name":"Isolated verified hospital fixture","address":"1 Synthetic Test Street",
               "description":"Isolated HTTP test data only","city":"Test City","state":"Test State","postal_code":"000001",
               "phone":"9900000011","email":"","facilities":["Synthetic fixture service"],
               "latitude":"24.588336","longitude":"73.679142","verification_status":"VERIFIED","location_status":"APPROVED"},
              {"id":9000000002,"name":"Isolated draft with approved position","address":"2 Synthetic Test Street",
               "description":"Must stay hidden","city":"Test City","state":"Test State","postal_code":"000001",
               "phone":"9900000012","email":"","facilities":[],
               "latitude":"24.588336","longitude":"73.679142","verification_status":"DRAFT","location_status":"APPROVED"}]}
            """.trimIndent()).setBodyDelay(500, TimeUnit.MILLISECONDS))
            val model = CareDirectoryViewModel(CareDirectoryRepository(CareDirectoryClient.create(server.url("/").toString())))
            assertTrue(model.state.value.loading)
            rule.setContent { VedAaharTheme { CommunityCareScreen(model = model) } }
            rule.waitUntil(15_000) { !model.state.value.loading }
            assertNull(model.state.value.error)
            assertEquals(listOf(9000000001L), model.state.value.hospitals.map { it.id })
            val initial = server.takeRequest(3, TimeUnit.SECONDS)!!
            assertNull(initial.getHeader("Authorization"))
            assertNull(initial.requestUrl!!.queryParameter("key"))

            val map = renderedMap()
            rule.runOnIdle {
                val marker = map.markers.single()
                assertEquals("Isolated verified hospital fixture", marker.title)
                assertEquals(24.588336, marker.position.latitude, 0.000001)
                assertEquals(73.679142, marker.position.longitude, 0.000001)
            }
            val point = rule.runOnIdle { map.projection.toScreenLocation(LatLng(24.588336, 73.679142)) }
            val density = rule.activity.resources.displayMetrics.density
            rule.onNodeWithTag("care-map-ready").performTouchInput { click(Offset(point.x, point.y - 18 * density)) }
            rule.waitUntil(10_000) { model.state.value.selectedId == 9000000001L }
            rule.onNodeWithTag("care-list").performScrollToNode(hasText("Selected hospital"))
            rule.onNodeWithText("Selected hospital").assertIsDisplayed()
            rule.onAllNodesWithText("1 Synthetic Test Street, Test City, Test State, 000001").onFirst().assertIsDisplayed()
            rule.onNodeWithText("Isolated draft with approved position").assertDoesNotExist()

            // A later empty search must remove the old marker and selection.
            server.enqueue(json("""{"count":0,"next":null,"previous":null,"results":[]}"""))
            rule.onNodeWithTag("care-list").performScrollToNode(hasText("Hospital name or city"))
            rule.onNodeWithText("Hospital name or city").performTextInput("NoSuchFixture")
            rule.onNodeWithText("Search directory").performClick()
            rule.waitUntil(15_000) { !model.state.value.loading && model.state.value.search.text == "NoSuchFixture" }
            assertTrue(model.state.value.hospitals.isEmpty())
            assertNull(model.state.value.selectedId)
            val emptyMap = renderedMap()
            rule.runOnIdle { assertTrue(emptyMap.markers.isEmpty()) }
            rule.onNodeWithTag("care-list").performScrollToNode(hasText("No verified hospitals found. Try another name or city."))
            rule.onNodeWithText("No verified hospitals found. Try another name or city.").assertIsDisplayed()

            server.enqueue(json("""{"detail":"Isolated unavailable fixture"}""").setResponseCode(503))
            rule.runOnUiThread { model.refresh() }
            rule.waitUntil(15_000) { !model.state.value.loading && model.state.value.error != null }
            rule.onNodeWithTag("care-list").performScrollToNode(hasText("Hospital directory is unavailable. Please retry."))
            rule.onNodeWithText("Hospital directory is unavailable. Please retry.").assertIsDisplayed()
        } finally { server.shutdown() }
    }

    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)

    private fun renderedMap(): MapLibreMap {
        rule.onNodeWithTag("care-list").performScrollToNode(hasTestTag("care-map"))
        rule.waitUntil(40_000) { rule.onAllNodesWithTag("care-map-ready").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        var result: MapLibreMap? = null
        rule.runOnUiThread { requireNotNull(findMap(rule.activity.window.decorView)).getMapAsync { result = it } }
        rule.waitUntil(10_000) { result != null }
        return requireNotNull(result)
    }

    private fun findMap(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findMap(view.getChildAt(i))?.let { return it }
        return null
    }
}
