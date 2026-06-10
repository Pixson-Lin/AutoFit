package com.pixson.autofit.ui

import com.pixson.autofit.domain.model.ExperimentConfig
import com.pixson.autofit.domain.model.ExperimentStatus
import java.util.UUID

data class ConfigUiState(
    val targetCadenceInput: String = "120",
    val randomRangeInput: String = "15",
    val durationMinutesInput: String = "5",
    val batchMinutes: Int = ExperimentConfig.DEFAULT_BATCH_MINUTES,
    val healthConnectStatus: String = "",
    val healthConnectReady: Boolean = false,
    val canStart: Boolean = false,
    val validationMessage: String? = null,
    val isStarting: Boolean = false,
    val errorMessage: String? = null,
)

data class RunningUiState(
    val experimentId: UUID,
    val status: ExperimentStatus,
    val elapsedMinutes: Int,
    val elapsedSecondsRemainder: Int,
    val remainingMinutes: Int,
    val totalStepsWritten: Int,
    val generatedSteps: Int,
    val writeSuccessCount: Int,
    val writeFailureCount: Int,
    val tickIndex: Int,
    val isActive: Boolean,
)

sealed class StartExperimentOutcome {
    data class Started(val experimentId: UUID) : StartExperimentOutcome()
    data class NeedsActivityRecognition(val experimentId: UUID) : StartExperimentOutcome()
    data class Blocked(val message: String) : StartExperimentOutcome()
}
