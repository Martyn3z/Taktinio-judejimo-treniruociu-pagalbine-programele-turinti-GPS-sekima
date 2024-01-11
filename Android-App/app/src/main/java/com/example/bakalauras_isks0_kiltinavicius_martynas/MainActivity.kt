// MainActivity.kt
package com.example.bakalauras_isks0_kiltinavicius_martynas

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import sha256

class MainActivity : AppCompatActivity() {

    private lateinit var slapyvardisField: EditText
    private lateinit var specialybeField: EditText
    private lateinit var sesijosKodasField: EditText
    private lateinit var sukurtiPrisijungtiButton: Button
    private lateinit var uzdarytiProgramaButton: Button
    private lateinit var pagalbaIrDUKButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        slapyvardisField = findViewById(R.id.slapyvardisField)
        specialybeField = findViewById(R.id.specialybeField)
        sesijosKodasField = findViewById(R.id.sesijosKodasField)
        sukurtiPrisijungtiButton = findViewById(R.id.sukurtiPrisijungtiButton)
        pagalbaIrDUKButton = findViewById(R.id.pagalbaIrDUKButton)
        uzdarytiProgramaButton = findViewById(R.id.uzdarytiProgramaButton)

        // Call getUserData to fill out fields if data exists
        getUserData()

        sukurtiPrisijungtiButton.setOnClickListener {
            // Input field text
            val password = sesijosKodasField.text.toString()
            val slapyvardis = slapyvardisField.text.toString()
            val specialybe = specialybeField.text.toString()
            // Intent object to navigate user to the map view
            val intent = Intent(this@MainActivity, MapActivity::class.java)

            // Check if password meets requirements
            if (isPasswordValid(password)) {
                // If password meets requirements, proceed
                if (hasLocationPermission()) {
                    // Perform SHA-256 encryption
                    val hashedPassword = sha256(password)
                    if (hashedPassword != null) {
                        // Save user data in SharedPreferences
                        saveUserData(slapyvardis, specialybe, hashedPassword)

                        // Use Coroutine to execute network request
                        GlobalScope.launch(Dispatchers.IO) {
                            startActivity(intent)
                        }
                    } else {
                        // If encryption fails, show a message
                        showToast("Failed to create encrypted stream, please try again.")
                    }
                } else {
                    // If location permission is not granted, navigate user to RequestPermissionActivity
                    val permissionIntent = Intent(this, RequestPermissionActivity::class.java)
                    startActivity(permissionIntent)
                }
            }
        }

        pagalbaIrDUKButton.setOnClickListener {
            // Handles the click of the "Help and FAQ" button
            val intent = Intent(this, HelpActivity::class.java)
            startActivity(intent)
        }

        uzdarytiProgramaButton.setOnClickListener {
            // Handles the click of the "Close the App" button
            this.finish()
        }
    }

    private fun saveUserData(slapyvardis: String, specialybe: String, hashedPassword: String) {
        val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Save user data in SharedPreferences
        editor.putString("username", slapyvardis)
        editor.putString("userClass", specialybe)
        editor.putString("userFull", "$slapyvardis - $specialybe")
        editor.putString("sessionCode", hashedPassword)

        editor.apply()
    }

    private fun getUserData() {
        val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)

        // Retrieve data from SharedPreferences
        val username = sharedPreferences.getString("username", "")
        val userClass = sharedPreferences.getString("userClass", "")

        // Set the retrieved data to the respective EditText fields
        slapyvardisField.setText(username)
        specialybeField.setText(userClass)
    }

    private fun isPasswordValid(password: String): Boolean {
        // Check if password meets the requirements
        if (password.length < 14) {
            showToast("Session code must be at least 14 characters long.")
            return false
        }

        // Check if password has at least one symbol
        val symbolRegex = Regex("[!@#\$%^&*(),.?\":{}|<>_-]")
        if (!symbolRegex.containsMatchIn(password)) {
            showToast("Session code must have at least one symbol.")
            return false
        }

        // Check if password has at least one digit
        val digitRegex = Regex("\\d")
        if (!digitRegex.containsMatchIn(password)) {
            showToast("Session code must have at least one digit.")
            return false
        }

        // Check if password has at least one uppercase letter
        val uppercaseRegex = Regex("[A-Z]")
        if (!uppercaseRegex.containsMatchIn(password)) {
            showToast("Session code must have at least one uppercase letter.")
            return false
        }

        // Check if password has at least one lowercase letter
        val lowercaseRegex = Regex("[a-z]")
        if (!lowercaseRegex.containsMatchIn(password)) {
            showToast("Session code must have at least one lowercase letter.")
            return false
        }

        // All requirements met
        return true
    }

    private fun hasLocationPermission(): Boolean {
        // Check if ACCESS_FINE_LOCATION permission is granted
        return ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
