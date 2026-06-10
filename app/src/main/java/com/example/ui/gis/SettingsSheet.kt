package com.example.ui.gis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun SettingsSheet(
    isOutdoorMode: Boolean,
    onToggleOutdoorMode: (Boolean) -> Unit,
    simWalkStepSize: Float,
    onSimStepSizeChange: (Float) -> Unit,
    onResetDatabase: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
    val overlayBgColor = if (isOutdoorMode) Color(0xFFF9FBF9) else Color(0xFF141917)
    val cardColor = if (isOutdoorMode) Color.White else Color(0xFF1E2623)
    val textColor = if (isOutdoorMode) Color.Black else Color.White

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(overlayBgColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GIS PARAMETERS & COMPASS SYSTEM SETTINGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = "Platform Configuration",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close settings sheet",
                    tint = textColor.copy(alpha = 0.6f)
                )
            }
        }

        Divider(color = textColor.copy(alpha = 0.1f))

        // SUNLIGHT READABILITY HIGH CONTRAST MODE
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleOutdoorMode(!isOutdoorMode) }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Sunlight Mode Icon",
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Sunlight Outdoor Contrast",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Forces high-contrast light colors for sun glare",
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                }
                Switch(
                    checked = isOutdoorMode,
                    onCheckedChange = onToggleOutdoorMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = accentColor
                    )
                )
            }
        }

        // SIMULATION STEP SIZE CONTROLS
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "GPS WALK SIMULATOR SETTINGS",
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "D-Pad Walk Distance: " + String.format(Locale.US, "%.0f", simWalkStepSize) + " meters",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Slider(
                    value = simWalkStepSize,
                    onValueChange = onSimStepSizeChange,
                    valueRange = 5f..100f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = textColor.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // HARD RESET WARNING DATABASE
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Critical Warning", tint = Color.Red)
                    Text(
                        text = "CRITICAL STORAGE MAINTENANCE",
                        fontSize = 10.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "This operation purges all logged stations, project core structures, borehole records, and field walk traces from local SQLite storage.",
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.7f)
                )

                Button(
                    onClick = onResetDatabase,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Trash", tint = Color.White)
                        Text(
                            text = "RESET ALL PLATFORM STORAGE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
