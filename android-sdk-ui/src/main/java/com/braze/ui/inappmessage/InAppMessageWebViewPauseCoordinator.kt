package com.braze.ui.inappmessage

import android.webkit.WebView
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.brazelog
import com.braze.ui.inappmessage.views.InAppMessageHtmlBaseView

/**
 * Defers [WebView.onPause] for displayed HTML in-app messages so lifecycle callbacks return promptly.
 */
internal class InAppMessageWebViewPauseCoordinator(
    private val viewWrapperProvider: () -> IInAppMessageViewWrapper?,
) {
    /**
     * Runnable posted to [pendingWebViewPauseTarget] that invokes [WebView.onPause] for a displayed
     * HTML in-app message. Cleared when the runnable runs or when [cancelPendingWebViewPause] runs.
     */
    var pendingWebViewPauseRunnable: Runnable? = null

    /**
     * [WebView] that owns the message queue for [pendingWebViewPauseRunnable]. Used to post the
     * deferred pause and to call [WebView.removeCallbacks] when pausing is cancelled on resume or
     * teardown.
     */
    private var pendingWebViewPauseTarget: WebView? = null

    fun pauseWebviewIfNecessary() {
        brazelog(V) { "Scheduling deferred InAppMessage WebView pause via pendingWebViewPauseRunnable" }
        val viewWrapper = viewWrapperProvider() ?: return
        val inAppMessageView = viewWrapper.inAppMessageView
        if (inAppMessageView !is InAppMessageHtmlBaseView) {
            return
        }
        val webView = inAppMessageView.messageWebView ?: return

        cancelPendingWebViewPause()

        val pauseRunnable =
            Runnable {
                brazelog(V) { "pendingWebViewPauseRunnable running" }
                pendingWebViewPauseRunnable = null
                pendingWebViewPauseTarget = null
                val currentWrapper = viewWrapperProvider()
                if (currentWrapper == null) {
                    brazelog(V) {
                        "pendingWebViewPauseRunnable finished without calling WebView.onPause: in-app message no longer displayed"
                    }
                    return@Runnable
                }
                val currentView = currentWrapper.inAppMessageView
                if (currentView is InAppMessageHtmlBaseView && currentView.messageWebView === webView) {
                    brazelog(V) { "pendingWebViewPauseRunnable calling WebView.onPause" }
                    // Assumed blocking on a busy WebView; deferred via post() so onActivityPaused returns first.
                    webView.onPause()
                } else {
                    brazelog(V) {
                        "pendingWebViewPauseRunnable finished without calling WebView.onPause: HTML WebView no longer matches"
                    }
                }
            }
        pendingWebViewPauseRunnable = pauseRunnable
        pendingWebViewPauseTarget = webView
        webView.post(pauseRunnable)
    }

    fun resumeWebviewIfNecessary() {
        brazelog { "Resuming InAppMessage WebView" }
        cancelPendingWebViewPause()
        val inAppMessageViewWrapper = viewWrapperProvider() ?: return
        val inAppMessageView = inAppMessageViewWrapper.inAppMessageView
        if (inAppMessageView is InAppMessageHtmlBaseView) {
            inAppMessageView.messageWebView?.onResume()
        }
    }

    fun cancelPendingWebViewPause() {
        val pauseRunnable = pendingWebViewPauseRunnable
        val webView = pendingWebViewPauseTarget
        if (pauseRunnable != null && webView != null) {
            brazelog(V) { "Cancelling pendingWebViewPauseRunnable before it runs WebView.onPause" }
            webView.removeCallbacks(pauseRunnable)
        }
        pendingWebViewPauseRunnable = null
        pendingWebViewPauseTarget = null
    }
}
