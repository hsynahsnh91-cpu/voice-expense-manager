package com.abuomar.sawti.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * مخزن JSON بسيط وآمن على القرص (بدل Room) — كافي لتطبيق بمستخدم واحد
 * ولا يحتاج أي معالج annotations.
 *
 * الملفات تقع في المساحة الخاصة بالتطبيق:
 *   filesDir/sawti/users.json
 *   filesDir/sawti/transactions.json
 *   filesDir/sawti/audio/<id>.m4a
 *
 * الكتابة ذرّية (ملف مؤقت ثم إعادة تسمية) والقراءة/الكتابة محمية بقفل.
 */
class JsonStore(context: Context) {

    private val dir = File(context.applicationContext.filesDir, ROOT_DIR).apply { mkdirs() }
    val audioDir = File(dir, AUDIO_DIR).apply { mkdirs() }

    private val usersFile = File(dir, "users.json")
    private val txFile = File(dir, "transactions.json")

    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
        isLenient = true
    }

    /* ------------------------------- الحسابات ------------------------------ */
    suspend fun readUsers(): List<User> = mutex.withLock {
        withContext(Dispatchers.IO) {
            readList(usersFile)
        }
    }

    suspend fun writeUsers(users: List<User>) = mutex.withLock {
        withContext(Dispatchers.IO) { writeList(usersFile, users) }
    }

    /* ------------------------------- المصاريف ------------------------------ */
    suspend fun readTransactions(): List<Transaction> = mutex.withLock {
        withContext(Dispatchers.IO) { readList(txFile) }
    }

    suspend fun writeTransactions(list: List<Transaction>) = mutex.withLock {
        withContext(Dispatchers.IO) { writeList(txFile, list) }
    }

    /* ------------------------------ الملفات الصوتية ------------------------ */
    fun audioFile(id: String): File = File(audioDir, "$id.m4a")

    fun hasAudioFile(id: String): Boolean = audioFile(id).exists() && audioFile(id).length() > 0

    fun deleteAudioFile(id: String): Boolean = audioFile(id).takeIf { it.exists() }?.delete() ?: true

    fun audioBytesTotal(): Long =
        audioDir.listFiles()?.sumOf { it.length() } ?: 0L

    fun audioFileCount(): Int = audioDir.listFiles()?.size ?: 0

    /* ------------------------------ حذف شامل ------------------------------ */
    suspend fun wipeEverything() = withContext(Dispatchers.IO) {
        audioDir.listFiles()?.forEach { it.delete() }
        usersFile.delete()
        txFile.delete()
    }

    suspend fun clearTransactionsOf(email: String) {
        val all = readTransactions()
        all.filter { it.ownerEmail == email }.forEach { deleteAudioFile(it.id) }
        writeTransactions(all.filterNot { it.ownerEmail == email })
    }

    /* -------------------------------- داخلي -------------------------------- */
    private inline fun <reified T> readList(file: File): List<T> {
        if (!file.exists() || file.length() == 0L) return emptyList()
        return runCatching { json.decodeFromString<List<T>>(file.readText(Charsets.UTF_8)) }
            .getOrDefault(emptyList())
    }

    private inline fun <reified T> writeList(file: File, list: List<T>) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(list), Charsets.UTF_8)
        if (file.exists()) file.delete()
        if (!tmp.renameTo(file)) {
            // إعادة التسمية فشلت (نادر) → نكتب مباشرة
            file.writeText(json.encodeToString(list), Charsets.UTF_8)
            tmp.delete()
        }
    }

    companion object {
        private const val ROOT_DIR = "sawti"
        private const val AUDIO_DIR = "audio"
    }
}
