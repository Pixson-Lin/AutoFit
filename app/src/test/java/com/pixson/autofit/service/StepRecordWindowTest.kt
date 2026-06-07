package com.pixson.autofit.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class StepRecordWindowTest {

    private val experimentStart = Instant.parse("2026-06-06T10:00:00Z")

    @Test
    fun `single tick uses one minute non overlapping window`() {
        val (start, end) = StepRecordWindow.forTickRange(
            experimentStartTime = experimentStart,
            startTickIndex = 1,
            endTickIndex = 1,
        )

        assertEquals(experimentStart, start)
        assertEquals(Instant.parse("2026-06-06T10:01:00Z"), end)
        assertTrue(end.isAfter(start))
    }

    @Test
    fun `batched ticks span contiguous windows`() {
        val (start, end) = StepRecordWindow.forTickRange(
            experimentStartTime = experimentStart,
            startTickIndex = 2,
            endTickIndex = 4,
        )

        assertEquals(Instant.parse("2026-06-06T10:01:00Z"), start)
        assertEquals(Instant.parse("2026-06-06T10:04:00Z"), end)
    }
}
