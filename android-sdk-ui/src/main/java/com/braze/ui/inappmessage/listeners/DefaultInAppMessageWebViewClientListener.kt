package com.braze.ui.inappmessage.listeners

import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.braze.Braze.Companion.getInstance
import com.braze.enums.Channel
import com.braze.enums.inappmessage.MessageType
import com.braze.models.inappmessage.IInAppMessage
import com.braze.models.inappmessage.IInAppMessageHtml
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.Priority.W
import com.braze.support.BrazeLogger.brazelog
import com.braze.support.isLocalUri
import com.braze.support.toBundle
import com.braze.ui.BrazeWebViewClient
import com.braze.ui.inappmessage.BrazeInAppMessageManager
import com.braze.ui.BrazeDeeplinkHandler.Companion.getInstance as getDeeplinkHandlerInstance

open class DefaultInAppMessageWebViewClientListener : IInAppMessageWebViewClientListener {
    private val inAppMessageManager: BrazeInAppMessageManager
        get() = BrazeInAppMessageManager.getInstance()

    override fun onCloseAction(inAppMessage: IInAppMessage, url: String, queryBundle: Bundle) {
        brazelog { "IInAppMessageWebViewClientListener.onCloseAction called." }
        logHtmlInAppMessageClick(inAppMessage, queryBundle)

        // Dismiss the in-app message due to the close action
        inAppMessageManager.hideCurrentlyDisplayingInAppMessage(true)
        inAppMessageManager.htmlInAppMessageActionListener.onCloseClicked(
            inAppMessage,
            url,
            queryBundle
        )
        brazelog { "IInAppMessageWebViewClientListener.onCloseAction finished." }
    }

    override fun onCustomEventAction(
        inAppMessage: IInAppMessage,
        url: String,
        queryBundle: Bundle
    ) {
        brazelog { "IInAppMessageWebViewClientListener.onCustomEventAction called." }
        if (inAppMessageManager.activity == null) {
            brazelog(W) { "Can't perform custom event action because the activity is null." }
            return
        }
        val wasHandled = inAppMessageManager.htmlInAppMessageActionListener.onCustomEventFired(
            inAppMessage,
            url,
            queryBundle
        )
        if (!wasHandled) {
            val customEventName =
                BrazeWebViewClient.parseCustomEventNameFromQueryBundle(queryBundle)
            if (customEventName.isNullOrBlank()) {
                return
            }
            val customEventProperties =
                BrazeWebViewClient.parsePropertiesFromQueryBundle(queryBundle)
            inAppMessageManager.activity?.let { activity ->
                getInstance(activity).logCustomEvent(
                    customEventName,
                    customEventProperties
                )
            }
        }
    }

    override fun onOtherUrlAction(inAppMessage: IInAppMessage, url: String, queryBundle: Bundle) {
        brazelog { "IInAppMessageWebViewClientListener.onOtherUrlAction called." }
        if (inAppMessageManager.activity == null) {
            brazelog(W) { "Can't perform other url action because the cached activity is null. Url: $url" }
            return
        }
        // Log a click since the uri link was followed
        logHtmlInAppMessageClick(inAppMessage, queryBundle)
        val wasHandled = inAppMessageManager.htmlInAppMessageActionListener.onOtherUrlAction(
            inAppMessage,
            url,
            queryBundle
        )
        if (wasHandled) {
            brazelog(V) {
                "HTML message action listener handled url in onOtherUrlAction. Doing nothing further. Url: $url"
            }
            return
        }

        // Parse the action
        val useWebViewForWebLinks = parseUseWebViewFromQueryBundle(inAppMessage, queryBundle)
        val inAppMessageBundle = inAppMessage.extras.toBundle()
        inAppMessageBundle.putAll(queryBundle)
        val uriAction = getDeeplinkHandlerInstance().createUriActionFromUrlString(
            url,
            inAppMessageBundle,
            useWebViewForWebLinks,
            Channel.INAPP_MESSAGE
        )
        if (uriAction == null) {
            brazelog(W) { "UriAction is null. Not passing any URI to BrazeDeeplinkHandler. Url: $url" }
            return
        }

        // If a local Uri is being handled here, then we want to keep the user in the Html in-app message and not hide the current in-app message.
        val uri = uriAction.uri
        if (uri.isLocalUri()) {
            brazelog(W) {
                "Not passing local uri to BrazeDeeplinkHandler. Got local uri: $uri for url: $url"
            }
            return
        }

        // Handle the action if it's not a local Uri
        inAppMessage.animateOut = false
        // Dismiss the in-app message since we're handling the URI outside of the in-app message webView
        inAppMessageManager.hideCurrentlyDisplayingInAppMessage(false)
        inAppMessageManager.activity?.let { activity ->
            getDeeplinkHandlerInstance().gotoUri(activity, uriAction)
        }
    }

    companion object {

        @JvmStatic
        @VisibleForTesting
        fun parseUseWebViewFromQueryBundle(
            inAppMessage: IInAppMessage,
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
            var useWebViewForWebLinks = inAppMessage.openUriInWebView
            if (isAnyQueryFlagSet) {
                useWebViewForWebLinks = !(isDeepLinkFlagSet || isExternalOpenFlagSet)
            }
            return useWebViewForWebLinks
        }

        @JvmStatic
        @VisibleForTesting
        fun logHtmlInAppMessageClick(inAppMessage: IInAppMessage, queryBundle: Bundle) {
            if (queryBundle.containsKey(BrazeWebViewClient.QUERY_NAME_BUTTON_ID)) {
                val inAppMessageHtml = inAppMessage as IInAppMessageHtml
                queryBundle.getString(BrazeWebViewClient.QUERY_NAME_BUTTON_ID)?.let {
                    inAppMessageHtml.logButtonClick(it)
                }
            } else if (inAppMessage.messageType === MessageType.HTML_FULL) {
                // HTML Full messages are the only html type that log clicks implicitly
                inAppMessage.logClick()
            }
        }
    }
}
