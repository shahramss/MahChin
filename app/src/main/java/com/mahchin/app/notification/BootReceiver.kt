package com.mahchin.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mahchin.app.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val settings = AppDatabase.getInstance(context).taskDao().getSettings()
                ?: com.mahchin.app.data.model.UserSettings()
            ReminderScheduler.schedulePeriodic(context, settings)
            pending.finish()
        }
    }
}
