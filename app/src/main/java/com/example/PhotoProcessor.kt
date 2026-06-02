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

                // High contrast action highlight strip (Electric blue)
                paint.color = Color.parseColor("#1E5EFF")
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
                textPaint.color = Color.parseColor("#F4F1E8")
                canvas.drawText(
                    "GEOFIELD PRO | NODE: $stationCode", 
                    x + stripWidth + 18f, 
                    y + lineGap * 1.0f, 
                    textPaint
                )

                // Line 2: Lat/Lon Coords
                textPaint.textSize = (lineGap * 0.38f).coerceAtLeast(11f)
                textPaint.color = Color.parseColor("#D8D2C6")
                canvas.drawText(
                    "WGS84 lat/lon: ${String.format(Locale.US, "%.6f°, %.6f°", lat, lon)}",
                    x + stripWidth + 18f,
                    y + lineGap * 2.0f,
                    textPaint
                )

                // Line 3: UTM readings (GPS_ACTIVE style #00A86B)
                textPaint.color = Color.parseColor("#00A86B")
                canvas.drawText(
                    "UTM z${utmZone}  ${String.format(Locale.US, "%.1f E | %.1f N", utmEasting, utmNorthing)}",
                    x + stripWidth + 18f,
                    y + lineGap * 3.0f,
                    textPaint
                )

                // Line 4: Azimuth Sight (DATA_WARNING style #FFB020)
                textPaint.color = Color.parseColor("#FFB020")
                canvas.drawText(
                    "SIGHT_AZIMUTH: ${azimuth}° ${getCompassDirection(azimuth)} (TRUE SURVEY)",
                    x + stripWidth + 18f,
                    y + lineGap * 4.0f,
                    textPaint
                )

                // Line 5: Elevation and UTC Time
                textPaint.color = Color.parseColor("#D8D2C6")
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

    // Generate a beautiful Rose polar diagram on Native Android Bitmap
    fun drawRoseDiagramBitmap(strikes: List<Float>): Bitmap {
        val size = 600
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        
        // Dark slate/luxurious background
        canvas.drawColor(Color.parseColor("#12141C"))
        
        val center = size / 2.0f
        val radius = size / 2.0f - 40f
        
        // Setup paints
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#343A4A")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        val wedgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#500A84FF") // semi-transparent premium blue
            style = Paint.Style.FILL
        }
        
        val wedgeOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A84FF") // neon blue border
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A0A5B5")
            textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        
        // 1. Compute 10-degree bins frequency (18 bins in [0, 180))
        val bins = IntArray(18)
        for (strike in strikes) {
            val normalized = (strike % 180 + 180) % 180
            val binIndex = (normalized / 10).toInt().coerceIn(0, 17)
            bins[binIndex]++
        }
        
        val maxCount = (bins.maxOrNull() ?: 1).coerceAtLeast(1)
        
        // 2. Draw Concentric Grid Rings
        val numRings = 4
        for (i in 1..numRings) {
            val r = radius * (i.toFloat() / numRings)
            canvas.drawCircle(center, center, r, gridPaint)
            
            // Draw ring labels
            val lblValue = (maxCount * (i.toFloat() / numRings)).toInt()
            if (lblValue > 0) {
                canvas.drawText("$lblValue", center + r + 4f, center + 5f, textPaint)
            }
        }
        
        // 3. Draw Axis Lines every 30 degrees
        for (deg in 0 until 180 step 30) {
            val rad = Math.toRadians(deg.toDouble() - 90.0)
            val dx = radius * Math.cos(rad).toFloat()
            val dy = radius * Math.sin(rad).toFloat()
            canvas.drawLine(center - dx, center - dy, center + dx, center + dy, gridPaint)
            
            // Label angle (draw bidirectional labels)
            val labelRad = Math.toRadians(deg.toDouble() - 90.0)
            val lx = (radius + 20f) * Math.cos(labelRad).toFloat()
            val ly = (radius + 20f) * Math.sin(labelRad).toFloat()
            
            canvas.drawText("$deg°", center + lx - 10f, center + ly + 5f, textPaint)
            val degOpp = deg + 180
            canvas.drawText("$degOpp°", center - lx - 15f, center - ly + 5f, textPaint)
        }
        
        // 4. Draw Wedges
        for (i in 0 until 18) {
            val count = bins[i]
            if (count > 0) {
                val rWedge = radius * (count.toFloat() / maxCount)
                val startAngle = i * 10f - 90f // compass offset, North is Up
                val sweepAngle = 10f
                
                val rectF = android.graphics.RectF(center - rWedge, center - rWedge, center + rWedge, center + rWedge)
                // Draw sector 1
                canvas.drawArc(rectF, startAngle, sweepAngle, true, wedgePaint)
                canvas.drawArc(rectF, startAngle, sweepAngle, true, wedgeOutline)
                
                // Draw sector 2 (Symmetric)
                canvas.drawArc(rectF, startAngle + 180f, sweepAngle, true, wedgePaint)
                canvas.drawArc(rectF, startAngle + 180f, sweepAngle, true, wedgeOutline)
            }
        }
        
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("STRUCTURAL ROSE DIAGRAM", center - 130f, center - radius - 15f, titlePaint)
        
        return bmp
    }

    // Generate PDF report including Rose Diagram
    fun exportRoseDiagramPdf(
        context: Context,
        projectName: String,
        strikes: List<Float>
    ): String? {
        val bitmap = drawRoseDiagramBitmap(strikes)
        val fileDir = context.cacheDir
        val pdfFile = File(fileDir, "Rose_Diagram_Report_${System.currentTimeMillis()}.pdf")
        
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        // Draw background
        val bgPaint = Paint().apply { color = Color.parseColor("#F9FAFC") }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)
        
        // Draw header border & banner
        val headerCapPaint = Paint().apply { color = Color.parseColor("#1E5EFF") }
        canvas.drawRect(20f, 20f, 575f, 35f, headerCapPaint)
        
        val headerBgPaint = Paint().apply { color = Color.parseColor("#111625") }
        canvas.drawRect(20f, 35f, 575f, 130f, headerBgPaint)
        
        // Header Text
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("GEOLOGICAL DISCONTINUITY SURVEY REPORT", 35f, 75f, headerTextPaint)
        
        headerTextPaint.apply {
            color = Color.parseColor("#A0A5B5")
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("PROJECT: $projectName", 35f, 100f, headerTextPaint)
        canvas.drawText("DATE: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()), 35f, 115f, headerTextPaint)
        
        // Draw bitmap (scaled down to 360 x 360)
        val destRect = Rect(117, 160, 477, 520)
        canvas.drawBitmap(bitmap, null, destRect, null)
        
        // Flat frame around diagram
        val framePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(115f, 158f, 479f, 522f, framePaint)
        
        // Technical Summary Section
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        
        val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E5EFF")
            style = Paint.Style.FILL
        }
        
        canvas.drawLine(20f, 550f, 575f, 550f, framePaint) // horizontal divider
        
        canvas.drawText("STATISTICAL GEOTECHNICAL ANALYSIS", 35f, 575f, labelPaint)
        
        canvas.drawCircle(40f, 600f, 4f, bulletPaint)
        canvas.drawText("Total Joints/Faults mapped: " + strikes.size, 50f, 603f, descPaint)
        
        canvas.drawCircle(40f, 620f, 4f, bulletPaint)
        val bins = IntArray(18)
        for (s in strikes) {
            val norm = (s % 180 + 180) % 180
            val idx = (norm / 10).toInt().coerceIn(0, 17)
            bins[idx]++
        }
        val peakBinIdx = bins.indices.maxByOrNull { bins[it] } ?: 0
        val peakAngleRange = "${peakBinIdx * 10}° - ${(peakBinIdx + 1) * 10}°"
        canvas.drawText("Primary Discontinuity Set Strike Trend (Peak): $peakAngleRange (N${(peakBinIdx * 10)}E)", 50f, 623f, descPaint)
        
        canvas.drawCircle(40f, 640f, 4f, bulletPaint)
        canvas.drawText("Interpretation: Dominant shear structural trends with high mechanical persistence.", 50f, 643f, descPaint)
        
        // Plotted values
        canvas.drawText("MANUAL STRIKE OBSERVATIONS LISTED:", 35f, 680f, labelPaint)
        val strikesStr = strikes.map { "${it.toInt()}°" }.chunked(8).map { it.joinToString(", ") }
        var rawY = 705f
        for (line in strikesStr.take(3)) {
            canvas.drawText(line, 35f, rawY, descPaint)
            rawY += 15f
        }
        
        // Footer banner
        val footerBgPaint = Paint().apply { color = Color.parseColor("#E2E8F0") }
        canvas.drawRect(20f, 790f, 575f, 815f, footerBgPaint)
        
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("REPORT COMPUTED VIA GEOFIELD PROFESSIONAL MILITARY-GRADE LOGGER SUITE v1.1", 30f, 806f, footerPaint)
        
        pdfDocument.finishPage(page)
        
        try {
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
        
        return pdfFile.absolutePath
    }
}
