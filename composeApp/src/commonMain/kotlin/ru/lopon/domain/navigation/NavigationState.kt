package ru.lopon.domain.navigation

import kotlinx.serialization.Serializable
import ru.lopon.core.metrics.TripMetrics
import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.Route

sealed class NavigationState {

    data object Idle : NavigationState()

    data class Initializing(
        val mode: NavigationMode,
        val route: Route?,
        val wheelCircumferenceMm: Double,
        val progress: InitProgress,
        val bleStatus: BleInitStatus = BleInitStatus.PENDING,
        val gpsStatus: GpsInitStatus = GpsInitStatus.PENDING
    ) : NavigationState()

    data class Recording(
        val tripId: String,
        val mode: NavigationMode,
        val route: Route?,
        val startTimeUtc: Long
    ) : NavigationState()

    data class Paused(
        val tripId: String,
        val mode: NavigationMode,
        val route: Route?,
        val pausedAtUtc: Long,
        val accumulatedMetrics: TripMetrics
    ) : NavigationState()

    data class Finished(
        val tripId: String,
        val finalMetrics: TripMetrics,
        val savedSuccessfully: Boolean
    ) : NavigationState()

    data class Error(
        val error: NavigationError,
        val canRetry: Boolean,
        val canContinueWithoutFeature: Boolean,
        val affectedFeature: String?
    ) : NavigationState()
}

@Serializable
enum class InitProgress {
    STARTING,
    CONNECTING_BLE,
    ACQUIRING_GPS,
    LOADING_ROUTE,
    READY
}

@Serializable
enum class BleInitStatus {
    PENDING,
    CONNECTING,
    CONNECTED,
    FAILED,
    NOT_REQUIRED
}


@Serializable
enum class GpsInitStatus {
    PENDING,
    SEARCHING,
    ACQUIRED,
    FAILED,
    NOT_REQUIRED
}
