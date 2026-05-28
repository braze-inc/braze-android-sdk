package com.appboy.sample.environment

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.appboy.sample.DroidboyApplication
import com.appboy.sample.R
import com.appboy.sample.util.DroidboyDataStoreUtils.readPrefsString
import com.appboy.sample.util.DroidboyPreferenceKeys
import com.braze.configuration.BrazeConfigurationProvider
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.brazelog

/**
 * Dialog that renders the currently configured Braze environment (endpoint + API key) as a
 * scannable QR code. Scanning the QR with the existing
 * `Settings → Set Environment via barcode` flow on another device mirrors this environment.
 */
class EnvironmentQrDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.environment_qr_dialog, null)
        val configurationProvider = BrazeConfigurationProvider(context)

        val endpoint =
            context
                .readPrefsString(DroidboyPreferenceKeys.OVERRIDE_ENDPOINT)
                ?.takeIf { it.isNotBlank() }
                ?: configurationProvider.customEndpoint?.takeIf { it.isNotBlank() }
                ?: configurationProvider.baseUrlForRequests
        val apiKey =
            context
                .readPrefsString(DroidboyPreferenceKeys.OVERRIDE_API_KEY)
                ?.takeIf { it.isNotBlank() }
                ?: DroidboyApplication.getApiKeyInUse(context)

        val payload = EnvironmentQrPayload.buildDeepLink(endpoint, apiKey)

        view.findViewById<TextView>(R.id.environment_qr_endpoint).text = endpoint
        view.findViewById<TextView>(R.id.environment_qr_api_key).text = apiKey ?: "(unset)"
        view.findViewById<TextView>(R.id.environment_qr_payload).text = payload ?: "(no payload)"

        if (payload != null) {
            try {
                val bitmap = EnvironmentQrPayload.encodeQrBitmap(payload)
                view.findViewById<ImageView>(R.id.environment_qr_image).setImageBitmap(bitmap)
            } catch (e: Exception) {
                brazelog(E, e) { "EnvironmentQrDialog failed to encode QR bitmap" }
                Toast.makeText(context, "Could not generate QR: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        val builder =
            AlertDialog
                .Builder(context)
                .setTitle("Current Braze environment")
                .setView(view)
                .setPositiveButton("Close", null)
        if (payload != null) {
            builder.setNeutralButton("Share link") { _, _ -> shareDeepLink(payload) }
        }
        return builder.create()
    }

    private fun shareDeepLink(payload: String) {
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, payload)
            }
        startActivity(Intent.createChooser(sendIntent, "Share Braze environment link"))
    }

    companion object {
        const val TAG = "EnvironmentQrDialog"

        fun newInstance(): EnvironmentQrDialog = EnvironmentQrDialog()
    }
}
