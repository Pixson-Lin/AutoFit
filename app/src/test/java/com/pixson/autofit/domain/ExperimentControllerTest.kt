package com.pixson.autofit.domain

import androidx.room.Room
import com.pixson.autofit.data.env.EnvironmentInspector
import com.pixson.autofit.data.local.AppDatabase
import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.model.ExperimentConfig
import com.pixson.autofit.domain.model.ExperimentStatus
import com.pixson.autofit.domain.model.PermissionGrantState
import com.pixson.autofit.system.PermissionManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ExperimentControllerTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ExperimentRepository
    private lateinit var permissionManager: PermissionManager
    private lateinit var environmentInspector: EnvironmentInspector
    private lateinit var controller: ExperimentController

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

        permissionManager = mockk()
        every { permissionManager.isIgnoringBatteryOptimizations() } returns false
        every { permissionManager.isPowerSaveMode() } returns true
        every { permissionManager.notificationPermissionState() } returns PermissionGrantState.NOT_REQUIRED
        every { permissionManager.canStartHealthForegroundService() } returns true
        coEvery { permissionManager.healthConnectPermissionState() } returns PermissionGrantState.DENIED

        environmentInspector = EnvironmentInspector(
            context = RuntimeEnvironment.getApplication(),
            permissionManager = permissionManager,
        )
        controller = ExperimentController(
            appContext = RuntimeEnvironment.getApplication(),
            repository = repository,
            environmentInspector = environmentInspector,
            permissionManager = permissionManager,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `createExperiment persists RUNNING experiment and environment snapshot`() = runTest {
        val experimentId = controller.createExperiment(
            ExperimentConfig(
                targetCadence = 120,
                randomRange = 15,
                durationMinutes = 60,
            ),
        )

        val experiment = repository.getExperiment(experimentId)
        assertNotNull(experiment)
        assertEquals(ExperimentStatus.RUNNING, experiment?.status)
        assertEquals(120, experiment?.targetCadence)
        assertEquals(15, experiment?.randomRange)
        assertEquals(60, experiment?.durationMinutes)

        val snapshot = repository.getEnvironmentSnapshot(experimentId)
        assertNotNull(snapshot)
        assertEquals(experimentId, snapshot?.experimentId)
        assertEquals(PermissionGrantState.DENIED, snapshot?.healthConnectPermission)
        assertEquals(true, snapshot?.powerSaveMode)
    }
}
