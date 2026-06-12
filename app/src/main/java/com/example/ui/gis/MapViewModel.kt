package com.example.ui.gis

import androidx.lifecycle.ViewModel
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
    private val _zoomLevel = MutableStateFlow(4)
    val zoomLevel: StateFlow<Int> = _zoomLevel.asStateFlow()

    private val _activeBaseMap = MutableStateFlow("Google Satellite")
    val activeBaseMap: StateFlow<String> = _activeBaseMap.asStateFlow()

    private val _isOutdoorMode = MutableStateFlow(false)
    val isOutdoorMode: StateFlow<Boolean> = _isOutdoorMode.asStateFlow()

    private val _simWalkStepSize = MutableStateFlow(20f)
    val simWalkStepSize: StateFlow<Float> = _simWalkStepSize.asStateFlow()

    // Overlay Switches
    private val _showGrids = MutableStateFlow(true)
    val showGrids: StateFlow<Boolean> = _showGrids.asStateFlow()

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

    fun updateUiState(newState: MapUiState) {
        _uiState.value = newState
    }

    fun setZoomLevel(zoom: Int) {
        _zoomLevel.value = zoom.coerceIn(1, 8)
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

    suspend fun downloadRegionForLocation(lat: Double, lon: Double, name: String) {
        if (_isDownloading.value) return
        _isDownloading.value = true
        _downloadProgress.value = 0f
        
        val steps = listOf(
            "Establishing connection to tile server..." to 0.15f,
            "Scanning cache indices..." to 0.35f,
            "Compiling raster map tiles (Z12 - Z17)..." to 0.65f,
            "Saving topography contour grids locally..." to 0.85f,
            "Syncing offline cache database..." to 0.95f,
            "Offline sector storage sync: COMPLETE!" to 1.0f
        )
        
        for (step in steps) {
            _downloadStatusText.value = step.first
            _downloadProgress.value = step.second
            kotlinx.coroutines.delay(400)
        }
        
        val newRegionId = "reg_${System.currentTimeMillis()}"
        val finalName = name.ifBlank { "Custom Offline Block ${(_downloadedRegionsList.value.size + 1)}" }
        
        // Form a nice bounding box around the specified coordinates
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
            zoomRange = "Z12 - Z17",
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
