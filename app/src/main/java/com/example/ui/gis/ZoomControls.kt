package com.example.ui.gis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZoomControls(
    zoomLevel: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
    isOutdoorMode: Boolean = false
) {
    val textCol = if (isOutdoorMode) Color.Black else Color.White
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF00FFE0)
    val panelBg = if (isOutdoorMode) Color.White.copy(alpha = 0.95f) else Color(0xDD000000)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(panelBg)
            .border(0.5.dp, textCol.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .width(50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Zoom In button (Minimum 48x48dp interactive area)
        IconButton(
            onClick = onZoomIn,
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Zoom In",
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        // Zoom Level indicator
        Text(
            text = "z$zoomLevel",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textCol,
            fontFamily = FontFamily.Monospace
        )

        // Zoom Out button
        IconButton(
            onClick = onZoomOut,
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Zoom Out",
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
