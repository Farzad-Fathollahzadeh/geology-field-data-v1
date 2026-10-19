package com.example.ui.gis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
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
import java.util.Locale

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
    isOutdoorMode: Boolean = false,
    
    // New parameters to support unified controls
    zoomLevel: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    
    showMoveSimulator: Boolean,
    onToggleMoveSimulator: () -> Unit,
    
    baseMapOpacity: Float,
    onBaseMapOpacityChange: (Float) -> Unit,
    
    showGrids: Boolean,
    onToggleGrids: (Boolean) -> Unit,
    
    isSimpleMode: Boolean,
    onToggleSimpleMode: () -> Unit,
    
    onShowWaypointSelector: () -> Unit,
    onShowHelp: () -> Unit,
    onShowStats: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
    val fontColor = if (isOutdoorMode) Color.Black else Color.White
    val cardBg = if (isOutdoorMode) Color.White else Color(0xFF0C100E)
    val itemBgColor = if (isOutdoorMode) Color(0xFFF2F4F2) else Color(0xFF151C1A)
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Dropdown actions menu panel
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                    expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, fontColor.copy(alpha = 0.15f)),
                modifier = Modifier
                    .width(280.dp)
                    .heightIn(max = 480.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ابزارها و کنترل نقشه",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Text(
                                text = "GIS CONSOLE & CONTROLS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = fontColor.copy(alpha = 0.5f)
                            )
                        }
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = fontColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Divider(color = fontColor.copy(alpha = 0.1f), thickness = 1.dp)

                    // SECTION 1: Exploration Logging
                    MenuSectionHeader("ثبت اطلاعات اکتشافی / LOG DATA", accentColor, fontColor)
                    
                    GISMenuButton(
                        label = "ایستگاه مشاهداتی (نقطه) | Log Station",
                        icon = Icons.Default.AddLocationAlt,
                        accentColor = accentColor,
                        textColor = fontColor,
                        onClick = {
                            onAddObservation()
                            isExpanded = false
                        }
                    )

                    GISMenuButton(
                        label = "برداشت مغزه نمونه | Core Sample",
                        icon = Icons.Default.Science,
                        accentColor = accentColor,
                        textColor = fontColor,
                        onClick = {
                            onAddSample()
                            isExpanded = false
                        }
                    )

                    GISMenuButton(
                        label = "کمپاس و شیب‌یاب | Strike & Dip",
                        icon = Icons.Default.Explore,
                        accentColor = accentColor,
                        textColor = fontColor,
                        onClick = {
                            onAddStructural()
                            isExpanded = false
                        }
                    )

                    GISMenuButton(
                        label = "گمانه اکتشافی | Borehole Collar",
                        icon = Icons.Default.Layers,
                        accentColor = accentColor,
                        textColor = fontColor,
                        onClick = {
                            onAddBorehole()
                            isExpanded = false
                        }
                    )

                    GISMenuButton(
                        label = "هدف‌گذاری و ناوبری | Waypoint Target",
                        icon = Icons.Default.LocationSearching,
                        accentColor = accentColor,
                        textColor = fontColor,
                        onClick = {
                            onShowWaypointSelector()
                            isExpanded = false
                        }
                    )

                    Divider(color = fontColor.copy(alpha = 0.06f))

                    // SECTION 2: Map Controls & View Tuning
                    MenuSectionHeader("تنظیمات و نمایش نقشه / MAP TUNING", accentColor, fontColor)

                    // Zoom Controls row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(itemBgColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بزرگنمایی / Zoom: $zoomLevel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = fontColor
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = onZoomOut,
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(fontColor.copy(alpha = 0.08f), CircleShape)
                            ) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = fontColor, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = onZoomIn,
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(fontColor.copy(alpha = 0.08f), CircleShape)
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = fontColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Opacity Slider Block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(itemBgColor, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("شفافیت نقشه پایه / Opacity", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fontColor)
                            Text("${(baseMapOpacity * 100).toInt()}%", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = baseMapOpacity,
                            onValueChange = onBaseMapOpacityChange,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = fontColor.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    // Grid lines toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(itemBgColor, RoundedCornerShape(8.dp))
                            .clickable { onToggleGrids(!showGrids) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Text("شبکه‌بندی UTM / Grid Lines", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fontColor)
                        }
                        Switch(
                            checked = showGrids,
                            onCheckedChange = onToggleGrids,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accentColor,
                                checkedTrackColor = accentColor.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .height(20.dp)
                                .width(36.dp)
                        )
                    }

                    Divider(color = fontColor.copy(alpha = 0.06f))

                    // SECTION 3: Tracking & Simulation
                    MenuSectionHeader("مسیر و شبیه‌ساز / TRACKS & SIM", accentColor, fontColor)

                    // Track Recording Trigger
                    GISMenuButton(
                        label = if (isTrackRecording) "توقف ضبط مسیر | Stop GPS Track" else "ضبط مسیر جی‌پی‌اس | Record GPS Track",
                        icon = if (isTrackRecording) Icons.Default.StopCircle else Icons.Default.PlayCircle,
                        accentColor = if (isTrackRecording) Color.Red else Color.Green,
                        textColor = fontColor,
                        onClick = {
                            onToggleTrackRecording()
                            isExpanded = false
                        }
                    )

                    // Move simulator toggle
                    GISMenuButton(
                        label = if (showMoveSimulator) "پنهان‌سازی کنترل شبیه‌ساز | Hide Sim" else "شبیه‌ساز راه رفتن | Walk Simulator",
                        icon = Icons.Default.DirectionsWalk,
                        accentColor = if (showMoveSimulator) accentColor else fontColor.copy(alpha = 0.7f),
                        textColor = fontColor,
                        onClick = {
                            onToggleMoveSimulator()
                            isExpanded = false
                        }
                    )

                    Divider(color = fontColor.copy(alpha = 0.06f))

                    // SECTION 4: GIS Utilities
                    MenuSectionHeader("ابزارها و فایل‌ها / GIS UTILITIES", accentColor, fontColor)

                    GISMenuButton(
                        label = "لایه‌ها و نقشه‌ها | Manage Layers",
                        icon = Icons.Default.Layers,
                        accentColor = accentColor,
                        textColor = fontColor,
                        onClick = {
                            onManageLayers()
                            isExpanded = false
                        }
                    )

                    GISMenuButton(
                        label = "سنجش فاصله | Measure Distance",
                        icon = Icons.Default.Straighten,
                        accentColor = accentColor,
                        textColor = fontColor,
                        onClick = {
                            onMeasureDistance()
                            isExpanded = false
                        }
                    )

                    GISMenuButton(
                        label = "محاسبه مساحت | Measure Area",
                        icon = Icons.Default.AllOut,
                        accentColor = accentColor,
                        textColor = fontColor,
                        onClick = {
                            onMeasureArea()
                            isExpanded = false
                        }
                    )

                    GISMenuButton(
                        label = "مرکز نقشه‌های آفلاین (MBTiles/GeoJSON)",
                        icon = Icons.Default.CloudDownload,
                        accentColor = accentColor,
                        textColor = fontColor,
                        onClick = {
                            onImportGpx()
                            isExpanded = false
                        }
                    )

                    GISMenuButton(
                        label = "خروجی نقشه و داده‌ها | Export GIS",
                        icon = Icons.Default.CloudUpload,
                        accentColor = accentColor,
                        textColor = fontColor,
                        onClick = {
                            onExportGpx()
                            isExpanded = false
                        }
                    )

                    Divider(color = fontColor.copy(alpha = 0.06f))

                    // SECTION 5: SUPPORT & GUIDE
                    MenuSectionHeader("آمارهای پروژه و راهنما / UTILS & GUIDE", accentColor, fontColor)

                    GISMenuButton(
                        label = "داشبورد آماری پروژه | Project Stats",
                        icon = Icons.Default.BarChart,
                        accentColor = accentColor,
                        textColor = fontColor,
                        onClick = {
                            onShowStats()
                            isExpanded = false
                        }
                    )

                    GISMenuButton(
                        label = "راهنمای تعاملی جی‌آی‌اس | GIS Interactive Guide",
                        icon = Icons.Default.HelpOutline,
                        accentColor = accentColor,
                        textColor = fontColor,
                        onClick = {
                            onShowHelp()
                            isExpanded = false
                        }
                    )
                }
            }
        }

        // Main Trigger circular control button
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            containerColor = accentColor,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.MenuOpen,
                contentDescription = "Toggle expandable GIS Menu",
                modifier = Modifier
                    .size(26.dp)
                    .rotate(if (isExpanded) 90f else 0f)
            )
        }
    }
}

@Composable
private fun MenuSectionHeader(
    text: String,
    accentColor: Color,
    fontColor: Color
) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = accentColor,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
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
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
