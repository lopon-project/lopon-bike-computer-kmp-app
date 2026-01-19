package ru.lopon.core.metrics

class SpeedCalculator(
    private val smoothingWindowSize: Int = 5
) {
    private val speedHistory = ArrayDeque<Double>()
    private var maxSpeed: Double = 0.0
    private var speedSum: Double = 0.0
    private var speedCount: Int = 0

    fun addSpeed(speedMs: Double): SmoothedSpeed {
        speedHistory.addLast(speedMs)
        if (speedHistory.size > smoothingWindowSize) {
            speedHistory.removeFirst()
        }

        if (speedMs > maxSpeed) {
            maxSpeed = speedMs
        }

        speedSum += speedMs
        speedCount++

        val smoothedCurrent = if (speedHistory.isNotEmpty()) {
            speedHistory.sum() / speedHistory.size
        } else {
            0.0
        }

        return SmoothedSpeed(
            current = smoothedCurrent,
            average = if (speedCount > 0) speedSum / speedCount else 0.0,
            max = maxSpeed
        )
    }

    fun reset() {
        speedHistory.clear()
        maxSpeed = 0.0
        speedSum = 0.0
        speedCount = 0
    }

    val currentMaxSpeed: Double get() = maxSpeed

    val currentAverageSpeed: Double get() = if (speedCount > 0) speedSum / speedCount else 0.0
}

