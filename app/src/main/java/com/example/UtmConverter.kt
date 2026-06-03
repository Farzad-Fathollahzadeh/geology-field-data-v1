package com.example

import kotlin.math.*

object UtmConverter {

    data class UtmResult(
        val easting: Double,
        val northing: Double,
        val zoneNumber: Int,
        val zoneLetter: Char,
        val zoneString: String
    )

    fun convertLatLngToUtm(latitude: Double, longitude: Double): UtmResult {
        val latRad = Math.toRadians(latitude)
        val lonRad = Math.toRadians(longitude)

        val zoneNumber = ((longitude + 180) / 6).toInt() + 1
        val lonOrigin = ((zoneNumber - 1) * 6 - 180 + 3).toDouble()
        val lonOriginRad = Math.toRadians(lonOrigin)

        val zoneLetter = getUtmLetterDesignator(latitude)

        // WGS84 Ellipsoid constants
        val a = 6378137.0
        val eccSquared = 0.00669437999013
        val k0 = 0.9996

        val eccPrimeSquared = eccSquared / (1 - eccSquared)
        val n = a / sqrt(1 - eccSquared * sin(latRad).pow(2.0))
        val t = tan(latRad).pow(2.0)
        val c = eccPrimeSquared * cos(latRad).pow(2.0)
        val m = a * ((1 - eccSquared / 4 - 3 * eccSquared.pow(2.0) / 64 - 5 * eccSquared.pow(3.0) / 256) * latRad -
                (3 * eccSquared / 8 + 3 * eccSquared.pow(2.0) / 32 + 45 * eccSquared.pow(3.0) / 1024) * sin(2 * latRad) +
                (15 * eccSquared.pow(2.0) / 256 + 45 * eccSquared.pow(3.0) / 1024) * sin(4 * latRad) -
                (35 * eccSquared.pow(3.0) / 3072) * sin(6 * latRad))

        val deltaLon = lonRad - lonOriginRad

        val easting = k0 * n * (deltaLon * cos(latRad) + deltaLon.pow(3.0) * cos(latRad).pow(3.0) * (1 - t + c) / 6 +
                deltaLon.pow(5.0) * cos(latRad).pow(5.0) * (5 - 18 * t + t.pow(2.0) + 72 * c - 58 * eccPrimeSquared) / 120) + 500000.0

        var northing = k0 * (m + n * tan(latRad) * (deltaLon.pow(2.0) * cos(latRad).pow(2.0) / 2 +
                deltaLon.pow(4.0) * cos(latRad).pow(4.0) * (5 - t + 9 * c + 4 * c.pow(2.0)) / 24 +
                deltaLon.pow(6.0) * cos(latRad).pow(6.0) * (61 - 58 * t + t.pow(2.0) + 600 * c - 330 * eccPrimeSquared) / 720))

        if (latitude < 0) {
            northing += 10000000.0 // Southern hemisphere offset
        }

        return UtmResult(
            easting = easting,
            northing = northing,
            zoneNumber = zoneNumber,
            zoneLetter = zoneLetter,
            zoneString = "$zoneNumber$zoneLetter"
        )
    }

    private fun getUtmLetterDesignator(latitude: Double): Char {
        return when {
            latitude >= 84 -> 'X'
            latitude >= 72 -> 'W'
            latitude >= 64 -> 'V'
            latitude >= 56 -> 'U'
            latitude >= 48 -> 'T'
            latitude >= 40 -> 'S'
            latitude >= 32 -> 'R'
            latitude >= 24 -> 'Q'
            latitude >= 16 -> 'P'
            latitude >= 8 -> 'N'
            latitude >= 0 -> 'M'
            latitude >= -8 -> 'L'
            latitude >= -16 -> 'K'
            latitude >= -24 -> 'J'
            latitude >= -32 -> 'H'
            latitude >= -40 -> 'G'
            latitude >= -48 -> 'F'
            latitude >= -56 -> 'E'
            latitude >= -64 -> 'D'
            latitude >= -72 -> 'C'
            else -> 'C'
        }
    }
}
