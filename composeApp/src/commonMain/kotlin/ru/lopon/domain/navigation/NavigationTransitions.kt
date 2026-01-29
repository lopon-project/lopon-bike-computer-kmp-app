package ru.lopon.domain.navigation

import kotlin.reflect.KClass

object NavigationTransitions {

    fun getValidTransitions(state: NavigationState): Set<KClass<out NavigationEvent>> {
        return when (state) {
            is NavigationState.Idle -> setOf(
                NavigationEvent.StartRequested::class
            )

            is NavigationState.Initializing -> setOf(
                NavigationEvent.BleConnected::class,
                NavigationEvent.BleConnectionFailed::class,
                NavigationEvent.BleNotRequired::class,
                NavigationEvent.GpsAcquired::class,
                NavigationEvent.GpsFailed::class,
                NavigationEvent.GpsNotRequired::class,
                NavigationEvent.RouteLoaded::class,
                NavigationEvent.RouteFailed::class,
                NavigationEvent.InitializationComplete::class,
                NavigationEvent.AbortRequested::class,
                NavigationEvent.ErrorOccurred::class
            )

            is NavigationState.Recording -> setOf(
                NavigationEvent.PauseRequested::class,
                NavigationEvent.StopRequested::class,
                NavigationEvent.ModeChangeRequested::class,
                NavigationEvent.ErrorOccurred::class
            )

            is NavigationState.Paused -> setOf(
                NavigationEvent.ResumeRequested::class,
                NavigationEvent.StopRequested::class,
                NavigationEvent.ErrorOccurred::class
            )

            is NavigationState.Finished -> setOf(
                NavigationEvent.StartRequested::class
            )

            is NavigationState.Error -> setOf(
                NavigationEvent.RetryRequested::class,
                NavigationEvent.ContinueWithoutFeature::class,
                NavigationEvent.AbortRequested::class
            )
        }
    }

    fun isValidTransition(state: NavigationState, event: NavigationEvent): Boolean {
        return event::class in getValidTransitions(state)
    }

    fun describeInvalidTransition(state: NavigationState, event: NavigationEvent): String {
        return "Cannot process ${event::class.simpleName} in state ${state::class.simpleName}"
    }
}

class InvalidTransitionException(message: String) : Exception(message)
