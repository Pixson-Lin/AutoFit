package com.pixson.autofit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pixson.autofit.domain.model.ExperimentStatus
import java.time.Instant
import java.util.UUID

@Entity(tableName = "experiments")
data class ExperimentEntity(
    @PrimaryKey val id: UUID,
    val startTime: Instant,
    val durationMinutes: Int,
    val targetCadence: Int,
    val randomRange: Int,
    val batchMinutes: Int,
    val status: ExperimentStatus,
)
