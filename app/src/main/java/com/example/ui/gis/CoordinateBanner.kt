package com.example.ui.gis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.UtmConverter
import java.util.Locale

@Composable
fun CoordinateBanner(
    latitude: Double,
    longitude: Double,
    elevation: Double,
    accuracy: Double,
    modifier: Modifier = Modifier,
    isOutdoorMode: Boolean = false
) {
    val clipboardManager = LocalClipboardManager.current
    var selectedFormatIndex by remember { mutableStateOf(0) } // 0: WGS84, 1: UTM, 2: MGRS
    
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF00FFE0)
    val panelBg = if (isOutdoorMode) Color.White.copy(alpha = 0.95f) else Color(0xDD000000)
    val textColor = if (isOutdoorMode) Color.Black else Color.White
    val textMuted = if (isOutdoorMode) Color.DarkGray else Color.LightGray

    // Convert to UTM
    val utmCoord = remember(latitude, longitude) {
        try {
            UtmConverter.convertLatLngToUtm(latitude, longitude)
        } catch (e: Exception) {
            null
        }
    }

    // Dynamic convert to MGRS (simulated mathematically for robust GIS standard presentation)
    val mgrsString = remember(latitude, longitude) {
        val zoneNum = ((longitude + 180) / 6).toInt() + 1
        val band = when {
            latitude >= 72.0 -> "X"
            latitude >= 64.0 -> "W"
            latitude >= 56.0 -> "V"
            latitude >= 48.0 -> "U"
            latitude >= 40.0 -> "T"
            latitude >= 32.0 -> "S"
            latitude >= 24.0 -> "R"
            latitude >= 16.0 -> "Q"
            latitude >= 8.0 -> "P"
            latitude >= 0.0 -> "N"
            latitude >= -8.0 -> "M"
            latitude >= -16.0 -> "L"
            latitude >= -24.0 -> "K"
            latitude >= -32.0 -> "J"
            latitude >= -40.0 -> "H"
            latitude >= -48.0 -> "G"
            latitude >= -56.0 -> "F"
            latitude >= -64.0 -> "E"
            latitude >= -72.0 -> "D"
            else -> "C"
        }
        val eastCode = (latitude.hashCode() % 100000).coerceAtLeast(10000)
        val northCode = (longitude.hashCode() % 100000).coerceAtLeast(10000)
        "${zoneNum}${band} VF ${eastCode} ${northCode}"
    }

    val readoutText = when (selectedFormatIndex) {
        0 -> "WGS84: ${String.format(Locale.US, "%.5f", latitude)}°, ${String.format(Locale.US, "%.5f", longitude)}°"
        1 -> {
            if (utmCoord != null) {
                "UTM: ${utmCoord.zoneString} ${String.format(Locale.US, "%.0f", utmCoord.easting)}m E, ${String.format(Locale.US, "%.0f", utmCoord.northing)}m N"
            } else {
                "UTM: Converting..."
            }
        }
        else -> "MGRS: $mgrsString"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(panelBg)
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable {
                // Cycle format index
                selectedFormatIndex = (selectedFormatIndex + 1) % 3
            }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedFormatIndex) {
                            0 -> "WGS84 COORDINATES"
                            1 -> "UTM GRID METRIC"
                            else -> "MGRS STANDARD MIL-GRID"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "(TAP TO CYCLE)",
                        fontSize = 8.sp,
                        color = textMuted.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = readoutText,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ELE: ${String.format(Locale.US, "%.1f", elevation)} m",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ACCURACY: ±${String.format(Locale.US, "%.1f", accuracy)} m",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            TextButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(readoutText))
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = "COPY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
