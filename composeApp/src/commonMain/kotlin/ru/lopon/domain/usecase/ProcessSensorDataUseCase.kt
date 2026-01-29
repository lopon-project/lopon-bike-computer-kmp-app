package ru.lopon.domain.usecase

import ru.lopon.core.DistanceCalculator
import ru.lopon.core.metrics.MetricsAggregator
import ru.lopon.core.settings.UnitConverter
import ru.lopon.domain.model.SensorReading
import ru.lopon.domain.processing.SensorDataFilter
import ru.lopon.domain.state.TripState
import ru.lopon.domain.state.TripStateManager

class ProcessSensorDataUseCase(
    private val stateManager: TripStateManager,
    private val metricsAggregator: MetricsAggregator,
    private val wheelCircumferenceMm: Double = DistanceCalculator.DEFAULT_WHEEL_CIRCUMFERENCE_MM
) {
    private var previousReading: SensorReading? = null


    operator fun invoke(reading: SensorReading): Result<SensorProcessingResult> {
        val currentState = stateManager.currentState
        if (currentState !is TripState.Recording) {
            return Result.failure(IllegalStateException("Cannot process sensor data: trip not recording"))
        }

        val prev = previousReading
        previousReading = reading

        if (prev == null) {
            return Result.success(SensorProcessingResult.FirstReading)
        }

        val revolutionsDelta = reading.revolutionsDelta(prev)
        val timeDeltaSeconds = reading.timeDeltaSeconds(prev)

        if (timeDeltaSeconds <= SensorDataFilter.MIN_TIME_DELTA_SECONDS) {
            return Result.success(SensorProcessingResult.Skipped("Time delta too small"))
        }

        val distanceDeltaM = DistanceCalculator.calculateDistanceMeters(
            revolutions = revolutionsDelta,
            wheelCircumferenceMm = wheelCircumferenceMm
        )

        val speedMs = distanceDeltaM / timeDeltaSeconds
        val speedKmh = UnitConverter.msToKmh(speedMs)

        if (speedKmh > SensorDataFilter.MAX_SPEED_KMH) {
            return Result.success(SensorProcessingResult.Skipped("Speed too high: $speedKmh km/h"))
        }

        metricsAggregator.processIncrement(
            distanceDeltaM = distanceDeltaM,
            speedMs = speedMs,
            cadenceRpm = reading.cadence
        )

        return Result.success(
            SensorProcessingResult.Processed(
                distanceDeltaM = distanceDeltaM,
                speedMs = speedMs,
                speedKmh = speedKmh,
                cadenceRpm = reading.cadence,
                revolutionsDelta = revolutionsDelta,
                timeDeltaSeconds = timeDeltaSeconds
            )
        )
    }


    fun reset() {
        previousReading = null
    }

    fun updateWheelCircumference(newCircumferenceMm: Double): ProcessSensorDataUseCase {
        return ProcessSensorDataUseCase(
            stateManager = stateManager,
            metricsAggregator = metricsAggregator,
            wheelCircumferenceMm = newCircumferenceMm
        )
    }
}

sealed class SensorProcessingResult {
    data object FirstReading : SensorProcessingResult()

    data class Skipped(val reason: String) : SensorProcessingResult()

    data class Processed(
        val distanceDeltaM: Double,
        val speedMs: Double,
        val speedKmh: Double,
        val cadenceRpm: Double?,
        val revolutionsDelta: Long,
        val timeDeltaSeconds: Double
    ) : SensorProcessingResult()
}
