package ru.lopon.domain.processing

import ru.lopon.domain.model.GeoCoordinate


object DataFusionProcessor {

    const val GPS_MAX_AGE_MS = 5000L

    const val MAX_DISTANCE_DISCREPANCY_METERS = 50.0

    const val MIN_POINTS_FOR_VECTOR = 2

    data class FusionResult(
        val position: GeoCoordinate?,
        val distanceMeters: Double,
        val speedKmh: Double,
        val source: String,
        val gpsAccuracyMeters: Double? = null,
        val confidence: Double = 1.0
    ) {
        companion object {
            const val SOURCE_GPS = "gps"
            const val SOURCE_EXTRAPOLATED = "extrapolated"
            const val SOURCE_CORRECTED = "corrected"
            const val SOURCE_BLE_ONLY = "ble_only"
        }
    }

    data class FusionState(
        val lastGpsPoints: List<GpsPoint> = emptyList(),
        val totalBleDistanceMeters: Double = 0.0,
        val totalGpsDistanceMeters: Double = 0.0,
        val lastGpsTimestamp: Long? = null,
        val lastBearing: Double? = null
    ) {

        data class GpsPoint(
            val coordinate: GeoCoordinate,
            val timestampUtc: Long,
            val accuracyMeters: Double? = null,
            val speedMps: Double? = null
        )

        companion object {
            const val MAX_GPS_HISTORY = 10
        }
    }

    fun updateWithGps(
        state: FusionState,
        gpsPoint: FusionState.GpsPoint
    ): FusionState {
        val updatedPoints = (state.lastGpsPoints + gpsPoint)
            .takeLast(FusionState.MAX_GPS_HISTORY)

        val gpsDistanceIncrement = if (state.lastGpsPoints.isNotEmpty()) {
            RouteCalculator.haversineDistance(
                state.lastGpsPoints.last().coordinate,
                gpsPoint.coordinate
            )
        } else 0.0

        val bearing = if (state.lastGpsPoints.isNotEmpty()) {
            RouteCalculator.calculateBearing(
                state.lastGpsPoints.last().coordinate,
                gpsPoint.coordinate
            )
        } else state.lastBearing

        return state.copy(
            lastGpsPoints = updatedPoints,
            totalGpsDistanceMeters = state.totalGpsDistanceMeters + gpsDistanceIncrement,
            lastGpsTimestamp = gpsPoint.timestampUtc,
            lastBearing = bearing
        )
    }


    fun updateWithBle(
        state: FusionState,
        distanceIncrementMeters: Double
    ): FusionState {
        return state.copy(
            totalBleDistanceMeters = state.totalBleDistanceMeters + distanceIncrementMeters
        )
    }

    fun fuse(
        state: FusionState,
        currentTimeUtc: Long,
        bleSpeedKmh: Double
    ): FusionResult {
        val lastGps = state.lastGpsPoints.lastOrNull()

        val gpsAge = if (lastGps != null) {
            currentTimeUtc - lastGps.timestampUtc
        } else Long.MAX_VALUE

        val gpsAvailable = gpsAge < GPS_MAX_AGE_MS

        return if (gpsAvailable && lastGps != null) {
            FusionResult(
                position = lastGps.coordinate,
                distanceMeters = state.totalBleDistanceMeters,
                speedKmh = bleSpeedKmh,
                source = FusionResult.SOURCE_GPS,
                gpsAccuracyMeters = lastGps.accuracyMeters,
                confidence = calculateGpsConfidence(lastGps.accuracyMeters, gpsAge)
            )
        } else if (state.lastGpsPoints.size >= MIN_POINTS_FOR_VECTOR && state.lastBearing != null) {
            val extrapolatedPosition = extrapolatePosition(state, currentTimeUtc, bleSpeedKmh)
            FusionResult(
                position = extrapolatedPosition,
                distanceMeters = state.totalBleDistanceMeters,
                speedKmh = bleSpeedKmh,
                source = FusionResult.SOURCE_EXTRAPOLATED,
                confidence = calculateExtrapolationConfidence(gpsAge)
            )
        } else {
            FusionResult(
                position = lastGps?.coordinate,
                distanceMeters = state.totalBleDistanceMeters,
                speedKmh = bleSpeedKmh,
                source = FusionResult.SOURCE_BLE_ONLY,
                confidence = 0.3
            )
        }
    }

    private fun extrapolatePosition(
        state: FusionState,
        currentTimeUtc: Long,
        speedKmh: Double
    ): GeoCoordinate? {
        val lastGps = state.lastGpsPoints.lastOrNull() ?: return null
        val bearing = state.lastBearing ?: return null

        val timeSinceLastGps = (currentTimeUtc - lastGps.timestampUtc) / 1000.0 // секунды
        val distanceMeters = (speedKmh / 3.6) * timeSinceLastGps

        return movePoint(lastGps.coordinate, bearing, distanceMeters)
    }

    private fun movePoint(
        start: GeoCoordinate,
        bearingDegrees: Double,
        distanceMeters: Double
    ): GeoCoordinate {
        val earthRadius = 6371000.0 // метры

        val lat1 = start.latitude * kotlin.math.PI / 180
        val lon1 = start.longitude * kotlin.math.PI / 180
        val bearing = bearingDegrees * kotlin.math.PI / 180
        val angularDistance = distanceMeters / earthRadius

        val lat2 = kotlin.math.asin(
            kotlin.math.sin(lat1) * kotlin.math.cos(angularDistance) +
                    kotlin.math.cos(lat1) * kotlin.math.sin(angularDistance) * kotlin.math.cos(bearing)
        )

        val lon2 = lon1 + kotlin.math.atan2(
            kotlin.math.sin(bearing) * kotlin.math.sin(angularDistance) * kotlin.math.cos(lat1),
            kotlin.math.cos(angularDistance) - kotlin.math.sin(lat1) * kotlin.math.sin(lat2)
        )

        return GeoCoordinate(
            latitude = lat2 * 180 / kotlin.math.PI,
            longitude = lon2 * 180 / kotlin.math.PI
        )
    }

    //TODO: Некоторая эвристика, которую ещё придётся корректировать и тестировать в реальных условиях после начала велосезона
    private fun calculateGpsConfidence(accuracyMeters: Double?, ageMs: Long): Double {
        var confidence = 1.0

        accuracyMeters?.let {
            confidence *= when {
                it < 5.0 -> 1.0
                it < 10.0 -> 0.9
                it < 20.0 -> 0.7
                it < 50.0 -> 0.5
                else -> 0.3
            }
        }
        confidence *= when {
            ageMs < 1000 -> 1.0
            ageMs < 2000 -> 0.95
            ageMs < 3000 -> 0.9
            ageMs < 4000 -> 0.8
            else -> 0.7
        }

        return confidence
    }


    private fun calculateExtrapolationConfidence(gpsAgeMs: Long): Double {
        return when {
            gpsAgeMs < 10_000 -> 0.6
            gpsAgeMs < 30_000 -> 0.4
            gpsAgeMs < 60_000 -> 0.2
            else -> 0.1
        }
    }

    fun calculateDistanceDiscrepancy(state: FusionState): Double {
        return state.totalBleDistanceMeters - state.totalGpsDistanceMeters
    }

    fun needsCorrection(state: FusionState): Boolean {
        return kotlin.math.abs(calculateDistanceDiscrepancy(state)) > MAX_DISTANCE_DISCREPANCY_METERS
    }
}

