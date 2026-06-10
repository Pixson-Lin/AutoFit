package com.pixson.autofit.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.pixson.autofit.R

class OverlayController(
    private val context: Context,
    private val overlayHost: OverlayWindowHost,
    private val throttleMs: Long = ServiceConstants.NOTIFICATION_THROTTLE_MS,
    private val elapsedRealtime: () -> Long,
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
) {

    private var lastUpdateElapsedMs: Long? = null
    private var lastRenderedText: String? = null

    fun shouldUpdate(force: Boolean = false): Boolean {
        if (!overlayHost.canShow()) return false
        if (force) return true
        val lastUpdate = lastUpdateElapsedMs ?: return true
        return elapsedRealtime() - lastUpdate >= throttleMs
    }

    fun markUpdated() {
        lastUpdateElapsedMs = elapsedRealtime()
    }

    fun resetThrottle() {
        lastUpdateElapsedMs = null
        lastRenderedText = null
    }

    fun update(snapshot: RunningNotificationSnapshot, force: Boolean = false) {
        if (!overlayHost.canShow()) {
            ServiceEventLogger.overlaySkipped("missing_permission")
            return
        }
        if (!shouldUpdate(force)) return

        val text = formatSnapshot(context, snapshot)
        if (text == lastRenderedText && !force) return

        mainHandler.post {
            if (!overlayHost.canShow()) return@post
            overlayHost.showOrUpdate(text)
            lastRenderedText = text
            markUpdated()
            ServiceEventLogger.overlayUpdated(snapshot.experimentId, snapshot.tickIndex)
        }
    }

    fun dismiss() {
        mainHandler.post {
            overlayHost.dismiss()
            lastRenderedText = null
            resetThrottle()
            ServiceEventLogger.overlayDismissed()
        }
    }

    companion object {
        fun formatSnapshot(context: Context, snapshot: RunningNotificationSnapshot): String {
            return context.getString(
                R.string.overlay_status_running,
                snapshot.totalSteps,
                snapshot.remainingMinutes,
                snapshot.tickIndex,
            )
        }
    }
}
