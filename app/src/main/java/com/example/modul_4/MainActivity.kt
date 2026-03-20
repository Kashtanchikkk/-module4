package com.example.modul_4

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.modul_4.ui.theme.Modul_4Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            Modul_4Theme() {
                RandomScreen()
            }
        }
    }
}

@Composable
fun RandomScreen() {
    val context = LocalContext.current
    var number by remember { mutableIntStateOf(-1) }
    var isServiceConnected by remember { mutableStateOf(false) }
    var randomService by remember { mutableStateOf<RandomService?>(null) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                randomService = (service as RandomService.RandomBinder).getService()
                randomService?.onNewNumber = { number = it }
                isServiceConnected = true
            }
            override fun onServiceDisconnected(name: ComponentName) {
                isServiceConnected = false
                randomService = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Случайное число: ${if (!isServiceConnected) "-" else number}",
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = {
                if (isServiceConnected) {
                    context.unbindService(connection)
                    isServiceConnected = false
                } else {
                    Intent(context, RandomService::class.java).also { intent ->
                        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isServiceConnected) {
                Text("Отключиться")
            } else {
                Text("Подключиться")
            }
        }
    }
}