package com.appboy.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.appboy.sample.dialog.CustomDialogBase
import com.appboy.sample.util.DroidboyDataStoreUtils.readAllPrefsApiKeys
import com.appboy.sample.util.DroidboyDataStoreUtils.readPrefsString
import com.appboy.sample.util.DroidboyDataStoreUtils.removePrefsKey
import com.appboy.sample.util.DroidboyDataStoreUtils.writePrefsApiKey
import com.appboy.sample.util.DroidboyDataStoreUtils.writePrefsString
import com.appboy.sample.util.DroidboyPreferenceKeys
import com.appboy.sample.util.LifecycleUtils

class SetEnvironmentPreference : CustomDialogBase() {
    private lateinit var apiKeyAliasTextView: TextView
    private lateinit var apiKeyTextView: TextView
    private lateinit var endpointTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.set_environment_preference, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        val overrideApiKeyAlias = context.readPrefsString(DroidboyPreferenceKeys.OVERRIDE_API_KEY_ALIAS)
        val overrideApiKey = context.readPrefsString(DroidboyPreferenceKeys.OVERRIDE_API_KEY)
        val overrideEndpointUrl = context.readPrefsString(DroidboyPreferenceKeys.OVERRIDE_ENDPOINT)

        apiKeyAliasTextView = view.findViewById(R.id.set_environment_override_api_key_alias)
        apiKeyTextView = view.findViewById(R.id.set_environment_override_api_key)
        endpointTextView = view.findViewById(R.id.set_environment_override_endpoint_url)

        if (overrideApiKeyAlias != null) {
            apiKeyAliasTextView.text = overrideApiKeyAlias
        }
        if (overrideApiKey != null) {
            apiKeyTextView.text = overrideApiKey
        }
        if (overrideEndpointUrl != null) {
            endpointTextView.text = overrideEndpointUrl
        }

        val storedApiKeyLinearLayout = view.findViewById<LinearLayout>(R.id.stored_api_key_layout)
        val apiKeys = context.readAllPrefsApiKeys()

        // Populate default API key
        if (!apiKeys.containsKey("Default")) {
            val brazeApiKey = DroidboyApplication.getApiKeyInUse(context)
            storedApiKeyLinearLayout.addView(getApiKeyButton("Default", brazeApiKey))
        }

        // Populate previously stored API keys
        for ((alias, apiKey) in apiKeys) {
            storedApiKeyLinearLayout.addView(getApiKeyButton(alias, apiKey))
        }
    }

    private fun getApiKeyButton(alias: String, apiKey: String?): Button {
        return Button(requireContext()).apply {
            setOnClickListener {
                apiKeyAliasTextView.text = alias
                apiKeyTextView.text = apiKey
            }
            text = "$alias: $apiKey"
        }
    }

    override fun onExitButtonPressed(clickedPositiveButton: Boolean) {
        if (clickedPositiveButton) {
            val context = requireContext()
            val apiKeyAlias = apiKeyAliasTextView.text.toString()
            val apiKey = apiKeyTextView.text.toString()
            val endpoint = endpointTextView.text.toString()

            if (apiKeyAlias.isNotEmpty()) {
                context.writePrefsString(DroidboyPreferenceKeys.OVERRIDE_API_KEY_ALIAS, apiKeyAlias)
            } else {
                context.removePrefsKey(DroidboyPreferenceKeys.OVERRIDE_API_KEY_ALIAS)
            }

            if (apiKey.isNotEmpty()) {
                context.writePrefsString(DroidboyPreferenceKeys.OVERRIDE_API_KEY, apiKey)
            } else {
                context.removePrefsKey(DroidboyPreferenceKeys.OVERRIDE_API_KEY)
            }

            if (apiKeyAlias.isNotEmpty() && apiKey.isNotEmpty()) {
                context.writePrefsApiKey(apiKeyAlias, apiKey)
            }

            if (endpoint.isNotEmpty()) {
                context.writePrefsString(DroidboyPreferenceKeys.OVERRIDE_ENDPOINT, endpoint)
            } else {
                context.removePrefsKey(DroidboyPreferenceKeys.OVERRIDE_ENDPOINT)
            }

            LifecycleUtils.restartApp(context)
        }
    }
}
