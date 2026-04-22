package ru.lopon.core.metrics

import kotlin.test.Test
import kotlin.test.assertEquals

class MetricsFormatterTest {

    // ============================================
    // Speed formatting tests
    // ============================================

    @Test
    fun `formatSpeedKmh converts ms to kmh correctly`() {
        // 10 м/с = 36 км/ч
        assertEquals("36.0", MetricsFormatter.formatSpeedKmh(10.0))
    }

    @Test
    fun `formatSpeedKmh handles zero speed`() {
        assertEquals("0.0", MetricsFormatter.formatSpeedKmh(0.0))
    }

    @Test
    fun `formatSpeedKmh respects decimals parameter`() {
        // 10 м/с = 36 км/ч
        assertEquals("36", MetricsFormatter.formatSpeedKmh(10.0, decimals = 0))
        assertEquals("36.00", MetricsFormatter.formatSpeedKmh(10.0, decimals = 2))
    }

    @Test
    fun `formatSpeedKmh rounds correctly`() {
        // 5.5 м/с = 19.8 км/ч
        assertEquals("19.8", MetricsFormatter.formatSpeedKmh(5.5))
        // 7.03 м/с = 25.308 км/ч → 25.3
        assertEquals("25.3", MetricsFormatter.formatSpeedKmh(7.03))
    }

    @Test
    fun `formatSpeedKmhWithUnit includes unit`() {
        assertEquals("36.0 км/ч", MetricsFormatter.formatSpeedKmhWithUnit(10.0))
    }

    // ============================================
    // Time formatting tests
    // ============================================

    @Test
    fun `formatTimeHHMMSS formats zero correctly`() {
        assertEquals("00:00:00", MetricsFormatter.formatTimeHHMMSS(0))
    }

    @Test
    fun `formatTimeHHMMSS formats seconds correctly`() {
        assertEquals("00:00:30", MetricsFormatter.formatTimeHHMMSS(30_000))
    }

    @Test
    fun `formatTimeHHMMSS formats minutes correctly`() {
        assertEquals("00:05:30", MetricsFormatter.formatTimeHHMMSS(330_000))
    }

    @Test
    fun `formatTimeHHMMSS formats hours correctly`() {
        assertEquals("01:23:45", MetricsFormatter.formatTimeHHMMSS(5_025_000))
    }

    @Test
    fun `formatTimeHHMMSS formats large hours correctly`() {
        // 10 часов, 30 минут, 15 секунд
        val timeMs = (10 * 3600 + 30 * 60 + 15) * 1000L
        assertEquals("10:30:15", MetricsFormatter.formatTimeHHMMSS(timeMs))
    }

    @Test
    fun `formatTimeHHMMSS handles negative time`() {
        assertEquals("00:00:00", MetricsFormatter.formatTimeHHMMSS(-1000))
    }

    @Test
    fun `formatTimeCompact omits hours when zero`() {
        assertEquals("05:30", MetricsFormatter.formatTimeCompact(330_000))
    }

    @Test
    fun `formatTimeCompact includes hours when present`() {
        assertEquals("1:23:45", MetricsFormatter.formatTimeCompact(5_025_000))
    }

    @Test
    fun `formatTimeCompact handles negative time`() {
        assertEquals("00:00", MetricsFormatter.formatTimeCompact(-1000))
    }

    // ============================================
    // Distance formatting tests
    // ============================================

    @Test
    fun `formatDistanceKm converts meters to km`() {
        assertEquals("1.00", MetricsFormatter.formatDistanceKm(1000.0))
        assertEquals("12.34", MetricsFormatter.formatDistanceKm(12340.0))
    }

    @Test
    fun `formatDistanceKm handles small distances`() {
        assertEquals("0.50", MetricsFormatter.formatDistanceKm(500.0))
        assertEquals("0.10", MetricsFormatter.formatDistanceKm(100.0))
    }

    @Test
    fun `formatDistanceKmWithUnit includes unit`() {
        assertEquals("1.00 км", MetricsFormatter.formatDistanceKmWithUnit(1000.0))
    }

    @Test
    fun `formatDistanceAdaptive uses meters for short distances`() {
        assertEquals("500 м", MetricsFormatter.formatDistanceAdaptive(500.0))
        assertEquals("999 м", MetricsFormatter.formatDistanceAdaptive(999.0))
    }

    @Test
    fun `formatDistanceAdaptive uses km for long distances`() {
        assertEquals("1.00 км", MetricsFormatter.formatDistanceAdaptive(1000.0))
        assertEquals("5.55 км", MetricsFormatter.formatDistanceAdaptive(5550.0))
    }

    // ============================================
    // Cadence formatting tests
    // ============================================

    @Test
    fun `formatCadence formats value correctly`() {
        assertEquals("85", MetricsFormatter.formatCadence(85.0))
        assertEquals("90", MetricsFormatter.formatCadence(89.6))
    }

    @Test
    fun `formatCadence handles null`() {
        assertEquals("--", MetricsFormatter.formatCadence(null))
    }

    @Test
    fun `formatCadenceWithUnit includes unit`() {
        assertEquals("85 об/мин", MetricsFormatter.formatCadenceWithUnit(85.0))
        assertEquals("-- об/мин", MetricsFormatter.formatCadenceWithUnit(null))
    }

    // ============================================
    // Elevation formatting tests
    // ============================================

    @Test
    fun `formatElevationGain formats value correctly`() {
        assertEquals("150", MetricsFormatter.formatElevationGain(150.0))
        assertEquals("151", MetricsFormatter.formatElevationGain(150.6))
    }

    @Test
    fun `formatElevationGain handles null`() {
        assertEquals("--", MetricsFormatter.formatElevationGain(null))
    }

    @Test
    fun `formatElevationGainWithUnit includes unit`() {
        assertEquals("150 м", MetricsFormatter.formatElevationGainWithUnit(150.0))
        assertEquals("-- м", MetricsFormatter.formatElevationGainWithUnit(null))
    }

    // ============================================
    // TripMetrics extension tests
    // ============================================

    @Test
    fun `TripMetrics formattedCurrentSpeedKmh works`() {
        val metrics = TripMetrics(currentSpeedMs = 10.0)
        assertEquals("36.0", metrics.formattedCurrentSpeedKmh)
    }

    @Test
    fun `TripMetrics formattedAverageSpeedKmh works`() {
        val metrics = TripMetrics(averageSpeedMs = 8.0)
        assertEquals("28.8", metrics.formattedAverageSpeedKmh)
    }

    @Test
    fun `TripMetrics formattedMaxSpeedKmh works`() {
        val metrics = TripMetrics(maxSpeedMs = 15.0)
        assertEquals("54.0", metrics.formattedMaxSpeedKmh)
    }

    @Test
    fun `TripMetrics formattedMovingTime works`() {
        val metrics = TripMetrics(movingTimeMs = 3661_000) // 1h 1m 1s
        assertEquals("01:01:01", metrics.formattedMovingTime)
    }

    @Test
    fun `TripMetrics formattedElapsedTime works`() {
        val metrics = TripMetrics(elapsedTimeMs = 7322_000) // 2h 2m 2s
        assertEquals("02:02:02", metrics.formattedElapsedTime)
    }

    @Test
    fun `TripMetrics formattedDistanceKm works`() {
        val metrics = TripMetrics(totalDistanceM = 25670.0)
        assertEquals("25.67", metrics.formattedDistanceKm)
    }

    @Test
    fun `TripMetrics formattedCadence works`() {
        val metricsWithCadence = TripMetrics(currentCadenceRpm = 75.0)
        assertEquals("75", metricsWithCadence.formattedCadence)

        val metricsWithoutCadence = TripMetrics(currentCadenceRpm = null)
        assertEquals("--", metricsWithoutCadence.formattedCadence)
    }

    @Test
    fun `TripMetrics formattedElevationGain works`() {
        val metricsWithElevation = TripMetrics(elevationGainM = 320.0)
        assertEquals("320", metricsWithElevation.formattedElevationGain)

        val metricsWithoutElevation = TripMetrics(elevationGainM = null)
        assertEquals("--", metricsWithoutElevation.formattedElevationGain)
    }

    // ============================================
    // Edge cases
    // ============================================

    @Test
    fun `formatSpeedKmh handles very small speeds`() {
        assertEquals("0.4", MetricsFormatter.formatSpeedKmh(0.1))
    }

    @Test
    fun `formatSpeedKmh handles very large speeds`() {
        // 30 м/с = 108 км/ч
        assertEquals("108.0", MetricsFormatter.formatSpeedKmh(30.0))
    }

    @Test
    fun `formatTimeHHMMSS handles exactly one hour`() {
        assertEquals("01:00:00", MetricsFormatter.formatTimeHHMMSS(3600_000))
    }

    @Test
    fun `formatTimeHHMMSS handles exactly one minute`() {
        assertEquals("00:01:00", MetricsFormatter.formatTimeHHMMSS(60_000))
    }
}

