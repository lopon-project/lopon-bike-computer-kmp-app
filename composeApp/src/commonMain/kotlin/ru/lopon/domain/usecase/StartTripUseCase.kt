package ru.lopon.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.lopon.core.DistanceCalculator
import ru.lopon.core.IdGenerator
import ru.lopon.core.TimeProvider
import ru.lopon.core.metrics.MetricsAggregator
import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.Route
import ru.lopon.domain.model.Trip
import ru.lopon.domain.processing.SensorModePositionCalculator
import ru.lopon.domain.repository.LocationRepository
import ru.lopon.domain.repository.SensorRepository
import ru.lopon.domain.state.TripState
import ru.lopon.domain.state.TripStateManager


class StartTripUseCase(
    private val stateManager: TripStateManager,
    private val sensorRepository: SensorRepository,
    private val locationRepository: LocationRepository,
    private val timeProvider: TimeProvider,
    private val idGenerator: IdGenerator,
    private val metricsAggregator: MetricsAggregator? = null,
    private val wheelCircumferenceMm: Double = DistanceCalculator.DEFAULT_WHEEL_CIRCUMFERENCE_MM
) {
    private var sensorJob: Job? = null
    private var locationJob: Job? = null
    private var metricsJob: Job? = null

    private var processSensorDataUseCase: ProcessSensorDataUseCase? = null
    private var processLocationDataUseCase: ProcessLocationDataUseCase? = null
    private var sensorModePositionCalculator: SensorModePositionCalculator? = null

    suspend operator fun invoke(
        mode: NavigationMode,
        scope: CoroutineScope,
        route: Route? = null
    ): Result<Trip> {
        if (stateManager.currentState is TripState.Finished) {
            stateManager.reset()
        }

        if (stateManager.currentState !is TripState.Idle) {
            return Result.failure(IllegalStateException("Cannot start trip: trip already active"))
        }

        if (mode is NavigationMode.Sensor && (route == null || !route.isValid)) {
            return Result.failure(IllegalArgumentException("Sensor mode requires a valid route"))
        }

        val initResult = initializeDataSources(mode, scope)
        if (initResult.isFailure) {
            return Result.failure(initResult.exceptionOrNull()!!)
        }

        val trip = Trip(
            id = idGenerator.generateId(),
            startTimeUtc = timeProvider.currentTimeMillis(),
            mode = mode,
            routeId = route?.id
        )


        metricsAggregator?.reset()
        metricsAggregator?.start(scope)

        metricsAggregator?.let { aggregator ->
            processSensorDataUseCase = ProcessSensorDataUseCase(
                stateManager = stateManager,
                metricsAggregator = aggregator,
                wheelCircumferenceMm = wheelCircumferenceMm
            )
            processLocationDataUseCase = ProcessLocationDataUseCase(
                stateManager = stateManager,
                metricsAggregator = aggregator
            )
        }

        sensorModePositionCalculator = if (mode is NavigationMode.Sensor && route != null) {
            SensorModePositionCalculator(route)
        } else {
            null
        }

        subscribeMetrics(scope)


        val started = stateManager.startTrip(trip, mode, route)
        if (!started) {
            cancelDataSources()
            metricsAggregator?.reset()
            return Result.failure(IllegalStateException("Failed to transition to Recording state"))
        }

        return Result.success(trip)
    }


    private suspend fun initializeDataSources(mode: NavigationMode, scope: CoroutineScope): Result<Unit> {
        return try {
            when (mode) {
                is NavigationMode.Sensor -> {
                    sensorRepository.startScanning().getOrThrow()
                    subscribeSensorData(scope)
                }

                is NavigationMode.Hybrid -> {
                    sensorRepository.startScanning().getOrThrow()
                    locationRepository.startTracking().getOrThrow()
                    subscribeSensorData(scope)
                    subscribeLocationData(scope)
                }

                is NavigationMode.Gps -> {
                    locationRepository.startTracking().getOrThrow()
                    subscribeLocationData(scope)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            cancelDataSources()
            Result.failure(e)
        }
    }


    private fun subscribeSensorData(scope: CoroutineScope) {
        sensorJob = sensorRepository.observeReadings()
            .onEach { reading ->
                val result = processSensorDataUseCase?.invoke(reading)?.getOrNull()
                val processed = result as? SensorProcessingResult.Processed ?: return@onEach

                val positionResult = sensorModePositionCalculator?.addDistance(processed.distanceDeltaM)
                positionResult?.let {
                    stateManager.updateSensorRouteProgress(
                        position = it.position,
                        distanceToRouteEndM = it.distanceToEndMeters,
                        routeProgressPercent = it.progressPercent
                    )
                }
            }
            .launchIn(scope)
    }


    private fun subscribeLocationData(scope: CoroutineScope) {
        locationJob = locationRepository.observeLocation()
            .onEach { trackPoint ->
                when (val result = processLocationDataUseCase?.invoke(trackPoint)?.getOrNull()) {
                    is LocationProcessingResult.FirstReading -> {
                        val lon = trackPoint.longitude
                        if (lon != null) {
                            stateManager.updateGpsPosition(GeoCoordinate(trackPoint.latitude, lon))
                        }
                    }

                    is LocationProcessingResult.Processed -> {
                        stateManager.updateGpsPosition(result.position)
                    }

                    else -> {}
                }
            }
            .launchIn(scope)
    }

    private fun subscribeMetrics(scope: CoroutineScope) {
        metricsAggregator?.let { aggregator ->
            metricsJob = aggregator.metrics
                .onEach { metrics ->
                    stateManager.updateMetrics(metrics)
                }
                .launchIn(scope)
        }
    }


    internal fun cancelDataSources() {
        sensorJob?.cancel()
        sensorJob = null
        locationJob?.cancel()
        locationJob = null
        metricsJob?.cancel()
        metricsJob = null

        processSensorDataUseCase?.reset()
        processLocationDataUseCase?.reset()
        sensorModePositionCalculator = null
    }

    // Для будущих тестов
    internal val activeSensorJob: Job?
        get() = sensorJob

    internal val activeLocationJob: Job?
        get() = locationJob
}
