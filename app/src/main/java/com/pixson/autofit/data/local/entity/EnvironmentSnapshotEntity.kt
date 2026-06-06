package com.pixson.autofit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "environment_snapshots",
    indices = [Index("experimentId")],
)
data class EnvironmentSnapshotEntity(
    @PrimaryKey val experimentId: UUID,
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val batteryOptimization: Boolean,
    val powerSaveMode: Boolean,
    val charging: Boolean,
    val batteryLevel: Int,
    val notificationPermission: Int,
    val healthConnectPermission: Int,
)
