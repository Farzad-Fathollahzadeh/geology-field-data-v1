package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.*
import java.text.SimpleDateFormat
import java.util.*
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@Composable
fun RockMechanicsContent(
    viewModel: GeologyViewModel,
    onNavigateToTab: ((com.example.MainTab) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val globalActiveProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val projectList by viewModel.projects.collectAsStateWithLifecycle()
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()

    // Decoupled tab-specific project selection to avoid trapping the user in a single project
    var localSelectedProjectId by remember { mutableStateOf<Int?>(null) }
    var lastSyncedGlobalProjectId by remember { mutableStateOf<Int?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(globalActiveProject) {
        if (globalActiveProject != null && globalActiveProject?.id != lastSyncedGlobalProjectId) {
            localSelectedProjectId = globalActiveProject?.id
            lastSyncedGlobalProjectId = globalActiveProject?.id
        }
    }

    val activeProject = projectList.find { it.id == localSelectedProjectId }

    var showCreateProjDialog by remember { mutableStateOf(false) }
    var projName by remember { mutableStateOf("") }
    var projDesc by remember { mutableStateOf("") }

    if (activeProject == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val path = Path()
                        val w = size.width
                        val h = size.height
                        
                        // Stratigraphic layered design
                        path.moveTo(0f, h * 0.7f)
                        path.quadraticTo(w * 0.3f, h * 0.5f, w * 0.6f, h * 0.8f)
                        path.quadraticTo(w * 0.8f, h * 0.95f, w, h * 0.6f)
                        path.lineTo(w, h)
                        path.lineTo(0f, h)
                        path.close()
                        drawPath(path = path, color = primaryColor.copy(alpha = 0.05f))

                        val path2 = Path()
                        path2.moveTo(0f, h * 0.83f)
                        path2.quadraticTo(w * 0.35f, h * 0.72f, w * 0.7f, h * 0.88f)
                        path2.lineTo(w, h)
                        path2.lineTo(0f, h)
                        path2.close()
                        drawPath(path = path2, color = primaryColor.copy(alpha = 0.08f))
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )

                    Text(
                        text = if (isFarsiMode) "مکانیک سنگ و رده‌بندی ژئوتکنیکی" else "Rock Mechanics & Geotechnical",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isFarsiMode) 
                            "یک پروژه اکتشافی یا مهندسی را برای محاسبات ساختاری RMR، پارامترهای توده سنگ هوک-بروان، رده‌بندی MRMR معدنی، شاخص توده سنگ RMi و سیستم Q انتخاب کنید."
                        else 
                            "Select a project for structural RMR calculations, Hoek-Brown rock mass parameters, miner MRMR classification, rock mass RMi, and Barton Q-system indexes.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            }

            Button(
                onClick = { showCreateProjDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isFarsiMode) "تعریف پروژه ژئوتکنیکی جدید" else "Define New Geotechnical Project",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Text(
                text = if (isFarsiMode) "پروژه‌های در دسترس:" else "Available Projects:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.Start)
            )

            if (projectList.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFarsiMode) "هنوز پروژه‌ای تعریف نشده است." else "No projects defined yet.",
                        color = Color.Gray,
                        fontSize = 12.5.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(projectList) { project ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    localSelectedProjectId = project.id
                                    viewModel.loadProject(project.id)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = project.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = project.description ?: (if (isFarsiMode) "پروژه ژئوتکنیکی عمومی" else "General Geotechnical Project"),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isFarsiMode) "مهندسی سنگ" else "Rock Eng.",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreateProjDialog) {
            AlertDialog(
                onDismissRequest = { showCreateProjDialog = false },
                title = { 
                    Text(
                        text = if (isFarsiMode) "تعریف پروژه ژئوتکنیکی جدید" else "Define New Geotechnical Project",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = projName,
                            onValueChange = { projName = it },
                            label = { Text(if (isFarsiMode) "نام پروژه" else "Project Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = projDesc,
                            onValueChange = { projDesc = it },
                            label = { Text(if (isFarsiMode) "شرح پروژه / محدوده" else "Scope/Description") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (projName.isNotBlank()) {
                                viewModel.createProjectSilently(projName, projDesc) { newId ->
                                    localSelectedProjectId = newId
                                    viewModel.loadProject(newId)
                                }
                                showCreateProjDialog = false
                                projName = ""
                                projDesc = ""
                            }
                        }
                    ) {
                        Text(if (isFarsiMode) "ایجاد" else "Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateProjDialog = false }) {
                        Text(if (isFarsiMode) "لغو" else "Cancel")
                    }
                }
            )
        }
    } else {
        GeotechnicalWorkspace(
            viewModel = viewModel,
            projectId = activeProject.id,
            projectName = activeProject.name,
            onBack = { localSelectedProjectId = null },
            onNavigateToTab = onNavigateToTab,
            modifier = modifier
        )
    }
}

@Composable
fun GeotechnicalWorkspace(
    viewModel: GeologyViewModel,
    projectId: Int,
    projectName: String,
    onBack: () -> Unit,
    onNavigateToTab: ((com.example.MainTab) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val evaluationsFlow = remember(projectId) { viewModel.getRockMechanicsEvaluationsForProject(projectId) }
    val evaluations by evaluationsFlow.collectAsState(initial = emptyList())
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    
    // Toggle state to switch between Simple and Advanced Geotechnical Views
    var isSimpleMode by remember { mutableStateOf(true) }
    
    // Dynamic tab filter mapping to prevent index out of bounds while decluttering
    val activeTabsList = remember(isSimpleMode, isFarsiMode) {
        if (isSimpleMode) {
            if (isFarsiMode) {
                listOf(
                    Triple(0, "لاگ‌های صحرایی", Icons.Default.Book),
                    Triple(1, "رده‌بندی RMR", Icons.Default.Star),
                    Triple(4, "سیستم Q بارتون", Icons.Default.Build)
                )
            } else {
                listOf(
                    Triple(0, "Log Journal", Icons.Default.Book),
                    Triple(1, "RMR Rating", Icons.Default.Star),
                    Triple(4, "Barton Q-System", Icons.Default.Build)
                )
            }
        } else {
            if (isFarsiMode) {
                listOf(
                    Triple(0, "دفترچه لاگ", Icons.Default.Book),
                    Triple(1, "رده‌بندی RMR", Icons.Default.Star),
                    Triple(2, "تحلیل هوک-براون", Icons.Default.TrendingUp),
                    Triple(3, "رده‌بندی MRMR & RMi", Icons.Default.Layers),
                    Triple(4, "سیستم Q بارتون", Icons.Default.Build),
                    Triple(5, "استریونت سینماتیک", Icons.Default.Info),
                    Triple(6, "پشتیبانی داینامیک", Icons.Default.Shield)
                )
            } else {
                listOf(
                    Triple(0, "Log Journal", Icons.Default.Book),
                    Triple(1, "Bieniawski RMR", Icons.Default.Star),
                    Triple(2, "Hoek-Brown", Icons.Default.TrendingUp),
                    Triple(3, "MRMR & RMi", Icons.Default.Layers),
                    Triple(4, "Barton Q-System", Icons.Default.Build),
                    Triple(5, "Kinematic Stereonet", Icons.Default.Info),
                    Triple(6, "Dynamic Support", Icons.Default.Shield)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(projectName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (isFarsiMode) "میز کار و داشبورد ارزیابی ژئوتکنیکی" else "Geotechnical Evaluator HUD", 
                    fontSize = 11.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (isFarsiMode) "بازگشت" else "Back", fontWeight = FontWeight.Bold)
            }
        }

        // Modern Bilingual Mode Switcher Card to de-clutter the geotechnical suite
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFarsiMode) {
                            if (isSimpleMode) "نمای ساده ژئوتکنیک فعال است" else "نمای کامل تخصصی مکانیک سنگ فعال است"
                        } else {
                            if (isSimpleMode) "Simple Geotechnical View active" else "Advanced Rock Mechanics Suite active"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = if (isFarsiMode) {
                            if (isSimpleMode) "فقط نمایش دو تحلیل اصلی RMR و Q جهت سهولت در فیلد" else "دسترسی به تمام ابزارها مثل تحلیل شکست هوک-براون، پایداری استریونت و سیستم نگهداری"
                        } else {
                            if (isSimpleMode) "Showing primary RMR & Q tools to prevent clutter" else "Full access to Hoek-Brown mass failure, Stereonets & dynamic support"
                        },
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        lineHeight = 12.sp
                    )
                }

                Button(
                    onClick = {
                        isSimpleMode = !isSimpleMode
                        // Auto snap active selection to prevent view loss
                        if (isSimpleMode && selectedTab != 0 && selectedTab != 1 && selectedTab != 4) {
                            selectedTab = 0
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSimpleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSimpleMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (isFarsiMode) {
                            if (isSimpleMode) "نمای پیشرفته" else "ساده‌سازی"
                        } else {
                            if (isSimpleMode) "Advanced Mode" else "Simplify"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Module selectors with dynamic active index
        ScrollableTabRow(
            selectedTabIndex = activeTabsList.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0),
            edgePadding = 0.dp
        ) {
            activeTabsList.forEach { (tabIndex, label, icon) ->
                Tab(
                    selected = selectedTab == tabIndex,
                    onClick = { selectedTab = tabIndex },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp))
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> LogJournalPanel(viewModel, projectId, evaluations)
            1 -> RmrPanel(viewModel, projectId, onNavigateToTab)
            2 -> HoekBrownPanel(viewModel, projectId)
            3 -> MrmrRmiPanel(viewModel, projectId)
            4 -> QSystemPanel(viewModel, projectId)
            5 -> KinematicStereonetPanel(viewModel, projectId, onNavigateToTab)
            6 -> DynamicSupportPanel(viewModel, projectId)
        }
    }
}

@Composable
fun LogJournalPanel(
    viewModel: GeologyViewModel,
    projectId: Int,
    evaluations: List<RockMechanicsEvaluation>
) {
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()

    if (evaluations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Book, contentDescription = null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                Text(
                    text = if (isFarsiMode) 
                        "هیچ لاگ ژئوتکنیکی برای این پروژه ثبت نشده است. از تب‌های محاسباتی برای ثبت ارزیابی‌ها استفاده کنید." 
                    else 
                        "No geotechnical logs submitted for this project. Use solver calculators next door to compile.", 
                    fontSize = 12.5.sp, 
                    color = Color.Gray, 
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(evaluations) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isFarsiMode) {
                                        "لاگ ${log.evaluationType}: ${log.locationName}"
                                    } else {
                                        "${log.evaluationType} Log: ${log.locationName}"
                                    }, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isFarsiMode) "تاریخ ثبت: ${log.evaluationDate}" else "Recorded: ${log.evaluationDate}", 
                                    fontSize = 10.sp, 
                                    color = Color.Gray
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deleteRockMechanicsEvaluation(log.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete log", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        when (log.evaluationType) {
                            "RMR" -> {
                                val translatedClass = if (isFarsiMode) {
                                    when {
                                        log.rmrClass.contains("Class I", ignoreCase = true) -> "بسیار خوب (Class I)"
                                        log.rmrClass.contains("Class II", ignoreCase = true) -> "خوب (Class II)"
                                        log.rmrClass.contains("Class III", ignoreCase = true) -> "متوسط (Class III)"
                                        log.rmrClass.contains("Class IV", ignoreCase = true) -> "ضعیف (Class IV)"
                                        else -> "بسیار ضعیف (Class V)"
                                    }
                                } else log.rmrClass

                                Text(
                                    text = if (isFarsiMode) "امتیاز پایه RMR: ${log.rmrScore.toInt()} ($translatedClass)" else "RMR Basic Score: ${log.rmrScore.toInt()} (${log.rmrClass})", 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.primary, 
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isFarsiMode)
                                        "مقاومت سنگ بکر (UCS): ${log.rmrUcsVal} | کیفیت سنگ (RQD): ${log.rmrRqdVal} | فاصله درزه‌ها: ${log.rmrSpacingVal} | آب زیرزمینی: ${log.rmrWaterVal} | تصحیح جهت‌داری: ${log.rmrAdjustmentVal}"
                                    else
                                        "UCS Rating: ${log.rmrUcsVal} | RQD: ${log.rmrRqdVal} | Spacing: ${log.rmrSpacingVal} | Water: ${log.rmrWaterVal} | Adj: ${log.rmrAdjustmentVal}", 
                                    fontSize = 11.sp, 
                                    color = Color.Gray,
                                    lineHeight = 14.sp
                                )
                            }
                            "Hoek-Brown" -> {
                                Text(
                                    text = if (isFarsiMode) 
                                        "مقاومت سنگ بکر (σci): ${log.hbSigci} مگاپاسکال | شاخص GSI سنگ: ${log.hbGsi}" 
                                    else 
                                        "Intact UCS (σci): ${log.hbSigci} MPa | Rock GSI: ${log.hbGsi}", 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.secondary, 
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "mb: ${String.format(Locale.US, "%.3f", log.hbMb)} | s: ${String.format(Locale.US, "%.5f", log.hbS)} | " + 
                                        (if (isFarsiMode) "مقاومت توده: ${String.format(Locale.US, "%.2f", log.hbSigmaci)} مگاپاسکال" else "Mass UCS: ${String.format(Locale.US, "%.2f", log.hbSigmaci)} MPa"), 
                                    fontSize = 11.sp, 
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.DarkGray
                                )
                            }
                            "MRMR" -> {
                                val translatedClass = if (isFarsiMode) {
                                    when {
                                        log.mrmrClass.contains("Class I", ignoreCase = true) -> "بسیار خوب (Class I)"
                                        log.mrmrClass.contains("Class II", ignoreCase = true) -> "خوب (Class II)"
                                        log.mrmrClass.contains("Class III", ignoreCase = true) -> "متوسط (Class III)"
                                        log.mrmrClass.contains("Class IV", ignoreCase = true) -> "ضعیف (Class IV)"
                                        else -> "بسیار ضعیف (Class V)"
                                    }
                                } else log.mrmrClass

                                Text(
                                    text = if (isFarsiMode) "امتیاز MRMR معدنی: ${String.format(Locale.US, "%.1f", log.mrmrScore)} ($translatedClass)" else "MRMR Score: ${String.format(Locale.US, "%.1f", log.mrmrScore)} (${log.mrmrClass})", 
                                    fontWeight = FontWeight.Bold, 
                                    color = Color(0xFFE65100), 
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isFarsiMode)
                                        "تصحیح هوازدگی: ${log.mrmrWeatheringAdj} | تصحیح حفاری/انفجار: ${log.mrmrDrillingAdj} | تصحیح جهت‌داری درزه‌ها: ${log.mrmrJointOrientationAdj}"
                                    else
                                        "Weathering Adj: ${log.mrmrWeatheringAdj} | Blast Adj: ${log.mrmrDrillingAdj} | Joint: ${log.mrmrJointOrientationAdj}", 
                                    fontSize = 11.sp, 
                                    color = Color.Gray,
                                    lineHeight = 14.sp
                                )
                                if (log.rmiValue > 0) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (isFarsiMode)
                                            "شاخص توده سنگ RMi: ${String.format(Locale.US, "%.4f", log.rmiValue)} (مقاومت بکر: ${log.rmiUcs} مگاپاسکال، Jp: ${log.rmiJp})"
                                        else
                                            "RMi Index: ${String.format(Locale.US, "%.4f", log.rmiValue)} (Intact UCS: ${log.rmiUcs} MPa, Jp: ${log.rmiJp})", 
                                        fontWeight = FontWeight.Bold, 
                                        color = Color(0xFF00ACC1), 
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                            "RMi" -> {
                                Text(
                                    text = if (isFarsiMode) "شاخص توده سنگ RMi: ${String.format(Locale.US, "%.4f", log.rmiValue)}" else "RMi Value: ${String.format(Locale.US, "%.4f", log.rmiValue)}", 
                                    fontWeight = FontWeight.Bold, 
                                    color = Color(0xFF00ACC1), 
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isFarsiMode)
                                        "پارامتر درزه (Jp): ${log.rmiJp} | مقاومت بکر (UCS): ${log.rmiUcs} مگاپاسکال"
                                    else
                                        "Joint parameter (Jp): ${log.rmiJp} | Intact UCS: ${log.rmiUcs} MPa", 
                                    fontSize = 11.sp, 
                                    color = Color.Gray
                                )
                            }
                            "Q-System" -> {
                                Text(
                                    text = if (isFarsiMode) "شاخص Q بارتون: ${String.format(Locale.US, "%.4f", log.rmiValue)}" else "Barton's Q-Value: ${String.format(Locale.US, "%.4f", log.rmiValue)}", 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.primary, 
                                    fontSize = 13.sp
                                )
                                val qText = when {
                                    log.rmiValue < 0.01 -> if (isFarsiMode) "فوق‌العاده ضعیف (Exceptionally poor)" else "Exceptionally poor"
                                    log.rmiValue < 0.1 -> if (isFarsiMode) "بسیار بسیار ضعیف (Extremely poor)" else "Extremely poor"
                                    log.rmiValue < 1.0 -> if (isFarsiMode) "بسیار ضعیف (Very poor)" else "Very poor"
                                    log.rmiValue < 4.0 -> if (isFarsiMode) "ضعیف (Poor)" else "Poor"
                                    log.rmiValue < 10.0 -> if (isFarsiMode) "متوسط (Fair)" else "Fair"
                                    log.rmiValue < 40.0 -> if (isFarsiMode) "خوب (Good)" else "Good"
                                    log.rmiValue < 100.0 -> if (isFarsiMode) "بسیار خوب (Very good)" else "Very good"
                                    log.rmiValue < 400.0 -> if (isFarsiMode) "بسیار بسیار عالی (Extremely good)" else "Extremely good"
                                    else -> if (isFarsiMode) "فوق‌العاده عالی (Exceptionally good)" else "Exceptionally good"
                                }
                                Text(
                                    text = if (isFarsiMode) "رده‌بندی: $qText | معادل RQD: ${log.rmiUcs.toInt()}%" else "Classification: $qText | RQD equivalent: ${log.rmiUcs.toInt()}%", 
                                    fontSize = 11.sp, 
                                    color = Color.Gray
                                )
                            }
                        }
                        if (log.notes.isNotBlank()) {
                            Text(
                                text = if (isFarsiMode) "یادداشت‌های صحرایی: ${log.notes}" else "Field Comments: ${log.notes}", 
                                fontSize = 11.sp, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RmrPanel(
    viewModel: GeologyViewModel, 
    projectId: Int,
    onNavigateToTab: ((com.example.MainTab) -> Unit)? = null
) {
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()

    var locName by remember { mutableStateOf(if (isFarsiMode) "سینه کار گالری ۴" else "Adit 04 Face") }
    var locError by remember { mutableStateOf(false) }
    var rmrUcs by remember { mutableStateOf("7.0") } // Score of Intact UCS (0-15)
    var rmrRqd by remember { mutableStateOf("13.0") } // Score of RQD (3-20)
    var rmrSpacing by remember { mutableStateOf("10.0") } // Score of spacing (5-20)
    var rmrCondition by remember { mutableStateOf("20.0") } // Score of conditions (0-30)
    var rmrWater by remember { mutableStateOf("10.0") } // Score of groundwater (0-15)
    var rmrAdjustment by remember { mutableStateOf("-5.0") } // Discontinuity adjustment (-12-0)
    var notes by remember { mutableStateOf(if (isFarsiMode) "درزه‌داری متوسط در سنگ شیست لایه‌ای. سطوح صیقلی." else "Moderate phyllite jointing. Slicken-sided surface.") }

    var rmrSpan by remember { mutableStateOf(5.0f) }

    var finalScore by remember { mutableStateOf(55.0) }
    var rockClass by remember { mutableStateOf("Class III: Fair Rock") }

    // Parsing & validation states
    val ucsVal = rmrUcs.toDoubleOrNull() ?: 0.0
    val isUcsError = rmrUcs.isNotBlank() && (ucsVal < 0 || ucsVal > 15)

    val rqdVal = rmrRqd.toDoubleOrNull() ?: 0.0
    val isRqdError = rmrRqd.isNotBlank() && (rqdVal < 3 || rqdVal > 20)

    val spacingVal = rmrSpacing.toDoubleOrNull() ?: 0.0
    val isSpacingError = rmrSpacing.isNotBlank() && (spacingVal < 5 || spacingVal > 20)

    val condVal = rmrCondition.toDoubleOrNull() ?: 0.0
    val isCondError = rmrCondition.isNotBlank() && (condVal < 0 || condVal > 30)

    val waterVal = rmrWater.toDoubleOrNull() ?: 0.0
    val isWaterError = rmrWater.isNotBlank() && (waterVal < 0 || waterVal > 15)

    val adjVal = rmrAdjustment.toDoubleOrNull() ?: 0.0
    val isAdjError = rmrAdjustment.isNotBlank() && (adjVal < -12 || adjVal > 0)

    fun calculateRmr() {
        val score = (ucsVal + rqdVal + spacingVal + condVal + waterVal + adjVal).coerceIn(0.0, 100.0)
        finalScore = score

        rockClass = if (isFarsiMode) {
            when {
                score >= 81 -> "کلاس I: سنگ بسیار خوب (Very Good)"
                score >= 61 -> "کلاس II: سنگ خوب (Good)"
                score >= 41 -> "کلاس III: سنگ متوسط (Fair)"
                score >= 21 -> "کلاس IV: سنگ ضعیف (Poor)"
                else -> "کلاس V: سنگ بسیار ضعیف (Very Poor)"
            }
        } else {
            when {
                score >= 81 -> "Class I: Very Good Rock"
                score >= 61 -> "Class II: Good Rock"
                score >= 41 -> "Class III: Fair Rock"
                score >= 21 -> "Class IV: Poor Rock"
                else -> "Class V: Very Poor Rock"
            }
        }
    }

    LaunchedEffect(rmrUcs, rmrRqd, rmrSpacing, rmrCondition, rmrWater, rmrAdjustment, isFarsiMode) {
        calculateRmr()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            val cohesionKpa = when {
                finalScore >= 80 -> 400.0 + (finalScore - 80) * 5.0
                finalScore >= 60 -> 300.0 + (finalScore - 60) * 5.0
                finalScore >= 40 -> 200.0 + (finalScore - 40) * 5.0
                finalScore >= 20 -> 100.0 + (finalScore - 20) * 5.0
                else -> (finalScore / 20.0) * 100.0
            }.coerceAtLeast(10.0)

            val frictionDeg = when {
                finalScore >= 80 -> 45.0 + (finalScore - 80) * 0.25
                finalScore >= 60 -> 35.0 + (finalScore - 60) * 0.5
                finalScore >= 40 -> 25.0 + (finalScore - 40) * 0.5
                finalScore >= 20 -> 15.0 + (finalScore - 20) * 0.5
                else -> (finalScore / 20.0) * 15.0
            }.coerceAtLeast(5.0)

            val standUpHours = Math.pow(10.0, (finalScore - 11.5 * rmrSpan + 10.0) / 13.0)
            val standUpText = if (isFarsiMode) {
                when {
                    standUpHours > 24 * 365 -> "${String.format(Locale.US, "%.1f", standUpHours / (24 * 365))} سال"
                    standUpHours > 24 * 30 -> "${String.format(Locale.US, "%.1f", standUpHours / (24 * 30))} ماه"
                    standUpHours > 24 * 7 -> "${String.format(Locale.US, "%.1f", standUpHours / (24 * 7))} هفته"
                    standUpHours > 24 -> "${String.format(Locale.US, "%.1f", standUpHours / 24)} روز"
                    standUpHours > 1.0 -> "${String.format(Locale.US, "%.1f", standUpHours)} ساعت"
                    else -> "${String.format(Locale.US, "%.0f", (standUpHours * 60).coerceAtLeast(1.0))} دقیقه"
                }
            } else {
                when {
                    standUpHours > 24 * 365 -> "${String.format(Locale.US, "%.1f", standUpHours / (24 * 365))} Years"
                    standUpHours > 24 * 30 -> "${String.format(Locale.US, "%.1f", standUpHours / (24 * 30))} Months"
                    standUpHours > 24 * 7 -> "${String.format(Locale.US, "%.1f", standUpHours / (24 * 7))} Weeks"
                    standUpHours > 24 -> "${String.format(Locale.US, "%.1f", standUpHours / 24)} Days"
                    standUpHours > 1.0 -> "${String.format(Locale.US, "%.1f", standUpHours)} Hours"
                    else -> "${String.format(Locale.US, "%.0f", (standUpHours * 60).coerceAtLeast(1.0))} Mins"
                }
            }

            val rockLoadKpa = ((100.0 - finalScore) / 100.0).coerceAtLeast(0.0) * 26.5 * rmrSpan

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isFarsiMode) "کارت امتیاز RMR بی نیاوسکی (استاندارد ۱۹۸۹)" else "Bieniawski RMR Score Card (1989 Std.)", 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = rockClass, 
                                fontWeight = FontWeight.ExtraBold, 
                                fontSize = 16.sp, 
                                color = when {
                                    finalScore >= 81 -> Color(0xFF2E7D32)
                                    finalScore >= 61 -> Color(0xFF00ACC1)
                                    finalScore >= 41 -> Color(0xFFE6A23C)
                                    finalScore >= 21 -> Color(0xFFEF6C00)
                                    else -> Color(0xFFC62828)
                                }
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text("${finalScore.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { (finalScore / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = when {
                                finalScore >= 81 -> Color(0xFF2E7D32)
                                finalScore >= 61 -> Color(0xFF00ACC1)
                                finalScore >= 41 -> Color(0xFFE6A23C)
                                finalScore >= 21 -> Color(0xFFEF6C00)
                                else -> Color(0xFFC62828)
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isFarsiMode) "۰ (بسیار ضعیف)" else "0 (Very Poor)", fontSize = 8.5.sp, color = Color.Gray)
                            Text(if (isFarsiMode) "متوسط" else "Fair", fontSize = 8.5.sp, color = Color.Gray)
                            Text(if (isFarsiMode) "۱۰۰ (بسیار خوب)" else "100 (Very Good)", fontSize = 8.5.sp, color = Color.Gray)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    Text(
                        text = if (isFarsiMode) "پارامترهای مقاومتی تخمینی توده سنگ:" else "Estimated Analytical Rock Mass Parameters:", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 11.sp, 
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Param grid row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.20f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isFarsiMode) "چسبندگی توده (C)" else "COHESION (C)", 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text("${cohesionKpa.toInt()} kPa", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    text = if (isFarsiMode) "میانگین مقاومت چسبندگی برآورد شده توده درزه‌دار." else "Calculated average cohesion limit of jointed block mass.", 
                                    fontSize = 8.5.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.20f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isFarsiMode) "زاویه اصطکاک (Φ)" else "FRICTION ANGLE (Φ)", 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text("${String.format(Locale.US, "%.1f", frictionDeg)}°", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    text = if (isFarsiMode) "حد مقاومت برشی اصطکاکی تخمینی برای گسیختگی." else "Estimated shear friction angle resistance limit.", 
                                    fontSize = 8.5.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }

                    // Excavation size input for Standup time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = if (isFarsiMode) "عرض دهانه باز سازه: ${String.format(Locale.US, "%.1f", rmrSpan)} متر" else "Structural Span / Width: ${String.format(Locale.US, "%.1f", rmrSpan)} m", 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = rmrSpan,
                                onValueChange = { rmrSpan = it },
                                valueRange = 1.0f..30.0f,
                                modifier = Modifier.height(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(0.7f)) {
                            Text(if (isFarsiMode) "دهانه تونل مبنا" else "Span Width Reference", fontSize = 8.sp, color = Color.Gray)
                            Text(if (isFarsiMode) "ابعاد گالری بین ۱ تا ۳۰ متر" else "1-30m tunnel/room bounds", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Param grid row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isFarsiMode) "زمان ایستایی" else "STAND-UP TIME", 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = standUpText, 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    color = if (standUpHours < 24) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isFarsiMode) "محدوده مجاز نگهداری لافر قبل از ریزش ناگهانی سقف." else "Lauffer standard exposure limit before ceiling failure.", 
                                    fontSize = 8.5.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isFarsiMode) "بار نهایی سقف (Pr)" else "ULTIMATE ROOF LOAD (Pr)", 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("${String.format(Locale.US, "%.1f", rockLoadKpa)} kPa", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    text = if (isFarsiMode) "فشار شعاعی معادل وارده به سیستم نگهداری تاج تونل." else "Equivalent crown pressure load sustained by support.", 
                                    fontSize = 8.5.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isFarsiMode) "شبیه‌سازی و پیش‌نمایش هندسه درزه‌ها:" else "Visual Rock Joints & Spacing Simulation Preview:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // RMR Joints drawing preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                color = when {
                                    finalScore >= 81 -> Color(0xFFE8F5E9)
                                    finalScore >= 61 -> Color(0xFFE0F7FA)
                                    finalScore >= 41 -> Color(0xFFFFFDE7)
                                    finalScore >= 21 -> Color(0xFFFFE0B2)
                                    else -> Color(0xFFFFEBEE)
                                }
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            val w = size.width
                            val h = size.height
                            
                            // Let's draw joints. Spacing score spacingVal (5 - 20) controls how many joints we draw!
                            val spacingFactor = spacingVal.coerceIn(5.0, 20.0)
                            // fewer score = smaller spacing = MORE joint lines
                            val numLines = (30.0 / spacingFactor * 4.0).toInt().coerceIn(3, 30)
                            val step = w / (numLines + 1)
                            
                            // Let's tilt based on orientation score (adjVal ranging from -12 to 0)
                            val tiltAngle = (adjVal * 4.0).toFloat().coerceIn(-48f, 0f)
                            
                            // Draw joint set 1
                            for (i in 1..numLines) {
                                val xOffset = i * step
                                drawLine(
                                    color = Color(0xFF4E342E).copy(alpha = 0.65f),
                                    start = androidx.compose.ui.geometry.Offset(xOffset - tiltAngle * 0.7f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(xOffset + tiltAngle * 0.7f, h),
                                    strokeWidth = (24.0 / spacingFactor).toFloat().coerceIn(1.5f, 6f)
                                )
                            }
                            
                            // Draw joint set 2 (conjugate joints)
                            val conjugateNum = (numLines / 2).coerceAtLeast(2)
                            val cStep = h / (conjugateNum + 1)
                            for (j in 1..conjugateNum) {
                                val yOffset = j * cStep
                                drawLine(
                                    color = Color(0xFF3E2723).copy(alpha = 0.35f),
                                    start = androidx.compose.ui.geometry.Offset(0f, yOffset),
                                    end = androidx.compose.ui.geometry.Offset(w, yOffset + tiltAngle * 0.3f),
                                    strokeWidth = 1.2f
                                )
                            }
                            
                            // Draw water inflow if water score is low (dripping water lines)
                            if (waterVal < 15.0) {
                                val drippingLevel = ((15.0 - waterVal) / 15.0).toFloat() // 0 to 1
                                val numDroplets = (12 * drippingLevel).toInt().coerceIn(0, 15)
                                val seed = java.util.Random(42)
                                for (k in 0 until numDroplets) {
                                    val dropX = seed.nextFloat() * w
                                    val dropYLen = (seed.nextFloat() * 18f + 5f) * drippingLevel * 2f
                                    val dropStartY = seed.nextFloat() * h * 0.5f
                                    drawLine(
                                        color = Color(0xFF1976D2).copy(alpha = 0.75f),
                                        start = androidx.compose.ui.geometry.Offset(dropX, dropStartY),
                                        end = androidx.compose.ui.geometry.Offset(dropX, dropStartY + dropYLen),
                                        strokeWidth = 2.0f
                                    )
                                }
                            }
                        }
                        
                        // Small label in drawing overlay
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isFarsiMode) {
                                    "تعداد درزه‌ها: ${(30.0 / spacingVal * 4.0).toInt().coerceIn(3, 30)} دسته | فاصله‌داری: ${spacingVal.toInt()}"
                                } else {
                                    "Joint set count: ${(30.0 / spacingVal * 4.0).toInt().coerceIn(3, 30)} sets | Spacing index: ${spacingVal.toInt()}"
                                },
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (waterVal < 15.0) {
                                Text(
                                    text = if (isFarsiMode) "آب زیرزمینی: مرطوب/تراوش فعال" else "Groundwater: Seeping/Water Flow", 
                                    color = Color(0xFF90CAF9), 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = if (isFarsiMode) "آب زیرزمینی: کاملاً خشک" else "Groundwater: Completely Dry", 
                                    color = Color.White, 
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = locName,
                onValueChange = { 
                    locName = it
                    locError = it.isBlank()
                },
                label = { Text(if (isFarsiMode) "نام ایستگاه / موقعیت" else "Location Name / Station") },
                isError = locError,
                supportingText = {
                    if (locError) {
                        Text(if (isFarsiMode) "وارد کردن نام ایستگاه الزامی است!" else "Location Name is required!", color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }

        item {
            var showImportDialog by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showImportDialog = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isFarsiMode) "بارگذاری داده‌ها از مغزه‌های حفاری" else "Import from Borehole Core Runs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )
            }

            if (showImportDialog) {
                BoreholeCoreRunImportDialog(
                    viewModel = viewModel,
                    projectId = projectId,
                    onDismiss = { showImportDialog = false },
                    onImport = { bhCode, run ->
                        locName = "BH-$bhCode Run-${run.runNumber}"
                        locError = false
                        
                        val ucs = run.ucsMpa
                        val ucsScore = when {
                            ucs >= 250.0 -> 15.0
                            ucs >= 100.0 -> 12.0
                            ucs >= 50.0 -> 7.0
                            ucs >= 25.0 -> 4.0
                            ucs >= 5.0 -> 2.0
                            ucs >= 1.0 -> 1.0
                            else -> 0.0
                        }
                        rmrUcs = ucsScore.toString()

                        val rqd = run.rqdPercent
                        val rqdScore = when {
                            rqd >= 90.0 -> 20.0
                            rqd >= 75.0 -> 17.0
                            rqd >= 50.0 -> 13.0
                            rqd >= 25.0 -> 8.0
                            else -> 3.0
                        }
                        rmrRqd = rqdScore.toString()

                        val importNotes = if (isFarsiMode) {
                            "بارگذاری شده از گمانه $bhCode ران ${run.runNumber} (لیتولوژی: ${run.lithology})."
                        } else {
                            "Loaded from Borehole $bhCode Core Run #${run.runNumber}. Lithology: ${run.lithology}."
                        }
                        notes = importNotes + "\n" + notes
                        showImportDialog = false
                    }
                )
            }
        }

        item {
            Text(
                text = if (isFarsiMode) "امتیازدهی متغیرهای ژئوتکنیکی RMR:" else "Bieniawski RMR Geotechnical Variables (Input Scores)", 
                fontWeight = FontWeight.Bold, 
                fontSize = 12.sp, 
                color = MaterialTheme.colorScheme.secondary
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rmrUcs,
                    onValueChange = { rmrUcs = it },
                    label = { Text(if (isFarsiMode) "۱. مقاومت سنگ بکر (0-15)" else "1. UCS Strength (0-15)", fontSize = 8.sp) },
                    isError = isUcsError,
                    supportingText = {
                        if (isUcsError) Text("Range: 0-15", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = rmrRqd,
                    onValueChange = { rmrRqd = it },
                    label = { Text(if (isFarsiMode) "۲. شاخص RQD سنگ (3-20)" else "2. RQD Score (3-20)", fontSize = 8.sp) },
                    isError = isRqdError,
                    supportingText = {
                        if (isRqdError) Text("Range: 3-20", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rmrSpacing,
                    onValueChange = { rmrSpacing = it },
                    label = { Text(if (isFarsiMode) "۳. فاصله‌داری درزه‌ها (5-20)" else "3. Spacing (5-20)", fontSize = 8.sp) },
                    isError = isSpacingError,
                    supportingText = {
                        if (isSpacingError) Text("Range: 5-20", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = rmrCondition,
                    onValueChange = { rmrCondition = it },
                    label = { Text(if (isFarsiMode) "۴. شرایط درزه‌ها (0-30)" else "4. Condition (0-30)", fontSize = 8.sp) },
                    isError = isCondError,
                    supportingText = {
                        if (isCondError) Text("Range: 0-30", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rmrWater,
                    onValueChange = { rmrWater = it },
                    label = { Text(if (isFarsiMode) "۵. آب زیرزمینی (0-15)" else "5. Groundwater (0-15)", fontSize = 8.sp) },
                    isError = isWaterError,
                    supportingText = {
                        if (isWaterError) Text("Range: 0-15", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = rmrAdjustment,
                    onValueChange = { rmrAdjustment = it },
                    label = { Text(if (isFarsiMode) "۶. تصحیح جهت درزه‌ها (-12-0)" else "6. Orientation Adj (-12-0)", fontSize = 8.sp) },
                    isError = isAdjError,
                    supportingText = {
                        if (isAdjError) Text("Range: -12 to 0", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(if (isFarsiMode) "یادداشت‌های ارزیابی / شرایط ناپیوستگی‌ها" else "Evaluation Notes / Joint set descriptions") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }

        item {
            Button(
                onClick = {
                    if (locName.isBlank()) {
                        locError = true
                    } else if (!isUcsError && !isRqdError && !isSpacingError && !isCondError && !isWaterError && !isAdjError) {
                        locError = false
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        viewModel.saveRockMechanicsEvaluation(
                            RockMechanicsEvaluation(
                                projectId = projectId,
                                locationName = locName,
                                evaluationType = "RMR",
                                evaluationDate = sdf.format(Date()),
                                rmrUcsVal = ucsVal,
                                rmrRqdVal = rqdVal,
                                rmrSpacingVal = spacingVal,
                                rmrConditionVal = condVal,
                                rmrWaterVal = waterVal,
                                rmrAdjustmentVal = adjVal,
                                rmrScore = finalScore,
                                rmrClass = rockClass,
                                notes = notes
                            )
                        )
                        notes = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = locName.isNotBlank() && !isUcsError && !isRqdError && !isSpacingError && !isCondError && !isWaterError && !isAdjError,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isFarsiMode) "ذخیره لاگ RMR در دفترچه گزارش" else "Add RMR Log to Journal")
            }
        }

        if (onNavigateToTab != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTab.invoke(com.example.MainTab.Stereonet) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.secondary, 
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFarsiMode) 
                                    "تحلیل پایداری ساختاری (Stereonet)" 
                                else 
                                    "Structural Stereonet Plotting",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = if (isFarsiMode)
                                    "آنالیز جهت‌داری ناپیوستگی‌ها جهت شناسایی گسیختگی‌های مستوی، واژگونی و گوه در استریونت"
                                else
                                    "Perform kinematic analyses for planar, toppling, and wedge failures on joint orientations.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForward, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.secondary, 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HoekBrownPanel(viewModel: GeologyViewModel, projectId: Int) {
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()

    var locName by remember { mutableStateOf(if (isFarsiMode) "دامنه سینه کار A" else "Face Slope A") }
    var locError by remember { mutableStateOf(false) }
    var hbSigci by remember { mutableStateOf("80.0") } // Uniaxial compressive strength of intact rock
    var hbGsi by remember { mutableStateOf("60.0") } // GSI
    var hbMi by remember { mutableStateOf("15.0") } // mi parameter
    var hbD by remember { mutableStateOf("0.2") } // Disturbance factor
    var notes by remember { mutableStateOf(if (isFarsiMode) "توده سنگ گرانیتی با مقاومت متوسط. انفجار با آشفتگی جزئی." else "Medium strength granite slab. Slightly disturbed blast.") }

    var computedMb by remember { mutableStateOf(0.0) }
    var computedS by remember { mutableStateOf(0.0) }
    var computedA by remember { mutableStateOf(0.5) }
    var computedSigmaci by remember { mutableStateOf(0.0) } // Mass Compressive Strength
    var computedTensile by remember { mutableStateOf(0.0) } // Rock Mass Tensile Strength
    var computedErm by remember { mutableStateOf(0.0) } // Rock Mass deformation modulus (Erm)
    var computedSgcmGlobal by remember { mutableStateOf(0.0) } // Global Rock Mass Strength (sigcm)

    // Parsing & validation states
    val sigciVal = hbSigci.toDoubleOrNull() ?: 0.0
    val isSigciError = hbSigci.isNotBlank() && sigciVal <= 0

    val gsiVal = hbGsi.toDoubleOrNull() ?: 0.0
    val isGsiError = hbGsi.isNotBlank() && (gsiVal < 0 || gsiVal > 100)

    val miVal = hbMi.toDoubleOrNull() ?: 0.0
    val isMiError = hbMi.isNotBlank() && miVal <= 0

    val dVal = hbD.toDoubleOrNull() ?: 0.0
    val isDError = hbD.isNotBlank() && (dVal < 0 || dVal > 1.0)

    fun computeHoekBrown() {
        if (!isSigciError && !isGsiError && !isMiError && !isDError) {
            val safeD = dVal.coerceIn(0.0, 0.999) // avoid division term 0
            val divisionTermGb = 28 - 14 * safeD
            val mb = miVal * Math.exp((gsiVal - 100) / if (divisionTermGb != 0.0) divisionTermGb else 1.0)
            val s = Math.exp((gsiVal - 100) / (9 - 3 * safeD))
            val a = 0.5 + (1.0 / 6.0) * (Math.exp(-gsiVal / 15.0) - Math.exp(-20.0 / 3.0))
            val massStrength = sigciVal * Math.pow(s, a)

            computedMb = mb
            computedS = s
            computedA = a
            computedSigmaci = massStrength
            computedTensile = if (mb > 0.0) (s * sigciVal / mb) else 0.0
            
            // Erm deformation modulus = 100 * (0.02 + (1 - D/2) / (1 + exp((60 + 15D - GSI)/11))) GPa
            computedErm = 100.0 * (0.02 + (1.0 - safeD / 2.0) / (1.0 + Math.exp((60.0 + 15.0 * safeD - gsiVal) / 11.0)))
            
            // Global Rock Mass Strength Sigcm = Sigci * ( (mb + 4*s - a*(mb - 8*s)) * (mb/4 + s)^(a-1) ) / ( 2*(1+a)*(2+a) )
            val term1 = mb + 4.0 * s - a * (mb - 8.0 * s)
            val term2 = Math.pow((mb / 4.0 + s).coerceAtLeast(0.0001), a - 1.0)
            val denominator = 2.0 * (1.0 + a) * (2.0 + a)
            computedSgcmGlobal = if (denominator > 0.0) (sigciVal * (term1 * term2) / denominator) else 0.0
        }
    }

    LaunchedEffect(hbSigci, hbGsi, hbMi, hbD, isFarsiMode) {
        computeHoekBrown()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isFarsiMode) "خروجی‌های ژئوتکنیکی توده سنگ هوک-براون" else "Generalized Hoek-Brown Geotechnical Outputs", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 11.sp, 
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    // Parameters grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isFarsiMode) "ضریب توده (mb):" else "mb (mass multiplier):", fontSize = 9.sp, color = Color.Gray)
                            Text(String.format(Locale.US, "%.4f", computedMb), fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isFarsiMode) "پارامتر پایداری (s):" else "s (joint parameter):", fontSize = 9.sp, color = Color.Gray)
                            Text(String.format(Locale.US, "%.6f", computedS), fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isFarsiMode) "ثابت منحنی شکست (a):" else "a (slope coefficient):", fontSize = 9.sp, color = Color.Gray)
                            Text(String.format(Locale.US, "%.4f", computedA), fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Main Rock Strengths Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (isFarsiMode) "مقاومت فشاری توده (σci)" else "MASS COMPRESSIVE (σci)", 
                                    fontSize = 8.sp, 
                                    color = Color.Gray, 
                                    fontWeight = FontWeight.Bold
                                )
                                Text("${String.format(Locale.US, "%.2f", computedSigmaci)} MPa", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = if (isFarsiMode) "مقاومت فشاری نهایی توده درزه‌دار." else "Uniaxial compressive strength of jointed mass.", 
                                    fontSize = 8.sp, 
                                    color = Color.DarkGray, 
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (isFarsiMode) "مقاومت کلی توده (σcm)" else "GLOBAL STRENGTH (σcm)", 
                                    fontSize = 8.sp, 
                                    color = Color.Gray, 
                                    fontWeight = FontWeight.Bold
                                )
                                Text("${String.format(Locale.US, "%.2f", computedSgcmGlobal)} MPa", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = if (isFarsiMode) "مقاومت توده سنگ معادل با فرمول هوک ۲۰۰۲." else "Equivalent rock mass strength (Hoek 2002).", 
                                    fontSize = 8.sp, 
                                    color = Color.DarkGray, 
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (isFarsiMode) "مقاومت کششی توده سنگ" else "MASS TENSILE STRENGTH", 
                                    fontSize = 8.sp, 
                                    color = Color.Gray, 
                                    fontWeight = FontWeight.Bold
                                )
                                Text("${String.format(Locale.US, "%.3f", computedTensile)} MPa", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828))
                                Text(
                                    text = if (isFarsiMode) "تخمین مرز گسیختگی کششی منفی." else "Tensile failure boundary estimation.", 
                                    fontSize = 8.sp, 
                                    color = Color.DarkGray, 
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (isFarsiMode) "مدول الاستیسیته (Erm)" else "DEFORMATION MODULUS (Erm)", 
                                    fontSize = 8.sp, 
                                    color = Color.Gray, 
                                    fontWeight = FontWeight.Bold
                                )
                                Text("${String.format(Locale.US, "%.2f", computedErm)} GPa", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                                Text(
                                    text = if (isFarsiMode) "شاخص مدول دگرشکلی دیریش ۲۰۰۶." else "Diederichs 2006 mass elasticity index.", 
                                    fontSize = 8.sp, 
                                    color = Color.DarkGray, 
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isFarsiMode) "نمودار پوش گسیختگی هوک-براون (تنش‌های اصلی σ₁ در برابر σ₃):" else "Failure Envelope Curve Plot (Principal Stresses σ₁ vs σ₃):", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 10.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // failure envelope canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFCFAF2))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
                            val w = size.width
                            val h = size.height

                            // Draw axes
                            drawLine(color = Color.DarkGray, start = androidx.compose.ui.geometry.Offset(0f, h), end = androidx.compose.ui.geometry.Offset(w, h), strokeWidth = 2.5f)
                            drawLine(color = Color.DarkGray, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(0f, h), strokeWidth = 2.5f)

                            // Let's compute envelope scale bounds
                            val xMax = Math.max(10.0, sigciVal / 2.0)
                            // yMax is sigma_1 value at xMax
                            val yMax = xMax + sigciVal * Math.pow((computedMb * xMax / sigciVal) + computedS, computedA)

                            // 1. Draw custom curve path
                            val curvePath = androidx.compose.ui.graphics.Path()
                            val stepsCount = 30
                            for (stepIdx in 0..stepsCount) {
                                val sig3Val = (stepIdx.toDouble() / stepsCount.toDouble()) * xMax
                                val sig1Val = sig3Val + sigciVal * Math.pow((computedMb * sig3Val / sigciVal) + computedS, computedA)

                                val xp = (sig3Val / xMax).toFloat() * w
                                val yp = h - (sig1Val / yMax).toFloat() * h
                                if (stepIdx == 0) {
                                    curvePath.moveTo(xp, yp)
                                } else {
                                    curvePath.lineTo(xp, yp)
                                }
                            }

                            // Fill shaded stable region under failure curve
                            val shadedPath = androidx.compose.ui.graphics.Path().apply {
                                addPath(curvePath)
                                lineTo(w, h)
                                lineTo(0f, h)
                                close()
                            }
                            drawPath(path = shadedPath, color = Color(0xFFE8F5E9).copy(alpha = 0.5f))

                            // Draw failure curve stroke
                            drawPath(
                                path = curvePath,
                                color = Color(0xFFD84315),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f)
                            )

                            // Draw Mass Compressive UCS marker dot at (sig3=0, sig1=computedSigmaci)
                            val ucsYVal = h - (computedSigmaci / yMax).toFloat() * h
                            drawCircle(
                                color = Color(0xFF1565C0),
                                radius = 6f,
                                center = androidx.compose.ui.geometry.Offset(0f, ucsYVal)
                              )

                            // Gridlines at 25%, 50%, 75%
                            for (tick in 1..3) {
                                val gridX = tick * 0.25f * w
                                val gridY = tick * 0.25f * h
                                drawLine(
                                    color = Color.LightGray,
                                    start = androidx.compose.ui.geometry.Offset(gridX, 0f),
                                    end = androidx.compose.ui.geometry.Offset(gridX, h),
                                    strokeWidth = 1f,
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                                )
                                drawLine(
                                    color = Color.LightGray,
                                    start = androidx.compose.ui.geometry.Offset(0f, gridY),
                                    end = androidx.compose.ui.geometry.Offset(w, gridY),
                                    strokeWidth = 1f,
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                                )
                            }
                        }

                        // Grid tags labels
                        Text(
                            text = if (isFarsiMode) "شکست الاستوپلاستیک" else "Failed Plastic State",
                            color = Color(0xFFC62828).copy(alpha = 0.75f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                        )

                        Text(
                            text = if (isFarsiMode) "محدوده الاستیک امن" else "Sustained Elastic Zone",
                            color = Color(0xFF2E7D32).copy(alpha = 0.75f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                        )

                        Text(
                            text = "UCS (σcm) = ${String.format(Locale.US, "%.1f", computedSigmaci)} MPa",
                            color = Color(0xFF1565C0),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = 20.dp)
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = locName,
                onValueChange = { 
                    locName = it
                    locError = it.isBlank()
                },
                isError = locError,
                supportingText = {
                    if (locError) {
                        Text(if (isFarsiMode) "وارد کردن نام موقعیت الزامی است!" else "Location Name is required!", color = MaterialTheme.colorScheme.error)
                    }
                },
                label = { Text(if (isFarsiMode) "نام موقعیت / بخش سینه کار شیب" else "Location Name / Slope Sector") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }

        item {
            var showImportDialog by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showImportDialog = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isFarsiMode) "بارگذاری داده‌ها از مغزه‌های حفاری" else "Import from Borehole Core Runs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )
            }

            if (showImportDialog) {
                BoreholeCoreRunImportDialog(
                    viewModel = viewModel,
                    projectId = projectId,
                    onDismiss = { showImportDialog = false },
                    onImport = { bhCode, run ->
                        locName = "BH-$bhCode Run-${run.runNumber}"
                        locError = false
                        hbSigci = run.ucsMpa.toString()
                        
                        val estimatedGsi = (run.rqdPercent - 15.0).coerceIn(10.0, 100.0)
                        hbGsi = estimatedGsi.toInt().toString()
                        
                        val importNotes = if (isFarsiMode) {
                            "بارگذاری شده از گمانه $bhCode ران ${run.runNumber} (لیتولوژی: ${run.lithology}). مقاومت بکر ${run.ucsMpa} MPa."
                        } else {
                            "Loaded from Borehole $bhCode Core Run #${run.runNumber}. Lithology: ${run.lithology}. Intact UCS ${run.ucsMpa} MPa."
                        }
                        notes = importNotes + "\n" + notes
                        showImportDialog = false
                    }
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hbSigci,
                    onValueChange = { hbSigci = it },
                    label = { Text(if (isFarsiMode) "مقاومت سنگ بکر (σci) [MPa]" else "Intact UCS (σci) [MPa]", fontSize = 8.sp) },
                    isError = isSigciError,
                    supportingText = {
                        if (isSigciError) Text("Must be > 0", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = hbGsi,
                    onValueChange = { hbGsi = it },
                    label = { Text(if (isFarsiMode) "امتیاز GSI سنگ (0-100)" else "GSI rating (0-100)", fontSize = 8.sp) },
                    isError = isGsiError,
                    supportingText = {
                        if (isGsiError) Text("Range: 0-100", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hbMi,
                    onValueChange = { hbMi = it },
                    label = { Text(if (isFarsiMode) "ثابت جنس سنگ (mi)" else "Constant (mi) (e.g. 5-35)", fontSize = 8.sp) },
                    isError = isMiError,
                    supportingText = {
                        if (isMiError) Text("Must be > 0", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = hbD,
                    onValueChange = { hbD = it },
                    label = { Text(if (isFarsiMode) "ضریب آشفتگی D (0-1.0)" else "Disturbance D (0-1.0)", fontSize = 8.sp) },
                    isError = isDError,
                    supportingText = {
                        if (isDError) Text("Range: 0 to 1", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(if (isFarsiMode) "توضیحات و محاسبات حل‌کننده هوک-براون" else "Hoek-Brown Solver comments") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }

        item {
            Button(
                onClick = {
                    if (locName.isBlank()) {
                        locError = true
                    } else if (!isSigciError && !isGsiError && !isMiError && !isDError) {
                        locError = false
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        viewModel.saveRockMechanicsEvaluation(
                            RockMechanicsEvaluation(
                                projectId = projectId,
                                locationName = locName,
                                evaluationType = "Hoek-Brown",
                                evaluationDate = sdf.format(Date()),
                                hbSigci = sigciVal,
                                hbGsi = gsiVal,
                                hbMi = miVal,
                                hbD = dVal,
                                hbMb = computedMb,
                                hbS = computedS,
                                hbA = computedA,
                                hbSigmaci = computedSigmaci,
                                notes = notes
                            )
                        )
                        notes = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = locName.isNotBlank() && !isSigciError && !isGsiError && !isMiError && !isDError,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isFarsiMode) "ذخیره پارامترهای هوک-براون در لاگ" else "Lock Hoek-Brown Parameters")
            }
        }
    }
}

@Composable
fun MrmrRmiPanel(viewModel: GeologyViewModel, projectId: Int) {
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()

    var locName by remember { mutableStateOf(if (isFarsiMode) "تاج کارگاه ۱۰۲" else "Stope 102 Crown") }
    var locError by remember { mutableStateOf(false) }
    var rmrBasic by remember { mutableStateOf("60.0") }
    var weatheringAdj by remember { mutableStateOf("0.9") } // factor (0.5 to 1.0)
    var jointOrientationAdj by remember { mutableStateOf("0.85") } // factor (0.7 to 1.0)
    var blastAdj by remember { mutableStateOf("0.95") } // factor
    
    var rmiUcs by remember { mutableStateOf("70.0") }
    var rmiJp by remember { mutableStateOf("0.04") } // Joint Factor

    var mrmrScore by remember { mutableStateOf(48.45) }
    var mrmrClass by remember { mutableStateOf("Class III: Fair Rock") }
    var rmiValue by remember { mutableStateOf(2.8) }

    // Parsing & validation states
    val basicVal = rmrBasic.toDoubleOrNull() ?: 60.0
    val isBasicError = rmrBasic.isNotBlank() && (basicVal < 0 || basicVal > 100)

    val weatherVal = weatheringAdj.toDoubleOrNull() ?: 1.0
    val isWeatherError = weatheringAdj.isNotBlank() && (weatherVal < 0.5 || weatherVal > 1.0)

    val jointVal = jointOrientationAdj.toDoubleOrNull() ?: 1.0
    val isJointError = jointOrientationAdj.isNotBlank() && (jointVal < 0.7 || jointVal > 1.0)

    val blastVal = blastAdj.toDoubleOrNull() ?: 1.0
    val isBlastError = blastAdj.isNotBlank() && (blastVal < 0.5 || blastVal > 1.0)

    val rmiUcsValVal = rmiUcs.toDoubleOrNull() ?: 70.0
    val isRmiUcsError = rmiUcs.isNotBlank() && rmiUcsValVal <= 0

    val rmiJpValVal = rmiJp.toDoubleOrNull() ?: 0.04
    val isRmiJpError = rmiJp.isNotBlank() && rmiJpValVal <= 0

    fun calculateMrmrAndRmi() {
        if (!isBasicError && !isWeatherError && !isJointError && !isBlastError) {
            val finalMrmr = basicVal * weatherVal * jointVal * blastVal
            mrmrScore = finalMrmr

            mrmrClass = when {
                finalMrmr >= 81 -> if (isFarsiMode) "رده معدنی ۱: فوق‌العاده پایدار" else "Mining I: Extremely Stable"
                finalMrmr >= 61 -> if (isFarsiMode) "رده معدنی ۲: زمین کاملاً پایدار و خوب" else "Mining II: Good Ground"
                finalMrmr >= 41 -> if (isFarsiMode) "رده معدنی ۳: پایدار همراه با راک‌بولت" else "Mining III: Stable with Bolting"
                finalMrmr >= 21 -> if (isFarsiMode) "رده معدنی ۴: زمین ناپایدار و مچالگی شدید" else "Mining IV: Heavy Squeezing"
                else -> if (isFarsiMode) "رده معدنی ۵: ریزش و جریان‌های شدید توده" else "Mining V: Extreme Flowing"
            }
        }

        if (!isRmiUcsError && !isRmiJpError) {
            val jpVal = rmiJpValVal.coerceAtLeast(0.0001)
            rmiValue = 0.04 * rmiUcsValVal * Math.pow(jpVal, 0.4)
        }
    }

    LaunchedEffect(rmrBasic, weatheringAdj, jointOrientationAdj, blastAdj, rmiUcs, rmiJp, isFarsiMode) {
        calculateMrmrAndRmi()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            val stableHR = (mrmrScore * 0.15).coerceIn(1.0, 20.0)
            val stableSpan = stableHR * 4.0
            
            val rmiBlockClass = when {
                rmiValue >= 10.0 -> if (isFarsiMode) "بلوک‌های توده‌ای فوق‌العاده قوی" else "Extremely Strong Massive Blocks"
                rmiValue >= 1.0 -> if (isFarsiMode) "توده سنگ بلوکی مستحکم و قوی" else "Strong / Blocky Rock Mass"
                rmiValue >= 0.1 -> if (isFarsiMode) "بلوک‌های درزه‌دار با شکستگی متوسط" else "Moderately Jointed / Very Blocky"
                rmiValue >= 0.01 -> if (isFarsiMode) "خردشده نسبی با درزه‌داری بالا" else "Partially Crushed / Heavily Jointed"
                else -> if (isFarsiMode) "توده به شدت خردشده شبیه خاک" else "Extremely Crushed / Soil-Like"
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isFarsiMode) "خروجی اصلاح‌شده MRMR (لابشر) و شاخص RMi" else "Modified MRMR (Laubscher) & RMi Core Outputs", 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.SemiBold, 
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = (if (isFarsiMode) "امتیاز پایداری معدنی: " else "MRMR Mining Rating: ") + String.format(Locale.US, "%.2f", mrmrScore), 
                                fontWeight = FontWeight.ExtraBold, 
                                fontSize = 16.sp, 
                                color = Color(0xFFE65100)
                            )
                            Text(mrmrClass, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (isFarsiMode) "شاخص RMi" else "RMi INDEX", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(String.format(Locale.US, "%.4f", rmiValue), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00ACC1))
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Engineering Calculations Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isFarsiMode) "شعاع هیدرولیکی پایدار (HR)" else "STABLE HYDRAULIC RADIUS (HR)", 
                                    fontSize = 8.sp, 
                                    color = Color.Gray, 
                                    fontWeight = FontWeight.Bold
                                )
                                Text("${String.format(Locale.US, "%.2f", stableHR)} m", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE65100))
                                Text(
                                    text = if (isFarsiMode) "حداکثر نسبت مساحت حفاری آزاد برای پایداری سقف کارگاه." else "Max undercut area ratio for stable roof.", 
                                    fontSize = 8.sp, 
                                    color = Color.DarkGray, 
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isFarsiMode) "حداکثر دهانه باز بدون نگهداری" else "MAX EXCAVATION SPAN", 
                                    fontSize = 8.sp, 
                                    color = Color.Gray, 
                                    fontWeight = FontWeight.Bold
                                )
                                Text("${String.format(Locale.US, "%.1f", stableSpan)} m", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = if (isFarsiMode) "دهانه پیشنهادی پایدار بدون سیستم‌های نگهداری راک‌بولت." else "Suggested stable unsupported stope diameter.", 
                                    fontSize = 8.sp, 
                                    color = Color.DarkGray, 
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isFarsiMode) "رده‌بندی ساختاری بلوک بر اساس PALMSTROM RMi:" else "PALMSTROM RMi BLOCK STRUCTURAL CLASSIFICATION:", 
                                    fontSize = 8.sp, 
                                    color = Color.Gray, 
                                    fontWeight = FontWeight.Bold
                                )
                                Text(rmiBlockClass, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00ACC1))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isFarsiMode) "شبیه‌سازی ابعاد بلوک درزه و قابلیت تخریب توده (Block Caving):" else "Block Caving Cavability & Joint Blocks Size Simulation:", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 10.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Caving / Block size canvas visualizer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF7F5F0))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            val w = size.width
                            val h = size.height

                            // Draw rock block mesh background based on rmiJpValVal (which determines jp / spacing of joints)
                            val blockSpacing = (rmiJpValVal * 1200.0).coerceIn(10.0, 70.0).toFloat()
                            
                            // Draw blocky mesh
                            val numXBlocks = (w / blockSpacing).toInt() + 2
                            val numYBlocks = (h / blockSpacing).toInt() + 2
                            for (ix in 0..numXBlocks) {
                                val xPos = ix * blockSpacing
                                drawLine(
                                    color = Color(0xFF8D6E63).copy(alpha = 0.22f),
                                    start = androidx.compose.ui.geometry.Offset(xPos, 0f),
                                    end = androidx.compose.ui.geometry.Offset(xPos, h),
                                    strokeWidth = 1f
                                )
                            }
                            for (iy in 0..numYBlocks) {
                                val yPos = iy * blockSpacing
                                drawLine(
                                    color = Color(0xFF8D6E63).copy(alpha = 0.22f),
                                    start = androidx.compose.ui.geometry.Offset(0f, yPos),
                                    end = androidx.compose.ui.geometry.Offset(w, yPos),
                                    strokeWidth = 1f
                                )
                            }

                            // Draw stope undercut at bottom center of size w * 0.4
                            val stopeW = w * 0.35f
                            val stopeH = h * 0.4f
                            val stopeLeft = (w - stopeW) / 2f
                            val stopeTop = h - stopeH

                            val stopePath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(stopeLeft, h)
                                lineTo(stopeLeft + stopeW * 0.1f, stopeTop)
                                lineTo(stopeLeft + stopeW * 0.9f, stopeTop)
                                lineTo(stopeLeft + stopeW, h)
                                close()
                            }

                            // Draw underground empty stope chamber
                            drawPath(path = stopePath, color = Color(0xFF263238))

                            // Draw block boundaries and cave propagation
                            if (mrmrScore < 50.0) {
                                // Low MRMR score means cave propagating! Show loose rock fragments falling and gravity load arrows
                                val collapsePercent = ((50.0 - mrmrScore) / 50.0).toFloat()
                                val numSpalls = (8 * collapsePercent).toInt().coerceAtLeast(3)
                                val seed = java.util.Random(99)
                                
                                for (spIdx in 0 until numSpalls) {
                                    val spw = (seed.nextFloat() * 14f + 8f)
                                    val sph = (seed.nextFloat() * 10f + 6f)
                                    val spx = stopeLeft + stopeW * 0.15f + seed.nextFloat() * (stopeW * 0.7f)
                                    val spy = stopeTop + seed.nextFloat() * (stopeH * 0.8f)

                                    // Draw loose falling block fragment
                                    drawRect(
                                        color = Color(0xFF5D4037).copy(alpha = 0.85f),
                                        topLeft = androidx.compose.ui.geometry.Offset(spx - spw / 2f, spy - sph / 2f),
                                        size = androidx.compose.ui.geometry.Size(spw, sph)
                                    )
                                    
                                    // Small down gravity arrows
                                    if (spIdx % 2 == 0) {
                                        drawLine(
                                            color = Color(0xFFFF7043),
                                            start = androidx.compose.ui.geometry.Offset(spx, spy - 6f),
                                            end = androidx.compose.ui.geometry.Offset(spx, spy + 6f),
                                            strokeWidth = 2f
                                        )
                                    }
                                }
                            } else {
                                // High MRMR is extremely stable! Draw steady anchor beams or bolts holding stope arch
                                drawLine(color = Color(0xFF4CAF50), start = androidx.compose.ui.geometry.Offset(stopeLeft + stopeW * 0.3f, stopeTop), end = androidx.compose.ui.geometry.Offset(stopeLeft + stopeW * 0.3f, stopeTop - 15f), strokeWidth = 2.5f)
                                drawLine(color = Color(0xFF4CAF50), start = androidx.compose.ui.geometry.Offset(stopeLeft + stopeW * 0.5f, stopeTop), end = androidx.compose.ui.geometry.Offset(stopeLeft + stopeW * 0.5f, stopeTop - 15f), strokeWidth = 2.5f)
                                drawLine(color = Color(0xFF4CAF50), start = androidx.compose.ui.geometry.Offset(stopeLeft + stopeW * 0.7f, stopeTop), end = androidx.compose.ui.geometry.Offset(stopeLeft + stopeW * 0.7f, stopeTop - 15f), strokeWidth = 2.5f)
                            }
                        }

                        // Labels
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = (if (isFarsiMode) "ضریب فاصله درزه: " else "Joint block parameter: ") + String.format(Locale.US, "%.3f", rmiJpValVal),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (mrmrScore < 50.0) {
                                Text(
                                    text = if (isFarsiMode) "سطح تحتانی: گسیختگی فعال تخریب ثقلی" else "Undercut Level: Active Gravity Cave Propagation", 
                                    color = Color(0xFFFFB74D), 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = if (isFarsiMode) "سطح تحتانی: سقف کارگاهی کاملاً پایدار" else "Undercut Level: Completely Stable", 
                                    color = Color(0xFF81C784), 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = locName,
                onValueChange = { 
                    locName = it
                    locError = it.isBlank()
                },
                isError = locError,
                supportingText = {
                    if (locError) {
                        Text(if (isFarsiMode) "وارد کردن نام موقعیت معدنی الزامی است!" else "Location Name is required!", color = MaterialTheme.colorScheme.error)
                    }
                },
                label = { Text(if (isFarsiMode) "بخش معدنکاری / موقعیت سینه کار کارگاه" else "Mining Segment / Face Location") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }

        item {
            var showImportDialog by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showImportDialog = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isFarsiMode) "بارگذاری داده‌ها از مغزه‌های حفاری" else "Import from Borehole Core Runs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )
            }

            if (showImportDialog) {
                BoreholeCoreRunImportDialog(
                    viewModel = viewModel,
                    projectId = projectId,
                    onDismiss = { showImportDialog = false },
                    onImport = { bhCode, run ->
                        locName = "BH-$bhCode Run-${run.runNumber}"
                        locError = false
                        rmiUcs = run.ucsMpa.toString()
                        
                        val ucs = run.ucsMpa
                        val ucsScore = when {
                            ucs >= 250.0 -> 15.0
                            ucs >= 100.0 -> 12.0
                            ucs >= 50.0 -> 7.0
                            ucs >= 25.0 -> 4.0
                            ucs >= 5.0 -> 2.0
                            ucs >= 1.0 -> 1.0
                            else -> 0.0
                        }
                        val rqd = run.rqdPercent
                        val rqdScore = when {
                            rqd >= 90.0 -> 20.0
                            rqd >= 75.0 -> 17.0
                            rqd >= 50.0 -> 13.0
                            rqd >= 25.0 -> 8.0
                            else -> 3.0
                        }
                        val rmrEst = (ucsScore + rqdScore + 25.0).coerceIn(10.0, 100.0)
                        rmrBasic = rmrEst.toString()
                        
                        showImportDialog = false
                    }
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rmrBasic,
                    onValueChange = { rmrBasic = it },
                    label = { Text(if (isFarsiMode) "امتیاز پایه RMR سنگ (0-100)" else "Basic RMR Score (0-100)", fontSize = 8.sp) },
                    isError = isBasicError,
                    supportingText = {
                        if (isBasicError) Text("Range: 1 to 100", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = weatheringAdj,
                    onValueChange = { weatheringAdj = it },
                    label = { Text(if (isFarsiMode) "ضریب هوازدگی (0.5-1.0)" else "Weathering Factor (0.5-1.0)", fontSize = 8.sp) },
                    isError = isWeatherError,
                    supportingText = {
                        if (isWeatherError) Text("Range: 0.5 to 1.0", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = jointOrientationAdj,
                    onValueChange = { jointOrientationAdj = it },
                    label = { Text(if (isFarsiMode) "اصلاح جهت درزه‌داری (0.7-1.0)" else "Structural Joint Adjust (0.7-1.0)", fontSize = 8.sp) },
                    isError = isJointError,
                    supportingText = {
                        if (isJointError) Text("Range: 0.7 to 1.0", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = blastAdj,
                    onValueChange = { blastAdj = it },
                    label = { Text(if (isFarsiMode) "اصلاح روش حفاری/انفجار" else "Explosive/Blast Adjust", fontSize = 8.sp) },
                    isError = isBlastError,
                    supportingText = {
                        if (isBlastError) Text("Range: 0.5 to 1.0", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rmiUcs,
                    onValueChange = { rmiUcs = it },
                    label = { Text(if (isFarsiMode) "مقاومت بکر RMi UCS [MPa]" else "RMi Intact UCS [MPa]", fontSize = 8.sp) },
                    isError = isRmiUcsError,
                    supportingText = {
                        if (isRmiUcsError) Text("Must be > 0", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = rmiJp,
                    onValueChange = { rmiJp = it },
                    label = { Text(if (isFarsiMode) "پارامتر فاصله درزه‌ها (Jp)" else "Joint parameter (Jp)", fontSize = 8.sp) },
                    isError = isRmiJpError,
                    supportingText = {
                        if (isRmiJpError) Text("Must be > 0", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        item {
            Button(
                onClick = {
                    if (locName.isBlank()) {
                        locError = true
                    } else if (!isBasicError && !isWeatherError && !isJointError && !isBlastError && !isRmiUcsError && !isRmiJpError) {
                        locError = false
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        viewModel.saveRockMechanicsEvaluation(
                            RockMechanicsEvaluation(
                                projectId = projectId,
                                locationName = locName,
                                evaluationType = "MRMR",
                                evaluationDate = sdf.format(Date()),
                                mrmrRmrBasic = basicVal,
                                mrmrWeatheringAdj = weatherVal,
                                mrmrJointOrientationAdj = jointVal,
                                mrmrDrillingAdj = blastVal,
                                mrmrScore = mrmrScore,
                                mrmrClass = mrmrClass,
                                rmiUcs = rmiUcsValVal,
                                rmiJp = rmiJpValVal,
                                rmiValue = rmiValue,
                                notes = if (isFarsiMode) "ارزیابی یکپارچه سینه کار معدن همراه با نمایه شاخص توده سنگ RMi." else "Unified Mine Block evaluation with RMi rock index indexation."
                            )
                        )
                        val successMsg = if (isFarsiMode) "محاسبات ژئوتکنیکی MRMR و RMi با موفقیت ثبت شد!" else "Unified MRMR & RMi evaluations saved successfully!"
                        Toast.makeText(viewModel.application, successMsg, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = locName.isNotBlank() && !isBasicError && !isWeatherError && !isJointError && !isBlastError && !isRmiUcsError && !isRmiJpError,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isFarsiMode) "ثبت محاسبات MRMR و RMi در سیستم" else "Commit MRMR & RMi Evaluations")
            }
        }
    }
}

@Composable
fun QSystemPanel(
    viewModel: GeologyViewModel,
    projectId: Int
) {
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()

    var locName by remember { mutableStateOf("") }
    var locError by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    // Barton's parameters selection
    var rqdStr by remember { mutableStateOf("75") }
    var jnSelected by remember { mutableStateOf(4.0) } // Default: Two joint sets (Jn = 4)
    var jrSelected by remember { mutableStateOf(1.5) } // Default: Rough/irregular planar (Jr = 1.5)
    var jaSelected by remember { mutableStateOf(1.0) } // Default: Unaltered walls (Ja = 1.0)
    var jwSelected by remember { mutableStateOf(1.0) } // Default: Dry (Jw = 1.0)
    var srfSelected by remember { mutableStateOf(1.0) } // Default: Medium stress (SRF = 1.0)

    var tunnelSpan by remember { mutableStateOf(8.0f) }
    var esrSelected by remember { mutableStateOf(1.3f) }

    val rqd = rqdStr.toDoubleOrNull() ?: 0.0
    val qScore = (if (rqd <= 0.0) 10.0 else rqd) / jnSelected * (jrSelected / jaSelected) * (jwSelected / srfSelected)

    val qClass = when {
        qScore < 0.01 -> if (isFarsiMode) "به طور استثنایی ضعیف" else "Exceptionally poor"
        qScore < 0.1 -> if (isFarsiMode) "فوق‌العاده ضعیف" else "Extremely poor"
        qScore < 1.0 -> if (isFarsiMode) "بسیار ضعیف" else "Very poor"
        qScore < 4.0 -> if (isFarsiMode) "ضعیف" else "Poor"
        qScore < 10.0 -> if (isFarsiMode) "متوسط (نیمه‌پایدار)" else "Fair"
        qScore < 40.0 -> if (isFarsiMode) "خوب (پایدار)" else "Good"
        qScore < 100.0 -> if (isFarsiMode) "بسیار خوب" else "Very good"
        qScore < 400.0 -> if (isFarsiMode) "فوق‌العاده خوب" else "Extremely good"
        else -> if (isFarsiMode) "به طور استثنایی عالی" else "Exceptionally good"
    }

    val jnOptions = listOf(
        0.5 to (if (isFarsiMode) "سنگ یکپارچه، بدون درزه یا درزه‌های خیلی کم (Jn = 0.5)" else "Massive, no or few joints (Jn = 0.5)"),
        1.0 to (if (isFarsiMode) "یک دسته درزه (Jn = 1.0)" else "One joint set (Jn = 1.0)"),
        2.0 to (if (isFarsiMode) "یک دسته درزه به همراه درزه تصادفی (Jn = 2.0)" else "One joint set plus random (Jn = 2.0)"),
        4.0 to (if (isFarsiMode) "دو دسته درزه (Jn = 4.0)" else "Two joint sets (Jn = 4.0)"),
        6.0 to (if (isFarsiMode) "دو دسته درزه به همراه درزه تصادفی (Jn = 6.0)" else "Two joint sets plus random (Jn = 6.0)"),
        9.0 to (if (isFarsiMode) "سه دسته درزه (Jn = 9.0)" else "Three joint sets (Jn = 9.0)"),
        12.0 to (if (isFarsiMode) "سه دسته درزه به همراه درزه تصادفی (Jn = 12.0)" else "Three joint sets plus random (Jn = 12.0)"),
        15.0 to (if (isFarsiMode) "چهار دسته درزه یا بیشتر (Jn = 15.0)" else "Four or more joint sets (Jn = 15.0)"),
        20.0 to (if (isFarsiMode) "سنگ کاملاً خردشده، شبیه خاک (Jn = 20.0)" else "Crushed rock, earth-like (Jn = 20.0)")
    )

    val jrOptions = listOf(
        4.0 to (if (isFarsiMode) "درزه‌های غیرممتد و گسسته (Jr = 4.0)" else "Discontinuous joints (Jr = 4.0)"),
        3.0 to (if (isFarsiMode) "زبر یا موج‌دار، ناهموار (Jr = 3.0)" else "Rough or undulating, rustic (Jr = 3.0)"),
        2.0 to (if (isFarsiMode) "صاف و موج‌دار (Jr = 2.0)" else "Smooth, undulating (Jr = 2.0)"),
        1.5 to (if (isFarsiMode) "زبر یا ناهموار، صفحه‌ای (Jr = 1.5)" else "Rough or irregular, planar (Jr = 1.5)"),
        1.0 to (if (isFarsiMode) "صاف و صفحه‌ای (Jr = 1.0)" else "Smooth, planar (Jr = 1.0)"),
        0.5 to (if (isFarsiMode) "آینه‌لغز یا لغزنده، صفحه‌ای (Jr = 0.5)" else "Slickensided, planar (Jr = 0.5)")
    )

    val jaOptions = listOf(
        0.75 to (if (isFarsiMode) "درزه‌های کاملاً جوش‌خورده با پرکننده سخت (Ja = 0.75)" else "Tightly healed, hard filler (Ja = 0.75)"),
        1.0 to (if (isFarsiMode) "دیواره‌های دست‌نخورده، فقط لکه‌دار (Ja = 1.0)" else "Unaltered walls, surface staining only (Ja = 1.0)"),
        2.0 to (if (isFarsiMode) "دیواره‌های درزه کمی دگرگون‌شده، پرکننده ماسه‌ای (Ja = 2.0)" else "Slightly altered joint walls, sandy filler (Ja = 2.0)"),
        3.0 to (if (isFarsiMode) "پوشش‌های رسی-ماسی یا لای‌دار (Ja = 3.0)" else "Silty- or sandy-clay coatings (Ja = 3.0)"),
        4.0 to (if (isFarsiMode) "پوشش‌های نرم‌شونده رسی رسوبی (Ja = 4.0)" else "Softening clay mineral coatings (Ja = 4.0)"),
        8.0 to (if (isFarsiMode) "پرکننده رسی متورم‌شونده مانند بنتونیت (Ja = 8.0)" else "Swelling clay filler e.g. bentonite (Ja = 8.0)")
    )

    val jwOptions = listOf(
        1.0 to (if (isFarsiMode) "خشک یا جریان جزئی و ناچیز آب (Jw = 1.0)" else "Dry, minor inflow (Jw = 1.0)"),
        0.66 to (if (isFarsiMode) "جریان آب متوسط با فشار گذرا (Jw = 0.66)" else "Medium inflow, transient pressure (Jw = 0.66)"),
        0.5 to (if (isFarsiMode) "جریان آب زیاد، فشار بالا در درزه‌های باز (Jw = 0.5)" else "Large inflow, high pressure in open joints (Jw = 0.5)"),
        0.33 to (if (isFarsiMode) "جریان بسیار شدید آب همراه با شستشوی پرکننده (Jw = 0.33)" else "Large inflow with washouts of filler (Jw = 0.33)"),
        0.1 to (if (isFarsiMode) "جریان آب فوق‌العاده شدید و استثنایی (Jw = 0.1)" else "Exceptionally high inflow (Jw = 0.1)")
    )

    val srfOptions = listOf(
        1.0 to (if (isFarsiMode) "سرباره با عمق متوسط، تنش پایدار (SRF = 1.0)" else "Medium depth overburden, stable stress (SRF = 1.0)"),
        2.5 to (if (isFarsiMode) "تنش بالا، ساختار پایدار و محکم (SRF = 2.5)" else "High stress, tight stable structure (SRF = 2.5)"),
        5.0 to (if (isFarsiMode) "مچالگی ملایم، زون‌های برشی رسی پلاستیک (SRF = 5.0)" else "Mild squeezing, plastic clay shear zones (SRF = 5.0)"),
        10.0 to (if (isFarsiMode) "سنگ مچاله‌شونده سنگین، عمق بسیار زیاد (SRF = 10.0)" else "Heavy squeezing rock, large depth (SRF = 10.0)"),
        20.0 to (if (isFarsiMode) "رس متورم‌شونده، هیدراتاسیون فعال (SRF = 20.0)" else "Swelling clay, active hydration (SRF = 20.0)")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isFarsiMode) "ماشین‌حساب ژئوتکنیکی سیستم Q بارتون" else "Barton's Q-System Geotechnical Calculator", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 13.sp, 
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isFarsiMode) "فرمول محاسباتی: Q = (RQD / Jn) * (Jr / Ja) * (Jw / SRF)" else "Computational Formula: Q = (RQD/Jn) * (Jr/Ja) * (Jw/SRF)", 
                        fontSize = 11.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isFarsiMode) "ابزار صحرایی برای رده‌بندی ساختاری کیفیت توده سنگ جهت ارزیابی پایداری هسته حفاری، تونل‌ها و مغارهای معدنی زیرزمینی." else "Field tool for structural rock mass quality classification to assess borehole core and underground mine stability.", 
                        fontSize = 10.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = locName,
                onValueChange = { locName = it; if (it.isNotBlank()) locError = false },
                label = { Text(if (isFarsiMode) "نام موقعیت (مانند پرتال تونل، دیواره شرقی کارگاه)" else "Location Name (e.g. Tunnel Portal, East Pit wall)") },
                modifier = Modifier.fillMaxWidth(),
                isError = locError,
                shape = RoundedCornerShape(8.dp)
            )
            if (locError) {
                Text(if (isFarsiMode) "نام موقعیت جهت ثبت لاگ اجباری است." else "Location name is required to save the log.", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
            }
        }

        item {
            var showImportDialog by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showImportDialog = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isFarsiMode) "بارگذاری داده‌ها از مغزه‌های حفاری" else "Import from Borehole Core Runs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )
            }

            if (showImportDialog) {
                BoreholeCoreRunImportDialog(
                    viewModel = viewModel,
                    projectId = projectId,
                    onDismiss = { showImportDialog = false },
                    onImport = { bhCode, run ->
                        locName = "BH-$bhCode Run-${run.runNumber}"
                        locError = false
                        rqdStr = run.rqdPercent.toString()
                        
                        val importNotes = if (isFarsiMode) {
                            "بارگذاری شده از گمانه $bhCode ران ${run.runNumber} (لیتولوژی: ${run.lithology})."
                        } else {
                            "Loaded from Borehole $bhCode Core Run #${run.runNumber}. Lithology: ${run.lithology}."
                        }
                        notes = importNotes + "\n" + notes
                        showImportDialog = false
                    }
                )
            }
        }

        item {
            OutlinedTextField(
                value = rqdStr,
                onValueChange = { rqdStr = it },
                label = { Text(if (isFarsiMode) "شاخص کیفیت سنگ، RQD (بین 0 تا 100)" else "Rock Quality Designation, RQD (0 - 100)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }

        item {
            Text(
                text = if (isFarsiMode) "ضریب سیستم درزه‌داری (Jn):" else "Joint Set Number (Jn):", 
                fontWeight = FontWeight.Bold, 
                fontSize = 11.sp, 
                color = MaterialTheme.colorScheme.primary
            )
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(jnOptions.firstOrNull { it.first == jnSelected }?.second ?: "", fontSize = 11.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    jnOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.second, fontSize = 11.sp) },
                            onClick = { jnSelected = opt.first; expanded = false }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (isFarsiMode) "شاخص زبری ناهمواری درزه (Jr):" else "Joint Roughness (Jr):", 
                fontWeight = FontWeight.Bold, 
                fontSize = 11.sp, 
                color = MaterialTheme.colorScheme.primary
            )
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(jrOptions.firstOrNull { it.first == jrSelected }?.second ?: "", fontSize = 11.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    jrOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.second, fontSize = 11.sp) },
                            onClick = { jrSelected = opt.first; expanded = false }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (isFarsiMode) "ضریب تغییر و دگرگونی دیواره و اصطکاک (Ja):" else "Joint Alteration & Friction (Ja):", 
                fontWeight = FontWeight.Bold, 
                fontSize = 11.sp, 
                color = MaterialTheme.colorScheme.primary
            )
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(jaOptions.firstOrNull { it.first == jaSelected }?.second ?: "", fontSize = 11.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    jaOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.second, fontSize = 11.sp) },
                            onClick = { jaSelected = opt.first; expanded = false }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (isFarsiMode) "ضریب جریان آب درون درزه (Jw):" else "Joint Water Factor (Jw):", 
                fontWeight = FontWeight.Bold, 
                fontSize = 11.sp, 
                color = MaterialTheme.colorScheme.primary
            )
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(jwOptions.firstOrNull { it.first == jwSelected }?.second ?: "", fontSize = 11.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    jwOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.second, fontSize = 11.sp) },
                            onClick = { jwSelected = opt.first; expanded = false }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (isFarsiMode) "ضریب کاهش تنش (SRF):" else "Stress Reduction Factor (SRF):", 
                fontWeight = FontWeight.Bold, 
                fontSize = 11.sp, 
                color = MaterialTheme.colorScheme.primary
            )
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(srfOptions.firstOrNull { it.first == srfSelected }?.second ?: "", fontSize = 11.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    srfOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.second, fontSize = 11.sp) },
                            onClick = { srfSelected = opt.first; expanded = false }
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isFarsiMode) "مقدار محاسبه‌شده شاخص ساختاری Q:" else "Calculated Structural Q-Value:", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 10.sp, 
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Q = ${String.format(Locale.US, "%.4f", qScore)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = (if (isFarsiMode) "کلاس کیفی توده سنگ: " else "Rock Quality Class: ") + qClass,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = if (isFarsiMode) "🔬 شبیه‌ساز مهندسی نگهداری تونل (استاندارد بارتون و گریمستاد)" else "🔬 Scientific Tunnel Support Simulator (Barton & Grimstad Standard)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Tunnel Span entry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.1f)) {
                            Text(
                                text = (if (isFarsiMode) "عرض دهانه تونل: " else "Tunnel Span / Width: ") + String.format(Locale.US, "%.1f", tunnelSpan) + " m", 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = tunnelSpan,
                                onValueChange = { tunnelSpan = it },
                                valueRange = 2.0f..25.0f,
                                modifier = Modifier.height(20.dp)
                            )
                        }

                        // ESR Selector
                        Column(modifier = Modifier.weight(0.9f)) {
                            Text(if (isFarsiMode) "ضریب کاربری ESR:" else "ESR Ratio:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            var esrExpanded by remember { mutableStateOf(false) }
                            val esrOptions = listOf(
                                0.8f to (if (isFarsiMode) "0.8 تونل‌های اصلی" else "0.8 Tunnels"),
                                1.0f to (if (isFarsiMode) "1.0 راه‌آهن/بزرگراه" else "1.0 Rail/Road"),
                                1.3f to (if (isFarsiMode) "1.3 آبراهه/مجاری" else "1.3 Conduits"),
                                1.6f to (if (isFarsiMode) "1.6 مغارهای معدنی" else "1.6 Mining Drifts"),
                                3.0f to (if (isFarsiMode) "3.0 تونل موقت" else "3.0 Temp Shafts")
                            )
                            Box {
                                OutlinedButton(
                                    onClick = { esrExpanded = true },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = esrOptions.firstOrNull { it.first == esrSelected }?.second ?: String.format(Locale.US, "%.1f", esrSelected),
                                        fontSize = 10.sp
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(expanded = esrExpanded, onDismissRequest = { esrExpanded = false }) {
                                    esrOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt.second, fontSize = 11.sp) },
                                            onClick = { esrSelected = opt.first; esrExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Perform precise engineering math
                    val deValue = tunnelSpan / esrSelected
                    val maxUnsupportedSpan = 2.0 * esrSelected * Math.pow(qScore, 0.4)
                    val boltLength = 2.0 + (0.15 * tunnelSpan / esrSelected)

                    // Spacing estimates (continuous curve modeling based on Q-value)
                    val boltSpacingVal = if (qScore < 0.001) {
                        0.8
                    } else if (qScore < 0.1) {
                        0.8 + 0.15 * Math.log10(qScore * 1000) // 0.8m to 1.1m
                    } else if (qScore < 10) {
                        1.1 + 0.35 * Math.log10(qScore * 10) // 1.1m to 1.8m
                    } else {
                        1.8 + 0.5 * Math.log10(qScore / 10) // 1.8m to 3.0m
                    }
                    val finalBoltSpacing = boltSpacingVal.coerceIn(0.8, 3.0)

                    // Shotcrete Thickness formula (based on Q and equivalent dimension De)
                    val baseShotcreteThickness = when {
                        qScore < 0.001 -> 250f
                        qScore < 0.01 -> 160f + 45f * (3f - Math.log10(qScore * 100000).toFloat())
                        qScore < 0.1 -> 100f + 30f * (2f - Math.log10(qScore * 1000).toFloat())
                        qScore < 1.0 -> 60f + 20f * (1f - Math.log10(qScore * 10).toFloat()) // Fixed domain term to keep it robust
                        qScore < 10.0 -> 30f + 15f * (1f - Math.log10(qScore).toFloat())
                        qScore < 40.0 -> 0f + 10f * (1f - Math.log10(qScore / 10).toFloat())
                        else -> 0f
                    }
                    val sizeFactor = Math.pow(deValue / 6.0, 0.25).toFloat().coerceIn(0.5f, 2.0f)
                    val shotcreteThicknessMm = (baseShotcreteThickness * sizeFactor).coerceIn(0f, 300f)

                    // Support Class Category designation (Grimstad 9 Classes)
                    val (supportClassStr, supportClassDesc) = when {
                        qScore < 0.001 -> (if (isFarsiMode) "رده نگهداری ۹" else "Category 9") to (if (isFarsiMode) "لاینینگ کامل بتن ریخته‌شده یا بتن پاششی به شدت مسلح (>200mm)." else "Cast concrete lining or extremely thick reinforced ribs (>200mm shotcrete).")
                        qScore < 0.01 -> (if (isFarsiMode) "رده نگهداری ۸" else "Category 8") to (if (isFarsiMode) "پیچ‌سنگ سیستماتیک با شاتکریت الیافی ضخیم >150mm و قاب‌های سنگین فولادی." else "Systematic bolting with >150mm fiber-reinforced shotcrete and heavy steel arches.")
                        qScore < 0.1 -> (if (isFarsiMode) "رده نگهداری ۷" else "Category 7") to (if (isFarsiMode) "پیچ‌سنگ سیستماتیک با شاتکریت الیافی 120-150mm و قاب‌های تقویت‌شده (RRS)." else "Systematic bolting with 120-150mm fiber-reinforced shotcrete and reinforced ribs (RRS).")
                        qScore < 1.0 -> (if (isFarsiMode) "رده نگهداری ۶" else "Category 6") to (if (isFarsiMode) "پیچ‌سنگ سیستماتیک با شاتکریت الیافی به ضخامت 90-120mm." else "Systematic bolting with 90-120mm fiber-reinforced shotcrete.")
                        qScore < 4.0 -> (if (isFarsiMode) "رده نگهداری ۵" else "Category 5") to (if (isFarsiMode) "پیچ‌سنگ سیستماتیک با شاتکریت الیافی به ضخامت 50-90mm." else "Systematic bolting with 50-90mm fiber-reinforced shotcrete.")
                        qScore < 10.0 -> (if (isFarsiMode) "رده نگهداری ۴" else "Category 4") to (if (isFarsiMode) "پیچ‌سنگ سیستماتیک با شاتکریت ساده به ضخامت 50mm." else "Systematic bolting with 50mm unreinforced shotcrete.")
                        qScore < 40.0 -> (if (isFarsiMode) "رده نگهداری ۳" else "Category 3") to (if (isFarsiMode) "پیچ‌سنگ سیستماتیک همراه با شاتکریت موضعی پراکنده برحسب نیاز." else "Systematic bolting, spot shotcrete as needed.")
                        qScore < 1000.0 -> (if (isFarsiMode) "رده نگهداری ۲" else "Category 2") to (if (isFarsiMode) "پیچ‌سنگ موضعی، معمولاً دیواره‌ها نیازی به نگهداری دائمی ندارند." else "Spot bolting, usually unsupported walls.")
                        else -> (if (isFarsiMode) "رده نگهداری ۱" else "Category 1") to (if (isFarsiMode) "توده سنگ پایدار، کاملاً بدون نیاز به نگهداری." else "Completely unsupported rock mass.")
                    }

                    // Barton Active Support Pressure (P_roof)
                    val safeJr = if (jrSelected <= 0.0) 1.0 else jrSelected
                    val safeJn = if (jnSelected <= 0.0) 1.0 else jnSelected
                    val pRoofKgCm2 = (2.0 * Math.sqrt(safeJn)) / (3.0 * safeJr * Math.pow(qScore, 1.0/3.0))
                    val pRoofKpa = pRoofKgCm2 * 98.0665

                    // Render dynamic values inside nice layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isFarsiMode) "کلاس نگهداری" else "SUPPORT CLASS", 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(supportClassStr, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                Text(supportClassDesc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 11.sp)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isFarsiMode) "طرح چیدمان راک‌بولت" else "BOLTING SCHEME", 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("S: ${String.format(Locale.US, "%.2f", finalBoltSpacing)}m | L: ${String.format(Locale.US, "%.2f", boltLength)}m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (isFarsiMode) "فاصله سیستماتیک راک‌بولت (S) و طول آنها (L)." else "Suggested systematic spacing and length of structural rock bolts.", 
                                    fontSize = 9.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isFarsiMode) "شاتکریت مسلح الیافی" else "FIBER SHOTCRETE (S(fr))", 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (shotcreteThicknessMm > 0) "${shotcreteThicknessMm.toInt()} mm" else (if (isFarsiMode) "بدون نیاز" else "None Required"), 
                                    fontSize = 14.sp, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    color = if (shotcreteThicknessMm > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isFarsiMode) "ضخامت شاتکریت الیاف‌دار تاییدشده گریمستاد." else "Fiber-reinforced concrete thickness computed via Grimstad criteria.", 
                                    fontSize = 9.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isFarsiMode) "فشار فعال نگهداری (Pr)" else "SUPPORT PRESSURE (Pr)", 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("${String.format(Locale.US, "%.1f", pRoofKpa)} kPa", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828))
                                Text(
                                    text = if (isFarsiMode) "معادل بار فعال ${String.format(Locale.US, "%.3f", pRoofKgCm2)} کیلوگرم بر سانتی‌متر مربع." else "Equivalent to ${String.format(Locale.US, "%.3f", pRoofKgCm2)} kg/cm² active reinforcement load.", 
                                    fontSize = 9.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isFarsiMode) "حداکثر دهانه بدون نگهداری" else "MAX UNSUPPORTED SPAN", 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", maxUnsupportedSpan)} " + (if (isFarsiMode) "متر" else "meters"), 
                                    fontSize = 14.sp, 
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = (if (isFarsiMode) "بعد معادل (De): " else "Equivalent Dimension (De): ") + String.format(Locale.US, "%.2f", deValue),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isFarsiMode) "مدل ترسیمی و الگوی مقطع پایدارسازی تونل:" else "Tunnel Support Cross Section Simulation Pattern:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Render the visual canvas drawing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            val w = size.width
                            val h = size.height
                            
                            val centerX = w / 2f
                            val centerY = h * 0.75f 
                            
                            val scale = (w * 0.40f) / 16.0f 
                            val tunnelRadius = (tunnelSpan / 2f) * scale
                            
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(centerX - tunnelRadius, centerY)
                                val wallHeight = tunnelRadius * 0.5f
                                lineTo(centerX - tunnelRadius, centerY - wallHeight)
                                cubicTo(
                                    centerX - tunnelRadius, centerY - wallHeight * 2.2f,
                                    centerX + tunnelRadius, centerY - wallHeight * 2.2f,
                                    centerX + tunnelRadius, centerY - wallHeight
                                )
                                lineTo(centerX + tunnelRadius, centerY)
                                lineTo(centerX - tunnelRadius, centerY)
                                close()
                            }
                            
                            // Background rock mass
                            drawRect(
                                color = Color(0xFFF1EFEA).copy(alpha = 0.1f),
                                size = size
                            )
                            
                            // Dark tunnel cave interior
                            drawPath(
                                path = path,
                                color = Color(0xFF1E1E1E)
                            )
                            
                            // 1. Fiber shotcrete coloring surrounding path
                            if (shotcreteThicknessMm > 0) {
                                val strokeWidth = (shotcreteThicknessMm / 10f).coerceIn(3f, 15f)
                                drawPath(
                                    path = path,
                                    color = Color(0xFF4CAF50).copy(alpha = 0.8f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = strokeWidth,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                            }
                            
                            // 2. Protruding radial Rock Bolts
                            val boltLenPx = (boltLength * scale * 1.5f).toFloat() // exaggerated to show visual difference nicely
                            val numBolts = 7
                            for (i in 0 until numBolts) {
                                val angleRad = Math.toRadians(15.0 + i * (150.0 / (numBolts - 1))).toFloat()
                                val cos = Math.cos(angleRad.toDouble()).toFloat()
                                val sin = Math.sin(angleRad.toDouble()).toFloat()
                                
                                val archX = centerX + tunnelRadius * cos
                                val archY = centerY - tunnelRadius * 0.4f - tunnelRadius * sin
                                
                                val endX = archX + boltLenPx * cos
                                val endY = archY - boltLenPx * sin
                                
                                // Anchor bolt line
                                drawLine(
                                    color = Color(0xFFFF9100),
                                    start = androidx.compose.ui.geometry.Offset(archX, archY),
                                    end = androidx.compose.ui.geometry.Offset(endX, endY),
                                    strokeWidth = 3f
                                )
                                
                                // Anchor washer dot on face
                                drawCircle(
                                    color = Color(0xFFFFCC80),
                                    radius = 4f,
                                    center = androidx.compose.ui.geometry.Offset(archX, archY)
                                )
                            }
                            
                            // Tunnel floor roadway line
                            drawLine(
                                color = Color.White.copy(alpha = 0.3f),
                                start = androidx.compose.ui.geometry.Offset(centerX - tunnelRadius, centerY),
                                end = androidx.compose.ui.geometry.Offset(centerX + tunnelRadius, centerY),
                                strokeWidth = 2f
                            )
                        }
                        
                        // Dimensions label overlay at bottom left of Canvas
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = (if (isFarsiMode) "دهانه تونل: " else "Tunnel Span: ") + String.format(Locale.US, "%.1f", tunnelSpan) + "m", 
                                color = Color.White, 
                                fontSize = 8.sp, 
                                fontWeight = FontWeight.Bold
                            )
                            if (shotcreteThicknessMm > 0) {
                                Text(
                                    text = (if (isFarsiMode) "شاتکریت الیافی: " else "Shotcrete: ") + "${shotcreteThicknessMm.toInt()}mm", 
                                    color = Color(0xFF81C784), 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(if (isFarsiMode) "شاتکریت: بدون نیاز" else "Shotcrete: None", color = Color.White, fontSize = 8.sp)
                            }
                        }
                        
                        // Dimensions label overlay at bottom right of Canvas
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = (if (isFarsiMode) "طول راک‌بولت: " else "Bolt Length: ") + String.format(Locale.US, "%.2f", boltLength) + "m", 
                                color = Color(0xFFFFE082), 
                                fontSize = 8.sp, 
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = (if (isFarsiMode) "فاصله گام‌ها: " else "Bolt Spacing: ") + String.format(Locale.US, "%.2f", finalBoltSpacing) + "m", 
                                color = Color(0xFFFFB74D), 
                                fontSize = 8.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = (if (isFarsiMode) "پروفیل مقطع طراحی سیستم نگهداری تجربی" else "Geotechnical Support Design Cross-Section") + " (ESR = ${String.format(Locale.US, "%.1f", esrSelected)})",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(if (isFarsiMode) "یادداشت‌های صحرایی / بررسی ژئومکانیکی درزه‌ها" else "Field Notes / Structural joint comments") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }

        item {
            Button(
                onClick = {
                    if (locName.isBlank()) {
                        locError = true
                    } else {
                        locError = false
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        viewModel.saveRockMechanicsEvaluation(
                            RockMechanicsEvaluation(
                                projectId = projectId,
                                locationName = locName,
                                evaluationType = "Q-System",
                                evaluationDate = sdf.format(Date()),
                                rmiUcs = rqd,
                                rmiJp = jnSelected,
                                rmiValue = qScore,
                                notes = "Jn: $jnSelected, Jr: $jrSelected, Ja: $jaSelected, Jw: $jwSelected, SRF: $srfSelected\n$notes"
                            )
                        )
                        locName = ""
                        notes = ""
                        val successMsg = if (isFarsiMode) "محاسبات ژئوتکنیکی سیستم Q بارتون با موفقیت ثبت شد!" else "Geotechnical Barton Q Log committed successfully!"
                        Toast.makeText(viewModel.application, successMsg, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = locName.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isFarsiMode) "ذخیره لاگ سیستم Q بارتون" else "Commit Q-System Log")
            }
        }
    }
}

@Composable
fun KinematicStereonetPanel(
    viewModel: GeologyViewModel,
    projectId: Int,
    onNavigateToTab: ((com.example.MainTab) -> Unit)? = null
) {
    val context = LocalContext.current
    val stations by viewModel.activeProjectStations.collectAsStateWithLifecycle()
    val allObservations by viewModel.allObservations.collectAsStateWithLifecycle()

    var slopeDip by remember { mutableStateOf("65") }
    var slopeDipDir by remember { mutableStateOf("135") }
    var frictionAngle by remember { mutableStateOf("30") }
    var lateralLimit by remember { mutableStateOf("20") }
    
    val stationIds = remember(stations) { stations.map { it.id }.toSet() }
    val projectObservations = remember(allObservations, stationIds) {
        allObservations.filter { it.stationId in stationIds }
    }

    val dipsJson = remember(projectObservations) {
        val filtered = projectObservations.filter { it.strike != null && it.dip != null }
        filtered.joinToString(prefix = "[", postfix = "]") { obs ->
            val strikeStr = obs.strike ?: 0
            val dipStr = obs.dip ?: 0
            val dipDirStr = obs.dipDirection ?: 0
            val code = (obs.notes ?: "").replace("\"", "\\\"").replace("\n", " ")
            """{"strike": $strikeStr, "dip": $dipStr, "dipDir": $dipDirStr, "code": "$code"}"""
        }
    }

    val htmlContent = remember(slopeDip, slopeDipDir, frictionAngle, lateralLimit, dipsJson) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                body {
                    background-color: #0c100d;
                    color: #fff;
                    font-family: system-ui, -apple-system, sans-serif;
                    padding: 8px;
                    margin: 0;
                    text-align: center;
                }
                canvas {
                    background-color: #0c100d;
                    border: 1px solid #444;
                    border-radius: 8px;
                    display: block;
                    margin: 12px auto;
                    max-width: 95%;
                    box-shadow: 0 4px 6px rgba(0,0,0,0.3);
                }
                .label {
                    color: #0dffe1;
                    font-weight: bold;
                }
                .card {
                    background: #151c17;
                    border-radius: 8px;
                    padding: 10px;
                    margin-top: 8px;
                    text-align: left;
                    font-size: 11px;
                    line-height: 1.5;
                }
            </style>
        </head>
        <body>
            <h4 style="margin:4px 0; color:#0dffe1;">3D Equal-Angle Schmidt Projection</h4>
            <canvas id="stereonet" width="300" height="300"></canvas>
            
            <div class="card">
                <span class="label">PLANAR SLIDING VERDICT:</span> <span id="planar_verdict" style="color:#ff5252; font-weight:bold;">0%</span><br/>
                <span class="label">WEDGE SLIDING VERDICT:</span> <span id="wedge_verdict" style="color:#e040fb; font-weight:bold;">0%</span><br/>
                <span class="label">TOPPLING HAZARD VERDICT:</span> <span id="toppling_verdict" style="color:#ffab40; font-weight:bold;">0%</span>
            </div>

            <script>
                var canvas = document.getElementById('stereonet');
                var ctx = canvas.getContext('2d');
                var cx = 150, cy = 150, r = 135;

                var slopeD = parseFloat("$slopeDip") || 65;
                var slopeDD = parseFloat("$slopeDipDir") || 135;
                var friction = parseFloat("$frictionAngle") || 30;
                var latLimit = parseFloat("$lateralLimit") || 20;
                var joints = $dipsJson;

                function drawNet() {
                    ctx.clearRect(0, 0, 300, 300);
                    
                    // Main circum
                    ctx.strokeStyle = '#444';
                    ctx.lineWidth = 1.5;
                    ctx.beginPath();
                    ctx.arc(cx, cy, r, 0, 2*Math.PI);
                    ctx.stroke();

                    // Net rings
                    ctx.strokeStyle = '#222';
                    for (var r_ang = 20; r_ang <= 80; r_ang += 20) {
                        ctx.beginPath();
                        var rad_i = r * Math.tan((r_ang/2) * Math.PI / 180);
                        ctx.arc(cx, cy, rad_i, 0, 2*Math.PI);
                        ctx.stroke();
                    }

                    // Card axes
                    ctx.strokeStyle = '#2d332f';
                    ctx.beginPath();
                    ctx.moveTo(cx - r, cy); ctx.lineTo(cx + r, cy);
                    ctx.moveTo(cx, cy - r); ctx.lineTo(cx, cy + r);
                    ctx.stroke();

                    // North label
                    ctx.fillStyle = '#ffb300';
                    ctx.font = 'bold 11px Arial';
                    ctx.fillText('N', cx - 4, cy - r - 4);
                }

                // Convert dip, dipDir to projection coordinate
                function getPoleCoords(dip, dipDir) {
                    var trend = (dipDir - 180) * Math.PI / 180;
                    var rad_dist = r * Math.tan((dip / 2) * Math.PI / 180);
                    var px = cx + rad_dist * Math.sin(trend);
                    var py = cy - rad_dist * Math.cos(trend);
                    return {x: px, y: py};
                }

                function drawEnvelopes() {
                    // Planar Limit friction zone
                    var f_rad = r * Math.tan((friction / 2) * Math.PI / 180);
                    ctx.fillStyle = 'rgba(255, 82, 82, 0.08)';
                    ctx.beginPath();
                    ctx.arc(cx, cy, f_rad, 0, 2*Math.PI);
                    ctx.fill();

                    // Lateral limits
                    ctx.strokeStyle = 'rgba(255, 171, 64, 0.4)';
                    ctx.lineWidth = 1.5;
                    ctx.beginPath();
                    var latMin = (slopeDD - latLimit) * Math.PI / 180;
                    var latMax = (slopeDD + latLimit) * Math.PI / 180;
                    ctx.moveTo(cx, cy);
                    ctx.lineTo(cx + r * Math.sin(latMin), cy - r * Math.cos(latMin));
                    ctx.moveTo(cx, cy);
                    ctx.lineTo(cx + r * Math.sin(latMax), cy - r * Math.cos(latMax));
                    ctx.stroke();
                }

                function logAnalysis() {
                    var planarFail = 0, toppleFail = 0, wedgeFail = 0;
                    var total = joints.length || 1;

                    joints.forEach(function(j) {
                        var jd = j.dip;
                        var jdd = j.dipDir;

                        // Coords plotting
                        var pt = getPoleCoords(jd, jdd);
                        
                        // Check Planar Sliding failure conditions
                        var ang_diff = Math.abs(jdd - slopeDD);
                        if (ang_diff > 180) ang_diff = 360 - ang_diff;

                        var inPlanarRegion = (ang_diff <= latLimit) && (jd < slopeD) && (jd > friction);
                        if (inPlanarRegion) planarFail++;

                        // Check Toppling conditions
                        var topple_diff = Math.abs(jdd - (slopeDD - 180));
                        if (topple_diff > 180) topple_diff = 360 - topple_diff;
                        var inToppleRegion = (topple_diff <= latLimit) && (jd >= (90 - slopeD + friction));
                        if (inToppleRegion) toppleFail++;

                        // Wedge sliding bounds indicator
                        if (jd > friction && jd < slopeD) {
                            wedgeFail++;
                        }

                        // Draw joint point
                        ctx.fillStyle = inPlanarRegion ? '#ff5252' : (inToppleRegion ? '#ffab40' : '#4caf50');
                        ctx.beginPath();
                        ctx.arc(pt.x, pt.y, 5, 0, 2*Math.PI);
                        ctx.fill();
                        ctx.strokeStyle = '#fff';
                        ctx.lineWidth = 1;
                        ctx.stroke();
                    });

                    document.getElementById('planar_verdict').innerText = Math.round((planarFail / total) * 100) + "% Risk (" + planarFail + " joints)";
                    document.getElementById('toppling_verdict').innerText = Math.round((toppleFail / total) * 100) + "% Risk (" + toppleFail + " joints)";
                    document.getElementById('wedge_verdict').innerText = Math.round((wedgeFail / total) * 100 * 0.7) + "% Risk";
                }

                function render() {
                    drawNet();
                    drawEnvelopes();
                    logAnalysis();
                }

                render();
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val isFarsi = viewModel.isFarsiModeSetting.value
        if (projectObservations.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFarsi) "برداشت‌های همگام‌سازی شده" else "Auto-Synced Discontinuities",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isFarsi)
                                "تعداد ${projectObservations.size} دسته درزه ساختاری فعال از بخش زمین‌شناسی بارگذاری شده است."
                            else
                                "${projectObservations.size} structural discontinuities loaded from Joint Mapping observations.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (onNavigateToTab != null) {
                        TextButton(
                            onClick = { onNavigateToTab(com.example.MainTab.GeologicalMapping) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isFarsi) "ویرایش برداشت‌ها" else "Edit Joints",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFarsi) "برداشتی یافت نشد" else "No Discontinuities Found",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (isFarsi)
                                "هیچ درزه یا لایه‌بندی برای این پروژه ثبت نشده است."
                            else
                                "Go to Geological Mapping to record dip/dip-direction first.",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (onNavigateToTab != null) {
                        Button(
                            onClick = { onNavigateToTab(com.example.MainTab.GeologicalMapping) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isFarsi) "ثبت درزه" else "Record Now",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Slope Face & Discontinuity Analyzer Configuration",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = slopeDip,
                        onValueChange = { slopeDip = it },
                        label = { Text("Slope Dip°", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = slopeDipDir,
                        onValueChange = { slopeDipDir = it },
                        label = { Text("Slope Dip Direction°", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = frictionAngle,
                        onValueChange = { frictionAngle = it },
                        label = { Text("Basal Friction Angle°", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lateralLimit,
                        onValueChange = { lateralLimit = it },
                        label = { Text("Lateral Limit (Standard 20°)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    }
}

@Composable
fun DynamicSupportPanel(
    viewModel: GeologyViewModel,
    projectId: Int
) {
    var excavationSpan by remember { mutableStateOf("10.0") }
    var esr by remember { mutableStateOf("1.0") } // Tunnel Excavation Support Ratio
    var customQ by remember { mutableStateOf("4.0") }
    var peakDynamicPpv by remember { mutableStateOf("500") } // Peak Particle Velocity (mm/s) under seismicity/blasting
    
    val spanVal = excavationSpan.toDoubleOrNull() ?: 10.0
    val esrVal = esr.toDoubleOrNull() ?: 1.0
    val qVal = customQ.toDoubleOrNull() ?: 4.0
    val ppvVal = peakDynamicPpv.toDoubleOrNull() ?: 500.0

    // Calculations
    val deq = spanVal / esrVal
    val boltLength = 2.0 + (0.15 * spanVal) / esrVal
    
    // Support pressure (Barton formula updated with dynamic load expansion)
    val supportPressureBase = (2.0 * 2.0 * Math.pow(qVal, -0.33)) / 1.5
    val dynamicMultiplier = 1.0 + (ppvVal / 1000.0) // multiplier based on peak dynamic vibration
    val correctedPressure = supportPressureBase * dynamicMultiplier

    // Shotcrete requirement
    val shotcreteThickness = if (qVal < 0.1) {
        (correctedPressure * 12.0).coerceIn(15.0, 30.0)
    } else if (qVal < 1.0) {
        (correctedPressure * 8.0).coerceIn(10.0, 15.0)
    } else if (qVal < 10.0) {
        (correctedPressure * 5.0).coerceIn(5.0, 10.0)
    } else {
        0.0
    }

    // Dynamic energy absorption requirement for yielding bolts (kJ)
    val kineticEnergyKJ = (1.5 * (ppvVal * ppvVal) / 1000000.0) * correctedPressure
    val boltYieldingClass = if (kineticEnergyKJ < 5.0) {
        "Standard Mechanical/Rebar (Anchor Bolts)"
    } else if (kineticEnergyKJ < 15.0) {
        "High-Energy Yielding Rock Bolts (e.g. Swellex Manganese)"
    } else {
        "Heavy-Duty Deep Yielding Dynamic D-Bolts (Energy Absorbing)"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Dynamic & Shock-Reinforced Support Design Input",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = excavationSpan,
                        onValueChange = { excavationSpan = it },
                        label = { Text("Excavation Span (m)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = esr,
                        onValueChange = { esr = it },
                        label = { Text("ESR Factor", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customQ,
                        onValueChange = { customQ = it },
                        label = { Text("Barton Q-Value", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = peakDynamicPpv,
                        onValueChange = { peakDynamicPpv = it },
                        label = { Text("Shock Peak Velocity PPV (mm/s)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Computed Dynamic Support Verdict",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Deq (Equivalent Dimension):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format(Locale.US, "%.2f m", deq), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Design Bolt Length (L):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format(Locale.US, "%.2f meters", boltLength), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF00C853))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dynamic Shock Support Pressure:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format(Locale.US, "%.2f kg/cm²", correctedPressure), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dynamic Impact Energy Requirement:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format(Locale.US, "%.2f kJ/m²", kineticEnergyKJ), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }

                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Text(
                    "Dynamic Bolt Classification:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    boltYieldingClass,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                if (shotcreteThickness > 0) {
                    Text(
                        "Reinforced Shotcrete Specification:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        String.format(Locale.US, "%.1f cm Steel Fiber Reinforced Shotcrete (Dosage: 45kg/m³)", shotcreteThickness),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    Text("No dynamic shotcrete reinforcement required.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun BoreholeCoreRunImportDialog(
    viewModel: GeologyViewModel,
    projectId: Int,
    onDismiss: () -> Unit,
    onImport: (boreholeCode: String, run: BoreholeCoreRun) -> Unit
) {
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()
    val boreholes by viewModel.getBoreholesForProject(projectId).collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedBorehole by remember { mutableStateOf<Borehole?>(null) }
    
    // Dynamically observe core runs once a borehole is selected
    val coreRuns = remember(selectedBorehole) {
        if (selectedBorehole != null) {
            viewModel.getCoreRunsForBorehole(selectedBorehole!!.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isFarsiMode) "بارگذاری داده‌ها از مغزه‌های حفاری" else "Import Data from Borehole Core Runs",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isFarsiMode) 
                        "ابتدا گمانه و سپس ران (پارت) حفاری مورد نظر را انتخاب کنید تا داده‌های RQD و مقاومت فشاری تک‌محوری (UCS) خودکار بارگذاری و امتیازدهی شوند."
                    else 
                        "Select a borehole and then the target core run to automatically populate and score RQD and UCS values.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (boreholes.isEmpty()) {
                    Text(
                        text = if (isFarsiMode) "هیچ گمانه‌ای در این پروژه ثبت نشده است." else "No boreholes recorded in this project yet.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    // Borehole Selector
                    Text(
                        text = if (isFarsiMode) "۱. انتخاب گمانه:" else "1. Select Borehole:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    var expandedBh by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { expandedBh = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedBorehole?.let { "BH: ${it.boreholeCode}" } 
                                        ?: (if (isFarsiMode) "لطفاً گمانه‌ای انتخاب کنید" else "Select Borehole..."),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }

                        DropdownMenu(
                            expanded = expandedBh,
                            onDismissRequest = { expandedBh = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            boreholes.forEach { bh ->
                                DropdownMenuItem(
                                    text = { Text("BH: ${bh.boreholeCode} (Planned: ${bh.plannedDepth}m)", fontSize = 12.sp) },
                                    onClick = {
                                        selectedBorehole = bh
                                        expandedBh = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedBorehole != null) {
                        Text(
                            text = if (isFarsiMode) "۲. انتخاب ران حفاری (مغزه):" else "2. Select Core Run:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp)
                        )

                        if (coreRuns.value.isEmpty()) {
                            Text(
                                text = if (isFarsiMode) "هیچ ران مغزه‌گیری برای این گمانه ثبت نشده است." else "No core runs logged for this borehole yet.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                coreRuns.value.forEach { run ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onImport(selectedBorehole!!.boreholeCode, run)
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Run #${run.runNumber} (${run.depthFrom}m - ${run.depthTo}m)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "RQD: ${run.rqdPercent}% | UCS: ${run.ucsMpa} MPa",
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            if (run.lithology.isNotBlank()) {
                                                Text(
                                                    text = "${if (isFarsiMode) "لیتولوژی: " else "Lithology: "}${run.lithology}",
                                                    fontSize = 10.5.sp,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFarsiMode) "لغو" else "Cancel")
            }
        }
    )
}

