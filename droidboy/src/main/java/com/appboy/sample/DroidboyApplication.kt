package com.appboy.sample

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.webkit.WebView
import androidx.annotation.RequiresApi
import com.appboy.sample.util.BrazeActionTestingUtil
import com.appboy.sample.util.ContentCardsTestingUtil
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.braze.Braze
import com.braze.BrazeActivityLifecycleCallbackListener
import com.braze.BrazeInternal
import com.braze.configuration.BrazeConfig
import com.braze.enums.BrazeSdkMetadata
import com.braze.events.BrazeSdkAuthenticationErrorEvent
import com.braze.support.BrazeLogger
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.Priority.I
import com.braze.support.BrazeLogger.brazelog
import com.braze.support.PackageUtils
import com.braze.support.hasPermission
import com.braze.ui.inappmessage.BrazeInAppMessageManager
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class DroidboyApplication : Application() {
    private var isSdkAuthEnabled: Boolean = false

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG && BuildConfig.STRICTMODE_ENABLED) {
            activateStrictMode()
        }
        WebView.setWebContentsDebuggingEnabled(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            setupChatDynamicShortcut()
        }

        registerActivityLifecycleCallbacks(BrazeActivityLifecycleCallbackListener())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (this.hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                setupNotificationChannels()
            } else {
                // Ask for the push prompt via a Braze Action #dogfooding
                brazelog { "BrazeInApp adding push prompt" }
                val brazeInAppMessageManager = BrazeInAppMessageManager.getInstance()
                brazeInAppMessageManager
                    .addInAppMessage(BrazeActionTestingUtil.getPushPromptInAppMessageModal(this.applicationContext))
                brazeInAppMessageManager.requestDisplayInAppMessage()
            }
        } else {
            setupNotificationChannels()
        }
        BrazeLogger.logLevel = applicationContext.getSharedPreferences(getString(R.string.log_level_dialog_title), MODE_PRIVATE)
            .getInt(getString(R.string.current_log_level), Log.VERBOSE)
        val sharedPreferences = applicationContext.getSharedPreferences(getString(R.string.shared_prefs_location), Context.MODE_PRIVATE)

        Braze.configure(this, null)
        val brazeConfigBuilder = BrazeConfig.Builder().setShouldUseWindowFlagSecureInActivities(true)
        brazeConfigBuilder.setSdkMetadata(EnumSet.of(BrazeSdkMetadata.MANUAL))
        setOverrideApiKeyIfConfigured(sharedPreferences, brazeConfigBuilder)
        setOverrideEndpointIfConfigured(sharedPreferences, brazeConfigBuilder)
        setMinTriggerIntervalIfConfigured(sharedPreferences, brazeConfigBuilder)
        isSdkAuthEnabled = setSdkAuthIfConfigured(sharedPreferences, brazeConfigBuilder)
        Braze.configure(this, brazeConfigBuilder.build())
        Braze.addSdkMetadata(this, EnumSet.of(BrazeSdkMetadata.BRANCH))

        if (isSdkAuthEnabled) {
            Braze.getInstance(applicationContext).subscribeToSdkAuthenticationFailures { message: BrazeSdkAuthenticationErrorEvent ->
                brazelog { "Got sdk auth error message $message" }
                message.userId?.let { setNewSdkAuthToken(it) }
            }
            // Fire off an update to start off
            Braze.getInstance(applicationContext).currentUser?.userId?.let { setNewSdkAuthToken(it) }
        }

        Braze.getInstance(applicationContext).subscribeToNetworkFailures { event ->
            brazelog { "Got braze network error event $event" }
        }

        Braze.getInstance(applicationContext).subscribeToPushNotificationEvents { event ->
            brazelog {
                """
                    Got braze push notification event $event 
                    with title '${event.notificationPayload.titleText}'
                    and deeplink '${event.notificationPayload.deeplink}'
                """.trimIndent()
            }
        }

        val droidboyPrefs = getSharedPreferences("droidboy", Context.MODE_PRIVATE)
        val placementId = droidboyPrefs.getString(MainFragment.BANNER_ID_KEY, null)
        val list = BANNER_PLACEMENT_IDS.toMutableList()
        if (!placementId.isNullOrBlank()) {
            list.add(placementId)
        }
        Braze.getInstance(applicationContext).requestBannersRefresh(list)

        listenForSpecialTabFeatureFlag()
    }

    override fun attachBaseContext(context: Context?) {
        super.attachBaseContext(context)
    }

    /**
     * Calls [Braze.changeUser] with a new SDK Auth token, if available. If no
     * token is available, just calls [Braze.changeUser] without a new SDK Auth token.
     */
    fun changeUserWithNewSdkAuthToken(userId: String) {
        val token = getSdkAuthToken(userId)
        if (token != null) {
            Braze.getInstance(applicationContext).changeUser(userId, token)
        } else {
            Braze.getInstance(applicationContext).changeUser(userId)
        }
        val sharedPreferences = getSharedPreferences("droidboy", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(MainFragment.USER_ID_KEY, userId)
        editor.apply()
    }

    private fun setNewSdkAuthToken(userId: String) {
        val token = getSdkAuthToken(userId) ?: return
        Braze.getInstance(applicationContext).setSdkAuthenticationSignature(token)
    }

    private fun getSdkAuthToken(userId: String): String? {
        if (!isSdkAuthEnabled) return null

        try {
            // Read the private key from the file
            val privateKeyPEM = applicationContext.assets.open(SDK_AUTH_KEY_FILE_PATH)
                .bufferedReader().use {
                    it.readText()
                }

            // Load the private key from the string resource
            val privateKeyPEMFormatted = privateKeyPEM
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("\\s".toRegex(), "")

            @OptIn(ExperimentalEncodingApi::class)
            val encoded = Base64.decode(privateKeyPEMFormatted)
            val keySpec = PKCS8EncodedKeySpec(encoded)
            val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)

            // Create the JWT
            val algorithm = Algorithm.RSA256(null, privateKey as RSAPrivateKey?)
            val jwt = JWT.create()
                .withAudience("braze")
                .withSubject(userId)
                .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(12)))
                .sign(algorithm)
            brazelog(I) { "Generated JWT: $jwt" }
            return jwt
        } catch (e: Exception) {
            brazelog(E) { "Failed to generate JWT: $e" }
            return null
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun setupChatDynamicShortcut() {
        val builder = ShortcutInfo.Builder(this, "droidboy_dynamic_shortcut_chat_id")
            .setShortLabel("Braze Chat")
            .setLongLabel("Conversational Push")
            .setIcon(Icon.createWithResource(this, android.R.drawable.ic_menu_send))
            .setIntent(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.braze.com?dynamicshortcut=true")
                )
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setLongLived(true)
        }

        val shortcutManager: ShortcutManager = getSystemService(ShortcutManager::class.java)
        shortcutManager.dynamicShortcuts = listOf(builder.build())
    }

    private fun setupNotificationChannels() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationGroup(notificationManager, R.string.droidboy_notification_group_01_id, R.string.droidboy_notification_group_01_name)
        createNotificationChannel(
            notificationManager, R.string.droidboy_notification_channel_01_id, R.string.droidboy_notification_channel_messages_name,
            R.string.droidboy_notification_channel_messages_desc, R.string.droidboy_notification_group_01_id
        )
        createNotificationChannel(
            notificationManager, R.string.droidboy_notification_channel_02_id, R.string.droidboy_notification_channel_matches_name,
            R.string.droidboy_notification_channel_matches_desc, R.string.droidboy_notification_group_01_id
        )
        createNotificationChannel(
            notificationManager, R.string.droidboy_notification_channel_03_id, R.string.droidboy_notification_channel_offers_name,
            R.string.droidboy_notification_channel_offers_desc, R.string.droidboy_notification_group_01_id
        )
        createNotificationChannel(
            notificationManager, R.string.droidboy_notification_channel_04_id, R.string.droidboy_notification_channel_recommendations_name,
            R.string.droidboy_notification_channel_recommendations_desc, R.string.droidboy_notification_group_01_id
        )
    }

    @SuppressLint("NewApi")
    private fun createNotificationGroup(notificationManager: NotificationManager, idResource: Int, nameResource: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannelGroup(
                NotificationChannelGroup(
                    getString(idResource),
                    getString(nameResource)
                )
            )
        }
    }

    @SuppressLint("NewApi")
    private fun createNotificationChannel(notificationManager: NotificationManager, idResource: Int, nameResource: Int, descResource: Int, groupResource: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(idResource),
                getString(nameResource),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(descResource)
            channel.enableLights(true)
            channel.lightColor = Color.RED
            channel.group = getString(groupResource)
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("NewApi")
    private fun activateStrictMode() {
        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()

        // We are explicitly not detecting detectLeakedClosableObjects(), detectLeakedSqlLiteObjects(), and detectUntaggedSockets()
        // The okhttp library used on most https calls trips the detectUntaggedSockets() check
        // com.google.android.gms.internal trips both the detectLeakedClosableObjects() and detectLeakedSqlLiteObjects() checks
        val vmPolicyBuilder = VmPolicy.Builder()
            .detectAll()
            .penaltyLog()

        // Note that some detections require a specific sdk version or higher to enable.
        vmPolicyBuilder.detectLeakedRegistrationObjects()
        vmPolicyBuilder.detectFileUriExposure()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            vmPolicyBuilder.detectCleartextNetwork()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vmPolicyBuilder.detectContentUriWithoutPermission()
            vmPolicyBuilder.detectUntaggedSockets()
        }
        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }

    private fun setOverrideApiKeyIfConfigured(sharedPreferences: SharedPreferences, config: BrazeConfig.Builder) {
        val overrideApiKey = sharedPreferences.getString(OVERRIDE_API_KEY_PREF_KEY, null)
        if (!overrideApiKey.isNullOrBlank()) {
            brazelog(I) { "Override API key found, configuring Braze with override key $overrideApiKey." }
            config.setApiKey(overrideApiKey)
            overrideApiKeyInUse = overrideApiKey
        }
    }

    private fun setOverrideEndpointIfConfigured(sharedPreferences: SharedPreferences, config: BrazeConfig.Builder) {
        val overrideEndpoint = sharedPreferences.getString(OVERRIDE_ENDPOINT_PREF_KEY, null)
        if (!overrideEndpoint.isNullOrBlank()) {
            brazelog(I) { "Override endpoint found, configuring Braze with override endpoint $overrideEndpoint." }
            config.setCustomEndpoint(overrideEndpoint)
        }
    }

    private fun setMinTriggerIntervalIfConfigured(sharedPreferences: SharedPreferences, config: BrazeConfig.Builder) {
        val minIntervalString = sharedPreferences.getString(MIN_TRIGGER_INTERVAL_KEY, null)
        if (!minIntervalString.isNullOrBlank()) {
            val minTriggerInterval = minIntervalString.toIntOrNull() ?: return
            brazelog(I) { "Min trigger interval found, configuring Braze with minimum interval $minTriggerInterval seconds." }
            config.setTriggerActionMinimumTimeIntervalSeconds(minTriggerInterval)
        }
    }

    private fun setSdkAuthIfConfigured(sharedPreferences: SharedPreferences, config: BrazeConfig.Builder): Boolean {
        // Default to true for testing dogfood purposes
        val isOverridingSdkAuth = sharedPreferences.getBoolean(ENABLE_SDK_AUTH_PREF_KEY, true)
        config.setIsSdkAuthenticationEnabled(isOverridingSdkAuth)
        return isOverridingSdkAuth
    }

    /**
     * Listens for any important Feature Flag updates to this application.
     */
    private fun listenForSpecialTabFeatureFlag() {
        Braze.getInstance(applicationContext).subscribeToFeatureFlagsUpdates {
            val specialTabFeatureFlagId = "helpful_office_tool_droidboy_tab"
            val specialTabFlag = Braze.getInstance(applicationContext).getFeatureFlag(specialTabFeatureFlagId)

            if (specialTabFlag?.enabled == true) {
                val imageUrl = specialTabFlag.getStringProperty("image_url")
                    ?: "https://raw.githubusercontent.com/Appboy/braze-android-sdk/master/braze-logo.png"
                val title = specialTabFlag.getStringProperty("helpful_title") ?: "title"
                val desc = specialTabFlag.getStringProperty("helpful_description") ?: "description"
                val card = ContentCardsTestingUtil.createCaptionedImageCardJson(specialTabFlag.id, title, desc, imageUrl)

                // Add our data to the Content Card feed
                Braze.getInstance(applicationContext).getCurrentUser { brazeUser ->
                    BrazeInternal.addSerializedContentCardToStorage(applicationContext, card.toString(), brazeUser.userId)
                }
            } else {
                // Remove the card if disabled
                val card = ContentCardsTestingUtil.getRemovedCardJson(specialTabFeatureFlagId)
                Braze.getInstance(applicationContext).getCurrentUser { brazeUser ->
                    BrazeInternal.addSerializedContentCardToStorage(applicationContext, card.toString(), brazeUser.userId)
                }
            }
        }
    }

    companion object {
        private var overrideApiKeyInUse: String? = null
        const val OVERRIDE_API_KEY_PREF_KEY = "override_api_key"
        const val OVERRIDE_ENDPOINT_PREF_KEY = "override_endpoint_url"
        const val ENABLE_SDK_AUTH_PREF_KEY = "enable_sdk_auth_if_present_pref_key"
        const val MIN_TRIGGER_INTERVAL_KEY = "min_trigger_interval"
        const val SDK_AUTH_KEY_FILE_PATH = "sdk_auth_example/example_rsa_private_key.txt"
        val BANNER_PLACEMENT_IDS =
            listOf<String>("placement_1", "placement_2", "sdk-test-1", "sdk-test-2", "custom_html")

        @JvmStatic
        fun getApiKeyInUse(context: Context): String? {
            return if (!overrideApiKeyInUse.isNullOrBlank()) {
                overrideApiKeyInUse
            } else {
                // Check if the api key is in resources
                readStringResourceValue(context, "com_braze_api_key", "NO-API-KEY-SET")
            }
        }

        private fun readStringResourceValue(context: Context, key: String?, defaultValue: String): String {
            return try {
                if (key == null) {
                    return defaultValue
                }
                val resId = context.resources.getIdentifier(key, "string", PackageUtils.getResourcePackageName(context))
                if (resId == 0) {
                    brazelog {
                        "Unable to find the xml string value with key $key. " +
                            "Using default value '$defaultValue'."
                    }
                    defaultValue
                } else {
                    context.resources.getString(resId)
                }
            } catch (ignored: Exception) {
                brazelog {
                    "Unexpected exception retrieving the xml string configuration" +
                        " value with key $key. Using default value $defaultValue'."
                }
                defaultValue
            }
        }
    }
}
