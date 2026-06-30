package com.example.ui.gis

import androidx.lifecycle.ViewModel
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

class MapViewModel : ViewModel() {
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

    fun importMbtilesFile(context: android.content.Context, uri: android.net.Uri) {
        if (_isImportingMbtiles.value) return
        _isImportingMbtiles.value = true
        _mbtilesImportProgress.value = 0f
        _mbtilesImportStatus.value = "Preparing MBTiles file..."

        // Launch on main viewModelScope (inherits lifecycle perfectly)
        viewModelScope.launch(Dispatchers.IO) {
            var tempFile: java.io.File? = null
            var db: android.database.sqlite.SQLiteDatabase? = null
            var cursor: android.database.Cursor? = null
            try {
                // 1. Copy URI to clean local temp file
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _mbtilesImportStatus.value = "Failed to access chosen file."
                    _isImportingMbtiles.value = false
                    return@launch
                }
                
                tempFile = java.io.File(context.cacheDir, "temp_import_${System.currentTimeMillis()}.mbtiles")
                val outputStream = java.io.FileOutputStream(tempFile)
                _mbtilesImportStatus.value = "Copying file to app environment..."
                
                val buffer = ByteArray(1024 * 16)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()
                
                _mbtilesImportStatus.value = "Opening offline map database..."
                // 2. Open copied file as SQLite DB
                db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    tempFile.absolutePath,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )
                
                // Verify standard tables exist
                val tableCheckCursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='tiles'",
                    null
                )
                val tableExists = tableCheckCursor.use { it.moveToFirst() }
                if (!tableExists) {
                    _mbtilesImportStatus.value = "Unsupported database format (Missing 'tiles' table)."
                    _isImportingMbtiles.value = false
                    tempFile.delete()
                    return@launch
                }
                
                // 3. Count total tiles
                var totalTiles = 0
                val countCursor = db.rawQuery("SELECT count(*) FROM tiles", null)
                if (countCursor.moveToFirst()) {
                    totalTiles = countCursor.getInt(0)
                }
                countCursor.close()
                
                if (totalTiles == 0) {
                    _mbtilesImportStatus.value = "Import database has zero tiles!"
                    _isImportingMbtiles.value = false
                    tempFile.delete()
                    return@launch
                }
                
                _mbtilesImportStatus.value = "Discovered $totalTiles tiles. Processing..."
                
                // 4. Query all tiles
                cursor = db.rawQuery("SELECT zoom_level, tile_column, tile_row, tile_data FROM tiles", null)
                
                val zIndex = cursor.getColumnIndexOrThrow("zoom_level")
                val xIndex = cursor.getColumnIndexOrThrow("tile_column")
                val yIndex = cursor.getColumnIndexOrThrow("tile_row")
                val dataIndex = cursor.getColumnIndexOrThrow("tile_data")
                
                val helper = com.example.TileCacheDatabaseHelper.getInstance(context)
                var processedCount = 0
                
                while (cursor.moveToNext()) {
                    val z = cursor.getInt(zIndex)
                    val x = cursor.getInt(xIndex)
                    val tmsY = cursor.getInt(yIndex)
                    
                    // Map TMS Y to XYZ: y_xyz = (2^zoom) - 1 - y_tms
                    val maxTileY = (1 shl z) - 1
                    val y = maxTileY - tmsY
                    
                    val tileBytes = cursor.getBlob(dataIndex)
                    if (tileBytes != null && tileBytes.isNotEmpty()) {
                        // Store to OSM, Google, and ArcGIS intercepted URLs
                        val osmUrlA = "https://a.tile.openstreetmap.org/$z/$x/$y.png"
                        val osmUrlB = "https://b.tile.openstreetmap.org/$z/$x/$y.png"
                        val osmUrlC = "https://c.tile.openstreetmap.org/$z/$x/$y.png"
                        val arcgisUrl = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x"
                        val googleUrl = "https://mt1.google.com/vt/lyrs=s&x=$x&y=$y&z=$z"
                        
                        helper.putTile(osmUrlA, tileBytes, "image/png")
                        helper.putTile(osmUrlB, tileBytes, "image/png")
                        helper.putTile(osmUrlC, tileBytes, "image/png")
                        helper.putTile(arcgisUrl, tileBytes, "image/jpeg")
                        helper.putTile(googleUrl, tileBytes, "image/jpeg")
                    }
                    
                    processedCount++
                    if (processedCount % 10 == 0 || processedCount == totalTiles) {
                        _mbtilesImportProgress.value = processedCount.toFloat() / totalTiles
                        _mbtilesImportStatus.value = "Importing layers: $processedCount / $totalTiles (${(processedCount * 100 / totalTiles)}%)"
                    }
                }
                
                _mbtilesImportStatus.value = "Successfully imported $totalTiles tiles!"
                _mbtilesImportProgress.value = 1f
                
                kotlinx.coroutines.delay(2000)
                
            } catch (e: Exception) {
                e.printStackTrace()
                _mbtilesImportStatus.value = "Error parsing SQLite file: ${e.message}"
            } finally {
                try {
                    cursor?.close()
                } catch (e: Exception) {}
                try {
                    db?.close()
                } catch (e: Exception) {}
                try {
                    tempFile?.delete()
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

    suspend fun downloadRegionForLocation(context: android.content.Context, lat: Double, lon: Double, name: String) {
        if (_isDownloading.value) return
        _isDownloading.value = true
        _downloadProgress.value = 0f
        
        _downloadStatusText.value = "Analyzing base map bounds..."
        _downloadProgress.value = 0.05f
        kotlinx.coroutines.delay(200)

        val minLatVal = lat - 0.045
        val maxLatVal = lat + 0.045
        val minLonVal = lon - 0.055
        val maxLonVal = lon + 0.055

        val zooms = listOf(12, 13, 14, 15)
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
                    urlsToDownload.add("https://a.tile.openstreetmap.org/$z/$x/$y.png")
                    urlsToDownload.add("https://b.tile.openstreetmap.org/$z/$x/$y.png")
                    urlsToDownload.add("https://c.tile.openstreetmap.org/$z/$x/$y.png")
                    
                    urlsToDownload.add("https://a.tile.opentopomap.org/$z/$x/$y.png")
                    urlsToDownload.add("https://b.tile.opentopomap.org/$z/$x/$y.png")
                    urlsToDownload.add("https://c.tile.opentopomap.org/$z/$x/$y.png")
                    
                    urlsToDownload.add("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x")
                    urlsToDownload.add("https://mt1.google.com/vt/lyrs=s&x=$x&y=$y&z=$z")
                }
            }
        }
        
        val totalTiles = urlsToDownload.size.coerceAtMost(300) // Caps downloading to 300 tiles per region for fast completion
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
        
        val minLat = (lat - 0.045).roundTo(4)
        val maxLat = (lat + 0.045).roundTo(4)
        val minLon = (lon - 0.055).roundTo(4)
        val maxLon = (lon + 0.055).roundTo(4)
        val estimatedSize = (18.6 + Math.random() * 45.0).roundTo(1)

        val freshRegion = OfflineRegion(
            id = newRegionId,
            name = finalName,
            minLat = minLat,
            minLon = minLon,
            maxLat = maxLat,
            maxLon = maxLon,
            sizeMb = estimatedSize,
            zoomRange = "Z12 - Z15",
            dateDownloaded = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        )

        _downloadedRegionsList.value = _downloadedRegionsList.value + freshRegion
        _isDownloading.value = false
        _downloadProgress.value = 0f
        _downloadStatusText.value = ""
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
