package com.braze.ui.banners.jsinterface

import android.content.Context
import android.webkit.JavascriptInterface
import com.braze.Braze
import com.braze.ui.JavascriptInterfaceBase
import com.braze.support.BrazeLogger.Priority.I
import com.braze.support.BrazeLogger.brazelog

/**
 * BannerJavascriptInterface.
 *
 * @param context
 * @param placementId The placement ID of the banner.
 * @param setHeightCallback a callback to set the height of the banner. You must convert to absolute pixels before calling this method.
 */
class BannerJavascriptInterface(
    context: Context,
    val placementId: String,
    val setHeightCallback: (Double) -> Unit = {}
) : JavascriptInterfaceBase(context) {

    @get:JavascriptInterface
    val user: BannerUserJavascriptInterface = BannerUserJavascriptInterface(context)

    @JavascriptInterface
    override fun logButtonClick(buttonId: String?) {
        brazelog(I) { "Banner logButtonClick() called. Logging banner click with button ID." }
        Braze.getInstance(context).logBannerClick(placementId, buttonId)
    }

    @JavascriptInterface
    override fun logClick() {
        brazelog(I) { "Banner logClick() called. Logging banner click without button ID." }
        Braze.getInstance(context).logBannerClick(placementId, null)
    }

    @JavascriptInterface
    fun setBannerHeight(height: Double) {
        // Height must be a finite number and not NaN
        if (height.isInfinite() || height.isNaN() || height < 0) {
            brazelog {
                "Banner setBannerHeight($height) called with invalid height. Height must be a finite number, not NaN, and greater or equal to 0."
            }
            return
        }
        brazelog(I) { "Banner setBannerHeight($height) called." }
        setHeightCallback(height)
    }
}
