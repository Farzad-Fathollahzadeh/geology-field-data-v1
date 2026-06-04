package com.example

import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object XlsxWriter {

    data class StationSheetData(
        val stationCode: String,
        val prefix: String,
        val latitude: Double,
        val longitude: Double,
        val elevation: Double,
        val accuracy: Double,
        val utmEasting: Double,
        val utmNorthing: Double,
        val utmZone: String,
        val createdAt: String
    )

    data class LithologySheetData(
        val stationCode: String,
        val timestamp: String,
        val lithologyType: String,
        val texture: String,
        val color: String,
        val grainSize: String,
        val alteration: String,
        val mineralization: String,
        val notes: String,
        val hasSample: String,
        val photos: String
    )

    data class AlterationSheetData(
        val stationCode: String,
        val timestamp: String,
        val alterationType: String,
        val intensity: String,
        val hostRock: String,
        val associatedMinerals: String,
        val notes: String,
        val hasSample: String,
        val photos: String
    )

    data class FaultSheetData(
        val stationCode: String,
        val timestamp: String,
        val structureType: String,
        val strike: Int?,
        val dip: Int?,
        val dipDirection: Int?,
        val rake: Int?,
        val rakeDirection: String?,
        val movement: String,
        val width: Double?,
        val notes: String,
        val hasSample: String,
        val photos: String
    )

    data class SampleSheetData(
        val stationCode: String,
        val observationType: String,
        val sampleId: String?,
        val sampleType: String,
        val description: String
    )

    fun generateXlsxFile(
        file: File,
        stations: List<StationSheetData>,
        lithology: List<LithologySheetData>,
        alteration: List<AlterationSheetData>,
        faults: List<FaultSheetData>,
        samples: List<SampleSheetData>
    ) {
        val staticFiles = mapOf(
            "_rels/.rels" to """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
            """.trimIndent(),

            "[Content_Types].xml" to """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    <Override PartName="/xl/worksheets/sheet5.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
            """.trimIndent(),

            "xl/workbook.xml" to """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                    <sheets>
                        <sheet name="Stations" sheetId="1" r:id="rId1"/>
                        <sheet name="Lithology" sheetId="2" r:id="rId2"/>
                        <sheet name="Alteration" sheetId="3" r:id="rId3"/>
                        <sheet name="Structures" sheetId="4" r:id="rId4"/>
                        <sheet name="Samples" sheetId="5" r:id="rId5"/>
                    </sheets>
                </workbook>
            """.trimIndent(),

            "xl/_rels/workbook.xml.rels" to """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
                    <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
                    <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
                    <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet5.xml"/>
                </Relationships>
            """.trimIndent()
        )

        // Compile XML representations of datasets maps
        val stationsHeaders = listOf(
            "Station Code", "Prefix", "Latitude", "Longitude", "Elevation (m)",
            "Accuracy (m)", "UTM Easting", "UTM Northing", "UTM Zone", "Created At"
        )
        val stationsRows = stations.map {
            listOf(it.stationCode, it.prefix, it.latitude, it.longitude, it.elevation, it.accuracy, it.utmEasting, it.utmNorthing, it.utmZone, it.createdAt)
        }
        val stationsXml = buildSheetXml(stationsHeaders, stationsRows)

        val lithologyHeaders = listOf(
            "Station Code", "Timestamp", "Lithology Type", "Texture", "Color",
            "Grain Size", "Alteration", "Mineralization", "Notes", "Has Sample?", "Photos"
        )
        val lithologyRows = lithology.map {
            listOf(it.stationCode, it.timestamp, it.lithologyType, it.texture, it.color, it.grainSize, it.alteration, it.mineralization, it.notes, it.hasSample, it.photos)
        }
        val lithologyXml = buildSheetXml(lithologyHeaders, lithologyRows)

        val alterationHeaders = listOf(
            "Station Code", "Timestamp", "Alteration Type", "Intensity", "Host Rock",
            "Associated Minerals", "Notes", "Has Sample?", "Photos"
        )
        val alterationRows = alteration.map {
            listOf(it.stationCode, it.timestamp, it.alterationType, it.intensity, it.hostRock, it.associatedMinerals, it.notes, it.hasSample, it.photos)
        }
        val alterationXml = buildSheetXml(alterationHeaders, alterationRows)

        val faultsHeaders = listOf(
            "Station Code", "Timestamp", "Structure Type", "Strike (deg)", "Dip (deg)",
            "Dip Direction (deg)", "Rake (deg)", "Rake Direction", "Movement", "Width (cm)", "Notes", "Has Sample?", "Photos"
        )
        val faultsRows = faults.map {
            listOf(it.stationCode, it.timestamp, it.structureType, it.strike, it.dip, it.dipDirection, it.rake, it.rakeDirection, it.movement, it.width, it.notes, it.hasSample, it.photos)
        }
        val faultsXml = buildSheetXml(faultsHeaders, faultsRows)

        val samplesHeaders = listOf(
            "Station Code", "Observation Type", "Sample ID", "Sample Type", "Description"
        )
        val samplesRows = samples.map {
            listOf(it.stationCode, it.observationType, it.sampleId, it.sampleType, it.description)
        }
        val samplesXml = buildSheetXml(samplesHeaders, samplesRows)

        // Write OpenXML Zip entries using solid standard ZipOutputStream
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // Static definitions write loop
            for ((entryName, entryContent) in staticFiles) {
                zos.putNextEntry(ZipEntry(entryName))
                zos.write(entryContent.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }

            // Sheets dynamic streams
            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zos.write(stationsXml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml"))
            zos.write(lithologyXml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/worksheets/sheet3.xml"))
            zos.write(alterationXml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/worksheets/sheet4.xml"))
            zos.write(faultsXml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/worksheets/sheet5.xml"))
            zos.write(samplesXml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }
    }

    private fun getExcelColumnName(colIndex: Int): String {
        return ('A'.toInt() + colIndex).toChar().toString()
    }

    private fun escape(value: Any?): String {
        if (value == null) return ""
        val str = value.toString()
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun buildSheetXml(headers: List<String>, rows: List<List<Any?>>): String {
        val xml = StringBuilder()
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        xml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
        xml.append("<sheetData>\n")

        // 1. Header row
        xml.append("<row r=\"1\">\n")
        for ((colIdx, header) in headers.withIndex()) {
            val cellRef = "${getExcelColumnName(colIdx)}1"
            xml.append("  <c r=\"$cellRef\" t=\"inlineStr\"><is><t>${escape(header)}</t></is></c>\n")
        }
        xml.append("</row>\n")

        // 2. Data rows
        for ((rowIdx, rowData) in rows.withIndex()) {
            val excelRowIdx = rowIdx + 2
            xml.append("<row r=\"$excelRowIdx\">\n")
            for ((colIdx, cellValue) in rowData.withIndex()) {
                val cellRef = "${getExcelColumnName(colIdx)}$excelRowIdx"
                if (cellValue is Number) {
                    xml.append("  <c r=\"$cellRef\" t=\"n\"><v>$cellValue</v></c>\n")
                } else {
                    xml.append("  <c r=\"$cellRef\" t=\"inlineStr\"><is><t>${escape(cellValue)}</t></is></c>\n")
                }
            }
            xml.append("</row>\n")
        }

        xml.append("</sheetData>\n")
        xml.append("</worksheet>")
        return xml.toString()
    }
}
