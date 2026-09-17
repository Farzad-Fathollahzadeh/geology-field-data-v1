package com.example.ui

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.*

// -------------------------------------------------------------
// Geological Cross-Section Data Models
// -------------------------------------------------------------

enum class TopoProfileType(val faName: String, val enName: String) {
    HILL("تپه / بلندی (Ridge)", "Ridge / Hill"),
    VALLEY("دره / شیار (Valley)", "Valley / Depr."),
    SLOPE("شیب یکنواخت (Slope)", "Uniform Slope"),
    PLATEAU("فلات مسطح (Plateau)", "Flat Plateau"),
    ANTICLINE_LAND("چین ناودیسی/طاقدیسی", "Folded Land")
}

enum class LithoPattern(val faName: String, val enName: String, val color: Color) {
    LIMESTONE("آهک (Limestone)", "Limestone", Color(0xFF64B5F6)),
    DOLOMITE("دولومیت (Dolomite)", "Dolomite", Color(0xFF4FC3F7)),
    SANDSTONE("ماسه سنگ (Sandstone)", "Sandstone", Color(0xFFFFD54F)),
    SHALE("شیل و سیلتستون (Shale)", "Shale / Siltstone", Color(0xFFA1887F)),
    CONGLOMERATE("کنگلومرا (Conglomerate)", "Conglomerate", Color(0xFFFF8A65)),
    BASALT("بازالت / ولکانیک (Basalt)", "Basalt / Volcanic", Color(0xFF78909C)),
    ANDESITE("آندزیت (Andesite)", "Andesite", Color(0xFF8D6E63)),
    GRANITE("گرانیت و پلوتونیک (Granite)", "Granite / Plutonic", Color(0xFFF06292)),
    SCHIST("شیست / دگرگونی (Schist)", "Schist / Metamorphic", Color(0xFF81C784)),
    ALLUVIUM("آبرفت کواترنری (Alluvium)", "Quaternary Alluvium", Color(0xFFFFE082))
}

data class GeoFormationLayer(
    val id: String,
    val name: String,
    val pattern: LithoPattern,
    val trueThickness: Double, // in meters
    val trueDip: Double, // 0 to 90 degrees
    val trueDipDir: Double // 0 to 360 azimuth
)

data class GeoFault(
    val enabled: Boolean = true,
    val faultName: String = "Fault F-1",
    val positionRatio: Float = 0.55f, // 0.0 to 1.0 along the section
    val dipAngle: Double = 65.0, // Dip angle of fault plane (e.g. 60 deg)
    val dipDirectionRight: Boolean = true, // dips towards right or left of section
    val verticalThrow: Double = 60.0 // displacement in meters (positive = downthrown block)
)

// -------------------------------------------------------------
// Geological Cross-Section Component
// -------------------------------------------------------------

@Composable
fun GeologicalCrossSectionModule(
    isFarsiMode: Boolean = true
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Section Parameters
    var sectionAzimuthStr by remember { mutableStateOf("045") }
    var sectionLengthStr by remember { mutableStateOf("1000") }
    var selectedTopoType by remember { mutableStateOf(TopoProfileType.HILL) }
    var baseElevationStr by remember { mutableStateOf("1600") }
    var maxElevationRelStr by remember { mutableStateOf("180") }

    // Fault Parameters
    var hasFault by remember { mutableStateOf(true) }
    var faultDipStr by remember { mutableStateOf("60") }
    var faultThrowStr by remember { mutableStateOf("45") }
    var faultDipRight by remember { mutableStateOf(false) }

    // Formations List
    var formations by remember {
        mutableStateOf(
            listOf(
                GeoFormationLayer(
                    id = "1",
                    name = if (isFarsiMode) "سازند قم (آهک و مارن)" else "Qom Fm (Limestone)",
                    pattern = LithoPattern.LIMESTONE,
                    trueThickness = 120.0,
                    trueDip = 35.0,
                    trueDipDir = 40.0
                ),
                GeoFormationLayer(
                    id = "2",
                    name = if (isFarsiMode) "ماسه سنگ قرمز بالایی" else "Upper Red Sandstone",
                    pattern = LithoPattern.SANDSTONE,
                    trueThickness = 180.0,
                    trueDip = 38.0,
                    trueDipDir = 45.0
                ),
                GeoFormationLayer(
                    id = "3",
                    name = if (isFarsiMode) "شیل و سیلتستون سازند کرج" else "Karaj Fm (Shale)",
                    pattern = LithoPattern.SHALE,
                    trueThickness = 140.0,
                    trueDip = 40.0,
                    trueDipDir = 50.0
                ),
                GeoFormationLayer(
                    id = "4",
                    name = if (isFarsiMode) "کنگلومرای قاعده دگرگونی" else "Basal Conglomerate",
                    pattern = LithoPattern.CONGLOMERATE,
                    trueThickness = 90.0,
                    trueDip = 42.0,
                    trueDipDir = 55.0
                )
            )
        )
    }

    // Modal / Layer Add State
    var showAddLayerDialog by remember { mutableStateOf(false) }
    var newLayerName by remember { mutableStateOf("") }
    var newLayerPattern by remember { mutableStateOf(LithoPattern.LIMESTONE) }
    var newLayerThickness by remember { mutableStateOf("100") }
    var newLayerDip by remember { mutableStateOf("30") }
    var newLayerDipDir by remember { mutableStateOf("045") }

    val sectionAzimuth = sectionAzimuthStr.toDoubleOrNull() ?: 45.0
    val sectionLength = (sectionLengthStr.toDoubleOrNull() ?: 1000.0).coerceAtLeast(100.0)
    val baseElevation = baseElevationStr.toDoubleOrNull() ?: 1600.0
    val maxRelElevation = maxElevationRelStr.toDoubleOrNull() ?: 180.0
    val faultDip = faultDipStr.toDoubleOrNull() ?: 60.0
    val faultThrow = faultThrowStr.toDoubleOrNull() ?: 45.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoGraph,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFarsiMode) "مولد برش عرضی و پروفیل زمین‌شناسی" else "Geological Cross-Section Generator",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = if (isFarsiMode)
                            "رسم اتوماتیک مقطع ساختاری بر اساس امتداد خط برش، زاویه شیب ظاهری (Apparent Dip) و گسل‌ها"
                        else
                            "Interactive structural profile based on section bearing, apparent dips, stratigraphy & faults",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.5.sp
                    )
                }
            }
        }

        // Section Setup Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isFarsiMode) "تنظیمات خط برش و مشخصات توپوگرافی" else "Section Line & Topography Setup",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = sectionAzimuthStr,
                        onValueChange = { sectionAzimuthStr = it },
                        label = { Text(if (isFarsiMode) "آزیموت خط برش (°)" else "Section Bearing (°)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = sectionLengthStr,
                        onValueChange = { sectionLengthStr = it },
                        label = { Text(if (isFarsiMode) "طول مقطع (m)" else "Length (m)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = baseElevationStr,
                        onValueChange = { baseElevationStr = it },
                        label = { Text(if (isFarsiMode) "ارتفاع مبنا (m)" else "Base Elev (m)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = maxElevationRelStr,
                        onValueChange = { maxElevationRelStr = it },
                        label = { Text(if (isFarsiMode) "برجستگی نسبی (m)" else "Rel. Relief (m)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Topo Type Selection
                Text(
                    text = if (isFarsiMode) "فرم توپوگرافی سطح زمین:" else "Ground Topography Form:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TopoProfileType.values().forEach { t ->
                        FilterChip(
                            selected = selectedTopoType == t,
                            onClick = { selectedTopoType = t },
                            label = { Text(if (isFarsiMode) t.faName else t.enName, fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Live Interactive Canvas View
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF00E676), CircleShape)
                        )
                        Text(
                            text = if (isFarsiMode) "پیش‌نمایش شماتیک مقطع زمین‌شناسی" else "Live Geological Cross-Section",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Az: ${sectionAzimuth.toInt()}° | L: ${sectionLength.toInt()}m",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // The Canvas Renderer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color(0xFF1E222B), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF374151), RoundedCornerShape(12.dp))
                ) {
                    GeologicalCrossSectionCanvas(
                        sectionAzimuth = sectionAzimuth,
                        sectionLength = sectionLength,
                        baseElevation = baseElevation,
                        maxRelElevation = maxRelElevation,
                        topoType = selectedTopoType,
                        formations = formations,
                        fault = GeoFault(
                            enabled = hasFault,
                            faultName = "F-1",
                            positionRatio = 0.58f,
                            dipAngle = faultDip,
                            dipDirectionRight = faultDipRight,
                            verticalThrow = faultThrow
                        ),
                        isFarsiMode = isFarsiMode
                    )
                }

                // Legend row
                Text(
                    text = if (isFarsiMode) "راهنمای لایه‌ها و سازندها:" else "Formation Legend & Apparent Dips:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    formations.forEachIndexed { idx, layer ->
                        val apparentDip = calculateApparentDip(layer.trueDip, layer.trueDipDir, sectionAzimuth)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(layer.pattern.color, RoundedCornerShape(3.dp))
                                )
                                Text(
                                    text = "${idx + 1}. ${layer.name}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "h=${layer.trueThickness.toInt()}m",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Dip: ${layer.trueDip.toInt()}° ➔ App: ${String.format(Locale.US, "%.1f°", apparentDip)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fault Control Card
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
                        text = if (isFarsiMode) "گسل ساختاری در مقطع (Fault Offset)" else "Structural Fault in Section",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Switch(
                        checked = hasFault,
                        onCheckedChange = { hasFault = it }
                    )
                }

                if (hasFault) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = faultDipStr,
                            onValueChange = { faultDipStr = it },
                            label = { Text(if (isFarsiMode) "شیب صفحه گسل (°)" else "Fault Dip (°)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = faultThrowStr,
                            onValueChange = { faultThrowStr = it },
                            label = { Text(if (isFarsiMode) "میزان جابه‌جایی (m)" else "Fault Throw (m)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFarsiMode) "جهت شیب گسل:" else "Fault Dip Direction:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = !faultDipRight,
                            onClick = { faultDipRight = false },
                            label = { Text(if (isFarsiMode) "به سمت چپ (West/SW)" else "Left (SW)", fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = faultDipRight,
                            onClick = { faultDipRight = true },
                            label = { Text(if (isFarsiMode) "به سمت راست (East/NE)" else "Right (NE)", fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Layer Management Card
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
                        text = if (isFarsiMode) "مدیریت لایه‌ها و سازندها" else "Layer & Formation Manager",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = {
                            newLayerName = ""
                            newLayerThickness = "100"
                            newLayerDip = "30"
                            newLayerDipDir = sectionAzimuthStr
                            showAddLayerDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFarsiMode) "افزودن لایه" else "Add Layer", fontSize = 11.sp)
                    }
                }

                formations.forEachIndexed { index, layer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(layer.pattern.color, RoundedCornerShape(6.dp))
                            )
                            Column {
                                Text(
                                    text = layer.name,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${layer.pattern.enName} | h=${layer.trueThickness}m | Strike/Dip: ${layer.trueDipDir.toInt()}°/${layer.trueDip.toInt()}°",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (formations.size > 1) {
                                    formations = formations.filterIndexed { i, _ -> i != index }
                                }
                            },
                            enabled = formations.size > 1,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = if (formations.size > 1) MaterialTheme.colorScheme.error else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons: Copy / Share Cross-Section Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val sb = StringBuilder()
                    sb.append("=== GEOLOGICAL CROSS-SECTION REPORT ===\n")
                    sb.append("Section Azimuth: $sectionAzimuth°\n")
                    sb.append("Section Length: $sectionLength m\n")
                    sb.append("Base Elevation: $baseElevation m (Relief: $maxRelElevation m)\n")
                    sb.append("Topography Type: ${selectedTopoType.enName}\n")
                    if (hasFault) {
                        sb.append("Fault: F-1 (Dip: $faultDip°, Throw: $faultThrow m)\n")
                    }
                    sb.append("\n--- STRATIGRAPHIC UNITS ---\n")
                    formations.forEachIndexed { i, f ->
                        val appDip = calculateApparentDip(f.trueDip, f.trueDipDir, sectionAzimuth)
                        sb.append("${i + 1}. ${f.name} (${f.pattern.enName}): TrueThick=${f.trueThickness}m, TrueDip=${f.trueDip}°, DipDir=${f.trueDipDir}°, ApparentDip=${String.format(Locale.US, "%.1f°", appDip)}\n")
                    }
                    clipboardManager.setText(AnnotatedString(sb.toString()))
                    android.widget.Toast.makeText(context, if (isFarsiMode) "خلاصه مشخصات مقطع کپی شد" else "Cross-section data copied", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isFarsiMode) "کپی گزارش مقطع" else "Copy Section Summary", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Add Layer Dialog
    if (showAddLayerDialog) {
        AlertDialog(
            onDismissRequest = { showAddLayerDialog = false },
            title = { Text(if (isFarsiMode) "افزودن سازند جدید به مقطع" else "Add Formation to Section", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newLayerName,
                        onValueChange = { newLayerName = it },
                        label = { Text(if (isFarsiMode) "نام سازند یا لایه" else "Formation Name", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Text(if (isFarsiMode) "نوع لیتولوژی و الگو:" else "Lithology Pattern:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LithoPattern.values().forEach { lp ->
                            FilterChip(
                                selected = newLayerPattern == lp,
                                onClick = { newLayerPattern = lp },
                                label = { Text(if (isFarsiMode) lp.faName else lp.enName, fontSize = 9.5.sp) },
                                shape = RoundedCornerShape(6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newLayerThickness,
                            onValueChange = { newLayerThickness = it },
                            label = { Text(if (isFarsiMode) "ضخامت واقعی (m)" else "True Thick (m)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = newLayerDip,
                            onValueChange = { newLayerDip = it },
                            label = { Text(if (isFarsiMode) "شیب واقعی (°)" else "True Dip (°)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = newLayerDipDir,
                        onValueChange = { newLayerDipDir = it },
                        label = { Text(if (isFarsiMode) "جهت شیب حقیقی (° DipDir)" else "True Dip Direction (°)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val th = newLayerThickness.toDoubleOrNull() ?: 80.0
                        val d = (newLayerDip.toDoubleOrNull() ?: 30.0).coerceIn(0.0, 90.0)
                        val dd = (newLayerDipDir.toDoubleOrNull() ?: 45.0).coerceIn(0.0, 360.0)
                        val nm = if (newLayerName.isBlank()) {
                            if (isFarsiMode) "سازند ${newLayerPattern.faName}" else "${newLayerPattern.enName} Unit"
                        } else newLayerName

                        formations = formations + GeoFormationLayer(
                            id = (formations.size + 1).toString(),
                            name = nm,
                            pattern = newLayerPattern,
                            trueThickness = th,
                            trueDip = d,
                            trueDipDir = dd
                        )
                        showAddLayerDialog = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isFarsiMode) "تأیید و افزودن" else "Add Layer", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLayerDialog = false }) {
                    Text(if (isFarsiMode) "انصراف" else "Cancel", fontSize = 11.sp)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Geological Cross-Section Canvas
// -------------------------------------------------------------

@Composable
fun GeologicalCrossSectionCanvas(
    sectionAzimuth: Double,
    sectionLength: Double,
    baseElevation: Double,
    maxRelElevation: Double,
    topoType: TopoProfileType,
    formations: List<GeoFormationLayer>,
    fault: GeoFault,
    isFarsiMode: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        val w = size.width
        val h = size.height

        val leftPad = 40f
        val rightPad = 25f
        val topPad = 35f
        val bottomPad = 30f

        val plotW = w - leftPad - rightPad
        val plotH = h - topPad - bottomPad

        // 1. Draw Background Grid & Elevation Axes
        val gridColor = Color(0xFF2C3240)
        val axisTextColor = android.graphics.Color.argb(180, 200, 210, 225)
        val paint = android.graphics.Paint().apply {
            color = axisTextColor
            textSize = 22f
            isAntiAlias = true
        }

        // Draw horizontal elevation lines (4 grid ticks)
        for (i in 0..3) {
            val ratio = i / 3f
            val y = topPad + plotH * ratio
            val elev = (baseElevation + maxRelElevation * (1f - ratio)).toInt()

            drawLine(
                color = gridColor,
                start = Offset(leftPad, y),
                end = Offset(leftPad + plotW, y),
                strokeWidth = 1f
            )

            drawContext.canvas.nativeCanvas.drawText(
                "$elev m",
                5f,
                y + 7f,
                paint
            )
        }

        // Distance ticks on bottom
        for (i in 0..4) {
            val ratio = i / 4f
            val x = leftPad + plotW * ratio
            val dist = (sectionLength * ratio).toInt()

            drawLine(
                color = gridColor,
                start = Offset(x, topPad),
                end = Offset(x, topPad + plotH),
                strokeWidth = 1f
            )

            drawContext.canvas.nativeCanvas.drawText(
                "$dist m",
                x - 20f,
                topPad + plotH + 24f,
                paint
            )
        }

        // 2. Generate Topographic Surface Curve
        val topoPoints = mutableListOf<Offset>()
        val numSamples = 60
        for (i in 0..numSamples) {
            val r = i.toFloat() / numSamples
            val x = leftPad + plotW * r
            val topoNorm = getNormalizedTopo(r, topoType)
            // topoNorm in [0, 1] where 1 is highest peak
            val y = topPad + (1f - topoNorm) * (plotH * 0.45f)
            topoPoints.add(Offset(x, y))
        }

        // 3. Draw Stratigraphic Layers (Subsurface Bands)
        var cumulativeDepth = 0f
        val faultX = leftPad + plotW * fault.positionRatio

        formations.forEachIndexed { _, layer ->
            val apparentDip = calculateApparentDip(layer.trueDip, layer.trueDipDir, sectionAzimuth)
            val radAppDip = Math.toRadians(apparentDip)
            val dipSlope = tan(radAppDip).toFloat()

            val layerThicknessPx = (layer.trueThickness / (sectionLength * 0.5) * plotH).toFloat().coerceIn(18f, 65f)
            val startY1 = topPad + plotH * 0.45f + cumulativeDepth
            val startY2 = startY1 + layerThicknessPx

            // Build Layer 1 Polygon
            val layerPath = Path()
            // Follow top curve & project downwards with apparent dip
            for (p in topoPoints) {
                val dx = p.x - leftPad
                var dipOffset = dx * (dipSlope * 0.35f)
                if (apparentDip < 0) dipOffset = -dipOffset

                // Fault displacement offset
                if (fault.enabled && p.x > faultX) {
                    val throwPx = (fault.verticalThrow / 100.0 * 25.0).toFloat()
                    dipOffset += throwPx
                }

                val currentY = (startY1 + dipOffset).coerceIn(p.y, topPad + plotH)
                if (p.x == topoPoints.first().x) {
                    layerPath.moveTo(p.x, currentY)
                } else {
                    layerPath.lineTo(p.x, currentY)
                }
            }

            // Bottom line
            for (j in topoPoints.indices.reversed()) {
                val p = topoPoints[j]
                val dx = p.x - leftPad
                var dipOffset = dx * (dipSlope * 0.35f)
                if (apparentDip < 0) dipOffset = -dipOffset

                if (fault.enabled && p.x > faultX) {
                    val throwPx = (fault.verticalThrow / 100.0 * 25.0).toFloat()
                    dipOffset += throwPx
                }

                val currentY2 = (startY2 + dipOffset).coerceIn(p.y, topPad + plotH)
                layerPath.lineTo(p.x, currentY2)
            }
            layerPath.close()

            // Draw layer body with color & pattern
            drawPath(
                path = layerPath,
                color = layer.pattern.color.copy(alpha = 0.55f)
            )

            // Draw boundary line
            drawPath(
                path = layerPath,
                color = layer.pattern.color.copy(alpha = 0.9f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
            )

            cumulativeDepth += layerThicknessPx
        }

        // 4. Draw Fault Line if enabled
        if (fault.enabled) {
            val faultRad = Math.toRadians(fault.dipAngle)
            val faultDx = (plotH * 0.7f / tan(faultRad)).toFloat() * (if (fault.dipDirectionRight) 1 else -1)

            val faultTop = Offset(faultX, topPad)
            val faultBottom = Offset(faultX + faultDx, topPad + plotH)

            drawLine(
                color = Color(0xFFFF5252),
                start = faultTop,
                end = faultBottom,
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
            )

            // Fault Label
            val faultPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#FF5252")
                textSize = 24f
                isFakeBoldText = true
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText("Fault ${fault.faultName} (${fault.dipAngle.toInt()}°)", faultX - 10f, topPad - 8f, faultPaint)
        }

        // 5. Draw Ground Topographic Surface Line & Shading
        val groundPath = Path()
        groundPath.moveTo(topoPoints.first().x, topoPoints.first().y)
        for (i in 1 until topoPoints.size) {
            groundPath.lineTo(topoPoints[i].x, topoPoints[i].y)
        }

        // Draw ground stroke
        drawPath(
            path = groundPath,
            color = Color(0xFF69F0AE),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f)
        )

        // Draw Section End Directions (e.g. SW / NE)
        val dirPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#69F0AE")
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val startDir = getCompassBearingLabel(sectionAzimuth + 180.0)
        val endDir = getCompassBearingLabel(sectionAzimuth)
        drawContext.canvas.nativeCanvas.drawText("A ($startDir)", leftPad, topPad - 10f, dirPaint)
        drawContext.canvas.nativeCanvas.drawText("A' ($endDir)", leftPad + plotW - 40f, topPad - 10f, dirPaint)
    }
}

// -------------------------------------------------------------
// Helper Calculations
// -------------------------------------------------------------

fun calculateApparentDip(trueDipDeg: Double, trueDipDirDeg: Double, sectionAzimuthDeg: Double): Double {
    val alphaTrue = Math.toRadians(trueDipDeg)
    val beta = Math.toRadians(abs(sectionAzimuthDeg - trueDipDirDeg))
    val tanApparent = tan(alphaTrue) * cos(beta)
    return Math.toDegrees(atan(abs(tanApparent)))
}

fun getNormalizedTopo(xNorm: Float, type: TopoProfileType): Float {
    return when (type) {
        TopoProfileType.HILL -> sin(xNorm * Math.PI.toFloat()).coerceIn(0f, 1f)
        TopoProfileType.VALLEY -> (1f - sin(xNorm * Math.PI.toFloat())).coerceIn(0f, 1f)
        TopoProfileType.SLOPE -> xNorm
        TopoProfileType.PLATEAU -> 0.75f
        TopoProfileType.ANTICLINE_LAND -> (sin(xNorm * Math.PI.toFloat() * 2f) * 0.4f + 0.5f).coerceIn(0f, 1f)
    }
}

fun getCompassBearingLabel(deg: Double): String {
    val d = (deg % 360.0 + 360.0) % 360.0
    return when {
        d in 337.5..360.0 || d in 0.0..22.5 -> "N"
        d in 22.5..67.5 -> "NE"
        d in 67.5..112.5 -> "E"
        d in 112.5..157.5 -> "SE"
        d in 157.5..202.5 -> "S"
        d in 202.5..247.5 -> "SW"
        d in 247.5..292.5 -> "W"
        else -> "NW"
    }
}
