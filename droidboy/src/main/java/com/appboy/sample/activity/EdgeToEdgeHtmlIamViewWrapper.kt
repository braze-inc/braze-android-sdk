package com.appboy.sample.activity

import android.view.View
import android.view.animation.Animation
import androidx.core.view.WindowInsetsCompat
import com.braze.configuration.BrazeConfigurationProvider
import com.braze.models.inappmessage.IInAppMessage
import com.braze.ui.inappmessage.DefaultInAppMessageViewWrapper
import com.braze.ui.inappmessage.listeners.IInAppMessageViewLifecycleListener

internal class EdgeToEdgeHtmlIamViewWrapper(
    inAppMessageView: View,
    inAppMessage: IInAppMessage,
    inAppMessageViewLifecycleListener: IInAppMessageViewLifecycleListener,
    configurationProvider: BrazeConfigurationProvider,
    openingAnimation: Animation?,
    closingAnimation: Animation?,
    clickableInAppMessageView: View?,
) : DefaultInAppMessageViewWrapper(
        inAppMessageView,
        inAppMessage,
        inAppMessageViewLifecycleListener,
        configurationProvider,
        openingAnimation,
        closingAnimation,
        clickableInAppMessageView,
    ) {
    override fun resolveWindowInsetsForInAppMessage(
        inAppMessageView: View,
        insets: WindowInsetsCompat,
    ): WindowInsetsCompat = EdgeToEdgeHtmlIamInsets.resolveIamWindowInsets(inAppMessageView.context, insets)
}
