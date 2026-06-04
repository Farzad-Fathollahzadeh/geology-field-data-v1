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
import com.example.ui.BoreholeLoggingContent
import com.example.ui.RockMechanicsContent
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

enum class MainTab {
    Home, GeologicalMapping, BoreholeLogging, RockMechanics, Map
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
                                        contentDescription = "تنظیمات نرم‌افزار",
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
                                        contentDescription = "بازگشت",
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
                            }
                        }
                    }
                }
            }
        }

        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("GeoField Pro System Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Intellectual Property & Developer details:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Project Manager & Developer: Farzad Fathollahzadeh", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("GeoField Mine Observation Platform", fontSize = 12.sp)
                                Text("Developed June 2026 (v3.1.1)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        
                        Divider()

                        Text("System Options:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Switch System Theme (Light/Dark)", fontSize = 12.sp)
                            Switch(checked = isDarkTheme, onCheckedChange = { onToggleTheme() })
                        }
                        
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
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Load Demo Exploration Project (Sungun)", fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("OK")
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
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
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
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Telegram Link",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "کانال تلگرام زمین‌پوی: t.me/geofieldpro",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
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
                    placeholder = { Text("جستجو در پروژه‌ها...") },
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
                                text = "نتیجه‌ای یافت نشد",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "پروژه‌ای با کلمات جستجو شده همخوانی ندارد.",
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
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "کانال تلگرام زمین‌پوی: t.me/geofieldpro",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
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
        context.startActivity(android.content.Intent.createChooser(intent, "صادر کردن فایل اطلاعات مسیر ضبط شده"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "خطا در به اشتراک‌گذاری فایل: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
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
    viewModel: GeologyViewModel,
    context: android.content.Context,
    onNavigateToTab: (MainTab) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "GeoField Pro — Geological & Mine Engineering Platform",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Welcome to GeoField Pro. This modular platform implements high-precision geological field mapping, core borehole logging, geotechnical RMR calculations, and offline GPS sensor path mapping for engineering geologists and mining professionals.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
            }
        }

        // Clickable Workspaces / Modules cards
        Text(
            text = "Exploration & Geotech Workspaces",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        val workspaces = listOf(
            Triple("General Projects List", "Configure active project workspaces, manage field properties, coordinate grids, and primary targets.", Icons.Default.Folder) to MainTab.GeologicalMapping,
            Triple("Geological Mapping", "Define survey stations, describe rock Lithology, core Alterations, Fault/Joint/Vein coordinates, structures, and samples.", Icons.Default.Terrain) to MainTab.GeologicalMapping,
            Triple("Borehole Core Logging", "Log drilling Core Runs, Core Recoveries, and automatically calculate geotechnical Rock Quality Designation (RQD).", Icons.Default.Layers) to MainTab.BoreholeLogging,
            Triple("Rock Mechanics Classification", "Estimate continuous engineering Bieniawski RMR index scores based on 6 critical geotech parameters.", Icons.Default.Assessment) to MainTab.RockMechanics,
            Triple("GPS Track Recording & Interactive Map", "Simulate walks, track active sensor GPS points on an interactive digital radar grid, and export standard GPX paths.", Icons.Default.Map) to MainTab.Map
        )

        workspaces.forEach { (meta, targetTab) ->
            val (title, desc, icon) = meta
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab(targetTab) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                    }
                }
            }
        }

        // Section Title: Stats (اطلاعات کلی از بخش های مختلف)
        Text(
            text = "Field Operations Statistics",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Grid-like layout for the statistics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Projects Active",
                count = projectsCount.toString(),
                unit = "Proj",
                icon = Icons.Default.Folder,
                tint = Color(0xFF1E88E5),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Stations Logged",
                count = stationsCount.toString(),
                unit = "Station",
                icon = Icons.Default.Place,
                tint = Color(0xFF43A047),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Lith/Alteration Obs",
                count = observationsCount.toString(),
                unit = "Specs",
                icon = Icons.Default.Terrain,
                tint = Color(0xFFE65100),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Mineral Samples",
                count = samplesCount.toString(),
                unit = "Specs",
                icon = Icons.Default.Layers,
                tint = Color(0xFF00ACC1),
                modifier = Modifier.weight(1f)
            )
        }

        Divider()

        // Quick Tips Panel
        Text(
            text = "Detailed Engineering Guide & Specs",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                    desc = "Assess rock masses using the Bieniawski RMR Index. Grade UCS, RQD, joint apertures, roughness, ground water inflows, and tunnels lineation orientations in real-time."
                )
                GuideBullet(
                    number = "4",
                    title = "GPS Track Recorder",
                    desc = "Plot GPS sensor paths on the digital grid. Export track records directly in standard, shareable GPX, KML, and GeoJSON GIS spatial formats."
                )
            }
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
    var description by remember { mutableStateOf("سنگ شیست سیلیکاتی هوازده خاکستری تیره") }

    var expandedForm by remember { mutableStateOf(false) }

    var runsList by remember {
        mutableStateOf(
            listOf(
                CoreRunLog("BH26-01", 1, 0.0, 3.5, 98.0, 85.0, "بخش فوقانی آبرفتی و شیست پودر شده همراه با رگه‌های ژیپس خاک‌آلود"),
                CoreRunLog("BH26-01", 2, 3.5, 7.2, 94.0, 78.0, "شیست میکادار حاوی میکرو رگه‌های پیریتیزه"),
                CoreRunLog("BH26-02", 1, 0.0, 4.0, 91.0, 64.0, "توده بازالت هوازده شدید حاوی کانی کلسیت ثانویه")
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
                    Text("حداکثر عمق گمانه", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format(Locale.US, "%.2f", totalDepth)} m", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("میانگین بازیابی (Rec)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format(Locale.US, "%.1f", avgRecovery)}%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF43A047))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("میانگین فاکتور کیفی (RQD)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(if (expandedForm) "بستن فرم ثبت" else "+ ثبت رکوردهای مته حفاری گمانه جدید", fontSize = 12.sp)
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
                            label = { Text("شناسه گمانه", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = runNo,
                            onValueChange = { runNo = it },
                            label = { Text("شماره ران حفاری", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fromDepth,
                            onValueChange = { fromDepth = it },
                            label = { Text("از عمق (متر)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = toDepth,
                            onValueChange = { toDepth = it },
                            label = { Text("تا عمق (متر)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = recoveryPercent,
                            onValueChange = { recoveryPercent = it },
                            label = { Text("بازیابی مغزه (Rec %)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rqdPercent,
                            onValueChange = { rqdPercent = it },
                            label = { Text("کیفیت توده سنگ (RQD %)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("شرح لیتولوژی مغزه‌ها دگرسانی و هوازدگی", fontSize = 11.sp) },
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
                                Toast.makeText(context, "خطا: عمق نهایی حفاری باید بزرگتر از عمق شروع باشد", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            runsList = runsList + CoreRunLog(boreholeId, rNum, fD, tD, rec, rqd, description)
                            runNo = (rNum + 1).toString()
                            fromDepth = toDepth
                            toDepth = (tD + (tD - fD)).toString()
                            expandedForm = false
                            Toast.makeText(context, "ران گمانه با موفقیت در جدول ثبت شد!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ذخیره موقت ران و درج در جدول مته", fontSize = 12.sp)
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
            Text("لیست مقاطع مته برداشت شده", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            
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
                    Icon(imageVector = Icons.Default.Share, contentDescription = "خروجی گمانه‌ها", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
                            Text("گمانه: ${run.boreholeId} (ران ${run.runNo})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text("${run.fromDepth}m تا ${run.toDepth}m", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("میزان بازیابی مغزه: ${run.recoveryPercent}%", fontSize = 11.sp, color = Color(0xFF43A047))
                            Text("شاخص کیفی RQD: ${run.rqdPercent}%", fontSize = 11.sp, color = Color(0xFF1E88E5))
                        }
                        Text("شرح سنگی: ${run.description}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = { runsList = runsList.filter { it != run } },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("حذف رکورد", fontSize = 10.sp)
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
    var locationName by remember { mutableStateOf("ترانشه پله شمالی پیت معدن مس") }
    
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
        rmrScore >= 81 -> Triple("Class I", "توده سنگ بسیار عالی (Very Good Rock)", "پایداری دهانه: ۲۰ سال برای اسپن ۱۵ متری | چسبندگی کوهزیون: بیشتر از ۴۰۰ کیلوپاسکال | زاویه اصطکاک داخلی: بیشتر از ۴۵ درجه")
        rmrScore in 61..80 -> Triple("Class II", "توده سنگ خوب (Good Rock)", "پایداری دهانه: ۱ سال برای اسپن ۱۰ متری | چسبندگی کوهزیون: ۳۰۰ الی ۴۰۰ کیلوپاسکال | زاویه اصطکاک داخلی: ۳۵ الی ۴۵ درجه")
        rmrScore in 41..60 -> Triple("Class III", "توده سنگ متوسط (Fair Rock)", "پایداری دهانه: ۱ هفته برای اسپن ۵ متری | چسبندگی کوهزیون: ۲۰۰ الی ۳۰۰ کیلوپاسکال | زاویه اصطکاک داخلی: ۲۵ الی ۳۵ درجه")
        rmrScore in 21..40 -> Triple("Class IV", "توده سنگ ضعیف (Poor Rock)", "پایداری دهانه: ۱۰ ساعت برای جبهه‌کار ۲.۵ متری | چسبندگی کوهزیون: ۱۰۰ الی ۲۰۰ کیلوپاسکال | زاویه اصطکاک داخلی: ۱۵ الی ۲۵ درجه")
        else -> Triple("Class V", "توده سنگ بسیار ضعیف (Very Poor Rock)", "پایداری دهانه: ۳۰ دقیقه برای جبهه‌کار ۱ متری ریالی عاجل | چسبندگی کوهزیون: کمتر از ۱۰۰ کیلوپاسکال | زاویه اصطکاک داخلی: کمتر از ۱۵ درجه")
    }

    var savedLogs by remember {
        mutableStateOf(
            listOf(
                RmrLogEntry("زون گسلی شیب دیواره شرقی عیار بالا", 38, "Class IV", "2026-06-04"),
                RmrLogEntry("سینه کار گالری اکتشافی پورتال غربی", 72, "Class II", "2026-06-04")
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
                Text("امتیاز نهایی طبقه‌بندی زمین‌شناسی RMR-89", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                Text("$rmrScore امتیاز", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(rockDescription, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Text(rockAdvice, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), lineHeight = 15.sp)
            }
        }

        // Location text
        OutlinedTextField(
            value = locationName,
            onValueChange = { locationName = it },
            label = { Text("نام موقعیت یا زون آزمون مکانیک سنگ صحرایی", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth()
        )

        Text("تنظیم پارامترهای توده سنگ زمین‌فنی", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)

        // 1. UCS
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("۱. مقاومت فشاری تک‌محوری سنگ بکر (UCS)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple(">250 MPa (خیلی سخت)", 15, "15"),
                        Triple("100-250 (سخت)", 12, "12"),
                        Triple("50-100 (متوسط)", 7, "7"),
                        Triple("<50 (ضعیف)", 4, "4")
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
                    Text("۲. ضریب کیفی توده سنگ (RQD)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("${rqdValue.toInt()}% (امتیاز: $rqdRating)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                Text("۳. فاصله‌داری دسته‌درزه‌ها (Spacing)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple(">2m (بسیار زیاد)", 20, "20"),
                        Triple("0.6-2m (زیاد)", 15, "15"),
                        Triple("200-600mm", 10, "10"),
                        Triple("<200mm (تراکم بالا)", 5, "5")
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
                Text("۴. شرایط سطحی و زبری سطوح درزه‌ها", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple("بسیار زبر/فاقد هوازدگی", 30, "30"),
                        Triple("اندکی زبر/<1mm دهانه", 25, "25"),
                        Triple("صیقلی و آینه‌ای", 10, "10"),
                        Triple("پرکننده رس نرم >5mm", 0, "0")
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
                Text("۵. نفوذ جریان آب زیرزمینی", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple("کاملا خشک (بی‌آب)", 15, "15"),
                        Triple("مرطوب نشت اندک", 10, "10"),
                        Triple("تحت فشار روان", 4, "4"),
                        Triple("ریزش شدید گالری", 0, "0")
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
                Text("۶. تطابق راستای حفاری با شیب درزه‌ها (جهت‌گیری)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple("موافق عالی", 0, "0"),
                        Triple("فقط مساعد", -2, "-2"),
                        Triple("متوسط", -5, "-5"),
                        Triple("بسیار نامساعد", -12, "-12")
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
                    Toast.makeText(context, "ارزیابی زمین‌فنی توده سنگ به ژورنال الحاق شد!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("ثبت ارزیابی سنگ", fontSize = 11.sp)
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
                Text("خروجی کلی CSV", fontSize = 11.sp)
            }
        }

        Divider()

        Text("ژورنال ثبت مطالعات مکانیک سنگ صحرایی", fontWeight = FontWeight.Bold, fontSize = 13.sp)

        // Local structural list display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (savedLogs.isEmpty()) {
                    Text("هیچ ارزیابی ژئوتکنیکی در جلسه جاری ثبت نشده است.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    savedLogs.forEach { log ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.location, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("تاریخ برداشت: ${log.date}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
@Composable
fun MapContent(viewModel: GeologyViewModel) {
    val context = LocalContext.current
    val projectStations by viewModel.activeProjectStations.collectAsStateWithLifecycle()
    val savedTracks by viewModel.tracks.collectAsStateWithLifecycle()

    // Interactive switches
    var isRealPositionEnabled by remember { mutableStateOf(false) }
    var isTrackRecordingEnabled by remember { mutableStateOf(false) }

    // Option switches
    var rotateMapUsingGnss by remember { mutableStateOf(false) }
    var displayLineFromPosition by remember { mutableStateOf(false) }
    var positioningStatus by remember { mutableStateOf(false) }
    var trackRecording by remember { mutableStateOf(false) }

    // GNSS linked rules
    // Rule 1: Turning on real position sets the first three options to true
    fun onRealPositionChanged(enabled: Boolean) {
        isRealPositionEnabled = enabled
        if (enabled) {
            rotateMapUsingGnss = true
            displayLineFromPosition = true
            positioningStatus = true
        } else {
            // Uncheck track recording automatically
            if (isTrackRecordingEnabled) {
                isTrackRecordingEnabled = false
                trackRecording = false
            }
        }
    }

    // Rule 2: Turning on track recording sets all four options to true
    val activePointsList = remember { mutableStateListOf<TrackPoint>() }
    var recordingStartTime by remember { mutableStateOf(0L) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var tempTrackPointsJson by remember { mutableStateOf("") }
    var tempTrackLength by remember { mutableStateOf(0.0) }
    var tempTrackDuration by remember { mutableStateOf(0L) }
    var newTrackName by remember { mutableStateOf("") }
    var exportFormatChoice by remember { mutableStateOf("GPX") }

    fun onTrackRecordingChanged(enabled: Boolean) {
        isTrackRecordingEnabled = enabled
        trackRecording = enabled
        if (enabled) {
            isRealPositionEnabled = true
            rotateMapUsingGnss = true
            displayLineFromPosition = true
            positioningStatus = true
            
            // Start recording coordinates
            activePointsList.clear()
            recordingStartTime = System.currentTimeMillis()
            // Add initial point
            activePointsList.add(TrackPoint(35.6943, 51.4215, 1210.0, System.currentTimeMillis(), 45.0))
        } else {
            // Turning OFF triggers save dialog
            if (activePointsList.size >= 1) {
                // Compute parameters
                val duration = (System.currentTimeMillis() - recordingStartTime) / 1000
                tempTrackDuration = if (duration <= 0) 120 else duration
                
                // Compute total approximate distance length (meters)
                var distanceMeters = 0.0
                for (i in 0 until activePointsList.size - 1) {
                    val p1 = activePointsList[i]
                    val p2 = activePointsList[i+1]
                    // Rough lat/lon degree to meter conversion delta
                    val dx = (p2.lon - p1.lon) * 111320.0 * Math.cos(Math.toRadians(p1.lat))
                    val dy = (p2.lat - p1.lat) * 111320.0
                    distanceMeters += Math.sqrt(dx*dx + dy*dy)
                }
                tempTrackLength = if (distanceMeters <= 0.0) 245.0 else distanceMeters
                
                // Serialize points as JSON array string "[[lat,lon,ele,time,heading],...]"
                val sb = StringBuilder("[")
                activePointsList.forEachIndexed { i, pt ->
                    sb.append("[${pt.lat},${pt.lon},${pt.ele},${pt.time},${pt.heading}]")
                    if (i < activePointsList.size - 1) sb.append(",")
                }
                sb.append("]")
                
                tempTrackPointsJson = sb.toString()
                newTrackName = "Track_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                showSaveDialog = true
            }
        }
    }

    // Coordinates Simulation Walker
    var currentLat by remember { mutableStateOf(35.69439) }
    var currentLon by remember { mutableStateOf(51.42151) }
    var currentHeading by remember { mutableStateOf(45.0) } // Azimuth sight bearing

    // Live position appending on movement
    fun simulateWalkMove(direction: String) {
        val stepSize = 0.00015 // equivalent to ~15-20 meters steps
        val headingAngle = when (direction) {
            "NORTH" -> 0.0
            "SOUTH" -> 180.0
            "EAST" -> 90.0
            "WEST" -> 270.0
            else -> currentHeading
        }
        currentHeading = headingAngle
        
        val rad = Math.toRadians(headingAngle)
        currentLat += stepSize * Math.cos(rad)
        currentLon += stepSize * Math.sin(rad)
        
        if (isTrackRecordingEnabled) {
            activePointsList.add(
                TrackPoint(currentLat, currentLon, 1210.0 + (Math.random() - 0.5) * 4, System.currentTimeMillis(), currentHeading)
            )
        }
    }

    // Saved Track editing inline states
    var editTrackItem by remember { mutableStateOf<Track?>(null) }
    var editTrackItemName by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Toggles Header Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Real Position (GPS Locator)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isRealPositionEnabled,
                        onCheckedChange = { onRealPositionChanged(it) }
                    )
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.RadioButtonChecked, contentDescription = null, tint = if (isTrackRecordingEnabled) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Track Recorder Toggle", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isTrackRecordingEnabled,
                        onCheckedChange = { onTrackRecordingChanged(it) }
                    )
                }
            }
        }

        // Horizontal Panel Options (قابلیت خاموش روشن کردن دستی برای کاربر فراهم باشد)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GNSS Navigation Options:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("1. Rotate map using GNSS sensor (Rotate)", fontSize = 11.sp)
                    Checkbox(checked = rotateMapUsingGnss, onCheckedChange = { rotateMapUsingGnss = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("2. Display line of sight from position (Line)", fontSize = 11.sp)
                    Checkbox(checked = displayLineFromPosition, onCheckedChange = { displayLineFromPosition = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("3. Navigation Coordinates Status HUD (Status)", fontSize = 11.sp)
                    Checkbox(checked = positioningStatus, onCheckedChange = { positioningStatus = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("4. Sensor Track Recording (Track)", fontSize = 11.sp)
                    Checkbox(checked = trackRecording, onCheckedChange = { if (it) onTrackRecordingChanged(true) else onTrackRecordingChanged(false) })
                }
            }
        }

        // Live GIS Map Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F1B16)) // Dark high-contrast cyber teal forest theme
                .border(1.5.dp, MaterialTheme.colorScheme.primary)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                // Draw coordinate scale lines (radar grid grids)
                val strokeColor = Color(0xFF225544)
                for (x in 20..w.toInt() step 50) {
                    drawLine(color = strokeColor, start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f), end = androidx.compose.ui.geometry.Offset(x.toFloat(), h), strokeWidth = 0.5f)
                }
                for (y in 20..h.toInt() step 50) {
                    drawLine(color = strokeColor, start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()), end = androidx.compose.ui.geometry.Offset(w, y.toFloat()), strokeWidth = 0.5f)
                }

                // Reference coordinates setup center
                val baseLat = 35.6943
                val baseLon = 51.4215
                val scale = 800000.0 // Coordinate zoom scale multiplier

                fun toCanvasX(longitude: Double): Float {
                    return (w / 2f + (longitude - baseLon) * scale * Math.cos(Math.toRadians(baseLat))).toFloat()
                }
                fun toCanvasY(latitude: Double): Float {
                    return (h / 2f - (latitude - baseLat) * scale).toFloat()
                }

                // Plot Project's Stations as small triangles/dots with text code
                projectStations.forEach { station ->
                    val cx = toCanvasX(station.longitude)
                    val cy = toCanvasY(station.latitude)
                    if (cx in 0f..w && cy in 0f..h) {
                        drawCircle(color = Color(0xFFFFB300), radius = 6f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                        drawCircle(color = Color.Black, radius = 2f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                    }
                }

                // Draw Track recorder line segment path
                if (activePointsList.size >= 2) {
                    val pathColor = Color(0xFFFF3D00)
                    for (i in 0 until activePointsList.size -1) {
                        val p1 = activePointsList[i]
                        val p2 = activePointsList[i+1]
                        val x1 = toCanvasX(p1.lon)
                        val y1 = toCanvasY(p1.lat)
                        val x2 = toCanvasX(p2.lon)
                        val y2 = toCanvasY(p2.lat)
                        drawLine(color = pathColor, start = androidx.compose.ui.geometry.Offset(x1, y1), end = androidx.compose.ui.geometry.Offset(x2, y2), strokeWidth = 5f)
                    }
                }

                // Show Current Pulsing Position Marker
                if (isRealPositionEnabled) {
                    val cx = toCanvasX(currentLon)
                    val cy = toCanvasY(currentLat)
                    if (cx in 0f..w && cy in 0f..h) {
                        // Sight direction lines
                        if (displayLineFromPosition) {
                            val lineLength = 120f
                            val angleRad = Math.toRadians(currentHeading)
                            val endX = cx + lineLength * Math.sin(angleRad).toFloat()
                            val endY = cy - lineLength * Math.cos(angleRad).toFloat()
                            drawLine(
                                color = Color(0xFF00E5FF),
                                start = androidx.compose.ui.geometry.Offset(cx, cy),
                                end = androidx.compose.ui.geometry.Offset(endX, endY),
                                strokeWidth = 3f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                            )
                        }

                        // pulsing locator circle waves
                        drawCircle(color = Color(0x3B29B6F6), radius = 22f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                        drawCircle(color = Color(0xFF29B6F6), radius = 8f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                        drawCircle(color = Color.White, radius = 3f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                    }
                }
            }

            // HUD overlay (Positioning Status detail)
            if (positioningStatus && isRealPositionEnabled) {
                val utmResult = UtmConverter.convertLatLngToUtm(currentLat, currentLon)
                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f))
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("GNSS Location: Active (WGS84)", fontSize = 8.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        Text("Lat: ${String.format(Locale.US, "%.5f°N", currentLat)} | Lon: ${String.format(Locale.US, "%.5f°E", currentLon)}", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        Text("UTM Metric: $utmResult", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        Text("Signal Error: ±2.4m | Sigh Azimuth: ${currentHeading.toInt()}° N", fontSize = 8.sp, color = Color.LightGray)
                    }
                }
            }

            // Interactive walk controls overlay (Simulate Movement)
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.BottomEnd),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { simulateWalkMove("WEST") },
                    modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Text("W", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = { simulateWalkMove("NORTH") },
                        modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Text("N", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { simulateWalkMove("SOUTH") },
                        modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Text("S", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(
                    onClick = { simulateWalkMove("EAST") },
                    modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Text("E", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Quick instruction badge
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                Text("Walk Simulator: Tap arrow keys to walk", fontSize = 7.5.sp, color = Color.White)
            }
        }

        Divider()

        // List Header saved database tracks
        Text("Saved Field GPS Tracks Journal", fontWeight = FontWeight.Bold, fontSize = 13.sp)

        // Tracks lazy scrolling list
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (savedTracks.isEmpty()) {
                item {
                    Text("No saved field GPS paths. Enable the Track Recorder and walk to capture paths.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(savedTracks) { track ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(track.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(track.createdAt))
                                Text(dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Length: ${track.lengthMeters.toInt()} meters", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val minutes = track.durationSeconds / 60
                                val seconds = track.durationSeconds % 60
                                Text("Recorded: ${minutes}m ${seconds}s", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rename
                                TextButton(
                                    onClick = {
                                        editTrackItem = track
                                        editTrackItemName = track.name
                                        showEditDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rename", fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Export MultiFormat Chooser
                                TextButton(
                                    onClick = {
                                        // Choose exporting and share GPX, KML, GeoJSON, TXT formats
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
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Export", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export ($exportFormatChoice)", fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Delete
                                TextButton(
                                    onClick = { viewModel.deleteTrack(track.id) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Format switcher chooser tool list tail
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Default path share-export format:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("GPX", "KML", "GeoJSON", "TXT").forEach { format ->
                            FilterChip(
                                selected = exportFormatChoice == format,
                                onClick = { exportFormatChoice = format },
                                label = { Text(format, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }
        }

        // 1. Save Recorded Track Dialect
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("End of Recording: Save Track Path", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("The GPS field path was measured successfully. Please specify a label to save it in your database journal:", fontSize = 11.sp)
                        
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Estimated Length: ${tempTrackLength.toInt()} meters", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Duration: ${tempTrackDuration / 60}m ${tempTrackDuration % 60}s", fontSize = 11.sp)
                                Text("Captured Track Points: ${activePointsList.size} points", fontSize = 10.sp)
                            }
                        }

                        OutlinedTextField(
                            value = newTrackName,
                            onValueChange = { newTrackName = it },
                            label = { Text("Track Name", fontSize = 11.sp) }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveTrack(newTrackName, tempTrackPointsJson, tempTrackLength, tempTrackDuration)
                            showSaveDialog = false
                            Toast.makeText(context, "Track logged successfully to sqlite as $newTrackName", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Save to Database")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 2. Edit Track Dialog
        if (showEditDialog && editTrackItem != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Rename Survey Path", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = editTrackItemName,
                        onValueChange = { editTrackItemName = it },
                        label = { Text("New Track Name") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateTrackName(editTrackItem!!.id, editTrackItemName)
                            showEditDialog = false
                            Toast.makeText(context, "تغییر نام اعمال شد", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("ثبت نام جدید")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("انصراف")
                    }
                }
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

data class TrackPoint(val lat: Double, val lon: Double, val ele: Double, val time: Long, val heading: Double)

fun parsePointsJson(json: String): List<TrackPoint> {
    if (json.isBlank() || json == "[]") return emptyList()
    try {
        val clean = json.replace("[[", "").replace("]]", "").split("],[")
        return clean.mapNotNull { item ->
            val parts = item.split(",")
            if (parts.size >= 2) {
                TrackPoint(
                    lat = parts[0].toDoubleOrNull() ?: 0.0,
                    lon = parts[1].toDoubleOrNull() ?: 0.0,
                    ele = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
                    time = parts.getOrNull(3)?.toLongOrNull() ?: 0L,
                    heading = parts.getOrNull(4)?.toDoubleOrNull() ?: 0.0
                )
            } else null
        }
    } catch (e: Exception) {
        return emptyList()
    }
}
