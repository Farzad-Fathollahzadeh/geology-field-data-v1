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
            
            val activeProj = activeProject.value
            val forcedZone = if (activeProj?.utmZoneMode == "MANUAL") activeProj.utmZoneManual else null
            val utm = convertLatLngToUtm(simLat, simLon, forcedZone)

            _gpsState.update {
                it.copy(
                    latitude = simLat,
                    longitude = simLon,
                    elevation = simElev,
                    accuracy = simAcc,
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
                _gpsState.update {
                    it.copy(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        elevation = location.altitude,
                        accuracy = location.accuracy.toDouble(),
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
                            listOf("Station Code", "Timestamp", "Structure Type", "Strike (deg)", "Dip (deg)", "Movement", "Width (cm)", "Notes", "Has Sample?", "Photos"),
                            excelFaults.map { listOf(it.stationCode, it.timestamp, it.structureType, it.strike ?: "", it.dip ?: "", it.movement ?: "", it.width ?: "", it.notes ?: "", it.hasSample, it.photos) }
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
}
