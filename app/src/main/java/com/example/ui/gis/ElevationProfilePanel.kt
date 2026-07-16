package com.example.ui.gis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TrackPoint
import java.util.Locale
import kotlin.math.abs

@Composable
fun ElevationProfilePanel(
    points: List<TrackPoint>,
    isOutdoorMode: Boolean = false,
    onClose: () -> Unit,
    onDownloadOfflineProfile: ((List<TrackPoint>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF00FFE0)
    val panelBg = if (isOutdoorMode) Color.White else Color(0xFF0C100E)
    val textColor = if (isOutdoorMode) Color.Black else Color.White
    val borderCol = if (isOutdoorMode) Color(0xFFCCCCCC) else Color(0xFF1B2321)
    val textMuted = if (isOutdoorMode) Color.DarkGray else Color.Gray

    // Profile Selector and Dip Angle Configuration State
    var selectedProfileType by remember { mutableStateOf(0) } // 0: Longitudinal Profile, 1: Geologic Cross-Section
    var dipAngle by remember { mutableStateOf(20f) }          // apparent dip angle of sub-surface layers

    // Scrubbing interactiveness states
    var scrubbedX by remember { mutableStateOf<Float?>(null) }
    var chartWidth by remember { mutableStateOf(1f) }

    // Calculate elevation stats safely
    val stats = remember(points) {
        if (points.isEmpty()) {
            ElevationStats(0.0, 0.0, 0.0, 0.0, 0.0)
        } else {
            val elevations = points.map { it.ele }
            val maxE = elevations.maxOrNull() ?: 0.0
            val minE = elevations.minOrNull() ?: 0.0
            val currentE = points.last().ele
            var ascent = 0.0
            var descent = 0.0
            for (i in 1 until points.size) {
                val diff = points[i].ele - points[i - 1].ele
                if (diff > 0) {
                    ascent += diff
                } else {
                    descent += abs(diff)
                }
            }
            ElevationStats(maxE, minE, ascent, descent, currentE)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = panelBg),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terrain,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (selectedProfileType == 0) "پروفایل طولی مسیر | LONGITUDINAL" else "مقطع عرضی و ساختار طبقات زمین‌شناسی | CROSS-SECTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Profile Type Segmented Tab Switcher
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(textColor.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0 to "پروفایل طولی (Elevation/Distance)", 1 to "مقطع عرضی زمین‌شناسی (Apparent Dip)").forEach { (idx, label) ->
                    val isSelected = selectedProfileType == idx
                    val activeBg = if (isSelected) accentColor else Color.Transparent
                    val activeFg = if (isSelected) Color.Black else textColor.copy(alpha = 0.7f)
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(activeBg)
                            .clickable { selectedProfileType = idx }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeFg
                        )
                    }
                }
            }

            if (selectedProfileType == 1) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(textColor.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "زاویه شیب ظاهری لایه‌ها / Apparent Dip Angle:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "شبیه‌ساز شیب گسلی و طبقات ساختاری زمین‌شناسی",
                            fontSize = 8.sp,
                            color = textMuted
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${dipAngle.toInt()}°",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(28.dp)
                        )
                        Slider(
                            value = dipAngle,
                            onValueChange = { dipAngle = it },
                            valueRange = -60f..60f,
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (points.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(textColor.copy(alpha = 0.03f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "جهت ترسیم پروفایل ارتفاعی، حداقل ۲ نقطه ثبت شده لازم است.\n(در حال ضبط مسیر حرکت یا پیمایش)",
                        fontSize = 10.5.sp,
                        color = textMuted,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            } else {
                // Statistics Dashboard
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(textColor.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBox(
                        label = "حداکثر ارتفاع",
                        subLabel = "MAX ALT",
                        value = "${stats.max.toInt()} m",
                        color = accentColor,
                        isOutdoorMode = isOutdoorMode
                    )
                    StatDivider(textColor)
                    StatBox(
                        label = "حداقل ارتفاع",
                        subLabel = "MIN ALT",
                        value = "${stats.min.toInt()} m",
                        color = textMuted,
                        isOutdoorMode = isOutdoorMode
                    )
                    StatDivider(textColor)
                    StatBox(
                        label = "صعود کل",
                        subLabel = "ASCENT",
                        value = "+${stats.ascent.toInt()} m",
                        color = Color(0xFF4CAF50),
                        isOutdoorMode = isOutdoorMode
                    )
                    StatDivider(textColor)
                    StatBox(
                        label = "فرود کل",
                        subLabel = "DESCENT",
                        value = "-${stats.descent.toInt()} m",
                        color = Color(0xFFFF9800),
                        isOutdoorMode = isOutdoorMode
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // main custom chart area with scrubbing tooltip overlay
                val elevations = remember(points) { points.map { it.ele } }
                val minEle = stats.min - 10.0 // pad margins
                val maxEle = stats.max + 10.0
                val eleDiff = if (maxEle - minEle == 0.0) 1.0 else maxEle - minEle

                val selectedPointIndex = scrubbedX?.let { sx ->
                    val ratio = (sx / chartWidth).coerceIn(0f, 1f)
                    val index = (ratio * (points.size - 1)).toInt()
                    index.coerceIn(0, points.size - 1)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(textColor.copy(alpha = 0.02f), RoundedCornerShape(6.dp))
                        .border(0.5.dp, borderCol, RoundedCornerShape(6.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .pointerInput(points) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        scrubbedX = offset.x
                                    },
                                    onDragEnd = {
                                        scrubbedX = null
                                    },
                                    onDragCancel = {
                                        scrubbedX = null
                                    },
                                    onDrag = { change, dragAmount ->
                                        scrubbedX = (scrubbedX ?: change.position.x) + dragAmount.x
                                    }
                                )
                            }
                            .pointerInput(points) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        scrubbedX = offset.x
                                        tryAwaitRelease()
                                        scrubbedX = null
                                    }
                                )
                            }
                    ) {
                        chartWidth = size.width
                        val w = size.width
                        val h = size.height

                        // 1. Draw horizontal grid lines
                        val numGridLines = 3
                        val gridPaintColor = textColor.copy(alpha = 0.1f)
                        for (i in 0..numGridLines) {
                            val y = h * (i.toFloat() / numGridLines)
                            drawLine(
                                color = gridPaintColor,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                        }

                        // 2. Build path for terrain profile area
                        val fillPath = Path()
                        val linePath = Path()

                        points.forEachIndexed { index, pt ->
                            val x = w * (index.toFloat() / (points.size - 1))
                            val normalizedY = ((pt.ele - minEle) / eleDiff).toFloat()
                            val y = h - (normalizedY * h)

                            if (index == 0) {
                                fillPath.moveTo(x, h)
                                fillPath.lineTo(x, y)
                                linePath.moveTo(x, y)
                            } else {
                                fillPath.lineTo(x, y)
                                linePath.lineTo(x, y)
                            }

                            if (index == points.size - 1) {
                                fillPath.lineTo(x, h)
                                fillPath.close()
                            }
                        }

                        // 3. Draw geologic layers or standard gradient beneath the profile
                        if (selectedProfileType == 1) {
                            clipPath(path = fillPath) {
                                // apparent geological dipping layers computation
                                val tanDip = kotlin.math.tan(Math.toRadians(dipAngle.toDouble()).toFloat())
                                
                                val formations = listOf(
                                    Pair("Limestone (K_ls)", Color(0xFF64B5F6)), // Light blue
                                    Pair("Shale (J_sh)", Color(0xFF81C784)),     // Light green
                                    Pair("Marl (TR_m)", Color(0xFFFFF176)),      // Yellow
                                    Pair("Sandstone (TR_ss)", Color(0xFFFFB74D)), // Orange
                                    Pair("Basement Volcanics (P_v)", Color(0xFFE57373)) // Reddish
                                )
                                
                                val formationCount = formations.size
                                val layerHeight = h / (formationCount - 1)
                                
                                for (i in 0 until formationCount) {
                                    val formationPath = Path()
                                    // Offset using trig Apparent Dip calculation across chart width
                                    val yLeft = i * layerHeight + (0 - w / 2) * tanDip
                                    val yRight = i * layerHeight + (w - w / 2) * tanDip
                                    
                                    val nextYLeft = (i + 1) * layerHeight + (0 - w / 2) * tanDip
                                    val nextYRight = (i + 1) * layerHeight + (w - w / 2) * tanDip
                                    
                                    formationPath.moveTo(0f, yLeft)
                                    formationPath.lineTo(w, yRight)
                                    formationPath.lineTo(w, nextYRight)
                                    formationPath.lineTo(0f, nextYLeft)
                                    formationPath.close()
                                    
                                    drawPath(
                                        path = formationPath,
                                        color = formations[i].second.copy(alpha = if (isOutdoorMode) 0.65f else 0.45f)
                                    )
                                    
                                    // Layer boundary line drawing
                                    drawLine(
                                        color = (if (isOutdoorMode) Color.DarkGray else Color.White).copy(alpha = 0.25f),
                                        start = Offset(0f, yLeft),
                                        end = Offset(w, yRight),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                            }
                        } else {
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(accentColor.copy(alpha = 0.45f), accentColor.copy(alpha = 0.02f)),
                                    startY = 0f,
                                    endY = h
                                )
                            )
                        }

                        // 4. Draw high-contrast outline trail
                        drawPath(
                            path = linePath,
                            color = accentColor,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // 5. Draw scrubbing cursor line and dot
                        selectedPointIndex?.let { idx ->
                            val scrubX = w * (idx.toFloat() / (points.size - 1))
                            val pt = points[idx]
                            val normalizedY = ((pt.ele - minEle) / eleDiff).toFloat()
                            val scrubY = h - (normalizedY * h)

                            // Vertical tracking cursor line
                            drawLine(
                                color = accentColor.copy(alpha = 0.8f),
                                start = Offset(scrubX, 0f),
                                end = Offset(scrubX, h),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                            )

                            // Highlight point on the trail
                            drawCircle(
                                color = panelBg,
                                radius = 5.dp.toPx(),
                                center = Offset(scrubX, scrubY)
                            )
                            drawCircle(
                                color = accentColor,
                                radius = 3.dp.toPx(),
                                center = Offset(scrubX, scrubY)
                            )
                        }
                    }

                    // Floating Tooltip showing precise elevation and distance on scrubbing
                    selectedPointIndex?.let { idx ->
                        val pt = points[idx]
                        // Estimate cumulative distance to this point
                        var cumDist = 0.0
                        for (i in 1..idx) {
                            val results = FloatArray(1)
                            android.location.Location.distanceBetween(
                                points[i-1].lat, points[i-1].lon,
                                points[i].lat, points[i].lon,
                                results
                            )
                            cumDist += results[0]
                        }

                        Box(
                            modifier = Modifier
                                .align(if (idx < points.size / 2) Alignment.TopEnd else Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor.copy(alpha = 0.9f))
                                .padding(vertical = 2.dp, horizontal = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "ارتفاع: ${pt.ele.toInt()} m",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "مسافت: ${if (cumDist >= 1000.0) String.format(Locale.US, "%.2f km", cumDist / 1000.0) else "${cumDist.toInt()} m"}",
                                    fontSize = 8.sp,
                                    color = Color.Black.copy(alpha = 0.8f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
                
                // Apparent Dip legend display
                if (selectedProfileType == 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(textColor.copy(alpha = 0.02f), RoundedCornerShape(4.dp))
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val formations = listOf(
                            "K_ls (Limestone)" to Color(0xFF64B5F6),
                            "J_sh (Shale)" to Color(0xFF81C784),
                            "TR_m (Marl)" to Color(0xFFFFF176),
                            "TR_ss (Sandstone)" to Color(0xFFFFB74D),
                            "P_v (Volcanics)" to Color(0xFFE57373)
                        )
                        formations.forEach { (label, col) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(col)
                                )
                                Text(
                                    text = label,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accentColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                        Text(
                            text = "تخمین و دانلود آفلاین پروفیل ارتفاعی UTM",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        val samplePoint = points.firstOrNull()
                        val utmZoneText = if (samplePoint != null) {
                            val utm = com.example.UtmConverter.convertLatLngToUtm(samplePoint.lat, samplePoint.lon)
                            "Zone ${utm.zoneNumber}${utm.zoneChar} (${points.size} pts)"
                        } else {
                            "ناشناخته"
                        }
                        Text(
                            text = "محاسبه ارتفاع توپوگرافی محلی بدون نیاز به شبکه بر اساس مختصات ($utmZoneText)",
                            fontSize = 8.sp,
                            color = textMuted
                        )
                    }
                    
                    Button(
                        onClick = {
                            val updatedPoints = points.map { pt ->
                                val estimatedEle = com.example.UtmConverter.estimateElevationFromUtm(pt.lat, pt.lon)
                                pt.copy(ele = estimatedEle)
                            }
                            onDownloadOfflineProfile?.invoke(updatedPoints)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("محاسبه و دانلود / Calculate", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Scrubbing instructions helper label
                Text(
                    text = "💡 جهت پیمایش مقطع زمین‌شناسی، انگشت خود را روی نمودار بکشید | Drag chart to explore structure",
                    fontSize = 7.5.sp,
                    color = textMuted,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

data class ElevationStats(
    val max: Double,
    val min: Double,
    val ascent: Double,
    val descent: Double,
    val current: Double
)

@Composable
private fun StatBox(
    label: String,
    subLabel: String,
    value: String,
    color: Color,
    isOutdoorMode: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            color = if (isOutdoorMode) Color.DarkGray else Color.LightGray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = subLabel,
            fontSize = 7.sp,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun StatDivider(textColor: Color) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .width(0.5.dp)
            .background(textColor.copy(alpha = 0.15f))
    )
}
