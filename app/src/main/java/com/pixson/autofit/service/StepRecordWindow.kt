package com.pixson.autofit.service

import java.time.Instant
import java.time.temporal.ChronoUnit

object StepRecordWindow {

    fun forTickRange(
        experimentStartTime: Instant,
        startTickIndex: Int,
        endTickIndex: Int,
    ): Pair<Instant, Instant> {
        require(startTickIndex > 0) { "startTickIndex must be positive" }
        require(endTickIndex >= startTickIndex) { "endTickIndex must be >= startTickIndex" }

        val startTime = experimentStartTime.plus(
            (startTickIndex - 1).toLong(),
            ChronoUnit.MINUTES,
        )
        val endTime = experimentStartTime.plus(
            endTickIndex.toLong(),
            ChronoUnit.MINUTES,
        )
        return startTime to endTime
    }
}
