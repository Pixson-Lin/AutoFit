package com.pixson.autofit.ui.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pixson.autofit.R

@Composable
fun AutoFitBottomBar(
    navController: NavController,
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == AutoFitRoutes.CONFIG,
            onClick = {
                navController.navigate(AutoFitRoutes.CONFIG) {
                    popUpTo(AutoFitRoutes.CONFIG) { inclusive = true }
                    launchSingleTop = true
                }
            },
            icon = { Text("⚙") },
            label = { Text(stringResource(R.string.nav_config)) },
        )
        NavigationBarItem(
            selected = currentRoute == AutoFitRoutes.HISTORY ||
                currentRoute?.startsWith("history/detail/") == true,
            onClick = {
                navController.navigate(AutoFitRoutes.HISTORY) {
                    launchSingleTop = true
                }
            },
            icon = { Text("📋") },
            label = { Text(stringResource(R.string.nav_history)) },
        )
        NavigationBarItem(
            selected = currentRoute == AutoFitRoutes.ENVIRONMENT,
            onClick = {
                navController.navigate(AutoFitRoutes.ENVIRONMENT) {
                    launchSingleTop = true
                }
            },
            icon = { Text("🔋") },
            label = { Text(stringResource(R.string.nav_environment)) },
        )
    }
}
