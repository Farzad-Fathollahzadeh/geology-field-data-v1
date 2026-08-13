package com.example

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// --- SCREEN ROTATION / CONFIGURATION ROUTING FLOWS ---
sealed class Screen {
    object ProjectList : Screen()
    object CreateProject : Screen()
    data class ProjectDashboard(val projectId: Int) : Screen()
    data class CreateStation(val projectId: Int) : Screen()
    data class StationDetail(val stationId: Int) : Screen()
    data class AddObservation(val stationId: Int) : Screen()
}

// --- GPS CAPTURE STATE ---
data class GpsState(
    val isCaptured: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val elevation: Double = 0.0,
    val accuracy: Double = 0.0,
    val elevationAccuracy: Double = 0.0,
    val utmZone: String = "",
    val utmEasting: Double = 0.0,
    val utmNorthing: Double = 0.0,
    val isLocating: Boolean = false,
    val source: String = "None"
)

data class MapCoordinate(
    val lat: Double,
    val lon: Double
)

// --- SPATIAL DATA OVERLAY MODELS ---
data class Coord(val lat: Double, val lon: Double)

data class ImportedFeature(
    val id: String,
    val name: String,
    val description: String,
    val type: String, // e.g. "LineString", "Polygon"
    val coordinates: List<Coord>,
    val colorHex: String
)

// --- CORRESPONDING GEOLOGY VIEWMODEL ---
class GeologyViewModel(
    val application: Application,
    val dao: GeologyDao,
    val repository: GeologyRepository = (application as GeologyApplication).repository
) : ViewModel() {

    private val prefs = application.getSharedPreferences("geofieldpro_prefs", Context.MODE_PRIVATE)

    // Basic flows backed by Room Repository & StateFlow
    val currentScreen = MutableStateFlow<Screen>(Screen.ProjectList)
    val projects: StateFlow<List<Project>> = repository.projects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allStations: StateFlow<List<Station>> = repository.allStations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allObservations: StateFlow<List<Observation>> = repository.allObservations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tracks: StateFlow<List<Track>> = repository.tracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSamples: StateFlow<List<Sample>> = repository.allSamples
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Selection State Flows
    private val _activeProject = MutableStateFlow<Project?>(null)
    val activeProject: StateFlow<Project?> = _activeProject

    private val _activeProjectStations = MutableStateFlow<List<Station>>(emptyList())
    val activeProjectStations: StateFlow<List<Station>> = _activeProjectStations

    private val _activeStation = MutableStateFlow<Station?>(null)
    val activeStation: StateFlow<Station?> = _activeStation

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeStationObservations: StateFlow<List<Observation>> = _activeStation
        .flatMapLatest { station ->
            if (station != null) {
                dao.getObservationsForStation(station.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Preference State Flows
    val themeSetting = MutableStateFlow(prefs.getString("theme", "Light") ?: "Light")
    val fontSizeSetting = MutableStateFlow(prefs.getString("font_size", "Standard") ?: "Standard")
    val coordSystemSetting = MutableStateFlow(prefs.getString("coord_system", "UTM") ?: "UTM")
    val customEpsgCode = MutableStateFlow(prefs.getString("custom_epsg_code", "32639") ?: "32639")
    val isFarsiModeSetting = MutableStateFlow(prefs.getBoolean("is_farsi_mode", false))
    val isKalmanSmoothingEnabled = MutableStateFlow(prefs.getBoolean("kalman_smoothing", true))
    val gpsAccuracyLimit = MutableStateFlow(prefs.getFloat("gps_accuracy_limit", 20.0f))
    val gpsMinDisplacement = MutableStateFlow(prefs.getFloat("gps_min_displacement", 2.0f))
    val gpsIntervalSetting = MutableStateFlow(prefs.getString("gps_interval", "Balanced") ?: "Balanced")

    // GPS drift and noise filtering preferences
    val isMovingAverageEnabled = MutableStateFlow(prefs.getBoolean("moving_average_enabled", true))
    val movingAverageWindowSize = MutableStateFlow(prefs.getInt("moving_average_window", 5))
    val geoidHeightCorrectionOffset = MutableStateFlow(prefs.getFloat("geoid_height_correction", -25.0f))
    val trackSpeedFilterEnabled = MutableStateFlow(prefs.getBoolean("track_speed_filter", true))
    val trackMinTimeBetweenPoints = MutableStateFlow(prefs.getInt("track_min_time", 3))

    // Custom Map Tile Layers
    val customTileUrl = MutableStateFlow(prefs.getString("custom_tile_url", "") ?: "")
    val customTileLabel = MutableStateFlow(prefs.getString("custom_tile_label", "") ?: "")
    val useCustomTile = MutableStateFlow(prefs.getBoolean("use_custom_tile", false))

    // Dynamic Spatial Data Overlays
    val importedFeatures = MutableStateFlow<List<ImportedFeature>>(emptyList())

    // Location Tracking Flows
    val liveLocation = MutableStateFlow<Location?>(null)
    val gpsState = MutableStateFlow(GpsState())
    val liveSatelliteCount = MutableStateFlow(10) // Mock base satellite count

    // Track Recording Flow States
    val isTrackRecordingActive = MutableStateFlow(false)
    val isTrackRecordingPaused = MutableStateFlow(false)
    val activePoints = MutableStateFlow<List<TrackPoint>>(emptyList())
    val recordingDurationSec = MutableStateFlow(0L)

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var isCallbackRegistered = false
    private val gpsKalmanFilter = GpsKalmanFilter()
    private var recordingJob: Job? = null
    private var startRecordingTimeMs = 0L

    private var projectCollectJob: Job? = null
    private var stationsCollectJob: Job? = null
    private var loadStationJob: Job? = null

    // Initialize state
    init {
        viewModelScope.launch {
            // Load initial states if needed
        }
    }

    // Navigation & State loader
    fun navigateTo(screen: Screen) {
        currentScreen.value = screen
        viewModelScope.launch {
            when (screen) {
                is Screen.ProjectDashboard -> {
                    loadProject(screen.projectId)
                }
                is Screen.CreateStation -> {
                    loadProject(screen.projectId)
                }
                is Screen.StationDetail -> {
                    loadStation(screen.stationId)
                }
                is Screen.AddObservation -> {
                    loadStation(screen.stationId)
                }
                else -> {}
            }
        }
    }

    fun loadProject(projectId: Int) {
        projectCollectJob?.cancel()
        stationsCollectJob?.cancel()
        
        projectCollectJob = viewModelScope.launch {
            dao.getProjectById(projectId).collect { proj ->
                _activeProject.value = proj
            }
        }
        stationsCollectJob = viewModelScope.launch {
            dao.getStationsForProject(projectId).collect { list ->
                _activeProjectStations.value = list
            }
        }
    }

    fun loadStation(stationId: Int) {
        loadStationJob?.cancel()
        loadStationJob = viewModelScope.launch {
            dao.getStationById(stationId).collect { station ->
                _activeStation.value = station
                station?.projectId?.let { loadProject(it) }
            }
        }
    }

    // Setters for SharedPreferences Settings
    fun setThemeSetting(theme: String) {
        themeSetting.value = theme
        prefs.edit().putString("theme", theme).apply()
    }

    fun setFontSizeSetting(size: String) {
        fontSizeSetting.value = size
        prefs.edit().putString("font_size", size).apply()
    }

    fun setCoordSystemSetting(coord: String) {
        coordSystemSetting.value = coord
        prefs.edit().putString("coord_system", coord).apply()
    }

    fun setCustomEpsgCode(code: String) {
        customEpsgCode.value = code
        prefs.edit().putString("custom_epsg_code", code).apply()
    }

    fun setFarsiMode(enabled: Boolean) {
        isFarsiModeSetting.value = enabled
        prefs.edit().putBoolean("is_farsi_mode", enabled).apply()
    }

    fun setKalmanSmoothingEnabled(enabled: Boolean) {
        isKalmanSmoothingEnabled.value = enabled
        prefs.edit().putBoolean("kalman_smoothing", enabled).apply()
    }

    fun setGpsAccuracyLimit(limit: Float) {
        gpsAccuracyLimit.value = limit
        prefs.edit().putFloat("gps_accuracy_limit", limit).apply()
    }

    fun setGpsMinDisplacement(disp: Float) {
        gpsMinDisplacement.value = disp
        prefs.edit().putFloat("gps_min_displacement", disp).apply()
    }

    fun setMovingAverageEnabled(enabled: Boolean) {
        isMovingAverageEnabled.value = enabled
        prefs.edit().putBoolean("moving_average_enabled", enabled).apply()
    }

    fun setMovingAverageWindowSize(size: Int) {
        movingAverageWindowSize.value = size
        prefs.edit().putInt("moving_average_window", size).apply()
    }

    fun setGeoidHeightCorrectionOffset(offset: Float) {
        geoidHeightCorrectionOffset.value = offset
        prefs.edit().putFloat("geoid_height_correction", offset).apply()
    }

    fun setTrackSpeedFilterEnabled(enabled: Boolean) {
        trackSpeedFilterEnabled.value = enabled
        prefs.edit().putBoolean("track_speed_filter", enabled).apply()
    }

    fun setTrackMinTimeBetweenPoints(seconds: Int) {
        trackMinTimeBetweenPoints.value = seconds
        prefs.edit().putInt("track_min_time", seconds).apply()
    }

    fun setCustomTileUrl(url: String) {
        customTileUrl.value = url
        prefs.edit().putString("custom_tile_url", url).apply()
    }

    fun setCustomTileLabel(label: String) {
        customTileLabel.value = label
        prefs.edit().putString("custom_tile_label", label).apply()
    }

    fun setUseCustomTile(enabled: Boolean) {
        useCustomTile.value = enabled
        prefs.edit().putBoolean("use_custom_tile", enabled).apply()
    }

    fun setGpsIntervalSetting(interval: String, context: Context?) {
        gpsIntervalSetting.value = interval
        prefs.edit().putString("gps_interval", interval).apply()
        if (context != null && (locationCallback != null)) {
            startLocationTracking(context)
        }
    }

    fun clearImportedFeatures() {
        importedFeatures.value = emptyList()
    }

    // Projects CRUD
    fun createProject(name: String, description: String?, date: String, utmZoneMode: String, utmZoneManual: String) {
        viewModelScope.launch {
            val proj = Project(
                name = name,
                description = description,
                date = date,
                utmZoneMode = utmZoneMode,
                utmZoneManual = utmZoneManual
            )
            val newId = dao.insertProject(proj).toInt()
            loadProject(newId)
            navigateTo(Screen.ProjectDashboard(newId))
        }
    }

    fun updateProjectSettings(projectId: Int, zoneMode: String, manualZone: String) {
        viewModelScope.launch {
            val proj = dao.getProjectById(projectId).firstOrNull()
            if (proj != null) {
                dao.insertProject(proj.copy(utmZoneMode = zoneMode, utmZoneManual = manualZone))
                loadProject(projectId)
            }
        }
    }

    fun deleteProject(projectId: Int) {
        viewModelScope.launch {
            dao.deleteProjectById(projectId)
            if (_activeProject.value?.id == projectId) {
                _activeProject.value = null
                _activeProjectStations.value = emptyList()
            }
        }
    }

    // GPS & Fused Location Service management
    fun startLocationTracking(context: Context) {
        stopLocationTracking() // avoid leaks
        
        val hasFinePermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasCoarsePermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasFinePermission && !hasCoarsePermission) {
            // Safe guard against AppOps and permissions issues
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient = client

        val intervalMs = when (gpsIntervalSetting.value) {
            "UltraPrecise" -> 1000L
            "BatterySaver" -> 10000L
            else -> 2500L
        }

        // Dynamically choose location request priority to prevent AppOps/permission mismatch errors
        val priority = if (hasFinePermission) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val locationRequest = LocationRequest.Builder(priority, intervalMs)
            .apply {
                setMinUpdateIntervalMillis(intervalMs / 2)
            }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                
                val correctedElevation = loc.altitude + geoidHeightCorrectionOffset.value.toDouble()
                
                // Live Location smoothing
                val (displayLat, displayLon) = if (isKalmanSmoothingEnabled.value) {
                    gpsKalmanFilter.process(loc.latitude, loc.longitude, loc.accuracy.toDouble(), System.currentTimeMillis())
                } else {
                    Pair(loc.latitude, loc.longitude)
                }
                
                val smoothedLoc = Location(loc).apply {
                    latitude = displayLat
                    longitude = displayLon
                    altitude = correctedElevation
                }
                liveLocation.value = smoothedLoc
                
                gpsState.value = GpsState(
                    isCaptured = true,
                    latitude = displayLat,
                    longitude = displayLon,
                    elevation = correctedElevation,
                    accuracy = loc.accuracy.toDouble()
                )
                
                // Append active tracking points for track registration
                if (isTrackRecordingActive.value && !isTrackRecordingPaused.value) {
                    val accLimit = gpsAccuracyLimit.value.toDouble()
                    if (loc.accuracy <= accLimit) {
                        val (finalLat, finalLon) = if (isKalmanSmoothingEnabled.value) {
                            gpsKalmanFilter.process(loc.latitude, loc.longitude, loc.accuracy.toDouble(), System.currentTimeMillis())
                        } else {
                            Pair(loc.latitude, loc.longitude)
                        }

                        // Apply moving average if enabled
                        var smoothedLat = finalLat
                        var smoothedLon = finalLon
                        if (isMovingAverageEnabled.value) {
                            val window = movingAverageWindowSize.value.coerceAtLeast(1)
                            val recentPoints = activePoints.value.takeLast(window - 1)
                            if (recentPoints.isNotEmpty()) {
                                val sumLat = recentPoints.sumOf { it.lat } + finalLat
                                val sumLon = recentPoints.sumOf { it.lon } + finalLon
                                smoothedLat = sumLat / (recentPoints.size + 1)
                                smoothedLon = sumLon / (recentPoints.size + 1)
                            }
                        }

                        val lastPoint = activePoints.value.lastOrNull()
                        var shouldAppend = true

                        // 1. Min Time Interval Check to filter out excessively frequent updates
                        if (lastPoint != null) {
                            val timeDiffSec = (System.currentTimeMillis() - lastPoint.time) / 1000.0
                            if (timeDiffSec < trackMinTimeBetweenPoints.value.toDouble()) {
                                shouldAppend = false
                            }
                        }

                        // 2. Speed Spike Check (if calculated speed > 15 m/s or 54 km/h, ignore as GPS noise)
                        if (shouldAppend && lastPoint != null && trackSpeedFilterEnabled.value) {
                            val results = FloatArray(1)
                            Location.distanceBetween(lastPoint.lat, lastPoint.lon, smoothedLat, smoothedLon, results)
                            val distance = results[0]
                            val timeDiffSec = (System.currentTimeMillis() - lastPoint.time) / 1000.0
                            if (timeDiffSec > 0.1) {
                                val calculatedSpeed = distance / timeDiffSec
                                if (calculatedSpeed > 15.0) { // Unrealistic hike speed, discard spike
                                    shouldAppend = false
                                }
                            }
                        }

                        // 3. Min Displacement Check
                        if (shouldAppend && lastPoint != null) {
                            val results = FloatArray(1)
                            Location.distanceBetween(lastPoint.lat, lastPoint.lon, smoothedLat, smoothedLon, results)
                            val distance = results[0]
                            if (distance < gpsMinDisplacement.value.toDouble()) {
                                shouldAppend = false
                            }
                        }

                        if (shouldAppend) {
                            val p = TrackPoint(
                                lat = smoothedLat,
                                lon = smoothedLon,
                                ele = correctedElevation,
                                time = System.currentTimeMillis(),
                                accuracy = loc.accuracy.toDouble(),
                                heading = loc.bearing.toDouble()
                            )
                            activePoints.value = activePoints.value + p
                        }
                    }
                }
            }
        }
        locationCallback = callback
        isCallbackRegistered = false

        try {
            client.requestLocationUpdates(locationRequest, callback, android.os.Looper.getMainLooper())
                .addOnSuccessListener {
                    isCallbackRegistered = true
                }
                .addOnFailureListener {
                    isCallbackRegistered = false
                    if (locationCallback == callback) {
                        locationCallback = null
                    }
                }
        } catch (e: java.lang.SecurityException) {
            // Clean up state if registration fails to avoid AppOps warnings on removal
            isCallbackRegistered = false
            locationCallback = null
            fusedLocationClient = null
        } catch (e: Exception) {
            // Clean up state if registration fails
            isCallbackRegistered = false
            locationCallback = null
            fusedLocationClient = null
        }
    }

    fun stopLocationTracking() {
        try {
            if (isCallbackRegistered) {
                locationCallback?.let {
                    fusedLocationClient?.removeLocationUpdates(it)
                }
            }
        } catch (e: Exception) {
            // Handled gracefully
        } finally {
            isCallbackRegistered = false
            locationCallback = null
            fusedLocationClient = null
        }
    }



    // Tracks Registration & Management
    fun startRecordingTrack() {
        gpsKalmanFilter.reset()
        isTrackRecordingActive.value = true
        isTrackRecordingPaused.value = false
        activePoints.value = emptyList()
        recordingDurationSec.value = 0L
        startRecordingTimeMs = System.currentTimeMillis()
        recordingJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                if (!isTrackRecordingPaused.value) {
                    recordingDurationSec.value += 1
                }
            }
        }
    }

    fun pauseRecordingTrack() {
        isTrackRecordingPaused.value = true
    }

    fun resumeRecordingTrack() {
        isTrackRecordingPaused.value = false
    }

    fun stopRecordingTrack(onDone: (String, Double, Long) -> Unit) {
        isTrackRecordingActive.value = false
        isTrackRecordingPaused.value = false
        recordingJob?.cancel()
        recordingJob = null
        
        val duration = recordingDurationSec.value
        val list = activePoints.value
        val pointsJson = list.joinToString(prefix = "[", postfix = "]") { p ->
            "[${p.lat},${p.lon},${p.ele},${p.time},${p.heading}]"
        }
        val length = calculateTrackDistance(list)
        onDone(pointsJson, length, duration)
    }

    private fun calculateTrackDistance(points: List<TrackPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            val res = FloatArray(1)
            Location.distanceBetween(
                points[i].lat, points[i].lon,
                points[i + 1].lat, points[i + 1].lon,
                res
            )
            total += res[0]
        }
        return total
    }

    fun saveTrack(
        name: String,
        pointsJson: String,
        lengthMeters: Double,
        durationSeconds: Long,
        colorHex: String = "#ff3d00",
        strokeWidth: Float = 4.5f
    ) {
        viewModelScope.launch {
            val projId = _activeProject.value?.id ?: 0
            dao.insertTrack(
                Track(
                    projectId = projId,
                    name = name,
                    pointsJson = pointsJson,
                    lengthMeters = lengthMeters,
                    durationSeconds = durationSeconds,
                    colorHex = colorHex,
                    strokeWidth = strokeWidth
                )
            )
        }
    }

    fun deleteTrack(trackId: Int) {
        viewModelScope.launch {
            dao.deleteTrackById(trackId)
        }
    }

    fun updateTrackName(trackId: Int, name: String) {
        viewModelScope.launch {
            dao.updateTrackName(trackId, name)
        }
    }

    fun updateTrackStyling(trackId: Int, name: String, colorHex: String, strokeWidth: Float) {
        viewModelScope.launch {
            dao.updateTrackStyling(trackId, name, colorHex, strokeWidth)
        }
    }

    fun updateTrackPoints(trackId: Int, pointsJson: String) {
        viewModelScope.launch {
            dao.updateTrackPoints(trackId, pointsJson)
        }
    }

    fun appendTrackPoint(p: TrackPoint) {
        if (isTrackRecordingActive.value) {
            activePoints.value = activePoints.value + p
        }
    }



    // Stations Saving and Insertion
    fun saveStation(
        projectId: Int,
        stationCode: String,
        prefix: String,
        onSaved: ((Int) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val gps = gpsState.value
            val utm = UtmConverter.convertLatLngToUtm(gps.latitude, gps.longitude, _activeProject.value?.utmZoneManual)
            val station = Station(
                projectId = projectId,
                stationCode = stationCode,
                prefix = prefix,
                latitude = gps.latitude,
                longitude = gps.longitude,
                elevation = gps.elevation,
                accuracy = gps.accuracy,
                utmEasting = utm.easting,
                utmNorthing = utm.northing,
                utmZone = utm.zoneString
            )
            val newId = dao.insertStation(station).toInt()
            loadProject(projectId)
            onSaved?.invoke(newId)
        }
    }

    fun saveStationWithObservation(
        projectId: Int,
        stationCode: String,
        prefix: String,
        obsType: String,
        notes: String?,
        photoPath: String?,
        
        lithType: String? = null,
        texture: String? = null,
        color: String? = null,
        grainSize: String? = null,
        lithAlteration: String? = null,
        lithMineralization: String? = null,
        
        altType: String? = null,
        altIntensity: String? = null,
        hostRock: String? = null,
        altAssociated: String? = null,
        
        faultType: String? = null,
        strike: Int? = null,
        dip: Int? = null,
        dipDirection: Int? = null,
        rake: Int? = null,
        rakeDirection: String? = null,
        movement: String? = null,
        width: Double? = null,
        
        jointType: String? = null,
        jointSpacing: Double? = null,
        
        veinType: String? = null,
        veinMineralogy: String? = null,
        
        structureType: String? = null,
        
        minStyle: String? = null,
        minAssociatedMinerals: String? = null,
        minGradeEstimate: String? = null,
        
        sampleTaken: Boolean = false,
        sampleIdCode: String? = null,
        sampleType: String? = null,
        sampleDesc: String? = null,
        
        onSaved: ((Int) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val gps = gpsState.value
            val utm = UtmConverter.convertLatLngToUtm(gps.latitude, gps.longitude, _activeProject.value?.utmZoneManual)
            
            val station = Station(
                projectId = projectId,
                stationCode = stationCode,
                prefix = prefix,
                latitude = gps.latitude,
                longitude = gps.longitude,
                elevation = gps.elevation,
                accuracy = gps.accuracy,
                utmEasting = utm.easting,
                utmNorthing = utm.northing,
                utmZone = utm.zoneString
            )
            val stId = dao.insertStation(station).toInt()
            
            val observation = Observation(
                stationId = stId,
                observationType = obsType,
                photoPath = photoPath,
                notes = notes,
                
                lithologyType = lithType,
                texture = texture,
                color = color,
                grainSize = grainSize,
                lithologyAlteration = lithAlteration,
                lithologyMineralization = lithMineralization,
                
                alterationType = altType,
                intensity = altIntensity,
                hostRock = hostRock,
                associatedMinerals = altAssociated,
                
                faultType = faultType,
                strike = strike,
                dip = dip,
                dipDirection = dipDirection,
                rake = rake,
                rakeDirection = rakeDirection,
                movement = movement,
                width = width,
                
                jointType = jointType,
                jointSpacing = jointSpacing,
                
                veinType = veinType,
                veinMineralogy = veinMineralogy,
                
                structureType = structureType,
                
                minStyle = minStyle,
                minAssociatedMinerals = minAssociatedMinerals,
                minGradeEstimate = minGradeEstimate
            )
            val obsId = dao.insertObservation(observation).toInt()
            
            if (sampleTaken && !sampleIdCode.isNullOrBlank()) {
                val sample = Sample(
                    observationId = obsId,
                    sampleCodeId = sampleIdCode,
                    sampleType = sampleType ?: "Hand specimen",
                    description = sampleDesc,
                    photoPath = null
                )
                dao.insertSample(sample)
            }
            
            loadProject(projectId)
            onSaved?.invoke(stId)
        }
    }

    fun deleteStation(stationId: Int, projectId: Int) {
        viewModelScope.launch {
            dao.deleteStationById(stationId)
            loadProject(projectId)
        }
    }

    // Spatial and File GIS importers
    fun importMapFile(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val uriStr = uri.toString()
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val isGpx = uriStr.endsWith(".gpx", ignoreCase = true) || mimeType.contains("gpx", ignoreCase = true)
                val isKmz = uriStr.endsWith(".kmz", ignoreCase = true) || mimeType.contains("kmz", ignoreCase = true)
                
                val features = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        if (isGpx) {
                            GpxParser.parse(inputStream)
                        } else {
                            KmlParser.parse(inputStream, isKmz)
                        }
                    } ?: emptyList()
                }

                if (features.isNotEmpty()) {
                    importedFeatures.value = features
                    withContext(Dispatchers.Main) {
                        val isFarsi = isFarsiModeSetting.value
                        val message = if (isFarsi) {
                            "تعداد ${features.size} لایه با موفقیت وارد شد!"
                        } else {
                            "Successfully imported ${features.size} map layers!"
                        }
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val isFarsi = isFarsiModeSetting.value
                        val message = if (isFarsi) {
                            "هیچ لایه‌ای یافت نشد یا فرمت فایل نامعتبر است."
                        } else {
                            "No map layers found or invalid KML/KMZ format."
                        }
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun importStationsFromFile(context: Context, uri: android.net.Uri, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            var count = 0
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val projId = _activeProject.value?.id ?: 0
                if (projId != 0) {
                    text.split("\n").forEach { line ->
                        val p = line.split(",")
                        if (p.size >= 5) {
                            val code = p[0].trim()
                            val prefix = p[1].trim()
                            val lat = p[2].toDoubleOrNull()
                            val lon = p[3].toDoubleOrNull()
                            val ele = p[4].toDoubleOrNull()
                            if (lat != null && lon != null) {
                                val utm = UtmConverter.convertLatLngToUtm(lat, lon, _activeProject.value?.utmZoneManual)
                                val st = Station(
                                    projectId = projId,
                                    stationCode = code,
                                    prefix = prefix,
                                    latitude = lat,
                                    longitude = lon,
                                    elevation = ele ?: 1320.0,
                                    accuracy = 3.0,
                                    utmEasting = utm.easting,
                                    utmNorthing = utm.northing,
                                    utmZone = utm.zoneString
                                )
                                dao.insertStation(st)
                                count++
                            }
                        }
                    }
                    loadProject(projId)
                }
            } catch (e: Exception) {
                // ignore
            }
            onDone(count)
        }
    }

    fun seedDemoDataset(context: Context) {
        viewModelScope.launch {
            val existing = dao.getAllProjects().firstOrNull() ?: emptyList()
            if (existing.isEmpty()) {
                val projId = dao.insertProject(
                    Project(
                        name = "Zanjan Copper Exploration (Porphyry)",
                        description = "Regional exploration geochemical drift and structural survey.",
                        date = "1402/10/25"
                    )
                ).toInt()
                
                val stId = dao.insertStation(
                    Station(
                        projectId = projId,
                        stationCode = "LITH-001",
                        prefix = "LITH",
                        latitude = 36.6712,
                        longitude = 48.4842,
                        elevation = 1640.0,
                        accuracy = 1.8,
                        utmEasting = 275142.1,
                        utmNorthing = 4061482.9,
                        utmZone = "39N"
                    )
                ).toInt()
                
                dao.insertObservation(
                    Observation(
                        stationId = stId,
                        observationType = "Lithology",
                        notes = "Moderately phyllic-altered andesite porphyry with disseminated chalcopyrite grains.",
                        lithologyType = "Andesite Porphyry",
                        texture = "Porphyritic",
                        color = "Light Greenish Grey",
                        grainSize = "Medium",
                        lithologyAlteration = "Phyllic",
                        lithologyMineralization = "Chalcopyrite"
                    )
                )
                loadProject(projId)
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            val projs = dao.getAllProjects().firstOrNull() ?: emptyList()
            projs.forEach { dao.deleteProjectById(it.id) }
            val trs = dao.getAllTracks().firstOrNull() ?: emptyList()
            trs.forEach { dao.deleteTrackById(it.id) }
            
            _activeProject.value = null
            _activeProjectStations.value = emptyList()
            _activeStation.value = null
        }
    }

    // --- BOREHOLE LOGGING STATE FLOWS ---
    private val _activeBorehole = MutableStateFlow<Borehole?>(null)
    val activeBorehole: StateFlow<Borehole?> = _activeBorehole

    fun setActiveProject(projectId: Int) {
        loadProject(projectId)
    }

    fun setActiveStation(stationId: Int) {
        loadStation(stationId)
    }

    fun setActiveBorehole(id: Int?) {
        if (id == null) {
            _activeBorehole.value = null
            return
        }
        viewModelScope.launch {
            dao.getBoreholeById(id).collect { borehole ->
                _activeBorehole.value = borehole
            }
        }
    }

    fun getBoreholesForProject(projectId: Int): Flow<List<Borehole>> {
        return dao.getBoreholesForProject(projectId)
    }

    fun getDailyReportsForBorehole(boreholeId: Int): Flow<List<BoreholeDailyReport>> {
        return dao.getDailyReportsForBorehole(boreholeId)
    }

    fun getCoreRunsForBorehole(boreholeId: Int): Flow<List<BoreholeCoreRun>> {
        return dao.getCoreRunsForBorehole(boreholeId)
    }

    fun saveBorehole(borehole: Borehole, onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            dao.insertBorehole(borehole)
            onSaved?.invoke()
        }
    }

    fun deleteBorehole(id: Int) {
        viewModelScope.launch {
            dao.deleteBoreholeById(id)
            if (_activeBorehole.value?.id == id) {
                _activeBorehole.value = null
            }
        }
    }

    fun saveDailyReport(report: BoreholeDailyReport) {
        viewModelScope.launch {
            dao.insertDailyReport(report)
        }
    }

    fun deleteDailyReport(id: Int) {
        viewModelScope.launch {
            dao.deleteDailyReportById(id)
        }
    }

    fun saveCoreRun(coreRun: BoreholeCoreRun) {
        viewModelScope.launch {
            dao.insertCoreRun(coreRun)
        }
    }

    fun deleteCoreRun(id: Int) {
        viewModelScope.launch {
            dao.deleteCoreRunById(id)
        }
    }

    // --- ROCK MECHANICS STATE FLOWS ---
    fun getRockMechanicsEvaluationsForProject(projectId: Int): Flow<List<RockMechanicsEvaluation>> {
        return dao.getRockMechanicsEvaluationsForProject(projectId)
    }

    fun saveRockMechanicsEvaluation(evaluation: RockMechanicsEvaluation) {
        viewModelScope.launch {
            dao.insertRockMechanicsEvaluation(evaluation)
        }
    }

    fun deleteRockMechanicsEvaluation(id: Int) {
        viewModelScope.launch {
            dao.deleteRockMechanicsEvaluationById(id)
        }
    }

    // --- SPATIAL COORD CONVERTER ---
    fun getNextStationCodeForPrefix(projectId: Int, prefix: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val stations = dao.getStationsForProjectSync(projectId)
            val filtered = stations.filter { it.prefix == prefix }
            if (filtered.isEmpty()) {
                onResult("$prefix-001")
            } else {
                val maxNum = filtered.mapNotNull { s ->
                    val part = s.stationCode.substringAfter("-")
                    part.toIntOrNull()
                }.maxOrNull() ?: 0
                onResult(String.format(java.util.Locale.US, "%s-%03d", prefix, maxNum + 1))
            }
        }
    }

    // --- ACTIVE POSITION CAPTURING ---
    fun resetGpsState() {
        gpsState.value = GpsState()
    }

    fun captureLocation(context: Context, mode: String) {
        if (mode == "GPS") {
            startLocationTracking(context)
        } else {
            setManualCoordinates(35.6892, 51.3890, 1200.0, 5.0)
        }
    }

    fun setManualCoordinates(lat: Double, lon: Double, elevation: Double, accuracy: Double) {
        val utm = UtmConverter.convertLatLngToUtm(lat, lon, _activeProject.value?.utmZoneManual)
        gpsState.value = GpsState(
            isCaptured = true,
            latitude = lat,
            longitude = lon,
            elevation = elevation,
            accuracy = accuracy,
            elevationAccuracy = accuracy,
            utmZone = utm.zoneString,
            utmEasting = utm.easting,
            utmNorthing = utm.northing,
            isLocating = false,
            source = "MANUAL"
        )
    }

    fun createProjectSilently(name: String, description: String?, onCreated: (Int) -> Unit) {
        viewModelScope.launch {
            val formattedDate = try {
                val cal = java.util.Calendar.getInstance()
                val yy = cal.get(java.util.Calendar.YEAR) - 621
                val mm = cal.get(java.util.Calendar.MONTH) + 1
                val dd = cal.get(java.util.Calendar.DAY_OF_MONTH)
                String.format(java.util.Locale.US, "%04d/%02d/%02d", yy, mm, dd)
            } catch (e: Exception) {
                "1405/01/01"
            }
            val proj = Project(
                name = name,
                description = description,
                date = formattedDate
            )
            val newId = dao.insertProject(proj).toInt()
            loadProject(newId)
            onCreated(newId)
        }
    }

    // --- BASE DATA SAVERS AND UPDATERS ---
    fun updateProject(project: Project) {
        viewModelScope.launch {
            dao.insertProject(project)
            loadProject(project.id)
        }
    }

    fun updateStation(station: Station) {
        viewModelScope.launch {
            dao.insertStation(station)
            loadStation(station.id)
        }
    }

    fun updateObservation(observation: Observation) {
        viewModelScope.launch {
            dao.insertObservation(observation)
            loadStation(observation.stationId)
        }
    }

    fun deleteObservation(observation: Observation) {
        viewModelScope.launch {
            dao.deleteObservationById(observation.id)
            loadStation(observation.stationId)
        }
    }

    fun deleteObservation(id: Int) {
        viewModelScope.launch {
            dao.getObservationById(id)?.let { obs ->
                dao.deleteObservationById(id)
                loadStation(obs.stationId)
            }
        }
    }

    fun saveSample(sample: Sample, onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            dao.insertSample(sample)
            onSaved?.invoke()
        }
    }

    fun saveObservation(
        stationId: Int,
        obsType: String,
        notes: String?,
        photoPath: String?,
        
        lithType: String? = null,
        texture: String? = null,
        color: String? = null,
        grainSize: String? = null,
        lithAlteration: String? = null,
        lithMineralization: String? = null,
        
        altType: String? = null,
        altIntensity: String? = null,
        hostRock: String? = null,
        altAssociated: String? = null,
        
        faultType: String? = null,
        strike: Int? = null,
        dip: Int? = null,
        dipDirection: Int? = null,
        rake: Int? = null,
        rakeDirection: String? = null,
        movement: String? = null,
        width: Double? = null,
        
        jointType: String? = null,
        jointSpacing: Double? = null,
        
        veinType: String? = null,
        veinMineralogy: String? = null,
        
        structureType: String? = null,
        
        minStyle: String? = null,
        minAssociatedMinerals: String? = null,
        minGradeEstimate: String? = null,
        
        sampleTaken: Boolean = false,
        sampleIdCode: String? = null,
        sampleType: String? = null,
        sampleDesc: String? = null,
        samplePhotoPath: String? = null,
        
        onSaved: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val observation = Observation(
                stationId = stationId,
                observationType = obsType,
                photoPath = photoPath ?: "",
                notes = notes ?: "",
                
                lithologyType = lithType,
                texture = texture,
                color = color,
                grainSize = grainSize,
                lithologyAlteration = lithAlteration,
                lithologyMineralization = lithMineralization,
                
                alterationType = altType,
                intensity = altIntensity,
                hostRock = hostRock,
                associatedMinerals = altAssociated,
                
                faultType = faultType,
                strike = strike,
                dip = dip,
                dipDirection = dipDirection,
                rake = rake,
                rakeDirection = rakeDirection,
                movement = movement,
                width = width,
                
                jointType = jointType,
                jointSpacing = jointSpacing,
                
                veinType = veinType,
                veinMineralogy = veinMineralogy,
                
                structureType = structureType,
                
                minStyle = minStyle,
                minAssociatedMinerals = minAssociatedMinerals,
                minGradeEstimate = minGradeEstimate
            )
            val obsId = dao.insertObservation(observation).toInt()
            
            if (sampleTaken && !sampleIdCode.isNullOrBlank()) {
                val sample = Sample(
                    observationId = obsId,
                    sampleCodeId = sampleIdCode,
                    sampleType = sampleType ?: "Hand specimen",
                    description = sampleDesc,
                    photoPath = samplePhotoPath
                )
                dao.insertSample(sample)
            }
            
            loadStation(stationId)
            onSaved?.invoke()
        }
    }

    // --- REAL INTEGRATION EXPORTS (EXPOSING HIGHLY POLISHED DOCUMENT OUTPUTS) ---
    private fun shareTextFile(context: Context, fileName: String, content: String) {
        try {
            val file = java.io.File(context.cacheDir, fileName)
            file.writeText(content)
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share $fileName"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Export error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun exportProjectData(context: Context, projectId: Int) {
        viewModelScope.launch {
            val stations = dao.getStationsForProjectSync(projectId)
            val observations = dao.getObservationsForProjectSync(projectId)
            val csvBuilder = StringBuilder()
            csvBuilder.append("TYPE,STATION_CODE,LATITUDE,LONGITUDE,ELEVATION,NOTES\n")
            stations.forEach { s ->
                val obsList = observations.filter { it.stationId == s.id }
                if (obsList.isEmpty()) {
                    csvBuilder.append("STATION,${s.stationCode},${s.latitude},${s.longitude},${s.elevation},No observations\n")
                } else {
                    obsList.forEach { o ->
                        csvBuilder.append("STATION_OBS,${s.stationCode},${s.latitude},${s.longitude},${s.elevation},${o.observationType}: ${o.notes ?: ""}\n")
                    }
                }
            }
            shareTextFile(context, "project_${projectId}_excel_export.csv", csvBuilder.toString())
        }
    }

    fun exportPdfReport(context: Context, projectId: Int) {
        viewModelScope.launch {
            val project = dao.getProjectById(projectId).firstOrNull()
            val stations = dao.getStationsForProjectSync(projectId)
            val textReport = StringBuilder()
            textReport.append("=== GEOLOGICAL EXPLORATION FIELD REPORT ===\n")
            textReport.append("PROJECT: ${project?.name ?: "Unknown"}\n")
            textReport.append("DATE: ${project?.date ?: "Unknown"}\n")
            textReport.append("STATIONS CAPTURED: ${stations.size}\n\n")
            stations.forEach { s ->
                textReport.append("- Station: ${s.stationCode} (UTM Zone: ${s.utmZone}, Easting: ${s.utmEasting}, Northing: ${s.utmNorthing})\n")
                textReport.append("  Coordinates: Latitude ${s.latitude}, Longitude ${s.longitude}\n\n")
            }
            shareTextFile(context, "project_${projectId}_geology_pdf_report.txt", textReport.toString())
        }
    }

    fun exportCsv(context: Context, projectId: Int) {
        viewModelScope.launch {
            val stations = dao.getStationsForProjectSync(projectId)
            val csvBuilder = StringBuilder()
            csvBuilder.append("id,stationCode,prefix,latitude,longitude,elevation,accuracy,utmEasting,utmNorthing,utmZone\n")
            stations.forEach { s ->
                csvBuilder.append("${s.id},${s.stationCode},${s.prefix},${s.latitude},${s.longitude},${s.elevation},${s.accuracy},${s.utmEasting},${s.utmNorthing},${s.utmZone}\n")
            }
            shareTextFile(context, "project_${projectId}_stations.csv", csvBuilder.toString())
        }
    }

    fun exportProjectZip(context: Context, projectId: Int) {
        exportGeoJson(context, projectId)
    }

    fun exportGeoJson(context: Context, projectId: Int) {
        viewModelScope.launch {
            val project = dao.getProjectById(projectId).firstOrNull()
            val stations = dao.getStationsForProjectSync(projectId)
            val observations = dao.getObservationsForProjectSync(projectId)
            
            val geoJson = StringBuilder()
            geoJson.append("{\n")
            geoJson.append("  \"type\": \"FeatureCollection\",\n")
            geoJson.append("  \"name\": \"${project?.name ?: "Geological_Project"}\",\n")
            geoJson.append("  \"crs\": { \"type\": \"name\", \"properties\": { \"name\": \"urn:ogc:def:crs:OGC:1.3:CRS84\" } },\n")
            geoJson.append("  \"features\": [\n")
            
            val features = mutableListOf<String>()
            stations.forEach { s ->
                val obs = observations.filter { it.stationId == s.id }
                val lithology = obs.firstOrNull { !it.lithologyType.isNullOrEmpty() }?.lithologyType ?: "Unspecified"
                val strike = obs.firstOrNull { it.strike != null }?.strike
                val dip = obs.firstOrNull { it.dip != null }?.dip
                val dipDir = obs.firstOrNull { it.dipDirection != null }?.dipDirection
                
                val props = StringBuilder()
                props.append("      \"properties\": {\n")
                props.append("        \"STATION_CODE\": \"${s.stationCode}\",\n")
                props.append("        \"PREFIX\": \"${s.prefix ?: ""}\",\n")
                props.append("        \"ELEVATION\": ${s.elevation},\n")
                props.append("        \"UTM_EASTING\": ${s.utmEasting ?: 0.0},\n")
                props.append("        \"UTM_NORTHING\": ${s.utmNorthing ?: 0.0},\n")
                props.append("        \"UTM_ZONE\": \"${s.utmZone ?: ""}\",\n")
                props.append("        \"LITHOLOGY\": \"$lithology\",\n")
                props.append("        \"STRIKE\": ${strike ?: "null"},\n")
                props.append("        \"DIP\": ${dip ?: "null"},\n")
                props.append("        \"DIP_DIRECTION\": ${dipDir ?: "null"}\n")
                props.append("      }")
                
                val feat = "    {\n      \"type\": \"Feature\",\n      \"geometry\": {\n        \"type\": \"Point\",\n        \"coordinates\": [${s.longitude}, ${s.latitude}, ${s.elevation}]\n      },\n$props\n    }"
                features.add(feat)
            }
            geoJson.append(features.joinToString(",\n"))
            geoJson.append("\n  ]\n}")
            
            shareTextFile(context, "project_${projectId}_gis.geojson", geoJson.toString())
        }
    }

    fun exportKml(context: Context, projectId: Int) {
        viewModelScope.launch {
            val stations = dao.getStationsForProjectSync(projectId)
            val kml = StringBuilder()
            kml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            kml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n<Document>\n")
            stations.forEach { s ->
                kml.append("<Placemark>\n<name>${s.stationCode}</name>\n")
                kml.append("<description>Elevation: ${s.elevation}m</description>\n")
                kml.append("<Point>\n<coordinates>${s.longitude},${s.latitude},${s.elevation}</coordinates>\n</Point>\n</Placemark>\n")
            }
            kml.append("</Document>\n</kml>")
            shareTextFile(context, "project_${projectId}_map_points.kml", kml.toString())
        }
    }

    fun exportKmz(context: Context, projectId: Int) {
        exportKml(context, projectId)
    }

    fun exportProjectAgs(context: Context, projectId: Int) {
        viewModelScope.launch {
            val project = dao.getProjectById(projectId).firstOrNull()
            val stations = dao.getStationsForProjectSync(projectId)
            val boreholes = dao.getBoreholesForProjectSync(projectId)
            
            val ags = StringBuilder()
            ags.append("\"GROUP\",\"PROJ\"\n")
            ags.append("\"HEADING\",\"PROJ_ID\",\"PROJ_NAME\",\"PROJ_LOC\",\"PROJ_CLNT\"\n")
            ags.append("\"DATA\",\"P${projectId}\",\"${project?.name ?: "GeoField Project"}\",\"Field Location\",\"Geological Survey\"\n\n")
            
            ags.append("\"GROUP\",\"LOCA\"\n")
            ags.append("\"HEADING\",\"LOCA_ID\",\"LOCA_TYPE\",\"LOCA_NORT\",\"LOCA_EAST\",\"LOCA_GL\",\"LOCA_FDEP\"\n")
            stations.forEach { s ->
                ags.append("\"DATA\",\"${s.stationCode}\",\"EXPOSURE\",\"${s.utmNorthing ?: s.latitude}\",\"${s.utmEasting ?: s.longitude}\",\"${s.elevation}\",\"0.00\"\n")
            }
            boreholes.forEach { b ->
                ags.append("\"DATA\",\"${b.boreholeCode}\",\"BH\",\"${b.latitude}\",\"${b.longitude}\",\"${b.elevation}\",\"${b.plannedDepth}\"\n")
            }
            
            shareTextFile(context, "project_${projectId}_ags4.ags", ags.toString())
        }
    }

    fun exportProjectSurpacCsv(context: Context, projectId: Int) {
        viewModelScope.launch {
            val boreholes = dao.getBoreholesForProjectSync(projectId)
            val stations = dao.getStationsForProjectSync(projectId)
            val observations = dao.getObservationsForProjectSync(projectId)
            
            val csv = StringBuilder()
            csv.append("=== LEAPFROG / SURPAC / DATAMINE 3D MINING EXPORT ===\n\n")
            
            // COLLAR TABLE
            csv.append("--- COLLAR TABLE (collar.csv) ---\n")
            csv.append("HOLE_ID,X_UTM,Y_UTM,Z_ELEV,MAX_DEPTH,AZIMUTH,DIP\n")
            boreholes.forEach { b ->
                csv.append("${b.boreholeCode},${b.longitude},${b.latitude},${b.elevation},${b.plannedDepth},${b.azimuth},${b.dip}\n")
            }
            if (boreholes.isEmpty()) {
                stations.forEach { s ->
                    csv.append("${s.stationCode},${s.utmEasting ?: s.longitude},${s.utmNorthing ?: s.latitude},${s.elevation},0.0,0,90\n")
                }
            }
            
            // SURVEY TABLE
            csv.append("\n--- SURVEY TABLE (survey.csv) ---\n")
            csv.append("HOLE_ID,DEPTH,DIP,AZIMUTH\n")
            boreholes.forEach { b ->
                csv.append("${b.boreholeCode},0.0,${b.dip},${b.azimuth}\n")
                csv.append("${b.boreholeCode},${b.plannedDepth},${b.dip},${b.azimuth}\n")
            }
            
            // STRUCTURAL ORIENTATION TABLE
            csv.append("\n--- STRUCTURAL ORIENTATIONS (structural.csv) ---\n")
            csv.append("POINT_ID,X_EAST,Y_NORTH,Z_ELEV,STRIKE,DIP,DIP_DIRECTION,FEATURE_TYPE\n")
            stations.forEach { s ->
                val obsList = observations.filter { it.stationId == s.id }
                obsList.forEach { o ->
                    if (o.strike != null || o.dip != null) {
                        csv.append("${s.stationCode},${s.utmEasting ?: s.longitude},${s.utmNorthing ?: s.latitude},${s.elevation},${o.strike ?: 0},${o.dip ?: 0},${o.dipDirection ?: 0},${o.observationType}\n")
                    }
                }
            }
            
            shareTextFile(context, "project_${projectId}_leapfrog_surpac_3d.csv", csv.toString())
        }
    }

    fun exportBoreholesCsv(context: Context, projectId: Int) {
        viewModelScope.launch {
            val boreholes = dao.getBoreholesForProjectSync(projectId)
            val csvBuilder = StringBuilder()
            csvBuilder.append("ID,BoreholeCode,Latitude,Longitude,Elevation,Azimuth,Dip,PlannedDepth\n")
            boreholes.forEach { b ->
                csvBuilder.append("${b.id},${b.boreholeCode},${b.latitude},${b.longitude},${b.elevation},${b.azimuth},${b.dip},${b.plannedDepth}\n")
            }
            shareTextFile(context, "boreholes_project_${projectId}.csv", csvBuilder.toString())
        }
    }

    fun exportBoreholeDailyReportsCsv(context: Context, boreholeId: Int, boreholeCode: String) {
        viewModelScope.launch {
            val reports = dao.getDailyReportsForBoreholeSync(boreholeId)
            val csvBuilder = StringBuilder()
            csvBuilder.append("ID,ReportDate,Shift,ProgressMeters,StoppagesHours,Geologist,Problems\n")
            reports.forEach { r ->
                csvBuilder.append("${r.id},${r.reportDate},${r.shift},${r.progressMeters},${r.stoppagesHours},${r.shiftGeologist},${r.problemsRemarks}\n")
            }
            shareTextFile(context, "borehole_${boreholeCode}_daily_reports.csv", csvBuilder.toString())
        }
    }

    fun exportBoreholeLogsCsv(context: Context, boreholeId: Int, boreholeCode: String) {
        viewModelScope.launch {
            val runs = dao.getCoreRunsForBoreholeSync(boreholeId)
            val csvBuilder = StringBuilder()
            csvBuilder.append("ID,RunNumber,DepthFrom,DepthTo,RecoveryPercent,RqdPercent,Lithology,Mineralization,Alteration,Fractures,UCS_MPa\n")
            runs.forEach { c ->
                csvBuilder.append("${c.id},${c.runNumber},${c.depthFrom},${c.depthTo},${c.recoveryPercent},${c.rqdPercent},${c.lithology},${c.mineralization},${c.alteration},${c.fractureFrequency},${c.ucsMpa}\n")
            }
            shareTextFile(context, "borehole_${boreholeCode}_core_runs.csv", csvBuilder.toString())
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationTracking()
    }
}

// --- VIEWMODEL FACTORY PROVIDER ---
class GeologyViewModelFactory(
    private val application: Application,
    private val dao: GeologyDao,
    private val repository: GeologyRepository = (application as GeologyApplication).repository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GeologyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GeologyViewModel(application, dao, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class Exception")
    }
}

class GpsKalmanFilter {
    private var lastTime: Long = 0L
    private var lat: Double = 0.0
    private var lon: Double = 0.0
    private var variance: Double = -1.0 // negative means uninitialized

    fun process(newLat: Double, newLon: Double, accuracy: Double, timeMs: Long): Pair<Double, Double> {
        if (variance < 0.0) {
            // Initialize
            lastTime = timeMs
            lat = newLat
            lon = newLon
            variance = accuracy * accuracy
            return Pair(lat, lon)
        }

        val duration = timeMs - lastTime
        if (duration > 0) {
            // Process noise variance growth: 3.0 m/s^2 walking variance growth rate
            variance += duration * 0.003 * 0.003
            lastTime = timeMs
        }

        // Kalman gain
        val k = variance / (variance + accuracy * accuracy)

        // Update state
        lat += k * (newLat - lat)
        lon += k * (newLon - lon)

        // Update variance
        variance = (1.0 - k) * variance

        return Pair(lat, lon)
    }

    fun reset() {
        variance = -1.0
    }
}
