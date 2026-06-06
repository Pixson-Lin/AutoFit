package com.pixson.autofit

import android.app.Application
import androidx.room.Room
import com.pixson.autofit.data.local.AppDatabase
import com.pixson.autofit.data.repo.ExperimentRepository

class AutoFitApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var experimentRepository: ExperimentRepository
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
    }
}
