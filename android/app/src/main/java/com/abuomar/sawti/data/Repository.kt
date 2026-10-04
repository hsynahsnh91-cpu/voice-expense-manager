package com.abuomar.sawti.data

import com.abuomar.sawti.core.CivilDate
import com.abuomar.sawti.core.Currency
import com.abuomar.sawti.core.PasswordHasher
import com.abuomar.sawti.core.PrefsStore
import com.abuomar.sawti.core.SecureStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * المستودع الوحيد للبيانات: الحسابات + المصاريف + التسجيلات + الملخصات.
 *
 * كل شي محلي على الجهاز. لا بيانات تجريبية، ولا seed، ولا حسابات وهمية:
 * أول ما يُثبَّت التطبيق تكون القوائم فارغة ولا يمكن الدخول إلا بعد إنشاء حساب.
 */
class Repository(
    private val store: JsonStore,
    private val secure: SecureStore,
    private val prefs: PrefsStore,
) {

    /* ------------------------------- الحالة ------------------------------- */
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _summary = MutableStateFlow(
        MonthSummary(CivilDate.monthKey(CivilDate.today()), 0, 0, 0, 0, 0, false, emptyList())
    )
    val summary: StateFlow<MonthSummary> = _summary.asStateFlow()

    /** عدد التسجيلات التي لم تُسمع بعد — تُعرض كشارة على تبويب «الوارد» */
    private val _unheardCount = MutableStateFlow(0)
    val unheardCount: StateFlow<Int> = _unheardCount.asStateFlow()

    /**
     * تحميل أولي — على خيط IO دائماً حتى لا تُحجب أول رسمّة للواجهة
     * بقراءة الملفات (كان هذا سبب تأخر الظهور عند أول فتح).
     */
    suspend fun bootstrap() = withContext(Dispatchers.IO) {
        val users = store.readUsers()
        val email = secure.sessionEmail
        val txs = store.readTransactions().sortedWith(TX_COMPARATOR)
        _users.value = users
        _currentUser.value = email?.let { e -> users.firstOrNull { it.email == e } }
        _transactions.value = txs
        recompute()
    }

    /* ============================ المصادقة الحقيقية ======================== */

    /**
     * إنشاء حساب جديد — يُرفض أي بريد مسجّل مسبقاً.
     * كلمة المرور تُحوَّل إلى بصمة PBKDF2 ولا تُخزَّن أبداً.
     */
    suspend fun signUp(name: String, email: String, password: String): Result<User> {
        val (cleanName, nameErr) = PasswordHasher.validateName(name)
        if (nameErr != null) return Result.failure(PasswordHasher.AuthException(nameErr))
        val (cleanEmail, emailErr) = PasswordHasher.validateEmail(email)
        if (emailErr != null) return Result.failure(PasswordHasher.AuthException(emailErr))
        val (cleanPass, passErr) = PasswordHasher.validatePassword(password)
        if (passErr != null) return Result.failure(PasswordHasher.AuthException(passErr))
        if (_users.value.any { it.email == cleanEmail }) {
            return Result.failure(PasswordHasher.AuthException(PasswordHasher.AuthError.EMAIL_TAKEN))
        }

        val hashed = PasswordHasher.hash(cleanPass!!)
        val user = User(
            email = cleanEmail!!,
            name = cleanName!!,
            salt = hashed.saltHex,
            digest = hashed.digestHex,
            iterations = hashed.iterations,
            algorithm = hashed.algorithm,
            createdAt = System.currentTimeMillis(),
        )
        val next = _users.value + user
        store.writeUsers(next)
        _users.value = next
        secure.sessionEmail = user.email
        _currentUser.value = user
        recompute()
        return Result.success(user)
    }

    /**
     * تسجيل الدخول — يتحقق فعلياً من البصمة.
     * لا يمكن الدخول بأي بريد وكلمة مرور عشوائية.
     */
    suspend fun signIn(email: String, password: String): Result<User> {
        val (cleanEmail, emailErr) = PasswordHasher.validateEmail(email)
        if (emailErr != null) return Result.failure(PasswordHasher.AuthException(emailErr))
        val user = _users.value.firstOrNull { it.email == cleanEmail }
            ?: return Result.failure(PasswordHasher.AuthException(PasswordHasher.AuthError.EMAIL_NOT_FOUND))
        if (password.isBlank()) {
            return Result.failure(PasswordHasher.AuthException(PasswordHasher.AuthError.PASSWORD_WRONG))
        }
        val ok = PasswordHasher.verify(password, user.salt, user.digest)
        if (!ok) return Result.failure(PasswordHasher.AuthException(PasswordHasher.AuthError.PASSWORD_WRONG))
        secure.sessionEmail = user.email
        _currentUser.value = user
        _transactions.value = store.readTransactions().sortedWith(TX_COMPARATOR)
        recompute()
        return Result.success(user)
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        val user = _currentUser.value
            ?: return Result.failure(PasswordHasher.AuthException(PasswordHasher.AuthError.GENERIC))
        if (!PasswordHasher.verify(oldPassword, user.salt, user.digest)) {
            return Result.failure(PasswordHasher.AuthException(PasswordHasher.AuthError.PASSWORD_WRONG))
        }
        val (cleanPass, err) = PasswordHasher.validatePassword(newPassword)
        if (err != null) return Result.failure(PasswordHasher.AuthException(err))
        val hashed = PasswordHasher.hash(cleanPass!!)
        val updated = user.copy(salt = hashed.saltHex, digest = hashed.digestHex)
        val next = _users.value.map { if (it.email == user.email) updated else it }
        store.writeUsers(next)
        _users.value = next
        _currentUser.value = updated
        return Result.success(Unit)
    }

    fun signOut() {
        secure.clearSession()
        _currentUser.value = null
    }

    /** حذف الحساب وكل البيانات (مصاريف + تسجيلات + تفضيلات) */
    suspend fun deleteAccountAndData() {
        val user = _currentUser.value
        if (user != null) {
            store.clearTransactionsOf(user.email)
            val next = _users.value.filterNot { it.email == user.email }
            store.writeUsers(next)
            _users.value = next
        }
        secure.wipeAll()
        prefs.clearAll()
        _currentUser.value = null
        _transactions.value = emptyList()
        recompute()
    }

    val isLoggedIn: Boolean get() = _currentUser.value != null
    val needsBudget: Boolean get() = !secure.onboarded || prefs.monthlyBudget <= 0L

    fun markOnboarded() { secure.onboarded = true }

    /* ================================ المصاريف ============================= */

    suspend fun addTransaction(
        amountNew: Long,
        category: Category,
        type: TxType,
        date: String,
        merchant: String?,
        note: String = "",
        transcript: String? = null,
        dateLabel: String? = null,
        audioFile: File? = null,
        audioDurationSec: Int = 0,
        muted: Boolean = false,
    ): Transaction {
        val owner = _currentUser.value
            ?: throw IllegalStateException("no-signed-in-user")
        val id = "tx_" + UUID.randomUUID().toString().replace("-", "").take(16)

        // نقل ملف الصوت المؤقت إلى مكانه النهائي باسم المصروف
        var hasAudio = false
        if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
            val target = store.audioFile(id)
            hasAudio = runCatching {
                if (target.exists()) target.delete()
                audioFile.copyTo(target, overwrite = true)
                audioFile.delete()
                true
            }.getOrDefault(false)
        }

        val tx = Transaction(
            id = id,
            ownerEmail = owner.email,
            amountNew = amountNew.coerceIn(0L, Currency.MAX_AMOUNT),
            category = category.id,
            type = type.id,
            date = if (CivilDate.isValid(date)) date else CivilDate.today(),
            merchant = merchant?.takeIf { it.isNotBlank() },
            note = note,
            transcript = transcript?.takeIf { it.isNotBlank() },
            dateLabel = dateLabel,
            hasAudio = hasAudio,
            audioDurationSec = audioDurationSec,
            listened = false,
            muted = muted,
        )
        val all = store.readTransactions() + tx
        store.writeTransactions(all)
        _transactions.value = all.sortedWith(TX_COMPARATOR)
        recompute()
        return tx
    }

    suspend fun updateTransaction(id: String, patch: Transaction.() -> Transaction): Transaction? {
        val all = store.readTransactions().toMutableList()
        val i = all.indexOfFirst { it.id == id }
        if (i == -1) return null
        val updated = all[i].patch()
        all[i] = updated
        store.writeTransactions(all)
        _transactions.value = all.sortedWith(TX_COMPARATOR)
        recompute()
        return updated
    }

    suspend fun deleteTransaction(id: String) {
        store.deleteAudioFile(id)
        val all = store.readTransactions().filterNot { it.id == id }
        store.writeTransactions(all)
        _transactions.value = all.sortedWith(TX_COMPARATOR)
        recompute()
    }

    suspend fun clearAllTransactions() {
        val owner = _currentUser.value?.email ?: return
        store.clearTransactionsOf(owner)
        _transactions.value = store.readTransactions().sortedWith(TX_COMPARATOR)
        recompute()
    }

    suspend fun markListened(id: String) {
        updateTransaction(id) { if (listened) this else copy(listened = true) }
    }

    fun audioFileFor(id: String): File? = store.audioFile(id).takeIf { store.hasAudioFile(id) }

    /* ============================== الملخّصات ============================== */

    private fun myTransactions(): List<Transaction> {
        val email = _currentUser.value?.email ?: return emptyList()
        return _transactions.value.filter { it.ownerEmail == email }
    }

    private fun recompute() {
        val mine = myTransactions()
        val monthKey = CivilDate.monthKey(CivilDate.today())
        val month = mine.filter { CivilDate.monthKey(it.date) == monthKey }
        val spent = month.filter { it.type != TxType.INCOME.id }.sumOf { it.amountNew }
        val income = month.filter { it.type == TxType.INCOME.id }.sumOf { it.amountNew }
        val budget = prefs.monthlyBudget
        val remaining = budget - spent
        val percent = if (budget > 0) ((spent * 100) / budget).toInt().coerceIn(0, 999) else 0
        val byCategory = month.filter { it.type != TxType.INCOME.id }
            .groupingBy { Category.of(it.category) }
            .fold(0L) { acc, t -> acc + t.amountNew }
            .entries.sortedByDescending { it.value }
            .map { it.key to it.value }

        _summary.value = MonthSummary(
            monthKey = monthKey,
            spent = spent,
            income = income,
            budget = budget,
            remaining = remaining,
            percent = percent,
            overBudget = budget > 0 && spent > budget,
            byCategory = byCategory,
        )
        _unheardCount.value = mine.count { it.hasAudio && !it.listened }
    }

    /** مصاريف ضمن فترة وفئة ونص بحث */
    fun query(from: String?, to: String?, category: String?, search: String?): List<Transaction> {
        val q = search?.trim()?.lowercase()
        return myTransactions().filter { tx ->
            if (from != null && tx.date < from) return@filter false
            if (to != null && tx.date > to) return@filter false
            if (category != null && category != "all" && tx.category != category) return@filter false
            if (!q.isNullOrEmpty()) {
                val hay = buildString {
                    append(tx.note); append(' ')
                    append(tx.merchant ?: ""); append(' ')
                    append(tx.transcript ?: ""); append(' ')
                    append(Category.of(tx.category).id)
                }.lowercase()
                if (!hay.contains(q)) return@filter false
            }
            true
        }
    }

    /** مصاريف مجمّعة حسب اليوم (للعرض في القائمة) */
    fun groupedByDate(list: List<Transaction>): List<Pair<String, List<Transaction>>> =
        list.groupBy { it.date }.entries.sortedByDescending { it.key }.map { it.key to it.value }

    companion object {
        /** الأحدث تاريخاً أولاً، وعند نفس التاريخ الأحدث إنشاءً أولاً */
        val TX_COMPARATOR = Comparator<Transaction> { a, b ->
            val d = b.date.compareTo(a.date)
            if (d != 0) d else b.createdAt.compareTo(a.createdAt)
        }
    }
}
