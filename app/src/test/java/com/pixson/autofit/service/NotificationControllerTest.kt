package com.pixson.autofit.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class NotificationControllerTest {

    private lateinit var clock: FakeMonotonicClock
    private lateinit var controller: NotificationController

    @Before
    fun setUp() {
        clock = FakeMonotonicClock()
        controller = NotificationController(
            context = RuntimeEnvironment.getApplication(),
            throttleMs = 60_000L,
            elapsedRealtime = clock::elapsedRealtime,
        )
    }

    @Test
    fun `first update allowed and second within throttle blocked`() {
        assertTrue(controller.shouldUpdate(force = false))
        controller.markUpdated()

        clock.advance(30_000L)
        assertFalse(controller.shouldUpdate(force = false))

        clock.advance(30_000L)
        assertTrue(controller.shouldUpdate(force = false))
    }

    @Test
    fun `force update bypasses throttle`() {
        controller.markUpdated()
        clock.advance(1_000L)
        assertTrue(controller.shouldUpdate(force = true))
    }

    @Test
    fun `reset throttle allows immediate update`() {
        controller.markUpdated()
        controller.resetThrottle()
        assertTrue(controller.shouldUpdate(force = false))
    }
}
