package com.example.modul_4

import android.os.Bundle
import com.example.modul_4.ui.theme.Modul_4Theme
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf

private val CITIES = listOf("Москва", "Лондон", "Нью-Йорк")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        enableEdgeToEdge()
        setContent {
            Modul_4Theme() {
                WeatherScreen()
            }
        }
    }
}

@Composable
fun WeatherScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)

    val weatherInfoList by workManager.getWorkInfosByTagLiveData("weather_tag")
        .observeAsState(emptyList())
    val reportInfoList by workManager.getWorkInfosByTagLiveData("report_tag")
        .observeAsState(emptyList())

    val reportInfo = reportInfoList.firstOrNull()

    val completedCount = weatherInfoList.count { it.state == WorkInfo.State.SUCCEEDED }
    val totalCities = CITIES.size

    val isAnyRunning = weatherInfoList.any {
        it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
    }
    val isReportRunning =
        reportInfo?.state == WorkInfo.State.RUNNING || reportInfo?.state == WorkInfo.State.ENQUEUED
    val isAllDone = reportInfo?.state == WorkInfo.State.SUCCEEDED
    val isInProgress = isAnyRunning || isReportRunning

    val statusText = when {
        isAllDone -> "Все данные получены!"
        isReportRunning -> "Формируем отчёт..."
        isAnyRunning && completedCount == 0 -> "Загрузка... ($totalCities в процессе)"
        isAnyRunning -> "Загрузка... ($completedCount из $totalCities готово)"
        else -> "Готов начать"
    }

    val cityStatusMap = buildCityStatusMap(weatherInfoList, CITIES)

    val reportText = if (isAllDone) buildReportText(reportInfo, weatherInfoList, CITIES) else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(60.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = statusText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (isAllDone) Color(0xFF1976D2) else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        CITIES.forEach { city ->
            CityWeatherCard(
                cityName = city,
                status = cityStatusMap[city],
                isRunning = isInProgress && cityStatusMap[city] == null
            )
            Spacer(Modifier.height(8.dp))
        }

        if (reportText != null) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = reportText,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isInProgress) {
            Button(
                onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()
            ) { Text("В процессе...") }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    workManager.cancelAllWorkByTag("weather_tag")
                    workManager.cancelAllWorkByTag("report_tag")
                }, modifier = Modifier.fillMaxWidth()
            ) { Text("Отменить") }
        } else {
            Button(
                onClick = { startWeatherParallel(workManager) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) { Text("Собрать прогноз") }
        }

        Spacer(Modifier.height(16.dp))
    }
}

data class CityWeatherStatus(
    val temperature: Int, val condition: String
)

@Composable
fun CityWeatherCard(
    cityName: String, status: CityWeatherStatus?, isRunning: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = cityName, fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
                Text(
                    text = when {
                        status != null -> "Готово"
                        isRunning -> "Загружается..."
                        else -> "Ожидание"
                    }, fontSize = 13.sp, color = when {
                        status != null -> Color(0xFF1976D2)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            when {
                status != null -> {
                    Text(
                        text = "${status.temperature}°C",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                isRunning -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp), strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

private fun buildCityStatusMap(
    workInfoList: List<WorkInfo>, cities: List<String>
): Map<String, CityWeatherStatus> {
    val result = mutableMapOf<String, CityWeatherStatus>()
    workInfoList.forEachIndexed { index, workInfo ->
        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
            val cityName = cities.getOrNull(index) ?: return@forEachIndexed
            val temp = workInfo.outputData.getInt(KEY_TEMPERATURE, 0)
            val condition = workInfo.outputData.getString(KEY_WEATHER_CONDITION) ?: "ясно"
            result[cityName] = CityWeatherStatus(temp, condition)
        }
    }
    return result
}

private fun buildReportText(
    reportInfo: WorkInfo?, weatherInfoList: List<WorkInfo>, cities: List<String>
): String {
    val cityLines = weatherInfoList.mapIndexed { index, workInfo ->
        val city = cities.getOrNull(index) ?: ""
        val temp = workInfo.outputData.getInt(KEY_TEMPERATURE, 0)
        val condition = workInfo.outputData.getString(KEY_WEATHER_CONDITION) ?: "ясно"
        "$city: ${temp}°C, $condition"
    }.joinToString("\n")

    val avgTemp = reportInfo?.outputData?.getInt(KEY_TEMPERATURE, 0) ?: 0
    return "Итоговый прогноз:\n$cityLines\n\nСредняя температура: ${avgTemp}°C"
}

private fun startWeatherParallel(workManager: WorkManager) {
    val weatherRequests = CITIES.map { city ->
        OneTimeWorkRequestBuilder<WeatherWorker>().setInputData(workDataOf(KEY_CITY_NAME to city))
            .addTag("weather_tag").build()
    }

    val avgTemp = (-5..20).random()
    val reportRequest =
        OneTimeWorkRequestBuilder<WeatherReportWorker>().setInputData(workDataOf(KEY_TEMPERATURE to avgTemp))
            .addTag("report_tag").build()

    workManager.beginWith(weatherRequests).then(reportRequest).enqueue()
}