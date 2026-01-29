package ru.lopon.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SensorReading(
    val cumulativeRevolutions: Long,
    val wheelEventTimeUnits: Int,
    val timestampUtc: Long? = null,
    val cadence: Double? = null
) {
    fun revolutionsDelta(previousReading: SensorReading): Long {
        val delta = cumulativeRevolutions - previousReading.cumulativeRevolutions
        return if (delta >= 0) delta else delta + (1L shl 32)
    }

    fun wheelEventTimeDelta(previousReading: SensorReading): Int {
        var delta = wheelEventTimeUnits - previousReading.wheelEventTimeUnits
        if (delta < 0) {
            delta += 0x10000
        }
        return delta
    }

    fun timeDeltaSeconds(previousReading: SensorReading): Double {
        val deltaUnits = wheelEventTimeDelta(previousReading)
        return deltaUnits / 1024.0
    }

    fun timestampDeltaMs(previousReading: SensorReading): Long {
        val current = timestampUtc ?: return 0
        val previous = previousReading.timestampUtc ?: return 0
        return (current - previous).coerceAtLeast(0)
    }
}

