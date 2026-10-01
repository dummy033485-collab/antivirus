package com.safeshield.app.feature.applock

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.safeshield.app.R
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.data.repository.AppLockRepository
import com.safeshield.app.ui.theme.DangerRed
import com.safeshield.app.ui.theme.SafeShieldTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shown over a locked app. Offers biometric first (fastest) and always keeps
 * the PIN as a fallback. The PIN is only ever compared as a salted hash.
 */
@AndroidEntryPoint
class LockScreenActivity : FragmentActivity() {

    @Inject lateinit var appLock: AppLockRepository
    @Inject lateinit var settings: SettingsRepository

    private lateinit var lockedPackage: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockedPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: run { finish(); return }

        setContent {
            SafeShieldTheme {
                var pin by remember { mutableStateOf("") }
                var error by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        stringResource(R.string.unlock_prompt, labelOf(lockedPackage)),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(8); error = false },
                        label = { Text(stringResource(R.string.applock_enter_pin)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        isError = error,
                        singleLine = true,
                    )
                    if (error) {
                        Text(stringResource(R.string.applock_wrong_pin), color = DangerRed)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        lifecycleScope.launch {
                            if (appLock.verifyPin(pin)) unlock() else error = true
                        }
                    }) { Text(stringResource(R.string.ok)) }

                    if (canUseBiometrics()) {
                        TextButton(onClick = ::promptBiometric) {
                            Text(stringResource(R.string.applock_use_biometric))
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            if (settings.current().biometricUnlockEnabled && canUseBiometrics()) promptBiometric()
        }
    }

    private fun labelOf(pkg: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    private fun canUseBiometrics(): Boolean =
        BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS

    private fun promptBiometric() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlock()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.unlock_prompt, labelOf(lockedPackage)))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        runCatching { prompt.authenticate(info) }
    }

    private fun unlock() {
        appLock.grantSession(lockedPackage)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "locked_package"
    }
}
