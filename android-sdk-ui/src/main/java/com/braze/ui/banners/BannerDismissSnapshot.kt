package com.braze.ui.banners

import androidx.annotation.Keep

/**
 * Identity fields for a dismissed banner, passed to [BannerView.onDismissCallback].
 */
@Keep
data class BannerDismissSnapshot(
    val placementId: String,
    val stableKey: String,
    val trackingId: String,
) {
    internal companion object {
        fun fromNullableFields(
            placementId: String?,
            stableKey: String?,
            trackingId: String?,
        ): BannerDismissSnapshot? {
            val nonNullPlacementId = placementId ?: return null
            val nonNullStableKey = stableKey ?: return null
            val nonNullTrackingId = trackingId ?: return null
            return BannerDismissSnapshot(
                placementId = nonNullPlacementId,
                stableKey = nonNullStableKey,
                trackingId = nonNullTrackingId,
            )
        }
    }
}
