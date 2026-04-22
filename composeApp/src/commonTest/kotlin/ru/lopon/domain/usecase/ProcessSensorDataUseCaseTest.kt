package ru.lopon.domain.usecase

import ru.lopon.core.metrics.MetricsAggregator
import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.SensorReading
import ru.lopon.domain.model.Trip
import ru.lopon.domain.state.TripStateManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProcessSensorDataUseCaseTest {

    private fun createUseCase(
        wheelCircumferenceMm: Double = 2100.0
    ): Pair<ProcessSensorDataUseCase, TripStateManager> {
        val stateManager = TripStateManager()
        val metricsAggregator = MetricsAggregator()

        val trip = Trip(
            id = "test-trip",
            startTimeUtc = 1000L,
            mode = NavigationMode.Sensor
        )
        stateManager.startTrip(trip, NavigationMode.Sensor)
        metricsAggregator.start()

        val useCase = ProcessSensorDataUseCase(
            stateManager = stateManager,
            metricsAggregator = metricsAggregator,
            wheelCircumferenceMm = wheelCircumferenceMm
        )

        return useCase to stateManager
    }

    @Test
    fun `first reading returns FirstReading result`() {
        val (useCase, _) = createUseCase()

        val reading = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 1024
        )

        val result = useCase(reading)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is SensorProcessingResult.FirstReading)
    }

    @Test
    fun `second reading calculates speed and distance`() {
        val (useCase, _) = createUseCase(wheelCircumferenceMm = 2100.0)

        val reading1 = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 0
        )
        useCase(reading1)

        val reading2 = SensorReading(
            cumulativeRevolutions = 110,
            wheelEventTimeUnits = 1024
        )

        val result = useCase(reading2)

        assertTrue(result.isSuccess)
        val processed = result.getOrNull() as SensorProcessingResult.Processed

        assertEquals(21.0, processed.distanceDeltaM, 0.01)

        assertEquals(21.0, processed.speedMs, 0.01)
        assertEquals(75.6, processed.speedKmh, 0.1)

        assertEquals(10L, processed.revolutionsDelta)
        assertEquals(1.0, processed.timeDeltaSeconds, 0.001)
    }

    @Test
    fun `filters out anomalous high speed`() {
        val (useCase, _) = createUseCase(wheelCircumferenceMm = 2100.0)

        val reading1 = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 0
        )
        useCase(reading1)

        val reading2 = SensorReading(
            cumulativeRevolutions = 1100,
            wheelEventTimeUnits = 1024
        )

        val result = useCase(reading2)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is SensorProcessingResult.Skipped)
    }

    @Test
    fun `skips reading with zero time delta`() {
        val (useCase, _) = createUseCase()

        val reading1 = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 1000
        )
        useCase(reading1)

        val reading2 = SensorReading(
            cumulativeRevolutions = 110,
            wheelEventTimeUnits = 1000
        )

        val result = useCase(reading2)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is SensorProcessingResult.Skipped)
    }

    @Test
    fun `fails when trip not recording`() {
        val stateManager = TripStateManager()
        val metricsAggregator = MetricsAggregator()

        val useCase = ProcessSensorDataUseCase(
            stateManager = stateManager,
            metricsAggregator = metricsAggregator
        )

        val reading = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 1024
        )

        val result = useCase(reading)

        assertTrue(result.isFailure)
    }

    @Test
    fun `reset clears previous reading`() {
        val (useCase, _) = createUseCase()

        useCase(SensorReading(cumulativeRevolutions = 100, wheelEventTimeUnits = 0))

        val result1 = useCase(SensorReading(cumulativeRevolutions = 110, wheelEventTimeUnits = 1024))
        assertTrue(result1.getOrNull() is SensorProcessingResult.Processed)

        useCase.reset()

        val result2 = useCase(SensorReading(cumulativeRevolutions = 200, wheelEventTimeUnits = 2048))
        assertTrue(result2.getOrNull() is SensorProcessingResult.FirstReading)
    }

    @Test
    fun `handles wheel event time wrap-around`() {
        val (useCase, _) = createUseCase(wheelCircumferenceMm = 2100.0)

        val reading1 = SensorReading(
            cumulativeRevolutions = 100,
            wheelEventTimeUnits = 65000
        )
        useCase(reading1)

        val reading2 = SensorReading(
            cumulativeRevolutions = 110,
            wheelEventTimeUnits = 1024
        )

        val result = useCase(reading2)

        assertTrue(result.isSuccess)
        val processed = result.getOrNull() as SensorProcessingResult.Processed

        assertEquals(1.523, processed.timeDeltaSeconds, 0.01)
    }

    @Test
    fun `includes cadence when available`() {
        val (useCase, _) = createUseCase()

        useCase(SensorReading(cumulativeRevolutions = 100, wheelEventTimeUnits = 0))

        val reading2 = SensorReading(
            cumulativeRevolutions = 110,
            wheelEventTimeUnits = 1024,
            cadence = 90.0
        )

        val result = useCase(reading2)
        val processed = result.getOrNull() as SensorProcessingResult.Processed

        assertEquals(90.0, processed.cadenceRpm)
    }
}
