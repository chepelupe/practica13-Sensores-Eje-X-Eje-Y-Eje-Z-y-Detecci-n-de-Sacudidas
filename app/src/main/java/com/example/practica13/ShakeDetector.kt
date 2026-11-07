package com.example.practica13

import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detecta sacudidas comparando la fuerza de aceleración vectorial contra un umbral.
 *
 * Fuerza G = √(gx² + gy² + gz²), donde g = aceleración / gravedad terrestre.
 * En reposo la fuerza es ≈ 1 g (solo la gravedad); al sacudir supera el umbral.
 */
class ShakeDetector(
    thresholdG: Float = 2.5f,
    private val cooldownMs: Long = 1000L   // tiempo mínimo entre dos sacudidas
) {
    // Se escribe desde la UI y se lee desde el hilo del sensor
    @Volatile
    var thresholdG: Float = thresholdG

    private var lastShakeTimeMs = 0L

    /** Magnitud del vector de aceleración expresada en "g". */
    fun gForce(x: Float, y: Float, z: Float): Float {
        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH
        return sqrt(gX * gX + gY * gY + gZ * gZ)
    }

    /** Devuelve true si la fuerza supera el umbral y ya pasó el tiempo de espera. */
    fun isShake(gForce: Float, timestampMs: Long): Boolean {
        if (gForce < thresholdG) return false
        if (timestampMs - lastShakeTimeMs < cooldownMs) return false
        lastShakeTimeMs = timestampMs
        return true
    }
}