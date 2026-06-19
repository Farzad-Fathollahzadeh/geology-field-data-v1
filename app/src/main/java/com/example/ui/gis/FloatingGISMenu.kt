package com.example.ui.gis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FloatingGISMenu(
    isTrackRecording: Boolean,
    onAddObservation: () -> Unit,
    onAddSample: () -> Unit,
    onAddBorehole: () -> Unit,
    onAddStructural: () -> Unit,
    onToggleTrackRecording: () -> Unit,
    onMeasureDistance: () -> Unit,
    onMeasureArea: () -> Unit,
    onImportGpx: () -> Unit,
    onExportGpx: () -> Unit,
    onManageLayers: () -> Unit,
    modifier: Modifier = Modifier,
    isOutdoorMode: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
    val buttonBgColor = if (isOutdoorMode) Color(0xFFF0F2F0) else Color(0xFF1B2321)
    val fontColor = if (isOutdoorMode) Color.Black else Color.White
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Dropdown actions menu panel (Using Compose spring transitions)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                    expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .background(
                        if (isOutdoorMode) Color.White else Color(0xFF0D1210),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
                    .width(220.dp)
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "FIELD GIS CONSOLE",
                    fontSize = 10.sp,
                    color = accentColor,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Divider(color = fontColor.copy(alpha = 0.08f), thickness = 1.dp)

                // Observation tool
                GISMenuButton(
                    label = "Log Obs Station",
                    icon = Icons.Default.AddLocationAlt,
                    accentColor = accentColor,
                    textColor = fontColor,
                    onClick = {
                        onAddObservation()
                        isExpanded = false
                    }
                )

                // Sample collector tool
                GISMenuButton(
                    label = "Draw Sample Core",
                    icon = Icons.Default.Science,
                    accentColor = accentColor,
                    textColor = fontColor,
                    onClick = {
                        onAddSample()
                        isExpanded = false
                    }
                )

                // Structural structural strike/dip tool
                GISMenuButton(
                    label = "Structural Compass",
                    icon = Icons.Default.Explore,
                    accentColor = accentColor,
                    textColor = fontColor,
                    onClick = {
                        onAddStructural()
                        isExpanded = false
                    }
                )

                // Borehole tool
                GISMenuButton(
                    label = "Plan Borehole Collar",
                    icon = Icons.Default.Layers,
                    accentColor = accentColor,
                    textColor = fontColor,
                    onClick = {
                        onAddBorehole()
                        isExpanded = false
                    }
                )

                Divider(color = fontColor.copy(alpha = 0.08f))

                // Track Recording Trigger (Saves GPS walk paths)
                GISMenuButton(
                    label = if (isTrackRecording) "Stop Logging Walk" else "Record GPS Path",
                    icon = if (isTrackRecording) Icons.Default.StopCircle else Icons.Default.PlayCircle,
                    accentColor = if (isTrackRecording) Color.Red else Color.Green,
                    textColor = fontColor,
                    onClick = {
                        onToggleTrackRecording()
                        isExpanded = false
                    }
                )

                Divider(color = fontColor.copy(alpha = 0.08f))

                // Distance tool
                GISMenuButton(
                    label = "Measure Distance",
                    icon = Icons.Default.Straighten,
                    accentColor = accentColor,
                    textColor = fontColor,
                    onClick = {
                        onMeasureDistance()
                        isExpanded = false
                    }
                )

                // Area tool
                GISMenuButton(
                    label = "Calculate Area Map",
                    icon = Icons.Default.AllOut,
                    accentColor = accentColor,
                    textColor = fontColor,
                    onClick = {
                        onMeasureArea()
                        isExpanded = false
                    }
                )

                // Import GPX
                GISMenuButton(
                    label = "Import GPX Track",
                    icon = Icons.Default.CloudUpload,
                    accentColor = accentColor,
                    textColor = fontColor,
                    onClick = {
                        onImportGpx()
                        isExpanded = false
                    }
                )

                // Export GPX
                GISMenuButton(
                    label = "Export Geo-Data",
                    icon = Icons.Default.CloudDownload,
                    accentColor = accentColor,
                    textColor = fontColor,
                    onClick = {
                        onExportGpx()
                        isExpanded = false
                    }
                )

                Divider(color = fontColor.copy(alpha = 0.08f))

                // Layers Managers Catalog
                GISMenuButton(
                    label = "Manage Base Layers",
                    icon = Icons.Default.Layers,
                    accentColor = accentColor,
                    textColor = fontColor,
                    onClick = {
                        onManageLayers()
                        isExpanded = false
                    }
                )
            }
        }

        // Main Trigger circular control button (Satisfies 48dp action size targets)
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            containerColor = accentColor,
            contentColor = if (isOutdoorMode) Color.White else Color.Black,
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Architecture,
                contentDescription = "Toggle expandable GIS Menu controls panel",
                modifier = Modifier
                    .size(26.dp)
                    .rotate(if (isExpanded) 90f else 0f)
            )
        }
    }
}

@Composable
private fun GISMenuButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
