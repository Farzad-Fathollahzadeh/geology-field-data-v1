package com.example

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

@Serializable
data class Station(
    val id: Long = 0,
    val stationCode: String,
    val title: String,
    val datetime: String,
    val latitude: Double,
    val longitude: Double,
    val utmZone: String = "",
    val lithologyType: String = "",
    val rockStrength: String = "",
    val notes: String = ""
)

@Serializable
data class Observation(
    val id: Long = 0,
    val stationCode: String,
    val strike: Int?,
    val dip: Int?,
    val dipDirection: Int?,
    val jointSet: String = "",
    val jointSpacing: Double = 0.0,
    val jointRoughness: String = "",
    val jointAlteration: String = "",
    val waterClass: String = "",
    val notes: String = ""
)

@Serializable
data class Borehole(
    val id: Long = 0,
    val name: String,
    val area: String,
    val datetime: String,
    val latitude: Double,
    val longitude: Double,
    val depth: Double = 0.0,
    val notes: String = ""
)

@Serializable
data class BoreholeLog(
    val id: Long = 0,
    val boreholeId: Long,
    val depthFrom: Double,
    val depthTo: Double,
    val rockType: String,
    val coreRecoveryPct: Double,
    val rqdPct: Double,
    val weatherClass: String,
    val notes: String = ""
)

@Serializable
data class GeotechCalc(
    val id: Long = 0,
    val calcType: String, // RMR, Q, Hoek-Brown, RMI, MRMR
    val datetime: String,
    val inputParams: String,
    val resultsJson: String,
    val notes: String = ""
)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "geofield_pro.db"
        const val DATABASE_VERSION = 2

        // Table Stations
        const val TABLE_STATIONS = "stations"
        const val COL_ST_ID = "id"
        const val COL_ST_CODE = "station_code"
        const val COL_ST_TITLE = "title"
        const val COL_ST_DATE = "datetime"
        const val COL_ST_LAT = "latitude"
        const val COL_ST_LON = "longitude"
        const val COL_ST_UTM = "utm_zone"
        const val COL_ST_LITH = "lithology_type"
        const val COL_ST_STRENGTH = "rock_strength"
        const val COL_ST_NOTES = "notes"

        // Table Observations
        const val TABLE_OBSERVATIONS = "observations"
        const val COL_OBS_ID = "id"
        const val COL_OBS_CODE = "station_code"
        const val COL_OBS_STRIKE = "strike"
        const val COL_OBS_DIP = "dip"
        const val COL_OBS_DIP_DIR = "dip_direction"
        const val COL_OBS_SET = "joint_set"
        const val COL_OBS_SPACING = "joint_spacing"
        const val COL_OBS_ROUGH = "joint_roughness"
        const val COL_OBS_ALT = "joint_alteration"
        const val COL_OBS_WATER = "water_class"
        const val COL_OBS_NOTES = "notes"

        // Table Boreholes
        const val TABLE_BOREHOLES = "boreholes"
        const val COL_BH_ID = "id"
        const val COL_BH_NAME = "name"
        const val COL_BH_AREA = "area"
        const val COL_BH_DATE = "datetime"
        const val COL_BH_LAT = "latitude"
        const val COL_BH_LON = "longitude"
        const val COL_BH_DEPTH = "depth"
        const val COL_BH_NOTES = "notes"

        // Table Borehole Logs
        const val TABLE_BH_LOGS = "borehole_logs"
        const val COL_LOG_ID = "id"
        const val COL_LOG_BH_ID = "borehole_id"
        const val COL_LOG_FROM = "depth_from"
        const val COL_LOG_TO = "depth_to"
        const val COL_LOG_TYPE = "rock_type"
        const val COL_LOG_RECOVERY = "core_recovery_pct"
        const val COL_LOG_RQD = "rqd_pct"
        const val COL_LOG_WEATHER = "weather_class"
        const val COL_LOG_NOTES = "notes"

        // Table Dynamic Calculations Saved
        const val TABLE_CALCS = "calculations"
        const val COL_CALC_ID = "id"
        const val COL_CALC_TYPE = "calc_type"
        const val COL_CALC_DATE = "datetime"
        const val COL_CALC_INPUT = "input_params"
        const val COL_CALC_RESULTS = "results_json"
        const val COL_CALC_NOTES = "notes"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createStations = """
            CREATE TABLE $TABLE_STATIONS (
                $COL_ST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ST_CODE TEXT UNIQUE,
                $COL_ST_TITLE TEXT,
                $COL_ST_DATE TEXT,
                $COL_ST_LAT REAL,
                $COL_ST_LON REAL,
                $COL_ST_UTM TEXT,
                $COL_ST_LITH TEXT,
                $COL_ST_STRENGTH TEXT,
                $COL_ST_NOTES TEXT
            )
        """.trimIndent()

        val createObservations = """
            CREATE TABLE $TABLE_OBSERVATIONS (
                $COL_OBS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_OBS_CODE TEXT,
                $COL_OBS_STRIKE INTEGER,
                $COL_OBS_DIP INTEGER,
                $COL_OBS_DIP_DIR INTEGER,
                $COL_OBS_SET TEXT,
                $COL_OBS_SPACING REAL,
                $COL_OBS_ROUGH TEXT,
                $COL_OBS_ALT TEXT,
                $COL_OBS_WATER TEXT,
                $COL_OBS_NOTES TEXT
            )
        """.trimIndent()

        val createBoreholes = """
            CREATE TABLE $TABLE_BOREHOLES (
                $COL_BH_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BH_NAME TEXT,
                $COL_BH_AREA TEXT,
                $COL_BH_DATE TEXT,
                $COL_BH_LAT REAL,
                $COL_BH_LON REAL,
                $COL_BH_DEPTH REAL,
                $COL_BH_NOTES TEXT
            )
        """.trimIndent()

        val createBoreholeLogs = """
            CREATE TABLE $TABLE_BH_LOGS (
                $COL_LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LOG_BH_ID INTEGER,
                $COL_LOG_FROM REAL,
                $COL_LOG_TO REAL,
                $COL_LOG_TYPE TEXT,
                $COL_LOG_RECOVERY REAL,
                $COL_LOG_RQD REAL,
                $COL_LOG_WEATHER TEXT,
                $COL_LOG_NOTES TEXT
            )
        """.trimIndent()

        val createCalcs = """
            CREATE TABLE $TABLE_CALCS (
                $COL_CALC_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CALC_TYPE TEXT,
                $COL_CALC_DATE TEXT,
                $COL_CALC_INPUT TEXT,
                $COL_CALC_RESULTS TEXT,
                $COL_CALC_NOTES TEXT
            )
        """.trimIndent()

        db.execSQL(createStations)
        db.execSQL(createObservations)
        db.execSQL(createBoreholes)
        db.execSQL(createBoreholeLogs)
        db.execSQL(createCalcs)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_OBSERVATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOREHOLES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BH_LOGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CALCS")
        onCreate(db)
    }

    // --- Station functions ---
    fun insertStation(station: Station): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_ST_CODE, station.stationCode)
            put(COL_ST_TITLE, station.title)
            put(COL_ST_DATE, station.datetime)
            put(COL_ST_LAT, station.latitude)
            put(COL_ST_LON, station.longitude)
            put(COL_ST_UTM, station.utmZone)
            put(COL_ST_LITH, station.lithologyType)
            put(COL_ST_STRENGTH, station.rockStrength)
            put(COL_ST_NOTES, station.notes)
        }
        return db.insertWithOnConflict(TABLE_STATIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllStations(): List<Station> {
        val list = mutableListOf<Station>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_STATIONS ORDER BY $COL_ST_ID DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Station(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ST_ID)),
                        stationCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_ST_CODE)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(COL_ST_TITLE)),
                        datetime = cursor.getString(cursor.getColumnIndexOrThrow(COL_ST_DATE)),
                        latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ST_LAT)),
                        longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ST_LON)),
                        utmZone = cursor.getString(cursor.getColumnIndexOrThrow(COL_ST_UTM)),
                        lithologyType = cursor.getString(cursor.getColumnIndexOrThrow(COL_ST_LITH)),
                        rockStrength = cursor.getString(cursor.getColumnIndexOrThrow(COL_ST_STRENGTH)),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_ST_NOTES))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteStation(stationCode: String): Int {
        val db = writableDatabase
        db.delete(TABLE_OBSERVATIONS, "$COL_OBS_CODE = ?", arrayOf(stationCode))
        return db.delete(TABLE_STATIONS, "$COL_ST_CODE = ?", arrayOf(stationCode))
    }

    // --- Observation functions ---
    fun insertObservation(obs: Observation): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_OBS_CODE, obs.stationCode)
            if (obs.strike != null) put(COL_OBS_STRIKE, obs.strike) else putNull(COL_OBS_STRIKE)
            if (obs.dip != null) put(COL_OBS_DIP, obs.dip) else putNull(COL_OBS_DIP)
            if (obs.dipDirection != null) put(COL_OBS_DIP_DIR, obs.dipDirection) else putNull(COL_OBS_DIP_DIR)
            put(COL_OBS_SET, obs.jointSet)
            put(COL_OBS_SPACING, obs.jointSpacing)
            put(COL_OBS_ROUGH, obs.jointRoughness)
            put(COL_OBS_ALT, obs.jointAlteration)
            put(COL_OBS_WATER, obs.waterClass)
            put(COL_OBS_NOTES, obs.notes)
        }
        return db.insert(TABLE_OBSERVATIONS, null, cv)
    }

    fun getObservationsForStation(stationCode: String): List<Observation> {
        val list = mutableListOf<Observation>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_OBSERVATIONS WHERE $COL_OBS_CODE = ?", arrayOf(stationCode))
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Observation(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_OBS_ID)),
                        stationCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_OBS_CODE)),
                        strike = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_OBS_STRIKE))) null else cursor.getInt(cursor.getColumnIndexOrThrow(COL_OBS_STRIKE)),
                        dip = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_OBS_DIP))) null else cursor.getInt(cursor.getColumnIndexOrThrow(COL_OBS_DIP)),
                        dipDirection = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_OBS_DIP_DIR))) null else cursor.getInt(cursor.getColumnIndexOrThrow(COL_OBS_DIP_DIR)),
                        jointSet = cursor.getString(cursor.getColumnIndexOrThrow(COL_OBS_SET)),
                        jointSpacing = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_OBS_SPACING)),
                        jointRoughness = cursor.getString(cursor.getColumnIndexOrThrow(COL_OBS_ROUGH)),
                        jointAlteration = cursor.getString(cursor.getColumnIndexOrThrow(COL_OBS_ALT)),
                        waterClass = cursor.getString(cursor.getColumnIndexOrThrow(COL_OBS_WATER)),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_OBS_NOTES))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteObservation(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_OBSERVATIONS, "$COL_OBS_ID = ?", arrayOf(id.toString()))
    }

    // --- Borehole functions ---
    fun insertBorehole(bh: Borehole): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_BH_NAME, bh.name)
            put(COL_BH_AREA, bh.area)
            put(COL_BH_DATE, bh.datetime)
            put(COL_BH_LAT, bh.latitude)
            put(COL_BH_LON, bh.longitude)
            put(COL_BH_DEPTH, bh.depth)
            put(COL_BH_NOTES, bh.notes)
        }
        return db.insert(TABLE_BOREHOLES, null, cv)
    }

    fun getAllBoreholes(): List<Borehole> {
        val list = mutableListOf<Borehole>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_BOREHOLES ORDER BY $COL_BH_ID DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Borehole(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_BH_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COL_BH_NAME)),
                        area = cursor.getString(cursor.getColumnIndexOrThrow(COL_BH_AREA)),
                        datetime = cursor.getString(cursor.getColumnIndexOrThrow(COL_BH_DATE)),
                        latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_BH_LAT)),
                        longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_BH_LON)),
                        depth = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_BH_DEPTH)),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_BH_NOTES))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteBorehole(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_BH_LOGS, "$COL_LOG_BH_ID = ?", arrayOf(id.toString()))
        db.delete(TABLE_BOREHOLES, "$COL_BH_ID = ?", arrayOf(id.toString()))
    }

    // --- Borehole Logs functions ---
    fun insertBoreholeLog(log: BoreholeLog): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_LOG_BH_ID, log.boreholeId)
            put(COL_LOG_FROM, log.depthFrom)
            put(COL_LOG_TO, log.depthTo)
            put(COL_LOG_TYPE, log.rockType)
            put(COL_LOG_RECOVERY, log.coreRecoveryPct)
            put(COL_LOG_RQD, log.rqdPct)
            put(COL_LOG_WEATHER, log.weatherClass)
            put(COL_LOG_NOTES, log.notes)
        }
        val id = db.insert(TABLE_BH_LOGS, null, cv)
        recomputeBoreholeTotalDepth(log.boreholeId)
        return id
    }

    fun getLogsForBorehole(boreholeId: Long): List<BoreholeLog> {
        val list = mutableListOf<BoreholeLog>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_BH_LOGS WHERE $COL_LOG_BH_ID = ? ORDER BY $COL_LOG_FROM ASC", arrayOf(boreholeId.toString()))
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    BoreholeLog(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_ID)),
                        boreholeId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_BH_ID)),
                        depthFrom = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LOG_FROM)),
                        depthTo = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LOG_TO)),
                        rockType = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_TYPE)),
                        coreRecoveryPct = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LOG_RECOVERY)),
                        rqdPct = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LOG_RQD)),
                        weatherClass = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_WEATHER)),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_NOTES))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteBoreholeLog(logId: Long, boreholeId: Long) {
        val db = writableDatabase
        db.delete(TABLE_BH_LOGS, "$COL_LOG_ID = ?", arrayOf(logId.toString()))
        recomputeBoreholeTotalDepth(boreholeId)
    }

    private fun recomputeBoreholeTotalDepth(boreholeId: Long) {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT MAX($COL_LOG_TO) FROM $TABLE_BH_LOGS WHERE $COL_LOG_BH_ID = ?", arrayOf(boreholeId.toString()))
        var maxDepth = 0.0
        if (cursor.moveToFirst()) {
            maxDepth = cursor.getDouble(0)
        }
        cursor.close()

        val cv = ContentValues().apply {
            put(COL_BH_DEPTH, maxDepth)
        }
        db.update(TABLE_BOREHOLES, cv, "$COL_BH_ID = ?", arrayOf(boreholeId.toString()))
    }

    // --- Calculation Functions ---
    fun saveCalculation(calc: GeotechCalc): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_CALC_TYPE, calc.calcType)
            put(COL_CALC_DATE, calc.datetime)
            put(COL_CALC_INPUT, calc.inputParams)
            put(COL_CALC_RESULTS, calc.resultsJson)
            put(COL_CALC_NOTES, calc.notes)
        }
        return db.insert(TABLE_CALCS, null, cv)
    }

    fun getAllCalculations(): List<GeotechCalc> {
        val list = mutableListOf<GeotechCalc>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CALCS ORDER BY $COL_CALC_ID DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    GeotechCalc(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CALC_ID)),
                        calcType = cursor.getString(cursor.getColumnIndexOrThrow(COL_CALC_TYPE)),
                        datetime = cursor.getString(cursor.getColumnIndexOrThrow(COL_CALC_DATE)),
                        inputParams = cursor.getString(cursor.getColumnIndexOrThrow(COL_CALC_INPUT)),
                        resultsJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_CALC_RESULTS)),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_CALC_NOTES))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteCalculation(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_CALCS, "$COL_CALC_ID = ?", arrayOf(id.toString()))
    }
}
