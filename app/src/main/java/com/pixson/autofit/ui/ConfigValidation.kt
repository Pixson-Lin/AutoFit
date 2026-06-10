package com.pixson.autofit.ui

import com.pixson.autofit.domain.model.ExperimentConfig

object ConfigValidation {
    const val MIN_CADENCE = 1
    const val MAX_CADENCE = 500
    const val MAX_RANDOM_RANGE = 200
    const val MIN_DURATION_MINUTES = 1
    const val MAX_DURATION_MINUTES = 180

    fun validatedConfig(
        targetCadenceInput: String,
        randomRangeInput: String,
        durationMinutesInput: String,
        batchMinutes: Int,
    ): ExperimentConfig? {
        val cadence = targetCadenceInput.trim().toIntOrNull() ?: return null
        val range = randomRangeInput.trim().toIntOrNull() ?: return null
        val duration = durationMinutesInput.trim().toIntOrNull() ?: return null
        if (cadence !in MIN_CADENCE..MAX_CADENCE) return null
        if (range !in 0..MAX_RANDOM_RANGE) return null
        if (duration !in MIN_DURATION_MINUTES..MAX_DURATION_MINUTES) return null
        if (batchMinutes !in ExperimentConfig.BATCH_MINUTE_OPTIONS) return null
        return ExperimentConfig(
            targetCadence = cadence,
            randomRange = range,
            durationMinutes = duration,
            batchMinutes = batchMinutes,
        )
    }

    fun validationHint(
        targetCadenceInput: String,
        randomRangeInput: String,
        durationMinutesInput: String,
    ): String? {
        val cadence = targetCadenceInput.trim().toIntOrNull()
        if (cadence == null || cadence !in MIN_CADENCE..MAX_CADENCE) {
            return "Target cadence must be $MIN_CADENCE–$MAX_CADENCE SPM"
        }
        val range = randomRangeInput.trim().toIntOrNull()
        if (range == null || range !in 0..MAX_RANDOM_RANGE) {
            return "Random range must be 0–$MAX_RANDOM_RANGE"
        }
        val duration = durationMinutesInput.trim().toIntOrNull()
        if (duration == null || duration !in MIN_DURATION_MINUTES..MAX_DURATION_MINUTES) {
            return "Duration must be $MIN_DURATION_MINUTES–$MAX_DURATION_MINUTES minutes"
        }
        return null
    }
}
