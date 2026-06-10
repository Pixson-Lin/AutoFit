package com.pixson.autofit.ui.environment

import android.content.Context
import com.pixson.autofit.domain.model.PermissionGrantState
import com.pixson.autofit.system.PermissionManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
class EnvironmentViewModelTest {

    private lateinit var context: Context
    private lateinit var permissionManager: PermissionManager
    private lateinit var viewModel: EnvironmentViewModel

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        permissionManager = mockk()
    }

    @Test
    fun `all green checklist when permissions and power state are ideal`() = runTest {
        every { permissionManager.isIgnoringBatteryOptimizations() } returns true
        every { permissionManager.isPowerSaveMode() } returns false
        every { permissionManager.notificationPermissionState() } returns PermissionGrantState.NOT_REQUIRED
        coEvery { permissionManager.healthConnectPermissionState() } returns PermissionGrantState.GRANTED
        every { permissionManager.canStartHealthForegroundService() } returns true

        viewModel = EnvironmentViewModel(context, permissionManager)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.items.all { it.isOk })
    }

    @Test
    fun `partial red when battery optimization and HC denied`() = runTest {
        every { permissionManager.isIgnoringBatteryOptimizations() } returns false
        every { permissionManager.isPowerSaveMode() } returns true
        every { permissionManager.notificationPermissionState() } returns PermissionGrantState.DENIED
        coEvery { permissionManager.healthConnectPermissionState() } returns PermissionGrantState.DENIED
        every { permissionManager.canStartHealthForegroundService() } returns false

        viewModel = EnvironmentViewModel(context, permissionManager)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.items.first { it.id == "battery_optimization" }.isOk)
        assertFalse(state.items.first { it.id == "power_save" }.isOk)
        assertFalse(state.items.first { it.id == "health_connect" }.isOk)
        assertFalse(state.items.first { it.id == "activity_recognition" }.isOk)
    }
}
