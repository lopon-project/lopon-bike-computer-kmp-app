package ru.lopon.core.metrics

import kotlin.math.roundToInt

object MetricsFormatter {

    fun formatSpeedKmh(speedMs: Double, decimals: Int = 1): String {
        val speedKmh = speedMs * 3.6
        return formatDouble(speedKmh, decimals)
    }

    fun formatSpeedKmhWithUnit(speedMs: Double, decimals: Int = 1): String {
        return "${formatSpeedKmh(speedMs, decimals)} км/ч"
    }

    fun formatTimeHHMMSS(timeMs: Long): String {
        if (timeMs < 0) return "00:00:00"

        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return buildString {
            append(hours.toString().padStart(2, '0'))
            append(':')
            append(minutes.toString().padStart(2, '0'))
            append(':')
            append(seconds.toString().padStart(2, '0'))
        }
    }

    fun formatTimeCompact(timeMs: Long): String {
        if (timeMs < 0) return "00:00"

        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            buildString {
                append(hours)
                append(':')
                append(minutes.toString().padStart(2, '0'))
                append(':')
                append(seconds.toString().padStart(2, '0'))
            }
        } else {
            buildString {
                append(minutes.toString().padStart(2, '0'))
                append(':')
                append(seconds.toString().padStart(2, '0'))
            }
        }
    }

    fun formatDistanceKm(distanceM: Double, decimals: Int = 2): String {
        val distanceKm = distanceM / 1000.0
        return formatDouble(distanceKm, decimals)
    }

    fun formatDistanceKmWithUnit(distanceM: Double, decimals: Int = 2): String {
        return "${formatDistanceKm(distanceM, decimals)} км"
    }

    fun formatDistanceAdaptive(distanceM: Double, thresholdM: Double = 1000.0): String {
        return if (distanceM < thresholdM) {
            "${distanceM.roundToInt()} м"
        } else {
            formatDistanceKmWithUnit(distanceM)
        }
    }

    fun formatCadence(cadenceRpm: Double?): String {
        return cadenceRpm?.roundToInt()?.toString() ?: "--"
    }

    fun formatCadenceWithUnit(cadenceRpm: Double?): String {
        return "${formatCadence(cadenceRpm)} об/мин"
    }

    fun formatElevationGain(elevationGainM: Double?): String {
        return elevationGainM?.roundToInt()?.toString() ?: "--"
    }

    fun formatElevationGainWithUnit(elevationGainM: Double?): String {
        return "${formatElevationGain(elevationGainM)} м"
    }

    private fun formatDouble(value: Double, decimals: Int): String {
        if (decimals <= 0) {
            return value.roundToInt().toString()
        }

        val multiplier = pow10(decimals)
        val rounded = (value * multiplier).roundToInt() / multiplier.toDouble()

        val intPart = rounded.toLong()
        val fracPart = ((rounded - intPart) * multiplier).roundToInt()

        return if (decimals > 0) {
            "$intPart.${fracPart.toString().padStart(decimals, '0')}"
        } else {
            intPart.toString()
        }
    }

    private fun pow10(n: Int): Int {
        var result = 1
        repeat(n) { result *= 10 }
        return result
    }
}

val TripMetrics.formattedCurrentSpeedKmh: String
    get() = MetricsFormatter.formatSpeedKmh(currentSpeedMs)

val TripMetrics.formattedAverageSpeedKmh: String
    get() = MetricsFormatter.formatSpeedKmh(averageSpeedMs)

val TripMetrics.formattedMaxSpeedKmh: String
    get() = MetricsFormatter.formatSpeedKmh(maxSpeedMs)

val TripMetrics.formattedMovingTime: String
    get() = MetricsFormatter.formatTimeHHMMSS(movingTimeMs)

val TripMetrics.formattedElapsedTime: String
    get() = MetricsFormatter.formatTimeHHMMSS(elapsedTimeMs)

val TripMetrics.formattedDistanceKm: String
    get() = MetricsFormatter.formatDistanceKm(totalDistanceM)

val TripMetrics.formattedCadence: String
    get() = MetricsFormatter.formatCadence(currentCadenceRpm)

val TripMetrics.formattedElevationGain: String
    get() = MetricsFormatter.formatElevationGain(elevationGainM)

