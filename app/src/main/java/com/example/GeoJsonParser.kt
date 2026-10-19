package com.example

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

/**
 * Robust GeoJSON parser for geological features, fault lines, polygons, and waypoints.
 * Complies with RFC 7946 GeoJSON standard.
 */
object GeoJsonParser {

    private val colorPalette = listOf(
        "#E53935", // Red
        "#43A047", // Green
        "#1E88E5", // Blue
        "#FDD835", // Yellow
        "#8E24AA", // Purple
        "#FB8C00", // Orange
        "#00ACC1"  // Cyan
    )

    fun parse(inputStream: InputStream): List<ImportedFeature> {
        val features = mutableListOf<ImportedFeature>()
        try {
            val jsonText = inputStream.bufferedReader().use { it.readText() }
            if (jsonText.isBlank()) return emptyList()

            val root = JSONObject(jsonText)
            val type = root.optString("type", "")

            when (type) {
                "FeatureCollection" -> {
                    val featureArray = root.optJSONArray("features") ?: JSONArray()
                    for (i in 0 until featureArray.length()) {
                        val fObj = featureArray.optJSONObject(i) ?: continue
                        parseSingleFeature(fObj, features)
                    }
                }
                "Feature" -> {
                    parseSingleFeature(root, features)
                }
                "GeometryCollection" -> {
                    val geometries = root.optJSONArray("geometries") ?: JSONArray()
                    for (i in 0 until geometries.length()) {
                        val gObj = geometries.optJSONObject(i) ?: continue
                        parseGeometry(gObj, "GeoJSON Geometry ${features.size + 1}", "", features)
                    }
                }
                else -> {
                    // Try parsing as a raw geometry (Point, LineString, Polygon, etc.)
                    parseGeometry(root, "GeoJSON Layer", "", features)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return features
    }

    private fun parseSingleFeature(featureObj: JSONObject, outList: MutableList<ImportedFeature>) {
        val properties = featureObj.optJSONObject("properties") ?: JSONObject()
        val geometry = featureObj.optJSONObject("geometry") ?: return

        val name = properties.optString("name").takeIf { it.isNotBlank() }
            ?: properties.optString("title").takeIf { it.isNotBlank() }
            ?: properties.optString("label").takeIf { it.isNotBlank() }
            ?: properties.optString("id").takeIf { it.isNotBlank() }
            ?: "GeoJSON Feature #${outList.size + 1}"

        val desc = properties.optString("description").takeIf { it.isNotBlank() }
            ?: properties.optString("desc").takeIf { it.isNotBlank() }
            ?: properties.optString("comment").takeIf { it.isNotBlank() }
            ?: buildPropertiesSummary(properties)

        val customColor = properties.optString("stroke").takeIf { it.isNotBlank() }
            ?: properties.optString("fill").takeIf { it.isNotBlank() }
            ?: properties.optString("color").takeIf { it.isNotBlank() }
            ?: properties.optString("marker-color").takeIf { it.isNotBlank() }

        parseGeometry(geometry, name, desc, outList, customColor)
    }

    private fun parseGeometry(
        geomObj: JSONObject,
        name: String,
        desc: String,
        outList: MutableList<ImportedFeature>,
        preferredColor: String? = null
    ) {
        val geomType = geomObj.optString("type", "")
        val coordinates = geomObj.optJSONArray("coordinates") ?: return
        val color = preferredColor ?: colorPalette[outList.size % colorPalette.size]

        when (geomType) {
            "Point" -> {
                // coordinates: [lon, lat] or [lon, lat, ele]
                if (coordinates.length() >= 2) {
                    val lon = coordinates.optDouble(0)
                    val lat = coordinates.optDouble(1)
                    if (!lon.isNaN() && !lat.isNaN()) {
                        outList.add(
                            ImportedFeature(
                                id = "geojson_pt_${outList.size}_${System.currentTimeMillis()}",
                                name = name,
                                description = desc,
                                type = "Point",
                                coordinates = listOf(Coord(lat, lon)),
                                colorHex = color
                            )
                        )
                    }
                }
            }
            "MultiPoint" -> {
                // coordinates: [[lon, lat], [lon, lat]]
                val pts = extractCoordinatesList(coordinates)
                pts.forEachIndexed { idx, coord ->
                    outList.add(
                        ImportedFeature(
                            id = "geojson_mpt_${outList.size}_$idx",
                            name = "$name #$idx",
                            description = desc,
                            type = "Point",
                            coordinates = listOf(coord),
                            colorHex = color
                        )
                    )
                }
            }
            "LineString" -> {
                // coordinates: [[lon, lat], [lon, lat], ...]
                val coords = extractCoordinatesList(coordinates)
                if (coords.isNotEmpty()) {
                    outList.add(
                        ImportedFeature(
                            id = "geojson_line_${outList.size}_${System.currentTimeMillis()}",
                            name = name,
                            description = desc,
                            type = "LineString",
                            coordinates = coords,
                            colorHex = color
                        )
                    )
                }
            }
            "MultiLineString" -> {
                // coordinates: [ [[lon, lat], ...], [[lon, lat], ...] ]
                for (i in 0 until coordinates.length()) {
                    val lineArr = coordinates.optJSONArray(i) ?: continue
                    val coords = extractCoordinatesList(lineArr)
                    if (coords.isNotEmpty()) {
                        outList.add(
                            ImportedFeature(
                                id = "geojson_mline_${outList.size}_$i",
                                name = "$name (Segment ${i + 1})",
                                description = desc,
                                type = "LineString",
                                coordinates = coords,
                                colorHex = color
                            )
                        )
                    }
                }
            }
            "Polygon" -> {
                // coordinates: [ [ [lon, lat], ... ], ... ] (first is outer boundary)
                if (coordinates.length() > 0) {
                    val outerRing = coordinates.optJSONArray(0)
                    if (outerRing != null) {
                        val coords = extractCoordinatesList(outerRing)
                        if (coords.isNotEmpty()) {
                            outList.add(
                                ImportedFeature(
                                    id = "geojson_poly_${outList.size}_${System.currentTimeMillis()}",
                                    name = name,
                                    description = desc,
                                    type = "Polygon",
                                    coordinates = coords,
                                    colorHex = color
                                )
                            )
                        }
                    }
                }
            }
            "MultiPolygon" -> {
                // coordinates: [ [ [ [lon, lat], ... ] ], ... ]
                for (i in 0 until coordinates.length()) {
                    val polyRings = coordinates.optJSONArray(i) ?: continue
                    if (polyRings.length() > 0) {
                        val outerRing = polyRings.optJSONArray(0)
                        if (outerRing != null) {
                            val coords = extractCoordinatesList(outerRing)
                            if (coords.isNotEmpty()) {
                                outList.add(
                                    ImportedFeature(
                                        id = "geojson_mpoly_${outList.size}_$i",
                                        name = "$name (Poly ${i + 1})",
                                        description = desc,
                                        type = "Polygon",
                                        coordinates = coords,
                                        colorHex = color
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extractCoordinatesList(arr: JSONArray): List<Coord> {
        val result = mutableListOf<Coord>()
        for (i in 0 until arr.length()) {
            val pt = arr.optJSONArray(i) ?: continue
            if (pt.length() >= 2) {
                val lon = pt.optDouble(0)
                val lat = pt.optDouble(1)
                if (!lon.isNaN() && !lat.isNaN()) {
                    result.add(Coord(lat, lon))
                }
            }
        }
        return result
    }

    private fun buildPropertiesSummary(props: JSONObject): String {
        val sb = StringBuilder()
        val keys = props.keys()
        var count = 0
        while (keys.hasNext() && count < 5) {
            val key = keys.next()
            val v = props.opt(key)
            if (v != null && key != "stroke" && key != "fill" && key != "color") {
                sb.append("<b>$key:</b> $v<br>")
                count++
            }
        }
        return sb.toString().trimEnd()
    }
}
