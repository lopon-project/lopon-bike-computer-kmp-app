package ru.lopon.core.metrics

import kotlin.test.Test
import kotlin.test.assertEquals

class SpeedCalculatorTest {

    @Test
    fun `smoothing averages recent values`() {
        val calc = SpeedCalculator(smoothingWindowSize = 3)
        calc.addSpeed(10.0)
        calc.addSpeed(12.0)
        val result = calc.addSpeed(14.0)

        // (10+12+14)/3 = 12.0
        assertEquals(12.0, result.current, 0.01)
    }

    @Test
    fun `max speed tracked correctly`() {
        val calc = SpeedCalculator()
        calc.addSpeed(5.0)
        calc.addSpeed(15.0)
        val result = calc.addSpeed(10.0)

        assertEquals(15.0, result.max, 0.01)
    }

    @Test
    fun `average speed calculated across all values`() {
        val calc = SpeedCalculator(smoothingWindowSize = 3)
        calc.addSpeed(10.0)
        calc.addSpeed(20.0)
        val result = calc.addSpeed(30.0)

        // (10+20+30)/3 = 20.0
        assertEquals(20.0, result.average, 0.01)
    }

    @Test
    fun `window slides correctly`() {
        val calc = SpeedCalculator(smoothingWindowSize = 2)
        calc.addSpeed(10.0) // current = 10.0
        calc.addSpeed(20.0) // current = (10+20)/2 = 15.0
        val result = calc.addSpeed(30.0) // current = (20+30)/2 = 25.0

        assertEquals(25.0, result.current, 0.01)
    }

    @Test
    fun `reset clears all data`() {
        val calc = SpeedCalculator()
        calc.addSpeed(100.0)
        calc.addSpeed(200.0)
        calc.reset()

        val result = calc.addSpeed(5.0)
        assertEquals(5.0, result.current, 0.01)
        assertEquals(5.0, result.average, 0.01)
        assertEquals(5.0, result.max, 0.01)
    }

    @Test
    fun `zero speed is handled correctly`() {
        val calc = SpeedCalculator(smoothingWindowSize = 3)
        calc.addSpeed(0.0)
        calc.addSpeed(0.0)
        val result = calc.addSpeed(0.0)

        assertEquals(0.0, result.current, 0.01)
        assertEquals(0.0, result.average, 0.01)
        assertEquals(0.0, result.max, 0.01)
    }

    @Test
    fun `single value returns same for current and average`() {
        val calc = SpeedCalculator()
        val result = calc.addSpeed(10.0)

        assertEquals(10.0, result.current, 0.01)
        assertEquals(10.0, result.average, 0.01)
        assertEquals(10.0, result.max, 0.01)
    }

    @Test
    fun `max speed decreases after reset`() {
        val calc = SpeedCalculator()
        calc.addSpeed(100.0)
        assertEquals(100.0, calc.currentMaxSpeed, 0.01)

        calc.reset()
        calc.addSpeed(10.0)
        assertEquals(10.0, calc.currentMaxSpeed, 0.01)
    }

    @Test
    fun `average through accessor property matches returned value`() {
        val calc = SpeedCalculator()
        calc.addSpeed(10.0)
        calc.addSpeed(20.0)
        val result = calc.addSpeed(30.0)

        assertEquals(result.average, calc.currentAverageSpeed, 0.01)
    }

    @Test
    fun `smoothing with window size 1 returns raw value`() {
        val calc = SpeedCalculator(smoothingWindowSize = 1)
        calc.addSpeed(10.0)
        calc.addSpeed(20.0)
        val result = calc.addSpeed(30.0)

        assertEquals(30.0, result.current, 0.01)
    }

    @Test
    fun `large speed values handled correctly`() {
        val calc = SpeedCalculator()
        val result = calc.addSpeed(1000.0) // ~3600 km/h

        assertEquals(1000.0, result.current, 0.01)
        assertEquals(1000.0, result.max, 0.01)
    }
}

