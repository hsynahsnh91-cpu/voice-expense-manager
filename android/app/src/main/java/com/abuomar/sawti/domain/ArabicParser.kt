package com.abuomar.sawti.domain

import com.abuomar.sawti.core.CivilDate
import com.abuomar.sawti.core.Currency
import com.abuomar.sawti.data.AmountUnit
import com.abuomar.sawti.data.Category
import com.abuomar.sawti.data.ParsedExpense
import com.abuomar.sawti.data.TxType

/**
 * محرك فهم الكلام العامّي السوري/الشامي.
 *
 * يحوّل جملة منطوقة مثل:
 *   «كلفني 5000 ليرة سوري روح على دمشق»
 *   «صرفت خمسة آلاف على الخضرة من السوق مبارح»
 *   «دفعت فاتورة الكهرباء مية ألف»
 * إلى [ParsedExpense] منظّم: مبلغ بالليرة السورية الجديدة، فئة، تاريخ، مكان، نوع.
 *
 * يدعم:
 *  • الأرقام اللاتينية والعربية-الهندية (٠١٢٣٤٥٦٧٨٩) والفارسية (۰۱۲۳)
 *  • الأرقام بالكلمات بما فيها العامية: مية، تلت ميه، خمستاشر، طناش، ألفين، ونص
 *  • المزيج: «٣٥ ألف»، «5 الاف»، «مليون ونص»
 *  • الليرة القديمة (÷١٠٠) مقابل الجديدة
 *  • التواريخ النسبية: اليوم، مبارح، أول مبارح، بكرا، السبت، ٥ الشهر، الشهر الماضي
 *  • الأماكن السورية: دمشق، حلب، حمص… و«روح على X»
 *
 * لا يوجد أي نص وهمي: إن لم يُفهم المبلغ تُرجع amountNew = null لتأكيد يدوي.
 */
object ArabicParser {

    /* ================================ التطبيع ============================== */

    private val DIACRITICS = Regex("[\u064B-\u065F\u0670\u0640\u06D6-\u06ED]")

    /** تطبيع النص العربي: إزالة التشكيل، توحيد الهمزات والأرقام */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        var s = DIACRITICS.replace(input, " ")
        s = Currency.normalizeDigits(s)
        val map = mapOf(
            'إ' to 'ا', 'أ' to 'ا', 'آ' to 'ا', 'ٱ' to 'ا',
            'ى' to 'ي', 'ؤ' to 'و', 'ئ' to 'ي', 'ة' to 'ه',
            'گ' to 'ك', 'پ' to 'ب', 'چ' to 'ج', 'ژ' to 'ز',
        )
        val sb = StringBuilder(s.length)
        for (ch in s) sb.append(map[ch] ?: ch)
        s = sb.toString()
        // نبقي الحروف والأرقام والمسافات وعلامات الترقيم المفيدة فقط
        s = s.replace(Regex("[^\\p{L}\\p{N}\\s.,\\-+/]"), " ")
        return s.replace(Regex("\\s+"), " ").trim()
    }

    private fun tokens(text: String?): List<String> = normalize(text).split(" ").filter { it.isNotEmpty() }

    private fun containsAny(text: String, words: List<String>): Boolean = words.any { text.contains(it) }

    /* ============================== الأرقام بالكلمات ======================= */

    private val ONES = mapOf(
        "واحد" to 1, "واحده" to 1, "احد" to 1,
        "اثنين" to 2, "اتنين" to 2, "تنين" to 2, "جوج" to 2, "زوج" to 2,
        "ثلاثه" to 3, "تلاته" to 3, "ثلاته" to 3, "ثلاث" to 3,
        "اربعه" to 4, "اربع" to 4, "اريعه" to 4,
        "خمسه" to 5, "خمس" to 5,
        "سته" to 6, "ست" to 6,
        "سبعه" to 7, "سبع" to 7,
        "ثمانيه" to 8, "تمانيه" to 8, "تمان" to 8, "ثماني" to 8, "تماني" to 8, "تمنيه" to 8,
        "تسعه" to 9, "تسع" to 9,
        "عشره" to 10, "عشر" to 10,
    )

    /** المراهقات العامية والفصحى */
    private val TEENS = mapOf(
        "احداش" to 11, "احدي عشر" to 11, "احدياشر" to 11, "احد عشر" to 11,
        "طناش" to 12, "اثنا عشر" to 12, "اثني عشر" to 12, "اتناشر" to 12,
        "ثلطاش" to 13, "ثلاثه عشر" to 13, "تلتاشر" to 13, "ثلاثه عشره" to 13,
        "اربطاش" to 14, "اربعه عشر" to 14, "اربعطاش" to 14,
        "خمسطاش" to 15, "خمسه عشر" to 15, "خمستاشر" to 15,
        "سطاش" to 16, "سته عشر" to 16, "ستطاشر" to 16,
        "سبعطاش" to 17, "سبعه عشر" to 17, "سبعتاشر" to 17,
        "تمنطاش" to 18, "ثمانيه عشر" to 18, "تمنتاشر" to 18,
        "تسعطاش" to 19, "تسعه عشر" to 19, "تسعتاشر" to 19,
    )

    private val TENS = mapOf(
        "عشره" to 10, "عشرين" to 20,
        "تلاتين" to 30, "ثلاثين" to 30, "ثلاتين" to 30,
        "اربعين" to 40, "اريعين" to 40,
        "خمسين" to 50,
        "ستين" to 60, "سيتين" to 60,
        "سبعين" to 70,
        "تمانين" to 80, "ثمانين" to 80,
        "تسعين" to 90,
    )

    /** المئات المركبة تُفحص قبل المفردة */
    private val HUNDREDS_COMPOUND = mapOf(
        "تلت ميه" to 300, "ثلاث ميه" to 300, "ثلاثميه" to 300, "تلتميه" to 300, "ثلاثمئه" to 300,
        "ربع ميه" to 400, "اربع ميه" to 400, "اربعمايه" to 400, "اربع مايه" to 400, "اربع مئه" to 400,
        "خمس ميه" to 500, "خمسميه" to 500, "خمسمئه" to 500, "خمسمية" to 500,
        "ست ميه" to 600, "ستميه" to 600, "ستمئه" to 600, "ستمية" to 600,
        "سبع ميه" to 700, "سبعميه" to 700, "سبعمئه" to 700, "سبعمية" to 700,
        "ثماني ميه" to 800, "تمنميه" to 800, "ثمانمئه" to 800, "تمنمية" to 800,
        "تسع ميه" to 900, "تسعميه" to 900, "تسعمئه" to 900, "تسعمية" to 900,
    )

    private val HUNDREDS = mapOf(
        "ميه" to 100, "مئه" to 100, "مائه" to 100,
        "ميته" to 200, "مئتين" to 200, "ميتين" to 200, "مياتين" to 200,
    )

    private val MULT = mapOf(
        "الف" to 1000L, "الاف" to 1000L, "الفا" to 1000L, "الفين" to 2000L,
        "مليون" to 1_000_000L, "ملايين" to 1_000_000L, "مليونين" to 2_000_000L,
        "مليار" to 1_000_000_000L, "مليارات" to 1_000_000_000L, "مليارين" to 2_000_000_000L,
    )

    /** الوحدة الأساسية للمضاعف — تُستخدم مع «ونص»: ألف ونص=١٥٠٠، ألفين ونص=٢٥٠٠ */
    private val MULT_BASE = mapOf(
        1000L to 1000L, 2000L to 1000L,
        1_000_000L to 1_000_000L, 2_000_000L to 1_000_000L,
        1_000_000_000L to 1_000_000_000L, 2_000_000_000L to 1_000_000_000L,
    )

    private val HALF = mapOf("ونص" to 0.5, "ونصف" to 0.5, "نص" to 0.5, "نصف" to 0.5)

    private data class NumberPhrase(val value: Long, val consumed: Int)

    /** يقرأ عبارة رقمية بالكلمات ابتداءً من الموضع i */
    private fun readNumberPhrase(t: List<String>, start: Int): NumberPhrase? {
        var total = 0L
        var current = 0L
        var consumed = 0
        var found = false

        while (start + consumed < t.size) {
            val tok = t[start + consumed]
            if (tok == "و" || tok == "ثم" || tok == "زائد") { consumed++; continue }

            // مئات مركبة (تلت ميه / خمس ميه)
            val two = if (start + consumed + 1 < t.size) t.subList(start + consumed, start + consumed + 2).joinToString(" ") else null
            if (two != null && HUNDREDS_COMPOUND.containsKey(two)) {
                current += HUNDREDS_COMPOUND.getValue(two); found = true; consumed += 2; continue
            }
            // مراهقة مركبة (احدي عشر)
            if (two != null && TEENS.containsKey(two)) {
                current += TEENS.getValue(two); found = true; consumed += 2; continue
            }
            // وحدة + "عشره" (خمسه عشره)
            val next = t.getOrNull(start + consumed + 1)
            val oneVal = ONES[tok]
            if (oneVal != null && oneVal <= 9 && (next == "عشره" || next == "عشر")) {
                current += oneVal + 10; found = true; consumed += 2; continue
            }
            // مفردات
            HUNDREDS_COMPOUND[tok]?.let { current += it; found = true; consumed++; continue }
            HUNDREDS[tok]?.let { current += it; found = true; consumed++; continue }
            TEENS[tok]?.let { current += it; found = true; consumed++; continue }
            TENS[tok]?.let { current += it; found = true; consumed++; continue }
            ONES[tok]?.let { current += it; found = true; consumed++; continue }

            // مضاعفات
            val m = MULT[tok]
            if (m != null) {
                val block = (if (current == 0L) 1L else current) * m
                total += block
                current = 0L; found = true; consumed++
                val halfTok = t.getOrNull(start + consumed)
                if (halfTok != null && HALF.containsKey(halfTok)) {
                    total += ((MULT_BASE[m] ?: m) * HALF.getValue(halfTok)).toLong()
                    consumed++
                }
                continue
            }
            break
        }
        total += current
        return if (found && consumed > 0) NumberPhrase(total, consumed) else null
    }

    /* ============================== استخراج المبلغ ========================= */

    private val AMOUNT_TRIGGERS = listOf(
        "ب", "بمبلغ", "مبلغ", "قيمته", "قيمتو", "ثمنه", "ثمن", "سعره", "سعر",
        "تكلفه", "كلفه", "كلفني", "صار", "صارو", "اجمالي", "مجموع", "قده", "قد",
    )
    private val CURRENCY_HINTS = listOf(
        "ليره", "ليرات", "ليره سوريه", "سوري", "سوريه", "ل س", "مصاري", "فلوس",
    )
    private val LEGACY_HINTS = listOf("قديمه", "قديم")
    private val FOREIGN_CURRENCY = Regex("دولار|يورو|ليره تركيه|تركي|دولارات|usd|eur|دينار|ريال")

    /** أرقام لا تدل على مصروف (تواريخ/أوقات/كميات) إلا إن سبقها تلميح عملة */
    private val NON_AMOUNT_NEXT = setOf(
        "الشهر", "يوم", "ايام", "ساعه", "ساعات", "دقيقه", "دقايق", "ثانيه",
        "شهر", "سنه", "سنوات", "مره", "مرات", "نفر", "اشخاص", "كيلو", "غرام",
        "متر", "سم", "درجه", "بيت", "طابق", "باص", "خط",
    )

    private data class AmountCandidate(
        val value: Long,
        val kind: String,
        val score: Int,
        val span: String,
        val spanStartToken: Int,
    )

    private val DIGITS_ONLY = Regex("^\\d{1,15}$")
    private val SLASH_DATE = Regex("^\\d{1,2}[/-]\\d{1,2}")

    /** يستخرج أفضل مبلغ من الجملة — القيمة النهائية دائماً بالليرة الجديدة */
    fun extractAmount(text: String?): AmountInfo? {
        val norm = normalize(text)
        if (norm.isEmpty()) return null
        val t = norm.split(" ")
        val candidates = ArrayList<AmountCandidate>(8)

        fun push(value: Long, kind: String, at: Int, score: Int, span: String) {
            if (value > 0) candidates += AmountCandidate(value, kind, score, span, at)
        }

        var i = 0
        while (i < t.size) {
            val tok = t[i]
            val prev3 = t.subList(maxOf(0, i - 3), i).joinToString(" ")
            val next3 = t.subList(i + 1, minOf(t.size, i + 4)).joinToString(" ")
            val currencyNear = containsAny("$prev3 $next3", CURRENCY_HINTS) || containsAny(prev3, AMOUNT_TRIGGERS)
            val plain = tok.replace(",", "").replace(".", "")

            /* --- أ) رقم صريح (لاتيني أو عربي بعد التطبيع) --- */
            if (DIGITS_ONLY.matches(plain)) {
                val v = plain.toLongOrNull() ?: 0L
                val nextTok = t.getOrNull(i + 1) ?: ""
                val looksLikeDateNum = NON_AMOUNT_NEXT.contains(nextTok) || SLASH_DATE.matches(tok)
                if (!looksLikeDateNum && v > 0) {
                    push(v, "digits", i, if (currencyNear) 96 else 92, tok)
                }
                /* --- ب) رقم + مضاعف بالكلمة: «٣٥ ألف» --- */
                val multTok = t.getOrNull(i + 1)
                var m = multTok?.let { MULT[it] }
                if (m == null && multTok == "الفين") m = 2000L
                if (m == null && multTok == "مليونين") m = 2_000_000L
                if (m != null && v > 0) {
                    val unitValue = MULT_BASE[m] ?: m
                    var value = v * unitValue
                    val halfTok = t.getOrNull(i + 2)
                    if (halfTok != null && HALF.containsKey(halfTok)) {
                        value += (unitValue * HALF.getValue(halfTok)).toLong()
                    }
                    push(value, "digits+mult", i, 97, "$tok $multTok")
                    i += 2
                    continue
                }
                i++
                continue
            }

            /* --- ج) مضاعف مفرد بلا رقم قبله: «ألف»، «ألفين»، «مليون» --- */
            var mSolo = MULT[tok]
            if (mSolo == null && tok == "الفين") mSolo = 2000L
            if (mSolo == null && tok == "مليونين") mSolo = 2_000_000L
            if (mSolo != null) {
                val prevTok = t.getOrNull(i - 1) ?: ""
                val prevIsNumber = DIGITS_ONLY.matches(prevTok.replace(",", "")) ||
                    ONES.containsKey(prevTok) || TENS.containsKey(prevTok) || TEENS.containsKey(prevTok)
                if (!prevIsNumber) {
                    var value = mSolo
                    val halfTok = t.getOrNull(i + 1)
                    if (halfTok != null && HALF.containsKey(halfTok)) {
                        value += ((MULT_BASE[mSolo] ?: mSolo) * HALF.getValue(halfTok)).toLong()
                    }
                    push(value, "mult", i, 78, tok)
                }
            }

            /* --- د) عبارة رقمية بالكلمات: «خمسة آلاف»، «تلت ميه»، «عشرين» --- */
            val phrase = readNumberPhrase(t, i)
            if (phrase != null && phrase.value >= 2) {
                val span = t.subList(i, minOf(t.size, i + phrase.consumed)).joinToString(" ")
                val nextTok = t.getOrNull(i + phrase.consumed) ?: ""
                val looksLikeDateNum = NON_AMOUNT_NEXT.contains(nextTok) && !currencyNear
                if (!looksLikeDateNum) {
                    var score = 55 + minOf(phrase.consumed * 7, 21)
                    if (containsAny("$span $next3", CURRENCY_HINTS)) score += 25
                    if (containsAny(prev3, AMOUNT_TRIGGERS)) score += 12
                    if (phrase.value >= 1000) score += 8
                    push(phrase.value, "words", i, score, span)
                }
                i += phrase.consumed
                continue
            }
            i++
        }

        if (candidates.isEmpty()) return null

        val best = candidates.sortedWith(compareByDescending<AmountCandidate> { it.score }.thenByDescending { it.value }).first()

        /* --- تحديد الوحدة: جديدة / قديمة / أجنبية --- */
        val spanIndex = norm.indexOf(best.span.split(" ").firstOrNull() ?: best.span)
        val from = maxOf(0, spanIndex - 45)
        val to = minOf(norm.length, (if (spanIndex < 0) 0 else spanIndex) + best.span.length + 55)
        val near = norm.substring(from, to)

        val saidLegacy = containsAny(near, LEGACY_HINTS) && containsAny(near, listOf("ليره", "سوري"))
        val unit: AmountUnit
        val valueNew: Long
        when {
            FOREIGN_CURRENCY.containsMatchIn(near) -> { unit = AmountUnit.OTHER; valueNew = best.value }
            saidLegacy -> { unit = AmountUnit.LEGACY; valueNew = Currency.legacyToNew(best.value) }
            else -> { unit = AmountUnit.NEW; valueNew = best.value }
        }

        return AmountInfo(
            rawValue = best.value,
            valueNew = valueNew,
            unit = unit,
            matchedText = best.span,
            source = best.kind,
            confidence = (minOf(99, best.score) / 100f),
        )
    }

    data class AmountInfo(
        val rawValue: Long,
        val valueNew: Long,
        val unit: AmountUnit,
        val matchedText: String,
        val source: String,
        val confidence: Float,
    )

    /* ================================ الفئات =============================== */

    private val CATEGORY_KEYS: Map<Category, List<String>> = mapOf(
        Category.FOOD to listOf(
            "اكل", "شرب", "طعام", "فطور", "غدا", "غداء", "عشا", "عشاء", "مطعم", "كافيه",
            "قهوه", "شاي", "متايه", "عصير", "فواكه", "خضره", "خضار", "لحمة", "لحم", "فروج",
            "دجاج", "سمك", "خبز", "سندويش", "سندويشه", "برغر", "بيتزا", "شاورما", "فلافل",
            "مشاوي", "حلويات", "حلوي", "كنافه", "بقلاوه", "سكاكر", "مكسرات", "رز", "برغل",
            "سكر", "زيت", "سمن", "حليب", "لبنة", "لبني", "جبنة", "جبنة", "بيض", "مونة",
            "مونه", "تموين", "سوبرماركت", "بقاله", "دكان", "فرن", "معجنات", "كرواسان",
            "بوظه", "مياه", "بيبسي", "كولا", "طبخ", "وليمه", "عزومه",
        ),
        Category.TRANSPORT to listOf(
            "مواصلات", "توصيله", "سرفيس", "ميكرو", "ميكروباص", "تكسي", "تاكسي", "سائق",
            "بنزين", "مازوت", "ديزل", "محروقات", "كاز", "باص", "بولمان", "قطار", "طيارة",
            "طيران", "سفر", "مطار", "موقف", "باركينغ", "غسيل سيارة", "ميكانيكي",
            "دولاب", "بطارية سيارة", "سوزوكي", "اجرة", "اجره", "ركوب",
            "روح علي", "راحت علي", "طلعت علي", "نزلت علي", "رحت علي",
        ),
        Category.BILLS to listOf(
            "فاتوره", "فواتير", "كهربا", "كهرباء", "مي", "ماء", "انترنت", "نت",
            "واي فاي", "موبايل", "رصيد", "شحن", "خط", "تلفون", "هاتف", "غاز", "اشتراك",
            "مقنن", "مولده", "مولدة", "امبير", "رسوم", "ضريبه", "بلديه",
        ),
        Category.HEALTH to listOf(
            "دوا", "دواء", "صيدليه", "صيدلية", "طبيب", "دكتور", "مستشفى", "عياده",
            "تحليل", "اشعه", "مبرره", "ابره", "حقنه", "عملية", "معاينه", "ادويه",
            "بانادول", "سيروم", "لقاح", "تطعيم", "اسنان", "نظارات", "علاج", "طبابة",
        ),
        Category.EDUCATION to listOf(
            "مدرسه", "جامعة", "معهد", "درس", "دروس", "خصوصي", "دوره", "قرطاسيه",
            "كتب", "دفاتر", "اقلام", "حقيبه مدرسيه", "اقساط", "قسط مدرسي", "روضه", "طلاب",
        ),
        Category.CLOTHING to listOf(
            "لبس", "ملابس", "ثياب", "قميص", "بنطلون", "جاكيت", "كنزة", "كنزه", "فستان",
            "جزدان", "حذاء", "كندرة", "جزمة", "جزمه", "شرابات", "جرابات", "شال",
            "اشارب", "حجاب", "مانطو", "بالتو", "تي شيرت", "تيشرت", "طقم", "بدله",
            "خياط", "مكوي", "كوي",
        ),
        Category.HOME to listOf(
            "ايجار", "اجار البيت", "مفروشات", "اثاث", "غساله", "براد", "ثلاجه",
            "بوتوغاز", "ادوات منزليه", "صحون", "طناجر", "فرشه", "مخده", "بطانيه",
            "سجاد", "موكيت", "ستائر", "دهان", "بلاط", "سباك", "كهربائي",
            "تصليح البيت", "ورق جدران",
        ),
        Category.FAMILY to listOf(
            "ولادي", "اولادي", "الاولاد", "الاطفال", "طفلة", "طفل", "مصروف الولاد",
            "العيله", "اهلي", "امي", "ابي", "مرتي", "زوجتي", "جيب الولد", "لعبة",
            "العاب", "حفاضات", "حليب اطفال", "بيبرون",
        ),
        // أولوية أعلى: كلمة «هدية» أقوى من «مرتي» في تحديد الفئة
        Category.GIFTS to listOf(
            "هديه", "هدايا", "عيدية", "عيديه", "مباركه", "عرس", "زفاف", "خطوبه",
            "عزومه", "وليمه", "تبرع", "صدقه", "زكاة", "مساعدت", "واجب", "نقوط",
        ),
        Category.PERSONAL to listOf(
            "حلاق", "كوافير", "صالون", "عطر", "كولونيا", "مكياج", "كريم", "صابون",
            "شامبو", "مسواك", "معجون اسنان", "رياضه", "نادي", "جيم", "سينما",
            "نزهه", "طلعه", "سفره سياحيه", "مسبح", "ارجيله", "دخان", "سجاير", "تنباك",
        ),
        Category.BUSINESS to listOf(
            "بضاعه", "محل", "تجاره", "موظفين", "عماله", "راتب موظف", "مواد",
            "عده", "ادوات شغل", "ورشة", "مستودع", "زبون", "مشروع", "راس مال",
        ),
        Category.DEBT to listOf(
            "دين", "ديون", "قرض", "سلفه", "سلفة", "اقساط", "قسط", "دفعة", "ربا",
            "جمعية مال", "مديون",
        ),
    )

    private val CATEGORY_PRIORITY: Map<Category, Float> = mapOf(Category.GIFTS to 1.15f)

    private val normalizedCategoryKeys: List<Triple<Category, String, Int>> by lazy {
        CATEGORY_KEYS.flatMap { (cat, keys) ->
            keys.mapNotNull { k ->
                val nk = normalize(k)
                if (nk.length > 1) Triple(cat, nk, nk.length) else null
            }
        }
    }

    fun detectCategory(text: String?): Pair<Category, Float> {
        val norm = normalize(text)
        if (norm.isEmpty()) return Category.OTHER to 0f
        var bestCat = Category.OTHER
        var bestConf = 0.15f
        for ((cat, key, len) in normalizedCategoryKeys) {
            if (norm.contains(key)) {
                val conf = minOf(0.97f, (0.55f + len / 40f) * (CATEGORY_PRIORITY[cat] ?: 1f))
                if (conf > bestConf) { bestConf = conf; bestCat = cat }
            }
        }
        return bestCat to bestConf
    }

    /* ================================ النوع ================================ */

    private val INCOME_KEYS = listOf(
        "قبضت", "دخل", "دخلني", "جاني", "راتب", "معاش", "ارباح", "ربح", "بيع", "بعت",
        "مكافاه", "علاوة", "عمولة", "عموله", "حوالة", "حواله", "استلمت", "وارد",
    )
    private val DEBT_KEYS = listOf(
        "مديون", "علي دين", "استلفت", "اخدت سلفة", "اخدت سلفه", "دين على",
        "قسط مترتب", "مستحق",
    )

    fun detectType(text: String?): TxType {
        val n = normalize(text)
        if (n.isEmpty()) return TxType.EXPENSE
        if (containsAny(n, DEBT_KEYS)) return TxType.DEBT
        if (containsAny(n, INCOME_KEYS)) return TxType.INCOME
        return TxType.EXPENSE
    }

    /* =============================== التاريخ =============================== */

    private val AR_DAYS = listOf("الاحد", "الاثنين", "الثلثاء", "الثلاثاء", "الاربعاء", "الخميس", "الجمعه", "السبت")

    fun detectDate(text: String?, today: String = CivilDate.today()): Pair<String, String?>? {
        val n = normalize(text)
        if (n.isEmpty()) return null

        fun has(w: String) = Regex("(^|\\s)$w(\\s|$)").containsMatchIn(n) || n.contains(w)

        if (has("النهارده") || has("نهارده")) return today to "النهارده"
        if (has("اليوم")) return today to "اليوم"
        // الأهم أولاً حتى لا يرجع «أول مبارح» نفس يوم «مبارح»
        if (has("اول مبارح") || has("اول امس")) return CivilDate.plusDays(today, -2) to "أول مبارح"
        if (has("مبارح") || has("امس") || has("البارحه")) return CivilDate.plusDays(today, -1) to "مبارح"
        if (has("بكرا") || has("غدا")) return CivilDate.plusDays(today, 1) to "بكرا"

        // اسم يوم الأسبوع → آخر occurrence ماضية
        for ((idx, day) in AR_DAYS.withIndex()) {
            val key = normalize(day)
            if (key.isNotEmpty() && Regex("(^|\\s)$key(\\s|$)").containsMatchIn(n)) {
                val todayIdx = CivilDate.weekday(today)          // 0=الأحد
                var diff = ((todayIdx - idx) % 7 + 7) % 7
                if (diff == 0) diff = 7
                return CivilDate.plusDays(today, -diff) to day
            }
        }

        // «٥ الشهر»
        val dayOfMonth = Regex("(\\d{1,2})\\s*(الشهر|هذا الشهر|من الشهر)").find(n)
        if (dayOfMonth != null) {
            val day = dayOfMonth.groupValues[1].toIntOrNull() ?: 0
            if (day in 1..31) {
                val p = CivilDate.parts(today)
                var candidate = CivilDate.of(p.year, p.month, day)
                if (candidate > today) {
                    val prev = CivilDate.plusMonths(today, -1)
                    val pp = CivilDate.parts(prev)
                    candidate = CivilDate.of(pp.year, pp.month, day)
                }
                return candidate to "$day الشهر"
            }
        }

        // «12/3» أو «12-3»
        val slash = Regex("(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?").find(n)
        if (slash != null) {
            val p = CivilDate.parts(today)
            val dd = slash.groupValues[1].padStart(2, '0')
            val mm = slash.groupValues[2].padStart(2, '0')
            val yy = slash.groupValues[3].let {
                when {
                    it.isEmpty() -> p.year.toString()
                    it.length == 2 -> p.year.toString().substring(0, 2) + it
                    else -> it
                }
            }
            val candidate = "$yy-$mm-$dd"
            if (CivilDate.isValid(candidate)) return candidate to candidate
        }

        if (has("الشهر الماضي")) {
            val prev = CivilDate.plusMonths(today, -1)
            return CivilDate.startOfMonth(prev) to "الشهر الماضي"
        }
        return null
    }

    /* ============================ المكان / التاجر ========================== */

    val SYRIAN_PLACES = listOf(
        "دمشق", "حلب", "حمص", "حماة", "حماه", "اللاذقية", "اللاذقيه", "طرطوس",
        "دير الزور", "ديرالزور", "الرقة", "الحسكة", "درعا", "السويداء",
        "القنيطرة", "ادلب", "قدسيا", "جرمانا", "صحنايا", "دوما", "حرستا",
        "المعضمية", "داريا", "الزبداني", "ركن الدين", "المزة", "المزه",
        "كفرسوسة", "باب شرقي", "الحميدية", "الحريقة", "الميدان", "الشعلان",
        "المالكي", "ابو رمانة", "برزة", "مصياف", "سلمية", "بانياس", "جبلة",
        "صافيتا", "الدريكيش", "عفرين", "منبج", "الباب", "اعزاز", "تل ابيض",
        "القامشلي", "عامودا", "راس العين", "المالكية", "نوى", "ازرع",
        "الصنمين", "شهبا", "صلخد",
    )

    private val PLACE_PATTERNS = listOf(
        Regex("(?:روح|راحت|طلعت|نزلت|سافرت|وديت|اخدت|رحت)\\s+(?:على|الي|إلى|ل)\\s+([^\\s،,.;!؟?]+)"),
        Regex("(?:عند|من)\\s+(سوبرماركت|بقالة|بقاله|صيدلية|صيدليه|مطعم|كافيه|فرن|ملحمة|ملحمه|خضرجي|محل)\\s*([^\\s،,.;!؟?]*)"),
        Regex("(?:فاتورة|فاتوره|اجار|ايجار|قسط)\\s+([^\\s،,.;!؟?]+)"),
    )
    private val PLACE_STOP = setOf("الدكان", "البيت", "الشغل", "السيارة", "السياره", "المدينة", "المدينه", "هون", "هناك", "فوق", "تحت")

    fun detectMerchant(text: String?): String? {
        val n = normalize(text)
        if (n.isEmpty()) return null
        for (p in PLACE_PATTERNS) {
            val m = p.find(n)
            if (m != null) {
                val name = buildString {
                    append(m.groupValues[1])
                    if (m.groupValues.size > 2 && m.groupValues[2].isNotBlank()) {
                        append(' '); append(m.groupValues[2])
                    }
                }.trim()
                if (name.length > 1 && !PLACE_STOP.contains(name)) return name
            }
        }
        for (place in SYRIAN_PLACES) {
            val key = normalize(place)
            if (key.isNotEmpty() && n.contains(key)) return place
        }
        return null
    }

    /* ============================ التحليل الكامل =========================== */

    fun parse(text: String?, today: String = CivilDate.today()): ParsedExpense? {
        val raw = text?.trim().orEmpty()
        if (raw.isEmpty()) return null

        val amount = extractAmount(raw)
        val (category, catConf) = detectCategory(raw)
        val type = detectType(raw)
        val dateHit = detectDate(raw, today)
        val merchant = detectMerchant(raw)

        var confidence = if (amount != null) amount.confidence * 0.6f else 0.05f
        confidence += catConf * 0.2f
        confidence += if (dateHit != null) 0.10f else 0.02f
        confidence += if (merchant != null) 0.10f else 0.02f
        confidence = minOf(0.99f, confidence)

        return ParsedExpense(
            raw = raw,
            amountNew = amount?.valueNew,
            amountRaw = amount?.rawValue ?: 0L,
            amountUnit = amount?.unit ?: AmountUnit.NEW,
            amountMatchedText = amount?.matchedText ?: "",
            amountConfidence = amount?.confidence ?: 0f,
            category = category,
            type = type,
            date = dateHit?.first ?: today,
            dateLabel = dateHit?.second,
            merchant = merchant,
            needsConfirmation = amount == null || amount.confidence < 0.5f,
            confidence = confidence,
        )
    }
}
