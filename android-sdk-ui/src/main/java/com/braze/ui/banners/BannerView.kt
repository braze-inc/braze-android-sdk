package com.braze.ui.banners

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Base64
import android.webkit.WebView
import com.braze.Braze
import com.braze.BrazeInternal
import com.braze.managers.IBannerView
import com.braze.ui.R
import com.braze.ui.banners.jsinterface.BannerJavascriptInterface
import com.braze.ui.banners.listeners.DefaultBannerWebViewClientListener
import com.braze.ui.banners.utils.BannerWebViewClient

/**
 * An Android View that displays a Braze banner.
 */
class BannerView : WebView, IBannerView {
    private var _placementId: String? = null
    private var loadedHtml: String? = null
    private var currentUserId: String? = null

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
        init(null, 0)
    }

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        setBackgroundColor(Color.TRANSPARENT)

        // Load attributes
        val attributes = context.obtainStyledAttributes(
            attrs, R.styleable.BannerView, defStyle, 0
        )

        if (attributes.hasValue(R.styleable.BannerView_placementId)) {
            _placementId = attributes.getString(
                R.styleable.BannerView_placementId
            )
        }

        attributes.recycle()

        initBanner(placementId)
    }

    private fun configureWebView(placementId: String) {
        settings.javaScriptEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.displayZoomControls = false
        settings.domStorageEnabled = true
        settings.allowFileAccess = false

        // This enables hardware acceleration if the manifest also has it defined.
        // If not defined, then the layer type will fallback to software.
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(Color.TRANSPARENT)

        val defaultBannerWebViewClientListener = DefaultBannerWebViewClientListener()
        webViewClient = BannerWebViewClient(context, defaultBannerWebViewClientListener)

        // Add the BannerJavascriptInterface to the WebView
        addJavascriptInterface(BannerJavascriptInterface(context, placementId, internalHeightCallback), "brazeInternalBridge")
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
}
