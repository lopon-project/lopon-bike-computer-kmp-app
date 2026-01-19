package ru.lopon.domain.usecase

import ru.lopon.core.metrics.MetricsAggregator
import ru.lopon.domain.state.TripState
import ru.lopon.domain.state.TripStateManager

class ResumeTripUseCase(
    private val stateManager: TripStateManager,
    private val metricsAggregator: MetricsAggregator? = null
) {
    operator fun invoke(): Result<Unit> {
        val currentState = stateManager.currentState
        if (currentState !is TripState.Paused) {
            return Result.failure(IllegalStateException("Cannot resume: trip is not paused"))
        }

        metricsAggregator?.resume()

        val resumed = stateManager.resumeTrip()
        if (!resumed) {
            metricsAggregator?.pause()
            return Result.failure(IllegalStateException("Failed to transition to Recording state"))
        }

        return Result.success(Unit)
    }
}

