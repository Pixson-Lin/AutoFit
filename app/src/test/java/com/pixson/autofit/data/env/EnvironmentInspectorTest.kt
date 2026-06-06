package com.pixson.autofit.data.env

import android.content.Context
import com.pixson.autofit.domain.model.PermissionGrantState
import com.pixson.autofit.system.PermissionManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
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
class EnvironmentInspectorTest {

    private lateinit var context: Context
    private lateinit var permissionManager: PermissionManager
    private lateinit var inspector: EnvironmentInspector

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        permissionManager = mockk()
        inspector = EnvironmentInspector(context, permissionManager)
    }

    @Test
    fun `capture maps permission and device fields`() = runTest {
        every { permissionManager.isIgnoringBatteryOptimizations() } returns true
        every { permissionManager.isPowerSaveMode() } returns false
        every { permissionManager.notificationPermissionState() } returns PermissionGrantState.NOT_REQUIRED
        coEvery { permissionManager.healthConnectPermissionState() } returns PermissionGrantState.DENIED

        val experimentId = UUID.fromString("00000000-0000-0000-0000-000000000099")
        val snapshot = inspector.capture(experimentId)

        assertEquals(experimentId, snapshot.experimentId)
        assertTrue(snapshot.batteryOptimization)
        assertFalse(snapshot.powerSaveMode)
        assertEquals(PermissionGrantState.NOT_REQUIRED, snapshot.notificationPermission)
        assertEquals(PermissionGrantState.DENIED, snapshot.healthConnectPermission)
    }
}
