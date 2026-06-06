package com.pixson.autofit

import android.app.Application
import androidx.room.Room
import com.pixson.autofit.data.env.EnvironmentInspector
import com.pixson.autofit.data.health.HealthConnectGatewayImpl
import com.pixson.autofit.data.health.HealthConnectManager
import com.pixson.autofit.data.local.AppDatabase
import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.ExperimentController
import com.pixson.autofit.system.PermissionManager
import com.pixson.autofit.system.SettingsNavigator

class AutoFitApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var experimentRepository: ExperimentRepository
        private set

    lateinit var healthConnectManager: HealthConnectManager
        private set

    lateinit var permissionManager: PermissionManager
        private set

    lateinit var environmentInspector: EnvironmentInspector
        private set

    lateinit var settingsNavigator: SettingsNavigator
        private set

    lateinit var experimentController: ExperimentController
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        )
            .fallbackToDestructiveMigration()
            .build()

        experimentRepository = ExperimentRepository(
            experimentDao = database.experimentDao(),
            heartbeatDao = database.heartbeatDao(),
            healthWriteEventDao = database.healthWriteEventDao(),
            resultDao = database.resultDao(),
            environmentDao = database.environmentDao(),
        )

        healthConnectManager = HealthConnectManager(
            gateway = HealthConnectGatewayImpl(applicationContext),
        )
        permissionManager = PermissionManager(
            context = applicationContext,
            healthConnectManager = healthConnectManager,
        )
        environmentInspector = EnvironmentInspector(
            context = applicationContext,
            permissionManager = permissionManager,
        )
        settingsNavigator = SettingsNavigator(applicationContext)
        experimentController = ExperimentController(
            appContext = applicationContext,
            repository = experimentRepository,
            environmentInspector = environmentInspector,
        )
    }
}
