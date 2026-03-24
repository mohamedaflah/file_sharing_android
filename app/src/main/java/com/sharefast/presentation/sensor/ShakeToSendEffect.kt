package com.sharefast.presentation.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Detects a firm shake and fires [onShake] at most once per [cooldownMs].
 * Fully offline; uses the accelerometer only.
 */
@Composable
fun ShakeToSendEffect(
    enabled: Boolean,
    cooldownMs: Long = 2_800L,
    onShake: () -> Unit,
) {
    val context = LocalContext.current
    DisposableEffect(enabled, context) {
        if (!enabled) {
            return@DisposableEffect onDispose { }
        }
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return@DisposableEffect onDispose { }
        var lastFire = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val g = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta = kotlin.math.abs(g - SensorManager.GRAVITY_EARTH)
                val now = SystemClock.elapsedRealtime()
                if (delta > 14f && now - lastFire > cooldownMs) {
                    lastFire = now
                    onShake()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
}
