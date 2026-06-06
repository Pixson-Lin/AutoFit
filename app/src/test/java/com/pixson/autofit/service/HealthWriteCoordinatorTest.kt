package com.pixson.autofit.service

import androidx.room.Room
import com.pixson.autofit.data.health.HealthConnectManager
import com.pixson.autofit.data.health.HealthSdkStatus
import com.pixson.autofit.data.health.WriteResult
import com.pixson.autofit.data.local.AppDatabase
import com.pixson.autofit.data.repo.ExperimentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class HealthWriteCoordinatorTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ExperimentRepository
    private lateinit var healthConnectManager: HealthConnectManager
    private val experimentId = UUID.fromString("00000000-0000-0000-0000-000000000020")
    private var now = Instant.parse("2026-06-06T10:00:00Z")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            org.robolectric.RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = ExperimentRepository(
            experimentDao = database.experimentDao(),
            heartbeatDao = database.heartbeatDao(),
            healthWriteEventDao = database.healthWriteEventDao(),
            resultDao = database.resultDao(),
            environmentDao = database.environmentDao(),
        )
        healthConnectManager = mockk()
        coEvery { healthConnectManager.getSdkStatus() } returns HealthSdkStatus.Available
        coEvery { healthConnectManager.hasWritePermission() } returns true
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `batch accumulates ticks before single health connect write`() = runTest {
        coEvery {
            healthConnectManager.writeSteps(any(), any(), any())
        } returns WriteResult.Success

        val coordinator = createCoordinator(batchTickCount = 3)

        coordinator.recordGeneratedSteps(experimentId, 100)
        coordinator.recordGeneratedSteps(experimentId, 110)
        assertEquals(0, repository.getHealthWriteEvents(experimentId).size)

        coordinator.recordGeneratedSteps(experimentId, 90)

        coVerify(exactly = 1) {
            healthConnectManager.writeSteps(
                stepCount = 300,
                startTime = any(),
                endTime = any(),
            )
        }

        val events = repository.getHealthWriteEvents(experimentId)
        assertEquals(1, events.size)
        assertEquals(300, events.first().stepCount)
        assertTrue(events.first().success)
    }

    @Test
    fun `failed write still records event and loop can continue`() = runTest {
        coEvery {
            healthConnectManager.writeSteps(any(), any(), any())
        } returns WriteResult.Failure("permission denied")

        val coordinator = createCoordinator(batchTickCount = 1)
        coordinator.recordGeneratedSteps(experimentId, 120)

        val events = repository.getHealthWriteEvents(experimentId)
        assertEquals(1, events.size)
        assertFalse(events.first().success)
        assertEquals("permission denied", events.first().errorMessage)
    }

    @Test
    fun `flush pending writes partial batch on stop`() = runTest {
        coEvery {
            healthConnectManager.writeSteps(any(), any(), any())
        } returns WriteResult.Success

        val coordinator = createCoordinator(batchTickCount = 3)
        coordinator.recordGeneratedSteps(experimentId, 50)
        coordinator.recordGeneratedSteps(experimentId, 60)

        coordinator.flushPending(experimentId)

        val events = repository.getHealthWriteEvents(experimentId)
        assertEquals(1, events.size)
        assertEquals(110, events.first().stepCount)
    }

    private fun createCoordinator(batchTickCount: Int): HealthWriteCoordinator {
        return HealthWriteCoordinator(
            healthConnectManager = healthConnectManager,
            repository = repository,
            batchTickCount = batchTickCount,
            currentInstant = {
                val value = now
                now = now.plusSeconds(1)
                value
            },
        )
    }
}
