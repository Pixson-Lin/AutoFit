package com.pixson.autofit.data.local.converter

import androidx.room.TypeConverter
import com.pixson.autofit.domain.model.ExperimentStatus
import java.time.Instant
import java.util.UUID

class Converters {

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromUuid(value: UUID?): String? = value?.toString()

    @TypeConverter
    fun toUuid(value: String?): UUID? = value?.let(UUID::fromString)

    @TypeConverter
    fun fromExperimentStatus(value: ExperimentStatus): String = value.name

    @TypeConverter
    fun toExperimentStatus(value: String): ExperimentStatus = ExperimentStatus.valueOf(value)
}
