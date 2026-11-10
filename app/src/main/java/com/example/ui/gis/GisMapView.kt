package com.example.ui.gis

import android.content.Context
import android.graphics.Color as AndroidColor
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.GeologicalContact
import com.example.ImportedFeature
import com.example.MbtilesDatabaseCache
import com.example.Observation
import com.example.Station
import com.example.TileCacheDatabaseHelper
import com.example.Track
import com.example.TrackPoint
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

/**
 * Ultra-high-performance, Hardware-Accelerated Leaflet GIS Map View.
 * Provides smooth rendering of base maps, offline MBTiles, geological symbols,
 * tracks, UTM grid, and live measurement tools.
 */
@Composable
fun GisMapView(
    stations: List<Station>,
    observations: List<Observation>,
    trackPoints: List<TrackPoint>,
    contactsList: List<GeologicalContact> = emptyList(),
    currentLat: Double,
    currentLon: Double,
    zoom: Int,
    tileUrl: String,
    gpsAccuracy: Double,
    lockGpsCenter: Boolean,
    onCenterChanged: (Double, Double) -> Unit,
    onZoomChanged: (Int) -> Unit = {},
    navigationTargetStation: Station? = null,
    showUtmGrid: Boolean = true,
    isOutdoorMode: Boolean = true,
    baseMapOpacity: Float = 0.95f,
    offlineModeForce: Boolean = false,
    showOfflineBoundaries: Boolean = false,
    downloadedRegions: List<OfflineRegion> = emptyList(),
    importedFeatures: List<ImportedFeature> = emptyList(),
    savedTracks: List<Track> = emptyList(),
    showTracksOverlay: Boolean = true,
    showStations: Boolean = true,
    showSymbols: Boolean = true,
    showFaultLines: Boolean = true,
    activeMbtilesList: List<Pair<String, Float>> = emptyList(),
    isRulerMode: Boolean = false,
    isAreaMode: Boolean = false,
    onMeasurementResult: (Double, String) -> Unit = { _, _ -> },
    onStationSelected: (Station) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val leafZoom = zoom.coerceIn(3, 21)

    // Capture latest state updates for the long-lived WebView callbacks
    val currentLatState = rememberUpdatedState(currentLat)
    val currentLonState = rememberUpdatedState(currentLon)
    val gpsAccuracyState = rememberUpdatedState(gpsAccuracy)
    val lockGpsCenterState = rememberUpdatedState(lockGpsCenter)
    val leafZoomState = rememberUpdatedState(leafZoom)
    val showUtmGridState = rememberUpdatedState(showUtmGrid)
    val navTargetState = rememberUpdatedState(navigationTargetStation)
    val offlineModeForceState = rememberUpdatedState(offlineModeForce)

    // Generate lightweight HTML content that only changes when base configuration changes
    val htmlContent = remember(
        tileUrl, isOutdoorMode, baseMapOpacity, offlineModeForce,
        showOfflineBoundaries, downloadedRegions, activeMbtilesList,
        showStations, showSymbols, showFaultLines
    ) {
        generateOptimizedMapHtml(
            tileUrl = tileUrl,
            initialLat = currentLat,
            initialLon = currentLon,
            initialZoom = leafZoom,
            isOutdoorMode = isOutdoorMode,
            baseMapOpacity = baseMapOpacity,
            offlineModeForce = offlineModeForce,
            showOfflineBoundaries = showOfflineBoundaries,
            downloadedRegions = downloadedRegions,
            activeMbtilesList = activeMbtilesList
        )
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onCenterChanged(lat: Double, lon: Double) {
                        post { onCenterChanged(lat, lon) }
                    }

                    @JavascriptInterface
                    fun onZoomChanged(lvl: Int) {
                        post { onZoomChanged(lvl) }
                    }

                    @JavascriptInterface
                    fun onMeasurementUpdate(value: Double, unitText: String) {
                        post { onMeasurementResult(value, unitText) }
                    }

                    @JavascriptInterface
                    fun onStationClicked(stationCode: String) {
                        post {
                            stations.firstOrNull { it.stationCode == stationCode }?.let {
                                onStationSelected(it)
                            }
                        }
                    }
                }, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString() ?: return null

                        // 0. Handle SAS.Planet ZMP & built-in providers
                        if (url.startsWith("https://sas.local/")) {
                            val context = view?.context ?: return null
                            return com.example.SasTileProxy.interceptTileRequest(context, url, offlineModeForceState.value)
                        }

                        // 1. Handle local MBTiles tiles directly from disk storage
                        if (url.startsWith("https://mbtiles.local/")) {
                            val context = view?.context ?: return null
                            val path = url.substring("https://mbtiles.local/".length)
                            val parts = path.split("/")
                            if (parts.size >= 4) {
                                try {
                                    val fileName = URLDecoder.decode(parts[0], "UTF-8")
                                    val z = parts[1].toIntOrNull() ?: 0
                                    val x = parts[2].toIntOrNull() ?: 0
                                    val yPart = parts[3]
                                    val yStr = if (yPart.contains(".")) yPart.substring(0, yPart.indexOf(".")) else yPart
                                    val y = yStr.toIntOrNull() ?: 0

                                    val mbtilesDir = File(context.filesDir, "mbtiles")
                                    val mbtilesFile = File(mbtilesDir, fileName)
                                    if (mbtilesFile.exists() && mbtilesFile.canonicalPath.startsWith(mbtilesDir.canonicalPath)) {
                                        val tileBytes = MbtilesDatabaseCache.getTileBytesDirectly(mbtilesFile, z, x, y)
                                        if (tileBytes != null) {
                                            val contentType = if (url.endsWith(".jpg") || url.endsWith(".jpeg")) "image/jpeg" else "image/png"
                                            return WebResourceResponse(contentType, "UTF-8", ByteArrayInputStream(tileBytes))
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            return WebResourceResponse("image/png", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                        }

                        // 2. Online raster map tiles caching via SQLite
                        if (url.contains("tile.opentopomap.org") ||
                            url.contains("tile.openstreetmap.org") ||
                            url.contains("ArcGIS/rest/services") ||
                            url.contains("google.com/vt") ||
                            url.contains("lyrs=s") ||
                            url.contains("MapServer/tile")
                        ) {
                            val context = view?.context ?: return null
                            val helper = TileCacheDatabaseHelper.getInstance(context)
                            val cached = helper.getTile(url)
                            if (cached != null) {
                                return WebResourceResponse(cached.contentType, "UTF-8", ByteArrayInputStream(cached.data))
                            }

                            if (offlineModeForceState.value) {
                                return WebResourceResponse("image/png", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                            }

                            try {
                                val connection = URL(url).openConnection() as HttpURLConnection
                                connection.connectTimeout = 3000
                                connection.readTimeout = 3000
                                connection.useCaches = false
                                connection.connect()

                                if (connection.responseCode == 200) {
                                    val contentType = connection.contentType ?: "image/png"
                                    val bytes = connection.inputStream.use { it.readBytes() }
                                    helper.putTile(url, bytes, contentType)
                                    return WebResourceResponse(contentType, "UTF-8", ByteArrayInputStream(bytes))
                                }
                            } catch (e: Exception) {
                                // Fail gracefully if offline
                            }
                        }

                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val lat = currentLatState.value
                        val lon = currentLonState.value
                        val acc = gpsAccuracyState.value
                        val lock = lockGpsCenterState.value
                        val lz = leafZoomState.value
                        val grid = showUtmGridState.value

                        view?.evaluateJavascript(
                            """
                            if (typeof updateLiveLocation !== 'undefined') {
                                updateLiveLocation($lat, $lon, $acc, true, $lock, $lz);
                                if (typeof setUtmGridVisible !== 'undefined') { setUtmGridVisible($grid); }
                            }
                            """.trimIndent(),
                            null
                        )
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    setSupportZoom(true)
                }
                setBackgroundColor(AndroidColor.WHITE)
                tag = htmlContent
                loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            val oldHtml = webView.tag as? String
            if (oldHtml != htmlContent) {
                webView.tag = htmlContent
                webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
            }

            // 1. Update Live GPS Location
            webView.evaluateJavascript(
                "if (typeof updateLiveLocation !== 'undefined') { updateLiveLocation($currentLat, $currentLon, $gpsAccuracy, true, $lockGpsCenter, $leafZoom); }",
                null
            )

            // 2. Update Live Track Points (recording)
            val trackPtsJson = trackPoints.joinToString(prefix = "[", postfix = "]") { pt ->
                "{\"lat\": ${pt.lat}, \"lon\": ${pt.lon}}"
            }
            webView.evaluateJavascript(
                "if (typeof updateLiveTrackPoints !== 'undefined') { updateLiveTrackPoints($trackPtsJson); }",
                null
            )

            // 3. Update UTM Grid Visibility
            webView.evaluateJavascript(
                "if (typeof setUtmGridVisible !== 'undefined') { setUtmGridVisible($showUtmGrid); }",
                null
            )

            // 4. Update Navigation Target line if any
            val navTarget = navTargetState.value
            if (navTarget != null) {
                webView.evaluateJavascript(
                    "if (typeof updateNavigationLine !== 'undefined') { updateNavigationLine($currentLat, $currentLon, ${navTarget.latitude}, ${navTarget.longitude}, true); }",
                    null
                )
            } else {
                webView.evaluateJavascript(
                    "if (typeof updateNavigationLine !== 'undefined') { updateNavigationLine(0, 0, 0, 0, false); }",
                    null
                )
            }

            // 5. Update Geological Stations and Symbols
            if (showStations) {
                val stationsJson = stations.joinToString(prefix = "[", postfix = "]") { st ->
                    val obs = if (showSymbols) observations.firstOrNull { it.stationId == st.id && (it.strike != null || it.dip != null) } else null
                    val strikeVal = obs?.strike ?: -1
                    val dipVal = obs?.dip ?: -1
                    val dipDirVal = obs?.dipDirection ?: -1
                    val typeStr = obs?.observationType ?: ""
                    """{"lat": ${st.latitude}, "lon": ${st.longitude}, "code": "${st.stationCode.replace("\"", "\\\"")}", "strike": $strikeVal, "dip": $dipVal, "dipDir": $dipDirVal, "type": "$typeStr"}"""
                }
                webView.evaluateJavascript(
                    "if (typeof updateStationsLayer !== 'undefined') { updateStationsLayer($stationsJson); }",
                    null
                )
            } else {
                webView.evaluateJavascript(
                    "if (typeof updateStationsLayer !== 'undefined') { updateStationsLayer([]); }",
                    null
                )
            }

            // 6. Update Saved Tracks Overlay
            if (showTracksOverlay && savedTracks.isNotEmpty()) {
                val savedTracksJson = savedTracks.joinToString(prefix = "[", postfix = "]") { trk ->
                    """{"id": ${trk.id}, "name": "${trk.name.replace("\"", "\\\"")}", "points": ${trk.pointsJson}, "color": "${trk.colorHex}", "weight": ${trk.strokeWidth}}"""
                }
                webView.evaluateJavascript(
                    "if (typeof updateSavedTracksLayer !== 'undefined') { updateSavedTracksLayer($savedTracksJson); }",
                    null
                )
            } else {
                webView.evaluateJavascript(
                    "if (typeof updateSavedTracksLayer !== 'undefined') { updateSavedTracksLayer([]); }",
                    null
                )
            }

            // 7. Toggle Measurement Modes
            if (isRulerMode) {
                webView.evaluateJavascript("if (typeof setRulerMode !== 'undefined') { setRulerMode(true); }", null)
            } else if (isAreaMode) {
                webView.evaluateJavascript("if (typeof setAreaMode !== 'undefined') { setAreaMode(true); }", null)
            } else {
                webView.evaluateJavascript("if (typeof clearAllMeasurements !== 'undefined') { clearAllMeasurements(); }", null)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Generates clean, fast, hardware-accelerated Leaflet HTML for Android WebView.
 */
private fun generateOptimizedMapHtml(
    tileUrl: String,
    initialLat: Double,
    initialLon: Double,
    initialZoom: Int,
    isOutdoorMode: Boolean,
    baseMapOpacity: Float,
    offlineModeForce: Boolean,
    showOfflineBoundaries: Boolean,
    downloadedRegions: List<OfflineRegion>,
    activeMbtilesList: List<Pair<String, Float>>
): String {
    val activeMbtilesJson = activeMbtilesList.joinToString(prefix = "[", postfix = "]") { (name, opacity) ->
        """{"name": "${name.replace("\"", "\\\"")}", "opacity": $opacity}"""
    }

    val downloadedRegionsJson = downloadedRegions.joinToString(prefix = "[", postfix = "]") { reg ->
        """{"id": "${reg.id}", "name": "${reg.name.replace("\"", "\\\"")}", "minLat": ${reg.minLat}, "minLon": ${reg.minLon}, "maxLat": ${reg.maxLat}, "maxLon": ${reg.maxLon}}"""
    }

    val isSatellite = tileUrl.contains("lyrs=s") || tileUrl.contains("World_Imagery") || tileUrl.contains("satellite")
    val filterVal = if (isSatellite) "brightness(90%) contrast(105%) saturate(110%)" else "none"

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes" />
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css" />
            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js"></script>
            <style>
                body { padding: 0; margin: 0; background-color: #ffffff; }
                html, body, #map { height: 100%; width: 100%; overflow: hidden; background: #ffffff; }
                .leaflet-tile { filter: $filterVal; -webkit-filter: $filterVal; }
                .custom-div-icon {
                    background-color: #ffb300;
                    border: 2px solid #000;
                    border-radius: 50%;
                    width: 12px;
                    height: 12px;
                }
                .leaflet-bar { border: none !important; box-shadow: none !important; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false,
                    touchZoom: true,
                    doubleClickZoom: true,
                    boxZoom: true
                }).setView([$initialLat, $initialLon], $initialZoom);

                // Base Tile Layer
                var baseTileLayer = L.tileLayer('$tileUrl', {
                    maxZoom: 21,
                    opacity: $baseMapOpacity
                }).addTo(map);

                // Active MBTiles Overlays
                var activeMbtiles = $activeMbtilesJson;
                activeMbtiles.forEach(function(item) {
                    L.tileLayer('https://mbtiles.local/' + encodeURIComponent(item.name) + '/{z}/{x}/{y}.png', {
                        maxZoom: 21,
                        opacity: item.opacity,
                        zIndex: 100
                    }).addTo(map);
                });

                // Downloaded Region Boundaries
                if ($showOfflineBoundaries) {
                    var boundsList = $downloadedRegionsJson;
                    boundsList.forEach(function(reg) {
                        L.rectangle([[reg.minLat, reg.minLon], [reg.maxLat, reg.maxLon]], {
                            color: "#4CAF50",
                            weight: 1.5,
                            dashArray: "4, 4",
                            fillColor: "rgba(76,175,80,0.05)",
                            fill: true,
                            interactive: false
                        }).addTo(map);
                    });
                }

                // Layers & Groups
                var liveAccuracyCircle = null;
                var liveMarkerPoint = null;
                var liveTrackPolyline = null;
                var stationsLayerGroup = L.layerGroup().addTo(map);
                var savedTracksLayerGroup = L.layerGroup().addTo(map);
                var navLine = null;
                var utmGridLayer = L.layerGroup().addTo(map);
                var utmGridVisible = true;

                // Live Location Marker
                window.updateLiveLocation = function(lat, lon, accuracy, isRealPos, lockCenter, leafZoom) {
                    var markerColor = isRealPos ? '#007A78' : '#ff9100';
                    var accuracyColor = isRealPos ? '#00b4d8' : '#ffb74d';

                    if (!liveAccuracyCircle) {
                        liveAccuracyCircle = L.circle([lat, lon], {
                            color: accuracyColor,
                            fillColor: accuracyColor,
                            fillOpacity: 0.15,
                            weight: 1,
                            radius: accuracy
                        }).addTo(map);
                    } else {
                        liveAccuracyCircle.setLatLng([lat, lon]);
                        liveAccuracyCircle.setRadius(accuracy);
                    }

                    if (!liveMarkerPoint) {
                        liveMarkerPoint = L.circleMarker([lat, lon], {
                            color: '#ffffff',
                            fillColor: markerColor,
                            fillOpacity: 0.95,
                            weight: 2.5,
                            radius: 7
                        }).addTo(map);
                    } else {
                        liveMarkerPoint.setLatLng([lat, lon]);
                        liveMarkerPoint.setStyle({ fillColor: markerColor });
                    }

                    if (lockCenter) {
                        map.setView([lat, lon], leafZoom, { animate: true });
                    }
                };

                // Live Track Trail
                window.updateLiveTrackPoints = function(points) {
                    if (liveTrackPolyline) {
                        map.removeLayer(liveTrackPolyline);
                        liveTrackPolyline = null;
                    }
                    if (points && points.length > 1) {
                        var latlngs = points.map(function(p) { return [p.lat, p.lon]; });
                        liveTrackPolyline = L.polyline(latlngs, {
                            color: '#e91e63',
                            weight: 4,
                            opacity: 0.85
                        }).addTo(map);
                    }
                };

                // Navigation Target Line
                window.updateNavigationLine = function(sLat, sLon, dLat, dLon, show) {
                    if (navLine) {
                        map.removeLayer(navLine);
                        navLine = null;
                    }
                    if (show) {
                        navLine = L.polyline([[sLat, sLon], [dLat, dLon]], {
                            color: '#9c27b0',
                            weight: 3,
                            dashArray: '6, 8',
                            opacity: 0.9
                        }).addTo(map);
                    }
                };

                // Stations and Structural Geological Symbols
                window.updateStationsLayer = function(stations) {
                    stationsLayerGroup.clearLayers();
                    if (!stations) return;

                    stations.forEach(function(st) {
                        var markerIcon;
                        if (st.strike >= 0 && st.dip >= 0) {
                            var rotation = st.dipDir - 180;
                            var color = "#ffeb3b";
                            var svgSymbol = '<line x1="8" y1="20" x2="32" y2="20" stroke="' + color + '" stroke-width="3" />' +
                                            '<line x1="20" y1="20" x2="20" y2="30" stroke="' + color + '" stroke-width="3" />';

                            var obsLower = (st.type || '').toLowerCase();
                            if (obsLower.includes("fault")) {
                                color = "#ff3d00";
                                svgSymbol = '<line x1="8" y1="20" x2="32" y2="20" stroke="' + color + '" stroke-width="3.5" />' +
                                            '<line x1="12" y1="16" x2="16" y2="24" stroke="' + color + '" stroke-width="2.5" />' +
                                            '<line x1="24" y1="16" x2="28" y2="24" stroke="' + color + '" stroke-width="2.5" />';
                            } else if (obsLower.includes("joint")) {
                                color = "#00e676";
                                svgSymbol = '<line x1="8" y1="20" x2="32" y2="20" stroke="' + color + '" stroke-width="3" />' +
                                            '<line x1="20" y1="14" x2="20" y2="26" stroke="' + color + '" stroke-width="3" />';
                            }

                            var svgHtml = '<div style="position: relative; width: 40px; height: 40px; left: -14px; top: -14px;">' +
                                '<svg width="40" height="40" viewBox="0 0 40 40" style="overflow: visible;">' +
                                '  <g transform="rotate(' + rotation + ', 20, 20)">' + svgSymbol + '</g>' +
                                '  <text x="28" y="38" fill="' + color + '" font-size="11" font-weight="bold" font-family="sans-serif" style="text-shadow: 1px 1px 1px black;">' + st.dip + '</text>' +
                                '  <div style="position: absolute; left: 26px; top: -12px; color: #00e5ff; font-weight: bold; font-size: 10px; text-shadow: 1px 1px 1.5px black; white-space: nowrap;">' + st.code + '</div>' +
                                '</svg>' +
                                '</div>';

                            markerIcon = L.divIcon({
                                className: 'custom-struct-icon',
                                html: svgHtml,
                                iconSize: [40, 40],
                                iconAnchor: [20, 20]
                            });
                        } else {
                            markerIcon = L.divIcon({
                                className: 'custom-div-icon',
                                html: '<div style="position: absolute; left: 16px; top: -6px; color: #ffeb3b; font-weight: bold; font-size: 11px; text-shadow: 1px 1px 2px black; white-space: nowrap;">' + st.code + '</div>',
                                iconSize: [12, 12]
                            });
                        }

                        var marker = L.marker([st.lat, st.lon], { icon: markerIcon }).addTo(stationsLayerGroup);
                        marker.on('click', function() {
                            if (window.AndroidBridge && window.AndroidBridge.onStationClicked) {
                                window.AndroidBridge.onStationClicked(st.code);
                            }
                        });
                    });
                };

                // Camera Fly To Coordinate
                window.flyToCoordinate = function(lat, lon, zoom) {
                    map.flyTo([lat, lon], zoom || 16, { animate: true, duration: 1.0 });
                };

                // Saved Tracks
                window.updateSavedTracksLayer = function(tracks) {
                    savedTracksLayerGroup.clearLayers();
                    if (!tracks) return;

                    tracks.forEach(function(trk) {
                        try {
                            var pts = typeof trk.points === 'string' ? JSON.parse(trk.points) : trk.points;
                            if (pts && pts.length > 1) {
                                var latlngs = pts.map(function(p) { return [p.lat, p.lon]; });
                                L.polyline(latlngs, {
                                    color: trk.color || '#007A78',
                                    weight: trk.weight || 3,
                                    opacity: 0.8
                                }).addTo(savedTracksLayerGroup).bindPopup(trk.name);
                            }
                        } catch (e) {}
                    });
                };

                // UTM Grid Calculations
                function latLngToUtm(lat, lon) {
                    var latRad = lat * Math.PI / 180;
                    var lonRad = lon * Math.PI / 180;
                    var zone = Math.floor((lon + 180) / 6) + 1;
                    if (zone <= 0) zone = 1;
                    if (zone > 60) zone = 60;
                    var lonOrigin = (zone - 1) * 6 - 180 + 3;
                    var lonOriginRad = lonOrigin * Math.PI / 180;
                    var a = 6378137.0;
                    var eccSquared = 0.00669437999013;
                    var k0 = 0.9996;
                    var eccPrimeSquared = eccSquared / (1 - eccSquared);
                    var n = a / Math.sqrt(1 - eccSquared * Math.sin(latRad) * Math.sin(latRad));
                    var t = Math.tan(latRad) * Math.tan(latRad);
                    var c = eccPrimeSquared * Math.cos(latRad) * Math.cos(latRad);
                    var aCoeff = Math.cos(latRad) * (lonRad - lonOriginRad);
                    var m = a * ((1 - eccSquared / 4 - 3 * eccSquared * eccSquared / 64) * latRad
                            - (3 * eccSquared / 8 + 3 * eccSquared * eccSquared / 32) * Math.sin(2 * latRad)
                            + (15 * eccSquared * eccSquared / 256) * Math.sin(4 * latRad));
                    var easting = k0 * n * (aCoeff + (1 - t + c) * aCoeff * aCoeff * aCoeff / 6) + 500000.0;
                    var northing = k0 * (m + n * Math.tan(latRad) * (aCoeff * aCoeff / 2 + (5 - t + 9 * c) * aCoeff * aCoeff * aCoeff * aCoeff / 24));
                    if (lat < 0) northing += 10000000.0;
                    return { easting: easting, northing: northing, zone: zone };
                }

                function utmToLatLng(easting, northing, zone, isSouthern) {
                    var k0 = 0.9996;
                    var a = 6378137.0;
                    var eccSquared = 0.00669437999013;
                    var e1 = (1 - Math.sqrt(1 - eccSquared)) / (1 + Math.sqrt(1 - eccSquared));
                    var x = easting - 500000.0;
                    var y = isSouthern ? (northing - 10000000.0) : northing;
                    var lonOrigin = (zone - 1) * 6 - 180 + 3;
                    var eccPrimeSquared = eccSquared / (1 - eccSquared);
                    var m = y / k0;
                    var mu = m / (a * (1 - eccSquared / 4 - 3 * eccSquared * eccSquared / 64));
                    var phi1Rad = mu + (3 * e1 / 2 - 27 * e1 * e1 * e1 / 32) * Math.sin(2 * mu) + (21 * e1 * e1 / 16) * Math.sin(4 * mu);
                    var lat1Rad = phi1Rad;
                    var n1 = a / Math.sqrt(1 - eccSquared * Math.sin(lat1Rad) * Math.sin(lat1Rad));
                    var t1 = Math.tan(lat1Rad) * Math.tan(lat1Rad);
                    var c1 = eccPrimeSquared * Math.cos(lat1Rad) * Math.cos(lat1Rad);
                    var r1 = a * (1 - eccSquared) / Math.pow(1 - eccSquared * Math.sin(lat1Rad) * Math.sin(lat1Rad), 1.5);
                    var d = x / (n1 * k0);
                    var lat = lat1Rad - (n1 * Math.tan(lat1Rad) / r1) * (d * d / 2);
                    lat = lat * 180 / Math.PI;
                    var lon = (d - (1 + 2 * t1 + c1) * Math.pow(d, 3) / 6) / Math.cos(lat1Rad);
                    lon = lonOrigin + lon * 180 / Math.PI;
                    return { lat: lat, lon: lon };
                }

                function drawUtmGrid() {
                    utmGridLayer.clearLayers();
                    if (!utmGridVisible) return;
                    var zoom = map.getZoom();
                    if (zoom < 7) return;

                    var bounds = map.getBounds();
                    var sw = bounds.getSouthWest();
                    var ne = bounds.getNorthEast();

                    var spacing = 10000;
                    if (zoom >= 18) spacing = 100;
                    else if (zoom >= 16) spacing = 500;
                    else if (zoom >= 14) spacing = 1000;
                    else if (zoom >= 11) spacing = 5000;
                    else if (zoom >= 9) spacing = 10000;
                    else spacing = 50000;

                    var swUtm = latLngToUtm(sw.lat, sw.lng);
                    var neUtm = latLngToUtm(ne.lat, ne.lng);
                    var utmZone = swUtm.zone;
                    var isSouthern = sw.lat < 0;

                    var eStart = Math.floor(Math.min(swUtm.easting, neUtm.easting) / spacing) * spacing;
                    var eEnd = Math.ceil(Math.max(swUtm.easting, neUtm.easting) / spacing) * spacing;
                    var nStart = Math.floor(Math.min(swUtm.northing, neUtm.northing) / spacing) * spacing;
                    var nEnd = Math.ceil(Math.max(swUtm.northing, neUtm.northing) / spacing) * spacing;

                    if ((eEnd - eStart) / spacing > 60 || (nEnd - nStart) / spacing > 60) return;

                    var lineStyle = {
                        color: '${if (isOutdoorMode) "#007A78" else "#00e5ff"}',
                        weight: 0.8,
                        opacity: 0.6,
                        dashArray: '3, 4',
                        interactive: false
                    };

                    for (var e = eStart; e <= eEnd; e += spacing) {
                        var latlngs = [];
                        var step = (nEnd - nStart) / 4;
                        for (var n = nStart; n <= nEnd; n += step) {
                            var pt = utmToLatLng(e, n, utmZone, isSouthern);
                            latlngs.push([pt.lat, pt.lon]);
                        }
                        L.polyline(latlngs, lineStyle).addTo(utmGridLayer);
                    }

                    for (var n = nStart; n <= nEnd; n += spacing) {
                        var latlngs = [];
                        var step = (eEnd - eStart) / 4;
                        for (var e = eStart; e <= eEnd; e += step) {
                            var pt = utmToLatLng(e, n, utmZone, isSouthern);
                            latlngs.push([pt.lat, pt.lon]);
                        }
                        L.polyline(latlngs, lineStyle).addTo(utmGridLayer);
                    }
                }

                window.setUtmGridVisible = function(visible) {
                    utmGridVisible = visible;
                    drawUtmGrid();
                };

                // Map Events
                map.on('move', drawUtmGrid);
                map.on('moveend', function() {
                    var center = map.getCenter();
                    if (window.AndroidBridge && window.AndroidBridge.onCenterChanged) {
                        window.AndroidBridge.onCenterChanged(center.lat, center.lng);
                    }
                    var zoomLvl = map.getZoom();
                    if (window.AndroidBridge && window.AndroidBridge.onZoomChanged) {
                        window.AndroidBridge.onZoomChanged(zoomLvl);
                    }
                });

                // Measurement Tools (Distance & Area)
                var rulerPoints = [];
                var rulerMarkers = [];
                var rulerPolyline = null;
                var isRulerActive = false;
                var isAreaActive = false;

                window.setRulerMode = function(active) {
                    isRulerActive = active;
                    isAreaActive = false;
                    map.getContainer().style.cursor = active ? 'crosshair' : '';
                };

                window.setAreaMode = function(active) {
                    isAreaActive = active;
                    isRulerActive = false;
                    map.getContainer().style.cursor = active ? 'crosshair' : '';
                };

                window.clearAllMeasurements = function() {
                    isRulerActive = false;
                    isAreaActive = false;
                    map.getContainer().style.cursor = '';
                    rulerMarkers.forEach(function(m) { map.removeLayer(m); });
                    if (rulerPolyline) map.removeLayer(rulerPolyline);
                    rulerPoints = [];
                    rulerMarkers = [];
                    rulerPolyline = null;
                };

                map.on('click', function(e) {
                    if (isRulerActive) {
                        rulerPoints.push(e.latlng);
                        var marker = L.circleMarker(e.latlng, { radius: 5, color: '#007A78', fillColor: '#fff', fillOpacity: 1 }).addTo(map);
                        rulerMarkers.push(marker);

                        if (rulerPoints.length > 1) {
                            if (rulerPolyline) map.removeLayer(rulerPolyline);
                            rulerPolyline = L.polyline(rulerPoints, { color: '#007A78', weight: 3, dashArray: '5, 5' }).addTo(map);

                            var totalDist = 0;
                            for (var i = 0; i < rulerPoints.length - 1; i++) {
                                totalDist += rulerPoints[i].distanceTo(rulerPoints[i+1]);
                            }
                            if (window.AndroidBridge && window.AndroidBridge.onMeasurementUpdate) {
                                window.AndroidBridge.onMeasurementUpdate(totalDist, totalDist >= 1000 ? (totalDist/1000).toFixed(2) + " km" : totalDist.toFixed(0) + " m");
                            }
                        }
                    } else if (isAreaActive) {
                        rulerPoints.push(e.latlng);
                        var marker = L.circleMarker(e.latlng, { radius: 5, color: '#e91e63', fillColor: '#fff', fillOpacity: 1 }).addTo(map);
                        rulerMarkers.push(marker);

                        if (rulerPoints.length >= 3) {
                            if (rulerPolyline) map.removeLayer(rulerPolyline);
                            rulerPolyline = L.polygon(rulerPoints, { color: '#e91e63', fillColor: '#e91e63', fillOpacity: 0.15 }).addTo(map);

                            // Approximate geodesic area
                            var area = 0;
                            for (var i = 0; i < rulerPoints.length; i++) {
                                var j = (i + 1) % rulerPoints.length;
                                var p1 = rulerPoints[i];
                                var p2 = rulerPoints[j];
                                area += (p2.lng - p1.lng) * (2 + Math.sin(p1.lat * Math.PI / 180) + Math.sin(p2.lat * Math.PI / 180));
                            }
                            area = Math.abs(area * 6378137.0 * 6378137.0 / 4.0);
                            if (window.AndroidBridge && window.AndroidBridge.onMeasurementUpdate) {
                                window.AndroidBridge.onMeasurementUpdate(area, area >= 10000 ? (area/10000).toFixed(2) + " ha" : area.toFixed(0) + " m²");
                            }
                        }
                    }
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}
