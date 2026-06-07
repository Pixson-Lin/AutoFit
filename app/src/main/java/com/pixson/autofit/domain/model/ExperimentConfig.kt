package com.pixson.autofit.domain.model

data class ExperimentConfig(
    val targetCadence: Int,
    val randomRange: Int,
    val durationMinutes: Int,
    val batchMinutes: Int = DEFAULT_BATCH_MINUTES,
) {
    init {
        require(targetCadence >= 0) { "targetCadence must be non-negative" }
        require(randomRange >= 0) { "randomRange must be non-negative" }
        require(durationMinutes > 0) { "durationMinutes must be positive" }
        require(batchMinutes in BATCH_MINUTE_OPTIONS) {
            "batchMinutes must be one of $BATCH_MINUTE_OPTIONS"
        }
    }

    companion object {
        const val DEFAULT_BATCH_MINUTES = 1
        val BATCH_MINUTE_OPTIONS = listOf(1, 3, 5)
    }
}
