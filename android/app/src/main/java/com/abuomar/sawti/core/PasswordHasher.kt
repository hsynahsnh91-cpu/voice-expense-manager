package com.abuomar.sawti.core

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * تجزئة كلمة المرور — PBKDF2-HMAC-SHA256 مع ملح عشوائي.
 *
 * كلمة المرور نفسها لا تُحفظ أبداً، لا في SharedPreferences ولا في أي ملف.
 * تُحفظ البصمة والملح داخل EncryptedSharedPreferences (مشفّرة بمفتاح
 * من Android Keystore).
 */
object PasswordHasher {

    private const val ITERATIONS = 150_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    data class HashResult(
        val saltHex: String,
        val digestHex: String,
        val iterations: Int = ITERATIONS,
        val algorithm: String = ALGORITHM,
    )

    private val random = SecureRandom()

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { random.nextBytes(it) }

    /** إنشاء بصمة جديدة بملح عشوائي */
    fun hash(password: String): HashResult {
        val salt = newSalt()
        return HashResult(toHex(salt), compute(password, salt))
    }

    /** التحقق من كلمة مرور مقابل بصمة محفوظة (مقارنة بثبات زمني) */
    fun verify(password: String, saltHex: String, expectedDigestHex: String): Boolean {
        val salt = runCatching { fromHex(saltHex) }.getOrNull() ?: return false
        val actual = compute(password, salt)
        return constantTimeEquals(actual, expectedDigestHex)
    }

    private fun compute(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            toHex(factory.generateSecret(spec).encoded)
        } finally {
            spec.clearPassword()
        }
    }

    /** مقارنة آمنة ضد هجمات التوقيت */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    /* ------------------------- قواعد كلمة المرور ------------------------- */

    private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[A-Za-z\\u0621-\\u064A]{2,}$")

    fun validateEmail(raw: String?): Pair<String?, AuthError?> {
        val value = raw?.trim()?.lowercase(java.util.Locale.ROOT) ?: ""
        return if (EMAIL_REGEX.matches(value)) value to null else null to AuthError.EMAIL_INVALID
    }

    fun validateName(raw: String?): Pair<String?, AuthError?> {
        val value = raw?.trim() ?: ""
        return if (value.length >= 2) value to null else null to AuthError.NAME_REQUIRED
    }

    fun validatePassword(raw: String?): Pair<String?, AuthError?> {
        val value = raw ?: ""
        if (value.length < 8) return null to AuthError.PASSWORD_SHORT
        val hasLetter = value.any { it.isLetter() || it.code in 0x0621..0x064A }
        val hasDigit = value.any { it.isDigit() }
        if (!hasLetter || !hasDigit) return null to AuthError.PASSWORD_WEAK
        return value to null
    }

    /* ------------------------------ hex ------------------------------ */
    private val HEX = "0123456789abcdef".toCharArray()

    fun toHex(bytes: ByteArray): String {
        val out = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            out[i * 2] = HEX[v ushr 4]
            out[i * 2 + 1] = HEX[v and 0x0F]
        }
        return String(out)
    }

    fun fromHex(hex: String): ByteArray {
        val clean = hex.trim()
        require(clean.length % 2 == 0) { "odd hex length" }
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    /** أخطاء المصادقة — تُترجم في طبقة العرض عبر AuthError.messageRes */
    enum class AuthError {
        EMAIL_INVALID, EMAIL_TAKEN, EMAIL_NOT_FOUND,
        PASSWORD_WRONG, PASSWORD_SHORT, PASSWORD_WEAK, PASSWORD_MISMATCH,
        NAME_REQUIRED, GENERIC,
    }

    class AuthException(val error: AuthError) : Exception(error.name)
}
