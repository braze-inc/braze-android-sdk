package com.braze.ui.inappmessage.jsinterface

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.annotation.VisibleForTesting
import com.braze.enums.Gender
import com.braze.enums.Month
import com.braze.enums.Month.Companion.getMonth
import com.braze.enums.NotificationSubscriptionType
import com.braze.enums.NotificationSubscriptionType.Companion.fromValue
import com.braze.Braze
import com.braze.BrazeUser
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.Priority.W
import com.braze.support.BrazeLogger.brazelog
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

@Suppress("TooManyFunctions")
class InAppMessageUserJavascriptInterface(private val context: Context) {
    @JavascriptInterface
    fun setFirstName(firstName: String?) {
        Braze.getInstance(context).runOnUser {
            it.setFirstName(firstName)
        }
    }

    @JavascriptInterface
    fun setLastName(lastName: String?) {
        Braze.getInstance(context).runOnUser {
            it.setLastName(lastName)
        }
    }

    @JavascriptInterface
    fun setEmail(email: String?) {
        Braze.getInstance(context).runOnUser {
            it.setEmail(email)
        }
    }

    @JavascriptInterface
    fun setGender(genderString: String) {
        val gender = parseGender(genderString)
        if (gender == null) {
            brazelog(W) {
                "Failed to parse gender in Braze HTML in-app message " +
                    "javascript interface with gender: $genderString"
            }
        } else {
            Braze.getInstance(context).runOnUser {
                it.setGender(gender)
            }
        }
    }

    @JavascriptInterface
    fun setDateOfBirth(year: String, month: String, day: String) {
        val yearValue = year.toIntOrNull()
        if (yearValue == null) {
            brazelog(W) { "Failed to parse setDateOfBirth year value '$year'" }
            return
        }

        val monthValue = monthFromInt(month)
        if (monthValue == null) {
            brazelog(W) { "Failed to parse setDateOfBirth month for value $month" }
            return
        }

        val dayValue = day.toIntOrNull()
        if (dayValue == null) {
            brazelog(W) { "Failed to parse setDateOfBirth day value '$day'" }
            return
        }

        Braze.getInstance(context).runOnUser {
            it.setDateOfBirth(yearValue, monthValue, dayValue)
        }
    }

    @JavascriptInterface
    fun setCountry(country: String?) {
        Braze.getInstance(context).runOnUser {
            it.setCountry(country)
        }
    }

    @JavascriptInterface
    fun setLanguage(language: String?) {
        Braze.getInstance(context).runOnUser {
            it.setLanguage(language)
        }
    }

    @JavascriptInterface
    fun setHomeCity(homeCity: String?) {
        Braze.getInstance(context).runOnUser {
            it.setHomeCity(homeCity)
        }
    }

    @JavascriptInterface
    fun setEmailNotificationSubscriptionType(subscriptionType: String) {
        val subscriptionTypeEnum = subscriptionTypeFromJavascriptString(subscriptionType)
        if (subscriptionTypeEnum == null) {
            brazelog(W) {
                "Failed to parse email subscription type in Braze HTML in-app message " +
                    "javascript interface with subscription $subscriptionType"
            }
            return
        }
        Braze.getInstance(context).runOnUser {
            it.setEmailNotificationSubscriptionType(subscriptionTypeEnum)
        }
    }

    @JavascriptInterface
    fun setPushNotificationSubscriptionType(subscriptionType: String) {
        val subscriptionTypeEnum = subscriptionTypeFromJavascriptString(subscriptionType)
        if (subscriptionTypeEnum == null) {
            brazelog(W) {
                "Failed to parse push subscription type in Braze HTML in-app message " +
                    "javascript interface with subscription: $subscriptionType"
            }
            return
        }
        Braze.getInstance(context).runOnUser {
            it.setPushNotificationSubscriptionType(subscriptionTypeEnum)
        }
    }

    @JavascriptInterface
    fun setPhoneNumber(phoneNumber: String?) {
        Braze.getInstance(context).runOnUser {
            it.setPhoneNumber(phoneNumber)
        }
    }

    @JavascriptInterface
    fun setCustomUserAttributeJSON(key: String, jsonStringValue: String, merge: Boolean) {
        Braze.getInstance(context).runOnUser {
            setCustomAttribute(it, key, jsonStringValue, merge)
        }
    }

    @JavascriptInterface
    fun setCustomUserAttributeArray(key: String, jsonArrayString: String?) {
        val arrayValue = parseStringArrayFromJsonString(jsonArrayString)
        if (arrayValue == null) {
            brazelog(W) { "Failed to set custom attribute array for key $key" }
            return
        }
        Braze.getInstance(context).runOnUser {
            it.setCustomAttributeArray(key, arrayValue)
        }
    }

    @JavascriptInterface
    fun addToCustomAttributeArray(key: String, value: String) {
        Braze.getInstance(context).runOnUser {
            it.addToCustomAttributeArray(key, value)
        }
    }

    @JavascriptInterface
    fun removeFromCustomAttributeArray(key: String, value: String) {
        Braze.getInstance(context).runOnUser {
            it.removeFromCustomAttributeArray(key, value)
        }
    }

    @JavascriptInterface
    fun incrementCustomUserAttribute(attribute: String, value: String) {
        val incValue = value.toIntOrNull()
        if (incValue == null) {
            brazelog(W) { "Failed to parse incrementCustomUserAttribute increment value '$value'" }
            return
        }
        Braze.getInstance(context).runOnUser {
            it.incrementCustomUserAttribute(attribute, incValue)
        }
    }

    @JavascriptInterface
    fun setCustomLocationAttribute(attribute: String, latitude: String, longitude: String) {
        val latitudeValue = latitude.toDoubleOrNull()
        if (latitudeValue == null) {
            brazelog(W) { "Failed to parse setCustomLocationAttribute latitude value '$latitude'" }
            return
        }

        val longitudeValue = longitude.toDoubleOrNull()
        if (longitudeValue == null) {
            brazelog(W) { "Failed to parse setCustomLocationAttribute longitude value '$longitude'" }
            return
        }

        Braze.getInstance(context).runOnUser {
            it.setLocationCustomAttribute(attribute, latitudeValue, longitudeValue)
        }
    }

    @JavascriptInterface
    fun addAlias(alias: String, label: String) {
        Braze.getInstance(context).runOnUser {
            it.addAlias(alias, label)
        }
    }

    @JavascriptInterface
    fun addToSubscriptionGroup(subscriptionGroupId: String) {
        Braze.getInstance(context).runOnUser {
            it.addToSubscriptionGroup(subscriptionGroupId)
        }
    }

    @JavascriptInterface
    fun removeFromSubscriptionGroup(subscriptionGroupId: String) {
        Braze.getInstance(context).runOnUser {
            it.removeFromSubscriptionGroup(subscriptionGroupId)
        }
    }

    @VisibleForTesting
    @Suppress("MagicNumber")
    /**
     * Converts a string of a number to a [Month] enum.
     *
     * @param month String representation of an integer for a month.
     * @return
     */
    fun monthFromInt(month: String): Month? {
        val monthInt = month.toIntOrNull() ?: return null
        return if (monthInt < 1 || monthInt > 12) {
            null
        } else {
            getMonth(monthInt - 1)
        }
    }

    @VisibleForTesting
    fun subscriptionTypeFromJavascriptString(subscriptionType: String?): NotificationSubscriptionType? =
        fromValue(subscriptionType)

    @VisibleForTesting
    fun setCustomAttribute(user: BrazeUser, key: String, jsonStringValue: String, merge: Boolean) {
        try {
            val jsonObject = JSONObject(jsonStringValue)
            // JSONObject in Android never deals with float values, which
            // accounts for why that instanceof check is missing below
            when (val valueObject = jsonObject[JS_BRIDGE_ATTRIBUTE_VALUE]) {
                is String -> {
                    user.setCustomUserAttribute(key, valueObject)
                }
                is Boolean -> {
                    user.setCustomUserAttribute(key, valueObject)
                }
                is Int -> {
                    user.setCustomUserAttribute(key, valueObject)
                }
                is Double -> {
                    user.setCustomUserAttribute(key, valueObject)
                }
                is JSONObject -> {
                    user.setCustomUserAttribute(key, valueObject, merge)
                }
                else -> {
                    brazelog(W) {
                        "Failed to parse custom attribute type for key: $key" +
                            " and json string value: $jsonStringValue"
                    }
                }
            }
        } catch (e: Exception) {
            brazelog(E, e) {
                "Failed to parse custom attribute type for key: $key" +
                    " and json string value: $jsonStringValue"
            }
        }
    }

    @VisibleForTesting
    fun parseStringArrayFromJsonString(jsonArrayString: String?): Array<String?>? {
        try {
            val parsedArray = JSONArray(jsonArrayString)
            return MutableList<String?>(parsedArray.length()) { i -> parsedArray.getString(i) }.toTypedArray()
        } catch (e: Exception) {
            brazelog(E, e) { "Failed to parse custom attribute array" }
        }
        return null
    }

    @VisibleForTesting
    @Suppress("ReturnCount")
    fun parseGender(genderString: String): Gender? {
        when (genderString.lowercase(Locale.US)) {
            Gender.MALE.forJsonPut() -> {
                return Gender.MALE
            }
            Gender.FEMALE.forJsonPut() -> {
                return Gender.FEMALE
            }
            Gender.OTHER.forJsonPut() -> {
                return Gender.OTHER
            }
            Gender.UNKNOWN.forJsonPut() -> {
                return Gender.UNKNOWN
            }
            Gender.NOT_APPLICABLE.forJsonPut() -> {
                return Gender.NOT_APPLICABLE
            }
            Gender.PREFER_NOT_TO_SAY.forJsonPut() -> {
                return Gender.PREFER_NOT_TO_SAY
            }
            else -> return null
        }
    }

    companion object {
        const val JS_BRIDGE_ATTRIBUTE_VALUE = "value"

        private fun Braze.runOnUser(block: (user: BrazeUser) -> Unit) {
            this.getCurrentUser {
                block(it)
            }
        }
    }
}
