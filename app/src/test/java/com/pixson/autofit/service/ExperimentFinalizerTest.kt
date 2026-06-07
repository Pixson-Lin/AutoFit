package com.pixson.autofit.service

import androidx.room.Room
import com.pixson.autofit.data.health.HealthConnectManager
import com.pixson.autofit.data.health.HealthSdkStatus
import com.pixson.autofit.data.health.WriteResult
import com.pixson.autofit.data.local.AppDatabase
import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.data.local.entity.HeartbeatEntity
import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.ResultAggregator
import com.pixson.autofit.domain.model.ExperimentStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ExperimentFinalizerTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ExperimentRepository
    private val experimentId = UUID.fromString("00000000-0000-0000-0000-000000000050")
    private val startTime = Instant.parse("2026-06-06T10:00:00Z")
    private val endTime = Instant.parse("2026-06-06T10:03:00Z")

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
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `finalize flushes pending writes aggregates result and updates status`() = runTest {
        repository.insertExperiment(
            ExperimentEntity(
                id = experimentId,
                startTime = startTime,
                durationMinutes = 5,
                targetCadence = 120,
                randomRange = 15,
                batchMinutes = 3,
                status = ExperimentStatus.RUNNING,
            ),
        )
        repository.insertHeartbeat(
            HeartbeatEntity(
                id = UUID.randomUUID(),
                experimentId = experimentId,
                timestamp = startTime.plusSeconds(60),
                generatedSteps = 110,
                batteryLevel = 80,
                screenOn = true,
                charging = false,
            ),
        )

        val healthConnectManager = mockk<HealthConnectManager>()
        coEvery { healthConnectManager.getSdkStatus() } returns HealthSdkStatus.Available
        coEvery { healthConnectManager.hasWritePermission() } returns true
        coEvery { healthConnectManager.writeStepsBatch(any()) } returns WriteResult.Success

        val coordinator = HealthWriteCoordinator(
            healthConnectManager = healthConnectManager,
            repository = repository,
            currentInstant = { endTime },
        )
        coordinator.recordGeneratedSteps(
            experimentId = experimentId,
            steps = 70,
            tickIndex = 1,
            experimentStartTime = startTime,
            batchMinutes = 3,
        )
        coordinator.recordGeneratedSteps(
            experimentId = experimentId,
            steps = 80,
            tickIndex = 2,
            experimentStartTime = startTime,
            batchMinutes = 3,
        )

        val finalizer = ExperimentFinalizer(
            repository = repository,
            resultAggregator = ResultAggregator(),
            healthWriteCoordinator = coordinator,
        )
        finalizer.finalize(experimentId, ExperimentStatus.COMPLETED, endTime, flushPending = true)

        assertEquals(ExperimentStatus.COMPLETED, repository.getExperiment(experimentId)?.status)
        val result = repository.getResult(experimentId)
        assertNotNull(result)
        assertEquals(150, result?.totalSteps)
        assertEquals(2, result?.writeSuccessCount)
        assertEquals(1, result?.heartbeatCount)
    }

    @Test
    fun `manual stop does not flush pending batch`() = runTest {
        repository.insertExperiment(
            ExperimentEntity(
                id = experimentId,
                startTime = startTime,
                durationMinutes = 5,
                targetCadence = 120,
                randomRange = 15,
                batchMinutes = 3,
                status = ExperimentStatus.RUNNING,
            ),
        )

        val healthConnectManager = mockk<HealthConnectManager>()
        coEvery { healthConnectManager.getSdkStatus() } returns HealthSdkStatus.Available
        coEvery { healthConnectManager.hasWritePermission() } returns true
        coEvery { healthConnectManager.writeStepsBatch(any()) } returns WriteResult.Success

        val coordinator = HealthWriteCoordinator(
            healthConnectManager = healthConnectManager,
            repository = repository,
            currentInstant = { endTime },
        )
        coordinator.recordGeneratedSteps(
            experimentId = experimentId,
            steps = 70,
            tickIndex = 1,
            experimentStartTime = startTime,
            batchMinutes = 3,
        )

        val finalizer = ExperimentFinalizer(
            repository = repository,
            resultAggregator = ResultAggregator(),
            healthWriteCoordinator = coordinator,
        )
        finalizer.finalize(experimentId, ExperimentStatus.STOPPED, endTime, flushPending = false)

        coVerify(exactly = 0) { healthConnectManager.writeStepsBatch(any()) }
        assertEquals(ExperimentStatus.STOPPED, repository.getExperiment(experimentId)?.status)
        assertEquals(0, repository.getHealthWriteEvents(experimentId).size)
        assertEquals(0, repository.getResult(experimentId)?.totalSteps)
    }
}
