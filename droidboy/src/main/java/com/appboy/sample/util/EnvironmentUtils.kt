package com.appboy.sample.util

import android.R
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import com.appboy.sample.util.DroidboyDataStoreUtils.writePrefsString
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.Priority.I
import com.braze.support.BrazeLogger.brazelog
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class EnvironmentUtils private constructor() {
    companion object {
        public const val BRAZE_ENVIRONMENT_DEEPLINK_SCHEME = "braze"
        public const val BRAZE_ENVIRONMENT_DEEPLINK_HOST = "environment"
        private const val BRAZE_ENVIRONMENT_DEEPLINK_SCHEME_HOST =
            "${BRAZE_ENVIRONMENT_DEEPLINK_SCHEME}://${BRAZE_ENVIRONMENT_DEEPLINK_HOST}"
        private const val BRAZE_ENVIRONMENT_DEEPLINK_ENDPOINT = "endpoint"
        private const val BRAZE_ENVIRONMENT_DEEPLINK_API_KEY = "api_key"

        @JvmStatic
        fun analyzeBitmapForEnvironmentBarcode(
            activity: Activity,
            bitmap: Bitmap,
        ) {
            // Build the barcode detector
            val options =
                BarcodeScannerOptions
                    .Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            val image = InputImage.fromBitmap(bitmap, 0)
            val scanner = BarcodeScanning.getClient(options)
            scanner
                .process(image)
                .addOnSuccessListener { barcodes: List<Barcode> ->
                    if (barcodes.isEmpty()) {
                        showToast(activity, "Couldn't find barcode. Please try again!")
                    } else {
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            if (rawValue?.startsWith(BRAZE_ENVIRONMENT_DEEPLINK_SCHEME_HOST) == true) {
                                showToast(activity, "Found barcode: $rawValue")
                                setEnvironmentViaDeepLink(activity, rawValue.toUri())
                            }
                        }
                    }
                }.addOnFailureListener { e: Exception -> brazelog(E, e) { "Failed to parse barcode bitmap" } }
                .addOnCompleteListener { bitmap.recycle() }
        }

        /**
         * Parses a `braze://environment?endpoint=ENDPOINT_HERE&api_key=API_KEY_HERE` URI and
         * shows a confirmation dialog that applies the new environment on acceptance.
         */
        @JvmStatic
        internal fun setEnvironmentViaDeepLink(
            context: Activity,
            uri: Uri,
        ) {
            val endpoint = uri.getQueryParameter(BRAZE_ENVIRONMENT_DEEPLINK_ENDPOINT)
            val apiKey = uri.getQueryParameter(BRAZE_ENVIRONMENT_DEEPLINK_API_KEY)

            brazelog(I) { "Using environment endpoint: $endpoint" }
            brazelog(I) { "Using environment api key: $apiKey" }
            val message =
                StringBuilder()
                    .append("Looks correct? 👌")
                    .append("\n\n")
                    .append("New environment endpoint: ")
                    .append("\n")
                    .append(endpoint)
                    .append("\n\n")
                    .append("New environment api key: ")
                    .append("\n")
                    .append(apiKey)
            AlertDialog
                .Builder(context)
                .setTitle("Changing Droidboy environment")
                .setMessage(message.toString())
                .setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                    apiKey?.let { context.writePrefsString(DroidboyPreferenceKeys.OVERRIDE_API_KEY, it) }
                    endpoint?.let { context.writePrefsString(DroidboyPreferenceKeys.OVERRIDE_ENDPOINT, it) }
                    LifecycleUtils.restartApp(context)
                } // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(R.string.cancel, null)
                .setIcon(R.drawable.ic_dialog_info)
                .show()
        }

        private fun showToast(
            context: Context,
            message: String,
        ) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
