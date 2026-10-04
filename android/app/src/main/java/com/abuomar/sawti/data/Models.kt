package com.abuomar.sawti.data

import androidx.annotation.StringRes
import com.abuomar.sawti.R

import kotlinx.serialization.Serializable

/**
 * نماذج البيانات — كلها قابلة للتسلسل إلى JSON وتُحفظ على الجهاز فقط.
 * لا يوجد أي سجل تجريبي (demo) ولا بيانات وهمية: المخزن يبدأ فارغاً.
 */

@Serializable
data class User(
    val email: String,
    val name: String,
    val salt: String,
    val digest: String,
    val iterations: Int,
    val algorithm: String,
    val createdAt: Long,
    val provider: String = "password",
)

enum class TxType(val id: String) {
    EXPENSE("expense"), INCOME("income"), DEBT("debt");

    companion object {
        fun of(id: String?): TxType = entries.firstOrNull { it.id == id } ?: EXPENSE
    }
}

/**
 * مصروف واحد.
 * @param amountNewMlps المبلغ بالليرة السورية الجديدة (وحدة التخزين الوحيدة)
 * @param date تاريخ مدني "yyyy-MM-dd" — لا يُحوَّل أبداً عبر UTC
 */
@Serializable
data class Transaction(
    val id: String,
    val ownerEmail: String,
    val amountNew: Long,
    val currency: String = "LSN",
    val type: String = TxType.EXPENSE.id,
    val category: String = Category.OTHER.id,
    val date: String,
    val merchant: String? = null,
    val note: String = "",
    val transcript: String? = null,
    val dateLabel: String? = null,
    val hasAudio: Boolean = false,
    val audioDurationSec: Int = 0,
    val listened: Boolean = false,
    val muted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val txType: TxType get() = TxType.of(type)
}

/** الفئات — نفس المجموعة في النسختين العربية والإنكليزية */
enum class Category(
    val id: String,
    /** معرّف نص اسم الفئة — R.string.cat_* */
    @StringRes val nameRes: Int,
    val colorHex: Long,
    val emoji: String,
) {
    FOOD("food", R.string.cat_food, 0xFFF97316, "🍽️"),
    TRANSPORT("transport", R.string.cat_transport, 0xFF3B82F6, "🚕"),
    BILLS("bills", R.string.cat_bills, 0xFF8B5CF6, "💡"),
    HEALTH("health", R.string.cat_health, 0xFFEF4444, "💊"),
    EDUCATION("education", R.string.cat_education, 0xFF14B8A6, "📚"),
    CLOTHING("clothing", R.string.cat_clothing, 0xFFEC4899, "👕"),
    HOME("home", R.string.cat_home, 0xFFF59E0B, "🏠"),
    FAMILY("family", R.string.cat_family, 0xFF22C55E, "👨‍👩‍👧"),
    GIFTS("gifts", R.string.cat_gifts, 0xFFA855F7, "🎁"),
    PERSONAL("personal", R.string.cat_personal, 0xFF0EA5E9, "💆"),
    BUSINESS("business", R.string.cat_business, 0xFF64748B, "🧰"),
    DEBT("debt", R.string.cat_debt, 0xFFDC2626, "🧾"),
    OTHER("other", R.string.cat_other, 0xFF9CA3AF, "📌");

    companion object {
        fun of(id: String?): Category = entries.firstOrNull { it.id == id } ?: OTHER

        /** كل الفئات القابلة للاختيار في الواجهة (بنفس ترتيب الإعلان) */
        val ALL_SELECTABLE: List<Category> get() = entries.toList()
    }
}

/** نتيجة تحليل جملة منطوقة بالعامية */
data class ParsedExpense(
    val raw: String,
    val amountNew: Long?,
    val amountRaw: Long,
    val amountUnit: AmountUnit,
    val amountMatchedText: String,
    val amountConfidence: Float,
    val category: Category,
    val type: TxType,
    val date: String,
    val dateLabel: String?,
    val merchant: String?,
    val needsConfirmation: Boolean,
    val confidence: Float,
)

enum class AmountUnit { NEW, LEGACY, OTHER }

/** ملخّص شهر للميزانية */
data class MonthSummary(
    val monthKey: String,
    val spent: Long,
    val income: Long,
    val budget: Long,
    val remaining: Long,
    val percent: Int,
    val overBudget: Boolean,
    val byCategory: List<Pair<Category, Long>>,
)
