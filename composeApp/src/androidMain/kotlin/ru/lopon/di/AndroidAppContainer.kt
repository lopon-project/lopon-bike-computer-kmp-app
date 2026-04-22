package ru.lopon.di

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
import ru.lopon.domain.usecase.ExportGpxUseCase
import ru.lopon.domain.usecase.ImportGpxUseCase
import ru.lopon.domain.repository.LocationRepository
import ru.lopon.domain.repository.SensorRepository
import ru.lopon.domain.repository.TripRepository
import ru.lopon.domain.state.TripStateManager
import ru.lopon.domain.usecase.CreateRouteUseCase
import ru.lopon.domain.usecase.PauseTripUseCase
import ru.lopon.domain.usecase.ResumeTripUseCase
import ru.lopon.domain.usecase.StartTripUseCase
import ru.lopon.domain.usecase.StopTripUseCase
import ru.lopon.platform.AndroidBleAdapter
import ru.lopon.platform.AndroidFileStorage
import ru.lopon.platform.AndroidLocationProvider
import ru.lopon.platform.AndroidPermissionsManager
import ru.lopon.platform.LocationStatus
import ru.lopon.data.routing.FallbackRoutingService
import ru.lopon.data.routing.OrsRoutingService
import ru.lopon.data.routing.OsrmRoutingService
import ru.lopon.domain.routing.RoutingService
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AndroidAppContainer private constructor(
    private val context: Context
) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: AndroidAppContainer? = null

        fun getInstance(context: Context): AndroidAppContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AndroidAppContainer(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    val timeProvider: TimeProvider = RealTimeProvider()
    val idGenerator: IdGenerator = UuidGenerator()
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val bleAdapter: AndroidBleAdapter by lazy {
        AndroidBleAdapter(context)
    }

    val permissionsManager: AndroidPermissionsManager by lazy {
        AndroidPermissionsManager(context)
    }

    val locationProvider: AndroidLocationProvider by lazy {
        AndroidLocationProvider(context)
    }

    val fileStorage: AndroidFileStorage by lazy {
        AndroidFileStorage(context)
    }

    val gpxParser: GpxParser by lazy { GpxParser() }
    val gpxSerializer: GpxSerializer by lazy { GpxSerializer() }

    val settingsRepository: SettingsRepositoryImpl by lazy {
        SettingsRepositoryImpl(fileStorage)
    }

    val metricsAggregator: MetricsAggregator by lazy {
        MetricsAggregator(timeProvider = timeProvider)
    }

    val tripStateManager: TripStateManager by lazy {
        TripStateManager()
    }

    val routeRepository: InMemoryRouteRepository by lazy {
        InMemoryRouteRepository()
    }

    val tripRepository: TripRepository by lazy {
        InMemoryTripRepository()
    }

    val httpClient: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    private val osrmRoutingService: OsrmRoutingService by lazy {
        OsrmRoutingService(httpClient = httpClient)
    }

    private val orsRoutingService: OrsRoutingService by lazy {
        OrsRoutingService(httpClient = httpClient)
    }

    val routingService: RoutingService by lazy {
        FallbackRoutingService(
            primary = osrmRoutingService,
            fallback = orsRoutingService
        )
    }

    private val sensorRepository: SensorRepository by lazy {
        createSensorRepository()
    }

    private val locationRepository: LocationRepository by lazy {
        createLocationRepository()
    }

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

    // Use cases

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

    private fun createSensorRepository(): SensorRepository {
        return object : SensorRepository {
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
    }

    private fun createLocationRepository(): LocationRepository {
        return object : LocationRepository {
            override fun observeLocation(): Flow<ru.lopon.domain.model.TrackPoint> {
                return locationProvider.observeLocations().map { locationData ->
                    ru.lopon.domain.model.TrackPoint(
                        latitude = locationData.latitude,
                        longitude = locationData.longitude,
                        timestampUtc = locationData.timestampUtc,
                        source = ru.lopon.domain.model.TrackPoint.SOURCE_GPS,
                        elevation = locationData.altitude,
                        speed = locationData.speed?.toDouble()
                    )
                }
            }

            override fun observeGpsAvailability(): Flow<Boolean> {
                return locationProvider.status.map { it == LocationStatus.ACTIVE }
            }

            override suspend fun startTracking(): Result<Unit> {
                return try {
                    locationProvider.start()
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            override suspend fun stopTracking() {
                locationProvider.stop()
            }

            override suspend fun getLastKnownLocation(): ru.lopon.domain.model.TrackPoint? = null

            override suspend fun isTracking(): Boolean {
                return locationProvider.status.value == LocationStatus.ACTIVE ||
                        locationProvider.status.value == LocationStatus.SEARCHING
            }
        }
    }

    fun release() {
        bleAdapter.release()
    }
}
