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
        coEvery { healthConnectManager.writeSteps(150, any(), any()) } returns WriteResult.Success

        val coordinator = HealthWriteCoordinator(
            healthConnectManager = healthConnectManager,
            repository = repository,
            batchTickCount = 3,
            currentInstant = { endTime },
        )
        coordinator.recordGeneratedSteps(experimentId, 70)
        coordinator.recordGeneratedSteps(experimentId, 80)

        val finalizer = ExperimentFinalizer(
            repository = repository,
            resultAggregator = ResultAggregator(),
            healthWriteCoordinator = coordinator,
        )
        finalizer.finalize(experimentId, ExperimentStatus.COMPLETED, endTime)

        assertEquals(ExperimentStatus.COMPLETED, repository.getExperiment(experimentId)?.status)
        val result = repository.getResult(experimentId)
        assertNotNull(result)
        assertEquals(150, result?.totalSteps)
        assertEquals(1, result?.writeSuccessCount)
        assertEquals(1, result?.heartbeatCount)
    }
}
