package com.braze.ui.support

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.webkit.WebSettings
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.brazelog
import com.braze.support.BrazeLogger.getBrazeLogTag

private val TAG = "WebViewUtils".getBrazeLogTag()

/**
 * Configures common [WebSettings] for Braze WebViews including JavaScript, DOM storage,
 * wide viewport, and dark mode support via AndroidX WebView compatibility APIs.
 *
 * @param settings The [WebSettings] to configure.
 * @param context The Android [Context] used for night mode detection.
 */
@SuppressLint("SetJavaScriptEnabled")
fun setWebViewSettings(
    settings: WebSettings,
    context: Context,
) {
    settings.javaScriptEnabled = true
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.displayZoomControls = false
    settings.domStorageEnabled = true
    settings.allowFileAccess = false

    if (isDeviceInNightMode(context)) {
        applyNightModeDarkening(settings)
    }
}

/**
 * Applies dark mode to [settings] for a device already known to be in night mode.
 *
 * Goal (unchanged across the FORCE_DARK deprecation): let WebView honor a page's own dark theme
 * (prefers-color-scheme) but never algorithmically invert custom in-app message / banner HTML that
 * lacks its own dark styling. This is achieved differently by platform level.
 */
private fun applyNightModeDarkening(settings: WebSettings) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+ (this SDK targets 33+): algorithmic darkening is already disallowed by
            // default, and WebView maps prefers-color-scheme to the app theme automatically. This
            // is exactly the web-theme-only behavior we want (a page's own dark theme is applied,
            // but content without dark styles is not color-inverted), so there is nothing to set.
            // The guard also prevents falling through to the deprecated FORCE_DARK path below.
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            // Pre-API-33: setAlgorithmicDarkeningAllowed(false) would map to FORCE_DARK_OFF and
            // disable dark reporting entirely. Instead enable Force Dark so WebView reports
            // prefers-color-scheme: dark, and restrict it to web-theme darkening only so
            // content without dark styles is not algorithmically inverted.
            @Suppress("DEPRECATION")
            WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                @Suppress("DEPRECATION")
                WebSettingsCompat.setForceDarkStrategy(
                    settings,
                    WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY,
                )
            }
        }
    } catch (e: Throwable) {
        brazelog(TAG, E, e) { "Failed to set dark mode WebView settings." }
    }
}
