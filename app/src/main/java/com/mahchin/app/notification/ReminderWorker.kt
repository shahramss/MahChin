package com.mahchin.app.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mahchin.app.MainActivity
import com.mahchin.app.R
import com.mahchin.app.data.db.AppDatabase
import com.mahchin.app.data.model.ReminderIntensity
import com.mahchin.app.data.repository.TaskRepository
import com.mahchin.app.domain.JalaliCalendar
import java.time.LocalTime

class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(context).taskDao()
        val repo = TaskRepository(dao)
        val settings = dao.getSettings() ?: com.mahchin.app.data.model.UserSettings()
        val nowHour = LocalTime.now().hour
        if (nowHour < settings.startHour || nowHour >= settings.endHour) return Result.success()

        val today = JalaliCalendar.today()
        val remaining = repo.remainingCount(today)
        if (remaining <= 0) {
            NotificationHelper.clearTodayReminder(context)
            return Result.success()
        }

        if (!NotificationHelper.canNotify(context)) return Result.success()

        showNotification(
            remaining,
            settings.reminderIntensity == ReminderIntensity.VERY_SERIOUS || settings.soundEnabled,
            settings.vibrationEnabled || settings.reminderIntensity == ReminderIntensity.VERY_SERIOUS
        )
        return Result.success()
    }

    private fun showNotification(remaining: Int, sound: Boolean, vibrate: Boolean) {
        val openIntent = Intent(context, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            context,
            10,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply { action = ReminderActionReceiver.ACTION_SNOOZE }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            11,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val moveIntent = Intent(context, ReminderActionReceiver::class.java).apply { action = ReminderActionReceiver.ACTION_MOVE_TO_TOMORROW }
        val movePendingIntent = PendingIntent.getBroadcast(
            context,
            12,
            moveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channel = if (sound || vibrate) NotificationChannels.CHANNEL_STRICT else NotificationChannels.CHANNEL_NORMAL
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("برنامه امروز کامل نشده")
            .setContentText("${remaining} کار از امروز باقی مانده.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("چند کار از امروز باقی مانده. برای مدیریت برنامه امروز وارد ماه‌چین شو."))
            .setPriority(if (sound || vibrate) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_launcher, "نمایش برنامه", openPendingIntent)
            .addAction(R.drawable.ic_launcher, "یادآوری ۱ ساعت دیگر", snoozePendingIntent)
            .addAction(R.drawable.ic_launcher, "انتقال باقی‌مانده‌ها به فردا", movePendingIntent)

        if (!sound && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) builder.setSilent(true)
        if (vibrate) builder.setVibrate(longArrayOf(0, 300, 150, 300))

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // کاربر مجوز نوتیفیکیشن را نداده است.
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1405
    }
}
