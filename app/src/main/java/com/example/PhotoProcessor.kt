package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoProcessor {

    data class ImageMetadata(
        val latitude: Double? = null,
        val longitude: Double? = null,
        val elevation: Double? = null,
        val timestamp: Long? = null
    )

    data class OverlaySettings(
        val isOverlayEnabled: Boolean = true,
        val overlayPosition: String = "BOTTOM_LEFT"
    )

    fun getCompassDirection(azimuth: Int): String {
        val degrees = (azimuth % 360 + 360) % 360
        return when (degrees) {
            in 0..22 -> "N"
            in 23..67 -> "NE"
            in 68..112 -> "E"
            in 113..157 -> "SE"
            in 158..202 -> "S"
            in 203..247 -> "SW"
            in 248..292 -> "W"
            in 293..337 -> "NW"
            else -> "N"
        }
    }

    fun getCompassDirection(azimuth: Double): String {
        return getCompassDirection(azimuth.toInt())
    }

    fun readImageExif(context: Context, uri: Uri): ImageMetadata {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                val latLong = FloatArray(2)
                val hasLatLong = exifInterface.getLatLong(latLong)
                val lat = if (hasLatLong) latLong[0].toDouble() else null
                val lon = if (hasLatLong) latLong[1].toDouble() else null

                val altAttr = exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)
                val altRefAttr = exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF)
                var elevation: Double? = null
                if (altAttr != null) {
                    val altRatio = altAttr.split("/")
                    if (altRatio.size == 2) {
                        val value = altRatio[0].toDouble() / altRatio[1].toDouble()
                        elevation = if (altRefAttr == "1") -value else value
                    } else {
                        elevation = altAttr.toDoubleOrNull()
                    }
                }

                val dateAttr = exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                var timestamp: Long? = null
                if (dateAttr != null) {
                    try {
                        val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                        timestamp = sdf.parse(dateAttr)?.time
                    } catch (e: Exception) {
                        // fallback
                    }
                }
                return ImageMetadata(
                    latitude = lat,
                    longitude = lon,
                    elevation = elevation,
                    timestamp = timestamp
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ImageMetadata()
    }

    fun saveAnnotatedImage(
        context: Context,
        baseBitmap: Bitmap,
        stationCode: String,
        lat: Double,
        lon: Double,
        elevation: Double,
        utmZone: String,
        utmEasting: Double,
        utmNorthing: Double,
        azimuth: Int,
        timestamp: Long,
        settings: OverlaySettings
    ): String? {
        try {
            val width = baseBitmap.width
            val height = baseBitmap.height
            val annotatedBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(annotatedBitmap)

            if (settings.isOverlayEnabled) {
                // Scaled sizes based on photo resolution for crisp display across all device sensors
                val scaleFactor = (width.coerceAtLeast(height) / 1000f).coerceIn(1f, 4f)
                val fontSize = 14f * scaleFactor
                val padding = 12f * scaleFactor
                val lineSpacing = 6f * scaleFactor

                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = fontSize
                    style = Paint.Style.FILL
                    setShadowLayer(2f, 1f, 1f, Color.BLACK)
                }

                val labelDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
                val notesLines = listOf(
                    "STATION: $stationCode",
                    "COORD: ${String.format(Locale.US, "%.6f", lat)}, ${String.format(Locale.US, "%.6f", lon)}",
                    "UTM: WGS84 Z$utmZone, E ${String.format(Locale.US, "%.1f", utmEasting)}, N ${String.format(Locale.US, "%.1f", utmNorthing)}",
                    "ELEV: ${String.format(Locale.US, "%.1f", elevation)}m",
                    "BEARING: $azimuth° (${getCompassDirection(azimuth)})",
                    "CAPTURED: $labelDate"
                )

                // Measure total text boundaries to draw a sleek semi-transparent background plate
                var maxWidth = 0f
                var totalHeight = 0f
                val textBounds = Rect()
                for (line in notesLines) {
                    paint.getTextBounds(line, 0, line.length, textBounds)
                    if (textBounds.width() > maxWidth) {
                        maxWidth = textBounds.width().toFloat()
                    }
                    totalHeight += fontSize + lineSpacing
                }
                totalHeight -= lineSpacing // remove trailing spacing

                // Calculate overlay plate coordinates based on selection settings
                val plateW = maxWidth + padding * 2
                val plateH = totalHeight + padding * 2

                val plateLeft: Float
                val plateTop: Float

                when (settings.overlayPosition) {
                    "TOP_LEFT" -> {
                        plateLeft = padding
                        plateTop = padding
                    }
                    "TOP_RIGHT" -> {
                        plateLeft = width - plateW - padding
                        plateTop = padding
                    }
                    "BOTTOM_RIGHT" -> {
                        plateLeft = width - plateW - padding
                        plateTop = height - plateH - padding
                    }
                    else -> { // "BOTTOM_LEFT" as generic industry standard
                        plateLeft = padding
                        plateTop = height - plateH - padding
                    }
                }

                val plateRight = plateLeft + plateW
                val plateBottom = plateTop + plateH

                // Draw background plate
                val bgPaint = Paint().apply {
                    color = Color.parseColor("#991A2421") // transluscent brand slate for ultimate sunlight contrast
                    style = Paint.Style.FILL
                }
                canvas.drawRect(plateLeft, plateTop, plateRight, plateBottom, bgPaint)

                // Draw solid aesthetic color side stripe representing professional active instrument
                val stripePaint = Paint().apply {
                    color = Color.parseColor("#FF10B981") // solid green stripe
                    style = Paint.Style.FILL
                }
                canvas.drawRect(plateLeft, plateTop, plateLeft + 4f * scaleFactor, plateBottom, stripePaint)

                // Draw overlay texts line-by-line
                var drawY = plateTop + padding + fontSize - 2f
                for (line in notesLines) {
                    canvas.drawText(line, plateLeft + padding + (4f * scaleFactor), drawY, paint)
                    drawY += fontSize + lineSpacing
                }
            }

            // Save annotated image to pictures directory or app cache
            val pictureDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.cacheDir
            if (!pictureDir.exists()) {
                pictureDir.mkdirs()
            }
            val filename = "GEOP_IMG_${System.currentTimeMillis()}.jpg"
            val file = File(pictureDir, filename)

            FileOutputStream(file).use { outStream ->
                annotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream)
            }
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
