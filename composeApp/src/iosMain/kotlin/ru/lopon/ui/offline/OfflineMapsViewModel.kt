package ru.lopon.ui.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.lopon.di.IosAppContainer
import ru.lopon.domain.map.CommonBounds
import ru.lopon.domain.map.CommonLatLng
import ru.lopon.domain.map.DownloadProgress
import ru.lopon.domain.map.OfflineMapManager
import ru.lopon.domain.map.OfflineRegionInfo

@Serializable
private data class NominatimPlace(
    val display_name: String = "",
    val boundingbox: List<String> = emptyList()
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
    private val container: IosAppContainer
) : ViewModel() {

    private val offlineManager: OfflineMapManager = container.createOfflineMapManager()
    private val httpClient = container.httpClient
    private val json = Json { ignoreUnknownKeys = true }
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
                val regions = offlineManager.listRegions()
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
            _uiState.update {
                it.copy(isSearching = true, searchResults = emptyList(), errorMessage = null)
            }
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
                    if (place.boundingbox.size != 4) return@mapNotNull null
                    val south = place.boundingbox[0].toDoubleOrNull() ?: return@mapNotNull null
                    val north = place.boundingbox[1].toDoubleOrNull() ?: return@mapNotNull null
                    val west = place.boundingbox[2].toDoubleOrNull() ?: return@mapNotNull null
                    val east = place.boundingbox[3].toDoubleOrNull() ?: return@mapNotNull null

                    val shortName = place.display_name.split(",")
                        .take(3)
                        .joinToString(",") { it.trim() }

                    val bounds = CommonBounds(
                        southWest = CommonLatLng(south, west),
                        northEast = CommonLatLng(north, east)
                    )
                    val estimatedSize = offlineManager.estimateRegionSizeBytes(bounds)

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
        val bounds = CommonBounds(
            southWest = CommonLatLng(southWestLat, southWestLng),
            northEast = CommonLatLng(northEastLat, northEastLng)
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloading = true,
                    downloadRegionName = name,
                    errorMessage = null
                )
            }
            try {
                offlineManager.downloadRegion(name, bounds).collect { progress ->
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
            offlineManager.deleteRegion(id).onSuccess {
                loadRegions()
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Ошибка удаления: ${error.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
