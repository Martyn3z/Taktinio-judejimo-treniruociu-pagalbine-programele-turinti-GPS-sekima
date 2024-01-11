// MapActivity.kt
package com.example.bakalauras_isks0_kiltinavicius_martynas

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var searchBar: EditText
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var scheduledExecutorService: ScheduledExecutorService
    private val markerMap: MutableMap<String, Marker> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Initialize map functionality
        initMap()

        // Initialize search bar
        searchBar = findViewById(R.id.searchBar)

        lifecycleScope.launch {
            try {
                // Retrieve user data from SharedPreferences
                val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                val sessionPassword = sharedPreferences.getString("sessionCode", "")
                val userFull = sharedPreferences.getString("userFull", "")

                // Start Azure SignalR connection
                AzureService.startConnection(sessionPassword, userFull)

                // Set up a listener for receiving logs from Azure
                AzureService.hubConnection.on("receiveLogs", { logMessage: String? ->
                    runOnUiThread {
                        // Process received log messages
                        if (logMessage != null) {
                            processReceivedData(logMessage)
                        } else {
                            showToast("Nepavyko prisijungti prie grupės")
                        }
                    }
                }, String::class.java)

                // Connection successful, start sending and receiving data
                startSendingData()
            } catch (e: Exception) {
                // Handle other exceptions if necessary
                e.printStackTrace()
            }
        }

        // Set up listener for text change in the search bar
        searchBar.setOnEditorActionListener { _, _, _ ->
            // Handle search action
            performCoordinateSearch()
            true
        }
    }

    // Function to start sending data to Azure SignalR
    private fun startSendingData() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        scheduledExecutorService.scheduleAtFixedRate({
            try {
                // Retrieve user data from SharedPreferences
                val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                val sessionPassword = sharedPreferences.getString("sessionCode", "")

                runOnUiThread {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            // Create user position and send to Azure
                            AzureService.sendToAzure(
                                getUserFull(),
                                it.latitude,
                                it.longitude,
                                sessionPassword
                            )
                        }
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                showToast("Failed to get location permission")
                Log.e("LocationPermission", "Failed to get location permission", e)
            }
        }, 0, 3, TimeUnit.SECONDS)
    }

    // Function to show received coordinates in the search bar
    @SuppressLint("SetTextI18n")
    private fun showCoordinatesInSearchBar(latLng: LatLng) {
        val searchBar = findViewById<EditText>(R.id.searchBar)
        searchBar.setText("${latLng.latitude}, ${latLng.longitude}")
    }

    // OnMapReadyCallback function, called when the map is ready
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (hasLocationPermission()) {
            // If location permission is granted, continue with map initialization

            googleMap.setOnMapClickListener { latLng ->
                showCoordinatesInSearchBar(latLng)
            }

            googleMap.setOnMarkerClickListener { marker ->
                showCoordinatesInSearchBar(marker.position)
                true // Return true to consume the event and prevent default behavior
            }

            // FOR TESTING: Add markers with coordinates
            updateMapWithCoordinates("Antanas - Sniper", 55.091918, 23.98152)
            updateMapWithCoordinates("Jonas - Shooter", 55.191918, 23.88152)

            // Zoom to user's location
            zoomToUserLocation()

            // Set map type to satellite
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
        } else {
            // If location permission is not granted, redirect to RequestPermissionActivity
            redirectToRequestPermissionActivity()
        }
    }

    // Function to check if location permission is granted
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Function to initialize the map
    private fun initMap() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // Function to handle coordinate search
    private fun performCoordinateSearch() {
        val coordinateText = searchBar.text.toString()

        try {
            // Parse coordinates and move the map
            val (latitude, longitude) = coordinateText.split(",").map { it.trim().toDouble() }
            val location = LatLng(latitude, longitude)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM))
        } catch (e: Exception) {
            showToast("Please use decimal degrees format for coordinates!")
        }
    }

    // Function to zoom to user's location
    private fun zoomToUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If permissions not granted, redirect to RequestPermissionActivity
            redirectToRequestPermissionActivity()
        } else {
            googleMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    // Zoom to user's location
                    val userLocation = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM))
                }
            }
        }
    }

    // Function to redirect to RequestPermissionActivity
    private fun redirectToRequestPermissionActivity() {
        val intent = Intent(this, RequestPermissionActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Function to process received data from Azure SignalR
    private fun processReceivedData(messageContent: String) {
        try {
            val json = JSONObject(messageContent)

            // Extract received data
            val receivedUser = json.getString("user")
            val receivedLatitude = json.getDouble("latitude")
            val receivedLongitude = json.getDouble("longitude")

            // Update the map with received coordinates
            updateMapWithCoordinates(receivedUser, receivedLatitude, receivedLongitude)
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle the exception
        }
    }

    // Function to update the map with coordinates
    private fun updateMapWithCoordinates(userFull: String, latitude: Double, longitude: Double) {
        // Check if a marker with the given userFull already exists
        val existingMarker = markerMap[userFull]

        if (existingMarker != null) {
            // Update the position of the existing marker
            existingMarker.position = LatLng(latitude, longitude)
        } else {
            // Create a new marker if it doesn't exist
            val customMarkerBitmapDescriptor =
                BitmapDescriptorFactory.defaultMarker(getColorFromHash(userFull))
            val markerOptions = MarkerOptions()
                .position(LatLng(latitude, longitude))
                .icon(customMarkerBitmapDescriptor)
                .title(userFull)

            // Add the new marker to the map and store it in the markerMap
            val newMarker: Marker? = googleMap.addMarker(markerOptions)
            if (newMarker != null) {
                markerMap[userFull] = newMarker
            }
        }
    }

    // Function to get a color based on the hash of a string
    private fun getColorFromHash(input: String): Float {
        val hash = input.hashCode()

        val red = (hash and 0xFF0000) shr 16
        val green = (hash and 0x00FF00) shr 8
        val blue = hash and 0x0000FF

        val hsv = FloatArray(3)
        Color.RGBToHSV(red, green, blue, hsv)

        return hsv[0]
    }

    // Function to get the user's full name
    private fun getUserFull(): String {
        val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userFull", "") ?: ""
    }

    companion object {
        private const val DEFAULT_ZOOM = 15f
    }

    override fun onDestroy() {
        // Shut down the connection when the activity is destroyed
        scheduledExecutorService.shutdown()
        AzureService.stopConnection(this)
        super.onDestroy()
    }

    // Function to show a toast message
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
