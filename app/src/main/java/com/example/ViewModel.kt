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
                if (location.accuracy > 35.0) return

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
                    val project = dao.getProjectById(projectId).firstOrNull() ?: return@withContext null
                    
                    val pdfDocument = android.graphics.pdf.PdfDocument()
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 pt
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    
                    val paint = android.graphics.Paint()
                    
                    // Header title
                    paint.color = android.graphics.Color.DKGRAY
                    paint.textSize = 18f
                    paint.isFakeBoldText = true
                    canvas.drawText("Geology & Mine Field Report", 40f, 60f, paint)
                    
                    // Subtitle
                    paint.textSize = 12f
                    paint.isFakeBoldText = false
                    canvas.drawText("Project: ${project.name}", 40f, 90f, paint)
                    canvas.drawText("Description: ${project.description ?: "General Survey"}", 40f, 110f, paint)
                    
                    paint.color = android.graphics.Color.LTGRAY
                    canvas.drawLine(40f, 130f, 555f, 130f, paint)
                    
                    paint.color = android.graphics.Color.BLACK
                    paint.textSize = 10f
                    canvas.drawText("Logged Stations Summary:", 40f, 160f, paint)
                    
                    var drawY = 190f
                    stations.take(20).forEach { st ->
                        if (drawY < 800) {
                            canvas.drawText(
                                "Station: ${st.stationCode} | Coords: ${String.format("%.5f", st.latitude)}, ${String.format("%.5f", st.longitude)} | UTM: Z${st.utmZone} E:${st.utmEasting.toInt()} N:${st.utmNorthing.toInt()}",
                                60f,
                                drawY,
                                paint
                            )
                            drawY += 22f
                        }
                    }
                    
                    if (stations.size > 20) {
                        canvas.drawText("... and ${stations.size - 20} more stations logged.", 60f, drawY, paint)
                    }
                    
                    pdfDocument.finishPage(page)
                    
                    val fileName = "Geotech_Report_${projectId}.pdf"
                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                    val pdfFile = File(dir, fileName)
                    pdfDocument.writeTo(pdfFile.outputStream())
                    pdfDocument.close()
                    pdfFile
                } ?: return@launch

                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "PDF Field Report Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share PDF Report")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "PDF Report Saved to Downloads: ${file.name}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "PDF Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
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
}
