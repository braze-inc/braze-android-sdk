package com.braze.unity.enums

import com.braze.ui.inappmessage.InAppMessageOperation

/**
 * Maps integer values received from the Unity layer to their corresponding
 * [InAppMessageOperation], controlling how the in-app message manager handles
 * the next message.
 *
 * @property inAppMessageOperation The SDK [InAppMessageOperation] this action maps to, or null for [UNKNOWN].
 */
@Suppress("MagicNumber")
enum class UnityInAppMessageManagerAction(
    private val value: Int,
    val inAppMessageOperation: InAppMessageOperation?,
) {
    /** Unrecognized action value. */
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
    IAM_REENQUEUE(3, InAppMessageOperation.REENQUEUE),
    ;

    companion object {
        /** Returns the [UnityInAppMessageManagerAction] matching [value], or null if none match. */
        fun getTypeFromValue(value: Int): UnityInAppMessageManagerAction? = entries.firstOrNull { it.value == value }
    }
}
