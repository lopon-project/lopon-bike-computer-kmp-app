package ru.lopon.domain.processing

import ru.lopon.domain.model.GeoCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


class DataFusionProcessorTest {

    @Test
    fun `updateWithGps adds point to history`() {
        var state = DataFusionProcessor.FusionState()
        val gpsPoint = DataFusionProcessor.FusionState.GpsPoint(
            coordinate = GeoCoordinate(55.75, 37.62),
            timestampUtc = 1000L,
            accuracyMeters = 5.0
        )

        state = DataFusionProcessor.updateWithGps(state, gpsPoint)

        assertEquals(1, state.lastGpsPoints.size)
        assertEquals(1000L, state.lastGpsTimestamp)
    }

    @Test
    fun `updateWithGps calculates distance between consecutive points`() {
        var state = DataFusionProcessor.FusionState()
        val point1 = DataFusionProcessor.FusionState.GpsPoint(
            coordinate = GeoCoordinate(55.75, 37.62),
            timestampUtc = 1000L
        )
        val point2 = DataFusionProcessor.FusionState.GpsPoint(
            coordinate = GeoCoordinate(55.76, 37.62), // ~1.1km на север
            timestampUtc = 2000L
        )

        state = DataFusionProcessor.updateWithGps(state, point1)
        state = DataFusionProcessor.updateWithGps(state, point2)

        assertTrue(state.totalGpsDistanceMeters > 1000 && state.totalGpsDistanceMeters < 1200)
    }

    @Test
    fun `updateWithGps calculates bearing`() {
        var state = DataFusionProcessor.FusionState()
        val point1 = DataFusionProcessor.FusionState.GpsPoint(
            coordinate = GeoCoordinate(55.75, 37.62),
            timestampUtc = 1000L
        )
        val point2 = DataFusionProcessor.FusionState.GpsPoint(
            coordinate = GeoCoordinate(55.76, 37.62), // на север
            timestampUtc = 2000L
        )

        state = DataFusionProcessor.updateWithGps(state, point1)
        state = DataFusionProcessor.updateWithGps(state, point2)

        val bearing = state.lastBearing
        assertNotNull(bearing)
        assertTrue(bearing !in 5.0..355.0)
    }

    @Test
    fun `updateWithGps limits history size`() {
        var state = DataFusionProcessor.FusionState()

        repeat(15) { i ->
            state = DataFusionProcessor.updateWithGps(
                state,
                DataFusionProcessor.FusionState.GpsPoint(
                    coordinate = GeoCoordinate(55.75 + i * 0.001, 37.62),
                    timestampUtc = i * 1000L
                )
            )
        }

        assertEquals(DataFusionProcessor.FusionState.MAX_GPS_HISTORY, state.lastGpsPoints.size)
    }

    @Test
    fun `updateWithBle increments total distance`() {
        var state = DataFusionProcessor.FusionState()

        state = DataFusionProcessor.updateWithBle(state, 100.0)
        state = DataFusionProcessor.updateWithBle(state, 50.0)

        assertEquals(150.0, state.totalBleDistanceMeters)
    }

    @Test
    fun `fuse returns GPS source when GPS is fresh`() {
        var state = DataFusionProcessor.FusionState()
        val gpsPoint = DataFusionProcessor.FusionState.GpsPoint(
            coordinate = GeoCoordinate(55.75, 37.62),
            timestampUtc = 1000L,
            accuracyMeters = 3.0
        )
        state = DataFusionProcessor.updateWithGps(state, gpsPoint)
        state = DataFusionProcessor.updateWithBle(state, 100.0)

        val result = DataFusionProcessor.fuse(
            state,
            currentTimeUtc = 1500L,
            bleSpeedKmh = 30.0
        )

        assertEquals(DataFusionProcessor.FusionResult.SOURCE_GPS, result.source)
        assertEquals(55.75, result.position?.latitude)
        assertEquals(100.0, result.distanceMeters)
        assertEquals(30.0, result.speedKmh)
        assertTrue(result.confidence > 0.9)
    }

    @Test
    fun `fuse returns extrapolated source when GPS is stale`() {
        var state = DataFusionProcessor.FusionState()

        state = DataFusionProcessor.updateWithGps(
            state,
            DataFusionProcessor.FusionState.GpsPoint(
                coordinate = GeoCoordinate(55.75, 37.62),
                timestampUtc = 1000L
            )
        )
        state = DataFusionProcessor.updateWithGps(
            state,
            DataFusionProcessor.FusionState.GpsPoint(
                coordinate = GeoCoordinate(55.76, 37.62),
                timestampUtc = 2000L
            )
        )
        state = DataFusionProcessor.updateWithBle(state, 100.0)

        val result = DataFusionProcessor.fuse(
            state,
            currentTimeUtc = 10000L,
            bleSpeedKmh = 30.0
        )

        assertEquals(DataFusionProcessor.FusionResult.SOURCE_EXTRAPOLATED, result.source)
        assertNotNull(result.position)
        assertTrue(result.confidence < 0.7)
    }

    @Test
    fun `fuse returns BLE only when no GPS history`() {
        val state = DataFusionProcessor.FusionState(
            totalBleDistanceMeters = 100.0
        )

        val result = DataFusionProcessor.fuse(
            state,
            currentTimeUtc = 1000L,
            bleSpeedKmh = 30.0
        )

        assertEquals(DataFusionProcessor.FusionResult.SOURCE_BLE_ONLY, result.source)
        assertNull(result.position)
        assertEquals(100.0, result.distanceMeters)
        assertTrue(result.confidence < 0.5)
    }

    @Test
    fun `calculateDistanceDiscrepancy returns difference between BLE and GPS`() {
        val state = DataFusionProcessor.FusionState(
            totalBleDistanceMeters = 1100.0,
            totalGpsDistanceMeters = 1000.0
        )

        val discrepancy = DataFusionProcessor.calculateDistanceDiscrepancy(state)

        assertEquals(100.0, discrepancy)
    }

    @Test
    fun `needsCorrection returns true when discrepancy exceeds threshold`() {
        val state = DataFusionProcessor.FusionState(
            totalBleDistanceMeters = 1100.0,
            totalGpsDistanceMeters = 1000.0
        )

        assertTrue(DataFusionProcessor.needsCorrection(state))
    }

    @Test
    fun `needsCorrection returns false when discrepancy is within threshold`() {
        val state = DataFusionProcessor.FusionState(
            totalBleDistanceMeters = 1020.0,
            totalGpsDistanceMeters = 1000.0
        )

        assertFalse(DataFusionProcessor.needsCorrection(state))
    }

    @Test
    fun `fuse calculates correct confidence for accurate GPS`() {
        var state = DataFusionProcessor.FusionState()
        state = DataFusionProcessor.updateWithGps(
            state,
            DataFusionProcessor.FusionState.GpsPoint(
                coordinate = GeoCoordinate(55.75, 37.62),
                timestampUtc = 1000L,
                accuracyMeters = 3.0
            )
        )

        val result = DataFusionProcessor.fuse(
            state,
            currentTimeUtc = 1500L,
            bleSpeedKmh = 30.0
        )

        assertTrue(result.confidence > 0.95)
    }

    @Test
    fun `fuse reduces confidence for inaccurate GPS`() {
        var state = DataFusionProcessor.FusionState()
        state = DataFusionProcessor.updateWithGps(
            state,
            DataFusionProcessor.FusionState.GpsPoint(
                coordinate = GeoCoordinate(55.75, 37.62),
                timestampUtc = 1000L,
                accuracyMeters = 30.0
            )
        )

        val result = DataFusionProcessor.fuse(
            state,
            currentTimeUtc = 1500L,
            bleSpeedKmh = 30.0
        )

        assertTrue(result.confidence < 0.8)
    }
}

