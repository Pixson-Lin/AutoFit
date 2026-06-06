package com.pixson.autofit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepGeneratorTest {

    @Test
    fun `zero random range returns target cadence`() {
        val generator = StepGenerator(seed = 42L)
        repeat(10) {
            assertEquals(120, generator.generate(targetCadence = 120, randomRange = 0))
        }
    }

    @Test
    fun `output is always non-negative`() {
        val generator = StepGenerator(seed = 99L)
        repeat(100) {
            val steps = generator.generate(targetCadence = 5, randomRange = 50)
            assertTrue(steps >= 0)
        }
    }

    @Test
    fun `same seed produces same sequence`() {
        val first = List(20) { StepGenerator(seed = 12345L).generate(120, 15) }
        val second = List(20) { StepGenerator(seed = 12345L).generate(120, 15) }
        assertEquals(first, second)
    }

    @Test
    fun `different seeds can produce different sequences`() {
        val first = List(10) { StepGenerator(seed = 1L).generate(120, 15) }
        val second = List(10) { StepGenerator(seed = 2L).generate(120, 15) }
        assertTrue(first != second)
    }

    @Test
    fun `large random range stays within bounds`() {
        val generator = StepGenerator(seed = 7L)
        repeat(200) {
            val steps = generator.generate(targetCadence = 100, randomRange = 30)
            assertTrue(steps in 70..130)
        }
    }

    @Test
    fun `zero target cadence with offset still non-negative`() {
        val generator = StepGenerator(seed = 11L)
        repeat(50) {
            assertTrue(generator.generate(targetCadence = 0, randomRange = 10) >= 0)
        }
    }
}
