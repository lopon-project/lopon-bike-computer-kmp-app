package ru.lopon.domain.processing

import ru.lopon.domain.model.SensorReading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue


class SensorDataFilterTest {

    private val wheelCircumferenceMm = 2100.0

    @Test
    fun `calculateSpeedKmh returns correct speed for valid readings`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 0
        )
        val current = SensorReading(
            cumulativeRevolutions = 110,
            wheelEventTimeUnits = 1024
        )

        val speed = SensorDataFilter.calculateSpeedKmh(current, previous, wheelCircumferenceMm)

        assertEquals(75.6, speed!!, 0.1)
    }

    @Test
    fun `calculateSpeedKmh returns null for too small time delta`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 0
        )
        val current = SensorReading(
            cumulativeRevolutions = 101,
            wheelEventTimeUnits = 5
        )

        val speed = SensorDataFilter.calculateSpeedKmh(current, previous, wheelCircumferenceMm)

        assertNull(speed)
    }

    @Test
    fun `validate returns invalid for speed too high`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 0
        )
        val current = SensorReading(
            cumulativeRevolutions = 200,
            wheelEventTimeUnits = 1024
        )

        val result = SensorDataFilter.validate(current, previous, wheelCircumferenceMm)

        assertFalse(result.isValid)
        assertEquals(SensorDataFilter.InvalidReason.SPEED_TOO_HIGH, result.reason)
    }

    @Test
    fun `validate returns valid for normal speed`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 0
        )
        val current = SensorReading(
            cumulativeRevolutions = 105,
            wheelEventTimeUnits = 1024
        )

        val result = SensorDataFilter.validate(current, previous, wheelCircumferenceMm)

        assertTrue(result.isValid)
        assertNull(result.reason)
    }

    @Test
    fun `validate handles wheelEventTime overflow`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 65000
        )
        val current = SensorReading(
            cumulativeRevolutions = 105,
            wheelEventTimeUnits = 536
        )

        val result = SensorDataFilter.validate(current, previous, wheelCircumferenceMm)

        assertTrue(result.isValid)
    }

    @Test
    fun `validate returns invalid for acceleration too high`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 0
        )
        val current = SensorReading(
            cumulativeRevolutions = 103,
            wheelEventTimeUnits = 512
        )

        val result = SensorDataFilter.validate(
            current,
            previous,
            wheelCircumferenceMm,
            previousSpeedKmh = 5.0
        )

        assertFalse(result.isValid)
        assertEquals(SensorDataFilter.InvalidReason.ACCELERATION_TOO_HIGH, result.reason)
    }

    @Test
    fun `validate returns invalid for duplicate data`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 1000
        )
        val current = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 1050
        )

        val result = SensorDataFilter.validate(current, previous, wheelCircumferenceMm)

        assertFalse(result.isValid)
        assertEquals(SensorDataFilter.InvalidReason.DUPLICATE_DATA, result.reason)
    }

    @Test
    fun `validate returns invalid for cadence too high`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 0
        )
        val current = SensorReading(
            cumulativeRevolutions = 105,
            wheelEventTimeUnits = 1024,
            cadence = 250.0
        )

        val result = SensorDataFilter.validate(current, previous, wheelCircumferenceMm)

        assertFalse(result.isValid)
        assertEquals(SensorDataFilter.InvalidReason.CADENCE_TOO_HIGH, result.reason)
    }

    @Test
    fun `filterCadence returns null for out of range values`() {
        assertNull(SensorDataFilter.filterCadence(10.0))
        assertNull(SensorDataFilter.filterCadence(250.0))
        assertNull(SensorDataFilter.filterCadence(null))
    }

    @Test
    fun `filterCadence returns value for valid cadence`() {
        assertEquals(90.0, SensorDataFilter.filterCadence(90.0))
        assertEquals(20.0, SensorDataFilter.filterCadence(20.0))
        assertEquals(200.0, SensorDataFilter.filterCadence(200.0))
    }

    @Test
    fun `filterSpeed clamps extreme values`() {
        assertEquals(0.0, SensorDataFilter.filterSpeed(0.5))
        assertEquals(100.0, SensorDataFilter.filterSpeed(150.0))
        assertEquals(50.0, SensorDataFilter.filterSpeed(50.0))
    }
}

