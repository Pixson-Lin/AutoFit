package com.pixson.autofit.domain

import com.pixson.autofit.data.local.entity.HealthWriteEventEntity
import com.pixson.autofit.data.local.entity.HeartbeatEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ResultAggregatorTest {

    private val aggregator = ResultAggregator()
    private val experimentId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val startTime = Instant.parse("2026-06-06T10:00:00Z")
    private val endTime = Instant.parse("2026-06-06T11:05:00Z")

    @Test
    fun `empty data returns zero counts`() {
        val result = aggregator.aggregate(
            experimentId = experimentId,
            heartbeats = emptyList(),
            writeEvents = emptyList(),
            startTime = startTime,
            endTime = endTime,
        )

        assertEquals(experimentId, result.experimentId)
        assertEquals(0, result.totalSteps)
        assertEquals(0, result.heartbeatCount)
        assertEquals(0, result.writeSuccessCount)
        assertEquals(0, result.writeFailureCount)
        assertEquals(65, result.actualDuration)
    }

    @Test
    fun `aggregates heartbeats and successful writes`() {
        val heartbeats = listOf(
            heartbeat(generatedSteps = 110),
            heartbeat(generatedSteps = 125),
        )
        val writeEvents = listOf(
            writeEvent(stepCount = 110, success = true),
            writeEvent(stepCount = 125, success = true),
            writeEvent(stepCount = 0, success = false, error = "permission denied"),
        )

        val result = aggregator.aggregate(
            experimentId = experimentId,
            heartbeats = heartbeats,
            writeEvents = writeEvents,
            startTime = startTime,
            endTime = endTime,
        )

        assertEquals(2, result.heartbeatCount)
        assertEquals(235, result.totalSteps)
        assertEquals(2, result.writeSuccessCount)
        assertEquals(1, result.writeFailureCount)
    }

    @Test
    fun `failed writes do not contribute to total steps`() {
        val writeEvents = listOf(
            writeEvent(stepCount = 50, success = false, error = "unavailable"),
            writeEvent(stepCount = 80, success = false, error = "timeout"),
        )

        val result = aggregator.aggregate(
            experimentId = experimentId,
            heartbeats = emptyList(),
            writeEvents = writeEvents,
            startTime = startTime,
            endTime = Instant.parse("2026-06-06T10:30:00Z"),
        )

        assertEquals(0, result.totalSteps)
        assertEquals(0, result.writeSuccessCount)
        assertEquals(2, result.writeFailureCount)
        assertEquals(30, result.actualDuration)
    }

    private fun heartbeat(generatedSteps: Int) = HeartbeatEntity(
        id = UUID.randomUUID(),
        experimentId = experimentId,
        timestamp = Instant.parse("2026-06-06T10:01:00Z"),
        generatedSteps = generatedSteps,
        batteryLevel = 80,
        screenOn = true,
        charging = false,
    )

    private fun writeEvent(
        stepCount: Int,
        success: Boolean,
        error: String = "",
    ) = HealthWriteEventEntity(
        id = UUID.randomUUID(),
        experimentId = experimentId,
        timestamp = Instant.parse("2026-06-06T10:01:00Z"),
        stepCount = stepCount,
        success = success,
        errorMessage = error,
    )
}
