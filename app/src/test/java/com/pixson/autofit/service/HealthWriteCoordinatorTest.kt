package com.pixson.autofit.service

import androidx.room.Room
import com.pixson.autofit.data.health.HealthConnectManager
import com.pixson.autofit.data.health.HealthSdkStatus
import com.pixson.autofit.data.health.StepRecordEntry
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
    private val experimentStart = Instant.parse("2026-06-06T10:00:00Z")
    private var now = Instant.parse("2026-06-06T10:05:00Z")

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
        coEvery { healthConnectManager.writeStepsBatch(any()) } returns WriteResult.Success

        val coordinator = createCoordinator()

        coordinator.recordGeneratedSteps(experimentId, 100, tickIndex = 1, experimentStartTime = experimentStart, batchMinutes = 3)
        coordinator.recordGeneratedSteps(experimentId, 110, tickIndex = 2, experimentStartTime = experimentStart, batchMinutes = 3)
        assertEquals(0, repository.getHealthWriteEvents(experimentId).size)

        coordinator.recordGeneratedSteps(experimentId, 90, tickIndex = 3, experimentStartTime = experimentStart, batchMinutes = 3)

        coVerify(exactly = 1) {
            healthConnectManager.writeStepsBatch(
                listOf(
                    StepRecordEntry(100, Instant.parse("2026-06-06T10:00:00Z"), Instant.parse("2026-06-06T10:01:00Z")),
                    StepRecordEntry(110, Instant.parse("2026-06-06T10:01:00Z"), Instant.parse("2026-06-06T10:02:00Z")),
                    StepRecordEntry(90, Instant.parse("2026-06-06T10:02:00Z"), Instant.parse("2026-06-06T10:03:00Z")),
                ),
            )
        }

        val events = repository.getHealthWriteEvents(experimentId)
        assertEquals(3, events.size)
        assertEquals(300, events.sumOf { it.stepCount })
        assertTrue(events.all { it.success })
        assertEquals(Instant.parse("2026-06-06T10:00:00Z"), events.first().recordStart)
        assertEquals(Instant.parse("2026-06-06T10:03:00Z"), events.last().recordEnd)
    }

    @Test
    fun `each minute uses distinct one minute window`() = runTest {
        coEvery { healthConnectManager.writeStepsBatch(any()) } returns WriteResult.Success

        val coordinator = createCoordinator()
        coordinator.recordGeneratedSteps(experimentId, 120, tickIndex = 1, experimentStartTime = experimentStart, batchMinutes = 1)
        coordinator.recordGeneratedSteps(experimentId, 130, tickIndex = 2, experimentStartTime = experimentStart, batchMinutes = 1)

        coVerify {
            healthConnectManager.writeStepsBatch(
                listOf(StepRecordEntry(120, Instant.parse("2026-06-06T10:00:00Z"), Instant.parse("2026-06-06T10:01:00Z"))),
            )
            healthConnectManager.writeStepsBatch(
                listOf(StepRecordEntry(130, Instant.parse("2026-06-06T10:01:00Z"), Instant.parse("2026-06-06T10:02:00Z"))),
            )
        }
    }

    @Test
    fun `failed write still records event and loop can continue`() = runTest {
        coEvery { healthConnectManager.writeStepsBatch(any()) } returns WriteResult.Failure("permission denied")

        val coordinator = createCoordinator()
        coordinator.recordGeneratedSteps(experimentId, 120, tickIndex = 1, experimentStartTime = experimentStart, batchMinutes = 1)

        val events = repository.getHealthWriteEvents(experimentId)
        assertEquals(1, events.size)
        assertFalse(events.first().success)
        assertEquals("permission denied", events.first().errorMessage)
    }

    @Test
    fun `batch five flushes only when tick index is divisible by five`() = runTest {
        coEvery { healthConnectManager.writeStepsBatch(any()) } returns WriteResult.Success

        val coordinator = createCoordinator()
        (1..4).forEach { tick ->
            coordinator.recordGeneratedSteps(
                experimentId,
                100 + tick,
                tickIndex = tick,
                experimentStartTime = experimentStart,
                batchMinutes = 5,
            )
        }
        assertEquals(0, repository.getHealthWriteEvents(experimentId).size)

        coordinator.recordGeneratedSteps(
            experimentId,
            200,
            tickIndex = 5,
            experimentStartTime = experimentStart,
            batchMinutes = 5,
        )

        coVerify(exactly = 1) { healthConnectManager.writeStepsBatch(any()) }
        assertEquals(5, repository.getHealthWriteEvents(experimentId).size)
        assertEquals(610, repository.getHealthWriteEvents(experimentId).sumOf { it.stepCount })
    }

    @Test
    fun `flush pending writes buffered minutes as a single batch`() = runTest {
        coEvery { healthConnectManager.writeStepsBatch(any()) } returns WriteResult.Success

        val coordinator = createCoordinator()
        coordinator.recordGeneratedSteps(experimentId, 50, tickIndex = 1, experimentStartTime = experimentStart, batchMinutes = 3)
        coordinator.recordGeneratedSteps(experimentId, 60, tickIndex = 2, experimentStartTime = experimentStart, batchMinutes = 3)

        coordinator.flushPending(experimentId)

        coVerify(exactly = 1) {
            healthConnectManager.writeStepsBatch(
                listOf(
                    StepRecordEntry(50, Instant.parse("2026-06-06T10:00:00Z"), Instant.parse("2026-06-06T10:01:00Z")),
                    StepRecordEntry(60, Instant.parse("2026-06-06T10:01:00Z"), Instant.parse("2026-06-06T10:02:00Z")),
                ),
            )
        }
        assertEquals(2, repository.getHealthWriteEvents(experimentId).size)
    }

    private fun createCoordinator(): HealthWriteCoordinator {
        return HealthWriteCoordinator(
            healthConnectManager = healthConnectManager,
            repository = repository,
            currentInstant = {
                val value = now
                now = now.plusSeconds(1)
                value
            },
        )
    }
}
