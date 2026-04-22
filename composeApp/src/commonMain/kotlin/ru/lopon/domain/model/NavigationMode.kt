package ru.lopon.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed class NavigationMode {
    @Serializable
    data object Sensor : NavigationMode()

    @Serializable
    data object Hybrid : NavigationMode()

    @Serializable
    data object Gps : NavigationMode()
}

fun NavigationMode.displayName(): String = when (this) {
    is NavigationMode.Sensor -> "Sensor"
    is NavigationMode.Hybrid -> "Hybrid"
    is NavigationMode.Gps -> "GPS"
}

