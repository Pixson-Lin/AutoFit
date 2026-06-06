package com.pixson.autofit.system

import android.os.Build
import com.pixson.autofit.data.health.HealthConnectManager
import com.pixson.autofit.data.health.HealthSdkStatus
import com.pixson.autofit.domain.model.PermissionGrantState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class PermissionManagerTest {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var permissionManager: PermissionManager

    @Before
    fun setUp() {
        healthConnectManager = mockk()
        every { healthConnectManager.writeStepsPermission } returns "android.permission.health.WRITE_STEPS"
        permissionManager = PermissionManager(
            context = RuntimeEnvironment.getApplication(),
            healthConnectManager = healthConnectManager,
        )
    }

    @Test
    fun `notification permission not required on API 31`() {
        assertEquals(
            PermissionGrantState.NOT_REQUIRED,
            permissionManager.notificationPermissionState(),
        )
        assertTrue(permissionManager.isNotificationPermissionGranted())
    }

    @Test
    fun `health connect permission denied when write not granted`() = runTest {
        every { healthConnectManager.getSdkStatus() } returns HealthSdkStatus.Available
        coEvery { healthConnectManager.hasWritePermission() } returns false

        assertEquals(
            PermissionGrantState.DENIED,
            permissionManager.healthConnectPermissionState(),
        )
    }

    @Test
    fun `health connect permission not applicable when sdk unavailable`() = runTest {
        every { healthConnectManager.getSdkStatus() } returns HealthSdkStatus.Unavailable(
            reason = "not installed",
            rawStatus = 1,
        )

        assertEquals(
            PermissionGrantState.NOT_APPLICABLE,
            permissionManager.healthConnectPermissionState(),
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `exact alarm state reflects canScheduleExactAlarms on API 31+`() {
        val state = permissionManager.exactAlarmPermissionState()
        assertTrue(
            state == PermissionGrantState.GRANTED || state == PermissionGrantState.DENIED,
        )
    }
}
