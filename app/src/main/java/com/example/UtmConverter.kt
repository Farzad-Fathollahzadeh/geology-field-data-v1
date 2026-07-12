package com.example

import java.util.Locale

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

    fun formatDMS(value: Double, isLat: Boolean): String {
        val direction = if (isLat) {
            if (value >= 0) "N" else "S"
        } else {
            if (value >= 0) "E" else "W"
        }
        val absVal = Math.abs(value)
        val degrees = absVal.toInt()
        val minutesAndSec = (absVal - degrees) * 60.0
        val minutes = minutesAndSec.toInt()
        val seconds = (minutesAndSec - minutes) * 60.0
        return String.format(Locale.US, "%d°%02d'%02.1f\"%s", degrees, minutes, seconds, direction)
    }

    fun formatDDM(value: Double, isLat: Boolean): String {
        val direction = if (isLat) {
            if (value >= 0) "N" else "S"
        } else {
            if (value >= 0) "E" else "W"
        }
        val absVal = Math.abs(value)
        val degrees = absVal.toInt()
        val minutes = (absVal - degrees) * 60.0
        return String.format(Locale.US, "%d°%07.4f'%s", degrees, minutes, direction)
    }

    fun formatMGRS(latitude: Double, longitude: Double): String {
        return try {
            val utmCoord = convertLatLngToUtm(latitude, longitude)
            val z = utmCoord.zoneNumber
            val band = utmCoord.zoneChar
            
            // 100km column letters (excluding I and O)
            val colLetters = "ABCDEFGHJKLMNPQRSTUVXYZ"
            val colIndex = (utmCoord.easting / 100000).toInt()
            val colOffset = ((z - 1) % 3) * 8
            val colLetter = colLetters[(colIndex - 1 + colOffset) % colLetters.length]
            
            // 100km row letters (excluding I and O)
            val rowLetters = "ABCDEFGHJKLMNPQRSTUV"
            val rowIndex = (utmCoord.northing / 100000).toInt() % 20
            val rowOffset = if (z % 2 != 0) 0 else 5
            val rowLetter = rowLetters[(rowIndex + rowOffset) % rowLetters.length]
            
            val eastMeters = (utmCoord.easting % 100000).toInt().coerceIn(0, 99999)
            val northMeters = (utmCoord.northing % 100000).toInt().coerceIn(0, 99999)
            
            val eStr = String.format(Locale.US, "%05d", eastMeters)
            val nStr = String.format(Locale.US, "%05d", northMeters)
            
            "$z$band $colLetter$rowLetter $eStr $nStr"
        } catch (e: Exception) {
            val zoneNum = (((longitude + 180) / 6).toInt() % 60) + 1
            "${zoneNum}N VF 00000 00000"
        }
    }

    fun formatCoordinateDisplay(
        lat: Double,
        lon: Double,
        utmZone: String,
        utmEasting: Double,
        utmNorthing: Double,
        coordSystemSetting: String,
        customEpsg: String = "32639"
    ): String {
        return when (coordSystemSetting) {
            "WGS84_DECIMAL" -> {
                String.format(Locale.US, "%.8f° N, %.8f° E", lat, lon)
            }
            "WGS84_DMS" -> {
                "${formatDMS(lat, true)}, ${formatDMS(lon, false)}"
            }
            "WGS84_DDM" -> {
                "${formatDDM(lat, true)}, ${formatDDM(lon, false)}"
            }
            "MGRS" -> {
                formatMGRS(lat, lon)
            }
            "WEB_MERCATOR" -> {
                val r = 6378137.0
                val x = r * Math.toRadians(lon)
                val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
                val y = r * Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0))
                String.format(Locale.US, "X: %.1f m, Y: %.1f m", x, y)
            }
            "CUSTOM_EPSG" -> {
                val digits = customEpsg.filter { it.isDigit() }
                when {
                    digits == "3857" -> {
                        val r = 6378137.0
                        val x = r * Math.toRadians(lon)
                        val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
                        val y = r * Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0))
                        String.format(Locale.US, "EPSG:3857 | X: %.1f m, Y: %.1f m", x, y)
                    }
                    digits == "4326" -> {
                        String.format(Locale.US, "EPSG:4326 | %.8f° N, %.8f° E", lat, lon)
                    }
                    digits.length == 5 && (digits.startsWith("326") || digits.startsWith("327")) -> {
                        val zone = digits.substring(3, 5).toIntOrNull() ?: 39
                        val isNorth = digits.startsWith("326")
                        val forcedZone = "$zone${if (isNorth) 'N' else 'S'}"
                        try {
                            val utm = convertLatLngToUtm(lat, lon, forcedZone)
                            String.format(Locale.US, "EPSG:%s | %.1f E, %.1f N", digits, utm.easting, utm.northing)
                        } catch (e: Exception) {
                            String.format(Locale.US, "EPSG:%s | %.6f°, %.6f°", digits, lat, lon)
                        }
                    }
                    else -> {
                        String.format(Locale.US, "EPSG:%s | %.6f°, %.6f°", digits, lat, lon)
                    }
                }
            }
            else -> { // "UTM"
                "Zone $utmZone | ${String.format(Locale.US, "%.0f E, %.0f N", utmEasting, utmNorthing)}"
            }
        }
    }
}
