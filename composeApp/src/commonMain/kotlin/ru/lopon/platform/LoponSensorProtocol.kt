package ru.lopon.platform

import ru.lopon.domain.model.SensorReading


// https://github.com/lopon-project/lopon-esp32-hall-speed-sensor-firmware
object LoponSensorProtocol {

    const val DEVICE_NAME = "Lopon HSS v1.0"

    const val DEVICE_NAME_PREFIX = "Lopon"

    const val CSC_SERVICE_UUID = "00001816-0000-1000-8000-00805f9b34fb"

    const val CSC_MEASUREMENT_UUID = "00002a5b-0000-1000-8000-00805f9b34fb"

    const val CONFIG_WRITE_UUID = "66f6197d-1bec-4e2a-9631-bd14b4cdb627"

    const val CONFIG_RESPONSE_UUID = "474032c1-116f-4614-95e9-28bc3388a1e2"

    const val CMD_SET_PPR: Byte = 0x01

    const val CMD_CONTROL: Byte = 0x02

    const val CONTROL_START: Byte = 0x01

    const val CONTROL_STOP: Byte = 0x02

    const val RESPONSE_SUCCESS: Byte = 0x00

    const val RESPONSE_ERROR: Byte = 0x01

    private const val CSC_FLAG_WHEEL_DATA: Int = 0x01

    private const val CSC_FLAG_CRANK_DATA: Int = 0x02

    const val CSC_WHEEL_PACKET_SIZE = 7


    fun createStartCommand(): ByteArray = byteArrayOf(CMD_CONTROL, CONTROL_START)

    fun createStopCommand(): ByteArray = byteArrayOf(CMD_CONTROL, CONTROL_STOP)

    fun createSetPprCommand(ppr: Int): ByteArray {
        require(ppr in 1..65535) { "PPR must be between 1 and 65535" }
        return byteArrayOf(
            CMD_SET_PPR,
            (ppr and 0xFF).toByte(),
            ((ppr shr 8) and 0xFF).toByte()
        )
    }


    fun parseCscMeasurement(data: ByteArray, timestampUtc: Long? = null): SensorReading? {
        if (data.isEmpty()) return null

        val flags = data[0].toInt() and 0xFF

        if ((flags and CSC_FLAG_WHEEL_DATA) == 0) {
            return null
        }

        if (data.size < CSC_WHEEL_PACKET_SIZE) {
            return null
        }

        val cumulativeRevolutions = parseUInt32LE(data, 1)

        val wheelEventTime = parseUInt16LE(data, 5)

        //TODO: если в будущем будет отдельный датчик для каденса шатуна
        val cadence: Double? = null

        return SensorReading(
            cumulativeRevolutions = cumulativeRevolutions,
            wheelEventTimeUnits = wheelEventTime,
            timestampUtc = timestampUtc,
            cadence = cadence
        )
    }

    fun parseConfigResponse(data: ByteArray): ConfigResponse {
        if (data.isEmpty()) return ConfigResponse.Unknown

        return when (data[0]) {
            RESPONSE_SUCCESS -> ConfigResponse.Success
            RESPONSE_ERROR -> ConfigResponse.Error
            else -> ConfigResponse.Unknown
        }
    }

    private fun parseUInt32LE(data: ByteArray, offset: Int): Long {
        return ((data[offset].toLong() and 0xFF)) or
                ((data[offset + 1].toLong() and 0xFF) shl 8) or
                ((data[offset + 2].toLong() and 0xFF) shl 16) or
                ((data[offset + 3].toLong() and 0xFF) shl 24)
    }

    private fun parseUInt16LE(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF)) or
                ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    fun isLoponDevice(deviceName: String?): Boolean {
        return deviceName?.startsWith(DEVICE_NAME_PREFIX, ignoreCase = true) == true
    }
}

sealed class ConfigResponse {
    data object Success : ConfigResponse()
    data object Error : ConfigResponse()
    data object Unknown : ConfigResponse()
}
