package com.braze.ui.banners.jsinterface

import android.content.Context
import android.webkit.JavascriptInterface
import com.braze.Braze
import com.braze.ui.JavascriptInterfaceBase
import com.braze.support.BrazeLogger.Priority.I
import com.braze.support.BrazeLogger.brazelog

class BannerJavascriptInterface(context: Context, val placementId: String) : JavascriptInterfaceBase(context) {

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
}
