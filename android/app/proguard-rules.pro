# =============================================================================
#  قواعد ProGuard/R8 لتطبيق «صوتي» — الإصدار 1.0.0
#  تُطبَّق فقط على بناء release (isMinifyEnabled = true).
# =============================================================================

# ---------------------------- إعدادات عامة --------------------------------
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations,
                AnnotationDefault, EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# لا نريد تحذيرات من مكتبات النظام الاختيارية
-dontwarn android.speech.**
-dontwarn androidx.security.crypto.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ------------------------- kotlinx.serialization ---------------------------
# النماذج @Serializable (User, Transaction) تُخزَّن كملفات JSON على الجهاز.
# يجب إبقاء المُولِّدات (serializers) وأسماء الحقول كما هي، وإلا فشلت القراءة
# بعد أي تحديث وفقد المستخدم بياناته.
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.abuomar.sawti.**$$serializer { *; }
-keepclassmembers class com.abuomar.sawti.** {
    *** Companion;
}
-keepclasseswithmembers class com.abuomar.sawti.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# نماذج البيانات نفسها — الحقول تُطابَق بالاسم عند فك الترميز
-keep class com.abuomar.sawti.data.User { *; }
-keep class com.abuomar.sawti.data.Transaction { *; }

# ------------------------------ Coroutines ---------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontnote kotlinx.coroutines.**

# --------------------------- Jetpack Compose -------------------------------
# لا يحتاج Compose قواعد خاصة، لكن نبقي أسماء الصفوف لتقارير الأعطال
-keep class androidx.compose.** { *; }
-dontnote androidx.compose.**

# --------------------- AndroidX Security / Keystore -------------------------
-keep class androidx.security.crypto.** { *; }
-dontnote androidx.security.crypto.**

# ------------------- نظام التعرّف على الكلام والنطق -------------------------
# SpeechRecognizer و TextToSpeech يستدعيان عبر الانعكاس من خدمات النظام
-keep class android.speech.** { *; }
-keep class android.speech.tts.** { *; }

# ------------------------- طبقات التطبيق المنطقية ---------------------------
# المحلّل العربي والمخزن والمحرّكات: أسماءها تُستخدم في سجلات التصحيح
-keep class com.abuomar.sawti.domain.ArabicParser { *; }
-keep class com.abuomar.sawti.core.** { *; }
-keep class com.abuomar.sawti.speech.** { *; }
-keep class com.abuomar.sawti.data.** { *; }
-keep class com.abuomar.sawti.vm.** { *; }
-keep class com.abuomar.sawti.SawtiApp { *; }
-keep class com.abuomar.sawti.AppContainer { *; }
-keep class com.abuomar.sawti.MainActivity { *; }
-keep class com.abuomar.sawti.SpeechFallbackBridge { *; }

# ------------------------------- التعدادات ----------------------------------
# تُستعمل معرّفاتها النصية (id) في التخزين، فلا يجوز إعادة تسمية القيم
-keepclassmembers enum com.abuomar.sawti.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# ------------------------------ الموارد -------------------------------------
# لا نحذف أي مورد يُشار إليه بالاسم من الكود (cat_*, strings)
-keep class com.abuomar.sawti.R { *; }
-keep class com.abuomar.sawti.R$* { *; }
-keepclassmembers class com.abuomar.sawti.R$* {
    public static <fields>;
}

# --------------------------- تحسينات آمنة ----------------------------------
-optimizationpasses 3
-allowaccessmodification
-repackageclasses 'com.abuomar.sawti.internal'

# استثناء: لا نعيد تغليف الطبقات المحفوظة أعلاه
-keep,allowoptimization class com.abuomar.sawti.data.** { *; }
