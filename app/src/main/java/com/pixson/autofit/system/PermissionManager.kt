package com.pixson.autofit.system

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.pixson.autofit.data.health.HealthConnectManager
import com.pixson.autofit.data.health.HealthSdkStatus
import com.pixson.autofit.domain.model.PermissionGrantState

class PermissionManager(
    private val context: Context,
    private val healthConnectManager: HealthConnectManager,
) {

    fun isNotificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun notificationPermissionState(): Int {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            PermissionGrantState.NOT_REQUIRED
        } else if (isNotificationPermissionGranted()) {
            PermissionGrantState.GRANTED
        } else {
            PermissionGrantState.DENIED
        }
    }

    suspend fun healthConnectPermissionState(): Int {
        return when (healthConnectManager.getSdkStatus()) {
            is HealthSdkStatus.Available ->
                if (healthConnectManager.hasWritePermission()) {
                    PermissionGrantState.GRANTED
                } else {
                    PermissionGrantState.DENIED
                }
            is HealthSdkStatus.Unavailable ->
                PermissionGrantState.NOT_APPLICABLE
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
            ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun exactAlarmPermissionState(): Int {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            PermissionGrantState.NOT_REQUIRED
        } else if (canScheduleExactAlarms()) {
            PermissionGrantState.GRANTED
        } else {
            PermissionGrantState.DENIED
        }
    }

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    fun overlayPermissionState(): Int {
        return if (canDrawOverlays()) {
            PermissionGrantState.GRANTED
        } else {
            PermissionGrantState.DENIED
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java)
            ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isPowerSaveMode(): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java)
            ?: return false
        return powerManager.isPowerSaveMode
    }

    fun requiredHealthConnectPermissions(): Set<String> =
        setOf(healthConnectManager.writeStepsPermission)
}
