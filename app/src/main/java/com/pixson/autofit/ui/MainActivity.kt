package com.pixson.autofit.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.pixson.autofit.AutoFitApplication
import com.pixson.autofit.data.health.HealthSdkStatus
import com.pixson.autofit.data.health.WriteResult
import com.pixson.autofit.domain.model.ExperimentConfig
import com.pixson.autofit.ui.theme.AutoFitTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class MainActivity : ComponentActivity() {

    private var pendingFgsExperimentId: UUID? = null

    private val requestHealthPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        statusMessage = if (granted.isNotEmpty()) {
            "Health Connect permissions updated."
        } else {
            "Health Connect permissions not granted."
        }
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        statusMessage = if (granted) {
            "Notification permission granted."
        } else {
            "Notification permission denied."
        }
    }

    private val requestActivityRecognition = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val app = application as AutoFitApplication
        val pendingId = pendingFgsExperimentId
        pendingFgsExperimentId = null
        statusMessage = if (granted) {
            if (pendingId != null) {
                app.experimentController.startExperiment(pendingId)
                "FGS started: $pendingId"
            } else {
                "Activity recognition granted (required for health FGS on Android 14+)."
            }
        } else {
            "Activity recognition denied. Health FGS cannot start on Android 14+."
        }
    }

    private var statusMessage by mutableStateOf("Sprint 3 — Foreground Service core loop")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AutoFitApplication

        fun requestFgsPrerequisites(experimentId: UUID) {
            if (!app.permissionManager.canStartHealthForegroundService()) {
                pendingFgsExperimentId = experimentId
                requestActivityRecognition.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                statusMessage = "Requesting Activity Recognition (required for health FGS)..."
                return
            }
            if (!app.permissionManager.isNotificationPermissionGranted()) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            app.experimentController.startExperiment(experimentId)
            statusMessage = "FGS started: $experimentId"
        }

        setContent {
            AutoFitTheme {
                val scope = rememberCoroutineScope()
                val scrollState = rememberScrollState()
                var hcStatusText by remember { mutableStateOf("Checking Health Connect...") }
                var activeExperimentId by remember { mutableStateOf<UUID?>(null) }

                suspend fun refreshStatus() {
                    hcStatusText = when (val status = app.healthConnectManager.getSdkStatus()) {
                        is HealthSdkStatus.Available -> {
                            val granted = app.healthConnectManager.hasWritePermission()
                            "Health Connect: AVAILABLE (write granted=$granted)"
                        }
                        is HealthSdkStatus.Unavailable ->
                            "Health Connect: ${status.reason}"
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "AutoFit",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            text = "Android Background Execution Lab",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = hcStatusText,
                            style = MaterialTheme.typography.bodySmall,
                        )

                        Button(
                            onClick = { scope.launch { refreshStatus() } },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Refresh HC Status")
                        }

                        Button(
                            onClick = {
                                requestHealthPermissions.launch(
                                    app.permissionManager.requiredHealthConnectPermissions(),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Request WRITE_STEPS")
                        }

                        Button(
                            onClick = {
                                requestActivityRecognition.launch(
                                    Manifest.permission.ACTIVITY_RECOGNITION,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Request Activity Recognition (FGS)")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val end = Instant.now().truncatedTo(ChronoUnit.SECONDS)
                                    val start = end.minus(1, ChronoUnit.MINUTES)
                                    when (
                                        val result = app.healthConnectManager.writeSteps(
                                            stepCount = 100,
                                            startTime = start,
                                            endTime = end,
                                        )
                                    ) {
                                        is WriteResult.Success ->
                                            statusMessage = "Steps write succeeded."
                                        is WriteResult.Failure ->
                                            statusMessage = "Steps write failed: ${result.reason}"
                                    }
                                    refreshStatus()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Test HC Write (100 steps)")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val id = app.experimentController.createExperiment(
                                        ExperimentConfig(
                                            targetCadence = 120,
                                            randomRange = 15,
                                            durationMinutes = 5,
                                        ),
                                    )
                                    activeExperimentId = id
                                    statusMessage = "Experiment created: $id"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Create Test Experiment")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val id = app.experimentController.createExperiment(
                                        ExperimentConfig(
                                            targetCadence = 120,
                                            randomRange = 15,
                                            durationMinutes = 3,
                                        ),
                                    )
                                    activeExperimentId = id
                                    requestFgsPrerequisites(id)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Create & Start FGS (3 min)")
                        }

                        Button(
                            onClick = {
                                val id = activeExperimentId
                                if (id == null) {
                                    statusMessage = "No active experiment id"
                                } else {
                                    requestFgsPrerequisites(id)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Start FGS (last experiment)")
                        }

                        Button(
                            onClick = {
                                val id = activeExperimentId
                                if (id == null) {
                                    statusMessage = "No active experiment id"
                                } else {
                                    app.experimentController.stopExperiment(id)
                                    statusMessage = "FGS stop requested: $id"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Stop FGS (last experiment)")
                        }

                        Button(
                            onClick = {
                                app.settingsNavigator.launch(
                                    app.settingsNavigator.openAppDetailsSettings(),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Open App Settings")
                        }
                    }
                }
            }
        }
    }
}
