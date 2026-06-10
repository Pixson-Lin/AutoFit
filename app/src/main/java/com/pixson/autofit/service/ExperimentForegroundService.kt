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
    private lateinit var overlayController: OverlayController
    private lateinit var loopRunner: ExperimentLoopRunner
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var experimentFinalizer: ExperimentFinalizer

    override fun onCreate() {
        super.onCreate()
        val app = application as AutoFitApplication
        val elapsedRealtime = { SystemClock.elapsedRealtime() }
        val currentInstant = { Instant.now().truncatedTo(ChronoUnit.MILLIS) }

        notificationController = NotificationController(
            context = this,
            elapsedRealtime = elapsedRealtime,
        )
        notificationController.ensureChannel()

        overlayController = OverlayController(
            context = this,
            overlayHost = SystemOverlayWindowHost(
                context = this,
                windowManager = getSystemService(WINDOW_SERVICE) as android.view.WindowManager,
                canDrawOverlays = { app.permissionManager.canDrawOverlays() },
            ),
            elapsedRealtime = elapsedRealtime,
        )

        val healthWriteCoordinator = HealthWriteCoordinator(
            healthConnectManager = app.healthConnectManager,
            repository = app.experimentRepository,
            currentInstant = currentInstant,
        )

        loopRunner = ExperimentLoopRunner(
            repository = app.experimentRepository,
            healthWriteCoordinator = healthWriteCoordinator,
            deviceStateSource = DeviceStateReader(this),
            wakeLockGateway = WakeLockHelper(
                powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager,
            ),
            elapsedRealtime = elapsedRealtime,
            currentInstant = currentInstant,
        )

        alarmScheduler = AlarmScheduler(
            context = this,
            canScheduleExact = { app.permissionManager.canScheduleExactAlarms() },
            elapsedRealtime = elapsedRealtime,
        )

        experimentFinalizer = ExperimentFinalizer(
            repository = app.experimentRepository,
            resultAggregator = app.resultAggregator,
            healthWriteCoordinator = healthWriteCoordinator,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ServiceConstants.ACTION_STOP) {
            val experimentId = intent.experimentIdOrNull()
            stopExperiment(experimentId, ExperimentStatus.STOPPED, "manual_stop")
            return START_NOT_STICKY
        }

        val experimentId = intent?.experimentIdOrNull()
        if (experimentId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent.action == ServiceConstants.ACTION_ALARM_BACKSTOP) {
            handleAlarmBackstop(experimentId)
            return START_REDELIVER_INTENT
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
        notificationController.resetThrottle()
        overlayController.resetThrottle()

        val notification = notificationController.buildStartingNotification(experimentId)
        startInForeground(notification)

        alarmScheduler.scheduleBackstop(experimentId)
        startLoopIfNeeded(experimentId)

        return START_REDELIVER_INTENT
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        ServiceEventLogger.taskRemoved(activeExperimentId)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        activeExperimentId?.let { alarmScheduler.cancel(it) }
        overlayController.dismiss()
        ServiceEventLogger.destroyed(activeExperimentId)
        loopJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleAlarmBackstop(experimentId: UUID) {
        if (activeExperimentId != experimentId) {
            activeExperimentId = experimentId
        }
        alarmScheduler.scheduleBackstop(experimentId)
        if (loopJob?.isActive != true) {
            startLoopIfNeeded(experimentId)
        }
    }

    private fun startLoopIfNeeded(experimentId: UUID) {
        if (loopJob?.isActive == true) return

        loopJob = serviceScope.launch {
            val exitReason = loopRunner.run(
                experimentId = experimentId,
                isStopped = { stopRequested || !isActive },
                onTick = { snapshot -> updateNotificationThrottled(experimentId, snapshot) },
            )

            val terminalStatus = when (exitReason) {
                LoopExitReason.COMPLETED -> ExperimentStatus.COMPLETED
                LoopExitReason.STOPPED_EARLY -> ExperimentStatus.STOPPED
                LoopExitReason.NOT_FOUND,
                LoopExitReason.NOT_RUNNING,
                -> null
            }

            if (terminalStatus != null) {
                finalizeAndStop(experimentId, terminalStatus, "loop_${exitReason.name.lowercase()}")
            } else {
                stopForegroundOnly(experimentId, "loop_${exitReason.name.lowercase()}")
            }
        }
    }

    private fun updateNotificationThrottled(experimentId: UUID, snapshot: TickSnapshot) {
        alarmScheduler.scheduleBackstop(experimentId)
        if (!notificationController.shouldUpdate()) return

        val notification = notificationController.buildRunningNotification(
            RunningNotificationSnapshot(
                experimentId = experimentId,
                totalSteps = snapshot.totalWrittenSteps,
                remainingMinutes = snapshot.remainingMinutes,
                tickIndex = snapshot.tickIndex,
            ),
        )
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(ServiceConstants.NOTIFICATION_ID, notification)
        notificationController.markUpdated()
        overlayController.update(
            RunningNotificationSnapshot(
                experimentId = experimentId,
                totalSteps = snapshot.totalWrittenSteps,
                remainingMinutes = snapshot.remainingMinutes,
                tickIndex = snapshot.tickIndex,
            ),
        )
    }

    private fun stopExperiment(
        experimentId: UUID?,
        terminalStatus: ExperimentStatus,
        reason: String,
    ) {
        stopRequested = true
        loopJob?.cancel()
        loopJob = null

        if (experimentId == null) {
            stopForegroundOnly(null, reason)
            return
        }

        serviceScope.launch {
            finalizeAndStop(experimentId, terminalStatus, reason)
        }
    }

    private suspend fun finalizeAndStop(
        experimentId: UUID,
        terminalStatus: ExperimentStatus,
        reason: String,
    ) {
        alarmScheduler.cancel(experimentId)
        experimentFinalizer.finalize(
            experimentId = experimentId,
            terminalStatus = terminalStatus,
            endTime = Instant.now().truncatedTo(ChronoUnit.MILLIS),
            flushPending = terminalStatus == ExperimentStatus.COMPLETED,
        )
        overlayController.dismiss()
        ServiceEventLogger.stopped(experimentId, reason)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopForegroundOnly(experimentId: UUID?, reason: String) {
        experimentId?.let { alarmScheduler.cancel(it) }
        overlayController.dismiss()
        ServiceEventLogger.stopped(experimentId, reason)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startInForeground(notification: android.app.Notification) {
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

        fun alarmBackstopIntent(context: Context, experimentId: String): Intent {
            return Intent(context, ExperimentForegroundService::class.java).apply {
                action = ServiceConstants.ACTION_ALARM_BACKSTOP
                putExtra(ServiceConstants.EXTRA_EXPERIMENT_ID, experimentId)
            }
        }
    }
}
