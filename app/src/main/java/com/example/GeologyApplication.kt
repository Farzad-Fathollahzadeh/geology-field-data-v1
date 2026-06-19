package com.example

import android.app.Application

class GeologyApplication : Application() {
    val database by lazy {
        androidx.room.Room.databaseBuilder(
            this,
            GeologyDatabase::class.java,
            "geology_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    val dao by lazy { database.dao() }
}
