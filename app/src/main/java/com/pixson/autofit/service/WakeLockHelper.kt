package com.pixson.autofit.service

import android.os.PowerManager

class WakeLockHelper(
    powerManager: PowerManager,
) : WakeLockGateway {

    private val wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        ServiceConstants.WAKE_LOCK_TAG,
    ).apply {
        setReferenceCounted(false)
    }

    override fun acquire() {
        if (!wakeLock.isHeld) {
            wakeLock.acquire(ServiceConstants.WAKE_LOCK_TIMEOUT_MS)
        }
    }

    override fun release() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }
}
