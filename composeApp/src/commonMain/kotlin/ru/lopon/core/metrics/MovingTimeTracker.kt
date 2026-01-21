package ru.lopon.core.metrics

import ru.lopon.core.TimeProvider
import ru.lopon.core.RealTimeProvider

class MovingTimeTracker(
    private val movingSpeedThresholdMs: Double = 0.5,
    private val timeProvider: TimeProvider = RealTimeProvider() // для тестирования
) {
    private var _movingTimeMs: Long = 0
    private var lastUpdateTime: Long? = null
    private var _isMoving: Boolean = false
    private var isPaused: Boolean = false

    val currentMovingTimeMs: Long get() = _movingTimeMs

    val isCurrentlyMoving: Boolean get() = _isMoving

    fun update(speedMs: Double) {
        if (isPaused) return

        val now = timeProvider.currentTimeMillis()
        val wasMoving = _isMoving
        _isMoving = speedMs >= movingSpeedThresholdMs

        lastUpdateTime?.let { last ->
            if (wasMoving && _isMoving) {
                _movingTimeMs += (now - last)
            }
        }

        lastUpdateTime = now
    }

    fun pause() {
        isPaused = true
        lastUpdateTime = null
    }

    fun resume() {
        isPaused = false
        lastUpdateTime = timeProvider.currentTimeMillis()
    }

    fun reset() {
        _movingTimeMs = 0
        lastUpdateTime = null
        _isMoving = false
        isPaused = false
    }
}

