package com.pixson.autofit.data.local

import androidx.room.Room
import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.domain.model.ExperimentStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class ExperimentDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryExperiment() = runTest {
        val experiment = sampleExperiment()
        database.experimentDao().insert(experiment)

        val loaded = database.experimentDao().getById(experiment.id)
        assertNotNull(loaded)
        assertEquals(experiment, loaded)
    }

    @Test
    fun observeExperimentEmitsUpdates() = runTest {
        val experiment = sampleExperiment()
        database.experimentDao().insert(experiment)

        val observed = database.experimentDao().observeById(experiment.id).first()
        assertEquals(experiment, observed)
    }

    @Test
    fun updateStatus() = runTest {
        val experiment = sampleExperiment(status = ExperimentStatus.RUNNING)
        database.experimentDao().insert(experiment)

        database.experimentDao().updateStatus(experiment.id, ExperimentStatus.COMPLETED)

        val updated = database.experimentDao().getById(experiment.id)
        assertEquals(ExperimentStatus.COMPLETED, updated?.status)
    }

    @Test
    fun getByStatusReturnsMatchingExperiment() = runTest {
        val running = sampleExperiment(status = ExperimentStatus.RUNNING)
        val completed = sampleExperiment(
            id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
            status = ExperimentStatus.COMPLETED,
        )
        database.experimentDao().insert(running)
        database.experimentDao().insert(completed)

        val found = database.experimentDao().getByStatus(ExperimentStatus.RUNNING)
        assertEquals(running.id, found?.id)
    }

    private fun sampleExperiment(
        id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        status: ExperimentStatus = ExperimentStatus.PENDING,
    ) = ExperimentEntity(
        id = id,
        startTime = Instant.parse("2026-06-06T10:00:00Z"),
        durationMinutes = 60,
        targetCadence = 120,
        randomRange = 15,
        status = status,
    )
}
