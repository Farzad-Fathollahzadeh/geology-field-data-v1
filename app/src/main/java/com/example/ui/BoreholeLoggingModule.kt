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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BoreholeLoggingContent(
    viewModel: GeologyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val projectList by viewModel.projects.collectAsStateWithLifecycle()
    
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var newProjectDesc by remember { mutableStateOf("") }

    if (activeProject == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            
            Text(
                text = "Borehole Core Logging",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Select a geological exploration project to begin core logging & daily drilling reporting.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Button(
                onClick = { showCreateProjectDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("bh_create_project_btn")
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
                    Text("No projects yet. Click 'Create New Project' to start.")
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
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(project.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(project.description ?: "No description", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
        
        if (showCreateProjectDialog) {
            AlertDialog(
                onDismissRequest = { showCreateProjectDialog = false },
                title = { Text("Create New Project") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newProjectName,
                            onValueChange = { newProjectName = it },
                            label = { Text("Project Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newProjectDesc,
                            onValueChange = { newProjectDesc = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newProjectName.isNotBlank()) {
                                viewModel.createProject(newProjectName, newProjectDesc)
                                showCreateProjectDialog = false
                                newProjectName = ""
                                newProjectDesc = ""
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateProjectDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    } else {
        // Project selected, enter Borehole logging workspace
        BoreholeWorkspace(
            viewModel = viewModel,
            projectId = activeProject!!.id,
            projectName = activeProject!!.name,
            modifier = modifier
        )
    }
}

@Composable
fun BoreholeWorkspace(
    viewModel: GeologyViewModel,
    projectId: Int,
    projectName: String,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val boreholes by viewModel.getBoreholesForProject(projectId).collectAsStateWithLifecycle()
    val activeBorehole by viewModel.activeBorehole.collectAsStateWithLifecycle()
    
    var showCreateBoreholeDialog by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var elev by remember { mutableStateOf("") }
    var azimuth by remember { mutableStateOf("") }
    var dip by remember { mutableStateOf("") }
    var planned by remember { mutableStateOf("100.0") }

    if (activeBorehole == null) {
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
                    Text(
                        text = projectName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Boreholes Directory", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                TextButton(
                    onClick = { viewModel.setActiveProject(-1) }, // Go back
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Change Project")
                }
            }

            Button(
                onClick = { showCreateBoreholeDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Exploration Borehole")
            }

            Text("Active Drills & Boreholes:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

            if (boreholes.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No boreholes logged. Add one to construct core logging sheets.", fontSize = 13.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(boreholes) { bh ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setActiveBorehole(bh.id)
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(Icons.Default.SettingsInputHdmi, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    Column {
                                        Text("ID: ${bh.boreholeCode}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Azimuth: ${bh.azimuth}° | Dip: ${bh.dip}°", fontSize = 12.sp, color = Color.Gray)
                                        Text("Planned Depth: ${bh.plannedDepth}m", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.deleteBorehole(bh.id) }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreateBoreholeDialog) {
            AlertDialog(
                onDismissRequest = { showCreateBoreholeDialog = false },
                title = { Text("Add New Borehole Log") },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("Borehole Code (e.g. BH-01)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = lat,
                                    onValueChange = { lat = it },
                                    label = { Text("Latitude") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = lon,
                                    onValueChange = { lon = it },
                                    label = { Text("Longitude") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = elev,
                                    onValueChange = { elev = it },
                                    label = { Text("Elevation (m)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = planned,
                                    onValueChange = { planned = it },
                                    label = { Text("Planned Depth (m)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = azimuth,
                                    onValueChange = { azimuth = it },
                                    label = { Text("Azimuth (°)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = dip,
                                    onValueChange = { dip = it },
                                    label = { Text("Dip (°)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Button(
                                onClick = {
                                    // Generate coordinates quickly
                                    lat = "36.2134"
                                    lon = "59.5245"
                                    elev = "1150.0"
                                    azimuth = "180.0"
                                    dip = "-45.0"
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Generate Simulated Coords")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (code.isNotBlank()) {
                                val latD = lat.toDoubleOrNull() ?: 0.0
                                val lonD = lon.toDoubleOrNull() ?: 0.0
                                val elevD = elev.toDoubleOrNull() ?: 0.0
                                val azD = azimuth.toDoubleOrNull() ?: 0.0
                                val dipD = dip.toDoubleOrNull() ?: -90.0
                                val plD = planned.toDoubleOrNull() ?: 100.0
                                viewModel.saveBorehole(
                                    Borehole(
                                        projectId = projectId,
                                        boreholeCode = code,
                                        latitude = latD,
                                        longitude = lonD,
                                        elevation = elevD,
                                        azimuth = azD,
                                        dip = dipD,
                                        plannedDepth = plD
                                    )
                                ) {
                                    showCreateBoreholeDialog = false
                                    code = ""
                                    lat = ""
                                    lon = ""
                                    elev = ""
                                    azimuth = ""
                                    dip = ""
                                }
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateBoreholeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    } else {
        // Log details inside selected Borehole
        BoreholeLoggingDetailsWorkspace(
            viewModel = viewModel,
            borehole = activeBorehole!!,
            modifier = modifier
        )
    }
}

@Composable
fun BoreholeLoggingDetailsWorkspace(
    viewModel: GeologyViewModel,
    borehole: Borehole,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Daily Reports, 1 = Core Logging Runs
    
    val dailyReports by viewModel.getDailyReportsForBorehole(borehole.id).collectAsStateWithLifecycle()
    val coreRuns by viewModel.getCoreRunsForBorehole(borehole.id).collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Heading Block
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Borehole: ${borehole.boreholeCode}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Planned Depth: ${borehole.plannedDepth}m | Coords: ${String.format(Locale.US, "%.5f", borehole.latitude)}, ${String.format(Locale.US, "%.5f", borehole.longitude)}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            
            TextButton(
                onClick = { viewModel.setActiveBorehole(null) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Exit Borehole")
            }
        }

        // Subtabs
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Daily Reports", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Core Logging (Runs)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedSubTab == 0) {
            DailyReportsPanel(
                viewModel = viewModel,
                boreholeId = borehole.id,
                reports = dailyReports
            )
        } else {
            CoreRunsPanel(
                viewModel = viewModel,
                boreholeId = borehole.id,
                runs = coreRuns
            )
        }
    }
}

@Composable
fun DailyReportsPanel(
    viewModel: GeologyViewModel,
    boreholeId: Int,
    reports: List<BoreholeDailyReport>
) {
    var showAddReportDialog by remember { mutableStateOf(false) }
    var rDate by remember { mutableStateOf("") }
    var shift by remember { mutableStateOf("Day") }
    var geomProblems by remember { mutableStateOf("") }
    var drillAdvancement by remember { mutableStateOf("") }
    var stoppagesHours by remember { mutableStateOf("") }
    var shiftGeologist by remember { mutableStateOf("") }
    var rigId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Drilling Shift Daylogs", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Button(
                onClick = {
                    showAddReportDialog = true
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    rDate = sdf.format(Date())
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Log Shift", fontSize = 12.sp)
            }
        }

        if (reports.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No daily reports recorded. Keep track of shift advances.", fontSize = 13.sp, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reports) { report ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Date: ${report.reportDate} (${report.shift} Shift)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { viewModel.deleteDailyReport(report.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text("Advance:", fontSize = 10.sp, color = Color.Gray)
                                    Text("+${report.progressMeters} m", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF43A047))
                                }
                                Column {
                                    Text("Downtime:", fontSize = 10.sp, color = Color.Gray)
                                    Text("${report.stoppagesHours} hrs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                                Column {
                                    Text("Geologist:", fontSize = 10.sp, color = Color.Gray)
                                    Text(report.shiftGeologist.ifBlank { "N/A" }, fontSize = 13.sp)
                                }
                            }
                            if (report.problemsRemarks.isNotBlank()) {
                                Text("Issues/Comments: ${report.problemsRemarks}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        if (showAddReportDialog) {
            AlertDialog(
                onDismissRequest = { showAddReportDialog = false },
                title = { Text("Add Drilling Daylog Report") },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            OutlinedTextField(
                                value = rDate,
                                onValueChange = { rDate = it },
                                label = { Text("Report Date") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Shift Type:", fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = shift == "Day", onClick = { shift = "Day" })
                                        Text("Day")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = shift == "Night", onClick = { shift = "Night" })
                                        Text("Night")
                                    }
                                }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = drillAdvancement,
                                    onValueChange = { drillAdvancement = it },
                                    label = { Text("Advance (m)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = stoppagesHours,
                                    onValueChange = { stoppagesHours = it },
                                    label = { Text("Stoppage (hrs)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = shiftGeologist,
                                onValueChange = { shiftGeologist = it },
                                label = { Text("Duty Shift Geologist") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = geomProblems,
                                onValueChange = { geomProblems = it },
                                label = { Text("Remarks, Downtime reasons & Stoppages") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val adv = drillAdvancement.toDoubleOrNull() ?: 0.0
                            val stop = stoppagesHours.toDoubleOrNull() ?: 0.0
                            viewModel.saveDailyReport(
                                BoreholeDailyReport(
                                    boreholeId = boreholeId,
                                    reportDate = rDate,
                                    shift = shift,
                                    progressMeters = adv,
                                    stoppagesHours = stop,
                                    problemsRemarks = geomProblems,
                                    shiftGeologist = shiftGeologist,
                                    drillRigId = rigId
                                )
                            )
                            showAddReportDialog = false
                            drillAdvancement = ""
                            stoppagesHours = ""
                            geomProblems = ""
                        }
                    ) {
                        Text("Add Record")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddReportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun CoreRunsPanel(
    viewModel: GeologyViewModel,
    boreholeId: Int,
    runs: List<BoreholeCoreRun>
) {
    var showAddRunDialog by remember { mutableStateOf(false) }

    var runNo by remember { mutableStateOf("1") }
    var depthFrom by remember { mutableStateOf("0.0") }
    var depthTo by remember { mutableStateOf("1.5") }
    
    // Core parameters (Auto calculation system parameters)
    var runLength by remember { mutableStateOf("1.5") }
    var recoveredLength by remember { mutableStateOf("1.4") }
    var soundPiecesLength by remember { mutableStateOf("1.15") }

    var lithology by remember { mutableStateOf("Basalt") }
    var mineralization by remember { mutableStateOf("Pyrite disseminated") }
    var alteration by remember { mutableStateOf("Propylitic") }
    var fractFreq by remember { mutableStateOf("4") }
    var ucs by remember { mutableStateOf("85.0") }

    // Smart automatic calculations
    var computedRecovery by remember { mutableStateOf(93.3) }
    var computedRqd by remember { mutableStateOf(76.7) }

    fun recalculateAverages() {
        val runLenD = runLength.toDoubleOrNull() ?: 1.0
        val recLenD = recoveredLength.toDoubleOrNull() ?: 0.0
        val soundLenD = soundPiecesLength.toDoubleOrNull() ?: 0.0

        computedRecovery = if (runLenD > 0) ((recLenD / runLenD) * 100).coerceAtMost(100.0) else 0.0
        computedRqd = if (runLenD > 0) ((soundLenD / runLenD) * 100).coerceAtMost(100.0) else 0.0
    }

    // Recalculate whenever inputs change
    LaunchedEffect(runLength, recoveredLength, soundPiecesLength) {
        recalculateAverages()
    }

    // Dictionary dropdown select lists
    var showLithologyMenu by remember { mutableStateOf(false) }
    val dictLithology = listOf("Basalt", "Andesite", "Dacite", "Granite", "Diorite", "Schist", "Siltstone", "Sandstone", "Limestone")

    var showAlterationMenu by remember { mutableStateOf(false) }
    val dictAlteration = listOf("Unaltered", "Potassic", "Phyllic", "Argillic", "Propylitic", "Silicification")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Standard Core Run Logs", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (runs.isNotEmpty()) {
                    val avgRec = runs.map { it.recoveryPercent }.average()
                    val avgRqd = runs.map { it.rqdPercent }.average()
                    Text(
                        text = "Avg Rec: ${String.format(Locale.US, "%.1f", avgRec)}% | Avg RQD: ${String.format(Locale.US, "%.1f", avgRqd)}%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Button(
                onClick = {
                    showAddRunDialog = true
                    // Prefill next sequential run
                    val nextNo = (runs.maxOfOrNull { it.runNumber } ?: 0) + 1
                    runNo = nextNo.toString()
                    val lastEnd = runs.maxOfOrNull { it.depthTo } ?: 0.0
                    depthFrom = lastEnd.toString()
                    depthTo = (lastEnd + 1.5).toString()
                    runLength = "1.5"
                    recoveredLength = "1.5"
                    soundPiecesLength = "1.3"
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Core Run", fontSize = 12.sp)
            }
        }

        if (runs.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No core runs recorded. Tap 'Add Core Run' to log intervals.", fontSize = 13.sp, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(runs) { run ->
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
                                Text(
                                    text = "Run #${run.runNumber}: Interval ${run.depthFrom} - ${run.depthTo} m",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { viewModel.deleteCoreRun(run.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column {
                                    Text("Recovery", fontSize = 10.sp, color = Color.Gray)
                                    Text("${String.format(Locale.US, "%.1f", run.recoveryPercent)}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF43A047))
                                }
                                Column {
                                    Text("RQD Score", fontSize = 10.sp, color = Color.Gray)
                                    Text("${String.format(Locale.US, "%.1f", run.rqdPercent)}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                                }
                                Column {
                                    Text("Fracture/m", fontSize = 10.sp, color = Color.Gray)
                                    Text("${run.fractureFrequency}", fontSize = 13.sp)
                                }
                                Column {
                                    Text("UCS Mass", fontSize = 10.sp, color = Color.Gray)
                                    Text("${run.ucsMpa} MPa", fontSize = 13.sp)
                                }
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text(run.lithology, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                                }
                                Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                                    Text(run.alteration, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                                }
                            }

                            if (run.mineralization.isNotBlank()) {
                                Text("Mineralization: ${run.mineralization}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        if (showAddRunDialog) {
            AlertDialog(
                onDismissRequest = { showAddRunDialog = false },
                title = { Text("Log Drill Core Run") },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = runNo,
                                    onValueChange = { runNo = it },
                                    label = { Text("Run No.") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = fractalUcsStr(fractFreq),
                                    onValueChange = { fractFreq = it },
                                    label = { Text("Fractures / m") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = depthFrom,
                                    onValueChange = { depthFrom = it },
                                    label = { Text("Depth From (m)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = depthTo,
                                    onValueChange = { depthTo = it },
                                    label = { Text("Depth To (m)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Calculator HUD
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("RQD & Recovery Auto-Solver", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedTextField(
                                            value = runLength,
                                            onValueChange = { runLength = it },
                                            label = { Text("Run L (m)", fontSize = 8.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = recoveredLength,
                                            onValueChange = { recoveredLength = it },
                                            label = { Text("Rec L (m)", fontSize = 8.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = soundPiecesLength,
                                            onValueChange = { soundPiecesLength = it },
                                            label = { Text("Sound L >10cm (m)", fontSize = 8.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Recovery: ${String.format(Locale.US, "%.1f", computedRecovery)}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF43A047))
                                        Text("RQD: ${String.format(Locale.US, "%.1f", computedRqd)}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E88E5))
                                    }
                                }
                            }
                        }

                        item {
                            Box {
                                OutlinedTextField(
                                    value = lithology,
                                    onValueChange = { lithology = it },
                                    label = { Text("Lithology Type") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = { showLithologyMenu = !showLithologyMenu }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = showLithologyMenu,
                                    onDismissRequest = { showLithologyMenu = false }
                                ) {
                                    dictLithology.forEach { rock ->
                                        DropdownMenuItem(
                                            text = { Text(rock) },
                                            onClick = {
                                                lithology = rock
                                                showLithologyMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Box {
                                OutlinedTextField(
                                    value = alteration,
                                    onValueChange = { alteration = it },
                                    label = { Text("Alteration Type") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = { showAlterationMenu = !showAlterationMenu }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = showAlterationMenu,
                                    onDismissRequest = { showAlterationMenu = false }
                                ) {
                                    dictAlteration.forEach { alt ->
                                        DropdownMenuItem(
                                            text = { Text(alt) },
                                            onClick = {
                                                alteration = alt
                                                showAlterationMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = mineralization,
                                onValueChange = { mineralization = it },
                                label = { Text("Mineralization Descriptors") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = ucs,
                                onValueChange = { ucs = it },
                                label = { Text("UCS Intact Compressive (MPa)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val rNo = runNo.toIntOrNull() ?: 1
                            val dFrom = depthFrom.toDoubleOrNull() ?: 0.0
                            val dTo = depthTo.toDoubleOrNull() ?: 0.0
                            val fr = fractFreq.toIntOrNull() ?: 0
                            val uc = ucs.toDoubleOrNull() ?: 50.0
                            
                            viewModel.saveCoreRun(
                                BoreholeCoreRun(
                                    boreholeId = boreholeId,
                                    runNumber = rNo,
                                    depthFrom = dFrom,
                                    depthTo = dTo,
                                    recoveryPercent = computedRecovery,
                                    rqdPercent = computedRqd,
                                    lithology = lithology,
                                    alteration = alteration,
                                    mineralization = mineralization,
                                    fractureFrequency = fr,
                                    ucsMpa = uc
                                )
                            )
                            showAddRunDialog = false
                            mineralization = ""
                        }
                    ) {
                        Text("Log Run")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddRunDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun fractalUcsStr(s: String): String {
    return s.filter { it.isDigit() }
}
