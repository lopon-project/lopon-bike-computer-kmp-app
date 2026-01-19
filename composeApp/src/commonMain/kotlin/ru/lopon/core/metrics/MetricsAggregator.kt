package ru.lopon.core.metrics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.lopon.core.TimeProvider
import ru.lopon.core.RealTimeProvider
import ru.lopon.domain.processing.DataFusionProcessor

class MetricsAggregator(
    private val movingSpeedThresholdMs: Double = 0.5,
    private val timeProvider: TimeProvider = RealTimeProvider()
) {
    private val speedCalculator = SpeedCalculator()
    private val cadenceCalculator = CadenceCalculator()
    private val movingTimeTracker = MovingTimeTracker(movingSpeedThresholdMs, timeProvider)

    private var totalDistanceM: Double = 0.0
    private var startTimeMs: Long? = null
    private var lastElevation: Double? = null
    private var elevationGainM: Double = 0.0

    private val _metrics = MutableStateFlow(TripMetrics.ZERO)

    val metrics: StateFlow<TripMetrics> = _metrics.asStateFlow()

    fun start() {
        startTimeMs = timeProvider.currentTimeMillis()
    }


    fun processData(data: DataFusionProcessor.FusionResult, cadenceRpm: Double? = null) {
        if (startTimeMs == null) return

        val speedMs = data.speedKmh / 3.6

        val speed = speedCalculator.addSpeed(speedMs)

        val cadence = cadenceCalculator.addCadence(cadenceRpm)

        movingTimeTracker.update(speedMs)

        emitMetrics(speed, cadence)
    }


    fun processIncrement(
        distanceDeltaM: Double,
        speedMs: Double,
        cadenceRpm: Double? = null,
        altitude: Double? = null
    ) {
        if (startTimeMs == null) return

        totalDistanceM += distanceDeltaM

        val speed = speedCalculator.addSpeed(speedMs)

        val cadence = cadenceCalculator.addCadence(cadenceRpm)

        movingTimeTracker.update(speedMs)

        altitude?.let { alt ->
            lastElevation?.let { lastAlt ->
                if (alt > lastAlt) {
                    elevationGainM += (alt - lastAlt)
                }
            }
            lastElevation = alt
        }

        emitMetrics(speed, cadence)
    }


    fun addDistance(distanceDeltaM: Double) {
        totalDistanceM += distanceDeltaM
    }

    private fun emitMetrics(speed: SmoothedSpeed, cadence: CadenceStats) {
        _metrics.value = TripMetrics(
            currentSpeedMs = speed.current,
            averageSpeedMs = calculateAverageSpeedFromMovingTime(speed),
            maxSpeedMs = speed.max,
            totalDistanceM = totalDistanceM,
            currentCadenceRpm = cadence.current,
            averageCadenceRpm = cadence.average,
            movingTimeMs = movingTimeTracker.currentMovingTimeMs,
            elapsedTimeMs = startTimeMs?.let { timeProvider.currentTimeMillis() - it } ?: 0,
            elevationGainM = if (elevationGainM > 0) elevationGainM else null
        )
    }

    private fun calculateAverageSpeedFromMovingTime(speed: SmoothedSpeed): Double {
        val movingTimeSeconds = movingTimeTracker.currentMovingTimeMs / 1000.0
        return if (movingTimeSeconds > 0) {
            totalDistanceM / movingTimeSeconds
        } else {
            speed.average
        }
    }

    fun pause() {
        movingTimeTracker.pause()
    }

    fun resume() {
        movingTimeTracker.resume()
    }

    fun reset() {
        speedCalculator.reset()
        cadenceCalculator.reset()
        movingTimeTracker.reset()
        totalDistanceM = 0.0
        startTimeMs = null
        lastElevation = null
        elevationGainM = 0.0
        _metrics.value = TripMetrics.ZERO
    }

    val currentMetrics: TripMetrics get() = _metrics.value

    val totalDistance: Double get() = totalDistanceM
}

