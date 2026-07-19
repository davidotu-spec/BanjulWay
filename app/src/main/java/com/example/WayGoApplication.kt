package com.example

import android.app.Application
import com.google.firebase.FirebaseApp
import com.example.data.WayGoDatabase
import com.example.data.WayGoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class WayGoApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { WayGoDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { WayGoRepository(database.dao()) }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "WayGo Alerts"
            val descriptionText = "Notifications for nearby ride requests and booking status updates"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("waygo_driver_alerts", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
