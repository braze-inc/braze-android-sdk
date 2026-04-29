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
    private lateinit var bannerSlotSpinner: Spinner
    private lateinit var currentBannerPlacementDisplay: TextView
    private lateinit var bannersMultiContainer: LinearLayout
    private lateinit var bannerPropertiesContainer: LinearLayout
    private lateinit var propertyFilterSpinner: Spinner
    private lateinit var cachedPlacementIdsChipGroup: ChipGroup

    private var bannerUpdateSubscriber: IEventSubscriber<BannersUpdatedEvent>? = null
    private var currentlyDisplayedBanner: Banner? = null
    private var currentFilterType: String? = null

    /**
     * Ordered list of currently rendered placement IDs, one per slot.
     */
    private val slotPlacementIds = mutableListOf<String>()
    private val slotBannerViews = mutableListOf<BannerView>()

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
        getBannerTextInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val trimmed = getBannerTextInput.text.toString().trim()
                if (trimmed != getBannerTextInput.text.toString()) {
                    getBannerTextInput.setText(trimmed)
                }
            }
        }
        getBannerButton = view.findViewById(R.id.get_banner_button)
        bannerSlotSpinner = view.findViewById(R.id.banner_slot_spinner)
        currentBannerPlacementDisplay = view.findViewById(R.id.current_banner_placement_display)
        bannersMultiContainer = view.findViewById(R.id.banners_multi_container)
        bannerPropertiesContainer = view.findViewById(R.id.banner_properties_container)
        propertyFilterSpinner = view.findViewById(R.id.property_filter_spinner)
        cachedPlacementIdsChipGroup = view.findViewById(R.id.cached_placement_ids_chip_group)

        renderSlotsForPlacements(DroidboyApplication.BANNER_PLACEMENT_IDS)
        setupPropertyFilters()

        // Listener for "Request Banners Refresh" button (by list of placement IDs)
        refreshBannersButton.setOnClickListener {
            val placementIdsString = refreshBannersTextInput.text.toString().trim()
            val inputPlacementIds = if (placementIdsString.isNotBlank()) {
                placementIdsString.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            } else {
                emptyList()
            }
            val combinedPlacementIds = combinePlacementIds(inputPlacementIds)
            if (combinedPlacementIds.isEmpty()) {
                showToast("No banner placement IDs entered. Enter at least one banner placement ID.")
                return@setOnClickListener
            }
            Braze.getInstance(requireContext()).requestBannersRefresh(combinedPlacementIds)
            renderSlotsForPlacements(combinedPlacementIds)
            showToast("Requested banner refresh for placements: ${combinedPlacementIds.joinToString()}")
            clearDisplayedBannerProperties()
        }

        // Listener for "Display This Banner" — placement from text field, target slot from spinner
        getBannerButton.setOnClickListener {
            val bannerId = getBannerTextInput.text.toString().trim()
            if (bannerId.isBlank()) {
                showToast("No banner placement ID entered. Enter a placement ID to display.")
                return@setOnClickListener
            }
            val slotIndex = bannerSlotSpinner.selectedItemPosition
            if (slotIndex !in slotBannerViews.indices) {
                showToast("Select a display slot.")
                return@setOnClickListener
            }
            val banner = Braze.getInstance(requireContext()).getBanner(bannerId)
            val targetView = slotBannerViews[slotIndex]
            if (banner != null) {
                targetView.placementId = banner.placementId
                slotPlacementIds[slotIndex] = banner.placementId
                refreshSlotLabels()
                refreshSlotSpinner()
                getBannerTextInput.setText(banner.placementId)
                currentlyDisplayedBanner = banner
                displayBannerProperties(banner)
                currentBannerPlacementDisplay.text =
                    "Banner properties: slot ${slotIndex + 1}, placement ${banner.placementId}"
                showToast("Displayed in slot ${slotIndex + 1}: $bannerId")
                brazelog { "BannersFragment: Got banner - $banner" }
            } else {
                targetView.placementId = null
                slotPlacementIds[slotIndex] = ""
                refreshSlotLabels()
                refreshSlotSpinner()
                currentlyDisplayedBanner = null
                displayBannerProperties(null)
                addPropertyTextView(null)
                currentBannerPlacementDisplay.text =
                    "Banner properties: slot ${slotIndex + 1}, no data for placement"
                showToast("No banner found for placement: $bannerId.")
            }
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        subscribeToBannerUpdates()
        refreshSlotBannerViews()
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
                    val combined = combinePlacementIds(placementIds)
                    if (combined.isNotEmpty()) {
                        renderSlotsForPlacements(combined)
                    }
                    refreshSlotBannerViews()
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
     * Computes the union of currently rendered placements, the supplied list, and the Droidboy
     * default list, preserving insertion order so existing slots remain stable.
     */
    private fun combinePlacementIds(extra: List<String>): List<String> {
        val combined = LinkedHashSet<String>()
        combined.addAll(slotPlacementIds.filter { it.isNotBlank() })
        combined.addAll(DroidboyApplication.BANNER_PLACEMENT_IDS)
        combined.addAll(extra.filter { it.isNotBlank() })
        return combined.toList()
    }

    /**
     * Builds one [BannerView] per supplied placement ID inside [bannersMultiContainer].
     * Reuses existing slot views when the placement set is unchanged in order, only rebuilding
     * when the slot list changes.
     */
    private fun renderSlotsForPlacements(placementIds: List<String>) {
        if (placementIds == slotPlacementIds && slotBannerViews.size == placementIds.size) {
            refreshSlotLabels()
            // slotPlacementIds may have been mutated in place (e.g. by "Display This Banner"),
            // so the spinner needs to be rebuilt even though the slot count is unchanged.
            refreshSlotSpinner()
            return
        }
        bannersMultiContainer.removeAllViews()
        slotBannerViews.clear()
        slotPlacementIds.clear()
        slotPlacementIds.addAll(placementIds)

        val labelTopMarginPx = (8 * resources.displayMetrics.density).toInt()
        val bottomMarginPx = (16 * resources.displayMetrics.density).toInt()
        val minHeightPx = (100 * resources.displayMetrics.density).toInt()

        placementIds.forEachIndexed { index, placementId ->
            val slotLabel = TextView(requireContext()).apply {
                text = formatSlotLabel(index, placementId)
                setPadding(0, if (index == 0) 0 else labelTopMarginPx, 0, 0)
            }
            bannersMultiContainer.addView(slotLabel)

            val bannerView = BannerView(requireContext(), null).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = bottomMarginPx }
                setBackgroundResource(android.R.color.darker_gray)
                minimumHeight = minHeightPx
                this.placementId = placementId
            }
            bannersMultiContainer.addView(bannerView)
            slotBannerViews.add(bannerView)
        }
        refreshSlotSpinner()
    }

    private fun refreshSlotLabels() {
        if (bannersMultiContainer.childCount != slotPlacementIds.size * 2) return
        slotPlacementIds.forEachIndexed { index, placementId ->
            val label = bannersMultiContainer.getChildAt(index * 2) as? TextView ?: return@forEachIndexed
            label.text = formatSlotLabel(index, placementId)
        }
    }

    private fun formatSlotLabel(index: Int, placementId: String): String {
        val displayedId = if (placementId.isBlank()) "(empty)" else placementId
        return "Slot ${index + 1} — $displayedId"
    }

    private fun refreshSlotSpinner() {
        val labels = slotPlacementIds.mapIndexed { index, placementId ->
            "Slot ${index + 1}${if (placementId.isNotBlank()) " ($placementId)" else ""}"
        }.toTypedArray()
        val previousSelection = bannerSlotSpinner.selectedItemPosition
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bannerSlotSpinner.adapter = adapter
        if (labels.isNotEmpty()) {
            bannerSlotSpinner.setSelection(previousSelection.coerceIn(0, labels.size - 1))
        }
    }

    private fun refreshSlotBannerViews() {
        slotBannerViews.forEach { bannerView ->
            bannerView.initBanner(bannerView.placementId)
        }
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
                    getBannerTextInput.setText(placementId.trim())
                    showToast("Selected placement ID: $placementId")
                }
            }
            cachedPlacementIdsChipGroup.addView(chip)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Clears the displayed banner-properties list and resets the current-placement label.
     */
    private fun clearDisplayedBannerProperties() {
        currentBannerPlacementDisplay.text = getString(R.string.banners_properties_none_placed_yet)
        currentlyDisplayedBanner = null
        displayBannerProperties(null)
    }
}
