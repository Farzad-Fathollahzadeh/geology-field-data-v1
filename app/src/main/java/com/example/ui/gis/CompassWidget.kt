package com.example.ui.gis

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassWidget(
    heading: Double,
    modifier: Modifier = Modifier,
    bearing: Double? = null,
    isOutdoorMode: Boolean = false
) {
    val animatedHeading by animateFloatAsState(targetValue = heading.toFloat())
    
    val textCol = if (isOutdoorMode) Color.Black else Color.White
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF00FFE0)
    val panelBg = if (isOutdoorMode) Color.White.copy(alpha = 0.95f) else Color(0xDD0A100E)
    val circleOutline = textCol.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(panelBg)
            .border(1.dp, circleOutline, CircleShape),
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
                    color = textCol.copy(alpha = if (angle % 90 == 0) 0.6f else 0.3f),
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
            drawPath(path = pathNorth, color = Color.Red)

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

        // Center overlay showing digital degrees
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${heading.toInt()}°",
                fontSize = 12.sp,
                color = textCol,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = getCardinalStr(heading),
                fontSize = 8.sp,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun getCardinalStr(degree: Double): String {
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
