package com.example.modul_4

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.modul_4.ui.theme.Modul_4Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import com.google.gson.Gson


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Modul_4Theme {

                }
            }
        loadAllData()
        }
    fun loadAllData(){
        val task = Task1(this)
        CoroutineScope(Dispatchers.IO).launch {

            val time = measureTimeMillis {

                runBlocking {

                    val usersDeferred = async {
                        try {
                            task.loadUsers()
                        } catch (e: Exception) {
                            Log.e("TASK1", "Users error: ${e.message}")
                            emptyList()
                        }
                    }

                    val salesDeferred = async {
                        try {
                            task.loadSales()
                        } catch (e: Exception) {
                            Log.e("TASK1", "Sales error: ${e.message}")
                            emptyMap()
                        }
                    }

                    val weatherDeferred = async {
                        try {
                            task.loadWeather()
                        } catch (e: Exception) {
                            Log.e("TASK1", "Weather error: ${e.message}")
                            emptyList()
                        }
                    }

                    val users = usersDeferred.await()
                    val sales = salesDeferred.await()
                    val weather = weatherDeferred.await()

                    Log.d("TASK1", "Users: $users")
                    Log.d("TASK1", "Sales: $sales")
                    Log.d("TASK1", "Weather: $weather")
                }
            }

            Log.d("TASK1", "Общее время: $time ms")
        }
    }
    class Task1(private val context: Context) {

        private val gson = Gson()

        suspend fun loadUsers(): List<String> {
            delay(1800L)

            if(Random.nextInt(10) < 3){
                throw RuntimeException("Ошибка загрузки пользователей")
            }

            val json = context.assets.open("users.json")
                .bufferedReader()
                .use { it.readText() }

            val users = gson.fromJson(json, Array<User>::class.java)

            return users.map { it.name }
        }

        suspend fun loadSales(): Map<String, Int> {
            delay(1200)

            if(Random.nextInt(10) < 3){
                throw RuntimeException("Ошибка загрузки продаж")
            }

            val json = context.assets.open("sales.json")
                .bufferedReader()
                .use { it.readText() }

            val report = gson.fromJson(json, Sale::class.java)

            return report.items.associate { it.product to it.revenue }
        }

        suspend fun loadWeather(): List<String> {
            delay(2500)

            if (Random.nextInt(10) < 3) {
                throw RuntimeException("Ошибка загрузки погоды")
            }

            val json = context.assets.open("weather.json")
                .bufferedReader()
                .use { it.readText() }

            val weather = gson.fromJson(json, Array<Weather>::class.java)

            return weather.map { "${it.city}: ${it.temp}°C" }
        }

    }

    data class SalesItem(
        val product: String,
        val qty: Int,
        val revenue: Int
    )

    data class Sale(
        val today: String,
        val items: List<SalesItem>
    )

    data class User(
        val id: Int,
        val name: String
    )

    data class Weather(
        val city: String,
        val temp: Int,
        val condition: String
    )

}


