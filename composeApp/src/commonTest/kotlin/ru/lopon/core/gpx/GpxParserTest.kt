package ru.lopon.core.gpx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GpxParserTest {

    private val parser = GpxParser()

    @Test
    fun `parse valid gpx with single track returns route`() {
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Test">
              <trk>
                <name>Test Route</name>
                <trkseg>
                  <trkpt lat="55.7558" lon="37.6173"/>
                  <trkpt lat="55.7600" lon="37.6200"/>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val result = parser.parse(gpx)

        assertTrue(result.isSuccess)
        val routes = result.getOrThrow()
        assertEquals(1, routes.size)
        assertEquals("Test Route", routes[0].name)
        assertEquals(2, routes[0].points.size)
        assertEquals(55.7558, routes[0].points[0].latitude, 0.0001)
        assertEquals(37.6173, routes[0].points[0].longitude, 0.0001)
    }

    @Test
    fun `parse gpx with multiple tracks returns all routes`() {
        val gpx = """
            <?xml version="1.0"?>
            <gpx version="1.1">
              <trk>
                <name>Route 1</name>
                <trkseg>
                  <trkpt lat="55.0" lon="37.0"/>
                  <trkpt lat="55.1" lon="37.1"/>
                </trkseg>
              </trk>
              <trk>
                <name>Route 2</name>
                <trkseg>
                  <trkpt lat="56.0" lon="38.0"/>
                  <trkpt lat="56.1" lon="38.1"/>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val result = parser.parse(gpx)

        assertTrue(result.isSuccess)
        val routes = result.getOrThrow()
        assertEquals(2, routes.size)
        assertEquals("Route 1", routes[0].name)
        assertEquals("Route 2", routes[1].name)
    }

    @Test
    fun `parse gpx with route element returns route`() {
        val gpx = """
            <?xml version="1.0"?>
            <gpx version="1.1">
              <rte>
                <name>Planned Route</name>
                <rtept lat="55.0" lon="37.0"/>
                <rtept lat="55.5" lon="37.5"/>
                <rtept lat="56.0" lon="38.0"/>
              </rte>
            </gpx>
        """.trimIndent()

        val result = parser.parse(gpx)

        assertTrue(result.isSuccess)
        val routes = result.getOrThrow()
        assertEquals(1, routes.size)
        assertEquals("Planned Route", routes[0].name)
        assertEquals(3, routes[0].points.size)
    }

    @Test
    fun `parse empty gpx returns failure`() {
        val gpx = """
            <?xml version="1.0"?>
            <gpx version="1.1">
            </gpx>
        """.trimIndent()

        val result = parser.parse(gpx)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is GpxParseException)
    }

    @Test
    fun `parseFirst returns first route`() {
        val gpx = """
            <?xml version="1.0"?>
            <gpx version="1.1">
              <trk>
                <name>First</name>
                <trkseg>
                  <trkpt lat="55.0" lon="37.0"/>
                  <trkpt lat="55.1" lon="37.1"/>
                </trkseg>
              </trk>
              <trk>
                <name>Second</name>
                <trkseg>
                  <trkpt lat="56.0" lon="38.0"/>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val result = parser.parseFirst(gpx)

        assertTrue(result.isSuccess)
        assertEquals("First", result.getOrThrow().name)
    }

    @Test
    fun `parse gpx with self-closing trkpt tags`() {
        val gpx = """
            <gpx version="1.1">
              <trk>
                <name>Test</name>
                <trkseg>
                  <trkpt lat="55.0" lon="37.0"/>
                  <trkpt lat="55.1" lon="37.1" />
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val result = parser.parse(gpx)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow()[0].points.size)
    }

    @Test
    fun `parse gpx generates default name if missing`() {
        val gpx = """
            <gpx version="1.1">
              <trk>
                <trkseg>
                  <trkpt lat="55.0" lon="37.0"/>
                  <trkpt lat="55.1" lon="37.1"/>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val result = parser.parse(gpx)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow()[0].name.contains("Track"))
    }
}
