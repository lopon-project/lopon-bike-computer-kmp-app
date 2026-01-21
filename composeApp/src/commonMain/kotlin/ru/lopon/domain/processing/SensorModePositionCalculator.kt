package ru.lopon.domain.processing

import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.Route


class SensorModePositionCalculator(
    private val route: Route
) {

    data class State(
        val distanceAlongRouteMeters: Double = 0.0,
        val currentSegmentIndex: Int = 0,
        val isRouteCompleted: Boolean = false
    )

    data class PositionResult(
        val position: GeoCoordinate,
        val distanceAlongRouteMeters: Double,
        val distanceToEndMeters: Double,
        val bearingDegrees: Double,
        val segmentIndex: Int,
        val isRouteCompleted: Boolean,
        val progressPercent: Double
    )

    private var state = State()

    private val routeLength: Double = RouteCalculator.calculateRouteLength(route)

    fun reset() {
        state = State()
    }

    fun setInitialDistance(distanceMeters: Double) {
        state = state.copy(
            distanceAlongRouteMeters = distanceMeters.coerceIn(0.0, routeLength),
            isRouteCompleted = distanceMeters >= routeLength
        )
    }


    fun addDistance(distanceIncrementMeters: Double): PositionResult? {
        if (!route.isValid || routeLength <= 0) return null

        val newDistance = (state.distanceAlongRouteMeters + distanceIncrementMeters)
            .coerceIn(0.0, routeLength)

        val routePosition = RouteCalculator.positionAtDistance(route, newDistance) ?: return null

        val isCompleted = newDistance >= routeLength
        val distanceToEnd = (routeLength - newDistance).coerceAtLeast(0.0)
        val progress = (newDistance / routeLength * 100).coerceIn(0.0, 100.0)

        state = state.copy(
            distanceAlongRouteMeters = newDistance,
            currentSegmentIndex = routePosition.segmentIndex,
            isRouteCompleted = isCompleted
        )

        return PositionResult(
            position = routePosition.point,
            distanceAlongRouteMeters = newDistance,
            distanceToEndMeters = distanceToEnd,
            bearingDegrees = routePosition.bearing,
            segmentIndex = routePosition.segmentIndex,
            isRouteCompleted = isCompleted,
            progressPercent = progress
        )
    }

    fun getCurrentPosition(): PositionResult? {
        if (!route.isValid || routeLength <= 0) return null

        val routePosition = RouteCalculator.positionAtDistance(route, state.distanceAlongRouteMeters)
            ?: return null

        val distanceToEnd = (routeLength - state.distanceAlongRouteMeters).coerceAtLeast(0.0)
        val progress = (state.distanceAlongRouteMeters / routeLength * 100).coerceIn(0.0, 100.0)

        return PositionResult(
            position = routePosition.point,
            distanceAlongRouteMeters = state.distanceAlongRouteMeters,
            distanceToEndMeters = distanceToEnd,
            bearingDegrees = routePosition.bearing,
            segmentIndex = routePosition.segmentIndex,
            isRouteCompleted = state.isRouteCompleted,
            progressPercent = progress
        )
    }

    fun getRouteLength(): Double = routeLength

    fun getCurrentDistance(): Double = state.distanceAlongRouteMeters

    fun isRouteCompleted(): Boolean = state.isRouteCompleted
}

