package com.pixson.autofit.service

object ServiceConstants {
    const val EXTRA_EXPERIMENT_ID = "experiment_id"
    const val ACTION_STOP = "com.pixson.autofit.action.STOP_EXPERIMENT"
    const val ACTION_ALARM_BACKSTOP = "com.pixson.autofit.action.ALARM_BACKSTOP"

    const val NOTIFICATION_CHANNEL_ID = "autofit_experiment"
    const val NOTIFICATION_ID = 1001
    const val ALARM_REQUEST_CODE_BASE = 2000

    const val TICK_INTERVAL_MS = 60_000L
    const val NOTIFICATION_THROTTLE_MS = TICK_INTERVAL_MS
    const val WRITE_BATCH_TICK_COUNT = 1
    const val WAKE_LOCK_TAG = "AutoFit::ExperimentTick"
    const val WAKE_LOCK_TIMEOUT_MS = 5_000L
}
