import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/* ------------------------------------------------------------------
 *  توقيع محلي اختياري (كي لا يرفع أحد مفتاحه للمستودع)
 *  أنشئ keystore.properties بجانب settings.gradle.kts بهذا الشكل:
 *     storeFile=/path/to/sawti.jks
 *     storePassword=...
 *     keyAlias=...
 *     keyPassword=...
 * ------------------------------------------------------------------ */
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.abuomar.sawti"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.abuomar.sawti"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        // العربية (سوريا) هي اللغة الافتراضية، والإنكليزية مدعومة بالكامل
        resourceConfigurations += listOf("ar", "en")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropsFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = false
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn")
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    androidResources {
        // تفعيل إعدادات اللغات لكل تطبيق (Android 13+)
        generateLocaleConfig = true
    }
}

dependencies {
    // ===== AndroidX أساسيات =====
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)          // مطلوب لـ AppCompatDelegate.setApplicationLocales
    implementation(libs.androidx.activity.compose)

    // ===== دورة الحياة وViewModel =====
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    // ===== التنقل =====
    implementation(libs.androidx.navigation.compose)

    // ===== التخزين المشفّر (كلمة المرور والتفضيلات) =====
    implementation(libs.androidx.security.crypto)

    // ===== Jetpack Compose (Material 3) =====
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    debugImplementation(libs.androidx.ui.tooling)

    // =====================================================================
    //  Accompanist Permissions — مكتبة طلب إذن الميكروفون
    //  تُستخدم في VoiceScreen/VoiceViewModel عبر rememberPermissionState(
    //      Manifest.permission.RECORD_AUDIO) لعرض نافذة الإذن النظامية،
    //      مع معالجة الرفض الدائم (shouldShowRationale) وتوجيه المستخدم
    //      إلى إعدادات التطبيق.
    // =====================================================================
    implementation(libs.accompanist.permissions)

    // ===== Kotlinx: Coroutines + Serialization =====
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
