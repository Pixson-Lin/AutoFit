package com.pixson.autofit.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class ExperimentAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ServiceConstants.ACTION_ALARM_BACKSTOP) return
        val experimentId = intent.getStringExtra(ServiceConstants.EXTRA_EXPERIMENT_ID) ?: return

        ServiceEventLogger.alarmFired(experimentId)
        ContextCompat.startForegroundService(
            context,
            ExperimentForegroundService.alarmBackstopIntent(context, experimentId),
        )
    }
}
