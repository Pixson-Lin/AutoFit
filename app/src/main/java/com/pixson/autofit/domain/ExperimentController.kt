package com.pixson.autofit.domain

import com.pixson.autofit.data.env.EnvironmentInspector
import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.model.ExperimentConfig
import com.pixson.autofit.domain.model.ExperimentStatus
import java.time.Instant
import java.util.UUID

class ExperimentController(
    private val repository: ExperimentRepository,
    private val environmentInspector: EnvironmentInspector,
) {

    suspend fun createExperiment(config: ExperimentConfig): UUID {
        val experimentId = UUID.randomUUID()
        val experiment = ExperimentEntity(
            id = experimentId,
            startTime = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
            durationMinutes = config.durationMinutes,
            targetCadence = config.targetCadence,
            randomRange = config.randomRange,
            status = ExperimentStatus.RUNNING,
        )
        repository.insertExperiment(experiment)
        repository.insertEnvironmentSnapshot(
            environmentInspector.capture(experimentId),
        )
        return experimentId
    }
}
