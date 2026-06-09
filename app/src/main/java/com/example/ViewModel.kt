package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.UtmConverter.convertLatLngToUtm
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Represents the screen backstack/routes for state-based navigation
sealed interface Screen {
    object ProjectList : Screen
    object CreateProject : Screen
    data class ProjectDashboard(val projectId: Int) : Screen
    data class CreateStation(val projectId: Int) : Screen
    data class StationDetail(val stationId: Int) : Screen
    data class AddObservation(val stationId: Int) : Screen
}

data class GpsData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val elevation: Double = 0.0,
    val accuracy: Double = 0.0,
    val elevationAccuracy: Double = 0.0,
    val utmEasting: Double = 0.0,
    val utmNorthing: Double = 0.0,
    val utmZone: String = "",
    val isCaptured: Boolean = false,
    val isLocating: Boolean = false,
    val source: String = "None", // GPS, Manual, Simulated
    val message: String = ""
)

class GeologyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        GeologyDatabase::class.java,
        "geology_field_logger.db"
    ).fallbackToDestructiveMigration().build()
    
    val dao = db.dao()

    private val prefs = application.getSharedPreferences("geofield_prefs", Context.MODE_PRIVATE)

    // --- REALTIME CONTINUOUS GPS & TRACKING ENGINE ---
    
    // Live location object
    private val _liveLocation = MutableStateFlow<android.location.Location?>(null)
    val liveLocation: StateFlow<android.location.Location?> = _liveLocation

    // Track recording indicators
    private val _isTrackRecordingEnabled = MutableStateFlow(false)
    val isTrackRecordingActive: StateFlow<Boolean> = _isTrackRecordingEnabled

    // Current active track points
    private val _activePoints = MutableStateFlow<List<TrackPoint>>(emptyList())
    val activePoints: StateFlow<List<TrackPoint>> = _activePoints

    val recordingStartTime = MutableStateFlow(0L)
    val recordingDurationSec = MutableStateFlow(0L)
    val tempTrackLength = MutableStateFlow(0.0)

    private var locationCallback: LocationCallback? = null
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(application) }

    init {
        // Recover draft/unsaved track path on start (crash protection!)
        recoverActiveTrackDraft()

        // Start a continuous 1-second ticker job to increment duration seconds if recording
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                if (_isTrackRecordingEnabled.value && recordingStartTime.value > 0) {
                    recordingDurationSec.value = (System.currentTimeMillis() - recordingStartTime.value) / 1000
                    saveActiveTrackDraft()
                }
            }
        }
    }

    fun startLocationTracking(context: Context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        // Stop any existing callback to avoid double listeners
        stopLocationTracking()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).apply {
            setMinUpdateIntervalMillis(1000L)
            setMinUpdateDistanceMeters(1.0f) // Only update when moved, saves battery and filters minor noise
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                
                // --- GPS Noise Smoothing/Filtering ---
                // Discard extremely poor accuracy positions (noise filter)
                if (location.accuracy > 150.0) return

                _liveLocation.value = location
                
                // Update standard _gpsState so other screens (CreateStation) see live details!
                val activeProj = activeProject.value
                val forcedZone = if (activeProj?.utmZoneMode == "MANUAL") activeProj.utmZoneManual else null
                val utm = convertLatLngToUtm(location.latitude, location.longitude, forcedZone)
                val elevAcc = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && location.hasVerticalAccuracy()) {
                    location.verticalAccuracyMeters.toDouble()
                } else {
                    location.accuracy.toDouble() * 1.5
                }

                _gpsState.update {
                    it.copy(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        elevation = location.altitude,
                        accuracy = location.accuracy.toDouble(),
                        elevationAccuracy = elevAcc,
                        utmEasting = utm.easting,
                        utmNorthing = utm.northing,
                        utmZone = utm.zoneString,
                        isCaptured = true,
                        isLocating = false,
                        source = "GPS (Continuous)"
                    )
                }

                // If currently recording track, add to our list
                if (_isTrackRecordingEnabled.value) {
                    val newPoint = TrackPoint(
                        lat = location.latitude,
                        lon = location.longitude,
                        ele = location.altitude,
                        time = System.currentTimeMillis(),
                        accuracy = location.accuracy.toDouble(),
                        heading = location.bearing.toDouble()
                    )
                    appendTrackPoint(newPoint)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                android.os.Looper.getMainLooper()
            )
        } catch (e: Exception) {
            // failed to request location
        }
    }

    fun stopLocationTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    fun startRecordingTrack() {
        _isTrackRecordingEnabled.value = true
        _activePoints.value = emptyList()
        recordingStartTime.value = System.currentTimeMillis()
        recordingDurationSec.value = 0L
        tempTrackLength.value = 0.0
        
        // Append initial point if location isn't null
        _liveLocation.value?.let { loc ->
            val initPoint = TrackPoint(
                lat = loc.latitude,
                lon = loc.longitude,
                ele = loc.altitude,
                time = System.currentTimeMillis(),
                accuracy = loc.accuracy.toDouble(),
                heading = loc.bearing.toDouble()
            )
            appendTrackPoint(initPoint)
        }
        saveActiveTrackDraft()
    }

    fun stopRecordingTrack(onFinished: (pointsJson: String, length: Double, duration: Long) -> Unit) {
        val points = _activePoints.value
        val duration = recordingDurationSec.value
        val length = tempTrackLength.value
        
        val pointsJson = points.joinToString(prefix = "[", postfix = "]") { pt ->
            "[${pt.lat},${pt.lon},${pt.ele},${pt.time},${pt.accuracy},${pt.heading}]"
        }

        _isTrackRecordingEnabled.value = false
        recordingStartTime.value = 0L
        recordingDurationSec.value = 0L
        
        onFinished(pointsJson, length, duration)
    }

    fun appendTrackPoint(pt: TrackPoint) {
        val currentList = _activePoints.value.toMutableList()
        if (currentList.isNotEmpty()) {
            val lastPt = currentList.last()
            
            // Haversine distance in meters to verify smoothing / distance traveled
            val dist = calculateDistanceMeters(lastPt.lat, lastPt.lon, pt.lat, pt.lon)
            
            // Filter noise: if point jumps by more than 200m in 1sec (physically impossible speed), discard as GPS telemetry jitter
            val secondsElapsed = (pt.time - lastPt.time) / 1000.0
            if (secondsElapsed > 0.1 && (dist / secondsElapsed) > 100.0) {
                return // Discard noise
            }

            // Cumulate distance
            tempTrackLength.value += dist
        }
        currentList.add(pt)
        _activePoints.value = currentList
        saveActiveTrackDraft()
    }

    fun addSimulatedPoint(pt: TrackPoint) {
        // Appends simulated point safely to tracking points list
        if (_isTrackRecordingEnabled.value) {
            appendTrackPoint(pt)
        }
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0)
        val c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a))
        return r * c
    }

    // Recover draft track paths from Prefs
    private fun saveActiveTrackDraft() {
        val points = _activePoints.value
        val pointsJson = points.joinToString(prefix = "[", postfix = "]") { pt ->
            "[${pt.lat},${pt.lon},${pt.ele},${pt.time},${pt.accuracy},${pt.heading}]"
        }
        prefs.edit()
            .putString("draft_track_points", pointsJson)
            .putBoolean("draft_track_recording", _isTrackRecordingEnabled.value)
            .putLong("draft_track_start_time", recordingStartTime.value)
            .putLong("draft_track_duration", recordingDurationSec.value)
            .putFloat("draft_track_length", tempTrackLength.value.toFloat())
            .apply()
    }

    fun recoverActiveTrackDraft() {
        val draftJson = prefs.getString("draft_track_points", null)
        val wasRecording = prefs.getBoolean("draft_track_recording", false)
        val startTime = prefs.getLong("draft_track_start_time", 0L)
        val duration = prefs.getLong("draft_track_duration", 0L)
        val length = prefs.getFloat("draft_track_length", 0.0f).toDouble()

        if (!draftJson.isNullOrEmpty() && draftJson != "[]") {
            val pts = parsePointsJson(draftJson)
            if (pts.isNotEmpty()) {
                _activePoints.value = pts
                _isTrackRecordingEnabled.value = wasRecording
                recordingStartTime.value = startTime
                recordingDurationSec.value = duration
                tempTrackLength.value = length
            }
        }
    }

    fun clearActiveTrackDraft() {
        prefs.edit()
            .remove("draft_track_points")
            .remove("draft_track_recording")
            .remove("draft_track_start_time")
            .remove("draft_track_duration")
            .remove("draft_track_length")
            .apply()
        _activePoints.value = emptyList()
        _isTrackRecordingEnabled.value = false
        recordingStartTime.value = 0L
        recordingDurationSec.value = 0L
        tempTrackLength.value = 0.0
    }

    // App Preferences (Settings state with SharedPreferences persistence)
    val themeSetting = MutableStateFlow(prefs.getString("theme", "Dark") ?: "Dark")
    val fontSizeSetting = MutableStateFlow(prefs.getString("font_size", "Standard") ?: "Standard")
    val coordSystemSetting = MutableStateFlow(prefs.getString("coord_system", "UTM") ?: "UTM")

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


    // Screen navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.ProjectList)
    val currentScreen: StateFlow<Screen> = _currentScreen

    // Navigation helper with built-in state synchronization
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        when (screen) {
            is Screen.ProjectDashboard -> {
                setActiveProject(screen.projectId)
            }
            is Screen.CreateStation -> {
                setActiveProject(screen.projectId)
            }
            is Screen.StationDetail -> {
                setActiveStation(screen.stationId)
                viewModelScope.launch {
                    val station = dao.getStationByIdOnce(screen.stationId)
                    if (station != null) {
                        setActiveProject(station.projectId)
                    }
                }
            }
            is Screen.AddObservation -> {
                setActiveStation(screen.stationId)
                viewModelScope.launch {
                    val station = dao.getStationByIdOnce(screen.stationId)
                    if (station != null) {
                        setActiveProject(station.projectId)
                    }
                }
            }
            else -> {}
        }
    }

    // List of all projects
    val projects: StateFlow<List<Project>> = dao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of all tracks
    val tracks: StateFlow<List<Track>> = dao.getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of all stations globally
    val allStations: StateFlow<List<Station>> = dao.getAllStations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of all observations globally
    val allObservations: StateFlow<List<Observation>> = dao.getAllObservations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of all samples globally
    val allSamples: StateFlow<List<Sample>> = dao.getAllSamples()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTrack(name: String, pointsJson: String, lengthMeters: Double, durationSeconds: Long) {
        viewModelScope.launch {
            val projId = activeProjectId.value ?: 0
            dao.insertTrack(
                Track(
                    projectId = projId,
                    name = name,
                    pointsJson = pointsJson,
                    lengthMeters = lengthMeters,
                    durationSeconds = durationSeconds
                )
            )
            clearActiveTrackDraft()
        }
    }

    fun updateTrackName(trackId: Int, newName: String) {
        viewModelScope.launch {
            dao.updateTrackName(trackId, newName)
        }
    }

    fun deleteTrack(trackId: Int) {
        viewModelScope.launch {
            dao.deleteTrackById(trackId)
        }
    }

    // Active screen selection IDs
    private val activeProjectId = MutableStateFlow<Int?>(null)
    private val activeStationId = MutableStateFlow<Int?>(null)

    // Active project state
    val activeProject: StateFlow<Project?> = activeProjectId.flatMapLatest { id ->
        if (id != null) dao.getProjectById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active project's stations
    val activeProjectStations: StateFlow<List<Station>> = activeProjectId.flatMapLatest { id ->
        if (id != null) dao.getStationsForProject(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active project's tracks
    val activeProjectTracks: StateFlow<List<Track>> = activeProjectId.flatMapLatest { id ->
        if (id != null) dao.getTracksForProject(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active station state
    val activeStation: StateFlow<Station?> = activeStationId.flatMapLatest { id ->
        if (id != null) dao.getStationById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active station's observations with nested samples
    val activeStationObservations: StateFlow<List<Observation>> = activeStationId.flatMapLatest { id ->
        if (id != null) dao.getObservationsForStation(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic samples for active station
    val activeStationSamples: StateFlow<List<Sample>> = activeStationId.flatMapLatest { id ->
        if (id != null) dao.getSamplesForStation(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // GPS Logging State (Used during Station creation)
    private val _gpsState = MutableStateFlow(GpsData())
    val gpsState: StateFlow<GpsData> = _gpsState

    fun setActiveProject(id: Int) {
        activeProjectId.value = id
    }

    fun setActiveStation(id: Int) {
        activeStationId.value = id
    }

    // --- WRITE OPERATIONS ---

    fun createProject(name: String, description: String, dateStr: String = "", utmZoneMode: String = "AUTO", utmZoneManual: String = "39N") {
        viewModelScope.launch {
            val finalDate = if (dateStr.isBlank()) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            } else {
                dateStr
            }
            val newProjId = dao.insertProject(
                Project(
                    name = name,
                    description = description.ifBlank { null },
                    date = finalDate,
                    utmZoneMode = utmZoneMode,
                    utmZoneManual = utmZoneManual
                )
            )
            setActiveProject(newProjId.toInt())
            navigateTo(Screen.ProjectDashboard(newProjId.toInt()))
        }
    }

    fun createProjectSilently(name: String, description: String, dateStr: String = "", utmZoneMode: String = "AUTO", utmZoneManual: String = "39N", onCompleted: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val finalDate = if (dateStr.isBlank()) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            } else {
                dateStr
            }
            val newProjId = dao.insertProject(
                Project(
                    name = name,
                    description = description.ifBlank { null },
                    date = finalDate,
                    utmZoneMode = utmZoneMode,
                    utmZoneManual = utmZoneManual
                )
            )
            setActiveProject(newProjId.toInt())
            onCompleted(newProjId.toInt())
        }
    }

    fun updateProjectSettings(projectId: Int, mode: String, manualZone: String) {
        viewModelScope.launch {
            val active = activeProject.value
            if (active != null && active.id == projectId) {
                val updated = active.copy(utmZoneMode = mode, utmZoneManual = manualZone)
                dao.insertProject(updated)
            }
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            dao.insertProject(project)
        }
    }

    fun deleteProject(projectId: Int) {
        viewModelScope.launch {
            dao.deleteProjectById(projectId)
            if (activeProjectId.value == projectId) {
                activeProjectId.value = null
            }
            navigateTo(Screen.ProjectList)
        }
    }

    // Auto generate sequential station codes (e.g. LITH-001)
    fun getNextStationCodeForPrefix(projectId: Int, prefix: String, onCodeReady: (String) -> Unit) {
        viewModelScope.launch {
            val count = dao.getStationCountForPrefix(projectId, prefix)
            val nextIndex = count + 1
            val proposedCode = String.format(Locale.US, "%s-%03d", prefix, nextIndex)
            onCodeReady(proposedCode)
        }
    }

    // Reset GPS logging state
    fun resetGpsState() {
        _gpsState.value = GpsData()
    }

    // GPS location capturing logic with simulation fallback
    fun captureLocation(context: Context, mode: String = "GPS") {
        if (mode == "SIMULATE") {
            // Generate simulated geology field location coordinates
            // (e.g., Mount Isa copper-gold district or typical outback outcrops)
            val baseLat = -20.723
            val baseLog = 139.493
            val randomOffsetLat = (Math.random() - 0.5) * 0.05
            val randomOffsetLon = (Math.random() - 0.5) * 0.05
            val simLat = baseLat + randomOffsetLat
            val simLon = baseLog + randomOffsetLon
            val simElev = 350.0 + (Math.random() * 50)
            val simAcc = 2.0 + (Math.random() * 3)
            val simElevAcc = 1.5 + (Math.random() * 2)
            
            val activeProj = activeProject.value
            val forcedZone = if (activeProj?.utmZoneMode == "MANUAL") activeProj.utmZoneManual else null
            val utm = convertLatLngToUtm(simLat, simLon, forcedZone)

            _gpsState.update {
                it.copy(
                    latitude = simLat,
                    longitude = simLon,
                    elevation = simElev,
                    accuracy = simAcc,
                    elevationAccuracy = simElevAcc,
                    utmEasting = utm.easting,
                    utmNorthing = utm.northing,
                    utmZone = utm.zoneString,
                    isCaptured = true,
                    isLocating = false,
                    source = "Simulated"
                )
            }
            return
        }

        // Real Location Request
        _gpsState.update { it.copy(isLocating = true) }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _gpsState.update {
                it.copy(
                    isLocating = false,
                    message = "GPS Permissions not granted. Please enable in settings or choose 'Simulate' below."
                )
            }
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cts.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                val activeProj = activeProject.value
                val forcedZone = if (activeProj?.utmZoneMode == "MANUAL") activeProj.utmZoneManual else null
                val utm = convertLatLngToUtm(location.latitude, location.longitude, forcedZone)
                val elevAcc = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && location.hasVerticalAccuracy()) {
                    location.verticalAccuracyMeters.toDouble()
                } else {
                    location.accuracy.toDouble() * 1.5
                }
                _gpsState.update {
                    it.copy(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        elevation = location.altitude,
                        accuracy = location.accuracy.toDouble(),
                        elevationAccuracy = elevAcc,
                        utmEasting = utm.easting,
                        utmNorthing = utm.northing,
                        utmZone = utm.zoneString,
                        isCaptured = true,
                        isLocating = false,
                        source = "GPS (Fused Satellite)"
                    )
                }
            } else {
                // Fallback to Native Hardware LocationManager for direct satellite tracking (essential for desert/underground drilling sites)
                try {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                    var bestLocation: android.location.Location? = null
                    
                    if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                        bestLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    }
                    if (bestLocation == null && locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                        val netLoc = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                        if (netLoc != null) bestLocation = netLoc
                    }
                    if (bestLocation == null && locationManager.isProviderEnabled(android.location.LocationManager.PASSIVE_PROVIDER)) {
                        val passiveLoc = locationManager.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER)
                        if (passiveLoc != null) bestLocation = passiveLoc
                    }

                    if (bestLocation != null) {
                        val activeProj = activeProject.value
                        val forcedZone = if (activeProj?.utmZoneMode == "MANUAL") activeProj.utmZoneManual else null
                        val utm = convertLatLngToUtm(bestLocation.latitude, bestLocation.longitude, forcedZone)
                        val elevAcc = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && bestLocation.hasVerticalAccuracy()) {
                            bestLocation.verticalAccuracyMeters.toDouble()
                        } else {
                            bestLocation.accuracy.toDouble() * 1.5
                        }
                        _gpsState.update {
                            it.copy(
                                latitude = bestLocation.latitude,
                                longitude = bestLocation.longitude,
                                elevation = bestLocation.altitude,
                                accuracy = bestLocation.accuracy.toDouble(),
                                elevationAccuracy = elevAcc,
                                utmEasting = utm.easting,
                                utmNorthing = utm.northing,
                                utmZone = utm.zoneString,
                                isCaptured = true,
                                isLocating = false,
                                source = "GPS (Native Hardware)"
                            )
                        }
                        Toast.makeText(context, "Hardware satellite lock active!", Toast.LENGTH_SHORT).show()
                    } else {
                        _gpsState.update {
                            it.copy(
                                isLocating = false,
                                source = "None",
                                message = "GPS signal weak/offline. Using Simulation is recommended!"
                            )
                        }
                        Toast.makeText(context, "GPS Signal offline. Try simulated mode!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    _gpsState.update {
                        it.copy(
                            isLocating = false,
                            source = "None",
                            message = "GPS Signal acquisition failed: ${e.localizedMessage}"
                        )
                    }
                }
            }
        }.addOnFailureListener { exception ->
            // Try Native LocationManager fallback directly as well
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                var bestLocation: android.location.Location? = null
                
                if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    bestLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                }
                if (bestLocation != null) {
                    val activeProj = activeProject.value
                    val forcedZone = if (activeProj?.utmZoneMode == "MANUAL") activeProj.utmZoneManual else null
                    val utm = convertLatLngToUtm(bestLocation.latitude, bestLocation.longitude, forcedZone)
                    _gpsState.update {
                        it.copy(
                            latitude = bestLocation.latitude,
                            longitude = bestLocation.longitude,
                            elevation = bestLocation.altitude,
                            accuracy = bestLocation.accuracy.toDouble(),
                            utmEasting = utm.easting,
                            utmNorthing = utm.northing,
                            utmZone = utm.zoneString,
                            isCaptured = true,
                            isLocating = false,
                            source = "GPS (Hardware Fallback)"
                        )
                    }
                } else {
                    _gpsState.update {
                        it.copy(
                            isLocating = false,
                            message = "GPS Capture Failed: ${exception.localizedMessage}"
                        )
                    }
                }
            } catch (e: Exception) {
                _gpsState.update {
                    it.copy(
                        isLocating = false,
                        message = "GPS Capture Failed: ${exception.localizedMessage}"
                    )
                }
            }
        }
    }

    // Set coordinates manually
    fun setManualCoordinates(lat: Double, lng: Double, elev: Double, acc: Double) {
        val activeProj = activeProject.value
        val forcedZone = if (activeProj?.utmZoneMode == "MANUAL") activeProj.utmZoneManual else null
        val utm = convertLatLngToUtm(lat, lng, forcedZone)
        _gpsState.value = GpsData(
            latitude = lat,
            longitude = lng,
            elevation = elev,
            accuracy = acc,
            utmEasting = utm.easting,
            utmNorthing = utm.northing,
            utmZone = utm.zoneString,
            isCaptured = true,
            isLocating = false,
            source = "Manual"
        )
    }

    // Save Station to Active Project
    fun saveStation(
        projectId: Int,
        stationCode: String,
        prefix: String,
        onSaved: (Int) -> Unit
    ) {
        val gps = _gpsState.value
        if (!gps.isCaptured) return

        viewModelScope.launch {
            val stationId = dao.insertStation(
                Station(
                    projectId = projectId,
                    stationCode = stationCode,
                    prefix = prefix,
                    latitude = gps.latitude,
                    longitude = gps.longitude,
                    elevation = gps.elevation,
                    accuracy = gps.accuracy,
                    elevationAccuracy = gps.elevationAccuracy,
                    utmEasting = gps.utmEasting,
                    utmNorthing = gps.utmNorthing,
                    utmZone = gps.utmZone
                )
            )
            setActiveStation(stationId.toInt())
            onSaved(stationId.toInt())
        }
    }

    // Save Station along with initial Observation and linked Sample
    fun saveStationWithObservation(
        projectId: Int,
        stationCode: String,
        prefix: String,
        obsType: String,
        notes: String,
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
        
        onSaved: (Int) -> Unit
    ) {
        val gps = _gpsState.value
        if (!gps.isCaptured) return

        viewModelScope.launch {
            val stationId = dao.insertStation(
                Station(
                    projectId = projectId,
                    stationCode = stationCode,
                    prefix = prefix,
                    latitude = gps.latitude,
                    longitude = gps.longitude,
                    elevation = gps.elevation,
                    accuracy = gps.accuracy,
                    elevationAccuracy = gps.elevationAccuracy,
                    utmEasting = gps.utmEasting,
                    utmNorthing = gps.utmNorthing,
                    utmZone = gps.utmZone
                )
            )

            val obsId = dao.insertObservation(
                Observation(
                    stationId = stationId.toInt(),
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
            )

            if (sampleTaken && !sampleIdCode.isNullOrBlank()) {
                dao.insertSample(
                    Sample(
                        observationId = obsId.toInt(),
                        sampleCodeId = sampleIdCode,
                        sampleType = sampleType ?: "Hand specimen",
                        description = sampleDesc,
                        photoPath = samplePhotoPath ?: photoPath
                    )
                )
            }

            setActiveStation(stationId.toInt())
            onSaved(stationId.toInt())
        }
    }

    fun deleteStation(stationId: Int, projectId: Int) {
        viewModelScope.launch {
            dao.deleteStationById(stationId)
            if (activeStationId.value == stationId) {
                activeStationId.value = null
            }
            navigateTo(Screen.ProjectDashboard(projectId))
        }
    }

    // Add general observation and link Sample if taken
    fun saveObservation(
        stationId: Int,
        obsType: String,
        notes: String,
        photoPath: String?,
        
        // Lithology Specifics
        lithType: String? = null,
        texture: String? = null,
        color: String? = null,
        grainSize: String? = null,
        lithAlteration: String? = null,
        lithMineralization: String? = null,

        // Alteration Specifics
        altType: String? = null,
        altIntensity: String? = null,
        hostRock: String? = null,
        altAssociated: String? = null,

        // Fault Specifics
        faultType: String? = null,
        strike: Int? = null,
        dip: Int? = null,
        dipDirection: Int? = null,
        rake: Int? = null,
        rakeDirection: String? = null,
        movement: String? = null,
        width: Double? = null,

        // Joint Specifics
        jointType: String? = null,
        jointSpacing: Double? = null,

        // Vein Specifics
        veinType: String? = null,
        veinMineralogy: String? = null,

        // Structure Specifics
        structureType: String? = null,

        // Mineralization Specifics
        minStyle: String? = null,
        minAssociatedMinerals: String? = null,
        minGradeEstimate: String? = null,

        // Sample Specifics (linked via inner Sample Taking flow)
        sampleTaken: Boolean = false,
        sampleIdCode: String? = null,
        sampleType: String? = null,
        sampleDesc: String? = null,
        samplePhotoPath: String? = null,
        
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val obsId = dao.insertObservation(
                Observation(
                    stationId = stationId,
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
            )

            if (sampleTaken && !sampleIdCode.isNullOrBlank()) {
                dao.insertSample(
                    Sample(
                        observationId = obsId.toInt(),
                        sampleCodeId = sampleIdCode,
                        sampleType = sampleType ?: "Hand specimen",
                        description = sampleDesc,
                        photoPath = samplePhotoPath ?: photoPath
                    )
                )
            }
            onComplete()
        }
    }

    fun deleteObservation(obsId: Int) {
        viewModelScope.launch {
            dao.deleteObservationById(obsId)
        }
    }

    // --- MULTI-SHEET EXPORT SYSTEM ---

    fun exportProjectData(context: Context, projectId: Int) {
        viewModelScope.launch {
            try {
                // Perform database queries and formatting on Dispatchers.IO background thread
                val shareData = withContext(Dispatchers.IO) {
                    val stations = dao.getStationsForProjectSync(projectId)
                    val observations = dao.getObservationsForProjectSync(projectId)
                    val samples = dao.getSamplesForProjectSync(projectId)

                    val stationCodeMap = stations.associate { it.id to it.stationCode }
                    val obsCodeMap = observations.associate { it.id to (stationCodeMap[it.stationId] ?: "Unknown") }
                    val obsTypeMap = observations.associate { it.id to it.observationType }

                    // Map database entries to XlsxWriter format
                    val stationRows = stations.map {
                        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.createdAt))
                        XlsxWriter.StationSheetData(
                            stationCode = it.stationCode,
                            prefix = it.prefix,
                            latitude = it.latitude,
                            longitude = it.longitude,
                            elevation = it.elevation,
                            accuracy = it.accuracy,
                            utmEasting = it.utmEasting,
                            utmNorthing = it.utmNorthing,
                            utmZone = it.utmZone,
                            createdAt = dateFormatted
                        )
                    }

                    val lithologyRows = observations.filter { it.observationType == "Lithology" }.map {
                        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.createdAt))
                        val hasSampleSync = dao.getSampleForObservationSync(it.id) != null
                        XlsxWriter.LithologySheetData(
                            stationCode = stationCodeMap[it.stationId] ?: "Unknown",
                            timestamp = dateFormatted,
                            lithologyType = it.lithologyType ?: "",
                            texture = it.texture ?: "",
                            color = it.color ?: "",
                            grainSize = it.grainSize ?: "",
                            alteration = it.lithologyAlteration ?: "",
                            mineralization = it.lithologyMineralization ?: "",
                            notes = it.notes ?: "",
                            hasSample = if (hasSampleSync) "Yes" else "No",
                            photos = it.photoPath ?: ""
                        )
                    }

                    val alterationRows = observations.filter { it.observationType == "Alteration" }.map {
                        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.createdAt))
                        val hasSampleSync = dao.getSampleForObservationSync(it.id) != null
                        XlsxWriter.AlterationSheetData(
                            stationCode = stationCodeMap[it.stationId] ?: "Unknown",
                            timestamp = dateFormatted,
                            alterationType = it.alterationType ?: "",
                            intensity = it.intensity ?: "",
                            hostRock = it.hostRock ?: "",
                            associatedMinerals = it.associatedMinerals ?: "",
                            notes = it.notes ?: "",
                            hasSample = if (hasSampleSync) "Yes" else "No",
                            photos = it.photoPath ?: ""
                        )
                    }

                    val faultRows = observations.filter { it.observationType == "Fault" || it.observationType == "Joint" || it.observationType == "Vein" || it.observationType == "Structure" }.map {
                        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.createdAt))
                        val hasSampleSync = dao.getSampleForObservationSync(it.id) != null
                        XlsxWriter.FaultSheetData(
                            stationCode = stationCodeMap[it.stationId] ?: "Unknown",
                            timestamp = dateFormatted,
                            structureType = it.observationType,
                            strike = it.strike,
                            dip = it.dip,
                            dipDirection = it.dipDirection,
                            rake = it.rake,
                            rakeDirection = it.rakeDirection,
                            movement = it.movement ?: it.veinMineralogy ?: "",
                            width = it.width,
                            notes = it.notes ?: "",
                            hasSample = if (hasSampleSync) "Yes" else "No",
                            photos = it.photoPath ?: ""
                        )
                    }

                    val sampleRows = samples.map {
                        XlsxWriter.SampleSheetData(
                            stationCode = obsCodeMap[it.observationId] ?: "Unknown",
                            observationType = obsTypeMap[it.observationId] ?: "General",
                            sampleId = it.sampleCodeId,
                            sampleType = it.sampleType,
                            description = it.description ?: ""
                        )
                    }

                    // File Creation
                    val fileName = "Geology_Logs_Project_${projectId}.xlsx"
                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    val file = File(dir, fileName)
                    
                    XlsxWriter.generateXlsxFile(
                        file,
                        stations = stationRows,
                        lithology = lithologyRows,
                        alteration = alterationRows,
                        faults = faultRows,
                        samples = sampleRows
                    )

                    Pair(file, fileName)
                }

                val (file, fileName) = shareData

                // Share file offline via custom Intent FileProvider (must run on Main Thread)
                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Geological Field Log Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = Intent.createChooser(intent, "Share Geology Excel Log")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                
                Toast.makeText(context, "Excel Log Generated: Downloads/${fileName}", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    // --- PREMIUM GIS ZIP EXPORT SYSTEM ---
    fun exportProjectZip(context: Context, projectId: Int) {
        viewModelScope.launch {
            try {
                // Perform heavy query, creation, and compression tasks inside Dispatchers.IO background thread
                val shareData = withContext(Dispatchers.IO) {
                    // Fetch all data synchronously
                    val project = dao.getProjectById(projectId).firstOrNull() ?: Project(id = projectId, name = "Geology Project", description = "Field survey data package", date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                    val stations = dao.getStationsForProjectSync(projectId)
                    val observations = dao.getObservationsForProjectSync(projectId)
                    val samples = dao.getSamplesForProjectSync(projectId)

                    val stationCodeMap = stations.associate { it.id to it.stationCode }
                    val obsCodeMap = observations.associate { it.id to (stationCodeMap[it.stationId] ?: "Unknown") }
                    val obsTypeMap = observations.associate { it.id to it.observationType }

                    // 1. Prepare standard spreadsheet file inside ZIP with clean relative photo references
                    val tempExcelFile = File.createTempFile("temp_export", ".xlsx", context.cacheDir)
                    
                    val excelStations = stations.map {
                        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.createdAt))
                        XlsxWriter.StationSheetData(
                            stationCode = it.stationCode,
                            prefix = it.prefix,
                            latitude = it.latitude,
                            longitude = it.longitude,
                            elevation = it.elevation,
                            accuracy = it.accuracy,
                            utmEasting = it.utmEasting,
                            utmNorthing = it.utmNorthing,
                            utmZone = it.utmZone,
                            createdAt = dateFormatted
                        )
                    }

                    val excelLithology = observations.filter { it.observationType == "Lithology" }.map {
                        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.createdAt))
                        val hasSampleSync = dao.getSampleForObservationSync(it.id) != null
                        val relativePhotos = it.photoPath?.split(",")?.filter { it.isNotBlank() }?.map { File(it).name }?.joinToString(", ") ?: ""
                        XlsxWriter.LithologySheetData(
                            stationCode = stationCodeMap[it.stationId] ?: "Unknown",
                            timestamp = dateFormatted,
                            lithologyType = it.lithologyType ?: "",
                            texture = it.texture ?: "",
                            color = it.color ?: "",
                            grainSize = it.grainSize ?: "",
                            alteration = it.lithologyAlteration ?: "",
                            mineralization = it.lithologyMineralization ?: "",
                            notes = it.notes ?: "",
                            hasSample = if (hasSampleSync) "Yes" else "No",
                            photos = relativePhotos
                        )
                    }

                    val excelAlteration = observations.filter { it.observationType == "Alteration" }.map {
                        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.createdAt))
                        val hasSampleSync = dao.getSampleForObservationSync(it.id) != null
                        val relativePhotos = it.photoPath?.split(",")?.filter { it.isNotBlank() }?.map { File(it).name }?.joinToString(", ") ?: ""
                        XlsxWriter.AlterationSheetData(
                            stationCode = stationCodeMap[it.stationId] ?: "Unknown",
                            timestamp = dateFormatted,
                            alterationType = it.alterationType ?: "",
                            intensity = it.intensity ?: "",
                            hostRock = it.hostRock ?: "",
                            associatedMinerals = it.associatedMinerals ?: "",
                            notes = it.notes ?: "",
                            hasSample = if (hasSampleSync) "Yes" else "No",
                            photos = relativePhotos
                        )
                    }

                    val excelFaults = observations.filter { it.observationType == "Fault" || it.observationType == "Joint" || it.observationType == "Vein" || it.observationType == "Structure" }.map {
                        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.createdAt))
                        val hasSampleSync = dao.getSampleForObservationSync(it.id) != null
                        val relativePhotos = it.photoPath?.split(",")?.filter { it.isNotBlank() }?.map { File(it).name }?.joinToString(", ") ?: ""
                        XlsxWriter.FaultSheetData(
                            stationCode = stationCodeMap[it.stationId] ?: "Unknown",
                            timestamp = dateFormatted,
                            structureType = it.observationType,
                            strike = it.strike,
                            dip = it.dip,
                            dipDirection = it.dipDirection,
                            rake = it.rake,
                            rakeDirection = it.rakeDirection,
                            movement = it.movement ?: it.veinMineralogy ?: "",
                            width = it.width,
                            notes = it.notes ?: "",
                            hasSample = if (hasSampleSync) "Yes" else "No",
                            photos = relativePhotos
                        )
                    }

                    val excelSamples = samples.map {
                        XlsxWriter.SampleSheetData(
                            stationCode = obsCodeMap[it.observationId] ?: "Unknown",
                            observationType = obsTypeMap[it.observationId] ?: "General",
                            sampleId = it.sampleCodeId,
                            sampleType = it.sampleType,
                            description = it.description ?: ""
                        )
                    }

                    XlsxWriter.generateXlsxFile(
                        tempExcelFile,
                        stations = excelStations,
                        lithology = excelLithology,
                        alteration = excelAlteration,
                        faults = excelFaults,
                        samples = excelSamples
                    )

                    // 2. Prepare final ZIP File
                    val zipDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                    if (!zipDir.exists()) {
                        zipDir.mkdirs()
                    }
                    val zipFileName = "GeoField_Project_${projectId}_GIS_Package.zip"
                    val zipFile = File(zipDir, zipFileName)

                    java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
                        // Add workbook.xlsx
                        zos.putNextEntry(java.util.zip.ZipEntry("Geology_Logs_Workbook.xlsx"))
                        tempExcelFile.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()

                        // Add GIS Vector: KML Map
                        try {
                            val obsByStation = observations.groupBy { it.stationId }
                            val samplesByObs = samples.groupBy { it.observationId }
                            val kmlContent = buildKmlContent(project.name, stations, obsByStation, samplesByObs, isKmzMode = false)
                            zos.putNextEntry(java.util.zip.ZipEntry("gis_vectors/field_map_placemarks.kml"))
                            zos.write(kmlContent.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
                            zos.closeEntry()
                        } catch (e: Exception) {
                            // fallback on individual write failures
                        }

                        // Add GIS Vector: GeoJSON locations Map
                        try {
                            val geoJsonSb = StringBuilder()
                            geoJsonSb.append("{\n  \"type\": \"FeatureCollection\",\n  \"features\": [\n")
                            stations.forEachIndexed { index, st ->
                                geoJsonSb.append("    {\n")
                                geoJsonSb.append("      \"type\": \"Feature\",\n")
                                geoJsonSb.append("      \"geometry\": {\n")
                                geoJsonSb.append("        \"type\": \"Point\",\n")
                                geoJsonSb.append("        \"coordinates\": [${st.longitude}, ${st.latitude}]\n")
                                geoJsonSb.append("      },\n")
                                geoJsonSb.append("      \"properties\": {\n")
                                geoJsonSb.append("        \"stationCode\": \"${st.stationCode}\",\n")
                                geoJsonSb.append("        \"prefix\": \"${st.prefix}\",\n")
                                geoJsonSb.append("        \"elevation\": ${st.elevation},\n")
                                geoJsonSb.append("        \"utmZone\": \"${st.utmZone}\",\n")
                                geoJsonSb.append("        \"utmEasting\": ${st.utmEasting},\n")
                                geoJsonSb.append("        \"utmNorthing\": ${st.utmNorthing}\n")
                                geoJsonSb.append("      }\n")
                                geoJsonSb.append("    }")
                                if (index < stations.size - 1) geoJsonSb.append(",")
                                geoJsonSb.append("\n")
                            }
                            geoJsonSb.append("  ]\n}")
                            zos.putNextEntry(java.util.zip.ZipEntry("gis_vectors/stations_locations.geojson"))
                            zos.write(geoJsonSb.toString().toByteArray(java.nio.charset.StandardCharsets.UTF_8))
                            zos.closeEntry()
                        } catch (e: Exception) {
                            // fallback
                        }

                        // Add Rich PDF Field Report inside the ZIP
                        try {
                            val pdfDoc = generatePdfDocumentSync(project, stations, observations, samples)
                            val tempPdfFile = File.createTempFile("temp_zip_pdf", ".pdf", context.cacheDir)
                            tempPdfFile.outputStream().use { pdfDoc.writeTo(it) }
                            pdfDoc.close()
                            
                            zos.putNextEntry(java.util.zip.ZipEntry("reports/Visual_Field_Survey_Report.pdf"))
                            tempPdfFile.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            tempPdfFile.delete()
                        } catch (e: Exception) {
                            // fallback
                        }

                        // Add CSV: stations
                        val stationCsv = toCsvString(
                            listOf("Station Code", "Prefix", "Latitude", "Longitude", "Elevation (m)", "Accuracy (m)", "UTM Easting", "UTM Northing", "UTM Zone", "Created At"),
                            excelStations.map { listOf(it.stationCode, it.prefix, it.latitude, it.longitude, it.elevation, it.accuracy, it.utmEasting, it.utmNorthing, it.utmZone, it.createdAt) }
                        )
                        zos.putNextEntry(java.util.zip.ZipEntry("data/stations_dataset.csv"))
                        zos.write(stationCsv.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
                        zos.closeEntry()

                        // Add CSV: lithology
                        val lithCsv = toCsvString(
                            listOf("Station Code", "Timestamp", "Lithology Type", "Texture", "Color", "Grain Size", "Alteration", "Mineralization", "Notes", "Has Sample?", "Photos"),
                            excelLithology.map { listOf(it.stationCode, it.timestamp, it.lithologyType, it.texture, it.color, it.grainSize, it.alteration, it.mineralization, it.notes, it.hasSample, it.photos) }
                        )
                        zos.putNextEntry(java.util.zip.ZipEntry("data/lithologies_dataset.csv"))
                        zos.write(lithCsv.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
                        zos.closeEntry()

                        // Add CSV: alteration
                        val altCsv = toCsvString(
                            listOf("Station Code", "Timestamp", "Alteration Type", "Intensity", "Host Rock", "Associated Minerals", "Notes", "Has Sample?", "Photos"),
                            excelAlteration.map { listOf(it.stationCode, it.timestamp, it.alterationType, it.intensity, it.hostRock, it.associatedMinerals, it.notes, it.hasSample, it.photos) }
                        )
                        zos.putNextEntry(java.util.zip.ZipEntry("data/alterations_dataset.csv"))
                        zos.write(altCsv.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
                        zos.closeEntry()

                        // Add CSV: structures/faults
                        val faultCsv = toCsvString(
                            listOf("Station Code", "Timestamp", "Structure Type", "Strike (deg)", "Dip (deg)", "Dip Direction (deg)", "Rake (deg)", "Rake Direction", "Movement", "Width (cm)", "Notes", "Has Sample?", "Photos"),
                            excelFaults.map { listOf(it.stationCode, it.timestamp, it.structureType, it.strike ?: "", it.dip ?: "", it.dipDirection ?: "", it.rake ?: "", it.rakeDirection ?: "", it.movement ?: "", it.width ?: "", it.notes ?: "", it.hasSample, it.photos) }
                        )
                        zos.putNextEntry(java.util.zip.ZipEntry("data/structures_dataset.csv"))
                        zos.write(faultCsv.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
                        zos.closeEntry()

                        // Add CSV: samples
                        val sampleCsv = toCsvString(
                            listOf("Station Code", "Observation Type", "Sample ID", "Sample Type", "Description"),
                            excelSamples.map { listOf(it.stationCode, it.observationType, it.sampleId, it.sampleType, it.description) }
                        )
                        zos.putNextEntry(java.util.zip.ZipEntry("data/samples_dataset.csv"))
                        zos.write(sampleCsv.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
                        zos.closeEntry()

                        // Copy photo files into the ZIP archive under field_photos/
                        val allLocalPhotoUrls = observations.mapNotNull { it.photoPath }
                            .flatMap { it.split(",") }
                            .filter { it.isNotBlank() }
                            .distinct()

                        allLocalPhotoUrls.forEach { photoPathUrl ->
                            val localPhotoFile = File(photoPathUrl)
                            if (localPhotoFile.exists() && localPhotoFile.isFile) {
                                zos.putNextEntry(java.util.zip.ZipEntry("field_photos/${localPhotoFile.name}"))
                                localPhotoFile.inputStream().use { it.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                    }

                    // Delete temporary Excel file
                    tempExcelFile.delete()

                    Pair(zipFile, zipFileName)
                }

                val (zipFile, zipFileName) = shareData

                // Share ZIP file offline via custom Intent FileProvider (must run on Main Thread)
                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, zipFile)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "GeoField GIS Package Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Share GIS ZIP Package")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "ZIP GIS Package Saved: Downloads/${zipFileName}", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    // Direct KML/KMZ and multi-format exporters
    private fun buildKmlContent(
        projectName: String,
        stations: List<Station>,
        obsByStation: Map<Int, List<Observation>>,
        samplesByObs: Map<Int, List<Sample>>,
        isKmzMode: Boolean
    ): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
        sb.append("  <Document>\n")
        sb.append("    <name>${projectName}</name>\n")
        sb.append("    <description>GeoField Professional GIS KML Export</description>\n")
        
        // Color mapping in KML using AABBGGRR (Alpha, Blue, Green, Red) format
        val colors = mapOf(
            "LITH" to "ff22ff22", // Pale green
            "ALT" to "ff00a5ff",  // Orange
            "FLT" to "ff0000ff",  // Red
            "STR" to "ffffcc00",  // Blue/Cyan
            "SMP" to "ffcc00cc"   // Purple
        )
        
        colors.forEach { (prefix, col) ->
            sb.append("    <Style id=\"style_$prefix\">\n")
            sb.append("      <IconStyle>\n")
            sb.append("        <color>$col</color>\n")
            sb.append("        <scale>1.2</scale>\n")
            sb.append("        <Icon>\n")
            sb.append("          <href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n")
            sb.append("        </Icon>\n")
            sb.append("      </IconStyle>\n")
            sb.append("    </Style>\n")
        }
        
        stations.forEach { st ->
            sb.append("    <Placemark>\n")
            sb.append("      <name>${st.stationCode}</name>\n")
            val styleKey = if (colors.containsKey(st.prefix)) st.prefix else "LITH"
            sb.append("      <styleUrl>#style_$styleKey</styleUrl>\n")
            
            val html = StringBuilder()
            html.append("<h3>Geological Station: ${st.stationCode}</h3>")
            html.append("<table border=\"1\" padding=\"4\" style=\"border-collapse: collapse; font-family: monospace;\">")
            html.append("<tr><td><b>Latitude</b></td><td>${st.latitude}</td></tr>")
            html.append("<tr><td><b>Longitude</b></td><td>${st.longitude}</td></tr>")
            html.append("<tr><td><b>Elevation</b></td><td>${st.elevation} m</td></tr>")
            html.append("<tr><td><b>UTM Zone</b></td><td>${st.utmZone}</td></tr>")
            html.append("<tr><td><b>Easting</b></td><td>${st.utmEasting}</td></tr>")
            html.append("<tr><td><b>Northing</b></td><td>${st.utmNorthing}</td></tr>")
            html.append("<tr><td><b>Accuracy</b></td><td>±${st.accuracy} m</td></tr>")
            html.append("</table><br/>")
            
            val stationObsList = obsByStation[st.id] ?: emptyList()
            if (stationObsList.isNotEmpty()) {
                html.append("<h4>Observations &amp; Rock Logs</h4>")
                stationObsList.forEach { obs ->
                    html.append("<div style=\"background-color:#f5f5f5; border-left:4px solid #4caf50; padding:6px; margin-bottom:8px; color: #000;\">")
                    html.append("<b>Type:</b> ${obs.observationType}<br/>")
                    
                    when (obs.observationType) {
                        "Lithology" -> {
                            html.append("<b>Lithology Type:</b> ${obs.lithologyType ?: "-"}<br/>")
                            html.append("<b>Texture:</b> ${obs.texture ?: "-"}<br/>")
                            html.append("<b>Color:</b> ${obs.color ?: "-"}<br/>")
                            html.append("<b>Grain Size:</b> ${obs.grainSize ?: "-"}<br/>")
                            if (!obs.lithologyAlteration.isNullOrBlank()) html.append("<b>Alt:</b> ${obs.lithologyAlteration}<br/>")
                            if (!obs.lithologyMineralization.isNullOrBlank()) html.append("<b>Mnz:</b> ${obs.lithologyMineralization}<br/>")
                        }
                        "Alteration" -> {
                            html.append("<b>Alteration:</b> ${obs.alterationType ?: "-"}<br/>")
                            html.append("<b>Intensity:</b> ${obs.intensity ?: "-"}<br/>")
                            html.append("<b>Associated Minerals:</b> ${obs.associatedMinerals ?: "-"}<br/>")
                        }
                        "Fault" -> {
                            html.append("<b>Fault Type:</b> ${obs.faultType ?: "-"}<br/>")
                            html.append("<b>Strike | Dip | DipDir:</b> ${obs.strike ?: "0"}&deg; | ${obs.dip ?: "0"}&deg; | ${obs.dipDirection ?: "0"}&deg;<br/>")
                            html.append("<b>Movement:</b> ${obs.movement ?: "-"}<br/>")
                        }
                        "Joint" -> {
                            html.append("<b>Joint Type:</b> ${obs.jointType ?: "-"}<br/>")
                            html.append("<b>Spacing:</b> ${obs.jointSpacing ?: "0"} cm<br/>")
                        }
                    }
                    
                    if (!obs.notes.isNullOrBlank()) {
                        html.append("<b>Notes:</b> ${obs.notes}<br/>")
                    }
                    
                    if (!obs.photoPath.isNullOrBlank()) {
                        val photoFile = File(obs.photoPath)
                        if (photoFile.exists()) {
                            if (isKmzMode) {
                                html.append("<br/><img src=\"images/${photoFile.name}\" width=\"240\" style=\"max-width:100%; border-radius:4px;\" /><br/>")
                            } else {
                                html.append("<br/><img src=\"file://${photoFile.absolutePath}\" width=\"240\" style=\"max-width:100%;\" /><br/>")
                            }
                        }
                    }
                    
                    val obsSamplesList = samplesByObs[obs.id] ?: emptyList()
                    if (obsSamplesList.isNotEmpty()) {
                        html.append("<h5>Samples Taken</h5>")
                        obsSamplesList.forEach { smp ->
                            html.append("<i>Code: <b>${smp.sampleCodeId}</b> (${smp.sampleType})</i><br/>")
                            if (!smp.description.isNullOrBlank()) html.append("Description: ${smp.description}<br/>")
                            if (!smp.photoPath.isNullOrBlank()) {
                                val smpFile = File(smp.photoPath)
                                if (smpFile.exists()) {
                                    if (isKmzMode) {
                                        html.append("<img src=\"images/${smpFile.name}\" width=\"160\" /><br/>")
                                    } else {
                                        html.append("<img src=\"file://${smpFile.absolutePath}\" width=\"160\" /><br/>")
                                    }
                                }
                            }
                        }
                    }
                    html.append("</div>")
                }
            }
            
            sb.append("      <description><![CDATA[$html]]></description>\n")
            sb.append("      <Point>\n")
            sb.append("        <coordinates>${st.longitude},${st.latitude},${st.elevation}</coordinates>\n")
            sb.append("      </Point>\n")
            sb.append("    </Placemark>\n")
        }
        
        sb.append("  </Document>\n")
        sb.append("</kml>\n")
        return sb.toString()
    }

    fun exportKml(context: Context, projectId: Int) {
        viewModelScope.launch {
            try {
                val kmlString = withContext(Dispatchers.IO) {
                    val project = dao.getProjectById(projectId).firstOrNull()
                    val projectName = project?.name ?: "Project_$projectId"
                    val stations = dao.getStationsForProjectSync(projectId)
                    val obsList = dao.getObservationsForProjectSync(projectId)
                    val samplesList = dao.getSamplesForProjectSync(projectId)
                    
                    val obsByStation = obsList.groupBy { it.stationId }
                    val samplesByObs = samplesList.groupBy { it.observationId }
                    
                    buildKmlContent(projectName, stations, obsByStation, samplesByObs, isKmzMode = false)
                }

                val fileName = "Project_${projectId}_Map.kml"
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val file = File(dir, fileName)
                file.writeText(kmlString)

                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.google-earth.kml+xml"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "KML Field Station Map Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share KML Map")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "KML Saved to Downloads: ${fileName}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "KML Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun exportKmz(context: Context, projectId: Int) {
        viewModelScope.launch {
            try {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val kmzFileName = "Project_${projectId}_GIS.kmz"
                val kmzFile = File(dir, kmzFileName)

                withContext(Dispatchers.IO) {
                    val project = dao.getProjectById(projectId).firstOrNull()
                    val projectName = project?.name ?: "Project_$projectId"
                    val stations = dao.getStationsForProjectSync(projectId)
                    val obsList = dao.getObservationsForProjectSync(projectId)
                    val samplesList = dao.getSamplesForProjectSync(projectId)
                    
                    val obsByStation = obsList.groupBy { it.stationId }
                    val samplesByObs = samplesList.groupBy { it.observationId }
                    
                    val kmlContent = buildKmlContent(projectName, stations, obsByStation, samplesByObs, isKmzMode = true)

                    java.util.zip.ZipOutputStream(java.io.FileOutputStream(kmzFile)).use { zos ->
                        // 1. Write doc.kml
                        zos.putNextEntry(java.util.zip.ZipEntry("doc.kml"))
                        zos.write(kmlContent.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()

                        // 2. Add attached photos under images/ inside the KMZ
                        val allPhotos = mutableSetOf<String>()
                        obsList.forEach { if (!it.photoPath.isNullOrBlank()) allPhotos.add(it.photoPath) }
                        samplesList.forEach { if (!it.photoPath.isNullOrBlank()) allPhotos.add(it.photoPath) }

                        allPhotos.forEach { path ->
                            val photoFile = File(path)
                            if (photoFile.exists()) {
                                try {
                                    zos.putNextEntry(java.util.zip.ZipEntry("images/${photoFile.name}"))
                                    java.io.FileInputStream(photoFile).use { fis ->
                                        fis.copyTo(zos)
                                    }
                                    zos.closeEntry()
                                } catch (e: Exception) {
                                    // omit individual missing/restricted photos gracefully
                                }
                            }
                        }
                    }
                }

                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, kmzFile)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.google-earth.kmz"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "KMZ Rich Map Pack Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share KMZ Pack")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "KMZ Saved to Downloads: ${kmzFileName}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "KMZ Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Direct multi-format exporters
    fun exportGeoJson(context: Context, projectId: Int) {
        viewModelScope.launch {
            try {
                val geoJsonString = withContext(Dispatchers.IO) {
                    val stations = dao.getStationsForProjectSync(projectId)
                    val sb = StringBuilder()
                    sb.append("{\n  \"type\": \"FeatureCollection\",\n  \"features\": [\n")
                    stations.forEachIndexed { index, st ->
                        sb.append("    {\n")
                        sb.append("      \"type\": \"Feature\",\n")
                        sb.append("      \"geometry\": {\n")
                        sb.append("        \"type\": \"Point\",\n")
                        sb.append("        \"coordinates\": [${st.longitude}, ${st.latitude}]\n")
                        sb.append("      },\n")
                        sb.append("      \"properties\": {\n")
                        sb.append("        \"stationCode\": \"${st.stationCode}\",\n")
                        sb.append("        \"prefix\": \"${st.prefix}\",\n")
                        sb.append("        \"elevation\": ${st.elevation},\n")
                        sb.append("        \"utmZone\": \"${st.utmZone}\",\n")
                        sb.append("        \"utmEasting\": ${st.utmEasting},\n")
                        sb.append("        \"utmNorthing\": ${st.utmNorthing}\n")
                        sb.append("      }\n")
                        sb.append("    }")
                        if (index < stations.size - 1) sb.append(",")
                        sb.append("\n")
                    }
                    sb.append("  ]\n}")
                    sb.toString()
                }

                val fileName = "Project_${projectId}_Map.geojson"
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val file = File(dir, fileName)
                file.writeText(geoJsonString)

                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/geo+json"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "GeoJSON Map Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share GeoJSON Map")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "GeoJSON Saved to Downloads: ${fileName}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "GeoJSON Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun exportPdfReport(context: Context, projectId: Int) {
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val stations = dao.getStationsForProjectSync(projectId)
                    val observations = dao.getObservationsForProjectSync(projectId)
                    val samples = dao.getSamplesForProjectSync(projectId)
                    val project = dao.getProjectById(projectId).firstOrNull() ?: return@withContext null
                    
                    val pdfDocument = generatePdfDocumentSync(project, stations, observations, samples)
                    
                    val fileName = "GeoField_Suite_Report_${projectId}.pdf"
                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                    val pdfFile = File(dir, fileName)
                    pdfFile.outputStream().use { pdfDocument.writeTo(it) }
                    pdfDocument.close()
                    pdfFile
                } ?: return@launch

                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "GeoField Professional Field Report")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share Professional Field Report")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "Premium PDF Saved to Downloads: ${file.name}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "PDF Report generation failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun generatePdfDocumentSync(
        project: Project,
        stations: List<Station>,
        observations: List<Observation>,
        samples: List<Sample>
    ): android.graphics.pdf.PdfDocument {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        
        var pageNumber = 1
        var currentPage: android.graphics.pdf.PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null
        var currentY = 50f
        
        val paintText = android.graphics.Paint()
        val paintDraw = android.graphics.Paint()
        
        // HELPER for dynamic automatic page layouting
        fun startNewPage() {
            if (currentPage != null) {
                pdfDocument.finishPage(currentPage)
            }
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            val newPage = pdfDocument.startPage(pageInfo)
            currentPage = newPage
            canvas = newPage.canvas
            currentY = 50f
            
            // Page headers/footers for normal pages (not cover page)
            val c = canvas!!
            
            // Header background accent bar at top
            paintDraw.color = android.graphics.Color.parseColor("#004D40") // Dark Teal
            paintDraw.style = android.graphics.Paint.Style.FILL
            c.drawRect(40f, 25f, 555f, 28f, paintDraw)
            
            // Header text
            paintText.color = android.graphics.Color.GRAY
            paintText.textSize = 7.5f
            paintText.isFakeBoldText = false
            paintText.textAlign = android.graphics.Paint.Align.LEFT
            c.drawText("FIELD REPORT | PROJECT: ${project.name.uppercase()}", 40f, 20f, paintText)
            paintText.textAlign = android.graphics.Paint.Align.RIGHT
            c.drawText("GEOTECH & GIS SYSTEM LOG", 555f, 20f, paintText)
            
            // Footer line and page numbering
            paintDraw.color = android.graphics.Color.parseColor("#E0E0E0")
            c.drawLine(40f, 805f, 555f, 805f, paintDraw)
            
            paintText.color = android.graphics.Color.GRAY
            paintText.textSize = 8f
            paintText.textAlign = android.graphics.Paint.Align.LEFT
            c.drawText("Page ${pageNumber - 1} | Produced Offline via GeoField Pro Suite", 40f, 820f, paintText)
            val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            paintText.textAlign = android.graphics.Paint.Align.RIGHT
            c.drawText("Date Exported: $dateFormatted", 555f, 820f, paintText)
            
            currentY = 60f
        }
        
        // ==========================================
        // PAGE 1: PROFESSIONAL COVERSHEET
        // ==========================================
        val pageInfo1 = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
        currentPage = pdfDocument.startPage(pageInfo1)
        val c1 = currentPage!!.canvas
        canvas = c1
        
        // Elegance background borders
        paintDraw.style = android.graphics.Paint.Style.STROKE
        paintDraw.strokeWidth = 1.5f
        paintDraw.color = android.graphics.Color.parseColor("#004D40") // Primary Teal
        c1.drawRect(30f, 30f, 565f, 812f, paintDraw)
        paintDraw.strokeWidth = 0.5f
        paintDraw.color = android.graphics.Color.GRAY
        c1.drawRect(33f, 33f, 562f, 809f, paintDraw)
        
        // Colored Banner Header Box in Cover page
        paintDraw.style = android.graphics.Paint.Style.FILL
        paintDraw.color = android.graphics.Color.parseColor("#004D40")
        c1.drawRect(34f, 34f, 561f, 180f, paintDraw)
        
        // Decorative Line in Header
        paintDraw.color = android.graphics.Color.parseColor("#00E676") // Cool green accent
        c1.drawRect(34f, 174f, 561f, 180f, paintDraw)
        
        // Banner Title Text
        paintText.color = android.graphics.Color.WHITE
        paintText.textSize = 21f
        paintText.isFakeBoldText = true
        paintText.textAlign = android.graphics.Paint.Align.CENTER
        c1.drawText("GEOLOGICAL FIELD SURVEY REPORT", 297f, 105f, paintText)
        
        paintText.textSize = 10.5f
        paintText.isFakeBoldText = false
        c1.drawText("PROFESSIONAL GIS & MINING COGNITIVE DATA SUITE", 297f, 130f, paintText)
        
        // Secondary Metadata Info Box (Cover Page middle)
        paintText.textAlign = android.graphics.Paint.Align.LEFT
        paintText.color = android.graphics.Color.parseColor("#212121")
        paintText.textSize = 17f
        paintText.isFakeBoldText = true
        c1.drawText("PROJECT SUMMARY LOGS", 60f, 240f, paintText)
        
        // Draw accent underline
        paintDraw.color = android.graphics.Color.parseColor("#009688")
        paintDraw.style = android.graphics.Paint.Style.FILL
        c1.drawRect(60f, 248f, 280f, 252f, paintDraw)
        
        // Details Table Box (Cover Page center-bottom)
        paintDraw.style = android.graphics.Paint.Style.FILL
        paintDraw.color = android.graphics.Color.parseColor("#F5F5F5")
        c1.drawRoundRect(60f, 275f, 535f, 440f, 8f, 8f, paintDraw)
        
        paintDraw.style = android.graphics.Paint.Style.STROKE
        paintDraw.strokeWidth = 0.5f
        paintDraw.color = android.graphics.Color.parseColor("#BDBDBD")
        c1.drawRoundRect(60f, 275f, 535f, 440f, 8f, 8f, paintDraw)
        
        // Metadata list details
        paintText.textSize = 11f
        paintText.isFakeBoldText = false
        paintText.color = android.graphics.Color.DKGRAY
        
        val itemsY = floatArrayOf(310f, 340f, 370f, 400f, 425f)
        val labelText = arrayOf(
            "Project Name:",
            "Description / Context:",
            "Total Locations (Stations):",
            "Total Recorded Observations:",
            "Total Samples Collected:"
        )
        val valText = arrayOf(
            project.name,
            project.description ?: "General Greenfield Mineral Survey",
            "${stations.size} Stations logged",
            "${observations.size} Observations details",
            "${samples.size} Specimens in hand inventory"
        )
        
        for (i in labelText.indices) {
            paintText.isFakeBoldText = true
            paintText.color = android.graphics.Color.parseColor("#004D40")
            c1.drawText(labelText[i], 80f, itemsY[i], paintText)
            
            paintText.isFakeBoldText = false
            paintText.color = android.graphics.Color.parseColor("#212121")
            c1.drawText(valText[i], 270f, itemsY[i], paintText)
        }
        
        // Cover Footer branding
        paintText.textAlign = android.graphics.Paint.Align.CENTER
        paintText.textSize = 9.5f
        paintText.isFakeBoldText = true
        paintText.color = android.graphics.Color.parseColor("#37474F")
        c1.drawText("GEOFIELD PROFESSIONAL GIS SYSTEMS LLC", 298f, 740f, paintText)
        paintText.isFakeBoldText = false
        paintText.color = android.graphics.Color.GRAY
        c1.drawText("All structures, lithologies, orientations and sample datasets are collected offline & verified digitally.", 298f, 758f, paintText)
        
        // ==========================================
        // PAGE 2: GRAPHICAL ANALYTICAL DASHBOARDS
        // ==========================================
        startNewPage()
        val c2 = canvas!!
        
        paintText.textAlign = android.graphics.Paint.Align.LEFT
        paintText.textSize = 13.5f
        paintText.isFakeBoldText = true
        paintText.color = android.graphics.Color.parseColor("#004D40")
        c2.drawText("1. STATISTICAL DISTRIBUTION AND SPATIAL GEOMETRY", 40f, 75f, paintText)
        
        // Draw divide line
        paintDraw.style = android.graphics.Paint.Style.STROKE
        paintDraw.strokeWidth = 0.5f
        paintDraw.color = android.graphics.Color.parseColor("#CFD8DC")
        c2.drawLine(40f, 85f, 555f, 85f, paintDraw)
        
        // Let's draw Chart 1: Lithology Bar Chart
        // Count prefixes
        val prefixCounts = mapOf(
            "LITH" to stations.count { it.prefix == "LITH" },
            "ALT" to stations.count { it.prefix == "ALT" },
            "FLT" to stations.count { it.prefix == "FLT" },
            "STR" to stations.count { it.prefix == "STR" || it.prefix == "JV" },
            "SMP" to samples.size
        )
        val maxVal = prefixCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        
        // Visual Framing Layout
        paintDraw.style = android.graphics.Paint.Style.FILL
        paintDraw.color = android.graphics.Color.parseColor("#ECEFF1")
        c2.drawRoundRect(40f, 105f, 280f, 290f, 6f, 6f, paintDraw)
        
        paintText.textSize = 9.5f
        paintText.isFakeBoldText = true
        paintText.color = android.graphics.Color.parseColor("#37474F")
        c2.drawText("LOGGED SPECIES GRAPH", 55f, 125f, paintText)
        
        // Draw Bars (Chart 1)
        val chartBottomY = 250f
        val chartTopY = 145f
        val chartHeight = chartBottomY - chartTopY
        val labelsList = listOf("Litho", "Alter", "Fault", "Struct", "Sample")
        val keysList = listOf("LITH", "ALT", "FLT", "STR", "SMP")
        val colList = listOf("#2E7D32", "#FF8F00", "#C62828", "#1565C0", "#6A1B9A")
        val barWidth = 24f
        val barSpacing = 16f
        val chartStartX = 75f
        
        // Y Axis line
        paintDraw.color = android.graphics.Color.parseColor("#78909C")
        paintDraw.strokeWidth = 1f
        c2.drawLine(chartStartX - 6f, chartTopY - 5f, chartStartX - 6f, chartBottomY, paintDraw)
        // X Axis line
        c2.drawLine(chartStartX - 6f, chartBottomY, chartStartX + (barWidth + barSpacing) * 5, chartBottomY, paintDraw)
        
        for (i in keysList.indices) {
            val countVal = prefixCounts[keysList[i]] ?: 0
            val valRatio = countVal.toFloat() / maxVal.toFloat()
            val currentBarHeight = chartHeight * valRatio
            val currentBarTop = chartBottomY - currentBarHeight
            
            val lX = chartStartX + i * (barWidth + barSpacing)
            val rX = lX + barWidth
            
            // Draw individual colored bar
            paintDraw.style = android.graphics.Paint.Style.FILL
            paintDraw.color = android.graphics.Color.parseColor(colList[i])
            c2.drawRect(lX, currentBarTop, rX, chartBottomY, paintDraw)
            
            // Draw value above bar
            paintText.textSize = 8f
            paintText.isFakeBoldText = true
            paintText.color = android.graphics.Color.parseColor("#263238")
            paintText.textAlign = android.graphics.Paint.Align.CENTER
            c2.drawText("$countVal", lX + (barWidth / 2), currentBarTop - 4f, paintText)
            
            // Draw label underneath axis
            paintText.isFakeBoldText = false
            paintText.color = android.graphics.Color.parseColor("#546E7A")
            c2.drawText(labelsList[i], lX + (barWidth / 2), chartBottomY + 12f, paintText)
        }
        
        // CHART 2: GEOLOGICAL CONCENTRIC STEREONET COMPASS PLOT
        paintDraw.style = android.graphics.Paint.Style.FILL
        paintDraw.color = android.graphics.Color.parseColor("#ECEFF1")
        c2.drawRoundRect(310f, 105f, 555f, 290f, 6f, 6f, paintDraw)
        
        paintText.textAlign = android.graphics.Paint.Align.LEFT
        paintText.isFakeBoldText = true
        paintText.textSize = 9.5f
        paintText.color = android.graphics.Color.parseColor("#37474F")
        c2.drawText("STRUCTURAL PLANE POLES (STEREONET)", 325f, 125f, paintText)
        
        val sCenterX = 432f
        val sCenterY = 205f
        val sRadius = 55f
        
        // Draw outer circle
        paintDraw.style = android.graphics.Paint.Style.STROKE
        paintDraw.strokeWidth = 1.2f
        paintDraw.color = android.graphics.Color.parseColor("#37474F")
        c2.drawCircle(sCenterX, sCenterY, sRadius, paintDraw)
        
        // Draw Concentric Dip rings (30 and 60 degrees)
        paintDraw.strokeWidth = 0.5f
        paintDraw.color = android.graphics.Color.parseColor("#90A4AE")
        paintDraw.pathEffect = android.graphics.DashPathEffect(floatArrayOf(3f, 3f), 0f)
        c2.drawCircle(sCenterX, sCenterY, sRadius * 0.33f, paintDraw) // 30 deg ring
        c2.drawCircle(sCenterX, sCenterY, sRadius * 0.66f, paintDraw) // 60 deg ring
        paintDraw.pathEffect = null // Reset
        
        // Axis Cross lines
        c2.drawLine(sCenterX - sRadius, sCenterY, sCenterX + sRadius, sCenterY, paintDraw)
        c2.drawLine(sCenterX, sCenterY - sRadius, sCenterX, sCenterY + sRadius, paintDraw)
        
        // Draw Compass Direction labels
        paintText.textSize = 7.5f
        paintText.isFakeBoldText = true
        paintText.color = android.graphics.Color.parseColor("#37474F")
        paintText.textAlign = android.graphics.Paint.Align.CENTER
        c2.drawText("N", sCenterX, sCenterY - sRadius - 4f, paintText)
        c2.drawText("S", sCenterX, sCenterY + sRadius + 9f, paintText)
        paintText.textAlign = android.graphics.Paint.Align.LEFT
        c2.drawText("E", sCenterX + sRadius + 3f, sCenterY + 3f, paintText)
        paintText.textAlign = android.graphics.Paint.Align.RIGHT
        c2.drawText("W", sCenterX - sRadius - 3f, sCenterY + 3f, paintText)
        
        // Plot structural observations (dip and dip directions) on the stereonets
        val structureObservations = observations.filter { 
            (it.observationType == "Fault" || it.observationType == "Joint" || it.observationType == "Vein") && 
            it.dip != null && it.dipDirection != null 
        }
        
        structureObservations.forEach { ob ->
            val dip = ob.dip ?: 0
            val ddir = ob.dipDirection ?: 0
            
            // Radians
            val radDir = Math.toRadians((ddir - 90).toDouble())
            // Equal-angle net project distance: r = R * tan(dip/2)
            val poleRatio = Math.tan(Math.toRadians(dip / 2.0)) / Math.tan(Math.toRadians(45.0))
            val finalRadius = (sRadius * poleRatio).coerceAtMost(sRadius.toDouble())
            
            val pX = (sCenterX + finalRadius * Math.cos(radDir)).toFloat()
            val pY = (sCenterY + finalRadius * Math.sin(radDir)).toFloat()
            
            // Dot color based on structure type
            paintDraw.style = android.graphics.Paint.Style.FILL
            paintDraw.color = when(ob.observationType) {
                "Fault" -> android.graphics.Color.RED
                "Joint" -> android.graphics.Color.BLUE
                else -> android.graphics.Color.MAGENTA // veins etc.
            }
            c2.drawCircle(pX, pY, 3.5f, paintDraw)
        }
        
        // Draw stereonet quick visual legend
        paintText.textAlign = android.graphics.Paint.Align.LEFT
        paintText.textSize = 7f
        paintText.isFakeBoldText = false
        paintText.color = android.graphics.Color.parseColor("#455A64")
        
        paintDraw.style = android.graphics.Paint.Style.FILL
        paintDraw.color = android.graphics.Color.RED
        c2.drawCircle(325f, 280f, 2.5f, paintDraw)
        c2.drawText("Fault Planes Plotted", 332f, 282f, paintText)
        
        paintDraw.color = android.graphics.Color.BLUE
        c2.drawCircle(410f, 280f, 2.5f, paintDraw)
        c2.drawText("Joint Fractures", 417f, 282f, paintText)
        
        paintDraw.color = android.graphics.Color.MAGENTA
        c2.drawCircle(490f, 280f, 2.5f, paintDraw)
        c2.drawText("Mineralized Veins", 497f, 282f, paintText)
        
        // Section 2 Header (Table preview in Dashboard Page)
        paintText.textAlign = android.graphics.Paint.Align.LEFT
        paintText.textSize = 12f
        paintText.isFakeBoldText = true
        paintText.color = android.graphics.Color.parseColor("#004D40")
        c2.drawText("2. CORE GEOLOGICAL STATION LOCATION METALS", 40f, 312f, paintText)
        
        // Draw divide line
        paintDraw.style = android.graphics.Paint.Style.STROKE
        paintDraw.color = android.graphics.Color.parseColor("#CFD8DC")
        c2.drawLine(40f, 320f, 555f, 320f, paintDraw)
        
        // Draw beautiful table header for station summaries
        var tableY = 335f
        paintDraw.style = android.graphics.Paint.Style.FILL
        paintDraw.color = android.graphics.Color.parseColor("#004D40")
        c2.drawRect(40f, tableY, 555f, tableY + 22f, paintDraw)
        
        paintText.color = android.graphics.Color.WHITE
        paintText.textSize = 8.5f
        paintText.isFakeBoldText = true
        
        c2.drawText("CODE", 48f, tableY + 14f, paintText)
        c2.drawText("PREFIX", 115f, tableY + 14f, paintText)
        c2.drawText("COORDINATES (LAT / LON)", 175f, tableY + 14f, paintText)
        c2.drawText("ELEVATION (M)", 350f, tableY + 14f, paintText)
        c2.drawText("UTM EASTING/NORTHING", 442f, tableY + 14f, paintText)
        
        tableY += 22f
        
        stations.take(15).forEachIndexed { index, st ->
            // Background zebra alternation band
            paintDraw.style = android.graphics.Paint.Style.FILL
            paintDraw.color = if (index % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F9FBFB")
            c2.drawRect(40f, tableY, 555f, tableY + 20f, paintDraw)
            
            // Table thin bounds lines
            paintDraw.style = android.graphics.Paint.Style.STROKE
            paintDraw.strokeWidth = 0.3f
            paintDraw.color = android.graphics.Color.parseColor("#ECEFF1")
            c2.drawRect(40f, tableY, 555f, tableY + 20f, paintDraw)
            
            paintText.color = android.graphics.Color.parseColor("#263238")
            paintText.textSize = 8f
            paintText.isFakeBoldText = false
            
            c2.drawText(st.stationCode, 48f, tableY + 12f, paintText)
            c2.drawText(st.prefix, 115f, tableY + 12f, paintText)
            val latStr = String.format("%.5f", st.latitude)
            val lonStr = String.format("%.5f", st.longitude)
            c2.drawText("$latStr, $lonStr", 175f, tableY + 12f, paintText)
            c2.drawText("${st.elevation.toInt()} m", 350f, tableY + 12f, paintText)
            c2.drawText("Z${st.utmZone} E:${st.utmEasting.toInt()} N:${st.utmNorthing.toInt()}", 442f, tableY + 12f, paintText)
            
            tableY += 20f
        }
        
        // Notice if extra stations exist
        if (stations.size > 15) {
            paintText.color = android.graphics.Color.parseColor("#00796B")
            paintText.textSize = 8.5f
            paintText.isFakeBoldText = true
            c2.drawText("* Total ${stations.size} stations logged. Continuing full tables on subsequent pages...", 40f, tableY + 15f, paintText)
        }
        
        // ==========================================
        // PAGE 3+: COMPREHENSIVE STATION OUTLOG DATABASES
        // ==========================================
        if (stations.isNotEmpty()) {
            startNewPage()
            var cN = canvas!!
            
            paintText.textAlign = android.graphics.Paint.Align.LEFT
            paintText.textSize = 12f
            paintText.isFakeBoldText = true
            paintText.color = android.graphics.Color.parseColor("#004D40")
            cN.drawText("3. DETAILED LOGGED STATION STATIONEER DATA SHEET", 40f, 75f, paintText)
            
            paintDraw.style = android.graphics.Paint.Style.STROKE
            paintDraw.strokeWidth = 0.5f
            paintDraw.color = android.graphics.Color.parseColor("#CFD8DC")
            cN.drawLine(40f, 83f, 555f, 83f, paintDraw)
            
            currentY = 95f
            
            stations.forEachIndexed { i, st ->
                val estimatedHeight = 100f
                if (currentY + estimatedHeight > 780f) {
                    startNewPage()
                    cN = canvas!!
                    currentY = 70f
                }
                
                // Station Card Border Frame
                paintDraw.style = android.graphics.Paint.Style.FILL
                paintDraw.color = android.graphics.Color.parseColor("#FAFAFA")
                cN.drawRoundRect(40f, currentY, 555f, currentY + 86f, 6f, 6f, paintDraw)
                
                paintDraw.style = android.graphics.Paint.Style.STROKE
                paintDraw.strokeWidth = 0.5f
                paintDraw.color = android.graphics.Color.parseColor("#CFD8DC")
                cN.drawRoundRect(40f, currentY, 555f, currentY + 86f, 6f, 6f, paintDraw)
                
                // Left vertical category visual color identifier bar
                paintDraw.style = android.graphics.Paint.Style.FILL
                paintDraw.color = when (st.prefix) {
                    "LITH" -> android.graphics.Color.parseColor("#2E7D32")
                    "ALT" -> android.graphics.Color.parseColor("#FF8F00")
                    "FLT" -> android.graphics.Color.parseColor("#C62828")
                    else -> android.graphics.Color.parseColor("#00796B")
                }
                cN.drawRect(40f, currentY, 44f, currentY + 86f, paintDraw)
                
                // Write codes inside cards
                paintText.color = android.graphics.Color.parseColor("#212121")
                paintText.textSize = 10f
                paintText.isFakeBoldText = true
                cN.drawText("STATION: ${st.stationCode}", 52f, currentY + 18f, paintText)
                
                // Extra properties inside card grid
                paintText.textSize = 8.2f
                paintText.color = android.graphics.Color.parseColor("#37474F")
                paintText.isFakeBoldText = false
                
                // Grid Column 1
                cN.drawText("Latitude: ${String.format("%.6f", st.latitude)}°", 52f, currentY + 36f, paintText)
                cN.drawText("Longitude: ${String.format("%.6f", st.longitude)}°", 52f, currentY + 50f, paintText)
                cN.drawText("Elevation: ${st.elevation} m (±${st.accuracy}m accuracy)", 52f, currentY + 64f, paintText)
                
                // Grid Column 2
                cN.drawText("UTM Coordinates: Zone ${st.utmZone}", 260f, currentY + 36f, paintText)
                cN.drawText("UTM Easting:  ${st.utmEasting.toInt()} m E", 260f, currentY + 50f, paintText)
                cN.drawText("UTM Northing: ${st.utmNorthing.toInt()} m N", 260f, currentY + 64f, paintText)
                
                // Timestamp at top right
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(st.createdAt))
                paintText.textAlign = android.graphics.Paint.Align.RIGHT
                paintText.color = android.graphics.Color.GRAY
                paintText.textSize = 7.5f
                cN.drawText("Logged: $dateStr", 545f, currentY + 16f, paintText)
                paintText.textAlign = android.graphics.Paint.Align.LEFT // reset
                
                currentY += 98f
            }
        }
        
        // ==========================================
        // PAGE 4+: RICH OBSERVATIONS, STRUCTURES AND SPECIMENS LOG
        // ==========================================
        if (observations.isNotEmpty()) {
            startNewPage()
            var cN = canvas!!
            
            paintText.textAlign = android.graphics.Paint.Align.LEFT
            paintText.textSize = 12f
            paintText.isFakeBoldText = true
            paintText.color = android.graphics.Color.parseColor("#004D40")
            cN.drawText("4. SURVEY OBSERVATIONS, STRUCTURAL PLOT ATTITUDES & SAMPLES LOG", 40f, 75f, paintText)
            
            paintDraw.style = android.graphics.Paint.Style.STROKE
            paintDraw.strokeWidth = 0.5f
            paintDraw.color = android.graphics.Color.parseColor("#CFD8DC")
            cN.drawLine(40f, 83f, 555f, 83f, paintDraw)
            
            currentY = 95f
            
            val stationCodeMap = stations.associate { it.id to it.stationCode }
            
            observations.forEachIndexed { idx, ob ->
                val linkedStationCode = stationCodeMap[ob.stationId] ?: "N/A"
                
                // Calculate dynamic card height securely
                var cardHeight = 115f
                if (!ob.notes.isNullOrBlank()) cardHeight += 20f
                
                if (currentY + cardHeight > 780f) {
                    startNewPage()
                    cN = canvas!!
                    currentY = 70f
                }
                
                // Outer capsule container
                paintDraw.style = android.graphics.Paint.Style.FILL
                paintDraw.color = android.graphics.Color.parseColor("#FAFAFA")
                cN.drawRoundRect(40f, currentY, 555f, currentY + cardHeight - 10f, 6f, 6f, paintDraw)
                
                paintDraw.style = android.graphics.Paint.Style.STROKE
                paintDraw.strokeWidth = 0.5f
                paintDraw.color = android.graphics.Color.parseColor("#CFD8DC")
                cN.drawRoundRect(40f, currentY, 555f, currentY + cardHeight - 10f, 6f, 6f, paintDraw)
                
                // Color classification bar
                paintDraw.style = android.graphics.Paint.Style.FILL
                paintDraw.color = when (ob.observationType) {
                    "Lithology" -> android.graphics.Color.parseColor("#2E7D32")
                    "Alteration" -> android.graphics.Color.parseColor("#FF8F00")
                    "Fault" -> android.graphics.Color.RED
                    else -> android.graphics.Color.parseColor("#6A1B9A")
                }
                cN.drawRect(40f, currentY, 44f, currentY + cardHeight - 10f, paintDraw)
                
                // Header row details inside the observation log card
                paintText.textSize = 9.5f
                paintText.isFakeBoldText = true
                paintText.color = android.graphics.Color.parseColor("#212121")
                cN.drawText("STATION REFER: $linkedStationCode  |  TYPE: ${ob.observationType.uppercase()}", 52f, currentY + 18f, paintText)
                
                // Timestamp
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(ob.createdAt))
                paintText.textAlign = android.graphics.Paint.Align.RIGHT
                paintText.color = android.graphics.Color.GRAY
                paintText.textSize = 7.5f
                cN.drawText("Record Time: $dateStr", 545f, currentY + 16f, paintText)
                paintText.textAlign = android.graphics.Paint.Align.LEFT // reset
                
                paintText.textSize = 8.5f
                paintText.color = android.graphics.Color.parseColor("#37474F")
                paintText.isFakeBoldText = false
                
                // Detail properties drawing values of specific type
                var itemY = currentY + 36f
                
                when (ob.observationType) {
                    "Lithology" -> {
                        cN.drawText("Lithology Class:  ${ob.lithologyType ?: "-"}", 52f, itemY, paintText)
                        cN.drawText("Rock Texture:     ${ob.texture ?: "-"}", 52f, itemY + 14f, paintText)
                        cN.drawText("Rock Solid Color: ${ob.color ?: "-"}", 280f, itemY, paintText)
                        cN.drawText("Grain Dimensions: ${ob.grainSize ?: "-"}", 280f, itemY + 14f, paintText)
                        itemY += 28f
                        
                        if (!ob.lithologyAlteration.isNullOrBlank() || !ob.lithologyMineralization.isNullOrBlank()) {
                            cN.drawText("Syn-Alteration:   ${ob.lithologyAlteration ?: "-"}", 52f, itemY, paintText)
                            cN.drawText("Mineralizations:  ${ob.lithologyMineralization ?: "-"}", 280f, itemY, paintText)
                            itemY += 14f
                        }
                    }
                    "Alteration" -> {
                        cN.drawText("Alteration Class: ${ob.alterationType ?: "-"}", 52f, itemY, paintText)
                        cN.drawText("Intensity Level:  ${ob.intensity ?: "-"}", 52f, itemY + 14f, paintText)
                        cN.drawText("Host Parent Rock: ${ob.hostRock ?: "-"}", 280f, itemY, paintText)
                        cN.drawText("Mineral Links:    ${ob.associatedMinerals ?: "-"}", 280f, itemY + 14f, paintText)
                        itemY += 28f
                    }
                    "Fault" -> {
                        cN.drawText("Fault Style:      ${ob.faultType ?: "-"}", 52f, itemY, paintText)
                        cN.drawText("Fault Strike:     ${ob.strike ?: 0}°", 52f, itemY + 14f, paintText)
                        cN.drawText("Movement Type:    ${ob.movement ?: "-"}", 52f, itemY + 28f, paintText)
                        
                        cN.drawText("Attitude Dip:     ${ob.dip ?: 0}°", 280f, itemY, paintText)
                        cN.drawText("Dip Direction:    ${ob.dipDirection ?: 0}°", 280f, itemY + 14f, paintText)
                        cN.drawText("Rake / Slippage:  ${ob.rake ?: 0}° ${ob.rakeDirection ?: ""}", 280f, itemY + 28f, paintText)
                        itemY += 42f
                    }
                    "Joint" -> {
                        cN.drawText("Fracture Joint:   ${ob.jointType ?: "-"}", 52f, itemY, paintText)
                        cN.drawText("Strike Angle:     ${ob.strike ?: 0}°", 52f, itemY + 14f, paintText)
                        cN.drawText("Spacing Width:    ${ob.jointSpacing ?: 0.0} cm", 280f, itemY, paintText)
                        cN.drawText("Fracture Dip:     ${ob.dip ?: 0}°  Dir: ${ob.dipDirection ?: 0}°", 280f, itemY + 14f, paintText)
                        itemY += 28f
                    }
                    "Vein" -> {
                        cN.drawText("Vein Type / Class: ${ob.veinType ?: "-"}", 52f, itemY, paintText)
                        cN.drawText("Mineralogy Comps: ${ob.veinMineralogy ?: "-"}", 52f, itemY + 14f, paintText)
                        cN.drawText("Planar Strike/Dip: ${ob.strike ?: 0}° / ${ob.dip ?: 0}°", 280f, itemY, paintText)
                        cN.drawText("Vein Aperture:    ${ob.width ?: 0.0} cm", 280f, itemY + 14f, paintText)
                        itemY += 28f
                    }
                    else -> {
                        cN.drawText("Category Details:  ${ob.observationType}", 52f, itemY, paintText)
                        itemY += 14f
                    }
                }
                
                // Append custom field notes if present
                if (!ob.notes.isNullOrBlank()) {
                    paintText.color = android.graphics.Color.parseColor("#455A64")
                    paintText.isFakeBoldText = true
                    cN.drawText("Geologist Field Notes: ", 52f, itemY, paintText)
                    paintText.isFakeBoldText = false
                    cN.drawText(ob.notes ?: "", 152f, itemY, paintText)
                    itemY += 14f
                }
                
                // Check description of samples if taken linked via database sync
                val linkedSample = samples.firstOrNull { it.observationId == ob.id }
                if (linkedSample != null) {
                    paintDraw.style = android.graphics.Paint.Style.FILL
                    paintDraw.color = android.graphics.Color.parseColor("#F3E5F5")
                    cN.drawRoundRect(50f, itemY, 545f, itemY + 18f, 3f, 3f, paintDraw)
                    
                    paintText.textSize = 8f
                    paintText.isFakeBoldText = true
                    paintText.color = android.graphics.Color.parseColor("#6A1B9A")
                    cN.drawText("LOCKED SPECIMEN DISCOVERY: ", 56f, itemY + 12f, paintText)
                    paintText.isFakeBoldText = false
                    cN.drawText("Code: ${linkedSample.sampleCodeId} | Category: ${linkedSample.sampleType} | ${linkedSample.description ?: ""}", 180f, itemY + 12f, paintText)
                }
                
                currentY += cardHeight
            }
        }
        
        if (currentPage != null) {
            pdfDocument.finishPage(currentPage)
        }
        
        return pdfDocument
    }

    fun exportCsv(context: Context, projectId: Int) {
        viewModelScope.launch {
            try {
                val csvString = withContext(Dispatchers.IO) {
                    val stations = dao.getStationsForProjectSync(projectId)
                    val sb = StringBuilder()
                    sb.append("StationCode,Prefix,Latitude,Longitude,Elevation,UTMZone,UTMEasting,UTMNorthing\n")
                    stations.forEach { st ->
                        sb.append("${st.stationCode},${st.prefix},${st.latitude},${st.longitude},${st.elevation},${st.utmZone},${st.utmEasting},${st.utmNorthing}\n")
                    }
                    sb.toString()
                }

                val fileName = "Project_${projectId}_Stations.csv"
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val file = File(dir, fileName)
                file.writeText(csvString)

                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "CSV Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share CSV Data")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "CSV Saved to Downloads: ${fileName}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "CSV Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun exportBoreholesCsv(context: Context, projectId: Int) {
        viewModelScope.launch {
            try {
                val csvString = withContext(Dispatchers.IO) {
                    val boreholes = dao.getBoreholesForProjectSync(projectId)
                    val sb = StringBuilder()
                    sb.append("BoreholeCode,Latitude,Longitude,Elevation,Azimuth,Dip,PlannedDepthM\n")
                    boreholes.forEach { bh ->
                        sb.append("${bh.boreholeCode},${bh.latitude},${bh.longitude},${bh.elevation},${bh.azimuth},${bh.dip},${bh.plannedDepth}\n")
                    }
                    sb.toString()
                }

                val fileName = "Project_${projectId}_Boreholes.csv"
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val file = File(dir, fileName)
                file.writeText(csvString)

                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Project Boreholes Directory")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share Boreholes CSV")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "Exported: ${fileName}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun exportBoreholeLogsCsv(context: Context, boreholeId: Int, boreholeCode: String) {
        viewModelScope.launch {
            try {
                val csvString = withContext(Dispatchers.IO) {
                    val runs = dao.getCoreRunsForBoreholeSync(boreholeId)
                    val sb = StringBuilder()
                    sb.append("RunNo,DepthFrom,DepthTo,RecoveryPercent,RqdPercent,Lithology,Alteration,Mineralization,FracturesPerM,UCS_Mpa\n")
                    runs.forEach { r ->
                        sb.append("${r.runNumber},${r.depthFrom},${r.depthTo},${r.recoveryPercent},${r.rqdPercent},\"${r.lithology}\",\"${r.alteration}\",\"${r.mineralization}\",${r.fractureFrequency},${r.ucsMpa}\n")
                    }
                    sb.toString()
                }

                val fileName = "Borehole_${boreholeCode}_CoreRuns.csv"
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val file = File(dir, fileName)
                file.writeText(csvString)

                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Borehole Core Runs Log")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share Core Runs CSV")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "Exported: ${fileName}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun exportBoreholeDailyReportsCsv(context: Context, boreholeId: Int, boreholeCode: String) {
        viewModelScope.launch {
            try {
                val csvString = withContext(Dispatchers.IO) {
                    val reports = dao.getDailyReportsForBoreholeSync(boreholeId)
                    val sb = StringBuilder()
                    sb.append("ReportDate,Shift,DrillAdvanceM,StoppageHours,ProblemsAndRemarks,ShiftGeologist\n")
                    reports.forEach { rp ->
                        sb.append("${rp.reportDate},${rp.shift},${rp.progressMeters},${rp.stoppagesHours},\"${rp.problemsRemarks}\",\"${rp.shiftGeologist}\"\n")
                    }
                    sb.toString()
                }

                val fileName = "Borehole_${boreholeCode}_DailyReports.csv"
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val file = File(dir, fileName)
                file.writeText(csvString)

                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Borehole Daily Shift Reports")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share Drilling Reports CSV")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "Exported: ${fileName}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Boreholes, Daily Reports, Core Runs, and Rock Mechanics operations
    private val activeBoreholeId = MutableStateFlow<Int?>(null)
    
    val activeBorehole: StateFlow<Borehole?> = activeBoreholeId.flatMapLatest { id ->
        if (id != null) dao.getBoreholeById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setActiveBorehole(id: Int?) {
        activeBoreholeId.value = id
    }

    fun getBoreholesForProject(projectId: Int): StateFlow<List<Borehole>> {
        return dao.getBoreholesForProject(projectId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun getDailyReportsForBorehole(boreholeId: Int): StateFlow<List<BoreholeDailyReport>> {
        return dao.getDailyReportsForBorehole(boreholeId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun getCoreRunsForBorehole(boreholeId: Int): StateFlow<List<BoreholeCoreRun>> {
        return dao.getCoreRunsForBorehole(boreholeId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun getRockMechanicsEvaluationsForProject(projectId: Int): StateFlow<List<RockMechanicsEvaluation>> {
        return dao.getRockMechanicsEvaluationsForProject(projectId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun saveBorehole(borehole: Borehole, onSaved: (Int) -> Unit) {
        viewModelScope.launch {
            val id = dao.insertBorehole(borehole)
            onSaved(id.toInt())
        }
    }

    fun deleteBorehole(id: Int) {
        viewModelScope.launch {
            dao.deleteBoreholeById(id)
            if (activeBoreholeId.value == id) {
                activeBoreholeId.value = null
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

    fun updateStation(station: Station) {
        viewModelScope.launch {
            dao.insertStation(station)
        }
    }

    fun updateObservation(observation: Observation) {
        viewModelScope.launch {
            dao.insertObservation(observation)
        }
    }

    private fun toCsvString(headers: List<String>, rows: List<List<Any?>>): String {
        val sb = java.lang.StringBuilder()
        sb.append(headers.joinToString(",") { escapeCsvField(it) }).append("\n")
        rows.forEach { row ->
            sb.append(row.joinToString(",") { escapeCsvField(it?.toString() ?: "") }).append("\n")
        }
        return sb.toString()
    }

    private fun escapeCsvField(field: String): String {
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\""
        }
        return field
    }

    fun seedDemoDataset(context: Context) {
        viewModelScope.launch {
            try {
                // 1. Create Project
                val projId = dao.insertProject(
                    Project(
                        name = "Sarcheshmeh Deep Copper Exploration Project",
                        description = "Supplementary exploration of Sarcheshmeh porphyry deposit, Kerman region - Copper Zone",
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        utmZoneMode = "MANUAL",
                        utmZoneManual = "40R"
                    )
                ).toInt()

                // 2. Create Stations
                val st1Id = dao.insertStation(
                    Station(
                        projectId = projId,
                        stationCode = "SARCH-ST-001",
                        prefix = "SARCH-ST",
                        latitude = 29.9675,
                        longitude = 55.8752,
                        elevation = 1620.0,
                        accuracy = 1.4,
                        utmEasting = 412500.0,
                        utmNorthing = 3315800.0,
                        utmZone = "40R"
                    )
                ).toInt()

                val st2Id = dao.insertStation(
                    Station(
                        projectId = projId,
                        stationCode = "SARCH-ST-002",
                        prefix = "SARCH-ST",
                        latitude = 29.9682,
                        longitude = 55.8761,
                        elevation = 1635.0,
                        accuracy = 2.1,
                        utmEasting = 412610.0,
                        utmNorthing = 3315910.0,
                        utmZone = "40R"
                    )
                ).toInt()

                // 3. Observations for Station 1
                val obs1Id = dao.insertObservation(
                    Observation(
                        stationId = st1Id,
                        observationType = "Lithology",
                        notes = "Subvolcanic granitoid outcrop showing intense potassic alteration",
                        lithologyType = "Granodiorite Porphyry",
                        texture = "Porphyritic",
                        color = "Light Gray",
                        grainSize = "Medium-Coarse",
                        lithologyAlteration = "Potassic",
                        lithologyMineralization = "Chalcopyrite-Pyrite disseminated",
                        photoPath = null
                    )
                ).toInt()

                dao.insertSample(
                    Sample(
                        observationId = obs1Id,
                        sampleCodeId = "SMP-SARCH-01",
                        sampleType = "Hand specimen",
                        description = "High-grade porphyry copper core sample with chalcopyrite mineralization",
                        photoPath = null
                    )
                )

                dao.insertObservation(
                    Observation(
                        stationId = st1Id,
                        observationType = "Alteration",
                        notes = "Potassic alteration zone focused around the porphyry stocks",
                        alterationType = "Potassic",
                        intensity = "Strong",
                        hostRock = "Granodiorite",
                        associatedMinerals = "K-Feldspar, Biotite, Magnetite",
                        photoPath = null
                    )
                )

                // Observations for Station 2
                val obs2Id = dao.insertObservation(
                    Observation(
                        stationId = st2Id,
                        observationType = "Fault",
                        notes = "High-angle shear fault zone hosting secondary copper mineralization",
                        faultType = "Shear Zone",
                        strike = 45,
                        dip = 62,
                        dipDirection = 135,
                        rake = 30,
                        rakeDirection = "SE",
                        movement = "Strike-Slip Dextral",
                        width = 1.2,
                        photoPath = null
                    )
                ).toInt()

                dao.insertObservation(
                    Observation(
                        stationId = st2Id,
                        observationType = "Mineralization",
                        notes = "High-grade secondary sulfide copper mineralization",
                        minStyle = "Stockwork Veinlets",
                        minAssociatedMinerals = "Chalcopyrite, Chalcocite, Malachite",
                        minGradeEstimate = "Cu 1.8% to 2.5%",
                        photoPath = null
                    )
                )

                // 4. Create Boreholes
                val bhId = dao.insertBorehole(
                    Borehole(
                        projectId = projId,
                        boreholeCode = "BH-SARCH-101",
                        latitude = 29.9678,
                        longitude = 55.8755,
                        elevation = 1622.0,
                        azimuth = 0.0,
                        dip = -90.0,
                        plannedDepth = 250.0
                    )
                ).toInt()

                // Borehole Daily Reports
                dao.insertDailyReport(
                    BoreholeDailyReport(
                        boreholeId = bhId,
                        reportDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        shift = "Day",
                        progressMeters = 18.5,
                        stoppagesHours = 1.5,
                        problemsRemarks = "Replaced drill bit and stabilized borehole wall collapse",
                        shiftGeologist = "Dr. Fatemi",
                        drillRigId = "XY-2B"
                    )
                )

                dao.insertDailyReport(
                    BoreholeDailyReport(
                        boreholeId = bhId,
                        reportDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        shift = "Night",
                        progressMeters = 22.0,
                        stoppagesHours = 0.5,
                        problemsRemarks = "Continuous smooth run in hard granodioritic rock",
                        shiftGeologist = "Eng. Moradi",
                        drillRigId = "XY-2B"
                    )
                )

                // Borehole Core Runs
                dao.insertCoreRun(
                    BoreholeCoreRun(
                        boreholeId = bhId,
                        runNumber = 1,
                        depthFrom = 0.0,
                        depthTo = 12.0,
                        recoveryPercent = 88.0,
                        rqdPercent = 65.0,
                        lithology = "Alluvium / Overburden",
                        mineralization = "None",
                        alteration = "None",
                        fractureFrequency = 12,
                        ucsMpa = 15.0
                    )
                )

                dao.insertCoreRun(
                    BoreholeCoreRun(
                        boreholeId = bhId,
                        runNumber = 2,
                        depthFrom = 12.0,
                        depthTo = 24.5,
                        recoveryPercent = 95.5,
                        rqdPercent = 82.0,
                        lithology = "Andesite",
                        mineralization = "Malachite stains",
                        alteration = "Propylitic",
                        fractureFrequency = 5,
                        ucsMpa = 72.0
                    )
                )

                dao.insertCoreRun(
                    BoreholeCoreRun(
                        boreholeId = bhId,
                        runNumber = 3,
                        depthFrom = 24.5,
                        depthTo = 38.0,
                        recoveryPercent = 98.2,
                        rqdPercent = 91.5,
                        lithology = "Granodiorite Porphyry",
                        mineralization = "Chalcopyrite stockwork",
                        alteration = "Potassic",
                        fractureFrequency = 2,
                        ucsMpa = 110.0
                    )
                )

                // 5. Rock Mechanics evaluations
                dao.insertRockMechanicsEvaluation(
                    RockMechanicsEvaluation(
                        projectId = projId,
                        locationName = "West Pit Wall",
                        evaluationType = "RMR",
                        evaluationDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        rmrUcsVal = 12.0,
                        rmrRqdVal = 17.0,
                        rmrSpacingVal = 15.0,
                        rmrConditionVal = 22.0,
                        rmrWaterVal = 10.0,
                        rmrAdjustmentVal = -5.0,
                        rmrScore = 71.0,
                        rmrClass = "Class II: Good Rock",
                        notes = "Stability assessment of the slope within the granitic zone"
                    )
                )

                dao.insertRockMechanicsEvaluation(
                    RockMechanicsEvaluation(
                        projectId = projId,
                        locationName = "North Pit Wall",
                        evaluationType = "Hoek-Brown",
                        evaluationDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        hbSigci = 85.0,
                        hbGsi = 55.0,
                        hbMi = 15.0,
                        hbD = 0.3,
                        hbMb = 3.65,
                        hbS = 0.0067,
                        hbA = 0.504,
                        hbSigmaci = 6.94,
                        notes = "Hoek-Brown intact rock mass parameter estimation for altered andesite"
                    )
                )

                dao.insertRockMechanicsEvaluation(
                    RockMechanicsEvaluation(
                        projectId = projId,
                        locationName = "Exploration Tunnel Level 1500",
                        evaluationType = "MRMR",
                        evaluationDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        mrmrRmrBasic = 65.0,
                        mrmrWeatheringAdj = 0.9,
                        mrmrJointOrientationAdj = 0.85,
                        mrmrDrillingAdj = 0.95,
                        mrmrScore = 47.3,
                        mrmrClass = "Class IIIb: Fair Rock",
                        notes = "Modified MRMR index estimation for mining gallery excavation in porphyry zone"
                    )
                )

                // Automatically navigate and focus on the seeded project!
                withContext(Dispatchers.Main) {
                    setActiveProject(projId)
                    navigateTo(Screen.ProjectDashboard(projId))
                    Toast.makeText(context, "Rich exploration demo dataset successfully seeded!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Seeding error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun exportProjectAgs(context: Context, projectId: Int) {
        viewModelScope.launch {
            try {
                val agsString = withContext(Dispatchers.IO) {
                    val project = dao.getProjectById(projectId).firstOrNull()
                    val projectName = project?.name ?: "Project_$projectId"
                    val stations = dao.getStationsForProjectSync(projectId)
                    val obsList = dao.getObservationsForProjectSync(projectId)
                    val boreholes = dao.getBoreholesForProjectSync(projectId)

                    val sb = java.lang.StringBuilder()
                    sb.append("\"GROUP\",\"PROJ\"\n")
                    sb.append("\"HEADING\",\"PROJ_ID\",\"PROJ_NAME\",\"PROJ_DESC\"\n")
                    sb.append("\"UNIT\",\"\",\"\",\"\"\n")
                    sb.append("\"TYPE\",\"X\",\"X\",\"X\"\n")
                    sb.append("\"DATA\",\"$projectId\",\"${projectName.replace("\"", "''")}\",\"Geotechnical Field Studies AGS Export\"\n\n")

                    sb.append("\"GROUP\",\"LOCA\"\n")
                    sb.append("\"HEADING\",\"LOCA_ID\",\"LOCA_TYPE\",\"LOCA_NORT\",\"LOCA_EAST\",\"LOCA_ALTD\",\"LOCA_LAT\",\"LOCA_LON\"\n")
                    sb.append("\"UNIT\",\"\",\"\",\"m\",\"m\",\"m\",\"\",\"\"\n")
                    sb.append("\"TYPE\",\"X\",\"X\",\"DP\",\"DP\",\"DP\",\"DP\",\"DP\"\n")
                    
                    stations.forEach { st ->
                        sb.append("\"DATA\",\"${st.stationCode}\",\"Field Station\",\"${st.utmNorthing}\",\"${st.utmEasting}\",\"${st.elevation}\",\"${st.latitude}\",\"${st.longitude}\"\n")
                    }
                    boreholes.forEach { bh ->
                        sb.append("\"DATA\",\"${bh.boreholeCode}\",\"Borehole\",\"\",\"\",\"${bh.elevation}\",\"${bh.latitude}\",\"${bh.longitude}\"\n")
                    }
                    sb.append("\n")

                    // Field Observations group
                    sb.append("\"GROUP\",\"GEOL\"\n")
                    sb.append("\"HEADING\",\"LOCA_ID\",\"GEOL_DESC\",\"GEOL_TYPE\",\"STRIKE\",\"DIP\",\"DIP_DIR\"\n")
                    sb.append("\"UNIT\",\"\",\"\",\"\",\"deg\",\"deg\",\"deg\"\n")
                    sb.append("\"TYPE\",\"X\",\"X\",\"X\",\"DP\",\"DP\",\"DP\"\n")
                    obsList.forEach { obs ->
                        val station = stations.firstOrNull { it.id == obs.stationId }
                        if (station != null) {
                            val notesClean = (obs.notes ?: "").replace("\"", "''").replace("\n", " ")
                            val typeClean = obs.observationType
                            val strike = obs.strike ?: ""
                            val dip = obs.dip ?: ""
                            val dipDir = obs.dipDirection ?: ""
                            sb.append("\"DATA\",\"${station.stationCode}\",\"$notesClean\",\"$typeClean\",\"$strike\",\"$dip\",\"$dipDir\"\n")
                        }
                    }
                    sb.append("\n")

                    // Core logs group
                    val coreRuns = mutableListOf<BoreholeCoreRun>()
                    boreholes.forEach { bh ->
                        coreRuns.addAll(dao.getCoreRunsForBoreholeSync(bh.id))
                    }
                    if (coreRuns.isNotEmpty()) {
                        sb.append("\"GROUP\",\"CORE\"\n")
                        sb.append("\"HEADING\",\"LOCA_ID\",\"CORE_RUN\",\"CORE_TOP\",\"CORE_BASE\",\"CORE_REC\",\"CORE_RQD\",\"GEOL_DESC\"\n")
                        sb.append("\"UNIT\",\"\",\"\",\"m\",\"m\",\"%\",\"%\",\"\"\n")
                        sb.append("\"TYPE\",\"X\",\"X\",\"DP\",\"DP\",\"DP\",\"DP\",\"X\"\n")
                        coreRuns.forEach { run ->
                            val bh = boreholes.firstOrNull { it.id == run.boreholeId }
                            if (bh != null) {
                                val desc = "Lith: ${run.lithology}, Alt: ${run.alteration}, Min: ${run.mineralization}".replace("\"", "''")
                                sb.append("\"DATA\",\"${bh.boreholeCode}\",\"${run.runNumber}\",\"${run.depthFrom}\",\"${run.depthTo}\",\"${run.recoveryPercent}\",\"${run.rqdPercent}\",\"$desc\"\n")
                            }
                        }
                    }

                    sb.toString()
                }

                val fileName = "Project_${projectId}_Geotechnical_AGS4.ags"
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val file = File(dir, fileName)
                file.writeText(agsString)

                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "AGS v4 Engineering Geotechnical Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share AGS v4 File")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "AGS file exported to Downloads: $fileName", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "AGS Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun exportProjectSurpacCsv(context: Context, projectId: Int) {
        viewModelScope.launch {
            try {
                val zipFile = withContext(Dispatchers.IO) {
                    val boreholes = dao.getBoreholesForProjectSync(projectId)
                    
                    val collarSb = java.lang.StringBuilder()
                    collarSb.append("Hole_ID,Latitude,Longitude,Elevation,Planned_Depth,Azimuth,Dip\n")
                    boreholes.forEach { bh ->
                        collarSb.append("${bh.boreholeCode},${bh.latitude},${bh.longitude},${bh.elevation},${bh.plannedDepth},${bh.azimuth},${bh.dip}\n")
                    }

                    val intervalSb = java.lang.StringBuilder()
                    intervalSb.append("Hole_ID,Depth_From,Depth_To,Recovery_Pct,RQD_Pct,Lithology,Alteration,Mineralization,UCS_MPa\n")
                    
                    boreholes.forEach { bh ->
                        val coreRuns = dao.getCoreRunsForBoreholeSync(bh.id)
                        coreRuns.forEach { run ->
                            val lith = run.lithology.replace(",", ";")
                            val alt = run.alteration.replace(",", ";")
                            val min = run.mineralization.replace(",", ";")
                            intervalSb.append("${bh.boreholeCode},${run.depthFrom},${run.depthTo},${run.recoveryPercent},${run.rqdPercent},$lith,$alt,$min,${run.ucsMpa}\n")
                        }
                    }

                    val tempCollar = File.createTempFile("collar_", ".csv", context.cacheDir)
                    tempCollar.writeText(collarSb.toString())

                    val tempInterval = File.createTempFile("interval_", ".csv", context.cacheDir)
                    tempInterval.writeText(intervalSb.toString())

                    val targetZip = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir, "Project_${projectId}_Leapfrog_3D_Modeling_Tables.zip")
                    java.util.zip.ZipOutputStream(java.io.FileOutputStream(targetZip)).use { zos ->
                        zos.putNextEntry(java.util.zip.ZipEntry("collar.csv"))
                        zos.write(tempCollar.readBytes())
                        zos.closeEntry()

                        zos.putNextEntry(java.util.zip.ZipEntry("intervals.csv"))
                        zos.write(tempInterval.readBytes())
                        zos.closeEntry()
                    }
                    tempCollar.delete()
                    tempInterval.delete()
                    targetZip
                }

                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, zipFile)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Leapfrog/Surpac 3D Geological Data Import Tables")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share Geological 3D Tables")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "Leapfrog ZIP exported: ${zipFile.name}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Leapfrog/Surpac Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
