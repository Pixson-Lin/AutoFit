package com.pixson.autofit.service

import com.pixson.autofit.data.local.entity.HeartbeatEntity
import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.StepGenerator
import com.pixson.autofit.domain.model.ExperimentStatus
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class ExperimentLoopRunner(
    private val repository: ExperimentRepository,
    private val deviceStateSource: DeviceStateSource,
    private val wakeLockGateway: WakeLockGateway,
    private val tickIntervalMs: Long = ServiceConstants.TICK_INTERVAL_MS,
    private val elapsedRealtime: () -> Long,
    private val currentInstant: () -> Instant,
    private val delayFn: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
) {

    suspend fun run(
        experimentId: UUID,
        isStopped: () -> Boolean,
        onTick: suspend (generatedSteps: Int, tickIndex: Int) -> Unit = { _, _ -> },
    ) {
        val experiment = repository.getExperiment(experimentId) ?: return
        if (experiment.status != ExperimentStatus.RUNNING) return

        val priorHeartbeats = repository.getHeartbeats(experimentId).size
        if (priorHeartbeats > 0) {
            ServiceEventLogger.recovered(experimentId, priorHeartbeats)
        }

        val stepGenerator = StepGenerator(seedFor(experimentId))
        val sessionStartElapsed = elapsedRealtime()
        var ticksThisSession = 0

        while (shouldContinue(experimentId) && !isStopped()) {
            ticksThisSession++
            val tickIndex = priorHeartbeats + ticksThisSession

            wakeLockGateway.acquire()
            val generatedSteps = try {
                val steps = stepGenerator.generate(
                    targetCadence = experiment.targetCadence,
                    randomRange = experiment.randomRange,
                )
                val deviceState = deviceStateSource.read()
                repository.insertHeartbeat(
                    HeartbeatEntity(
                        id = UUID.randomUUID(),
                        experimentId = experimentId,
                        timestamp = currentInstant(),
                        generatedSteps = steps,
                        batteryLevel = deviceState.batteryLevel,
                        screenOn = deviceState.screenOn,
                        charging = deviceState.charging,
                    ),
                )
                ServiceEventLogger.heartbeatRecorded(experimentId, steps, tickIndex)
                onTick(steps, tickIndex)
                steps
            } finally {
                wakeLockGateway.release()
            }

            if (!shouldContinue(experimentId)) break

            val nextTickElapsed = sessionStartElapsed + ticksThisSession * tickIntervalMs
            val delayMs = nextTickElapsed - elapsedRealtime()
            if (delayMs > 0) {
                delayFn(delayMs)
            }
        }

        if (!isStopped() && !shouldContinue(experimentId)) {
            ServiceEventLogger.durationReached(experimentId)
            repository.updateExperimentStatus(experimentId, ExperimentStatus.COMPLETED)
        }
    }

    private suspend fun shouldContinue(experimentId: UUID): Boolean {
        val experiment = repository.getExperiment(experimentId) ?: return false
        if (experiment.status != ExperimentStatus.RUNNING) return false
        val elapsedMinutes = Duration.between(experiment.startTime, currentInstant()).toMinutes()
        return elapsedMinutes < experiment.durationMinutes
    }

    companion object {
        fun seedFor(experimentId: UUID): Long =
            experimentId.mostSignificantBits xor experimentId.leastSignificantBits
    }
}
