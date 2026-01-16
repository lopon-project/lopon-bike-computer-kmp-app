package ru.lopon.domain.usecase

import ru.lopon.domain.repository.LocationRepository
import ru.lopon.domain.repository.SensorRepository
import ru.lopon.domain.state.TripState
import ru.lopon.domain.state.TripStateManager


class PauseTripUseCase(
    private val stateManager: TripStateManager,
    private val sensorRepository: SensorRepository,
    private val locationRepository: LocationRepository
) {

    suspend operator fun invoke(): Result<Unit> {
        val currentState = stateManager.currentState
        if (currentState !is TripState.Recording) {
            return Result.failure(IllegalStateException("Cannot pause: trip is not recording"))
        }

        val paused = stateManager.pauseTrip()
        if (!paused) {
            return Result.failure(IllegalStateException("Failed to transition to Paused state"))
        }


        return Result.success(Unit)
    }
}

