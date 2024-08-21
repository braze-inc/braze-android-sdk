package com.appboy.sample.activity

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appboy.sample.R
import com.appboy.sample.R.id
import com.braze.models.BrazeGeofence
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.brazelog
import com.braze.support.BrazeLogger.getBrazeLogTag
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONException
import org.json.JSONObject

class GeofencesMapActivity : AppCompatActivity(), OnMapReadyCallback {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.geofences_map)
        val mapFragment = supportFragmentManager
            .findFragmentById(id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // Note that this is for testing purposes only.  This storage location and format are not a supported API.
        val registeredGeofencePrefs = applicationContext
            .getSharedPreferences(REGISTERED_GEOFENCE_SHARED_PREFS_LOCATION, MODE_PRIVATE)
        val registeredGeofences = retrieveBrazeGeofencesFromLocalStorage(registeredGeofencePrefs)
        val color = Color.BLUE

        val cameraPosition = CameraPosition.builder()
            .zoom(12f)

        if (registeredGeofences.isNotEmpty()) {
            for (registeredGeofence in registeredGeofences) {
                googleMap.addCircle(
                    CircleOptions()
                        .center(LatLng(registeredGeofence.latitude, registeredGeofence.longitude))
                        .radius(registeredGeofence.radiusMeters)
                        .strokeColor(Color.RED)
                        .fillColor(Color.argb(Math.round(Color.alpha(color) * .20).toInt(), Color.red(color), Color.green(color), Color.blue(color)))
                )
                googleMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(registeredGeofence.latitude, registeredGeofence.longitude))
                        .title("Braze Geofence")
                        .snippet(
                            registeredGeofence.latitude.toString() + ", " + registeredGeofence.longitude
                                + ", radius: " + registeredGeofence.radiusMeters + "m"
                        )
                )
            }
            val firstGeofence = registeredGeofences[0]
            cameraPosition.target(LatLng(firstGeofence.latitude, firstGeofence.longitude))
        } else {
            // NYC
            cameraPosition.target(LatLng(40.730610, -73.935242))
        }
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition.build()))
    }

    companion object {
        private val TAG = getBrazeLogTag(GeofencesMapActivity::class.java)
        private const val REGISTERED_GEOFENCE_SHARED_PREFS_LOCATION = "com.appboy.support.geofences"

        // Note that this is for testing purposes only.  This storage location and format are not a supported API.
        private fun retrieveBrazeGeofencesFromLocalStorage(sharedPreferences: SharedPreferences): List<BrazeGeofence> {
            val geofences: MutableList<BrazeGeofence> = ArrayList()
            val storedGeofences = sharedPreferences.all
            if (storedGeofences.isNullOrEmpty()) {
                brazelog(TAG) { "Did not find stored geofences." }
                return geofences
            }
            val keys: Set<String> = storedGeofences.keys
            for (key in keys) {
                val geofenceString = sharedPreferences.getString(key, null) ?: continue
                try {
                    val geofenceJson = JSONObject(geofenceString)
                    val brazeGeofence = BrazeGeofence(geofenceJson)
                    geofences.add(brazeGeofence)
                } catch (e: JSONException) {
                    brazelog(TAG, E, e) { "Encountered Json exception while parsing stored geofence: $geofenceString" }
                } catch (e: Exception) {
                    brazelog(TAG, E, e) { "Encountered unexpected exception while parsing stored geofence: $geofenceString" }
                }
            }
            return geofences
        }
    }
}
