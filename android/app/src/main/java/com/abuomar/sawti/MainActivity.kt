package com.abuomar.sawti

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abuomar.sawti.core.PrefsStore
import com.abuomar.sawti.ui.nav.SawtiRoot
import com.abuomar.sawti.ui.theme.SawtiTheme
import com.abuomar.sawti.vm.AuthViewModel
import com.abuomar.sawti.vm.DataViewModel

/**
 * النشاط الوحيد في التطبيق (single-activity + Compose).
 *
 * يضبط: الحواف الآمنة (edge-to-edge)، السمة المحفوظة، اللغة المحفوظة،
 * وشاشة البداية قبل أول تركيب.
 */
class MainActivity : ComponentActivity() {

    private val prefs: PrefsStore by lazy { SawtiApp.container().prefsStore }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs.applyTheme()

        setContent {
            val dataVm: DataViewModel = viewModel()
            val authVm: AuthViewModel = viewModel()
            val prefsState by dataVm.prefsState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (prefsState.themeMode) {
                PrefsStore.THEME_LIGHT -> false
                PrefsStore.THEME_DARK -> true
                else -> systemDark
            }

            SawtiTheme(
                darkTheme = dark,
                arabic = prefsState.arabic,
                displayUnit = prefsState.displayUnit,
            ) {
                SawtiRoot(
                    authViewModel = authVm,
                    dataViewModel = dataVm,
                )
            }
        }
    }

    /** محرّك التعرّف الاحتياطي: نقرأ النتيجة ونعيدها للـViewModel عبر LocalIntentResult */
    private val speechFallbackLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            // النتيجة تُعالج داخل VoiceScreen عبر SpeechFallbackBridge
            SpeechFallbackBridge.deliver(data)
        }

    fun launchSpeechFallback(intent: Intent) {
        runCatching { speechFallbackLauncher.launch(intent) }
    }

    override fun onResume() {
        super.onResume()
        SawtiApp.container().prefsStore.applyTheme()
    }
}

/**
 * جسر بسيط لنقل نتيجة Intent التعرّف الاحتياطي من Activity إلى Compose،
 * لأن ActivityResultLauncher يجب أن يُسجَّل داخل Activity.
 */
object SpeechFallbackBridge {
    private var pending: Intent? = null
    private var listener: ((Intent?) -> Unit)? = null

    fun deliver(data: Intent?) {
        val l = listener
        if (l != null) { l(data); pending = null } else pending = data
    }

    fun observe(onResult: (Intent?) -> Unit) {
        listener = onResult
        pending?.let { onResult(it); pending = null }
    }

    fun release() { listener = null }
}
