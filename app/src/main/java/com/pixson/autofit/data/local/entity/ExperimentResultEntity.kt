package com.pixson.autofit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "experiment_results")
data class ExperimentResultEntity(
    @PrimaryKey val experimentId: UUID,
    val totalSteps: Int,
    val heartbeatCount: Int,
    val writeSuccessCount: Int,
    val writeFailureCount: Int,
    val actualDuration: Int,
)
