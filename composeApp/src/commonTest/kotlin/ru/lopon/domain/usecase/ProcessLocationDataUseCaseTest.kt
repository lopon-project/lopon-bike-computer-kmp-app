package ru.lopon.domain.usecase

import ru.lopon.core.metrics.MetricsAggregator
import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.TrackPoint
import ru.lopon.domain.model.Trip
import ru.lopon.domain.state.TripStateManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProcessLocationDataUseCaseTest {

    private fun createUseCase(): Pair<ProcessLocationDataUseCase, TripStateManager> {
        val stateManager = TripStateManager()
        val metricsAggregator = MetricsAggregator()

        // Запускаем поездку
        val trip = Trip(
            id = "test-trip",
            startTimeUtc = 1000L,
            mode = NavigationMode.Gps
        )
        stateManager.startTrip(trip, NavigationMode.Gps)
        metricsAggregator.start()

        val useCase = ProcessLocationDataUseCase(
            stateManager = stateManager,
            metricsAggregator = metricsAggregator
        )

        return useCase to stateManager
    }

    @Test
    fun `first reading returns FirstReading result`() {
        val (useCase, _) = createUseCase()

        val point = TrackPoint(
            latitude = 55.7558,
            longitude = 37.6173,
            timestampUtc = 1000L
        )

        val result = useCase(point)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is LocationProcessingResult.FirstReading)
    }

    @Test
    fun `second reading calculates distance and speed`() {
        val (useCase, _) = createUseCase()

        // Первая точка - Москва, Красная площадь
        val point1 = TrackPoint(
            latitude = 55.7539,
            longitude = 37.6208,
            timestampUtc = 1000L
        )
        useCase(point1)

        // Вторая точка - ~11 метров на север (примерно 0.0001 градуса)
        // Используем меньшее смещение чтобы не было position jump
        val point2 = TrackPoint(
            latitude = 55.7540,
            longitude = 37.6208,
            timestampUtc = 11000L // +10 секунд
        )

        val result = useCase(point2)

        assertTrue(result.isSuccess)
        val processed = result.getOrNull()
        assertTrue(processed is LocationProcessingResult.Processed, "Expected Processed but got: $processed")
        processed as LocationProcessingResult.Processed

        // Расстояние ~11 метров (0.0001 градуса ≈ 11м)
        assertTrue(processed.distanceDeltaM > 5.0, "Distance: ${processed.distanceDeltaM}")
        assertTrue(processed.distanceDeltaM < 20.0, "Distance: ${processed.distanceDeltaM}")
    }

    @Test
    fun `uses GPS speed when available`() {
        val (useCase, _) = createUseCase()

        val point1 = TrackPoint(
            latitude = 55.7539,
            longitude = 37.6208,
            timestampUtc = 1000L
        )
        useCase(point1)

        // GPS сообщает скорость 10 м/с, небольшое перемещение за 10 секунд
        val point2 = TrackPoint(
            latitude = 55.7540,
            longitude = 37.6208,
            timestampUtc = 11000L, // +10 секунд чтобы избежать position jump
            speed = 10.0
        )

        val result = useCase(point2)
        val processed = result.getOrNull()
        assertTrue(processed is LocationProcessingResult.Processed, "Expected Processed but got: $processed")
        processed as LocationProcessingResult.Processed

        // Должна использоваться скорость из GPS
        assertEquals(10.0, processed.speedMs)
        assertEquals(36.0, processed.speedKmh, 0.1)
    }

    @Test
    fun `filters position jumps GPS drift`() {
        val (useCase, _) = createUseCase()

        val point1 = TrackPoint(
            latitude = 55.7539,
            longitude = 37.6208,
            timestampUtc = 1000L
        )
        useCase(point1)

        // Аномальный прыжок на 1км за 1 секунду
        val point2 = TrackPoint(
            latitude = 55.7639, // +0.01 градуса ≈ 1.1км
            longitude = 37.6208,
            timestampUtc = 2000L
        )

        val result = useCase(point2)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is LocationProcessingResult.Skipped)
    }

    @Test
    fun `skips reading without longitude`() {
        val (useCase, _) = createUseCase()

        val point1 = TrackPoint(
            latitude = 55.7539,
            longitude = 37.6208,
            timestampUtc = 1000L
        )
        useCase(point1)

        // Точка без longitude
        val point2 = TrackPoint(
            latitude = 55.7549,
            longitude = null,
            timestampUtc = 2000L
        )

        val result = useCase(point2)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is LocationProcessingResult.Skipped)
    }

    @Test
    fun `fails when trip not recording`() {
        val stateManager = TripStateManager() // Idle state
        val metricsAggregator = MetricsAggregator()

        val useCase = ProcessLocationDataUseCase(
            stateManager = stateManager,
            metricsAggregator = metricsAggregator
        )

        val point = TrackPoint(
            latitude = 55.7539,
            longitude = 37.6208,
            timestampUtc = 1000L
        )

        val result = useCase(point)

        assertTrue(result.isFailure)
    }

    @Test
    fun `reset clears previous point`() {
        val (useCase, _) = createUseCase()

        // Первое показание
        useCase(TrackPoint(latitude = 55.7539, longitude = 37.6208, timestampUtc = 1000L))

        // Второе показание (обработается)
        val result1 = useCase(TrackPoint(latitude = 55.7540, longitude = 37.6208, timestampUtc = 2000L))
        assertTrue(result1.getOrNull() is LocationProcessingResult.Processed)

        // Reset
        useCase.reset()

        // После reset - снова FirstReading
        val result2 = useCase(TrackPoint(latitude = 55.7550, longitude = 37.6208, timestampUtc = 3000L))
        assertTrue(result2.getOrNull() is LocationProcessingResult.FirstReading)
    }

    @Test
    fun `includes elevation when available`() {
        val (useCase, _) = createUseCase()

        useCase(TrackPoint(latitude = 55.7539, longitude = 37.6208, timestampUtc = 1000L, elevation = 150.0))

        val point2 = TrackPoint(
            latitude = 55.7540,
            longitude = 37.6208,
            timestampUtc = 2000L,
            elevation = 155.0
        )

        val result = useCase(point2)
        val processed = result.getOrNull() as LocationProcessingResult.Processed

        assertEquals(155.0, processed.elevation)
    }

    @Test
    fun `returns correct position`() {
        val (useCase, _) = createUseCase()

        useCase(TrackPoint(latitude = 55.7539, longitude = 37.6208, timestampUtc = 1000L))

        val point2 = TrackPoint(
            latitude = 55.7540,
            longitude = 37.6210,
            timestampUtc = 2000L
        )

        val result = useCase(point2)
        val processed = result.getOrNull() as LocationProcessingResult.Processed

        assertEquals(55.7540, processed.position.latitude)
        assertEquals(37.6210, processed.position.longitude)
    }
}
