package com.example

import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

object GpxParser {

    fun parse(inputStream: InputStream): List<ImportedFeature> {
        val features = mutableListOf<ImportedFeature>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                factory.isXIncludeAware = false
                factory.isExpandEntityReferences = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(inputStream)

            val palette = listOf(
                "#FF1744", // Neon Red
                "#00E676", // Neon Green
                "#29B6F6", // Bright Blue
                "#FFEA00", // Bright Yellow
                "#D500F9", // Neon Purple
                "#FF9100", // Bright Orange
                "#00E5FF"  // Electric Cyan
            )

            // 1. Parse tracks (<trk>)
            val trks = doc.getElementsByTagName("trk")
            for (i in 0 until trks.length) {
                val trk = trks.item(i) as? Element ?: continue
                val name = trk.getElementsByTagName("name").item(0)?.textContent ?: "GPX Track ${features.size + 1}"
                val description = trk.getElementsByTagName("desc").item(0)?.textContent ?: ""
                
                val trkpts = trk.getElementsByTagName("trkpt")
                val coordinates = mutableListOf<Coord>()
                for (j in 0 until trkpts.length) {
                    val trkpt = trkpts.item(j) as? Element ?: continue
                    val latAttr = trkpt.getAttribute("lat").toDoubleOrNull()
                    val lonAttr = trkpt.getAttribute("lon").toDoubleOrNull()
                    if (latAttr != null && lonAttr != null) {
                        coordinates.add(Coord(latAttr, lonAttr))
                    }
                }
                if (coordinates.isNotEmpty()) {
                    features.add(
                        ImportedFeature(
                            id = "gpx_trk_${features.size}_${System.currentTimeMillis()}",
                            name = name.trim(),
                            description = description.trim(),
                            type = "LineString",
                            coordinates = coordinates,
                            colorHex = palette[features.size % palette.size]
                        )
                    )
                }
            }

            // 2. Parse routes (<rte>)
            val rtes = doc.getElementsByTagName("rte")
            for (i in 0 until rtes.length) {
                val rte = rtes.item(i) as? Element ?: continue
                val name = rte.getElementsByTagName("name").item(0)?.textContent ?: "GPX Route ${features.size + 1}"
                val description = rte.getElementsByTagName("desc").item(0)?.textContent ?: ""
                
                val rtepts = rte.getElementsByTagName("rtept")
                val coordinates = mutableListOf<Coord>()
                for (j in 0 until rtepts.length) {
                    val rtept = rtepts.item(j) as? Element ?: continue
                    val latAttr = rtept.getAttribute("lat").toDoubleOrNull()
                    val lonAttr = rtept.getAttribute("lon").toDoubleOrNull()
                    if (latAttr != null && lonAttr != null) {
                        coordinates.add(Coord(latAttr, lonAttr))
                    }
                }
                if (coordinates.isNotEmpty()) {
                    features.add(
                        ImportedFeature(
                            id = "gpx_rte_${features.size}_${System.currentTimeMillis()}",
                            name = name.trim(),
                            description = description.trim(),
                            type = "LineString",
                            coordinates = coordinates,
                            colorHex = palette[features.size % palette.size]
                        )
                    )
                }
            }

            // 3. Parse waypoints (<wpt>)
            val wpts = doc.getElementsByTagName("wpt")
            for (i in 0 until wpts.length) {
                val wpt = wpts.item(i) as? Element ?: continue
                val name = wpt.getElementsByTagName("name").item(0)?.textContent ?: "Waypoint ${features.size + 1}"
                val description = wpt.getElementsByTagName("desc").item(0)?.textContent ?: ""
                val latAttr = wpt.getAttribute("lat").toDoubleOrNull()
                val lonAttr = wpt.getAttribute("lon").toDoubleOrNull()
                if (latAttr != null && lonAttr != null) {
                    features.add(
                        ImportedFeature(
                            id = "gpx_wpt_${features.size}_${System.currentTimeMillis()}",
                            name = name.trim(),
                            description = description.trim(),
                            type = "Point",
                            coordinates = listOf(Coord(latAttr, lonAttr)),
                            colorHex = palette[features.size % palette.size]
                        )
                    )
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return features
    }
}
