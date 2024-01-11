package com.example.bakalauras_isks0_kiltinavicius_martynas

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class RequestPermissionActivity : AppCompatActivity() {

    private val timeoutHandler = Handler()
    private val timeoutMillis: Long = 3000 // Set the waiting time to 3 seconds

    private val checkPermissionRunnable = Runnable {
        // Check if location and foreground service permissions are granted
        if (hasLocationPermission() && hasForegroundServicePermission()) {
            // If both permissions are granted, proceed to MapActivity
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // If not, check which permission is missing and request it
            checkAndRequestPermissions()
        }
    }

    private fun hasLocationPermission(): Boolean {
        // Check if ACCESS_FINE_LOCATION permission is granted
        return ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasForegroundServicePermission(): Boolean {
        // Check if FOREGROUND_SERVICE permission is granted (depends on Android version)
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.FOREGROUND_SERVICE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // FOREGROUND_SERVICE permission not needed on versions prior to Android Q
        }
    }

    private fun checkAndRequestPermissions() {
        // Check and request missing permissions
        if (!hasLocationPermission()) {
            requestLocationPermission()
        }
        if (!hasForegroundServicePermission()) {
            requestForegroundServicePermission()
        }
    }

    private fun requestLocationPermission() {
        // Request ACCESS_FINE_LOCATION permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun requestForegroundServicePermission() {
        // Request FOREGROUND_SERVICE permission (depends on Android version)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.FOREGROUND_SERVICE),
                FOREGROUND_SERVICE_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_permission)

        val messageTextView = findViewById<TextView>(R.id.messageTextView)

        // Open app settings when the user clicks on the messageTextView
        messageTextView.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Settings.Secure.getUriFor("package:$packageName")
            intent.data = uri
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        // Delay the permission check to allow the user to grant permission
        timeoutHandler.postDelayed(checkPermissionRunnable, timeoutMillis)
    }

    override fun onPause() {
        super.onPause()
        timeoutHandler.removeCallbacks(checkPermissionRunnable)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val FOREGROUND_SERVICE_PERMISSION_REQUEST_CODE = 2
    }
}
