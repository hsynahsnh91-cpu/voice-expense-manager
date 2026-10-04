package com.abuomar.sawti.core

import java.util.Calendar
import java.util.Locale

/**
 * التواريخ المدنية — كل التواريخ نصوص "yyyy-MM-dd" تُحسب بالتقويم المحلي دائماً.
 *
 * هذا هو الإصلاح الجوهري لخطأ «اليوم الناقص» الشهير: لا نستخدم أبداً
 * SimpleDateFormat/Instant.parse على نص تاريخ لأنه يفسّره UTC فيرجع يوماً قبله
 * في المناطق الواقعة شرق غرينتش (سوريا UTC+3).
 */
object CivilDate {

    const val PATTERN: String = "yyyy-MM-dd"

    /** اليوم المحلي كنص مدني */
    fun today(): String = of(Calendar.getInstance())

    fun of(year: Int, month1: Int, day: Int): String =
        "%04d-%02d-%02d".format(year, month1, day)

    fun of(cal: Calendar): String =
        of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))

    /** تفكيك نص مدني إلى (سنة، شهر ١-١٢، يوم) — بلا أي تحويل لمنطقة زمنية */
    data class Parts(val year: Int, val month: Int, val day: Int)

    fun parts(civil: String): Parts {
        val p = civil.split("-")
        return Parts(
            year = p.getOrNull(0)?.toIntOrNull() ?: 1970,
            month = (p.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1, 12),
            day = (p.getOrNull(2)?.toIntOrNull() ?: 1).coerceIn(1, 31),
        )
    }

    /** تحويل نص مدني إلى Calendar محلي (للقراءة والعرض فقط) */
    fun toCalendar(civil: String): Calendar {
        val p = parts(civil)
        return Calendar.getInstance().apply {
            clear()
            set(p.year, p.month - 1, p.day, 12, 0, 0)
        }
    }

    fun plusDays(civil: String, days: Int): String =
        of(toCalendar(civil).apply { add(Calendar.DAY_OF_MONTH, days) })

    fun plusMonths(civil: String, months: Int): String {
        val p = parts(civil)
        val cal = Calendar.getInstance().apply {
            clear()
            set(p.year, p.month - 1, 1)
            add(Calendar.MONTH, months)
        }
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, minOf(p.day, lastDay))
    }

    fun startOfMonth(civil: String): String {
        val p = parts(civil)
        return of(p.year, p.month, 1)
    }

    fun endOfMonth(civil: String): String {
        val cal = toCalendar(startOfMonth(civil))
        return of(cal.apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) })
    }

    /** عدد أيام الشهر */
    fun daysInMonth(civil: String): Int =
        toCalendar(startOfMonth(civil)).getActualMaximum(Calendar.DAY_OF_MONTH)

    /** 0=الأحد … 6=السبت (مثل Calendar.SUNDAY-1) */
    fun weekday(civil: String): Int = toCalendar(civil).get(Calendar.DAY_OF_WEEK) - 1

    /** فرق الأيام بين تاريخين مدنيين */
    fun daysBetween(from: String, to: String): Int {
        val a = toCalendar(from).timeInMillis
        val b = toCalendar(to).timeInMillis
        return Math.round((b - a) / 86_400_000.0).toInt()
    }

    fun monthKey(civil: String): String = civil.substring(0, 7)

    /** بداية الأسبوع حسب اللغة: السبت(6) للعربية السورية، الأحد(0) للإنكليزية */
    fun weekStart(arabic: Boolean): Int = if (arabic) 6 else 0

    fun isBetween(civil: String, from: String?, to: String?): Boolean {
        if (from == null || to == null) return false
        val a = minOf(from, to)
        val b = maxOf(from, to)
        return civil >= a && civil <= b
    }

    /** عرض جميل حسب اللغة — يستخدم التقويم المحلي لا UTC */
    fun formatPretty(civil: String, arabic: Boolean): String {
        val locale = if (arabic) Locale("ar", "SY") else Locale.ENGLISH
        val cal = toCalendar(civil)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val year = cal.get(Calendar.YEAR)
        return if (arabic) {
            "$day ${AR_MONTHS[cal.get(Calendar.MONTH)]} $year"
        } else {
            val monthName = java.text.DateFormatSymbols(locale).shortMonths[cal.get(Calendar.MONTH)]
            "$monthName $day, $year"
        }
    }

    fun formatMonthYear(civil: String, arabic: Boolean): String {
        val cal = toCalendar(civil)
        return if (arabic) {
            "${AR_MONTHS[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
        } else {
            val name = java.text.DateFormatSymbols(Locale.ENGLISH).months[cal.get(Calendar.MONTH)]
            "$name ${cal.get(Calendar.YEAR)}"
        }
    }

    /** أسماء الأشهر الشامية المستخدمة في سوريا */
    val AR_MONTHS = arrayOf(
        "كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران",
        "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول",
    )

    /** أسماء الأيام المختصرة، مرتّبة بدءاً من index معيّن */
    fun weekdayLabels(arabic: Boolean, weekStart: Int): List<String> {
        val ar = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val en = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val src = if (arabic) ar else en
        return (0 until 7).map { src[(it + weekStart) % 7] }
    }

    fun isValid(civil: String?): Boolean =
        civil != null && civil.length == 10 && civil[4] == '-' && civil[7] == '-' &&
            runCatching { parts(civil) }.isSuccess
}
