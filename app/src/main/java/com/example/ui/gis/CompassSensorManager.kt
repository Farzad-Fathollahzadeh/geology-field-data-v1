package com.example.ui.gis

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sensor data model representing real-time hardware compass and orientation values.
 */
data class CompassSensorData(
    val azimuthDegrees: Float = 0f,       // 0° = North, 90° = East, 180° = South, 270° = West
    val pitchDegrees: Float = 0f,         // Front/back tilt (-90° to +90°)
    val rollDegrees: Float = 0f,          // Left/right tilt (-180° to +180°)
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val isAvailable: Boolean = true,
    val sensorTypeName: String = "Rotation Vector Sensor"
)

/**
 * Angular smoothing filter that prevents boundary spin when crossing 0°/360°.
 */
private class AngularLowPassFilter(private val alpha: Float = 0.18f) {
    private var sinSum = 0f
    private var cosSum = 0f
    private var initialized = false

    fun filter(targetDeg: Float): Float {
        val rad = Math.toRadians(targetDeg.toDouble()).toFloat()
        val s = sin(rad)
        val c = cos(rad)

        if (!initialized) {
            sinSum = s
            cosSum = c
            initialized = true
        } else {
            sinSum += alpha * (s - sinSum)
            cosSum += alpha * (c - cosSum)
        }

        var deg = Math.toDegrees(atan2(sinSum.toDouble(), cosSum.toDouble())).toFloat()
        if (deg < 0f) deg += 360f
        return deg
    }
}

/**
 * Composable state hook to access high-accuracy real-time device digital compass and orientation sensors.
 */
@Composable
fun rememberCompassSensor(): State<CompassSensorData> {
    val context = LocalContext.current
    val compassState = remember { mutableStateOf(CompassSensorData()) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            compassState.value = CompassSensorData(isAvailable = false)
            return@DisposableEffect onDispose {}
        }

        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val azimuthFilter = AngularLowPassFilter(0.20f)
        var currentAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH

        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        val accelValues = FloatArray(3)
        val magValues = FloatArray(3)
        var hasAccel = false
        var hasMag = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)

                        var rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                        if (rawAzimuth < 0f) rawAzimuth += 360f

                        val smoothedAzimuth = azimuthFilter.filter(rawAzimuth)
                        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                        compassState.value = CompassSensorData(
                            azimuthDegrees = smoothedAzimuth,
                            pitchDegrees = pitch,
                            rollDegrees = roll,
                            accuracy = currentAccuracy,
                            isAvailable = true,
                            sensorTypeName = "6-DOF Rotation Vector (Fused)"
                        )
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, accelValues, 0, 3)
                        hasAccel = true
                        updateFallback()
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magValues, 0, 3)
                        hasMag = true
                        updateFallback()
                    }
                }
            }

            private fun updateFallback() {
                if (rotationVectorSensor != null) return // Prefer rotation vector if registered
                if (hasAccel && hasMag) {
                    val success = SensorManager.getRotationMatrix(rotationMatrix, null, accelValues, magValues)
                    if (success) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        var rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                        if (rawAzimuth < 0f) rawAzimuth += 360f

                        val smoothedAzimuth = azimuthFilter.filter(rawAzimuth)
                        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                        compassState.value = CompassSensorData(
                            azimuthDegrees = smoothedAzimuth,
                            pitchDegrees = pitch,
                            rollDegrees = roll,
                            accuracy = currentAccuracy,
                            isAvailable = true,
                            sensorTypeName = "Accel + Magnetometer"
                        )
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR || sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    currentAccuracy = accuracy
                }
            }
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            if (accelSensor != null) {
                sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            }
            if (magSensor != null) {
                sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
            }
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return compassState
}
