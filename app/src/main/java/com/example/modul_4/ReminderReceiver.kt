package com.example.modul_4

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import java.util.Calendar

const val ALARM_REQUEST_CODE = 911
const val PREFS_NAME = "pill_prefs"
const val PREFS_KEY_ENABLED = "reminder_enabled"
const val CHANNEL_ID = "pill_channel"
const val CHANNEL_NAME = "Напоминание о таблетке"

fun scheduleAlarm(context: Context) {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 4)
        set(Calendar.MINUTE, 52)
        set(Calendar.SECOND, 30)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !alarmManager.canScheduleExactAlarms()
    ) {
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    } else {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit { putBoolean(PREFS_KEY_ENABLED, true) }
}

fun cancelAlarm(context: Context) {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)

    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit { putBoolean(PREFS_KEY_ENABLED, false) }
}

fun isReminderEnabled(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(PREFS_KEY_ENABLED, false)
}

fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Ежедневное напоминание принять таблетку в 20:00"
    }
    context.getSystemService(NotificationManager::class.java)
        .createNotificationChannel(channel)
}

fun nextReminderLabel(): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 7)
        set(Calendar.MINUTE, 42)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return if (target.timeInMillis > now.timeInMillis) "сегодня" else "завтра"
}

class ReminderViewModel : ViewModel() {
    var isEnabled by mutableStateOf(false)
        private set

    fun init(context: Context) {
        isEnabled = isReminderEnabled(context)
    }

    fun enable(context: Context) {
        createNotificationChannel(context)
        scheduleAlarm(context)
        isEnabled = true
    }

    fun disable(context: Context) {
        cancelAlarm(context)
        isEnabled = false
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("Alarm", "ReminderReceiver сработал!")
        showNotification(context)
        scheduleAlarm(context)
    }

    private fun showNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)            .setContentTitle("Время таблетки!")
            .setContentText("Не забудьте принять таблетку в 20:00")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Не забудьте принять таблетку в 20:00")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(2001, notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            if (isReminderEnabled(context)) {
                createNotificationChannel(context)
                scheduleAlarm(context)
            }
        }
    }
}