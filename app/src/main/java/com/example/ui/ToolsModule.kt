package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.QAPFClassifierPanel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import java.text.DecimalFormat
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray

@Composable
fun ToolsContent(
    viewModel: GeologyViewModel,
    modifier: Modifier = Modifier
) {
    var selectedToolTab by remember { mutableStateOf(0) } // 0 = Mineral ID, 1 = Lithology, 2 = QAPF Plotter, 3 = Structural Geometry, 4 = Converter, 5 = Developer Profile
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Tab Title - Beautiful custom topographic/geological layered background
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val path = Path()
                    val w = size.width
                    val h = size.height
                    
                    // Layer 1 - Deep stratigraphic layer
                    path.moveTo(0f, h * 0.65f)
                    path.quadraticTo(w * 0.25f, h * 0.45f, w * 0.5f, h * 0.75f)
                    path.quadraticTo(w * 0.75f, h * 0.95f, w, h * 0.55f)
                    path.lineTo(w, h)
                    path.lineTo(0f, h)
                    path.close()
                    drawPath(path = path, color = primaryColor.copy(alpha = 0.05f))

                    // Layer 2 - Shaded soil/fault boundary
                    val path2 = Path()
                    path2.moveTo(0f, h * 0.8f)
                    path2.quadraticTo(w * 0.3f, h * 0.65f, w * 0.6f, h * 0.85f)
                    path2.quadraticTo(w * 0.8f, h * 0.92f, w, h * 0.7f)
                    path2.lineTo(w, h)
                    path2.lineTo(0f, h)
                    path2.close()
                    drawPath(path = path2, color = primaryColor.copy(alpha = 0.09f))
                },
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isFarsiMode) "ابزارهای تخصصی ژئوتکنیک" else "Geotech Field Tools",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isFarsiMode) "مجموعه ابزارهای کاربردی جهت تخمین صحرایی و رده‌بندی توصیفی ویژگی‌های زمین‌شناسی" else "Handy micro-tools to accelerate field estimations and diagnostic classification",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Tab selection row - bilingual and styled beautifully
        ScrollableTabRow(
            selectedTabIndex = selectedToolTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            Tab(
                selected = selectedToolTab == 0,
                onClick = { selectedToolTab = 0 },
                text = { Text(if (isFarsiMode) "کلاینومتر و برانتون" else "Field Compass", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 1,
                onClick = { selectedToolTab = 1 },
                text = { Text(if (isFarsiMode) "چکش اشمیت (ISRM)" else "Schmidt Hammer", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Hardware, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 2,
                onClick = { selectedToolTab = 2 },
                text = { Text(if (isFarsiMode) "اقتصاد معدن (NSR)" else "Mining Economics", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 3,
                onClick = { selectedToolTab = 3 },
                text = { Text(if (isFarsiMode) "برش عرضی" else "Cross-Section", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 4,
                onClick = { selectedToolTab = 4 },
                text = { Text(if (isFarsiMode) "دستیار هوش مصنوعی" else "AI Assistant", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 5,
                onClick = { selectedToolTab = 5 },
                text = { Text(if (isFarsiMode) "بارکد و برچسب نمونه" else "Sample Tag & QR", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 6,
                onClick = { selectedToolTab = 6 },
                text = { Text(if (isFarsiMode) "کانی‌شناسی" else "Mineralogy", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 7,
                onClick = { selectedToolTab = 7 },
                text = { Text(if (isFarsiMode) "سنگ‌شناسی" else "Lithology", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 8,
                onClick = { selectedToolTab = 8 },
                text = { Text(if (isFarsiMode) "پلاتر QAPF" else "QAPF Plotter", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 9,
                onClick = { selectedToolTab = 9 },
                text = { Text(if (isFarsiMode) "هندسه ساختاری" else "Structural Geometry", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 10,
                onClick = { selectedToolTab = 10 },
                text = { Text(if (isFarsiMode) "تبدیل واحد" else "Converter", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 11,
                onClick = { selectedToolTab = 11 },
                text = { Text(if (isFarsiMode) "توسعه‌دهنده" else "Developer", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 12,
                onClick = { selectedToolTab = 12 },
                text = { Text(if (isFarsiMode) "نقشه‌های کاربر" else "Custom Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

        // Content Area
        Box(modifier = Modifier.weight(1f)) {
            when (selectedToolTab) {
                0 -> FieldCompassModule(isFarsiMode)
                1 -> SchmidtHammerModule(isFarsiMode)
                2 -> MiningEconomicsModule(isFarsiMode)
                3 -> GeologicalCrossSectionModule(isFarsiMode)
                4 -> AiGeoAssistantModule(isFarsiMode)
                5 -> SampleLabelManagerModule(viewModel, isFarsiMode)
                6 -> MineralIdHelper(viewModel)
                7 -> LithologyHelper()
                8 -> Box(modifier = Modifier.verticalScroll(rememberScrollState())) { QAPFClassifierPanel() }
                9 -> StructuralGeometryCalculator(isFarsiMode)
                10 -> GeotechConverter()
                11 -> DeveloperProfilePage()
                12 -> {
                    UserMapImportCenterSheet(
                        viewModel = viewModel,
                        onDismiss = { selectedToolTab = 0 },
                        isOutdoorMode = false
                    )
                }
            }
        }
    }
}

// ---------------------------------------------
// 1. MINERAL IDENTIFICATION HELPER
// ---------------------------------------------
data class GeotechMineral(
    val nameFa: String,
    val nameEn: String,
    val hardnessRange: ClosedFloatingPointRange<Double>,
    val color: String,
    val streak: String,
    val luster: String,
    val cleavage: String,
    val density: Double,
    val acidReaction: Boolean = false,
    val magnetic: Boolean = false,
    val specialFeat: String = "",
    val enFeat: String = "",
    val feel: String = "normal",
    val shape: String = "massive",
    val taste: String = "none"
)

private fun formatLuster(luster: String, isFarsi: Boolean): String {
    return when (luster.lowercase()) {
        "metallic" -> if (isFarsi) "فلزی (Metallic)" else "Metallic"
        "submetallic" -> if (isFarsi) "نیمه‌فلزی (Submetallic)" else "Submetallic"
        "non_metallic" -> if (isFarsi) "غیرفلزی (Non-metallic)" else "Non-metallic"
        else -> luster
    }
}

fun getMineralGroup(mineral: GeotechMineral, isFarsi: Boolean): String {
    val name = mineral.nameEn.lowercase()
    return when {
        name.contains("quartz") || name.contains("feldspar") || name.contains("orthoclase") || name.contains("plagioclase") || 
        name.contains("talc") || name.contains("biotite") || name.contains("muscovite") || name.contains("olivine") || 
        name.contains("garnet") || name.contains("chlorite") || name.contains("epidote") || name.contains("actinolite") || 
        name.contains("kaolinite") || name.contains("tourmaline") || name.contains("sericite") || name.contains("wollastonite") || 
        name.contains("spodumene") -> {
            if (isFarsi) "سیلیکات‌ها (Silicates)" else "Silicates"
        }
        name.contains("calcite") || name.contains("dolomite") || name.contains("siderite") || name.contains("azurite") || name.contains("malachite") -> {
            if (isFarsi) "کربنات‌ها (Carbonates)" else "Carbonates"
        }
        name.contains("pyrite") || name.contains("chalcopyrite") || name.contains("galena") || name.contains("sphalerite") || 
        name.contains("bornite") || name.contains("chalcocite") || name.contains("arsenopyrite") || name.contains("pyrrhotite") || 
        name.contains("pentlandite") || name.contains("cinnabar") || name.contains("stibnite") || name.contains("molybdenite") || name.contains("jarosite") -> {
            if (isFarsi) "سولفیدها / سولفات‌ها (Sulfides)" else "Sulfides"
        }
        name.contains("gypsum") || name.contains("alunite") || name.contains("barite") || name.contains("celestine") -> {
            if (isFarsi) "سولفات‌ها (Sulfates)" else "Sulfates"
        }
        name.contains("hematite") || name.contains("magnetite") || name.contains("chromite") || name.contains("cassiterite") || 
        name.contains("limonite") || name.contains("goethite") || name.contains("bauxite") || name.contains("ilmenite") || 
        name.contains("rutile") || name.contains("scheelite") || name.contains("wolframite") -> {
            if (isFarsi) "اکسیدها / هیدروکسیدها (Oxides)" else "Oxides & Hydroxides"
        }
        name.contains("halite") || name.contains("sylvite") || name.contains("fluorite") -> {
            if (isFarsi) "هالیدها (Halides)" else "Halides"
        }
        name.contains("gold") || name.contains("silver") || name.contains("copper") || name.contains("graphite") || name.contains("sulfur") -> {
            if (isFarsi) "عناصر طبیعی (Elements)" else "Native Elements"
        }
        else -> {
            if (isFarsi) "سایر گروه‌ها" else "Other Groups"
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MineralIdHelper(viewModel: GeologyViewModel) {
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()
    
    val minerals: List<GeotechMineral> = remember {
        listOf(
            GeotechMineral("کوارتز (Quartz)", "Quartz", 7.0..7.0, "Colorless, white, gray, purple", "White", "non_metallic", "None (conchoidal fracture)", 2.65, specialFeat = "سختی بسیار بالا (۷)، عدم واکنش با اسید کلریدریک، نوع فاقد رخ و دارای جلا و شکستگی شیشه‌ای تند", enFeat = "High hardness (7), no reaction with acid, no cleavage, conchoidal fracture", feel = "smooth", shape = "hexagonal", taste = "none"),
            GeotechMineral("کلسیت (Calcite)", "Calcite", 3.0..3.0, "Colorless, white, yellow, orange", "White", "non_metallic", "Perfect rhombohedral in 3 directions", 2.71, acidReaction = true, specialFeat = "جوشش شدید و سریع با اسید رقیق، رخ‌های کامل لوزی‌شکل (۳ جهت شکست لوزی)، سختی متوسط (۳)", enFeat = "Strong effervescence with cold dilute HCl, perfect rhombohedral cleavages", feel = "normal", shape = "rhombohedral", taste = "none"),
            GeotechMineral("دولومیت (Dolomite)", "Dolomite", 3.5..4.0, "Pink, white, gray", "White", "non_metallic", "Rhombohedral in 3 directions", 2.85, acidReaction = true, specialFeat = "واکنش ضعیف با اسید ملایم مگر در حالت پودری یا گرم، رنگ‌های صورتی یا شیری ملایم", enFeat = "Effervesces with HCl only when powdered/warm, typical flesh-pink crystals", feel = "normal", shape = "rhombohedral", taste = "none"),
            GeotechMineral("پیریت (Pyrite)", "Pyrite", 6.0..6.5, "Pale brass yellow", "Greenish-black to brownish-black", "metallic", "None", 5.01, specialFeat = "طلای ابلهان، جلا فلزی براق، خاکه سیاه مایل به سبز تیره، سختی بالا که شیشه را خط می‌اندازد، بلور مکعبی", enFeat = "Fool's gold, dark green-black streak, cubic crystals, scratches glass", feel = "normal", shape = "cubic", taste = "none"),
            GeotechMineral("ارتوکلاز (Orthoclase)", "Orthoclase (K-Feldspar)", 6.0..6.0, "Pink, cream, flesh red, white", "White", "non_metallic", "Two directions at 90 degrees", 2.56, specialFeat = "معمولاً صورتی گوشتی (سلمونی)، دارای دو جهت رخ عمود بر هم، سطح کدر یا براق شیشه‌ای", enFeat = "Two cleavage planes at 90 deg, classic pink-salmon hue", feel = "normal", shape = "massive", taste = "none"),
            GeotechMineral("پلاژیوکلاز (Plagioclase)", "Plagioclase", 6.0..6.5, "White, gray, light green", "White", "non_metallic", "Two directions at 86 degrees", 2.68, specialFeat = "دارای خطوط ماکل بسیار ریز و موازی (شیارهای دوقلویی موازی) روی سطوح صاف رخ کانی", enFeat = "Twinning striations on cleavage surfaces, white to gray shades", feel = "normal", shape = "massive", taste = "none"),
            GeotechMineral("ژیپس / گچ (Gypsum)", "Gypsum", 2.0..2.0, "Colorless, white, gray, yellow", "White", "non_metallic", "Perfect cleavage in 1 direction (peels)", 2.31, specialFeat = "بسیار نرم، با ناخن دست به راحتی خط می‌افتد، ورقه‌های لایه‌ای تورق نازک و غیراستیک در ۱ جهت شکست", enFeat = "Scratched easily by fingernail, soft rock-forming sulfate", feel = "smooth", shape = "fibrous", taste = "none"),
            GeotechMineral("تالک (Talc)", "Talc", 1.0..1.0, "Green, white, pale gray", "White", "non_metallic", "Perfect cleavage in 1 direction", 2.75, specialFeat = "نرم‌ترین کانی فرضی موهس (۱)، حس لمس فوق‌العاده صابونی و چرب، به راحتی به پودر تبدیل می‌شود", enFeat = "Softest mineral on Mohs scale, highly soapy and greasy feel", feel = "soapy", shape = "plates", taste = "none"),
            GeotechMineral("هماتیت (Hematite)", "Hematite", 5.5..6.5, "Reddish-brown, dark metallic gray to silver", "Cherry red / reddish-brown", "metallic", "None", 5.26, specialFeat = "رنگ خاکه صددرصد قرمز گیلاسی یا خاکستری سرخ‌رنگ، کانی اکسید آهن با وزن مخصوص بالا", enFeat = "Cherry red streak regardless of specular silver or dark earthy outer color", feel = "dusty", shape = "massive", taste = "none"),
            GeotechMineral("مگنتیت (Magnetite)", "Magnetite", 5.5..6.5, "Dark iron black", "Black", "metallic", "None", 5.18, magnetic = true, specialFeat = "خاصیت مغناطیسی قوی و جذب آهنربا، رنگ تیره سیاه، کانی اکسید آهن با وزن مخصوص بالا", enFeat = "Strongly magnetic, attracts metals, dark heavy iron oxide", feel = "normal", shape = "cubic", taste = "none"),
            GeotechMineral("هالیت / نمک (Halite)", "Halite", 2.5..2.5, "Colorless, white, blue, red", "White", "non_metallic", "Perfect cubic cleavage (3 directions at 90 deg)", 2.16, specialFeat = "طعم شور بلامنازع، بلورهای تماماً مکعبی، انحلال سریع در آب صحرایی مکتشفان", enFeat = "Salty taste, highly soluble in water, perfect cubic cleavage", feel = "smooth", shape = "cubic", taste = "salty"),
            GeotechMineral("بیوتیت (Biotite)", "Biotite (Black Mica)", 2.5..3.0, "Dark brown to shiny black", "White/gray", "non_metallic", "Perfect basal cleavage (flexible elastic sheets)", 3.02, specialFeat = "میکای سیاه، ورقه‌های منعطف و فوق‌العاده کشسان که به راحتی با نوک چاقو از هم لایه‌لایه باز می‌شوند", enFeat = "Dark flexible mineral sheets, dark mica", feel = "smooth", shape = "plates", taste = "none"),
            GeotechMineral("مسکوویت (Muscovite)", "Muscovite (White Mica)", 2.0..2.5, "Colorless, silver, light green", "White", "non_metallic", "Perfect basal cleavage (flexible elastic sheets)", 2.82, specialFeat = "میکای سفید، ورقه‌های منعطف بی‌رنگ یا نقره‌ای کانی، عایق حرارتی و الکتریکی عالی", enFeat = "Clear/silver highly flexible elastic sheets, light mica", feel = "smooth", shape = "plates", taste = "none"),
            GeotechMineral("فلوریت (Fluorite)", "Fluorite", 4.0..4.0, "Purple, green, yellow, blue, colorless", "White", "non_metallic", "Perfect octahedral cleavage in 4 directions", 3.18, specialFeat = "بلورهای زیبای بنفش یا سبز شفاف، رخ یا رخساره منظم متقاطع هشت‌وجهی در ۴ جهت، سختی ۴", enFeat = "Purple or green octahedral crystals, octahedral cleavage 4 directions", feel = "smooth", shape = "cubic", taste = "none"),
            GeotechMineral("گالن / سرب (Galena)", "Galena", 2.5..2.5, "Bright lead gray", "Lead gray", "metallic", "Perfect cubic cleavage", 7.58, specialFeat = "سنگینی غیرعادی خیره‌کننده (چگالی فوق‌العاده ۷.۶)، جلای فلزی نقره‌ای سرب، رخ کامل مکعبی شکل زوایای ۹۰ درجه", enFeat = "Extremely heavy (G=7.6), bright lead-gray metallic, perfect cubic blocks", feel = "normal", shape = "cubic", taste = "none"),
            GeotechMineral("الیوین (Olivine)", "Olivine", 6.5..7.0, "Olive green, yellow-green", "White", "non_metallic", "None (conchoidal fracture)", 3.27, specialFeat = "بلورهای دانه شکری زیتونی رنگ در سنگ‌های بازالت و پریدوتیت، فاقد رخ با شکستگی صدفی شیشه‌ای", enFeat = "Olive-green sugary grains in mafic/ultramafic rocks, conchoidal fracture", feel = "normal", shape = "massive", taste = "none"),
            GeotechMineral("گرافیت (Graphite)", "Graphite", 1.0..2.0, "Dark gray to black", "Lead gray", "submetallic", "Perfect cleavage in 1 direction", 2.20, specialFeat = "بسیار نرم، روی کاغذ خط تیره می‌اندازد (سیاهی مغز مداد)، حس لمس فوق‌العاده چرب و کثیف‌کننده انگشتان", enFeat = "Very soft, writes on paper (like pencil lead), extremely greasy, marks fingers", feel = "soapy", shape = "plates", taste = "none"),
            GeotechMineral("گوگرد (Sulfur)", "Sulfur", 1.5..2.5, "Bright yellow", "White", "non_metallic", "Poor / None", 2.07, specialFeat = "رنگ زرد درخشان خورشیدی متمایز، بوی خفیف گوگرد در سایش، کلوخه‌های شیاری بسیار سبک‌وزن", enFeat = "Distinct solar bright yellow color, faint sulfur smell when rubbed, low density", feel = "normal", shape = "massive", taste = "none"),
            GeotechMineral("گارنت / لعل (Garnet)", "Garnet", 6.5..7.5, "Reddish-brown, ruby red, green", "White", "non_metallic", "None", 4.00, specialFeat = "بلورهای ۱۲ وجهی متمایز کروی شکل مشابه توپ فوتبال کوچک، سختی بالا در سنگ‌های دگرگونی شیست", enFeat = "Distinctive dodecahedral roundish crystals, glass-scratching hardness, metamorphic", feel = "normal", shape = "hexagonal", taste = "none"),
            GeotechMineral("کالکوپیریت (Chalcopyrite)", "Chalcopyrite", 3.5..4.0, "Brass yellow, golden", "Dark", "metallic", "None", 4.20, specialFeat = "زرد برنجی براق تیره شبیه به پیریت ولی نرم‌تر که به راحتی با نوک فلز خط می‌افتد، خاکه تیره", enFeat = "Brass-yellow metallic copper ore, softer than pyrite, dark greenish-black streak", feel = "normal", shape = "massive", taste = "none"),
            // Core Minerals Expansion for Professional Exploration:
            GeotechMineral("مالاکیت (Malachite)", "Malachite", 3.5..4.0, "Bright green, deep green", "Pale green", "non_metallic", "Perfect in 1 direction", 4.00, acidReaction = true, specialFeat = "رنگ سبز پاششی بسیار متمایز، کربنات مس حاصل دگرسانی سولفیدهای مس، با اسید کلریدریک رقیق فوراً با جوشش واکنش می‌دهد", enFeat = "Bright green carbonate, effervesces with acid, key marker for copper oxide enrichment zones"),
            GeotechMineral("آزوریت (Azurite)", "Azurite", 3.5..4.0, "Azure-blue, dark rich blue", "Light blue", "non_metallic", "Fair to poor", 3.77, acidReaction = true, specialFeat = "رنگ آبی لاجوردی شاهانه و بسیار چشم‌نواز، کربنات مس همراه مالاکیت در زون‌های اکسیده کانسارهای مس", enFeat = "Beautiful azure-blue copper carbonate, effervesces with acid, associated with malachite in supergene copper cappings"),
            GeotechMineral("بورنیت (Bornite)", "Bornite (Peacock Ore)", 3.0..3.0, "Bronze-brown with iridescent blue-purple tarnish", "Gray-black", "metallic", "None", 5.06, specialFeat = "مشهور به کانی پرطاووسی به دلیل تلاطم رنگهای بنفش و آبی متالیک تیره روی سطح در معرض هوا، عیار مس فوق‌العاده بالا (~۶۳٪ مس)", enFeat = "Peacock ore, unique violet-blue metallic iridescence on fresh bronze surface, critical high-grade copper sulfide"),
            GeotechMineral("کالکوسیت (Chalcocite)", "Chalcocite", 2.5..3.0, "Dark lead-gray to blackish-gray", "Dark gray", "metallic", "Poor", 5.70, specialFeat = "سربی‌کدر و بسیار چکش‌خوار، با چاقو به راحتی مانند ورقه بریده می‌شود، غنی‌ترین کانی ثانویه مس زون سوپرژن با ۸۰٪ مس خالص", enFeat = "Lead grey chalcocite, highly malleable/sectile, master mineral of supergene enriched copper blankets"),
            GeotechMineral("اسفالریت (Sphalerite)", "Sphalerite", 3.5..4.0, "Brown, yellow, black, red (Ruby Zinc)", "Light brown to golden yellow", "non_metallic", "Perfect in 6 directions", 4.00, specialFeat = "جلای صمغی بسیار براق، کانی اصلی سنگ معدن روی (سولفید روی)، سایش تند برگه‌های آن بوی خفیف گوگرد می‌دهد با خاکه زرد قهوه‌ای", enFeat = "Vitreous to resinous luster zinc sulfide, brown-yellow streak, primary zinc source worldwide"),
            GeotechMineral("آرسنوپیریت (Arsenopyrite)", "Arsenopyrite", 5.5..6.0, "Silver-white to steel gray", "Dark gray-black", "metallic", "Poor", 6.07, specialFeat = "رنگ سفید نقره‌ای فلزی بسیار سرد و مستحکم، ضربه شدید چکش به آن بوی آشکار سیر سمی (آرسنیک) تولید می‌کند، کانی گواهر طلا", enFeat = "Steel silver arsenopyrite, garlic-scented arsenic release upon hard impact, key pathfinder for orogenic gold deposits"),
            GeotechMineral("پیروتیت (Pyrrhotite)", "Pyrrhotite", 3.5..4.5, "Bronze yellow to brownish bronze", "Dark gray", "metallic", "None", 4.60, magnetic = true, specialFeat = "پیریت مغناطیسی، سولفید آهن غیرآلوتروپیک که به رنگ برنزی تیره دیده شده و توسط آهنربا جذب می‌شود. میزبان کانی مس-نیکل", enFeat = "Bronze-brown magnetic pyrrhotite, strongly to weakly attracted by magnet, associated with mafic nickel-copper sulfide ore"),
            GeotechMineral("مولیبدنیت (Molybdenite)", "Molybdenite", 1.0..1.5, "Lead gray, bluish gray", "Greenish gray / Bluish gray", "metallic", "Perfect basal (highly flexible sheets)", 4.70, specialFeat = "فوق‌العاده نرم با حس بسیار سرامیکی چرب، لایه لایه شده و کاغذ را خط می‌اندازد، همراهی چاه‌های پورفیری مس-مولیبدن هیدروترمال", enFeat = "Molybdenum sulfide ore, extremely soft, markers of copper-mollibdenum systems, leaves grey marker on paper like graphite"),
            GeotechMineral("طلا (Gold)", "Native Gold", 2.5..3.0, "Golden yellow, brass yellow", "Shiny golden yellow", "metallic", "None", 19.30, specialFeat = "چگالی ماورای تصور (۱۹.۳)، بسیار نرم و صددرصد چکش‌خوار با قابلیت خم‌شدن، هرگز در اسیدها کدر یا سیاه نمی‌شود", enFeat = "Native gold element, ultra-heavy, highly malleable, does not tarnish, diagnostic high-value exploration targets"),
            GeotechMineral("نقره (Silver)", "Native Silver", 2.5..3.0, "Silver white which tarnishes to black", "Shiny silver wire", "metallic", "None", 10.50, specialFeat = "جلای فلزی درخشان نقره‌ای، معمولا کلوخه‌ای دندریتی یا سیمی که در مجاورت هوا کدر و به سیاه تبدیل می‌شود. چکش‌خوار", enFeat = "Native silver metal, ductile, branches into dendritic forms, oxidized black wire surface"),
            GeotechMineral("پنتلاندیت (Pentlandite)", "Pentlandite", 3.5..4.0, "Bronze yellow, pale brassy", "Greenish black", "metallic", "None", 4.80, specialFeat = "کانی اصلی سنگ معدن نیکل، چسبیده و همزاد پیروتیت در ماورای مافیک‌های لایه‌ای، برخلاف پیروتیت هرگز مگنتیت یا مغناطیسی نیست", enFeat = "Primary nickel sulfide, non-magnetic bronzy yellow inclusions in magnetic pyrrhotite ores"),
            GeotechMineral("کرومیت (Chromite)", "Chromite", 5.5..5.5, "Dark brown to iron black", "Dark brown", "submetallic", "None", 4.72, specialFeat = "دیرگداز، خاکه قهوه‌ای سوخته منحصربفرد، به شکل ردیف‌های دانه‌ای یا نوارهای افشان متراکم در سرپانتینیت‌ها و افیولیت‌ها", enFeat = "Chromium iron oxide, diagnostic chocolate brown streak, dense layer accumulations in mantle peridotite structures"),
            GeotechMineral("کاسی تریت (Cassiterite)", "Cassiterite", 6.0..7.0, "Brown, black, yellow", "Pale brown to dusty gray", "non_metallic", "Poor", 6.90, specialFeat = "وزن مخصوص استثنایی سنگین برای کانی شیشه‌ای کدر، منشا گرانبها فلز قلع، بلورهای تیره منشورهای دوقلو در پگماتیت‌ها", enFeat = "Main tin oxide ore, remarkably heavy, adamantine to vitreous brown crystals, key pegmatitic target"),
            GeotechMineral("شیلیت (Scheelite)", "Scheelite", 4.5..5.0, "White, golden yellow, pale brown", "White", "non_metallic", "Good", 6.10, specialFeat = "کانی تنگستات کلسیم، سنگین‌وزن، بلورهای هرمی کدر، بهترین تست صحرایی درخشش آبی درخشان خیره‌کننده زیر نور فرابنفش (UV) است", enFeat = "Heavy calcium tungstate skarn ore, glows bright pale-blue under shortwave UV radiation"),
            GeotechMineral("ولفرامیت (Wolframite)", "Wolframite", 5.0..5.5, "Dark brownish black", "Dark reddish-brown to black", "submetallic", "Perfect in 1 direction", 7.30, specialFeat = "سنگین‌وزن و کدر با جلای کدر سربی رگه‌ای، منبع اصلی تنگستن در کانسارهای هیدروترمال عمیق رگ رگ و کوارتز گرایزن", enFeat = "Iron manganese tungstate, heavy steel black crystals, perfect cleavages, high-temp magmatic pneumatolytic mineral"),
            GeotechMineral("سینابار (Cinnabar)", "Cinnabar", 2.0..2.5, "Scarlet red, vivid cinnabar red", "Scarlet red", "non_metallic", "Perfect rhombohedral", 8.10, specialFeat = "رنگ سرخ قرمز آتشین بسیار متمایز، چگالی فوق‌العاده بالا جیوه (~۸.۱)، در چشمه‌های گرم و رسوبات جوان سیمانی یافت می‌شود", enFeat = "Scarlet-red mercury sulfide ore, extremamente dense (8.1), toxic hydrothermal low-temp mineralization indicator"),
            GeotechMineral("رئالگار (Realgar)", "Realgar", 1.5..2.0, "Fiery red to red-orange", "Orange-red", "non_metallic", "Good in 1 direction", 3.56, specialFeat = "رنگ قرمز مایل به نارنجی تابان، سولفید آرسنیک، در برابر نور مداوم خورشید ناپایدار بوده و به پودر زرد اورپیمنت تبدیل می‌شود", enFeat = "Vivid red-orange arsenic sulfide, unstable under long exposure to light, low temperature hydrothermal vein marker"),
            GeotechMineral("اورپیمنت (Orpiment)", "Orpiment", 1.5..2.0, "Lemon-yellow to bright gold-yellow", "Pale yellow", "non_metallic", "Perfect in 1 direction", 3.49, specialFeat = "رنگ زرد لیمویی بی‌نظیر زرنیخ، کانی آرسنیک ورقه‌ای بسیار نرم که به راحتی قاچ می‌شود. همراه سنگ‌های سینابار و رئالگار", enFeat = "Lemon-yellow arsenic disulfide, foliated micaceous plates, associated with hot springs and toxic fumes"),
            GeotechMineral("استیب نیت (Stibnite)", "Stibnite", 2.0..2.0, "Lead gray with brilliant metallic sheen", "Lead gray", "metallic", "Perfect in 1 direction", 4.63, specialFeat = "سنگ معدن اصلی فلز آنتیموان، شکل سوزنی طویل و مدادی با تیرگی گوتیک در هم تنیده، به موازات رخ لایه‌لایه طولی خم می‌شود", enFeat = "Primary antimony sulfide ore, forms spectacular needle-like prismatic lead grey columns, extremely low melting point"),
            GeotechMineral("باریت (Barite)", "Barite", 3.0..3.5, "White, yellow, pink, grey", "White", "non_metallic", "Perfect in 2 directions", 4.50, specialFeat = "سنگین‌کانی با وزن مخصوص استثنایی و چرب مانند برای کانی توده‌ای غیرفلزی، پرکننده ارزشمند گل‌های عمیق حفاری اکتشافی", enFeat = "Barium sulfate, outstandingly heavy density for a white nonmetal mineral, core petroleum drilling weight ingredient"),
            GeotechMineral("سلستین (Celestine)", "Celestine", 3.0..3.5, "Pale blue, milky white", "White", "non_metallic", "Perfect in 3 directions", 3.97, specialFeat = "سولفات استرانسیم، بلورهای آبی کمرنگ آسمانی متقاطع، متمایل به سنگین چگالی بالا، تشکیل در سنگ‌های تبخیری و رسوبی", enFeat = "Strontium sulfate, pale blue tint prismatic crystals, diagnostic high weight, associated with dolomite and gypsum evaporites"),
            GeotechMineral("سیدریت (Siderite)", "Siderite", 3.5..4.5, "Brown, yellow-brown, grayish", "White", "non_metallic", "Perfect rhombohedral", 3.96, acidReaction = true, specialFeat = "کربنات آهن طبیعی، ترجیحاً برنزی رنگ، پس از حرارت دیدن شدید به قوی جذب آهنربا می‌شود، جوشش ضعیف با اسید داغ تند", enFeat = "Iron carbonate, yellowish-brown crystals, effervesces marginally in warm HCl, becomes magnetic after roasted by blowpipe"),
            GeotechMineral("کائولینیت (Kaolinite)", "Kaolinite (China Clay)", 2.0..2.5, "Chalky white, earthy beige", "White text", "non_metallic", "Perfect basal, obscured", 2.63, specialFeat = "کانی رسی دگرسانی آلومینیومی، حس کاملا خاکی خرد شونده، چسبندگی فوق‌العاده شدید به زبان به دلیل تشنه جذب آب بودن", enFeat = "Clay mineral, pure white earthy structures, distinctive clay scent when wet, sticks to tongue due to high capillarity"),
            GeotechMineral("کلریت (Chlorite)", "Chlorite", 2.0..2.5, "Dull dark green, forest green", "Greenish gray", "non_metallic", "Perfect basal plates", 2.80, specialFeat = "کانی ورقه‌ای غیرقابل ارتجاع سبز رنگ، از کانی‌های شاخص دگرسانی پروپیلیتیک (Propylitic) در مجاورت ذخایر مس پورفیری", enFeat = "Green sheet silicate, key index mineral of propylitic alteration in porphyry copper and epithermal systems"),
            GeotechMineral("اپیدوت (Epidote)", "Epidote", 6.0..7.0, "Pistachio-green, olive yellow", "White", "non_metallic", "Perfect in 1 direction", 3.40, specialFeat = "رنگ سبز مغزپسته‌ای منحصربفرد، به شکل رگچه‌ها یا دانه‌های سخت در سنگ‌های دگرگون شده سبز یا زون‌های اولیه اسکارن", enFeat = "Pistachio green sorosilicate, hardness 6-7, marker for copper skarns and retrograde calc-silicate alteration haloes"),
            GeotechMineral("آلونیت (Alunite)", "Alunite", 3.5..4.0, "White, gray, light reddish pink", "White", "non_metallic", "Good", 2.80, specialFeat = "کانی سولفات دگرسانی اسیدی، تشکیل در زون بسیار شدید با حرارت اسید-سولفات خاک‌های آرژیلیک پیشرفته کانتین پورت‌ها", enFeat = "Sulfate mineral, direct index of advanced argillic geothermal and epithermal alteration caps"),
            GeotechMineral("لیمونیت (Limonite)", "Limonite (Iron Hat)", 4.0..5.5, "Yellowish-brown, ochre yellow", "Yellowish brown", "non_metallic", "None", 3.60, specialFeat = "کلاهک اکسیدی زرد رنگ کانسارهای معدنی (گوگرد زدایی و شستشوی سولفیدها در گوسان)، فاقد بافت کریستالی منظم", enFeat = "Hydrous iron oxides, yellow-brown rust-like aggregations, primary material of mineralized gossan hats/cappings"),
            GeotechMineral("گوتیت (Goethite)", "Goethite", 5.0..5.5, "Dark brown, yellowish brown", "Ochre yellow", "submetallic", "Perfect in 1 direction", 4.28, specialFeat = "رنگ خاکه زرد مایل به قهوه‌ای کاملا کدر، توده‌های کلیوی یا الیافی تیره، بخش سخت گوسان اکسیده مس و آهن", enFeat = "Iron oxide hydroxide, diagnostic ocher-yellow streak, crystalline part of iron hats indicating gossan mineralization"),
            GeotechMineral("بوکسیت (Bauxite)", "Bauxite", 1.0..3.0, "Earthy red-brown, whitish", "Reddish brown", "non_metallic", "None", 2.50, specialFeat = "سنگ معدن اصلی آلومینیوم متشکل از بافت‌های بسیار ظریف و گوی مانند (پیزولیتی) مانند گلوله‌های چسبیده پالتوها", enFeat = "Primary aluminum ore mass, exhibits beautiful concentric pisolitic pea-like diagnostic features"),
            GeotechMineral("سیلویت (Sylvite)", "Sylvite (Potash)", 2.0..2.0, "White, pinkish-orange, red", "White", "non_metallic", "Perfect cubic", 1.99, specialFeat = "مشابه سنگ نمک ولی دارای طعم به شدت تلخ، تیز و متمایز سوزاننده، کانی کود پتاس تبخیری‌ها", enFeat = "Potassium chloride, cubic habit, highly sharp bitter-burning taste, premium potassium potash fertilizer input"),
            GeotechMineral("ایلمنیت (Ilmenite)", "Ilmenite", 5.0..6.0, "Iron black", "Black", "metallic", "None", 4.79, specialFeat = "جلای نیمه‌شور تیره تیتان آهن اکسید، با مغناطیس جزئی ذرات آن متمایز از مگنتیت است، رسوب در پلاسرهای فوق سنگین", enFeat = "Titanium iron oxide, weakly magnetic compared to magnetite, heavy mineral sands placer concentrations"),
            GeotechMineral("روتیل (Rutile)", "Rutile", 6.0..6.5, "Reddish-brown, dark blood red, brassy black", "Chalky yellow-brown", "non_metallic", "Good", 4.25, specialFeat = "ضریب شکست نوری بالاتر از الماس، بلورهای طویل سوزنی مانند مو داخل کوارتز (در کوارتز رتیله) یا منشورهای قلابدار متقاطع", enFeat = "Titanium dioxide polymorphic, dark blood-red adamantine crystals, high refractive index, forms needles in quartz"),
            GeotechMineral("تورمالین (Tourmaline)", "Tourmaline", 7.0..7.5, "Coal black, deep green", "White", "non_metallic", "No / Conchoidal fracture", 3.06, specialFeat = "بلورهای منشوری طویل با سطح مقطع مثلث کلوخی با شیارهای طولی، سختی شدید (۷.۵)، کانی شاخص پگماتیت‌ها", enFeat = "Complex borosilicate, unique diagnostic curved-triangular cross-section prisms, hardness 7.5, pegs pathfinder"),
            GeotechMineral("مس طبیعی (Copper)", "Native Copper", 2.5..3.0, "Copper red, tarnishes green", "Copper-red shiny metallic", "metallic", "None", 8.94, specialFeat = "فلز مس قرمز طبیعی چکش‌خوار، با توده‌های دندریتی، معمولا با سطوح پوسته سبز رنگ پودری مالاکیت اکسید پوشیده می‌شود", enFeat = "Native element copper red, extremely dense and ductile, oxidation skin of green malachite protects core"),
            GeotechMineral("اکتینولیت (Actinolite)", "Actinolite", 5.5..6.0, "Pistachio dark green to emerald", "White", "non_metallic", "Perfect amphibole angles (56 and 124 deg)", 3.04, specialFeat = "بلورهای سوزنی شکل دراز شعاعی و نازک مانند چوب، کانی دگرگونی سنگ‌های شیست سبز و هاله‌های بزرگ اسکارن", enFeat = "Green fibrous amphibole, forms needle-like cluster sprays in greenschist facies and iron skarn zones"),
            GeotechMineral("کریسوکولا (Chrysocolla)", "Chrysocolla", 2.0..4.0, "Cyan, greenish-blue, turquoise", "Light blue", "non_metallic", "None", 2.20, specialFeat = "سیلیکات مس بسیار سبک‌وزن، رنگ فیروزه‌ای مات تابان، به شدت جذب رطوبت زبان می‌شود، در زون‌های اکسیده مس یافت می‌شود", enFeat = "Lightweight copper silicate, vivid blue-green, highly absorbent of tongue moisture, oxidation cap zone indicator", feel = "smooth", shape = "massive"),
            GeotechMineral("لپیدولیت (Lepidolite)", "Lepidolite (Lithium Mica)", 2.5..4.0, "Lilac-gray, purple, rose-pink", "White", "non_metallic", "Perfect basal plates", 2.85, specialFeat = "میکای بنفش غنی از لیتیوم، فلس‌های صورتی تا بنفش درخشان، از کانی‌های شاخص و پیشرو پگماتیت‌های غنی از لیتیوم (LCT)", enFeat = "Lilac-pink lithium-bearing mica, diagnostic violet sparkly flakes, signature of highly fractionated pegmatites", feel = "smooth", shape = "plates"),
            GeotechMineral("اسپودومن (Spodumene)", "Spodumene (Lithium Ore)", 6.5..7.0, "White, grayish, yellowish, kunzite pink", "White", "non_metallic", "Perfect prismatic in 2 directions", 3.15, specialFeat = "پیروکسن لیتیوم‌دار، بلورهای منشوری طویل شیاردار، سختی بسیار بالا (۷)، منبع اصلی لیتیوم جهان در پگماتیت‌ها", enFeat = "Primary lithium pyroxene, elongated flattened prisms, high hardness (6.5-7), major lithium resource", feel = "normal", shape = "columnar"),
            GeotechMineral("بریل (Beryl)", "Beryl (Emerald/Aquamarine)", 7.5..8.0, "Green, blue, yellow, white, pink", "White", "non_metallic", "Poor / None", 2.76, specialFeat = "سختی فوق‌العاده بالا (۸)، بلورهای کاملاً شش‌وجهی منشوری منظم صاف، شامل نگین‌های ارزشمند زمرد و آکوآمارین", enFeat = "Extreme hardness (7.5-8), perfect hexagonal prisms, source of beryllium and emerald gemstones", feel = "normal", shape = "hexagonal"),
            GeotechMineral("وانادینیت (Vanadinite)", "Vanadinite", 2.5..3.0, "Bright red, orange, brown", "White to pale yellow", "non_metallic", "None", 6.85, specialFeat = "بلورهای هگزاگونال شش‌گوشه نارنجی-قرمز با جلای صمغی بسیار براق، چگالی فوق‌العاده سنگین برای کانی شیشه‌ای به دلیل داشتن سرب و وانادیوم", enFeat = "Bright red hexagonal prism plates, remarkably high density (6.8) due to lead-vanadium content, supergene zone", feel = "normal", shape = "hexagonal"),
            GeotechMineral("وولفنیت (Wulfenite)", "Wulfenite", 2.7..3.0, "Yellow, orange, bright red", "White", "non_metallic", "Good", 6.80, specialFeat = "مولیبدات سرب، بلورهای بسیار نازک مربعی یا لوحی زردرنگ تا نارنجی براق، در زون‌های بسیار اکسیده شده کانسارهای سرب و روی", enFeat = "Thin tabular square crystals, resinous butter-yellow to orange-red, lead-molybdate marker", feel = "normal", shape = "plates"),
            GeotechMineral("سروسیت (Cerussite)", "Cerussite (Lead Carbonate)", 3.0..3.5, "Colorless, white, gray, adamantine", "White", "non_metallic", "Good in 2 directions", 6.55, acidReaction = true, specialFeat = "کربنات سرب ثانویه، دارای جلای قوی الماسی بسیار براق و چگالی فوق‌العاده سنگین، با اسید کلریدریک گرم جوشش شدیدی دارد", enFeat = "Extremely heavy lead carbonate, adamantine sparkling luster, effervesces strongly in warm dilute HCl", feel = "normal", shape = "rhombohedral"),
            GeotechMineral("آنگلزیت (Anglesite)", "Anglesite (Lead Sulfate)", 2.5..3.0, "Colorless, white, gray, yellowish", "White", "non_metallic", "Good", 6.30, specialFeat = "سولفات سرب، حاصل تجزیه گالن، بسیار سنگین‌وزن با جلای الماسی، برخلاف سروسیت هرگز با اسید کلریدریک نمی‌جوشد", enFeat = "Heavy lead sulfate formed from galena oxidation, adamantine luster, no effervescence with acid", feel = "normal", shape = "massive"),
            GeotechMineral("اسمیت زونیت (Smithsonite)", "Smithsonite (Zinc Spar)", 4.0..4.5, "Green, blue, pink, white, gray", "White", "non_metallic", "Perfect rhombohedral", 4.40, acidReaction = true, specialFeat = "کربنات روی ثانویه، معمولاً با ظاهر خوشه‌ای (کلوئیدی) شبیه به حباب‌های صابون چسبیده، با اسید کلریدریک رقیق و گرم جوشش شدید دارد", enFeat = "Zinc carbonate, botryoidal bubbly clusters (dry bone ore), effervesces vigorously in warm HCl", feel = "smooth", shape = "massive"),
            GeotechMineral("استارولیت (Staurolite)", "Staurolite (Fairy Cross)", 7.0..7.5, "Reddish-brown, dark brown, black", "Gray/White", "non_metallic", "Poor", 3.75, specialFeat = "بلورهای دوقلوی چلیپایی بسیار متمایز شبیه به علامت صلیب یا ضربدر، سختی بسیار بالا (۷.۵)، کانی شاخص دگرگونی شیست شدید", enFeat = "Metamorphic index mineral, distinct cross-shaped twin crystals, highly resistant, diagnostic schist marker", feel = "normal", shape = "massive"),
            GeotechMineral("رودونیت (Rhodonite)", "Rhodonite", 5.5..6.5, "Rose-pink, red with black veins", "White", "non_metallic", "Perfect in 2 directions", 3.60, specialFeat = "رنگ صورتی تیره تا قرمز گل‌بهی غنی از منگنز همراه با رگچه‌ها و لکه‌های شبکه متقاطع سیاه رنگ دندریتی اکسید منگنز، سختی بالا", enFeat = "Vibrant rose-pink silicate with dramatic dark black dendritic manganese oxide veins, high hardness (5.5-6.5)", feel = "normal", shape = "massive"),
            GeotechMineral("رودوکروزیت (Rhodochrosite)", "Rhodochrosite", 3.5..4.0, "Raspberry pink, rose red", "White", "non_metallic", "Perfect rhombohedral", 3.70, acidReaction = true, specialFeat = "کربنات منگنز صورتی تمشکی خیره‌کننده، نوارهای موازی صورتی-سفید، با اسید کلریدریک گرم جوشش متمایز دارد", enFeat = "Stunning raspberry-pink manganese carbonate with white banding layers, effervesces beautifully in warm acid", feel = "smooth", shape = "rhombohedral"),
            GeotechMineral("ولاستونیت (Wollastonite)", "Wollastonite", 4.5..5.0, "White, gray, pale green", "White", "non_metallic", "Perfect in 2 directions", 2.90, specialFeat = "سیلیکات کلسیم، بلورهای توده‌ای الیافی یا سوزنی کلاف مانند، کانی دگرگونی تماسی اسکارن آهکی مجاور سنگ‌های آذرین", enFeat = "Fibrous to acicular white calc-silicate, diagnostic of contact metamorphic skarns, industrial mineral", feel = "normal", shape = "fibrous"),
            GeotechMineral("پیروژنوسیت / یاقوت کروم (Pyrope Garnet)", "Pyrope Garnet", 7.0..7.5, "Deep blood red, purplish-red", "White", "non_metallic", "None", 3.56, specialFeat = "کانی شاخص گوشته و لوله‌های کیمبرلیتی حاوی الماس، رنگ قرمز خون کبوتری تیره بسیار سخت، فاقد هرگونه رخ متمایز", enFeat = "Chrome-rich deep blood-red garnet, ultra-stable, primary pathfinder used globally in diamond exploration programs", feel = "normal", shape = "hexagonal"),
            GeotechMineral("سرپانتین (Serpentine)", "Serpentine", 3.0..5.0, "Apple green, olive green, blackish green", "White", "non_metallic", "Poor", 2.60, specialFeat = "رنگ سبز ماری چرب و بسیار براق موم مانند، سطح لغزنده و پودری، تشکیل دهنده اصلی افیولیت‌ها و زون‌های بخیه زمین‌درز ایران", enFeat = "Waxy mottled green silicate, greasy feel, primary component of altered mantle peridotites in ophiolite suites", feel = "soapy", shape = "massive")
        ) + com.example.ui.getExtraMinerals()
    }


    // Tab control
    var activeMineralTab by remember { mutableStateOf(0) } // 0 = Wizard, 1 = Manual Catalog, 2 = AI Co-Database (500+ Minerals)
    var selectedMineralForLab by remember { mutableStateOf<GeotechMineral?>(null) }
    var isQuickMode by remember { mutableStateOf(true) } // true = Easy 4-Step Key, false = Classic 10Q

    var selectedGroupFilter by remember { mutableStateOf(if (isFarsiMode) "همه کانی‌ها" else "All") }
    var mohsScaleExpanded by remember { mutableStateOf(false) }

    // Quick Mode Selection parameters
    var quickHardness by remember { mutableStateOf("any") } // "any", "nail", "coin", "knife", "glass"
    var quickLuster by remember { mutableStateOf("any") } // "any", "metallic", "non_metallic"
    var quickMagnet by remember { mutableStateOf("any") } // "any", "yes", "no"

    // Quick Mode Refinements
    var quickAcid by remember { mutableStateOf(false) }
    var quickSalty by remember { mutableStateOf(false) }
    var quickSoapy by remember { mutableStateOf(false) }
    var quickSheets by remember { mutableStateOf(false) }

    fun resetQuickFinder() {
        quickHardness = "any"
        quickLuster = "any"
        quickMagnet = "any"
        quickAcid = false
        quickSalty = false
        quickSoapy = false
        quickSheets = false
    }

    // WIZARD QUESTION STATES (10 diagnostic steps)
    var currentStep by remember { mutableStateOf(1) } // 1 to 10
    var answerLuster by remember { mutableStateOf("any") }
    var answerHardness by remember { mutableStateOf("any") }
    var answerStreak by remember { mutableStateOf("any") }
    var answerAcid by remember { mutableStateOf("any") }
    var answerMagnet by remember { mutableStateOf("any") }
    var answerCleavage by remember { mutableStateOf("any") }
    var answerDensity by remember { mutableStateOf("any") }
    var answerFeel by remember { mutableStateOf("any") }
    var answerTaste by remember { mutableStateOf("any") }
    var answerShape by remember { mutableStateOf("any") }

    // CATALOG FILTER STATES
    var catalogSearchQuery by remember { mutableStateOf("") }
    var searchHardness by remember { mutableStateOf("") }
    var filterLuster by remember { mutableStateOf("All Lusters") }
    var filterAcid by remember { mutableStateOf(false) }
    var filterMagnet by remember { mutableStateOf(false) }

    fun resetWizard() {
        currentStep = 1
        answerLuster = "any"
        answerHardness = "any"
        answerStreak = "any"
        answerAcid = "any"
        answerMagnet = "any"
        answerCleavage = "any"
        answerDensity = "any"
        answerFeel = "any"
        answerTaste = "any"
        answerShape = "any"
    }

    // Helper function to calculate weighted matching score
    fun getScore(min: GeotechMineral): Int {
        var penalties = 0
        var activeCount = 0

        // 1. Luster matching
        if (answerLuster != "any") {
            activeCount++
            val minLuster = min.luster
            if (minLuster != answerLuster) {
                if ((answerLuster == "submetallic" && minLuster == "metallic") || 
                    (answerLuster == "metallic" && minLuster == "submetallic")) {
                    penalties += 10
                } else {
                    penalties += 30
                }
            }
        }

        // 2. Hardness matching
        if (answerHardness != "any") {
            activeCount++
            val minStart = min.hardnessRange.start
            val minEnd = min.hardnessRange.endInclusive
            val matches = when (answerHardness) {
                "very_soft" -> minStart <= 2.5
                "soft" -> minStart > 2.0 && minEnd <= 3.5
                "medium" -> minStart > 2.5 && minEnd <= 5.5
                "hard" -> minEnd > 5.5
                else -> true
            }
            if (!matches) {
                penalties += 30
            }
        }

        // 3. Streak color matching
        if (answerStreak != "any") {
            activeCount++
            val minStreakMapped = when {
                min.streak.lowercase().contains("white") || min.streak.lowercase().contains("colorless") -> "white"
                min.streak.lowercase().contains("cherry") || min.streak.lowercase().contains("red") -> "red"
                min.streak.lowercase().contains("greenish-black") || min.streak.lowercase().contains("black") -> "dark"
                min.streak.lowercase().contains("lead") || min.streak.lowercase().contains("charcoal") || min.streak.lowercase().contains("gray") -> "gray"
                min.streak.lowercase().contains("yellow") || min.streak.lowercase().contains("golden") -> "yellow"
                else -> "white"
            }
            if (minStreakMapped != answerStreak) {
                penalties += 25
            }
        }

        // 4. Acid Reaction matching
        if (answerAcid != "any") {
            activeCount++
            val isCalcite = min.nameEn.lowercase().contains("calcite")
            val isDolomite = min.nameEn.lowercase().contains("dolomite")
            val minAcid = when {
                min.acidReaction && isCalcite -> "strong"
                min.acidReaction && isDolomite -> "weak"
                else -> "none"
            }
            if (minAcid != answerAcid) {
                penalties += 35
            }
        }

        // 5. Magnetism matching
        if (answerMagnet != "any") {
            activeCount++
            val minMag = if (min.magnetic) "magnetic" else "non_magnetic"
            if (minMag != answerMagnet) {
                penalties += 45
            }
        }

        // 6. Cleavage matching
        if (answerCleavage != "any") {
            activeCount++
            val minCleavageMapped = when {
                min.cleavage.lowercase().contains("basal") || min.cleavage.lowercase().contains("peels") || min.cleavage.lowercase().contains("1 direction") -> "sheets"
                min.cleavage.lowercase().contains("perfect cubic") || min.cleavage.lowercase().contains("cubic cleavage") -> "cubic"
                min.cleavage.lowercase().contains("rhombohedral") -> "rhombohedral"
                min.cleavage.lowercase().contains("octahedral") -> "octahedral"
                min.cleavage.lowercase().contains("none") || min.cleavage.lowercase().contains("conchoidal") -> "conchoidal"
                else -> "conchoidal"
            }
            if (minCleavageMapped != answerCleavage) {
                penalties += 25
            }
        }

        // 7. Density matching
        if (answerDensity != "any") {
            activeCount++
            val minDensityMapped = when {
                min.density < 2.5 -> "light"
                min.density >= 2.5 && min.density <= 4.5 -> "medium"
                else -> "heavy"
            }
            if (minDensityMapped != answerDensity) {
                penalties += 25
            }
        }

        // 8. Feel matching
        if (answerFeel != "any") {
            activeCount++
            if (min.feel != answerFeel) {
                penalties += 20
            }
        }

        // 9. Taste matching
        if (answerTaste != "any") {
            activeCount++
            if (min.taste != answerTaste) {
                penalties += 50
            }
        }

        // 10. Crystal Shape matching
        if (answerShape != "any") {
            activeCount++
            if (min.shape != answerShape) {
                penalties += 20
            }
        }

        if (activeCount == 0) return 100
        val finalScore = 100 - penalties
        return finalScore.coerceIn(0, 100)
    }

    // Dynamic candidates calculated for manual filter tab
    val filteredCatalogList = remember(catalogSearchQuery, searchHardness, filterLuster, filterAcid, filterMagnet, selectedGroupFilter) {
        val targetHardness = searchHardness.toDoubleOrNull()
        minerals.filter { min ->
            val matchesSearch = if (catalogSearchQuery.isNotEmpty()) {
                min.nameFa.contains(catalogSearchQuery, ignoreCase = true) ||
                min.nameEn.contains(catalogSearchQuery, ignoreCase = true) ||
                min.specialFeat.contains(catalogSearchQuery, ignoreCase = true) ||
                min.enFeat.contains(catalogSearchQuery, ignoreCase = true) ||
                min.color.contains(catalogSearchQuery, ignoreCase = true) ||
                min.streak.contains(catalogSearchQuery, ignoreCase = true) ||
                min.luster.contains(catalogSearchQuery, ignoreCase = true)
            } else true

            val matchesHardness = if (targetHardness != null) {
                targetHardness in min.hardnessRange || 
                (targetHardness >= min.hardnessRange.start - 0.5 && targetHardness <= min.hardnessRange.endInclusive + 0.5)
            } else true

            val matchesLuster = if (filterLuster != "All Lusters") {
                if (filterLuster == "Metallic Luster" || filterLuster == "جلای فلزی") {
                    min.luster.lowercase().contains("metallic")
                } else {
                    !min.luster.lowercase().contains("metallic")
                }
            } else true

            val matchesAcid = if (filterAcid) min.acidReaction else true
            val matchesMagnet = if (filterMagnet) min.magnetic else true

            val matchesGroup = if (selectedGroupFilter != "All" && selectedGroupFilter != "همه کانی‌ها") {
                val groupText = getMineralGroup(min, isFarsiMode)
                groupText.contains(selectedGroupFilter) || selectedGroupFilter.contains(groupText) || 
                (selectedGroupFilter == "Sulfides" && groupText.contains("Sulfides")) ||
                (selectedGroupFilter == "Sulfates" && groupText.contains("Sulfates")) ||
                (selectedGroupFilter == "Oxides" && groupText.contains("Oxides")) ||
                (selectedGroupFilter == "Halides" && groupText.contains("Halides")) ||
                (selectedGroupFilter == "Elements" && groupText.contains("Elements")) ||
                (selectedGroupFilter == "سولفیدها" && groupText.contains("سولفیدها")) ||
                (selectedGroupFilter == "سولفات‌ها" && groupText.contains("سولفات‌ها")) ||
                (selectedGroupFilter == "کربنات‌ها" && groupText.contains("کربنات‌ها")) ||
                (selectedGroupFilter == "سیلیکات‌ها" && groupText.contains("سیلیکات‌ها")) ||
                (selectedGroupFilter == "اکسیدها" && groupText.contains("اکسیدها")) ||
                (selectedGroupFilter == "هالیدها" && groupText.contains("هالیدها")) ||
                (selectedGroupFilter == "عناصر طبیعی" && groupText.contains("عناصر طبیعی"))
            } else true

            matchesSearch && matchesHardness && matchesLuster && matchesAcid && matchesMagnet && matchesGroup
        }
    }

    // Candidates sorted by wizard scores
    val scoredWizardList = remember(
        answerLuster, answerHardness, answerStreak, answerAcid, answerMagnet,
        answerCleavage, answerDensity, answerFeel, answerTaste, answerShape
    ) {
        minerals.map { min -> min to getScore(min) }.sortedByDescending { it.second }
    }

    // Interactive simplified key matching over database
    val quickFilteredList = remember(quickHardness, quickLuster, quickMagnet, quickAcid, quickSalty, quickSoapy, quickSheets) {
        minerals.filter { min ->
            // 1. Hardness check
            val matchesHardness = when (quickHardness) {
                "any" -> true
                "nail" -> min.hardnessRange.start <= 2.5
                "coin" -> (min.hardnessRange.start >= 2.0 && min.hardnessRange.start <= 3.6) || (min.hardnessRange.endInclusive >= 2.5 && min.hardnessRange.endInclusive <= 3.5)
                "knife" -> (min.hardnessRange.start >= 3.0 && min.hardnessRange.start <= 5.6) || (min.hardnessRange.endInclusive >= 3.5 && min.hardnessRange.endInclusive <= 5.5) || (min.hardnessRange.start <= 3.5 && min.hardnessRange.endInclusive >= 3.5)
                "glass" -> min.hardnessRange.endInclusive > 5.5
                else -> true
            }
            
            // 2. Luster check
            val matchesLuster = when (quickLuster) {
                "any" -> true
                "metallic" -> min.luster.lowercase() == "metallic" || min.luster.lowercase() == "submetallic"
                "non_metallic" -> min.luster.lowercase() == "non_metallic"
                else -> true
            }
            
            // 3. Magnetism check
            val matchesMagnet = when (quickMagnet) {
                "any" -> true
                "yes" -> min.magnetic
                "no" -> !min.magnetic
                else -> true
            }
            
            // 4. Refinements
            val matchesAcid = if (quickAcid) min.acidReaction else true
            val matchesSalty = if (quickSalty) min.taste == "salty" else true
            val matchesSoapy = if (quickSoapy) min.feel == "soapy" else true
            val matchesSheets = if (quickSheets) min.cleavage.lowercase().contains("basal") || min.cleavage.lowercase().contains("peels") || min.cleavage.lowercase().contains("1 direction") else true
            
            matchesHardness && matchesLuster && matchesMagnet && matchesAcid && matchesSalty && matchesSoapy && matchesSheets
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tab indicator segment
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { activeMineralTab = 0 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeMineralTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (activeMineralTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(40.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(if (isFarsiMode) "شناسایی ۱۰سوالی" else "Wizard (10Q)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { activeMineralTab = 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeMineralTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (activeMineralTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(40.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(if (isFarsiMode) "کاتالوگ دستی" else "Catalog", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { activeMineralTab = 2 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeMineralTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (activeMineralTab == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1.1f).height(40.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = if (activeMineralTab == 2) Color.Yellow else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(if (isFarsiMode) "جستجوی ۵۰۰ کانی" else "AI 500+ Minerals", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Collapsible Mohs Hardness Scale & Field Reference Guide
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (mohsScaleExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { mohsScaleExpanded = !mohsScaleExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isFarsiMode) "مقياس سختی موهس و راهنمای تست صحرایی" else "Mohs Hardness Scale & Field Scratch Guide",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = if (mohsScaleExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (mohsScaleExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val scaleItems = remember {
                        listOf(
                            Triple("1", if (isFarsiMode) "تالک (نرم‌ترین - با ناخن دست به راحتی پودر می‌شود)" else "Talc (Softest - feels soapy, easily scratched by fingernail)", Color(0xFF4CAF50)),
                            Triple("2", if (isFarsiMode) "ژیپس (با ناخن دست خط برمی‌دارد)" else "Gypsum (Scratched by fingernail)", Color(0xFF8BC34A)),
                            Triple("3", if (isFarsiMode) "کلسیت (با سکه مسی خراش برمی‌دارد)" else "Calcite (Scratched by copper coin)", Color(0xFFCDDC39)),
                            Triple("4", if (isFarsiMode) "فلوریت (با چاقوی جیبی به راحتی خط می‌افتد)" else "Fluorite (Scratched easily by pocket knife)", Color(0xFFFFEB3B)),
                            Triple("5", if (isFarsiMode) "آپاتیت (با چاقوی جیبی به سختی خط می‌افتد)" else "Apatite (Scratched with difficulty by knife)", Color(0xFFFFC107)),
                            Triple("6", if (isFarsiMode) "ارتوکلاز (شیشه پنجره را خط می‌اندازد)" else "Orthoclase (Scratches window glass)", Color(0xFFFF9800)),
                            Triple("7", if (isFarsiMode) "کوارتز (شیشه و فولاد چاقو را به راحتی خط می‌اندازد)" else "Quartz (Scratches steel & glass easily)", Color(0xFFFF5722)),
                            Triple("8", if (isFarsiMode) "توپاز (شیشه را عمیق خط می‌اندازد)" else "Topaz (Scratches quartz)", Color(0xFFF44336)),
                            Triple("9", if (isFarsiMode) "کوراندوم (فقط توسط الماس خط می‌افتد)" else "Corundum (Scratched only by diamond)", Color(0xFFE91E63)),
                            Triple("10", if (isFarsiMode) "الماس (سخت‌ترین ماده طبیعی، همه کانی‌ها را خط می‌اندازد)" else "Diamond (Hardest - scratches all substances)", Color(0xFF9C27B0))
                        )
                    }

                    val fieldObjects = remember {
                        listOf(
                            Triple(if (isFarsiMode) "💅 ناخن دست" else "💅 Fingernail", "2.5", Color(0xFF4CAF50)),
                            Triple(if (isFarsiMode) "🪙 سکه مسی" else "🪙 Copper Coin", "3.5", Color(0xFFCDDC39)),
                            Triple(if (isFarsiMode) "🔪 چاقوی فولادی" else "🔪 Pocket Knife", "5.5", Color(0xFFFFC107)),
                            Triple(if (isFarsiMode) "🖥️ شیشه پنجره" else "🖥️ Window Glass", "5.5", Color(0xFFFF9800)),
                            Triple(if (isFarsiMode) "🛠️ سوهان فولادی" else "🛠️ Steel File", "6.5", Color(0xFFFF5722)),
                            Triple(if (isFarsiMode) "🧱 چینی بدون لعاب" else "🧱 Streak Plate", "6.5", Color(0xFFE91E63))
                        )
                    }

                    Text(
                        text = if (isFarsiMode) "ابزارهای صحرایی مرجع سختی:" else "Field Reference Tools:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        fieldObjects.forEach { (name, hardness, color) ->
                            Box(
                                modifier = Modifier
                                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(color, RoundedCornerShape(3.dp)))
                                    Text(text = "$name: $hardness", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        scaleItems.forEach { (level, desc, color) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(color, RoundedCornerShape(9.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = level, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(color.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                        .border(0.5.dp, color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(text = desc, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (activeMineralTab == 0) {
            // Mode selector: Quick Key Selector vs Comprehensive 10-Question Wizard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { isQuickMode = true },
                    modifier = Modifier.weight(1.0f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isQuickMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (isQuickMode) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFarsiMode) "عیب‌یاب فوق‌سریع کانی" else "Quick 4-Step Key", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { isQuickMode = false },
                    modifier = Modifier.weight(1.0f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isQuickMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (!isQuickMode) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFarsiMode) "آزمون تخصصی ۱۰ سوالی" else "Classic 10Q Wizard", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isQuickMode) {
                // EASY / INTERACTIVE DYNAMIC MULTI-STEP FINDER
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
                                             .clickable { quickHardness = value }
                                             .padding(horizontal = 10.dp, vertical = 8.dp),
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         RadioButton(
                                             selected = isSelected,
                                             onClick = { quickHardness = value },
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
                                             .clickable { quickLuster = value }
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
                                             .clickable { quickMagnet = value }
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
                             onClick = { resetQuickFinder() },
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
                                     onClick = { quickAcid = !quickAcid },
                                     label = { Text(if (isFarsiMode) "🧪 جوشش با اسید" else "🧪 Acid Reaction", fontSize = 10.sp) },
                                     leadingIcon = if (quickAcid) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) } } else null
                                 )
                                 FilterChip(
                                     selected = quickSalty,
                                     onClick = { quickSalty = !quickSalty },
                                     label = { Text(if (isFarsiMode) "👅 طعم شور" else "👅 Salty Taste", fontSize = 10.sp) },
                                     leadingIcon = if (quickSalty) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) } } else null
                                 )
                                 FilterChip(
                                     selected = quickSoapy,
                                     onClick = { quickSoapy = !quickSoapy },
                                     label = { Text(if (isFarsiMode) "🧈 احساس چربی/صابون" else "🧈 Soapy Feel", fontSize = 10.sp) },
                                     leadingIcon = if (quickSoapy) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) } } else null
                                 )
                                 FilterChip(
                                     selected = quickSheets,
                                     onClick = { quickSheets = !quickSheets },
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
                                shape = RoundedCornerShape(16.dp),
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
            } else {
                // RENDER ORIGINAL 10-QUESTION DEEP COMPREHENSIVE WIZARD
                Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header progress bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFarsiMode) "پرسشنامه فیلتر هوشمند" else "Smart Identification Progress",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Text(
                            text = if (isFarsiMode) "گام $currentStep از ۱۰" else "Step $currentStep of 10",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = currentStep / 10f,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // CURRENT QUESTION DISPLAY (10 robust steps in Persian/English)
                    when (currentStep) {
                        1 -> {
                            WizardQuestionPane(
                                title = if (isFarsiMode) "۱. جلای انعکاسی کانی (Luster):" else "1. Reflective Luster:",
                                desc = if (isFarsiMode) "سطح تازه‌شکسته کانی چگونه نور را منعکس می‌کند؟" else "How does the freshly broken mineral surface reflect light?",
                                options = listOf(
                                    Triple("metallic", if (isFarsiMode) "جلای فلزی (Metallic)" else "Metallic Luster", if (isFarsiMode) "براق و کاملاً صیقلی مانند فلز خام (مثال: پیریت، گالن، مگنتیت)" else "High metallic reflection like metals"),
                                    Triple("submetallic", if (isFarsiMode) "جلای نیمه‌فلزی (Submetallic)" else "Submetallic Luster", if (isFarsiMode) "درخشش ضعیف‌تر فلزی کدر شده (مثال: هماتیت، گرافیت)" else "Weathered metallic ores or dull metal shine"),
                                    Triple("non_metallic", if (isFarsiMode) "جلای غیرفلزی (Non-Metallic)" else "Non-Metallic Luster", if (isFarsiMode) "جلاهای شیشه‌ای، مرواریدی، چرب، صابونی یا کدر خاکی (سایر کانی‌ها)" else "Glassy, silky, greasy, pearly, or dull earthy surfaces"),
                                    Triple("any", if (isFarsiMode) "نادیده گرفتن این فیلتر" else "Ignore Luster Filter", if (isFarsiMode) "عبور بدون درنظر گرفتن جلای کانی" else "Skip luster constraints entirely")
                                ),
                                selectedVal = answerLuster,
                                onSelect = { answerLuster = it }
                            )
                        }
                        2 -> {
                            WizardQuestionPane(
                                title = if (isFarsiMode) "۲. مقیاس سختی تجربی کانی (Hardness Scale):" else "2. Mohs Hardness Level:",
                                desc = if (isFarsiMode) "کدام شی روی بدنه کانی خط می‌اندازد؟ ناخن (۲.۵)، سکه مسی (۳.۵)، چاقو (۵.۵) یا شیشه (۶.۰)" else "Which tool can scratch the freshly exposed mineral surface?",
                                options = listOf(
                                    Triple("very_soft", if (isFarsiMode) "بسیار نرم (موهس کمتر از ۲.۵)" else "Very Soft (Mohs <= 2.5)", if (isFarsiMode) "با ناخن دست به راحتی خش می‌افتد (مثال: تالک، ژیپس)" else "Easily scratched by your fingernail"),
                                    Triple("soft", if (isFarsiMode) "نرم (موهس بین ۲.۵ تا ۳.۵)" else "Soft (Mohs 2.5 to 3.5)", if (isFarsiMode) "با ناخن آسیب نمی‌بیند ولی با سکه یا میخ مسی خراش می‌خورد (کلسیت)" else "Resists fingernail, but scratched by a copper coin"),
                                    Triple("medium", if (isFarsiMode) "سختی متوسط (موهس ۴.۰ تا ۵.۵)" else "Medium (Mohs 4.0 to 5.5)", if (isFarsiMode) "با مس خراش نمی‌خورد ولی با چاقوی فولادی یا میخ آهنی خط می‌افتد (فلوریت)" else "Copper coin fails, but scratched comfortably by steel pocket knife"),
                                    Triple("hard", if (isFarsiMode) "سخت و محکم (موهس بالاتر از ۵.۵)" else "Hard (Mohs > 5.5)", if (isFarsiMode) "روی شیشه خط می‌اندازد و فولاد چاقو روی آن سر می‌خورد (کوارتز، مگنتیت)" else "Easily scratches glass plates, iron knife cannot scratch it"),
                                    Triple("any", if (isFarsiMode) "نادیده گرفتن مقیاس سختی" else "Ignore Hardness Filter", if (isFarsiMode) "عبور از غربال‌گری سختی" else "Continue with all hardness levels")
                                ),
                                selectedVal = answerHardness,
                                onSelect = { answerHardness = it }
                            )
                        }
                        3 -> {
                            WizardQuestionPane(
                                title = if (isFarsiMode) "۳. رنگ گچ / خاکه کانی (Streak Powder Color):" else "3. Streak Powder Color:",
                                desc = if (isFarsiMode) "وقتی کانی را روی صفحه چینی بی لعاب می‌کشید، چه رنگ پودری بر جای سایش می‌ماند؟" else "Rub the mineral against an unglazed porcelain streak plate. What is the powder color?",
                                options = listOf(
                                    Triple("white", if (isFarsiMode) "رنگ مایل به سفید یا بی‌رنگ" else "White / Pale Colorless", if (isFarsiMode) "اثر پودر سفید گچی یا شفاف پدید می‌آید (کلسیت، کلسیم، کوارتز)" else "Creates white or barely visible powder streak"),
                                    Triple("red", if (isFarsiMode) "سرخ گیلاسی تا قهوه‌ای" else "Cherry Red / Red-Brown", if (isFarsiMode) "رنگ مشخص قرمز آجری یا کلوخه آهن رسوبی (هماتیت)" else "Characteristic reddish-brown cherry red streak"),
                                    Triple("dark", if (isFarsiMode) "تیره مایل به سبز یا سیاه" else "Dark Green-Black to Black", if (isFarsiMode) "خاکه سیاه یا مایل به سبز تیره از فلز برنزی پدید می‌آید (پیریت، مگنتیت، کالکوپیریت)" else "Coals of greenish-black or dense blocky black streak"),
                                    Triple("gray", if (isFarsiMode) "خاکستری سربی تا خاکستری نقره‌ای" else "Lead Gray to Silver Gray", if (isFarsiMode) "خاکه سربی متالیک غلیظ بر جای می‌گذارید (گالن، گرافیت)" else "Dense reflective gray or dark lead-gray color streak"),
                                    Triple("yellow", if (isFarsiMode) "زرد مایل به نارنجی یا لیمویی" else "Yellow to Brownish-Yellow", if (isFarsiMode) "خاکه کاملاً رنگ زرد به جا می‌گذارد" else "Yellowish or faint mustard orange powder"),
                                    Triple("any", if (isFarsiMode) "رد شدن و بدون لحاظ فیلتر" else "Ignore Streak Filter", if (isFarsiMode) "این ویژگی برایم ملموس نیست" else "Skip streak powder checking")
                                ),
                                selectedVal = answerStreak,
                                onSelect = { answerStreak = it }
                            )
                        }
                        4 -> {
                            WizardQuestionPane(
                                title = if (isFarsiMode) "۴. تست اسید کلریدریک رقیق (Acid Reaction):" else "4. HCl Acid Effervescence:",
                                desc = if (isFarsiMode) "آیا با ریختن قطره اسید سرد روی کانی گاز کربن دی اکسید ساطع می‌شود؟" else "Does dropping cold dilute hydrochloric acid make the mineral bubble?",
                                options = listOf(
                                    Triple("strong", if (isFarsiMode) "جوشش سریع و فراوان بلافاصله" else "Strong Effervescence Immediately", if (isFarsiMode) "بلافاصله جوشش پرحباب تندی ساطع می‌شود (مثال مبرهن کلسیت)" else "Violent and rapid bubbly carbon release immediately (Calcite)"),
                                    Triple("weak", if (isFarsiMode) "جوشش ضعیف (فقط روی پودر خراشیده)" else "Weak / Slow reaction (Powdered)", if (isFarsiMode) "کانی را ابتدا غبار کنید، پودر حاصله جوشش ملایمی دارد (دولومیت)" else "Fizzes moderately only when sample is powdered/scratched"),
                                    Triple("none", if (isFarsiMode) "بدون هیچ‌گونه واکنشی (سایر کانی‌ها)" else "Completely Inert / No Reaction", if (isFarsiMode) "فاقد جوشش یا خروج حباب است" else "No response or gas bubbles visible"),
                                    Triple("any", if (isFarsiMode) "مطمئن نیستم" else "Ignore Acid Filter", if (isFarsiMode) "این آزمایش انجام نشده است" else "Skip acid reaction filter parameters")
                                ),
                                selectedVal = answerAcid,
                                onSelect = { answerAcid = it }
                            )
                        }
                        5 -> {
                            WizardQuestionPane(
                                title = if (isFarsiMode) "۵. کشش و جاذبه مغناطیسی (Magnetism):" else "5. Magnetic Force Response:",
                                desc = if (isFarsiMode) "آیا آهنربای صحرایی نسبت به کانی واکنش و جذب نشان می‌دهد؟" else "Is the mineral specimen attracted to a hand magnet?",
                                options = listOf(
                                    Triple("magnetic", if (isFarsiMode) "بله، مغناطیسی قوی است" else "Strongly Magnetic", if (isFarsiMode) "آهنربا چسبیده و جذب می‌گردد یا عقربه قطب‌نما منحرف می‌شود (مگنتیت)" else "Attracts steel magnets instantly (Classic Magnetite)"),
                                    Triple("non_magnetic", if (isFarsiMode) "خیر، کاملاً غیرمغناطیسی" else "Completely Non-Magnetic", if (isFarsiMode) "کانی پدیده مغناطیسی ندارد" else "Zero magnetic indicators detected"),
                                    Triple("any", if (isFarsiMode) "نادیده گرفتن تست مغناطیسی" else "Ignore Magnetism Filter", if (isFarsiMode) "بخش مغناطیس لحاظ نگردد" else "Skip magnetism query parameters")
                                ),
                                selectedVal = answerMagnet,
                                onSelect = { answerMagnet = it }
                            )
                        }
                        6 -> {
                            WizardQuestionPane(
                                title = if (isFarsiMode) "۶. جهت رخ کانی و گسیختگی (Cleavage Planes):" else "6. Cleavage & Fracture Planes:",
                                desc = if (isFarsiMode) "کانی چگونه موازی سطوح صاف بازتابی می‌شکند یا گسیخته می‌شود؟" else "What flat surfaces are produced when the mineral is broken?",
                                options = listOf(
                                    Triple("sheets", if (isFarsiMode) "تورق عالی در ۱ جهت (ورقه‌ای)" else "Excellent Basal 1 Direction", if (isFarsiMode) "مانند برگه دفتر لایه‌لایه منعطف جدا می‌شود (تالک، مسکوویت، بیوتیت)" else "Peels cleanly into thin elastic bendable flakes"),
                                    Triple("cubic", if (isFarsiMode) "سه جهت عمود بر هم (رخ مکعبی)" else "Perfect Cubic 3 Directions", if (isFarsiMode) "شکست با زوایای قائم ۹۰ درجه بلوک مکعبی را می‌سازد (گالن، هالیت)" else "Produces square cube chunks with exact 90-degree angles"),
                                    Triple("rhombohedral", if (isFarsiMode) "سه جهت مایل غیرقائم (لوزی رخ)" else "Perfect Rhombohedral 3 Directions", if (isFarsiMode) "به صورت بلوک‌های بلور متوازی‌الاضلاع مورب بازتاب می‌دهد (کلسیت، دولومیت)" else "Breaks into oblique diamond rhombohedrons"),
                                    Triple("octahedral", if (isFarsiMode) "چهار جهت رخ متقاطع (هشت‌وجهی)" else "Octahedral cleavage in 4 directions", if (isFarsiMode) "در خلاف وجوه مکعب بلور را می‌تراشد (فلوریت)" else "Cracks systematically in 4 intersecting directions"),
                                    Triple("conchoidal", if (isFarsiMode) "فاقد رخ / شکستگی صدفی شیشه‌ای" else "None / Conchoidal glassy fracture", if (isFarsiMode) "فاقد سطوح شکست صاف، شکستگی هلالی مقعر شیشه‌ای تند (کوارتز، الیوین)" else "No cleavage planes, leaves curved smooth conchoidal lines"),
                                    Triple("any", if (isFarsiMode) "کل رخ‌ها نادیده گرفته شود" else "Ignore Cleavage Filter", if (isFarsiMode) "رخ کانی نادیده گرفته شود" else "No cleavage constraint matches")
                                ),
                                selectedVal = answerCleavage,
                                onSelect = { answerCleavage = it }
                            )
                        }
                        7 -> {
                            WizardQuestionPane(
                                title = if (isFarsiMode) "۷. حس سنگینی و چگالی نسبی (Feel of Weight):" else "7. Relative Density (Feel of Weight):",
                                desc = if (isFarsiMode) "جرم سنگ با توجه به ابعاد نمونه در دست چگونه ارزیابی می‌شود؟" else "Hold the sample. How dense and heavy does it feel for its size?",
                                options = listOf(
                                    Triple("light", if (isFarsiMode) "بسیار سبک و توخالی (کمتر از ۲.۵)" else "Light Weight (G < 2.5)", if (isFarsiMode) "حس کلوخه بسیار سبکی دارد (مثال: هالیت، ژیپس، گوگرد)" else "Surprisingly low density and light feel"),
                                    Triple("medium", if (isFarsiMode) "طبیعی و وزن نرمال پایه‌ای (۲.۵ تا ۴.۵)" else "Medium Density (G 2.5 - 4.5)", if (isFarsiMode) "چگالی متوسط اکثر سنگ‌های پوسته (کوارتز، کلسیت، فلدسپات)" else "Standard rock weight, no extraordinary sensation"),
                                    Triple("heavy", if (isFarsiMode) "فوق‌العاده سنگین و متراکم (بزرگتر از ۴.۵)" else "Abnormally High Density (G > 4.5)", if (isFarsiMode) "وزنی مشابه شمش فلز دارد (گالن، پیریت، هماتیت، مگنتیت)" else "Extremely heavy, feels like a solid ore of steel or lead"),
                                    Triple("any", if (isFarsiMode) "سنگینی نسبی ملاک نباشد" else "Ignore Weight Filter", if (isFarsiMode) "وزن نسبی بررسی نگردد" else "Do not constrain match search with density")
                                ),
                                selectedVal = answerDensity,
                                onSelect = { answerDensity = it }
                            )
                        }
                        8 -> {
                            WizardQuestionPane(
                                title = if (isFarsiMode) "۸. حس لمس سطحی کانی (Surface Touch Feel):" else "8. Specimen Touch Sensation:",
                                desc = if (isFarsiMode) "انگشت خود را روی سطح کانی بکشید. لایه‌ی کانی چه بافتی می‌دهد؟" else "Rub fingers against smooth surfaces. What texture is prominent?",
                                options = listOf(
                                    Triple("soapy", if (isFarsiMode) "فوق‌العاده صابونی، چرب و سرخورنده" else "Greasy to Highly Soapy", if (isFarsiMode) "مانند لایه صابون لغزنده است و رد چربی می‌گذارد (تالک، گرافیت)" else "Extremely slippery oil-slick feeling"),
                                    Triple("smooth", if (isFarsiMode) "صاف، صیقلی، خنک و شیشه‌ای" else "Smooth & Glassy", if (isFarsiMode) "بافت منظم شیشه‌ای صیقلی دارد (کوارتز، ژیپس، هالیت)" else "Polished glassy flat window texture"),
                                    Triple("dusty", if (isFarsiMode) "خاکی، کدر پودری و چسبنده" else "Earthy / Chalky dusty feel", if (isFarsiMode) "خشک است، به دست مالیده می‌شود و اندکی رطوبت دست را می‌گیرد (هماتیت)" else "Feels dry, transfers persistent dust chalk residue"),
                                    Triple("normal", if (isFarsiMode) "معمولی و فاقد حس سطحی خاص" else "Normal / No Outstanding Feel", if (isFarsiMode) "زبری سنگی عادی در اکثر کانی‌ها" else "Standard dry raw crystal surfaces"),
                                    Triple("any", if (isFarsiMode) "مجموع جلا و لمس نادیده گرفته شود" else "Ignore Touch Feel Filter", if (isFarsiMode) "ویژگی لمسی گذرانده شود" else "Skip texture feeling properties")
                                ),
                                selectedVal = answerFeel,
                                onSelect = { answerFeel = it }
                            )
                        }
                        9 -> {
                            WizardQuestionPane(
                                title = if (isFarsiMode) "۹. طعم یا حل‌شدگی بزاقی (Taste Test):" else "9. Gustatory Sensation (Taste Test):",
                                desc = if (isFarsiMode) "آیا کانی به آسانی طعم شور، گس یا تندی در زبان تولید می‌کند؟ (طعم هالیت)" else "Taste testing a single fresh corner of the specimen safely:",
                                options = listOf(
                                    Triple("salty", if (isFarsiMode) "شور بی‌بدیل نمک طعام (هالیت)" else "Highly Salty Taste", if (isFarsiMode) "انحلال سریع و شوری بلامنازع نمکی در زبان (هالیت)" else "Familiar table salt taste, dissolves extremely fast"),
                                    Triple("bitter", if (isFarsiMode) "تلخ، گس، سرد یا قابض" else "Bitter or Caustic Taste", if (isFarsiMode) "طعم‌های تلخ شبه شیمیایی" else "Unpleasant bitter chemical or astringent notes"),
                                    Triple("tasteless", if (isFarsiMode) "بدون طعم و فاقد اسید حل‌شدگی" else "Completely Tasteless", if (isFarsiMode) "خنثی و نامحلول در دهان" else "No taste or saliva solubility noticed"),
                                    Triple("any", if (isFarsiMode) "عدم انجام تست بررسی کانی" else "Ignore Taste Filter", if (isFarsiMode) "تست چشایی انجام نشود" else "Continue without taste constraints")
                                ),
                                selectedVal = answerTaste,
                                onSelect = { answerTaste = it }
                            )
                        }
                        10 -> {
                            WizardQuestionPane(
                                title = if (isFarsiMode) "۱۰. ساختار بلوری برجسته (Outstanding Crystal Habit):" else "10. Outstanding Crystal Shape:",
                                desc = if (isFarsiMode) "بلورهای کانی کلاً چه شکل و شمایل هندسی غالب دارند؟" else "What geometric habit or crystal form can you visually see?",
                                options = listOf(
                                    Triple("cubic", if (isFarsiMode) "مکعبی چهارگوش متمایز منظم" else "Cubic / Boxy crystals", if (isFarsiMode) "مکعب‌های بلوری براق یا زاویه‌دار ۹۰ درجه (گالن، پیریت، هالیت)" else "Square-faced cubic blocks or cubes"),
                                    Triple("hexagonal", if (isFarsiMode) "منشوری ستونی ۶ وجهی" else "Hexagonal columns / prisms", if (isFarsiMode) "ستون‌های شند استوانه‌ای کشیده نوک‌تیز (کوارتز)" else "Six-sided long columns with pyramidal tips"),
                                    Triple("plates", if (isFarsiMode) "برگه‌ای الاستیک و لایه‌لایه صاف" else "Micaceous Platy Sheets", if (isFarsiMode) "توده‌های لایه‌ای باریک و پولکی‌مانند چیده شده (میکاها)" else "Thin wafer plates flat and stacked together"),
                                    Triple("fibrous", if (isFarsiMode) "الیافی یا رشته‌های نخی صیقلی" else "Fibrous satin threads", if (isFarsiMode) "رشته‌های ابریشمی یا الیاف‌های فشرده سوزنی همراستا (ژیپس)" else "Silky fibrous threads or capillary needles"),
                                    Triple("rhombohedral", if (isFarsiMode) "لوزی‌شکل اریب (رومبوئدرال)" else "Oblique Rhombohedral faces", if (isFarsiMode) "بلورهای اریب با سطوح صیقلی متوازی‌الاضلاع (کلسیت، دولومیت)" else "Diamond-like angled block prisms"),
                                    Triple("massive", if (isFarsiMode) "کلوخه‌ای خاکی، بی‌شکل یا متراکم" else "Massive / Earthy / Amorphous", if (isFarsiMode) "توده‌های دانه ریز یا خاکی بدون بلور متمایز (هماتیت، گوگرد، الیوین)" else "Granular massive chunks with zero outstanding geometry"),
                                    Triple("any", if (isFarsiMode) "رد کردن این تست هندسی" else "Ignore Crystal Habit Filter", if (isFarsiMode) "شکل بلور مشخص یا در دسترس نیست" else "Skip shape geometry checking")
                                ),
                                selectedVal = answerShape,
                                onSelect = { answerShape = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Navigation wizard buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentStep > 1) currentStep-- else resetWizard() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isFarsiMode) "قبلی" else "Back")
                        }

                        // Secondary action: Reset
                        TextButton(onClick = { resetWizard() }) {
                            Text(if (isFarsiMode) "شروع مجدد" else "Reset Guide", color = MaterialTheme.colorScheme.error)
                        }

                        Button(
                            onClick = {
                                if (currentStep < 10) {
                                    currentStep++
                                }
                            },
                            enabled = currentStep <= 10,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (currentStep == 10) {
                                    if (isFarsiMode) "پایان تحلیل" else "Analysis Finished"
                                } else {
                                    if (isFarsiMode) "گام بعدی" else "Next Step"
                                }
                            )
                        }
                    }
                }
            }

            // DYNAMIC CLASSIFICATION RESULTS BOX
            Text(
                text = if (isFarsiMode) "نتایج تشخیص هوشمند بر اساس پاسخ‌ها:" else "Diagnosis Matches & Scores:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )

            val bestMatch = scoredWizardList.first()
            if (bestMatch.second <= 50) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = if (isFarsiMode) "تطابق قویی با کانی‌های موجود حاصل نشد. لطفاً پاسخ‌های خود را ارزیابی مجدد کنید یا از تب 'کاتالوگ دستی' استفاده نمایید." else "No highly probable match found (Score <= 50%). Review choices or consult Manual Catalog tool.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                // Show Best Mineral Match Card with progress indicator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isFarsiMode) "بهترین گزینه‌ی تشخیص داده شده:" else "Identified Candidate Mineral:",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = bestMatch.first.nameFa,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = bestMatch.first.nameEn,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }

                            // Match Score Widget
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (bestMatch.second >= 85) Color(0xFF00C853).copy(alpha = 0.15f) else Color(0xFFFFB300).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${bestMatch.second}% MATCH",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (bestMatch.second >= 85) Color(0xFF003300) else Color(0xFFE65100)
                                    )
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isFarsiMode) "سختی تجربی: ${bestMatch.first.hardnessRange.start.toInt()}" else "Mohs scale: ${bestMatch.first.hardnessRange.start.toInt()}",
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (isFarsiMode) "جلا: ${formatLuster(bestMatch.first.luster, isFarsiMode)}" else "Luster: ${formatLuster(bestMatch.first.luster, isFarsiMode)}",
                                    fontSize = 11.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isFarsiMode) "خاکه پودری: ${bestMatch.first.streak}" else "Streak: ${bestMatch.first.streak}",
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (isFarsiMode) "رنگ کانی: ${bestMatch.first.color}" else "Colors: ${bestMatch.first.color}",
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = if (isFarsiMode) "جهت رخ: ${bestMatch.first.cleavage}" else "Cleavage limits: ${bestMatch.first.cleavage}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { selectedMineralForLab = bestMatch.first },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFarsiMode) "آزمایشگاه صحرایی و شبیه‌ساز تست" else "Run Field Lab & Diagnostic Tests", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (isFarsiMode) "چگونه در صحرا تفکیک کنیم؟ ${bestMatch.first.specialFeat}" else "How to diagnose in field: ${bestMatch.first.enFeat}",
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

            // Small collapsible list of secondary candidates (if they want to look further)
            var showOtherMatches by remember { mutableStateOf(false) }
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showOtherMatches = !showOtherMatches }.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFarsiMode) "نمایش سایر احتمالات و کاندیداها (${scoredWizardList.size - 1})" else "Show alternative match candidates (${scoredWizardList.size - 1})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Icon(
                        imageVector = if (showOtherMatches) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (showOtherMatches) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            scoredWizardList.drop(1).take(4).forEach { (altMin, altScore) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(altMin.nameFa, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(altMin.nameEn, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = { selectedMineralForLab = altMin },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Science, contentDescription = "Lab test", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                        Text(
                                            text = "$altScore%",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (altScore >= 60) Color(0xFFE65100) else Color.Gray
                                        )
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            }
                        }
                    }
                }
            }
            } // end of else (isQuickMode)

        } else if (activeMineralTab == 1) {
            // ORIGINAL MANUAL FILTERS & CATALOG VIEW
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isFarsiMode) "فیلتر کارهای تشخیصی برای شناسایی سریع کانی:" else "Filter mineral diagnostic features for rapid identification:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = catalogSearchQuery,
                        onValueChange = { catalogSearchQuery = it },
                        placeholder = { 
                            Text(
                                text = if (isFarsiMode) "جستجوی نام، مشخصه یا رنگ کانی..." else "Search name, color, feature...",
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = if (catalogSearchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { catalogSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchHardness,
                            onValueChange = { searchHardness = it },
                            label = { Text(if (isFarsiMode) "سختی حدودی (مثلاً ۳ یا ۶)" else "Approx Hardness (e.g., 3, 6, 7)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )

                        // Luster selector
                        var expandedL by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f).align(Alignment.CenterVertically)) {
                            Button(
                                onClick = { expandedL = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(if (isFarsiMode) {
                                    when (filterLuster) {
                                        "All Lusters" -> "تمامی جلاها"
                                        "Metallic Luster" -> "جلای فلزی"
                                        else -> "جلای غیرفلزی"
                                    }
                                } else filterLuster, fontSize = 11.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = expandedL, onDismissRequest = { expandedL = false }) {
                                DropdownMenuItem(text = { Text(if (isFarsiMode) "تمامی جلاها" else "All Lusters") }, onClick = { filterLuster = "All Lusters"; expandedL = false })
                                DropdownMenuItem(text = { Text(if (isFarsiMode) "جلای فلزی" else "Metallic Luster") }, onClick = { filterLuster = "Metallic Luster"; expandedL = false })
                                DropdownMenuItem(text = { Text(if (isFarsiMode) "جلای غیرفلزی" else "Non-Metallic Luster") }, onClick = { filterLuster = "Non-Metallic"; expandedL = false })
                            }
                        }
                    }

                    // Mineral Group horizontal chip row
                    Text(
                        text = if (isFarsiMode) "دسته‌بندی کانی‌ها (گروه شیمیایی):" else "Mineral Chemical / Structural Group:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    val groupFilters = if (isFarsiMode) {
                        listOf("همه کانی‌ها", "سیلیکات‌ها", "کربنات‌ها", "سولفیدها", "سولفات‌ها", "اکسیدها", "هالیدها", "عناصر طبیعی")
                    } else {
                        listOf("All", "Silicates", "Carbonates", "Sulfides", "Sulfates", "Oxides", "Halides", "Elements")
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        groupFilters.forEach { group ->
                            val isSelected = selectedGroupFilter == group
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedGroupFilter = group },
                                label = { Text(group, fontSize = 9.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Special reactive chips bilingually
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterAcid,
                            onClick = { filterAcid = !filterAcid },
                            label = { Text(if (isFarsiMode) "جوشش با اسید (HCl)" else "Acid Reaction (HCl)", fontSize = 10.sp) },
                            leadingIcon = if (filterAcid) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                        )

                        FilterChip(
                            selected = filterMagnet,
                            onClick = { filterMagnet = !filterMagnet },
                            label = { Text(if (isFarsiMode) "خاصیت مغناطیسی" else "Magnetic Material", fontSize = 10.sp) },
                            leadingIcon = if (filterMagnet) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                        )

                        if (catalogSearchQuery.isNotEmpty() || searchHardness.isNotEmpty() || filterLuster != "All Lusters" || filterAcid || filterMagnet || (selectedGroupFilter != "All" && selectedGroupFilter != "همه کانی‌ها")) {
                            IconButton(
                                onClick = {
                                    catalogSearchQuery = ""
                                    searchHardness = ""
                                    filterLuster = "All Lusters"
                                    filterAcid = false
                                    filterMagnet = false
                                    selectedGroupFilter = if (isFarsiMode) "همه کانی‌ها" else "All"
                                },
                                modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset filters", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Results Indicator
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFarsiMode) "تعداد کانی کاتالوگ منطبق (${filteredCatalogList.size}):" else "Matching Minerals Found (${filteredCatalogList.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isFarsiMode) "مجموعاً ${minerals.size} کانی" else "Total DB: ${minerals.size} core rock-forming",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            if (filteredCatalogList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isFarsiMode) "هیچ کانی منطبقی یافت نشد. فیلترها را ریست کنید." else "No matching minerals found. Try clearing filters.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCatalogList) { mineral ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMineralForLab = mineral },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mineral.nameFa,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = mineral.nameEn,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                    }

                                    // Hardness tag
                                    val hardnessText = if (mineral.hardnessRange.start == mineral.hardnessRange.endInclusive) {
                                        mineral.hardnessRange.start.toString().replace(".0", "")
                                    } else {
                                        "${mineral.hardnessRange.start.toString().replace(".0", "")} - ${mineral.hardnessRange.endInclusive.toString().replace(".0", "")}"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Mohs: $hardnessText",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                                // Diagnostic list grid with Icons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(13.dp))
                                            Text(if (isFarsiMode) "رنگ: ${mineral.color}" else "Color: ${mineral.color}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Brush, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(13.dp))
                                            Text(if (isFarsiMode) "خاکه: ${mineral.streak}" else "Streak: ${mineral.streak}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(13.dp))
                                            Text(if (isFarsiMode) "جلا: ${formatLuster(mineral.luster, isFarsiMode)}" else "Luster: ${mineral.luster}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Scale, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(13.dp))
                                            Text(if (isFarsiMode) "چگالی: ${mineral.density} g/cm³" else "Density: ${mineral.density} g/cm³", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.BrokenImage, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = if (isFarsiMode) "رخ / گسیختگی: ${mineral.cleavage}" else "Cleavage/Fracture: ${mineral.cleavage}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = if (isFarsiMode) "شناسه صحرایی: ${mineral.specialFeat}" else "Field Identifiers: ${mineral.specialFeat}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isFarsiMode) "نکته زمین‌شناسی: ${mineral.enFeat}" else "Geological Tip: ${mineral.enFeat}",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                // Interactive action button to run field lab
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Science,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (isFarsiMode) "ورود به شبیه‌ساز تست صحرایی" else "Run Field Lab Simulator",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // AI EXPLORATION DATABASE VIEW (activeMineralTab == 2)
            AiMineralExplorerView(isFarsiMode)
        }

        selectedMineralForLab?.let { mineral ->
            MineralFieldLabDialog(
                mineral = mineral,
                isFarsi = isFarsiMode,
                onDismiss = { selectedMineralForLab = null }
            )
        }
    }
}

// Private helper block to draw beautifully padded cards for each question
@Composable
private fun WizardQuestionPane(
    title: String,
    desc: String,
    options: List<Triple<String, String, String>>,
    selectedVal: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = desc,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 15.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        options.forEach { (optionVal, optionLabel, optionDesc) ->
            val isSelected = selectedVal == optionVal
            Card(
                onClick = { onSelect(optionVal) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelect(optionVal) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )

                    Column {
                        Text(
                            text = optionLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = optionDesc,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineralFieldLabDialog(
    mineral: GeotechMineral,
    isFarsi: Boolean,
    onDismiss: () -> Unit
) {
    var activeLabTab by remember { mutableStateOf(0) } // 0: Scratch, 1: Streak, 2: Acid, 3: Magnetism
    
    // 1. Scratch states
    var selectedScratchTool by remember { mutableStateOf("nail") } // "nail", "coin", "knife", "glass"
    var scratchResultText by remember { mutableStateOf<String?>(null) }
    var scratchIsSuccess by remember { mutableStateOf<Boolean?>(null) }
    
    // 2. Streak states
    val strokePoints = remember { mutableStateListOf<Offset>() }
    var streakDrawnLength by remember { mutableStateOf(0) }
    val streakColor = remember(mineral.nameEn) { getStreakHexColor(mineral.nameEn) }
    
    // 3. Acid states
    var isAcidPoured by remember { mutableStateOf(false) }
    
    // 4. Magnet states
    var isMagnetNear by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFarsi) "🔬 آزمایشگاه صحرایی و شبیه‌ساز تست" else "🔬 Interactive Field Testing Lab",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${mineral.nameFa} (${mineral.nameEn})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Tabs Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabTitles = if (isFarsi) {
                        listOf("💅 تست سختی", "🧱 تست خاکه", "🧪 تست اسید", "🧲 مغناطيس")
                    } else {
                        listOf("💅 Scratch", "🧱 Streak", "🧪 Acid", "🧲 Magnet")
                    }
                    tabTitles.forEachIndexed { index, title ->
                        val isSelected = activeLabTab == index
                        FilterChip(
                            selected = isSelected,
                            onClick = { activeLabTab = index },
                            label = { Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tab Contents
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeLabTab) {
                        0 -> {
                            // SCRATCH TEST TAB
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (isFarsi) "ابزار خراش صحرایی را انتخاب کرده و سختی کانی را تخمین بزنید:" else "Select a field scratch tool to estimate the mineral hardness:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                val toolsList = listOf(
                                    Triple("nail", if (isFarsi) "💅 ناخن دست (سختی ۲.۵)" else "💅 Fingernail (Mohs 2.5)", 2.5),
                                    Triple("coin", if (isFarsi) "🪙 سکه مسی (سختی ۳.۵)" else "🪙 Copper Coin (Mohs 3.5)", 3.5),
                                    Triple("knife", if (isFarsi) "🔪 چاقوی فولادی / میخ (سختی ۵.۵)" else "🔪 Steel Knife/Needle (Mohs 5.5)", 5.5),
                                    Triple("glass", if (isFarsi) "🥛 صفحه شیشه‌ای (سختی ۶.۵)" else "🥛 Glass Plate (Mohs 6.5)", 6.5)
                                )
                                
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    toolsList.forEach { (id, label, hValue) ->
                                        val isSelected = selectedScratchTool == id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { 
                                                    selectedScratchTool = id
                                                    scratchResultText = null
                                                    scratchIsSuccess = null
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { 
                                                    selectedScratchTool = id
                                                    scratchResultText = null
                                                    scratchIsSuccess = null
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                                
                                Button(
                                    onClick = {
                                        val toolVal = toolsList.first { it.first == selectedScratchTool }.third
                                        val minHardness = mineral.hardnessRange.start
                                        val maxHardness = mineral.hardnessRange.endInclusive
                                        
                                        if (toolVal >= minHardness) {
                                            scratchIsSuccess = true
                                            scratchResultText = if (isFarsi) {
                                                "خراش موفقیت‌آمیز بود! 🌟 ابزار انتخابی شما (${toolVal}) از سختی پایه کانی (${minHardness}) بیشتر است. کانی خراش برداشت."
                                            } else {
                                                "Scratch Successful! 🌟 The selected tool hardness (${toolVal}) is greater than or equal to the mineral's baseline hardness (${minHardness}). The specimen is scratched."
                                            }
                                        } else {
                                            scratchIsSuccess = false
                                            scratchResultText = if (isFarsi) {
                                                "خراش ناموفق بود! ❌ ابزار انتخابی شما (${toolVal}) از سختی کانی (${minHardness} تا ${maxHardness}) نرم‌تر است. ابزار ساییده شده ولی کانی خراش برنمی‌دارد."
                                            } else {
                                                "Scratch Failed! ❌ The selected tool hardness (${toolVal}) is softer than the mineral's hardness (${minHardness} - ${maxHardness}). The tool itself is ground down, leaving no mark on the mineral."
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if (isFarsi) "شبیه‌سازی تست خراش" else "Simulate Scratch Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                scratchResultText?.let { result ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = if (scratchIsSuccess == true) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (scratchIsSuccess == true) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = result,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            color = if (scratchIsSuccess == true) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                
                                // Educational summary
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (isFarsi) "💡 یادداشت آموزشی سختی موهس:" else "💡 Mohs Hardness Scale Guide:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (isFarsi) {
                                                "سختی این کانی در بازه ${mineral.hardnessRange.start} تا ${mineral.hardnessRange.endInclusive} تعریف می‌شود. با مالش ابزارهای استاندارد صحرایی، می‌توان مرزهای کیفی سختی کانی را بدون دیتابیس هم به سرعت تعیین کرد."
                                            } else {
                                                "This mineral's Mohs hardness range is ${mineral.hardnessRange.start} to ${mineral.hardnessRange.endInclusive}. By testing with qualitative baseline instruments, you can successfully narrow its identity on site."
                                            },
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        1 -> {
                            // STREAK TEST TAB
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (isFarsi) "انگشت خود را روی کاشی چینی زیر بکشید تا اثر خاکه کانی مشخص شود:" else "Swipe your finger on the porcelain tile to see the mineral's diagnostic streak color:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(Color(0xFFFCFCFC), RoundedCornerShape(16.dp))
                                        .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                        .pointerInput(mineral.nameEn) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    strokePoints.add(offset)
                                                    streakDrawnLength += 5
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val lastPoint = strokePoints.lastOrNull()
                                                    val nextPoint = if (lastPoint != null) lastPoint + dragAmount else change.position
                                                    strokePoints.add(nextPoint)
                                                    streakDrawnLength += 1
                                                }
                                            )
                                        }
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawRect(
                                            color = Color(0xFFF0F0F0).copy(alpha = 0.4f)
                                        )
                                        
                                        if (strokePoints.isNotEmpty()) {
                                            for (i in 0 until strokePoints.size - 1) {
                                                val p1 = strokePoints[i]
                                                val p2 = strokePoints[i+1]
                                                if ((p1 - p2).getDistance() < 60f) {
                                                    drawLine(
                                                        color = streakColor,
                                                        start = p1,
                                                        end = p2,
                                                        strokeWidth = 16f,
                                                        cap = StrokeCap.Round
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (strokePoints.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                                                Text(
                                                    text = if (isFarsi) "اینجا خط بکشید..." else "Draw here with your finger...",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                    
                                    IconButton(
                                        onClick = { 
                                            strokePoints.clear()
                                            streakDrawnLength = 0
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                            .size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Clear plate", modifier = Modifier.size(16.dp))
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(10.dp)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(modifier = Modifier.size(12.dp).background(streakColor, CircleShape).border(0.5.dp, Color.Gray, CircleShape))
                                            Text(
                                                text = if (isFarsi) "خاکه کانی: ${mineral.streak}" else "Streak: ${mineral.streak}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                
                                if (streakDrawnLength > 15) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = if (isFarsi) "✨ تحلیل علمی رنگ خاکه کانی:" else "✨ Mineral Scientific Streak Analysis:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = if (isFarsi) {
                                                    "بدنه کانی ممکن است به دلیل ناخالصی‌ها تغییر رنگ دهد (رنگ ظاهری: ${mineral.color})، اما اثر پودر شده آن بر روی چینی بدون لعاب (${mineral.streak}) ویژگی بسیار دقیق‌تر و ثابت‌تری برای کانی‌شناس است."
                                                } else {
                                                    "While a mineral's apparent body color (${mineral.color}) varies due to structural impurities, its powdered streak color (${mineral.streak}) is extremely diagnostic and remains constant."
                                                },
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        2 -> {
                            // ACID TEST TAB
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (isFarsi) "آیا این کانی با اسید هیدروکلریک ۱۰٪ سرد واکنش می‌دهد؟ قطره اسید را بچکانید:" else "Does this mineral react with cold dilute 10% hydrochloric acid? Pour a drop:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(100.dp)
                                                .background(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(Color(0xFF9E9E9E), Color(0xFF616161))
                                                    ),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .border(2.dp, Color.DarkGray, RoundedCornerShape(20.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = mineral.nameEn.take(6),
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontWeight = FontWeight.Bold
                                            )
                                            
                                            if (isAcidPoured && mineral.acidReaction) {
                                                val infiniteTransition = rememberInfiniteTransition(label = "fizzing_anim")
                                                val b1Scale by infiniteTransition.animateFloat(
                                                    initialValue = 0.4f,
                                                    targetValue = 1.3f,
                                                    animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart),
                                                    label = "b1"
                                                )
                                                val b1Alpha by infiniteTransition.animateFloat(
                                                    initialValue = 1f,
                                                    targetValue = 0f,
                                                    animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart),
                                                    label = "ba1"
                                                )
                                                
                                                val b2Scale by infiniteTransition.animateFloat(
                                                    initialValue = 0.2f,
                                                    targetValue = 1.5f,
                                                    animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
                                                    label = "b2"
                                                )
                                                val b2Alpha by infiniteTransition.animateFloat(
                                                    initialValue = 1f,
                                                    targetValue = 0f,
                                                    animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
                                                    label = "ba2"
                                                )
                                                
                                                Box(modifier = Modifier.fillMaxSize()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.Center)
                                                            .size(14.dp)
                                                            .background(Color(0xFF81C784).copy(alpha = 0.5f), CircleShape)
                                                    )
                                                    
                                                    val offsets = listOf(
                                                        Offset(-20f, -20f), Offset(15f, 25f), Offset(-10f, 30f), Offset(25f, -15f)
                                                    )
                                                    offsets.forEachIndexed { i, offset ->
                                                        val scale = if (i % 2 == 0) b1Scale else b2Scale
                                                        val alpha = if (i % 2 == 0) b1Alpha else b2Alpha
                                                        Box(
                                                            modifier = Modifier
                                                                .offset(x = offset.x.dp, y = offset.y.dp)
                                                                .align(Alignment.Center)
                                                                .size((12 * scale).dp)
                                                                .background(
                                                                    Color(0xFFE8F5E9).copy(alpha = alpha),
                                                                    CircleShape
                                                                )
                                                                .border(1.dp, Color(0xFF81C784).copy(alpha = alpha), CircleShape)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Default.Science,
                                                contentDescription = null,
                                                tint = if (isAcidPoured) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Text("HCl (10%)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        if (isAcidPoured) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(8.dp)
                                                    .background(
                                                        if (mineral.acidReaction) Color(0xFFE8F5E9) else Color(0xFFECEFF1),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (mineral.acidReaction) {
                                                        val isDolomite = mineral.nameEn.lowercase().contains("dolomite")
                                                        if (isDolomite) {
                                                            if (isFarsi) "⚡ جوشش ضعیف (در خاکه یا اسید داغ!)" else "⚡ Weak Effervescence (On powder/warm)"
                                                        } else {
                                                            if (isFarsi) "💥 جوشش شدید و گاز CO2 فورا آزاد شد!" else "💥 Highly Reactive! Rapid CO2 fizzing"
                                                        }
                                                    } else {
                                                        if (isFarsi) "❌ بدون واکنش (خاکه و بدنه غیرفعال است)" else "❌ Inert (No reaction detected)"
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (mineral.acidReaction) Color(0xFF2E7D32) else Color(0xFF37474F)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Button(
                                    onClick = { 
                                        isAcidPoured = true 
                                    },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isFarsi) "چکاندن اسید کلریدریک" else "Pour Hydrochloric Acid Drop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                if (isAcidPoured) {
                                    OutlinedButton(
                                        onClick = { isAcidPoured = false },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (isFarsi) "تمیز کردن اسید از روی کانی" else "Wash/Clean Specimen Surface")
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = if (isFarsi) "💡 اهمیت تست اسید در صحرا:" else "💡 Importance of Acid Field Tests:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (isFarsi) {
                                                "این تست تفاوت اساسی کلسیت (شدید فورا فیز می‌کند) را از دولومیت (فقط در صورت کوبیده شدن پودری فیز می‌کند) و سیلیکات‌ها (بدون هیچ واکنشی) آشکار می‌کند."
                                            } else {
                                                "This acid test instantly discriminates Calcite (strong rapid fizzing) from Dolomite (requires powder or heat to fizz) and inert silicates."
                                            },
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        3 -> {
                            // MAGNETISM & TOUCH FEEL TAB
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (isFarsi) "تست نزدیکی آهنربا صحرایی و لمس فیزیکی کانی:" else "Field magnet attraction and physical touch indicators:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        val animXOffset by animateDpAsState(
                                            targetValue = if (isMagnetNear && mineral.magnetic) (-25).dp else 0.dp,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                            label = "specimen_offset"
                                        )
                                        
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .offset(x = animXOffset)
                                                .size(80.dp)
                                                .background(Color(0xFF424242), RoundedCornerShape(16.dp))
                                                .border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isFarsi) "کانی" else "Specimen",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        val magnetXOffset by animateDpAsState(
                                            targetValue = if (isMagnetNear) 40.dp else 100.dp,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                            label = "magnet_offset"
                                        )
                                        
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .offset(x = magnetXOffset)
                                                .size(60.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.OfflineBolt,
                                                contentDescription = "Magnet",
                                                tint = Color.Red,
                                                modifier = Modifier.size(54.dp)
                                            )
                                            Text(
                                                "N-S",
                                                fontSize = 8.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(8.dp)
                                                .background(
                                                    if (isMagnetNear) {
                                                        if (mineral.magnetic) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                                    } else Color(0xFFECEFF1),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isMagnetNear) {
                                                    if (mineral.magnetic) {
                                                        if (isFarsi) "🧲 جذب مغناطیسی قوی! کانی به آهنربا چسبید." else "🧲 Strong magnetic attraction! Specimen snapped."
                                                    } else {
                                                        if (isFarsi) "❌ غیرمغناطیسی: هیچ واکنشی دیده نشد." else "❌ Non-magnetic: No attraction detected."
                                                    }
                                                } else {
                                                    if (isFarsi) "آهنربا را برای تست نزدیک کنید" else "Bring the magnet closer to test"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isMagnetNear) {
                                                    if (mineral.magnetic) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                } else Color(0xFF37474F)
                                            )
                                        }
                                    }
                                }
                                
                                Button(
                                    onClick = { isMagnetNear = !isMagnetNear },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.OfflineBolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isMagnetNear) {
                                            if (isFarsi) "دور کردن آهنربا" else "Move Magnet Away"
                                        } else {
                                            if (isFarsi) "نزدیک کردن آهنربا صحرایی" else "Bring Magnet Close"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (isFarsi) "کارهای لمسی و ویژگی‌های فرعی:" else "Diagnostic Textures & Physical Feels:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(
                                                text = if (isFarsi) "🧈 لمس بدنه (Feel): ${mineral.feel}" else "Hand Feel: ${mineral.feel}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (isFarsi) "📐 بلوربندی (Habit): ${mineral.shape}" else "Crystal Habit: ${mineral.shape}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        
                                        if (mineral.taste != "none") {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isFarsi) "👅 ویژگی طعم در زبان: ${mineral.taste} (سوزاننده/شور)" else "👅 Taste indicators: ${mineral.taste} (Do not swallow!)",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Footer
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isFarsi) "اتمام آزمایش و بازگشت" else "Complete Lab & Return", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun getStreakHexColor(mineralNameEn: String): Color {
    val name = mineralNameEn.lowercase()
    return when {
        name.contains("hematite") -> Color(0xFF8B2500) // Cherry red / reddish brown
        name.contains("pyrite") || name.contains("chalcopyrite") || name.contains("pentlandite") -> Color(0xFF2E3B2E) // Dark greenish-black
        name.contains("magnetite") || name.contains("ilmenite") -> Color(0xFF222222) // Iron Black
        name.contains("galena") || name.contains("stibnite") -> Color(0xFF708090) // Lead Gray
        name.contains("graphite") -> Color(0xFF3A3A3A) // Dark lead gray
        name.contains("sulfur") || name.contains("orpiment") -> Color(0xFFE6D31C) // Yellow
        name.contains("malachite") -> Color(0xFF4CAF50) // Bright pale green
        name.contains("azurite") -> Color(0xFF4682B4) // Azure blue / light blue
        name.contains("bornite") -> Color(0xFF4F4F4F) // Grayish-black
        name.contains("chalcocite") -> Color(0xFF5C5C5C) // Dark lead-gray
        name.contains("sphalerite") -> Color(0xFF8B5A2B) // Light brown / golden yellow
        name.contains("arsenopyrite") -> Color(0xFF3F4F4F) // Dark gray-black
        name.contains("pyrrhotite") -> Color(0xFF555555) // Dark gray
        name.contains("molybdenite") -> Color(0xFF778899) // Greenish/bluish gray
        name.contains("gold") -> Color(0xFFFFD700) // Gold
        name.contains("silver") -> Color(0xFFC0C0C0) // Silver
        name.contains("chromite") -> Color(0xFF4A2F22) // Chocolate brown
        name.contains("cassiterite") -> Color(0xFF8B7355) // Pale brown
        name.contains("cinnabar") -> Color(0xFFE03C31) // Scarlet red
        name.contains("realgar") -> Color(0xFFFF5722) // Orange-red
        name.contains("chlorite") -> Color(0xFF6F8F6F) // Greenish-gray
        name.contains("limonite") -> Color(0xFFB8860B) // Yellowish brown
        name.contains("goethite") -> Color(0xFFCD9B1D) // Ochre yellow
        name.contains("bauxite") -> Color(0xFF8B4513) // Reddish brown
        name.contains("rutile") -> Color(0xFFB5A642) // Chalky yellow-brown
        name.contains("copper") -> Color(0xFFCD7F32) // Copper red
        else -> Color(0xFFE0E0E0) // White / Light Gray for all silicates and sulfates
    }
}

// ---------------------------------------------
// 2. LITHOLOGY IDENTIFICATION HELPER
// ---------------------------------------------
data class GeotechRock(
    val nameFa: String,
    val nameEn: String,
    val origin: String, // Igneous, Sedimentary, Metamorphic
    val texture: String,
    val typicalMinerals: String,
    val unitWeight: String, // kN/m³
    val typicalStrength: String, // MPa (UCS)
    val fieldTips: String,
    val description: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LithologyHelper() {
    val rocks = remember {
        listOf(
            GeotechRock("Granite", "Granite", "Intrusive Igneous", "Coarse-grained crystalline (Phaneritic)", "Quartz, Orthoclase (K-Feldspar), Plagioclase, Mica (Biotite)", "25 - 27 kN/m³", "100 - 250 MPa (Very Strong)", "Very high field strength, fully coarse crystalline texture, highly resistant to weathering", "The most abundant intrusive igneous rock in the continental crust. Used extensively in heavy geotechnical structures and dam foundations."),
            GeotechRock("Basalt", "Basalt", "Extrusive Igneous", "Fine-grained dark (Aphanitic) or vesicular", "Ca-plagioclase, Pyroxene, Olivine; lacks quartz", "28 - 31 kN/m³", "150 - 300 MPa (Extremely Strong)", "Extremely fine-grained black to dark gray texture, sometimes displays vesicular gas bubbles or column jointing", "The major constituent of oceanic crust. Extremely high mechanical strength but potential permeability from columnar joints needs analysis."),
            GeotechRock("Andesite", "Andesite", "Extrusive Igneous", "Fine-grained porphyritic (Porphyritic)", "Plagioclase, Hornblende, Pyroxene", "25 - 28 kN/m³", "80 - 180 MPa", "Medium to dark gray, contains prominent phenocryst crystals embedded in a fine-grained groundmass", "Formed at subduction zone volcanoes. Rated mechanical and cutting rock strength as medium to highly resistant."),
            GeotechRock("Sandstone", "Sandstone", "Clastic Sedimentary", "Clastic cemented (Clastic)", "Quartz grains, Feldspar, Calcite/Silica/Clay cements", "22 - 25 kN/m³", "30 - 120 MPa (Highly Variable)", "Gritty and sandpapery surface texture, sedimentary bedding layers are easily visible to the naked eye", "Common hydrocarbon reservoir and groundwater aquifer. Regular elastic brittle behavior is measured under shear tests."),
            GeotechRock("Limestone", "Limestone", "Chemical Sedimentary", "Microcrystalline dense, fossiliferous, or clastic", "Calcium carbonate (Calcite) matrix texture", "24 - 27 kN/m³", "40 - 140 MPa", "Strong effervescence with dilute HCl at room temperature, potential fossil frames or white calcite veins", "Highly prone to karstification, subterranean cavities, and heavy dissolution. Geotechnical loading requires extensive grout curtain testing."),
            GeotechRock("Shale", "Shale", "Clastic Sedimentary", "Very fine-grained laminated (Fissility)", "Clay minerals (Illite, Kaolinite), microscopic quartz", "20 - 24 kN/m³", "5 - 40 MPa (Very Soft / Friable)", "Highly laminated or wafery thin split sheets, smells damp/earthy when wet, easily degraded mechanics on failure plane", "The most common landslide-inducing sedimentary wall rock. Swelling potential in smectite-bearing shales is geotechnically critical."),
            GeotechRock("Conglomerate", "Conglomerate", "Clastic Sedimentary", "Coarse-grained rounded gravel", "Rounded pebbles (granite, limestone, quartz) in a sandy matrix", "23 - 26 kN/m³", "20 - 80 MPa", "Rounded pebbles/cobbles larger than 2 mm tightly locked in sandy cement matrix", "Highly heterogeneous mechanical behavior depending heavily on bounding cement strength. Exploration drilling is arduous due to contrasting pebble/matrix hardness."),
            GeotechRock("Schist", "Schist", "Foliated Metamorphic", "Foliated sheets of dense light-dark mica (Schistosity)", "Biotite, Muscovite, Feldspar, Quartz, sometimes Garnet or Talc", "26 - 28 kN/m³", "30 - 100 MPa", "Brilliant, highly shiny parallel alignments that catch light from mica flakes, clean splitting along foliation surfaces", "Highly anisotropic rock. Compressive strength parallel to foliation planes falls to its absolute minimum."),
            GeotechRock("Quartzite", "Quartzite", "Non-Foliated Metamorphic", "Non-foliated dense crystalline mosaic", "Recrystallized quartz from sandstone", "26 - 27 kN/m³", "150 - 300 MPa (Extremely Hard)", "Extreme hardness (steel blade cannot scratch it but is ground down), homogeneous glassy texture with no distinct grains", "The most resistant rock against environmental weathering. Hard rock TBM drilling results in severe disc cutter wear in quartzite layers."),
            GeotechRock("Marble", "Marble", "Non-Foliated Metamorphic", "Non-foliated crystalline mosaic (sugar-like)", "Recrystallized calcite from limestone base", "26 - 28 kN/m³", "50 - 120 MPa", "Reacts with cold dilute HCl, crystalline sugar-like mosaic texture, scratches easily with copper coin", "Highly decorative stone with mechanical characteristics similar to dense limestone. Vulnerable to dissolution under acidic rainwater.")
        )
    }

    var selectedOriginFilter by remember { mutableStateOf("All Origins") }
    val filteredRocks = remember(selectedOriginFilter) {
        if (selectedOriginFilter == "All Origins") rocks else {
            rocks.filter { it.origin.contains(selectedOriginFilter) }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Multi-choice Origin filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Geological Origin:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 4.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("All", "Igneous", "Sedimentary", "Metamorphic").forEach { orig ->
                    val isSelected = (orig == "All" && selectedOriginFilter == "All Origins") || 
                                     (orig != "All" && selectedOriginFilter == orig)
                    ElevatedAssistChip(
                        onClick = {
                            selectedOriginFilter = if (orig == "All") "All Origins" else orig
                        },
                        label = { Text(orig, fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredRocks) { rock ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
                                    Text(rock.nameFa, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when {
                                                    rock.origin.contains("Igneous") -> Color(0xFFC62828).copy(alpha = 0.15f)
                                                    rock.origin.contains("Sedimentary") -> Color(0xFFFBC02D).copy(alpha = 0.15f)
                                                    else -> Color(0xFF00ACC1).copy(alpha = 0.15f)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (rock.origin.contains("Igneous")) "Igneous" else if (rock.origin.contains("Sedimentary")) "Sedimentary" else "Metamorphic",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                rock.origin.contains("Igneous") -> Color(0xFFC62828)
                                                rock.origin.contains("Sedimentary") -> Color(0xFFE65100)
                                                else -> Color(0xFF00ACC1)
                                            }
                                        )
                                    }
                                }
                                Text(rock.nameEn, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            }

                            // UCS Strength tag
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Strength (UCS)", fontSize = 9.sp, color = Color.Gray)
                                Text(rock.typicalStrength, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                        // Engineering facts grid
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("Texture: ${rock.texture}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Minerals: ${rock.typicalMinerals}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(modifier = Modifier.weight(0.8f), horizontalAlignment = Alignment.End) {
                                Text("Unit Weight", fontSize = 9.sp, color = Color.Gray)
                                Text(rock.unitWeight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = rock.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )
                                Text(
                                    text = "Field Identification: ${rock.fieldTips}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)).padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------
// 3. GEOTECHNICAL UNIT CONVERTER
// ---------------------------------------------
@Composable
fun GeotechConverter() {
    var converterType by remember { mutableStateOf(0) } // 0 = Stress, 1 = Density, 2 = Length, 3 = Angle
    var inputValue by remember { mutableStateOf("1.0") }

    val decimalFormat = remember { DecimalFormat("#,##0.000000") }
    val normVal = inputValue.toDoubleOrNull() ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Stress / Press", "Density / Unit wt", "Length / Depth", "Dip Angles").forEachIndexed { index, mode ->
                ElevatedFilterChip(
                    selected = converterType == index,
                    onClick = { converterType = index },
                    label = { Text(mode, fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Input TextField
        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = { Text("Input Value to Convert") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (inputValue.isNotEmpty()) {
                    IconButton(onClick = { inputValue = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Simultaneous Unit Conversions:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                when (converterType) {
                    0 -> { // STRESS & PRESSURE (Base: MPa)
                        val mpa = normVal
                        ConverterResultRow("Megapascals (MPa)", mpa, "RMR rock rating base index & rock mass strength")
                        ConverterResultRow("Gigapascals (GPa)", mpa * 0.001, "Elastic Young's Modulus scale")
                        ConverterResultRow("Kilopascals (kPa)", mpa * 1000.0, "Soil bearing capacity standard")
                        ConverterResultRow("Pounds per sq inch (psi)", mpa * 145.0377, "Common Imperial/British unit")
                        ConverterResultRow("Kilogram-force per sq cm (kgf/cm²)", mpa * 10.19716, "Classical geotechnical index pressure")
                        ConverterResultRow("Bar (bar)", mpa * 10.0, "Hydromechanical & groundwater pressure")
                    }
                    1 -> { // DENSITY & UNIT WEIGHT (Base: g/cm³)
                        val gcm3 = normVal
                        ConverterResultRow("Grams per cubic cm (g/cm³)", gcm3, "Specific mineral grain density")
                        ConverterResultRow("Kilograms per cubic meter (kg/m³)", gcm3 * 1000.0, "Bulk engineering dry/wet density")
                        ConverterResultRow("Pounds per cubic foot (lb/ft³)", gcm3 * 62.42796, "Imperial dry unit density weight")
                        ConverterResultRow("Kilonewtons per cubic meter (kN/m³)", gcm3 * 9.80665, "Geotechnical in-situ unit weight (γ)")
                    }
                    2 -> { // LENGTH (Base: meter)
                        val m = normVal
                        ConverterResultRow("Meters (m)", m, "Borehole logging length or fault displacement scale")
                        ConverterResultRow("Centimeters (cm)", m * 100.0, "Joint aperture or core block spacing")
                        ConverterResultRow("Millimeters (mm)", m * 1000.0, "Clay seam thickness or micro-fault rifts")
                        ConverterResultRow("Feet (ft)", m * 3.28084, "Imperial borehole or excavation depth")
                        ConverterResultRow("Inches (in)", m * 39.37008, "Standard core barrel diameter sizing (e.g., HQ, NQ, BQ)")
                    }
                    3 -> { // ANGLE (Base: Degrees)
                        val deg = normVal
                        val rad = deg * (Math.PI / 180.0)
                        val grad = deg * (200.0 / 180.0)
                        ConverterResultRow("Degree (°)", deg, "Standard geological dip & strike angle system")
                        ConverterResultRow("Radian (rad)", rad, "Used for stability formulas & numerical analysis")
                        ConverterResultRow("Gradian (grad)", grad, "Used in mine surveying engineering layouts")
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            val strike = (deg + 90) % 360
                            Text(
                                text = "Field Guide: If the angle of the dip is ${normVal.toInt()}°, the maximum gradient down direction will be orthogonal (perpendicular) to the strike line of the bed (at ${strike.toInt()}° or ${((strike+180)%360).toInt()}°).",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConverterResultRow(
    unitName: String,
    value: Double,
    description: String
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val formattedVal = remember(value) {
        if (value == 0.0) "0"
        else if (value < 0.0001) String.format(Locale.US, "%.8f", value)
        else String.format(Locale.US, "%.4f", value)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clipboard.setText(AnnotatedString(formattedVal))
                Toast.makeText(context, "Copied $formattedVal to clipboard", Toast.LENGTH_SHORT).show()
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(unitName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(description, fontSize = 9.sp, color = Color.Gray)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = formattedVal,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.End
            )
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
        }
    }
}


// ---------------------------------------------
// 4. DEVELOPER PROFILE & SOCIAL LINKS
// ---------------------------------------------
@Composable
fun DeveloperProfilePage() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val links = remember {
        listOf(
            Triple("Geological Field Log Telegram Channel", "https://t.me/geofieldpro", Icons.Default.Send),
            Triple("GitHub Developer Profile", "https://github.com/farzad-f", Icons.Default.Code),
            Triple("LinkedIn Professional Profile", "https://www.linkedin.com/in/farzad-fathollahzadeh-b3a6a1165", Icons.Default.Hub)
        )
    }

    fun openLink(url: String) {
        val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open this link on your device.", Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Profile Card Header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(36.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
                    }

                    Text(
                        text = "Farzad Fathollahzadeh",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Founder & Lead Developer of Integrated Geological Field Systems",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "This application is a comprehensive mobile geological exploration helper. All databases—such as structural dip & strike mapping, borehole logging sheets, RMR engineering values, and Barton Q calculators—work hundred percent offline in remote field areas.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Justify,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Social Connect & Useful Links:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                links.forEach { (title, url, icon) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openLink(url) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = url,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(url))
                                        Toast.makeText(context, "Copied link to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp).align(Alignment.CenterVertically))
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "GeoField Pro version 1.4.0 • Built with Jetpack Compose & SQLite",
                fontSize = 10.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AiMineralExplorerView(isFarsiMode: Boolean) {
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var aiResult by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val suggestions = if (isFarsiMode) {
        listOf("آلونیت", "جاروسیت", "سریسیت", "اسپودومن", "ولاستونیت", "شیلیت", "پیریت", "کالکوپیریت", "هماتیت", "مگنتیت")
    } else {
        listOf("Alunite", "Jarosite", "Sericite", "Spodumene", "Wollastonite", "Scheelite", "Pyrite", "Chalcopyrite", "Hematite", "Magnetite")
    }

    val explorationGuides = if (isFarsiMode) {
        listOf(
            "دگرسانی پروپیلیتیک (اپیدوت)" to "Propylitic alteration (Epidote + Chlorite)",
            "دگرسانی فیلیک (سریسیت)" to "Phyllic alteration (Sericite + Pyrite)",
            "دگرسانی آرژیلیک پیشرفته" to "Advanced Argillic alteration (Alunite)",
            "کلاهک آهنی گوسان (گوتیت)" to "Iron Hat Gossan indicators (Goethite + Jarosite)",
            "کانی‌های ردیاب لیتیوم" to "Lithium Pegmatite pathfinders (Spodumene + Lepidolite)"
        )
    } else {
        listOf(
            "Propylitic Alteration" to "Propylitic alteration (Epidote + Chlorite)",
            "Phyllic Alteration" to "Phyllic alteration (Sericite + Pyrite)",
            "Advanced Argillic" to "Advanced Argillic alteration (Alunite)",
            "Gossan Iron Hat" to "Iron Hat Gossan indicators (Goethite + Jarosite)",
            "Lithium Pathfinders" to "Lithium Pegmatite pathfinders (Spodumene + Lepidolite)"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isFarsiMode) "پایگاه داده جامع کانی‌های اکتشافی (۵۰۰+ کانی)" else "Comprehensive Exploration Minerals DB (500+)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = if (isFarsiMode) {
                        "نام کانی درخواستی را به فارسی یا انگلیسی جستجو کنید تا اطلاعات کامل صحرایی و زمین‌شناسی آن با تکیه بر هوش مصنوعی گوگل بازیابی شود."
                    } else {
                        "Search for any mineral name (Farsi/English) to query physical, chemical, and exploration diagnostic profiles from Google Gemini AI."
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { 
                        Text(
                            text = if (isFarsiMode) "نام کانی (مثلاً جاروسیت، ولاستونیت، Spodumene)..." else "Mineral name (e.g., Alunite, Lepidolite)...",
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Chips for quick suggestions
                Text(
                    text = if (isFarsiMode) "کانی‌های پیشنهادی اکتشافی:" else "Suggested Pathfinder Minerals:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )

                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { name ->
                        FilterChip(
                            selected = searchQuery.lowercase() == name.lowercase(),
                            onClick = { searchQuery = name },
                            label = { Text(name, fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Pathfinder presets
                Text(
                    text = if (isFarsiMode) "راهنماهای اکتشافی و دگرسانی‌های سنگ دیواره:" else "Exploration Alteration Presets:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )

                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    explorationGuides.forEach { (label, target) ->
                        val isSelected = searchQuery == target
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                searchQuery = target
                                isLoading = true
                                aiResult = ""
                                coroutineScope.launch {
                                    aiResult = queryGeminiMineral(target, isFarsiMode)
                                    isLoading = false
                                }
                            },
                            label = { Text(label, fontSize = 9.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        if (searchQuery.trim().isEmpty()) {
                            Toast.makeText(context, if (isFarsiMode) "لطفاً ابتدا نام کانی را بنویسید" else "Please enter a mineral name first", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        aiResult = ""
                        coroutineScope.launch {
                            aiResult = queryGeminiMineral(searchQuery.trim(), isFarsiMode)
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isFarsiMode) "در حال استعلام از دیتابیس هوشمند..." else "Querying Smart AI DB...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFarsiMode) "استعلام از پایگاه کانی‌های هوشمند" else "Query AI Exploration DB", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (isLoading || aiResult.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isFarsiMode) "پروفایل اکتشافی: $searchQuery" else "Exploration Profile: $searchQuery",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (aiResult.isNotEmpty() && !aiResult.contains("Error") && !aiResult.contains("خطا")) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(aiResult))
                                    Toast.makeText(context, if (isFarsiMode) "گزارش کانی در حافظه کپی شد" else "Mineral report copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy report", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                            Text(
                                text = if (isFarsiMode) "تحلیل ساختار بلوری و مشخصات اکتشافی..." else "Analyzing crystal habit and exploration significance...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        // Display the fetched AI report
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = aiResult,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = if (isFarsiMode) TextAlign.Right else TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

suspend fun queryGeminiMineral(mineralName: String, isFarsi: Boolean): String = withContext(Dispatchers.IO) {
    val apiKey = com.example.BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "placeholder") {
        return@withContext if (isFarsi) {
            "کلید API هوش مصنوعی (Gemini API Key) در تنظیمات یا پرونده تنظیمات یافت نشد. لطفا کلید معتبر خود را در پنل Secrets وارد کنید تا به ۵۰۰+ کانی محبوب در اکتشاف دسترسی یابید."
        } else {
            "Gemini API key is missing or set to placeholder. Please add a valid key in the Secrets panel to query 500+ exploration minerals."
        }
    }

    val systemInstruction = if (isFarsi) {
        "شما یک کارشناس ارشد مکتشف کانی‌ها و زمین‌شناس نظام مهندسی معدن هستید. یک پروفایل فوق‌العاده تخصصی، فنی، سازمان‌یافته به زبان فارسی سلیس کانی یابی برای کانی درخواستی تولید کنید. از عناوین مشخص با ایموجی‌های مناسب استفاده کنید شامل: فرمول شیمیایی دقیق، گروه کانی‌شناسی، پارامترهای فیزیکی شاخص صحرایی (سختی، جلا، خاکه، چگالی، رخ)، نوع ذخایر مرتبط اکتشافی (مثلا پورفیری، فرومغناطیس، اسکارن، اپی‌ترمال، VMS)، اهمیت اکتشافی و فلز اکتشافی هدف و روش‌های تشخیص تفکیک دستی سریع در صحرا."
    } else {
        "You are an expert mineral exploration geologist. Generate an extremely professional, technical, well-structured mineral profile in English. Include: Exact Chemical Formula, Mineral Group, Diagnostic Physical Field Properties, Primary Associated Deposit Types (e.g., Porphyry, Epithermal, Skarn, VMS), Pathfinder & Exploration Significance, and Rapid Field Discrimination Tests."
    }

    val prompt = "Please generate a comprehensive exploration profile for the mineral: $mineralName."

    val jsonRequest = JSONObject().apply {
        put("contents", JSONArray().put(
            JSONObject().put("parts", JSONArray().put(
                JSONObject().put("text", prompt)
            ))
        ))
        put("systemInstruction", JSONObject().put("parts", JSONArray().put(
            JSONObject().put("text", systemInstruction)
        )))
    }

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = jsonRequest.toString().toRequestBody(mediaType)
    
    val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val request = Request.Builder()
        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
        .post(body)
        .build()

    try {
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful && responseBody.isNotEmpty()) {
            val jsonResponse = JSONObject(responseBody)
            if (jsonResponse.has("error")) {
                val errorObj = jsonResponse.optJSONObject("error")
                val errMsg = errorObj?.optString("message") ?: "Unknown API Error"
                if (isFarsi) "خطای وب‌سرویس: $errMsg" else "API Error: $errMsg"
            } else {
                val candidates = jsonResponse.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                val text = firstPart?.optString("text")
                text ?: if (isFarsi) "خطا: ساختار پاسخ معتبر نبود." else "Error: Invalid response format."
            }
        } else {
            var detailedError = ""
            if (responseBody.isNotEmpty()) {
                try {
                    val jsonResponse = JSONObject(responseBody)
                    val errorObj = jsonResponse.optJSONObject("error")
                    detailedError = errorObj?.optString("message") ?: ""
                } catch (e: Exception) {}
            }
            val errorMsg = if (detailedError.isNotEmpty()) {
                detailedError
            } else if (response.code == 400) {
                if (isFarsi) "کلید معتبر نیست یا فرمت درخواست اشتباه است." else "Bad request. Your API key might be invalid."
            } else {
                "HTTP ${response.code}: ${response.message}"
            }
            if (isFarsi) "خطا در ارتباط با سرور هوش مصنوعی: $errorMsg" else "API connection failed: $errorMsg"
        }
    } catch (e: Exception) {
        if (isFarsi) "خطای سیستمی رخ داد: ${e.localizedMessage}" else "System Exception: ${e.localizedMessage}"
    }
}

// ---------------------------------------------
// 6. STRUCTURAL GEOMETRY CALCULATOR
// ---------------------------------------------
fun compassToCartesian(centerX: Float, centerY: Float, radius: Float, angleDegrees: Double): Offset {
    val phiRad = Math.toRadians(angleDegrees - 90.0)
    return Offset(
        (centerX + radius * Math.cos(phiRad)).toFloat(),
        (centerY + radius * Math.sin(phiRad)).toFloat()
    )
}

@Composable
fun StructuralGeometryCalculator(isFarsiMode: Boolean) {
    var subTab by remember { mutableStateOf(0) } // 0 = Strike/Dip Conv, 1 = Apparent Dip, 2 = True Thickness
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = if (isFarsiMode) {
                listOf("تبدیل امتداد/شیب", "شیب ظاهری", "ضخامت لایه")
            } else {
                listOf("Strike/Dip Conv", "Apparent Dip", "True Thickness")
            }
            modes.forEachIndexed { index, mode ->
                ElevatedFilterChip(
                    selected = subTab == index,
                    onClick = { subTab = index },
                    label = { Text(mode, fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        
        when (subTab) {
            0 -> StrikeDipConverter(isFarsiMode)
            1 -> ApparentDipCalculator(isFarsiMode)
            2 -> TrueThicknessCalculator(isFarsiMode)
        }
    }
}

@Composable
fun StrikeDipConverter(isFarsiMode: Boolean) {
    var modeByStrike by remember { mutableStateOf(true) } // true = Input Strike/Dip, false = Input DipDir/Dip
    var inputStrike by remember { mutableStateOf("45") }
    var inputDip by remember { mutableStateOf("30") }
    var inputDipDir by remember { mutableStateOf("135") }
    
    val strike = inputStrike.toDoubleOrNull() ?: 0.0
    val dip = inputDip.toDoubleOrNull() ?: 0.0
    val dipDir = inputDipDir.toDoubleOrNull() ?: 0.0
    
    // Derived values
    val calculatedDipDir = remember(strike) { (strike + 90.0) % 360.0 }
    val calculatedStrike = remember(dipDir) { (dipDir - 90.0 + 360.0) % 360.0 }
    
    val activeStrike = if (modeByStrike) strike else calculatedStrike
    val activeDip = dip
    val activeDipDir = if (modeByStrike) calculatedDipDir else dipDir
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isFarsiMode) "روش ورودی:" else "Input Method:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.4f)
            )
            FilledTonalButton(
                onClick = { modeByStrike = !modeByStrike },
                modifier = Modifier.weight(0.6f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (modeByStrike) {
                        if (isFarsiMode) "امتداد و شیب ➔ جهت شیب" else "Strike/Dip ➔ Dip Dir"
                    } else {
                        if (isFarsiMode) "جهت شیب و شیب ➔ امتداد" else "Dip Dir/Dip ➔ Strike"
                    },
                    fontSize = 11.sp
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (modeByStrike) {
                OutlinedTextField(
                    value = inputStrike,
                    onValueChange = { inputStrike = it.take(5) },
                    label = { Text(if (isFarsiMode) "امتداد (Strike °)" else "Strike (°)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            } else {
                OutlinedTextField(
                    value = inputDipDir,
                    onValueChange = { inputDipDir = it.take(5) },
                    label = { Text(if (isFarsiMode) "جهت شیب (Dip Dir °)" else "Dip Direction (°)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            
            OutlinedTextField(
                value = inputDip,
                onValueChange = { inputDip = it.take(5) },
                label = { Text(if (isFarsiMode) "شیب (Dip °)" else "Dip Angle (°)", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        
        // Calculated result Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isFarsiMode) "نتایج تبدیل هندسی" else "Geometric Conversion Results",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isFarsiMode) "فرمت امتداد / شیب (RHR)" else "Strike / Dip (RHR)", fontSize = 10.sp, color = Color.Gray)
                        Text("${activeStrike.toInt()}° / ${activeDip.toInt()}°", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isFarsiMode) "فرمت جهت شیب / شیب" else "Dip Dir / Dip", fontSize = 10.sp, color = Color.Gray)
                        Text(String.format(Locale.US, "%03d° / %02d°", activeDipDir.toInt(), activeDip.toInt()), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        
        // Beautiful dynamic compass representation!
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isFarsiMode) "نمای قطب‌نما (صفحه دوبعدی لایه)" else "Compass View (Bed Plane 2D)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val primaryColorVal = MaterialTheme.colorScheme.primary
                    val secondaryColorVal = MaterialTheme.colorScheme.secondary
                    val onSurfaceVariantColorVal = MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = size.minDimension / 2f * 0.82f
                        
                        // Draw compass cardinal directions
                        drawContext.canvas.nativeCanvas.drawText("N", cx, cy - r + 12f, android.graphics.Paint().apply {
                            color = onSurfaceVariantColorVal.hashCode()
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        })
                        drawContext.canvas.nativeCanvas.drawText("S", cx, cy + r - 2f, android.graphics.Paint().apply {
                            color = onSurfaceVariantColorVal.hashCode()
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        })
                        drawContext.canvas.nativeCanvas.drawText("E", cx + r - 12f, cy + 8f, android.graphics.Paint().apply {
                            color = onSurfaceVariantColorVal.hashCode()
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        })
                        drawContext.canvas.nativeCanvas.drawText("W", cx - r + 14f, cy + 8f, android.graphics.Paint().apply {
                            color = onSurfaceVariantColorVal.hashCode()
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        })
                        
                        // Draw thin reference cross-hairs
                        drawLine(color = onSurfaceVariantColorVal.copy(alpha = 0.15f), start = Offset(cx, cy - r), end = Offset(cx, cy + r), strokeWidth = 1f)
                        drawLine(color = onSurfaceVariantColorVal.copy(alpha = 0.15f), start = Offset(cx - r, cy), end = Offset(cx + r, cy), strokeWidth = 1f)
                        
                        // 1. Draw Strike Line (passes through center from (strike) to (strike + 180))
                        val strikeStart = compassToCartesian(cx, cy, r * 0.9f, activeStrike)
                        val strikeEnd = compassToCartesian(cx, cy, r * 0.9f, activeStrike + 180.0)
                        drawLine(
                            color = secondaryColorVal,
                            start = strikeStart,
                            end = strikeEnd,
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                        
                        // 2. Draw Dip Direction Arrow starting from center to activeDipDir
                        val dipDirOffset = compassToCartesian(cx, cy, (r * 0.85f * (activeDip / 90.0).coerceIn(0.1, 1.0)).toFloat(), activeDipDir)
                        drawLine(
                            color = primaryColorVal,
                            start = Offset(cx, cy),
                            end = dipDirOffset,
                            strokeWidth = 8f,
                            cap = StrokeCap.Round
                        )
                        
                        // Draw small arrow head at dipDirOffset
                        val angleRad = Math.toRadians(activeDipDir - 90.0)
                        val arrowLength = 16f
                        val arrowAngle = Math.toRadians(35.0)
                        val p1 = Offset(
                            (dipDirOffset.x - arrowLength * Math.cos(angleRad - arrowAngle)).toFloat(),
                            (dipDirOffset.y - arrowLength * Math.sin(angleRad - arrowAngle)).toFloat()
                        )
                        val p2 = Offset(
                            (dipDirOffset.x - arrowLength * Math.cos(angleRad + arrowAngle)).toFloat(),
                            (dipDirOffset.y - arrowLength * Math.sin(angleRad + arrowAngle)).toFloat()
                        )
                        drawLine(color = primaryColorVal, start = dipDirOffset, end = p1, strokeWidth = 8f, cap = StrokeCap.Round)
                        drawLine(color = primaryColorVal, start = dipDirOffset, end = p2, strokeWidth = 8f, cap = StrokeCap.Round)
                        
                        // Draw tiny hub in center
                        drawCircle(color = onSurfaceVariantColorVal, radius = 6f)
                    }
                }
                
                Text(
                    text = if (isFarsiMode) {
                        "خط زرد نشان‌دهنده امتداد لایه (Strike) و فلش آبی رنگ نشان‌دهنده شیب لایه (Dip Direction) است. طول فلش متناسب با مقدار شیب ترسیم شده است."
                    } else {
                        "The yellow line shows Bed Strike and the blue arrow shows Bed Dip Direction. Arrow length scales with dip magnitude."
                    },
                    fontSize = 9.5.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
fun ApparentDipCalculator(isFarsiMode: Boolean) {
    var trueDip by remember { mutableStateOf("35") }
    var trueDipDir by remember { mutableStateOf("120") }
    var sectionTrend by remember { mutableStateOf("150") }
    
    val tDip = trueDip.toDoubleOrNull() ?: 0.0
    val tDipDir = trueDipDir.toDoubleOrNull() ?: 0.0
    val sTrend = sectionTrend.toDoubleOrNull() ?: 0.0
    
    // Beta is angle difference between True Dip Dir and Section Line
    val rawDiff = Math.abs(tDipDir - sTrend)
    val beta = if (rawDiff > 180.0) 360.0 - rawDiff else rawDiff
    
    val betaRad = Math.toRadians(beta)
    val tDipRad = Math.toRadians(tDip)
    val apparentDipRad = Math.atan(Math.tan(tDipRad) * Math.cos(betaRad))
    val apparentDipDeg = Math.toDegrees(apparentDipRad).coerceIn(0.0, 90.0)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isFarsiMode) {
                "محاسبه شیب ظاهری در مقاطع عرضی زمین‌شناسی براساس راستای مقطع و جهت شیب واقعی لایه‌ها"
            } else {
                "Calculate Apparent Dip on structural geological sections based on cross-section trend and true bedding dip"
            },
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 15.sp
        )
        
        OutlinedTextField(
            value = trueDip,
            onValueChange = { trueDip = it.take(5) },
            label = { Text(if (isFarsiMode) "زاویه شیب واقعی (True Dip °)" else "True Dip Angle (°)", fontSize = 10.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = trueDipDir,
                onValueChange = { trueDipDir = it.take(5) },
                label = { Text(if (isFarsiMode) "جهت شیب واقعی (True Dip Dir °)" else "True Dip Direction (°)", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            OutlinedTextField(
                value = sectionTrend,
                onValueChange = { sectionTrend = it.take(5) },
                label = { Text(if (isFarsiMode) "راستای مقطع (Section Line °)" else "Section Line Azimuth (°)", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isFarsiMode) "زاویه شیب ظاهری نهایی" else "Apparent Dip Result",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = String.format(Locale.US, "%.1f°", apparentDipDeg),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isFarsiMode) "اختلاف زاویه جهت‌ها (Beta):" else "Angle Offset (Beta):",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f°", beta),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun CrossSectionDiagramCanvas(
    width: Double,
    dip: Double,
    slope: Double,
    slopeDirectionSame: Boolean,
    thickness: Double,
    isFarsiMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isFarsiMode) "مقطع عرضی دوبعدی هندسه لایه‌بندی" else "2D Geological Cross-Section Diagram",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val tertiaryColor = MaterialTheme.colorScheme.tertiary
                val surfaceColor = MaterialTheme.colorScheme.surface
                val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

                Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    // 1. Draw horizontal reference baseline
                    drawLine(
                        color = gridColor,
                        start = Offset(20f, cy),
                        end = Offset(w - 20f, cy),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )

                    // 2. Draw ground surface line
                    val effSlopeDeg = if (slopeDirectionSame) -slope else slope
                    val slopeRad = Math.toRadians(effSlopeDeg)
                    val groundLength = w * 0.7f
                    val gDx = (groundLength / 2f * Math.cos(slopeRad)).toFloat()
                    val gDy = (groundLength / 2f * Math.sin(slopeRad)).toFloat()

                    val groundStart = Offset(cx - gDx, cy + gDy)
                    val groundEnd = Offset(cx + gDx, cy - gDy)

                    // Draw ground fill
                    val groundPath = Path().apply {
                        moveTo(groundStart.x, groundStart.y)
                        lineTo(groundEnd.x, groundEnd.y)
                        lineTo(groundEnd.x, h)
                        lineTo(groundStart.x, h)
                        close()
                    }
                    drawPath(
                        path = groundPath,
                        color = tertiaryColor.copy(alpha = 0.08f)
                    )

                    // Ground line
                    drawLine(
                        color = Color(0xFF795548), // Brown earth tone
                        start = groundStart,
                        end = groundEnd,
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )

                    // 3. Draw Top and Bottom Bedding Planes
                    val dipRad = Math.toRadians(dip)
                    val bedLength = w * 0.5f
                    val bDx = (bedLength / 2f * Math.cos(dipRad)).toFloat()
                    val bDy = (bedLength / 2f * Math.sin(dipRad)).toFloat()

                    // Top bedding plane (intersects ground center)
                    val topBedStart = Offset(cx - bDx, cy - bDy)
                    val topBedEnd = Offset(cx + bDx, cy + bDy)

                    drawLine(
                        color = primaryColor,
                        start = topBedStart,
                        end = topBedEnd,
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round
                    )

                    // Bottom bedding plane offset by thickness scale
                    val normRad = dipRad + Math.PI / 2.0
                    val thickPx = (h * 0.22f).coerceAtLeast(18f)
                    val nDx = (thickPx * Math.cos(normRad)).toFloat()
                    val nDy = (thickPx * Math.sin(normRad)).toFloat()

                    val botBedStart = Offset(topBedStart.x + nDx, topBedStart.y + nDy)
                    val botBedEnd = Offset(topBedEnd.x + nDx, topBedEnd.y + nDy)

                    drawLine(
                        color = primaryColor.copy(alpha = 0.7f),
                        start = botBedStart,
                        end = botBedEnd,
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f),
                        cap = StrokeCap.Round
                    )

                    // 4. Perpendicular thickness dimension arrow
                    val dimMidTop = Offset(cx, cy)
                    val dimMidBot = Offset(cx + nDx, cy + nDy)

                    drawLine(
                        color = Color(0xFFD32F2F), // Red highlight for thickness
                        start = dimMidTop,
                        end = dimMidBot,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )

                    // Dimension end caps
                    drawCircle(color = Color(0xFFD32F2F), radius = 4f, center = dimMidTop)
                    drawCircle(color = Color(0xFFD32F2F), radius = 4f, center = dimMidBot)

                    // Text labels
                    val paint = android.graphics.Paint().apply {
                        textSize = 24f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        if (isFarsiMode) "سطح زمین" else "Ground Surface",
                        groundEnd.x - 60f,
                        (groundEnd.y - 12f).coerceIn(20f, h - 20f),
                        paint.apply { color = android.graphics.Color.GRAY }
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        String.format(Locale.US, "t = %.2fm", thickness),
                        dimMidBot.x + 10f,
                        dimMidBot.y,
                        paint.apply { color = android.graphics.Color.RED }
                    )
                }
            }
        }
    }
}

@Composable
fun TrueThicknessCalculator(isFarsiMode: Boolean) {
    var outcropWidth by remember { mutableStateOf("12.5") }
    var traverseAngle by remember { mutableStateOf("90") } // Angle relative to strike (90 = perpendicular)
    var bedDip by remember { mutableStateOf("30") }
    var groundSlope by remember { mutableStateOf("10") }
    var slopeDirectionSame by remember { mutableStateOf(false) } // false = Opposite direction, true = Same direction

    val measuredWidth = outcropWidth.toDoubleOrNull() ?: 0.0
    val travAngle = (traverseAngle.toDoubleOrNull() ?: 90.0).coerceIn(1.0, 90.0)
    val dip = bedDip.toDoubleOrNull() ?: 0.0
    val slope = groundSlope.toDoubleOrNull() ?: 0.0

    // Correct measured width for non-perpendicular traverse angle
    val trueOutcropWidth = measuredWidth * Math.sin(Math.toRadians(travAngle))

    val dipRad = Math.toRadians(dip)
    val slopeRad = Math.toRadians(slope)

    val thickness = remember(trueOutcropWidth, dip, slope, slopeDirectionSame) {
        if (slopeDirectionSame) {
            Math.abs(trueOutcropWidth * Math.sin(dipRad - slopeRad))
        } else {
            trueOutcropWidth * Math.sin(dipRad + slopeRad)
        }
    }

    val verticalDepth = remember(trueOutcropWidth, dip, slope, slopeDirectionSame) {
        if (slopeDirectionSame) {
            trueOutcropWidth * Math.sin(dipRad) / Math.cos(slopeRad)
        } else {
            trueOutcropWidth * Math.sin(dipRad) * Math.cos(slopeRad)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isFarsiMode) {
                "محاسبه ضخامت واقعی لایه‌بندی و عمق قائم با اعمال زاویه پیمایش صحرایی، شیب لایه و توپوگرافی زمین"
            } else {
                "Calculate true stratigraphic thickness and vertical depth applying traverse angle, bed dip, and topography"
            },
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 15.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = outcropWidth,
                onValueChange = { outcropWidth = it.take(6) },
                label = { Text(if (isFarsiMode) "عرض اندازه‌گیری (m)" else "Measured Width (m)", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = traverseAngle,
                onValueChange = { traverseAngle = it.take(5) },
                label = { Text(if (isFarsiMode) "زاویه پیمایش به امتداد (°)" else "Traverse to Strike (°)", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = bedDip,
                onValueChange = { bedDip = it.take(5) },
                label = { Text(if (isFarsiMode) "شیب لایه (Bed Dip °)" else "Bed Dip Angle (°)", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = groundSlope,
                onValueChange = { groundSlope = it.take(5) },
                label = { Text(if (isFarsiMode) "شیب زمین (Ground Slope °)" else "Ground Slope (°)", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        // Relative Direction Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isFarsiMode) "جهت شیب زمین نسبت به لایه:" else "Terrain Slope relative to Bed Dip:",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.5f)
            )

            FilledTonalButton(
                onClick = { slopeDirectionSame = !slopeDirectionSame },
                modifier = Modifier.weight(0.5f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (slopeDirectionSame) {
                        if (isFarsiMode) "هم‌جهت" else "Same Direction"
                    } else {
                        if (isFarsiMode) "مخالف جهت" else "Opposite Direction"
                    },
                    fontSize = 11.sp
                )
            }
        }

        // Display result Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isFarsiMode) "ضخامت لایه‌بندی واقعی (True Stratigraphic Thickness)" else "True Stratigraphic Thickness",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )

                Text(
                    text = String.format(Locale.US, "%.2f m", thickness),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.tertiary
                )

                Divider(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isFarsiMode) "عرض واقعی عمود بر امتداد:" else "True Perpendicular Outcrop Width:",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f m", trueOutcropWidth),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isFarsiMode) "تخمین عمق قائم به کف لایه:" else "Est. Vertical Depth to Bed Base:",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f m", verticalDepth),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 2D Cross-Section Diagram
        CrossSectionDiagramCanvas(
            width = trueOutcropWidth,
            dip = dip,
            slope = slope,
            slopeDirectionSame = slopeDirectionSame,
            thickness = thickness,
            isFarsiMode = isFarsiMode
        )
    }
}


