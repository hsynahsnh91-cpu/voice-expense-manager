/* =============================================================================
 *  Sawti — i18n : ملف الترجمة الكامل (English ⇄ العربية)
 *  التطبيق مصمم بالعربية والإنجليزية معاً: RTL/LTR، بداية الأسبوع، الأرقام،
 *  وتنسيق العملة كلها تتبع اللغة المختارة.
 * ========================================================================== */

export const LOCALES = {
  ar: {
    tag: 'ar-SY',
    dir: 'rtl',
    name: 'العربية (سوريا)',
    weekStart: 6,          // السبت — بداية الأسبوع في سوريا
    speechTags: ['ar-SY', 'ar-LB', 'ar-JO', 'ar'],   // الترتيب المطلوب لمحرك الصوت
    numbering: 'arab',
  },
  en: {
    tag: 'en-US',
    dir: 'ltr',
    name: 'English',
    weekStart: 0,          // Sunday
    speechTags: ['en-US', 'en-GB', 'en'],
    numbering: 'latn',
  },
};

const dict = {
  ar: {
    'app.name': 'صوتي',
    'app.tagline': 'مدير المصاريف بالصوت',
    'app.madeBy': 'تم إنشاء التطبيق بواسطة أبو عمر',
    'app.version': 'الإصدار',

    /* —— تسجيل الدخول —— */
    'auth.signIn': 'تسجيل الدخول',
    'auth.signUp': 'إنشاء حساب جديد',
    'auth.email': 'البريد الإلكتروني',
    'auth.password': 'كلمة المرور',
    'auth.confirmPassword': 'تأكيد كلمة المرور',
    'auth.name': 'الاسم',
    'auth.showPassword': 'إظهار كلمة المرور',
    'auth.hidePassword': 'إخفاء كلمة المرور',
    'auth.continueGoogle': 'المتابعة مع Google',
    'auth.continueApple': 'المتابعة مع Apple',
    'auth.or': 'أو',
    'auth.noAccount': 'ما عندك حساب؟',
    'auth.haveAccount': 'عندك حساب بالفعل؟',
    'auth.createOne': 'أنشئ حساباً',
    'auth.signInLink': 'سجّل دخولك',
    'auth.forgot': 'نسيت كلمة المرور؟',
    'auth.welcomeBack': 'أهلاً بعودتك',
    'auth.welcomeBackSub': 'سجّل دخولك حتى تكمل مصاريفك',
    'auth.createTitle': 'حساب جديد',
    'auth.createSub': 'ثواني وبتبلش تحكي مصاريفك بالصوت',
    'auth.termsNote': 'بإنشاء حساب أنت توافق على سياسة الخصوصية.',
    'auth.err.emailInvalid': 'البريد الإلكتروني غير صحيح.',
    'auth.err.emailTaken': 'هذا البريد مسجّل بالفعل — جرّب تسجيل الدخول.',
    'auth.err.emailNotFound': 'ما في حساب بهاد البريد. أنشئ حساباً جديداً.',
    'auth.err.passwordWrong': 'كلمة المرور غلط.',
    'auth.err.passwordShort': 'كلمة المرور لازم تكون ٨ محارف على الأقل.',
    'auth.err.passwordWeak': 'كلمة المرور لازم تحتوي حروفاً وأرقاماً.',
    'auth.err.passwordMismatch': 'كلمتا المرور غير متطابقتين.',
    'auth.err.nameRequired': 'اكتب اسمك من فضلك.',
    'auth.err.generic': 'صار خطأ، جرّب مرة تانية.',
    'auth.federatedPending': 'تسجيل الدخول عبر Google/Apple غير مفعّل بعد على هذا الجهاز.',
    'auth.secureNote': 'بياناتك محفوظة على جهازك ومشفّرة — ما بتطلع لأي سيرفر.',
    'auth.signingIn': 'عم نسجّل دخولك…',
    'auth.creating': 'عم ننشئ حسابك…',

    /* —— الميزانية / بعد الدخول —— */
    'budget.title': 'ما ميزانيتك؟',
    'budget.subtitle': 'حدّد مصروفك الشهري بالليرة السورية الجديدة، وبعدين ادخل للتطبيق.',
    'budget.amountLabel': 'الميزانية الشهرية',
    'budget.amountPlaceholder': 'مثال: 2000000',
    'budget.currencyLabel': 'عملة الميزانية',
    'budget.currencyNew': 'ليرة سورية جديدة (ل.س)',
    'budget.currencyLegacy': 'ليرة سورية قديمة (قبل حذف الصفرين)',
    'budget.hintLegacy': 'كل ١٠٠ ليرة قديمة = ١ ليرة سورية جديدة.',
    'budget.quick': 'مبالغ سريعة',
    'budget.enter': 'الدخول للتطبيق',
    'budget.saving': 'عم نحفظ…',
    'budget.err.amount': 'اكتب مبلغاً صحيحاً أكبر من صفر.',
    'budget.err.range': 'المبلغ كبير كتير — الحد الأقصى ٩٩٩ مليار ليرة.',
    'budget.edit': 'تعديل الميزانية',
    'budget.convertedNote': 'بالليرة الجديدة: {amount}',

    /* —— التنقل السفلي —— */
    'nav.transactions': 'المصاريف',
    'nav.voice': 'الصوت',
    'nav.inbox': 'الوارد',
    'nav.settings': 'الإعدادات',

    /* —— شاشة المصاريف —— */
    'tx.title': 'مصاريفك',
    'tx.empty': 'لسه ما في مصاريف',
    'tx.emptyHint': 'اضغط على «الصوت» واحكي مصروفك بالعامية، متل: «كلفني ٥٠٠٠ ليرة روح على دمشق».',
    'tx.add': 'إضافة مصروف',
    'tx.edit': 'تعديل المصروف',
    'tx.delete': 'حذف',
    'tx.deleted': 'انحذف المصروف.',
    'tx.saved': 'انحفظ المصروف ✅',
    'tx.spent': 'المصروف',
    'tx.remaining': 'الباقي',
    'tx.income': 'الوارد',
    'tx.balance': 'الرصيد',
    'tx.overBudget': 'تجاوزت الميزانية!',
    'tx.thisMonth': 'هاد الشهر',
    'tx.dateRange': 'الفترة',
    'tx.allTime': 'كل الأوقات',
    'tx.thisWeek': 'هاد الأسبوع',
    'tx.lastMonth': 'الشهر الماضي',
    'tx.custom': 'فترة مخصصة',
    'tx.filterCategory': 'الفئة',
    'tx.allCategories': 'كل الفئات',
    'tx.search': 'بحث…',
    'tx.amount': 'المبلغ',
    'tx.category': 'الفئة',
    'tx.date': 'التاريخ',
    'tx.note': 'ملاحظة',
    'tx.merchant': 'المكان / التاجر',
    'tx.listen': 'استمع للتسجيل',
    'tx.noAudio': 'ما في تسجيل صوتي لهذا المصروف.',
    'tx.count': '{n} مصروف',
    'tx.byCategory': 'حسب الفئة',
    'tx.progress': 'استهلكت {pct}٪ من ميزانيتك',

    /* —— شاشة الصوت —— */
    'voice.title': 'احكي مصروفك',
    'voice.subtitle': 'اضغط الميكروفون واحكي بالعامية — متل: «صرفت ٢٥ ألف على الفروج»',
    'voice.tap': 'اضغط لتسجّل',
    'voice.listening': 'عم أسمعك… احكي هلأ',
    'voice.thinking': 'عم أفهم الكلام…',
    'voice.processing': 'عم أحوّل الصوت لنص…',
    'voice.stop': 'إيقاف',
    'voice.cancel': 'إلغاء',
    'voice.send': 'إرسال',
    'voice.editBefore': 'فيك تعدّل النص قبل الإرسال',
    'voice.transcriptPlaceholder': 'هون بيظهر اللي حكيتو… أو اكتبو بإيدك',
    'voice.detected': 'فهمتك هيك:',
    'voice.parsedAmount': 'المبلغ',
    'voice.parsedCategory': 'الفئة',
    'voice.parsedDate': 'التاريخ',
    'voice.parsedPlace': 'المكان',
    'voice.parsedType': 'النوع',
    'voice.confirmAmount': 'تأكيد المبلغ',
    'voice.noAmount': 'ما قدرت أفهم المبلغ من كلامك — حدّدو بإيدك.',
    'voice.engineReal': 'المحرك: تعرّف صوتي حقيقي من النظام ({engine})',
    'voice.engineNone': 'ما في محرك تعرّف صوتي على هذا الجهاز',
    'voice.notSupported': 'متصفحك ما بيدعم التعرّف الصوتي. جرّب Chrome أو Edge، أو استخدم تطبيق أندرويد.',
    'voice.permissionNeeded': 'لازم إذن الميكروفون حتى تسمعني.',
    'voice.permissionDenied': 'انرفض إذن الميكروفون. افتح إعدادات المتصفح وامنح الإذن.',
    'voice.grantPermission': 'امنح إذن الميكروفون',
    'voice.micInUse': 'الميكروفون مشغول بتطبيق تاني.',
    'voice.network': 'ما في نت — التعرّف الصوتي بحاجة لاتصال (إلا إذا نزلت الحزمة اللغوية).',
    'voice.noResult': 'ما سمعت شي واضح. جرّب مرة تانية واحكي أقرب للميكروفون.',
    'voice.examples': 'جرّب تقول:',
    'voice.ex1': 'كلفني ٥٠٠٠ ليرة سوري روح على دمشق',
    'voice.ex2': 'دفعت فاتورة الكهربا مية ألف',
    'voice.ex3': 'اشتريت فروج بخمسة وعشرين ألف مبارح',
    'voice.ex4': 'صرفت ٢٠٠٠٠٠ على مونة البيت',
    'voice.ex5': 'قبضت راتبي مليون ونص',
    'voice.recordingAudio': 'عم نسجّل الصوت كمان حتى ترجع تسمعو من «الوارد»',
    'voice.mutedAfterSave': 'الكتم مفعّل — ما رح ينعاد صوتك بعد الإرسال.',
    'voice.speakBack': 'نطق الرد',
    'voice.speakBackHint': 'بيقرا المساعد جوابه بصوت بعد كل مصروف.',
    'voice.savedToast': 'انضاف المصروف: {amount}',
    'voice.holdToRecord': 'اضغط مطولاً أو ضغطة وحدة',
    'voice.language': 'لغة التعرّف',

    /* —— شاشة الوارد —— */
    'inbox.title': 'الوارد',
    'inbox.subtitle': 'كل تسجيلاتك الصوتية محفوظة هون — اسمعها أي وقت تتأكد.',
    'inbox.empty': 'ما في تسجيلات بعد',
    'inbox.emptyHint': 'أي مصروف تسجّلو بالصوت بينحفظ تسجيلو هون.',
    'inbox.play': 'تشغيل التسجيل',
    'inbox.pause': 'إيقاف مؤقت',
    'inbox.listen': 'استمع',
    'inbox.noAudioFile': 'هاد المصروف ما الو تسجيل (انضاف كتابةً).',
    'inbox.markListened': 'تحديد كمستمع',
    'inbox.unheard': 'ما انسمعت بعد',
    'inbox.heard': 'انسمعت',
    'inbox.duration': '{s} ثانية',
    'inbox.delete': 'حذف التسجيل',
    'inbox.deleted': 'انحذف التسجيل.',
    'inbox.voiceNote': 'تسجيل صوتي',
    'inbox.of': 'من',
    'inbox.storageNote': 'التسجيلات محفوظة على جهازك فقط.',
    'inbox.mutedBadge': 'الكتم مفعّل',

    /* —— شاشة الإعدادات —— */
    'settings.title': 'الإعدادات',
    'settings.account': 'الحساب',
    'settings.signedInAs': 'مسجّل الدخول كـ',
    'settings.signOut': 'تسجيل الخروج',
    'settings.signOutConfirm': 'متأكد بدك تطلع؟ بياناتك بتبقى محفوظة على الجهاز.',
    'settings.deleteAccount': 'حذف الحساب وكل البيانات',
    'settings.deleteConfirm': 'هاد الإجراء نهائي: بينحذف حسابك وكل مصاريفك وكل تسجيلاتك الصوتية. متابعة؟',
    'settings.deleted': 'انحذف الحساب وكل البيانات.',
    'settings.language': 'اللغة',
    'settings.appearance': 'المظهر',
    'settings.themeLight': 'فاتح',
    'settings.themeDark': 'داكن',
    'settings.themeSystem': 'حسب النظام',
    'settings.voice': 'الصوت والتسجيل',
    'settings.mutePlayback': 'كتم إعادة الصوت بعد التسجيل',
    'settings.mutePlaybackHint': 'لما تفعّلو: بعد ما تسجّل مصروفك بالصوت وتضغط «إرسال»، ما بينعاد نطق الكلام اللي قلتو ولا بينقرأ التسجيل تلقائياً — بينحفظ بصمت بخانة الوارد.',
    'settings.speakResponses': 'نطق ردود المساعد',
    'settings.speakResponsesHint': 'المساعد بيقلك «انضاف المصروف…» بصوت بعد كل عملية.',
    'settings.voiceRate': 'سرعة الصوت',
    'settings.voicePitch': 'طبقة الصوت',
    'settings.testVoice': 'جرّب الصوت',
    'settings.testVoiceText': 'أهلاً فيك بصوتي، مدير مصاريفك بالعامية. احكي مصروفك وأنا بسمعلك.',
    'settings.currencySection': 'العملة والميزانية',
    'settings.monthlyBudget': 'الميزانية الشهرية',
    'settings.displayUnit': 'وحدة العرض',
    'settings.displayUnitHint': 'اقلب العرض بين الليرة الجديدة والقديمة (القيمة المخزّنة دائماً بالجديدة).',
    'settings.data': 'البيانات',
    'settings.export': 'تصدير البيانات (JSON)',
    'settings.exported': 'تم التصدير.',
    'settings.import': 'استيراد بيانات',
    'settings.imported': 'تم الاستيراد بنجاح.',
    'settings.importError': 'الملف غير صالح.',
    'settings.clearTx': 'حذف كل المصاريف',
    'settings.clearTxConfirm': 'بينحذفوا كل المصاريف والتسجيلات. متابعة؟',
    'settings.about': 'حول التطبيق',
    'settings.privacy': 'سياسة الخصوصية',
    'settings.terms': 'شروط الاستخدام',
    'settings.version': 'رقم الإصدار',
    'settings.build': 'البناء',
    'settings.madeBy': 'تم إنشاء التطبيق بواسطة أبو عمر',
    'settings.credits': 'صُنع بحب في سوريا 🇸🇾',
    'settings.engineStatus': 'حالة محرك الصوت',
    'settings.engineOk': 'محرك التعرّف الصوتي جاهز ✅',
    'settings.engineMissing': 'لا يوجد محرك تعرّف صوتي — ثبّت «خدمات الكلام من Google» من المتجر.',
    'settings.storageUsed': 'المساحة المستخدمة للتسجيلات',

    /* —— منتقي التاريخ —— */
    'date.title': 'اختر الفترة',
    'date.from': 'من',
    'date.to': 'إلى',
    'date.today': 'اليوم',
    'date.clear': 'مسح',
    'date.apply': 'تطبيق',
    'date.pickMonth': 'الشهر',
    'date.prevMonth': 'الشهر السابق',
    'date.nextMonth': 'الشهر التالي',
    'date.prevYear': 'السنة السابقة',
    'date.nextYear': 'السنة التالية',
    'date.rangeHint': 'اضغط يوم البداية ثم يوم النهاية. الأسهم للتنقل، PageUp/PageDown للشهر.',
    'date.selected': 'المحدد',
    'date.days': '{n} يوم',
    'date.calendar': 'التقويم',

    /* —— الفئات —— */
    'cat.food': 'أكل وشرب',
    'cat.transport': 'مواصلات',
    'cat.bills': 'فواتير',
    'cat.health': 'صحة ودوا',
    'cat.education': 'تعليم',
    'cat.clothing': 'لبس',
    'cat.home': 'البيت',
    'cat.family': 'عائلة وأولاد',
    'cat.gifts': 'هدايا وعزايم',
    'cat.personal': 'شخصي',
    'cat.business': 'شغل ومصلحة',
    'cat.debt': 'ديون وأقساط',
    'cat.other': 'غير مصنف',
    'type.expense': 'مصروف',
    'type.income': 'دخل',
    'type.debt': 'دين',

    /* —— عام —— */
    'common.cancel': 'إلغاء',
    'common.save': 'حفظ',
    'common.confirm': 'تأكيد',
    'common.close': 'إغلاق',
    'common.back': 'رجوع',
    'common.next': 'التالي',
    'common.done': 'تم',
    'common.loading': 'عم يحمّل…',
    'common.yes': 'نعم',
    'common.no': 'لا',
    'common.optional': 'اختياري',
    'common.required': 'مطلوب',
    'common.error': 'خطأ',
    'common.newSyrianPound': 'ليرة سورية جديدة',
    'common.legacySyrianPound': 'ليرة سورية قديمة',
  },

  en: {
    'app.name': 'Sawti',
    'app.tagline': 'Voice Expense Manager',
    'app.madeBy': 'App created by Abu Omar',
    'app.version': 'Version',

    'auth.signIn': 'Sign in',
    'auth.signUp': 'Create account',
    'auth.email': 'Email address',
    'auth.password': 'Password',
    'auth.confirmPassword': 'Confirm password',
    'auth.name': 'Name',
    'auth.showPassword': 'Show password',
    'auth.hidePassword': 'Hide password',
    'auth.continueGoogle': 'Continue with Google',
    'auth.continueApple': 'Continue with Apple',
    'auth.or': 'or',
    'auth.noAccount': "Don't have an account?",
    'auth.haveAccount': 'Already have an account?',
    'auth.createOne': 'Create one',
    'auth.signInLink': 'Sign in',
    'auth.forgot': 'Forgot password?',
    'auth.welcomeBack': 'Welcome back',
    'auth.welcomeBackSub': 'Sign in to pick up your expenses',
    'auth.createTitle': 'Create your account',
    'auth.createSub': 'A few seconds and you can start speaking your expenses',
    'auth.termsNote': 'By creating an account you agree to the Privacy Policy.',
    'auth.err.emailInvalid': 'Please enter a valid email address.',
    'auth.err.emailTaken': 'This email is already registered — try signing in.',
    'auth.err.emailNotFound': 'No account with this email. Please create one.',
    'auth.err.passwordWrong': 'Incorrect password.',
    'auth.err.passwordShort': 'Password must be at least 8 characters.',
    'auth.err.passwordWeak': 'Password must contain both letters and numbers.',
    'auth.err.passwordMismatch': 'Passwords do not match.',
    'auth.err.nameRequired': 'Please enter your name.',
    'auth.err.generic': 'Something went wrong, please try again.',
    'auth.federatedPending': 'Google/Apple sign-in is not enabled on this device yet.',
    'auth.secureNote': 'Your data stays on this device, encrypted — nothing is sent to a server.',
    'auth.signingIn': 'Signing you in…',
    'auth.creating': 'Creating your account…',

    'budget.title': "What's your budget?",
    'budget.subtitle': 'Set your monthly spending in the New Syrian Pound, then enter the app.',
    'budget.amountLabel': 'Monthly budget',
    'budget.amountPlaceholder': 'e.g. 2000000',
    'budget.currencyLabel': 'Budget currency',
    'budget.currencyNew': 'New Syrian Pound (SYP)',
    'budget.currencyLegacy': 'Old Syrian Pound (pre-redenomination)',
    'budget.hintLegacy': 'Every 100 old pounds = 1 New Syrian Pound.',
    'budget.quick': 'Quick amounts',
    'budget.enter': 'Enter the app',
    'budget.saving': 'Saving…',
    'budget.err.amount': 'Enter a valid amount greater than zero.',
    'budget.err.range': 'Amount too large — max is 999 billion.',
    'budget.edit': 'Edit budget',
    'budget.convertedNote': 'In new pounds: {amount}',

    'nav.transactions': 'Expenses',
    'nav.voice': 'Voice',
    'nav.inbox': 'Inbox',
    'nav.settings': 'Settings',

    'tx.title': 'Your expenses',
    'tx.empty': 'No expenses yet',
    'tx.emptyHint': 'Tap “Voice” and say your expense in colloquial Arabic, e.g. “it cost me 5000 pounds going to Damascus”.',
    'tx.add': 'Add expense',
    'tx.edit': 'Edit expense',
    'tx.delete': 'Delete',
    'tx.deleted': 'Expense deleted.',
    'tx.saved': 'Expense saved ✅',
    'tx.spent': 'Spent',
    'tx.remaining': 'Remaining',
    'tx.income': 'Income',
    'tx.balance': 'Balance',
    'tx.overBudget': 'Over budget!',
    'tx.thisMonth': 'This month',
    'tx.dateRange': 'Period',
    'tx.allTime': 'All time',
    'tx.thisWeek': 'This week',
    'tx.lastMonth': 'Last month',
    'tx.custom': 'Custom range',
    'tx.filterCategory': 'Category',
    'tx.allCategories': 'All categories',
    'tx.search': 'Search…',
    'tx.amount': 'Amount',
    'tx.category': 'Category',
    'tx.date': 'Date',
    'tx.note': 'Note',
    'tx.merchant': 'Place / Merchant',
    'tx.listen': 'Listen to recording',
    'tx.noAudio': 'No voice recording for this expense.',
    'tx.count': '{n} expenses',
    'tx.byCategory': 'By category',
    'tx.progress': 'You used {pct}% of your budget',

    'voice.title': 'Say your expense',
    'voice.subtitle': 'Tap the mic and speak in colloquial Arabic — e.g. “I spent 25 thousand on chicken”.',
    'voice.tap': 'Tap to record',
    'voice.listening': 'Listening… speak now',
    'voice.thinking': 'Understanding your speech…',
    'voice.processing': 'Converting speech to text…',
    'voice.stop': 'Stop',
    'voice.cancel': 'Cancel',
    'voice.send': 'Send',
    'voice.editBefore': 'You can edit the text before sending',
    'voice.transcriptPlaceholder': 'What you say shows up here… or type it',
    'voice.detected': 'Here is what I understood:',
    'voice.parsedAmount': 'Amount',
    'voice.parsedCategory': 'Category',
    'voice.parsedDate': 'Date',
    'voice.parsedPlace': 'Place',
    'voice.parsedType': 'Type',
    'voice.confirmAmount': 'Confirm amount',
    'voice.noAmount': "I couldn't catch an amount — please set it manually.",
    'voice.engineReal': 'Engine: real system speech recognition ({engine})',
    'voice.engineNone': 'No speech recognition engine on this device',
    'voice.notSupported': 'Your browser does not support speech recognition. Try Chrome or Edge, or use the Android app.',
    'voice.permissionNeeded': 'Microphone permission is needed so I can hear you.',
    'voice.permissionDenied': 'Microphone permission denied. Open browser settings and allow it.',
    'voice.grantPermission': 'Grant microphone permission',
    'voice.micInUse': 'The microphone is busy with another app.',
    'voice.network': 'No connection — speech recognition needs internet unless an offline language pack is installed.',
    'voice.noResult': "I didn't hear anything clear. Try again, closer to the mic.",
    'voice.examples': 'Try saying:',
    'voice.ex1': 'It cost me 5000 Syrian pounds going to Damascus',
    'voice.ex2': 'I paid the electricity bill, one hundred thousand',
    'voice.ex3': 'I bought chicken for 25 thousand yesterday',
    'voice.ex4': 'I spent 200 thousand on groceries',
    'voice.ex5': 'I got my salary, one and a half million',
    'voice.recordingAudio': 'Audio is recorded too, so you can replay it from Inbox.',
    'voice.mutedAfterSave': 'Mute is on — your voice will not be replayed after sending.',
    'voice.speakBack': 'Speak responses',
    'voice.speakBackHint': 'The assistant says “expense added…” out loud after each entry.',
    'voice.savedToast': 'Expense added: {amount}',
    'voice.holdToRecord': 'Tap once, or press and hold',
    'voice.language': 'Recognition language',

    'inbox.title': 'Inbox',
    'inbox.subtitle': 'All your voice recordings are kept here — listen any time to verify.',
    'inbox.empty': 'No recordings yet',
    'inbox.emptyHint': 'Every expense you record by voice stores its audio here.',
    'inbox.play': 'Play recording',
    'inbox.pause': 'Pause',
    'inbox.listen': 'Listen',
    'inbox.noAudioFile': 'This expense has no recording (it was typed).',
    'inbox.markListened': 'Mark as listened',
    'inbox.unheard': 'Not listened yet',
    'inbox.heard': 'Listened',
    'inbox.duration': '{s}s',
    'inbox.delete': 'Delete recording',
    'inbox.deleted': 'Recording deleted.',
    'inbox.voiceNote': 'Voice recording',
    'inbox.of': 'from',
    'inbox.storageNote': 'Recordings are stored on your device only.',
    'inbox.mutedBadge': 'Muted',

    'settings.title': 'Settings',
    'settings.account': 'Account',
    'settings.signedInAs': 'Signed in as',
    'settings.signOut': 'Sign out',
    'settings.signOutConfirm': 'Sign out? Your data stays saved on this device.',
    'settings.deleteAccount': 'Delete account and all data',
    'settings.deleteConfirm': 'This is permanent: your account, all expenses and all voice recordings will be deleted. Continue?',
    'settings.deleted': 'Account and all data deleted.',
    'settings.language': 'Language',
    'settings.appearance': 'Appearance',
    'settings.themeLight': 'Light',
    'settings.themeDark': 'Dark',
    'settings.themeSystem': 'System',
    'settings.voice': 'Voice & recording',
    'settings.mutePlayback': 'Mute voice replay after recording',
    'settings.mutePlaybackHint': 'When on: after you record an expense and press “Send”, your own speech is not repeated back and the recording is not auto-played — it is saved silently to Inbox.',
    'settings.speakResponses': 'Speak assistant responses',
    'settings.speakResponsesHint': 'The assistant says “expense added…” out loud after each action.',
    'settings.voiceRate': 'Speech rate',
    'settings.voicePitch': 'Voice pitch',
    'settings.testVoice': 'Test voice',
    'settings.testVoiceText': 'Welcome to Sawti, your colloquial expense manager. Say your expense and I will listen.',
    'settings.currencySection': 'Currency & budget',
    'settings.monthlyBudget': 'Monthly budget',
    'settings.displayUnit': 'Display unit',
    'settings.displayUnitHint': 'Switch the display between new and old pounds (stored value is always new).',
    'settings.data': 'Data',
    'settings.export': 'Export data (JSON)',
    'settings.exported': 'Exported.',
    'settings.import': 'Import data',
    'settings.imported': 'Imported successfully.',
    'settings.importError': 'Invalid file.',
    'settings.clearTx': 'Delete all expenses',
    'settings.clearTxConfirm': 'All expenses and recordings will be deleted. Continue?',
    'settings.about': 'About',
    'settings.privacy': 'Privacy Policy',
    'settings.terms': 'Terms of Use',
    'settings.version': 'Version',
    'settings.build': 'Build',
    'settings.madeBy': 'App created by Abu Omar',
    'settings.credits': 'Made with love in Syria 🇸🇾',
    'settings.engineStatus': 'Speech engine status',
    'settings.engineOk': 'Speech recognition engine ready ✅',
    'settings.engineMissing': 'No recognition engine — install “Speech Services by Google” from the store.',
    'settings.storageUsed': 'Storage used by recordings',

    'date.title': 'Choose period',
    'date.from': 'From',
    'date.to': 'To',
    'date.today': 'Today',
    'date.clear': 'Clear',
    'date.apply': 'Apply',
    'date.pickMonth': 'Month',
    'date.prevMonth': 'Previous month',
    'date.nextMonth': 'Next month',
    'date.prevYear': 'Previous year',
    'date.nextYear': 'Next year',
    'date.rangeHint': 'Tap a start day then an end day. Arrow keys move by day/week, PageUp/PageDown by month.',
    'date.selected': 'Selected',
    'date.days': '{n} days',
    'date.calendar': 'Calendar',

    'cat.food': 'Food & Drink',
    'cat.transport': 'Transport',
    'cat.bills': 'Bills',
    'cat.health': 'Health',
    'cat.education': 'Education',
    'cat.clothing': 'Clothing',
    'cat.home': 'Home',
    'cat.family': 'Family & Kids',
    'cat.gifts': 'Gifts & Events',
    'cat.personal': 'Personal',
    'cat.business': 'Business',
    'cat.debt': 'Debts',
    'cat.other': 'Other',
    'type.expense': 'Expense',
    'type.income': 'Income',
    'type.debt': 'Debt',

    'common.cancel': 'Cancel',
    'common.save': 'Save',
    'common.confirm': 'Confirm',
    'common.close': 'Close',
    'common.back': 'Back',
    'common.next': 'Next',
    'common.done': 'Done',
    'common.loading': 'Loading…',
    'common.yes': 'Yes',
    'common.no': 'No',
    'common.optional': 'Optional',
    'common.required': 'Required',
    'common.error': 'Error',
    'common.newSyrianPound': 'New Syrian Pound',
    'common.legacySyrianPound': 'Old Syrian Pound',
  },
};

/* سياسة الخصوصية — نص كامل باللغتين (تُعرض داخل التطبيق) */
export const PRIVACY_POLICY = {
  ar: `سياسة الخصوصية — تطبيق «صوتي»

آخر تحديث: ٤ تشرين الأول ٢٠٢٦
الإصدار: 1.0.0

١) من نحن
«صوتي» هو تطبيق لإدارة المصاريف بالصوت، طوّره أبو عمر. هذه السياسة تشرح أي بيانات نجمعها وكيف تُستخدم وأين تُحفظ.

٢) البيانات التي تُحفظ
• بيانات الحساب: اسمك، بريدك الإلكتروني، وبصمة كلمة المرور (تجزئة PBKDF2 مع ملح عشوائي — لا تُحفظ كلمة المرور نفسها أبداً).
• المصاريف: المبلغ بالليرة السورية الجديدة، الفئة، التاريخ، المكان/التاجر، والملاحظات.
• التسجيلات الصوتية: ملفات صوتك التي تسجّلها عند إضافة مصروف بالصوت، والنص المحوَّل من الصوت.
• التفضيلات: اللغة، المظهر، الميزانية الشهرية، إعدادات الصوت (سرعة النطق، الطبقة، كتم إعادة الصوت، نطق الردود).

٣) أين تُحفظ البيانات (مهم)
كل البيانات المذكورة تُحفظ محلياً على جهازك فقط:
• في تطبيق الويب: في IndexedDB و localStorage داخل متصفحك.
• في تطبيق أندرويد: في مجلد البيانات الخاص بالتطبيق، وكلمة المرور والتفضيلات داخل EncryptedSharedPreferences المشفّرة بمفتاح من Android Keystore.
لا يوجد سيرفر خاص بالتطبيق، ولا نرفع مصاريفك أو تسجيلاتك أو بيانات حسابك إلى أي مكان.

٤) الميكروفون والصوت
• نطلب إذن الميكروفون لغرض واحد فقط: تحويل كلامك إلى نص لإضافة مصروف، وتسجيل مقطع صوتي تستطيع الرجوع إليه من قسم «الوارد».
• تحويل الصوت إلى نص يتم عبر محرّك التعرّف الصوتي الموجود في نظام التشغيل (على أندرويد: SpeechRecognizer / خدمات الكلام من Google). قد يُرسل هذا المحرّك الصوت إلى مزوّده (Google) لمعالجته إذا لم تكن الحزمة اللغوية مثبّتة على الجهاز — وهذه المعالجة تخضع لسياسة خصوصية Google وليس لسياستنا، ولا نتحكم بها.
• يمكنك تفعيل خيار «كتم إعادة الصوت بعد التسجيل» في الإعدادات، وعندها لا يُعاد نطق كلامك ولا يُشغَّل تسجيلك تلقائياً بعد الإرسال.
• يمكنك حذف أي تسجيل صوتي من قسم «الوارد» في أي وقت، ويُحذف الملف نهائياً من الجهاز.

٥) ما لا نفعله
• لا نبيع بياناتك ولا نشاركها مع أي جهة إعلانية.
• لا نضع ملفات تعريف ارتباط (Cookies) للتتبع ولا أدوات تحليل إعلانية.
• لا نطلب أذونات لا حاجة لها (لا جهات اتصال، لا موقع، لا رسائل، لا كاميرا).
• لا توجد حسابات تجريبية (demo) ولا بيانات وهمية داخل التطبيق.

٦) أذونات أندرويد المستخدمة
• android.permission.RECORD_AUDIO — تسجيل الصوت وتحويله إلى نص.
• android.permission.INTERNET و ACCESS_NETWORK_STATE — لعمل محرّك التعرّف الصوتي الخاص بالنظام فقط.
• لا نستخدم أي إذن تخزين خارجي؛ التسجيلات تُحفظ في مساحة التطبيق الخاصة.

٧) دخولك وبياناتك
• تسجيل الدخول يتم بالبريد الإلكتروني وكلمة المرور، ويُتحقق منهما محلياً مقابل البصمة المحفوظة على جهازك. لا يمكن الدخول بأي بريد وكلمة مرور عشوائية.
• أزرار «المتابعة مع Google / Apple» غير مفعّلة حالياً وستُربط لاحقاً بمزوّد الدخول الخاص بك.
• من الإعدادات تستطيع: تسجيل الخروج، تصدير بياناتك بصيغة JSON، استيرادها، حذف كل المصاريف، أو حذف الحساب وكل البيانات نهائياً.

٨) الاحتفاظ والحذف
تبقى البيانات على جهازك حتى تحذفها أنت. حذف التطبيق أو مسح بياناته من إعدادات النظام يحذف كل شيء نهائياً ولا يمكن استرجاعه.

٩) الأطفال
التطبيق غير موجّه لمن هم دون ١٣ سنة، ولا نجمع بيانات أطفال عن قصد.

١٠) التغييرات على هذه السياسة
قد نحدّث السياسة مع تحديثات التطبيق؛ يُذكر تاريخ آخر تحديث في الأعلى ويُعرض رقم الإصدار الجديد داخل التطبيق.

١١) التواصل
لأي استفسار حول الخصوصية تواصل مع المطوّر: أبو عمر — عبر قناة الدعم داخل التطبيق أو البريد الذي حصلت عليه منه.

١٢) العملة
كل المبالغ في التطبيق تُحفظ وتُعرض بالليرة السورية الجديدة (حذف صفرين اعتباراً من ١ كانون الثاني ٢٠٢٦)، وكل ١٠٠ ليرة قديمة تعادل ١ ليرة سورية جديدة. لا يقدّم التطبيق أسعار صرف ولا نصائح مالية.`,

  en: `Privacy Policy — "Sawti" app

Last updated: 4 October 2026
Version: 1.0.0

1) Who we are
"Sawti" is a voice-based expense manager created by Abu Omar. This policy explains what data is collected, how it is used, and where it is stored.

2) Data that is stored
• Account data: your name, email address, and a password digest (PBKDF2 hash with a random salt — the password itself is never stored).
• Expenses: amount in the New Syrian Pound, category, date, place/merchant, and notes.
• Voice recordings: the audio clips you record when adding an expense by voice, plus the transcript.
• Preferences: language, theme, monthly budget, and voice settings (rate, pitch, mute-after-recording, speak responses).

3) Where the data lives (important)
All of the above is stored locally on your device only:
• In the web app: in IndexedDB and localStorage inside your browser.
• In the Android app: in the app's private data directory, with the password digest and preferences inside EncryptedSharedPreferences backed by the Android Keystore.
There is no app backend. Your expenses, recordings and account data are never uploaded anywhere.

4) Microphone and audio
• We request microphone permission for one purpose only: turning your speech into text to add an expense, and recording a clip you can replay later from the Inbox tab.
• Speech-to-text runs through the operating system's recognition engine (on Android: SpeechRecognizer / Speech Services by Google). That engine may send audio to its provider (Google) for processing when no on-device language pack is installed. That processing is governed by Google's privacy policy, not ours, and is outside our control.
• You can enable "Mute voice replay after recording" in Settings; then your own speech is never repeated and your clip is never auto-played after sending.
• You can delete any recording from Inbox at any time; the file is permanently removed from the device.

5) What we do not do
• We never sell or share your data with advertisers.
• No tracking cookies, no advertising analytics SDKs.
• No unnecessary permissions (no contacts, location, SMS, or camera).
• No demo accounts and no fake/seed data anywhere in the app.

6) Android permissions used
• android.permission.RECORD_AUDIO — recording audio and speech-to-text.
• android.permission.INTERNET and ACCESS_NETWORK_STATE — required only by the system speech engine.
• No external-storage permission; recordings live in the app's private space.

7) Sign-in and your data
• Sign-in uses email and password, verified locally against the digest stored on your device. Random email/password combinations do not work; you must create an account first.
• "Continue with Google / Apple" buttons are not enabled yet and will be wired to your own identity provider later.
• From Settings you can: sign out, export your data as JSON, import it, delete all expenses, or permanently delete the account and all data.

8) Retention and deletion
Data remains on your device until you delete it. Uninstalling the app or clearing its data from system settings erases everything permanently and it cannot be recovered.

9) Children
The app is not directed to children under 13, and we do not knowingly collect children's data.

10) Changes to this policy
We may update this policy with app updates; the last-updated date appears at the top and the new version number is shown inside the app.

11) Contact
For privacy questions contact the developer: Abu Omar — via the in-app support channel or the email you received it from.

12) Currency
All amounts are stored and displayed in the New Syrian Pound (two zeros removed effective 1 January 2026); every 100 old pounds equal 1 New Syrian Pound. The app provides no exchange rates and no financial advice.`,
};

export const TERMS = {
  ar: `شروط الاستخدام — «صوتي» (الإصدار 1.0.0)

١) الاستخدام: التطبيق أداة شخصية لتنظيم المصاريف بالصوت. أنت مسؤول عن صحة ما تُدخله.
٢) لا نصائح مالية: التطبيق لا يقدّم استشارات مالية أو استثمارية ولا أسعار صرف.
٣) البيانات على مسؤوليتك: كل شيء يُحفظ على جهازك. خذ نسخة احتياطية عبر «تصدير البيانات» لأن حذف التطبيق أو بياناته نهائي ولا رجعة فيه.
٤) الحساب: يجب إنشاء حساب ببريد وكلمة مرور حقيقية للدخول. لا توجد حسابات تجريبية.
٥) الميكروفون: استخدام الميكروفون اختياري؛ يمكنك كتابة المصاريف يدوياً.
٦) العملة: المبالغ بالليرة السورية الجديدة، وكل ١٠٠ ليرة قديمة = ١ ليرة جديدة.
٧) الملكية: التطبيق من تطوير أبو عمر. يُمنع إعادة بيعه أو نسبته لغير مطوّره.
٨) بدون ضمان: يُقدَّم التطبيق «كما هو» دون أي ضمان صريح أو ضمني، ولا يتحمّل المطوّر مسؤولية أي خسارة بيانات.
٩) التعديل: قد تتغيّر الشروط مع التحديثات، ويُعدّ استمرارك في الاستخدام موافقة عليها.`,

  en: `Terms of Use — "Sawti" (version 1.0.0)

1) Use: A personal tool for organising expenses by voice. You are responsible for the accuracy of what you enter.
2) No financial advice: The app gives no financial, investment, or exchange-rate advice.
3) Your data, your responsibility: Everything is stored on your device. Take backups via "Export data" — deleting the app or its data is permanent and irreversible.
4) Account: You must create a real account with email and password to sign in. There are no demo accounts.
5) Microphone: Voice input is optional; you can type expenses manually.
6) Currency: Amounts are in the New Syrian Pound; every 100 old pounds = 1 new pound.
7) Ownership: Developed by Abu Omar. Reselling it or attributing it to anyone else is not permitted.
8) No warranty: Provided "as is" without any express or implied warranty; the developer is not liable for data loss.
9) Changes: Terms may change with updates; continued use counts as acceptance.`,
};

/* ============================== محرك الترجمة ============================== */
let current = 'ar';
const listeners = new Set();

export function setLocale(l) {
  if (!LOCALES[l]) return;
  current = l;
  listeners.forEach(fn => fn(l));
}
export function getLocale() { return current; }
export function getDir() { return LOCALES[current].dir; }
export function onChange(fn) { listeners.add(fn); return () => listeners.delete(fn); }

export function t(key, vars) {
  const table = dict[current] || dict.ar;
  let s = table[key];
  if (s === undefined) s = (dict.ar[key] !== undefined ? dict.ar[key] : key);
  if (vars) for (const k in vars) s = s.replace(new RegExp(`\\{${k}\\}`, 'g'), vars[k]);
  return s;
}

/** اسم الفئة مترجماً حسب اللغة الحالية */
export function categoryName(id) { return t('cat.' + id); }
