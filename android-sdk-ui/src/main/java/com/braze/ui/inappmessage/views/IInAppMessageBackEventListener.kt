package com.braze.ui.inappmessage.views

import android.window.BackEvent
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.brazelog

/**
 * [IInAppMessageBackEventListener] allows in-app message views to listen to back animation
 * callbacks for back events. Requires API 34+.
 *
 * Animations are supported for gesture navigation modes on API 34+ and for 3-button navigation modes on API 36+.
 *
 * See also [BackEvent](https://developer.android.com/reference/android/window/BackEvent)
 * and [OnBackAnimationCallback](https://developer.android.com/reference/android/window/OnBackAnimationCallback)
 */
interface IInAppMessageBackEventListener {
    /**
     * Called when the back gesture or button has started.
     *
     * @param backEvent The BackEvent containing information about the gesture.
     */
    fun onBackStarted(backEvent: BackEvent) {
        brazelog(V) { "IInAppMessageBackEventListener: onBackStarted() called." }
    }

    /**
     * Called as the user progresses with the back gesture or holds down the back button.
     *
     * @param backEvent The BackEvent containing information about the gesture, including the progress.
     */
    fun onBackProgressed(backEvent: BackEvent)

    /**
     * Called if the user cancels the back gesture or button.
     */
    fun onBackCancelled() {
        brazelog(V) { "IInAppMessageBackEventListener: onBackCancelled() called." }
    }

    /**
     * Called when the back gesture or button is completed (the user lifts their finger).
     */
    fun onBackInvoked() {
        brazelog(V) { "IInAppMessageBackEventListener: onBackInvoked() called." }
    }
}
