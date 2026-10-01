package com.safeshield.app.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.safeshield.app.domain.model.RiskLevel
import com.safeshield.app.domain.model.WifiSecurityInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rates ONLY the network the device is currently joined to. Nothing is logged,
 * stored or transmitted; the SSID never leaves this function's return value.
 */
@Singleton
class WifiSecurityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) {
    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun currentNetwork(): WifiSecurityInfo = withContext(ioDispatcher) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val onWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        if (!onWifi) return@withContext WifiSecurityInfo(false, null, "NONE", 0, RiskLevel.LOW)

        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        @Suppress("DEPRECATION")
        val info = runCatching { wifi?.connectionInfo }.getOrNull()
        val ssid = info?.ssid?.trim('"')?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }

        val encryption = detectEncryption(wifi, ssid)
        val rating = ratingFor(encryption)
        WifiSecurityInfo(
            connected = true,
            ssid = ssid,
            encryption = encryption,
            rating = rating,
            level = when {
                rating >= 80 -> RiskLevel.LOW
                rating >= 50 -> RiskLevel.MEDIUM
                else -> RiskLevel.HIGH
            },
        )
    }

    private fun detectEncryption(wifi: WifiManager?, ssid: String?): String {
        if (wifi == null || ssid == null || !hasLocationPermission()) return "UNKNOWN"
        val result: ScanResult = runCatching {
            @Suppress("DEPRECATION")
            wifi.scanResults?.firstOrNull { it.SSID == ssid }
        }.getOrNull() ?: return "UNKNOWN"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ exposes an explicit security type; prefer it when present.
            val type = runCatching { result.javaClass.getMethod("getSecurityTypes") }
                .getOrNull()
            if (type != null) {
                val types = runCatching { type.invoke(result) as? IntArray }.getOrNull()
                mapSecurityTypes(types)?.let { return it }
            }
        }
        return fromCapabilities(result.capabilities)
    }

    /** Values mirror WifiInfo.SECURITY_TYPE_* constants. */
    private fun mapSecurityTypes(types: IntArray?): String? {
        if (types == null || types.isEmpty()) return null
        return when {
            types.contains(4) || types.contains(8) || types.contains(9) -> "WPA3"
            types.contains(2) || types.contains(5) -> "WPA2"
            types.contains(3) -> "WPA"
            types.contains(1) -> "WEP"
            types.contains(0) -> "OPEN"
            else -> null
        }
    }

    private fun fromCapabilities(capabilities: String?): String {
        val caps = capabilities.orEmpty().uppercase()
        return when {
            caps.contains("SAE") || caps.contains("WPA3") -> "WPA3"
            caps.contains("RSN") || caps.contains("WPA2") -> "WPA2"
            caps.contains("WPA") -> "WPA"
            caps.contains("WEP") -> "WEP"
            caps.isBlank() || caps.contains("ESS") && !caps.contains("WPA") && !caps.contains("WEP") -> "OPEN"
            else -> "UNKNOWN"
        }
    }

    private fun ratingFor(encryption: String): Int = when (encryption) {
        "WPA3" -> 100
        "WPA2" -> 85
        "WPA" -> 60
        "WEP" -> 25
        "OPEN" -> 10
        else -> 50
    }
}
