package ru.lopon.domain.usecase

import ru.lopon.core.metrics.MetricsAggregator
import ru.lopon.core.settings.UnitConverter
import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.TrackPoint
import ru.lopon.domain.processing.RouteCalculator
import ru.lopon.domain.state.TripState
import ru.lopon.domain.state.TripStateManager

class ProcessLocationDataUseCase(
    private val stateManager: TripStateManager,
    private val metricsAggregator: MetricsAggregator
) {
    private var previousPoint: TrackPoint? = null
    private var previousTimestamp: Long? = null

    operator fun invoke(trackPoint: TrackPoint): Result<LocationProcessingResult> {
        val currentState = stateManager.currentState
        if (currentState !is TripState.Recording) {
            return Result.failure(IllegalStateException("Cannot process location: trip not recording"))
        }

        val prev = previousPoint
        val prevTime = previousTimestamp

        previousPoint = trackPoint
        previousTimestamp = trackPoint.timestampUtc

        if (prev == null || prevTime == null) {
            return Result.success(LocationProcessingResult.FirstReading)
        }

        val currentLon = trackPoint.longitude ?: return Result.success(
            LocationProcessingResult.Skipped("No longitude in track point")
        )
        val prevLon = prev.longitude ?: return Result.success(
            LocationProcessingResult.Skipped("No longitude in previous point")
        )

        val distanceDeltaM = RouteCalculator.haversineDistance(
            GeoCoordinate(prev.latitude, prevLon),
            GeoCoordinate(trackPoint.latitude, currentLon)
        )

        val currentTime = trackPoint.timestampUtc ?: return Result.success(
            LocationProcessingResult.Skipped("No timestamp in track point")
        )
        val timeDeltaMs = currentTime - prevTime
        val timeDeltaSeconds = timeDeltaMs / 1000.0

        if (timeDeltaSeconds < 0.1) {
            return Result.success(LocationProcessingResult.Skipped("Time delta too small"))
        }

        val speedMs = trackPoint.speed ?: (distanceDeltaM / timeDeltaSeconds)
        val speedKmh = UnitConverter.msToKmh(speedMs)

        if (speedKmh > 150.0) {
            return Result.success(LocationProcessingResult.Skipped("Speed too high: $speedKmh km/h"))
        }

        if (distanceDeltaM > 100.0 && timeDeltaSeconds < 2.0) {
            return Result.success(LocationProcessingResult.Skipped("Position jump detected: $distanceDeltaM m"))
        }

        metricsAggregator.processIncrement(
            distanceDeltaM = distanceDeltaM,
            speedMs = speedMs,
            cadenceRpm = null,
            altitude = trackPoint.elevation
        )

        return Result.success(
            LocationProcessingResult.Processed(
                distanceDeltaM = distanceDeltaM,
                speedMs = speedMs,
                speedKmh = speedKmh,
                elevation = trackPoint.elevation,
                timeDeltaSeconds = timeDeltaSeconds,
                position = GeoCoordinate(trackPoint.latitude, currentLon)
            )
        )
    }


    fun reset() {
        previousPoint = null
        previousTimestamp = null
    }
}


sealed class LocationProcessingResult {
    data object FirstReading : LocationProcessingResult()

    data class Skipped(val reason: String) : LocationProcessingResult()

    data class Processed(
        val distanceDeltaM: Double,
        val speedMs: Double,
        val speedKmh: Double,
        val elevation: Double?,
        val timeDeltaSeconds: Double,
        val position: GeoCoordinate
    ) : LocationProcessingResult()
}
