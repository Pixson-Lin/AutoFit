package com.pixson.autofit.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pixson.autofit.AutoFitApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as AutoFitApplication
        val pendingResult = goAsync()
        scope.launch {
            try {
                app.bootInterruptionHandler.handleBootCompleted()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
