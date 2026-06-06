package com.pixson.autofit.service

import android.util.Log
import java.util.UUID

object ServiceEventLogger {

    private const val TAG = "AutoFit/Service"

    fun started(experimentId: UUID) {
        Log.i(TAG, "Service started experimentId=$experimentId")
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
}
