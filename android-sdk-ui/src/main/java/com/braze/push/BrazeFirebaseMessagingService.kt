package com.braze.push

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.braze.Braze
import com.braze.BrazeInternal.applyPendingRuntimeConfiguration
import com.braze.Constants
import com.braze.configuration.BrazeConfigurationProvider
import com.braze.support.BrazeLogger.Priority.I
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.brazelog
import com.braze.support.ReflectionUtils.constructObjectQuietly
import com.braze.support.ReflectionUtils.getDeclaredMethodQuietly
import com.braze.support.ReflectionUtils.getMethodQuietly
import com.braze.support.ReflectionUtils.invokeMethodQuietly
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

open class BrazeFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        applyPendingRuntimeConfiguration(this)
        val configurationProvider = BrazeConfigurationProvider(this)
        if (Braze.getConfiguredApiKey(configurationProvider).isNullOrEmpty()) {
            brazelog(V) { "No configured API key, not registering token in onNewToken. Token: $newToken" }
            return
        }
        if (!configurationProvider.isFirebaseMessagingServiceOnNewTokenRegistrationEnabled) {
            brazelog(V) {
                "Automatic FirebaseMessagingService.OnNewToken() registration" +
                    " disabled, not registering token: $newToken"
            }
            return
        }
        brazelog(V) { "Registering Firebase push token in onNewToken. Token: $newToken" }
        Braze.getInstance(this).registeredPushToken = newToken
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        handleBrazeRemoteMessage(this, remoteMessage)
    }

    companion object {
        /**
         * The [onMessageReceived] method for a [FirebaseMessagingService] class with
         * the expected signature of `fun onMessageReceived(remoteMessage: RemoteMessage)`.
         */
        private const val FCM_SERVICE_OMR_METHOD = "onMessageReceived"
        private const val CONTEXT_ATTACH_METHOD = "attachBaseContext"

        /**
         * Consumes an incoming [RemoteMessage] if it originated from Braze. If the [RemoteMessage] did
         * not originate from Braze, then this method does nothing and returns false.
         *
         * @param remoteMessage The [RemoteMessage] from Firebase.
         * @return true iff the [RemoteMessage] originated from Braze and was consumed. Returns false
         * if the [RemoteMessage] did not originate from Braze or otherwise could not be handled by Braze.
         */
        @JvmStatic
        fun handleBrazeRemoteMessage(context: Context, remoteMessage: RemoteMessage): Boolean {
            if (!isBrazePushNotification(remoteMessage)) {
                brazelog(I) {
                    "Remote message did not originate from Braze. Not " +
                        "consuming remote message: $remoteMessage"
                }

                val configurationProvider = BrazeConfigurationProvider(context)
                if (configurationProvider.isFallbackFirebaseMessagingServiceEnabled) {
                    val fallbackClassPath = configurationProvider.fallbackFirebaseMessagingServiceClasspath
                    if (fallbackClassPath != null) {
                        brazelog(I) {
                            "Fallback FCM service enabled. Attempting to use " +
                                "fallback class at $fallbackClassPath"
                        }
                        invokeFallbackFirebaseService(fallbackClassPath, remoteMessage, context)
                    } else {
                        brazelog {
                            "Fallback FCM service enabled but classpath " +
                                "is null. Not routing to any fallback service."
                        }
                    }
                }

                return false
            }
            val remoteMessageData = remoteMessage.data
            brazelog(I) { "Got remote message from FCM: $remoteMessageData" }
            val pushIntent = Intent(BrazePushReceiver.FIREBASE_MESSAGING_SERVICE_ROUTING_ACTION)
            val bundle = Bundle()
            for ((key, value) in remoteMessageData) {
                brazelog(V) { "Adding bundle item from FCM remote data with key: $key and value: $value" }
                bundle.putString(key, value)
            }
            pushIntent.putExtras(bundle)
            BrazePushReceiver.handleReceivedIntent(context, pushIntent)
            return true
        }

        /**
         * Determines if the Firebase [RemoteMessage] originated from Braze and should be
         * forwarded to [BrazeFirebaseMessagingService.handleBrazeRemoteMessage].
         *
         * @param remoteMessage The [RemoteMessage] from [FirebaseMessagingService.onMessageReceived]
         * @return true iff this [RemoteMessage] originated from Braze or otherwise
         * should be passed to [BrazeFirebaseMessagingService.handleBrazeRemoteMessage].
         */
        @JvmStatic
        fun isBrazePushNotification(remoteMessage: RemoteMessage): Boolean {
            val remoteMessageData = remoteMessage.data
            return "true" == remoteMessageData[Constants.BRAZE_PUSH_BRAZE_KEY]
        }

        /**
         * Invokes the [FCM_SERVICE_OMR_METHOD] method of the argument class with the provided
         * [RemoteMessage].
         */
        internal fun invokeFallbackFirebaseService(classpath: String, remoteMessage: RemoteMessage, context: Context) {
            val fallbackObject = constructObjectQuietly(classpath)
            if (fallbackObject == null) {
                brazelog { "Fallback firebase messaging service $classpath could not be constructed. Not routing fallback RemoteMessage." }
                return
            }

            val attachMethod = getDeclaredMethodQuietly(classpath, CONTEXT_ATTACH_METHOD, Context::class.java)
            if (attachMethod != null) {
                // Since this is protected, we must make it accessible before calling it
                attachMethod.isAccessible = true
                brazelog { "Attempting to call $classpath $CONTEXT_ATTACH_METHOD" }
                val invokeReturn = invokeMethodQuietly(fallbackObject, attachMethod, context)
                if (!invokeReturn.first) {
                    brazelog {
                        "Failure invoking $classpath.$CONTEXT_ATTACH_METHOD. Not doing anything."
                    }
                    return
                }
            } else {
                brazelog {
                    "Could not find $CONTEXT_ATTACH_METHOD. Not doing anything."
                }
                return
            }

            val method = getMethodQuietly(classpath, FCM_SERVICE_OMR_METHOD, RemoteMessage::class.java)
            if (method == null) {
                brazelog {
                    "Fallback firebase messaging service method $classpath.$FCM_SERVICE_OMR_METHOD " +
                        "could not be retrieved. Not routing fallback RemoteMessage."
                }
                return
            }
            brazelog { "Attempting to invoke firebase messaging fallback service $classpath.$FCM_SERVICE_OMR_METHOD" }
            val omrReturn = invokeMethodQuietly(fallbackObject, method, remoteMessage)
            if (!omrReturn.first) {
                "Failure invoking $classpath.$FCM_SERVICE_OMR_METHOD."
            }
        }
    }
}
