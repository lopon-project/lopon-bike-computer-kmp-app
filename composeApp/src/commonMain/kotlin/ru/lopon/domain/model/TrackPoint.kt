package ru.lopon.domain.model

import kotlinx.serialization.Serializable

/**
 * Точка трека с географическими координатами и временной меткой.
 *
 * @property latitude Широта в градусах (-90 до +90).
 * @property longitude Долгота в градусах (-180 до +180), null если позиция определена только по датчику.
 * @property timestampUtc Временная метка в миллисекундах UTC.
 * @property source Источник данных точки ("gps", "sensor", "hybrid" или null).
 */
@Serializable
data class TrackPoint(
    val latitude: Double,
    val longitude: Double? = null,
    val timestampUtc: Long,
    val source: String? = null
) {
    companion object {
        const val SOURCE_GPS = "gps"

        const val SOURCE_SENSOR = "sensor"

        const val SOURCE_HYBRID = "hybrid"
    }
}

