package com.braze.ui.inappmessage.views

import android.content.Context
import android.util.AttributeSet
import com.braze.ui.R

/**
 * View for rendering non-full-screen HTML in-app messages. Extends [InAppMessageHtmlBaseView]
 * and binds to the standard HTML WebView layout resource.
 */
open class InAppMessageHtmlView(
    context: Context?,
    attrs: AttributeSet?,
) : InAppMessageHtmlBaseView(context, attrs) {
    override fun getWebViewViewId(): Int = R.id.com_braze_inappmessage_html_webview
}
