package com.example.modul_4

import androidx.lifecycle.viewmodel.compose.viewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.modul_4.ui.theme.Modul_4Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Modul_4Theme() {
                CurrencyScreen()
            }
        }
    }
}

class CurrencyViewModel : ViewModel() {
    private val _rate = MutableStateFlow(90.50)
    val rate: StateFlow<Double> = _rate.asStateFlow()
    private val _previousRate = MutableStateFlow(90.50)
    val previousRate: StateFlow<Double> = _previousRate.asStateFlow()
    private val _lastUpdated = MutableStateFlow("")
    val lastUpdated: StateFlow<String> = _lastUpdated.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(5000L)
                updateRate()
            }
        }
    }

    fun refreshNow() {
        viewModelScope.launch {
            updateRate()
        }
    }

    private fun updateRate() {
        _previousRate.value = _rate.value
        val raw = 90.50 + Random.nextDouble(-2.0, 2.0)
        _rate.value = (raw * 100).toLong() / 100.0
        _lastUpdated.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
}

@Composable
fun CurrencyScreen(vm: CurrencyViewModel = viewModel()) {
    val rate by vm.rate.collectAsState()
    val lastUpdated by vm.lastUpdated.collectAsState()
    val previousRate by vm.previousRate.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFEFEF))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "USD в RUB",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedContent(
            targetState = rate,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                )) togetherWith fadeOut(animationSpec = tween(300))
            }

        ) { targetRate ->
            Text(
                text = "₽ $targetRate",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (targetRate > previousRate) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Обновлено в: $lastUpdated",
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { vm.refreshNow() }) {
            Text(text = "Обновить сейчас")
        }
    }
}