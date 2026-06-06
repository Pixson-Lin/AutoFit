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

data class TickSnapshot(
    val generatedSteps: Int,
    val tickIndex: Int,
    val totalWrittenSteps: Int,
    val remainingMinutes: Int,
)

class ExperimentLoopRunner(
    private val repository: ExperimentRepository,
    private val healthWriteCoordinator: HealthWriteCoordinator,
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
        onTick: suspend (TickSnapshot) -> Unit = {},
    ): LoopExitReason {
        val experiment = repository.getExperiment(experimentId) ?: return LoopExitReason.NOT_FOUND
        if (experiment.status != ExperimentStatus.RUNNING) return LoopExitReason.NOT_RUNNING

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
            try {
                val steps = stepGenerator.generate(
                    targetCadence = experiment.targetCadence,
                    randomRange = experiment.randomRange,
                )
                healthWriteCoordinator.recordGeneratedSteps(experimentId, steps)

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

                val totalWrittenSteps = healthWriteCoordinator.totalWrittenSteps(experimentId)
                val remainingMinutes = remainingMinutes(experimentId)
                onTick(
                    TickSnapshot(
                        generatedSteps = steps,
                        tickIndex = tickIndex,
                        totalWrittenSteps = totalWrittenSteps,
                        remainingMinutes = remainingMinutes,
                    ),
                )
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

        return when {
            isStopped() -> LoopExitReason.STOPPED_EARLY
            !shouldContinue(experimentId) -> {
                ServiceEventLogger.durationReached(experimentId)
                LoopExitReason.COMPLETED
            }
            else -> LoopExitReason.STOPPED_EARLY
        }
    }

    private suspend fun remainingMinutes(experimentId: UUID): Int {
        val experiment = repository.getExperiment(experimentId) ?: return 0
        val elapsedMinutes = Duration.between(experiment.startTime, currentInstant()).toMinutes()
        return (experiment.durationMinutes - elapsedMinutes).toInt().coerceAtLeast(0)
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
