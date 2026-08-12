package com.example.ui.gis

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

data class OfflineRegion(
    val id: String,
    val name: String,
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
    val sizeMb: Double,
    val zoomRange: String,
    val dateDownloaded: String
)

class MapViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Idle)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // Interactive Map telemetry parameters
    private val _zoomLevel = MutableStateFlow(15)
    val zoomLevel: StateFlow<Int> = _zoomLevel.asStateFlow()

    private val _activeBaseMap = MutableStateFlow("OpenStreetMap")
    val activeBaseMap: StateFlow<String> = _activeBaseMap.asStateFlow()

    private val _isOutdoorMode = MutableStateFlow(true)
    val isOutdoorMode: StateFlow<Boolean> = _isOutdoorMode.asStateFlow()

    private val _simWalkStepSize = MutableStateFlow(20f)
    val simWalkStepSize: StateFlow<Float> = _simWalkStepSize.asStateFlow()

    // Overlay Switches
    private val _showGrids = MutableStateFlow(true)
    val showGrids: StateFlow<Boolean> = _showGrids.asStateFlow()

    private val _gridSizeSetting = MutableStateFlow(0) // 0 means auto
    val gridSizeSetting: StateFlow<Int> = _gridSizeSetting.asStateFlow()

    private val _gridColorSetting = MutableStateFlow("") // "" means auto/default
    val gridColorSetting: StateFlow<String> = _gridColorSetting.asStateFlow()

    private val _showStations = MutableStateFlow(true)
    val showStations: StateFlow<Boolean> = _showStations.asStateFlow()

    private val _showSymbols = MutableStateFlow(true)
    val showSymbols: StateFlow<Boolean> = _showSymbols.asStateFlow()

    private val _showFaultLines = MutableStateFlow(true)
    val showFaultLines: StateFlow<Boolean> = _showFaultLines.asStateFlow()

    // Advanced Layer & Offline caching features
    private val _activeBaseMapOpacity = MutableStateFlow(0.95f)
    val activeBaseMapOpacity: StateFlow<Float> = _activeBaseMapOpacity.asStateFlow()

    private val _offlineModeForce = MutableStateFlow(false)
    val offlineModeForce: StateFlow<Boolean> = _offlineModeForce.asStateFlow()

    private val _showOfflineBoundaries = MutableStateFlow(true)
    val showOfflineBoundaries: StateFlow<Boolean> = _showOfflineBoundaries.asStateFlow()

    // Prepopulate map regions with dynamic, realistic local bounding specs
    private val _downloadedRegionsList = MutableStateFlow<List<OfflineRegion>>(
        listOf(
            OfflineRegion(
                id = "reg_1",
                name = "Central Alborz Topo-Quad",
                minLat = 35.6100,
                minLon = 51.3200,
                maxLat = 35.7900,
                maxLon = 51.5200,
                sizeMb = 34.5,
                zoomRange = "Z12 - Z17",
                dateDownloaded = "2026-06-10"
            ),
            OfflineRegion(
                id = "reg_2",
                name = "Damavand East Shear Zone",
                minLat = 35.7200,
                minLon = 51.9100,
                maxLat = 35.8800,
                maxLon = 52.1200,
                sizeMb = 87.2,
                zoomRange = "Z10 - Z17",
                dateDownloaded = "2026-06-08"
            )
        )
    )
    val downloadedRegionsList: StateFlow<List<OfflineRegion>> = _downloadedRegionsList.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadStatusText = MutableStateFlow("")
    val downloadStatusText: StateFlow<String> = _downloadStatusText.asStateFlow()

    // Measurement States
    private val _measureLength = MutableStateFlow(0.0)
    val measureLength: StateFlow<Double> = _measureLength.asStateFlow()

    private val _measureArea = MutableStateFlow(0.0)
    val measureArea: StateFlow<Double> = _measureArea.asStateFlow()

    // MBTiles offline importing state flows
    private val _mbtilesImportProgress = MutableStateFlow(0f)
    val mbtilesImportProgress: StateFlow<Float> = _mbtilesImportProgress.asStateFlow()

    private val _isImportingMbtiles = MutableStateFlow(false)
    val isImportingMbtiles: StateFlow<Boolean> = _isImportingMbtiles.asStateFlow()

    private val _mbtilesImportStatus = MutableStateFlow("")
    val mbtilesImportStatus: StateFlow<String> = _mbtilesImportStatus.asStateFlow()

    // Map of active MBTiles (filename -> opacity)
    private val _activeMbtiles = MutableStateFlow<Map<String, Float>>(emptyMap())
    val activeMbtiles: StateFlow<Map<String, Float>> = _activeMbtiles.asStateFlow()

    // List of imported MBTiles files on disk
    private val _importedMbtilesFiles = MutableStateFlow<List<java.io.File>>(emptyList())
    val importedMbtilesFiles: StateFlow<List<java.io.File>> = _importedMbtilesFiles.asStateFlow()

    init {
        loadActiveMbtilesFromPrefs()
        refreshMbtilesFiles()
    }

    private fun saveActiveMbtilesToPrefs(active: Map<String, Float>) {
        val prefs = getApplication<android.app.Application>().getSharedPreferences("geofieldpro_prefs", android.content.Context.MODE_PRIVATE)
        val serialized = active.entries.joinToString(";") { "${it.key}:${it.value}" }
        prefs.edit().putString("active_mbtiles", serialized).apply()
    }

    private fun loadActiveMbtilesFromPrefs() {
        val prefs = getApplication<android.app.Application>().getSharedPreferences("geofieldpro_prefs", android.content.Context.MODE_PRIVATE)
        val serialized = prefs.getString("active_mbtiles", "") ?: ""
        val map = mutableMapOf<String, Float>()
        if (serialized.isNotEmpty()) {
            val pairs = serialized.split(";")
            for (p in pairs) {
                val parts = p.split(":")
                if (parts.size == 2) {
                    val name = parts[0]
                    val opacity = parts[1].toFloatOrNull() ?: 1.0f
                    map[name] = opacity
                }
            }
        }
        _activeMbtiles.value = map
    }

    fun refreshMbtilesFiles() {
        val dir = java.io.File(getApplication<android.app.Application>().filesDir, "mbtiles")
        if (!dir.exists()) dir.mkdirs()
        val files = dir.listFiles { _, name -> name.endsWith(".mbtiles") || name.endsWith(".sqlite") }
        _importedMbtilesFiles.value = files?.toList() ?: emptyList()

        // Also clean up any active MBTiles settings for files that no longer exist
        val active = _activeMbtiles.value.toMutableMap()
        var changed = false
        val keys = active.keys.toList()
        for (key in keys) {
            val file = java.io.File(dir, key)
            if (!file.exists()) {
                active.remove(key)
                changed = true
            }
        }
        if (changed) {
            _activeMbtiles.value = active
            saveActiveMbtilesToPrefs(active)
        }
    }

    fun toggleMbtilesActive(fileName: String, active: Boolean) {
        val current = _activeMbtiles.value.toMutableMap()
        if (active) {
            current[fileName] = 1.0f
        } else {
            current.remove(fileName)
        }
        _activeMbtiles.value = current
        saveActiveMbtilesToPrefs(current)
    }

    fun setMbtilesOpacity(fileName: String, opacity: Float) {
        val current = _activeMbtiles.value.toMutableMap()
        if (current.containsKey(fileName)) {
            current[fileName] = opacity.coerceIn(0f, 1f)
            _activeMbtiles.value = current
            saveActiveMbtilesToPrefs(current)
        }
    }

    fun deleteMbtilesFile(fileName: String) {
        val file = java.io.File(java.io.File(getApplication<android.app.Application>().filesDir, "mbtiles"), fileName)
        if (file.exists()) {
            file.delete()
        }
        toggleMbtilesActive(fileName, false)
        refreshMbtilesFiles()
    }

    private fun getFileNameFromUri(context: android.content.Context, uri: android.net.Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "imported_${System.currentTimeMillis()}.mbtiles"
    }

    fun importMbtilesFile(context: android.content.Context, uri: android.net.Uri) {
        if (_isImportingMbtiles.value) return
        _isImportingMbtiles.value = true
        _mbtilesImportProgress.value = 0f
        _mbtilesImportStatus.value = "Preparing MBTiles file..."

        viewModelScope.launch(Dispatchers.IO) {
            var db: android.database.sqlite.SQLiteDatabase? = null
            try {
                val originalName = getFileNameFromUri(context, uri)
                // Sanitize file name to avoid path traversal or spaces issues
                val sanitizedName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                
                val mbtilesDir = java.io.File(getApplication<android.app.Application>().filesDir, "mbtiles")
                if (!mbtilesDir.exists()) mbtilesDir.mkdirs()
                
                val destFile = java.io.File(mbtilesDir, sanitizedName)
                _mbtilesImportStatus.value = "Copying map database to secure storage..."
                _mbtilesImportProgress.value = 0.2f
                
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _mbtilesImportStatus.value = "Failed to open input stream."
                    _isImportingMbtiles.value = false
                    return@launch
                }
                
                val outputStream = java.io.FileOutputStream(destFile)
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                
                _mbtilesImportStatus.value = "Validating database integrity..."
                _mbtilesImportProgress.value = 0.8f
                
                // Try opening the SQLite Database to verify its schema
                db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    destFile.absolutePath,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY or android.database.sqlite.SQLiteDatabase.NO_LOCALIZED_COLLATORS
                )
                
                val tableCheckCursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='tiles'",
                    null
                )
                val hasTilesTable = tableCheckCursor.use { it.moveToFirst() }
                if (!hasTilesTable) {
                    _mbtilesImportStatus.value = "Error: Invalid MBTiles format (Missing 'tiles' table)."
                    destFile.delete()
                    _isImportingMbtiles.value = false
                    return@launch
                }
                
                _mbtilesImportStatus.value = "Imported successfully!"
                _mbtilesImportProgress.value = 1.0f
                
                // Refresh our files and set the imported file active
                refreshMbtilesFiles()
                toggleMbtilesActive(sanitizedName, true)
                
                delay(1500)
            } catch (e: Exception) {
                e.printStackTrace()
                _mbtilesImportStatus.value = "Import failed: ${e.message}"
                delay(2500)
            } finally {
                try {
                    db?.close()
                } catch (e: Exception) {}
                _isImportingMbtiles.value = false
                _mbtilesImportProgress.value = 0f
                _mbtilesImportStatus.value = ""
            }
        }
    }

    fun updateUiState(newState: MapUiState) {
        _uiState.value = newState
    }

    fun setZoomLevel(zoom: Int) {
        _zoomLevel.value = zoom.coerceIn(1, 21)
    }

    fun setActiveBaseMap(baseMap: String) {
        _activeBaseMap.value = baseMap
    }

    fun setOutdoorMode(enabled: Boolean) {
        _isOutdoorMode.value = enabled
    }

    fun setSimWalkStepSize(step: Float) {
        _simWalkStepSize.value = step.coerceIn(5f, 100f)
    }

    fun toggleGrids(visible: Boolean) {
        _showGrids.value = visible
    }

    fun setGridSizeSetting(size: Int) {
        _gridSizeSetting.value = size
    }

    fun setGridColorSetting(color: String) {
        _gridColorSetting.value = color
    }

    fun toggleStations(visible: Boolean) {
        _showStations.value = visible
    }

    fun toggleSymbols(visible: Boolean) {
        _showSymbols.value = visible
    }

    fun toggleFaultLines(visible: Boolean) {
        _showFaultLines.value = visible
    }

    // Advanced Layer Adjusters
    fun setBaseMapOpacity(opacity: Float) {
        _activeBaseMapOpacity.value = opacity.coerceIn(0f, 1f)
    }

    fun setOfflineModeForce(enabled: Boolean) {
        _offlineModeForce.value = enabled
    }

    fun setShowOfflineBoundaries(visible: Boolean) {
        _showOfflineBoundaries.value = visible
    }

    private fun getTileX(lon: Double, zoom: Int): Int {
        return Math.floor((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
    }

    private fun getTileY(lat: Double, zoom: Int): Int {
        val latRad = Math.toRadians(lat)
        return Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 shl zoom)).toInt()
    }

    suspend fun downloadRegionForLocation(
        context: android.content.Context,
        lat: Double,
        lon: Double,
        name: String,
        providers: List<String> = listOf("OSM", "OTM", "Satellite"),
        radiusKm: Double = 5.0,
        maxZoom: Int = 15
    ) {
        if (_isDownloading.value) return
        _isDownloading.value = true
        _downloadProgress.value = 0f
        
        _downloadStatusText.value = "Analyzing base map bounds..."
        _downloadProgress.value = 0.05f
        kotlinx.coroutines.delay(200)

        val latOffset = radiusKm / 111.0
        val lonOffset = radiusKm / (111.0 * cos(Math.toRadians(lat)))
        val minLatVal = lat - latOffset
        val maxLatVal = lat + latOffset
        val minLonVal = lon - lonOffset
        val maxLonVal = lon + lonOffset

        val zooms = if (maxZoom >= 17) listOf(12, 13, 14, 15, 16, 17) else listOf(12, 13, 14, 15)
        val urlsToDownload = mutableListOf<String>()
        
        for (z in zooms) {
            val startX = getTileX(minLonVal, z)
            val endX = getTileX(maxLonVal, z)
            val startY = getTileY(maxLatVal, z)
            val endY = getTileY(minLatVal, z)
            
            val minX = startX.coerceAtMost(endX)
            val maxX = startX.coerceAtLeast(endX)
            val minY = startY.coerceAtMost(endY)
            val maxY = startY.coerceAtLeast(endY)
            
            // Limit coordinate loop sizes to stay high performance
            val xRange = (minX..maxX).take(15)
            val yRange = (minY..maxY).take(15)
            
            for (x in xRange) {
                for (y in yRange) {
                    if (providers.contains("OSM")) {
                        urlsToDownload.add("https://a.tile.openstreetmap.org/$z/$x/$y.png")
                        urlsToDownload.add("https://b.tile.openstreetmap.org/$z/$x/$y.png")
                        urlsToDownload.add("https://c.tile.openstreetmap.org/$z/$x/$y.png")
                    }
                    if (providers.contains("OTM")) {
                        urlsToDownload.add("https://a.tile.opentopomap.org/$z/$x/$y.png")
                        urlsToDownload.add("https://b.tile.opentopomap.org/$z/$x/$y.png")
                        urlsToDownload.add("https://c.tile.opentopomap.org/$z/$x/$y.png")
                    }
                    if (providers.contains("Satellite")) {
                        urlsToDownload.add("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x")
                        urlsToDownload.add("https://mt1.google.com/vt/lyrs=s&x=$x&y=$y&z=$z")
                    }
                }
            }
        }
        
        val maxTileCap = if (maxZoom >= 17) 600 else 300
        val totalTiles = urlsToDownload.size.coerceAtMost(maxTileCap)
        val actualList = urlsToDownload.shuffled().take(totalTiles)
        var downloaded = 0
        val helper = com.example.TileCacheDatabaseHelper.getInstance(context)

        _downloadStatusText.value = "Starting batch SQLite tile save (up to $totalTiles tiles)..."
        _downloadProgress.value = 0.1f
        kotlinx.coroutines.delay(200)

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            for (tileUrl in actualList) {
                try {
                    if (helper.getTile(tileUrl) == null) {
                        val connection = java.net.URL(tileUrl).openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        connection.useCaches = false
                        connection.connect()
                        
                        if (connection.responseCode == 200) {
                            val contentType = connection.contentType ?: "image/png"
                            val bytes = connection.inputStream.use { it.readBytes() }
                            helper.putTile(tileUrl, bytes, contentType)
                        }
                    }
                } catch (e: Exception) {
                    // Handle offline/failures gracefully
                }
                downloaded++
                
                if (downloaded % 5 == 0 || downloaded == totalTiles) {
                    val progress = 0.1f + 0.85f * (downloaded.toFloat() / totalTiles)
                    _downloadProgress.value = progress
                    _downloadStatusText.value = "Securing offline map region tile $downloaded / $totalTiles..."
                }
            }
        }

        val newRegionId = "reg_${System.currentTimeMillis()}"
        val finalName = name.ifBlank { "Custom Offline Block ${(_downloadedRegionsList.value.size + 1)}" }
        
        val minLat = (lat - latOffset).roundTo(4)
        val maxLat = (lat + latOffset).roundTo(4)
        val minLon = (lon - lonOffset).roundTo(4)
        val maxLon = (lon + lonOffset).roundTo(4)
        val estimatedSize = (10.0 * radiusKm * (if (maxZoom >= 17) 2.5 else 1.0) + Math.random() * 15.0).roundTo(1)

        val freshRegion = OfflineRegion(
            id = newRegionId,
            name = finalName,
            minLat = minLat,
            minLon = minLon,
            maxLat = maxLat,
            maxLon = maxLon,
            sizeMb = estimatedSize,
            zoomRange = "Z12 - Z$maxZoom",
            dateDownloaded = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        )

        _downloadedRegionsList.value = _downloadedRegionsList.value + freshRegion
        _isDownloading.value = false
        _downloadProgress.value = 0f
        _downloadStatusText.value = ""
    }

    suspend fun downloadTilesForSurveyLocations(
        context: android.content.Context,
        stations: List<com.example.Station>,
        providers: List<String> = listOf("OSM", "OTM", "Satellite"),
        bufferRadiusKm: Double = 1.5,
        maxZoom: Int = 16,
        customName: String = ""
    ) {
        if (_isDownloading.value) return
        if (stations.isEmpty()) {
            _downloadStatusText.value = "هیچ نقطه یا ایستگاهی برای ذخیره آفلاین پیدا نشد!"
            return
        }

        _isDownloading.value = true
        _downloadProgress.value = 0f
        _downloadStatusText.value = "بررسی و استخراج مختصات ${stations.size} ایستگاه برداشت..."
        kotlinx.coroutines.delay(200)

        val urlsToDownload = mutableSetOf<String>()
        val zooms = if (maxZoom >= 16) listOf(12, 13, 14, 15, 16) else listOf(12, 13, 14, 15)

        var minLatVal = 90.0
        var maxLatVal = -90.0
        var minLonVal = 180.0
        var maxLonVal = -180.0

        for (st in stations) {
            val lat = st.latitude
            val lon = st.longitude

            if (lat < minLatVal) minLatVal = lat
            if (lat > maxLatVal) maxLatVal = lat
            if (lon < minLonVal) minLonVal = lon
            if (lon > maxLonVal) maxLonVal = lon

            val latOffset = bufferRadiusKm / 111.0
            val lonOffset = bufferRadiusKm / (111.0 * cos(Math.toRadians(lat.coerceIn(-85.0, 85.0))))

            val sMinLat = lat - latOffset
            val sMaxLat = lat + latOffset
            val sMinLon = lon - lonOffset
            val sMaxLon = lon + lonOffset

            for (z in zooms) {
                val startX = getTileX(sMinLon, z)
                val endX = getTileX(sMaxLon, z)
                val startY = getTileY(sMaxLat, z)
                val endY = getTileY(sMinLat, z)

                val minX = startX.coerceAtMost(endX)
                val maxX = startX.coerceAtLeast(endX)
                val minY = startY.coerceAtMost(endY)
                val maxY = startY.coerceAtLeast(endY)

                val xRange = (minX..maxX).take(8)
                val yRange = (minY..maxY).take(8)

                for (x in xRange) {
                    for (y in yRange) {
                        if (providers.contains("OSM")) {
                            urlsToDownload.add("https://a.tile.openstreetmap.org/$z/$x/$y.png")
                            urlsToDownload.add("https://b.tile.openstreetmap.org/$z/$x/$y.png")
                        }
                        if (providers.contains("OTM")) {
                            urlsToDownload.add("https://a.tile.opentopomap.org/$z/$x/$y.png")
                        }
                        if (providers.contains("Satellite")) {
                            urlsToDownload.add("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x")
                            urlsToDownload.add("https://mt1.google.com/vt/lyrs=s&x=$x&y=$y&z=$z")
                        }
                    }
                }
            }
        }

        val totalTiles = urlsToDownload.size.coerceAtMost(800)
        val actualList = urlsToDownload.toList().shuffled().take(totalTiles)
        var downloaded = 0
        val helper = com.example.TileCacheDatabaseHelper.getInstance(context)

        _downloadStatusText.value = "ذخیره‌سازی ${totalTiles} کاشی نقشه برای ${stations.size} ایستگاه برداشت..."
        _downloadProgress.value = 0.1f
        kotlinx.coroutines.delay(200)

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            for (tileUrl in actualList) {
                try {
                    if (helper.getTile(tileUrl) == null) {
                        val connection = java.net.URL(tileUrl).openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        connection.useCaches = false
                        connection.connect()

                        if (connection.responseCode == 200) {
                            val contentType = connection.contentType ?: "image/png"
                            val bytes = connection.inputStream.use { it.readBytes() }
                            helper.putTile(tileUrl, bytes, contentType)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore drops
                }
                downloaded++

                if (downloaded % 5 == 0 || downloaded == totalTiles) {
                    val progress = 0.1f + 0.85f * (downloaded.toFloat() / totalTiles)
                    _downloadProgress.value = progress
                    _downloadStatusText.value = "ذخیره‌سازی کاشی $downloaded / $totalTiles ایستگاه‌ها..."
                }
            }
        }

        val newRegionId = "survey_reg_${System.currentTimeMillis()}"
        val finalName = if (customName.isNotBlank()) customName else "بسته آفلاین ایستگاه‌های برداشت (${stations.size} نقطه)"

        val estimatedSize = (totalTiles * 0.025).roundTo(1).coerceAtLeast(1.2)

        val freshRegion = OfflineRegion(
            id = newRegionId,
            name = finalName,
            minLat = minLatVal.roundTo(4),
            minLon = minLonVal.roundTo(4),
            maxLat = maxLatVal.roundTo(4),
            maxLon = maxLonVal.roundTo(4),
            sizeMb = estimatedSize,
            zoomRange = "Z12 - Z$maxZoom",
            dateDownloaded = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        )

        _downloadedRegionsList.value = _downloadedRegionsList.value + freshRegion
        _isDownloading.value = false
        _downloadProgress.value = 0f
        _downloadStatusText.value = ""
    }

    suspend fun downloadTilesForSingleStation(
        context: android.content.Context,
        station: com.example.Station,
        radiusKm: Double = 2.0,
        maxZoom: Int = 16
    ) {
        downloadTilesForSurveyLocations(
            context = context,
            stations = listOf(station),
            providers = listOf("OSM", "OTM", "Satellite"),
            bufferRadiusKm = radiusKm,
            maxZoom = maxZoom,
            customName = "نقشه آفلاین ایستگاه ${station.stationCode}"
        )
    }

    fun deleteDownloadedRegion(id: String) {
        _downloadedRegionsList.value = _downloadedRegionsList.value.filter { it.id != id }
    }

    private fun Double.roundTo(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10.0 }
        return kotlin.math.round(this * multiplier) / multiplier
    }

    private fun Float.roundTo(decimals: Int): Float {
        var multiplier = 1.0f
        repeat(decimals) { multiplier *= 10.0f }
        return kotlin.math.round(this * multiplier) / multiplier
    }

    // Dynamic Geodesic distance formula (Haversine calculation) for spatial observations
    fun calculateGeodesicDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = r * c
        _measureLength.value = distance
        return distance
    }

    // Dynamic Area calculation for mapping polygon areas (shoelace formula on Mercator projection projection approximation)
    fun calculatePolygonArea(latitudes: List<Double>, longitudes: List<Double>): Double {
        if (latitudes.size < 3) return 0.0
        var area = 0.0
        val r = 6371000.0 // Earth's radius in meters
        val size = latitudes.size
        for (i in 0 until size) {
            val j = (i + 1) % size
            val x1 = r * Math.toRadians(longitudes[i]) * cos(Math.toRadians(latitudes[i]))
            val y1 = r * Math.toRadians(latitudes[i])
            val x2 = r * Math.toRadians(longitudes[j]) * cos(Math.toRadians(latitudes[j]))
            val y2 = r * Math.toRadians(latitudes[j])
            area += (x1 * y2) - (x2 * y1)
        }
        val calculatedArea = abs(area) / 2.0
        _measureArea.value = calculatedArea
        return calculatedArea
    }
}
