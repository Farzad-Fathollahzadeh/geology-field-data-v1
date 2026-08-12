package com.example

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repository layer providing thread-safe, reactive, and robust access
 * to local SQLite Room database operations for geological field data.
 */
class GeologyRepository(
    private val dao: GeologyDao,
    private val db: GeologyDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        private const val TAG = "GeologyRepository"
    }

    // --- REVENUE & PROJECT FLOWS ---
    val projects: Flow<List<Project>> = dao.getAllProjects()
        .catch { e ->
            Log.e(TAG, "Error fetching projects flow", e)
            emit(emptyList())
        }
        .flowOn(ioDispatcher)

    val allStations: Flow<List<Station>> = dao.getAllStations()
        .catch { e ->
            Log.e(TAG, "Error fetching all stations flow", e)
            emit(emptyList())
        }
        .flowOn(ioDispatcher)

    val allObservations: Flow<List<Observation>> = dao.getAllObservations()
        .catch { e ->
            Log.e(TAG, "Error fetching all observations flow", e)
            emit(emptyList())
        }
        .flowOn(ioDispatcher)

    val allSamples: Flow<List<Sample>> = dao.getAllSamples()
        .catch { e ->
            Log.e(TAG, "Error fetching all samples flow", e)
            emit(emptyList())
        }
        .flowOn(ioDispatcher)

    val tracks: Flow<List<Track>> = dao.getAllTracks()
        .catch { e ->
            Log.e(TAG, "Error fetching tracks flow", e)
            emit(emptyList())
        }
        .flowOn(ioDispatcher)

    fun getProjectById(id: Int): Flow<Project?> = dao.getProjectById(id)
        .catch { e ->
            Log.e(TAG, "Error fetching project $id", e)
            emit(null)
        }
        .flowOn(ioDispatcher)

    fun getStationsForProject(projectId: Int): Flow<List<Station>> = dao.getStationsForProject(projectId)
        .catch { e ->
            Log.e(TAG, "Error fetching stations for project $projectId", e)
            emit(emptyList())
        }
        .flowOn(ioDispatcher)

    fun getStationById(stationId: Int): Flow<Station?> = dao.getStationById(stationId)
        .catch { e ->
            Log.e(TAG, "Error fetching station $stationId", e)
            emit(null)
        }
        .flowOn(ioDispatcher)

    fun getObservationsForStation(stationId: Int): Flow<List<Observation>> = dao.getObservationsForStation(stationId)
        .catch { e ->
            Log.e(TAG, "Error fetching observations for station $stationId", e)
            emit(emptyList())
        }
        .flowOn(ioDispatcher)

    fun getTracksForProject(projectId: Int): Flow<List<Track>> = dao.getTracksForProject(projectId)
        .catch { e ->
            Log.e(TAG, "Error fetching tracks for project $projectId", e)
            emit(emptyList())
        }
        .flowOn(ioDispatcher)

    fun getBoreholesForProject(projectId: Int): Flow<List<Borehole>> = dao.getBoreholesForProject(projectId)
        .catch { e ->
            Log.e(TAG, "Error fetching boreholes for project $projectId", e)
            emit(emptyList())
        }
        .flowOn(ioDispatcher)

    fun getBoreholeById(id: Int): Flow<Borehole?> = dao.getBoreholeById(id)
        .catch { e ->
            Log.e(TAG, "Error fetching borehole $id", e)
            emit(null)
        }
        .flowOn(ioDispatcher)

    fun getDailyReportsForBorehole(boreholeId: Int): Flow<List<BoreholeDailyReport>> = dao.getDailyReportsForBorehole(boreholeId)
        .catch { e ->
            Log.e(TAG, "Error fetching daily reports for borehole $boreholeId", e)
            emit(emptyList())
        }
        .flowOn(ioDispatcher)

    fun getCoreRunsForBorehole(boreholeId: Int): Flow<List<BoreholeCoreRun>> = dao.getCoreRunsForBorehole(boreholeId)
        .catch { e ->
            Log.e(TAG, "Error fetching core runs for borehole $boreholeId", e)
            emit(emptyList())
        }
        .flowOn(ioDispatcher)

    fun getRockMechanicsEvaluationsForProject(projectId: Int): Flow<List<RockMechanicsEvaluation>> = dao.getRockMechanicsEvaluationsForProject(projectId)
        .catch { e ->
            Log.e(TAG, "Error fetching rock mechanics for project $projectId", e)
            emit(emptyList())
        }
        .flowOn(ioDispatcher)

    // --- WRITE / MUTATION SUSPEND OPERATIONS ---

    suspend fun insertProject(project: Project): Long = withContext(ioDispatcher) {
        try {
            dao.insertProject(project)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting project", e)
            -1L
        }
    }

    suspend fun deleteProjectById(id: Int) = withContext(ioDispatcher) {
        try {
            dao.deleteProjectById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting project $id", e)
        }
    }

    suspend fun insertStation(station: Station): Long = withContext(ioDispatcher) {
        try {
            dao.insertStation(station)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting station", e)
            -1L
        }
    }

    suspend fun deleteStationById(id: Int) = withContext(ioDispatcher) {
        try {
            dao.deleteStationById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting station $id", e)
        }
    }

    suspend fun insertObservation(observation: Observation): Long = withContext(ioDispatcher) {
        try {
            dao.insertObservation(observation)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting observation", e)
            -1L
        }
    }

    suspend fun deleteObservationById(id: Int) = withContext(ioDispatcher) {
        try {
            dao.deleteObservationById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting observation $id", e)
        }
    }

    suspend fun insertSample(sample: Sample): Long = withContext(ioDispatcher) {
        try {
            dao.insertSample(sample)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting sample", e)
            -1L
        }
    }

    suspend fun insertTrack(track: Track): Long = withContext(ioDispatcher) {
        try {
            dao.insertTrack(track)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting track", e)
            -1L
        }
    }

    suspend fun deleteTrackById(id: Int) = withContext(ioDispatcher) {
        try {
            dao.deleteTrackById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting track $id", e)
        }
    }

    suspend fun updateTrackName(id: Int, name: String) = withContext(ioDispatcher) {
        try {
            dao.updateTrackName(id, name)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating track name", e)
        }
    }

    suspend fun updateTrackStyling(id: Int, name: String, colorHex: String, strokeWidth: Float) = withContext(ioDispatcher) {
        try {
            dao.updateTrackStyling(id, name, colorHex, strokeWidth)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating track styling", e)
        }
    }

    suspend fun updateTrackPoints(id: Int, pointsJson: String) = withContext(ioDispatcher) {
        try {
            dao.updateTrackPoints(id, pointsJson)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating track points", e)
        }
    }

    suspend fun insertBorehole(borehole: Borehole): Long = withContext(ioDispatcher) {
        try {
            dao.insertBorehole(borehole)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting borehole", e)
            -1L
        }
    }

    suspend fun deleteBoreholeById(id: Int) = withContext(ioDispatcher) {
        try {
            dao.deleteBoreholeById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting borehole $id", e)
        }
    }

    suspend fun insertDailyReport(report: BoreholeDailyReport): Long = withContext(ioDispatcher) {
        try {
            dao.insertDailyReport(report)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting daily report", e)
            -1L
        }
    }

    suspend fun deleteDailyReportById(id: Int) = withContext(ioDispatcher) {
        try {
            dao.deleteDailyReportById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting daily report $id", e)
        }
    }

    suspend fun insertCoreRun(coreRun: BoreholeCoreRun): Long = withContext(ioDispatcher) {
        try {
            dao.insertCoreRun(coreRun)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting core run", e)
            -1L
        }
    }

    suspend fun deleteCoreRunById(id: Int) = withContext(ioDispatcher) {
        try {
            dao.deleteCoreRunById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting core run $id", e)
        }
    }

    suspend fun insertRockMechanicsEvaluation(evaluation: RockMechanicsEvaluation): Long = withContext(ioDispatcher) {
        try {
            dao.insertRockMechanicsEvaluation(evaluation)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting rock mechanics evaluation", e)
            -1L
        }
    }

    suspend fun deleteRockMechanicsEvaluationById(id: Int) = withContext(ioDispatcher) {
        try {
            dao.deleteRockMechanicsEvaluationById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting rock mechanics evaluation $id", e)
        }
    }

    // --- SYNCHRONOUS/EXPORT BATCH QUERIES ---

    suspend fun getStationsForProjectSync(projectId: Int): List<Station> = withContext(ioDispatcher) {
        try { dao.getStationsForProjectSync(projectId) } catch (e: Exception) { emptyList() }
    }

    suspend fun getObservationsForProjectSync(projectId: Int): List<Observation> = withContext(ioDispatcher) {
        try { dao.getObservationsForProjectSync(projectId) } catch (e: Exception) { emptyList() }
    }

    suspend fun getSamplesForProjectSync(projectId: Int): List<Sample> = withContext(ioDispatcher) {
        try { dao.getSamplesForProjectSync(projectId) } catch (e: Exception) { emptyList() }
    }

    suspend fun getTracksForProjectSync(projectId: Int): List<Track> = withContext(ioDispatcher) {
        try { dao.getTracksForProjectSync(projectId) } catch (e: Exception) { emptyList() }
    }

    suspend fun getBoreholesForProjectSync(projectId: Int): List<Borehole> = withContext(ioDispatcher) {
        try { dao.getBoreholesForProjectSync(projectId) } catch (e: Exception) { emptyList() }
    }

    suspend fun getDailyReportsForBoreholeSync(boreholeId: Int): List<BoreholeDailyReport> = withContext(ioDispatcher) {
        try { dao.getDailyReportsForBoreholeSync(boreholeId) } catch (e: Exception) { emptyList() }
    }

    suspend fun getCoreRunsForBoreholeSync(boreholeId: Int): List<BoreholeCoreRun> = withContext(ioDispatcher) {
        try { dao.getCoreRunsForBoreholeSync(boreholeId) } catch (e: Exception) { emptyList() }
    }

    /**
     * Optimizes SQLite storage performance by executing VACUUM and WAL checkpoint.
     */
    suspend fun optimizeDatabase() = withContext(ioDispatcher) {
        try {
            db.openHelper.writableDatabase.execSQL("VACUUM")
            db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
            Log.i(TAG, "Database maintenance & vacuum completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error optimizing database", e)
        }
    }
}
