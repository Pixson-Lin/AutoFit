package com.pixson.autofit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixson.autofit.data.local.entity.HeartbeatEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface HeartbeatDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(heartbeat: HeartbeatEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(heartbeats: List<HeartbeatEntity>)

    @Query("SELECT * FROM heartbeats WHERE experimentId = :experimentId ORDER BY timestamp ASC")
    suspend fun getByExperimentId(experimentId: UUID): List<HeartbeatEntity>

    @Query("SELECT * FROM heartbeats WHERE experimentId = :experimentId ORDER BY timestamp ASC")
    fun observeByExperimentId(experimentId: UUID): Flow<List<HeartbeatEntity>>

    @Query("SELECT COUNT(*) FROM heartbeats WHERE experimentId = :experimentId")
    suspend fun countByExperimentId(experimentId: UUID): Int
}
