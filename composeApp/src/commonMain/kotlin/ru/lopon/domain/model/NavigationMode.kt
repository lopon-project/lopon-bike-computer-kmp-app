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

