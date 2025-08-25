package com.braze.ui.banners.listeners

import android.content.Context
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.braze.Braze
import com.braze.enums.Channel
import com.braze.support.BrazeLogger.brazelog
import com.braze.support.BrazeLogger.Priority.W
import com.braze.support.isLocalUri
import com.braze.ui.BrazeWebViewClient
import com.braze.ui.BrazeDeeplinkHandler.Companion.getInstance as getDeeplinkHandlerInstance

open class DefaultBannerWebViewClientListener : IBannerWebViewClientListener {
    override fun onCloseAction(context: Context, url: String, queryBundle: Bundle) {
        brazelog(W) { "Close action not available on Banners." }
    }

    override fun onCustomEventAction(context: Context, url: String, queryBundle: Bundle) {
        brazelog { "DefaultBannerWebViewClientListener.onCustomEventAction called." }

        val customEventName = BrazeWebViewClient.parseCustomEventNameFromQueryBundle(queryBundle)
        if (customEventName.isNullOrBlank()) {
            return
        }
        val customEventProperties = BrazeWebViewClient.parsePropertiesFromQueryBundle(queryBundle)
        Braze.getInstance(context).logCustomEvent(customEventName, customEventProperties)
    }

    override fun onOtherUrlAction(context: Context, url: String, queryBundle: Bundle) {
        brazelog { "DefaultBannerWebViewClientListener.onOtherUrlAction called." }

        val useWebViewForWebLinks = parseUseWebViewFromQueryBundle(queryBundle)
        val uriAction = getDeeplinkHandlerInstance().createUriActionFromUrlString(
            url,
            queryBundle,
            useWebViewForWebLinks,
            Channel.BANNER
        )
        if (uriAction == null) {
            brazelog(W) { "UriAction is null. Not passing any URI to BrazeDeeplinkHandler. Url: $url." }
            return
        }

        val uri = uriAction.uri
        if (uri.isLocalUri()) {
            brazelog(W) {
                "Not passing local uri to BrazeDeeplinkHandler. Got local uri: $uri for url: $url."
            }
            return
        }
        getDeeplinkHandlerInstance().gotoUri(context, uriAction)
    }

    companion object {
        @JvmStatic
        @VisibleForTesting
        fun parseUseWebViewFromQueryBundle(
            queryBundle: Bundle
        ): Boolean {
            var isAnyQueryFlagSet = false
            var isDeepLinkFlagSet = false
            if (queryBundle.containsKey(BrazeWebViewClient.QUERY_NAME_DEEPLINK)) {
                isDeepLinkFlagSet =
                    queryBundle.getString(BrazeWebViewClient.QUERY_NAME_DEEPLINK).toBoolean()
                isAnyQueryFlagSet = true
            }
            var isExternalOpenFlagSet = false
            if (queryBundle.containsKey(BrazeWebViewClient.QUERY_NAME_EXTERNAL_OPEN)) {
                isExternalOpenFlagSet =
                    queryBundle.getString(BrazeWebViewClient.QUERY_NAME_EXTERNAL_OPEN).toBoolean()
                isAnyQueryFlagSet = true
            }
            var useWebViewForWebLinks = true
            if (isAnyQueryFlagSet) {
                useWebViewForWebLinks = !(isDeepLinkFlagSet || isExternalOpenFlagSet)
            }
            return useWebViewForWebLinks
        }
    }
}
