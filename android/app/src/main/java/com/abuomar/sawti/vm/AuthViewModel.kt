package com.abuomar.sawti.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abuomar.sawti.SawtiApp
import com.abuomar.sawti.core.PasswordHasher
import com.abuomar.sawti.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel للمصادقة الحقيقية: إنشاء حساب + تسجيل دخول بالبريد وكلمة المرور.
 *
 * لا توجد حسابات تجريبية ولا دخول بأي بريد/كلمة مرور: يُتحقق فعلياً من
 * بصمة PBKDF2 المحفوظة على الجهاز.
 */
class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SawtiApp.container().repository

    data class UiState(
        val busy: Boolean = false,
        val errorRes: Int? = null,
        val signedInAs: String? = null,
        val needsBudget: Boolean = false,
        val enteredApp: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.bootstrap()
            val user = repo.currentUser.value
            if (user != null) {
                _state.update {
                    it.copy(
                        signedInAs = user.email,
                        needsBudget = repo.needsBudget,
                        enteredApp = !repo.needsBudget,
                    )
                }
            }
        }
    }

    fun clearError() = _state.update { it.copy(errorRes = null) }

    /** إنشاء حساب جديد — بعد النجاح تظهر شاشة «ما ميزانيتك؟» */
    fun signUp(name: String, email: String, password: String, confirmPassword: String) {
        if (_state.value.busy) return
        if (password != confirmPassword) {
            _state.update { it.copy(errorRes = errorResOf(PasswordHasher.AuthError.PASSWORD_MISMATCH)) }
            return
        }
        _state.update { it.copy(busy = true, errorRes = null) }
        viewModelScope.launch {
            val result = repo.signUp(name, email, password)
            result.fold(
                onSuccess = { user -> onAuthSuccess(user, needsBudget = true) },
                onFailure = { e -> onFailure(e) },
            )
        }
    }

    /** تسجيل دخول نظامي */
    fun signIn(email: String, password: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, errorRes = null) }
        viewModelScope.launch {
            val result = repo.signIn(email, password)
            result.fold(
                onSuccess = { user -> onAuthSuccess(user, needsBudget = repo.needsBudget) },
                onFailure = { e -> onFailure(e) },
            )
        }
    }

    private fun onAuthSuccess(user: User, needsBudget: Boolean) {
        _state.update {
            it.copy(
                busy = false,
                errorRes = null,
                signedInAs = user.email,
                needsBudget = needsBudget,
                enteredApp = !needsBudget,
            )
        }
    }

    private fun onFailure(e: Throwable) {
        val authError = (e as? PasswordHasher.AuthException)?.error ?: PasswordHasher.AuthError.GENERIC
        _state.update { it.copy(busy = false, errorRes = errorResOf(authError)) }
    }

    /** يُستدعى بعد ضبط الميزانية من شاشة «ما ميزانيتك؟» */
    fun budgetCompleted() {
        repo.markOnboarded()
        _state.update { it.copy(needsBudget = false, enteredApp = true) }
    }

    fun signOut() {
        repo.signOut()
        _state.update { UiState() }
    }

    private fun errorResOf(error: PasswordHasher.AuthError): Int = when (error) {
        PasswordHasher.AuthError.EMAIL_INVALID -> com.abuomar.sawti.R.string.err_email_invalid
        PasswordHasher.AuthError.EMAIL_TAKEN -> com.abuomar.sawti.R.string.err_email_taken
        PasswordHasher.AuthError.EMAIL_NOT_FOUND -> com.abuomar.sawti.R.string.err_email_not_found
        PasswordHasher.AuthError.PASSWORD_WRONG -> com.abuomar.sawti.R.string.err_password_wrong
        PasswordHasher.AuthError.PASSWORD_SHORT -> com.abuomar.sawti.R.string.err_password_short
        PasswordHasher.AuthError.PASSWORD_WEAK -> com.abuomar.sawti.R.string.err_password_weak
        PasswordHasher.AuthError.PASSWORD_MISMATCH -> com.abuomar.sawti.R.string.err_password_mismatch
        PasswordHasher.AuthError.NAME_REQUIRED -> com.abuomar.sawti.R.string.err_name_required
        PasswordHasher.AuthError.GENERIC -> com.abuomar.sawti.R.string.err_generic
    }
}
