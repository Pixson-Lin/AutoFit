package com.pixson.autofit.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class HealthConnectManagerTest {

    private lateinit var gateway: HealthConnectGateway
    private lateinit var manager: HealthConnectManager

    @Before
    fun setUp() {
        gateway = mockk()
        manager = HealthConnectManager(gateway)
    }

    @Test
    fun `sdk available when status is SDK_AVAILABLE`() {
        coEvery { gateway.getSdkStatus() } returns HealthConnectClient.SDK_AVAILABLE

        val status = manager.getSdkStatus()

        assertTrue(status is HealthSdkStatus.Available)
    }

    @Test
    fun `sdk unavailable when provider update required`() {
        coEvery { gateway.getSdkStatus() } returns
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

        val status = manager.getSdkStatus()

        assertTrue(status is HealthSdkStatus.Unavailable)
        assertEquals(
            "Health Connect update required",
            (status as HealthSdkStatus.Unavailable).reason,
        )
    }

    @Test
    fun `sdk unavailable when not installed`() {
        coEvery { gateway.getSdkStatus() } returns HealthConnectClient.SDK_UNAVAILABLE

        val status = manager.getSdkStatus()

        assertTrue(status is HealthSdkStatus.Unavailable)
    }

    @Test
    fun `write fails when permission not granted`() = runTest {
        coEvery { gateway.getSdkStatus() } returns HealthConnectClient.SDK_AVAILABLE
        coEvery { gateway.getGrantedPermissions() } returns emptySet()

        val result = manager.writeSteps(
            stepCount = 100,
            startTime = Instant.parse("2026-06-06T10:00:00Z"),
            endTime = Instant.parse("2026-06-06T10:01:00Z"),
        )

        assertTrue(result is WriteResult.Failure)
        assertEquals(
            "WRITE_STEPS permission not granted",
            (result as WriteResult.Failure).reason,
        )
    }

    @Test
    fun `write succeeds when permission granted`() = runTest {
        coEvery { gateway.getSdkStatus() } returns HealthConnectClient.SDK_AVAILABLE
        coEvery { gateway.getGrantedPermissions() } returns setOf(manager.writeStepsPermission)
        coEvery { gateway.insertRecords(any()) } returns Unit

        val result = manager.writeSteps(
            stepCount = 120,
            startTime = Instant.parse("2026-06-06T10:00:00Z"),
            endTime = Instant.parse("2026-06-06T10:01:00Z"),
        )

        assertTrue(result is WriteResult.Success)
        coVerify { gateway.insertRecords(match { it.size == 1 && it.first() is StepsRecord }) }
    }

    @Test
    fun `write fails when end time is not after start time`() = runTest {
        coEvery { gateway.getSdkStatus() } returns HealthConnectClient.SDK_AVAILABLE
        coEvery { gateway.getGrantedPermissions() } returns setOf(manager.writeStepsPermission)

        val instant = Instant.parse("2026-06-06T10:00:00Z")
        val result = manager.writeSteps(
            stepCount = 120,
            startTime = instant,
            endTime = instant,
        )

        assertTrue(result is WriteResult.Failure)
        assertEquals(
            "endTime must be after startTime",
            (result as WriteResult.Failure).reason,
        )
    }

    @Test
    fun `hasWritePermission returns false when sdk unavailable`() = runTest {
        coEvery { gateway.getSdkStatus() } returns HealthConnectClient.SDK_UNAVAILABLE

        assertFalse(manager.hasWritePermission())
    }
}
