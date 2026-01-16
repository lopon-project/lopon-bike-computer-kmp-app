package ru.lopon.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.lopon.core.IdGenerator
import ru.lopon.core.TimeProvider
import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.Trip
import ru.lopon.domain.repository.LocationRepository
import ru.lopon.domain.repository.SensorRepository
import ru.lopon.domain.state.TripState
import ru.lopon.domain.state.TripStateManager


class StartTripUseCase(
    private val stateManager: TripStateManager,
    private val sensorRepository: SensorRepository,
    private val locationRepository: LocationRepository,
    private val timeProvider: TimeProvider,
    private val idGenerator: IdGenerator
) {
    private var sensorJob: Job? = null
    private var locationJob: Job? = null

    suspend operator fun invoke(mode: NavigationMode, scope: CoroutineScope): Result<Trip> {
        if (stateManager.currentState !is TripState.Idle) {
            return Result.failure(IllegalStateException("Cannot start trip: trip already active"))
        }

        val initResult = initializeDataSources(mode, scope)
        if (initResult.isFailure) {
            return Result.failure(initResult.exceptionOrNull()!!)
        }

        val trip = Trip(
            id = idGenerator.generateId(),
            startTimeUtc = timeProvider.currentTimeMillis(),
            mode = mode
        )

        // Переводим состояние в Recording
        val started = stateManager.startTrip(trip, mode)
        if (!started) {
            cancelDataSources()
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
            //TODO: в отдельном useCase будет обработка данных с датчика
            }
            .launchIn(scope)
    }


    private fun subscribeLocationData(scope: CoroutineScope) {
        locationJob = locationRepository.observeLocation()
            .onEach { location ->
                //TODO: в отдельном useCase будет обработка данных с датчика
            }
            .launchIn(scope)
    }


    internal fun cancelDataSources() {
        sensorJob?.cancel()
        sensorJob = null
        locationJob?.cancel()
        locationJob = null
    }

    // Для будущих тестов
    internal val activeSensorJob: Job?
        get() = sensorJob

    internal val activeLocationJob: Job?
        get() = locationJob
}

