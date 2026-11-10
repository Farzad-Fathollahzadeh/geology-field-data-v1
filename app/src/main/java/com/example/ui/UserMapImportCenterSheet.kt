package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMapImportCenterSheet(
    viewModel: GeologyViewModel,
    onDismiss: () -> Unit,
    onZoomToCoordinates: (Double, Double, Int) -> Unit = { _, _, _ -> },
    onZoomToBounds: (Double, Double, Double, Double) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier,
    isOutdoorMode: Boolean = true,
    mapCenterLat: Double? = null,
    mapCenterLon: Double? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()

    val calibratedMaps by viewModel.calibratedMaps.collectAsStateWithLifecycle()
    val activeMbtilesMap by viewModel.activeMbtilesMap.collectAsStateWithLifecycle()
    val importedMbtilesFiles by viewModel.importedMbtilesFiles.collectAsStateWithLifecycle()
    val importedFeatures by viewModel.importedFeatures.collectAsStateWithLifecycle()
    val customTileUrl by viewModel.customTileUrl.collectAsStateWithLifecycle()
    val useCustomTile by viewModel.useCustomTile.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()

    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF00E5FF)
    val cardBg = if (isOutdoorMode) Color.White else Color(0xFF141A17)
    val sheetBg = if (isOutdoorMode) Color(0xFFF7F9F7) else Color(0xFF0B100D)
    val textColor = if (isOutdoorMode) Color.Black else Color.White

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = MBTiles, 1 = Calibrated Image, 2 = Vector (KML/GPX/GeoJSON), 3 = Custom XYZ URL
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var selectedImageUriForCalibration by remember { mutableStateOf<Uri?>(null) }
    var isImportingMbtiles by remember { mutableStateOf(false) }
    var importStatusText by remember { mutableStateOf("") }

    // File pickers
    val mbtilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            isImportingMbtiles = true
            importStatusText = if (isFarsiMode) "در حال بارگذاری فایل نقشه آفلاین..." else "Importing MBTiles database..."
            coroutineScope.launch {
                val res = withContext(Dispatchers.IO) {
                    UserMapManager.importMbtilesFromUri(context, it) { _, status ->
                        importStatusText = status
                    }
                }
                withContext(Dispatchers.Main) {
                    isImportingMbtiles = false
                    res.onSuccess {
                        viewModel.refreshUserMaps()
                        val msg = if (isFarsiMode) "نقشه آفلاین با موفقیت بارگذاری و فعال شد!" else "Offline MBTiles map activated!"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "خطا در ورود نقشه: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUriForCalibration = it
            showCalibrationDialog = true
        }
    }

    val vectorPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importMapFile(context, it)
        }
    }

    // Sheet container
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 680.dp)
            .background(sheetBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = if (isFarsiMode) "مرکز ورود نقشه‌های کاربر" else "User Map Import Center",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = if (isFarsiMode) "بارگذاری نقشه آفلاین، تصاویر کالیبره‌شده و لایه‌های GIS" else "Load offline MBTiles, calibrated geological scans & vectors",
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.65f)
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = textColor.copy(alpha = 0.7f)
                )
            }
        }

        // Active layers summary badge
        val totalActiveUserLayers = activeMbtilesMap.size + calibratedMaps.count { it.isVisible } + if (importedFeatures.isNotEmpty()) 1 else 0
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isFarsiMode) "وضعیت لایه‌های کاربر:" else "Active user overlays:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
            Text(
                text = if (isFarsiMode) "$totalActiveUserLayers لایه فعال" else "$totalActiveUserLayers active layer(s)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }

        // --- TAB BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(textColor.copy(alpha = 0.05f))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                Pair(if (isFarsiMode) "MBTiles آفلاین" else "MBTiles", Icons.Default.Map),
                Pair(if (isFarsiMode) "تصویر کالیبره‌شده" else "Calibrated Map", Icons.Default.Image),
                Pair(if (isFarsiMode) "برداری (KML/GPX)" else "Vectors", Icons.Default.Polyline),
                Pair(if (isFarsiMode) "SAS.Planet / XYZ" else "SAS / ZMP", Icons.Default.Language)
            )

            tabs.forEachIndexed { index, (label, icon) ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) accentColor else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = label,
                            fontSize = 9.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else textColor.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Divider(color = textColor.copy(alpha = 0.08f), thickness = 1.dp)

        // --- TAB CONTENT ---
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> MbtilesTabContent(
                    importedFiles = importedMbtilesFiles,
                    activeMbtiles = activeMbtilesMap,
                    isImporting = isImportingMbtiles,
                    statusText = importStatusText,
                    onImportClick = {
                        mbtilesLauncher.launch(arrayOf("*/*", "application/octet-stream", "application/x-sqlite3"))
                    },
                    onToggleActive = { name, active -> viewModel.toggleMbtilesActive(name, active) },
                    onOpacityChange = { name, op -> viewModel.setMbtilesOpacity(name, op) },
                    onDelete = { name -> viewModel.deleteMbtiles(name) },
                    onZoomToExtent = { file ->
                        val bounds = UserMapManager.getMbtilesBounds(file)
                        if (bounds != null) {
                            onZoomToBounds(bounds[1], bounds[0], bounds[3], bounds[2])
                            onDismiss()
                        } else {
                            Toast.makeText(context, if (isFarsiMode) "مختصات محدوده در متادیتای فایل یافت نشد." else "Bounds not found in metadata", Toast.LENGTH_SHORT).show()
                        }
                    },
                    accentColor = accentColor,
                    textColor = textColor,
                    cardBg = cardBg,
                    isFarsiMode = isFarsiMode
                )

                1 -> CalibratedMapTabContent(
                    calibratedMaps = calibratedMaps,
                    onAddNewClick = { imagePickerLauncher.launch("image/*") },
                    onToggleVisible = { id, vis -> viewModel.toggleCalibratedMap(id, vis) },
                    onOpacityChange = { id, op -> viewModel.setCalibratedMapOpacity(id, op) },
                    onDelete = { id -> viewModel.deleteCalibratedMap(id) },
                    onZoomToMap = { map ->
                        onZoomToBounds(map.southLat, map.westLon, map.northLat, map.eastLon)
                        onDismiss()
                    },
                    accentColor = accentColor,
                    textColor = textColor,
                    cardBg = cardBg,
                    isFarsiMode = isFarsiMode
                )

                2 -> VectorLayersTabContent(
                    importedFeatures = importedFeatures,
                    onImportKmlKmz = {
                        vectorPickerLauncher.launch(arrayOf("application/vnd.google-earth.kml+xml", "application/vnd.google-earth.kmz", "application/xml", "text/xml", "*/*"))
                    },
                    onImportGpx = {
                        vectorPickerLauncher.launch(arrayOf("application/gpx+xml", "application/xml", "*/*"))
                    },
                    onImportGeoJson = {
                        vectorPickerLauncher.launch(arrayOf("application/geo+json", "application/json", "*/*"))
                    },
                    onClearFeatures = {
                        viewModel.clearImportedFeatures()
                        val msg = if (isFarsiMode) "لایه‌های برداری پاک‌سازی شدند." else "Vector overlays cleared."
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onZoomToFeatures = {
                        val coords = importedFeatures.flatMap { it.coordinates }
                        if (coords.isNotEmpty()) {
                            val minLat = coords.minOf { it.lat }
                            val maxLat = coords.maxOf { it.lat }
                            val minLon = coords.minOf { it.lon }
                            val maxLon = coords.maxOf { it.lon }
                            onZoomToBounds(minLat, minLon, maxLat, maxLon)
                            onDismiss()
                        }
                    },
                    accentColor = accentColor,
                    textColor = textColor,
                    cardBg = cardBg,
                    isFarsiMode = isFarsiMode
                )

                3 -> CustomTileServerTabContent(
                    viewModel = viewModel,
                    accentColor = accentColor,
                    textColor = textColor,
                    cardBg = cardBg,
                    isFarsiMode = isFarsiMode,
                    mapCenterLat = mapCenterLat,
                    mapCenterLon = mapCenterLon
                )
            }
        }
    }

    // Calibration Dialog when user selects an image
    if (showCalibrationDialog && selectedImageUriForCalibration != null) {
        CalibrateImageDialog(
            imageUri = selectedImageUriForCalibration!!,
            currentLat = liveLocation?.latitude ?: 35.69439,
            currentLon = liveLocation?.longitude ?: 51.42151,
            onDismiss = {
                showCalibrationDialog = false
                selectedImageUriForCalibration = null
            },
            onSave = { name, north, south, east, west, opacity ->
                coroutineScope.launch {
                    val res = withContext(Dispatchers.IO) {
                        UserMapManager.importAndCalibrateImage(
                            context = context,
                            uri = selectedImageUriForCalibration!!,
                            name = name,
                            northLat = north,
                            southLat = south,
                            eastLon = east,
                            westLon = west,
                            opacity = opacity
                        )
                    }
                    withContext(Dispatchers.Main) {
                        showCalibrationDialog = false
                        selectedImageUriForCalibration = null
                        res.onSuccess {
                            viewModel.refreshUserMaps()
                            val msg = if (isFarsiMode) "تصویر زمین‌شناسی با موفقیت کالیبره و به نقشه افزوده شد!" else "Calibrated map image overlay added successfully!"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }.onFailure { err ->
                            Toast.makeText(context, "خطا در ذخیره تصویر: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            accentColor = accentColor,
            isFarsiMode = isFarsiMode
        )
    }
}

// -------------------------------------------------------------
// TAB 0: MBTILES OFFLINE MAPS
// -------------------------------------------------------------
@Composable
private fun MbtilesTabContent(
    importedFiles: List<File>,
    activeMbtiles: Map<String, Float>,
    isImporting: Boolean,
    statusText: String,
    onImportClick: () -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    onOpacityChange: (String, Float) -> Unit,
    onDelete: (String) -> Unit,
    onZoomToExtent: (File) -> Unit,
    accentColor: Color,
    textColor: Color,
    cardBg: Color,
    isFarsiMode: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Import Action Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFarsiMode) "ورود پایگاه داده نقشه آفلاین (MBTiles)" else "Import MBTiles Offline Map",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = if (isFarsiMode) "فرمت استاندارد نقشه‌های آفلاین خروجی از SAS.Planet، QGIS و MOBAC" else "Industry standard offline map tiles from SAS.Planet or QGIS",
                            fontSize = 10.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }

                    Button(
                        onClick = onImportClick,
                        enabled = !isImporting,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFarsiMode) "انتخاب فایل" else "Select File",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isImporting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = accentColor)
                        Text(text = statusText, fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // List of Imported MBTiles files
        Text(
            text = if (isFarsiMode) "فایل‌های MBTiles بارگذاری‌شده (${importedFiles.size})" else "Loaded MBTiles Files (${importedFiles.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        if (importedFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LayersClear,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = if (isFarsiMode) "هنوز فایل نقشه‌ای وارد نشده است." else "No MBTiles maps imported yet.",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (isFarsiMode) "روی دکمه «انتخاب فایل» در بالا بزنید." else "Tap 'Select File' above to import .mbtiles",
                        fontSize = 10.sp,
                        color = accentColor
                    )
                }
            }
        } else {
            importedFiles.forEach { file ->
                val fileName = file.name
                val isActive = activeMbtiles.containsKey(fileName)
                val opacity = activeMbtiles[fileName] ?: 0.95f
                val fileSizeMb = file.length() / (1024.0 * 1024.0)

                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(0.5.dp, if (isActive) accentColor.copy(alpha = 0.4f) else textColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isActive,
                                    onCheckedChange = { onToggleActive(fileName, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                )
                                Column {
                                    Text(
                                        text = fileName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1f MB • %s", fileSizeMb, if (isActive) (if (isFarsiMode) "فعال در نمایش نقشه" else "Active on map") else (if (isFarsiMode) "غیرفعال" else "Inactive")),
                                        fontSize = 10.sp,
                                        color = if (isActive) accentColor else textColor.copy(alpha = 0.5f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { onZoomToExtent(file) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomInMap,
                                        contentDescription = "Zoom to bounds",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(fileName) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (isActive) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isFarsiMode) "شفافیت: ${(opacity * 100).toInt()}%" else "Opacity: ${(opacity * 100).toInt()}%",
                                    fontSize = 10.sp,
                                    color = textColor.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(80.dp)
                                )
                                Slider(
                                    value = opacity,
                                    onValueChange = { onOpacityChange(fileName, it) },
                                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                                    modifier = Modifier.weight(1f).height(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: CALIBRATED MAP IMAGE OVERLAYS
// -------------------------------------------------------------
@Composable
private fun CalibratedMapTabContent(
    calibratedMaps: List<CalibratedMap>,
    onAddNewClick: () -> Unit,
    onToggleVisible: (String, Boolean) -> Unit,
    onOpacityChange: (String, Float) -> Unit,
    onDelete: (String) -> Unit,
    onZoomToMap: (CalibratedMap) -> Unit,
    accentColor: Color,
    textColor: Color,
    cardBg: Color,
    isFarsiMode: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Add Calibrated Image Action Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFarsiMode) "کالیبراسیون تصویر زمین‌شناسی" else "Calibrate Geological Image",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = if (isFarsiMode) "بارگذاری عکس نقشه یا برگه ۱:۱۰۰،۰۰۰ با ورود مختصات چهارگوشه یا UTM" else "Overlay custom scanned geological quadrangle or aerial photo with corner coordinates",
                            fontSize = 10.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }

                    Button(
                        onClick = onAddNewClick,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFarsiMode) "انتخاب عکس" else "Pick Image",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // List of Calibrated Maps
        Text(
            text = if (isFarsiMode) "نقشه‌های کالیبره‌شده (${calibratedMaps.size})" else "Calibrated Map Overlays (${calibratedMaps.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        if (calibratedMaps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HideImage,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = if (isFarsiMode) "هنوز تصویر نقشه‌ای کالیبره نشده است." else "No calibrated map overlays yet.",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (isFarsiMode) "تصویر نقشه زمین‌شناسی یا پلان معدن خود را انتخاب و کالیبره کنید." else "Pick a geological sheet or mine plan image to georeference.",
                        fontSize = 10.sp,
                        color = accentColor
                    )
                }
            }
        } else {
            calibratedMaps.forEach { mapItem ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(0.5.dp, if (mapItem.isVisible) accentColor.copy(alpha = 0.4f) else textColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = mapItem.isVisible,
                                    onCheckedChange = { onToggleVisible(mapItem.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                )
                                Column {
                                    Text(
                                        text = mapItem.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = String.format(Locale.US, "N:%.3f S:%.3f | E:%.3f W:%.3f", mapItem.northLat, mapItem.southLat, mapItem.eastLon, mapItem.westLon),
                                        fontSize = 9.sp,
                                        color = textColor.copy(alpha = 0.6f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { onZoomToMap(mapItem) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomInMap,
                                        contentDescription = "Zoom",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(mapItem.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (mapItem.isVisible) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isFarsiMode) "شفافیت: ${(mapItem.opacity * 100).toInt()}%" else "Opacity: ${(mapItem.opacity * 100).toInt()}%",
                                    fontSize = 10.sp,
                                    color = textColor.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(80.dp)
                                )
                                Slider(
                                    value = mapItem.opacity,
                                    onValueChange = { onOpacityChange(mapItem.id, it) },
                                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                                    modifier = Modifier.weight(1f).height(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: VECTOR LAYERS (KML / KMZ / GPX / GEOJSON)
// -------------------------------------------------------------
@Composable
private fun VectorLayersTabContent(
    importedFeatures: List<ImportedFeature>,
    onImportKmlKmz: () -> Unit,
    onImportGpx: () -> Unit,
    onImportGeoJson: () -> Unit,
    onClearFeatures: () -> Unit,
    onZoomToFeatures: () -> Unit,
    accentColor: Color,
    textColor: Color,
    cardBg: Color,
    isFarsiMode: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Vector Import Options Grid
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isFarsiMode) "ورود لایه‌های برداری مکانی" else "Import Spatial Vector Overlays",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onImportKmlKmz,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f), contentColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("KML / KMZ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (isFarsiMode) "گوگل ارث" else "Google Earth", fontSize = 8.5.sp)
                        }
                    }

                    Button(
                        onClick = onImportGpx,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f), contentColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("GPX", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (isFarsiMode) "جی‌پی‌اس گارمین" else "Garmin GPS", fontSize = 8.5.sp)
                        }
                    }

                    Button(
                        onClick = onImportGeoJson,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f), contentColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("GeoJSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (isFarsiMode) "سامانه GIS" else "QGIS / Web", fontSize = 8.5.sp)
                        }
                    }
                }
            }
        }

        // Active Vector Features Status
        if (importedFeatures.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp),
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
                        Text(
                            text = if (isFarsiMode) "لایه‌های فعال: ${importedFeatures.size} عارضه" else "Active Overlays: ${importedFeatures.size} feature(s)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = onZoomToFeatures,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomInMap,
                                    contentDescription = "Zoom",
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onClearFeatures,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Clear",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    val pointsCount = importedFeatures.count { it.type.equals("Point", true) }
                    val linesCount = importedFeatures.count { it.type.equals("LineString", true) }
                    val polysCount = importedFeatures.count { it.type.equals("Polygon", true) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📌 $pointsCount نقاط", fontSize = 10.sp, color = textColor.copy(alpha = 0.7f))
                        Text("〰️ $linesCount خطوط/گسل‌ها", fontSize = 10.sp, color = textColor.copy(alpha = 0.7f))
                        Text("🔷 $polysCount پلی‌گون/محدوده‌ها", fontSize = 10.sp, color = textColor.copy(alpha = 0.7f))
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isFarsiMode) "هیچ لایه برداری بارگذاری نشده است." else "No vector overlays loaded.",
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: SAS.PLANET ZMP IMPORTER & CUSTOM TILE SERVER (XYZ / TMS / QUADKEY)
// -------------------------------------------------------------
@Composable
private fun CustomTileServerTabContent(
    viewModel: GeologyViewModel,
    accentColor: Color,
    textColor: Color,
    cardBg: Color,
    isFarsiMode: Boolean,
    mapCenterLat: Double? = null,
    mapCenterLon: Double? = null
) {
    val context = LocalContext.current
    val sasCustomSources by viewModel.sasCustomSources.collectAsStateWithLifecycle()
    val activeSasSourceId by viewModel.activeSasSourceId.collectAsStateWithLifecycle()
    val activeSasOverlayIds by viewModel.activeSasOverlayIds.collectAsStateWithLifecycle()
    val sasOverlayOpacity by viewModel.sasOverlayOpacity.collectAsStateWithLifecycle()
    val sasDownloadProgress by viewModel.sasDownloadProgress.collectAsStateWithLifecycle()
    val sasCacheStats by viewModel.sasCacheStats.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()

    var selectedRadiusKm by remember { mutableStateOf(3.0) }
    var selectedMaxZoom by remember { mutableIntStateOf(15) }

    var urlInput by remember { mutableStateOf(viewModel.customTileUrl.value) }
    var labelInput by remember { mutableStateOf(viewModel.customTileLabel.value) }
    var enabled by remember { mutableStateOf(viewModel.useCustomTile.value) }

    var saveMapName by remember { mutableStateOf("") }
    var isSavingMbtiles by remember { mutableStateOf(false) }

    val saveMbtilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            isSavingMbtiles = true
            viewModel.exportDownloadedAreaToUri(context, it) { result ->
                isSavingMbtiles = false
                result.onSuccess { count ->
                    val msg = if (isFarsiMode) {
                        "فایل MBTiles با $count کاشی در حافظه گوشی ذخیره شد!"
                    } else {
                        "Saved .mbtiles ($count tiles) to device storage!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(context, "خطا در ذخیره فایل: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val zmpPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importSasZmp(context, it) { result ->
                result.onSuccess { src ->
                    val msg = if (isFarsiMode) {
                        "منبع نقشه SAS.Planet (${src.name}) وارد و فعال شد!"
                    } else {
                        "SAS.Planet ZMP (${src.name}) imported & activated!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        if (isFarsiMode) "خطا در خواندن ZMP: ${err.message}" else "ZMP Error: ${err.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 0. SAS.PLANET OFFLINE AREA TILE PRE-DOWNLOADER & CACHE STATS
        item {
            val activeSrc = SasZmpStorage.findSourceById(context, activeSasSourceId)
                ?: SasPlanetCatalog.builtInBaseMaps.first()
            val cacheMb = String.format("%.2f", sasCacheStats.second / (1024.0 * 1024.0))
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFarsiMode) "دانلود آفلاین محدوده (SAS.Planet Cache)" else "SAS.Planet Offline Area Downloader",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = if (isFarsiMode) "منبع فعال: ${activeSrc.nameFa}" else "Active Source: ${activeSrc.name}",
                                fontSize = 10.sp,
                                color = accentColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Surface(
                            color = accentColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${sasCacheStats.first} tiles ($cacheMb MB)",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1.0, 3.0, 5.0, 10.0).forEach { r ->
                            val sel = selectedRadiusKm == r
                            FilterChip(
                                selected = sel,
                                onClick = { selectedRadiusKm = r },
                                label = { Text("${r.toInt()} km", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFarsiMode) "حداکثر زوم:" else "Max Zoom:",
                            fontSize = 10.5.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        listOf(14, 15, 16, 17).forEach { z ->
                            val sel = selectedMaxZoom == z
                            FilterChip(
                                selected = sel,
                                onClick = { selectedMaxZoom = z },
                                label = { Text("Z$z", fontSize = 10.sp) }
                            )
                        }
                    }

                    val prog = sasDownloadProgress
                    if (prog != null) {
                        val done = prog.completedTiles + prog.skippedCached + prog.failedTiles
                        val fraction = if (prog.totalTiles > 0) done.toFloat() / prog.totalTiles else 0f
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = accentColor
                        )
                        Text(
                            text = if (isFarsiMode) {
                                "${if (prog.isRunning) "در حال دانلود" else "تکمیل شد"}: $done از ${prog.totalTiles} (جدید: ${prog.completedTiles}، کش قبلی: ${prog.skippedCached})"
                            } else {
                                "${if (prog.isRunning) "Downloading" else "Done"}: $done/${prog.totalTiles} (New: ${prog.completedTiles}, Cached: ${prog.skippedCached})"
                            },
                            fontSize = 10.sp,
                            color = textColor.copy(alpha = 0.75f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (prog?.isRunning == true) {
                            OutlinedButton(
                                onClick = { viewModel.cancelSasAreaDownload() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isFarsiMode) "توقف دانلود" else "Cancel", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val lat = mapCenterLat ?: liveLocation?.latitude ?: 35.69439
                                    val lon = mapCenterLon ?: liveLocation?.longitude ?: 51.42151
                                    viewModel.startSasAreaDownload(lat, lon, selectedRadiusKm, 11, selectedMaxZoom)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isFarsiMode) "ذخیره آفلاین محدوده" else "Cache Area Offline", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (sasCacheStats.first > 0) {
                            OutlinedButton(
                                onClick = { viewModel.clearSasTileCache() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Cache", tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // SAVE / EXPORT AS PERMANENT .MBTILES AFTER DOWNLOAD
                    if (prog != null && !prog.isRunning && (prog.completedTiles + prog.skippedCached) > 0) {
                        HorizontalDivider(color = textColor.copy(alpha = 0.08f))
                        OutlinedTextField(
                            value = saveMapName,
                            onValueChange = { saveMapName = it },
                            label = { Text(if (isFarsiMode) "نام نقشه برای ذخیره (اختیاری)" else "Map Name to Save (Optional)", fontSize = 10.sp) },
                            placeholder = { Text("Field_Area_${prog.sourceId}_Z${prog.maxZoom}", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                enabled = !isSavingMbtiles,
                                onClick = {
                                    isSavingMbtiles = true
                                    viewModel.saveDownloadedAreaToMbtiles(saveMapName.trim().ifEmpty { null }) { res ->
                                        isSavingMbtiles = false
                                        res.onSuccess { (file, count) ->
                                            val msg = if (isFarsiMode) {
                                                "نقشه ${file.name} ($count کاشی) در بخش MBTiles آفلاین ذخیره و فعال شد!"
                                            } else {
                                                "Saved ${file.name} ($count tiles) to Offline MBTiles!"
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "خطا در ذخیره: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isFarsiMode) "ذخیره در نقشه‌های آفلاین" else "Save to Offline Maps",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            OutlinedButton(
                                enabled = !isSavingMbtiles,
                                onClick = {
                                    val defaultFile = (saveMapName.trim().ifEmpty { "SAS_${prog.sourceId}_Z${prog.maxZoom}" })
                                        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_") + ".mbtiles"
                                    saveMbtilesLauncher.launch(defaultFile)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.SaveAs, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isFarsiMode) "خروجی در حافظه گوشی" else "Save File (.mbtiles)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. SAS.PLANET ZMP IMPORT CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFarsiMode) "پشتیبانی از منابع SAS.Planet (فایل ZMP / params.txt)" else "SAS.Planet ZMP Importer (sasgis/sas.planet.src)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = if (isFarsiMode) "واردسازی مستقیم بسته‌های .zmp یا params.txt (با پشتیبانی از QuadKey، EPSG:3395 و کش آفلاین)" else "Import .zmp packages or params.txt with QuadKey, EPSG:3395 & offline SQLite caching",
                                fontSize = 10.sp,
                                color = textColor.copy(alpha = 0.65f)
                            )
                        }
                    }

                    Button(
                        onClick = { zmpPickerLauncher.launch(arrayOf("*/*", "application/zip", "text/plain")) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("import_sas_zmp_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isFarsiMode) "انتخاب فایل ZMP یا params.txt از حافظه" else "Import SAS.Planet .ZMP / params.txt",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. USER-IMPORTED ZMP SOURCES LIST
        if (sasCustomSources.isNotEmpty()) {
            item {
                Text(
                    text = if (isFarsiMode) "منابع ZMP واردشده توسط کاربر (${sasCustomSources.size})" else "Imported SAS.Planet ZMP Sources (${sasCustomSources.size})",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            items(sasCustomSources, key = { it.id }) { source ->
                val isSelected = if (source.isOverlay) activeSasOverlayIds.contains(source.id) else activeSasSourceId == source.id && !enabled
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) accentColor.copy(alpha = 0.12f) else cardBg
                    ),
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 0.5.dp,
                        if (isSelected) accentColor else textColor.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (source.isOverlay) {
                                viewModel.toggleSasOverlay(source.id)
                            } else {
                                viewModel.setUseCustomTile(false)
                                enabled = false
                                viewModel.setActiveSasSourceId(source.id)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFarsiMode) source.nameFa else source.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = "${source.projection.label} • ${source.addressing.name} • ${if (source.isOverlay) "Overlay" else "Base"}",
                                fontSize = 9.5.sp,
                                color = textColor.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = { viewModel.deleteSasCustomSource(source.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete ZMP", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // 3. BUILT-IN SAS.PLANET OVERLAYS (asLayer=1)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isFarsiMode) "لایه‌های ترکیبی SAS.Planet (روی نقشه پایه)" else "SAS.Planet Transparent Overlays",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    SasPlanetCatalog.builtInOverlays.forEach { ov ->
                        val checked = activeSasOverlayIds.contains(ov.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.toggleSasOverlay(ov.id) }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isFarsiMode) ov.nameFa else ov.name,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                                Text(ov.category, fontSize = 9.sp, color = textColor.copy(alpha = 0.55f))
                            }
                            Switch(
                                checked = checked,
                                onCheckedChange = { viewModel.toggleSasOverlay(ov.id) },
                                colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.4f))
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isFarsiMode) "شفافیت لایه‌ها: ${(sasOverlayOpacity * 100).toInt()}%" else "Overlay Opacity: ${(sasOverlayOpacity * 100).toInt()}%",
                            fontSize = 10.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        Slider(
                            value = sasOverlayOpacity,
                            onValueChange = { viewModel.setSasOverlayOpacity(it) },
                            valueRange = 0.15f..1.0f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. MANUAL CUSTOM XYZ / TMS SERVER CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isFarsiMode) "آدرس سرور کاشی اختصاصی (XYZ / TMS)" else "Custom Map Tile Server (XYZ / TMS)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = if (isFarsiMode) "امکان اتصال به سرورهای محلی سازمان یا شبکه داخلی در پروژه با فرمت {z}/{x}/{y}" else "Connect to corporate GIS servers or local tile mirrors using {z}/{x}/{y} format",
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.6f)
                    )

                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text(if (isFarsiMode) "نام سرور" else "Server Label") },
                        placeholder = { Text("Local Geoserver / Offline Tile Server") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text(if (isFarsiMode) "آدرس الگو (URL Template)" else "URL Template") },
                        placeholder = { Text("http://192.168.1.10:8080/tiles/{z}/{x}/{y}.png") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFarsiMode) "فعال‌سازی این سرور روی نقشه" else "Enable on Map",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.5f))
                        )
                    }

                    Button(
                        onClick = {
                            val prefs = context.getSharedPreferences("geofieldpro_prefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putString("custom_tile_url", urlInput.trim())
                                .putString("custom_tile_label", labelInput.trim())
                                .putBoolean("use_custom_tile", enabled)
                                .apply()
                            viewModel.customTileUrl.value = urlInput.trim()
                            viewModel.customTileLabel.value = labelInput.trim()
                            viewModel.useCustomTile.value = enabled
                            Toast.makeText(context, if (isFarsiMode) "تنظیمات سرور نقشه ذخیره شد!" else "Custom tile settings saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isFarsiMode) "ذخیره تنظیمات سرور" else "Save Server Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CALIBRATION DIALOG: GEOREFERENCING IMAGE OVERLAY
// -------------------------------------------------------------
@Composable
fun CalibrateImageDialog(
    imageUri: Uri,
    currentLat: Double,
    currentLon: Double,
    onDismiss: () -> Unit,
    onSave: (name: String, north: Double, south: Double, east: Double, west: Double, opacity: Float) -> Unit,
    accentColor: Color,
    isFarsiMode: Boolean
) {
    var mapName by remember { mutableStateOf("Geological Map") }
    var calibrationMode by remember { mutableIntStateOf(0) } // 0 = UTM, 1 = Lat/Lon, 2 = GPS Radius

    // UTM inputs (default to Iran zone 39N)
    var utmZone by remember { mutableStateOf("39N") }
    var northingMaxInput by remember { mutableStateOf("3950000") } // North
    var northingMinInput by remember { mutableStateOf("3920000") } // South
    var eastingMinInput by remember { mutableStateOf("530000") }   // West
    var eastingMaxInput by remember { mutableStateOf("560000") }   // East

    // Lat/Lon inputs (calculated or manual)
    var northLatInput by remember { mutableStateOf(String.format(Locale.US, "%.5f", currentLat + 0.05)) }
    var southLatInput by remember { mutableStateOf(String.format(Locale.US, "%.5f", currentLat - 0.05)) }
    var eastLonInput by remember { mutableStateOf(String.format(Locale.US, "%.5f", currentLon + 0.05)) }
    var westLonInput by remember { mutableStateOf(String.format(Locale.US, "%.5f", currentLon - 0.05)) }

    // Radius input for GPS mode
    var radiusKmInput by remember { mutableStateOf("5.0") }
    var opacityVal by remember { mutableFloatStateOf(0.85f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isFarsiMode) "کالیبراسیون و زمین‌مرجع کردن تصویر نقشه" else "Georeference Map Image",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = mapName,
                    onValueChange = { mapName = it },
                    label = { Text(if (isFarsiMode) "عنوان نقشه" else "Map Title") },
                    placeholder = { Text("نقشه زمین‌شناسی ۱:۱۰۰،۰۰۰") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Mode switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.1f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf(
                        if (isFarsiMode) "مختصات UTM" else "UTM Grid",
                        if (isFarsiMode) "درجه اعشاری" else "Lat / Lon",
                        if (isFarsiMode) "مرکز GPS فعلی" else "Current GPS"
                    ).forEachIndexed { idx, title ->
                        val sel = calibrationMode == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (sel) accentColor else Color.Transparent)
                                .clickable { calibrationMode = idx }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 9.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) Color.White else Color.Gray
                            )
                        }
                    }
                }

                when (calibrationMode) {
                    0 -> {
                        // UTM mode
                        Text(
                            text = if (isFarsiMode) "مختصات چهارگوشه برگه نقشه را بر اساس UTM وارد کنید:" else "Enter the 4 corner UTM coordinates of your sheet:",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        OutlinedTextField(
                            value = utmZone,
                            onValueChange = { utmZone = it },
                            label = { Text("UTM Zone (مثلا 39N)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = northingMaxInput,
                                onValueChange = { northingMaxInput = it },
                                label = { Text("North (N Max)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = northingMinInput,
                                onValueChange = { northingMinInput = it },
                                label = { Text("South (N Min)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = eastingMinInput,
                                onValueChange = { eastingMinInput = it },
                                label = { Text("West (E Min)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = eastingMaxInput,
                                onValueChange = { eastingMaxInput = it },
                                label = { Text("East (E Max)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    1 -> {
                        // Lat / Lon mode
                        Text(
                            text = if (isFarsiMode) "مختصات چهارگوشه برگه نقشه (درجه اعشاری WGS84):" else "Bounding coordinates (Decimal Degrees):",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = northLatInput,
                                onValueChange = { northLatInput = it },
                                label = { Text("North Lat") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = southLatInput,
                                onValueChange = { southLatInput = it },
                                label = { Text("South Lat") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = westLonInput,
                                onValueChange = { westLonInput = it },
                                label = { Text("West Lon") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = eastLonInput,
                                onValueChange = { eastLonInput = it },
                                label = { Text("East Lon") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    2 -> {
                        // GPS Radius mode
                        Text(
                            text = if (isFarsiMode) "تصویر نقشه به مرکز موقعیت فعلی GPS با شعاع دلخواه گسترانده می‌شود:" else "Project image centered around current GPS fix with radius:",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        OutlinedTextField(
                            value = radiusKmInput,
                            onValueChange = { radiusKmInput = it },
                            label = { Text(if (isFarsiMode) "شعاع پوشش (کیلومتر)" else "Coverage Radius (km)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Opacity slider
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isFarsiMode) "شفافیت اولیه" else "Initial Opacity", fontSize = 10.sp)
                        Text("${(opacityVal * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    }
                    Slider(
                        value = opacityVal,
                        onValueChange = { opacityVal = it },
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var finalNorth = 0.0
                    var finalSouth = 0.0
                    var finalEast = 0.0
                    var finalWest = 0.0

                    when (calibrationMode) {
                        0 -> {
                            // UTM
                            val nMax = northingMaxInput.toDoubleOrNull() ?: 3950000.0
                            val nMin = northingMinInput.toDoubleOrNull() ?: 3920000.0
                            val eMin = eastingMinInput.toDoubleOrNull() ?: 530000.0
                            val eMax = eastingMaxInput.toDoubleOrNull() ?: 560000.0

                            val zoneNum = utmZone.filter { it.isDigit() }.toIntOrNull() ?: 39
                            val zoneLetter = utmZone.firstOrNull { it.isLetter() } ?: 'N'
                            val nw = UtmConverter.convertUtmToLatLng(eMin, nMax, zoneNum, zoneLetter)
                            val se = UtmConverter.convertUtmToLatLng(eMax, nMin, zoneNum, zoneLetter)
                            finalNorth = nw.latitude
                            finalSouth = se.latitude
                            finalWest = nw.longitude
                            finalEast = se.longitude
                        }
                        1 -> {
                            // Lat/Lon
                            finalNorth = northLatInput.toDoubleOrNull() ?: (currentLat + 0.05)
                            finalSouth = southLatInput.toDoubleOrNull() ?: (currentLat - 0.05)
                            finalEast = eastLonInput.toDoubleOrNull() ?: (currentLon + 0.05)
                            finalWest = westLonInput.toDoubleOrNull() ?: (currentLon - 0.05)
                        }
                        2 -> {
                            // GPS Radius
                            val rKm = radiusKmInput.toDoubleOrNull() ?: 5.0
                            val deg = (rKm / 111.0)
                            finalNorth = currentLat + deg
                            finalSouth = currentLat - deg
                            finalEast = currentLon + deg
                            finalWest = currentLon - deg
                        }
                    }

                    onSave(
                        mapName.ifBlank { "Calibrated Map" },
                        maxOf(finalNorth, finalSouth),
                        minOf(finalNorth, finalSouth),
                        maxOf(finalEast, finalWest),
                        minOf(finalEast, finalWest),
                        opacityVal
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(if (isFarsiMode) "ذخیره و نمایش روی نقشه" else "Save & Overlay", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFarsiMode) "انصراف" else "Cancel")
            }
        }
    )
}
