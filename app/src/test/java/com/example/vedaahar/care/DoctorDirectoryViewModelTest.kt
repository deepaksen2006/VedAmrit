package com.example.vedaahar.care

import com.example.vedaahar.network.CareDirectoryApi
import com.example.vedaahar.network.dto.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DoctorDirectoryViewModelTest {
    private val api = DoctorApiFake()
    @Before fun before() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun after() { Dispatchers.resetMain() }
    private fun model() = DoctorDirectoryViewModel(DoctorDirectoryRepository(api))

    @Test fun unreviewedDraftAndHiddenHospitalRowsFailClosed() {
        val valid = fixtureDoctor()
        api.respond = { _, _ -> page(listOf(valid, valid.copy(id = 2, verificationStatus = "DRAFT"),
            valid.copy(id = 3, credentialsReviewedAt = null), valid.copy(id = 4, qualifications = emptyList()),
            valid.copy(id = 5, affiliations = valid.affiliations.map { it.copy(verificationStatus = "DRAFT") }))) }
        assertEquals(listOf(1L), model().state.value.doctors.map { it.id })
        assertNull(valid.copy(registrationCouncil = "").publicProfile())
        val mixed = valid.copy(affiliations = valid.affiliations + valid.affiliations[0].copy(hospitalId = 2, verificationStatus = "DRAFT"))
        assertEquals(1, mixed.publicProfile()!!.affiliations.size)
    }

    @Test fun loadingEmptyOfflineAndBackendErrorAreDistinctAndRetryRecovers() {
        val gate = CompletableDeferred<Unit>()
        api.respond = { _, _ -> gate.await(); page(emptyList()) }
        val model = model()
        assertTrue(model.state.value.loading)
        gate.complete(Unit)
        assertFalse(model.state.value.loading)
        assertNull(model.state.value.error)
        api.respond = { _, _ -> throw IOException("private URL or body") }
        model.refresh()
        assertTrue(model.state.value.error!!.contains("offline"))
        api.respond = { _, _ -> Response.error(503, "secret".toResponseBody()) }
        model.refresh()
        assertEquals("Doctor directory is unavailable. Please retry.", model.state.value.error)
        api.respond = { _, _ -> page(emptyList()) }
        model.refresh()
        assertNull(model.state.value.error)
    }

    @Test fun normalizedFiltersPaginationDeduplicationAndFailedPageRetryUseFixedApi() {
        api.respond = { _, page -> if (page == 1) page(listOf(fixtureDoctor()), "https://untrusted.test/page")
            else page(listOf(fixtureDoctor(), fixtureDoctor().copy(id = 2))) }
        val model = model()
        model.search(DoctorSearch("  Test  ", "  Specialty  ", 7, "Hospital"))
        assertEquals(DoctorSearch("Test", "Specialty", 7), api.calls.last().first)
        api.respond = { _, _ -> throw IOException() }
        model.loadMore()
        assertEquals(1, model.state.value.page)
        api.respond = { _, _ -> page(listOf(fixtureDoctor(), fixtureDoctor().copy(id = 2))) }
        model.loadMore()
        assertEquals(2, api.calls.last().second)
        assertEquals(listOf(1L, 2L), model.state.value.doctors.map { it.id })
        assertFalse(model.state.value.hasMore)
    }

    @Test fun detailIsFetchedAndRevocationClearsPreviouslyVisibleCredentials() {
        val model = model()
        model.openProfile(1)
        assertEquals(1L, api.profileCalls.last())
        assertNotNull(model.state.value.profile)
        api.detail = { Response.error(404, "not public".toResponseBody()) }
        model.refresh()
        assertNull(model.state.value.profile)
        assertEquals("This doctor profile is no longer publicly available.", model.state.value.profileError)
    }

    @Test fun malformedDetailAndInactiveMetadataDoNotFallBackToList() {
        val model = model()
        api.detail = { Response.success(fixtureDoctor().copy(verificationStatus = "PENDING")) }
        model.openProfile(1)
        assertNull(model.state.value.profile)
        assertNotNull(model.state.value.profileError)
        api.detail = { Response.success(fixtureDoctor().copy(id = 99)) }
        model.refresh()
        assertNull(model.state.value.profile)
    }

    @Test fun returningToDirectoryRevalidatesRatherThanRestoringStaleCards() {
        api.respond = { _, _ -> page(listOf(fixtureDoctor())) }
        val model = model()
        model.openProfile(1)
        api.respond = { _, _ -> page(emptyList()) }
        model.directory()
        assertNull(model.state.value.selectedId)
        assertTrue(model.state.value.doctors.isEmpty())
    }

    @Test fun lateCancelledResponseCannotRestoreOldSearchOrProfile() = runTest {
        val gate = CompletableDeferred<Unit>()
        api.respond = { _, _ -> withContext(NonCancellable) { gate.await() }; page(listOf(fixtureDoctor())) }
        val model = model()
        api.respond = { _, _ -> page(emptyList()) }
        model.search(DoctorSearch("new search"))
        gate.complete(Unit)
        runCurrent()
        assertTrue(model.state.value.doctors.isEmpty())
        assertEquals("new search", model.state.value.search.text)
    }

    private fun page(rows: List<CareDoctorDto>, next: String? = null) = Response.success(CarePage(rows.size, next, null, rows))
}

private fun fixtureDoctor() = CareDoctorDto(1, "Isolated doctor fixture", listOf("Fixture qualification"), "Fixture specialty",
    "TEST-001", "Fixture council", 2, "Fixture biography", "VERIFIED", null,
    listOf(CareAffiliationDto(1, 7, "Isolated hospital fixture", "Fixture city", "IN_PERSON", "0.00", "VERIFIED")),
    "2026-10-27T00:00:00Z")

private class DoctorApiFake : CareDirectoryApi {
    val calls = mutableListOf<Pair<DoctorSearch, Int>>()
    val profileCalls = mutableListOf<Long>()
    var respond: suspend (DoctorSearch, Int) -> Response<CarePage<CareDoctorDto>> = { _, _ -> Response.success(CarePage(0, null, null, emptyList())) }
    var detail: suspend (Long) -> Response<CareDoctorDto> = { Response.success(fixtureDoctor()) }
    override suspend fun doctors(city: String?, search: String?, state: String?, specialization: String?, hospitalId: Long?, page: Int, pageSize: Int): Response<CarePage<CareDoctorDto>> {
        val query = DoctorSearch(search.orEmpty(), specialization.orEmpty(), hospitalId)
        calls += query to page
        return respond(query, page)
    }
    override suspend fun doctor(id: Long): Response<CareDoctorDto> { profileCalls += id; return detail(id) }
    override suspend fun hospitals(city: String?, search: String?, state: String?, page: Int, pageSize: Int): Response<CarePage<CareHospitalDto>> = error("Unused")
    override suspend fun nearbyHospitals(latitude: Double, longitude: Double, radiusKm: Double, search: String?, page: Int, pageSize: Int): Response<CarePage<CareHospitalDto>> = error("Unused")
    override suspend fun hospital(id: Long): Response<CareHospitalDto> = error("Unused")
}
