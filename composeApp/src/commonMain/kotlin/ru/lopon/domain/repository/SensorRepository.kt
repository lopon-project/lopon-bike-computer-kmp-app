package ru.lopon.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.lopon.domain.model.SensorReading


interface SensorRepository {
    fun observeConnectionState(): Flow<Boolean>

    fun observeReadings(): Flow<SensorReading>

    suspend fun startScanning(): Result<Unit>

    suspend fun stopAndDisconnect()

    suspend fun isConnected(): Boolean
}

