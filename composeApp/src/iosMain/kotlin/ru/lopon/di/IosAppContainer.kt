package ru.lopon.di

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.lopon.core.IdGenerator
import ru.lopon.core.RealTimeProvider
import ru.lopon.core.TimeProvider
import ru.lopon.core.UuidGenerator
import ru.lopon.core.gpx.GpxParser
import ru.lopon.core.gpx.GpxSerializer
import ru.lopon.core.metrics.MetricsAggregator
import ru.lopon.data.repository.InMemoryRouteRepository
import ru.lopon.data.repository.InMemoryTripRepository
import ru.lopon.data.repository.SettingsRepositoryImpl
import ru.lopon.domain.map.NoOpOfflineMapHelper
import ru.lopon.domain.map.OfflineMapManager
import ru.lopon.domain.map.PlatformOfflineMapHelper
import ru.lopon.domain.repository.LocationRepository
import ru.lopon.domain.repository.RouteRepository
import ru.lopon.domain.repository.SensorRepository
import ru.lopon.domain.repository.SettingsRepository
import ru.lopon.domain.repository.TripRepository
import ru.lopon.domain.routing.RoutingService
import ru.lopon.domain.state.TripStateManager
import ru.lopon.domain.usecase.CreateRouteUseCase
import ru.lopon.domain.usecase.ExportGpxUseCase
import ru.lopon.domain.usecase.ImportGpxUseCase
import ru.lopon.domain.usecase.PauseTripUseCase
import ru.lopon.domain.usecase.ResumeTripUseCase
import ru.lopon.domain.usecase.StartTripUseCase
import ru.lopon.domain.usecase.StopTripUseCase
import ru.lopon.platform.IosBleAdapter
import ru.lopon.platform.IosFileStorage
import ru.lopon.platform.IosLocationProvider
import ru.lopon.platform.IosPermissionsManager
import ru.lopon.platform.IosRoutingServiceFactory
import ru.lopon.platform.LocationStatus
import ru.lopon.ui.history.HistoryViewModel
import ru.lopon.ui.routes.RoutesViewModel
import ru.lopon.ui.sensor.SensorTestViewModel
import ru.lopon.ui.settings.SettingsViewModel

class IosAppContainer private constructor() {

    companion object {
        val shared: IosAppContainer by lazy { IosAppContainer() }
    }

    val timeProvider: TimeProvider = RealTimeProvider()
    val idGenerator: IdGenerator = UuidGenerator()
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val bleAdapter: IosBleAdapter by lazy { IosBleAdapter() }

    val locationProvider: IosLocationProvider by lazy { IosLocationProvider() }

    val fileStorage: IosFileStorage by lazy { IosFileStorage() }

    val permissionsManager: IosPermissionsManager by lazy { IosPermissionsManager() }

    val httpClient: HttpClient by lazy { IosRoutingServiceFactory.createHttpClient() }

    val routingService: RoutingService by lazy {
        IosRoutingServiceFactory.createRoutingService(httpClient)
    }

    val gpxParser: GpxParser by lazy { GpxParser() }
    val gpxSerializer: GpxSerializer by lazy { GpxSerializer() }

    val metricsAggregator: MetricsAggregator by lazy {
        MetricsAggregator(timeProvider = timeProvider)
    }

    val tripStateManager: TripStateManager by lazy { TripStateManager() }

    val routeRepository: RouteRepository by lazy { InMemoryRouteRepository() }

    val tripRepository: TripRepository by lazy { InMemoryTripRepository() }

    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(fileStorage) }

    private val sensorRepository: SensorRepository by lazy { createSensorRepository() }
    private val locationRepository: LocationRepository by lazy { createLocationRepository() }

    val createRouteUseCase: CreateRouteUseCase by lazy {
        CreateRouteUseCase(
            routingService = routingService,
            routeRepository = routeRepository,
            idGenerator = idGenerator
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

    val startTripUseCase: StartTripUseCase by lazy {
        StartTripUseCase(
            stateManager = tripStateManager,
            sensorRepository = sensorRepository,
            locationRepository = locationRepository,
            timeProvider = timeProvider,
            idGenerator = idGenerator,
            metricsAggregator = metricsAggregator
        )
    }

    val stopTripUseCase: StopTripUseCase by lazy {
        StopTripUseCase(
            stateManager = tripStateManager,
            tripRepository = tripRepository,
            sensorRepository = sensorRepository,
            locationRepository = locationRepository,
            timeProvider = timeProvider,
            startTripUseCase = startTripUseCase,
            metricsAggregator = metricsAggregator
        )
    }

    val pauseTripUseCase: PauseTripUseCase by lazy {
        PauseTripUseCase(
            stateManager = tripStateManager,
            sensorRepository = sensorRepository,
            locationRepository = locationRepository,
            metricsAggregator = metricsAggregator
        )
    }

    val resumeTripUseCase: ResumeTripUseCase by lazy {
        ResumeTripUseCase(
            stateManager = tripStateManager,
            metricsAggregator = metricsAggregator
        )
    }

    val historyViewModel: HistoryViewModel by lazy { HistoryViewModel(tripRepository) }

    val routesViewModel: RoutesViewModel by lazy {
        RoutesViewModel(routeRepository, createRouteUseCase)
    }

    val settingsViewModel: SettingsViewModel by lazy { SettingsViewModel(settingsRepository) }

    val sensorTestViewModel: SensorTestViewModel by lazy {
        SensorTestViewModel(bleAdapter, permissionsManager, settingsRepository)
    }

    private var offlineMapHelper: PlatformOfflineMapHelper = NoOpOfflineMapHelper()

    fun attachOfflineMapHelper(helper: PlatformOfflineMapHelper) {
        offlineMapHelper = helper
    }

    fun createOfflineMapManager(): OfflineMapManager = OfflineMapManager(offlineMapHelper)

    private fun createSensorRepository(): SensorRepository = object : SensorRepository {
        override fun observeConnectionState() = kotlinx.coroutines.flow.flow {
            bleAdapter.connectionState.collect { state ->
                emit(state is ru.lopon.platform.BleConnectionState.Connected)
            }
        }

        override fun observeReadings() = bleAdapter.observeWheelData()

        override suspend fun startScanning(): Result<Unit> {
            if (bleAdapter.connectionState.value is ru.lopon.platform.BleConnectionState.Connected) {
                return Result.success(Unit)
            }
            val devices = bleAdapter.scan()
            if (devices.isEmpty()) {
                return Result.failure(Exception("No devices found"))
            }
            return bleAdapter.connect(devices.first().id)
        }

        override suspend fun stopAndDisconnect() {
            bleAdapter.stopSensor()
        }

        override suspend fun isConnected(): Boolean {
            return bleAdapter.connectionState.value is ru.lopon.platform.BleConnectionState.Connected
        }
    }

    private fun createLocationRepository(): LocationRepository = object : LocationRepository {
        override fun observeLocation() =
            locationProvider.observeLocations().let { flow ->
                kotlinx.coroutines.flow.flow {
                    flow.collect { locationData ->
                        emit(
                            ru.lopon.domain.model.TrackPoint(
                                latitude = locationData.latitude,
                                longitude = locationData.longitude,
                                timestampUtc = locationData.timestampUtc,
                                source = ru.lopon.domain.model.TrackPoint.SOURCE_GPS,
                                elevation = locationData.altitude,
                                speed = locationData.speed?.toDouble()
                            )
                        )
                    }
                }
            }

        override fun observeGpsAvailability() =
            kotlinx.coroutines.flow.flow {
                locationProvider.status.collect { status ->
                    emit(status == LocationStatus.ACTIVE)
                }
            }

        override suspend fun startTracking(): Result<Unit> = try {
            locationProvider.start()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

        override suspend fun stopTracking() {
            locationProvider.stop()
        }

        override suspend fun getLastKnownLocation(): ru.lopon.domain.model.TrackPoint? = null

        override suspend fun isTracking(): Boolean =
            locationProvider.status.value == LocationStatus.ACTIVE ||
                    locationProvider.status.value == LocationStatus.SEARCHING
    }
}
