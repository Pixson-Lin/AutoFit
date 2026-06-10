package com.pixson.autofit.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.pixson.autofit.AutoFitApplication
import com.pixson.autofit.ui.environment.EnvironmentViewModelFactory
import com.pixson.autofit.ui.history.HistoryViewModelFactory
import com.pixson.autofit.ui.navigation.AutoFitNavHost
import com.pixson.autofit.ui.theme.AutoFitTheme
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {

    private var pendingFgsExperimentId: UUID? = null
    private lateinit var viewModelFactory: ExperimentViewModelFactory

    private val requestHealthPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        if (granted.isNotEmpty()) {
            refreshHealthConnectFromActivity()
        }
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        // Notification permission is optional for starting; no follow-up required.
    }

    private val requestActivityRecognition = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val pendingId = pendingFgsExperimentId
        pendingFgsExperimentId = null
        if (granted && pendingId != null) {
            viewModelHolder?.onActivityRecognitionGranted(pendingId)
        } else if (!granted) {
            viewModelHolder?.onActivityRecognitionDenied()
        }
    }

    /** Set by [AutoFitNavHost] so permission callbacks can reach the active ViewModel. */
    var viewModelHolder: ExperimentViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AutoFitApplication
        viewModelFactory = ExperimentViewModelFactory(
            repository = app.experimentRepository,
            experimentController = app.experimentController,
            healthConnectManager = app.healthConnectManager,
            permissionManager = app.permissionManager,
        )

        setContent {
            AutoFitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AutoFitNavHost(
                        viewModelFactory = viewModelFactory,
                        historyViewModelFactory = HistoryViewModelFactory(
                            repository = app.experimentRepository,
                        ),
                        environmentViewModelFactory = EnvironmentViewModelFactory(
                            appContext = applicationContext,
                            permissionManager = app.permissionManager,
                        ),
                        settingsNavigator = app.settingsNavigator,
                        onRequestHealthPermissions = {
                            requestHealthPermissions.launch(
                                app.permissionManager.requiredHealthConnectPermissions(),
                            )
                        },
                        onRequestActivityRecognition = {
                            requestActivityRecognition.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        },
                        onRequestNotificationPermission = {
                            if (!app.permissionManager.isNotificationPermissionGranted()) {
                                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onNeedsActivityRecognition = { experimentId ->
                            pendingFgsExperimentId = experimentId
                            requestActivityRecognition.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        },
                        onViewModelReady = { viewModel ->
                            viewModelHolder = viewModel
                        },
                    )
                }
            }
        }
    }

    private fun refreshHealthConnectFromActivity() {
        lifecycleScope.launch {
            viewModelHolder?.refreshHealthConnectStatus()
        }
    }
}
