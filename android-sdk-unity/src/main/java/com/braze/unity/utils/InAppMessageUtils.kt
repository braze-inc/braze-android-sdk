package com.braze.unity.utils

import android.content.Context
import com.braze.Braze.Companion.getInstance
import com.braze.models.inappmessage.IInAppMessage
import com.braze.models.inappmessage.IInAppMessageImmersive
import com.braze.support.BrazeLogger.Priority.W
import com.braze.support.BrazeLogger.brazelog

/**
 * Utility methods for deserializing and logging analytics on in-app messages
 * from the Unity layer.
 */
object InAppMessageUtils {
    /**
     * Deserializes a JSON string into an [IInAppMessage] instance.
     *
     * @param context Android context, or null to return null.
     * @param messageJSONString JSON representation of the in-app message, or null to return null.
     * @return The deserialized [IInAppMessage], or null if either parameter is null.
     */
    fun inAppMessageFromString(
        context: Context?,
        messageJSONString: String?,
    ): IInAppMessage? =
        if (messageJSONString == null || context == null) {
            null
        } else {
            getInstance(context)
                .deserializeInAppMessageString(messageJSONString)
        }

    /**
     * Logs a click event on the given in-app message.
     *
     * @param inAppMessage The in-app message that was clicked, or null to no-op.
     */
    fun logInAppMessageClick(inAppMessage: IInAppMessage?) {
        if (inAppMessage != null) {
            inAppMessage.logClick()
        } else {
            brazelog(W) {
                "The in-app message is null. Not logging in-app message click."
            }
        }
    }

    /**
     * Logs a button click event on the given in-app message.
     *
     * @param inAppMessage The in-app message containing the button, or null to no-op.
     * @param buttonId The identifier of the button that was clicked.
     */
    fun logInAppMessageButtonClick(
        inAppMessage: IInAppMessage?,
        buttonId: Int,
    ) {
        if (inAppMessage == null) {
            brazelog(W) { "The in-app message is null. Not logging in-app message button $buttonId click." }
            return
        }
        if (inAppMessage is IInAppMessageImmersive) {
            inAppMessage.messageButtons
                .firstOrNull { it.id == buttonId }
                ?.let { inAppMessage.logButtonClick(it) }
        } else {
            brazelog(W) {
                "The in-app message isn't an instance of InAppMessageImmersive. " +
                    "Not logging in-app message button click."
            }
        }
    }

    /**
     * Logs an impression event on the given in-app message.
     *
     * @param inAppMessage The in-app message that was displayed, or null to no-op.
     */
    fun logInAppMessageImpression(inAppMessage: IInAppMessage?) {
        if (inAppMessage != null) {
            inAppMessage.logImpression()
        } else {
            brazelog(W) {
                "The in-app message is null, Not logging in-app message impression."
            }
        }
    }
}
