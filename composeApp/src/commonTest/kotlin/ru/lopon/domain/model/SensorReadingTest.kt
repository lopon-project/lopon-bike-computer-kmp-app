package ru.lopon.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SensorReadingTest {

    @Test
    fun `revolutionsDelta with normal increment returns correct delta`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 1000,
            timestampUtc = 1000000L
        )
        val current = SensorReading(
            cumulativeRevolutions = 150,
            wheelEventTimeUnits = 2000,
            timestampUtc = 1001000L
        )

        val delta = current.revolutionsDelta(previous)
        assertEquals(50, delta, "Delta should be 50 revolutions")
    }

    @Test
    fun `revolutionsDelta with 32-bit overflow returns correct delta`() {
        val previous = SensorReading(
            cumulativeRevolutions = 0xFFFFFFF0L,
            wheelEventTimeUnits = 1000,
            timestampUtc = 1000000L
        )
        val current = SensorReading(
            cumulativeRevolutions = 10L,
            wheelEventTimeUnits = 2000,
            timestampUtc = 1001000L
        )

        val delta = current.revolutionsDelta(previous)
        assertEquals(26, delta, "Delta should handle 32-bit overflow")
    }

    @Test
    fun `revolutionsDelta with zero increment returns zero`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 1000,
            timestampUtc = 1000000L
        )
        val current = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 1100,
            timestampUtc = 1001000L
        )

        val delta = current.revolutionsDelta(previous)
        assertEquals(0, delta, "Delta should be 0 when revolutions are equal")
    }

    @Test
    fun `wheelEventTimeDelta with normal increment returns correct delta`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 1000,
            timestampUtc = 1000000L
        )
        val current = SensorReading(
            cumulativeRevolutions = 150,
            wheelEventTimeUnits = 2500,
            timestampUtc = 1001000L
        )

        val delta = current.wheelEventTimeDelta(previous)
        assertEquals(1500, delta, "Time delta should be 1500 units")
    }

    @Test
    fun `wheelEventTimeDelta with 16-bit wrap-around returns correct delta`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 65530,
            timestampUtc = 1000000L
        )
        val current = SensorReading(
            cumulativeRevolutions = 110,
            wheelEventTimeUnits = 10,
            timestampUtc = 1001000L
        )

        val delta = current.wheelEventTimeDelta(previous)
        assertEquals(16, delta, "Time delta should handle 16-bit wrap-around")
    }

    @Test
    fun `wheelEventTimeDelta at exact wrap-around boundary`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 65535,
            timestampUtc = 1000000L
        )
        val current = SensorReading(
            cumulativeRevolutions = 110,
            wheelEventTimeUnits = 0,
            timestampUtc = 1001000L
        )

        val delta = current.wheelEventTimeDelta(previous)
        assertEquals(1, delta, "Time delta should be 1 at wrap-around boundary")
    }

    @Test
    fun `timeDeltaSeconds converts units correctly`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 1000,
            timestampUtc = 1000000L
        )
        val current = SensorReading(
            cumulativeRevolutions = 150,
            wheelEventTimeUnits = 2024,
            timestampUtc = 1001000L
        )

        val deltaSeconds = current.timeDeltaSeconds(previous)
        assertEquals(1.0, deltaSeconds, 0.001, "1024 units should be exactly 1 second")
    }

    @Test
    fun `timeDeltaSeconds with wrap-around`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 65000,
            timestampUtc = 1000000L
        )
        val current = SensorReading(
            cumulativeRevolutions = 110,
            wheelEventTimeUnits = 1024,
            timestampUtc = 1001000L
        )

        val deltaSeconds = current.timeDeltaSeconds(previous)
        assertEquals(1.5234375, deltaSeconds, 0.0001, "Should handle wrap-around in seconds calculation")
    }

    @Test
    fun `timestampDeltaMs with both timestamps set returns correct delta`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 1000,
            timestampUtc = 1000000L
        )
        val current = SensorReading(
            cumulativeRevolutions = 150,
            wheelEventTimeUnits = 2000,
            timestampUtc = 1002000L
        )

        val deltaMs = current.timestampDeltaMs(previous)
        assertEquals(2000, deltaMs, "Timestamp delta should be 2000 ms")
    }

    @Test
    fun `timestampDeltaMs with null current timestamp returns zero`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 1000,
            timestampUtc = 1000000L
        )
        val current = SensorReading(
            cumulativeRevolutions = 150,
            wheelEventTimeUnits = 2000,
            timestampUtc = null
        )

        val deltaMs = current.timestampDeltaMs(previous)
        assertEquals(0, deltaMs, "Delta should be 0 when current timestamp is null")
    }

    @Test
    fun `timestampDeltaMs with null previous timestamp returns zero`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 1000,
            timestampUtc = null
        )
        val current = SensorReading(
            cumulativeRevolutions = 150,
            wheelEventTimeUnits = 2000,
            timestampUtc = 1002000L
        )

        val deltaMs = current.timestampDeltaMs(previous)
        assertEquals(0, deltaMs, "Delta should be 0 when previous timestamp is null")
    }

    @Test
    fun `timestampDeltaMs with negative delta returns zero`() {
        val previous = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 1000,
            timestampUtc = 1002000L
        )
        val current = SensorReading(
            cumulativeRevolutions = 150,
            wheelEventTimeUnits = 2000,
            timestampUtc = 1000000L
        )

        val deltaMs = current.timestampDeltaMs(previous)
        assertEquals(0, deltaMs, "Delta should be 0 when time goes backwards")
    }
}

