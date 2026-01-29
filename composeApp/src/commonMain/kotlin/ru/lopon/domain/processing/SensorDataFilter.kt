package ru.lopon.domain.processing

import ru.lopon.core.settings.UnitConverter
import ru.lopon.domain.model.SensorReading

//TODO: Все константы для определения аномалий тоже пока выбраны на глаз, требуют реальных экспериментов и корректировок.
object SensorDataFilter {

    const val MAX_SPEED_KMH = 100.0

    const val MIN_SPEED_KMH = 1.0

    const val MAX_CADENCE_RPM = 200.0

    const val MIN_CADENCE_RPM = 20.0

    const val MAX_ACCELERATION_MS2 = 10.0

    const val MIN_TIME_DELTA_SECONDS = 0.01

    data class ValidationResult(
        val isValid: Boolean,
        val reason: InvalidReason? = null,
        val filteredReading: SensorReading? = null
    ) {
        companion object {
            fun valid(reading: SensorReading) = ValidationResult(
                isValid = true,
                filteredReading = reading
            )

            fun invalid(reason: InvalidReason) = ValidationResult(
                isValid = false,
                reason = reason
            )
        }
    }

    enum class InvalidReason {
        SPEED_TOO_HIGH,

        ACCELERATION_TOO_HIGH,

        INVALID_TIME_DELTA,

        CADENCE_TOO_HIGH,

        NEGATIVE_REVOLUTIONS,

        DUPLICATE_DATA
    }

    fun calculateSpeedKmh(
        current: SensorReading,
        previous: SensorReading,
        wheelCircumferenceMm: Double
    ): Double? {
        val timeDeltaSeconds = current.timeDeltaSeconds(previous)
        if (timeDeltaSeconds < MIN_TIME_DELTA_SECONDS) {
            return null
        }

        val revolutions = current.revolutionsDelta(previous)
        if (revolutions < 0) {
            return null
        }

        val distanceMeters = revolutions * (wheelCircumferenceMm / 1000.0)
        val speedMs = distanceMeters / timeDeltaSeconds
        return UnitConverter.msToKmh(speedMs)
    }

    fun validate(
        current: SensorReading,
        previous: SensorReading,
        wheelCircumferenceMm: Double,
        previousSpeedKmh: Double? = null
    ): ValidationResult {
        val revolutions = current.revolutionsDelta(previous)
        if (revolutions < 0) {
            return ValidationResult.invalid(InvalidReason.NEGATIVE_REVOLUTIONS)
        }

        val timeDeltaSeconds = current.timeDeltaSeconds(previous)
        if (timeDeltaSeconds < MIN_TIME_DELTA_SECONDS) {
            return ValidationResult.invalid(InvalidReason.INVALID_TIME_DELTA)
        }

        if (revolutions == 0L && timeDeltaSeconds < 0.1) {
            return ValidationResult.invalid(InvalidReason.DUPLICATE_DATA)
        }

        val speedKmh = calculateSpeedKmh(current, previous, wheelCircumferenceMm)
        if (speedKmh != null && speedKmh > MAX_SPEED_KMH) {
            return ValidationResult.invalid(InvalidReason.SPEED_TOO_HIGH)
        }

        if (speedKmh != null && previousSpeedKmh != null) {
            val speedDeltaMs = UnitConverter.kmhToMs(speedKmh - previousSpeedKmh)
            val acceleration = kotlin.math.abs(speedDeltaMs / timeDeltaSeconds)
            if (acceleration > MAX_ACCELERATION_MS2) {
                return ValidationResult.invalid(InvalidReason.ACCELERATION_TOO_HIGH)
            }
        }

        current.cadence?.let { cadence ->
            if (cadence > MAX_CADENCE_RPM) {
                return ValidationResult.invalid(InvalidReason.CADENCE_TOO_HIGH)
            }
        }

        return ValidationResult.valid(current)
    }

    fun filterCadence(cadence: Double?): Double? {
        if (cadence == null) return null
        if (cadence !in MIN_CADENCE_RPM..MAX_CADENCE_RPM) return null
        return cadence
    }

    fun filterSpeed(speedKmh: Double): Double {
        return when {
            speedKmh < MIN_SPEED_KMH -> 0.0
            speedKmh > MAX_SPEED_KMH -> MAX_SPEED_KMH
            else -> speedKmh
        }
    }
}

