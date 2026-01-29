package ru.lopon.domain.navigation

sealed class NavigationError(open val message: String) {

    data class BleConnectionError(override val message: String) : NavigationError(message)

    data class BleDisconnected(override val message: String) : NavigationError(message)

    data class GpsUnavailable(override val message: String) : NavigationError(message)

    data class GpsLost(override val message: String) : NavigationError(message)

    data class RouteLoadError(override val message: String) : NavigationError(message)

    data class StorageError(override val message: String) : NavigationError(message)

    data class PermissionDenied(
        val permission: String,
        override val message: String
    ) : NavigationError(message)

    data class UnknownError(override val message: String) : NavigationError(message)
}
