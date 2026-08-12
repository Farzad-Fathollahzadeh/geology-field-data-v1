package com.example.ui.gis

import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.UtmConverter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Enhanced Digital Compass Widget for field orientation.
 * Supports hardware device sensors or simulated heading input.
 */
@Composable
fun CompassWidget(
    heading: Double,
    modifier: Modifier = Modifier,
    bearing: Double? = null,
    isOutdoorMode: Boolean = false,
    useDeviceSensor: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    // Read hardware sensors if enabled
    val sensorData by rememberCompassSensor()
    val activeHeading = if (useDeviceSensor && sensorData.isAvailable) {
        sensorData.azimuthDegrees.toDouble()
    } else {
        heading
    }

    val animatedHeading by animateFloatAsState(targetValue = activeHeading.toFloat(), label = "CompassHeading")

    val textCol = if (isOutdoorMode) Color.Black else Color.White
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF00FFE0)
    val panelBg = if (isOutdoorMode) Color.White.copy(alpha = 0.95f) else Color(0xDD0A100E)
    val circleOutline = textCol.copy(alpha = 0.18f)

    Box(
        modifier = modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(panelBg)
            .border(1.5.dp, if (sensorData.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) accentColor.copy(alpha = 0.6f) else circleOutline, CircleShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .rotate(-animatedHeading) // Orient dial to point North up
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Draw circular tick marks
            for (angle in 0 until 360 step 15) {
                val angleRad = angle * PI / 180.0
                val startLen = radius - (if (angle % 90 == 0) 10.dp.toPx() else 6.dp.toPx())
                val startX = center.x + startLen * cos(angleRad)
                val startY = center.y + startLen * sin(angleRad)
                val endX = center.x + radius * cos(angleRad)
                val endY = center.y + radius * sin(angleRad)

                drawLine(
                    color = textCol.copy(alpha = if (angle % 90 == 0) 0.7f else 0.35f),
                    start = Offset(startX.toFloat(), startY.toFloat()),
                    end = Offset(endX.toFloat(), endY.toFloat()),
                    strokeWidth = if (angle % 90 == 0) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            // Draw directional pointer (North needle)
            val pathNorth = Path().apply {
                moveTo(center.x, center.y - radius + 12.dp.toPx())
                lineTo(center.x - 7.dp.toPx(), center.y)
                lineTo(center.x + 7.dp.toPx(), center.y)
                close()
            }
            drawPath(path = pathNorth, color = Color(0xFFFF3B30))

            // South needle
            val pathSouth = Path().apply {
                moveTo(center.x, center.y + radius - 12.dp.toPx())
                lineTo(center.x - 5.dp.toPx(), center.y)
                lineTo(center.x + 5.dp.toPx(), center.y)
                close()
            }
            drawPath(path = pathSouth, color = textCol.copy(alpha = 0.5f))

            // Indicator course bearing overlay
            bearing?.let { targetBearing ->
                rotate(targetBearing.toFloat(), pivot = center) {
                    val bearingRadius = radius - 15.dp.toPx()
                    val targetX = center.x + bearingRadius * sin(0.0).toFloat()
                    val targetY = center.y - bearingRadius * cos(0.0).toFloat()

                    drawCircle(
                        color = accentColor,
                        radius = 4.dp.toPx(),
                        center = Offset(targetX, targetY)
                    )
                }
            }
        }

        // Center overlay showing digital degrees & cardinal direction
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${activeHeading.toInt()}°",
                fontSize = 12.sp,
                color = textCol,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = getCardinalStr(activeHeading),
                fontSize = 8.sp,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Full Field Digital Compass Card displaying orientation heading alongside real-time GPS/UTM coordinates.
 */
@Composable
fun FieldDigitalCompassCard(
    latitude: Double,
    longitude: Double,
    elevation: Double,
    accuracy: Double,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isOutdoorMode: Boolean = false
) {
    val sensorData by rememberCompassSensor()
    val clipboardManager = LocalClipboardManager.current

    val heading = sensorData.azimuthDegrees.toDouble()
    val pitch = sensorData.pitchDegrees
    val roll = sensorData.rollDegrees

    val textCol = if (isOutdoorMode) Color.Black else Color.White
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF00FFE0)
    val panelBg = if (isOutdoorMode) Color(0xFFF7FAF8) else Color(0xFF101614)
    val cardBg = if (isOutdoorMode) Color.White else Color(0xFF19221E)

    // Calculate UTM and Grid Convergence
    val utmCoord = remember(latitude, longitude) {
        try {
            UtmConverter.convertLatLngToUtm(latitude, longitude)
        } catch (e: Exception) {
            null
        }
    }

    val gridConvergence = remember(latitude, longitude) {
        try {
            UtmConverter.calculateGridConvergence(latitude, longitude)
        } catch (e: Exception) {
            0.0
        }
    }

    val animatedHeading by animateFloatAsState(targetValue = heading.toFloat(), label = "HeadingAnim")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        color = panelBg,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CompassCalibration,
                        contentDescription = "Digital Compass",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "FIELD DIGITAL COMPASS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Real-time Orientation & Coordinates",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textCol
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textCol.copy(alpha = 0.7f)
                    )
                }
            }

            // Main Digital Compass Dial + Pitch/Roll Attitude Bubble Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Large Circular Compass Rose
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(cardBg)
                        .border(2.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .rotate(-animatedHeading)
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2

                        // Ticks
                        for (angle in 0 until 360 step 15) {
                            val angleRad = angle * PI / 180.0
                            val startLen = radius - (if (angle % 90 == 0) 12.dp.toPx() else 6.dp.toPx())
                            val startX = center.x + startLen * cos(angleRad)
                            val startY = center.y + startLen * sin(angleRad)
                            val endX = center.x + radius * cos(angleRad)
                            val endY = center.y + radius * sin(angleRad)

                            drawLine(
                                color = textCol.copy(alpha = if (angle % 90 == 0) 0.8f else 0.4f),
                                start = Offset(startX.toFloat(), startY.toFloat()),
                                end = Offset(endX.toFloat(), endY.toFloat()),
                                strokeWidth = if (angle % 90 == 0) 2.5.dp.toPx() else 1.dp.toPx()
                            )
                        }

                        // Pointer (North)
                        val pathNorth = Path().apply {
                            moveTo(center.x, center.y - radius + 14.dp.toPx())
                            lineTo(center.x - 8.dp.toPx(), center.y)
                            lineTo(center.x + 8.dp.toPx(), center.y)
                            close()
                        }
                        drawPath(path = pathNorth, color = Color(0xFFFF3B30))

                        // Pointer (South)
                        val pathSouth = Path().apply {
                            moveTo(center.x, center.y + radius - 14.dp.toPx())
                            lineTo(center.x - 6.dp.toPx(), center.y)
                            lineTo(center.x + 6.dp.toPx(), center.y)
                            close()
                        }
                        drawPath(path = pathSouth, color = textCol.copy(alpha = 0.4f))
                    }

                    // Center Digital Readout
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${heading.toInt()}°",
                            fontSize = 18.sp,
                            color = textCol,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = getCardinalStr(heading),
                            fontSize = 11.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 2. Attitude & Bubble Level (Pitch & Roll for inclination/dip measurements)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ATTITUDE / INCLINATION",
                        fontSize = 8.sp,
                        color = accentColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    // 2D Bubble level canvas
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .border(1.dp, textCol.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val maxOffsetPx = size.width / 2 - 8.dp.toPx()

                            // Crosshair lines
                            drawLine(textCol.copy(0.2f), Offset(0f, center.y), Offset(size.width, center.y), 1f)
                            drawLine(textCol.copy(0.2f), Offset(center.x, 0f), Offset(center.x, size.height), 1f)
                            drawCircle(textCol.copy(0.2f), center = center, radius = 12.dp.toPx(), style = Stroke(1f))

                            // Calculate bubble position based on pitch & roll
                            val bubbleX = center.x + (roll.coerceIn(-45f, 45f) / 45f) * maxOffsetPx
                            val bubbleY = center.y + (pitch.coerceIn(-45f, 45f) / 45f) * maxOffsetPx

                            drawCircle(
                                color = accentColor,
                                radius = 6.dp.toPx(),
                                center = Offset(bubbleX, bubbleY)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Pitch: ${String.format(Locale.US, "%.1f°", pitch)}",
                            fontSize = 10.sp,
                            color = textCol.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Roll: ${String.format(Locale.US, "%.1f°", roll)}",
                            fontSize = 10.sp,
                            color = textCol.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Coordinates & Location Telemetry Block
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, textCol.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Location",
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "CURRENT FIELD POSITION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        TextButton(
                            onClick = {
                                val textToCopy = "Lat: %.6f, Lon: %.6f | UTM: %s %.1fE %.1fN | Ele: %.1fm | Heading: %d°".format(
                                    Locale.US, latitude, longitude,
                                    utmCoord?.zoneString ?: "", utmCoord?.easting ?: 0.0, utmCoord?.northing ?: 0.0,
                                    elevation, heading.toInt()
                                )
                                clipboardManager.setText(AnnotatedString(textToCopy))
                            },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(
                                text = "COPY GPS DATA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // WGS84 Readout
                    Text(
                        text = "WGS84: ${String.format(Locale.US, "%.7f° N, %.7f° E", latitude, longitude)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = textCol
                    )

                    // UTM Metric Grid Readout
                    if (utmCoord != null) {
                        Text(
                            text = "UTM: Zone ${utmCoord.zoneString} • ${String.format(Locale.US, "%.1f", utmCoord.easting)}m E • ${String.format(Locale.US, "%.1f", utmCoord.northing)}m N",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = textCol
                        )
                    }

                    // Elevation & Accuracy & Grid Convergence
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Elevation: ${String.format(Locale.US, "%.1f m", elevation)}",
                            fontSize = 10.sp,
                            color = textCol.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Accuracy: ±${String.format(Locale.US, "%.1f m", accuracy)}",
                            fontSize = 10.sp,
                            color = textCol.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Grid Conv: ${String.format(Locale.US, "%+.2f°", gridConvergence)}",
                            fontSize = 10.sp,
                            color = textCol.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Sensor Accuracy & Calibration Status Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = when (sensorData.accuracy) {
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Color(0xFF4CAF50)
                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                    val statusText = when (sensorData.accuracy) {
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Sensor: High Accuracy"
                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Sensor: Medium Accuracy"
                        else -> "Sensor: Uncalibrated (Rotate in Figure 8)"
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = statusText,
                        fontSize = 9.sp,
                        color = textCol.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = sensorData.sensorTypeName,
                    fontSize = 8.sp,
                    color = accentColor.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

public fun getCardinalStr(degree: Double): String {
    val deg = (degree % 360 + 360) % 360
    return when {
        deg >= 337.5 || deg < 22.5 -> "N"
        deg >= 22.5 && deg < 67.5 -> "NE"
        deg >= 67.5 && deg < 112.5 -> "E"
        deg >= 112.5 && deg < 157.5 -> "SE"
        deg >= 157.5 && deg < 202.5 -> "S"
        deg >= 202.5 && deg < 247.5 -> "SW"
        deg >= 247.5 && deg < 292.5 -> "W"
        else -> "NW"
    }
}
