package com.pixson.autofit.domain

import android.content.Context
import androidx.core.content.ContextCompat
import com.pixson.autofit.data.env.EnvironmentInspector
import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.model.ExperimentConfig
import com.pixson.autofit.domain.model.ExperimentStatus
import com.pixson.autofit.service.ExperimentForegroundService
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class ExperimentController(
    private val appContext: Context,
    private val repository: ExperimentRepository,
    private val environmentInspector: EnvironmentInspector,
) {

    suspend fun createExperiment(config: ExperimentConfig): UUID {
        val experimentId = UUID.randomUUID()
        val experiment = ExperimentEntity(
            id = experimentId,
            startTime = Instant.now().truncatedTo(ChronoUnit.MILLIS),
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

    suspend fun createAndStartExperiment(config: ExperimentConfig): UUID {
        val experimentId = createExperiment(config)
        startExperiment(experimentId)
        return experimentId
    }

    fun startExperiment(experimentId: UUID) {
        ContextCompat.startForegroundService(
            appContext,
            ExperimentForegroundService.startIntent(appContext, experimentId),
        )
    }

    fun stopExperiment(experimentId: UUID) {
        appContext.startService(
            ExperimentForegroundService.stopIntent(appContext, experimentId),
        )
    }
}
