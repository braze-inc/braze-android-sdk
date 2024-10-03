package com.braze.ui.inappmessage.jsinterface

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.annotation.VisibleForTesting
import com.braze.Braze
import com.braze.coroutine.BrazeCoroutineScope
import com.braze.models.inappmessage.IInAppMessageHtml
import com.braze.models.outgoing.BrazeProperties
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.Priority.W
import com.braze.support.BrazeLogger.brazelog
import com.braze.support.requestPushPermissionPrompt
import com.braze.ui.inappmessage.BrazeInAppMessageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.math.BigDecimal

/**
 * Used to generate the javascript API in html in-app messages.
 */
class InAppMessageJavascriptInterface(
    private val context: Context,
    private val inAppMessage: IInAppMessageHtml
) {
    @get:JavascriptInterface
    val user: InAppMessageUserJavascriptInterface = InAppMessageUserJavascriptInterface(context)

    /**
     * Tracks whether `brazeBridge.closeMessage()` was called on this interface via
     * `onCloseMessageCalled()`.
     */
    var wasCloseMessageCalled = false

    @JavascriptInterface
    fun changeUser(userId: String, sdkAuthSignature: String?) {
        Braze.getInstance(context).changeUser(userId, sdkAuthSignature)
    }

    @JavascriptInterface
    fun requestImmediateDataFlush() {
        Braze.getInstance(context).requestImmediateDataFlush()
    }

    @JavascriptInterface
    fun logCustomEventWithJSON(eventName: String?, propertiesJSON: String?) {
        val brazeProperties = parseProperties(propertiesJSON)
        Braze.getInstance(context).logCustomEvent(eventName, brazeProperties)
    }

    @JavascriptInterface
    fun logPurchaseWithJSON(
        productId: String?,
        price: String,
        currencyCode: String?,
        quantity: String,
        propertiesJSON: String?
    ) {
        val brazeProperties = parseProperties(propertiesJSON)
        val priceValue = price.toDoubleOrNull()
        if (priceValue == null) {
            brazelog(W) { "Failed to parse logPurchaseWithJSON price value '$price'" }
            return
        }

        val quantityValue = quantity.toIntOrNull()
        if (quantityValue == null) {
            brazelog(W) { "Failed to parse logPurchaseWithJSON quantity value '$quantity'" }
            return
        }

        Braze.getInstance(context).logPurchase(
            productId,
            currencyCode,
            BigDecimal(priceValue.toString()),
            quantityValue,
            brazeProperties
        )
    }

    @JavascriptInterface
    fun logButtonClick(buttonId: String?) {
        buttonId?.let { inAppMessage.logButtonClick(it) }
    }

    @JavascriptInterface
    fun logClick() {
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

    @VisibleForTesting
    fun parseProperties(propertiesJSON: String?): BrazeProperties? {
        try {
            if (propertiesJSON != null && propertiesJSON != "undefined"
                && propertiesJSON != "null"
            ) {
                return BrazeProperties(JSONObject(propertiesJSON))
            }
        } catch (e: Exception) {
            brazelog(E, e) { "Failed to parse properties JSON String: $propertiesJSON" }
        }
        return null
    }

    companion object {
        private const val PUSH_PROMPT_INITIAL_DELAY_MS = 75L
        private const val PUSH_PROMPT_WAIT_FOR_DISPLAY_TIMEOUT_MS = 2500L
        private const val PUSH_PROMPT_WAIT_DELAY_TIMEOUT_MS = 25L
    }
}
