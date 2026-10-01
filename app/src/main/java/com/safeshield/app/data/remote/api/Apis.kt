package com.safeshield.app.data.remote.api

import com.safeshield.app.data.local.json.SignatureFeed
import com.safeshield.app.data.remote.dto.SbRequest
import com.safeshield.app.data.remote.dto.SbResponse
import com.safeshield.app.data.remote.dto.VtFileResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * VirusTotal v3. We only ever call the *hash lookup* endpoint — no file upload
 * endpoint is declared here, so no code path can leak a user's file.
 */
interface VirusTotalApi {
    @GET("files/{hash}")
    suspend fun lookupHash(
        @Path("hash") sha256: String,
        @Header("x-apikey") apiKey: String,
    ): Response<VtFileResponse>
}

interface SafeBrowsingApi {
    @POST("v4/threatMatches:find")
    suspend fun findThreatMatches(
        @Query("key") apiKey: String,
        @Body body: SbRequest,
    ): Response<SbResponse>
}

interface SignatureFeedApi {
    @GET
    suspend fun fetchFeed(@Url url: String): Response<SignatureFeed>
}
