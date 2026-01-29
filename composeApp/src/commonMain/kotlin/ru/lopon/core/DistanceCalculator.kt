package ru.lopon.core


object DistanceCalculator {

    const val MIN_WHEEL_CIRCUMFERENCE_MM = 1000.0

    const val MAX_WHEEL_CIRCUMFERENCE_MM = 3500.0

    const val DEFAULT_WHEEL_CIRCUMFERENCE_MM = 2100.0

    fun calculateDistanceMeters(revolutions: Long, wheelCircumferenceMm: Double): Double {
        if (revolutions <= 0 || wheelCircumferenceMm <= 0.0) {
            return 0.0
        }
        val circumferenceMeters = wheelCircumferenceMm / 1000.0
        return revolutions * circumferenceMeters
    }

    fun calculateDistanceMetersValidated(
        revolutions: Long,
        wheelCircumferenceMm: Double
    ): Result<Double> {
        if (wheelCircumferenceMm !in MIN_WHEEL_CIRCUMFERENCE_MM..MAX_WHEEL_CIRCUMFERENCE_MM) {
            return Result.failure(
                IllegalArgumentException(
                    "Wheel circumference must be between " +
                            "$MIN_WHEEL_CIRCUMFERENCE_MM and $MAX_WHEEL_CIRCUMFERENCE_MM mm, " +
                            "but was $wheelCircumferenceMm mm"
                )
            )
        }

        return Result.success(calculateDistanceMeters(revolutions, wheelCircumferenceMm))
    }

    fun isValidWheelCircumference(wheelCircumferenceMm: Double): Boolean {
        return wheelCircumferenceMm in MIN_WHEEL_CIRCUMFERENCE_MM..MAX_WHEEL_CIRCUMFERENCE_MM
    }
}

