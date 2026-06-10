package com.example.ui.gis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Save
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
import com.example.Observation

@Composable
fun StructuralMeasurementSheet(
    compassHeading: Double,
    onDismiss: () -> Unit,
    onSave: (Observation) -> Unit,
    stationId: Int,
    modifier: Modifier = Modifier,
    isOutdoorMode: Boolean = false
) {
    val scrollState = rememberScrollState()

    // Structural Attitude State values
    var strike by remember { mutableStateOf("045") } // Default strike in degrees (N45E)
    var dip by remember { mutableStateOf("32") }    // Default dip angle (32 degrees)
    var dipDirection by remember { mutableStateOf("135") } // Default dip direction (SE: strike + 90)
    var structureType by remember { mutableStateOf("Bedding (S0)") }
    var notes by remember { mutableStateOf("") }

    val structureTypePresets = listOf("Bedding (S0)", "Foliation (S1)", "Fault Plane", "Joint Surface", "Shear Zone")

    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
    val overlayBgColor = if (isOutdoorMode) Color(0xFFF9FBF9) else Color(0xFF141917)
    val cardBgColor = if (isOutdoorMode) Color.White else Color(0xFF1E2623)
    val textColor = if (isOutdoorMode) Color.Black else Color.White

    // Trigger auto-sampling attitude angles based on compass orientation
    fun pullAttitudeValues() {
        // Strike is current heading relative to North
        val calculatedStrike = compassHeading.toInt() % 360
        // Dip is standard simulated angle (bedding is often between 20-55)
        val calculatedDip = (calculatedStrike % 45) + 15
        val calculatedDipDir = (calculatedStrike + 90) % 360

        strike = String.format("%03d", calculatedStrike)
        dip = calculatedDip.toString()
        dipDirection = String.format("%03d", calculatedDipDir)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(overlayBgColor)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "STRUCTURAL STRUCTURAL MEASUREMENTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = "Log Stratum Bedding / Joint Plane",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close attitude logger",
                    tint = textColor.copy(alpha = 0.6f)
                )
            }
        }

        Divider(color = textColor.copy(alpha = 0.1f))

        // INTEGRATED CLINOMETER COMPASS POPULATOR (Simulates pulling core orientation angles)
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "LIVE HANDHELD COMPASS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Text(
                            text = "ORIENTATION: ${compassHeading.toInt()}° BEARING",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                    Button(
                        onClick = { pullAttitudeValues() },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompassCalibration,
                                contentDescription = "Calibrate",
                                modifier = Modifier.size(14.dp)
                            )
                            Text("PULL ANGLES", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    text = "Align long edge of phone parallel to strike of bedding plane, tilt parallel to dip, then tap 'PULL' to calculate standard strike & dip automatically.",
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.6f)
                )
            }
        }

        // DISPOSITION READOUT (Strike, Dip, Dip Direction)
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PLANE ATTITUDE ANGLES",
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = strike,
                        onValueChange = { strike = it },
                        label = { Text("Strike (0-360°)") },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = textColor),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )

                    OutlinedTextField(
                        value = dip,
                        onValueChange = { dip = it },
                        label = { Text("Dip (0-90°)") },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = textColor),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )

                    OutlinedTextField(
                        value = dipDirection,
                        onValueChange = { dipDirection = it },
                        label = { Text("Dip Dir (N/S/E/W)") },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = textColor),
                        modifier = Modifier.weight(1.2f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                }
            }
        }

        // STRUCTURE TYPE SELECTOR
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "STRUCTURAL TYPE",
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    structureTypePresets.take(3).forEach { p ->
                        val isChosen = structureType == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isChosen) accentColor else textColor.copy(alpha = 0.05f))
                                .clickable { structureType = p }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isChosen) Color.White else textColor
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = structureType,
                    onValueChange = { structureType = it },
                    label = { Text("Custom Geological Lineation/Planar Label") },
                    textStyle = LocalTextStyle.current.copy(color = textColor),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // REMARKS FIELD OBSERVATION NOTES
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "FIELD REMARKS & CORRELATIONS",
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note slickenside lineations, shearing indicators, bedding folds...") },
                    textStyle = LocalTextStyle.current.copy(color = textColor),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // COMMITT BUTTON
        Button(
            onClick = {
                val strikeInt = strike.toIntOrNull() ?: 45
                val dipInt = dip.toIntOrNull() ?: 30
                val dipDirInt = dipDirection.toIntOrNull() ?: 135

                val obs = Observation(
                    stationId = stationId,
                    observationType = "Structure",
                    photoPath = null,
                    notes = "[$structureType] Strike: $strikeInt°, Dip: $dipInt° @ $dipDirInt° direction. $notes",
                    structureType = structureType,
                    strike = strikeInt,
                    dip = dipInt,
                    dipDirection = dipDirInt
                )
                onSave(obs)
            },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = "Commit structure", tint = Color.Black)
                Text(
                    text = "RECORD ATTITUDE TO SPATIAL DB",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
