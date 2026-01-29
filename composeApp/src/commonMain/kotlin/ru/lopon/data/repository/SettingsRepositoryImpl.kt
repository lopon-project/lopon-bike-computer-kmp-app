package ru.lopon.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import ru.lopon.core.settings.ValidationResult
import ru.lopon.core.settings.WheelCircumferenceValidator
import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.Settings
import ru.lopon.domain.model.UnitSystem
import ru.lopon.domain.repository.SettingsRepository
import ru.lopon.platform.FileStorage

class SettingsRepositoryImpl(
    private val fileStorage: FileStorage,
    private val settingsPath: String = "settings.json",
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
) : SettingsRepository {

    private val _settings = MutableStateFlow(Settings.DEFAULT)

    override fun getSettings(): Flow<Settings> = _settings.asStateFlow()

    override suspend fun getCurrentSettings(): Settings = _settings.value

    override suspend fun updateSettings(settings: Settings): Result<Unit> {
        _settings.value = settings
        return saveToFile(settings)
    }

    override suspend fun updateWheelCircumference(mm: Double): Result<Unit> {
        val validation = WheelCircumferenceValidator.validate(mm)
        if (validation !is ValidationResult.Valid) {
            val message = when (validation) {
                is ValidationResult.TooSmall -> "Wheel circumference too small (min: ${validation.minValue} mm)"
                is ValidationResult.TooLarge -> "Wheel circumference too large (max: ${validation.maxValue} mm)"
            }
            return Result.failure(IllegalArgumentException(message))
        }
        return updateSettings(_settings.value.copy(wheelCircumferenceMm = mm))
    }

    override suspend fun updateUnits(units: UnitSystem): Result<Unit> {
        return updateSettings(_settings.value.copy(units = units))
    }

    override suspend fun updateDefaultMode(mode: NavigationMode): Result<Unit> {
        return updateSettings(_settings.value.copy(defaultMode = mode))
    }

    override suspend fun updateAutoConnectBle(enabled: Boolean): Result<Unit> {
        return updateSettings(_settings.value.copy(autoConnectBle = enabled))
    }

    override suspend fun saveLastBleDevice(deviceId: String, deviceName: String?): Result<Unit> {
        return updateSettings(_settings.value.copy(
            lastBleDeviceId = deviceId,
            lastBleDeviceName = deviceName
        ))
    }

    override suspend fun clearLastBleDevice(): Result<Unit> {
        return updateSettings(_settings.value.copy(
            lastBleDeviceId = null,
            lastBleDeviceName = null
        ))
    }

    private suspend fun saveToFile(settings: Settings): Result<Unit> {
        return try {
            val content = json.encodeToString(Settings.serializer(), settings)
            fileStorage.writeText(settingsPath, content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadFromFile(): Result<Settings> {
        return fileStorage.readText(settingsPath)
            .mapCatching { content ->
                json.decodeFromString(Settings.serializer(), content)
            }
            .onSuccess { settings ->
                _settings.value = settings
            }
            .onFailure {
                _settings.value = Settings.DEFAULT
            }
    }
}
