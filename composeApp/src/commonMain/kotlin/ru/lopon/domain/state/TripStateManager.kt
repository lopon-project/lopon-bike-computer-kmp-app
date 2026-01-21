package ru.lopon.domain.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.lopon.core.metrics.TripMetrics
import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.Trip


class TripStateManager {

    private val _state = MutableStateFlow<TripState>(TripState.Idle)

    val state: StateFlow<TripState> = _state.asStateFlow()

    val currentState: TripState
        get() = _state.value


    fun startTrip(trip: Trip, mode: NavigationMode): Boolean {
        return when (_state.value) {
            is TripState.Idle -> {
                _state.value = TripState.Recording(
                    trip = trip,
                    mode = mode,
                    distanceMeters = 0.0,
                    elapsedMs = 0
                )
                true
            }

            else -> false
        }
    }

    fun pauseTrip(): Boolean {
        return when (val current = _state.value) {
            is TripState.Recording -> {
                _state.value = TripState.Paused(
                    trip = current.trip,
                    mode = current.mode,
                    distanceMeters = current.distanceMeters,
                    elapsedMs = current.elapsedMs,
                    metrics = current.metrics
                )
                true
            }

            else -> false
        }
    }

    fun resumeTrip(): Boolean {
        return when (val current = _state.value) {
            is TripState.Paused -> {
                _state.value = TripState.Recording(
                    trip = current.trip,
                    mode = current.mode,
                    distanceMeters = current.distanceMeters,
                    elapsedMs = current.elapsedMs,
                    metrics = current.metrics
                )
                true
            }

            else -> false
        }
    }

    fun stopTrip(finalTrip: Trip, finalMetrics: TripMetrics = TripMetrics.ZERO): Boolean {
        return when (_state.value) {
            is TripState.Recording, is TripState.Paused -> {
                _state.value = TripState.Finished(trip = finalTrip, finalMetrics = finalMetrics)
                true
            }

            else -> false
        }
    }

    fun reset(): Boolean {
        return when (_state.value) {
            is TripState.Finished -> {
                _state.value = TripState.Idle
                true
            }

            else -> false
        }
    }

    fun switchMode(newMode: NavigationMode): Boolean {
        return when (val current = _state.value) {
            is TripState.Recording -> {
                _state.value = current.copy(mode = newMode)
                true
            }

            is TripState.Paused -> {
                _state.value = current.copy(mode = newMode)
                true
            }

            else -> false
        }
    }

    fun updateProgress(distanceMeters: Double, elapsedMs: Long, metrics: TripMetrics? = null): Boolean {
        return when (val current = _state.value) {
            is TripState.Recording -> {
                _state.value = current.copy(
                    distanceMeters = distanceMeters,
                    elapsedMs = elapsedMs,
                    metrics = metrics ?: current.metrics
                )
                true
            }

            else -> false
        }
    }

    fun updateMetrics(metrics: TripMetrics): Boolean {
        return when (val current = _state.value) {
            is TripState.Recording -> {
                _state.value = current.copy(
                    distanceMeters = metrics.totalDistanceM,
                    elapsedMs = metrics.elapsedTimeMs,
                    metrics = metrics
                )
                true
            }

            else -> false
        }
    }
}

