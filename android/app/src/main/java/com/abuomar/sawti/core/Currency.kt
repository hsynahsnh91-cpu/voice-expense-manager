package com.abuomar.sawti.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * العملة: الليرة السورية الجديدة.
 *
 * اعتباراً من ١ كانون الثاني ٢٠٢٦ حُذف صفران من العملة السورية، فأصبحت
 * كل ١٠٠ ليرة قديمة = ١ ليرة سورية جديدة.
 *
 * كل القيم داخل التطبيق تُخزَّن دائماً بالليرة الجديدة (Long، بلا كسور عشرية).
 * العرض فقط هو الذي يمكن قلبه إلى الوحدة القديمة.
 */
object Currency {

    const val CODE_NEW: String = "LSN"          // الليرة السورية الجديدة
    const val CODE_LEGACY: String = "SYP"       // القديمة (قبل حذف الصفرين)
    const val LEGACY_DIVISOR: Long = 100L       // 100 قديمة = 1 جديدة
    const val MAX_AMOUNT: Long = 999_000_000_000L

    /** قديمة → جديدة */
    fun legacyToNew(legacy: Long): Long = Math.round(legacy.toDouble() / LEGACY_DIVISOR)

    /** جديدة → قديمة */
    fun newToLegacy(new: Long): Long = new * LEGACY_DIVISOR

    enum class Unit(val id: String) { NEW("new"), LEGACY("legacy") }

    private val arSymbols = DecimalFormatSymbols(Locale("ar", "SY")).apply {
        groupingSeparator = '\u066C'   // ٬ فاصلة الآلاف العربية
        decimalSeparator = '\u066B'
    }
    private val enSymbols = DecimalFormatSymbols(Locale.ENGLISH).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    private fun formatter(arabic: Boolean): DecimalFormat =
        DecimalFormat("#,##0", if (arabic) arSymbols else enSymbols)

    /**
     * تنسيق مبلغ مخزَّن بالليرة الجديدة للعرض.
     * @param valueNew القيمة بالليرة السورية الجديدة
     * @param unit وحدة العرض
     * @param arabic هل الواجهة عربية؟
     * @param withSymbol إرفاق رمز العملة
     */
    fun format(
        valueNew: Long,
        unit: Unit = Unit.NEW,
        arabic: Boolean = true,
        withSymbol: Boolean = true,
    ): String {
        val value = if (unit == Unit.LEGACY) newToLegacy(valueNew) else valueNew
        val sign = if (value < 0) "-" else ""
        val grouped = formatter(arabic).format(Math.abs(value))
        if (!withSymbol) return "$sign$grouped"
        val symbol = if (arabic) {
            if (unit == Unit.LEGACY) "ل.س قديمة" else "ل.س"
        } else {
            if (unit == Unit.LEGACY) "old SYP" else "SYP"
        }
        return "$sign$grouped $symbol"
    }

    /** صيغة منطوقة طبيعية بالعربية (للنطق الصوتي) — أقرب لكلام البشر */
    fun spoken(valueNew: Long, arabic: Boolean = true): String {
        val v = Math.abs(valueNew)
        return if (arabic) {
            "${formatter(true).format(v)} ليرة سورية"
        } else {
            "${formatter(false).format(v)} Syrian pounds"
        }
    }

    /** قراءة رقم مكتوب (يدعم الأرقام العربية-الهندية ٠١٢٣ والفواصل) */
    fun parseAmount(text: String?): Long {
        if (text.isNullOrBlank()) return 0L
        val normalized = normalizeDigits(text).filter { it.isDigit() }
        if (normalized.isEmpty()) return 0L
        return runCatching { normalized.toLong() }.getOrDefault(0L)
    }

    /** تحويل ٠١٢٣٤٥٦٧٨٩ و ۰۱۲۳۴۵۶۷۸۹ إلى 0123456789 */
    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '\u0660'..'\u0669' -> sb.append(ch - '\u0660' + '0')   // عربية-هندية
                in '\u06F0'..'\u06F9' -> sb.append(ch - '\u06F0' + '0')   // فارسية
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
