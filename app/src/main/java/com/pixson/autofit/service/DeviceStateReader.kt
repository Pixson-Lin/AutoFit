package com.pixson.autofit.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.view.Display
import android.hardware.display.DisplayManager

data class DeviceState(
    val batteryLevel: Int,
    val screenOn: Boolean,
    val charging: Boolean,
)

interface DeviceStateSource {
    fun read(): DeviceState
}

class DeviceStateReader(
    private val context: Context,
) : DeviceStateSource {

    override fun read(): DeviceState {
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

        val powerManager = context.getSystemService(PowerManager::class.java)
        val screenOn = if (powerManager != null) {
            powerManager.isInteractive
        } else {
            val displayManager = context.getSystemService(DisplayManager::class.java)
            displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.state == Display.STATE_ON
        }

        return DeviceState(
            batteryLevel = normalizedLevel,
            screenOn = screenOn,
            charging = charging,
        )
    }
}
