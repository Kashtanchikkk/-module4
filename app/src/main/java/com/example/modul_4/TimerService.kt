package com.example.modul_4

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Helper {
    const val CHANNEL_ID = "timer_channel"
    const val CHANNEL_NAME = "Timer Channel"

    fun createNotificationChannel(context: Context) {
        val notificationManager: NotificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }

    fun createNotificationMessage(context: Context, seconds: Int) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("Таймер")
        .setContentText("Осталось $seconds секунд")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .build()

    fun createCompletedNotificationMessage(context: Context) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("Таймер")
        .setContentText("Таймер завершён!")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .build()
}

class TimerService : Service() {
    private var seconds = 0

    private var duration = 0
    private var job: Job? = null
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private val SERVICE_ID = 100

    override fun onBind(intent: Intent): IBinder {
        return Binder()
    }

    override fun onCreate() {
        super.onCreate()
        Helper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        duration = intent?.getIntExtra("duration", 0) ?: 0

        seconds = 0

        startForeground(SERVICE_ID, Helper.createNotificationMessage(this, duration))

        job = scope.launch {
            while (true) {
                delay(1000)
                seconds++

                Log.d("TimerService", "Прошло $seconds секунд")

                if (seconds >= duration) {
                    timerCompleted()
                    break
                } else {
                    updateNotification()
                }
            }
        }
        return START_STICKY
    }

    private fun updateNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(SERVICE_ID, Helper.createNotificationMessage(this, duration - seconds))
    }

    private fun timerCompleted() {
        stopSelf()
        sendBroadcast(
            Intent("TIMER_COMPLETE").putExtra("timer_complete", true).setPackage(packageName)
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(SERVICE_ID, Helper.createCompletedNotificationMessage(this))
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}