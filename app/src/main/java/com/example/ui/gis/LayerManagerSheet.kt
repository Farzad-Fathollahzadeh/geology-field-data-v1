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
import androidx.compose.foundation.shape.CircleShape
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
    onSeedDemoDataset: () -> Unit = {},
    onDismiss: () -> Unit,
    
    // Core Offline & Opacity features
    activeBaseMapOpacity: Float,
    onBaseMapOpacityChange: (Float) -> Unit,
    offlineModeForce: Boolean = false,
    onToggleOfflineModeForce: (Boolean) -> Unit = {},
    showOfflineBoundaries: Boolean = false,
    onToggleOfflineBoundaries: (Boolean) -> Unit = {},
    downloadedRegionsList: List<OfflineRegion> = emptyList(),
    onDownloadRegion: (String, List<String>, Double, Int, Double, Double) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteRegion: (String) -> Unit = {},
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    downloadStatusText: String = "",
    currentLat: Double = 0.0,
    currentLon: Double = 0.0,

    // Survey locations tile caching parameters (kept for backward compatibility)
    surveyStationsList: List<com.example.Station> = emptyList(),
    onDownloadSurveyLocationsTiles: (List<com.example.Station>, Double, List<String>, String) -> Unit = { _, _, _, _ -> },

    // Tracks library parameters
    savedTracks: List<com.example.Track> = emptyList(),
    onDeleteTrack: (Int) -> Unit = {},
    onSelectTrackForElevationProfile: (com.example.Track) -> Unit = {},
    onExportTrack: (com.example.Track) -> Unit = {},

    // MBTiles maps overlays integration
    activeMbtiles: Map<String, Float> = emptyMap(),
    importedMbtilesFiles: List<java.io.File> = emptyList(),
    onToggleMbtilesActive: (String, Boolean) -> Unit = { _, _ -> },
    onMbtilesOpacityChange: (String, Float) -> Unit = { _, _ -> },
    onDeleteMbtiles: (String) -> Unit = {},
    onImportMbtilesClick: () -> Unit = {},
    onImportGisVectorClick: () -> Unit = {},
    importedFeaturesCount: Int = 0,
    onClearImportedFeatures: () -> Unit = {},
    onStartRuler: () -> Unit = {},
    onStartArea: () -> Unit = {},
    onShowStats: () -> Unit = {},
    onExportData: () -> Unit = {},

    modifier: Modifier = Modifier,
    isOutdoorMode: Boolean = false
) {
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
    val overlayBgColor = if (isOutdoorMode) Color(0xFFF9FBF9) else Color(0xFF0E1311)
    val cardColor = if (isOutdoorMode) Color.White else Color(0xFF161E1B)
    val textColor = if (isOutdoorMode) Color.Black else Color.White

    val mapProviders = listOf("Google Satellite", "Esri Satellite (No-Key)", "OpenTopoMap", "Offline Topo-Raster")
    var selectedTab by remember { mutableStateOf(0) } // 0: Maps & Layers, 1: Saved Walks, 2: Field Tools
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp)
            .background(overlayBgColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "OFFLINE GIS WORKSTATION / مدیریت نقشه‌ها",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (selectedTab == 0) "نقشه‌های آفلاین و لایه‌ها / Maps" else "مسیرهای پیمایش / Logged Walks",
                    fontSize = 17.sp,
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

        // --- 2 FOCUSED TABS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(textColor.copy(alpha = 0.05f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabButton(
                text = "نقشه‌ها و لایه‌ها",
                icon = Icons.Default.Layers,
                isSelected = selectedTab == 0,
                activeColor = accentColor,
                textColor = textColor,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "مسیرها (${savedTracks.size})",
                icon = Icons.Default.DirectionsWalk,
                isSelected = selectedTab == 1,
                activeColor = accentColor,
                textColor = textColor,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "ابزارها و آمار",
                icon = Icons.Default.Straighten,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedTab == 0) {
                // ==========================================
                // 🚀 ACTION: QUICK MAP & VECTOR IMPORT BAR
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Import MBTiles Button
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = accentColor.copy(alpha = 0.12f)
                        ),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onImportMbtilesClick() }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "+ افزودن نقشه MBTiles",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                            Text(
                                text = "تصاویر ماهواره‌ای و نقشه‌های ۱:۱۰۰k زمین‌شناسی",
                                fontSize = 9.sp,
                                color = textColor.copy(alpha = 0.7f),
                                lineHeight = 12.sp
                            )
                        }
                    }

                    // 2. Import Vector (KMZ / GeoJSON) Button
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF29B6F6).copy(alpha = 0.10f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF29B6F6).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onImportGisVectorClick() }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = Color(0xFF29B6F6),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "+ افزودن وکتور GIS",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF29B6F6)
                                )
                            }
                            Text(
                                text = "گسل‌ها، زون‌های کانسار، فایل‌های KMZ و GeoJSON",
                                fontSize = 9.sp,
                                color = textColor.copy(alpha = 0.7f),
                                lineHeight = 12.sp
                            )
                        }
                    }
                }

                // ==========================================
                // 🗺️ SECTION 1: MY OFFLINE MBTILES MAPS
                // ==========================================
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "نقشه‌های آفلاین من (MBTiles / SQLite)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                            Text(
                                text = "${importedMbtilesFiles.size} نقشه",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (importedMbtilesFiles.isEmpty()) {
                            // Helpful instructions for first-time use
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(textColor.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                    .border(1.dp, textColor.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "هنوز فایل نقشه‌ای وارد نشده است.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "• برای نمایش آفلاین کامل، فایل‌های .mbtiles یا .sqlite (خروجی SAS.Planet، QGIS یا Global Mapper) را با دکمه بالا وارد کنید.\n• فایل‌های نقشه زمین‌شناسی و تصاویر ماهواره‌ای با هر حجمی بدون نیاز به اینترنت لود می‌شوند.",
                                        fontSize = 10.sp,
                                        color = textColor.copy(alpha = 0.6f),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                importedMbtilesFiles.forEach { file ->
                                    val fileName = file.name
                                    val isActive = activeMbtiles.containsKey(fileName)
                                    val opacity = activeMbtiles[fileName] ?: 1.0f
                                    val sizeMb = String.format(Locale.US, "%.1f", file.length() / (1024.0 * 1024.0))

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isActive) accentColor.copy(alpha = 0.07f) else textColor.copy(alpha = 0.03f))
                                            .border(1.dp, if (isActive) accentColor.copy(alpha = 0.4f) else textColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Header Row: Status, File Name, and Delete
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = if (isActive) accentColor else textColor.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = fileName,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textColor,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "$sizeMb MB • ${if (isActive) "فعال روی نقشه" else "غیرفعال"}",
                                                        fontSize = 9.5.sp,
                                                        color = if (isActive) accentColor else textColor.copy(alpha = 0.5f),
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Switch(
                                                    checked = isActive,
                                                    onCheckedChange = { onToggleMbtilesActive(fileName, it) },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = accentColor,
                                                        uncheckedThumbColor = textColor.copy(alpha = 0.4f),
                                                        uncheckedTrackColor = textColor.copy(alpha = 0.1f)
                                                    ),
                                                    modifier = Modifier.scaleScaleHelper()
                                                )
                                                IconButton(
                                                    onClick = { onDeleteMbtiles(fileName) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "حذف نقشه",
                                                        tint = Color(0xFFEF5350).copy(alpha = 0.8f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Opacity Slider (Only if active)
                                        if (isActive) {
                                            Divider(color = textColor.copy(alpha = 0.06f))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "شفافیت نقشه (Opacity)",
                                                    fontSize = 10.sp,
                                                    color = textColor.copy(alpha = 0.7f)
                                                )
                                                Text(
                                                    text = "${(opacity * 100).toInt()}%",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Slider(
                                                value = opacity,
                                                onValueChange = { onMbtilesOpacityChange(fileName, it) },
                                                colors = SliderDefaults.colors(
                                                    thumbColor = accentColor,
                                                    activeTrackColor = accentColor,
                                                    inactiveTrackColor = textColor.copy(alpha = 0.12f)
                                                ),
                                                modifier = Modifier.height(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 📍 SECTION 2: IMPORTED VECTOR OVERLAYS
                // ==========================================
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFF29B6F6), modifier = Modifier.size(18.dp))
                                Text(
                                    text = "لایه‌های وکتور فعال (GeoJSON / KMZ)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                            if (importedFeaturesCount > 0) {
                                TextButton(
                                    onClick = onClearImportedFeatures,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("پاک‌سازی وکتورها", fontSize = 10.sp, color = Color(0xFFEF5350))
                                }
                            }
                        }

                        if (importedFeaturesCount > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF29B6F6).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF29B6F6), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "$importedFeaturesCount عارضه وکتور در حال حاضر روی نقشه ترسیم شده است",
                                        fontSize = 10.5.sp,
                                        color = textColor
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "هیچ لایه وکتوری لود نشده است. با دکمه «+ افزودن وکتور GIS» بالا می‌توانید خطوط گسل، عوارض لیتولوژی و فایل‌های گوگل ارث (KMZ/KML) را اضافه کنید.",
                                fontSize = 10.sp,
                                color = textColor.copy(alpha = 0.6f),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                // ==========================================
                // ⚙️ SECTION 3: GEOLOGICAL SYMBOLS & GRIDS
                // ==========================================
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "عوارض و نمادهای ساختاری نقشه",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        OverlaySwitchRow(
                            label = "نمادهای شیب و امتداد لایه‌بندی (Strike & Dip)",
                            checked = showSymbols,
                            onCheckedChange = onToggleSymbols,
                            textColor = textColor,
                            accentColor = accentColor
                        )

                        Divider(color = textColor.copy(alpha = 0.05f))

                        OverlaySwitchRow(
                            label = "نمایش ایستگاه‌های مشاهداتی پروژه (Stations)",
                            checked = showStations,
                            onCheckedChange = onToggleStations,
                            textColor = textColor,
                            accentColor = accentColor
                        )

                        Divider(color = textColor.copy(alpha = 0.05f))

                        OverlaySwitchRow(
                            label = "خطوط و امتداد گسل‌های زمین‌شناسی (Faults)",
                            checked = showFaultLines,
                            onCheckedChange = onToggleFaultLines,
                            textColor = textColor,
                            accentColor = Color(0xFFEF5350)
                        )

                        Divider(color = textColor.copy(alpha = 0.05f))

                        OverlaySwitchRow(
                            label = "شبکه‌بندی مختصات UTM (UTM Grid Lines)",
                            checked = showGrids,
                            onCheckedChange = onToggleGrids,
                            textColor = textColor,
                            accentColor = accentColor
                        )
                    }
                }

                // ==========================================
                // 🌐 SECTION 4: DEFAULT BASEMAP PROVIDER
                // ==========================================
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Text(
                                text = "نقشه پایه زمینه (Basemap Provider)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            mapProviders.forEach { provider ->
                                val isChosen = activeBaseMap == provider
                                val label = when {
                                    provider.contains("Google", ignoreCase = true) -> "گوگل ماهواره"
                                    provider.contains("Esri", ignoreCase = true) -> "اسری ماهواره"
                                    provider.contains("Topo", ignoreCase = true) -> "توپوگرافی"
                                    else -> "آفلاین تراز"
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChosen) accentColor.copy(alpha = 0.15f) else textColor.copy(alpha = 0.03f))
                                        .border(1.dp, if (isChosen) accentColor else textColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .clickable { onBaseMapChange(provider) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChosen) accentColor else textColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // Basemap Opacity
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("شفافیت نقشه زمینه", fontSize = 10.5.sp, color = textColor.copy(alpha = 0.7f))
                            Text("${(activeBaseMapOpacity * 100).toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = accentColor, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = activeBaseMapOpacity,
                            onValueChange = onBaseMapOpacityChange,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = textColor.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }

            } else if (selectedTab == 1) {
                // ==========================================
                // 🚶 TAB 1: SAVED TRACKS & ELEVATION PROFILES
                // ==========================================
                if (savedTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, textColor.copy(alpha = 0.08f)), RoundedCornerShape(12.dp))
                            .background(cardColor.copy(alpha = 0.5f))
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null, tint = textColor.copy(alpha = 0.25f), modifier = Modifier.size(36.dp))
                            Text(
                                text = "هیچ مسیر حرکت ذخیره شده‌ای یافت نشد",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = "برای ضبط مسیر پیمایش، از دکمه ضبط در منوی ابزار نقشه استفاده کنید.",
                                fontSize = 10.sp,
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
                                shape = RoundedCornerShape(10.dp),
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
            } else {
                // ==========================================
                // 🛠️ TAB 2: FIELD TOOLS & EXPORT
                // ==========================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ابزارهای اندازه‌گیری و محاسبات مهندسی / MEASUREMENT TOOLS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )

                    // 1. Distance Ruler
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onStartRuler() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Straighten, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("خط‌کش اندازه‌گیری مسافت (Distance Ruler)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Text("سنجش فاصله بین چند نقطه روی نقشه با کلیک روی عوارض", fontSize = 10.sp, color = textColor.copy(alpha = 0.6f))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textColor.copy(alpha = 0.4f))
                        }
                    }

                    // 2. Area Measurement
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onStartArea() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.AllOut, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("مساحی و محاسبه مساحت محدوده (Area Measurement)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Text("محاسبه مساحت زون کانی‌سازی، کانسار یا رخنمون به هکتار و مترمربع", fontSize = 10.sp, color = textColor.copy(alpha = 0.6f))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textColor.copy(alpha = 0.4f))
                        }
                    }

                    Divider(color = textColor.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "آمار و خروجی پروژه / PROJECT STATS & EXPORT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )

                    // 3. Project Stats Dashboard
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onShowStats() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = Color(0xFF29B6F6), modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("داشبورد آماری پروژه (Project Statistics)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Text("تعداد ایستگاه‌ها، نمونه‌ها، ساختارهای ثبت‌شده و خلاصه پیمایش", fontSize = 10.sp, color = textColor.copy(alpha = 0.6f))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textColor.copy(alpha = 0.4f))
                        }
                    }

                    // 4. Export GIS
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onExportData() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("استخراج خروجی نقشه‌ها (Export GPX / KML)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Text("خروجی ایستگاه‌ها، مسیرها و نقاط برای ArcGIS, QGIS و Google Earth", fontSize = 10.sp, color = textColor.copy(alpha = 0.6f))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textColor.copy(alpha = 0.4f))
                        }
                    }
                }
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
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(0.5.dp, borderCol, RoundedCornerShape(8.dp))
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
                modifier = Modifier.size(15.dp)
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
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
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
    return this.height(24.dp).padding(vertical = 0.dp)
}
