package com.pixson.autofit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pixson.autofit.R
import java.util.UUID

class NotificationController(
    private val context: Context,
) {

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

    fun buildRunningNotification(
        experimentId: UUID,
        statusLine: String,
    ): Notification {
        return NotificationCompat.Builder(context, ServiceConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_title_running))
            .setContentText(statusLine)
            .setSubText(experimentId.toString())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}
