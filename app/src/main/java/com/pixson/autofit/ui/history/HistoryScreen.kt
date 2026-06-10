package com.pixson.autofit.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixson.autofit.R

@Composable
fun HistoryScreen(
    items: List<HistoryListItemUiState>,
    onItemClick: (HistoryListItemUiState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.experimentId }) { item ->
                HistoryListCard(
                    item = item,
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

@Composable
private fun HistoryListCard(
    item: HistoryListItemUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRunning) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.history_item_status, item.status.name),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(R.string.history_item_start, item.startTimeLabel),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (item.endTimeLabel != null) {
                Text(
                    text = stringResource(R.string.history_item_end, item.endTimeLabel),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(R.string.history_item_duration, item.durationLabel),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(
                    R.string.history_item_steps_rate,
                    item.totalStepsLabel,
                    item.successRateLabel,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            if (item.isRunning) {
                Text(
                    text = stringResource(R.string.history_item_running_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
