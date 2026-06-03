package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// --- Premium Field Palette ("Mineral Slate & Ochre") ---
val SlateDark = Color(0xFF0F1520)      // Sleek rich charcoal background
val SlateBack = Color(0xFF161F2E)      // Surface layer
val SlateCard = Color(0xFF1E293B)      // Nested card containers
val CopperOrange = Color(0xFFFF8F00)   // Primary mineral explorer gold/copper
val JadeGreen = Color(0xFF00E676)      // GPS active / favorable indicator
val EarthSand = Color(0xFFFFD54F)      // Stand-up alert/secondary highlight
val CoralRed = Color(0xFFFF5252)       // Critical / high stress alarm
val TextWhite = Color(0xFFECEFF1)      // Crisp primary labels
val TextMuted = Color(0xFF90A4AE)      // Cool grey descriptions

@Composable
fun GeoFieldTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = CopperOrange,
        secondary = JadeGreen,
        tertiary = EarthSand,
        background = SlateDark,
        surface = SlateBack,
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = TextWhite,
        onSurface = TextWhite,
        surfaceVariant = SlateCard,
        onSurfaceVariant = TextMuted
    )
    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = Typography(),
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize OpenStreetMap configuration prior to inflating MapView user-agent
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            GeoFieldTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppLayout()
                }
            }
        }
    }
}

data class NavigationTab(val id: String, val label: String, val icon: ImageVector)

// --- Main App Layout ---
@Composable
fun MainAppLayout() {
    val context = LocalContext.current
    val vm: GeologyViewModel = viewModel()
    
    val stations by vm.stations.collectAsStateWithLifecycle()
    val boreholes by vm.boreholes.collectAsStateWithLifecycle()
    val calculations by vm.calculations.collectAsStateWithLifecycle()
    
    var currentTab by remember { mutableStateOf("dashboard") }
    var locationPermissionGranted by remember { mutableStateOf(false) }

    // Navigation and GPS permissions check
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            locationPermissionGranted = true
            startGpsTracking(context, vm)
        } else {
            vm.setGpsUnavailable("Permission Denied")
        }
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            locationPermissionGranted = true
            startGpsTracking(context, vm)
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SlateBack,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    NavigationTab("dashboard", "Dashboard", Icons.Default.Home),
                    NavigationTab("stations", "Stations", Icons.Default.Place),
                    NavigationTab("boreholes", "Boreholes", Icons.Default.Menu),
                    NavigationTab("geotech", "Geotech", Icons.Default.Build),
                    NavigationTab("map", "Field Map", Icons.Default.LocationOn)
                )

                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab.id,
                        onClick = { currentTab = tab.id },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CopperOrange,
                            selectedTextColor = CopperOrange,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = SlateCard
                        ),
                        modifier = Modifier.testTag("tab_btn_${tab.id}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "dashboard" -> DashboardScreen(
                    vm = vm,
                    stations = stations,
                    boreholes = boreholes,
                    calculations = calculations,
                    onNavigateToTab = { currentTab = it }
                )
                "stations" -> StationsScreen(vm = vm, stations = stations)
                "boreholes" -> BoreholesScreen(vm = vm, boreholes = boreholes)
                "geotech" -> GeotechnicalSuiteScreen(vm = vm, calculations = calculations)
                "map" -> FieldMapScreen(vm = vm, stations = stations, boreholes = boreholes)
            }
        }
    }
}

// --- GPS Location Acquisition Hook ---
private fun startGpsTracking(context: Context, vm: GeologyViewModel) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        vm.setGpsUnavailable("Hardware unavailable")
        return
    }

    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    
    if (!hasFine && !hasCoarse) {
        vm.setGpsUnavailable("Permission missing")
        return
    }

    val locationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            vm.updateGps(loc.latitude, loc.longitude, loc.accuracy)
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    try {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                4000L,
                2f,
                locationListener
            )
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000L,
                5f,
                locationListener
            )
        }
        // Grab immediate last known fixes
        val gpsFix = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val netFix = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val bestFix = gpsFix ?: netFix
        if (bestFix != null) {
            vm.updateGps(bestFix.latitude, bestFix.longitude, bestFix.accuracy)
        }
    } catch (e: SecurityException) {
        vm.setGpsUnavailable("Security Exception")
    }
}

// ==========================================
// 1. DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(
    vm: GeologyViewModel,
    stations: List<Station>,
    boreholes: List<Borehole>,
    calculations: List<GeotechCalc>,
    onNavigateToTab: (String) -> Unit
) {
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Unit
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "🌐 GeoField Pro",
                    style = MaterialTheme.typography.headlineLarge,
                    color = CopperOrange,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Professional Exploration and Geo-Engineering Suite",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        // Live GPS Status Widget
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = SlateBack),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (vm.gpsState.isAvailable) JadeGreen.copy(alpha = 0.2f) else CoralRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (vm.gpsState.isAvailable) Icons.Default.LocationOn else Icons.Default.Warning,
                        contentDescription = "GPS Status Indicator",
                        tint = if (vm.gpsState.isAvailable) JadeGreen else CoralRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "GPS Status Console",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = vm.gpsState.stateMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = if (vm.gpsState.isAvailable) JadeGreen else TextMuted
                    )
                }
            }
        }

        // Quick Navigation Grid
        Text(
            text = "Active Operation Panels",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = TextMuted
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardShortcutCard(
                title = "Geology Stations",
                count = stations.size.toString(),
                subtitle = "Mapped localities",
                icon = Icons.Default.Place,
                onClick = { onNavigateToTab("stations") },
                modifier = Modifier.weight(1f)
            )
            DashboardShortcutCard(
                title = "Drill Boreholes",
                count = boreholes.size.toString(),
                subtitle = "Stratigraphy logs",
                icon = Icons.Default.Menu,
                onClick = { onNavigateToTab("boreholes") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardShortcutCard(
                title = "Geo-Engineering Calc",
                count = calculations.size.toString(),
                subtitle = "RMR, Q, RMI, MRMR",
                icon = Icons.Default.Build,
                onClick = { onNavigateToTab("geotech") },
                modifier = Modifier.weight(1f)
            )
            DashboardShortcutCard(
                title = "Interactive GIS Map",
                count = "OSM",
                subtitle = "Structural orientation",
                icon = Icons.Default.LocationOn,
                onClick = { onNavigateToTab("map") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Professional CSV & Zip Exporter Module
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "⚡ Structured Field Data Hub Export",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CopperOrange
                )
                Text(
                    text = "Pack the entire offline database containing geologic stations, bedding orientations, borehole drilling logs, and rock mas classifications into a fully-structured, standardized ZIP archive containing multi-sheet Excel/CSV files ready for CAD/Leapfrog.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )

                if (isExporting) {
                    CircularProgressIndicator(
                        color = CopperOrange,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    Button(
                        onClick = {
                            isExporting = true
                            val designZipFile = TableDataExporter.exportToAllCvsZip(
                                context,
                                stations,
                                DatabaseHelper(context),
                                boreholes,
                                calculations
                            )
                            isExporting = false
                            if (designZipFile != null && designZipFile.exists()) {
                                Toast.makeText(context, "Export Successful! Saved to Downloads", Toast.LENGTH_LONG).show()
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, Uri.fromFile(designZipFile))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Geological zip Export"))
                                } catch (e: Exception) {
                                    // Fallback if direct file uri sharing fails on Android 7+ content policy
                                    Toast.makeText(context, "Saved at: Downloads/${designZipFile.name}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Export run failed. Please verify storage permissions.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("export_database_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = CopperOrange, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export & Pack Database ZIP", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardShortcutCard(
    title: String,
    count: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .height(130.dp)
            .clickable { onClick() },
        colors = CardDefaults.elevatedCardColors(containerColor = SlateBack),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CopperOrange,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = count,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}

// ==========================================
// 2. GEOLOGY STATIONS SCREEN
// ==========================================
@Composable
fun StationsScreen(vm: GeologyViewModel, stations: List<Station>) {
    var showAddStationFork by remember { mutableStateOf(false) }
    val activeStationCode = vm.activeStationCode
    val activeObs by vm.activeObservations.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📐 Bedding & Stations",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = CopperOrange
                    )
                    Text(
                        text = "Mapped field stations with strike/dip logs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                IconButton(
                    onClick = { showAddStationFork = true },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = CopperOrange, contentColor = Color.Black),
                    modifier = Modifier.testTag("add_st_trigger")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add New Station")
                }
            }
        }

        if (stations.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Stations logged yet. Add your first locality station!",
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        items(stations) { st ->
            val isSelected = activeStationCode == st.stationCode
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) CopperOrange else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        if (isSelected) vm.selectStation(null) else vm.selectStation(st.stationCode)
                    },
                colors = CardDefaults.cardColors(containerColor = if (isSelected) SlateCard else SlateBack),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = st.stationCode,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CopperOrange
                            )
                            Text(
                                text = st.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextWhite
                            )
                        }
                        IconButton(
                            onClick = { vm.deleteStation(st.stationCode) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralRed, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            InfoLabelValue(label = "UTM Zone", value = st.utmZone.ifEmpty { "WGS84 GPS" })
                            InfoLabelValue(label = "Lithology Units", value = st.lithologyType.ifEmpty { "Not specified" })
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            InfoLabelValue(label = "Lat/Lon", value = String.format(Locale.US, "%.5f, %.5f", st.latitude, st.longitude))
                            InfoLabelValue(label = "Uniax Strength", value = st.rockStrength.ifEmpty { "Unestimated" })
                        }
                    }

                    if (st.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Comment: ${st.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Nested Observations Panel for the Expanded Station
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = SlateBack, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Discontinuities & Bedding Sets",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            
                            var showAddObsDialog by remember { mutableStateOf(false) }
                            
                            Button(
                                onClick = { showAddObsDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateBack, contentColor = CopperOrange),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Dip/Strike", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            if (showAddObsDialog) {
                                AddObservationDialog(
                                    stationCode = st.stationCode,
                                    onDismiss = { showAddObsDialog = false },
                                    onSave = { obs ->
                                        vm.addObservation(obs)
                                        showAddObsDialog = false
                                    }
                                )
                            }
                        }

                        if (activeObs.isEmpty()) {
                            Text(
                                text = "No bedding joint observations registered yet.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            activeObs.forEach { ob ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(SlateBack, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.0f)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Strike: ${ob.strike ?: "---"}° / Dip: ${ob.dip ?: "---"}° ${ob.dipDirection ?: ""}°",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = CopperOrange
                                            )
                                            Text(
                                                text = "[Set: ${ob.jointSet}]",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                        }
                                        Text(
                                            text = "Spacing: ${ob.jointSpacing}m | Rough: ${ob.jointRoughness} | Water: ${ob.waterClass}",
                                            fontSize = 10.sp,
                                            color = TextWhite
                                        )
                                        if (ob.notes.isNotEmpty()) {
                                            Text(
                                                text = "Note: ${ob.notes}",
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { vm.deleteObservation(ob.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete observation", tint = CoralRed, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddStationFork) {
        AddStationDialog(
            gpsState = vm.gpsState,
            onDismiss = { showAddStationFork = false },
            onSave = { st ->
                vm.addStation(st)
                showAddStationFork = false
            }
        )
    }
}

@Composable
fun InfoLabelValue(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "$label:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 11.sp, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// Dialog: Register New Locality Station
@Composable
fun AddStationDialog(gpsState: GpsState, onDismiss: () -> Unit, onSave: (Station) -> Unit) {
    var code by remember { mutableStateOf("ST-${(100..999).random()}") }
    var locality by remember { mutableStateOf("") }
    var manualLat by remember { mutableStateOf(if (gpsState.isAvailable) gpsState.latitude.toString() else "") }
    var manualLon by remember { mutableStateOf(if (gpsState.isAvailable) gpsState.longitude.toString() else "") }
    var lithology by remember { mutableStateOf("") }
    var strength by remember { mutableStateOf("V. Strong (100-250 MPa)") }
    var notes by remember { mutableStateOf("") }
    var useManualGps by remember { mutableStateOf(!gpsState.isAvailable) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateBack,
        title = { Text("Log Locality Station", color = CopperOrange, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Station Code (Unique)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_st_code")
                )
                OutlinedTextField(
                    value = locality,
                    onValueChange = { locality = it },
                    label = { Text("Locality / Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = useManualGps,
                        onCheckedChange = { useManualGps = it }
                    )
                    Text("Manual coordinates entry override (No GPS)", fontSize = 11.sp)
                }

                if (useManualGps) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualLat,
                            onValueChange = { manualLat = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = manualLon,
                            onValueChange = { manualLon = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                } else {
                    Text(
                        text = "Auto capturing GPS WGS84: ${gpsState.latitude}, ${gpsState.longitude}",
                        fontSize = 11.sp,
                        color = JadeGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = lithology,
                    onValueChange = { lithology = it },
                    label = { Text("Lithology Unit Type (eg: Granite)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Rock Compressive Strength", fontSize = 11.sp, color = TextMuted)
                val strengths = listOf("V. Strong (100-250 MPa)", "Strong (50-100 MPa)", "Medium (25-50 MPa)", "Weak (5-25 MPa)", "Soil (<1 MPa)")
                var dropdownExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { dropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard)
                    ) {
                        Text(strength, color = TextWhite)
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(SlateCard)
                    ) {
                        strengths.forEach { stG ->
                            DropdownMenuItem(
                                text = { Text(stG, color = TextWhite) },
                                onClick = {
                                    strength = stG
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Geologist remarks") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalLat = if (useManualGps) manualLat.toDoubleOrNull() ?: 0.0 else gpsState.latitude
                    val finalLon = if (useManualGps) manualLon.toDoubleOrNull() ?: 0.0 else gpsState.longitude
                    val conv = UtmConverter.convertLatLngToUtm(finalLat, finalLon)
                    val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    
                    onSave(
                        Station(
                            stationCode = code,
                            title = locality,
                            datetime = formattedDate,
                            latitude = finalLat,
                            longitude = finalLon,
                            utmZone = conv.zoneString,
                            lithologyType = lithology,
                            rockStrength = strength,
                            notes = notes
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CopperOrange)
            ) {
                Text("Confirm", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextWhite) }
        }
    )
}

// Dialog: Add bedding/joint plane observations
@Composable
fun AddObservationDialog(stationCode: String, onDismiss: () -> Unit, onSave: (Observation) -> Unit) {
    var strike by remember { mutableStateOf("") }
    var dip by remember { mutableStateOf("") }
    var dipDir by remember { mutableStateOf("") }
    var jSet by remember { mutableStateOf("Joint Set 1") }
    var spacing by remember { mutableStateOf("") }
    var roughness by remember { mutableStateOf("Slightly Rough Undulating") }
    var alteration by remember { mutableStateOf("Unaltered Joint Walls") }
    var water by remember { mutableStateOf("Completely Dry") }
    var notes by remember { mutableStateOf("") }

    var showClinometerRegister by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateBack,
        title = { Text("Log Bedding structural data", color = CopperOrange, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showClinometerRegister = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⚡ Professional Sensor Clinometer", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = strike,
                        onValueChange = { strike = it },
                        label = { Text("Strike°") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = dip,
                        onValueChange = { dip = it },
                        label = { Text("Dip°") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = dipDir,
                        onValueChange = { dipDir = it },
                        label = { Text("Direction°") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = jSet,
                    onValueChange = { jSet = it },
                    label = { Text("Discontinuity Joint Set Name (eg: J1)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = spacing,
                    onValueChange = { spacing = it },
                    label = { Text("Joint Spacing (meters)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text("Surface Roughness", fontSize = 11.sp, color = TextMuted)
                val roughnesses = listOf("Very Rough/Undulating", "Slightly Rough Undulating", "Smooth Planar", "Slickensided")
                var rExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { rExpanded = true }, colors = ButtonDefaults.buttonColors(containerColor = SlateCard)) {
                        Text(roughness, color = TextWhite)
                    }
                    DropdownMenu(expanded = rExpanded, onDismissRequest = { rExpanded = false }, modifier = Modifier.background(SlateCard)) {
                        roughnesses.forEach { r ->
                            DropdownMenuItem(text = { Text(r, color = TextWhite) }, onClick = { roughness = r; rExpanded = false })
                        }
                    }
                }

                Text("Joint Alteration/Infill", fontSize = 11.sp, color = TextMuted)
                val alterations = listOf("Unaltered Joint Walls", "Clay Infill < 1mm", "Thickness Clay > 5mm")
                var aExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { aExpanded = true }, colors = ButtonDefaults.buttonColors(containerColor = SlateCard)) {
                        Text(alteration, color = TextWhite)
                    }
                    DropdownMenu(expanded = aExpanded, onDismissRequest = { aExpanded = false }, modifier = Modifier.background(SlateCard)) {
                        alterations.forEach { al ->
                            DropdownMenuItem(text = { Text(al, color = TextWhite) }, onClick = { alteration = al; aExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Physical notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        Observation(
                            stationCode = stationCode,
                            strike = strike.toIntOrNull() ?: 0,
                            dip = dip.toIntOrNull() ?: 0,
                            dipDirection = dipDir.toIntOrNull() ?: 0,
                            jointSet = jSet,
                            jointSpacing = spacing.toDoubleOrNull() ?: 0.1,
                            jointRoughness = roughness,
                            jointAlteration = alteration,
                            waterClass = water,
                            notes = notes
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CopperOrange)
            ) {
                Text("Save", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextWhite) }
        }
    )

    if (showClinometerRegister) {
        GeologicalClinometerDialog(
            isOpen = showClinometerRegister,
            onDismissRequest = { showClinometerRegister = false },
            onCaptured = { cStrike, cDip, cDipDir ->
                strike = cStrike.toString()
                dip = cDip.toString()
                dipDir = cDipDir.toString()
                showClinometerRegister = false
            }
        )
    }
}

// ==========================================
// 3. DRILL BOREHOLES SCREEN (STRATIGRAPHY & LITHOLOGY)
// ==========================================
@Composable
fun BoreholesScreen(vm: GeologyViewModel, boreholes: List<Borehole>) {
    var showAddBorehole by remember { mutableStateOf(false) }
    val activeBhId = vm.activeBoreholeId
    val activeLogs by vm.activeBoreholeLogs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🕳️ Core Logging Depth Logs",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = CopperOrange
                    )
                    Text(
                        text = "Record borehole core drills + stratigraphy layers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                IconButton(
                    onClick = { showAddBorehole = true },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = CopperOrange, contentColor = Color.Black),
                    modifier = Modifier.testTag("add_bh_trigger")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Drill Hole")
                }
            }
        }

        if (boreholes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No exploratory drill holes saved.",
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        items(boreholes) { bh ->
            val isSelected = activeBhId == bh.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) CopperOrange else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        if (isSelected) vm.selectBorehole(null) else vm.selectBorehole(bh.id)
                    },
                colors = CardDefaults.cardColors(containerColor = if (isSelected) SlateCard else SlateBack),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = bh.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CopperOrange
                            )
                            Text(
                                text = "Area: ${bh.area} | Date: ${bh.datetime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        IconButton(onClick = { vm.deleteBorehole(bh.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "delete", tint = CoralRed, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoLabelValue(label = "Total Depth", value = "${bh.depth} meters")
                        InfoLabelValue(label = "GPS", value = String.format(Locale.US, "%.5f, %.5f", bh.latitude, bh.longitude))
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = SlateBack, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lithological Core Intervals",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )

                            var showAddLogDialog by remember { mutableStateOf(false) }

                            Button(
                                onClick = { showAddLogDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateBack, contentColor = CopperOrange),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Depth Interval", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            if (showAddLogDialog) {
                                AddBoreholeLogDialog(
                                    boreholeId = bh.id,
                                    logs = activeLogs,
                                    onDismiss = { showAddLogDialog = false },
                                    onSave = { lg ->
                                        vm.addBoreholeLog(lg)
                                        showAddLogDialog = false
                                    }
                                )
                            }
                        }

                        if (activeLogs.isEmpty()) {
                            Text(
                                text = "No intervals drilled yet. Log a core depth segment!",
                                color = TextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Visual Core Stratigraphy columns map helper
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(SlateBack, RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                // 1. Custom Canvas for dynamic scale
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(0.4f)
                                ) {
                                    val totalLoggedDepth = activeLogs.lastOrNull()?.depthTo ?: 10.0
                                    val safeTotal = if (totalLoggedDepth <= 0.0) 10.0 else totalLoggedDepth

                                    // Dynamic segments layer colors map
                                    activeLogs.forEach { log ->
                                        val startPct = (log.depthFrom / safeTotal).toFloat()
                                        val endPct = (log.depthTo / safeTotal).toFloat()
                                        val heightPx = (endPct - startPct) * size.height
                                        val startPx = startPct * size.height

                                        // Color map selectors
                                        val intervalColor = when (log.rockType.lowercase()) {
                                            "sandstone", "sand" -> Color(0xFFD4AF37)
                                            "granite", "basalt", "andesite" -> Color(0xFFC62828)
                                            "shale", "claystone", "clay" -> Color(0xFF795548)
                                            "limestone", "dolomite" -> Color(0xFF03A9F4)
                                            else -> Color(0xFF9E9E9E)
                                        }

                                        drawRect(
                                            color = intervalColor,
                                            topLeft = Offset(10f, startPx),
                                            size = androidx.compose.ui.geometry.Size(size.width - 20f, heightPx)
                                        )

                                        // Draw interval boundaries text labels inside Canvas or border offsets
                                        drawLine(
                                            color = TextMuted,
                                            start = Offset(0f, startPx),
                                            end = Offset(size.width, startPx),
                                            strokeWidth = 1f
                                        )
                                    }
                                }

                                // 2. Scrollable Legends text details for stratigraphy column
                                Column(
                                    modifier = Modifier
                                        .weight(0.6f)
                                        .fillMaxHeight()
                                        .verticalScroll(rememberScrollState())
                                        .padding(start = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Structural Leg", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    activeLogs.forEach { log ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when (log.rockType.lowercase()) {
                                                                "sandstone", "sand" -> Color(0xFFD4AF37)
                                                                "granite", "basalt" -> Color(0xFFC62828)
                                                                "shale", "clay" -> Color(0xFF795548)
                                                                "limestone" -> Color(0xFF03A9F4)
                                                                else -> Color(0xFF9E9E9E)
                                                            }
                                                        )
                                                )
                                                Text(
                                                    text = "${log.depthFrom}-${log.depthTo}m: ${log.rockType}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextWhite
                                                )
                                            }
                                            IconButton(
                                                onClick = { vm.deleteBoreholeLog(log.id, bh.id) },
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = CoralRed, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                        Text(
                                            text = "TCR: ${log.coreRecoveryPct}% | RQD: ${log.rqdPct}% | Weather: ${log.weatherClass}",
                                            fontSize = 9.sp,
                                            color = TextMuted,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                        Divider(color = SlateCard, thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddBorehole) {
        AddBoreholeDialog(
            gpsState = vm.gpsState,
            onDismiss = { showAddBorehole = false },
            onSave = { bh ->
                vm.addBorehole(bh)
                showAddBorehole = false
            }
        )
    }
}

// Dialog: Add Drill Borehole metadata
@Composable
fun AddBoreholeDialog(gpsState: GpsState, onDismiss: () -> Unit, onSave: (Borehole) -> Unit) {
    var name by remember { mutableStateOf("BH-${(10..99).random()}") }
    var area by remember { mutableStateOf("") }
    var manualLat by remember { mutableStateOf(if (gpsState.isAvailable) gpsState.latitude.toString() else "") }
    var manualLon by remember { mutableStateOf(if (gpsState.isAvailable) gpsState.longitude.toString() else "") }
    var notes by remember { mutableStateOf("") }
    var useManualGps by remember { mutableStateOf(!gpsState.isAvailable) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateBack,
        title = { Text("Setup Drill Borehole Log", color = CopperOrange, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Borehole ID/Name") },
                    modifier = Modifier.fillMaxWidth().testTag("add_bh_name")
                )
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Project Mineral/Drill Area") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = useManualGps,
                        onCheckedChange = { useManualGps = it }
                    )
                    Text("Manual coordinates entry override (No GPS)", fontSize = 11.sp)
                }

                if (useManualGps) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualLat,
                            onValueChange = { manualLat = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = manualLon,
                            onValueChange = { manualLon = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                } else {
                    Text(
                        text = "Auto capturing GPS Coordinates: ${gpsState.latitude}, ${gpsState.longitude}",
                        fontSize = 11.sp,
                        color = JadeGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Core run details") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalLat = if (useManualGps) manualLat.toDoubleOrNull() ?: 0.0 else gpsState.latitude
                    val finalLon = if (useManualGps) manualLon.toDoubleOrNull() ?: 0.0 else gpsState.longitude
                    val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    
                    onSave(
                        Borehole(
                            name = name,
                            area = area,
                            datetime = formattedDate,
                            latitude = finalLat,
                            longitude = finalLon,
                            depth = 0.0,
                            notes = notes
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CopperOrange)
            ) {
                Text("Confirm", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextWhite) }
        }
    )
}

// Dialog: Add Core run interval logs
@Composable
fun AddBoreholeLogDialog(
    boreholeId: Long,
    logs: List<BoreholeLog>,
    onDismiss: () -> Unit,
    onSave: (BoreholeLog) -> Unit
) {
    val defaultFrom = logs.lastOrNull()?.depthTo ?: 0.0
    var fromStr by remember { mutableStateOf(defaultFrom.toString()) }
    var toStr by remember { mutableStateOf("") }
    var rType by remember { mutableStateOf("") }
    var recoveryStr by remember { mutableStateOf("90") }
    var rqdStr by remember { mutableStateOf("80") }
    var weathering by remember { mutableStateOf("Fresh / Unweathered") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateBack,
        title = { Text("Log Segment Interval", color = CopperOrange, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fromStr,
                        onValueChange = { fromStr = it },
                        label = { Text("Depth From (m)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = toStr,
                        onValueChange = { toStr = it },
                        label = { Text("Depth To (m)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = rType,
                    onValueChange = { rType = it },
                    label = { Text("Rock Type (eg: Granite, Limestone)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = recoveryStr,
                        onValueChange = { recoveryStr = it },
                        label = { Text("Core Recovery %") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = rqdStr,
                        onValueChange = { rqdStr = it },
                        label = { Text("RQD %") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Text("Weathering Grade", fontSize = 11.sp, color = TextMuted)
                val grades = listOf("Fresh / Unweathered", "Slightly Weathered", "Moderately Weathered", "Highly Weathered", "Completely Weathered")
                var dropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { dropdownExpanded = true }, colors = ButtonDefaults.buttonColors(containerColor = SlateCard)) {
                        Text(weathering, color = TextWhite)
                    }
                    DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }, modifier = Modifier.background(SlateCard)) {
                        grades.forEach { g ->
                            DropdownMenuItem(text = { Text(g, color = TextWhite) }, onClick = { weathering = g; dropdownExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Joint/Mechanical features") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        BoreholeLog(
                            boreholeId = boreholeId,
                            depthFrom = fromStr.toDoubleOrNull() ?: 0.0,
                            depthTo = toStr.toDoubleOrNull() ?: 1.0,
                            rockType = rType,
                            coreRecoveryPct = recoveryStr.toDoubleOrNull() ?: 100.0,
                            rqdPct = rqdStr.toDoubleOrNull() ?: 90.0,
                            weatherClass = weathering,
                            notes = notes
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CopperOrange)
            ) {
                Text("Confirm", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextWhite) }
        }
    )
}

// ==========================================
// 4. GEOTECHNICAL CALCULATORS SUITE SCREEN
// ==========================================
@Composable
fun GeotechnicalSuiteScreen(vm: GeologyViewModel, calculations: List<GeotechCalc>) {
    var activeGeotechTab by remember { mutableStateOf("RMR") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "🏗️ Geotechnical Rock Mass Suite",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CopperOrange
            )
            Text(
                text = "State-of-the-art calculators for professional geomechanics engineering",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        ScrollableTabRow(
            selectedTabIndex = when (activeGeotechTab) {
                "RMR" -> 0
                "Q-System" -> 1
                "Hoek-Brown" -> 2
                "RMI" -> 3
                "MRMR" -> 4
                else -> 0
            },
            containerColor = SlateBack,
            contentColor = CopperOrange,
            edgePadding = 0.dp
        ) {
            val tabs = listOf("RMR", "Q-System", "Hoek-Brown", "RMI", "MRMR")
            tabs.forEach { tb ->
                Tab(
                    selected = activeGeotechTab == tb,
                    onClick = { activeGeotechTab = tb },
                    text = { Text(tb, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when (activeGeotechTab) {
                "RMR" -> RmrCalculatorPanel(onSave = { calc -> vm.saveGeotechCalc(calc) })
                "Q-System" -> QSystemCalculatorPanel(onSave = { calc -> vm.saveGeotechCalc(calc) })
                "Hoek-Brown" -> HoekBrownCalculatorPanel(onSave = { calc -> vm.saveGeotechCalc(calc) })
                "RMI" -> RmiCalculatorPanel(onSave = { calc -> vm.saveGeotechCalc(calc) })
                "MRMR" -> MrmrCalculatorPanel(onSave = { calc -> vm.saveGeotechCalc(calc) })
            }
        }
    }
}

// --- RMR 1989 CALCULATOR ---
@Composable
fun RmrCalculatorPanel(onSave: (GeotechCalc) -> Unit) {
    val context = LocalContext.current
    var strengthScore by remember { mutableIntStateOf(12) } // 100-250 MPa
    var rqdScore by remember { mutableIntStateOf(17) }     // 75-90%
    var spacingScore by remember { mutableIntStateOf(10) } // 0.2-0.6m
    var conditionScore by remember { mutableIntStateOf(20) } // Highly weathered
    var groundwaterScore by remember { mutableIntStateOf(15) } // Completely Dry
    var adjustScore by remember { mutableIntStateOf(0) }    // Very Favorable
    var notes by remember { mutableStateOf("") }

    val totalRmr = strengthScore + rqdScore + spacingScore + conditionScore + groundwaterScore + adjustScore

    val rmrClass = when {
        totalRmr >= 81 -> "Class I (Very Good Rock)"
        totalRmr >= 61 -> "Class II (Good Rock)"
        totalRmr >= 41 -> "Class III (Fair Rock)"
        totalRmr >= 21 -> "Class IV (Poor Rock)"
        else -> "Class V (Very Poor Rock)"
    }

    val standUpTime = when {
        totalRmr >= 81 -> "20 years for 15m span"
        totalRmr >= 61 -> "1 year for 10m span"
        totalRmr >= 41 -> "1 week for 5m span"
        totalRmr >= 21 -> "10 hours for 2.5m span"
        else -> "30 minutes for 1m span"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        Text("Bieniawski Rock Mass Rating (RMR 1989)", style = MaterialTheme.typography.titleMedium, color = CopperOrange, fontWeight = FontWeight.Bold)

        // RMR Score Card Results Display
        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SlateCard)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CALCULATED RMR 89", fontSize = 11.sp, color = TextMuted)
                Text(totalRmr.toString(), style = MaterialTheme.typography.displayMedium, color = CopperOrange, fontWeight = FontWeight.ExtraBold)
                Text(rmrClass, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextWhite)
                Text("Stand-Up Time: $standUpTime", style = MaterialTheme.typography.bodyMedium, color = EarthSand)
            }
        }

        // Dropdown inputs
        RmrScoreSelectorItem(label = "1. Uniaxial Strength of Intact Rock", currentSelected = strengthScore, options = listOf(15 to "> 250 MPa", 12 to "100 - 250 MPa", 7 to "50 - 100 MPa", 4 to "25 - 50 MPa", 2 to "5 - 25 MPa", 0 to "< 1 MPa"), onChecked = { strengthScore = it })
        RmrScoreSelectorItem(label = "2. Rock Quality Designation (RQD %)", currentSelected = rqdScore, options = listOf(20 to "90% - 100% (Excellent)", 17 to "75% - 90% (Good)", 13 to "50% - 75% (Fair)", 8 to "25% - 50% (Poor)", 3 to "< 25% (Very Poor)"), onChecked = { rqdScore = it })
        RmrScoreSelectorItem(label = "3. Spacing of Discontinuities", currentSelected = spacingScore, options = listOf(20 to "> 2.0 m", 15 to "0.6 to 2.0 m", 10 to "0.2 to 0.6 m", 8 to "60 to 200 mm", 5 to "< 60 mm"), onChecked = { spacingScore = it })
        RmrScoreSelectorItem(label = "4. Discontinuity Conditions", currentSelected = conditionScore, options = listOf(30 to "V. Rough, unweathered, tight (<0.1mm)", 25 to "Slightly rough, weathered, aperture <1mm", 20 to "Highly weathered joints", 10 to "Gouge infill < 5mm or aperture 1-5mm", 0 to "Soft continuous clay infill > 5mm"), onChecked = { conditionScore = it })
        RmrScoreSelectorItem(label = "5. Groundwater Conditions", currentSelected = groundwaterScore, options = listOf(15 to "Completely Dry", 10 to "Damp", 7 to "Wet", 4 to "Dripping", 0 to "Flowing/Gushing"), onChecked = { groundwaterScore = it })
        RmrScoreSelectorItem(label = "6. Discontinuity Orientation Adjustment", currentSelected = adjustScore, options = listOf(0 to "Very Favorable", -2 to "Favorable", -5 to "Fair", -10 to "Unfavorable", -12 to "Very Unfavorable"), onChecked = { adjustScore = it })

        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Add engineering context remarks") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                val inputObj = buildJsonObject {
                    put("strength_score", strengthScore)
                    put("rqd_score", rqdScore)
                    put("spacing_score", spacingScore)
                    put("condition_score", conditionScore)
                    put("groundwater_score", groundwaterScore)
                    put("adjust_score", adjustScore)
                }
                val resultsObj = buildJsonObject {
                    put("rmr_value", totalRmr)
                    put("class_string", rmrClass)
                    put("stand_up_time", standUpTime)
                }
                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                onSave(
                    GeotechCalc(
                        calcType = "RMR",
                        datetime = formattedDate,
                        inputParams = inputObj.toString(),
                        resultsJson = resultsObj.toString(),
                        notes = notes
                    )
                )
                Toast.makeText(context, "RMR calculation saved!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CopperOrange, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Geotechnical Calculation", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RmrScoreSelectorItem(label: String, currentSelected: Int, options: List<Pair<Int, String>>, onChecked: (Int) -> Unit) {
    Column {
        Text(label, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
        var expanded by remember { mutableStateOf(false) }
        val currentLabel = options.firstOrNull { it.first == currentSelected }?.second ?: "Select Option"
        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = SlateBack),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(currentLabel, color = TextWhite, fontSize = 12.sp)
                    Text("Score: $currentSelected", color = CopperOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(SlateCard)) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(opt.second, color = TextWhite, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Score: ${opt.first}", color = CopperOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = {
                            onChecked(opt.first)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// --- BARTON Q-SYSTEM CALCULATOR ---
@Composable
fun QSystemCalculatorPanel(onSave: (GeotechCalc) -> Unit) {
    val context = LocalContext.current
    var rqd by remember { mutableStateOf("80") }
    // Dropdown choices holding Barton specific decimal numbers
    var jnScore by remember { mutableStateOf(4.0) } // 2 sets
    var jrScore by remember { mutableStateOf(1.5) } // Rough/Planar
    var jaScore by remember { mutableStateOf(1.0) } // Unaltered
    var jwScore by remember { mutableStateOf(1.0) } // Dry
    var srfScore by remember { mutableStateOf(1.0) } // Stress factor stable
    var esr by remember { mutableStateOf("1.6") }   // Excavation support ratio (permanent mine)
    var notes by remember { mutableStateOf("") }

    val rqdVal = rqd.toDoubleOrNull() ?: 80.0
    val qVal = (rqdVal / jnScore) * (jrScore / jaScore) * (jwScore / srfScore)
    val esrVal = esr.toDoubleOrNull() ?: 1.6

    val unsupportedSpan = 2.0 * esrVal * qVal.pow(0.4)

    val qClass = when {
        qVal >= 400.0 -> "Exceptionally Good"
        qVal >= 100.0 -> "Extremely Good"
        qVal >= 40.0 -> "Very Good"
        qVal >= 10.0 -> "Good"
        qVal >= 4.0 -> "Fair"
        qVal >= 1.0 -> "Poor"
        qVal >= 0.1 -> "Extremely Poor"
        else -> "Exceptionally Poor"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        Text("Barton Q-System Tunnel Design", style = MaterialTheme.typography.titleMedium, color = CopperOrange, fontWeight = FontWeight.Bold)

        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SlateCard)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CALCULATED Q-VALUE", fontSize = 11.sp, color = TextMuted)
                Text(String.format(Locale.US, "%.3f", qVal), style = MaterialTheme.typography.displayMedium, color = CopperOrange, fontWeight = FontWeight.ExtraBold)
                Text("Quality: $qClass", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextWhite)
                Text(String.format(Locale.US, "Max Unsupported Span: %.2f meters", unsupportedSpan), style = MaterialTheme.typography.bodyMedium, color = JadeGreen)
            }
        }

        OutlinedTextField(
            value = rqd,
            onValueChange = { rqd = it },
            label = { Text("Core run RQD % (0 to 100 value)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        RmrScoreSelectorItem(label = "Joint Set Number (Jn)", currentSelected = (jnScore * 100).toInt(), options = listOf(50 to "Jn=0.5 (Massive)", 200 to "Jn=2.0 (1 Joint set)", 400 to "Jn=4.0 (2 Joint sets)", 900 to "Jn=9.0 (3 Joint sets)", 1500 to "Jn=15.0 (4+ Joint sets)"), onChecked = { jnScore = it.toDouble() / 100.0 })
        RmrScoreSelectorItem(label = "Joint Roughness (Jr)", currentSelected = (jrScore * 10).toInt(), options = listOf(40 to "Jr=4.0 (Non-continuous)", 30 to "Jr=3.0 (Rough undulating)", 20 to "Jr=2.0 (Smooth undulating)", 15 to "Jr=1.5 (Rough planar)", 10 to "Jr=1.0 (Smooth planar)"), onChecked = { jrScore = it.toDouble() / 10.0 })
        RmrScoreSelectorItem(label = "Joint Alteration (Ja)", currentSelected = (jaScore * 100).toInt(), options = listOf(75 to "Ja=0.75 (Healed/Tight)", 100 to "Ja=1.0 (Unaltered walls)", 200 to "Ja=2.0 (Slightly altered / clay-head)", 400 to "Ja=4.0 (Clay filling silt/sand)", 800 to "Ja=8.0 (Swelling clay layer)"), onChecked = { jaScore = it.toDouble() / 100.0 })
        RmrScoreSelectorItem(label = "Joint Water Factor (Jw)", currentSelected = (jwScore * 100).toInt(), options = listOf(100 to "Jw=1.0 (Dry / minor inflow)", 66 to "Jw=0.66 (Medium inflow)", 50 to "Jw=0.5 (Large gushing inflow)", 20 to "Jw=0.2 (Exceptionally high flow)"), onChecked = { jwScore = it.toDouble() / 100.0 })
        RmrScoreSelectorItem(label = "Stress Reduction Factor (SRF)", currentSelected = (srfScore * 100).toInt(), options = listOf(100 to "SRF=1.0 (Stable / medium stress)", 250 to "SRF=2.5 (High jointed shear zone)", 500 to "SRF=5.0 (Squeezing ground rock)", 1000 to "SRF=10.0 (Severe rockburst rock)"), onChecked = { srfScore = it.toDouble() / 100.0 })

        OutlinedTextField(
            value = esr,
            onValueChange = { esr = it },
            label = { Text("Excavation Support Ratio (ESR)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Add engineering context remarks") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                val inputObj = buildJsonObject {
                    put("rqd", rqdVal)
                    put("jn", jnScore)
                    put("jr", jrScore)
                    put("ja", jaScore)
                    put("jw", jwScore)
                    put("srf", srfScore)
                    put("esr", esrVal)
                }
                val resultsObj = buildJsonObject {
                    put("q_value", qVal)
                    put("q_class", qClass)
                    put("unsupported_span", unsupportedSpan)
                }
                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                onSave(
                    GeotechCalc(
                        calcType = "Q-System",
                        datetime = formattedDate,
                        inputParams = inputObj.toString(),
                        resultsJson = resultsObj.toString(),
                        notes = notes
                    )
                )
                Toast.makeText(context, "Q-System calculation saved!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CopperOrange, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Geotechnical Calculation", fontWeight = FontWeight.Bold)
        }
    }
}

// --- HOEK-BROWN & GSI CALCULATOR ---
@Composable
fun HoekBrownCalculatorPanel(onSave: (GeotechCalc) -> Unit) {
    val context = LocalContext.current
    var gsi by remember { mutableStateOf("55") }
    var sigmaci by remember { mutableStateOf("120") } // MPa
    var mi by remember { mutableStateOf("17") }      // e.g. Sandstone
    var disturbance by remember { mutableStateOf("0.0") } // D index
    var notes by remember { mutableStateOf("") }

    val gsiVal = gsi.toDoubleOrNull() ?: 55.0
    val sigVal = sigmaci.toDoubleOrNull() ?: 120.0
    val miVal = mi.toDoubleOrNull() ?: 17.0
    val dVal = disturbance.toDoubleOrNull() ?: 0.0

    // Calculations
    val mb = miVal * exp((gsiVal - 100.0) / (28.0 - 14.0 * dVal))
    val s = exp((gsiVal - 100.0) / (9.0 - 3.0 * dVal))
    val a = 0.5 + (1.0 / 6.0) * (exp(-gsiVal / 15.0) - exp(-20.0 / 3.0))

    val compStrength = sigVal * s.pow(a)
    val tensileStrength = -s * sigVal / mb

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        Text("GSI & Hoek-Brown Failure Criterion", style = MaterialTheme.typography.titleMedium, color = CopperOrange, fontWeight = FontWeight.Bold)

        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SlateCard)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ROCK MASS COMPRESSIVE STRENGTH", fontSize = 11.sp, color = TextMuted)
                Text(String.format(Locale.US, "%.3f MPa", compStrength), style = MaterialTheme.typography.headlineMedium, color = CopperOrange, fontWeight = FontWeight.ExtraBold)
                
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("mb parameter", fontSize = 10.sp, color = TextMuted)
                        Text(String.format(Locale.US, "%.4f", mb), fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("s parameter", fontSize = 10.sp, color = TextMuted)
                        Text(String.format(Locale.US, "%.5f", s), fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("a parameter", fontSize = 10.sp, color = TextMuted)
                        Text(String.format(Locale.US, "%.4f", a), fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(String.format(Locale.US, "Estimated Tensile Strength: %.4f MPa", tensileStrength), fontSize = 12.sp, color = EarthSand, fontWeight = FontWeight.Bold)
            }
        }

        OutlinedTextField(value = gsi, onValueChange = { gsi = it }, label = { Text("Geological Strength Index (GSI 0-100)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(value = sigmaci, onValueChange = { sigmaci = it }, label = { Text("Intact Rock Strength sigmaci (MPa)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(value = mi, onValueChange = { mi = it }, label = { Text("Intact constant mi (eg: Granite=32, Sandstone=17)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(value = disturbance, onValueChange = { disturbance = it }, label = { Text("Disturbance Factor D (0.0 to 1.0)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Add engineering context remarks") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                val inputObj = buildJsonObject {
                    put("gsi", gsiVal)
                    put("sigmaci", sigVal)
                    put("mi", miVal)
                    put("disturbance", dVal)
                }
                val resultsObj = buildJsonObject {
                    put("mb", mb)
                    put("s_param", s)
                    put("a_param", a)
                    put("comp_strength", compStrength)
                    put("tensile_strength", tensileStrength)
                }
                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                onSave(
                    GeotechCalc(
                        calcType = "Hoek-Brown",
                        datetime = formattedDate,
                        inputParams = inputObj.toString(),
                        resultsJson = resultsObj.toString(),
                        notes = notes
                    )
                )
                Toast.makeText(context, "Hoek-Brown calculation saved!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CopperOrange, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Geotechnical Calculation", fontWeight = FontWeight.Bold)
        }
    }
}

// --- RMI (ROCK MASS INDEX) CALCULATOR ---
@Composable
fun RmiCalculatorPanel(onSave: (GeotechCalc) -> Unit) {
    val context = LocalContext.current
    var intactUCS by remember { mutableStateOf("120") } // MPa
    var jvJoints by remember { mutableStateOf("5.0") }   // Volumetric Joint Count (joints/m3)
    var jrRough by remember { mutableStateOf(1.5) }       // Joint roughness factor
    var jaAlter by remember { mutableStateOf(1.0) }       // Joint alteration factor
    var jlLength by remember { mutableStateOf(1.0) }       // Joint length factor
    var notes by remember { mutableStateOf("") }

    val ucsVal = intactUCS.toDoubleOrNull() ?: 120.0
    val jvVal = jvJoints.toDoubleOrNull() ?: 5.0

    // Joint condition factor jC
    val jC = jrRough * jlLength / jaAlter
    
    // Jointing parameter Jp (empirical continuous model)
    // Jp ranges from ~0.0001 (very blocky joints) to 1.0 (massive)
    val jpFactor = exp(-0.04 * jvVal / sqrt(jC)).coerceIn(0.0001, 1.0)
    
    val rmi_val = ucsVal * jpFactor

    val rmiClass = when {
        rmi_val >= 10.0 -> "Massive / Strong Rock Mass (RMI > 10)"
        rmi_val >= 1.0 -> "Moderately Strong Rock Mass (RMI 1-10)"
        rmi_val >= 0.1 -> "Weak Rock Mass (RMI 0.1 - 1)"
        rmi_val >= 0.01 -> "Very Weak Rock Mass (RMI 0.01 - 0.1)"
        else -> "Shattered / Soft Soils (RMI < 0.01)"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        Text("RMI (Rock Mass Index) Calculator", style = MaterialTheme.typography.titleMedium, color = CopperOrange, fontWeight = FontWeight.Bold)

        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SlateCard)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ROCK MASS INDEX (RMI)", fontSize = 11.sp, color = TextMuted)
                Text(String.format(Locale.US, "%.3f MPa", rmi_val), style = MaterialTheme.typography.displayMedium, color = CopperOrange, fontWeight = FontWeight.ExtraBold)
                Text("Structural Integrity: $rmiClass", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextWhite)
                Text(String.format(Locale.US, "Jointing Parameter Jp: %.4f | jC: %.2f", jpFactor, jC), fontSize = 11.sp, color = EarthSand)
            }
        }

        OutlinedTextField(value = intactUCS, onValueChange = { intactUCS = it }, label = { Text("Intact Uniaxial Strength sigmac (MPa)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(value = jvJoints, onValueChange = { jvJoints = it }, label = { Text("Volumetric Joint Count Jv (joints/m³)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

        RmrScoreSelectorItem(label = "Joint Roughness (jR)", currentSelected = (jrRough * 10).toInt(), options = listOf(30 to "jR = 3.0 (Rough, undulating)", 20 to "jR = 2.0 (Smooth, undulating)", 15 to "jR = 1.5 (Slickensided undulating)", 10 to "jR = 1.0 (Smooth planar)", 5 to "jR = 0.5 (Slickensided planar)"), onChecked = { jrRough = it.toDouble() / 10.0 })
        RmrScoreSelectorItem(label = "Joint Alteration (jA)", currentSelected = (jaAlter * 100).toInt(), options = listOf(75 to "jA = 0.75 (Tight, unaltered)", 100 to "jA = 1.0 (Soft/low altered walls)", 200 to "jA = 2.0 (Sand/gouge filled)", 400 to "jA = 4.0 (Non-softening clay layer)", 800 to "jA = 8.0 (Swelling thick clay)"), onChecked = { jaAlter = it.toDouble() / 100.0 })
        RmrScoreSelectorItem(label = "Joint Length/Persistence (jL)", currentSelected = (jlLength * 100).toInt(), options = listOf(200 to "jL = 2.0 (Short < 1m)", 150 to "jL = 1.5 (Medium 1-3m)", 100 to "jL = 1.0 (Long 3-10m)", 50 to "jL = 0.5 (Extremely persistent >10m)"), onChecked = { jlLength = it.toDouble() / 100.0 })

        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Add engineering context remarks") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                val inputObj = buildJsonObject {
                    put("ucs", ucsVal)
                    put("jv", jvVal)
                    put("jr", jrRough)
                    put("ja", jaAlter)
                    put("jl", jlLength)
                }
                val resultsObj = buildJsonObject {
                    put("rmi", rmi_val)
                    put("jp", jpFactor)
                    put("jc", jC)
                    put("class", rmiClass)
                }
                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                onSave(
                    GeotechCalc(
                        calcType = "RMI",
                        datetime = formattedDate,
                        inputParams = inputObj.toString(),
                        resultsJson = resultsObj.toString(),
                        notes = notes
                    )
                )
                Toast.makeText(context, "RMI calculation saved!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CopperOrange, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Geotechnical Calculation", fontWeight = FontWeight.Bold)
        }
    }
}

// --- MRMR (LAUBSCHER 1990) CALCULATOR ---
@Composable
fun MrmrCalculatorPanel(onSave: (GeotechCalc) -> Unit) {
    val context = LocalContext.current
    var basicRmr by remember { mutableStateOf("60") } // Basic RMR out of 100
    var weatheringPct by remember { mutableIntStateOf(90) } // 90% (Slight)
    var jointOrientationPct by remember { mutableIntStateOf(95) } // 95% (Favorable)
    var stressPct by remember { mutableIntStateOf(85) } // 85% (Moderate block load)
    var blastingPct by remember { mutableIntStateOf(90) } // 90% (Good conventional blasting)
    var notes by remember { mutableStateOf("") }

    val baseRmrVal = basicRmr.toDoubleOrNull() ?: 60.0
    val finalMrmr = baseRmrVal * (weatheringPct.toDouble() / 100.0) * (jointOrientationPct.toDouble() / 100.0) * (stressPct.toDouble() / 100.0) * (blastingPct.toDouble() / 100.0)

    val designClass = when {
        finalMrmr >= 80 -> "Very Stable - Minimal underground reinforcement required"
        finalMrmr >= 60 -> "Stable Rock - Systematic systematic support with rockbolts"
        finalMrmr >= 40 -> "Transition Zone - Heavy support/mesh required"
        else -> "Cavable Ground / High mining risk"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        Text("Laubscher Modified RMR (MRMR 1990)", style = MaterialTheme.typography.titleMedium, color = CopperOrange, fontWeight = FontWeight.Bold)

        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SlateCard)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FINAL MRMR VALUE", fontSize = 11.sp, color = TextMuted)
                Text(String.format(Locale.US, "%.2f", finalMrmr), style = MaterialTheme.typography.displayMedium, color = CopperOrange, fontWeight = FontWeight.ExtraBold)
                Text("Mining Support: $designClass", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextWhite, textAlign = TextAlign.Center)
            }
        }

        OutlinedTextField(
            value = basicRmr,
            onValueChange = { basicRmr = it },
            label = { Text("Basic Rock Mass Rating (RMR Basic 0-100)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        RmrScoreSelectorItem(label = "Weathering Adjustment Factor", currentSelected = weatheringPct, options = listOf(100 to "100% (No weathering)", 90 to "90% (Slight weathering over life)", 75 to "75% (Moderate degradation)", 60 to "60% (Severe continuous weathering)"), onChecked = { weatheringPct = it })
        RmrScoreSelectorItem(label = "Joint Orientation Adjustment", currentSelected = jointOrientationPct, options = listOf(100 to "100% (Very favorable orientation)", 90 to "90% (Favorable direction)", 80 to "80% (Fair joint intersections)", 70 to "70% (Unfavorable direction)"), onChecked = { jointOrientationPct = it })
        RmrScoreSelectorItem(label = "Mining-Induced Stress Adjustment", currentSelected = stressPct, options = listOf(100 to "100% (Standard active stress)", 90 to "90% (Low stress concentration)", 80 to "80% (High horizontal load/abutment)", 65 to "65% (Extremely high stress cave)"), onChecked = { stressPct = it })
        RmrScoreSelectorItem(label = "Blasting Support Adjustment", currentSelected = blastingPct, options = listOf(100 to "100% (TBM bored or smooth blast)", 95 to "95% (Presplit / cushion blast)", 90 to "90% (Good production blasting)", 80 to "80% (Heavy conventional overbreak)"), onChecked = { blastingPct = it })

        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Add engineering context remarks") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                val inputObj = buildJsonObject {
                    put("basic_rmr", baseRmrVal)
                    put("weathering_pct", weatheringPct)
                    put("orientation_pct", jointOrientationPct)
                    put("stress_pct", stressPct)
                    put("blasting_pct", blastingPct)
                }
                val resultsObj = buildJsonObject {
                    put("final_mrmr", finalMrmr)
                    put("class_description", designClass)
                }
                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                onSave(
                    GeotechCalc(
                        calcType = "MRMR",
                        datetime = formattedDate,
                        inputParams = inputObj.toString(),
                        resultsJson = resultsObj.toString(),
                        notes = notes
                    )
                )
                Toast.makeText(context, "MRMR calculation saved!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CopperOrange, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Geotechnical Calculation", fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// 5. INTERACTIVE STRUCTURAL FIELD MAP SCREEN
// ==========================================
@Composable
fun FieldMapScreen(vm: GeologyViewModel, stations: List<Station>, boreholes: List<Borehole>) {
    val context = LocalContext.current
    var activeMarkerSheetStation by remember { mutableStateOf<Station?>(null) }
    var activeMarkerSheetBorehole by remember { mutableStateOf<Borehole?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Embed native OSM MapView wrapped in Compose AndroidView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    isTilesScaledToDpi = true
                    setMultiTouchControls(true)
                    
                    // Zoom controller configuration
                    controller.setZoom(12.5)
                    
                    // Initial centering on last captured GPS or default map center
                    val centerPt = if (vm.gpsState.isAvailable) {
                        GeoPoint(vm.gpsState.latitude, vm.gpsState.longitude)
                    } else {
                        GeoPoint(35.6892, 51.3890) // Default Iran map coordinates fallback
                    }
                    controller.setCenter(centerPt)
                }
            },
            update = { mapView ->
                mapView.overlays.clear()

                // Add active GPS client location marker
                if (vm.gpsState.isAvailable) {
                    val gpsMarker = Marker(mapView).apply {
                        position = GeoPoint(vm.gpsState.latitude, vm.gpsState.longitude)
                        title = "My Location Indicator"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        // Dynamic styling custom design using simple built-in markers or assets
                    }
                    mapView.overlays.add(gpsMarker)
                }

                // Append Geological stations markers
                stations.forEach { st ->
                    if (st.latitude != 0.0 && st.longitude != 0.0) {
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(st.latitude, st.longitude)
                            title = "Station: ${st.stationCode}"
                            subDescription = "Lithology: ${st.lithologyType}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            
                            setOnMarkerClickListener { m, _ ->
                                activeMarkerSheetStation = st
                                activeMarkerSheetBorehole = null
                                m.showInfoWindow()
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }
                }

                // Append Drilled Boreholes markers
                boreholes.forEach { bh ->
                    if (bh.latitude != 0.0 && bh.longitude != 0.0) {
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(bh.latitude, bh.longitude)
                            title = "Borehole: ${bh.name}"
                            subDescription = "Depth: ${bh.depth}m"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            
                            setOnMarkerClickListener { m, _ ->
                                activeMarkerSheetBorehole = bh
                                activeMarkerSheetStation = null
                                m.showInfoWindow()
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }
                }

                mapView.invalidate()
            }
        )

        // Centering floating actions
        FloatingActionButton(
            onClick = {
                // Instantly re-request location manager or context check
                Toast.makeText(context, "Locating exploratory sensors...", Toast.LENGTH_SHORT).show()
                startGpsTracking(context, vm)
            },
            containerColor = CopperOrange,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("force_gps_locate_btn")
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Center on GPS Position")
        }

        // Overlay Sheets: Detail card for selected Station marker
        AnimatedVisibility(
            visible = activeMarkerSheetStation != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
        ) {
            activeMarkerSheetStation?.let { st ->
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = SlateBack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍 Mapped Station: ${st.stationCode}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CopperOrange
                            )
                            IconButton(onClick = { activeMarkerSheetStation = null }) {
                                Icon(Icons.Default.Close, contentDescription = "close", tint = TextMuted)
                            }
                        }
                        Text("Locality Coordinates Details: ${st.title}", style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lithology: ${st.lithologyType}", fontSize = 11.sp, color = TextWhite)
                            Text("Zone: ${st.utmZone}", fontSize = 11.sp, color = TextWhite)
                        }

                        Button(
                            onClick = {
                                vm.selectStation(st.stationCode)
                                Toast.makeText(context, "Loading ${st.stationCode} in console...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open in Details Console", color = CopperOrange)
                        }
                    }
                }
            }
        }

        // Overlay Sheets: Detail card for selected Borehole marker
        AnimatedVisibility(
            visible = activeMarkerSheetBorehole != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
        ) {
            activeMarkerSheetBorehole?.let { bh ->
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = SlateBack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🕳️ Core Exploration: ${bh.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CopperOrange
                            )
                            IconButton(onClick = { activeMarkerSheetBorehole = null }) {
                                Icon(Icons.Default.Close, contentDescription = "close", tint = TextMuted)
                            }
                        }
                        Text("District / Area: ${bh.area}", style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                        Text("Logged Core Depth: ${bh.depth} meters", style = MaterialTheme.typography.bodyMedium, color = TextWhite)

                        Button(
                            onClick = {
                                vm.selectBorehole(bh.id)
                                Toast.makeText(context, "Opening core records for ${bh.name}", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Drilling stratigraphy", color = CopperOrange)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PROFESSIONAL CLINOMETER SENSOR COMPASS DIALOG
// ==========================================
@Composable
fun GeologicalClinometerDialog(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    onCaptured: (Int, Int, Int) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var strikeValue by remember { mutableIntStateOf(0) }
    var dipValue by remember { mutableIntStateOf(0) }
    var dipDirectionValue by remember { mutableIntStateOf(0) }

    // Register Android Hardware orientation sensors
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    var hasHardwareOrientation by remember { mutableStateOf(rotationVectorSensor != null) }

    DisposableEffect(isOpen) {
        if (sensorManager == null || rotationVectorSensor == null) {
            hasHardwareOrientation = false
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null) return
                    if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        val rotationMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        
                        val orientationValues = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientationValues)

                        // Angle conversions
                        val yaw = Math.toDegrees(orientationValues[0].toDouble())   // Azimuth (-180 to 180)
                        val pitch = Math.toDegrees(orientationValues[1].toDouble()) // Pitch (-90 to 90)
                        val roll = Math.toDegrees(orientationValues[2].toDouble())  // Roll (-180 to 180)

                        // 1. Strike conversion RHR: Strike fits the 0 to 360 circle
                        val positiveYaw = (yaw + 360.0) % 360.0
                        strikeValue = positiveYaw.roundToInt()

                        // 2. Dip angle: absolute pitch/roll incline slope steepness
                        val incline = abs(pitch).coerceIn(0.0, 90.0)
                        dipValue = incline.roundToInt()

                        // 3. Dip direction: standard quadrant perpendicular (Strike + 90) RHR
                        dipDirectionValue = ((positiveYaw + 90.0) % 360.0).roundToInt()
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
        onDispose {}
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = SlateBack),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Professional Sensor Clinometer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CopperOrange
                )

                if (!hasHardwareOrientation) {
                    Text(
                        text = "⚠️ Orientation Sensor absent in Emulator. Adjust simulated dials manually below represent physical device pitching.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }

                // Interactive Canvas Dial displaying Pitch/Roll inclination compass
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .border(2.dp, CopperOrange.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val angleOffset = strikeValue.toFloat()
                    val rawDip = dipValue.toFloat()

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2

                        // Outer graduation circle dial markings
                        drawCircle(
                            color = CopperOrange,
                            radius = radius - 15f,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Highlight captured Strike line cross-hairs
                        val radians = Math.toRadians((angleOffset - 90f).toDouble())
                        val endX = center.x + (radius - 20f) * cos(radians).toFloat()
                        val endY = center.y + (radius - 20f) * sin(radians).toFloat()
                        drawLine(
                            color = JadeGreen,
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 3.dp.toPx()
                        )

                        // Dip vector bubble indicator centering displacement
                        val dipOffsetPct = (rawDip / 90f) * (radius - 30f)
                        val dipRadians = Math.toRadians((dipDirectionValue - 90f).toDouble())
                        val bubbleX = center.x + dipOffsetPct * cos(dipRadians).toFloat()
                        val bubbleY = center.y + dipOffsetPct * sin(dipRadians).toFloat()

                        drawCircle(
                            color = CopperOrange,
                            radius = 12f,
                            center = Offset(bubbleX, bubbleY)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${strikeValue}°",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite
                        )
                        Text(
                            text = "D/DD: ${dipValue}° / ${dipDirectionValue}°",
                            style = MaterialTheme.typography.bodySmall,
                            color = EarthSand,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Simulated sliders fallback in emulator environment
                if (!hasHardwareOrientation) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Simulate Azimuth (Strike): $strikeValue°", fontSize = 11.sp, color = TextWhite)
                        Slider(
                            value = strikeValue.toFloat(),
                            onValueChange = {
                                strikeValue = it.roundToInt()
                                dipDirectionValue = ((it + 90.0) % 360.0).roundToInt()
                            },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(thumbColor = CopperOrange, activeTrackColor = CopperOrange)
                        )

                        Text("Simulate Inclination (Dip): $dipValue°", fontSize = 11.sp, color = TextWhite)
                        Slider(
                            value = dipValue.toFloat(),
                            onValueChange = { dipValue = it.roundToInt() },
                            valueRange = 0f..90f,
                            colors = SliderDefaults.colors(thumbColor = CopperOrange, activeTrackColor = CopperOrange)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextWhite)
                    }
                    Button(
                        onClick = {
                            onCaptured(strikeValue, dipValue, dipDirectionValue)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CopperOrange, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CAP 🎯", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
