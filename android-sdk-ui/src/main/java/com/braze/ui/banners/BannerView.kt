package com.braze.ui.banners

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Base64
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.VisibleForTesting
import androidx.core.content.withStyledAttributes
import com.braze.Braze
import com.braze.BrazeInternal
import com.braze.events.BannerDismissedEvent
import com.braze.events.IEventSubscriber
import com.braze.managers.banners.IBannerView
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.brazelog
import com.braze.ui.R
import com.braze.ui.banners.jsinterface.BannerJavascriptInterface
import com.braze.ui.banners.listeners.DefaultBannerWebViewClientListener
import com.braze.ui.banners.utils.BannerWebViewClient
import com.braze.ui.support.setWebViewSettings
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An Android View that displays a Braze banner.
 */
class BannerView : WebView, IBannerView {
    private var _placementId: String? = null
    private var loadedHtml: String? = null
    private var currentUserId: String? = null
    private val isDismissed = AtomicBoolean(false)

    /**
     * Callback invoked when the banner is dismissed (e.g. via Braze bridge closeMessage).
     * Set by integrators to run custom logic when the banner is dismissed.
     * Invoked after the view's visibility is set to [GONE] and its WebView processing is paused.
     * The view remains in the hierarchy so it can be reused if new content is loaded.
     */
    var onDismissCallback: (() -> Unit)? = null

    private val dismissSubscriber = IEventSubscriber<BannerDismissedEvent> { event ->
        if (event.placementId == _placementId) {
            dismiss()
        }
    }

    private val attachStateListener = object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            BrazeInternal.subscribeToBannersDismissedEvent(context, dismissSubscriber)
        }

        override fun onViewDetachedFromWindow(v: View) {
            BrazeInternal.unsubscribeFromBannersDismissedEvent(context, dismissSubscriber)
        }
    }

    var placementId: String?
        get() = _placementId
        set(value) {
            _placementId = value
            initBanner(value)
        }

    /**
     * A callback that is called when the height of the banner changes.
     * It is used where a parent view needs to know the new height of the banner.
     * The height is in dp. It may or may not need to be converted to pixels before using it
     * depending on the subsequent interface that will use the height.
     */
    var heightCallback: ((Double) -> Unit)? = null

    // Make this a constant so the heightCallback can be set at anytime, not just during initialization.
    private val internalHeightCallback: (Double) -> Unit = { height ->
        heightCallback?.invoke(height)
    }

    // This constructor is specifically for the Jetpack integration so the placement ID can be passed in immediately.
    constructor(context: Context, placementId: String?) : super(context) {
        _placementId = placementId
        addOnAttachStateChangeListener(attachStateListener)
        init(null, 0)
    }

    constructor(context: Context) : super(context) {
        addOnAttachStateChangeListener(attachStateListener)
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        addOnAttachStateChangeListener(attachStateListener)
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        addOnAttachStateChangeListener(attachStateListener)
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        setBackgroundColor(Color.TRANSPARENT)

        // Load attributes
        context.withStyledAttributes(
            attrs, R.styleable.BannerView, defStyle, 0
        ) {

            if (hasValue(R.styleable.BannerView_placementId)) {
                _placementId = getString(
                    R.styleable.BannerView_placementId
                )
            }
        }

        initBanner(placementId)
    }

    private fun configureWebView(placementId: String) {
        setWebViewSettings(settings, context)

        // This enables hardware acceleration if the manifest also has it defined.
        // If not defined, then the layer type will fallback to software.
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(Color.TRANSPARENT)

        webViewClient = BannerWebViewClient(context, createBannerWebViewClientListener())

        addJavascriptInterface(
            BannerJavascriptInterface(
                context = context,
                placementId = placementId,
                setHeightCallback = internalHeightCallback
            ),
            JS_BRIDGE_NAME
        )
    }

    /**
     * Creates the [IBannerWebViewClientListener] used by this view's [BannerWebViewClient].
     * Overrides [DefaultBannerWebViewClientListener.onCloseAction] so that `appboy://close`
     * URL intercepts trigger [dismiss].
     */
    @VisibleForTesting
    internal fun createBannerWebViewClientListener(): DefaultBannerWebViewClientListener =
        object : DefaultBannerWebViewClientListener() {
            override fun onCloseAction(context: Context, url: String, queryBundle: Bundle) {
                dismiss()
            }
        }

    override fun initBanner(placementId: String?) {
        val banner = placementId?.let { Braze.getInstance(context).getBanner(it) }
        if (banner == null) {
            currentUserId = null
            setWebviewToEmpty()
            if (placementId != null) {
                // If we don't have the banner data yet, still register it so that on sync of the banner, the
                // [BannerManager] will be able to update the view. But we do want to skip monitoring for the impression
                // since there's no banner campaign to report.
                BrazeInternal.addBannerViewMonitor(placementId, this, skipImpressionMonitoring = true)
            }
            return
        }

        // Don't reload if the HTML is the same
        if (banner.html != loadedHtml || banner.userId != currentUserId) {
            loadedHtml = banner.html
            currentUserId = banner.userId
            if (banner.isControl) {
                setWebviewToEmpty()
            } else {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    Handler(Looper.getMainLooper()).post {
                        loadHtmlData(placementId)
                    }
                } else {
                    loadHtmlData(placementId)
                }
            }
            BrazeInternal.addBannerViewMonitor(banner.placementId, this, skipImpressionMonitoring = false)
        }
    }

    private fun loadHtmlData(placementId: String) {
        val wasDismissed = isDismissed.getAndSet(false)
        if (wasDismissed) {
            onResume()
            settings.javaScriptEnabled = true
            visibility = VISIBLE
        }
        configureWebView(placementId)
        loadedHtml?.let { html ->
            loadData(
                Base64.encodeToString(html.toByteArray(), Base64.NO_PADDING).orEmpty(),
                "text/html",
                "base64"
            )
            invalidate()
        }
    }

    private fun setWebviewToEmpty() {
        loadedHtml = null

        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                loadData("", "text/html", "base64")
                invalidate()
                internalHeightCallback(0.0)
            }
        } else {
            loadData("", "text/html", "base64")
            invalidate()
            internalHeightCallback(0.0)
        }
    }

    /**
     * Shuts down the WebView and hides it via [GONE]. Stops any
     * in-flight load, clears content, disables JavaScript, removes
     * the JS bridge/client, and pauses all internal WebView processing
     * so the view consumes zero CPU while dismissed.
     *
     * Runs teardown inline when already on the UI thread. When called
     * from a background thread, posts to the main looper and re-checks
     * [isDismissed] before executing, so a reload that occurred in the
     * interim is not clobbered by a stale dismiss runnable.
     *
     * Guarded by [isDismissed] so duplicate calls (e.g. from both the
     * `appboy://close` URL intercept and the [BannerDismissedEvent]
     * subscriber) are safe.
     */
    private fun dismiss() {
        if (!isDismissed.compareAndSet(false, true)) return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            performDismissTeardown()
        } else {
            Handler(Looper.getMainLooper()).post {
                if (!isDismissed.get()) return@post
                performDismissTeardown()
            }
        }
    }

    /**
     * Performs the actual teardown. Must be called on the main thread.
     */
    private fun performDismissTeardown() {
        try {
            stopLoading()
            setWebviewToEmpty()
            clearHistory()
            settings.javaScriptEnabled = false
            removeJavascriptInterface(JS_BRIDGE_NAME)
            webViewClient = WebViewClient()
            onPause()
            visibility = GONE
            onDismissCallback?.invoke()
            brazelog(V) { "Banner dismiss completed. placementId=$_placementId" }
        } catch (e: Exception) {
            brazelog(E, e) {
                "Banner dismiss: error during view teardown or onDismissCallback for placementId=$_placementId"
            }
        }
    }

    private companion object {
        private const val JS_BRIDGE_NAME = "brazeInternalBridge"
    }
}
