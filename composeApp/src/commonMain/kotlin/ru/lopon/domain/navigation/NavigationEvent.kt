package ru.lopon.domain.navigation

import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.Route

sealed class NavigationEvent {


    data class StartRequested(
        val mode: NavigationMode,
        val route: Route?,
        val wheelCircumferenceMm: Double
    ) : NavigationEvent()

    data object BleConnected : NavigationEvent()

    data object BleConnectionFailed : NavigationEvent()

    data object BleNotRequired : NavigationEvent()

    data object GpsAcquired : NavigationEvent()

    data object GpsFailed : NavigationEvent()

    data object GpsNotRequired : NavigationEvent()

    data object RouteLoaded : NavigationEvent()

    data object RouteFailed : NavigationEvent()

    data object InitializationComplete : NavigationEvent()

    data object PauseRequested : NavigationEvent()

    data object ResumeRequested : NavigationEvent()

    data object StopRequested : NavigationEvent()

    data class ModeChangeRequested(val newMode: NavigationMode) : NavigationEvent()

    data class ErrorOccurred(val error: NavigationError) : NavigationEvent()

    data object RetryRequested : NavigationEvent()

    data object ContinueWithoutFeature : NavigationEvent()

    data object AbortRequested : NavigationEvent()
}
