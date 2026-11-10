package com.example

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Data model for a georeferenced/calibrated geological map scan or aerial photo.
 * Projected as a high-performance Leaflet GroundOverlay directly on top of GPS telemetry.
 */
data class CalibratedMap(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val fileName: String,
    val northLat: Double,
    val southLat: Double,
    val eastLon: Double,
    val westLon: Double,
    val opacity: Float = 0.85f,
    val isVisible: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val centerLat: Double get() = (northLat + southLat) / 2.0
    val centerLon: Double get() = (eastLon + westLon) / 2.0

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("fileName", fileName)
            put("northLat", northLat)
            put("southLat", southLat)
            put("eastLon", eastLon)
            put("westLon", westLon)
            put("opacity", opacity.toDouble())
            put("isVisible", isVisible)
            put("createdAt", createdAt)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): CalibratedMap {
            return CalibratedMap(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name", "Map Overlay"),
                fileName = obj.optString("fileName", ""),
                northLat = obj.optDouble("northLat", 0.0),
                southLat = obj.optDouble("southLat", 0.0),
                eastLon = obj.optDouble("eastLon", 0.0),
                westLon = obj.optDouble("westLon", 0.0),
                opacity = obj.optDouble("opacity", 0.85).toFloat(),
                isVisible = obj.optBoolean("isVisible", true),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}

/**
 * Central manager for all user-imported maps:
 * 1. Calibrated Geological Map Scans (JPG/PNG/WebP with UTM or Lat/Lon bounds)
 * 2. Offline MBTiles packages (.mbtiles, .sqlite from SAS Planet, QGIS, MOBAC)
 * 3. Vector Layers (KML, KMZ, GPX, GeoJSON)
 */
object UserMapManager {

    private const val PREFS_NAME = "user_maps_storage_prefs"
    private const val KEY_CALIBRATED_MAPS = "calibrated_maps_json"
    private const val KEY_ACTIVE_MBTILES = "active_mbtiles_json"

    fun getGeorefDir(context: Context): File {
        val dir = File(context.filesDir, "georef_maps")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getMbtilesDir(context: Context): File {
        val dir = File(context.filesDir, "mbtiles")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // --- CALIBRATED MAP IMAGES MANAGEMENT ---

    fun getCalibratedMaps(context: Context): List<CalibratedMap> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CALIBRATED_MAPS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<CalibratedMap>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                list.add(CalibratedMap.fromJson(obj))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveCalibratedMaps(context: Context, maps: List<CalibratedMap>) {
        val array = JSONArray()
        maps.forEach { array.put(it.toJson()) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CALIBRATED_MAPS, array.toString())
            .apply()
    }

    fun addCalibratedMap(context: Context, map: CalibratedMap) {
        val current = getCalibratedMaps(context).toMutableList()
        current.removeAll { it.id == map.id }
        current.add(0, map)
        saveCalibratedMaps(context, current)
    }

    fun deleteCalibratedMap(context: Context, id: String) {
        val current = getCalibratedMaps(context).toMutableList()
        val toRemove = current.find { it.id == id }
        if (toRemove != null) {
            val file = File(getGeorefDir(context), toRemove.fileName)
            if (file.exists()) file.delete()
            current.remove(toRemove)
            saveCalibratedMaps(context, current)
        }
    }

    fun toggleCalibratedMapVisibility(context: Context, id: String, isVisible: Boolean) {
        val current = getCalibratedMaps(context).map {
            if (it.id == id) it.copy(isVisible = isVisible) else it
        }
        saveCalibratedMaps(context, current)
    }

    fun updateCalibratedMapOpacity(context: Context, id: String, opacity: Float) {
        val current = getCalibratedMaps(context).map {
            if (it.id == id) it.copy(opacity = opacity.coerceIn(0.05f, 1.0f)) else it
        }
        saveCalibratedMaps(context, current)
    }

    /**
     * Imports an image file from URI and calibrates it into a CalibratedMap.
     */
    fun importAndCalibrateImage(
        context: Context,
        uri: Uri,
        name: String,
        northLat: Double,
        southLat: Double,
        eastLon: Double,
        westLon: Double,
        opacity: Float = 0.85f
    ): Result<CalibratedMap> {
        return try {
            val extension = getExtensionFromUri(context, uri) ?: "jpg"
            val sanitizedBase = name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(30)
            val fileName = "georef_${sanitizedBase}_${System.currentTimeMillis()}.$extension"
            val destFile = File(getGeorefDir(context), fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("Cannot open image input stream"))

            val calibrated = CalibratedMap(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { "Geological Map Image" },
                fileName = fileName,
                northLat = maxOf(northLat, southLat),
                southLat = minOf(northLat, southLat),
                eastLon = maxOf(eastLon, westLon),
                westLon = minOf(eastLon, westLon),
                opacity = opacity,
                isVisible = true
            )
            addCalibratedMap(context, calibrated)
            Result.success(calibrated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- MBTILES MAP OVERLAYS MANAGEMENT ---

    fun getImportedMbtilesFiles(context: Context): List<File> {
        val dir = getMbtilesDir(context)
        val files = dir.listFiles { _, name ->
            name.endsWith(".mbtiles", ignoreCase = true) || name.endsWith(".sqlite", ignoreCase = true)
        }
        return files?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun getActiveMbtiles(context: Context): Map<String, Float> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ACTIVE_MBTILES, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, Float>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optDouble(k, 1.0).toFloat()
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveActiveMbtiles(context: Context, map: Map<String, Float>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v.toDouble()) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_MBTILES, obj.toString())
            .apply()
    }

    fun toggleMbtilesActive(context: Context, fileName: String, isActive: Boolean) {
        val current = getActiveMbtiles(context).toMutableMap()
        if (isActive) {
            if (!current.containsKey(fileName)) current[fileName] = 0.95f
        } else {
            current.remove(fileName)
        }
        saveActiveMbtiles(context, current)
    }

    fun setMbtilesOpacity(context: Context, fileName: String, opacity: Float) {
        val current = getActiveMbtiles(context).toMutableMap()
        current[fileName] = opacity.coerceIn(0.05f, 1.0f)
        saveActiveMbtiles(context, current)
    }

    fun deleteMbtilesFile(context: Context, fileName: String) {
        val file = File(getMbtilesDir(context), fileName)
        if (file.exists()) file.delete()
        MbtilesDatabaseCache.closeDatabase(file)
        toggleMbtilesActive(context, fileName, false)
    }

    fun importMbtilesFromUri(
        context: Context,
        uri: Uri,
        onProgress: (Float, String) -> Unit
    ): Result<File> {
        return try {
            onProgress(0.1f, "در حال آماده‌سازی فایل نقشه...")
            val rawName = queryFileName(context, uri) ?: "imported_map_${System.currentTimeMillis()}.mbtiles"
            val sanitized = rawName.replace(Regex("[^a-zA-Z0-9_\\-\\.]"), "_")
            val finalName = if (sanitized.endsWith(".mbtiles", true) || sanitized.endsWith(".sqlite", true)) {
                sanitized
            } else {
                "$sanitized.mbtiles"
            }

            val destFile = File(getMbtilesDir(context), finalName)
            onProgress(0.3f, "در حال کپی پایگاه داده کاشی‌های نقشه...")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            } ?: return Result.failure(Exception("Cannot open MBTiles stream"))

            onProgress(0.85f, "در حال اعتبارسنجی یکپارچگی نقشه...")
            val db = MbtilesDatabaseCache.getDatabase(destFile)
            if (db == null) {
                destFile.delete()
                return Result.failure(Exception("فایل MBTiles معتبر نیست یا آسیب دیده است."))
            }

            // Set active by default
            toggleMbtilesActive(context, finalName, true)
            onProgress(1.0f, "نقشه با موفقیت بارگذاری و فعال شد!")
            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMbtilesBounds(file: File): DoubleArray? {
        // Returns [minLon, minLat, maxLon, maxLat] if found in metadata table
        return try {
            val db = MbtilesDatabaseCache.getDatabase(file) ?: return null
            var cursor = db.rawQuery("SELECT value FROM metadata WHERE name = 'bounds'", null)
            var boundsStr: String? = null
            if (cursor.moveToFirst()) {
                boundsStr = cursor.getString(0)
            }
            cursor.close()

            if (boundsStr != null) {
                val parts = boundsStr.split(",").mapNotNull { it.trim().toDoubleOrNull() }
                if (parts.size >= 4) {
                    return doubleArrayOf(parts[0], parts[1], parts[2], parts[3])
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun getExtensionFromUri(context: Context, uri: Uri): String? {
        val mime = context.contentResolver.getType(uri)
        return when (mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> {
                val path = uri.path ?: ""
                val dot = path.lastIndexOf('.')
                if (dot >= 0) path.substring(dot + 1) else null
            }
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {}
        return name
    }
}
