package com.appboy.sample.ecommerce

import com.braze.models.outgoing.BrazeProperties
import org.json.JSONException
import org.json.JSONObject

/**
 * Parses JSON text into [BrazeProperties] for [com.braze.Braze.logCustomEvent].
 */
object CustomEventJsonParser {
    /**
     * Parses [propertiesJson] as a JSON object. Returns empty properties when blank.
     *
     * @throws IllegalArgumentException when the text is not a JSON object.
     */
    fun parseProperties(propertiesJson: String): BrazeProperties {
        val trimmed = propertiesJson.trim()
        if (trimmed.isEmpty()) {
            return BrazeProperties()
        }
        require(trimmed.startsWith("{")) { "Properties must be a JSON object." }
        try {
            return BrazeProperties(JSONObject(trimmed))
        } catch (e: JSONException) {
            throw IllegalArgumentException("Invalid JSON: ${e.message}", e)
        }
    }
}
