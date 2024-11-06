package com.braze.ui.inappmessage.jsinterface

import android.content.Context
import android.webkit.JavascriptInterface
import com.braze.coroutine.BrazeCoroutineScope
import com.braze.models.inappmessage.IInAppMessageHtml
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.brazelog
import com.braze.support.requestPushPermissionPrompt
import com.braze.ui.JavascriptInterfaceBase
import com.braze.ui.inappmessage.BrazeInAppMessageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/**
 * Used to generate the javascript API in html in-app messages.
 */
class InAppMessageJavascriptInterface(
    context: Context,
    private val inAppMessage: IInAppMessageHtml
) : JavascriptInterfaceBase(context) {

    @get:JavascriptInterface
    val user: InAppMessageUserJavascriptInterface = InAppMessageUserJavascriptInterface(context)

    /**
     * Tracks whether `brazeBridge.closeMessage()` was called on this interface via
     * `onCloseMessageCalled()`.
     */
    var wasCloseMessageCalled = false

    @JavascriptInterface
    override fun logButtonClick(buttonId: String?) {
        buttonId?.let { inAppMessage.logButtonClick(it) }
    }

    @JavascriptInterface
    override fun logClick() {
        inAppMessage.logClick()
    }

    @JavascriptInterface
    fun beforeMessageClosed() {
        wasCloseMessageCalled = true
    }

    @JavascriptInterface
    fun requestPushPermission() {
        BrazeInAppMessageManager.getInstance().shouldNextUnregisterBeSkipped = true
        BrazeCoroutineScope.launchDelayed(PUSH_PROMPT_INITIAL_DELAY_MS) {
            if (wasCloseMessageCalled) {
                withTimeout(PUSH_PROMPT_WAIT_FOR_DISPLAY_TIMEOUT_MS) {
                    brazelog(V) { "Waiting for IAM to be fully closed before requesting push prompt" }
                    while (BrazeInAppMessageManager.getInstance().isCurrentlyDisplayingInAppMessage) {
                        delay(PUSH_PROMPT_WAIT_DELAY_TIMEOUT_MS)
                    }
                }
            }

            brazelog(V) { "Requesting push prompt from Braze bridge html interface" }
            BrazeInAppMessageManager.getInstance().activity.requestPushPermissionPrompt()
        }
    }

    companion object {
        private const val PUSH_PROMPT_INITIAL_DELAY_MS = 75L
        private const val PUSH_PROMPT_WAIT_FOR_DISPLAY_TIMEOUT_MS = 2500L
        private const val PUSH_PROMPT_WAIT_DELAY_TIMEOUT_MS = 25L
    }
}
