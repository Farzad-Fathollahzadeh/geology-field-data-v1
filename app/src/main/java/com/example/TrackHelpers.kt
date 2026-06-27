package com.example

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Stop

// -------------------------------------------------------------------------
// 5. GPS EXPORT SHAPE TRANSLATORS HELPER FUNCTIONS
// -------------------------------------------------------------------------
fun generateGpxString(name: String, points: List<TrackPoint>): String {
    val sb = java.lang.StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    sb.append("<gpx version=\"1.1\" creator=\"GeoField Platform\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
    sb.append("  <metadata>\n")
    sb.append("    <name>$name</name>\n")
    sb.append("    <desc>Geotechnical Field Mapping Route</desc>\n")
    sb.append("  </metadata>\n")
    sb.append("  <trk>\n")
    sb.append("    <name>$name</name>\n")
    sb.append("    <trkseg>\n")
    points.forEach { pt ->
        val isoTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(pt.time))
        sb.append("      <trkpt lat=\"${pt.lat}\" lon=\"${pt.lon}\">\n")
        sb.append("        <ele>${pt.ele}</ele>\n")
        sb.append("        <time>$isoTime</time>\n")
        sb.append("      </trkpt>\n")
    }
    sb.append("    </trkseg>\n")
    sb.append("  </trk>\n")
    sb.append("</gpx>\n")
    return sb.toString()
}

fun generateKmlString(name: String, points: List<TrackPoint>): String {
    val sb = java.lang.StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
    sb.append("  <Document>\n")
    sb.append("    <name>$name</name>\n")
    sb.append("    <description>Geofield GIS Track Path</description>\n")
    sb.append("    <Style id=\"redLinePoly\">\n")
    sb.append("      <LineStyle>\n")
    sb.append("        <color>ff0000ff</color>\n") // red
    sb.append("        <width>4</width>\n")
    sb.append("      </LineStyle>\n")
    sb.append("    </Style>\n")
    sb.append("    <Placemark>\n")
    sb.append("      <name>Recorded Route Path</name>\n")
    sb.append("      <styleUrl>#redLinePoly</styleUrl>\n")
    sb.append("      <LineString>\n")
    sb.append("        <coordinates>\n")
    points.forEach { pt ->
        sb.append("          ${pt.lon},${pt.lat},${pt.ele}\n")
    }
    sb.append("        </coordinates>\n")
    sb.append("      </LineString>\n")
    sb.append("    </Placemark>\n")
    sb.append("  </Document>\n")
    sb.append("</kml>\n")
    return sb.toString()
}

fun generateGeoJsonString(name: String, points: List<TrackPoint>): String {
    val sb = java.lang.StringBuilder()
    sb.append("{\n")
    sb.append("  \"type\": \"FeatureCollection\",\n")
    sb.append("  \"features\": [\n")
    sb.append("    {\n")
    sb.append("      \"type\": \"Feature\",\n")
    sb.append("      \"properties\": {\n")
    sb.append("        \"name\": \"$name\",\n")
    sb.append("        \"type\": \"GPS_Track_Log\"\n")
    sb.append("      },\n")
    sb.append("      \"geometry\": {\n")
    sb.append("        \"type\": \"LineString\",\n")
    sb.append("        \"coordinates\": [\n")
    points.forEachIndexed { i, pt ->
        sb.append("          [${pt.lon}, ${pt.lat}, ${pt.ele}]")
        if (i < points.size - 1) sb.append(",")
        sb.append("\n")
    }
    sb.append("        ]\n")
    sb.append("      }\n")
    sb.append("    }\n")
    sb.append("  ]\n")
    sb.append("}\n")
    return sb.toString()
}

fun generateTxtString(name: String, points: List<TrackPoint>, length: Double, duration: Long): String {
    val sb = java.lang.StringBuilder()
    sb.append("=========================================\n")
    sb.append("GEOFIELD FIELD TRACK LOG REPORT SUMMARY  \n")
    sb.append("=========================================\n")
    sb.append("Track Name: $name\n")
    sb.append("Total Distance Estimate: ${length.toInt()} meters\n")
    sb.append("Recording duration time: ${duration / 60} minutes, ${duration % 60} seconds\n")
    sb.append("Recorded GPS Coordinates point captures: ${points.size}\n\n")
    sb.append("Point Coordinates Matrix List:\n")
    points.forEachIndexed { idx, pt ->
        val tm = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(pt.time))
        sb.append("${idx + 1}. Lat: ${pt.lat}, Lon: ${pt.lon}, Alt: ${pt.ele}m, Heading: ${pt.heading}deg, Time: $tm\n")
    }
    return sb.toString()
}

@Composable
fun LiveTrackRecordingHUD(
    isFarsiMode: Boolean,
    isOutdoorMode: Boolean,
    durationSec: Long,
    trackPoints: List<TrackPoint>,
    liveLocation: android.location.Location?,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onQuickLogStation: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate live distance
    val liveDistanceMeters = remember(trackPoints) {
        var total = 0.0
        for (i in 0 until trackPoints.size - 1) {
            val p1 = trackPoints[i]
            val p2 = trackPoints[i + 1]
            val results = FloatArray(1)
            android.location.Location.distanceBetween(p1.lat, p1.lon, p2.lat, p2.lon, results)
            total += results[0]
        }
        total
    }

    // Format duration
    val hours = durationSec / 3600
    val minutes = (durationSec % 3600) / 60
    val seconds = durationSec % 60
    val durationStr = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

    // Format distance
    val distanceStr = if (liveDistanceMeters < 1000) {
        String.format(Locale.US, "%.0f m", liveDistanceMeters)
    } else {
        String.format(Locale.US, "%.2f km", liveDistanceMeters / 1000.0)
    }

    // Calculate speeds (current and average)
    val currentSpeedKmh = (liveLocation?.speed ?: 0f) * 3.6f
    val avgSpeedKmh = if (durationSec > 0) {
        (liveDistanceMeters / durationSec) * 3.6
    } else {
        0.0
    }

    // Altitude stats
    val (minEle, maxEle, currentEle) = remember(trackPoints, liveLocation) {
        if (trackPoints.isEmpty()) {
            val alt = liveLocation?.altitude ?: 0.0
            Triple(alt, alt, alt)
        } else {
            val elevations = trackPoints.map { it.ele }
            val min = elevations.minOrNull() ?: 0.0
            val max = elevations.maxOrNull() ?: 0.0
            val current = liveLocation?.altitude ?: trackPoints.last().ele
            Triple(min, max, current)
        }
    }

    // Theme Colors
    val cardBgColor = if (isOutdoorMode) Color.White else Color(0xFF141A1B)
    val cardBorderColor = if (isOutdoorMode) Color(0xFF00796B) else Color(0xFF00E5FF)
    val textColor = if (isOutdoorMode) Color.Black else Color.White
    val subTextColor = if (isOutdoorMode) Color.Gray else Color(0xFFB0BEC5)
    val accentRed = Color(0xFFFF3D00)

    // Animated breathing / blinking dot for recording indicator
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.2.dp, cardBorderColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Status Indicator & Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .graphicsLayer { alpha = if (isPaused) 1f else pulseAlpha }
                            .background(if (isPaused) Color.Gray else accentRed, CircleShape)
                    )
                    Text(
                        text = if (isPaused) {
                            if (isFarsiMode) "ضبط مسیر متوقف شده" else "RECORDING PAUSED"
                        } else {
                            if (isFarsiMode) "درحال ثبت زنده مسیر..." else "RECORDING TRACK LIVE"
                        },
                        color = if (isPaused) Color.Gray else textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = durationStr,
                    color = if (isPaused) Color.Gray else cardBorderColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Metrics Row: 3 Columns (Distance, Speed, Altitude)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Distance Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(textColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = cardBorderColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isFarsiMode) "مسافت" else "DISTANCE",
                        fontSize = 9.sp,
                        color = subTextColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = distanceStr,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 2. Speed Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(textColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = cardBorderColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isFarsiMode) "سرعت (لحظه/متوسط)" else "SPEED (CUR/AVG)",
                        fontSize = 9.sp,
                        color = subTextColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f / %.1f", currentSpeedKmh, avgSpeedKmh),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "km/h",
                        fontSize = 8.sp,
                        color = subTextColor
                    )
                }

                // 3. Elevation Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(textColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = cardBorderColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isFarsiMode) "ارتفاع (حداکثر/حداقل)" else "ALTITUDE (MAX/MIN)",
                        fontSize = 9.sp,
                        color = subTextColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = String.format(Locale.US, "%.0f / %.0f", maxEle, minEle),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(Locale.US, "فعلی: %.1f m", currentEle),
                        fontSize = 8.sp,
                        color = subTextColor
                    )
                }
            }

            // Action Buttons Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Resume Button
                Button(
                    onClick = onPauseToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) cardBorderColor else textColor.copy(alpha = 0.1f),
                        contentColor = if (isPaused) Color.Black else textColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.2f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPaused) {
                            if (isFarsiMode) "ادامه" else "Resume"
                        } else {
                            if (isFarsiMode) "توقف موقت" else "Pause"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Quick-Log Station along the route
                Button(
                    onClick = onQuickLogStation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardBorderColor.copy(alpha = 0.15f),
                        contentColor = cardBorderColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.5f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFarsiMode) "ثبت ایستگاه" else "Log Station",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // End & Save Button
                Button(
                    onClick = onStopRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.2f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFarsiMode) "پایان زنده" else "End & Save",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
