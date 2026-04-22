package ru.lopon.core.metrics

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ru.lopon.core.FakeTimeProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetricsAggregatorTest {

    @Test
    fun `initial metrics are zero`() = runTest {
        val aggregator = MetricsAggregator()

        val metrics = aggregator.metrics.first()
        assertEquals(TripMetrics.ZERO, metrics)
    }

    @Test
    fun `start initializes elapsed time tracking`() = runTest {
        val fakeTime = FakeTimeProvider(1000)
        val aggregator = MetricsAggregator(timeProvider = fakeTime)

        aggregator.start()
        fakeTime.advance(5000)

        aggregator.processIncrement(
            distanceDeltaM = 100.0,
            speedMs = 10.0
        )

        val metrics = aggregator.currentMetrics
        assertEquals(5000, metrics.elapsedTimeMs)
    }

    @Test
    fun `processIncrement updates distance`() = runTest {
        val fakeTime = FakeTimeProvider(0)
        val aggregator = MetricsAggregator(timeProvider = fakeTime)

        aggregator.start()
        aggregator.processIncrement(distanceDeltaM = 100.0, speedMs = 5.0)
        aggregator.processIncrement(distanceDeltaM = 150.0, speedMs = 5.0)

        val metrics = aggregator.currentMetrics
        assertEquals(250.0, metrics.totalDistanceM, 0.01)
    }

    @Test
    fun `processIncrement updates speed`() = runTest {
        val fakeTime = FakeTimeProvider(0)
        val aggregator = MetricsAggregator(timeProvider = fakeTime)

        aggregator.start()
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 10.0)

        val metrics = aggregator.currentMetrics
        assertEquals(10.0, metrics.currentSpeedMs, 0.01)
    }

    @Test
    fun `max speed is tracked`() = runTest {
        val fakeTime = FakeTimeProvider(0)
        val aggregator = MetricsAggregator(timeProvider = fakeTime)

        aggregator.start()
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0)
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 15.0)
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 10.0)

        val metrics = aggregator.currentMetrics
        assertEquals(15.0, metrics.maxSpeedMs, 0.01)
    }

    @Test
    fun `cadence is tracked when provided`() = runTest {
        val fakeTime = FakeTimeProvider(0)
        val aggregator = MetricsAggregator(timeProvider = fakeTime)

        aggregator.start()
        aggregator.processIncrement(
            distanceDeltaM = 0.0,
            speedMs = 5.0,
            cadenceRpm = 80.0
        )

        val metrics = aggregator.currentMetrics
        assertEquals(80.0, metrics.currentCadenceRpm!!, 0.01)
        assertEquals(80.0, metrics.averageCadenceRpm!!, 0.01)
    }

    @Test
    fun `cadence is null when not provided`() = runTest {
        val fakeTime = FakeTimeProvider(0)
        val aggregator = MetricsAggregator(timeProvider = fakeTime)

        aggregator.start()
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0)

        val metrics = aggregator.currentMetrics
        assertNull(metrics.currentCadenceRpm)
        assertNull(metrics.averageCadenceRpm)
    }

    @Test
    fun `moving time only counts when speed above threshold`() = runTest {
        val fakeTime = FakeTimeProvider(0)
        val threshold = 0.5
        val aggregator = MetricsAggregator(
            movingSpeedThresholdMs = threshold,
            timeProvider = fakeTime
        )

        aggregator.start()

        // Not moving
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 0.3)
        fakeTime.advance(1000)
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 0.3)

        var metrics = aggregator.currentMetrics
        assertEquals(0, metrics.movingTimeMs)

        // Now moving
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0)
        fakeTime.advance(2000)
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0)

        metrics = aggregator.currentMetrics
        assertEquals(2000, metrics.movingTimeMs)
    }

    @Test
    fun `pause stops moving time accumulation`() = runTest {
        val fakeTime = FakeTimeProvider(0)
        val aggregator = MetricsAggregator(timeProvider = fakeTime)

        aggregator.start()
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0)
        fakeTime.advance(1000)
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0)

        var metrics = aggregator.currentMetrics
        assertEquals(1000, metrics.movingTimeMs)

        aggregator.pause()
        fakeTime.advance(5000)
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0)

        metrics = aggregator.currentMetrics
        assertEquals(1000, metrics.movingTimeMs) // unchanged
    }

    @Test
    fun `resume continues moving time accumulation`() = runTest {
        val fakeTime = FakeTimeProvider(0)
        val aggregator = MetricsAggregator(timeProvider = fakeTime)

        aggregator.start()
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0)
        fakeTime.advance(1000)
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0)

        aggregator.pause()
        fakeTime.advance(5000)

        aggregator.resume()
        fakeTime.advance(2000)
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0)

        val metrics = aggregator.currentMetrics
        assertEquals(3000, metrics.movingTimeMs) // 1000 + 2000
    }

    @Test
    fun `reset clears all metrics`() = runTest {
        val fakeTime = FakeTimeProvider(0)
        val aggregator = MetricsAggregator(timeProvider = fakeTime)

        aggregator.start()
        aggregator.processIncrement(
            distanceDeltaM = 100.0,
            speedMs = 10.0,
            cadenceRpm = 80.0,
            altitude = 100.0
        )
        fakeTime.advance(5000)
        aggregator.processIncrement(
            distanceDeltaM = 100.0,
            speedMs = 10.0,
            altitude = 150.0
        )

        aggregator.reset()

        val metrics = aggregator.currentMetrics
        assertEquals(TripMetrics.ZERO, metrics)
    }

    @Test
    fun `elevation gain calculated from altitude changes`() = runTest {
        val fakeTime = FakeTimeProvider(0)
        val aggregator = MetricsAggregator(timeProvider = fakeTime)

        aggregator.start()
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0, altitude = 100.0)
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0, altitude = 150.0)
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0, altitude = 120.0) // descent, not counted
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0, altitude = 180.0)

        val metrics = aggregator.currentMetrics
        assertEquals(110.0, metrics.elevationGainM!!, 0.01) // 50 + 60
    }

    @Test
    fun `descent does not add to elevation gain`() = runTest {
        val fakeTime = FakeTimeProvider(0)
        val aggregator = MetricsAggregator(timeProvider = fakeTime)

        aggregator.start()
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0, altitude = 200.0)
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 5.0, altitude = 100.0)

        val metrics = aggregator.currentMetrics
        assertNull(metrics.elevationGainM) // No gain, should be null
    }

    @Test
    fun `average speed calculated from moving time`() = runTest {
        val fakeTime = FakeTimeProvider(0)
        val aggregator = MetricsAggregator(
            movingSpeedThresholdMs = 0.5,
            timeProvider = fakeTime
        )

        aggregator.start()

        // Moving at 10 m/s for 10 seconds = 100 meters
        aggregator.processIncrement(distanceDeltaM = 0.0, speedMs = 10.0)
        repeat(10) {
            fakeTime.advance(1000)
            aggregator.processIncrement(distanceDeltaM = 10.0, speedMs = 10.0)
        }

        val metrics = aggregator.currentMetrics
        assertEquals(100.0, metrics.totalDistanceM, 0.01)
        assertEquals(10000, metrics.movingTimeMs)
        // Average = totalDistance / movingTime = 100 / 10 = 10 m/s
        assertEquals(10.0, metrics.averageSpeedMs, 0.5)
    }

    @Test
    fun `processIncrement does nothing before start`() = runTest {
        val fakeTime = FakeTimeProvider(0)
        val aggregator = MetricsAggregator(timeProvider = fakeTime)

        // Don't call start()
        aggregator.processIncrement(distanceDeltaM = 100.0, speedMs = 10.0)

        val metrics = aggregator.currentMetrics
        assertEquals(TripMetrics.ZERO, metrics)
    }

    @Test
    fun `metrics flow emits updates`() = runTest {
        val fakeTime = FakeTimeProvider(0)
        val aggregator = MetricsAggregator(timeProvider = fakeTime)

        aggregator.start()
        aggregator.processIncrement(distanceDeltaM = 100.0, speedMs = 5.0)

        val metrics = aggregator.metrics.first()
        assertEquals(100.0, metrics.totalDistanceM, 0.01)
    }

    @Test
    fun `TripMetrics conversion properties work correctly`() {
        val metrics = TripMetrics(
            currentSpeedMs = 10.0,
            averageSpeedMs = 8.0,
            maxSpeedMs = 15.0,
            totalDistanceM = 5000.0
        )

        assertEquals(36.0, metrics.currentSpeedKmh, 0.01) // 10 * 3.6
        assertEquals(28.8, metrics.averageSpeedKmh, 0.01) // 8 * 3.6
        assertEquals(54.0, metrics.maxSpeedKmh, 0.01) // 15 * 3.6
        assertEquals(5.0, metrics.totalDistanceKm, 0.01) // 5000 / 1000
    }
}

