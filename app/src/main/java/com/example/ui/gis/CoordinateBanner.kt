package com.example.ui.gis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Sync
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
    val panelBg = if (isOutdoorMode) Color.White.copy(alpha = 0.92f) else Color(0xEE0A100E)
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

    // Dynamic convert to MGRS
    val mgrsString = remember(latitude, longitude, utmCoord) {
        if (utmCoord == null) {
            "GZD Converting..."
        } else {
            val z = utmCoord.zoneNumber
            val band = utmCoord.zoneChar
            
            val colLetters = "ABCDEFGHJKLMNPQRSTUVXYZ"
            val colIndex = (utmCoord.easting / 100000).toInt()
            val colOffset = ((z - 1) % 3) * 8
            val colLetter = colLetters[(colIndex - 1 + colOffset) % colLetters.length]
            
            val rowLetters = "ABCDEFGHJKLMNPQRSTUV"
            val rowIndex = (utmCoord.northing / 100000).toInt() % 20
            val rowOffset = if (z % 2 != 0) 0 else 5
            val rowLetter = rowLetters[(rowIndex + rowOffset) % rowLetters.length]
            
            val eastMeters = (utmCoord.easting % 100000).toInt().coerceIn(0, 99999)
            val northMeters = (utmCoord.northing % 100000).toInt().coerceIn(0, 99999)
            
            val eStr = String.format(Locale.US, "%05d", eastMeters)
            val nStr = String.format(Locale.US, "%05d", northMeters)
            
            "$z$band $colLetter$rowLetter $eStr $nStr"
        }
    }

    val readoutText = when (selectedFormatIndex) {
        0 -> "${String.format(Locale.US, "%.6f", latitude)}°, ${String.format(Locale.US, "%.6f", longitude)}°"
        1 -> {
            if (utmCoord != null) {
                "${utmCoord.zoneString}  ${String.format(Locale.US, "%.1f", utmCoord.easting)}m E  ${String.format(Locale.US, "%.1f", utmCoord.northing)}m N"
            } else {
                "UTM Calculating..."
            }
        }
        else -> mgrsString
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = panelBg,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, accentColor.copy(alpha = 0.35f)),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                selectedFormatIndex = (selectedFormatIndex + 1) % 3
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedFormatIndex) {
                            0 -> "WGS84"
                            1 -> "UTM GRID"
                            else -> "MGRS"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Cycle Format",
                        tint = textMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(11.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = readoutText,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ارتفاع: ${elevation.toInt()}m",
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.75f)
                    )
                    Text(
                        text = "دقت: ±${String.format(Locale.US, "%.1f", accuracy)}m",
                        fontSize = 10.sp,
                        color = if (accuracy < 3.0) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                    val sensorData by rememberCompassSensor()
                    if (sensorData.isAvailable) {
                        Text(
                            text = "جهت: ${sensorData.azimuthDegrees.toInt()}°",
                            fontSize = 10.sp,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(readoutText))
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Coordinates",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
