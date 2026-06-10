package com.pixson.autofit.ui.history

import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.data.local.entity.ExperimentResultEntity
import com.pixson.autofit.domain.model.ExperimentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.util.UUID

class HistoryMapperTest {

    private val experimentId = UUID.fromString("00000000-0000-0000-0000-000000000060")
    private val startTime = Instant.parse("2026-06-07T10:00:00Z")

    @Test
    fun `computeSuccessRate returns ratio of successful writes`() {
        val result = ExperimentResultEntity(
            experimentId = experimentId,
            totalSteps = 240,
            heartbeatCount = 3,
            writeSuccessCount = 2,
            writeFailureCount = 1,
            actualDuration = 2,
        )

        assertEquals(2f / 3f, HistoryMapper.computeSuccessRate(result)!!, 0.001f)
        assertEquals("66%", HistoryMapper.formatSuccessRate(result))
    }

    @Test
    fun `computeSuccessRate returns null when no writes`() {
        val result = ExperimentResultEntity(
            experimentId = experimentId,
            totalSteps = 0,
            heartbeatCount = 1,
            writeSuccessCount = 0,
            writeFailureCount = 0,
            actualDuration = 0,
        )

        assertNull(HistoryMapper.computeSuccessRate(result))
        assertEquals("—", HistoryMapper.formatSuccessRate(result))
    }

    @Test
    fun `mapList joins experiments with results`() {
        val experiment = ExperimentEntity(
            id = experimentId,
            startTime = startTime,
            durationMinutes = 3,
            targetCadence = 120,
            randomRange = 15,
            batchMinutes = 1,
            status = ExperimentStatus.COMPLETED,
        )
        val result = ExperimentResultEntity(
            experimentId = experimentId,
            totalSteps = 300,
            heartbeatCount = 4,
            writeSuccessCount = 3,
            writeFailureCount = 1,
            actualDuration = 3,
        )

        val items = HistoryMapper.mapList(listOf(experiment), listOf(result))

        assertEquals(1, items.size)
        assertEquals("300", items.first().totalStepsLabel)
        assertEquals("75%", items.first().successRateLabel)
    }

    @Test
    fun `toListItem maps experiment and result fields`() {
        val experiment = ExperimentEntity(
            id = experimentId,
            startTime = startTime,
            durationMinutes = 5,
            targetCadence = 120,
            randomRange = 15,
            batchMinutes = 1,
            status = ExperimentStatus.COMPLETED,
        )
        val result = ExperimentResultEntity(
            experimentId = experimentId,
            totalSteps = 360,
            heartbeatCount = 6,
            writeSuccessCount = 5,
            writeFailureCount = 0,
            actualDuration = 5,
        )

        val item = HistoryMapper.toListItem(experiment, result)

        assertEquals("360", item.totalStepsLabel)
        assertEquals("100%", item.successRateLabel)
        assertEquals("5 min", item.durationLabel)
        assertEquals(false, item.isRunning)
    }
}
