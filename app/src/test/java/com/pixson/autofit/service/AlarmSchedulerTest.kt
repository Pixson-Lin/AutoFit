package com.pixson.autofit.service

import android.app.AlarmManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AlarmSchedulerTest {

    private lateinit var alarmManager: AlarmManager
    private lateinit var clock: FakeMonotonicClock
    private val experimentId = UUID.fromString("00000000-0000-0000-0000-000000000040")

    @Before
    fun setUp() {
        alarmManager = mockk(relaxed = true)
        clock = FakeMonotonicClock()
    }

    @Test
    fun `schedule uses exact alarm when permitted`() {
        every {
            alarmManager.setExactAndAllowWhileIdle(any(), any(), any())
        } returns Unit

        val scheduler = AlarmScheduler(
            context = RuntimeEnvironment.getApplication(),
            canScheduleExact = { true },
            elapsedRealtime = clock::elapsedRealtime,
            alarmManager = alarmManager,
        )

        scheduler.scheduleBackstop(experimentId, delayMs = 60_000L)

        verify { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
    }

    @Test
    fun `schedule uses inexact alarm when exact not permitted`() {
        every {
            alarmManager.setAndAllowWhileIdle(any(), any(), any())
        } returns Unit

        val scheduler = AlarmScheduler(
            context = RuntimeEnvironment.getApplication(),
            canScheduleExact = { false },
            elapsedRealtime = clock::elapsedRealtime,
            alarmManager = alarmManager,
        )

        scheduler.scheduleBackstop(experimentId, delayMs = 60_000L)

        verify { alarmManager.setAndAllowWhileIdle(any(), any(), any()) }
    }

    @Test
    fun `cancel clears pending alarm`() {
        val scheduler = AlarmScheduler(
            context = RuntimeEnvironment.getApplication(),
            canScheduleExact = { true },
            elapsedRealtime = clock::elapsedRealtime,
            alarmManager = alarmManager,
        )

        scheduler.cancel(experimentId)

        verify { alarmManager.cancel(any<android.app.PendingIntent>()) }
    }
}
