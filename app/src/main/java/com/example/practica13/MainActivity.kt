package com.example.practica13

import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.practica13.ui.theme.Practica13Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Obtenemos el servicio nativo de sensores de Android
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        enableEdgeToEdge()
        setContent {
            Practica13Theme {
                BurbujaScreen(sensorManager = sensorManager)
            }
        }
    }
}