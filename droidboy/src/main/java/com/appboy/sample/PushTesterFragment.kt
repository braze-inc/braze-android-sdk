package com.appboy.sample

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import com.appboy.sample.util.SpinnerUtils
import com.braze.Braze.Companion.customBrazeNotificationFactory
import com.braze.Constants
import com.braze.Constants.isAmazonDevice
import com.braze.models.push.BrazeNotificationPayload
import com.braze.push.BrazeNotificationUtils.getNotificationId
import com.braze.push.BrazePushReceiver
import com.braze.push.BrazePushReceiver.Companion.handleReceivedIntent
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.Priority.W
import com.braze.support.BrazeLogger.brazelog
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.util.Random

class PushTesterFragment : Fragment(), AdapterView.OnItemSelectedListener {
    private var priority = "0"
    private var image: String? = null
    private var clickActionUrl: String? = null
    private var category: String? = null
    private var visibility: String? = null
    private var actionType: String? = null
    private var accentColorString: String? = null
    private var largeIconString: String? = null
    private var notificationFactoryType: String? = null
    private var pushStoryTitleGravity: String? = null
    private var pushStorySubtitleGravity: String? = null
    private var channel: String? = null

    private var pushStoryType = 0
    private var pushStoryNumPages = 0

    private var shouldUseSummary = false
    private var shouldUseBigSummary = false
    private var shouldUseImage = false
    private var shouldOverflowText = false
    private var shouldUseBigTitle = false
    private var shouldUseClickAction = false
    private var shouldUseCategory = false
    private var shouldUseVisibility = false
    private var shouldSetPublicVersion = false
    private var shouldSetAccentColor = false
    private var shouldSetLargeIcon = false
    private var shouldOpenInWebview = false
    private var shouldTestTriggerFetch = false
    private var shouldSetChannel = false
    private var shouldUseConstantNotificationId = false
    private var shouldSetStoryDeepLink = false
    private var shouldSetStoryTitles = true
    private var shouldSetStorySubtitles = true
    private var isInlineImagePushEnabled = false
    private var isConversationPushEnabled = false
    private var shouldSetHtmlText = false
    private var shouldTestPushDeliveryEvents = false

    private val publicVersionNotificationString: String
        get() {
            val publicVersionJSON = JSONObject()
            publicVersionJSON.put(Constants.BRAZE_PUSH_TITLE_KEY, "Don't open in public (title)")
            publicVersionJSON.put(Constants.BRAZE_PUSH_CONTENT_KEY, "Please (content)")
            publicVersionJSON.put(Constants.BRAZE_PUSH_SUMMARY_TEXT_KEY, "Summary")
            return publicVersionJSON.toString()
        }

    override fun onCreateView(layoutInflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = layoutInflater.inflate(R.layout.push_tester, container, false)
        view.findViewById<View>(R.id.push_tester_big_title).onCheckboxChecked { shouldUseBigTitle = it }
        view.findViewById<View>(R.id.push_tester_summary).onCheckboxChecked { shouldUseSummary = it }
        view.findViewById<View>(R.id.push_tester_big_summary).onCheckboxChecked { shouldUseBigSummary = it }
        view.findViewById<View>(R.id.push_tester_overflow_text).onCheckboxChecked { shouldOverflowText = it }
        view.findViewById<View>(R.id.push_tester_set_public_version).onCheckboxChecked { shouldSetPublicVersion = it }
        view.findViewById<View>(R.id.push_tester_test_triggers).onCheckboxChecked { shouldTestTriggerFetch = it }
        view.findViewById<View>(R.id.push_tester_constant_nid).onCheckboxChecked { shouldUseConstantNotificationId = it }
        view.findViewById<View>(R.id.push_tester_set_open_webview).onCheckboxChecked { shouldOpenInWebview = it }
        view.findViewById<View>(R.id.push_tester_story_deep_link).onCheckboxChecked { shouldSetStoryDeepLink = it }
        view.findViewById<View>(R.id.push_tester_story_title).onCheckboxChecked { shouldSetStoryTitles = !it }
        view.findViewById<View>(R.id.push_tester_story_subtitle).onCheckboxChecked { shouldSetStorySubtitles = !it }
        view.findViewById<View>(R.id.push_tester_inline_image_push_enabled).onCheckboxChecked { isInlineImagePushEnabled = it }
        view.findViewById<View>(R.id.push_tester_conversational_push_enabled).onCheckboxChecked { isConversationPushEnabled = it }
        view.findViewById<View>(R.id.push_tester_html).onCheckboxChecked { shouldSetHtmlText = it }
        view.findViewById<View>(R.id.test_push_delivery_events).onCheckboxChecked { shouldTestPushDeliveryEvents = it }

        SpinnerUtils.setUpSpinner(view.findViewById(R.id.push_image_spinner), this, R.array.push_image_options)
        SpinnerUtils.setUpSpinner(view.findViewById(R.id.push_image_number_spinner), this, R.array.push_image_number_options)
        SpinnerUtils.setUpSpinner(view.findViewById(R.id.push_priority_spinner), this, R.array.push_priority_options)
        SpinnerUtils.setUpSpinner(view.findViewById(R.id.push_click_action_spinner), this, R.array.push_click_action_options)
        SpinnerUtils.setUpSpinner(view.findViewById(R.id.push_category_spinner), this, R.array.push_category_options)
        SpinnerUtils.setUpSpinner(view.findViewById(R.id.push_visibility_spinner), this, R.array.push_visibility_options)
        SpinnerUtils.setUpSpinner(view.findViewById(R.id.push_image_spinner), this, R.array.push_image_options)
        SpinnerUtils.setUpSpinner(view.findViewById(R.id.push_story_title_align_spinner), this, R.array.push_story_title_align_options)
        SpinnerUtils.setUpSpinner(view.findViewById(R.id.push_story_subtitle_align_spinner), this, R.array.push_story_subtitle_align_options)
        SpinnerUtils.setUpSpinner(view.findViewById(R.id.push_action_spinner), this, R.array.push_action_options)
        SpinnerUtils.setUpSpinner(view.findViewById(R.id.push_accent_color_spinner), this, R.array.push_accent_color_options)
        SpinnerUtils.setUpSpinner(view.findViewById(R.id.push_large_icon_spinner), this, R.array.push_large_icon_options)
        SpinnerUtils.setUpSpinner(view.findViewById(R.id.push_notification_factory_spinner), this, R.array.push_notification_factory_options)
        SpinnerUtils.setUpSpinner(view.findViewById(R.id.push_channel_spinner), this, R.array.push_channel_options)

        view.findViewById<Button>(R.id.test_push_button).setOnClickListener {
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                onSendPushButtonClicked()
            }
        }
        return view
    }

    @Suppress("ComplexMethod")
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (parent == null) return

        when (parent.id) {
            R.id.push_image_spinner -> {
                val pushImageUriString = resources.getStringArray(R.array.push_image_values)[parent.selectedItemPosition]
                if (!pushImageUriString.isNullOrBlank()) {
                    if (pushImageUriString == getString(R.string.push_story)) {
                        pushStoryType = 1
                        shouldUseImage = false
                    } else {
                        pushStoryType = 0
                        shouldUseImage = true
                        image = pushImageUriString
                    }
                } else {
                    shouldUseImage = false
                    pushStoryType = 0
                }
            }

            R.id.push_image_number_spinner -> {
                val pushImageNumberString = resources.getStringArray(R.array.push_image_number_values)[parent.selectedItemPosition]
                pushStoryNumPages = pushImageNumberString.toInt()
            }

            R.id.push_story_title_align_spinner ->
                pushStoryTitleGravity =
                    resources.getStringArray(R.array.push_story_title_align_values)[parent.selectedItemPosition]

            R.id.push_story_subtitle_align_spinner ->
                pushStorySubtitleGravity =
                    resources.getStringArray(R.array.push_story_subtitle_align_values)[parent.selectedItemPosition]

            R.id.push_priority_spinner -> priority = resources.getStringArray(R.array.push_priority_values)[parent.selectedItemPosition]
            R.id.push_click_action_spinner -> {
                val pushClickActionUriString = resources.getStringArray(R.array.push_click_action_values)[parent.selectedItemPosition]
                if (!pushClickActionUriString.isNullOrBlank()) {
                    shouldUseClickAction = true
                    clickActionUrl = pushClickActionUriString
                } else {
                    shouldUseClickAction = false
                }
            }

            R.id.push_category_spinner -> {
                category = resources.getStringArray(R.array.push_category_values)[parent.selectedItemPosition]
                shouldUseCategory = !category.isNullOrBlank()
            }

            R.id.push_visibility_spinner -> {
                visibility = resources.getStringArray(R.array.push_visibility_values)[parent.selectedItemPosition]
                shouldUseVisibility = !visibility.isNullOrBlank()
            }

            R.id.push_action_spinner -> actionType = resources.getStringArray(R.array.push_action_values)[parent.selectedItemPosition]
            R.id.push_accent_color_spinner -> {
                val pushAccentColorString = resources.getStringArray(R.array.push_accent_color_values)[parent.selectedItemPosition]
                if (!pushAccentColorString.isNullOrBlank()) {
                    shouldSetAccentColor = true
                    // Convert our hexadecimal string to the decimal expected by Braze
                    accentColorString = java.lang.Long.decode(pushAccentColorString).toString()
                } else {
                    shouldSetAccentColor = false
                }
            }

            R.id.push_large_icon_spinner -> {
                val largeIconString = resources.getStringArray(R.array.push_large_icon_values)[parent.selectedItemPosition]
                if (!largeIconString.isNullOrBlank()) {
                    shouldSetLargeIcon = true
                    this.largeIconString = largeIconString
                } else {
                    shouldSetLargeIcon = false
                }
            }

            R.id.push_notification_factory_spinner ->
                notificationFactoryType =
                    resources.getStringArray(R.array.push_notification_factory_values)[parent.selectedItemPosition]

            R.id.push_channel_spinner -> {
                channel = resources.getStringArray(R.array.push_channel_values)[parent.selectedItemPosition]
                shouldSetChannel = !channel.isNullOrBlank()
            }

            else -> brazelog(W) { "Item selected for unknown spinner" }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do nothing
    }

    @Suppress("ComplexMethod")
    private fun onSendPushButtonClicked() {
        var notificationExtras = Bundle()
        if (shouldSetHtmlText) {
            notificationExtras.putString(Constants.BRAZE_PUSH_TITLE_KEY, getString(R.string.html_push_title_text))
            notificationExtras.putString(Constants.BRAZE_PUSH_CONTENT_KEY, getString(R.string.html_push_body_text))
        } else {
            notificationExtras.putString(Constants.BRAZE_PUSH_TITLE_KEY, generateDisplayValue(TITLE))
            notificationExtras.putString(Constants.BRAZE_PUSH_CONTENT_KEY, generateDisplayValue(CONTENT + random.nextInt()))
        }
        notificationExtras.putString(Constants.BRAZE_PUSH_BRAZE_KEY, TRUE_STRING)
        val notificationId = if (shouldUseConstantNotificationId) {
            "100"
        } else {
            getNotificationId(BrazeNotificationPayload(notificationExtras)).toString()
        }
        notificationExtras.putString(Constants.BRAZE_PUSH_CUSTOM_NOTIFICATION_ID, notificationId)
        notificationExtras = addActionButtons(notificationExtras)
        if (isInlineImagePushEnabled) {
            notificationExtras.putString(Constants.BRAZE_PUSH_INLINE_IMAGE_STYLE_KEY, TRUE_STRING)
        }
        if (shouldUseSummary) {
            notificationExtras.putString(Constants.BRAZE_PUSH_SUMMARY_TEXT_KEY, generateDisplayValue(SUMMARY_TEXT))
        }
        if (shouldUseClickAction) {
            notificationExtras.putString(Constants.BRAZE_PUSH_DEEP_LINK_KEY, clickActionUrl)
        }
        notificationExtras.putString(Constants.BRAZE_PUSH_PRIORITY_KEY, priority)
        if (shouldUseBigTitle) {
            notificationExtras.putString(Constants.BRAZE_PUSH_BIG_TITLE_TEXT_KEY, generateDisplayValue(BIG_TITLE))
        }
        if (shouldUseBigSummary) {
            notificationExtras.putString(Constants.BRAZE_PUSH_BIG_SUMMARY_TEXT_KEY, generateDisplayValue(BIG_SUMMARY))
        }
        if (shouldUseCategory) {
            notificationExtras.putString(Constants.BRAZE_PUSH_CATEGORY_KEY, category)
        }
        if (shouldUseVisibility) {
            notificationExtras.putString(Constants.BRAZE_PUSH_VISIBILITY_KEY, visibility)
        }
        if (shouldOpenInWebview) {
            notificationExtras.putString(Constants.BRAZE_PUSH_OPEN_URI_IN_WEBVIEW_KEY, TRUE_STRING)
        }
        if (shouldSetPublicVersion) {
            try {
                notificationExtras.putString(Constants.BRAZE_PUSH_PUBLIC_NOTIFICATION_KEY, publicVersionNotificationString)
            } catch (e: JSONException) {
                brazelog(E, e) { "Failed to created public version notification JSON string" }
            }
        }
        if (shouldTestTriggerFetch) {
            notificationExtras.putString(Constants.BRAZE_PUSH_FETCH_TEST_TRIGGERS_KEY, TRUE_STRING)
        }
        if (shouldSetAccentColor) {
            notificationExtras.putString(Constants.BRAZE_PUSH_ACCENT_KEY, accentColorString)
        }
        if (shouldSetLargeIcon) {
            notificationExtras.putString(Constants.BRAZE_PUSH_LARGE_ICON_KEY, largeIconString)
        }
        if (shouldSetChannel) {
            notificationExtras.putString(Constants.BRAZE_PUSH_NOTIFICATION_CHANNEL_ID_KEY, channel)
        }
        setNotificationFactory()
        if (pushStoryType != 0) {
            addPushStoryPages(notificationExtras)
            notificationExtras.putString(Constants.BRAZE_PUSH_STORY_KEY, pushStoryType.toString())
        }
        if (isConversationPushEnabled) {
            notificationExtras.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_STYLE_KEY, "1")
            addConversationPush(notificationExtras)
        }
        if (shouldTestPushDeliveryEvents) {
            notificationExtras.putString(Constants.BRAZE_PUSH_DELIVERY_ENABLED_KEY, "1")
            notificationExtras.putString(Constants.BRAZE_PUSH_CAMPAIGN_ID_KEY, "test-fake-campaign-id-" + System.currentTimeMillis())
            notificationExtras.putString(Constants.BRAZE_PUSH_DELIVERY_FLUSH_MIN_KEY, "0")
            notificationExtras.putString(Constants.BRAZE_PUSH_DELIVERY_FLUSH_MAX_KEY, "1")
        }

        // Manually build the Braze extras bundle.
        var extrasBundle = Bundle()
        if (shouldUseImage) {
            var pushImageUrl = image.orEmpty()
            // Template the image if it's random
            if (image == getString(R.string.random_2_by_1_image_url)) {
                pushImageUrl = "https://picsum.photos/seed/" + System.nanoTime() + "/800/400"
            } else if (image == getString(R.string.random_3_by_2_image_url)) {
                pushImageUrl = "https://picsum.photos/seed/" + System.nanoTime() + "/750/500"
            }
            if (isAmazonDevice) {
                // Amazon flattens the extras bundle so we have to put it in the regular notification
                // extras to imitate that functionality.
                notificationExtras.putString(Constants.BRAZE_PUSH_BIG_IMAGE_URL_KEY, pushImageUrl.replace("&amp;".toRegex(), "&"))
                extrasBundle = Bundle(notificationExtras)
            } else {
                extrasBundle.putString(Constants.BRAZE_PUSH_BIG_IMAGE_URL_KEY, pushImageUrl.replace("&amp;".toRegex(), "&"))
            }
        }
        extrasBundle.putString(EXAMPLE_EXTRA_KEY_1, "Hamburgers")
        extrasBundle.putString(EXAMPLE_EXTRA_KEY_2, "Fries")
        extrasBundle.putString(EXAMPLE_EXTRA_KEY_3, "Lemonade")
        notificationExtras.putBundle(Constants.BRAZE_PUSH_EXTRAS_KEY, extrasBundle)
        val pushIntent = Intent(BrazePushReceiver.FIREBASE_MESSAGING_SERVICE_ROUTING_ACTION)
        pushIntent.putExtras(notificationExtras)
        handleReceivedIntent(requireContext(), pushIntent)
    }

    /**
     * Add the push story fields to the notificationExtras bundle.
     *
     * @param notificationExtras Notification extras as provided by FCM/ADM.
     * @return the modified notificationExtras, now including the image/text information for the push story.
     */
    private fun addPushStoryPages(notificationExtras: Bundle): Bundle {
        for (i in 0 until pushStoryNumPages) {
            if (shouldSetStoryDeepLink) {
                notificationExtras.putString(Constants.BRAZE_PUSH_STORY_DEEP_LINK_KEY_TEMPLATE.replace("*", i.toString()), PUSH_STORY_PAGE_VALUES[i].deeplink)
            }
            if (shouldSetStoryTitles) {
                notificationExtras.putString(Constants.BRAZE_PUSH_STORY_TITLE_KEY_TEMPLATE.replace("*", i.toString()), PUSH_STORY_PAGE_VALUES[i].title)
            }
            if (shouldSetStorySubtitles) {
                notificationExtras.putString(Constants.BRAZE_PUSH_STORY_SUBTITLE_KEY_TEMPLATE.replace("*", i.toString()), PUSH_STORY_PAGE_VALUES[i].subtitle)
            }
            notificationExtras.putString(Constants.BRAZE_PUSH_STORY_IMAGE_KEY_TEMPLATE.replace("*", i.toString()), PUSH_STORY_PAGE_VALUES[i].imageUrl)
            if (!pushStoryTitleGravity.isNullOrBlank()) {
                val replaced = Constants.BRAZE_PUSH_STORY_TITLE_JUSTIFICATION_KEY_TEMPLATE.replace("*", i.toString())
                notificationExtras.putString(replaced, pushStoryTitleGravity)
            }
            if (!pushStorySubtitleGravity.isNullOrBlank()) {
                val replaced = Constants.BRAZE_PUSH_STORY_SUBTITLE_JUSTIFICATION_KEY_TEMPLATE.replace("*", i.toString())
                notificationExtras.putString(replaced, pushStorySubtitleGravity)
            }
            if (shouldOpenInWebview) {
                notificationExtras.putString(Constants.BRAZE_PUSH_STORY_USE_WEBVIEW_KEY_TEMPLATE.replace("*", i.toString()), "true")
            }
        }
        notificationExtras.putBoolean(Constants.BRAZE_PUSH_STORY_IS_NEWLY_RECEIVED, true)
        return notificationExtras
    }

    private fun addActionButtons(notificationExtras: Bundle): Bundle {
        if (actionType.isNullOrBlank()) {
            return notificationExtras
        }
        when (actionType) {
            Constants.BRAZE_PUSH_ACTION_TYPE_OPEN -> {
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TYPE_KEY_TEMPLATE.replace("*", "0"), Constants.BRAZE_PUSH_ACTION_TYPE_OPEN)
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TEXT_KEY_TEMPLATE.replace("*", "0"), "Open app")
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TYPE_KEY_TEMPLATE.replace("*", "1"), Constants.BRAZE_PUSH_ACTION_TYPE_NONE)
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TEXT_KEY_TEMPLATE.replace("*", "1"), getString(R.string.droidboy_close_button_text))
            }

            Constants.BRAZE_PUSH_ACTION_TYPE_URI -> {
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TYPE_KEY_TEMPLATE.replace("*", "0"), Constants.BRAZE_PUSH_ACTION_TYPE_URI)
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TEXT_KEY_TEMPLATE.replace("*", "0"), "Braze (webview)")
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_URI_KEY_TEMPLATE.replace("*", "0"), getString(R.string.braze_homepage_url))
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_USE_WEBVIEW_KEY_TEMPLATE.replace("*", "0"), "true")
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TYPE_KEY_TEMPLATE.replace("*", "1"), Constants.BRAZE_PUSH_ACTION_TYPE_URI)
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TEXT_KEY_TEMPLATE.replace("*", "1"), "Google")
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_URI_KEY_TEMPLATE.replace("*", "1"), getString(R.string.google_url))
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_USE_WEBVIEW_KEY_TEMPLATE.replace("*", "1"), "false")
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TYPE_KEY_TEMPLATE.replace("*", "2"), Constants.BRAZE_PUSH_ACTION_TYPE_NONE)
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TEXT_KEY_TEMPLATE.replace("*", "2"), getString(R.string.droidboy_close_button_text))
                if (shouldOpenInWebview) {
                    notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_USE_WEBVIEW_KEY_TEMPLATE.replace("*", "0"), "true")
                    notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_USE_WEBVIEW_KEY_TEMPLATE.replace("*", "1"), "true")
                }
            }

            "deep_link" -> {
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TYPE_KEY_TEMPLATE.replace("*", "0"), Constants.BRAZE_PUSH_ACTION_TYPE_URI)
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TEXT_KEY_TEMPLATE.replace("*", "0"), "Preferences")
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_URI_KEY_TEMPLATE.replace("*", "0"), getString(R.string.droidboy_deep_link))
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TYPE_KEY_TEMPLATE.replace("*", "1"), Constants.BRAZE_PUSH_ACTION_TYPE_URI)
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TEXT_KEY_TEMPLATE.replace("*", "1"), "Telephone")
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_URI_KEY_TEMPLATE.replace("*", "1"), getString(R.string.telephone_uri))
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TYPE_KEY_TEMPLATE.replace("*", "2"), Constants.BRAZE_PUSH_ACTION_TYPE_NONE)
                notificationExtras.putString(Constants.BRAZE_PUSH_ACTION_TEXT_KEY_TEMPLATE.replace("*", "2"), getString(R.string.droidboy_close_button_text))
            }

            else -> {}
        }
        return notificationExtras
    }

    private fun addConversationPush(bundle: Bundle) {
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_SHORTCUT_ID_KEY, "droidboy_dynamic_shortcut_chat_id")
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_REPLY_PERSON_ID_KEY, "person2")

        // Add messages
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_MESSAGE_TEXT_TEMPLATE.replace("*", "0"), "Message 1")
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_MESSAGE_PERSON_ID_TEMPLATE.replace("*", "0"), "person1")
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_MESSAGE_TIMESTAMP_TEMPLATE.replace("*", "0"), (System.currentTimeMillis() - 3600).toString())
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_MESSAGE_TEXT_TEMPLATE.replace("*", "1"), "Message 2")
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_MESSAGE_PERSON_ID_TEMPLATE.replace("*", "1"), "person2")
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_MESSAGE_TIMESTAMP_TEMPLATE.replace("*", "1"), System.currentTimeMillis().toString())

        // Add persons
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_PERSON_ID_TEMPLATE.replace("*", "0"), "person1")
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_PERSON_NAME_TEMPLATE.replace("*", "0"), "Jack Black")
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_PERSON_ID_TEMPLATE.replace("*", "1"), "person2")
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_PERSON_NAME_TEMPLATE.replace("*", "1"), "Giraffe")
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_PERSON_URI_TEMPLATE.replace("*", "1"), "mailto://giraffe@zoo.org")
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_PERSON_IS_BOT_TEMPLATE.replace("*", "1"), "true")
        bundle.putString(Constants.BRAZE_CONVERSATIONAL_PUSH_PERSON_IS_IMPORTANT_TEMPLATE.replace("*", "1"), "true")
    }

    // If shouldOverflowText is specified we concatenate an append string
    // This is to test big text and ellipsis cutoff in varying screen sizes
    private fun generateDisplayValue(field: String): String {
        return if (shouldOverflowText) {
            field + getString(R.string.overflow_string)
        } else field
    }

    /**
     * Sets the Braze instance's notification factory.
     */
    private fun setNotificationFactory() {
        customBrazeNotificationFactory = when (notificationFactoryType) {
            "DroidboyNotificationFactory" -> {
                DroidboyNotificationFactory()
            }

            "FullyCustomNotificationFactory" -> {
                FullyCustomNotificationFactory()
            }

            else -> {
                null
            }
        }
    }

    companion object {
        private const val TRUE_STRING = "true"
        private const val TITLE = "Title"
        private const val CONTENT = "Content"
        private const val BIG_TITLE = "Big Title"
        private const val BIG_SUMMARY = "Big Summary"
        private const val SUMMARY_TEXT = "Summary Text"
        private val random = Random()
        const val EXAMPLE_EXTRA_KEY_1 = "Entree"
        const val EXAMPLE_EXTRA_KEY_2 = "Side"
        const val EXAMPLE_EXTRA_KEY_3 = "Drink"

        private data class PushStoryTesterPage(
            val deeplink: String,
            val title: String,
            val subtitle: String,
            val imageUrl: String,
        )

        private val PUSH_STORY_PAGE_VALUES = mutableListOf<PushStoryTesterPage>()

        init {
            PUSH_STORY_PAGE_VALUES.add(
                PushStoryTesterPage(
                    "https://braze.com",
                    "Twenty WWWWWWW WWWW#",
                    "Twenty WWWWWWW WWWW#",
                    "https://cdn-staging.braze.com/appboy/communication/assets/image_assets/images/60c82de467360e39d4ac9c4b/original.jpeg?1623731684"
                )
            )
            PUSH_STORY_PAGE_VALUES.add(
                PushStoryTesterPage(
                    "http://google.com",
                    "Twenty Five WW WWWW WWWW#",
                    "Twenty Five WW WWWW WWWW#",
                    "https://cdn-staging.braze.com/appboy/communication/assets/image_assets/images/60c82de467360e2ab3ac9cf0/original.jpeg?1623731684"
                )
            )

            PUSH_STORY_PAGE_VALUES.add(
                PushStoryTesterPage(
                    "https://braze.com",
                    "Thirty WW WWWW WWWW WWWW WWWW#",
                    "Thirty WW WWWW WWWW WWWW WWWW#",
                    "https://cdn-staging.braze.com/appboy/communication/assets/image_assets/images/60c82de4ad561022b6418bd8/original.jpeg?1623731684"
                )
            )
            PUSH_STORY_PAGE_VALUES.add(
                PushStoryTesterPage(
                    "https://braze.com",
                    "Forty WWW WWWW WWWW WWWW WWWW WWWW WWWW#",
                    "Forty WWW WWWW WWWW WWWW WWWW WWWW WWWW#",
                    "https://cdn-staging.braze.com/appboy/communication/assets/image_assets/images/60c82de567360e3aa4ac9bed/original.jpeg?1623731685"
                )
            )
            PUSH_STORY_PAGE_VALUES.add(
                PushStoryTesterPage(
                    "https://braze.com",
                    "Forty Five WWW WWWW WWWW WWWW WWWW WWWW WWWW#",
                    "Forty Five WWW WWWW WWWW WWWW WWWW WWWW WWWW#",
                    "https://cdn-staging.braze.com/appboy/communication/assets/image_assets/images/60c82de467360e7d35ac9e5f/original.jpeg?1623731684"
                )
            )
            PUSH_STORY_PAGE_VALUES.add(
                PushStoryTesterPage(
                    "https://braze.com",
                    "Fifteen W WWW#",
                    "Fifteen W WWW#",
                    "https://cdn-staging.braze.com/appboy/communication/assets/image_assets/images/60c82de567360e2ab3ac9cf1/original.jpeg?1623731685"
                )
            )
            PUSH_STORY_PAGE_VALUES.add(
                PushStoryTesterPage(
                    "https://braze.com",
                    "Ten  WWWW#",
                    "Ten  WWWW#",
                    "https://cdn-staging.braze.com/appboy/communication/assets/image_assets/images/60c82de53a531a3ff3e7ef46/original.jpeg?1623731685"
                )
            )
            PUSH_STORY_PAGE_VALUES.add(
                PushStoryTesterPage(
                    "https://braze.com",
                    "Five#",
                    "Five#",
                    "https://cdn-staging.braze.com/appboy/communication/assets/image_assets/images/60c82de5ad5610327e418ac2/original.jpeg?1623731684"
                )
            )
        }

        private fun View.onCheckboxChecked(onCheckedListener: (isChecked: Boolean) -> Unit) {
            (this as CheckBox).setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                onCheckedListener(isChecked)
            }
        }
    }
}
