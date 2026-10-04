package com.abuomar.sawti.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abuomar.sawti.R
import com.abuomar.sawti.SawtiApp
import com.abuomar.sawti.core.CivilDate
import com.abuomar.sawti.core.Currency
import com.abuomar.sawti.core.PrefsStore
import com.abuomar.sawti.data.Category
import com.abuomar.sawti.data.Transaction
import com.abuomar.sawti.data.TxType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel مشترك لشاشات: المصاريف، الوارد، والإعدادات.
 * يدير الفلترة بالفترة (تواريخ مدنية yyyy-MM-dd)، والفئة، والبحث،
 * وكل التفضيلات (اللغة، السمة، وحدة العرض، كتم الصوت، النطق، السرعة، الطبقة).
 */
class DataViewModel(app: Application) : AndroidViewModel(app) {

    private val container = SawtiApp.container()
    private val repo = container.repository
    private val prefs = container.prefsStore

    /* ------------------------------ الفلترة ------------------------------ */

    data class Filters(
        val from: String = CivilDate.startOfMonth(CivilDate.today()),
        val to: String = CivilDate.today(),
        val category: String = "all",
        val search: String = "",
        val active: Boolean = true,
    )

    private val _filters = MutableStateFlow(Filters())
    val filters: StateFlow<Filters> = _filters.asStateFlow()

    /* ------------------------------ التفضيلات ----------------------------- */

    data class Prefs(
        val language: String = PrefsStore.DEFAULT_LANG,
        val arabic: Boolean = true,
        val themeMode: String = PrefsStore.THEME_SYSTEM,
        val displayUnit: Currency.Unit = Currency.Unit.NEW,
        val monthlyBudget: Long = 0L,
        val budgetCurrency: String = Currency.CODE_NEW,
        val mutePlaybackAfterRecord: Boolean = true,
        val speakResponses: Boolean = true,
        val voiceRate: Float = 1f,
        val voicePitch: Float = 1f,
        val speechLanguage: String? = null,
        val saveAudioClip: Boolean = true,
        val concurrentCaptureSupported: Boolean = true,
    )

    private val _prefs = MutableStateFlow(readPrefs())
    val prefsState: StateFlow<Prefs> = _prefs.asStateFlow()

    private fun readPrefs() = Prefs(
        language = prefs.appLanguage,
        arabic = prefs.isArabic,
        themeMode = prefs.themeMode,
        displayUnit = prefs.displayUnitEnum(),
        monthlyBudget = prefs.monthlyBudget,
        budgetCurrency = prefs.budgetCurrency,
        mutePlaybackAfterRecord = prefs.mutePlaybackAfterRecord,
        speakResponses = prefs.speakResponses,
        voiceRate = prefs.voiceRate,
        voicePitch = prefs.voicePitch,
        speechLanguage = prefs.speechLanguage,
        saveAudioClip = prefs.saveAudioClip,
        concurrentCaptureSupported = prefs.concurrentCaptureSupported,
    )

    fun reloadPrefs() { _prefs.value = readPrefs() }

    /* ------------------------------- البيانات ----------------------------- */

    val transactions: StateFlow<List<Transaction>> = repo.transactions
    val summary = repo.summary
    val unheardCount: StateFlow<Int> = repo.unheardCount
    val currentUser = repo.currentUser

    /** المصاريف بعد تطبيق الفلاتر */
    val filtered: StateFlow<List<Transaction>> = combine(transactions, _filters) { all, f ->
        if (!f.active) all
        else repo.query(
            from = f.from,
            to = f.to,
            category = f.category,
            search = f.search,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** المصاريف المصنّفة حسب اليوم للعرض */
    val grouped: StateFlow<List<Pair<String, List<Transaction>>>> =
        filtered.map { list -> repo.groupedByDate(list) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** تسجيلات الوارد (الأحدث أولاً) */
    val inbox: StateFlow<List<Transaction>> = transactions.map { all ->
        val email = repo.currentUser.value?.email
        all.filter { it.ownerEmail == email && it.hasAudio }
            .sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repo.bootstrap() }
    }

    fun refresh() = viewModelScope.launch { repo.bootstrap() }

    /* ------------------------------ الفلاتر ------------------------------ */

    fun setRange(from: String?, to: String?) {
        _filters.update { it.copy(from = from ?: "", to = to ?: "", active = from != null) }
    }

    fun applyPreset(preset: Preset) {
        val today = CivilDate.today()
        when (preset) {
            Preset.THIS_MONTH -> _filters.update {
                it.copy(from = CivilDate.startOfMonth(today), to = today, active = true)
            }
            Preset.LAST_MONTH -> {
                val prev = CivilDate.plusMonths(today, -1)
                _filters.update {
                    it.copy(from = CivilDate.startOfMonth(prev), to = CivilDate.endOfMonth(prev), active = true)
                }
            }
            Preset.THIS_WEEK -> {
                val ws = CivilDate.weekStart(prefs.isArabic)
                val todayWd = CivilDate.weekday(today)
                val back = ((todayWd - ws) % 7 + 7) % 7
                _filters.update { it.copy(from = CivilDate.plusDays(today, -back), to = today, active = true) }
            }
            Preset.ALL_TIME -> _filters.update { it.copy(active = false, from = "", to = "") }
            Preset.TODAY -> _filters.update { it.copy(from = today, to = today, active = true) }
        }
    }

    fun setCategory(id: String) = _filters.update { it.copy(category = id) }
    fun setSearch(q: String) = _filters.update { it.copy(search = q) }

    enum class Preset { TODAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, ALL_TIME }

    /* --------------------------- تعديل المصاريف --------------------------- */

    fun saveTransaction(
        existingId: String?,
        amountNew: Long,
        category: Category,
        type: TxType,
        date: String,
        merchant: String?,
        note: String,
    ) = viewModelScope.launch {
        if (amountNew <= 0L) return@launch
        if (existingId == null) {
            repo.addTransaction(
                amountNew = amountNew, category = category, type = type, date = date,
                merchant = merchant, note = note, muted = prefs.mutePlaybackAfterRecord,
            )
        } else {
            repo.updateTransaction(existingId) {
                copy(
                    amountNew = amountNew, category = category.id, type = type.id,
                    date = date, merchant = merchant?.takeIf { it.isNotBlank() }, note = note,
                )
            }
        }
    }

    fun deleteTransaction(id: String) = viewModelScope.launch { repo.deleteTransaction(id) }

    fun markListened(id: String) = viewModelScope.launch { repo.markListened(id) }

    fun audioFileFor(id: String): File? = repo.audioFileFor(id)

    /* ------------------------------ التفضيلات ----------------------------- */

    fun setLanguage(tag: String) {
        prefs.appLanguage = tag
        reloadPrefs()
    }

    fun setTheme(mode: String) {
        prefs.themeMode = mode
        prefs.applyTheme()
        reloadPrefs()
    }

    fun setDisplayUnit(unit: Currency.Unit) {
        prefs.displayUnit = unit.id
        reloadPrefs()
    }

    fun setMutePlayback(value: Boolean) {
        prefs.mutePlaybackAfterRecord = value
        if (value) container.speechOutput.stop()
        reloadPrefs()
    }

    fun setSpeakResponses(value: Boolean) {
        prefs.speakResponses = value
        reloadPrefs()
    }

    fun setVoiceRate(value: Float) { prefs.voiceRate = value; reloadPrefs() }
    fun setVoicePitch(value: Float) { prefs.voicePitch = value; reloadPrefs() }
    fun setSpeechLanguage(value: String?) { prefs.speechLanguage = value; reloadPrefs() }

    fun setSaveAudioClip(value: Boolean) { prefs.saveAudioClip = value; reloadPrefs() }

    fun setBudget(rawAmount: Long, currency: String) = viewModelScope.launch {
        val budgetNew = if (currency == BudgetViewModel.CURRENCY_LEGACY) Currency.legacyToNew(rawAmount) else rawAmount
        prefs.monthlyBudget = budgetNew.coerceAtLeast(0L)
        prefs.budgetCurrency = currency
        repo.bootstrap()
        reloadPrefs()
    }

    fun testVoice() {
        container.speechOutput.speak(
            getApplication<Application>().getString(R.string.settings_test_voice_text),
            prefs.voiceRate, prefs.voicePitch,
        )
    }

    /* -------------------------------- البيانات ---------------------------- */

    suspend fun exportJson(): String {
        val email = repo.currentUser.value?.email ?: return "{}"
        val txs = repo.query(null, null, null, null).filter { it.ownerEmail == email }
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"app\": \"sawti\",\n")
        sb.append("  \"version\": \"${SawtiApp.VERSION_NAME}\",\n")
        sb.append("  \"exportedAt\": ${System.currentTimeMillis()},\n")
        sb.append("  \"currency\": \"LSN (New Syrian Pound)\",\n")
        sb.append("  \"monthlyBudgetNew\": ${prefs.monthlyBudget},\n")
        sb.append("  \"transactions\": [\n")
        txs.forEachIndexed { i, t ->
            sb.append("    {")
            sb.append("\"id\":\"${esc(t.id)}\",")
            sb.append("\"amountNew\":${t.amountNew},")
            sb.append("\"currency\":\"${t.currency}\",")
            sb.append("\"type\":\"${t.type}\",")
            sb.append("\"category\":\"${t.category}\",")
            sb.append("\"date\":\"${t.date}\",")
            sb.append("\"merchant\":${t.merchant?.let { "\"${esc(it)}\"" } ?: "null"},")
            sb.append("\"note\":\"${esc(t.note)}\",")
            sb.append("\"transcript\":${t.transcript?.let { "\"${esc(it)}\"" } ?: "null"},")
            sb.append("\"hasAudio\":${t.hasAudio},")
            sb.append("\"audioDurationSec\":${t.audioDurationSec},")
            sb.append("\"listened\":${t.listened}")
            sb.append("}")
            if (i < txs.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n")
        sb.append("  \"audioClips\": ${txs.count { it.hasAudio }},\n")
        sb.append("  \"note\": \"Recordings are stored on-device only and are not included in this export.\"\n")
        sb.append("}\n")
        return sb.toString()
    }

    private fun esc(s: String): String = s
        .replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", " ").replace("\r", " ").replace("\t", " ")

    fun clearAllTransactions() = viewModelScope.launch { repo.clearAllTransactions() }

    fun signOut() = repo.signOut()

    fun deleteAccountAndAll() = viewModelScope.launch {
        repo.deleteAccountAndData()
        container.jsonStore.wipeEverything()
    }

    /** المساحة المستخدمة بالتسجيلات */
    fun audioStorageBytes(): Long = container.jsonStore.audioBytesTotal()

    fun formatMoney(valueNew: Long): String =
        Currency.format(valueNew, prefs.displayUnitEnum(), prefs.isArabic)

    fun formatPlain(valueNew: Long): Long =
        if (prefs.displayUnit == Currency.Unit.LEGACY.id) Currency.newToLegacy(valueNew) else valueNew
}
