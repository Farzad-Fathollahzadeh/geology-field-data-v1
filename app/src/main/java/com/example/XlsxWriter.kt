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
        val movement: String?,
        val width: Double?,
        val notes: String?,
        val hasSample: String,
        val photos: String
    )

    data class SampleSheetData(
        val stationCode: String,
        val observationType: String,
        val sampleId: String,
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
        ZipOutputStream(FileOutputStream(file)).use { zipOut ->
            // 1. [Content_Types].xml
            zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
            val contentTypes = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/worksheets/sheet5.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
            """.trimIndent()
            zipOut.write(contentTypes.toByteArray(StandardCharsets.UTF_8))
            zipOut.closeEntry()

            // 2. _rels/.rels
            zipOut.putNextEntry(ZipEntry("_rels/.rels"))
            val rels = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
            """.trimIndent()
            zipOut.write(rels.toByteArray(StandardCharsets.UTF_8))
            zipOut.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zipOut.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            val workbookRels = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
                  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
                  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
                  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet5.xml"/>
                  <Relationship Id="rId6" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
            """.trimIndent()
            zipOut.write(workbookRels.toByteArray(StandardCharsets.UTF_8))
            zipOut.closeEntry()

            // 4. xl/workbook.xml
            zipOut.putNextEntry(ZipEntry("xl/workbook.xml"))
            val workbook = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                    <sheet name="Stations" sheetId="1" r:id="rId1"/>
                    <sheet name="Lithology" sheetId="2" r:id="rId2"/>
                    <sheet name="Alteration" sheetId="3" r:id="rId3"/>
                    <sheet name="Faults" sheetId="4" r:id="rId4"/>
                    <sheet name="Samples" sheetId="5" r:id="rId5"/>
                  </sheets>
                </workbook>
            """.trimIndent()
            zipOut.write(workbook.toByteArray(StandardCharsets.UTF_8))
            zipOut.closeEntry()

            // 5. xl/styles.xml
            zipOut.putNextEntry(ZipEntry("xl/styles.xml"))
            val styles = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
                  <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
                  <borders count="1"><border><left/><right/><top/><bottom/></border></borders>
                  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
                </styleSheet>
            """.trimIndent()
            zipOut.write(styles.toByteArray(StandardCharsets.UTF_8))
            zipOut.closeEntry()

            // 6. Worksheets
            writeSheet(
                zipOut,
                "xl/worksheets/sheet1.xml",
                listOf("Station Code", "Prefix", "Latitude", "Longitude", "Elevation (m)", "Accuracy (m)", "UTM Easting (m)", "UTM Northing (m)", "UTM Zone", "Created At"),
                stations.map { listOf(it.stationCode, it.prefix, it.latitude, it.longitude, it.elevation, it.accuracy, it.utmEasting, it.utmNorthing, it.utmZone, it.createdAt) }
            )

            writeSheet(
                zipOut,
                "xl/worksheets/sheet2.xml",
                listOf("Station Code", "Timestamp", "Lithology Type", "Texture", "Color", "Grain Size", "Alteration", "Mineralization", "Notes", "Has Sample?", "Photos"),
                lithology.map { listOf(it.stationCode, it.timestamp, it.lithologyType, it.texture, it.color, it.grainSize, it.alteration, it.mineralization, it.notes, it.hasSample, it.photos) }
            )

            writeSheet(
                zipOut,
                "xl/worksheets/sheet3.xml",
                listOf("Station Code", "Timestamp", "Alteration Type", "Intensity", "Host Rock", "Associated Minerals", "Notes", "Has Sample?", "Photos"),
                alteration.map { listOf(it.stationCode, it.timestamp, it.alterationType, it.intensity, it.hostRock, it.associatedMinerals, it.notes, it.hasSample, it.photos) }
            )

            writeSheet(
                zipOut,
                "xl/worksheets/sheet4.xml",
                listOf("Station Code", "Timestamp", "Structure Type", "Strike (°)", "Dip (°)", "Dip Direction (°)", "Rake (°)", "Rake Direction", "Movement", "Width (cm)", "Notes", "Has Sample?", "Photos"),
                faults.map { listOf(it.stationCode, it.timestamp, it.structureType, it.strike ?: "", it.dip ?: "", it.dipDirection ?: "", it.rake ?: "", it.rakeDirection ?: "", it.movement ?: "", it.width ?: "", it.notes ?: "", it.hasSample, it.photos) }
            )

            writeSheet(
                zipOut,
                "xl/worksheets/sheet5.xml",
                listOf("Station Code", "Observation Type", "Sample ID", "Sample Type", "Description"),
                samples.map { listOf(it.stationCode, it.observationType, it.sampleId, it.sampleType, it.description) }
            )
        }
    }

    private fun writeSheet(zipOut: ZipOutputStream, entryName: String, headers: List<String>, rows: List<List<Any?>>) {
        zipOut.putNextEntry(ZipEntry(entryName))
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
        sb.append("  <sheetData>\n")
        
        // Write headers
        if (headers.isNotEmpty()) {
            sb.append("    <row r=\"1\">\n")
            headers.forEachIndexed { colIndex, header ->
                val colLetter = getColumnLetter(colIndex)
                val escapedHeader = escapeXml(header)
                sb.append("      <c r=\"${colLetter}1\" t=\"inlineStr\"><is><t>$escapedHeader</t></is></c>\n")
            }
            sb.append("    </row>\n")
        }

        // Write rows
        rows.forEachIndexed { rowIndex, rowData ->
            val excelRowIndex = rowIndex + 2
            sb.append("    <row r=\"$excelRowIndex\">\n")
            rowData.forEachIndexed { colIndex, valObj ->
                if (valObj != null) {
                    val colLetter = getColumnLetter(colIndex)
                    val ref = "$colLetter$excelRowIndex"
                    
                    when (valObj) {
                        is Number -> {
                            sb.append("      <c r=\"$ref\"><v>${valObj.toDouble()}</v></c>\n")
                        }
                        is Boolean -> {
                            val valStr = if (valObj) "1" else "0"
                            sb.append("      <c r=\"$ref\" t=\"b\"><v>$valStr</v></c>\n")
                        }
                        else -> {
                            val escapedStr = escapeXml(valObj.toString())
                            sb.append("      <c r=\"$ref\" t=\"inlineStr\"><is><t>$escapedStr</t></is></c>\n")
                        }
                    }
                }
            }
            sb.append("    </row>\n")
        }
        
        sb.append("  </sheetData>\n")
        sb.append("</worksheet>")
        
        zipOut.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()
    }

    private fun getColumnLetter(colIndex: Int): String {
        var temp = colIndex
        var letter = ""
        while (temp >= 0) {
            letter = ('A'.code + (temp % 26)).toChar().toString() + letter
            temp = (temp / 26) - 1
        }
        return letter
    }

    private fun escapeXml(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
