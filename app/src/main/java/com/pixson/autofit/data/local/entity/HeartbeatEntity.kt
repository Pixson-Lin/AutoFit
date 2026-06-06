package com.pixson.autofit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "heartbeats",
    indices = [Index("experimentId")],
)
data class HeartbeatEntity(
    @PrimaryKey val id: UUID,
    val experimentId: UUID,
    val timestamp: Instant,
    val generatedSteps: Int,
    val batteryLevel: Int,
    val screenOn: Boolean,
    val charging: Boolean,
)
