package ru.lopon.core.metrics

import ru.lopon.core.FakeTimeProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MovingTimeTrackerTest {

    @Test
    fun `only counts time when moving`() {
        val fakeTime = FakeTimeProvider(0)
        val tracker = MovingTimeTracker(movingSpeedThresholdMs = 1.0, timeProvider = fakeTime)

        tracker.update(0.5) // not moving
        fakeTime.advance(1000)
        tracker.update(0.5) // still not moving
        assertEquals(0, tracker.currentMovingTimeMs)

        tracker.update(2.0) // start moving
        fakeTime.advance(1000)
        tracker.update(2.0) // still moving
        assertEquals(1000, tracker.currentMovingTimeMs)
    }

    @Test
    fun `pause stops time accumulation`() {
        val fakeTime = FakeTimeProvider(0)
        val tracker = MovingTimeTracker(movingSpeedThresholdMs = 0.5, timeProvider = fakeTime)

        tracker.update(5.0) // start moving
        fakeTime.advance(1000)
        tracker.update(5.0) // accumulate 1000ms
        assertEquals(1000, tracker.currentMovingTimeMs)

        tracker.pause()
        fakeTime.advance(5000) // time passes but not counted
        tracker.update(5.0)
        assertEquals(1000, tracker.currentMovingTimeMs) // still 1000
    }

    @Test
    fun `resume continues accumulation`() {
        val fakeTime = FakeTimeProvider(0)
        val tracker = MovingTimeTracker(movingSpeedThresholdMs = 0.5, timeProvider = fakeTime)

        tracker.update(5.0)
        fakeTime.advance(1000)
        tracker.update(5.0)
        assertEquals(1000, tracker.currentMovingTimeMs)

        tracker.pause()
        fakeTime.advance(5000)

        tracker.resume()
        fakeTime.advance(2000)
        tracker.update(5.0)
        assertEquals(3000, tracker.currentMovingTimeMs) // 1000 + 2000
    }

    @Test
    fun `isCurrentlyMoving reflects speed`() {
        val fakeTime = FakeTimeProvider(0)
        val tracker = MovingTimeTracker(movingSpeedThresholdMs = 1.0, timeProvider = fakeTime)

        tracker.update(0.5)
        assertFalse(tracker.isCurrentlyMoving)

        tracker.update(1.5)
        assertTrue(tracker.isCurrentlyMoving)

        tracker.update(0.5)
        assertFalse(tracker.isCurrentlyMoving)
    }

    @Test
    fun `reset clears all state`() {
        val fakeTime = FakeTimeProvider(0)
        val tracker = MovingTimeTracker(movingSpeedThresholdMs = 0.5, timeProvider = fakeTime)

        tracker.update(5.0)
        fakeTime.advance(5000)
        tracker.update(5.0)
        assertTrue(tracker.currentMovingTimeMs > 0)

        tracker.reset()
        assertEquals(0, tracker.currentMovingTimeMs)
        assertFalse(tracker.isCurrentlyMoving)
    }

    @Test
    fun `threshold exactly at boundary counts as moving`() {
        val fakeTime = FakeTimeProvider(0)
        val threshold = 1.0
        val tracker = MovingTimeTracker(movingSpeedThresholdMs = threshold, timeProvider = fakeTime)

        tracker.update(threshold)
        assertTrue(tracker.isCurrentlyMoving)
    }

    @Test
    fun `threshold below boundary does not count as moving`() {
        val fakeTime = FakeTimeProvider(0)
        val threshold = 1.0
        val tracker = MovingTimeTracker(movingSpeedThresholdMs = threshold, timeProvider = fakeTime)

        tracker.update(threshold - 0.01)
        assertFalse(tracker.isCurrentlyMoving)
    }

    @Test
    fun `transition from stopped to moving does not count transition time`() {
        val fakeTime = FakeTimeProvider(0)
        val tracker = MovingTimeTracker(movingSpeedThresholdMs = 1.0, timeProvider = fakeTime)

        tracker.update(0.0) // stopped
        fakeTime.advance(1000)
        tracker.update(5.0) // start moving - time from stopped state shouldn't count
        fakeTime.advance(1000)
        tracker.update(5.0) // this interval should count

        assertEquals(1000, tracker.currentMovingTimeMs)
    }

    @Test
    fun `transition from moving to stopped counts time correctly`() {
        val fakeTime = FakeTimeProvider(0)
        val tracker = MovingTimeTracker(movingSpeedThresholdMs = 1.0, timeProvider = fakeTime)

        tracker.update(5.0) // start moving
        fakeTime.advance(2000)
        tracker.update(5.0) // still moving
        fakeTime.advance(1000)
        tracker.update(0.0) // stopped - previous interval should not count

        assertEquals(2000, tracker.currentMovingTimeMs)
    }

    @Test
    fun `multiple start stop cycles accumulate correctly`() {
        val fakeTime = FakeTimeProvider(0)
        val tracker = MovingTimeTracker(movingSpeedThresholdMs = 1.0, timeProvider = fakeTime)

        // First moving period
        tracker.update(5.0)
        fakeTime.advance(1000)
        tracker.update(5.0)

        // Stopped
        tracker.update(0.0)
        fakeTime.advance(2000)
        tracker.update(0.0)

        // Second moving period
        tracker.update(5.0)
        fakeTime.advance(1500)
        tracker.update(5.0)

        assertEquals(2500, tracker.currentMovingTimeMs) // 1000 + 1500
    }

    @Test
    fun `first update does not count time`() {
        val fakeTime = FakeTimeProvider(1000) // start at time 1000
        val tracker = MovingTimeTracker(movingSpeedThresholdMs = 0.5, timeProvider = fakeTime)

        tracker.update(5.0) // first update, no time counted yet
        assertEquals(0, tracker.currentMovingTimeMs)
    }
}

