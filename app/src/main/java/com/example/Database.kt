package com.example

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// --- ENTITIES ---

@Entity(tableName = "window_mappings")
data class WindowMapping(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startX: Double = 0.0,
    val startY: Double = 0.0,
    val startZ: Double = 0.0,
    val endX: Double = 0.0,
    val endY: Double = 0.0,
    val endZ: Double = 0.0,
    val lithology: String = "",
    val discontinuityType: String = "",
    val dip: Int = 0,
    val dipDirection: Int = 0,
    val jointSetNumbers: String = "",
    val persistence: String = "",
    val termination: String = "",
    val spacing: Double = 0.0,
    val aperture: Double = 0.0,
    val surfaceShape: String = "",
    val surfaceRoughness: String = "",
    val natureOfFilling: String = "",
    val wavelength: Double = 0.0,
    val waveAmplitude: Double = 0.0,
    val jrc: Double = 0.0,
    val jcs: Double = 0.0,
    val weatheringCondition: String = "", // Weathering condition
    val waterFlow: String = "",
    val sampleNo: String = "",
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String?,
    val date: String,
    val utmZoneMode: String = "AUTO", // AUTO or MANUAL
    val utmZoneManual: String = "39N"
)

@Entity(
    tableName = "stations",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Station(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val stationCode: String, // e.g. LITH-001
    val prefix: String,      // e.g. LITH
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val accuracy: Double,
    val elevationAccuracy: Double = 0.0,
    val utmEasting: Double,
    val utmNorthing: Double,
    val utmZone: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "observations",
    foreignKeys = [
        ForeignKey(
            entity = Station::class,
            parentColumns = ["id"],
            childColumns = ["stationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Observation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val stationId: Int,
    val observationType: String, // Lithology, Alteration, Fault, Joint, Vein, Structure, Mineralization, Sample
    val photoPath: String?,
    val notes: String?,
    val createdAt: Long = System.currentTimeMillis(),

    // Lithology-specific fields
    val lithologyType: String? = null,
    val texture: String? = null,
    val color: String? = null,
    val grainSize: String? = null,
    val lithologyAlteration: String? = null,
    val lithologyMineralization: String? = null,

    // Alteration-specific fields
    val alterationType: String? = null,
    val intensity: String? = null,
    val hostRock: String? = null,
    val associatedMinerals: String? = null,

    // Fault-specific fields
    val faultType: String? = null,
    val strike: Int? = null,
    val dip: Int? = null,
    val dipDirection: Int? = null,
    val rake: Int? = null,
    val rakeDirection: String? = null,
    val movement: String? = null,
    val width: Double? = null, // Width in cm/meters

    // Joint-specific fields
    val jointType: String? = null,
    val jointSpacing: Double? = null,

    // Vein-specific fields
    val veinType: String? = null,
    val veinMineralogy: String? = null,

    // Structure-specific fields
    val structureType: String? = null,

    // Mineralization-specific fields
    val minStyle: String? = null,
    val minAssociatedMinerals: String? = null,
    val minGradeEstimate: String? = null
)

@Entity(
    tableName = "samples",
    foreignKeys = [
        ForeignKey(
            entity = Observation::class,
            parentColumns = ["id"],
            childColumns = ["observationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Sample(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val observationId: Int,
    val sampleCodeId: String, // e.g. DZ-014-A
    val sampleType: String,   // Hand specimen, Core, Soil, Stream sediment, etc.
    val description: String?,
    val photoPath: String?
)

@Entity(tableName = "boreholes")
data class Borehole(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val accuracy: Double,
    val elevationAccuracy: Double = 0.0,
    val utmEasting: Double,
    val utmNorthing: Double,
    val utmZone: String,
    val startDate: String,
    val azimuth: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "daily_drilling_logs",
    foreignKeys = [
        ForeignKey(
            entity = Borehole::class,
            parentColumns = ["id"],
            childColumns = ["boreholeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DailyDrillingLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val boreholeId: Int,
    val date: String,
    val depthDrilled: Double, // meters/day
    val lithologySummary: String,
    val operationalNotes: String,
    val comments: String
)

@Entity(
    tableName = "core_runs",
    foreignKeys = [
        ForeignKey(
            entity = Borehole::class,
            parentColumns = ["id"],
            childColumns = ["boreholeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CoreRun(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val boreholeId: Int,
    val fromDepth: Double,
    val toDepth: Double,
    val piecesOver10Cm: Double,
    val recoveredLength: Double,
    
    // Geology & Properties
    val rockType: String,
    val lithologyDescription: String,
    val alterationType: String, // propylitic / argillic / silicic / potassic / phyllic
    val alterationIntensity: String,
    val mineralizationType: String,
    val mineralizationIntensity: String,
    val fractureFrequency: Double? = null,
    val veinDensity: Double? = null,
    val jointPresence: String? = null,
    val color: String,
    val texture: String,
    val grainSize: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getRunLength(): Double = toDepth - fromDepth
    
    fun getRqd(): Double {
        val len = getRunLength()
        return if (len > 0.0) {
            (piecesOver10Cm / len) * 100.0
        } else 0.0
    }

    fun getCoreRecovery(): Double {
        val len = getRunLength()
        return if (len > 0.0) {
            (recoveredLength / len) * 100.0
        } else 0.0
    }
}


// --- DAO ---

@Dao
interface GeologyDao {
    
    // Projects
    @Query("SELECT * FROM projects ORDER BY id DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Int): Flow<Project?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)

    // Stations
    @Query("SELECT * FROM stations WHERE projectId = :projectId ORDER BY id ASC")
    fun getStationsForProject(projectId: Int): Flow<List<Station>>

    @Query("SELECT * FROM stations WHERE id = :stationId")
    fun getStationById(stationId: Int): Flow<Station?>

    @Query("SELECT * FROM stations WHERE id = :stationId")
    suspend fun getStationByIdOnce(stationId: Int): Station?

    @Query("SELECT COUNT(*) FROM stations WHERE projectId = :projectId AND prefix = :prefix")
    suspend fun getStationCountForPrefix(projectId: Int, prefix: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: Station): Long

    @Query("DELETE FROM stations WHERE id = :id")
    suspend fun deleteStationById(id: Int)

    // Observations
    @Query("SELECT * FROM observations WHERE stationId = :stationId ORDER BY id DESC")
    fun getObservationsForStation(stationId: Int): Flow<List<Observation>>

    @Query("SELECT * FROM observations WHERE id = :observationId")
    suspend fun getObservationById(observationId: Int): Observation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(observation: Observation): Long

    @Query("DELETE FROM observations WHERE id = :id")
    suspend fun deleteObservationById(id: Int)

    // Samples
    @Query("SELECT * FROM samples WHERE observationId = :observationId LIMIT 1")
    fun getSampleForObservation(observationId: Int): Flow<Sample?>

    @Query("SELECT * FROM samples WHERE observationId = :observationId LIMIT 1")
    suspend fun getSampleForObservationSync(observationId: Int): Sample?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: Sample): Long

    @Query("SELECT * FROM samples WHERE observationId IN (SELECT id FROM observations WHERE stationId = :stationId)")
    fun getSamplesForStation(stationId: Int): Flow<List<Sample>>

    // Export query bindings (Sync flows/lists of everything for a Project)
    @Query("SELECT * FROM stations WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun getStationsForProjectSync(projectId: Int): List<Station>

    @Query("SELECT * FROM observations WHERE stationId IN (SELECT id FROM stations WHERE projectId = :projectId) ORDER BY id ASC")
    suspend fun getObservationsForProjectSync(projectId: Int): List<Observation>

    @Query("SELECT * FROM samples WHERE observationId IN (SELECT id FROM observations WHERE stationId IN (SELECT id FROM stations WHERE projectId = :projectId)) ORDER BY id ASC")
    suspend fun getSamplesForProjectSync(projectId: Int): List<Sample>

    // --- Boreholes DAO Functions ---
    @Query("SELECT * FROM boreholes ORDER BY id DESC")
    fun getAllBoreholes(): Flow<List<Borehole>>

    @Query("SELECT * FROM boreholes WHERE id = :id")
    fun getBoreholeById(id: Int): Flow<Borehole?>

    @Query("SELECT * FROM boreholes WHERE id = :id")
    suspend fun getBoreholeByIdSync(id: Int): Borehole?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBorehole(borehole: Borehole): Long

    @Query("DELETE FROM boreholes WHERE id = :id")
    suspend fun deleteBoreholeById(id: Int)

    // --- Daily Drilling Logs ---
    @Query("SELECT * FROM daily_drilling_logs WHERE boreholeId = :boreholeId ORDER BY date DESC")
    fun getDailyLogsForBorehole(boreholeId: Int): Flow<List<DailyDrillingLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLog(log: DailyDrillingLog): Long

    @Query("DELETE FROM daily_drilling_logs WHERE id = :id")
    suspend fun deleteDailyLogById(id: Int)

    // --- Core Runs ---
    @Query("SELECT * FROM core_runs WHERE boreholeId = :boreholeId ORDER BY fromDepth ASC")
    fun getCoreRunsForBorehole(boreholeId: Int): Flow<List<CoreRun>>

    @Query("SELECT * FROM core_runs WHERE id = :id")
    suspend fun getCoreRunByIdOnce(id: Int): CoreRun?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoreRun(run: CoreRun): Long

    @Query("DELETE FROM core_runs WHERE id = :id")
    suspend fun deleteCoreRunById(id: Int)

    // --- Sync Export functions for Logging ---
    @Query("SELECT * FROM boreholes ORDER BY id ASC")
    suspend fun getAllBoreholesSync(): List<Borehole>

    @Query("SELECT * FROM core_runs ORDER BY boreholeId ASC, fromDepth ASC")
    suspend fun getAllCoreRunsSync(): List<CoreRun>

    @Query("SELECT * FROM daily_drilling_logs ORDER BY boreholeId ASC, date DESC")
    suspend fun getAllDailyLogsSync(): List<DailyDrillingLog>

    // --- Window Mappings ---
    @Query("SELECT * FROM window_mappings ORDER BY id DESC")
    fun getAllWindowMappings(): Flow<List<WindowMapping>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWindowMapping(windowMapping: WindowMapping): Long

    @Query("DELETE FROM window_mappings WHERE id = :id")
    suspend fun deleteWindowMappingById(id: Int)

    @Query("SELECT * FROM window_mappings ORDER BY id DESC")
    suspend fun getAllWindowMappingsSync(): List<WindowMapping>
}


// --- DATABASE ---

@Database(
    entities = [
        Project::class, 
        Station::class, 
        Observation::class, 
        Sample::class,
        Borehole::class,
        DailyDrillingLog::class,
        CoreRun::class,
        WindowMapping::class
    ],
    version = 5,
    exportSchema = false
)
abstract class GeologyDatabase : RoomDatabase() {
    abstract fun dao(): GeologyDao
}
