package com.abuomar.sawti.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * نطق ردود المساعد بصوت واقعي (TextToSpeech الحقيقي من النظام).
 *
 * اختيار الصوت: نبحث عن أفضل صوت عربي متاح بالترتيب
 *   ar-SY → ar-LB → ar-JO → ar-SA → ar-EG → أي ar
 * ونضبط السرعة والطبقة من الإعدادات. النطق أبطأ قليلاً (×0.97) وطبقة طبيعية
 * حتى يبدو الكلام أقرب للإنسان لا للآلة.
 *
 * يُحترم خيار «كتم إعادة الصوت بعد التسجيل»: عند تفعيله لا يُنطق كلام
 * المستخدم المسجَّل أبداً، ولا يُشغَّل تسجيله تلقائياً.
 */
class SpeechOutput(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var chosenVoice: TextToSpeech.Voice? = null
    var availableVoices: List<String> = emptyList()
        private set

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                configure()
            } else {
                ready = false
            }
        }
    }

    fun isReady(): Boolean = ready

    /** اسم المحرك والصوت المختار — يُعرض في الإعدادات */
    fun engineInfo(): String? {
        val engine = tts?.defaultEngine?.substringAfterLast('.')
        val voice = chosenVoice?.name ?: tts?.voice?.name
        return when {
            engine != null && voice != null -> "$engine · $voice"
            engine != null -> engine
            else -> null
        }
    }

    private fun configure() {
        val t = tts ?: return
        try {
            val voices = t.voices ?: emptySet()
            availableVoices = voices.mapNotNull { it.name }.sorted()
            val arabic = voices.filter { it.locale?.language == "ar" }
            val pool = if (arabic.isNotEmpty()) arabic else voices.toList()

            fun score(v: TextToSpeech.Voice): Int {
                var s = 0
                val tag = "${v.locale?.language ?: ""}-${v.locale?.country ?: ""}".lowercase()
                s += when {
                    tag.startsWith("ar-sy") -> 100
                    tag.startsWith("ar-lb") || tag.startsWith("ar-jo") -> 80
                    tag.startsWith("ar-sa") || tag.startsWith("ar-eg") -> 60
                    tag.startsWith("ar") -> 40
                    else -> 0
                }
                val name = v.name.lowercase()
                if ("google" in name) s += 30
                if (name.contains("natural") || name.contains("neural") ||
                    name.contains("enhanced") || name.contains("premium")) s += 25
                if (!v.isNetworkConnectionRequired) s += 6        // صوت على الجهاز = استجابة أسرع
                if (v.locale?.language == "ar" && v.locale?.country == "SY") s += 10
                return s
            }

            chosenVoice = pool.maxByOrNull { score(it) }
            chosenVoice?.let { runCatching { t.voice = it } }
            if (chosenVoice == null) runCatching { t.language = Locale("ar") }
        } catch (_: Throwable) {
            runCatching { t.language = Locale("ar") }
        }
    }

    /**
     * نطق جملة.
     * @param rate سرعة الصوت من الإعدادات
     * @param pitch طبقة الصوت من الإعدادات
     */
    fun speak(text: String, rate: Float = 1f, pitch: Float = 1f, onDone: (() -> Unit)? = null) {
        if (!ready || text.isBlank()) { onDone?.invoke(); return }
        val t = tts ?: return
        try {
            t.stop()
            // نطق أبطأ قليلاً وطبقة طبيعية = أقرب للكلام الحقيقي
            t.setSpeechRate(rate.coerceIn(0.5f, 2f) * 0.97f)
            t.setPitch(pitch.coerceIn(0.5f, 1.6f))
            if (onDone != null) {
                t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { onDone() }
                    override fun onDone(utteranceId: String?) { onDone() }
                })
            }
            t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sawti_${System.currentTimeMillis()}")
        } catch (_: Throwable) {
            onDone?.invoke()
        }
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }
}
