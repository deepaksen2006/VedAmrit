package com.example.vedaahar.profile

import android.content.Context
import com.example.vedaahar.network.PatientSession
import com.example.vedaahar.agentic.AgenticApiErrorType
import com.example.vedaahar.agentic.AgenticApiResult
import com.example.vedaahar.network.ProfileApi
import com.example.vedaahar.network.dto.PatientProfileDto
import com.google.gson.JsonSyntaxException
import java.io.IOException
import java.net.SocketTimeoutException
import retrofit2.Response

interface PatientProfileGateway {
    suspend fun loadProfile(): PatientProfileLoadResult
    suspend fun saveProfile(profile: StoredPatientProfile): AgenticApiResult<StoredPatientProfile>
}

sealed class PatientProfileLoadResult {
    data class Remote(val profile: StoredPatientProfile) : PatientProfileLoadResult()
    data class Cache(val profile: StoredPatientProfile, val message: String) : PatientProfileLoadResult()
    data class Error(val type: AgenticApiErrorType, val message: String) : PatientProfileLoadResult()
}

class PatientProfileRepository(
    private val api: ProfileApi,
    private val accessTokenProvider: () -> String?,
    private val cache: PatientProfileCache
) : PatientProfileGateway {
    constructor(
        api: ProfileApi,
        accessTokenProvider: () -> String?,
        context: Context
    ) : this(
        api = api,
        accessTokenProvider = accessTokenProvider,
        cache = SharedPreferencesPatientProfileCache(context)
    )

    override suspend fun loadProfile(): PatientProfileLoadResult {
        if (accessTokenProvider().isNullOrBlank()) {
            return PatientProfileLoadResult.Error(
                AgenticApiErrorType.AuthenticationRequired,
                "Sign in to sync your profile."
            )
        }
        return try {
            val response = api.profile()
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return cachedLoad("The server returned an empty profile.")
                val profile = body.toStoredProfile()
                cache.save(profile)
                PatientProfileLoadResult.Remote(profile)
            } else {
                if (response.code() in setOf(401, 403)) {
                    PatientProfileLoadResult.Error(response.errorType(), response.errorMessage())
                } else {
                    cachedLoad(response.errorMessage())
                }
            }
        } catch (_: SocketTimeoutException) {
            cachedLoad("Profile sync timed out. Showing your saved local copy.")
        } catch (_: IOException) {
            cachedLoad("Network is unavailable. Showing your saved local copy.")
        } catch (_: JsonSyntaxException) {
            PatientProfileLoadResult.Error(
                AgenticApiErrorType.Parse,
                "The server profile could not be read."
            )
        }
    }

    override suspend fun saveProfile(profile: StoredPatientProfile): AgenticApiResult<StoredPatientProfile> {
        if (accessTokenProvider().isNullOrBlank()) {
            return AgenticApiResult.Error(
                AgenticApiErrorType.AuthenticationRequired,
                "Sign in before saving your health profile."
            )
        }
        return try {
            val response = api.putProfile(profile.toDto())
            if (response.isSuccessful) {
                val saved = response.body()?.toStoredProfile() ?: return AgenticApiResult.Error(
                    AgenticApiErrorType.Parse,
                    "The server returned an empty saved profile. Please retry syncing your profile."
                )
                cache.save(saved)
                AgenticApiResult.Success(saved)
            } else {
                AgenticApiResult.Error(response.errorType(), response.errorMessage())
            }
        } catch (_: SocketTimeoutException) {
            AgenticApiResult.Error(AgenticApiErrorType.Timeout, "Profile save timed out. Please try again.")
        } catch (_: IOException) {
            AgenticApiResult.Error(AgenticApiErrorType.Network, "Network is unavailable. Your profile was not synced.")
        } catch (_: JsonSyntaxException) {
            AgenticApiResult.Error(AgenticApiErrorType.Parse, "The saved profile response could not be read.")
        }
    }

    private fun cachedLoad(message: String): PatientProfileLoadResult.Cache {
        return PatientProfileLoadResult.Cache(cache.current(), message)
    }
}

class LocalPatientProfileRepository(
    private val cache: PatientProfileCache
) : PatientProfileGateway {
    constructor(context: Context) : this(SharedPreferencesPatientProfileCache(context))

    override suspend fun loadProfile(): PatientProfileLoadResult {
        return PatientProfileLoadResult.Cache(
            cache.current(),
            "Local development profile loaded."
        )
    }

    override suspend fun saveProfile(profile: StoredPatientProfile): AgenticApiResult<StoredPatientProfile> {
        val localProfile = profile.withEmptyLists()
        cache.save(localProfile)
        return AgenticApiResult.Success(localProfile)
    }
}

interface PatientProfileCache {
    fun current(): StoredPatientProfile
    fun save(profile: StoredPatientProfile)
}

class SharedPreferencesPatientProfileCache(context: Context) : PatientProfileCache {
    private val appContext = context.applicationContext
    private val owner = PatientSession.currentKey(context)

    override fun current(): StoredPatientProfile = PatientProfileStore.current(appContext, owner)

    override fun save(profile: StoredPatientProfile) {
        PatientProfileStore.save(appContext, profile, owner)
    }
}

fun StoredPatientProfile.toDto(): PatientProfileDto {
    return PatientProfileDto(
        name = fullName.trim(),
        age = age.trim().toIntOrNull(),
        gender = gender.trim().lowercase(),
        heightCm = heightCm.trim(),
        weightKg = weightKg.trim(),
        dietaryPreference = dietaryPreference.trim().lowercase(),
        goal = goal.trim().lowercase(),
        activityLevel = activityLevel.trim().lowercase(),
        allergies = allergies,
        foodIntolerances = foodIntolerances,
        foodsToAvoid = foodsToAvoid,
        healthConditions = healthConditions,
        currentMedicines = currentMedicines,
        medicineAllergies = medicineAllergies,
        medicationRestrictions = medicationRestrictions,
        doctorRestrictions = doctorRestrictions,
        pregnancyInformation = pregnancyInformation.trim()
    )
}

fun PatientProfileDto.toStoredProfile(): StoredPatientProfile {
    return StoredPatientProfile(
        profileVersion = profileVersion,
        fullName = name.orEmpty(),
        age = age?.toString().orEmpty(),
        gender = gender.toDisplayGender(),
        heightCm = heightCm.orEmpty(),
        weightKg = weightKg.orEmpty(),
        dietaryPreference = dietaryPreference.orEmpty(),
        goal = goal.orEmpty(),
        activityLevel = activityLevel.orEmpty(),
        allergies = allergies,
        foodIntolerances = foodIntolerances ?: intolerances,
        foodsToAvoid = foodsToAvoid,
        healthConditions = healthConditions,
        currentMedicines = currentMedicines,
        medicineAllergies = medicineAllergies,
        medicationRestrictions = medicationRestrictions,
        doctorRestrictions = doctorRestrictions,
        pregnancyInformation = pregnancyInformation ?: pregnancyStatus.orEmpty()
    )
}

private fun StoredPatientProfile.withEmptyLists(): StoredPatientProfile {
    return copy(
        allergies = allergies.orEmpty(),
        foodIntolerances = foodIntolerances.orEmpty(),
        foodsToAvoid = foodsToAvoid.orEmpty(),
        healthConditions = healthConditions.orEmpty(),
        medicineAllergies = medicineAllergies.orEmpty(),
        currentMedicines = currentMedicines.orEmpty(),
        medicationRestrictions = medicationRestrictions.orEmpty(),
        doctorRestrictions = doctorRestrictions.orEmpty()
    )
}

private fun String?.toDisplayGender(): String {
    return when (this?.trim()?.lowercase()) {
        "male" -> "Male"
        "female" -> "Female"
        "other" -> "Other"
        else -> this.orEmpty()
    }
}

private fun <T> Response<T>.errorType(): AgenticApiErrorType {
    return when (code()) {
        400 -> AgenticApiErrorType.BadRequest
        401 -> AgenticApiErrorType.AuthenticationExpired
        403 -> AgenticApiErrorType.AuthorizationDenied
        404 -> AgenticApiErrorType.NotFound
        in 500..599 -> AgenticApiErrorType.BackendFailure
        else -> AgenticApiErrorType.Unknown
    }
}

private fun <T> Response<T>.errorMessage(): String {
    return when (code()) {
        400 -> "The profile has missing or invalid fields."
        401 -> "Your session expired. Please sign in again."
        403 -> "You are not allowed to update this profile."
        404 -> "The profile endpoint was not found."
        in 500..599 -> "The backend could not save your profile right now."
        else -> "The profile request failed with HTTP ${code()}."
    }
}
