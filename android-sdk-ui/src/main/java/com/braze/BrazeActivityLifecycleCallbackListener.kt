package com.braze

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.braze.configuration.BrazeConfigurationProvider
import com.braze.coroutine.BrazeCoroutineScope
import com.braze.push.NotificationTrampolineActivity
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.brazelog
import com.braze.ui.inappmessage.BrazeInAppMessageManager
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Can be used to automatically handle Braze lifecycle methods.
 * Optionally, openSession() and closeSession() are called on onActivityStarted and onActivityStopped respectively.
 * The InAppMessageManager methods of registerInAppMessageManager() and unregisterInAppMessageManager() can be optionally
 * called here as well.
 * Note: This callback should not be set in any Activity. It must be set in the Application class of your app.
 *
 * @param sessionHandlingEnabled              When true, handles calling openSession and closeSession in onActivityStarted
 * and onActivityStopped respectively.
 * @param registerInAppMessageManager         When true, registers and unregisters the [BrazeInAppMessageManager] in
 * [Application.ActivityLifecycleCallbacks.onActivityResumed] and [Application.ActivityLifecycleCallbacks.onActivityPaused]
 * respectively.
 * @param inAppMessagingRegistrationBlocklist A set of [Activity]s for which in-app message registration will not occur.
 * Each class should be retrieved via [Activity.getClass]. If null, an empty set is used instead.
 * @param sessionHandlingBlocklist            A set of [Activity]s for which session handling
 * will not occur. Each class should be retrieved via [Activity.getClass].
 * If null, an empty set is used instead.
 */
@Suppress("TooManyFunctions", "BooleanPropertyNaming")
open class BrazeActivityLifecycleCallbackListener @JvmOverloads constructor(
    private val sessionHandlingEnabled: Boolean = true,
    private val registerInAppMessageManager: Boolean = true,
    inAppMessagingRegistrationBlocklist: Set<Class<*>?>? = emptySet<Class<*>>(),
    sessionHandlingBlocklist: Set<Class<*>?>? = emptySet<Class<*>>()
) : ActivityLifecycleCallbacks {
    private var inAppMessagingRegistrationBlocklist: Set<Class<*>?>
    private var sessionHandlingBlocklist: Set<Class<*>?>

    @Volatile
    @VisibleForTesting
    var shouldPersistWebView: Boolean? = null

    private val isLoadingShouldPersistWebView = AtomicBoolean(false)
    private var currentActivityRef: WeakReference<Activity>? = null

    init {
        this.inAppMessagingRegistrationBlocklist = inAppMessagingRegistrationBlocklist
            ?: emptySet<Class<*>>()
        this.sessionHandlingBlocklist = sessionHandlingBlocklist ?: emptySet<Class<*>>()
        brazelog(V) {
            "BrazeActivityLifecycleCallbackListener using in-app messaging blocklist: ${this.inAppMessagingRegistrationBlocklist}"
        }
        brazelog(V) {
            "BrazeActivityLifecycleCallbackListener using session handling blocklist: ${this.sessionHandlingBlocklist}"
        }
    }

    /**
     * Constructor that sets a blocklist for session handling and [BrazeInAppMessageManager] registration while also
     * enabling both features.
     *
     * @param inAppMessagingRegistrationBlocklist A set of [Activity]s for which in-app message registration will not
     * occur. Each class should be retrieved via [Activity.getClass].
     * @param sessionHandlingBlocklist            A set of [Activity]s for which session handling will not occur. Each
     * class should be retrieved via [Activity.getClass].
     */
    @JvmOverloads
    constructor(
        inAppMessagingRegistrationBlocklist: Set<Class<*>?>?,
        sessionHandlingBlocklist: Set<Class<*>?>? = emptySet<Class<*>>()
    ) : this(true, true, inAppMessagingRegistrationBlocklist, sessionHandlingBlocklist)

    /**
     * Sets the [Activity.getClass] blocklist for which in-app message registration will not occur.
     */
    fun setInAppMessagingRegistrationBlocklist(blocklist: Set<Class<*>?>) {
        brazelog(V) { "setInAppMessagingRegistrationBlocklist called with blocklist: $blocklist" }
        inAppMessagingRegistrationBlocklist = blocklist
    }

    /**
     * Sets the [Activity.getClass] blocklist for which session handling will not occur.
     */
    fun setSessionHandlingBlocklist(blocklist: Set<Class<*>?>) {
        brazelog(V) { "setSessionHandlingBlocklist called with blocklist: $blocklist" }
        sessionHandlingBlocklist = blocklist
    }

    override fun onActivityStarted(activity: Activity) {
        if (sessionHandlingEnabled && shouldHandleLifecycleMethodsInActivity(activity, true)) {
            brazelog(V) {
                "Automatically calling lifecycle method: openSession for class: ${activity.javaClass}"
            }
            Braze.getInstance(activity.applicationContext).openSession(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (sessionHandlingEnabled && shouldHandleLifecycleMethodsInActivity(activity, true)) {
            brazelog(V) {
                "Automatically calling lifecycle method: closeSession for class: ${activity.javaClass}"
            }
            Braze.getInstance(activity.applicationContext).closeSession(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (registerInAppMessageManager) {
            if (shouldHandleLifecycleMethodsInActivity(activity, false)) {
                val previousActivity = currentActivityRef?.get()
                if (shouldPersistWebView == true && // We should be persisting (so we didn't unregister during onPause) AND
                    previousActivity != null && // The previous activity has been set AND
                    previousActivity != activity // The previous activity is different from the current activity
                ) {
                    brazelog(V) {
                        "Activity is different from previous activity. Unregistering in-app message manager"
                    }
                    BrazeInAppMessageManager.getInstance().unregisterInAppMessageManager(activity)
                }

                // If the previous activity is null, this is the first activity, so register.
                // Or if the activity has changed, we also need to register.
                if (shouldPersistWebView != true || // We're not persisting (so we unregistered during onPause) OR
                    previousActivity == null || // The previous activity is null (so this is the first activity) OR
                    previousActivity != activity // The activity has changed
                ) {
                    brazelog(V) {
                        "Automatically calling lifecycle method: registerInAppMessageManager for class: ${activity.javaClass}"
                    }
                    BrazeInAppMessageManager.getInstance().registerInAppMessageManager(activity)
                } else {
                    BrazeInAppMessageManager.getInstance().resumeWebviewIfNecessary()
                }
            } else {
                // We're handling IAM registration in general, but this class is specifically in the block list, so unregister the IAM Manager.
                BrazeInAppMessageManager.getInstance().unregisterInAppMessageManager(activity)
            }
        }

        // Always keep track of the current activity so we have the reference in case the customer isn't using
        // automatic In-App Message registration, we need the activity reference to get the push permission prompt
        // for banners.
        currentActivityRef = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (registerInAppMessageManager &&
            shouldHandleLifecycleMethodsInActivity(activity, false)
        ) {
            // When shouldPersistWebView is null (async config load not yet complete), default to persist behavior.
            // This is the safer default because persisting the webview is less disruptive than unregistering,
            // and matches the SDK's default configuration value of true.
            if (shouldPersistWebView == false) {
                brazelog(V) {
                    "Automatically calling lifecycle method: unregisterInAppMessageManager for class: ${activity.javaClass}"
                }
                BrazeInAppMessageManager.getInstance().unregisterInAppMessageManager(activity)
            } else {
                BrazeInAppMessageManager.getInstance().pauseWebviewIfNecessary()
                brazelog(V) {
                    "Skipping unregisterInAppMessageManager in onActivityPaused. " +
                        "shouldPersistWebView=$shouldPersistWebView (null means async load incomplete, defaulting to persist)"
                }
            }
        }
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        brazelog(V) {
            "Automatically calling lifecycle method: ensureSubscribedToInAppMessageEvents for class: ${activity.javaClass}"
        }
        BrazeInAppMessageManager.getInstance().ensureSubscribedToInAppMessageEvents(activity.applicationContext)

        // Pre-load shouldPersistWebView on IO thread to avoid blocking read in onActivityPaused
        // Uses compareAndSet to prevent race condition if onActivityCreated is called rapidly
        if (registerInAppMessageManager &&
            shouldPersistWebView == null &&
            isLoadingShouldPersistWebView.compareAndSet(false, true)
        ) {
            val context = activity.applicationContext
            BrazeCoroutineScope.launch {
                try {
                    val configurationProvider = BrazeConfigurationProvider(context)
                    shouldPersistWebView = configurationProvider.shouldPersistWebViewWhenBackgroundingApp
                } catch (e: Exception) {
                    brazelog(priority = E, e) {
                        "Error while reading shouldPersistWebViewWhenBackgroundingApp from BrazeConfigurationProvider"
                    }
                }
                brazelog(V) { "Async load of shouldPersistWebView completed: $shouldPersistWebView" }
            }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    /**
     * Determines if this [Activity] should be ignored for the purposes of session tracking or in-app message registration.
     */
    @VisibleForTesting
    fun shouldHandleLifecycleMethodsInActivity(
        activity: Activity,
        forSessionHandling: Boolean
    ): Boolean {
        val activityClass: Class<out Activity> = activity.javaClass
        if (activityClass == NotificationTrampolineActivity::class.java) {
            brazelog(V) { "Skipping automatic registration for notification trampoline activity class." }
            // Always ignore
            return false
        }
        return if (forSessionHandling) {
            !sessionHandlingBlocklist.contains(activityClass)
        } else {
            !inAppMessagingRegistrationBlocklist.contains(activityClass)
        }
    }

    /**
     * Registers this listener directly against the Application.
     * Equivalent to:
     *
     * ```
     * applicationInstance.registerActivityLifecycleCallbacks(thisListener)
     * ```
     */
    fun registerOnApplication(context: Context) {
        try {
            (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
        } catch (e: Exception) {
            brazelog(priority = E, e) { "Failed to register this lifecycle callback listener directly against application class" }
        }
    }
}
