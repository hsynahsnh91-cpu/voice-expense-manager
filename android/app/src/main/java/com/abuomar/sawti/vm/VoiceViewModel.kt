package com.abuomar.sawti.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abuomar.sawti.R
import com.abuomar.sawti.SawtiApp
import com.abuomar.sawti.core.CivilDate
import com.abuomar.sawti.core.Currency
import com.abuomar.sawti.data.AmountUnit
import com.abuomar.sawti.data.Category
import com.abuomar.sawti.data.ParsedExpense
import com.abuomar.sawti.data.Transaction
import com.abuomar.sawti.data.TxType
import com.abuomar.sawti.domain.ArabicParser
import com.abuomar.sawti.speech.AudioRecorder
import com.abuomar.sawti.speech.SpeechToTextEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel لإدارة عملية تسجيل الصوت وتحويله إلى نص باستخدام مكتبة
 * [SpeechToTextEngine] (المبنية على android.speech.SpeechRecognizer)،
 * مع دمجها بطلب إذن الميكروفون المُعدّ مسبقاً عبر Accompanist Permissions.
 *
 * التدفق الكامل:
 *  ١) الشاشة تفحص الإذن عبر rememberPermissionState(RECORD_AUDIO)
 *     وتستدعي [onPermissionResult] بعد كل محاولة.
 *  ٢) عند منح الإذن → [startListening] يشغّل SpeechRecognizer بالتوازي مع
 *     AudioRecorder لحفظ المقطع في «الوارد».
 *  ٣) النتائج الجزئية تُكتب لحظياً في خانة الإرسال ([transcript]).
 *  ٤) [ArabicParser] يفهم العامية: المبلغ (بالليرة السورية الجديدة)، الفئة،
 *     التاريخ المدني، المكان، والنوع.
 *  ٥) [send] يحفظ المصروف، وينطق رد المساعد (إن كان النطق مفعلاً)، ويحترم
 *     خيار «كتم إعادة الصوت بعد التسجيل» فلا يُعاد كلام المستخدم أبداً.
 */
class VoiceViewModel(app: Application) : AndroidViewModel(app) {

    private val container = SawtiApp.container()
    private val repo = container.repository
    private val prefs = container.prefsStore
    private val speechOutput = container.speechOutput

    val engine = SpeechToTextEngine(container.appContext)
    private val recorder = AudioRecorder(container.appContext)

    /* ------------------------------- الحالة ------------------------------- */

    enum class Phase {
        IDLE,                 // جاهز للتسجيل
        NEEDS_PERMISSION,     // بانتظار إذن الميكروفون
        PERMISSION_DENIED,    // رُفض الإذن
        LISTENING,            // الميكروفون مفتوح ويستمع
        PROCESSING,           // يحوّل الصوت لنص
        ERROR,                // خطأ من المحرك
        UNSUPPORTED,          // لا يوجد محرك تعرّف على الجهاز
    }

    data class UiState(
        val phase: Phase = Phase.IDLE,
        val transcript: String = "",
        val parsed: ParsedExpense? = null,
        val level: Float = 0f,
        val errorRes: Int? = null,
        val engineLabel: String? = null,
        val engineAvailable: Boolean = true,
        val micPermissionGranted: Boolean = false,
        val showRationale: Boolean = false,
        val saving: Boolean = false,
        val audioSaved: Boolean = false,
        val audioDuration: Int = 0,
        val lastSavedId: String? = null,
        val toastRes: Int? = null,
        val toastText: String? = null,
        val manualAmountNeeded: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** النص الحي المعروض في خانة الإرسال */
    val transcript: StateFlow<String> = _state.asStateFlow()

    private var tempAudioFile: File? = null

    init {
        viewModelScope.launch {
            repo.bootstrap()
            val available = engine.isRecognizerAvailable()
            _state.update {
                it.copy(
                    engineAvailable = available,
                    engineLabel = engine.recognizerServiceName(),
                    phase = if (available) Phase.IDLE else Phase.UNSUPPORTED,
                )
            }
        }
    }

    /* ==================== إذن الميكروفون (Accompanist) ==================== */

    /**
     * تُستدعى من الشاشة بعد فحص/طلب الإذن.
     * @param granted هل مُنح الإذن؟
     * @param shouldShowRationale هل نعرض سبب الحاجة للإذن؟
     */
    fun onPermissionResult(granted: Boolean, shouldShowRationale: Boolean = false) {
        _state.update {
            it.copy(
                micPermissionGranted = granted,
                showRationale = !granted && shouldShowRationale,
                phase = when {
                    granted && it.phase == Phase.NEEDS_PERMISSION -> Phase.IDLE
                    granted -> it.phase
                    else -> if (it.phase == Phase.LISTENING || it.phase == Phase.PROCESSING) it.phase
                            else Phase.PERMISSION_DENIED
                },
            )
        }
    }

    fun markNeedsPermission() {
        _state.update { it.copy(phase = Phase.NEEDS_PERMISSION) }
    }

    fun clearPermissionUi() {
        _state.update { it.copy(showRationale = false, phase = if (it.micPermissionGranted) Phase.IDLE else it.phase) }
    }

    /* ============================ بدء/إيقاف الاستماع ====================== */

    /** يبدأ التعرّف الصوتي. يفترض أن الإذن ممنوح (تتحقق منه الشاشة أولاً). */
    fun startListening() {
        val s = _state.value
        if (s.phase == Phase.LISTENING || s.phase == Phase.PROCESSING) return
        if (!s.micPermissionGranted) { _state.update { it.copy(phase = Phase.NEEDS_PERMISSION) }; return }
        if (!s.engineAvailable) { _state.update { it.copy(phase = Phase.UNSUPPORTED) }; return }

        _state.update { it.copy(errorRes = null, level = 0f, audioSaved = false, audioDuration = 0) }

        // ١) تسجيل المقطع الصوتي بالتوازي (إن كان الجهاز يدعمه والإعداد مفعّل)
        tempAudioFile = null
        if (prefs.saveAudioClip && prefs.concurrentCaptureSupported) {
            tempAudioFile = recorder.start()
        }

        // ٢) محرّك التعرّف الحقيقي
        val chain = engine.languageChain(prefs.speechLanguage, prefs.isArabic)
        engine.start(
            langChain = chain,
            onStateChange = { st ->
                _state.update {
                    it.copy(
                        phase = when (st) {
                            SpeechToTextEngine.State.PREPARING -> Phase.PROCESSING
                            SpeechToTextEngine.State.LISTENING -> Phase.LISTENING
                            SpeechToTextEngine.State.PROCESSING -> Phase.PROCESSING
                            SpeechToTextEngine.State.ERROR -> Phase.ERROR
                            SpeechToTextEngine.State.IDLE -> Phase.IDLE
                        }
                    )
                }
            },
            onPartialResult = { text -> onTranscript(text) },
            onRmsLevel = { level -> _state.update { it.copy(level = level) } },
            onSpeechError = { err ->
                val audioConcurrentFailed = err == SpeechToTextEngine.SpeechError.AUDIO &&
                    prefs.saveAudioClip && prefs.concurrentCaptureSupported
                if (audioConcurrentFailed) {
                    // الميكروفون محجوز للتسجيل المتوازي → نوقف التسجيل ونعيد المحاولة بدونه
                    recorder.cancel()
                    prefs.concurrentCaptureSupported = false
                    tempAudioFile = null
                    _state.update { it.copy(phase = Phase.PROCESSING, level = 0f) }
                    viewModelScope.launch {
                        delay(250)
                        retryWithoutRecording(chain)
                    }
                } else {
                    _state.update { it.copy(phase = Phase.ERROR, errorRes = err.messageRes, level = 0f) }
                    finishAudioQuietly()
                }
            },
            onFinished = { text ->
                _state.update { it.copy(level = 0f) }
                finishAudioQuietly()
                if (text.isBlank() && _state.value.transcript.isBlank()) {
                    _state.update { it.copy(phase = Phase.IDLE, errorRes = R.string.voice_no_result) }
                } else {
                    _state.update { it.copy(phase = Phase.IDLE, errorRes = null) }
                }
            },
        )
    }

    private fun retryWithoutRecording(chain: List<String>) {
        engine.start(
            langChain = chain,
            onStateChange = { st ->
                _state.update {
                    it.copy(phase = if (st == SpeechToTextEngine.State.LISTENING) Phase.LISTENING
                    else if (st == SpeechToTextEngine.State.IDLE) Phase.IDLE else Phase.PROCESSING)
                }
            },
            onPartialResult = { text -> onTranscript(text) },
            onRmsLevel = { level -> _state.update { it.copy(level = level) } },
            onSpeechError = { err -> _state.update { it.copy(phase = Phase.ERROR, errorRes = err.messageRes) } },
            onFinished = { _ ->
                finishAudioQuietly()
                _state.update { it.copy(phase = Phase.IDLE, level = 0f) }
            },
        )
    }

    /** إيقاف طبيعي — يُنهي التعرّف ويحفظ المقطع ويحلّل النص */
    fun stopListening() {
        if (_state.value.phase != Phase.LISTENING && _state.value.phase != Phase.PROCESSING) return
        _state.update { it.copy(phase = Phase.PROCESSING) }
        val text = engine.stop()
        finishAudioQuietly()
        if (text.isNotBlank()) onTranscript(text)
        _state.update { it.copy(phase = Phase.IDLE) }
    }

    /** إلغاء — يتجاهل النص والتسجيل */
    fun cancelListening() {
        engine.cancel()
        recorder.cancel()
        tempAudioFile = null
        _state.update {
            it.copy(phase = Phase.IDLE, transcript = "", parsed = null, level = 0f,
                errorRes = null, audioSaved = false, audioDuration = 0, manualAmountNeeded = false)
        }
    }

    private fun finishAudioQuietly() {
        val file = recorder.stop()
        tempAudioFile = file
        _state.update {
            it.copy(
                audioSaved = file != null,
                audioDuration = if (file != null) recorder.durationSeconds() else 0,
            )
        }
        // إن كان التسجيل صامتاً → الالتقاط المتوازي غير مدعوم على هذا الجهاز
        if (file == null && prefs.saveAudioClip && recorder.wasSilentCapture()) {
            prefs.concurrentCaptureSupported = false
        }
    }

    /* =========================== النص والفهم ============================== */

    /** أي نص (من المحرك أو من كتابة المستخدم) يُحلَّل فوراً */
    fun onTranscript(text: String) {
        val clean = text.trim()
        val parsed = if (clean.isEmpty()) null else ArabicParser.parse(clean, CivilDate.today())
        _state.update {
            it.copy(
                transcript = clean,
                parsed = parsed,
                manualAmountNeeded = parsed?.amountNew == null && clean.isNotEmpty(),
                errorRes = if (clean.isNotEmpty()) null else it.errorRes,
            )
        }
    }

    fun onManualEdit(text: String) = onTranscript(text)

    /** تعديل الفئة يدوياً قبل الإرسال */
    fun setCategory(cat: Category) {
        val p = _state.value.parsed ?: return
        _state.update { it.copy(parsed = p.copy(category = cat)) }
    }

    fun setDate(civil: String) {
        if (!CivilDate.isValid(civil)) return
        val p = _state.value.parsed ?: return
        _state.update { it.copy(parsed = p.copy(date = civil, dateLabel = null)) }
    }

    fun setMerchant(name: String) {
        val p = _state.value.parsed ?: return
        _state.update { it.copy(parsed = p.copy(merchant = name.ifBlank { null })) }
    }

    fun setType(type: TxType) {
        val p = _state.value.parsed ?: return
        _state.update { it.copy(parsed = p.copy(type = type)) }
    }

    /** قلب وحدة المبلغ: قديمة ÷١٠٠ ↔ جديدة */
    fun toggleAmountUnit() {
        val p = _state.value.parsed ?: return
        val raw = p.amountRaw
        if (raw <= 0L) return
        val (newUnit, newValue) = if (p.amountUnit == AmountUnit.LEGACY) {
            AmountUnit.NEW to raw
        } else {
            AmountUnit.LEGACY to Currency.legacyToNew(raw)
        }
        _state.update {
            it.copy(parsed = p.copy(amountUnit = newUnit, amountNew = newValue, needsConfirmation = false))
        }
    }

    /** إدخال المبلغ يدوياً عندما لا يفهمه المحرك من الكلام */
    fun setManualAmount(text: String) {
        val value = Currency.parseAmount(text)
        if (value <= 0L) return
        val p = _state.value.parsed
        val amountNew = if (prefs.displayUnit == Currency.Unit.LEGACY.id) Currency.legacyToNew(value) else value
        if (p == null) {
            val fresh = ParsedExpense(
                raw = _state.value.transcript,
                amountNew = amountNew,
                amountRaw = value,
                amountUnit = AmountUnit.NEW,
                amountMatchedText = text,
                amountConfidence = 1f,
                category = Category.OTHER,
                type = TxType.EXPENSE,
                date = CivilDate.today(),
                dateLabel = null,
                merchant = null,
                needsConfirmation = false,
                confidence = 0.9f,
            )
            _state.update { it.copy(parsed = fresh, manualAmountNeeded = false) }
        } else {
            _state.update {
                it.copy(
                    parsed = p.copy(
                        amountNew = amountNew, amountRaw = value,
                        amountUnit = AmountUnit.NEW, needsConfirmation = false, amountConfidence = 1f,
                    ),
                    manualAmountNeeded = false,
                )
            }
        }
    }

    /* ================================ الإرسال ============================= */

    /** حفظ المصروف + نطق رد المساعد (مع احترام خيار الكتم) */
    fun send(onSaved: (Transaction) -> Unit = {}) {
        val s = _state.value
        if (s.saving) return
        val text = s.transcript.trim()
        if (text.isEmpty()) {
            _state.update { it.copy(toastRes = R.string.voice_no_result) }
            return
        }
        val p = s.parsed ?: ArabicParser.parse(text)
        val amountNew = p?.amountNew ?: 0L
        if (amountNew <= 0L) {
            _state.update { it.copy(manualAmountNeeded = true, toastRes = R.string.voice_no_amount) }
            return
        }

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val tx = repo.addTransaction(
                amountNew = amountNew,
                category = p?.category ?: Category.OTHER,
                type = p?.type ?: TxType.EXPENSE,
                date = p?.date ?: CivilDate.today(),
                merchant = p?.merchant,
                note = "",
                transcript = text,
                dateLabel = p?.dateLabel,
                audioFile = tempAudioFile,
                audioDurationSec = s.audioDuration,
                // الكتم: لا يُعاد نطق كلام المستخدم ولا يُشغَّل تسجيله تلقائياً
                muted = prefs.mutePlaybackAfterRecord,
            )
            tempAudioFile = null

            _state.update {
                it.copy(
                    saving = false,
                    transcript = "",
                    parsed = null,
                    manualAmountNeeded = false,
                    audioSaved = false,
                    audioDuration = 0,
                    level = 0f,
                    lastSavedId = tx.id,
                    toastText = getApplication<Application>()
                        .getString(R.string.voice_saved_toast, displayAmount(tx.amountNew)),
                )
            }
            onSaved(tx)
            announceIfEnabled(tx)
        }
    }

    /**
     * نطق ردّ المساعد بصوت واقعي.
     *
     * • «كتم إعادة الصوت بعد التسجيل» = لا نُعيد كلام المستخدم إطلاقاً.
     * • «نطق ردود المساعد» = يتحكم بجملة المساعد فقط.
     */
    private fun announceIfEnabled(tx: Transaction) {
        if (!prefs.speakResponses) return
        val arabic = prefs.isArabic
        val category = Category.of(tx.category)
        val catName = getApplication<Application>().getString(category.nameRes)
        val amountSpoken = Currency.spoken(tx.amountNew, arabic)

        val summary = repo.summary.value
        val tail = when {
            summary.budget <= 0L -> ""
            summary.overBudget -> getApplication<Application>().getString(R.string.assistant_over_budget)
            else -> getApplication<Application>().getString(
                R.string.assistant_remaining,
                Currency.format(summary.remaining.coerceAtLeast(0), Currency.Unit.NEW, arabic, withSymbol = false),
            )
        }
        val line = getApplication<Application>().getString(R.string.assistant_reply_added, amountSpoken, catName, tail)

        // خيار الكتم يمنع أي إعادة لصوت المستخدم — جملة المساعد فقط تُنطق
        if (prefs.mutePlaybackAfterRecord) {
            // لا نُشغّل تسجيل المستخدم، وننطق رد المساعد فقط إن سمح المستخدم بالنطق
            speechOutput.speak(line, prefs.voiceRate, prefs.voicePitch)
        } else {
            speechOutput.speak(line, prefs.voiceRate, prefs.voicePitch)
        }
    }

    /** معاينة صوتية للمساعدة في ضبط السرعة/الطبقة من الإعدادات */
    fun testVoice() {
        speechOutput.speak(
            getApplication<Application>().getString(R.string.settings_test_voice_text),
            prefs.voiceRate, prefs.voicePitch,
        )
    }

    fun consumeToast() = _state.update { it.copy(toastRes = null, toastText = null) }
    fun consumeError() = _state.update { it.copy(errorRes = null) }

    private fun displayAmount(valueNew: Long): String =
        Currency.format(valueNew, prefs.displayUnitEnum(), prefs.isArabic)

    /** نتيجة Intent التعرّف الاحتياطي (عند غياب خدمة SpeechRecognizer) */
    fun onFallbackResult(text: String?) {
        if (text.isNullOrBlank()) {
            _state.update { it.copy(phase = Phase.IDLE, errorRes = R.string.voice_no_result) }
            return
        }
        onTranscript(text)
        _state.update { it.copy(phase = Phase.IDLE, errorRes = null) }
    }

    fun refreshEngineStatus() {
        val available = engine.isRecognizerAvailable()
        _state.update {
            it.copy(
                engineAvailable = available,
                engineLabel = engine.recognizerServiceName(),
                phase = if (!available) Phase.UNSUPPORTED else if (it.phase == Phase.UNSUPPORTED) Phase.IDLE else it.phase,
            )
        }
    }

    override fun onCleared() {
        engine.destroy()
        recorder.cancel()
        tempAudioFile?.delete()
        super.onCleared()
    }

    companion object {
        /** اسم الفئة كنص — يُستعمل في نطق ردّ المساعد */
        fun categoryName(context: android.content.Context, c: Category): String =
            context.getString(c.nameRes)
    }
}
