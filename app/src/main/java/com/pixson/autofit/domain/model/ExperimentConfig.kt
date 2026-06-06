package com.pixson.autofit.domain.model

data class ExperimentConfig(
    val targetCadence: Int,
    val randomRange: Int,
    val durationMinutes: Int,
) {
    init {
        require(targetCadence >= 0) { "targetCadence must be non-negative" }
        require(randomRange >= 0) { "randomRange must be non-negative" }
        require(durationMinutes > 0) { "durationMinutes must be positive" }
    }
}
