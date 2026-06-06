package com.pixson.autofit.service

class CountingWakeLockGateway : WakeLockGateway {
    var acquireCount = 0
        private set
    var releaseCount = 0
        private set

    override fun acquire() {
        acquireCount++
    }

    override fun release() {
        releaseCount++
    }
}
