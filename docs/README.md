# صوتي — مدير المصاريف الصوتي
### Sawti · Voice Expense Manager · v1.0.0

تطبيق لتسجيل المصاريف **بالصوت** وبالعامية الشامية/السورية. تحكي جملة مثل
«كلفني 5000 ليرة سوري روح على دمشق» فيفهمها التطبيق: **المبلغ + الفئة + المكان + التاريخ**،
ويحفظها، ويحفظ تسجيل صوتك في قسم «الوارد» لتسمعه لاحقاً وتتأكد.

> **لا بيانات تجريبية إطلاقاً.** لا حسابات وهمية، ولا مصاريف مزروعة، ولا محرك صوتي
> مزيف «للمنظرة». كل ما تراه في القوائم هو ما أدخلته أنت فعلاً.

---

## 1) ما الذي في هذا المستودع

```
voice-expense-manager/
├── web/                     ← نسخة الويب (PWA) — تعمل الآن وتُعاين مباشرة
│   ├── index.html           ← هيكل الصفحة وكل معرّفات العناصر
│   ├── styles.css           ← السمة (فاتح/داكن) + الحركات + التقويم
│   ├── manifest.json        ← PWA manifest
│   ├── sw.js                ← Service Worker (يعمل دون اتصال)
│   ├── server.js            ← مخدم ثابت بدون أي اعتماديات (Node)
│   ├── icon.svg             ← الأيقونة الجديدة (512×512)
│   ├── icons/icon-*.png     ← 72…1024px
│   └── js/
│       ├── parser.js        ← محلّل الكلام العامي (26/26 اختباراً ناجحاً)
│       ├── i18n.js          ← EN → AR (~180 مفتاحاً × 2) + سياسة الخصوصية
│       ├── auth.js          ← PBKDF2-SHA256 (WebCrypto + بديل JS مطابق)
│       ├── store.js         ← IndexedDB للصوت + localStorage للبيانات
│       ├── speech.js        ← Web Speech API + TTS + تشغيل التسجيلات
│       ├── calendar.js      ← تقويم الفترة (تواريخ مدنية، بلا UTC)
│       └── app.js           ← التوجيه والشاشات وكل الربط
├── android/                 ← تطبيق أندرويد أصلي (Kotlin + Jetpack Compose)
└── docs/
    ├── parser-test.mjs      ← 26 حالة اختبار للمحلّل
    ├── core-test.mjs        ← i18n + التواريخ المدنية + التحقق من المدخلات
    ├── crypto-test.mjs      ← تطابق WebCrypto مع البديل JS وnode:crypto
    ├── app-icon-1024.png    ← الأيقونة الرئيسية
    └── README.md            ← هذا الملف
```

---

## 2) نسخة الويب — تشغيل فوري

```bash
cd web
node server.js          # → http://localhost:8080
```

لا يحتاج `npm install` — المخدم مكتوب بـ Node القياسي بدون أي حزم.

### ملاحظة مهمة عن الميكروفون في المعاينة
نافذة المعاينة داخل المحرّات تعمل كـ **iframe معزول** (`sandbox="allow-scripts"`)، والمتصفح
يمنع `getUserMedia` و Web Crypto داخل الإطار المعزول. لذلك:

- **الميكروفون لن يعمل داخل نافذة المعاينة** — افتح الرابط في **تبويب متصفح حقيقي**
  (`http://localhost:8080`) فيعمل التسجيل والتعرّف على الكلام طبيعياً.
- التطبيق يكتشف هذه الحالة ويعرض تنبيهاً (`#mic-warning`) يشرح السبب، ويبقى كل شيء
  آخر يعمل: الكتابة اليدوية في خانة الإرسال تُحلَّل **بنفس المحلّل** تماماً.
- لهذا السبب أيضاً يوجد بديل JS لـ PBKDF2 مطابق لـ WebCrypto (تحقّق منه `docs/crypto-test.mjs`).

---

## 3) نسخة أندرويد — كيف تبنيها

بيئة العمل هنا لا تملك Android SDK (ولا ذاكرة كافية للبناء)، لذلك **المصادر كاملة وجاهزة**
لكن البناء يتم عندك في Android Studio. لم يُترجم أي ملف Kotlin محلياً.

### المتطلبات
| الشيء | القيمة |
|---|---|
| Android Studio | Narwhal 3 أو أحدث |
| JDK | **17** |
| Gradle | 8.14.3 (يُنزَّل تلقائياً عبر الـwrapper) |
| Android Gradle Plugin | 8.13.0 |
| Kotlin | 2.2.20 |
| Compose BOM | 2025.10.01 |
| compileSdk / targetSdk | 36 |
| minSdk | **24** (أندرويد 7.0) |

كل هذه الإصدارات **تحقّقتُ من وجودها فعلاً** على `maven.google.com` / Maven Central.

### خطوات البناء
1. افتح Android Studio → **Open** → اختر مجلد `android/`.
2. أنشئ `android/local.properties` (انسخ من `local.properties.example`) وضع فيه `sdk.dir`.
   Android Studio عادةً ينشئه وحده.
3. **ملف `gradle/wrapper/gradle-wrapper.jar` غير موجود** (لا يمكن توليد ملف ثنائي هنا).
   عند أول فتح سيقترح Android Studio إصلاح الـwrapper، أو نفّذ مرة واحدة:
   ```bash
   cd android
   gradle wrapper --gradle-version 8.14.3
   ```
4. انتظر مزامنة Gradle (تنزيل الاعتماديات).
5. **Build → Make Project** ثم شغّل على جهاز حقيقي (المحاكي غالباً لا يملك خدمة
   تعرّف على الكلام، فيظهر «لا يوجد محرك تعرّف على هذا الجهاز» — وهذا سلوك صحيح وصادق).
6. لإصدار موقَّع: أنشئ `android/keystore.properties` (انسخ من `keystore.properties.example`)
   ثم **Build → Generate Signed App Bundle / APK**.

### الأذونات
`RECORD_AUDIO` فقط (بالإضافة إلى `INTERNET`/`ACCESS_NETWORK_STATE` لأن خدمة التعرّف
على الكلام في بعض الأجهزة تعمل عبر الشبكة). **لا** جهات اتصال، **لا** موقع، **لا** كاميرا،
**لا** تخزين خارجي — التسجيلات تُحفظ في المساحة الخاصة بالتطبيق.

طلب الإذن مضبوط عبر **Accompanist Permissions 0.37.3**:
```kotlin
val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
LaunchedEffect(micPermission.status.isGranted, micPermission.status.shouldShowRationale) {
    viewModel.onPermissionResult(micPermission.status.isGranted,
                                 micPermission.status.shouldShowRationale)
}
```

---

## 4) العمارة

### الويب
وحدات ES مستقلة، بلا أي إطار عمل وبلا CDN (يعمل دون اتصال):
`parser` → `i18n` → `auth` → `store` → `speech` → `calendar` → `app`.

### أندرويد
```
com.abuomar.sawti/
├── SawtiApp.kt            Application + VERSION_NAME = "1.0.0"
├── AppContainer.kt        حاوية اعتماديات بسيطة (بلا Hilt/Koin)
├── MainActivity.kt        نشاط واحد + Compose + edge-to-edge
├── core/
│   ├── Currency.kt        الليرة السورية الجديدة/القديمة (×100) + تنسيق ١٬٢٣٤
│   ├── CivilDate.kt       تواريخ مدنية yyyy-MM-dd (Calendar محلي، لا UTC أبداً)
│   ├── PasswordHasher.kt  PBKDF2-SHA256 · 150,000 دورة · ملح 128-بت · مقارنة ثابتة الزمن
│   ├── SecureStore.kt     EncryptedSharedPreferences (MasterKey AES256_GCM)
│   └── PrefsStore.kt      كل التفضيلات (اللغة، السمة، الميزانية، الكتم، النطق…)
├── data/
│   ├── Models.kt          User · Transaction · Category · ParsedExpense · MonthSummary
│   ├── JsonStore.kt       ملفات JSON ذرّية (tmp → rename) + مجلد audio/
│   └── Repository.kt      الحالة (StateFlow) + المصادقة + CRUD + الملخص الشهري
├── domain/ArabicParser.kt المحلّل العامي (نسخة Kotlin من parser.js، بنفس 26 حالة)
├── speech/
│   ├── SpeechToTextEngine.kt  SpeechRecognizer + بديل RecognizerIntent + سلسلة لغات
│   ├── AudioRecorder.kt       MediaRecorder (AAC/M4A) + كشف التسجيل الصامت
│   ├── SpeechOutput.kt        TextToSpeech + اختيار أفضل صوت عربي
│   └── AudioPlayback.kt       MediaPlayer لتشغيل تسجيلات «الوارد»
├── ui/
│   ├── theme/  Color · Type · Theme (فاتح/داكن + RTL + وحدة العرض)
│   ├── components/  Common · DateRangePicker · PolicyText
│   ├── screens/  Auth · Budget · Home · Transactions · Voice · Inbox · Settings
│   └── nav/SawtiRoot.kt   يقرر المرحلة: مصادقة → ميزانية → التطبيق
└── vm/  AuthViewModel · BudgetViewModel · DataViewModel · VoiceViewModel
```

**لماذا ملفات JSON بدل Room؟** لتقليل خطر فشل البناء (لا KSP ولا مهاجرات)،
مع كتابة ذرّية تمنع تلف البيانات. التخزين يبقى محلياً بالكامل.

---

## 5) الليرة السورية الجديدة

اعتباراً من **1 كانون الثاني 2026** حُذف صفران: **100 ليرة قديمة = 1 ليرة سورية جديدة**.
الفئات الجديدة: 10 (الوردة الشامية)، 25 (التوت الشامي)، 50 (الحمضيات)، 100 (القطن)،
200 (الزيتون)، 500 (القمح) — بلا صور أشخاص.

كيف تعامل التطبيق مع ذلك:

- **التخزين الداخلي دائماً بالليرة الجديدة** (`amountNew`). لا يوجد أي تخزين مزدوج.
- عند قول «٧٥٠٠٠ ليرة قديمة» → المحلّل يحوّلها تلقائياً إلى **750** ليرة جديدة.
- وحدة العرض قابلة للقلب من الإعدادات أو من زر داخل بطاقة الفهم.
- عملة الميزانية تُختار في شاشة «ما ميزانيتك؟» (جديدة/قديمة) وتُحوَّل عند الحفظ.
- التنسيق العربي يستخدم الفاصل `٬` والأرقام اللاتينية: `1٬234٬567 ل.س`.
- النطق الصوتي يقول «ليرة سورية» بصيغة مقروءة.

---

## 6) المحلّل العامي — أمثلة مختبرة

```
كلفني 5000 ليرة سوري روح على دمشق   → 5٬000 · مواصلات · دمشق
خمسة آلاف على الخضرة من السوق مبارح → 5٬000 · طعام · 2026-10-03
فاتورة الكهرباء مية ألف             → 100٬000 · فواتير
ألفين ونص على الفروج               → 2٬500 · طعام          (ونص = ×0.5)
٧٥٠٠٠ ليرة قديمة بنطلون            → 750 · ملابس           (تحويل ÷100)
قبضت راتبي مليون ونص               → 1٬500٬000 · دخل
تلت ميه ألف على الدوا              → 300٬000 · صحة
```

يدعم: الأرقام اللاتينية والعربية-الهندية (٠١٢٣٤٥٦٧٨٩ والفارسية ۰۱۲)، الأعداد بالكلمات
(مركّبة: «تلت ميه ألف»)، المضاعفات (ألف/آلاف/مليون/ملايين)، «ونص/ونصف»،
13 فئة مع أولويات (الهدايا والأهل تُحسم أولاً عند التعارض)، التواريخ النسبية
(مبارح، أول مبارح، السبت، 5 الشهر، الشهر الماضي، 12/3)، وأسماء المدن السورية.

**مخطط الثقة:** أرقام صريحة 92 (+4 مع ذكر العملة)، أرقام+مضاعف 96، مضاعف وحده 78،
كلمات `55 + min(كلمات×7, 21)`. ثقة الجملة = `مبلغ×0.6 + فئة×0.2 + تاريخ + تاجر`،
وعندما تقل ثقة المبلغ عن 0.5 يطلب التطبيق التأكيد **ويظهر حقل إدخال يدوي** —
لا يخمّن رقماً أبداً.

---

## 7) المتطلبات كما طُلبت — وأين نُفّذت

| المطلوب | الويب | أندرويد |
|---|---|---|
| مصادقة حقيقية (بريد+كلمة مرور) ترفض الخطأ | `js/auth.js` + `#card-signin` | `PasswordHasher` + `Repository.signIn` |
| أزرار Google/Apple **فوق** فاصل فيه «أو» | `.federated` ثم `.divider-or` | `FederatedButtons()` ثم `OrDivider()` |
| دلالات نموذج أصلية (email/password/submit/Show password) | `<form>`, `autocomplete`, `type=submit` | `KeyboardType.Email/Password` + `ImeAction.Done` + زر باسم وصول |
| شاشة «ما ميزانيتك؟» + عملة + زر دخول | `#view-budget` | `BudgetScreen.kt` |
| تنقّل سفلي بأربع وجهات + شارة على الوارد | `nav#tabbar` + `aria-current` | Material 3 `NavigationBar` + `BadgedBox` |
| تبديل بلا push animation | تلاشٍ 150ms | `AnimatedContent` (fade + slide خفيف) |
| تقويم فترة (`range_start/end/middle`) | `js/calendar.js` | `DateRangePicker.kt` (نفس المفاتيح الدلالية) |
| تواريخ مدنية بلا UTC | `civilToday()` | `CivilDate` (Calendar محلي) |
| بداية الأسبوع حسب اللغة + أسهم/PageUp/PageDown | ✔ | ✔ |
| i18n EN→AR + RTL | `js/i18n.js` | `values/` + `values-ar/` + `locales_config.xml` |
| الوارد: استماع لتسجيل المتكلم | waveform + `playAudioBlob` | `AudioPlayback` + `InboxScreen` |
| كتم إعادة الصوت بعد التسجيل | `#set-mute` (افتراضي **مفعّل**) | `PrefsStore.mutePlaybackAfterRecord` |
| إصدار 1.0.0 + سياسة خصوصية + «تم إنشاء التطبيق بواسطة أبو عمر» | `#set-version` + `#privacy` | `SettingsScreen` + `PolicyText.kt` |
| أيقونة جديدة عريضة ونظيفة | `icon.svg` + PNGs | أيقونة تكيّفية (قرش+ميكروفون) |
| Accompanist Permissions + ViewModel + SpeechRecognizer | — | ✔ `VoiceViewModel` + `SpeechToTextEngine` |
| الليرة السورية الجديدة | `Currency` في `parser.js` | `core/Currency.kt` |
| الميكروفون يكتب ما يُقال في خانة الإرسال | نتائج جزئية لحظية | `onPartialResults` → `transcript` |
| حركات وانتقالات وتفاعلات عند النقر | ripple + keyframes | `rememberPressScale` + `AnimatedContent` |

---

## 8) الاختبارات

```bash
node docs/parser-test.mjs    # 26/26 ✔
node docs/core-test.mjs      # i18n + تواريخ + تحقق ✔
node docs/crypto-test.mjs    # WebCrypto == JS fallback == node:crypto ✔
```

المتجه المرجعي للتشفير (للتحقق من أي تعديل مستقبلي):
```
pw = sawti2026
salt = 000102030405060708090a0b0c0d0e0f
iterations = 150000   algorithm = PBKDF2-SHA256   keyLen = 32
digest = 9cd120ae80879aa1d5c989825a12428fd7d0d0a9c94564a9d84baa41bdc30bd3
```

---

## 9) ملاحظات صريحة (بلا تجميل)

1. **أندرويد لم يُترجم.** لا يوجد SDK في هذه البيئة. المصادر كاملة ومتسقة
   (دقّقتُ كل مفاتيح النصوص: 352 مفتاحاً في `values` و`values-ar` بلا تكرار ولا نقص،
   وتواقيع الدوال بين الطبقات)، لكن أول بناء في Android Studio قد يحتاج تعديلات صغيرة.
2. **الالتقاط المتوازي** (MediaRecorder + SpeechRecognizer معاً) يفشل على بعض الأجهزة
   بسبب سياسة Android 10+. الحل المطبَّق: محاولة، ثم قياس `getMaxAmplitude()`؛
   إذا كان التسجيل صامتاً يُحذف الملف، ويُضبط `concurrentCaptureSupported = false`،
   وتُعاد محاولة التعرّف وحده مع تنبيه المستخدم مرة واحدة. **لا يُحفظ مقطع صامت أبداً.**
3. **محرك التعرّف** هو خدمة النظام. إن لم تكن موجودة يظهر ذلك بصدق
   («لا يوجد محرك تعرّف على هذا الجهاز») ويبقى الإدخال النصي يعمل.
4. **الدخول عبر Google/Apple** موجود كزرّين في المكان المطلوب، لكنه **لا ينشئ جلسة وهمية** —
   الربط الحقيقي تتركه لك كما طلبت.
5. **لا استعادة لكلمة المرور** — لأنه لا خادم. هذا مقصود ومذكور في شروط الاستخدام.

---

**تم إنشاء التطبيق بواسطة أبو عمر** · الإصدار 1.0.0
