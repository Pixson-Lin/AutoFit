package com.pixson.autofit.ui.environment

import android.content.Context
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixson.autofit.domain.model.PermissionGrantState
import com.pixson.autofit.system.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EnvironmentViewModel(
    private val appContext: Context,
    private val permissionManager: PermissionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(EnvironmentUiState())
    val state: StateFlow<EnvironmentUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { refresh() }
    }

    suspend fun refresh() {
        _state.update { it.copy(isRefreshing = true) }

        val batteryStatus = appContext.registerReceiver(
            null,
            IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED),
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

        val batteryOptimizationIgnored = permissionManager.isIgnoringBatteryOptimizations()
        val powerSaveOn = permissionManager.isPowerSaveMode()
        val notificationState = permissionManager.notificationPermissionState()
        val healthConnectState = permissionManager.healthConnectPermissionState()
        val activityRecognitionGranted = permissionManager.canStartHealthForegroundService()
        val overlayGranted = permissionManager.canDrawOverlays()

        val items = listOf(
            EnvironmentChecklistItem(
                id = "battery_optimization",
                title = "Battery optimization",
                statusText = if (batteryOptimizationIgnored) "Ignored (recommended)" else "Restricted",
                isOk = batteryOptimizationIgnored,
                fixAction = if (batteryOptimizationIgnored) null else EnvironmentFixAction.BATTERY_OPTIMIZATION,
            ),
            EnvironmentChecklistItem(
                id = "power_save",
                title = "Power save mode",
                statusText = if (powerSaveOn) "On" else "Off",
                isOk = !powerSaveOn,
                fixAction = if (powerSaveOn) EnvironmentFixAction.BATTERY_SAVER else null,
            ),
            EnvironmentChecklistItem(
                id = "charging",
                title = "Charging state",
                statusText = when {
                    charging -> "Charging"
                    normalizedLevel >= 0 -> "Not charging ($normalizedLevel%)"
                    else -> "Unknown"
                },
                isOk = true,
                fixAction = null,
            ),
            EnvironmentChecklistItem(
                id = "notification",
                title = "Notification permission",
                statusText = permissionLabel(notificationState),
                isOk = notificationState == PermissionGrantState.GRANTED ||
                    notificationState == PermissionGrantState.NOT_REQUIRED,
                fixAction = when (notificationState) {
                    PermissionGrantState.DENIED -> EnvironmentFixAction.REQUEST_NOTIFICATION
                    PermissionGrantState.GRANTED,
                    PermissionGrantState.NOT_REQUIRED,
                    -> EnvironmentFixAction.NOTIFICATION_SETTINGS
                    else -> null
                },
            ),
            EnvironmentChecklistItem(
                id = "health_connect",
                title = "Health Connect WRITE_STEPS",
                statusText = permissionLabel(healthConnectState),
                isOk = healthConnectState == PermissionGrantState.GRANTED,
                fixAction = when (healthConnectState) {
                    PermissionGrantState.GRANTED -> EnvironmentFixAction.HEALTH_CONNECT_SETTINGS
                    PermissionGrantState.DENIED -> EnvironmentFixAction.REQUEST_HEALTH_PERMISSIONS
                    PermissionGrantState.NOT_APPLICABLE -> EnvironmentFixAction.HEALTH_CONNECT_INSTALL
                    else -> null
                },
            ),
            EnvironmentChecklistItem(
                id = "overlay",
                title = "Display over other apps",
                statusText = if (overlayGranted) {
                    "Granted (optional overlay chip)"
                } else {
                    "Not granted (notification only)"
                },
                isOk = true,
                fixAction = if (overlayGranted) null else EnvironmentFixAction.OVERLAY_SETTINGS,
            ),
            EnvironmentChecklistItem(
                id = "activity_recognition",
                title = "Activity recognition",
                statusText = if (activityRecognitionGranted) "Granted" else "Denied",
                isOk = activityRecognitionGranted,
                fixAction = if (activityRecognitionGranted) {
                    null
                } else {
                    EnvironmentFixAction.REQUEST_ACTIVITY_RECOGNITION
                },
            ),
        )

        val deviceSummary = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"

        _state.update {
            it.copy(
                items = items,
                deviceSummary = deviceSummary,
                isRefreshing = false,
            )
        }
    }

    private fun permissionLabel(state: Int): String = when (state) {
        PermissionGrantState.GRANTED -> "Granted"
        PermissionGrantState.DENIED -> "Denied"
        PermissionGrantState.NOT_REQUIRED -> "Not required"
        PermissionGrantState.NOT_APPLICABLE -> "Not applicable"
        else -> "Unknown"
    }
}
