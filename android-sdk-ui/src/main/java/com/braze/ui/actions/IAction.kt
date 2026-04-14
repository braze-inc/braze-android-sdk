package com.braze.ui.actions

import android.content.Context
import com.braze.enums.Channel

/**
 * Represents an executable action triggered by user interaction with
 * a Braze message (push notification, in-app message, Content Card, etc.).
 */
interface IAction {
    /** The [Channel] from which this action originated. */
    val channel: Channel

    /**
     * Executes this action.
     *
     * @param context The Android [Context] used to start activities or services.
     */
    fun execute(context: Context)
}
