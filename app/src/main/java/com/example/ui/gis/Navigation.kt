package com.example.ui.gis

import android.os.Bundle
import com.example.Station

/**
 * Type-Safe Route coordinates target parameters for deep integration with the maps engine.
 */
sealed interface GisRouteTarget {
    object Default : GisRouteTarget
    
    data class CoordinateAnchor(
        val latitude: Double,
        val longitude: Double,
        val label: String = "Marker",
        val zoom: Int = 15
    ) : GisRouteTarget

    data class StationAnchor(
        val station: Station,
        val zoom: Int = 16
    ) : GisRouteTarget

    data class TrackFocus(
        val trackId: Int,
        val name: String
    ) : GisRouteTarget
}

/**
 * Handles seamless intent parameter extraction and navigation triggers from secondary pages
 * (e.g. Station List, Project Records) directly into the main interactive map engine.
 */
object GisNavigationController {
    var pendingRouteTarget: GisRouteTarget = GisRouteTarget.Default
        private set

    fun navigateTo(target: GisRouteTarget, onMainScreenTrigger: () -> Unit) {
        pendingRouteTarget = target
        onMainScreenTrigger()
    }

    fun clear() {
        pendingRouteTarget = GisRouteTarget.Default
    }
}
