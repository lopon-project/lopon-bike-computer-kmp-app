package ru.lopon.platform

interface PermissionsManager {

    fun checkPermission(permission: AppPermission): PermissionState

    suspend fun requestPermission(permission: AppPermission): PermissionState
}


enum class AppPermission {
    LOCATION,

    BLUETOOTH,

    FILE_ACCESS
}


enum class PermissionState {
    GRANTED,

    DENIED,

    DENIED_FOREVER
}

