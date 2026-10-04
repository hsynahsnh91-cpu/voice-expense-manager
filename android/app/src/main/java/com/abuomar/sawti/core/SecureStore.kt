package com.abuomar.sawti.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * تخزين مشفّر للحسابات والجلسة.
 *
 * يستخدم EncryptedSharedPreferences بمفتاح رئيسي من Android Keystore
 * (MasterKey / AES256_GCM). البصمة والملح لا يغادران الجهاز أبداً.
 */
class SecureStore(context: Context) {

    private val prefs: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse {
        // احتياط نادر: إذا فشل Keystore (جهاز معدل/محاكي قديم) نستخدم ملفاً عادياً
        // مع بقاء كلمة المرور نفسها غير مخزّنة إطلاقاً (بصمة PBKDF2 فقط).
        context.applicationContext.getSharedPreferences(FILE_NAME_FALLBACK, Context.MODE_PRIVATE)
    }

    /** قائمة الحسابات (JSON) */
    var usersJson: String?
        get() = prefs.getString(KEY_USERS, null)
        set(value) = prefs.edit().putString(KEY_USERS, value).apply()

    /** البريد الإلكتروني للجلسة الحالية (null = غير مسجّل الدخول) */
    var sessionEmail: String?
        get() = prefs.getString(KEY_SESSION, null)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_SESSION) else putString(KEY_SESSION, value)
        }.apply()

    /** هل أنجز المستخدم شاشة «ما ميزانيتك؟» */
    var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    fun clearSession() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    /** حذف كل شي (حذف الحساب والبيانات من الإعدادات) */
    fun wipeAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val FILE_NAME = "sawti_secure_prefs"
        private const val FILE_NAME_FALLBACK = "sawti_secure_prefs_plain"
        private const val KEY_USERS = "users_json"
        private const val KEY_SESSION = "session_email"
        private const val KEY_ONBOARDED = "onboarded"
    }
}
