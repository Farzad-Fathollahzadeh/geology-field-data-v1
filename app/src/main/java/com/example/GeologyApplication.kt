package com.example

import android.app.Application

class GeologyApplication : Application() {
    companion object {
        lateinit var instance: GeologyApplication
            private set
    }

    val database: GeologyDatabase by lazy {
        GeologyDatabase.getInstance(this)
    }
    val dao: GeologyDao by lazy { database.dao() }
    val repository: GeologyRepository by lazy { GeologyRepository(dao, database) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
