package com.pixson.autofit.ui

import com.pixson.autofit.data.health.HealthConnectManager
import com.pixson.autofit.data.health.HealthSdkStatus
import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.data.repo.ExperimentRepository
import com.pixson.autofit.domain.ExperimentController
import com.pixson.autofit.domain.model.ExperimentConfig
import com.pixson.autofit.domain.model.ExperimentStatus
import com.pixson.autofit.system.PermissionManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ExperimentViewModelTest {

    private val repository: ExperimentRepository = mockk(relaxed = true)
    private val experimentController: ExperimentController = mockk(relaxed = true)
    private val healthConnectManager: HealthConnectManager = mockk()
    private val permissionManager: PermissionManager = mockk()

    private val fixedNow = Instant.parse("2026-06-07T12:00:00Z")

    @Test
    fun `default batch minutes is 1`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(ExperimentConfig.DEFAULT_BATCH_MINUTES, viewModel.configState.value.batchMinutes)
    }

    @Test
    fun `updateBatchMinutes accepts 1 3 and 5`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateBatchMinutes(3)
        assertEquals(3, viewModel.configState.value.batchMinutes)

        viewModel.updateBatchMinutes(5)
        assertEquals(5, viewModel.configState.value.batchMinutes)

        viewModel.updateBatchMinutes(99)
        assertEquals(5, viewModel.configState.value.batchMinutes)
    }

    @Test
    fun `invalid cadence disables start`() = runTest {
        val viewModel = createViewModel(healthReady = true)
        advanceUntilIdle()

        viewModel.updateTargetCadence("0")
        advanceUntilIdle()

        assertFalse(viewModel.configState.value.canStart)
        assertNotNull(viewModel.configState.value.validationMessage)
    }

    @Test
    fun `invalid duration disables start`() = runTest {
        val viewModel = createViewModel(healthReady = true)
        advanceUntilIdle()

        viewModel.updateDurationMinutes("0")
        advanceUntilIdle()

        assertFalse(viewModel.configState.value.canStart)
        assertNotNull(viewModel.configState.value.validationMessage)
    }

    @Test
    fun `health connect unavailable disables start`() = runTest {
        coEvery { healthConnectManager.getSdkStatus() } returns HealthSdkStatus.Unavailable(
            reason = "Health Connect is not available on this device",
            rawStatus = 1,
        )
        coEvery { healthConnectManager.hasWritePermission() } returns false

        val viewModel = createViewModel(healthReady = false, skipDefaultHealthMocks = true)
        advanceUntilIdle()

        assertFalse(viewModel.configState.value.canStart)
        assertTrue(viewModel.configState.value.healthConnectStatus.contains("not available"))
    }

    @Test
    fun `start returns Started when permissions are ready`() = runTest {
        val experimentId = UUID.randomUUID()
        val experimentFlow = MutableStateFlow(runningExperiment(experimentId))

        coEvery { experimentController.createExperiment(any()) } returns experimentId
        every { permissionManager.canStartHealthForegroundService() } returns true
        every { experimentController.startExperiment(experimentId) } just runs
        every { repository.observeExperiment(experimentId) } returns experimentFlow
        every { repository.observeHeartbeats(experimentId) } returns flowOf(emptyList())
        every { repository.observeHealthWriteEvents(experimentId) } returns flowOf(emptyList())

        val viewModel = createViewModel(healthReady = true)
        advanceUntilIdle()

        val outcome = viewModel.onStartClicked()
        advanceUntilIdle()

        assertTrue(outcome is StartExperimentOutcome.Started)
        assertEquals(experimentId, (outcome as StartExperimentOutcome.Started).experimentId)
        assertEquals(experimentId, viewModel.activeExperimentId.value)
        verify { experimentController.startExperiment(experimentId) }
    }

    @Test
    fun `start returns NeedsActivityRecognition when FGS permission missing`() = runTest {
        val experimentId = UUID.randomUUID()

        coEvery { experimentController.createExperiment(any()) } returns experimentId
        every { permissionManager.canStartHealthForegroundService() } returns false
        every { repository.observeExperiment(experimentId) } returns flowOf(runningExperiment(experimentId))
        every { repository.observeHeartbeats(experimentId) } returns flowOf(emptyList())
        every { repository.observeHealthWriteEvents(experimentId) } returns flowOf(emptyList())

        val viewModel = createViewModel(healthReady = true)
        advanceUntilIdle()

        val outcome = viewModel.onStartClicked()
        advanceUntilIdle()

        assertTrue(outcome is StartExperimentOutcome.NeedsActivityRecognition)
        assertEquals(experimentId, viewModel.activeExperimentId.value)
        verify(exactly = 0) { experimentController.startExperiment(any()) }
    }

    @Test
    fun `stop delegates to experiment controller`() = runTest {
        val experimentId = UUID.randomUUID()
        val experimentFlow = MutableStateFlow(runningExperiment(experimentId))

        every { repository.observeExperiment(experimentId) } returns experimentFlow
        every { repository.observeHeartbeats(experimentId) } returns flowOf(emptyList())
        every { repository.observeHealthWriteEvents(experimentId) } returns flowOf(emptyList())
        every { experimentController.stopExperiment(experimentId) } just runs

        val viewModel = createViewModel(healthReady = true)
        advanceUntilIdle()

        viewModel.onActivityRecognitionGranted(experimentId)
        advanceUntilIdle()

        viewModel.stopExperiment()
        verify { experimentController.stopExperiment(experimentId) }
    }

    @Test
    fun `dismissRunningSession clears active experiment`() = runTest {
        val experimentId = UUID.randomUUID()

        every { repository.observeExperiment(experimentId) } returns flowOf(runningExperiment(experimentId))
        every { repository.observeHeartbeats(experimentId) } returns flowOf(emptyList())
        every { repository.observeHealthWriteEvents(experimentId) } returns flowOf(emptyList())

        val viewModel = createViewModel(healthReady = true)
        advanceUntilIdle()

        viewModel.onActivityRecognitionGranted(experimentId)
        advanceUntilIdle()

        viewModel.dismissRunningSession()

        assertNull(viewModel.activeExperimentId.value)
        assertNull(viewModel.runningState.value)
    }

    private fun createViewModel(
        healthReady: Boolean = false,
        skipDefaultHealthMocks: Boolean = false,
    ): ExperimentViewModel {
        if (!skipDefaultHealthMocks) {
            if (healthReady) {
                coEvery { healthConnectManager.getSdkStatus() } returns HealthSdkStatus.Available
                coEvery { healthConnectManager.hasWritePermission() } returns true
            } else {
                coEvery { healthConnectManager.getSdkStatus() } returns HealthSdkStatus.Available
                coEvery { healthConnectManager.hasWritePermission() } returns false
            }
        }
        coEvery { repository.getRunningExperiment() } returns null

        return ExperimentViewModel(
            repository = repository,
            experimentController = experimentController,
            healthConnectManager = healthConnectManager,
            permissionManager = permissionManager,
            currentInstant = { fixedNow },
        )
    }

    private fun runningExperiment(id: UUID): ExperimentEntity = ExperimentEntity(
        id = id,
        startTime = fixedNow,
        durationMinutes = 5,
        targetCadence = 120,
        randomRange = 15,
        batchMinutes = 1,
        status = ExperimentStatus.RUNNING,
    )
}
