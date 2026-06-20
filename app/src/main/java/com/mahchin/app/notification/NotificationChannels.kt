package com.mahchin.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHANNEL_NORMAL = "mahchin_reminders"
    const val CHANNEL_STRICT = "mahchin_reminders_strict"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val normal = NotificationChannel(
                CHANNEL_NORMAL,
                "یادآوری‌های ماه‌چین",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "یادآوری کارهای باقی‌مانده امروز"
                enableVibration(false)
            }
            val strict = NotificationChannel(
                CHANNEL_STRICT,
                "یادآوری خیلی جدی ماه‌چین",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "یادآوری همراه با صدا و ویبره"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
            }
            manager.createNotificationChannels(listOf(normal, strict))
        }
    }
}
