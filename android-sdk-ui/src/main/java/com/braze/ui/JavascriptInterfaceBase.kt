package com.braze.ui

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.annotation.VisibleForTesting
import com.braze.Braze
import com.braze.models.outgoing.BrazeProperties
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.Priority.W
import com.braze.support.BrazeLogger.brazelog
import org.json.JSONObject
import java.math.BigDecimal

/**
 * Base class for JavaScript bridge interfaces exposed to Braze WebViews.
 * Provides common SDK operations (user changes, event logging, purchases,
 * data flushing) that can be invoked from HTML/JavaScript content.
 *
 * @param context The Android [Context] for Braze SDK access.
 */
abstract class JavascriptInterfaceBase(
    protected val context: Context
) {

    /**
     * Logs a button click for the given button identifier.
     *
     * @param buttonId The identifier of the clicked button, or null if unavailable.
     */
    @JavascriptInterface
    abstract fun logButtonClick(buttonId: String?)

    /**
     * Logs a click on the message associated with this JavaScript interface.
     */
    @JavascriptInterface
    abstract fun logClick()

    /**
     * Changes the current Braze user.
     *
     * @param userId The user ID to switch to.
     * @param sdkAuthSignature Optional SDK authentication signature.
     */
    @JavascriptInterface
    fun changeUser(userId: String, sdkAuthSignature: String?) {
        Braze.getInstance(context).changeUser(userId, sdkAuthSignature)
    }

    /**
     * Requests an immediate flush of any pending analytics data to the Braze servers.
     */
    @JavascriptInterface
    fun requestImmediateDataFlush() {
        Braze.getInstance(context).requestImmediateDataFlush()
    }

    /**
     * Logs a custom event with optional JSON-encoded properties.
     *
     * @param eventName The name of the custom event.
     * @param propertiesJSON Optional JSON string of event properties.
     */
    @JavascriptInterface
    fun logCustomEventWithJSON(eventName: String?, propertiesJSON: String?) {
        val brazeProperties = parseProperties(propertiesJSON)
        Braze.getInstance(context).logCustomEvent(eventName, brazeProperties)
    }

    /**
     * Logs a purchase event with the specified product, price, currency, quantity,
     * and optional JSON-encoded properties.
     *
     * @param productId The product identifier.
     * @param price The price as a string representation of a decimal number.
     * @param currencyCode The ISO 4217 currency code.
     * @param quantity The quantity purchased as a string representation of an integer.
     * @param propertiesJSON Optional JSON string of purchase properties.
     */
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
}
