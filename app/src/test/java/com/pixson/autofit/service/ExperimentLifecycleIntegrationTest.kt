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
import com.pixson.autofit.ui.history.HistoryMapper
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ExperimentLifecycleIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ExperimentRepository
    private val experimentId = UUID.fromString("00000000-0000-0000-0000-000000000072")
    private val startTime = Instant.parse("2026-06-07T10:00:00Z")

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
    fun `create run complete surfaces history list item`() = runBlocking {
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
            deviceStateSource = LifecycleFakeDeviceStateSource(),
            wakeLockGateway = LifecycleNoOpWakeLockGateway(),
            tickIntervalMs = 60_000L,
            elapsedRealtime = { 0L },
            currentInstant = clock::currentInstant,
            delayFn = { clock.advanceMinutes(1) },
        )

        val exitReason = runner.run(experimentId, isStopped = { false })
        assertEquals(LoopExitReason.COMPLETED, exitReason)

        val finalizer = ExperimentFinalizer(
            repository = repository,
            resultAggregator = ResultAggregator(),
            healthWriteCoordinator = coordinator,
        )
        finalizer.finalize(
            experimentId = experimentId,
            terminalStatus = ExperimentStatus.COMPLETED,
            endTime = clock.currentInstant(),
            flushPending = true,
        )

        val experiment = repository.getExperiment(experimentId)!!
        val result = repository.getResult(experimentId)!!
        val historyItem = HistoryMapper.toListItem(experiment, result)

        assertEquals(ExperimentStatus.COMPLETED, historyItem.status)
        assertNotNull(historyItem.endTimeLabel)
        assertEquals("100%", historyItem.successRateLabel)
    }
}

private class LifecycleFakeDeviceStateSource : DeviceStateSource {
    override fun read(): DeviceState = DeviceState(
        batteryLevel = 80,
        screenOn = true,
        charging = false,
    )
}

private class LifecycleNoOpWakeLockGateway : WakeLockGateway {
    override fun acquire() = Unit
    override fun release() = Unit
}
