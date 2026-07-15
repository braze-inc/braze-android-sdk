package com.braze.ui.inappmessage

import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import androidx.core.view.WindowInsetsCompat
import com.braze.ui.support.getMaxSafeBottomInset
import com.braze.ui.support.getMaxSafeLeftInset
import com.braze.ui.support.getMaxSafeRightInset
import com.braze.ui.support.getMaxSafeTopInset

/**
 * Injects Android window safe-area insets into HTML in-app message WebViews as CSS custom properties.
 */
object HtmlInAppMessageSafeAreaInjector {
    const val CSS_PROPERTY_TOP = "--braze-safe-area-inset-top"
    const val CSS_PROPERTY_RIGHT = "--braze-safe-area-inset-right"
    const val CSS_PROPERTY_BOTTOM = "--braze-safe-area-inset-bottom"
    const val CSS_PROPERTY_LEFT = "--braze-safe-area-inset-left"
    const val SAFE_AREA_INSETS_CHANGED_EVENT = "brazeSafeAreaInsetsChanged"

    fun inject(
        webView: WebView,
        insets: WindowInsetsCompat,
    ) {
        val javascript =
            buildInjectionJavascript(
                topPx = getMaxSafeTopInset(insets),
                rightPx = getMaxSafeRightInset(insets),
                bottomPx = getMaxSafeBottomInset(insets),
                leftPx = getMaxSafeLeftInset(insets),
            )
        webView.evaluateJavascript(javascript, null)
    }

    @VisibleForTesting
    internal fun buildInjectionJavascript(
        topPx: Int,
        rightPx: Int,
        bottomPx: Int,
        leftPx: Int,
    ): String =
        """
        (function() {
          var root = document.documentElement;
          root.style.setProperty('$CSS_PROPERTY_TOP', '${topPx}px');
          root.style.setProperty('$CSS_PROPERTY_RIGHT', '${rightPx}px');
          root.style.setProperty('$CSS_PROPERTY_BOTTOM', '${bottomPx}px');
          root.style.setProperty('$CSS_PROPERTY_LEFT', '${leftPx}px');
          root.dispatchEvent(new CustomEvent('$SAFE_AREA_INSETS_CHANGED_EVENT', {
            detail: { top: $topPx, right: $rightPx, bottom: $bottomPx, left: $leftPx }
          }));
        })();
        """.trimIndent()
}
