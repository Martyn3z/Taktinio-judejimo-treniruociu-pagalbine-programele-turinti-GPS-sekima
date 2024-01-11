// ForegroundService.kt
package com.example.bakalauras_isks0_kiltinavicius_martynas

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Perform your periodic task here (e.g., save data)

        // Display an ongoing notification to keep the service in the foreground
        startForeground(NOTIFICATION_ID, createNotification())

        // Return START_STICKY to ensure the service is restarted if it gets terminated
        return START_STICKY
    }

    private fun createNotification(): Notification {
        // Create a notification with ongoing behavior
        val notificationIntent = Intent(this, MapActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vietovės Sekimas Fone")
            .setContentText("Vietovės sekimas veikia...")
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ForegroundServiceChannel"
    }
}
