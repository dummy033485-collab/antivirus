package com.safeshield.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.safeshield.app.feature.applock.AppLockScreen
import com.safeshield.app.feature.apps.AppsScreen
import com.safeshield.app.feature.history.HistoryScreen
import com.safeshield.app.feature.home.HomeScreen
import com.safeshield.app.feature.junk.JunkScreen
import com.safeshield.app.feature.link.LinkCheckerScreen
import com.safeshield.app.feature.privacy.PrivacyScreen
import com.safeshield.app.feature.result.ResultScreen
import com.safeshield.app.feature.scan.ScanScreen
import com.safeshield.app.feature.settings.SettingsScreen
import com.safeshield.app.feature.tools.ToolsScreen
import com.safeshield.app.feature.wifi.WifiScreen

@Composable
fun SafeShieldApp(
    sharedLink: String? = null,
    onSharedLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // A link shared from another app jumps straight to the Link Checker.
    LaunchedEffect(sharedLink) {
        if (sharedLink != null) navController.navigate(Routes.LINK)
    }

    val showBottomBar = currentRoute in BottomTab.entries.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val destination = backStackEntry?.destination
                    BottomTab.entries.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) +
                    fadeIn(tween(260))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(220)) +
                    fadeOut(tween(220))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) +
                    fadeIn(tween(260))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(220)) +
                    fadeOut(tween(220))
            },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onScanStarted = { navController.navigate(Routes.SCAN) },
                    onOpenApps = { navController.navigate(Routes.APPS) },
                    onOpenHistory = { navController.navigate(Routes.HISTORY) },
                )
            }
            composable(Routes.APPS) { AppsScreen() }
            composable(Routes.TOOLS) {
                ToolsScreen(
                    onOpenWifi = { navController.navigate(Routes.WIFI) },
                    onOpenAppLock = { navController.navigate(Routes.APP_LOCK) },
                    onOpenJunk = { navController.navigate(Routes.JUNK) },
                    onOpenLink = { navController.navigate(Routes.LINK) },
                    onOpenHistory = { navController.navigate(Routes.HISTORY) },
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onOpenPrivacy = { navController.navigate(Routes.PRIVACY) })
            }
            composable(Routes.SCAN) {
                ScanScreen(
                    onFinished = { scanId ->
                        navController.navigate(Routes.result(scanId)) {
                            popUpTo(Routes.SCAN) { inclusive = true }
                        }
                    },
                    onCancelled = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.RESULT,
                arguments = listOf(navArgument("scanId") { type = NavType.LongType }),
            ) { entry ->
                ResultScreen(
                    scanId = entry.arguments?.getLong("scanId") ?: 0L,
                    onDone = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.WIFI) { WifiScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.APP_LOCK) { AppLockScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.JUNK) { JunkScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.LINK) {
                LinkCheckerScreen(
                    prefilledUrl = sharedLink,
                    onPrefillConsumed = onSharedLinkConsumed,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.HISTORY) { HistoryScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.PRIVACY) { PrivacyScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
