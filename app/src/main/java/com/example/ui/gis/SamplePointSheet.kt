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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Sample
import java.util.Locale

@Composable
fun SamplePointSheet(
    latitude: Double,
    longitude: Double,
    onDismiss: () -> Unit,
    onSave: (Sample) -> Unit,
    observationId: Int,
    modifier: Modifier = Modifier,
    isOutdoorMode: Boolean = false
) {
    val scrollState = rememberScrollState()

    // Core state forms conforming to specs
    var sampleIdInput by remember { mutableStateOf("SMP-C" + System.currentTimeMillis().toString().takeLast(4)) }
    var sampleType by remember { mutableStateOf("Hand Specimen") }
    var description by remember { mutableStateOf("") }

    val sampleTypes = listOf("Hand Specimen", "Core Run Segment", "Soil Grid Sample", "Stream Sediment", "Channel Chip")

    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF109D96)
    val overlayBgColor = if (isOutdoorMode) Color(0xFFF9FBF9) else Color(0xFF141917)
    val cardBgColor = if (isOutdoorMode) Color.White else Color(0xFF1E2623)
    val textColor = if (isOutdoorMode) Color.Black else Color.White

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(overlayBgColor)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SPECIMEN SAMPLE CAPTURE FORM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = "Log Mining Sample Point",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss modal",
                    tint = textColor.copy(alpha = 0.6f)
                )
            }
        }

        Divider(color = textColor.copy(alpha = 0.1f))

        // Position Telemetry Tracker Display
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CAPTURE LOCATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = "LAT: ${String.format(Locale.US, "%.5f", latitude)}° | LON: ${String.format(Locale.US, "%.5f", longitude)}°",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textColor
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "GPS LOCK",
                        fontSize = 9.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // SPECIMEN IDENTIFIER
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "METADATA DETAILS",
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = sampleIdInput,
                    onValueChange = { sampleIdInput = it },
                    label = { Text("Unique Sample Code / Tag ID") },
                    textStyle = LocalTextStyle.current.copy(color = textColor),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // SPECIMEN TYPE SELECTOR
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "SPECIMEN COLLECTION METHOD",
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                sampleTypes.forEach { type ->
                    val isSel = sampleType == type
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) accentColor.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { sampleType = type }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSel,
                            onClick = { sampleType = type },
                            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = type,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        }

        // DESCRIPTION & REMARKS
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "FIELD GEOLOGIST REMARKS",
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Lithology matrix, visible chalcopyrite, density, alteration, etc.") },
                    textStyle = LocalTextStyle.current.copy(color = textColor),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor),
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional camera snap simulator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(textColor.copy(alpha = 0.04f))
                        .clickable {
                            // capture mock camera photo
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = "Add image", tint = accentColor)
                        Text("Add Sample Specimen Closeup Photo", fontSize = 11.sp, color = textColor)
                    }
                }
            }
        }

        // SUBMIT CORE FORM BUTTON
        Button(
            onClick = {
                val sampleObj = Sample(
                    observationId = observationId,
                    sampleCodeId = sampleIdInput,
                    sampleType = sampleType,
                    description = description,
                    photoPath = null
                )
                onSave(sampleObj)
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
                Icon(imageVector = Icons.Default.Save, contentDescription = "Save sample button icon", tint = Color.Black)
                Text(
                    text = "COMMIT SAMPLE SPECIMEN RECORD",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
