package com.example.vedaahar.care

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vedaahar.network.CareDirectoryApi
import com.example.vedaahar.network.CareDirectoryClient
import com.example.vedaahar.network.dto.CareDoctorDto
import com.example.vedaahar.network.dto.CarePage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** Older/incomplete responses fail closed; association never supplies doctor approval. */
fun CareDoctorDto.publicProfile(): CareDoctorDto? {
    if (id <= 0 || verificationStatus != "VERIFIED" || credentialsReviewedAt.isNullOrBlank() ||
        fullName.isBlank() || registrationNumber.isBlank() || registrationCouncil.isBlank() ||
        qualifications.isEmpty() || qualifications.any { it.isBlank() }) return null
    val eligible = affiliations.filter { it.verificationStatus == "VERIFIED" && it.hospitalId > 0 && it.hospitalName.isNotBlank() }
    return if (eligible.isEmpty()) null else copy(affiliations = eligible)
}

data class DoctorSearch(val text: String = "", val specialty: String = "", val hospitalId: Long? = null, val hospitalName: String = "")
data class DoctorDirectoryState(
    val search: DoctorSearch = DoctorSearch(), val doctors: List<CareDoctorDto> = emptyList(),
    val loading: Boolean = false, val error: String? = null, val count: Int = 0,
    val page: Int = 0, val hasMore: Boolean = false,
    val selectedId: Long? = null, val profile: CareDoctorDto? = null,
    val profileLoading: Boolean = false, val profileError: String? = null
)

class DoctorDirectoryRepository(private val api: CareDirectoryApi = CareDirectoryClient.create()) {
    private fun <T> Response<T>.requiredBody(): T {
        if (!isSuccessful) throw HttpException(this)
        return body() ?: throw IOException("Empty public doctor response")
    }
    suspend fun page(search: DoctorSearch, page: Int): CarePage<CareDoctorDto> = api.doctors(
        search = search.text.ifBlank { null }, specialization = search.specialty.ifBlank { null },
        hospitalId = search.hospitalId, page = page).requiredBody()
    suspend fun profile(id: Long): CareDoctorDto = api.doctor(id).requiredBody()
}

class DoctorDirectoryViewModel(private val repository: DoctorDirectoryRepository = DoctorDirectoryRepository()) : ViewModel() {
    private val _state = MutableStateFlow(DoctorDirectoryState())
    val state = _state.asStateFlow()
    private var request: Job? = null
    private var generation = 0
    init { search(DoctorSearch()) }

    fun search(query: DoctorSearch) {
        if (query.hospitalId != null && query.hospitalId <= 0) return
        load(query.copy(text = query.text.trim().take(200), specialty = query.specialty.trim().take(140)), false)
    }
    fun refresh() { state.value.selectedId?.let { openProfile(it) } ?: load(state.value.search, false) }
    fun directory() = load(state.value.search, false)
    fun loadMore() { if (!state.value.loading && state.value.hasMore) load(state.value.search, true) }

    private fun load(query: DoctorSearch, append: Boolean) {
        request?.cancel()
        val version = ++generation
        val previous = state.value
        val page = if (append) previous.page + 1 else 1
        _state.value = if (append) previous.copy(loading = true, error = null)
            else DoctorDirectoryState(search = query, loading = true)
        request = viewModelScope.launch {
            try {
                val result = repository.page(query, page)
                val publicRows = result.results.mapNotNull { it.publicProfile() }
                if (version != generation) return@launch
                _state.value = DoctorDirectoryState(search = query,
                    doctors = ((if (append) previous.doctors else emptyList()) + publicRows).distinctBy { it.id },
                    count = result.count, page = page, hasMore = result.next != null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (version == generation) _state.value = state.value.copy(loading = false, error = message(error))
            }
        }
    }

    fun openProfile(id: Long) {
        request?.cancel()
        val version = ++generation
        _state.value = state.value.copy(selectedId = id, profile = null, profileLoading = true, profileError = null, loading = false)
        request = viewModelScope.launch {
            try {
                val profile = repository.profile(id).takeIf { it.id == id }?.publicProfile()
                if (version != generation) return@launch
                _state.value = state.value.copy(profile = profile, profileLoading = false,
                    profileError = if (profile == null) "This doctor profile is no longer publicly available." else null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (version == generation) _state.value = state.value.copy(profile = null, profileLoading = false, profileError = message(error))
            }
        }
    }

    private fun message(error: Exception): String = when (error) {
        is HttpException -> when (error.code()) {
            404 -> "This doctor profile is no longer publicly available."
            400 -> "The doctor search was rejected. Check the filters and retry."
            429 -> "Too many searches. Please wait and retry."
            else -> "Doctor directory is unavailable. Please retry."
        }
        is IOException -> "You appear to be offline. Check your connection and retry."
        else -> "Could not read doctor results. Please retry."
    }
}
