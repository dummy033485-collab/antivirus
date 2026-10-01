package com.safeshield.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.safeshield.app.R

object Routes {
    const val HOME = "home"
    const val APPS = "apps"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"

    const val SCAN = "scan"
    const val RESULT = "result/{scanId}"
    fun result(scanId: Long) = "result/$scanId"

    const val WIFI = "tools/wifi"
    const val APP_LOCK = "tools/applock"
    const val JUNK = "tools/junk"
    const val LINK = "tools/link"
    const val HISTORY = "tools/history"
    const val PRIVACY = "privacy"
}

enum class BottomTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    APPS(Routes.APPS, R.string.nav_apps, Icons.Filled.Apps),
    TOOLS(Routes.TOOLS, R.string.nav_tools, Icons.Filled.Build),
    SETTINGS(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
}
