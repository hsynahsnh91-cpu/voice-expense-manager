package com.abuomar.sawti

import android.app.Application

/**
 * تطبيق «صوتي» — مدير المصاريف الصوتي بالعامية السورية.
 *
 * لا يوجد أي سيرفر ولا بيانات تجريبية: كل شي يُخزَّن على الجهاز.
 */
class SawtiApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
        // تطبيق السمة واللغة المحفوظتين قبل ظهور أول شاشة
        container.prefsStore.applyTheme()
    }

    override fun onTerminate() {
        container.release()
        super.onTerminate()
    }

    companion object {
        /** رقم الإصدار المعروض في آخر شاشة الإعدادات */
        const val VERSION_NAME: String = "1.0.0"
        const val VERSION_CODE: Int = 1

        private lateinit var instance: SawtiApp

        fun get(): SawtiApp = instance
        fun container(): AppContainer = get().container
    }
}
