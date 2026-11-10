package com.example

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh

/**
 * SAS.Planet-compatible projection modes (matching params.txt `projection=` values in sasgis/sas.planet.src).
 */
enum class SasProjection(val code: Int, val label: String) {
    SPHERICAL_MERCATOR(1, "EPSG:3857 (Web Mercator)"),
    ELLIPTICAL_MERCATOR(2, "EPSG:3395 (Elliptical / Yandex)"),
    LON_LAT(3, "EPSG:4326 (Geographic Lon/Lat)");

    companion object {
        fun fromCode(code: Int): SasProjection = entries.find { it.code == code } ?: SPHERICAL_MERCATOR
    }
}

/**
 * Tile addressing schemes supported by SAS.Planet ZMP providers.
 */
enum class SasTileAddressing {
    XYZ,
    TMS,
    QUADKEY,
    Z_PLUS_1
}

/**
 * Represents a map or overlay source following the SAS.Planet ZMP (params.txt + GetUrlScript.txt) model.
 */
data class SasMapSource(
    val id: String,
    val name: String,
    val nameFa: String = name,
    val category: String = "Satellite",
    val urlTemplate: String,
    val subdomains: List<String> = emptyList(),
    val projection: SasProjection = SasProjection.SPHERICAL_MERCATOR,
    val addressing: SasTileAddressing = SasTileAddressing.XYZ,
    val isOverlay: Boolean = false,
    val minZoom: Int = 1,
    val maxZoom: Int = 20,
    val referer: String? = null,
    val userAgent: String? = null,
    val contentType: String = "image/png",
    val isBuiltIn: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("nameFa", nameFa)
        put("category", category)
        put("urlTemplate", urlTemplate)
        put("subdomains", JSONArray(subdomains))
        put("projection", projection.code)
        put("addressing", addressing.name)
        put("isOverlay", isOverlay)
        put("minZoom", minZoom)
        put("maxZoom", maxZoom)
        if (referer != null) put("referer", referer)
        if (userAgent != null) put("userAgent", userAgent)
        put("contentType", contentType)
        put("isBuiltIn", isBuiltIn)
    }

    companion object {
        fun fromJson(obj: JSONObject): SasMapSource {
            val subsArray = obj.optJSONArray("subdomains")
            val subs = mutableListOf<String>()
            if (subsArray != null) {
                for (i in 0 until subsArray.length()) {
                    val s = subsArray.optString(i)
                    if (s.isNotBlank()) subs.add(s)
                }
            }
            val addrName = obj.optString("addressing", SasTileAddressing.XYZ.name)
            val addr = try { SasTileAddressing.valueOf(addrName) } catch (_: Exception) { SasTileAddressing.XYZ }
            return SasMapSource(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name", "Custom SAS Map"),
                nameFa = obj.optString("nameFa", obj.optString("name", "نقشه سفارشی SAS")),
                category = obj.optString("category", "Custom ZMP"),
                urlTemplate = obj.optString("urlTemplate", ""),
                subdomains = subs,
                projection = SasProjection.fromCode(obj.optInt("projection", 1)),
                addressing = addr,
                isOverlay = obj.optBoolean("isOverlay", false),
                minZoom = obj.optInt("minZoom", 1),
                maxZoom = obj.optInt("maxZoom", 20),
                referer = obj.optString("referer", "").takeIf { it.isNotBlank() },
                userAgent = obj.optString("userAgent", "").takeIf { it.isNotBlank() },
                contentType = obj.optString("contentType", "image/png"),
                isBuiltIn = obj.optBoolean("isBuiltIn", false)
            )
        }
    }
}

/**
 * Built-in catalog of SAS.Planet (sas.maps) base maps and transparent overlays.
 */
object SasPlanetCatalog {
    val builtInBaseMaps: List<SasMapSource> = listOf(
        SasMapSource(
            id = "sas_google_sat",
            name = "Google Satellite",
            nameFa = "ماهواره گوگل (Google Sat)",
            category = "Satellite",
            urlTemplate = "https://mt{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}",
            subdomains = listOf("0", "1", "2", "3"),
            maxZoom = 21,
            contentType = "image/jpeg",
            isBuiltIn = true
        ),
        SasMapSource(
            id = "sas_google_hybrid",
            name = "Google Hybrid",
            nameFa = "ترکیبی گوگل (Google Hybrid)",
            category = "Satellite",
            urlTemplate = "https://mt{s}.google.com/vt/lyrs=y&x={x}&y={y}&z={z}",
            subdomains = listOf("0", "1", "2", "3"),
            maxZoom = 21,
            contentType = "image/jpeg",
            isBuiltIn = true
        ),
        SasMapSource(
            id = "sas_bing_sat",
            name = "Bing Maps Satellite",
            nameFa = "ماهواره بینگ (Bing QuadKey)",
            category = "Satellite",
            urlTemplate = "https://ecn.t{s}.tiles.virtualearth.net/tiles/a{q}.jpeg?g=14571",
            subdomains = listOf("0", "1", "2", "3"),
            addressing = SasTileAddressing.QUADKEY,
            maxZoom = 20,
            contentType = "image/jpeg",
            referer = "https://www.bing.com/maps",
            isBuiltIn = true
        ),
        SasMapSource(
            id = "sas_esri_sat",
            name = "Esri World Imagery",
            nameFa = "ماهواره Esri World Imagery",
            category = "Satellite",
            urlTemplate = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
            maxZoom = 19,
            contentType = "image/jpeg",
            isBuiltIn = true
        ),
        SasMapSource(
            id = "sas_yandex_sat",
            name = "Yandex Satellite (EPSG:3395)",
            nameFa = "ماهواره یاندکس (Elliptical 3395)",
            category = "Satellite",
            urlTemplate = "https://sat0{s}.maps.yandex.net/tiles?l=sat&x={x}&y={y}&z={z}",
            subdomains = listOf("1", "2", "3", "4"),
            projection = SasProjection.ELLIPTICAL_MERCATOR,
            maxZoom = 19,
            contentType = "image/jpeg",
            referer = "https://yandex.com/maps/",
            isBuiltIn = true
        ),
        SasMapSource(
            id = "sas_opentopomap",
            name = "OpenTopoMap",
            nameFa = "توپوگرافی OpenTopoMap",
            category = "Topo",
            urlTemplate = "https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png",
            subdomains = listOf("a", "b", "c"),
            maxZoom = 17,
            contentType = "image/png",
            referer = "https://opentopomap.org/",
            isBuiltIn = true
        ),
        SasMapSource(
            id = "sas_google_terrain",
            name = "Google Terrain / Topo",
            nameFa = "توپوگرافی و ارتفاعات گوگل",
            category = "Topo",
            urlTemplate = "https://mt{s}.google.com/vt/lyrs=p&x={x}&y={y}&z={z}",
            subdomains = listOf("0", "1", "2", "3"),
            maxZoom = 18,
            contentType = "image/jpeg",
            isBuiltIn = true
        ),
        SasMapSource(
            id = "sas_esri_topo",
            name = "Esri World Topo",
            nameFa = "نقشه توپوگرافی Esri",
            category = "Topo",
            urlTemplate = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}",
            maxZoom = 19,
            contentType = "image/jpeg",
            isBuiltIn = true
        ),
        SasMapSource(
            id = "sas_macrostrat_base",
            name = "Macrostrat Geology Map",
            nameFa = "نقشه زمین‌شناسی Macrostrat",
            category = "Geology",
            urlTemplate = "https://tiles.macrostrat.org/carto/{z}/{x}/{y}.png",
            maxZoom = 16,
            contentType = "image/png",
            isBuiltIn = true
        ),
        SasMapSource(
            id = "sas_osm",
            name = "OpenStreetMap Standard",
            nameFa = "نقشه معابر OSM",
            category = "Street",
            urlTemplate = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
            subdomains = listOf("a", "b", "c"),
            maxZoom = 19,
            contentType = "image/png",
            referer = "https://www.openstreetmap.org/",
            isBuiltIn = true
        ),
        SasMapSource(
            id = "sas_google_roadmap",
            name = "Google Roadmap",
            nameFa = "نقشه راه‌های گوگل",
            category = "Street",
            urlTemplate = "https://mt{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
            subdomains = listOf("0", "1", "2", "3"),
            maxZoom = 20,
            contentType = "image/png",
            isBuiltIn = true
        )
    )

    val builtInOverlays: List<SasMapSource> = listOf(
        SasMapSource(
            id = "sas_ov_macrostrat",
            name = "Macrostrat Geology Overlay",
            nameFa = "لایه زمین‌شناسی (Macrostrat)",
            category = "Geology Overlay",
            urlTemplate = "https://tiles.macrostrat.org/carto/{z}/{x}/{y}.png",
            isOverlay = true,
            maxZoom = 16,
            contentType = "image/png",
            isBuiltIn = true
        ),
        SasMapSource(
            id = "sas_ov_esri_hillshade",
            name = "Esri World Hillshade",
            nameFa = "لایه سایه‌روشن ناهمواری (Hillshade)",
            category = "Terrain Overlay",
            urlTemplate = "https://services.arcgisonline.com/arcgis/rest/services/Elevation/World_Hillshade/MapServer/tile/{z}/{y}/{x}",
            isOverlay = true,
            maxZoom = 18,
            contentType = "image/jpeg",
            isBuiltIn = true
        ),
        SasMapSource(
            id = "sas_ov_google_labels",
            name = "Google Roads & Labels",
            nameFa = "لایه برچسب‌ها و جاده‌های گوگل",
            category = "Labels Overlay",
            urlTemplate = "https://mt{s}.google.com/vt/lyrs=h&x={x}&y={y}&z={z}",
            subdomains = listOf("0", "1", "2", "3"),
            isOverlay = true,
            maxZoom = 20,
            contentType = "image/png",
            isBuiltIn = true
        ),
        SasMapSource(
            id = "sas_ov_esri_boundaries",
            name = "Esri Places & Boundaries",
            nameFa = "لایه مرزها و نام مکان‌ها (Esri)",
            category = "Labels Overlay",
            urlTemplate = "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}",
            isOverlay = true,
            maxZoom = 18,
            contentType = "image/png",
            isBuiltIn = true
        )
    )

    fun allBuiltIn(): List<SasMapSource> = builtInBaseMaps + builtInOverlays
}

/**
 * Persistent storage for user-imported SAS.Planet ZMP sources.
 */
object SasZmpStorage {
    private const val PREFS_NAME = "sas_planet_zmp_prefs"
    private const val KEY_ZMP_SOURCES = "sas_zmp_sources_json"

    fun getCustomSources(context: Context): List<SasMapSource> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ZMP_SOURCES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SasMapSource>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(SasMapSource.fromJson(obj))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCustomSources(context: Context, sources: List<SasMapSource>) {
        val arr = JSONArray()
        sources.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ZMP_SOURCES, arr.toString())
            .apply()
    }

    fun addOrUpdateSource(context: Context, source: SasMapSource) {
        val current = getCustomSources(context).toMutableList()
        current.removeAll { it.id == source.id }
        current.add(0, source)
        saveCustomSources(context, current)
    }

    fun deleteSource(context: Context, id: String) {
        val current = getCustomSources(context).toMutableList()
        current.removeAll { it.id == id }
        saveCustomSources(context, current)
    }

    fun findSourceById(context: Context, id: String): SasMapSource? {
        return SasPlanetCatalog.allBuiltIn().find { it.id == id }
            ?: getCustomSources(context).find { it.id == id }
    }
}

/**
 * Parses SAS.Planet `.zmp` archives (ZIP containing `params.txt` & optional `GetUrlScript.txt`)
 * or standalone `params.txt` files directly from Android URIs.
 */
object SasZmpParser {

    fun importFromUri(context: Context, uri: Uri): Result<SasMapSource> {
        return try {
            var paramsText: String? = null
            var scriptText: String? = null

            val fileName = queryFileName(context, uri) ?: "custom.zmp"
            val isTxt = fileName.endsWith(".txt", ignoreCase = true)

            if (isTxt) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    paramsText = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                }
            } else {
                // Try reading as ZIP (.zmp)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val zis = ZipInputStream(stream)
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val entryName = entry.name.lowercase()
                        if (!entry.isDirectory) {
                            if (entryName == "params.txt" || entryName.endsWith("/params.txt")) {
                                paramsText = zis.readBytes().toString(Charsets.UTF_8)
                            } else if (entryName == "geturlscript.txt" || entryName.endsWith("/geturlscript.txt")) {
                                scriptText = zis.readBytes().toString(Charsets.UTF_8)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                // Fallback: if not a zip, try reading raw text as params.txt
                if (paramsText == null) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                        if (text.contains("[PARAMS]", ignoreCase = true) || text.contains("DefURLBase", ignoreCase = true)) {
                            paramsText = text
                        }
                    }
                }
            }

            val rawParams = paramsText ?: return Result.failure(
                Exception("فایل params.txt در بسته ZMP یافت نشد.")
            )

            val parsed = parseParamsAndScript(rawParams, scriptText, fileName)
            if (parsed.urlTemplate.isBlank()) {
                return Result.failure(Exception("آدرس سرور (DefURLBase) در فایل ZMP معتبر نیست."))
            }
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseParamsAndScript(
        paramsText: String,
        scriptText: String? = null,
        fallbackName: String = "SAS ZMP"
    ): SasMapSource {
        val map = mutableMapOf<String, String>()
        val cleanParams = paramsText.removePrefix("\uFEFF")
        for (line in cleanParams.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith(";") || trimmed.startsWith("#") || trimmed.startsWith("[")) {
                continue
            }
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx > 0) {
                val key = trimmed.substring(0, eqIdx).trim().lowercase()
                val value = trimmed.substring(eqIdx + 1).trim()
                map[key] = value
            }
        }

        val guid = map["guid"]?.replace("{", "")?.replace("}", "")?.takeIf { it.isNotBlank() }
            ?: "zmp_${UUID.randomUUID().toString().take(8)}"
        val name = map["name_en"] ?: map["name"] ?: fallbackName.removeSuffix(".zmp")
        val nameFa = map["name"] ?: name
        var defUrlBase = map["defurlbase"] ?: ""
        val sradiusB = map["sradiusb"]?.toDoubleOrNull()
        val rawProjCode = map["projection"]?.toIntOrNull()
        val projCode = rawProjCode ?: if (sradiusB != null && sradiusB < 6360000.0) 2 else 1
        val isOverlay = (map["aslayer"]?.toIntOrNull() ?: 0) == 1
        val ext = map["ext"]?.lowercase() ?: ".png"
        val contentType = if (ext.contains("jpg") || ext.contains("jpeg")) "image/jpeg" else "image/png"
        val maxZoom = map["maxzoom"]?.toIntOrNull() ?: 20
        val minZoom = map["minzoom"]?.toIntOrNull() ?: 1

        // Parse RequestHead for Referer and User-Agent
        val requestHead = map["requesthead"] ?: ""
        var referer: String? = null
        var userAgent: String? = null
        for (part in requestHead.split("\\r\\n", "\n", ";")) {
            val p = part.trim()
            if (p.startsWith("Referer:", ignoreCase = true)) {
                referer = p.substringAfter(":").trim()
            } else if (p.startsWith("User-Agent:", ignoreCase = true)) {
                userAgent = p.substringAfter(":").trim()
            }
        }

        var addressing = SasTileAddressing.XYZ
        val subdomains = mutableListOf<String>()

        // Analyze GetUrlScript.txt if present and DefURLBase doesn't already have {x}/{y}/{z}
        if (scriptText != null) {
            val lowerScript = scriptText.lowercase()
            if (lowerScript.contains("quadkey") || lowerScript.contains("getq")) {
                addressing = SasTileAddressing.QUADKEY
            } else if (lowerScript.contains("1 shl getz") || lowerScript.contains("tms")) {
                addressing = SasTileAddressing.TMS
            } else if (lowerScript.contains("getz+1") || lowerScript.contains("getz + 1")) {
                addressing = SasTileAddressing.Z_PLUS_1
            }

            // Detect common subdomain arrays in GetUrlScript.txt
            if (lowerScript.contains("'a'") && lowerScript.contains("'b'") && lowerScript.contains("'c'")) {
                subdomains.addAll(listOf("a", "b", "c"))
            } else if (lowerScript.contains("mt0") || lowerScript.contains("mt1")) {
                subdomains.addAll(listOf("0", "1", "2", "3"))
            }
        }

        // Normalize DefURLBase into a template if it doesn't contain placeholders yet
        var urlTemplate = defUrlBase
        if (urlTemplate.isNotBlank() &&
            !urlTemplate.contains("{x}", ignoreCase = true) &&
            !urlTemplate.contains("{q}", ignoreCase = true)
        ) {
            if (addressing == SasTileAddressing.QUADKEY) {
                urlTemplate = "${urlTemplate.trimEnd('/')}/{q}$ext"
            } else {
                // Standard SAS.Planet REST fallback if script wasn't explicit
                val yToken = if (addressing == SasTileAddressing.TMS) "{-y}" else "{y}"
                val zToken = if (addressing == SasTileAddressing.Z_PLUS_1) "{z+1}" else "{z}"
                urlTemplate = "${urlTemplate.trimEnd('/')}/$zToken/{x}/$yToken$ext"
            }
        }

        return SasMapSource(
            id = guid,
            name = name,
            nameFa = nameFa,
            category = if (isOverlay) "Custom ZMP Overlay" else "Custom ZMP",
            urlTemplate = urlTemplate,
            subdomains = subdomains,
            projection = SasProjection.fromCode(projCode),
            addressing = addressing,
            isOverlay = isOverlay,
            minZoom = minZoom,
            maxZoom = maxZoom,
            referer = referer,
            userAgent = userAgent,
            contentType = contentType,
            isBuiltIn = false
        )
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = cursor.getString(idx)
                }
            }
        } catch (_: Exception) {}
        return name
    }
}

/**
 * SAS.Planet-inspired Tile Proxy & Coordinate Projection Engine.
 * Intercepts `https://sas.local/{sourceId}/{z}/{x}/{y}.tile` requests from Leaflet WebView,
 * handles Elliptical Mercator (EPSG:3395), Bing QuadKey, TMS Y-inversion, custom Referer/User-Agent headers,
 * and automatic SQLite offline tile caching.
 */
object SasTileProxy {
    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    fun buildLocalTileUrl(sourceId: String): String {
        val encoded = java.net.URLEncoder.encode(sourceId, "UTF-8")
        return "https://sas.local/$encoded/{z}/{x}/{y}.tile"
    }

    /**
     * Converts standard Web Mercator (EPSG:3857) tile Y at zoom z to Elliptical Mercator (EPSG:3395, e.g. Yandex) tile Y.
     */
    fun webMercatorYToEllipticalY(z: Int, y3857: Int): Int {
        val n = (1 shl z).toDouble()
        val latRad = atan(sinh(PI * (1.0 - 2.0 * (y3857 + 0.5) / n)))
        val e = 0.08181919084262149 // WGS84 eccentricity
        val sinLat = sin(latRad)
        val eSinLat = e * sinLat
        val mercY = ln(
            kotlin.math.tan(PI / 4.0 + latRad / 2.0) *
                ((1.0 - eSinLat) / (1.0 + eSinLat)).pow(e / 2.0)
        )
        val yNorm = (1.0 - mercY / PI) / 2.0
        return (yNorm * n).toInt().coerceIn(0, (1 shl z) - 1)
    }

    /**
     * Converts tile (x, y, z) to Microsoft Bing Maps QuadKey string.
     */
    fun tileXYToQuadKey(x: Int, y: Int, z: Int): String {
        val quadKey = StringBuilder()
        for (i in z downTo 1) {
            var digit = '0'
            val mask = 1 shl (i - 1)
            if ((x and mask) != 0) digit++
            if ((y and mask) != 0) digit += 2
            quadKey.append(digit)
        }
        return quadKey.toString()
    }

    fun resolveRemoteUrl(source: SasMapSource, z: Int, x: Int, y: Int): String {
        val effectiveY = when (source.projection) {
            SasProjection.ELLIPTICAL_MERCATOR -> webMercatorYToEllipticalY(z, y)
            else -> y
        }
        val tmsY = (1 shl z) - 1 - effectiveY
        val quadKey = tileXYToQuadKey(x, effectiveY, z)
        val sub = if (source.subdomains.isNotEmpty()) {
            source.subdomains[((x + y).let { if (it < 0) -it else it }) % source.subdomains.size]
        } else {
            ""
        }

        var finalUrl = source.urlTemplate
        finalUrl = finalUrl
            .replace("{z+1}", (z + 1).toString())
            .replace("{z-1}", (z - 1).coerceAtLeast(0).toString())
            .replace("{z}", z.toString())
            .replace("{x}", x.toString())
            .replace("{-y}", tmsY.toString())
            .replace("{y}", if (source.addressing == SasTileAddressing.TMS) tmsY.toString() else effectiveY.toString())
            .replace("{q}", quadKey)
            .replace("{s}", sub)
        return finalUrl
    }

    fun interceptTileRequest(
        context: Context,
        requestUrl: String,
        offlineModeForce: Boolean
    ): WebResourceResponse? {
        if (!requestUrl.startsWith("https://sas.local/")) return null

        val path = requestUrl.substring("https://sas.local/".length)
        val parts = path.split("/")
        if (parts.size < 4) {
            return WebResourceResponse("image/png", "UTF-8", ByteArrayInputStream(ByteArray(0)))
        }

        val sourceId = try {
            java.net.URLDecoder.decode(parts[0], "UTF-8")
        } catch (_: Exception) {
            parts[0]
        }
        val z = parts[1].toIntOrNull() ?: 0
        val x = parts[2].toIntOrNull() ?: 0
        val yPart = parts[3]
        val yStr = if (yPart.contains(".")) yPart.substring(0, yPart.indexOf(".")) else yPart
        val y = yStr.toIntOrNull() ?: 0

        val source = SasZmpStorage.findSourceById(context, sourceId)
            ?: return WebResourceResponse("image/png", "UTF-8", ByteArrayInputStream(ByteArray(0)))

        val remoteUrl = resolveRemoteUrl(source, z, x, y)
        val helper = TileCacheDatabaseHelper.getInstance(context)
        val cached = helper.getTile(remoteUrl)
        if (cached != null) {
            return WebResourceResponse(cached.contentType, "UTF-8", ByteArrayInputStream(cached.data))
        }

        if (offlineModeForce) {
            return WebResourceResponse("image/png", "UTF-8", ByteArrayInputStream(ByteArray(0)))
        }

        try {
            val connection = URL(remoteUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.useCaches = false
            connection.setRequestProperty("User-Agent", source.userAgent ?: DEFAULT_USER_AGENT)
            if (!source.referer.isNullOrBlank()) {
                connection.setRequestProperty("Referer", source.referer)
            }
            connection.connect()

            if (connection.responseCode == 200) {
                val rawType = connection.contentType ?: source.contentType
                val contentType = rawType.substringBefore(";").trim().ifEmpty { source.contentType }
                if (contentType.startsWith("image/", ignoreCase = true)) {
                    val bytes = connection.inputStream.use { it.readBytes() }
                    if (bytes.isNotEmpty()) {
                        helper.putTile(remoteUrl, bytes, contentType)
                        return WebResourceResponse(contentType, "UTF-8", ByteArrayInputStream(bytes))
                    }
                }
            }
        } catch (_: Exception) {
            // Fail gracefully when offline or server unreachable
        }

        return WebResourceResponse("image/png", "UTF-8", ByteArrayInputStream(ByteArray(0)))
    }
}

data class SasDownloadProgress(
    val sourceId: String,
    val sourceName: String,
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Double,
    val minZoom: Int,
    val maxZoom: Int,
    val totalTiles: Int,
    val completedTiles: Int,
    val skippedCached: Int,
    val failedTiles: Int,
    val isRunning: Boolean
)

/**
 * SAS.Planet-style Area Tile Pre-Downloader & MBTiles Exporter.
 * Pre-caches all tiles for a geographic radius around (centerLat, centerLon) across a zoom range
 * into TileCacheDatabaseHelper, and can export the downloaded area as a standard .mbtiles file.
 */
object SasAreaDownloader {
    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    fun computeBounds(centerLat: Double, centerLon: Double, radiusKm: Double): DoubleArray {
        val latDelta = radiusKm / 111.32
        val cosLat = kotlin.math.cos(Math.toRadians(centerLat)).coerceAtLeast(0.1)
        val lonDelta = radiusKm / (111.32 * cosLat)
        val minLat = (centerLat - latDelta).coerceIn(-85.0, 85.0)
        val maxLat = (centerLat + latDelta).coerceIn(-85.0, 85.0)
        val minLon = (centerLon - lonDelta).coerceIn(-180.0, 180.0)
        val maxLon = (centerLon + lonDelta).coerceIn(-180.0, 180.0)
        return doubleArrayOf(minLon, minLat, maxLon, maxLat)
    }

    fun computeTileList(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        minZoom: Int,
        maxZoom: Int,
        maxTilesCap: Int = 1200
    ): List<Triple<Int, Int, Int>> {
        val bounds = computeBounds(centerLat, centerLon, radiusKm)
        val minLon = bounds[0]
        val minLat = bounds[1]
        val maxLon = bounds[2]
        val maxLat = bounds[3]

        val result = mutableListOf<Triple<Int, Int, Int>>()
        for (z in minZoom..maxZoom) {
            val n = 1 shl z
            val xMin = (((minLon + 180.0) / 360.0) * n).toInt().coerceIn(0, n - 1)
            val xMax = (((maxLon + 180.0) / 360.0) * n).toInt().coerceIn(0, n - 1)
            val yMin = latToTileY(maxLat, z).coerceIn(0, n - 1)
            val yMax = latToTileY(minLat, z).coerceIn(0, n - 1)

            for (x in xMin..xMax) {
                for (y in yMin..yMax) {
                    result.add(Triple(z, x, y))
                    if (result.size >= maxTilesCap) return result
                }
            }
        }
        return result
    }

    private fun latToTileY(lat: Double, z: Int): Int {
        val latRad = Math.toRadians(lat)
        val n = (1 shl z).toDouble()
        return ((1.0 - ln(kotlin.math.tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / PI) / 2.0 * n).toInt()
    }

    fun downloadSingleTile(
        context: Context,
        source: SasMapSource,
        z: Int,
        x: Int,
        y: Int
    ): Int {
        // Returns: 0 = skipped (already cached), 1 = downloaded, -1 = failed
        val remoteUrl = SasTileProxy.resolveRemoteUrl(source, z, x, y)
        val helper = TileCacheDatabaseHelper.getInstance(context)
        if (helper.getTile(remoteUrl) != null) return 0

        return try {
            val connection = URL(remoteUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 4500
            connection.readTimeout = 4500
            connection.useCaches = false
            connection.setRequestProperty("User-Agent", source.userAgent ?: DEFAULT_USER_AGENT)
            if (!source.referer.isNullOrBlank()) {
                connection.setRequestProperty("Referer", source.referer)
            }
            connection.connect()
            if (connection.responseCode == 200) {
                val rawType = connection.contentType ?: source.contentType
                val contentType = rawType.substringBefore(";").trim().ifEmpty { source.contentType }
                if (contentType.startsWith("image/", ignoreCase = true)) {
                    val bytes = connection.inputStream.use { it.readBytes() }
                    if (bytes.isNotEmpty()) {
                        helper.putTile(remoteUrl, bytes, contentType)
                        1
                    } else -1
                } else -1
            } else -1
        } catch (_: Exception) {
            -1
        }
    }

    /**
     * Packages all cached tiles for the specified area & source into a standard MBTiles 1.3 SQLite file.
     * Returns the number of tiles written into the MBTiles file.
     */
    fun exportAreaToMbtiles(
        context: Context,
        source: SasMapSource,
        mapTitle: String,
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        minZoom: Int,
        maxZoom: Int,
        outputFile: java.io.File
    ): Result<Int> {
        return try {
            MbtilesDatabaseCache.closeDatabase(outputFile)
            if (outputFile.exists()) outputFile.delete()
            val db = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(outputFile, null)
            val helper = TileCacheDatabaseHelper.getInstance(context)
            val tiles = computeTileList(centerLat, centerLon, radiusKm, minZoom, maxZoom)
            val bounds = computeBounds(centerLat, centerLon, radiusKm)
            val format = if (source.contentType.contains("jpeg", true) || source.contentType.contains("jpg", true)) "jpg" else "png"

            var writtenCount = 0
            db.beginTransaction()
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS metadata (name TEXT, value TEXT)")
                db.execSQL("CREATE TABLE IF NOT EXISTS tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS tile_index ON tiles (zoom_level, tile_column, tile_row)")

                val metaEntries = listOf(
                    "name" to mapTitle,
                    "type" to (if (source.isOverlay) "overlay" else "baselayer"),
                    "version" to "1.3",
                    "description" to "Exported from GeoField Pro (SAS.Planet Engine: ${source.name})",
                    "format" to format,
                    "bounds" to "${bounds[0]},${bounds[1]},${bounds[2]},${bounds[3]}",
                    "center" to "$centerLon,$centerLat,$maxZoom",
                    "minzoom" to minZoom.toString(),
                    "maxzoom" to maxZoom.toString()
                )
                for ((k, v) in metaEntries) {
                    val cv = android.content.ContentValues().apply {
                        put("name", k)
                        put("value", v)
                    }
                    db.insert("metadata", null, cv)
                }

                for ((z, x, y) in tiles) {
                    val remoteUrl = SasTileProxy.resolveRemoteUrl(source, z, x, y)
                    val cached = helper.getTile(remoteUrl) ?: continue
                    val tmsY = (1 shl z) - 1 - y
                    val cv = android.content.ContentValues().apply {
                        put("zoom_level", z)
                        put("tile_column", x)
                        put("tile_row", tmsY)
                        put("tile_data", cached.data)
                    }
                    db.insertWithOnConflict("tiles", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                    writtenCount++
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
                db.close()
            }

            if (writtenCount == 0) {
                outputFile.delete()
                return Result.failure(Exception("هیچ کاشی دانلودشده‌ای برای ذخیره یافت نشد."))
            }
            Result.success(writtenCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
