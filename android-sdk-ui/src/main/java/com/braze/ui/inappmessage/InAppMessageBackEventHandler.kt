package com.braze.ui.inappmessage

import android.app.Activity
import android.os.Build
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedDispatcher
import com.braze.support.BrazeLogger.brazelog
import com.braze.ui.inappmessage.utils.InAppMessageViewUtils
import com.braze.ui.inappmessage.views.IInAppMessageBackEventListener

/**
 * [InAppMessageBackEventHandler] handles back events by creating a back animation callback. Requires API 34+.
 *
 * Animations are supported for gesture navigation modes on API 34+ and for 3-button navigation modes on API 36+.
 *
 * See also [BackEvent](https://developer.android.com/reference/android/window/BackEvent)
 * and [OnBackAnimationCallback](https://developer.android.com/reference/android/window/OnBackAnimationCallback)
 */
open class InAppMessageBackEventHandler(
    activity: Activity,
    private val inAppMessageView: IInAppMessageBackEventListener?
) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && BrazeInAppMessageManager.getInstance().doesBackButtonDismissInAppMessageView) {
            activity.let {
                val inAppMessageBackAnimationCallback = object : OnBackAnimationCallback {
                    override fun onBackInvoked() {
                        brazelog { "Back button intercepted by in-app message back animation callback, closing in-app message." }
                        InAppMessageViewUtils.closeInAppMessageOnKeycodeBack()
                        it.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(this)
                    }

                    override fun onBackStarted(backEvent: BackEvent) {
                        brazelog { "Back button intercepted by in-app message back animation callback, back event started." }
                        super.onBackStarted(backEvent)
                        inAppMessageView?.onBackStarted(backEvent)
                    }

                    override fun onBackProgressed(backEvent: BackEvent) {
                        brazelog { "Back button intercepted by in-app message back animation callback, back event in progress." }
                        super.onBackProgressed(backEvent)
                        inAppMessageView?.onBackProgressed(backEvent)
                    }

                    override fun onBackCancelled() {
                        brazelog { "Back button intercepted by in-app message back animation callback, back event cancelled." }
                        super.onBackCancelled()
                        inAppMessageView?.onBackCancelled()
                    }
                }

                it.onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY, inAppMessageBackAnimationCallback)
            }
        }
    }
}
