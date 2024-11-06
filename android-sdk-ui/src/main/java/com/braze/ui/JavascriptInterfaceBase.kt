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

abstract class JavascriptInterfaceBase(
    protected val context: Context
) {

    @JavascriptInterface
    abstract fun logButtonClick(buttonId: String?)

    @JavascriptInterface
    abstract fun logClick()

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
