package com.pixson.autofit.system

import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.model.ExperimentStatus
import com.pixson.autofit.service.ExperimentFinalizer
import com.pixson.autofit.service.ServiceEventLogger
import java.time.Instant

class BootInterruptionHandler(
    private val repository: ExperimentRepository,
    private val experimentFinalizer: ExperimentFinalizer,
    private val currentInstant: () -> Instant = { Instant.now() },
    private val staleHeartbeatThresholdMs: Long = STALE_HEARTBEAT_THRESHOLD_MS,
) {

    /**
     * Marks a [ExperimentStatus.RUNNING] experiment as [ExperimentStatus.INTERRUPTED_BY_REBOOT]
     * when the device reboots and the foreground service cannot resume automatically.
     */
    suspend fun handleBootCompleted() {
        val running = repository.getRunningExperiment() ?: return
        val heartbeats = repository.getHeartbeats(running.id)
        val lastHeartbeat = heartbeats.lastOrNull()

        if (lastHeartbeat != null) {
            val gapMs = currentInstant().toEpochMilli() - lastHeartbeat.timestamp.toEpochMilli()
            if (gapMs < staleHeartbeatThresholdMs) {
                ServiceEventLogger.rebootSkipped(running.id, "recent_heartbeat")
                return
            }
        }

        val endTime = lastHeartbeat?.timestamp ?: running.startTime
        experimentFinalizer.finalize(
            experimentId = running.id,
            terminalStatus = ExperimentStatus.INTERRUPTED_BY_REBOOT,
            endTime = endTime,
            flushPending = false,
        )
        ServiceEventLogger.rebootInterrupted(
            experimentId = running.id,
            lastHeartbeatAt = lastHeartbeat?.timestamp,
            rebootGapMinutes = java.time.Duration.between(endTime, currentInstant()).toMinutes().toInt(),
        )
    }

    companion object {
        /** Heartbeats newer than this are treated as an active service (no reboot finalize). */
        const val STALE_HEARTBEAT_THRESHOLD_MS = 120_000L
    }
}
