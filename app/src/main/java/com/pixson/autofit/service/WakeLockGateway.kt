package com.pixson.autofit.service

interface WakeLockGateway {
    fun acquire()
    fun release()
}
