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
    ],
    indices = [androidx.room.Index(value = ["projectId"])]
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
    ],
    indices = [androidx.room.Index(value = ["stationId"])]
)
data class Observation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val stationId: Int,
    val observationType: String, // Lithology, Alteration, Fault, Joint, Vein, Structure, Mineralization, Sample
    val photoPath: String? = null,
    val notes: String? = null,
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
    ],
    indices = [androidx.room.Index(value = ["observationId"])]
)
data class Sample(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val observationId: Int,
    val sampleCodeId: String, // e.g. DZ-014-A
    val sampleType: String,   // Hand specimen, Core, Soil, Stream sediment, etc.
    val description: String?,
    val photoPath: String?
)

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int = 0,
    val name: String,
    val pointsJson: String, // format: "[[lat,lon,ele,time,accuracy,heading],...]"
    val lengthMeters: Double = 0.0,
    val durationSeconds: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val colorHex: String = "#ff3d00",
    val strokeWidth: Float = 4.5f
)

data class TrackPoint(
    val lat: Double,
    val lon: Double,
    val ele: Double,
    val time: Long,
    val accuracy: Double,
    val heading: Double
)

fun parsePointsJson(json: String): List<TrackPoint> {
    if (json.isBlank() || json == "[]") return emptyList()
    try {
        val clean = json.replace("[[", "").replace("]]", "").split("],[")
        return clean.mapNotNull { item ->
            val parts = item.split(",")
            if (parts.size >= 5) {
                val hasAccuracyElement = parts.size >= 6
                TrackPoint(
                    lat = parts[0].toDoubleOrNull() ?: 0.0,
                    lon = parts[1].toDoubleOrNull() ?: 0.0,
                    ele = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
                    time = parts.getOrNull(3)?.toLongOrNull() ?: 0L,
                    accuracy = if (hasAccuracyElement) (parts.getOrNull(4)?.toDoubleOrNull() ?: 1.0) else 1.0,
                    heading = if (hasAccuracyElement) (parts.getOrNull(5)?.toDoubleOrNull() ?: 0.0) else (parts.getOrNull(4)?.toDoubleOrNull() ?: 0.0)
                )
            } else if (parts.size >= 2) {
                TrackPoint(
                    lat = parts[0].toDoubleOrNull() ?: 0.0,
                    lon = parts[1].toDoubleOrNull() ?: 0.0,
                    ele = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
                    time = parts.getOrNull(3)?.toLongOrNull() ?: 0L,
                    accuracy = 5.0,
                    heading = 0.0
                )
            } else null
        }
    } catch (e: Exception) {
        return emptyList()
    }
}

@Entity(
    tableName = "boreholes",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["projectId"])]
)
data class Borehole(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val boreholeCode: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val azimuth: Double,
    val dip: Double,
    val plannedDepth: Double = 50.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "borehole_daily_reports",
    foreignKeys = [
        ForeignKey(
            entity = Borehole::class,
            parentColumns = ["id"],
            childColumns = ["boreholeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["boreholeId"])]
)
data class BoreholeDailyReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val boreholeId: Int,
    val reportDate: String,
    val shift: String, // Day, Night
    val progressMeters: Double,
    val stoppagesHours: Double,
    val problemsRemarks: String,
    val shiftGeologist: String = "",
    val drillRigId: String = ""
)

@Entity(
    tableName = "borehole_core_runs",
    foreignKeys = [
        ForeignKey(
            entity = Borehole::class,
            parentColumns = ["id"],
            childColumns = ["boreholeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["boreholeId"])]
)
data class BoreholeCoreRun(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val boreholeId: Int,
    val runNumber: Int,
    val depthFrom: Double,
    val depthTo: Double,
    val recoveryPercent: Double,
    val rqdPercent: Double,
    val lithology: String,
    val mineralization: String,
    val alteration: String,
    val fractureFrequency: Int,
    val ucsMpa: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "rock_mechanics_evaluations",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["projectId"])]
)
data class RockMechanicsEvaluation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val locationName: String,
    val evaluationType: String, // RMR, Hoek-Brown, MRMR, RMi
    val evaluationDate: String,
    
    // RMR fields
    val rmrUcsVal: Double = 0.0,
    val rmrRqdVal: Double = 0.0,
    val rmrSpacingVal: Double = 0.0,
    val rmrConditionVal: Double = 0.0,
    val rmrWaterVal: Double = 0.0,
    val rmrAdjustmentVal: Double = 0.0,
    val rmrScore: Double = 0.0,
    val rmrClass: String = "",
    
    // Hoek-Brown fields
    val hbSigci: Double = 0.0,
    val hbGsi: Double = 0.0,
    val hbMi: Double = 0.0,
    val hbD: Double = 0.0,
    val hbMb: Double = 0.0,
    val hbS: Double = 0.0,
    val hbA: Double = 0.0,
    val hbSigmaci: Double = 0.0,
    
    // MRMR fields
    val mrmrRmrBasic: Double = 0.0,
    val mrmrWeatheringAdj: Double = 0.0,
    val mrmrJointOrientationAdj: Double = 0.0,
    val mrmrDrillingAdj: Double = 0.0,
    val mrmrScore: Double = 0.0,
    val mrmrClass: String = "",
    
    // RMi fields
    val rmiUcs: Double = 0.0,
    val rmiJp: Double = 0.0,
    val rmiValue: Double = 0.0,
    
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)


// --- DAO ---

@Dao
interface GeologyDao {
    
    // Projects
    @Query("SELECT * FROM projects ORDER BY id DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM stations")
    fun getAllStations(): Flow<List<Station>>

    @Query("SELECT * FROM observations")
    fun getAllObservations(): Flow<List<Observation>>

    @Query("SELECT * FROM samples")
    fun getAllSamples(): Flow<List<Sample>>

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

    // Tracks
    @Query("SELECT * FROM tracks ORDER BY id DESC")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY id DESC")
    fun getTracksForProject(projectId: Int): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY id DESC")
    suspend fun getTracksForProjectSync(projectId: Int): List<Track>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track): Long

    @Query("UPDATE tracks SET name = :name WHERE id = :id")
    suspend fun updateTrackName(id: Int, name: String)

    @Query("UPDATE tracks SET name = :name, colorHex = :colorHex, strokeWidth = :strokeWidth WHERE id = :id")
    suspend fun updateTrackStyling(id: Int, name: String, colorHex: String, strokeWidth: Float)

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrackById(id: Int)

    // Boreholes
    @Query("SELECT * FROM boreholes WHERE projectId = :projectId ORDER BY id ASC")
    fun getBoreholesForProject(projectId: Int): Flow<List<Borehole>>

    @Query("SELECT * FROM boreholes WHERE id = :id")
    fun getBoreholeById(id: Int): Flow<Borehole?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBorehole(borehole: Borehole): Long

    @Query("DELETE FROM boreholes WHERE id = :id")
    suspend fun deleteBoreholeById(id: Int)

    // Borehole Daily Reports
    @Query("SELECT * FROM borehole_daily_reports WHERE boreholeId = :boreholeId ORDER BY id DESC")
    fun getDailyReportsForBorehole(boreholeId: Int): Flow<List<BoreholeDailyReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyReport(report: BoreholeDailyReport): Long

    @Query("DELETE FROM borehole_daily_reports WHERE id = :id")
    suspend fun deleteDailyReportById(id: Int)

    // Borehole Core Runs
    @Query("SELECT * FROM borehole_core_runs WHERE boreholeId = :boreholeId ORDER BY runNumber ASC")
    fun getCoreRunsForBorehole(boreholeId: Int): Flow<List<BoreholeCoreRun>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoreRun(coreRun: BoreholeCoreRun): Long

    @Query("DELETE FROM borehole_core_runs WHERE id = :id")
    suspend fun deleteCoreRunById(id: Int)

    @Query("SELECT * FROM boreholes WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun getBoreholesForProjectSync(projectId: Int): List<Borehole>

    @Query("SELECT * FROM borehole_daily_reports WHERE boreholeId = :boreholeId ORDER BY id DESC")
    suspend fun getDailyReportsForBoreholeSync(boreholeId: Int): List<BoreholeDailyReport>

    @Query("SELECT * FROM borehole_core_runs WHERE boreholeId = :boreholeId ORDER BY runNumber ASC")
    suspend fun getCoreRunsForBoreholeSync(boreholeId: Int): List<BoreholeCoreRun>

    // Rock Mechanics Evaluations
    @Query("SELECT * FROM rock_mechanics_evaluations WHERE projectId = :projectId ORDER BY id DESC")
    fun getRockMechanicsEvaluationsForProject(projectId: Int): Flow<List<RockMechanicsEvaluation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRockMechanicsEvaluation(evaluation: RockMechanicsEvaluation): Long

    @Query("DELETE FROM rock_mechanics_evaluations WHERE id = :id")
    suspend fun deleteRockMechanicsEvaluationById(id: Int)
}


// --- DATABASE ---

@Database(
    entities = [
        Project::class, 
        Station::class, 
        Observation::class, 
        Sample::class, 
        Track::class,
        Borehole::class,
        BoreholeDailyReport::class,
        BoreholeCoreRun::class,
        RockMechanicsEvaluation::class
    ],
    version = 8,
    exportSchema = false
)
abstract class GeologyDatabase : RoomDatabase() {
    abstract fun dao(): GeologyDao
}
