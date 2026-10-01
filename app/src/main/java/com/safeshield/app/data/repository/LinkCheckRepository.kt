package com.safeshield.app.data.repository

import com.safeshield.app.BuildConfig
import com.safeshield.app.data.local.dao.LinkCheckDao
import com.safeshield.app.data.local.entity.LinkCheckEntity
import com.safeshield.app.data.remote.api.SafeBrowsingApi
import com.safeshield.app.data.remote.dto.SbClient
import com.safeshield.app.data.remote.dto.SbRequest
import com.safeshield.app.data.remote.dto.SbThreatEntry
import com.safeshield.app.data.remote.dto.SbThreatInfo
import com.safeshield.app.domain.model.LinkVerdict
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkCheckRepository @Inject constructor(
    private val api: SafeBrowsingApi,
    private val dao: LinkCheckDao,
    private val ioDispatcher: CoroutineDispatcher,
) {
    sealed interface Outcome {
        data class Checked(val verdict: LinkVerdict) : Outcome
        data object InvalidUrl : Outcome
        data object NotConfigured : Outcome
        data class Failed(val message: String) : Outcome
    }

    val isConfigured: Boolean get() = BuildConfig.SAFE_BROWSING_API_KEY.isNotBlank()

    fun observeHistory(): Flow<List<LinkCheckEntity>> = dao.observeRecent()

    suspend fun check(rawUrl: String): Outcome = withContext(ioDispatcher) {
        val url = normalize(rawUrl) ?: return@withContext Outcome.InvalidUrl
        if (!isConfigured) return@withContext Outcome.NotConfigured

        runCatching {
            val response = api.findThreatMatches(
                apiKey = BuildConfig.SAFE_BROWSING_API_KEY,
                body = SbRequest(
                    client = SbClient("com.safeshield.app", BuildConfig.VERSION_NAME),
                    threatInfo = SbThreatInfo(
                        threatTypes = listOf(
                            "MALWARE", "SOCIAL_ENGINEERING",
                            "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION",
                        ),
                        platformTypes = listOf("ANY_PLATFORM"),
                        threatEntryTypes = listOf("URL"),
                        threatEntries = listOf(SbThreatEntry(url)),
                    ),
                ),
            )
            if (!response.isSuccessful) {
                return@runCatching Outcome.Failed("HTTP ${response.code()}")
            }
            val threats = response.body()?.matches?.mapNotNull { it.threatType }.orEmpty().distinct()
            val verdict = LinkVerdict(url, threats.isEmpty(), threats, System.currentTimeMillis())
            dao.insert(
                LinkCheckEntity(
                    url = url,
                    safe = verdict.safe,
                    threatTypes = threats,
                    checkedAt = verdict.checkedAt,
                )
            )
            Outcome.Checked(verdict)
        }.getOrElse { Outcome.Failed(it.message ?: "error") }
    }

    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else "https://$trimmed"
        return runCatching {
            val uri = java.net.URI(withScheme)
            if (uri.host.isNullOrBlank() || !uri.host.contains('.')) null else withScheme
        }.getOrNull()
    }

    suspend fun clear() = dao.clear()
}
