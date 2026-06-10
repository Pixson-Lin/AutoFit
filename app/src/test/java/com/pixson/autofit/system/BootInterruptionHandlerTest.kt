package com.pixson.autofit.system

import androidx.room.Room
import com.pixson.autofit.data.local.AppDatabase
import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.data.local.entity.HeartbeatEntity
import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.ResultAggregator
import com.pixson.autofit.domain.model.ExperimentStatus
import com.pixson.autofit.service.ExperimentFinalizer
import com.pixson.autofit.service.HealthWriteCoordinator
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class BootInterruptionHandlerTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ExperimentRepository
    private lateinit var handler: BootInterruptionHandler

    private val experimentId = UUID.fromString("00000000-0000-0000-0000-000000000071")
    private val startTime = Instant.parse("2026-06-07T08:00:00Z")
    private val staleHeartbeatTime = Instant.parse("2026-06-07T08:05:00Z")
    private val bootTime = Instant.parse("2026-06-07T09:00:00Z")

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
        val finalizer = ExperimentFinalizer(
            repository = repository,
            resultAggregator = ResultAggregator(),
            healthWriteCoordinator = HealthWriteCoordinator(
                healthConnectManager = mockk(relaxed = true),
                repository = repository,
                currentInstant = { bootTime },
            ),
        )
        handler = BootInterruptionHandler(
            repository = repository,
            experimentFinalizer = finalizer,
            currentInstant = { bootTime },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `handleBootCompleted no-op when no running experiment`() = runTest {
        handler.handleBootCompleted()
        assertNull(repository.getRunningExperiment())
    }

    @Test
    fun `handleBootCompleted marks stale running experiment interrupted by reboot`() = runTest {
        repository.insertExperiment(
            ExperimentEntity(
                id = experimentId,
                startTime = startTime,
                durationMinutes = 60,
                targetCadence = 120,
                randomRange = 15,
                batchMinutes = 1,
                status = ExperimentStatus.RUNNING,
            ),
        )
        repository.insertHeartbeat(
            HeartbeatEntity(
                id = UUID.randomUUID(),
                experimentId = experimentId,
                timestamp = staleHeartbeatTime,
                generatedSteps = 110,
                batteryLevel = 80,
                screenOn = false,
                charging = false,
            ),
        )

        handler.handleBootCompleted()

        assertEquals(
            ExperimentStatus.INTERRUPTED_BY_REBOOT,
            repository.getExperiment(experimentId)?.status,
        )
        val result = repository.getResult(experimentId)
        assertNotNull(result)
        assertEquals(1, result?.heartbeatCount)
    }

    @Test
    fun `handleBootCompleted skips when heartbeat is recent`() = runTest {
        repository.insertExperiment(
            ExperimentEntity(
                id = experimentId,
                startTime = startTime,
                durationMinutes = 60,
                targetCadence = 120,
                randomRange = 15,
                batchMinutes = 1,
                status = ExperimentStatus.RUNNING,
            ),
        )
        repository.insertHeartbeat(
            HeartbeatEntity(
                id = UUID.randomUUID(),
                experimentId = experimentId,
                timestamp = bootTime.minusSeconds(30),
                generatedSteps = 110,
                batteryLevel = 80,
                screenOn = true,
                charging = false,
            ),
        )

        handler.handleBootCompleted()

        assertEquals(ExperimentStatus.RUNNING, repository.getExperiment(experimentId)?.status)
        assertNull(repository.getResult(experimentId))
    }
}
