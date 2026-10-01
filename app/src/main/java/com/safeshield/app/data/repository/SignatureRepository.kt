package com.safeshield.app.data.repository

import android.content.Context
import com.safeshield.app.core.Constants
import com.safeshield.app.data.local.dao.SignatureDao
import com.safeshield.app.data.local.entity.MalwareSignatureEntity
import com.safeshield.app.data.local.json.SignatureFeed
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.data.remote.api.SignatureFeedApi
import com.safeshield.app.domain.model.Severity
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the offline malware-hash database: seeds it from the bundled asset on
 * first launch and can refresh it from a signed JSON feed.
 */
@Singleton
class SignatureRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: SignatureDao,
    private val moshi: Moshi,
    private val api: SignatureFeedApi,
    private val settings: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun seedIfEmpty() = withContext(ioDispatcher) {
        if (dao.count() > 0) return@withContext
        val feed = readBundledFeed() ?: return@withContext
        dao.upsertAll(feed.signatures.map { it.toEntity() })
        settings.setSignatureMeta(feed.version, System.currentTimeMillis())
    }

    suspend fun lookup(hash: String): MalwareSignatureEntity? = dao.findByHash(hash.lowercase())

    suspend fun lookupAll(hashes: List<String>): Map<String, MalwareSignatureEntity> =
        if (hashes.isEmpty()) emptyMap()
        else dao.findByHashes(hashes.map { it.lowercase() }).associateBy { it.sha256 }

    /** Returns the new signature count, or null when the update failed. */
    suspend fun updateFromRemote(url: String = Constants.SIGNATURE_FEED_URL): Int? =
        withContext(ioDispatcher) {
            runCatching {
                val response = api.fetchFeed(url)
                val feed = response.body()
                if (!response.isSuccessful || feed == null) return@runCatching null
                val local = settings.current().signatureVersion
                if (feed.version <= local) return@runCatching dao.count()
                dao.upsertAll(feed.signatures.map { it.toEntity() })
                settings.setSignatureMeta(feed.version, System.currentTimeMillis())
                dao.count()
            }.getOrNull()
        }

    suspend fun clear() = dao.clear()

    private fun readBundledFeed(): SignatureFeed? = runCatching {
        context.assets.open(Constants.BUNDLED_SIGNATURES_ASSET).use { stream ->
            val json = stream.bufferedReader().readText()
            moshi.adapter(SignatureFeed::class.java).fromJson(json)
        }
    }.getOrNull()

    private fun com.safeshield.app.data.local.json.SignatureDto.toEntity() = MalwareSignatureEntity(
        sha256 = sha256.lowercase(),
        malwareName = name,
        severity = runCatching { Severity.valueOf(severity.uppercase()) }.getOrDefault(Severity.HIGH),
        platform = platform,
    )
}
