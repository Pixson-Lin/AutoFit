package com.pixson.autofit.data.local

import androidx.room.Room
import com.pixson.autofit.data.local.entity.HeartbeatEntity
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
class HeartbeatDaoTest {

    private lateinit var database: AppDatabase
    private val experimentId = UUID.fromString("00000000-0000-0000-0000-000000000001")

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
    fun insertAndQueryByExperimentId() = runTest {
        val heartbeat = HeartbeatEntity(
            id = UUID.randomUUID(),
            experimentId = experimentId,
            timestamp = Instant.parse("2026-06-06T10:01:00Z"),
            generatedSteps = 118,
            batteryLevel = 90,
            screenOn = false,
            charging = true,
        )
        database.heartbeatDao().insert(heartbeat)

        val loaded = database.heartbeatDao().getByExperimentId(experimentId)
        assertEquals(1, loaded.size)
        assertEquals(heartbeat, loaded.first())
    }

    @Test
    fun observeByExperimentIdEmitsList() = runTest {
        val heartbeat = HeartbeatEntity(
            id = UUID.randomUUID(),
            experimentId = experimentId,
            timestamp = Instant.parse("2026-06-06T10:02:00Z"),
            generatedSteps = 121,
            batteryLevel = 88,
            screenOn = true,
            charging = false,
        )
        database.heartbeatDao().insert(heartbeat)

        val observed = database.heartbeatDao().observeByExperimentId(experimentId).first()
        assertEquals(1, observed.size)
    }

    @Test
    fun bulkInsertAndCount() = runTest {
        val heartbeats = (1..5).map { index ->
            HeartbeatEntity(
                id = UUID.randomUUID(),
                experimentId = experimentId,
                timestamp = Instant.parse("2026-06-06T10:0${index}:00Z"),
                generatedSteps = 100 + index,
                batteryLevel = 80,
                screenOn = true,
                charging = false,
            )
        }
        database.heartbeatDao().insertAll(heartbeats)

        assertEquals(5, database.heartbeatDao().countByExperimentId(experimentId))
    }
}
