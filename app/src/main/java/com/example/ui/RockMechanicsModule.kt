package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val projectList by viewModel.projects.collectAsStateWithLifecycle()

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
                text = "Rock Mechanics & Geotech Ratings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Select an exploration project to manage spatial RMR, Hoek-Brown state, MRMR mining scales, and RMi indexes.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Button(
                onClick = { showCreateProjDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create New Project")
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
                    Text("No projects yet.")
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
                                    viewModel.setActiveProject(project.id)
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
                title = { Text("Create Geotech Project") },
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
                                viewModel.createProject(projName, projDesc)
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
            projectId = activeProject!!.id,
            projectName = activeProject!!.name,
            modifier = modifier
        )
    }
}

@Composable
fun GeotechnicalWorkspace(
    viewModel: GeologyViewModel,
    projectId: Int,
    projectName: String,
    modifier: Modifier = Modifier
) {
    val evaluations by viewModel.getRockMechanicsEvaluationsForProject(projectId).collectAsStateWithLifecycle()
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
                onClick = { viewModel.setActiveProject(-1) },
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
        }

        when (selectedTab) {
            0 -> LogJournalPanel(viewModel, projectId, evaluations)
            1 -> RmrPanel(viewModel, projectId)
            2 -> HoekBrownPanel(viewModel, projectId)
            3 -> MrmrRmiPanel(viewModel, projectId)
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
            Text("No geotechnical logs submitted for this project. Use solver calculators next door to compile.", fontSize = 13.sp, color = Color.Gray)
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
                            Column {
                                Text("${log.evaluationType} Log: ${log.locationName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Recorded: ${log.evaluationDate}", fontSize = 10.sp, color = Color.Gray)
                            }
                            IconButton(onClick = { viewModel.deleteRockMechanicsEvaluation(log.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))

                        when (log.evaluationType) {
                            "RMR" -> {
                                Text("RMR Basic Score: ${log.rmrScore} (${log.rmrClass})", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                Text("RQD Rating: ${log.rmrRqdVal} | Joint Spacing: ${log.rmrSpacingVal} | Water: ${log.rmrWaterVal}", fontSize = 11.sp, color = Color.Gray)
                            }
                            "Hoek-Brown" -> {
                                Text("Intact UCS: ${log.hbSigci} MPa | Rock GSI: ${log.hbGsi}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                                Text("mb: ${String.format(Locale.US, "%.3f", log.hbMb)} | s: ${String.format(Locale.US, "%.5f", log.hbS)} | Mass UCS: ${String.format(Locale.US, "%.2f", log.hbSigmaci)} MPa", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            "MRMR" -> {
                                Text("MRMR Score: ${log.mrmrScore} (${log.mrmrClass})", fontWeight = FontWeight.SemiBold, color = Color(0xFFE65100), fontSize = 13.sp)
                                Text("Weathering Adj: ${log.mrmrWeatheringAdj} | Blast Adj: ${log.mrmrDrillingAdj}", fontSize = 11.sp, color = Color.Gray)
                            }
                            "RMi" -> {
                                Text("RMi Value: ${String.format(Locale.US, "%.4f", log.rmiValue)}", fontWeight = FontWeight.SemiBold, color = Color(0xFF00ACC1), fontSize = 13.sp)
                                Text("Joint parameter (Jp): ${log.rmiJp}", fontSize = 11.sp, color = Color.Gray)
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
    var rmrUcs by remember { mutableStateOf("7.0") } // Score of Intact UCS (0-15)
    var rmrRqd by remember { mutableStateOf("13.0") } // Score of RQD (3-20)
    var rmrSpacing by remember { mutableStateOf("10.0") } // Score of spacing (5-20)
    var rmrCondition by remember { mutableStateOf("20.0") } // Score of conditions (0-30)
    var rmrWater by remember { mutableStateOf("10.0") } // Score of groundwater (0-15)
    var rmrAdjustment by remember { mutableStateOf("-5.0") } // Discontinuity adjustment (-12-0)
    var notes by remember { mutableStateOf("Moderate phyllite jointing. Slicken-sided surface.") }

    var finalScore by remember { mutableStateOf(55.0) }
    var rockClass by remember { mutableStateOf("Class III: Fair Rock") }

    fun calculateRmr() {
        val ucs = rmrUcs.toDoubleOrNull() ?: 0.0
        val rqd = rmrRqd.toDoubleOrNull() ?: 0.0
        val sp = rmrSpacing.toDoubleOrNull() ?: 0.0
        val cond = rmrCondition.toDoubleOrNull() ?: 0.0
        val wat = rmrWater.toDoubleOrNull() ?: 0.0
        val adj = rmrAdjustment.toDoubleOrNull() ?: 0.0

        val score = (ucs + rqd + sp + cond + wat + adj).coerceIn(0.0, 100.0)
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Calculated RMR Score", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Text(rockClass, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text("${finalScore.toInt()}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            OutlinedTextField(
                value = locName,
                onValueChange = { locName = it },
                label = { Text("Location Name / Station") },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rmrRqd,
                    onValueChange = { rmrRqd = it },
                    label = { Text("2. RQD Score (3-20)", fontSize = 8.sp) },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rmrCondition,
                    onValueChange = { rmrCondition = it },
                    label = { Text("4. Condition (0-30)", fontSize = 8.sp) },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rmrAdjustment,
                    onValueChange = { rmrAdjustment = it },
                    label = { Text("6. Orientation Adj (-12-0)", fontSize = 8.sp) },
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
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    viewModel.saveRockMechanicsEvaluation(
                        RockMechanicsEvaluation(
                            projectId = projectId,
                            locationName = locName,
                            evaluationType = "RMR",
                            evaluationDate = sdf.format(Date()),
                            rmrUcsVal = rmrUcs.toDoubleOrNull() ?: 1.0,
                            rmrRqdVal = rmrRqd.toDoubleOrNull() ?: 1.0,
                            rmrSpacingVal = rmrSpacing.toDoubleOrNull() ?: 1.0,
                            rmrConditionVal = rmrCondition.toDoubleOrNull() ?: 1.0,
                            rmrWaterVal = rmrWater.toDoubleOrNull() ?: 1.0,
                            rmrAdjustmentVal = rmrAdjustment.toDoubleOrNull() ?: 0.0,
                            rmrScore = finalScore,
                            rmrClass = rockClass,
                            notes = notes
                        )
                    )
                    notes = ""
                },
                modifier = Modifier.fillMaxWidth()
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
    var hbSigci by remember { mutableStateOf("80.0") } // Uniaxial compressive strength of intact rock
    var hbGsi by remember { mutableStateOf("60.0") } // GSI
    var hbMi by remember { mutableStateOf("15.0") } // mi parameter
    var hbD by remember { mutableStateOf("0.2") } // Disturbance factor
    var notes by remember { mutableStateOf("Medium strength granite slab. Slightly disturbed blast.") }

    var computedMb by remember { mutableStateOf(0.0) }
    var computedS by remember { mutableStateOf(0.0) }
    var computedA by remember { mutableStateOf(0.5) }
    var computedSigmaci by remember { mutableStateOf(0.0) } // Mass Compressive Strength

    fun computeHoekBrown() {
        val s_ci = hbSigci.toDoubleOrNull() ?: 50.0
        val gsi = hbGsi.toDoubleOrNull() ?: 50.0
        val mi = hbMi.toDoubleOrNull() ?: 15.0
        val d = hbD.toDoubleOrNull() ?: 0.0

        val mb = mi * Math.exp((gsi - 100) / (28 - 14 * d))
        val s = Math.exp((gsi - 100) / (9 - 3 * d))
        val a = 0.5 + (1.0 / 6.0) * (Math.exp(-gsi / 15.0) - Math.exp(-20.0 / 3.0))
        val massStrength = s_ci * Math.pow(s, a)

        computedMb = mb
        computedS = s
        computedA = a
        computedSigmaci = massStrength
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
                onValueChange = { locName = it },
                label = { Text("Location Name / Slope Sector") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hbSigci,
                    onValueChange = { hbSigci = it },
                    label = { Text("Intact UCS (σci) [MPa]") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = hbGsi,
                    onValueChange = { hbGsi = it },
                    label = { Text("GSI rating (0-100)") },
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
                    label = { Text("Rock Parameter Constant (mi)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = hbD,
                    onValueChange = { hbD = it },
                    label = { Text("Disturbance Factor (0-1.0)") },
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
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    viewModel.saveRockMechanicsEvaluation(
                        RockMechanicsEvaluation(
                            projectId = projectId,
                            locationName = locName,
                            evaluationType = "Hoek-Brown",
                            evaluationDate = sdf.format(Date()),
                            hbSigci = hbSigci.toDoubleOrNull() ?: 50.0,
                            hbGsi = hbGsi.toDoubleOrNull() ?: 50.0,
                            hbMi = hbMi.toDoubleOrNull() ?: 15.0,
                            hbD = hbD.toDoubleOrNull() ?: 0.0,
                            hbMb = computedMb,
                            hbS = computedS,
                            hbA = computedA,
                            hbSigmaci = computedSigmaci,
                            notes = notes
                        )
                    )
                    notes = ""
                },
                modifier = Modifier.fillMaxWidth()
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
    var rmrBasic by remember { mutableStateOf("60.0") }
    var weatheringAdj by remember { mutableStateOf("0.9") } // factor (0.5 to 1.0)
    var jointOrientationAdj by remember { mutableStateOf("0.85") } // factor (0.7 to 1.0)
    var blastAdj by remember { mutableStateOf("0.95") } // factor
    
    var rmiUcs by remember { mutableStateOf("70.0") }
    var rmiJp by remember { mutableStateOf("0.04") } // Joint Factor

    var mrmrScore by remember { mutableStateOf(48.45) }
    var mrmrClass by remember { mutableStateOf("Class III: Fair Rock") }
    var rmiValue by remember { mutableStateOf(2.8) }

    fun calculateMrmrAndRmi() {
        val basic = rmrBasic.toDoubleOrNull() ?: 60.0
        val weather = weatheringAdj.toDoubleOrNull() ?: 1.0
        val joint = jointOrientationAdj.toDoubleOrNull() ?: 1.0
        val blast = blastAdj.toDoubleOrNull() ?: 1.0

        val finalMrmr = basic * weather * joint * blast
        mrmrScore = finalMrmr

        mrmrClass = when {
            finalMrmr >= 81 -> "Mining I: Extremely Stable"
            finalMrmr >= 61 -> "Mining II: Good Ground"
            finalMrmr >= 41 -> "Mining III: Stable with Bolting"
            finalMrmr >= 21 -> "Mining IV: Heavy Squeezing"
            else -> "Mining V: Extreme Flowing"
        }

        val ucsVal = rmiUcs.toDoubleOrNull() ?: 50.0
        val jpVal = rmiJp.toDoubleOrNull() ?: 0.1
        rmiValue = ucsVal * 0.5 * jpVal
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
                onValueChange = { locName = it },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weatheringAdj,
                    onValueChange = { weatheringAdj = it },
                    label = { Text("Weathering Factor (0.5-1.0)", fontSize = 8.sp) },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = blastAdj,
                    onValueChange = { blastAdj = it },
                    label = { Text("Explosive/Blast Adjust", fontSize = 8.sp) },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rmiJp,
                    onValueChange = { rmiJp = it },
                    label = { Text("Joint parameter (Jp)", fontSize = 8.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Button(
                onClick = {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    // Save MRMR log
                    viewModel.saveRockMechanicsEvaluation(
                        RockMechanicsEvaluation(
                            projectId = projectId,
                            locationName = locName,
                            evaluationType = "MRMR",
                            evaluationDate = sdf.format(Date()),
                            mrmrRmrBasic = rmrBasic.toDoubleOrNull() ?: 60.0,
                            mrmrWeatheringAdj = weatheringAdj.toDoubleOrNull() ?: 1.0,
                            mrmrJointOrientationAdj = jointOrientationAdj.toDoubleOrNull() ?: 1.0,
                            mrmrDrillingAdj = blastAdj.toDoubleOrNull() ?: 1.0,
                            mrmrScore = mrmrScore,
                            mrmrClass = mrmrClass,
                            notes = "RMi Palmstrom Value computed: ${String.format("%.4f", rmiValue)}"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Commit MRMR & RMi Evaluations")
            }
        }
    }
}
