package ru.lopon.domain.processing

import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.Route
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


object RouteCalculator {

    private const val EARTH_RADIUS_METERS = 6371000.0

    data class SnapResult(
        val snappedPoint: GeoCoordinate,
        val segmentIndex: Int,
        val distanceAlongRoute: Double,
        val perpendicularDistance: Double
    )

    data class PositionOnRoute(
        val point: GeoCoordinate,
        val segmentIndex: Int,
        val bearing: Double
    )

    fun haversineDistance(p1: GeoCoordinate, p2: GeoCoordinate): Double {
        val lat1Rad = p1.latitude.toRadians()
        val lat2Rad = p2.latitude.toRadians()
        val deltaLat = (p2.latitude - p1.latitude).toRadians()
        val deltaLon = (p2.longitude - p1.longitude).toRadians()

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }


    fun calculateRouteLength(route: Route): Double {
        if (route.points.size < 2) return 0.0

        var totalDistance = 0.0
        for (i in 0 until route.points.size - 1) {
            totalDistance += haversineDistance(route.points[i], route.points[i + 1])
        }
        return totalDistance
    }

    fun calculateCumulativeDistances(route: Route): List<Double> {
        if (route.points.isEmpty()) return emptyList()

        val distances = mutableListOf(0.0)
        for (i in 0 until route.points.size - 1) {
            val segmentDistance = haversineDistance(route.points[i], route.points[i + 1])
            distances.add(distances.last() + segmentDistance)
        }
        return distances
    }


    fun positionAtDistance(route: Route, distanceAlongRoute: Double): PositionOnRoute? {
        if (!route.isValid) return null

        val cumulativeDistances = calculateCumulativeDistances(route)
        val totalLength = cumulativeDistances.last()

        if (distanceAlongRoute >= totalLength) {
            val lastPoint = route.points.last()
            val bearing = if (route.points.size >= 2) {
                calculateBearing(
                    route.points[route.points.size - 2],
                    lastPoint
                )
            } else 0.0
            return PositionOnRoute(
                point = lastPoint,
                segmentIndex = route.points.size - 2,
                bearing = bearing
            )
        }

        if (distanceAlongRoute <= 0) {
            val bearing = if (route.points.size >= 2) {
                calculateBearing(route.points[0], route.points[1])
            } else 0.0
            return PositionOnRoute(
                point = route.points[0],
                segmentIndex = 0,
                bearing = bearing
            )
        }

        for (i in 0 until cumulativeDistances.size - 1) {
            val startDistance = cumulativeDistances[i]
            val endDistance = cumulativeDistances[i + 1]

            if (distanceAlongRoute in startDistance..endDistance) {
                val segmentLength = endDistance - startDistance
                if (segmentLength < 0.001) {
                    return PositionOnRoute(
                        point = route.points[i],
                        segmentIndex = i,
                        bearing = calculateBearing(route.points[i], route.points[i + 1])
                    )
                }

                val fraction = (distanceAlongRoute - startDistance) / segmentLength
                val interpolatedPoint = interpolate(route.points[i], route.points[i + 1], fraction)
                val bearing = calculateBearing(route.points[i], route.points[i + 1])

                return PositionOnRoute(
                    point = interpolatedPoint,
                    segmentIndex = i,
                    bearing = bearing
                )
            }
        }

        return PositionOnRoute(
            point = route.points.last(),
            segmentIndex = route.points.size - 2,
            bearing = 0.0
        )
    }


    fun snapToRoute(point: GeoCoordinate, route: Route): SnapResult? {
        if (!route.isValid) return null

        val cumulativeDistances = calculateCumulativeDistances(route)

        var bestSnapResult: SnapResult? = null
        var minDistance = Double.MAX_VALUE

        for (i in 0 until route.points.size - 1) {
            val segmentStart = route.points[i]
            val segmentEnd = route.points[i + 1]

            val closestOnSegment = closestPointOnSegment(point, segmentStart, segmentEnd)
            val distance = haversineDistance(point, closestOnSegment.point)

            if (distance < minDistance) {
                minDistance = distance

                val segmentLength = haversineDistance(segmentStart, segmentEnd)
                val distanceOnSegment = closestOnSegment.fraction * segmentLength
                val distanceAlongRoute = cumulativeDistances[i] + distanceOnSegment

                bestSnapResult = SnapResult(
                    snappedPoint = closestOnSegment.point,
                    segmentIndex = i,
                    distanceAlongRoute = distanceAlongRoute,
                    perpendicularDistance = distance
                )
            }
        }

        return bestSnapResult
    }


    private data class ClosestPointResult(
        val point: GeoCoordinate,
        val fraction: Double
    )


    private fun closestPointOnSegment(
        point: GeoCoordinate,
        segmentStart: GeoCoordinate,
        segmentEnd: GeoCoordinate
    ): ClosestPointResult {
        val cosLat = cos(point.latitude.toRadians())

        val px = point.longitude * cosLat
        val py = point.latitude
        val ax = segmentStart.longitude * cosLat
        val ay = segmentStart.latitude
        val bx = segmentEnd.longitude * cosLat
        val by = segmentEnd.latitude

        val abx = bx - ax
        val aby = by - ay
        val apx = px - ax
        val apy = py - ay

        val abSquared = abx * abx + aby * aby

        if (abSquared < 1e-12) {
            return ClosestPointResult(segmentStart, 0.0)
        }

        val t = (apx * abx + apy * aby) / abSquared
        val tClamped = t.coerceIn(0.0, 1.0)

        return ClosestPointResult(
            point = interpolate(segmentStart, segmentEnd, tClamped),
            fraction = tClamped
        )
    }

    fun interpolate(p1: GeoCoordinate, p2: GeoCoordinate, fraction: Double): GeoCoordinate {
        val lat = p1.latitude + (p2.latitude - p1.latitude) * fraction
        val lon = p1.longitude + (p2.longitude - p1.longitude) * fraction
        return GeoCoordinate(lat, lon)
    }

    fun calculateBearing(p1: GeoCoordinate, p2: GeoCoordinate): Double {
        val lat1 = p1.latitude.toRadians()
        val lat2 = p2.latitude.toRadians()
        val deltaLon = (p2.longitude - p1.longitude).toRadians()

        val x = sin(deltaLon) * cos(lat2)
        val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)

        val bearing = atan2(x, y).toDegrees()
        return (bearing + 360) % 360
    }

    fun movePoint(start: GeoCoordinate, bearingDegrees: Double, distanceMeters: Double): GeoCoordinate {
        val earthRadius = EARTH_RADIUS_METERS

        val lat1 = start.latitude.toRadians()
        val lon1 = start.longitude.toRadians()
        val bearing = bearingDegrees.toRadians()
        val angularDistance = distanceMeters / earthRadius

        val lat2 = kotlin.math.asin(
            sin(lat1) * cos(angularDistance) +
            cos(lat1) * sin(angularDistance) * cos(bearing)
        )

        val lon2 = lon1 + atan2(
            sin(bearing) * sin(angularDistance) * cos(lat1),
            cos(angularDistance) - sin(lat1) * sin(lat2)
        )

        return GeoCoordinate(
            latitude = lat2.toDegrees(),
            longitude = lon2.toDegrees()
        )
    }

    private fun Double.toRadians(): Double = this * PI / 180.0

    private fun Double.toDegrees(): Double = this * 180.0 / PI
}

