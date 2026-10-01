package com.safeshield.app.data.local.json

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Shape of assets/malware_signatures.json and the remote update feed. */
@JsonClass(generateAdapter = true)
data class SignatureFeed(
    @Json(name = "version") val version: Int,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "signatures") val signatures: List<SignatureDto>,
)

@JsonClass(generateAdapter = true)
data class SignatureDto(
    @Json(name = "sha256") val sha256: String,
    @Json(name = "name") val name: String,
    @Json(name = "severity") val severity: String,
    @Json(name = "platform") val platform: String = "android",
)
