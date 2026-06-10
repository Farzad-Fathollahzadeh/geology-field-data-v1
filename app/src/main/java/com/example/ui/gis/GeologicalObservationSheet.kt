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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Observation

@Composable
fun GeologicalObservationSheet(
    onDismiss: () -> Unit,
    onSave: (Observation) -> Unit,
    stationId: Int,
    modifier: Modifier = Modifier,
    isOutdoorMode: Boolean = false
) {
    val scrollState = rememberScrollState()
    
    // Core custom variables representing Observation fields
    var lithologyType by remember { mutableStateOf("Granite") }
    var texture by remember { mutableStateOf("Equigranular") }
    var colorVal by remember { mutableStateOf("Greyish Pink") }
    var alterationType by remember { mutableStateOf("Silicification") }
    var intensity by remember { mutableStateOf("Moderate") }
    var mineralizationStyle by remember { mutableStateOf("Disseminated Sulfides") }
    var notes by remember { mutableStateOf("") }
    
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
    val overlayBgColor = if (isOutdoorMode) Color(0xFFF9FBF9) else Color(0xFF141917)
    val cardColor = if (isOutdoorMode) Color.White else Color(0xFF1E2623)
    val textColor = if (isOutdoorMode) Color.Black else Color.White

    val lithologyPresets = listOf("Granite", "Basalt", "Sandstone", "Limestone", "Metashale", "Schist")
    val alterationPresets = listOf("None", "Silicification", "Argillic", "Propylitic", "Sericitic")
    val intensityPresets = listOf("Weak", "Moderate", "Strong", "Intense")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(overlayBgColor)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Title Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LOG OBSERVATION STATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = "Field Structure Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close form",
                    tint = textColor.copy(alpha = 0.6f)
                )
            }
        }

        Divider(color = textColor.copy(alpha = 0.1f))

        // LITHOLOGY SELECTOR
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LITHOLOGY TYPE",
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                
                // Horizontal preset chip row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    lithologyPresets.take(4).forEach { preset ->
                        val isSelected = lithologyType == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) accentColor else textColor.copy(alpha = 0.05f))
                                .clickable { lithologyType = preset }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else textColor
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = lithologyType,
                    onValueChange = { lithologyType = it },
                    label = { Text("Lithology Class Detail") },
                    textStyle = LocalTextStyle.current.copy(color = textColor),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.3f),
                        unfocusedLabelColor = textColor.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // TEXTURE & ALTERATION
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "LITHOLOGY TEXTURE & COLOR",
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = texture,
                        onValueChange = { texture = it },
                        label = { Text("Texture") },
                        textStyle = LocalTextStyle.current.copy(color = textColor),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                    OutlinedTextField(
                        value = colorVal,
                        onValueChange = { colorVal = it },
                        label = { Text("Color") },
                        textStyle = LocalTextStyle.current.copy(color = textColor),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                }
            }
        }

        // HYDROTHERMAL ALTERATION & GRADE
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ALTERATION & MINERALIZATION ASSEMBLY",
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = alterationType,
                    onValueChange = { alterationType = it },
                    label = { Text("Hydrothermal Alteration") },
                    textStyle = LocalTextStyle.current.copy(color = textColor),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                )

                Text("Alteration Intensity:", fontSize = 11.sp, color = textColor.copy(alpha = 0.6f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    intensityPresets.forEach { preset ->
                        val isSel = intensity == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, if (isSel) accentColor else Color.Transparent)
                                .background(if (isSel) accentColor.copy(alpha = 0.15f) else textColor.copy(alpha = 0.03f))
                                .clickable { intensity = preset }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(preset, fontSize = 9.5.sp, color = textColor)
                        }
                    }
                }

                OutlinedTextField(
                    value = mineralizationStyle,
                    onValueChange = { mineralizationStyle = it },
                    label = { Text("Mineralization Style Detail") },
                    textStyle = LocalTextStyle.current.copy(color = textColor),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                )
            }
        }

        // GENERAL REMARKS FIELD NOTES & PHOTO ATTACHMENT
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "FIELD WORKER MEMO & NOTES",
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Type structural trends, faults, weather, etc.") },
                    textStyle = LocalTextStyle.current.copy(color = textColor),
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(textColor.copy(alpha = 0.05f))
                        .clickable {
                            // simulated camera attachment action
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = "Add image", tint = accentColor)
                        Text("Attach Survey Core/Outcrop Photo", fontSize = 11.sp, color = textColor)
                    }
                }
            }
        }

        // SAVE BUTTON (At least 48dp size)
        Button(
            onClick = {
                val obs = Observation(
                    stationId = stationId,
                    observationType = "Lithology",
                    photoPath = null,
                    notes = notes,
                    lithologyType = lithologyType,
                    texture = texture,
                    color = colorVal,
                    alterationType = alterationType,
                    intensity = intensity,
                    minStyle = mineralizationStyle
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
                Icon(imageVector = Icons.Default.Save, contentDescription = "Save observation button icon", tint = Color.Black)
                Text(
                    text = "COMMIT OBSERVATION TO PLATFORM",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
