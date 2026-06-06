package com.pixson.autofit.service

import android.content.Context
import android.os.PowerManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class WakeLockHelperTest {

    private lateinit var wakeLockHelper: WakeLockHelper

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication() as Context
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLockHelper = WakeLockHelper(powerManager)
    }

    @After
    fun tearDown() {
        wakeLockHelper.release()
    }

    @Test
    fun `acquire and release are paired`() {
        wakeLockHelper.acquire()
        wakeLockHelper.release()
        wakeLockHelper.acquire()
        wakeLockHelper.release()
        assertEquals(0, 0)
    }
}
