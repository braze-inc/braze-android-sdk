package com.appboy.sample.activity

import android.view.View
import android.view.animation.Animation
import com.braze.configuration.BrazeConfigurationProvider
import com.braze.models.inappmessage.IInAppMessage
import com.braze.ui.inappmessage.IInAppMessageViewWrapper
import com.braze.ui.inappmessage.factories.DefaultInAppMessageViewWrapperFactory

internal class EdgeToEdgeHtmlIamViewWrapperFactory : DefaultInAppMessageViewWrapperFactory() {
    override fun createInAppMessageViewWrapper(
        inAppMessageView: View,
        inAppMessage: IInAppMessage,
        inAppMessageViewLifecycleListener: com.braze.ui.inappmessage.listeners.IInAppMessageViewLifecycleListener,
        configurationProvider: BrazeConfigurationProvider,
        openingAnimation: Animation?,
        closingAnimation: Animation?,
        clickableInAppMessageView: View?,
    ): IInAppMessageViewWrapper =
        EdgeToEdgeHtmlIamViewWrapper(
            inAppMessageView,
            inAppMessage,
            inAppMessageViewLifecycleListener,
            configurationProvider,
            openingAnimation,
            closingAnimation,
            clickableInAppMessageView,
        )

    override fun createInAppMessageViewWrapper(
        inAppMessageView: View,
        inAppMessage: IInAppMessage,
        inAppMessageViewLifecycleListener: com.braze.ui.inappmessage.listeners.IInAppMessageViewLifecycleListener,
        configurationProvider: BrazeConfigurationProvider,
        openingAnimation: Animation?,
        closingAnimation: Animation?,
        clickableInAppMessageView: View?,
        buttons: List<View>?,
        closeButton: View?,
    ): IInAppMessageViewWrapper =
        super.createInAppMessageViewWrapper(
            inAppMessageView,
            inAppMessage,
            inAppMessageViewLifecycleListener,
            configurationProvider,
            openingAnimation,
            closingAnimation,
            clickableInAppMessageView,
            buttons,
            closeButton,
        )
}
