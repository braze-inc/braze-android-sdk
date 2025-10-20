package com.appboy.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.braze.Braze
import com.braze.events.BannersUpdatedEvent
import com.braze.events.IEventSubscriber
import com.braze.models.Banner
import com.braze.models.IPropertiesObject
import com.braze.support.BrazeLogger.Priority.I
import com.braze.support.BrazeLogger.brazelog
import com.braze.ui.banners.BannerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class BannersFragment : Fragment() {

    private lateinit var refreshBannersTextInput: EditText
    private lateinit var refreshBannersButton: Button
    private lateinit var getBannerTextInput: EditText
    private lateinit var getBannerButton: Button
    private lateinit var currentBannerPlacementDisplay: TextView
    private lateinit var bannersFragmentBannerView: BannerView
    private lateinit var bannerPropertiesContainer: LinearLayout
    private lateinit var propertyFilterSpinner: Spinner
    private lateinit var cachedPlacementIdsChipGroup: ChipGroup

    private var bannerUpdateSubscriber: IEventSubscriber<BannersUpdatedEvent>? = null
    private var currentlyDisplayedBanner: Banner? = null
    private var currentFilterType: String? = null

    // Define property types and their display names for filters, null means no filter and show all
    private val propertyFilterTypes = mapOf(
        "All" to null,
        "String" to IPropertiesObject.PROPERTIES_TYPE_STRING,
        "Number" to IPropertiesObject.PROPERTIES_TYPE_NUMBER,
        "Boolean" to IPropertiesObject.PROPERTIES_TYPE_BOOLEAN,
        "Image" to IPropertiesObject.PROPERTIES_TYPE_IMAGE,
        "JSON" to IPropertiesObject.PROPERTIES_TYPE_JSON,
        "Timestamp" to IPropertiesObject.PROPERTIES_TYPE_DATETIME
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.banners_fragment, container, false)

        refreshBannersTextInput = view.findViewById(R.id.refresh_banners_input)
        refreshBannersButton = view.findViewById(R.id.refresh_banners_button)
        getBannerTextInput = view.findViewById(R.id.get_banner_input)
        getBannerButton = view.findViewById(R.id.get_banner_button)
        currentBannerPlacementDisplay = view.findViewById(R.id.current_banner_placement_display)
        bannersFragmentBannerView = view.findViewById(R.id.banners_fragment_banner_view)
        bannerPropertiesContainer = view.findViewById(R.id.banner_properties_container)
        propertyFilterSpinner = view.findViewById(R.id.property_filter_spinner)
        cachedPlacementIdsChipGroup = view.findViewById(R.id.cached_placement_ids_chip_group)

        setupPropertyFilters()

        // Listener for "Request Banners Refresh" button (by list of placement IDs)
        refreshBannersButton.setOnClickListener {
            val placementIdsString = refreshBannersTextInput.text.toString().trim()
            if (placementIdsString.isNotBlank()) {
                val placementIdsToRefresh = if (placementIdsString.isNotBlank()) {
                    placementIdsString.split(",").map { idString -> idString.trim() }.filter { trimmedId -> trimmedId.isNotBlank() }.toMutableList()
                } else {
                    mutableListOf()
                }
                // Get the distinct IDs between the default and input banner placement IDs, then refresh
                val allPlacementIds = DroidboyApplication.BANNER_PLACEMENT_IDS.toMutableList()
                allPlacementIds.addAll(placementIdsToRefresh)
                val uniquePlacementIds = allPlacementIds.distinct()
                if (uniquePlacementIds.isNotEmpty()) {
                    Braze.getInstance(requireContext()).requestBannersRefresh(uniquePlacementIds)
                    showToast("Requested banner refresh for placements: ${uniquePlacementIds.joinToString()}")

                    // Clear the currently displayed banner
                    currentBannerPlacementDisplay.text = "Current Banner Displayed: None"
                    bannersFragmentBannerView.placementId = ""
                    bannersFragmentBannerView.initBanner(bannersFragmentBannerView.placementId)
                    currentlyDisplayedBanner = null
                    displayBannerProperties(null)
                }
            } else {
                showToast("No banner placement IDs entered. Enter at least one banner placement ID.")
            }
        }

        // Listener for "Display This Banner" button (by placement ID)
        getBannerButton.setOnClickListener {
            val bannerId = getBannerTextInput.text.toString().trim()
            if (bannerId.isNotBlank()) {
                val banner = Braze.getInstance(requireContext()).getBanner(bannerId)
                if (banner != null) {
                    bannersFragmentBannerView.placementId = banner.placementId
                    bannersFragmentBannerView.initBanner(bannersFragmentBannerView.placementId)
                    currentBannerPlacementDisplay.text = "Current Banner Displayed: ${banner.placementId}"
                    currentlyDisplayedBanner = banner
                    displayBannerProperties(banner)
                    showToast("Displayed banner with ID: $bannerId. See console for details.")
                    brazelog { "BannersFragment: Got banner - $banner" }
                } else {
                    bannersFragmentBannerView.placementId = ""
                    bannersFragmentBannerView.initBanner(null)
                    currentBannerPlacementDisplay.text = "Current Banner Displayed: None"
                    currentlyDisplayedBanner = null
                    displayBannerProperties(null)
                    addPropertyTextView(null)
                    showToast("No banner found for placement: $bannerId.")
                }
            } else {
                showToast("No banner placement ID entered. Enter a placement ID to display and log.")
            }
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        subscribeToBannerUpdates()
        val initialPlacementId = refreshBannersTextInput.text.toString().trim()
        if (initialPlacementId.isNotBlank()) {
            bannersFragmentBannerView.placementId = initialPlacementId
            bannersFragmentBannerView.initBanner(bannersFragmentBannerView.placementId)
        }
    }

    override fun onPause() {
        super.onPause()
        unsubscribeFromBannerUpdates()
    }

    private fun subscribeToBannerUpdates() {
        if (bannerUpdateSubscriber == null) {
            bannerUpdateSubscriber = IEventSubscriber { update ->
                brazelog(I) {
                    "BannersFragment: Received ${update.banners.size} banners with " +
                        "placement IDs: ${update.banners.map { it.placementId }}"
                }

                val placementIds = update.banners.map { it.placementId }
                cachedPlacementIdsChipGroup.post {
                    updateCachedPlacementChips(placementIds)
                }
            }
            bannerUpdateSubscriber?.let {
                Braze.getInstance(requireContext()).subscribeToBannersUpdates(it)
            }
        }
    }

    private fun unsubscribeFromBannerUpdates() {
        bannerUpdateSubscriber?.let {
            Braze.getInstance(requireContext()).removeSingleSubscription(it, BannersUpdatedEvent::class.java)
        }
        bannerUpdateSubscriber = null
    }

    /**
     * Sets up the filter dropdown for property types.
     */
    private fun setupPropertyFilters() {
        val filterOptions = propertyFilterTypes.keys.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        propertyFilterSpinner.adapter = adapter

        // Set initial selection to "All" (null)
        val allIndex = filterOptions.indexOf("All")
        if (allIndex != -1) {
            propertyFilterSpinner.setSelection(allIndex)
            currentFilterType = null
        }

        // Listener for when property filter dropdown item is selected
        propertyFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedDisplayName = filterOptions[position]
                currentFilterType = propertyFilterTypes[selectedDisplayName]

                // Re-display properties with the new filter
                displayBannerProperties(currentlyDisplayedBanner)
                brazelog { "onItemSelected - $parent" }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                brazelog { "onNothingSelected - $parent" }
            }
        }
    }

    /**
     * Dynamically adds TextViews to display each property of the banner, applying the current filter.
     */
    private fun displayBannerProperties(banner: Banner?) {
        bannerPropertiesContainer.removeAllViews()

        if (banner == null || banner.properties.length() == 0) {
            addPropertyTextView(null)
            return
        }
        val properties = banner.properties
        val keys = properties.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val propertyJson = properties.optJSONObject(key)
            if (propertyJson != null) {
                val type = propertyJson.optString(IPropertiesObject.PROPERTIES_TYPE)

                // Apply filter: only display property if currentFilterType is null (All types) or matches the property's type
                if (currentFilterType == null || type == currentFilterType) {
                    addPropertyTextView(key, retrievePropertyValue(banner, key, type), type)
                }
            } else {
                // If the property is not a well-formed JSONObject with 'type' and 'value',
                // we'll display it if the filter is "All" or if there's no specific type filter.
                if (currentFilterType == null) {
                    val rawValue = properties.opt(key)?.toString() ?: "null"
                    addPropertyTextView(key, rawValue, "Raw (Malformed)")
                }
            }
        }
    }

    private fun retrievePropertyValue(banner: Banner, key: String, type: String): String {
        // Use the Banner object to retrieve
        var value = "Unknown"
        when (type) {
            IPropertiesObject.PROPERTIES_TYPE_STRING -> {
                value = banner.getStringProperty(key).toString()
            }
            IPropertiesObject.PROPERTIES_TYPE_NUMBER -> {
                value = banner.getNumberProperty(key).toString()
            }
            IPropertiesObject.PROPERTIES_TYPE_BOOLEAN -> {
                value = banner.getBooleanProperty(key).toString()
            }
            IPropertiesObject.PROPERTIES_TYPE_IMAGE -> {
                value = banner.getImageProperty(key).toString()
            }
            IPropertiesObject.PROPERTIES_TYPE_JSON -> {
                value = banner.getJSONProperty(key).toString()
            }
            IPropertiesObject.PROPERTIES_TYPE_DATETIME -> {
                value = banner.getTimestampProperty(key).toString()
            }
        }
        return value
    }

    private fun addPropertyTextView(key: String?, value: String? = null, type: String? = null) {
        val textView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            text = if (key == null) {
                ""
                return
            } else {
                "Key: $key\nType: $type\nValue: $value\n"
            }
            setPadding(8, 8, 8, 8)
            setBackgroundResource(R.drawable.property_item_background)
        }
        bannerPropertiesContainer.addView(textView)
    }

    /**
     * Updates the ChipGroup with chips for each cached placement ID.
     * Each chip is clickable and will prefill the "Get Banner" input field.
     */
    private fun updateCachedPlacementChips(placementIds: List<String>) {
        cachedPlacementIdsChipGroup.removeAllViews()

        placementIds.forEach { placementId ->
            val chip = Chip(requireContext()).apply {
                text = placementId
                isClickable = true
                isCheckable = false
                setOnClickListener {
                    // Prefill the get banner input field with this placement ID
                    getBannerTextInput.setText(placementId)
                    showToast("Selected placement ID: $placementId")
                }
            }
            cachedPlacementIdsChipGroup.addView(chip)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val BANNER_ID_KEY = "banner.id"
    }
}
