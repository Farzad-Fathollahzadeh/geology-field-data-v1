package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun formatLuster(luster: String, isFarsi: Boolean): String {
    return when (luster.lowercase()) {
        "metallic" -> if (isFarsi) "فلزی (Metallic)" else "Metallic"
        "submetallic" -> if (isFarsi) "نیمه‌فلزی (Submetallic)" else "Submetallic"
        "non_metallic" -> if (isFarsi) "غیرفلزی (Non-metallic)" else "Non-metallic"
        else -> luster
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuickDiagnosticKey(
    isFarsiMode: Boolean,
    minerals: List<GeotechMineral>,
    quickFilteredList: List<GeotechMineral>,
    quickHardness: String,
    onHardnessChange: (String) -> Unit,
    quickLuster: String,
    onLusterChange: (String) -> Unit,
    quickMagnet: String,
    onMagnetChange: (String) -> Unit,
    quickAcid: Boolean,
    onAcidChange: (Boolean) -> Unit,
    quickSalty: Boolean,
    onSaltyChange: (Boolean) -> Unit,
    quickSoapy: Boolean,
    onSoapyChange: (Boolean) -> Unit,
    quickSheets: Boolean,
    onSheetsChange: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    Card(
         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
         border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
         shape = RoundedCornerShape(16.dp),
         modifier = Modifier.fillMaxWidth()
    ) {
         Column(
             modifier = Modifier.padding(14.dp),
             verticalArrangement = Arrangement.spacedBy(12.dp)
         ) {
             // Interactive Title & Tip
             Row(
                 verticalAlignment = Alignment.CenterVertically,
                 horizontalArrangement = Arrangement.spacedBy(8.dp)
             ) {
                 Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                 Column {
                     Text(
                         text = if (isFarsiMode) "کلید تشخیص گام‌به‌گام کانی‌ها" else "Dichotomous Mineral Key Assistant",
                         fontSize = 13.sp,
                         fontWeight = FontWeight.Bold,
                         color = MaterialTheme.colorScheme.primary
                     )
                     Text(
                         text = if (isFarsiMode) "با ویژگی‌های ساده (سختی، جلا، بارهای مغناطیسی) کانی‌ها را شناسایی کنید." else "Sift through basic properties to isolate the exact mineral.",
                         fontSize = 10.sp,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                 }
             }

             Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

             // 1. HARDNESS QUESTION (خراش‌پذیری)
             Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                 Text(
                     text = if (isFarsiMode) "💅 گام اول: کانی با چه وسیله‌ای خراش برمی‌دارد؟ (سختی)" else "💅 Step 1: What scratches the mineral? (Hardness)",
                     fontSize = 11.sp,
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.secondary
                 )

                 Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                     listOf(
                         "any" to (if (isFarsiMode) "معمولی / بدون انتخاب" else "Any / Not checked"),
                         "nail" to (if (isFarsiMode) "با ناخن آسیب می‌بیند (بسیار نرم: موهس کمتر از ۲.۵)" else "Scratched by Fingernail (Mohs <= 2.5)"),
                         "coin" to (if (isFarsiMode) "با ناخن خط نمی‌افتد ولی با سکه مسی خط برمی‌دارد (موهس ۲.۵ تا ۳.۵)" else "Scratched by Copper Coin (Mohs 2.5 - 3.5)"),
                         "knife" to (if (isFarsiMode) "با سکه خط نمی‌افتد ولی با چاقوی فولادی یا میخ خط می‌افتد (موهس ۳.۵ تا ۵.۵)" else "Scratched by Steel Knife (Mohs 3.5 - 5.5)"),
                         "glass" to (if (isFarsiMode) "بسیار سخت است، روی شیشه خط می‌اندازد (موهس بالای ۵.۵)" else "Scratches Glass Plates (Mohs > 5.5)")
                     ).forEach { (value, label) ->
                         val isSelected = quickHardness == value
                         Row(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .background(
                                     color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                                     shape = RoundedCornerShape(8.dp)
                                 )
                                 .border(
                                      width = 1.dp,
                                      color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                      shape = RoundedCornerShape(8.dp)
                                 )
                                 .clickable { onHardnessChange(value) }
                                 .padding(horizontal = 10.dp, vertical = 8.dp),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             RadioButton(
                                 selected = isSelected,
                                 onClick = { onHardnessChange(value) },
                                 colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                             )
                             Spacer(modifier = Modifier.width(6.dp))
                             Text(
                                 text = label,
                                 fontSize = 11.sp,
                                 fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                 color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                             )
                         }
                     }
                 }
             }

             // 2. LUSTER QUESTION (جلای انعکاسی)
             Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                 Text(
                     text = if (isFarsiMode) "⚡ گام دوم: آیا کانی جلای فلزی دارد؟ (یا براق تیره مانند برنز/طلا/سرب است؟)" else "⚡ Step 2: Does it have a Metallic Luster?",
                     fontSize = 11.sp,
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.secondary
                 )

                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.spacedBy(8.dp)
                 ) {
                     listOf(
                         "any" to (if (isFarsiMode) "فرقی ندارد" else "Any"),
                         "metallic" to (if (isFarsiMode) "بله، جلای فلزی دارد" else "Yes, Metallic"),
                         "non_metallic" to (if (isFarsiMode) "خیر، جلای غیرفلزی دارد" else "No, Non-Metallic")
                     ).forEach { (value, label) ->
                         val isSelected = quickLuster == value
                         Box(
                             modifier = Modifier
                                 .weight(1f)
                                 .background(
                                     color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                     shape = RoundedCornerShape(8.dp)
                                 )
                                 .border(
                                      width = 1.dp,
                                      color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                                      shape = RoundedCornerShape(8.dp)
                                 )
                                 .clickable { onLusterChange(value) }
                                 .padding(vertical = 8.dp),
                             contentAlignment = Alignment.Center
                         ) {
                             Text(
                                 text = label,
                                 fontSize = 11.sp,
                                 fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                 color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                 textAlign = TextAlign.Center
                             )
                         }
                     }
                 }
             }

             // 3. MAGNETISM QUESTION (مغناطیس)
             Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                 Text(
                     text = if (isFarsiMode) "🧲 گام سوم: آیا توسط آهنربا جذب می‌شود؟ (مغناطيس)" else "🧲 Step 3: Is it attracted by a Magnet? (Magnetism)",
                     fontSize = 11.sp,
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.secondary
                 )

                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.spacedBy(8.dp)
                 ) {
                     listOf(
                         "any" to (if (isFarsiMode) "فرقی ندارد" else "Any"),
                         "yes" to (if (isFarsiMode) "بله، مغناطیسی" else "Yes"),
                         "no" to (if (isFarsiMode) "خیر، غیرمغناطیسی" else "No")
                     ).forEach { (value, label) ->
                         val isSelected = quickMagnet == value
                         Box(
                             modifier = Modifier
                                 .weight(1f)
                                 .background(
                                     color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                     shape = RoundedCornerShape(8.dp)
                                 )
                                 .border(
                                      width = 1.dp,
                                      color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                                      shape = RoundedCornerShape(8.dp)
                                 )
                                 .clickable { onMagnetChange(value) }
                                 .padding(vertical = 8.dp),
                             contentAlignment = Alignment.Center
                         ) {
                             Text(
                                 text = label,
                                 fontSize = 11.sp,
                                 fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                 color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                 textAlign = TextAlign.Center
                             )
                         }
                     }
                 }
             }

             // Reset Quick Mode Button
             OutlinedButton(
                 onClick = { onReset() },
                 modifier = Modifier.align(Alignment.End),
                 shape = RoundedCornerShape(8.dp),
                 colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
             ) {
                 Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                 Spacer(modifier = Modifier.width(4.dp))
                 Text(if (isFarsiMode) "شروع مجدد فیلترها" else "Reset Filters")
             }
         }
    }

    // Count Header & Refinements Section
    Card(
         colors = CardDefaults.cardColors(
             containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
         ),
         shape = RoundedCornerShape(12.dp),
         modifier = Modifier.fillMaxWidth()
    ) {
         Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
             Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.SpaceBetween,
                 verticalAlignment = Alignment.CenterVertically
             ) {
                 Text(
                     text = if (isFarsiMode) "کانی‌های کاندیدا منطبق: ${quickFilteredList.size}" else "Matching Candidates: ${quickFilteredList.size}",
                     fontWeight = FontWeight.Bold,
                     fontSize = 13.sp,
                     color = MaterialTheme.colorScheme.primary
                 )
                 Text(
                     text = if (isFarsiMode) "از کل ۵۰ کانی اصلی" else "Out of 50 core DB",
                     fontSize = 11.sp,
                     color = Color.Gray
                 )
             }

             if (quickFilteredList.size > 1) {
                 Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                 Text(
                     text = if (isFarsiMode) "🔍 گام چهارم: فیلترهای ممیزی کمکی (برای محدودتر شدن نام کانی):" else "🔍 Step 4: Diagnostic Refinements (Check to narrow search further):",
                     fontSize = 10.sp,
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.secondary
                 )

                 // Render beautiful refinement toggles based on remaining candidates
                 FlowRow(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.spacedBy(8.dp),
                     verticalArrangement = Arrangement.spacedBy(6.dp)
                 ) {
                     FilterChip(
                         selected = quickAcid,
                         onClick = { onAcidChange(!quickAcid) },
                         label = { Text(if (isFarsiMode) "🧪 جوشش با اسید" else "🧪 Acid Reaction", fontSize = 10.sp) },
                         leadingIcon = if (quickAcid) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) } } else null
                     )
                     FilterChip(
                         selected = quickSalty,
                         onClick = { onSaltyChange(!quickSalty) },
                         label = { Text(if (isFarsiMode) "👅 طعم شور" else "👅 Salty Taste", fontSize = 10.sp) },
                         leadingIcon = if (quickSalty) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) } } else null
                     )
                     FilterChip(
                         selected = quickSoapy,
                         onClick = { onSoapyChange(!quickSoapy) },
                         label = { Text(if (isFarsiMode) "🧈 احساس چربی/صابون" else "🧈 Soapy Feel", fontSize = 10.sp) },
                         leadingIcon = if (quickSoapy) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) } } else null
                     )
                     FilterChip(
                         selected = quickSheets,
                         onClick = { onSheetsChange(!quickSheets) },
                         label = { Text(if (isFarsiMode) "📄 ساختار ورقه‌ای" else "📄 Basal Sheets", fontSize = 10.sp) },
                         leadingIcon = if (quickSheets) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) } } else null
                     )
                 }
             }
         }
    }

    // MATCH LIST RENDERER
    if (quickFilteredList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                Text(
                    text = if (isFarsiMode) "هیچ کانی کاملاً منطبقی یافت نشد. دکمه شروع مجدد فیلترها را لمس کنید." else "No exact mineral matches these properties. Please adjust/reset filters.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // List of match cards. If only 1, we show a premium gold star and success checkmark!
        val singleExactFound = quickFilteredList.size == 1
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (singleExactFound) {
                 Box(
                     modifier = Modifier
                         .fillMaxWidth()
                         .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                         .border(1.2.dp, Color(0xFF2E7D32), RoundedCornerShape(12.dp))
                         .padding(12.dp)
                 ) {
                     Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                         Text(
                             text = if (isFarsiMode) "تبریک! هویت کانی شما با دقت ۱۰۰٪ تشخیصی آشکار شد:" else "Success! Your specimen has been fully identified:",
                             fontSize = 11.sp,
                             fontWeight = FontWeight.Bold,
                             color = Color(0xFF2E7D32)
                         )
                     }
                 }
            }

            quickFilteredList.forEach { mineral ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = if (singleExactFound) BorderStroke(1.5.dp, Color(0xFFFFB300)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = mineral.nameFa,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (singleExactFound) Color(0xFFD84315) else MaterialTheme.colorScheme.primary
                                    )
                                    if (singleExactFound) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(
                                    text = mineral.nameEn,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Mohs ${mineral.hardnessRange.start.toInt()}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1.0f)) {
                                Text(if (isFarsiMode) "رنگ بدنه: ${mineral.color}" else "Body Color: ${mineral.color}", fontSize = 11.sp)
                                Text(if (isFarsiMode) "رنگ خاکه: ${mineral.streak}" else "Streak Powder: ${mineral.streak}", fontSize = 11.sp)
                            }
                            Column(modifier = Modifier.weight(1.0f)) {
                                Text(if (isFarsiMode) "جلا: ${formatLuster(mineral.luster, isFarsiMode)}" else "Luster: ${formatLuster(mineral.luster, isFarsiMode)}", fontSize = 11.sp)
                                Text(if (isFarsiMode) "چگالی: ${mineral.density} g/cm³" else "Density: ${mineral.density} g/cm³", fontSize = 11.sp)
                            }
                        }

                        Text(
                            text = if (isFarsiMode) "رخ کانی: ${mineral.cleavage}" else "Cleavage type: ${mineral.cleavage}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (isFarsiMode) "💡 تشخیص صحرایی: ${mineral.specialFeat}" else "💡 Field Guide: ${mineral.enFeat}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
