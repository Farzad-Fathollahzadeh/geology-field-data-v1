package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
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
import java.util.Locale

// -------------------------------------------------------------
// Mining Economics & Cut-off Grade Calculator Module
// -------------------------------------------------------------

@Composable
fun MiningEconomicsModule(
    isFarsiMode: Boolean = true
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Commodity Grades
    var gradeCuStr by remember { mutableStateOf("0.85") } // %
    var gradeAuStr by remember { mutableStateOf("0.45") } // g/t (ppm)
    var gradeMoStr by remember { mutableStateOf("0.025") } // % (or ppm)
    var gradeAgStr by remember { mutableStateOf("12.0") } // g/t

    // Commodity Market Prices
    var priceCuStr by remember { mutableStateOf("9200") } // $/tonne ($4.17/lb)
    var priceAuStr by remember { mutableStateOf("2650") } // $/troy oz ($85.2/g)
    var priceMoStr by remember { mutableStateOf("45000") } // $/tonne ($20.4/lb)
    var priceAgStr by remember { mutableStateOf("31.5") } // $/troy oz ($1.01/g)

    // Metallurgical Recovery %
    var recCuStr by remember { mutableStateOf("88") } // %
    var recAuStr by remember { mutableStateOf("72") } // %
    var recMoStr by remember { mutableStateOf("65") } // %
    var recAgStr by remember { mutableStateOf("60") } // %

    // Operating Costs ($/tonne processed)
    var miningCostStr by remember { mutableStateOf("3.50") } // $/tonne ore
    var millingCostStr by remember { mutableStateOf("9.20") } // $/tonne processed
    var gaCostStr by remember { mutableStateOf("2.30") } // G&A $/tonne
    var transportRefiningStr by remember { mutableStateOf("2.80") } // $/tonne

    // Parsed Values
    val gCu = gradeCuStr.toDoubleOrNull() ?: 0.85
    val gAu = gradeAuStr.toDoubleOrNull() ?: 0.45
    val gMo = gradeMoStr.toDoubleOrNull() ?: 0.025
    val gAg = gradeAgStr.toDoubleOrNull() ?: 12.0

    val pCu = priceCuStr.toDoubleOrNull() ?: 9200.0
    val pAu = priceAuStr.toDoubleOrNull() ?: 2650.0
    val pMo = priceMoStr.toDoubleOrNull() ?: 45000.0
    val pAg = priceAgStr.toDoubleOrNull() ?: 31.5

    val rCu = (recCuStr.toDoubleOrNull() ?: 88.0) / 100.0
    val rAu = (recAuStr.toDoubleOrNull() ?: 72.0) / 100.0
    val rMo = (recMoStr.toDoubleOrNull() ?: 65.0) / 100.0
    val rAg = (recAgStr.toDoubleOrNull() ?: 60.0) / 100.0

    val costMining = miningCostStr.toDoubleOrNull() ?: 3.50
    val costMilling = millingCostStr.toDoubleOrNull() ?: 9.20
    val costGA = gaCostStr.toDoubleOrNull() ?: 2.30
    val costRefining = transportRefiningStr.toDoubleOrNull() ?: 2.80

    val totalOpexPerTonne = costMining + costMilling + costGA + costRefining

    // Revenue per tonne of ore
    // Cu: % -> tonne of Cu per 100 tonne of ore = gCu/100 * pCu * rCu
    val revCu = (gCu / 100.0) * pCu * rCu
    // Au: g/t -> (gAu / 31.1035) * pAu * rAu
    val revAu = (gAu / 31.1034768) * pAu * rAu
    // Mo: % -> (gMo / 100.0) * pMo * rMo
    val revMo = (gMo / 100.0) * pMo * rMo
    // Ag: g/t -> (gAg / 31.1035) * pAg * rAg
    val revAg = (gAg / 31.1034768) * pAg * rAg

    val totalNSRPerTonne = revCu + revAu + revMo + revAg
    val netMarginPerTonne = totalNSRPerTonne - totalOpexPerTonne

    // Copper Equivalent (CuEq %)
    // CuEq % = gCu + (revAu / (pCu * rCu / 100.0)) + (revMo / (pCu * rCu / 100.0)) + (revAg / (pCu * rCu / 100.0))
    val cuPriceFactor = if (pCu * rCu > 0) pCu * rCu / 100.0 else 1.0
    val cuEq = if (cuPriceFactor > 0) totalNSRPerTonne / cuPriceFactor else 0.0

    // Gold Equivalent (AuEq g/t)
    val auPriceFactor = if (pAu * rAu > 0) (pAu / 31.1034768) * rAu else 1.0
    val auEq = if (auPriceFactor > 0) totalNSRPerTonne / auPriceFactor else 0.0

    // Break-even Cut-Off Grade (CuEq % and AuEq g/t)
    val breakEvenCuEq = if (cuPriceFactor > 0) totalOpexPerTonne / cuPriceFactor else 0.0
    val breakEvenAuEq = if (auPriceFactor > 0) totalOpexPerTonne / auPriceFactor else 0.0

    val df2 = DecimalFormat("#,##0.00")
    val df3 = DecimalFormat("#,##0.000")

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
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFFFB300),
                                    Color(0xFFFF6F00)
                                )
                            ),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFarsiMode) "محاسبه‌گر عیار معادل (CuEq/AuEq) و عیار حد کانسار" else "NSR, Equivalent Grade & Cut-Off Calculator",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = if (isFarsiMode)
                            "محاسبه ارزش ویژه کارخانه ذوب (NSR)، عیار معادل مس و طلا، عیار حد سربه سر (Break-even Cutoff) و حاشیه سود"
                        else
                            "Net Smelter Return (NSR), CuEq/AuEq multi-element normalization & break-even cut-off threshold",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Summary KPI Display Dashboard
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430)),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
            border = BorderStroke(1.dp, Color(0xFF384252))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isFarsiMode) "نتایج اقتصادی و عیارهای معادل کانسنگ" else "Economic Valuation & Normalized Equivalence",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCA28)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // CuEq KPI Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF263242), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF455A64), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isFarsiMode) "عیار معادل مس" else "Copper Equivalent",
                                fontSize = 10.sp,
                                color = Color(0xFFB0BEC5)
                            )
                            Text(
                                text = "${df2.format(cuEq)} %",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFF8A65)
                            )
                            Text(
                                text = "CuEq (Cu+Au+Mo+Ag)",
                                fontSize = 9.sp,
                                color = Color(0xFF90A4AE)
                            )
                        }
                    }

                    // AuEq KPI Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF263242), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF455A64), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isFarsiMode) "عیار معادل طلا" else "Gold Equivalent",
                                fontSize = 10.sp,
                                color = Color(0xFFB0BEC5)
                            )
                            Text(
                                text = "${df2.format(auEq)} g/t",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFD54F)
                            )
                            Text(
                                text = "AuEq (Au+Cu+Mo+Ag)",
                                fontSize = 9.sp,
                                color = Color(0xFF90A4AE)
                            )
                        }
                    }
                }

                // Revenue & Cut-off Breakdown Table
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161B22), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isFarsiMode) "ارزش ناخالص درجا (NSR):" else "Gross Value (NSR/t):", fontSize = 11.sp, color = Color(0xFF90A4AE))
                        Text("$${df2.format(totalNSRPerTonne)} / tonne", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isFarsiMode) "مجموع هزینه عملیاتی (OPEX):" else "Total OPEX/t:", fontSize = 11.sp, color = Color(0xFF90A4AE))
                        Text("$${df2.format(totalOpexPerTonne)} / tonne", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFAB91))
                    }
                    Divider(color = Color(0xFF30363D))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isFarsiMode) "حاشیه سود خالص (Net Margin):" else "Net Margin / Profit:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "$${df2.format(netMarginPerTonne)} / tonne",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = if (netMarginPerTonne >= 0) Color(0xFF00E676) else Color(0xFFFF5252)
                        )
                    }
                    Divider(color = Color(0xFF30363D))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isFarsiMode) "عیار حد سربه سر (Cut-off CuEq):" else "Break-even Cut-off (CuEq):", fontSize = 10.5.sp, color = Color(0xFFFFD54F))
                        Text("${df3.format(breakEvenCuEq)} % CuEq", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isFarsiMode) "عیار حد سربه سر (Cut-off AuEq):" else "Break-even Cut-off (AuEq):", fontSize = 10.5.sp, color = Color(0xFFFFD54F))
                        Text("${df3.format(breakEvenAuEq)} g/t AuEq", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                    }
                }
            }
        }

        // Grade & Recovery Inputs
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isFarsiMode) "عیار کانسار، بازیابی و قیمت‌های فلزات" else "Grades, Recoveries & Metal Prices",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Copper (Cu) Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = gradeCuStr,
                        onValueChange = { gradeCuStr = it },
                        label = { Text("Cu (%)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = recCuStr,
                        onValueChange = { recCuStr = it },
                        label = { Text("Cu Rec (%)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = priceCuStr,
                        onValueChange = { priceCuStr = it },
                        label = { Text("Cu ($/t)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Gold (Au) Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = gradeAuStr,
                        onValueChange = { gradeAuStr = it },
                        label = { Text("Au (g/t)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = recAuStr,
                        onValueChange = { recAuStr = it },
                        label = { Text("Au Rec (%)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = priceAuStr,
                        onValueChange = { priceAuStr = it },
                        label = { Text("Au ($/oz)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Molybdenum (Mo) & Silver (Ag) Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = gradeMoStr,
                        onValueChange = { gradeMoStr = it },
                        label = { Text("Mo (%)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = gradeAgStr,
                        onValueChange = { gradeAgStr = it },
                        label = { Text("Ag (g/t)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // OPEX Cost Setup
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isFarsiMode) "هزینه‌های استخراج و فرآوری (OPEX $/Tonne)" else "Operating Costs Breakdown ($/t)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = miningCostStr,
                        onValueChange = { miningCostStr = it },
                        label = { Text(if (isFarsiMode) "استخراج ($/t)" else "Mining", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = millingCostStr,
                        onValueChange = { millingCostStr = it },
                        label = { Text(if (isFarsiMode) "فرآوری ($/t)" else "Processing", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = gaCostStr,
                        onValueChange = { gaCostStr = it },
                        label = { Text(if (isFarsiMode) "عمومی G&A" else "G&A", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Copy Economics Summary
        Button(
            onClick = {
                val report = """
                    === ORE VALUATION & CUT-OFF REPORT ===
                    Cu: $gCu% (Rec: ${(rCu * 100).toInt()}%, Price: $$pCu/t) -> Rev: $${df2.format(revCu)}/t
                    Au: ${gAu}g/t (Rec: ${(rAu * 100).toInt()}%, Price: $$pAu/oz) -> Rev: $${df2.format(revAu)}/t
                    Mo: $gMo% (Rec: ${(rMo * 100).toInt()}%, Price: $$pMo/t) -> Rev: $${df2.format(revMo)}/t
                    Ag: ${gAg}g/t (Rec: ${(rAg * 100).toInt()}%, Price: $$pAg/oz) -> Rev: $${df2.format(revAg)}/t
                    ---------------------------------------
                    Gross NSR      : $${df2.format(totalNSRPerTonne)} / tonne
                    Total OPEX     : $${df2.format(totalOpexPerTonne)} / tonne
                    Net Margin     : $${df2.format(netMarginPerTonne)} / tonne
                    ---------------------------------------
                    Cu Equivalent  : ${df2.format(cuEq)} % CuEq
                    Au Equivalent  : ${df2.format(auEq)} g/t AuEq
                    Break-even Cut : ${df3.format(breakEvenCuEq)} % CuEq (${df3.format(breakEvenAuEq)} g/t AuEq)
                """.trimIndent()
                clipboardManager.setText(AnnotatedString(report))
                android.widget.Toast.makeText(context, if (isFarsiMode) "گزارش اقتصادی کپی شد" else "Ore valuation report copied", android.widget.Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isFarsiMode) "کپی گزارش کامل عیار معادل و NSR" else "Copy Full Economic Report", fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
