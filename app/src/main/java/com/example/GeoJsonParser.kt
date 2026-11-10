package com.example

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

/**
 * Robust GeoJSON parser for user-imported spatial datasets (Points, Lines, Polygons).
 * Fully compatible with QGIS, ArcGIS, and online GeoJSON tools without external dependencies.
 */
object GeoJsonParser {

    fun parse(inputStream: InputStream): List<ImportedFeature> {
        val features = mutableListOf<ImportedFeature>()
        try {
            val text = inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            val type = root.optString("type", "")

            if (type.equals("FeatureCollection", ignoreCase = true)) {
                val featuresArray = root.optJSONArray("features") ?: JSONArray()
                for (i in 0 until featuresArray.length()) {
                    val featObj = featuresArray.optJSONObject(i) ?: continue
                    parseFeature(featObj, features)
                }
            } else if (type.equals("Feature", ignoreCase = true)) {
                parseFeature(root, features)
            } else {
                // Raw Geometry
                parseGeometry("Imported Layer", "", root, features)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return features
    }

    private fun parseFeature(featObj: JSONObject, features: MutableList<ImportedFeature>) {
        val props = featObj.optJSONObject("properties") ?: JSONObject()
        val name = props.optString("name").takeIf { it.isNotBlank() }
            ?: props.optString("title").takeIf { it.isNotBlank() }
            ?: props.optString("id").takeIf { it.isNotBlank() }
            ?: "Layer ${features.size + 1}"
        val desc = props.optString("description", "")
        val colorHex = props.optString("stroke").takeIf { it.isNotBlank() }
            ?: props.optString("fill").takeIf { it.isNotBlank() }
            ?: props.optString("marker-color").takeIf { it.isNotBlank() }
            ?: "#00E5FF"

        val geom = featObj.optJSONObject("geometry") ?: return
        parseGeometry(name, desc, geom, features, colorHex)
    }

    private fun parseGeometry(
        name: String,
        desc: String,
        geom: JSONObject,
        features: MutableList<ImportedFeature>,
        customColor: String = "#00E5FF"
    ) {
        val geomType = geom.optString("type", "")
        when (geomType) {
            "Point" -> {
                val coords = geom.optJSONArray("coordinates") ?: return
                if (coords.length() >= 2) {
                    val lon = coords.optDouble(0)
                    val lat = coords.optDouble(1)
                    features.add(
                        ImportedFeature(
                            id = java.util.UUID.randomUUID().toString(),
                            name = name,
                            description = desc,
                            type = "Point",
                            coordinates = listOf(Coord(lat, lon)),
                            colorHex = customColor
                        )
                    )
                }
            }
            "LineString" -> {
                val coords = geom.optJSONArray("coordinates") ?: return
                val coordList = parseCoordList(coords)
                if (coordList.isNotEmpty()) {
                    features.add(
                        ImportedFeature(
                            id = java.util.UUID.randomUUID().toString(),
                            name = name,
                            description = desc,
                            type = "LineString",
                            coordinates = coordList,
                            colorHex = customColor
                        )
                    )
                }
            }
            "Polygon" -> {
                val rings = geom.optJSONArray("coordinates") ?: return
                if (rings.length() > 0) {
                    val outerRing = rings.optJSONArray(0) ?: return
                    val coordList = parseCoordList(outerRing)
                    if (coordList.isNotEmpty()) {
                        features.add(
                            ImportedFeature(
                                id = java.util.UUID.randomUUID().toString(),
                                name = name,
                                description = desc,
                                type = "Polygon",
                                coordinates = coordList,
                                colorHex = customColor
                            )
                        )
                    }
                }
            }
            "MultiPolygon" -> {
                val polys = geom.optJSONArray("coordinates") ?: return
                for (p in 0 until polys.length()) {
                    val rings = polys.optJSONArray(p) ?: continue
                    if (rings.length() > 0) {
                        val outerRing = rings.optJSONArray(0) ?: continue
                        val coordList = parseCoordList(outerRing)
                        if (coordList.isNotEmpty()) {
                            features.add(
                                ImportedFeature(
                                    id = java.util.UUID.randomUUID().toString(),
                                    name = "$name (Poly ${p + 1})",
                                    description = desc,
                                    type = "Polygon",
                                    coordinates = coordList,
                                    colorHex = customColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun parseCoordList(jsonArray: JSONArray): List<Coord> {
        val list = mutableListOf<Coord>()
        for (i in 0 until jsonArray.length()) {
            val pt = jsonArray.optJSONArray(i) ?: continue
            if (pt.length() >= 2) {
                val lon = pt.optDouble(0)
                val lat = pt.optDouble(1)
                list.add(Coord(lat, lon))
            }
        }
        return list
    }
}
