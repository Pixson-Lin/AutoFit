package com.pixson.autofit.ui.history

import com.pixson.autofit.data.local.entity.EnvironmentSnapshotEntity
import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.data.local.entity.ExperimentResultEntity
import com.pixson.autofit.domain.model.ExperimentStatus
import com.pixson.autofit.domain.model.PermissionGrantState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

object HistoryMapper {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withLocale(Locale.getDefault())

    fun mapList(
        experiments: List<ExperimentEntity>,
        results: List<ExperimentResultEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<HistoryListItemUiState> {
        val resultById = results.associateBy { it.experimentId }
        return experiments.map { experiment ->
            toListItem(
                experiment = experiment,
                result = resultById[experiment.id],
                zoneId = zoneId,
            )
        }
    }

    fun toListItem(
        experiment: ExperimentEntity,
        result: ExperimentResultEntity?,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): HistoryListItemUiState {
        val endInstant = result?.let {
            experiment.startTime.plusSeconds(it.actualDuration * 60L)
        }
        val totalWrites = result?.let { it.writeSuccessCount + it.writeFailureCount } ?: 0

        return HistoryListItemUiState(
            experimentId = experiment.id,
            startTimeLabel = formatInstant(experiment.startTime, zoneId),
            endTimeLabel = endInstant?.let { formatInstant(it, zoneId) },
            durationLabel = result?.actualDuration?.let { "$it min" }
                ?: "${experiment.durationMinutes} min (planned)",
            totalStepsLabel = result?.totalSteps?.toString() ?: "—",
            successRateLabel = result?.let { formatSuccessRate(it) } ?: "—",
            status = experiment.status,
            isRunning = experiment.status == ExperimentStatus.RUNNING,
        )
    }

    fun toDetail(
        experiment: ExperimentEntity,
        result: ExperimentResultEntity?,
        environment: EnvironmentSnapshotEntity?,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): HistoryDetailUiState {
        val endInstant = result?.let {
            experiment.startTime.plusSeconds(it.actualDuration * 60L)
        }

        return HistoryDetailUiState(
            experimentId = experiment.id,
            status = experiment.status,
            startTimeLabel = formatInstant(experiment.startTime, zoneId),
            endTimeLabel = endInstant?.let { formatInstant(it, zoneId) },
            configuredDurationMinutes = experiment.durationMinutes,
            actualDurationMinutes = result?.actualDuration,
            targetCadence = experiment.targetCadence,
            randomRange = experiment.randomRange,
            batchMinutes = experiment.batchMinutes,
            totalSteps = result?.totalSteps,
            heartbeatCount = result?.heartbeatCount,
            writeSuccessCount = result?.writeSuccessCount,
            writeFailureCount = result?.writeFailureCount,
            successRateLabel = result?.let { formatSuccessRate(it) },
            deviceModel = environment?.deviceModel,
            manufacturer = environment?.manufacturer,
            androidVersion = environment?.androidVersion,
            batteryOptimization = environment?.batteryOptimization,
            powerSaveMode = environment?.powerSaveMode,
            charging = environment?.charging,
            batteryLevel = environment?.batteryLevel,
            notificationPermissionLabel = environment?.notificationPermission?.let(::permissionLabel),
            healthConnectPermissionLabel = environment?.healthConnectPermission?.let(::permissionLabel),
        )
    }

    fun computeSuccessRate(result: ExperimentResultEntity): Float? {
        val total = result.writeSuccessCount + result.writeFailureCount
        if (total == 0) return null
        return result.writeSuccessCount.toFloat() / total.toFloat()
    }

    fun formatSuccessRate(result: ExperimentResultEntity): String {
        val rate = computeSuccessRate(result)
        return if (rate == null) {
            "—"
        } else {
            "${(rate * 100).toInt()}%"
        }
    }

    fun formatInstant(instant: Instant, zoneId: ZoneId): String =
        formatter.format(instant.atZone(zoneId))

    fun permissionLabel(state: Int): String = when (state) {
        PermissionGrantState.GRANTED -> "Granted"
        PermissionGrantState.DENIED -> "Denied"
        PermissionGrantState.NOT_REQUIRED -> "Not required"
        PermissionGrantState.NOT_APPLICABLE -> "Not applicable"
        else -> "Unknown"
    }
}
