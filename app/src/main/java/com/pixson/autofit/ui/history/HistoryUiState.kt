package com.pixson.autofit.ui.history

import com.pixson.autofit.domain.model.ExperimentStatus
import java.util.UUID

data class HistoryListItemUiState(
    val experimentId: UUID,
    val startTimeLabel: String,
    val endTimeLabel: String?,
    val durationLabel: String,
    val totalStepsLabel: String,
    val successRateLabel: String,
    val status: ExperimentStatus,
    val isRunning: Boolean,
)

data class HistoryDetailUiState(
    val experimentId: UUID,
    val status: ExperimentStatus,
    val startTimeLabel: String,
    val endTimeLabel: String?,
    val configuredDurationMinutes: Int,
    val actualDurationMinutes: Int?,
    val targetCadence: Int,
    val randomRange: Int,
    val batchMinutes: Int,
    val totalSteps: Int?,
    val heartbeatCount: Int?,
    val writeSuccessCount: Int?,
    val writeFailureCount: Int?,
    val successRateLabel: String?,
    val deviceModel: String?,
    val manufacturer: String?,
    val androidVersion: String?,
    val batteryOptimization: Boolean?,
    val powerSaveMode: Boolean?,
    val charging: Boolean?,
    val batteryLevel: Int?,
    val notificationPermissionLabel: String?,
    val healthConnectPermissionLabel: String?,
)
