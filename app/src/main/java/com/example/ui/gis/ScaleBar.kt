package com.example.ui.gis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScaleBar(
    zoomLevel: Int,
    modifier: Modifier = Modifier,
    isOutdoorMode: Boolean = false
) {
    val textCol = if (isOutdoorMode) Color.Black else Color.White
    val accentColor = if (isOutdoorMode) Color(0xFF007A78) else Color(0xFF00FFE0)
    val panelBg = if (isOutdoorMode) Color.White.copy(alpha = 0.95f) else Color(0x99000000)

    val scaleLabel = remember(zoomLevel) {
        when (zoomLevel) {
            1 -> "500 m"
            2 -> "1 km"
            3 -> "2.5 km"
            4 -> "5 km"
            5 -> "10 km"
            6 -> "25 km"
            7 -> "50 km"
            8 -> "100 km"
            else -> "5 km"
        }
    }

    Box(
        modifier = modifier
            .width(80.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(panelBg)
            .border(0.5.dp, textCol.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = scaleLabel,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                color = textCol,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            ) {
                val h = size.height
                val w = size.width

                // Draw bracket [ === ]
                drawLine(
                    color = accentColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, h),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = accentColor,
                    start = Offset(0f, h / 2),
                    end = Offset(w, h / 2),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = accentColor,
                    start = Offset(w, 0f),
                    end = Offset(w, h),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}
