package com.example

import android.app.Application
import com.example.data.BanjulWayDatabase
import com.example.data.BanjulWayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class BanjulWayApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { BanjulWayDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { BanjulWayRepository(database.dao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "BanjulWay Alerts"
            val descriptionText = "Notifications for nearby ride requests and booking status updates"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("banjulway_driver_alerts", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
