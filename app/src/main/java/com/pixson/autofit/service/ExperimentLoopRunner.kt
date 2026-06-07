package com.pixson.autofit.service

import com.pixson.autofit.data.local.entity.HeartbeatEntity
import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.StepGenerator
import com.pixson.autofit.domain.model.ExperimentStatus
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

        var nextTickIndex = repository.getHeartbeats(experimentId).size
        if (nextTickIndex > 0) {
            ServiceEventLogger.recovered(experimentId, nextTickIndex)
        }

        val stepGenerator = StepGenerator(seedFor(experimentId))

        while (!isStopped() && nextTickIndex <= experiment.durationMinutes) {
            if (nextTickIndex >= 1) {
                delayUntilTick(experiment.startTime, nextTickIndex)
            }
            if (isStopped()) break

            wakeLockGateway.acquire()
            try {
                val steps = if (nextTickIndex == 0) {
                    0
                } else {
                    stepGenerator.generate(
                        targetCadence = experiment.targetCadence,
                        randomRange = experiment.randomRange,
                    ).also { generated ->
                        healthWriteCoordinator.recordGeneratedSteps(
                            experimentId = experimentId,
                            steps = generated,
                            tickIndex = nextTickIndex,
                            experimentStartTime = experiment.startTime,
                            batchMinutes = experiment.batchMinutes,
                        )
                    }
                }

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
                ServiceEventLogger.heartbeatRecorded(experimentId, steps, nextTickIndex)

                val totalWrittenSteps = healthWriteCoordinator.totalWrittenSteps(experimentId)
                onTick(
                    TickSnapshot(
                        generatedSteps = steps,
                        tickIndex = nextTickIndex,
                        totalWrittenSteps = totalWrittenSteps,
                        remainingMinutes = remainingMinutes(
                            durationMinutes = experiment.durationMinutes,
                            tickIndex = nextTickIndex,
                        ),
                    ),
                )
            } finally {
                wakeLockGateway.release()
            }

            nextTickIndex++
        }

        return when {
            isStopped() -> LoopExitReason.STOPPED_EARLY
            nextTickIndex > experiment.durationMinutes -> {
                ServiceEventLogger.durationReached(experimentId)
                LoopExitReason.COMPLETED
            }
            else -> LoopExitReason.STOPPED_EARLY
        }
    }

    private suspend fun delayUntilTick(experimentStartTime: Instant, tickIndex: Int) {
        val target = experimentStartTime.plus(tickIndex * tickIntervalMs, ChronoUnit.MILLIS)
        val delayMs = Duration.between(currentInstant(), target).toMillis()
        if (delayMs > 0) {
            delayFn(delayMs)
        }
    }

    private fun remainingMinutes(durationMinutes: Int, tickIndex: Int): Int =
        (durationMinutes - tickIndex).coerceAtLeast(0)

    companion object {
        fun seedFor(experimentId: UUID): Long =
            experimentId.mostSignificantBits xor experimentId.leastSignificantBits
    }
}
