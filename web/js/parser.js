/* =============================================================================
 *  Sawti — Voice Expense Manager
 *  parser.js : محرك فهم الكلام العامّي (Levantine / Syrian Arabic NLU)
 *  ---------------------------------------------------------------------------
 *  يحول جملة مثل:
 *      "كلفني 5000 ليرة سوري روح على دمشق"
 *      "صرفت خمسة آلاف على الخضرة من السوق مبارح"
 *      "دفعت فاتورة الكهرباء مية ألف"
 *  إلى كائن مصروف منظم: { المبلغ، الوحدة، الفئة، التاريخ، المكان/التاجر، النوع، الملخص }
 *
 *  ملاحظة العملة: الليرة السورية الجديدة (حذف صفرين من 1 كانون الثاني 2026)
 *  كل 100 ليرة قديمة = 1 ليرة سورية جديدة.
 * ========================================================================== */

export const CURRENCY_NEW = {
  code: 'LSN',            // الليرة السورية الجديدة
  legacyCode: 'SYP',
  arSymbol: 'ل.س',
  enSymbol: 'SYP',
  arName: 'ليرة سورية جديدة',
  enName: 'New Syrian Pound',
  decimals: 0,
  legacyDivisor: 100,     // القديمة → الجديدة
};

/* ------------------------------- تطبيع النص ------------------------------- */
const DIACRITICS = /[\u064B-\u065F\u0670\u0640\u06D6-\u06ED]/g;

export function normalizeAr(input) {
  if (!input) return '';
  let s = String(input).replace(DIACRITICS, ' ');
  s = s.replace(/[إأآٱ]/g, 'ا')
       .replace(/ى/g, 'ي')
       .replace(/ؤ/g, 'و')
       .replace(/ئ/g, 'ي')
       .replace(/ة/g, 'ه')
       .replace(/[٠-٩]/g, d => String('٠١٢٣٤٥٦٧٨٩'.indexOf(d)))
       .replace(/[۰-۹]/g, d => String('۰۱۲۳۴۵۶۷۸۹'.indexOf(d)))
       .replace(/[^\p{L}\p{N}\s.,\-+'/]/gu, ' ')
       .replace(/\s+/g, ' ')
       .trim();
  return s;
}

const tokenize = s => normalizeAr(s).split(' ').filter(Boolean);

function hasAny(text, list) {
  return list.some(w => text.includes(w));
}

/* ------------------------------- الأرقام ---------------------------------- */
const ONES = {
  'واحد': 1, 'واحده': 1, 'واحده': 1, 'احد': 1, 'ا one': 1,
  'اثنين': 2, 'اتنين': 2, 'تنين': 2, 'جوج': 2, 'زوج': 2,
  'ثلاثه': 3, 'تلاته': 3, 'ثلاتة': 3, 'ثلاث': 3,
  'اربعه': 4, 'اربع': 4, 'اريعه': 4,
  'خمسه': 5, 'خمس': 5, 'خمسه': 5,
  'سته': 6, 'ست': 6, 'سته': 6, 'ستي': 6,
  'سبعه': 7, 'سبع': 7,
  'ثمانيه': 8, 'تمانيه': 8, 'تمان': 8, 'ثماني': 8, 'تماني': 8, 'تمنية': 8,
  'تسعه': 9, 'تسع': 9,
  'عشره': 10, 'عشر': 10, 'عشرة': 10,
};
const TEENS = {
  'احداش': 11, 'احدي عشر': 11, 'احدياشر': 11, 'احد عشر': 11,
  'طناش': 12, 'اثنا عشر': 12, 'اثني عشر': 12, 'اتناشر': 12, 'اثني عشر': 12,
  'ثلطاش': 13, 'ثلاثه عشر': 13, 'ثلاثه عشره': 13, 'تلتاشر': 13,
  'اربطاش': 14, 'اربعه عشر': 14, 'اربعطاش': 14,
  'خمسطاش': 15, 'خمسه عشر': 15, 'خمستاشر': 15,
  'سطاش': 16, 'سته عشر': 16, 'ستطاشر': 16,
  'سبعطاش': 17, 'سبعه عشر': 17, 'سبعتاشر': 17,
  'تمنطاش': 18, 'ثمانيه عشر': 18, 'تمنتاشر': 18,
  'تسعطاش': 19, 'تسعه عشر': 19, 'تسعتاشر': 19,
};
const TENS = {
  'عشره': 10, 'عشرين': 20, 'عشرينا': 20,
  'تلاتين': 30, 'ثلاثين': 30, 'ثلاتين': 30,
  'اربعين': 40, 'اريعين': 40,
  'خمسين': 50, 'خمسین': 50,
  'ستين': 60, 'سيتين': 60,
  'سبعين': 70, 'سبعین': 70,
  'تمانين': 80, 'ثمانين': 80, 'تمنين': 80,
  'تسعين': 90, 'تسعين': 90,
};
const HUNDREDS = {
  'ميه': 100, 'مئه': 100, 'مية': 100, 'مائه': 100,
  'ميته': 200, 'مئتين': 200, 'ميتين': 200, 'مياتين': 200,
  'تلت ميه': 300, 'ثلاث ميه': 300, 'ثلاثميه': 300, 'تلتمية': 300, 'ثلاثمئة': 300,
  'ربع ميه': 400, 'اربع ميه': 400, 'اربعماية': 400, 'اربعمايه': 400, 'اربع مئه': 400,
  'خمسميه': 500, 'خمس ميه': 500, 'خمسمئة': 500, 'خمسمية': 500,
  'ستميه': 600, 'ست ميه': 600, 'ستمئة': 600, 'ستمية': 600,
  'سبعميه': 700, 'سبع ميه': 700, 'سبعمئة': 700, 'سبعمية': 700,
  'تمنميه': 800, 'ثماني ميه': 800, 'ثمانمئة': 800, 'تمنمية': 800,
  'تسعميه': 900, 'تسع ميه': 900, 'تسعمئة': 900, 'تسعمية': 900,
};
const MULT = {
  'الف': 1000, 'الاف': 1000, 'آلاف': 1000, 'الفين': 2000, 'الفين': 2000, 'الفا': 1000,
  'مليون': 1000000, 'ملايين': 1000000, 'مليونين': 2000000,
  'مليار': 1000000000, 'مليارات': 1000000000, 'مليارين': 2000000000,
};
const HALF = { 'ونص': 0.5, 'ونصف': 0.5, 'ونص': 0.5, 'نص': 0.5, 'نصف': 0.5 };

/** يقرأ عبارة رقمية (أرقام أو كلمات) من بداية قائمة الرموز */
function readNumberPhrase(tokens, i) {
  let total = 0;        // المجموع التراكمي (آلاف + وحدات)
  let current = 0;      // الجزء الحالي قبل المضاعف
  let consumed = 0;
  let found = false;

  const tryDigits = (t) => {
    const clean = t.replace(/[^\d.,]/g, '').replace(/,$/, '');
    if (!clean || !/^\d{1,3}(,\d{3})*(\.\d+)?$|^\d+(\.\d+)?$/.test(clean)) return null;
    return parseFloat(clean.replace(/,/g, ''));
  };

  while (i + consumed < tokens.length) {
    const t = tokens[i + consumed];
    if (t === 'و' || t === 'ثم' || t === 'زائد') { consumed++; continue; }

    // 1) أرقام صريحة
    const dv = tryDigits(t);
    if (dv !== null) { current += dv; found = true; consumed++; continue; }

    // 2) مئات مركبة (تلت ميه / خمس ميه ...)
    const two = tokens.slice(i + consumed, i + consumed + 2).join(' ');
    if (HUNDREDS[two] !== undefined) { current += HUNDREDS[two]; found = true; consumed += 2; continue; }

    // 3) مراهقات مركبة (احدي عشر ...)
    if (TEENS[two] !== undefined) { current += TEENS[two]; found = true; consumed += 2; continue; }

    // 4) مراهقة "عشره" بعد وحدة (خمسه عشره)
    if (ONES[t] !== undefined && ONES[t] <= 9 && tokens[i + consumed + 1] &&
        (tokens[i + consumed + 1] === 'عشره' || tokens[i + consumed + 1] === 'عشر')) {
      current += ONES[t] + 10; found = true; consumed += 2; continue;
    }

    // 5) كلمات مفردة
    if (HUNDREDS[t] !== undefined) { current += HUNDREDS[t]; found = true; consumed++; continue; }
    if (TEENS[t] !== undefined) { current += TEENS[t]; found = true; consumed++; continue; }
    if (TENS[t] !== undefined) { current += TENS[t]; found = true; consumed++; continue; }
    if (ONES[t] !== undefined) { current += ONES[t]; found = true; consumed++; continue; }

    // 6) مضاعفات (ألف / مليون / مليار)
    const BASE = { 1000: 1000, 2000: 1000, 1000000: 1000000, 2000000: 1000000,
                   1000000000: 1000000000, 2000000000: 1000000000 };
    let m = MULT[t];
    if (m === undefined) {
      if (t === 'الفين') m = 2000;
      else if (t === 'مليونين') m = 2000000;
      else if (t === 'مليارين') m = 2000000000;
    }
    if (m !== undefined) {
      total += (current === 0 ? 1 : current) * m;   // ألفين = 2000، خمسة آلاف = 5000
      current = 0; found = true; consumed++;
      // "ونص" بعد المضاعف → نص الوحدة الأساسية:
      //   ألف ونص = 1500 | ألفين ونص = 2500 | مليون ونص = 1500000
      const nx = tokens[i + consumed];
      if (nx && HALF[nx] !== undefined) {
        total += (BASE[m] || m) * HALF[nx];
        consumed++;
      }
      continue;
    }

    break;
  }

  total += current;
  return found ? { value: total, consumed } : null;
}

const AMOUNT_TRIGGERS = [
  'ب', 'بمبلغ', 'مبلغ', 'قيمته', 'قيمته', 'ثمنه', 'ثمن', 'سعره', 'سعر', 'تكلفه', 'كلفه',
  'صار', 'صارو', 'اجمالي', 'مجموع', 'قده', 'قد',
];
const CURRENCY_HINTS = [
  'ليره', 'ليرات', 'ليره سوريه', 'سوري', 'سوريه', 'ل س', 'ل.س', 'مصاري', 'فلوس',
];
const LEGACY_HINTS = ['قديمه', 'قديم'];
/** أرقام ما بتدل على مصروف (تواريخ/أوقات/كميات) — تجاهلها إذا لم يسبقها تلميح عملة */
const NON_AMOUNT_NEXT = ['الشهر', 'يوم', 'ايام', 'ساعه', 'ساعات', 'دقيقه', 'دقايق', 'ثانيه',
                         'شهر', 'سنه', 'سنوات', 'مره', 'مرات', 'نفر', 'اشخاص', 'كيلو', 'غرام',
                         'متر', 'سم', 'درجه', 'بيت', 'طابق', 'باص', 'خط'];
const OTHER_CURRENCY = /دولار|يورو|ليره تركيه|تركي|دولارات|usd|eur|دينار|ريال/;

/**
 * يستخرج كل المبالغ المذكورة في الجملة ويرجع أفضل مرشح.
 * يدعم: الأرقام اللاتينية، الأرقام العربية-الهندية (٠١٢٣)، والأرقام بالكلمات،
 * ومزيجها مثل "٣٥ ألف" أو "5 الاف" أو "ميه الف".
 */
export function extractAmount(text) {
  const raw = String(text || '');
  const norm = normalizeAr(raw);          // يحوّل ٣٥ → 35 ويطبّع الهمزات
  if (!norm) return null;
  const tokens = norm.split(' ');
  const candidates = [];
  const push = (value, kind, at, score, span) => {
    if (value > 0 && isFinite(value)) candidates.push({ value, kind, at, score, span });
  };

  for (let i = 0; i < tokens.length; i++) {
    const t = tokens[i];
    const isDigits = /^\d{1,15}$/.test(t.replace(/[.,]/g, '')) && /[\d]/.test(t);
    const prev3 = tokens.slice(Math.max(0, i - 3), i).join(' ');
    const next3 = tokens.slice(i + 1, i + 4).join(' ');
    const hasCurrencyNear = hasAny(prev3 + ' ' + next3, CURRENCY_HINTS) || hasAny(prev3, AMOUNT_TRIGGERS);

    /* ---- أ) رقم صريح (لاتيني أو عربي بعد التطبيع) ---- */
    if (isDigits) {
      const v = parseFloat(t.replace(/,/g, ''));
      // تجاهل أرقام التواريخ/الأوقات/الكميات
      const nextTok = tokens[i + 1] || '';
      const looksLikeDateNum = NON_AMOUNT_NEXT.includes(nextTok) || /^\d{1,2}[\/\-]\d{1,2}/.test(t);
      if (!looksLikeDateNum) {
        let score = 92;
        if (hasCurrencyNear) score += 4;
        push(v, 'digits', i, score, t);
      }
      /* ---- ب) رقم + مضاعف بالكلمة: "٣٥ ألف" ، "5 الاف" ، "١٢ مليون" ---- */
      const multTok = tokens[i + 1];
      let m = multTok ? MULT[multTok] : undefined;
      if (m === undefined && multTok === 'الفين') m = 2000;
      if (m === undefined && multTok === 'مليونين') m = 2000000;
      if (m !== undefined) {
        const BASE = { 1000: 1000, 2000: 1000, 1000000: 1000000, 2000000: 1000000,
                       1000000000: 1000000000, 2000000000: 1000000000 };
        const unitValue = BASE[m] || m;                     // ألفين → الوحدة ألف
        let value = v * unitValue;                          // ٣٥ ألف = 35000
        const halfTok = tokens[i + 2];
        if (halfTok && HALF[halfTok] !== undefined) value += unitValue * HALF[halfTok]; // ٣٥ ألف ونص = 35500
        push(Math.round(value), 'digits+mult', i, 96, `${t} ${multTok}`);
        i += 1;
        continue;
      }
      continue;
    }

    /* ---- ج) مضاعف مفرد بلا رقم قبله: "ألف"، "ألفين"، "مليون" ---- */
    let mSolo = MULT[t];
    if (mSolo === undefined && t === 'الفين') mSolo = 2000;
    if (mSolo === undefined && t === 'مليونين') mSolo = 2000000;
    if (mSolo !== undefined && !isFinite(parseFloat(tokens[i - 1] ?? 'x'))) {
      let value = mSolo;
      const halfTok = tokens[i + 1];
      if (halfTok && HALF[halfTok] !== undefined) {
        const BASE = { 1000: 1000, 2000: 1000, 1000000: 1000000, 2000000: 1000000 };
        value += (BASE[mSolo] || mSolo) * HALF[halfTok];
      }
      push(value, 'mult', i, 78, t);
    }

    /* ---- د) عبارة رقمية بالكلمات: "خمسة آلاف"، "تلت ميه"، "عشرين" ---- */
    const r = readNumberPhrase(tokens, i);
    if (r && r.value > 0 && r.consumed > 0 && r.value >= 2) {
      const span = tokens.slice(i, i + r.consumed).join(' ');
      const nextTok = tokens[i + r.consumed] || '';
      const looksLikeDateNum = NON_AMOUNT_NEXT.includes(nextTok) && !hasCurrencyNear;
      if (!looksLikeDateNum) {
        let score = 55 + Math.min(r.consumed * 7, 21);
        if (hasAny(span + ' ' + next3, CURRENCY_HINTS)) score += 25;
        if (hasAny(prev3, AMOUNT_TRIGGERS)) score += 12;
        if (r.value >= 1000) score += 8;
        push(r.value, 'words', i, score, span);
      }
      i += r.consumed - 1;
    }
  }

  if (!candidates.length) return null;

  candidates.sort((a, b) => (b.score - a.score) || (b.value - a.value));
  const best = candidates[0];

  /* ---- الوحدة: جديدة أم قديمة أم عملة أجنبية ---- */
  const windowStart = Math.max(0, best.span ? norm.indexOf(best.span.split(' ')[0]) : 0);
  const near = norm.slice(Math.max(0, windowStart - 45), windowStart + best.span.length + 55);
  const saidLegacy = hasAny(near, LEGACY_HINTS) && hasAny(near, ['ليره', 'سوري']);
  let unit = 'new';
  let value = Math.round(best.value);
  if (OTHER_CURRENCY.test(near)) unit = 'other';
  else if (saidLegacy) { unit = 'legacy'; value = Math.round(best.value / CURRENCY_NEW.legacyDivisor); }

  return {
    raw: best.value,
    value,                                   // القيمة النهائية بالليرة السورية الجديدة
    unit,                                    // new | legacy | other
    currency: CURRENCY_NEW.code,
    source: best.kind,
    matchedText: best.span || String(best.value),
    confidence: Math.min(0.99, best.score / 100),
  };
}

/* ------------------------------- الفئات ----------------------------------- */
export const CATEGORIES = [
  { id: 'food',        ar: 'أكل وشرب',    en: 'Food & Drink',   color: '#F97316',
    keys: ['اكل','شرب','طعام','فطور','غدا','غداء','عشا','عشاء','مطعم','كافيه','قهوه','شاي','متايه','عصير','فواكه','خضره','خضار','لحمة','لحم','فروج','دجاج','سمك','خبز','سندويش','سندويشه','برغر','بيتزا','شاورما','فلافل','مشاوي','حلويات','حلوي','كنافه','بقلاوه','سكاكر','مكسرات','تمن','رز','برغل','سكر','زيت','سمن','حليب','لبنة','لبني','جبنة','جبنة','بيض','مونة','تموين','سوبرماركت','بقاله','دكان','فرن','معجنات','كرواسان','ايس كريم','بوظه','مياه','بيبسي','كولا','طبخ','وليمه','عزومه','مصروف البيت','بقالة'] },
  { id: 'transport',   ar: 'مواصلات',      en: 'Transport',      color: '#3B82F6',
    keys: ['مواصلات','توصيله','سرفيس','ميكرو','ميكروباص','تكسي','تاكسي','سائق','بنزين','مازوت','ديزل','محروقات','غاز للسيارة','كاز','باص','بولمان','قطار','طيارة','طيران','سفر','مطار','موقف','باركينغ','غسيل سيارة','ميكانيكي','تصليح السيارة','دولاب','بطارية سيارة','سوزوكي','اجرة','ركوب','روح على','راحت على','طلعت على','نزلت على'] },
  { id: 'bills',       ar: 'فواتير',        en: 'Bills',          color: '#8B5CF6',
    keys: ['فاتوره','فواتير','كهربا','كهرباء','مي','ماء','انترنت','نت','واي فاي','موبايل','رصيد','شحن','خط','تلفون','هاتف','غاز','اشتراك','مقنن','مولده','امبير','رسوم','ضريبه','بلديه','تلفزيون'] },
  { id: 'health',      ar: 'صحة ودوا',      en: 'Health',         color: '#EF4444',
    keys: ['دوا','دواء','صيدليه','صيدلية','طبيب','دكتور','مستشفى','عياده','تحليل','اشعه','مبرره','ابره','حقنه','عملية','معاينه','ادويه','بانادول','سيروم','لقاح','تطعيم','اسنان','سن','نظارات','علاج','طبابة'] },
  { id: 'education',   ar: 'تعليم',         en: 'Education',      color: '#14B8A6',
    keys: ['مدرسه','جامعة','معهد','درس','دروس','خصوصي','دوره','قرطاسيه','كتب','كتاب','دفاتر','اقلام','حقيبه مدرسيه','مواصلات المدرسه','اقساط','قسط مدرسي','روضه','روضات','طلاب'] },
  { id: 'clothing',    ar: 'لبس',           en: 'Clothing',       color: '#EC4899',
    keys: ['لبس','ملابس','ثياب','قميص','بنطلون','جاكيت','كنزة','كنزه','فستان','جزدان','حذاء','كندرة','جزمة','جزمه','شرابات','جرابات','شال','اشارب','حجاب','مانطو','بالتو','تي شيرت','تيشرت','طقم','بدله','ملابس ولادي','ملابس بناتي','خياط','مكوي','كوي'] },
  { id: 'home',        ar: 'البيت',         en: 'Home',           color: '#F59E0B',
    keys: ['ايجار','اجار البيت','مفروشات','اثاث','غساله','براد','ثلاجه','بوتوغاز','غاز بيت','ادوات منزليه','صحون','طناجر','فرشه','مخده','بطانيه','سجاد','موكيت','ستائر','دهان','بلاط','سباك','كهربائي','تصليح البيت','ورق جدران','مصروف البيت'] },
  { id: 'family',      ar: 'عائلة وأولاد',  en: 'Family & Kids',  color: '#22C55E',
    keys: ['ولادي','اولادي','الاولاد','الاطفال','طفلة','طفل','مصروف الولاد','العيله','اهلي','امي','ابي','مرتي','زوجتي','جيب الولد','لعبة','العاب','حفاضات','حليب اطفال','بيبرون'] },
  { id: 'gifts',       ar: 'هدايا وعزايم',  en: 'Gifts & Events', color: '#A855F7', priority: 1.15,
    keys: ['هديه','هدايا','عيدية','عيديه','مباركه','عرس','زفاف','خطوبه','عزومه','وليمه','تبرع','صدقه','زكاة','مساعدت','واجب','نقوط'] },
  { id: 'personal',    ar: 'شخصي',          en: 'Personal',       color: '#0EA5E9',
    keys: ['حلاق','كوافير','صالون','عطر','كولونيا','مكياج','كريم','صابون','شامبو','مسواك','معجون اسنان','رياضه','نادي','جيم','سينما','نزهه','طلعه','سفره سياحيه','مسبح','ارجيله','دخان','سجاير','تنباك','قهوة'] },
  { id: 'business',    ar: 'شغل ومصلحة',    en: 'Business',       color: '#64748B',
    keys: ['بضاعه','محل','دكان','تجاره','موظفين','عماله','راتب موظف','مواد','عده','ادوات شغل','ورشة','مستودع','زبون','شغل','مشروع','رأس مال','راس مال'] },
  { id: 'debt',        ar: 'ديون وأقساط',   en: 'Debts & Loans',  color: '#DC2626',
    keys: ['دين','ديون','قرض','سلفه','سلفة','اقساط','قسط','دفعة','ربا','جمعية مال','مديون'] },
  { id: 'other',       ar: 'غير مصنف',      en: 'Other',          color: '#9CA3AF', keys: [] },
];

export function detectCategory(text) {
  const norm = normalizeAr(text);
  if (!norm) return { id: 'other', confidence: 0 };
  let best = { id: 'other', confidence: 0.15, matched: '' };
  for (const c of CATEGORIES) {
    for (const k of c.keys) {
      const nk = normalizeAr(k);
      if (nk && nk.length > 1 && norm.includes(nk)) {
        // طول الكلمة المطابقة + أولوية الفئة (هدايا/ديون أهم من "عائلة")
        const conf = Math.min(0.97, (0.55 + nk.length / 40) * (c.priority || 1));
        if (conf > best.confidence) best = { id: c.id, confidence: conf, matched: k };
      }
    }
  }
  return best;
}

/* ------------------------------- النوع ------------------------------------ */
export function detectType(text) {
  const n = normalizeAr(text);
  const incomeKeys = ['قبضت','دخل','دخلني','جاني','راتب','معاش','ارباح','ربح','بيع','بعت','هدية جاتني','مكافاه','علاوة','عمولة','حوالة','استلمت','وارد','مصروف جاني','سلفه اخذتها'];
  const debtKeys = ['مديون','علي دين','لغيري دين','استلفت','اخدت سلفة','دين على','قسط مترتب','مستحق'];
  if (hasAny(n, debtKeys)) return { type: 'debt', confidence: 0.8 };
  if (hasAny(n, incomeKeys)) return { type: 'income', confidence: 0.85 };
  return { type: 'expense', confidence: 0.9 };
}

/* ------------------------------- التاريخ ---------------------------------- */
const AR_DAYS = ['الاحد','الاثنين','الثلثاء','الثلاثاء','الاربعاء','الخميس','الجمعه','السبت'];
const AR_MONTHS = ['كانون الثاني','شباط','اذار','نيسان','ايار','حزيران','تموز','اب','ايلول','تشرين الاول','تشرين الثاني','كانون الاول',
                   'يناير','فبراير','مارس','ابريل','مايو','يونيو','يوليو','اغسطس','سبتمبر','اكتوبر','نوفمبر','ديسمبر'];

/** تاريخ مدني yyyy-mm-dd بدون أي تحويل UTC (تجنب خطأ اليوم الناقص) */
export function civilDate(d = new Date()) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
export function addDays(civil, n) {
  const [y, m, d] = civil.split('-').map(Number);
  const dt = new Date(y, m - 1, d + n);   // محلي بالكامل
  return civilDate(dt);
}
export function parseCivil(civil) {
  const [y, m, d] = String(civil).split('-').map(Number);
  return new Date(y, (m || 1) - 1, d || 1);
}

export function detectDate(text, today = civilDate()) {
  const n = normalizeAr(text);
  if (!n) return null;
  if (/(^|\s)النهارده|النهاردا|نهارده/.test(n)) return { date: today, label: 'النهارده' };
  if (/(^|\s)اليوم/.test(n)) return { date: today, label: 'اليوم' };
  // الأهم أولاً: "أول مبارح" قبل "مبارح" حتى لا يرجع نفس اليوم
  if (/(^|\s)اول مبارح|اول امس|البارحه الاولانيه/.test(n)) return { date: addDays(today, -2), label: 'أول مبارح' };
  if (/(^|\s)مبارح|امس|البارحه/.test(n)) return { date: addDays(today, -1), label: 'مبارح' };
  if (/(^|\s)بكرا|غدا/.test(n)) return { date: addDays(today, 1), label: 'بكرا' };

  // "السبت" / "يوم السبت" → أقرب يوم ماضٍ بنفس الاسم (ماشي مع التقويم السوري)
  for (let i = 0; i < AR_DAYS.length; i++) {
    const key = normalizeAr(AR_DAYS[i]);
    if (key && new RegExp(`(^|\\s)${key}(\\s|$)`).test(n)) {
      const todayIdx = parseCivil(today).getDay();       // 0=الأحد … 6=السبت
      const diff = ((todayIdx - i) + 7) % 7 || 7;        // آخر occurrence (لا يرجع اليوم نفسه)
      return { date: addDays(today, -diff), label: AR_DAYS[i] };
    }
  }

  // "٥ الشهر" / "5 الشهر" / "في 12"
  const mDay = n.match(/(\d{1,2})\s*(الشهر|هذا الشهر|من الشهر|منو)/);
  if (mDay) {
    const day = parseInt(mDay[1], 10);
    const [y, mo] = today.split('-').map(Number);
    if (day >= 1 && day <= 31) {
      let candidate = `${y}-${String(mo).padStart(2,'0')}-${String(day).padStart(2,'0')}`;
      if (candidate > today) {
        const py = mo === 1 ? y - 1 : y;
        const pm = mo === 1 ? 12 : mo - 1;
        candidate = `${py}-${String(pm).padStart(2,'0')}-${String(day).padStart(2,'0')}`;
      }
      return { date: candidate, label: `${day} الشهر` };
    }
  }

  // "12/3" أو "12-3"
  const mSlash = n.match(/(\d{1,2})[\/\-](\d{1,2})(?:[\/\-](\d{2,4}))?/);
  if (mSlash) {
    const y0 = today.split('-')[0];
    const dd = mSlash[1].padStart(2, '0');
    const mm = mSlash[2].padStart(2, '0');
    const yy = mSlash[3] ? (mSlash[3].length === 2 ? y0.slice(0,2) + mSlash[3] : mSlash[3]) : y0;
    const cand = `${yy}-${mm}-${dd}`;
    if (/^\d{4}-\d{2}-\d{2}$/.test(cand)) return { date: cand, label: cand };
  }

  // "الشهر الماضي" / "هاد الشهر"
  if (/الشهر الماضي|الشهر اللي طلع/.test(n)) {
    const d = parseCivil(today);
    const prev = new Date(d.getFullYear(), d.getMonth() - 1, 1);
    return { date: civilDate(prev), label: 'الشهر الماضي' };
  }
  for (let i = 0; i < AR_MONTHS.length; i++) {
    const key = normalizeAr(AR_MONTHS[i]);
    if (key && n.includes(key)) {
      const idx = i % 12;
      const y = today.split('-')[0];
      return { date: `${y}-${String(idx + 1).padStart(2,'0')}-01`, label: AR_MONTHS[i] };
    }
  }
  return null;
}

/* ------------------------- المكان / التاجر ------------------------------- */
export const SYRIAN_PLACES = ['دمشق','حلب','حمص','حماة','حماه','اللاذقية','اللاذقيه','طرطوس','دير الزور','ديرالزور','الرقة','الحسكة','درعا','السويداء','القنيطرة','ادلب','حرمون','قدسيا','جرمانا','صحنايا','دوما','حرستا','المعضمية','داريا','الزبداني','معضمية','ركن الدين','المزة','المزه','كفرسوسة','باب شرقي','الحميدية','الحريقة','الميدان','الشعلان','المالكي','ابو رمانة','برزة','القدم','سبينة','ببيلا','يلدا','التل','قاسيون','مصياف','سلمية','بانياس','جبلة','صافيتا','الدريكيش','عفرين','منبج','الباب','اعزاز','تل ابيض','القامشلي','عامودا','رأس العين','المالكية','نوى','ازرع','الصنمين','شهبا','صلخد'];

export function detectMerchant(text) {
  const n = normalizeAr(text);
  if (!n) return null;
  const patterns = [
    /(?:روح|راحت|طلعت|نزلت|سافرت|وديت|اخدت)\s+(?:على|الى|إلى|ل)\s+([^\s،,.;!؟?]+)/,
    /(?:من|ب|في|عند)\s+(?:سوق\s+)?([^\s،,.;!؟?]+)\s+(?:ب|في|من)\s*$/,
    /(?:عند|من)\s+(سوبرماركت|بقالة|صيدلية|مطعم|كافيه|فرن|ملحمة|خضرجي|محل)\s*([^\s،,.;!؟?]*)/,
    /(?:فاتورة|اجار|قسط)\s+([^\s،,.;!؟?]+)/,
  ];
  for (const p of patterns) {
    const m = n.match(p);
    if (m && m[1]) {
      const name = (m[1] + (m[2] ? ' ' + m[2] : '')).trim();
      const stop = ['الدكان','البيت','الشغل','السيارة','السياره','المدينة','المدينه','هون','هناك','فوق','تحت'];
      if (name.length > 1 && !stop.includes(name)) {
        return { name, isPlace: SYRIAN_PLACES.some(c => normalizeAr(c) === name || name.includes(normalizeAr(c))) };
      }
    }
  }
  for (const c of SYRIAN_PLACES) {
    const k = normalizeAr(c);
    if (n.includes(k)) return { name: c, isPlace: true };
  }
  return null;
}

/* ------------------------------ التحليل الكامل ---------------------------- */
export function parseUtterance(text, today = civilDate()) {
  const raw = String(text || '').trim();
  if (!raw) return null;
  const amount = extractAmount(raw);
  const category = detectCategory(raw);
  const type = detectType(raw);
  const date = detectDate(raw, today);
  const merchant = detectMerchant(raw);

  const needsConfirmation = !amount || amount.confidence < 0.5;

  let confidence = amount ? amount.confidence * 0.6 : 0.05;
  confidence += category.confidence * 0.2;
  confidence += (date ? 0.1 : 0.02);
  confidence += (merchant ? 0.1 : 0.02);
  confidence = Math.min(0.99, confidence);

  return {
    raw,
    amount,                                   // قد يكون null
    amountNew: amount ? amount.value : null,  // بالليرة السورية الجديدة
    category: category.id,
    categoryConfidence: category.confidence,
    type: type.type,
    date: date ? date.date : today,
    dateLabel: date ? date.label : null,
    merchant: merchant ? merchant.name : null,
    needsConfirmation,
    confidence,
  };
}

/* ------------------------------ الملخّص الكلامي --------------------------- */
export function categoryById(id) {
  return CATEGORIES.find(c => c.id === id) || CATEGORIES[CATEGORIES.length - 1];
}

/** رقم عربي مجمّل بفواصل الآلاف — بدون أي كسور عشرية لليرة الجديدة */
export function formatMoney(value, lang = 'ar', unit = 'new') {
  const v = Math.round(Number(value) || 0);
  const grouped = Math.abs(v).toString().replace(/\B(?=(\d{3})+(?!\d))/g, lang === 'ar' ? '٬' : ',');
  const sign = v < 0 ? '-' : '';
  return lang === 'ar'
    ? `${sign}${grouped} ${CURRENCY_NEW.arSymbol}`
    : `${sign}${grouped} ${CURRENCY_NEW.enSymbol}`;
}

/** تحويل ليرة قديمة → جديدة */
export function legacyToNew(v) { return Math.round((Number(v) || 0) / CURRENCY_NEW.legacyDivisor); }
export function newToLegacy(v) { return Math.round((Number(v) || 0) * CURRENCY_NEW.legacyDivisor); }

export function summarizeAr(p) {
  if (!p) return '';
  const cat = categoryById(p.category);
  const parts = [];
  parts.push(`${p.type === 'income' ? 'دخلك' : 'صرفت'} ${formatMoney(p.amountNew, 'ar')} على ${cat.ar}`);
  if (p.merchant) parts.push(`في ${p.merchant}`);
  if (p.dateLabel) parts.push(`(${p.dateLabel})`);
  return parts.join(' ');
}

export function summarizeEn(p) {
  if (!p) return '';
  const cat = categoryById(p.category);
  const parts = [];
  parts.push(`${p.type === 'income' ? 'Income' : 'Spent'} ${formatMoney(p.amountNew, 'en')} on ${cat.en}`);
  if (p.merchant) parts.push(`at ${p.merchant}`);
  return parts.join(' ');
}
