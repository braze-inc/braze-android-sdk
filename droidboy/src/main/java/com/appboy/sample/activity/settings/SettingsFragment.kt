package com.appboy.sample.activity.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.provider.MediaStore
import android.text.InputType
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.appboy.sample.BuildConfig
import com.appboy.sample.DroidboyApplication
import com.appboy.sample.R
import com.appboy.sample.SetEnvironmentPreference
import com.appboy.sample.UserProfileDialog
import com.appboy.sample.environment.EnvironmentQrDialog
import com.appboy.sample.imageloading.GlideImageLoader
import com.appboy.sample.logging.CustomEventDialog
import com.appboy.sample.logging.CustomPurchaseDialog
import com.appboy.sample.logging.CustomUserAttributeDialog
import com.appboy.sample.networkconsole.NetworkConsoleDialogFragment
import com.appboy.sample.subscriptions.EmailSubscriptionStateDialog
import com.appboy.sample.subscriptions.PushSubscriptionStateDialog
import com.appboy.sample.util.ContentCardsTestingUtil.Companion.createRandomCards
import com.appboy.sample.util.DroidboyDataStoreUtils.readPrefsBoolean
import com.appboy.sample.util.DroidboyDataStoreUtils.removePrefsKey
import com.appboy.sample.util.DroidboyDataStoreUtils.writePrefsBoolean
import com.appboy.sample.util.DroidboyDataStoreUtils.writePrefsString
import com.appboy.sample.util.DroidboyPreferenceKeys
import com.appboy.sample.util.EnvironmentUtils
import com.appboy.sample.util.LifecycleUtils
import com.appboy.sample.util.LogcatExportUtil.Companion.exportLogcatToFile
import com.appboy.sample.util.RuntimePermissionUtils
import com.braze.Braze
import com.braze.BrazeInternal
import com.braze.BrazeUser
import com.braze.Constants
import com.braze.enums.DelayedInitializationAnalyticsBehavior
import com.braze.images.DefaultBrazeImageLoader
import com.braze.models.outgoing.AttributionData
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.brazelog
import java.io.File

@SuppressLint("ApplySharedPref")
class SettingsFragment : PreferenceFragmentCompat() {
    private val requestPermissionLauncher =
        registerForActivityResult(RequestPermission()) { result ->
            Toast
                .makeText(
                    context,
                    "Location permission ${if (result) "granted" else "denied"}",
                    Toast.LENGTH_SHORT,
                ).show()
        }

    private var environmentQrPhotoUri: Uri? = null

    private val cameraActivityLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                environmentQrPhotoUri?.let { uri ->
                    try {
                        val contentResolver = requireActivity().contentResolver
                        val bitmap: Bitmap =
                            if (Build.VERSION.SDK_INT < 28) {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(contentResolver, uri)
                            } else {
                                val source: ImageDecoder.Source = ImageDecoder.createSource(contentResolver, uri)
                                // The copy() removes HARDWARE from the Bitmap.config, which prevents processing
                                ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
                            }

                        EnvironmentUtils.analyzeBitmapForEnvironmentBarcode(
                            this.requireActivity(),
                            bitmap,
                        )
                    } catch (e: Exception) {
                        brazelog(E, e) { "Error getting image" }
                    }
                }
            }
        }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val context = this.requireContext()

        setContentCardsPrefs(context)
        setSdkAuthPrefs(context)
        setNotchPrefs(context)
        setGdprPrefs(context)
        setDelayedInitPrefs(context)
        setNetworkPrefs(context)
        setImageDisplayPrefs(context)
        setSessionPrefs(context)
        setLocationPrefs(context)
        setMiscellaneousPrefs(context)
        setEnvironmentPrefs(context)
        setAboutInfo(context)
        setCustomLoggingSection()
        setInAppMessagePrefs(context)
    }

    private fun setSdkAuthPrefs(context: Context) {
        val pref = findPreference<SwitchPreferenceCompat>("sdk_auth_enabled") ?: return
        pref.isChecked = context.readPrefsBoolean(DroidboyPreferenceKeys.ENABLE_SDK_AUTH, true)
        pref.setOnPreferenceChangeListener { _, newValue ->
            context.writePrefsBoolean(DroidboyPreferenceKeys.ENABLE_SDK_AUTH, newValue as Boolean)
            LifecycleUtils.restartApp(context)
            true
        }
    }

    private fun setCustomLoggingSection() {
        showDialogOnClick("show_user_dialog", UserProfileDialog())
        showDialogOnClick("show_user_attribute_dialog", CustomUserAttributeDialog())
        showDialogOnClick("show_custom_event_dialog", CustomEventDialog())
        showDialogOnClick("show_log_purchase_dialog", CustomPurchaseDialog())
        showDialogOnClick("show_push_subscription_state_dialog", PushSubscriptionStateDialog())
        showDialogOnClick("show_email_subscription_state_dialog", EmailSubscriptionStateDialog())
    }

    private fun setAboutInfo(context: Context) {
        setSummary("sdk_version", Constants.BRAZE_SDK_VERSION)
        DroidboyApplication.getApiKeyInUse(context)?.let { setSummary("api_key", it) }
        setSummary("push_token", Braze.getInstance(context).registeredPushToken ?: "No push token registered")
        setSummary("build_type", BuildConfig.BUILD_TYPE)
        setSummary("version_code", BuildConfig.VERSION_CODE.toString())
        setSummary("build_name", BuildConfig.VERSION_NAME)
        setSummary("build_time_and_info", BuildConfig.BUILD_TIME)
        Braze.getInstance(context).runOnUser { user -> this@SettingsFragment.setSummary("current_user_id", user.userId) }
        setSummary("device_id", Braze.getInstance(context).deviceId)
    }

    /**
     * Provides the URI of a temporary file.
     *
     * NOTE: Calling this multiple times will return different URI's by the FileProvider, so call
     * once and reuse the returned value
     */
    private fun getTmpFileUri(): Uri {
        val context = requireContext()
        val tmpFile =
            File.createTempFile("tmp_image_file", ".jpg", context.cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tmpFile)
    }

    private fun setEnvironmentPrefs(context: Context) {
        setClickPreference("environment_barcode_picture_intent_key") {
            environmentQrPhotoUri = getTmpFileUri()
            cameraActivityLauncher.launch(environmentQrPhotoUri)
        }
        setClickPreference("environment_reset_key") {
            context.removePrefsKey(DroidboyPreferenceKeys.OVERRIDE_API_KEY)
            context.removePrefsKey(DroidboyPreferenceKeys.OVERRIDE_ENDPOINT)
            LifecycleUtils.restartApp(context)
        }
        setClickPreference("show_current_environment_qr") {
            EnvironmentQrDialog.newInstance().show(childFragmentManager, EnvironmentQrDialog.TAG)
        }
        setClickPreference("environment_switch_dev") { changeEndpointToDevelopment() }
        showDialogOnClick("show_set_environment_dialog", SetEnvironmentPreference())
    }

    private fun setMiscellaneousPrefs(context: Context) {
        setClickPreference("open_network_console") {
            NetworkConsoleDialogFragment().show(parentFragmentManager, NetworkConsoleDialogFragment.TAG)
        }
        setClickPreference("anonymous_revert") {
            // Note: SDK internal storage "com.appboy.offline.storagemap" is managed by the SDK
            // We only need to clear our own user ID preference
            context.removePrefsKey(DroidboyPreferenceKeys.USER_ID)
            LifecycleUtils.restartApp(context)
        }
        setClickPreference("log_attribution") {
            Braze.getInstance(context).runOnUser { user ->
                val uniqueInt = SystemClock.currentThreadTimeMillis() % 1000
                user.setAttributionData(
                    AttributionData(
                        "network_val_$uniqueInt",
                        "campaign_val_$uniqueInt",
                        "adgroup_val_$uniqueInt",
                        "creative_val_$uniqueInt",
                    ),
                )
                showToast("Attribution data sent to server")
            }
        }
        setClickPreference("logcat_export_file_key") {
            val logcatFileUri = exportLogcatToFile(context)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_STREAM, logcatFileUri)

            // Grant temporary read permission to the content URI
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Export logcat as a big text file"))
        }
    }

    private fun setLocationPrefs(context: Context) {
        setClickPreference("set_manual_location") {
            Braze.getInstance(context).runOnUser { user ->
                user.setLastKnownLocation(1.0, 2.0, 3.0, 4.0)
                showToast("Manually set location to latitude 1.0d, longitude 2.0d, altitude 3.0m, accuracy 4.0m.")
            }
        }
        setClickPreference("location_runtime_permission_dialog") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                RuntimePermissionUtils.requestPermissionWithRationale(
                    activity as Activity,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    RuntimePermissionUtils.LOCATION_RATIONALE,
                    requestPermissionLauncher,
                )
            } else {
                showToast("Below Android M there is no need to check for runtime permissions.")
            }
        }
    }

    private fun setInAppMessagePrefs(context: Context) {
        setEditTextPreference("min_trigger_interval", true) { newValue: String ->
            if (newValue.isEmpty()) {
                showToast("Clearing setting. Restart for new value to take effect")
                context.removePrefsKey(DroidboyPreferenceKeys.MIN_TRIGGER_INTERVAL)
                return@setEditTextPreference
            }

            newValue.toIntOrNull() ?: run {
                Toast.makeText(context, "Interval must be only digits", Toast.LENGTH_LONG).show()
                context.removePrefsKey(DroidboyPreferenceKeys.MIN_TRIGGER_INTERVAL)
                return@setEditTextPreference
            }

            context.writePrefsString(DroidboyPreferenceKeys.MIN_TRIGGER_INTERVAL, newValue)
            Toast.makeText(context, "Restart for new value to take effect", Toast.LENGTH_LONG).show()
        }
    }

    private fun setSessionPrefs(context: Context) {
        setClickPreference("open_session") { Braze.getInstance(context).openSession(this.activity) }
        setClickPreference("close_session") {
            Braze.getInstance(context).closeSession(this.activity)
            showToast(getString(R.string.close_session_toast))
        }
    }

    private fun setImageDisplayPrefs(context: Context) {
        val pref = findPreference<SwitchPreferenceCompat>("glide_image_loader_enabled") ?: return
        pref.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                Braze.getInstance(context).imageLoader = GlideImageLoader()
                showToast("Glide enabled")
            } else {
                Braze.getInstance(context).imageLoader = DefaultBrazeImageLoader(context)
                showToast("Glide disabled. Default Image loader in use.")
            }
            true
        }
    }

    private fun setNetworkPrefs(context: Context) {
        val outboundPref = findPreference<SwitchPreferenceCompat>("outbound_network_enabled")
        outboundPref?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            Braze.outboundNetworkRequestsOffline = !isEnabled
            val toastMessage =
                if (isEnabled) {
                    getString(R.string.enabled_outbound_network_requests_toast)
                } else {
                    getString(R.string.disabled_outbound_network_requests_toast)
                }
            showToast(toastMessage)
            true
        }
        setClickPreference("data_flush") {
            Braze.getInstance(context).requestImmediateDataFlush()
            showToast(getString(R.string.data_flush_toast))
        }
        setClickPreference("request_cc_sync") {
            Braze.getInstance(context).requestContentCardsRefresh()
            showToast(getString(R.string.data_flush_toast))
        }
        setClickPreference("request_ff_sync") {
            Braze.getInstance(context).refreshFeatureFlags()
            showToast(getString(R.string.data_flush_toast))
        }
    }

    private fun setGdprPrefs(context: Context) {
        setClickPreference("wipe_data_preference_key") { Braze.wipeData(context) }
        val sdkEnabledPref = findPreference<SwitchPreferenceCompat>("sdk_enabled")
        sdkEnabledPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                Braze.enableSdk(context)
                showToast("Braze SDK enabled")
            } else {
                Braze.disableSdk(context)
                showToast("Braze SDK disabled")
            }
            true
        }
    }

    private fun setDelayedInitPrefs(context: Context) {
        val pref = findPreference<ListPreference>("delayed_init_mode") ?: return
        pref.setOnPreferenceChangeListener { _, newValue ->
            when (newValue as String) {
                DELAYED_INIT_QUEUE -> Braze.enableDelayedInitialization(context, DelayedInitializationAnalyticsBehavior.QUEUE)
                DELAYED_INIT_DROP -> Braze.enableDelayedInitialization(context, DelayedInitializationAnalyticsBehavior.DROP)
                else -> Braze.disableDelayedInitialization(context)
            }
            true
        }
    }

    private fun setContentCardsPrefs(context: Context) {
        setClickPreference("content_card_dismiss_all_cards_setting_key") {
            val cachedContentCards = Braze.getInstance(context).getCachedContentCards()
            if (cachedContentCards != null) {
                for (card in cachedContentCards) {
                    card.isDismissed = true
                }
            }
        }
        setClickPreference("content_card_populate_random_cards_setting_key") {
            val randomCards = createRandomCards(5)
            Braze.getInstance(context).currentUser?.userId?.let { userId ->
                randomCards.iterator().forEach { card ->
                    BrazeInternal.addSerializedContentCardToStorage(context, card.forJsonPut().toString(), userId)
                }
            }
        }
    }

    private fun setNotchPrefs(context: Context) {
        setSwitchPreference("display_in_full_cutout_setting_key") { newValue: Boolean ->
            // Restart the app to force onCreate() to re-run
            // Note that an app restart won't commit prefs changes so we have to do it manually
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("display_in_full_cutout_setting_key", newValue)
                .commit()
            LifecycleUtils.restartApp(context)
        }
        setSwitchPreference("display_no_limits_setting_key") { newValue: Boolean ->
            // Restart the app to force onCreate() to re-run
            // Note that an app restart won't commit prefs changes so we have to do it manually
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("display_no_limits_setting_key", newValue)
                .commit()
            LifecycleUtils.restartApp(context)
        }
    }

    private fun showToast(message: String) {
        Handler(this.requireActivity().mainLooper).post {
            Toast.makeText(this.requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showDialogOnClick(
        key: String,
        dialog: DialogFragment,
    ) {
        setClickPreference(key) {
            dialog.show(childFragmentManager, "")
        }
    }

    private fun changeEndpointToDevelopment() {
        val context = requireContext()
        Braze.wipeData(context)
        Braze.enableSdk(context)
        if (Constants.isAmazonDevice) {
            context.writePrefsString(DroidboyPreferenceKeys.OVERRIDE_API_KEY, DEV_FIREOS_DROIDBOY_API_KEY)
        } else {
            context.writePrefsString(DroidboyPreferenceKeys.OVERRIDE_API_KEY, DEV_DROIDBOY_API_KEY)
        }
        context.writePrefsString(DroidboyPreferenceKeys.OVERRIDE_ENDPOINT, DEV_SDK_ENDPOINT)
        LifecycleUtils.restartApp(context)
    }

    companion object {
        private const val DELAYED_INIT_QUEUE = "queue"
        private const val DELAYED_INIT_DROP = "drop"
        private const val DEV_DROIDBOY_API_KEY = "da8f263e-1483-4e9f-ac0c-7b40030c8f40"
        private const val DEV_FIREOS_DROIDBOY_API_KEY = "ecb81855-149f-465c-bab0-0254d6512133"
        private const val DEV_SDK_ENDPOINT = "https://elsa.braze.com/"

        /**
         * Extension function for preferences that are only clicked
         */
        fun PreferenceFragmentCompat.setClickPreference(
            key: String,
            block: () -> Unit,
        ) {
            this.findPreference<Preference>(key)?.setOnPreferenceClickListener {
                block.invoke()
                return@setOnPreferenceClickListener true
            }
        }

        fun PreferenceFragmentCompat.setSwitchPreference(
            key: String,
            block: (newValue: Boolean) -> Unit,
        ) {
            this.findPreference<Preference>(key)?.setOnPreferenceChangeListener { _, newValue ->
                block.invoke(
                    newValue as
                        @kotlin.ParameterName(name = "newValue")
                        Boolean,
                )
                return@setOnPreferenceChangeListener true
            }
        }

        fun PreferenceFragmentCompat.setEditTextPreference(
            key: String,
            numberOnly: Boolean = false,
            block: (newValue: String) -> Unit,
        ) {
            this.findPreference<Preference>(key)?.setOnPreferenceChangeListener { _, newValue ->
                block.invoke(
                    newValue as
                        @kotlin.ParameterName(name = "newValue")
                        String,
                )
                return@setOnPreferenceChangeListener true
            }

            if (numberOnly) {
                (this.findPreference<Preference>(key) as EditTextPreference?)?.setOnBindEditTextListener {
                    it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                }
            }
        }

        fun PreferenceFragmentCompat.setSummary(
            key: String,
            summary: String,
        ) {
            this.findPreference<Preference>(key)?.summary = summary
        }

        fun Braze.runOnUser(block: (user: BrazeUser) -> Unit) {
            this.getCurrentUser { user ->
                block(user)
            }
        }
    }
}
