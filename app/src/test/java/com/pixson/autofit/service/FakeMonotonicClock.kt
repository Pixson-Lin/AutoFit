package com.pixson.autofit.service

import java.time.Instant

class FakeMonotonicClock(
    initialInstant: Instant = Instant.parse("2026-06-06T10:00:00Z"),
) {
    private var elapsedMs = 0L
    private var instant = initialInstant

    fun elapsedRealtime(): Long = elapsedMs

    fun currentInstant(): Instant = instant

    fun advance(ms: Long) {
        elapsedMs += ms
        instant = instant.plusMillis(ms)
    }
}
