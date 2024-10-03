package com.braze.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.braze.managers.BrazeGeofenceManager
import com.braze.managers.IBrazeGeofenceLocationUpdateListener
import com.braze.models.BrazeGeofence
import com.braze.models.outgoing.BrazeLocation
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.Priority.W
import com.braze.support.BrazeLogger.brazelog
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@SuppressLint("MissingPermission")
object GooglePlayLocationUtils {
    private const val REGISTERED_GEOFENCE_SHARED_PREFS_LOCATION = "com.appboy.support.geofences"

    /**
     * Requests to register the given list of geofences with Google Play Location Services.
     *
     * If a given geofence is already registered with Google Play Location Services, it will not be
     * needlessly re-registered. Geofences that are registered with Google Play Location Services but
     * not included in [desiredGeofencesToRegister] will be un-registered.
     *
     * If [desiredGeofencesToRegister] is empty, all geofences will be un-registered and deleted from local
     * storage.
     * @param context used by shared preferences
     * @param desiredGeofencesToRegister list of [BrazeGeofence] objects to register if new or updated. Otherwise ignored.
     * @param geofenceRequestIntent pending intent to fire when geofences transition events occur.
     * @param removalFunction function to remove geofences from Google Play Location Services.
     * @param registerFunction function to register geofences with Google Play Location Services.
     */
    @JvmStatic
    fun registerGeofencesWithGooglePlayIfNecessary(
        context: Context,
        desiredGeofencesToRegister: List<BrazeGeofence>,
        geofenceRequestIntent: PendingIntent,
        removalFunction: (List<String>) -> Unit = { removeGeofencesRegisteredWithGeofencingClient(context, it) },
        registerFunction: (List<BrazeGeofence>) -> Unit = {
            registerGeofencesWithGeofencingClient(context, it, geofenceRequestIntent)
        }
    ) {
        brazelog(V) { "registerGeofencesWithGooglePlayIfNecessary called with $desiredGeofencesToRegister" }
        try {
            val prefs = getRegisteredGeofenceSharedPrefs(context)
            val registeredGeofences = BrazeGeofenceManager.retrieveBrazeGeofencesFromLocalStorage(prefs)
            val registeredGeofencesById = registeredGeofences.associateBy { it.id }

            // Given the input [desiredGeofencesToRegister] and the [registeredGeofences], we need to determine
            // which geofences are to be registered, which are obsolete, and which will be no-ops.
            // We can do this by comparing the geofence data against the registered geofences.
            // We only want to register geofences that are not already registered.
            // An obsolete Geofence is one that is registered with Google Play Services but is not in the desired list.

            // If any previously registered Geofence is missing from the desired list, it is obsolete.
            val obsoleteGeofenceIds = registeredGeofences.filter { registeredGeofence ->
                desiredGeofencesToRegister.none { desiredGeofence ->
                    desiredGeofence.id == registeredGeofence.id
                }
            }.map { it.id }

            // If any desired Geofence is not already registered, it is new and needs to be registered.
            // Additionally, any previously registered geofence that has received updates should be re-registered.
            val newGeofencesToRegister = mutableListOf<BrazeGeofence>()
            for (desiredGeofence in desiredGeofencesToRegister) {
                val registeredGeofenceWithSameId = registeredGeofencesById[desiredGeofence.id]
                if (registeredGeofenceWithSameId == null || !desiredGeofence.equivalentServerData(registeredGeofenceWithSameId)) {
                    brazelog { "Geofence with id: ${desiredGeofence.id} is new or has been updated." }
                    newGeofencesToRegister.add(desiredGeofence)
                }
            }

            if (obsoleteGeofenceIds.isNotEmpty()) {
                brazelog { "Un-registering $obsoleteGeofenceIds obsolete geofences from Google Play Services." }
                removalFunction(obsoleteGeofenceIds)
            } else {
                brazelog { "No obsolete geofences need to be unregistered from Google Play Services." }
            }
            if (newGeofencesToRegister.isNotEmpty()) {
                brazelog { "Registering $newGeofencesToRegister new geofences with Google Play Services." }
                registerFunction(newGeofencesToRegister)
            } else {
                brazelog { "No new geofences need to be registered with Google Play Services." }
            }
        } catch (e: Exception) {
            brazelog(E, e) { "Exception while adding geofences." }
        }
    }

    /**
     * Requests a single location update from Google Play Location Services for the given pending intent.
     *
     * @param context
     * @param resultListener A callback of type [IBrazeGeofenceLocationUpdateListener]
     * which will be informed of the result of location update.
     */
    @JvmStatic
    fun requestSingleLocationUpdateFromGooglePlay(
        context: Context,
        resultListener: IBrazeGeofenceLocationUpdateListener
    ) {
        try {
            brazelog { "Requesting single location update from Google Play Services." }
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener {
                    brazelog(V) { "Single location request from Google Play services was successful." }
                    resultListener.onLocationRequestComplete(BrazeLocation(it))
                }
                .addOnFailureListener { error: Exception? ->
                    brazelog(E, error) { "Failed to get single location update from Google Play services." }
                    resultListener.onLocationRequestComplete(null)
                }
        } catch (e: Exception) {
            brazelog(W, e) { "Failed to request location update due to exception." }
        }
    }

    /**
     * Delete the cache of registered geofences. This will cause any geofences passed to
     * [.registerGeofencesWithGooglePlayIfNecessary]
     * on the next call to that method to be registered.
     */
    @JvmStatic
    fun deleteRegisteredGeofenceCache(context: Context) {
        brazelog { "Deleting registered geofence cache." }
        getRegisteredGeofenceSharedPrefs(context).edit().clear().apply()
    }

    /**
     * Registers a list of [Geofence] with a [com.google.android.gms.location.GeofencingClient].
     *
     * @param context
     * @param newGeofencesToRegister List of [BrazeGeofence]s to register
     * @param geofenceRequestIntent A pending intent to fire on completion of adding the [Geofence]s with
     * the [com.google.android.gms.location.GeofencingClient].
     */
    private fun registerGeofencesWithGeofencingClient(
        context: Context,
        newGeofencesToRegister: List<BrazeGeofence>,
        geofenceRequestIntent: PendingIntent
    ) {
        val newGooglePlayGeofencesToRegister = newGeofencesToRegister.map { it.toGeofence() }
        val geofencingRequest = GeofencingRequest.Builder()
            .addGeofences(newGooglePlayGeofencesToRegister) // no initial trigger
            .setInitialTrigger(0)
            .build()
        LocationServices.getGeofencingClient(context).addGeofences(geofencingRequest, geofenceRequestIntent)
            .addOnSuccessListener {
                brazelog { "Geofences successfully registered with Google Play Services." }
                storeGeofencesToSharedPrefs(context, newGeofencesToRegister)
            }
            .addOnFailureListener { geofenceError: Exception? ->
                if (geofenceError is ApiException) {
                    when (val statusCode = geofenceError.statusCode) {
                        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> brazelog(W) {
                            "Geofences not registered with Google Play Services due to GEOFENCE_TOO_MANY_GEOFENCES: $statusCode"
                        }
                        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> brazelog(W) {
                            "Geofences not registered with Google Play Services due to GEOFENCE_TOO_MANY_PENDING_INTENTS: $statusCode"
                        }
                        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> brazelog(W) {
                            "Geofences not registered with Google Play Services due to GEOFENCE_NOT_AVAILABLE: $statusCode"
                        }
                        GeofenceStatusCodes.SUCCESS ->
                            // Since we're in the failure listener, we don't expect this status code to appear. Nonetheless, it would
                            // be good to not surface this status code as unknown
                            brazelog {
                                "Received Geofence registration success code in failure block with Google Play Services."
                            }
                        else -> brazelog(W) { "Geofence pending result returned unknown status code: $statusCode" }
                    }
                } else {
                    brazelog(E, geofenceError) { "Geofence exception encountered while adding geofences." }
                }
            }
    }

    /**
     * Un-registers a list of [Geofence] with a [com.google.android.gms.location.GeofencingClient].
     *
     * @param context
     * @param obsoleteGeofenceIds List of [String]s containing Geofence IDs that needs to be un-registered
     */
    @VisibleForTesting
    internal fun removeGeofencesRegisteredWithGeofencingClient(context: Context, obsoleteGeofenceIds: List<String>) {
        LocationServices.getGeofencingClient(context).removeGeofences(obsoleteGeofenceIds)
            .addOnSuccessListener {
                brazelog { "Geofences successfully un-registered with Google Play Services." }
                removeGeofencesFromSharedPrefs(context, obsoleteGeofenceIds)
            }
            .addOnFailureListener { geofenceError: Exception? ->
                if (geofenceError is ApiException) {
                    when (val statusCode = geofenceError.statusCode) {
                        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> brazelog(W) {
                            "Geofences cannot be un-registered with Google Play Services due to GEOFENCE_TOO_MANY_GEOFENCES: $statusCode"
                        }
                        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> brazelog(W) {
                            "Geofences cannot be un-registered with Google Play Services due to GEOFENCE_TOO_MANY_PENDING_INTENTS: $statusCode"
                        }
                        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> brazelog(W) {
                            "Geofences cannot be un-registered with Google Play Services due to GEOFENCE_NOT_AVAILABLE: $statusCode"
                        }
                        GeofenceStatusCodes.SUCCESS ->
                            // Since we're in the failure listener, we don't expect this status code to appear. Nonetheless, it would
                            // be good to not surface this status code as unknown
                            brazelog {
                                "Received Geofence un-registration success code in failure block with Google Play Services."
                            }
                        else -> brazelog(W) { "Geofence pending result returned unknown status code: $statusCode" }
                    }
                } else {
                    brazelog(E, geofenceError) { "Geofence exception encountered while removing geofences." }
                }
            }
    }

    /**
     * Returns a [SharedPreferences] instance holding list of registered [BrazeGeofence]s.
     */
    @VisibleForTesting
    internal fun getRegisteredGeofenceSharedPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(REGISTERED_GEOFENCE_SHARED_PREFS_LOCATION, Context.MODE_PRIVATE)

    /**
     * Stores the list of [BrazeGeofence] which are successfully registered.
     *
     * @param context
     * @param newGeofencesToRegister List of [BrazeGeofence]s to store in SharedPreferences
     */
    @VisibleForTesting
    internal fun storeGeofencesToSharedPrefs(context: Context, newGeofencesToRegister: List<BrazeGeofence>) {
        val editor = getRegisteredGeofenceSharedPrefs(context).edit()
        for (brazeGeofence in newGeofencesToRegister) {
            editor.putString(brazeGeofence.id, brazeGeofence.forJsonPut().toString())
            brazelog(V) { "Geofence with id: ${brazeGeofence.id} added to shared preferences." }
        }
        editor.apply()
    }

    /**
     * Removes the list of [BrazeGeofence] which are now un-registered with Google Play Services.
     *
     * @param context
     * @param obsoleteGeofenceIds List of [String]s containing Geofence IDs that are un-registered
     */
    private fun removeGeofencesFromSharedPrefs(context: Context, obsoleteGeofenceIds: List<String>) {
        val editor = getRegisteredGeofenceSharedPrefs(context).edit()
        for (id in obsoleteGeofenceIds) {
            editor.remove(id)
            brazelog(V) { "Geofence with id: $id removed from shared preferences." }
        }
        editor.apply()
    }
}

/**
 * Creates a Google Play Location Services Geofence object from a BrazeGeofence.
 * @return A Geofence object.
 */
fun BrazeGeofence.toGeofence(): Geofence {
    val builder = Geofence.Builder()
    builder
        .setRequestId(id)
        .setCircularRegion(latitude, longitude, radiusMeter.toFloat())
        .setNotificationResponsiveness(notificationResponsivenessMs)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
    var transitionTypes = 0
    if (enterEvents) {
        transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_ENTER
    }
    if (exitEvents) {
        transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_EXIT
    }
    builder.setTransitionTypes(transitionTypes)
    return builder.build()
}
