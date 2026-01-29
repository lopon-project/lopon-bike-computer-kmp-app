package ru.lopon.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TrackPoint(
    val latitude: Double,
    val longitude: Double? = null,
    val timestampUtc: Long? = null,
    val source: String? = null,
    val elevation: Double? = null,
    val speed: Double? = null
) {
    companion object {
        const val SOURCE_GPS = "gps"
        const val SOURCE_SENSOR = "sensor"

        const val SOURCE_HYBRID = "hybrid"
    }
}

