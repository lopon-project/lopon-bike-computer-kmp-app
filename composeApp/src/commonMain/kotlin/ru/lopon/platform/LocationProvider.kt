package ru.lopon.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LocationProvider {

    val status: StateFlow<LocationStatus>

    fun observeLocations(): Flow<LocationData>

    suspend fun start()

    suspend fun stop()
}

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val altitude: Double?,
    val speed: Float?,
    val timestampUtc: Long
)

enum class LocationStatus {
    DISABLED,

    SEARCHING,

    ACTIVE,

    ERROR
}

