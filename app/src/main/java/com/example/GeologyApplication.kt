package com.example

import android.app.Application

class GeologyApplication : Application() {
    val database by lazy {
        androidx.room.Room.databaseBuilder(
            this,
            GeologyDatabase::class.java,
            "geology_db"
        )
        .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .enableMultiInstanceInvalidation()
        .fallbackToDestructiveMigration()
        .build()
    }
    val dao by lazy { database.dao() }
    val repository by lazy { GeologyRepository(dao, database) }
}
