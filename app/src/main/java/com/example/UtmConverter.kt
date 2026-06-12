package com.example

object UtmConverter {
    data class UtmResult(
        val easting: Double,
        val northing: Double,
        val zoneNumber: Int,
        val zoneChar: Char
    ) {
        val zoneString: String get() = "$zoneNumber$zoneChar"
        
        override fun toString(): String {
            return "${String.format("%.3f", easting)} E, ${String.format("%.3f", northing)} N, Zone $zoneNumber$zoneChar"
        }
    }

    fun convertLatLngToUtm(latitude: Double, longitude: Double, forcedZoneString: String? = null): UtmResult {
        // Normalise longitude to [-180, 180) to cover all global coordinates perfectly
        var adjustedLon = longitude
        while (adjustedLon < -180.0) adjustedLon += 360.0
        while (adjustedLon >= 180.0) adjustedLon -= 360.0

        // Clamp latitude to standard UTM limits (-80 to 84) to prevent projection distortions
        val adjustedLat = latitude.coerceIn(-80.0, 84.0)

        val latRad = Math.toRadians(adjustedLat)
        val lonRad = Math.toRadians(adjustedLon)

        var zoneNumber = (((adjustedLon + 180) / 6).toInt() % 60) + 1
        if (zoneNumber <= 0) zoneNumber = 1
        if (zoneNumber > 60) zoneNumber = 60
        
        val parsedDigits = forcedZoneString?.filter { it.isDigit() } ?: ""
        val parsedLetters = forcedZoneString?.filter { it.isLetter() } ?: ""
        val parsedZoneNumber = parsedDigits.toIntOrNull()
        val parsedZoneChar = parsedLetters.firstOrNull()?.uppercaseChar()

        val actualZoneNumber = parsedZoneNumber ?: if (adjustedLat >= 56.0 && adjustedLat < 64.0 && adjustedLon >= 3.0 && adjustedLon < 12.0) {
            32
        } else if (adjustedLat >= 72.0 && adjustedLat < 84.0) {
            if (adjustedLon >= 0.0 && adjustedLon < 9.0) 31
            else if (adjustedLon >= 9.0 && adjustedLon < 21.0) 33
            else if (adjustedLon >= 21.0 && adjustedLon < 33.0) 35
            else if (adjustedLon >= 33.0 && adjustedLon < 42.0) 37
            else zoneNumber
        } else {
            zoneNumber
        }

        val lonOrigin = (actualZoneNumber - 1) * 6 - 180 + 3
        val lonOriginRad = Math.toRadians(lonOrigin.toDouble())

        val utmLetterDesignator = parsedZoneChar ?: getUtmLetterDesignator(adjustedLat)

        // WGS84 Ellipsoid constants
        val a = 6378137.0
        val eccSquared = 0.00669437999013
        val k0 = 0.9996

        val eccPrimeSquared = eccSquared / (1 - eccSquared)
        val n = a / Math.sqrt(1 - eccSquared * Math.sin(latRad) * Math.sin(latRad))
        val t = Math.tan(latRad) * Math.tan(latRad)
        val c = eccPrimeSquared * Math.cos(latRad) * Math.cos(latRad)
        val aCoeff = Math.cos(latRad) * (lonRad - lonOriginRad)

        val m = a * ((1 - eccSquared / 4 - 3 * eccSquared * eccSquared / 64 - 5 * eccSquared * eccSquared * eccSquared / 256) * latRad
                - (3 * eccSquared / 8 + 3 * eccSquared * eccSquared / 32 + 45 * eccSquared * eccSquared * eccSquared / 1024) * Math.sin(2 * latRad)
                + (15 * eccSquared * eccSquared / 256 + 45 * eccSquared * eccSquared * eccSquared / 1024) * Math.sin(4 * latRad)
                - (35 * eccSquared * eccSquared * eccSquared / 3072) * Math.sin(6 * latRad))

        val easting = k0 * n * (aCoeff + (1 - t + c) * aCoeff * aCoeff * aCoeff / 6
                + (5 - 18 * t + t * t + 72 * c - 58 * eccPrimeSquared) * aCoeff * aCoeff * aCoeff * aCoeff * aCoeff / 120) + 500000.0

        var northing = k0 * (m + n * Math.tan(latRad) * (aCoeff * aCoeff / 2 + (5 - t + 9 * c + 4 * c * c) * aCoeff * aCoeff * aCoeff * aCoeff / 24
                + (61 - 58 * t + t * t + 600 * c - 330 * eccPrimeSquared) * aCoeff * aCoeff * aCoeff * aCoeff * aCoeff * aCoeff / 720))

        val isSouthern = if (parsedZoneChar != null) {
            parsedZoneChar in 'C'..'M'
        } else {
            latitude < 0
        }

        if (isSouthern) {
            northing += 10000000.0
        }

        return UtmResult(easting, northing, actualZoneNumber, utmLetterDesignator)
    }

    private fun getUtmLetterDesignator(lat: Double): Char {
        return when {
            lat >= 84 || lat < -80 -> 'Z'
            lat >= 72 -> 'X'
            lat >= 64 -> 'W'
            lat >= 56 -> 'V'
            lat >= 48 -> 'U'
            lat >= 40 -> 'T'
            lat >= 32 -> 'S'
            lat >= 24 -> 'R'
            lat >= 16 -> 'Q'
            lat >= 8 -> 'P'
            lat >= 0 -> 'N'
            lat >= -8 -> 'M'
            lat >= -16 -> 'L'
            lat >= -24 -> 'K'
            lat >= -32 -> 'J'
            lat >= -40 -> 'H'
            lat >= -48 -> 'G'
            lat >= -56 -> 'F'
            lat >= -64 -> 'E'
            lat >= -72 -> 'D'
            else -> 'C'
        }
    }
}
