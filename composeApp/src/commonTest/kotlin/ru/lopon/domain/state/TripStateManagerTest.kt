package ru.lopon.domain.state

import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.Trip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TripStateManagerTest {

    private val testTrip = Trip(
        id = "test-trip-1",
        startTimeUtc = 1000L,
        mode = NavigationMode.Sensor
    )

    @Test
    fun `initial state is idle`() {
        val manager = TripStateManager()
        assertIs<TripState.Idle>(manager.currentState)
    }

    @Test
    fun `start trip transitions from idle to recording`() {
        val manager = TripStateManager()

        val result = manager.startTrip(testTrip, NavigationMode.Sensor)

        assertTrue(result)
        val state = manager.currentState
        assertIs<TripState.Recording>(state)
        assertEquals(testTrip, state.trip)
        assertEquals(NavigationMode.Sensor, state.mode)
        assertEquals(0.0, state.distanceMeters)
        assertEquals(0L, state.elapsedMs)
    }

    @Test
    fun `start trip fails when not idle`() {
        val manager = TripStateManager()
        manager.startTrip(testTrip, NavigationMode.Sensor)

        val result = manager.startTrip(testTrip, NavigationMode.Gps)

        assertFalse(result)
        assertIs<TripState.Recording>(manager.currentState)
    }

    @Test
    fun `pause trip transitions from recording to paused`() {
        val manager = TripStateManager()
        manager.startTrip(testTrip, NavigationMode.Sensor)
        manager.updateProgress(100.0, 5000L)

        val result = manager.pauseTrip()

        assertTrue(result)
        val state = manager.currentState
        assertIs<TripState.Paused>(state)
        assertEquals(testTrip, state.trip)
        assertEquals(100.0, state.distanceMeters)
        assertEquals(5000L, state.elapsedMs)
    }

    @Test
    fun `pause trip fails when not recording`() {
        val manager = TripStateManager()

        val result = manager.pauseTrip()

        assertFalse(result)
        assertIs<TripState.Idle>(manager.currentState)
    }

    @Test
    fun `resume trip transitions from paused to recording`() {
        val manager = TripStateManager()
        manager.startTrip(testTrip, NavigationMode.Sensor)
        manager.updateProgress(100.0, 5000L)
        manager.pauseTrip()

        val result = manager.resumeTrip()

        assertTrue(result)
        val state = manager.currentState
        assertIs<TripState.Recording>(state)
        assertEquals(100.0, state.distanceMeters)
        assertEquals(5000L, state.elapsedMs)
    }

    @Test
    fun `resume trip fails when not paused`() {
        val manager = TripStateManager()
        manager.startTrip(testTrip, NavigationMode.Sensor)

        val result = manager.resumeTrip()

        assertFalse(result)
        assertIs<TripState.Recording>(manager.currentState)
    }

    @Test
    fun `stop trip transitions from recording to finished`() {
        val manager = TripStateManager()
        manager.startTrip(testTrip, NavigationMode.Sensor)
        val finishedTrip = testTrip.copy(endTimeUtc = 2000L, distanceMeters = 150.0)

        val result = manager.stopTrip(finishedTrip)

        assertTrue(result)
        val state = manager.currentState
        assertIs<TripState.Finished>(state)
        assertEquals(finishedTrip, state.trip)
    }

    @Test
    fun `stop trip transitions from paused to finished`() {
        val manager = TripStateManager()
        manager.startTrip(testTrip, NavigationMode.Sensor)
        manager.pauseTrip()
        val finishedTrip = testTrip.copy(endTimeUtc = 2000L)

        val result = manager.stopTrip(finishedTrip)

        assertTrue(result)
        assertIs<TripState.Finished>(manager.currentState)
    }

    @Test
    fun `stop trip fails when idle`() {
        val manager = TripStateManager()
        val finishedTrip = testTrip.copy(endTimeUtc = 2000L)

        val result = manager.stopTrip(finishedTrip)

        assertFalse(result)
        assertIs<TripState.Idle>(manager.currentState)
    }

    @Test
    fun `reset transitions from finished to idle`() {
        val manager = TripStateManager()
        manager.startTrip(testTrip, NavigationMode.Sensor)
        manager.stopTrip(testTrip.copy(endTimeUtc = 2000L))

        val result = manager.reset()

        assertTrue(result)
        assertIs<TripState.Idle>(manager.currentState)
    }

    @Test
    fun `reset fails when not finished`() {
        val manager = TripStateManager()
        manager.startTrip(testTrip, NavigationMode.Sensor)

        val result = manager.reset()

        assertFalse(result)
        assertIs<TripState.Recording>(manager.currentState)
    }

    @Test
    fun `switch mode updates recording state`() {
        val manager = TripStateManager()
        manager.startTrip(testTrip, NavigationMode.Sensor)

        val result = manager.switchMode(NavigationMode.Hybrid)

        assertTrue(result)
        val state = manager.currentState
        assertIs<TripState.Recording>(state)
        assertEquals(NavigationMode.Hybrid, state.mode)
    }

    @Test
    fun `switch mode updates paused state`() {
        val manager = TripStateManager()
        manager.startTrip(testTrip, NavigationMode.Sensor)
        manager.pauseTrip()

        val result = manager.switchMode(NavigationMode.Gps)

        assertTrue(result)
        val state = manager.currentState
        assertIs<TripState.Paused>(state)
        assertEquals(NavigationMode.Gps, state.mode)
    }

    @Test
    fun `switch mode fails when idle`() {
        val manager = TripStateManager()

        val result = manager.switchMode(NavigationMode.Hybrid)

        assertFalse(result)
    }

    @Test
    fun `update progress updates recording state`() {
        val manager = TripStateManager()
        manager.startTrip(testTrip, NavigationMode.Sensor)

        val result = manager.updateProgress(500.0, 10000L)

        assertTrue(result)
        val state = manager.currentState
        assertIs<TripState.Recording>(state)
        assertEquals(500.0, state.distanceMeters)
        assertEquals(10000L, state.elapsedMs)
    }

    @Test
    fun `update progress fails when not recording`() {
        val manager = TripStateManager()

        val result = manager.updateProgress(100.0, 5000L)

        assertFalse(result)
    }
}

