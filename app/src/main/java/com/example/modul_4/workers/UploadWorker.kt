package com.example.modul_4.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = inputData.getString(Keys.KEY_FILE_NAME)
                    ?: return@withContext Result.failure(
                        workDataOf(Keys.KEY_ERROR to "Файл не найден")
                    )

                delay(2000)

                val uploadedUrl = "https://cloud.example.com/photos/$fileName"
                Result.success(workDataOf(Keys.KEY_RESULT to uploadedUrl))
            } catch (e: Exception) {
                Result.failure(workDataOf(Keys.KEY_ERROR to e.message))
            }
        }
    }
}