package ru.lopon.core.metrics

import kotlinx.serialization.Serializable
import ru.lopon.core.settings.UnitConverter

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
    val currentSpeedKmh: Double get() = UnitConverter.msToKmh(currentSpeedMs)

    val averageSpeedKmh: Double get() = UnitConverter.msToKmh(averageSpeedMs)

    val maxSpeedKmh: Double get() = UnitConverter.msToKmh(maxSpeedMs)

    val totalDistanceKm: Double get() = totalDistanceM / 1000.0

    companion object {
        val ZERO = TripMetrics()
    }
}

