package com.example.vedaahar.care

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vedaahar.CommunityCareScreen
import com.example.vedaahar.network.CareDirectoryClient
import com.example.vedaahar.ui.theme.VedAaharTheme
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CommunityCareScreenTest {
    @get:Rule val rule = createComposeRule()
    private val server = MockWebServer()
    @Before fun start() { server.start() }
    @After fun stop() { server.shutdown() }

    private fun response(body: String, code: Int = 200) = MockResponse().setResponseCode(code)
        .setHeader("Content-Type", "application/json").setBody(body)
    private val empty = """{"count":0,"next":null,"previous":null,"results":[]}"""

    private fun display(): CareDirectoryViewModel {
        val model = CareDirectoryViewModel(CareDirectoryRepository(CareDirectoryClient.create(server.url("/").toString())))
        rule.setContent { VedAaharTheme { CommunityCareScreen(model = model) } }
        return model
    }

    private fun scrollTo(text: String) { rule.onNodeWithTag("care-list").performScrollToNode(hasText(text)) }

    @Test fun emptyDirectoryCanSearchByCityAndNearbyCoordinatesWithoutLocationPermission() {
        server.enqueue(response(empty))
        val model = display()
        rule.waitUntil(10_000) { !model.state.value.loading }
        scrollTo("No verified hospitals found. Try another name or city.")
        assertEquals("/api/care/hospitals/", server.takeRequest(3, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath)
        scrollTo("Hospital name or city")
        rule.onNodeWithText("Hospital name or city").performTextInput("Udaipur")
        server.enqueue(response(empty))
        rule.onNodeWithText("Search directory").performClick()
        assertEquals("Udaipur", server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!.queryParameter("search"))
        scrollTo("Coordinates")
        rule.onNodeWithText("Coordinates").performClick()
        scrollTo("Latitude (−90 to 90)")
        rule.onNodeWithText("Latitude (−90 to 90)").performTextInput("24.5854")
        scrollTo("Longitude (−180 to 180)")
        rule.onNodeWithText("Longitude (−180 to 180)").performTextInput("73.7125")
        server.enqueue(response(empty))
        scrollTo("Search near coordinates")
        rule.onNodeWithText("Search near coordinates").performClick()
        val request = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("/api/care/hospitals/nearby/", request.requestUrl!!.encodedPath)
        assertEquals("24.5854", request.requestUrl!!.queryParameter("latitude"))
        assertEquals("73.7125", request.requestUrl!!.queryParameter("longitude"))
        assertNull(request.getHeader("Authorization"))
        rule.waitUntil(10_000) { !model.state.value.loading }
        scrollTo("No verified hospitals with approved locations were found in this radius.")
        rule.onNodeWithText("No verified hospitals with approved locations were found in this radius.").assertIsDisplayed()
    }

    @Test fun errorHasRetryAndNeverShowsDemoHospitals() {
        server.enqueue(response("""{"detail":"private body"}""", 503))
        val model = display()
        rule.waitUntil(10_000) { !model.state.value.loading }
        scrollTo("Hospital directory is unavailable. Please retry.")
        rule.onNodeWithText("Hospital directory is unavailable. Please retry.").assertIsDisplayed()
        server.enqueue(response(empty))
        scrollTo("Retry")
        rule.onNodeWithText("Retry").performClick()
        rule.waitUntil(10_000) { !model.state.value.loading }
        scrollTo("No verified hospitals found. Try another name or city.")
        rule.onNodeWithText("No verified hospitals found. Try another name or city.").assertIsDisplayed()
        rule.onNodeWithText("Arogya Multispeciality Hospital").assertDoesNotExist()
        rule.onNodeWithText("private body").assertDoesNotExist()
    }
}
