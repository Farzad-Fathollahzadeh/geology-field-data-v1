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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Represents the screen backstack/routes for state-based navigation
sealed interface Screen {
    object Home : Screen
    object ProjectList : Screen
    object CreateProject : Screen
    data class ProjectDashboard(val projectId: Int) : Screen
    data class CreateStation(val projectId: Int) : Screen
    data class StationDetail(val stationId: Int) : Screen
    data class AddObservation(val stationId: Int) : Screen
    
    // Borehole logging section
    object BoreholeList : Screen
    object CreateBorehole : Screen
    data class BoreholeDashboard(val boreholeId: Int) : Screen
    data class CreateDailyLog(val boreholeId: Int) : Screen
    data class CreateCoreRun(val boreholeId: Int, val runId: Int? = null) : Screen
    object RockEngineering : Screen
    object Settings : Screen
    object StationOfflineMap : Screen
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

    private val prefs = application.getSharedPreferences("geofield_premium_prefs", Context.MODE_PRIVATE)

    // Live preference states accessible globally in UI
    val fontSizeMultiplier = MutableStateFlow(prefs.getFloat("font_size_multiplier", 1.0f))
    val fontFamilySelection = MutableStateFlow(prefs.getString("font_family", "SansSerif") ?: "SansSerif")
    val gpsAccuracyMode = MutableStateFlow(prefs.getString("gps_accuracy_mode", "HIGH_ACCURACY") ?: "HIGH_ACCURACY")
    val themeSelection = MutableStateFlow(prefs.getString("theme_selection", "DARK") ?: "DARK")
    val touchSizingProfile = MutableStateFlow(prefs.getString("touch_sizing_profile", "STANDARD") ?: "STANDARD")

    fun saveFontSizeMultiplier(multiplier: Float) {
        fontSizeMultiplier.value = multiplier
        prefs.edit().putFloat("font_size_multiplier", multiplier).apply()
    }

    fun saveFontFamily(family: String) {
        fontFamilySelection.value = family
        prefs.edit().putString("font_family", family).apply()
    }

    fun saveGpsAccuracyMode(mode: String) {
        gpsAccuracyMode.value = mode
        prefs.edit().putString("gps_accuracy_mode", mode).apply()
    }

    fun saveThemeSelection(theme: String) {
        themeSelection.value = theme
        prefs.edit().putString("theme_selection", theme).apply()
    }

    fun saveTouchSizingProfile(profile: String) {
        touchSizingProfile.value = profile
        prefs.edit().putString("touch_sizing_profile", profile).apply()
    }

    private val db = Room.databaseBuilder(
        application,
        GeologyDatabase::class.java,
        "geology_field_logger.db"
    ).fallbackToDestructiveMigration().build()
    
    val dao = db.dao()

    // Screen navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
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
            is Screen.BoreholeDashboard -> {
                setActiveBorehole(screen.boreholeId)
            }
            is Screen.CreateDailyLog -> {
                setActiveBorehole(screen.boreholeId)
            }
            is Screen.CreateCoreRun -> {
                setActiveBorehole(screen.boreholeId)
            }
            else -> {}
        }
    }

    // List of all projects
    val projects: StateFlow<List<Project>> = dao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // --- Boreholes State Management ---
    val boreholes: StateFlow<List<Borehole>> = dao.getAllBoreholes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStations: StateFlow<List<Station>> = dao.getAllStations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTracks: StateFlow<List<MapTrack>> = dao.getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWaypoints: StateFlow<List<MapWaypoint>> = dao.getAllWaypoints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allObservations: StateFlow<List<Observation>> = dao.getAllObservations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWindowMappings: StateFlow<List<WindowMapping>> = dao.getAllWindowMappings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val activeBoreholeId = MutableStateFlow<Int?>(null)

    val activeBorehole: StateFlow<Borehole?> = activeBoreholeId.flatMapLatest { id ->
        if (id != null) dao.getBoreholeById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeBoreholeRuns: StateFlow<List<CoreRun>> = activeBoreholeId.flatMapLatest { id ->
        if (id != null) dao.getCoreRunsForBorehole(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeBoreholeDailyLogs: StateFlow<List<DailyDrillingLog>> = activeBoreholeId.flatMapLatest { id ->
        if (id != null) dao.getDailyLogsForBorehole(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setActiveBorehole(id: Int) {
        activeBoreholeId.value = id
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

    fun updateProjectSettings(projectId: Int, mode: String, manualZone: String) {
        viewModelScope.launch {
            val active = activeProject.value
            if (active != null && active.id == projectId) {
                val updated = active.copy(utmZoneMode = mode, utmZoneManual = manualZone)
                dao.insertProject(updated)
            }
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

        val isHighAccuracy = gpsAccuracyMode.value == "HIGH_ACCURACY"
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Battery saver: First attempt to retrieve cached location instantly with ZERO battery usage
        if (!isHighAccuracy) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { cachedLoc ->
                    if (cachedLoc != null && (System.currentTimeMillis() - cachedLoc.time) < 180000) { // Under 3 mins old
                        val activeProj = activeProject.value
                        val forcedZone = if (activeProj?.utmZoneMode == "MANUAL") activeProj.utmZoneManual else null
                        val utm = convertLatLngToUtm(cachedLoc.latitude, cachedLoc.longitude, forcedZone)
                        val elevAcc = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && cachedLoc.hasVerticalAccuracy()) {
                            cachedLoc.verticalAccuracyMeters.toDouble()
                        } else {
                            cachedLoc.accuracy.toDouble() * 1.5
                        }
                        _gpsState.update {
                            it.copy(
                                latitude = cachedLoc.latitude,
                                longitude = cachedLoc.longitude,
                                elevation = cachedLoc.altitude,
                                accuracy = cachedLoc.accuracy.toDouble(),
                                elevationAccuracy = elevAcc,
                                utmEasting = utm.easting,
                                utmNorthing = utm.northing,
                                utmZone = utm.zoneString,
                                isCaptured = true,
                                isLocating = false,
                                source = "GPS (Battery-Saver Instant Cache)"
                            )
                        }
                        return@addOnSuccessListener
                    }
                }
            } catch (e: Exception) {
                // Ignore and proceed to fresh request
            }
        }

        val priority = Priority.PRIORITY_HIGH_ACCURACY

        val cts = CancellationTokenSource()

        if (!isHighAccuracy) {
            // Rapid single-shot high-accuracy query (saves battery by searching up to 15s instead of indefinite)
            viewModelScope.launch {
                delay(15000)
                cts.cancel()
            }
        } else {
            // Absolute Micro-Precision: Allow full robust GNSS carrier phase satellite search up to 45 seconds
            viewModelScope.launch {
                delay(45000)
                cts.cancel()
            }
        }

        fusedLocationClient.getCurrentLocation(
            priority,
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

    fun deleteStation(stationId: Int, projectId: Int) {
        viewModelScope.launch {
            dao.deleteStationById(stationId)
            if (activeStationId.value == stationId) {
                activeStationId.value = null
            }
            navigateTo(Screen.ProjectDashboard(projectId))
        }
    }

    fun updateStation(station: Station) {
        viewModelScope.launch {
            dao.insertStation(station)
            // Trigger state refresh for observation & details UI observers
            val currentId = activeStationId.value
            if (currentId == station.id) {
                activeStationId.value = null
                activeStationId.value = currentId
            }
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
                    width = width
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

                // Save to public Downloads directory for easy access across different devices and versions
                saveFileToPublicDownloads(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

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
                
                Toast.makeText(context, "Excel Log Saved to Public Downloads/GeoFieldPro folder!", Toast.LENGTH_LONG).show()

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

                // Save to public Downloads directory for easy access across different devices and versions
                saveFileToPublicDownloads(context, zipFile, "application/zip")

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

                Toast.makeText(context, "GIS ZIP Archive Saved to Public Downloads/GeoFieldPro folder!", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
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

    // --- BOREHOLE METHODS & DATABASE WORKFLOWS ---
    fun createBorehole(
        name: String,
        lat: Double,
        lon: Double,
        elev: Double,
        acc: Double,
        elevAcc: Double,
        easting: Double,
        northing: Double,
        zone: String,
        startDate: String,
        azimuth: Double?
    ) {
        viewModelScope.launch {
            val borehole = Borehole(
                name = name,
                latitude = lat,
                longitude = lon,
                elevation = elev,
                accuracy = acc,
                elevationAccuracy = elevAcc,
                utmEasting = easting,
                utmNorthing = northing,
                utmZone = zone,
                startDate = startDate,
                azimuth = azimuth
            )
            val newId = dao.insertBorehole(borehole)
            navigateTo(Screen.BoreholeDashboard(newId.toInt()))
        }
    }

    fun deleteBorehole(id: Int) {
        viewModelScope.launch {
            dao.deleteBoreholeById(id)
        }
    }

    fun updateBorehole(borehole: Borehole) {
        viewModelScope.launch {
            dao.insertBorehole(borehole)
        }
    }

    fun saveWindowMapping(windowMapping: WindowMapping) {
        viewModelScope.launch {
            dao.insertWindowMapping(windowMapping)
        }
    }

    fun deleteWindowMapping(id: Int) {
        viewModelScope.launch {
            dao.deleteWindowMappingById(id)
        }
    }

    // --- Map Tracker Actions ---
    fun saveTrack(track: MapTrack) {
        viewModelScope.launch {
            dao.insertTrack(track)
        }
    }

    fun deleteTrack(id: Int) {
        viewModelScope.launch {
            dao.deleteTrackById(id)
        }
    }

    fun clearAllTracks() {
        viewModelScope.launch {
            dao.deleteAllTracks()
        }
    }

    fun saveWaypoint(waypoint: MapWaypoint) {
        viewModelScope.launch {
            dao.insertWaypoint(waypoint)
        }
    }

    fun deleteWaypoint(id: Int) {
        viewModelScope.launch {
            dao.deleteWaypointById(id)
        }
    }

    fun clearAllWaypoints() {
        viewModelScope.launch {
            dao.deleteAllWaypoints()
        }
    }

    fun saveDailyLog(
        logId: Int,
        boreholeId: Int,
        date: String,
        depthDrilled: Double,
        lithologySummary: String,
        operationalNotes: String,
        comments: String
    ) {
        viewModelScope.launch {
            val log = DailyDrillingLog(
                id = logId,
                boreholeId = boreholeId,
                date = date,
                depthDrilled = depthDrilled,
                lithologySummary = lithologySummary,
                operationalNotes = operationalNotes,
                comments = comments
            )
            dao.insertDailyLog(log)
            navigateTo(Screen.BoreholeDashboard(boreholeId))
        }
    }

    fun deleteDailyLog(id: Int, boreholeId: Int) {
        viewModelScope.launch {
            dao.deleteDailyLogById(id)
        }
    }

    fun saveCoreRun(
        runId: Int,
        boreholeId: Int,
        fromDepth: Double,
        toDepth: Double,
        piecesOver10Cm: Double,
        recoveredLength: Double,
        rockType: String,
        lithologyDescription: String,
        alterationType: String,
        alterationIntensity: String,
        mineralizationType: String,
        mineralizationIntensity: String,
        fractureFrequency: Double?,
        veinDensity: Double?,
        jointPresence: String?,
        color: String,
        texture: String,
        grainSize: String,
        notes: String
    ) {
        viewModelScope.launch {
            val run = CoreRun(
                id = runId,
                boreholeId = boreholeId,
                fromDepth = fromDepth,
                toDepth = toDepth,
                piecesOver10Cm = piecesOver10Cm,
                recoveredLength = recoveredLength,
                rockType = rockType,
                lithologyDescription = lithologyDescription,
                alterationType = alterationType,
                alterationIntensity = alterationIntensity,
                mineralizationType = mineralizationType,
                mineralizationIntensity = mineralizationIntensity,
                fractureFrequency = fractureFrequency,
                veinDensity = veinDensity,
                jointPresence = jointPresence,
                color = color,
                texture = texture,
                grainSize = grainSize,
                notes = notes
            )
            dao.insertCoreRun(run)
            navigateTo(Screen.BoreholeDashboard(boreholeId))
        }
    }

    fun deleteCoreRun(id: Int, boreholeId: Int) {
        viewModelScope.launch {
            dao.deleteCoreRunById(id)
        }
    }

    // --- BOREHOLE MULTI-SHEET EXCEL EXPORT ---
    fun exportBoreholeData(context: Context) {
        viewModelScope.launch {
            try {
                val shareData = withContext(Dispatchers.IO) {
                    val boreholesList = dao.getAllBoreholesSync()
                    val coreRunsList = dao.getAllCoreRunsSync()
                    val dailyLogsList = dao.getAllDailyLogsSync()

                    val boreholeMap = boreholesList.associate { it.id to it.name }

                    val excelBoreholes = boreholesList.map {
                        XlsxWriter.BoreholeSheetData(
                            name = it.name,
                            latitude = it.latitude,
                            longitude = it.longitude,
                            elevation = it.elevation,
                            accuracy = it.accuracy,
                            utmEasting = it.utmEasting,
                            utmNorthing = it.utmNorthing,
                            utmZone = it.utmZone,
                            startDate = it.startDate,
                            azimuth = it.azimuth
                        )
                    }

                    val excelDailyLogs = dailyLogsList.map {
                        XlsxWriter.DailyLogSheetData(
                            boreholeName = boreholeMap[it.boreholeId] ?: "BH-${it.boreholeId}",
                            date = it.date,
                            depthDrilled = it.depthDrilled,
                            lithologySummary = it.lithologySummary,
                            operationalNotes = it.operationalNotes,
                            comments = it.comments
                        )
                    }

                    val excelCoreRuns = coreRunsList.map {
                        val runLen = it.getRunLength()
                        XlsxWriter.CoreRunSheetData(
                            boreholeName = boreholeMap[it.boreholeId] ?: "BH-${it.boreholeId}",
                            fromDepth = it.fromDepth,
                            toDepth = it.toDepth,
                            lengthPiecesOver10Cm = it.piecesOver10Cm,
                            recoveredLength = it.recoveredLength,
                            runLength = runLen,
                            rqd = it.getRqd(),
                            coreRecovery = it.getCoreRecovery(),
                            rockType = it.rockType,
                            lithologyDescription = it.lithologyDescription,
                            alterationType = it.alterationType,
                            alterationIntensity = it.alterationIntensity,
                            mineralizationType = it.mineralizationType,
                            mineralizationIntensity = it.mineralizationIntensity,
                            fractureFrequency = it.fractureFrequency,
                            veinDensity = it.veinDensity,
                            jointPresence = it.jointPresence,
                            color = it.color,
                            texture = it.texture,
                            grainSize = it.grainSize,
                            notes = it.notes
                        )
                    }

                    // Create offline Excel file inside safe app compartment
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val excelFileName = "GeoField_Logging_Export_$timestamp.xlsx"
                    val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                    val excelFile = File(downloadsDir, excelFileName)

                    XlsxWriter.generateLoggingXlsxFile(
                        file = excelFile,
                        boreholes = excelBoreholes,
                        dailyLogs = excelDailyLogs,
                        coreRuns = excelCoreRuns
                    )

                    Pair(excelFile, excelFileName)
                }

                val (excelFile, excelFileName) = shareData

                // Save to public Downloads directory for easy access across different devices and versions
                saveFileToPublicDownloads(context, excelFile, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, excelFile)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "GeoField Borehole Export (Excel)")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Share Borehole/Core Logs (Excel)")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "Excel Logs Saved to Public Downloads/GeoFieldPro folder!", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    // --- BOREHOLE ZIPPED CSV EXPORT ---
    fun exportBoreholeZip(context: Context) {
        viewModelScope.launch {
            try {
                val shareData = withContext(Dispatchers.IO) {
                    val boreholesList = dao.getAllBoreholesSync()
                    val coreRunsList = dao.getAllCoreRunsSync()
                    val dailyLogsList = dao.getAllDailyLogsSync()

                    val boreholeMap = boreholesList.associate { it.id to it.name }

                    // Create temp files for CSVs
                    val tempBoreholes = File.createTempFile("boreholes", ".csv", context.cacheDir)
                    val tempDailyLogs = File.createTempFile("daily_logs", ".csv", context.cacheDir)
                    val tempCoreRuns = File.createTempFile("core_runs", ".csv", context.cacheDir)

                    // Write CSV contents
                    tempBoreholes.writeText(
                        toCsvString(
                            listOf("Borehole Name", "Latitude", "Longitude", "Elevation (m)", "Accuracy (m)", "UTM Easting (m)", "UTM Northing (m)", "UTM Zone", "Start Date", "Azimuth (°)"),
                            boreholesList.map { listOf(it.name, it.latitude, it.longitude, it.elevation, it.accuracy, it.utmEasting, it.utmNorthing, it.utmZone, it.startDate, it.azimuth ?: "Vertical") }
                        )
                    )

                    tempDailyLogs.writeText(
                        toCsvString(
                            listOf("Borehole Name", "Date", "Depth Drilled (m)", "Lithology Summary", "Operational Notes", "Comments"),
                            dailyLogsList.map { listOf(boreholeMap[it.boreholeId] ?: "BH-${it.boreholeId}", it.date, it.depthDrilled, it.lithologySummary, it.operationalNotes, it.comments) }
                        )
                    )

                    tempCoreRuns.writeText(
                        toCsvString(
                            listOf("Borehole Name", "From Depth (m)", "To Depth (m)", "Pieces > 10cm (m)", "Recovered Length (m)", "Run Length (m)", "RQD (%)", "Core Recovery (%)", "Rock Type", "Lithology Desc", "Alteration Type", "Alteration Intensity", "Mineralization Type", "Mineralization Intensity", "Fracture Freq (1/m)", "Vein Density (1/m)", "Joint Status", "Color", "Texture", "Grain Size", "Notes"),
                            coreRunsList.map { listOf(boreholeMap[it.boreholeId] ?: "BH-${it.boreholeId}", it.fromDepth, it.toDepth, it.piecesOver10Cm, it.recoveredLength, it.getRunLength(), it.getRqd(), it.getCoreRecovery(), it.rockType, it.lithologyDescription, it.alterationType, it.alterationIntensity, it.mineralizationType, it.mineralizationIntensity, it.fractureFrequency ?: "", it.veinDensity ?: "", it.jointPresence ?: "None", it.color, it.texture, it.grainSize, it.notes) }
                        )
                    )

                    // Create final ZIP inside safe app compartment
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val zipFileName = "GeoField_CoreLogging_$timestamp.zip"
                    val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                    val zipFile = File(downloadsDir, zipFileName)

                    java.util.zip.ZipOutputStream(zipFile.outputStream()).use { zos ->
                        // Add CSVs
                        zos.putNextEntry(java.util.zip.ZipEntry("boreholes.csv"))
                        tempBoreholes.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()

                        zos.putNextEntry(java.util.zip.ZipEntry("daily_logs.csv"))
                        tempDailyLogs.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()

                        zos.putNextEntry(java.util.zip.ZipEntry("core_runs.csv"))
                        tempCoreRuns.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }

                    // Delete temp CSV files
                    tempBoreholes.delete()
                    tempDailyLogs.delete()
                    tempCoreRuns.delete()

                    Pair(zipFile, zipFileName)
                }

                val (zipFile, zipFileName) = shareData

                // Save to public Downloads directory for easy access across different devices and versions
                saveFileToPublicDownloads(context, zipFile, "application/zip")

                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, zipFile)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "GeoField Logging CSV ZIP Package")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Share Logging ZIP CSVs")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "Logging ZIP Saved to Public Downloads/GeoFieldPro folder!", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(context, "ZIP Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    fun exportWindowMappingsCsv(context: Context) {
        viewModelScope.launch {
            try {
                val mappings = dao.getAllWindowMappingsSync()
                if (mappings.isEmpty()) {
                    Toast.makeText(context, "No window mapping data to export!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val fileName = "WINDOW_MAPPING_EXPORT_${System.currentTimeMillis()}.csv"
                val file = java.io.File(dir, fileName)

                withContext(Dispatchers.IO) {
                    file.bufferedWriter().use { writer ->
                        // Header
                        writer.write("ID,Start_X,Start_Y,Start_Z,End_X,End_Y,End_Z,Lithology,Discontinuity_Type,Dip,Dip_Direction,Joint_Set_Numbers,Persistence,Termination,Spacing,Aperture,Surface_Shape,Surface_Roughness,Nature_Of_Filling,Wavelength,Wave_Amplitude,JRC,JCS,Weathering_Condition,Water_Flow,Sample_No,Photo_Path,Created_At\n")
                        
                        mappings.forEach { m ->
                            val line = listOf(
                                m.id,
                                m.startX, m.startY, m.startZ,
                                m.endX, m.endY, m.endZ,
                                "\"${m.lithology.replace("\"", "\"\"")}\"",
                                "\"${m.discontinuityType.replace("\"", "\"\"")}\"",
                                m.dip,
                                m.dipDirection,
                                "\"${m.jointSetNumbers.replace("\"", "\"\"")}\"",
                                "\"${m.persistence.replace("\"", "\"\"")}\"",
                                "\"${m.termination.replace("\"", "\"\"")}\"",
                                m.spacing,
                                m.aperture,
                                "\"${m.surfaceShape.replace("\"", "\"\"")}\"",
                                "\"${m.surfaceRoughness.replace("\"", "\"\"")}\"",
                                "\"${m.natureOfFilling.replace("\"", "\"\"")}\"",
                                m.wavelength,
                                m.waveAmplitude,
                                m.jrc,
                                m.jcs,
                                "\"${m.weatheringCondition.replace("\"", "\"\"")}\"",
                                "\"${m.waterFlow.replace("\"", "\"\"")}\"",
                                "\"${m.sampleNo.replace("\"", "\"\"")}\"",
                                "\"${(m.photoPath ?: "").replace("\"", "\"\"")}\"",
                                m.createdAt
                            ).joinToString(",")
                            writer.write(line + "\n")
                        }
                    }
                }

                // Save to public Downloads directory for easy access across different devices and versions
                saveFileToPublicDownloads(context, file, "text/csv")

                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "GeoField Window Mapping Export (CSV)")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Share Window Mapping (CSV)")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                Toast.makeText(context, "CSV Export Saved to Public Downloads/GeoFieldPro folder!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun saveFileToPublicDownloads(context: Context, srcFile: File, mimeType: String): Uri? {
        val resolver = context.contentResolver
        val fileName = srcFile.name
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/GeoFieldPro")
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        srcFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    return uri
                }
            } else {
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(publicDir, "GeoFieldPro")
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                val targetFile = File(targetDir, fileName)
                targetFile.outputStream().use { out ->
                    srcFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                return Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun exportRoseDiagramFiles(context: Context, strikes: List<Float>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Draw Rose Diagram Bitmap
                val bmp = PhotoProcessor.drawRoseDiagramBitmap(strikes)
                
                // Save PNG temporarily
                val tempPngFile = java.io.File(context.cacheDir, "Rose_Diagram_${System.currentTimeMillis()}.png")
                java.io.FileOutputStream(tempPngFile).use { out ->
                    bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
                
                // 2. Export PDF
                val pdfPath = PhotoProcessor.exportRoseDiagramPdf(context, activeProject.value?.name ?: "Unnamed Project", strikes)
                val tempPdfFile = pdfPath?.let { java.io.File(it) }
                
                // 3. Save to Public downloads
                val pngUri = saveFileToPublicDownloads(context, tempPngFile, "image/png")
                val pdfUri = tempPdfFile?.let { saveFileToPublicDownloads(context, it, "application/pdf") }
                
                withContext(Dispatchers.Main) {
                    if (pngUri != null && pdfUri != null && tempPdfFile != null) {
                        Toast.makeText(context, "Rose Diagram Image & PDF Saved to Public Downloads/GeoFieldPro folder!", Toast.LENGTH_LONG).show()
                        
                        // Launch share sheet for PDF Document!
                        val authority = "${context.packageName}.fileprovider"
                        val pdfSharedUri = androidx.core.content.FileProvider.getUriForFile(context, authority, tempPdfFile)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, pdfSharedUri)
                            putExtra(Intent.EXTRA_SUBJECT, "Geological Strike Rose Diagram Analysis Report")
                            putExtra(Intent.EXTRA_TEXT, "Discontinuities Strike Rose Diagram Analysis Report. Logged with GeoFieldPro.")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        
                        val chooser = Intent.createChooser(shareIntent, "Share Rose Diagram PDF Report")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                    } else {
                        Toast.makeText(context, "Failed to save export files.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }
    }
}
