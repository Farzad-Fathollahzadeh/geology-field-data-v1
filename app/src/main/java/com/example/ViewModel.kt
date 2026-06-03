package com.example

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GpsState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val isAvailable: Boolean = false,
    val stateMessage: String = "No GPS Fix"
)

class GeologyViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = DatabaseHelper(application)

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations.asStateFlow()

    private val _boreholes = MutableStateFlow<List<Borehole>>(emptyList())
    val boreholes: StateFlow<List<Borehole>> = _boreholes.asStateFlow()

    private val _activeObservations = MutableStateFlow<List<Observation>>(emptyList())
    val activeObservations: StateFlow<List<Observation>> = _activeObservations.asStateFlow()

    private val _activeBoreholeLogs = MutableStateFlow<List<BoreholeLog>>(emptyList())
    val activeBoreholeLogs: StateFlow<List<BoreholeLog>> = _activeBoreholeLogs.asStateFlow()

    private val _calculations = MutableStateFlow<List<GeotechCalc>>(emptyList())
    val calculations: StateFlow<List<GeotechCalc>> = _calculations.asStateFlow()

    var activeStationCode by mutableStateOf<String?>(null)
        private set

    var activeBoreholeId by mutableStateOf<Long?>(null)
        private set

    var gpsState by mutableStateOf(GpsState())
        private set

    init {
        loadStations()
        loadBoreholes()
        loadCalculations()
    }

    fun loadStations() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = dbHelper.getAllStations()
            _stations.value = list
        }
    }

    fun loadBoreholes() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = dbHelper.getAllBoreholes()
            _boreholes.value = list
        }
    }

    fun loadCalculations() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = dbHelper.getAllCalculations()
            _calculations.value = list
        }
    }

    fun selectStation(stationCode: String?) {
        activeStationCode = stationCode
        if (stationCode != null) {
            loadObservationsForStation(stationCode)
        } else {
            _activeObservations.value = emptyList()
        }
    }

    fun selectBorehole(boreholeId: Long?) {
        activeBoreholeId = boreholeId
        if (boreholeId != null) {
            loadLogsForBorehole(boreholeId)
        } else {
            _activeBoreholeLogs.value = emptyList()
        }
    }

    private fun loadObservationsForStation(stationCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = dbHelper.getObservationsForStation(stationCode)
            _activeObservations.value = list
        }
    }

    private fun loadLogsForBorehole(boreholeId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = dbHelper.getLogsForBorehole(boreholeId)
            _activeBoreholeLogs.value = list
        }
    }

    fun addStation(station: Station) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.insertStation(station)
            loadStations()
        }
    }

    fun deleteStation(stationCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteStation(stationCode)
            if (activeStationCode == stationCode) {
                selectStation(null)
            }
            loadStations()
        }
    }

    fun addObservation(obs: Observation) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.insertObservation(obs)
            activeStationCode?.let { loadObservationsForStation(it) }
        }
    }

    fun deleteObservation(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteObservation(id)
            activeStationCode?.let { loadObservationsForStation(it) }
        }
    }

    fun addBorehole(borehole: Borehole) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.insertBorehole(borehole)
            loadBoreholes()
        }
    }

    fun deleteBorehole(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteBorehole(id)
            if (activeBoreholeId == id) {
                selectBorehole(null)
            }
            loadBoreholes()
        }
    }

    fun addBoreholeLog(log: BoreholeLog) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.insertBoreholeLog(log)
            activeBoreholeId?.let { loadLogsForBorehole(it) }
            loadBoreholes() // Re-load to update total logged depths
        }
    }

    fun deleteBoreholeLog(logId: Long, boreholeId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteBoreholeLog(logId, boreholeId)
            loadLogsForBorehole(boreholeId)
            loadBoreholes()
        }
    }

    fun saveGeotechCalc(calc: GeotechCalc) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.saveCalculation(calc)
            loadCalculations()
        }
    }

    fun deleteGeotechCalc(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteCalculation(id)
            loadCalculations()
        }
    }

    fun updateGps(latitude: Double, longitude: Double, accuracy: Float) {
        gpsState = GpsState(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            isAvailable = true,
            stateMessage = String.format(java.util.Locale.US, "Fix: %.5f, %.5f (±%.1fm)", latitude, longitude, accuracy)
        )
    }

    fun setGpsUnavailable(message: String) {
        gpsState = gpsState.copy(isAvailable = false, stateMessage = message)
    }
}
