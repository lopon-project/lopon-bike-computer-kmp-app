package ru.lopon.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoponSensorProtocolTest {


    @Test
    fun `parseCscMeasurement returns valid SensorReading for standard packet`() {

        val packet = byteArrayOf(
            0x01,
            0xE8.toByte(), 0x03,
            0x00, 0x00,
            0x00, 0x08
        )

        val reading = LoponSensorProtocol.parseCscMeasurement(packet, timestampUtc = 1000L)

        assertNotNull(reading)
        assertEquals(1000L, reading.cumulativeRevolutions)
        assertEquals(2048, reading.wheelEventTimeUnits)
        assertEquals(1000L, reading.timestampUtc)
        assertNull(reading.cadence)
    }

    @Test
    fun `parseCscMeasurement handles large revolution count`() {
        val packet = byteArrayOf(
            0x01,
            0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte()
        )

        val reading = LoponSensorProtocol.parseCscMeasurement(packet)

        assertNotNull(reading)
        assertEquals(4294967295L, reading.cumulativeRevolutions)
        assertEquals(65535, reading.wheelEventTimeUnits)
    }

    @Test
    fun `parseCscMeasurement returns null for empty packet`() {
        val reading = LoponSensorProtocol.parseCscMeasurement(byteArrayOf())
        assertNull(reading)
    }

    @Test
    fun `parseCscMeasurement returns null when wheel data flag is not set`() {
        val packet = byteArrayOf(
            0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00
        )

        val reading = LoponSensorProtocol.parseCscMeasurement(packet)
        assertNull(reading)
    }

    @Test
    fun `parseCscMeasurement returns null for packet too short`() {
        val packet = byteArrayOf(0x01, 0x00, 0x00)

        val reading = LoponSensorProtocol.parseCscMeasurement(packet)
        assertNull(reading)
    }

    @Test
    fun `parseCscMeasurement handles zero values`() {
        val packet = byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

        val reading = LoponSensorProtocol.parseCscMeasurement(packet)

        assertNotNull(reading)
        assertEquals(0L, reading.cumulativeRevolutions)
        assertEquals(0, reading.wheelEventTimeUnits)
    }


    @Test
    fun `createStartCommand returns correct bytes`() {
        val command = LoponSensorProtocol.createStartCommand()

        assertEquals(2, command.size)
        assertEquals(0x02.toByte(), command[0])
        assertEquals(0x01.toByte(), command[1])
    }

    @Test
    fun `createStopCommand returns correct bytes`() {
        val command = LoponSensorProtocol.createStopCommand()

        assertEquals(2, command.size)
        assertEquals(0x02.toByte(), command[0])
        assertEquals(0x02.toByte(), command[1])
    }

    @Test
    fun `createSetPprCommand returns correct bytes for PPR 1`() {
        val command = LoponSensorProtocol.createSetPprCommand(1)

        assertEquals(3, command.size)
        assertEquals(0x01.toByte(), command[0])
        assertEquals(0x01.toByte(), command[1])
        assertEquals(0x00.toByte(), command[2])
    }

    @Test
    fun `createSetPprCommand returns correct bytes for PPR 256`() {
        val command = LoponSensorProtocol.createSetPprCommand(256)

        assertEquals(3, command.size)
        assertEquals(0x01.toByte(), command[0])
        assertEquals(0x00.toByte(), command[1])
        assertEquals(0x01.toByte(), command[2])
    }

    @Test
    fun `createSetPprCommand returns correct bytes for max PPR`() {
        val command = LoponSensorProtocol.createSetPprCommand(65535)

        assertEquals(3, command.size)
        assertEquals(0x01.toByte(), command[0])
        assertEquals(0xFF.toByte(), command[1])
        assertEquals(0xFF.toByte(), command[2])
    }


    @Test
    fun `parseConfigResponse returns Success for 0x00`() {
        val response = LoponSensorProtocol.parseConfigResponse(byteArrayOf(0x00))
        assertEquals(ConfigResponse.Success, response)
    }

    @Test
    fun `parseConfigResponse returns Error for 0x01`() {
        val response = LoponSensorProtocol.parseConfigResponse(byteArrayOf(0x01))
        assertEquals(ConfigResponse.Error, response)
    }

    @Test
    fun `parseConfigResponse returns Unknown for empty data`() {
        val response = LoponSensorProtocol.parseConfigResponse(byteArrayOf())
        assertEquals(ConfigResponse.Unknown, response)
    }

    @Test
    fun `parseConfigResponse returns Unknown for unexpected value`() {
        val response = LoponSensorProtocol.parseConfigResponse(byteArrayOf(0x42))
        assertEquals(ConfigResponse.Unknown, response)
    }


    @Test
    fun `isLoponDevice returns true for exact device name`() {
        assertTrue(LoponSensorProtocol.isLoponDevice("Lopon HSS v1.0"))
    }

    @Test
    fun `isLoponDevice returns true for name starting with Lopon`() {
        assertTrue(LoponSensorProtocol.isLoponDevice("Lopon Sensor"))
        assertTrue(LoponSensorProtocol.isLoponDevice("LoponTest"))
    }

    @Test
    fun `isLoponDevice returns true case insensitive`() {
        assertTrue(LoponSensorProtocol.isLoponDevice("lopon HSS"))
        assertTrue(LoponSensorProtocol.isLoponDevice("LOPON Sensor"))
    }

    @Test
    fun `isLoponDevice returns false for non-matching names`() {
        assertEquals(false, LoponSensorProtocol.isLoponDevice("Some Other Sensor"))
        assertEquals(false, LoponSensorProtocol.isLoponDevice("NotLopon"))
    }

    @Test
    fun `isLoponDevice returns false for null`() {
        assertEquals(false, LoponSensorProtocol.isLoponDevice(null))
    }


    @Test
    fun `device name constant is correct`() {
        assertEquals("Lopon HSS v1.0", LoponSensorProtocol.DEVICE_NAME)
    }

    @Test
    fun `CSC service UUID is standard`() {
        assertEquals("00001816-0000-1000-8000-00805f9b34fb", LoponSensorProtocol.CSC_SERVICE_UUID)
    }

    @Test
    fun `CSC measurement UUID is standard`() {
        assertEquals("00002a5b-0000-1000-8000-00805f9b34fb", LoponSensorProtocol.CSC_MEASUREMENT_UUID)
    }

    @Test
    fun `custom config UUIDs are correct`() {
        assertEquals("66f6197d-1bec-4e2a-9631-bd14b4cdb627", LoponSensorProtocol.CONFIG_WRITE_UUID)
        assertEquals("474032c1-116f-4614-95e9-28bc3388a1e2", LoponSensorProtocol.CONFIG_RESPONSE_UUID)
    }
}
