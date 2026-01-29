package ru.lopon.core.settings

import ru.lopon.domain.model.UnitSystem

object UnitConverter {
    private const val KM_TO_MI = 0.621371
    private const val M_TO_FT = 3.28084
    private const val MS_TO_KMH = 3.6

    fun msToKmh(ms: Double): Double = ms * MS_TO_KMH

    fun kmhToMs(kmh: Double): Double = kmh / MS_TO_KMH

    fun metersToDisplay(meters: Double, units: UnitSystem): Double {
        return when (units) {
            UnitSystem.METRIC -> meters / 1000.0
            UnitSystem.IMPERIAL -> meters / 1000.0 * KM_TO_MI
        }
    }

    fun displayToMeters(value: Double, units: UnitSystem): Double {
        return when (units) {
            UnitSystem.METRIC -> value * 1000.0
            UnitSystem.IMPERIAL -> value / KM_TO_MI * 1000.0
        }
    }

    fun displayUnitDistance(units: UnitSystem): String = when (units) {
        UnitSystem.METRIC -> "km"
        UnitSystem.IMPERIAL -> "mi"
    }

    fun msToDisplaySpeed(ms: Double, units: UnitSystem): Double {
        val kmh = ms * MS_TO_KMH
        return when (units) {
            UnitSystem.METRIC -> kmh
            UnitSystem.IMPERIAL -> kmh * KM_TO_MI
        }
    }

    fun kmhToDisplaySpeed(kmh: Double, units: UnitSystem): Double {
        return when (units) {
            UnitSystem.METRIC -> kmh
            UnitSystem.IMPERIAL -> kmh * KM_TO_MI
        }
    }

    fun displaySpeedToMs(value: Double, units: UnitSystem): Double {
        val kmh = when (units) {
            UnitSystem.METRIC -> value
            UnitSystem.IMPERIAL -> value / KM_TO_MI
        }
        return kmh / MS_TO_KMH
    }

    fun displayUnitSpeed(units: UnitSystem): String = when (units) {
        UnitSystem.METRIC -> "km/h"
        UnitSystem.IMPERIAL -> "mph"
    }

    fun metersToDisplayElevation(meters: Double, units: UnitSystem): Double {
        return when (units) {
            UnitSystem.METRIC -> meters
            UnitSystem.IMPERIAL -> meters * M_TO_FT
        }
    }

    fun displayToMetersElevation(value: Double, units: UnitSystem): Double {
        return when (units) {
            UnitSystem.METRIC -> value
            UnitSystem.IMPERIAL -> value / M_TO_FT
        }
    }

    fun displayUnitElevation(units: UnitSystem): String = when (units) {
        UnitSystem.METRIC -> "m"
        UnitSystem.IMPERIAL -> "ft"
    }

    fun formatDistance(meters: Double, units: UnitSystem, decimals: Int = 2): String {
        val value = metersToDisplay(meters, units)
        val unit = displayUnitDistance(units)
        return "${formatDouble(value, decimals)} $unit"
    }

    fun formatSpeed(ms: Double, units: UnitSystem, decimals: Int = 1): String {
        val value = msToDisplaySpeed(ms, units)
        val unit = displayUnitSpeed(units)
        return "${formatDouble(value, decimals)} $unit"
    }

    fun formatElevation(meters: Double, units: UnitSystem, decimals: Int = 0): String {
        val value = metersToDisplayElevation(meters, units)
        val unit = displayUnitElevation(units)
        return "${formatDouble(value, decimals)} $unit"
    }

    private fun formatDouble(value: Double, decimals: Int): String {
        if (decimals <= 0) {
            return kotlin.math.round(value).toLong().toString()
        }

        val multiplier = tenPow(decimals)
        val rounded = kotlin.math.round(value * multiplier) / multiplier

        val parts = rounded.toString().split(".")
        val intPart = parts[0]
        val fracPart = if (parts.size > 1) parts[1] else ""

        return if (decimals > 0) {
            "$intPart.${fracPart.padEnd(decimals, '0').take(decimals)}"
        } else {
            intPart
        }
    }

    private fun tenPow(n: Int): Double {
        var result = 1.0
        repeat(n) { result *= 10.0 }
        return result
    }
}
