package com.abuomar.sawti.core

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * تفضيلات المستخدم غير الحساسة:
 * اللغة، السمة، الميزانية الشهرية، وحدة العرض، وإعدادات الصوت
 * (كتم إعادة الصوت بعد التسجيل، نطق ردود المساعد، السرعة، الطبقة، لغة التعرّف).
 */
class PrefsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /* ------------------------------- اللغة ------------------------------- */
    var appLanguage: String
        get() = prefs.getString(KEY_LANG, DEFAULT_LANG) ?: DEFAULT_LANG
        set(value) {
            prefs.edit().putString(KEY_LANG, value).apply()
            // تطبيق اللغة فوراً بدون إعادة تشغيل التطبيق (Android 13+ وللأقدم عبر AppCompat)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(value))
        }

    val isArabic: Boolean get() = appLanguage.startsWith("ar")

    /* ------------------------------- السمة ------------------------------- */
    var themeMode: String
        get() = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(
            when (themeMode) {
                THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    /* -------------------------- الميزانية والعملة ------------------------ */
    /** الميزانية الشهرية — دائماً بالليرة السورية الجديدة */
    var monthlyBudget: Long
        get() = prefs.getLong(KEY_BUDGET, 0L)
        set(value) = prefs.edit().putLong(KEY_BUDGET, value).apply()

    /** العملة التي أدخل بها المستخدم ميزانيته: LSN (جديدة) أو SYP-legacy (قديمة) */
    var budgetCurrency: String
        get() = prefs.getString(KEY_BUDGET_CURRENCY, Currency.CODE_NEW) ?: Currency.CODE_NEW
        set(value) = prefs.edit().putString(KEY_BUDGET_CURRENCY, value).apply()

    /** وحدة العرض: new | legacy */
    var displayUnit: String
        get() = prefs.getString(KEY_DISPLAY_UNIT, Currency.Unit.NEW.id) ?: Currency.Unit.NEW.id
        set(value) = prefs.edit().putString(KEY_DISPLAY_UNIT, value).apply()

    fun displayUnitEnum(): Currency.Unit =
        if (displayUnit == Currency.Unit.LEGACY.id) Currency.Unit.LEGACY else Currency.Unit.NEW

    /* ------------------------------- الصوت ------------------------------- */
    /**
     * كتم إعادة الصوت بعد التسجيل:
     * عند التفعيل لا يُعاد نطق الكلام الذي قلته، ولا يُشغَّل تسجيلك تلقائياً
     * بعد الضغط على «إرسال» — يُحفظ بصمت في خانة الوارد.
     */
    var mutePlaybackAfterRecord: Boolean
        get() = prefs.getBoolean(KEY_MUTE_PLAYBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_MUTE_PLAYBACK, value).apply()

    /** نطق ردود المساعد بصوت واقعي بعد كل عملية */
    var speakResponses: Boolean
        get() = prefs.getBoolean(KEY_SPEAK_RESPONSES, true)
        set(value) = prefs.edit().putBoolean(KEY_SPEAK_RESPONSES, value).apply()

    var voiceRate: Float
        get() = prefs.getFloat(KEY_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_RATE, value.coerceIn(0.6f, 1.6f)).apply()

    var voicePitch: Float
        get() = prefs.getFloat(KEY_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_PITCH, value.coerceIn(0.6f, 1.5f)).apply()

    /** لغة التعرّف المختارة يدوياً؛ null = تلقائي حسب لغة الواجهة */
    var speechLanguage: String?
        get() = prefs.getString(KEY_SPEECH_LANG, null)
        set(value) = prefs.edit().apply {
            if (value.isNullOrBlank()) remove(KEY_SPEECH_LANG) else putString(KEY_SPEECH_LANG, value)
        }.apply()

    /**
     * هل يدعم هذا الجهاز التقاط الصوت بالتوازي مع محرك التعرّف؟
     * يُكتشف تلقائياً في أول محاولة (انظر AudioRecorder.concurrentCaptureSupported).
     */
    var concurrentCaptureSupported: Boolean
        get() = prefs.getBoolean(KEY_CONCURRENT_CAPTURE, true)
        set(value) = prefs.edit().putBoolean(KEY_CONCURRENT_CAPTURE, value).apply()

    /** حفظ المقطع الصوتي مع المصروف (يمكن إطفاءه لتوفير المساحة) */
    var saveAudioClip: Boolean
        get() = prefs.getBoolean(KEY_SAVE_CLIP, true)
        set(value) = prefs.edit().putBoolean(KEY_SAVE_CLIP, value).apply()

    fun clearAll() = prefs.edit().clear().apply()

    companion object {
        private const val FILE_NAME = "sawti_prefs"
        private const val KEY_LANG = "app_language"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_BUDGET = "monthly_budget_new"
        private const val KEY_BUDGET_CURRENCY = "budget_currency"
        private const val KEY_DISPLAY_UNIT = "display_unit"
        private const val KEY_MUTE_PLAYBACK = "mute_playback_after_record"
        private const val KEY_SPEAK_RESPONSES = "speak_responses"
        private const val KEY_RATE = "voice_rate"
        private const val KEY_PITCH = "voice_pitch"
        private const val KEY_SPEECH_LANG = "speech_language"
        private const val KEY_CONCURRENT_CAPTURE = "concurrent_capture_supported"
        private const val KEY_SAVE_CLIP = "save_audio_clip"

        const val DEFAULT_LANG = "ar"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }
}
