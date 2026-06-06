package com.pixson.autofit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.domain.model.ExperimentStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ExperimentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(experiment: ExperimentEntity)

    @Update
    suspend fun update(experiment: ExperimentEntity)

    @Query("SELECT * FROM experiments WHERE id = :id")
    suspend fun getById(id: UUID): ExperimentEntity?

    @Query("SELECT * FROM experiments WHERE id = :id")
    fun observeById(id: UUID): Flow<ExperimentEntity?>

    @Query("SELECT * FROM experiments WHERE status = :status LIMIT 1")
    suspend fun getByStatus(status: ExperimentStatus): ExperimentEntity?

    @Query("SELECT * FROM experiments ORDER BY startTime DESC")
    fun observeAll(): Flow<List<ExperimentEntity>>

    @Query("UPDATE experiments SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: UUID, status: ExperimentStatus)
}
