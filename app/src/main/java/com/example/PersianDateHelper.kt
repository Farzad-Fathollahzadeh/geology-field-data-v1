package com.example

import java.util.Calendar
import java.util.Date
import java.util.Locale

object PersianDateHelper {

    fun getPersianDateString(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)

        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        if ((gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0) {
            gDaysInMonth[2] = 29
        }

        var gDayNo = 0
        for (i in 1 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79
        var jYear = gy - 621

        if (jDayNo <= 0) {
            val prevGy = gy - 1
            val prevLeap = ((prevGy % 4 == 0 && prevGy % 100 != 0) || prevGy % 400 == 0)
            jDayNo += if (prevLeap) 366 else 365
            jYear -= 1
        }

        var jMonth = 1
        for (i in 1..12) {
            val monthDays = if (i == 12 && ((jYear + 1) % 4 == 0)) 30 else jDaysInMonth[i]
            if (jDayNo <= monthDays) {
                jMonth = i
                break
            }
            jDayNo -= monthDays
        }
        val jDay = jDayNo

        // Add time suffix
        val hr = cal.get(Calendar.HOUR_OF_DAY)
        val min = cal.get(Calendar.MINUTE)
        val timeSuffix = String.format(Locale.US, " %02d:%02d", hr, min)

        return "$jYear/${String.format(Locale.US, "%02d", jMonth)}/${String.format(Locale.US, "%02d", jDay)}$timeSuffix"
    }
}
