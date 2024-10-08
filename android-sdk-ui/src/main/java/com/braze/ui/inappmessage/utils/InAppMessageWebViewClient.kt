package com.braze.ui.inappmessage.utils

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.braze.BrazeInternal
import com.braze.Constants.TRIGGERS_ASSETS_FOLDER
import com.braze.coroutine.BrazeCoroutineScope
import com.braze.models.inappmessage.IInAppMessage
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.Priority.I
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.brazelog
import com.braze.support.WebContentUtils.ASSET_LOADER_DUMMY_DOMAIN
import com.braze.support.getAssetFileStringContents
import com.braze.ui.inappmessage.BrazeInAppMessageManager
import com.braze.ui.inappmessage.listeners.IInAppMessageWebViewClientListener
import com.braze.ui.inappmessage.listeners.IWebViewClientStateListener
import com.braze.ui.support.getQueryParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * InAppMessageWebViewClient.
 *
 * @param context
 * @param inAppMessage The in-app message being displayed.
 * @param inAppMessageWebViewClientListener
 * @param assetDirectoryUrl The directory that preloaded assets are loaded in. Required when inAppMessage.messageType is
 * HTML_FULL. For all other message types, this must be null.
 */
open class InAppMessageWebViewClient(
    private val context: Context,
    private val inAppMessage: IInAppMessage,
    private val inAppMessageWebViewClientListener: IInAppMessageWebViewClientListener?,
    assetDirectoryUrl: String? = null
) : WebViewClientCompat() {
    private var webViewClientStateListener: IWebViewClientStateListener? = null
    private var hasPageFinishedLoading = false
    private val hasCalledPageFinishedOnListener = AtomicBoolean(false)
    private var markPageFinishedJob: Job? = null

    private val maxOnPageFinishedWaitTimeMs: Int =
        BrazeInternal.getConfigurationProvider(context).inAppMessageWebViewClientOnPageFinishedMaxWaitMs

    private val assetLoader =
        if (assetDirectoryUrl != null) {
            WebViewAssetLoader.Builder()
                .setDomain(ASSET_LOADER_DUMMY_DOMAIN)
                .addPathHandler(
                    "/",
                    WebViewAssetLoader.InternalStoragePathHandler(this.context, File(assetDirectoryUrl))
                )
                .build()
        } else {
            val triggerAssetsDir = File(context.cacheDir, TRIGGERS_ASSETS_FOLDER)

            WebViewAssetLoader.Builder()
                .setDomain(ASSET_LOADER_DUMMY_DOMAIN)
                .addPathHandler(
                    "/$TRIGGERS_ASSETS_FOLDER/",
                    WebViewAssetLoader.InternalStoragePathHandler(this.context, triggerAssetsDir)
                )
                .build()
        }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? =
        request?.let {
            assetLoader.shouldInterceptRequest(request.url)
        }

    @Deprecated("Use shouldInterceptRequest(WebView, WebResourceRequest)")
    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? =
        assetLoader.shouldInterceptRequest(Uri.parse(url))

    override fun onPageFinished(view: WebView, url: String) {
        appendBridgeJavascript(view)
        webViewClientStateListener?.let { stateListener ->
            if (hasCalledPageFinishedOnListener.compareAndSet(false, true)) {
                brazelog(V) { "Page has finished loading. Calling onPageFinished on listener" }
                stateListener.onPageFinished()
            }
        }
        hasPageFinishedLoading = true

        // Cancel any pending jobs based on the page finished wait
        markPageFinishedJob?.cancel()
        markPageFinishedJob = null
    }

    private fun markPageFinished() {
        webViewClientStateListener?.let { stateListener ->
            if (hasCalledPageFinishedOnListener.compareAndSet(false, true)) {
                brazelog(V) {
                    "Page may not have finished loading, but max wait time has expired." +
                        " Calling onPageFinished on listener."
                }
                stateListener.onPageFinished()
            }
        }
    }

    private fun appendBridgeJavascript(view: WebView) {
        val javascriptString: String = try {
            context.assets.getAssetFileStringContents(BRIDGE_JS_FILE)
        } catch (e: Exception) {
            // Fail instead of present a broken WebView
            BrazeInAppMessageManager.getInstance().hideCurrentlyDisplayingInAppMessage(false)
            brazelog(E, e) { "Failed to get HTML in-app message javascript additions" }
            return
        }
        view.loadUrl(JAVASCRIPT_PREFIX + javascriptString)
    }

    /**
     * Handles `appboy` schemed ("appboy://") urls in the HTML content WebViews. If the url isn't
     * `appboy` schemed, then the url is passed to the attached IInAppMessageWebViewClientListener.
     *
     * We expect the URLs to be hierarchical and have `appboy` equal the scheme.
     * For example, `appboy://close` is one such URL.
     *
     * @return true since all actions in Html In-App Messages are handled outside of the In-App Message itself.
     */
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
        handleUrlOverride(request.url.toString())

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String) = handleUrlOverride(url)

    fun setWebViewClientStateListener(listener: IWebViewClientStateListener?) {
        // If the page is already done loading, inform the new listener
        if (listener != null &&
            hasPageFinishedLoading &&
            hasCalledPageFinishedOnListener.compareAndSet(false, true)
        ) {
            listener.onPageFinished()
        } else {
            markPageFinishedJob = BrazeCoroutineScope.launchDelayed(maxOnPageFinishedWaitTimeMs) {
                withContext(Dispatchers.Main) {
                    markPageFinished()
                }
            }
        }
        webViewClientStateListener = listener
    }

    private fun handleUrlOverride(url: String): Boolean {
        if (inAppMessageWebViewClientListener == null) {
            brazelog(I) { "InAppMessageWebViewClient was given null IInAppMessageWebViewClientListener listener. Returning true." }
            return true
        }
        if (url.isBlank()) {
            // Blank urls shouldn't be passed back to the WebView. We return true here to indicate
            // to the WebView that we handled the url.
            brazelog(I) { "InAppMessageWebViewClient.shouldOverrideUrlLoading was given blank url. Returning true." }
            return true
        }
        val uri = Uri.parse(url)
        val queryBundle = getBundleFromUrl(url)
        if (uri.scheme != null && uri.scheme == BRAZE_INAPP_MESSAGE_SCHEME) {
            // Check the authority
            when (uri.authority) {
                null -> brazelog { "Uri authority was null. Uri: $uri" }
                AUTHORITY_NAME_CLOSE -> inAppMessageWebViewClientListener.onCloseAction(
                    inAppMessage,
                    url,
                    queryBundle
                )
                AUTHORITY_NAME_NEWSFEED -> inAppMessageWebViewClientListener.onNewsfeedAction(
                    inAppMessage,
                    url,
                    queryBundle
                )
                AUTHORITY_NAME_CUSTOM_EVENT -> inAppMessageWebViewClientListener.onCustomEventAction(
                    inAppMessage,
                    url,
                    queryBundle
                )
            }
            return true
        } else {
            brazelog { "Uri scheme was null or not an appboy url. Uri: $uri" }
        }
        inAppMessageWebViewClientListener.onOtherUrlAction(inAppMessage, url, queryBundle)
        return true
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        brazelog(I) { "The webview rendering process crashed, returning true" }

        // The app crashes after detecting the renderer crashed. Returning true to avoid app crash.
        return true
    }

    companion object {
        private const val BRIDGE_JS_FILE = "braze-html-in-app-message-bridge.js"

        private const val BRAZE_INAPP_MESSAGE_SCHEME = "appboy"
        private const val AUTHORITY_NAME_CLOSE = "close"
        private const val AUTHORITY_NAME_NEWSFEED = "feed"
        private const val AUTHORITY_NAME_CUSTOM_EVENT = "customEvent"

        /**
         * The query key for the button id for tracking.
         */
        const val QUERY_NAME_BUTTON_ID = "abButtonId"

        /**
         * The query key for opening links externally (i.e. outside your app). Url intents will be opened with
         * the INTENT.ACTION_VIEW intent. Links beginning with the appboy:// scheme are unaffected by this query key.
         */
        const val QUERY_NAME_EXTERNAL_OPEN = "abExternalOpen"

        /**
         * Query key for directing Braze to open Url intents using the INTENT.ACTION_VIEW.
         */
        const val QUERY_NAME_DEEPLINK = "abDeepLink"
        const val JAVASCRIPT_PREFIX = "javascript:"

        /**
         * Returns the string mapping of the query keys and values from the query string of the url. If the query string
         * contains duplicate keys, then the last key in the string will be kept.
         *
         * @param url the url
         * @return a bundle containing the key/value mapping of the query string. Will not be null.
         */
        @JvmStatic
        @VisibleForTesting
        fun getBundleFromUrl(url: String): Bundle {
            val queryBundle = Bundle()
            if (url.isBlank()) {
                return queryBundle
            }
            val uri = Uri.parse(url)

            uri.getQueryParameters().forEach { entry ->
                queryBundle.putString(entry.key, entry.value)
            }

            return queryBundle
        }
    }
}
