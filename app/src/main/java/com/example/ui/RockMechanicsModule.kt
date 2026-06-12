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
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier
) {
    val globalActiveProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val projectList by viewModel.projects.collectAsStateWithLifecycle()

    // Decoupled tab-specific project selection to avoid trapping the user in a single project
    var localSelectedProjectId by remember { mutableStateOf<Int?>(null) }
    var lastSyncedGlobalProjectId by remember { mutableStateOf<Int?>(null) }

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
            Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = "Rock Mechanics & Geotechnical Classification",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Select an exploration or engineering project for structural RMR calculations, Hoek-Brown rock mass parameters, miner MRMR classification, and rock mass RMi index calculations.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Button(
                onClick = { showCreateProjDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Define New Geotechnical Project")
            }

            Divider()

            Text(
                text = "Available Projects:",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )

            if (projectList.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No projects defined yet.", color = Color.Gray, fontSize = 13.sp)
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
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(project.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(project.description ?: "General Geotechnical Project", fontSize = 12.sp, color = Color.Gray)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }

        if (showCreateProjDialog) {
            AlertDialog(
                onDismissRequest = { showCreateProjDialog = false },
                title = { Text("Define New Geotechnical Project") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = projName,
                            onValueChange = { projName = it },
                            label = { Text("Project Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = projDesc,
                            onValueChange = { projDesc = it },
                            label = { Text("Scope/Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (projName.isNotBlank()) {
                                viewModel.createProjectSilently(projName, projDesc) { newId ->
                                    localSelectedProjectId = newId
                                }
                                showCreateProjDialog = false
                                projName = ""
                                projDesc = ""
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateProjDialog = false }) {
                        Text("Cancel")
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
    modifier: Modifier = Modifier
) {
    val evaluationsFlow = remember(projectId) { viewModel.getRockMechanicsEvaluationsForProject(projectId) }
    val evaluations by evaluationsFlow.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    
    // Toggle state to switch between Simple and Advanced Geotechnical Views
    var isSimpleMode by remember { mutableStateOf(true) }
    
    // Dynamic tab filter mapping to prevent index out of bounds while decluttering
    val activeTabsList = remember(isSimpleMode) {
        if (isSimpleMode) {
            listOf(
                Triple(0, "ثبت لاگ / Logs", Icons.Default.Book),
                Triple(1, "رده‌بندی RMR", Icons.Default.Star),
                Triple(4, "سیستم Q بارتون", Icons.Default.Build)
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
                Text("Geotechnical Evaluator HUD", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Back")
            }
        }

        // Modern Bilingual Mode Switcher Card to de-clutter the geotechnical suite
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
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
                        text = if (isSimpleMode) "نمای ساده جئوتکنیک فعال است" else "نمای کامل تخصصی مکانیک سنگ active",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = if (isSimpleMode) "فقط نمایش دو تحلیل اصلی RMR و Q جهت جلوگیری از شلوغی" else "دسترسی به تمام ابزارها مثل تحلیل شکست هوک-براون و پایداری استریونت",
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
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
                        text = if (isSimpleMode) "نمای پیشرفته" else "ساده‌سازی",
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
                            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> LogJournalPanel(viewModel, projectId, evaluations)
            1 -> RmrPanel(viewModel, projectId)
            2 -> HoekBrownPanel(viewModel, projectId)
            3 -> MrmrRmiPanel(viewModel, projectId)
            4 -> QSystemPanel(viewModel, projectId)
            5 -> KinematicStereonetPanel(viewModel, projectId)
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
    if (evaluations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No geotechnical logs submitted for this project. Use solver calculators next door to compile.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(evaluations) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${log.evaluationType} Log: ${log.locationName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Recorded: ${log.evaluationDate}", fontSize = 10.sp, color = Color.Gray)
                            }
                            IconButton(onClick = { viewModel.deleteRockMechanicsEvaluation(log.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete log", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        when (log.evaluationType) {
                            "RMR" -> {
                                Text("RMR Basic Score: ${log.rmrScore.toInt()} (${log.rmrClass})", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                Text("UCS Rating: ${log.rmrUcsVal} | RQD: ${log.rmrRqdVal} | Spacing: ${log.rmrSpacingVal} | Water: ${log.rmrWaterVal} | Adj: ${log.rmrAdjustmentVal}", fontSize = 11.sp, color = Color.Gray)
                            }
                            "Hoek-Brown" -> {
                                Text("Intact UCS (σci): ${log.hbSigci} MPa | Rock GSI: ${log.hbGsi}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                                Text("mb: ${String.format(Locale.US, "%.3f", log.hbMb)} | s: ${String.format(Locale.US, "%.5f", log.hbS)} | Mass UCS: ${String.format(Locale.US, "%.2f", log.hbSigmaci)} MPa", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            "MRMR" -> {
                                Text("MRMR Score: ${String.format(Locale.US, "%.1f", log.mrmrScore)} (${log.mrmrClass})", fontWeight = FontWeight.SemiBold, color = Color(0xFFE65100), fontSize = 13.sp)
                                Text("Weathering Adj: ${log.mrmrWeatheringAdj} | Blast Adj: ${log.mrmrDrillingAdj} | Joint: ${log.mrmrJointOrientationAdj}", fontSize = 11.sp, color = Color.Gray)
                                if (log.rmiValue > 0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("RMi Index: ${String.format(Locale.US, "%.4f", log.rmiValue)} (Intact UCS: ${log.rmiUcs} MPa, Jp: ${log.rmiJp})", fontWeight = FontWeight.SemiBold, color = Color(0xFF00ACC1), fontSize = 12.sp)
                                }
                            }
                            "RMi" -> {
                                Text("RMi Value: ${String.format(Locale.US, "%.4f", log.rmiValue)}", fontWeight = FontWeight.SemiBold, color = Color(0xFF00ACC1), fontSize = 13.sp)
                                Text("Joint parameter (Jp): ${log.rmiJp} | Intact UCS: ${log.rmiUcs} MPa", fontSize = 11.sp, color = Color.Gray)
                            }
                            "Q-System" -> {
                                Text("Barton's Q-Value: ${String.format(Locale.US, "%.4f", log.rmiValue)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                val qText = when {
                                    log.rmiValue < 0.01 -> "Exceptionally poor"
                                    log.rmiValue < 0.1 -> "Extremely poor"
                                    log.rmiValue < 1.0 -> "Very poor"
                                    log.rmiValue < 4.0 -> "Poor"
                                    log.rmiValue < 10.0 -> "Fair"
                                    log.rmiValue < 40.0 -> "Good"
                                    log.rmiValue < 100.0 -> "Very good"
                                    log.rmiValue < 400.0 -> "Extremely good"
                                    else -> "Exceptionally good"
                                }
                                Text("Classification: $qText | RQD equivalent: ${log.rmiUcs.toInt()}%", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        if (log.notes.isNotBlank()) {
                            Text("Field Comments: ${log.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RmrPanel(viewModel: GeologyViewModel, projectId: Int) {
    var locName by remember { mutableStateOf("Adit 04 Face") }
    var locError by remember { mutableStateOf(false) }
    var rmrUcs by remember { mutableStateOf("7.0") } // Score of Intact UCS (0-15)
    var rmrRqd by remember { mutableStateOf("13.0") } // Score of RQD (3-20)
    var rmrSpacing by remember { mutableStateOf("10.0") } // Score of spacing (5-20)
    var rmrCondition by remember { mutableStateOf("20.0") } // Score of conditions (0-30)
    var rmrWater by remember { mutableStateOf("10.0") } // Score of groundwater (0-15)
    var rmrAdjustment by remember { mutableStateOf("-5.0") } // Discontinuity adjustment (-12-0)
    var notes by remember { mutableStateOf("Moderate phyllite jointing. Slicken-sided surface.") }

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

        rockClass = when {
            score >= 81 -> "Class I: Very Good Rock"
            score >= 61 -> "Class II: Good Rock"
            score >= 41 -> "Class III: Fair Rock"
            score >= 21 -> "Class IV: Poor Rock"
            else -> "Class V: Very Poor Rock"
        }
    }

    LaunchedEffect(rmrUcs, rmrRqd, rmrSpacing, rmrCondition, rmrWater, rmrAdjustment) {
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
            val standUpText = when {
                standUpHours > 24 * 365 -> "${String.format(Locale.US, "%.1f", standUpHours / (24 * 365))} Years"
                standUpHours > 24 * 30 -> "${String.format(Locale.US, "%.1f", standUpHours / (24 * 30))} Months"
                standUpHours > 24 * 7 -> "${String.format(Locale.US, "%.1f", standUpHours / (24 * 7))} Weeks"
                standUpHours > 24 -> "${String.format(Locale.US, "%.1f", standUpHours / 24)} Days"
                standUpHours > 1.0 -> "${String.format(Locale.US, "%.1f", standUpHours)} Hours"
                else -> "${String.format(Locale.US, "%.0f", (standUpHours * 60).coerceAtLeast(1.0))} Mins"
            }

            val rockLoadKpa = ((100.0 - finalScore) / 100.0).coerceAtLeast(0.0) * 26.5 * rmrSpan

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bieniawski RMR Score Card (1989 Std.)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text(rockClass, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = when {
                                finalScore >= 81 -> Color(0xFF2E7D32)
                                finalScore >= 61 -> Color(0xFF00ACC1)
                                finalScore >= 41 -> Color(0xFFFBC02D)
                                finalScore >= 21 -> Color(0xFFEF6C00)
                                else -> Color(0xFFC62828)
                            })
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(27.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text("${finalScore.toInt()}", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = (finalScore / 100.0).toFloat().coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = when {
                                finalScore >= 81 -> Color(0xFF2E7D32)
                                finalScore >= 61 -> Color(0xFF00ACC1)
                                finalScore >= 41 -> Color(0xFFFBC02D)
                                finalScore >= 21 -> Color(0xFFEF6C00)
                                else -> Color(0xFFC62828)
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("0 (Very Poor)", fontSize = 8.sp, color = Color.Gray)
                            Text("Fair", fontSize = 8.sp, color = Color.Gray)
                            Text("100 (Very Good)", fontSize = 8.sp, color = Color.Gray)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Text("Estimated Analytical Rock Mass Parameters:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)

                    // Param grid row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("COHESION (C)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Text("${cohesionKpa.toInt()} kPa", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Calculated average cohesion limit of jointed block mass.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 11.sp)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("FRICTION ANGLE (Φ)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Text("${String.format(Locale.US, "%.1f", frictionDeg)}°", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Estimated shear friction angle resistance limit.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 11.sp)
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
                            Text("Structural Span / Width: ${String.format(Locale.US, "%.1f", rmrSpan)} m", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = rmrSpan,
                                onValueChange = { rmrSpan = it },
                                valueRange = 1.0f..30.0f,
                                modifier = Modifier.height(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(0.7f)) {
                            Text("Span Width Reference", fontSize = 8.sp, color = Color.Gray)
                            Text("1-30m tunnel/room bounds", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Param grid row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("STAND-UP TIME", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(standUpText, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (standUpHours < 24) Color(0xFFC62828) else MaterialTheme.colorScheme.primary)
                                Text("Lauffer standard exposure limit before ceiling failure.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 11.sp)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("ULTIMATE ROOF LOAD (Pr)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("${String.format(Locale.US, "%.1f", rockLoadKpa)} kPa", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Equivalent crown pressure load sustained by support.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Visual Rock Joints & Spacing Simulation Preview:",
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
                                "Joint set count: ${(30.0 / spacingVal * 4.0).toInt().coerceIn(3, 30)} sets | Spacing index: ${spacingVal.toInt()}",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (waterVal < 15.0) {
                                Text("Groundwater: Seeping/Water Flow", color = Color(0xFF90CAF9), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Groundwater: Completely Dry", color = Color.White, fontSize = 8.sp)
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
                label = { Text("Location Name / Station") },
                isError = locError,
                supportingText = {
                    if (locError) {
                        Text("Location Name is required!", color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text("Bieniawski RMR Geotechnical Variables (Input Scores)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rmrUcs,
                    onValueChange = { rmrUcs = it },
                    label = { Text("1. UCS Strength (0-15)", fontSize = 8.sp) },
                    isError = isUcsError,
                    supportingText = {
                        if (isUcsError) Text("Range: 0-15", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rmrRqd,
                    onValueChange = { rmrRqd = it },
                    label = { Text("2. RQD Score (3-20)", fontSize = 8.sp) },
                    isError = isRqdError,
                    supportingText = {
                        if (isRqdError) Text("Range: 3-20", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rmrSpacing,
                    onValueChange = { rmrSpacing = it },
                    label = { Text("3. Spacing (5-20)", fontSize = 8.sp) },
                    isError = isSpacingError,
                    supportingText = {
                        if (isSpacingError) Text("Range: 5-20", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rmrCondition,
                    onValueChange = { rmrCondition = it },
                    label = { Text("4. Condition (0-30)", fontSize = 8.sp) },
                    isError = isCondError,
                    supportingText = {
                        if (isCondError) Text("Range: 0-30", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rmrWater,
                    onValueChange = { rmrWater = it },
                    label = { Text("5. Groundwater (0-15)", fontSize = 8.sp) },
                    isError = isWaterError,
                    supportingText = {
                        if (isWaterError) Text("Range: 0-15", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rmrAdjustment,
                    onValueChange = { rmrAdjustment = it },
                    label = { Text("6. Orientation Adj (-12-0)", fontSize = 8.sp) },
                    isError = isAdjError,
                    supportingText = {
                        if (isAdjError) Text("Range: -12 to 0", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Evaluation Notes / Joint set descriptions") },
                modifier = Modifier.fillMaxWidth()
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
                enabled = locName.isNotBlank() && !isUcsError && !isRqdError && !isSpacingError && !isCondError && !isWaterError && !isAdjError
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add RMR Log to Journal")
            }
        }
    }
}

@Composable
fun HoekBrownPanel(viewModel: GeologyViewModel, projectId: Int) {
    var locName by remember { mutableStateOf("Face Slope A") }
    var locError by remember { mutableStateOf(false) }
    var hbSigci by remember { mutableStateOf("80.0") } // Uniaxial compressive strength of intact rock
    var hbGsi by remember { mutableStateOf("60.0") } // GSI
    var hbMi by remember { mutableStateOf("15.0") } // mi parameter
    var hbD by remember { mutableStateOf("0.2") } // Disturbance factor
    var notes by remember { mutableStateOf("Medium strength granite slab. Slightly disturbed blast.") }

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

    LaunchedEffect(hbSigci, hbGsi, hbMi, hbD) {
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
                    Text("Generalized Hoek-Brown Geotechnical Outputs", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    
                    // Parameters grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("mb (mass multiplier):", fontSize = 9.sp, color = Color.Gray)
                            Text(String.format(Locale.US, "%.4f", computedMb), fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("s (joint parameter):", fontSize = 9.sp, color = Color.Gray)
                            Text(String.format(Locale.US, "%.6f", computedS), fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("a (slope coefficient):", fontSize = 9.sp, color = Color.Gray)
                            Text(String.format(Locale.US, "%.4f", computedA), fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Main Rock Strengths Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("MASS COMPRESSIVE (σci)", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("${String.format(Locale.US, "%.2f", computedSigmaci)} MPa", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
                                Text("Uniaxial compressive strength of jointed mass.", fontSize = 7.5.sp, color = Color.DarkGray, lineHeight = 9.sp)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("GLOBAL STRENGTH (σcm)", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("${String.format(Locale.US, "%.2f", computedSgcmGlobal)} MPa", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                Text("Equivalent rock mass strength (Hoek 2002).", fontSize = 7.5.sp, color = Color.DarkGray, lineHeight = 9.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("MASS TENSILE STRENGTH", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("${String.format(Locale.US, "%.3f", computedTensile)} MPa", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828))
                                Text("Tensile failure boundary estimation.", fontSize = 7.5.sp, color = Color.DarkGray, lineHeight = 9.sp)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("DEFORMATION MODULUS (Erm)", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("${String.format(Locale.US, "%.2f", computedErm)} GPa", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                                Text("Diederichs 2006 mass elasticity index.", fontSize = 7.5.sp, color = Color.DarkGray, lineHeight = 9.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Failure Envelope Curve Plot (Principal Stresses σ₁ vs σ₃):", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                            text = "Failed Plastic State",
                            color = Color(0xFFC62828).copy(alpha = 0.75f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                        )

                        Text(
                            text = "Sustained Elastic Zone",
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
                        Text("Location Name is required!", color = MaterialTheme.colorScheme.error)
                    }
                },
                label = { Text("Location Name / Slope Sector") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hbSigci,
                    onValueChange = { hbSigci = it },
                    label = { Text("Intact UCS (σci) [MPa]", fontSize = 8.sp) },
                    isError = isSigciError,
                    supportingText = {
                        if (isSigciError) Text("Must be > 0", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = hbGsi,
                    onValueChange = { hbGsi = it },
                    label = { Text("GSI rating (0-100)", fontSize = 8.sp) },
                    isError = isGsiError,
                    supportingText = {
                        if (isGsiError) Text("Range: 0-100", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hbMi,
                    onValueChange = { hbMi = it },
                    label = { Text("Constant (mi) (e.g. 5-35)", fontSize = 8.sp) },
                    isError = isMiError,
                    supportingText = {
                        if (isMiError) Text("Must be > 0", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = hbD,
                    onValueChange = { hbD = it },
                    label = { Text("Disturbance D (0-1.0)", fontSize = 8.sp) },
                    isError = isDError,
                    supportingText = {
                        if (isDError) Text("Range: 0 to 1", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Hoek-Brown Solver comments") },
                modifier = Modifier.fillMaxWidth()
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
                enabled = locName.isNotBlank() && !isSigciError && !isGsiError && !isMiError && !isDError
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Lock Hoek-Brown Parameters")
            }
        }
    }
}

@Composable
fun MrmrRmiPanel(viewModel: GeologyViewModel, projectId: Int) {
    var locName by remember { mutableStateOf("Stope 102 Crown") }
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
                finalMrmr >= 81 -> "Mining I: Extremely Stable"
                finalMrmr >= 61 -> "Mining II: Good Ground"
                finalMrmr >= 41 -> "Mining III: Stable with Bolting"
                finalMrmr >= 21 -> "Mining IV: Heavy Squeezing"
                else -> "Mining V: Extreme Flowing"
            }
        }

        if (!isRmiUcsError && !isRmiJpError) {
            val jpVal = rmiJpValVal.coerceAtLeast(0.0001)
            rmiValue = 0.04 * rmiUcsValVal * Math.pow(jpVal, 0.4)
        }
    }

    LaunchedEffect(rmrBasic, weatheringAdj, jointOrientationAdj, blastAdj, rmiUcs, rmiJp) {
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
                rmiValue >= 10.0 -> "Extremely Strong Massive Blocks"
                rmiValue >= 1.0 -> "Strong / Blocky Rock Mass"
                rmiValue >= 0.1 -> "Moderately Jointed / Very Blocky"
                rmiValue >= 0.01 -> "Partially Crushed / Heavily Jointed"
                else -> "Extremely Crushed / Soil-Like"
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
                            Text("Modified MRMR (Laubscher) & RMi Core Outputs", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text("MRMR Mining Rating: ${String.format(Locale.US, "%.2f", mrmrScore)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFFE65100))
                            Text(mrmrClass, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("RMi INDEX", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(String.format(Locale.US, "%.4f", rmiValue), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00ACC1))
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Engineering Calculations Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("STABLE HYDRAULIC RADIUS (HR)", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("${String.format(Locale.US, "%.2f", stableHR)} m", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE65100))
                                Text("Max undercut area ratio for stable roof.", fontSize = 7.5.sp, color = Color.DarkGray, lineHeight = 9.sp)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("MAX EXCAVATION SPAN", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("${String.format(Locale.US, "%.1f", stableSpan)} m", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                Text("Suggested stable unsupported stope diameter.", fontSize = 7.5.sp, color = Color.DarkGray, lineHeight = 9.sp)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PALMSTROM RMi BLOCK STRUCTURAL CLASSIFICATION:", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(rmiBlockClass, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00ACC1))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Block Caving Cavability & Joint Blocks Size Simulation:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                                "Joint block parameter: ${String.format(Locale.US, "%.3f", rmiJpValVal)}",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (mrmrScore < 50.0) {
                                Text("Undercut Level: Active Gravity Cave Propagation", color = Color(0xFFFFB74D), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Undercut Level: Completely Stable", color = Color(0xFF81C784), fontSize = 8.sp, fontWeight = FontWeight.Bold)
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
                        Text("Location Name is required!", color = MaterialTheme.colorScheme.error)
                    }
                },
                label = { Text("Mining Segment / Face Location") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rmrBasic,
                    onValueChange = { rmrBasic = it },
                    label = { Text("Basic RMR Score (0-100)", fontSize = 8.sp) },
                    isError = isBasicError,
                    supportingText = {
                        if (isBasicError) Text("Range: 1 to 100", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weatheringAdj,
                    onValueChange = { weatheringAdj = it },
                    label = { Text("Weathering Factor (0.5-1.0)", fontSize = 8.sp) },
                    isError = isWeatherError,
                    supportingText = {
                        if (isWeatherError) Text("Range: 0.5 to 1.0", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = jointOrientationAdj,
                    onValueChange = { jointOrientationAdj = it },
                    label = { Text("Structural Joint Adjust (0.7-1.0)", fontSize = 8.sp) },
                    isError = isJointError,
                    supportingText = {
                        if (isJointError) Text("Range: 0.7 to 1.0", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = blastAdj,
                    onValueChange = { blastAdj = it },
                    label = { Text("Explosive/Blast Adjust", fontSize = 8.sp) },
                    isError = isBlastError,
                    supportingText = {
                        if (isBlastError) Text("Range: 0.5 to 1.0", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rmiUcs,
                    onValueChange = { rmiUcs = it },
                    label = { Text("RMi Intact UCS [MPa]", fontSize = 8.sp) },
                    isError = isRmiUcsError,
                    supportingText = {
                        if (isRmiUcsError) Text("Must be > 0", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rmiJp,
                    onValueChange = { rmiJp = it },
                    label = { Text("Joint parameter (Jp)", fontSize = 8.sp) },
                    isError = isRmiJpError,
                    supportingText = {
                        if (isRmiJpError) Text("Must be > 0", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
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
                        // Save unified MRMR & RMi log
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
                                notes = "Unified Mine Block evaluation with RMi rock index indexation."
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = locName.isNotBlank() && !isBasicError && !isWeatherError && !isJointError && !isBlastError && !isRmiUcsError && !isRmiJpError
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Commit MRMR & RMi Evaluations")
            }
        }
    }
}

@Composable
fun QSystemPanel(
    viewModel: GeologyViewModel,
    projectId: Int
) {
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
        qScore < 0.01 -> "Exceptionally poor"
        qScore < 0.1 -> "Extremely poor"
        qScore < 1.0 -> "Very poor"
        qScore < 4.0 -> "Poor"
        qScore < 10.0 -> "Fair"
        qScore < 40.0 -> "Good"
        qScore < 100.0 -> "Very good"
        qScore < 400.0 -> "Extremely good"
        else -> "Exceptionally good"
    }

    val jnOptions = listOf(
        0.5 to "Massive, no or few joints (Jn = 0.5)",
        1.0 to "One joint set (Jn = 1.0)",
        2.0 to "One joint set plus random (Jn = 2.0)",
        4.0 to "Two joint sets (Jn = 4.0)",
        6.0 to "Two joint sets plus random (Jn = 6.0)",
        9.0 to "Three joint sets (Jn = 9.0)",
        12.0 to "Three joint sets plus random (Jn = 12.0)",
        15.0 to "Four or more joint sets (Jn = 15.0)",
        20.0 to "Crushed rock, earth-like (Jn = 20.0)"
    )

    val jrOptions = listOf(
        4.0 to "Discontinuous joints (Jr = 4.0)",
        3.0 to "Rough or undulating, rustic (Jr = 3.0)",
        2.0 to "Smooth, undulating (Jr = 2.0)",
        1.5 to "Rough or irregular, planar (Jr = 1.5)",
        1.0 to "Smooth, planar (Jr = 1.0)",
        0.5 to "Slickensided, planar (Jr = 0.5)"
    )

    val jaOptions = listOf(
        0.75 to "Tightly healed, hard filler (Ja = 0.75)",
        1.0 to "Unaltered walls, surface staining only (Ja = 1.0)",
        2.0 to "Slightly altered joint walls, sandy filler (Ja = 2.0)",
        3.0 to "Silty- or sandy-clay coatings (Ja = 3.0)",
        4.0 to "Softening clay mineral coatings (Ja = 4.0)",
        8.0 to "Swelling clay filler e.g. bentonite (Ja = 8.0)"
    )

    val jwOptions = listOf(
        1.0 to "Dry, minor inflow (Jw = 1.0)",
        0.66 to "Medium inflow, transient pressure (Jw = 0.66)",
        0.5 to "Large inflow, high pressure in open joints (Jw = 0.5)",
        0.33 to "Large inflow with washouts of filler (Jw = 0.33)",
        0.1 to "Exceptionally high inflow (Jw = 0.1)"
    )

    val srfOptions = listOf(
        1.0 to "Medium depth overburden, stable stress (SRF = 1.0)",
        2.5 to "High stress, tight stable structure (SRF = 2.5)",
        5.0 to "Mild squeezing, plastic clay shear zones (SRF = 5.0)",
        10.0 to "Heavy squeezing rock, large depth (SRF = 10.0)",
        20.0 to "Swelling clay, active hydration (SRF = 20.0)"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Barton's Q-System Geotechnical Calculator", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Computational Formula: Q = (RQD/Jn) * (Jr/Ja) * (Jw/SRF)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Field tool for structural rock mass quality classification to assess borehole core and underground mine stability.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            OutlinedTextField(
                value = locName,
                onValueChange = { locName = it; if (it.isNotBlank()) locError = false },
                label = { Text("Location Name (e.g. Tunnel Portal, East Pit wall)") },
                modifier = Modifier.fillMaxWidth(),
                isError = locError
            )
            if (locError) {
                Text("Location name is required to save the log.", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
            }
        }

        item {
            OutlinedTextField(
                value = rqdStr,
                onValueChange = { rqdStr = it },
                label = { Text("Rock Quality Designation, RQD (0 - 100)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text("Joint Set Number (Jn):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
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
            Text("Joint Roughness (Jr):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
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
            Text("Joint Alteration & Friction (Ja):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
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
            Text("Joint Water Factor (Jw):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
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
            Text("Stress Reduction Factor (SRF):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Calculated Structural Q-Value:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
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
                        text = "Rock Quality Class: $qClass",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "🔬 Scientific Tunnel Support Simulator (Barton & Grimstad Standard)",
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
                            Text("Tunnel Span / Width: ${String.format(Locale.US, "%.1f", tunnelSpan)} m", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Slider(
                                value = tunnelSpan,
                                onValueChange = { tunnelSpan = it },
                                valueRange = 2.0f..25.0f,
                                modifier = Modifier.height(20.dp)
                            )
                        }

                        // ESR Selector
                        Column(modifier = Modifier.weight(0.9f)) {
                            Text("ESR Ratio:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            var esrExpanded by remember { mutableStateOf(false) }
                            val esrOptions = listOf(
                                0.8f to "0.8 Tunnels",
                                1.0f to "1.0 Rail/Road",
                                1.3f to "1.3 Conduits",
                                1.6f to "1.6 Mining Drifts",
                                3.0f to "3.0 Temp Shafts"
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
                        qScore < 1.0 -> 60f + 20f * (1f - Math.log10(qScore * 100).toFloat())
                        qScore < 10.0 -> 30f + 15f * (1f - Math.log10(qScore * 10).toFloat())
                        qScore < 40.0 -> 0f + 10f * (1f - Math.log10(qScore / 10).toFloat())
                        else -> 0f
                    }
                    val sizeFactor = Math.pow(deValue / 6.0, 0.25).toFloat().coerceIn(0.5f, 2.0f)
                    val shotcreteThicknessMm = (baseShotcreteThickness * sizeFactor).coerceIn(0f, 300f)

                    // Support Class Category designation (Grimstad 9 Classes)
                    val (supportClassStr, supportClassDesc) = when {
                        qScore < 0.001 -> "Category 9" to "Cast concrete lining or extremely thick reinforced ribs (>200mm shotcrete)."
                        qScore < 0.01 -> "Category 8" to "Systematic bolting with >150mm fiber-reinforced shotcrete and heavy steel arches."
                        qScore < 0.1 -> "Category 7" to "Systematic bolting with 120-150mm fiber-reinforced shotcrete and reinforced ribs (RRS)."
                        qScore < 1.0 -> "Category 6" to "Systematic bolting with 90-120mm fiber-reinforced shotcrete."
                        qScore < 4.0 -> "Category 5" to "Systematic bolting with 50-90mm fiber-reinforced shotcrete."
                        qScore < 10.0 -> "Category 4" to "Systematic bolting with 50mm unreinforced shotcrete."
                        qScore < 40.0 -> "Category 3" to "Systematic bolting, spot shotcrete as needed."
                        qScore < 1000.0 -> "Category 2" to "Spot bolting, usually unsupported walls."
                        else -> "Category 1" to "Completely unsupported rock mass."
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
                                Text("SUPPORT CLASS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                                Text("BOLTING SCHEME", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("S: ${String.format(Locale.US, "%.2f", finalBoltSpacing)}m | L: ${String.format(Locale.US, "%.2f", boltLength)}m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Suggested systematic spacing and length of structural rock bolts.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 11.sp)
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
                                Text("FIBER SHOTCRETE (S(fr))", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(if (shotcreteThicknessMm > 0) "${shotcreteThicknessMm.toInt()} mm" else "None Required", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (shotcreteThicknessMm > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface)
                                Text("Fiber-reinforced concrete thickness computed via Grimstad criteria.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 11.sp)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("SUPPORT PRESSURE (Pr)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("${String.format(Locale.US, "%.1f", pRoofKpa)} kPa", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828))
                                Text("Equivalent to ${String.format(Locale.US, "%.3f", pRoofKgCm2)} kg/cm² active reinforcement load.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 11.sp)
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
                                Text("MAX UNSUPPORTED SPAN", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("${String.format(Locale.US, "%.2f", maxUnsupportedSpan)} meters", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Text(
                                text = "Equivalent Dimension (De): ${String.format(Locale.US, "%.2f", deValue)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tunnel Support Cross Section Simulation Pattern:",
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
                            Text("Tunnel Span: ${String.format(Locale.US, "%.1f", tunnelSpan)}m", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            if (shotcreteThicknessMm > 0) {
                                Text("Shotcrete: ${shotcreteThicknessMm.toInt()}mm", color = Color(0xFF81C784), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Shotcrete: None", color = Color.White, fontSize = 8.sp)
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
                            Text("Bolt Length: ${String.format(Locale.US, "%.2f", boltLength)}m", color = Color(0xFFFFE082), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("Bolt Spacing: ${String.format(Locale.US, "%.2f", finalBoltSpacing)}m", color = Color(0xFFFFB74D), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Text(
                            text = "Geotechnical Support Design Cross-Section (ESR = ${String.format(Locale.US, "%.1f", esrSelected)})",
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
                label = { Text("Field Notes / Structural joint comments") },
                modifier = Modifier.fillMaxWidth()
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
                        Toast.makeText(viewModel.getApplication(), "Geotechnical Barton Q Log committed successfully!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = locName.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Commit Q-System Log")
            }
        }
    }
}

@Composable
fun KinematicStereonetPanel(
    viewModel: GeologyViewModel,
    projectId: Int
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

