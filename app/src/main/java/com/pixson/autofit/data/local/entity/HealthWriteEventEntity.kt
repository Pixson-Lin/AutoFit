package com.pixson.autofit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "health_write_events",
    indices = [Index("experimentId")],
)
data class HealthWriteEventEntity(
    @PrimaryKey val id: UUID,
    val experimentId: UUID,
    val timestamp: Instant,
    val recordStart: Instant,
    val recordEnd: Instant,
    val stepCount: Int,
    val success: Boolean,
    val errorMessage: String,
)
