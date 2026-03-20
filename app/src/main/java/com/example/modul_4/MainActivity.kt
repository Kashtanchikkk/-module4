package com.example.modul_4

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.modul_4.ui.theme.Modul_4Theme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Locale


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Modul_4Theme() {
                LocationScreen()
            }
        }
    }
}

sealed class LocationState {
    object Idle : LocationState()
    object Loading : LocationState()
    data class Success(val address: String, val lat: Double, val lng: Double) : LocationState()
    data class Error(val message: String) : LocationState()
}


@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
fun LocationScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<LocationState>(LocationState.Idle) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch { state = fetchAddress(context) }
        } else {
            state = LocationState.Error("Разрешение на геолокацию отклонено")
        }
    }

    fun onGetAddress() {
        val fine =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            scope.launch { state = fetchAddress(context) }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val s = state) {
            is LocationState.Idle -> {
                Text(
                    text = "Нажмите кнопку",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            is LocationState.Loading -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Определяем местоположение…",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            is LocationState.Success -> {
                Text(
                    text = s.address,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Lat: ${"%.6f".format(s.lat)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Lng: ${"%.6f".format(s.lng)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            is LocationState.Error -> {
                Text(
                    text = " ${s.message}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = { onGetAddress() },
            enabled = state !is LocationState.Loading
        ) {
            Text("Получить мой адрес", fontSize = 16.sp)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
private suspend fun fetchAddress(context: Context): LocationState {
    return try {
        val client = LocationServices.getFusedLocationProviderClient(context)

        val location = client
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .await()

        if (location == null) {
            return LocationState.Error("Не удалось получить координаты.\nПроверьте GPS и интернет.")
        }

        val lat = location.latitude
        val lng = location.longitude
        val address = reverseGeocode(context, lat, lng)

        LocationState.Success(address = address, lat = lat, lng = lng)

    } catch (e: Exception) {
        LocationState.Error("Ошибка: ${e.localizedMessage}")
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
    val geocoder = Geocoder(context, Locale.getDefault())

    return suspendCancellableCoroutine { cont ->
        geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<android.location.Address>) {
                val address = addresses.firstOrNull()
                cont.resume(address?.formatAddress() ?: "Адрес не найден") { cause, _, _ -> null?.let { it(cause) } }
            }

            override fun onError(errorMessage: String?) {
                cont.resume("Ошибка геокодирования: $errorMessage") { cause, _, _ -> null?.let { it(cause) } }
            }
        })
    }
}

private fun android.location.Address.formatAddress(): String {
    val parts = mutableListOf<String>()
    for (i in 0..maxAddressLineIndex) {
        getAddressLine(i)?.let { parts.add(it) }
    }
    return parts.joinToString(", ")
}