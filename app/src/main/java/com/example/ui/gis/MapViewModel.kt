package com.example.ui.gis

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

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
