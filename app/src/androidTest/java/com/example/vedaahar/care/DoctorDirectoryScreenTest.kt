package com.example.vedaahar.care

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vedaahar.CommunityCareScreen
import com.example.vedaahar.network.CareDirectoryClient
import com.example.vedaahar.ui.theme.VedAaharTheme
import okhttp3.mockwebserver.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/** Positive paths are HTTP fixtures inside instrumentation only, never live records. */
@RunWith(AndroidJUnit4::class)
class DoctorDirectoryScreenTest {
    @get:Rule val rule = createComposeRule()
    private val server = MockWebServer()
    private var stopped = false
    @Before fun start() { server.start() }
    @After fun stop() { if (!stopped) server.shutdown() }
    private val empty = """{"count":0,"next":null,"previous":null,"results":[]}"""
    private val fixture = """{"id":881,"full_name":"Isolated doctor fixture","qualifications":["Isolated qualification"],
        "specialization":"Fixture specialty","registration_number":"FIXTURE-881","registration_council":"Isolated test council",
        "experience_years":2,"bio":"Synthetic instrumentation fixture only.","verification_status":"VERIFIED",
        "updated_at":null,"credentials_reviewed_at":"2026-10-27T00:00:00Z","affiliations":[
        {"id":991,"hospital_id":997,"hospital_name":"Isolated hospital fixture","city":"Fixture city",
         "consultation_mode":"IN_PERSON","fees":"0.00","verification_status":"VERIFIED"}]}"""
    private fun response(body: String, code: Int = 200) = MockResponse().setResponseCode(code)
        .setHeader("Content-Type", "application/json").setBody(body)
    private fun page(rows: String = fixture) = """{"count":1,"next":null,"previous":null,"results":[$rows]}"""
    private fun model() = DoctorDirectoryViewModel(DoctorDirectoryRepository(CareDirectoryClient.create(server.url("/").toString())))
    private fun display(model: DoctorDirectoryViewModel = model()): DoctorDirectoryViewModel {
        rule.setContent { VedAaharTheme { DoctorDirectoryScreen(onBack = {}, model = model) } }
        return model
    }
    private fun scroll(text: String) { rule.onNodeWithTag("doctor-directory").performScrollToNode(hasText(text)) }
    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.cacheDir, "doctor-directory-verification").apply { mkdirs() }
        File(folder, name).outputStream().use { rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun fixtureProfileCredentialsHospitalFilterAndHiddenDraft() {
        val draft = fixture.replace("\"id\":881", "\"id\":882").replace("Isolated doctor fixture", "Hidden draft fixture")
            .replaceFirst("\"verification_status\":\"VERIFIED\"", "\"verification_status\":\"DRAFT\"")
        server.enqueue(response(page("$fixture,$draft")))
        val model = display()
        rule.waitUntil(10_000) { !model.state.value.loading }
        assertEquals("/api/care/doctors/", server.takeRequest(3, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath)
        rule.onNodeWithText("Hidden draft fixture").assertDoesNotExist()
        server.enqueue(response(fixture))
        rule.onNodeWithTag("doctor-card-881").performScrollTo().performClick()
        rule.waitUntil(10_000) { !model.state.value.profileLoading }
        val detail = server.takeRequest(3, TimeUnit.SECONDS)!!
        assertEquals("/api/care/doctors/881/", detail.requestUrl!!.encodedPath)
        assertNull(detail.getHeader("Authorization"))
        scroll("Registration: FIXTURE-881")
        rule.onNodeWithText("Registration: FIXTURE-881").assertIsDisplayed()
        rule.onNodeWithText("Issuing council: Isolated test council").assertIsDisplayed()
        screenshot("fixture-profile.png")
        server.enqueue(response(empty))
        scroll("Doctors at this hospital")
        rule.onNodeWithText("Doctors at this hospital").performClick()
        rule.waitUntil(10_000) { model.state.value.selectedId == null && !model.state.value.loading }
        assertEquals("997", server.takeRequest(3, TimeUnit.SECONDS)!!.requestUrl!!.queryParameter("hospital_id"))
        scroll("No verified doctors found. Try another name or specialty.")
        rule.onNodeWithText("No verified doctors found. Try another name or specialty.").assertIsDisplayed()
        screenshot("fixture-filter-empty.png")
    }

    @Test fun loadingSearchEmptyBackendErrorRetryAndRevokedDetail() {
        server.enqueue(response(empty).setBodyDelay(1, TimeUnit.SECONDS))
        val model = display()
        rule.onNodeWithText("Loading doctors…").assertExists()
        rule.waitUntil(10_000) { !model.state.value.loading }
        server.takeRequest(3, TimeUnit.SECONDS)
        rule.onNodeWithText("Doctor name or specialty").performTextInput("Fixture")
        rule.onNodeWithText("Specialty filter (optional)").performTextInput("Ayurveda")
        server.enqueue(response("""{"detail":"private server diagnostic"}""", 503))
        rule.onNodeWithText("Search doctors").performClick()
        rule.waitUntil(10_000) { model.state.value.error != null }
        val query = server.takeRequest(3, TimeUnit.SECONDS)!!.requestUrl!!
        assertEquals("Fixture", query.queryParameter("search"))
        assertEquals("Ayurveda", query.queryParameter("specialization"))
        scroll("Doctor directory is unavailable. Please retry.")
        rule.onNodeWithText("Doctor directory is unavailable. Please retry.").assertIsDisplayed()
        rule.onNodeWithText("private server diagnostic").assertDoesNotExist()
        server.enqueue(response(page()))
        scroll("Retry doctors")
        rule.onNodeWithText("Retry doctors").performClick()
        rule.waitUntil(10_000) { !model.state.value.loading }
        server.takeRequest(3, TimeUnit.SECONDS)
        server.enqueue(response("{}", 404))
        rule.onNodeWithTag("doctor-card-881").performScrollTo().performClick()
        rule.waitUntil(10_000) { !model.state.value.profileLoading }
        scroll("This doctor profile is no longer publicly available.")
        rule.onNodeWithText("This doctor profile is no longer publicly available.").assertIsDisplayed()
        rule.onNodeWithText("Registration: FIXTURE-881").assertDoesNotExist()
    }

    @Test fun offlineIsVisibleWithoutCachedCredentialsOrSecrets() {
        val url = server.url("/").toString()
        server.shutdown(); stopped = true
        val model = display(DoctorDirectoryViewModel(DoctorDirectoryRepository(CareDirectoryClient.create(url))))
        rule.waitUntil(15_000) { !model.state.value.loading }
        scroll("You appear to be offline. Check your connection and retry.")
        rule.onNodeWithText("You appear to be offline. Check your connection and retry.").assertIsDisplayed()
    }

    @Test fun communityCareCanBrowseDoctorsAndReturnToHospitalMap() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = response(empty)
        }
        val api = CareDirectoryClient.create(server.url("/").toString())
        val hospitals = CareDirectoryViewModel(CareDirectoryRepository(api))
        val doctors = DoctorDirectoryViewModel(DoctorDirectoryRepository(api))
        rule.setContent { VedAaharTheme { CommunityCareScreen(model = hospitals, doctorModel = doctors) } }
        rule.onNodeWithText("Browse verified doctors").performClick()
        rule.onNodeWithText("Verified Ayurvedic doctors").assertIsDisplayed()
        Espresso.pressBack()
        rule.onNodeWithTag("care-list").assertExists()
        rule.onNodeWithText("Community Care").assertIsDisplayed()
    }

    @Test fun liveDirectoryRemainsEmptyUntilGenuineHumanVerification() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("verifyLiveDoctorDirectory") == "true")
        val model = display(DoctorDirectoryViewModel())
        rule.waitUntil(40_000) { !model.state.value.loading }
        assertNull(model.state.value.error)
        assertEquals(0, model.state.value.count)
        scroll("No verified doctors found. Try another name or specialty.")
        rule.onNodeWithText("No verified doctors found. Try another name or specialty.").assertIsDisplayed()
        screenshot("live-directory-empty.png")
    }
}
