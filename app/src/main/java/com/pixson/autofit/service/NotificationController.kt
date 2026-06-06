package com.pixson.autofit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pixson.autofit.R
import java.util.UUID

data class RunningNotificationSnapshot(
    val experimentId: UUID,
    val totalSteps: Int,
    val remainingMinutes: Int,
    val tickIndex: Int,
)

class NotificationController(
    private val context: Context,
    private val throttleMs: Long = ServiceConstants.NOTIFICATION_THROTTLE_MS,
    private val elapsedRealtime: () -> Long,
) {

    private var lastUpdateElapsedMs: Long? = null

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            ServiceConstants.NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun shouldUpdate(force: Boolean = false): Boolean {
        if (force) return true
        val lastUpdate = lastUpdateElapsedMs ?: return true
        return elapsedRealtime() - lastUpdate >= throttleMs
    }

    fun markUpdated() {
        lastUpdateElapsedMs = elapsedRealtime()
    }

    fun resetThrottle() {
        lastUpdateElapsedMs = null
    }

    fun buildRunningNotification(snapshot: RunningNotificationSnapshot): Notification {
        val statusLine = context.getString(
            R.string.notification_status_running,
            snapshot.totalSteps,
            snapshot.remainingMinutes,
            snapshot.tickIndex,
        )
        return NotificationCompat.Builder(context, ServiceConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title_running))
            .setContentText(statusLine)
            .setSubText(snapshot.experimentId.toString())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun buildStartingNotification(experimentId: UUID): Notification {
        return NotificationCompat.Builder(context, ServiceConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title_running))
            .setContentText(context.getString(R.string.notification_status_starting))
            .setSubText(experimentId.toString())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}
