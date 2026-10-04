package com.abuomar.sawti.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.StringRes
import com.abuomar.sawti.R

/**
 * محرك التعرّف على الكلام — حقيقي ١٠٠٪ من نظام أندرويد.
 *
 * • يستخدم [SpeechRecognizer] مع RecognizerListener (نتائج جزئية لحظية تُكتب
 *   في خانة الإرسال مباشرة).
 * • إن لم يكن هناك خدمة تعرّف على الجهاز، يرجع إلى [RecognizerIntent]
 *   (تطبيق Google) وهو محرّك حقيقي أيضاً.
 * • يجرّب اللغات بالترتيب: ar-SY ثم ar-LB ثم ar-JO ثم ar — حتى يفهم العامية
 *   الشامية بأفضل دقة متاحة.
 *
 * لا يوجد أي نص مُصنَّع أو محاكاة: كل ما يظهر هو ما قاله المستخدم فعلاً.
 */
class SpeechToTextEngine(private val context: Context) {

    enum class State { IDLE, PREPARING, LISTENING, PROCESSING, ERROR }

    enum class SpeechError(@StringRes val messageRes: Int) {
        NO_MATCH(R.string.speech_err_no_match),
        SPEECH_TIMEOUT(R.string.speech_err_no_match),
        NETWORK(R.string.speech_err_network),
        AUDIO(R.string.speech_err_audio),
        PERMISSION(R.string.speech_err_permission),
        UNAVAILABLE(R.string.speech_err_unavailable),
        LANGUAGE_NOT_SUPPORTED(R.string.speech_err_language),
        BUSY(R.string.speech_err_busy),
        UNKNOWN(R.string.speech_err_no_match),
    }

    /** ترتيب اللغات المطلوب لمحرك التعرّف */
    fun languageChain(preferred: String?, arabic: Boolean): List<String> {
        val base = if (arabic) listOf("ar-SY", "ar-LB", "ar-JO", "ar") else listOf("en-US", "en-GB", "en")
        return if (preferred.isNullOrBlank()) base
        else listOf(preferred) + base.filterNot { it == preferred }
    }

    /** هل يوجد محرك تعرّف صوتي منصّب على الجهاز؟ */
    fun isRecognizerAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /** اسم خدمة التعرّف الفعلية (للعرض في الإعدادات) */
    fun recognizerServiceName(): String? {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        val resolved = context.packageManager.queryIntentServices(intent, 0)
        return resolved.firstOrNull()?.let {
            val name = it.loadLabel(context.packageManager)?.toString() ?: it.serviceInfo.name
            name.substringAfterLast('.')
        }
    }

    /** يبني Intent التعرّف الاحتياطي (عند غياب خدمة SpeechRecognizer) */
    fun buildFallbackIntent(lang: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_listening))
        }

    private var recognizer: SpeechRecognizer? = null
    private var chain: List<String> = emptyList()
    private var chainIndex = 0
    private var running = false
    private var finalText = StringBuilder()
    private var partialText = ""
    private var onLevel: ((Float) -> Unit)? = null
    private var onPartial: ((String) -> Unit)? = null
    private var onState: ((State) -> Unit)? = null
    private var onErrorCb: ((SpeechError) -> Unit)? = null
    private var onDone: ((String) -> Unit)? = null
    private var lastRms = 0f
    private var sawSpeech = false

    val isRunning: Boolean get() = running

    /** النص الكامل حتى اللحظة */
    fun transcript(): String = (finalText.toString() + if (partialText.isNotEmpty()) " $partialText" else "").trim()

    /**
     * بدء التعرّف. يجب أن يُستدعى من الخيط الرئيسي.
     */
    fun start(
        langChain: List<String>,
        onStateChange: (State) -> Unit,
        onPartialResult: (String) -> Unit,
        onRmsLevel: (Float) -> Unit,
        onSpeechError: (SpeechError) -> Unit,
        onFinished: (String) -> Unit,
    ) {
        cancelInternal(keepCallbacks = false)
        chain = langChain.ifEmpty { listOf("ar") }
        chainIndex = 0
        finalText = StringBuilder()
        partialText = ""
        lastRms = 0f
        sawSpeech = false
        onState = onStateChange
        onPartial = onPartialResult
        onLevel = onRmsLevel
        onErrorCb = onSpeechError
        onDone = onFinished
        running = true
        onStateChange(State.PREPARING)

        if (!isRecognizerAvailable()) {
            // لا توجد خدمة تعرّف — الواجهة تفتح Intent الاحتياطي
            running = false
            onStateChange(State.ERROR)
            onSpeechError(SpeechError.UNAVAILABLE)
            return
        }

        try {
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = sr
            sr.setRecognitionListener(listener)
            sr.startListening(buildIntent(chain[chainIndex]))
        } catch (e: Throwable) {
            running = false
            onStateChange(State.ERROR)
            onSpeechError(SpeechError.UNAVAILABLE)
        }
    }

    private fun buildIntent(lang: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // نطلب نتائج بالكلمات غير المفلترة حتى نفهم العامية كما هي
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { onState?.invoke(State.LISTENING) }
        override fun onBeginningOfSpeech() { sawSpeech = true; onState?.invoke(State.LISTENING) }

        override fun onRmsChanged(rmsdB: Float) {
            // rmsdB عادة بين -2 و 10 → نحوّله لمستوى 0..1 لرسم الموجات
            val level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            lastRms = rmsdB
            onLevel?.invoke(level)
        }

        override fun onBufferReceived(buffer: ByteArray?) { /* لا نستخدمه */ }
        override fun onEndOfSpeech() { onState?.invoke(State.PROCESSING) }

        override fun onError(error: Int) {
            val mapped = mapError(error)
            // اللغة غير مدعومة → جرّب اللغة التالية في السلسلة
            if ((error == ERROR_LANGUAGE_UNSUPPORTED || error == ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS)
                && chainIndex < chain.size - 1) {
                chainIndex++
                restartWithCurrentLang()
                return
            }
            // لا يوجد تطابق: إن ما صار كلام أصلاً، جرّب لغة تانية مرة وحدة
            if ((error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                && !sawSpeech && chainIndex < chain.size - 1
            ) {
                chainIndex++
                restartWithCurrentLang()
                return
            }
            finishWithError(mapped)
        }

        override fun onResults(results: Bundle?) {
            val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = list?.firstOrNull()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                if (finalText.isNotEmpty()) finalText.append(' ')
                finalText.append(text)
            }
            partialText = ""
            emit()
            finishOk()
        }

        override fun onPartialResults(partial: Bundle?) {
            val list = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = list?.firstOrNull()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                partialText = text
                if (!sawSpeech) sawSpeech = true
                emit()
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) { /* لا شيء */ }
    }

    private fun emit() {
        onPartial?.invoke(transcript())
    }

    private fun restartWithCurrentLang() {
        val sr = recognizer ?: return
        try {
            sr.cancel()
            sr.startListening(buildIntent(chain[chainIndex]))
        } catch (e: Throwable) {
            finishWithError(SpeechError.BUSY)
        }
    }

    private fun finishOk() {
        val text = transcript()
        stopInternal()
        onState?.invoke(State.IDLE)
        onDone?.invoke(text)
    }

    private fun finishWithError(err: SpeechError) {
        val text = transcript()
        stopInternal()
        onState?.invoke(State.ERROR)
        onErrorCb?.invoke(err)
        // إن كان عندنا نص جزئي مفيد، نسلّمه حتى لا يضيع كلام المستخدم
        if (text.isNotEmpty()) onDone?.invoke(text)
    }

    /** إيقاف طبيعي: يُنهي الجلسة ويُرجع النص الحالي */
    fun stop(): String {
        val text = transcript()
        try { recognizer?.stopListening() } catch (_: Throwable) {}
        stopInternal()
        onState?.invoke(State.PROCESSING)
        return text
    }

    /** إلغاء: يتجاهل كل شي */
    fun cancel() = cancelInternal(keepCallbacks = false)

    private fun cancelInternal(keepCallbacks: Boolean) {
        running = false
        partialText = ""
        try { recognizer?.cancel() } catch (_: Throwable) {}
        try { recognizer?.destroy() } catch (_: Throwable) {}
        recognizer = null
        if (!keepCallbacks) { onLevel = null; onPartial = null; onState = null; onErrorCb = null; onDone = null }
    }

    private fun stopInternal() {
        running = false
        try { recognizer?.destroy() } catch (_: Throwable) {}
        recognizer = null
        onLevel?.invoke(0f)
    }

    /** تحرير كامل — يُستدعى عند إنهاء الـViewModel */
    fun destroy() {
        cancelInternal(keepCallbacks = false)
    }

    private fun mapError(code: Int): SpeechError = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH -> SpeechError.NO_MATCH
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.SPEECH_TIMEOUT
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechError.NETWORK
        SpeechRecognizer.ERROR_AUDIO -> SpeechError.AUDIO
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.PERMISSION
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechError.BUSY
        SpeechRecognizer.ERROR_CLIENT -> SpeechError.UNKNOWN
        SpeechRecognizer.ERROR_SERVER -> SpeechError.NETWORK
        ERROR_SERVER_DISCONNECTED, ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS -> SpeechError.UNAVAILABLE
        ERROR_TOO_MANY_REQUESTS -> SpeechError.BUSY
        ERROR_LANGUAGE_UNSUPPORTED -> SpeechError.LANGUAGE_NOT_SUPPORTED
        else -> SpeechError.UNKNOWN
    }

    /** قراءة نتيجة Intent الاحتياطي */
    fun parseFallbackResult(data: Intent?): String? {
        val list = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        return list?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }

    companion object {
        /* ثوابت أخطاء أُضيفت في API 33 — نعرّفها كأرقام حتى يعمل التطبيق
           على minSdk 24 بدون مشاكل توافق أو تحذيرات NewApi.
           القيم مطابقة تماماً لـ android.speech.SpeechRecognizer: */
        private const val ERROR_TOO_MANY_REQUESTS = 10
        private const val ERROR_SERVER_DISCONNECTED = 11
        private const val ERROR_LANGUAGE_UNSUPPORTED = 12
        private const val ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS = 15
    }
}
