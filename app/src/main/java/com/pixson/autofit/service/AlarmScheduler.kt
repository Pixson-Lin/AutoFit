package com.pixson.autofit.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import java.util.UUID

class AlarmScheduler(
    private val context: Context,
    private val canScheduleExact: () -> Boolean,
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() },
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java),
) {

    fun scheduleBackstop(experimentId: UUID, delayMs: Long = ServiceConstants.TICK_INTERVAL_MS) {
        val triggerAtElapsed = elapsedRealtime() + delayMs
        val triggerAtWallClock = System.currentTimeMillis() + delayMs
        val pendingIntent = backstopPendingIntent(experimentId)

        if (canScheduleExact()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtWallClock,
                    pendingIntent,
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtElapsed,
                    pendingIntent,
                )
            }
            ServiceEventLogger.alarmScheduled(experimentId, exact = true, delayMs = delayMs)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtWallClock,
                    pendingIntent,
                )
            } else {
                alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtElapsed,
                    pendingIntent,
                )
            }
            ServiceEventLogger.alarmScheduled(experimentId, exact = false, delayMs = delayMs)
        }
    }

    fun cancel(experimentId: UUID) {
        alarmManager.cancel(backstopPendingIntent(experimentId))
        ServiceEventLogger.alarmCancelled(experimentId)
    }

    private fun backstopPendingIntent(experimentId: UUID): PendingIntent {
        val intent = Intent(context, ExperimentAlarmReceiver::class.java).apply {
            action = ServiceConstants.ACTION_ALARM_BACKSTOP
            putExtra(ServiceConstants.EXTRA_EXPERIMENT_ID, experimentId.toString())
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(experimentId),
            intent,
            flags,
        )
    }

    companion object {
        fun requestCodeFor(experimentId: UUID): Int =
            ServiceConstants.ALARM_REQUEST_CODE_BASE + experimentId.hashCode()
    }
}
