package com.pixson.autofit.domain

import kotlin.random.Random

/**
 * Generates step counts per FR-003:
 * generatedSteps = targetCadence + randomOffset, randomOffset ∈ [-randomRange, +randomRange]
 */
class StepGenerator(
    private val random: Random,
) {
    constructor(seed: Long) : this(Random(seed))

    fun generate(targetCadence: Int, randomRange: Int): Int {
        require(targetCadence >= 0) { "targetCadence must be non-negative" }
        require(randomRange >= 0) { "randomRange must be non-negative" }

        val offset = if (randomRange == 0) {
            0
        } else {
            random.nextInt(-randomRange, randomRange + 1)
        }
        return (targetCadence + offset).coerceAtLeast(0)
    }
}
