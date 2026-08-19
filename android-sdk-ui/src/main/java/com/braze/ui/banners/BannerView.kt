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
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.core.content.withStyledAttributes
import com.braze.Braze
import com.braze.BrazeInternal
import com.braze.coroutine.BrazeCoroutineScope
import com.braze.events.BannerDismissedEvent
import com.braze.events.IEventSubscriber
import com.braze.managers.banners.IBannerView
import com.braze.models.Banner
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.Priority.W
import com.braze.support.BrazeLogger.brazelog
import com.braze.ui.R
import com.braze.ui.banners.jsinterface.BannerJavascriptInterface
import com.braze.ui.banners.listeners.DefaultBannerWebViewClientListener
import com.braze.ui.banners.utils.BannerWebViewClient
import com.braze.ui.support.setWebViewSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * An Android View that displays a Braze banner.
 */
class BannerView :
    WebView,
    IBannerView {
    private var _placementId: String? = null
    private var loadedHtml: String? = null
    private var currentUserId: String? = null
    private val isDismissed = AtomicBoolean(false)
    private val isDestroyed = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val initJobLock = Any()
    private var initJob: Job? = null

    /**
     * Latest resolved banner identity from [initBanner] when banner data was present. Held in an
     * [AtomicReference] because [initBanner] may run off the main thread while dismiss runs on the
     * main thread.
     */
    private val dismissSnapshot = AtomicReference<PendingBannerDismissSnapshot?>(null)

    /**
     * Callback invoked when the banner is dismissed (e.g. via Braze bridge closeMessage).
     * Set by integrators to run custom logic when the banner is dismissed.
     * Invoked after the view's visibility is set to [GONE] and its WebView processing is paused.
     * The view remains in the hierarchy so it can be reused if new content is loaded.
     *
     * @param snapshot [BannerDismissSnapshot] with placement, stable key, and tracking id.
     */
    var onDismissCallback: ((BannerDismissSnapshot) -> Unit)? = null

    private val dismissSubscriber =
        IEventSubscriber<BannerDismissedEvent> { event ->
            if (event.placementId == _placementId) {
                dismiss()
            }
        }

    private val attachStateListener =
        object : OnAttachStateChangeListener {
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
            // Compose AndroidView `update` often reassigns the same placementId after factory
            // construction. Skip so we do not cancel an in-flight resolve for the same placement.
            if (_placementId == value) return
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

    private fun init(
        attrs: AttributeSet?,
        defStyle: Int,
    ) {
        setBackgroundColor(Color.TRANSPARENT)

        // Load attributes
        context.withStyledAttributes(
            attrs,
            R.styleable.BannerView,
            defStyle,
            0,
        ) {
            if (hasValue(R.styleable.BannerView_placementId)) {
                _placementId =
                    getString(
                        R.styleable.BannerView_placementId,
                    )
            }
        }

        initBanner(placementId)
    }

    /**
     * Tears down banner resources for embedders that destroy the underlying [WebView].
     * Unregisters from banner monitoring and ensures pending async HTML loads no-op safely.
     * Idempotent — safe to call more than once.
     */
    override fun destroy() {
        if (!isDestroyed.compareAndSet(false, true)) return
        cancelPendingInit()
        mainHandler.removeCallbacksAndMessages(null)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            teardownForRelease()
            super.destroy()
        } else {
            mainHandler.post {
                teardownForRelease()
                super.destroy()
            }
        }
    }

    @MainThread
    private fun teardownForRelease() {
        BrazeInternal.removeBannerViewMonitor(this)
        BrazeInternal.unsubscribeFromBannersDismissedEvent(context, dismissSubscriber)
        removeOnAttachStateChangeListener(attachStateListener)
        heightCallback = null
        onDismissCallback = null
        try {
            stopLoading()
            removeJavascriptInterface(JS_BRIDGE_NAME)
            webViewClient = WebViewClient()
        } catch (e: Exception) {
            brazelog(E, e) {
                "Banner release: error during WebView teardown for placementId=$_placementId"
            }
        }
    }

    @MainThread
    private fun configureWebView(placementId: String) {
        setWebViewSettings(settings, context)

        // This enables hardware acceleration if the manifest also has it defined.
        // If not defined, then the layer type will fallback to software.
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(Color.TRANSPARENT)

        webViewClient = BannerWebViewClient(context, createBannerWebViewClientListener(placementId))

        addJavascriptInterface(
            BannerJavascriptInterface(
                context = context,
                placementId = placementId,
                setHeightCallback = internalHeightCallback,
            ),
            JS_BRIDGE_NAME,
        )
    }

    /**
     * Creates the [IBannerWebViewClientListener] used by this view's [BannerWebViewClient].
     * Overrides [DefaultBannerWebViewClientListener.onCloseAction] so that `appboy://close`
     * URL intercepts trigger [dismiss].
     */
    @VisibleForTesting
    internal fun createBannerWebViewClientListener(placementId: String): DefaultBannerWebViewClientListener =
        object : DefaultBannerWebViewClientListener(placementId) {
            override fun onCloseAction(
                context: Context,
                url: String,
                queryBundle: Bundle,
            ) {
                super.onCloseAction(context, url, queryBundle)
                dismiss()
            }
        }

    /**
     * Resolves banner data off the calling thread, then applies WebView updates on the main thread.
     *
     * [Braze.getBanner] uses a blocking serial dispatcher and may perform disk-backed SDK-enablement
     * reads. Calling it inline from a View setter or Compose `AndroidView` factory on the main thread
     * can ANR; this method never invokes that path on the main thread.
     */
    override fun initBanner(placementId: String?) {
        if (isDestroyed.get()) return
        // BannersManager may call initBanner with a prior monitor placement after the view's
        // placementId has already changed. Ignore those so we do not cancel the newer resolve.
        if (placementId != _placementId) {
            brazelog(V) {
                "Ignoring initBanner for placementId=$placementId; current placementId=$_placementId"
            }
            return
        }
        if (placementId == null) {
            cancelPendingInit()
            runOnMainThread {
                performInitBanner(placementId = null, banner = null)
            }
            return
        }
        synchronized(initJobLock) {
            initJob?.cancel()
            brazelog(V) { "Resolving Banner off main thread for placementId=$placementId" }
            initJob =
                BrazeCoroutineScope.launch {
                    if (isDestroyed.get()) return@launch
                    val banner =
                        try {
                            resolveBanner(placementId)
                        } catch (e: Exception) {
                            brazelog(E, e) {
                                "Failed to resolve Banner for placementId=$placementId"
                            }
                            null
                        }
                    brazelog(V) {
                        "Banner resolve finished for placementId=$placementId; " +
                            "found=${banner != null}"
                    }
                    if (!isActive || isDestroyed.get()) {
                        brazelog(V) {
                            "Skipping Banner apply for placementId=$placementId; " +
                                "init cancelled or view destroyed"
                        }
                        return@launch
                    }
                    runOnMainThread {
                        // Drop stale results if placementId changed while resolving.
                        if (placementId != _placementId) {
                            brazelog(V) {
                                "Dropping stale Banner resolve for placementId=$placementId; " +
                                    "current placementId=$_placementId"
                            }
                            return@runOnMainThread
                        }
                        performInitBanner(placementId, banner)
                    }
                }
        }
    }

    /**
     * Looks up the cached Banner for [placementId]. Overridable in tests to assert calling thread.
     */
    @VisibleForTesting
    internal fun resolveBanner(placementId: String): Banner? = Braze.getInstance(context).getBanner(placementId)

    @MainThread
    private fun performInitBanner(
        placementId: String?,
        banner: Banner?,
    ) {
        if (banner == null) {
            dismissSnapshot.set(null)
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

        dismissSnapshot.set(
            PendingBannerDismissSnapshot(
                placementId = banner.placementId,
                stableKey = banner.stableKey,
                trackingId = banner.trackingId,
            ),
        )

        // Don't reload if the HTML is the same
        if (banner.html != loadedHtml || banner.userId != currentUserId) {
            loadedHtml = banner.html
            currentUserId = banner.userId
            if (banner.isControl) {
                setWebviewToEmpty()
            } else {
                loadHtmlData(banner.placementId)
            }
            BrazeInternal.addBannerViewMonitor(banner.placementId, this, skipImpressionMonitoring = false)
        }
    }

    /**
     * Cancels any in-flight [initBanner] resolve job.
     */
    private fun cancelPendingInit() {
        synchronized(initJobLock) {
            initJob?.cancel()
            initJob = null
        }
    }

    @MainThread
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
                "base64",
            )
            invalidate()
        }
    }

    @MainThread
    private fun setWebviewToEmpty() {
        loadedHtml = null
        loadData("", "text/html", "base64")
        invalidate()
        internalHeightCallback(0.0)
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
        if (isDestroyed.get()) return
        if (!isDismissed.compareAndSet(false, true)) return
        runDismissOnMainThread {
            performDismissTeardown()
        }
    }

    /**
     * Performs the actual teardown. Must be called on the main thread.
     */
    @MainThread
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
            fireOnDismissCallback()
            brazelog(V) { "Banner dismiss completed. placementId=$_placementId" }
        } catch (e: Exception) {
            brazelog(E, e) {
                "Banner dismiss: error during view teardown or onDismissCallback for placementId=$_placementId"
            }
        }
    }

    @MainThread
    private fun fireOnDismissCallback() {
        val callback = onDismissCallback ?: return
        val cached = dismissSnapshot.get()
        val placementId = cached?.placementId ?: _placementId
        val stableKey = cached?.stableKey
        val trackingId = cached?.trackingId
        val snapshot =
            BannerDismissSnapshot.fromNullableFields(
                placementId = placementId,
                stableKey = stableKey,
                trackingId = trackingId,
            )
        if (snapshot == null) {
            brazelog(W) {
                "Banner dismiss callback skipped because required snapshot fields were missing. " +
                    "placementId=$placementId stableKey=$stableKey trackingId=$trackingId"
            }
            return
        }
        callback.invoke(snapshot)
    }

    private inline fun runOnMainThread(crossinline block: () -> Unit) {
        if (isDestroyed.get()) return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (isDestroyed.get()) return
            block()
        } else {
            mainHandler.post {
                if (isDestroyed.get()) return@post
                block()
            }
        }
    }

    private inline fun runDismissOnMainThread(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (!isDismissed.get() || isDestroyed.get()) return
            block()
        } else {
            mainHandler.post {
                if (!isDismissed.get() || isDestroyed.get()) return@post
                block()
            }
        }
    }

    private companion object {
        private const val JS_BRIDGE_NAME = "brazeInternalBridge"
    }
}

private data class PendingBannerDismissSnapshot(
    val placementId: String?,
    val stableKey: String?,
    val trackingId: String?,
)
