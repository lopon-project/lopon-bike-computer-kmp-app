package ru.lopon.domain.processing

import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SensorModePositionCalculatorTest {

    private fun createTestRoute(): Route {
        return Route(
            id = "test",
            name = "Test Route",
            points = listOf(
                GeoCoordinate(55.75, 37.62),
                GeoCoordinate(55.76, 37.62),
                GeoCoordinate(55.76, 37.63)
            )
        )
    }

    @Test
    fun `constructor calculates route length`() {
        val route = createTestRoute()
        val calculator = SensorModePositionCalculator(route)

        val routeLength = calculator.getRouteLength()

        assertTrue(routeLength > 1500 && routeLength < 2500)
    }

    @Test
    fun `getCurrentPosition returns start position initially`() {
        val route = createTestRoute()
        val calculator = SensorModePositionCalculator(route)

        val position = calculator.getCurrentPosition()

        assertNotNull(position)
        assertEquals(55.75, position.position.latitude, 0.001)
        assertEquals(37.62, position.position.longitude, 0.001)
        assertEquals(0.0, position.distanceAlongRouteMeters, 0.1)
        assertEquals(0, position.segmentIndex)
        assertFalse(position.isRouteCompleted)
    }

    @Test
    fun `addDistance moves position along route`() {
        val route = createTestRoute()
        val calculator = SensorModePositionCalculator(route)

        val position = calculator.addDistance(500.0)

        assertNotNull(position)
        assertTrue(position.position.latitude > 55.75 && position.position.latitude < 55.76)
        assertEquals(37.62, position.position.longitude, 0.001)
        assertEquals(500.0, position.distanceAlongRouteMeters, 1.0)
        assertEquals(0, position.segmentIndex)
    }

    @Test
    fun `addDistance transitions to second segment`() {
        val route = createTestRoute()
        val calculator = SensorModePositionCalculator(route)
        val firstSegmentLength = RouteCalculator.haversineDistance(
            route.points[0],
            route.points[1]
        )

        val position = calculator.addDistance(firstSegmentLength + 100.0)

        assertNotNull(position)
        assertEquals(1, position.segmentIndex)
        assertEquals(55.76, position.position.latitude, 0.001)
        assertTrue(position.position.longitude > 37.62)
    }

    @Test
    fun `addDistance sets isRouteCompleted at route end`() {
        val route = createTestRoute()
        val calculator = SensorModePositionCalculator(route)
        val routeLength = calculator.getRouteLength()

        val position = calculator.addDistance(routeLength + 100.0)

        assertNotNull(position)
        assertTrue(position.isRouteCompleted)
        assertEquals(100.0, position.progressPercent, 0.1)
    }

    @Test
    fun `progressPercent is calculated correctly`() {
        val route = createTestRoute()
        val calculator = SensorModePositionCalculator(route)
        val routeLength = calculator.getRouteLength()

        val position = calculator.addDistance(routeLength / 2)

        assertNotNull(position)
        assertEquals(50.0, position.progressPercent, 1.0)
    }

    @Test
    fun `reset clears state`() {
        val route = createTestRoute()
        val calculator = SensorModePositionCalculator(route)
        calculator.addDistance(1000.0)

        calculator.reset()

        assertEquals(0.0, calculator.getCurrentDistance())
        assertFalse(calculator.isRouteCompleted())
    }

    @Test
    fun `setInitialDistance sets starting point`() {
        val route = createTestRoute()
        val calculator = SensorModePositionCalculator(route)

        calculator.setInitialDistance(500.0)

        assertEquals(500.0, calculator.getCurrentDistance(), 1.0)
    }




    @Test
    fun `addDistance handles invalid route`() {
        val invalidRoute = Route(
            id = "invalid",
            name = "Invalid",
            points = listOf(GeoCoordinate(55.75, 37.62))
        )
        val calculator = SensorModePositionCalculator(invalidRoute)

        val position = calculator.addDistance(100.0)

        assertNull(position)
    }

    @Test
    fun `distanceToEndMeters is calculated correctly`() {
        val route = createTestRoute()
        val calculator = SensorModePositionCalculator(route)
        val routeLength = calculator.getRouteLength()

        val position = calculator.addDistance(routeLength / 2)

        assertNotNull(position)
        assertEquals(routeLength / 2, position.distanceToEndMeters, 10.0)
    }

    @Test
    fun `bearing is calculated for current segment`() {
        val route = createTestRoute()
        val calculator = SensorModePositionCalculator(route)

        val position = calculator.addDistance(100.0)

        assertNotNull(position)
        assertTrue(position.bearingDegrees !in 5.0..355.0)
    }

    @Test
    fun `cumulative distance is correct after multiple addDistance calls`() {
        val route = createTestRoute()
        val calculator = SensorModePositionCalculator(route)

        calculator.addDistance(100.0)
        calculator.addDistance(200.0)
        calculator.addDistance(300.0)

        assertEquals(600.0, calculator.getCurrentDistance(), 1.0)
    }
}

