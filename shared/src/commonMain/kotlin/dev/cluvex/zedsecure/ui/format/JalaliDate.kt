package dev.cluvex.zedsecure.ui.format

import java.util.Calendar

/**
 * Gregorian-to-Jalali conversion for subscription expiry labels.
 * Uses an integer-only break-table calculation and the device's local date.
 */
object JalaliDate {
    private val months = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
        1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )

    fun format(epochMillis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val date = toJalali(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
        )
        return date[2].toString() + " " + months[date[1] - 1] + " " + date[0]
    }

    fun toJalali(gy: Int, gm: Int, gd: Int): IntArray {
        val jdn = gregorianToDayNumber(gy, gm, gd)
        var jy = gy - 621
        val result = jalaliCalendar(jy)
        val firstDay = gregorianToDayNumber(gy, 3, result[2])
        var day = jdn - firstDay

        if (day >= 0) {
            if (day <= 185) return intArrayOf(jy, 1 + day / 31, day % 31 + 1)
            day -= 186
        } else {
            jy -= 1
            day += 179
            if (result[0] == 1) day += 1
        }
        return intArrayOf(jy, 7 + day / 30, day % 30 + 1)
    }

    private fun jalaliCalendar(jy: Int): IntArray {
        val gy = jy + 621
        var leapJ = -14
        var previous = breaks[0]
        var jump = 0

        for (index in 1 until breaks.size) {
            val next = breaks[index]
            jump = next - previous
            if (jy < next) break
            leapJ += (jump / 33) * 8 + ((jump % 33) / 4)
            previous = next
        }

        var offset = jy - previous
        leapJ += (offset / 33) * 8 + (((offset % 33) + 3) / 4)
        if ((jump % 33) == 4 && jump - offset == 4) leapJ += 1

        val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG
        if (jump - offset < 6) offset = offset - jump + ((jump + 4) / 33) * 33

        var leap = (((offset + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4
        return intArrayOf(leap, gy, march)
    }

    private fun gregorianToDayNumber(gy: Int, gm: Int, gd: Int): Int {
        val monthOffset = (gm - 8) / 6
        val adjustedYear = gy + monthOffset + 100100
        var dayNumber = (adjustedYear * 1461) / 4 +
            (153 * ((gm + 9) % 12) + 2) / 5 + gd - 34840408
        dayNumber -= (((gy + 100100 + monthOffset) / 100) * 3) / 4
        return dayNumber + 752
    }
}
