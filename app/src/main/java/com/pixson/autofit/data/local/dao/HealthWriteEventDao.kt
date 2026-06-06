package com.pixson.autofit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixson.autofit.data.local.entity.HealthWriteEventEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface HealthWriteEventDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: HealthWriteEventEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(events: List<HealthWriteEventEntity>)

    @Query("SELECT * FROM health_write_events WHERE experimentId = :experimentId ORDER BY timestamp ASC")
    suspend fun getByExperimentId(experimentId: UUID): List<HealthWriteEventEntity>

    @Query("SELECT * FROM health_write_events WHERE experimentId = :experimentId ORDER BY timestamp ASC")
    fun observeByExperimentId(experimentId: UUID): Flow<List<HealthWriteEventEntity>>
}
