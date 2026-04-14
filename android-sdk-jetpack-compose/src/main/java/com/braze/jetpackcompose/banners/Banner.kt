package com.braze.jetpackcompose.banners

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.braze.ui.banners.BannerView

/**
 * Renders a Braze Banner by wrapping [BannerView] in a Jetpack Compose [AndroidView].
 *
 * @param placementId The Banner placement identifier, or null for the default placement.
 * @param heightCallback Optional callback invoked with the rendered Banner height in dp.
 */
@Composable
fun Banner(placementId: String? = null, heightCallback: ((Double) -> Unit)? = null) {
    // Adding a BannerView inside AndroidView
    // with layout as full screen
    AndroidView(factory = {
        BannerView(it, placementId).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            this.heightCallback = heightCallback
        }
    }, update = { it.placementId = placementId })
}
