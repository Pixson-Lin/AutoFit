package com.pixson.autofit.service

import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.ResultAggregator
import com.pixson.autofit.domain.model.ExperimentStatus
import java.time.Instant
import java.util.UUID

class ExperimentFinalizer(
    private val repository: ExperimentRepository,
    private val resultAggregator: ResultAggregator,
    private val healthWriteCoordinator: HealthWriteCoordinator,
) {

    suspend fun finalize(
        experimentId: UUID,
        terminalStatus: ExperimentStatus,
        endTime: Instant,
    ) {
        val experiment = repository.getExperiment(experimentId) ?: return
        if (experiment.status != ExperimentStatus.RUNNING) {
            return
        }

        healthWriteCoordinator.flushPending(experimentId)
        healthWriteCoordinator.clear(experimentId)

        val heartbeats = repository.getHeartbeats(experimentId)
        val writeEvents = repository.getHealthWriteEvents(experimentId)
        val result = resultAggregator.aggregate(
            experimentId = experimentId,
            heartbeats = heartbeats,
            writeEvents = writeEvents,
            startTime = experiment.startTime,
            endTime = endTime,
        )

        repository.upsertResult(result)
        repository.updateExperimentStatus(experimentId, terminalStatus)
        ServiceEventLogger.finalized(experimentId, terminalStatus, result.totalSteps)
    }
}
