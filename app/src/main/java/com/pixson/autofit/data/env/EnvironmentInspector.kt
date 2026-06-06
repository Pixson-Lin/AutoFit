package com.pixson.autofit.data.env

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.pixson.autofit.data.local.entity.EnvironmentSnapshotEntity
import com.pixson.autofit.system.PermissionManager
import java.util.UUID

class EnvironmentInspector(
    private val context: Context,
    private val permissionManager: PermissionManager,
) {

    suspend fun capture(experimentId: UUID): EnvironmentSnapshotEntity {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        val batteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val batteryScale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val normalizedLevel = if (batteryLevel >= 0 && batteryScale > 0) {
            (batteryLevel * 100) / batteryScale
        } else {
            -1
        }
        val chargingStatus = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = chargingStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
            chargingStatus == BatteryManager.BATTERY_STATUS_FULL

        return EnvironmentSnapshotEntity(
            experimentId = experimentId,
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            batteryOptimization = permissionManager.isIgnoringBatteryOptimizations(),
            powerSaveMode = permissionManager.isPowerSaveMode(),
            charging = charging,
            batteryLevel = normalizedLevel,
            notificationPermission = permissionManager.notificationPermissionState(),
            healthConnectPermission = permissionManager.healthConnectPermissionState(),
        )
    }
}
