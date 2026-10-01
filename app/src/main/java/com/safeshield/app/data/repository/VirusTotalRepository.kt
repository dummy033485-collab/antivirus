package com.safeshield.app.data.repository

import com.safeshield.app.BuildConfig
import com.safeshield.app.core.Constants
import com.safeshield.app.data.remote.api.VirusTotalApi
import com.safeshield.app.domain.model.Severity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional online verdicts. Only a SHA-256 hash is transmitted – never a file.
 * Results are memoised for the process lifetime and limited to 4 concurrent
 * calls to stay inside VirusTotal's public rate limit.
 */
@Singleton
class VirusTotalRepository @Inject constructor(
    private val api: VirusTotalApi,
    private val ioDispatcher: CoroutineDispatcher,
) {
    data class Verdict(
        val malicious: Int,
        val suspicious: Int,
        val label: String?,
    ) {
        val severity: Severity
            get() = when {
                malicious >= 10 -> Severity.CRITICAL
                malicious >= Constants.VT_MALICIOUS_THRESHOLD -> Severity.HIGH
                malicious >= 1 || suspicious >= 3 -> Severity.MEDIUM
                else -> Severity.CLEAN
            }
    }

    private val cache = mutableMapOf<String, Verdict?>()
    private val gate = Semaphore(4)

    val isConfigured: Boolean get() = BuildConfig.VIRUSTOTAL_API_KEY.isNotBlank()

    /** Null means "no answer" (offline, not configured, rate limited, unknown hash). */
    suspend fun lookup(sha256: String): Verdict? {
        if (!isConfigured) return null
        val key = sha256.lowercase()
        synchronized(cache) { if (cache.containsKey(key)) return cache[key] }

        val verdict = withContext(ioDispatcher) {
            gate.withPermit {
                runCatching {
                    val response = api.lookupHash(key, BuildConfig.VIRUSTOTAL_API_KEY)
                    when {
                        response.code() == 404 -> null          // unknown file, not a verdict
                        !response.isSuccessful -> null          // 401/429/5xx -> stay silent
                        else -> response.body()?.data?.attributes?.let { attr ->
                            val stats = attr.stats ?: return@let null
                            Verdict(
                                malicious = stats.malicious,
                                suspicious = stats.suspicious,
                                label = attr.classification?.suggestedThreatLabel ?: attr.meaningfulName,
                            )
                        }
                    }
                }.getOrNull()
            }
        }
        synchronized(cache) { cache[key] = verdict }
        return verdict
    }

    fun clearCache() = synchronized(cache) { cache.clear() }
}
