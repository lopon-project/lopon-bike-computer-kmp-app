package ru.lopon.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class WheelCircumferenceValidatorTest {

    @Test
    fun `valid circumference returns Valid`() {
        val result = WheelCircumferenceValidator.validate(2100.0)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `minimum valid circumference returns Valid`() {
        val result = WheelCircumferenceValidator.validate(1000.0)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `maximum valid circumference returns Valid`() {
        val result = WheelCircumferenceValidator.validate(3500.0)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `too small returns TooSmall`() {
        val result = WheelCircumferenceValidator.validate(500.0)
        assertTrue(result is ValidationResult.TooSmall)
        assertEquals(1000.0, (result as ValidationResult.TooSmall).minValue)
    }

    @Test
    fun `too large returns TooLarge`() {
        val result = WheelCircumferenceValidator.validate(4000.0)
        assertTrue(result is ValidationResult.TooLarge)
        assertEquals(3500.0, (result as ValidationResult.TooLarge).maxValue)
    }

    @Test
    fun `isValid returns true for valid value`() {
        assertTrue(WheelCircumferenceValidator.isValid(2100.0))
        assertTrue(WheelCircumferenceValidator.isValid(1000.0))
        assertTrue(WheelCircumferenceValidator.isValid(3500.0))
    }

    @Test
    fun `isValid returns false for invalid value`() {
        assertFalse(WheelCircumferenceValidator.isValid(500.0))
        assertFalse(WheelCircumferenceValidator.isValid(4000.0))
    }

    @Test
    fun `commonSizes returns non-empty list`() {
        val sizes = WheelCircumferenceValidator.commonSizes()
        assertTrue(sizes.isNotEmpty())
        assertTrue(sizes.size > 10)
    }

    @Test
    fun `commonSizes contains 700x25c`() {
        val sizes = WheelCircumferenceValidator.commonSizes()
        val found = sizes.find { it.name == "700x25c" }
        assertTrue(found != null)
        assertEquals(2105.0, found.circumferenceMm)
    }

    @Test
    fun `findClosestSize returns correct size`() {
        val closest = WheelCircumferenceValidator.findClosestSize(2100.0)
        assertTrue(closest != null)
        assertTrue(kotlin.math.abs(closest.circumferenceMm - 2100.0) < 10)
    }

    @Test
    fun `findClosestSize returns null for invalid circumference`() {
        val closest = WheelCircumferenceValidator.findClosestSize(500.0)
        assertEquals(closest, null)
    }
}
