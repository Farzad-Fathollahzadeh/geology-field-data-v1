package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: GeologyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkThemeSetup by remember { mutableStateOf(false) }
            
            MyApplicationTheme(darkTheme = darkThemeSetup, dynamicColor = false) {
                // High contrast color scheme choice for sunlight exposure matching the Professional Polish palette
                val backgroundColor = MaterialTheme.colorScheme.background

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("app_surface"),
                    color = backgroundColor
                ) {
                    MainScreenContent(
                        viewModel = viewModel,
                        isDarkTheme = darkThemeSetup,
                        onToggleTheme = { darkThemeSetup = !darkThemeSetup }
                    )
                }
            }
        }
    }
}

// ==========================================
// GEOLOGY SECTION BRANDING REPRESENTATIONS
// ==========================================
data class GeologyTypeInfo(
    val name: String,
    val persianName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val description: String
)

fun getGeologyTypeInfo(type: String): GeologyTypeInfo {
    return when (type.lowercase(Locale.ROOT).trim()) {
        "lithology" -> GeologyTypeInfo(
            name = "Lithology",
            persianName = "سنگ‌شناسی (Lithology)",
            icon = Icons.Default.Terrain,
            color = Color(0xFF8D6E63), // Clay/Slate Brown (Earthy rock)
            description = "جنس سنگ، بافت، رنگ و اندازه دانه‌ها"
        )
        "alteration" -> GeologyTypeInfo(
            name = "Alteration",
            persianName = "دگرسانی (Alteration)",
            icon = Icons.Default.Waves, // Wavey alteration patterns
            color = Color(0xFFEF6C00), // Hydrothermal terracotta orange
            description = "شدت و نوع دگرسانی هیدروترمال و کانی‌های همراه"
        )
        "fault" -> GeologyTypeInfo(
            name = "Fault",
            persianName = "برداشت گسل (Fault)",
            icon = Icons.Default.CompareArrows, // Shear sliding arrows
            color = Color(0xFFD32F2F), // Tension fracture active red
            description = "امتداد، شیب، نوع حرکت و ضخامت گسل"
        )
        "joint" -> GeologyTypeInfo(
            name = "Joint",
            persianName = "درزه (Joint)",
            icon = Icons.Default.GridOn, // Fracture joint grid pattern
            color = Color(0xFF455A64), // Basalt slate dark blue-grey
            description = "فاصله‌داری و نوع درزه‌ها و شکستگی‌ها"
        )
        "vein" -> GeologyTypeInfo(
            name = "Vein",
            persianName = "رگه (Vein)",
            icon = Icons.Default.Bolt, // High-energy mineral veins
            color = Color(0xFFFFB300), // Quartz/Gold Vein Yellow
            description = "کانی‌سازی رگه‌ای و ضخامت آن"
        )
        "structure" -> GeologyTypeInfo(
            name = "Structure",
            persianName = "ساختار (Structure)",
            icon = Icons.Default.Menu, // Planar layering horizontal lines
            color = Color(0xFF00796B), // Foliated Metamorphic Teal
            description = "لایه‌بندی، شیب، امتداد و لولای چین‌خوردگی"
        )
        "mineralization" -> GeologyTypeInfo(
            name = "Mineralization",
            persianName = "کانی‌سازی (Mineralization)",
            icon = Icons.Default.Diamond, // Precious ore minerals
            color = Color(0xFFFF8F00), // Bronze/gold metallic kani
            description = "تیپ و درصد تخمینی کانی‌سازی فلزی"
        )
        else -> GeologyTypeInfo(
            name = type,
            persianName = type,
            icon = Icons.Default.Layers,
            color = Color(0xFF78909C),
            description = "برداشت اطلاعات زمین‌شناسی صحرایی"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreenContent(
    viewModel: GeologyViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val stations by viewModel.activeProjectStations.collectAsStateWithLifecycle()
    val activeStation by viewModel.activeStation.collectAsStateWithLifecycle()
    val observations by viewModel.activeStationObservations.collectAsStateWithLifecycle()
    val projectList by viewModel.projects.collectAsStateWithLifecycle()

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(450) // Crisp, lightning-fast boot sequence for optimal user response
        showSplash = false
    }

    if (showSplash) {
        GeoFieldSplashScreen()
    } else {
        // System Back Button Navigation System
        BackHandler(enabled = currentScreen != Screen.ProjectList) {
            when (currentScreen) {
                is Screen.CreateProject -> viewModel.navigateTo(Screen.ProjectList)
                is Screen.ProjectDashboard -> viewModel.navigateTo(Screen.ProjectList)
                is Screen.CreateStation -> viewModel.navigateTo(Screen.ProjectDashboard(activeProject?.id ?: 0))
                is Screen.StationDetail -> viewModel.navigateTo(Screen.ProjectDashboard(activeProject?.id ?: 0))
                is Screen.AddObservation -> viewModel.navigateTo(Screen.StationDetail(activeStation?.id ?: 0))
                else -> viewModel.navigateTo(Screen.ProjectList)
            }
        }

        val title = when (val screen = currentScreen) {
            is Screen.ProjectList -> "GeoField"
            is Screen.CreateProject -> "New Field Project"
            is Screen.ProjectDashboard -> activeProject?.name ?: "Project Dashboard"
            is Screen.CreateStation -> "Add Station"
            is Screen.StationDetail -> activeStation?.stationCode ?: "Station Details"
            is Screen.AddObservation -> "Log Field Observation"
        }

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (currentScreen is Screen.StationDetail && activeStation != null) {
                                    Text(
                                        text = activeProject?.name ?: "Field Log Project",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (currentScreen != Screen.ProjectList) {
                                IconButton(
                                    onClick = {
                                        when (currentScreen) {
                                            is Screen.CreateProject -> viewModel.navigateTo(Screen.ProjectList)
                                            is Screen.ProjectDashboard -> viewModel.navigateTo(Screen.ProjectList)
                                            is Screen.CreateStation -> viewModel.navigateTo(Screen.ProjectDashboard(activeProject?.id ?: 0))
                                            is Screen.StationDetail -> viewModel.navigateTo(Screen.ProjectDashboard(activeProject?.id ?: 0))
                                            is Screen.AddObservation -> viewModel.navigateTo(Screen.StationDetail(activeStation?.id ?: 0))
                                            else -> viewModel.navigateTo(Screen.ProjectList)
                                        }
                                    },
                                    modifier = Modifier
                                        .testTag("back_button")
                                        .padding(start = 8.dp, end = 4.dp)
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = onToggleTheme,
                                modifier = Modifier.testTag("theme_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Daylight/Night High-legibility Mode"
                                )
                            }
                            if (currentScreen is Screen.ProjectDashboard) {
                                val activeProjId = activeProject?.id
                                if (activeProjId != null) {
                                    IconButton(
                                        onClick = { viewModel.exportProjectData(context, activeProjId) },
                                        modifier = Modifier.testTag("export_button")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Export Excel (.xlsx)")
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Divider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        is Screen.ProjectList -> {
                            ProjectListScreen(
                                projects = projectList,
                                onSelectProject = {
                                    viewModel.setActiveProject(it.id)
                                    viewModel.navigateTo(Screen.ProjectDashboard(it.id))
                                },
                                onCreateClick = {
                                    viewModel.navigateTo(Screen.CreateProject)
                                },
                                onDeleteClick = {
                                    viewModel.deleteProject(it)
                                }
                            )
                        }
                        is Screen.CreateProject -> {
                            CreateProjectScreen(
                                onSave = { name, desc, date, mode, zone ->
                                    viewModel.createProject(name, desc, date, mode, zone)
                                },
                                onCancel = {
                                    viewModel.navigateTo(Screen.ProjectList)
                                }
                            )
                        }
                        is Screen.ProjectDashboard -> {
                            ProjectDashboardScreen(
                                project = activeProject,
                                stations = stations,
                                viewModel = viewModel,
                                onAddStationClick = {
                                    viewModel.navigateTo(Screen.CreateStation(activeProject?.id ?: 0))
                                },
                                onStationSelect = {
                                    viewModel.setActiveStation(it.id)
                                    viewModel.navigateTo(Screen.StationDetail(it.id))
                                }
                            )
                        }
                        is Screen.CreateStation -> {
                            CreateStationScreen(
                                viewModel = viewModel,
                                projectId = screen.projectId,
                                onSaved = { stationId ->
                                    viewModel.navigateTo(Screen.StationDetail(stationId))
                                }
                            )
                        }
                        is Screen.StationDetail -> {
                            StationDetailScreen(
                                station = activeStation,
                                observations = observations,
                                onAddObservationClick = {
                                    viewModel.navigateTo(Screen.AddObservation(activeStation?.id ?: 0))
                                },
                                onDeleteObservation = {
                                    viewModel.deleteObservation(it)
                                }
                            )
                        }
                        is Screen.AddObservation -> {
                            AddObservationScreen(
                                station = activeStation,
                                onSave = { type, notes, details, sampleTaken, sample ->
                                    val stId = activeStation?.id ?: 0
                                    viewModel.saveObservation(
                                        stationId = stId,
                                        obsType = type,
                                        notes = notes,
                                        photoPath = details["photoPath"] as? String,
                                        
                                        // Lithology
                                        lithType = details["lithologyType"] as? String,
                                        texture = details["texture"] as? String,
                                        color = details["color"] as? String,
                                        grainSize = details["grainSize"] as? String,
                                        lithAlteration = details["lithologyAlteration"] as? String,
                                        lithMineralization = details["lithologyMineralization"] as? String,

                                        // Alteration
                                        altType = details["alterationType"] as? String,
                                        altIntensity = details["alterationIntensity"] as? String,
                                        hostRock = details["alterationHostRock"] as? String,
                                        altAssociated = details["alterationMinerals"] as? String,

                                        // Fault / Structures
                                        faultType = details["faultType"] as? String,
                                        strike = details["strike"] as? Int,
                                        dip = details["dip"] as? Int,
                                        movement = details["movement"] as? String,
                                        width = details["width"] as? Double,

                                        // Linked Sample Taken
                                        sampleTaken = sampleTaken,
                                        sampleIdCode = sample?.get("sampleCode") ?: "",
                                        sampleType = sample?.get("sampleType") ?: "Hand specimen",
                                        sampleDesc = sample?.get("sampleDesc") ?: "",
                                        samplePhotoPath = sample?.get("samplePhoto") ?: ""
                                    ) {
                                        viewModel.navigateTo(Screen.StationDetail(stId))
                                    }
                                },
                                onCancel = {
                                    viewModel.navigateTo(Screen.StationDetail(activeStation?.id ?: 0))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeoFieldSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Apple-styled minimal geo tool instrument badge
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Nested decorative geometric/instrument design
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(38.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(38.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▲",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.offset(y = (-4).dp)
                    )
                    Text(
                        text = "▼",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.offset(y = 12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "GeoField",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "GEOLOGICAL FIELD INSTRUMENT",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(40.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "OFFLINE-FIRST FIELD DATA LOGGER • v1.1",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
    }
}

// ==========================================
// 1. PROJECTS LIST SCREEN LOGIC
// ==========================================
@Composable
fun ProjectListScreen(
    projects: List<Project>,
    onSelectProject: (Project) -> Unit,
    onCreateClick: () -> Unit,
    onDeleteClick: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (projects.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Field Projects Found",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Create your first geology project to start logging field stations.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onCreateClick,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(58.dp)
                        .testTag("create_first_project_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Project", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GEOLOGICAL MAPPING PROJECTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "v1.1",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(projects) { project ->
                        val isDark = isSystemInDarkTheme()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("project_item_${project.id}")
                                .clickable { onSelectProject(project) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color(0xFF232620) else Color(0xFFF9F7F5))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📂", fontSize = 18.sp)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = project.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "START: ${project.date}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteClick(project.id) },
                                            modifier = Modifier.testTag("delete_project_${project.id}").size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete project",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                              )
                                        }
                                    }
                                    if (!project.description.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = project.description,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("new_project_fab"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ADD NEW FIELD PROJECT", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

// ==========================================
// 2. CREATE PROJECT SCREEN
// ==========================================
@Composable
fun CreateProjectScreen(
    onSave: (String, String, String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var date by remember {
        mutableStateOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
    }
    var utmZoneMode by remember { mutableStateOf("AUTO") }
    var utmZoneManual by remember { mutableStateOf("39N") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create Field Project",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "PROJECT METADATA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    placeholder = { Text("e.g. Broken Hill Zinc Deposit") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    placeholder = { Text("Describe locality, logistics, mapping objectives...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("project_desc_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_date_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("cancel_project_creation"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, desc, date, utmZoneMode, utmZoneManual)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .weight(1.3f)
                    .height(56.dp)
                    .testTag("save_project_button"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Insert Project", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 3. PROJECT DASHBOARD SCREEN
// ==========================================
@Composable
fun ProjectDashboardScreen(
    project: Project?,
    stations: List<Station>,
    viewModel: GeologyViewModel,
    onAddStationClick: () -> Unit,
    onStationSelect: (Station) -> Unit
) {
    if (project == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Project Header Info (Drawn on a beautiful Minimalist Apple Card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = project.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "START: ${project.date}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${stations.size} Stations Recorded",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
                if (!project.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = project.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOGGED FIELD STATIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (stations.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Stations Yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tapping add station will auto lock GPS and convert coordinates to UTM.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stations) { station ->
                    val isDark = isSystemInDarkTheme()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStationSelect(station) }
                            .testTag("station_card_${station.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF232620) else Color(0xFFF9F7F5))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = station.prefix,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = station.stationCode,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Zone ${station.utmZone} | ${String.format(Locale.US, "%.0f E, %.0f N", station.utmEasting, station.utmNorthing)}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddStationClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .testTag("add_new_station_btn"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ADD NEW STATION", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
    }
}

// ==========================================
// 4. CREATE STATION SCREEN
// ==========================================
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CreateStationScreen(
    viewModel: GeologyViewModel,
    projectId: Int,
    onSaved: (Int) -> Unit
) {
    val context = LocalContext.current
    val gps by viewModel.gpsState.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    
    val prefixes = listOf("LITH", "ALT", "FLT", "STR", "SMP")
    var selectedPrefix by remember { mutableStateOf("LITH") }
    var generatedCode by remember { mutableStateOf("LITH-001") }
    
    // GPS request logic
    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Sync generated station code whenever prefix transitions
    LaunchedEffect(selectedPrefix) {
        viewModel.getNextStationCodeForPrefix(projectId, selectedPrefix) { code ->
            generatedCode = code
        }
    }

    // Trigger auto GPS locating safely on screen enter
    LaunchedEffect(Unit) {
        viewModel.resetGpsState()
        if (locationPermissionState.status.isGranted) {
            viewModel.captureLocation(context, "GPS")
        } else {
            viewModel.captureLocation(context, "SIMULATE")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Station Workflow: $generatedCode",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Select Prefix Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "STEP 1: SELECT STATION PREFIX",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                var isDropdownExpanded by remember { mutableStateOf(false) }
                val prefixDescriptions = mapOf(
                    "LITH" to "Lithology Observation (LITH) - جنس سنگ",
                    "ALT" to "Alteration Mapping (ALT) - دگرسانی",
                    "FLT" to "Fault Survey (FLT) - برداشت گسل",
                    "STR" to "Structural Logging (STR) - لایه‌بندی ساختاری",
                    "SMP" to "Physical Specimen Sample (SMP) - نمونه فیزیکی"
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSystemInDarkTheme()) Color(0xFF2C2C2F) else Color(0xFFF9F7F5))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { isDropdownExpanded = true }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = prefixDescriptions[selectedPrefix] ?: selectedPrefix,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown indicator",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f).background(MaterialTheme.colorScheme.surface)
                    ) {
                        prefixes.forEach { prefix ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = prefixDescriptions[prefix] ?: prefix,
                                        fontWeight = if (prefix == selectedPrefix) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (prefix == selectedPrefix) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedPrefix = prefix
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // GPS Capturing Status Card
        val gpsActiveColor = Color(0xFF00A86B)
        val borderHighlightColor = if (gps.isCaptured) {
            gpsActiveColor
        } else {
            MaterialTheme.colorScheme.outline
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, borderHighlightColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STEP 2: OFFLINE LOCATION LOCK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (gps.isCaptured) gpsActiveColor else MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    if (gps.isCaptured) {
                        Badge(
                            containerColor = gpsActiveColor.copy(alpha = 0.15f),
                            contentColor = gpsActiveColor
                        ) {
                            Text(
                                text = "Locked | ${gps.source}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (gps.isLocating) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Connecting to satellites...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (gps.isCaptured) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Latitude | Longitude", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text(
                                text = String.format(Locale.US, "%.6f°, %.6f°", gps.latitude, gps.longitude),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("UTM Coordinates", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text(
                                text = String.format(Locale.US, "%.1f E | %.1f N", gps.utmEasting, gps.utmNorthing),
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("UTM Grid Zone", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text(
                                text = "Zone ${gps.utmZone}", 
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Elevation", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text(
                                text = String.format(Locale.US, "%.0f m", gps.elevation), 
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Horizontal Accuracy", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            val accuracyColor = when {
                                gps.accuracy < 5.0 -> Color(0xFF00A86B)        // GPS_ACTIVE
                                gps.accuracy < 15.0 -> Color(0xFFFFB020)       // DATA_WARNING
                                else -> Color(0xFFFF3B30)                      // DATA_ERROR
                            }
                            Text(
                                text = String.format(Locale.US, "± %.1f m", gps.accuracy),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = accuracyColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Coordinates not found! Click 'Capture telemetry' or 'Simulate' below to proceed.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (locationPermissionState.status.isGranted) {
                                viewModel.captureLocation(context, "GPS")
                            } else {
                                locationPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("gps_capture_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Capture satellite GPS", fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.captureLocation(context, "SIMULATE")
                        },
                        modifier = Modifier
                            .weight(1.0f)
                            .height(48.dp)
                            .testTag("simulate_gps_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Simulate GPS", fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // COORD CONFIGURATION SYSTEM (Moved safely to station capture level to prevent project-level coordinate confusion)
        activeProject?.let { project ->
            var expandedZones by remember { mutableStateOf(false) }
            val zonesList = listOf("11N", "32V", "38N", "39N", "40N", "50J", "54K", "55S")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "COORDINATE PROJECTION SYSTEM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    
                    Text(
                        text = "Customize projection mode specifically for this station point:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isAuto = project.utmZoneMode == "AUTO"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAuto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    viewModel.updateProjectSettings(project.id, "AUTO", project.utmZoneManual)
                                    if (gps.isCaptured && gps.source != "None") {
                                        viewModel.setManualCoordinates(gps.latitude, gps.longitude, gps.elevation, gps.accuracy)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Auto UTM (GPS)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isAuto) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val isManual = project.utmZoneMode == "MANUAL"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isManual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    viewModel.updateProjectSettings(project.id, "MANUAL", project.utmZoneManual)
                                    if (gps.isCaptured && gps.source != "None") {
                                        viewModel.setManualCoordinates(gps.latitude, gps.longitude, gps.elevation, gps.accuracy)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Manual Override", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isManual) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (project.utmZoneMode == "MANUAL") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedZones = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Station UTM Zone: ${project.utmZoneManual}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Icon(Icons.Default.KeyboardArrowDown, null)
                                }
                            }
                            DropdownMenu(expanded = expandedZones, onDismissRequest = { expandedZones = false }) {
                                zonesList.forEach { zone ->
                                    DropdownMenuItem(
                                        text = { Text(zone, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            viewModel.updateProjectSettings(project.id, "MANUAL", zone)
                                            if (gps.isCaptured && gps.source != "None") {
                                                viewModel.setManualCoordinates(gps.latitude, gps.longitude, gps.elevation, gps.accuracy)
                                            }
                                            expandedZones = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.navigateTo(Screen.ProjectDashboard(projectId))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("back_to_dashboard"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (gps.isCaptured) {
                        viewModel.saveStation(projectId, generatedCode, selectedPrefix, onSaved)
                    }
                },
                enabled = gps.isCaptured,
                modifier = Modifier
                    .weight(1.2f)
                    .height(56.dp)
                    .testTag("save_station_btn"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lock Station", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 5. STATION DETAIL SCREEN
// ==========================================
@Composable
fun StationDetailScreen(
    station: Station?,
    observations: List<Observation>,
    onAddObservationClick: () -> Unit,
    onDeleteObservation: (Int) -> Unit
) {
    if (station == null) return

    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(station.createdAt))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Station details plaque (High contrast minimal Apple card theme)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = station.stationCode,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val accuracyColor = when {
                        station.accuracy < 5.0 -> Color(0xFF00A86B)        // GPS_ACTIVE
                        station.accuracy < 15.0 -> Color(0xFFFFB020)       // DATA_WARNING
                        else -> Color(0xFFFF3B30)                      // DATA_ERROR
                    }
                    Badge(
                        containerColor = accuracyColor.copy(alpha = 0.15f),
                        contentColor = accuracyColor
                    ) {
                        Text(
                            text = "Accuracy ±${String.format(Locale.US, "%.1f m", station.accuracy)}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = accuracyColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "UTM COORDINATES", 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f E\n%.1f N\nZone %s", station.utmEasting, station.utmNorthing, station.utmZone),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "ELEVATION & DATE", 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f m", station.elevation)} a.s.l.\n$dateStr",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "WGS84 Lat/Lon: ${String.format(Locale.US, "%.6f, %.6f", station.latitude, station.longitude)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Text(
            text = "GEOLOGICAL OBSERVATIONS LOCK",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        if (observations.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Observations Captured",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Log Lithologies, Fault structures, and rock samples taken at this station.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(observations) { obs ->
                    val isDark = isSystemInDarkTheme()
                    val info = getGeologyTypeInfo(obs.observationType)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(info.color.copy(alpha = 0.12f))
                                    .border(1.dp, info.color.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = info.icon,
                                    contentDescription = info.name,
                                    tint = info.color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = info.name.uppercase(Locale.getDefault()),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = info.color,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = info.persianName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Light,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteObservation(obs.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete entry",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                when (obs.observationType) {
                                    "Lithology" -> {
                                        Text(
                                            text = "${obs.lithologyType ?: "Unnamed rock"}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${obs.grainSize ?: "N/A"}-grained | Texture: ${obs.texture ?: "N/A"} | Color: ${obs.color ?: "N/A"}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    "Alteration" -> {
                                        Text(
                                            text = "${obs.alterationType ?: "Alteration style"}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Intensity: ${obs.intensity ?: "Moderate"} | Host rock: ${obs.hostRock ?: "N/A"}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    "Fault" -> {
                                        Text(
                                            text = "${obs.faultType ?: "Fault Structure"}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Attitude: Strike ${obs.strike ?: 0}° / Dip ${obs.dip ?: 0}°",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = obs.observationType,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (!obs.notes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "NOTES: ${obs.notes}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        maxLines = 2
                                    )
                                }

                                val photoPathsList = obs.photoPath?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                                if (photoPathsList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        items(photoPathsList) { path ->
                                            Card(
                                                modifier = Modifier.size(54.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                            ) {
                                                AsyncImage(
                                                    model = path,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
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
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddObservationClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .testTag("add_observation_btn"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ADD OBSERVATION", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
    }
}

// ==========================================================
// 6. ADD OBSERVATION / DYNAMIC FORM SCREEN
// ==========================================
@Composable
fun AddObservationScreen(
    station: Station?,
    onSave: (String, String, Map<String, Any?>, Boolean, Map<String, String>?) -> Unit,
    onCancel: () -> Unit
) {
    val stationCode = station?.stationCode ?: "SMP-001"
    var obsType by remember { mutableStateOf("Lithology") }
    var notes by remember { mutableStateOf("") }
    
    // Lithology State
    var lithType by remember { mutableStateOf("") }
    var texture by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var grainSize by remember { mutableStateOf("Medium") }
    var lithAlt by remember { mutableStateOf("") }
    var lithMin by remember { mutableStateOf("") }

    // Alteration State
    var altType by remember { mutableStateOf("") }
    var altIntensity by remember { mutableStateOf("Moderate") }
    var altHostRock by remember { mutableStateOf("") }
    var altMinerals by remember { mutableStateOf("") }

    // Fault / Structure State
    var faultType by remember { mutableStateOf("") }
    var strike by remember { mutableStateOf("") }
    var dip by remember { mutableStateOf("") }
    var movement by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }

    // Sample State
    var sampleTaken by remember { mutableStateOf(false) }
    var sampleIdCode by remember { mutableStateOf("") }
    var sampleType by remember { mutableStateOf("Hand specimen") }
    var sampleDesc by remember { mutableStateOf("") }

    // Photo Watermarking States
    var attachedPhotoPathsList by remember { mutableStateOf(emptyList<String>()) }
    var isOverlayEnabled by remember { mutableStateOf(true) }
    var overlayPosition by remember { mutableStateOf("BOTTOM_LEFT") }
    var sightAzimuth by remember { mutableStateOf(180) }
    var activeViewerPhotoPath by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var galleryMetadata by remember { mutableStateOf<PhotoProcessor.ImageMetadata?>(null) }
    var showGpsWarningDialog by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            val meta = PhotoProcessor.readImageExif(context, uri)
            galleryMetadata = meta
            if (meta.latitude == null || meta.longitude == null) {
                // GPS EXIF missing, trigger warning confirmation dialog
                showGpsWarningDialog = true
            } else {
                // GPS EXIF present, use it!
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            val path = PhotoProcessor.saveAnnotatedImage(
                                context = context,
                                baseBitmap = bitmap,
                                stationCode = stationCode,
                                lat = meta.latitude,
                                lon = meta.longitude,
                                elevation = meta.elevation ?: station?.elevation ?: 350.0,
                                utmZone = station?.utmZone ?: "39N",
                                utmEasting = station?.utmEasting ?: 500000.0,
                                utmNorthing = station?.utmNorthing ?: 10000000.0,
                                azimuth = sightAzimuth,
                                timestamp = meta.timestamp ?: System.currentTimeMillis(),
                                settings = PhotoProcessor.OverlaySettings(isOverlayEnabled, overlayPosition)
                            )
                            if (path != null) {
                                attachedPhotoPathsList = attachedPhotoPathsList + path
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    if (showGpsWarningDialog && selectedUri != null) {
        val uri = selectedUri!!
        val meta = galleryMetadata
        AlertDialog(
            onDismissRequest = { showGpsWarningDialog = false },
            title = { Text("GPS EXIF Tag Missing", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("This gallery image has no GPS metadata coordinates tag. Force inherit current station's coordinates (${stationCode})?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    onClick = {
                        showGpsWarningDialog = false
                        try {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                val bitmap = BitmapFactory.decodeStream(stream)
                                if (bitmap != null) {
                                    val path = PhotoProcessor.saveAnnotatedImage(
                                        context = context,
                                        baseBitmap = bitmap,
                                        stationCode = stationCode,
                                        lat = station?.latitude ?: 0.0,
                                        lon = station?.longitude ?: 0.0,
                                        elevation = station?.elevation ?: 0.0,
                                        utmZone = station?.utmZone ?: "39N",
                                        utmEasting = station?.utmEasting ?: 0.0,
                                        utmNorthing = station?.utmNorthing ?: 0.0,
                                        azimuth = sightAzimuth,
                                        timestamp = meta?.timestamp ?: System.currentTimeMillis(),
                                        settings = PhotoProcessor.OverlaySettings(isOverlayEnabled, overlayPosition)
                                    )
                                    if (path != null) {
                                        attachedPhotoPathsList = attachedPhotoPathsList + path
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                ) {
                    Text("Yes, Inherit Station GPS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showGpsWarningDialog = false }
                ) {
                    Text("Cancel Import", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Auto generate sample id when toggled
    LaunchedEffect(sampleTaken) {
        if (sampleTaken && sampleIdCode.isBlank()) {
            sampleIdCode = "${stationCode}-S1"
        }
    }

    val obsTypes = listOf("Lithology", "Alteration", "Fault", "Joint", "Vein", "Structure", "Mineralization")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Field Observation Logger",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Select Observation type Dropdown
        var expandedTypes by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            val currentTypeInfo = getGeologyTypeInfo(obsType)
            OutlinedButton(
                onClick = { expandedTypes = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("obs_type_dropdown_btn"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, currentTypeInfo.color)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(currentTypeInfo.icon, null, tint = currentTypeInfo.color, modifier = Modifier.size(22.dp))
                        Text(
                            text = "${obsType.uppercase(Locale.getDefault())} (${currentTypeInfo.persianName.substringBefore(" (")})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = currentTypeInfo.color,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown, 
                        contentDescription = null,
                        tint = currentTypeInfo.color
                    )
                }
            }
            DropdownMenu(
                expanded = expandedTypes,
                onDismissRequest = { expandedTypes = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                obsTypes.forEach { type ->
                    val typeInfo = getGeologyTypeInfo(type)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(typeInfo.icon, null, tint = typeInfo.color, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(typeInfo.name, fontWeight = FontWeight.Bold, color = typeInfo.color, fontSize = 13.sp)
                                    Text(typeInfo.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                                }
                            }
                        },
                        onClick = {
                            obsType = type
                            expandedTypes = false
                        }
                    )
                }
            }
        }

        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // --- DYNAMIC FIELDS ---
        when (obsType) {
            "Lithology" -> {
                Text("LITHOLOGY DESCRIPTION FORM", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, letterSpacing = 1.sp)
                
                // Rock presets quick selector row
                val rockPresets = listOf("Granite", "Basalt", "Sandstone", "Shale", "Schist", "Gneiss")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rockPresets.take(3).forEach { rock ->
                        AssistChip(
                            onClick = { lithType = rock },
                            label = { Text(rock, fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("rock_preset_$rock")
                        )
                    }
                }

                OutlinedTextField(
                    value = lithType,
                    onValueChange = { lithType = it },
                    label = { Text("Rock / Lithology Type") },
                    placeholder = { Text("e.g. Granodiorite, Amphibolite") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lithology_type"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = texture,
                    onValueChange = { texture = it },
                    label = { Text("Texture / Fabric") },
                    placeholder = { Text("e.g. Porphyritic, Foliated, Gneissic") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Color") },
                    placeholder = { Text("e.g. Grey-pink, Dark green") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Grain Size Selector dropdown
                val sizes = listOf("Very Coarse", "Coarse", "Medium", "Fine", "Very Fine")
                var expandedSizes by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expandedSizes = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Grain Size: $grainSize", 
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    DropdownMenu(expanded = expandedSizes, onDismissRequest = { expandedSizes = false }) {
                        sizes.forEach { size ->
                            DropdownMenuItem(
                                text = { Text(size, fontWeight = FontWeight.Medium) }, 
                                onClick = { grainSize = size; expandedSizes = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = lithAlt,
                    onValueChange = { lithAlt = it },
                    label = { Text("Alteration Style") },
                    placeholder = { Text("e.g. Potassic, Propylitic, Weak Chlorite") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = lithMin,
                    onValueChange = { lithMin = it },
                    label = { Text("Mineralization Index") },
                    placeholder = { Text("e.g. 2% Pyrite cubes, trace Chalcopyrite") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            "Alteration" -> {
                Text("ALTERATION ZONE FORM", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, letterSpacing = 1.sp)

                OutlinedTextField(
                    value = altType,
                    onValueChange = { altType = it },
                    label = { Text("Alteration Assembly") },
                    placeholder = { Text("e.g. Quartz-Sericite-Pyrite (Phyllic)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alteration_type"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Intensity
                val intensities = listOf("Strong", "Moderate", "Weak")
                val isDark = isSystemInDarkTheme()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    intensities.forEach { rep ->
                        val active = altIntensity == rep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (active) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        if (isDark) Color(0xFF2C2C2F) else Color(0xFFF9F7F5)
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (active) Color.Transparent else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { altIntensity = rep },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                rep, 
                                fontWeight = FontWeight.Bold,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = altHostRock,
                    onValueChange = { altHostRock = it },
                    label = { Text("Host Rock") },
                    placeholder = { Text("e.g. Altered Dacite porphyry") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = altMinerals,
                    onValueChange = { altMinerals = it },
                    label = { Text("Associated Alteration Minerals") },
                    placeholder = { Text("e.g. Alunite, Kaolinite, Gypsum") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            else -> {
                // Structural Forms (Fault, Joint, Vein, or General)
                Text("STRUCTURAL READINGS FORM: ${obsType.uppercase(Locale.getDefault())}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, letterSpacing = 1.sp)

                val faultPresets = listOf("Normal", "Reverse", "Strike-Slip", "Thrust", "Shear Zone")
                if (obsType == "Fault") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        faultPresets.take(3).forEach { ft ->
                            AssistChip(
                                onClick = { faultType = ft },
                                label = { Text(ft, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = if (obsType == "Fault") faultType else obsType,
                    onValueChange = { if (obsType == "Fault") faultType = it },
                    label = { Text("Structural / Feature Type") },
                    placeholder = { Text("e.g. Shear Joint, Normal Fault, Quartz Vein") },
                    enabled = obsType == "Fault" || obsType == "Structure",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fault_type"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = strike,
                        onValueChange = { strike = it },
                        label = { Text("Strike (0-360°)") },
                        placeholder = { Text("e.g. 045") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("strike_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = dip,
                        onValueChange = { dip = it },
                        label = { Text("Dip (0-90°)") },
                        placeholder = { Text("e.g. 60") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dip_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = movement,
                        onValueChange = { movement = it },
                        label = { Text(if (obsType == "Vein") "Vein Mineralogy" else "Sense of Movement") },
                        placeholder = { Text(if (obsType == "Vein") "Quartz + Calcite" else "e.g. Sinistral, Slickenside") },
                        modifier = Modifier.weight(1.3f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = width,
                        onValueChange = { width = it },
                        label = { Text("Width (cm)") },
                        placeholder = { Text("e.g. 15") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Detailed Observation Notes") },
            placeholder = { Text("Mention grain shapes, relative age relationships, structural features, etc...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .testTag("observation_notes_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // ==========================================
        // SAMPLE LOGGING NESTED SYSTEM
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, if (sampleTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Log Solid Specimen Sample", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Record physical rock sample at this node", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = sampleTaken,
                        onCheckedChange = { sampleTaken = it },
                        modifier = Modifier.testTag("sample_taken_toggle")
                    )
                }

                if (sampleTaken) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = sampleIdCode,
                            onValueChange = { sampleIdCode = it },
                            label = { Text("Sample ID Code") },
                            placeholder = { Text("e.g. DZ-014-A") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sample_id_field"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Sample Type Choices
                        val sampleTypes = listOf("Hand specimen", "Grab sample", "Channel sample", "Drill Core", "Soil")
                        var expandedSampleTypes by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expandedSampleTypes = true },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Specimen Type: $sampleType", 
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            DropdownMenu(expanded = expandedSampleTypes, onDismissRequest = { expandedSampleTypes = false }) {
                                sampleTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type, fontWeight = FontWeight.Medium) }, 
                                        onClick = { sampleType = type; expandedSampleTypes = false }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = sampleDesc,
                            onValueChange = { sampleDesc = it },
                            label = { Text("Sample Description & Purpose") },
                            placeholder = { Text("e.g. Geochronology zircon, assaying, thin section slice") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sample_description"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // ----------------- PHOTO CLUTTER MANAGER -----------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VISUAL FIELD EVIDENCE LOTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Badge(containerColor = MaterialTheme.colorScheme.primary.copy(0.1f), contentColor = MaterialTheme.colorScheme.primary) {
                        Text("${attachedPhotoPathsList.size} Photos", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                if (attachedPhotoPathsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No geo-referenced photos added yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(attachedPhotoPathsList) { path ->
                            Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))) {
                                AsyncImage(
                                    model = path,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clickable { activeViewerPhotoPath = path },
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize().background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(0.6f))
                                        )
                                    )
                                )
                                IconButton(
                                    onClick = { attachedPhotoPathsList = attachedPhotoPathsList.filter { it != path } },
                                    modifier = Modifier.align(Alignment.TopEnd).size(28.dp).background(Color.Black.copy(0.4f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = Color.White)
                                }
                                Text(
                                    "STATION PHOTO",
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Sighting Azimuth dial slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sighting Azimuth", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("${sightAzimuth}° (${com.example.PhotoProcessor.getCompassDirection(sightAzimuth)})", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = sightAzimuth.toFloat(),
                        onValueChange = { sightAzimuth = it.toInt() },
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Settings controls
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Watermark Stamp Overlay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Switch(checked = isOverlayEnabled, onCheckedChange = { isOverlayEnabled = it }, modifier = Modifier.scale(0.8f))
                    }
                    
                    if (isOverlayEnabled) {
                        var expandedPos by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expandedPos = true }, modifier = Modifier.height(36.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text("Position: $overlayPosition", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(expanded = expandedPos, onDismissRequest = { expandedPos = false }) {
                                listOf("BOTTOM_LEFT", "BOTTOM_RIGHT", "TOP_LEFT", "TOP_RIGHT").forEach { pos ->
                                    DropdownMenuItem(text = { Text(pos, fontSize = 12.sp) }, onClick = { overlayPosition = pos; expandedPos = false })
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { pickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Photo from Phone Storage (انتخاب عکس از حافظه گوشی)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("cancel_observation_btn"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val mapDetails = mutableMapOf<String, Any?>()
                    mapDetails["photoPath"] = if (attachedPhotoPathsList.isNotEmpty()) attachedPhotoPathsList.filter { it.isNotBlank() }.joinToString(",") else null
                    
                    when (obsType) {
                        "Lithology" -> {
                            mapDetails["lithologyType"] = lithType.ifBlank { "Unnamed Lithology" }
                            mapDetails["texture"] = texture.ifBlank { "N/A" }
                            mapDetails["color"] = color.ifBlank { "N/A" }
                            mapDetails["grainSize"] = grainSize
                            mapDetails["lithologyAlteration"] = lithAlt.ifBlank { null }
                            mapDetails["lithologyMineralization"] = lithMin.ifBlank { null }
                        }
                        "Alteration" -> {
                            mapDetails["alterationType"] = altType.ifBlank { "Unnamed Alteration" }
                            mapDetails["alterationIntensity"] = altIntensity
                            mapDetails["alterationHostRock"] = altHostRock.ifBlank { "N/A" }
                            mapDetails["alterationMinerals"] = altMinerals.ifBlank { "N/A" }
                        }
                        else -> {
                            mapDetails["faultType"] = faultType.ifBlank { obsType }
                            mapDetails["strike"] = strike.toIntOrNull()
                            mapDetails["dip"] = dip.toIntOrNull()
                            mapDetails["movement"] = movement.ifBlank { null }
                            mapDetails["width"] = width.toDoubleOrNull()
                        }
                    }

                    val sampleMap = if (sampleTaken) {
                        mapOf(
                            "sampleCode" to sampleIdCode,
                            "sampleType" to sampleType,
                            "sampleDesc" to sampleDesc
                        )
                    } else null

                    onSave(obsType, notes, mapDetails, sampleTaken, sampleMap)
                },
                modifier = Modifier
                    .weight(1.3f)
                    .height(56.dp)
                    .testTag("save_observation_btn"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Observation", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Geotagged Fullscreen Photo Viewer Dialog with Metadata bottom panel
    if (activeViewerPhotoPath != null) {
        Dialog(
            onDismissRequest = { activeViewerPhotoPath = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = activeViewerPhotoPath,
                    contentDescription = "Geotagged Station Evidence Photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 200.dp),
                    contentScale = ContentScale.Fit
                )

                // HUD Overlay Dismiss header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VISUAL INSPECTION SYSTEM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = { activeViewerPhotoPath = null },
                        modifier = Modifier.background(Color.Black.copy(0.40f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close viewer",
                            tint = Color.White
                        )
                    }
                }

                // BOTTOM METADATA HUD PANEL
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "DEC COMPILING TELEMETRY STAMP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(0.5f))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("STATION NODE", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stationCode, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("TRUE PLANE SIGHT", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${sightAzimuth}° ${PhotoProcessor.getCompassDirection(sightAzimuth)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB020), fontFamily = FontFamily.Monospace)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("WGS84 COORDINATES", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${String.format(Locale.US, "%.6f°", station?.latitude ?: 0.0)}, ${String.format(Locale.US, "%.6f°", station?.longitude ?: 0.0)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("UTM PROJECTION COORDINATES", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "z${station?.utmZone ?: "39N"} ${String.format(Locale.US, "%.1f E", station?.utmEasting ?: 0.0)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00A86B),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("ALTITUDE (ELEVATION)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${String.format(Locale.US, "%.1f m", station?.elevation ?: 0.0)} a.s.l.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("IMAGE TIMECODE EXIF", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(station?.createdAt ?: System.currentTimeMillis())),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
