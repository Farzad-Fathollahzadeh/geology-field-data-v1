package com.example

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TileCacheDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    init {
        setWriteAheadLoggingEnabled(true)
    }

    // Fast in-memory LRU cache for up to 120 map tiles to eliminate disk reads on pan/zoom
    private val memoryCache = object : android.util.LruCache<String, TileData>(120) {}

    companion object {
        private const val DATABASE_NAME = "geogis_tile_cache.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_TILE_CACHE = "tile_cache"
        
        private const val COLUMN_URL_KEY = "url_key"
        private const val COLUMN_DATA = "tile_data"
        private const val COLUMN_CONTENT_TYPE = "content_type"
        private const val COLUMN_CREATED_AT = "created_at"

        @Volatile
        private var instance: TileCacheDatabaseHelper? = null

        fun getInstance(context: Context): TileCacheDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: TileCacheDatabaseHelper(context).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_TILE_CACHE (
                $COLUMN_URL_KEY TEXT PRIMARY KEY,
                $COLUMN_DATA BLOB,
                $COLUMN_CONTENT_TYPE TEXT,
                $COLUMN_CREATED_AT INTEGER
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TILE_CACHE")
        onCreate(db)
    }

    fun getTile(url: String): TileData? {
        val cachedTile = memoryCache.get(url)
        if (cachedTile != null) return cachedTile

        val db = readableDatabase
        val cursor = db.query(
            TABLE_TILE_CACHE,
            arrayOf(COLUMN_DATA, COLUMN_CONTENT_TYPE),
            "$COLUMN_URL_KEY = ?",
            arrayOf(url),
            null, null, null
        )
        
        cursor.use {
            if (it.moveToFirst()) {
                val data = it.getBlob(0)
                val contentType = it.getString(1)
                val tile = TileData(data, contentType)
                memoryCache.put(url, tile)
                return tile
            }
        }
        return null
    }

    fun putTile(url: String, data: ByteArray, contentType: String) {
        val tile = TileData(data, contentType)
        memoryCache.put(url, tile)
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_URL_KEY, url)
                put(COLUMN_DATA, data)
                put(COLUMN_CONTENT_TYPE, contentType)
                put(COLUMN_CREATED_AT, System.currentTimeMillis())
            }
            db.insertWithOnConflict(
                TABLE_TILE_CACHE,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class TileData(val data: ByteArray, val contentType: String)
