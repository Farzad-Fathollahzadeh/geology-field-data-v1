package com.example.ui.gis

import android.widget.Toast
import com.google.accompanist.permissions.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.math.abs
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.text.SimpleDateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
    val importedFeatures by geologyViewModel.importedFeatures.collectAsStateWithLifecycle()
    val allObservationsList by geologyViewModel.allObservations.collectAsStateWithLifecycle()
    val allSamplesList by geologyViewModel.allSamples.collectAsStateWithLifecycle()
    val projects by geologyViewModel.projects.collectAsStateWithLifecycle()
    
    val stationsToShow = if (activeProjectStations.isNotEmpty()) activeProjectStations else allStationsList
    val isTrackRecordingActive by geologyViewModel.isTrackRecordingActive.collectAsStateWithLifecycle()
    val activePoints by geologyViewModel.activePoints.collectAsStateWithLifecycle()
    val liveLocation by geologyViewModel.liveLocation.collectAsStateWithLifecycle()

    // GIS Map UI settings from specialized MapViewModel
    val uiState by gisViewModel.uiState.collectAsStateWithLifecycle()
    val zoomLevel by gisViewModel.zoomLevel.collectAsStateWithLifecycle()
    val activeBaseMap by gisViewModel.activeBaseMap.collectAsStateWithLifecycle()
    val isOutdoorMode by gisViewModel.isOutdoorMode.collectAsStateWithLifecycle()
    val simWalkStepSize by gisViewModel.simWalkStepSize.collectAsStateWithLifecycle()

    val showGrids by gisViewModel.showGrids.collectAsStateWithLifecycle()
    val gridSizeSetting by gisViewModel.gridSizeSetting.collectAsStateWithLifecycle()
    val gridColorSetting by gisViewModel.gridColorSetting.collectAsStateWithLifecycle()
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

    val isImportingMbtiles by gisViewModel.isImportingMbtiles.collectAsStateWithLifecycle()
    val mbtilesImportProgress by gisViewModel.mbtilesImportProgress.collectAsStateWithLifecycle()
    val mbtilesImportStatus by gisViewModel.mbtilesImportStatus.collectAsStateWithLifecycle()

    val activeMbtiles by gisViewModel.activeMbtiles.collectAsStateWithLifecycle()
    val importedMbtilesFiles by gisViewModel.importedMbtilesFiles.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    val locationPermissionsState = com.google.accompanist.permissions.rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val isAnyLocationPermissionGranted = locationPermissionsState.permissions.any { it.status.isGranted }

    val gisFileImportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            geologyViewModel.importMapFile(context, it)
        }
    }

    val mbtilesImportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            gisViewModel.importMbtilesFile(context, it)
        }
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var selectedExportTrack by remember { mutableStateOf<com.example.Track?>(null) }
    var selectedExportFormat by remember { mutableStateOf("GPX") }

    var showCreatePlacemarkDialog by remember { mutableStateOf(false) }
    var showEditPlacemarkDialog by remember { mutableStateOf(false) }
    var selectedPlacemarkToEdit by remember { mutableStateOf<Station?>(null) }

    var placemarkNameInput by remember { mutableStateOf("") }
    var placemarkLatInput by remember { mutableStateOf("") }
    var placemarkLonInput by remember { mutableStateOf("") }
    var placemarkEleInput by remember { mutableStateOf("") }

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
    var showTopHud by remember { mutableStateOf(true) }
    var isQuickMapDeckExpanded by remember { mutableStateOf(false) }
    var showMoveSimulator by remember { mutableStateOf(false) }
    var showWaypointSelectorDialog by remember { mutableStateOf(false) }
    var showProjectStatsDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showQuickOpacitySlider by remember { mutableStateOf(false) }
    var isLeftToolsExpanded by remember { mutableStateOf(true) }
    var showElevationProfile by remember { mutableStateOf(false) }
    var elevationProfilePoints by remember { mutableStateOf<List<TrackPoint>>(emptyList()) }

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
                trackPoints = if (isTrackRecordingActive) activePoints else emptyList(),
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
                gridSizeSetting = gridSizeSetting,
                gridColorSetting = gridColorSetting,
                
                // Opacities & Offline
                baseMapOpacity = activeBaseMapOpacity,
                offlineModeForce = offlineModeForce,
                showOfflineBoundaries = showOfflineBoundaries,
                downloadedRegions = downloadedRegionsList,
                importedFeatures = importedFeatures,
                isSimpleMode = isSimpleMode,
                activeMbtilesList = activeMbtiles.toList(),

                modifier = Modifier.fillMaxSize()
            )

            // Draw elegant AlpineQuest-style Center Crosshair/Reticle over the map
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 1.5f
                    val crosshairColor = if (isOutdoorMode) Color(0xAA007A78) else Color(0xAA00FFE0)
                    
                    // Draw center target dot
                    drawCircle(color = crosshairColor, radius = 2f)
                    
                    // Draw precise circular ring
                    drawCircle(
                        color = crosshairColor,
                        radius = 8f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW)
                    )
                    
                    // Draw four tick marks pointing outwards
                    val centerPx = size.width / 2f
                    // Top tick
                    drawLine(color = crosshairColor, start = Offset(centerPx, 0f), end = Offset(centerPx, 8f), strokeWidth = strokeW)
                    // Bottom tick
                    drawLine(color = crosshairColor, start = Offset(centerPx, size.height - 8f), end = Offset(centerPx, size.height), strokeWidth = strokeW)
                    // Left tick
                    drawLine(color = crosshairColor, start = Offset(0f, centerPx), end = Offset(8f, centerPx), strokeWidth = strokeW)
                    // Right tick
                    drawLine(color = crosshairColor, start = Offset(size.width - 8f, centerPx), end = Offset(size.width, centerPx), strokeWidth = strokeW)
                }
            }

            // Real-time Range & Bearing HUD Tooltip (AlpineQuest GPS exploration feature)
            if (!lockGpsCenter) {
                val crosshairDist = remember(currentLat, currentLon, simulatorLat, simulatorLon) {
                    val r = 6371000.0 // meters
                    val dLat = Math.toRadians(simulatorLat - currentLat)
                    val dLon = Math.toRadians(simulatorLon - currentLon)
                    val a = sin(dLat / 2) * sin(dLat / 2) +
                            cos(Math.toRadians(currentLat)) * cos(Math.toRadians(simulatorLat)) *
                            sin(dLon / 2) * sin(dLon / 2)
                    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                    r * c
                }
                val crosshairBearing = remember(currentLat, currentLon, simulatorLat, simulatorLon) {
                    val phi1 = Math.toRadians(currentLat)
                    val phi2 = Math.toRadians(simulatorLat)
                    val deltaLambda = Math.toRadians(simulatorLon - currentLon)
                    val y = sin(deltaLambda) * cos(phi2)
                    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
                    val b = Math.toDegrees(atan2(y, x))
                    (b + 360.0) % 360.0
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 60.dp) // Float it neatly below the crosshair ring
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.85f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📏 " + (if (crosshairDist >= 1000.0) String.format(Locale.US, "%.2f km", crosshairDist / 1000.0) else String.format(Locale.US, "%.0f m", crosshairDist)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .height(10.dp)
                                    .width(1.dp)
                                    .background(Color.White.copy(alpha = 0.3f))
                            )
                            Text(
                                text = "🧭 ${String.format(Locale.US, "%.0f°", crosshairBearing)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // --- LAYER 1: ALPINEQUEST-STYLE UNIFIED GLASSMORPHIC HUD TELEMETRY BANNER ---
        if (uiState is MapUiState.Idle && showTopHud) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Unified HUD Box: adaptive premium background depending on theme
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = (if (isOutdoorMode) Color.White else Color(0xFF141A17)).copy(alpha = 0.92f)
                    ),
                    border = BorderStroke(1.dp, if (isOutdoorMode) Color(0xFFE0E0E0) else accentColor.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT SECTION: Mode switches with minimalist circular controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Simple/Advanced Toggle Button
                            IconButton(
                                onClick = { isSimpleMode = !isSimpleMode },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (isSimpleMode) Color(0xFFF5F5F5) else accentColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isSimpleMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "حالت نقشه",
                                    tint = if (isSimpleMode) Color.Gray else accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // UTM Grid Toggle Button
                            val showGridsState = showGrids
                            IconButton(
                                onClick = { gisViewModel.toggleGrids(!showGridsState) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (showGridsState) accentColor.copy(alpha = 0.15f) else Color(0xFFF5F5F5),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "شبکه UTM",
                                    tint = if (showGridsState) accentColor else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // MIDDLE SECTION: Real-time Coordinate Readout Box (Click to Cycle WGS84 / UTM / MGRS)
                        var coordinateFormatIndex by remember { mutableStateOf(0) } // 0: WGS84, 1: UTM, 2: MGRS
                        
                        // Convert to UTM dynamically
                        val utmCoord = remember(currentLat, currentLon) {
                            try {
                                com.example.UtmConverter.convertLatLngToUtm(currentLat, currentLon)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        // Simulated MGRS string
                        val mgrsString = remember(currentLat, currentLon, utmCoord) {
                            if (utmCoord == null) {
                                "MGRS converting..."
                            } else {
                                val z = utmCoord.zoneNumber
                                val band = utmCoord.zoneChar
                                val colLetters = "ABCDEFGHJKLMNPQRSTUVXYZ"
                                val colIndex = (utmCoord.easting / 100000).toInt()
                                val colLetter = colLetters[(colIndex - 1 + ((z - 1) % 3) * 8) % colLetters.length]
                                val rowLetters = "ABCDEFGHJKLMNPQRSTUV"
                                val rowIndex = (utmCoord.northing / 100000).toInt() % 20
                                val rowLetter = rowLetters[(rowIndex + (if (z % 2 != 0) 0 else 5)) % rowLetters.length]
                                val eStr = String.format(Locale.US, "%05d", (utmCoord.easting % 100000).toInt().coerceIn(0, 99999))
                                val nStr = String.format(Locale.US, "%05d", (utmCoord.northing % 100000).toInt().coerceIn(0, 99999))
                                "$z$band $colLetter$rowLetter $eStr $nStr"
                            }
                        }

                        val readoutText = when (coordinateFormatIndex) {
                            0 -> "${String.format(Locale.US, "%.5f", currentLat)}°N  ${String.format(Locale.US, "%.5f", currentLon)}°E"
                            1 -> {
                                if (utmCoord != null) {
                                    "${utmCoord.zoneString}  ${String.format(Locale.US, "%.0f", utmCoord.easting)}m E  ${String.format(Locale.US, "%.0f", utmCoord.northing)}m N"
                                } else {
                                    "UTM Converting..."
                                }
                            }
                            else -> "MGRS: $mgrsString"
                        }

                        val formatLabel = when (coordinateFormatIndex) {
                            0 -> "WGS84 (لمس جهت تغییر)"
                            1 -> "UTM Grid (لمس جهت تغییر)"
                            else -> "MGRS Grid (لمس جهت تغییر)"
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    coordinateFormatIndex = (coordinateFormatIndex + 1) % 3
                                }
                                .padding(vertical = 2.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = readoutText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = accentColor,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatLabel,
                                    fontSize = 7.5.sp,
                                    color = fontColor.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = "• ALT: ${currentEle.toInt()}m",
                                    fontSize = 7.5.sp,
                                    color = fontColor.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // RIGHT SECTION: GPS Status telemetry
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = "ACC: ${String.format(Locale.US, "%.1fm", currentAccuracy)}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentAccuracy < 3.0) Color(0xFF2E7D32) else Color(0xFFE65100),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "ZOOM: $zoomLevel",
                                    fontSize = 8.sp,
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            // Glowing green dot indicating perfect GPS fix
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (currentAccuracy < 3.0) Color(0xFF4CAF50) else Color(0xFFFF9800))
                            )
                        }
                    }
                }

                // Small scale bar row that floats neatly below the HUD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(1.dp))
                    ScaleBar(
                        zoomLevel = zoomLevel,
                        isOutdoorMode = isOutdoorMode
                    )
                }

                // AlpineQuest Waypoint Target Navigation HUD
            if (navigationTargetStation != null) {
                val target = navigationTargetStation!!
                val geodesicDist = remember(currentLat, currentLon, target) {
                    val r = 6371000.0 // meters
                    val dLat = Math.toRadians(target.latitude - currentLat)
                    val dLon = Math.toRadians(target.longitude - currentLon)
                    val a = sin(dLat / 2) * sin(dLat / 2) +
                            cos(Math.toRadians(currentLat)) * cos(Math.toRadians(target.latitude)) *
                            sin(dLon / 2) * sin(dLon / 2)
                    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                    r * c
                }

                val bearingToTarget = remember(currentLat, currentLon, target) {
                    val phi1 = Math.toRadians(currentLat)
                    val phi2 = Math.toRadians(target.latitude)
                    val deltaLambda = Math.toRadians(target.longitude - currentLon)
                    val y = sin(deltaLambda) * cos(phi2)
                    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
                    val b = Math.toDegrees(atan2(y, x))
                    (b + 360.0) % 360.0
                }

                val turnDeviation = remember(bearingToTarget, currentHeading) {
                    var diff = bearingToTarget - currentHeading
                    while (diff < -180.0) diff += 360.0
                    while (diff > 180.0) diff -= 360.0
                    diff
                }

                val directionIndicator = remember(bearingToTarget) {
                    val dirs = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
                    val idx = ((bearingToTarget + 11.25) / 22.5).toInt() % 16
                    dirs[idx]
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = (if (isOutdoorMode) Color.White else Color(0xFF0F1412)).copy(alpha = 0.9f)
                    ),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = "Target Direction",
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "NAV TARGET: ${target.stationCode}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "(${directionIndicator})",
                                        fontSize = 10.sp,
                                        color = fontColor.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DIST: " + (if (geodesicDist >= 1000.0) String.format(Locale.US, "%.2f km", geodesicDist / 1000.0) else String.format(Locale.US, "%.0f m", geodesicDist)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = fontColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "BEARING: ${String.format(Locale.US, "%.0f°", bearingToTarget)}",
                                        fontSize = 11.sp,
                                        color = fontColor.copy(alpha = 0.8f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (abs(turnDeviation) < 12.0) "🟢 ON TRACK" else if (turnDeviation > 0) "➡️ TURN R ${(turnDeviation).toInt()}°" else "⬅️ TURN L ${abs(turnDeviation).toInt()}°",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (abs(turnDeviation) < 12.0) Color(0xFF4CAF50) else Color(0xFFFFA726)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { navigationTargetStation = null },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Navigation",
                                tint = Color.Red.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

        // --- LAYER 2: UNIFIED EXPANDABLE GIS MENU & CONTROLS ---
        if (uiState is MapUiState.Idle) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp, start = 12.dp, end = 12.dp)
            ) {
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

                // Single unified menu button replacing both left and right crowded columns
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
                        gisFileImportLauncher.launch("*/*")
                    },
                    onExportGpx = {
                        showExportDialog = true
                    },
                    onManageLayers = { gisViewModel.updateUiState(MapUiState.LayersExpanded) },
                    isOutdoorMode = isOutdoorMode,
                    zoomLevel = zoomLevel,
                    onZoomIn = { gisViewModel.setZoomLevel(zoomLevel + 1) },
                    onZoomOut = { gisViewModel.setZoomLevel(zoomLevel - 1) },
                    showMoveSimulator = showMoveSimulator,
                    onToggleMoveSimulator = { showMoveSimulator = !showMoveSimulator },
                    baseMapOpacity = activeBaseMapOpacity,
                    onBaseMapOpacityChange = { gisViewModel.setBaseMapOpacity(it) },
                    showGrids = showGrids,
                    onToggleGrids = { gisViewModel.toggleGrids(it) },
                    isSimpleMode = isSimpleMode,
                    onToggleSimpleMode = { isSimpleMode = !isSimpleMode },
                    onShowWaypointSelector = { showWaypointSelectorDialog = true },
                    onShowHelp = { showHelpDialog = true },
                    onShowStats = { showProjectStatsDialog = true },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                )

            // Real-time Track Recording Status Widget
            if (isTrackRecordingActive) {
                val activeTrackPoints = activePoints
                val recordingDurationSec by geologyViewModel.recordingDurationSec.collectAsStateWithLifecycle()
                val isTrackRecordingPaused by geologyViewModel.isTrackRecordingPaused.collectAsStateWithLifecycle()
                
                val trackLength = remember(activeTrackPoints) {
                    if (activeTrackPoints.size < 2) 0.0
                    else {
                        var dist = 0.0
                        for (i in 0 until activeTrackPoints.size - 1) {
                            val p1 = activeTrackPoints[i]
                            val p2 = activeTrackPoints[i + 1]
                            val r = 6371000.0 // meters
                            val dLat = Math.toRadians(p2.lat - p1.lat)
                            val dLon = Math.toRadians(p2.lon - p1.lon)
                            val a = sin(dLat / 2) * sin(dLat / 2) +
                                    cos(Math.toRadians(p1.lat)) * cos(Math.toRadians(p2.lat)) *
                                    sin(dLon / 2) * sin(dLon / 2)
                            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                            dist += r * c
                        }
                        dist
                    }
                }
                
                val avgSpeedKmh = remember(trackLength, recordingDurationSec) {
                    if (recordingDurationSec < 2) 0.0
                    else {
                        val hours = recordingDurationSec / 3600.0
                        val km = trackLength / 1000.0
                        km / hours
                    }
                }
                
                val durationFormatted = remember(recordingDurationSec) {
                    val h = recordingDurationSec / 3600
                    val m = (recordingDurationSec % 3600) / 60
                    val s = recordingDurationSec % 60
                    String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = (if (isOutdoorMode) Color.White else Color(0xFF141A17)).copy(alpha = 0.9f)
                    ),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = if (!isSimpleMode) 170.dp else 110.dp)
                        .width(180.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val blinkAlpha = if (recordingDurationSec % 2 == 0L) 1f else 0.3f
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Red.copy(alpha = blinkAlpha))
                                )
                                Text(
                                    text = if (isTrackRecordingPaused) "PAUSED" else "RECORDING",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTrackRecordingPaused) Color.Yellow else Color.Red,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            Text(
                                text = durationFormatted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = fontColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Divider(color = fontColor.copy(alpha = 0.1f))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("DISTANCE", fontSize = 8.sp, color = fontColor.copy(alpha = 0.5f))
                                Text(
                                    text = if (trackLength >= 1000.0) String.format(Locale.US, "%.2f km", trackLength / 1000.0) else String.format(Locale.US, "%.0f m", trackLength),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = fontColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("AVG SPEED", fontSize = 8.sp, color = fontColor.copy(alpha = 0.5f))
                                Text(
                                    text = String.format(Locale.US, "%.1f km/h", avgSpeedKmh),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = fontColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (isTrackRecordingPaused) {
                                        geologyViewModel.resumeRecordingTrack()
                                    } else {
                                        geologyViewModel.pauseRecordingTrack()
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTrackRecordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = "Pause or resume",
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    showElevationProfile = !showElevationProfile
                                    elevationProfilePoints = emptyList() // clear so it tracks activePoints
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terrain,
                                    contentDescription = "نمایش پروفایل ارتفاعی",
                                    tint = if (showElevationProfile && elevationProfilePoints.isEmpty()) accentColor else fontColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    geologyViewModel.stopRecordingTrack { pointsJson, length, duration ->
                                        geologyViewModel.saveTrack(
                                            name = "GisWalk_" + SimpleDateFormat("HHmm", Locale.US).format(java.util.Date()),
                                            pointsJson = pointsJson,
                                            lengthMeters = length,
                                            durationSeconds = duration
                                        )
                                        Toast.makeText(context, "Track saved successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- LAYER 2: MINIMALIST QUICK MAP DECK (DECLUTTERING CONTROL DECK) ---
            if (uiState is MapUiState.Idle && !isSimpleMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 80.dp, start = 12.dp)
                ) {
                if (!isQuickMapDeckExpanded) {
                    // Small circular button when collapsed
                    IconButton(
                        onClick = { isQuickMapDeckExpanded = true },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = (if (isOutdoorMode) Color.White else Color(0xFF141A17)).copy(alpha = 0.9f),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.5.dp,
                                color = accentColor.copy(alpha = 0.7f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "کنترل لایه‌ها و خلوت‌سازی",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // Expanded translucent control deck
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = (if (isOutdoorMode) Color.White else Color(0xFF141A17)).copy(alpha = 0.92f)
                        ),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .wrapContentSize()
                            .widthIn(max = 300.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "تنظیمات لایه‌ها / Map Layers",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = fontColor
                                    )
                                }
                                IconButton(
                                    onClick = { isQuickMapDeckExpanded = false },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "بستن",
                                        tint = fontColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Divider(color = fontColor.copy(alpha = 0.1f))

                            // Grid of toggles
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 1. Placemarks/Stations Layer Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { gisViewModel.toggleStations(!showStations) }
                                        .background(if (showStations) accentColor.copy(alpha = 0.1f) else Color.Transparent)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = if (showStations) accentColor else fontColor.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "ایستگاه‌ها / Stations",
                                            fontSize = 10.sp,
                                            color = fontColor
                                        )
                                    }
                                    Switch(
                                        checked = showStations,
                                        onCheckedChange = { gisViewModel.toggleStations(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = accentColor,
                                            uncheckedThumbColor = fontColor.copy(alpha = 0.5f),
                                            uncheckedTrackColor = fontColor.copy(alpha = 0.1f)
                                        ),
                                        modifier = Modifier.scale(0.7f)
                                    )
                                }

                                // 2. Geological Symbols Layer Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { gisViewModel.toggleSymbols(!showSymbols) }
                                        .background(if (showSymbols) accentColor.copy(alpha = 0.1f) else Color.Transparent)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Category,
                                            contentDescription = null,
                                            tint = if (showSymbols) accentColor else fontColor.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "نمادها / Symbols",
                                            fontSize = 10.sp,
                                            color = fontColor
                                        )
                                    }
                                    Switch(
                                        checked = showSymbols,
                                        onCheckedChange = { gisViewModel.toggleSymbols(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = accentColor,
                                            uncheckedThumbColor = fontColor.copy(alpha = 0.5f),
                                            uncheckedTrackColor = fontColor.copy(alpha = 0.1f)
                                        ),
                                        modifier = Modifier.scale(0.7f)
                                    )
                                }

                                // 3. Fault Lines Layer Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { gisViewModel.toggleFaultLines(!showFaultLines) }
                                        .background(if (showFaultLines) accentColor.copy(alpha = 0.1f) else Color.Transparent)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingFlat,
                                            contentDescription = null,
                                            tint = if (showFaultLines) accentColor else fontColor.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "خطوط گسل / Faults",
                                            fontSize = 10.sp,
                                            color = fontColor
                                        )
                                    }
                                    Switch(
                                        checked = showFaultLines,
                                        onCheckedChange = { gisViewModel.toggleFaultLines(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = accentColor,
                                            uncheckedThumbColor = fontColor.copy(alpha = 0.5f),
                                            uncheckedTrackColor = fontColor.copy(alpha = 0.1f)
                                        ),
                                        modifier = Modifier.scale(0.7f)
                                    )
                                }

                                // 4. Walk simulator controller toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showMoveSimulator = !showMoveSimulator }
                                        .background(if (showMoveSimulator) accentColor.copy(alpha = 0.1f) else Color.Transparent)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsWalk,
                                            contentDescription = null,
                                            tint = if (showMoveSimulator) accentColor else fontColor.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "شبیه‌ساز / Walk D-Pad",
                                            fontSize = 10.sp,
                                            color = fontColor
                                        )
                                    }
                                    Switch(
                                        checked = showMoveSimulator,
                                        onCheckedChange = { showMoveSimulator = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = accentColor,
                                            uncheckedThumbColor = fontColor.copy(alpha = 0.5f),
                                            uncheckedTrackColor = fontColor.copy(alpha = 0.1f)
                                        ),
                                        modifier = Modifier.scale(0.7f)
                                    )
                                }

                                // 5. Show Top HUD Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showTopHud = !showTopHud }
                                        .background(if (showTopHud) accentColor.copy(alpha = 0.1f) else Color.Transparent)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (showTopHud) accentColor else fontColor.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "پنل بالا / Top HUD Info",
                                            fontSize = 10.sp,
                                            color = fontColor
                                        )
                                    }
                                    Switch(
                                        checked = showTopHud,
                                        onCheckedChange = { showTopHud = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = accentColor,
                                            uncheckedThumbColor = fontColor.copy(alpha = 0.5f),
                                            uncheckedTrackColor = fontColor.copy(alpha = 0.1f)
                                        ),
                                        modifier = Modifier.scale(0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

            // 📊 1. Exploration Survey Stats Dialog (داشبورد آمارهای اکتشافی پروژه)
            if (showProjectStatsDialog) {
                AlertDialog(
                    onDismissRequest = { showProjectStatsDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showProjectStatsDialog = false }) {
                            Text("تایید و بستن / OK", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    title = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                            Text(
                                text = "داشبورد آماری اکتشاف / Exploration Stats",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = fontColor
                            )
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "خلاصه وضعیت داده‌های ثبت‌شده در این پیمایش صحرایی:",
                                fontSize = 11.sp,
                                color = fontColor.copy(alpha = 0.7f)
                            )
                            Divider(color = fontColor.copy(alpha = 0.1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("پروژه فعال / Active Project:", fontSize = 11.sp, color = fontColor.copy(alpha = 0.6f))
                                Text(activeProject?.name ?: "پروژه عمومی / Default", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("شناسه پروژه / Project ID:", fontSize = 11.sp, color = fontColor.copy(alpha = 0.6f))
                                Text(activeProject?.let { "PRJ-${it.id}" } ?: "DEF-001", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fontColor)
                            }

                            Divider(color = fontColor.copy(alpha = 0.05f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                    Text("ایستگاه‌های این پروژه / Project Stations:", fontSize = 11.sp, color = fontColor)
                                }
                                Text("${activeProjectStations.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Map, contentDescription = null, tint = fontColor.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                    Text("کل ایستگاه‌های کل سیستم / Total Stations:", fontSize = 11.sp, color = fontColor.copy(alpha = 0.7f))
                                }
                                Text("${allStationsList.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fontColor)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Category, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                    Text("نمونه‌های سنگ و خاک / Geologic Samples:", fontSize = 11.sp, color = fontColor)
                                }
                                Text("${allSamplesList.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EditNote, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                    Text("کل مشاهدات صحرایی / Field Observations:", fontSize = 11.sp, color = fontColor)
                                }
                                Text("${allObservationsList.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fontColor)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Route, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                    Text("مسیرهای جی‌پی‌اس / Tracks Recorded:", fontSize = 11.sp, color = fontColor)
                                }
                                Text("${savedTracks.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                        }
                    }
                )
            }

            // 🗺️ 2. GIS Interactive Guide Dialog (راهنمای جامع تعاملی و آموزشی)
            if (showHelpDialog) {
                var activeHelpTab by remember { mutableStateOf(0) }
                AlertDialog(
                    onDismissRequest = { showHelpDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showHelpDialog = false }) {
                            Text("متوجه شدم / Close", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    title = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                            Text(
                                text = "راهنمای استفاده از سیستم / GIS Tutorial",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = fontColor
                            )
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Tab Rows for Selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val tabs = listOf("آفلاین", "UTM", "برداشت", "ناوبری")
                                tabs.forEachIndexed { index, label ->
                                    val isSel = activeHelpTab == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) accentColor else fontColor.copy(alpha = 0.05f))
                                            .clickable { activeHelpTab = index }
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else fontColor.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            Divider(color = fontColor.copy(alpha = 0.1f))

                            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 280.dp)) {
                                when (activeHelpTab) {
                                    0 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("📦 نقشه‌های آفلاین و MBTiles:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                            Text(
                                                "این نرم‌افزار به زمین‌شناسان اجازه می‌دهد تا در نقاط کور صحرایی بدون اینترنت از نقشه‌ها استفاده کنند.\\n" +
                                                "۱. فایل‌های نقشه با فرمت .mbtiles را کپی و از بخش مدیریت لایه‌ها لود کنید.\\n" +
                                                "۲. همچنین می‌توانید محدوده فعلی را از پنل دانلود کرده و به صورت آفلاین کش (Cache) کنید تا در فیلد ذخیره بماند.",
                                                fontSize = 10.sp, color = fontColor.copy(alpha = 0.8f), lineHeight = 14.sp
                                            )
                                        }
                                    }
                                    1 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("🌐 گرید UTM و فرمت‌های مختصاتی:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                            Text(
                                                "۱. با لمس نوار اطلاعات بالایی صفحه، می‌توانید فرمت نمایش مختصات را بین WGS84 دسی‌مال، شبکه UTM و فرمت نظامی MGRS تغییر دهید.\\n" +
                                                "۲. از منوی کشویی جی‌آی‌اس، می‌توانید خطوط شبکه گرید UTM را فعال کنید تا همزمان خطوط فاصله روی نقشه تصویر شوند.",
                                                fontSize = 10.sp, color = fontColor.copy(alpha = 0.8f), lineHeight = 14.sp
                                            )
                                        }
                                    }
                                    2 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("🔨 ثبت لایه‌های ساختاری و زمین‌شناسی:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                            Text(
                                                "برای ثبت داده‌ها در ایستگاه فعلی:\\n" +
                                                "۱. با زدن دکمه ثبت ایستگاه جدید، یک نقطه لیتولوژی ایجاد کنید.\\n" +
                                                "۲. سپس سنجش‌های ساختاری شیب و امتداد (Strike/Dip)، نمونه خاک/سنگ و یا اطلاعات گمانه حفاری (Borehole) را با سنسور یا کیبورد ثبت کنید تا بلافاصله روی نقشه با علائم استاندارد نمایش داده شوند.",
                                                fontSize = 10.sp, color = fontColor.copy(alpha = 0.8f), lineHeight = 14.sp
                                            )
                                        }
                                    }
                                    3 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("📍 ناوبری، ردیابی و فایل‌های بیرونی:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                            Text(
                                                "۱. ضبط مسیر: با زدن ضبط در منوی کشویی، پیمایش شما با طول مسافت و سرعت متوسط ردیابی و ثبت می‌گردد.\\n" +
                                                "۲. ایمپورت/اکسپورت: فایل‌های KML/GPX مسیرها را وارد نقشه کنید.\\n" +
                                                "۳. سنجش فاصله و مساحت: فاصله‌های ژئودزیک را به کمک ابزارهای پیشرفته اندازه بگیرید.",
                                                fontSize = 10.sp, color = fontColor.copy(alpha = 0.8f), lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }

            // AlpineQuest Waypoint Quick Navigation Target Selector Dialog
            if (showWaypointSelectorDialog) {
                AlertDialog(
                    onDismissRequest = { showWaypointSelectorDialog = false },
                    title = {
                        Text(
                            text = "ALPINEQUEST WAYPOINT SELECTOR",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                        ) {
                            Text(
                                text = "انتخاب ایستگاه برای ناوبری و جهت‌یابی روی نقشه:",
                                fontSize = 11.sp,
                                color = fontColor.copy(alpha = 0.7f)
                            )
                            
                            Divider(color = fontColor.copy(alpha = 0.1f))

                            if (stationsToShow.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("No outcrop waypoints active.", fontSize = 12.sp, color = fontColor.copy(alpha = 0.5f))
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(stationsToShow, key = { it.id }) { station ->
                                        val dist = remember(currentLat, currentLon, station) {
                                            val r = 6371000.0 // meters
                                            val dLat = Math.toRadians(station.latitude - currentLat)
                                            val dLon = Math.toRadians(station.longitude - currentLon)
                                            val a = sin(dLat / 2) * sin(dLat / 2) +
                                                    cos(Math.toRadians(currentLat)) * cos(Math.toRadians(station.latitude)) *
                                                    sin(dLon / 2) * sin(dLon / 2)
                                            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                                            r * c
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(fontColor.copy(alpha = 0.04f))
                                                .clickable {
                                                    navigationTargetStation = station
                                                    lockGpsCenter = false
                                                    showWaypointSelectorDialog = false
                                                }
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = station.stationCode,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = fontColor
                                                )
                                                Text(
                                                    text = "LAT: ${String.format(Locale.US, "%.4f", station.latitude)} | LON: ${String.format(Locale.US, "%.4f", station.longitude)}",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = fontColor.copy(alpha = 0.5f)
                                                )
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(accentColor.copy(alpha = 0.12f))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = if (dist >= 1000.0) String.format(Locale.US, "%.1f km", dist / 1000.0) else String.format(Locale.US, "%.0f m", dist),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showWaypointSelectorDialog = false }) {
                            Text("CLOSE", color = fontColor.copy(alpha = 0.6f))
                        }
                    },
                    containerColor = if (isOutdoorMode) Color.White else Color(0xFF161E1B)
                )
            }
        }
    }

        // --- DIMMABLE BACKGROUND OVERLAY FOR SHEETS ---
        AnimatedVisibility(
            visible = uiState !is MapUiState.Idle,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { gisViewModel.updateUiState(MapUiState.Idle) }
            )
        }

        // --- LAYER 3: PREMIUM M3 BILINGUAL BOTTOM NAVIGATION BAR ---
        Card(
            colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color.White else Color(0xFF0C100E)),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .border(
                    BorderStroke(0.5.dp, fontColor.copy(alpha = 0.12f)),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.height(64.dp)
            ) {
                // Tab 1: Map
                NavigationBarItem(
                    selected = uiState is MapUiState.Idle,
                    onClick = { gisViewModel.updateUiState(MapUiState.Idle) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "نقشه",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text("نقشه", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    },
                    alwaysShowLabel = false,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor.copy(alpha = 0.12f),
                        unselectedIconColor = fontColor.copy(alpha = 0.4f),
                        unselectedTextColor = fontColor.copy(alpha = 0.4f)
                    )
                )

                // Tab 2: Layers & Data
                NavigationBarItem(
                    selected = uiState is MapUiState.LayersExpanded || uiState is MapUiState.DataExpanded,
                    onClick = {
                        if (uiState is MapUiState.LayersExpanded) {
                            gisViewModel.updateUiState(MapUiState.DataExpanded)
                        } else if (uiState is MapUiState.DataExpanded) {
                            gisViewModel.updateUiState(MapUiState.LayersExpanded)
                        } else {
                            gisViewModel.updateUiState(MapUiState.LayersExpanded)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "لایه‌ها و داده‌ها",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text("لایه‌ها و داده‌ها", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    },
                    alwaysShowLabel = false,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor.copy(alpha = 0.12f),
                        unselectedIconColor = fontColor.copy(alpha = 0.4f),
                        unselectedTextColor = fontColor.copy(alpha = 0.4f)
                    )
                )

                // Tab 3: Settings & GNSS
                NavigationBarItem(
                    selected = uiState is MapUiState.SettingsExpanded || uiState is MapUiState.PositionExpanded,
                    onClick = {
                        if (uiState is MapUiState.SettingsExpanded) {
                            gisViewModel.updateUiState(MapUiState.PositionExpanded)
                        } else if (uiState is MapUiState.PositionExpanded) {
                            gisViewModel.updateUiState(MapUiState.SettingsExpanded)
                        } else {
                            gisViewModel.updateUiState(MapUiState.SettingsExpanded)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "تنظیمات",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text("تنظیمات", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    },
                    alwaysShowLabel = false,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor.copy(alpha = 0.12f),
                        unselectedIconColor = fontColor.copy(alpha = 0.4f),
                        unselectedTextColor = fontColor.copy(alpha = 0.4f)
                    )
                )
            }
        }

        // --- LAYER 4: DETAILED SLIDE-UP BILINGUAL GIS DRAWER SHEET STATE DISPATCHER ---
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                slideInVertically(initialOffsetY = { it }) + fadeIn() togetherWith
                slideOutVertically(targetOffsetY = { it }) + fadeOut()
            },
            label = "SheetSlideAnimation",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 76.dp)
                .navigationBarsPadding()
        ) { state ->
            when (state) {
                is MapUiState.LayersExpanded, is MapUiState.DataExpanded -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color.White else Color(0xFF101412)),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .border(BorderStroke(0.5.dp, fontColor.copy(alpha = 0.15f)), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(fontColor.copy(alpha = 0.05f))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (state is MapUiState.LayersExpanded) accentColor else Color.Transparent)
                                            .clickable { gisViewModel.updateUiState(MapUiState.LayersExpanded) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "لایه‌ها / Layers",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state is MapUiState.LayersExpanded) Color.White else fontColor.copy(alpha = 0.7f)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (state is MapUiState.DataExpanded) accentColor else Color.Transparent)
                                            .clickable { gisViewModel.updateUiState(MapUiState.DataExpanded) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "ایستگاه‌ها / Data",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state is MapUiState.DataExpanded) Color.White else fontColor.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { gisViewModel.updateUiState(MapUiState.Idle) }, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = fontColor)
                                }
                            }
                            Divider(color = fontColor.copy(alpha = 0.1f))

                            if (state is MapUiState.LayersExpanded) {
                                Box(modifier = Modifier.fillMaxSize()) {
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
                                        activeBaseMapOpacity = activeBaseMapOpacity,
                                        onBaseMapOpacityChange = { gisViewModel.setBaseMapOpacity(it) },
                                        offlineModeForce = offlineModeForce,
                                        onToggleOfflineModeForce = { gisViewModel.setOfflineModeForce(it) },
                                        showOfflineBoundaries = showOfflineBoundaries,
                                        onToggleOfflineBoundaries = { gisViewModel.setShowOfflineBoundaries(it) },
                                        downloadedRegionsList = downloadedRegionsList,
                                        onDownloadRegion = { name, providers, radius, zoom, lat, lon ->
                                            scope.launch {
                                                gisViewModel.downloadRegionForLocation(
                                                    context = context,
                                                    lat = lat,
                                                    lon = lon,
                                                    name = name,
                                                    providers = providers,
                                                    radiusKm = radius,
                                                    maxZoom = zoom
                                                )
                                            }
                                        },
                                        onDeleteRegion = { id -> gisViewModel.deleteDownloadedRegion(id) },
                                        isDownloading = isDownloading,
                                        downloadProgress = downloadProgress,
                                        downloadStatusText = downloadStatusText,
                                        currentLat = currentLat,
                                        currentLon = currentLon,
                                        savedTracks = savedTracks,
                                        onDeleteTrack = { trackId ->
                                            geologyViewModel.deleteTrack(trackId)
                                            Toast.makeText(context, "مسیر حرکت حذف شد.", Toast.LENGTH_SHORT).show()
                                        },
                                        onSelectTrackForElevationProfile = { track ->
                                            val parsed = com.example.parsePointsJson(track.pointsJson)
                                            elevationProfilePoints = parsed
                                            showElevationProfile = true
                                            gisViewModel.updateUiState(MapUiState.Idle)
                                            Toast.makeText(context, "نمودار ارتفاعی ${track.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        onExportTrack = { track ->
                                            val parsed = com.example.parsePointsJson(track.pointsJson)
                                            val gpxContent = com.example.generateGpxString(track.name, parsed)
                                            com.example.shareFileWithApps(context, "${track.name.replace(" ", "_")}.gpx", "application/gpx+xml", gpxContent)
                                        },
                                        isOutdoorMode = isOutdoorMode,
                                        activeMbtiles = activeMbtiles,
                                        importedMbtilesFiles = importedMbtilesFiles,
                                        onToggleMbtilesActive = { file, active ->
                                            gisViewModel.toggleMbtilesActive(file, active)
                                        },
                                        onMbtilesOpacityChange = { file, opacity ->
                                            gisViewModel.setMbtilesOpacity(file, opacity)
                                        },
                                        onDeleteMbtiles = { file ->
                                            gisViewModel.deleteMbtilesFile(file)
                                        },
                                        onImportMbtilesClick = {
                                            mbtilesImportLauncher.launch("*/*")
                                        }
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                placemarkNameInput = "Point-${stationsToShow.size + 1}"
                                                placemarkLatInput = String.format(Locale.US, "%.6f", currentLat)
                                                placemarkLonInput = String.format(Locale.US, "%.6f", currentLon)
                                                placemarkEleInput = if (liveLocation != null) String.format(Locale.US, "%.1f", liveLocation!!.altitude) else "1320.0"
                                                showCreatePlacemarkDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1.3f).padding(end = 4.dp).height(36.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.AddLocationAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ثبت نقطه / New Point", fontSize = 10.sp, color = Color.White)
                                        }

                                        FilterChip(
                                            selected = showStations,
                                            onClick = { gisViewModel.toggleStations(!showStations) },
                                            label = { Text("نمایش روی نقشه", fontSize = 10.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (showStations) Icons.Default.Check else Icons.Default.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = accentColor.copy(alpha = 0.15f),
                                                selectedLabelColor = accentColor
                                            ),
                                            modifier = Modifier.height(32.dp)
                                        )
                                    }

                                    if (stationsToShow.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("هیچ نقطه‌ای ثبت نشده است | No active placemarks on record.", fontSize = 11.sp, color = fontColor.copy(alpha = 0.5f))
                                        }
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(stationsToShow, key = { it.id }) { station ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(fontColor.copy(alpha = 0.03f))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                navigationTargetStation = station
                                                                lockGpsCenter = false
                                                                gisViewModel.updateUiState(MapUiState.Idle)
                                                                Toast.makeText(context, "تمرکز روی نقطه: ${station.stationCode}", Toast.LENGTH_SHORT).show()
                                                            }
                                                    ) {
                                                        Text(text = "ایستگاه " + station.stationCode, fontWeight = FontWeight.Bold, color = fontColor, fontSize = 12.sp)
                                                        Text(
                                                            text = "LAT: ${String.format(Locale.US, "%.5f", station.latitude)} | LON: ${String.format(Locale.US, "%.5f", station.longitude)}",
                                                            fontSize = 10.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = fontColor.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        IconButton(
                                                            onClick = {
                                                                selectedPlacemarkToEdit = station
                                                                placemarkNameInput = station.stationCode
                                                                placemarkLatInput = station.latitude.toString()
                                                                placemarkLonInput = station.longitude.toString()
                                                                placemarkEleInput = station.elevation.toString()
                                                                showEditPlacemarkDialog = true
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = "Edit point",
                                                                tint = accentColor,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                geologyViewModel.deleteStation(station.id, station.projectId)
                                                                Toast.makeText(context, "نقطه حذف شد | Point deleted", Toast.LENGTH_SHORT).show()
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete point",
                                                                tint = Color.Red.copy(alpha = 0.8f),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                navigationTargetStation = station
                                                                lockGpsCenter = false
                                                                gisViewModel.updateUiState(MapUiState.Idle)
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Directions,
                                                                contentDescription = "Navigate to point",
                                                                tint = accentColor,
                                                                modifier = Modifier.size(16.dp)
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

                is MapUiState.SettingsExpanded, is MapUiState.PositionExpanded -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color.White else Color(0xFF101412)),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .border(BorderStroke(0.5.dp, fontColor.copy(alpha = 0.15f)), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(fontColor.copy(alpha = 0.05f))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (state is MapUiState.SettingsExpanded) accentColor else Color.Transparent)
                                            .clickable { gisViewModel.updateUiState(MapUiState.SettingsExpanded) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "تنظیمات / Settings",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state is MapUiState.SettingsExpanded) Color.White else fontColor.copy(alpha = 0.7f)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (state is MapUiState.PositionExpanded) accentColor else Color.Transparent)
                                            .clickable { gisViewModel.updateUiState(MapUiState.PositionExpanded) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "سیگنال GNSS / Signal",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state is MapUiState.PositionExpanded) Color.White else fontColor.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { gisViewModel.updateUiState(MapUiState.Idle) }, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = fontColor)
                                }
                            }
                            Divider(color = fontColor.copy(alpha = 0.1f))

                            if (state is MapUiState.SettingsExpanded) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    SettingsSheet(
                                        geologyViewModel = geologyViewModel,
                                        isOutdoorMode = isOutdoorMode,
                                        onToggleOutdoorMode = { gisViewModel.setOutdoorMode(it) },
                                        simWalkStepSize = simWalkStepSize,
                                        onSimStepSizeChange = { gisViewModel.setSimWalkStepSize(it) },
                                        onResetDatabase = {
                                            geologyViewModel.clearAllData()
                                            Toast.makeText(context, "GIS storage cache purged successfully!", Toast.LENGTH_SHORT).show()
                                            gisViewModel.updateUiState(MapUiState.Idle)
                                        },
                                        onDismiss = { gisViewModel.updateUiState(MapUiState.Idle) },
                                        isImportingMbtiles = isImportingMbtiles,
                                        mbtilesImportProgress = mbtilesImportProgress,
                                        mbtilesImportStatus = mbtilesImportStatus,
                                        onImportMbtiles = { gisViewModel.importMbtilesFile(context, it) },
                                        gridSizeSetting = gridSizeSetting,
                                        onGridSizeSettingChange = { gisViewModel.setGridSizeSetting(it) },
                                        gridColorSetting = gridColorSetting,
                                        onGridColorSettingChange = { gisViewModel.setGridColorSetting(it) },
                                        activeBaseMapOpacity = activeBaseMapOpacity,
                                        onBaseMapOpacityChange = { gisViewModel.setBaseMapOpacity(it) },
                                        offlineModeForce = offlineModeForce,
                                        onOfflineModeForceChange = { gisViewModel.setOfflineModeForce(it) },
                                        showOfflineBoundaries = showOfflineBoundaries,
                                        onShowOfflineBoundariesChange = { gisViewModel.setShowOfflineBoundaries(it) }
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(130.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(fontColor.copy(alpha = 0.03f))
                                                .border(0.5.dp, fontColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SatelliteRadarSkyplot(isOutdoorMode = isOutdoorMode)
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("منبع موقعیت‌یابی:", fontSize = 9.sp, color = fontColor.copy(alpha = 0.6f))
                                                Text(
                                                    text = if (isRealPositionEnabled) "HARDWARE GPS" else "SIMULATOR",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Switch(
                                                    checked = isRealPositionEnabled,
                                                    onCheckedChange = { isRealPositionEnabled = it },
                                                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor),
                                                    modifier = Modifier.scale(0.7f).height(20.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text("شاخص‌های دقت مکان (HDOP):", fontSize = 8.sp, color = fontColor.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                            
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Column {
                                                    Text("HDOP", fontSize = 8.sp, color = fontColor.copy(alpha = 0.5f))
                                                    Text("0.85", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fontColor, fontFamily = FontFamily.Monospace)
                                                }
                                                Column {
                                                    Text("VDOP", fontSize = 8.sp, color = fontColor.copy(alpha = 0.5f))
                                                    Text("1.12", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fontColor, fontFamily = FontFamily.Monospace)
                                                }
                                                Column {
                                                    Text("PDOP", fontSize = 8.sp, color = fontColor.copy(alpha = 0.5f))
                                                    Text("1.40", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fontColor, fontFamily = FontFamily.Monospace)
                                                }
                                                Column {
                                                    Text("وضعیت", fontSize = 8.sp, color = fontColor.copy(alpha = 0.5f))
                                                    Text("3D FIX", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontFamily = FontFamily.Monospace)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text("صورت‌های فلکی فعال:", fontSize = 8.sp, color = fontColor.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "WGS84 GPS + GLONASS + GALILEO",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = fontColor
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "قدرت سیگنال کانال‌های فعال (dBHz):",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = fontColor.copy(alpha = 0.5f),
                                            fontFamily = FontFamily.Monospace
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            SignalBar(name = "GPS", dbHz = 42, color = Color(0xFF4CAF50), modifier = Modifier.weight(1f), isOutdoor = isOutdoorMode)
                                            SignalBar(name = "GLO", dbHz = 38, color = Color(0xFFFFA726), modifier = Modifier.weight(1f), isOutdoor = isOutdoorMode)
                                            SignalBar(name = "GAL", dbHz = 45, color = Color(0xFF29B6F6), modifier = Modifier.weight(1f), isOutdoor = isOutdoorMode)
                                            SignalBar(name = "BDS", dbHz = 35, color = Color(0xFFAB47BC), modifier = Modifier.weight(1f), isOutdoor = isOutdoorMode)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is MapUiState.ObservationCreationMode -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color.White else Color(0xFF101412)),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(0.5.dp, fontColor.copy(alpha = 0.15f)), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    ) {
                        GeologicalObservationSheet(
                            stationId = stationsToShow.firstOrNull()?.id ?: 1,
                            onDismiss = { gisViewModel.updateUiState(MapUiState.Idle) },
                            onSave = { obs: com.example.Observation ->
                                geologyViewModel.saveObservation(
                                    stationId = obs.stationId,
                                    obsType = obs.observationType,
                                    notes = obs.notes ?: "",
                                    photoPath = obs.photoPath,
                                    lithType = obs.lithologyType,
                                    texture = obs.texture,
                                    color = obs.color,
                                    minStyle = obs.minStyle,
                                    onSaved = {
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color.White else Color(0xFF101412)),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(0.5.dp, fontColor.copy(alpha = 0.15f)), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isOutdoorMode) Color.White else Color(0xFF101412)),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(0.5.dp, fontColor.copy(alpha = 0.15f)), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
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
                                    onSaved = {
                                        Toast.makeText(context, "Bedding attitude committed successfully!", Toast.LENGTH_SHORT).show()
                                        gisViewModel.updateUiState(MapUiState.Idle)
                                    }
                                )
                            },
                            isOutdoorMode = isOutdoorMode
                        )
                    }
                }



                else -> {}
            }
        }

        // --- LAYER 8: INTERACTIVE REAL-TIME ELEVATION PROFILE PANEL ---
        if (showElevationProfile) {
            val pointsToDisplay = if (elevationProfilePoints.isNotEmpty()) {
                elevationProfilePoints
            } else {
                activePoints
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 76.dp)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp)
                    .widthIn(max = 680.dp)
            ) {
                ElevationProfilePanel(
                    points = pointsToDisplay,
                    isOutdoorMode = isOutdoorMode,
                    onClose = { showElevationProfile = false }
                )
            }
        }

        if (showExportDialog) {
            val tracksList = savedTracks
            if (tracksList.isEmpty()) {
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = { Text("خطا | Error") },
                    text = { Text("هیچ مسیر ثبت‌شده‌ای برای خروجی وجود ندارد.\nNo saved walk tracks available to export.") },
                    confirmButton = {
                        TextButton(onClick = { showExportDialog = false }) {
                            Text("تایید | OK")
                        }
                    }
                )
            } else {
                // Set default selected track if null
                LaunchedEffect(tracksList) {
                    if (selectedExportTrack == null && tracksList.isNotEmpty()) {
                        selectedExportTrack = tracksList.first()
                    }
                }
                
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = { 
                        Text("خروجی اطلاعات GIS | Export GIS Data") 
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("فایل یا مسیر مورد نظر جهت خروجی گرفتن را انتخاب کنید:")
                            
                            // Track Selection Dropdown or Radio list
                            var expandedTrackSelect by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedTrackSelect = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(selectedExportTrack?.name ?: "انتخاب مسیر / Select Track")
                                }
                                DropdownMenu(
                                    expanded = expandedTrackSelect,
                                    onDismissRequest = { expandedTrackSelect = false }
                                ) {
                                    tracksList.forEach { track ->
                                        DropdownMenuItem(
                                            text = { Text(track.name) },
                                            onClick = {
                                                selectedExportTrack = track
                                                expandedTrackSelect = false
                                            }
                                        )
                                    }
                                }
                            }

                            Divider()

                            Text("فرمت فایل خروجی را انتخاب کنید:")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("GPX", "KML", "KMZ").forEach { format ->
                                    val isSel = selectedExportFormat == format
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedExportFormat = format },
                                        label = { Text(format, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val track = selectedExportTrack
                                if (track != null) {
                                    val parsedPoints = com.example.parsePointsJson(track.pointsJson)
                                    val sanitizedName = track.name.replace(" ", "_")
                                    when (selectedExportFormat) {
                                        "GPX" -> {
                                            val gpxContent = com.example.generateGpxString(track.name, parsedPoints)
                                            com.example.shareFileWithApps(context, "$sanitizedName.gpx", "application/gpx+xml", gpxContent)
                                        }
                                        "KML" -> {
                                            val kmlContent = com.example.generateKmlString(track.name, parsedPoints)
                                            com.example.shareFileWithApps(context, "$sanitizedName.kml", "application/vnd.google-earth.kml+xml", kmlContent)
                                        }
                                        "KMZ" -> {
                                            val kmlContent = com.example.generateKmlString(track.name, parsedPoints)
                                            com.example.shareKmzFileWithApps(context, "$sanitizedName.kmz", kmlContent)
                                        }
                                    }
                                    Toast.makeText(context, "فایل آماده اشتراک‌گذاری است / Sharing file...", Toast.LENGTH_SHORT).show()
                                }
                                showExportDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                            )
                        ) {
                            Text("اشتراک‌گذاری | Share", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExportDialog = false }) {
                            Text("انصراف | Cancel")
                        }
                    }
                )
            }
        }

        if (showCreatePlacemarkDialog) {
            AlertDialog(
                onDismissRequest = { showCreatePlacemarkDialog = false },
                title = {
                    Column {
                        Text(
                            text = "CREATE NEW PLACEMARK",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = placemarkNameInput,
                            onValueChange = { placemarkNameInput = it },
                            label = { Text("Name or Code") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = placemarkLatInput,
                            onValueChange = { placemarkLatInput = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = placemarkLonInput,
                            onValueChange = { placemarkLonInput = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = placemarkEleInput,
                            onValueChange = { placemarkEleInput = it },
                            label = { Text("Elevation (m)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                placemarkLatInput = String.format(Locale.US, "%.6f", currentLat)
                                placemarkLonInput = String.format(Locale.US, "%.6f", currentLon)
                                placemarkEleInput = if (liveLocation != null) String.format(Locale.US, "%.1f", liveLocation!!.altitude) else "1320.0"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor.copy(alpha = 0.15f),
                                contentColor = accentColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use GPS Location", fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val latVal = placemarkLatInput.toDoubleOrNull()
                            val lonVal = placemarkLonInput.toDoubleOrNull()
                            val eleVal = placemarkEleInput.toDoubleOrNull() ?: 0.0

                            if (placemarkNameInput.isBlank()) {
                                Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (latVal == null || lonVal == null) {
                                Toast.makeText(context, "Invalid coordinates", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val utm = com.example.UtmConverter.convertLatLngToUtm(latVal, lonVal, activeProject?.utmZoneManual)
                            val newStation = com.example.Station(
                                projectId = activeProject?.id ?: (projects.firstOrNull()?.id ?: 1),
                                stationCode = placemarkNameInput,
                                prefix = placemarkNameInput.take(4).filter { it.isLetter() }.ifEmpty { "ST" },
                                latitude = latVal,
                                longitude = lonVal,
                                elevation = eleVal,
                                accuracy = 1.0,
                                utmEasting = utm.easting,
                                utmNorthing = utm.northing,
                                utmZone = utm.zoneString
                            )

                            geologyViewModel.updateStation(newStation)
                            Toast.makeText(context, "Placemark saved successfully!", Toast.LENGTH_SHORT).show()
                            showCreatePlacemarkDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                        )
                    ) {
                        Text("Save", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlacemarkDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showEditPlacemarkDialog && selectedPlacemarkToEdit != null) {
            AlertDialog(
                onDismissRequest = { showEditPlacemarkDialog = false },
                title = {
                    Column {
                        Text(
                            text = "EDIT PLACEMARK",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = placemarkNameInput,
                            onValueChange = { placemarkNameInput = it },
                            label = { Text("Name or Code") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = placemarkLatInput,
                            onValueChange = { placemarkLatInput = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = placemarkLonInput,
                            onValueChange = { placemarkLonInput = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = placemarkEleInput,
                            onValueChange = { placemarkEleInput = it },
                            label = { Text("Elevation (m)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                placemarkLatInput = String.format(Locale.US, "%.6f", currentLat)
                                placemarkLonInput = String.format(Locale.US, "%.6f", currentLon)
                                placemarkEleInput = if (liveLocation != null) String.format(Locale.US, "%.1f", liveLocation!!.altitude) else "1320.0"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor.copy(alpha = 0.15f),
                                contentColor = accentColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use GPS Location", fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val latVal = placemarkLatInput.toDoubleOrNull()
                            val lonVal = placemarkLonInput.toDoubleOrNull()
                            val eleVal = placemarkEleInput.toDoubleOrNull() ?: 0.0

                            if (placemarkNameInput.isBlank()) {
                                Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (latVal == null || lonVal == null) {
                                Toast.makeText(context, "Invalid coordinates", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val target = selectedPlacemarkToEdit!!
                            val utm = com.example.UtmConverter.convertLatLngToUtm(latVal, lonVal, activeProject?.utmZoneManual)
                            val updatedStation = target.copy(
                                stationCode = placemarkNameInput,
                                prefix = placemarkNameInput.take(4).filter { it.isLetter() }.ifEmpty { "ST" },
                                latitude = latVal,
                                longitude = lonVal,
                                elevation = eleVal,
                                utmEasting = utm.easting,
                                utmNorthing = utm.northing,
                                utmZone = utm.zoneString
                            )

                            geologyViewModel.updateStation(updatedStation)
                            Toast.makeText(context, "Changes saved successfully!", Toast.LENGTH_SHORT).show()
                            showEditPlacemarkDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                        )
                    ) {
                        Text("Save", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditPlacemarkDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
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

@Composable
fun SettingsSheet(
    geologyViewModel: com.example.GeologyViewModel,
    isOutdoorMode: Boolean,
    onToggleOutdoorMode: (Boolean) -> Unit,
    simWalkStepSize: Float,
    onSimStepSizeChange: (Float) -> Unit,
    onResetDatabase: () -> Unit,
    onDismiss: () -> Unit,
    isImportingMbtiles: Boolean,
    mbtilesImportProgress: Float,
    mbtilesImportStatus: String,
    onImportMbtiles: (android.net.Uri) -> Unit,
    gridSizeSetting: Int,
    onGridSizeSettingChange: (Int) -> Unit,
    gridColorSetting: String,
    onGridColorSettingChange: (String) -> Unit,
    activeBaseMapOpacity: Float,
    onBaseMapOpacityChange: (Float) -> Unit,
    offlineModeForce: Boolean,
    onOfflineModeForceChange: (Boolean) -> Unit,
    showOfflineBoundaries: Boolean,
    onShowOfflineBoundariesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { onImportMbtiles(it) }
    }

    var activeTab by remember { mutableStateOf(0) } // 0: MBTiles, 1: Grids, 2: General

    // Advanced Collected Settings States from SharedPreferences/Room Flows
    val coordSystem by geologyViewModel.coordSystemSetting.collectAsStateWithLifecycle()
    val customEpsgCode by geologyViewModel.customEpsgCode.collectAsStateWithLifecycle()
    val prefs = remember { geologyViewModel.application.getSharedPreferences("geofieldpro_prefs", android.content.Context.MODE_PRIVATE) }
    var distanceUnit by remember { mutableStateOf(prefs.getString("distance_unit", "Metric") ?: "Metric") }
    var keepScreenOn by remember { mutableStateOf(prefs.getBoolean("keep_screen_on", true)) }

    // Live binding of FLAG_KEEP_SCREEN_ON window state based on keepScreenOn setting
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    DisposableEffect(keepScreenOn) {
        if (activity != null) {
            if (keepScreenOn) {
                activity.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {}
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutdoorMode) Color.White else Color(0xFF1E1E1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, if (isOutdoorMode) Color.LightGray else Color.DarkGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GIS Layer & System Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOutdoorMode) Color.Black else Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (isOutdoorMode) Color.Black else Color.White
                    )
                }
            }
            
            Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray)

            // --- NAVIGATION TABS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isOutdoorMode) Color(0xFFF1F5F9) else Color(0xFF2D2D2D))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabItems = listOf(
                    Triple(0, "Offline Map\nMBTiles Layer", Icons.Default.CloudDownload),
                    Triple(1, "Grid & Coords\nUTM & Grids", Icons.Default.GridView),
                    Triple(2, "GPS Accuracy\nFilter & Precision", Icons.Default.LocationOn),
                    Triple(3, "General Settings\nUnits & Theme", Icons.Default.Settings)
                )

                tabItems.forEach { (index, title, icon) ->
                    val isSelected = activeTab == index
                    val activeBgColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                    val activeContentColor = Color.White
                    val inactiveContentColor = if (isOutdoorMode) Color.DarkGray else Color.LightGray

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) activeBgColor else Color.Transparent)
                            .clickable { activeTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) activeContentColor else inactiveContentColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = title.split("\n").first(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) activeContentColor else inactiveContentColor
                            )
                            Text(
                                text = title.split("\n").last(),
                                fontSize = 8.sp,
                                color = if (isSelected) activeContentColor.copy(alpha = 0.7f) else inactiveContentColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

            // --- TAB CONTENT ---
            when (activeTab) {
                0 -> {
                    // TAB 0: OFFLINE MAP MBTILES IMPORT, OPACITY & OFFLINE PREFERENCES
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Offline Layer Import & Opacity",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isOutdoorMode) Color.Black else Color.White
                        )
                        Text(
                            text = "Import offline geological GIS map layers (.mbtiles) for remote navigation without internet access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOutdoorMode) Color.Gray else Color.LightGray
                        )
                        
                        if (isImportingMbtiles) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isOutdoorMode) Color(0xFFF0FDFC) else Color(0xFF0F2D2A),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, Color(0xFF007A78), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = mbtilesImportStatus,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF43C6C1)
                                )
                                LinearProgressIndicator(
                                    progress = mbtilesImportProgress,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF109D96),
                                    trackColor = if (isOutdoorMode) Color(0xFFE0F2F1) else Color(0xFF004D40)
                                )
                            }
                        } else {
                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Upload",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Select .mbtiles File", color = Color.White)
                            }
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 1. Base Map Opacity Slider
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Base Map Opacity:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutdoorMode) Color.Black else Color.White
                                )
                                Text(
                                    text = "${(activeBaseMapOpacity * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            }
                            Slider(
                                value = activeBaseMapOpacity,
                                onValueChange = onBaseMapOpacityChange,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                    activeTrackColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 2. Force Offline Mode Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Force Offline Mode",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutdoorMode) Color.Black else Color.White
                                )
                                Text(
                                    text = "Disable all map internet requests to save battery & improve stability",
                                    fontSize = 9.sp,
                                    color = if (isOutdoorMode) Color.Gray else Color.LightGray
                                )
                            }
                            Switch(
                                checked = offlineModeForce,
                                onCheckedChange = onOfflineModeForceChange,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            )
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 3. Show Offline Boundaries Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show Download Boundaries",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutdoorMode) Color.Black else Color.White
                                )
                                Text(
                                    text = "Show red guide borders around downloaded offline regions",
                                    fontSize = 9.sp,
                                    color = if (isOutdoorMode) Color.Gray else Color.LightGray
                                )
                            }
                            Switch(
                                checked = showOfflineBoundaries,
                                onCheckedChange = onShowOfflineBoundariesChange,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            )
                        }
                    }
                }
                1 -> {
                    // TAB 1: COORDINATES & NETWORKS (GRID SETTINGS, COORDINATE FORMAT & UNITS)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "UTM Grid & Coordinates",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isOutdoorMode) Color.Black else Color.White
                        )

                        // 1. Spacing Choices
                        Text(
                            text = "Grid Spacing: " + if (gridSizeSetting == 0) "Auto" else "${gridSizeSetting}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOutdoorMode) Color.Gray else Color.LightGray
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val spacingOptions = listOf(0 to "Auto", 100 to "100m", 500 to "500m", 1000 to "1km", 5000 to "5km")
                            spacingOptions.forEach { (value, label) ->
                                val isSelected = gridSizeSetting == value
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onGridSizeSettingChange(value) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 2. Color Choices
                        Text(
                            text = "Grid Line Color",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOutdoorMode) Color.Gray else Color.LightGray
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val colorOptions = listOf(
                                "" to (if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF00E5FF)), // default
                                "#FF5252" to Color(0xFFFF5252),
                                "#4CAF50" to Color(0xFF4CAF50),
                                "#FFEB3B" to Color(0xFFFFEB3B),
                                "#FF9800" to Color(0xFFFF9800),
                                "#000000" to Color(0xFF000000),
                                "#FFFFFF" to Color(0xFFFFFFFF)
                            )

                            colorOptions.forEach { (hex, colorVal) ->
                                val isSelected = gridColorSetting == hex
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(colorVal)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) (if (isOutdoorMode) Color.Black else Color.White) else Color.Gray.copy(alpha = 0.5f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                        .clickable { onGridColorSettingChange(hex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = if (colorVal == Color.White || hex == "#FFEB3B") Color.Black else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 3. Coordinate System Selection (UTM, DD, DMS)
                        Text(
                            text = "Coordinate Display System",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOutdoorMode) Color.Gray else Color.LightGray
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val coordOptions = listOf(
                                "WGS84_DECIMAL" to "WGS84",
                                "UTM" to "UTM",
                                "MGRS" to "MGRS",
                                "WEB_MERCATOR" to "Web Mercator",
                                "CUSTOM_EPSG" to "Custom EPSG"
                            )
                            coordOptions.forEach { (key, label) ->
                                val isSelected = coordSystem == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { geologyViewModel.setCoordSystemSetting(key) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        if (coordSystem == "CUSTOM_EPSG") {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customEpsgCode,
                                onValueChange = { geologyViewModel.setCustomEpsgCode(it) },
                                label = { Text("Custom EPSG Code", color = if (isOutdoorMode) Color.DarkGray else Color.LightGray) },
                                placeholder = { Text("e.g. 32639") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = androidx.compose.ui.text.TextStyle(color = if (isOutdoorMode) Color.Black else Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                                    focusedLabelColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                    unfocusedLabelColor = if (isOutdoorMode) Color.DarkGray else Color.LightGray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 4. Distance Unit Selection (Metric, Imperial)
                        Text(
                            text = "Distance Measurement Unit",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOutdoorMode) Color.Gray else Color.LightGray
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val unitOptions = listOf("Metric" to "Metric (m/km)", "Imperial" to "Imperial (ft/mi)")
                            unitOptions.forEach { (key, label) ->
                                val isSelected = distanceUnit == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        distanceUnit = key
                                        prefs.edit().putString("distance_unit", key).apply()
                                    },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: GPS ACCURACY, MIN DISPLACEMENT, UPDATE INTERVALS & FILTER NOISE SETTINGS
                    val isKalmanSmoothingEnabled by geologyViewModel.isKalmanSmoothingEnabled.collectAsStateWithLifecycle()
                    val gpsAccuracyLimit by geologyViewModel.gpsAccuracyLimit.collectAsStateWithLifecycle()
                    val gpsMinDisplacement by geologyViewModel.gpsMinDisplacement.collectAsStateWithLifecycle()
                    val gpsIntervalSetting by geologyViewModel.gpsIntervalSetting.collectAsStateWithLifecycle()
                    val isMovingAverageEnabled by geologyViewModel.isMovingAverageEnabled.collectAsStateWithLifecycle()
                    val movingAverageWindowSize by geologyViewModel.movingAverageWindowSize.collectAsStateWithLifecycle()
                    val geoidHeightCorrectionOffset by geologyViewModel.geoidHeightCorrectionOffset.collectAsStateWithLifecycle()
                    val trackSpeedFilterEnabled by geologyViewModel.trackSpeedFilterEnabled.collectAsStateWithLifecycle()
                    val trackMinTimeBetweenPoints by geologyViewModel.trackMinTimeBetweenPoints.collectAsStateWithLifecycle()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "GPS Precision & Noise Filtering",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isOutdoorMode) Color.Black else Color.White
                        )
                        Text(
                            text = "Filters noisy GPS signals and sudden jumps in valleys or rocky areas using Kalman filter and moving average.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOutdoorMode) Color.Gray else Color.LightGray
                        )

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 1. Kalman Smoothing Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Kalman Smoothing Filter",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutdoorMode) Color.Black else Color.White
                                )
                                Text(
                                    text = "Eliminate random cursor jumps and drift while stationary",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isOutdoorMode) Color.Gray else Color.LightGray
                                )
                            }
                            Switch(
                                checked = isKalmanSmoothingEnabled,
                                onCheckedChange = { geologyViewModel.setKalmanSmoothingEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            )
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 2. Moving Average Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Track Moving Average",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutdoorMode) Color.Black else Color.White
                                )
                                Text(
                                    text = "Smooth recorded track lines to prevent line jitter",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isOutdoorMode) Color.Gray else Color.LightGray
                                )
                            }
                            Switch(
                                checked = isMovingAverageEnabled,
                                onCheckedChange = { geologyViewModel.setMovingAverageEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            )
                        }

                        if (isMovingAverageEnabled) {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Smoothing window size:",
                                        fontSize = 11.sp,
                                        color = if (isOutdoorMode) Color.Black else Color.White
                                    )
                                    Text(
                                        text = "$movingAverageWindowSize points",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                    )
                                }
                                Slider(
                                    value = movingAverageWindowSize.toFloat(),
                                    onValueChange = { geologyViewModel.setMovingAverageWindowSize(it.toInt()) },
                                    valueRange = 3f..15f,
                                    steps = 11,
                                    colors = SliderDefaults.colors(
                                        thumbColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                        activeTrackColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                    )
                                )
                            }
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 3. GPS Minimum Displacement
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Min Displacement:",
                                    fontSize = 11.sp,
                                    color = if (isOutdoorMode) Color.Black else Color.White
                                )
                                Text(
                                    text = "${gpsMinDisplacement.toInt()} meters",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            }
                            Text(
                                text = "Minimum distance moved before adding a new track point to avoid zig-zag lines while stationary",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOutdoorMode) Color.Gray else Color.LightGray
                            )
                            Slider(
                                value = gpsMinDisplacement,
                                onValueChange = { geologyViewModel.setGpsMinDisplacement(it) },
                                valueRange = 1f..20f,
                                steps = 19,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                    activeTrackColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            )
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 4. GPS Power Interval Mode
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "GPS Refresh Mode",
                                fontSize = 11.sp,
                                color = if (isOutdoorMode) Color.Black else Color.White
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val refreshOptions = listOf(
                                    "High Performance" to "High Accuracy (~1s)",
                                    "Balanced" to "Balanced (~5s)",
                                    "Battery Saving" to "Battery Saving (~15s)"
                                )
                                refreshOptions.forEach { (key, label) ->
                                    val isSelected = gpsIntervalSetting == key
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { geologyViewModel.setGpsIntervalSetting(key, context) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 5. GPS Accuracy Threshold limit
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Max Accuracy Error:",
                                    fontSize = 11.sp,
                                    color = if (isOutdoorMode) Color.Black else Color.White
                                )
                                Text(
                                    text = "${gpsAccuracyLimit.toInt()} m",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            }
                            Text(
                                text = "Discard weak GPS points with accuracy error higher than defined",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOutdoorMode) Color.Gray else Color.LightGray
                            )
                            Slider(
                                value = gpsAccuracyLimit,
                                onValueChange = { geologyViewModel.setGpsAccuracyLimit(it) },
                                valueRange = 5f..100f,
                                steps = 19,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                    activeTrackColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            )
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 6. Geoid Correction Offset
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Geoid Correction Offset:",
                                    fontSize = 11.sp,
                                    color = if (isOutdoorMode) Color.Black else Color.White
                                )
                                Text(
                                    text = "${geoidHeightCorrectionOffset.toInt()} meters",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            }
                            Text(
                                text = "Applies correction for GPS ellipsoidal height to mean sea level",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOutdoorMode) Color.Gray else Color.LightGray
                            )
                            Slider(
                                value = geoidHeightCorrectionOffset,
                                onValueChange = { geologyViewModel.setGeoidHeightCorrectionOffset(it) },
                                valueRange = -50f..50f,
                                steps = 20,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                    activeTrackColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            )
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 7. Track Recording Speed Filter Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Route Speed Jump Filter",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutdoorMode) Color.Black else Color.White
                                )
                                Text(
                                    text = "Filters out sudden GPS jumps caused by signal noise",
                                    fontSize = 9.sp,
                                    color = if (isOutdoorMode) Color.Gray else Color.LightGray
                                )
                            }
                            Switch(
                                checked = trackSpeedFilterEnabled,
                                onCheckedChange = { geologyViewModel.setTrackSpeedFilterEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            )
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 8. Track Recording Time Interval Slider
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Min Recording Time:",
                                    fontSize = 11.sp,
                                    color = if (isOutdoorMode) Color.Black else Color.White
                                )
                                Text(
                                    text = "$trackMinTimeBetweenPoints seconds",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            }
                            Text(
                                text = "Minimum elapsed time before recording next point",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOutdoorMode) Color.Gray else Color.LightGray
                            )
                            Slider(
                                value = trackMinTimeBetweenPoints.toFloat(),
                                onValueChange = { geologyViewModel.setTrackMinTimeBetweenPoints(it.toInt()) },
                                valueRange = 1f..15f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                    activeTrackColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            )
                        }
                    }
                }
                3 -> {
                    // TAB 3: GENERAL SYSTEM / SIMULATOR, THEME, & KEEP SCREEN ON SETTINGS
                    val themeSetting by geologyViewModel.themeSetting.collectAsStateWithLifecycle()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Outdoor Mode Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Outdoor Contrast Mode",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isOutdoorMode) Color.Black else Color.White
                                )
                                Text(
                                    text = "Enhances color contrast and map elements for high-glare sunlight situations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isOutdoorMode) Color.Gray else Color.LightGray
                                )
                            }
                            Switch(
                                checked = isOutdoorMode,
                                onCheckedChange = { onToggleOutdoorMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            )
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 2. Keep Screen On Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Keep Screen On",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isOutdoorMode) Color.Black else Color.White
                                )
                                Text(
                                    text = "Prevents screen from dimming or locking while navigating with active tracking",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isOutdoorMode) Color.Gray else Color.LightGray
                                )
                            }
                            Switch(
                                checked = keepScreenOn,
                                onCheckedChange = {
                                    keepScreenOn = it
                                    prefs.edit().putBoolean("keep_screen_on", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            )
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)

                        // 3. Application Theme Selection
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Application Theme",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (isOutdoorMode) Color.Black else Color.White
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val themes = listOf("Light" to "Light", "Dark" to "Dark", "System" to "System")
                                themes.forEach { (themeKey, label) ->
                                    val isSelected = themeSetting == themeKey
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { geologyViewModel.setThemeSetting(themeKey) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)
                        
                        // 4. Walking Simulation Step Slider
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Walking Simulation Step",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isOutdoorMode) Color.Black else Color.White
                                )
                                Text(
                                    text = String.format(Locale.US, "%.5f°", simWalkStepSize),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            }
                            Slider(
                                value = simWalkStepSize,
                                onValueChange = { onSimStepSizeChange(it) },
                                valueRange = 0.00001f..0.001f,
                                steps = 10,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96),
                                    activeTrackColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
                                )
                            )
                        }

                        Divider(color = if (isOutdoorMode) Color.LightGray else Color.DarkGray, thickness = 0.5.dp)
                        
                        // 5. Purge Storage warning
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Purge Cache Database",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isOutdoorMode) Color.Black else Color.White
                                )
                                Text(
                                    text = "Permanently delete all stations, projects, and recorded tracks from the local database",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isOutdoorMode) Color.Gray else Color.LightGray
                                )
                            }
                            Button(
                                onClick = onResetDatabase,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Purge Database", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
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

@Composable
fun SignalBar(
    name: String,
    dbHz: Int,
    color: Color,
    isOutdoor: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = (dbHz / 50f).coerceIn(0f, 1f)
    val textCol = if (isOutdoor) Color.Black else Color.White
    Card(
        colors = CardDefaults.cardColors(containerColor = textCol.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(name, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textCol)
                Text("${dbHz}dB", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = textCol.copy(alpha = 0.7f))
            }
            LinearProgressIndicator(
                progress = progress,
                color = color,
                trackColor = textCol.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

private data class SkyplotSat(val dist: Float, val az: Float, val name: String)

@Composable
fun SatelliteRadarSkyplot(isOutdoorMode: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val gridColor = if (isOutdoorMode) Color(0xFF007A78).copy(alpha = 0.4f) else Color(0xFF109D96).copy(alpha = 0.4f)
    val sweepColor = if (isOutdoorMode) Color(0xFF007A78).copy(alpha = 0.15f) else Color(0xFF109D96).copy(alpha = 0.15f)
    val strokeStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)

    val satellites = remember {
        listOf(
            SkyplotSat(0.3f, 45f, "G01"),
            SkyplotSat(0.7f, 120f, "G14"),
            SkyplotSat(0.5f, 210f, "R05"),
            SkyplotSat(0.8f, 315f, "E12"),
            SkyplotSat(0.4f, 280f, "B02")
        )
    }

    Canvas(
        modifier = Modifier
            .size(110.dp)
            .padding(4.dp)
    ) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f

        drawCircle(color = gridColor, radius = maxRadius, style = strokeStyle)
        drawCircle(color = gridColor, radius = maxRadius * 0.66f, style = strokeStyle)
        drawCircle(color = gridColor, radius = maxRadius * 0.33f, style = strokeStyle)

        drawLine(color = gridColor, start = androidx.compose.ui.geometry.Offset(0f, center.y), end = androidx.compose.ui.geometry.Offset(size.width, center.y), strokeWidth = 1f)
        drawLine(color = gridColor, start = androidx.compose.ui.geometry.Offset(center.x, 0f), end = androidx.compose.ui.geometry.Offset(center.x, size.height), strokeWidth = 1f)

        drawArc(
            color = sweepColor,
            startAngle = angle - 30f,
            sweepAngle = 30f,
            useCenter = true,
            size = size
        )

        for (sat in satellites) {
            val dist = sat.dist
            val az = sat.az
            val rad = java.lang.Math.toRadians(az.toDouble())
            val r = dist * maxRadius
            val x = center.x + (r * java.lang.Math.cos(rad)).toFloat()
            val y = center.y - (r * java.lang.Math.sin(rad)).toFloat()

            drawCircle(color = Color(0xFF4CAF50), radius = 6f, center = androidx.compose.ui.geometry.Offset(x, y))
            drawCircle(color = Color(0xFF4CAF50).copy(alpha = 0.3f), radius = 12f, center = androidx.compose.ui.geometry.Offset(x, y), style = strokeStyle)
        }
    }
}
