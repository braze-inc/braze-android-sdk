package com.braze.ui.inappmessage

import androidx.annotation.Keep
import java.util.Locale

/**
 * Specifies how the [BrazeInAppMessageManager][com.braze.ui.inappmessage.BrazeInAppMessageManager]
 * should handle an in-app message that is ready for display.
 */
enum class InAppMessageOperation {
    /** Display the in-app message immediately. */
    DISPLAY_NOW,

    /** Delay display until the next eligible time. */
    DISPLAY_LATER,

    /** Permanently discard the in-app message. */
    DISCARD,

    /** Return the in-app message to the front of the display stack. */
    REENQUEUE,

    ;

    @Keep
    companion object {
        @JvmStatic
        fun fromValue(value: String?): InAppMessageOperation? = entries.firstOrNull { it.name == value?.uppercase(Locale.US) }
    }
}
