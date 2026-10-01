package com.safeshield.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.safeshield.app.BuildConfig

/**
 * The ONLY ad surface in SafeShield: one small banner on the results screen.
 * Never shown while a scan is running, and removed entirely once the user buys
 * the "Remove ads" product.
 */
@Composable
fun ResultAdBanner(adsRemoved: Boolean, modifier: Modifier = Modifier) {
    if (adsRemoved) return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
