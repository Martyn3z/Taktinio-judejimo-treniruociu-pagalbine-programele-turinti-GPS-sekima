// AzureService.kt
package com.example.bakalauras_isks0_kiltinavicius_martynas

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object AzureService {
    private const val SIGNALR_NEGOTIATE_URL = "https://isks0androidappcomms.azurewebsites.net/api/negotiate"
    lateinit var hubConnection: HubConnection

    // Function to start the SignalR connection
    suspend fun startConnection(sessionPassword: String?, userFull: String?): Boolean {
        return try {
            val negotiationResponse = negotiate(SIGNALR_NEGOTIATE_URL)

            val hubUrl = negotiationResponse["url"].toString()
            val accessToken = negotiationResponse["accessToken"].toString()

            if (hubUrl.isNotEmpty() && accessToken.isNotEmpty()) {
                hubConnection = HubConnectionBuilder
                    .create(hubUrl)
                    .withAccessTokenProvider(Single.defer { Single.just(accessToken) })
                    .build()
                hubConnection.start().blockingAwait()

                hubConnection.on("connected",
                    {
                        // Additional actions upon successful connection
                    }, String::class.java)

                val joinGroupResult = joinGroup(sessionPassword, userFull)
                joinGroupResult
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // Function to join a SignalR group
    private fun joinGroup(groupName: String?, userFull: String?): Boolean {
        return try {
            if (groupName != null) {
                if (hubConnection.connectionState == HubConnectionState.CONNECTED) {
                    hubConnection.invoke("JoinGroup", groupName, userFull)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // Function to perform SignalR negotiation
    private suspend fun negotiate(negotiateUrl: String): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val url = URL(negotiateUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"

            val inputStream = connection.inputStream
            val jsonResponse = inputStream.bufferedReader().use { it.readText() }

            Gson().fromJson(jsonResponse, Map::class.java) as Map<String, Any>

        } catch (e: Exception) {
            mapOf("error" to "An error occurred. Check the logs for details.")
        }
    }

    // Function to send data to Azure SignalR
    fun sendToAzure(userFull: String, latitude: Double, longitude: Double, sessionPassword: String?) {
        Executors.newSingleThreadExecutor().execute {
            try {
                if (hubConnection.connectionState == HubConnectionState.CONNECTED) {
                    val messageContent = "{\"user\":\"$userFull\",\"latitude\":$latitude,\"longitude\":$longitude}"
                    hubConnection.invoke("SendMapData", sessionPassword, messageContent)
                } else {
                    Log.i("Data Send", "Hub connection is not in a valid state for invoking methods.")

                    // Launch a coroutine to call startConnection asynchronously
                    GlobalScope.launch(Dispatchers.IO) {
                        // If sending data fails, attempt to start the session again
                        val sessionStarted = AzureService.startConnection(sessionPassword, userFull)

                        if (!sessionStarted) {
                            // If starting the session fails
                            Log.e("Failed Session", "Starting session failed.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.i("Data Send", "Failed to send data to the hub ${e.message}")

                // Launch a coroutine to call startConnection asynchronously
                GlobalScope.launch(Dispatchers.IO) {
                    // If sending data fails, attempt to start the session again
                    val sessionStarted = AzureService.startConnection(sessionPassword, userFull)

                    if (!sessionStarted) {
                        // If starting the session fails
                        Log.e("Failed Session", "Starting session failed.")
                    }
                }
            }
        }
    }

    // Function to stop the SignalR connection
    fun stopConnection(context: Context) {
        GlobalScope.launch(Dispatchers.Main) {
            hubConnection.stop().blockingAwait()

            if (hubConnection.connectionState == HubConnectionState.DISCONNECTED) {
                showToast(context, "Atsijungta sėkmingai")
            } else {
                showToast(context, "Nepavyko atsijungti")
            }
        }
    }

    // Function to show a toast message
    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
