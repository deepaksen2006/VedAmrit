package com.example.vedaahar.care

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vedaahar.network.CareDirectoryApi
import com.example.vedaahar.network.CareDirectoryClient
import com.example.vedaahar.network.dto.CareHospitalDto
import com.example.vedaahar.network.dto.CarePage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class CarePoint(val latitude: Double, val longitude: Double) {
    val isValid: Boolean get() = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
}

/** Fail closed: missing approval metadata from an older server never creates a marker. */
fun CareHospitalDto.approvedPoint(): CarePoint? {
    if (verificationStatus != "VERIFIED" || locationStatus != "APPROVED") return null
    val point = CarePoint(latitude?.toDoubleOrNull() ?: return null,
        longitude?.toDoubleOrNull() ?: return null)
    return point.takeIf { it.isValid }
}

data class CareSearch(val text: String = "", val center: CarePoint? = null, val radiusKm: Double = 20.0)
data class CareDirectoryState(
    val search: CareSearch = CareSearch(),
    val hospitals: List<CareHospitalDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val count: Int = 0,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val selectedId: Long? = null
)

class CareDirectoryRepository(private val api: CareDirectoryApi = CareDirectoryClient.create()) {
    suspend fun page(search: CareSearch, page: Int): CarePage<CareHospitalDto> {
        val text = search.text.trim().take(200).ifBlank { null }
        val center = search.center
        val response = if (center == null) api.hospitals(search = text, page = page)
        else api.nearbyHospitals(center.latitude, center.longitude, search.radiusKm, text, page)
        if (!response.isSuccessful) throw HttpException(response)
        return response.body() ?: throw IOException("Empty directory response")
    }
}

class CareDirectoryViewModel(
    private val repository: CareDirectoryRepository = CareDirectoryRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(CareDirectoryState())
    val state = _state.asStateFlow()
    private var request: Job? = null
    private var generation = 0

    init { search(CareSearch()) }

    fun search(query: CareSearch) {
        if ((query.center != null && !query.center.isValid) || !query.radiusKm.isFinite() ||
            query.radiusKm !in 0.01..100.0) {
            request?.cancel()
            generation++
            _state.value = CareDirectoryState(error = "Enter valid coordinates and a radius from 0.01 to 100 km.")
            return
        }
        load(query.copy(text = query.text.trim().take(200)), append = false)
    }

    fun refresh() = load(_state.value.search, append = false)
    fun loadMore() {
        if (_state.value.hasMore && !_state.value.loading) load(_state.value.search, append = true)
    }
    fun select(id: Long) {
        if (_state.value.hospitals.any { it.id == id }) _state.value = _state.value.copy(selectedId = id)
    }

    private fun load(query: CareSearch, append: Boolean) {
        request?.cancel()
        val version = ++generation
        val previous = _state.value
        val page = if (append) previous.page + 1 else 1
        _state.value = if (append) previous.copy(loading = true, error = null)
            else CareDirectoryState(search = query, loading = true)
        request = viewModelScope.launch {
            try {
                val result = repository.page(query, page)
                if (version != generation) return@launch
                val publicRows = result.results.filter { it.verificationStatus == "VERIFIED" }
                    .filter { query.center == null || it.approvedPoint() != null }
                val rows = ((if (append) previous.hospitals else emptyList()) + publicRows).distinctBy { it.id }
                _state.value = CareDirectoryState(query, rows, count = result.count, page = page,
                    hasMore = result.next != null,
                    selectedId = previous.selectedId?.takeIf { id -> rows.any { it.id == id } })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (version != generation) return@launch
                // Never display response bodies, URLs, tokens or provider credentials.
                val message = when (error) {
                    is HttpException -> when (error.code()) {
                        400 -> "Search was rejected. Check the coordinates and radius."
                        401, 403 -> "The public directory is unavailable. Please try again later."
                        429 -> "Too many searches. Please wait and retry."
                        else -> "Hospital directory is unavailable. Please retry."
                    }
                    is IOException -> "Could not reach the hospital directory. Check your connection and retry."
                    else -> "Could not read hospital results. Please retry."
                }
                _state.value = _state.value.copy(loading = false, error = message)
            }
        }
    }
}
