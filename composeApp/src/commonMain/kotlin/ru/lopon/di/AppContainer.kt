package ru.lopon.di

import kotlinx.coroutines.CoroutineScope
import ru.lopon.core.IdGenerator
import ru.lopon.core.TimeProvider
import ru.lopon.core.gpx.GpxParser
import ru.lopon.core.gpx.GpxSerializer
import ru.lopon.core.metrics.MetricsAggregator
import ru.lopon.data.repository.InMemoryRouteRepository
import ru.lopon.data.repository.InMemoryTripRepository
import ru.lopon.data.repository.SettingsRepositoryImpl
import ru.lopon.domain.navigation.NavigationStateMachine
import ru.lopon.domain.repository.RouteRepository
import ru.lopon.domain.repository.SettingsRepository
import ru.lopon.domain.repository.TripRepository
import ru.lopon.domain.routing.RoutingService
import ru.lopon.domain.state.TripStateManager
import ru.lopon.domain.usecase.*
import ru.lopon.platform.BleAdapter
import ru.lopon.platform.FileStorage
import ru.lopon.platform.LocationProvider
import ru.lopon.platform.PermissionsManager

//TODO: возможно заменить на DI фреймворк в будущем
class AppContainer(
    val bleAdapter: BleAdapter,
    val locationProvider: LocationProvider,
    val fileStorage: FileStorage,
    val permissionsManager: PermissionsManager,
    val routingService: RoutingService,
    val timeProvider: TimeProvider,
    val idGenerator: IdGenerator,
    val applicationScope: CoroutineScope
) {


    val gpxParser: GpxParser by lazy { GpxParser() }

    val gpxSerializer: GpxSerializer by lazy { GpxSerializer() }

    val metricsAggregator: MetricsAggregator by lazy {
        MetricsAggregator(timeProvider = timeProvider)
    }

    val tripRepository: TripRepository by lazy { InMemoryTripRepository() }

    val routeRepository: RouteRepository by lazy { InMemoryRouteRepository() }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(fileStorage)
    }


    val tripStateManager: TripStateManager by lazy { TripStateManager() }

    val navigationStateMachine: NavigationStateMachine by lazy {
        NavigationStateMachine(
            bleAdapter = bleAdapter,
            locationProvider = locationProvider,
            metricsAggregator = metricsAggregator,
            timeProvider = timeProvider,
            idGenerator = idGenerator,
            scope = applicationScope
        )
    }


    val startTripUseCase: StartTripUseCase by lazy {
        StartTripUseCase(
            stateManager = tripStateManager,
            sensorRepository = createSensorRepository(),
            locationRepository = createLocationRepository(),
            timeProvider = timeProvider,
            idGenerator = idGenerator,
            metricsAggregator = metricsAggregator
        )
    }

    val stopTripUseCase: StopTripUseCase by lazy {
        StopTripUseCase(
            stateManager = tripStateManager,
            tripRepository = tripRepository,
            sensorRepository = createSensorRepository(),
            locationRepository = createLocationRepository(),
            timeProvider = timeProvider,
            startTripUseCase = startTripUseCase,
            metricsAggregator = metricsAggregator
        )
    }

    val pauseTripUseCase: PauseTripUseCase by lazy {
        PauseTripUseCase(
            stateManager = tripStateManager,
            sensorRepository = createSensorRepository(),
            locationRepository = createLocationRepository(),
            metricsAggregator = metricsAggregator
        )
    }

    val resumeTripUseCase: ResumeTripUseCase by lazy {
        ResumeTripUseCase(
            stateManager = tripStateManager,
            metricsAggregator = metricsAggregator
        )
    }

    val switchModeUseCase: SwitchModeUseCase by lazy {
        SwitchModeUseCase(
            stateManager = tripStateManager,
            sensorRepository = createSensorRepository(),
            locationRepository = createLocationRepository()
        )
    }

    val importGpxUseCase: ImportGpxUseCase by lazy {
        ImportGpxUseCase(
            fileStorage = fileStorage,
            gpxParser = gpxParser,
            routeRepository = routeRepository,
            idGenerator = idGenerator
        )
    }

    val exportGpxUseCase: ExportGpxUseCase by lazy {
        ExportGpxUseCase(
            fileStorage = fileStorage,
            gpxSerializer = gpxSerializer,
            tripRepository = tripRepository
        )
    }

    val createRouteUseCase: CreateRouteUseCase by lazy {
        CreateRouteUseCase(
            routingService = routingService,
            routeRepository = routeRepository,
            idGenerator = idGenerator
        )
    }


    private fun createSensorRepository(): ru.lopon.domain.repository.SensorRepository {
        return BleBasedSensorRepository(bleAdapter)
    }

    private fun createLocationRepository(): ru.lopon.domain.repository.LocationRepository {
        return LocationProviderBasedRepository(locationProvider)
    }
}


private class BleBasedSensorRepository(
    private val bleAdapter: BleAdapter
) : ru.lopon.domain.repository.SensorRepository {

    override fun observeConnectionState() = kotlinx.coroutines.flow.flow {
        bleAdapter.connectionState.collect { state ->
            emit(state is ru.lopon.platform.BleConnectionState.Connected)
        }
    }

    override fun observeReadings() = bleAdapter.observeWheelData()

    override suspend fun startScanning(): Result<Unit> {
        val devices = bleAdapter.scan()
        if (devices.isEmpty()) {
            return Result.failure(Exception("No devices found"))
        }
        return bleAdapter.connect(devices.first().id)
    }

    override suspend fun stopAndDisconnect() {
        bleAdapter.disconnect()
    }

    override suspend fun isConnected(): Boolean {
        return bleAdapter.connectionState.value is ru.lopon.platform.BleConnectionState.Connected
    }
}


private class LocationProviderBasedRepository(
    private val locationProvider: LocationProvider
) : ru.lopon.domain.repository.LocationRepository {

    private var lastLocation: ru.lopon.domain.model.TrackPoint? = null
    private var isCurrentlyTracking = false

    override fun observeLocation() = kotlinx.coroutines.flow.flow {
        locationProvider.observeLocations().collect { location ->
            val trackPoint = ru.lopon.domain.model.TrackPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                timestampUtc = location.timestampUtc,
                source = ru.lopon.domain.model.TrackPoint.SOURCE_GPS,
                elevation = location.altitude,
                speed = location.speed?.toDouble()
            )
            lastLocation = trackPoint
            emit(trackPoint)
        }
    }

    override fun observeGpsAvailability() = kotlinx.coroutines.flow.flow {
        locationProvider.status.collect { status ->
            emit(status == ru.lopon.platform.LocationStatus.ACTIVE)
        }
    }

    override suspend fun startTracking(): Result<Unit> {
        return try {
            locationProvider.start()
            isCurrentlyTracking = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopTracking() {
        locationProvider.stop()
        isCurrentlyTracking = false
    }

    override suspend fun getLastKnownLocation(): ru.lopon.domain.model.TrackPoint? = lastLocation

    override suspend fun isTracking(): Boolean = isCurrentlyTracking
}
