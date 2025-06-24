package com.braze.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.VisibleForTesting
import androidx.webkit.WebViewAssetLoader
import com.braze.BrazeInternal
import com.braze.Constants
import com.braze.coroutine.BrazeCoroutineScope
import com.braze.models.inappmessage.IInAppMessage
import com.braze.models.outgoing.BrazeProperties
import com.braze.support.BrazeLogger.brazelog
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.Priority.I
import com.braze.support.WebContentUtils
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
import com.braze.ui.banners.listeners.IBannerWebViewClientListener

open class BrazeWebViewClient(
    val context: Context,
    val type: Type,
    private val inAppMessage: IInAppMessage? = null,
    private val inAppMessageWebViewClientListener: IInAppMessageWebViewClientListener? = null,
    private val bannerWebViewClientListener: IBannerWebViewClientListener? = null,
    assetDirectoryUrl: String? = null
) : WebViewClient() {

    enum class Type {
        BANNER,
        IN_APP_MESSAGE
    }

    private var webViewClientStateListener: IWebViewClientStateListener? = null
    private var hasPageFinishedLoading = false
    private val hasCalledPageFinishedOnListener = AtomicBoolean(false)
    private var markPageFinishedJob: Job? = null

    private val maxOnPageFinishedWaitTimeMs: Int =
        BrazeInternal.getConfigurationProvider(context).inAppMessageWebViewClientOnPageFinishedMaxWaitMs

    private val assetLoader =
        if (assetDirectoryUrl != null) {
            WebViewAssetLoader.Builder()
                .setDomain(WebContentUtils.ASSET_LOADER_DUMMY_DOMAIN)
                .addPathHandler(
                    "/",
                    WebViewAssetLoader.InternalStoragePathHandler(
                        this.context,
                        File(assetDirectoryUrl)
                    )
                )
                .build()
        } else {
            val triggerAssetsDir = File(context.cacheDir, Constants.TRIGGERS_ASSETS_FOLDER)

            WebViewAssetLoader.Builder()
                .setDomain(WebContentUtils.ASSET_LOADER_DUMMY_DOMAIN)
                .addPathHandler(
                    "/${Constants.TRIGGERS_ASSETS_FOLDER}/",
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

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
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

    private fun appendBridgeJavascript(view: WebView) {
        val javascriptString: String = try {
            context.assets.getAssetFileStringContents(BRIDGE_JS_FILE)
        } catch (e: Exception) {
            // Fail instead of presenting a broken WebView
            if (type == Type.IN_APP_MESSAGE) {
                BrazeInAppMessageManager.getInstance().hideCurrentlyDisplayingInAppMessage(false)
            }
            brazelog(E, e) { "Failed to get HTML ${type.name} javascript additions" }
            return
        }
        view.loadUrl(JAVASCRIPT_PREFIX + javascriptString)
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

    @Deprecated("Deprecated in API 24")
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

        // When given a null listener or a blank url, log message and return early.
        val logMessage = when {
            type == Type.IN_APP_MESSAGE && inAppMessageWebViewClientListener == null -> {
                "BrazeWebViewClient was given null IInAppMessageWebViewClientListener listener. Returning true."
            }
            type == Type.BANNER && bannerWebViewClientListener == null -> {
                "BrazeWebViewClient was given null IBannerWebViewClientListener listener. Returning true."
            }
            url.isBlank() -> {
                // Blank urls shouldn't be passed back to the WebView. We return true here to indicate
                // to the WebView that we handled the url.
                "BrazeWebViewClient.shouldOverrideUrlLoading was given blank url. Returning true."
            }
            else -> null
        }
        logMessage?.let {
            brazelog(I) { it }
            return true
        }
        val uri = Uri.parse(url)
        val queryBundle = getBundleFromUrl(url)
        if (uri.scheme != null && uri.scheme == BRAZE_SCHEME) {
            handleQueryAction(url, uri, queryBundle)
            return true
        } else {
            brazelog { "Uri scheme was null or not an appboy url. Uri: $uri" }
        }
        when (type) {
            Type.IN_APP_MESSAGE -> {
                inAppMessage?.let {
                    inAppMessageWebViewClientListener?.onOtherUrlAction(it, url, queryBundle)
                }
            }
            Type.BANNER -> {
                bannerWebViewClientListener?.onOtherUrlAction(context, url, queryBundle)
            }
        }
        return true
    }

    private fun handleQueryAction(url: String, uri: Uri, queryBundle: Bundle) {
        // Check the authority
        when (uri.authority) {
            null -> brazelog { "Uri authority was null. Uri: $uri" }
            AUTHORITY_NAME_CLOSE ->
                if (type == Type.IN_APP_MESSAGE) {
                    inAppMessage?.let {
                        inAppMessageWebViewClientListener?.onCloseAction(
                            it,
                            url,
                            queryBundle
                        )
                    }
                } else if (type == Type.BANNER) {
                    bannerWebViewClientListener?.onCloseAction(context, url, queryBundle)
                }

            AUTHORITY_NAME_NEWSFEED ->
                if (type == Type.IN_APP_MESSAGE) {
                    inAppMessage?.let {
                        inAppMessageWebViewClientListener?.onNewsfeedAction(
                            it,
                            url,
                            queryBundle
                        )
                    }
                } else if (type == Type.BANNER) {
                    bannerWebViewClientListener?.onNewsfeedAction(context, url, queryBundle)
                }

            AUTHORITY_NAME_CUSTOM_EVENT ->
                if (type == Type.IN_APP_MESSAGE) {
                    inAppMessage?.let {
                        inAppMessageWebViewClientListener?.onCustomEventAction(
                            it,
                            url,
                            queryBundle
                        )
                    }
                } else if (type == Type.BANNER) {
                    bannerWebViewClientListener?.onCustomEventAction(context, url, queryBundle)
                }
        }
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        brazelog(I) { "The webview rendering process crashed, returning true" }

        // The app crashes after detecting the renderer crashed. Returning true to avoid app crash.
        return true
    }

    companion object {
        const val BRIDGE_JS_FILE = "braze-html-bridge.js"
        const val JAVASCRIPT_PREFIX = "javascript:"

        private const val BRAZE_SCHEME = "appboy"
        private const val AUTHORITY_NAME_CLOSE = "close"
        private const val AUTHORITY_NAME_NEWSFEED = "feed"
        private const val AUTHORITY_NAME_CUSTOM_EVENT = "customEvent"

        private const val BRAZE_CUSTOM_EVENT_NAME_KEY = "name"

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

        @JvmStatic
        fun parseCustomEventNameFromQueryBundle(queryBundle: Bundle): String? =
            queryBundle.getString(BRAZE_CUSTOM_EVENT_NAME_KEY)

        @JvmStatic
        fun parsePropertiesFromQueryBundle(queryBundle: Bundle): BrazeProperties {
            val customEventProperties = BrazeProperties()
            for (key in queryBundle.keySet()) {
                if (key != BRAZE_CUSTOM_EVENT_NAME_KEY) {
                    val propertyValue = queryBundle.getString(key, null)
                    if (!propertyValue.isNullOrBlank()) {
                        customEventProperties.addProperty(key, propertyValue)
                    }
                }
            }
            return customEventProperties
        }
    }
}
