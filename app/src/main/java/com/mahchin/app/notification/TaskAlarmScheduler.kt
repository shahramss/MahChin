package com.mahchin.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mahchin.app.data.model.TaskItem
import com.mahchin.app.data.model.TaskOrigin

object TaskAlarmScheduler {
    fun schedule(context: Context, task: TaskItem) {
        val alarmAt = task.alarmAtMillis ?: return
        if (alarmAt <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context, task)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmAt, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmAt, pendingIntent)
        }
    }

    fun cancel(context: Context, task: TaskItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, task))
    }

    fun rescheduleAll(context: Context, tasks: List<TaskItem>) {
        tasks.forEach { schedule(context, it) }
    }

    private fun pendingIntent(context: Context, task: TaskItem): PendingIntent {
        val requestCode = when (task.origin) {
            TaskOrigin.DAILY_INSTANCE -> 910000 + task.id.toInt()
            TaskOrigin.ONE_TIME -> 920000 + task.id.toInt()
            TaskOrigin.TEMPLATE -> 930000 + task.id.toInt()
        }
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(TaskAlarmReceiver.EXTRA_TITLE, task.title)
            putExtra(TaskAlarmReceiver.EXTRA_PROJECT, task.projectName ?: "")
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
