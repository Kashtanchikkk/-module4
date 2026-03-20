package com.example.modul_4

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay

const val KEY_CITY_NAME = "key_city_name"
const val KEY_TEMPERATURE = "key_temperature"
const val KEY_WEATHER_CONDITION = "key_weather_condition"

private const val WEATHER_CHANNEL_ID = "weather_channel"
private const val WEATHER_NOTIFICATION_ID = 101

private val CONDITIONS = listOf("ясно", "облачно", "дождь", "снег", "туман")

class WeatherWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cityName = inputData.getString(KEY_CITY_NAME) ?: "Неизвестный город"

        setForeground(createForegroundInfo("Загружаем погоду для $cityName..."))

        return try {
            delay((1500L..3000L).random())

            val temperature = (-10..35).random()
            val condition = CONDITIONS.random()

            Result.success(
                workDataOf(
                    KEY_CITY_NAME to cityName,
                    KEY_TEMPERATURE to temperature,
                    KEY_WEATHER_CONDITION to condition
                )
            )
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun createForegroundInfo(message: String): ForegroundInfo {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, WEATHER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle("Прогноз погоды")
            .setContentText(message)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return ForegroundInfo(
            WEATHER_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            WEATHER_CHANNEL_ID,
            "Прогноз погоды",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}