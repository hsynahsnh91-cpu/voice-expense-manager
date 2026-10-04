package com.abuomar.sawti

import android.content.Context
import com.abuomar.sawti.core.PrefsStore
import com.abuomar.sawti.core.SecureStore
import com.abuomar.sawti.data.JsonStore
import com.abuomar.sawti.data.Repository
import com.abuomar.sawti.speech.AudioPlayback
import com.abuomar.sawti.speech.SpeechOutput

/**
 * حاوية الاعتماديات البسيطة (بدون Hilt/Koin حتى يبقى البناء خفيفاً وموثوقاً).
 * تُنشأ مرة واحدة في [SawtiApp] وتُشارك بين كل الـViewModels.
 */
class AppContainer(context: Context) {

    /* التفضيلات خفيفة فتُنشأ فوراً (تُحتاج للسمة واللغة قبل أول رسمّة).
       أما المتاجر المشفّرة فتُنشأ عند أول استعمال فقط: إنشاء MasterKey
       في Android Keystore عملية ثقيلة كانت تؤخر فتح التطبيق سابقاً. */
    val prefsStore: PrefsStore = PrefsStore(context)

    val secureStore: SecureStore by lazy { SecureStore(context) }
    val jsonStore: JsonStore by lazy { JsonStore(context) }
    val repository: Repository by lazy { Repository(jsonStore, secureStore, prefsStore) }

    /** النطق الصوتي — كائن واحد مشترك حتى لا ننشئ TextToSpeech لكل شاشة */
    val speechOutput: SpeechOutput by lazy { SpeechOutput(context) }

    /** مشغّل تسجيلات الوارد — مشترك حتى يستمر التشغيل بين الشاشات */
    val audioPlayback: AudioPlayback by lazy { AudioPlayback(context) }

    val appContext: Context = context.applicationContext

    fun release() {
        speechOutput.shutdown()
        audioPlayback.release()
    }
}
