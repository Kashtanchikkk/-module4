package com.example.modul_4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.modul_4.ui.theme.Modul_4Theme
import com.example.modul_4.workers.CompressWorker
import com.example.modul_4.workers.Keys
import com.example.modul_4.workers.UploadWorker
import com.example.modul_4.workers.WatermarkWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Modul_4Theme() {
                PhotoProcessingScreen()
            }
        }
    }
}

@Composable
fun PhotoProcessingScreen() {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }

    val uploadInfo by workManager
        .getWorkInfosByTagLiveData("upload_tag").observeAsState()
    val compressInfo by workManager
        .getWorkInfosByTagLiveData("compress_tag").observeAsState()
    val watermarkInfo by workManager
        .getWorkInfosByTagLiveData("watermark_tag").observeAsState()

    val compressState = compressInfo?.firstOrNull()?.state
    val watermarkState = watermarkInfo?.firstOrNull()?.state
    val uploadState = uploadInfo?.firstOrNull()

    val statusText = when {
        compressState == WorkInfo.State.RUNNING -> "Сжимаем фото..."
        watermarkState == WorkInfo.State.RUNNING -> "Добавляем водяной знак..."
        uploadState?.state == WorkInfo.State.RUNNING -> "Загружаем в облако..."
        uploadState?.state == WorkInfo.State.SUCCEEDED -> {
            val path = uploadState.outputData.getString(Keys.KEY_RESULT) ?: ""
            "Готово! Фото загружено\n$path"
        }

        uploadState?.state == WorkInfo.State.FAILED ||
                compressState == WorkInfo.State.FAILED -> "Ошибка! Выполнение прервано."

        else -> "Нажмите кнопку для обработки фото"
    }

    val isRunning = listOf(compressState, watermarkState, uploadState?.state).any {
        it == WorkInfo.State.RUNNING || it == WorkInfo.State.ENQUEUED
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            statusText,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = when (uploadState?.state) {
                WorkInfo.State.SUCCEEDED -> Color.Blue
                WorkInfo.State.FAILED -> Color.Red
                else -> MaterialTheme.colorScheme.onSurface
            }
        )

        Spacer(Modifier.height(24.dp))

        if (isRunning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { startProcessing(workManager, "photo.png") },
            enabled = !isRunning
        ) {
            Text("Начать обработку и загрузку")
        }
    }
}

private fun startProcessing(workManager: WorkManager, fileName: String) {
    val inputData = workDataOf(Keys.KEY_FILE_NAME to fileName)

    val compressRequest = OneTimeWorkRequestBuilder<CompressWorker>()
        .setInputData(inputData)
        .addTag("compress_tag")
        .build()

    val watermarkRequest = OneTimeWorkRequestBuilder<WatermarkWorker>()
        .addTag("watermark_tag")
        .build()

    val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
        .addTag("upload_tag")
        .build()

    workManager.beginUniqueWork(
        "photo_processing",
        ExistingWorkPolicy.REPLACE,
        compressRequest
    ).then(watermarkRequest)
        .then(uploadRequest)
        .enqueue()
}