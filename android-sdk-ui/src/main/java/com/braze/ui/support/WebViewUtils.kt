package com.braze.ui.support

import android.annotation.SuppressLint
import android.content.Context
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

    try {
        // Note that this check is OS version agnostic since the Android WebView can be
        // updated independently
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) &&
            isDeviceInNightMode(context)
        ) {
            WebSettingsCompat.setForceDark(
                settings,
                WebSettingsCompat.FORCE_DARK_ON,
            )
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            WebSettingsCompat.setForceDarkStrategy(
                settings,
                WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY,
            )
        }
    } catch (e: Throwable) {
        brazelog(TAG, E, e) { "Failed to set dark mode WebView settings." }
    }
}
