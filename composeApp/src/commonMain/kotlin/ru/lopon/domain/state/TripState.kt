package ru.lopon.domain.state

import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.Trip

sealed class TripState {

    data object Idle : TripState()

    data class Recording(
        val trip: Trip,
        val mode: NavigationMode,
        val distanceMeters: Double = 0.0,
        val elapsedMs: Long = 0
    ) : TripState()

    data class Paused(
        val trip: Trip,
        val mode: NavigationMode,
        val distanceMeters: Double,
        val elapsedMs: Long
    ) : TripState()

    data class Finished(
        val trip: Trip
    ) : TripState()
}

