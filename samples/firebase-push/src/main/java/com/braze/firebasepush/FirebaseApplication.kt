package com.braze.firebasepush

import android.app.Application
import android.content.Context
import android.util.Log
import com.braze.Braze
import com.braze.Braze.Companion.configure
import com.braze.BrazeActivityLifecycleCallbackListener
import com.braze.configuration.BrazeConfig
import com.braze.enums.BrazePushEventType
import com.braze.push.BrazeNotificationUtils
import com.braze.support.BrazeLogger
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging

class FirebaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BrazeLogger.enableVerboseLogging()

        val brazeConfig = BrazeConfig.Builder()
            .setDefaultNotificationChannelName("Braze Push")
            .setDefaultNotificationChannelDescription("Braze related push")
            .setPushDeepLinkBackStackActivityEnabled(true)
            .setPushDeepLinkBackStackActivityClass(MainActivity::class.java)
            .setInAppMessageTestPushEagerDisplayEnabled(true)

            // Setting this to false since we're manually handling
            // push with `subscribeToPushNotificationEvents`
            .setHandlePushDeepLinksAutomatically(false)
        configure(this, brazeConfig.build())
        registerActivityLifecycleCallbacks(BrazeActivityLifecycleCallbackListener())

        // Example of how to register for Firebase Cloud Messaging manually.
        val applicationContext: Context = this
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task: Task<String> ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.i(TAG, "================")
            Log.i(TAG, "================")
            Log.i(TAG, "Registering firebase token in Application class: $token")
            Log.i(TAG, "================")
            Log.i(TAG, "================")
            Braze.getInstance(applicationContext).registeredPushToken = token
        }

        Braze.getInstance(applicationContext).subscribeToPushNotificationEvents { event ->
            if (event.eventType == BrazePushEventType.NOTIFICATION_RECEIVED) {
                Log.i(TAG, "================")
                Log.i(TAG, "================")
                Log.i(TAG, "Push has been received: $event")
                Log.i(TAG, "================")
                Log.i(TAG, "================")
                if (event.notificationPayload.extras.containsKey("my_custom_braze_kvp_here")) {
                    // Download the data for this special push
                }
            } else if (event.eventType == BrazePushEventType.NOTIFICATION_OPENED) {
                Log.i(TAG, "================")
                Log.i(TAG, "================")
                Log.i(TAG, "Push has been opened: $event")
                Log.i(TAG, "================")
                Log.i(TAG, "================")
                if (event.notificationPayload.extras.containsKey("my_custom_braze_kvp_here")) {
                    // Execute the special push experience
                } else {
                    // Let Braze handle this push notification
                    BrazeNotificationUtils.routeUserWithNotificationOpenedIntent(
                        applicationContext,
                        event
                    )
                }
            }
        }
    }

    companion object {
        private val TAG = FirebaseApplication::class.java.name
    }
}
