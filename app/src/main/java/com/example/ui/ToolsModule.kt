package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    var selectedToolTab by remember { mutableStateOf(0) } // 0 = Mineral ID, 1 = Lithology, 2 = QAPF Plotter, 3 = Converter, 4 = Developer Profile

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Tab Title
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                     imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Column {
                    Text(
                        text = "Geotech Field Tools",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Handy micro-tools to accelerate field estimations and diagnostic classification",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Tab selection row
        ScrollableTabRow(
            selectedTabIndex = selectedToolTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            Tab(
                selected = selectedToolTab == 0,
                onClick = { selectedToolTab = 0 },
                text = { Text("Mineralogy", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 1,
                onClick = { selectedToolTab = 1 },
                text = { Text("Lithology", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 2,
                onClick = { selectedToolTab = 2 },
                text = { Text("QAPF Plotter", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 3,
                onClick = { selectedToolTab = 3 },
                text = { Text("Unit Converter", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 4,
                onClick = { selectedToolTab = 4 },
                text = { Text("Developer", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

        // Content Area
        Box(modifier = Modifier.weight(1f)) {
            when (selectedToolTab) {
                0 -> MineralIdHelper(viewModel)
                1 -> LithologyHelper()
                2 -> Box(modifier = Modifier.verticalScroll(rememberScrollState())) { QAPFClassifierPanel() }
                3 -> GeotechConverter()
                4 -> DeveloperProfilePage()
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MineralIdHelper(viewModel: GeologyViewModel) {
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()
    
    val minerals = remember {
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
            GeotechMineral("اکتینولیت (Actinolite)", "Actinolite", 5.5..6.0, "Pistachio dark green to emerald", "White", "non_metallic", "Perfect amphibole angles (56 and 124 deg)", 3.04, specialFeat = "بلورهای سوزنی شکل دراز شعاعی و نازک مانند چوب، کانی دگرگونی سنگ‌های شیست سبز و هاله‌های بزرگ اسکارن", enFeat = "Green fibrous amphibole, forms needle-like cluster sprays in greenschist facies and iron skarn zones")
        )
    }


    // Tab control
    var activeMineralTab by remember { mutableStateOf(0) } // 0 = Wizard, 1 = Manual Catalog, 2 = AI Co-Database (500+ Minerals)

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
    val filteredCatalogList = remember(searchHardness, filterLuster, filterAcid, filterMagnet) {
        val targetHardness = searchHardness.toDoubleOrNull()
        minerals.filter { min ->
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

            matchesHardness && matchesLuster && matchesAcid && matchesMagnet
        }
    }

    // Candidates sorted by wizard scores
    val scoredWizardList = remember(
        answerLuster, answerHardness, answerStreak, answerAcid, answerMagnet,
        answerCleavage, answerDensity, answerFeel, answerTaste, answerShape
    ) {
        minerals.map { min -> min to getScore(min) }.sortedByDescending { it.second }
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

        if (activeMineralTab == 0) {
            // STEP BY STEP DIAGNOSTIC WIZARD VIEW (10 questions total)
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
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            scoredWizardList.drop(1).take(4).forEach { (altMin, altScore) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(altMin.nameFa, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(altMin.nameEn, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                                    }
                                    Text(
                                        text = "$altScore%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (altScore >= 60) Color(0xFFE65100) else Color.Gray
                                    )
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            }
                        }
                    }
                }
            }

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

                        if (searchHardness.isNotEmpty() || filterLuster != "All Lusters" || filterAcid || filterMagnet) {
                            IconButton(
                                onClick = {
                                    searchHardness = ""
                                    filterLuster = "All Lusters"
                                    filterAcid = false
                                    filterMagnet = false
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
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Mohs Hardness: ${mineral.hardnessRange.start.toInt()}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                                // Diagnostic list grid
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (isFarsiMode) "رنگ: ${mineral.color}" else "Mineral Color: ${mineral.color}", fontSize = 11.sp)
                                        Text(if (isFarsiMode) "خاکه: ${mineral.streak}" else "Streak Powder: ${mineral.streak}", fontSize = 11.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (isFarsiMode) "جلا: ${mineral.luster}" else "Luster: ${mineral.luster}", fontSize = 11.sp)
                                        Text(if (isFarsiMode) "چگالی مخصوص: ${mineral.density} g/cm³" else "Specific Gravity: ${mineral.density} g/cm³", fontSize = 11.sp)
                                    }
                                }

                                Text(
                                    text = if (isFarsiMode) "رخ / گسیختگی: ${mineral.cleavage}" else "Cleavage/Fracture: ${mineral.cleavage}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

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
                            }
                        }
                    }
                }
            }
        } else {
            // AI EXPLORATION DATABASE VIEW (activeMineralTab == 2)
            AiMineralExplorerView(isFarsiMode)
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
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                    shape = RoundedCornerShape(12.dp),
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
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
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val text = firstPart?.optString("text")
            text ?: if (isFarsi) "پاسخی دریافت نشد." else "No response text"
        } else {
            val errorMsg = if (response.code == 400) {
                if (isFarsi) "خطای ۴۰۰: احتمالاً کلید نادرست است یا مدل در دسترس نیست." else "Error 400: Bad request. API key might be invalid."
            } else {
                "Error code ${response.code}: ${response.message}"
            }
            if (isFarsi) "خطا در ارتباط با سرور هوش مصنوعی: $errorMsg" else "API connection failed: $errorMsg"
        }
    } catch (e: Exception) {
        if (isFarsi) "خطای سیستمی رخ داد: ${e.localizedMessage}" else "System Exception: ${e.localizedMessage}"
    }
}

