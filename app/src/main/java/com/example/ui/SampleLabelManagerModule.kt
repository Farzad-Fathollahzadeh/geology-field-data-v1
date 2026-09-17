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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
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
import com.example.GeologyViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

// -------------------------------------------------------------
// Sample Bag Data Types
// -------------------------------------------------------------

enum class SampleBagType(val faName: String, val enName: String, val prefix: String) {
    ROCK_CHIP("خرده‌سنگی (Rock Chip)", "Rock Chip", "RC"),
    CHANNEL("شیاری (Channel)", "Channel", "CH"),
    DRILL_CORE("مغزه حفاری (Core)", "Drill Core", "DC"),
    GRAB("توده‌ای دستی (Grab)", "Grab", "GB"),
    STREAM_SEDIMENT("رسوب آبراهه‌ای", "Stream Sediment", "SS"),
    SOIL_GEOCHEM("خاک‌شناسی (Soil)", "Soil Geochem", "SL"),
    HEAVY_MINERAL("کانی سنگین (Pan)", "Heavy Mineral", "HM")
}

enum class SampleLabAnalysis(val label: String, val code: String) {
    ICP_MS_4ACID("ICP-MS (4-Acid Digest / 48 Elements)", "ICP48"),
    FIRE_ASSAY_AU("Fire Assay Gold (Au-AA26 / 50g)", "FA-Au"),
    XRF_MAJORS("XRF Major Oxides + Trace", "XRF-01"),
    THIN_SECTION("مقطع نازک پتروگرافی (Thin Section)", "TS-PET"),
    POLISHED_SECTION("مقطع صیقلی کانه‌نگاری (Polished)", "PS-MIN"),
    XRD_MINERAL("آنالیز پراش پرتو ایکس (XRD)", "XRD-01")
}

enum class SampleQcType(val faName: String, val enName: String, val badgeColor: Color) {
    ROUTINE("نمونه روتین (Routine)", "Routine", Color(0xFF4CAF50)),
    FIELD_DUPLICATE("دوبلیکیت تکرار (Field Duplicate)", "Field Duplicate", Color(0xFFFF9800)),
    CRM_STANDARD("استاندارد مرجع (CRM Standard)", "CRM Standard", Color(0xFF2196F3)),
    BLANK_QC("بلانک شاهد (Blank QC)", "Blank QC", Color(0xFF9C27B0))
}

// -------------------------------------------------------------
// Sample Bag Tag Generator Module
// -------------------------------------------------------------

@Composable
fun SampleLabelManagerModule(
    viewModel: GeologyViewModel,
    isFarsiMode: Boolean = true
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val activeStation by viewModel.activeStation.collectAsStateWithLifecycle()

    var projectCode by remember { mutableStateOf(activeProject?.name ?: "PRJ-2026") }
    var stationCode by remember { mutableStateOf(activeStation?.stationCode ?: "STN-01") }
    var sampleNumberStr by remember { mutableStateOf("101") }
    var samplerName by remember { mutableStateOf("F. Fathollazadeh") }
    var selectedType by remember { mutableStateOf(SampleBagType.ROCK_CHIP) }
    var selectedAnalysis by remember { mutableStateOf(SampleLabAnalysis.ICP_MS_4ACID) }
    var selectedQc by remember { mutableStateOf(SampleQcType.ROUTINE) }
    var lithologyNotes by remember { mutableStateOf("Hydrothermal Quartz-Pyrite Veinlet in Dacite") }
    var utmCoordsStr by remember { mutableStateOf("39S 542310 E / 3892450 N (1840m)") }

    val currentDate = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()) }

    val fullSampleTag = "${projectCode.take(4).uppercase()}-${selectedType.prefix}-${stationCode}-${sampleNumberStr}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFarsiMode) "شناسنامه و بارکد کیسه نمونه‌های صحرایی" else "Sample Bag Tag & Barcode Master",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = if (isFarsiMode)
                            "تولید برچسب استاندارد ضدآب، بارکد ماتریسی و شناسنامه نمونه‌های اکتشافی با کنترل کیفیت QA/QC"
                        else
                            "Generate professional sample passport tags, matrix barcodes & QA/QC dispatch records",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Live Graphic Passport Tag Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF384252))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
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
                                .background(selectedQc.badgeColor, CircleShape)
                        )
                        Text(
                            text = if (isFarsiMode) "پیش‌نمایش برچسب شناسنامه کیسه نمونه" else "Field Sample Passport Tag Preview",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        color = selectedQc.badgeColor.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, selectedQc.badgeColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = if (isFarsiMode) selectedQc.faName else selectedQc.enName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = selectedQc.badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Barcode + QR Matrix Canvas Generator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Matrix QR Code Graphic
                        SampleQrMatrixCanvas(
                            sampleCode = fullSampleTag,
                            modifier = Modifier.size(100.dp)
                        )

                        // Right: Sample Tag Text & Barcode Lines
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = fullSampleTag,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black
                                )
                                Text(
                                    text = "${selectedType.enName} | ${selectedAnalysis.code}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF455A64)
                                )
                                Text(
                                    text = "Date: $currentDate",
                                    fontSize = 9.sp,
                                    color = Color(0xFF78909C)
                                )
                            }

                            // Linear Barcode Simulation
                            SampleLinearBarcodeCanvas(
                                code = fullSampleTag,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                            )
                        }
                    }
                }

                // Sample Tag Attribute Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF262F3E), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isFarsiMode) "پروژه / ایستگاه:" else "Project / Station:", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        Text("$projectCode / $stationCode", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isFarsiMode) "موقعیت مکانی UTM:" else "UTM Coordinates:", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        Text(utmCoordsStr, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF81D4FA))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isFarsiMode) "آنالیز درخواستی آزمایشگاه:" else "Lab Package:", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        Text(selectedAnalysis.label, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isFarsiMode) "توصیف صحرایی نمونه:" else "Field Lithology:", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        Text(lithologyNotes, fontSize = 9.5.sp, color = Color(0xFFE0E0E0))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isFarsiMode) "نمونه‌بردار:" else "Sampler:", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        Text(samplerName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Configuration Form
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
                    text = if (isFarsiMode) "مشخصات و متادیتای نمونه" else "Sample Metadata Entry",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = projectCode,
                        onValueChange = { projectCode = it },
                        label = { Text(if (isFarsiMode) "کد پروژه" else "Project Code", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = stationCode,
                        onValueChange = { stationCode = it },
                        label = { Text(if (isFarsiMode) "کد ایستگاه" else "Station", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = sampleNumberStr,
                        onValueChange = { sampleNumberStr = it },
                        label = { Text(if (isFarsiMode) "شماره نمونه" else "Sample #", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Sample Type Chips
                Text(if (isFarsiMode) "نوع روش نمونه‌برداری:" else "Sampling Method:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SampleBagType.values().forEach { st ->
                        FilterChip(
                            selected = selectedType == st,
                            onClick = { selectedType = st },
                            label = { Text(if (isFarsiMode) st.faName else st.enName, fontSize = 9.5.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // QA/QC Type Chips
                Text(if (isFarsiMode) "وضعیت کنترل کیفیت (QA/QC):" else "QA/QC Classification:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SampleQcType.values().forEach { qc ->
                        FilterChip(
                            selected = selectedQc == qc,
                            onClick = { selectedQc = qc },
                            label = { Text(if (isFarsiMode) qc.faName else qc.enName, fontSize = 9.5.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Lab Analysis Method Chips
                Text(if (isFarsiMode) "روش آنالیز درخواستی آزمایشگاه:" else "Requested Lab Method:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SampleLabAnalysis.values().forEach { la ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAnalysis = la }
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedAnalysis == la,
                                onClick = { selectedAnalysis = la }
                            )
                            Text(
                                text = la.label,
                                fontSize = 11.sp,
                                color = if (selectedAnalysis == la) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = lithologyNotes,
                    onValueChange = { lithologyNotes = it },
                    label = { Text(if (isFarsiMode) "توصیف صحرایی و ساختار کانسنگ" else "Lithology / Ore Description", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = samplerName,
                    onValueChange = { samplerName = it },
                    label = { Text(if (isFarsiMode) "نام نمونه‌بردار" else "Sampler Name", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val passport = """
                        ========================================
                        GEOFIELD PRO SAMPLE PASSPORT TAG
                        ========================================
                        SAMPLE ID    : $fullSampleTag
                        PROJECT      : $projectCode
                        STATION      : $stationCode
                        SAMPLE TYPE  : ${selectedType.enName} (${selectedType.faName})
                        QA/QC STATUS : ${selectedQc.enName}
                        DATE / TIME  : $currentDate
                        COORDINATES  : $utmCoordsStr
                        LAB PACKAGE  : ${selectedAnalysis.label}
                        LITHOLOGY    : $lithologyNotes
                        SAMPLER      : $samplerName
                        ========================================
                    """.trimIndent()
                    clipboardManager.setText(AnnotatedString(passport))
                    android.widget.Toast.makeText(context, if (isFarsiMode) "اطلاعات شناسنامه نمونه کپی شد" else "Sample passport tag copied", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isFarsiMode) "کپی متن شناسنامه" else "Copy Tag Info", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// -------------------------------------------------------------
// Barcode & QR Code Canvas Graphics
// -------------------------------------------------------------

@Composable
fun SampleQrMatrixCanvas(
    sampleCode: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val matrixSize = 15
        val cellSize = w / matrixSize

        // Draw Corner Finder Patterns (Top-Left, Top-Right, Bottom-Left)
        drawFinderPattern(0, 0, cellSize)
        drawFinderPattern(matrixSize - 4, 0, cellSize)
        drawFinderPattern(0, matrixSize - 4, cellSize)

        // Draw Deterministic Hash Grid
        val hash = sampleCode.hashCode()
        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                // Skip finder patterns
                val inTopLeft = r < 5 && c < 5
                val inTopRight = r < 5 && c >= matrixSize - 5
                val inBottomLeft = r >= matrixSize - 5 && c < 5

                if (!inTopLeft && !inTopRight && !inBottomLeft) {
                    val bit = ((hash xor (r * 31 + c * 17)) and (1 shl ((r + c) % 16))) != 0
                    if (bit) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 0.95f, cellSize * 0.95f)
                        )
                    }
                }
            }
        }
    }
}

fun DrawScope.drawFinderPattern(row: Int, col: Int, cellSize: Float) {
    val x = col * cellSize
    val y = row * cellSize
    val pSize = cellSize * 4f

    // Outer box
    drawRect(
        color = Color.Black,
        topLeft = Offset(x, y),
        size = Size(pSize, pSize)
    )
    // White hollow
    drawRect(
        color = Color.White,
        topLeft = Offset(x + cellSize * 0.7f, y + cellSize * 0.7f),
        size = Size(pSize - cellSize * 1.4f, pSize - cellSize * 1.4f)
    )
    // Inner solid center
    drawRect(
        color = Color.Black,
        topLeft = Offset(x + cellSize * 1.3f, y + cellSize * 1.3f),
        size = Size(pSize - cellSize * 2.6f, pSize - cellSize * 2.6f)
    )
}

@Composable
fun SampleLinearBarcodeCanvas(
    code: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bars = 36
        val barWidth = w / (bars * 1.3f)

        val hash = code.hashCode()
        for (i in 0 until bars) {
            val bit = ((hash xor (i * 29)) and 1) == 0 || i % 3 == 0 || i == 0 || i == bars - 1
            if (bit) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(i * barWidth * 1.3f, 0f),
                    size = Size(barWidth, h)
                )
            }
        }
    }
}
