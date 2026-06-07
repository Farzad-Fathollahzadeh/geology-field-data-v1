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
                text = "مکانیک سنگ و رده‌بندی ژئوتکنیکی",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "یک پروژه اکتشافی یا مهندسی را جهت محاسبات ساختاری RMR، پارامترهای توده‌سنگ Hoek-Brown، رده‌بندی معدنی MRMR و شاخص RMi توده‌سنگ انتخاب کنید.",
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
                Text("تعریف پروژه جدید ژئوتکنیک")
            }

            Divider()

            Text(
                text = "پروژه‌های در دسترس:",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )

            if (projectList.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("هیچ پروژه‌ای تعریف نشده است.", color = Color.Gray, fontSize = 13.sp)
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
                                    Text(project.description ?: "پروژه عمومی ژئوتکنیک", fontSize = 12.sp, color = Color.Gray)
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
                title = { Text("تعریف پروژه ژئوتکنیک جدید") },
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
    var selectedTab by remember { mutableStateOf(0) } // 0 = Saved Logs, 1 = RMR Calculator, 2 = Hoek-Brown Solver, 3 = MRMR & RMi
    
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
                Text("Geotechnical Evaluator HUD", fontSize = 12.sp, color = Color.Gray)
            }
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Back")
            }
        }

        // Module selectors
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Log Journal", fontSize = 11.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Bieniawski RMR", fontSize = 11.sp) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Hoek-Brown", fontSize = 11.sp) })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("MRMR & RMi", fontSize = 11.sp) })
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Barton Q-System", fontSize = 11.sp) })
        }

        when (selectedTab) {
            0 -> LogJournalPanel(viewModel, projectId, evaluations)
            1 -> RmrPanel(viewModel, projectId)
            2 -> HoekBrownPanel(viewModel, projectId)
            3 -> MrmrRmiPanel(viewModel, projectId)
            4 -> QSystemPanel(viewModel, projectId)
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
                                    log.rmiValue < 0.01 -> "Exceptionally poor (سنگ فوق‌العاده ضعیف)"
                                    log.rmiValue < 0.1 -> "Extremely poor (سنگ بسیار بسیار ضعیف)"
                                    log.rmiValue < 1.0 -> "Very poor (سنگ بسیار ضعیف)"
                                    log.rmiValue < 4.0 -> "Poor (سنگ ضعیف)"
                                    log.rmiValue < 10.0 -> "Fair (سنگ متوسط)"
                                    log.rmiValue < 40.0 -> "Good (سنگ خوب)"
                                    log.rmiValue < 100.0 -> "Very good (سنگ بسیار خوب)"
                                    log.rmiValue < 400.0 -> "Extremely good (سنگ فوق‌العاده خوب)"
                                    else -> "Exceptionally good (سنگ استثنایی)"
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
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bieniawski RMR Score Card", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text(rockClass, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = when {
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
                                .size(60.dp)
                                .clip(RoundedCornerShape(30.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text("${finalScore.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = (finalScore / 100.0).toFloat().coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                    Text("Generalized Hoek-Brown Numerical Outputs", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    Text("mb (mass parameter): ${String.format(Locale.US, "%.4f", computedMb)}", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text("s (joints parameter):  ${String.format(Locale.US, "%.6f", computedS)}", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text("a (structural shape): ${String.format(Locale.US, "%.4f", computedA)}", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Mass UCS Strength (σcm): ${String.format(Locale.US, "%.2f", computedSigmaci)} MPa",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
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
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                    Text("Laubscher Modified MRMR Mining Outputs", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFE65100))
                    Spacer(Modifier.height(4.dp))
                    Text("Basic RMR Core: $rmrBasic", fontSize = 12.sp)
                    Text("MRMR Mining Class: $mrmrClass", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("MRMR Mining Rating: ${String.format(Locale.US, "%.2f", mrmrScore)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
            ) {
                Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                    Text("Palmstrom RMi Standard Rock Index", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF00ACC1))
                    Spacer(Modifier.height(4.dp))
                    Text("UCS: $rmiUcs MPa | Joint Factor Jp: $rmiJp", fontSize = 12.sp)
                    Text("RMi Index: ${String.format(Locale.US, "%.5f", rmiValue)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00ACC1))
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

    val rqd = rqdStr.toDoubleOrNull() ?: 0.0
    val qScore = (if (rqd <= 0.0) 10.0 else rqd) / jnSelected * (jrSelected / jaSelected) * (jwSelected / srfSelected)

    val qClass = when {
        qScore < 0.01 -> "Exceptionally poor (سنگ فوق‌العاده ضعیف)"
        qScore < 0.1 -> "Extremely poor (سنگ بسیار بسیار ضعیف)"
        qScore < 1.0 -> "Very poor (سنگ بسیار ضعیف)"
        qScore < 4.0 -> "Poor (سنگ ضعیف)"
        qScore < 10.0 -> "Fair (سنگ متوسط)"
        qScore < 40.0 -> "Good (سنگ خوب)"
        qScore < 100.0 -> "Very good (سنگ بسیار خوب)"
        qScore < 400.0 -> "Extremely good (سنگ فوق‌العاده خوب)"
        else -> "Exceptionally good (سنگ استثنایی)"
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
                    Text("فرمول محاسباتی: Q = (RQD/Jn) * (Jr/Ja) * (Jw/SRF)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("نرم‌افزار صحرایی طبقه‌بندی کیفی توده‌سنگ‌های ساختاری به منظور ارزیابی پایداری مغزه‌ها و حفریات معدنی.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    val supportAdvice = when {
                        qScore < 0.1 -> "Extremely Poor Rock: Requires urgent heavy systematic steel sets, thick fiber-reinforced shotcrete (>150mm), and immediate structural ribs."
                        qScore < 1.0 -> "Very Poor Rock: Systematic rock bolting (1.5m spacing) with 100-150mm of reinforced shotcrete is recommended."
                        qScore < 4.0 -> "Poor Rock Mass: Systematic pattern rock bolting (1.5-2.0m spacing) with 50-100mm of shotcrete is advised."
                        qScore < 10.0 -> "Fair Rock Quality: Spaced pattern bolting is suggested. Spot shotcrete (50mm) as local condition requires."
                        qScore < 40.0 -> "Good Rock Mass: Spot/local key-block bolting is recommended where joint sets intersect. Shotcrete generally unnecessary."
                        else -> "Excellent Rock Mass: Normally unsupported (no major structural reinforcement required for standard mining spans)."
                    }
                    Text(
                        text = "Tunnel Support Advisory (سیستم نگهداری پیشنهادی):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = supportAdvice,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
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

