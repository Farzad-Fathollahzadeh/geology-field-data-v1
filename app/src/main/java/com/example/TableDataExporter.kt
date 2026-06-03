package com.example

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object TableDataExporter {

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\n", " ").replace("\r", "")
        if (clean.contains(",") || clean.contains("\"")) {
            return "\"" + clean.replace("\"", "\"\"") + "\""
        }
        return clean
    }

    fun exportToAllCvsZip(
        context: Context,
        stations: List<Station>,
        dbHelper: DatabaseHelper,
        boreholes: List<Borehole>,
        calculations: List<GeotechCalc>
    ): File? {
        val cacheDir = context.cacheDir
        val tempFolder = File(cacheDir, "GeoFieldExport_${System.currentTimeMillis()}")
        if (!tempFolder.exists()) tempFolder.mkdirs()

        // 1. Export Stations Sheet
        val stationsFile = File(tempFolder, "01_Geological_Stations.csv")
        PrintWriter(FileOutputStream(stationsFile)).use { pw ->
            pw.println("Station ID,Station Code,Title/Locality,Datetime,Latitude,Longitude,UTM Zone,Lithology Type,Estimated Rock Strength,Geologist Field Notes")
            stations.forEach { st ->
                pw.println("${st.id},${escapeCsv(st.stationCode)},${escapeCsv(st.title)},${st.datetime},${st.latitude},${st.longitude},${st.utmZone},${escapeCsv(st.lithologyType)},${escapeCsv(st.rockStrength)},${escapeCsv(st.notes)}")
            }
        }

        // 2. Export Discontinuities Observations Sheet
        val obsFile = File(tempFolder, "02_Joint_Discontinuities.csv")
        PrintWriter(FileOutputStream(obsFile)).use { pw ->
            pw.println("Observation ID,Parent Station Code,Discontinuity Strike (RHR),Dip Angle,Dip Direction,Discontinuity Set/Type,Joint Spacing (m),Surface Roughness,Discontinuity Alteration,Groundwater Class,Field Notes/Observations")
            stations.forEach { st ->
                val obsList = dbHelper.getObservationsForStation(st.stationCode)
                obsList.forEach { ob ->
                    pw.println("${ob.id},${escapeCsv(ob.stationCode)},${ob.strike ?: ""},${ob.dip ?: ""},${ob.dipDirection ?: ""},${escapeCsv(ob.jointSet)},${ob.jointSpacing},${escapeCsv(ob.jointRoughness)},${escapeCsv(ob.jointAlteration)},${escapeCsv(ob.waterClass)},${escapeCsv(ob.notes)}")
                }
            }
        }

        // 3. Export Boreholes Overview
        val bhFile = File(tempFolder, "03_Borehole_Metadata.csv")
        PrintWriter(FileOutputStream(bhFile)).use { pw ->
            pw.println("Borehole ID,Borehole Name,Exploration Area,Log Datetime,GPS Latitude,GPS Longitude,Current Logged Depth (m),Exploration comments")
            boreholes.forEach { bh ->
                pw.println("${bh.id},${escapeCsv(bh.name)},${escapeCsv(bh.area)},${bh.datetime},${bh.latitude},${bh.longitude},${bh.depth},${escapeCsv(bh.notes)}")
            }
        }

        // 4. Export Boreholes Lithology Segments
        val bhLogsFile = File(tempFolder, "04_Boreholes_Stratigraphy.csv")
        PrintWriter(FileOutputStream(bhLogsFile)).use { pw ->
            pw.println("Interval Segment ID,Borehole ID,Borehole Name,Depth From (m),Depth To (m),Lithological Rock Unit,Total Core Recovery (TCR %),Rock Quality Designation (RQD %),Weathering Grade,Geotechnical Comments")
            boreholes.forEach { bh ->
                val logsList = dbHelper.getLogsForBorehole(bh.id)
                logsList.forEach { log ->
                    pw.println("${log.id},${bh.id},${escapeCsv(bh.name)},${log.depthFrom},${log.depthTo},${escapeCsv(log.rockType)},${log.coreRecoveryPct},${log.rqdPct},${escapeCsv(log.weatherClass)},${escapeCsv(log.notes)}")
                }
            }
        }

        // 5. Export Geomechanical Calculations
        val calcsFile = File(tempFolder, "05_Geomechanical_Suite_Calculations.csv")
        PrintWriter(FileOutputStream(calcsFile)).use { pw ->
            pw.println("Calculation Record ID,Calculation Suite Method,Timestamp,Configuration Inputs,Mathematical Output Results,General Engineering Comments")
            calculations.forEach { calc ->
                pw.println("${calc.id},${escapeCsv(calc.calcType)},${calc.datetime},${escapeCsv(calc.inputParams)},${escapeCsv(calc.resultsJson)},${escapeCsv(calc.notes)}")
            }
        }

        // Bundle into ZIP Archive
        val destZipFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "GeoFieldPro_GeotechExport_${System.currentTimeMillis() / 1000}.zip")
        ZipOutputStream(FileOutputStream(destZipFile)).use { zos ->
            val filesToZip = listOf(stationsFile, obsFile, bhFile, bhLogsFile, calcsFile)
            filesToZip.forEach { file ->
                file.inputStream().use { fis ->
                    zos.putNextEntry(ZipEntry(file.name))
                    val buffer = ByteArray(4096)
                    var len: Int
                    while (fis.read(buffer).also { len = it } > 0) {
                        zos.write(buffer, 0, len)
                    }
                    zos.closeEntry()
                }
            }
        }

        // Clean up temp folder directory
        tempFolder.deleteRecursively()

        return if (destZipFile.exists()) destZipFile else null
    }
}
