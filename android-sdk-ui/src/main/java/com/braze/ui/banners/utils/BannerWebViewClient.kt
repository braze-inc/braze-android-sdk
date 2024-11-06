package com.braze.ui.banners.utils

import android.content.Context
import com.braze.ui.BrazeWebViewClient
import com.braze.ui.banners.listeners.IBannerWebViewClientListener

/**
 * BannerWebViewClient.
 *
 * @param context
 * @param bannerWebViewClientListener
 */
class BannerWebViewClient(
    context: Context,
    bannerWebViewClientListener: IBannerWebViewClientListener?
) : BrazeWebViewClient(
    context,
    Type.BANNER,
    null,
    null,
    bannerWebViewClientListener,
    null
)
