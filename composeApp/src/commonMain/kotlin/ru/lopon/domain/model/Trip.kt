package ru.lopon.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Trip(
    val id: String,
    val startTimeUtc: Long,
    val endTimeUtc: Long? = null,
    val mode: NavigationMode,
    val distanceMeters: Double = 0.0,
    val movingTimeMs: Long = 0,
    val averageSpeedMs: Double? = null,
    val maxSpeedMs: Double? = null,
    val averageCadenceRpm: Double? = null,
    val elevationGainM: Double? = null,
    val routeId: String? = null
) {
    val isFinished: Boolean
        get() = endTimeUtc != null

    fun durationMs(currentTimeUtc: Long): Long {
        val end = endTimeUtc ?: currentTimeUtc
        return (end - startTimeUtc).coerceAtLeast(0)
    }
}

