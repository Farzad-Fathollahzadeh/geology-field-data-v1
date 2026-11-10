package com.example.ui.gis

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.GeologyViewModel
import com.example.Station
import com.example.TrackPoint
import com.example.UtmConverter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

/**
 * Modern, Ultra-High-Performance Field Geology GIS Map Screen.
 * Completely rebuilt from scratch with clean ergonomic bottom actions,
 * precision reticle, offline MBTiles management, and real-time sensor integration.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    geologyViewModel: GeologyViewModel,
    gisViewModel: MapViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Core Database & Field Flows from GeologyViewModel
    val activeProjectStations by geologyViewModel.activeProjectStations.collectAsStateWithLifecycle()
    val allStationsList by geologyViewModel.allStations.collectAsStateWithLifecycle()
    val savedTracks by geologyViewModel.tracks.collectAsStateWithLifecycle()
    val activeProject by geologyViewModel.activeProject.collectAsStateWithLifecycle()
    val importedFeatures by geologyViewModel.importedFeatures.collectAsStateWithLifecycle()
    val allObservationsList by geologyViewModel.allObservations.collectAsStateWithLifecycle()
    val allSamplesList by geologyViewModel.allSamples.collectAsStateWithLifecycle()
    val stationsToShow = if (activeProjectStations.isNotEmpty()) activeProjectStations else allStationsList

    // GPS & Track Recording Flows
    val isTrackRecordingActive by geologyViewModel.isTrackRecordingActive.collectAsStateWithLifecycle()
    val activePoints by geologyViewModel.activePoints.collectAsStateWithLifecycle()
    val liveLocation by geologyViewModel.liveLocation.collectAsStateWithLifecycle()
    val recordingDurationSec by geologyViewModel.recordingDurationSec.collectAsStateWithLifecycle()

    // GIS Map Engine Settings from MapViewModel
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
    val activeMbtiles by gisViewModel.activeMbtiles.collectAsStateWithLifecycle()
    val importedMbtilesFiles by gisViewModel.importedMbtilesFiles.collectAsStateWithLifecycle()

    // File Import Launchers
    val mbtilesImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { gisViewModel.importMbtilesFile(context, it) }
    }

    val gisFileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { geologyViewModel.importMapFile(context, it) }
    }

    // Location Permission Handling
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val isLocationGranted = locationPermissionsState.permissions.any { it.status.isGranted }

    LaunchedEffect(Unit) {
        if (!isLocationGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        } else {
            geologyViewModel.startLocationTracking(context)
        }
    }

    LaunchedEffect(isLocationGranted) {
        if (isLocationGranted) {
            geologyViewModel.startLocationTracking(context)
        }
    }

    // Positioning State
    val defaultInitialLat = remember(activeProjectStations) {
        if (activeProjectStations.isNotEmpty()) activeProjectStations.map { it.latitude }.average() else 35.69439
    }
    val defaultInitialLon = remember(activeProjectStations) {
        if (activeProjectStations.isNotEmpty()) activeProjectStations.map { it.longitude }.average() else 51.42151
    }

    var lockGpsCenter by remember { mutableStateOf(true) }
    var simulatorLat by remember(defaultInitialLat) { mutableStateOf(defaultInitialLat) }
    var simulatorLon by remember(defaultInitialLon) { mutableStateOf(defaultInitialLon) }
    var simulatorHeading by remember { mutableStateOf(45.0) }

    val liveCompassData by rememberCompassSensor()

    val currentLat = if (liveLocation != null) liveLocation!!.latitude else simulatorLat
    val currentLon = if (liveLocation != null) liveLocation!!.longitude else simulatorLon
    val currentHeading = if (liveCompassData.isAvailable) {
        liveCompassData.azimuthDegrees.toDouble()
    } else if (liveLocation != null) {
        liveLocation!!.bearing.toDouble()
    } else {
        simulatorHeading
    }
    val currentEle = if (liveLocation != null) liveLocation!!.altitude else 1420.0
    val currentAccuracy = if (liveLocation != null) liveLocation!!.accuracy.toDouble() else 1.8

    // Interactive UI toggles
    var coordinateFormatIndex by remember { mutableStateOf(0) } // 0: WGS84, 1: UTM, 2: MGRS
    var showQuickOpacitySlider by remember { mutableStateOf(false) }
    var showMoveSimulator by remember { mutableStateOf(false) }
    var isRulerMode by remember { mutableStateOf(false) }
    var isAreaMode by remember { mutableStateOf(false) }
    var measurementResultText by remember { mutableStateOf("") }

    // Dialog & Sheet States
    var showAddStationDialog by remember { mutableStateOf(false) }
    var showAddBoreholeDialog by remember { mutableStateOf(false) }
    var showProjectStatsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showCompassModal by remember { mutableStateOf(false) }

    // Colors & Theme Styling
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF00FFE0)
    val fontColor = if (isOutdoorMode) Color.Black else Color.White
    val cardBg = if (isOutdoorMode) Color.White.copy(alpha = 0.94f) else Color(0xFF141A17).copy(alpha = 0.94f)

    // Resolve Map Base Tile URL
    val mapTileUrl = remember(activeBaseMap) {
        when (activeBaseMap) {
            "OpenTopoMap" -> "https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png"
            "OpenStreetMap" -> "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            "Google Satellite" -> "https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}"
            "Esri Satellite (No-Key)" -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
            else -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
        }
    }

    // Step simulation walk
    fun simulateWalk(direction: String) {
        val stepDeg = (simWalkStepSize / 111320.0).toDouble()
        when (direction) {
            "NORTH" -> { simulatorLat += stepDeg; simulatorHeading = 0.0 }
            "SOUTH" -> { simulatorLat -= stepDeg; simulatorHeading = 180.0 }
            "EAST" -> { simulatorLon += stepDeg / cos(Math.toRadians(simulatorLat)); simulatorHeading = 90.0 }
            "WEST" -> { simulatorLon -= stepDeg / cos(Math.toRadians(simulatorLat)); simulatorHeading = 270.0 }
        }
        lockGpsCenter = true
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isOutdoorMode) Color.White else Color.Black)
    ) {
        // ==========================================
        // 0. HIGH PERFORMANCE HARDWARE-ACCELERATED MAP
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp)
        ) {
            GisMapView(
                stations = stationsToShow,
                observations = allObservationsList,
                trackPoints = if (isTrackRecordingActive) activePoints else emptyList(),
                currentLat = currentLat,
                currentLon = currentLon,
                zoom = zoomLevel,
                tileUrl = mapTileUrl,
                gpsAccuracy = currentAccuracy,
                lockGpsCenter = lockGpsCenter,
                onCenterChanged = { lat, lon ->
                    if (!lockGpsCenter) {
                        simulatorLat = lat
                        simulatorLon = lon
                    }
                },
                onZoomChanged = { gisViewModel.setZoomLevel(it) },
                showUtmGrid = showGrids,
                isOutdoorMode = isOutdoorMode,
                baseMapOpacity = activeBaseMapOpacity,
                offlineModeForce = offlineModeForce,
                showOfflineBoundaries = showOfflineBoundaries,
                downloadedRegions = downloadedRegionsList,
                importedFeatures = importedFeatures,
                savedTracks = savedTracks,
                showTracksOverlay = true,
                showStations = showStations,
                showSymbols = showSymbols,
                showFaultLines = showFaultLines,
                activeMbtilesList = activeMbtiles.toList(),
                isRulerMode = isRulerMode,
                isAreaMode = isAreaMode,
                onMeasurementResult = { _, unitText ->
                    measurementResultText = unitText
                },
                modifier = Modifier.fillMaxSize()
            )

            // Precision Target Reticle in Center of Map
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 1.5f
                    val crosshairColor = if (isOutdoorMode) Color(0xCC007A78) else Color(0xCC00FFE0)
                    drawCircle(color = crosshairColor, radius = 2f)
                    drawCircle(
                        color = crosshairColor,
                        radius = 8f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW)
                    )
                    val centerPx = size.width / 2f
                    drawLine(crosshairColor, Offset(centerPx, 0f), Offset(centerPx, 8f), strokeW)
                    drawLine(crosshairColor, Offset(centerPx, size.height - 8f), Offset(centerPx, size.height), strokeW)
                    drawLine(crosshairColor, Offset(0f, centerPx), Offset(8f, centerPx), strokeW)
                    drawLine(crosshairColor, Offset(size.width - 8f, centerPx), Offset(size.width, centerPx), strokeW)
                }
            }

            // Real-time Range & Bearing HUD Tooltip when exploring away from GPS
            if (!lockGpsCenter) {
                val crosshairDist = remember(currentLat, currentLon, simulatorLat, simulatorLon) {
                    val r = 6371000.0
                    val dLat = Math.toRadians(simulatorLat - currentLat)
                    val dLon = Math.toRadians(simulatorLon - currentLon)
                    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(currentLat)) * cos(Math.toRadians(simulatorLat)) * sin(dLon / 2).pow(2)
                    r * 2 * atan2(sqrt(a), sqrt(1 - a))
                }
                val crosshairBearing = remember(currentLat, currentLon, simulatorLat, simulatorLon) {
                    val phi1 = Math.toRadians(currentLat)
                    val phi2 = Math.toRadians(simulatorLat)
                    val deltaLambda = Math.toRadians(simulatorLon - currentLon)
                    val y = sin(deltaLambda) * cos(phi2)
                    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
                    (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
                }

                if (crosshairDist > 8.0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 56.dp)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)),
                            shadowElevation = 4.dp
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
                                Box(modifier = Modifier.height(10.dp).width(1.dp).background(Color.White.copy(alpha = 0.3f)))
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
        }

        // ==========================================
        // 1. TOP DYNAMIC TELEMETRY ISLAND
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 10.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val utmCoord = remember(currentLat, currentLon) {
                try { UtmConverter.convertLatLngToUtm(currentLat, currentLon) } catch (e: Exception) { null }
            }

            val mgrsString = remember(currentLat, currentLon, utmCoord) {
                if (utmCoord == null) "MGRS..." else {
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
                1 -> if (utmCoord != null) "${utmCoord.zoneString} ${String.format(Locale.US, "%.0f", utmCoord.easting)}m E ${String.format(Locale.US, "%.0f", utmCoord.northing)}m N" else "UTM..."
                else -> "MGRS: $mgrsString"
            }

            // Floating Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, if (isOutdoorMode) Color(0xFFD6DDD8) else accentColor.copy(alpha = 0.4f)),
                shadowElevation = 4.dp,
                modifier = Modifier.wrapContentWidth().height(40.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // GPS Fix Indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (currentAccuracy < 3.0) Color(0xFF4CAF50) else Color(0xFFFF9800))
                        )
                        Text(
                            text = "±${String.format(Locale.US, "%.1fm", currentAccuracy)}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentAccuracy < 3.0) Color(0xFF2E7D32) else Color(0xFFE65100),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(modifier = Modifier.height(14.dp).width(1.dp).background(fontColor.copy(alpha = 0.15f)))

                    // Coordinates & Altitude
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { coordinateFormatIndex = (coordinateFormatIndex + 1) % 3 }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = readoutText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = accentColor
                        )
                        Text(
                            text = "▲${currentEle.toInt()}m",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = fontColor.copy(alpha = 0.75f),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // UTM Grid toggle icon
                    IconButton(
                        onClick = { gisViewModel.toggleGrids(!showGrids) },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "شبکه UTM",
                            tint = if (showGrids) accentColor else fontColor.copy(alpha = 0.35f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Active Offline MBTiles chip
                    val activeMbtilesEntries = activeMbtiles.toList()
                    if (activeMbtilesEntries.isNotEmpty()) {
                        val primary = activeMbtilesEntries.first()
                        Box(modifier = Modifier.height(14.dp).width(1.dp).background(fontColor.copy(alpha = 0.15f)))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showQuickOpacitySlider = !showQuickOpacitySlider }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = accentColor, modifier = Modifier.size(10.dp))
                                Text(
                                    text = "${primary.first.take(6)} ${(primary.second * 100).toInt()}%",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Active Track Recording Strip
            if (isTrackRecordingActive) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD32F2F).copy(alpha = 0.95f),
                    shadowElevation = 3.dp,
                    modifier = Modifier.wrapContentWidth().height(30.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        val totalDistanceMeters = remember(activePoints.size) {
                            if (activePoints.size < 2) 0.0 else {
                                var dist = 0.0
                                for (i in 0 until activePoints.size - 1) {
                                    dist += gisViewModel.calculateGeodesicDistance(
                                        activePoints[i].lat, activePoints[i].lon,
                                        activePoints[i+1].lat, activePoints[i+1].lon
                                    )
                                }
                                dist
                            }
                        }
                        val min = recordingDurationSec / 60
                        val sec = recordingDurationSec % 60
                        Text(
                            text = "🔴 در حال ثبت مسیر: ${String.format(Locale.US, "%02d:%02d", min, sec)} | ${activePoints.size} نقطه | " +
                                    (if (totalDistanceMeters >= 1000) String.format(Locale.US, "%.2f km", totalDistanceMeters / 1000.0) else String.format(Locale.US, "%.0f m", totalDistanceMeters)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Live Measurement Tool Strip (Distance / Area)
            if (isRulerMode || isAreaMode) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = (if (isOutdoorMode) Color.White else Color(0xFF1B2320)).copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, accentColor),
                    shadowElevation = 4.dp,
                    modifier = Modifier.wrapContentWidth().height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isRulerMode) Icons.Default.Straighten else Icons.Default.AllOut,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isRulerMode) "خط‌کش مسافت: ${measurementResultText.ifEmpty { "روی نقشه ضربه بزنید" }}"
                            else "مساحی پلیگون: ${measurementResultText.ifEmpty { "نقاط محدوده را علامت بزنید" }}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = fontColor
                        )
                        IconButton(
                            onClick = {
                                isRulerMode = false
                                isAreaMode = false
                                measurementResultText = ""
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = fontColor.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Quick Opacity Mini-Panel
            if (showQuickOpacitySlider && activeMbtiles.isNotEmpty()) {
                val primary = activeMbtiles.toList().first()
                val fileName = primary.first
                val opacity = primary.second
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "شفافیت نقشه آفلاین: $fileName",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = fontColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${(opacity * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = opacity,
                            onValueChange = { gisViewModel.setMbtilesOpacity(fileName, it) },
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor
                            ),
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. RIGHT-SIDE FLOATING DOCK (COMPASS, RECENTER, ZOOM, SIMULATOR)
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Compass Widget
            CompassWidget(
                heading = currentHeading,
                isOutdoorMode = isOutdoorMode,
                useDeviceSensor = true,
                onClick = { showCompassModal = true },
                modifier = Modifier.size(46.dp)
            )

            // Recenter on GPS FAB
            Surface(
                onClick = {
                    lockGpsCenter = true
                    liveLocation?.let {
                        simulatorLat = it.latitude
                        simulatorLon = it.longitude
                    }
                    Toast.makeText(context, "مرکز نقشه بر روی موقعیت شما قفل شد", Toast.LENGTH_SHORT).show()
                },
                shape = CircleShape,
                color = cardBg,
                border = BorderStroke(1.5.dp, if (lockGpsCenter) accentColor else fontColor.copy(alpha = 0.25f)),
                shadowElevation = 4.dp,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (lockGpsCenter) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                        contentDescription = "مرکز بر موقعیت GPS",
                        tint = if (lockGpsCenter) accentColor else fontColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Compact Zoom Pill (+ / -)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, fontColor.copy(alpha = 0.2f)),
                shadowElevation = 3.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(36.dp)
                ) {
                    IconButton(
                        onClick = { gisViewModel.setZoomLevel((zoomLevel + 1).coerceAtMost(21)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "بزرگ‌نمایی", tint = fontColor, modifier = Modifier.size(18.dp))
                    }
                    Divider(color = fontColor.copy(alpha = 0.15f), modifier = Modifier.width(22.dp))
                    IconButton(
                        onClick = { gisViewModel.setZoomLevel((zoomLevel - 1).coerceAtLeast(3)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "کوچک‌نمایی", tint = fontColor, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Walk Simulator Controller Toggle
            Surface(
                onClick = { showMoveSimulator = !showMoveSimulator },
                shape = CircleShape,
                color = if (showMoveSimulator) accentColor else cardBg,
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                shadowElevation = 3.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = "شبیه‌ساز گام‌زنی",
                        tint = if (showMoveSimulator) Color.White else fontColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ==========================================
        // 3. WALK SIMULATOR D-PAD (WHEN ENABLED)
        // ==========================================
        if (showMoveSimulator) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 80.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.width(120.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("حرکت مجازی", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        IconButton(onClick = { showMoveSimulator = false }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = fontColor.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                        }
                    }
                    IconButton(onClick = { simulateWalk("NORTH") }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "North", tint = accentColor)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconButton(onClick = { simulateWalk("WEST") }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "West", tint = accentColor)
                        }
                        IconButton(onClick = { simulateWalk("EAST") }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "East", tint = accentColor)
                        }
                    }
                    IconButton(onClick = { simulateWalk("SOUTH") }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "South", tint = accentColor)
                    }
                }
            }
        }

        // ==========================================
        // 4. BOTTOM-START SCALE BAR DOCK
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 80.dp)
        ) {
            ScaleBar(zoomLevel = zoomLevel, isOutdoorMode = isOutdoorMode)
        }

        // ==========================================
        // 5. ERGONOMIC FIELD COMMAND BAR (BOTTOM)
        // ==========================================
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, if (isOutdoorMode) Color(0xFFD6DDD8) else accentColor.copy(alpha = 0.35f)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp, start = 16.dp, end = 16.dp)
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Add Station / Point
                FieldActionButton(
                    icon = Icons.Default.AddLocationAlt,
                    label = "ایستگاه",
                    tint = accentColor,
                    onClick = { showAddStationDialog = true }
                )

                // 2. Strike & Dip / Structural
                FieldActionButton(
                    icon = Icons.Default.Explore,
                    label = "ساختار",
                    tint = Color(0xFFFFB300),
                    onClick = { gisViewModel.updateUiState(MapUiState.MeasurementMode) }
                )

                // 3. Rock Sample
                FieldActionButton(
                    icon = Icons.Default.Science,
                    label = "نمونه",
                    tint = Color(0xFF00E676),
                    onClick = { gisViewModel.updateUiState(MapUiState.SampleCreationMode) }
                )

                // 4. Borehole
                FieldActionButton(
                    icon = Icons.Default.Layers,
                    label = "گمانه",
                    tint = Color(0xFF29B6F6),
                    onClick = { showAddBoreholeDialog = true }
                )

                // 5. GPS Track Recording Toggle
                FieldActionButton(
                    icon = if (isTrackRecordingActive) Icons.Default.StopCircle else Icons.Default.PlayCircle,
                    label = if (isTrackRecordingActive) "توقف" else "پیمایش",
                    tint = if (isTrackRecordingActive) Color(0xFFE53935) else Color(0xFF43A047),
                    onClick = {
                        if (isTrackRecordingActive) {
                            geologyViewModel.stopRecordingTrack { pointsJson, length, duration ->
                                geologyViewModel.saveTrack(
                                    name = "Track_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()),
                                    pointsJson = pointsJson,
                                    lengthMeters = length,
                                    durationSeconds = duration
                                )
                                Toast.makeText(context, "مسیر پیمایش با موفقیت ذخیره شد!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            geologyViewModel.startRecordingTrack()
                            Toast.makeText(context, "ضبط مسیر GPS آغاز شد...", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                // 6. Layers & Tools Bottom Sheet
                FieldActionButton(
                    icon = Icons.Default.Tune,
                    label = "لایه‌ها",
                    tint = fontColor,
                    onClick = { gisViewModel.updateUiState(MapUiState.LayersExpanded) }
                )
            }
        }

        // ==========================================
        // 6. MODAL BOTTOM SHEETS
        // ==========================================
        // Layer Manager Sheet (Base Maps, MBTiles, Overlays, Tools)
        if (uiState is MapUiState.LayersExpanded) {
            ModalBottomSheet(
                onDismissRequest = { gisViewModel.updateUiState(MapUiState.Idle) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = cardBg,
                dragHandle = { BottomSheetDefaults.DragHandle(color = accentColor) }
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
                    activeBaseMapOpacity = activeBaseMapOpacity,
                    onBaseMapOpacityChange = { gisViewModel.setBaseMapOpacity(it) },
                    offlineModeForce = offlineModeForce,
                    onToggleOfflineModeForce = { gisViewModel.setOfflineModeForce(it) },
                    showOfflineBoundaries = showOfflineBoundaries,
                    onToggleOfflineBoundaries = { gisViewModel.setShowOfflineBoundaries(it) },
                    downloadedRegionsList = downloadedRegionsList,
                    currentLat = currentLat,
                    currentLon = currentLon,
                    importedMbtilesFiles = importedMbtilesFiles,
                    activeMbtiles = activeMbtiles,
                    onToggleMbtilesActive = { name, active -> gisViewModel.toggleMbtilesActive(name, active) },
                    onMbtilesOpacityChange = { name, op -> gisViewModel.setMbtilesOpacity(name, op) },
                    onImportMbtilesClick = { mbtilesImportLauncher.launch("*/*") },
                    onDeleteMbtiles = { gisViewModel.deleteMbtilesFile(it) },
                    onImportGisVectorClick = { gisFileImportLauncher.launch("*/*") },
                    onDismiss = { gisViewModel.updateUiState(MapUiState.Idle) },
                    isOutdoorMode = isOutdoorMode,
                    onStartRuler = {
                        isRulerMode = true
                        isAreaMode = false
                        gisViewModel.updateUiState(MapUiState.Idle)
                    },
                    onStartArea = {
                        isAreaMode = true
                        isRulerMode = false
                        gisViewModel.updateUiState(MapUiState.Idle)
                    },
                    onShowStats = {
                        showProjectStatsDialog = true
                        gisViewModel.updateUiState(MapUiState.Idle)
                    },
                    onExportData = {
                        showExportDialog = true
                        gisViewModel.updateUiState(MapUiState.Idle)
                    }
                )
            }
        }

        // Structural Measurement Sheet (Strike & Dip)
        if (uiState is MapUiState.MeasurementMode) {
            ModalBottomSheet(
                onDismissRequest = { gisViewModel.updateUiState(MapUiState.Idle) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = cardBg
            ) {
                StructuralMeasurementSheet(
                    compassHeading = currentHeading,
                    onDismiss = { gisViewModel.updateUiState(MapUiState.Idle) },
                    stationId = stationsToShow.firstOrNull()?.id ?: 1,
                    isOutdoorMode = isOutdoorMode,
                    onSave = { obs ->
                        val targetStId = stationsToShow.firstOrNull()?.id ?: 1
                        geologyViewModel.saveObservation(
                            stationId = targetStId,
                            obsType = obs.observationType,
                            notes = obs.notes ?: "",
                            photoPath = null,
                            strike = obs.strike,
                            dip = obs.dip,
                            dipDirection = obs.dipDirection,
                            faultType = obs.faultType
                        )
                        gisViewModel.updateUiState(MapUiState.Idle)
                        Toast.makeText(context, "عوارض ساختاری با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Rock Sample Sheet
        if (uiState is MapUiState.SampleCreationMode) {
            ModalBottomSheet(
                onDismissRequest = { gisViewModel.updateUiState(MapUiState.Idle) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = cardBg
            ) {
                SamplePointSheet(
                    latitude = currentLat,
                    longitude = currentLon,
                    onDismiss = { gisViewModel.updateUiState(MapUiState.Idle) },
                    observationId = allObservationsList.firstOrNull()?.id ?: 1,
                    isOutdoorMode = isOutdoorMode,
                    onSave = { sample ->
                        geologyViewModel.saveSample(sample)
                        gisViewModel.updateUiState(MapUiState.Idle)
                        Toast.makeText(context, "نمونه سنگ با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Geological Observation Sheet
        if (uiState is MapUiState.ObservationCreationMode) {
            ModalBottomSheet(
                onDismissRequest = { gisViewModel.updateUiState(MapUiState.Idle) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = cardBg
            ) {
                GeologicalObservationSheet(
                    onDismiss = { gisViewModel.updateUiState(MapUiState.Idle) },
                    stationId = stationsToShow.firstOrNull()?.id ?: 1,
                    isOutdoorMode = isOutdoorMode,
                    onSave = { obs ->
                        geologyViewModel.saveObservation(
                            stationId = obs.stationId,
                            obsType = obs.observationType,
                            notes = obs.notes ?: "",
                            photoPath = null,
                            lithType = obs.lithologyType,
                            altType = obs.alterationType
                        )
                        gisViewModel.updateUiState(MapUiState.Idle)
                        Toast.makeText(context, "مشاهده زمین‌شناسی ذخیره شد", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // ==========================================
        // 7. DIALOGS
        // ==========================================
        // Add Station Dialog
        if (showAddStationDialog) {
            var stationCodeInput by remember { mutableStateOf("ST-" + String.format(Locale.US, "%03d", stationsToShow.size + 1)) }
            var stationNotesInput by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddStationDialog = false },
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = accentColor)
                        Text("ثبت ایستگاه مشاهداتی جدید", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fontColor)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "مختصات فعلی: ${String.format(Locale.US, "%.5f", currentLat)}°N , ${String.format(Locale.US, "%.5f", currentLon)}°E (▲${currentEle.toInt()}m)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontFamily = FontFamily.Monospace
                        )
                        OutlinedTextField(
                            value = stationCodeInput,
                            onValueChange = { stationCodeInput = it },
                            label = { Text("کد ایستگاه (Station Code)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = stationNotesInput,
                            onValueChange = { stationNotesInput = it },
                            label = { Text("توضیحات و لیتولوژی (Notes)") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val activeProjId = activeProject?.id ?: 1
                            val utm = UtmConverter.convertLatLngToUtm(currentLat, currentLon, activeProject?.utmZoneManual)
                            val newStation = Station(
                                id = 0,
                                projectId = activeProjId,
                                stationCode = stationCodeInput.ifBlank { "ST-${System.currentTimeMillis() % 1000}" },
                                prefix = "ST",
                                latitude = currentLat,
                                longitude = currentLon,
                                elevation = currentEle,
                                accuracy = currentAccuracy,
                                utmEasting = utm.easting,
                                utmNorthing = utm.northing,
                                utmZone = utm.zoneString
                            )
                            geologyViewModel.updateStation(newStation)
                            showAddStationDialog = false
                            Toast.makeText(context, "ایستگاه $stationCodeInput با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("ذخیره ایستگاه", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddStationDialog = false }) {
                        Text("انصراف", color = fontColor.copy(alpha = 0.6f))
                    }
                }
            )
        }

        // Add Borehole Dialog
        if (showAddBoreholeDialog) {
            var boreholeCodeInput by remember { mutableStateOf("BH-" + String.format(Locale.US, "%02d", (stationsToShow.size % 20) + 1)) }
            var plannedDepthInput by remember { mutableStateOf("150") }

            AlertDialog(
                onDismissRequest = { showAddBoreholeDialog = false },
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF29B6F6))
                        Text("ثبت دهانه گمانه اکتشافی (Borehole)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fontColor)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "موقعیت دهانه: ${String.format(Locale.US, "%.5f", currentLat)}°N , ${String.format(Locale.US, "%.5f", currentLon)}°E",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF29B6F6),
                            fontFamily = FontFamily.Monospace
                        )
                        OutlinedTextField(
                            value = boreholeCodeInput,
                            onValueChange = { boreholeCodeInput = it },
                            label = { Text("کد گمانه (Borehole ID)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = plannedDepthInput,
                            onValueChange = { plannedDepthInput = it },
                            label = { Text("عمق پیش‌بینی‌شده به متر (Planned Depth)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val activeProjId = activeProject?.id ?: 1
                            val utm = UtmConverter.convertLatLngToUtm(currentLat, currentLon, activeProject?.utmZoneManual)
                            val newStation = Station(
                                id = 0,
                                projectId = activeProjId,
                                stationCode = boreholeCodeInput.ifBlank { "BH-01" },
                                prefix = "BH",
                                latitude = currentLat,
                                longitude = currentLon,
                                elevation = currentEle,
                                accuracy = currentAccuracy,
                                utmEasting = utm.easting,
                                utmNorthing = utm.northing,
                                utmZone = utm.zoneString
                            )
                            geologyViewModel.updateStation(newStation)
                            showAddBoreholeDialog = false
                            Toast.makeText(context, "دهانه گمانه $boreholeCodeInput ثبت شد", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6))
                    ) {
                        Text("ثبت گمانه", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddBoreholeDialog = false }) {
                        Text("انصراف", color = fontColor.copy(alpha = 0.6f))
                    }
                }
            )
        }

        // Project Stats Dialog
        if (showProjectStatsDialog) {
            AlertDialog(
                onDismissRequest = { showProjectStatsDialog = false },
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = accentColor)
                        Text("داشبورد آماری پیمایش و اکتشاف", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fontColor)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("پروژه فعال: ${activeProject?.name ?: "پروژه عمومی"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fontColor)
                        Divider(color = fontColor.copy(alpha = 0.1f))
                        StatRow("تعداد ایستگاه‌ها (Stations):", "${stationsToShow.size}")
                        StatRow("تعداد ساختارهای ثبت‌شده:", "${allObservationsList.count { it.strike != null }}")
                        StatRow("تعداد نمونه‌های سنگی (Samples):", "${allSamplesList.size}")
                        StatRow("مسیرهای پیمایش ذخیره‌شده:", "${savedTracks.size}")
                        StatRow("نقشه‌های آفلاین MBTiles فعال:", "${activeMbtiles.size}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProjectStatsDialog = false }) {
                        Text("تایید و بستن", color = accentColor, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Export Dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = accentColor)
                        Text("خروجی اطلاعات GIS و نقشه‌ها", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fontColor)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("داده‌های این پروژه را می‌توانید به صورت فایل استاندارد استخراج کنید:", fontSize = 11.sp, color = fontColor.copy(alpha = 0.7f))
                        Button(
                            onClick = {
                                showExportDialog = false
                                Toast.makeText(context, "فایل GPX با ${stationsToShow.size} ایستگاه استخراج شد", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("خروجی فایل GPX (مختصات و مسیرها)", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = {
                                showExportDialog = false
                                Toast.makeText(context, "فایل KML برای Google Earth استخراج شد", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("خروجی KML (Google Earth / Global Mapper)", color = fontColor)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("بستن", color = fontColor.copy(alpha = 0.6f))
                    }
                }
            )
        }
    }
}

@Composable
private fun FieldActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Text(text = label, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
