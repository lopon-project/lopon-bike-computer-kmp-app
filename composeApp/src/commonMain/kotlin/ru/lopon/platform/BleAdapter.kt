package ru.lopon.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.lopon.domain.model.SensorReading

interface BleAdapter {
    val connectionState: StateFlow<BleConnectionState>
    fun observeWheelData(): Flow<SensorReading>
    suspend fun scan(timeoutMs: Long = 15000): List<BleDevice>
    suspend fun connect(deviceId: String): Result<Unit>
    suspend fun disconnect()
}

sealed class BleConnectionState {
    data object Disconnected : BleConnectionState()
    data object Connecting : BleConnectionState()
    data object Connected : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

data class BleDevice(
    val id: String,
    val name: String?,
    val rssi: Int
)
