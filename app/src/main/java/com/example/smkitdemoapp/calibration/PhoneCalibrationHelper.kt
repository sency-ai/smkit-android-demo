package com.example.smkitdemoapp.calibration

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Reports whether the phone is held "upright" for assessment (like iOS "Tilt your phone upright").
 * Uses accelerometer: in portrait, upright = gravity mostly along device Y axis (abs(y) dominant).
 */
class PhoneCalibrationHelper(private val sensorManager: SensorManager) : SensorEventListener {

    private val _isPhoneReady = MutableLiveData(false)
    val isPhoneReady: LiveData<Boolean> = _isPhoneReady

    private var registered = false
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun start() {
        if (registered || accelerometer == null) return
        val ok = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        registered = ok
    }

    fun stop() {
        if (!registered) return
        sensorManager.unregisterListener(this)
        registered = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER || event.values.size < 3) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        // Upright portrait: gravity along -Y, so |y| ~ 9.8 and |x|,|z| small.
        val magnitude = sqrt(x * x + y * y + z * z)
        if (magnitude < 5f) return
        val absY = abs(y)
        val inRange = absY >= 7f
        if (_isPhoneReady.value != inRange) _isPhoneReady.postValue(inRange)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
