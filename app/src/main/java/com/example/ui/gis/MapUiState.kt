package com.example.ui.gis

sealed interface MapUiState {
    object Idle : MapUiState
    object LayersExpanded : MapUiState
    object DataExpanded : MapUiState
    object PositionExpanded : MapUiState
    object SettingsExpanded : MapUiState
    object TrackRecordingActive : MapUiState
    object MeasurementMode : MapUiState
    object ObservationCreationMode : MapUiState
    object SampleCreationMode : MapUiState
    object BoreholeCreationMode : MapUiState
}
