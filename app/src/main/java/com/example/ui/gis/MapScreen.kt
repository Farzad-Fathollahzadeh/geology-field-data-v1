package com.example.ui.gis

import android.widget.Toast
import com.google.accompanist.permissions.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlin.math.cos
import kotlin.math.sin
import java.text.SimpleDateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    geologyViewModel: GeologyViewModel,
    gisViewModel: MapViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 600

    // Database state flows from Core GeologyViewModel
    val activeProjectStations by geologyViewModel.activeProjectStations.collectAsStateWithLifecycle()
    val allStationsList by geologyViewModel.allStations.collectAsStateWithLifecycle()
    val savedTracks by geologyViewModel.tracks.collectAsStateWithLifecycle()
    val activeProject by geologyViewModel.activeProject.collectAsStateWithLifecycle()
    val allObservationsList by geologyViewModel.allObservations.collectAsStateWithLifecycle()
    
    val stationsToShow = if (activeProjectStations.isNotEmpty()) activeProjectStations else allStationsList
    val isTrackRecordingActive by geologyViewModel.isTrackRecordingActive.collectAsStateWithLifecycle()
    val liveLocation by geologyViewModel.liveLocation.collectAsStateWithLifecycle()

    // GIS Map UI settings from specialized MapViewModel
    val uiState by gisViewModel.uiState.collectAsStateWithLifecycle()
    val zoomLevel by gisViewModel.zoomLevel.collectAsStateWithLifecycle()
    val activeBaseMap by gisViewModel.activeBaseMap.collectAsStateWithLifecycle()
    val isOutdoorMode by gisViewModel.isOutdoorMode.collectAsStateWithLifecycle()
    val simWalkStepSize by gisViewModel.simWalkStepSize.collectAsStateWithLifecycle()

    val showGrids by gisViewModel.showGrids.collectAsStateWithLifecycle()
    val showStations by gisViewModel.showStations.collectAsStateWithLifecycle()
    val showSymbols by gisViewModel.showSymbols.collectAsStateWithLifecycle()
    val showFaultLines by gisViewModel.showFaultLines.collectAsStateWithLifecycle()

    val activeBaseMapOpacity by gisViewModel.activeBaseMapOpacity.collectAsStateWithLifecycle()
    val offlineModeForce by gisViewModel.offlineModeForce.collectAsStateWithLifecycle()
    val showOfflineBoundaries by gisViewModel.showOfflineBoundaries.collectAsStateWithLifecycle()
    val downloadedRegionsList by gisViewModel.downloadedRegionsList.collectAsStateWithLifecycle()
    val isDownloading by gisViewModel.isDownloading.collectAsStateWithLifecycle()
    val downloadProgress by gisViewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadStatusText by gisViewModel.downloadStatusText.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    val locationPermissionsState = com.google.accompanist.permissions.rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val isAnyLocationPermissionGranted = locationPermissionsState.permissions.any { it.status.isGranted }

    // GPS tracking local telemetry parameter overrides
    var isRealPositionEnabled by remember { mutableStateOf(true) }
    var lockGpsCenter by remember { mutableStateOf(true) }

    // Automatically trigger live GPS tracking on screen launch and auto-request permission if not granted
    LaunchedEffect(Unit) {
        if (!isAnyLocationPermissionGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        } else {
            isRealPositionEnabled = true
            geologyViewModel.startLocationTracking(context)
        }
    }

    LaunchedEffect(isAnyLocationPermissionGranted) {
        if (isAnyLocationPermissionGranted) {
            isRealPositionEnabled = true
            geologyViewModel.startLocationTracking(context)
        }
    }

    // Manual navigation target
    var navigationTargetStation by remember { mutableStateOf<Station?>(null) }

    // Simplify/De-clutter view states
    var isSimpleMode by remember { mutableStateOf(true) }
    var showMoveSimulator by remember { mutableStateOf(false) }

    // Base coordinate anchor defaults
    val defaultInitialLat = remember(activeProjectStations) {
        if (activeProjectStations.isNotEmpty()) activeProjectStations.map { it.latitude }.average() else 35.69439
    }
    val defaultInitialLon = remember(activeProjectStations) {
        if (activeProjectStations.isNotEmpty()) activeProjectStations.map { it.longitude }.average() else 51.42151
    }

    var simulatorLat by remember(defaultInitialLat) { mutableStateOf(defaultInitialLat) }
    var simulatorLon by remember(defaultInitialLon) { mutableStateOf(defaultInitialLon) }
    var simulatorHeading by remember { mutableStateOf(45.0) }

    val currentLat = if (isRealPositionEnabled && liveLocation != null) liveLocation!!.latitude else simulatorLat
    val currentLon = if (isRealPositionEnabled && liveLocation != null) liveLocation!!.longitude else simulatorLon
    val currentHeading = if (isRealPositionEnabled && liveLocation != null) liveLocation!!.bearing.toDouble() else simulatorHeading
    val currentEle = if (isRealPositionEnabled && liveLocation != null) liveLocation!!.altitude else 1420.0
    val currentAccuracy = if (isRealPositionEnabled && liveLocation != null) liveLocation!!.accuracy.toDouble() else 1.8

    // Trigger walk moves
    fun simulateWalk(direction: String) {
        val stepDeg = (simWalkStepSize / 111320.0).toDouble()
        when (direction) {
            "NORTH" -> {
                simulatorLat += stepDeg
                simulatorHeading = 0.0
            }
            "SOUTH" -> {
                simulatorLat -= stepDeg
                simulatorHeading = 180.0
            }
            "EAST" -> {
                simulatorLon += stepDeg / cos(Math.toRadians(simulatorLat))
                simulatorHeading = 90.0
            }
            "WEST" -> {
                simulatorLon -= stepDeg / cos(Math.toRadians(simulatorLat))
                simulatorHeading = 270.0
            }
        }
        lockGpsCenter = true
        // If recording active, append point directly to core viewModel path tracker
        if (isTrackRecordingActive) {
            geologyViewModel.appendTrackPoint(
                TrackPoint(
                    lat = simulatorLat,
                    lon = simulatorLon,
                    ele = currentEle,
                    time = System.currentTimeMillis(),
                    accuracy = currentAccuracy,
                    heading = simulatorHeading
                )
            )
        }
    }

    // Colors matching specifications
    val fontColor = if (isOutdoorMode) Color.Black else Color.White
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
    val containerBg = if (isOutdoorMode) Color(0xFFFFFFFF) else Color(0xFF000000)
    val mapTileUrl = remember(activeBaseMap) {
        when (activeBaseMap) {
            "OpenTopoMap" -> "https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png"
            "OpenStreetMap" -> "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            "Offline Topo-Raster" -> "https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png" // simulated offline raster tiles
            "Esri Satellite (No-Key)" -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
            "Google Satellite" -> "https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}"
            else -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(containerBg)
    ) {
        // --- LAYER 0: MAP COMPASS INTERACTIVE GPS PORT ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp) // Height of Bottom Navigation bar of 64dp
        ) {
            WebViewMap(
                stations = if (showStations) stationsToShow else emptyList(),
                observations = if (showSymbols) allObservationsList else emptyList(),
                trackPoints = if (isTrackRecordingActive) geologyViewModel.activePoints.value else emptyList(),
                contactsList = emptyList(),
                isRealPos = isRealPositionEnabled,
                currentLat = currentLat,
                currentLon = currentLon,
                zoom = zoomLevel,
                tileUrl = mapTileUrl,
                gpsAccuracy = currentAccuracy,
                lockGpsCenter = lockGpsCenter,
                mapboxToken = "pk.eyJ1IjoiZmFyemFkZiIsImEiOiJjbHpiaXdwZ3cwMTF5MndzYzhvdzBoMHp1In0.6R0R9z84Vd-M208r_6oY7g",
                onCenterChanged = { lat, lon ->
                    if (!lockGpsCenter) {
                        simulatorLat = lat
                        simulatorLon = lon
                    }
                },
                navigationTargetStation = navigationTargetStation,
                showUtmGrid = showGrids,
                isOutdoorMode = isOutdoorMode,
                
                // Opacities & Offline
                baseMapOpacity = activeBaseMapOpacity,
                offlineModeForce = offlineModeForce,
                showOfflineBoundaries = showOfflineBoundaries,
                downloadedRegions = downloadedRegionsList,

                modifier = Modifier.fillMaxSize()
            )
        }

        // --- LAYER 1: HEADS-UP COORDINATES TELEMETRY BANNER & MODE SELECTOR ---
        if (uiState is MapUiState.Idle) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 12.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            // Elegant bilingual mode switcher at the top to quickly toggle Simple/Advanced views to avoid confusion
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = (if (isOutdoorMode) Color.White else Color(0xFF161E1B)).copy(alpha = 0.9f)
                ),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
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
                            text = if (isSimpleMode) "رویت ساده نقشه (بدون شلوغی)" else "نمای پیشرفته مهندسی (GIS Telemetry)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = fontColor
                        )
                        Text(
                            text = if (isSimpleMode) "Simple View • Clear Screen" else "Advanced HUD • Satellites, Grids, Telemetry",
                            fontSize = 9.sp,
                            color = fontColor.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // A modern beautiful toggle button to immediately de-clutter/re-clutter the app
                    Button(
                        onClick = { isSimpleMode = !isSimpleMode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSimpleMode) accentColor else fontColor.copy(alpha = 0.1f),
                            contentColor = if (isSimpleMode) Color.Black else fontColor
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSimpleMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isSimpleMode) "تغییر نمای نقشه" else "حالت ساده",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Display dense coordinates telemetry banner ONLY when NOT in Simple Mode
            if (!isSimpleMode) {
                CoordinateBanner(
                    latitude = currentLat,
                    longitude = currentLon,
                    elevation = currentEle,
                    accuracy = currentAccuracy,
                    isOutdoorMode = isOutdoorMode,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GPSIndicator(
                        satelliteCount = 16,
                        accuracy = currentAccuracy,
                        isRealPosition = isRealPositionEnabled,
                        isOutdoorMode = isOutdoorMode
                    )

                    ScaleBar(
                        zoomLevel = zoomLevel,
                        isOutdoorMode = isOutdoorMode
                    )
                }
            }
        }
    }

        // --- LAYER 2: INTERACTIVE D-PAD COMPASS & FLYOUT SHORTCUTS ---
        if (uiState is MapUiState.Idle) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp, start = 12.dp, end = 12.dp)
            ) {
            // Left column: Zoom triggers + Compass Widget + Move Simulator Toggle
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Compass belongs to advanced suite to reduce cognitive load in simple view
                if (!isSimpleMode) {
                    CompassWidget(
                        heading = currentHeading,
                        bearing = navigationTargetStation?.let {
                            gisViewModel.calculateGeodesicDistance(currentLat, currentLon, it.latitude, it.longitude)
                            currentHeading // target heading offset
                        },
                        isOutdoorMode = isOutdoorMode
                    )
                }

                ZoomControls(
                    zoomLevel = zoomLevel,
                    onZoomIn = { gisViewModel.setZoomLevel(zoomLevel + 1) },
                    onZoomOut = { gisViewModel.setZoomLevel(zoomLevel - 1) },
                    isOutdoorMode = isOutdoorMode
                )

                // Micro Controller Toggle for the interactive walk-path move simulator (Instantly de-clutters the central view!)
                ElevatedButton(
                    onClick = { showMoveSimulator = !showMoveSimulator },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (showMoveSimulator) accentColor else (if (isOutdoorMode) Color.White else Color(0xFF1B2321)),
                        contentColor = if (showMoveSimulator) Color.Black else fontColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = "Simulate Walk",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (showMoveSimulator) "بستن شبیه‌ساز" else "شبیه‌ساز حرکت",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Sliced center overlay: Manual Walk D-Pad controller (Saves steps instantly)
            // Displays cleanly and ONLY when showMoveSimulator is true - freeing map view space!
            if (showMoveSimulator) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOutdoorMode) Color.White.copy(alpha = 0.95f) else Color(0xDD000000)
                    ),
                    border = BorderStroke(0.5.dp, fontColor.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(135.dp)
                        .height(115.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MOVE SIMULATOR | شبیه‌ساز",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        IconButton(
                            onClick = { simulateWalk("NORTH") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowDropUp, contentDescription = "Sim step North", tint = fontColor)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { simulateWalk("WEST") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ArrowLeft, contentDescription = "Sim step West", tint = fontColor)
                            }
                            IconButton(
                                onClick = { lockGpsCenter = !lockGpsCenter },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (lockGpsCenter) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                                    contentDescription = "Lock walk pointer to center",
                                    tint = accentColor
                                )
                            }
                            IconButton(
                                onClick = { simulateWalk("EAST") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ArrowRight, contentDescription = "Sim step East", tint = fontColor)
                            }
                        }
                        IconButton(
                            onClick = { simulateWalk("SOUTH") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Sim step South", tint = fontColor)
                        }
                    }
                }
            }

            // Expanding Flyout Side-GIS Panel
            FloatingGISMenu(
                isTrackRecording = isTrackRecordingActive,
                onAddObservation = { gisViewModel.updateUiState(MapUiState.ObservationCreationMode) },
                onAddSample = { gisViewModel.updateUiState(MapUiState.SampleCreationMode) },
                onAddStructural = { gisViewModel.updateUiState(MapUiState.MeasurementMode) },
                onAddBorehole = { gisViewModel.updateUiState(MapUiState.BoreholeCreationMode) },
                onToggleTrackRecording = {
                    if (isTrackRecordingActive) {
                        geologyViewModel.stopRecordingTrack { pointsJson, length, duration ->
                            geologyViewModel.saveTrack(
                                name = "GisWalk_" + SimpleDateFormat("HHmm", Locale.US).format(java.util.Date()),
                                pointsJson = pointsJson,
                                lengthMeters = length,
                                durationSeconds = duration
                            )
                            Toast.makeText(context, "Walk path logged to database successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        geologyViewModel.startRecordingTrack()
                        Toast.makeText(context, "Recording walk tracks...", Toast.LENGTH_SHORT).show()
                    }
                },
                onMeasureDistance = {
                    val d = gisViewModel.calculateGeodesicDistance(currentLat, currentLon, currentLat + 0.005, currentLon + 0.005)
                    Toast.makeText(context, "Approx distance: ${String.format(Locale.US, "%.0f", d)} meters", Toast.LENGTH_LONG).show()
                },
                onMeasureArea = {
                    val a = gisViewModel.calculatePolygonArea(
                        listOf(currentLat, currentLat + 0.01, currentLat + 0.01, currentLat),
                        listOf(currentLon, currentLon, currentLon + 0.01, currentLon + 0.01)
                    )
                    Toast.makeText(context, "Approx bounding block area: ${String.format(Locale.US, "%.1f", a)} sq-meters", Toast.LENGTH_LONG).show()
                },
                onImportGpx = {
                    Toast.makeText(context, "Accessing file explorer for GPX payload...", Toast.LENGTH_SHORT).show()
                },
                onExportGpx = {
                    val filesList = savedTracks
                    if (filesList.isEmpty()) {
                        Toast.makeText(context, "No completed walk path logs available to export.", Toast.LENGTH_SHORT).show()
                    } else {
                        val firstTrack = filesList.first()
                        Toast.makeText(context, "Exported: ${firstTrack.name} in GPX configuration format", Toast.LENGTH_LONG).show()
                    }
                },
                onManageLayers = { gisViewModel.updateUiState(MapUiState.LayersExpanded) },
                isOutdoorMode = isOutdoorMode,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }

        // --- LAYER 3: COMPACT BOTTOM BAR OR LANDSCAPE TAB SHEET (64.dp) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color.White else Color(0xFF0C100E)),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(64.dp)
                .border(0.5.dp, fontColor.copy(alpha = 0.08f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GisNavButton(
                    label = "Interactive Map",
                    icon = Icons.Default.Map,
                    selected = uiState is MapUiState.Idle,
                    onClick = { gisViewModel.updateUiState(MapUiState.Idle) },
                    accentColor = accentColor,
                    fontColor = fontColor
                )

                GisNavButton(
                    label = "Manage Layers",
                    icon = Icons.Default.Layers,
                    selected = uiState is MapUiState.LayersExpanded,
                    onClick = { gisViewModel.updateUiState(MapUiState.LayersExpanded) },
                    accentColor = accentColor,
                    fontColor = fontColor
                )

                GisNavButton(
                    label = "Station Data",
                    icon = Icons.Default.Inventory,
                    selected = uiState is MapUiState.DataExpanded,
                    onClick = { gisViewModel.updateUiState(MapUiState.DataExpanded) },
                    accentColor = accentColor,
                    fontColor = fontColor
                )

                GisNavButton(
                    label = "GNSS Position",
                    icon = Icons.Default.MyLocation,
                    selected = uiState is MapUiState.PositionExpanded,
                    onClick = { gisViewModel.updateUiState(MapUiState.PositionExpanded) },
                    accentColor = accentColor,
                    fontColor = fontColor
                )

                GisNavButton(
                    label = "GIS Settings",
                    icon = Icons.Default.Settings,
                    selected = uiState is MapUiState.SettingsExpanded,
                    onClick = { gisViewModel.updateUiState(MapUiState.SettingsExpanded) },
                    accentColor = accentColor,
                    fontColor = fontColor
                )
            }
        }

        // --- LAYER 4: DETAILED GIS BOTTOM SHEETS STATE DISPATCHER ---
        when (uiState) {
            is MapUiState.LayersExpanded -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                ) {
                    LayerManagerSheet(
                        activeBaseMap = activeBaseMap,
                        onBaseMapChange = { gisViewModel.setActiveBaseMap(it) },
                        showGrids = showGrids,
                        onToggleGrids = { gisViewModel.toggleGrids(it) },
                        showStations = showStations,
                        onToggleStations = { gisViewModel.toggleStations(it) },
                        showSymbols = showSymbols,
                        onToggleSymbols = { gisViewModel.toggleSymbols(it) },
                        showFaultLines = showFaultLines,
                        onToggleFaultLines = { gisViewModel.toggleFaultLines(it) },
                        onSeedDemoDataset = {
                            geologyViewModel.seedDemoDataset(context)
                            Toast.makeText(context, "Seeded Geological points successfully!", Toast.LENGTH_SHORT).show()
                            gisViewModel.updateUiState(MapUiState.Idle)
                        },
                        onDismiss = { gisViewModel.updateUiState(MapUiState.Idle) },
                        
                        // Advanced Offline & Opacity
                        activeBaseMapOpacity = activeBaseMapOpacity,
                        onBaseMapOpacityChange = { gisViewModel.setBaseMapOpacity(it) },
                        offlineModeForce = offlineModeForce,
                        onToggleOfflineModeForce = { gisViewModel.setOfflineModeForce(it) },
                        showOfflineBoundaries = showOfflineBoundaries,
                        onToggleOfflineBoundaries = { gisViewModel.setShowOfflineBoundaries(it) },
                        downloadedRegionsList = downloadedRegionsList,
                        onDownloadRegion = { name ->
                            scope.launch {
                                gisViewModel.downloadRegionForLocation(currentLat, currentLon, name)
                            }
                        },
                        onDeleteRegion = { id -> gisViewModel.deleteDownloadedRegion(id) },
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        downloadStatusText = downloadStatusText,
                        currentLat = currentLat,
                        currentLon = currentLon,

                        isOutdoorMode = isOutdoorMode
                    )
                }
            }

            is MapUiState.SettingsExpanded -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                ) {
                    SettingsSheet(
                        isOutdoorMode = isOutdoorMode,
                        onToggleOutdoorMode = { gisViewModel.setOutdoorMode(it) },
                        simWalkStepSize = simWalkStepSize,
                        onSimStepSizeChange = { gisViewModel.setSimWalkStepSize(it) },
                        onResetDatabase = {
                            geologyViewModel.clearAllData()
                            Toast.makeText(context, "GIS storage cache purged successfully!", Toast.LENGTH_SHORT).show()
                            gisViewModel.updateUiState(MapUiState.Idle)
                        },
                        onDismiss = { gisViewModel.updateUiState(MapUiState.Idle) }
                    )
                }
            }

            is MapUiState.ObservationCreationMode -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                ) {
                    GeologicalObservationSheet(
                        stationId = stationsToShow.firstOrNull()?.id ?: 1,
                        onDismiss = { gisViewModel.updateUiState(MapUiState.Idle) },
                        onSave = { obs ->
                            geologyViewModel.saveObservation(
                                stationId = obs.stationId,
                                obsType = obs.observationType,
                                notes = obs.notes ?: "",
                                photoPath = obs.photoPath,
                                lithType = obs.lithologyType,
                                texture = obs.texture,
                                color = obs.color,
                                minStyle = obs.minStyle,
                                onComplete = {
                                    Toast.makeText(context, "Observation committed successfully!", Toast.LENGTH_SHORT).show()
                                    gisViewModel.updateUiState(MapUiState.Idle)
                                }
                            )
                        },
                        isOutdoorMode = isOutdoorMode
                    )
                }
            }

            is MapUiState.SampleCreationMode -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                ) {
                    SamplePointSheet(
                        latitude = currentLat,
                        longitude = currentLon,
                        observationId = allObservationsList.firstOrNull()?.id ?: 1,
                        onDismiss = { gisViewModel.updateUiState(MapUiState.Idle) },
                        onSave = { smp ->
                            geologyViewModel.saveSample(smp) {
                                Toast.makeText(context, "Ore sample specimen recorded!", Toast.LENGTH_SHORT).show()
                                gisViewModel.updateUiState(MapUiState.Idle)
                            }
                        },
                        isOutdoorMode = isOutdoorMode
                    )
                }
            }

            is MapUiState.MeasurementMode -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                ) {
                    StructuralMeasurementSheet(
                        compassHeading = currentHeading,
                        stationId = stationsToShow.firstOrNull()?.id ?: 1,
                        onDismiss = { gisViewModel.updateUiState(MapUiState.Idle) },
                        onSave = { obs ->
                            geologyViewModel.saveObservation(
                                stationId = obs.stationId,
                                obsType = obs.observationType,
                                notes = obs.notes ?: "",
                                photoPath = obs.photoPath,
                                structureType = obs.structureType,
                                strike = obs.strike,
                                dip = obs.dip,
                                dipDirection = obs.dipDirection,
                                onComplete = {
                                    Toast.makeText(context, "Bedding attitude committed successfully!", Toast.LENGTH_SHORT).show()
                                    gisViewModel.updateUiState(MapUiState.Idle)
                                }
                            )
                        },
                        isOutdoorMode = isOutdoorMode
                    )
                }
            }

            is MapUiState.DataExpanded -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color.White else Color(0xFF141917)),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .border(0.5.dp, fontColor.copy(alpha = 0.15f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EXPLORATION OUTPOST STATS (${stationsToShow.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                                IconButton(onClick = { gisViewModel.updateUiState(MapUiState.Idle) }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = fontColor)
                                }
                            }

                            Divider(color = fontColor.copy(alpha = 0.1f))

                            if (stationsToShow.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No logged stations on record active.", fontSize = 12.sp, color = fontColor.copy(alpha = 0.5f))
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(stationsToShow) { station ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(fontColor.copy(alpha = 0.03f))
                                                .clickable {
                                                    navigationTargetStation = station
                                                    lockGpsCenter = false
                                                    gisViewModel.updateUiState(MapUiState.Idle)
                                                }
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = "Station: " + station.stationCode, fontWeight = FontWeight.Bold, color = fontColor, fontSize = 13.sp)
                                                Text(
                                                    text = "LAT: ${String.format(Locale.US, "%.5f", station.latitude)} | LON: ${String.format(Locale.US, "%.5f", station.longitude)}",
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = fontColor.copy(alpha = 0.6f)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Directions,
                                                contentDescription = "Pan To target",
                                                tint = accentColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is MapUiState.PositionExpanded -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color.White else Color(0xFF141917)),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .border(0.5.dp, fontColor.copy(alpha = 0.15f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GNSS METRICS TELEMETRY CORE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                                IconButton(onClick = { gisViewModel.updateUiState(MapUiState.Idle) }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = fontColor)
                                }
                            }

                            Divider(color = fontColor.copy(alpha = 0.1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Receiver Source Indicator:", fontSize = 11.sp, color = fontColor.copy(alpha = 0.6f))
                                    Text(
                                        text = if (isRealPositionEnabled) "ACTUAL HARDWARE GNSS" else "DEMO SIMULATOR CHIP",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = accentColor
                                    )
                                }

                                Switch(
                                    checked = isRealPositionEnabled,
                                    onCheckedChange = { isRealPositionEnabled = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Satellite Lock Suffix:", fontSize = 11.sp, color = fontColor.copy(alpha = 0.6f))
                                Text("WGS84-Ellipsoidal GPS + GLONASS Constellation", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fontColor)
                            }
                        }
                    }
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun GisNavButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    fontColor: Color
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) accentColor else fontColor.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label.split(" ").last(), // Take last word
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) accentColor else fontColor.copy(alpha = 0.5f)
        )
    }
}

// Preview Provider supporting modern compose tooling previews
@Preview(showBackground = true)
@Composable
fun PreviewMapScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Geo Field Pro GIS Engine Mockup", color = Color.White)
        }
    }
}
