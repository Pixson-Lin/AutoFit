package com.pixson.autofit.ui.environment

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pixson.autofit.system.PermissionManager

class EnvironmentViewModelFactory(
    private val appContext: Context,
    private val permissionManager: PermissionManager,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EnvironmentViewModel::class.java)) {
            return EnvironmentViewModel(
                appContext = appContext,
                permissionManager = permissionManager,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
