package com.example.vedaahar.profile

import com.example.vedaahar.agentic.AgenticApiErrorType
import com.example.vedaahar.agentic.AgenticApiResult
import com.example.vedaahar.network.ProfileApi
import com.example.vedaahar.network.dto.PatientProfileDto
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class PatientProfileRepositoryTest {
    @Test
    fun loadProfileFetchesRemoteProfileAndCachesIt() = runTest {
        val cache = FakeProfileCache()
        val api = FakeProfileApi(profileResponse = Response.success(remoteProfile()))
        val repository = PatientProfileRepository(api, accessTokenProvider = { "jwt" }, cache = cache)

        val result = repository.loadProfile()

        assertTrue(result is PatientProfileLoadResult.Remote)
        assertEquals("Anaya Rao", (result as PatientProfileLoadResult.Remote).profile.fullName)
        assertEquals("Anaya Rao", cache.saved?.fullName)
        assertEquals(1, api.profileCalls)
    }

    @Test
    fun loadProfileFallsBackToCacheWhenNetworkFails() = runTest {
        val repository = PatientProfileRepository(
            FakeProfileApi(profileError = IOException("offline")),
            accessTokenProvider = { "jwt" },
            cache = FakeProfileCache(storedProfile())
        )

        val result = repository.loadProfile()

        assertTrue(result is PatientProfileLoadResult.Cache)
    }

    @Test
    fun missingTokenIsAnAuthenticationErrorAndDoesNotLoadCachedProfile() = runTest {
        val api = FakeProfileApi()
        val repository = PatientProfileRepository(api, { null }, FakeProfileCache(storedProfile()))
        val result = repository.loadProfile() as PatientProfileLoadResult.Error
        assertEquals(AgenticApiErrorType.AuthenticationRequired, result.type)
        assertEquals(0, api.profileCalls)
    }

    @Test
    fun expiredSessionIsAnErrorInsteadOfSuccessfulCacheFallback() = runTest {
        val api = FakeProfileApi(profileResponse = Response.error(401, "{}".toResponseBody(null)))
        val repository = PatientProfileRepository(api, { "expired" }, FakeProfileCache(storedProfile()))
        val result = repository.loadProfile() as PatientProfileLoadResult.Error
        assertEquals(AgenticApiErrorType.AuthenticationExpired, result.type)
        assertTrue(result.message.contains("sign in", ignoreCase = true))
    }

    @Test
    fun saveProfileRequiresJwtAndDoesNotCallApiWhenMissing() = runTest {
        val api = FakeProfileApi()
        val repository = PatientProfileRepository(api, accessTokenProvider = { null }, cache = FakeProfileCache())

        val result = repository.saveProfile(storedProfile())

        assertTrue(result is AgenticApiResult.Error)
        assertEquals(AgenticApiErrorType.AuthenticationRequired, (result as AgenticApiResult.Error).type)
        assertEquals(0, api.putCalls)
    }

    @Test
    fun saveProfilePutsProfileToDjangoAndReturnsSavedProfile() = runTest {
        val api = FakeProfileApi(putResponse = Response.success(remoteProfile().copy(name = "Saved Anaya")))
        val cache = FakeProfileCache()
        val repository = PatientProfileRepository(api, accessTokenProvider = { "jwt" }, cache = cache)

        val result = repository.saveProfile(storedProfile())

        assertTrue(result is AgenticApiResult.Success)
        assertEquals("Saved Anaya", (result as AgenticApiResult.Success).value.fullName)
        assertEquals(1, api.putCalls)
        assertEquals("Saved Anaya", cache.saved?.fullName)
        assertEquals("vegetarian", api.lastPut?.dietaryPreference)
        assertEquals("not_pregnant", api.lastPut?.pregnancyInformation)
    }

    @Test
    fun saveProfileMapsBadRequest() = runTest {
        val api = FakeProfileApi(putResponse = Response.error(400, "{}".toResponseBody(null)))
        val repository = PatientProfileRepository(api, accessTokenProvider = { "jwt" }, cache = FakeProfileCache())

        val result = repository.saveProfile(storedProfile())

        assertTrue(result is AgenticApiResult.Error)
        assertEquals(AgenticApiErrorType.BadRequest, (result as AgenticApiResult.Error).type)
    }

    @Test
    fun emptySaveResponseIsNotReportedAsSynced() = runTest {
        val cache = FakeProfileCache()
        val api = FakeProfileApi(putResponse = Response.success(null))
        val result = PatientProfileRepository(api, { "jwt" }, cache).saveProfile(storedProfile()) as AgenticApiResult.Error
        assertEquals(AgenticApiErrorType.Parse, result.type)
        org.junit.Assert.assertNull(cache.saved)
    }

    @Test
    fun expiredTokenOnSaveIsVisibleAndDoesNotUpdateCache() = runTest {
        val cache = FakeProfileCache()
        val api = FakeProfileApi(putResponse = Response.error(401, "{}".toResponseBody(null)))
        val result = PatientProfileRepository(api, { "expired" }, cache).saveProfile(storedProfile()) as AgenticApiResult.Error
        assertEquals(AgenticApiErrorType.AuthenticationExpired, result.type)
        org.junit.Assert.assertNull(cache.saved)
    }

    private fun remoteProfile() = PatientProfileDto(
        name = "Anaya Rao",
        age = 34,
        gender = "female",
        heightCm = "164.00",
        weightKg = "58.00",
        dietaryPreference = "vegetarian",
        goal = "general wellness",
        activityLevel = "moderate",
        allergies = emptyList(),
        intolerances = listOf("lactose"),
        foodsToAvoid = emptyList(),
        healthConditions = emptyList(),
        currentMedicines = emptyList(),
        medicineAllergies = emptyList(),
        medicationRestrictions = emptyList(),
        doctorRestrictions = emptyList(),
        pregnancyStatus = "not_pregnant"
    )

    private fun storedProfile() = StoredPatientProfile(
        fullName = "Anaya Rao",
        age = "34",
        gender = "Female",
        heightCm = "164",
        weightKg = "58",
        dietaryPreference = "vegetarian",
        goal = "general wellness",
        activityLevel = "moderate",
        allergies = emptyList(),
        foodIntolerances = emptyList(),
        foodsToAvoid = emptyList(),
        healthConditions = emptyList(),
        currentMedicines = emptyList(),
        medicineAllergies = emptyList(),
        medicationRestrictions = emptyList(),
        doctorRestrictions = emptyList(),
        pregnancyInformation = "not_pregnant"
    )

    private class FakeProfileApi(
        private val profileResponse: Response<PatientProfileDto> = Response.success(remoteProfileStatic()),
        private val putResponse: Response<PatientProfileDto> = Response.success(remoteProfileStatic()),
        private val profileError: IOException? = null
    ) : ProfileApi {
        var profileCalls = 0
            private set
        var putCalls = 0
            private set
        var lastPut: PatientProfileDto? = null
            private set

        override suspend fun profile(): Response<PatientProfileDto> {
            profileCalls += 1
            profileError?.let { throw it }
            return profileResponse
        }

        override suspend fun putProfile(profile: PatientProfileDto): Response<PatientProfileDto> {
            putCalls += 1
            lastPut = profile
            return putResponse
        }

        override suspend fun patchProfile(profile: PatientProfileDto): Response<PatientProfileDto> {
            throw NotImplementedError()
        }

        companion object {
            private fun remoteProfileStatic() = PatientProfileDto(name = "Anaya Rao")
        }
    }

    private class FakeProfileCache(
        private val current: StoredPatientProfile = StoredPatientProfile("", "", "", "", "")
    ) : PatientProfileCache {
        var saved: StoredPatientProfile? = null
            private set

        override fun current(): StoredPatientProfile = current

        override fun save(profile: StoredPatientProfile) {
            saved = profile
        }
    }
}
