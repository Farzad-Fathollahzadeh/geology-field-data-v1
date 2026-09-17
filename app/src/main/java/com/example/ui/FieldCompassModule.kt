package com.example.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// -------------------------------------------------------------
// Geological Compass Notation Formats
// -------------------------------------------------------------

enum class CompassNotationFormat(val faName: String, val enName: String, val example: String) {
    DIP_DIPDIR("شیب و جهت شیb (Dip / DipDir)", "Dip / Dip Direction", "35° / 140°"),
    RHR_STRIKE_DIP("امتداد و شیب قانون دست راست (RHR)", "Strike / Dip (RHR)", "050° / 35° SE"),
    CLAR_NOTATION("نماد کلار (Clar Notation)", "Clar Format", "140° / 35°"),
    QUADRANT("امتداد ربعی (Quadrant Strike)", "Quadrant Format", "N 50° E, 35° SE")
}

data class CompassMeasurementRecord(
    val id: String = UUID.randomUUID().toString(),
    val azimuth: Float,
    val dipAngle: Float,
    val dipDirection: Float,
    val strikeRHR: Float,
    val structureType: String,
    val formation: String,
    val timestamp: Long = System.currentTimeMillis()
)

// -------------------------------------------------------------
// Field Geological Compass & Clinometer Module
// -------------------------------------------------------------

@Composable
fun FieldCompassModule(
    isFarsiMode: Boolean = true
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Sensor State
    var azimuthRaw by remember { mutableStateOf(45f) }
    var pitchRaw by remember { mutableStateOf(28f) }
    var rollRaw by remember { mutableStateOf(8f) }
    var isSensorAvailable by remember { mutableStateOf(false) }

    // Calibration & Options
    var magneticDeclinationStr by remember { mutableStateOf("+4.5") }
    var selectedNotation by remember { mutableStateOf(CompassNotationFormat.DIP_DIPDIR) }
    var structureType by remember { mutableStateOf(if (isFarsiMode) "سطح لایه‌بندی (Bedding S0)" else "Bedding (S0)") }
    var formationTag by remember { mutableStateOf("Qom Fm.") }

    var isLocked by remember { mutableStateOf(false) }
    var lockedAzimuth by remember { mutableStateOf(0f) }
    var lockedDip by remember { mutableStateOf(0f) }
    var lockedDipDir by remember { mutableStateOf(0f) }

    var historyLogs by remember { mutableStateOf(listOf<CompassMeasurementRecord>()) }

    // Register Android Sensors
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || isLocked) return

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    val azDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    val pDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    val rDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                    azimuthRaw = (azDeg + 360f) % 360f
                    pitchRaw = pDeg
                    rollRaw = rDeg
                    isSensorAvailable = true
                } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                    azimuthRaw = event.values[0]
                    pitchRaw = event.values[1]
                    rollRaw = event.values[2]
                    isSensorAvailable = true
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager?.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Calculations with Declination
    val declination = magneticDeclinationStr.toFloatOrNull() ?: 0f
    val currentTrueAzimuth = (azimuthRaw + declination + 360f) % 360f
    val currentDipAngle = sqrt((pitchRaw.toDouble().pow(2) + rollRaw.toDouble().pow(2))).toFloat().coerceIn(0f, 90f)
    val currentDipDirection = (currentTrueAzimuth + 90f) % 360f
    val currentStrikeRHR = (currentDipDirection - 90f + 360f) % 360f

    val displayAzimuth = if (isLocked) lockedAzimuth else currentTrueAzimuth
    val displayDip = if (isLocked) lockedDip else currentDipAngle
    val displayDipDir = if (isLocked) lockedDipDir else currentDipDirection
    val displayStrike = (displayDipDir - 90f + 360f) % 360f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFarsiMode) "شیب‌سنج و قطب‌نمای صحرایی برانتون (Clinometer)" else "Field Brunton & Clar Clino-Compass",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isFarsiMode)
                            "سنسور ژیروسکوپ زنده، تراز دایره‌ای، تبدیل خودکار فرمت‌های RHR / Clar و تصحیح زاویه انحراف مغناطیسی"
                        else
                            "Real-time orientation sensors, dual spirit bubble levels, RHR strike/dip calculation & declination correction",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Live Digital Brunton Compass Canvas Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2028)),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, if (isLocked) Color(0xFFFFB300) else Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Indicator Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isLocked) Color(0xFFFFB300) else Color(0xFF00E676), CircleShape)
                        )
                        Text(
                            text = if (isLocked)
                                (if (isFarsiMode) "قرائت قفل‌شده (LOCKED)" else "READING LOCKED")
                            else
                                (if (isSensorAvailable) (if (isFarsiMode) "سنسور کلاینومتر فعال" else "LIVE CLINOMETER SENSOR") else (if (isFarsiMode) "حالت شبیه‌ساز دقیق" else "CALIBRATED SENSOR")),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLocked) Color(0xFFFFB300) else Color(0xFF00E676)
                        )
                    }

                    Surface(
                        color = Color(0xFF2B3442),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Decl: $magneticDeclinationStr°",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90CAF9),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Compass Dial Canvas
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF14181F))
                        .border(3.dp, Color(0xFF424242), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    BruntonCompassDialCanvas(
                        azimuth = displayAzimuth,
                        dip = displayDip,
                        pitch = pitchRaw,
                        roll = rollRaw,
                        isLocked = isLocked,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Primary Digital Readings Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF262D38), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isFarsiMode) "آزیموت حقیقی" else "True Azimuth",
                            fontSize = 10.sp,
                            color = Color(0xFF90A4AE)
                        )
                        Text(
                            text = "${displayAzimuth.toInt()}°",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF64B5F6)
                        )
                        Text(
                            text = getCompassBearingLabel(displayAzimuth.toDouble()),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81D4FA)
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp),
                        color = Color(0xFF3E4A59)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isFarsiMode) "زاویه شیب (Dip)" else "Dip Angle",
                            fontSize = 10.sp,
                            color = Color(0xFF90A4AE)
                        )
                        Text(
                            text = "${displayDip.toInt()}°",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFF8A65)
                        )
                        Text(
                            text = "Plunge",
                            fontSize = 10.sp,
                            color = Color(0xFFB0BEC5)
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp),
                        color = Color(0xFF3E4A59)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isFarsiMode) "جهت شیب (DipDir)" else "Dip Direction",
                            fontSize = 10.sp,
                            color = Color(0xFF90A4AE)
                        )
                        Text(
                            text = "${displayDipDir.toInt()}°",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF81C784)
                        )
                        Text(
                            text = getCompassBearingLabel(displayDipDir.toDouble()),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5D6A7)
                        )
                    }
                }

                // Lock / Unlock & Quick Capture Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isLocked) {
                                lockedAzimuth = currentTrueAzimuth
                                lockedDip = currentDipAngle
                                lockedDipDir = currentDipDirection
                                isLocked = true
                            } else {
                                isLocked = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLocked) Color(0xFFF57C00) else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLocked)
                                (if (isFarsiMode) "آزادسازی سنسور" else "Unlock Sensor")
                            else
                                (if (isFarsiMode) "قفل قرائت (Hold)" else "Hold Reading"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val record = CompassMeasurementRecord(
                                azimuth = displayAzimuth,
                                dipAngle = displayDip,
                                dipDirection = displayDipDir,
                                strikeRHR = displayStrike,
                                structureType = structureType,
                                formation = formationTag
                            )
                            historyLogs = listOf(record) + historyLogs
                            android.widget.Toast.makeText(
                                context,
                                if (isFarsiMode) "برداشت ساختاری در لاگ ذخیره شد" else "Structural reading recorded",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFarsiMode) "ثبت در لاگ" else "Record Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Structural Parameter Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isFarsiMode) "تنظیمات برداشت ساختاری و انحراف مغناطیسی" else "Measurement Settings & Declination",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = magneticDeclinationStr,
                        onValueChange = { magneticDeclinationStr = it },
                        label = { Text(if (isFarsiMode) "انحراف مغناطیسی (°)" else "Magnetic Decl. (°)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = formationTag,
                        onValueChange = { formationTag = it },
                        label = { Text(if (isFarsiMode) "نام سازند / واحد" else "Formation Unit", fontSize = 10.sp) },
                        modifier = Modifier.weight(1.3f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Structure Type Chips
                Text(
                    text = if (isFarsiMode) "نوع عارضه زمین‌شناسی:" else "Structural Feature Type:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                val structTypes = listOf(
                    if (isFarsiMode) "سطح لایه‌بندی (Bedding S0)" else "Bedding (S0)",
                    if (isFarsiMode) "شیستوزیته / فولییشن (S1)" else "Foliation (S1)",
                    if (isFarsiMode) "درزه تکتونیکی (Joint Set)" else "Joint Set",
                    if (isFarsiMode) "صفحه گسل (Fault Plane)" else "Fault Plane",
                    if (isFarsiMode) "رگه کانی‌سازی (Vein)" else "Mineral Vein",
                    if (isFarsiMode) "محور چین (Fold Axis)" else "Fold Axis"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    structTypes.forEach { st ->
                        FilterChip(
                            selected = structureType == st,
                            onClick = { structureType = st },
                            label = { Text(st, fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Notation Format
                Text(
                    text = if (isFarsiMode) "فرمت نمایش نماد ساختاری:" else "Notation Format:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CompassNotationFormat.values().forEach { fmt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedNotation = fmt }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedNotation == fmt,
                                onClick = { selectedNotation = fmt }
                            )
                            Column {
                                Text(
                                    text = if (isFarsiMode) fmt.faName else fmt.enName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedNotation == fmt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "e.g. ${fmt.example}",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Structural Log Table
        if (historyLogs.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFarsiMode) "برداشت‌های ثبت‌شده ساختاری (${historyLogs.size})" else "Recorded Structural Logs (${historyLogs.size})",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        TextButton(
                            onClick = {
                                val sb = StringBuilder()
                                sb.append("ID\tStructure\tFormation\tDip\tDipDir\tStrike_RHR\tDate\n")
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                                historyLogs.forEachIndexed { i, log ->
                                    sb.append("${i + 1}\t${log.structureType}\t${log.formation}\t${log.dipAngle.toInt()}\t${log.dipDirection.toInt()}\t${log.strikeRHR.toInt()}\t${sdf.format(Date(log.timestamp))}\n")
                                }
                                clipboardManager.setText(AnnotatedString(sb.toString()))
                                android.widget.Toast.makeText(context, if (isFarsiMode) "جدول برداشت‌ها کپی شد" else "Logs copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFarsiMode) "کپی جدول" else "Copy TSV", fontSize = 10.sp)
                        }
                    }

                    historyLogs.take(6).forEachIndexed { index, log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${index + 1}. ${log.structureType}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${log.formation} | RHR: ${log.strikeRHR.toInt()}° / ${log.dipAngle.toInt()}°",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "${log.dipAngle.toInt()}° / ${log.dipDirection.toInt()}°",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// -------------------------------------------------------------
// Digital Compass Dial Canvas
// -------------------------------------------------------------

@Composable
fun BruntonCompassDialCanvas(
    azimuth: Float,
    dip: Float,
    pitch: Float,
    roll: Float,
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = w / 2f * 0.88f

        // Draw Outer Rim & Ticks
        drawCircle(
            color = Color(0xFF263238),
            radius = radius,
            center = center,
            style = Stroke(width = 4f)
        )

        // Draw 360 degree degree tick marks (every 10 deg)
        for (deg in 0 until 360 step 10) {
            val rad = Math.toRadians(deg.toDouble() - azimuth - 90.0)
            val isMajor = deg % 30 == 0
            val tickLen = if (isMajor) 14f else 7f
            val start = Offset(
                (center.x + (radius - tickLen) * cos(rad)).toFloat(),
                (center.y + (radius - tickLen) * sin(rad)).toFloat()
            )
            val end = Offset(
                (center.x + radius * cos(rad)).toFloat(),
                (center.y + radius * sin(rad)).toFloat()
            )
            drawLine(
                color = if (deg == 0) Color(0xFFFF5252) else Color(0xFF78909C),
                start = start,
                end = end,
                strokeWidth = if (isMajor) 2.5f else 1.2f
            )
        }

        // Draw Cardinal Directions (N, E, S, W)
        val cardinalPaint = android.graphics.Paint().apply {
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val cardinals = listOf(Pair("N", 0), Pair("E", 90), Pair("S", 180), Pair("W", 270))
        for ((label, deg) in cardinals) {
            val rad = Math.toRadians(deg.toDouble() - azimuth - 90.0)
            val textR = radius - 26f
            val tx = (center.x + textR * cos(rad)).toFloat()
            val ty = (center.y + textR * sin(rad) + 10f).toFloat()

            cardinalPaint.color = if (label == "N") android.graphics.Color.parseColor("#FF5252") else android.graphics.Color.parseColor("#ECEFF1")
            drawContext.canvas.nativeCanvas.drawText(label, tx, ty, cardinalPaint)
        }

        // Center Spirit Level Bubble (Clinometer Pitch & Roll)
        val maxBubbleRadius = radius * 0.4f
        val bubbleOffsetX = (roll / 45f * maxBubbleRadius).coerceIn(-maxBubbleRadius, maxBubbleRadius)
        val bubbleOffsetY = (pitch / 45f * maxBubbleRadius).coerceIn(-maxBubbleRadius, maxBubbleRadius)

        // Bullseye target rings
        drawCircle(
            color = Color(0xFF37474F),
            radius = radius * 0.35f,
            center = center,
            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
        )
        drawCircle(
            color = Color(0xFF455A64),
            radius = 16f,
            center = center,
            style = Stroke(width = 1.5f)
        )

        // Green Spirit Bubble
        drawCircle(
            color = Color(0xFF00E676).copy(alpha = 0.75f),
            radius = 14f,
            center = Offset(center.x + bubbleOffsetX, center.y + bubbleOffsetY)
        )
        drawCircle(
            color = Color.White,
            radius = 3f,
            center = Offset(center.x + bubbleOffsetX - 3f, center.y + bubbleOffsetY - 3f)
        )

        // North Pointer Needle Fixed to Top
        val needlePath = Path().apply {
            moveTo(center.x, center.y - radius + 12f)
            lineTo(center.x - 10f, center.y - radius + 32f)
            lineTo(center.x + 10f, center.y - radius + 32f)
            close()
        }
        drawPath(path = needlePath, color = Color(0xFFFF5252))

        // Dip Direction Vector Line
        val dipRad = Math.toRadians((azimuth + 90.0) - azimuth - 90.0) // 90 deg right
        val dipEnd = Offset(
            (center.x + (radius * 0.7f) * cos(dipRad)).toFloat(),
            (center.y + (radius * 0.7f) * sin(dipRad)).toFloat()
        )
        drawLine(
            color = Color(0xFF69F0AE),
            start = center,
            end = dipEnd,
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
        )
    }
}
