package com.appboy.sample.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.appboy.sample.BannersFragment
import com.appboy.sample.BuildConfig
import com.appboy.sample.InAppMessageTesterFragment
import com.appboy.sample.MainFragment
import com.appboy.sample.PushTesterFragment
import com.appboy.sample.PushUnregisterFragment
import com.appboy.sample.R
import com.appboy.sample.ecommerce.EcommerceFragment
import com.appboy.sample.featureflag.view.FeatureFlagFragment
import com.appboy.sample.networkconsole.NetworkConsoleDialogFragment
import com.appboy.sample.util.DroidboyDataStoreUtils.readPrefsString
import com.appboy.sample.util.DroidboyPreferenceKeys
import com.appboy.sample.util.EnvironmentUtils
import com.appboy.sample.util.RuntimePermissionUtils
import com.appboy.sample.util.RuntimePermissionUtils.requestPermissionWithRationale
import com.appboy.sample.util.RuntimePermissionUtils.requestPermissionsWithRationale
import com.appboy.sample.util.ViewUtils
import com.braze.Braze
import com.braze.Constants
import com.braze.configuration.BrazeConfigurationProvider
import com.braze.events.IEventSubscriber
import com.braze.events.NoMatchingTriggerEvent
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.Priority.I
import com.braze.support.BrazeLogger.brazelog
import com.braze.support.hasPermission
import com.braze.ui.contentcards.ContentCardsFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class DroidBoyActivity : AppCompatActivity() {
    private var drawerLayout: DrawerLayout? = null
    private var noMatchingTriggerEventSubscriber: IEventSubscriber<NoMatchingTriggerEvent>? = null
    private var noInAppMessageTriggeredPrefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private val requestMultiplePermissionLauncher =
        registerForActivityResult(RequestMultiplePermissions()) { result: Map<String, Boolean> ->
            if (result.containsKey(Manifest.permission.ACCESS_FINE_LOCATION) &&
                result[Manifest.permission.ACCESS_FINE_LOCATION] != true
            ) {
                showToast("Location permissions denied.")
            } else if (result.containsKey(Manifest.permission.ACCESS_BACKGROUND_LOCATION) &&
                result[Manifest.permission.ACCESS_BACKGROUND_LOCATION] != true
            ) {
                showToast("Background location permissions denied.")
            } else {
                showToast("All required location permissions granted.")
            }
        }

    private val requestLocalNetworkPermissionLauncher =
        registerForActivityResult(RequestPermission()) { granted: Boolean ->
            if (granted) {
                showToast("Local network permission granted.")
            } else {
                showToast("Local network permission denied. Override endpoint traffic may fail on Android 17+.")
            }
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        if (defaultSharedPreferences.getBoolean("display_in_full_cutout_setting_key", false)) {
            setTheme(R.style.DisplayInNotchTheme)
            ViewUtils.enableImmersiveMode(window.decorView)
        }

        if (defaultSharedPreferences.getBoolean("display_no_limits_setting_key", false)) {
            ViewUtils.enableNoLimitsMode(window)
        }
        setContentView(R.layout.landing_page)
        val currentFragment = supportFragmentManager.findFragmentById(R.id.viewpager)
        brazelog(I) { "Creating DroidBoyActivity with current fragment: $currentFragment" }
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = null
        val viewPager = findViewById<ViewPager2>(R.id.viewpager)
        viewPager?.let { setupViewPager(it) }

        val tabLayout = findViewById<TabLayout>(R.id.tabs)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = (viewPager.adapter as Adapter).getTitle(position)
        }.attach()
        drawerLayout = findViewById(R.id.root)
        setupNoInAppMessageTriggeredListener()
    }

    private fun setupNoInAppMessageTriggeredListener() {
        fun handlePref(value: Boolean) {
            if (value) {
                if (noMatchingTriggerEventSubscriber == null) {
                    noMatchingTriggerEventSubscriber =
                        IEventSubscriber { message ->
                            runOnUiThread {
                                // A simple non-Braze created message that we do on our own
                                val dialog =
                                    AlertDialog
                                        .Builder(this)
                                        .setMessage("Received no trigger for ${message.sourceEventType}")
                                        .setTitle("Non-Braze Message")
                                        .setPositiveButton(R.string.user_dialog_okay) { _, _ -> }
                                        .create()
                                dialog.show()
                            }
                        }
                    noMatchingTriggerEventSubscriber?.let {
                        Braze
                            .getInstance(this)
                            .subscribeToNoMatchingTriggerForEvent(it)
                    }
                }
            } else if (noMatchingTriggerEventSubscriber != null) {
                noMatchingTriggerEventSubscriber?.let {
                    Braze.getInstance(this).removeSingleSubscription(it, NoMatchingTriggerEvent::class.java)
                }
                noMatchingTriggerEventSubscriber = null
            }
        }

        val inAppMessagesSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        noInAppMessageTriggeredPrefListener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs: SharedPreferences, prefName: String? ->
                if (prefName != "show_own_message_when_inapp_msg_not_triggered") {
                    return@OnSharedPreferenceChangeListener
                }
                handlePref(
                    sharedPrefs.getBoolean(
                        "show_own_message_when_inapp_msg_not_triggered",
                        false,
                    ),
                )
            }
        inAppMessagesSharedPrefs.registerOnSharedPreferenceChangeListener(
            noInAppMessageTriggeredPrefListener,
        )
        handlePref(
            inAppMessagesSharedPrefs.getBoolean(
                "show_own_message_when_inapp_msg_not_triggered",
                false,
            ),
        )
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val locationPermissionsToRequest = mutableListOf(Manifest.permission.INTERNET)
        if (!didRequestLocationPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val hasFineLocationPermission =
                    applicationContext.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                if (!hasFineLocationPermission) {
                    locationPermissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                } else if (!applicationContext.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    // Request background now that fine is set
                    locationPermissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("UnnecessaryParentheses")
                val hasAllPermissions = (
                    applicationContext.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) &&
                        applicationContext.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                )
                if (!hasAllPermissions) {
                    // Request both BACKGROUND and FINE location permissions
                    locationPermissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    locationPermissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            } else {
                // From M to P, FINE gives us BACKGROUND access
                if (!applicationContext.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Request only FINE location permission
                    locationPermissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            didRequestLocationPermission = true
        }
        requestPermissionsWithRationale(
            this,
            locationPermissionsToRequest.toTypedArray(),
            RuntimePermissionUtils.LOCATION_RATIONALE,
            requestMultiplePermissionLauncher,
        )

        if (shouldRequestLocalNetworkPermission()) {
            requestPermissionWithRationale(
                this,
                ACCESS_LOCAL_NETWORK_PERMISSION,
                RuntimePermissionUtils.LOCAL_NETWORK_RATIONALE,
                requestLocalNetworkPermissionLauncher,
            )
        }
    }

    /**
     * Android 17 (API 37) requires apps to hold ACCESS_LOCAL_NETWORK before they
     * can reach LAN addresses (10.0.2.2 from an emulator, Wi-Fi LAN IPs,
     * Charles / Proxyman on the dev's laptop, etc.). When Droidboy has been
     * pointed at a non-loopback override endpoint, make sure we ask for it so
     * dev traffic keeps flowing. Loopback and public domains are unaffected.
     */
    private fun shouldRequestLocalNetworkPermission(): Boolean {
        if (Build.VERSION.SDK_INT < ANDROID_17_API_LEVEL) return false
        val overrideEndpoint =
            applicationContext.readPrefsString(
                DroidboyPreferenceKeys.OVERRIDE_ENDPOINT,
            )
        if (overrideEndpoint.isNullOrBlank() || isLoopbackEndpoint(overrideEndpoint)) return false
        if (applicationContext.hasPermission(ACCESS_LOCAL_NETWORK_PERMISSION)) return false
        return true
    }

    private fun isLoopbackEndpoint(endpoint: String): Boolean {
        val withScheme = if (endpoint.contains("://")) endpoint else "http://$endpoint"
        val host =
            android.net.Uri
                .parse(withScheme)
                .host
                ?.lowercase() ?: return false
        return host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "[::1]"
    }

    private fun setupViewPager(viewPager: ViewPager2) {
        val adapter = Adapter(supportFragmentManager, lifecycle, applicationContext)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = adapter.itemCount + 1
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    public override fun onResume() {
        super.onResume()
        processIntent()
        val configurationProvider = BrazeConfigurationProvider(this)
        val endpoint =
            configurationProvider.customEndpoint.let {
                if (it.isNullOrEmpty()) {
                    configurationProvider.baseUrlForRequests
                } else {
                    it
                }
            }

        (findViewById<View>(R.id.toolbar_info_endpoint) as TextView).text = "endpoint: $endpoint"
        val configuredApiKey = Braze.getConfiguredApiKey(configurationProvider)
        (findViewById<View>(R.id.toolbar_info_api_key) as TextView).text = "current api key: $configuredApiKey"
        (findViewById<View>(R.id.toolbar_info_build_info) as TextView).text = BuildConfig.BUILD_TIME
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.actionbar_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.action_compose -> startActivity(Intent(this, ComposeActivity::class.java))
            R.id.geofences_map -> {
                drawerLayout?.closeDrawers()
                startActivity(Intent(applicationContext, GeofencesMapActivity::class.java))
            }
            R.id.iam_sandbox -> startActivity(Intent(applicationContext, InAppMessageSandboxActivity::class.java))
            R.id.edge_to_edge_html_iam ->
                startActivity(Intent(applicationContext, EdgeToEdgeHtmlIamActivity::class.java))
            R.id.action_network_console ->
                NetworkConsoleDialogFragment().show(
                    supportFragmentManager,
                    NetworkConsoleDialogFragment.TAG,
                )
            R.id.action_flush -> {
                Braze.getInstance(this).requestContentCardsRefresh()
                Braze.getInstance(this).requestImmediateDataFlush()
                showToast("Requested data flush and content card sync.")
            }
            R.id.sample_activity -> startActivity(Intent(this, SampleActivity::class.java))
            R.id.sample_appcompatactivity -> startActivity(Intent(this, SampleAppCompatActivity::class.java))
            R.id.translucent_activity -> startActivity(Intent(this, TranslucentActivity::class.java))
            else -> {
                brazelog(E) { "The ${item.title} options item was not found. Ignoring." }
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    private fun replaceCurrentFragment(newFragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val currentFragment = fragmentManager.findFragmentById(R.id.root)
        if (currentFragment != null && currentFragment.javaClass == newFragment.javaClass) {
            brazelog(I) {
                "Fragment of type ${currentFragment.javaClass} is already the active fragment. Ignoring " +
                    "request to replace current fragment."
            }
            return
        }
        hideSoftKeyboard()
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out,
            android.R.anim.fade_in,
            android.R.anim.fade_out,
        )
        fragmentTransaction.replace(R.id.root, newFragment, newFragment.javaClass.toString())
        if (currentFragment != null) {
            fragmentTransaction.addToBackStack(newFragment.javaClass.toString())
        } else {
            fragmentTransaction.addToBackStack(null)
        }
        fragmentTransaction.commit()
    }

    private fun processIntent() {
        val data = intent.data
        if (data != null &&
            data.scheme == EnvironmentUtils.BRAZE_ENVIRONMENT_DEEPLINK_SCHEME &&
            data.host == EnvironmentUtils.BRAZE_ENVIRONMENT_DEEPLINK_HOST
        ) {
            EnvironmentUtils.setEnvironmentViaDeepLink(this, data)
            intent = Intent()
            return
        }

        // Check to see if the Activity was opened by the Broadcast Receiver. If it was, navigate to the
        // correct fragment.
        val extras = intent.extras
        if (extras != null && Constants.BRAZE_INTENT_SOURCE == extras.getString(resources.getString(R.string.source_key))) {
            navigateToDestination(extras)
            val bundleLogString = bundleToLogString(extras)
            showToast(bundleLogString)
            brazelog { bundleLogString }
        }

        // Clear the intent so that screen rotations don't cause the intent to be re-executed on.
        intent = Intent()
    }

    private fun navigateToDestination(extras: Bundle) {
        // DESTINATION_VIEW holds the name of the fragment we're trying to visit.
        val destination = extras.getString(resources.getString(R.string.destination_view))
        if (resources.getString(R.string.home) == destination) {
            replaceCurrentFragment(MainFragment())
        }
    }

    private fun hideSoftKeyboard() {
        currentFocus?.let {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(
                it.windowToken,
                InputMethodManager.HIDE_IMPLICIT_ONLY,
            )
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Adapter that has all the information for the Fragments to feed the ViewPager2
     */
    internal class Adapter(
        fm: FragmentManager,
        lifecycle: Lifecycle,
        val context: Context,
    ) : FragmentStateAdapter(fm, lifecycle) {
        data class FragmentInfo(
            val fragmentConstructor: () -> Fragment,
            val title: String,
        )

        private val fragmentInfo =
            arrayOf(
                FragmentInfo(
                    { MainFragment() },
                    context.getString(R.string.tab_user),
                ),
                FragmentInfo(
                    { ContentCardsFragment() },
                    context.getString(R.string.tab_cards),
                ),
                FragmentInfo(
                    { InAppMessageTesterFragment() },
                    context.getString(R.string.inappmessage_tester_tab_title),
                ),
                FragmentInfo(
                    { PushTesterFragment() },
                    context.getString(R.string.tab_push),
                ),
                FragmentInfo(
                    { PushUnregisterFragment() },
                    context.getString(R.string.tab_push_unregister),
                ),
                FragmentInfo(
                    { FeatureFlagFragment() },
                    context.getString(R.string.tab_flags),
                ),
                FragmentInfo(
                    { BannersFragment() },
                    context.getString(R.string.tab_banners),
                ),
                FragmentInfo(
                    { EcommerceFragment() },
                    context.getString(R.string.tab_ecommerce),
                ),
            )

        override fun getItemCount() = fragmentInfo.size

        override fun createFragment(position: Int) = fragmentInfo[position].fragmentConstructor.invoke()

        fun getTitle(position: Int) = fragmentInfo[position].title
    }

    companion object {
        private var didRequestLocationPermission = false

        private const val ANDROID_17_API_LEVEL = 37
        private const val ACCESS_LOCAL_NETWORK_PERMISSION =
            "android.permission.ACCESS_LOCAL_NETWORK"

        private fun bundleToLogString(bundle: Bundle): String {
            val bundleString = StringBuilder()
            bundleString.append("Received intent with extras Bundle of size ${bundle.size()} from Braze containing [")
            for (key in bundle.keySet()) {
                @Suppress("DEPRECATION")
                bundleString.append(" '$key':'${bundle[key]}'")
            }
            bundleString.append(" ].")
            return bundleString.toString()
        }
    }
}
