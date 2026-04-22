package ru.lopon.ui.routes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.lopon.data.geocoding.PhotonGeocodingService
import ru.lopon.data.geocoding.PhotonSearchResult
import ru.lopon.di.AndroidAppContainer
import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.Route
import ru.lopon.domain.routing.RoutingProfile
import ru.lopon.platform.AppPermission
import ru.lopon.platform.PermissionState

enum class WizardTab { POINTS, GPX_IMPORT }

data class RouteDetails(
    val distanceMeters: Double,
    val durationSeconds: Long,
    val elevationGainMeters: Double?,
    val elevationLossMeters: Double?
)

data class RouteWizardUiState(
    val activeTab: WizardTab = WizardTab.POINTS,
    val waypoints: List<GeoCoordinate> = emptyList(),
    val calculatedRoute: Route? = null,
    val routeDetails: RouteDetails? = null,
    val importedRoute: Route? = null,
    val routeName: String = "",
    val isCalculating: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
    val currentLocation: GeoCoordinate? = null,
    val locationPermissionGranted: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<PhotonSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val showSearchResults: Boolean = false,
    val selectedWaypointIndex: Int? = null
)

class RouteWizardViewModel(
    application: Application,
    private val container: AndroidAppContainer
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RouteWizardUiState())
    val uiState: StateFlow<RouteWizardUiState> = _uiState.asStateFlow()

    private var autoCalculateJob: Job? = null
    private var searchJob: Job? = null
    private var locationJob: Job? = null

    private val geocodingService: PhotonGeocodingService by lazy {
        PhotonGeocodingService(httpClient = container.httpClient)
    }

    companion object {
        private const val AUTO_CALCULATE_DEBOUNCE_MS = 800L
        private const val SEARCH_DEBOUNCE_MS = 400L
    }

    init {
        val permState = container.permissionsManager.checkPermission(AppPermission.LOCATION)
        if (permState == PermissionState.GRANTED) {
            _uiState.update { it.copy(locationPermissionGranted = true) }
            startLocationTracking()
        }
    }

    fun resetState() {
        autoCalculateJob?.cancel()
        autoCalculateJob = null
        _uiState.update {
            RouteWizardUiState(
                locationPermissionGranted = it.locationPermissionGranted,
                currentLocation = it.currentLocation
            )
        }
    }

    fun requestLocationPermission() {
        viewModelScope.launch {
            val result = container.permissionsManager.requestPermission(AppPermission.LOCATION)
            if (result == PermissionState.GRANTED) {
                _uiState.update { it.copy(locationPermissionGranted = true) }
                startLocationTracking()
            }
        }
    }

    private fun startLocationTracking() {
        if (locationJob != null) return
        locationJob = viewModelScope.launch {
            try {
                container.locationProvider.start()
                container.locationProvider.observeLocations().collect { locationData ->
                    _uiState.update {
                        it.copy(
                            currentLocation = GeoCoordinate(
                                latitude = locationData.latitude,
                                longitude = locationData.longitude,
                                bearing = locationData.bearing
                            )
                        )
                    }
                }
            } catch (_: SecurityException) {
                // Permission not granted
            } catch (_: Exception) {
                // Location provider error
            }
        }
    }

    fun setActiveTab(tab: WizardTab) {
        _uiState.update { it.copy(activeTab = tab, errorMessage = null) }
    }

    fun addWaypoint(coord: GeoCoordinate) {
        val currentState = _uiState.value
        if (currentState.selectedWaypointIndex != null) {
            moveSelectedWaypoint(coord)
            return
        }

        _uiState.update {
            it.copy(
                waypoints = it.waypoints + coord,
                calculatedRoute = null,
                routeDetails = null,
                errorMessage = null,
                showSearchResults = false,
                searchQuery = ""
            )
        }
        scheduleAutoCalculate()
    }

    fun insertWaypoint(index: Int, coord: GeoCoordinate) {
        _uiState.update {
            val newWaypoints = it.waypoints.toMutableList().apply {
                add(index.coerceIn(0, size), coord)
            }
            it.copy(
                waypoints = newWaypoints,
                calculatedRoute = null,
                routeDetails = null,
                selectedWaypointIndex = index.coerceIn(0, newWaypoints.size - 1),
                errorMessage = null
            )
        }
        scheduleAutoCalculate()
    }

    fun moveSelectedWaypoint(coord: GeoCoordinate) {
        val index = _uiState.value.selectedWaypointIndex ?: return
        _uiState.update {
            if (index >= it.waypoints.size) return@update it.copy(selectedWaypointIndex = null)
            val newWaypoints = it.waypoints.toMutableList().apply {
                set(index, coord)
            }
            it.copy(
                waypoints = newWaypoints,
                calculatedRoute = null,
                routeDetails = null,
                selectedWaypointIndex = null
            )
        }
        scheduleAutoCalculate()
    }

    fun cancelWaypointEdit() {
        _uiState.update { it.copy(selectedWaypointIndex = null) }
    }

    fun removeLastWaypoint() {
        _uiState.update {
            if (it.waypoints.isEmpty()) return@update it
            it.copy(
                waypoints = it.waypoints.dropLast(1),
                calculatedRoute = null,
                routeDetails = null
            )
        }
        if (_uiState.value.waypoints.size >= 2) {
            scheduleAutoCalculate()
        }
    }

    fun clearAll() {
        autoCalculateJob?.cancel()
        _uiState.update {
            RouteWizardUiState(
                activeTab = it.activeTab,
                currentLocation = it.currentLocation,
                locationPermissionGranted = it.locationPermissionGranted
            )
        }
    }

    fun updateRouteName(name: String) {
        _uiState.update { it.copy(routeName = name) }
    }


    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, showSearchResults = query.length >= 2) }
        if (query.length >= 2) {
            scheduleSearch(query)
        } else {
            searchJob?.cancel()
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    fun selectSearchResult(result: PhotonSearchResult) {
        addWaypoint(result.coordinate)
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                showSearchResults = false
            )
        }
    }

    fun dismissSearch() {
        _uiState.update {
            it.copy(
                showSearchResults = false,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isSearching = true) }

            val loc = _uiState.value.currentLocation
            val result = geocodingService.search(
                query = query,
                lat = loc?.latitude,
                lon = loc?.longitude,
                limit = 7,
                lang = "ru"
            )

            result.onSuccess { results ->
                _uiState.update {
                    it.copy(
                        searchResults = results,
                        isSearching = false,
                        showSearchResults = true
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }


    private fun scheduleAutoCalculate() {
        autoCalculateJob?.cancel()
        autoCalculateJob = viewModelScope.launch {
            delay(AUTO_CALCULATE_DEBOUNCE_MS)
            calculateRouteInternal()
        }
    }

    fun calculateRoute() {
        autoCalculateJob?.cancel()
        viewModelScope.launch {
            calculateRouteInternal()
        }
    }

    private suspend fun calculateRouteInternal() {
        val waypoints = _uiState.value.waypoints
        if (waypoints.size < 2) {
            _uiState.update { it.copy(errorMessage = "Добавьте минимум 2 точки") }
            return
        }

        _uiState.update { it.copy(isCalculating = true, errorMessage = null) }
        val name = _uiState.value.routeName.ifBlank { "Маршрут ${System.currentTimeMillis()}" }

        val result = container.createRouteUseCase(
            waypoints = waypoints,
            name = name,
            profile = RoutingProfile.BIKE,
            saveToRepository = false
        )

        result.onSuccess { routeWithDetails ->
            _uiState.update {
                it.copy(
                    isCalculating = false,
                    calculatedRoute = routeWithDetails.route,
                    routeDetails = RouteDetails(
                        distanceMeters = routeWithDetails.distanceMeters,
                        durationSeconds = routeWithDetails.durationSeconds,
                        elevationGainMeters = routeWithDetails.elevationGainMeters,
                        elevationLossMeters = routeWithDetails.elevationLossMeters
                    )
                )
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(isCalculating = false, errorMessage = "Ошибка: ${error.message}")
            }
        }
    }

    fun importGpx(content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalculating = true, errorMessage = null) }
            val result = container.importGpxUseCase.importFromContent(content)

            result.onSuccess { route ->
                _uiState.update {
                    it.copy(
                        isCalculating = false,
                        importedRoute = route,
                        routeName = route.name,
                        savedSuccessfully = true
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isCalculating = false, errorMessage = "Ошибка импорта: ${error.message}")
                }
            }
        }
    }

    fun saveRoute() {
        val route = _uiState.value.calculatedRoute ?: return
        val name = _uiState.value.routeName.ifBlank { route.name }
        val routeToSave = route.copy(name = name)

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = container.routeRepository.saveRoute(routeToSave)

            result.onSuccess {
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Ошибка сохранения: ${error.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        locationJob?.cancel()
    }

    class Factory(
        private val application: Application,
        private val container: AndroidAppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RouteWizardViewModel(application, container) as T
        }
    }
}
