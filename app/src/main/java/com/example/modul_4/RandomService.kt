package com.example.modul_4

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RandomService : Service() {
    private var random_number = 0
    private var job: Job? = null
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private val binder = RandomBinder()

    var onNewNumber: ((Int) -> Unit)? = null

    inner class RandomBinder : Binder() {
        fun getService(): RandomService = this@RandomService
    }


    private fun startGeneration() {
        job = scope.launch {
            while (true) {
                random_number = (0..100).random()
                Log.d("RandomService", "Generated number: $random_number")
                onNewNumber?.invoke(random_number)
                delay(1000)
            }
        }
    }

    private fun stopGeneration() {
        job?.cancel()
    }

    override fun onBind(intent: Intent): IBinder {
        startGeneration()
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopGeneration()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}