package ru.lopon.domain.navigation

sealed class NavigationError(open val message: String) {

    data class BleConnectionError(override val message: String) : NavigationError(message)

    data class BleDisconnected(override val message: String) : NavigationError(message)

    data class GpsUnavailable(override val message: String) : NavigationError(message)

    data class GpsLost(override val message: String) : NavigationError(message)

    data class UnknownError(override val message: String) : NavigationError(message)
}
