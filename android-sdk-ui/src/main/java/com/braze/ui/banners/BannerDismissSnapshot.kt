package com.braze.ui.banners

import androidx.annotation.Keep

/**
 * Identity fields for a dismissed banner, passed to [BannerView.onDismissCallback].
 * Fields may be null when the view had no resolved banner data (for example placement set before sync).
 */
@Keep
data class BannerDismissSnapshot(
    val placementId: String?,
    val stableKey: String?,
    val trackingId: String?,
)
