package com.braze.ui.inappmessage.utils

import android.content.Context
import com.braze.models.inappmessage.IInAppMessage
import com.braze.ui.inappmessage.listeners.IInAppMessageWebViewClientListener
import com.braze.ui.BrazeWebViewClient

/**
 * InAppMessageWebViewClient.
 *
 * @param context
 * @param inAppMessage The in-app message being displayed.
 * @param inAppMessageWebViewClientListener
 * @param assetDirectoryUrl The directory that preloaded assets are loaded in. Required when inAppMessage.messageType is
 * HTML_FULL. For all other message types, this must be null.
 */
open class InAppMessageWebViewClient(
    context: Context,
    private val inAppMessage: IInAppMessage,
    private val inAppMessageWebViewClientListener: IInAppMessageWebViewClientListener?,
    assetDirectoryUrl: String? = null
) : BrazeWebViewClient(
    context,
    Type.IN_APP_MESSAGE,
    inAppMessage,
    inAppMessageWebViewClientListener,
    null,
    assetDirectoryUrl
)
