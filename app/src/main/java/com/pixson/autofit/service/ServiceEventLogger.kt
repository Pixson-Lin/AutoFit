package com.pixson.autofit.service

import android.util.Log
import java.util.UUID

object ServiceEventLogger {

    private const val TAG = "AutoFit/Service"

    fun started(experimentId: UUID) {
        Log.i(TAG, "Service started experimentId=$experimentId")
    }

    fun startBlocked(experimentId: UUID, reason: String) {
        Log.w(TAG, "Service start blocked experimentId=$experimentId reason=$reason")
    }

    fun stopped(experimentId: UUID?, reason: String) {
        Log.i(TAG, "Service stopped experimentId=$experimentId reason=$reason")
    }

    fun destroyed(experimentId: UUID?) {
        Log.i(TAG, "Service destroyed experimentId=$experimentId")
    }

    fun taskRemoved(experimentId: UUID?) {
        Log.i(TAG, "Task removed experimentId=$experimentId")
    }

    fun recovered(experimentId: UUID, heartbeatCount: Int) {
        Log.i(TAG, "Service recovered experimentId=$experimentId priorHeartbeats=$heartbeatCount")
    }

    fun heartbeatRecorded(experimentId: UUID, generatedSteps: Int, tickIndex: Int) {
        Log.d(TAG, "Heartbeat experimentId=$experimentId steps=$generatedSteps tick=$tickIndex")
    }

    fun durationReached(experimentId: UUID) {
        Log.i(TAG, "Duration reached experimentId=$experimentId")
    }

    fun healthWriteRecorded(
        experimentId: UUID,
        stepCount: Int,
        success: Boolean,
        errorMessage: String,
    ) {
        Log.i(
            TAG,
            "Health write experimentId=$experimentId steps=$stepCount success=$success error=$errorMessage",
        )
    }

    fun finalized(experimentId: UUID, status: com.pixson.autofit.domain.model.ExperimentStatus, totalSteps: Int) {
        Log.i(TAG, "Finalized experimentId=$experimentId status=$status totalSteps=$totalSteps")
    }

    fun alarmScheduled(experimentId: UUID, exact: Boolean, delayMs: Long) {
        Log.d(TAG, "Alarm scheduled experimentId=$experimentId exact=$exact delayMs=$delayMs")
    }

    fun alarmCancelled(experimentId: UUID) {
        Log.d(TAG, "Alarm cancelled experimentId=$experimentId")
    }

    fun alarmFired(experimentId: String) {
        Log.i(TAG, "Alarm fired experimentId=$experimentId")
    }
}
