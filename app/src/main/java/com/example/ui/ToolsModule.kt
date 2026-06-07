package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.GeologyViewModel
import java.text.DecimalFormat
import java.util.Locale

@Composable
fun ToolsContent(
    viewModel: GeologyViewModel,
    modifier: Modifier = Modifier
) {
    var selectedToolTab by remember { mutableStateOf(0) } // 0 = Mineral ID, 1 = Lithology, 2 = Converter, 3 = Developer Profile

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Tab Title
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Column {
                    Text(
                        text = "Geotech Field Tools",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Handy micro-tools to accelerate field estimations and diagnostic classification",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Tab selection row
        ScrollableTabRow(
            selectedTabIndex = selectedToolTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            Tab(
                selected = selectedToolTab == 0,
                onClick = { selectedToolTab = 0 },
                text = { Text("Mineralogy", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 1,
                onClick = { selectedToolTab = 1 },
                text = { Text("Lithology", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 2,
                onClick = { selectedToolTab = 2 },
                text = { Text("Unit Converter", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedToolTab == 3,
                onClick = { selectedToolTab = 3 },
                text = { Text("Developer", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

        // Content Area
        Box(modifier = Modifier.weight(1f)) {
            when (selectedToolTab) {
                0 -> MineralIdHelper()
                1 -> LithologyHelper()
                2 -> GeotechConverter()
                3 -> DeveloperProfilePage()
            }
        }
    }
}

// ---------------------------------------------
// 1. MINERAL IDENTIFICATION HELPER
// ---------------------------------------------
data class GeotechMineral(
    val nameFa: String,
    val nameEn: String,
    val hardnessRange: ClosedFloatingPointRange<Double>,
    val color: String,
    val streak: String,
    val luster: String,
    val cleavage: String,
    val density: Double,
    val acidReaction: Boolean = false,
    val magnetic: Boolean = false,
    val specialFeat: String = "",
    val enFeat: String = ""
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MineralIdHelper() {
    val minerals = remember {
        listOf(
            GeotechMineral("Quartz", "Quartz", 7.0..7.0, "Colorless, white, gray, purple", "White", "Vitreous", "None (conchoidal fracture)", 2.65, specialFeat = "High hardness, does not react with acid, infusible", enFeat = "High hardness, no cleavage, conchoidal fracture"),
            GeotechMineral("Calcite", "Calcite", 3.0..3.0, "Colorless, white, yellow, orange", "White", "Vitreous to resinous", "Perfect rhombohedral in 3 directions", 2.71, acidReaction = true, specialFeat = "Strong effervescence with cold dilute HCl, perfect rhombohedral rifts", enFeat = "Strong effervescence with cold dilute HCl"),
            GeotechMineral("Dolomite", "Dolomite", 3.5..4.0, "Pink, white, gray", "White", "Vitreous to pearly", "Rhombohedral in 3 directions", 2.85, acidReaction = true, specialFeat = "Effervesces with micro HCl only when powdered or hot", enFeat = "Effervesces with HCl only when powdered/warm"),
            GeotechMineral("Pyrite", "Pyrite", 6.0..6.5, "Pale brass yellow", "Greenish-black to brownish-black", "Strong Metallic", "None", 5.01, specialFeat = "Fool's gold, dark streak, cubic crystals, scratches copper/glass", enFeat = "Fool's gold, dark green-black streak, cubic crystals"),
            GeotechMineral("Orthoclase (K-Feldspar)", "Orthoclase (K-Feldspar)", 6.0..6.0, "Pink, cream, flesh red, white", "White", "Vitreous", "Two directions at 90 degrees", 2.56, specialFeat = "Often salmon-pink, classic orthogonal cleavage planes", enFeat = "Two cleavage planes at 90 deg, classic pink hue"),
            GeotechMineral("Plagioclase", "Plagioclase", 6.0..6.5, "White, gray, light green", "White", "Vitreous", "Two directions at 86 degrees", 2.68, specialFeat = "Shows parallel lamellar twin striations on cleavage surfaces", enFeat = "Twinning striations on cleavage surfaces"),
            GeotechMineral("Gypsum", "Gypsum", 2.0..2.0, "Colorless, white, gray, yellow", "White", "Vitreous, pearly to silky", "Perfect cleavage in 1 direction (peels)", 2.31, specialFeat = "Very soft, easily scratched by fingernail, flexible but inelastic", enFeat = "Scratched easily by fingernail, soft rock forming"),
            GeotechMineral("Talc", "Talc", 1.0..1.0, "Green, white, pale gray", "White", "Greasy to pearly", "Perfect cleavage in 1 direction", 2.75, specialFeat = "Softest mineral on Mohs scale, highly soapy and greasy feel", enFeat = "Talcum base, extremely soft, greasy soap feel"),
            GeotechMineral("Hematite", "Hematite", 5.5..6.5, "Reddish-brown, dark metallic gray to silver", "Cherry red / reddish-brown", "Metallic to earthy", "None", 5.26, specialFeat = "Distinctive cherry-red streak regardless of specular silver color", enFeat = "Cherry red streak regardless of iron metallic outer color"),
            GeotechMineral("Magnetite", "Magnetite", 5.5..6.5, "Dark iron black", "Black", "Metallic to submetallic", "None", 5.18, magnetic = true, specialFeat = "Strongly magnetic, attracts compass needle, high specific gravity", enFeat = "Strongly magnetic, attracts metals, dark heavy iron"),
            GeotechMineral("Halite", "Halite", 2.5..2.5, "Colorless, white, blue, red", "White", "Vitreous", "Perfect cubic cleavage (3 directions at 90 deg)", 2.16, specialFeat = "Distinctive salty taste, highly soluble in water, perfect cubic breaks", enFeat = "Salty taste, highly soluble in water, cubic cleavage"),
            GeotechMineral("Biotite (Black Mica)", "Biotite (Black Mica)", 2.5..3.0, "Dark brown to shiny black", "White/gray", "Vitreous to pearly", "Perfect basal cleavage (flexible elastic sheets)", 3.02, specialFeat = "Dark flexible and elastic mica sheets", enFeat = "Dark flexible mineral sheets, dark mica"),
            GeotechMineral("Muscovite (White Mica)", "Muscovite (White Mica)", 2.0..2.5, "Colorless, silver, light green", "White", "Vitreous to pearly", "Perfect basal cleavage (flexible elastic sheets)", 2.82, specialFeat = "Clear/silver highly flexible elastic sheets, electrical insulator", enFeat = "Clear highly flexible elastic sheets, light mica"),
            GeotechMineral("Fluorite", "Fluorite", 4.0..4.0, "Purple, green, yellow, blue, colorless", "White", "Vitreous", "Perfect octahedral cleavage in 4 directions", 3.18, specialFeat = "Cubic crystals, octahedral 4-directional rifts, fluorescent under UV", enFeat = "Isotropic cubic crystals, octahedral cleavage 4 directions"),
            GeotechMineral("Galena", "Galena", 2.5..2.5, "Bright lead gray", "Lead gray", "Bright metallic", "Perfect cubic cleavage", 7.58, specialFeat = "Extremely heavy (G=7.6), charcoal-gray streak, cubic blocks", enFeat = "Very heavy metallic ore, lead sulfide, bright cubic cleavage")
        )
    }

    // Diagnostic states
    var searchHardness by remember { mutableStateOf("") }
    var filterLuster by remember { mutableStateOf("All Lusters") }
    var filterColor by remember { mutableStateOf("All Colors") }
    var filterAcid by remember { mutableStateOf(false) }
    var filterMagnet by remember { mutableStateOf(false) }

    val filteredList = remember(searchHardness, filterLuster, filterColor, filterAcid, filterMagnet) {
        val targetHardness = searchHardness.toDoubleOrNull()
        minerals.filter { min ->
            val matchesHardness = if (targetHardness != null) {
                targetHardness in min.hardnessRange || 
                (targetHardness >= min.hardnessRange.start - 0.5 && targetHardness <= min.hardnessRange.endInclusive + 0.5)
            } else true

            val matchesLuster = if (filterLuster != "All Lusters") {
                if (filterLuster == "Metallic Luster") {
                    min.luster.lowercase().contains("metallic")
                } else {
                    !min.luster.lowercase().contains("metallic") || min.luster.lowercase().contains("vitreous") || min.luster.lowercase().contains("pearly") || min.luster.lowercase().contains("greasy")
                }
            } else true

            val matchesColor = if (filterColor != "All Colors") {
                min.color.lowercase().contains(filterColor.lowercase()) || min.nameEn.lowercase().contains(filterColor.lowercase())
            } else true

            val matchesAcid = if (filterAcid) min.acidReaction else true
            val matchesMagnet = if (filterMagnet) min.magnetic else true

            matchesHardness && matchesLuster && matchesColor && matchesAcid && matchesMagnet
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Filter mineral diagnostic features for rapid identification:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchHardness,
                            onValueChange = { searchHardness = it },
                            label = { Text("Approx Hardness (e.g., 3, 6, 7)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )

                        // Luster selector
                        var expandedL by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f).align(Alignment.CenterVertically)) {
                            Button(
                                onClick = { expandedL = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(filterLuster, fontSize = 11.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = expandedL, onDismissRequest = { expandedL = false }) {
                                DropdownMenuItem(text = { Text("All Lusters") }, onClick = { filterLuster = "All Lusters"; expandedL = false })
                                DropdownMenuItem(text = { Text("Metallic Luster") }, onClick = { filterLuster = "Metallic Luster"; expandedL = false })
                                DropdownMenuItem(text = { Text("Non-Metallic Luster") }, onClick = { filterLuster = "Non-Metallic"; expandedL = false })
                            }
                        }
                    }

                    // Special reactive chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterAcid,
                            onClick = { filterAcid = !filterAcid },
                            label = { Text("Acid Reaction (HCl)", fontSize = 10.sp) },
                            leadingIcon = if (filterAcid) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                        )

                        FilterChip(
                            selected = filterMagnet,
                            onClick = { filterMagnet = !filterMagnet },
                            label = { Text("Magnetic Material", fontSize = 10.sp) },
                            leadingIcon = if (filterMagnet) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                        )

                        if (searchHardness.isNotEmpty() || filterLuster != "All Lusters" || filterColor != "All Colors" || filterAcid || filterMagnet) {
                            IconButton(
                                onClick = {
                                    searchHardness = ""
                                    filterLuster = "All Lusters"
                                    filterColor = "All Colors"
                                    filterAcid = false
                                    filterMagnet = false
                                },
                                modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset filters", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Matching Minerals Found (${filteredList.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Total DB: ${minerals.size} core rock-forming minerals",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No matching minerals found. Try clearing filters.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(filteredList) { mineral ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            Column {
                                Text(
                                    text = mineral.nameFa,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = mineral.nameEn,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }

                            // Hardness tag
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Mohs Hardness: ${mineral.hardnessRange.start.toInt()}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                        // Diagnostic list grid
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mineral Color: ${mineral.color}", fontSize = 11.sp)
                                Text("Streak Powder: ${mineral.streak}", fontSize = 11.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Luster: ${mineral.luster}", fontSize = 11.sp)
                                Text("Specific Gravity: ${mineral.density} g/cm³", fontSize = 11.sp)
                            }
                        }

                        Text(
                            text = "Cleavage/Fracture: ${mineral.cleavage}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Field Identifiers: ${mineral.specialFeat}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Geological Tip: ${mineral.enFeat}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------
// 2. LITHOLOGY IDENTIFICATION HELPER
// ---------------------------------------------
data class GeotechRock(
    val nameFa: String,
    val nameEn: String,
    val origin: String, // Igneous, Sedimentary, Metamorphic
    val texture: String,
    val typicalMinerals: String,
    val unitWeight: String, // kN/m³
    val typicalStrength: String, // MPa (UCS)
    val fieldTips: String,
    val description: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LithologyHelper() {
    val rocks = remember {
        listOf(
            GeotechRock("Granite", "Granite", "Intrusive Igneous", "Coarse-grained crystalline (Phaneritic)", "Quartz, Orthoclase (K-Feldspar), Plagioclase, Mica (Biotite)", "25 - 27 kN/m³", "100 - 250 MPa (Very Strong)", "Very high field strength, fully coarse crystalline texture, highly resistant to weathering", "The most abundant intrusive igneous rock in the continental crust. Used extensively in heavy geotechnical structures and dam foundations."),
            GeotechRock("Basalt", "Basalt", "Extrusive Igneous", "Fine-grained dark (Aphanitic) or vesicular", "Ca-plagioclase, Pyroxene, Olivine; lacks quartz", "28 - 31 kN/m³", "150 - 300 MPa (Extremely Strong)", "Extremely fine-grained black to dark gray texture, sometimes displays vesicular gas bubbles or column jointing", "The major constituent of oceanic crust. Extremely high mechanical strength but potential permeability from columnar joints needs analysis."),
            GeotechRock("Andesite", "Andesite", "Extrusive Igneous", "Fine-grained porphyritic (Porphyritic)", "Plagioclase, Hornblende, Pyroxene", "25 - 28 kN/m³", "80 - 180 MPa", "Medium to dark gray, contains prominent phenocryst crystals embedded in a fine-grained groundmass", "Formed at subduction zone volcanoes. Rated mechanical and cutting rock strength as medium to highly resistant."),
            GeotechRock("Sandstone", "Sandstone", "Clastic Sedimentary", "Clastic cemented (Clastic)", "Quartz grains, Feldspar, Calcite/Silica/Clay cements", "22 - 25 kN/m³", "30 - 120 MPa (Highly Variable)", "Gritty and sandpapery surface texture, sedimentary bedding layers are easily visible to the naked eye", "Common hydrocarbon reservoir and groundwater aquifer. Regular elastic brittle behavior is measured under shear tests."),
            GeotechRock("Limestone", "Limestone", "Chemical Sedimentary", "Microcrystalline dense, fossiliferous, or clastic", "Calcium carbonate (Calcite) matrix texture", "24 - 27 kN/m³", "40 - 140 MPa", "Strong effervescence with dilute HCl at room temperature, potential fossil frames or white calcite veins", "Highly prone to karstification, subterranean cavities, and heavy dissolution. Geotechnical loading requires extensive grout curtain testing."),
            GeotechRock("Shale", "Shale", "Clastic Sedimentary", "Very fine-grained laminated (Fissility)", "Clay minerals (Illite, Kaolinite), microscopic quartz", "20 - 24 kN/m³", "5 - 40 MPa (Very Soft / Friable)", "Highly laminated or wafery thin split sheets, smells damp/earthy when wet, easily degraded mechanics on failure plane", "The most common landslide-inducing sedimentary wall rock. Swelling potential in smectite-bearing shales is geotechnically critical."),
            GeotechRock("Conglomerate", "Conglomerate", "Clastic Sedimentary", "Coarse-grained rounded gravel", "Rounded pebbles (granite, limestone, quartz) in a sandy matrix", "23 - 26 kN/m³", "20 - 80 MPa", "Rounded pebbles/cobbles larger than 2 mm tightly locked in sandy cement matrix", "Highly heterogeneous mechanical behavior depending heavily on bounding cement strength. Exploration drilling is arduous due to contrasting pebble/matrix hardness."),
            GeotechRock("Schist", "Schist", "Foliated Metamorphic", "Foliated sheets of dense light-dark mica (Schistosity)", "Biotite, Muscovite, Feldspar, Quartz, sometimes Garnet or Talc", "26 - 28 kN/m³", "30 - 100 MPa", "Brilliant, highly shiny parallel alignments that catch light from mica flakes, clean splitting along foliation surfaces", "Highly anisotropic rock. Compressive strength parallel to foliation planes falls to its absolute minimum."),
            GeotechRock("Quartzite", "Quartzite", "Non-Foliated Metamorphic", "Non-foliated dense crystalline mosaic", "Recrystallized quartz from sandstone", "26 - 27 kN/m³", "150 - 300 MPa (Extremely Hard)", "Extreme hardness (steel blade cannot scratch it but is ground down), homogeneous glassy texture with no distinct grains", "The most resistant rock against environmental weathering. Hard rock TBM drilling results in severe disc cutter wear in quartzite layers."),
            GeotechRock("Marble", "Marble", "Non-Foliated Metamorphic", "Non-foliated crystalline mosaic (sugar-like)", "Recrystallized calcite from limestone base", "26 - 28 kN/m³", "50 - 120 MPa", "Reacts with cold dilute HCl, crystalline sugar-like mosaic texture, scratches easily with copper coin", "Highly decorative stone with mechanical characteristics similar to dense limestone. Vulnerable to dissolution under acidic rainwater.")
        )
    }

    var selectedOriginFilter by remember { mutableStateOf("All Origins") }
    val filteredRocks = remember(selectedOriginFilter) {
        if (selectedOriginFilter == "All Origins") rocks else {
            rocks.filter { it.origin.contains(selectedOriginFilter) }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Multi-choice Origin filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Geological Origin:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 4.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("All", "Igneous", "Sedimentary", "Metamorphic").forEach { orig ->
                    val isSelected = (orig == "All" && selectedOriginFilter == "All Origins") || 
                                     (orig != "All" && selectedOriginFilter == orig)
                    ElevatedAssistChip(
                        onClick = {
                            selectedOriginFilter = if (orig == "All") "All Origins" else orig
                        },
                        label = { Text(orig, fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredRocks) { rock ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(rock.nameFa, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when {
                                                    rock.origin.contains("Igneous") -> Color(0xFFC62828).copy(alpha = 0.15f)
                                                    rock.origin.contains("Sedimentary") -> Color(0xFFFBC02D).copy(alpha = 0.15f)
                                                    else -> Color(0xFF00ACC1).copy(alpha = 0.15f)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (rock.origin.contains("Igneous")) "Igneous" else if (rock.origin.contains("Sedimentary")) "Sedimentary" else "Metamorphic",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                rock.origin.contains("Igneous") -> Color(0xFFC62828)
                                                rock.origin.contains("Sedimentary") -> Color(0xFFE65100)
                                                else -> Color(0xFF00ACC1)
                                            }
                                        )
                                    }
                                }
                                Text(rock.nameEn, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            }

                            // UCS Strength tag
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Strength (UCS)", fontSize = 9.sp, color = Color.Gray)
                                Text(rock.typicalStrength, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                        // Engineering facts grid
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("Texture: ${rock.texture}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Minerals: ${rock.typicalMinerals}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(modifier = Modifier.weight(0.8f), horizontalAlignment = Alignment.End) {
                                Text("Unit Weight", fontSize = 9.sp, color = Color.Gray)
                                Text(rock.unitWeight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = rock.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )
                                Text(
                                    text = "Field Identification: ${rock.fieldTips}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)).padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------
// 3. GEOTECHNICAL UNIT CONVERTER
// ---------------------------------------------
@Composable
fun GeotechConverter() {
    var converterType by remember { mutableStateOf(0) } // 0 = Stress, 1 = Density, 2 = Length, 3 = Angle
    var inputValue by remember { mutableStateOf("1.0") }

    val decimalFormat = remember { DecimalFormat("#,##0.000000") }
    val normVal = inputValue.toDoubleOrNull() ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Stress / Press", "Density / Unit wt", "Length / Depth", "Dip Angles").forEachIndexed { index, mode ->
                ElevatedFilterChip(
                    selected = converterType == index,
                    onClick = { converterType = index },
                    label = { Text(mode, fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Input TextField
        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = { Text("Input Value to Convert") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (inputValue.isNotEmpty()) {
                    IconButton(onClick = { inputValue = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Simultaneous Unit Conversions:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                when (converterType) {
                    0 -> { // STRESS & PRESSURE (Base: MPa)
                        val mpa = normVal
                        ConverterResultRow("Megapascals (MPa)", mpa, "RMR rock rating base index & rock mass strength")
                        ConverterResultRow("Gigapascals (GPa)", mpa * 0.001, "Elastic Young's Modulus scale")
                        ConverterResultRow("Kilopascals (kPa)", mpa * 1000.0, "Soil bearing capacity standard")
                        ConverterResultRow("Pounds per sq inch (psi)", mpa * 145.0377, "Common Imperial/British unit")
                        ConverterResultRow("Kilogram-force per sq cm (kgf/cm²)", mpa * 10.19716, "Classical geotechnical index pressure")
                        ConverterResultRow("Bar (bar)", mpa * 10.0, "Hydromechanical & groundwater pressure")
                    }
                    1 -> { // DENSITY & UNIT WEIGHT (Base: g/cm³)
                        val gcm3 = normVal
                        ConverterResultRow("Grams per cubic cm (g/cm³)", gcm3, "Specific mineral grain density")
                        ConverterResultRow("Kilograms per cubic meter (kg/m³)", gcm3 * 1000.0, "Bulk engineering dry/wet density")
                        ConverterResultRow("Pounds per cubic foot (lb/ft³)", gcm3 * 62.42796, "Imperial dry unit density weight")
                        ConverterResultRow("Kilonewtons per cubic meter (kN/m³)", gcm3 * 9.80665, "Geotechnical in-situ unit weight (γ)")
                    }
                    2 -> { // LENGTH (Base: meter)
                        val m = normVal
                        ConverterResultRow("Meters (m)", m, "Borehole logging length or fault displacement scale")
                        ConverterResultRow("Centimeters (cm)", m * 100.0, "Joint aperture or core block spacing")
                        ConverterResultRow("Millimeters (mm)", m * 1000.0, "Clay seam thickness or micro-fault rifts")
                        ConverterResultRow("Feet (ft)", m * 3.28084, "Imperial borehole or excavation depth")
                        ConverterResultRow("Inches (in)", m * 39.37008, "Standard core barrel diameter sizing (e.g., HQ, NQ, BQ)")
                    }
                    3 -> { // ANGLE (Base: Degrees)
                        val deg = normVal
                        val rad = deg * (Math.PI / 180.0)
                        val grad = deg * (200.0 / 180.0)
                        ConverterResultRow("Degree (°)", deg, "Standard geological dip & strike angle system")
                        ConverterResultRow("Radian (rad)", rad, "Used for stability formulas & numerical analysis")
                        ConverterResultRow("Gradian (grad)", grad, "Used in mine surveying engineering layouts")
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            val strike = (deg + 90) % 360
                            Text(
                                text = "Field Guide: If the angle of the dip is ${normVal.toInt()}°, the maximum gradient down direction will be orthogonal (perpendicular) to the strike line of the bed (at ${strike.toInt()}° or ${((strike+180)%360).toInt()}°).",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConverterResultRow(
    unitName: String,
    value: Double,
    description: String
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val formattedVal = remember(value) {
        if (value == 0.0) "0"
        else if (value < 0.0001) String.format(Locale.US, "%.8f", value)
        else String.format(Locale.US, "%.4f", value)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clipboard.setText(AnnotatedString(formattedVal))
                Toast.makeText(context, "Copied $formattedVal to clipboard", Toast.LENGTH_SHORT).show()
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(unitName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(description, fontSize = 9.sp, color = Color.Gray)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = formattedVal,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.End
            )
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
        }
    }
}


// ---------------------------------------------
// 4. DEVELOPER PROFILE & SOCIAL LINKS
// ---------------------------------------------
@Composable
fun DeveloperProfilePage() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val links = remember {
        listOf(
            Triple("Geological Field Log Telegram Channel", "https://t.me/geofieldpro", Icons.Default.Send),
            Triple("GitHub Developer Profile", "https://github.com/farzad-f", Icons.Default.Code),
            Triple("LinkedIn Professional Profile", "https://www.linkedin.com/in/farzad-fathollahzadeh-b3a6a1165", Icons.Default.Hub)
        )
    }

    fun openLink(url: String) {
        val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open this link on your device.", Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Profile Card Header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(36.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
                    }

                    Text(
                        text = "Farzad Fathollahzadeh",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Founder & Lead Developer of Integrated Geological Field Systems",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "This application is a comprehensive mobile geological exploration helper. All databases—such as structural dip & strike mapping, borehole logging sheets, RMR engineering values, and Barton Q calculators—work hundred percent offline in remote field areas.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Justify,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Social Connect & Useful Links:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                links.forEach { (title, url, icon) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openLink(url) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = url,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(url))
                                        Toast.makeText(context, "Copied link to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp).align(Alignment.CenterVertically))
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "GeoField Pro version 1.4.0 • Built with Jetpack Compose & SQLite",
                fontSize = 10.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
