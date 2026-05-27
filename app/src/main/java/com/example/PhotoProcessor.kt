package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoProcessor {

    data class ImageMetadata(
        val latitude: Double?,
        val longitude: Double?,
        val timestamp: Long?,
        val elevation: Double?,
        val source: String
    )

    data class OverlaySettings(
        val isEnabled: Boolean = true,
        val position: String = "BOTTOM_LEFT" // BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT
    )

    // Parse EXIF attributes from file uri
    fun readImageExif(context: Context, uri: Uri): ImageMetadata {
        var latitude: Double? = null
        var longitude: Double? = null
        var timestamp: Long? = null
        var elevation: Double? = null
        var source = "None"

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val tempFile = File.createTempFile("exif_temp", ".jpg", context.cacheDir)
                FileOutputStream(tempFile).use { out ->
                    inputStream.copyTo(out)
                }

                val exif = ExifInterface(tempFile.absolutePath)
                val latLong = FloatArray(2)
                if (exif.getLatLong(latLong)) {
                    latitude = latLong[0].toDouble()
                    longitude = latLong[1].toDouble()
                    source = "EXIF Header"
                }

                elevation = exif.getAltitude(0.0)
                if (elevation == 0.0) {
                    elevation = null
                }

                val dateTimeStr = exif.getAttribute(ExifInterface.TAG_DATETIME)
                if (!dateTimeStr.isNullOrBlank()) {
                    val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    timestamp = sdf.parse(dateTimeStr)?.time
                }

                tempFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ImageMetadata(latitude, longitude, timestamp, elevation, source)
    }

    // Helper to format compassion direction code based on degrees
    fun getCompassDirection(degrees: Int): String {
        val d = (degrees % 360 + 360) % 360
        return when {
            d >= 338.5 || d < 22.5 -> "N"
            d >= 22.5 && d < 67.5 -> "NE"
            d >= 67.5 && d < 112.5 -> "E"
            d >= 112.5 && d < 157.5 -> "SE"
            d >= 157.5 && d < 202.5 -> "S"
            d >= 202.5 && d < 247.5 -> "SW"
            d >= 247.5 && d < 292.5 -> "W"
            else -> "NW"
        }
    }

    // Embed geological data overlay on image Bitmap and save it persistently
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
        settings: OverlaySettings,
        isCustomPhoto: Boolean = false
    ): String? {
        try {
            // Create mutable bitmap
            val processedBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(processedBitmap)

            if (settings.isEnabled) {
                val width = processedBitmap.width
                val height = processedBitmap.height

                // Draw watermarked overlay
                val padding = (width * 0.04f).coerceAtLeast(24f)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                
                // Overlay Card Bounds
                val cardWidth = (width * 0.55f).coerceAtLeast(420f).coerceAtMost(width.toFloat() - padding * 2)
                val cardHeight = (height * 0.22f).coerceAtLeast(180f).coerceAtMost(height.toFloat() - padding * 2)

                val x = when (settings.position) {
                    "BOTTOM_RIGHT", "TOP_RIGHT" -> width - cardWidth - padding
                    else -> padding
                }
                val y = when (settings.position) {
                    "BOTTOM_LEFT", "BOTTOM_RIGHT" -> height - cardHeight - padding
                    else -> padding
                }

                // Semi-transparent apple-inspired ultra dark background card
                paint.color = Color.parseColor("#E6111111")
                canvas.drawRect(x, y, x + cardWidth, y + cardHeight, paint)

                // High contrast copper highlight strip
                paint.color = Color.parseColor("#C96A2B")
                val stripWidth = 8f
                canvas.drawRect(x, y, x + stripWidth, y + cardHeight, paint)

                // Draw Text Lines
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }

                val lineCount = 5
                val lineGap = cardHeight / (lineCount + 1)
                
                // Line 1: Station Title
                textPaint.textSize = (lineGap * 0.45f).coerceAtLeast(14f)
                textPaint.color = Color.parseColor("#F5F3EE")
                canvas.drawText(
                    "GEOFIELD PRO | NODE: $stationCode", 
                    x + stripWidth + 18f, 
                    y + lineGap * 1.0f, 
                    textPaint
                )

                // Line 2: Lat/Lon Coords
                textPaint.textSize = (lineGap * 0.38f).coerceAtLeast(11f)
                textPaint.color = Color.parseColor("#C4C2BE")
                canvas.drawText(
                    "WGS84 lat/lon: ${String.format(Locale.US, "%.6f°, %.6f°", lat, lon)}",
                    x + stripWidth + 18f,
                    y + lineGap * 2.0f,
                    textPaint
                )

                // Line 3: UTM readings 
                textPaint.color = Color.parseColor("#0A84FF") // Tech Blue
                canvas.drawText(
                    "UTM z${utmZone}  ${String.format(Locale.US, "%.1f E | %.1f N", utmEasting, utmNorthing)}",
                    x + stripWidth + 18f,
                    y + lineGap * 3.0f,
                    textPaint
                )

                // Line 4: Azimuth Sight
                textPaint.color = Color.parseColor("#FF9F0A") // Compass Yellow
                canvas.drawText(
                    "SIGHT_AZIMUTH: ${azimuth}° ${getCompassDirection(azimuth)} (TRUE SURVEY)",
                    x + stripWidth + 18f,
                    y + lineGap * 4.0f,
                    textPaint
                )

                // Line 5: Elevation and UTC Time
                textPaint.color = Color.parseColor("#C4C2BE")
                val surveyDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
                canvas.drawText(
                    "ALT: ${String.format(Locale.US, "%.1f m", elevation)} a.s.l. | $surveyDate",
                    x + stripWidth + 18f,
                    y + lineGap * 5.0f,
                    textPaint
                )
            }

            // Save processed file
            val fileDir = File(context.filesDir, "field_photos")
            if (!fileDir.exists()) {
                fileDir.mkdirs()
            }
            val fileName = "GEOPHOTO_${System.currentTimeMillis()}.jpg"
            val file = File(fileDir, fileName)
            FileOutputStream(file).use { outStream ->
                processedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, outStream)
            }
            processedBitmap.recycle()
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Generate simulated high-fidelity geologic outcrop sketch visually for the emulator preview
    fun generateMockOutcropBitmap(context: Context, prefix: String): Bitmap {
        val width = 1200
        val height = 900
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw layered geological strata sketch using earth colors
        val colors = intArrayOf(
            Color.parseColor("#3C2E21"), // deep bed silts
            Color.parseColor("#4D3B2C"), // fault shale 
            Color.parseColor("#5C4B3A"), // copper soil host
            Color.parseColor("#6E5B4B"), // sand intrusion
            Color.parseColor("#806E5C")  // weathering crust
        )
        val shader = LinearGradient(0f, 0f, 0f, height.toFloat(), colors, null, Shader.TileMode.CLAMP)
        paint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Draw stratified bed layers lines
        paint.color = Color.parseColor("#221A12")
        paint.strokeWidth = 5f
        paint.style = Paint.Style.STROKE
        
        val pathsCount = 12
        for (i in 1..pathsCount) {
            val baseY = (height / (pathsCount + 1)) * i
            val pathY1 = baseY + (Math.sin(i.toDouble()) * 80).toFloat()
            val pathY2 = baseY + (Math.sin((i+1).toDouble()) * 80).toFloat()
            canvas.drawLine(0f, pathY1, width.toFloat(), pathY2, paint)
        }

        // Add some localized fault line or quartz vein intrusion
        paint.color = if (prefix == "FLT") Color.parseColor("#BF360C") else Color.parseColor("#FFF59D") // magma red or vein quartz yellow
        paint.strokeWidth = 14f
        canvas.drawLine(width * 0.25f, 0f, width * 0.75f, height.toFloat(), paint)

        // Text label to identify mock style
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        canvas.drawText("SURFACE OUTCROP WORKFLOW: ${prefix.uppercase(Locale.getDefault())} SECTION MODEL", 40f, height - 40f, paint)

        return bitmap
    }
}
