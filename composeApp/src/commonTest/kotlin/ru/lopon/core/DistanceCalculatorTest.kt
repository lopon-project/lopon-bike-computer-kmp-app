package ru.lopon.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DistanceCalculatorTest {

    @Test
    fun `calculateDistanceMeters with zero revolutions returns zero`() {
        val result = DistanceCalculator.calculateDistanceMeters(
            revolutions = 0,
            wheelCircumferenceMm = 2100.0
        )
        assertEquals(0.0, result, "Zero revolutions should return 0 meters")
    }

    @Test
    fun `calculateDistanceMeters with negative revolutions returns zero`() {
        val result = DistanceCalculator.calculateDistanceMeters(
            revolutions = -10,
            wheelCircumferenceMm = 2100.0
        )
        assertEquals(0.0, result, "Negative revolutions should return 0 meters")
    }

    @Test
    fun `calculateDistanceMeters with zero circumference returns zero`() {
        val result = DistanceCalculator.calculateDistanceMeters(
            revolutions = 100,
            wheelCircumferenceMm = 0.0
        )
        assertEquals(0.0, result, "Zero circumference should return 0 meters")
    }

    @Test
    fun `calculateDistanceMeters with negative circumference returns zero`() {
        val result = DistanceCalculator.calculateDistanceMeters(
            revolutions = 100,
            wheelCircumferenceMm = -2100.0
        )
        assertEquals(0.0, result, "Negative circumference should return 0 meters")
    }

    @Test
    fun `calculateDistanceMeters with standard wheel returns correct distance`() {
        val result = DistanceCalculator.calculateDistanceMeters(
            revolutions = 100,
            wheelCircumferenceMm = 2100.0
        )
        assertEquals(210.0, result, 0.001, "100 revolutions with 2100mm wheel should be 210 meters")
    }

    @Test
    fun `calculateDistanceMeters with single revolution`() {
        val result = DistanceCalculator.calculateDistanceMeters(
            revolutions = 1,
            wheelCircumferenceMm = 2100.0
        )
        assertEquals(2.1, result, 0.001, "1 revolution with 2100mm wheel should be 2.1 meters")
    }

    @Test
    fun `calculateDistanceMeters with small wheel`() {
        val result = DistanceCalculator.calculateDistanceMeters(
            revolutions = 100,
            wheelCircumferenceMm = 1000.0
        )
        assertEquals(100.0, result, 0.001, "100 revolutions with 1000mm wheel should be 100 meters")
    }

    @Test
    fun `calculateDistanceMeters with large wheel`() {
        val result = DistanceCalculator.calculateDistanceMeters(
            revolutions = 100,
            wheelCircumferenceMm = 3000.0
        )
        assertEquals(300.0, result, 0.001, "100 revolutions with 3000mm wheel should be 300 meters")
    }

    @Test
    fun `calculateDistanceMeters with large number of revolutions`() {
        val result = DistanceCalculator.calculateDistanceMeters(
            revolutions = 100_000,
            wheelCircumferenceMm = 2100.0
        )
        assertEquals(210_000.0, result, 0.1, "Large number of revolutions should calculate correctly")
    }

    @Test
    fun `calculateDistanceMeters with very large number of revolutions`() {

        val result = DistanceCalculator.calculateDistanceMeters(
            revolutions = 1_000_000,
            wheelCircumferenceMm = 2100.0
        )
        assertEquals(2_100_000.0, result, 1.0, "Very large number of revolutions should not overflow")
    }

    @Test
    fun `calculateDistanceMeters with fractional circumference`() {
        val result = DistanceCalculator.calculateDistanceMeters(
            revolutions = 10,
            wheelCircumferenceMm = 2105.5
        )
        assertEquals(21.055, result, 0.001, "Fractional circumference should calculate correctly")
    }


    @Test
    fun `calculateDistanceMetersValidated with valid circumference returns success`() {
        val result = DistanceCalculator.calculateDistanceMetersValidated(
            revolutions = 100,
            wheelCircumferenceMm = 2100.0
        )
        assertTrue(result.isSuccess, "Valid circumference should return success")
        assertEquals(210.0, result.getOrNull()!!, 0.001)
    }

    @Test
    fun `calculateDistanceMetersValidated with too small circumference returns failure`() {
        val result = DistanceCalculator.calculateDistanceMetersValidated(
            revolutions = 100,
            wheelCircumferenceMm = 500.0
        )
        assertTrue(result.isFailure, "Too small circumference should return failure")
    }

    @Test
    fun `calculateDistanceMetersValidated with too large circumference returns failure`() {
        val result = DistanceCalculator.calculateDistanceMetersValidated(
            revolutions = 100,
            wheelCircumferenceMm = 4000.0
        )
        assertTrue(result.isFailure, "Too large circumference should return failure")
    }

    @Test
    fun `calculateDistanceMetersValidated with min circumference returns success`() {
        val result = DistanceCalculator.calculateDistanceMetersValidated(
            revolutions = 100,
            wheelCircumferenceMm = DistanceCalculator.MIN_WHEEL_CIRCUMFERENCE_MM
        )
        assertTrue(result.isSuccess, "Min circumference should be valid")
    }

    @Test
    fun `calculateDistanceMetersValidated with max circumference returns success`() {
        val result = DistanceCalculator.calculateDistanceMetersValidated(
            revolutions = 100,
            wheelCircumferenceMm = DistanceCalculator.MAX_WHEEL_CIRCUMFERENCE_MM
        )
        assertTrue(result.isSuccess, "Max circumference should be valid")
    }

    @Test
    fun `isValidWheelCircumference returns true for valid values`() {
        assertTrue(DistanceCalculator.isValidWheelCircumference(2100.0))
        assertTrue(DistanceCalculator.isValidWheelCircumference(1000.0))
        assertTrue(DistanceCalculator.isValidWheelCircumference(3500.0))
    }

    @Test
    fun `isValidWheelCircumference returns false for invalid values`() {
        assertFalse(DistanceCalculator.isValidWheelCircumference(999.9))
        assertFalse(DistanceCalculator.isValidWheelCircumference(3500.1))
        assertFalse(DistanceCalculator.isValidWheelCircumference(0.0))
        assertFalse(DistanceCalculator.isValidWheelCircumference(-100.0))
    }
}

