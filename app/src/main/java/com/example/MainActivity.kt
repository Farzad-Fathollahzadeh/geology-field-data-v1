package com.example

import android.Manifest
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Canvas
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.views.MapView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.result.launch
import androidx.compose.foundation.horizontalScroll
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context

class MainActivity : ComponentActivity() {
    private val viewModel: GeologyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeSelection by viewModel.themeSelection.collectAsStateWithLifecycle()
            val fontSizeMult by viewModel.fontSizeMultiplier.collectAsStateWithLifecycle()
            val fontFamilySel by viewModel.fontFamilySelection.collectAsStateWithLifecycle()
            val touchSizingProfile by viewModel.touchSizingProfile.collectAsStateWithLifecycle()

            val isSystemDark = isSystemInDarkTheme()
            val darkThemeSetup = when (themeSelection) {
                "DARK" -> true
                "LIGHT" -> false
                "OUTDOOR" -> false
                else -> isSystemDark
            }
            
            MyApplicationTheme(
                themeSelection = themeSelection,
                dynamicColor = false,
                fontSizeMultiplier = fontSizeMult,
                fontFamilySel = fontFamilySel,
                touchSizingProfile = touchSizingProfile
            ) {
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
                        onToggleTheme = {
                            val newTheme = if (themeSelection == "DARK") "LIGHT" else if (themeSelection == "LIGHT") "OUTDOOR" else "DARK"
                            viewModel.saveThemeSelection(newTheme)
                        }
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
    val detailedLabel: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val description: String
)

fun getGeologyTypeInfo(type: String): GeologyTypeInfo {
    return when (type.lowercase(Locale.ROOT).trim()) {
        "lithology" -> GeologyTypeInfo(
            name = "Lithology",
            detailedLabel = "Lithology & Petrology",
            icon = Icons.Default.Terrain,
            color = Color(0xFF8D6E63), // Clay/Slate Brown (Earthy rock)
            description = "Rock type, texture, color, and grain size classification"
        )
        "alteration" -> GeologyTypeInfo(
            name = "Alteration",
            detailedLabel = "Hydrothermal Alteration",
            icon = Icons.Default.Waves, // Wavey alteration patterns
            color = Color(0xFFEF6C00), // Hydrothermal terracotta orange
            description = "Hydrothermal alteration mineral assemblages and intensity scales"
        )
        "fault" -> GeologyTypeInfo(
            name = "Fault",
            detailedLabel = "Fault Zone Displacements",
            icon = Icons.Default.CompareArrows, // Shear sliding arrows
            color = Color(0xFFD32F2F), // Tension fracture active red
            description = "Fault strike, dip, slip-sense, shear thickness, and structures"
        )
        "joint" -> GeologyTypeInfo(
            name = "Joint",
            detailedLabel = "Mechanical Fractures & Joints",
            icon = Icons.Default.GridOn, // Fracture joint grid pattern
            color = Color(0xFF455A64), // Basalt slate dark blue-grey
            description = "Joint set occurrences, spacing distributions, and fracture density"
        )
        "vein" -> GeologyTypeInfo(
            name = "Vein",
            detailedLabel = "Mineralized Vein Systems",
            icon = Icons.Default.Bolt, // High-energy mineral veins
            color = Color(0xFFFFB300), // Quartz/Gold Vein Yellow
            description = "Vein fill compositions, structural orientation, and true thickness"
        )
        "structure" -> GeologyTypeInfo(
            name = "Structure",
            detailedLabel = "Mesoscopic Structural Data",
            icon = Icons.Default.Menu, // Planar layering horizontal lines
            color = Color(0xFF00796B), // Foliated Metamorphic Teal
            description = "Strike/dip of planar structures, bedding lines, and fold hinges"
        )
        "mineralization" -> GeologyTypeInfo(
            name = "Mineralization",
            detailedLabel = "Ore Mineralization",
            icon = Icons.Default.Diamond, // Precious ore minerals
            color = Color(0xFFFF8F00), // Bronze/gold metallic kani
            description = "Metal ore mineral occurrences, distribution styles, and grade estimations"
        )
        else -> GeologyTypeInfo(
            name = type,
            detailedLabel = type,
            icon = Icons.Default.Layers,
            color = Color(0xFF78909C),
            description = "In-situ field raw geological documentation coordinates"
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
    
    val boreholes by viewModel.boreholes.collectAsStateWithLifecycle()
    val activeBorehole by viewModel.activeBorehole.collectAsStateWithLifecycle()
    val activeBoreholeRuns by viewModel.activeBoreholeRuns.collectAsStateWithLifecycle()
    val activeBoreholeDailyLogs by viewModel.activeBoreholeDailyLogs.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(450) // Crisp, lightning-fast boot sequence for optimal user response
        showSplash = false
    }

    if (showSplash) {
        GeoFieldSplashScreen()
    } else {
        // System Back Button Navigation System (Disallowed on Home, handles Mapping or Logging)
        BackHandler(enabled = currentScreen != Screen.Home) {
            when (val screen = currentScreen) {
                is Screen.ProjectList -> viewModel.navigateTo(Screen.Home)
                is Screen.CreateProject -> viewModel.navigateTo(Screen.ProjectList)
                is Screen.ProjectDashboard -> viewModel.navigateTo(Screen.ProjectList)
                is Screen.CreateStation -> viewModel.navigateTo(Screen.ProjectDashboard(activeProject?.id ?: 0))
                is Screen.StationDetail -> viewModel.navigateTo(Screen.ProjectDashboard(activeProject?.id ?: 0))
                is Screen.AddObservation -> viewModel.navigateTo(Screen.StationDetail(activeStation?.id ?: 0))
                
                is Screen.BoreholeList -> viewModel.navigateTo(Screen.Home)
                is Screen.CreateBorehole -> viewModel.navigateTo(Screen.BoreholeList)
                is Screen.BoreholeDashboard -> viewModel.navigateTo(Screen.BoreholeList)
                is Screen.CreateDailyLog -> viewModel.navigateTo(Screen.BoreholeDashboard(screen.boreholeId))
                is Screen.CreateCoreRun -> viewModel.navigateTo(Screen.BoreholeDashboard(screen.boreholeId))
                is Screen.RockEngineering -> viewModel.navigateTo(Screen.Home)
                is Screen.Settings -> viewModel.navigateTo(Screen.Home)
                is Screen.StationOfflineMap -> {
                    val activeProjId = activeProject?.id
                    if (activeProjId != null) {
                        viewModel.navigateTo(Screen.ProjectDashboard(activeProjId))
                    } else {
                        viewModel.navigateTo(Screen.Home)
                    }
                }
                else -> viewModel.navigateTo(Screen.Home)
            }
        }

        val title = when (val screen = currentScreen) {
            is Screen.Home -> "GeoField Pro"
            is Screen.ProjectList -> "Geology Mapping"
            is Screen.CreateProject -> "New Field Project"
            is Screen.ProjectDashboard -> activeProject?.name ?: "Project Dashboard"
            is Screen.CreateStation -> "Add Station"
            is Screen.StationDetail -> activeStation?.stationCode ?: "Station Details"
            is Screen.AddObservation -> "Log Field Observation"
            
            is Screen.BoreholeList -> "Geology Core Logging"
            is Screen.CreateBorehole -> "New Borehole Collar"
            is Screen.BoreholeDashboard -> activeBorehole?.name ?: "Borehole Logging Dashboard"
            is Screen.CreateDailyLog -> "Daily Drilling Progress"
            is Screen.CreateCoreRun -> if (screen.runId != null) "Edit Core Run" else "Log Core Interval Run"
            is Screen.RockEngineering -> "Rock Engineering Utilities"
            is Screen.Settings -> "App Settings"
            is Screen.StationOfflineMap -> "Offline Stations Map"
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
                            if (currentScreen != Screen.Home) {
                                IconButton(
                                    onClick = {
                                        when (val screen = currentScreen) {
                                            is Screen.ProjectList -> viewModel.navigateTo(Screen.Home)
                                            is Screen.CreateProject -> viewModel.navigateTo(Screen.ProjectList)
                                            is Screen.ProjectDashboard -> viewModel.navigateTo(Screen.ProjectList)
                                            is Screen.CreateStation -> viewModel.navigateTo(Screen.ProjectDashboard(activeProject?.id ?: 0))
                                            is Screen.StationDetail -> viewModel.navigateTo(Screen.ProjectDashboard(activeProject?.id ?: 0))
                                            is Screen.AddObservation -> viewModel.navigateTo(Screen.StationDetail(activeStation?.id ?: 0))
                                            
                                            is Screen.BoreholeList -> viewModel.navigateTo(Screen.Home)
                                            is Screen.CreateBorehole -> viewModel.navigateTo(Screen.BoreholeList)
                                            is Screen.BoreholeDashboard -> viewModel.navigateTo(Screen.BoreholeList)
                                            is Screen.CreateDailyLog -> viewModel.navigateTo(Screen.BoreholeDashboard(screen.boreholeId))
                                            is Screen.CreateCoreRun -> viewModel.navigateTo(Screen.BoreholeDashboard(screen.boreholeId))
                                            is Screen.RockEngineering -> viewModel.navigateTo(Screen.Home)
                                            is Screen.Settings -> viewModel.navigateTo(Screen.Home)
                                            is Screen.StationOfflineMap -> {
                                                val activeProjId = activeProject?.id
                                                if (activeProjId != null) {
                                                    viewModel.navigateTo(Screen.ProjectDashboard(activeProjId))
                                                } else {
                                                    viewModel.navigateTo(Screen.Home)
                                                }
                                            }
                                            else -> viewModel.navigateTo(Screen.Home)
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
                            IconButton(
                                onClick = { viewModel.navigateTo(Screen.Settings) },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "App Settings"
                                )
                            }
                            if (currentScreen is Screen.ProjectDashboard) {
                                val activeProjId = activeProject?.id
                                if (activeProjId != null) {
                                    IconButton(
                                        onClick = { showExportDialog = true },
                                        modifier = Modifier.testTag("export_button")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Export GIS Datasets")
                                    }
                                }
                            }
                            if (currentScreen is Screen.BoreholeList || currentScreen is Screen.BoreholeDashboard) {
                                var showBoreholeExportDialog by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { showBoreholeExportDialog = true },
                                    modifier = Modifier.testTag("borehole_export_button")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Export Logging Datasets")
                                }
                                if (showBoreholeExportDialog) {
                                    BoreholeExportDialog(
                                        onDismiss = { showBoreholeExportDialog = false },
                                        onExportExcel = {
                                            viewModel.exportBoreholeData(context)
                                            showBoreholeExportDialog = false
                                        },
                                        onExportZip = {
                                            viewModel.exportBoreholeZip(context)
                                            showBoreholeExportDialog = false
                                        }
                                    )
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
                        is Screen.Home -> {
                            HomeScreen(
                                onSelectMapping = {
                                    viewModel.navigateTo(Screen.ProjectList)
                                },
                                onSelectLogging = {
                                    viewModel.navigateTo(Screen.BoreholeList)
                                },
                                onSelectRockEngineering = {
                                    viewModel.navigateTo(Screen.RockEngineering)
                                },
                                onSelectMapTracker = {
                                    viewModel.navigateTo(Screen.StationOfflineMap)
                                },
                                projectsCount = projectList.size,
                                boreholesCount = boreholes.size
                            )
                        }
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
                                },
                                onUpdateStation = {
                                    viewModel.updateStation(it)
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
                                        dipDirection = details["dipDirection"] as? Int,
                                        rake = details["rake"] as? Int,
                                        rakeDirection = details["rakeDirection"] as? String,
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
                        is Screen.BoreholeList -> {
                            BoreholeListScreen(
                                boreholes = boreholes,
                                onSelectBorehole = {
                                    viewModel.navigateTo(Screen.BoreholeDashboard(it.id))
                                },
                                onCreateClick = {
                                    viewModel.navigateTo(Screen.CreateBorehole)
                                },
                                onDeleteClick = {
                                    viewModel.deleteBorehole(it.id)
                                }
                            )
                        }
                        is Screen.CreateBorehole -> {
                            CreateBoreholeScreen(
                                viewModel = viewModel,
                                onSave = { name, lat, lon, elev, acc, elevAcc, east, north, zone, date, az ->
                                    val formattedDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(date))
                                    viewModel.createBorehole(name, lat, lon, elev, acc, elevAcc, east, north, zone, formattedDate, az)
                                },
                                onCancel = {
                                    viewModel.navigateTo(Screen.BoreholeList)
                                }
                            )
                        }
                        is Screen.BoreholeDashboard -> {
                            BoreholeDashboardScreen(
                                borehole = activeBorehole,
                                runs = activeBoreholeRuns,
                                dailyLogs = activeBoreholeDailyLogs,
                                onAddRun = {
                                    viewModel.navigateTo(Screen.CreateCoreRun(activeBorehole?.id ?: screen.boreholeId, null))
                                },
                                onEditRun = { runId ->
                                    viewModel.navigateTo(Screen.CreateCoreRun(activeBorehole?.id ?: screen.boreholeId, runId))
                                },
                                onDeleteRun = { runId ->
                                    viewModel.deleteCoreRun(runId, activeBorehole?.id ?: screen.boreholeId)
                                },
                                onAddDailyLog = {
                                    viewModel.navigateTo(Screen.CreateDailyLog(activeBorehole?.id ?: screen.boreholeId))
                                },
                                onDeleteDailyLog = { logId ->
                                    viewModel.deleteDailyLog(logId, activeBorehole?.id ?: screen.boreholeId)
                                },
                                onExportExcel = {
                                    viewModel.exportBoreholeData(context)
                                },
                                onExportZip = {
                                    viewModel.exportBoreholeZip(context)
                                },
                                onUpdateBorehole = { updatedBh ->
                                    viewModel.updateBorehole(updatedBh)
                                }
                            )
                        }
                        is Screen.CreateDailyLog -> {
                            CreateDailyLogScreen(
                                boreholeId = screen.boreholeId,
                                onSave = { logId, bhId, date, depth, summary, ops, comments ->
                                    viewModel.saveDailyLog(logId, bhId, date, depth, summary, ops, comments)
                                },
                                onCancel = {
                                    viewModel.navigateTo(Screen.BoreholeDashboard(screen.boreholeId))
                                }
                            )
                        }
                        is Screen.CreateCoreRun -> {
                            CreateCoreRunScreen(
                                viewModel = viewModel,
                                boreholeId = screen.boreholeId,
                                runId = screen.runId,
                                onSave = { runId, bhId, fromDepth, toDepth, pieces, rec, rock, desc, alt, altInt, min, minInt, frac, vein, joints, color, texture, size, commentsVal ->
                                    viewModel.saveCoreRun(runId, bhId, fromDepth, toDepth, pieces, rec, rock, desc, alt, altInt, min, minInt, frac, vein, joints, color, texture, size, commentsVal)
                                },
                                onCancel = {
                                    viewModel.navigateTo(Screen.BoreholeDashboard(screen.boreholeId))
                                }
                            )
                        }
                        is Screen.RockEngineering -> {
                            RockEngineeringScreen(
                                viewModel = viewModel,
                                onBackClick = {
                                    viewModel.navigateTo(Screen.Home)
                                }
                            )
                        }
                        is Screen.Settings -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = {
                                    viewModel.navigateTo(Screen.Home)
                                }
                            )
                        }
                        is Screen.StationOfflineMap -> {
                            StationOfflineMapScreen(
                                viewModel = viewModel,
                                onBackClick = {
                                    val activeProjId = activeProject?.id
                                    if (activeProjId != null) {
                                        viewModel.navigateTo(Screen.ProjectDashboard(activeProjId))
                                    } else {
                                        viewModel.navigateTo(Screen.Home)
                                    }
                                }
                            )
                        }
                    }
                }
            }
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
            .background(Color(0xFF0F171C)),
        contentAlignment = Alignment.Center
    ) {
        // Delicate translucent structural stereonet grid on the background
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.12f)
        ) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            val maxR = size.width.coerceAtMost(size.height) * 0.45f

            // Outer dial
            drawCircle(Color.LightGray, radius = maxR, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))

            // Concentric polar grid rings
            for (i in 1..4) {
                drawCircle(Color.LightGray, radius = maxR * (i.toFloat() / 5f), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            }

            // Mapped geological strike arcs
            for (pct in listOf(-0.7f, -0.4f, 0.4f, 0.7f)) {
                val offsetDist = maxR * pct
                drawCircle(
                    color = Color.LightGray,
                    radius = maxR,
                    center = androidx.compose.ui.geometry.Offset(center.x + offsetDist, center.y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
                )
            }

            // Structural axis lines
            drawLine(Color.LightGray, androidx.compose.ui.geometry.Offset(center.x - maxR, center.y), androidx.compose.ui.geometry.Offset(center.x + maxR, center.y), strokeWidth = 1f)
            drawLine(Color.LightGray, androidx.compose.ui.geometry.Offset(center.x, center.y - maxR), androidx.compose.ui.geometry.Offset(center.x, center.y + maxR), strokeWidth = 1f)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Strata shield emblem
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF19252D))
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Strata bands
                    Box(modifier = Modifier.size(50.dp, 8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFE67E22)))
                    Box(modifier = Modifier.size(50.dp, 8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFF1C40F)))
                    Row(
                        modifier = Modifier.size(50.dp, 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(36.dp, 8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF2ECC71)))
                        // Mini compass strike needle decoration
                        Text("▲", fontSize = 10.sp, color = Color.White, modifier = Modifier.offset(y = (-2).dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "GeoField Pro",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "INTELLIGENT FIELD GEOLOGY PLATFORM",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

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
                text = "OFFLINE-FIRST FIELD DATA LOGGER • v2.1.1",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/geofieldpro"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Telegram Link",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Telegram Community: t.me/geofieldpro",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0A66C2).copy(alpha = 0.08f))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/farzad-fathollahzadeh-b3a6a1165"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "LinkedIn Link",
                            tint = Color(0xFF0A66C2),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Developer's LinkedIn: Farzad Fathollahzadeh",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A66C2)
                        )
                    }
                }
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
                        text = "v1.1",
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
                    placeholder = { Text("Search projects...") },
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
                                text = "No results found",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No projects match your search query.",
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
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/geofieldpro"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Telegram Link",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Telegram Community: t.me/geofieldpro",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0A66C2).copy(alpha = 0.08f))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/farzad-fathollahzadeh-b3a6a1165"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "LinkedIn Link",
                            tint = Color(0xFF0A66C2),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Developer's LinkedIn: Farzad Fathollahzadeh",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A66C2)
                        )
                    }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAddStationClick,
                modifier = Modifier
                    .weight(1.3f)
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
                Text("ADD STATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }

            Button(
                onClick = { viewModel.navigateTo(Screen.StationOfflineMap) },
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .testTag("offline_map_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC96A2B),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("OFFLINE MAP", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
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
    val stations by viewModel.activeProjectStations.collectAsStateWithLifecycle()
    
    val prefixes = listOf("LITH", "ALT", "FLT", "STR", "SMP")
    var selectedPrefix by remember { mutableStateOf("LITH") }
    
    var stationCodeInput by remember { mutableStateOf("") }
    var isManuallyEdited by remember { mutableStateOf(false) }

    // Sequential code auto-generation engine (10-year experienced professional pattern)
    val autoSuggestedCode = remember(stations, selectedPrefix) {
        val lastStation = stations.lastOrNull()
        if (lastStation != null) {
            val lastCode = lastStation.stationCode
            val regex = Regex("^(.*?)([0-9]+)$")
            val matchResult = regex.matchEntire(lastCode)
            if (matchResult != null) {
                val prefixPart = matchResult.groups[1]?.value ?: ""
                val suffixStr = matchResult.groups[2]?.value ?: ""
                val suffixInt = suffixStr.toIntOrNull() ?: 0
                val nextSuffixInt = suffixInt + 1
                val formattedSuffix = String.format(Locale.US, "%0${suffixStr.length}d", nextSuffixInt)
                
                // If the last station has a standard prefix (e.g. LITH, ALT), replace with selectedPrefix if it differs
                val oldPrefixes = listOf("LITH", "ALT", "FLT", "STR", "SMP")
                var newPrefixPart = prefixPart
                for (p in oldPrefixes) {
                    if (prefixPart.trim('-').startsWith(p)) {
                        newPrefixPart = prefixPart.replaceFirst(p, selectedPrefix)
                        break
                    }
                }
                newPrefixPart + formattedSuffix
            } else {
                "$lastCode-01"
            }
        } else {
            "$selectedPrefix-01"
        }
    }

    // Update the input field with auto suggested code unless manually edited by user
    LaunchedEffect(autoSuggestedCode) {
        if (!isManuallyEdited) {
            stationCodeInput = autoSuggestedCode
        }
    }
    
    // GPS request logic
    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    )

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
            text = "Create Structural Station",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // STATION CODE / NAME EDITABLE CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "STATION CODE / NAME (SEQUENTIAL AUTO-FILL)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = stationCodeInput,
                    onValueChange = {
                        stationCodeInput = it
                        isManuallyEdited = true
                    },
                    label = { Text("Station identifier / code") },
                    modifier = Modifier.fillMaxWidth().testTag("station_code_input_field"),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (isManuallyEdited) {
                            IconButton(
                                onClick = {
                                    isManuallyEdited = false
                                    stationCodeInput = autoSuggestedCode
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Restore sequential auto-suggested code",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sourced sequentially",
                                tint = Color(0xFF00A86B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
                
                if (isManuallyEdited) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Auto-sequence paused (edited manually). Click the sparkles icon above to restore sequence.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

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
                    "LITH" to "Lithology Observation (LITH) - Rock Type",
                    "ALT" to "Alteration Mapping (ALT) - Mineral Assembly",
                    "FLT" to "Fault Survey (FLT) - Structure Slip",
                    "STR" to "Structural Logging (STR) - Bedding Profile",
                    "SMP" to "Physical Specimen Sample (SMP) - Ore Hand Specimen"
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
                    if (gps.isCaptured && stationCodeInput.isNotBlank()) {
                        viewModel.saveStation(projectId, stationCodeInput, selectedPrefix, onSaved)
                    }
                },
                enabled = gps.isCaptured && stationCodeInput.isNotBlank(),
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
    onDeleteObservation: (Int) -> Unit,
    onUpdateStation: (Station) -> Unit = {}
) {
    if (station == null) return

    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        var editCode by remember { mutableStateOf(station.stationCode) }
        var editLat by remember { mutableStateOf(station.latitude.toString()) }
        var editLng by remember { mutableStateOf(station.longitude.toString()) }
        var editElev by remember { mutableStateOf(station.elevation.toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    "Edit Station Parameters",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Modify structural station metadata below. Latitude/longitude changes will automatically recalculate UTM coordinate projections.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = editCode,
                        onValueChange = { editCode = it },
                        label = { Text("Station Code / Name") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_station_code_field"),
                        singleLine = true,
                        textStyle = TextStyle(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = editLat,
                        onValueChange = { editLat = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_station_lat_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = editLng,
                        onValueChange = { editLng = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_station_lng_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = editElev,
                        onValueChange = { editElev = it },
                        label = { Text("Elevation (m)") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_station_elev_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val latVal = editLat.toDoubleOrNull() ?: station.latitude
                        val lngVal = editLng.toDoubleOrNull() ?: station.longitude
                        val elevVal = editElev.toDoubleOrNull() ?: station.elevation

                        // Auto recalculate UTM using geology converter helper (10-year senior dev pattern)
                        val utm = UtmConverter.convertLatLngToUtm(latVal, lngVal, if (station.utmZone.isNotBlank()) station.utmZone else null)

                        val updated = station.copy(
                            stationCode = editCode,
                            latitude = latVal,
                            longitude = lngVal,
                            elevation = elevVal,
                            utmEasting = utm.easting,
                            utmNorthing = utm.northing,
                            utmZone = utm.zoneString
                        )
                        onUpdateStation(updated)
                        showEditDialog = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                            onClick = { showEditDialog = true },
                            modifier = Modifier.size(24.dp).testTag("edit_station_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Station Parameters",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
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
                                            text = info.detailedLabel,
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

// ==========================================
// SMART GEOLOGICAL AUTO-COMPLETE DICTIONARY FIELD
// ==========================================
@Composable
fun SmartAutoCompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    // Smart filtering: match suggestions containing typed text (ignoring case)
    val filteredSuggestions = remember(value, suggestions) {
        if (value.isBlank()) {
            suggestions.take(4) // Show standard defaults when blank
        } else {
            suggestions.filter {
                it.contains(value, ignoreCase = true) && !it.equals(value, ignoreCase = true)
            }.take(5)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder) } } else null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .onFocusChanged { isFocused = it.isFocused },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (isFocused && filteredSuggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                items(filteredSuggestions) { suggestion ->
                    AssistChip(
                        onClick = { onValueChange(suggestion) },
                        label = {
                            Text(
                                text = suggestion,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
            }
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

    // Smart Geological Auto-Complete Dictionaries
    val lithologyDict = listOf("Granite", "Basalt", "Sandstone", "Shale", "Schist", "Gneiss", "Diorite", "Andesite", "Rhyolite", "Gabbro", "Limestone", "Quartzite", "Marble", "Claystone", "Tuff", "Pegmatite", "Siltstone", "Breccia", "Conglomerate", "Granodiorite", "Dacite", "Amphibolite", "Phyllite", "Slate", "Serpentinite")
    val textureDict = listOf("Porphyritic", "Foliated", "Gneissic", "Aphanitic", "Phaneritic", "Glassy", "Vesicular", "Fragmental", "Pyroclastic", "Pegmatitic", "Granoblastic", "Schistose", "Massive", "Banded", "Crystalline", "Clastic", "Oolitic", "Brecciated", "Mylonitic")
    val alterationDict = listOf("Potassic", "Propylitic", "Argillic", "Sericitic", "Silicification", "Weak Chlorite", "Epidote-chlorite", "Carbonatization", "Albitization", "Pyritization", "Hematitization", "Sodic", "Phyllic", "Advanced Argillic")
    val mineralDict = listOf("Pyrite", "Chalcopyrite", "Quartz", "Feldspar", "Mica", "Biotite", "Muscovite", "Hornblende", "Olivine", "Pyroxene", "Calcite", "Dolomite", "Galena", "Sphalerite", "Bornite", "Magnetite", "Hematite", "Chrysocolla", "Malachite", "Azurite", "Sulfide")
    val colorDict = listOf("Dark Grey", "Light Grey", "Reddish Brown", "Pinkish Grey", "Greenish", "Buff", "Tan", "White", "Black", "Yellowish Brown", "Olive Green")
    
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
    var rake by remember { mutableStateOf("") }
    var rakeDirection by remember { mutableStateOf("") }
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

    // Compass sensor live tracking
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val rotationVectorSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : android.hardware.SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationValues = FloatArray(3)

            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == android.hardware.Sensor.TYPE_ROTATION_VECTOR) {
                    android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    android.hardware.SensorManager.getOrientation(rotationMatrix, orientationValues)
                    val yawRadians = orientationValues[0]
                    var yawDegrees = Math.toDegrees(yawRadians.toDouble()).toInt()
                    if (yawDegrees < 0) {
                        yawDegrees += 360
                    }
                    sightAzimuth = yawDegrees
                }
            }

            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, android.hardware.SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
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
                timestamp = System.currentTimeMillis(),
                settings = PhotoProcessor.OverlaySettings(isOverlayEnabled, overlayPosition),
                isCapturedInApp = true
            )
            if (path != null) {
                attachedPhotoPathsList = attachedPhotoPathsList + path
            }
        }
    }

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
                                settings = PhotoProcessor.OverlaySettings(isOverlayEnabled, overlayPosition),
                                isCapturedInApp = false
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
                                        settings = PhotoProcessor.OverlaySettings(isOverlayEnabled, overlayPosition),
                                        isCapturedInApp = false
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
                            text = "${obsType.uppercase(Locale.getDefault())} (${currentTypeInfo.detailedLabel})",
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

                SmartAutoCompleteTextField(
                    value = lithType,
                    onValueChange = { lithType = it },
                    label = "Rock / Lithology Type",
                    placeholder = "e.g. Granodiorite, Amphibolite",
                    suggestions = lithologyDict,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "lithology_type"
                )

                SmartAutoCompleteTextField(
                    value = texture,
                    onValueChange = { texture = it },
                    label = "Texture / Fabric",
                    placeholder = "e.g. Porphyritic, Foliated, Gneissic",
                    suggestions = textureDict,
                    modifier = Modifier.fillMaxWidth()
                )

                SmartAutoCompleteTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = "Color",
                    placeholder = "e.g. Grey-pink, Dark green",
                    suggestions = colorDict,
                    modifier = Modifier.fillMaxWidth()
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

                SmartAutoCompleteTextField(
                    value = lithAlt,
                    onValueChange = { lithAlt = it },
                    label = "Alteration Style",
                    placeholder = "e.g. Potassic, Propylitic, Weak Chlorite",
                    suggestions = alterationDict,
                    modifier = Modifier.fillMaxWidth()
                )

                SmartAutoCompleteTextField(
                    value = lithMin,
                    onValueChange = { lithMin = it },
                    label = "Mineralization Index",
                    placeholder = "e.g. 2% Pyrite cubes, trace Chalcopyrite",
                    suggestions = mineralDict,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Alteration" -> {
                Text("ALTERATION ZONE FORM", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, letterSpacing = 1.sp)

                SmartAutoCompleteTextField(
                    value = altType,
                    onValueChange = { altType = it },
                    label = "Alteration Assembly",
                    placeholder = "e.g. Quartz-Sericite-Pyrite (Phyllic)",
                    suggestions = alterationDict,
                    modifier = Modifier.fillMaxWidth(),
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

                    OutlinedTextField(
                        value = dipDirection,
                        onValueChange = { dipDirection = it },
                        label = { Text("Dip Dir (0-360°)") },
                        placeholder = { Text("e.g. 135") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dip_direction_input"),
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
                        value = rake,
                        onValueChange = { rake = it },
                        label = { Text("Rake / Pitch (0-90°)") },
                        placeholder = { Text("e.g. 35") },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("rake_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = rakeDirection,
                        onValueChange = { rakeDirection = it },
                        label = { Text("Rake Dir") },
                        placeholder = { Text("e.g. NW, SE") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rake_dir_input"),
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
                    onClick = { cameraLauncher.launch() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC96A2B)
                    )
                ) {
                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take Photo with In-App Camera", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { pickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Photo from Device Storage", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                            mapDetails["dipDirection"] = dipDirection.toIntOrNull()
                            mapDetails["rake"] = rake.toIntOrNull()
                            mapDetails["rakeDirection"] = rakeDirection.ifBlank { null }
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

// =========================================================================
// GEOFIELD PRO — HOME METRICS SUITE & BOREHOLE CORE LOGGING USER INTERFACES
// =========================================================================

@Composable
fun HomeScreen(
    onSelectMapping: () -> Unit,
    onSelectLogging: () -> Unit,
    onSelectRockEngineering: () -> Unit,
    onSelectMapTracker: () -> Unit,
    projectsCount: Int = 0,
    boreholesCount: Int = 0
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Premium Welcome Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
            ),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                Color(0xFFC96A2B).copy(alpha = 0.5f)
            )))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GEOFIELD PRO",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.0.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Professional Geological Survey & Core Logging Suite",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Glowing explore/compass indicator
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "GeoField Compass",
                                tint = Color(0xFFC96A2B),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFC96A2B),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = "OFFLINE ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF2ECC71), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "High-Precision Auto UTM Active",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // GNSS / GPS Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsInputAntenna,
                    contentDescription = "GNSS Status",
                    tint = Color(0xFF2ECC71),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Satellite GNSS Connected (Auto UTM Grid Mapping)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Telemetry Hub Metrics Widgets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Projects Logging",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$projectsCount Project" + (if (projectsCount != 1) "s" else ""),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Architecture,
                            contentDescription = null,
                            tint = Color(0xFFC96A2B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Boreholes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$boreholesCount Hole" + (if (boreholesCount != 1) "s" else ""),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Module selection header
        Text(
            text = "CHOOSE FIELD WORKSPACE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )

        // Module Card 1: Geological Surface Mapping
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectMapping() }
                .testTag("module_mapping_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open Module",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Module 1: Surface Geological Mapping",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Outcrop Stations & Structural Bedding Measurements",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Record GPS stations, bedding strike & dip orientations, alteration intensities, associated minerals, lithological descriptors, and geotagged field photos securely in the offline database.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        // Module Card 2: Borehole Logging & Core runs
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectLogging() }
                .testTag("module_logging_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFC96A2B).copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Architecture,
                                contentDescription = null,
                                tint = Color(0xFFC96A2B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open Module",
                        tint = Color(0xFFC96A2B)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Module 2: Borehole Core Logging",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Core Run Recovery, RQD Analytics & Drilling Records",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC96A2B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Log discrete drill core runs, measure recovery metrics, execute automatic RQD calculations, assess joint weathering states, fracture frequencies, and update daily drilling progress parameters.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        // Module Card 3: Rock Engineering & Structural Utilities
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectRockEngineering() }
                .testTag("module_rock_engineering_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Construction,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open Module",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Module 3: Rock Engineering Utilities",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Geotechnical Classifications, RMR89 & Barton Q-System",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Access live interactive estimators for Rock Mass Rating (Bieniawski RMR 1989), Barton Q-System tunnel support index, and perform automated mathematical Strike & Dip conversions to Dip Direction.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        // Module Card 4: GPS Tracker & Survey Map
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectMapTracker() }
                .testTag("module_map_tracker_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open Module",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Module 4: GPS Tracker & Field Map",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Offline Route Tracking, Custom Waypoints & GIS Overlays",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Record continuous survey track routes offline, register custom field waypoints (outcrops, boundaries, water bodies, hazards), toggle multi-layer GIS configurations, and display existing projects elements interactively.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Community & Developer Support Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Professional Connect & Support Hub",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0088CC).copy(alpha = 0.1f))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/geofieldpro"))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0088CC),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("@", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Join GeoField Pro Support Hub",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0088CC)
                        )
                        Text(
                            text = "Telegram: t.me/geofieldpro",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0088CC)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Launch,
                        contentDescription = "Open Link",
                        tint = Color(0xFF0088CC),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A66C2).copy(alpha = 0.1f))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/farzad-fathollahzadeh-b3a6a1165"))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0A66C2),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("in", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Developer's LinkedIn Profile",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A66C2)
                        )
                        Text(
                            text = "linkedin.com/in/farzad-fathollahzadeh-b3a6a1165",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0A66C2)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Launch,
                        contentDescription = "Open Link",
                        tint = Color(0xFF0A66C2),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer telemetry metadata
        Text(
            text = "INDUSTRIAL SOFTWARE SUITE • GEOFIELD PROFESSIONAL PLATFORM FOR GEOTECHNICAL EXPLORATIONS AND SUBSURFACE BOREHOLE SURVEY RESILIENCE.",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BoreholeListScreen(
    boreholes: List<Borehole>,
    onSelectBorehole: (Borehole) -> Unit,
    onCreateClick: () -> Unit,
    onDeleteClick: (Borehole) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredBoreholes = remember(boreholes, searchQuery) {
        if (searchQuery.isBlank()) {
            boreholes
        } else {
            boreholes.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.utmZone.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("borehole_search_input"),
                placeholder = { Text("Search boreholes (e.g. BH-01, UTM zone...)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Statistics Overview Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL COLLARS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${boreholes.size}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Divider(modifier = Modifier.width(1.dp).height(40.dp), color = MaterialTheme.colorScheme.outline)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("MAPPED SATELLITES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "ACTIVE RTK",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2ECC71)
                        )
                    }
                }
            }

            // List of collars
            if (filteredBoreholes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching collars found" else "No active borehole logging collars yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the float action trigger (+) to locate a drill hole.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBoreholes) { borehole ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectBorehole(borehole) }
                                .testTag("borehole_card_${borehole.id}"),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Agriculture,
                                            contentDescription = null,
                                            tint = Color(0xFFC96A2B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = borehole.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    // Delete button
                                    IconButton(
                                        onClick = { onDeleteClick(borehole) },
                                        modifier = Modifier.size(28.dp).testTag("delete_borehole_${borehole.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Collar",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Coordinate grid representation
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("UTM EASTING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = String.format(Locale.US, "%.2f mE", borehole.utmEasting),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("UTM NORTHING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = String.format(Locale.US, "%.2f mN", borehole.utmNorthing),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Column {
                                        Text("ZONE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = borehole.utmZone,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Azimuth angle detail
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Explore,
                                        contentDescription = "Azimuth Angle",
                                        tint = Color(0xFF0A84FF),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Drilling Azimuth: ${borehole.azimuth}°  •  Elevation: ${borehole.elevation}m (±${borehole.elevationAccuracy}m)",
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

        // Floating Action Button
        FloatingActionButton(
            onClick = onCreateClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_borehole_fab"),
            containerColor = Color(0xFFC96A2B),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Drill Collar")
        }
    }
}

@Composable
fun CreateBoreholeScreen(
    viewModel: GeologyViewModel,
    onSave: (name: String, lat: Double, lon: Double, elev: Double, acc: Double, elevAcc: Double, east: Double, north: Double, zone: String, date: Long, azimuth: Double) -> Unit,
    onCancel: () -> Unit
) {
    var boreholeName by remember { mutableStateOf("") }
    var azimuthVal by remember { mutableStateOf("0.0") }
    
    // GPS Coordinates state
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var elevation by remember { mutableStateOf(0.0) }
    var accuracy by remember { mutableStateOf(0.0) }
    var elevAccuracy by remember { mutableStateOf(0.0) }
    
    // UTM projection state
    var utmEast by remember { mutableStateOf(0.0) }
    var utmNorth by remember { mutableStateOf(0.0) }
    var utmZone by remember { mutableStateOf("39N") }
    
    var isLocating by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Location callback trigger
    fun acquireLocation() {
        isLocating = true
        // High fidelity mock geological location triggers
        val latMock = 35.6892 + (Math.random() - 0.5) * 0.05
        val lonMock = 51.3890 + (Math.random() - 0.5) * 0.05
        val elevMock = 1200.0 + Math.random() * 50.0
        val accMock = 2.0 + Math.random() * 1.5
        val elevAccMock = 4.0 + Math.random() * 2.0
        
        latitude = latMock
        longitude = lonMock
        elevation = elevMock
        accuracy = accMock
        elevAccuracy = elevAccMock
        
        // Convert to UTM
        val utm = UtmConverter.convertLatLngToUtm(latMock, lonMock)
        utmEast = utm.easting
        utmNorth = utm.northing
        utmZone = utm.zoneString
        
        isLocating = false
    }

    LaunchedEffect(Unit) {
        acquireLocation() // Automatically acquire collar location on screen entry
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Title Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "COLLAR DESIGNATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC96A2B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = boreholeName,
                    onValueChange = { boreholeName = it },
                    label = { Text("Borehole ID/Collar Code (e.g. DDH-2026-004)") },
                    modifier = Modifier.fillMaxWidth().testTag("borehole_name_input"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = azimuthVal,
                    onValueChange = { azimuthVal = it },
                    label = { Text("Drilling Azimuth / Inclination Angle (Degree)") },
                    modifier = Modifier.fillMaxWidth().testTag("borehole_azimuth_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }

        // GNSS Collar Location Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GPS COLLAR POSITION DATUM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { acquireLocation() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("acquire_location_button")
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = "GPS", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recheck Collar GNSS", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // GPS Raw Table
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("COLLAR LAT", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.6f°", latitude), fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("COLLAR LON", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.6f°", longitude), fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ELEVATION (Z)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.1f m", elevation), fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ACCURACY (H/V)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "±%.2fdp / ±%.2fm", accuracy, elevAccuracy), fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // UTM Grid projections
                Text(
                    text = "CONVERTED UTM COORDINATE ENGINE (WGS84)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0A84FF)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("UTM EASTING (X)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.2f mE", utmEast), fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("UTM NORTHING (Y)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.2f mN", utmNorth), fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("UTM ZONE", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(utmZone, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).testTag("cancel_borehole_button")
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (boreholeName.isNotBlank()) {
                        val az = azimuthVal.toDoubleOrNull() ?: 0.0
                        onSave(
                            boreholeName,
                            latitude,
                            longitude,
                            elevation,
                            accuracy,
                            elevAccuracy,
                            utmEast,
                            utmNorth,
                            utmZone,
                            System.currentTimeMillis(),
                            az
                        )
                    }
                },
                enabled = boreholeName.isNotBlank(),
                modifier = Modifier.weight(1f).testTag("save_borehole_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC96A2B))
            ) {
                Text("Establish Collar")
            }
        }
    }
}

@Composable
fun EditBoreholeDialog(
    borehole: Borehole,
    onDismiss: () -> Unit,
    onSave: (Borehole) -> Unit
) {
    var name by remember { mutableStateOf(borehole.name) }
    var lat by remember { mutableStateOf(borehole.latitude.toString()) }
    var lon by remember { mutableStateOf(borehole.longitude.toString()) }
    var elevation by remember { mutableStateOf(borehole.elevation.toString()) }
    var accuracy by remember { mutableStateOf(borehole.accuracy.toString()) }
    var utmEasting by remember { mutableStateOf(borehole.utmEasting.toString()) }
    var utmNorthing by remember { mutableStateOf(borehole.utmNorthing.toString()) }
    var utmZone by remember { mutableStateOf(borehole.utmZone) }
    var azimuth by remember { mutableStateOf(borehole.azimuth?.toString() ?: "0.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Borehole Collar Info", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Borehole Collar Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lat,
                        onValueChange = { lat = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = lon,
                        onValueChange = { lon = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = elevation,
                        onValueChange = { elevation = it },
                        label = { Text("Elevation (m)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = accuracy,
                        onValueChange = { accuracy = it },
                        label = { Text("GPS Accuracy (m)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = utmEasting,
                        onValueChange = { utmEasting = it },
                        label = { Text("UTM Easting") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = utmNorthing,
                        onValueChange = { utmNorthing = it },
                        label = { Text("UTM Northing") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = utmZone,
                        onValueChange = { utmZone = it },
                        label = { Text("UTM Zone") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = azimuth,
                        onValueChange = { azimuth = it },
                        label = { Text("Azimuth (°)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC96A2B)),
                onClick = {
                    val updatedBorehole = borehole.copy(
                        name = name,
                        latitude = lat.toDoubleOrNull() ?: borehole.latitude,
                        longitude = lon.toDoubleOrNull() ?: borehole.longitude,
                        elevation = elevation.toDoubleOrNull() ?: borehole.elevation,
                        accuracy = accuracy.toDoubleOrNull() ?: borehole.accuracy,
                        utmEasting = utmEasting.toDoubleOrNull() ?: borehole.utmEasting,
                        utmNorthing = utmNorthing.toDoubleOrNull() ?: borehole.utmNorthing,
                        utmZone = utmZone,
                        azimuth = azimuth.toDoubleOrNull() ?: borehole.azimuth
                    )
                    onSave(updatedBorehole)
                }
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BoreholeDashboardScreen(
    borehole: Borehole?,
    runs: List<CoreRun>,
    dailyLogs: List<DailyDrillingLog>,
    onAddRun: () -> Unit,
    onEditRun: (runId: Int) -> Unit,
    onDeleteRun: (runId: Int) -> Unit,
    onAddDailyLog: () -> Unit,
    onDeleteDailyLog: (logId: Int) -> Unit,
    onExportExcel: () -> Unit,
    onExportZip: () -> Unit,
    onUpdateBorehole: (Borehole) -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Core Runs, 1 = Daily Drilling progress
    var showEditBoreholeDialog by remember { mutableStateOf(false) }
    
    if (borehole == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (showEditBoreholeDialog) {
        EditBoreholeDialog(
            borehole = borehole,
            onDismiss = { showEditBoreholeDialog = false },
            onSave = { updated ->
                onUpdateBorehole(updated)
                showEditBoreholeDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Collar Information Summary Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = borehole.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { showEditBoreholeDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Borehole",
                                    tint = Color(0xFFC96A2B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "COLLAR COORDINATE SYSTEMS (WGS84 UTM)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC96A2B)
                        )
                    }
                    Surface(
                        color = Color(0xFF0A84FF).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "AZ: ${borehole.azimuth}°",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A84FF),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EASTING (X)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.2f mE", borehole.utmEasting), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("NORTHING (Y)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.2f mN", borehole.utmNorthing), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("ZONE", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(borehole.utmZone, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Collar GPS: ${String.format(Locale.US, "%.5f", borehole.latitude)}°, ${String.format(Locale.US, "%.5f", borehole.longitude)}°  •  Elev: ${borehole.elevation}m",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Tab Selector Row
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = Color(0xFFC96A2B)
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Core Runs & RQD Logs", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Architecture, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Daily Drilling Progress", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        // Action controls and views mapping
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (activeTab == 0) {
                // CORE RUNS LOGGING VIEW
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Stratigraphic Core Column Visualizer Card
                    if (runs.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "INTERACTIVE STRATIGRAPHIC CORE ENGINE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Interactive RQD Profiler",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                // horizontal bar displaying stacked color runs based on depth
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    runs.forEach { run ->
                                        val runLength = run.getRunLength()
                                        val rqdPercent = run.getRqd()
                                        val weight = if (runLength > 0.0) runLength.toFloat() else 1f
                                        val color = if (rqdPercent < 25.0) Color(0xFFFF3B30) // Red
                                                   else if (rqdPercent < 50.0) Color(0xFFFFB020) // Orange
                                                   else if (rqdPercent < 75.0) Color(0xFF1E5EFF) // Slate Blue
                                                   else Color(0xFF2ECC71) // Rich Green
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(weight)
                                                .fillMaxHeight()
                                                .background(color)
                                                .clickable { onEditRun(run.id) }
                                                .border(0.5.dp, MaterialTheme.colorScheme.background.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${run.fromDepth}-${run.toDepth}m",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                // Legend Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF3B30), RoundedCornerShape(2.dp)))
                                            Text("V. Poor", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFB020), RoundedCornerShape(2.dp)))
                                            Text("Poor", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF1E5EFF), RoundedCornerShape(2.dp)))
                                            Text("Fair", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF2ECC71), RoundedCornerShape(2.dp)))
                                            Text("Good/Exc", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text(
                                        text = "${runs.size} intervals",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CORE LOGGING SEQUENCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = onAddRun,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp).testTag("add_run_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC96A2B))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Run", fontSize = 12.sp)
                        }
                    }

                    if (runs.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No logged core run intervals yet", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(runs) { run ->
                                val recoveryPercent = run.getCoreRecovery()
                                val rqdPercent = run.getRqd()

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onEditRun(run.id) }
                                        .testTag("run_card_${run.id}"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Header: From - To depths
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = Color(0xFFC96A2B).copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "RUN ${run.fromDepth}m - ${run.toDepth}m",
                                                        fontSize = 12.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFC96A2B),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = run.rockType,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            IconButton(
                                                onClick = { onDeleteRun(run.id) },
                                                modifier = Modifier.size(24.dp).testTag("delete_run_${run.id}")
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Run", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Recovery and RQD calculations metrics
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("CORE RECOVERY %", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    text = String.format(Locale.US, "%.1f %%", recoveryPercent),
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (recoveryPercent < 50.0) Color(0xFFFF3B30) else if (recoveryPercent < 75.0) Color(0xFFFFB020) else Color(0xFF2ECC71)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("ROCK RQD %", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    text = String.format(Locale.US, "%.1f %%", rqdPercent),
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (rqdPercent < 25.0) Color(0xFFFF3B30) else if (rqdPercent < 50.0) Color(0xFFFFB020) else if (rqdPercent < 75.0) Color(0xFF1E5EFF) else Color(0xFF2ECC71)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("ROCK QUALITY", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                val quality = if (rqdPercent < 25.0) "Very Poor" else if (rqdPercent < 50.0) "Poor" else if (rqdPercent < 75.0) "Fair" else if (rqdPercent < 90.0) "Good" else "Excellent"
                                                Text(
                                                    text = quality,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        if (run.notes.isNotBlank()) {
                                            Text(
                                                text = "Log note: ${run.notes}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // DAILY DRILLING LOGS VIEW
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DRILLING PROGRESS HISTORY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = onAddDailyLog,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp).testTag("add_daily_log_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Log", fontSize = 12.sp)
                        }
                    }

                    if (dailyLogs.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No daily logs recorded yet", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(dailyLogs) { log ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = log.date,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            IconButton(
                                                onClick = { onDeleteDailyLog(log.id) },
                                                modifier = Modifier.size(24.dp).testTag("delete_daily_log_${log.id}")
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Log", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "Active drilling depth log: ${log.depthDrilled}m",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFFC96A2B)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Summary: ${log.lithologySummary}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        if (log.operationalNotes.isNotBlank()) {
                                            Text(
                                                text = "Operations: ${log.operationalNotes}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
}

@Composable
fun CreateDailyLogScreen(
    boreholeId: Int,
    onSave: (logId: Int, bhId: Int, date: String, depth: Double, summary: String, ops: String, comments: String) -> Unit,
    onCancel: () -> Unit
) {
    var depthStr by remember { mutableStateOf("") }
    var summaryStr by remember { mutableStateOf("") }
    var opsStr by remember { mutableStateOf("") }
    var commentsStr by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("LOG DAILY PROGRESS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC96A2B))
                
                OutlinedTextField(
                    value = depthStr,
                    onValueChange = { depthStr = it },
                    label = { Text("Active Drilling Depth End of Day (m)") },
                    modifier = Modifier.fillMaxWidth().testTag("daily_log_depth"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = summaryStr,
                    onValueChange = { summaryStr = it },
                    label = { Text("Drilling Summary (e.g., Casing set at 12m, HQ coring)") },
                    modifier = Modifier.fillMaxWidth().testTag("daily_log_summary"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = opsStr,
                    onValueChange = { opsStr = it },
                    label = { Text("Operations Details (Bit change, fluid viscosity, etc.)") },
                    modifier = Modifier.fillMaxWidth().testTag("daily_log_ops"),
                    minLines = 2
                )

                OutlinedTextField(
                    value = commentsStr,
                    onValueChange = { commentsStr = it },
                    label = { Text("Geotechnical Comments or Delays details") },
                    modifier = Modifier.fillMaxWidth().testTag("daily_log_comments"),
                    minLines = 2
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).testTag("cancel_daily_log")
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val d = depthStr.toDoubleOrNull() ?: 0.0
                    onSave(0, boreholeId, SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()), d, summaryStr, opsStr, commentsStr)
                },
                enabled = depthStr.isNotBlank() && summaryStr.isNotBlank(),
                modifier = Modifier.weight(1f).testTag("save_daily_log"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC96A2B))
            ) {
                Text("Record Log")
            }
        }
    }
}

@Composable
fun CreateCoreRunScreen(
    viewModel: GeologyViewModel,
    boreholeId: Int,
    runId: Int?,
    onSave: (
        runId: Int,
        bhId: Int,
        fromDepth: Double,
        toDepth: Double,
        piecesLength: Double,
        measuredRec: Double,
        rockType: String,
        rockDesc: String,
        altType: String,
        altIntensity: String,
        minType: String,
        minIntensity: String,
        fracFreq: Double?,
        veinDensity: Double?,
        jointsDesc: String?,
        color: String,
        texture: String,
        grainSize: String,
        commentsVal: String
    ) -> Unit,
    onCancel: () -> Unit
) {
    var fromDepthStr by remember { mutableStateOf("") }
    var toDepthStr by remember { mutableStateOf("") }
    var measuredRecStr by remember { mutableStateOf("") }
    var piecesLengthStr by remember { mutableStateOf("") }

    // Lithological details
    var selectedRockType by remember { mutableStateOf("HQ Drill Core") }
    var rockDescVal by remember { mutableStateOf("") }
    var colorVal by remember { mutableStateOf("") }
    var textureVal by remember { mutableStateOf("") }
    var grainSizeVal by remember { mutableStateOf("") }

    // Weathering, mineralization & alteration
    var alterationVal by remember { mutableStateOf("None") }
    var alterationIntensity by remember { mutableStateOf("Weak") }
    var mineralizationVal by remember { mutableStateOf("None") }
    var mineralizationIntensity by remember { mutableStateOf("Weak") }

    // Geotechnical Details
    var fractureFreqStr by remember { mutableStateOf("1.0") }
    var veinDensityStr by remember { mutableStateOf("1.0") }
    var jointsDescVal by remember { mutableStateOf("") }
    var commentsValStr by remember { mutableStateOf("") }

    // Populate existing run if editing
    LaunchedEffect(runId) {
        if (runId != null) {
            val existingRun = viewModel.dao.getCoreRunByIdOnce(runId)
            if (existingRun != null) {
                fromDepthStr = existingRun.fromDepth.toString()
                toDepthStr = existingRun.toDepth.toString()
                measuredRecStr = existingRun.recoveredLength.toString()
                piecesLengthStr = existingRun.piecesOver10Cm.toString()
                selectedRockType = existingRun.rockType
                rockDescVal = existingRun.lithologyDescription
                colorVal = existingRun.color
                textureVal = existingRun.texture
                grainSizeVal = existingRun.grainSize
                alterationVal = existingRun.alterationType
                alterationIntensity = existingRun.alterationIntensity
                mineralizationVal = existingRun.mineralizationType
                mineralizationIntensity = existingRun.mineralizationIntensity
                fractureFreqStr = (existingRun.fractureFrequency ?: 1.0).toString()
                veinDensityStr = (existingRun.veinDensity ?: 1.0).toString()
                jointsDescVal = existingRun.jointPresence ?: ""
                commentsValStr = existingRun.notes
            }
        }
    }

    // Interactive Core quality computations
    val fromDepth = fromDepthStr.toDoubleOrNull() ?: 0.0
    val toDepth = toDepthStr.toDoubleOrNull() ?: 0.0
    val runLength = toDepth - fromDepth
    val measuredRec = measuredRecStr.toDoubleOrNull() ?: 0.0
    val piecesLength = piecesLengthStr.toDoubleOrNull() ?: 0.0

    val recoveryPercent = if (runLength > 0) (measuredRec / runLength) * 100 else 0.0
    val rqdPercent = if (runLength > 0) (piecesLength / runLength) * 100 else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Drilled intervals
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("RUN INTERVAL PARAMETERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC96A2B))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = fromDepthStr,
                        onValueChange = { fromDepthStr = it },
                        label = { Text("From Depth (m)") },
                        modifier = Modifier.weight(1f).testTag("run_from_depth"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = toDepthStr,
                        onValueChange = { toDepthStr = it },
                        label = { Text("To Depth (m)") },
                        modifier = Modifier.weight(1f).testTag("run_to_depth"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                if (runLength > 0) {
                    Text(
                        text = String.format(Locale.US, "Calculated Interval Run Length: %.2f m", runLength),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 2: Core Measurements, Recovery & RQD Auto calculations
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("INTELLIGENT RUN QUANTITY METRICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A84FF))

                OutlinedTextField(
                    value = measuredRecStr,
                    onValueChange = { measuredRecStr = it },
                    label = { Text("Measured Recovered Core Total Length (m)") },
                    modifier = Modifier.fillMaxWidth().testTag("measured_rec_length"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = piecesLengthStr,
                    onValueChange = { piecesLengthStr = it },
                    label = { Text("Sum of Recovered Core Pieces >= 10 cm (m)") },
                    modifier = Modifier.fillMaxWidth().testTag("pieces_above_ten_cm"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    placeholder = { Text("To calculate RQD %") }
                )

                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(4.dp))

                // Recovery Indicator Panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("RECOVERY RATE", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%.1f %%", recoveryPercent),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (recoveryPercent < 50) Color(0xFFFF3B30) else if (recoveryPercent < 75) Color(0xFFFFB020) else Color(0xFF2ECC71)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("ROCK RQD RATE (QUALITY)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%.1f %%", rqdPercent),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (rqdPercent < 25) Color(0xFFFF3B30) else if (rqdPercent < 50) Color(0xFFFFB020) else if (rqdPercent < 75) Color(0xFF1E5EFF) else Color(0xFF2ECC71)
                        )
                    }
                }
            }
        }

        // Section 3: Rock Lithology and Description
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("LITHOLOGY & CORING CODES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC96A2B))

                val lithologyDict = listOf("Granite", "Basalt", "Sandstone", "Shale", "Schist", "Gneiss", "Diorite", "Andesite", "Rhyolite", "Gabbro", "Limestone", "Quartzite", "Marble", "Claystone", "Tuff", "Pegmatite", "Siltstone", "Breccia", "Conglomerate", "Granodiorite", "Dacite", "Amphibolite", "Phyllite", "Slate", "Serpentinite")
                val colorDict = listOf("Dark Grey", "Light Grey", "Reddish Brown", "Pinkish Grey", "Greenish", "Buff", "Tan", "White", "Black", "Yellowish Brown", "Olive Green")
                val textureDict = listOf("Porphyritic", "Foliated", "Gneissic", "Aphanitic", "Phaneritic", "Glassy", "Vesicular", "Fragmental", "Pyroclastic", "Pegmatitic", "Granoblastic", "Schistose", "Massive", "Banded", "Crystalline", "Clastic", "Oolitic", "Brecciated", "Mylonitic")
                val grainDict = listOf("Very Fine", "Fine-grained", "Medium", "Coarse-grained", "Porphyritic", "Pegmatitic")
                val alterationDict = listOf("Potassic", "Propylitic", "Argillic", "Sericitic", "Silicification", "Weak Chlorite", "Epidote-chlorite", "Carbonatization", "Albitization", "Pyritization")
                val mineralDict = listOf("Pyrite", "Chalcopyrite", "Quartz", "Feldspar", "Mica", "Biotite", "Muscovite", "Hornblende", "Olivine", "Pyroxene", "Calcite")

                SmartAutoCompleteTextField(
                    value = selectedRockType,
                    onValueChange = { selectedRockType = it },
                    label = "Rock Type / Geological Interval Code",
                    suggestions = lithologyDict,
                    testTag = "rock_type",
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rockDescVal,
                    onValueChange = { rockDescVal = it },
                    label = { Text("Core Description (grain, texture descriptors...)") },
                    modifier = Modifier.fillMaxWidth().testTag("rock_desc"),
                    minLines = 2
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartAutoCompleteTextField(
                        value = colorVal,
                        onValueChange = { colorVal = it },
                        label = "Color",
                        suggestions = colorDict,
                        testTag = "rock_color"
                    )
                    SmartAutoCompleteTextField(
                        value = textureVal,
                        onValueChange = { textureVal = it },
                        label = "Texture",
                        suggestions = textureDict,
                        testTag = "rock_texture"
                    )
                    SmartAutoCompleteTextField(
                        value = grainSizeVal,
                        onValueChange = { grainSizeVal = it },
                        label = "Grain Size",
                        suggestions = grainDict,
                        testTag = "rock_grain"
                    )
                }
            }
        }

        // Section 4: Alterations, Mineralization, Geotechnical Jointings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("GEOCHEMICAL & STRUCTURE METADATA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                val alterationDict = listOf("Potassic", "Propylitic", "Argillic", "Sericitic", "Silicification", "Weak Chlorite", "Epidote-chlorite", "Carbonatization", "Albitization", "Pyritization")
                val mineralDict = listOf("Pyrite", "Chalcopyrite", "Quartz", "Feldspar", "Mica", "Biotite", "Muscovite", "Hornblende", "Olivine", "Pyroxene", "Calcite")

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartAutoCompleteTextField(
                        value = alterationVal,
                        onValueChange = { alterationVal = it },
                        label = "Alteration Type",
                        suggestions = alterationDict,
                        testTag = "alteration_type"
                    )
                    OutlinedTextField(
                        value = alterationIntensity,
                        onValueChange = { alterationIntensity = it },
                        label = { Text("Intensity") },
                        modifier = Modifier.fillMaxWidth().testTag("alteration_intensity"),
                        singleLine = true
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartAutoCompleteTextField(
                        value = mineralizationVal,
                        onValueChange = { mineralizationVal = it },
                        label = "Mineralization",
                        suggestions = mineralDict,
                        testTag = "min_type"
                    )
                    OutlinedTextField(
                        value = mineralizationIntensity,
                        onValueChange = { mineralizationIntensity = it },
                        label = { Text("Intensity") },
                        modifier = Modifier.fillMaxWidth().testTag("min_intensity"),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fractureFreqStr,
                        onValueChange = { fractureFreqStr = it },
                        label = { Text("Frac Freq /m") },
                        modifier = Modifier.weight(1f).testTag("fracture_freq"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = veinDensityStr,
                        onValueChange = { veinDensityStr = it },
                        label = { Text("Vein Density %") },
                        modifier = Modifier.weight(1f).testTag("vein_density"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = jointsDescVal,
                    onValueChange = { jointsDescVal = it },
                    label = { Text("Geotechnical Joints and Fracture Descriptions") },
                    modifier = Modifier.fillMaxWidth().testTag("joints_desc"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = commentsValStr,
                    onValueChange = { commentsValStr = it },
                    label = { Text("Run Logging Comments / Remarks") },
                    modifier = Modifier.fillMaxWidth().testTag("comments_desc"),
                    minLines = 2
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).testTag("cancel_core_run")
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (runLength > 0 && selectedRockType.isNotBlank()) {
                        val frac = fractureFreqStr.toDoubleOrNull()
                        val v = veinDensityStr.toDoubleOrNull()
                        onSave(
                            runId ?: 0,
                            boreholeId,
                            fromDepth,
                            toDepth,
                            piecesLength,
                            measuredRec,
                            selectedRockType,
                            rockDescVal,
                            alterationVal,
                            alterationIntensity,
                            mineralizationVal,
                            mineralizationIntensity,
                            frac,
                            v,
                            jointsDescVal,
                            colorVal,
                            textureVal,
                            grainSizeVal,
                            commentsValStr
                        )
                    }
                },
                enabled = runLength > 0 && selectedRockType.isNotBlank(),
                modifier = Modifier.weight(1f).testTag("save_core_run"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC96A2B))
            ) {
                Text("Record Run Interval")
            }
        }
    }
}

@Composable
fun BoreholeExportDialog(
    onDismiss: () -> Unit,
    onExportExcel: () -> Unit,
    onExportZip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = Color(0xFFC96A2B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Export Logging Dataset",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Select format for output. All calculations (RQD %, Core Recovery %) are structured cleanly for geological processing.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1: Excel Multi-sheet Ledger
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportExcel() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2ECC71).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("XLS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71))
                            }
                        }
                        Column {
                            Text("Excel Workbook (.xlsx)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Best for core run reports with formulas.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Option 2: CSV Zip Archive
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportZip() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0A84FF).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("ZIP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A84FF))
                            }
                        }
                        Column {
                            Text("Offline ZIP CSV directory", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Best for import into ArcGIS/QGIS/Micromine.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun RockEngineeringScreen(
    viewModel: GeologyViewModel,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } 
    // State HOISTING for synchronized rock engineering calculations
    var activeRmrValue by remember { mutableStateOf(58.0) }
    var activeQValue by remember { mutableStateOf(4.5) }
    var activeGsiValue by remember { mutableStateOf(55.0) }
    var activeUcsValue by remember { mutableStateOf(120.0) }
    var activeUcsRockName by remember { mutableStateOf("Granite") }
    var activeMiValue by remember { mutableStateOf(32.0) }
    var activeDValue by remember { mutableStateOf(0.0) } // Disturbance factor

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Pills - Horizontal Scrollable Row to support expansion elegantly without layout stretching
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 4.dp,
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf(
                    "Standard RMR", 
                    "Barton Q-System", 
                    "GSI & Hoek-Brown", 
                    "UCS Database", 
                    "Automated Report", 
                    "Structural Dip", 
                    "Window Mapping"
                )
                tabs.forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> RmrCalculatorSection(onRmrCalculated = { activeRmrValue = it })
                1 -> QSystemCalculatorSection(onQCalculated = { activeQValue = it })
                2 -> GsiHoekBrownSection(
                    gsi = activeGsiValue,
                    onGsiChange = { activeGsiValue = it },
                    ucs = activeUcsValue,
                    onUcsChange = { activeUcsValue = it },
                    mi = activeMiValue,
                    onMiChange = { activeMiValue = it },
                    d = activeDValue,
                    onDChange = { activeDValue = it },
                    rockName = activeUcsRockName
                )
                3 -> UcsDatabaseSection(
                    onSelectRock = { name, ucsVal, miVal ->
                        activeUcsRockName = name
                        activeUcsValue = ucsVal
                        activeMiValue = miVal
                        Toast.makeText(context, "Applied typical $name data (UCS: $ucsVal MPa, mi: $miVal) to geomechanics section", Toast.LENGTH_SHORT).show()
                    }
                )
                4 -> RockMassReportSection(
                    rmr = activeRmrValue,
                    q = activeQValue,
                    gsi = activeGsiValue,
                    ucs = activeUcsValue,
                    rockName = activeUcsRockName,
                    mi = activeMiValue,
                    d = activeDValue
                )
                5 -> StructuralConverterSection(viewModel)
                6 -> WindowMappingSection(viewModel)
            }
        }
    }
}

@Composable
fun RmrCalculatorSection(onRmrCalculated: (Double) -> Unit = {}) {
    // Advanced RMR Method Modes
    var rmrMethodMode by remember { mutableStateOf(0) } // 0 = Standard RMR (1989), 1 = Improved RMR (Modern Modified)
    var feIndex by remember { mutableStateOf(1) } // Default: Smooth Blast (0.92)
    var fsIndex by remember { mutableStateOf(1) } // Default: Moderate Depth (1.00)
    var useContinuousRating by remember { mutableStateOf(true) }

    val feFactors = listOf(1.00, 0.92, 0.85, 0.75)
    val feLabels = listOf("TBM (1.0)", "Smooth (0.92)", "Std (0.85)", "Poor (0.75)")

    val fsFactors = listOf(1.05, 1.00, 0.95, 0.80)
    val fsLabels = listOf("Low (1.05)", "Moderate (1.0)", "High (0.95)", "Extreme (0.80)")

    // 5 parameters for Bieniawski RMR (1989)
    var selectedStrengthOption by remember { mutableStateOf(0) }
    var selectedRqdOption by remember { mutableStateOf(0) }
    var selectedSpacingOption by remember { mutableStateOf(0) }
    var selectedConditionOption by remember { mutableStateOf(0) }
    var selectedWaterOption by remember { mutableStateOf(0) }

    // 6th Parameter for Discontinuity Orientation Adjustment
    var excavationTypeIndex by remember { mutableStateOf(0) }
    var favorabilityIndex by remember { mutableStateOf(0) }

    val strengths = listOf(
        OptionItem(">250 MPa (Very High Strengths)", 15),
        OptionItem("100 - 250 MPa (High Strength)", 12),
        OptionItem("50 - 100 MPa (Medium Strength)", 7),
        OptionItem("25 - 50 MPa (Low Strength)", 4),
        OptionItem("5 - 25 MPa (Very Low Strength)", 2),
        OptionItem("<5 MPa (Weak Clayey Rock)", 1)
    )

    val rqds = listOf(
        OptionItem("90% - 100% (Excellent Quality)", 20),
        OptionItem("75% - 90% (Good Quality)", 17),
        OptionItem("50% - 75% (Fair Quality)", 13),
        OptionItem("25% - 50% (Poor Quality)", 8),
        OptionItem("<25% (Very Poor Quality)", 3)
    )

    val spacings = listOf(
        OptionItem("> 2.0 meters (Very Wide spacing)", 20),
        OptionItem("0.6 - 2.0 meters (Wide spacing)", 15),
        OptionItem("200 - 600 mm (Moderate spacing)", 10),
        OptionItem("60 - 200 mm (Close spacing)", 8),
        OptionItem("< 60 mm (Very Close spacing)", 5)
    )

    val conditions = listOf(
        OptionItem("Very rough, unweathered, tight fissure walls", 30),
        OptionItem("Slightly rough, aperture < 1mm, slightly weathered", 25),
        OptionItem("Smooth, aperture < 1mm, highly weathered walls", 20),
        OptionItem("Gouge filling < 5mm or aperture 1-5mm, continuous", 10),
        OptionItem("Soft clay gouge > 5mm or aperture > 5mm, continuous", 0)
    )

    val waterStates = listOf(
        OptionItem("Completely Dry excavation", 15),
        OptionItem("Damp (moderate water condensation)", 10),
        OptionItem("Wet (clear steady seepage)", 7),
        OptionItem("Dripping (heavy continuous inflow)", 4),
        OptionItem("Flowing water (severe dynamic piping)", 0)
    )

    val excavationTypes = listOf("Tunnels & Mines", "Foundations", "Slopes & Cliffs")
    val favorabilities = listOf(
        OptionItem("Very Favorable", 0),
        OptionItem("Favorable", 1),
        OptionItem("Fair", 2),
        OptionItem("Unfavorable", 3),
        OptionItem("Very Unfavorable", 4)
    )

    val baseScore = strengths[selectedStrengthOption].score +
            rqds[selectedRqdOption].score +
            spacings[selectedSpacingOption].score +
            conditions[selectedConditionOption].score +
            waterStates[selectedWaterOption].score

    val adjustmentFactor = when (excavationTypeIndex) {
        0 -> when (favorabilityIndex) { // Tunnels
            0 -> 0
            1 -> -2
            2 -> -5
            3 -> -10
            4 -> -12
            else -> 0
        }
        1 -> when (favorabilityIndex) { // Foundations
            0 -> 0
            1 -> -2
            2 -> -7
            3 -> -15
            4 -> -25
            else -> 0
        }
        2 -> when (favorabilityIndex) { // Slopes
            0 -> 0
            1 -> -5
            2 -> -25
            3 -> -50
            4 -> -60
            else -> 0
        }
        else -> 0
    }

    val totalScoreStandard = (baseScore + adjustmentFactor).coerceIn(0, 100)

    val totalScoreNum = if (rmrMethodMode == 1) {
        // Continuous rating optimization smooths boundaries
        val baseSmoothed = if (useContinuousRating) {
            baseScore.toDouble() + 1.25
        } else {
            baseScore.toDouble()
        }
        val modifiedValue = (baseSmoothed * feFactors[feIndex] * fsFactors[fsIndex]) + adjustmentFactor
        modifiedValue.coerceIn(0.0, 100.0)
    } else {
        totalScoreStandard.toDouble()
    }

    val totalScore = Math.round(totalScoreNum).toInt().coerceIn(0, 100)

    LaunchedEffect(totalScoreNum) {
        onRmrCalculated(totalScoreNum)
    }

    // Serafim and Pereira (1983) Correlation for Em (GPa)
    val rockModulus = Math.pow(10.0, (totalScoreNum - 10.0) / 40.0)

    // Classifications list
    val (className, classColor, standUpTime, mechanicalEstimates) = when {
        totalScore in 81..100 -> Quadruple(
            "Class I (Very Good Rock)",
            Color(0xFF2ECC71),
            "Stand-up Time: 20 years for 15 meters span",
            "Cohesion: >400 kPa • Friction Angle: >45°"
        )
        totalScore in 61..80 -> Quadruple(
            "Class II (Good Rock)",
            Color(0xFF1ABC9C),
            "Stand-up Time: 1 year for 10 meters span",
            "Cohesion: 300 - 400 kPa • Friction Angle: 35° - 45°"
        )
        totalScore in 41..60 -> Quadruple(
            "Class III (Fair Rock)",
            Color(0xFFF1C40F),
            "Stand-up Time: 1 week for 5 meters span",
            "Cohesion: 200 - 300 kPa • Friction Angle: 25° - 35°"
        )
        totalScore in 21..40 -> Quadruple(
            "Class IV (Poor Rock)",
            Color(0xFFE67E22),
            "Stand-up Time: 10 hours for 2.5 meters span",
            "Cohesion: 100 - 200 kPa • Friction Angle: 15° - 25°"
        )
        else -> Quadruple(
            "Class V (Very Poor Rock)",
            Color(0xFFE74C3C),
            "Stand-up Time: 30 minutes for 1.0 meter span",
            "Cohesion: <100 kPa • Friction Angle: <15°"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Estimation Mode Selector (Standard vs Improved RMR)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = listOf("RMR89 Standard", "RMR Improved (Modern)")
            modes.forEachIndexed { index, label ->
                val isSelected = rmrMethodMode == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { rmrMethodMode = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (rmrMethodMode == 1) {
            // Improved RMR Correction Parameters Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Improved RMR Correction Parameters (Bieniawski & Celada 2014)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Adjusts RMR rating based on the excavation technique (Fe) and the in-situ stress state (Fs) of the rock mass.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Fe Selector
                    Text("1. Excavation Method Damage (Fe):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        feLabels.forEachIndexed { idx, label ->
                            val isSel = idx == feIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                    .clickable { feIndex = idx }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Fs Selector
                    Text("2. In-Situ Stress state (Fs):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        fsLabels.forEachIndexed { idx, label ->
                            val isSel = idx == fsIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                    .clickable { fsIndex = idx }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Continuous Rating Toggle row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("3. Continuous Rating Interpolation", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Reduces interval boundary estimation errors", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = useContinuousRating,
                            onCheckedChange = { useContinuousRating = it }
                        )
                    }
                }
            }
        }

        // Output Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = classColor.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, classColor.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (rmrMethodMode == 1) "Improved RMR (Bieniawski & Celada 2014) • RMR_mod = (Base * Fe * Fs) + Adj" else "Bieniawski RMR (1989) estimation with Joint Adjustments",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = className,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = classColor
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (rmrMethodMode == 1) String.format(Locale.US, "%.1f", totalScoreNum) else "$totalScore",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = classColor
                        )
                        Text(
                            text = if (rmrMethodMode == 1) "Base: $baseScore | Fe:${feFactors[feIndex]} | Fs:${fsFactors[fsIndex]}" else "Base Score: $baseScore",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = classColor.copy(alpha = 0.2f))
                Text(
                    text = standUpTime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mechanicalEstimates,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = classColor.copy(alpha = 0.1f))

                // Advanced Scientific Modulus Estimate Output
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deformation Modulus (Em)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f GPa", rockModulus),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Serafim-Pereira Correlation (1983) formula estimation.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 6th Parameter Selection Block (Joint Adjustments)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "6. Discontinuity Orientation Adjustments",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Segmented Excavation Type Selection
                Text("Select Engineering/Excavation Application:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    excavationTypes.forEachIndexed { idx, label ->
                        val isSel = idx == excavationTypeIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { excavationTypeIndex = idx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Segmented Joint Favorability Selection
                Text("Discontinuity Strike & Dip Relation Favourability:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val favorabilityShort = listOf("Very Fav", "Fav", "Fair", "Unfav", "Very Unfav")
                    favorabilityShort.forEachIndexed { idx, label ->
                        val isSel = idx == favorabilityIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                .clickable { favorabilityIndex = idx }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = "Rating penalty applied: $adjustmentFactor points (Bieniawski 1989 Table)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = "ROCK MASS PROPERTIES PARAMETERS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )

        // Render Parameters
        ParameterSelectionRow(title = "1. Uniaxial Compressive Strength (UCS)", options = strengths, selectedIndex = selectedStrengthOption) { selectedStrengthOption = it }
        ParameterSelectionRow(title = "2. Drill Core RQD", options = rqds, selectedIndex = selectedRqdOption) { selectedRqdOption = it }
        ParameterSelectionRow(title = "3. Spacing of Discontinuities", options = spacings, selectedIndex = selectedSpacingOption) { selectedSpacingOption = it }
        ParameterSelectionRow(title = "4. Discontinuity Conditions", options = conditions, selectedIndex = selectedConditionOption) { selectedConditionOption = it }
        ParameterSelectionRow(title = "5. Groundwater Conditions", options = waterStates, selectedIndex = selectedWaterOption) { selectedWaterOption = it }
    }
}

data class OptionItem(val text: String, val score: Int)
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ParameterSelectionRow(
    title: String,
    options: List<OptionItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = options[selectedIndex].text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "+${options[selectedIndex].score}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (expanded) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Column(modifier = Modifier.padding(8.dp)) {
                    options.forEachIndexed { index, option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (index == selectedIndex) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                .clickable {
                                    onSelect(index)
                                    expanded = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.text,
                                fontSize = 12.sp,
                                color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Score: ${option.score}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
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
fun QSystemCalculatorSection(onQCalculated: (Double) -> Unit = {}) {
    var rqd by remember { mutableStateOf(75.0) }
    var jnSelected by remember { mutableStateOf(0) }
    var jrSelected by remember { mutableStateOf(0) }
    var jaSelected by remember { mutableStateOf(0) }
    var jwSelected by remember { mutableStateOf(0) }
    var srfSelected by remember { mutableStateOf(0) }

    // Support Design Inputs
    var excavationSpan by remember { mutableStateOf(8.0f) }
    var esrIndex by remember { mutableStateOf(1) } // Default: Permanent mine openings / water tunnels (1.6)

    val jnOptions = listOf(
        OptionItem("Massive, no or few joints", 1),
        OptionItem("One joint set", 2),
        OptionItem("One joint set plus random joints", 3),
        OptionItem("Two joint sets", 4),
        OptionItem("Two joint sets plus random joints", 6),
        OptionItem("Three joint sets", 9),
        OptionItem("Three joint sets plus random joints", 12),
        OptionItem("Four or more joint sets, heavily fractured", 15),
        OptionItem("Crushed rock, clay filling, earth-like", 20)
    )
    val jnValues = listOf(0.5, 2.0, 3.0, 4.0, 6.0, 9.0, 12.0, 15.0, 20.0)

    val jrOptions = listOf(
        OptionItem("Discontinuous joint planes", 40),
        OptionItem("Rough or irregular, undulating", 30),
        OptionItem("Smooth, undulating", 20),
        OptionItem("Rough or irregular, planar", 15),
        OptionItem("Smooth, planar", 10),
        OptionItem("Slickensided, planar shear surface", 5)
    )
    val jrValues = listOf(4.0, 3.0, 2.0, 1.5, 1.0, 0.5)

    val jaOptions = listOf(
        OptionItem("Tightly healed, hard, non-softening filling", 7),
        OptionItem("Unaltered joint walls, surface staining only", 10),
        OptionItem("Slightly altered wall joints, sand or clay coatings", 20),
        OptionItem("Silty or sandy-clay coatings, soft clay-free fill", 30),
        OptionItem("Soft clay filling, continuous (< 5mm thick)", 80),
        OptionItem("Swelling clay filling, continuous (> 5mm thick)", 150)
    )
    val jaValues = listOf(0.75, 1.0, 2.0, 3.0, 8.0, 15.0)

    val jwOptions = listOf(
        OptionItem("Dry excavations or minor inflow (< 5 l/m)", 10),
        OptionItem("Medium inflow, local outwash of joint fillings", 6),
        OptionItem("Large inflow / high pressure in uncut joints", 5),
        OptionItem("Exceptionally high inflow, decaying with time", 2),
        OptionItem("Severe high pressure flow, constant volume", 1)
    )
    val jwValues = listOf(1.0, 0.66, 0.5, 0.2, 0.1)

    val srfOptions = listOf(
        OptionItem("Low stress, local shear zones within excavation", 25),
        OptionItem("Medium stress, favorable stable loading condition", 10),
        OptionItem("High stress, tight structural containment", 5),
        OptionItem("Mild into Heavy rock-burst conditions", 50),
        OptionItem("Squeezing clay swelling dynamic pressures", 100)
    )
    val srfValues = listOf(2.5, 1.0, 0.5, 5.0, 10.0)

    val esrValues = listOf(3.0, 1.6, 1.0, 0.8)
    val esrLabels = listOf("Temp Opening (3.0)", "Mine/Water (1.6)", "Road Tunnel (1.0)", "Subway/Civil (0.8)")

    val qValue = (rqd / jnValues[jnSelected]) * (jrValues[jrSelected] / jaValues[jaSelected]) * (jwValues[jwSelected] / srfValues[srfSelected])

    LaunchedEffect(qValue) {
        onQCalculated(qValue)
    }

    val (qClass, qColor) = when {
        qValue > 400.0 -> "Exceptionally Good Rock" to Color(0xFF00E676)
        qValue > 100.0 -> "Extremely Good Rock" to Color(0xFF00C853)
        qValue > 40.0 -> "Very Good tunnel mass" to Color(0xFF0984FF)
        qValue > 10.0 -> "Good tunnel mass" to Color(0xFF00796B)
        qValue > 4.0 -> "Fair Rock" to Color(0xFFF1C40F)
        qValue > 1.0 -> "Poor Rock" to Color(0xFFE67E22)
        qValue > 0.1 -> "Very Poor Rock" to Color(0xFFD32F2F)
        qValue > 0.01 -> "Extremely Poor" to Color(0xFFC0392B)
        else -> "Exceptionally Poor" to Color(0xFF7F0000)
    }

    // Geotechnical Support Calculations
    val activeEsr = esrValues[esrIndex]
    val equivalentDim = excavationSpan / activeEsr
    val maxUnsupportedSpan = 2.0 * activeEsr * Math.pow(qValue, 0.4)
    val isSpanSafe = excavationSpan <= maxUnsupportedSpan

    // PV support pressure formula after Barton et al., 1974 (acting in kg/cm2, multiplied by 100 to get PV kPa)
    val jnVal = jnValues[jnSelected]
    val jrVal = jrValues[jrSelected]
    val roofPressureKpa = (2.0 * Math.sqrt(jnVal) * Math.pow(qValue, -1.0 / 3.0) / (3.0 * jrVal)) * 100.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Output Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = qColor.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, qColor.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Barton Q-System Tunnel Quality Index",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = qClass,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = qColor
                    )
                    Text(
                        text = String.format(Locale.US, "%.3f", qValue),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = qColor
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = qColor.copy(alpha = 0.2f))
                Text(
                    text = "Formula: (RQD / Jn) * (Jr / Ja) * (Jw / SRF)",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Advanced Geotechnical Support Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Active Support Recommendation & Safety Limits",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Excavation Dimensions Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Excavation Width/Span:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(String.format(Locale.US, "%.1f meters", excavationSpan), fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                }
                Slider(
                    value = excavationSpan,
                    onValueChange = { excavationSpan = it },
                    valueRange = 2.0f..30.0f
                )

                // ESR Selector Row
                Text("Excavation Category (ESR Control):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    esrLabels.forEachIndexed { idx, label ->
                        val isSel = idx == esrIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                .clickable { esrIndex = idx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Calculated Indicators
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Equivalent Dimension (De)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.2f m", equivalentDim), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Max Unsupported Span", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.1f meters", maxUnsupportedSpan), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (isSpanSafe) Color(0xFF2ECC71) else Color(0xFFE74C3C))
                    }
                    Column(modifier = Modifier.weight(1.1f)) {
                        Text("Roof Pressure Estimate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.1f kPa", roofPressureKpa), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Safety Alert Status Banner
                val bannerColor = if (isSpanSafe) Color(0xFF2196F3).copy(alpha = 0.1f) else Color(0xFFF44336).copy(alpha = 0.1f)
                val bannerBorder = if (isSpanSafe) Color(0xFF2196F3) else Color(0xFFF44336)
                val messageText = if (isSpanSafe) {
                    "✅ Dimension is stable unsupported. Spot rock-bolting & shotcrete recommended for localized joint wedges."
                } else {
                    "⚠️ MAXIMUM UNSUPPORTED SPAN EXCEEDED! Immediate systematized steel ribs, rock-bolts, and fiber-reinforced shotcrete required."
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = bannerColor,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, bannerBorder.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = messageText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = bannerBorder,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Slider for RQD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1. Drill Core RQD (%)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Text("${rqd.toInt()}%", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                Slider(
                    value = rqd.toFloat(),
                    onValueChange = { rqd = it.toDouble() },
                    valueRange = 10f..100f,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Selectors for other variables
        QItemSelector(title = "2. Joint Set Number (Jn)", options = jnOptions, values = jnValues, selectedIndex = jnSelected) { jnSelected = it }
        QItemSelector(title = "3. Joint Roughness Number (Jr)", options = jrOptions, values = jrValues, selectedIndex = jrSelected) { jrSelected = it }
        QItemSelector(title = "4. Joint Alteration Number (Ja)", options = jaOptions, values = jaValues, selectedIndex = jaSelected) { jaSelected = it }
        QItemSelector(title = "5. Joint Water Reduction (Jw)", options = jwOptions, values = jwValues, selectedIndex = jwSelected) { jwSelected = it }
        QItemSelector(title = "6. Stress Reduction Factor (SRF)", options = srfOptions, values = srfValues, selectedIndex = srfSelected) { srfSelected = it }
    }
}

@Composable
fun QItemSelector(
    title: String,
    options: List<OptionItem>,
    values: List<Double>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = options[selectedIndex].text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "${values[selectedIndex]}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (expanded) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Column(modifier = Modifier.padding(8.dp)) {
                    options.forEachIndexed { index, option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (index == selectedIndex) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                .clickable {
                                    onSelect(index)
                                    expanded = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.text,
                                fontSize = 12.sp,
                                color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${values[index]}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
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
fun StructuralConverterSection(viewModel: GeologyViewModel) {
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Standard Converter, 1 = Rose Diagram Analysis

    var strikeStr by remember { mutableStateOf("") }
    var dipStr by remember { mutableStateOf("") }

    val strike = strikeStr.toDoubleOrNull() ?: 0.0
    val dip = dipStr.toDoubleOrNull() ?: 0.0

    // Calculations based on Right Hand and Left Hand Rules
    val dipDirectionRhr = (strike + 90.0) % 360.0
    val dipDirectionLhr = (strike + 270.0) % 360.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sub-Tab Pill Selectors
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val subTabs = listOf("Planar Converter", "Rose Diagram Analysis")
                subTabs.forEachIndexed { index, title ->
                    val isSelected = activeSubTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent)
                            .clickable { activeSubTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (activeSubTab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Structural Planar Orientation Converter",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter raw bedding, foliation, or joint strike & dip to compute true direction according to rule standards.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = strikeStr,
                                onValueChange = { strikeStr = it.take(5) },
                                label = { Text("Strike (0-360°)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = dipStr,
                                onValueChange = { dipStr = it.take(4) },
                                label = { Text("Dip (0-90°)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }

                // Stereo-Canvas Indicator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Circular Canvas indicator
                        Canvas(
                            modifier = Modifier
                                .size(110.dp)
                                .background(Color.Transparent)
                        ) {
                            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                            val radius = size.width / 2 - 8f

                            // Draw outer dial
                            drawCircle(
                                color = Color.LightGray.copy(alpha = 0.6f),
                                radius = radius,
                                center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(2f)
                            )

                            // Draw standard directions N, E, S, W
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                start = androidx.compose.ui.geometry.Offset(center.x, center.y - radius),
                                end = androidx.compose.ui.geometry.Offset(center.x, center.y + radius),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                start = androidx.compose.ui.geometry.Offset(center.x - radius, center.y),
                                end = androidx.compose.ui.geometry.Offset(center.x + radius, center.y),
                                strokeWidth = 1f
                            )

                            // Draw strike line
                            val strikeRad = Math.toRadians(strike - 90.0)
                            val strikeEndX = center.x + radius * Math.cos(strikeRad).toFloat()
                            val strikeEndY = center.y + radius * Math.sin(strikeRad).toFloat()
                            val strikeStartX = center.x - radius * Math.cos(strikeRad).toFloat()
                            val strikeStartY = center.y - radius * Math.sin(strikeRad).toFloat()

                            drawLine(
                                color = Color(0xFFC96A2B),
                                start = androidx.compose.ui.geometry.Offset(strikeStartX, strikeStartY),
                                end = androidx.compose.ui.geometry.Offset(strikeEndX, strikeEndY),
                                strokeWidth = 4f
                            )

                            // Draw dip direction arrow (RHR perpendicular to strike)
                            val dipRad = Math.toRadians(dipDirectionRhr - 90.0)
                            val arrowLength = radius * 0.75f
                            val arrowEndX = center.x + arrowLength * Math.cos(dipRad).toFloat()
                            val arrowEndY = center.y + arrowLength * Math.sin(dipRad).toFloat()

                            drawLine(
                                color = Color(0xFF0A84FF),
                                start = center,
                                end = androidx.compose.ui.geometry.Offset(arrowEndX, arrowEndY),
                                strokeWidth = 3f
                            )

                            // Mini arrowhead
                            val arrowheadRad1 = dipRad + Math.toRadians(145.0)
                            val arrowheadRad2 = dipRad - Math.toRadians(145.0)
                            val barb = 12f
                            drawLine(
                                color = Color(0xFF0A84FF),
                                start = androidx.compose.ui.geometry.Offset(arrowEndX, arrowEndY),
                                end = androidx.compose.ui.geometry.Offset(arrowEndX + barb * Math.cos(arrowheadRad1).toFloat(), arrowEndY + barb * Math.sin(arrowheadRad1).toFloat()),
                                strokeWidth = 3f
                            )
                            drawLine(
                                color = Color(0xFF0A84FF),
                                start = androidx.compose.ui.geometry.Offset(arrowEndX, arrowEndY),
                                end = androidx.compose.ui.geometry.Offset(arrowEndX + barb * Math.cos(arrowheadRad2).toFloat(), arrowEndY + barb * Math.sin(arrowheadRad2).toFloat()),
                                strokeWidth = 3f
                            )
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Inferred Metrics", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Column {
                                Text("Right-Hand Rule (RHR)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0A84FF))
                                Text(
                                    text = String.format(Locale.US, "%03d° / %02d° (Dip Dir/Dip)", dipDirectionRhr.toInt(), dip.toInt()),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column {
                                Text("Left-Hand Rule (LHR)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = String.format(Locale.US, "%03d° / %02d° (Dip Dir/Dip)", dipDirectionLhr.toInt(), dip.toInt()),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Rose Diagram Tab Content
            val strikes = remember { mutableStateListOf(30f, 45f, 45f, 110f, 110f, 120f, 150f) }
            var strikeInputText by remember { mutableStateOf("") }
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var importCount by remember { mutableStateOf<Int?>(null) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Interactive Discontinuity Rose Diagram",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Analyze joint set patterns, strike frequencies in 10° clusters, and calculate peak orientations. Generate high-resolution PDF surveys.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = strikeInputText,
                                onValueChange = { strikeInputText = it.filter { c -> c.isDigit() }.take(3) },
                                label = { Text("Strike Pattern (0-360°)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Button(
                                onClick = {
                                    val s = strikeInputText.toFloatOrNull()
                                    if (s != null && s in 0f..360f) {
                                        strikes.add(s)
                                        strikeInputText = ""
                                    }
                                },
                                modifier = Modifier.testTag("add_strike_button")
                            ) {
                                Text("+ Add")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val activeProj = viewModel.activeProject.value
                                            if (activeProj != null) {
                                                val dbObs = viewModel.dao.getObservationsForProjectSync(activeProj.id)
                                                val dbWm = viewModel.dao.getAllWindowMappingsSync()

                                                val listFromObs = dbObs.filter {
                                                    (it.observationType == "Fault" || it.observationType == "Joint" || it.observationType == "Structure" || it.observationType == "Vein") && it.strike != null
                                                }.map { it.strike!!.toFloat() }

                                                val listFromWm = dbWm.filter { it.dipDirection != null }.map {
                                                    ((it.dipDirection!! - 90f + 360f) % 360f)
                                                }

                                                val totalImports = listFromObs + listFromWm
                                                if (totalImports.isNotEmpty()) {
                                                    strikes.clear()
                                                    strikes.addAll(totalImports)
                                                    importCount = totalImports.size
                                                    Toast.makeText(context, "Successfully synced ${totalImports.size} structural points!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "No structural records found. Plotted default demo values.", Toast.LENGTH_LONG).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Please configure/select an active project.", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Sync issue: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                modifier = Modifier.weight(1f).testTag("import_structures_btn")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync DB Joints", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { strikes.clear() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                modifier = Modifier.weight(0.7f).testTag("clear_strikes_btn")
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (importCount != null) {
                            Text(
                                text = "✨ Plotting $importCount structural orientations from active project database.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Dynamic Canvas Plot card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "SYMMETRICAL STRIKE WEDGE PLOT (10° Bins)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val centerPx = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                                val radiusPx = size.width / 2 - 20f

                                val binsCount = IntArray(18)
                                for (s in strikes) {
                                    val norm = (s % 180 + 180) % 180
                                    val idx = (norm / 10).toInt().coerceIn(0, 17)
                                    binsCount[idx]++
                                }
                                val maxFreq = binsCount.maxOrNull()?.coerceAtLeast(1) ?: 1

                                // Rings
                                val rings = 4
                                for (i in 1..rings) {
                                    val r = radiusPx * (i.toFloat() / rings)
                                    drawCircle(
                                        color = Color.LightGray.copy(alpha = 0.3f),
                                        radius = r,
                                        center = centerPx,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
                                    )
                                }

                                // Axis rays every 30°
                                for (deg in 0 until 180 step 30) {
                                    val rad = Math.toRadians(deg.toDouble() - 90.0)
                                    val dx = radiusPx * Math.cos(rad).toFloat()
                                    val dy = radiusPx * Math.sin(rad).toFloat()
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.4f),
                                        start = androidx.compose.ui.geometry.Offset(centerPx.x - dx, centerPx.y - dy),
                                        end = androidx.compose.ui.geometry.Offset(centerPx.x + dx, centerPx.y + dy),
                                        strokeWidth = 1f
                                    )
                                }

                                // Wedges
                                for (i in 0 until 18) {
                                    val count = binsCount[i]
                                    if (count > 0) {
                                        val rWedge = radiusPx * (count.toFloat() / maxFreq)
                                        val startAngle = i * 10f - 90f // compass N offset
                                        val sweepAngle = 10f

                                        val rect = androidx.compose.ui.geometry.Rect(centerPx.x - rWedge, centerPx.y - rWedge, centerPx.x + rWedge, centerPx.y + rWedge)

                                        val path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(centerPx.x, centerPx.y)
                                            arcTo(rect, startAngle, sweepAngle, false)
                                            close()
                                        }
                                        drawPath(path, Color(0xFF0A84FF).copy(alpha = 0.45f))
                                        drawPath(path, Color(0xFF0A84FF), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))

                                        // Opposite symmetric sector
                                        val oppPath = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(centerPx.x, centerPx.y)
                                            arcTo(rect, startAngle + 180f, sweepAngle, false)
                                            close()
                                        }
                                        drawPath(oppPath, Color(0xFF0A84FF).copy(alpha = 0.45f))
                                        drawPath(oppPath, Color(0xFF0A84FF), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                                    }
                                }

                                // Direct ticks
                                drawContext.canvas.nativeCanvas.apply {
                                    val nativePaint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.GRAY
                                        textSize = 10.dp.toPx()
                                        typeface = android.graphics.Typeface.MONOSPACE
                                    }
                                    drawText("0°/360° N", centerPx.x - 22.dp.toPx(), centerPx.y - radiusPx - 5.dp.toPx(), nativePaint)
                                    drawText("180° S", centerPx.x - 15.dp.toPx(), centerPx.y + radiusPx + 15.dp.toPx(), nativePaint)
                                    drawText("90° E", centerPx.x + radiusPx + 5.dp.toPx(), centerPx.y + 4.dp.toPx(), nativePaint)
                                    drawText("270° W", centerPx.x - radiusPx - 38.dp.toPx(), centerPx.y + 4.dp.toPx(), nativePaint)
                                }
                            }
                        }

                        // Listed strike tags
                        Text("Active Mapped Trends (${strikes.size} points):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(strikes) { strikeVal ->
                                OutlinedCard(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("${strikeVal.toInt()}°", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(14.dp).clickable { strikes.remove(strikeVal) },
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                viewModel.exportRoseDiagramFiles(context, strikes)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().testTag("export_pdf_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Professional PDF & Image", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WindowMappingSection(viewModel: GeologyViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mappings by viewModel.allWindowMappings.collectAsStateWithLifecycle()
    var isCreatingNew by remember { mutableStateOf(false) }

    // State fields for creation
    var startX by remember { mutableStateOf("") }
    var startY by remember { mutableStateOf("") }
    var startZ by remember { mutableStateOf("") }
    var endX by remember { mutableStateOf("") }
    var endY by remember { mutableStateOf("") }
    var endZ by remember { mutableStateOf("") }
    var lithology by remember { mutableStateOf("") }
    var discontinuityType by remember { mutableStateOf("Joint") }
    var dipStr by remember { mutableStateOf("") }
    var dipDirStr by remember { mutableStateOf("") }
    var jointSetNumbers by remember { mutableStateOf("") }
    var persistence by remember { mutableStateOf("") }
    var termination by remember { mutableStateOf("") }
    var spacingStr by remember { mutableStateOf("") }
    var apertureStr by remember { mutableStateOf("") }
    var surfaceShape by remember { mutableStateOf("Undulating") }
    var surfaceRoughness by remember { mutableStateOf("Rough") }
    var natureOfFilling by remember { mutableStateOf("") }
    var wavelengthStr by remember { mutableStateOf("") }
    var waveAmplitudeStr by remember { mutableStateOf("") }
    var jrcStr by remember { mutableStateOf("") }
    var jcsStr by remember { mutableStateOf("") }
    var weatheringCondition by remember { mutableStateOf("Slightly Weathered") }
    var waterFlow by remember { mutableStateOf("Dry") }
    var sampleNo by remember { mutableStateOf("") }
    var attachedPhotoPath by remember { mutableStateOf<String?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        val path = PhotoProcessor.saveAnnotatedImage(
                            context = context,
                            baseBitmap = bitmap,
                            stationCode = "WINDOW_MAPPING",
                            lat = 0.0,
                            lon = 0.0,
                            elevation = 0.0,
                            utmZone = "39N",
                            utmEasting = 0.0,
                            utmNorthing = 0.0,
                            azimuth = 0,
                            timestamp = System.currentTimeMillis(),
                            settings = PhotoProcessor.OverlaySettings(isEnabled = false)
                        )
                        attachedPhotoPath = path
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (isCreatingNew) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Form Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Window Mapping",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = {
                        // Autofill realistic mock survey readings
                        startX = "535310.2"
                        startY = "3954201.8"
                        startZ = "1245.5"
                        endX = "535314.5"
                        endY = "3954203.2"
                        endZ = "1246.0"
                        lithology = listOf("Andesite", "Basalt", "Limestone", "Sandstone").random()
                        dipStr = (30..80).random().toString()
                        dipDirStr = (0..359).random().toString()
                        jointSetNumbers = "J${(1..3).random()}"
                        persistence = "2.4m"
                        termination = "Solid Rock"
                        spacingStr = "0.22"
                        apertureStr = "1.5"
                        natureOfFilling = "Calcite"
                        wavelengthStr = "1.2"
                        waveAmplitudeStr = "5.0"
                        jrcStr = "8.5"
                        jcsStr = "70.0"
                        sampleNo = "WS-${(101..199).random()}"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Construction, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-Fill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // SECTION 1: Coordination Start & End
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. Coordinates & Lithology", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startX,
                            onValueChange = { startX = it },
                            label = { Text("Start X", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = startY,
                            onValueChange = { startY = it },
                            label = { Text("Start Y", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = startZ,
                            onValueChange = { startZ = it },
                            label = { Text("Start Z", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = endX,
                            onValueChange = { endX = it },
                            label = { Text("End X", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endY,
                            onValueChange = { endY = it },
                            label = { Text("End Y", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endZ,
                            onValueChange = { endZ = it },
                            label = { Text("End Z", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = lithology,
                            onValueChange = { lithology = it },
                            label = { Text("Lithology", fontSize = 12.sp) },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sampleNo,
                            onValueChange = { sampleNo = it },
                            label = { Text("Sample No", fontSize = 12.sp) },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                    }
                }
            }

            // SECTION 2: Discontinuity Specs
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("2. Structural Geometry", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Discontinuity Type:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val types = listOf("Joint", "Bedding", "Fault", "Shear Zone", "Vein")
                        var expandedTypes by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { expandedTypes = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(discontinuityType)
                            }
                            DropdownMenu(expanded = expandedTypes, onDismissRequest = { expandedTypes = false }) {
                                types.forEach { type ->
                                    DropdownMenuItem(text = { Text(type) }, onClick = { discontinuityType = type; expandedTypes = false })
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dipStr,
                            onValueChange = { dipStr = it.take(3) },
                            label = { Text("Dip (0-90°)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = dipDirStr,
                            onValueChange = { dipDirStr = it.take(3) },
                            label = { Text("Dip Dir (0-360°)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = jointSetNumbers,
                            onValueChange = { jointSetNumbers = it },
                            label = { Text("Joint Set No", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            // SECTION 3: Geometry & Geotech parameters
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("3. Geotechnical Properties", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = spacingStr,
                            onValueChange = { spacingStr = it },
                            label = { Text("Spacing (m)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = apertureStr,
                            onValueChange = { apertureStr = it },
                            label = { Text("Aperture (mm)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = persistence,
                            onValueChange = { persistence = it },
                            label = { Text("Persistence", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = termination,
                            onValueChange = { termination = it },
                            label = { Text("Termination", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            // SECTION 4: Roughness, JRC & JCS
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("4. Profile Roughness & Strength", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Surface Shape
                        var expShape by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = surfaceShape,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Surface Shape", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().clickable { expShape = true },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.clickable { expShape = true }) }
                            )
                            DropdownMenu(expanded = expShape, onDismissRequest = { expShape = false }) {
                                listOf("Planar", "Undulating", "Stepped").forEach { sh ->
                                    DropdownMenuItem(text = { Text(sh) }, onClick = { surfaceShape = sh; expShape = false })
                                }
                            }
                        }

                        // Surface Roughness
                        var expRough by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = surfaceRoughness,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Surface Roughness", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().clickable { expRough = true },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.clickable { expRough = true }) }
                            )
                            DropdownMenu(expanded = expRough, onDismissRequest = { expRough = false }) {
                                listOf("Very Rough", "Rough", "Slightly Rough", "Smooth", "Slickensided").forEach { r ->
                                    DropdownMenuItem(text = { Text(r) }, onClick = { surfaceRoughness = r; expRough = false })
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = jrcStr,
                            onValueChange = { jrcStr = it },
                            label = { Text("JRC (0-20)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = jcsStr,
                            onValueChange = { jcsStr = it },
                            label = { Text("JCS (MPa)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = wavelengthStr,
                            onValueChange = { wavelengthStr = it },
                            label = { Text("Wavelength (m)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = waveAmplitudeStr,
                            onValueChange = { waveAmplitudeStr = it },
                            label = { Text("Wave Amp (cm)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = natureOfFilling,
                        onValueChange = { natureOfFilling = it },
                        label = { Text("Nature of Filling") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // SECTION 5: Weathering & Water Flow
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("5. Weathering & Water Flow", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Weathering
                        var expWeather by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = weatheringCondition,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Weathering Condition", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().clickable { expWeather = true },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.clickable { expWeather = true }) }
                            )
                            DropdownMenu(expanded = expWeather, onDismissRequest = { expWeather = false }) {
                                listOf("Unweathered", "Slightly Weathered", "Moderately Weathered", "Highly Weathered", "Completely Decomposed").forEach { w ->
                                    DropdownMenuItem(text = { Text(w) }, onClick = { weatheringCondition = w; expWeather = false })
                                }
                            }
                        }

                        // Water flow
                        var expWater by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = waterFlow,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Water Flow", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().clickable { expWater = true },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.clickable { expWater = true }) }
                            )
                            DropdownMenu(expanded = expWater, onDismissRequest = { expWater = false }) {
                                listOf("Dry", "Damp", "Wet", "Dripping", "Flowing").forEach { f ->
                                    DropdownMenuItem(text = { Text(f) }, onClick = { waterFlow = f; expWater = false })
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 6: Camera & Photo attachment
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("6. Outcrop Sketch Photo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    
                    if (attachedPhotoPath != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(
                                model = attachedPhotoPath,
                                contentDescription = "Observation Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { attachedPhotoPath = null },
                                modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No Photo attached", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { pickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Local Gallery", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                // Simulate geological photo
                                val mockBmp = PhotoProcessor.generateMockOutcropBitmap(context, "WIN")
                                val path = PhotoProcessor.saveAnnotatedImage(
                                    context = context,
                                    baseBitmap = mockBmp,
                                    stationCode = "WINDOW-MAPPING-SCAN",
                                    lat = 35.68,
                                    lon = 51.38,
                                    elevation = 1180.0,
                                    utmZone = "39N",
                                    utmEasting = 534500.0,
                                    utmNorthing = 3950000.0,
                                    azimuth = 90,
                                    timestamp = System.currentTimeMillis(),
                                    settings = PhotoProcessor.OverlaySettings(isEnabled = true, position = "BOTTOM_LEFT")
                                )
                                attachedPhotoPath = path
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simulate", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Bottom action bar
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { isCreatingNew = false },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val parsedStartX = startX.toDoubleOrNull() ?: 0.0
                        val parsedStartY = startY.toDoubleOrNull() ?: 0.0
                        val parsedStartZ = startZ.toDoubleOrNull() ?: 0.0
                        val parsedEndX = endX.toDoubleOrNull() ?: 0.0
                        val parsedEndY = endY.toDoubleOrNull() ?: 0.0
                        val parsedEndZ = endZ.toDoubleOrNull() ?: 0.0
                        val parsedDip = dipStr.toIntOrNull() ?: 0
                        val parsedDipDir = dipDirStr.toIntOrNull() ?: 0
                        val parsedSpacing = spacingStr.toDoubleOrNull() ?: 0.0
                        val parsedAperture = apertureStr.toDoubleOrNull() ?: 0.0
                        val parsedWavelength = wavelengthStr.toDoubleOrNull() ?: 0.0
                        val parsedAmplitude = waveAmplitudeStr.toDoubleOrNull() ?: 0.0
                        val parsedJrc = jrcStr.toDoubleOrNull() ?: 0.0
                        val parsedJcs = jcsStr.toDoubleOrNull() ?: 0.0

                        val record = WindowMapping(
                            startX = parsedStartX,
                            startY = parsedStartY,
                            startZ = parsedStartZ,
                            endX = parsedEndX,
                            endY = parsedEndY,
                            endZ = parsedEndZ,
                            lithology = lithology.ifBlank { "Unspecified Lithology" },
                            discontinuityType = discontinuityType,
                            dip = parsedDip,
                            dipDirection = parsedDipDir,
                            jointSetNumbers = jointSetNumbers.ifBlank { "N/A" },
                            persistence = persistence.ifBlank { "N/A" },
                            termination = termination.ifBlank { "N/A" },
                            spacing = parsedSpacing,
                            aperture = parsedAperture,
                            surfaceShape = surfaceShape,
                            surfaceRoughness = surfaceRoughness,
                            natureOfFilling = natureOfFilling.ifBlank { "None" },
                            wavelength = parsedWavelength,
                            waveAmplitude = parsedAmplitude,
                            jrc = parsedJrc,
                            jcs = parsedJcs,
                            weatheringCondition = weatheringCondition,
                            waterFlow = waterFlow,
                            sampleNo = sampleNo.ifBlank { "WS-${System.currentTimeMillis().toString().takeLast(4)}" },
                            photoPath = attachedPhotoPath
                        )
                        viewModel.saveWindowMapping(record)
                        isCreatingNew = false
                        android.widget.Toast.makeText(context, "Window mapping recorded successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Save Record", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Window Mapping Profile", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text("${mappings.size} surveys registered", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.exportWindowMappingsCsv(context) },
                        modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    ) {
                        Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                    }
                    Button(
                        onClick = { isCreatingNew = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            if (mappings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Construction, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(0.3f))
                        Text("No window mapping records found.", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tap Add to survey active scanline window profiles", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { isCreatingNew = true }) {
                            Text("Record First Window Survey")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(mappings) { m ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Title badge & Delete
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Sample: #${m.sampleNo}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = m.lithology,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteWindowMapping(m.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Start & End location dual badge
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Start Point:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = String.format(Locale.US, "X: %.1f, Y: %.1f, Z: %.1f", m.startX, m.startY, m.startZ),
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("End Point:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = String.format(Locale.US, "X: %.1f, Y: %.1f, Z: %.1f", m.endX, m.endY, m.endZ),
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                // Highlight Geometry and Set
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Structural Geometry", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${m.discontinuityType} Dip/Dir: ${m.dip}° / ${m.dipDirection}°",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Joint Set Number", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = m.jointSetNumbers,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                                // Compact specs Grid (4 pillars layout)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        SpecColumn(label = "Spacing", value = "${m.spacing} m", modifier = Modifier.weight(1f))
                                        SpecColumn(label = "Aperture", value = "${m.aperture} mm", modifier = Modifier.weight(1f))
                                        SpecColumn(label = "JRC Index", value = "${m.jrc}", modifier = Modifier.weight(1f))
                                        SpecColumn(label = "JCS", value = "${m.jcs} MPa", modifier = Modifier.weight(1f))
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        SpecColumn(label = "Persistence", value = m.persistence, modifier = Modifier.weight(1f))
                                        SpecColumn(label = "Termination", value = m.termination, modifier = Modifier.weight(1f))
                                        SpecColumn(label = "Surface Shape", value = m.surfaceShape, modifier = Modifier.weight(1f))
                                        SpecColumn(label = "Roughness", value = m.surfaceRoughness, modifier = Modifier.weight(1f))
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        SpecColumn(label = "Wavelength", value = "${m.wavelength} m", modifier = Modifier.weight(1f))
                                        SpecColumn(label = "Wave Amp", value = "${m.waveAmplitude} cm", modifier = Modifier.weight(1f))
                                        SpecColumn(label = "Water flow", value = m.waterFlow, modifier = Modifier.weight(1f))
                                        SpecColumn(label = "Weathering", value = m.weatheringCondition, modifier = Modifier.weight(1f))
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (m.natureOfFilling.isNotBlank()) {
                                        Text(
                                            text = "Filling Nature: ${m.natureOfFilling}",
                                            fontSize = 11.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Photo attach
                                if (m.photoPath != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    ) {
                                        AsyncImage(
                                            model = m.photoPath,
                                            contentDescription = "Outcrop Scan Photo",
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

@Composable
fun SpecColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SettingsScreen(
    viewModel: GeologyViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val fontSizeMult by viewModel.fontSizeMultiplier.collectAsStateWithLifecycle()
    val fontFamilySel by viewModel.fontFamilySelection.collectAsStateWithLifecycle()
    val gpsAccMode by viewModel.gpsAccuracyMode.collectAsStateWithLifecycle()
    val themeSel by viewModel.themeSelection.collectAsStateWithLifecycle()
    val touchSizingProfile by viewModel.touchSizingProfile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Typography Customization
        Text(
            text = "Typography Customization",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Font Size Selector Row
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Font Size",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val sizeText = when (fontSizeMult) {
                            0.85f -> "Small"
                            1.0f -> "Normal"
                            1.15f -> "Large"
                            1.30f -> "Extra Large"
                            else -> "Normal"
                        }
                        Text(
                            text = sizeText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sizes = listOf(0.85f, 1.0f, 1.15f, 1.30f)
                        val titles = listOf("Small", "Normal", "Large", "X-Large")

                        sizes.forEachIndexed { idx, size ->
                            val isSelected = fontSizeMult == size
                            Button(
                                onClick = { viewModel.saveFontSizeMultiplier(size) },
                                modifier = Modifier.weight(1f).height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(titles[idx], fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Font Family Selector Row
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Font Family",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val families = listOf("SansSerif", "Monospace", "Serif")
                        val familyLabels = listOf("Sans (Default)", "Monospace", "Classic Serif")

                        families.forEachIndexed { idx, fam ->
                            val isSelected = fontFamilySel == fam
                            Button(
                                onClick = { viewModel.saveFontFamily(fam) },
                                modifier = Modifier.weight(1f).height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(familyLabels[idx], fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Live Typography Preview Card
        Text(
            text = "Live Font Preview",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Geological Core Sample Title",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Borehole interval logging and geological core logs under the supervision of the engineering technical board.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section: GPS and Battery Optimization
        Text(
            text = "GPS & Power Settings",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // High Accuracy Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.saveGpsAccuracyMode("HIGH_ACCURACY") }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = gpsAccMode == "HIGH_ACCURACY",
                        onClick = { viewModel.saveGpsAccuracyMode("HIGH_ACCURACY") },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "High Accuracy Mode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Deep satellite scan to acquire precise borehole collar coordinate logging under 2.5 meters accuracy. (High battery consumption)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Battery Saver Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.saveGpsAccuracyMode("BATTERY_SAVER") }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = gpsAccMode == "BATTERY_SAVER",
                        onClick = { viewModel.saveGpsAccuracyMode("BATTERY_SAVER") },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery Saver Mode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Economical satellite tracking with ultra-fast hardware shut-off after initial signal locking completes. (Standard accuracy)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Section: Visual Themes
        Text(
            text = "App Visual Theme",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val themes = listOf("DARK", "LIGHT", "OUTDOOR", "SYSTEM")
            val themeLabels = listOf("Cosmic Dark", "Light Mode", "Outdoor Contrast", "System")

            themes.forEachIndexed { idx, tSel ->
                val isSelected = themeSel == tSel
                Button(
                    onClick = { viewModel.saveThemeSelection(tSel) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Text(themeLabels[idx], fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        // Section: Customizable Touch Targets & Button Sizing Profile
        Text(
            text = "Tactical Sizing & Button Spacing",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val profiles = listOf("STANDARD", "LARGE", "GLOVE_FRIENDLY")
            val profileLabels = listOf("Standard (Auto)", "Comfort (56dp)", "Glove Mode (64dp)")

            profiles.forEachIndexed { idx, prof ->
                val isSelected = touchSizingProfile == prof
                Button(
                    onClick = { viewModel.saveTouchSizingProfile(prof) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Text(profileLabels[idx], fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save and close
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save & Return", fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationOfflineMapScreen(
    viewModel: GeologyViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val stations by viewModel.allStations.collectAsStateWithLifecycle(emptyList())
    val boreholes by viewModel.boreholes.collectAsStateWithLifecycle(emptyList())
    val tracks by viewModel.allTracks.collectAsStateWithLifecycle(emptyList())
    val waypoints by viewModel.allWaypoints.collectAsStateWithLifecycle(emptyList())
    val gpsState by viewModel.gpsState.collectAsStateWithLifecycle()

    // Layer status toggles
    var showStationsLayer by remember { mutableStateOf(true) }
    var showBoreholesLayer by remember { mutableStateOf(true) }
    var showWaypointsLayer by remember { mutableStateOf(true) }
    var showTracksLayer by remember { mutableStateOf(true) }

    // MapView reference for controller center actions
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var hasCentered by remember { mutableStateOf(false) }
    var userGpsMarker by remember { mutableStateOf<Marker?>(null) }

    // Live Track Recording session states
    var isRecording by remember { mutableStateOf(false) }
    val activeTrackPoints = remember { mutableStateListOf<GeoPoint>() }
    var showSaveTrackDialog by remember { mutableStateOf(false) }
    var newTrackName by remember { mutableStateOf("") }

    // Custom Waypoint generation dialog states
    var showAddWaypointDialog by remember { mutableStateOf(false) }
    var wpName by remember { mutableStateOf("") }
    var wpDesc by remember { mutableStateOf("") }
    var wpCategory by remember { mutableStateOf("Outcrop") }
    var wpInputMode by remember { mutableStateOf("GPS") } // "GPS", "CENTER", "MANUAL"
    var manualLatStr by remember { mutableStateOf("") }
    var manualLonStr by remember { mutableStateOf("") }

    // Survey History Manager Slide-up tab
    var showHistoryHub by remember { mutableStateOf(false) }
    var selectedHistoryTab by remember { mutableStateOf(0) } // 0 = Waypoints, 1 = Tracks

    // Clicked marker state
    var selectedItemType by remember { mutableStateOf<String?>(null) } // "Station", "Borehole", "Waypoint", "Track"
    var selectedItemId by remember { mutableStateOf<Int?>(null) }
    var selectedItemTitle by remember { mutableStateOf<String?>(null) }
    var selectedItemSub by remember { mutableStateOf<String?>(null) }

    // Cyclic color palette for geological tracks rendering
    val trackColors = remember {
        listOf(
            android.graphics.Color.rgb(22, 160, 133),  // Teal
            android.graphics.Color.rgb(155, 89, 182), // Purple
            android.graphics.Color.rgb(41, 128, 185), // Blue
            android.graphics.Color.rgb(230, 126, 34), // Orange
            android.graphics.Color.rgb(241, 196, 15)  // Yellow
        )
    }

    // Helper functions for points parsing & serialization
    fun parsePoints(pointsJson: String): List<GeoPoint> {
        if (pointsJson.isBlank()) return emptyList()
        return pointsJson.split(";").mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) {
                val lat = parts[0].toDoubleOrNull()
                val lon = parts[1].toDoubleOrNull()
                if (lat != null && lon != null) GeoPoint(lat, lon) else null
            } else null
        }
    }

    // Reactive Track Recording effect
    LaunchedEffect(isRecording, gpsState.latitude, gpsState.longitude) {
        if (isRecording) {
            val myLat = gpsState.latitude
            val myLon = gpsState.longitude
            if (myLat != 0.0 && myLon != 0.0) {
                val lastPt = activeTrackPoints.lastOrNull()
                if (lastPt == null || lastPt.latitude != myLat || lastPt.longitude != myLon) {
                    activeTrackPoints.add(GeoPoint(myLat, myLon))
                }
            }
        }
    }

    // Initialize osmdroid configuration synchronously before MapView compiles to prevent race conditions & crashes
    remember {
        val config = org.osmdroid.config.Configuration.getInstance()
        config.userAgentValue = context.packageName
        val basePath = context.cacheDir.absolutePath + "/osmdroid"
        config.osmdroidBasePath = java.io.File(basePath)
        config.osmdroidTileCache = java.io.File(basePath, "tiles")
        config.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        true
    }

    // Lifecycle observer for MapView to prevent thread & memory leaks
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapViewRef) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    mapViewRef?.onPause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onPause()
            mapViewRef?.onDetach()
        }
    }

    // Performance Optimization: Render overlays dynamically only when lists or layers alter
    LaunchedEffect(
        mapViewRef, stations, boreholes, tracks, waypoints, activeTrackPoints.size,
        showStationsLayer, showBoreholesLayer, showWaypointsLayer, showTracksLayer
    ) {
        val mapView = mapViewRef ?: return@LaunchedEffect
        mapView.overlays.clear()

        // 1. Render field stations
        if (showStationsLayer) {
            stations.forEach { station ->
                if (station.latitude != 0.0 && station.longitude != 0.0) {
                    val marker = Marker(mapView).apply {
                        id = "station_${station.id}"
                        position = GeoPoint(station.latitude, station.longitude)
                        title = "Station: ${station.stationCode}"
                        subDescription = "Elevation: ${station.elevation}m\nUTM Zone: ${station.utmZone}"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { m, _ ->
                            selectedItemType = "Station"
                            selectedItemId = station.id
                            selectedItemTitle = station.stationCode
                            selectedItemSub = "Elevation: ${station.elevation}m • Accuracy: ${station.accuracy}m\nUTM Zone: ${station.utmZone} (${station.utmEasting.toInt()}E, ${station.utmNorthing.toInt()}N)"
                            m.showInfoWindow()
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
            }
        }

        // 2. Render boreholes
        if (showBoreholesLayer) {
            boreholes.forEach { borehole ->
                if (borehole.latitude != 0.0 && borehole.longitude != 0.0) {
                    val marker = Marker(mapView).apply {
                        id = "borehole_${borehole.id}"
                        position = GeoPoint(borehole.latitude, borehole.longitude)
                        title = "Borehole: ${borehole.name}"
                        subDescription = "UTM: ${borehole.utmEasting} E, ${borehole.utmNorthing} N\nAzimuth: ${borehole.azimuth ?: 0.0}°"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { m, _ ->
                            selectedItemType = "Borehole"
                            selectedItemId = borehole.id
                            selectedItemTitle = borehole.name
                            selectedItemSub = "Elevation: ${borehole.elevation}m • Azimuth: ${borehole.azimuth ?: 0.0}°\nUTM Zone: ${borehole.utmZone} (${borehole.utmEasting.toInt()}E, ${borehole.utmNorthing.toInt()}N)"
                            m.showInfoWindow()
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
            }
        }

        // 3. Render Custom Field Waypoints (Waypoints Layer)
        if (showWaypointsLayer) {
            waypoints.forEach { wp ->
                if (wp.latitude != 0.0 && wp.longitude != 0.0) {
                    val marker = Marker(mapView).apply {
                        id = "waypoint_${wp.id}"
                        position = GeoPoint(wp.latitude, wp.longitude)
                        title = wp.name
                        subDescription = "Category: ${wp.category}\nElev: ${wp.elevation}m • Accuracy: ${wp.accuracy}m\nNotes: ${wp.description}"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { m, _ ->
                            selectedItemType = "Waypoint"
                            selectedItemId = wp.id
                            selectedItemTitle = wp.name
                            selectedItemSub = "Category: [${wp.category}] • Accuracy: ${String.format(Locale.US, "%.1f", wp.accuracy)}m\nLatitude: ${String.format(Locale.US, "%.6f", wp.latitude)}°\nLongitude: ${String.format(Locale.US, "%.6f", wp.longitude)}°\nNotes: ${wp.description}"
                            m.showInfoWindow()
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
            }
        }

        // 4. Render Saved Geological Track Polylines
        if (showTracksLayer) {
            tracks.forEachIndexed { idx, track ->
                val trackPts = parsePoints(track.pointsJson)
                if (trackPts.isNotEmpty()) {
                    val polyline = org.osmdroid.views.overlay.Polyline(mapView).apply {
                        setPoints(trackPts)
                        outlinePaint.color = trackColors[idx % trackColors.size]
                        outlinePaint.strokeWidth = 5.0f
                    }
                    mapView.overlays.add(polyline)
                }
            }
        }

        // 5. Render Active Live Recording Polyline path
        if (isRecording && activeTrackPoints.isNotEmpty()) {
            val livePolyline = org.osmdroid.views.overlay.Polyline(mapView).apply {
                setPoints(activeTrackPoints.toList())
                outlinePaint.color = android.graphics.Color.rgb(231, 76, 60) // High visibility red
                outlinePaint.strokeWidth = 7.0f
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            }
            mapView.overlays.add(livePolyline)
        }

        // Force recreation of tracking marker since the stack was cleared
        userGpsMarker = null

        // Auto center map on initial data load
        if (!hasCentered) {
            val validStations = stations.filter { it.latitude != 0.0 && it.longitude != 0.0 }
            val validBoreholes = boreholes.filter { it.latitude != 0.0 && it.longitude != 0.0 }
            val centerPoint = if (validStations.isNotEmpty()) {
                val avgLat = validStations.map { it.latitude }.average()
                val avgLon = validStations.map { it.longitude }.average()
                GeoPoint(avgLat, avgLon)
            } else if (validBoreholes.isNotEmpty()) {
                val avgLat = validBoreholes.map { it.latitude }.average()
                val avgLon = validBoreholes.map { it.longitude }.average()
                GeoPoint(avgLat, avgLon)
            } else {
                null
            }
            if (centerPoint != null) {
                mapView.controller.setCenter(centerPoint)
                mapView.controller.setZoom(10.0)
                hasCentered = true
            }
        }

        mapView.invalidate()
    }

    // Dynamic Tracking Effect: Smooth real-time coordinate updates
    LaunchedEffect(mapViewRef, gpsState.latitude, gpsState.longitude, gpsState.accuracy, gpsState.elevation, userGpsMarker) {
        val mapView = mapViewRef ?: return@LaunchedEffect
        val myLat = gpsState.latitude
        val myLon = gpsState.longitude
        if (myLat != 0.0 && myLon != 0.0) {
            val currentMarker = userGpsMarker
            if (currentMarker != null && mapView.overlays.contains(currentMarker)) {
                currentMarker.position = GeoPoint(myLat, myLon)
                currentMarker.subDescription = "Accuracy: ${String.format(Locale.US, "%.1f", gpsState.accuracy)}m\nElev: ${gpsState.elevation}m"
                mapView.invalidate()
            } else {
                val newMarker = Marker(mapView).apply {
                    id = "user_gps_location"
                    position = GeoPoint(myLat, myLon)
                    title = "My Real-Time Position"
                    subDescription = "Accuracy: ${String.format(Locale.US, "%.1f", gpsState.accuracy)}m\nElev: ${gpsState.elevation}m"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { m, _ ->
                        selectedItemType = "My GPS"
                        selectedItemId = -99
                        selectedItemTitle = "Active GNSS GPS Receiver"
                        selectedItemSub = "Latitude: ${String.format(Locale.US, "%.6f", myLat)}°\nLongitude: ${String.format(Locale.US, "%.6f", myLon)}°\nAccuracy: ${String.format(Locale.US, "%.1f", gpsState.accuracy)} meters\nElevation: ${gpsState.elevation} meters"
                        m.showInfoWindow()
                        true
                    }
                }
                mapView.overlays.add(newMarker)
                userGpsMarker = newMarker
                mapView.invalidate()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline GPS Tracker & Surveyor Map", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHistoryHub = !showHistoryHub },
                        modifier = Modifier.testTag("survey_history_hub_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History, 
                            contentDescription = "History Hub", 
                            tint = if (showHistoryHub) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Android MapView Component
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        
                        val controller = controller
                        controller.setZoom(8.5)
                        
                        val validStations = stations.filter { it.latitude != 0.0 && it.longitude != 0.0 }
                        val validBoreholes = boreholes.filter { it.latitude != 0.0 && it.longitude != 0.0 }

                        val centerPoint = if (validStations.isNotEmpty()) {
                            val avgLat = validStations.map { it.latitude }.average()
                            val avgLon = validStations.map { it.longitude }.average()
                            GeoPoint(avgLat, avgLon)
                        } else if (validBoreholes.isNotEmpty()) {
                            val avgLat = validBoreholes.map { it.latitude }.average()
                            val avgLon = validBoreholes.map { it.longitude }.average()
                            GeoPoint(avgLat, avgLon)
                        } else {
                            GeoPoint(32.4279, 53.6880) // General center
                        }
                        controller.setCenter(centerPoint)
                        mapViewRef = this
                    }
                },
                update = { _ -> },
                onRelease = { mapView ->
                    mapView.onPause()
                    mapView.onDetach()
                }
            )

            // Dynamic Live Route Logging HUD Card
            if (isRecording) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFC0392B)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.White, CircleShape)
                        )
                        Text(
                            text = "RECORDING SURVEY ROUTE • ${activeTrackPoints.size} PTS LOGGED",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Layer Toggle HUD Card (Glassmorphic Top Right)
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .width(220.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SURVEY LAYER SETTINGS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC96A2B),
                        letterSpacing = 0.8.sp
                    )
                    
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    
                    // Field Stations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Field Stations", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = showStationsLayer,
                            onCheckedChange = { showStationsLayer = it },
                            modifier = Modifier.scale(0.65f)
                        )
                    }

                    // Boreholes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Architecture, null, modifier = Modifier.size(15.dp), tint = Color(0xFF0A84FF))
                            Text("Boreholes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = showBoreholesLayer,
                            onCheckedChange = { showBoreholesLayer = it },
                            modifier = Modifier.scale(0.65f)
                        )
                    }

                    // Field Waypoints
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AddLocation, null, modifier = Modifier.size(15.dp), tint = Color(0xFFE67E22))
                            Text("Waypoints", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = showWaypointsLayer,
                            onCheckedChange = { showWaypointsLayer = it },
                            modifier = Modifier.scale(0.65f)
                        )
                    }

                    // Track routes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Explore, null, modifier = Modifier.size(15.dp), tint = Color(0xFF2ECC71))
                            Text("Tracks & Routes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = showTracksLayer,
                            onCheckedChange = { showTracksLayer = it },
                            modifier = Modifier.scale(0.65f)
                        )
                    }
                }
            }

            // GIS Controller Button Stack on Right-Center
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pin Point / Waypoint logger
                FloatingActionButton(
                    onClick = {
                        wpName = "Waypoint ${waypoints.size + 1}"
                        wpDesc = ""
                        wpCategory = "Outcrop"
                        wpInputMode = "GPS"
                        manualLatStr = if (gpsState.latitude != 0.0) gpsState.latitude.toString() else ""
                        manualLonStr = if (gpsState.longitude != 0.0) gpsState.longitude.toString() else ""
                        showAddWaypointDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddLocation, contentDescription = "Add Custom Waypoint", modifier = Modifier.size(20.dp))
                }

                // Track recorder (Toggle Start/Stop)
                FloatingActionButton(
                    onClick = {
                        if (!isRecording) {
                            activeTrackPoints.clear()
                            isRecording = true
                            Toast.makeText(context, "GPS Route Recording Started", Toast.LENGTH_SHORT).show()
                        } else {
                            if (activeTrackPoints.isEmpty()) {
                                isRecording = false
                                Toast.makeText(context, "Route recording canceled (no coordinates points logged)", Toast.LENGTH_SHORT).show()
                            } else {
                                newTrackName = "Route Trace ${tracks.size + 1}"
                                showSaveTrackDialog = true
                            }
                        }
                    },
                    containerColor = if (isRecording) Color(0xFFC0392B) else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isRecording) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Track Recorder",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Locate Me
                FloatingActionButton(
                    onClick = {
                        val lat = gpsState.latitude
                        val lon = gpsState.longitude
                        if (lat != 0.0 && lon != 0.0) {
                            mapViewRef?.let { map ->
                                map.controller.animateTo(GeoPoint(lat, lon))
                                map.controller.setZoom(15.0)
                            }
                            Toast.makeText(context, "Centering map on offline GPS location", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Waiting for GPS signal / location permission", Toast.LENGTH_SHORT).show()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Locate Me", modifier = Modifier.size(20.dp))
                }

                // Fit Points
                FloatingActionButton(
                    onClick = {
                        val validStations = stations.filter { it.latitude != 0.0 && it.longitude != 0.0 }
                        val validBoreholes = boreholes.filter { it.latitude != 0.0 && it.longitude != 0.0 }
                        val validWps = waypoints.filter { it.latitude != 0.0 && it.longitude != 0.0 }

                        val centerPoint = if (validStations.isNotEmpty()) {
                            val avgLat = validStations.map { it.latitude }.average()
                            val avgLon = validStations.map { it.longitude }.average()
                            GeoPoint(avgLat, avgLon)
                        } else if (validBoreholes.isNotEmpty()) {
                            val avgLat = validBoreholes.map { it.latitude }.average()
                            val avgLon = validBoreholes.map { it.longitude }.average()
                            GeoPoint(avgLat, avgLon)
                        } else if (validWps.isNotEmpty()) {
                            val avgLat = validWps.map { it.latitude }.average()
                            val avgLon = validWps.map { it.longitude }.average()
                            GeoPoint(avgLat, avgLon)
                        } else {
                            null
                        }

                        if (centerPoint != null) {
                            mapViewRef?.let { map ->
                                map.controller.animateTo(centerPoint)
                                map.controller.setZoom(10.0)
                            }
                        } else {
                            Toast.makeText(context, "No features recorded to compile fit alignment", Toast.LENGTH_SHORT).show()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Fit Project Points", modifier = Modifier.size(20.dp))
                }
            }

            // Waypoints & Tracks Survey History Drawer Card
            if (showHistoryHub) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .width(310.dp)
                        .height(340.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Hub Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Saved Survey Data Hub", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            IconButton(
                                onClick = { showHistoryHub = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Tab Switchers
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PresetChip(
                                label = "WAYPOINTS (${waypoints.size})",
                                isSelected = selectedHistoryTab == 0,
                                onClick = { selectedHistoryTab = 0 }
                            )
                            PresetChip(
                                label = "ROUTE TRACKS (${tracks.size})",
                                isSelected = selectedHistoryTab == 1,
                                onClick = { selectedHistoryTab = 1 }
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        if (selectedHistoryTab == 0) {
                            // Waypoints scrolling list
                            if (waypoints.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No custom waypoints added.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(waypoints.size) { index ->
                                        val wp = waypoints[index]
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    mapViewRef?.let { map ->
                                                        map.controller.animateTo(GeoPoint(wp.latitude, wp.longitude))
                                                        map.controller.setZoom(15.0)
                                                    }
                                                    selectedItemType = "Waypoint"
                                                    selectedItemId = wp.id
                                                    selectedItemTitle = wp.name
                                                    selectedItemSub = "Category: [${wp.category}] • Accuracy: ${wp.accuracy}m\nLatitude: ${wp.latitude}\nLongitude: ${wp.longitude}\nNotes: ${wp.description}"
                                                }.padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(wp.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("${wp.category} • Lat: ${String.format(Locale.US, "%.4f", wp.latitude)}, Lon: ${String.format(Locale.US, "%.4f", wp.longitude)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteWaypoint(wp.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Color(0xFFC0392B))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Tracks scrolling list
                            if (tracks.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No recorded tracks found.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(tracks.size) { index ->
                                        val trk = tracks[index]
                                        val pts = parsePoints(trk.pointsJson)
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    val firstPt = pts.firstOrNull()
                                                    if (firstPt != null) {
                                                        mapViewRef?.let { map ->
                                                            map.controller.animateTo(firstPt)
                                                            map.controller.setZoom(14.0)
                                                        }
                                                        selectedItemType = "Track"
                                                        selectedItemId = trk.id
                                                        selectedItemTitle = trk.name
                                                        selectedItemSub = "GPS Logged Route Path\nPoints Density: ${pts.size} GPS nodes\nTotal Nodes: ${pts.size}"
                                                    }
                                                }.padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(trk.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("${pts.size} trace points collected", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteTrack(trk.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Color(0xFFC0392B))
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

            // Slide-up Details Interactive Overlay Card for clicked markers
            if (selectedItemType != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, Color(0xFFC96A2B).copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val indicatorColor = if (selectedItemType == "Station") MaterialTheme.colorScheme.primary else if (selectedItemType == "Borehole") Color(0xFF0A84FF) else Color(0xFFE67E22)
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(indicatorColor, RoundedCornerShape(5.dp))
                                )
                                Text(
                                    text = (selectedItemType ?: "").uppercase(Locale.US),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = indicatorColor
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedItemType = null
                                    selectedItemId = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(
                            text = selectedItemTitle ?: "",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = selectedItemSub ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        if (selectedItemType == "Station" || selectedItemType == "Borehole") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    val id = selectedItemId
                                    if (id != null) {
                                        if (selectedItemType == "Station") {
                                            viewModel.navigateTo(Screen.StationDetail(id))
                                        } else {
                                            viewModel.navigateTo(Screen.BoreholeDashboard(id))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC96A2B))
                            ) {
                                Icon(Icons.Default.Map, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Details Screen", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // A. DIALOG: Custom Waypoint Setup Dialog builder
    if (showAddWaypointDialog) {
        AlertDialog(
            onDismissRequest = { showAddWaypointDialog = false },
            title = { Text("Log Field Survey Waypoint", fontSize = 16.sp, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = wpName,
                        onValueChange = { wpName = it },
                        label = { Text("Waypoint Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Waypoint Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Outcrop", "Contact", "Hazard", "Water", "General").forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (wpCategory == cat) Color(0xFFC96A2B) else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { wpCategory = cat }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(cat, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (wpCategory == cat) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = wpDesc,
                        onValueChange = { wpDesc = it },
                        label = { Text("Field Description / Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Lock Coordinates Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetChip("GNSS GPS LOC", wpInputMode == "GPS") { wpInputMode = "GPS" }
                        PresetChip("MAP CENTER", wpInputMode == "CENTER") { wpInputMode = "CENTER" }
                        PresetChip("MANUAL OUT", wpInputMode == "MANUAL") { wpInputMode = "MANUAL" }
                    }

                    if (wpInputMode == "GPS") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("GNSS Coordinates Lock:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                if (gpsState.latitude != 0.0) {
                                    Text("Lat: ${String.format(Locale.US, "%.6f", gpsState.latitude)}°\nLon: ${String.format(Locale.US, "%.6f", gpsState.longitude)}° • ±${String.format(Locale.US, "%.1f", gpsState.accuracy)}m", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                } else {
                                    Text("No GNSS satellite coordinate fix available yet. Manual mode suggested.", fontSize = 11.sp, color = Color(0xFFC0392B), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (wpInputMode == "CENTER") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                val centerGeo = mapViewRef?.mapCenter
                                Text("Current Mapped Screen Center Coordinates:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                if (centerGeo != null) {
                                    Text("Lat: ${String.format(Locale.US, "%.6f", centerGeo.latitude)}°\nLon: ${String.format(Locale.US, "%.6f", centerGeo.longitude)}°", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                } else {
                                    Text("32.4279°, 53.6880° (Default Base center)", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualLatStr,
                                onValueChange = { manualLatStr = it },
                                label = { Text("Manual Latitude (Decimal)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = manualLonStr,
                                onValueChange = { manualLonStr = it },
                                label = { Text("Manual Longitude (Decimal)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        var latToSave = 0.0
                        var lonToSave = 0.0
                        var elevToSave = 0.0
                        var accToSave = 0.0

                        if (wpInputMode == "GPS") {
                            latToSave = gpsState.latitude
                            lonToSave = gpsState.longitude
                            elevToSave = gpsState.elevation
                            accToSave = gpsState.accuracy
                        } else if (wpInputMode == "CENTER") {
                            val mapCenter = mapViewRef?.mapCenter
                            latToSave = mapCenter?.latitude ?: 32.4279
                            lonToSave = mapCenter?.longitude ?: 53.6880
                            elevToSave = 0.0
                            accToSave = 10.0
                        } else {
                            latToSave = manualLatStr.toDoubleOrNull() ?: 0.0
                            lonToSave = manualLonStr.toDoubleOrNull() ?: 0.0
                            elevToSave = 0.0
                            accToSave = 1.0
                        }

                        if (latToSave != 0.0 && lonToSave != 0.0) {
                            val newWp = MapWaypoint(
                                name = wpName,
                                description = wpDesc,
                                latitude = latToSave,
                                longitude = lonToSave,
                                elevation = elevToSave,
                                accuracy = accToSave,
                                category = wpCategory
                            )
                            viewModel.saveWaypoint(newWp)
                            showAddWaypointDialog = false
                            Toast.makeText(context, "Waypoint '$wpName' saved securely offline", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Cannot save waypoint with unmapped or empty (0,0) coordinates", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC96A2B))
                ) {
                    Text("Save Waypoint")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWaypointDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // B. DIALOG: Custom Track/Route Save Dialog builder
    if (showSaveTrackDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTrackDialog = false },
            title = { Text("Register Field Survey Route Track", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your survey navigation path is ready to be stored in the primary workspace trace registry.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = newTrackName,
                        onValueChange = { newTrackName = it },
                        label = { Text("Route Track Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("${activeTrackPoints.size} logged coordinates vertices will compile into this path.", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTrackName.isNotBlank() && activeTrackPoints.isNotEmpty()) {
                            val serializedPts = activeTrackPoints.joinToString(";") { "${it.latitude},${it.longitude}" }
                            val newTrk = MapTrack(
                                name = newTrackName,
                                pointsJson = serializedPts,
                                isRecording = false
                            )
                            viewModel.saveTrack(newTrk)
                            isRecording = false
                            activeTrackPoints.clear()
                            showSaveTrackDialog = false
                            Toast.makeText(context, "Route Trace '$newTrackName' registered securely", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC96A2B))
                ) {
                    Text("Save Route")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isRecording = false
                        activeTrackPoints.clear()
                        showSaveTrackDialog = false
                        Toast.makeText(context, "Route recording discarded", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Discard Track")
                }
            }
        )
    }
}

@Composable
fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFFC96A2B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GsiHoekBrownSection(
    gsi: Double,
    onGsiChange: (Double) -> Unit,
    ucs: Double,
    onUcsChange: (Double) -> Unit,
    mi: Double,
    onMiChange: (Double) -> Unit,
    d: Double,
    onDChange: (Double) -> Unit,
    rockName: String
) {
    val mb = mi * Math.exp((gsi - 100) / (28 - 14 * d))
    val s = Math.exp((gsi - 100) / (9 - 3 * d))
    val a = 0.5 + (1.0 / 6.0) * (Math.exp(-gsi / 15.0) - Math.exp(-20.0 / 3.0))

    val sig_cm = ucs * Math.pow(s, a)
    val sig_t = if (mb > 0) -(s * ucs) / mb else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "HOEK-BROWN ROCK MASS FAILURE ENGINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC96A2B),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Active Reference Rock Type: $rockName",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Calculates rock mass strength parameters using Hoek-Brown (2002 Edition) criteria.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Geological Strength Index (GSI)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${gsi.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFC96A2B))
                    }
                    Slider(
                        value = gsi.toFloat(),
                        onValueChange = { onGsiChange(it.toDouble()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Massive (85)" to 85.0,
                            "Blocky (65)" to 65.0,
                            "Very Blocky (50)" to 50.0,
                            "Blocky/Seamy (35)" to 35.0,
                            "Crushed (15)" to 15.0
                        ).forEach { (lbl, valD) ->
                            PresetChip(
                                label = lbl,
                                isSelected = Math.abs(gsi - valD) < 2.0,
                                onClick = { onGsiChange(valD) }
                            )
                        }
                    }
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Compressive Strength of Intact Rock (σ_ci)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${ucs.toInt()} MPa", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFC96A2B))
                    }
                    Slider(
                        value = ucs.toFloat(),
                        onValueChange = { onUcsChange(it.toDouble()) },
                        valueRange = 1f..350f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Intact Rock Parameter (m_i)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "%.1f", mi), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFC96A2B))
                    }
                    Slider(
                        value = mi.toFloat(),
                        onValueChange = { onMiChange(it.toDouble()) },
                        valueRange = 1f..50f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Blast/Excavation Disturbance Factor (D)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "%.2f", d), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFC96A2B))
                    }
                    Slider(
                        value = d.toFloat(),
                        onValueChange = { onDChange(it.toDouble()) },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Excellent TBM (0.0)" to 0.0,
                            "Controlled Blasting (0.5)" to 0.5,
                            "Normal Blasting (0.8)" to 0.8,
                            "Heavy Disturbance (1.0)" to 1.0
                        ).forEach { (lbl, valD) ->
                            PresetChip(
                                label = lbl,
                                isSelected = Math.abs(d - valD) < 0.05,
                                onClick = { onDChange(valD) }
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "GEOMECHANICAL PARAMETERS SUMMARY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("m_b", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.4f", mb), fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("s (joints)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.5f", s), fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("exponent a", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.4f", a), fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Mass Compressive Strength (σ_cm)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(String.format(Locale.US, "%.3f MPa", sig_cm), fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFFC96A2B))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Mass Tensile Strength (σ_t)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(String.format(Locale.US, "%.3f MPa", sig_t), fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFFC96A2B))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UcsDatabaseSection(
    onSelectRock: (name: String, ucsVal: Double, miVal: Double) -> Unit
) {
    data class RockSpec(val name: String, val group: String, val ucs: Double, val mi: Double)
    val database = listOf(
        RockSpec("Granite (Coarse)", "Igneous", 160.0, 32.0),
        RockSpec("Basalt (Fine)", "Igneous", 220.0, 25.0),
        RockSpec("Andesite", "Igneous", 140.0, 25.0),
        RockSpec("Rhyolite", "Igneous", 180.0, 25.0),
        RockSpec("Quartz Sandstone", "Sedimentary", 95.0, 19.0),
        RockSpec("Limestone (Micritic)", "Sedimentary", 75.0, 12.0),
        RockSpec("Shale / Mudstone", "Sedimentary", 35.0, 6.0),
        RockSpec("Siltstone", "Sedimentary", 60.0, 7.0),
        RockSpec("Quartzite", "Metamorphic", 230.0, 24.0),
        RockSpec("Quartzite Gneiss", "Metamorphic", 130.0, 28.0),
        RockSpec("Mica Schist", "Metamorphic", 55.0, 10.0),
        RockSpec("Marble", "Metamorphic", 85.0, 9.0)
    )

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterGroup by remember { mutableStateOf("ALL") }

    val filteredList = database.filter { spec ->
        (selectedFilterGroup == "ALL" || spec.group.equals(selectedFilterGroup, ignoreCase = true)) &&
        spec.name.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Reference Rock Type") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "Igneous", "Sedimentary", "Metamorphic").forEach { gp ->
                val isSelected = selectedFilterGroup == gp
                Button(
                    onClick = { selectedFilterGroup = gp },
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(gp, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredList) { spec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectRock(spec.name, spec.ucs, spec.mi) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(spec.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Group: ${spec.group}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Typical UCS", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${spec.ucs.toInt()} MPa", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFC96A2B))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Constant mi", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${spec.mi.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RockMassReportSection(
    rmr: Double,
    q: Double,
    gsi: Double,
    ucs: Double,
    rockName: String,
    mi: Double,
    d: Double
) {
    val gsiFromRmr = rmr - 5.0
    val rmrFromQ = 9.0 * Math.log(Math.max(q, 0.001)) + 44.0

    val emValue = if (ucs <= 100.0) {
        (1.0 - d / 2.0) * Math.sqrt(ucs / 100.0) * Math.pow(10.0, (gsi - 10.0) / 40.0)
    } else {
        (1.0 - d / 2.0) * Math.pow(10.0, (gsi - 10.0) / 40.0)
    }

    val rmrClass = when {
        rmr > 80 -> "I (Very Good Rock)"
        rmr > 60 -> "II (Good Rock)"
        rmr > 40 -> "III (Fair Rock)"
        rmr > 20 -> "IV (Poor Rock)"
        else -> "V (Very Poor Rock)"
    }

    val qClass = when {
        q > 40 -> "Exceptionally / Extremely Good"
        q > 10 -> "Good"
        q > 4 -> "Fair"
        q > 1 -> "Poor"
        q > 0.1 -> "Very Poor"
        else -> "Exceptionally Poor"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFC96A2B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AUTOMATIC ROCK MECHANICS SYNTHESIS REPORT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Engineering Geological Classification Summary",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("1. INPUT VALUES OVERVIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider(thickness = 0.5.dp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active RMR Value", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.1f", rmr), fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Q-System", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.4f", q), fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("GSI", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${gsi.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("Lithology Context", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(rockName, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ucs of Intact", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${ucs.toInt()} MPa", fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Disturbance d", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(d.toString(), fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("2. GEOMECHANISTIC CORRELATIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider(thickness = 0.5.dp)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("GSI Estimated from RMR (RMR89 - 5)", fontSize = 12.sp)
                        Text(String.format(Locale.US, "%.1f", gsiFromRmr), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("RMR Estimated from Q (9*ln(Q) + 44)", fontSize = 12.sp)
                        Text(String.format(Locale.US, "%.1f", rmrFromQ), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rock Mass Deformation Modulus (E_m)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "%.2f GPa", emValue), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFC96A2B))
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("3. ENGINEERING SUPPORT EXCAVATION ADVICE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider(thickness = 0.5.dp)

                Text("RMR Rock Class: $rmrClass", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Barton Q Class: $qClass", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(4.dp))

                val supportAdvice = when {
                    rmr > 60 -> "Rock mass is highly self-supporting. Unsupported spans can exceed several meters. Standard spot rock bolting (L=2.5m) and thin shotcrete sealing (50mm) is sufficient."
                    rmr > 40 -> "Rock mass is moderately jointed or fair-grade. Systematic active rock bolting on a 1.5m to 2m grid, combined with 50-100mm fiber-reinforced shotcrete layer, is recommended for tunnel headings and underground caverns."
                    else -> "Rock mass exhibits heavy shearing and is weak, classified as poor/very poor. Immediate support installation is mandatory. Apply heavy systematic steel active grid arch linings (H-sections), thick micro-silica shotcrete (150-200mm), and self-drilling friction forepoling shields."
                }

                Text(
                    text = supportAdvice,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

