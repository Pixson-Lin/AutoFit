package com.pixson.autofit.service

import com.pixson.autofit.data.health.HealthConnectManager
import com.pixson.autofit.data.health.StepRecordEntry
import com.pixson.autofit.data.health.WriteResult
import com.pixson.autofit.data.local.entity.HealthWriteEventEntity
import com.pixson.autofit.data.repo.ExperimentRepository
import java.time.Instant
import java.util.UUID

/**
 * Buffers per-minute generated steps and flushes them to Health Connect retrospectively
 * (only minutes that have already elapsed) in batches. Each batch is written with a single
 * [HealthConnectManager.writeStepsBatch] call (one StepsRecord per minute, no summation) to
 * minimise IPC, and persists one [HealthWriteEventEntity] per minute sharing the batch result.
 */
class HealthWriteCoordinator(
    private val healthConnectManager: HealthConnectManager,
    private val repository: ExperimentRepository,
    private val defaultBatchMinutes: Int = ServiceConstants.WRITE_BATCH_TICK_COUNT,
    private val currentInstant: () -> Instant,
    private val idSupplier: () -> UUID = { UUID.randomUUID() },
) {

    private val pendingBatches = mutableMapOf<UUID, PendingBatch>()

    suspend fun recordGeneratedSteps(
        experimentId: UUID,
        steps: Int,
        tickIndex: Int,
        experimentStartTime: Instant,
        batchMinutes: Int = defaultBatchMinutes,
    ): List<HealthWriteEventEntity> {
        val batch = pendingBatches.getOrPut(experimentId) {
            PendingBatch(experimentStartTime = experimentStartTime)
        }
        batch.experimentStartTime = experimentStartTime
        require(tickIndex > 0) { "tickIndex must be positive for step records" }
        batch.entries += BufferedMinute(tickIndex = tickIndex, steps = steps)

        return if (tickIndex % batchMinutes.coerceAtLeast(1) == 0) {
            flushPending(experimentId)
        } else {
            emptyList()
        }
    }

    suspend fun flushPending(experimentId: UUID): List<HealthWriteEventEntity> {
        val batch = pendingBatches[experimentId] ?: return emptyList()
        val startTime = batch.experimentStartTime
        if (batch.entries.isEmpty() || startTime == null) {
            pendingBatches.remove(experimentId)
            return emptyList()
        }

        val windows = batch.entries.map { minute ->
            val (start, end) = StepRecordWindow.forTickRange(
                experimentStartTime = startTime,
                startTickIndex = minute.tickIndex,
                endTickIndex = minute.tickIndex,
            )
            StepRecordEntry(stepCount = minute.steps, startTime = start, endTime = end)
        }

        val writeResult = healthConnectManager.writeStepsBatch(windows)
        val success = writeResult is WriteResult.Success
        val errorMessage = (writeResult as? WriteResult.Failure)?.reason.orEmpty()
        val writtenAt = currentInstant()

        val events = windows.map { window ->
            HealthWriteEventEntity(
                id = idSupplier(),
                experimentId = experimentId,
                timestamp = writtenAt,
                recordStart = window.startTime,
                recordEnd = window.endTime,
                stepCount = window.stepCount,
                success = success,
                errorMessage = errorMessage,
            )
        }
        repository.insertHealthWriteEvents(events)
        pendingBatches.remove(experimentId)

        ServiceEventLogger.healthWriteRecorded(
            experimentId = experimentId,
            stepCount = windows.sumOf { it.stepCount },
            success = success,
            errorMessage = errorMessage,
        )
        return events
    }

    fun pendingStepCount(experimentId: UUID): Int =
        pendingBatches[experimentId]?.entries?.sumOf { it.steps } ?: 0

    suspend fun totalWrittenSteps(experimentId: UUID): Int {
        val flushed = repository.getHealthWriteEvents(experimentId)
            .filter { it.success }
            .sumOf { it.stepCount }
        return flushed + pendingStepCount(experimentId)
    }

    fun clear(experimentId: UUID) {
        pendingBatches.remove(experimentId)
    }

    private data class BufferedMinute(
        val tickIndex: Int,
        val steps: Int,
    )

    private data class PendingBatch(
        val entries: MutableList<BufferedMinute> = mutableListOf(),
        var experimentStartTime: Instant? = null,
    )
}
