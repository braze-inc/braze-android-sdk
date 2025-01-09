package com.appboy.sample

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.braze.Braze
import com.braze.BrazeUser
import com.braze.enums.Gender
import com.braze.enums.Month
import com.braze.enums.NotificationSubscriptionType
import com.braze.events.IValueCallback
import com.braze.models.outgoing.AttributionData
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.brazelog
import com.braze.support.convertStringJsonArrayToList
import com.braze.ui.banners.BannerView
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import java.math.BigDecimal
import java.util.Date
import java.util.LinkedList
import java.util.Queue

class MainFragment : Fragment() {
    private lateinit var customEventTextView: AutoCompleteTextView
    private lateinit var customPurchaseTextView: AutoCompleteTextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var aliasEditText: EditText
    private lateinit var aliasLabelEditText: EditText
    private lateinit var customEventsAndPurchasesArrayAdapter: ArrayAdapter<String?>
    private val lastSeenCustomEventsAndPurchases: Queue<String?> = LinkedList()
    private lateinit var userIdEditText: EditText
    private lateinit var bannerIdEditText: EditText
    private lateinit var bannerView: BannerView

    override fun onCreateView(
        layoutInflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val contentView = layoutInflater.inflate(R.layout.main_fragment, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("droidboy", Context.MODE_PRIVATE)
        customEventTextView =
            contentView.findViewById(R.id.com_appboy_sample_custom_event_autocomplete_text_view)
        customPurchaseTextView =
            contentView.findViewById(R.id.com_appboy_sample_purchase_autocomplete_text_view)
        customEventsAndPurchasesArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            getLastSeenCustomEventsAndPurchasesFromLocalStorage()
        )
        customEventTextView.setAdapter(customEventsAndPurchasesArrayAdapter)
        customPurchaseTextView.setAdapter(customEventsAndPurchasesArrayAdapter)

        userIdEditText =
            contentView.findViewById(R.id.com_appboy_sample_set_user_id_edit_text)

        bannerIdEditText = contentView.findViewById(R.id.set_banner_placement_text_box_edit_text)
        bannerView = contentView.findViewById<BannerView>(R.id.main_banner_2)

        contentView.setOnButtonClick(R.id.com_appboy_sample_set_user_id_button) {
            val userId = userIdEditText.text.toString()
            if (userId.isNotBlank()) {
                (requireActivity().applicationContext as DroidboyApplication).changeUserWithNewSdkAuthToken(userId)
                Toast.makeText(requireContext(), "Set userId to: $userId", Toast.LENGTH_SHORT)
                    .show()
                val editor = sharedPreferences.edit()
                editor.putString(USER_ID_KEY, userId)
                editor.apply()
            } else {
                Toast.makeText(requireContext(), "Please enter a userId.", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        contentView.setOnButtonClickWithEditableText(
            R.id.com_braze_sample_set_sdk_auth_signature_button,
            R.id.com_braze_sample_set_sdk_auth_signature_edit_text
        ) { _, signature ->
            if (signature.isNotBlank()) {
                Braze.getInstance(requireContext()).setSdkAuthenticationSignature(signature)
                showToast("Set signature to: $signature")
            } else {
                showToast("Please enter a signature.")
            }
        }

        aliasEditText = contentView.findViewById(R.id.com_appboy_sample_set_alias_edit_text)
        aliasLabelEditText =
            contentView.findViewById(R.id.com_appboy_sample_set_alias_label_edit_text)
        contentView.setOnButtonClick(R.id.com_appboy_sample_set_user_alias_button) { handleAliasClick() }

        contentView.setOnButtonClick(R.id.com_appboy_sample_log_custom_event_button) {
            val customEvent = customEventTextView.text.toString()
            if (customEvent.isNotBlank()) {
                Braze.getInstance(requireContext()).logCustomEvent(customEvent)
                Toast.makeText(
                    requireContext(),
                    String.format("Logged custom event %s.", customEvent),
                    Toast.LENGTH_SHORT
                ).show()
                onCustomEventOrPurchaseLogged(customEvent)
            } else {
                Toast.makeText(requireContext(), "Please enter a custom event.", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        contentView.setOnButtonClick(R.id.com_appboy_sample_log_purchase_button) {
            val purchase = customPurchaseTextView.text.toString()
            if (purchase.isNotBlank()) {
                Braze.getInstance(requireContext()).logPurchase(purchase, "USD", BigDecimal.TEN)
                Toast.makeText(
                    requireContext(),
                    String.format("Logged purchase %s.", purchase),
                    Toast.LENGTH_SHORT
                ).show()
                onCustomEventOrPurchaseLogged(purchase)
            } else {
                Toast.makeText(requireContext(), "Please enter a purchase.", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        contentView.setOnButtonClick(R.id.com_appboy_sample_set_user_attributes_button) {
            Braze.getInstance(requireContext()).getCurrentUser(object : IValueCallback<BrazeUser> {
                override fun onSuccess(value: BrazeUser) {
                    value.setFirstName("first name least")
                    value.setLastName("lastName")
                    value.setEmail("email@test.com")
                    value.setGender(Gender.FEMALE)
                    value.setCountry("USA")
                    value.setLanguage("cs")
                    value.setHomeCity("New York")
                    value.setPhoneNumber("1234567890")
                    value.setLineId("U8189cf6745fc0d808977bdb0b9f22995")
                    value.setDateOfBirth(1984, Month.AUGUST, 18)
                    value.setPushNotificationSubscriptionType(NotificationSubscriptionType.OPTED_IN)
                    value.setEmailNotificationSubscriptionType(NotificationSubscriptionType.OPTED_IN)
                    value.setCustomUserAttribute(STRING_ATTRIBUTE_KEY, "stringValue")
                    value.setCustomUserAttribute(FLOAT_ATTRIBUTE_KEY, 1.5f)
                    value.setCustomUserAttribute(INT_ATTRIBUTE_KEY, 100)
                    value.setCustomUserAttribute(BOOL_ATTRIBUTE_KEY, true)
                    value.setCustomUserAttribute(LONG_ATTRIBUTE_KEY, 10L)
                    value.setCustomUserAttribute(INCREMENT_ATTRIBUTE_KEY, 1)
                    value.setCustomUserAttribute(DOUBLE_ATTRIBUTE_KEY, 3.1)
                    value.incrementCustomUserAttribute(INCREMENT_ATTRIBUTE_KEY, 4)
                    value.setCustomUserAttributeToSecondsFromEpoch(
                        DATE_ATTRIBUTE_KEY,
                        Date().time / 1000L
                    )
                    value.setCustomAttributeArray(
                        STRING_ARRAY_ATTRIBUTE_KEY,
                        arrayOf("a", "b")
                    )
                    value.addToCustomAttributeArray(ARRAY_ATTRIBUTE_KEY, "c")
                    value.removeFromCustomAttributeArray(ARRAY_ATTRIBUTE_KEY, "b")
                    value.addToCustomAttributeArray(PETS_ARRAY_ATTRIBUTE_KEY, "cat")
                    value.addToCustomAttributeArray(PETS_ARRAY_ATTRIBUTE_KEY, "dog")
                    value.removeFromCustomAttributeArray(PETS_ARRAY_ATTRIBUTE_KEY, "bird")
                    value.removeFromCustomAttributeArray(PETS_ARRAY_ATTRIBUTE_KEY, "deer")
                    value.setAttributionData(
                        AttributionData(
                            "network",
                            "campaign",
                            "ad group",
                            "creative"
                        )
                    )
                    value.setLocationCustomAttribute(
                        "Favorite Location",
                        33.078883,
                        -116.603131
                    )
                    showToast("Set user attributes.")
                }

                override fun onError() {
                    showToast("Failed to set user attributes.")
                }
            })
        }

        contentView.setOnButtonClick(R.id.com_appboy_sample_unset_user_attributes_button) {
            Braze.getInstance(requireContext()).getCurrentUser(object : IValueCallback<BrazeUser> {
                override fun onSuccess(value: BrazeUser) {
                    // Unset current user default attributes
                    value.setFirstName(null)
                    value.setLastName(null)
                    value.setEmail(null)
                    value.setGender(Gender.UNKNOWN)
                    value.setCountry(null)
                    value.setLanguage(null)
                    value.setHomeCity(null)
                    value.setPhoneNumber(null)
                    value.setLineId(null)
                    value.setDateOfBirth(1970, Month.JANUARY, 1)
                    value.setPushNotificationSubscriptionType(NotificationSubscriptionType.UNSUBSCRIBED)
                    value.setEmailNotificationSubscriptionType(NotificationSubscriptionType.UNSUBSCRIBED)
                    // Unset current user custom attributes
                    value.unsetCustomUserAttribute(STRING_ATTRIBUTE_KEY)
                    value.unsetCustomUserAttribute(FLOAT_ATTRIBUTE_KEY)
                    value.unsetCustomUserAttribute(INT_ATTRIBUTE_KEY)
                    value.unsetCustomUserAttribute(BOOL_ATTRIBUTE_KEY)
                    value.unsetCustomUserAttribute(LONG_ATTRIBUTE_KEY)
                    value.unsetCustomUserAttribute(DATE_ATTRIBUTE_KEY)
                    value.unsetCustomUserAttribute(ARRAY_ATTRIBUTE_KEY)
                    value.unsetCustomUserAttribute(STRING_ARRAY_ATTRIBUTE_KEY)
                    value.unsetCustomUserAttribute(PETS_ARRAY_ATTRIBUTE_KEY)
                    value.unsetCustomUserAttribute(INCREMENT_ATTRIBUTE_KEY)
                    value.unsetCustomUserAttribute(DOUBLE_ATTRIBUTE_KEY)
                    value.unsetCustomUserAttribute(ATTRIBUTION_DATA_KEY)
                    value.unsetLocationCustomAttribute("Mediocre Location")
                    showToast("Unset user attributes.")
                }

                override fun onError() {
                    showToast("Failed to unset user attributes.")
                }
            })
        }

        contentView.setOnButtonClick(R.id.com_appboy_sample_request_flush_button) {
            Braze.getInstance(requireContext()).requestImmediateDataFlush()
            Toast.makeText(requireContext(), "Requested data flush.", Toast.LENGTH_SHORT).show()
        }

        contentView.setOnButtonClick(R.id.com_appboy_sample_collect_and_flush_google_advertising_id_button) {
            this.lifecycleScope.launch(context = Dispatchers.IO) {
                try {
                    val advertisingIdInfo =
                        AdvertisingIdClient.getAdvertisingIdInfo(requireContext())
                    Braze.getInstance(requireContext()).setGoogleAdvertisingId(
                        advertisingIdInfo.id,
                        advertisingIdInfo.isLimitAdTrackingEnabled
                    )
                    Braze.getInstance(requireContext()).requestImmediateDataFlush()
                } catch (e: Exception) {
                    brazelog(E, e) { "Failed to collect Google Advertising ID information." }
                }
            }
        }

        contentView.setOnButtonClickWithEditableText(
            buttonId = R.id.set_banner_placement_text_box_button,
            editTextId = R.id.set_banner_placement_text_box_edit_text
        ) { _, newPlacementId ->
            if (newPlacementId.isNotBlank()) {
                Toast.makeText(requireContext(), "Set custom banner to: $newPlacementId", Toast.LENGTH_SHORT)
                    .show()
                sharedPreferences.edit().putString(BANNER_ID_KEY, newPlacementId).apply()

                val list = DroidboyApplication.BANNER_PLACEMENT_IDS.toMutableList()
                list.add(newPlacementId)
                Braze.getInstance(requireContext()).requestBannersRefresh(list)
            } else {
                Toast.makeText(requireContext(), "No custom banner set.", Toast.LENGTH_SHORT)
                    .show()
                sharedPreferences.edit().remove(BANNER_ID_KEY).apply()
            }

            bannerView.placementId = newPlacementId

            // Then update the singleton of the placement ids
            Braze.getInstance(requireContext()).requestBannersRefresh(DroidboyApplication.BANNER_PLACEMENT_IDS + newPlacementId)
        }
        return contentView
    }

    private fun getLastSeenCustomEventsAndPurchasesFromLocalStorage(): Array<String?> {
        val serializedEvents =
            sharedPreferences.getString(LAST_SEEN_CUSTOM_EVENTS_AND_PURCHASES_PREFERENCE_KEY, null)
        try {
            if (serializedEvents != null) {
                lastSeenCustomEventsAndPurchases.addAll(
                    JSONArray(serializedEvents).convertStringJsonArrayToList()
                )
            }
        } catch (e: JSONException) {
            brazelog(E, e) { "Failed to get recent events from storage" }
        }
        return lastSeenCustomEventsAndPurchases.toTypedArray()
    }

    private fun onCustomEventOrPurchaseLogged(eventOrPurchaseName: String) {
        if (lastSeenCustomEventsAndPurchases.contains(eventOrPurchaseName)) {
            return
        }
        lastSeenCustomEventsAndPurchases.add(eventOrPurchaseName)
        if (lastSeenCustomEventsAndPurchases.size > 5) {
            lastSeenCustomEventsAndPurchases.remove()
        }
        val editor = sharedPreferences.edit()
        editor.putString(
            LAST_SEEN_CUSTOM_EVENTS_AND_PURCHASES_PREFERENCE_KEY,
            JSONArray(lastSeenCustomEventsAndPurchases).toString()
        )
        editor.apply()
        customEventsAndPurchasesArrayAdapter.clear()
        customEventsAndPurchasesArrayAdapter.addAll(*lastSeenCustomEventsAndPurchases.toTypedArray())
    }

    private fun handleAliasClick() {
        val alias = aliasEditText.text.toString()
        val label = aliasLabelEditText.text.toString()
        Braze.getInstance(requireContext()).getCurrentUser(object : IValueCallback<BrazeUser> {
            override fun onSuccess(value: BrazeUser) {
                value.addAlias(alias, label)
                Toast.makeText(
                    requireContext(),
                    "Added alias " + alias + " with label "
                        + label,
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onError() {
                Toast.makeText(requireContext(), "Failed to add alias", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Shows a toast on the activity's UI thread
     */
    private fun showToast(msg: String) {
        activity?.runOnUiThread { Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show() }
    }

    override fun onResume() {
        super.onResume()
        userIdEditText.setText(sharedPreferences.getString(USER_ID_KEY, null))

        val bannerId = sharedPreferences.getString(BANNER_ID_KEY, null)
        bannerIdEditText.setText(bannerId)
        if (bannerView.placementId != bannerId) {
            bannerView.placementId = bannerId
        }
    }

    companion object {
        private const val STRING_ARRAY_ATTRIBUTE_KEY = "stringArrayAttribute"
        private const val ARRAY_ATTRIBUTE_KEY = "arrayAttribute"
        private const val DATE_ATTRIBUTE_KEY = "dateAttribute"
        private const val PETS_ARRAY_ATTRIBUTE_KEY = "arrayAttributePets"
        private const val FLOAT_ATTRIBUTE_KEY = "floatAttribute"
        private const val BOOL_ATTRIBUTE_KEY = "boolAttribute"
        private const val INT_ATTRIBUTE_KEY = "intAttribute"
        private const val LONG_ATTRIBUTE_KEY = "longAttribute"
        private const val STRING_ATTRIBUTE_KEY = "stringAttribute"
        private const val DOUBLE_ATTRIBUTE_KEY = "doubleAttribute"
        private const val INCREMENT_ATTRIBUTE_KEY = "incrementAttribute"
        private const val ATTRIBUTION_DATA_KEY = "ab_install_attribution"
        private const val LAST_SEEN_CUSTOM_EVENTS_AND_PURCHASES_PREFERENCE_KEY =
            "last_seen_custom_events_and_purchases"
        const val USER_ID_KEY = "user.id"
        const val BANNER_ID_KEY = "banner.id"

        fun View.setOnButtonClick(id: Int, block: (view: View) -> Unit) {
            val view = this.findViewById<Button>(id)
            view.setOnClickListener { block(view) }
        }

        /**
         * A combo of a button id and EditText id
         */
        fun View.setOnButtonClickWithEditableText(
            buttonId: Int,
            editTextId: Int,
            block: (view: View, textValue: String) -> Unit
        ) {
            val view = this.findViewById<Button>(buttonId)
            view.setOnClickListener {
                val editTextValue = this.findViewById<EditText>(editTextId).text.toString()
                block(view, editTextValue)
            }
        }
    }
}
