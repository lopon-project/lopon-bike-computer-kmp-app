package ru.lopon.core.metrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class CadenceCalculatorTest {

    @Test
    fun `adds cadence and returns smoothed current`() {
        val calc = CadenceCalculator(smoothingWindowSize = 3)
        calc.addCadence(80.0)
        calc.addCadence(90.0)
        val result = calc.addCadence(100.0)

        assertEquals(90.0, result.current!!, 0.01)
    }

    @Test
    fun `average calculated across all values`() {
        val calc = CadenceCalculator(smoothingWindowSize = 2)
        calc.addCadence(60.0)
        calc.addCadence(80.0)
        val result = calc.addCadence(100.0)

        assertEquals(80.0, result.average!!, 0.01)
    }

    @Test
    fun `null cadence returns last known value`() {
        val calc = CadenceCalculator(smoothingWindowSize = 3)
        calc.addCadence(80.0)
        calc.addCadence(90.0)
        val result = calc.addCadence(null)

        assertEquals(90.0, result.current!!, 0.01)
    }

    @Test
    fun `null cadence preserves average`() {
        val calc = CadenceCalculator()
        calc.addCadence(80.0)
        calc.addCadence(100.0)
        val result = calc.addCadence(null)

        assertEquals(90.0, result.average!!, 0.01)
    }

    @Test
    fun `initial null returns null current and average`() {
        val calc = CadenceCalculator()
        val result = calc.addCadence(null)

        assertNull(result.current)
        assertNull(result.average)
    }

    @Test
    fun `reset clears all data`() {
        val calc = CadenceCalculator()
        calc.addCadence(80.0)
        calc.addCadence(100.0)
        calc.reset()

        val result = calc.addCadence(null)
        assertNull(result.current)
        assertNull(result.average)
    }

    @Test
    fun `reset followed by value resets statistics`() {
        val calc = CadenceCalculator()
        calc.addCadence(80.0)
        calc.addCadence(100.0)
        calc.reset()

        val result = calc.addCadence(60.0)
        assertEquals(60.0, result.current!!, 0.01)
        assertEquals(60.0, result.average!!, 0.01)
    }

    @Test
    fun `window slides correctly`() {
        val calc = CadenceCalculator(smoothingWindowSize = 2)
        calc.addCadence(60.0)
        calc.addCadence(80.0)
        val result = calc.addCadence(100.0)

        assertEquals(90.0, result.current!!, 0.01)
    }

    @Test
    fun `hasData returns false initially`() {
        val calc = CadenceCalculator()
        assertFalse(calc.hasData)
    }

    @Test
    fun `hasData returns true after adding value`() {
        val calc = CadenceCalculator()
        calc.addCadence(80.0)
        assertTrue(calc.hasData)
    }

    @Test
    fun `hasData returns false after reset`() {
        val calc = CadenceCalculator()
        calc.addCadence(80.0)
        calc.reset()
        assertFalse(calc.hasData)
    }

    @Test
    fun `multiple null values do not affect statistics`() {
        val calc = CadenceCalculator()
        calc.addCadence(80.0)
        calc.addCadence(null)
        calc.addCadence(null)
        calc.addCadence(null)

        val result = calc.addCadence(100.0)
        assertEquals(90.0, result.average!!, 0.01)
    }

    @Test
    fun `zero cadence is valid value`() {
        val calc = CadenceCalculator()
        val result = calc.addCadence(0.0)

        assertEquals(0.0, result.current!!, 0.01)
        assertEquals(0.0, result.average!!, 0.01)
    }

    @Test
    fun `high cadence values handled correctly`() {
        val calc = CadenceCalculator()
        val result = calc.addCadence(200.0)

        assertEquals(200.0, result.current!!, 0.01)
    }
}

