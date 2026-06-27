package com.example

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

object PersianDateHelper {
    fun getPersianDateString(timestamp: Long): String {
        val date = Date(timestamp)
        val cal = GregorianCalendar()
        cal.time = date
        
        val gYear = cal.get(Calendar.YEAR)
        val gMonth = cal.get(Calendar.MONTH) + 1
        val gDay = cal.get(Calendar.DAY_OF_MONTH)
        
        // Days in Gregorian and Jalali months
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        
        var gy = gYear - 1600
        var gm = gMonth - 1
        var gd = gDay - 1
        
        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) {
            var days = gDaysInMonth[i + 1]
            if (i == 1 && ((gYear % 4 == 0 && gYear % 100 != 0) || (gYear % 400 == 0))) {
                days = 29
            }
            gDayNo += days
        }
        gDayNo += gd
        
        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053
        
        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461
        
        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }
        
        var jm = 0
        for (i in 0..11) {
            val days = jDaysInMonth[i + 1]
            if (jDayNo < days) {
                jm = i + 1
                break
            }
            jDayNo -= days
        }
        val jd = jDayNo + 1
        
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        
        val farsiDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        fun toFarsiString(str: String): String {
            return str.map { if (it in '0'..'9') farsiDigits[it - '0'] else it }.joinToString("")
        }
        
        val formatted = String.format("%04d/%02d/%02d %02d:%02d", jy, jm, jd, hour, minute)
        return toFarsiString(formatted)
    }
}
