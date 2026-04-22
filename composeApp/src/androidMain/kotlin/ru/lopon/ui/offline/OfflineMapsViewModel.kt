package ru.lopon.ui.offline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import ru.lopon.di.AndroidAppContainer
import ru.lopon.platform.DownloadProgress
import ru.lopon.platform.MapLibreOfflineHelper
import ru.lopon.platform.OfflineRegionInfo

@Serializable
data class NominatimPlace(
    val displayName: String,
    val boundingBox: List<String>
)

data class SearchResult(
    val name: String,
    val swLat: Double,
    val swLng: Double,
    val neLat: Double,
    val neLng: Double,
    val estimatedSizeBytes: Long = 0
)

data class OfflineMapsUiState(
    val regions: List<OfflineRegionInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isDownloading: Boolean = false,
    val downloadProgress: DownloadProgress? = null,
    val downloadRegionName: String = "",
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null
)

class OfflineMapsViewModel(
    application: Application,
    private val container: AndroidAppContainer
) : AndroidViewModel(application) {

    private val offlineHelper = MapLibreOfflineHelper(application.applicationContext)
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient(OkHttp)
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(OfflineMapsUiState())
    val uiState: StateFlow<OfflineMapsUiState> = _uiState.asStateFlow()

    init {
        loadRegions()
    }

    fun loadRegions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val regions = offlineHelper.listRegions()
                _uiState.update { it.copy(regions = regions, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Ошибка загрузки: ${e.message}")
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun searchRegion() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchResults = emptyList(), errorMessage = null) }
            try {
                val response = httpClient.get("https://nominatim.openstreetmap.org/search") {
                    parameter("q", query)
                    parameter("format", "json")
                    parameter("countrycodes", "ru")
                    parameter("limit", "10")
                    parameter("bounded", "0")
                    header("User-Agent", "Lopon-CyclingApp/1.0")
                    header("Accept-Language", "ru")
                }

                val body = response.bodyAsText()
                val places = json.decodeFromString<List<NominatimPlace>>(body)

                val results = places.mapNotNull { place ->
                    if (place.boundingBox.size != 4) return@mapNotNull null
                    val south = place.boundingBox[0].toDoubleOrNull() ?: return@mapNotNull null
                    val north = place.boundingBox[1].toDoubleOrNull() ?: return@mapNotNull null
                    val west = place.boundingBox[2].toDoubleOrNull() ?: return@mapNotNull null
                    val east = place.boundingBox[3].toDoubleOrNull() ?: return@mapNotNull null

                    val shortName = place.displayName.split(",")
                        .take(3)
                        .joinToString(",") { it.trim() }

                    val bounds = LatLngBounds.Builder()
                        .include(LatLng(south, west))
                        .include(LatLng(north, east))
                        .build()
                    val estimatedSize = offlineHelper.estimateRegionSizeBytes(bounds)

                    SearchResult(
                        name = shortName,
                        swLat = south,
                        swLng = west,
                        neLat = north,
                        neLng = east,
                        estimatedSizeBytes = estimatedSize
                    )
                }

                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = results,
                        errorMessage = if (results.isEmpty()) "Ничего не найдено" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSearching = false, errorMessage = "Ошибка поиска: ${e.message}")
                }
            }
        }
    }

    fun downloadRegion(
        name: String,
        southWestLat: Double,
        southWestLng: Double,
        northEastLat: Double,
        northEastLng: Double
    ) {
        val bounds = LatLngBounds.Builder()
            .include(LatLng(southWestLat, southWestLng))
            .include(LatLng(northEastLat, northEastLng))
            .build()

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, errorMessage = null) }
            try {
                offlineHelper.downloadRegion(name, bounds).collect { progress ->
                    _uiState.update { it.copy(downloadProgress = progress) }
                }
                _uiState.update { it.copy(isDownloading = false, downloadProgress = null) }
                loadRegions()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadProgress = null,
                        errorMessage = "Ошибка загрузки: ${e.message}"
                    )
                }
            }
        }
    }

    fun downloadSearchResult(result: SearchResult) {
        downloadRegion(
            name = result.name,
            southWestLat = result.swLat,
            southWestLng = result.swLng,
            northEastLat = result.neLat,
            northEastLng = result.neLng
        )
    }

    fun deleteRegion(id: Long) {
        viewModelScope.launch {
            offlineHelper.deleteRegion(id).onSuccess {
                loadRegions()
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Ошибка удаления: ${error.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }

    class Factory(
        private val application: Application,
        private val container: AndroidAppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OfflineMapsViewModel(application, container) as T
        }
    }
}
