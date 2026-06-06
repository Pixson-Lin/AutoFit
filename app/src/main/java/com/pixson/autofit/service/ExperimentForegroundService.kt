package com.pixson.autofit.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import com.pixson.autofit.AutoFitApplication
import com.pixson.autofit.domain.model.ExperimentStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class ExperimentForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null
    private var activeExperimentId: UUID? = null
    private var stopRequested = false

    private lateinit var notificationController: NotificationController
    private lateinit var loopRunner: ExperimentLoopRunner

    override fun onCreate() {
        super.onCreate()
        val app = application as AutoFitApplication
        notificationController = NotificationController(this)
        notificationController.ensureChannel()
        loopRunner = ExperimentLoopRunner(
            repository = app.experimentRepository,
            deviceStateSource = DeviceStateReader(this),
            wakeLockGateway = WakeLockHelper(
                powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager,
            ),
            elapsedRealtime = { SystemClock.elapsedRealtime() },
            currentInstant = { Instant.now().truncatedTo(ChronoUnit.MILLIS) },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ServiceConstants.ACTION_STOP) {
            val experimentId = intent.experimentIdOrNull()
            stopExperiment(experimentId, "manual_stop")
            return START_NOT_STICKY
        }

        val experimentId = intent?.experimentIdOrNull()
        if (experimentId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val app = application as AutoFitApplication
        if (!app.permissionManager.canStartHealthForegroundService()) {
            ServiceEventLogger.startBlocked(experimentId, "missing_activity_recognition")
            stopSelf()
            return START_NOT_STICKY
        }

        activeExperimentId = experimentId
        stopRequested = false
        ServiceEventLogger.started(experimentId)

        val notification = notificationController.buildRunningNotification(
            experimentId = experimentId,
            statusLine = getString(com.pixson.autofit.R.string.notification_status_starting),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                ServiceConstants.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
            )
        } else {
            startForeground(ServiceConstants.NOTIFICATION_ID, notification)
        }

        loopJob?.cancel()
        loopJob = serviceScope.launch {
            loopRunner.run(
                experimentId = experimentId,
                isStopped = { stopRequested || !isActive },
                onTick = { steps, tickIndex ->
                    val updated = notificationController.buildRunningNotification(
                        experimentId = experimentId,
                        statusLine = "Tick $tickIndex · $steps steps",
                    )
                    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                    notificationManager.notify(ServiceConstants.NOTIFICATION_ID, updated)
                },
            )
            stopExperiment(experimentId, "loop_finished")
        }

        return START_REDELIVER_INTENT
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        ServiceEventLogger.taskRemoved(activeExperimentId)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        ServiceEventLogger.destroyed(activeExperimentId)
        loopJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopExperiment(experimentId: UUID?, reason: String) {
        stopRequested = true
        loopJob?.cancel()
        loopJob = null

        if (reason == "manual_stop" && experimentId != null) {
            serviceScope.launch {
                val app = application as AutoFitApplication
                val experiment = app.experimentRepository.getExperiment(experimentId)
                if (experiment?.status == ExperimentStatus.RUNNING) {
                    app.experimentRepository.updateExperimentStatus(
                        experimentId,
                        ExperimentStatus.STOPPED,
                    )
                }
            }
        }

        ServiceEventLogger.stopped(experimentId, reason)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun Intent.experimentIdOrNull(): UUID? {
        val raw = getStringExtra(ServiceConstants.EXTRA_EXPERIMENT_ID) ?: return null
        return runCatching { UUID.fromString(raw) }.getOrNull()
    }

    companion object {
        fun startIntent(context: Context, experimentId: UUID): Intent {
            return Intent(context, ExperimentForegroundService::class.java).apply {
                putExtra(ServiceConstants.EXTRA_EXPERIMENT_ID, experimentId.toString())
            }
        }

        fun stopIntent(context: Context, experimentId: UUID): Intent {
            return Intent(context, ExperimentForegroundService::class.java).apply {
                action = ServiceConstants.ACTION_STOP
                putExtra(ServiceConstants.EXTRA_EXPERIMENT_ID, experimentId.toString())
            }
        }
    }
}
