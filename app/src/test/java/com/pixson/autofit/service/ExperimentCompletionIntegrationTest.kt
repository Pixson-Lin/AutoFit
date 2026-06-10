package com.pixson.autofit.service

import androidx.room.Room
import com.pixson.autofit.data.health.HealthConnectManager
import com.pixson.autofit.data.health.HealthSdkStatus
import com.pixson.autofit.data.health.WriteResult
import com.pixson.autofit.data.local.AppDatabase
import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.ResultAggregator
import com.pixson.autofit.domain.model.ExperimentStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
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

/**
 * FR-008: duration reached → COMPLETED status + ExperimentResult persisted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ExperimentCompletionIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ExperimentRepository
    private val experimentId = UUID.fromString("00000000-0000-0000-0000-000000000062")
    private val startTime = Instant.parse("2026-06-07T12:00:00Z")

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
    fun `duration completion finalizes with COMPLETED status and result`() = runBlocking {
        repository.insertExperiment(
            ExperimentEntity(
                id = experimentId,
                startTime = startTime,
                durationMinutes = 1,
                targetCadence = 120,
                randomRange = 15,
                batchMinutes = 1,
                status = ExperimentStatus.RUNNING,
            ),
        )

        val healthConnectManager = mockk<HealthConnectManager>()
        coEvery { healthConnectManager.getSdkStatus() } returns HealthSdkStatus.Available
        coEvery { healthConnectManager.hasWritePermission() } returns true
        coEvery { healthConnectManager.writeStepsBatch(any()) } returns WriteResult.Success

        val clock = object {
            var instant = startTime
            fun currentInstant(): Instant = instant
            fun advanceMinutes(minutes: Long) {
                instant = instant.plusSeconds(minutes * 60)
            }
        }

        val coordinator = HealthWriteCoordinator(
            healthConnectManager = healthConnectManager,
            repository = repository,
            currentInstant = clock::currentInstant,
        )
        val runner = ExperimentLoopRunner(
            repository = repository,
            healthWriteCoordinator = coordinator,
            deviceStateSource = FakeDeviceStateSource(),
            wakeLockGateway = NoOpWakeLockGateway(),
            tickIntervalMs = 60_000L,
            elapsedRealtime = { 0L },
            currentInstant = clock::currentInstant,
            delayFn = { clock.advanceMinutes(1) },
        )

        val exitReason = runner.run(experimentId, isStopped = { false })
        assertEquals(LoopExitReason.COMPLETED, exitReason)

        val endTime = clock.currentInstant()
        val finalizer = ExperimentFinalizer(
            repository = repository,
            resultAggregator = ResultAggregator(),
            healthWriteCoordinator = coordinator,
        )
        finalizer.finalize(
            experimentId = experimentId,
            terminalStatus = ExperimentStatus.COMPLETED,
            endTime = endTime,
            flushPending = true,
        )

        assertEquals(ExperimentStatus.COMPLETED, repository.getExperiment(experimentId)?.status)
        val result = repository.getResult(experimentId)
        assertNotNull(result)
        assertEquals(2, result?.heartbeatCount)
        assertEquals(1, result?.writeSuccessCount)
    }
}

private class FakeDeviceStateSource : DeviceStateSource {
    override fun read(): DeviceState = DeviceState(
        batteryLevel = 80,
        screenOn = true,
        charging = false,
    )
}

private class NoOpWakeLockGateway : WakeLockGateway {
    override fun acquire() = Unit
    override fun release() = Unit
}
