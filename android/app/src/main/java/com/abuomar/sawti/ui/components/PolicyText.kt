package com.abuomar.sawti.ui.components

/* =============================================================================
 *  سياسة الخصوصية وشروط الاستخدام — نصان كاملان (عربي/إنكليزي).
 *  يُعرضان داخل التطبيق بدون أي اتصال بالإنترنت.
 * ========================================================================== */

/** سياسة الخصوصية — العربية */
const val PRIVACY_POLICY_AR = """
سياسة الخصوصية — صوتي (مدير المصاريف الصوتي)
الإصدار 1.0.0 · آخر تحديث: 1 كانون الثاني 2026

1) مبدأ أساسي
تطبيق «صوتي» يعمل بالكامل على جهازك. لا يوجد خادم، ولا حساب سحابي، ولا رفع
لأي معلومة. كل ما تُدخله — بريدك الإلكتروني، اسمك، كلمة مرورك، ميزانيتك،
مصاريفك، وتسجيلاتك الصوتية — يبقى مخزَّناً داخل مساحة التطبيق الخاصة على
هذا الجهاز فقط.

2) البيانات التي نعالجها ولماذا
 • البريد الإلكتروني والاسم: للتعرف عليك داخل الجهاز وفصل مصاريف حساب عن
   آخر إذا استخدم التطبيق أكثر من شخص على الجهاز نفسه.
 • كلمة المرور: لا تُحفظ أبداً كنص صريح. نحفظ فقط بصمة مشفَّرة ناتجة عن
   PBKDF2-SHA256 مع 150,000 دورة تكرار وملح عشوائي 128-بت لكل حساب. حتى لو
   وصل أحدهم إلى ملفات التطبيق لا يستطيع استخراج كلمة مرورك.
 • المبالغ والفئات والتواريخ والملاحظات: لتوليد الملخصات والتقارير.
 • التسجيلات الصوتية: تُحفظ بصيغة AAC/M4A في مجلد التطبيق الخاص، وتُستخدم
   فقط لكي تتمكن من الاستماع إليها لاحقاً في قسم «الوارد».

3) الميكروفون
لا يُفتح الميكروفون إلا بعد إذن صريح منك، ولا يبدأ التسجيل إلا عندما تضغط
زر الميكروفون بنفسك. لا يوجد تسجيل في الخلفية ولا مراقبة مستمرة. إن رفضت
الإذن يبقى التطبيق يعمل بالكامل، وتكتب المصروف يدوياً أو تُلصق نصاً في خانة
الإرسال فيُحلَّل بالطريقة نفسها.

4) التعرّف على الكلام
يستخدم التطبيق خدمة التعرّف على الكلام المدمجة في نظام أندرويد (SpeechRecognizer).
على بعض الأجهزة تعالج Google هذا الصوت على خوادمها وفق سياسة خصوصية Google،
وعلى أجهزة أخرى تتم المعالجة على الجهاز نفسه. التطبيق لا يرسل نصك أو
مبالغك إلى أي جهة من جهته. إن أردت التأكد أو منع المعالجة الشبكية، راجع
إعدادات «الصوت والإدخال» في نظام جهازك.

5) تحويل الكلام إلى نص بصوت المساعد
ردود المساعد تُنطق عبر محرك النطق في نظام الجهاز (TextToSpeech) ولا تُرسل
إلى أي خادم من التطبيق. خيار «كتم إعادة الصوت بعد التسجيل» يمنع إعادة نطق
كلامك أو تشغيل تسجيلك تلقائياً بعد الحفظ.

6) العملة
المبالغ تُحفظ داخلياً دائماً بوحدة الليرة السورية الجديدة (بعد حذف صفرين
اعتباراً من 1 كانون الثاني 2026). عند اختيار الليرة القديمة يجري التحويل
تلقائياً بقسمة المبلغ على 100. لا يُستخدم أي سعر صرف خارجي ولا أي اتصال
بالإنترنت.

7) النسخ الاحتياطي
إن فعّلت النسخ الاحتياطي في نظام أندرويد، فقد تُنقل تفضيلاتك العامة
(اللغة، السمة، الميزانية) بين أجهزتك. أما بيانات الحساب المشفَّرة
(بصمة كلمة المرور والجلسة) فهي مستثناة من النسخ السحابي ولا تُرفع.
التسجيلات الصوتية لا تُنسخ احتياطياً أبداً.

8) التصدير
زر «تصدير البيانات» ينشئ ملف JSON على جهازك أنت تختار مكانه، ويحتوي
المصاريف فقط (المبلغ، الفئة، التاريخ، التاجر، النص المنطوق، ومدة التسجيل).
لا يحتوي الملف على كلمة مرورك ولا بصمتها ولا على التسجيلات نفسها.

9) الحذف
 • «تسجيل الخروج» يحذف الجلسة فقط ويُبقي بياناتك.
 • «حذف كل المصاريف» يحذف المصاريف والتسجيلات ويُبقي الحساب.
 • «حذف الحساب وكل البيانات» يحذف الحساب وبصمة كلمة المرور وكل المصاريف
   وكل التسجيلات والتفضيلات، ولا يمكن التراجع عنه.
 • إلغاء تثبيت التطبيق يحذف كل ما سبق نهائياً.

10) الأطراف الثالثة
لا نبيع بياناتك ولا نشاركها ولا نعلن داخل التطبيق. لا نستخدم أدوات تتبع
ولا SDKs إعلانية ولا خدمات تحليلية.

11) الأطفال
التطبيق أداة مالية شخصية ولا يستهدف الأطفال دون 13 عاماً.

12) الأمان
البيانات الحساسة تُخزَّن عبر EncryptedSharedPreferences بمفتاح رئيسي AES256-GCM
مولَّد في منطقة Android Keystore الآمنة، والملفات تُكتب بطريقة ذرّية (ملف
مؤقت ثم إعادة تسمية) حتى لا تتلف البيانات عند انقطاع الطاقة.

13) تغييرات هذه السياسة
قد نُحدّث هذا النص عند إضافة ميزة جديدة. يبقى النص الكامل متاحاً داخل
التطبيق في أي وقت، ورقم الإصدار في أعلى الصفحة يدل على نسخته.

14) تواصل
هذا تطبيق محلي بلا خادم، لذا لا توجد بيانات لدينا لنرسلها أو نحذفها.
للاستفسار تواصل مع المطوِّر: أبو عمر.
"""

/** سياسة الخصوصية — الإنكليزية */
const val PRIVACY_POLICY_EN = """
Privacy Policy — Sawti (Voice Expense Manager)
Version 1.0.0 · Last updated: January 1, 2026

1) Core principle
Sawti runs entirely on your device. There is no server, no cloud account and no
upload of any kind. Everything you enter — your email, name, password, budget,
expenses and voice recordings — stays inside this app's private storage on this
device only.

2) Data we process and why
 • Email and name: to identify you on-device and keep separate profiles if more
   than one person uses the app on the same device.
 • Password: never stored in plain text. We store only a PBKDF2-SHA256 digest
   with 150,000 iterations and a random 128-bit salt per account. Even with
   access to the app's files your password cannot be recovered.
 • Amounts, categories, dates and notes: to build your summaries and reports.
 • Voice recordings: stored as AAC/M4A inside the app's private folder, used
   only so you can replay them later from the Inbox.

3) Microphone
The microphone is never opened without your explicit permission, and recording
only starts when you press the mic button yourself. There is no background
recording and no continuous monitoring. If you deny permission the app keeps
working fully: type an expense manually or paste text into the send field and it
is parsed the same way.

4) Speech recognition
The app uses the speech recognition service built into Android (SpeechRecognizer).
On some devices Google processes that audio on its servers under Google's privacy
policy; on others processing happens on-device. The app itself never sends your
text or amounts anywhere. To verify or disable network processing, review the
"Voice & input" settings of your device.

5) Spoken assistant replies
Assistant replies are spoken by the device's Text-to-Speech engine and are never
sent to any server by the app. The "Mute replay after recording" option prevents
your own words or recording from being played back automatically after saving.

6) Currency
Amounts are always stored internally in New Syrian Pounds (two zeros removed as
of January 1, 2026). Choosing the legacy lira converts automatically by dividing
by 100. No external exchange rate and no internet connection is ever used.

7) Backup
If Android backup is enabled on your device, general preferences (language,
theme, budget) may transfer between your devices. Encrypted account data
(password digest and session) is excluded from cloud backup and is never
uploaded. Voice recordings are never backed up.

8) Export
"Export data" creates a JSON file on your device at a location you choose,
containing only expenses (amount, category, date, merchant, transcript and
recording duration). It never contains your password, its digest, or the
recordings themselves.

9) Deletion
 • "Sign out" removes the session only and keeps your data.
 • "Delete all transactions" removes expenses and recordings, keeps the account.
 • "Delete account and all data" removes the account, the password digest, all
   expenses, all recordings and all preferences. This cannot be undone.
 • Uninstalling the app permanently deletes all of the above.

10) Third parties
We do not sell or share your data and show no ads. The app contains no trackers,
no advertising SDKs and no analytics.

11) Children
Sawti is a personal finance tool and is not directed at children under 13.

12) Security
Sensitive data is stored through EncryptedSharedPreferences with an AES256-GCM
master key generated in the Android Keystore, and files are written atomically
(temp file then rename) so data cannot corrupt on power loss.

13) Changes to this policy
We may update this text when a new feature is added. The full text always stays
available inside the app, and the version number at the top identifies it.

14) Contact
This is an offline app with no server, so there is no data held by us to send or
delete. For questions, contact the developer: Abu Omar.
"""

/** شروط الاستخدام — العربية */
const val TERMS_AR = """
شروط الاستخدام — صوتي
الإصدار 1.0.0

1) الترخيص
يُرخَّص لك باستخدام «صوتي» على أجهزة تملكها أو تتحكم بها، لأغراض شخصية.

2) مسؤولية الحساب
أنت مسؤول عن حفظ بريدك الإلكتروني وكلمة مرورك على جهازك. لا توجد وسيلة
لاستعادة كلمة المرور لأن التطبيق لا يملك أي خادم ولا يخزّن كلمة المرور نفسها
— بل بصمتها المشفَّرة فقط. احفظ نسخة احتياطية من بياناتك عبر زر التصدير.

3) دقة النتائج
تحليل الكلام العامي دقيق لكنه ليس معصوماً: قد يخطئ في مبلغ أو فئة أو تاريخ.
راجع البطاقة الظاهرة قبل الإرسال، وعدّل أي حقل يدوياً. الأرقام التي تُدخلها
يدوياً تُعتمد كما هي دائماً.

4) العملة
الوحدة الداخلية للتخزين هي الليرة السورية الجديدة. إذا أدخلت مبالغ بالليرة
القديمة فاختر وحدة العرض أو عملة الميزانية المناسبة حتى يجري التحويل
تلقائياً بقسمة على 100. لا يتحمل التطبيق مسؤولية أخطاء ناتجة عن اختيار
الوحدة الخطأ.

5) التسجيلات
التسجيلات ملكك وتُحفظ على جهازك فقط. أنت مسؤول عن قانونية تسجيل أي شخص آخر،
وعن الحصول على موافقته عند اللزوم.

6) حدود المسؤولية
يُقدَّم التطبيق «كما هو» دون أي ضمان. لا نتحمل مسؤولية أي خسارة مالية أو
فقدان بيانات ناتج عن حذف التطبيق، أو تغيير الجهاز، أو تلف العتاد، أو إعادة
تعيين المصنع. صدّر بياناتك دورياً.

7) بدون خدمات مالية
التطبيق أداة تسجيل ومتابعة فقط، وليس بنكاً ولا خدمة دفع ولا استشارة مالية
ولا يستبدل محاسبك.

8) التغييرات
قد نُحدّث هذه الشروط مع تحديث التطبيق. استمرارك في الاستخدام بعد التحديث
يعني موافقتك عليها.
"""

/** شروط الاستخدام — الإنكليزية */
const val TERMS_EN = """
Terms of Use — Sawti
Version 1.0.0

1) License
You are licensed to use Sawti on devices you own or control, for personal use.

2) Account responsibility
You are responsible for keeping your email and password on your device. There is
no password recovery because the app has no server and never stores your actual
password — only its encrypted digest. Back up your data with the export button.

3) Accuracy of results
Colloquial speech parsing is accurate but not infallible: an amount, category or
date may occasionally be wrong. Review the card shown before sending and edit any
field manually. Numbers you type yourself are always taken as entered.

4) Currency
The internal storage unit is the New Syrian Pound. If you enter amounts in legacy
lira, choose the matching display unit or budget currency so conversion happens
automatically by dividing by 100. The app is not responsible for errors caused by
selecting the wrong unit.

5) Recordings
Recordings belong to you and are stored on your device only. You are responsible
for the legality of recording another person and for obtaining consent where
required.

6) Limitation of liability
The app is provided "as is" without warranty of any kind. We are not liable for
any financial loss or data loss caused by uninstalling, changing device, hardware
failure or factory reset. Export your data regularly.

7) No financial services
Sawti is a tracking tool only. It is not a bank, a payment service, financial
advice, or a replacement for your accountant.

8) Changes
These terms may be updated with app updates. Continued use after an update means
you accept them.
"""

/** بطاقة الاعتماد — تُعرض في نهاية شاشة الإعدادات */
const val CREDIT_AR = "تم إنشاء التطبيق بواسطة أبو عمر"
const val CREDIT_EN = "Created by Abu Omar"
