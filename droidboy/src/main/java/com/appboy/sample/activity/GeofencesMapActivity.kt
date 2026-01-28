package com.appboy.sample.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.appboy.sample.R
import com.braze.Braze
import com.braze.support.BrazeLogger
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class GeofencesMapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // UI Elements
    private lateinit var consoleText: TextView
    private lateinit var consoleScroll: ScrollView
    private lateinit var btnClear: Button
    private lateinit var btnRealLocation: Button
    private lateinit var btnHelp: Button

    private var isMockMode = false
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.geofences_map)

        // Initialize UI
        consoleText = findViewById(R.id.console_text)
        consoleScroll = findViewById(R.id.console_scroll)
        btnClear = findViewById(R.id.btn_clear_map)
        btnRealLocation = findViewById(R.id.btn_real_location)
        btnHelp = findViewById(R.id.btn_help)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Setup Braze logger callback to capture geofence events
        setupBrazeLoggerCallback()

        // Setup Map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        btnClear.setOnClickListener {
            if (this::map.isInitialized) {
                map.clear()
                logToConsole("Map cleared.")
            } else {
                logToConsole("Map not yet initialized. Nothing to clear.")
            }
        }

        btnRealLocation.setOnClickListener {
            useRealLocation()
        }

        btnHelp.setOnClickListener {
            showInstructionsDialog()
        }

        // Show instructions on first launch
        showInstructionsDialog()
        logToConsole("Ready. Long-press map to TELEPORT.")
    }

    override fun onDestroy() {
        // Remove the logger callback to prevent memory leaks
        BrazeLogger.onLoggedCallback = null

        // Disable mock mode when leaving
        if (isMockMode) {
            try {
                fusedLocationClient.setMockMode(false)
            } catch (e: Exception) {
                logToConsole("⚠️ Could not disable mock mode: ${e.message}")
            }
        }

        super.onDestroy()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_HYBRID
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true

        // Check permissions for "Blue Dot"
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        } else {
            logToConsole("⚠️ Missing Location Permissions")
            requestLocationPermissions()
        }

        // Default view: New York
        val startPoint = LatLng(40.7484, -73.9857)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 13f))

        // Teleport User on Long Press
        map.setOnMapLongClickListener { target ->
            teleportUser(target)
        }
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    && this::map.isInitialized
                ) {
                    map.isMyLocationEnabled = true
                    logToConsole("✅ Location permissions granted")
                }
            } else {
                logToConsole("❌ Location permissions denied")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun useRealLocation() {
        logToConsole("------------------------------")
        logToConsole("📍 Using real device location...")

        // Disable mock mode first
        if (isMockMode) {
            fusedLocationClient.setMockMode(false)
                .addOnSuccessListener {
                    isMockMode = false
                    logToConsole("Mock mode disabled")
                    fetchAndUseRealLocation()
                }
                .addOnFailureListener { e ->
                    logToConsole("⚠️ Could not disable mock mode: ${e.message}")
                    fetchAndUseRealLocation()
                }
        } else {
            fetchAndUseRealLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchAndUseRealLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            logToConsole("❌ Location permission not granted")
            requestLocationPermissions()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    logToConsole(
                        "📍 Real Location: ${String.format(
                            Locale.getDefault(),
                            "%.4f", location.latitude
                        )}, ${String.format(
                            Locale.getDefault(),
                            "%.4f", location.longitude
                        )}"
                    )

                    // Add marker
                    map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Real Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                    )
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                    // Request Braze Geofences at real location
                    logToConsole("Requesting Braze Geofences...")
                    Braze.getInstance(this).requestGeofences(location.latitude, location.longitude)
                } else {
                    logToConsole("❌ Could not get real location. Try again.")
                }
            }
            .addOnFailureListener { e ->
                logToConsole("❌ Error getting location: ${e.message}")
            }
    }

    private fun teleportUser(target: LatLng) {
        logToConsole("------------------------------")
        logToConsole(
            "📍 Teleporting to: ${String.format(
                Locale.getDefault(),
                "%.4f", target.latitude
            )}, ${String.format(
                Locale.getDefault(),
                "%.4f", target.longitude
            )}"
        )

        // Create Cyan Marker
        map.addMarker(
            MarkerOptions()
                .position(target)
                .title("Mock Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
        )
        map.animateCamera(CameraUpdateFactory.newLatLng(target))

        // Set Mock Location
        setMockLocation(target)
    }

    @SuppressLint("MissingPermission")
    private fun setMockLocation(latLng: LatLng) {
        val mockLocation = Location("fused").apply {
            latitude = latLng.latitude
            longitude = latLng.longitude
            altitude = 0.0
            time = System.currentTimeMillis()
            accuracy = 1.0f
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bearingAccuracyDegrees = 0.1f
                verticalAccuracyMeters = 0.1f
                speedAccuracyMetersPerSecond = 0.1f
            }
        }

        try {
            fusedLocationClient.setMockMode(true)
                .addOnSuccessListener {
                    isMockMode = true
                    fusedLocationClient.setMockLocation(mockLocation)
                        .addOnSuccessListener {
                            logToConsole("GPS Override: Success")
                            // Request Braze Geofences to update
                            logToConsole("Requesting Braze Geofences...")
                            Braze.getInstance(this).requestGeofences(latLng.latitude, latLng.longitude)
                        }
                        .addOnFailureListener { e ->
                            logToConsole("❌ Failed to set location: ${e.message}")
                        }
                }
                .addOnFailureListener {
                    logToConsole("❌ ERROR: Please enable 'Select Mock Location App' in Developer Options.")
                    Toast.makeText(this, "Enable Mock Location in Dev Options", Toast.LENGTH_LONG).show()
                }
        } catch (e: SecurityException) {
            logToConsole("❌ Permission Error: ${e.message}")
        }
    }

    private fun setupBrazeLoggerCallback() {
        BrazeLogger.onLoggedCallback = { _, message, _ ->
            // Filter for geofence-related logs
            if (isGeofenceRelatedLog(message)) {
                val formattedMessage = formatGeofenceLog(message)
                if (formattedMessage != null) {
                    logToConsole(formattedMessage)
                }

                // Try to extract and display geofences when they're received
                if (message.contains("geofence", ignoreCase = true)) {
                    if (message.contains("BrazeGeofence") || message.contains("\"latitude\"") || message.contains("registered")) {
                        tryExtractAndDisplayGeofences(message)
                    }
                }
            }
        }

        logToConsole("🎧 Listening for Braze geofence events...")
    }

    private fun isGeofenceRelatedLog(message: String?): Boolean {
        if (message == null) return false

        return message.contains("geofence", ignoreCase = true) ||
            message.contains("geo_id", ignoreCase = true) ||
            message.contains("RequestDispatchStartedEvent") && message.contains("geofence_event")
    }

    private fun formatGeofenceLog(message: String): String? {
        return try {
            when {
                message.contains("\"event_type\": \"enter\"") -> {
                    val geoId = extractGeoId(message)
                    "🟢 ENTERED geofence: $geoId"
                }
                message.contains("\"event_type\": \"exit\"") -> {
                    val geoId = extractGeoId(message)
                    "🔴 EXITED geofence: $geoId"
                }
                message.contains("registered", ignoreCase = true) && message.contains("geofence", ignoreCase = true) -> {
                    "📍 Geofence registered with system"
                }
                message.contains("requesting geofences", ignoreCase = true) -> {
                    "🌐 Requesting geofences from server..."
                }
                message.contains("received", ignoreCase = true) && message.contains("geofence", ignoreCase = true) -> {
                    "✅ Geofences received from server"
                }
                else -> {
                    null
                }
            }
        } catch (_: Exception) {
            "ℹ️ Geofence event"
        }
    }

    private fun extractGeoId(message: String): String {
        return try {
            val geoIdPattern = "\"geo_id\":\\s*\"([^\"]+)\"".toRegex()
            val match = geoIdPattern.find(message)
            match?.groupValues?.get(1)?.substringAfterLast("_") ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    private fun tryExtractAndDisplayGeofences(message: String) {
        try {
            // Look for JSON array or object patterns in the log message
            val jsonPattern = """\[.*?]|\{.*?\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val matches = jsonPattern.findAll(message)

            for (match in matches) {
                val jsonStr = match.value

                // Try parsing as a single geofence object
                if (jsonStr.startsWith("{") && jsonStr.contains("latitude") && jsonStr.contains("longitude")) {
                    parseAndDisplaySingleGeofence(jsonStr)
                }

                // Try parsing as an array of geofences
                if (jsonStr.startsWith("[")) {
                    parseAndDisplayGeofenceArray(jsonStr)
                }
            }
        } catch (_: Exception) {
            // Silently fail - this is just opportunistic parsing
        }
    }

    private fun parseAndDisplaySingleGeofence(jsonStr: String) {
        try {
            val geofenceObj = JSONObject(jsonStr)

            val latitude = geofenceObj.optDouble("latitude", 0.0)
            val longitude = geofenceObj.optDouble("longitude", 0.0)
            val radius = when {
                geofenceObj.has("radiusMeters") -> geofenceObj.optDouble("radiusMeters", 0.0)
                geofenceObj.has("radius") -> geofenceObj.optDouble("radius", 0.0)
                else -> 0.0
            }
            val geofenceId = geofenceObj.optString("id", "unknown")

            if (latitude != 0.0 && longitude != 0.0 && radius > 0) {
                runOnUiThread {
                    displayGeofenceOnMap(latitude, longitude, radius, geofenceId)
                }
            }
        } catch (_: Exception) {
            // Not a valid geofence JSON
        }
    }

    private fun parseAndDisplayGeofenceArray(jsonStr: String) {
        try {
            val geofencesArray = JSONArray(jsonStr)
            var displayedCount = 0

            for (i in 0 until geofencesArray.length()) {
                val geofenceObj = geofencesArray.getJSONObject(i)

                val latitude = geofenceObj.optDouble("latitude", 0.0)
                val longitude = geofenceObj.optDouble("longitude", 0.0)
                val radius = when {
                    geofenceObj.has("radiusMeters") -> geofenceObj.optDouble("radiusMeters", 0.0)
                    geofenceObj.has("radius") -> geofenceObj.optDouble("radius", 0.0)
                    else -> 0.0
                }
                val geofenceId = geofenceObj.optString("id", "unknown")

                if (latitude != 0.0 && longitude != 0.0 && radius > 0) {
                    runOnUiThread {
                        displayGeofenceOnMap(latitude, longitude, radius, geofenceId)
                    }
                    displayedCount++
                }
            }

            if (displayedCount > 0) {
                runOnUiThread {
                    logToConsole("📍 Displayed $displayedCount geofence(s) on map")
                }
            }
        } catch (_: Exception) {
            // Not a valid geofence array
        }
    }

    private fun displayGeofenceOnMap(latitude: Double, longitude: Double, radius: Double, geofenceId: String) {
        if (!this::map.isInitialized) return

        val center = LatLng(latitude, longitude)

        // Draw circle on map
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            map.addCircle(
                CircleOptions()
                    .center(center)
                    .radius(radius)
                    .strokeColor(getColor(R.color.geofence_circle_stroke_color))
                    .strokeWidth(3f)
                    .fillColor(getColor(R.color.geofence_circle_fill_color))
            )
        }

        // Add marker at center
        map.addMarker(
            MarkerOptions()
                .position(center)
                .title("Geofence")
                .snippet("ID: ${geofenceId.substringAfterLast("_")}\nRadius: ${radius.toInt()}m")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
        )
    }

    private fun showInstructionsDialog() {
        GeofencesInstructionDialogFragment().show(supportFragmentManager, "instructions")
    }

    private fun logToConsole(msg: String) {
        runOnUiThread {
            val currentTimeString = dateFormat.format(Date())
            consoleText.append("[$currentTimeString] $msg\n")
            consoleScroll.post { consoleScroll.fullScroll(View.FOCUS_DOWN) }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
}
