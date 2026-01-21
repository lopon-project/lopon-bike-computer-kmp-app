package ru.lopon.domain.usecase

import ru.lopon.core.TimeProvider
import ru.lopon.core.metrics.MetricsAggregator
import ru.lopon.domain.model.Trip
import ru.lopon.domain.repository.LocationRepository
import ru.lopon.domain.repository.SensorRepository
import ru.lopon.domain.repository.TripRepository
import ru.lopon.domain.state.TripState
import ru.lopon.domain.state.TripStateManager

class StopTripUseCase(
    private val stateManager: TripStateManager,
    private val tripRepository: TripRepository,
    private val sensorRepository: SensorRepository,
    private val locationRepository: LocationRepository,
    private val timeProvider: TimeProvider,
    private val startTripUseCase: StartTripUseCase? = null,
    private val metricsAggregator: MetricsAggregator? = null
) {

    suspend operator fun invoke(): Result<Trip> {
        val currentState = stateManager.currentState

        val (trip, distanceMeters, currentMetrics) = when (currentState) {
            is TripState.Recording -> Triple(currentState.trip, currentState.distanceMeters, currentState.metrics)
            is TripState.Paused -> Triple(currentState.trip, currentState.distanceMeters, currentState.metrics)
            else -> return Result.failure(
                IllegalStateException("Cannot stop: no active trip (current state: ${currentState::class.simpleName})")
            )
        }

        stopDataSources()

        startTripUseCase?.cancelDataSources()

        val finalMetrics = metricsAggregator?.currentMetrics ?: currentMetrics

        metricsAggregator?.reset()

        val finishedTrip = trip.copy(
            endTimeUtc = timeProvider.currentTimeMillis(),
            distanceMeters = finalMetrics.totalDistanceM
        )

        val saveResult = tripRepository.saveTrip(finishedTrip)
        if (saveResult.isFailure) {
            return Result.failure(
                saveResult.exceptionOrNull() ?: Exception("Failed to save trip")
            )
        }

        val stopped = stateManager.stopTrip(finishedTrip, finalMetrics)
        if (!stopped) {
            return Result.failure(IllegalStateException("Failed to transition to Finished state"))
        }

        return Result.success(finishedTrip)
    }

    private suspend fun stopDataSources() {
        try {
            sensorRepository.stopAndDisconnect()
        } catch (_: Exception) {
        }

        try {
            locationRepository.stopTracking()
        } catch (_: Exception) {
        }
    }
}

