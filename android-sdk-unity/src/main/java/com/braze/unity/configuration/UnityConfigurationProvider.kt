package com.braze.unity.configuration

import android.content.Context
import com.braze.unity.enums.UnityMessageType
import com.braze.configuration.CachedConfigurationProvider
import com.braze.enums.DataStoreKey
import com.braze.support.BrazeLogger.brazelog
import com.braze.ui.inappmessage.InAppMessageOperation
import com.braze.ui.inappmessage.InAppMessageOperation.Companion.fromValue

/**
 * [CachedConfigurationProvider] that reads Unity-specific Braze configuration
 * from the `braze.xml` resource file, including GameObject names, callback
 * method names, and in-app message display behavior.
 */
class UnityConfigurationProvider(context: Context) : CachedConfigurationProvider(
    context, false
) {
    /** Unity GameObject name for the in-app message listener. */
    val inAppMessageListenerGameObjectName: String?
        get() = getStringValue(DataStoreKey.INAPP_LISTENER_GAME_OBJECT_NAME.key, null)

    /** Unity callback method name for the in-app message listener. */
    val inAppMessageListenerCallbackMethodName: String?
        get() = getStringValue(DataStoreKey.INAPP_LISTENER_CALLBACK_METHOD_NAME.key, null)

    /** Whether in-app messages are shown automatically when received. */
    @Suppress("BooleanPropertyNaming")
    val showInAppMessagesAutomaticallyKey: Boolean
        get() = getBooleanValue(DataStoreKey.INAPP_SHOW_INAPP_MESSAGES_AUTOMATICALLY.key, true)

    /** Unity GameObject name for push-received events. */
    val pushReceivedGameObjectName: String?
        get() = getStringValue(DataStoreKey.PUSH_RECEIVED_GAME_OBJECT_NAME.key, null)

    /** Unity callback method name for push-received events. */
    val pushReceivedCallbackMethodName: String?
        get() = getStringValue(DataStoreKey.PUSH_RECEIVED_CALLBACK_METHOD_NAME.key, null)

    /** Unity GameObject name for push-opened events. */
    val pushOpenedGameObjectName: String?
        get() = getStringValue(DataStoreKey.PUSH_OPENED_GAME_OBJECT_NAME.key, null)

    /** Unity callback method name for push-opened events. */
    val pushOpenedCallbackMethodName: String?
        get() = getStringValue(DataStoreKey.PUSH_OPENED_CALLBACK_METHOD_NAME.key, null)

    /** Unity GameObject name for push-deleted events. */
    val pushDeletedGameObjectName: String?
        get() = getStringValue(DataStoreKey.PUSH_DELETED_GAME_OBJECT_NAME.key, null)

    /** Unity callback method name for push-deleted events. */
    val pushDeletedCallbackMethodName: String?
        get() = getStringValue(DataStoreKey.PUSH_DELETED_CALLBACK_METHOD_NAME.key, null)

    /** Unity GameObject name for Content Cards updated events. */
    val contentCardsUpdatedListenerGameObjectName: String?
        get() = getStringValue(DataStoreKey.CONTENT_CARDS_UPDATED_LISTENER_GAME_OBJECT_NAME.key, null)

    /** Unity callback method name for Content Cards updated events. */
    val contentCardsUpdatedListenerCallbackMethodName: String?
        get() = getStringValue(DataStoreKey.CONTENT_CARDS_UPDATED_LISTENER_CALLBACK_METHOD_NAME.key, null)

    /** Unity GameObject name for Feature Flags updated events. */
    val featureFlagsUpdatedListenerGameObjectName: String?
        get() = getStringValue(DataStoreKey.FEATURE_FLAGS_UPDATED_LISTENER_GAME_OBJECT_NAME.key, null)

    /** Unity callback method name for Feature Flags updated events. */
    val featureFlagsUpdatedListenerCallbackMethodName: String?
        get() = getStringValue(DataStoreKey.FEATURE_FLAGS_UPDATED_LISTENER_CALLBACK_METHOD_NAME.key, null)

    /** Unity GameObject name for SDK authentication failure events. */
    val sdkAuthenticationFailureListenerGameObjectName: String?
        get() = getStringValue(DataStoreKey.SDK_AUTHENTICATION_FAILURE_LISTENER_GAME_OBJECT_NAME.key, null)

    /** Unity callback method name for SDK authentication failure events. */
    val sdkAuthenticationFailureListenerCallbackMethodName: String?
        get() = getStringValue(DataStoreKey.SDK_AUTHENTICATION_FAILURE_LISTENER_CALLBACK_METHOD_NAME.key, null)

    /** Whether the in-app message manager listener is automatically set during [Activity][android.app.Activity] creation. */
    @Suppress("BooleanPropertyNaming")
    val autoSetInAppMessageManagerListener: Boolean
        get() = getBooleanValue(DataStoreKey.INAPP_AUTO_SET_MANAGER_LISTENER.key, true)

    /** The [InAppMessageOperation] to use when an in-app message is first ready for display. */
    val initialInAppMessageDisplayOperation: InAppMessageOperation
        get() {
            val rawValue = getStringValue(DataStoreKey.INAPP_INITIAL_DISPLAY_OPERATION.key, null)
            val operation = fromValue(rawValue)
            return operation ?: InAppMessageOperation.DISPLAY_NOW
        }

    /**
     * Registers a Unity callback for the given [UnityMessageType] by persisting
     * the [gameObject] name and [methodName] into the runtime configuration.
     *
     * @param messageTypeValue Integer value corresponding to a [UnityMessageType].
     * @param gameObject Unity GameObject name to receive callbacks.
     * @param methodName Unity method name to invoke on the GameObject.
     */
    @Suppress("LongMethod")
    fun configureListener(messageTypeValue: Int, gameObject: String, methodName: String) {
        val messageType = UnityMessageType.getTypeFromValue(messageTypeValue)
        if (messageType == null) {
            brazelog {
                "Got bad message type $messageTypeValue. Cannot configure a listener on object " +
                    "$gameObject for method $methodName"
            }
            return
        }
        when (messageType) {
            UnityMessageType.PUSH_PERMISSIONS_PROMPT_RESPONSE,
            UnityMessageType.PUSH_TOKEN_RECEIVED_FROM_SYSTEM -> {}
            UnityMessageType.PUSH_RECEIVED -> {
                putStringIntoRuntimeConfiguration(DataStoreKey.PUSH_RECEIVED_GAME_OBJECT_NAME.key, gameObject)
                putStringIntoRuntimeConfiguration(
                    DataStoreKey.PUSH_RECEIVED_CALLBACK_METHOD_NAME.key,
                    methodName
                )
            }
            UnityMessageType.PUSH_OPENED -> {
                putStringIntoRuntimeConfiguration(DataStoreKey.PUSH_OPENED_GAME_OBJECT_NAME.key, gameObject)
                putStringIntoRuntimeConfiguration(DataStoreKey.PUSH_OPENED_CALLBACK_METHOD_NAME.key, methodName)
            }
            UnityMessageType.PUSH_DELETED -> {
                putStringIntoRuntimeConfiguration(DataStoreKey.PUSH_DELETED_GAME_OBJECT_NAME.key, gameObject)
                putStringIntoRuntimeConfiguration(DataStoreKey.PUSH_DELETED_CALLBACK_METHOD_NAME.key, methodName)
            }
            UnityMessageType.IN_APP_MESSAGE -> {
                putStringIntoRuntimeConfiguration(DataStoreKey.INAPP_LISTENER_GAME_OBJECT_NAME.key, gameObject)
                putStringIntoRuntimeConfiguration(
                    DataStoreKey.INAPP_LISTENER_CALLBACK_METHOD_NAME.key,
                    methodName
                )
            }
            UnityMessageType.CONTENT_CARDS_UPDATED -> {
                putStringIntoRuntimeConfiguration(
                    DataStoreKey.CONTENT_CARDS_UPDATED_LISTENER_GAME_OBJECT_NAME.key, gameObject
                )
                putStringIntoRuntimeConfiguration(
                    DataStoreKey.CONTENT_CARDS_UPDATED_LISTENER_CALLBACK_METHOD_NAME.key, methodName
                )
            }
            UnityMessageType.SDK_AUTHENTICATION_FAILURE -> {
                putStringIntoRuntimeConfiguration(
                    DataStoreKey.SDK_AUTHENTICATION_FAILURE_LISTENER_GAME_OBJECT_NAME.key, gameObject
                )
                putStringIntoRuntimeConfiguration(
                    DataStoreKey.SDK_AUTHENTICATION_FAILURE_LISTENER_CALLBACK_METHOD_NAME.key, methodName
                )
            }
            UnityMessageType.FEATURE_FLAGS_UPDATED -> {
                putStringIntoRuntimeConfiguration(
                    DataStoreKey.FEATURE_FLAGS_UPDATED_LISTENER_GAME_OBJECT_NAME.key, gameObject
                )
                putStringIntoRuntimeConfiguration(
                    DataStoreKey.FEATURE_FLAGS_UPDATED_LISTENER_CALLBACK_METHOD_NAME.key, methodName
                )
            }
        }
    }

    private fun putStringIntoRuntimeConfiguration(key: String, value: String) {
        runtimeAppConfigurationProvider.writeString(key, value)
    }
}
