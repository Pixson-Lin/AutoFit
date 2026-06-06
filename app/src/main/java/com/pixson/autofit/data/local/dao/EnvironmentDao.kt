package com.pixson.autofit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixson.autofit.data.local.entity.EnvironmentSnapshotEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface EnvironmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: EnvironmentSnapshotEntity)

    @Query("SELECT * FROM environment_snapshots WHERE experimentId = :experimentId")
    suspend fun getByExperimentId(experimentId: UUID): EnvironmentSnapshotEntity?

    @Query("SELECT * FROM environment_snapshots WHERE experimentId = :experimentId")
    fun observeByExperimentId(experimentId: UUID): Flow<EnvironmentSnapshotEntity?>
}
