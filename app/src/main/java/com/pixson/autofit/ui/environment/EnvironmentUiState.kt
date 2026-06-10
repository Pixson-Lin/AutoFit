package com.pixson.autofit.ui.environment

data class EnvironmentChecklistItem(
    val id: String,
    val title: String,
    val statusText: String,
    val isOk: Boolean,
    val fixAction: EnvironmentFixAction?,
)

enum class EnvironmentFixAction {
    BATTERY_OPTIMIZATION,
    BATTERY_SAVER,
    NOTIFICATION_SETTINGS,
    HEALTH_CONNECT_SETTINGS,
    REQUEST_HEALTH_PERMISSIONS,
    REQUEST_NOTIFICATION,
    REQUEST_ACTIVITY_RECOGNITION,
    OVERLAY_SETTINGS,
    HEALTH_CONNECT_INSTALL,
}

data class EnvironmentUiState(
    val items: List<EnvironmentChecklistItem> = emptyList(),
    val deviceSummary: String = "",
    val isRefreshing: Boolean = false,
)
