package com.pixson.autofit.data.repo

import androidx.room.Room
import com.pixson.autofit.data.local.AppDatabase
import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.data.local.entity.HeartbeatEntity
import com.pixson.autofit.domain.model.ExperimentStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ExperimentRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ExperimentRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
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
    fun insertAndObserveExperiment() = runTest {
        val experiment = ExperimentEntity(
            id = UUID.randomUUID(),
            startTime = Instant.parse("2026-06-06T10:00:00Z"),
            durationMinutes = 30,
            targetCadence = 100,
            randomRange = 10,
            status = ExperimentStatus.RUNNING,
        )
        repository.insertExperiment(experiment)

        val observed = repository.observeExperiment(experiment.id).first()
        assertEquals(experiment, observed)
    }

    @Test
    fun insertHeartbeatAndQuery() = runTest {
        val experimentId = UUID.randomUUID()
        val heartbeat = HeartbeatEntity(
            id = UUID.randomUUID(),
            experimentId = experimentId,
            timestamp = Instant.parse("2026-06-06T10:01:00Z"),
            generatedSteps = 115,
            batteryLevel = 75,
            screenOn = false,
            charging = false,
        )
        repository.insertHeartbeat(heartbeat)

        val heartbeats = repository.getHeartbeats(experimentId)
        assertEquals(1, heartbeats.size)
        assertEquals(115, heartbeats.first().generatedSteps)
    }
}
