package com.braze.ui.inappmessage

import androidx.annotation.Keep
import java.util.*

enum class InAppMessageOperation {
    DISPLAY_NOW, DISPLAY_LATER, DISCARD, REENQUEUE;

    @Keep
    companion object {
        @JvmStatic
        fun fromValue(value: String?): InAppMessageOperation? =
            entries.firstOrNull { it.name == value?.uppercase(Locale.US) }
    }
}
