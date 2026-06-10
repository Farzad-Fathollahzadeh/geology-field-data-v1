package com.example.ui.gis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LayerManagerSheet(
    activeBaseMap: String,
    onBaseMapChange: (String) -> Unit,
    showGrids: Boolean,
    onToggleGrids: (Boolean) -> Unit,
    showStations: Boolean,
    onToggleStations: (Boolean) -> Unit,
    showSymbols: Boolean,
    onToggleSymbols: (Boolean) -> Unit,
    showFaultLines: Boolean,
    onToggleFaultLines: (Boolean) -> Unit,
    onSeedDemoDataset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isOutdoorMode: Boolean = false
) {
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
    val overlayBgColor = if (isOutdoorMode) Color(0xFFF9FBF9) else Color(0xFF141917)
    val cardColor = if (isOutdoorMode) Color.White else Color(0xFF1E2623)
    val textColor = if (isOutdoorMode) Color.Black else Color.White

    val mapProviders = listOf("Google Satellite", "OpenTopoMap", "OpenStreetMap", "Offline Topo-Raster")

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
                    text = "GIS LAYERS & DATA SOURCE MANAGER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = "Configure Base-Maps",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close overlay selector",
                    tint = textColor.copy(alpha = 0.6f)
                )
            }
        }

        Divider(color = textColor.copy(alpha = 0.1f))

        // MAP TYPE SELECTOR GRID
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "BASE LAYER RASTER PROTOCOLS",
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mapProviders.take(2).forEach { provider ->
                        val isChosen = activeBaseMap == provider
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isChosen) accentColor else textColor.copy(alpha = 0.05f))
                                .clickable { onBaseMapChange(provider) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = provider,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isChosen) Color.White else textColor
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mapProviders.drop(2).forEach { provider ->
                        val isChosen = activeBaseMap == provider
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isChosen) accentColor else textColor.copy(alpha = 0.05f))
                                .clickable { onBaseMapChange(provider) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = provider,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isChosen) Color.White else textColor
                            )
                        }
                    }
                }
            }
        }

        // FEATURE OVERLAYS VISIBILITY CHECKBOXES
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "GIS VECTOR OVERLAYS",
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                // Grid switcher
                OverlaySwitchRow(
                    label = "UTM Grid Coordinates overlay",
                    checked = showGrids,
                    onCheckedChange = onToggleGrids,
                    textColor = textColor,
                    accentColor = accentColor
                )

                // Station markers switcher
                OverlaySwitchRow(
                    label = "Geological Outcrop Stations (Markers)",
                    checked = showStations,
                    onCheckedChange = onToggleStations,
                    textColor = textColor,
                    accentColor = accentColor
                )

                // Symbols bedding strike/dip switcher
                OverlaySwitchRow(
                    label = "Structural Plane Bearing Symbols",
                    checked = showSymbols,
                    onCheckedChange = onToggleSymbols,
                    textColor = textColor,
                    accentColor = accentColor
                )

                // Fault lines switcher
                OverlaySwitchRow(
                    label = "Shearing Structural Fault Contact Lines",
                    checked = showFaultLines,
                    onCheckedChange = onToggleFaultLines,
                    textColor = textColor,
                    accentColor = accentColor
                )
            }
        }

        // SEED DATA SET CORE UTILITY
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DEMONSTRATION SYSTEM DATASETS",
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Populate database with pre-recorded exploration stations and fault traces for mapping.",
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.6f)
                )

                Button(
                    onClick = onSeedDemoDataset,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Storage, contentDescription = "Seed data icon", tint = Color.Black)
                        Text(
                            text = "SEED PROFESSIONAL GIS DEMO DATASET",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlaySwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = textColor.copy(alpha = 0.4f),
                uncheckedTrackColor = textColor.copy(alpha = 0.1f)
            )
        )
    }
}
