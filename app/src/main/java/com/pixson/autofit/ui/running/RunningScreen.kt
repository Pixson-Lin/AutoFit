package com.pixson.autofit.ui.running

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixson.autofit.R
import com.pixson.autofit.domain.model.ExperimentStatus
import com.pixson.autofit.ui.RunningUiState

@Composable
fun RunningScreen(
    state: RunningUiState?,
    onStop: () -> Unit,
    onBackToConfig: () -> Unit,
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
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.running_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        Text(
            text = stringResource(R.string.running_status, state.status.name),
            style = MaterialTheme.typography.titleMedium,
        )

        MetricRow(
            label = stringResource(R.string.running_elapsed),
            value = stringResource(
                R.string.running_elapsed_value,
                state.elapsedMinutes,
                state.elapsedSecondsRemainder,
            ),
        )
        MetricRow(
            label = stringResource(R.string.running_remaining),
            value = stringResource(R.string.running_minutes_value, state.remainingMinutes),
        )
        MetricRow(
            label = stringResource(R.string.running_tick),
            value = state.tickIndex.toString(),
        )
        MetricRow(
            label = stringResource(R.string.running_steps_written),
            value = state.totalStepsWritten.toString(),
        )
        MetricRow(
            label = stringResource(R.string.running_steps_generated),
            value = state.generatedSteps.toString(),
        )
        MetricRow(
            label = stringResource(R.string.running_writes),
            value = stringResource(
                R.string.running_writes_value,
                state.writeSuccessCount,
                state.writeFailureCount,
            ),
        )

        Text(
            text = stringResource(R.string.running_experiment_id, state.experimentId),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.isActive) {
            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.running_stop))
            }
        } else {
            Text(
                text = when (state.status) {
                    ExperimentStatus.COMPLETED -> stringResource(R.string.running_finished_completed)
                    ExperimentStatus.STOPPED -> stringResource(R.string.running_finished_stopped)
                    else -> stringResource(R.string.running_finished_other, state.status.name)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onBackToConfig,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.running_new_experiment))
            }
        }
    }
}

@Composable
private fun MetricRow(
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
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
