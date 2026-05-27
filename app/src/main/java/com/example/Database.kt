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
}


// --- DATABASE ---

@Database(
    entities = [Project::class, Station::class, Observation::class, Sample::class],
    version = 2,
    exportSchema = false
)
abstract class GeologyDatabase : RoomDatabase() {
    abstract fun dao(): GeologyDao
}
