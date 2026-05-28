package com.appboy.sample.ecommerce

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.appboy.sample.R
import com.braze.Braze
import com.braze.support.BrazeLogger.brazelog
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Test harness fragment for exercising the eCommerce event logging API.
 *
 * Select an event type to edit its properties, then send via [Braze.logEcommerceEvent]
 * or [Braze.logCustomEvent] for [EcommerceEventType.CUSTOM].
 */
class EcommerceFragment : Fragment() {
    private lateinit var eventTypeSpinner: Spinner
    private lateinit var propertyFieldsContainer: LinearLayout
    private lateinit var sendEventButton: Button
    private var selectedEventType: EcommerceEventType? = null
    private var shouldSuppressEventTypeSelection = false
    private val propertyInputs = linkedMapOf<String, TextInputEditText>()
    private val propertyDropdowns = linkedMapOf<String, MaterialAutoCompleteTextView>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root = inflater.inflate(R.layout.ecommerce_tester_fragment, container, false)
        eventTypeSpinner = root.findViewById(R.id.event_type_spinner)
        propertyFieldsContainer = root.findViewById(R.id.property_fields_container)
        sendEventButton = root.findViewById(R.id.send_event_button)

        setupEventTypeSpinner()
        sendEventButton.setOnClickListener { dispatchSelectedEvent() }

        return root
    }

    private fun setupEventTypeSpinner() {
        val eventTypes = EcommerceEventType.entries
        val labels = eventTypes.map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        eventTypeSpinner.adapter = adapter
        eventTypeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (shouldSuppressEventTypeSelection) {
                        return
                    }
                    selectEventType(eventTypes[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        shouldSuppressEventTypeSelection = true
        eventTypeSpinner.setSelection(0)
        shouldSuppressEventTypeSelection = false
        selectEventType(eventTypes.first())
    }

    private fun selectEventType(eventType: EcommerceEventType) {
        selectedEventType = eventType
        propertyFieldsContainer.removeAllViews()
        propertyInputs.clear()
        propertyDropdowns.clear()

        val inflater = LayoutInflater.from(requireContext())
        if (eventType.isCustom) {
            addSamplePicker(inflater)
        }
        for (field in eventType.propertyFields) {
            if (field.dropdownOptions != null) {
                addDropdownField(inflater, field)
            } else {
                addTextField(inflater, field)
            }
        }
    }

    private fun addTextField(
        inflater: LayoutInflater,
        field: EcommercePropertyField,
    ) {
        val isMultiline = field.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0
        val layoutId =
            if (isMultiline) {
                R.layout.ecommerce_property_multiline_field
            } else {
                R.layout.ecommerce_property_field
            }
        val fieldView = inflater.inflate(layoutId, propertyFieldsContainer, false)
        val inputLayout = fieldView as TextInputLayout
        inputLayout.hint = field.label
        val editText = fieldView.findViewById<TextInputEditText>(R.id.ecommerce_property_input)
        editText.setText(field.defaultValue)
        if (!isMultiline) {
            editText.inputType = field.inputType
        }
        propertyInputs[field.key] = editText
        propertyFieldsContainer.addView(fieldView)
    }

    private fun addSamplePicker(inflater: LayoutInflater) {
        val sampleEventTypes = EcommerceEventJsonSamples.sampleEventTypes
        val labels = sampleEventTypes.map { it.displayName }.toTypedArray()
        val fieldView = inflater.inflate(R.layout.ecommerce_property_dropdown, propertyFieldsContainer, false)
        val inputLayout = fieldView as TextInputLayout
        inputLayout.hint = getString(R.string.ecommerce_sample_hint)
        val dropdown = fieldView.findViewById<MaterialAutoCompleteTextView>(R.id.ecommerce_property_dropdown)
        dropdown.setSimpleItems(labels)
        dropdown.setOnItemClickListener { _, _, position, _ ->
            applySample(sampleEventTypes[position])
        }
        propertyFieldsContainer.addView(fieldView)
    }

    private fun applySample(eventType: EcommerceEventType) {
        val sample = EcommerceEventJsonSamples.sampleFor(eventType) ?: return
        propertyInputs[EcommerceEventType.KEY_EVENT_NAME]?.setText(sample.eventName)
        propertyInputs[EcommerceEventType.KEY_PROPERTIES_JSON]?.setText(sample.propertiesJson)
    }

    private fun addDropdownField(
        inflater: LayoutInflater,
        field: EcommercePropertyField,
    ) {
        val options = field.dropdownOptions ?: return
        val fieldView = inflater.inflate(R.layout.ecommerce_property_dropdown, propertyFieldsContainer, false)
        val inputLayout = fieldView as TextInputLayout
        inputLayout.hint = field.label
        val dropdown = fieldView.findViewById<MaterialAutoCompleteTextView>(R.id.ecommerce_property_dropdown)
        dropdown.setSimpleItems(options.toTypedArray())
        dropdown.setText(field.defaultValue, false)
        propertyDropdowns[field.key] = dropdown
        propertyFieldsContainer.addView(fieldView)
    }

    private fun collectInputs(): Map<String, String> {
        val inputs = linkedMapOf<String, String>()
        propertyInputs.forEach { (key, editText) ->
            inputs[key] = editText.text?.toString().orEmpty()
        }
        propertyDropdowns.forEach { (key, dropdown) ->
            inputs[key] = dropdown.text?.toString().orEmpty()
        }
        return inputs
    }

    private fun dispatchSelectedEvent() {
        val eventType = selectedEventType
        if (eventType == null) {
            showToast(getString(R.string.ecommerce_select_event_first))
            return
        }

        try {
            if (eventType.isCustom) {
                dispatchCustomEvent(collectInputs())
            } else {
                val event = eventType.buildEvent(collectInputs())
                Braze.getInstance(requireContext()).logEcommerceEvent(event)
                reportDispatched(event.eventName)
            }
        } catch (e: IllegalArgumentException) {
            brazelog { "eCommerce event validation failed: ${e.message}" }
            showToast(getString(R.string.ecommerce_event_invalid, e.message ?: "Invalid input"))
        }
    }

    private fun dispatchCustomEvent(inputs: Map<String, String>) {
        val eventName = inputs[EcommerceEventType.KEY_EVENT_NAME]?.trim().orEmpty()
        require(eventName.isNotEmpty()) { "Event name is required." }
        val propertiesJson = inputs[EcommerceEventType.KEY_PROPERTIES_JSON].orEmpty()
        val properties = CustomEventJsonParser.parseProperties(propertiesJson)
        brazelog { "Logging custom event: $eventName properties=${properties.forJsonPut()}" }
        Braze.getInstance(requireContext()).logCustomEvent(eventName, properties)
        reportDispatched(eventName)
    }

    private fun reportDispatched(eventName: String) {
        brazelog { "Dispatched $eventName" }
        showToast(getString(R.string.ecommerce_event_dispatched, eventName))
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
