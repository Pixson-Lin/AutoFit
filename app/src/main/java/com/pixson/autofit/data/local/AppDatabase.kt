package com.pixson.autofit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pixson.autofit.data.local.converter.Converters
import com.pixson.autofit.data.local.dao.EnvironmentDao
import com.pixson.autofit.data.local.dao.ExperimentDao
import com.pixson.autofit.data.local.dao.HealthWriteEventDao
import com.pixson.autofit.data.local.dao.HeartbeatDao
import com.pixson.autofit.data.local.dao.ResultDao
import com.pixson.autofit.data.local.entity.EnvironmentSnapshotEntity
import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.data.local.entity.ExperimentResultEntity
import com.pixson.autofit.data.local.entity.HealthWriteEventEntity
import com.pixson.autofit.data.local.entity.HeartbeatEntity

@Database(
    entities = [
        ExperimentEntity::class,
        HeartbeatEntity::class,
        HealthWriteEventEntity::class,
        ExperimentResultEntity::class,
        EnvironmentSnapshotEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun experimentDao(): ExperimentDao
    abstract fun heartbeatDao(): HeartbeatDao
    abstract fun healthWriteEventDao(): HealthWriteEventDao
    abstract fun resultDao(): ResultDao
    abstract fun environmentDao(): EnvironmentDao

    companion object {
        const val DATABASE_NAME = "autofit.db"
    }
}
