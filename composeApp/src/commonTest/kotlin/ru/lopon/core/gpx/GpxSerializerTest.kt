package ru.lopon.core.gpx

import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.Route
import ru.lopon.domain.model.TrackPoint
import ru.lopon.domain.model.Trip
import ru.lopon.domain.model.NavigationMode
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class GpxSerializerTest {

    private val serializer = GpxSerializer()

    @Test
    fun `serializeRoute produces valid gpx`() {
        val route = Route(
            id = "test-1",
            name = "Test Route",
            points = listOf(
                GeoCoordinate(55.7558, 37.6173),
                GeoCoordinate(55.7600, 37.6200)
            )
        )

        val gpx = serializer.serializeRoute(route)

        assertTrue(gpx.contains("<?xml version=\"1.0\""))
        assertTrue(gpx.contains("<gpx version=\"1.1\""))
        assertTrue(gpx.contains("<name>Test Route</name>"))
        assertTrue(gpx.contains("<trkpt lat=\"55.7558\" lon=\"37.6173\""))
        assertTrue(gpx.contains("<trkpt lat=\"55.76\" lon=\"37.62\""))
        assertTrue(gpx.contains("</gpx>"))
    }

    @Test
    fun `serializeTrip includes track points`() {
        val trip = Trip(
            id = "trip-1",
            startTimeUtc = 1705700000000,
            mode = NavigationMode.Gps
        )

        val trackPoints = listOf(
            TrackPoint(55.7558, 37.6173, 1705700000000, "gps", 150.0, 5.0),
            TrackPoint(55.7600, 37.6200, 1705700060000, "gps", 155.0, 6.0)
        )

        val gpx = serializer.serializeTrip(trip, trackPoints, "My Trip")

        assertTrue(gpx.contains("<name>My Trip</name>"))
        assertTrue(gpx.contains("<type>cycling</type>"))
        assertTrue(gpx.contains("<trkpt lat=\"55.7558\" lon=\"37.6173\""))
        assertTrue(gpx.contains("<ele>150.0</ele>"))
        assertTrue(gpx.contains("<speed>5.0</speed>"))
    }

    @Test
    fun `serializeCoordinates creates simple gpx`() {
        val points = listOf(
            GeoCoordinate(55.0, 37.0),
            GeoCoordinate(55.5, 37.5),
            GeoCoordinate(56.0, 38.0)
        )

        val gpx = serializer.serializeCoordinates(points, "Simple Route")

        assertTrue(gpx.contains("<name>Simple Route</name>"))
        assertTrue(gpx.contains("<trkpt lat=\"55.0\" lon=\"37.0\""))
        assertTrue(gpx.contains("<trkpt lat=\"55.5\" lon=\"37.5\""))
        assertTrue(gpx.contains("<trkpt lat=\"56.0\" lon=\"38.0\""))
    }

    @Test
    fun `serializer escapes xml special characters`() {
        val route = Route(
            id = "test-1",
            name = "Route with <special> & \"chars\"",
            points = listOf(GeoCoordinate(55.0, 37.0), GeoCoordinate(56.0, 38.0))
        )

        val gpx = serializer.serializeRoute(route)

        assertTrue(gpx.contains("&lt;special&gt;"))
        assertTrue(gpx.contains("&amp;"))
        assertTrue(gpx.contains("&quot;chars&quot;"))
        assertFalse(gpx.contains("<special>"))
    }

    @Test
    fun `round trip parse and serialize preserves points`() {
        val original = Route(
            id = "test-1",
            name = "Round Trip Test",
            points = listOf(
                GeoCoordinate(55.7558, 37.6173),
                GeoCoordinate(55.8000, 37.7000)
            )
        )

        val gpx = serializer.serializeRoute(original)
        val parsed = GpxParser().parseFirst(gpx)

        assertTrue(parsed.isSuccess)
        val route = parsed.getOrThrow()
        assertTrue(route.points.size == 2)
        assertTrue(kotlin.math.abs(route.points[0].latitude - 55.7558) < 0.001)
        assertTrue(kotlin.math.abs(route.points[0].longitude - 37.6173) < 0.001)
    }
}
