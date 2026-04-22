package ru.lopon.ui.trip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.lopon.core.metrics.MetricsAggregator
import ru.lopon.core.metrics.TripMetrics
import ru.lopon.di.AndroidAppContainer
import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.Route
import ru.lopon.domain.routing.RoutingProfile
import ru.lopon.domain.state.TripState
import ru.lopon.domain.state.TripStateManager
import ru.lopon.domain.usecase.*
import ru.lopon.platform.AndroidBleAdapter
import ru.lopon.platform.AppPermission
import ru.lopon.platform.BleConnectionState
import ru.lopon.platform.PermissionState

class TripViewModel(
    application: Application,
    private val container: AndroidAppContainer
) : AndroidViewModel(application) {

    private val bleAdapter: AndroidBleAdapter = container.bleAdapter
    private val tripStateManager: TripStateManager = container.tripStateManager
    private val metricsAggregator: MetricsAggregator = container.metricsAggregator
    private val createRouteUseCase: CreateRouteUseCase = container.createRouteUseCase
    private val startTripUseCase: StartTripUseCase = container.startTripUseCase
    private val stopTripUseCase: StopTripUseCase = container.stopTripUseCase
    private val pauseTripUseCase: PauseTripUseCase = container.pauseTripUseCase
    private val resumeTripUseCase: ResumeTripUseCase = container.resumeTripUseCase

    private val _uiState = MutableStateFlow(
        TripUiState(
            isConnected = bleAdapter.connectionState.value is BleConnectionState.Connected,
            connectionState = bleAdapter.connectionState.value
        )
    )
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()


    init {
        observeTripState()
        observeMetrics()
        observeConnectionState()
        observeRoutes()
        requestLocationAndStartTracking()
    }

    private fun requestLocationAndStartTracking() {
        viewModelScope.launch {
            val permState = container.permissionsManager.checkPermission(AppPermission.LOCATION)
            if (permState == PermissionState.GRANTED) {
                startLocationProvider()
                observeBackgroundLocation()
            } else {
                val result = container.permissionsManager.requestPermission(AppPermission.LOCATION)
                if (result == PermissionState.GRANTED) {
                    startLocationProvider()
                    observeBackgroundLocation()
                }
            }
        }
    }

    private suspend fun startLocationProvider() {
        try {
            container.locationProvider.start()
        } catch (_: Exception) {
        }
    }

    class Factory(
        private val application: Application,
        private val container: AndroidAppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TripViewModel(application, container) as T
        }
    }

    private fun observeTripState() {
        viewModelScope.launch {
            tripStateManager.state.collect { state ->
                _uiState.update {
                    it.copy(
                        tripState = state,
                        isRecording = state is TripState.Recording,
                        isPaused = state is TripState.Paused,
                        routeProgressPercent = when (state) {
                            is TripState.Recording -> state.routeProgressPercent
                            is TripState.Paused -> state.routeProgressPercent
                            else -> null
                        },
                        distanceToRouteEndM = when (state) {
                            is TripState.Recording -> state.distanceToRouteEndM
                            is TripState.Paused -> state.distanceToRouteEndM
                            else -> null
                        }
                    )
                }
            }
        }
    }

    private fun observeMetrics() {
        viewModelScope.launch {
            metricsAggregator.metrics.collect { metrics ->
                _uiState.update { it.copy(metrics = metrics) }
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            bleAdapter.connectionState.collect { state ->
                _uiState.update {
                    it.copy(
                        isConnected = state is BleConnectionState.Connected,
                        connectionState = state
                    )
                }
            }
        }
    }

    private fun observeRoutes() {
        viewModelScope.launch {
            container.routeRepository.observeRoutes().collect { routes ->
                _uiState.update { state ->
                    val selectedRouteId = when {
                        state.selectedRouteId == null && routes.isNotEmpty() -> routes.first().id
                        state.selectedRouteId != null && routes.any { it.id == state.selectedRouteId } -> state.selectedRouteId
                        else -> null
                    }
                    state.copy(
                        availableRoutes = routes,
                        selectedRouteId = selectedRouteId
                    )
                }
            }
        }
    }

    private fun observeBackgroundLocation() {
        viewModelScope.launch {
            try {
                container.locationProvider.observeLocations().collect { locationData ->
                    val newCoord = GeoCoordinate(
                        latitude = locationData.latitude,
                        longitude = locationData.longitude,
                        bearing = locationData.bearing
                    )
                    _uiState.update { state ->
                        val updatedTrack = if (state.isRecording) {
                            val activeRoute = when (val ts = state.tripState) {
                                is TripState.Recording -> ts.route
                                else -> null
                            }
                            if (activeRoute == null) {
                                state.recordedTrackPoints + newCoord
                            } else state.recordedTrackPoints
                        } else state.recordedTrackPoints

                        state.copy(
                            lastKnownLocation = newCoord,
                            recordedTrackPoints = updatedTrack
                        )
                    }
                }
            } catch (_: SecurityException) {
            }
        }
    }

    fun openStartWizard() {
        _uiState.update { it.copy(isWizardOpen = true, errorMessage = null) }
    }

    fun openStartWizardWithRoute(routeId: String) {
        _uiState.update {
            it.copy(
                isWizardOpen = true,
                selectedRouteId = routeId,
                selectedMode = NavigationMode.Sensor,
                errorMessage = null
            )
        }
    }

    fun closeStartWizard() {
        _uiState.update { it.copy(isWizardOpen = false) }
    }

    fun selectMode(mode: NavigationMode) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun selectRoute(routeId: String) {
        _uiState.update { it.copy(selectedRouteId = routeId) }
    }

    fun createRouteFromPoints(
        startLat: String,
        startLon: String,
        endLat: String,
        endLon: String
    ) {
        viewModelScope.launch {
            val parsed = parseRouteInputs(startLat, startLon, endLat, endLon)
            if (parsed == null) {
                _uiState.update { it.copy(errorMessage = "Некорректные координаты маршрута") }
                return@launch
            }

            _uiState.update { it.copy(isCreatingRoute = true, errorMessage = null) }

            val (start, end) = parsed
            val routeName = "Маршрут ${System.currentTimeMillis()}"
            val result = createRouteUseCase(
                waypoints = listOf(start, end),
                name = routeName,
                profile = RoutingProfile.BIKE,
                saveToRepository = true
            )

            result.onSuccess { routeWithDetails ->
                _uiState.update {
                    it.copy(
                        isCreatingRoute = false,
                        selectedRouteId = routeWithDetails.route.id
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isCreatingRoute = false,
                        errorMessage = "Не удалось построить маршрут: ${error.message}"
                    )
                }
            }
        }
    }

    fun createQuickRoute() {
        createRouteFromPoints(
            startLat = "55.7558",
            startLon = "37.6173",
            endLat = "55.7648",
            endLon = "37.6173"
        )
    }

    fun quickStartTrip() {
        viewModelScope.launch {
            if (tripStateManager.currentState is TripState.Finished) {
                tripStateManager.reset()
            }

            _uiState.update {
                it.copy(
                    selectedMode = NavigationMode.Gps,
                    errorMessage = null,
                    recordedTrackPoints = emptyList()
                )
            }

            val result = startTripUseCase(
                mode = NavigationMode.Gps,
                scope = viewModelScope,
                route = null
            )

            result.onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Ошибка старта: ${error.message}") }
            }
        }
    }

    fun startTripFromWizard() {
        viewModelScope.launch {
            val state = _uiState.value
            val mode = state.selectedMode
            val selectedRoute = state.availableRoutes.firstOrNull { it.id == state.selectedRouteId }

            if (tripStateManager.currentState is TripState.Finished) {
                tripStateManager.reset()
            }

            if (mode is NavigationMode.Sensor && selectedRoute == null) {
                _uiState.update {
                    it.copy(errorMessage = "Для Sensor режима сначала создайте или выберите маршрут")
                }
                return@launch
            }

            val result = startTripUseCase(mode, viewModelScope, selectedRoute)

            result.onSuccess {
                if (mode is NavigationMode.Sensor || mode is NavigationMode.Hybrid) {
                    val sensorStartResult = bleAdapter.startSensor()
                    if (sensorStartResult.isFailure) {
                        _uiState.update {
                            it.copy(
                                isWizardOpen = false,
                                errorMessage = "Поездка запущена, но не удалось активировать датчик: ${sensorStartResult.exceptionOrNull()?.message}"
                            )
                        }
                        return@launch
                    }
                }
                _uiState.update { it.copy(isWizardOpen = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = "Ошибка старта: ${error.message}")
                }
            }
        }
    }

    fun stopTrip() {
        viewModelScope.launch {
            bleAdapter.stopSensor()

            val finalMetrics = _uiState.value.metrics
            val modeName = when (_uiState.value.selectedMode) {
                NavigationMode.Sensor -> "Sensor"
                NavigationMode.Gps -> "GPS"
                NavigationMode.Hybrid -> "Hybrid"
            }

            val result = stopTripUseCase()

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        showTripSummary = true,
                        summaryMetrics = finalMetrics,
                        summaryModeName = modeName
                    )
                }
                tripStateManager.reset()
                metricsAggregator.reset()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = "Ошибка остановки: ${error.message}")
                }
            }
        }
    }

    fun dismissTripSummary() {
        _uiState.update {
            it.copy(
                showTripSummary = false,
                summaryMetrics = null,
                summaryModeName = "",
                recordedTrackPoints = emptyList()
            )
        }
    }

    fun pauseTrip() {
        viewModelScope.launch {
            val stopResult = bleAdapter.stopSensor()
            if (stopResult.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = "Не удалось приостановить датчик: ${stopResult.exceptionOrNull()?.message}")
                }
                return@launch
            }
            val pauseResult = pauseTripUseCase()
            pauseResult.onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Ошибка паузы: ${error.message}") }
            }
        }
    }

    fun resumeTrip() {
        viewModelScope.launch {
            val startResult = bleAdapter.startSensor()
            if (startResult.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = "Не удалось запустить датчик: ${startResult.exceptionOrNull()?.message}")
                }
                return@launch
            }
            val resumeResult = resumeTripUseCase()
            resumeResult.onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Ошибка возобновления: ${error.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun parseRouteInputs(
        startLat: String,
        startLon: String,
        endLat: String,
        endLon: String
    ): Pair<GeoCoordinate, GeoCoordinate>? {
        return ru.lopon.core.GeoCoordinateParser.parseCoordinatePair(startLat, startLon, endLat, endLon)
    }
}

data class TripUiState(
    val tripState: TripState = TripState.Idle,
    val metrics: TripMetrics = TripMetrics.ZERO,
    val isConnected: Boolean = false,
    val connectionState: BleConnectionState = BleConnectionState.Disconnected,
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val isWizardOpen: Boolean = false,
    val isCreatingRoute: Boolean = false,
    val selectedMode: NavigationMode = NavigationMode.Sensor,
    val availableRoutes: List<Route> = emptyList(),
    val selectedRouteId: String? = null,
    val routeProgressPercent: Double? = null,
    val distanceToRouteEndM: Double? = null,
    val errorMessage: String? = null,
    val lastKnownLocation: GeoCoordinate? = null,
    val showTripSummary: Boolean = false,
    val summaryMetrics: TripMetrics? = null,
    val summaryModeName: String = "",
    val recordedTrackPoints: List<GeoCoordinate> = emptyList()
)
