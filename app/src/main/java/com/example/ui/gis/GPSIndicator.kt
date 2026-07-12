package com.example.ui.gis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GPSIndicator(
    satelliteCount: Int,
    accuracy: Double,
    isRealPosition: Boolean,
    modifier: Modifier = Modifier,
    isOutdoorMode: Boolean = false
) {
    val textCol = if (isOutdoorMode) Color.Black else Color.White
    val panelBg = if (isOutdoorMode) Color.White.copy(alpha = 0.95f) else Color(0xDD000000)

    // Decouple states based on specs: Searching, Weak Signal, Good Signal, Fixed
    val (statusLabel, statusColor, statusIcon) = when {
        !isRealPosition -> Triple("SIMULATED", Color.Cyan, Icons.Default.GpsNotFixed)
        satelliteCount == 0 -> Triple("SEARCHING", Color.Red, Icons.Default.GpsOff)
        accuracy > 10.0 -> Triple("WEAK SIGNAL", Color(0xFFFF9100), Icons.Default.GpsNotFixed)
        satelliteCount >= 8 && accuracy < 3.0 -> Triple("FIXED RTK", Color(0xFF00E676), Icons.Default.GpsFixed)
        else -> Triple("GOOD SIGNAL", Color(0xFFFFEA00), Icons.Default.GpsFixed)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(panelBg)
            .border(0.5.dp, textCol.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = "GNSS Fix status indicator icon",
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )

            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(statusColor, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = statusLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = statusColor
                    )
                }
                Text(
                    text = if (isRealPosition) "$satelliteCount SATS | ±${String.format(java.util.Locale.US, "%.1f", accuracy)}m" else "SIMULATED | ±${String.format(java.util.Locale.US, "%.1f", accuracy)}m",
                    fontSize = 11.sp,
                    color = textCol,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
