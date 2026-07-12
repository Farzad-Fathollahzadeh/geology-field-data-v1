package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class UtmBackgroundService : Service() {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    companion object {
        val liveLocation = MutableStateFlow<Location?>(null)
        val utmString = MutableStateFlow<String>("N/A")
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(101, createNotification())
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .apply {
                setMinUpdateIntervalMillis(5000L)
                setMaxUpdateDelayMillis(5000L)
            }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                liveLocation.value = loc
                try {
                    val utm = UtmConverter.convertLatLngToUtm(loc.latitude, loc.longitude)
                    utmString.value = "${utm.zoneString}  ${String.format(java.util.Locale.US, "%.1f", utm.easting)}m E  ${String.format(java.util.Locale.US, "%.1f", utm.northing)}m N"
                } catch (e: Exception) {
                    utmString.value = "${String.format(java.util.Locale.US, "%.5f", loc.latitude)}, ${String.format(java.util.Locale.US, "%.5f", loc.longitude)}"
                }
            }
        }

        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                android.os.Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Permission check missing or denied
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "utm_service_channel",
                "UTM Background Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Updates UTM Coordinates in Background"
            }
            val manager = getSystemService(NotificationManager::class.java) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "utm_service_channel")
            .setContentTitle("GeoField UTM Tracker")
            .setContentText("Tracking UTM coordinates in the background (5s updates)")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
        serviceJob.cancel()
    }
}
