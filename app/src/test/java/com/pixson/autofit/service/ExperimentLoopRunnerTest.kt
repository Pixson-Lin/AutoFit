package com.pixson.autofit.service

import androidx.room.Room
import com.pixson.autofit.data.local.AppDatabase
import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.data.local.entity.HeartbeatEntity
import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.model.ExperimentStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class ExperimentLoopRunnerTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ExperimentRepository
    private lateinit var clock: FakeMonotonicClock
    private lateinit var wakeLock: CountingWakeLockGateway

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
        clock = FakeMonotonicClock()
        wakeLock = CountingWakeLockGateway()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `short experiment records heartbeats and completes`() = runBlocking {
        val experimentId = insertRunningExperiment(durationMinutes = 1)
        val runner = createRunner(tickIntervalMs = 50L)

        runner.run(experimentId, isStopped = { false })

        val heartbeats = repository.getHeartbeats(experimentId)
        assertTrue(heartbeats.size >= 2)
        assertEquals(ExperimentStatus.COMPLETED, repository.getExperiment(experimentId)?.status)
        assertEquals(heartbeats.size, wakeLock.acquireCount)
        assertEquals(heartbeats.size, wakeLock.releaseCount)
    }

    @Test
    fun `recovery continues heartbeat sequence after prior records`() = runBlocking {
        val experimentId = insertRunningExperiment(durationMinutes = 5)
        repository.insertHeartbeat(
            HeartbeatEntity(
                id = UUID.randomUUID(),
                experimentId = experimentId,
                timestamp = Instant.parse("2026-06-06T10:00:00Z"),
                generatedSteps = 110,
                batteryLevel = 70,
                screenOn = true,
                charging = false,
            ),
        )

        val runner = createRunner(tickIntervalMs = 10L)
        var ticks = 0
        runner.run(
            experimentId = experimentId,
            isStopped = {
                ticks++
                ticks >= 2
            },
        )

        val heartbeats = repository.getHeartbeats(experimentId)
        assertTrue(heartbeats.size >= 2)
    }

    @Test
    fun `stop flag ends loop without completion status`() = runBlocking {
        val experimentId = insertRunningExperiment(durationMinutes = 10)
        val runner = createRunner(tickIntervalMs = 10L)
        var ticks = 0

        runner.run(
            experimentId = experimentId,
            isStopped = {
                ticks++
                ticks > 2
            },
        )

        assertEquals(ExperimentStatus.RUNNING, repository.getExperiment(experimentId)?.status)
        assertEquals(2, repository.getHeartbeats(experimentId).size)
    }

    private fun createRunner(tickIntervalMs: Long): ExperimentLoopRunner {
        return ExperimentLoopRunner(
            repository = repository,
            deviceStateSource = FakeDeviceStateReader(),
            wakeLockGateway = wakeLock,
            tickIntervalMs = tickIntervalMs,
            elapsedRealtime = clock::elapsedRealtime,
            currentInstant = clock::currentInstant,
            delayFn = { ms -> clock.advance(ms) },
        )
    }

    private suspend fun insertRunningExperiment(durationMinutes: Int): UUID {
        val experimentId = UUID.fromString("00000000-0000-0000-0000-000000000010")
        repository.insertExperiment(
            ExperimentEntity(
                id = experimentId,
                startTime = clock.currentInstant(),
                durationMinutes = durationMinutes,
                targetCadence = 120,
                randomRange = 15,
                status = ExperimentStatus.RUNNING,
            ),
        )
        return experimentId
    }
}
