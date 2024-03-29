package com.braze.unity.enums

import com.braze.ui.inappmessage.InAppMessageOperation

@Suppress("MagicNumber")
enum class UnityInAppMessageManagerAction(
    private val value: Int,
    val inAppMessageOperation: InAppMessageOperation?
) {
    UNKNOWN(-1, null),

    /**
     * Maps to [InAppMessageOperation.DISPLAY_NOW].
     */
    IAM_DISPLAY_NOW(0, InAppMessageOperation.DISPLAY_NOW),

    /**
     * Maps to [InAppMessageOperation.DISPLAY_LATER].
     */
    IAM_DISPLAY_LATER(1, InAppMessageOperation.DISPLAY_LATER),

    /**
     * Maps to [InAppMessageOperation.DISCARD].
     */
    IAM_DISCARD(2, InAppMessageOperation.DISCARD),

    /**
     * Maps to [InAppMessageOperation.REENQUEUE].
     */
    IAM_REENQUEUE(3, InAppMessageOperation.REENQUEUE);

    companion object {
        fun getTypeFromValue(value: Int): UnityInAppMessageManagerAction? =
            values().firstOrNull { it.value == value }
    }
}
