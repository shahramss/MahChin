package com.mahchin.app.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mahchin.app.data.model.UserSettings
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val PERIODIC_WORK = "mahchin_periodic_reminders"
    private const val SNOOZE_WORK = "mahchin_snooze_reminder"

    fun schedulePeriodic(context: Context, settings: UserSettings) {
        val intervalHours = settings.reminderIntensity.hours.coerceAtLeast(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(intervalHours, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelayMillis(settings.startHour), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleSnooze(context: Context, minutes: Long = 60) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SNOOZE_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun calculateInitialDelayMillis(startHour: Int): Long {
        val now = LocalDateTime.now()
        var next = now.withHour(startHour.coerceIn(0, 23)).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(0)
    }
}
