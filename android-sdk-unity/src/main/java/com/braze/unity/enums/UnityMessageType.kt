package com.braze.unity.enums

import androidx.annotation.VisibleForTesting

/**
 * Types of messages that Braze can be configured to send to a GameObject method at runtime.
 */
@Suppress("MagicNumber")
enum class UnityMessageType(
    @get:VisibleForTesting internal val value: Int,
) {
    /** Response to a push permissions prompt. */
    PUSH_PERMISSIONS_PROMPT_RESPONSE(0),

    /** Push token received from the system. */
    PUSH_TOKEN_RECEIVED_FROM_SYSTEM(1),

    /** Push notification received. */
    PUSH_RECEIVED(2),

    /** Push notification opened by the user. */
    PUSH_OPENED(3),

    /** Push notification deleted by the user. */
    PUSH_DELETED(4),

    /** In-app message received. */
    IN_APP_MESSAGE(5),

    /** Content Cards list updated. */
    CONTENT_CARDS_UPDATED(7),

    /** SDK authentication failure occurred. */
    SDK_AUTHENTICATION_FAILURE(8),

    /** Feature Flags updated. */
    FEATURE_FLAGS_UPDATED(9),
    ;

    companion object {
        /** Returns the [UnityMessageType] matching [value], or null if none match. */
        fun getTypeFromValue(value: Int) = entries.firstOrNull { it.value == value }
    }
}
