package com.example.ui

import android.content.Context
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.UtmConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    val globalActiveProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val projectList by viewModel.projects.collectAsStateWithLifecycle()
    
    // Decoupled tab-specific project selection to avoid disrupting other workspaces
    var localSelectedProjectId by remember { mutableStateOf<Int?>(null) }
    var lastSyncedGlobalProjectId by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(globalActiveProject) {
        if (globalActiveProject != null && globalActiveProject?.id != lastSyncedGlobalProjectId) {
            localSelectedProjectId = globalActiveProject?.id
            lastSyncedGlobalProjectId = globalActiveProject?.id
        }
    }
    
    val activeProject = projectList.find { it.id == localSelectedProjectId }
    
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var newProjectDesc by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    if (activeProject == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Header Illustration Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Core Logging Workspace (گمانه ها)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Text(
                text = "Select an exploration project below to start core logging & daily shift reporting.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Button(
                onClick = { showCreateProjectDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("bh_create_project_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New Project (پروژه جدید)", fontWeight = FontWeight.SemiBold)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Exploration Projects:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        "${projectList.size} Projects",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (projectList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            "No geological projects available.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Tap 'New Project' to initialize a campaign.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(projectList) { project ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    localSelectedProjectId = project.id
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        project.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        project.description ?: "No description configured.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
        
        if (showCreateProjectDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showCreateProjectDialog = false
                    nameError = false
                },
                title = { Text("New Geological Project", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newProjectName,
                            onValueChange = { 
                                newProjectName = it
                                if (it.isNotBlank()) nameError = false
                            },
                            label = { Text("Project Title (نام پروژه)") },
                            isError = nameError,
                            supportingText = {
                                if (nameError) {
                                    Text("Project name is required", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newProjectDesc,
                            onValueChange = { newProjectDesc = it },
                            label = { Text("Description & Target Site") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newProjectName.isBlank()) {
                                nameError = true
                            } else {
                                viewModel.createProjectSilently(newProjectName, newProjectDesc) { newId ->
                                    localSelectedProjectId = newId
                                }
                                showCreateProjectDialog = false
                                newProjectName = ""
                                newProjectDesc = ""
                            }
                        }
                    ) {
                        Text("Create Project")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showCreateProjectDialog = false
                        nameError = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    } else {
        BoreholeWorkspace(
            viewModel = viewModel,
            projectId = activeProject.id,
            projectName = activeProject.name,
            onBack = { localSelectedProjectId = null },
            modifier = modifier
        )
    }
}

@Composable
fun BoreholeWorkspace(
    viewModel: GeologyViewModel,
    projectId: Int,
    projectName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val boreholesFlow = remember(projectId) { viewModel.getBoreholesForProject(projectId) }
    val boreholes by boreholesFlow.collectAsStateWithLifecycle()
    val activeBorehole by viewModel.activeBorehole.collectAsStateWithLifecycle()
    
    var showCreateBoreholeDialog by remember { mutableStateOf(false) }
    var showEditBoreholeDialog by remember { mutableStateOf(false) }
    var editingBorehole by remember { mutableStateOf<Borehole?>(null) }
    
    // Form Inputs
    var code by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var elev by remember { mutableStateOf("") }
    var azimuth by remember { mutableStateOf("") }
    var dip by remember { mutableStateOf("") }
    var planned by remember { mutableStateOf("100.0") }
    
    // Input Errors
    var codeErr by remember { mutableStateOf(false) }
    var latErr by remember { mutableStateOf(false) }
    var lonErr by remember { mutableStateOf(false) }
    var elevErr by remember { mutableStateOf(false) }
    var azErr by remember { mutableStateOf(false) }
    var dipErr by remember { mutableStateOf(false) }
    var plannedErr by remember { mutableStateOf(false) }

    fun validateInputs(): Boolean {
        codeErr = code.isBlank()
        latErr = lat.toDoubleOrNull() == null
        lonErr = lon.toDoubleOrNull() == null
        elevErr = elev.toDoubleOrNull() == null
        azErr = azimuth.toDoubleOrNull() == null || (azimuth.toDoubleOrNull() ?: -1.0) !in 0.0..360.0
        dipErr = dip.toDoubleOrNull() == null || (dip.toDoubleOrNull() ?: -99.0) !in -90.0..90.0
        plannedErr = planned.toDoubleOrNull() == null || (planned.toDoubleOrNull() ?: 0.0) <= 0.0

        return !(codeErr || latErr || lonErr || elevErr || azErr || dipErr || plannedErr)
    }

    if (activeBorehole == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Screen Header Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = projectName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Text(
                            text = "Geological Boreholes Directory",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Exit project",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Quick Actions & CSV Export Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        code = ""
                        lat = ""
                        lon = ""
                        elev = ""
                        azimuth = "0"
                        dip = "-90"
                        planned = "50"
                        codeErr = false
                        latErr = false
                        lonErr = false
                        elevErr = false
                        azErr = false
                        dipErr = false
                        plannedErr = false
                        showCreateBoreholeDialog = true 
                    },
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Borehole (گمانه جدید)", fontSize = 11.5.sp)
                }

                if (boreholes.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = { viewModel.exportBoreholesCsv(context, projectId) },
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export CSV", fontSize = 11.5.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Active Borehole Logs (لیست گمانه ها):",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        "${boreholes.size} Holes",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (boreholes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "No borehole logs saved in this project.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Tap 'Add Borehole' to configure exploration logs.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(boreholes) { bh ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setActiveBorehole(bh.id)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Layers,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = bh.boreholeCode,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "کد گمانه: ${bh.boreholeCode}",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                editingBorehole = bh
                                                code = bh.boreholeCode
                                                lat = bh.latitude.toString()
                                                lon = bh.longitude.toString()
                                                elev = bh.elevation.toString()
                                                azimuth = bh.azimuth.toString()
                                                dip = bh.dip.toString()
                                                planned = bh.plannedDepth.toString()
                                                
                                                codeErr = false
                                                latErr = false
                                                lonErr = false
                                                elevErr = false
                                                azErr = false
                                                dipErr = false
                                                plannedErr = false
                                                showEditBoreholeDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        
                                        IconButton(
                                            onClick = { viewModel.deleteBorehole(bh.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Orientation (شیب/جهت)", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            "Azimuth: ${bh.azimuth}° | Dip: ${bh.dip}°",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Planned Depth (عمق طرح)", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            "${bh.plannedDepth} meters",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Coords: ${String.format(Locale.US, "%.5f", bh.latitude)}, ${String.format(Locale.US, "%.5f", bh.longitude)}",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Elev: ${bh.elevation}m",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Create Borehole Dialog
        if (showCreateBoreholeDialog) {
            AlertDialog(
                onDismissRequest = { showCreateBoreholeDialog = false },
                title = { Text("Log New Drilling Borehole", fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("Borehole ID (کد گمانه)") },
                                placeholder = { Text("e.g. BH-01") },
                                isError = codeErr,
                                supportingText = { if (codeErr) Text("Required identifier", color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = lat,
                                    onValueChange = { lat = it },
                                    label = { Text("Latitude (عرض)") },
                                    isError = latErr,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = lon,
                                    onValueChange = { lon = it },
                                    label = { Text("Longitude (طول)") },
                                    isError = lonErr,
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
                                    isError = elevErr,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = planned,
                                    onValueChange = { planned = it },
                                    label = { Text("Target Depth (m)") },
                                    isError = plannedErr,
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
                                    label = { Text("Azimuth (0-360°)") },
                                    isError = azErr,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = dip,
                                    onValueChange = { dip = it },
                                    label = { Text("Dip Angle (-90 to +90°)") },
                                    isError = dipErr,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Button(
                                onClick = {
                                    lat = "35.6892"
                                    lon = "51.3890"
                                    elev = "1180.0"
                                    azimuth = "180.0"
                                    dip = "-90.0"
                                    planned = "120"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Auto GPS Coords (تهران)")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (validateInputs()) {
                                viewModel.saveBorehole(
                                    Borehole(
                                        projectId = projectId,
                                        boreholeCode = code,
                                        latitude = lat.toDouble(),
                                        longitude = lon.toDouble(),
                                        elevation = elev.toDouble(),
                                        azimuth = azimuth.toDouble(),
                                        dip = dip.toDouble(),
                                        plannedDepth = planned.toDouble()
                                    )
                                ) {
                                    showCreateBoreholeDialog = false
                                }
                            }
                        }
                    ) {
                        Text("Add Record")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateBoreholeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Edit Borehole Dialog
        if (showEditBoreholeDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showEditBoreholeDialog = false
                    editingBorehole = null
                },
                title = { Text("Update Borehole Parameters", fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("Borehole ID (کد گمانه)") },
                                isError = codeErr,
                                supportingText = { if (codeErr) Text("Required identifier", color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = lat,
                                    onValueChange = { lat = it },
                                    label = { Text("Latitude") },
                                    isError = latErr,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = lon,
                                    onValueChange = { lon = it },
                                    label = { Text("Longitude") },
                                    isError = lonErr,
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
                                    isError = elevErr,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = planned,
                                    onValueChange = { planned = it },
                                    label = { Text("Target Depth (m)") },
                                    isError = plannedErr,
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
                                    label = { Text("Azimuth (0-360°)") },
                                    isError = azErr,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = dip,
                                    onValueChange = { dip = it },
                                    label = { Text("Dip Angle (-90 to +90°)") },
                                    isError = dipErr,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (validateInputs() && editingBorehole != null) {
                                viewModel.saveBorehole(
                                    editingBorehole!!.copy(
                                        boreholeCode = code,
                                        latitude = lat.toDouble(),
                                        longitude = lon.toDouble(),
                                        elevation = elev.toDouble(),
                                        azimuth = azimuth.toDouble(),
                                        dip = dip.toDouble(),
                                        plannedDepth = planned.toDouble()
                                    )
                                ) {
                                    showEditBoreholeDialog = false
                                    editingBorehole = null
                                }
                            }
                        }
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showEditBoreholeDialog = false
                        editingBorehole = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    } else {
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
    var selectedSubTab by remember { mutableStateOf(0) }
    
    val dailyReportsFlow = remember(borehole.id) { viewModel.getDailyReportsForBorehole(borehole.id) }
    val dailyReports by dailyReportsFlow.collectAsStateWithLifecycle()
    val coreRunsFlow = remember(borehole.id) { viewModel.getCoreRunsForBorehole(borehole.id) }
    val coreRuns by coreRunsFlow.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Detailed Info Header Frame
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "Borehole: ${borehole.boreholeCode}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.setActiveBorehole(null) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Planned Depth: ${borehole.plannedDepth}m | Dip: ${borehole.dip}° | Azimuth: ${borehole.azimuth}°",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Modular Tabs with beautiful indicators
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Daily drilling shift logs", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Core Log Runs (کیفیت)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedSubTab == 0) {
            DailyReportsPanel(
                viewModel = viewModel,
                boreholeId = borehole.id,
                boreholeCode = borehole.boreholeCode,
                reports = dailyReports
            )
        } else {
            CoreRunsPanel(
                viewModel = viewModel,
                boreholeId = borehole.id,
                boreholeCode = borehole.boreholeCode,
                runs = coreRuns
            )
        }
    }
}

@Composable
fun DailyReportsPanel(
    viewModel: GeologyViewModel,
    boreholeId: Int,
    boreholeCode: String,
    reports: List<BoreholeDailyReport>
) {
    val context = LocalContext.current
    var showAddReportDialog by remember { mutableStateOf(false) }
    
    // Inputs
    var rDate by remember { mutableStateOf("") }
    var shift by remember { mutableStateOf("Day") }
    var geomProblems by remember { mutableStateOf("") }
    var drillAdvancement by remember { mutableStateOf("") }
    var stoppagesHours by remember { mutableStateOf("") }
    var shiftGeologist by remember { mutableStateOf("") }
    
    // Error trackers
    var advErr by remember { mutableStateOf(false) }
    var dateErr by remember { mutableStateOf(false) }
    var stopErr by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        dateErr = rDate.isBlank()
        advErr = drillAdvancement.toDoubleOrNull() == null || (drillAdvancement.toDoubleOrNull() ?: -1.0) < 0.0
        stopErr = stoppagesHours.toDoubleOrNull() == null || (stoppagesHours.toDoubleOrNull() ?: -1.0) < 0.0
        return !(dateErr || advErr || stopErr)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Daily Drilling KPI Sheets", 
                fontWeight = FontWeight.Bold, 
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (reports.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.exportBoreholeDailyReportsCsv(context, boreholeId, boreholeCode) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                Button(
                    onClick = {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        rDate = sdf.format(Date())
                        drillAdvancement = ""
                        stoppagesHours = "0.0"
                        geomProblems = ""
                        shiftGeologist = ""
                        dateErr = false
                        advErr = false
                        stopErr = false
                        showAddReportDialog = true
                    },
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Shift Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.EventNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        "No daily drilling shift logs captured.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap 'Add Shift Log' to record drill rig advance rates.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            // High value drilling KPI dashboard
            val totalM = reports.sumOf { it.progressMeters }
            val totalH = reports.sumOf { it.stoppagesHours }
            val avgSpeedMultiplier = reports.map { it.progressMeters }.average()
            val avgSpeed = if (avgSpeedMultiplier.isNaN()) 0.0 else avgSpeedMultiplier
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cumulative Drilled", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                        Text("${String.format(Locale.US, "%.1f", totalM)} m", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Avg Advance / Shift", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                        Text("${String.format(Locale.US, "%.1f", avgSpeed)} m/shift", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Rig Delays & Stops", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                        Text("${String.format(Locale.US, "%.1f", totalH)} hrs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reports) { report ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Drill Progress (پیشرفت)", fontSize = 9.sp, color = Color.Gray)
                                    Text("+${report.progressMeters} meters", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF43A047))
                                }
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text("Stoppages & Downtime (توقف)", fontSize = 9.sp, color = Color.Gray)
                                    Text("${report.stoppagesHours} hours delay", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text("Expert On Duty", fontSize = 9.sp, color = Color.Gray)
                                    Text(report.shiftGeologist.ifBlank { "Unassigned" }, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            if (report.problemsRemarks.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Problem and Remarks: ${report.problemsRemarks}", 
                                        fontSize = 11.sp, 
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddReportDialog) {
            AlertDialog(
                onDismissRequest = { showAddReportDialog = false },
                title = { Text("Log Shift Progress Sheet", fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            OutlinedTextField(
                                value = rDate,
                                onValueChange = { rDate = it },
                                label = { Text("Report Date (YYYY-MM-DD)") },
                                isError = dateErr,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Shift Mode (شیفت کاری):", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = shift == "Day", onClick = { shift = "Day" })
                                        Text("Day Row")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = shift == "Night", onClick = { shift = "Night" })
                                        Text("Night Row")
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
                                    isError = advErr,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = stoppagesHours,
                                    onValueChange = { stoppagesHours = it },
                                    label = { Text("Stoppage (hrs)") },
                                    isError = stopErr,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = shiftGeologist,
                                onValueChange = { shiftGeologist = it },
                                label = { Text("Shift Onsite Geologist (زمین شناس)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = geomProblems,
                                onValueChange = { geomProblems = it },
                                label = { Text("Technical remarks and delays") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (validate()) {
                                viewModel.saveDailyReport(
                                    BoreholeDailyReport(
                                        boreholeId = boreholeId,
                                        reportDate = rDate,
                                        shift = shift,
                                        progressMeters = drillAdvancement.toDouble(),
                                        stoppagesHours = stoppagesHours.toDouble(),
                                        problemsRemarks = geomProblems,
                                        shiftGeologist = shiftGeologist,
                                        drillRigId = ""
                                    )
                                )
                                showAddReportDialog = false
                            }
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
    boreholeCode: String,
    runs: List<BoreholeCoreRun>
) {
    val context = LocalContext.current
    var showAddRunDialog by remember { mutableStateOf(false) }

    var runNo by remember { mutableStateOf("1") }
    var depthFrom by remember { mutableStateOf("0.0") }
    var depthTo by remember { mutableStateOf("1.5") }
    
    var runLength by remember { mutableStateOf("1.5") }
    var recoveredLength by remember { mutableStateOf("1.4") }
    var soundPiecesLength by remember { mutableStateOf("1.15") }

    var lithology by remember { mutableStateOf("Basalt") }
    var mineralization by remember { mutableStateOf("Disseminated Pyrite") }
    var alteration by remember { mutableStateOf("Propylitic") }
    var fractFreq by remember { mutableStateOf("4") }
    var ucs by remember { mutableStateOf("85.0") }

    var computedRecovery by remember { mutableStateOf(93.3) }
    var computedRqd by remember { mutableStateOf(76.7) }

    // Error states
    var runNoErr by remember { mutableStateOf(false) }
    var depthFromErr by remember { mutableStateOf(false) }
    var depthToErr by remember { mutableStateOf(false) }
    var runLenErr by remember { mutableStateOf(false) }
    var recLenErr by remember { mutableStateOf(false) }
    var ucsErr by remember { mutableStateOf(false) }

    fun recalculateAverages() {
        val runLenD = runLength.toDoubleOrNull() ?: 1.0
        val recLenD = recoveredLength.toDoubleOrNull() ?: 0.0
        val soundLenD = soundPiecesLength.toDoubleOrNull() ?: 0.0

        computedRecovery = if (runLenD > 0) ((recLenD / runLenD) * 100).coerceAtMost(100.0) else 0.0
        computedRqd = if (runLenD > 0) ((soundLenD / runLenD) * 100).coerceAtMost(100.0) else 0.0
    }

    LaunchedEffect(runLength, recoveredLength, soundPiecesLength) {
        recalculateAverages()
    }

    // Dropdowns
    var showLithologyMenu by remember { mutableStateOf(false) }
    val dictLithology = listOf("Basalt", "Andesite", "Dacite", "Granite", "Diorite", "Schist", "Siltstone", "Sandstone", "Limestone")

    var showAlterationMenu by remember { mutableStateOf(false) }
    val dictAlteration = listOf("Unaltered", "Potassic", "Phyllic", "Argillic", "Propylitic", "Silicification")

    fun validate(): Boolean {
        runNoErr = runNo.toIntOrNull() == null || (runNo.toIntOrNull() ?: 0) <= 0
        depthFromErr = depthFrom.toDoubleOrNull() == null || (depthFrom.toDoubleOrNull() ?: -1.0) < 0.0
        depthToErr = depthTo.toDoubleOrNull() == null || (depthTo.toDoubleOrNull() ?: -1.0) <= (depthFrom.toDoubleOrNull() ?: 0.0)
        runLenErr = runLength.toDoubleOrNull() == null || (runLength.toDoubleOrNull() ?: 0.0) <= 0.0
        recLenErr = recoveredLength.toDoubleOrNull() == null || (recoveredLength.toDoubleOrNull() ?: 0.0) < 0.0
        ucsErr = ucs.toDoubleOrNull() == null || (ucs.toDoubleOrNull() ?: 0.0) < 0.0
        
        return !(runNoErr || depthFromErr || depthToErr || runLenErr || recLenErr || ucsErr)
    }

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
                Text(
                    "Standard Core logging intervals", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (runs.isNotEmpty()) {
                    val avgRec = runs.map { it.recoveryPercent }.average()
                    val avgRqd = runs.map { it.rqdPercent }.average()
                    Text(
                        text = "Avg Recovery: ${String.format(Locale.US, "%.1f", avgRec)}% | Avg RQD: ${String.format(Locale.US, "%.1f", avgRqd)}%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (runs.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.exportBoreholeLogsCsv(context, boreholeId, boreholeCode) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                Button(
                    onClick = {
                        val nextNo = (runs.maxOfOrNull { it.runNumber } ?: 0) + 1
                        runNo = nextNo.toString()
                        val lastEnd = runs.maxOfOrNull { it.depthTo } ?: 0.0
                        depthFrom = lastEnd.toString()
                        depthTo = (lastEnd + 1.5).toString()
                        runLength = "1.5"
                        recoveredLength = "1.5"
                        soundPiecesLength = "1.30"
                        mineralization = ""
                        
                        runNoErr = false
                        depthFromErr = false
                        depthToErr = false
                        runLenErr = false
                        recLenErr = false
                        ucsErr = false
                        
                        showAddRunDialog = true
                    },
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Run", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (runs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ListAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        "No geological core runs parsed.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Configure depth runs to compute RQD indices.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            CoreTrayVisualizer(runs = runs)

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(runs) { run ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Run #${run.runNumber}: Depth ${run.depthFrom} - ${run.depthTo} m",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "رن شماره ${run.runNumber}: از ${run.depthFrom} تا ${run.depthTo} متری",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteCoreRun(run.id) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Recovery (بازیابی)", fontSize = 9.sp, color = Color.Gray)
                                    Text("${String.format(Locale.US, "%.1f", run.recoveryPercent)}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF43A047))
                                }
                                Column {
                                    Text("RQD Score (کیفیت)", fontSize = 9.sp, color = Color.Gray)
                                    Text("${String.format(Locale.US, "%.1f", run.rqdPercent)}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                                }
                                Column {
                                    Text("Fractures/m", fontSize = 9.sp, color = Color.Gray)
                                    Text("${run.fractureFrequency}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("UCS Mass", fontSize = 9.sp, color = Color.Gray)
                                    Text("${run.ucsMpa} MPa", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            // Color-coded Progress Indices
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Recovery:", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.width(55.dp))
                                    LinearProgressIndicator(
                                        progress = (run.recoveryPercent / 100.0).toFloat().coerceIn(0f, 1f),
                                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = Color(0xFF43A047),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("RQD Index:", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.width(55.dp))
                                    LinearProgressIndicator(
                                        progress = (run.rqdPercent / 100.0).toFloat().coerceIn(0f, 1f),
                                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = Color(0xFF1E88E5),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Lithology: " + run.lithology, fontSize = 10.sp) }
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Alt: " + run.alteration, fontSize = 10.sp) }
                                )
                            }

                            if (run.mineralization.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Mineralization: ${run.mineralization}", 
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

        if (showAddRunDialog) {
            AlertDialog(
                onDismissRequest = { showAddRunDialog = false },
                title = { Text("Log Drill Core Run", fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = runNo,
                                    onValueChange = { runNo = it },
                                    label = { Text("Run No.") },
                                    isError = runNoErr,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = fractFreq,
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
                                    isError = depthFromErr,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = depthTo,
                                    onValueChange = { depthTo = it },
                                    label = { Text("Depth To (m)") },
                                    isError = depthToErr,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Calculator HUD
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("RQD & Recovery Auto-Solver", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedTextField(
                                            value = runLength,
                                            onValueChange = { runLength = it },
                                            isError = runLenErr,
                                            label = { Text("Run L (m)", fontSize = 8.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = recoveredLength,
                                            onValueChange = { recoveredLength = it },
                                            isError = recLenErr,
                                            label = { Text("Rec L (m)", fontSize = 8.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = soundPiecesLength,
                                            onValueChange = { soundPiecesLength = it },
                                            label = { Text("Sound L >10cm (m)", fontSize = 8.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1.2f)
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Calculated Recovery: ${String.format(Locale.US, "%.1f", computedRecovery)}%", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF43A047))
                                        Text("Calculated RQD: ${String.format(Locale.US, "%.1f", computedRqd)}%", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                                    }
                                }
                            }
                        }

                        item {
                            Box {
                                OutlinedTextField(
                                    value = lithology,
                                    onValueChange = { lithology = it },
                                    label = { Text("Lithology Type (نوع سنگ)") },
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
                                    label = { Text("Alteration (دگرسانی)") },
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
                                placeholder = { Text("e.g. hematite veinlets") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = ucs,
                                onValueChange = { ucs = it },
                                label = { Text("UCS Intact Compressive (MPa)") },
                                isError = ucsErr,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (validate()) {
                                viewModel.saveCoreRun(
                                    BoreholeCoreRun(
                                        boreholeId = boreholeId,
                                        runNumber = runNo.toInt(),
                                        depthFrom = depthFrom.toDouble(),
                                        depthTo = depthTo.toDouble(),
                                        recoveryPercent = computedRecovery,
                                        rqdPercent = computedRqd,
                                        lithology = lithology,
                                        alteration = alteration,
                                        mineralization = mineralization,
                                        fractureFrequency = fractFreq.toIntOrNull() ?: 0,
                                        ucsMpa = ucs.toDouble()
                                    )
                                )
                                showAddRunDialog = false
                            }
                        }
                    ) {
                        Text("Log Core Run")
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

@Composable
fun CoreTrayVisualizer(runs: List<BoreholeCoreRun>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stratigraphic Core Box (جعبه مغزه دیجیتال)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Lanes color-coded by RQD quality",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Grid Box representing wooden Core Box Tray
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEBE9).copy(alpha = 0.5f)),
                border = BorderStroke(1.5.dp, Color(0xFF795548).copy(alpha = 0.7f)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val tookRuns = runs.take(4)
                    tookRuns.forEach { run ->
                        val recoveryVal = run.recoveryPercent
                        val rqdVal = run.rqdPercent
                        val color = when {
                            rqdVal >= 90.0 -> Color(0xFF00796B) // High Teal
                            rqdVal >= 75.0 -> Color(0xFF2E7D32) // Soft green
                            rqdVal >= 50.0 -> Color(0xFFFBC02D) // Yellow
                            rqdVal >= 25.0 -> Color(0xFFEF6C00) // Dark orange
                            else -> Color(0xFFC62828) // Deep red
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFF5D4037).copy(alpha = 0.08f), RoundedCornerShape(3.dp))
                                .border(0.5.dp, Color(0xFF8D6E63).copy(alpha = 0.25f), RoundedCornerShape(3.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(76.dp)
                                    .background(Color(0xFF8D6E63).copy(alpha = 0.12f))
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Run #${run.runNumber}: ${run.depthFrom}m",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5D4037)
                                )
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 1.dp, horizontal = 2.dp)
                                    .background(color = Color.LightGray.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)),
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                val pct = (recoveryVal / 100f).toFloat().coerceIn(0f, 1f)
                                if (pct > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(pct)
                                            .background(
                                                color = color,
                                                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                                            )
                                            .border(0.2.dp, Color.White.copy(0.2f), RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                                    ) {
                                        Text(
                                            "RQD: ${rqdVal.toInt()}% | Rec: ${recoveryVal.toInt()}% (${run.lithology})",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (tookRuns.size < 4) {
                        for (i in tookRuns.size until 4) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(3.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {}
                        }
                    }
                }
            }
        }
    }
}
