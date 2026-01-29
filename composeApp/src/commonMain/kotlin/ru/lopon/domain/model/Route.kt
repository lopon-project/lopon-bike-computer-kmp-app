package ru.lopon.domain.model

import kotlinx.serialization.Serializable

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
}

