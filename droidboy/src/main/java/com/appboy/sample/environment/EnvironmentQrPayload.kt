package com.appboy.sample.environment

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Builds and encodes the `braze://environment` deep link consumed by
 * `com.appboy.sample.util.EnvironmentUtils.setEnvironmentViaDeepLink`.
 *
 * The payload format matches the constants in `EnvironmentUtils`:
 *   `braze://environment?endpoint=<endpoint>&api_key=<api_key>`.
 */
object EnvironmentQrPayload {
    const val DEFAULT_SIZE_PX = 512

    private const val SCHEME_PATH = "braze://environment"
    private const val ENDPOINT_PARAM = "endpoint"
    private const val API_KEY_PARAM = "api_key"

    /**
     * Builds the deep link string for [endpoint] and [apiKey]. Both parameters are URL-encoded.
     *
     * @return the deep link, or null when both [endpoint] and [apiKey] are blank.
     */
    fun buildDeepLink(
        endpoint: String?,
        apiKey: String?,
    ): String? {
        val hasEndpoint = !endpoint.isNullOrBlank()
        val hasApiKey = !apiKey.isNullOrBlank()
        if (!hasEndpoint && !hasApiKey) return null
        val parts = mutableListOf<String>()
        if (hasEndpoint) parts += "$ENDPOINT_PARAM=${Uri.encode(endpoint)}"
        if (hasApiKey) parts += "$API_KEY_PARAM=${Uri.encode(apiKey)}"
        return "$SCHEME_PATH?${parts.joinToString("&")}"
    }

    /**
     * Encodes [payload] as a square QR code bitmap of [size] pixels per side using ZXing.
     *
     * @throws com.google.zxing.WriterException when the payload cannot be encoded.
     */
    fun encodeQrBitmap(
        payload: String,
        size: Int = DEFAULT_SIZE_PX,
    ): Bitmap {
        val bitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
