package com.pixson.autofit.ui.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixson.autofit.R
import com.pixson.autofit.domain.model.ExperimentConfig
import com.pixson.autofit.ui.ConfigUiState

@Composable
fun ConfigScreen(
    state: ConfigUiState,
    onTargetCadenceChange: (String) -> Unit,
    onRandomRangeChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onBatchMinutesChange: (Int) -> Unit,
    onRequestHealthPermissions: () -> Unit,
    onRequestActivityRecognition: () -> Unit,
    onRefreshHealthStatus: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.config_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.config_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.targetCadenceInput,
            onValueChange = onTargetCadenceChange,
            label = { Text(stringResource(R.string.config_cadence_label)) },
            supportingText = { Text(stringResource(R.string.config_cadence_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.randomRangeInput,
            onValueChange = onRandomRangeChange,
            label = { Text(stringResource(R.string.config_range_label)) },
            supportingText = { Text(stringResource(R.string.config_range_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.durationMinutesInput,
            onValueChange = onDurationChange,
            label = { Text(stringResource(R.string.config_duration_label)) },
            supportingText = { Text(stringResource(R.string.config_duration_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(R.string.config_batch_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExperimentConfig.BATCH_MINUTE_OPTIONS.forEach { option ->
                val selected = option == state.batchMinutes
                Button(
                    onClick = { onBatchMinutesChange(option) },
                    modifier = Modifier.weight(1f),
                    colors = if (selected) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                ) {
                    Text(option.toString())
                }
            }
        }

        Text(
            text = state.healthConnectStatus,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onRefreshHealthStatus,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.config_refresh_hc))
            }
            Button(
                onClick = onRequestHealthPermissions,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.config_request_hc))
            }
        }

        Button(
            onClick = onRequestActivityRecognition,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.config_request_activity))
        }

        state.validationMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        state.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = onStart,
            enabled = state.canStart && !state.isStarting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isStarting) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(stringResource(R.string.config_start))
        }
    }
}
