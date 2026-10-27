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
class CareDirectoryViewModelTest {
    private val api = FakeCareApi()
    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }
    private fun model() = CareDirectoryViewModel(CareDirectoryRepository(api))

    @Test fun draftIdentityWithApprovedLocationNeverBecomesPublic() {
        api.respond = { _, _ -> page(listOf(hospital(1).copy(verificationStatus = "DRAFT"), hospital(2))) }
        val model = model()
        assertEquals(listOf(2L), model.state.value.hospitals.map { it.id })
        assertNull(hospital(1).copy(verificationStatus = "DRAFT").approvedPoint())
        assertNull(hospital(1).copy(verificationStatus = "SUSPENDED").approvedPoint())
    }

    @Test fun missingApprovalInvalidCoordinatesAndNonfiniteNumbersNeverCreateMarkers() {
        val hospital = hospital(1)
        assertNotNull(hospital.approvedPoint())
        for (status in listOf(null, "UNVERIFIED", "STALE", "REJECTED")) assertNull(hospital.copy(locationStatus = status).approvedPoint())
        for ((lat, lon) in listOf(null to "73", "24" to null, "NaN" to "73", "24" to "Infinity", "91" to "73", "24" to "181", "bad" to "73")) {
            assertNull(hospital.copy(latitude = lat, longitude = lon).approvedPoint())
        }
        assertEquals(CarePoint(0.0, 0.0), hospital.copy(latitude = "0", longitude = "0").approvedPoint())
    }

    @Test fun directoryCanListVerifiedHospitalWithoutLocationButNearbyCannot() {
        api.respond = { _, _ -> page(listOf(hospital(1).copy(locationStatus = null), hospital(2))) }
        val model = model()
        assertEquals(2, model.state.value.hospitals.size)
        model.search(CareSearch("test", CarePoint(24.0, 73.0), 25.0))
        assertEquals(listOf(2L), model.state.value.hospitals.map { it.id })
        assertEquals(25.0, api.calls.last().first.radiusKm, 0.0)
    }

    @Test fun loadingEmptyAndNetworkErrorRemainDistinctAndRetryRecovers() = runTest {
        val gate = CompletableDeferred<Unit>()
        api.respond = { _, _ -> gate.await(); page(emptyList()) }
        val model = model()
        assertTrue(model.state.value.loading)
        assertNull(model.state.value.error)
        gate.complete(Unit)
        assertFalse(model.state.value.loading)
        assertTrue(model.state.value.hospitals.isEmpty())
        assertNull(model.state.value.error)
        api.respond = { _, _ -> throw IOException("secret URL must not appear") }
        model.refresh()
        assertFalse(model.state.value.loading)
        assertNotNull(model.state.value.error)
        assertFalse(model.state.value.error!!.contains("secret"))
        api.respond = { _, _ -> page(listOf(hospital(3))) }
        model.refresh()
        assertNull(model.state.value.error)
        assertEquals(3L, model.state.value.hospitals.single().id)
    }

    @Test fun httpFailuresNeverTurnIntoEmptySuccessOrKeepPreviousQueryMarkers() {
        api.respond = { _, _ -> page(listOf(hospital(1))) }
        val model = model()
        for (code in listOf(400, 401, 403, 429, 500)) {
            api.respond = { _, _ -> Response.error(code, "private backend body".toResponseBody()) }
            model.search(CareSearch("new query $code"))
            assertTrue(model.state.value.hospitals.isEmpty())
            assertNotNull(model.state.value.error)
            assertFalse(model.state.value.error!!.contains("private"))
        }
    }

    @Test fun paginationDeduplicatesAndRetriesSameFailedPage() {
        api.respond = { _, number -> if (number == 1) page(listOf(hospital(1)), "https://untrusted.invalid/next")
            else page(listOf(hospital(1), hospital(2))) }
        val model = model()
        model.select(1)
        model.loadMore()
        assertEquals(listOf(1L, 2L), model.state.value.hospitals.map { it.id })
        assertEquals(1L, model.state.value.selectedId)
        assertFalse(model.state.value.hasMore)
        model.search(CareSearch("fresh"))
        api.respond = { _, _ -> throw IOException() }
        model.loadMore()
        assertEquals(1, model.state.value.page)
        assertEquals(1, model.state.value.hospitals.size)
        api.respond = { _, _ -> page(listOf(hospital(2))) }
        model.loadMore()
        assertEquals(2, api.calls.last().second)
        assertNull(model.state.value.error)
    }

    @Test fun newerSearchWinsEvenWhenOldRequestDoesNotCooperateWithCancellation() = runTest {
        val gate = CompletableDeferred<Unit>()
        api.respond = { query, _ ->
            if (query.text.isEmpty()) withContext(NonCancellable) { gate.await() }
            page(listOf(hospital(if (query.text.isEmpty()) 1 else 2)))
        }
        val model = model()
        model.search(CareSearch("new"))
        assertEquals(2L, model.state.value.hospitals.single().id)
        gate.complete(Unit)
        assertEquals(2L, model.state.value.hospitals.single().id)
    }

    @Test fun invalidRadiusAndLocationNeverCallApiAndSearchCanReturnToDirectory() {
        val model = model()
        val initial = api.calls.size
        for (query in listOf(CareSearch(center = CarePoint(Double.NaN, 73.0)), CareSearch(radiusKm = 0.0),
            CareSearch(radiusKm = 101.0), CareSearch(radiusKm = Double.POSITIVE_INFINITY))) {
            model.search(query)
            assertNotNull(model.state.value.error)
        }
        assertEquals(initial, api.calls.size)
        model.search(CareSearch("  Udaipur  "))
        assertNull(api.calls.last().first.center)
        assertEquals("Udaipur", api.calls.last().first.text)
    }

    private fun hospital(id: Long) = CareHospitalDto(id, "Isolated test hospital $id", "", "Fixture Street", "Udaipur", "Rajasthan",
        "313001", "24.5854", "73.7125", "", "", emptyList(), "VERIFIED", null, "APPROVED")
    private fun page(rows: List<CareHospitalDto>, next: String? = null) = Response.success(CarePage(2, next, null, rows))
}

private class FakeCareApi : CareDirectoryApi {
    val calls = mutableListOf<Pair<CareSearch, Int>>()
    var respond: suspend (CareSearch, Int) -> Response<CarePage<CareHospitalDto>> = { _, _ -> Response.success(CarePage(0, null, null, emptyList())) }
    private suspend fun result(query: CareSearch, page: Int): Response<CarePage<CareHospitalDto>> {
        calls += query to page
        return respond(query, page)
    }
    override suspend fun hospitals(city: String?, search: String?, state: String?, page: Int, pageSize: Int) = result(CareSearch(search.orEmpty()), page)
    override suspend fun nearbyHospitals(latitude: Double, longitude: Double, radiusKm: Double, search: String?, page: Int, pageSize: Int) =
        result(CareSearch(search.orEmpty(), CarePoint(latitude, longitude), radiusKm), page)
    override suspend fun hospital(id: Long): Response<CareHospitalDto> = error("Unused")
    override suspend fun doctors(city: String?, search: String?, state: String?, specialization: String?, hospitalId: Long?, page: Int, pageSize: Int): Response<CarePage<CareDoctorDto>> = error("Unused")
    override suspend fun doctor(id: Long): Response<CareDoctorDto> = error("Unused")
}
