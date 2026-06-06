package com.pixson.autofit.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.health.connect.client.HealthConnectClient

class SettingsNavigator(
    private val context: Context,
) {

    fun openBatteryOptimizationSettings(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openAppDetailsSettings(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openNotificationSettings(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            openAppDetailsSettings()
        }
    }

    fun openHealthConnectSettings(): Intent? {
        return runCatching {
            HealthConnectClient.getHealthConnectManageDataIntent(context)
        }.getOrNull()?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun launch(intent: Intent) {
        context.startActivity(intent)
    }
}
