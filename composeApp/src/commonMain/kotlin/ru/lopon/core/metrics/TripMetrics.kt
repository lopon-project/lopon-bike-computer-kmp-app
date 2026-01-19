package ru.lopon.core.metrics

import kotlinx.serialization.Serializable

@Serializable
data class TripMetrics(
    val currentSpeedMs: Double = 0.0,
    val averageSpeedMs: Double = 0.0,
    val maxSpeedMs: Double = 0.0,
    val totalDistanceM: Double = 0.0,
    val currentCadenceRpm: Double? = null,
    val averageCadenceRpm: Double? = null,
    val movingTimeMs: Long = 0,
    val elapsedTimeMs: Long = 0,
    val elevationGainM: Double? = null
) {
    val currentSpeedKmh: Double get() = currentSpeedMs * 3.6

    val averageSpeedKmh: Double get() = averageSpeedMs * 3.6

    val maxSpeedKmh: Double get() = maxSpeedMs * 3.6

    val totalDistanceKm: Double get() = totalDistanceM / 1000.0

    companion object {
        val ZERO = TripMetrics()
    }
}

