package com.abuomar.sawti.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abuomar.sawti.R
import com.abuomar.sawti.SawtiApp
import com.abuomar.sawti.core.Currency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel لشاشة «ما ميزانيتك؟» — تظهر بعد إنشاء الحساب أو تسجيل الدخول.
 * الميزانية وعملة الميزانية (ليرة سورية جديدة / قديمة) مع تحويل صحيح.
 */
class BudgetViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = SawtiApp.container().prefsStore

    data class UiState(
        val amountText: String = "",
        val currency: String = Currency.CODE_NEW,      // LSN | SYP-legacy
        val busy: Boolean = false,
        val errorRes: Int? = null,
        val convertedText: String? = null,             // معاينة التحويل لليرة الجديدة
        val done: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** المبالغ السريعة المعروضة كأزرار اختيار — بالوحدة المختارة */
    val quickAmountsNew: List<Long> = listOf(500_000, 1_000_000, 2_000_000, 5_000_000, 10_000_000)
    val quickAmountsLegacy: List<Long> = listOf(100_000, 500_000, 1_000_000, 2_500_000, 5_000_000)

    init {
        val saved = prefs.monthlyBudget
        val unit = prefs.budgetCurrency
        _state.update {
            it.copy(
                amountText = if (saved > 0) {
                    if (unit == CURRENCY_LEGACY) Currency.newToLegacy(saved).toString() else saved.toString()
                } else "",
                currency = unit,
            )
        }
        recomputePreview()
    }

    /** يقبل الأرقام فقط (لاتينية أو عربية-هندية) */
    fun onAmountChange(raw: String) {
        val digits = Currency.normalizeDigits(raw).filter { it.isDigit() }.take(15)
        _state.update { it.copy(amountText = digits, errorRes = null) }
        recomputePreview()
    }

    fun onCurrencyChange(value: String) {
        _state.update { it.copy(currency = value, errorRes = null) }
        recomputePreview()
    }

    fun pickQuick(value: Long) {
        _state.update { it.copy(amountText = value.toString(), errorRes = null) }
        recomputePreview()
    }

    private fun recomputePreview() {
        val s = _state.value
        val raw = Currency.parseAmount(s.amountText)
        val converted = if (raw > 0 && s.currency == CURRENCY_LEGACY) Currency.legacyToNew(raw) else null
        _state.update {
            it.copy(
                convertedText = converted?.let { c ->
                    getApplication<Application>().getString(
                        R.string.budget_converted,
                        Currency.format(c, Currency.Unit.NEW, prefs.isArabic),
                    )
                }
            )
        }
    }

    /** حفظ الميزانية والدخول للتطبيق */
    fun save() {
        val s = _state.value
        if (s.busy) return
        val raw = Currency.parseAmount(s.amountText)
        if (raw <= 0L) {
            _state.update { it.copy(errorRes = R.string.err_amount) }
            return
        }
        if (raw > Currency.MAX_AMOUNT) {
            _state.update { it.copy(errorRes = R.string.err_amount_range) }
            return
        }
        _state.update { it.copy(busy = true, errorRes = null) }
        viewModelScope.launch {
            val budgetNew = if (s.currency == CURRENCY_LEGACY) Currency.legacyToNew(raw) else raw
            prefs.monthlyBudget = budgetNew.coerceAtLeast(1L)
            prefs.budgetCurrency = s.currency
            prefs.displayUnit = Currency.Unit.NEW.id     // العرض الافتراضي دائماً بالجديدة
            _state.update { it.copy(busy = false, done = true) }
        }
    }

    /** تعديل الميزانية من شاشة الإعدادات (بدون إغلاق التطبيق) */
    fun updateBudgetFromSettings(raw: Long, currency: String, onDone: (Long) -> Unit) {
        if (raw <= 0L) return
        val budgetNew = if (currency == CURRENCY_LEGACY) Currency.legacyToNew(raw) else raw
        prefs.monthlyBudget = budgetNew
        prefs.budgetCurrency = currency
        viewModelScope.launch {
            SawtiApp.container().repository.bootstrap()
            onDone(budgetNew)
        }
    }

    companion object {
        const val CURRENCY_LEGACY = "SYP-legacy"
    }
}
