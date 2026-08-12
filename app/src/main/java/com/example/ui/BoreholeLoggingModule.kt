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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.MainTab

@Composable
fun BoreholeLoggingContent(
    viewModel: GeologyViewModel,
    onNavigateToTab: ((MainTab) -> Unit)? = null,
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
                        text = "Core Logging Workspace",
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
                Text("New Project", fontWeight = FontWeight.SemiBold)
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
                                    viewModel.loadProject(project.id)
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
                            label = { Text("Project Title") },
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
                                    viewModel.loadProject(newId)
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
            onNavigateToTab = onNavigateToTab,
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
    onNavigateToTab: ((com.example.MainTab) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val boreholesFlow = remember(projectId) { viewModel.getBoreholesForProject(projectId) }
    val boreholes by boreholesFlow.collectAsState(initial = emptyList())
    val activeBorehole by viewModel.activeBorehole.collectAsState(initial = null)
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()
    
    var showCreateBoreholeDialog by remember { mutableStateOf(false) }
    var showEditBoreholeDialog by remember { mutableStateOf(false) }
    var editingBorehole by remember { mutableStateOf<Borehole?>(null) }
    
    // Core Simple/Advanced mode to de-clutter inputs & keep UI extremely simple
    var isSimpleMode by remember { mutableStateOf(true) }
    
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
        if (isSimpleMode) {
            latErr = false
            lonErr = false
            elevErr = false
            azErr = false
            dipErr = false
            plannedErr = planned.toDoubleOrNull() == null || (planned.toDoubleOrNull() ?: 0.0) <= 0.0
        } else {
            latErr = lat.toDoubleOrNull() == null
            lonErr = lon.toDoubleOrNull() == null
            elevErr = elev.toDoubleOrNull() == null
            azErr = azimuth.toDoubleOrNull() == null || (azimuth.toDoubleOrNull() ?: -1.0) !in 0.0..360.0
            dipErr = dip.toDoubleOrNull() == null || (dip.toDoubleOrNull() ?: -99.0) !in -90.0..90.0
            plannedErr = planned.toDoubleOrNull() == null || (planned.toDoubleOrNull() ?: 0.0) <= 0.0
        }

        return !(codeErr || latErr || lonErr || elevErr || azErr || dipErr || plannedErr)
    }

    if (activeBorehole == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Screen Header Card with simple switcher built of clean Material 3 design and bilingual labels
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSimpleMode) "ثبت سریع و ساده گمانه" else "نمای کامل مهندسی و محاسبات زوایا (HUD)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = if (isSimpleMode) "پنهان‌سازی فیلدهای پیچیده و زوایای انحراف" else "نمایش تمام مشخصات و پارامترهای مختصاتی",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { isSimpleMode = !isSimpleMode },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSimpleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSimpleMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(text = if (isSimpleMode) "نمای کامل" else "ساده‌سازی", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
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
                    Text("Add Borehole", fontSize = 11.5.sp)
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
                    text = "Active Borehole Logs:",
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
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Layers,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = bh.boreholeCode,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isFarsiMode) "کد گمانه: ${bh.boreholeCode}" else "Borehole Code: ${bh.boreholeCode}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
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
                                
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = if (isFarsiMode) "جهت‌گیری گمانه" else "Orientation (Dip/Dir)",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isFarsiMode) "آزیموت: ${bh.azimuth}° | شیب: ${bh.dip}°" else "Azimuth: ${bh.azimuth}° | Dip: ${bh.dip}°",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isFarsiMode) "عمق طراحی" else "Planned Depth",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isFarsiMode) "${bh.plannedDepth} متر" else "${bh.plannedDepth} meters",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isFarsiMode) "مختصات: ${String.format(Locale.US, "%.5f", bh.latitude)}، ${String.format(Locale.US, "%.5f", bh.longitude)}" else "Coords: ${String.format(Locale.US, "%.5f", bh.latitude)}, ${String.format(Locale.US, "%.5f", bh.longitude)}",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = if (isFarsiMode) "ارتفاع: ${bh.elevation} متر" else "Elev: ${bh.elevation}m",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
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
                title = { Text(if (isSimpleMode) "ثبت سریع گمانه اکتشافی" else "Log New Drilling Borehole", fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("Borehole ID / شناسه گمانه") },
                                placeholder = { Text("e.g. BH-01") },
                                isError = codeErr,
                                supportingText = { if (codeErr) Text("Required identifier / شناسه گمانه الزامی است", color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (isSimpleMode) {
                            item {
                                OutlinedTextField(
                                    value = planned,
                                    onValueChange = { planned = it },
                                    label = { Text("Target Depth (m) / عمق نهایی (متر)") },
                                    isError = plannedErr,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                var showCoordsByToggle by remember { mutableStateOf(false) }
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { showCoordsByToggle = !showCoordsByToggle },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "تنظیمات موقعیت و زاویه پیش‌فرض",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Icon(
                                                imageVector = if (showCoordsByToggle) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        if (showCoordsByToggle) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = lat.ifEmpty { "35.6892" },
                                                    onValueChange = { lat = it },
                                                    label = { Text("Latitude", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = lon.ifEmpty { "51.3890" },
                                                    onValueChange = { lon = it },
                                                    label = { Text("Longitude", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "حفاری عمودی (شیب: -۹۰ درجه) • آزیموت مایل: ۰ درجه • موقعیت پیش‌فرض تهران",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
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
                                    Text("Auto GPS Coords (Tehran)")
                                }
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
                                        latitude = if (isSimpleMode && lat.isBlank()) 35.6892 else (lat.toDoubleOrNull() ?: 35.6892),
                                        longitude = if (isSimpleMode && lon.isBlank()) 51.3890 else (lon.toDoubleOrNull() ?: 51.3890),
                                        elevation = if (isSimpleMode && elev.isBlank()) 1180.0 else (elev.toDoubleOrNull() ?: 1180.0),
                                        azimuth = if (isSimpleMode && azimuth.isBlank()) 0.0 else (azimuth.toDoubleOrNull() ?: 0.0),
                                        dip = if (isSimpleMode && dip.isBlank()) -90.0 else (dip.toDoubleOrNull() ?: -90.0),
                                        plannedDepth = planned.toDoubleOrNull() ?: 100.0
                                    )
                                ) {
                                    showCreateBoreholeDialog = false
                                }
                            }
                        }
                    ) {
                        Text(if (isSimpleMode) "ثبت گمانه" else "Add Record")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateBoreholeDialog = false }) {
                        Text(if (isSimpleMode) "انصراف" else "Cancel")
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
                                label = { Text("Borehole ID") },
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
                                val currentBh = editingBorehole!!
                                viewModel.saveBorehole(
                                    currentBh.copy(
                                        boreholeCode = code,
                                        latitude = lat.toDoubleOrNull() ?: currentBh.latitude,
                                        longitude = lon.toDoubleOrNull() ?: currentBh.longitude,
                                        elevation = elev.toDoubleOrNull() ?: currentBh.elevation,
                                        azimuth = azimuth.toDoubleOrNull() ?: currentBh.azimuth,
                                        dip = dip.toDoubleOrNull() ?: currentBh.dip,
                                        plannedDepth = planned.toDoubleOrNull() ?: currentBh.plannedDepth
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
            onNavigateToTab = onNavigateToTab,
            modifier = modifier
        )
    }
}

@Composable
fun BoreholeLoggingDetailsWorkspace(
    viewModel: GeologyViewModel,
    borehole: Borehole,
    onNavigateToTab: ((com.example.MainTab) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableStateOf(0) }
    
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()
    val dailyReportsFlow = remember(borehole.id) { viewModel.getDailyReportsForBorehole(borehole.id) }
    val dailyReports by dailyReportsFlow.collectAsState(initial = emptyList())
    val coreRunsFlow = remember(borehole.id) { viewModel.getCoreRunsForBorehole(borehole.id) }
    val coreRuns by coreRunsFlow.collectAsState(initial = emptyList())

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
                            text = if (isFarsiMode) "گمانه: ${borehole.boreholeCode}" else "Borehole: ${borehole.boreholeCode}",
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
                        text = if (isFarsiMode) "عمق طراحی: ${borehole.plannedDepth} متر | شیب: ${borehole.dip}° | آزیموت: ${borehole.azimuth}°" else "Planned Depth: ${borehole.plannedDepth}m | Dip: ${borehole.dip}° | Azimuth: ${borehole.azimuth}°",
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
                text = { Text(if (isFarsiMode) "شیفت‌های حفاری" else "Shift Logs", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text(if (isFarsiMode) "لاگ مغزه‌ها (کیفیت)" else "Core Runs (RQD)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text(if (isFarsiMode) "ستون گرافیکی" else "Graphic Column", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
            )
        }

        when (selectedSubTab) {
            0 -> {
                DailyReportsPanel(
                    viewModel = viewModel,
                    boreholeId = borehole.id,
                    boreholeCode = borehole.boreholeCode,
                    reports = dailyReports
                )
            }
            1 -> {
                CoreRunsPanel(
                    viewModel = viewModel,
                    boreholeId = borehole.id,
                    boreholeCode = borehole.boreholeCode,
                    runs = coreRuns,
                    onNavigateToTab = onNavigateToTab
                )
            }
            2 -> {
                GraphicalBoreholeLog(
                    runs = coreRuns,
                    isFarsiMode = isFarsiMode
                )
            }
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
                                    Text("Drill Progress", fontSize = 9.sp, color = Color.Gray)
                                    Text("+${report.progressMeters} meters", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF43A047))
                                }
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text("Stoppages & Downtime", fontSize = 9.sp, color = Color.Gray)
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
                                Text("Shift Mode:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                                label = { Text("Shift Onsite Geologist") },
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
    runs: List<BoreholeCoreRun>,
    onNavigateToTab: ((com.example.MainTab) -> Unit)? = null
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

        if (runs.isNotEmpty() && onNavigateToTab != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab.invoke(com.example.MainTab.RockMechanics) }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Layers, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (viewModel.isFarsiModeSetting.value) 
                                "تحلیل مهندسی توده سنگ (RMR & Q)" 
                            else 
                                "Analyze Rock Mass Quality (RMR & Q)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (viewModel.isFarsiModeSetting.value)
                                "انتقال خودکار داده‌های RQD و UCS این گمانه به پنل مکانیک سنگ جهت تحلیل پایداری"
                            else
                                "Load RQD and UCS from these core runs directly into the Rock Mechanics suite.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ArrowForward, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.size(16.dp)
                    )
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
                                        text = "Run #${run.runNumber}: from ${run.depthFrom} to ${run.depthTo} m",
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
                                    Text("Recovery", fontSize = 9.sp, color = Color.Gray)
                                    Text("${String.format(Locale.US, "%.1f", run.recoveryPercent)}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF43A047))
                                }
                                Column {
                                    Text("RQD Score (Quality)", fontSize = 9.sp, color = Color.Gray)
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
                                    label = { Text("Alteration") },
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
                    text = "Stratigraphic Core Box (Digital)",
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

@Composable
fun GraphicalBoreholeLog(
    runs: List<BoreholeCoreRun>,
    isFarsiMode: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedQualityFilter by remember { mutableStateOf(0) } // 0: All, 1: Excellent (RQD >= 90), 2: Good (75-90), 3: Fair (50-75), 4: Poor (25-50), 5: Very Poor (< 25)
    var selectedRunForDetail by remember { mutableStateOf<BoreholeCoreRun?>(null) }

    // Weighted summary stats calculation
    val totalInterval = runs.sumOf { it.depthTo - it.depthFrom }
    val avgRqd = if (totalInterval > 0) {
        runs.sumOf { (it.depthTo - it.depthFrom) * it.rqdPercent } / totalInterval
    } else 0.0

    val avgRecovery = if (totalInterval > 0) {
        runs.sumOf { (it.depthTo - it.depthFrom) * it.recoveryPercent } / totalInterval
    } else 0.0

    val filteredRuns = runs.filter { run ->
        val matchesSearch = run.lithology.contains(searchQuery, ignoreCase = true) ||
                run.mineralization.contains(searchQuery, ignoreCase = true) ||
                run.alteration.contains(searchQuery, ignoreCase = true)
        
        val matchesQuality = when (selectedQualityFilter) {
            1 -> run.rqdPercent >= 90.0
            2 -> run.rqdPercent >= 75.0 && run.rqdPercent < 90.0
            3 -> run.rqdPercent >= 50.0 && run.rqdPercent < 75.0
            4 -> run.rqdPercent >= 25.0 && run.rqdPercent < 50.0
            5 -> run.rqdPercent < 25.0
            else -> true
        }
        matchesSearch && matchesQuality
    }.sortedBy { it.depthFrom }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Stats Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isFarsiMode) "مجموع لاگ حفاری" else "Total Logged",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f m", totalInterval),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isFarsiMode) "میانگین وزنی RQD" else "Weighted Avg RQD",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%%", avgRqd),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            avgRqd >= 75.0 -> Color(0xFF2E7D32)
                            avgRqd >= 50.0 -> Color(0xFFEF6C00)
                            else -> Color(0xFFC62828)
                        }
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isFarsiMode) "میانگین وزنی بازیابی" else "Weighted TCR",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%%", avgRecovery),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Search and Quality Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp),
                placeholder = {
                    Text(
                        text = if (isFarsiMode) "جستجوی لیتولوژی..." else "Search lithology...",
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Scrollable filter pills for RQD quality levels
            val filters = if (isFarsiMode) {
                listOf("همه RQD", "عالی (>=۹۰)", "خوب (۷۵-۹۰)", "متوسط (۵۰-۷۵)", "ضعیف (۲۵-۵۰)", "بسیار ضعیف (<۲۵)")
            } else {
                listOf("All RQD", "Excellent (>=90)", "Good (75-90)", "Fair (50-75)", "Poor (25-50)", "Very Poor (<25)")
            }

            var expandedFilter by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { expandedFilter = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = filters[selectedQualityFilter],
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                    }
                }
                DropdownMenu(
                    expanded = expandedFilter,
                    onDismissRequest = { expandedFilter = false }
                ) {
                    filters.forEachIndexed { idx, title ->
                        DropdownMenuItem(
                            text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            onClick = {
                                selectedQualityFilter = idx
                                expandedFilter = false
                            }
                        )
                    }
                }
            }
        }

        // Legend Card explaining visual styles
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isFarsiMode) "راهنمای علائم گرافیکی" else "Graphic Legend",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Row showing some lithology colors and patterns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Limestone
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFFCFD8DC)).border(0.5.dp, Color.Gray))
                        Text(if (isFarsiMode) "آهک" else "Limestone", fontSize = 8.sp)
                    }
                    // Sandstone
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFF9C4)).border(0.5.dp, Color.Gray))
                        Text(if (isFarsiMode) "ماسه سنگ" else "Sandstone", fontSize = 8.sp)
                    }
                    // Clay
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFFD7CCC8)).border(0.5.dp, Color.Gray))
                        Text(if (isFarsiMode) "رس" else "Clay/Soil", fontSize = 8.sp)
                    }
                    // Granite
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFCCBC)).border(0.5.dp, Color.Gray))
                        Text(if (isFarsiMode) "گرانیت" else "Granite", fontSize = 8.sp)
                    }
                }
            }
        }

        if (filteredRuns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (isFarsiMode) "مغزه‌ای جهت نمایش وجود ندارد" else "No core runs to build graphic column.",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isFarsiMode) "برای نمایش ستون زمین‌شناسی، از تب دوم مغزه‌ها را اضافه کنید." else "Add Core Runs in the Core Logs panel to populate the geological column.",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRuns) { run ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRunForDetail = run },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        ) {
                            // 1. LEFT COLUMN: Depth scale (Paper style)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(62.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                // Draw a ruler indicator
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val h = size.height
                                    // Tick lines
                                    drawLine(Color.Gray.copy(alpha = 0.6f), Offset(size.width - 2.dp.toPx(), 0f), Offset(size.width - 2.dp.toPx(), h), strokeWidth = 1.dp.toPx())
                                    drawLine(Color.Gray, Offset(size.width - 10.dp.toPx(), 4.dp.toPx()), Offset(size.width, 4.dp.toPx()), strokeWidth = 1.dp.toPx())
                                    drawLine(Color.Gray, Offset(size.width - 10.dp.toPx(), h - 4.dp.toPx()), Offset(size.width, h - 4.dp.toPx()), strokeWidth = 1.dp.toPx())
                                    drawLine(Color.Gray.copy(alpha = 0.5f), Offset(size.width - 6.dp.toPx(), h / 2f), Offset(size.width, h / 2f), strokeWidth = 0.8.dp.toPx())
                                }

                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = "${run.depthFrom}m",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Run #${run.runNumber}",
                                        fontSize = 7.5.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        text = "${run.depthTo}m",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // 2. CENTER COLUMN: Lithology pattern
                            val lithLower = run.lithology.lowercase().trim()
                            val baseColor = when {
                                lithLower.contains("clay") || lithLower.contains("soil") || lithLower.contains("رس") || lithLower.contains("خاک") -> Color(0xFFD7CCC8)
                                lithLower.contains("sand") || lithLower.contains("ماسه") -> Color(0xFFFFF9C4)
                                lithLower.contains("lime") || lithLower.contains("آهک") -> Color(0xFFCFD8DC)
                                lithLower.contains("basalt") || lithLower.contains("بازالت") -> Color(0xFF37474F)
                                lithLower.contains("granite") || lithLower.contains("گرانیت") -> Color(0xFFFFCCBC)
                                lithLower.contains("shale") || lithLower.contains("شیل") -> Color(0xFFB0BEC5)
                                lithLower.contains("silt") || lithLower.contains("سیلت") -> Color(0xFFE0E0E0)
                                else -> Color(0xFFE0F2F1)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(55.dp)
                                    .background(baseColor)
                                    .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    
                                    when {
                                        lithLower.contains("clay") || lithLower.contains("soil") || lithLower.contains("رس") || lithLower.contains("خاک") -> {
                                            val path = Path()
                                            for (y in 8..h.toInt() step 16) {
                                                path.reset()
                                                path.moveTo(0f, y.toFloat())
                                                path.quadraticTo(w * 0.25f, y.toFloat() - 3f, w * 0.5f, y.toFloat())
                                                path.quadraticTo(w * 0.75f, y.toFloat() + 3f, w, y.toFloat())
                                                drawPath(path, color = Color(0xFF8D6E63).copy(alpha = 0.6f), style = Stroke(width = 1f))
                                            }
                                        }
                                        lithLower.contains("sand") || lithLower.contains("ماسه") -> {
                                            for (x in 6..w.toInt() - 6 step 12) {
                                                for (y in 6..h.toInt() - 6 step 12) {
                                                    val rx = x + (Math.sin(y.toDouble() * 3.0) * 3).toFloat()
                                                    val ry = y + (Math.cos(x.toDouble() * 3.0) * 3).toFloat()
                                                    drawCircle(color = Color(0xFF795548).copy(alpha = 0.6f), radius = 1.2f, center = Offset(rx, ry))
                                                }
                                            }
                                        }
                                        lithLower.contains("lime") || lithLower.contains("آهک") -> {
                                            for (y in 16..h.toInt() step 20) {
                                                drawLine(color = Color(0xFF78909C), start = Offset(0f, y.toFloat()), end = Offset(w, y.toFloat()), strokeWidth = 1f)
                                            }
                                            var isOdd = false
                                            for (y in 16..h.toInt() step 20) {
                                                val startX = if (isOdd) 15f else 5f
                                                for (x in startX.toInt()..w.toInt() step 25) {
                                                    drawLine(color = Color(0xFF78909C), start = Offset(x.toFloat(), y - 20f), end = Offset(x.toFloat(), y.toFloat()), strokeWidth = 1f)
                                                }
                                                isOdd = !isOdd
                                            }
                                        }
                                        lithLower.contains("basalt") || lithLower.contains("بازالت") -> {
                                            for (x in 8..w.toInt() - 8 step 20) {
                                                for (y in 8..h.toInt() - 8 step 20) {
                                                    drawCircle(color = Color.Black.copy(alpha = 0.2f), radius = 3f, center = Offset(x.toFloat(), y.toFloat()), style = Stroke(width = 1f))
                                                }
                                            }
                                        }
                                        lithLower.contains("granite") || lithLower.contains("گرانیت") -> {
                                            for (x in 8..w.toInt() - 8 step 18) {
                                                for (y in 8..h.toInt() - 8 step 18) {
                                                    drawLine(color = Color(0xFFD84315).copy(alpha = 0.35f), start = Offset(x - 3f, y - 3f), end = Offset(x + 3f, y + 3f), strokeWidth = 1f)
                                                    drawLine(color = Color(0xFFD84315).copy(alpha = 0.35f), start = Offset(x - 3f, y + 3f), end = Offset(x + 3f, y - 3f), strokeWidth = 1f)
                                                }
                                            }
                                        }
                                        lithLower.contains("shale") || lithLower.contains("شیل") -> {
                                            for (y in 6..h.toInt() step 8) {
                                                drawLine(color = Color(0xFF546E7A).copy(alpha = 0.4f), start = Offset(0f, y.toFloat()), end = Offset(w, y.toFloat()), strokeWidth = 1f)
                                            }
                                        }
                                        else -> {
                                            // Diagonal stripes for other types
                                            for (offset in -h.toInt()..w.toInt() step 14) {
                                                drawLine(color = Color(0xFF004D40).copy(alpha = 0.15f), start = Offset(offset.toFloat(), 0f), end = Offset(offset.toFloat() + h, h), strokeWidth = 1f)
                                            }
                                        }
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Text(
                                        text = run.lithology,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (lithLower.contains("basalt")) Color.White else Color.Black,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .background(
                                                color = if (lithLower.contains("basalt")) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.65f),
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                            .padding(horizontal = 2.dp)
                                    )
                                }
                            }

                            // 3. RIGHT COLUMN: TCR and RQD bars & indicators
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Lithology title
                                Text(
                                    text = if (isFarsiMode) "لیتولوژی: ${run.lithology}" else "Lithology: ${run.lithology}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // TCR bar (Total Core Recovery)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isFarsiMode) "بازیابی:" else "Rec:",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.width(36.dp)
                                    )
                                    LinearProgressIndicator(
                                        progress = { (run.recoveryPercent / 100f).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.secondary,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        text = "${run.recoveryPercent.toInt()}%",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // RQD bar
                                val rqdVal = run.rqdPercent
                                val rqdColor = when {
                                    rqdVal >= 90.0 -> Color(0xFF00796B) // Teal
                                    rqdVal >= 75.0 -> Color(0xFF388E3C) // Green
                                    rqdVal >= 50.0 -> Color(0xFFFBC02D) // Yellow
                                    rqdVal >= 25.0 -> Color(0xFFF57C00) // Orange
                                    else -> Color(0xFFD32F2F) // Red
                                }
                                val rqdQualityText = when {
                                    rqdVal >= 90.0 -> if (isFarsiMode) "عالی" else "Excellent"
                                    rqdVal >= 75.0 -> if (isFarsiMode) "خوب" else "Good"
                                    rqdVal >= 50.0 -> if (isFarsiMode) "متوسط" else "Fair"
                                    rqdVal >= 25.0 -> if (isFarsiMode) "ضعیف" else "Poor"
                                    else -> if (isFarsiMode) "بسیار ضعیف" else "V. Poor"
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "RQD:",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.width(36.dp)
                                    )
                                    LinearProgressIndicator(
                                        progress = { (rqdVal / 100f).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = rqdColor,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        text = "${rqdVal.toInt()}% ($rqdQualityText)",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = rqdColor
                                    )
                                }

                                // Structural frequency and UCS inline status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = if (isFarsiMode) "بسامد شکست: ${run.fractureFrequency} /متر" else "Frac Freq: ${run.fractureFrequency} /m",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "UCS: ${run.ucsMpa.toInt()} MPa",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail dialog on click
    if (selectedRunForDetail != null) {
        val run = selectedRunForDetail!!
        AlertDialog(
            onDismissRequest = { selectedRunForDetail = null },
            icon = { Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = if (isFarsiMode) "جزئیات لاگ مغزه" else "Core Run Log Details",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isFarsiMode) "شماره ران: ${run.runNumber}" else "Run Number: ${run.runNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isFarsiMode) "عمق: از ${run.depthFrom} تا ${run.depthTo} متر" else "Depth: from ${run.depthFrom} to ${run.depthTo} m",
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isFarsiMode) "نوع سنگ (لیتولوژی): ${run.lithology}" else "Lithology Rock Type: ${run.lithology}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isFarsiMode) "بازیابی مغزه (TCR): ${run.recoveryPercent}%" else "Total Core Recovery (TCR): ${run.recoveryPercent}%",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "RQD: ${run.rqdPercent}%",
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isFarsiMode) "کانی‌سازی: ${run.mineralization}" else "Mineralization: ${run.mineralization}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isFarsiMode) "دگرسانی: ${run.alteration}" else "Alteration: ${run.alteration}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isFarsiMode) "بسامد درزه‌ها: ${run.fractureFrequency} عدد در متر" else "Fracture Frequency: ${run.fractureFrequency} /m",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "UCS: ${run.ucsMpa} MPa",
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRunForDetail = null }) {
                    Text(if (isFarsiMode) "تایید" else "OK")
                }
            }
        )
    }
}
