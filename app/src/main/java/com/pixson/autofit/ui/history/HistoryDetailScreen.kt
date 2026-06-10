package com.pixson.autofit.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixson.autofit.R
import com.pixson.autofit.domain.model.ExperimentStatus

@Composable
fun HistoryDetailScreen(
    state: HistoryDetailUiState?,
    onOpenRunning: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state == null) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.history_detail_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        DetailRow(stringResource(R.string.history_detail_status), state.status.name)
        DetailRow(stringResource(R.string.history_detail_start), state.startTimeLabel)
        state.endTimeLabel?.let {
            DetailRow(stringResource(R.string.history_detail_end), it)
        }
        DetailRow(
            stringResource(R.string.history_detail_configured_duration),
            "${state.configuredDurationMinutes} min",
        )
        state.actualDurationMinutes?.let {
            DetailRow(stringResource(R.string.history_detail_actual_duration), "$it min")
        }
        DetailRow(stringResource(R.string.history_detail_cadence), "${state.targetCadence} SPM")
        DetailRow(stringResource(R.string.history_detail_range), "±${state.randomRange}")
        DetailRow(stringResource(R.string.history_detail_batch), "${state.batchMinutes} min")

        Text(
            text = stringResource(R.string.history_detail_results_header),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        state.totalSteps?.let {
            DetailRow(stringResource(R.string.history_detail_total_steps), it.toString())
        }
        state.heartbeatCount?.let {
            DetailRow(stringResource(R.string.history_detail_heartbeats), it.toString())
        }
        if (state.writeSuccessCount != null && state.writeFailureCount != null) {
            DetailRow(
                stringResource(R.string.history_detail_writes),
                "${state.writeSuccessCount} / ${state.writeFailureCount}",
            )
        }
        state.successRateLabel?.let {
            DetailRow(stringResource(R.string.history_detail_success_rate), it)
        }

        if (state.deviceModel != null) {
            Text(
                text = stringResource(R.string.history_detail_environment_header),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            DetailRow(stringResource(R.string.history_detail_device), state.deviceModel)
            state.manufacturer?.let {
                DetailRow(stringResource(R.string.history_detail_manufacturer), it)
            }
            state.androidVersion?.let {
                DetailRow(stringResource(R.string.history_detail_android), it)
            }
            state.batteryLevel?.takeIf { it >= 0 }?.let {
                DetailRow(stringResource(R.string.history_detail_battery_level), "$it%")
            }
            state.charging?.let {
                DetailRow(
                    stringResource(R.string.history_detail_charging),
                    if (it) "Yes" else "No",
                )
            }
            state.batteryOptimization?.let {
                DetailRow(
                    stringResource(R.string.history_detail_battery_opt),
                    if (it) "Ignored" else "Restricted",
                )
            }
            state.powerSaveMode?.let {
                DetailRow(
                    stringResource(R.string.history_detail_power_save),
                    if (it) "On" else "Off",
                )
            }
            state.notificationPermissionLabel?.let {
                DetailRow(stringResource(R.string.history_detail_notification_perm), it)
            }
            state.healthConnectPermissionLabel?.let {
                DetailRow(stringResource(R.string.history_detail_hc_perm), it)
            }
        }

        if (state.status == ExperimentStatus.RUNNING) {
            Button(
                onClick = onOpenRunning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.history_detail_open_running))
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.history_detail_back))
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
