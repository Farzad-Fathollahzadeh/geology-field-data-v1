package com.example.ui.gis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

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
    
    // Core Offline & Opacity features
    activeBaseMapOpacity: Float,
    onBaseMapOpacityChange: (Float) -> Unit,
    offlineModeForce: Boolean,
    onToggleOfflineModeForce: (Boolean) -> Unit,
    showOfflineBoundaries: Boolean,
    onToggleOfflineBoundaries: (Boolean) -> Unit,
    downloadedRegionsList: List<OfflineRegion>,
    onDownloadRegion: (String) -> Unit,
    onDeleteRegion: (String) -> Unit,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadStatusText: String,
    currentLat: Double,
    currentLon: Double,

    // Tracks library parameters
    savedTracks: List<com.example.Track> = emptyList(),
    onDeleteTrack: (Int) -> Unit = {},
    onSelectTrackForElevationProfile: (com.example.Track) -> Unit = {},
    onExportTrack: (com.example.Track) -> Unit = {},

    modifier: Modifier = Modifier,
    isOutdoorMode: Boolean = false
) {
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
    val overlayBgColor = if (isOutdoorMode) Color(0xFFF9FBF9) else Color(0xFF0E1311)
    val cardColor = if (isOutdoorMode) Color.White else Color(0xFF161E1B)
    val textColor = if (isOutdoorMode) Color.Black else Color.White

    val mapProviders = listOf("Esri Satellite (No-Key)", "Google Satellite", "OpenTopoMap", "OpenStreetMap", "Offline Topo-Raster")
    var selectedTab by remember { mutableStateOf(0) } // 0: Layers & Opacity, 1: Offline Caching, 2: Tracks
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 580.dp)
            .background(overlayBgColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FIELD GIS WORKSTATION / میز کار زمین‌شناسی",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (selectedTab == 0) "Configure Layers / لایه‌های نقشه" else if (selectedTab == 1) "Offline Cache / ذخیره آفلاین" else "Logged Walks / کتابخانه مسیرها",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close GIS panel",
                    tint = textColor.copy(alpha = 0.6f)
                )
            }
        }

        // --- TAB BAR CONTROL ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(textColor.copy(alpha = 0.05f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabButton(
                text = "Layers / لایه‌ها",
                icon = Icons.Default.Layers,
                isSelected = selectedTab == 0,
                activeColor = accentColor,
                textColor = textColor,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "Offline / ذخیره",
                icon = Icons.Default.OfflineBolt,
                isSelected = selectedTab == 1,
                activeColor = accentColor,
                textColor = textColor,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "Walks / مسیرها",
                icon = Icons.Default.DirectionsWalk,
                isSelected = selectedTab == 2,
                activeColor = accentColor,
                textColor = textColor,
                onClick = { selectedTab = 2 },
                modifier = Modifier.weight(1f)
            )
        }

        Divider(color = textColor.copy(alpha = 0.08f), thickness = 1.dp)

        // --- CONTENT COLUMN ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedTab == 0) {
                // TAB 0: LAYER PROTOCOLS & STAC OPACITIES
                
                // RASTER TILES SELECTION CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Text(
                                text = "BASEMAP RASTER PROVIDER / نوع نقشه پایه",
                                fontSize = 11.sp,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                mapProviders.take(2).forEach { provider ->
                                    val isChosen = activeBaseMap == provider
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isChosen) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(1.dp, if (isChosen) accentColor else textColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                            .clickable { onBaseMapChange(provider) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = provider,
                                            fontSize = 11.sp,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isChosen) accentColor else textColor
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                mapProviders.drop(2).forEach { provider ->
                                    val isChosen = activeBaseMap == provider
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isChosen) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(1.dp, if (isChosen) accentColor else textColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                            .clickable { onBaseMapChange(provider) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = provider,
                                            fontSize = 11.sp,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isChosen) accentColor else textColor
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // OPACITY SLIDER CONTROLLER
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Basemap Opacity / شفافیت نقشه پایه",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "${(activeBaseMapOpacity * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Slider(
                                value = activeBaseMapOpacity,
                                onValueChange = onBaseMapOpacityChange,
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor,
                                    inactiveTrackColor = textColor.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }

                // GIS VECTOR OVERLAYS VISIDILITY
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.FilterFrames, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Text(
                                text = "VECTOR DATA VISIBILITY / قابلیت رویت لایه‌ها",
                                fontSize = 11.sp,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        OverlaySwitchRow(
                            label = "Coordinate Grid lines / بیضی شبکه قائم UTM",
                            checked = showGrids,
                            onCheckedChange = onToggleGrids,
                            textColor = textColor,
                            accentColor = accentColor
                        )

                        OverlaySwitchRow(
                            label = "Stations & Field Outcrops / ایستگاه‌ها و برون‌زدها",
                            checked = showStations,
                            onCheckedChange = onToggleStations,
                            textColor = textColor,
                            accentColor = accentColor
                        )

                        OverlaySwitchRow(
                            label = "Bedding Strike-Dips / شیب و امتداد لایه‌ها",
                            checked = showSymbols,
                            onCheckedChange = onToggleSymbols,
                            textColor = textColor,
                            accentColor = accentColor
                        )

                        OverlaySwitchRow(
                            label = "Fault Contacts & Lines / گسل‌ها و درزه‌های فعال",
                            checked = showFaultLines,
                            onCheckedChange = onToggleFaultLines,
                            textColor = textColor,
                            accentColor = accentColor
                        )
                    }
                }

                // GIS SEED DEMO CONTROL
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeedDemoDataset() }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.BackupTable, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Load Representative Outcrop Dataset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = "Instantly populates active grids with test structural beddings.",
                                fontSize = 10.sp,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = textColor.copy(alpha = 0.3f))
                    }
                }

            } else if (selectedTab == 1) {
                // TAB 1: LOCAL OFFLINE CACHE STORAGE ENGINE
                
                // FORCE OFFLINE DISCONNECT / SYSTEM STATUS CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(1.dp, if (offlineModeForce) accentColor else textColor.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (offlineModeForce) Color(0xFFFFA726) else Color(0xFF66BB6A))
                                )
                                Text(
                                    text = if (offlineModeForce) "SYSTEM OFFLINE: ENFORCED (HIGH-RISK SAFE) / آفلاین اجباری" else "CONNECTED FIELD WORKSTATION / کارگاه زمین‌شناسی متصل",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (offlineModeForce) Color(0xFFFFA726) else Color(0xFF66BB6A),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        OverlaySwitchRow(
                            label = "Force Offline Safe Mode / اجبار کارکرد کامل آفلاین",
                            checked = offlineModeForce,
                            onCheckedChange = { onToggleOfflineModeForce(it) },
                            textColor = textColor,
                            accentColor = accentColor
                        )
                        Text(
                            text = "تمام اتصالات یا پردازش‌های همگام‌سازی نقشه پایه را مسدود می‌کند. نقشه را کاملاً متکی بر حافظه آفلاین و SQLite محلی کرده تا بدون هیچ ریسکی در عمق معدن کار کنید.",
                            fontSize = 9.5.sp,
                            color = textColor.copy(alpha = 0.5f),
                            lineHeight = 13.sp
                        )

                        Divider(color = textColor.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

                        OverlaySwitchRow(
                            label = "Draw Cached Map Boundaries / رسم محدوده بسته‌های نقشه",
                            checked = showOfflineBoundaries,
                            onCheckedChange = { onToggleOfflineBoundaries(it) },
                            textColor = textColor,
                            accentColor = accentColor
                        )
                    }
                }

                // DOWNLOADING ENGINE / SIMULATOR PACKET COMPILER
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Text(
                                text = "OFFLINE COMPILER & DOWNLOADER / دانلودکننده آفلاین",
                                fontSize = 11.sp,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "تنفس و دانلود نقشه ماهواره‌ای/توپوگرافی در شعاع ۵ کیلومتری موقعیت طرح جهت استفاده بدون دسترسی به شبکه تلفن همراه.",
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bound Lat: ${currentLat.toString().take(7)} | Lon: ${currentLon.toString().take(7)}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = textColor.copy(alpha = 0.7f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "GRID RANGE: 5 KM",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        var inputRegionName by remember { mutableStateOf("") }
                        
                        OutlinedTextField(
                            value = inputRegionName,
                            onValueChange = { inputRegionName = it },
                            placeholder = { Text("E.g. Northern Fold Quad-2", fontSize = 11.sp) },
                            label = { Text("Offline Packet Label / نام بسته آفلاین", fontSize = 11.sp) },
                            singleLine = true,
                            enabled = !isDownloading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                                focusedLabelColor = accentColor,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isDownloading) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = downloadStatusText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = downloadProgress,
                                    color = accentColor,
                                    trackColor = textColor.copy(alpha = 0.1f),
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    onDownloadRegion(inputRegionName)
                                    inputRegionName = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.DownloadForOffline, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Text("شروع دانلود بسته نقشه / START DOWNLOAD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // DOWNLOADED OFFLINE MAP PACKETS LIST
                Text(
                    text = "STORED REGIONAL SECTOR CACHES (${downloadedRegionsList.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )

                if (downloadedRegionsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, textColor.copy(alpha = 0.05f)), RoundedCornerShape(8.dp))
                            .background(cardColor.copy(alpha = 0.5f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudQueue, contentDescription = null, tint = textColor.copy(alpha = 0.2f), modifier = Modifier.size(32.dp))
                            Text(
                                text = "No local offline map packages compiled",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.4f)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        downloadedRegionsList.forEach { region ->
                            OfflineRegionItem(
                                region = region,
                                onDelete = { onDeleteRegion(region.id) },
                                isOutdoor = isOutdoorMode,
                                accentColor = accentColor,
                                cardColor = cardColor,
                                textColor = textColor
                            )
                        }
                    }
                }
            } else {
                // TAB 2: SAVED TRACKS & EXPORTS
                if (savedTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, textColor.copy(alpha = 0.05f)), RoundedCornerShape(8.dp))
                            .background(cardColor.copy(alpha = 0.5f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null, tint = textColor.copy(alpha = 0.2f), modifier = Modifier.size(32.dp))
                            Text(
                                text = "هیچ مسیر حرکت ذخیره شده‌ای یافت نشد",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = "جهت شروع ضبط مسیر، از منوی ابزار نقشه اقدام کنید.",
                                fontSize = 9.5.sp,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        savedTracks.forEach { track ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardColor),
                                border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(accentColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsWalk,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                            Text(
                                                text = track.name,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val km = track.lengthMeters / 1000.0
                                                val distText = if (track.lengthMeters >= 1000.0) String.format(Locale.US, "%.2f km", km) else "${track.lengthMeters.toInt()} m"
                                                Text(
                                                    text = distText,
                                                    fontSize = 10.sp,
                                                    color = accentColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                val h = track.durationSeconds / 3600
                                                val m = (track.durationSeconds % 3600) / 60
                                                val s = track.durationSeconds % 60
                                                val durationText = if (h > 0) "${h}h ${m}m" else "${m}m ${s}s"
                                                Text(
                                                    text = durationText,
                                                    fontSize = 10.sp,
                                                    color = textColor.copy(alpha = 0.5f),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                val dateText = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(track.createdAt))
                                                Text(
                                                    text = "• $dateText",
                                                    fontSize = 9.sp,
                                                    color = textColor.copy(alpha = 0.4f)
                                                )
                                            }
                                        }
                                    }

                                    // Action buttons row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Elevation Profile Action
                                        IconButton(
                                            onClick = { onSelectTrackForElevationProfile(track) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Terrain,
                                                contentDescription = "نمودار ارتفاعی",
                                                tint = accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // GPX Export Action
                                        IconButton(
                                            onClick = { onExportTrack(track) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "اشتراک‌گذاری GPX",
                                                tint = accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Delete Action
                                        IconButton(
                                            onClick = { onDeleteTrack(track.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteForever,
                                                contentDescription = "حذف مسیر",
                                                tint = Color(0xFFEF5350).copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineRegionItem(
    region: OfflineRegion,
    onDelete: () -> Unit,
    isOutdoor: Boolean,
    accentColor: Color,
    cardColor: Color,
    textColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2E7D32).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Saved offline",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = region.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${region.sizeMb} MB",
                            fontSize = 10.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = region.zoomRange,
                            fontSize = 9.sp,
                            color = textColor.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "• ${region.dateDownloaded}",
                            fontSize = 9.sp,
                            color = textColor.copy(alpha = 0.4f)
                        )
                    }
                    Text(
                        text = "Lat: [${region.minLat}~${region.maxLat}] | Lon: [${region.minLon}~${region.maxLon}]",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textColor.copy(alpha = 0.4f)
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Delete cache",
                    tint = Color(0xFFEF5350).copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent
    val borderCol = if (isSelected) activeColor else Color.Transparent
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(0.5.dp, borderCol, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeColor else textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) activeColor else textColor.copy(alpha = 0.7f)
            )
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
            fontSize = 11.5.sp,
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
            ),
            modifier = Modifier.scaleScaleHelper()
        )
    }
}

@Composable
private fun Modifier.scaleScaleHelper(): Modifier {
    // scale switch down slightly for elegant tight layout spacing
    return this.height(24.dp).padding(vertical = 0.dp)
}
