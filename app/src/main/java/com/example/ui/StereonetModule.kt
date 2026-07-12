package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.*
import java.text.SimpleDateFormat
import java.util.*

// -------------------------------------------------------------------------
// 2D NATIVE STEREONET DATA STRUCTURES & THEMES
// -------------------------------------------------------------------------

enum class StereonetTheme(val label: String, val labelFarsi: String) {
    OBSIDIAN("Classic Obsidian", "تاریک شیک"),
    SLATE("Slate Field", "صحرایی مات"),
    PAPER("Pristine Paper", "کاغذ مهندسی"),
    TERMINAL("Retro Terminal", "ترمینال قدیمی")
}

enum class StereonetFeatureType(val label: String, val labelFarsi: String) {
    JOINT("Joint", "درزه / ناپیوستگی"),
    PLANE("Bedding", "صفحه / لایه‌بندی"),
    FAULT("Fault", "گسل / صفحه لغزش"),
    LINE("Lineation", "خط‌واره / خط نقطه")
}

data class StereonetJoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dip: Int,
    val dipDirection: Int,
    val color: Color,
    val notes: String = "",
    val isFromProject: Boolean = false,
    val type: StereonetFeatureType = StereonetFeatureType.JOINT
)

data class WedgeIntersection(
    val jointA: StereonetJoint,
    val jointB: StereonetJoint,
    val plunge: Double,
    val trend: Double,
    val isHazard: Boolean
)

@Composable
fun StereonetScreen(
    viewModel: GeologyViewModel,
    modifier: Modifier = Modifier
) {
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()
    val globalActiveProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val projectList by viewModel.projects.collectAsStateWithLifecycle()
    val allObservations by viewModel.allObservations.collectAsStateWithLifecycle()
    val stations by viewModel.activeProjectStations.collectAsStateWithLifecycle()

    var localSelectedProjectId by remember { mutableStateOf<Int?>(null) }
    var lastSyncedGlobalProjectId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(globalActiveProject) {
        if (globalActiveProject != null && globalActiveProject?.id != lastSyncedGlobalProjectId) {
            localSelectedProjectId = globalActiveProject?.id
            lastSyncedGlobalProjectId = globalActiveProject?.id
        }
    }

    val activeProject = projectList.find { it.id == localSelectedProjectId }

    if (activeProject == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val path = Path()
                        val w = size.width
                        val h = size.height
                        path.moveTo(0f, h * 0.7f)
                        path.quadraticTo(w * 0.3f, h * 0.5f, w * 0.6f, h * 0.8f)
                        path.quadraticTo(w * 0.8f, h * 0.95f, w, h * 0.6f)
                        path.lineTo(w, h)
                        path.lineTo(0f, h)
                        path.close()
                        drawPath(path = path, color = Color(0xFF4CAF50).copy(alpha = 0.05f))
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )

                    Text(
                        text = if (isFarsiMode) "تحلیل استریونت دوبعدی زمین‌شناسی" else "2D Geotechnical Stereonet Analyser",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isFarsiMode)
                            "برای شروع تحلیل ساختاری، ترسیم دایره اصطکاک و بررسی خطر لغزش صفحه‌ای و واژگونی، لطفا یک پروژه زمین‌شناسی را انتخاب کنید."
                        else
                            "To begin structural projection analysis, plot friction circles, and analyze planar/toppling hazards, please select an active project.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            }

            Text(
                text = if (isFarsiMode) "پروژه‌های در دسترس:" else "Available Projects:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.Start)
            )

            if (projectList.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFarsiMode) "هنوز پروژه‌ای تعریف نشده است. لطفا از بخش خانه‌ یا برداشت پروژه بسازید." else "No projects defined yet. Please create one on the Home tab.",
                        color = Color.Gray,
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(projectList, key = { it.id }) { project ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    localSelectedProjectId = project.id
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(project.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(project.description ?: (if (isFarsiMode) "پروژه عمومی" else "General Project"), fontSize = 11.sp, color = Color.Gray)
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        StereonetWorkspace(
            viewModel = viewModel,
            projectId = activeProject.id,
            projectName = activeProject.name,
            onBack = { localSelectedProjectId = null },
            modifier = modifier
        )
    }
}

@Composable
fun StereonetWorkspace(
    viewModel: GeologyViewModel,
    projectId: Int,
    projectName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFarsiMode by viewModel.isFarsiModeSetting.collectAsStateWithLifecycle()
    val allObservations by viewModel.allObservations.collectAsStateWithLifecycle()
    val stations by viewModel.activeProjectStations.collectAsStateWithLifecycle()

    // Filter project observations with valid structural measurements (dip and dipDirection)
    val projectStationIds = remember(stations) { stations.map { it.id }.toSet() }
    val databaseJoints = remember(allObservations, projectStationIds) {
        allObservations.filter { it.stationId in projectStationIds && it.dip != null && it.dipDirection != null }
            .mapIndexed { idx, obs ->
                val type = when (obs.observationType.lowercase()) {
                    "fault" -> StereonetFeatureType.FAULT
                    "lineation", "line" -> StereonetFeatureType.LINE
                    "bedding", "foliation" -> StereonetFeatureType.PLANE
                    else -> StereonetFeatureType.JOINT
                }
                StereonetJoint(
                    id = "db_${obs.id}",
                    name = obs.observationType + " #${obs.id} (${obs.notes?.take(15) ?: "Obs"})",
                    dip = obs.dip ?: 0,
                    dipDirection = obs.dipDirection ?: 0,
                    color = when (type) {
                        StereonetFeatureType.JOINT -> Color(0xFF4CAF50)
                        StereonetFeatureType.FAULT -> Color(0xFFF44336)
                        StereonetFeatureType.PLANE -> Color(0xFF2196F3)
                        StereonetFeatureType.LINE -> Color(0xFFE040FB)
                    },
                    notes = obs.notes ?: "",
                    isFromProject = true,
                    type = type
                )
            }
    }

    // Interactive slope parameters state
    var slopeDip by remember { mutableStateOf(65f) }
    var slopeDipDir by remember { mutableStateOf(135f) }
    var frictionAngle by remember { mutableStateOf(30f) }
    var lateralLimit by remember { mutableStateOf(20f) }

    // Display configuration state
    var selectedTheme by remember { mutableStateOf(StereonetTheme.OBSIDIAN) }
    var isEqualArea by remember { mutableStateOf(true) } // Schmidt (true) vs Wulff (false)
    var isUpperHemisphere by remember { mutableStateOf(false) } // Default lower hemisphere

    // Interactive custom joints list
    var customJoints by remember {
        mutableStateOf(
            listOf(
                StereonetJoint(
                    name = if (isFarsiMode) "درزه‌کوه شرقی - دسته ۱" else "East Wall Joint - Set A",
                    dip = 45,
                    dipDirection = 120,
                    color = Color(0xFF00E676),
                    type = StereonetFeatureType.JOINT
                ),
                StereonetJoint(
                    name = if (isFarsiMode) "سطح لغزش گسل اصلی" else "Main Fault Slip Plane",
                    dip = 70,
                    dipDirection = 145,
                    color = Color(0xFFFF1744),
                    type = StereonetFeatureType.FAULT
                ),
                StereonetJoint(
                    name = if (isFarsiMode) "لایه‌بندی آهک دیواره" else "Limestone Bedding Plane",
                    dip = 30,
                    dipDirection = 220,
                    color = Color(0xFF2196F3),
                    type = StereonetFeatureType.PLANE
                ),
                StereonetJoint(
                    name = if (isFarsiMode) "خش‌لغزش گسل (خط‌واره)" else "Fault Slickensides (Line)",
                    dip = 25, // Plunge
                    dipDirection = 135, // Trend
                    color = Color(0xFFE040FB),
                    type = StereonetFeatureType.LINE
                )
            )
        )
    }

    // Toggle showing database joints vs custom joints
    var showDatabaseJoints by remember { mutableStateOf(true) }
    val activeJointsList = remember(customJoints, databaseJoints, showDatabaseJoints) {
        if (showDatabaseJoints) customJoints + databaseJoints else customJoints
    }

    // Highlighting & selection
    var selectedJointId by remember { mutableStateOf<String?>(null) }
    val highlightedJoint = activeJointsList.find { it.id == selectedJointId }

    // Calculate wedge intersections for active joints (planes only, ignoring lineations)
    val wedgeIntersections = remember(activeJointsList, slopeDip, slopeDipDir, frictionAngle) {
        val list = mutableListOf<WedgeIntersection>()
        val planesOnly = activeJointsList.filter { it.type != StereonetFeatureType.LINE }
        for (i in 0 until planesOnly.size) {
            for (j in i + 1 until planesOnly.size) {
                val ja = planesOnly[i]
                val jb = planesOnly[j]
                val inter = calculatePlaneIntersection(
                    ja.dip.toDouble(), ja.dipDirection.toDouble(),
                    jb.dip.toDouble(), jb.dipDirection.toDouble()
                )
                if (inter != null) {
                    val (plunge, trend) = inter
                    val isHazard = isWedgeSlidingHazard(
                        plunge, trend,
                        slopeDip.toDouble(), slopeDipDir.toDouble(),
                        frictionAngle.toDouble()
                    )
                    list.add(WedgeIntersection(ja, jb, plunge, trend, isHazard))
                }
            }
        }
        list
    }

    // Inline dialog for adding custom joints
    var showAddJointDialog by remember { mutableStateOf(false) }
    var showEditJointDialog by remember { mutableStateOf(false) }
    var editingJointId by remember { mutableStateOf<String?>(null) }
    var savingJointToProject by remember { mutableStateOf<StereonetJoint?>(null) }

    var editJointName by remember { mutableStateOf("") }
    var editJointType by remember { mutableStateOf(StereonetFeatureType.JOINT) }
    var editJointInputFormat by remember { mutableStateOf(0) }
    var editJointDip by remember { mutableStateOf("45") }
    var editJointDipDir by remember { mutableStateOf("120") }
    var editJointStrike by remember { mutableStateOf("30") }
    var editJointDipQuadrant by remember { mutableStateOf("SE") }
    var editJointPlunge by remember { mutableStateOf("25") }
    var editJointTrend by remember { mutableStateOf("135") }
    var editJointColor by remember { mutableStateOf(Color(0xFF00E676)) }

    var newJointName by remember { mutableStateOf("") }
    var newJointType by remember { mutableStateOf(StereonetFeatureType.JOINT) }
    var newJointInputFormat by remember { mutableStateOf(0) } // 0: Dip/DipDirection, 1: Strike/Dip
    var newJointDip by remember { mutableStateOf("45") }
    var newJointDipDir by remember { mutableStateOf("120") }
    var newJointStrike by remember { mutableStateOf("30") }
    var newJointDipQuadrant by remember { mutableStateOf("SE") } // SE, NW, etc.
    var newJointPlunge by remember { mutableStateOf("25") } // for LINE
    var newJointTrend by remember { mutableStateOf("135") } // for LINE
    var newJointColor by remember { mutableStateOf(Color(0xFF00E676)) }

    // Tab Workspace configuration state
    var selectedTab by remember { mutableStateOf(0) }

    // Layout configuration states for zooming and custom styles
    var pointSize by remember { mutableStateOf(5f) }
    var showLabels by remember { mutableStateOf(false) }
    var visibleLayers by remember {
        mutableStateOf(
            mapOf(
                StereonetFeatureType.JOINT to true,
                StereonetFeatureType.PLANE to true,
                StereonetFeatureType.FAULT to true,
                StereonetFeatureType.LINE to true
            )
        )
    }
    var rotX by remember { mutableStateOf(0f) }
    var rotY by remember { mutableStateOf(0f) }
    var rotZ by remember { mutableStateOf(0f) }
    var zoomScale by remember { mutableStateOf(1.0f) }
    var targetDip by remember { mutableStateOf<Int?>(null) }
    var targetDipDir by remember { mutableStateOf<Int?>(null) }

    // Real-time geological converter states in the sidebar
    var converterInputStandard by remember { mutableStateOf(0) } // 0: DipDirection, 1: Strike
    var convDip by remember { mutableStateOf("45") }
    var convDipDirOrStrike by remember { mutableStateOf("120") }
    var convRhrQuadrant by remember { mutableStateOf("SE") }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Workspace header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(projectName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (isFarsiMode) "تحلیل استریونت سینماتیک پایداری شیب" else "Bilingual Kinematic Stereonet Workspace",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High-End PDF Report Exporter Button
                Button(
                    onClick = {
                        exportStereonetToPdf(
                            context = context,
                            projectName = projectName,
                            slopeDip = slopeDip.toDouble(),
                            slopeDipDir = slopeDipDir.toDouble(),
                            frictionAngle = frictionAngle.toDouble(),
                            lateralLimit = lateralLimit.toDouble(),
                            joints = activeJointsList,
                            wedgeIntersections = wedgeIntersections,
                            isEqualArea = isEqualArea,
                            isUpperHemisphere = isUpperHemisphere,
                            isFarsi = isFarsiMode
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "PDF Report",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFarsiMode) "خروجی PDF" else "PDF Report",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (isFarsiMode) "تغییر پروژه" else "Change Project", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stereonet Canvas Card
            Card(
                modifier = Modifier
                    .weight(1.2f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterVertically),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when (selectedTheme) {
                                StereonetTheme.OBSIDIAN -> Color(0xFF0C100D)
                                StereonetTheme.SLATE -> Color(0xFF263238)
                                StereonetTheme.PAPER -> Color(0xFFFFFFFF)
                                StereonetTheme.TERMINAL -> Color(0xFF001100)
                            }
                        )
                ) {
                    StereonetCanvas(
                        slopeDip = slopeDip.toDouble(),
                        slopeDipDir = slopeDipDir.toDouble(),
                        frictionAngle = frictionAngle.toDouble(),
                        lateralLimit = lateralLimit.toDouble(),
                        joints = activeJointsList,
                        highlightedJoint = highlightedJoint,
                        wedgeIntersections = wedgeIntersections,
                        selectedJointId = selectedJointId,
                        isEqualArea = isEqualArea,
                        isUpperHemisphere = isUpperHemisphere,
                        theme = selectedTheme,
                        pointSize = pointSize,
                        showLabels = showLabels,
                        visibleLayers = visibleLayers,
                        rotX = rotX.toDouble(),
                        rotY = rotY.toDouble(),
                        rotZ = rotZ.toDouble(),
                        zoomScale = zoomScale,
                        targetDip = targetDip,
                        targetDipDir = targetDipDir,
                        onTap = { dip, dipDir ->
                            targetDip = dip
                            targetDipDir = dipDir
                            newJointDip = dip.toString()
                            newJointDipDir = dipDir.toString()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )

                    // Overlay labels
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isFarsiMode) "شیب پله: ${slopeDip.toInt()}° / ${slopeDipDir.toInt()}°" else "Slope Face: ${slopeDip.toInt()}° / ${slopeDipDir.toInt()}°",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isFarsiMode) "دایره اصطکاک: ${frictionAngle.toInt()}°" else "Friction Circle: ${frictionAngle.toInt()}°",
                            color = Color(0xFFFFB74D),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isFarsiMode) "💡 لمس: نشانه‌گذاری نقطه تفصیلی" else "💡 Tap: Target structural point",
                            color = Color(0xFF00E676),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Interactive target coordinate HUD control pill
                    if (targetDip != null && targetDipDir != null) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .fillMaxWidth(0.92f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp).copy(alpha = 0.96f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MyLocation,
                                            contentDescription = null,
                                            tint = Color(0xFFFF1744),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (isFarsiMode) "هدف: شیب ${targetDip}° / جهت شیب ${targetDipDir}°" else "Target: Dip ${targetDip}° / Dip Dir ${targetDipDir}°",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            targetDip = null
                                            targetDipDir = null
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // 1. Quick Plot
                                    Button(
                                        onClick = {
                                            val tDip = targetDip ?: return@Button
                                            val tDir = targetDipDir ?: return@Button
                                            val newJoint = StereonetJoint(
                                                name = if (isFarsiMode) "درزه ترسیمی ${customJoints.size + 1}" else "Quick Plot #${customJoints.size + 1}",
                                                dip = tDip,
                                                dipDirection = tDir,
                                                color = Color(0xFF00E676),
                                                type = StereonetFeatureType.JOINT
                                            )
                                            customJoints = customJoints + newJoint
                                            selectedJointId = newJoint.id
                                            targetDip = null
                                            targetDipDir = null
                                            Toast.makeText(context, if (isFarsiMode) "با موفقیت ثبت شد" else "Successfully plotted!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                        modifier = Modifier.weight(1f).height(26.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(if (isFarsiMode) "ترسیم سریع" else "Quick Plot", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                    
                                    // 2. Add with details
                                    Button(
                                        onClick = {
                                            showAddJointDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                        modifier = Modifier.weight(1f).height(26.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(if (isFarsiMode) "ثبت با جزییات" else "Plot Feature", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Theme selector floating bubble
                    IconButton(
                        onClick = {
                            val nextTheme = when (selectedTheme) {
                                StereonetTheme.OBSIDIAN -> StereonetTheme.SLATE
                                StereonetTheme.SLATE -> StereonetTheme.PAPER
                                StereonetTheme.PAPER -> StereonetTheme.TERMINAL
                                StereonetTheme.TERMINAL -> StereonetTheme.OBSIDIAN
                            }
                            selectedTheme = nextTheme
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = "Switch Theme", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Quick Info & Projection toggles panel
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isFarsiMode) "تنظیمات تصویر" else "Projection Engine",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Schmidt vs Wulff switches
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isFarsiMode) "شبکه مساوی (اشمیت)" else "Equal Area (Schmidt)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = isEqualArea,
                                onCheckedChange = { isEqualArea = it },
                                modifier = Modifier.scale(0.6f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isFarsiMode) "نیمکره شمالی" else "Upper Hemisphere", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = isUpperHemisphere,
                                onCheckedChange = { isUpperHemisphere = it },
                                modifier = Modifier.scale(0.6f)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        // Toggle project database joints integration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isFarsiMode) "نمایش کل برداشت‌های پروژه" else "Plot GIS Project Joints", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = showDatabaseJoints,
                                onCheckedChange = { showDatabaseJoints = it },
                                modifier = Modifier.scale(0.6f)
                            )
                        }

                        if (showDatabaseJoints) {
                            Text(
                                text = if (isFarsiMode) "${databaseJoints.size} درزه از پایگاه‌داده بارگذاری شد" else "Loaded ${databaseJoints.size} database joints",
                                fontSize = 8.5.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Help hint card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Gesture, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isFarsiMode) "می‌توانید شیب شیروانی را مستقیماً با کشیدن انگشت روی استریونت تغییر دهید!" else "Tip: Drag on stereonet to interactive shift slope face angle!",
                            fontSize = 8.sp,
                            lineHeight = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Kinematic Safety Verdict Card
        KinematicStabilityReport(
            slopeDip = slopeDip.toDouble(),
            slopeDipDir = slopeDipDir.toDouble(),
            frictionAngle = frictionAngle.toDouble(),
            lateralLimit = lateralLimit.toDouble(),
            joints = activeJointsList,
            wedgeIntersections = wedgeIntersections,
            isFarsiMode = isFarsiMode
        )

        // Tab selection row above scrollable content
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 }
            ) {
                Text(
                    text = if (isFarsiMode) "داده‌ها و لایه‌ها" else "Data & Layers",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 }
            ) {
                Text(
                    text = if (isFarsiMode) "شیروانی" else "Slope",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 }
            ) {
                Text(
                    text = if (isFarsiMode) "آمار" else "Stats",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 }
            ) {
                Text(
                    text = if (isFarsiMode) "چین و تنش" else "Fold & Stress",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 }
            ) {
                Text(
                    text = if (isFarsiMode) "دوران سه‌بعدی" else "3D Rotate",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }

        // Main Tab Content Panel
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: DATA AND LAYERS
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isFarsiMode) "مدیریت لایه‌های نمایشی استریونت" else "Stereonet Layer Visibility Control",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StereonetFeatureType.values().forEach { fType ->
                                    val isVisible = visibleLayers[fType] == true
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                visibleLayers = visibleLayers.toMutableMap().apply {
                                                    put(fType, !isVisible)
                                                }
                                            }
                                    ) {
                                        Checkbox(
                                            checked = isVisible,
                                            onCheckedChange = {
                                                visibleLayers = visibleLayers.toMutableMap().apply {
                                                    put(fType, it)
                                                }
                                            },
                                            modifier = Modifier.scale(0.8f)
                                        )
                                        Text(
                                            text = if (isFarsiMode) fType.labelFarsi.take(12) else fType.label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Geological Standards Auto-Converter Panel
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isFarsiMode) "تبدیل خودکار استانداردهای زمین‌شناسی" else "Geological Standards Auto-Converter",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { converterInputStandard = 0 },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (converterInputStandard == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isFarsiMode) "جهت شیب / شیب" else "Dip Direction / Dip", fontSize = 8.sp)
                                }
                                Button(
                                    onClick = { converterInputStandard = 1 },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (converterInputStandard == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isFarsiMode) "قاعده دست راست (RHR)" else "Right Hand Rule (RHR)", fontSize = 8.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = convDip,
                                    onValueChange = { convDip = it },
                                    label = { Text(if (isFarsiMode) "شیب" else "Dip", fontSize = 8.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = convDipDirOrStrike,
                                    onValueChange = { convDipDirOrStrike = it },
                                    label = {
                                        Text(
                                            if (converterInputStandard == 0) {
                                                if (isFarsiMode) "جهت شیب" else "Dip Dir"
                                            } else {
                                                if (isFarsiMode) "امتداد (Strike)" else "Strike"
                                            },
                                            fontSize = 8.sp
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1.2f)
                                )
                                if (converterInputStandard == 1) {
                                    OutlinedTextField(
                                        value = convRhrQuadrant,
                                        onValueChange = { convRhrQuadrant = it },
                                        label = { Text(if (isFarsiMode) "ربع" else "Quad", fontSize = 8.sp) },
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }
                            }

                            // Output conversions in real-time
                            val dVal = convDip.toDoubleOrNull() ?: 0.0
                            val ddOrStVal = convDipDirOrStrike.toDoubleOrNull() ?: 0.0

                            val displayClar = if (converterInputStandard == 0) {
                                "${dVal.toInt()}/${ddOrStVal.toInt()}"
                            } else {
                                val resolvedDD = resolveStrikeToDipDirection(ddOrStVal, convRhrQuadrant)
                                "${dVal.toInt()}/${resolvedDD.toInt()}"
                            }

                            val displayRhr = if (converterInputStandard == 0) {
                                val strike = (ddOrStVal - 90.0 + 360.0) % 360.0
                                "Strike: ${strike.toInt()}° / Dip: ${dVal.toInt()}° (RHR)"
                            } else {
                                "Strike: ${ddOrStVal.toInt()}° / Dip: ${dVal.toInt()}° (RHR)"
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isFarsiMode) "نتایج تبدیل به سایر استانداردها:" else "Conversions to other formats:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Clar Notation (Dip/DipDir): $displayClar",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Right Hand Rule: $displayRhr",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )

                                Button(
                                    onClick = {
                                        val parsedDip = dVal.toInt().coerceIn(0, 90)
                                        val parsedDipDir = if (converterInputStandard == 0) {
                                            ddOrStVal.toInt().coerceIn(0, 360)
                                        } else {
                                            resolveStrikeToDipDirection(ddOrStVal, convRhrQuadrant).toInt().coerceIn(0, 360)
                                        }

                                        customJoints = customJoints + StereonetJoint(
                                            name = if (isFarsiMode) "صفحه تبدیل شده" else "Converted Plane",
                                            dip = parsedDip,
                                            dipDirection = parsedDipDir,
                                            color = Color(0xFF2979FF),
                                            type = StereonetFeatureType.PLANE,
                                            notes = "Converted via standards tool"
                                        )
                                        Toast.makeText(context, if (isFarsiMode) "به استریونت اضافه شد" else "Added converted plane", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(28.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(if (isFarsiMode) "ترسیم مستقیم این صفحه" else "Plot Converted Plane", fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    // Logged structural features list
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
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
                                Text(
                                    text = if (isFarsiMode) "لیست درزه‌ها و صفحات ساختاری" else "Logged Structural Planes (Discontinuities)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Button(
                                    onClick = { showAddJointDialog = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isFarsiMode) "افزودن درزه" else "Add Plane", fontSize = 10.sp)
                                }
                            }

                            if (activeJointsList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isFarsiMode) "هیچ درزه‌ای اضافه نشده است." else "No discontinuities to plot.",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            } else {
                                activeJointsList.forEach { joint ->
                                    val isSelected = selectedJointId == joint.id
                                    val isPlanarSafe = !isJointPlanarSlidingHazard(joint.dip.toDouble(), joint.dipDirection.toDouble(), slopeDip.toDouble(), slopeDipDir.toDouble(), frictionAngle.toDouble(), lateralLimit.toDouble())
                                    val isToppleSafe = !isJointTopplingHazard(joint.dip.toDouble(), joint.dipDirection.toDouble(), slopeDip.toDouble(), slopeDipDir.toDouble(), frictionAngle.toDouble(), lateralLimit.toDouble())

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedJointId = if (isSelected) null else joint.id
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(joint.color)
                                            )

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(joint.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(joint.color.copy(alpha = 0.12f))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isFarsiMode) joint.type.labelFarsi else joint.type.label,
                                                            fontSize = 7.5.sp,
                                                            color = joint.color,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    if (joint.isFromProject) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text("GIS Database", fontSize = 7.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = if (joint.type == StereonetFeatureType.LINE) {
                                                        if (isFarsiMode) "پلنج (انحراف): ${joint.dip}° / روند: ${joint.dipDirection}°" else "Plunge: ${joint.dip}° / Trend: ${joint.dipDirection}°"
                                                    } else {
                                                        if (isFarsiMode) "شیب: ${joint.dip}° / جهت شیب: ${joint.dipDirection}°" else "Dip: ${joint.dip}° / Dip Direction: ${joint.dipDirection}°"
                                                    },
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (joint.notes.isNotBlank()) {
                                                    Text(joint.notes, fontSize = 8.5.sp, color = Color.Gray)
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                if (joint.type != StereonetFeatureType.LINE) {
                                                    if (!isPlanarSafe) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0xFFFFCDD2))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(if (isFarsiMode) "خطر لغزش" else "Planar Hazard", color = Color(0xFFC62828), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    if (!isToppleSafe) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0xFFFFE0B2))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(if (isFarsiMode) "خطر واژگونی" else "Toppling Hazard", color = Color(0xFFE65100), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    if (isPlanarSafe && isToppleSafe) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0xFFC8E6C9))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(if (isFarsiMode) "پایدار" else "Stable", color = Color(0xFF2E7D32), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(if (isFarsiMode) "نقطه خط" else "Line Point", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                // Edit & Delete button for custom joints
                                                if (!joint.isFromProject) {
                                                    IconButton(
                                                        onClick = {
                                                            savingJointToProject = joint
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CloudUpload,
                                                            contentDescription = "Save to Station",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            editingJointId = joint.id
                                                            editJointName = joint.name
                                                            editJointType = joint.type
                                                            editJointColor = joint.color
                                                            editJointInputFormat = 0
                                                            if (joint.type == StereonetFeatureType.LINE) {
                                                                editJointPlunge = joint.dip.toString()
                                                                editJointTrend = joint.dipDirection.toString()
                                                            } else {
                                                                editJointDip = joint.dip.toString()
                                                                editJointDipDir = joint.dipDirection.toString()
                                                            }
                                                            showEditJointDialog = true
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit",
                                                            tint = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            customJoints = customJoints.filter { it.id != joint.id }
                                                            Toast.makeText(context, "Joint removed", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete",
                                                            tint = MaterialTheme.colorScheme.error,
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
                
                1 -> {
                    // TAB 1: SLOPE FACE PARAMETERS
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isFarsiMode) "پارامترهای هندسی شیروانی سنگی" else "Rock Slope Face Parameters",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Slope Dip Slider
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isFarsiMode) "زاویه شیب شیروانی" else "Slope Face Dip Angle", fontSize = 11.sp)
                                    Text("${slopeDip.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Slider(
                                    value = slopeDip,
                                    onValueChange = { slopeDip = it },
                                    valueRange = 10f..90f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }

                            // Slope Dip Direction Slider
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isFarsiMode) "جهت شیب شیروانی (Dip Direction)" else "Slope Face Dip Direction (Azimuth)", fontSize = 11.sp)
                                    Text("${slopeDipDir.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Slider(
                                    value = slopeDipDir,
                                    onValueChange = { slopeDipDir = it },
                                    valueRange = 0f..360f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }

                            // Basal Friction Angle Slider
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isFarsiMode) "زاویه اصطکاک مبنای سنگ (φ)" else "Basal Rock Friction Angle (φ)", fontSize = 11.sp)
                                    Text("${frictionAngle.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Slider(
                                    value = frictionAngle,
                                    onValueChange = { frictionAngle = it },
                                    valueRange = 10f..60f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }

                            // Lateral Limit Slider
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isFarsiMode) "محدوده جانبی انحراف صفحات لغزش" else "Planar Sliding Lateral Boundary Limit", fontSize = 11.sp)
                                    Text("±${lateralLimit.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Slider(
                                    value = lateralLimit,
                                    onValueChange = { lateralLimit = it },
                                    valueRange = 5f..45f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }
                }
                
                2 -> {
                    // TAB 2: STATISTICAL ANALYSIS (Mean Vector, Fisher, Eigenvalues)
                    val meanVec = calculateMeanVector(activeJointsList)
                    val eigenResult = calculateOrientationTensor(activeJointsList)

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isFarsiMode) "نتایج محاسبات آماری فیشر و بردار میانگین" else "Fisher Statistics & Mean Vector Analysis",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (meanVec != null) {
                                val meanPlunge = meanVec.first
                                val meanTrend = meanVec.second
                                val meanDip = 90.0 - meanPlunge
                                val meanDipDir = (meanTrend + 180.0) % 360.0

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(if (isFarsiMode) "میانگین قطب درزه‌ها (Poles Mean Vector):" else "Mean Poles Vector:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("• Plunge: ${String.format("%.1f", meanPlunge)}° / Trend: ${String.format("%.1f", meanTrend)}°", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                    
                                    Text(if (isFarsiMode) "صفحه میانگین نماینده (Representative Mean Plane):" else "Mean Plane Orientation:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("• Clar (Dip/DipDir): ${String.format("%.1f", meanDip)}°/${String.format("%.1f", meanDipDir)}°", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    
                                    val rhrStrike = (meanDipDir - 90.0 + 360.0) % 360.0
                                    Text("• RHR Strike: ${String.format("%.1f", rhrStrike)}° / Dip: ${String.format("%.1f", meanDip)}°", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }

                                // Fisher statistics Calculations
                                val N = activeJointsList.filter { it.type != StereonetFeatureType.LINE }.size
                                if (N > 1) {
                                    // Calculate Fisher k
                                    var sumX = 0.0
                                    var sumY = 0.0
                                    var sumZ = 0.0
                                    activeJointsList.filter { it.type != StereonetFeatureType.LINE }.forEach { j ->
                                        val dipRad = Math.toRadians(j.dip.toDouble())
                                        val dipDirRad = Math.toRadians(j.dipDirection.toDouble())
                                        sumX += -Math.sin(dipRad) * Math.sin(dipDirRad)
                                        sumY += -Math.sin(dipRad) * Math.cos(dipDirRad)
                                        sumZ += -Math.cos(dipRad)
                                    }
                                    val R = Math.sqrt(sumX*sumX + sumY*sumY + sumZ*sumZ)
                                    val k = (N - 1.0) / (N - R).coerceAtLeast(1e-4)

                                    var alpha95 = 0.0
                                    val temp = 1.0 - ((N - R) / R) * (Math.pow(20.0, 1.0 / (N - 1)) - 1.0)
                                    if (temp in 0.0..1.0) {
                                        alpha95 = Math.toDegrees(Math.acos(temp))
                                    } else {
                                        alpha95 = 139.5 / Math.sqrt((k * N).coerceAtLeast(1e-4))
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(if (isFarsiMode) "پارامترهای آماری توزیع فیشر (Fisher Density):" else "Fisher Dispersion Parameters:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                        Text("• Population size (N): $N", fontSize = 10.sp)
                                        Text("• Resultant Vector Length (R): ${String.format("%.4f", R)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        Text("• Precision Parameter (k): ${String.format("%.1f", k)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        Text("• 95% Confidence Cone (α95): ${String.format("%.2f", alpha95)}°", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Text(
                                    text = if (isFarsiMode) "جهت محاسبه بردار میانگین حداقل یک صفحه ترسیم کنید." else "Please plot at least 1 plane to compute mean vector.",
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = Color.Gray
                                )
                            }

                            // Eigenvalues / Orientation Tensor report
                            if (eigenResult != null) {
                                val s1 = eigenResult.eigenvalues[0]
                                val s2 = eigenResult.eigenvalues[1]
                                val s3 = eigenResult.eigenvalues[2]

                                val shapeDesc = if (s1 > 0.75 && s2 < 0.15) {
                                    if (isFarsiMode) "خوشه‌ای متراکم (ترجیحی موازی)" else "Tight cluster (Highly uniform joints)"
                                } else if (s1 > 0.45 && s2 > 0.35 && s3 < 0.12) {
                                    if (isFarsiMode) "کمربند صفحه‌ای (چین‌خورده یا چرخیده)" else "Bedding girdle (Girdle distribution/Folded)"
                                } else {
                                    if (isFarsiMode) "تصادفی / پراکنده" else "Randomly scattered / Isotropic"
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(if (isFarsiMode) "آنالیز تانسور جهت‌یافتگی (Orientation Tensor):" else "Eigenvalue Shape Analysis:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                                    Text("• S1 (حداکثر تراکم): ${String.format("%.4f", s1)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("• S2 (تراکم میانی): ${String.format("%.4f", s2)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("• S3 (حداقل تراکم): ${String.format("%.4f", s3)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                    
                                    Text(
                                        text = if (isFarsiMode) "تفسیر ساختار زمین‌شناسی: $shapeDesc" else "Interpretation: $shapeDesc",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
                
                3 -> {
                    // TAB 3: STRUCTURAL ANALYSIS (Fold Axis, Cylindrical fold, Paleostress inversion)
                    val eigenResult = calculateOrientationTensor(activeJointsList)

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isFarsiMode) "محاسبه محور چین‌خوردگی (Fold Axis β-axis)" else "Fold Axis & Cylindrical Fold Analysis",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (eigenResult != null) {
                                // Fold axis is normal to girdle (eigenvector corresponding to S3)
                                val s3 = eigenResult.eigenvalues[2]
                                val s2 = eigenResult.eigenvalues[1]
                                val s1 = eigenResult.eigenvalues[0]
                                val e3 = eigenResult.eigenvectors[2]

                                var tx = e3[0]
                                var ty = e3[1]
                                var tz = e3[2]
                                if (tz > 0) {
                                    tx = -tx
                                    ty = -ty
                                    tz = -tz
                                }
                                val foldPlunge = Math.toDegrees(Math.asin(Math.abs(tz)))
                                var foldTrend = Math.toDegrees(Math.atan2(tx, ty))
                                foldTrend = (foldTrend + 360.0) % 360.0

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(if (isFarsiMode) "مشخصات هندسی محور چین (Fold β-Axis):" else "Computed Fold Axis (β-Axis):", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("• Plunge: ${String.format("%.1f", foldPlunge)}°", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    Text("• Trend (Azimuth): ${String.format("%.1f", foldTrend)}°", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                    // Cylindrical fold criteria: S1 ~= S2 >> S3
                                    val cylPercent = ((s2 - s3) / s1 * 100.0).coerceIn(0.0, 100.0)
                                    Text(
                                        text = if (isFarsiMode) "درجه استوانه‌ای بودن چین (Cylindrical Degree): ${String.format("%.1f", cylPercent)}%" 
                                               else "Folding Cylindricity Index: ${String.format("%.1f", cylPercent)}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (cylPercent > 60) Color(0xFF2E7D32) else Color(0xFFE65100)
                                    )
                                }
                            } else {
                                Text(
                                    text = if (isFarsiMode) "جهت تحلیل تاشدگی چین و محاسبه بتا، حداقل دو لایه‌بندی متفاوت ترسیم کنید." else "Please plot at least 2 distinct Beddings to compute Fold β-axis.",
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = Color.Gray
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // PALEOSTRESS ANALYSIS (STRESS INVERSION)
                            Text(
                                text = if (isFarsiMode) "وارون‌سازی تنش دیرینه ساختاری (Paleostress Inversion)" else "Paleostress Analysis (Stress Inversion)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            val faultsList = activeJointsList.filter { it.type == StereonetFeatureType.FAULT }
                            if (faultsList.isNotEmpty()) {
                                // Calculate paleostress based on fault slip kinematics (Andersonian Classification)
                                var sumNormalDip = 0.0
                                faultsList.forEach { sumNormalDip += it.dip }
                                val avgDip = sumNormalDip / faultsList.size

                                val regime = if (avgDip < 45) {
                                    if (isFarsiMode) "رژیم زمین‌ساختی فشاری (شاهد راندگی و معکوس)" else "Andersonian Thrust/Reverse Regime (Horizontal Compression)"
                                } else if (avgDip > 65) {
                                    if (isFarsiMode) "رژیم زمین‌ساختی کششی (شاهد گسل‌های نرمال)" else "Andersonian Normal/Extensional Regime (Vertical σ1)"
                                } else {
                                    if (isFarsiMode) "رژیم زمین‌ساختی امتدادلغز (برشی جانبی)" else "Andersonian Strike-Slip Regime (Horizontal Shear)"
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(if (isFarsiMode) "رژیم تنش زمین‌ساختی حاکم (Tectonic Regime):" else "Tectonic Stress State:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(regime, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                    Text(if (isFarsiMode) "راستای تنش‌های اصلی تخمینی:" else "Estimated Principal Stress Axes:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    if (avgDip > 65) {
                                        Text("• σ1 (عمودی): Plunge ~80° / Trend ~0°", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        Text("• σ3 (کشش افقی): Plunge ~10° / Trend ~90°", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    } else if (avgDip < 45) {
                                        Text("• σ1 (فشار افقی): Plunge ~10° / Trend ~180°", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        Text("• σ3 (عمودی): Plunge ~80° / Trend ~0°", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    } else {
                                        Text("• σ1 (بیشینه افقی): Plunge ~15° / Trend ~45°", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        Text("• σ2 (متوسط عمودی): Plunge ~75° / Trend ~225°", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            } else {
                                Text(
                                    text = if (isFarsiMode) "هیچ گسلی (Fault) جهت برآورد وارون‌سازی تنش ثبت نشده است." else "Please add at least 1 Fault feature to calculate paleostress inversion.",
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                
                4 -> {
                    // TAB 4: 3D COORDINATE ROTATION & STYLES
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
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
                                Text(
                                    text = if (isFarsiMode) "دوران سه‌بعدی داده‌های استریونت حول محورها" else "3D Rotation Transforms (X, Y, Z Matrix)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = {
                                        rotX = 0f
                                        rotY = 0f
                                        rotZ = 0f
                                    }
                                ) {
                                    Text(if (isFarsiMode) "بازنشانی دوران" else "Reset Rotation", fontSize = 10.sp)
                                }
                            }

                            // Rotation X
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isFarsiMode) "دوران حول محور افقی X" else "Rotate around X-Axis", fontSize = 11.sp)
                                    Text("${rotX.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Slider(
                                    value = rotX,
                                    onValueChange = { rotX = it },
                                    valueRange = 0f..360f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }

                            // Rotation Y
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isFarsiMode) "دوران حول محور قائم Y" else "Rotate around Y-Axis", fontSize = 11.sp)
                                    Text("${rotY.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Slider(
                                    value = rotY,
                                    onValueChange = { rotY = it },
                                    valueRange = 0f..360f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }

                            // Rotation Z
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isFarsiMode) "دوران حول محور قائم Z" else "Rotate around Z-Axis (Spin)", fontSize = 11.sp)
                                    Text("${rotZ.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Slider(
                                    value = rotZ,
                                    onValueChange = { rotZ = it },
                                    valueRange = 0f..360f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = if (isFarsiMode) "اندازه نمادها و برچسب‌های متنی استریونت" else "Symbol Sizes & Label Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            // Point Size Slider
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isFarsiMode) "اندازه نقطه‌ها / نمادها" else "Symbol Point Size", fontSize = 11.sp)
                                    Text("${pointSize.toInt()} dp", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Slider(
                                    value = pointSize,
                                    onValueChange = { pointSize = it },
                                    valueRange = 3f..15f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }

                            // Zoom Scale Slider
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isFarsiMode) "ضریب بزرگنمایی استریونت" else "Zoom Scale", fontSize = 11.sp)
                                    Text("${String.format("%.1f", zoomScale)}x", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Slider(
                                    value = zoomScale,
                                    onValueChange = { zoomScale = it },
                                    valueRange = 1.0f..3.0f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }

                            // Show Point Labels Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(if (isFarsiMode) "نمایش برچسب متنی نام عوارض روی استریونت" else "Show Feature Name Labels on Net", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Switch(
                                    checked = showLabels,
                                    onCheckedChange = { showLabels = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddJointDialog) {
        AlertDialog(
            onDismissRequest = { showAddJointDialog = false },
            title = {
                Text(
                    text = if (isFarsiMode) "ثبت عارضه ساختاری جدید" else "Add New Structural Feature",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newJointName,
                        onValueChange = { newJointName = it },
                        label = { Text(if (isFarsiMode) "عنوان عارضه (اختیاری)" else "Feature Name (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Type Selector Segmented-like row
                    Text(
                        text = if (isFarsiMode) "نوع عارضه زمین‌شناسی:" else "Geological Feature Type:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StereonetFeatureType.values().forEach { type ->
                            val isSelected = newJointType == type
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { newJointType = type },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isFarsiMode) type.labelFarsi.substringBefore(" ") else type.label,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    if (newJointType == StereonetFeatureType.LINE) {
                        // Lineation: Trend & Plunge
                        Text(
                            text = if (isFarsiMode) "مختصات بردار (خط نقطه):" else "Vector Coordinates (Lineation):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newJointPlunge,
                                onValueChange = { newJointPlunge = it },
                                label = { Text(if (isFarsiMode) "پلنج/زاویه (0-90)" else "Plunge°") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = newJointTrend,
                                onValueChange = { newJointTrend = it },
                                label = { Text(if (isFarsiMode) "روند/آزیموت (0-360)" else "Trend°") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    } else {
                        // Planar Features: Choose between Dip/DipDir and Strike/Dip
                        Text(
                            text = if (isFarsiMode) "فرمت ورود اطلاعات صفحه:" else "Plane Data Entering Format:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                (if (isFarsiMode) "شیب و جهت شیب" else "Dip & Dip Dir") to 0,
                                (if (isFarsiMode) "امتداد و شیب" else "Strike & Dip") to 1
                            ).forEach { (lbl, idx) ->
                                val isSelected = newJointInputFormat == idx
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { newJointInputFormat = idx },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(lbl, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (newJointInputFormat == 0) {
                            // Dip / Dip Dir
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newJointDip,
                                    onValueChange = { newJointDip = it },
                                    label = { Text(if (isFarsiMode) "زاویه شیب (0-90)" else "Dip°") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                OutlinedTextField(
                                    value = newJointDipDir,
                                    onValueChange = { newJointDipDir = it },
                                    label = { Text(if (isFarsiMode) "جهت شیب (0-360)" else "Dip Dir°") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        } else {
                            // Strike / Dip
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newJointStrike,
                                    onValueChange = { newJointStrike = it },
                                    label = { Text(if (isFarsiMode) "امتداد (0-360)" else "Strike°") },
                                    modifier = Modifier.weight(1.1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                OutlinedTextField(
                                    value = newJointDip,
                                    onValueChange = { newJointDip = it },
                                    label = { Text(if (isFarsiMode) "شیب (0-90)" else "Dip°") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                var expandedQuadrant by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = newJointDipQuadrant,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(if (isFarsiMode) "جهت" else "Side") },
                                        shape = RoundedCornerShape(8.dp),
                                        trailingIcon = {
                                            IconButton(onClick = { expandedQuadrant = true }) {
                                                Icon(Icons.Default.ArrowDropDown, null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    DropdownMenu(
                                        expanded = expandedQuadrant,
                                        onDismissRequest = { expandedQuadrant = false }
                                    ) {
                                        listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW").forEach { q ->
                                            DropdownMenuItem(
                                                text = { Text(q) },
                                                onClick = {
                                                    newJointDipQuadrant = q
                                                    expandedQuadrant = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Choose colors row
                    Text(
                        text = if (isFarsiMode) "انتخاب رنگ بندی اختصاصی نماد روی استریونت:" else "Choose Custom Stereonet Color Symbol:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Color(0xFF00E676), // Green
                            Color(0xFFFF1744), // Red
                            Color(0xFF2979FF), // Blue
                            Color(0xFFFFD600), // Yellow
                            Color(0xFFE040FB), // Magenta
                            Color(0xFF00E5FF)  // Cyan
                        ).forEach { col ->
                            val isSelectedColor = newJointColor == col
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(col)
                                    .border(
                                        2.dp,
                                        if (isSelectedColor) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { newJointColor = col }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedDip: Int
                        val parsedDipDir: Int

                        if (newJointType == StereonetFeatureType.LINE) {
                            val plungeVal = newJointPlunge.toIntOrNull() ?: 25
                            val trendVal = newJointTrend.toIntOrNull() ?: 135
                            parsedDip = plungeVal.coerceIn(0, 90)
                            parsedDipDir = trendVal.coerceIn(0, 360)
                        } else {
                            if (newJointInputFormat == 0) {
                                parsedDip = (newJointDip.toIntOrNull() ?: 45).coerceIn(0, 90)
                                parsedDipDir = (newJointDipDir.toIntOrNull() ?: 120).coerceIn(0, 360)
                            } else {
                                val strikeVal = (newJointStrike.toIntOrNull() ?: 30).toDouble().coerceIn(0.0, 360.0)
                                val dipVal = (newJointDip.toIntOrNull() ?: 45).toDouble().coerceIn(0.0, 90.0)
                                val resolvedDipDir = resolveStrikeToDipDirection(strikeVal, newJointDipQuadrant)
                                parsedDip = dipVal.toInt()
                                parsedDipDir = resolvedDipDir.toInt()
                            }
                        }

                        val nameStr = if (newJointName.isBlank()) {
                            when (newJointType) {
                                StereonetFeatureType.JOINT -> if (isFarsiMode) "درزه جدید" else "New Joint"
                                StereonetFeatureType.PLANE -> if (isFarsiMode) "لایه‌بندی جدید" else "New Bedding"
                                StereonetFeatureType.FAULT -> if (isFarsiMode) "گسل جدید" else "New Fault"
                                StereonetFeatureType.LINE -> if (isFarsiMode) "خط‌واره جدید" else "New Lineation"
                            }
                        } else newJointName

                        val notesStr = if (newJointType != StereonetFeatureType.LINE && newJointInputFormat == 1) {
                            if (isFarsiMode) "ثبت با امتداد: ${newJointStrike}° / شیب: ${newJointDip}° ${newJointDipQuadrant}"
                            else "Entered via Strike/Dip: ${newJointStrike}° / ${newJointDip}° ${newJointDipQuadrant}"
                        } else ""

                        customJoints = customJoints + StereonetJoint(
                            name = nameStr,
                            dip = parsedDip,
                            dipDirection = parsedDipDir,
                            color = newJointColor,
                            type = newJointType,
                            notes = notesStr
                        )
                        showAddJointDialog = false
                        newJointName = ""
                        newJointDip = "45"
                        newJointDipDir = "120"
                        newJointStrike = "30"
                        newJointPlunge = "25"
                        newJointTrend = "135"
                        Toast.makeText(context, if (isFarsiMode) "با موفقیت ترسیم شد" else "Added successfully", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isFarsiMode) "تأیید" else "Plot Feature")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddJointDialog = false }) {
                    Text(if (isFarsiMode) "لغو" else "Cancel")
                }
            }
        )
    }

    if (showEditJointDialog && editingJointId != null) {
        AlertDialog(
            onDismissRequest = { showEditJointDialog = false },
            title = {
                Text(
                    text = if (isFarsiMode) "ویرایش عارضه ساختاری" else "Edit Structural Feature",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editJointName,
                        onValueChange = { editJointName = it },
                        label = { Text(if (isFarsiMode) "نام عارضه (مثلاً درزه J1)" else "Feature Name (e.g., J1 Joint)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Feature Type Segmented Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StereonetFeatureType.values().forEach { fType ->
                            Button(
                                onClick = { editJointType = fType },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (editJointType == fType) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isFarsiMode) fType.labelFarsi.take(8) else fType.label, fontSize = 8.sp, maxLines = 1)
                            }
                        }
                    }

                    if (editJointType != StereonetFeatureType.LINE) {
                        // Choice of input format: Dip/DipDirection or Strike/Dip
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { editJointInputFormat = 0 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (editJointInputFormat == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isFarsiMode) "جهت شیب / شیب" else "Dip Dir / Dip", fontSize = 10.sp)
                            }
                            Button(
                                onClick = { editJointInputFormat = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (editJointInputFormat == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isFarsiMode) "قاعده دست راست" else "Strike/Dip (RHR)", fontSize = 10.sp)
                            }
                        }

                        if (editJointInputFormat == 0) {
                            OutlinedTextField(
                                value = editJointDip,
                                onValueChange = { editJointDip = it },
                                label = { Text(if (isFarsiMode) "زاویه شیب (Dip) (بین ۰ تا ۹۰)" else "Dip Angle (0-90)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editJointDipDir,
                                onValueChange = { editJointDipDir = it },
                                label = { Text(if (isFarsiMode) "جهت شیب (Dip Direction) (۰ تا ۳۶۰)" else "Dip Direction (0-360)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = editJointStrike,
                                onValueChange = { editJointStrike = it },
                                label = { Text(if (isFarsiMode) "امتداد صفحه (Strike) (۰ تا ۳۶۰)" else "Strike (0-360)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editJointDip,
                                onValueChange = { editJointDip = it },
                                label = { Text(if (isFarsiMode) "زاویه شیب (Dip) (بین ۰ تا ۹۰)" else "Dip Angle (0-90)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editJointDipQuadrant,
                                onValueChange = { editJointDipQuadrant = it },
                                label = { Text(if (isFarsiMode) "جهت شیب صفحه (مثلاً SE یا NW)" else "Dip Quadrant (e.g., SE, NW)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        // Line feature inputs
                        OutlinedTextField(
                            value = editJointPlunge,
                            onValueChange = { editJointPlunge = it },
                            label = { Text(if (isFarsiMode) "انحراف خط (Plunge) (۰ تا ۹۰)" else "Line Plunge (0-90)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editJointTrend,
                            onValueChange = { editJointTrend = it },
                            label = { Text(if (isFarsiMode) "روند خط (Trend) (۰ تا ۳۶۰)" else "Line Trend (0-360)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Choose colors row
                    Text(
                        text = if (isFarsiMode) "انتخاب رنگ بندی اختصاصی نماد روی استریونت:" else "Choose Custom Stereonet Color Symbol:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Color(0xFF00E676), // Green
                            Color(0xFFFF1744), // Red
                            Color(0xFF2979FF), // Blue
                            Color(0xFFFFD600), // Yellow
                            Color(0xFFE040FB), // Magenta
                            Color(0xFF00E5FF)  // Cyan
                        ).forEach { col ->
                            val isSelectedColor = editJointColor == col
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(col)
                                    .border(
                                        2.dp,
                                        if (isSelectedColor) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { editJointColor = col }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedDip: Int
                        val parsedDipDir: Int

                        if (editJointType == StereonetFeatureType.LINE) {
                            val plungeVal = editJointPlunge.toIntOrNull() ?: 25
                            val trendVal = editJointTrend.toIntOrNull() ?: 135
                            parsedDip = plungeVal.coerceIn(0, 90)
                            parsedDipDir = trendVal.coerceIn(0, 360)
                        } else {
                            if (editJointInputFormat == 0) {
                                parsedDip = (editJointDip.toIntOrNull() ?: 45).coerceIn(0, 90)
                                parsedDipDir = (editJointDipDir.toIntOrNull() ?: 120).coerceIn(0, 360)
                            } else {
                                val strikeVal = (editJointStrike.toIntOrNull() ?: 30).toDouble().coerceIn(0.0, 360.0)
                                val dipVal = (editJointDip.toIntOrNull() ?: 45).toDouble().coerceIn(0.0, 90.0)
                                val resolvedDipDir = resolveStrikeToDipDirection(strikeVal, editJointDipQuadrant)
                                parsedDip = dipVal.toInt()
                                parsedDipDir = resolvedDipDir.toInt()
                            }
                        }

                        val notesStr = if (editJointType != StereonetFeatureType.LINE && editJointInputFormat == 1) {
                            if (isFarsiMode) "ثبت با امتداد: ${editJointStrike}° / شیب: ${editJointDip}° ${editJointDipQuadrant}"
                            else "Entered via Strike/Dip: ${editJointStrike}° / ${editJointDip}° ${editJointDipQuadrant}"
                        } else ""

                        customJoints = customJoints.map {
                            if (it.id == editingJointId) {
                                it.copy(
                                    name = editJointName.ifBlank { it.name },
                                    dip = parsedDip,
                                    dipDirection = parsedDipDir,
                                    color = editJointColor,
                                    type = editJointType,
                                    notes = notesStr
                                )
                            } else it
                        }
                        showEditJointDialog = false
                        Toast.makeText(context, if (isFarsiMode) "با موفقیت ویرایش شد" else "Updated successfully", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isFarsiMode) "به‌روزرسانی" else "Update Feature")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditJointDialog = false }) {
                    Text(if (isFarsiMode) "لغو" else "Cancel")
                }
            }
        )
    }

    var selectedStationForSave by remember { mutableStateOf<Int?>(null) }
    if (savingJointToProject != null) {
        val jointToSave = savingJointToProject!!
        AlertDialog(
            onDismissRequest = { savingJointToProject = null },
            title = {
                Text(
                    text = if (isFarsiMode) "ذخیره در ایستگاه‌های پروژه" else "Save to Project Station",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isFarsiMode) 
                            "عارضه: ${jointToSave.name}\nنوع: ${jointToSave.type.labelFarsi}\nشیب: ${jointToSave.dip}° / جهت شیب: ${jointToSave.dipDirection}°" 
                            else "Feature: ${jointToSave.name}\nType: ${jointToSave.type.label}\nDip: ${jointToSave.dip}° / Dip Dir: ${jointToSave.dipDirection}°",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (stations.isEmpty()) {
                        Text(
                            text = if (isFarsiMode) 
                                "⚠️ هیچ ایستگاهی در این پروژه پیدا نشد! لطفاً ابتدا یک ایستگاه در بخش برداشت‌ها بسازید."
                                else "⚠️ No stations found. Please create a geological mapping station first.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = if (isFarsiMode) "ایستگاه مقصد را انتخاب کنید:" else "Select target station:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 180.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(stations) { station ->
                                val isSelected = selectedStationForSave == station.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                        .clickable { selectedStationForSave = station.id }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedStationForSave = station.id }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(station.stationCode, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Prefix: ${station.prefix}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (stations.isNotEmpty()) {
                    Button(
                        enabled = selectedStationForSave != null,
                        onClick = {
                            val stId = selectedStationForSave!!
                            val station = stations.firstOrNull { it.id == stId }
                            val obsType = when (jointToSave.type) {
                                StereonetFeatureType.FAULT -> "Fault"
                                StereonetFeatureType.PLANE -> "Structure"
                                StereonetFeatureType.LINE -> "Structure"
                                else -> "Joint"
                            }
                            
                            viewModel.saveObservation(
                                stationId = stId,
                                obsType = obsType,
                                notes = if (isFarsiMode) "اضافه شده از بخش تحلیل استریونت - ${jointToSave.notes}" else "Added from Stereonet - ${jointToSave.notes}",
                                photoPath = null,
                                dip = jointToSave.dip,
                                dipDirection = jointToSave.dipDirection,
                                strike = ((jointToSave.dipDirection - 90 + 360) % 360),
                                onSaved = {}
                            )
                            
                            Toast.makeText(
                                context,
                                if (isFarsiMode) "عارضه با موفقیت به ایستگاه ${station?.stationCode} اضافه شد!" 
                                else "Successfully added feature to station ${station?.stationCode}!",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            savingJointToProject = null
                        }
                    ) {
                        Text(if (isFarsiMode) "ذخیره" else "Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { savingJointToProject = null }) {
                    Text(if (isFarsiMode) "انصراف" else "Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------------------
// COMPOSABLE INTERACTIVE NATIVE 2D CANVAS
// -------------------------------------------------------------------------

@Composable
fun StereonetCanvas(
    slopeDip: Double,
    slopeDipDir: Double,
    frictionAngle: Double,
    lateralLimit: Double,
    joints: List<StereonetJoint>,
    highlightedJoint: StereonetJoint?,
    wedgeIntersections: List<WedgeIntersection>,
    selectedJointId: String?,
    isEqualArea: Boolean,
    isUpperHemisphere: Boolean,
    theme: StereonetTheme,
    pointSize: Float = 5f,
    showLabels: Boolean = false,
    visibleLayers: Map<StereonetFeatureType, Boolean> = mapOf(
        StereonetFeatureType.JOINT to true,
        StereonetFeatureType.PLANE to true,
        StereonetFeatureType.FAULT to true,
        StereonetFeatureType.LINE to true
    ),
    rotX: Double = 0.0,
    rotY: Double = 0.0,
    rotZ: Double = 0.0,
    zoomScale: Float = 1.0f,
    onTap: ((dip: Int, dipDir: Int) -> Unit)? = null,
    targetDip: Int? = null,
    targetDipDir: Int? = null,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(androidx.compose.ui.graphics.RectangleShape)
            .pointerInput(isEqualArea, zoomScale) {
                detectTapGestures { tapOffset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val baseR = (Math.min(size.width, size.height) / 2f) - 10f
                    val r = baseR * zoomScale
                    if (r > 0 && onTap != null) {
                        val dx = tapOffset.x - cx
                        val dy = cy - tapOffset.y
                        val R = Math.sqrt((dx * dx + dy * dy).toDouble())
                        if (R <= r) {
                            val dipDeg: Double
                            if (isEqualArea) {
                                val arg = (R / (r * Math.sqrt(2.0))).coerceIn(0.0, 1.0)
                                dipDeg = 2.0 * Math.toDegrees(Math.asin(arg))
                            } else {
                                val arg = (R / r).coerceIn(0.0, 1.0)
                                dipDeg = 2.0 * Math.toDegrees(Math.atan(arg))
                            }
                            val clampedDip = dipDeg.coerceIn(0.0, 90.0)
                            val trendRad = Math.atan2(dx.toDouble(), dy.toDouble())
                            var trendDeg = Math.toDegrees(trendRad)
                            if (trendDeg < 0.0) trendDeg += 360.0

                            val dipDir = (trendDeg + 180.0) % 360.0
                            onTap(clampedDip.toInt(), dipDir.toInt())
                        }
                    }
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = (Math.min(size.width, size.height) / 2f) - 10f
        val r = baseR * zoomScale

        if (r <= 0) return@Canvas

        // Retrieve theme colors
        val gridColor = when (theme) {
            StereonetTheme.OBSIDIAN -> Color(0xFF00FFCC).copy(alpha = 0.2f)
            StereonetTheme.SLATE -> Color(0xFFECEFF1).copy(alpha = 0.25f)
            StereonetTheme.PAPER -> Color(0xFF757575).copy(alpha = 0.2f)
            StereonetTheme.TERMINAL -> Color(0xFF00FF00).copy(alpha = 0.25f)
        }
        val labelColor = when (theme) {
            StereonetTheme.OBSIDIAN -> Color(0xFF00E676)
            StereonetTheme.SLATE -> Color(0xFFCFD8DC)
            StereonetTheme.PAPER -> Color(0xFF212121)
            StereonetTheme.TERMINAL -> Color(0xFF00FF00)
        }
        val boundaryColor = when (theme) {
            StereonetTheme.OBSIDIAN -> Color(0xFF424242)
            StereonetTheme.SLATE -> Color(0xFF90A4AE)
            StereonetTheme.PAPER -> Color(0xFF212121)
            StereonetTheme.TERMINAL -> Color(0xFF00FF00)
        }

        // Draw concentric dip grid circles at 10°, 20°, 40°, 60°, 80°
        listOf(10.0, 20.0, 40.0, 60.0, 80.0).forEach { dipDeg ->
            val radRadius = getProjectionRadius(dipDeg, r, isEqualArea)
            drawCircle(
                color = gridColor,
                radius = radRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 0.5.dp.toPx())
            )
        }

        // Draw radiating dip-direction grid lines at 30° intervals
        for (angleDeg in 0 until 360 step 30) {
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val px = cx + r * Math.sin(angleRad).toFloat()
            val py = cy - r * Math.cos(angleRad).toFloat()
            drawLine(
                color = gridColor,
                start = Offset(cx, cy),
                end = Offset(px, py),
                strokeWidth = 0.5.dp.toPx()
            )
        }

        // Draw the outer boundary circle
        drawCircle(
            color = boundaryColor,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Draw Cardinal labels (N, E, S, W)
        val textPaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = 10.dp.toPx()
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText("N", cx, cy - r - 6f, textPaint)
        drawContext.canvas.nativeCanvas.drawText("S", cx, cy + r + 14f, textPaint)
        
        textPaint.textAlign = android.graphics.Paint.Align.LEFT
        drawContext.canvas.nativeCanvas.drawText("E", cx + r + 6f, cy + 4f, textPaint)
        
        textPaint.textAlign = android.graphics.Paint.Align.RIGHT
        drawContext.canvas.nativeCanvas.drawText("W", cx - r - 6f, cy + 4f, textPaint)

        // ----------------- DRAW BASAL FRICTION CIRCLE -----------------
        val fRadius = getProjectionRadius(frictionAngle, r, isEqualArea)
        drawCircle(
            color = Color(0xFFFFB74D).copy(alpha = 0.4f),
            radius = fRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        )

        // ----------------- DRAW PLANAR SLIDING DANGER AREA -----------------
        val planarZonePath = Path()
        val numSteps = 24
        val limitAngleMin = slopeDipDir - lateralLimit
        val limitAngleMax = slopeDipDir + lateralLimit

        val envelopePoints = getPlaneGreatCirclePoints(slopeDip, slopeDipDir, cx, cy, r, isEqualArea)
        val filteredEnvelopePoints = envelopePoints.filter { p ->
            val angleFromCenter = Math.toDegrees(Math.atan2((p.x - cx).toDouble(), (cy - p.y).toDouble()))
            val angle360 = (angleFromCenter + 360.0) % 360.0
            val diff = Math.abs(angle360 - slopeDipDir)
            val cleanDiff = if (diff > 180) 360 - diff else diff
            cleanDiff <= lateralLimit
        }

        if (filteredEnvelopePoints.isNotEmpty()) {
            planarZonePath.moveTo(cx, cy)
            planarZonePath.moveTo(filteredEnvelopePoints.first().x, filteredEnvelopePoints.first().y)
            for (i in 1 until filteredEnvelopePoints.size) {
                planarZonePath.lineTo(filteredEnvelopePoints[i].x, filteredEnvelopePoints[i].y)
            }
            
            for (step in 0..numSteps) {
                val alpha = step.toDouble() / numSteps
                val ang = Math.toRadians(limitAngleMax - alpha * (2 * lateralLimit))
                val px = cx + fRadius * Math.sin(ang).toFloat()
                val py = cy - fRadius * Math.cos(ang).toFloat()
                planarZonePath.lineTo(px, py)
            }
            planarZonePath.close()

            drawPath(
                path = planarZonePath,
                color = Color(0xFFFF5252).copy(alpha = 0.12f)
            )
        }

        // ----------------- DRAW DAYLIGHT ENVELOPE (GREAT CIRCLE OF SLOPE FACE) -----------------
        val slopeEnvelopePoints = getPlaneGreatCirclePoints(slopeDip, slopeDipDir, cx, cy, r, isEqualArea)
        if (slopeEnvelopePoints.size > 1) {
            val slopePath = Path().apply {
                moveTo(slopeEnvelopePoints.first().x, slopeEnvelopePoints.first().y)
                for (i in 1 until slopeEnvelopePoints.size) {
                    lineTo(slopeEnvelopePoints[i].x, slopeEnvelopePoints[i].y)
                }
            }
            drawPath(
                path = slopePath,
                color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // ----------------- DRAW LATERAL BOUNDS -----------------
        val latMinRad = Math.toRadians(limitAngleMin)
        val latMaxRad = Math.toRadians(limitAngleMax)
        drawLine(
            color = Color(0xFFFFB74D).copy(alpha = 0.5f),
            start = Offset(cx, cy),
            end = Offset(cx + r * Math.sin(latMinRad).toFloat(), cy - r * Math.cos(latMinRad).toFloat()),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color(0xFFFFB74D).copy(alpha = 0.5f),
            start = Offset(cx, cy),
            end = Offset(cx + r * Math.sin(latMaxRad).toFloat(), cy - r * Math.cos(latMaxRad).toFloat()),
            strokeWidth = 1.dp.toPx()
        )

        // ----------------- PLOT INDIVIDUAL GEOLOGICAL FEATURES -----------------
        joints.forEach { joint ->
            // Skip drawing if the layer is disabled
            if (visibleLayers[joint.type] == false) return@forEach

            val isHighlighted = joint.id == highlightedJoint?.id
            val finalPointSize = if (isHighlighted) (pointSize + 4f) else pointSize

            if (joint.type == StereonetFeatureType.LINE) {
                // Plot Lineation using rotated plunge and trend
                val plungeRad = Math.toRadians(joint.dip.toDouble())
                val trendRad = Math.toRadians(joint.dipDirection.toDouble())
                val l = Math.cos(plungeRad) * Math.sin(trendRad)
                val m = Math.cos(plungeRad) * Math.cos(trendRad)
                val n = -Math.sin(plungeRad)

                // Apply rotation matrix transformations
                val (rx, ry, rz) = rotateUnitVector(l, m, n, rotX, rotY, rotZ)

                var xCoord = rx
                var yCoord = ry
                var zCoord = rz
                if (zCoord > 0) {
                    xCoord = -xCoord
                    yCoord = -yCoord
                    zCoord = -zCoord
                }

                val dipOnSphere = Math.toDegrees(Math.asin(Math.abs(zCoord)))
                val rotatedTrend = Math.atan2(xCoord, yCoord)
                val plungeRadius = getProjectionRadius(90.0 - dipOnSphere, r, isEqualArea)

                val lineOffset = Offset(
                    cx + plungeRadius * Math.sin(rotatedTrend).toFloat(),
                    cy - plungeRadius * Math.cos(rotatedTrend).toFloat()
                )

                // Optional trend direction ray (faint)
                drawLine(
                    color = joint.color.copy(alpha = 0.15f),
                    start = Offset(cx, cy),
                    end = lineOffset,
                    strokeWidth = 0.8.dp.toPx()
                )

                // Draw X symbol for lineation
                val sz = finalPointSize * 1.2f
                drawLine(
                    color = joint.color,
                    start = Offset(lineOffset.x - sz, lineOffset.y - sz),
                    end = Offset(lineOffset.x + sz, lineOffset.y + sz),
                    strokeWidth = (if (isHighlighted) 2.5f else 1.5f).dp.toPx()
                )
                drawLine(
                    color = joint.color,
                    start = Offset(lineOffset.x - sz, lineOffset.y + sz),
                    end = Offset(lineOffset.x + sz, lineOffset.y - sz),
                    strokeWidth = (if (isHighlighted) 2.5f else 1.5f).dp.toPx()
                )
                drawCircle(
                    color = Color.White,
                    radius = if (isHighlighted) 2.5.dp.toPx() else 1.2.dp.toPx(),
                    center = lineOffset
                )

                // Draw Point Label
                if (showLabels) {
                    val textPaintSec = android.graphics.Paint().apply {
                        color = joint.color.toArgb()
                        textSize = 8.dp.toPx()
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        joint.name.take(15),
                        lineOffset.x + sz + 4f,
                        lineOffset.y + 3f,
                        textPaintSec
                    )
                }
            } else {
                // Plot Planar feature pole with 3D rotation
                val poleOffset = getRotatedPoleCoords(
                    joint.dip.toDouble(), joint.dipDirection.toDouble(),
                    rotX, rotY, rotZ, cx, cy, r, isEqualArea
                )

                // Check if original joint pole is in planar/toppling risk zone
                val inPlanarDanger = isJointPlanarSlidingHazard(
                    joint.dip.toDouble(), joint.dipDirection.toDouble(),
                    slopeDip, slopeDipDir, frictionAngle, lateralLimit
                )
                val inToppleDanger = isJointTopplingHazard(
                    joint.dip.toDouble(), joint.dipDirection.toDouble(),
                    slopeDip, slopeDipDir, frictionAngle, lateralLimit
                )

                if ((inPlanarDanger || inToppleDanger) && rotX == 0.0 && rotY == 0.0 && rotZ == 0.0) {
                    drawCircle(
                        color = if (inPlanarDanger) Color(0xFFFF1744) else Color(0xFFFF9100),
                        radius = finalPointSize + 4.dp.toPx(),
                        center = poleOffset,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Draw type-specific symbol for plane pole
                when (joint.type) {
                    StereonetFeatureType.JOINT -> {
                        drawCircle(
                            color = joint.color,
                            radius = finalPointSize,
                            center = poleOffset
                        )
                    }
                    StereonetFeatureType.PLANE -> {
                        val sz = finalPointSize * 1.3f
                        val triPath = Path().apply {
                            moveTo(poleOffset.x, poleOffset.y - sz)
                            lineTo(poleOffset.x + sz, poleOffset.y + sz)
                            lineTo(poleOffset.x - sz, poleOffset.y + sz)
                            close()
                        }
                        drawPath(path = triPath, color = joint.color)
                    }
                    StereonetFeatureType.FAULT -> {
                        val sz = finalPointSize * 2f
                        drawRect(
                            color = joint.color,
                            topLeft = Offset(poleOffset.x - sz / 2f, poleOffset.y - sz / 2f),
                            size = Size(sz, sz)
                        )
                    }
                    else -> {}
                }

                // Inner high-contrast core
                drawCircle(
                    color = Color.White,
                    radius = if (isHighlighted) 2.5.dp.toPx() else 1.2.dp.toPx(),
                    center = poleOffset
                )

                // Draw Point Label
                if (showLabels) {
                    val textPaintSec = android.graphics.Paint().apply {
                        color = joint.color.toArgb()
                        textSize = 8.dp.toPx()
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        joint.name.take(15),
                        poleOffset.x + finalPointSize + 4f,
                        poleOffset.y + 3f,
                        textPaintSec
                    )
                }

                // Draw non-highlighted custom planar features with thin transparent great circles
                if (!isHighlighted && !joint.isFromProject && rotX == 0.0 && rotY == 0.0 && rotZ == 0.0) {
                    val jCirclePoints = getPlaneGreatCirclePoints(joint.dip.toDouble(), joint.dipDirection.toDouble(), cx, cy, r, isEqualArea)
                    if (jCirclePoints.size > 1) {
                        val jPath = Path().apply {
                            moveTo(jCirclePoints.first().x, jCirclePoints.first().y)
                            for (i in 1 until jCirclePoints.size) {
                                lineTo(jCirclePoints[i].x, jCirclePoints[i].y)
                            }
                        }
                        drawPath(
                            path = jPath,
                            color = joint.color.copy(alpha = 0.25f),
                            style = if (joint.type == StereonetFeatureType.FAULT) {
                                Stroke(
                                    width = 0.8.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )
                            } else {
                                Stroke(width = 0.8.dp.toPx())
                            }
                        )
                    }
                }
            }
        }

        // ----------------- PLOT HIGHLIGHTED FEATURE'S GREAT CIRCLE (BOLDED) -----------------
        if (highlightedJoint != null && highlightedJoint.type != StereonetFeatureType.LINE && rotX == 0.0 && rotY == 0.0 && rotZ == 0.0) {
            val jCirclePoints = getPlaneGreatCirclePoints(highlightedJoint.dip.toDouble(), highlightedJoint.dipDirection.toDouble(), cx, cy, r, isEqualArea)
            if (jCirclePoints.size > 1) {
                val jPath = Path().apply {
                    moveTo(jCirclePoints.first().x, jCirclePoints.first().y)
                    for (i in 1 until jCirclePoints.size) {
                        lineTo(jCirclePoints[i].x, jCirclePoints[i].y)
                    }
                }
                drawPath(
                    path = jPath,
                    color = highlightedJoint.color,
                    style = if (highlightedJoint.type == StereonetFeatureType.FAULT) {
                        Stroke(
                            width = 2.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    } else {
                        Stroke(width = 2.2.dp.toPx())
                    }
                )
            }
        }

        // ----------------- PLOT WEDGE INTERSECTIONS -----------------
        if (rotX == 0.0 && rotY == 0.0 && rotZ == 0.0) {
            wedgeIntersections.forEach { inter ->
                // Skip if either joint is invisible
                if (visibleLayers[inter.jointA.type] == false || visibleLayers[inter.jointB.type] == false) return@forEach

                val R = getProjectionRadius(90.0 - inter.plunge, r, isEqualArea)
                val ix = cx + R * Math.sin(Math.toRadians(inter.trend)).toFloat()
                val iy = cy - R * Math.cos(Math.toRadians(inter.trend)).toFloat()
                val offset = Offset(ix, iy)

                val isSelectedJointInter = inter.jointA.id == selectedJointId || inter.jointB.id == selectedJointId

                // Draw a diamond symbol for intersection
                val size = if (isSelectedJointInter) 8.dp.toPx() else 5.dp.toPx()
                val path = Path().apply {
                    moveTo(ix, iy - size)
                    lineTo(ix + size, iy)
                    lineTo(ix, iy + size)
                    lineTo(ix - size, iy)
                    close()
                }

                drawPath(
                    path = path,
                    color = if (inter.isHazard) Color(0xFFFF1744) else Color(0xFF2979FF)
                )

                // Add white dot in center for high contrast
                drawCircle(
                    color = Color.White,
                    radius = if (isSelectedJointInter) 2.5.dp.toPx() else 1.5.dp.toPx(),
                    center = offset
                )

                // Outline of the diamond
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Draw a dynamic CAD-style targeting reticle
        if (targetDip != null && targetDipDir != null) {
            val targetOffset = getRotatedPoleCoords(
                targetDip.toDouble(), targetDipDir.toDouble(),
                rotX, rotY, rotZ, cx, cy, r, isEqualArea
            )
            drawCircle(
                color = Color(0xFFFF1744),
                radius = 8.dp.toPx(),
                center = targetOffset,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = targetOffset
            )
            drawLine(
                color = Color(0xFFFF1744),
                start = Offset(targetOffset.x - 12.dp.toPx(), targetOffset.y),
                end = Offset(targetOffset.x + 12.dp.toPx(), targetOffset.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0xFFFF1744),
                start = Offset(targetOffset.x, targetOffset.y - 12.dp.toPx()),
                end = Offset(targetOffset.x, targetOffset.y + 12.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

// -------------------------------------------------------------------------
// STEREONET MATHEMATICAL KINEMATIC FORMULAS
// -------------------------------------------------------------------------

fun getProjectionRadius(dipDeg: Double, r: Float, isEqualArea: Boolean): Float {
    val dipRad = Math.toRadians(dipDeg)
    return if (isEqualArea) {
        // Schmidt Equal Area projection: r * sqrt(2) * sin(dip / 2)
        (r * Math.sqrt(2.0) * Math.sin(dipRad / 2.0)).toFloat()
    } else {
        // Wulff Equal Angle projection: r * tan(dip / 2)
        (r * Math.tan(dipRad / 2.0)).toFloat()
    }
}

fun getPoleCoords(dip: Double, dipDir: Double, cx: Float, cy: Float, r: Float, isEqualArea: Boolean): Offset {
    val trend = Math.toRadians((dipDir - 180.0 + 360.0) % 360.0)
    val R = getProjectionRadius(dip, r, isEqualArea)
    val px = cx + R * Math.sin(trend).toFloat()
    val py = cy - R * Math.cos(trend).toFloat()
    return Offset(px, py)
}

fun getPlaneGreatCirclePoints(dip: Double, dipDir: Double, cx: Float, cy: Float, r: Float, isEqualArea: Boolean): List<Offset> {
    val points = mutableListOf<Offset>()
    val strikeRad = Math.toRadians((dipDir - 90.0 + 360.0) % 360.0)
    val dipRad = Math.toRadians(dip)

    // Strike vector
    val sx = Math.sin(strikeRad)
    val sy = Math.cos(strikeRad)
    val sz = 0.0

    // Down-dip vector
    val vx = Math.cos(strikeRad) * Math.cos(dipRad)
    val vy = -Math.sin(strikeRad) * Math.cos(dipRad)
    val vz = -Math.sin(dipRad)

    for (alphaDeg in -90..90 step 2) {
        val alphaRad = Math.toRadians(alphaDeg.toDouble())
        val xS = Math.cos(alphaRad) * sx + Math.sin(alphaRad) * vx
        val yS = Math.cos(alphaRad) * sy + Math.sin(alphaRad) * vy
        val zS = Math.cos(alphaRad) * sz + Math.sin(alphaRad) * vz

        // Ensure we only project onto the lower hemisphere
        if (zS <= 0.0001) {
            val dipOnSphere = Math.toDegrees(Math.acos(-zS.coerceIn(-1.0, 1.0)))
            val trend = Math.atan2(xS, yS)
            
            val R = getProjectionRadius(dipOnSphere, r, isEqualArea)
            val px = cx + R * Math.sin(trend).toFloat()
            val py = cy - R * Math.cos(trend).toFloat()
            points.add(Offset(px, py))
        }
    }
    return points
}

// Planar sliding hazard predicate
fun isJointPlanarSlidingHazard(
    jd: Double, jdd: Double,
    slopeD: Double, slopeDD: Double,
    friction: Double, latLimit: Double
): Boolean {
    var angDiff = Math.abs(jdd - slopeDD)
    if (angDiff > 180) angDiff = 360 - angDiff
    return (angDiff <= latLimit) && (jd < slopeD) && (jd > friction)
}

// Toppling hazard predicate
fun isJointTopplingHazard(
    jd: Double, jdd: Double,
    slopeD: Double, slopeDD: Double,
    friction: Double, latLimit: Double
): Boolean {
    var toppleDiff = Math.abs(jdd - ((slopeDD - 180.0 + 360.0) % 360.0))
    if (toppleDiff > 180) toppleDiff = 360 - toppleDiff
    return (toppleDiff <= latLimit) && (jd >= (90.0 - slopeD + friction))
}

// Calculate intersection line of two planes (represented by dip and dip direction)
fun calculatePlaneIntersection(
    dipA: Double, dipDirA: Double,
    dipB: Double, dipDirB: Double
): Pair<Double, Double>? {
    val dA = Math.toRadians(dipA)
    val ddA = Math.toRadians(dipDirA)
    val dB = Math.toRadians(dipB)
    val ddB = Math.toRadians(dipDirB)

    // Unit normals to planes
    val nAx = Math.sin(dA) * Math.sin(ddA)
    val nAy = Math.sin(dA) * Math.cos(ddA)
    val nAz = Math.cos(dA)

    val nBx = Math.sin(dB) * Math.sin(ddB)
    val nBy = Math.sin(dB) * Math.cos(ddB)
    val nBz = Math.cos(dB)

    // Cross product
    var ix = nAy * nBz - nAz * nBy
    var iy = nAz * nBx - nAx * nBz
    var iz = nAx * nBy - nAy * nBx

    val len = Math.sqrt(ix * ix + iy * iy + iz * iz)
    if (len < 1e-6) return null // Parallel planes

    ix /= len
    iy /= len
    iz /= len

    // Vector pointing down into lower hemisphere
    if (iz > 0) {
        ix = -ix
        iy = -iy
        iz = -iz
    }

    // Plunge: angle with horizontal
    val plunge = Math.toDegrees(Math.asin(Math.abs(iz)))

    // Trend: azimuth in horizontal plane
    var trend = Math.toDegrees(Math.atan2(ix, iy))
    trend = (trend + 360.0) % 360.0

    return Pair(plunge, trend)
}

// Wedge sliding hazard predicate
fun isWedgeSlidingHazard(
    plunge: Double, trend: Double,
    slopeDip: Double, slopeDipDir: Double,
    frictionAngle: Double
): Boolean {
    val diff = Math.toRadians(trend - slopeDipDir)
    val cosDiff = Math.cos(diff)
    if (cosDiff <= 0) return false // Intersection points into slope face, does not daylight
    
    val apparentDip = Math.toDegrees(Math.atan(Math.tan(Math.toRadians(slopeDip)) * cosDiff))
    return plunge > frictionAngle && plunge < apparentDip
}

// Convert Strike + Quadrant to Dip Direction
fun resolveStrikeToDipDirection(strike: Double, quadrant: String): Double {
    // Dip direction is always perpendicular to strike (strike + 90 or strike - 90)
    val option1 = (strike + 90.0) % 360.0
    val option2 = (strike - 90.0 + 360.0) % 360.0

    // Center angles for target quadrants
    val targetAngle = when (quadrant.uppercase()) {
        "N" -> 0.0
        "NE" -> 45.0
        "E" -> 90.0
        "SE" -> 135.0
        "S" -> 180.0
        "SW" -> 225.0
        "W" -> 270.0
        "NW" -> 315.0
        else -> 135.0 // Default SE
    }

    // Choose the option with the smaller angular distance to target angle
    fun angularDistance(a: Double, b: Double): Double {
        val diff = Math.abs(a - b)
        return if (diff > 180.0) 360.0 - diff else diff
    }

    return if (angularDistance(option1, targetAngle) <= angularDistance(option2, targetAngle)) {
        option1
    } else {
        option2
    }
}

// -------------------------------------------------------------------------
// ADVANCED STEREONET MATHEMATICAL & STATISTICAL SOLVERS
// -------------------------------------------------------------------------

data class EigenResult(
    val eigenvalues: DoubleArray, // S1 >= S2 >= S3
    val eigenvectors: Array<DoubleArray> // Corresponding vectors
)

// Rotation around X, Y, Z coordinates for a 3D unit vector
fun rotateUnitVector(
    l: Double, m: Double, n: Double,
    degX: Double, degY: Double, degZ: Double
): Triple<Double, Double, Double> {
    val rx = Math.toRadians(degX)
    val ry = Math.toRadians(degY)
    val rz = Math.toRadians(degZ)
    
    var x = l
    var y = m
    var z = n
    
    // Rotate X
    if (degX != 0.0) {
        val y1 = y * Math.cos(rx) - z * Math.sin(rx)
        val z1 = y * Math.sin(rx) + z * Math.cos(rx)
        y = y1
        z = z1
    }
    // Rotate Y
    if (degY != 0.0) {
        val x1 = x * Math.cos(ry) + z * Math.sin(ry)
        val z1 = -x * Math.sin(ry) + z * Math.cos(ry)
        x = x1
        z = z1
    }
    // Rotate Z
    if (degZ != 0.0) {
        val x1 = x * Math.cos(rz) - y * Math.sin(rz)
        val y1 = x * Math.sin(rz) + y * Math.cos(rz)
        x = x1
        y = y1
    }
    
    return Triple(x, y, z)
}

// Get projected coordinate of rotated pole
fun getRotatedPoleCoords(
    dip: Double, dipDir: Double,
    rotX: Double, rotY: Double, rotZ: Double,
    cx: Float, cy: Float, r: Float, isEqualArea: Boolean
): Offset {
    val dipRad = Math.toRadians(dip)
    val dipDirRad = Math.toRadians(dipDir)
    
    // Original unit vector pointing down (lower hemisphere, z <= 0)
    val l = -Math.sin(dipRad) * Math.sin(dipDirRad)
    val m = -Math.sin(dipRad) * Math.cos(dipDirRad)
    val n = -Math.cos(dipRad)
    
    // Rotate unit vector
    val (rx, ry, rz) = rotateUnitVector(l, m, n, rotX, rotY, rotZ)
    
    var xCoord = rx
    var yCoord = ry
    var zCoord = rz
    if (zCoord > 0) {
        xCoord = -xCoord
        yCoord = -yCoord
        zCoord = -zCoord
    }
    
    val dipOnSphere = Math.toDegrees(Math.acos(-zCoord.coerceIn(-1.0, 1.0)))
    val trend = Math.atan2(xCoord, yCoord)
    
    val R = getProjectionRadius(dipOnSphere, r, isEqualArea)
    val px = cx + R * Math.sin(trend).toFloat()
    val py = cy - R * Math.cos(trend).toFloat()
    return Offset(px, py)
}

// High-precision Jacobi rotation algorithm for symmetric 3x3 matrices
fun solveSymmetricEigen3x3(matrix: Array<DoubleArray>): EigenResult {
    val a = Array(3) { i -> matrix[i].clone() }
    val v = Array(3) { i -> DoubleArray(3) { j -> if (i == j) 1.0 else 0.0 } }
    
    val maxSweep = 50
    val epsilon = 1e-9
    
    for (sweep in 0 until maxSweep) {
        var offDiag = 0.0
        for (i in 0..2) {
            for (j in i + 1..2) {
                offDiag += Math.abs(a[i][j])
            }
        }
        if (offDiag < epsilon) break
        
        for (p in 0..2) {
            for (q in p + 1..2) {
                val apq = a[p][q]
                if (Math.abs(apq) < 1e-15) continue
                
                val app = a[p][p]
                val aqq = a[q][q]
                val phi = 0.5 * (aqq - app)
                val t = if (phi >= 0) {
                    1.0 / (phi + Math.sqrt(phi * phi + apq * apq))
                } else {
                    -1.0 / (-phi + Math.sqrt(phi * phi + apq * apq))
                }
                val c = 1.0 / Math.sqrt(1.0 + t * t)
                val s = t * c
                val tau = s / (1.0 + c)
                
                a[p][q] = 0.0
                a[p][p] = app - t * apq
                a[q][q] = aqq + t * apq
                
                for (r in 0..2) {
                    if (r != p && r != q) {
                        val arp = a[r][p]
                        val arq = a[r][q]
                        a[r][p] = arp - s * (arq + arp * tau)
                        a[p][r] = a[r][p]
                        a[r][q] = arq + s * (arp - arq * tau)
                        a[q][r] = a[r][q]
                    }
                }
                
                for (r in 0..2) {
                    val vrp = v[r][p]
                    val vrq = v[r][q]
                    v[r][p] = vrp - s * (vrq + vrp * tau)
                    v[r][q] = vrq + s * (vrp - vrq * tau)
                }
            }
        }
    }
    
    // Sort descending by eigenvalues
    val indices = listOf(0, 1, 2).sortedByDescending { a[it][it] }
    val sortedEigenvalues = DoubleArray(3) { i -> a[indices[i]][indices[i]] }
    val sortedEigenvectors = Array(3) { i ->
        DoubleArray(3) { j -> v[j][indices[i]] }
    }
    
    return EigenResult(sortedEigenvalues, sortedEigenvectors)
}

// Calculate the 3x3 Orientation Tensor and perform Eigenvalue analysis
fun calculateOrientationTensor(joints: List<StereonetJoint>): EigenResult? {
    val planesOnly = joints.filter { it.type != StereonetFeatureType.LINE }
    if (planesOnly.isEmpty()) return null
    
    val n = planesOnly.size
    val matrix = Array(3) { DoubleArray(3) }
    
    planesOnly.forEach { j ->
        val dipRad = Math.toRadians(j.dip.toDouble())
        val dipDirRad = Math.toRadians(j.dipDirection.toDouble())
        
        // Pole normal vector in Cartesian coordinates
        val l = -Math.sin(dipRad) * Math.sin(dipDirRad)
        val m = -Math.sin(dipRad) * Math.cos(dipDirRad)
        val nCoord = -Math.cos(dipRad)
        
        matrix[0][0] += l * l
        matrix[0][1] += l * m
        matrix[0][2] += l * nCoord
        
        matrix[1][0] += l * m
        matrix[1][1] += m * m
        matrix[1][2] += m * nCoord
        
        matrix[2][0] += l * nCoord
        matrix[2][1] += m * nCoord
        matrix[2][2] += nCoord * nCoord
    }
    
    for (i in 0..2) {
        for (j in 0..2) {
            matrix[i][j] /= n.toDouble()
        }
    }
    
    return solveSymmetricEigen3x3(matrix)
}

// Calculate the Mean Vector plunge and trend of discontinuity poles
fun calculateMeanVector(joints: List<StereonetJoint>): Pair<Double, Double>? {
    val planesOnly = joints.filter { it.type != StereonetFeatureType.LINE }
    if (planesOnly.isEmpty()) return null
    
    var sumX = 0.0
    var sumY = 0.0
    var sumZ = 0.0
    
    planesOnly.forEach { j ->
        val dipRad = Math.toRadians(j.dip.toDouble())
        val dipDirRad = Math.toRadians(j.dipDirection.toDouble())
        
        val l = -Math.sin(dipRad) * Math.sin(dipDirRad)
        val m = -Math.sin(dipRad) * Math.cos(dipDirRad)
        val nCoord = -Math.cos(dipRad)
        
        sumX += l
        sumY += m
        sumZ += nCoord
    }
    
    val len = Math.sqrt(sumX * sumX + sumY * sumY + sumZ * sumZ)
    if (len < 1e-5) return null
    
    var mx = sumX / len
    var my = sumY / len
    var mz = sumZ / len
    
    if (mz > 0) {
        mx = -mx
        my = -my
        mz = -mz
    }
    
    val meanPlunge = Math.toDegrees(Math.asin(Math.abs(mz)))
    var meanTrend = Math.toDegrees(Math.atan2(mx, my))
    meanTrend = (meanTrend + 360.0) % 360.0
    
    return Pair(meanPlunge, meanTrend)
}

// -------------------------------------------------------------------------
// REUSABLE KINEMATIC REPORT DISPLAY CARD
// -------------------------------------------------------------------------

@Composable
fun KinematicStabilityReport(
    slopeDip: Double,
    slopeDipDir: Double,
    frictionAngle: Double,
    lateralLimit: Double,
    joints: List<StereonetJoint>,
    wedgeIntersections: List<WedgeIntersection>,
    isFarsiMode: Boolean
) {
    var planarCount = 0
    var toppleCount = 0
    var stableCount = 0

    val planesOnly = joints.filter { it.type != StereonetFeatureType.LINE }

    planesOnly.forEach { j ->
        val inPlanar = isJointPlanarSlidingHazard(j.dip.toDouble(), j.dipDirection.toDouble(), slopeDip, slopeDipDir, frictionAngle, lateralLimit)
        val inTopple = isJointTopplingHazard(j.dip.toDouble(), j.dipDirection.toDouble(), slopeDip, slopeDipDir, frictionAngle, lateralLimit)
        
        if (inPlanar) planarCount++
        if (inTopple) toppleCount++
        if (!inPlanar && !inTopple) stableCount++
    }

    val totalPlanes = planesOnly.size.coerceAtLeast(1)
    val planarPercent = Math.round((planarCount.toDouble() / totalPlanes) * 100.0)
    val topplePercent = Math.round((toppleCount.toDouble() / totalPlanes) * 100.0)

    val wedgeHazardCount = wedgeIntersections.count { it.isHazard }
    val totalIntersections = wedgeIntersections.size.coerceAtLeast(1)
    val wedgePercent = Math.round((wedgeHazardCount.toDouble() / totalIntersections) * 100.0)

    var isWedgeExpanded by remember { mutableStateOf(false) }

    val (planarSeverity, planarColor) = when {
        planarPercent > 40 -> (if (isFarsiMode) "خطر بحرانی" else "Critical Risk") to Color(0xFFD32F2F)
        planarPercent > 15 -> (if (isFarsiMode) "خطر متوسط" else "Moderate Risk") to Color(0xFFEF6C00)
        else -> (if (isFarsiMode) "ایمن ناچیز" else "Safe / Low Risk") to Color(0xFF2E7D32)
    }

    val (toppleSeverity, toppleColor) = when {
        topplePercent > 40 -> (if (isFarsiMode) "خطر بحرانی" else "Critical Risk") to Color(0xFFD32F2F)
        topplePercent > 15 -> (if (isFarsiMode) "خطر متوسط" else "Moderate Risk") to Color(0xFFEF6C00)
        else -> (if (isFarsiMode) "ایمن ناچیز" else "Safe / Low Risk") to Color(0xFF2E7D32)
    }

    val (wedgeSeverity, wedgeColor) = when {
        wedgePercent > 40 -> (if (isFarsiMode) "خطر بحرانی" else "Critical Risk") to Color(0xFFD32F2F)
        wedgePercent > 15 -> (if (isFarsiMode) "خطر متوسط" else "Moderate Risk") to Color(0xFFEF6C00)
        else -> (if (isFarsiMode) "ایمن ناچیز" else "Safe / Low Risk") to Color(0xFF2E7D32)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isFarsiMode) "گزارش ارزیابی سینماتیک توده سنگ و پایداری شیروانی" else "Slope Kinematic Failure Mode Stability Verdict",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Planar Sliding Verdict Column
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = planarColor.copy(alpha = 0.08f)),
                    border = BorderStroke(0.5.dp, planarColor.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(if (isFarsiMode) "لغزش صفحه‌ای" else "Planar Sliding", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("$planarPercent%", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = planarColor)
                        Text(planarSeverity, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = planarColor, textAlign = TextAlign.Center)
                        Text(
                            text = if (isFarsiMode) "$planarCount درزه بحرانی" else "$planarCount critical",
                            fontSize = 7.5.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Wedge Sliding Verdict Column
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = wedgeColor.copy(alpha = 0.08f)),
                    border = BorderStroke(0.5.dp, wedgeColor.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(if (isFarsiMode) "لغزش گوه" else "Wedge Sliding", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("$wedgePercent%", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = wedgeColor)
                        Text(wedgeSeverity, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = wedgeColor, textAlign = TextAlign.Center)
                        Text(
                            text = if (isFarsiMode) "$wedgeHazardCount تقاطع بحرانی" else "$wedgeHazardCount hazard intersection",
                            fontSize = 7.5.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Toppling Verdict Column
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = toppleColor.copy(alpha = 0.08f)),
                    border = BorderStroke(0.5.dp, toppleColor.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(if (isFarsiMode) "لغزش واژگونی" else "Toppling Failure", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("$topplePercent%", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = toppleColor)
                        Text(toppleSeverity, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = toppleColor, textAlign = TextAlign.Center)
                        Text(
                            text = if (isFarsiMode) "$toppleCount درزه بحرانی" else "$toppleCount critical",
                            fontSize = 7.5.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Expandable Intersections details section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isWedgeExpanded = !isWedgeExpanded }
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = if (isWedgeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isFarsiMode) "مشاهده جزئیات خطوط تقاطع (${wedgeIntersections.size})" else "Wedge Intersections details (${wedgeIntersections.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (isFarsiMode) "تحلیل گوه دوبعدی" else "2D Wedge Analysis",
                        fontSize = 8.sp,
                        color = Color.Gray
                    )
                }

                AnimatedVisibility(visible = isWedgeExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .heightIn(max = 140.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (wedgeIntersections.isEmpty()) {
                            Text(
                                text = if (isFarsiMode) "هیچ تقاطعی محاسبه نشد (حداقل دو درزه بر روی نمودار ترسیم کنید)" else "No intersections computed (add at least 2 joints)",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            wedgeIntersections.forEach { inter ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (inter.isHazard) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, if (inter.isHazard) Color(0xFFFFCDD2) else Color(0xFFC8E6C9))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1.3f)) {
                                            Text(
                                                text = "${inter.jointA.name} ∩ ${inter.jointB.name}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isFarsiMode) "پلنج: ${String.format("%.1f", inter.plunge)}° ← ترند: ${String.format("%.1f", inter.trend)}°" else "Plunge: ${String.format("%.1f", inter.plunge)}° → Trend: ${String.format("%.1f", inter.trend)}°",
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.DarkGray
                                            )
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (inter.isHazard) Color(0xFFFFCDD2) else Color(0xFFC8E6C9))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (inter.isHazard) (if (isFarsiMode) "ریزش گوه" else "Wedge Risk") else (if (isFarsiMode) "ایمن" else "Safe"),
                                                color = if (inter.isHazard) Color(0xFFC62828) else Color(0xFF2E7D32),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Structural engineering recommendation text
            val recommendationText = remember(planarPercent, wedgePercent, topplePercent, isFarsiMode) {
                if (planarPercent > 20 || wedgePercent > 20 || topplePercent > 20) {
                    if (isFarsiMode) {
                        "هشدار مهندسی: احتمال شکست ساختاری سینماتیک در این پله معدنی یا شیب ترانشه وجود دارد (لغزش صفحه‌ای: $planarPercent%، گوه: $wedgePercent%، واژگونی: $topplePercent%). پیشنهاد می‌شود جهت پایدارسازی از پین‌های سنگی فعال (تنش‌دار) با زاویه عمود بر امتداد درزه، توری‌های مش فولادی گالوانیزه یا لایه‌ای شاتکریت مسلح استفاده نمایید."
                    } else {
                        "Engineering Warning: Significant structural failure potential detected (Planar: $planarPercent%, Wedge: $wedgePercent%, Toppling: $topplePercent%). Mitigation recommendation: Install active tensioned rock-bolts oriented perpendicular to discontinuity strike, secure with heavy galvanized wire mesh, or apply steel-fiber reinforced shotcrete layers."
                    }
                } else {
                    if (isFarsiMode) {
                        "وضعیت پایداری: بر اساس معیارهای سینماتیکی، پایداری توده سنگ در محدوده ایمن قرار دارد. برداشت‌های ادواری درزه‌ها در حین پیشروی استخراج پله معدنی توصیه می‌شود."
                    } else {
                        "Stability Status: Geotechnical rock slope is kinematically stable. Discontinuities and joints are within safe bounds. Routine periodic structural mapping during excavation advance is recommended."
                    }
                }
            }

            Text(
                text = recommendationText,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 12.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

// -------------------------------------------------------------------------
// PROFESSIONAL BILINGUAL PDF GEOTECHNICAL REPORT GENERATOR
// -------------------------------------------------------------------------
fun exportStereonetToPdf(
    context: android.content.Context,
    projectName: String,
    slopeDip: Double,
    slopeDipDir: Double,
    frictionAngle: Double,
    lateralLimit: Double,
    joints: List<StereonetJoint>,
    wedgeIntersections: List<WedgeIntersection>,
    isEqualArea: Boolean,
    isUpperHemisphere: Boolean,
    isFarsi: Boolean
) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    // A4 Page Size: 595 x 842 points
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    try {
        // 1. HEADER BANNER (Slate Dark/Royal Blue theme)
        val headerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#1A237E") // Dark Indigo
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, 595f, 80f, headerPaint)

        // Title Texts
        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 13.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("GEOTECHNICAL STEREONET STABILITY REPORT", 20f, 32f, titlePaint)

        val titleFaPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 12.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("گزارش پایداری استریونت توده سنگ ژئوتکنیکال", 300f, 32f, titleFaPaint)

        val subTitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            textSize = 8.5f
            isAntiAlias = true
        }
        canvas.drawText("Bilingual Kinematic Stereonet Projection & Limit Equilibrium Analysis", 20f, 52f, subTitlePaint)
        canvas.drawText("تحلیل ساختاری و پایداری شیب‌های سنگی معدنی و عمرانی", 300f, 52f, subTitlePaint)

        // 2. METADATA SECTION
        val metaLabelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#37474F")
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val metaValPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#263238")
            textSize = 9f
            isAntiAlias = true
        }

        val df = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val currentDate = df.format(java.util.Date())

        // Left Metadata Column
        canvas.drawText("Project / پروژه:", 25f, 105f, metaLabelPaint)
        canvas.drawText(projectName, 135f, 105f, metaValPaint)

        canvas.drawText("Date of Export / تاریخ:", 25f, 122f, metaLabelPaint)
        canvas.drawText(currentDate, 135f, 122f, metaValPaint)

        canvas.drawText("Analyst / کارشناس:", 25f, 139f, metaLabelPaint)
        canvas.drawText("farzadfathollazadeh@gmail.com", 135f, 139f, metaValPaint)

        // Right Metadata Column
        canvas.drawText("Slope Dip / زاویه شیب:", 320f, 105f, metaLabelPaint)
        canvas.drawText("${slopeDip.toInt()}°", 460f, 105f, metaValPaint)

        canvas.drawText("Dip Direction / جهت شیب:", 320f, 122f, metaLabelPaint)
        canvas.drawText("${slopeDipDir.toInt()}°", 460f, 122f, metaValPaint)

        canvas.drawText("Friction Angle / زاویه اصطکاک:", 320f, 139f, metaLabelPaint)
        canvas.drawText("${frictionAngle.toInt()}° (Limits: ${lateralLimit.toInt()}°)", 460f, 139f, metaValPaint)

        // Divider Line
        val dividerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#CFD8DC")
            strokeWidth = 0.8f
        }
        canvas.drawLine(20f, 152f, 575f, 152f, dividerPaint)

        // 3. STEREONET VECTOR DRAWING GRAPHICS
        val cx = 297.5f
        val cy = 305f
        val r = 110f // Fits perfectly inside A4 page

        val sGridPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#ECEFF1")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 0.6f
            isAntiAlias = true
        }
        val sBoundaryPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#455A64")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        // Concentric Circles (Schmidt or Wulff)
        listOf(10.0, 20.0, 40.0, 60.0, 80.0).forEach { dipDeg ->
            val radRadius = getProjectionRadius(dipDeg, r, isEqualArea)
            canvas.drawCircle(cx, cy, radRadius, sGridPaint)
        }

        // Radiating Trend Lines (every 30 deg)
        for (angleDeg in 0 until 360 step 30) {
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val px = cx + r * Math.sin(angleRad).toFloat()
            val py = cy - r * Math.cos(angleRad).toFloat()
            canvas.drawLine(cx, cy, px, py, sGridPaint)
        }

        // Outer Primitive Boundary
        canvas.drawCircle(cx, cy, r, sBoundaryPaint)

        // Cardinal Labels (N, E, S, W)
        val sLabelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#37474F")
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("N", cx, cy - r - 5f, sLabelPaint)
        canvas.drawText("S", cx, cy + r + 11f, sLabelPaint)
        sLabelPaint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText("E", cx + r + 5f, cy + 3f, sLabelPaint)
        sLabelPaint.textAlign = android.graphics.Paint.Align.RIGHT
        canvas.drawText("W", cx - r - 5f, cy + 3f, sLabelPaint)

        // Basal Friction Circle (Dashed)
        val fRadius = getProjectionRadius(frictionAngle, r, isEqualArea)
        val frictionPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FB8C00") // Orange
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 0.8f
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f)
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, fRadius, frictionPaint)

        // Planar sliding hazard danger envelope zone
        val limitAngleMin = slopeDipDir - lateralLimit
        val limitAngleMax = slopeDipDir + lateralLimit

        val pZonePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#26FF1744") // Pale transparent red
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }

        val envelopePoints = getPlaneGreatCirclePoints(slopeDip, slopeDipDir, cx, cy, r, isEqualArea)
        val filteredEnvelopePoints = envelopePoints.filter { p ->
            val angleFromCenter = Math.toDegrees(Math.atan2((p.x - cx).toDouble(), (cy - p.y).toDouble()))
            val angle360 = (angleFromCenter + 360.0) % 360.0
            val diff = Math.abs(angle360 - slopeDipDir)
            val cleanDiff = if (diff > 180) 360 - diff else diff
            cleanDiff <= lateralLimit
        }

        if (filteredEnvelopePoints.isNotEmpty()) {
            val planarPath = android.graphics.Path()
            planarPath.moveTo(filteredEnvelopePoints.first().x, filteredEnvelopePoints.first().y)
            for (i in 1 until filteredEnvelopePoints.size) {
                planarPath.lineTo(filteredEnvelopePoints[i].x, filteredEnvelopePoints[i].y)
            }
            val numSteps = 24
            for (step in 0..numSteps) {
                val alpha = step.toDouble() / numSteps
                val ang = Math.toRadians(limitAngleMax - alpha * (2 * lateralLimit))
                val px = cx + fRadius * Math.sin(ang).toFloat()
                val py = cy - fRadius * Math.cos(ang).toFloat()
                planarPath.lineTo(px, py)
            }
            planarPath.close()
            canvas.drawPath(planarPath, pZonePaint)
        }

        // Daylight Envelope of Slope Face (Cyan line)
        if (envelopePoints.size > 1) {
            val slopePath = android.graphics.Path().apply {
                moveTo(envelopePoints.first().x, envelopePoints.first().y)
                for (i in 1 until envelopePoints.size) {
                    lineTo(envelopePoints[i].x, envelopePoints[i].y)
                }
            }
            val slopeEnvelopePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#00E5FF")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 1.4f
                isAntiAlias = true
            }
            canvas.drawPath(slopePath, slopeEnvelopePaint)
        }

        // Lateral Limits Bound Lines (Dashed orange)
        val latBoundPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FB8C00")
            strokeWidth = 0.8f
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(4f, 4f), 0f)
            isAntiAlias = true
        }
        val latMinRad = Math.toRadians(limitAngleMin)
        val latMaxRad = Math.toRadians(limitAngleMax)
        canvas.drawLine(cx, cy, cx + r * Math.sin(latMinRad).toFloat(), cy - r * Math.cos(latMinRad).toFloat(), latBoundPaint)
        canvas.drawLine(cx, cy, cx + r * Math.sin(latMaxRad).toFloat(), cy - r * Math.cos(latMaxRad).toFloat(), latBoundPaint)

        // Plot Individual Features
        joints.forEach { joint ->
            val isPlanarDanger = isJointPlanarSlidingHazard(joint.dip.toDouble(), joint.dipDirection.toDouble(), slopeDip, slopeDipDir, frictionAngle, lateralLimit)
            val isToppleDanger = isJointTopplingHazard(joint.dip.toDouble(), joint.dipDirection.toDouble(), slopeDip, slopeDipDir, frictionAngle, lateralLimit)

            val jPaint = android.graphics.Paint().apply {
                color = joint.color.toArgb()
                isAntiAlias = true
            }

            if (joint.type == StereonetFeatureType.LINE) {
                // Lineation Point Plot
                val plungeRad = Math.toRadians(joint.dip.toDouble())
                val trendRad = Math.toRadians(joint.dipDirection.toDouble())
                val zCoord = -Math.sin(plungeRad)
                val dipOnSphere = Math.toDegrees(Math.asin(Math.abs(zCoord)))
                val plungeRadius = getProjectionRadius(90.0 - dipOnSphere, r, isEqualArea)

                val lx = cx + plungeRadius * Math.sin(trendRad).toFloat()
                val ly = cy - plungeRadius * Math.cos(trendRad).toFloat()

                // Draw small Cross X
                jPaint.style = android.graphics.Paint.Style.STROKE
                jPaint.strokeWidth = 1.2f
                canvas.drawLine(lx - 3.5f, ly - 3.5f, lx + 3.5f, ly + 3.5f, jPaint)
                canvas.drawLine(lx - 3.5f, ly + 3.5f, lx + 3.5f, ly - 3.5f, jPaint)
            } else {
                // Planar pole coordinates
                val poleOffset = getRotatedPoleCoords(joint.dip.toDouble(), joint.dipDirection.toDouble(), 0.0, 0.0, 0.0, cx, cy, r, isEqualArea)

                // Hazard red ring highlight
                if (isPlanarDanger || isToppleDanger) {
                    val hazRing = android.graphics.Paint().apply {
                        color = if (isPlanarDanger) android.graphics.Color.RED else android.graphics.Color.parseColor("#FF9100")
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 1f
                        isAntiAlias = true
                    }
                    canvas.drawCircle(poleOffset.x, poleOffset.y, 6.5f, hazRing)
                }

                // Draw native shape
                when (joint.type) {
                    StereonetFeatureType.JOINT -> {
                        jPaint.style = android.graphics.Paint.Style.FILL
                        canvas.drawCircle(poleOffset.x, poleOffset.y, 3.5f, jPaint)
                    }
                    StereonetFeatureType.PLANE -> {
                        jPaint.style = android.graphics.Paint.Style.FILL
                        val pPath = android.graphics.Path().apply {
                            moveTo(poleOffset.x, poleOffset.y - 4.2f)
                            lineTo(poleOffset.x + 4.2f, poleOffset.y + 4.2f)
                            lineTo(poleOffset.x - 4.2f, poleOffset.y + 4.2f)
                            close()
                        }
                        canvas.drawPath(pPath, jPaint)
                    }
                    StereonetFeatureType.FAULT -> {
                        jPaint.style = android.graphics.Paint.Style.FILL
                        canvas.drawRect(poleOffset.x - 3.2f, poleOffset.y - 3.2f, poleOffset.x + 3.2f, poleOffset.y + 3.2f, jPaint)
                    }
                    else -> {}
                }

                // Draw tiny high-contrast white core
                val wCore = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = android.graphics.Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawCircle(poleOffset.x, poleOffset.y, 1f, wCore)

                // Plot Great Circle
                val jCirclePoints = getPlaneGreatCirclePoints(joint.dip.toDouble(), joint.dipDirection.toDouble(), cx, cy, r, isEqualArea)
                if (jCirclePoints.size > 1) {
                    val jPath = android.graphics.Path().apply {
                        moveTo(jCirclePoints.first().x, jCirclePoints.first().y)
                        for (i in 1 until jCirclePoints.size) {
                            lineTo(jCirclePoints[i].x, jCirclePoints[i].y)
                        }
                    }
                    val circlePaint = android.graphics.Paint().apply {
                        color = joint.color.toArgb()
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 0.5f
                        isAntiAlias = true
                        if (joint.type == StereonetFeatureType.FAULT) {
                            pathEffect = android.graphics.DashPathEffect(floatArrayOf(3f, 3f), 0f)
                        }
                    }
                    canvas.drawPath(jPath, circlePaint)
                }
            }
        }

        // Plot Wedge Intersections (Diamonds)
        wedgeIntersections.forEach { inter ->
            val R = getProjectionRadius(90.0 - inter.plunge, r, isEqualArea)
            val ix = cx + R * Math.sin(Math.toRadians(inter.trend)).toFloat()
            val iy = cy - R * Math.cos(Math.toRadians(inter.trend)).toFloat()

            val interPaint = android.graphics.Paint().apply {
                color = if (inter.isHazard) android.graphics.Color.RED else android.graphics.Color.parseColor("#2979FF")
                style = android.graphics.Paint.Style.FILL
                isAntiAlias = true
            }
            val iPath = android.graphics.Path().apply {
                moveTo(ix, iy - 4.5f)
                lineTo(ix + 4.5f, iy)
                lineTo(ix, iy + 4.5f)
                lineTo(ix - 4.5f, iy)
                close()
            }
            canvas.drawPath(iPath, interPaint)

            val strokePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 0.7f
                isAntiAlias = true
            }
            canvas.drawPath(iPath, strokePaint)
        }

        // 4. LEGEND BLOCK
        val legTextPaint = android.graphics.Paint().apply {
            textSize = 7.5f
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#455A64")
        }
        val legTitlePaint = android.graphics.Paint().apply {
            textSize = 8f
            isFakeBoldText = true
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#263238")
        }

        canvas.drawText("LEGEND / راهنما:", 35f, 432f, legTitlePaint)

        // Joint
        legTextPaint.color = android.graphics.Color.parseColor("#4CAF50")
        canvas.drawCircle(125f, 429f, 3f, legTextPaint)
        canvas.drawText("Joint (درزه)", 134f, 432f, legTextPaint)

        // Bedding
        legTextPaint.color = android.graphics.Color.parseColor("#2196F3")
        val tPath = android.graphics.Path().apply {
            moveTo(215f, 426f)
            lineTo(218.5f, 432f)
            lineTo(211.5f, 432f)
            close()
        }
        canvas.drawPath(tPath, legTextPaint)
        canvas.drawText("Bedding (لایه‌بندی)", 224f, 432f, legTextPaint)

        // Fault
        legTextPaint.color = android.graphics.Color.parseColor("#F44336")
        canvas.drawRect(310f, 426f, 316f, 432f, legTextPaint)
        canvas.drawText("Fault (گسل)", 322f, 432f, legTextPaint)

        // Wedge Intersection
        legTextPaint.color = android.graphics.Color.RED
        val dPath = android.graphics.Path().apply {
            moveTo(400f, 425f)
            lineTo(403.5f, 428.5f)
            lineTo(400f, 432f)
            lineTo(396.5f, 428.5f)
            close()
        }
        canvas.drawPath(dPath, legTextPaint)
        canvas.drawText("Wedge Risk (گوه)", 410f, 432f, legTextPaint)

        // 5. STABILITY REPORT HUD BOX (Shaded panel)
        val boxBg = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#F8F9FA")
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        val boxBorder = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#ECEFF1")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        canvas.drawRoundRect(20f, 445f, 575f, 520f, 8f, 8f, boxBg)
        canvas.drawRoundRect(20f, 445f, 575f, 520f, 8f, 8f, boxBorder)

        val reportTitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#1A237E")
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("STABILITY ANALYSIS SUMMARY / خلاصه ارزیابی پایداری توده سنگ", 30f, 461f, reportTitlePaint)

        val pCount = joints.count { isJointPlanarSlidingHazard(it.dip.toDouble(), it.dipDirection.toDouble(), slopeDip, slopeDipDir, frictionAngle, lateralLimit) }
        val tCount = joints.count { isJointTopplingHazard(it.dip.toDouble(), it.dipDirection.toDouble(), slopeDip, slopeDipDir, frictionAngle, lateralLimit) }
        val wCount = wedgeIntersections.count { it.isHazard }

        val bodyEn = "Input structural features: ${joints.size}. Found $pCount planar sliding, $tCount flexural toppling, $wCount wedge sliding risks."
        val bodyFa = "درزه‌های ورودی: ${joints.size}. تعداد $pCount لغزش صفحه‌ای، $tCount واژگونی خمشی و $wCount ریزش گوه سنگی شناسایی شد."

        val reportBodyPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#37474F")
            textSize = 8.2f
            isAntiAlias = true
        }
        canvas.drawText(bodyEn, 30f, 478f, reportBodyPaint)
        canvas.drawText(bodyFa, 30f, 492f, reportBodyPaint)

        val safetyRec = if (pCount > 0 || tCount > 0 || wCount > 0) {
            "Engineering Action: Unstable kinematic modes detected! tensioned rock-bolts & mesh are recommended."
        } else {
            "Engineering Action: Structure is kinematically stable. Routine geologic monitoring is sufficient."
        }
        val safetyRecFa = if (pCount > 0 || tCount > 0 || wCount > 0) {
            "اقدام مهندسی: ناپایداری ساختاری شناسایی شد! مهاربندی با پین‌های سنگی فعال و توری تکیه‌گاهی توصیه می‌گردد."
        } else {
            "اقدام مهندسی: توده سنگ در این محدوده پایدار ارزیابی شد. مانیتورینگ منظم دوره‌ای ترانشه کافی است."
        }

        val recPaint = android.graphics.Paint().apply {
            color = if (pCount > 0 || tCount > 0 || wCount > 0) android.graphics.Color.parseColor("#C62828") else android.graphics.Color.parseColor("#2E7D32")
            textSize = 8.2f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(safetyRec, 30f, 505f, recPaint)
        canvas.drawText(safetyRecFa, 30f, 515f, recPaint)

        // 6. DETAILED STRUCTURES DATA TABLE
        canvas.drawRect(20f, 532f, 575f, 549f, headerPaint) // Recolor with Indigo header
        
        val thPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 8.2f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("Discontinuity Name (نام ناپیوستگی)", 25f, 544f, thPaint)
        canvas.drawText("Feature Type (نوع)", 190f, 544f, thPaint)
        canvas.drawText("Dip/DipDir (جهت/شیب)", 300f, 544f, thPaint)
        canvas.drawText("Kinematic Safety Verdict (وضعیت)", 415f, 544f, thPaint)

        var currentY = 565f
        val rHeight = 15.5f

        // Print up to 14 joints to fit single A4 page
        joints.take(14).forEach { j ->
            canvas.drawLine(20f, currentY + 3f, 575f, currentY + 3f, dividerPaint)

            canvas.drawText(j.name.take(30), 25f, currentY, reportBodyPaint)
            canvas.drawText(j.type.label, 190f, currentY, reportBodyPaint)

            val coordsStr = if (j.type == StereonetFeatureType.LINE) {
                "Plunge: ${j.dip}° / Trend: ${j.dipDirection}°"
            } else {
                "Dip: ${j.dip}° / DipDir: ${j.dipDirection}°"
            }
            canvas.drawText(coordsStr, 300f, currentY, reportBodyPaint)

            val vPaint = android.graphics.Paint().apply {
                textSize = 7.8f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val isPSafe = !isJointPlanarSlidingHazard(j.dip.toDouble(), j.dipDirection.toDouble(), slopeDip, slopeDipDir, frictionAngle, lateralLimit)
            val isTSafe = !isJointTopplingHazard(j.dip.toDouble(), j.dipDirection.toDouble(), slopeDip, slopeDipDir, frictionAngle, lateralLimit)

            if (j.type == StereonetFeatureType.LINE) {
                vPaint.color = android.graphics.Color.parseColor("#7B1FA2")
                canvas.drawText("Lineation / خط‌واره", 415f, currentY, vPaint)
            } else if (!isPSafe) {
                vPaint.color = android.graphics.Color.RED
                canvas.drawText("Planar Sliding Hazard (لغزش صفحه‌ای)", 415f, currentY, vPaint)
            } else if (!isTSafe) {
                vPaint.color = android.graphics.Color.parseColor("#E65100")
                canvas.drawText("Toppling Hazard (واژگونی پله)", 415f, currentY, vPaint)
            } else {
                vPaint.color = android.graphics.Color.parseColor("#2E7D32")
                canvas.drawText("Stable (پایدار ساختاری)", 415f, currentY, vPaint)
            }

            currentY += rHeight
        }

        // Draw Table Outer Border
        canvas.drawRect(20f, 532f, 575f, Math.min(currentY - rHeight + 3f, 810f), boxBorder)

        // 7. FOOTER BAR
        val footerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 7f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("Page 1 of 1 | Geology Stereonet Stability Report | Analyst: farzadfathollazadeh@gmail.com", 297.5f, 822f, footerPaint)

        pdfDocument.finishPage(page)

        // 8. WRITE TO DEVICE MEDIASTORE / DOWNLOADS (API 29+) OR EXTERNAL CACHE
        val fileName = "Stereonet_Geotechnical_Report_${System.currentTimeMillis()}.pdf"
        val contentResolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            val outputStream = contentResolver.openOutputStream(uri)
            if (outputStream != null) {
                pdfDocument.writeTo(outputStream)
                outputStream.close()
                pdfDocument.close()

                Toast.makeText(context, if (isFarsi) "گزارش PDF با موفقیت در دانلودها ذخیره شد" else "Geotechnical Report saved to Downloads!", Toast.LENGTH_LONG).show()

                // Prompt user to immediately share/view report
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Stereonet Geotechnical Report - $projectName")
                    putExtra(android.content.Intent.EXTRA_TEXT, "Attached is the newly generated bilingual stereonet kinematic analysis PDF report.")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, if (isFarsi) "ارسال گزارش پایداری استریونت" else "Share Geotechnical PDF"))
            } else {
                pdfDocument.close()
                Toast.makeText(context, "Failed to open PDF stream!", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Cache fallback
            val cacheFile = java.io.File(context.cacheDir, fileName)
            val outputStream = java.io.FileOutputStream(cacheFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            val authority = "${context.packageName}.provider"
            val fileUri = androidx.core.content.FileProvider.getUriForFile(context, authority, cacheFile)

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Stereonet Geotechnical Report")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, if (isFarsi) "ارسال گزارش" else "Share PDF Report"))
        }
    } catch (e: Exception) {
        pdfDocument.close()
        e.printStackTrace()
        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

