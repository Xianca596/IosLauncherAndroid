package com.example.ioslauncheronandroid.ui.glass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import com.example.ioslauncheronandroid.data.LauncherSettings
import kotlin.math.abs

object Tilt {
    var x by mutableFloatStateOf(0f)
    var y by mutableFloatStateOf(0f)
    val dx: Float get() = if (LauncherSettings.parallax) x else 0f
    val dy: Float get() = if (LauncherSettings.parallax) y else 0f
}

class TiltSensor(context: Context) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val rot = FloatArray(9)
    private val ori = FloatArray(3)
    private var fastX = 0f; private var fastY = 0f
    private var baseX = 0f; private var baseY = 0f
    private var primed = false

    fun start() {
        sensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sm.unregisterListener(this)
        primed = false
        Tilt.x = 0f; Tilt.y = 0f
    }

    override fun onSensorChanged(e: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rot, e.values)
        SensorManager.getOrientation(rot, ori)
        val rawY = ori[1]
        val rawX = ori[2]
        if (!primed) { fastX = rawX; fastY = rawY; baseX = rawX; baseY = rawY; primed = true }
        fastX += 0.12f * (rawX - fastX); fastY += 0.12f * (rawY - fastY)
        baseX += 0.006f * (rawX - baseX); baseY += 0.006f * (rawY - baseY)
        val nx = ((fastX - baseX) / 0.45f).coerceIn(-1f, 1f)
        val ny = ((fastY - baseY) / 0.45f).coerceIn(-1f, 1f)
        if (abs(nx - Tilt.x) > 0.02f) Tilt.x = nx
        if (abs(ny - Tilt.y) > 0.02f) Tilt.y = ny
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
