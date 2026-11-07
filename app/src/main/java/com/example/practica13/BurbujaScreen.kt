package com.example.practica13

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val ColorFondo = Color(0xFF263238)
private val ColorSacudida = Color(0xFFB71C1C)
private val ColorEjeX = Color(0xFFEF5350)
private val ColorEjeY = Color(0xFF66BB6A)
private val ColorEjeZ = Color(0xFF42A5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurbujaScreen(sensorManager: SensorManager) {
    // Lecturas del acelerómetro (m/s²)
    var ejeX by remember { mutableFloatStateOf(0f) }
    var ejeY by remember { mutableFloatStateOf(0f) }
    var ejeZ by remember { mutableFloatStateOf(0f) }
    var fuerzaG by remember { mutableFloatStateOf(1f) }

    // Estado de la sacudida
    var sacudidas by remember { mutableIntStateOf(0) }
    var sacudidaActiva by remember { mutableStateOf(false) }
    var umbral by remember { mutableFloatStateOf(2.5f) }

    val detector = remember { ShakeDetector() }
    val acelerometro = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    // El umbral elegido en el slider se pasa al detector
    LaunchedEffect(umbral) { detector.thresholdG = umbral }

    // Enciende el sensor al abrir la pantalla y lo apaga al cerrarla.
    // Las lecturas llegan en un HandlerThread propio, NO en el hilo de la UI.
    DisposableEffect(sensorManager) {
        if (acelerometro == null) return@DisposableEffect onDispose { }

        val hiloSensor = HandlerThread("HiloAcelerometro").apply { start() }
        val handler = Handler(hiloSensor.looper)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val e = event ?: return
                val x = e.values[0]
                val y = e.values[1]
                val z = e.values[2]

                // El estado de Compose se puede escribir desde otro hilo:
                // la UI se recompone sola con los nuevos valores.
                ejeX = x
                ejeY = y
                ejeZ = z

                val g = detector.gForce(x, y, z)
                fuerzaG = g
                if (detector.isShake(g, e.timestamp / 1_000_000)) {
                    sacudidas++
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            acelerometro,
            SensorManager.SENSOR_DELAY_GAME,
            handler
        )

        onDispose {
            sensorManager.unregisterListener(listener)
            hiloSensor.quitSafely()
        }
    }

    // Respuesta visual: fondo rojo + temblor de la burbuja + mensaje
    val temblor = remember { Animatable(0f) }
    LaunchedEffect(sacudidas) {
        if (sacudidas == 0) return@LaunchedEffect
        sacudidaActiva = true
        repeat(6) { i ->
            temblor.animateTo(if (i % 2 == 0) 25f else -25f, tween(durationMillis = 50))
        }
        temblor.animateTo(0f, tween(durationMillis = 50))
        delay(800)
        sacudidaActiva = false
    }
    val colorFondo by animateColorAsState(
        targetValue = if (sacudidaActiva) ColorSacudida else ColorFondo,
        animationSpec = tween(durationMillis = 250),
        label = "fondo"
    )

    // Posición de la burbuja: inclinación completa (9.81 m/s²) = orilla del círculo
    val density = LocalDensity.current
    val radioMaximoPx = with(density) { (125.dp - 20.dp).toPx() }
    val sensibilidad = radioMaximoPx / SensorManager.GRAVITY_EARTH
    var offsetX = -ejeX * sensibilidad
    var offsetY = ejeY * sensibilidad
    val distancia = sqrt(offsetX * offsetX + offsetY * offsetY)
    if (distancia > radioMaximoPx) {
        val proporcion = radioMaximoPx / distancia
        offsetX *= proporcion
        offsetY *= proporcion
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nivel de Burbuja y Sacudidas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorFondo)
                .padding(paddingValues)
        ) {
            if (acelerometro == null) {
                Text(
                    "Este dispositivo no tiene acelerómetro.",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
                return@Box
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Valores de los tres ejes en tiempo real
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EjeCard("Eje X", ejeX, ColorEjeX, Modifier.weight(1f))
                    EjeCard("Eje Y", ejeY, ColorEjeY, Modifier.weight(1f))
                    EjeCard("Eje Z", ejeZ, ColorEjeZ, Modifier.weight(1f))
                }

                // Nivel de burbuja (base de la práctica)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(temblor.value.roundToInt(), 0) }
                        .size(250.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .background(Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(50.dp).border(2.dp, Color.Red, CircleShape))
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .size(40.dp)
                            .background(Color(0xFF00FF00), CircleShape)
                    )
                }

                // Fuerza vectorial y umbral
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Fuerza de aceleración: %.2f g".format(fuerzaG),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (fuerzaG / 4f).coerceIn(0f, 1f) },
                            color = if (fuerzaG >= umbral) ColorSacudida else ColorEjeY,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Umbral de sacudida: %.1f g".format(umbral),
                            color = Color.White
                        )
                        Slider(
                            value = umbral,
                            onValueChange = { umbral = (it * 10).roundToInt() / 10f },
                            valueRange = 1.3f..4f
                        )
                        Text(
                            "Sacudidas detectadas: $sacudidas",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Mensaje flotante al detectar la sacudida
            AnimatedVisibility(
                visible = sacudidaActiva,
                enter = scaleIn() + fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        "📳 ¡Sacudida detectada!",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        color = ColorSacudida,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/** Tarjeta que muestra el valor de un eje y una barra proporcional. */
@Composable
private fun EjeCard(nombre: String, valor: Float, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.35f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(nombre, color = color, fontWeight = FontWeight.Bold)
            Text(
                "%+.2f".format(valor),
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace
            )
            Text("m/s²", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (abs(valor) / SensorManager.GRAVITY_EARTH).coerceIn(0f, 1f) },
                color = color,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}