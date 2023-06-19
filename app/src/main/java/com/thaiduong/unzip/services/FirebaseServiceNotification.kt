package com.thaiduong.unzip.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.thaiduong.unzip.R

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FirebaseServiceNotification : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        message.let {
            val data = it.notification
            createNotification(data!!)
        }
    }

    private fun createNotification(data: RemoteMessage.Notification) {
        val intent = Intent()
        intent.action = "SEND_NOTIFICATION"
        intent.putExtra(FirebaseServiceNotification::class.java.toString(), true)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flag)
        val channelId = this.applicationContext.packageName
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(this, R.color.purple_200))
            .setContentTitle(if (data.title.isNullOrBlank()) "New message" else data.title)
            .setContentText(data.body)
            .setDefaults(Notification.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setNumber(1)
        val notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        let {
            val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel(channelId, "User Setting", NotificationManager.IMPORTANCE_HIGH)
            } else {
                return
            }
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0, notificationBuilder.build())
    }

}