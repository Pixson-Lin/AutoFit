package com.pixson.autofit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pixson.autofit.data.health.HealthConnectManager
import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.ExperimentController
import com.pixson.autofit.system.PermissionManager

class ExperimentViewModelFactory(
    private val repository: ExperimentRepository,
    private val experimentController: ExperimentController,
    private val healthConnectManager: HealthConnectManager,
    private val permissionManager: PermissionManager,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExperimentViewModel::class.java)) {
            return ExperimentViewModel(
                repository = repository,
                experimentController = experimentController,
                healthConnectManager = healthConnectManager,
                permissionManager = permissionManager,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
