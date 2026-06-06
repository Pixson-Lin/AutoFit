package com.pixson.autofit.system

import android.content.Intent
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class SettingsNavigatorTest {

    private lateinit var navigator: SettingsNavigator

    @Before
    fun setUp() {
        navigator = SettingsNavigator(RuntimeEnvironment.getApplication())
    }

    @Test
    fun `openAppDetailsSettings targets package`() {
        val intent = navigator.openAppDetailsSettings()
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertNotNull(intent.data)
    }

    @Test
    fun `openBatteryOptimizationSettings uses ignore optimizations action`() {
        val intent = navigator.openBatteryOptimizationSettings()
        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, intent.action)
    }

    @Test
    fun `openNotificationSettings returns valid intent`() {
        val intent = navigator.openNotificationSettings()
        assertNotNull(intent.action)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
