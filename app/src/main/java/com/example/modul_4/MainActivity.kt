package com.example.modul_4

import com.example.modul_4.ui.theme.Modul_4Theme
import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Modul_4Theme() {
                CompassApplication()
            }
        }
    }
}

@Composable
fun CompassApplication() {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val magnetometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }
    val sensorsAvailable = accelerometer != null && magnetometer != null

    var azimuth by rememberSaveable { mutableFloatStateOf(0f) }

    val gravity = remember { FloatArray(3) }
    val geomagnetic = remember { FloatArray(3) }
    var hasGravity by remember { mutableStateOf(false) }
    var hasMagnetic by remember { mutableStateOf(false) }

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val alpha = 0.8f
                        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        val alpha = 0.8f
                        geomagnetic[0] = alpha * geomagnetic[0] + (1 - alpha) * event.values[0]
                        geomagnetic[1] = alpha * geomagnetic[1] + (1 - alpha) * event.values[1]
                        geomagnetic[2] = alpha * geomagnetic[2] + (1 - alpha) * event.values[2]
                        hasMagnetic = true
                    }
                }

                if (hasGravity && hasMagnetic) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    val success = SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)
                    if (success) {
                        val orientation = FloatArray(3)
                        val rRemapped = FloatArray(9)
                        val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
                            .defaultDisplay.rotation
                        val (axisX, axisY) = when (rotation) {
                            android.view.Surface.ROTATION_90  -> Pair(SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X)
                            android.view.Surface.ROTATION_180 -> Pair(SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y)
                            android.view.Surface.ROTATION_270 -> Pair(SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X)
                            else -> Pair(SensorManager.AXIS_X, SensorManager.AXIS_Y)
                        }
                        SensorManager.remapCoordinateSystem(r, axisX, axisY, rRemapped)
                        SensorManager.getOrientation(rRemapped, orientation)

                        val newAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        azimuth = (newAzimuth + 360f) % 360f
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(sensorsAvailable) {
        if (sensorsAvailable) {
            sensorManager.registerListener(
                sensorListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
            sensorManager.registerListener(
                sensorListener,
                magnetometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    CompassScreen(
        azimuth = azimuth,
        sensorsAvailable = sensorsAvailable
    )
}

@Composable
fun CompassScreen(azimuth: Float, sensorsAvailable: Boolean) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var currentAngle by remember { mutableFloatStateOf(azimuth) }

    LaunchedEffect(azimuth) {
        var delta = (azimuth - currentAngle) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        currentAngle += delta
    }

    val animatedAzimuth by animateFloatAsState(
        targetValue = currentAngle,
        animationSpec = tween(durationMillis = 300),
        label = "azimuth_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        if (!sensorsAvailable) {
            Text(
                text = "Устройство не поддерживает датчик ориентации",
                color = Color.Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        } else {
            if (isLandscape) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    BoxWithConstraints(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxHeight(0.85f)
                            .aspectRatio(1f)
                    ) {
                        val compassSize = maxWidth
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCompass(animatedAzimuth)
                        }
                        Text(
                            text = "N",
                            color = Color(0xFFE53935),
                            fontSize = (compassSize.value * 0.12f).sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = (compassSize.value * 0.06f).dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Компас",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Азимут: ${azimuth.toInt()}°",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Компас",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 40.dp)
                    )

                    BoxWithConstraints(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .aspectRatio(1f)
                    ) {
                        val compassSize = maxWidth
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCompass(animatedAzimuth)
                        }
                        Text(
                            text = "N",
                            color = Color(0xFFE53935),
                            fontSize = (compassSize.value * 0.12f).sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = (compassSize.value * 0.06f).dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Азимут: ${azimuth.toInt()}°",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

fun DrawScope.drawCompass(azimuth: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = size.minDimension / 2f

    drawCircle(
        color = Color(0xFF2C2C2C),
        radius = radius,
        center = Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
    )

    drawCircle(
        color = Color(0xFF141414),
        radius = radius - 2.dp.toPx(),
        center = Offset(cx, cy)
    )

    rotate(degrees = azimuth, pivot = Offset(cx, cy)) {
        val arrowLength = radius * 0.72f
        val arrowWidth = radius * 0.06f

        drawLine(
            color = Color(0xFFE53935),
            start = Offset(cx, cy),
            end = Offset(cx, cy - arrowLength),
            strokeWidth = arrowWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color(0xFF9E9E9E),
            start = Offset(cx, cy),
            end = Offset(cx, cy + arrowLength),
            strokeWidth = arrowWidth,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = arrowWidth,
            center = Offset(cx, cy)
        )
    }
}