package ru.lopon.core.metrics

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.lopon.core.RealTimeProvider
import ru.lopon.core.TimeProvider

class MetricsAggregator(
    private val movingSpeedThresholdMs: Double = 0.5,
    private val timeProvider: TimeProvider = RealTimeProvider(),
    private val tickIntervalMs: Long = 1000L,
    private val speedDecayTimeoutMs: Long = 2000L
) {
    private val speedCalculator = SpeedCalculator()
    private val cadenceCalculator = CadenceCalculator()
    private val movingTimeTracker = MovingTimeTracker(movingSpeedThresholdMs, timeProvider)

    private var totalDistanceM: Double = 0.0
    private var startTimeMs: Long? = null
    private var lastElevation: Double? = null
    private var elevationGainM: Double = 0.0

    // For speed decay when no sensor data
    private var lastDataReceivedMs: Long = 0L
    private var lastKnownSpeedMs: Double = 0.0

    private val _metrics = MutableStateFlow(TripMetrics.ZERO)
    val metrics: StateFlow<TripMetrics> = _metrics.asStateFlow()

    private var tickerJob: Job? = null
    private var tickerScope: CoroutineScope? = null

    fun start(scope: CoroutineScope? = null) {
        startTimeMs = timeProvider.currentTimeMillis()
        lastDataReceivedMs = timeProvider.currentTimeMillis()

        scope?.let {
            tickerScope = it
            startTicker(it)
        }
    }

    private fun startTicker(scope: CoroutineScope) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(tickIntervalMs)
                tick()
            }
        }
    }

    private fun tick() {
        if (startTimeMs == null) return

        val currentTime = timeProvider.currentTimeMillis()
        val timeSinceLastData = currentTime - lastDataReceivedMs

        val currentSpeedMs = if (timeSinceLastData > speedDecayTimeoutMs) {
            val decayFactor = 1.0 - ((timeSinceLastData - speedDecayTimeoutMs) / 3000.0).coerceIn(0.0, 1.0)
            (lastKnownSpeedMs * decayFactor).coerceAtLeast(0.0)
        } else {
            lastKnownSpeedMs
        }

        movingTimeTracker.update(currentSpeedMs)

        _metrics.value = _metrics.value.copy(
            currentSpeedMs = currentSpeedMs,
            movingTimeMs = movingTimeTracker.currentMovingTimeMs,
            elapsedTimeMs = currentTime - (startTimeMs ?: currentTime)
        )
    }

    fun processIncrement(
        distanceDeltaM: Double,
        speedMs: Double,
        cadenceRpm: Double? = null,
        altitude: Double? = null
    ) {
        if (startTimeMs == null) return

        lastDataReceivedMs = timeProvider.currentTimeMillis()
        lastKnownSpeedMs = speedMs
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
        tickerJob?.cancel()
        movingTimeTracker.pause()
    }

    fun resume() {
        movingTimeTracker.resume()
        tickerScope?.let { startTicker(it) }
    }

    fun reset() {
        tickerJob?.cancel()
        tickerJob = null
        tickerScope = null
        speedCalculator.reset()
        cadenceCalculator.reset()
        movingTimeTracker.reset()
        totalDistanceM = 0.0
        startTimeMs = null
        lastElevation = null
        elevationGainM = 0.0
        lastDataReceivedMs = 0L
        lastKnownSpeedMs = 0.0
        _metrics.value = TripMetrics.ZERO
    }

    val currentMetrics: TripMetrics get() = _metrics.value
    val totalDistance: Double get() = totalDistanceM
}

