package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// -------------------------------------------------------------
// AI Chat Message Model
// -------------------------------------------------------------

data class GeoChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val category: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// -------------------------------------------------------------
// AI Geological Assistant Module
// -------------------------------------------------------------

@Composable
fun AiGeoAssistantModule(
    isFarsiMode: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    var messages by remember {
        mutableStateOf(
            listOf(
                GeoChatMessage(
                    id = "init_0",
                    isUser = false,
                    text = if (isFarsiMode)
                        "سلام! من دستیار هوشمند زمین‌شناسی و معدن GeoField Pro هستم. می‌توانید درباره تشخیص سنگ‌ها، مدل‌های کانسارسازی (پورفیری، اپی‌ترمال، اسکارن و...)، زون‌های دگرسانی، تحلیل استریونت یا اصلاح یادداشت‌های صحرایی سوال بپرسید."
                    else
                        "Hello! I am your AI Geological & Mining Field Assistant. Ask me about lithology identification, ore deposit models (Porphyry, Epithermal, Skarn, VMS), alteration halos, stereonet interpretation, or field note polishing."
                )
            )
        )
    }

    val quickPrompts = listOf(
        Pair(
            if (isFarsiMode) "🔍 تشخیص زون دگرسانی هیدروترمال" else "🔍 Hydrothermal Alteration Zones",
            if (isFarsiMode)
                "لطفاً ویژگی‌ها، کانی‌های شاخص و متد تمایز زون‌های دگرسانی پتاسیک، فیلیک (سرسیتی)، آرژیلیک و پروپیلیتیک در کانسارهای مس-طلای پورفیری را شرح بده."
            else
                "Describe the key index minerals and diagnostic criteria to distinguish Potassic, Phyllic, Argillic, and Propylitic alteration halos in porphyry copper-gold deposits."
        ),
        Pair(
            if (isFarsiMode) "💎 راهنمای اکتشاف طلای اپی‌ترمال" else "💎 Epithermal Gold Exploration",
            if (isFarsiMode)
                "تفاوت‌های کلیدی سیستم‌های اپی‌ترمال Low-Sulfidation و High-Sulfidation از نظر بافت سیلیس، کانی‌شناسی کانسنگ و بافت‌های جوشش (Boiling Textures) چیست؟"
            else
                "What are the diagnostic differences between Low-Sulfidation and High-Sulfidation epithermal gold systems in terms of quartz textures and ore mineralogy?"
        ),
        Pair(
            if (isFarsiMode) "🪨 تخمین صحرایی شاخص GSI" else "🪨 Field GSI Estimation Guide",
            if (isFarsiMode)
                "چگونه شاخص مقاومت زمین‌شناسی (GSI) را برای یک توده سنگ درزه‌دار با هوازدگی متوسط در صحرا تخمین بزنم؟"
            else
                "How do I systematically estimate Geological Strength Index (GSI) in the field for a jointed, moderately weathered rock mass?"
        ),
        Pair(
            if (isFarsiMode) "📝 ویرایش علمی یادداشت صحرایی" else "📝 Polish Field Log Notes",
            if (isFarsiMode)
                "یک یادداشت صحرایی خام برای من بنویس که شامل لیتولوژی آندزیت پورفیری، دگرسانی سیلیسی-سرسیتی، رگچه‌های کوارتز-پیریت استوک‌ورک و شیب لایه ۴۵ درجه به شمال‌شرق باشد."
            else
                "Convert these raw observations into a professional geological outcrop description: porphyritic andesite, quartz-pyrite stockwork, sericite alteration, bedding dipping 45 deg NE."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Card
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
                        .size(44.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFarsiMode) "دستیار تخصصی هوش مصنوعی زمین‌شناسی" else "AI Geological Field Assistant",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isFarsiMode)
                            "پاسخ‌دهی آنی به پرسش‌های پترولوژی، متالوژنی، دگرسانی و ژئوتکنیک (آنلاین با Gemini + پایگاه داده آفلاین)"
                        else
                            "Real-time petrology, metallogeny, alteration & geotechnical guidance (Gemini AI + Offline Knowledge)",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Quick Topic Suggestions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { (label, prompt) ->
                SuggestionChip(
                    onClick = {
                        inputText = prompt
                    },
                    label = { Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Chat Message Stream
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .padding(8.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubbleItem(
                        message = msg,
                        isFarsiMode = isFarsiMode,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(msg.text))
                            android.widget.Toast.makeText(context, if (isFarsiMode) "متن کپی شد" else "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (isGenerating) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isFarsiMode) "در حال پردازش و استخراج پاسخ تخصصی..." else "Analyzing geological data...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        // Input & Send Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        if (isFarsiMode) "پرسش زمین‌شناسی یا توصیف صحرایی را وارد کنید..." else "Enter geology question or field notes...",
                        fontSize = 11.5.sp
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            IconButton(
                onClick = {
                    val prompt = inputText.trim()
                    if (prompt.isNotBlank() && !isGenerating) {
                        val userMsg = GeoChatMessage(
                            id = "u_${System.currentTimeMillis()}",
                            isUser = true,
                            text = prompt
                        )
                        messages = messages + userMsg
                        inputText = ""
                        isGenerating = true

                        coroutineScope.launch {
                            val answer = executeGeoAiQuery(prompt, isFarsiMode)
                            messages = messages + GeoChatMessage(
                                id = "a_${System.currentTimeMillis()}",
                                isUser = false,
                                text = answer
                            )
                            isGenerating = false
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                },
                enabled = inputText.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (inputText.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank() && !isGenerating) Color.White else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Chat Bubble Composable
// -------------------------------------------------------------

@Composable
fun ChatBubbleItem(
    message: GeoChatMessage,
    isFarsiMode: Boolean,
    onCopy: () -> Unit
) {
    val isUser = message.isUser
    val align = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(bgColor, RoundedCornerShape(12.dp))
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isUser) Icons.Default.Person else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isUser) (if (isFarsiMode) "زمین‌شناس صحرا" else "Field Geologist") else (if (isFarsiMode) "دستیار هوشمند GeoField" else "GeoField AI"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                    }

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                SelectionContainer {
                    Text(
                        text = message.text,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Geological Intelligence Engine (Online Gemini + Offline Matrix)
// -------------------------------------------------------------

suspend fun executeGeoAiQuery(prompt: String, isFarsi: Boolean): String = withContext(Dispatchers.IO) {
    val apiKey = try {
        BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String ?: ""
    } catch (e: Exception) {
        ""
    }

    if (apiKey.isNotBlank()) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            val systemInstruction = "You are GeoField Pro, an elite senior exploration geologist, structural geologist, and rock mechanics specialist. Provide concise, highly accurate, practical field advice using standard geological terminology."

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "$systemInstruction\n\nUser Question: $prompt"))
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val resStr = response.body?.string() ?: ""
                val resJson = JSONObject(resStr)
                val text = resJson.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                if (!text.isNullOrBlank()) {
                    return@withContext text.trim()
                }
            }
        } catch (e: Exception) {
            // Fallback to offline expert engine
        }
    }

    // Offline Expert Engine based on keywords
    val lower = prompt.lowercase()
    when {
        lower.contains("دگرسانی") || lower.contains("alteration") || lower.contains("پتاسیک") || lower.contains("فیلیک") || lower.contains("آرژیلیک") -> {
            if (isFarsi) {
                """
                🔷 **راهنمای تشخیص زون‌های دگرسانی هیدروترمال (Hydrothermal Alteration Guide):**
                
                ۱. **زون پتاسیک (Potassic):**
                   - کانی‌های شاخص: بیوتیت ثانویه (ریزبلور/قهوه‌ای)، فلدسپار پتاسیم صورتی/گوشتی (K-feldspar)، مگنتیت.
                   - محل تشکیل: هسته سیستم پورفیری، دمای بالا (>450°C).
                
                ۲. **زون فیلیک / سرسیتی (Phyllic/Sericitic):**
                   - کانی‌های شاخص: کوارتز + سرسیت (میکای ریزبلور سفید) + پیریت فراوان (بافت D-veins).
                   - نشانه هیدرولیز شدید فلدسپارها و شستشوی کاتیون‌ها.
                
                ۳. **زون آرژیلیک (Argillic):**
                   - کانی‌های شاخص: کائولینیت، اسمکتیت، ایلیت.
                   - ظاهر: سنگ سفید/شیری گچ‌مانند با چسبندگی به زبان و سختی پایین.
                
                ۴. **زون پروپیلیتیک (Propylitic):**
                   - کانی‌های شاخص: کلریت (سبز تیره)، اپیدوت (سبز پسته‌ای)، کلسیت، پیریت.
                   - موقعیت: هاله‌های خارجی سیستم (فاصله دور از هسته نفوذی).
                """.trimIndent()
            } else {
                """
                🔷 **Hydrothermal Alteration Zonation Guide:**
                
                1. **Potassic Zone (Core):**
                   - Key Minerals: Secondary Biotite (fine brown), K-Feldspar (pink rims/flooding), Magnetite, Anhydrite.
                   - Temperature: >450°C, near intrusive core.
                
                2. **Phyllic (Sericitic) Zone:**
                   - Key Minerals: Quartz + Sericite (fine white mica) + Pyrite (D-type veins).
                   - Indicates strong acid leaching & destruction of primary feldspars.
                
                3. **Argillic Zone:**
                   - Key Minerals: Kaolinite, Smectite, Illite.
                   - Field signature: Chalky white appearance, low hardness, sticky to tongue.
                
                4. **Propylitic Zone (Outer Halo):**
                   - Key Minerals: Chlorite (dark green), Epidote (pistachio green), Calcite, Albite.
                """.trimIndent()
            }
        }

        lower.contains("اپیتارمال") || lower.contains("اپی‌ترمال") || lower.contains("epithermal") || lower.contains("طلا") || lower.contains("gold") -> {
            if (isFarsi) {
                """
                🏆 **مدل کانسارسازی طلای اپی‌ترمال (Epithermal Gold Systems):**
                
                • **سیستم Low-Sulfidation (LS):**
                   - بافت‌های سیلیس: کوارتز کلوفرم (Colloform)، پوسته‌ای (Crustiform)، بلیدد کلسیت (Bladed Calcite pseudomorphs) ناشی از جوشش سیال (Boiling).
                   - سنگ درون‌گیر: خنثی تا ولکانیکی با دگرسانی پروپیلیتیک گسترده و آدولاریا.
                   - عیار طلا: متمرکز در رگه‌های کوارتزی پرعیار (Bonanza Ore).
                
                • **سیستم High-Sulfidation (HS):**
                   - بافت شاخص: سیلیس اسفنجی/حفره‌دار (Vuggy Silica) ناشی از شستشوی فوق‌اسیدی.
                   - کانی‌های همراه: انارژیت، لوزونیت، پیروفیلیت، آلونیت، آلونیت صورتی.
                """.trimIndent()
            } else {
                """
                🏆 **Epithermal Gold Mineralization Diagnostic Guide:**
                
                • **Low-Sulfidation (LS) Systems:**
                   - Quartz Textures: Crustiform, colloform banding, chalcedonic quartz, lattice-bladed calcite pseudomorphs (boiling indicator).
                   - Associated Minerals: Adularia, Electrum, Argentite, Base Metal sulfides.
                   - Style: Bonanza vein shoots.
                
                • **High-Sulfidation (HS) Systems:**
                   - Diagnostic Texture: **Vuggy Silica** produced by extreme acid leaching of all rock constituents except quartz.
                   - Key Minerals: Enargite/Luzonite, Pyrite, Alunite, Dickite, Pyrophyllite.
                """.trimIndent()
            }
        }

        lower.contains("gsi") || lower.contains("rmr") || lower.contains("ژئوتکنیک") || lower.contains("مکانیک سنگ") || lower.contains("joint") -> {
            if (isFarsi) {
                """
                🧱 **راهنمای تخمین صحرایی شاخص مقاومت زمین‌شناسی (GSI):**
                
                ۱. **ساختار هندسی توده سنگ (محور قائم):**
                   - توده سنگ بلوکی سالم (Blocky): ۳ دسته درزه متعامد متقاطع (GSI: 60-80).
                   - توده سنگ خیلی بلوکی (Very Blocky): درزه‌های پرتعداد با میانگین فواصل ۱۰ تا ۳۰ سانتی‌متر (GSI: 40-60).
                   - بلوکی خردشده / گسلی (Blocky/Disturbed/Seamy): چین‌خورده با خردشدگی سطحی (GSI: 20-40).
                   - متلاشی / لهیده (Disintegrated): فاقد ساختار بلوکی مشخص (GSI: <20).
                
                ۲. **کیفیت سطوح درزه‌ها (محور افقی):**
                   - خیلی خوب: سطوح زبر و بدون هوازدگی بدون پرشدگی رسی.
                   - خوب: سطوح زبر با لایه اکسید آهن جزئی.
                   - ضعیف: سطوح آینه‌ای (Slickensided) با پرشدگی رس متورم‌شونده.
                """.trimIndent()
            } else {
                """
                🧱 **Geological Strength Index (GSI) Field Assessment:**
                
                1. **Rock Structure (Vertical Axis):**
                   - Blocky: Well interlocked, undisturbed rock with 3 intersecting joint sets (GSI: 65-80).
                   - Very Blocky: Interlocked, partially disturbed with multi-faceted blocks (GSI: 40-65).
                   - Disturbed/Seamy: Folded and/or faulted with angular blocks (GSI: 25-45).
                   - Disintegrated: Heavily broken with mixture of fines (GSI: <20).
                
                2. **Surface Conditions of Discontinuities (Horizontal Axis):**
                   - Very Good: Very rough, unweathered surfaces.
                   - Fair: Smooth, moderately weathered with iron oxide staining.
                   - Poor: Slickensided surfaces with clay gouge/fillings.
                """.trimIndent()
            }
        }

        else -> {
            if (isFarsi) {
                """
                📌 **تحلیل تخصصی داده‌های صحرایی GeoField:**
                
                بررسی داده‌های وارد شده نشان می‌دهد که برای اعتبارسنجی دقیق صحرایی، موارد زیر توصیه می‌شود:
                ۱. **برداشت ساختاری:** ثبت حداقل ۱۰ تا ۱۵ برداشت Strike/Dip روی برون‌زد و بررسی در استریونت جهت تفکیک درزه‌ها از سطوح لایه‌بندی اصلی.
                ۲. **آزمایش اسید کلریدریک (HCl 10%):** کنترل واکنش جوشش کلسیت در برابر دولومیت و کلسیت‌های تأخیری.
                ۳. **بررسی لیتولوژی مادر:** ارزیابی هاله دگرسانی مجاور توده‌های نفوذی یا زون‌های گسلی.
                
                💡 برای دریافت جزییات بیشتر، می‌توانید نام سنگ یا مختصات عارضه را مطرح کنید.
                """.trimIndent()
            } else {
                """
                📌 **GeoField Expert Exploration Synthesis:**
                
                Based on your inquiry, recommended field validation steps:
                1. **Structural Verification:** Measure 10-15 Strike/Dip readings across the outcrop to isolate systematic tectonic joint sets on the Stereonet.
                2. **Chemical Reactivity:** Apply 10% cold HCl to distinguish calcite vs dolomite and hydrothermal veinlets.
                3. **Wallrock Alteration:** Examine intensity halos around shear zones and structural conduits.
                """.trimIndent()
            }
        }
    }
}
