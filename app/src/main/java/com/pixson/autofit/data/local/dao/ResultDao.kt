package com.pixson.autofit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixson.autofit.data.local.entity.ExperimentResultEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(result: ExperimentResultEntity)

    @Query("SELECT * FROM experiment_results WHERE experimentId = :experimentId")
    suspend fun getByExperimentId(experimentId: UUID): ExperimentResultEntity?

    @Query("SELECT * FROM experiment_results ORDER BY experimentId DESC")
    fun observeAll(): Flow<List<ExperimentResultEntity>>
}
