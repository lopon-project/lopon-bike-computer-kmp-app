package ru.lopon.core.settings

import ru.lopon.domain.model.UnitSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnitConverterTest {


    @Test
    fun `10 km in metric returns 10`() {
        val result = UnitConverter.metersToDisplay(10000.0, UnitSystem.METRIC)
        assertEquals(10.0, result, 0.001)
    }

    @Test
    fun `10 km to miles`() {
        val miles = UnitConverter.metersToDisplay(10000.0, UnitSystem.IMPERIAL)
        assertEquals(6.21, miles, 0.01)
    }

    @Test
    fun `displayToMeters converts km back to meters`() {
        val meters = UnitConverter.displayToMeters(10.0, UnitSystem.METRIC)
        assertEquals(10000.0, meters, 0.001)
    }

    @Test
    fun `displayToMeters converts miles back to meters`() {
        val meters = UnitConverter.displayToMeters(6.21371, UnitSystem.IMPERIAL)
        assertEquals(10000.0, meters, 1.0) // Allow 1m tolerance
    }

    @Test
    fun `displayUnitDistance returns km for metric`() {
        assertEquals("km", UnitConverter.displayUnitDistance(UnitSystem.METRIC))
    }

    @Test
    fun `displayUnitDistance returns mi for imperial`() {
        assertEquals("mi", UnitConverter.displayUnitDistance(UnitSystem.IMPERIAL))
    }


    @Test
    fun `30 kmh in metric from ms`() {
        val speedMs = 30.0 / 3.6 // ~8.33 m/s
        val result = UnitConverter.msToDisplaySpeed(speedMs, UnitSystem.METRIC)
        assertEquals(30.0, result, 0.1)
    }

    @Test
    fun `30 kmh to mph`() {
        val speedMs = 30.0 / 3.6
        val mph = UnitConverter.msToDisplaySpeed(speedMs, UnitSystem.IMPERIAL)
        assertEquals(18.64, mph, 0.1)
    }

    @Test
    fun `kmhToDisplaySpeed metric returns same value`() {
        val result = UnitConverter.kmhToDisplaySpeed(25.0, UnitSystem.METRIC)
        assertEquals(25.0, result, 0.001)
    }

    @Test
    fun `kmhToDisplaySpeed imperial converts to mph`() {
        val result = UnitConverter.kmhToDisplaySpeed(100.0, UnitSystem.IMPERIAL)
        assertEquals(62.14, result, 0.1)
    }

    @Test
    fun `displaySpeedToMs converts back correctly`() {
        val ms = UnitConverter.displaySpeedToMs(36.0, UnitSystem.METRIC)
        assertEquals(10.0, ms, 0.01) // 36 km/h = 10 m/s
    }

    @Test
    fun `displayUnitSpeed returns kmh for metric`() {
        assertEquals("km/h", UnitConverter.displayUnitSpeed(UnitSystem.METRIC))
    }

    @Test
    fun `displayUnitSpeed returns mph for imperial`() {
        assertEquals("mph", UnitConverter.displayUnitSpeed(UnitSystem.IMPERIAL))
    }


    @Test
    fun `100 meters elevation in metric returns 100`() {
        val result = UnitConverter.metersToDisplayElevation(100.0, UnitSystem.METRIC)
        assertEquals(100.0, result, 0.001)
    }

    @Test
    fun `100 meters to feet`() {
        val feet = UnitConverter.metersToDisplayElevation(100.0, UnitSystem.IMPERIAL)
        assertEquals(328.08, feet, 0.1)
    }

    @Test
    fun `displayToMetersElevation converts feet back`() {
        val meters = UnitConverter.displayToMetersElevation(328.084, UnitSystem.IMPERIAL)
        assertEquals(100.0, meters, 0.1)
    }

    @Test
    fun `displayUnitElevation returns m for metric`() {
        assertEquals("m", UnitConverter.displayUnitElevation(UnitSystem.METRIC))
    }

    @Test
    fun `displayUnitElevation returns ft for imperial`() {
        assertEquals("ft", UnitConverter.displayUnitElevation(UnitSystem.IMPERIAL))
    }

    // ============== Formatting Tests ==============

    @Test
    fun `formatDistance includes unit`() {
        val formatted = UnitConverter.formatDistance(10000.0, UnitSystem.METRIC)
        assertTrue(formatted.contains("10"))
        assertTrue(formatted.contains("km"))
    }

    @Test
    fun `formatSpeed includes unit`() {
        val speedMs = 10.0 // ~36 km/h
        val formatted = UnitConverter.formatSpeed(speedMs, UnitSystem.METRIC)
        assertTrue(formatted.contains("36"))
        assertTrue(formatted.contains("km/h"))
    }

    @Test
    fun `formatElevation includes unit`() {
        val formatted = UnitConverter.formatElevation(150.0, UnitSystem.METRIC)
        assertTrue(formatted.contains("150"))
        assertTrue(formatted.contains("m"))
    }
}
