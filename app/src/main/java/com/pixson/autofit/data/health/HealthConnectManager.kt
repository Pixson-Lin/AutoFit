package com.pixson.autofit.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import java.time.Instant
import java.time.ZoneOffset

sealed class HealthSdkStatus {
    data object Available : HealthSdkStatus()
    data class Unavailable(val reason: String, val rawStatus: Int) : HealthSdkStatus()
}

sealed class WriteResult {
    data object Success : WriteResult()
    data class Failure(val reason: String) : WriteResult()
}

data class StepRecordEntry(
    val stepCount: Int,
    val startTime: Instant,
    val endTime: Instant,
)

class HealthConnectManager(
    private val gateway: HealthConnectGateway,
) {

    val writeStepsPermission: String =
        HealthPermission.getWritePermission(StepsRecord::class)

    fun getSdkStatus(): HealthSdkStatus {
        val status = gateway.getSdkStatus()
        return when (status) {
            HealthConnectClient.SDK_AVAILABLE -> HealthSdkStatus.Available
            HealthConnectClient.SDK_UNAVAILABLE -> HealthSdkStatus.Unavailable(
                reason = "Health Connect is not available on this device",
                rawStatus = status,
            )
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthSdkStatus.Unavailable(
                    reason = "Health Connect update required",
                    rawStatus = status,
                )
            else -> HealthSdkStatus.Unavailable(
                reason = "Unknown Health Connect SDK status: $status",
                rawStatus = status,
            )
        }
    }

    suspend fun hasWritePermission(): Boolean {
        if (getSdkStatus() !is HealthSdkStatus.Available) return false
        return gateway.getGrantedPermissions().contains(writeStepsPermission)
    }

    suspend fun writeSteps(
        stepCount: Int,
        startTime: Instant,
        endTime: Instant,
    ): WriteResult {
        if (stepCount < 0) {
            return WriteResult.Failure("stepCount must be non-negative")
        }

        when (val status = getSdkStatus()) {
            is HealthSdkStatus.Available -> Unit
            is HealthSdkStatus.Unavailable -> return WriteResult.Failure(status.reason)
        }

        if (!hasWritePermission()) {
            return WriteResult.Failure("WRITE_STEPS permission not granted")
        }

        if (!endTime.isAfter(startTime)) {
            return WriteResult.Failure("endTime must be after startTime")
        }

        return writeStepsBatch(listOf(StepRecordEntry(stepCount, startTime, endTime)))
    }

    /**
     * Retrospective batch write: one StepsRecord per entry (per minute, no summation),
     * flushed in a single insertRecords call to minimise IPC (FR-004).
     */
    suspend fun writeStepsBatch(entries: List<StepRecordEntry>): WriteResult {
        if (entries.isEmpty()) return WriteResult.Success

        if (entries.any { it.stepCount < 0 }) {
            return WriteResult.Failure("stepCount must be non-negative")
        }
        if (entries.any { !it.endTime.isAfter(it.startTime) }) {
            return WriteResult.Failure("endTime must be after startTime")
        }

        when (val status = getSdkStatus()) {
            is HealthSdkStatus.Available -> Unit
            is HealthSdkStatus.Unavailable -> return WriteResult.Failure(status.reason)
        }

        if (!hasWritePermission()) {
            return WriteResult.Failure("WRITE_STEPS permission not granted")
        }

        return try {
            val records = entries.map { entry ->
                StepsRecord(
                    count = entry.stepCount.toLong(),
                    startTime = entry.startTime,
                    endTime = entry.endTime,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                )
            }
            gateway.insertRecords(records)
            WriteResult.Success
        } catch (exception: Exception) {
            WriteResult.Failure(exception.message ?: exception.javaClass.simpleName)
        }
    }
}
