package com.pixson.autofit.domain

import com.pixson.autofit.data.local.entity.ExperimentResultEntity
import com.pixson.autofit.data.local.entity.HealthWriteEventEntity
import com.pixson.autofit.data.local.entity.HeartbeatEntity
import java.time.Duration
import java.time.Instant
import java.util.UUID

class ResultAggregator {

    fun aggregate(
        experimentId: UUID,
        heartbeats: List<HeartbeatEntity>,
        writeEvents: List<HealthWriteEventEntity>,
        startTime: Instant,
        endTime: Instant,
    ): ExperimentResultEntity {
        val totalSteps = writeEvents
            .filter { it.success }
            .sumOf { it.stepCount }

        val writeSuccessCount = writeEvents.count { it.success }
        val writeFailureCount = writeEvents.count { !it.success }
        val actualDurationMinutes = Duration.between(startTime, endTime)
            .toMinutes()
            .toInt()
            .coerceAtLeast(0)

        return ExperimentResultEntity(
            experimentId = experimentId,
            totalSteps = totalSteps,
            heartbeatCount = heartbeats.size,
            writeSuccessCount = writeSuccessCount,
            writeFailureCount = writeFailureCount,
            actualDuration = actualDurationMinutes,
        )
    }
}
