package com.pixson.autofit.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pixson.autofit.ui.ExperimentViewModel
import com.pixson.autofit.ui.ExperimentViewModelFactory
import com.pixson.autofit.ui.StartExperimentOutcome
import com.pixson.autofit.ui.config.ConfigScreen
import com.pixson.autofit.ui.environment.EnvironmentFixAction
import com.pixson.autofit.ui.environment.EnvironmentScreen
import com.pixson.autofit.ui.environment.EnvironmentViewModel
import com.pixson.autofit.ui.environment.EnvironmentViewModelFactory
import com.pixson.autofit.ui.history.HistoryDetailScreen
import com.pixson.autofit.ui.history.HistoryScreen
import com.pixson.autofit.ui.history.HistoryViewModel
import com.pixson.autofit.ui.history.HistoryViewModelFactory
import com.pixson.autofit.ui.running.RunningScreen
import com.pixson.autofit.system.SettingsNavigator
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.util.UUID

object AutoFitRoutes {
    const val CONFIG = "config"
    const val RUNNING = "running/{experimentId}"
    const val HISTORY = "history"
    const val HISTORY_DETAIL = "history/detail/{experimentId}"
    const val ENVIRONMENT = "environment"

    fun running(experimentId: UUID): String = "running/$experimentId"

    fun historyDetail(experimentId: UUID): String = "history/detail/$experimentId"
}

@Composable
fun AutoFitNavHost(
    viewModelFactory: ExperimentViewModelFactory,
    historyViewModelFactory: HistoryViewModelFactory,
    environmentViewModelFactory: EnvironmentViewModelFactory,
    settingsNavigator: SettingsNavigator,
    onRequestHealthPermissions: () -> Unit,
    onRequestActivityRecognition: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onNeedsActivityRecognition: (UUID) -> Unit,
    onViewModelReady: (ExperimentViewModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val experimentViewModel: ExperimentViewModel = viewModel(factory = viewModelFactory)
    val historyViewModel: HistoryViewModel = viewModel(factory = historyViewModelFactory)
    val environmentViewModel: EnvironmentViewModel = viewModel(factory = environmentViewModelFactory)

    val configState by experimentViewModel.configState.collectAsState()
    val runningState by experimentViewModel.runningState.collectAsState()
    val historyList by historyViewModel.listState.collectAsState()
    val historyDetail by historyViewModel.detailState.collectAsState()
    val environmentState by environmentViewModel.state.collectAsState()

    val showBottomBar = currentRoute in setOf(
        AutoFitRoutes.CONFIG,
        AutoFitRoutes.HISTORY,
        AutoFitRoutes.ENVIRONMENT,
    ) || currentRoute?.startsWith("history/detail/") == true

    LaunchedEffect(experimentViewModel) {
        onViewModelReady(experimentViewModel)
    }

    LaunchedEffect(experimentViewModel) {
        scope.launch { experimentViewModel.refreshHealthConnectStatus() }
        snapshotFlow { experimentViewModel.activeExperimentId.value }
            .filterNotNull()
            .take(1)
            .collect { runningId ->
                if (navController.currentBackStackEntry?.destination?.route == AutoFitRoutes.CONFIG) {
                    navController.navigate(AutoFitRoutes.running(runningId)) {
                        launchSingleTop = true
                    }
                }
            }
    }

    fun handleEnvironmentFix(action: EnvironmentFixAction) {
        when (action) {
            EnvironmentFixAction.BATTERY_OPTIMIZATION ->
                settingsNavigator.launch(settingsNavigator.openBatteryOptimizationSettings())
            EnvironmentFixAction.BATTERY_SAVER ->
                settingsNavigator.launch(settingsNavigator.openBatterySaverSettings())
            EnvironmentFixAction.NOTIFICATION_SETTINGS ->
                settingsNavigator.launch(settingsNavigator.openNotificationSettings())
            EnvironmentFixAction.HEALTH_CONNECT_SETTINGS ->
                settingsNavigator.openHealthConnectSettings()?.let(settingsNavigator::launch)
            EnvironmentFixAction.REQUEST_HEALTH_PERMISSIONS ->
                onRequestHealthPermissions()
            EnvironmentFixAction.REQUEST_NOTIFICATION ->
                onRequestNotificationPermission()
            EnvironmentFixAction.REQUEST_ACTIVITY_RECOGNITION ->
                onRequestActivityRecognition()
            EnvironmentFixAction.OVERLAY_SETTINGS ->
                settingsNavigator.launch(settingsNavigator.openOverlaySettings())
            EnvironmentFixAction.HEALTH_CONNECT_INSTALL ->
                settingsNavigator.launch(settingsNavigator.openHealthConnectInstall())
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                AutoFitBottomBar(navController = navController)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AutoFitRoutes.CONFIG,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AutoFitRoutes.CONFIG) {
                ConfigScreen(
                    state = configState,
                    onTargetCadenceChange = experimentViewModel::updateTargetCadence,
                    onRandomRangeChange = experimentViewModel::updateRandomRange,
                    onDurationChange = experimentViewModel::updateDurationMinutes,
                    onBatchMinutesChange = experimentViewModel::updateBatchMinutes,
                    onRequestHealthPermissions = onRequestHealthPermissions,
                    onRequestActivityRecognition = onRequestActivityRecognition,
                    onRefreshHealthStatus = {
                        scope.launch { experimentViewModel.refreshHealthConnectStatus() }
                    },
                    onStart = {
                        scope.launch {
                            onRequestNotificationPermission()
                            when (val outcome = experimentViewModel.onStartClicked()) {
                                is StartExperimentOutcome.Started -> {
                                    navController.navigate(AutoFitRoutes.running(outcome.experimentId)) {
                                        launchSingleTop = true
                                    }
                                }
                                is StartExperimentOutcome.NeedsActivityRecognition -> {
                                    onNeedsActivityRecognition(outcome.experimentId)
                                }
                                is StartExperimentOutcome.Blocked -> Unit
                            }
                        }
                    },
                )
            }

            composable(AutoFitRoutes.HISTORY) {
                HistoryScreen(
                    items = historyList,
                    onItemClick = { item ->
                        if (item.isRunning) {
                            experimentViewModel.openRunningSession(item.experimentId)
                            navController.navigate(AutoFitRoutes.running(item.experimentId)) {
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(AutoFitRoutes.historyDetail(item.experimentId))
                        }
                    },
                )
            }

            composable(
                route = AutoFitRoutes.HISTORY_DETAIL,
                arguments = listOf(
                    navArgument("experimentId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val experimentId = UUID.fromString(
                    backStackEntry.arguments?.getString("experimentId"),
                )
                LaunchedEffect(experimentId) {
                    historyViewModel.loadDetail(experimentId)
                }
                HistoryDetailScreen(
                    state = historyDetail,
                    onOpenRunning = {
                        experimentViewModel.openRunningSession(experimentId)
                        navController.navigate(AutoFitRoutes.running(experimentId)) {
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        historyViewModel.clearDetail()
                        navController.popBackStack()
                    },
                )
            }

            composable(AutoFitRoutes.ENVIRONMENT) {
                LaunchedEffect(Unit) {
                    environmentViewModel.refresh()
                }
                EnvironmentScreen(
                    state = environmentState,
                    onRefresh = {
                        scope.launch { environmentViewModel.refresh() }
                    },
                    onFix = ::handleEnvironmentFix,
                )
            }

            composable(
                route = AutoFitRoutes.RUNNING,
                arguments = listOf(
                    navArgument("experimentId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val experimentId = UUID.fromString(
                    backStackEntry.arguments?.getString("experimentId"),
                )
                LaunchedEffect(experimentId) {
                    if (experimentViewModel.runningState.value?.experimentId != experimentId) {
                        experimentViewModel.openRunningSession(experimentId)
                    }
                }
                RunningScreen(
                    state = runningState,
                    onStop = experimentViewModel::stopExperiment,
                    onBackToConfig = {
                        experimentViewModel.dismissRunningSession()
                        navController.navigate(AutoFitRoutes.CONFIG) {
                            popUpTo(AutoFitRoutes.CONFIG) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}
