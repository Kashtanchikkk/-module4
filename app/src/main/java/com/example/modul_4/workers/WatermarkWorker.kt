package com.example.modul_4.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class WatermarkWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.Default) {
            try {
                val fileName = inputData.getString(Keys.KEY_FILE_NAME)
                    ?: return@withContext Result.failure(
                        workDataOf(Keys.KEY_ERROR to "Файл не найден")
                    )

                delay(4000)

                val watermarkedName = "watermarked_$fileName"
                Result.success(workDataOf(Keys.KEY_FILE_NAME to watermarkedName))
            } catch (e: Exception) {
                Result.failure(workDataOf(Keys.KEY_ERROR to e.message))
            }
        }
    }
}