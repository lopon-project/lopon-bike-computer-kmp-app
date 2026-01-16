package ru.lopon.domain.usecase

import kotlinx.coroutines.CoroutineScope
import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.repository.LocationRepository
import ru.lopon.domain.repository.SensorRepository
import ru.lopon.domain.state.TripState
import ru.lopon.domain.state.TripStateManager


class SwitchModeUseCase(
    private val stateManager: TripStateManager,
    private val sensorRepository: SensorRepository,
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(newMode: NavigationMode, scope: CoroutineScope): Result<Unit> {
        val currentMode = when (val currentState = stateManager.currentState) {
            is TripState.Recording -> currentState.mode
            is TripState.Paused -> currentState.mode
            else -> return Result.failure(
                IllegalStateException("Cannot switch mode: no active trip")
            )
        }

        if (currentMode == newMode) {
            return Result.success(Unit)
        }

        val needsSensor = newMode is NavigationMode.Sensor || newMode is NavigationMode.Hybrid
        val needsLocation = newMode is NavigationMode.Hybrid || newMode is NavigationMode.Gps

        val hadSensor = currentMode is NavigationMode.Sensor || currentMode is NavigationMode.Hybrid
        val hadLocation = currentMode is NavigationMode.Hybrid || currentMode is NavigationMode.Gps

        try {
            if (needsSensor && !hadSensor) {
                sensorRepository.startScanning().getOrThrow()
            }
            if (needsLocation && !hadLocation) {
                locationRepository.startTracking().getOrThrow()
            }
        } catch (e: Exception) {
            return Result.failure(
                Exception("Failed to start required data sources for mode $newMode: ${e.message}", e)
            )
        }

        try {
            if (!needsSensor && hadSensor) {
                sensorRepository.stopAndDisconnect()
            }
            if (!needsLocation && hadLocation) {
                locationRepository.stopTracking()
            }
        } catch (_: Exception) {
        }

        val switched = stateManager.switchMode(newMode)
        if (!switched) {
            return Result.failure(IllegalStateException("Failed to switch mode in state manager"))
        }

        return Result.success(Unit)
    }
}

