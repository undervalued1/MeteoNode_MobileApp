package com.example.meteonode.ui.fragments

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.meteonode.R

object NotificationHelper {

    const val CHANNEL_ID = "meteonode_alerts"

    fun createChannel(context: Context) {

        val channel = NotificationChannel(
            CHANNEL_ID,
            "MeteoNode уведомления",
            NotificationManager.IMPORTANCE_HIGH
        )

        val manager =
            context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(channel)
    }

    fun showAlert(
        context: Context,
        title: String,
        text: String
    ) {

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager =
            context.getSystemService(NotificationManager::class.java)

        manager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}