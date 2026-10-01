package com.safeshield.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeshield.app.data.prefs.ThemeMode
import com.safeshield.app.ui.navigation.SafeShieldApp
import com.safeshield.app.ui.theme.SafeShieldTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleSharedLink(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val sharedLink by viewModel.sharedLink.collectAsStateWithLifecycle()

            SafeShieldTheme(themeMode = settings?.themeMode ?: ThemeMode.SYSTEM) {
                SafeShieldApp(
                    sharedLink = sharedLink,
                    onSharedLinkConsumed = viewModel::consumeSharedLink,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedLink(intent)
    }

    private fun handleSharedLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let(viewModel::onSharedLink)
    }
}
