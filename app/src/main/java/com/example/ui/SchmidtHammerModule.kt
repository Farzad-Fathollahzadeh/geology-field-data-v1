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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import kotlin.math.*

// -------------------------------------------------------------
// Schmidt Rebound Hammer ISRM / ASTM D5873 Analyzer Module
// -------------------------------------------------------------

enum class HammerOrientation(val faName: String, val enName: String, val angleDeg: Int) {
    VERTICALLY_DOWN("عمود به پایین (کف / زمین - ۹۰°)", "Vertically Down (-90°)", -90),
    INCLINED_DOWN("مایل به پایین (-۴۵°)", "Inclined Down (-45°)", -45),
    HORIZONTAL("افقی (دیواره گمانه / ترانشه ۰°)", "Horizontal (0°)", 0),
    INCLINED_UP("مایل به بالا (+۴۵°)", "Inclined Up (+45°)", 45),
    VERTICALLY_UP("عمود به بالا (سقف تونل / سینه کار +۹۰°)", "Vertically Up (+90°)", 90)
}

enum class HammerType(val label: String, val impactEnergy: String) {
    TYPE_L("چکش اشمیت تیپ L (سنگ‌های ژئوتکنیکی ضعیف تا متوسط)", "0.735 Nm"),
    TYPE_N("چکش اشمیت تیپ N (سنگ‌های سخت و بتن سازه‌ای)", "2.207 Nm")
}

@Composable
fun SchmidtHammerModule(
    isFarsiMode: Boolean = true
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var hammerType by remember { mutableStateOf(HammerType.TYPE_L) }
    var orientation by remember { mutableStateOf(HammerOrientation.HORIZONTAL) }
    var rockDensityStr by remember { mutableStateOf("26.5") } // kN/m3 (2.7 g/cm3)
    var rockName by remember { mutableStateOf("Diorite / Andesite") }

    // 10 to 12 Schmidt impacts
    var impacts by remember {
        mutableStateOf(listOf(44, 46, 42, 48, 45, 52, 41, 47, 49, 43))
    }
    var newImpactValueStr by remember { mutableStateOf("") }

    // Corrections per ISRM (1978) & Deere & Miller (1966)
    // Angle correction delta R
    val angleCorrection = when (orientation) {
        HammerOrientation.VERTICALLY_DOWN -> 0.0
        HammerOrientation.INCLINED_DOWN -> 1.5
        HammerOrientation.HORIZONTAL -> 3.2
        HammerOrientation.INCLINED_UP -> 4.8
        HammerOrientation.VERTICALLY_UP -> 6.0
    }

    // ASTM / ISRM Outlier rejection:
    // Discard lowest 50% or calculate mean of top 50% / mean minus outliers
    val sortedImpacts = impacts.sorted()
    val topHalf = if (sortedImpacts.size >= 4) {
        sortedImpacts.subList(sortedImpacts.size / 2, sortedImpacts.size)
    } else {
        sortedImpacts
    }

    val meanRaw = if (impacts.isNotEmpty()) impacts.average() else 0.0
    val meanTopHalf = if (topHalf.isNotEmpty()) topHalf.average() else meanRaw
    val correctedR = (meanTopHalf + angleCorrection).coerceIn(10.0, 75.0)

    val gamma = (rockDensityStr.toDoubleOrNull() ?: 26.5) // kN/m3 (e.g. 26.5 kN/m3 = 2.7 g/cm3)
    val densityGcm3 = gamma / 9.81

    // Empirical UCS equations:
    // 1. Deere & Miller (1966): log10(UCS in MPa) = 0.00088 * gamma * R + 1.01 (gamma in kN/m3)
    val ucsMillerMPa = 10.0.pow(0.00088 * gamma * correctedR + 1.01)

    // 2. Barton & Choubey (1977) JCS: log10(JCS in MPa) = 0.00088 * gamma * R + 1.01
    val jcsMPa = ucsMillerMPa

    // 3. Sachpazis (1990) for L-Hammer: UCS (MPa) = 1.958 * R - 27.57
    val ucsSachpazisMPa = (1.958 * correctedR - 27.57).coerceAtLeast(5.0)

    // Rock hardness classification
    val hardnessClass = when {
        ucsMillerMPa > 250 -> if (isFarsiMode) "فوق‌العاده سخت (Extremely Strong - R6)" else "Extremely Strong (R6)"
        ucsMillerMPa > 100 -> if (isFarsiMode) "بسیار سخت (Very Strong - R5)" else "Very Strong (R5)"
        ucsMillerMPa > 50 -> if (isFarsiMode) "سخت (Strong - R4)" else "Strong (R4)"
        ucsMillerMPa > 25 -> if (isFarsiMode) "متوسط (Medium Strong - R3)" else "Medium Strong (R3)"
        ucsMillerMPa > 5 -> if (isFarsiMode) "ضعیف (Weak - R2)" else "Weak (R2)"
        else -> if (isFarsiMode) "بسیار ضعیف (Very Weak - R1)" else "Very Weak (R1)"
    }

    val df1 = DecimalFormat("#,##0.0")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card
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
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hardware,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFarsiMode) "تست چکش اشمیت و مقاومت سنگ (JCS / UCS)" else "Schmidt Rebound Hammer & JCS Estimator",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = if (isFarsiMode)
                            "ثبت ضربات صحرایی (۱۰ الی ۲۰ ضربه)، تصحیح زاویه طبق استاندارد ISRM، حذف مقادیر پرت و تخمین مقاومت فشاری تک‌محوری"
                        else
                            "ISRM & ASTM D5873 rebound logging, inclination angle compensation & Barton JCS / UCS estimation",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Result KPI Dashboard Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2530)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isFarsiMode) "نتایج محاسباتی مقاومت فشاری و عدد بازگشت (ISRM)" else "ISRM Rebound & Strength Results",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81D4FA)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Corrected R Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF263242), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isFarsiMode) "عدد بازگشت تصحیح‌شده (R)" else "Corrected R Value",
                                fontSize = 10.sp,
                                color = Color(0xFFB0BEC5)
                            )
                            Text(
                                text = df1.format(correctedR),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64B5F6)
                            )
                            Text(
                                text = "Raw Avg: ${df1.format(meanRaw)} (+${angleCorrection})",
                                fontSize = 9.sp,
                                color = Color(0xFF90A4AE)
                            )
                        }
                    }

                    // UCS / JCS Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF263242), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isFarsiMode) "مقاومت فشاری (UCS/JCS)" else "Estimated UCS (MPa)",
                                fontSize = 10.sp,
                                color = Color(0xFFB0BEC5)
                            )
                            Text(
                                text = "${df1.format(ucsMillerMPa)} MPa",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFB74D)
                            )
                            Text(
                                text = "Deere & Miller (1966)",
                                fontSize = 9.sp,
                                color = Color(0xFF90A4AE)
                            )
                        }
                    }
                }

                // Classification Badge
                Surface(
                    color = Color(0xFF2C3848),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isFarsiMode) "رده‌بندی سختی ISRM:" else "ISRM Rock Hardness:",
                            fontSize = 11.sp,
                            color = Color(0xFFB0BEC5)
                        )
                        Text(
                            text = hardnessClass,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                    }
                }
            }
        }

        // Hammer Configuration
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
                    text = if (isFarsiMode) "مشخصات دستگاه و جهت‌گیری ضربه" else "Hammer Setup & Impact Orientation",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Orientation Selector
                Text(
                    text = if (isFarsiMode) "زاویه ضربه چکش به سطح سنگ:" else "Impact Orientation Angle:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                HammerOrientation.values().forEach { orient ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { orientation = orient }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = orientation == orient,
                            onClick = { orientation = orient }
                        )
                        Text(
                            text = if (isFarsiMode) orient.faName else orient.enName,
                            fontSize = 11.sp,
                            fontWeight = if (orientation == orient) FontWeight.Bold else FontWeight.Normal,
                            color = if (orientation == orient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rockDensityStr,
                        onValueChange = { rockDensityStr = it },
                        label = { Text(if (isFarsiMode) "وزن مخصوص (kN/m³)" else "Unit Wt (kN/m³)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = rockName,
                        onValueChange = { rockName = it },
                        label = { Text(if (isFarsiMode) "نام جنس سنگ" else "Lithology", fontSize = 10.sp) },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Impact Readings Logger
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
                        text = if (isFarsiMode) "قرائت ضربات صحرایی (${impacts.size} ضربه)" else "Impact Readings (${impacts.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    TextButton(
                        onClick = {
                            impacts = listOf(40, 42, 45, 47, 44, 46, 50, 43, 48, 41)
                        }
                    ) {
                        Text(if (isFarsiMode) "بازنشانی پیش‌فرض" else "Reset Sample", fontSize = 10.sp)
                    }
                }

                // Add Impact Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newImpactValueStr,
                        onValueChange = { newImpactValueStr = it },
                        placeholder = { Text(if (isFarsiMode) "عدد ضربه جدید (۱۰-۷۰)" else "Value (10-70)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            val v = newImpactValueStr.toIntOrNull()
                            if (v != null && v in 5..80) {
                                impacts = impacts + v
                                newImpactValueStr = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFarsiMode) "افزودن" else "Add", fontSize = 11.sp)
                    }
                }

                // Impact Chips Flow
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    impacts.forEachIndexed { idx, imp ->
                        val isUsedInTopHalf = topHalf.contains(imp)
                        Surface(
                            color = if (isUsedInTopHalf) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isUsedInTopHalf) MaterialTheme.colorScheme.primary else Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "#${idx + 1}: $imp",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUsedInTopHalf) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            impacts = impacts.filterIndexed { i, _ -> i != idx }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Copy Schmidt Test Report Button
        Button(
            onClick = {
                val report = """
                    === SCHMIDT REBOUND HAMMER TEST (ISRM / ASTM D5873) ===
                    Rock Type: $rockName
                    Unit Weight (γ): $rockDensityStr kN/m³
                    Impact Direction: ${orientation.enName} (Correction: +$angleCorrection)
                    Impacts: ${impacts.joinToString(", ")}
                    Raw Average R: ${df1.format(meanRaw)}
                    Corrected R (ISRM top 50%): ${df1.format(correctedR)}
                    Estimated UCS (Deere & Miller): ${df1.format(ucsMillerMPa)} MPa
                    Estimated JCS (Barton): ${df1.format(jcsMPa)} MPa
                    ISRM Hardness Class: $hardnessClass
                """.trimIndent()
                clipboardManager.setText(AnnotatedString(report))
                android.widget.Toast.makeText(context, if (isFarsiMode) "گزارش آزمون اشمیت کپی شد" else "Schmidt test report copied", android.widget.Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isFarsiMode) "کپی گزارش آزمون چکش اشمیت" else "Copy Schmidt Test Report", fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
