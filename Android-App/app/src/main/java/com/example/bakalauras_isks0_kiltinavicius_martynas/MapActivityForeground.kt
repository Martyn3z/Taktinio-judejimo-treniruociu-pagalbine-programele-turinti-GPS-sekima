package com.example.bakalauras_isks0_kiltinavicius_martynas

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

class MapForegroundServiceActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Initialize map and start foreground service with data
        initMap()
        startForegroundService()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Additional map initialization if needed
    }

    private fun initMap() {
        // Obtain location client and map fragment
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun startForegroundService() {
        // Check if ACCESS_FINE_LOCATION permission is granted
        if (hasLocationPermission()) {
            // Pass data to ForegroundService using Intent
            val serviceIntent = Intent(this, ForegroundService::class.java)
            serviceIntent.putExtra("someKey", "someValue") // Add any data you want to pass
            startService(serviceIntent)
        } else {
            // Request ACCESS_FINE_LOCATION permission if not granted
            requestLocationPermission()
        }
    }

    private fun hasLocationPermission(): Boolean {
        // Check if ACCESS_FINE_LOCATION permission is granted
        return ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        // Request ACCESS_FINE_LOCATION permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
