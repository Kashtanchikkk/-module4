package com.example.modul_4

import com.example.modul_4.ui.theme.Modul_4Theme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Modul_4Theme() {
                FactsScreen()
            }
        }
    }
}

class RandomAnimalFactsViewModel() : ViewModel() {
    var isLoading by mutableStateOf(false)
    var currentFact by mutableStateOf<String?>(null)

    val facts = listOf(
        "Венерин пеньчатый кит: Этот морской обитатель - самое большое существо на Земле. Некоторые взрослые особи могут достигать длины до 30 метров и веса до 200 тонн, что делает их значительно больше даже динозавров.",
        "Креветка-мантис: У креветок-мантисов одни из самых быстрых и смертоносных ударов в животном мире. Они могут нанести удар скоростью до 23 м/с, что достаточно, чтобы разбить стекло аквариума.",
        "Большая Панда: Эти узнаваемые медведи способны есть до 14 часов в день, потребляя более 12 кг бамбука. Интересно, что их микробиота больше подходит для пищеварения мяса, но они адаптировались к растительному рациону.",
        "Гигантский кальмар: Это морское чудовище может достигать длины до 13 метров. Глаза гигантского кальмара – самые большие в животном мире, их диаметр может превышать 25 см.",
        "Слон: У слонов наиболее развитый мозг среди всех наземных животных. Они обладают удивительной памятью, способны учиться, понимать человеческую речь и даже испытывать чувства, схожие с человеческими.",
        "Медуза Турритопсис nutricula: Эта медуза по сути бессмертна. Она способна возвращать свои клетки в более молодую стадию, что позволяет ей избегать старения и смерти от старости.",
        "Щитоносный жук: Щитоносные жуки обладают одной из самых удивительных защитных способностей в животном мире - они могут выпускать горячую, кислотную жидкость на противников.",
        "Попугай Какаду: Некоторые виды попугаев Какаду обладают удивительно продолжительным сроком жизни и могут доживать до 100 лет.",
        "Сова: Совы могут вращать голову на 270 градусов без повреждения своих сосудов и лигаментов. Это помогает им наблюдать за окружающей средой без необходимости двигать тело.",
        "Мурена: Мурены одни из немногих видов рыб, которые имеют вторую пару челюстей в своем горле, которые используются для помощи в поглощении пищи."
    )

    fun getRandomFact(): Flow<String> = flow {
        val delayMs = (1500L..3000L).random()
        delay(delayMs)
        emit(facts.random())
    }

    fun fetchFact() {
        isLoading = true
        viewModelScope.launch {
            getRandomFact().collect { fact ->
                currentFact = fact
                isLoading = false
            }
        }
    }
}

@Composable
fun FactsScreen(vm: RandomAnimalFactsViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Случайные факты о животных",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1B1F)
        )
        Spacer(modifier = Modifier.height(24.dp))
        AnimalFactCard(
            fact = vm.currentFact,
            isLoading = vm.isLoading
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { vm.fetchFact() },
            enabled = !vm.isLoading,
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(48.dp)
        ) {
            Text(text = "Новый факт", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun AnimalFactCard(fact: String?, isLoading: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Ищем интересный факт...",
                            fontSize = 14.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }

                fact != null -> {
                    AnimatedContent(
                        targetState = fact,
                        transitionSpec = {
                            fadeIn(tween(400)) + slideInVertically { it / 2 } togetherWith
                                    fadeOut(tween(200))
                        },
                        label = "fact"
                    ) { currentFact ->
                        Text(
                            text = currentFact,
                            fontSize = 17.sp,
                            lineHeight = 26.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF1C1B1F)
                        )
                    }
                }

                else -> {
                    Text(
                        text = "Нажми кнопку, чтобы узнать факт!",
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF9E9E9E)
                    )
                }
            }
        }
    }
}