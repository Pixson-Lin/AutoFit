package com.pixson.autofit.service

import android.content.Context
import com.pixson.autofit.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class OverlayControllerTest {

    private lateinit var context: Context
    private lateinit var fakeHost: RecordingOverlayWindowHost

    @Before
    fun setUp() {
        context = mockk(relaxed = true) {
            every {
                getString(R.string.overlay_status_running, any(), any(), any())
            } returns "overlay-chip"
        }
        fakeHost = RecordingOverlayWindowHost(canShow = false)
    }

    @Test
    fun `update without overlay permission does not show window`() {
        val controller = OverlayController(
            context = context,
            overlayHost = fakeHost,
            elapsedRealtime = { 0L },
            mainHandler = android.os.Handler(android.os.Looper.getMainLooper()),
        )

        controller.update(sampleSnapshot(), force = true)

        assertEquals(0, fakeHost.showCount)
        assertFalse(fakeHost.canShow())
    }

    @Test
    fun `update with permission shows throttled overlay text`() {
        fakeHost = RecordingOverlayWindowHost(canShow = true)
        var elapsed = 0L
        val controller = OverlayController(
            context = context,
            overlayHost = fakeHost,
            throttleMs = 60_000L,
            elapsedRealtime = { elapsed },
            mainHandler = android.os.Handler(android.os.Looper.getMainLooper()),
        )

        controller.update(sampleSnapshot(totalSteps = 100), force = true)
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertEquals(1, fakeHost.showCount)
        assertEquals("overlay-chip", fakeHost.lastText)

        controller.update(sampleSnapshot(totalSteps = 200), force = false)
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertEquals(1, fakeHost.showCount)

        elapsed = 60_001L
        controller.update(sampleSnapshot(totalSteps = 200), force = true)
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertEquals(2, fakeHost.showCount)
    }

    private fun sampleSnapshot(
        totalSteps: Int = 50,
    ): RunningNotificationSnapshot = RunningNotificationSnapshot(
        experimentId = UUID.fromString("00000000-0000-0000-0000-000000000070"),
        totalSteps = totalSteps,
        remainingMinutes = 3,
        tickIndex = 2,
    )
}

private class RecordingOverlayWindowHost(
    private var canShow: Boolean,
) : OverlayWindowHost {

    var showCount = 0
    var lastText: CharSequence? = null

    override fun canShow(): Boolean = canShow

    override fun showOrUpdate(text: CharSequence) {
        showCount++
        lastText = text
    }

    override fun dismiss() = Unit
}
