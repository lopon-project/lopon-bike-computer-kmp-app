package ru.lopon.domain.model

import kotlinx.serialization.Serializable
import ru.lopon.core.settings.UnitConverter

@Serializable
data class Settings(
    val wheelCircumferenceMm: Double = 2100.0,
    val defaultMode: NavigationMode = NavigationMode.Hybrid,
    val units: UnitSystem = UnitSystem.METRIC,
    val autoConnectBle: Boolean = true,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val movingSpeedThresholdKmh: Double = 2.0,
    val keepScreenOn: Boolean = true,
    val mapStyle: MapStyle = MapStyle.AUTO,
    val lastBleDeviceId: String? = null,
    val lastBleDeviceName: String? = null
) {
    companion object {
        val DEFAULT = Settings()
    }

    val movingSpeedThresholdMs: Double
        get() = UnitConverter.kmhToMs(movingSpeedThresholdKmh)
}

@Serializable
enum class UnitSystem {
    METRIC,
    IMPERIAL
}

@Serializable
enum class AppLanguage {
    SYSTEM,
    RU,
    EN
}

@Serializable
enum class MapStyle {
    AUTO,
    LIGHT,
    DARK
}
