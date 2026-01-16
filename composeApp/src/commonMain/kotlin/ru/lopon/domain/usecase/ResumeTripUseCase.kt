package ru.lopon.domain.usecase

import ru.lopon.domain.state.TripState
import ru.lopon.domain.state.TripStateManager

class ResumeTripUseCase(
    private val stateManager: TripStateManager
) {
    operator fun invoke(): Result<Unit> {
        val currentState = stateManager.currentState
        if (currentState !is TripState.Paused) {
            return Result.failure(IllegalStateException("Cannot resume: trip is not paused"))
        }

        val resumed = stateManager.resumeTrip()
        if (!resumed) {
            return Result.failure(IllegalStateException("Failed to transition to Recording state"))
        }

        return Result.success(Unit)
    }
}

