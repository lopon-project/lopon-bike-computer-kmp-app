package ru.lopon.domain.processing

import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteCalculatorTest {

    @Test
    fun `haversineDistance calculates correct distance between Moscow and SPb`() {
        val moscow = GeoCoordinate(55.7558, 37.6173)
        val spb = GeoCoordinate(59.9343, 30.3351)

        val distance = RouteCalculator.haversineDistance(moscow, spb)

        assertTrue(distance > 620_000 && distance < 640_000, "Distance: $distance")
    }

    @Test
    fun `haversineDistance returns zero for same point`() {
        val point = GeoCoordinate(55.7558, 37.6173)

        val distance = RouteCalculator.haversineDistance(point, point)

        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `haversineDistance calculates short distance correctly`() {
        val p1 = GeoCoordinate(55.75580, 37.61730)
        val p2 = GeoCoordinate(55.75670, 37.61730)

        val distance = RouteCalculator.haversineDistance(p1, p2)

        assertTrue(distance > 95 && distance < 105, "Distance: $distance")
    }

    @Test
    fun `calculateRouteLength returns correct total distance`() {
        val route = Route(
            id = "test",
            name = "Test Route",
            points = listOf(
                GeoCoordinate(55.75, 37.62),
                GeoCoordinate(55.76, 37.62),
                GeoCoordinate(55.76, 37.63)
            )
        )

        val length = RouteCalculator.calculateRouteLength(route)

        assertTrue(length > 1500 && length < 2500, "Length: $length")
    }

    @Test
    fun `calculateRouteLength returns zero for empty route`() {
        val route = Route(id = "test", name = "Empty", points = emptyList())

        val length = RouteCalculator.calculateRouteLength(route)

        assertEquals(0.0, length)
    }

    @Test
    fun `calculateRouteLength returns zero for single point route`() {
        val route = Route(
            id = "test",
            name = "Single",
            points = listOf(GeoCoordinate(55.75, 37.62))
        )

        val length = RouteCalculator.calculateRouteLength(route)

        assertEquals(0.0, length)
    }

    @Test
    fun `positionAtDistance returns start point for zero distance`() {
        val startPoint = GeoCoordinate(55.75, 37.62)
        val route = Route(
            id = "test",
            name = "Test",
            points = listOf(
                startPoint,
                GeoCoordinate(55.76, 37.62)
            )
        )

        val position = RouteCalculator.positionAtDistance(route, 0.0)

        assertNotNull(position)
        assertEquals(startPoint.latitude, position.point.latitude, 0.0001)
        assertEquals(startPoint.longitude, position.point.longitude, 0.0001)
    }

    @Test
    fun `positionAtDistance returns end point for distance exceeding route length`() {
        val endPoint = GeoCoordinate(55.76, 37.62)
        val route = Route(
            id = "test",
            name = "Test",
            points = listOf(
                GeoCoordinate(55.75, 37.62),
                endPoint
            )
        )
        val routeLength = RouteCalculator.calculateRouteLength(route)

        val position = RouteCalculator.positionAtDistance(route, routeLength + 1000.0)

        assertNotNull(position)
        assertEquals(endPoint.latitude, position.point.latitude, 0.0001)
        assertEquals(endPoint.longitude, position.point.longitude, 0.0001)
    }

    @Test
    fun `positionAtDistance returns interpolated point for mid-route distance`() {
        val route = Route(
            id = "test",
            name = "Test",
            points = listOf(
                GeoCoordinate(55.75, 37.62),
                GeoCoordinate(55.76, 37.62)
            )
        )
        val routeLength = RouteCalculator.calculateRouteLength(route)

        val position = RouteCalculator.positionAtDistance(route, routeLength / 2)

        assertNotNull(position)
        assertTrue(position.point.latitude > 55.754 && position.point.latitude < 55.756)
        assertEquals(37.62, position.point.longitude, 0.0001)
    }

    @Test
    fun `positionAtDistance returns null for invalid route`() {
        val route = Route(id = "test", name = "Invalid", points = listOf(GeoCoordinate(55.75, 37.62)))

        val position = RouteCalculator.positionAtDistance(route, 100.0)

        assertNull(position)
    }

    @Test
    fun `snapToRoute finds closest point on route`() {
        val route = Route(
            id = "test",
            name = "Test",
            points = listOf(
                GeoCoordinate(55.75, 37.62),
                GeoCoordinate(55.76, 37.62)
            )
        )
        val point = GeoCoordinate(55.755, 37.625)

        val snapResult = RouteCalculator.snapToRoute(point, route)

        assertNotNull(snapResult)
        assertEquals(37.62, snapResult.snappedPoint.longitude, 0.001)
        assertTrue(snapResult.snappedPoint.latitude > 55.754 && snapResult.snappedPoint.latitude < 55.756)
        assertTrue(snapResult.perpendicularDistance > 300 && snapResult.perpendicularDistance < 400)
    }

    @Test
    fun `snapToRoute returns null for invalid route`() {
        val route = Route(id = "test", name = "Invalid", points = emptyList())
        val point = GeoCoordinate(55.75, 37.62)

        val snapResult = RouteCalculator.snapToRoute(point, route)

        assertNull(snapResult)
    }

    @Test
    fun `snapToRoute finds correct segment`() {
        val route = Route(
            id = "test",
            name = "Test",
            points = listOf(
                GeoCoordinate(55.75, 37.62),
                GeoCoordinate(55.76, 37.62),
                GeoCoordinate(55.76, 37.63)
            )
        )
        val point = GeoCoordinate(55.76, 37.625)

        val snapResult = RouteCalculator.snapToRoute(point, route)

        assertNotNull(snapResult)
        assertEquals(1, snapResult.segmentIndex)
    }

    @Test
    fun `calculateBearing returns correct north bearing`() {
        val p1 = GeoCoordinate(55.75, 37.62)
        val p2 = GeoCoordinate(55.76, 37.62)

        val bearing = RouteCalculator.calculateBearing(p1, p2)

        assertTrue(bearing !in 5.0..355.0, "Bearing: $bearing")
    }

    @Test
    fun `calculateBearing returns correct east bearing`() {
        val p1 = GeoCoordinate(55.75, 37.62)
        val p2 = GeoCoordinate(55.75, 37.63)

        val bearing = RouteCalculator.calculateBearing(p1, p2)
        assertTrue(bearing > 85 && bearing < 95, "Bearing: $bearing")
    }

    @Test
    fun `interpolate returns midpoint correctly`() {
        val p1 = GeoCoordinate(55.75, 37.62)
        val p2 = GeoCoordinate(55.76, 37.64)

        val midpoint = RouteCalculator.interpolate(p1, p2, 0.5)

        assertEquals(55.755, midpoint.latitude, 0.0001)
        assertEquals(37.63, midpoint.longitude, 0.0001)
    }

    @Test
    fun `interpolate returns start point for fraction 0`() {
        val p1 = GeoCoordinate(55.75, 37.62)
        val p2 = GeoCoordinate(55.76, 37.64)

        val point = RouteCalculator.interpolate(p1, p2, 0.0)

        assertEquals(p1.latitude, point.latitude)
        assertEquals(p1.longitude, point.longitude)
    }

    @Test
    fun `interpolate returns end point for fraction 1`() {
        val p1 = GeoCoordinate(55.75, 37.62)
        val p2 = GeoCoordinate(55.76, 37.64)

        val point = RouteCalculator.interpolate(p1, p2, 1.0)

        assertEquals(p2.latitude, point.latitude)
        assertEquals(p2.longitude, point.longitude)
    }
}

