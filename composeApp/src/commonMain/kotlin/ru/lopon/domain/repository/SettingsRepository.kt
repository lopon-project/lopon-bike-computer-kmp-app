package ru.lopon.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.Settings
import ru.lopon.domain.model.UnitSystem

interface SettingsRepository {

    fun getSettings(): Flow<Settings>

    suspend fun getCurrentSettings(): Settings

    suspend fun updateSettings(settings: Settings): Result<Unit>

    suspend fun updateWheelCircumference(mm: Double): Result<Unit>

    suspend fun updateUnits(units: UnitSystem): Result<Unit>

    suspend fun updateDefaultMode(mode: NavigationMode): Result<Unit>

    suspend fun updateAutoConnectBle(enabled: Boolean): Result<Unit>

    suspend fun saveLastBleDevice(deviceId: String, deviceName: String?): Result<Unit>

    suspend fun clearLastBleDevice(): Result<Unit>
}
