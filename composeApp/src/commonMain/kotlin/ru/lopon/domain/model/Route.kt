package ru.lopon.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class Route(
    val id: String,
    val name: String,
    val points: List<GeoCoordinate>
) {
    val isValid: Boolean
        get() = points.size >= 2

    val pointCount: Int
        get() = points.size

    val startPoint: GeoCoordinate?
        get() = points.firstOrNull()

    val endPoint: GeoCoordinate?
        get() = points.lastOrNull()

    val distanceMeters: Double
        get() {
            if (points.size < 2) return 0.0
            var total = 0.0
            for (i in 0 until points.size - 1) {
                total += haversine(
                    points[i].latitude, points[i].longitude,
                    points[i + 1].latitude, points[i + 1].longitude
                )
            }
            return total
        }

    private companion object {
        private const val EARTH_RADIUS_METERS = 6371000.0
        private const val DEG_TO_RAD = PI / 180.0

        fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = (lat2 - lat1) * DEG_TO_RAD
            val dLon = (lon2 - lon1) * DEG_TO_RAD
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(lat1 * DEG_TO_RAD) * cos(lat2 * DEG_TO_RAD) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return EARTH_RADIUS_METERS * c
        }
    }
}

