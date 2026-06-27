package com.example

import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

object KmlParser {

    /**
     * Parses a KMZ (Zip containing KML) or KML input stream and extracts features.
     */
    fun parse(inputStream: InputStream, isKmz: Boolean): List<ImportedFeature> {
        return try {
            val kmlText = if (isKmz) {
                extractKmlFromKmz(inputStream)
            } else {
                inputStream.bufferedReader().use { it.readText() }
            }

            if (!kmlText.isNullOrBlank()) {
                parseKmlText(kmlText)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun extractKmlFromKmz(inputStream: InputStream): String? {
        try {
            val zipInput = ZipInputStream(inputStream)
            var entry = zipInput.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".kml", ignoreCase = true)) {
                    return zipInput.bufferedReader().use { it.readText() }
                }
                entry = zipInput.nextEntry
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseKmlText(kmlText: String): List<ImportedFeature> {
        val features = mutableListOf<ImportedFeature>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val builder = factory.newDocumentBuilder()
            
            // Handle cases with byte order marks or minor xml headers safely
            val cleanedXml = kmlText.trim()
            val doc = builder.parse(cleanedXml.byteInputStream())

            val placemarks = doc.getElementsByTagName("Placemark")
            for (i in 0 until placemarks.length) {
                val placemark = placemarks.item(i) as? Element ?: continue
                
                val name = placemark.getElementsByTagName("name").item(0)?.textContent ?: "Layer ${features.size + 1}"
                val description = placemark.getElementsByTagName("description").item(0)?.textContent ?: ""

                var type = "Point"
                var coordString = ""

                val pointList = placemark.getElementsByTagName("Point")
                val lineList = placemark.getElementsByTagName("LineString")
                val polyList = placemark.getElementsByTagName("Polygon")

                if (polyList.length > 0) {
                    type = "Polygon"
                    val polyElem = polyList.item(0) as Element
                    val coords = polyElem.getElementsByTagName("coordinates")
                    if (coords.length > 0) {
                        coordString = coords.item(0).textContent
                    }
                } else if (lineList.length > 0) {
                    type = "LineString"
                    val lineElem = lineList.item(0) as Element
                    val coords = lineElem.getElementsByTagName("coordinates")
                    if (coords.length > 0) {
                        coordString = coords.item(0).textContent
                    }
                } else if (pointList.length > 0) {
                    type = "Point"
                    val pointElem = pointList.item(0) as Element
                    val coords = pointElem.getElementsByTagName("coordinates")
                    if (coords.length > 0) {
                        coordString = coords.item(0).textContent
                    }
                }

                if (coordString.isNotBlank()) {
                    val coordinates = parseCoordinates(coordString)
                    if (coordinates.isNotEmpty()) {
                        // High-contrast neon/bright color palette for geological/military visibility
                        val palette = listOf(
                            "#FF1744", // Neon Red
                            "#00E676", // Neon Green
                            "#29B6F6", // Bright Blue
                            "#FFEA00", // Bright Yellow
                            "#D500F9", // Neon Purple
                            "#FF9100", // Bright Orange
                            "#00E5FF"  // Electric Cyan
                        )
                        val colorHex = palette[features.size % palette.size]

                        features.add(
                            ImportedFeature(
                                id = "kml_${features.size}_${System.currentTimeMillis()}",
                                name = name.trim(),
                                description = description.trim(),
                                type = type,
                                coordinates = coordinates,
                                colorHex = colorHex
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return features
    }

    private fun parseCoordinates(coordsStr: String): List<Coord> {
        val result = mutableListOf<Coord>()
        // Split by any sequence of whitespace
        val tokens = coordsStr.trim().split(Regex("\\s+"))
        for (token in tokens) {
            val parts = token.split(",")
            if (parts.size >= 2) {
                // KML coordinates are: longitude, latitude, altitude
                val lon = parts[0].trim().toDoubleOrNull()
                val lat = parts[1].trim().toDoubleOrNull()
                if (lat != null && lon != null) {
                    result.add(Coord(lat, lon))
                }
            }
        }
        return result
    }
}
