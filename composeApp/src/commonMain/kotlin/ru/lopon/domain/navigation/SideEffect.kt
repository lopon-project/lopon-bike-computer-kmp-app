package ru.lopon.domain.navigation

import ru.lopon.domain.model.NavigationMode


sealed class SideEffect {

    data class ConnectBle(val deviceId: String? = null) : SideEffect()

    data object DisconnectBle : SideEffect()

    data object StartGps : SideEffect()

    data object StopGps : SideEffect()

    data object StartMetrics : SideEffect()

    data object PauseMetrics : SideEffect()

    data object ResumeMetrics : SideEffect()

    data object StopMetrics : SideEffect()

    data class CreateTrip(
        val tripId: String,
        val mode: NavigationMode
    ) : SideEffect()

    data class SaveTrip(val tripId: String) : SideEffect()

    data class ShowNotification(
        val title: String,
        val message: String,
        val type: NotificationType = NotificationType.INFO
    ) : SideEffect()

    data class ShowErrorDialog(
        val error: NavigationError,
        val actions: List<ErrorAction>
    ) : SideEffect()
}

enum class NotificationType {
    INFO,
    WARNING,
    ERROR
}

data class ErrorAction(
    val label: String,
    val event: NavigationEvent
)
