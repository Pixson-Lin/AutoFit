package com.pixson.autofit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixson.autofit.data.health.HealthConnectManager
import com.pixson.autofit.data.health.HealthSdkStatus
import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.data.local.entity.HealthWriteEventEntity
import com.pixson.autofit.data.local.entity.HeartbeatEntity
import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.ExperimentController
import com.pixson.autofit.domain.model.ExperimentConfig
import com.pixson.autofit.domain.model.ExperimentStatus
import com.pixson.autofit.system.PermissionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.UUID

class ExperimentViewModel(
    private val repository: ExperimentRepository,
    private val experimentController: ExperimentController,
    private val healthConnectManager: HealthConnectManager,
    private val permissionManager: PermissionManager,
    private val currentInstant: () -> Instant = { Instant.now() },
) : ViewModel() {

    private val _configState = MutableStateFlow(ConfigUiState())
    val configState: StateFlow<ConfigUiState> = _configState.asStateFlow()

    private val _runningState = MutableStateFlow<RunningUiState?>(null)
    val runningState: StateFlow<RunningUiState?> = _runningState.asStateFlow()

    private val _activeExperimentId = MutableStateFlow<UUID?>(null)
    val activeExperimentId: StateFlow<UUID?> = _activeExperimentId.asStateFlow()

    private var observationJob: Job? = null

    init {
        viewModelScope.launch {
            refreshHealthConnectStatus()
            resumeRunningExperimentIfAny()
        }
    }

    fun updateTargetCadence(value: String) {
        _configState.update { it.copy(targetCadenceInput = value, errorMessage = null) }
        revalidateConfig()
    }

    fun updateRandomRange(value: String) {
        _configState.update { it.copy(randomRangeInput = value, errorMessage = null) }
        revalidateConfig()
    }

    fun updateDurationMinutes(value: String) {
        _configState.update { it.copy(durationMinutesInput = value, errorMessage = null) }
        revalidateConfig()
    }

    fun updateBatchMinutes(value: Int) {
        if (value !in ExperimentConfig.BATCH_MINUTE_OPTIONS) return
        _configState.update { it.copy(batchMinutes = value, errorMessage = null) }
        revalidateConfig()
    }

    fun clearError() {
        _configState.update { it.copy(errorMessage = null) }
    }

    suspend fun refreshHealthConnectStatus() {
        val (statusText, ready) = when (val status = healthConnectManager.getSdkStatus()) {
            is HealthSdkStatus.Available -> {
                val granted = healthConnectManager.hasWritePermission()
                val text = if (granted) {
                    "Health Connect ready (WRITE_STEPS granted)"
                } else {
                    "Health Connect available — grant WRITE_STEPS to start"
                }
                text to granted
            }
            is HealthSdkStatus.Unavailable -> status.reason to false
        }
        _configState.update {
            it.copy(
                healthConnectStatus = statusText,
                healthConnectReady = ready,
            )
        }
        revalidateConfig()
    }

    suspend fun onStartClicked(): StartExperimentOutcome {
        val state = _configState.value
        if (!state.canStart || state.isStarting) {
            val message = state.validationMessage
                ?: if (!state.healthConnectReady) "Health Connect is not ready"
                else state.errorMessage
                ?: "Cannot start experiment"
            _configState.update { it.copy(errorMessage = message) }
            return StartExperimentOutcome.Blocked(message)
        }

        val config = ConfigValidation.validatedConfig(
            targetCadenceInput = state.targetCadenceInput,
            randomRangeInput = state.randomRangeInput,
            durationMinutesInput = state.durationMinutesInput,
            batchMinutes = state.batchMinutes,
        ) ?: return StartExperimentOutcome.Blocked("Invalid experiment configuration")

        _configState.update { it.copy(isStarting = true, errorMessage = null) }
        return try {
            val experimentId = experimentController.createExperiment(config)
            if (!permissionManager.canStartHealthForegroundService()) {
                _activeExperimentId.value = experimentId
                startObserving(experimentId)
                StartExperimentOutcome.NeedsActivityRecognition(experimentId)
            } else {
                experimentController.startExperiment(experimentId)
                _activeExperimentId.value = experimentId
                startObserving(experimentId)
                StartExperimentOutcome.Started(experimentId)
            }
        } finally {
            _configState.update { it.copy(isStarting = false) }
        }
    }

    fun onActivityRecognitionGranted(experimentId: UUID) {
        experimentController.startExperiment(experimentId)
        _activeExperimentId.value = experimentId
        startObserving(experimentId)
    }

    fun onActivityRecognitionDenied() {
        _configState.update {
            it.copy(errorMessage = "Activity recognition is required for the health foreground service.")
        }
    }

    fun stopExperiment() {
        val experimentId = _activeExperimentId.value ?: return
        experimentController.stopExperiment(experimentId)
    }

    fun openRunningSession(experimentId: UUID) {
        _activeExperimentId.value = experimentId
        startObserving(experimentId)
    }

    fun dismissRunningSession() {
        observationJob?.cancel()
        observationJob = null
        _runningState.value = null
        _activeExperimentId.value = null
    }

    private suspend fun resumeRunningExperimentIfAny() {
        val running = repository.getRunningExperiment() ?: return
        _activeExperimentId.value = running.id
        startObserving(running.id)
    }

    private fun startObserving(experimentId: UUID) {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            val ticker = flow {
                while (isActive) {
                    emit(Unit)
                    delay(TICKER_INTERVAL_MS)
                }
            }
            combine(
                ticker,
                repository.observeExperiment(experimentId),
                repository.observeHeartbeats(experimentId),
                repository.observeHealthWriteEvents(experimentId),
            ) { _, experiment, heartbeats, writeEvents ->
                buildRunningState(experiment, heartbeats, writeEvents)
            }.collect { state ->
                _runningState.value = state
                if (state != null && !state.isActive) {
                    _activeExperimentId.value = null
                }
            }
        }
    }

    private fun buildRunningState(
        experiment: ExperimentEntity?,
        heartbeats: List<HeartbeatEntity>,
        writeEvents: List<HealthWriteEventEntity>,
    ): RunningUiState? {
        if (experiment == null) return null

        val now = currentInstant()
        val elapsed = Duration.between(experiment.startTime, now).coerceAtLeast(Duration.ZERO)
        val elapsedMinutes = elapsed.toMinutes().toInt()
        val elapsedSecondsRemainder = (elapsed.seconds % 60).toInt()
        val remainingMinutes = (experiment.durationMinutes - elapsedMinutes).coerceAtLeast(0)

        val writeSuccessCount = writeEvents.count { it.success }
        val writeFailureCount = writeEvents.count { !it.success }
        val totalStepsWritten = writeEvents.filter { it.success }.sumOf { it.stepCount }
        val generatedSteps = heartbeats.sumOf { it.generatedSteps }
        val tickIndex = (heartbeats.size - 1).coerceAtLeast(0)
        val isActive = experiment.status == ExperimentStatus.RUNNING

        return RunningUiState(
            experimentId = experiment.id,
            status = experiment.status,
            elapsedMinutes = elapsedMinutes,
            elapsedSecondsRemainder = elapsedSecondsRemainder,
            remainingMinutes = remainingMinutes,
            totalStepsWritten = totalStepsWritten,
            generatedSteps = generatedSteps,
            writeSuccessCount = writeSuccessCount,
            writeFailureCount = writeFailureCount,
            tickIndex = tickIndex,
            isActive = isActive,
        )
    }

    private fun revalidateConfig() {
        _configState.update { state ->
            val validationMessage = ConfigValidation.validationHint(
                targetCadenceInput = state.targetCadenceInput,
                randomRangeInput = state.randomRangeInput,
                durationMinutesInput = state.durationMinutesInput,
            )
            state.copy(
                validationMessage = validationMessage,
                canStart = validationMessage == null && state.healthConnectReady,
            )
        }
    }

    companion object {
        private const val TICKER_INTERVAL_MS = 1_000L
    }
}
