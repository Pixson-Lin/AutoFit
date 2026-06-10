package com.pixson.autofit.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixson.autofit.data.repo.ExperimentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HistoryViewModel(
    private val repository: ExperimentRepository,
) : ViewModel() {

    val listState: StateFlow<List<HistoryListItemUiState>> = combine(
        repository.observeAllExperiments(),
        repository.observeAllResults(),
        HistoryMapper::mapList,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _detailState = MutableStateFlow<HistoryDetailUiState?>(null)
    val detailState: StateFlow<HistoryDetailUiState?> = _detailState

    private var detailJob: Job? = null

    fun loadDetail(experimentId: UUID) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            combine(
                repository.observeExperiment(experimentId),
                repository.observeResult(experimentId),
                repository.observeEnvironmentSnapshot(experimentId),
            ) { experiment, result, environment ->
                experiment?.let {
                    HistoryMapper.toDetail(
                        experiment = it,
                        result = result,
                        environment = environment,
                    )
                }
            }.collect { detail ->
                _detailState.value = detail
            }
        }
    }

    fun clearDetail() {
        detailJob?.cancel()
        detailJob = null
        _detailState.value = null
    }
}
