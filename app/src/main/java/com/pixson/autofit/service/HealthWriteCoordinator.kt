package com.pixson.autofit.service

import com.pixson.autofit.data.health.HealthConnectManager
import com.pixson.autofit.data.health.WriteResult
import com.pixson.autofit.data.local.entity.HealthWriteEventEntity
import com.pixson.autofit.data.repo.ExperimentRepository
import java.time.Instant
import java.util.UUID

class HealthWriteCoordinator(
    private val healthConnectManager: HealthConnectManager,
    private val repository: ExperimentRepository,
    private val batchTickCount: Int = ServiceConstants.WRITE_BATCH_TICK_COUNT,
    private val currentInstant: () -> Instant,
    private val idSupplier: () -> UUID = { UUID.randomUUID() },
) {

    private val pendingBatches = mutableMapOf<UUID, PendingBatch>()

    suspend fun recordGeneratedSteps(
        experimentId: UUID,
        steps: Int,
    ): HealthWriteEventEntity? {
        val batch = pendingBatches.getOrPut(experimentId) { PendingBatch() }
        if (batch.ticksInBatch == 0) {
            batch.batchStartTime = currentInstant()
        }
        batch.accumulatedSteps += steps
        batch.ticksInBatch++

        return if (batch.ticksInBatch >= batchTickCount) {
            flushPending(experimentId)
        } else {
            null
        }
    }

    suspend fun flushPending(experimentId: UUID): HealthWriteEventEntity? {
        val batch = pendingBatches[experimentId] ?: return null
        if (batch.ticksInBatch == 0 || batch.batchStartTime == null) {
            return null
        }

        val startTime = batch.batchStartTime!!
        val endTime = currentInstant()
        val stepCount = batch.accumulatedSteps

        val writeResult = healthConnectManager.writeSteps(
            stepCount = stepCount,
            startTime = startTime,
            endTime = endTime,
        )

        val event = HealthWriteEventEntity(
            id = idSupplier(),
            experimentId = experimentId,
            timestamp = endTime,
            stepCount = stepCount,
            success = writeResult is WriteResult.Success,
            errorMessage = (writeResult as? WriteResult.Failure)?.reason.orEmpty(),
        )
        repository.insertHealthWriteEvent(event)
        pendingBatches.remove(experimentId)

        ServiceEventLogger.healthWriteRecorded(
            experimentId = experimentId,
            stepCount = stepCount,
            success = event.success,
            errorMessage = event.errorMessage,
        )
        return event
    }

    fun pendingStepCount(experimentId: UUID): Int =
        pendingBatches[experimentId]?.accumulatedSteps ?: 0

    suspend fun totalWrittenSteps(experimentId: UUID): Int {
        val flushed = repository.getHealthWriteEvents(experimentId)
            .filter { it.success }
            .sumOf { it.stepCount }
        return flushed + pendingStepCount(experimentId)
    }

    fun clear(experimentId: UUID) {
        pendingBatches.remove(experimentId)
    }

    private data class PendingBatch(
        var accumulatedSteps: Int = 0,
        var ticksInBatch: Int = 0,
        var batchStartTime: Instant? = null,
    )
}
