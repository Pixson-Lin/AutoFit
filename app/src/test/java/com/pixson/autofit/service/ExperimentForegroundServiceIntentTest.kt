package com.pixson.autofit.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ExperimentForegroundServiceIntentTest {

    @Test
    fun `start intent carries experiment id`() {
        val context = RuntimeEnvironment.getApplication()
        val experimentId = UUID.fromString("00000000-0000-0000-0000-000000000099")

        val intent = ExperimentForegroundService.startIntent(context, experimentId)

        assertEquals(experimentId.toString(), intent.getStringExtra(ServiceConstants.EXTRA_EXPERIMENT_ID))
    }

    @Test
    fun `stop intent carries action and experiment id`() {
        val context = RuntimeEnvironment.getApplication()
        val experimentId = UUID.fromString("00000000-0000-0000-0000-000000000099")

        val intent = ExperimentForegroundService.stopIntent(context, experimentId)

        assertEquals(ServiceConstants.ACTION_STOP, intent.action)
        assertNotNull(intent.getStringExtra(ServiceConstants.EXTRA_EXPERIMENT_ID))
    }
}
