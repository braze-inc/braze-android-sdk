package com.appboy.sample.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.braze.coroutine.BrazeCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Single DataStore instance for all Droidboy app preferences.
 * This replaces the multiple SharedPreferences files previously used.
 */
val Context.prefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "droidboy_prefs"
)

internal val dataStoreScope: CoroutineScope
    get() = CoroutineScope(
        BrazeCoroutineScope.coroutineContext + SupervisorJob()
    )

/**
 * Keys for all preferences stored in the DataStore.
 * Organized by the original SharedPreferences file they came from.
 */
object DroidboyPreferenceKeys {
    // From "droidboy" SharedPreferences
    val USER_ID = stringPreferencesKey("user.id")
    val BANNER_ID = stringPreferencesKey("banner.id")
    val LAST_SEEN_CUSTOM_EVENTS_AND_PURCHASES = stringPreferencesKey("last_seen_custom_events_and_purchases")

    // From "com.appboy.sample.sharedpreferences"
    val OVERRIDE_API_KEY = stringPreferencesKey("override_api_key")
    val OVERRIDE_ENDPOINT = stringPreferencesKey("override_endpoint_url")
    val ENABLE_SDK_AUTH = booleanPreferencesKey("enable_sdk_auth_if_present_pref_key")
    val MIN_TRIGGER_INTERVAL = stringPreferencesKey("min_trigger_interval")
    val OVERRIDE_API_KEY_ALIAS = stringPreferencesKey("override_api_key_alias")
    val USER_FIRST_NAME = stringPreferencesKey("user.firstname")
    val USER_LAST_NAME = stringPreferencesKey("user.lastname")
    val USER_LANGUAGE = stringPreferencesKey("user.language")
    val USER_EMAIL = stringPreferencesKey("user.email")
    val USER_GENDER = intPreferencesKey("user.gender_resource_id")
    val USER_BIRTHDAY = stringPreferencesKey("user.birthday")
    val USER_PHONE_NUMBER = stringPreferencesKey("user.phone_number")

    // From "com.appboy.sample.sharedpreferences.api_key"
    // API keys are stored with dynamic keys (alias -> key), so we use a prefix
    const val API_KEY_PREFIX = "stored_api_key_"

    // Log level from log_level_dialog_title prefs
    val CURRENT_LOG_LEVEL = intPreferencesKey("current_log_level")
}

/**
 * Extension functions for DataStore operations.
 * Read operations use runBlocking for compatibility with existing synchronous code.
 * Write operations use dataStoreScope for non-blocking fire-and-forget writes.
 * For new code, prefer using the suspend functions directly with coroutines.
 */
object DroidboyDataStoreUtils {

    /**
     * Synchronously reads a string value from the DataStore.
     */
    fun Context.readPrefsString(key: Preferences.Key<String>, defaultValue: String? = null): String? {
        return runBlocking {
            prefsDataStore.data.map { preferences ->
                preferences[key] ?: defaultValue
            }.first()
        }
    }

    /**
     * Synchronously reads an int value from the DataStore.
     */
    fun Context.readPrefsInt(key: Preferences.Key<Int>, defaultValue: Int): Int {
        return runBlocking {
            prefsDataStore.data.map { preferences ->
                preferences[key] ?: defaultValue
            }.first()
        }
    }

    /**
     * Synchronously reads a boolean value from the DataStore.
     */
    fun Context.readPrefsBoolean(key: Preferences.Key<Boolean>, defaultValue: Boolean): Boolean {
        return runBlocking {
            prefsDataStore.data.map { preferences ->
                preferences[key] ?: defaultValue
            }.first()
        }
    }

    /**
     * Asynchronously writes a string value to the DataStore.
     */
    fun Context.writePrefsString(key: Preferences.Key<String>, value: String?) {
        dataStoreScope.launch {
            prefsDataStore.edit { preferences ->
                if (value != null) {
                    preferences[key] = value
                } else {
                    preferences.remove(key)
                }
            }
        }
    }

    /**
     * Asynchronously writes an int value to the DataStore.
     */
    fun Context.writePrefsInt(key: Preferences.Key<Int>, value: Int) {
        dataStoreScope.launch {
            prefsDataStore.edit { preferences ->
                preferences[key] = value
            }
        }
    }

    /**
     * Asynchronously writes a boolean value to the DataStore.
     */
    fun Context.writePrefsBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStoreScope.launch {
            prefsDataStore.edit { preferences ->
                preferences[key] = value
            }
        }
    }

    /**
     * Asynchronously removes a key from the DataStore.
     */
    fun Context.removePrefsKey(key: Preferences.Key<*>) {
        dataStoreScope.launch {
            prefsDataStore.edit { preferences ->
                preferences.remove(key)
            }
        }
    }

    /**
     * Synchronously reads all stored API keys (alias -> key mappings).
     */
    fun Context.readAllPrefsApiKeys(): Map<String, String> {
        return runBlocking {
            prefsDataStore.data.map { preferences ->
                preferences.asMap()
                    .filter { (key, value) -> key.name.startsWith(DroidboyPreferenceKeys.API_KEY_PREFIX) && value is String }
                    .mapKeys { (key, _) -> key.name.removePrefix(DroidboyPreferenceKeys.API_KEY_PREFIX) }
                    .mapValues { (_, value) -> value as String }
            }.first()
        }
    }

    /**
     * Asynchronously writes an API key with its alias.
     */
    fun Context.writePrefsApiKey(alias: String, apiKey: String) {
        val key = stringPreferencesKey("${DroidboyPreferenceKeys.API_KEY_PREFIX}$alias")
        dataStoreScope.launch {
            prefsDataStore.edit { preferences ->
                preferences[key] = apiKey
            }
        }
    }
}
