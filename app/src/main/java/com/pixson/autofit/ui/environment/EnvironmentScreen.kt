package com.pixson.autofit.ui.environment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixson.autofit.R

@Composable
fun EnvironmentScreen(
    state: EnvironmentUiState,
    onRefresh: () -> Unit,
    onFix: (EnvironmentFixAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.environment_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = state.deviceSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onRefresh,
                enabled = !state.isRefreshing,
                modifier = Modifier.weight(1f),
            ) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(stringResource(R.string.environment_refresh))
            }
        }

        state.items.forEach { item ->
            EnvironmentChecklistCard(
                item = item,
                onFix = { item.fixAction?.let(onFix) },
            )
        }
    }
}

@Composable
private fun EnvironmentChecklistCard(
    item: EnvironmentChecklistItem,
    onFix: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isOk) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = item.statusText,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (item.fixAction != null && !item.isOk) {
                Button(
                    onClick = onFix,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.environment_fix))
                }
            }
        }
    }
}
