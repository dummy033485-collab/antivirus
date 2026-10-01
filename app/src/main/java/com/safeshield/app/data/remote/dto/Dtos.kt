package com.safeshield.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---------- VirusTotal v3 (hash lookup only) ----------

@JsonClass(generateAdapter = true)
data class VtFileResponse(
    @Json(name = "data") val data: VtFileData?,
)

@JsonClass(generateAdapter = true)
data class VtFileData(
    @Json(name = "id") val id: String?,
    @Json(name = "attributes") val attributes: VtAttributes?,
)

@JsonClass(generateAdapter = true)
data class VtAttributes(
    @Json(name = "last_analysis_stats") val stats: VtStats?,
    @Json(name = "meaningful_name") val meaningfulName: String?,
    @Json(name = "popular_threat_classification") val classification: VtClassification?,
)

@JsonClass(generateAdapter = true)
data class VtStats(
    @Json(name = "harmless") val harmless: Int = 0,
    @Json(name = "malicious") val malicious: Int = 0,
    @Json(name = "suspicious") val suspicious: Int = 0,
    @Json(name = "undetected") val undetected: Int = 0,
)

@JsonClass(generateAdapter = true)
data class VtClassification(
    @Json(name = "suggested_threat_label") val suggestedThreatLabel: String?,
)

// ---------- Google Safe Browsing v4 ----------

@JsonClass(generateAdapter = true)
data class SbRequest(
    @Json(name = "client") val client: SbClient,
    @Json(name = "threatInfo") val threatInfo: SbThreatInfo,
)

@JsonClass(generateAdapter = true)
data class SbClient(
    @Json(name = "clientId") val clientId: String,
    @Json(name = "clientVersion") val clientVersion: String,
)

@JsonClass(generateAdapter = true)
data class SbThreatInfo(
    @Json(name = "threatTypes") val threatTypes: List<String>,
    @Json(name = "platformTypes") val platformTypes: List<String>,
    @Json(name = "threatEntryTypes") val threatEntryTypes: List<String>,
    @Json(name = "threatEntries") val threatEntries: List<SbThreatEntry>,
)

@JsonClass(generateAdapter = true)
data class SbThreatEntry(@Json(name = "url") val url: String)

@JsonClass(generateAdapter = true)
data class SbResponse(
    @Json(name = "matches") val matches: List<SbMatch>?,
)

@JsonClass(generateAdapter = true)
data class SbMatch(
    @Json(name = "threatType") val threatType: String?,
    @Json(name = "platformType") val platformType: String?,
)
