package com.braze.ui.inappmessage

import android.app.Activity
import android.view.View
import com.braze.models.inappmessage.IInAppMessage
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.Priority.W
import com.braze.support.BrazeLogger.brazelog
import com.braze.ui.inappmessage.views.InAppMessageHtmlBaseView
import com.braze.ui.support.removeViewFromParent

/**
 * Coordinates in-app message state across Activity [BrazeInAppMessageManager.registerInAppMessageManager]
 * and [BrazeInAppMessageManager.unregisterInAppMessageManager] transitions.
 */
internal class InAppMessageActivityTransitionCoordinator {
    /**
     * Result of tearing down a displayed in-app message during Activity unregister.
     *
     * @property carryoverInAppMessage Message to redisplay on the next Activity, or null when none should carry over.
     * @property shouldClearActiveWrapper Whether active wrapper/back-handler/WebView pause state should be cleared.
     */
    data class UnregisterDisplayedMessageResult(
        val carryoverInAppMessage: IInAppMessage?,
        val shouldClearActiveWrapper: Boolean,
    )

    fun logUnregisterActivity(activity: Activity?) {
        if (activity == null) {
            brazelog(W) { "Null Activity passed to unregisterInAppMessageManager." }
        } else {
            brazelog(V) { "Unregistering InAppMessageManager from activity: ${activity.localClassName}" }
        }
    }

    /**
     * Saves or tears down the displayed in-app message when unregistering during an Activity transition.
     */
    fun resolveUnregisterDisplayedMessage(
        viewWrapper: IInAppMessageViewWrapper?,
        resetAfterClose: (IInAppMessageViewWrapper) -> Boolean,
        notifyAfterClosed: (IInAppMessage) -> Unit,
    ): UnregisterDisplayedMessageResult {
        if (viewWrapper == null) {
            return UnregisterDisplayedMessageResult(
                carryoverInAppMessage = null,
                shouldClearActiveWrapper = false,
            )
        }
        val inAppMessageView = viewWrapper.inAppMessageView
        if (inAppMessageView is InAppMessageHtmlBaseView) {
            brazelog { "In-app message view includes HTML. Removing the page finished listener." }
            inAppMessageView.setHtmlPageFinishedListener(null)
        }
        val carryoverInAppMessage =
            if (viewWrapper.isAnimatingClose) {
                finalizeAnimatingCloseOnUnregister(
                    viewWrapper = viewWrapper,
                    inAppMessageView = inAppMessageView,
                    resetAfterClose = resetAfterClose,
                    notifyAfterClosed = notifyAfterClosed,
                )
                null
            } else {
                saveInAppMessageForCarryoverOnUnregister(
                    viewWrapper = viewWrapper,
                    inAppMessageView = inAppMessageView,
                )
            }
        return UnregisterDisplayedMessageResult(
            carryoverInAppMessage = carryoverInAppMessage,
            shouldClearActiveWrapper = true,
        )
    }

    private fun finalizeAnimatingCloseOnUnregister(
        viewWrapper: IInAppMessageViewWrapper,
        inAppMessageView: View,
        resetAfterClose: (IInAppMessageViewWrapper) -> Boolean,
        notifyAfterClosed: (IInAppMessage) -> Unit,
    ) {
        inAppMessageView.removeViewFromParent()
        if (resetAfterClose(viewWrapper)) {
            notifyAfterClosed(viewWrapper.inAppMessage)
        }
    }

    private fun saveInAppMessageForCarryoverOnUnregister(
        viewWrapper: IInAppMessageViewWrapper,
        inAppMessageView: View,
    ): IInAppMessage {
        viewWrapper.prepareForActivityTransitionCarryover()
        inAppMessageView.removeViewFromParent()
        return viewWrapper.inAppMessage
    }
}
