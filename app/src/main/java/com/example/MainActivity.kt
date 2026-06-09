package com.example

import android.Manifest
import android.widget.Toast
import android.content.Intent
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.BoreholeLoggingContent
import com.example.ui.RockMechanicsContent
import com.example.ui.ToolsContent
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView

// ----------------- AUTOCOMPLETE PRESETS GLOBAL CONSTANTS -----------------
val lithologySuggestions = listOf("Basalt", "Andesite", "Granite", "Tuff", "Diorite", "Limestone", "Schist", "Sandstone", "Shale", "Dacite", "Rhyolite", "Gabbro")
val textureSuggestions = listOf("Porphyritic", "Aphanitic", "Phaneritic", "Glassy", "Foliated", "Massive", "Granular", "Brecciated")
val colorSuggestions = listOf("Dark Gray", "Light Gray", "Earthy Brown", "Terracotta Red", "Pale Green", "Cream White", "Jet Black")
val alterationSuggestions = listOf("Argillic", "Phyllic", "Potassic", "Propylitic", "Silicification", "Sericitization", "Carbonatization")
val alterationMineralsSuggestions = listOf("Quartz", "Sericite", "Chlorite", "Calcite", "Pyrite", "Chalcopyrite", "Epidote", "Kaolinite")
val faultSuggestions = listOf("Normal Fault", "Reverse Fault", "Thrust Fault", "Strike-Slip - Dextral", "Strike-Slip - Sinistral", "Shear Zone")
val jointSuggestions = listOf("Systematic Joint Set", "Nonsystematic Fractures", "Columnar Jointing")
val veinSuggestions = listOf("Quartz-Veinlet", "Stockwork Veinlets", "Massive Quartz Vein", "Brecciated Vein")
val structureSuggestions = listOf("Bedding (S0)", "Foliation (S1)", "Cleavage", "Fold Axis")
val mineralizationSuggestions = listOf("Disseminated", "Massive Sulfide", "Stockwork", "Supergene Enrichment")

class MainActivity : ComponentActivity() {
    private val viewModel: GeologyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeSetting by viewModel.themeSetting.collectAsStateWithLifecycle()
            val fontSizeSetting by viewModel.fontSizeSetting.collectAsStateWithLifecycle()
            
            MyApplicationTheme(themeSetting = themeSetting) {
                val currentDensity = androidx.compose.ui.platform.LocalDensity.current
                val scaleMultiplier = when (fontSizeSetting) {
                    "Large" -> 1.2f
                    "Huge" -> 1.4f
                    else -> 1.0f
                }
                val customDensity = remember(currentDensity, scaleMultiplier) {
                    androidx.compose.ui.unit.Density(
                        density = currentDensity.density,
                        fontScale = currentDensity.fontScale * scaleMultiplier
                    )
                }
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides customDensity
                ) {
                    val backgroundColor = MaterialTheme.colorScheme.background

                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("app_surface"),
                        color = backgroundColor
                    ) {
                        MainScreenContent(
                            viewModel = viewModel,
                            isDarkTheme = (themeSetting == "Dark" || themeSetting == "HighContrast"),
                            onToggleTheme = {
                                val nextStyle = when (themeSetting) {
                                    "Light" -> "Dark"
                                    "Dark" -> "HighContrast"
                                    "HighContrast" -> "Silver"
                                    else -> "Light"
                                }
                                viewModel.setThemeSetting(nextStyle)
                            }
                        )
                    }
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
            persianName = "Lithology",
            icon = Icons.Default.Terrain,
            color = Color(0xFF8D6E63), // Clay/Slate Brown (Earthy rock)
            description = "Rock type, texture, color, and grain size details"
        )
        "alteration" -> GeologyTypeInfo(
            name = "Alteration",
            persianName = "Alteration",
            icon = Icons.Default.Waves, // Wavey alteration patterns
            color = Color(0xFFEF6C00), // Hydrothermal terracotta orange
            description = "Intensity, hydrothermal style, and associated minerals"
        )
        "fault" -> GeologyTypeInfo(
            name = "Fault",
            persianName = "Fault Structure",
            icon = Icons.Default.CompareArrows, // Shear sliding arrows
            color = Color(0xFFD32F2F), // Tension fracture active red
            description = "Attitude (strike, dip), kinematics, slip, and zone width"
        )
        "joint" -> GeologyTypeInfo(
            name = "Joint",
            persianName = "Joint Set",
            icon = Icons.Default.GridOn, // Fracture joint grid pattern
            color = Color(0xFF455A64), // Basalt slate dark blue-grey
            description = "Spacing (cm), fracture type, and joint set attitude"
        )
        "vein" -> GeologyTypeInfo(
            name = "Vein",
            persianName = "Hydrothermal Vein",
            icon = Icons.Default.Bolt, // High-energy mineral veins
            color = Color(0xFFFFB300), // Quartz/Gold Vein Yellow
            description = "Mineral assemblages, width, and style of vein systems"
        )
        "structure" -> GeologyTypeInfo(
            name = "Structure",
            persianName = "Structural Bedding",
            icon = Icons.Default.Menu, // Planar layering horizontal lines
            color = Color(0xFF00796B), // Foliated Metamorphic Teal
            description = "Bedding structural attitude (strike, dip, cleavage)"
        )
        "mineralization" -> GeologyTypeInfo(
            name = "Mineralization",
            persianName = "Mineralization",
            icon = Icons.Default.Diamond, // Precious ore minerals
            color = Color(0xFFFF8F00), // Bronze/gold metallic kani
            description = "Visual ore minerals, style (disseminated/massive), and grade"
        )
        else -> GeologyTypeInfo(
            name = type,
            persianName = type,
            icon = Icons.Default.Layers,
            color = Color(0xFF78909C),
            description = "Geological field survey mapping observations"
        )
    }
}

enum class MainTab {
    Home, GeologicalMapping, BoreholeLogging, RockMechanics, Map, Tools
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
    val themeSetting by viewModel.themeSetting.collectAsStateWithLifecycle()
    val fontSizeSetting by viewModel.fontSizeSetting.collectAsStateWithLifecycle()
    val coordSystemSetting by viewModel.coordSystemSetting.collectAsStateWithLifecycle()

    // Global Stats flows
    val allStations by viewModel.allStations.collectAsStateWithLifecycle()
    val allObservationsByState by viewModel.allObservations.collectAsStateWithLifecycle()
    val allSamplesByState by viewModel.allSamples.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(MainTab.Home) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(450) // Crisp, lightning-fast boot sequence for optimal user response
        showSplash = false
    }

    if (showSplash) {
        GeoFieldSplashScreen()
    } else {
        // System Back Button Navigation System (active under Mapping tab only)
        BackHandler(enabled = activeTab == MainTab.GeologicalMapping && currentScreen != Screen.ProjectList) {
            when (currentScreen) {
                is Screen.CreateProject -> viewModel.navigateTo(Screen.ProjectList)
                is Screen.ProjectDashboard -> viewModel.navigateTo(Screen.ProjectList)
                is Screen.CreateStation -> viewModel.navigateTo(Screen.ProjectDashboard(activeProject?.id ?: 0))
                is Screen.StationDetail -> viewModel.navigateTo(Screen.ProjectDashboard(activeProject?.id ?: 0))
                is Screen.AddObservation -> viewModel.navigateTo(Screen.StationDetail(activeStation?.id ?: 0))
                else -> viewModel.navigateTo(Screen.ProjectList)
            }
        }

        val mappingTitle = when (val screen = currentScreen) {
            is Screen.ProjectList -> "Geological Mapping Projects"
            is Screen.CreateProject -> "Create New Project"
            is Screen.ProjectDashboard -> activeProject?.name ?: "Active Project"
            is Screen.CreateStation -> "Add Survey Station"
            is Screen.StationDetail -> activeStation?.stationCode ?: "Station Details"
            is Screen.AddObservation -> "Log Field Observation"
        }

        val topBarTitle = when (activeTab) {
            MainTab.Home -> "Dashboard"
            MainTab.GeologicalMapping -> mappingTitle
            MainTab.BoreholeLogging -> "Borehole Core Logging"
            MainTab.RockMechanics -> "Rock Geotechnical Mechanics"
            MainTab.Map -> "Spatial Navigation Map"
            MainTab.Tools -> "Geotech Field Tools"
        }

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = topBarTitle,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (activeTab == MainTab.GeologicalMapping && currentScreen is Screen.StationDetail && activeStation != null) {
                                    Text(
                                        text = activeProject?.name ?: "Field Log Project",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "GeoField Platform v3.1.1",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (activeTab == MainTab.Home) {
                                // Settings button in Home's top-left corner as requested
                                IconButton(
                                    onClick = { showSettingsDialog = true },
                                    modifier = Modifier
                                        .testTag("settings_button")
                                        .padding(start = 8.dp)
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Software Settings",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else if (activeTab == MainTab.GeologicalMapping && currentScreen != Screen.ProjectList) {
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
                                        contentDescription = "Go Back",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                            IconButton(
                                onClick = onToggleTheme,
                                modifier = Modifier.testTag("theme_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Day/Night theme"
                                )
                            }
                            if (activeTab == MainTab.GeologicalMapping && currentScreen is Screen.ProjectDashboard) {
                                val activeProjId = activeProject?.id
                                if (activeProjId != null) {
                                    IconButton(
                                        onClick = { showExportDialog = true },
                                        modifier = Modifier.testTag("export_button")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Export GIS")
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
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = activeTab == MainTab.Home,
                        onClick = { activeTab = MainTab.Home },
                        label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home dashboard") }
                    )
                    NavigationBarItem(
                        selected = activeTab == MainTab.GeologicalMapping,
                        onClick = { activeTab = MainTab.GeologicalMapping },
                        label = { Text("Mapping", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Terrain, contentDescription = "Geology mapping") }
                    )
                    NavigationBarItem(
                        selected = activeTab == MainTab.BoreholeLogging,
                        onClick = { activeTab = MainTab.BoreholeLogging },
                        label = { Text("Borehole", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Layers, contentDescription = "Borehole core log") }
                    )
                    NavigationBarItem(
                        selected = activeTab == MainTab.RockMechanics,
                        onClick = { activeTab = MainTab.RockMechanics },
                        label = { Text("Rock Mech", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Rock mechanics analysis") }
                    )
                    NavigationBarItem(
                        selected = activeTab == MainTab.Map,
                        onClick = { activeTab = MainTab.Map },
                        label = { Text("Map", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Map, contentDescription = "Interactive grid map") }
                    )
                    NavigationBarItem(
                        selected = activeTab == MainTab.Tools,
                        onClick = { activeTab = MainTab.Tools },
                        label = { Text("Tools", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Build, contentDescription = "Field tools and contact links") }
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
                when (activeTab) {
                    MainTab.Home -> {
                        HomeDashboardContent(
                            projectsCount = projectList.size,
                            stationsCount = allStations.size,
                            observationsCount = allObservationsByState.size,
                            samplesCount = allSamplesByState.size,
                            allObservations = allObservationsByState,
                            viewModel = viewModel,
                            context = context,
                            onNavigateToTab = { activeTab = it }
                        )
                    }
                    MainTab.BoreholeLogging -> {
                        BoreholeLoggingContent(viewModel)
                    }
                    MainTab.RockMechanics -> {
                        RockMechanicsContent(viewModel)
                    }
                    MainTab.Map -> {
                        MapContent(viewModel)
                    }
                    MainTab.Tools -> {
                        ToolsContent(viewModel)
                    }
                    MainTab.GeologicalMapping -> {
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
                                        onUpdateObservation = {
                                            viewModel.updateObservation(it)
                                        },
                                        onAddObservationClick = {
                                            viewModel.navigateTo(Screen.AddObservation(activeStation?.id ?: 0))
                                        },
                                        onDeleteObservation = {
                                            viewModel.deleteObservation(it)
                                        },
                                        onUpdateStation = {
                                            viewModel.updateStation(it)
                                        },
                                        coordSystemSetting = coordSystemSetting
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
                                                dipDirection = details["dipDirection"] as? Int,
                                                rake = details["rake"] as? Int,
                                                rakeDirection = details["rakeDirection"] as? String,
                                                movement = details["movement"] as? String,
                                                width = details["width"] as? Double,

                                                // Joint
                                                jointType = details["jointType"] as? String,
                                                jointSpacing = details["jointSpacing"] as? Double,

                                                // Vein
                                                veinType = details["veinType"] as? String,
                                                veinMineralogy = details["veinMineralogy"] as? String,

                                                // Structure
                                                structureType = details["structureType"] as? String,

                                                // Mineralization
                                                minStyle = details["minStyle"] as? String,
                                                minAssociatedMinerals = details["minAssociatedMinerals"] as? String,
                                                minGradeEstimate = details["minGradeEstimate"] as? String,

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
                                        },
                                        coordSystemSetting = coordSystemSetting
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GeoField Settings & Data Hub",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Appearance Settings (Outdoor Optimized):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(2.dp))
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Theme Setting Selectors
                                    Text("App Theme:", fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(
                                            "Light" to "☀️ Light",
                                            "Dark" to "🌙 Dark",
                                            "HighContrast" to "⚡ Contrast",
                                            "Silver" to "🔘 Silver"
                                        ).forEach { (themeId, label) ->
                                            val isSelected = themeSetting == themeId
                                            Button(
                                                onClick = { viewModel.setThemeSetting(themeId) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                ),
                                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f).height(30.dp),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Font Size Setting Selectors
                                    Text("Font Size:", fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("Standard" to "Standard", "Large" to "Large", "Huge" to "Huge").forEach { (sizeId, label) ->
                                            val isSelected = fontSizeSetting == sizeId
                                            Button(
                                                onClick = { viewModel.setFontSizeSetting(sizeId) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                ),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f).height(30.dp),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Coordinate System Selectors
                                    Text("Display Coordinate System:", fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("UTM" to "Universal UTM", "WGS84_DECIMAL" to "WGS84 Decimal").forEach { (coordId, label) ->
                                            val isSelected = coordSystemSetting == coordId
                                            Button(
                                                onClick = { viewModel.setCoordSystemSetting(coordId) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                ),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f).height(30.dp),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Divider()
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Database Maintenance & Demo Seeder:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Load extensive exploratory and hypothetical demo data to test and master core logging drills, geological mappings, fault lines, and porphyry mineralization scenarios:",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.seedDemoDataset(context)
                                            showSettingsDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Seed Exploratory Demo Dataset", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        item {
                            Divider()
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Export & Share Hub:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(2.dp))
                        }

                        if (projectList.isEmpty()) {
                            item {
                                Text("No projects found to export or share.", fontSize = 10.sp, color = Color.Gray)
                            }
                        } else {
                            items(projectList) { proj ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = proj.name, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = "Last Saved: ${proj.date ?: ""}", fontSize = 9.sp, color = Color.Gray)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Excel XLS
                                            Button(
                                                onClick = { viewModel.exportProjectData(context, proj.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                                                Spacer(Modifier.width(2.dp))
                                                Text("Excel", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            // PDF Report
                                            Button(
                                                onClick = { viewModel.exportPdfReport(context, proj.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                                                Spacer(Modifier.width(2.dp))
                                                Text("PDF", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            // CSV
                                            Button(
                                                onClick = { viewModel.exportCsv(context, proj.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                                                Spacer(Modifier.width(2.dp))
                                                Text("CSV", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            // ZIP Package
                                            Button(
                                                onClick = { viewModel.exportProjectZip(context, proj.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                                                Spacer(Modifier.width(2.dp))
                                                Text("ZIP", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // KML GIS
                                            Button(
                                                onClick = { viewModel.exportKml(context, proj.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                                                Spacer(Modifier.width(2.dp))
                                                Text("KML Map", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            // KMZ GIS Pack
                                            Button(
                                                onClick = { viewModel.exportKmz(context, proj.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                                                Spacer(Modifier.width(2.dp))
                                                Text("KMZ (Orients + Photos)", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // AGS Engineering Geotechnical Format
                                            Button(
                                                onClick = { viewModel.exportProjectAgs(context, proj.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Icon(Icons.Default.Schema, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                                                Spacer(Modifier.width(2.dp))
                                                Text("AGS v4 Format", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            // Surpac/Leapfrog 3D modeling CSV zip
                                            Button(
                                                onClick = { viewModel.exportProjectSurpacCsv(context, proj.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2)),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                                                Spacer(Modifier.width(2.dp))
                                                Text("Leapfrog 3D CSV", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Divider()
                            Spacer(Modifier.height(2.dp))
                            Button(
                                onClick = {
                                    showSettingsDialog = false
                                    coroutineScope.launch {
                                        // Inject premium simulated samples to the database for geological field studies!
                                        val projId = viewModel.dao.insertProject(
                                            Project(
                                                name = "Sungun Copper-Gold Porphyry Project",
                                                description = "West Alborz - Azerbaijan structural zone. Hydrothermal alteration and porphyry ore deposits survey area.",
                                                date = "2026-06-04",
                                                utmZoneMode = "MANUAL",
                                                utmZoneManual = "38S"
                                            )
                                        )
                                        val st1 = viewModel.dao.insertStation(
                                            Station(
                                                projectId = projId.toInt(),
                                                stationCode = "SG-ST01",
                                                prefix = "SG",
                                                latitude = 38.7180,
                                                longitude = 46.7214,
                                                elevation = 1680.0,
                                                accuracy = 3.2,
                                                elevationAccuracy = 1.5,
                                                utmEasting = 649641.0,
                                                utmNorthing = 4287012.0,
                                                utmZone = "38S"
                                            )
                                        )
                                        viewModel.dao.insertObservation(
                                            Observation(
                                                stationId = st1.toInt(),
                                                observationType = "Alteration",
                                                notes = "Intense potassic alteration accompanied by intense pyrite and chalcopyrite mineralization in Diorite Porphyry host rock.",
                                                photoPath = null,
                                                alterationType = "Potassic",
                                                intensity = "Strong",
                                                hostRock = "Diorite Porphyry",
                                                associatedMinerals = "K-Feldspar, Biotite, Chalcopyrite, Quartz"
                                            )
                                        )
                                        viewModel.dao.insertObservation(
                                            Observation(
                                                stationId = st1.toInt(),
                                                observationType = "Lithology",
                                                notes = "Holocrystalline porphyritic texture, plagioclase phenocrysts, and intergrown biotites in a potassic groundmass.",
                                                photoPath = null,
                                                lithologyType = "Granodiorite Porphyry",
                                                texture = "Porphyritic",
                                                color = "Greyish pink",
                                                grainSize = "Medium to Coarse",
                                                lithologyAlteration = "Chloritized biotites"
                                            )
                                        )
                                        Toast.makeText(context, "Demo Sungun exploration campaign loaded successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Load Demo Sungun Copper Mine Project", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        item {
                            Spacer(Modifier.height(2.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("Project Sponsor & Developer: Farzad Fathollahzadeh", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    Text("GeoField Mine Observation Platform", fontSize = 9.sp)
                                    Text("Developed June 2026 (v3.2.0 • Offline GIS)", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }


        if (showExportDialog) {
            val activeProjId = activeProject?.id
            if (activeProjId != null) {
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Export Project Datasets",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Choose the format to export your field data. All datasets are packaged offline and shared directly from internal storage.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // Option 1: Excel Workbook
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showExportDialog = false
                                        viewModel.exportProjectData(context, activeProjId)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Excel Workbook (.xlsx)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Multi-sheet spreadsheet format with structural tables.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Option 2: Complete GIS ZIP Package
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showExportDialog = false
                                        viewModel.exportProjectZip(context, activeProjId)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Archive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Complete GIS ZIP Package",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Bridges to QGIS/ArcGIS. Contains Excel workbook, separate Georeferenced CSV sheets, and all geotagged photos.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Option 3: KML GIS Map
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showExportDialog = false
                                        viewModel.exportKml(context, activeProjId)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "KML GIS Vector Map File",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Standard KML file with styled geological points categorized by lithology. Compatible with Google Earth and QGIS.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Option 4: KMZ Map Pack (With Photos)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showExportDialog = false
                                        viewModel.exportKmz(context, activeProjId)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "KMZ Interactive Tour Map Package",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Rich KMZ archive containing complete styled database and embedded field/sample photos inside popup markers.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Option 5: AGS v4 Format
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showExportDialog = false
                                        viewModel.exportProjectAgs(context, activeProjId)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schema,
                                        contentDescription = null,
                                        tint = Color(0xFF00796B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "AGS v4 Geotechnical Transfer (.ags)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "International industry-standard format for transferring ground investigation / borehole and laboratory data.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Option 6: Leapfrog 3D Modeling Tables
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showExportDialog = false
                                        viewModel.exportProjectSurpacCsv(context, activeProjId)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dashboard,
                                        contentDescription = null,
                                        tint = Color(0xFF7E57C2),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Leapfrog / Surpac 3D Import Tables (.zip)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Generates perfectly formatted Collar and Interval CSV tables for rapid import into 3D modeling programs.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showExportDialog = false }) {
                            Text("CLOSE", fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                )
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
                text = "OFFLINE-FIRST FIELD DATA LOGGER • v3.1.1",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
    }
}

// ==========================================
// DEVELOPER SOCIAL & COMMUNITY PANEL PANEL
// ==========================================
@Composable
fun DeveloperSocialPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "GeoField / ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "PROJECT GEOFIELD PRO COMMUNITY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "Developer: Farzad Fathollazadeh (farzad-f)",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Telegram button
                Button(
                    onClick = { openBrowserLink(context, "https://t.me/geofieldpro") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0088CC),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Telegram", modifier = Modifier.size(11.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Telegram", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                
                // GitHub button
                Button(
                    onClick = { openBrowserLink(context, "https://github.com/farzad-f") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF24292E),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = "GitHub", modifier = Modifier.size(11.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GitHub", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                
                // LinkedIn button
                Button(
                    onClick = { openBrowserLink(context, "https://www.linkedin.com/in/farzadff") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0077B5),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "LinkedIn", modifier = Modifier.size(11.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LinkedIn", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
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
    val context = LocalContext.current
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
                Spacer(modifier = Modifier.height(16.dp))
                DeveloperSocialPanel(modifier = Modifier.fillMaxWidth(0.8f))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                var searchQuery by remember { mutableStateOf("") }
                val filteredProjects = remember(projects, searchQuery) {
                    if (searchQuery.isBlank()) {
                        projects
                    } else {
                        projects.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            (it.description?.contains(searchQuery, ignoreCase = true) ?: false)
                        }
                    }
                }

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
                        text = "v3.1.1",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("project_search_input"),
                    placeholder = { Text("Search in projects...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (filteredProjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Projects Found",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No projects match your search keywords.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProjects) { project ->
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
                Spacer(modifier = Modifier.height(12.dp))
                DeveloperSocialPanel(modifier = Modifier.fillMaxWidth())
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

    var showEditProjDialog by remember { mutableStateOf(false) }
    var editProjName by remember { mutableStateOf(project.name) }
    var editProjDesc by remember { mutableStateOf(project.description ?: "") }
    val coordSystemSetting by viewModel.coordSystemSetting.collectAsStateWithLifecycle()

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            editProjName = project.name
                            editProjDesc = project.description ?: ""
                            showEditProjDialog = true
                        },
                        modifier = Modifier.size(36.dp).testTag("edit_project_dashboard_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Project Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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
                                    text = if (coordSystemSetting == "WGS84_DECIMAL") {
                                        String.format(Locale.US, "%.6f°, %.6f°", station.latitude, station.longitude)
                                    } else {
                                        "Zone ${station.utmZone} | ${String.format(Locale.US, "%.0f E, %.0f N", station.utmEasting, station.utmNorthing)}"
                                    },
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

        if (showEditProjDialog) {
            AlertDialog(
                onDismissRequest = { showEditProjDialog = false },
                title = { Text("Edit Project Info") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editProjName,
                            onValueChange = { editProjName = it },
                            label = { Text("Project Name") },
                            modifier = Modifier.fillMaxWidth().testTag("edit_project_name_input")
                        )
                        OutlinedTextField(
                            value = editProjDesc,
                            onValueChange = { editProjDesc = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth().testTag("edit_project_desc_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editProjName.isNotBlank()) {
                                viewModel.updateProject(
                                    project.copy(
                                        name = editProjName,
                                        description = editProjDesc.ifBlank { null }
                                    )
                                )
                                showEditProjDialog = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProjDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
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
    val coordSystemSetting by viewModel.coordSystemSetting.collectAsStateWithLifecycle()
    
    val prefixes = listOf("LITH", "ALT", "FLT", "STR", "SMP")
    var selectedPrefix by remember { mutableStateOf("LITH") }
    var generatedCode by remember { mutableStateOf("LITH-001") }
    
    var isManualEditing by remember { mutableStateOf(false) }
    var latInput by remember { mutableStateOf("") }
    var lonInput by remember { mutableStateOf("") }
    var eleInput by remember { mutableStateOf("") }

    LaunchedEffect(gps) {
        if (!isManualEditing && gps.isCaptured) {
            latInput = gps.latitude.toString()
            lonInput = gps.longitude.toString()
            eleInput = gps.elevation.toString()
        }
    }
    
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

    // ==========================================
    // MERGED OBSERVATION & SAMPLE STATES
    // ==========================================
    var includeObservation by remember { mutableStateOf(true) }
    val obsType by remember(selectedPrefix) {
        mutableStateOf(
            when (selectedPrefix) {
                "LITH" -> "Lithology"
                "ALT" -> "Alteration"
                "FLT" -> "Fault"
                "STR" -> "Structure"
                "SMP" -> "Mineralization"
                else -> "Lithology"
            }
        )
    }
    
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
    var dipDirection by remember { mutableStateOf("") }
    var autoCalcDipDir by remember { mutableStateOf(true) }
    var rake by remember { mutableStateOf("") }
    var rakeDirection by remember { mutableStateOf("") }
    var movement by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }

    // Joint State
    var jointTypeInput by remember { mutableStateOf("") }
    var jointSpacingInput by remember { mutableStateOf("") }

    // Vein State
    var veinTypeInput by remember { mutableStateOf("") }
    var veinMineralogyInput by remember { mutableStateOf("") }

    // Structure State
    var foldStructureType by remember { mutableStateOf("") }

    // Mineralization State
    var mineralizationStyle by remember { mutableStateOf("") }
    var associatedOreMinerals by remember { mutableStateOf("") }
    var gradeEstimate by remember { mutableStateOf("") }

    // Sample State
    var sampleTaken by remember(selectedPrefix) { mutableStateOf(selectedPrefix == "SMP") }
    var sampleIdCode by remember { mutableStateOf("") }
    var sampleType by remember { mutableStateOf("Hand specimen") }
    var sampleDesc by remember { mutableStateOf("") }

    // Photo Watermarking States
    var attachedPhotoPathsList by remember { mutableStateOf(emptyList<String>()) }
    var isOverlayEnabled by remember { mutableStateOf(true) }
    var overlayPosition by remember { mutableStateOf("BOTTOM_LEFT") }
    var sightAzimuth by remember { mutableStateOf(180) }
    var activeViewerPhotoPath by remember { mutableStateOf<String?>(null) }

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
                showGpsWarningDialog = true
            } else {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            val path = PhotoProcessor.saveAnnotatedImage(
                                context = context,
                                baseBitmap = bitmap,
                                stationCode = generatedCode,
                                lat = meta.latitude,
                                lon = meta.longitude,
                                elevation = meta.elevation ?: gps.elevation,
                                utmZone = gps.utmZone.ifBlank { "39N" },
                                utmEasting = gps.utmEasting,
                                utmNorthing = gps.utmNorthing,
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
            text = { Text("This gallery image has no GPS metadata coordinates tag. Force inherit current station's coordinates ($generatedCode)?") },
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
                                        stationCode = generatedCode,
                                        lat = gps.latitude,
                                        lon = gps.longitude,
                                        elevation = gps.elevation,
                                        utmZone = gps.utmZone.ifBlank { "39N" },
                                        utmEasting = gps.utmEasting,
                                        utmNorthing = gps.utmNorthing,
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
    LaunchedEffect(sampleTaken, generatedCode) {
        if (sampleTaken && sampleIdCode.isBlank()) {
            sampleIdCode = "${generatedCode}-S1"
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
                    "LITH" to "Lithology Observation (LITH)",
                    "ALT" to "Alteration Mapping (ALT)",
                    "FLT" to "Fault Survey (FLT)",
                    "STR" to "Structural Logging (STR)",
                    "SMP" to "Physical Specimen Sample (SMP)"
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

                if (gps.isCaptured) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .clickable { isManualEditing = !isManualEditing }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Edit Coordinates Manually", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Checkbox(
                            checked = isManualEditing,
                            onCheckedChange = { isManualEditing = it ?: false },
                            modifier = Modifier.size(24.dp).testTag("manual_coord_checkbox")
                        )
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
                        if (isManualEditing) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = latInput,
                                    onValueChange = {
                                        latInput = it
                                        val latVal = it.toDoubleOrNull() ?: gps.latitude
                                        val lonVal = lonInput.toDoubleOrNull() ?: gps.longitude
                                        val eleVal = eleInput.toDoubleOrNull() ?: gps.elevation
                                        viewModel.setManualCoordinates(latVal, lonVal, eleVal, 1.0)
                                    },
                                    label = { Text("Latitude", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1.1f).testTag("lat_input_field"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = lonInput,
                                    onValueChange = {
                                        lonInput = it
                                        val latVal = latInput.toDoubleOrNull() ?: gps.latitude
                                        val lonVal = it.toDoubleOrNull() ?: gps.longitude
                                        val eleVal = eleInput.toDoubleOrNull() ?: gps.elevation
                                        viewModel.setManualCoordinates(latVal, lonVal, eleVal, 1.0)
                                    },
                                    label = { Text("Longitude", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1.1f).testTag("lon_input_field"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = eleInput,
                                    onValueChange = {
                                        eleInput = it
                                        val latVal = latInput.toDoubleOrNull() ?: gps.latitude
                                        val lonVal = lonInput.toDoubleOrNull() ?: gps.longitude
                                        val eleVal = it.toDoubleOrNull() ?: gps.elevation
                                        viewModel.setManualCoordinates(latVal, lonVal, eleVal, 1.0)
                                    },
                                    label = { Text("Elevation (m)", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(0.9f).testTag("ele_input_field"),
                                    singleLine = true
                                )
                            }
                        } else {
                            if (coordSystemSetting == "WGS84_DECIMAL") {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Latitude | Longitude (Primary)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                    Text(
                                        text = String.format(Locale.US, "%.6f°, %.6f°", gps.latitude, gps.longitude),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Latitude | Longitude", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                    Text(
                                        text = String.format(Locale.US, "%.6f°, %.6f°", gps.latitude, gps.longitude),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        if (coordSystemSetting == "WGS84_DECIMAL") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("UTM Coordinates", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                Text(
                                    text = String.format(Locale.US, "%.1f E | %.1f N", gps.utmEasting, gps.utmNorthing),
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("UTM Grid Zone", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                Text(
                                    text = "Zone ${gps.utmZone}", 
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("UTM Coordinates (Primary)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                Text(
                                    text = String.format(Locale.US, "%.1f E | %.1f N", gps.utmEasting, gps.utmNorthing),
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("UTM Grid Zone", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                Text(
                                    text = "Zone ${gps.utmZone}", 
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Elevation (Altitude)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text(
                                text = String.format(Locale.US, "%.2f m", gps.elevation), 
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                        if (gps.elevationAccuracy > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Elevation Error Margin", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                val elevAccColor = when {
                                    gps.elevationAccuracy < 8.0 -> Color(0xFF00A86B)
                                    gps.elevationAccuracy < 20.0 -> Color(0xFFFFB020)
                                    else -> Color(0xFFFF3B30)
                                }
                                Text(
                                    text = String.format(Locale.US, "± %.2f m", gps.elevationAccuracy),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = elevAccColor,
                                    fontSize = 14.sp
                                )
                            }
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

        // COORD CONFIGURATION SYSTEM
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

        // STEP 4: INITIAL OBSERVATION LOGGER
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STEP 4: LOG GEOLOGICAL OBSERVATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Auto-formatted observation of type: $obsType",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = includeObservation,
                        onCheckedChange = { includeObservation = it },
                        modifier = Modifier.testTag("include_observation_toggle")
                    )
                }

                if (includeObservation) {
                    Divider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // DYNAMIC FIELDS BASED ON SELECTED PREFIX-BASED OBSTYPE
                    when (obsType) {
                        "Lithology" -> {
                            AutoCompleteTextField(
                                value = lithType,
                                onValueChange = { lithType = it },
                                label = "Rock / Lithology Type",
                                suggestions = lithologySuggestions,
                                placeholder = "e.g. Granodiorite, Amphibolite, Basalt",
                                testTag = "lithology_type"
                            )

                            AutoCompleteTextField(
                                value = texture,
                                onValueChange = { texture = it },
                                label = "Texture / Fabric",
                                suggestions = textureSuggestions,
                                placeholder = "e.g. Porphyritic, Foliated, Gneissic"
                            )

                            AutoCompleteTextField(
                                value = color,
                                onValueChange = { color = it },
                                label = "Color",
                                suggestions = colorSuggestions,
                                placeholder = "e.g. Dark grey, Pinkish grey, Terracotta"
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

                            AutoCompleteTextField(
                                value = lithAlt,
                                onValueChange = { lithAlt = it },
                                label = "Alteration Style",
                                suggestions = alterationSuggestions,
                                placeholder = "e.g. Weak Chlorite, Argillic"
                            )

                            AutoCompleteTextField(
                                value = lithMin,
                                onValueChange = { lithMin = it },
                                label = "Mineralization Index",
                                suggestions = alterationMineralsSuggestions,
                                placeholder = "e.g. 2% Pyrite cubes, Malachite staining"
                            )
                        }
                        
                        "Alteration" -> {
                            AutoCompleteTextField(
                                value = altType,
                                onValueChange = { altType = it },
                                label = "Alteration Assembly",
                                suggestions = alterationSuggestions,
                                placeholder = "e.g. Quartz-Sericite-Pyrite (Phyllic)",
                                testTag = "alteration_type"
                            )

                            // Intensity Select
                            val intensities = listOf("Strong", "Moderate", "Weak")
                            val isDark = isSystemInDarkTheme()
                            Text("Alteration Intensity", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                            AutoCompleteTextField(
                                value = altMinerals,
                                onValueChange = { altMinerals = it },
                                label = "Associated Alteration Minerals",
                                suggestions = alterationMineralsSuggestions,
                                placeholder = "e.g. Sericite, Jarosite, Alunite"
                            )
                        }

                        "Fault" -> {
                            AutoCompleteTextField(
                                value = faultType,
                                onValueChange = { faultType = it },
                                label = "Fault Type / Kinematics",
                                suggestions = faultSuggestions,
                                placeholder = "e.g. Strike-Slip - Dextral, Normal Fault",
                                testTag = "fault_type"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = strike,
                                    onValueChange = {
                                        strike = it
                                        if (autoCalcDipDir) {
                                            val sI = it.toIntOrNull()
                                            if (sI != null) {
                                                dipDirection = String.format(Locale.ROOT, "%03d", (sI + 90) % 360)
                                            }
                                        }
                                    },
                                    label = { Text("Strike (0-360°)") },
                                    placeholder = { Text("045") },
                                    modifier = Modifier.weight(1f).testTag("strike_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = dip,
                                    onValueChange = { dip = it },
                                    label = { Text("Dip (0-90°)") },
                                    placeholder = { Text("60") },
                                    modifier = Modifier.weight(1f).testTag("dip_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = dipDirection,
                                    onValueChange = { dipDirection = it },
                                    label = { Text("Dip Dir (0-360°)") },
                                    placeholder = { Text("135") },
                                    modifier = Modifier.weight(1f).testTag("dip_direction_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }

                            // RHR Auto-calculation checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { autoCalcDipDir = !autoCalcDipDir }
                            ) {
                                Checkbox(
                                    checked = autoCalcDipDir,
                                    onCheckedChange = { autoCalcDipDir = it },
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auto-calc Dip Dir via Right-Hand Rule (RHR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            // Interactive Geological compass dial
                            GeologicalCompassDial(
                                strike = strike,
                                dip = dip,
                                dipDirection = dipDirection,
                                onStrikeChange = { strike = it },
                                onDipChange = { dip = it },
                                onDipDirectionChange = { dipDirection = it },
                                autoCalcDipDir = autoCalcDipDir
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = rake,
                                    onValueChange = { rake = it },
                                    label = { Text("Rake / Pitch (0-90°)") },
                                    placeholder = { Text("35") },
                                    modifier = Modifier.weight(1.3f).testTag("rake_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = rakeDirection,
                                    onValueChange = { rakeDirection = it },
                                    label = { Text("Rake Dir") },
                                    placeholder = { Text("NW, SE") },
                                    modifier = Modifier.weight(1f).testTag("rake_dir_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = movement,
                                    onValueChange = { movement = it },
                                    label = { Text("Sense of Movement") },
                                    placeholder = { Text("e.g. Slickenside microsteps") },
                                    modifier = Modifier.weight(1.3f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = width,
                                    onValueChange = { width = it },
                                    label = { Text("Width (cm)") },
                                    placeholder = { Text("15") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }

                        "Structure" -> {
                            val isFlt = selectedPrefix == "FLT"
                            val isAlt = selectedPrefix == "ALT"
                            val isLith = selectedPrefix == "LITH"
                            
                            // Let geologist choose if it's bed/joint/vein structural data
                            var selectedStructTab by remember { mutableStateOf("Bedding") }
                            val tabChoices = listOf("Bedding", "Joint", "Vein", "Foliation")
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                tabChoices.forEach { tab ->
                                    val act = selectedStructTab == tab
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (act) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { selectedStructTab = tab }
                                            .border(1.dp, if (act) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(tab, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (act) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            when (selectedStructTab) {
                                "Bedding" -> {
                                    AutoCompleteTextField(
                                        value = foldStructureType,
                                        onValueChange = { foldStructureType = it },
                                        label = "Bedding Structure Type",
                                        suggestions = structureSuggestions,
                                        placeholder = "e.g. Bedding S0"
                                    )
                                }
                                "Joint" -> {
                                    AutoCompleteTextField(
                                        value = jointTypeInput,
                                        onValueChange = { jointTypeInput = it },
                                        label = "Joint Structure Set",
                                        suggestions = jointSuggestions,
                                        placeholder = "e.g. Systematic Joint Set A"
                                    )
                                    OutlinedTextField(
                                        value = jointSpacingInput,
                                        onValueChange = { jointSpacingInput = it },
                                        label = { Text("Joint Spacing (cm)") },
                                        placeholder = { Text("35") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                                "Vein" -> {
                                    AutoCompleteTextField(
                                        value = veinTypeInput,
                                        onValueChange = { veinTypeInput = it },
                                        label = "Vein Style",
                                        suggestions = veinSuggestions,
                                        placeholder = "e.g. Quartz Tension Veinlets"
                                    )
                                    AutoCompleteTextField(
                                        value = veinMineralogyInput,
                                        onValueChange = { veinMineralogyInput = it },
                                        label = "Vein Mineralogy",
                                        suggestions = alterationMineralsSuggestions,
                                        placeholder = "e.g. Quartz + Sulfides"
                                    )
                                    OutlinedTextField(
                                        value = width,
                                        onValueChange = { width = it },
                                        label = { Text("Vein Width (cm)") },
                                        placeholder = { Text("12.5") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                                "Foliation" -> {
                                    OutlinedTextField(
                                        value = foldStructureType,
                                        onValueChange = { foldStructureType = it },
                                        label = { Text("Foliation / Symbol Type") },
                                        placeholder = { Text("e.g. Cleavage S1, Surturing Boundary") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = strike,
                                    onValueChange = {
                                        strike = it
                                        if (autoCalcDipDir) {
                                            val sI = it.toIntOrNull()
                                            if (sI != null) {
                                                dipDirection = String.format(Locale.ROOT, "%03d", (sI + 90) % 360)
                                            }
                                        }
                                    },
                                    label = { Text("Strike / Trend (0-360°)") },
                                    placeholder = { Text("270") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = dip,
                                    onValueChange = { dip = it },
                                    label = { Text("Dip / Plunge (0-90°)") },
                                    placeholder = { Text("30") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = dipDirection,
                                    onValueChange = { dipDirection = it },
                                    label = { Text("Dip Dir (0-360°)") },
                                    placeholder = { Text("360") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }

                            // RHR Auto-calculation checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { autoCalcDipDir = !autoCalcDipDir }
                            ) {
                                Checkbox(
                                    checked = autoCalcDipDir,
                                    onCheckedChange = { autoCalcDipDir = it },
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auto-calc Dip Dir via Right-Hand Rule (RHR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            // Interactive Geological compass dial
                            GeologicalCompassDial(
                                strike = strike,
                                dip = dip,
                                dipDirection = dipDirection,
                                onStrikeChange = { strike = it },
                                onDipChange = { dip = it },
                                onDipDirectionChange = { dipDirection = it },
                                autoCalcDipDir = autoCalcDipDir
                            )
                        }

                        "Mineralization" -> {
                            AutoCompleteTextField(
                                value = mineralizationStyle,
                                onValueChange = { mineralizationStyle = it },
                                label = "Mineralization Index Style",
                                suggestions = mineralizationSuggestions,
                                placeholder = "e.g. Disseminated, Clotted veins"
                            )

                            AutoCompleteTextField(
                                value = associatedOreMinerals,
                                onValueChange = { associatedOreMinerals = it },
                                label = "Associated Ore Minerals",
                                suggestions = alterationMineralsSuggestions,
                                placeholder = "e.g. Chalcopyrite, Bornite, Pyrite"
                            )

                            OutlinedTextField(
                                value = gradeEstimate,
                                onValueChange = { gradeEstimate = it },
                                label = { Text("Field Visual Grade Estimate") },
                                placeholder = { Text("e.g. 1.2% Cu, 0.4 ppm Au") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Detailed Observation Notes") },
                        placeholder = { Text("Mention structures, grain patterns, boundaries, visual textures, etc...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(85.dp)
                            .testTag("observation_notes_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // SOLID SPECIMEN NESTED logger
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, if (sampleTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Layers, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Log Solid Specimen Sample", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Record physical sample taken at this station", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = sampleTaken,
                                    onCheckedChange = { sampleTaken = it },
                                    modifier = Modifier.scale(0.85f).testTag("sample_taken_toggle")
                                )
                            }

                            if (sampleTaken) {
                                OutlinedTextField(
                                    value = sampleIdCode,
                                    onValueChange = { sampleIdCode = it },
                                    label = { Text("Sample ID Code") },
                                    modifier = Modifier.fillMaxWidth().testTag("sample_id_field"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                val sampleChoices = listOf("Hand specimen", "Grab sample", "Channel sample", "Drill Core", "Soil")
                                var expandedSmpTypes by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(
                                        onClick = { expandedSmpTypes = true },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Specimen Type: $sampleType", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    DropdownMenu(expanded = expandedSmpTypes, onDismissRequest = { expandedSmpTypes = false }) {
                                        sampleChoices.forEach { sc ->
                                            DropdownMenuItem(text = { Text(sc, fontSize = 12.sp) }, onClick = { sampleType = sc; expandedSmpTypes = false })
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = sampleDesc,
                                    onValueChange = { sampleDesc = it },
                                    label = { Text("Sample Description & Purpose") },
                                    placeholder = { Text("zircon dating, assaying, core log") },
                                    modifier = Modifier.fillMaxWidth().testTag("sample_description"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // CAMERA ATTACHMENTS
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("WATERMARKED FIELD EVIDENCE CAM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Badge { Text("${attachedPhotoPathsList.size} Photos") }
                            }

                            if (attachedPhotoPathsList.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(60.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(0.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Text("No geocut evidence photos attached yet", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    items(attachedPhotoPathsList) { path ->
                                        Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(10.dp))) {
                                            AsyncImage(
                                                model = path,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize().clickable { activeViewerPhotoPath = path },
                                                contentScale = ContentScale.Crop
                                            )
                                            IconButton(
                                                onClick = { attachedPhotoPathsList = attachedPhotoPathsList.filter { it != path } },
                                                modifier = Modifier.align(Alignment.TopEnd).size(22.dp).background(Color.Black.copy(0.4f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Close, null, modifier = Modifier.size(10.dp), tint = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Stamping Stamp Overlay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Switch(checked = isOverlayEnabled, onCheckedChange = { isOverlayEnabled = it }, modifier = Modifier.scale(0.75f))
                            }

                            Button(
                                onClick = { pickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Snap/Select Photo from Storage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Text("Observation form is bypassed. Only coordinate locking is performed.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f), fontSize = 12.sp)
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
                        val notesVal = notes
                        val photoPathVal = if (attachedPhotoPathsList.isNotEmpty()) attachedPhotoPathsList.filter { it.isNotBlank() }.joinToString(",") else null
                        
                        var lType: String? = null
                        var tex: String? = null
                        var col: String? = null
                        var gSize: String? = null
                        var lAlt: String? = null
                        var lMin: String? = null
                        
                        var aType: String? = null
                        var aInt: String? = null
                        var aHost: String? = null
                        var aMin: String? = null
                        
                        var fType: String? = null
                        var jType: String? = null
                        var jSp: Double? = null
                        var vType: String? = null
                        var vMin: String? = null
                        var sType: String? = null
                        var mStyle: String? = null
                        var mAssoc: String? = null
                        var mGrade: String? = null
                        
                        var strk: Int? = null
                        var dp: Int? = null
                        var dpDir: Int? = null
                        var rk: Int? = null
                        var rkDir: String? = null
                        var mvt: String? = null
                        var wdh: Double? = null

                        if (includeObservation) {
                            when (obsType) {
                                "Lithology" -> {
                                    lType = lithType.ifBlank { "Unnamed Lithology" }
                                    tex = texture.ifBlank { "N/A" }
                                    col = color.ifBlank { "N/A" }
                                    gSize = grainSize
                                    lAlt = lithAlt.ifBlank { null }
                                    lMin = lithMin.ifBlank { null }
                                }
                                "Alteration" -> {
                                    aType = altType.ifBlank { "Unnamed Alteration" }
                                    aInt = altIntensity
                                    aHost = altHostRock.ifBlank { "N/A" }
                                    aMin = altMinerals.ifBlank { "N/A" }
                                }
                                "Fault" -> {
                                    fType = faultType.ifBlank { "Unnamed Fault" }
                                    strk = strike.toIntOrNull()
                                    dp = dip.toIntOrNull()
                                    dpDir = dipDirection.toIntOrNull()
                                    rk = rake.toIntOrNull()
                                    rkDir = rakeDirection.ifBlank { null }
                                    mvt = movement.ifBlank { null }
                                    wdh = width.toDoubleOrNull()
                                }
                                "Joint" -> {
                                    jType = jointTypeInput.ifBlank { "Systematic Set" }
                                    jSp = jointSpacingInput.toDoubleOrNull()
                                    strk = strike.toIntOrNull()
                                    dp = dip.toIntOrNull()
                                    dpDir = dipDirection.toIntOrNull()
                                }
                                "Vein" -> {
                                    vType = veinTypeInput.ifBlank { "Tension Vein" }
                                    vMin = veinMineralogyInput.ifBlank { "Quartz" }
                                    wdh = width.toDoubleOrNull()
                                    strk = strike.toIntOrNull()
                                    dp = dip.toIntOrNull()
                                    dpDir = dipDirection.toIntOrNull()
                                }
                                "Structure" -> {
                                    sType = foldStructureType.ifBlank { "Bedding S0" }
                                    strk = strike.toIntOrNull()
                                    dp = dip.toIntOrNull()
                                    dpDir = dipDirection.toIntOrNull()
                                }
                                "Mineralization" -> {
                                    mStyle = mineralizationStyle.ifBlank { "Disseminated" }
                                    mAssoc = associatedOreMinerals.ifBlank { "N/A" }
                                    mGrade = gradeEstimate.ifBlank { "N/A" }
                                }
                            }
                        }

                        if (includeObservation) {
                            viewModel.saveStationWithObservation(
                                projectId = projectId,
                                stationCode = generatedCode,
                                prefix = selectedPrefix,
                                obsType = obsType,
                                notes = notesVal,
                                photoPath = photoPathVal,
                                
                                lithType = lType,
                                texture = tex,
                                color = col,
                                grainSize = gSize,
                                lithAlteration = lAlt,
                                lithMineralization = lMin,
                                
                                altType = aType,
                                altIntensity = aInt,
                                hostRock = aHost,
                                altAssociated = aMin,
                                
                                faultType = fType,
                                strike = strk,
                                dip = dp,
                                dipDirection = dpDir,
                                rake = rk,
                                rakeDirection = rkDir,
                                movement = mvt,
                                width = wdh,
                                
                                jointType = jType,
                                jointSpacing = jSp,
                                
                                veinType = vType,
                                veinMineralogy = vMin,
                                
                                structureType = sType,
                                
                                minStyle = mStyle,
                                minAssociatedMinerals = mAssoc,
                                minGradeEstimate = mGrade,
                                
                                sampleTaken = sampleTaken,
                                sampleIdCode = sampleIdCode,
                                sampleType = sampleType,
                                sampleDesc = sampleDesc,
                                
                                onSaved = onSaved
                            )
                        } else {
                            viewModel.saveStation(projectId, generatedCode, selectedPrefix, onSaved)
                        }
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

    // Fullscreen viewing popup
    if (activeViewerPhotoPath != null) {
        Dialog(
            onDismissRequest = { activeViewerPhotoPath = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = activeViewerPhotoPath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clickable { activeViewerPhotoPath = null },
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { activeViewerPhotoPath = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
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
    onUpdateObservation: (Observation) -> Unit,
    onAddObservationClick: () -> Unit,
    onDeleteObservation: (Int) -> Unit,
    onUpdateStation: (Station) -> Unit,
    coordSystemSetting: String = "UTM"
) {
    if (station == null) return

    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(station.createdAt))
    var showEditStationDialog by remember { mutableStateOf(false) }

    var showEditObsDialog by remember { mutableStateOf(false) }
    var editingObservation by remember { mutableStateOf<Observation?>(null) }
    var editObsNotes by remember { mutableStateOf("") }
    
    // Lithology-specific fields
    var editLithType by remember { mutableStateOf("") }
    var editLithTexture by remember { mutableStateOf("") }
    var editLithColor by remember { mutableStateOf("") }
    var editLithGrain by remember { mutableStateOf("") }

    // Alteration-specific fields
    var editAltType by remember { mutableStateOf("") }
    var editAltIntensity by remember { mutableStateOf("") }
    var editAltHost by remember { mutableStateOf("") }

    // Fault-specific fields
    var editFaultType by remember { mutableStateOf("") }
    var editFaultStrike by remember { mutableStateOf("") }
    var editFaultDip by remember { mutableStateOf("") }

    // Joint-specific fields
    var editJointType by remember { mutableStateOf("") }
    var editJointSpacing by remember { mutableStateOf("") }

    // Vein-specific fields
    var editVeinType by remember { mutableStateOf("") }
    var editVeinMineralogy by remember { mutableStateOf("") }
    var editVeinWidth by remember { mutableStateOf("") }

    // Structure-specific fields
    var editStructureType by remember { mutableStateOf("") }

    // Mineralization-specific fields
    var editMinStyle by remember { mutableStateOf("") }
    var editMinAssociatedMinerals by remember { mutableStateOf("") }
    var editMinGradeEstimate by remember { mutableStateOf("") }

    // Generic attitude elements for editing structures
    var editStrike by remember { mutableStateOf("") }
    var editDip by remember { mutableStateOf("") }
    var editDipDirection by remember { mutableStateOf("") }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = station.stationCode,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { showEditStationDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit coordinates",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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
                            text = if (coordSystemSetting == "WGS84_DECIMAL") "GEOGRAPHIC COORDS" else "UTM COORDINATES", 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (coordSystemSetting == "WGS84_DECIMAL") {
                                String.format(Locale.US, "%.6f° N\n%.6f° E", station.latitude, station.longitude)
                            } else {
                                String.format(Locale.US, "%.1f E\n%.1f N\nZone %s", station.utmEasting, station.utmNorthing, station.utmZone)
                            },
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
                    text = if (coordSystemSetting == "WGS84_DECIMAL") {
                        "UTM Grid Zone: ${station.utmZone} • Easting: ${String.format(Locale.US, "%.1f m", station.utmEasting)} • Northing: ${String.format(Locale.US, "%.1f m", station.utmNorthing)}"
                    } else {
                        "WGS84 Lat/Lon: ${String.format(Locale.US, "%.6f, %.6f", station.latitude, station.longitude)}"
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                if (showEditStationDialog) {
                    var codeEdit by remember { mutableStateOf(station.stationCode) }
                    var latEdit by remember { mutableStateOf(station.latitude.toString()) }
                    var lonEdit by remember { mutableStateOf(station.longitude.toString()) }
                    var elevEdit by remember { mutableStateOf(station.elevation.toString()) }
                    var zoneEdit by remember { mutableStateOf(station.utmZone) }
                    var eastEdit by remember { mutableStateOf(station.utmEasting.toString()) }
                    var northEdit by remember { mutableStateOf(station.utmNorthing.toString()) }

                    AlertDialog(
                        onDismissRequest = { showEditStationDialog = false },
                        title = { Text("Edit Survey Station", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = codeEdit,
                                    onValueChange = { codeEdit = it },
                                    label = { Text("Station Code") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = latEdit,
                                    onValueChange = { latEdit = it },
                                    label = { Text("Latitude (WGS84)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = lonEdit,
                                    onValueChange = { lonEdit = it },
                                    label = { Text("Longitude (WGS84)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = elevEdit,
                                    onValueChange = { elevEdit = it },
                                    label = { Text("Elevation (m)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = zoneEdit,
                                    onValueChange = { zoneEdit = it },
                                    label = { Text("UTM Zone") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = eastEdit,
                                    onValueChange = { eastEdit = it },
                                    label = { Text("UTM Easting") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = northEdit,
                                    onValueChange = { northEdit = it },
                                    label = { Text("UTM Northing") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val finalLat = latEdit.toDoubleOrNull() ?: station.latitude
                                    val finalLon = lonEdit.toDoubleOrNull() ?: station.longitude
                                    val finalElev = elevEdit.toDoubleOrNull() ?: station.elevation
                                    val finalEast = eastEdit.toDoubleOrNull() ?: station.utmEasting
                                    val finalNorth = northEdit.toDoubleOrNull() ?: station.utmNorthing
                                    
                                    val updated = station.copy(
                                        stationCode = codeEdit,
                                        latitude = finalLat,
                                        longitude = finalLon,
                                        elevation = finalElev,
                                        utmEasting = finalEast,
                                        utmNorthing = finalNorth,
                                        utmZone = zoneEdit
                                    )
                                    onUpdateStation(updated)
                                    showEditStationDialog = false
                                }
                            ) {
                                Text("Save Changes")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditStationDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
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

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { 
                                                editingObservation = obs
                                                editObsNotes = obs.notes ?: ""
                                                editLithType = obs.lithologyType ?: ""
                                                editLithTexture = obs.texture ?: ""
                                                editLithColor = obs.color ?: ""
                                                editLithGrain = obs.grainSize ?: ""
                                                editAltType = obs.alterationType ?: ""
                                                editAltIntensity = obs.intensity ?: ""
                                                editAltHost = obs.hostRock ?: ""
                                                editFaultType = obs.faultType ?: ""
                                                editFaultStrike = obs.strike?.toString() ?: ""
                                                editFaultDip = obs.dip?.toString() ?: ""

                                                // Joint properties initialization
                                                editJointType = obs.jointType ?: ""
                                                editJointSpacing = obs.jointSpacing?.toString() ?: ""

                                                // Vein properties initialization
                                                editVeinType = obs.veinType ?: ""
                                                editVeinMineralogy = obs.veinMineralogy ?: ""
                                                editVeinWidth = obs.width?.toString() ?: ""

                                                // Structure properties initialization
                                                editStructureType = obs.structureType ?: ""

                                                // Mineralization properties initialization
                                                editMinStyle = obs.minStyle ?: ""
                                                editMinAssociatedMinerals = obs.minAssociatedMinerals ?: ""
                                                editMinGradeEstimate = obs.minGradeEstimate ?: ""

                                                // General attitudes initialization
                                                editStrike = obs.strike?.toString() ?: ""
                                                editDip = obs.dip?.toString() ?: ""
                                                editDipDirection = obs.dipDirection?.toString() ?: ""

                                                showEditObsDialog = true
                                            },
                                            modifier = Modifier.size(24.dp).testTag("edit_observation_item_btn")
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit entry",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteObservation(obs.id) },
                                            modifier = Modifier.size(24.dp).testTag("delete_observation_item_btn")
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete entry",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
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
                                            text = "Attitude: Strike ${obs.strike ?: 0}° / Dip ${obs.dip ?: 0}° Dir ${obs.dipDirection ?: 0}°",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    "Joint" -> {
                                        Text(
                                            text = "${obs.jointType ?: "Joint Fracture Set"}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Spacing: ${obs.jointSpacing?.let { "${it} cm" } ?: "N/A"} | Attitude: Strike ${obs.strike ?: 0}° / Dip ${obs.dip ?: 0}° Dir ${obs.dipDirection ?: 0}°",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    "Vein" -> {
                                        Text(
                                            text = "${obs.veinType ?: "Hydrothermal Vein"}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Mineralogy: ${obs.veinMineralogy ?: "Quartz"} | Width: ${obs.width?.let { "${it} cm" } ?: "N/A"} | Attitude: Strike ${obs.strike ?: 0}° / Dip ${obs.dip ?: 0}°",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    "Structure" -> {
                                        Text(
                                            text = "${obs.structureType ?: "Structural Symbol"}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Attitude: Strike ${obs.strike ?: 0}° / Dip ${obs.dip ?: 0}° Dir ${obs.dipDirection ?: 0}°",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    "Mineralization" -> {
                                        Text(
                                            text = "${obs.minStyle ?: "Ore Mineralization"}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Ore Minerals: ${obs.minAssociatedMinerals ?: "N/A"} | Est. Grade: ${obs.minGradeEstimate ?: "N/A"}",
                                            fontSize = 12.sp,
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

        if (showEditObsDialog && editingObservation != null) {
            val type = editingObservation!!.observationType
            AlertDialog(
                onDismissRequest = { showEditObsDialog = false },
                title = { Text("Edit $type Observation", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editObsNotes,
                            onValueChange = { editObsNotes = it },
                            label = { Text("Field Notes") },
                            modifier = Modifier.fillMaxWidth().testTag("edit_obs_notes")
                        )

                        if (type == "Lithology") {
                            OutlinedTextField(
                                value = editLithType,
                                onValueChange = { editLithType = it },
                                label = { Text("Rock Type / Lithology") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_lith_type")
                            )
                            OutlinedTextField(
                                value = editLithTexture,
                                onValueChange = { editLithTexture = it },
                                label = { Text("Texture") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editLithGrain,
                                onValueChange = { editLithGrain = it },
                                label = { Text("Grain Size") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editLithColor,
                                onValueChange = { editLithColor = it },
                                label = { Text("Color") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (type == "Alteration") {
                            OutlinedTextField(
                                value = editAltType,
                                onValueChange = { editAltType = it },
                                label = { Text("Alteration Style / Minerals") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_alt_type")
                            )
                            OutlinedTextField(
                                value = editAltIntensity,
                                onValueChange = { editAltIntensity = it },
                                label = { Text("Intensity (e.g. Strong)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editAltHost,
                                onValueChange = { editAltHost = it },
                                label = { Text("Host Rock") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (type == "Fault") {
                            OutlinedTextField(
                                value = editFaultType,
                                onValueChange = { editFaultType = it },
                                label = { Text("Fault Type / Kinematics") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_fault_type")
                            )
                            OutlinedTextField(
                                value = editStrike,
                                onValueChange = { editStrike = it },
                                label = { Text("Strike Direction (°)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editDip,
                                onValueChange = { editDip = it },
                                label = { Text("Dip Angle (°)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editDipDirection,
                                onValueChange = { editDipDirection = it },
                                label = { Text("Dip Direction (°)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (type == "Joint") {
                            OutlinedTextField(
                                value = editJointType,
                                onValueChange = { editJointType = it },
                                label = { Text("Joint Structure Type") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editJointSpacing,
                                onValueChange = { editJointSpacing = it },
                                label = { Text("Spacing (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editStrike,
                                onValueChange = { editStrike = it },
                                label = { Text("Strike Direction (°)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editDip,
                                onValueChange = { editDip = it },
                                label = { Text("Dip Angle (°)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (type == "Vein") {
                            OutlinedTextField(
                                value = editVeinType,
                                onValueChange = { editVeinType = it },
                                label = { Text("Vein Style") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editVeinMineralogy,
                                onValueChange = { editVeinMineralogy = it },
                                label = { Text("Vein Mineralogy") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editVeinWidth,
                                onValueChange = { editVeinWidth = it },
                                label = { Text("Width (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editStrike,
                                onValueChange = { editStrike = it },
                                label = { Text("Strike Direction (°)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editDip,
                                onValueChange = { editDip = it },
                                label = { Text("Dip Angle (°)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (type == "Structure") {
                            OutlinedTextField(
                                value = editStructureType,
                                onValueChange = { editStructureType = it },
                                label = { Text("Structural Symbol Type") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editStrike,
                                onValueChange = { editStrike = it },
                                label = { Text("Strike / Trend (°)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editDip,
                                onValueChange = { editDip = it },
                                label = { Text("Dip / Plunge (°)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (type == "Mineralization") {
                            OutlinedTextField(
                                value = editMinStyle,
                                onValueChange = { editMinStyle = it },
                                label = { Text("Mineralization Index Style") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editMinAssociatedMinerals,
                                onValueChange = { editMinAssociatedMinerals = it },
                                label = { Text("Associated Ore Minerals") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editMinGradeEstimate,
                                onValueChange = { editMinGradeEstimate = it },
                                label = { Text("Field Grade Estimate") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val base = editingObservation
                            if (base != null) {
                                val updated = base.copy(
                                    notes = editObsNotes.ifBlank { null },
                                    lithologyType = if (type == "Lithology") editLithType else base.lithologyType,
                                    texture = if (type == "Lithology") editLithTexture else base.texture,
                                    color = if (type == "Lithology") editLithColor else base.color,
                                    grainSize = if (type == "Lithology") editLithGrain else base.grainSize,
                                    alterationType = if (type == "Alteration") editAltType else base.alterationType,
                                    intensity = if (type == "Alteration") editAltIntensity else base.intensity,
                                    hostRock = if (type == "Alteration") editAltHost else base.hostRock,
                                    faultType = if (type == "Fault") editFaultType else base.faultType,
                                    strike = if (type == "Fault" || type == "Joint" || type == "Vein" || type == "Structure") editStrike.toIntOrNull() else base.strike,
                                    dip = if (type == "Fault" || type == "Joint" || type == "Vein" || type == "Structure") editDip.toIntOrNull() else base.dip,
                                    dipDirection = if (type == "Fault" || type == "Joint" || type == "Vein" || type == "Structure") editDipDirection.toIntOrNull() else base.dipDirection,
                                    jointType = if (type == "Joint") editJointType else base.jointType,
                                    jointSpacing = if (type == "Joint") editJointSpacing.toDoubleOrNull() else base.jointSpacing,
                                    veinType = if (type == "Vein") editVeinType else base.veinType,
                                    veinMineralogy = if (type == "Vein") editVeinMineralogy else base.veinMineralogy,
                                    width = if (type == "Vein") editVeinWidth.toDoubleOrNull() else base.width,
                                    structureType = if (type == "Structure") editStructureType else base.structureType,
                                    minStyle = if (type == "Mineralization") editMinStyle else base.minStyle,
                                    minAssociatedMinerals = if (type == "Mineralization") editMinAssociatedMinerals else base.minAssociatedMinerals,
                                    minGradeEstimate = if (type == "Mineralization") editMinGradeEstimate else base.minGradeEstimate
                                )
                                onUpdateObservation(updated)
                                showEditObsDialog = false
                            }
                        },
                        modifier = Modifier.testTag("save_obs_edit_btn")
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditObsDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ==========================================================
// 6. GEOLOGY TRANSLATING DICTIONARY AUTO-SUGGEST & FORM SCREEN
// ==========================================================
val GeologyDictionary = listOf(
    "basalt" to "Basalt",
    "andesite" to "Andesite",
    "fault" to "Fault",
    "quartz" to "Quartz",
    "granite" to "Granite",
    "tuff" to "Tuff",
    "diorite" to "Diorite",
    "limestone" to "Limestone",
    "schist" to "Schist",
    "vein" to "Vein",
    "joint" to "Joint",
    "strike" to "Strike",
    "dip" to "Dip",
    "alteration" to "Alteration",
    "argillic" to "Argillic",
    "potassic" to "Potassic",
    "propyllitic" to "Propyllitic",
    "phylic" to "Phylic",
    "chalcocite" to "Chalcocite",
    "chalcopyrite" to "Chalcopyrite",
    "pyrite" to "Pyrite",
    "magnetite" to "Magnetite",
    "hematite" to "Hematite",
    "malachite" to "Malachite",
    "porphyry" to "Porphyry"
)

@Composable
fun GeologyDictionaryAutoSuggest(
    query: String,
    onSuggestSelected: (String) -> Unit
) {
    if (query.trim().isEmpty()) return
    
    val matches = remember(query) {
        val q = query.lowercase(Locale.US).trim()
        GeologyDictionary.filter { (eng, per) ->
            eng.contains(q) || per.contains(q)
        }.take(3)
    }
    
    if (matches.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook, 
                contentDescription = "Dictionary",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Auto-Suggest:", 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary
            )
            matches.forEach { (eng, per) ->
                AssistChip(
                    onClick = { onSuggestSelected("$eng ($per)") },
                    label = { Text("$eng / $per", fontSize = 10.sp) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    testTag: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(value, suggestions) {
        if (value.trim().isEmpty()) {
            suggestions.take(6)
        } else {
            suggestions.filter { it.contains(value, ignoreCase = true) }.take(6)
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (filteredSuggestions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                filteredSuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion, fontWeight = FontWeight.Medium, fontSize = 13.sp) },
                        onClick = {
                            onValueChange(suggestion)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@Composable
fun AddObservationScreen(
    station: Station?,
    onSave: (String, String, Map<String, Any?>, Boolean, Map<String, String>?) -> Unit,
    onCancel: () -> Unit,
    coordSystemSetting: String = "UTM"
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
    var dipDirection by remember { mutableStateOf("") }
    var autoCalcDipDir by remember { mutableStateOf(true) }
    var rake by remember { mutableStateOf("") }
    var rakeDirection by remember { mutableStateOf("") }
    var movement by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }

    // Joint State
    var jointTypeInput by remember { mutableStateOf("") }
    var jointSpacingInput by remember { mutableStateOf("") }

    // Vein State
    var veinTypeInput by remember { mutableStateOf("") }
    var veinMineralogyInput by remember { mutableStateOf("") }

    // Structure State
    var foldStructureType by remember { mutableStateOf("") }

    // Mineralization State
    var mineralizationStyle by remember { mutableStateOf("") }
    var associatedOreMinerals by remember { mutableStateOf("") }
    var gradeEstimate by remember { mutableStateOf("") }

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

                AutoCompleteTextField(
                    value = lithType,
                    onValueChange = { lithType = it },
                    label = "Rock / Lithology Type",
                    suggestions = lithologySuggestions,
                    placeholder = "e.g. Granodiorite, Amphibolite",
                    testTag = "lithology_type"
                )

                AutoCompleteTextField(
                    value = texture,
                    onValueChange = { texture = it },
                    label = "Texture / Fabric",
                    suggestions = textureSuggestions,
                    placeholder = "e.g. Porphyritic, Foliated, Gneissic"
                )

                AutoCompleteTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = "Color",
                    suggestions = colorSuggestions,
                    placeholder = "e.g. Grey-pink, Dark green"
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

                AutoCompleteTextField(
                    value = lithAlt,
                    onValueChange = { lithAlt = it },
                    label = "Alteration Style",
                    suggestions = alterationSuggestions,
                    placeholder = "e.g. Potassic, Propylitic, Weak Chlorite"
                )

                AutoCompleteTextField(
                    value = lithMin,
                    onValueChange = { lithMin = it },
                    label = "Mineralization Index",
                    suggestions = alterationMineralsSuggestions,
                    placeholder = "e.g. 2% Pyrite cubes, trace Chalcopyrite"
                )

                // Grain Size Reference Assist Panel (Pocket Guide)
                var showGrainSizeGuide by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showGrainSizeGuide = !showGrainSizeGuide },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp).animateContentSize()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Grain Size Pocket Guide (Wentworth Scale)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Icon(
                                imageVector = if (showGrainSizeGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (showGrainSizeGuide) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Standard Petrographic Grain Size Scale (Wentworth System):",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            val scales = listOf(
                                "Very Coarse ➔ > 2 mm (Gravel & Cobbles)",
                                "Coarse ➔ 1 mm - 2 mm (Coarse Sandstone)",
                                "Medium ➔ 0.25 mm - 0.5 mm",
                                "Fine ➔ 0.1 mm - 0.25 mm (Silt & Fine Sandstone)",
                                "Very Fine ➔ < 0.06 mm (Clay & Mud)"
                            )
                            scales.forEach { scale ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.secondary, CircleShape).clip(CircleShape))
                                    Text(scale, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            "Alteration" -> {
                Text("ALTERATION ZONE FORM", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, letterSpacing = 1.sp)

                AutoCompleteTextField(
                    value = altType,
                    onValueChange = { altType = it },
                    label = "Alteration Assembly",
                    suggestions = alterationSuggestions,
                    placeholder = "e.g. Quartz-Sericite-Pyrite (Phyllic)",
                    testTag = "alteration_type"
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

                AutoCompleteTextField(
                    value = altHostRock,
                    onValueChange = { altHostRock = it },
                    label = "Host Rock",
                    suggestions = lithologySuggestions,
                    placeholder = "e.g. Altered Dacite porphyry"
                )

                AutoCompleteTextField(
                    value = altMinerals,
                    onValueChange = { altMinerals = it },
                    label = "Associated Alteration Minerals",
                    suggestions = alterationMineralsSuggestions,
                    placeholder = "e.g. Alunite, Kaolinite, Gypsum"
                )
            }
            "Fault" -> {
                Text("FAULT & SHEAR ZONE MEASUREMENTS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, letterSpacing = 1.sp)
                
                val faultPresets = listOf("Normal", "Reverse", "Strike-Slip", "Thrust", "Shear Zone")
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

                AutoCompleteTextField(
                    value = faultType,
                    onValueChange = { faultType = it },
                    label = "Fault Type / Kinematics",
                    suggestions = faultSuggestions,
                    placeholder = "e.g. Dextral Strike-Slip, Low-angle Thrust",
                    testTag = "fault_type"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = strike,
                        onValueChange = {
                            strike = it
                            if (autoCalcDipDir) {
                                val sI = it.toIntOrNull()
                                if (sI != null) {
                                    dipDirection = String.format(Locale.ROOT, "%03d", (sI + 90) % 360)
                                }
                            }
                        },
                        label = { Text("Strike (0-360°)") },
                        placeholder = { Text("045") },
                        modifier = Modifier.weight(1f).testTag("strike_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = dip,
                        onValueChange = { dip = it },
                        label = { Text("Dip (0-90°)") },
                        placeholder = { Text("60") },
                        modifier = Modifier.weight(1f).testTag("dip_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = dipDirection,
                        onValueChange = { dipDirection = it },
                        label = { Text("Dip Dir (0-360°)") },
                        placeholder = { Text("135") },
                        modifier = Modifier.weight(1f).testTag("dip_direction_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // RHR Auto-calculation checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { autoCalcDipDir = !autoCalcDipDir }
                ) {
                    Checkbox(
                        checked = autoCalcDipDir,
                        onCheckedChange = { autoCalcDipDir = it },
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-calc Dip Dir via Right-Hand Rule (RHR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                // Interactive Geological compass dial
                GeologicalCompassDial(
                    strike = strike,
                    dip = dip,
                    dipDirection = dipDirection,
                    onStrikeChange = { strike = it },
                    onDipChange = { dip = it },
                    onDipDirectionChange = { dipDirection = it },
                    autoCalcDipDir = autoCalcDipDir
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = rake,
                        onValueChange = { rake = it },
                        label = { Text("Rake / Pitch (0-90°)") },
                        placeholder = { Text("35") },
                        modifier = Modifier.weight(1.3f).testTag("rake_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = rakeDirection,
                        onValueChange = { rakeDirection = it },
                        label = { Text("Rake Dir") },
                        placeholder = { Text("NW, SE") },
                        modifier = Modifier.weight(1f).testTag("rake_dir_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = movement,
                        onValueChange = { movement = it },
                        label = { Text("Sense of Movement") },
                        placeholder = { Text("e.g. Sinistral, Slickenside") },
                        modifier = Modifier.weight(1.3f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = width,
                        onValueChange = { width = it },
                        label = { Text("Width (cm)") },
                        placeholder = { Text("15") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
            "Joint" -> {
                Text("GEOLOGICAL JOINT & FRACTURE FORM", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, letterSpacing = 1.sp)
                
                val jointPresets = listOf("Systematic", "Columnar", "Tension Gash", "Conjugate")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    jointPresets.forEach { jt ->
                        AssistChip(
                            onClick = { jointTypeInput = jt },
                            label = { Text(jt, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                AutoCompleteTextField(
                    value = jointTypeInput,
                    onValueChange = { jointTypeInput = it },
                    label = "Joint Structure / Set Type",
                    suggestions = jointSuggestions,
                    placeholder = "e.g. Regional Systematic Joint Set A"
                )

                OutlinedTextField(
                    value = jointSpacingInput,
                    onValueChange = { jointSpacingInput = it },
                    label = { Text("Joint Spacing (cm)") },
                    placeholder = { Text("e.g. 45") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = strike,
                        onValueChange = {
                            strike = it
                            if (autoCalcDipDir) {
                                val sI = it.toIntOrNull()
                                if (sI != null) {
                                    dipDirection = String.format(Locale.ROOT, "%03d", (sI + 90) % 360)
                                }
                            }
                        },
                        label = { Text("Strike (0-360°)") },
                        placeholder = { Text("090") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = dip,
                        onValueChange = { dip = it },
                        label = { Text("Dip (0-90°)") },
                        placeholder = { Text("85") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = dipDirection,
                        onValueChange = { dipDirection = it },
                        label = { Text("Dip Dir (0-360°)") },
                        placeholder = { Text("180") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // RHR Auto-calculation checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { autoCalcDipDir = !autoCalcDipDir }
                ) {
                    Checkbox(
                        checked = autoCalcDipDir,
                        onCheckedChange = { autoCalcDipDir = it },
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-calc Dip Dir via Right-Hand Rule (RHR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                // Interactive Geological compass dial
                GeologicalCompassDial(
                    strike = strike,
                    dip = dip,
                    dipDirection = dipDirection,
                    onStrikeChange = { strike = it },
                    onDipChange = { dip = it },
                    onDipDirectionChange = { dipDirection = it },
                    autoCalcDipDir = autoCalcDipDir
                )
            }
            "Vein" -> {
                Text("HYDROTHERMAL VEIN DESCRIPTION", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, letterSpacing = 1.sp)
                
                val veinPresets = listOf("Stockwork", "Saddle Reef", "Sheeted", "Brecciated")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    veinPresets.forEach { vt ->
                        AssistChip(
                            onClick = { veinTypeInput = vt },
                            label = { Text(vt, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                AutoCompleteTextField(
                    value = veinTypeInput,
                    onValueChange = { veinTypeInput = it },
                    label = "Vein Style",
                    suggestions = veinSuggestions,
                    placeholder = "e.g. Tension Veinlet, Massive Quartz Reef"
                )

                AutoCompleteTextField(
                    value = veinMineralogyInput,
                    onValueChange = { veinMineralogyInput = it },
                    label = "Vein Mineralogy",
                    suggestions = alterationMineralsSuggestions,
                    placeholder = "e.g. Quartz + Fluorite + Chalcopyrite"
                )

                OutlinedTextField(
                    value = width,
                    onValueChange = { width = it },
                    label = { Text("Vein Width (cm)") },
                    placeholder = { Text("e.g. 12.5") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = strike,
                        onValueChange = {
                            strike = it
                            if (autoCalcDipDir) {
                                val sI = it.toIntOrNull()
                                if (sI != null) {
                                    dipDirection = String.format(Locale.ROOT, "%03d", (sI + 90) % 360)
                                }
                            }
                        },
                        label = { Text("Strike (0-360°)") },
                        placeholder = { Text("120") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = dip,
                        onValueChange = { dip = it },
                        label = { Text("Dip (0-90°)") },
                        placeholder = { Text("45") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = dipDirection,
                        onValueChange = { dipDirection = it },
                        label = { Text("Dip Dir (0-360°)") },
                        placeholder = { Text("210") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // RHR Auto-calculation checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { autoCalcDipDir = !autoCalcDipDir }
                ) {
                    Checkbox(
                        checked = autoCalcDipDir,
                        onCheckedChange = { autoCalcDipDir = it },
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-calc Dip Dir via Right-Hand Rule (RHR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                // Interactive Geological compass dial
                GeologicalCompassDial(
                    strike = strike,
                    dip = dip,
                    dipDirection = dipDirection,
                    onStrikeChange = { strike = it },
                    onDipChange = { dip = it },
                    onDipDirectionChange = { dipDirection = it },
                    autoCalcDipDir = autoCalcDipDir
                )
            }
            "Structure" -> {
                Text("STRUCTURAL & BEDDING SYMBOLS FORM", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, letterSpacing = 1.sp)
                
                val structPresets = listOf("Bedding (S0)", "Foliation (S1)", "Cleavage", "Fold Axis")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    structPresets.take(3).forEach { st ->
                        AssistChip(
                            onClick = { foldStructureType = st },
                            label = { Text(st, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                AutoCompleteTextField(
                    value = foldStructureType,
                    onValueChange = { foldStructureType = it },
                    label = "Structural Symbol Type",
                    suggestions = structureSuggestions,
                    placeholder = "e.g. Bedding S0, Fold Hinge Plunge"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = strike,
                        onValueChange = {
                            strike = it
                            if (autoCalcDipDir) {
                                val sI = it.toIntOrNull()
                                if (sI != null) {
                                    dipDirection = String.format(Locale.ROOT, "%03d", (sI + 90) % 360)
                                }
                            }
                        },
                        label = { Text("Strike / Trend (0-360°)") },
                        placeholder = { Text("310") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = dip,
                        onValueChange = { dip = it },
                        label = { Text("Dip / Plunge (0-90°)") },
                        placeholder = { Text("30") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = dipDirection,
                        onValueChange = { dipDirection = it },
                        label = { Text("Dip Dir / Plunge Dir (0-360°)") },
                        placeholder = { Text("040") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // RHR Auto-calculation checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { autoCalcDipDir = !autoCalcDipDir }
                ) {
                    Checkbox(
                        checked = autoCalcDipDir,
                        onCheckedChange = { autoCalcDipDir = it },
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-calc Dip Dir via Right-Hand Rule (RHR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                // Interactive Geological compass dial
                GeologicalCompassDial(
                    strike = strike,
                    dip = dip,
                    dipDirection = dipDirection,
                    onStrikeChange = { strike = it },
                    onDipChange = { dip = it },
                    onDipDirectionChange = { dipDirection = it },
                    autoCalcDipDir = autoCalcDipDir
                )
            }
            "Mineralization" -> {
                Text("MINERALIZATION ORE DEPOSIT FORM", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, letterSpacing = 1.sp)
                
                val minPresets = listOf("Disseminated", "Massive Sulfide", "Stockwork", "Supergene")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    minPresets.forEach { mt ->
                        AssistChip(
                            onClick = { mineralizationStyle = mt },
                            label = { Text(mt, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                AutoCompleteTextField(
                    value = mineralizationStyle,
                    onValueChange = { mineralizationStyle = it },
                    label = "Mineralization Index Style",
                    suggestions = mineralizationSuggestions,
                    placeholder = "e.g. Disseminated Chalcopyrite in potassic zone"
                )

                AutoCompleteTextField(
                    value = associatedOreMinerals,
                    onValueChange = { associatedOreMinerals = it },
                    label = "Associated Ore Minerals",
                    suggestions = alterationMineralsSuggestions,
                    placeholder = "e.g. Pyrite, Chalcopyrite, Bornite, Malachite"
                )

                OutlinedTextField(
                    value = gradeEstimate,
                    onValueChange = { gradeEstimate = it },
                    label = { Text("Field Visual Grade Estimate") },
                    placeholder = { Text("e.g. 1.5% Cu, Trace Au") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            else -> {
                Text("GENERAL FIELD DISCOVERY FORM", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, letterSpacing = 1.sp)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Findings Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
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
                    Text("Select Photo from Phone Storage", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                        "Fault" -> {
                            mapDetails["faultType"] = faultType.ifBlank { "Unnamed Fault" }
                            mapDetails["strike"] = strike.toIntOrNull()
                            mapDetails["dip"] = dip.toIntOrNull()
                            mapDetails["dipDirection"] = dipDirection.toIntOrNull()
                            mapDetails["rake"] = rake.toIntOrNull()
                            mapDetails["rakeDirection"] = rakeDirection.ifBlank { null }
                            mapDetails["movement"] = movement.ifBlank { null }
                            mapDetails["width"] = width.toDoubleOrNull()
                        }
                        "Joint" -> {
                            mapDetails["jointType"] = jointTypeInput.ifBlank { "Systematic Set" }
                            mapDetails["jointSpacing"] = jointSpacingInput.toDoubleOrNull()
                            mapDetails["strike"] = strike.toIntOrNull()
                            mapDetails["dip"] = dip.toIntOrNull()
                            mapDetails["dipDirection"] = dipDirection.toIntOrNull()
                        }
                        "Vein" -> {
                            mapDetails["veinType"] = veinTypeInput.ifBlank { "Tension Vein" }
                            mapDetails["veinMineralogy"] = veinMineralogyInput.ifBlank { "Quartz" }
                            mapDetails["width"] = width.toDoubleOrNull()
                            mapDetails["strike"] = strike.toIntOrNull()
                            mapDetails["dip"] = dip.toIntOrNull()
                            mapDetails["dipDirection"] = dipDirection.toIntOrNull()
                        }
                        "Structure" -> {
                            mapDetails["structureType"] = foldStructureType.ifBlank { "Bedding S0" }
                            mapDetails["strike"] = strike.toIntOrNull()
                            mapDetails["dip"] = dip.toIntOrNull()
                            mapDetails["dipDirection"] = dipDirection.toIntOrNull()
                        }
                        "Mineralization" -> {
                            mapDetails["minStyle"] = mineralizationStyle.ifBlank { "Disseminated" }
                            mapDetails["minAssociatedMinerals"] = associatedOreMinerals.ifBlank { "N/A" }
                            mapDetails["minGradeEstimate"] = gradeEstimate.ifBlank { "N/A" }
                        }
                        else -> {
                            mapDetails["faultType"] = obsType
                            mapDetails["strike"] = strike.toIntOrNull()
                            mapDetails["dip"] = dip.toIntOrNull()
                            mapDetails["dipDirection"] = dipDirection.toIntOrNull()
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
                                    fontWeight = if (coordSystemSetting == "WGS84_DECIMAL") FontWeight.Bold else FontWeight.Medium,
                                    color = if (coordSystemSetting == "WGS84_DECIMAL") Color(0xFF00A86B) else MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("UTM PROJECTION COORDINATES", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "z${station?.utmZone ?: "39N"} ${String.format(Locale.US, "%.1f E", station?.utmEasting ?: 0.0)}",
                                    fontSize = 13.sp,
                                    fontWeight = if (coordSystemSetting == "UTM") FontWeight.Bold else FontWeight.Medium,
                                    color = if (coordSystemSetting == "UTM") Color(0xFF00A86B) else MaterialTheme.colorScheme.onSurface,
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

// =========================================================================
// NEW PLATFORM TABS IMPLEMENTATION (HOME, BOREHOLE, ROCK MECHANICS, GIS MAP)
// =========================================================================

// SHARE EXPORT FILE HELPER
fun shareFileWithApps(context: android.content.Context, fileName: String, mimeType: String, content: String) {
    try {
        val cacheDir = context.cacheDir
        val file = java.io.File(cacheDir, fileName)
        file.writeText(content)
        val fileUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, fileName)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Export Recorded Route Information File"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error sharing file: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

// -------------------------------------------------------------------------
// 1. HOME TAB: STATS OVERVIEW & GENERAL DASHBOARD
// -------------------------------------------------------------------------
@Composable
fun HomeDashboardContent(
    projectsCount: Int,
    stationsCount: Int,
    observationsCount: Int,
    samplesCount: Int,
    allObservations: List<Observation> = emptyList(),
    viewModel: GeologyViewModel,
    context: android.content.Context,
    onNavigateToTab: (MainTab) -> Unit
) {
    val scrollState = rememberScrollState()
    var isGuideExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Title: Stats (Field statistics summary parameters - now at the very top of home dashboard as requested!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Field Statistics Overview",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF39FF14).copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "GPS / RTK FIX: ACTIVE",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF39FF14)
                )
            }
        }

        // High visibility, clean unified statistics panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple("Projects", projectsCount, Color(0xFF1E88E5)),
                    Triple("Stations", stationsCount, Color(0xFF43A047)),
                    Triple("Observations", observationsCount, Color(0xFFE65100)),
                    Triple("Samples", samplesCount, Color(0xFF00ACC1))
                ).forEach { (label, count, color) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Section Title: Workspaces
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active Modules",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // High-fidelity Grid List of modules (Using Row of Columns to stay nested safe and extremely clean)
        val workspaces = listOf(
            Triple("Project Manager", "Projects List", Icons.Default.Folder) to MainTab.GeologicalMapping,
            Triple("Field Mapping", "Geological Observations", Icons.Default.Terrain) to MainTab.GeologicalMapping,
            Triple("Borehole Core Logging", "Core Runs Log", Icons.Default.Layers) to MainTab.BoreholeLogging,
            Triple("Rock Mechanics & RMR", "Geotech Rate assessment", Icons.Default.Assessment) to MainTab.RockMechanics,
            Triple("Interactive Web Map", "Spatial GPS GIS Map", Icons.Default.Map) to MainTab.Map,
            Triple("Geotech Field Tools", "Mineralogy, Rocks, Converters", Icons.Default.Build) to MainTab.Tools
        )

        // Row 1 (first 2 items)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(workspaces[0], workspaces[1]).forEach { (meta, targetTab) ->
                val (title, sub, icon) = meta
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab(targetTab) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = sub, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Row 2 (next 2 items)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(workspaces[2], workspaces[3]).forEach { (meta, targetTab) ->
                val (title, sub, icon) = meta
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab(targetTab) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = sub, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Row 3 (items 4 and 5)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(workspaces[4], workspaces[5]).forEach { (meta, targetTab) ->
                val (title, sub, icon) = meta
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab(targetTab) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = sub, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Recent Field Journal Timeline
        RecentObservationsTimeline(allObservations, onNavigateToTab)

        // Geotechnical Unit Converter Module
        GeotechnicalUnitConverter()

        // Interactive QAPF Ternary Rock Classifier & Plotter Module
        QAPFClassifierPanel()

        // Expandable Guidelines Accordion Panel (Keeps screen perfectly clean, professional and decluttered)
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.animateContentSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isGuideExpanded = !isGuideExpanded }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Geological & Mining Reference Guide",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = if (isGuideExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (isGuideExpanded) {
                    Column(
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        GuideBullet(
                            number = "1",
                            title = "Geological Field Mapping",
                            desc = "Initialize mapping campaigns, record coordinates, describe primary lithology attributes (Texture, Mineralogy), alterations index, and structure strike/dip angles."
                        )
                        GuideBullet(
                            number = "2",
                            title = "Drilling Borehole Core Logger",
                            desc = "Maintain continuous core runs and daily drilling metrics. Input From/To core run intervals and total core recovered to compute recovery and Rock Quality Designation (RQD) instantly."
                        )
                        GuideBullet(
                            number = "3",
                            title = "Rock Geotechnical Mechanics",
                            desc = "Assess rock masses using the Bieniawski RMR Index. Grade UCS, RQD, joint apertures, roughness, ground water inflows, and tunnels lineation orientations"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Text(
            text = "GeoField Pro is a modular, high-precision offline GIS logging and geotech analyzer tool engineered for field geologists, mining engineers, and exploration geophysicists.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )

        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = { uriHandler.openUri("https://github.com/farzadf7/geofield-pro") },
                label = { Text("GitHub Repo", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Code, contentDescription = "GitHub", modifier = Modifier.size(12.dp)) },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )
            AssistChip(
                onClick = { uriHandler.openUri("https://t.me/geofield_pro") },
                label = { Text("Telegram Support", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Send, contentDescription = "Telegram", modifier = Modifier.size(12.dp)) },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = count, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = unit, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun GuideBullet(number: String, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = desc, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
        }
    }
}

// -------------------------------------------------------------------------
// REUSABLE GEOLOGICAL INSTRUMENT & CALCULATION COMPOSABLES
// -------------------------------------------------------------------------
fun getCardinalDirection(angleString: String): String {
    val angle = angleString.toIntOrNull() ?: return "N"
    val normalizedAngle = ((angle % 360) + 360) % 360
    return when (normalizedAngle) {
        in 338..360, in 0..22 -> "N"
        in 23..67 -> "NE"
        in 68..112 -> "E"
        in 113..157 -> "SE"
        in 158..202 -> "S"
        in 203..247 -> "SW"
        in 248..292 -> "W"
        in 293..337 -> "NW"
        else -> "N"
    }
}

fun classifySandstoneQFL(q: Float, f: Float, l: Float): String {
    val total = q + f + l
    if (total <= 0f) return "Sandstone (Indeterminate)"
    val qNorm = (q / total) * 100f
    val fNorm = (f / total) * 100f
    val lNorm = (l / total) * 100f
    
    val fRatio = if ((fNorm + lNorm) > 0f) fNorm / (fNorm + lNorm) else 0.5f

    return when {
        qNorm >= 95f -> "Quartz Arenite (Pure Quartz)"
        qNorm >= 75f -> {
            if (fRatio > 0.5f) "Subarkose (Quartz-Feldspar)"
            else "Sublitharenite (Quartz-Lithic)"
        }
        else -> {
            if (fRatio > 0.5f) "Arkose / Feldspathic Arenite"
            else "Litharenite (Feldspathic-Lithics)"
        }
    }
}

fun classifyPlutonicQAP(q: Float, a: Float, p: Float): String {
    if (q >= 90f) return "Quartzolite"
    if (q >= 60f) return "Quartz-rich Granitoid"
    
    val totalFeldspar = a + p
    if (totalFeldspar <= 0f) return "Plutonic Rock (Indeterminate)"
    val pRatio = p / totalFeldspar
    
    if (q >= 20f) {
        return when {
            pRatio < 0.10f -> "Alkali Feldspar Granite"
            pRatio < 0.35f -> "Syenogranite (Granite)"
            pRatio < 0.65f -> "Monzogranite"
            pRatio < 0.90f -> "Granodiorite"
            else -> "Tonalite"
        }
    } else if (q >= 5f) {
        return when {
            pRatio < 0.10f -> "Quartz Alkali Feldspar Syenite"
            pRatio < 0.35f -> "Quartz Syenite"
            pRatio < 0.65f -> "Quartz Monzonite"
            pRatio < 0.90f -> "Quartz Monzodiorite"
            else -> "Quartz Diorite / Quartz Gabbro"
        }
    } else {
        return when {
            pRatio < 0.10f -> "Alkali Feldspar Syenite"
            pRatio < 0.35f -> "Syenite"
            pRatio < 0.65f -> "Monzonite"
            pRatio < 0.90f -> "Monzodiorite / Monzogabbro"
            else -> "Diorite / Gabbro / Anorthosite"
        }
    }
}

fun classifyVolcanicQAP(q: Float, a: Float, p: Float): String {
    if (q >= 90f) return "Quartzolite (Volcanic tuff equivalent / Silexite)"
    if (q >= 60f) return "Quartz-rich Glassy/Aphanitic Rock (Acid Tuff)"
    
    val totalFeldspar = a + p
    if (totalFeldspar <= 0f) return "Volcanic Rock (Indeterminate)"
    val pRatio = p / totalFeldspar
    
    if (q >= 20f) {
        return when {
            pRatio < 0.10f -> "Alkali Feldspar Rhyolite"
            pRatio < 0.35f -> "Rhyolite (Granite equivalent)"
            pRatio < 0.65f -> "Rhyodacite / Dacite"
            pRatio < 0.90f -> "Dacite"
            else -> "Plagidacite / Quartz Andesite"
        }
    } else if (q >= 5f) {
        return when {
            pRatio < 0.10f -> "Quartz Alkali Feldspar Trachyte"
            pRatio < 0.35f -> "Quartz Trachyte"
            pRatio < 0.65f -> "Quartz Latite"
            pRatio < 0.90f -> "Quartz Latite-Andesite"
            else -> "Quartz Andesite / Quartz Basalt"
        }
    } else {
        return when {
            pRatio < 0.10f -> "Alkali Feldspar Trachyte"
            pRatio < 0.35f -> "Trachyte"
            pRatio < 0.65f -> "Latite"
            pRatio < 0.90f -> "Latite-Andesite / Mugearite"
            else -> "Andesite / Basalt (Aphanitic)"
        }
    }
}

fun classifySedimentaryMixedQCC(q: Float, c: Float, clay: Float): String {
    val total = q + c + clay
    if (total <= 0f) return "Mixed Clay-Lime-Silica Rock (Indeterminate)"
    val qNorm = (q / total) * 100f
    val cNorm = (c / total) * 100f
    val clayNorm = (clay / total) * 100f

    return when {
        // High Sandy / Siliceous
        qNorm >= 90f -> "Pure Quartz Arenite (Siliceous Sandstone)"
        qNorm >= 50f -> {
            when {
                cNorm >= 30f -> "Calcareous Sandstone / Sandy Limestone transition"
                clayNorm >= 30f -> "Argillaceous Sandstone"
                else -> "Quartzose Sandstone (Siliceous Mixed)"
            }
        }
        // High Carbonate
        cNorm >= 90f -> "Pure Limestone / Chalk (Carbonate)"
        cNorm >= 50f -> {
            when {
                qNorm >= 30f -> "Sandy Limestone (Calcite-Rich)"
                clayNorm >= 30f -> "Marly Limestone / Margel (Calc-Clay)"
                else -> "Dolostone / Limestone"
            }
        }
        // High Shale / Clay
        clayNorm >= 90f -> "Pure Claystone / Mudshale"
        clayNorm >= 50f -> {
            when {
                qNorm >= 30f -> "Silty Shale / Sandy Mudstone"
                cNorm >= 30f -> "Calcareous Shale / Marl"
                else -> "Pelitic Mudstone / Shale"
            }
        }
        // Evenly Mixed
        else -> "Mixed Calcareous Argillaceous Sandstone / Greywacke"
    }
}

fun classifyCarbonateDunham(
    grainPercent: Float,
    mudPercent: Float,
    boundOrganic: Boolean,
    crystallineTexture: Boolean
): String {
    if (crystallineTexture) return "Crystalline Carbonate (Texture Not Recognizable)"
    if (boundOrganic) return "Boundstone (Organic components bound together during deposition)"
    val total = grainPercent + mudPercent
    if (total <= 0f) return "Limestone / Carbonate (Indeterminate)"
    val grainRatio = grainPercent / total
    
    return if (mudPercent > 0.1f) {
        if (grainRatio < 0.10f) {
            "Mudstone (< 10% grains, Mud-supported Carbonate)"
        } else if (grainRatio < 0.50f) {
            "Wackestone (> 10% grains, Mud-supported Lime)"
        } else {
            "Packstone (Grain-supported Carbonate with muddy matrix)"
        }
    } else {
        "Grainstone (Grain-supported Carbonate, containing no Lime Mud)"
    }
}

fun classifyMetamorphic(tempC: Float, depthKm: Float, protolith: String): String {
    val pressureKb = depthKm * 0.27f
    val facies = when {
        tempC < 150f -> "Low-Temperature Diagenetic Zone"
        tempC < 300f -> "Zeolite / Prehnite-Pumpellyite Facies"
        pressureKb < 2.5f -> "Sanidinite / Hornfels Facies (Contact Metamorphism)"
        pressureKb >= 10f && tempC >= 600f -> "Eclogite Facies (High-Pressure Mantle Zone)"
        pressureKb >= 8f && tempC in 300f..500f -> "Blueschist Facies (Subduction Metamorphism)"
        tempC in 300f..450f -> "Greenschist Facies (Low-Grade Regional)"
        tempC in 450f..650f -> "Amphibolite Facies (Medium-to-High Regional)"
        tempC >= 650f -> "Granulite Facies (High-Grade Crustal)"
        else -> "Greenschist-Amphibolite Transition Facies"
    }

    val rockName = when (protolith.lowercase().trim()) {
        "shale", "mudstone", "claystone" -> {
            when {
                tempC < 250f -> "Slate"
                tempC < 400f -> "Phyllite"
                tempC < 600f -> "Mica Schist (Quartz-Mica-Garnet)"
                else -> "Paragneiss / Migmatite"
            }
        }
        "basalt", "gabbro", "volcanic" -> {
            when {
                facies.contains("Blueschist") -> "Blueschist"
                facies.contains("Greenschist") -> "Greenschist / Greenstone"
                facies.contains("Amphibolite") -> "Amphibolite (Hornblende rock)"
                facies.contains("Eclogite") -> "Eclogite (Omphacite-Garnet)"
                else -> "Metabasite / Greenstone"
            }
        }
        "limestone", "dolostone", "carbonate" -> {
            if (tempC >= 200f) "Marble" else "Metacalcite / Carbonate Slate"
        }
        "sandstone", "quartzose", "silica" -> {
            if (tempC >= 200f) "Quartzite" else "Meta-sandstone"
        }
        "granite", "rhyolite" -> {
            if (tempC >= 500f) "Orthogneiss (Granitic Gneiss)" else "Meta-granitoid"
        }
        else -> {
            when {
                tempC < 300f -> "Meta-rock"
                tempC < 550f -> "Schistose Crystal Rock"
                else -> "Foliated Gneiss"
            }
        }
    }

    return "$rockName ($facies)"
}

@Composable
fun GeologicalCompassDial(
    strike: String,
    dip: String,
    dipDirection: String,
    onStrikeChange: (String) -> Unit,
    onDipChange: (String) -> Unit,
    onDipDirectionChange: (String) -> Unit,
    autoCalcDipDir: Boolean,
    modifier: Modifier = Modifier
) {
    val strikeVal = strike.toIntOrNull() ?: 0
    val dipVal = dip.toIntOrNull() ?: 0
    val dipDirVal = dipDirection.toIntOrNull() ?: 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Interactive Clinometer Compass Tool",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .pointerInput(autoCalcDipDir) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            angle = (angle + 90f + 360f) % 360f
                            val newStrike = angle.toInt()
                            onStrikeChange(newStrike.toString())
                            if (autoCalcDipDir) {
                                val newDipDir = (newStrike + 90) % 360
                                onDipDirectionChange(String.format(Locale.ROOT, "%03d", newDipDir))
                            }
                        }
                    }
                    .pointerInput(autoCalcDipDir) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = change.position.x - cx
                            val dy = change.position.y - cy
                            var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            angle = (angle + 90f + 360f) % 360f
                            val newStrike = angle.toInt()
                            onStrikeChange(newStrike.toString())
                            if (autoCalcDipDir) {
                                val newDipDir = (newStrike + 90) % 360
                                onDipDirectionChange(String.format(Locale.ROOT, "%03d", newDipDir))
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val r = size.width / 2

                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        radius = r - 10,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
                    )

                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        drawText("N", center.x, 24f, paint)
                        drawText("S", center.x, size.height - 8f, paint)
                        drawText("E", size.width - 20f, center.y + 8f, paint)
                        drawText("W", 20f, center.y + 8f, paint)
                    }

                    val strikeRad = Math.toRadians((strikeVal - 90).toDouble())
                    val strikeX1 = center.x + (r - 12) * Math.cos(strikeRad).toFloat()
                    val strikeY1 = center.y + (r - 12) * Math.sin(strikeRad).toFloat()
                    val strikeX2 = center.x - (r - 12) * Math.cos(strikeRad).toFloat()
                    val strikeY2 = center.y - (r - 12) * Math.sin(strikeRad).toFloat()

                    drawLine(
                        color = Color(0xFFE53935),
                        start = Offset(strikeX1, strikeY1),
                        end = Offset(strikeX2, strikeY2),
                        strokeWidth = 4f
                    )

                    val dipDirRad = Math.toRadians((dipDirVal - 90).toDouble())
                    val dipLength = (r - 20) * (dipVal / 90f)
                    val dipX = center.x + dipLength * Math.cos(dipDirRad).toFloat()
                    val dipY = center.y + dipLength * Math.sin(dipDirRad).toFloat()

                    drawLine(
                        color = Color(0xFF1E88E5),
                        start = center,
                        end = Offset(dipX, dipY),
                        strokeWidth = 6f
                    )
                    drawCircle(
                        color = Color(0xFF1E88E5),
                        radius = 6f,
                        center = Offset(dipX, dipY)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Strike: ${strikeVal}°",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = getCardinalDirection(strikeVal.toString()),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Slider(
                    value = strikeVal.toFloat(),
                    onValueChange = {
                        val newStrike = it.toInt()
                        onStrikeChange(newStrike.toString())
                        if (autoCalcDipDir) {
                            val newDipDir = (newStrike + 90) % 360
                            onDipDirectionChange(String.format(Locale.ROOT, "%03d", newDipDir))
                        }
                    },
                    valueRange = 0f..360f,
                    modifier = Modifier.height(28.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dip: ${dipVal}°",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (dipVal < 20) "Flat" else if (dipVal < 60) "Moderate" else "Steep",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Slider(
                    value = dipVal.toFloat(),
                    onValueChange = {
                        onDipChange(it.toInt().toString())
                    },
                    valueRange = 0f..90f,
                    modifier = Modifier.height(28.dp)
                )
            }
        }
        Text(
            text = "Tap or drag compass visually to configure measurements on-the-field.",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
    }
}

@Composable
fun RecentObservationsTimeline(
    allObservations: List<Observation>,
    onNavigateToTab: (MainTab) -> Unit
) {
    val recentObs = allObservations.sortedByDescending { it.createdAt }.take(3)

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Recent Field Journal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                TextButton(
                    onClick = { onNavigateToTab(MainTab.GeologicalMapping) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("View Journal", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (recentObs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "No recent field notes. Click 'Seeding' in settings to load rich test dataset!",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    recentObs.forEach { obs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            when (obs.observationType) {
                                                "Lithology" -> Color(0xFF1E88E5)
                                                "Alteration" -> Color(0xFFE65100)
                                                "Fault", "Joint", "Vein", "Structure" -> Color(0xFF43A047)
                                                "Mineralization" -> Color(0xFFFFB020)
                                                else -> MaterialTheme.colorScheme.primary
                                            },
                                            CircleShape
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .height(30.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val typeLabel = when (obs.observationType) {
                                        "Lithology" -> "Lithology: ${obs.lithologyType ?: "Rock Unit"}"
                                        "Alteration" -> "Alteration: ${obs.alterationType ?: "Index"}"
                                        "Fault" -> "Fault: ${obs.faultType ?: "Fracture"}"
                                        "Mineralization" -> "Mineralization"
                                        else -> obs.observationType
                                    }
                                    Text(
                                        text = typeLabel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val timeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date(obs.createdAt))
                                    Text(
                                        text = timeStr,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = obs.notes ?: "No descriptive remarks logged.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeotechnicalUnitConverter() {
    var expanded by remember { mutableStateOf(false) }
    var inputVal by remember { mutableStateOf("100") }
    // 0: MPa to psi, 1: psi to MPa, 2: g/cm3 to kg/m3, 3: kg/m3 to g/cm3, 4: meters to ft, 5: ft to meters
    var conversionType by remember { mutableStateOf(0) }

    val types = listOf(
        "UCS Strength: MPa ➔ psi",
        "UCS Strength: psi ➔ MPa",
        "Rock Density: g/cm³ ➔ kg/m³",
        "Rock Density: kg/m³ ➔ g/cm³",
        "Core Length: meters ➔ feet",
        "Core Length: feet ➔ meters"
    )

    val d = inputVal.toDoubleOrNull() ?: 0.0
    val outputVal = when (conversionType) {
        0 -> d * 145.0377
        1 -> d / 145.0377
        2 -> d * 1000.0
        3 -> d / 1000.0
        4 -> d * 3.28084
        5 -> d / 3.28084
        else -> 0.0
    }

    val outputUnit = when (conversionType) {
        0 -> "psi"
        1 -> "MPa"
        2 -> "kg/m³"
        3 -> "g/cm³"
        4 -> "ft"
        5 -> "m"
        else -> ""
    }

    val inputUnit = when (conversionType) {
        0 -> "MPa"
        1 -> "psi"
        2 -> "g/cm³"
        3 -> "kg/m³"
        4 -> "m"
        5 -> "ft"
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Geotechnical Unit Converter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Quick calculator and standard rock mechanics unit converter for field actions:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var menuExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(types[conversionType], fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            types.forEachIndexed { index, label ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 11.sp) },
                                    onClick = {
                                        conversionType = index
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputVal,
                            onValueChange = { inputVal = it },
                            label = { Text("Input Value ($inputUnit)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.3f", outputVal)} $outputUnit",
                                    fontSize = 13.sp,
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

@Composable
fun QAPFClassifierPanel() {
    var isExpanded by remember { mutableStateOf(false) }
    var activeClassifierTab by remember { mutableStateOf(0) } // 0 = Plutonic (QAP), 1 = Volcanic (QAP), 2 = Sandstone (QFL), 3 = Mixed (QCC), 4 = Limestone (Dunham), 5 = Metamorphic

    // QAP Inputs
    var qInput by remember { mutableStateOf(35f) }
    var aInput by remember { mutableStateOf(40f) }
    var pInput by remember { mutableStateOf(25f) }

    // Volcanic QAP Inputs
    var volqInput by remember { mutableStateOf(20f) }
    var volaInput by remember { mutableStateOf(30f) }
    var volpInput by remember { mutableStateOf(50f) }

    // QFL Inputs
    var qflQInput by remember { mutableStateOf(80f) }
    var qflFInput by remember { mutableStateOf(15f) }
    var qflLInput by remember { mutableStateOf(5f) }

    // QCC Inputs
    var qccQInput by remember { mutableStateOf(40f) }
    var qccCInput by remember { mutableStateOf(30f) }
    var qccClayInput by remember { mutableStateOf(30f) }

    // Carbonate Dunham Inputs
    var carbonateGrainInput by remember { mutableStateOf(40f) }
    var carbonateMudInput by remember { mutableStateOf(60f) }
    var carbonateBoundOrganic by remember { mutableStateOf(false) }
    var carbonateCrystalline by remember { mutableStateOf(false) }

    // Metamorphic Inputs
    var metamorphicTempInput by remember { mutableStateOf(400f) }
    var metamorphicDepthInput by remember { mutableStateOf(12f) }
    var metamorphicProtolithSelected by remember { mutableStateOf("Shale") }

    // Normalize QAP
    val sum = qInput + aInput + pInput
    val qNorm = if (sum > 0) (qInput / sum) * 100f else 33.3f
    val aNorm = if (sum > 0) (aInput / sum) * 100f else 33.3f
    val pNorm = if (sum > 0) (pInput / sum) * 100f else 33.3f

    // Normalize Volcanic QAP
    val volSum = volqInput + volaInput + volpInput
    val volqNorm = if (volSum > 0) (volqInput / volSum) * 100f else 33.3f
    val volaNorm = if (volSum > 0) (volaInput / volSum) * 100f else 33.3f
    val volpNorm = if (volSum > 0) (volpInput / volSum) * 100f else 33.3f

    // Normalize QFL
    val qflSum = qflQInput + qflFInput + qflLInput
    val qflQNorm = if (qflSum > 0) (qflQInput / qflSum) * 100f else 33.3f
    val qflFNorm = if (qflSum > 0) (qflFInput / qflSum) * 100f else 33.3f
    val qflLNorm = if (qflSum > 0) (qflLInput / qflSum) * 100f else 33.3f

    // Normalize QCC
    val qccSum = qccQInput + qccCInput + qccClayInput
    val qccQNorm = if (qccSum > 0) (qccQInput / qccSum) * 100f else 33.3f
    val qccCNorm = if (qccSum > 0) (qccCInput / qccSum) * 100f else 33.3f
    val qccClayNorm = if (qccSum > 0) (qccClayInput / qccSum) * 100f else 33.3f

    // Normalize Carbonates
    val carbonateSum = carbonateGrainInput + carbonateMudInput
    val carbGrainNorm = if (carbonateSum > 0) (carbonateGrainInput / carbonateSum) * 100f else 50f
    val carbMudNorm = if (carbonateSum > 0) (carbonateMudInput / carbonateSum) * 100f else 50f

    val classifiedName = remember(qNorm, aNorm, pNorm) {
        classifyPlutonicQAP(qNorm, aNorm, pNorm)
    }

    val classifiedVolcanicName = remember(volqNorm, volaNorm, volpNorm) {
        classifyVolcanicQAP(volqNorm, volaNorm, volpNorm)
    }

    val classifiedSandstoneName = remember(qflQNorm, qflFNorm, qflLNorm) {
        classifySandstoneQFL(qflQNorm, qflFNorm, qflLNorm)
    }

    val classifiedQccName = remember(qccQNorm, qccCNorm, qccClayNorm) {
        classifySedimentaryMixedQCC(qccQNorm, qccCNorm, qccClayNorm)
    }

    val classifiedDunhamName = remember(carbGrainNorm, carbMudNorm, carbonateBoundOrganic, carbonateCrystalline) {
        classifyCarbonateDunham(carbGrainNorm, carbMudNorm, carbonateBoundOrganic, carbonateCrystalline)
    }

    val classifiedMetamorphicName = remember(metamorphicTempInput, metamorphicDepthInput, metamorphicProtolithSelected) {
        classifyMetamorphic(metamorphicTempInput, metamorphicDepthInput, metamorphicProtolithSelected)
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("qapf_classifier_card"),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.animateContentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "QAPF Chart",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "IUGS Multi-Ternary Rock Classifier",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        when (activeClassifierTab) {
                            0 -> "PLUTONIC (QAP)"
                            1 -> "VOLCANIC (QAP)"
                            2 -> "SEDIMENTARY (QFL)"
                            3 -> "SEDIMENTARY (QCC)"
                            4 -> "LIMESTONE (DUNHAM)"
                            else -> "METAMORPHIC"
                        },
                        fontSize = 8.5.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (isExpanded) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Selector tabs for Igneous vs Sedimentary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Plutonic (QAP)", "Volcanic (QAP)", "Sandstone (QFL)", "Mixed (QCC)", "Limestone (Dunham)", "Metamorphic").forEachIndexed { index, title ->
                        Button(
                            onClick = { activeClassifierTab = index },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeClassifierTab == index) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (activeClassifierTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(title, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeClassifierTab == 0) {
                        // Plutonic Igneous QAP Tab
                        Text(
                            text = "Interactive plot for plutonic rocks based on Normalized Quartz (Q), Alkali Feldspar (A), and Plagioclase (P) percentages (IUGS standard). Drag sliders or tap directly on the vector triangle.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Triangle Plotter Canvas
                            Box(
                                modifier = Modifier
                                    .size(145.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.2f), RoundedCornerShape(8.dp))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.4f), RoundedCornerShape(8.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            val xQ = w / 2f
                                            val yQ = 12f
                                            val xA = 12f
                                            val yA = h - 12f
                                            val xP = w - 12f
                                            val yP = h - 12f

                                            val det = (xA - xP) * (yQ - yP) - (xQ - xP) * (yA - yP)
                                            if (det != 0f) {
                                                val u = ((yQ - yP) * (offset.x - xP) + (xP - xQ) * (offset.y - yP)) / det
                                                val v = ((yP - yA) * (offset.x - xP) + (xA - xP) * (offset.y - yP)) / det
                                                val wr = 1f - u - v
                                                if (u >= -0.05f && v >= -0.05f && wr >= -0.05f) {
                                                    val uClamp = u.coerceIn(0f, 1f)
                                                    val vClamp = v.coerceIn(0f, 1f)
                                                    val wrClamp = (1f - uClamp - vClamp).coerceIn(0f, 1f)
                                                    qInput = vClamp * 100f
                                                    aInput = uClamp * 100f
                                                    pInput = wrClamp * 100f
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val xQ = w / 2f
                                    val yQ = 12f
                                    val xA = 12f
                                    val yA = h - 12f
                                    val xP = w - 12f
                                    val yP = h - 12f

                                    // Geological Fields Division lines (Better visual appearance!)
                                    val granitePath = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(xQ, yQ)
                                        lineTo(xA + 0.1f * (xQ - xA), yQ + 0.9f * (yA - yQ))
                                        lineTo(xP - 0.1f * (xP - xQ), yQ + 0.9f * (yP - yQ))
                                        close()
                                    }
                                    drawPath(path = granitePath, color = Color(0xFFFFCC80).copy(0.12f))

                                    // Field division lines
                                    val y90 = yQ + 0.9f * (yA - yQ)
                                    val xL90 = xA + 0.1f * (xQ - xA)
                                    val xR90 = xP - 0.1f * (xP - xQ)
                                    drawLine(color = Color.LightGray.copy(0.4f), start = Offset(xL90, y90), end = Offset(xR90, y90), strokeWidth = 1f)

                                    val y60 = yQ + 0.6f * (yA - yQ)
                                    val xL60 = xA + 0.4f * (xQ - xA)
                                    val xR60 = xP - 0.4f * (xP - xQ)
                                    drawLine(color = Color.LightGray.copy(0.4f), start = Offset(xL60, y60), end = Offset(xR60, y60), strokeWidth = 1.2f)

                                    val y20 = yQ + 0.2f * (yA - yQ)
                                    val xL20 = xA + 0.8f * (xQ - xA)
                                    val xR20 = xP - 0.8f * (xP - xQ)
                                    drawLine(color = Color(0xFF64B5F6).copy(0.6f), start = Offset(xL20, y20), end = Offset(xR20, y20), strokeWidth = 1.5f)

                                    val y5 = yQ + 0.05f * (yA - yQ)
                                    val xL5 = xA + 0.95f * (xQ - xA)
                                    val xR5 = xP - 0.95f * (xP - xQ)
                                    drawLine(color = Color.LightGray.copy(0.4f), start = Offset(xL5, y5), end = Offset(xR5, y5), strokeWidth = 1f)

                                    val ticks = listOf(0.1f, 0.35f, 0.65f, 0.9f)
                                    ticks.forEach { t ->
                                        val xAP = xA + t * (xP - xA)
                                        val yAP = yA
                                        drawLine(color = Color.Gray.copy(0.2f), start = Offset(xQ, yQ), end = Offset(xAP, yAP), strokeWidth = 0.8f)
                                    }

                                    drawLine(color = Color.Gray, start = Offset(xQ, yQ), end = Offset(xA, yA), strokeWidth = 1.5f)
                                    drawLine(color = Color.Gray, start = Offset(xA, yA), end = Offset(xP, yP), strokeWidth = 1.5f)
                                    drawLine(color = Color.Gray, start = Offset(xP, yP), end = Offset(xQ, yQ), strokeWidth = 1.5f)

                                    drawContext.canvas.nativeCanvas.apply {
                                        val paint = android.graphics.Paint().apply {
                                            color = android.graphics.Color.GRAY
                                            textSize = 18f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        }
                                        drawText("Q", xQ, yQ - 2f, paint)
                                        drawText("A", xA - 5f, yA + 6f, paint)
                                        drawText("P", xP + 7f, yP + 6f, paint)
                                    }

                                    val px = (aNorm * xA + pNorm * xP + qNorm * xQ) / 100f
                                    val py = (aNorm * yA + pNorm * yP + qNorm * yQ) / 100f

                                    drawCircle(color = Color(0xFFE53935).copy(0.3f), radius = 11f, center = Offset(px, py))
                                    drawCircle(color = Color(0xFFE53935), radius = 4.5f, center = Offset(px, py))
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "Quartz (Q): ${qNorm.toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = qInput,
                                    onValueChange = { qInput = it },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(20.dp)
                                )

                                Text(text = "Alkali Feld. (A): ${aNorm.toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = aInput,
                                    onValueChange = { aInput = it },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(20.dp)
                                )

                                Text(text = "Plagioclase (P): ${pNorm.toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = pInput,
                                    onValueChange = { pInput = it },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(20.dp)
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "IUGS Igneous Classification Result:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = classifiedName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Normalized ratio: Q: ${qNorm.toInt()}%  A: ${aNorm.toInt()}%  P: ${pNorm.toInt()}%",
                                    fontSize = 8.5.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.75f)
                                )
                            }
                        }
                    } else if (activeClassifierTab == 1) {
                        // Volcanic Igneous QAP Tab
                        Text(
                            text = "Interactive plot for volcanic rocks based on Normalized Quartz (Q), Alkali Feldspar (A), and Plagioclase (P) percentages. Drag sliders or tap directly on the vector triangle.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Triangle Plotter Canvas
                            Box(
                                modifier = Modifier
                                    .size(145.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.2f), RoundedCornerShape(8.dp))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.4f), RoundedCornerShape(8.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            val xQ = w / 2f
                                            val yQ = 12f
                                            val xA = 12f
                                            val yA = h - 12f
                                            val xP = w - 12f
                                            val yP = h - 12f

                                            val det = (xA - xP) * (yQ - yP) - (xQ - xP) * (yA - yP)
                                            if (det != 0f) {
                                                val u = ((yQ - yP) * (offset.x - xP) + (xP - xQ) * (offset.y - yP)) / det
                                                val v = ((yP - yA) * (offset.x - xP) + (xA - xP) * (offset.y - yP)) / det
                                                val wr = 1f - u - v
                                                if (u >= -0.05f && v >= -0.05f && wr >= -0.05f) {
                                                    val uClamp = u.coerceIn(0f, 1f)
                                                    val vClamp = v.coerceIn(0f, 1f)
                                                    val wrClamp = (1f - uClamp - vClamp).coerceIn(0f, 1f)
                                                    volqInput = vClamp * 100f
                                                    volaInput = uClamp * 100f
                                                    volpInput = wrClamp * 100f
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val xQ = w / 2f
                                    val yQ = 12f
                                    val xA = 12f
                                    val yA = h - 12f
                                    val xP = w - 12f
                                    val yP = h - 12f

                                    // Geological Fields Division lines
                                    val volcanicPath = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(xQ, yQ)
                                        lineTo(xA + 0.2f * (xQ - xA), yQ + 0.8f * (yA - yQ))
                                        lineTo(xP - 0.2f * (xP - xQ), yQ + 0.8f * (yP - yQ))
                                        close()
                                    }
                                    drawPath(path = volcanicPath, color = Color(0xFFCE93D8).copy(0.12f))

                                    // Field division lines
                                    val y90 = yQ + 0.9f * (yA - yQ)
                                    val xL90 = xA + 0.1f * (xQ - xA)
                                    val xR90 = xP - 0.1f * (xP - xQ)
                                    drawLine(color = Color.LightGray.copy(0.4f), start = Offset(xL90, y90), end = Offset(xR90, y90), strokeWidth = 1f)

                                    val y60 = yQ + 0.6f * (yA - yQ)
                                    val xL60 = xA + 0.4f * (xQ - xA)
                                    val xR60 = xP - 0.4f * (xP - xQ)
                                    drawLine(color = Color.LightGray.copy(0.4f), start = Offset(xL60, y60), end = Offset(xR60, y60), strokeWidth = 1.2f)

                                    val y20 = yQ + 0.2f * (yA - yQ)
                                    val xL20 = xA + 0.8f * (xQ - xA)
                                    val xR20 = xP - 0.8f * (xP - xQ)
                                    drawLine(color = Color(0xFF64B5F6).copy(0.6f), start = Offset(xL20, y20), end = Offset(xR20, y20), strokeWidth = 1.5f)

                                    drawLine(color = Color.Gray, start = Offset(xQ, yQ), end = Offset(xA, yA), strokeWidth = 1.5f)
                                    drawLine(color = Color.Gray, start = Offset(xA, yA), end = Offset(xP, yP), strokeWidth = 1.5f)
                                    drawLine(color = Color.Gray, start = Offset(xP, yP), end = Offset(xQ, yQ), strokeWidth = 1.5f)

                                    drawContext.canvas.nativeCanvas.apply {
                                        val paint = android.graphics.Paint().apply {
                                            color = android.graphics.Color.GRAY
                                            textSize = 18f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        }
                                        drawText("Q", xQ, yQ - 2f, paint)
                                        drawText("A", xA - 5f, yA + 6f, paint)
                                        drawText("P", xP + 7f, yP + 6f, paint)
                                    }

                                    val px = (volaNorm * xA + volpNorm * xP + volqNorm * xQ) / 100f
                                    val py = (volaNorm * yA + volpNorm * yP + volqNorm * yQ) / 100f

                                    drawCircle(color = Color(0xFF9C27B0).copy(0.3f), radius = 11f, center = Offset(px, py))
                                    drawCircle(color = Color(0xFF9C27B0), radius = 4.5f, center = Offset(px, py))
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "Quartz (Q): ${volqNorm.toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = volqInput,
                                    onValueChange = { volqInput = it },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(20.dp)
                                )

                                Text(text = "Alkali Feld. (A): ${volaNorm.toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = volaInput,
                                    onValueChange = { volaInput = it },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(20.dp)
                                )

                                Text(text = "Plagioclase (P): ${volpNorm.toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = volpInput,
                                    onValueChange = { volpInput = it },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(20.dp)
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "IUGS Volcanic Classification Result:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = classifiedVolcanicName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Normalized ratio: Q: ${volqNorm.toInt()}%  A: ${volaNorm.toInt()}%  P: ${volpNorm.toInt()}%",
                                    fontSize = 8.5.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(0.75f)
                                )
                            }
                        }
                    } else if (activeClassifierTab == 2) {
                        // Sandstone Sedimentary QFL Tab
                        Text(
                            text = "Folk & Pettijohn standard Sandstone classification based on normalized Quartz grains (Q), Feldspar (F), and Lithic rock fragments (L) ratios. Tap directly on the triangle or adjust raw inputs below.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sedimentary Triangle Canvas
                            Box(
                                modifier = Modifier
                                    .size(145.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.2f), RoundedCornerShape(8.dp))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.4f), RoundedCornerShape(8.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            val xQ = w / 2f
                                            val yQ = 12f
                                            val xF = 12f
                                            val yF = h - 12f
                                            val xL = w - 12f
                                            val yL = h - 12f

                                            val det = (xF - xL) * (yQ - yL) - (xQ - xL) * (yF - yL)
                                            if (det != 0f) {
                                                val u = ((yQ - yL) * (offset.x - xL) + (xL - xQ) * (offset.y - yL)) / det
                                                val v = ((yL - yF) * (offset.x - xL) + (xF - xL) * (offset.y - yL)) / det
                                                val wr = 1f - u - v
                                                if (u >= -0.05f && v >= -0.05f && wr >= -0.05f) {
                                                    val uClamp = u.coerceIn(0f, 1f)
                                                    val vClamp = v.coerceIn(0f, 1f)
                                                    val wrClamp = (1f - uClamp - vClamp).coerceIn(0f, 1f)
                                                    qflQInput = vClamp * 100f
                                                    qflFInput = uClamp * 100f
                                                    qflLInput = wrClamp * 100f
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val xQ = w / 2f
                                    val yQ = 12f
                                    val xF = 12f
                                    val yF = h - 12f
                                    val xL = w - 12f
                                    val yL = h - 12f

                                    // Outer triangle boundary
                                    drawLine(color = Color.Gray, start = Offset(xQ, yQ), end = Offset(xF, yF), strokeWidth = 1.5f)
                                    drawLine(color = Color.Gray, start = Offset(xF, yF), end = Offset(xL, yL), strokeWidth = 1.5f)
                                    drawLine(color = Color.Gray, start = Offset(xL, yL), end = Offset(xQ, yQ), strokeWidth = 1.5f)

                                    // Draw Q = 95% line
                                    val y95 = yQ + 0.95f * (yF - yQ)
                                    val xLF95 = xF + 0.05f * (xQ - xF)
                                    val xLL95 = xL - 0.05f * (xL - xQ)
                                    drawLine(color = Color(0xFF4CAF50).copy(0.6f), start = Offset(xLF95, y95), end = Offset(xLL95, y95), strokeWidth = 1.2f)

                                    // Draw Q = 75% line
                                    val y75 = yQ + 0.75f * (yF - yQ)
                                    val xLF75 = xF + 0.25f * (xQ - xF)
                                    val xLL75 = xL - 0.25f * (xL - xQ)
                                    drawLine(color = Color(0xFFFF9800).copy(0.6f), start = Offset(xLF75, y75), end = Offset(xLL75, y75), strokeWidth = 1.2f)

                                    // Draw vertical sector dividing line (Feldspar vs Lithic ratio division) at 50%
                                    val xMidBase = xF + 0.5f * (xL - xF)
                                    drawLine(color = Color.Gray.copy(0.4f), start = Offset(xQ, yQ), end = Offset(xMidBase, yF), strokeWidth = 1f)

                                    drawContext.canvas.nativeCanvas.apply {
                                        val paint = android.graphics.Paint().apply {
                                            color = android.graphics.Color.GRAY
                                            textSize = 18f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        }
                                        drawText("Q", xQ, yQ - 2f, paint)
                                        drawText("F", xF - 5f, yF + 6f, paint)
                                        drawText("L", xL + 7f, yL + 6f, paint)
                                    }

                                    // Plot dynamic normalized point marker
                                    val px = (qflFNorm * xF + qflLNorm * xL + qflQNorm * xQ) / 100f
                                    val py = (qflFNorm * yF + qflLNorm * yL + qflQNorm * yQ) / 100f

                                    drawCircle(color = Color(0xFF43A047).copy(0.3f), radius = 11f, center = Offset(px, py))
                                    drawCircle(color = Color(0xFF43A047), radius = 4.5f, center = Offset(px, py))
                                }
                            }

                            // Sliders for QFL values
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "Quartz (Q): ${qflQNorm.toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = qflQInput,
                                    onValueChange = { qflQInput = it },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(20.dp)
                                )

                                Text(text = "Feldspar (F): ${qflFNorm.toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = qflFInput,
                                    onValueChange = { qflFInput = it },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(20.dp)
                                )

                                Text(text = "Lithic Frags (L): ${qflLNorm.toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = qflLInput,
                                    onValueChange = { qflLInput = it },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(20.dp)
                                )
                            }
                        }

                        // Sandstone classification result
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "IUGS Sandstone Classification Result:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = classifiedSandstoneName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Normalized ratio: Q: ${qflQNorm.toInt()}%  F: ${qflFNorm.toInt()}%  L: ${qflLNorm.toInt()}%",
                                    fontSize = 8.5.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.75f)
                                )
                            }
                        }
                    } else if (activeClassifierTab == 3) {
                        // Mixed Clay-Lime-Silica QCC Tab
                        Text(
                            text = "Ternary classification for mixed sedimentary rocks based on normalized Quartz grains (Q), Carbonate / Lime (C), and Clay / Mud (Clay) content. Tap directly on the triangle or adjust inputs below.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sedimentary QCC Triangle Canvas
                            Box(
                                modifier = Modifier
                                    .size(145.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.2f), RoundedCornerShape(8.dp))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.4f), RoundedCornerShape(8.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            val xQ = w / 2f
                                            val yQ = 12f
                                            val xC = 12f
                                            val yC = h - 12f
                                            val xClay = w - 12f
                                            val yClay = h - 12f

                                            val det = (xC - xClay) * (yQ - yClay) - (xQ - xClay) * (yC - yClay)
                                            if (det != 0f) {
                                                val u = ((yQ - yClay) * (offset.x - xClay) + (xClay - xQ) * (offset.y - yClay)) / det
                                                val v = ((yClay - yC) * (offset.x - xClay) + (xC - xClay) * (offset.y - yClay)) / det
                                                val wr = 1f - u - v
                                                if (u >= -0.05f && v >= -0.05f && wr >= -0.05f) {
                                                    val uClamp = u.coerceIn(0f, 1f)
                                                    val vClamp = v.coerceIn(0f, 1f)
                                                    val wrClamp = (1f - uClamp - vClamp).coerceIn(0f, 1f)
                                                    qccQInput = vClamp * 100f
                                                    qccCInput = uClamp * 100f
                                                    qccClayInput = wrClamp * 100f
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val xQ = w / 2f
                                    val yQ = 12f
                                    val xC = 12f
                                    val yC = h - 12f
                                    val xClay = w - 12f
                                    val yClay = h - 12f

                                    // Outer triangle boundary
                                    drawLine(color = Color.Gray, start = Offset(xQ, yQ), end = Offset(xC, yC), strokeWidth = 1.5f)
                                    drawLine(color = Color.Gray, start = Offset(xC, yC), end = Offset(xClay, yClay), strokeWidth = 1.5f)
                                    drawLine(color = Color.Gray, start = Offset(xClay, yClay), end = Offset(xQ, yQ), strokeWidth = 1.5f)

                                    // Draw Q = 50% line
                                    val y50 = yQ + 0.5f * (yC - yQ)
                                    val xLC50 = xC + 0.5f * (xQ - xC)
                                    val xLClay50 = xClay - 0.5f * (xClay - xQ)
                                    drawLine(color = Color(0xFF673AB7).copy(0.6f), start = Offset(xLC50, y50), end = Offset(xLClay50, y50), strokeWidth = 1.2f)

                                    drawContext.canvas.nativeCanvas.apply {
                                        val paint = android.graphics.Paint().apply {
                                            color = android.graphics.Color.GRAY
                                            textSize = 18f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        }
                                        drawText("Q", xQ, yQ - 2f, paint)
                                        drawText("C", xC - 5f, yC + 6f, paint)
                                        drawText("Clay", xClay + 7f, yClay + 6f, paint)
                                    }

                                    // Plot dynamic normalized point marker
                                    val px = (qccCNorm * xC + qccClayNorm * xClay + qccQNorm * xQ) / 100f
                                    val py = (qccCNorm * yC + qccClayNorm * yClay + qccQNorm * yQ) / 100f

                                    drawCircle(color = Color(0xFFFF5722).copy(0.3f), radius = 11f, center = Offset(px, py))
                                    drawCircle(color = Color(0xFFFF5722), radius = 4.5f, center = Offset(px, py))
                                }
                            }

                            // Sliders for QCC values
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "Quartz (Q): ${qccQNorm.toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = qccQInput,
                                    onValueChange = { qccQInput = it },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(20.dp)
                                )

                                Text(text = "Carbonate (C): ${qccCNorm.toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = qccCInput,
                                    onValueChange = { qccCInput = it },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(20.dp)
                                )

                                Text(text = "Clay / Mud: ${qccClayNorm.toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = qccClayInput,
                                    onValueChange = { qccClayInput = it },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(20.dp)
                                )
                            }
                        }

                        // QCC classification result
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Sedimentary QCC Classification Result:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = classifiedQccName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Normalized ratio: Q: ${qccQNorm.toInt()}%  C: ${qccCNorm.toInt()}%  Clay: ${qccClayNorm.toInt()}%",
                                    fontSize = 8.5.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(0.75f)
                                )
                            }
                        }
                    } else if (activeClassifierTab == 4) {
                        // Limestone Dunham Classification
                        Text(
                            text = "Dunham classification for carbonate rocks based on matrix mud abundance vs grain support, or secondary textures.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1.2f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "Carbonate Grains (Allochems): ${carbGrainNorm.toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = carbonateGrainInput,
                                    onValueChange = { carbonateGrainInput = it },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(20.dp)
                                )

                                Text(text = "Lime Mud Matrix (Micrite): ${carbMudNorm.toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = carbonateMudInput,
                                    onValueChange = { carbonateMudInput = it },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(20.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(0.8f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Checkbox(
                                        checked = carbonateBoundOrganic,
                                        onCheckedChange = { carbonateBoundOrganic = it }
                                    )
                                    Text("Bound Organic", fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Checkbox(
                                        checked = carbonateCrystalline,
                                        onCheckedChange = { carbonateCrystalline = it }
                                    )
                                    Text("Crystalline", fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Dunham Classification Result:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = classifiedDunhamName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Matrix mud: ${carbMudNorm.toInt()}% | Grains: ${carbGrainNorm.toInt()}%",
                                    fontSize = 8.5.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(0.75f)
                                )
                            }
                        }
                    } else {
                        // Metamorphic Grade & Facies Classification
                        Text(
                            text = "Metamorphic rock and metamorphic facies classifier based on protolith, temperature, and crustal depth.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1.1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "Temperature: ${metamorphicTempInput.toInt()} °C", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = metamorphicTempInput,
                                    onValueChange = { metamorphicTempInput = it },
                                    valueRange = 0f..1000f,
                                    modifier = Modifier.height(20.dp)
                                )

                                Text(text = "Depth: ${metamorphicDepthInput.toInt()} km", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = metamorphicDepthInput,
                                    onValueChange = { metamorphicDepthInput = it },
                                    valueRange = 0f..50f,
                                    modifier = Modifier.height(20.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(0.9f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Protolith Stack:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                listOf("Shale", "Basalt", "Limestone", "Sandstone", "Granite").forEach { protolith ->
                                    val isSelected = metamorphicProtolithSelected == protolith
                                    Button(
                                        onClick = { metamorphicProtolithSelected = protolith },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.fillMaxWidth().height(26.dp),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(protolith, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Metamorphic Facies & Name Result:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = classifiedMetamorphicName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                val pressureKbVal = metamorphicDepthInput * 0.27f
                                Text(
                                    text = "Estimated Pressure: ${String.format("%.2f", pressureKbVal)} kbar | Protolith: $metamorphicProtolithSelected",
                                    fontSize = 8.5.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.75f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 2. BOREHOLE LOGGING TAB: DRILLING CORE RUN ENGINE & EXPORT
// -------------------------------------------------------------------------
data class CoreRunLog(
    val boreholeId: String,
    val runNo: Int,
    val fromDepth: Double,
    val toDepth: Double,
    val recoveryPercent: Double,
    val rqdPercent: Double,
    val description: String
)

@Composable
fun BoreholeLoggingContent() {
    val context = LocalContext.current
    var boreholeId by remember { mutableStateOf("BH26-01") }
    var runNo by remember { mutableStateOf("1") }
    var fromDepth by remember { mutableStateOf("0.0") }
    var toDepth by remember { mutableStateOf("3.5") }
    var recoveryPercent by remember { mutableStateOf("95") }
    var rqdPercent by remember { mutableStateOf("80") }
    var description by remember { mutableStateOf("Dark gray weathered silicate mica schist rock") }

    var expandedForm by remember { mutableStateOf(false) }

    var runsList by remember {
        mutableStateOf(
            listOf(
                CoreRunLog("BH26-01", 1, 0.0, 3.5, 98.0, 85.0, "Upper alluvial section and powdered schist with dusty gypsum veins"),
                CoreRunLog("BH26-01", 2, 3.5, 7.2, 94.0, 78.0, "Micaceous schist containing pyritized micro-veins"),
                CoreRunLog("BH26-02", 1, 0.0, 4.0, 91.0, 64.0, "Severely weathered basalt mass containing secondary calcite mineralization")
            )
        )
    }

    // Diagnostics calculate
    val totalDepth = if (runsList.isNotEmpty()) runsList.map { it.toDepth }.maxOrNull() ?: 0.0 else 0.0
    val avgRecovery = if (runsList.isNotEmpty()) runsList.map { it.recoveryPercent }.average() else 0.0
    val avgRqd = if (runsList.isNotEmpty()) runsList.map { it.rqdPercent }.average() else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stats HUD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Max Borehole Depth", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format(Locale.US, "%.2f", totalDepth)} m", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Avg Recovery (Rec)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format(Locale.US, "%.1f", avgRecovery)}%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF43A047))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Avg Quality Factor (RQD)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format(Locale.US, "%.1f", avgRqd)}%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                }
            }
        }

        // Toggle Form
        Button(
            onClick = { expandedForm = !expandedForm },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (expandedForm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (expandedForm) "Close Logging Form" else "+ Log New Borehole Core Run", fontSize = 12.sp)
        }

        if (expandedForm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = boreholeId,
                            onValueChange = { boreholeId = it },
                            label = { Text("Borehole ID", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = runNo,
                            onValueChange = { runNo = it },
                            label = { Text("Run Number", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fromDepth,
                            onValueChange = { fromDepth = it },
                            label = { Text("From Depth (meters)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = toDepth,
                            onValueChange = { toDepth = it },
                            label = { Text("To Depth (meters)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = recoveryPercent,
                            onValueChange = { recoveryPercent = it },
                            label = { Text("Core Recovery (Rec %)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rqdPercent,
                            onValueChange = { rqdPercent = it },
                            label = { Text("Rock Quality Factor (RQD %)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Core Lithology, Alteration & Structural Description", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val rNum = runNo.toIntOrNull() ?: 1
                            val fD = fromDepth.toDoubleOrNull() ?: 0.0
                            val tD = toDepth.toDoubleOrNull() ?: 0.0
                            val rec = recoveryPercent.toDoubleOrNull() ?: 100.0
                            val rqd = rqdPercent.toDoubleOrNull() ?: 100.0
                            if (tD <= fD) {
                                Toast.makeText(context, "Error: Final depth must be greater than start depth.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            runsList = runsList + CoreRunLog(boreholeId, rNum, fD, tD, rec, rqd, description)
                            runNo = (rNum + 1).toString()
                            fromDepth = toDepth
                            toDepth = (tD + (tD - fD)).toString()
                            expandedForm = false
                            Toast.makeText(context, "Core run logged successfully in drill table!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Core Run and Add to Log Sheet", fontSize = 12.sp)
                    }
                }
            }
        }

        // Table List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Logged Borehole Core Runs Sheet", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            
            IconButton(
                onClick = {
                    val csvString = StringBuilder()
                    csvString.append("Borehole_ID,Run_No,From_Depth_m,To_Depth_m,Recovery_Percent,RQD_Percent,Description\n")
                    runsList.forEach {
                        csvString.append("${it.boreholeId},${it.runNo},${it.fromDepth},${it.toDepth},${it.recoveryPercent},${it.rqdPercent},\"${it.description.replace("\"", "\"\"")}\"\n")
                    }
                    shareFileWithApps(context, "Borehole_Core_Logs.csv", "text/csv", csvString.toString())
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Export Borehole Core Runs Sheet", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(runsList) { run ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Borehole: ${run.boreholeId} (Run ${run.runNo})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text("${run.fromDepth}m to ${run.toDepth}m", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Core Recovery Rec: ${run.recoveryPercent}%", fontSize = 11.sp, color = Color(0xFF43A047))
                            Text("Quality Factor RQD: ${run.rqdPercent}%", fontSize = 11.sp, color = Color(0xFF1E88E5))
                        }
                        Text("Lithology Description: ${run.description}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = { runsList = runsList.filter { it != run } },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete Core Run Record", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 3. ROCK MECHANICS TAB: ACTIVE RMR BIENIAWSKI CALCULATOR STYLE
// -------------------------------------------------------------------------
data class RmrLogEntry(
    val location: String,
    val score: Int,
    val rockClass: String,
    val date: String
)

@Composable
fun RockMechanicsContent() {
    val context = LocalContext.current
    var locationName by remember { mutableStateOf("Copper Mine Northern Bench Trench") }
    
    // Sliders / Options ratings state
    var ucsIndex by remember { mutableStateOf(12) } // rating values: 15, 12, 7, 4, 1
    var rqdValue by remember { mutableStateOf(75f) } // rating slider
    var spacingIndex by remember { mutableStateOf(10) } // rating values: 20, 15, 10, 8, 5
    var jointConditionIndex by remember { mutableStateOf(25) } // rating values: 30, 25, 10, 0
    var groundwaterIndex by remember { mutableStateOf(15) } // rating values: 15, 10, 4, 0
    var adjustmentIndex by remember { mutableStateOf(0) } // rating values: 0, -2, -5, -10, -12

    // Computing RQD rating
    val rqdRating = when {
        rqdValue > 90f -> 20
        rqdValue > 75f -> 17
        rqdValue > 50f -> 13
        rqdValue > 25f -> 8
        else -> 3
    }

    // Computing Total rating
    val rmrScore = ucsIndex + rqdRating + spacingIndex + jointConditionIndex + groundwaterIndex + adjustmentIndex

    val (rockClass, rockDescription, rockAdvice) = when {
        rmrScore >= 81 -> Triple("Class I", "Very Good Rock", "Stand-up time: 20 years for 15m span | Cohesion: >400 kPa | Friction angle: >45°")
        rmrScore in 61..80 -> Triple("Class II", "Good Rock", "Stand-up time: 1 year for 10m span | Cohesion: 300-400 kPa | Friction angle: 35-45°")
        rmrScore in 41..60 -> Triple("Class III", "Fair Rock", "Stand-up time: 1 week for 5m span | Cohesion: 200-300 kPa | Friction angle: 25-35°")
        rmrScore in 21..40 -> Triple("Class IV", "Poor Rock", "Stand-up time: 10 hours for 2.5m span | Cohesion: 100-200 kPa | Friction angle: 15-25°")
        else -> Triple("Class V", "Very Poor Rock", "Stand-up time: 30 minutes for 1m span, immediate support | Cohesion: <100 kPa | Friction angle: <15°")
    }

    var savedLogs by remember {
        mutableStateOf(
            listOf(
                RmrLogEntry("High grade East Wall Shear Zone Fault", 38, "Class IV", "2026-06-04"),
                RmrLogEntry("West Portal Exploration Gallery Portal Tunnel Face", 72, "Class II", "2026-06-04")
            )
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Output HUD Rmr score
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("RMR-89 Rock Mass Rating Classification Final Score", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                Text("$rmrScore Points", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(rockDescription, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Text(rockAdvice, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), lineHeight = 15.sp)
            }
        }

        // Location text
        OutlinedTextField(
            value = locationName,
            onValueChange = { locationName = it },
            label = { Text("Location tag / Shear Zone / Portal Wall / Mine Face", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Configure Rock Mass Geotechnical Parameters", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)

        // 1. UCS
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("1. Uniaxial Compressive Strength of Intact Rock (UCS)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple(">250 MPa (Very Hard)", 15, "15"),
                        Triple("100-250 (Hard)", 12, "12"),
                        Triple("50-100 (Medium)", 7, "7"),
                        Triple("<50 (Weak)", 4, "4")
                    ).forEach { (label, rat, scoreUcs) ->
                        FilterChip(
                            selected = ucsIndex == rat,
                            onClick = { ucsIndex = rat },
                            label = { Text("$scoreUcs | $label", fontSize = 8.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. RQD Slider
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("2. Rock Quality Designation (RQD)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("${rqdValue.toInt()}% (Rating Score: $rqdRating)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = rqdValue,
                    onValueChange = { rqdValue = it },
                    valueRange = 0f..100f
                )
            }
        }

        // 3. Joints Spacing
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("3. Spacing of Discontinuities (Joint Spacing)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple(">2m (Very Wide)", 20, "20"),
                        Triple("0.6-2m (Wide)", 15, "15"),
                        Triple("200-600mm", 10, "10"),
                        Triple("<200mm (Close)", 5, "5")
                    ).forEach { (label, rat, scoreSp) ->
                        FilterChip(
                            selected = spacingIndex == rat,
                            onClick = { spacingIndex = rat },
                            label = { Text("$scoreSp | $label", fontSize = 8.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Joints Condition
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("4. Condition of Discontinuities (Roughness & Infill)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple("Very Rough/Unweathered", 30, "30"),
                        Triple("Slightly Rough/<1mm Open", 25, "25"),
                        Triple("Slickensided/Glossy", 10, "10"),
                        Triple("Soft Clay Infill >5mm", 0, "0")
                    ).forEach { (label, rat, scoreJn) ->
                        FilterChip(
                            selected = jointConditionIndex == rat,
                            onClick = { jointConditionIndex = rat },
                            label = { Text("$scoreJn | $label", fontSize = 8.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. Groundwater State
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("5. Groundwater Inflow & Wetness State", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple("Completely Dry", 15, "15"),
                        Triple("Damp / Minor Seepage", 10, "10"),
                        Triple("Under Flow Pressure", 4, "4"),
                        Triple("Heavy Gushing Inflow", 0, "0")
                    ).forEach { (label, rat, scoreGw) ->
                        FilterChip(
                            selected = groundwaterIndex == rat,
                            onClick = { groundwaterIndex = rat },
                            label = { Text("$scoreGw | $label", fontSize = 8.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 6. Joint Orientation
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("6. Tunnel/Drive Alignment Discontinuity Orientation Adjustment", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple("Very Favorable", 0, "0"),
                        Triple("Favorable", -2, "-2"),
                        Triple("Fair / Medium", -5, "-5"),
                        Triple("Very Unfavorable", -12, "-12")
                    ).forEach { (label, rat, scoreAdj) ->
                        FilterChip(
                            selected = adjustmentIndex == rat,
                            onClick = { adjustmentIndex = rat },
                            label = { Text("$scoreAdj | $label", fontSize = 8.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Save & Export control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val dateToday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    savedLogs = savedLogs + RmrLogEntry(locationName, rmrScore, rockClass, dateToday)
                    Toast.makeText(context, "Rock Mass Geotechnical evaluation added to journal!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Log Rock Evaluation", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    val csvString = StringBuilder()
                    csvString.append("Location,RMR_Score,Rock_Class,Evaluation_Date\n")
                    savedLogs.forEach {
                        csvString.append("\"${it.location.replace("\"", "\"\"")}\",${it.score},${it.rockClass},${it.date}\n")
                    }
                    shareFileWithApps(context, "Rock_Mass_Rating_Logs.csv", "text/csv", csvString.toString())
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Export All to CSV", fontSize = 11.sp)
            }
        }

        Divider()

        Text("Geotechnical Rock Mechanics Studies Journal", fontWeight = FontWeight.Bold, fontSize = 13.sp)

        // Local structural list display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (savedLogs.isEmpty()) {
                    Text("No geotechnical studies logged in this session.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    savedLogs.forEach { log ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.location, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Date of Study: ${log.date}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (log.score > 60) Color(0xFF43A047).copy(alpha = 0.15f)
                                            else if (log.score > 40) Color(0xFFFFB300).copy(alpha = 0.15f)
                                            else Color(0xFFD32F2F).copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${log.score} (${log.rockClass})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (log.score > 60) Color(0xFF43A047)
                                                else if (log.score > 40) Color(0xFFE65100)
                                                else Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 4. MAP TAB: CANVAS GIS PLOTTER & TRACK RECORDER
// -------------------------------------------------------------------------
enum class MapMode { ONLINE, OFFLINE }

fun isInternetAvailable(context: android.content.Context): Boolean {
    return try {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val cap = cm.getNetworkCapabilities(net) ?: return false
        cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (e: Exception) {
        false
    }
}

fun openBrowserLink(context: android.content.Context, url: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Safe fallback
    }
}

fun generateLeafletHtml(
    stations: List<Station>,
    observations: List<Observation>,
    trackPoints: List<TrackPoint>,
    isRealPos: Boolean,
    currentLat: Double,
    currentLon: Double,
    zoom: Int,
    tileUrl: String,
    gpsAccuracy: Double,
    lockGpsCenter: Boolean
): String {
    val stationsJson = stations.joinToString(prefix = "[", postfix = "]") { st ->
        val obs = observations.firstOrNull { it.stationId == st.id && (it.strike != null || it.dip != null) }
        val strikeVal = obs?.strike ?: -1
        val dipVal = obs?.dip ?: -1
        val dipDirVal = obs?.dipDirection ?: -1
        val typeStr = obs?.observationType ?: ""
        """{"lat": ${st.latitude}, "lon": ${st.longitude}, "code": "${st.stationCode.replace("\"", "\\\"")}", "strike": $strikeVal, "dip": $dipVal, "dipDir": $dipDirVal, "type": "$typeStr"}"""
    }
    
    val trackJson = trackPoints.joinToString(prefix = "[", postfix = "]") { pt ->
        """{"lat": ${pt.lat}, "lon": ${pt.lon}}"""
    }

    val defaultLat = if (lockGpsCenter) currentLat else (if (stations.isNotEmpty()) stations.map { it.latitude }.average() else currentLat)
    val defaultLon = if (lockGpsCenter) currentLon else (if (stations.isNotEmpty()) stations.map { it.longitude }.average() else currentLon)

    val leafZoom = when (zoom) {
        1 -> 8
        2 -> 10
        3 -> 12
        4 -> 14
        5 -> 15
        6 -> 16
        7 -> 17
        8 -> 18
        else -> 14
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin="" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>
            <style>
                body { padding: 0; margin: 0; background-color: #0F1B16; }
                html, body, #map { height: 100%; width: 100%; }
                .leaflet-container { background: #0F1B16; }
                .custom-div-icon {
                    background-color: #ffb300;
                    border: 2px solid #000;
                    border-radius: 50%;
                    width: 12px;
                    height: 12px;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                }).setView([$defaultLat, $defaultLon], $leafZoom);

                L.tileLayer('$tileUrl', {
                    maxZoom: 21,
                    subdomains: ['mt0', 'mt1', 'mt2', 'mt3', 'a', 'b', 'c']
                }).addTo(map);

                var stations = $stationsJson;
                stations.forEach(function(st) {
                    var markerIcon;
                    if (st.strike >= 0 && st.dip >= 0) {
                        var rotation = st.dipDir - 180;
                        var svgHtml = '<div style="position: relative; width: 40px; height: 40px; left: -14px; top: -14px;">' +
                            '<svg width="40" height="40" viewBox="0 0 40 40" style="overflow: visible;">' +
                            '  <g transform="rotate(' + rotation + ', 20, 20)">' +
                            '    <line x1="8" y1="20" x2="32" y2="20" stroke="#ffeb3b" stroke-width="3" />' +
                            '    <line x1="20" y1="20" x2="20" y2="30" stroke="#ffeb3b" stroke-width="3" />' +
                            '  </g>' +
                            '  <text x="28" y="38" fill="#ffffff" font-size="11" font-weight="bold" font-family="sans-serif" style="text-shadow: 1px 1px 1px black;">' + st.dip + '</text>' +
                            '  <div style="position: absolute; left: 26px; top: -12px; color: #00ffcc; font-weight: bold; font-size: 10px; text-shadow: 1px 1px 1.5px black; white-space: nowrap;">' + st.code + '</div>' +
                            '</svg>' +
                            '</div>';
                        markerIcon = L.divIcon({
                            className: 'custom-struct-icon',
                            html: svgHtml,
                            iconSize: [40, 40],
                            iconAnchor: [20, 20]
                        });
                    } else {
                        markerIcon = L.divIcon({
                            className: 'custom-div-icon',
                            html: '<div style="position: absolute; left: 16px; top: -6px; color: #ffeb3b; font-weight: bold; font-size: 11px; text-shadow: 1px 1px 2px black; white-space: nowrap;">' + st.code + '</div>',
                            iconSize: [12, 12]
                        });
                    }
                    var marker = L.marker([st.lat, st.lon], { icon: markerIcon }).addTo(map);
                    var popupText = "<b>" + st.code + "</b><br>Lat: " + st.lat + "<br>Lon: " + st.lon;
                    if (st.strike >= 0 && st.dip >= 0) {
                        popupText += "<br><b>Structure Type: " + st.type + "</b><br>Strike: " + st.strike + "°<br>Dip: " + st.dip + "° (Dir: " + st.dipDir + "°)";
                    }
                    marker.bindPopup(popupText);
                });

                var trackPoints = $trackJson;
                var polyline = null;
                if (trackPoints.length > 1) {
                    var latlngs = trackPoints.map(function(pt) { return [pt.lat, pt.lon]; });
                    polyline = L.polyline(latlngs, {color: '#ff3d00', weight: 4}).addTo(map);
                }

                // GPS pulse accuracy and location dot
                var liveAccuracyCircle = null;
                var liveMarkerPoint = null;

                if ($isRealPos) {
                    liveAccuracyCircle = L.circle([$currentLat, $currentLon], {
                        color: '#00e5ff',
                        fillColor: '#00e5ff',
                        fillOpacity: 0.15,
                        weight: 1,
                        radius: $gpsAccuracy
                    }).addTo(map);

                    liveMarkerPoint = L.circleMarker([$currentLat, $currentLon], {
                        color: '#ffffff',
                        fillColor: '#29b6f6',
                        fillOpacity: 0.9,
                        weight: 2,
                        radius: 7
                    }).addTo(map);
                    liveMarkerPoint.bindPopup("GNSS Tracker Target Locked");
                }

                // Lock tracking camera logic
                if ($lockGpsCenter && $isRealPos) {
                    map.setView([$currentLat, $currentLon], $leafZoom);
                } else if (!$lockGpsCenter && polyline) {
                    map.fitBounds(polyline.getBounds());
                } else if (stations.length > 0) {
                    var markersGroup = stations.map(function(st) { return L.marker([st.lat, st.lon]); });
                    if (markersGroup.length > 0) {
                        var group = new L.featureGroup(markersGroup);
                        map.fitBounds(group.getBounds());
                    }
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}

@Composable
fun WebViewMap(
    stations: List<Station>,
    observations: List<Observation>,
    trackPoints: List<TrackPoint>,
    isRealPos: Boolean,
    currentLat: Double,
    currentLon: Double,
    zoom: Int,
    tileUrl: String,
    gpsAccuracy: Double,
    lockGpsCenter: Boolean,
    mapboxToken: String,
    modifier: Modifier = Modifier
) {
    val resolvedTileUrl = if (tileUrl.contains("{token}")) {
        tileUrl.replace("{token}", mapboxToken)
    } else {
        tileUrl
    }
    val htmlContent = remember(stations, observations, trackPoints, isRealPos, currentLat, currentLon, zoom, resolvedTileUrl, gpsAccuracy, lockGpsCenter) {
        generateLeafletHtml(stations, observations, trackPoints, isRealPos, currentLat, currentLon, zoom, resolvedTileUrl, gpsAccuracy, lockGpsCenter)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}

enum class MapLayerProvider(val label: String, val url: String) {
    GOOGLE_SATELLITE("Google Sat", "https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}"),
    GOOGLE_HYBRID("Google Hybrid", "https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}"),
    GOOGLE_TERRAIN("Google Terrain/Topo", "https://mt1.google.com/vt/lyrs=p&x={x}&y={y}&z={z}"),
    GOOGLE_ROADMAP("Google Roadmap", "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}"),
    MAPBOX_SAT_STREETS("Mapbox Satellite Streets", "https://api.mapbox.com/styles/v1/mapbox/satellite-streets-v11/tiles/{z}/{x}/{y}?access_token={token}"),
    MAPBOX_SATELLITE("Mapbox Pure Sat", "https://api.mapbox.com/styles/v1/mapbox/satellite-v9/tiles/{z}/{x}/{y}?access_token={token}"),
    MAPBOX_OUTDOORS("Mapbox Outdoors Topo", "https://api.mapbox.com/styles/v1/mapbox/outdoors-v11/tiles/{z}/{x}/{y}?access_token={token}"),
    OPEN_STREET_MAP("OSM Standard", "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"),
    OPEN_TOPO_MAP("OpenTopoMap", "https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png")
}

@Composable
fun MapContent(viewModel: GeologyViewModel) {
    val context = LocalContext.current
    val projectStations by viewModel.activeProjectStations.collectAsStateWithLifecycle()
    val allStationsList by viewModel.allStations.collectAsStateWithLifecycle()
    val savedTracks by viewModel.tracks.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val coordSystemSetting by viewModel.coordSystemSetting.collectAsStateWithLifecycle()
    val allObservationsList by viewModel.allObservations.collectAsStateWithLifecycle()

    val stationsToShow = if (projectStations.isNotEmpty()) projectStations else allStationsList
    var zoomLevel by remember { mutableStateOf(4) } // 1 to 8, default 4
    var mapMode by remember { mutableStateOf(if (isInternetAvailable(context)) MapMode.ONLINE else MapMode.OFFLINE) }
    var mapLayer by remember { mutableStateOf(MapLayerProvider.GOOGLE_SATELLITE) }
    var mapboxTokenInput by remember { mutableStateOf("pk.eyJ1IjoiZmFyemFkZiIsImEiOiJjbHpiaXdwZ3cwMTF5MndzYzhvdzBoMHp1In0.6R0R9z84Vd-M208r_6oY7g") }
    
    // UI Panel Panels states
    var isMapLayerPanelVisible by remember { mutableStateOf(false) }
    var isSurveyorPanelVisible by remember { mutableStateOf(false) }
    var isJournalPanelVisible by remember { mutableStateOf(false) }
    var isSettingsPanelVisible by remember { mutableStateOf(false) }
    var isAddPointDialogVisible by remember { mutableStateOf(false) }
    var isOutdoorMode by remember { mutableStateOf(false) }
    var showSimulator by remember { mutableStateOf(false) }

    // GNSS values & custom settings
    var satelliteCount by remember { mutableStateOf(16) }
    var gpsAccuracy by remember { mutableStateOf(1.8) }
    var lockGpsCenter by remember { mutableStateOf(true) }
    
    val scale = remember(zoomLevel) {
        when (zoomLevel) {
            1 -> 40000.0
            2 -> 120000.0
            3 -> 350000.0
            4 -> 800000.0
            5 -> 1800000.0
            6 -> 4000000.0
            7 -> 9000000.0
            8 -> 20000000.0
            else -> 800000.0
        }
    }

    val isTrackRecordingEnabled by viewModel.isTrackRecordingActive.collectAsStateWithLifecycle()
    val activePointsList by viewModel.activePoints.collectAsStateWithLifecycle()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()

    var isRealPositionEnabled by remember { mutableStateOf(false) }

    // Option switches
    var rotateMapUsingGnss by remember { mutableStateOf(true) }
    var displayLineFromPosition by remember { mutableStateOf(true) }
    var positioningStatus by remember { mutableStateOf(true) }
    var trackRecording by remember { mutableStateOf(false) }

    // Start location tracking if real position is chosen
    LaunchedEffect(isRealPositionEnabled) {
        if (isRealPositionEnabled) {
            viewModel.startLocationTracking(context)
        } else if (!isTrackRecordingEnabled) {
            viewModel.stopLocationTracking()
        }
    }

    // GNSS linked rules
    fun onRealPositionChanged(enabled: Boolean) {
        isRealPositionEnabled = enabled
        if (enabled) {
            rotateMapUsingGnss = true
            displayLineFromPosition = true
            positioningStatus = true
        } else {
            if (isTrackRecordingEnabled) {
                viewModel.stopRecordingTrack { _, _, _ -> }
                trackRecording = false
            }
        }
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var tempTrackPointsJson by remember { mutableStateOf("") }
    var tempTrackLength by remember { mutableStateOf(0.0) }
    var tempTrackDuration by remember { mutableStateOf(0L) }
    var newTrackName by remember { mutableStateOf("") }
    var exportFormatChoice by remember { mutableStateOf("GPX") }

    fun onTrackRecordingChanged(enabled: Boolean) {
        trackRecording = enabled
        if (enabled) {
            isRealPositionEnabled = true
            rotateMapUsingGnss = true
            displayLineFromPosition = true
            positioningStatus = true
            
            viewModel.startLocationTracking(context)
            viewModel.startRecordingTrack()
        } else {
            viewModel.stopRecordingTrack { pointsJson, length, duration ->
                tempTrackPointsJson = pointsJson
                tempTrackLength = if (length <= 0.0) 245.0 else length
                tempTrackDuration = if (duration <= 0) 120 else duration
                newTrackName = "Track_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                showSaveDialog = true
            }
        }
    }

    // Coordinates Simulation Walker
    var simulatorLat by remember { mutableStateOf(35.69439) }
    var simulatorLon by remember { mutableStateOf(51.42151) }
    var simulatorHeading by remember { mutableStateOf(45.0) }

    val currentLat = if (isRealPositionEnabled && liveLocation != null) liveLocation!!.latitude else simulatorLat
    val currentLon = if (isRealPositionEnabled && liveLocation != null) liveLocation!!.longitude else simulatorLon
    val currentHeading = if (isRealPositionEnabled && liveLocation != null) liveLocation!!.bearing.toDouble() else simulatorHeading

    fun simulateWalkMove(direction: String) {
        val stepSize = 0.00015
        val headingAngle = when (direction) {
            "NORTH" -> 0.0
            "SOUTH" -> 180.0
            "EAST" -> 90.0
            "WEST" -> 270.0
            else -> simulatorHeading
        }
        simulatorHeading = headingAngle
        
        val rad = Math.toRadians(headingAngle)
        simulatorLat += stepSize * Math.cos(rad)
        simulatorLon += stepSize * Math.sin(rad)
        
        satelliteCount = (12..21).random()
        gpsAccuracy = 1.1 + Math.random() * 1.8

        if (isTrackRecordingEnabled) {
            viewModel.addSimulatedPoint(
                TrackPoint(
                    lat = simulatorLat,
                    lon = simulatorLon,
                    ele = 1210.0 + (Math.random() - 0.5) * 4,
                    time = System.currentTimeMillis(),
                    accuracy = gpsAccuracy,
                    heading = simulatorHeading
                )
            )
        }
    }

    // Saved track editing inline states
    var editTrackItem by remember { mutableStateOf<Track?>(null) }
    var editTrackItemName by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }

    // Add Point Dialog variables
    var quickStationCode by remember { mutableStateOf("") }
    var quickStationPrefix by remember { mutableStateOf("ST") }
    
    LaunchedEffect(quickStationPrefix, activeProject, isAddPointDialogVisible) {
        val projId = activeProject?.id
        if (projId != null && isAddPointDialogVisible) {
            viewModel.getNextStationCodeForPrefix(projId, quickStationPrefix) { generated ->
                quickStationCode = generated
            }
        }
    }

    // Main Full-screen Box
    val mapBgColor = if (isOutdoorMode) Color(0xFFF5F6F5) else Color(0xFF0C100D)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(mapBgColor)
    ) {
        // -------------------------------------------------------------
        // LAYER 1: THE MAP CANVAS / WEBVIEW (OCCUPIES 100% SIZE)
        // -------------------------------------------------------------
        if (mapMode == MapMode.ONLINE) {
            WebViewMap(
                stations = stationsToShow,
                observations = allObservationsList,
                trackPoints = activePointsList,
                isRealPos = isRealPositionEnabled,
                currentLat = currentLat,
                currentLon = currentLon,
                zoom = zoomLevel,
                tileUrl = mapLayer.url,
                gpsAccuracy = gpsAccuracy,
                lockGpsCenter = lockGpsCenter,
                mapboxToken = mapboxTokenInput,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val strokeColor = if (isOutdoorMode) Color(0xFFB0BEC5) else Color(0x3300E5FF) // High-contrast steel-gray or neon cyber cyan grid lines

                // Render vector grids
                for (x in 0..w.toInt() step 60) {
                    drawLine(color = strokeColor, start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f), end = androidx.compose.ui.geometry.Offset(x.toFloat(), h), strokeWidth = 0.5f)
                }
                for (y in 0..h.toInt() step 60) {
                    drawLine(color = strokeColor, start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()), end = androidx.compose.ui.geometry.Offset(w, y.toFloat()), strokeWidth = 0.5f)
                }

                val baseLat = if (lockGpsCenter && isRealPositionEnabled) currentLat else (if (stationsToShow.isNotEmpty()) stationsToShow.map { it.latitude }.average() else currentLat)
                val baseLon = if (lockGpsCenter && isRealPositionEnabled) currentLon else (if (stationsToShow.isNotEmpty()) stationsToShow.map { it.longitude }.average() else currentLon)

                fun toCanvasX(longitude: Double): Float {
                    return (w / 2f + (longitude - baseLon) * scale * Math.cos(Math.toRadians(baseLat))).toFloat()
                }
                fun toCanvasY(latitude: Double): Float {
                    return (h / 2f - (latitude - baseLat) * scale).toFloat()
                }

                // Draw logged stations
                stationsToShow.forEach { station ->
                    val cx = toCanvasX(station.longitude)
                    val cy = toCanvasY(station.latitude)
                    if (cx in 0f..w && cy in 0f..h) {
                        val outerColor = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF)
                        val innerColor = if (isOutdoorMode) Color(0xFFFAFAFA) else Color(0xFF0B1411)
                        val dotColor = if (isOutdoorMode) Color(0xFFFF6D00) else Color(0xFFFFB300)

                        // Core structural symbol drawing
                        val obs = allObservationsList.firstOrNull { it.stationId == station.id && (it.strike != null || it.dip != null) }
                        val strike = obs?.strike
                        val dip = obs?.dip
                        val dipDir = obs?.dipDirection
                        if (strike != null && dip != null && dipDir != null) {
                            val rad = Math.toRadians((dipDir - 180).toDouble())
                            val L_line = 24f
                            val L_tick = 10f
                            
                            // Strike line resides at orthogonal direction
                            val sx = -Math.cos(rad) * L_line
                            val sy = -Math.sin(rad) * L_line
                            
                            val dx = Math.sin(rad) * L_tick
                            val dy = -Math.cos(rad) * L_tick
                            
                            drawLine(
                                color = if (isOutdoorMode) Color(0xFFC62828) else Color(0xFFFFEB3B),
                                start = androidx.compose.ui.geometry.Offset((cx - sx).toFloat(), (cy - sy).toFloat()),
                                end = androidx.compose.ui.geometry.Offset((cx + sx).toFloat(), (cy + sy).toFloat()),
                                strokeWidth = 3.5f
                            )
                            drawLine(
                                color = if (isOutdoorMode) Color(0xFFC62828) else Color(0xFFFFEB3B),
                                start = androidx.compose.ui.geometry.Offset(cx, cy),
                                end = androidx.compose.ui.geometry.Offset((cx + dx).toFloat(), (cy + dy).toFloat()),
                                strokeWidth = 3.5f
                            )
                            
                            drawContext.canvas.nativeCanvas.drawText(
                                dip.toString(),
                                (cx + dx * 1.8).toFloat(),
                                (cy + dy * 1.8 + 8f).toFloat(),
                                android.graphics.Paint().apply {
                                    color = if (isOutdoorMode) android.graphics.Color.RED else android.graphics.Color.YELLOW
                                    textSize = 22f
                                    isFakeBoldText = true
                                }
                            )
                        } else {
                            // Standard geological mapping diamond node
                            drawCircle(color = outerColor, radius = 8f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                            drawCircle(color = innerColor, radius = 4.5f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                            drawCircle(color = dotColor, radius = 2.5f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                        }
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            station.stationCode,
                            cx + 14f,
                            cy + 8f,
                            android.graphics.Paint().apply {
                                color = if (isOutdoorMode) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                                textSize = 24f
                                isFakeBoldText = true
                                if (isOutdoorMode) {
                                    setShadowLayer(2f, 1f, 1f, android.graphics.Color.WHITE)
                                } else {
                                    setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
                                }
                            }
                        )
                    }
                }

                // Draw Track recorded paths
                if (activePointsList.size >= 2) {
                    val pathColor = if (isOutdoorMode) Color(0xFFD50000) else Color(0xFFFF3D00)
                    for (i in 0 until activePointsList.size - 1) {
                        val p1 = activePointsList[i]
                        val p2 = activePointsList[i+1]
                        val x1 = toCanvasX(p1.lon)
                        val y1 = toCanvasY(p1.lat)
                        val x2 = toCanvasX(p2.lon)
                        val y2 = toCanvasY(p2.lat)
                        drawLine(color = pathColor, start = androidx.compose.ui.geometry.Offset(x1, y1), end = androidx.compose.ui.geometry.Offset(x2, y2), strokeWidth = 5f)
                    }
                }

                // Simulated sight line & user radar
                if (isRealPositionEnabled) {
                    val cx = toCanvasX(currentLon)
                    val cy = toCanvasY(currentLat)
                    if (cx in 0f..w && cy in 0f..h) {
                        if (displayLineFromPosition) {
                            val lineLength = 160f
                            val angleRad = Math.toRadians(currentHeading)
                            val endX = cx + lineLength * Math.sin(angleRad).toFloat()
                            val endY = cy - lineLength * Math.cos(angleRad).toFloat()
                            drawLine(
                                color = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF),
                                start = androidx.compose.ui.geometry.Offset(cx, cy),
                                end = androidx.compose.ui.geometry.Offset(endX, endY),
                                strokeWidth = 3f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                            )
                        }

                        val pxAccRadius = (gpsAccuracy * (scale / 111320.0)).toFloat()
                        drawCircle(color = if (isOutdoorMode) Color(0x3300796B) else Color(0x1A00E5FF), radius = Math.max(16f, pxAccRadius), center = androidx.compose.ui.geometry.Offset(cx, cy))
                        drawCircle(color = if (isOutdoorMode) Color(0xFF0288D1) else Color(0xFF00B0FF), radius = 10f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                        drawCircle(color = Color.White, radius = 4f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // LAYER 2: ALPINEQUEST TARGET CENTRAL CROSSHAIR INDEX
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.Center)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val color = Color(0xFF00FFE0) // Neon turquoise target index crosshair
                val stroke = 1.5.dp.toPx()
                drawLine(color = color.copy(alpha = 0.6f), start = androidx.compose.ui.geometry.Offset(0f, size.height / 2), end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2), strokeWidth = stroke)
                drawLine(color = color.copy(alpha = 0.6f), start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f), end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height), strokeWidth = stroke)
                drawCircle(color = color, radius = 6.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
            }
        }

        // -------------------------------------------------------------
        // LAYER 3: FLOATING SIGHT CENTER COORDINATE STATUS RIBBON (BOTTOM CENTER)
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isOutdoorMode) Color.White else Color(0xE6101512)) // Dynamic background
                .border(2.dp, if (isOutdoorMode) Color.Black else Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val centerLat = if (lockGpsCenter && isRealPositionEnabled) currentLat else (if (stationsToShow.isNotEmpty()) stationsToShow.map { it.latitude }.average() else currentLat)
            val centerLon = if (lockGpsCenter && isRealPositionEnabled) currentLon else (if (stationsToShow.isNotEmpty()) stationsToShow.map { it.longitude }.average() else currentLon)
            val utmResult = UtmConverter.convertLatLngToUtm(centerLat, centerLon)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CENTER COORDINATES",
                    fontSize = 8.sp,
                    color = if (isOutdoorMode) Color.DarkGray else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "LAT: ${String.format(Locale.US, "%.6f°N", centerLat)} | LON: ${String.format(Locale.US, "%.6f°E", centerLon)}",
                    fontSize = 11.sp,
                    color = if (isOutdoorMode) Color(0xFF004D40) else Color(0xFF00FFE0),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "UTM Grid: $utmResult",
                    fontSize = 9.sp,
                    color = if (isOutdoorMode) Color.Black else Color.White.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // -------------------------------------------------------------
        // LAYER 4: GNSS TELEMETRY HEADS-UP DISPLAY (TOP-LEFT FLOATING CARD)
        // -------------------------------------------------------------
        if (positioningStatus && isRealPositionEnabled) {
            val isAccuracyGood = gpsAccuracy <= 2.5
            val accuracyColor = if (isAccuracyGood) {
                if (isOutdoorMode) Color(0xFF2E7D32) else Color(0xFF00FF00)
            } else {
                Color(0xFFFF5252)
            }
            var isTelemetryExpanded by remember { mutableStateOf(true) }
            
            Card(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
                    .width(if (isTelemetryExpanded) 220.dp else 120.dp)
                    .clickable { isTelemetryExpanded = !isTelemetryExpanded },
                colors = CardDefaults.cardColors(
                    containerColor = if (isOutdoorMode) Color.White else Color(0xCC151B17)
                ),
                border = BorderStroke(2.dp, if (isOutdoorMode) Color.Black else Color(0xFF00E5FF).copy(alpha = 0.4f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Pulsing active solid LED accuracy lock status dot
                            var ledAlpha by remember { mutableStateOf(1f) }
                            LaunchedEffect(Unit) {
                                while(true) {
                                    ledAlpha = 0.3f
                                    delay(400L)
                                    ledAlpha = 1f
                                    delay(400L)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(accuracyColor.copy(alpha = ledAlpha), CircleShape)
                                    .border(1.dp, if (isOutdoorMode) Color.Black else Color.White, CircleShape)
                            )
                            
                            Text(
                                text = if (isTelemetryExpanded) {
                                    if (isAccuracyGood) "GNSS LOCK: 3D RTK" else "GNSS POOR ACCY"
                                } else "GNSS RTK",
                                fontSize = 8.5.sp,
                                color = accuracyColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (isTelemetryExpanded) {
                            Box(
                                modifier = Modifier
                                    .background(if (isOutdoorMode) Color(0xFFEEEEEE) else Color(0xFF00E5FF).copy(0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Sats: $satelliteCount",
                                    color = if (isOutdoorMode) Color.Black else Color(0xFF00E5FF),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (isTelemetryExpanded) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val labelTextCol = if (isOutdoorMode) Color.Black else Color.White
                        
                        Text("LAT: ${String.format(Locale.US, "%.5f°N", currentLat)}", fontSize = 9.sp, color = labelTextCol, fontFamily = FontFamily.Monospace)
                        Text("LON: ${String.format(Locale.US, "%.5f°E", currentLon)}", fontSize = 9.sp, color = labelTextCol, fontFamily = FontFamily.Monospace)
                        Text("ALTITUDE: 1210m MSL", fontSize = 9.sp, color = labelTextCol, fontFamily = FontFamily.Monospace)
                        Text("PRECISION: ±${String.format(Locale.US, "%.1f", gpsAccuracy)} meters", fontSize = 8.5.sp, color = accuracyColor)
                        
                        if (isTrackRecordingEnabled) {
                            val minutes = recordingDurationSec / 60
                            val seconds = recordingDurationSec % 60
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFF3D00).copy(0.12f), RoundedCornerShape(4.dp))
                                    .padding(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                                Text(
                                    "REC TIMER: ${String.format("%02d:%02d", minutes, seconds)}",
                                    fontSize = 8.5.sp,
                                    color = Color(0xFFFF5252),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text("Click to expand", fontSize = 7.5.sp, color = if (isOutdoorMode) Color.Black else Color.Gray)
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // LAYER 5: MAIN FLOATING GIS VERTICAL BAR (TOP-RIGHT)
        // -------------------------------------------------------------
        val toolbarBgColor = if (isOutdoorMode) Color.White else Color(0xCC1E2522)
        val toolbarBorderColor = if (isOutdoorMode) Color.Black else Color(0xFF00E5FF).copy(0.4f)
        val toolbarIconColorOpen = if (isOutdoorMode) Color.White else Color.Black
        val toolbarBgOpen = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF)
        val toolbarIconColorClosed = if (isOutdoorMode) Color.Black else Color.White

        Column(
            modifier = Modifier
                .padding(top = 12.dp, end = 12.dp)
                .align(Alignment.TopEnd),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Quick Add Point / Save Station (Large touch target button)
            IconButton(
                onClick = { isAddPointDialogVisible = true },
                modifier = Modifier
                    .size(44.dp)
                    .background(if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF), RoundedCornerShape(10.dp))
                    .border(1.5.dp, if (isOutdoorMode) Color.Black else Color.White, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.AddLocation,
                    contentDescription = "Quick Add Geological Station Point",
                    tint = if (isOutdoorMode) Color.White else Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Map Layer switcher button
            IconButton(
                onClick = { 
                    isMapLayerPanelVisible = !isMapLayerPanelVisible
                    isSurveyorPanelVisible = false
                    isJournalPanelVisible = false
                    isSettingsPanelVisible = false
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isMapLayerPanelVisible) toolbarBgOpen else toolbarBgColor, CircleShape)
                    .border(1.dp, if (isMapLayerPanelVisible) Color.White else toolbarBorderColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Map Layer Switcher",
                    tint = if (isMapLayerPanelVisible) toolbarIconColorOpen else toolbarIconColorClosed,
                    modifier = Modifier.size(20.dp)
                )
            }

            // GPS Lock re-center button
            IconButton(
                onClick = { lockGpsCenter = !lockGpsCenter },
                modifier = Modifier
                    .size(40.dp)
                    .background(if (lockGpsCenter) (if (isOutdoorMode) Color(0xFF0288D1) else Color(0xFF00B0FF)) else toolbarBgColor, CircleShape)
                    .border(1.dp, if (lockGpsCenter) Color.White else toolbarBorderColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "Lock Camera to GNSS Center",
                    tint = if (lockGpsCenter) Color.White else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Track recording button
            IconButton(
                onClick = { 
                    onTrackRecordingChanged(!isTrackRecordingEnabled)
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isTrackRecordingEnabled) Color(0xFFFF3030) else toolbarBgColor, CircleShape)
                    .border(1.dp, if (isTrackRecordingEnabled) Color.White else toolbarBorderColor, CircleShape)
            ) {
                Icon(
                    imageVector = if (isTrackRecordingEnabled) Icons.Default.Stop else Icons.Default.RadioButtonChecked,
                    contentDescription = "Track Recorder Play Toggle",
                    tint = if (isTrackRecordingEnabled) Color.White else Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Walk Simulator Toggle button
            IconButton(
                onClick = { showSimulator = !showSimulator },
                modifier = Modifier
                    .size(40.dp)
                    .background(if (showSimulator) toolbarBgOpen else toolbarBgColor, CircleShape)
                    .border(1.dp, if (showSimulator) Color.White else toolbarBorderColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsWalk,
                    contentDescription = "Simulator Walk Controller Toggle",
                    tint = if (showSimulator) toolbarIconColorOpen else toolbarIconColorClosed,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Surveyor Distance & Bearing Calculator
            IconButton(
                onClick = { 
                    isSurveyorPanelVisible = !isSurveyorPanelVisible
                    isMapLayerPanelVisible = false
                    isJournalPanelVisible = false
                    isSettingsPanelVisible = false
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isSurveyorPanelVisible) toolbarBgOpen else toolbarBgColor, CircleShape)
                    .border(1.dp, if (isSurveyorPanelVisible) Color.White else toolbarBorderColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Surveyor Calculator Overlay",
                    tint = if (isSurveyorPanelVisible) toolbarIconColorOpen else toolbarIconColorClosed,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Saved Journals database track list
            IconButton(
                onClick = { 
                    isJournalPanelVisible = !isJournalPanelVisible
                    isMapLayerPanelVisible = false
                    isSurveyorPanelVisible = false
                    isSettingsPanelVisible = false
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isJournalPanelVisible) toolbarBgOpen else toolbarBgColor, CircleShape)
                    .border(1.dp, if (isJournalPanelVisible) Color.White else toolbarBorderColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Field Logs Track Journal list",
                    tint = if (isJournalPanelVisible) toolbarIconColorOpen else toolbarIconColorClosed,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Advanced GIS Engine Settings Toggle
            IconButton(
                onClick = { 
                    isSettingsPanelVisible = !isSettingsPanelVisible
                    isMapLayerPanelVisible = false
                    isSurveyorPanelVisible = false
                    isJournalPanelVisible = false
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isSettingsPanelVisible) toolbarBgOpen else toolbarBgColor, CircleShape)
                    .border(1.dp, if (isSettingsPanelVisible) Color.White else toolbarBorderColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Advanced System Options",
                    tint = if (isSettingsPanelVisible) toolbarIconColorOpen else toolbarIconColorClosed,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Outdoor Legibility Sunlight Contrast switcher
            IconButton(
                onClick = { isOutdoorMode = !isOutdoorMode },
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isOutdoorMode) Color(0xFFFFD54F) else toolbarBgColor, CircleShape)
                    .border(1.dp, if (isOutdoorMode) Color.Black else toolbarBorderColor, CircleShape)
            ) {
                Icon(
                    imageVector = if (isOutdoorMode) Icons.Default.WbSunny else Icons.Default.BrightnessMedium,
                    contentDescription = "Toggle Outdoor High-Contrast Mode",
                    tint = if (isOutdoorMode) Color.Black else Color(0xFFFFD54F),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Compact Zoom In/Out separate buttons
            IconButton(
                onClick = { if (zoomLevel < 8) zoomLevel++ },
                modifier = Modifier
                    .size(36.dp)
                    .background(toolbarBgColor, RoundedCornerShape(4.dp))
                    .border(0.5.dp, toolbarBorderColor, RoundedCornerShape(4.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = toolbarIconColorClosed, modifier = Modifier.size(16.dp))
            }
            Text(
                text = "x$zoomLevel", 
                color = if (isOutdoorMode) Color.Black else Color(0xFF00FFE0), 
                fontSize = 8.sp, 
                fontWeight = FontWeight.Bold, 
                fontFamily = FontFamily.Monospace
            )
            IconButton(
                onClick = { if (zoomLevel > 1) zoomLevel-- },
                modifier = Modifier
                    .size(36.dp)
                    .background(toolbarBgColor, RoundedCornerShape(4.dp))
                    .border(0.5.dp, toolbarBorderColor, RoundedCornerShape(4.dp))
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = toolbarIconColorClosed, modifier = Modifier.size(16.dp))
            }
        }

        // -------------------------------------------------------------
        // LAYER 6: FLOATING SIMULATED WALK D-PAD CONTROLLER (BOTTOM-RIGHT)
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = showSimulator,
            enter = fadeIn() + expandIn(),
            exit = fadeOut() + shrinkOut(),
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.BottomEnd)
        ) {
            Card(
                modifier = Modifier.width(135.dp),
                colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color.White else Color(0xE61E2522)),
                border = BorderStroke(2.dp, if (isOutdoorMode) Color.Black else Color(0xFF00E5FF).copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "WALK SIMULATOR", 
                        fontSize = 7.5.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = if (isOutdoorMode) Color.Black else Color.LightGray
                    )
                    
                    // N key on top row
                    IconButton(
                        onClick = { simulateWalkMove("NORTH") },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF263238), CircleShape)
                    ) {
                        Text("N", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                    }
                    
                    // West and East middle row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { simulateWalkMove("WEST") },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF263238), CircleShape)
                        ) {
                            Text("W", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { simulateWalkMove("EAST") },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF263238), CircleShape)
                        ) {
                            Text("E", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    // South key on bottom row
                    IconButton(
                        onClick = { simulateWalkMove("SOUTH") },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF263238), CircleShape)
                    ) {
                        Text("S", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // LAYER 7: FIELD INFO HELP BADGE (BOTTOM-LEFT)
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Field GIS Dashboard Loaded.", fontSize = 7.5.sp, color = Color.White)
                Text("Tap map controls for overlays.", fontSize = 7.sp, color = Color.Gray)
            }
        }

        // -------------------------------------------------------------
        // DYNAMIC OVERLAY 1: FLOATING MAP LAYER SWITCHER PANEL
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = isMapLayerPanelVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .padding(top = 80.dp, end = 75.dp)
                .align(Alignment.TopEnd)
        ) {
            Card(
                modifier = Modifier.width(260.dp),
                colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color.White else Color(0xFA1A201C)),
                border = BorderStroke(1.5.dp, if (isOutdoorMode) Color.Black else Color(0xFF00E5FF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Map Layer Provider",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isOutdoorMode) Color(0xFFF1F1F1) else Color.Black.copy(0.3f), RoundedCornerShape(6.dp))
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = { mapMode = MapMode.ONLINE },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mapMode == MapMode.ONLINE) (if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF)) else Color.Transparent,
                                contentColor = if (mapMode == MapMode.ONLINE) (if (isOutdoorMode) Color.White else Color.Black) else (if (isOutdoorMode) Color.Black else Color.White)
                            ),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Online Webs", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { mapMode = MapMode.OFFLINE },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mapMode == MapMode.OFFLINE) (if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF)) else Color.Transparent,
                                contentColor = if (mapMode == MapMode.OFFLINE) (if (isOutdoorMode) Color.White else Color.Black) else (if (isOutdoorMode) Color.Black else Color.White)
                            ),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Offline Grid", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (mapMode == MapMode.ONLINE) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Standard Map Layers:", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            val visibleLayers = MapLayerProvider.values()
                            visibleLayers.forEach { provider ->
                                val selected = mapLayer == provider
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            mapLayer = provider 
                                            isMapLayerPanelVisible = false
                                        }
                                        .background(
                                            if (selected) (if (isOutdoorMode) Color(0xFF00796B).copy(0.12f) else Color(0xFF00E5FF).copy(0.12f)) else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = if (selected) (if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF)) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = provider.label,
                                        fontSize = 11.sp,
                                        color = if (selected) (if (isOutdoorMode) Color(0xFF004D40) else Color(0xFF00FFE0)) else (if (isOutdoorMode) Color.Black else Color.White)
                                    )
                                }
                            }
                        }
                    } else {
                        // Offline specifications
                        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Offline Vector Grid Mode Active", fontSize = 10.sp, color = if (isOutdoorMode) Color.Black else Color.LightGray)
                            Text("Allows field recording of tracks and targets cleanly without high-resolution base layers if remote signal is offline.", fontSize = 9.sp, color = Color.Gray, lineHeight = 13.sp)
                        }
                    }
                    
                    Divider(color = if (isOutdoorMode) Color.LightGray else Color.White.copy(0.15f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Outdoor External Maps:", fontSize = 8.5.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { openBrowserLink(context, "https://www.google.com/maps/search/?api=1&query=$currentLat,$currentLon") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = "Google Maps Web", tint = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                            }
                            IconButton(
                                onClick = { openBrowserLink(context, "https://www.openstreetmap.org/#map=17/$currentLat/$currentLon") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Explore, contentDescription = "OSM Web", tint = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // DYNAMIC OVERLAY 2: SURVEYOR COORDINATE SYSTEM & AZIMUTH TOOL
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = isSurveyorPanelVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .padding(top = 170.dp, end = 75.dp)
                .align(Alignment.TopEnd)
        ) {
            var targetLatStr by remember { mutableStateOf("35.7000") }
            var targetLonStr by remember { mutableStateOf("51.4300") }

            val targetLatVal = targetLatStr.toDoubleOrNull() ?: 35.7
            val targetLonVal = targetLonStr.toDoubleOrNull() ?: 51.43

            // Great circle distance (Haversine formula in meters)
            val lat1Rad = Math.toRadians(currentLat)
            val lat2Rad = Math.toRadians(targetLatVal)
            val deltaLatRad = Math.toRadians(targetLatVal - currentLat)
            val deltaLonRad = Math.toRadians(targetLonVal - currentLon)
            val a = Math.sin(deltaLatRad / 2.0) * Math.sin(deltaLatRad / 2.0) +
                    Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                    Math.sin(deltaLonRad / 2.0) * Math.sin(deltaLonRad / 2.0)
            val cVal = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a))
            val computedDistanceMeters = 6371000.0 * cVal

            // Forward Azimuth Bearing (degrees)
            val yAzimuth = Math.sin(deltaLonRad) * Math.cos(lat2Rad)
            val xAzimuth = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                    Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad)
            val bearingDeg = (Math.toDegrees(Math.atan2(yAzimuth, xAzimuth)) + 360.0) % 360.0

            Card(
                modifier = Modifier.width(280.dp),
                colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color.White else Color(0xFF161C19)),
                border = BorderStroke(1.5.dp, if (isOutdoorMode) Color.Black else Color(0xFF00E5FF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Explore, contentDescription = null, tint = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                            Text("Bearing & Distance Surveyor", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = if (isOutdoorMode) Color.Black else Color.White)
                        }
                        IconButton(onClick = { isSurveyorPanelVisible = false }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Text(
                        text = "High-precision geodesic distance to borehole target",
                        fontSize = 9.sp,
                        color = if (isOutdoorMode) Color.DarkGray else Color.LightGray,
                        lineHeight = 13.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = targetLatStr,
                            onValueChange = { targetLatStr = it },
                            label = { Text("Target Lat (°)", fontSize = 8.sp, color = if (isOutdoorMode) Color.Black else Color.LightGray) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = if (isOutdoorMode) Color.Black else Color.White, fontFamily = FontFamily.Monospace),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isOutdoorMode) Color.Black else Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.Gray.copy(0.5f),
                                focusedLabelColor = if (isOutdoorMode) Color.Black else Color(0xFF00E5FF),
                                focusedContainerColor = if (isOutdoorMode) Color(0xFFF1F1F1) else Color.Black.copy(0.2f),
                                unfocusedContainerColor = if (isOutdoorMode) Color(0xFFF5F5F5) else Color.Black.copy(0.2f)
                            )
                        )
                        OutlinedTextField(
                            value = targetLonStr,
                            onValueChange = { targetLonStr = it },
                            label = { Text("Target Lng (°)", fontSize = 8.sp, color = if (isOutdoorMode) Color.Black else Color.LightGray) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = if (isOutdoorMode) Color.Black else Color.White, fontFamily = FontFamily.Monospace),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isOutdoorMode) Color.Black else Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.Gray.copy(0.5f),
                                focusedLabelColor = if (isOutdoorMode) Color.Black else Color(0xFF00E5FF),
                                focusedContainerColor = if (isOutdoorMode) Color(0xFFF1F1F1) else Color.Black.copy(0.2f),
                                unfocusedContainerColor = if (isOutdoorMode) Color(0xFFF5F5F5) else Color.Black.copy(0.2f)
                            )
                        )
                    }

                    if (stationsToShow.isNotEmpty()) {
                        var snapExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { snapExpanded = !snapExpanded },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isOutdoorMode) Color.Black else Color(0xFF00E5FF)),
                                border = BorderStroke(0.5.dp, if (isOutdoorMode) Color.Black else Color(0xFF00E5FF).copy(0.6f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Snap targeting to mapped station", fontSize = 10.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            
                            DropdownMenu(
                                expanded = snapExpanded,
                                onDismissRequest = { snapExpanded = false },
                                modifier = Modifier.background(if (isOutdoorMode) Color.White else Color(0xFF161C19)).border(1.dp, if (isOutdoorMode) Color.Black else Color(0xFF00E5FF))
                            ) {
                                stationsToShow.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s.stationCode} (Lat: ${s.latitude})", fontSize = 11.sp, color = if (isOutdoorMode) Color.Black else Color.White) },
                                        onClick = {
                                            targetLatStr = s.latitude.toString()
                                            targetLonStr = s.longitude.toString()
                                            snapExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color(0xFFEEEEEE) else Color.Black.copy(0.4f)),
                        border = BorderStroke(0.5.dp, if (isOutdoorMode) Color.Black else Color(0xFF00E5FF).copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Spheroidal Dist", fontSize = 8.5.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                val dStr = if (computedDistanceMeters >= 1000.0) {
                                    "${String.format(Locale.US, "%.3f", computedDistanceMeters / 1000.0)} km"
                                } else {
                                    "${computedDistanceMeters.toInt()} m"
                                }
                                Text(dStr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isOutdoorMode) Color(0xFF004D40) else Color(0xFF00FFE0))
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(0.5f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("True Bearing", fontSize = 8.5.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("${String.format(Locale.US, "%.1f° N", bearingDeg)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isOutdoorMode) Color(0xFF004D40) else Color(0xFF00FFE0))
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // DYNAMIC OVERLAY 3: FLOATING SENSOR & MAPBOX CONFIG SETTINGS PANEL
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = isSettingsPanelVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .padding(top = 110.dp, end = 75.dp)
                .align(Alignment.TopEnd)
        ) {
            Card(
                modifier = Modifier.width(280.dp),
                colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color.White else Color(0xFF151817)),
                border = BorderStroke(1.5.dp, if (isOutdoorMode) Color.Black else Color(0xFF00E5FF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                            Text("Advanced GIS Configs", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = if (isOutdoorMode) Color.Black else Color.White)
                        }
                        IconButton(onClick = { isSettingsPanelVisible = false }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("MAPPING TELEMETRIES:", fontSize = 9.sp, color = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Live GNSS lock positioning", fontSize = 10.5.sp, color = if (isOutdoorMode) Color.Black else Color.White)
                            Checkbox(
                                checked = isRealPositionEnabled, 
                                onCheckedChange = { onRealPositionChanged(it) },
                                colors = CheckboxDefaults.colors(checkedColor = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF))
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Compass map auto orientation", fontSize = 10.5.sp, color = if (isOutdoorMode) Color.Black else Color.White)
                            Checkbox(
                                checked = rotateMapUsingGnss, 
                                onCheckedChange = { rotateMapUsingGnss = it },
                                colors = CheckboxDefaults.colors(checkedColor = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF))
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Line of sight dashed indicator", fontSize = 10.5.sp, color = if (isOutdoorMode) Color.Black else Color.White)
                            Checkbox(
                                checked = displayLineFromPosition, 
                                onCheckedChange = { displayLineFromPosition = it },
                                colors = CheckboxDefaults.colors(checkedColor = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF))
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Telemetry status overlays", fontSize = 10.5.sp, color = if (isOutdoorMode) Color.Black else Color.White)
                            Checkbox(
                                checked = positioningStatus, 
                                onCheckedChange = { positioningStatus = it },
                                colors = CheckboxDefaults.colors(checkedColor = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF))
                            )
                        }
                    }

                    Divider(color = if (isOutdoorMode) Color.LightGray else Color.White.copy(0.12f))

                    // Mapbox details
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("MAPBOX TILES INTEGRATION API:", fontSize = 9.sp, color = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        Text("Required for outdoor satellite topo overlays:", fontSize = 8.5.sp, color = Color.Gray, lineHeight = 12.sp)
                        
                        OutlinedTextField(
                            value = mapboxTokenInput,
                            onValueChange = { mapboxTokenInput = it },
                            label = { Text("Mapbox Private Key", fontSize = 8.5.sp, color = if (isOutdoorMode) Color.Black else Color.LightGray) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = if (isOutdoorMode) Color.Black else Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isOutdoorMode) Color.Black else Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.Gray.copy(0.4f),
                                focusedLabelColor = if (isOutdoorMode) Color.Black else Color(0xFF00E5FF)
                            ),
                            trailingIcon = {
                                IconButton(onClick = { mapboxTokenInput = "pk.eyJ1IjoiZmFyemFkZiIsImEiOiJjbHpiaXdwZ3cwMTF5MndzYzhvdzBoMHp1In0.6R0R9z84Vd-M208r_6oY7g" }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Default Token", tint = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                    
                    Divider(color = if (isOutdoorMode) Color.LightGray else Color.White.copy(0.12f))

                    // Simulated settings
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("SIMULATED CONSTELLATION:", fontSize = 9.sp, color = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Sats lock count: $satelliteCount", fontSize = 10.sp, color = if (isOutdoorMode) Color.Black else Color.White, modifier = Modifier.weight(1f))
                            Slider(
                                value = satelliteCount.toFloat(),
                                onValueChange = { satelliteCount = it.toInt() },
                                valueRange = 4f..24f,
                                modifier = Modifier.width(130.dp),
                                colors = SliderDefaults.colors(activeTrackColor = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF), thumbColor = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF))
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("GPS error radius: ±${String.format(Locale.US, "%.1f", gpsAccuracy)}m", fontSize = 10.sp, color = if (isOutdoorMode) Color.Black else Color.White, modifier = Modifier.weight(1f))
                            Slider(
                                value = gpsAccuracy.toFloat(),
                                onValueChange = { gpsAccuracy = it.toDouble() },
                                valueRange = 0.5f..12.0f,
                                modifier = Modifier.width(130.dp),
                                colors = SliderDefaults.colors(activeTrackColor = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF), thumbColor = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF))
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // DYNAMIC OVERLAY 4: FLOATING MAPPED TRACK JOURNAL JOURNAL PANEL
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = isJournalPanelVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .padding(top = 180.dp, end = 75.dp)
                .align(Alignment.TopEnd)
        ) {
            Card(
                modifier = Modifier.width(310.dp),
                colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color.White else Color(0xFC111513)),
                border = BorderStroke(1.5.dp, if (isOutdoorMode) Color.Black else Color(0xFF00E5FF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.List, contentDescription = null, tint = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                            Text("Saved Paths Journal", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isOutdoorMode) Color.Black else Color.White)
                        }
                        IconButton(onClick = { isJournalPanelVisible = false }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }

                    Divider(color = if (isOutdoorMode) Color.LightGray else Color.White.copy(0.12f))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (savedTracks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No saved field GPS paths. Tap the red record indicator widget to log walks.",
                                        fontSize = 10.sp,
                                        color = if (isOutdoorMode) Color.DarkGray else Color.LightGray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(savedTracks) { track ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color(0xFFEEEEEE) else Color(0xFF1E2521)),
                                    border = BorderStroke(0.5.dp, if (isOutdoorMode) Color.Gray else Color(0xFF00E5FF).copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(track.name, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = if (isOutdoorMode) Color(0xFF004D40) else Color(0xFF00E5FF))
                                            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(track.createdAt))
                                            Text(dateStr, fontSize = 8.sp, color = if (isOutdoorMode) Color.DarkGray else Color.Gray, fontFamily = FontFamily.Monospace)
                                        }
                                        
                                        Spacer(modifier = Modifier.height(2.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Len: ${track.lengthMeters.toInt()}m | Dur: ${track.durationSeconds / 60}m ${track.durationSeconds % 60}s", fontSize = 9.sp, color = if (isOutdoorMode) Color.Black else Color.LightGray)
                                            IconButton(
                                                onClick = { viewModel.deleteTrack(track.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    editTrackItem = track
                                                    editTrackItemName = track.name
                                                    showEditDialog = true
                                                },
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(10.dp), tint = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00FFE0))
                                                Spacer(Modifier.width(2.dp))
                                                Text("Rename", fontSize = 9.sp, color = if (isOutdoorMode) Color(0xFF004D40) else Color(0xFF00FFE0))
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            TextButton(
                                                onClick = {
                                                    val points = parsePointsJson(track.pointsJson)
                                                    val fileString = when (exportFormatChoice) {
                                                        "GPX" -> generateGpxString(track.name, points)
                                                        "KML" -> generateKmlString(track.name, points)
                                                        "GeoJSON" -> generateGeoJsonString(track.name, points)
                                                        else -> generateTxtString(track.name, points, track.lengthMeters, track.durationSeconds)
                                                    }
                                                    val extension = exportFormatChoice.lowercase(Locale.US)
                                                    val mime = if (exportFormatChoice == "GeoJSON") "application/json" else "application/xml"
                                                    shareFileWithApps(context, "${track.name}.$extension", mime, fileString)
                                                },
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(10.dp), tint = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00FFE0))
                                                Spacer(Modifier.width(2.dp))
                                                Text("Share ($exportFormatChoice)", fontSize = 9.sp, color = if (isOutdoorMode) Color(0xFF004D40) else Color(0xFF00FFE0))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Export Layout Format:", fontSize = 8.5.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("GPX", "KML", "GeoJSON", "TXT").forEach { format ->
                                FilterChip(
                                    selected = exportFormatChoice == format,
                                    onClick = { exportFormatChoice = format },
                                    label = { Text(format, fontSize = 8.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isOutdoorMode) Color(0xFF004D40) else Color(0xFF00E5FF),
                                        selectedLabelColor = if (isOutdoorMode) Color.White else Color.Black,
                                        containerColor = if (isOutdoorMode) Color(0xFFE0E0E0) else Color.White.copy(0.1f),
                                        labelColor = if (isOutdoorMode) Color.Black else Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // DYNAMIC OVERLAY 5: ADD GEOLOGICAL POINT QUICK-LOG DIALOG
        // -------------------------------------------------------------
        if (isAddPointDialogVisible) {
            AlertDialog(
                onDismissRequest = { isAddPointDialogVisible = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AddLocation, contentDescription = null, tint = Color(0xFF00E5FF))
                        Text("Simulate Quick-Log Station", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val activeProj = activeProject
                        if (activeProj == null) {
                            Text(
                                "No active geological project selected! Please select a project on 'Geological Mapping' tab first before logging.", 
                                fontSize = 11.sp, 
                                color = Color.Red
                            )
                        } else {
                            Text(
                                "Registers a new point instantly at the current sight target center coords in active project: '${activeProj.name}'",
                                fontSize = 10.5.sp,
                                color = Color.White
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                                border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(0.3f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Sight target: ${String.format(Locale.US, "%.5f", currentLat)}°N , ${String.format(Locale.US, "%.5f", currentLon)}°E", fontSize = 10.sp, color = Color(0xFF00FFE0), fontFamily = FontFamily.Monospace)
                                    val utmResultCoord = UtmConverter.convertLatLngToUtm(currentLat, currentLon)
                                    Text("UTM Zone: $utmResultCoord", fontSize = 9.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = quickStationPrefix,
                                    onValueChange = { quickStationPrefix = it },
                                    label = { Text("Prefix", fontSize = 9.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E5FF))
                                )
                                OutlinedTextField(
                                    value = quickStationCode,
                                    onValueChange = { quickStationCode = it },
                                    label = { Text("Station Code", fontSize = 9.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(2f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E5FF))
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    val activeProj = activeProject
                    if (activeProj != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Quick Save
                            Button(
                                onClick = {
                                    viewModel.setManualCoordinates(currentLat, currentLon, 1210.0, gpsAccuracy)
                                    viewModel.saveStation(activeProj.id, quickStationCode, quickStationPrefix) {
                                        isAddPointDialogVisible = false
                                        Toast.makeText(context, "Logged $quickStationCode successfully at target position!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                            ) {
                                Text("Quick Save")
                            }

                            // Full Registration Navigation
                            Button(
                                onClick = {
                                    viewModel.setManualCoordinates(currentLat, currentLon, 1210.0, gpsAccuracy)
                                    viewModel.navigateTo(Screen.CreateStation(activeProj.id))
                                    isAddPointDialogVisible = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF), contentColor = Color.White)
                            ) {
                                Text("Full Details Form")
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isAddPointDialogVisible = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF161B18),
                textContentColor = Color.White,
                titleContentColor = Color.White
            )
        }

        // -------------------------------------------------------------
        // STANDARD TRACK SAVE AND TRACK NAME EDIT DIALOGS
        // -------------------------------------------------------------
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("End of Recording: Save Track Path", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("The GPS field path was logged successfully. Specify a tag to save in database journal:", fontSize = 11.sp)
                        
                        Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.3f))) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Estimated Length: ${tempTrackLength.toInt()} meters", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                                Text("Duration: ${tempTrackDuration / 60}m ${tempTrackDuration % 60}s", fontSize = 10.1.sp)
                                Text("Captured Track Points: ${activePointsList.size} positions", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        OutlinedTextField(
                            value = newTrackName,
                            onValueChange = { newTrackName = it },
                            label = { Text("Track Name", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E5FF))
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveTrack(newTrackName, tempTrackPointsJson, tempTrackLength, tempTrackDuration)
                            showSaveDialog = false
                            Toast.makeText(context, "Track logged successfully to journal as $newTrackName", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                    ) {
                        Text("Save to Database")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = Color(0xFF161B18),
                textContentColor = Color.White,
                titleContentColor = Color.White
            )
        }

        if (showEditDialog && editTrackItem != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Rename Survey Path", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = editTrackItemName,
                        onValueChange = { editTrackItemName = it },
                        label = { Text("New Track Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E5FF))
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateTrackName(editTrackItem!!.id, editTrackItemName)
                            showEditDialog = false
                            Toast.makeText(context, "Track renamed successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                    ) {
                        Text("Save Name")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = Color(0xFF161B18),
                textContentColor = Color.White,
                titleContentColor = Color.White
            )
        }
    }
}

// -------------------------------------------------------------------------
// 5. GPS EXPORT SHAPE TRANSLATORS HELPER FUNCTIONS
// -------------------------------------------------------------------------
fun generateGpxString(name: String, points: List<TrackPoint>): String {
    val sb = java.lang.StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    sb.append("<gpx version=\"1.1\" creator=\"GeoField Platform\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
    sb.append("  <metadata>\n")
    sb.append("    <name>$name</name>\n")
    sb.append("    <desc>Geotechnical Field Mapping Route</desc>\n")
    sb.append("  </metadata>\n")
    sb.append("  <trk>\n")
    sb.append("    <name>$name</name>\n")
    sb.append("    <trkseg>\n")
    points.forEach { pt ->
        val isoTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(pt.time))
        sb.append("      <trkpt lat=\"${pt.lat}\" lon=\"${pt.lon}\">\n")
        sb.append("        <ele>${pt.ele}</ele>\n")
        sb.append("        <time>$isoTime</time>\n")
        sb.append("      </trkpt>\n")
    }
    sb.append("    </trkseg>\n")
    sb.append("  </trk>\n")
    sb.append("</gpx>\n")
    return sb.toString()
}

fun generateKmlString(name: String, points: List<TrackPoint>): String {
    val sb = java.lang.StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
    sb.append("  <Document>\n")
    sb.append("    <name>$name</name>\n")
    sb.append("    <description>Geofield GIS Track Path</description>\n")
    sb.append("    <Style id=\"redLinePoly\">\n")
    sb.append("      <LineStyle>\n")
    sb.append("        <color>ff0000ff</color>\n") // red
    sb.append("        <width>4</width>\n")
    sb.append("      </LineStyle>\n")
    sb.append("    </Style>\n")
    sb.append("    <Placemark>\n")
    sb.append("      <name>Recorded Route Path</name>\n")
    sb.append("      <styleUrl>#redLinePoly</styleUrl>\n")
    sb.append("      <LineString>\n")
    sb.append("        <coordinates>\n")
    points.forEach { pt ->
        sb.append("          ${pt.lon},${pt.lat},${pt.ele}\n")
    }
    sb.append("        </coordinates>\n")
    sb.append("      </LineString>\n")
    sb.append("    </Placemark>\n")
    sb.append("  </Document>\n")
    sb.append("</kml>\n")
    return sb.toString()
}

fun generateGeoJsonString(name: String, points: List<TrackPoint>): String {
    val sb = java.lang.StringBuilder()
    sb.append("{\n")
    sb.append("  \"type\": \"FeatureCollection\",\n")
    sb.append("  \"features\": [\n")
    sb.append("    {\n")
    sb.append("      \"type\": \"Feature\",\n")
    sb.append("      \"properties\": {\n")
    sb.append("        \"name\": \"$name\",\n")
    sb.append("        \"type\": \"GPS_Track_Log\"\n")
    sb.append("      },\n")
    sb.append("      \"geometry\": {\n")
    sb.append("        \"type\": \"LineString\",\n")
    sb.append("        \"coordinates\": [\n")
    points.forEachIndexed { i, pt ->
        sb.append("          [${pt.lon}, ${pt.lat}, ${pt.ele}]")
        if (i < points.size - 1) sb.append(",")
        sb.append("\n")
    }
    sb.append("        ]\n")
    sb.append("      }\n")
    sb.append("    }\n")
    sb.append("  ]\n")
    sb.append("}\n")
    return sb.toString()
}

fun generateTxtString(name: String, points: List<TrackPoint>, length: Double, duration: Long): String {
    val sb = java.lang.StringBuilder()
    sb.append("=========================================\n")
    sb.append("GEOFIELD FIELD TRACK LOG REPORT SUMMARY  \n")
    sb.append("=========================================\n")
    sb.append("Track Name: $name\n")
    sb.append("Total Distance Estimate: ${length.toInt()} meters\n")
    sb.append("Recording duration time: ${duration / 60} minutes, ${duration % 60} seconds\n")
    sb.append("Recorded GPS Coordinates point captures: ${points.size}\n\n")
    sb.append("Point Coordinates Matrix List:\n")
    points.forEachIndexed { idx, pt ->
        val tm = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(pt.time))
        sb.append("${idx + 1}. Lat: ${pt.lat}, Lon: ${pt.lon}, Alt: ${pt.ele}m, Heading: ${pt.heading}deg, Time: $tm\n")
    }
    return sb.toString()
}
