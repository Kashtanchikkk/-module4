package com.example.modul_4

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBarDefaults.InputField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.modul_4.ui.theme.Modul_4Theme
import kotlin.concurrent.timer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        enableEdgeToEdge()
        setContent {
            Modul_4Theme() {
                TimerScreen()
            }
        }
    }
}

@Composable
fun TimerScreen() {
    val context = LocalContext.current
    var seconds by remember { mutableIntStateOf(0) }
    var isStartButtonEnabled by remember { mutableStateOf(true) }
    val timerService = remember { Intent(context, TimerService::class.java) }
    var timerComplete by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                timerComplete = intent.getBooleanExtra("timer_complete", false)
                if (timerComplete) {
                    isStartButtonEnabled = true
                }
            }
        }
        ContextCompat.registerReceiver(
            context, receiver,
            IntentFilter("TIMER_COMPLETE"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = seconds.toString(),
            onValueChange = { newValue ->
                seconds = newValue.toIntOrNull() ?: 0
                timerService.putExtra("duration", seconds)
            },
            label = { Text("Введите количество секунд") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "секунд", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = {
                context.startForegroundService(timerService)
                isStartButtonEnabled = false
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isStartButtonEnabled
        ) {
            Text("Старт")
        }
    }
}