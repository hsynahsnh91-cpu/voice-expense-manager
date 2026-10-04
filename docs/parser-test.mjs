import { parseUtterance, extractAmount, formatMoney, civilDate, addDays, detectDate, newToLegacy, legacyToNew } from '../web/js/parser.js';

const today = civilDate();
const cases = [
  ['كلفني 5000 ليرة سوري روح على دمشق', 5000, 'transport', 'دمشق'],
  ['صرفت خمسة آلاف على الخضرة من السوق مبارح', 5000, 'food', null],
  ['دفعت فاتورة الكهرباء مية ألف', 100000, 'bills', null],
  ['ألفين ونص على الفروج', 2500, 'food', null],
  ['اشتريت بنطلون بـ ٧٥٠٠٠ ليرة قديمة', 750, 'clothing', null],
  ['قبضت راتبي مليون ونص', 1500000, 'other', null],
  ['عشرين ألف بنزين للسوزوكي', 20000, 'transport', null],
  ['أكلنا بمطعم بـ 350 الف', 350000, 'food', null],
  ['دفعت إيجار البيت ٨٠٠ ليرة جديدة', 800, 'home', null],
  ['تلت ميه ألف على الدوا من الصيدلية', 300000, 'health', null],
  ['اشتريت حليب وبيض بـ 45 الف', 45000, 'food', null],
  ['خمستاشر ألف سرفيس على الشغل', 15000, 'transport', null],
  ['صار علي ٢٥٠٠٠٠ مصروف المدرسة', 250000, 'education', null],
  ['دفعت اشتراك الإنترنت ٣٥ ألف', 35000, 'bills', null],
  ['١٢ ألف قهوة وشاي', 12000, 'food', null],
  ['دفعت 25000 ليرة للميكانيكي', 25000, 'transport', null],
  ['اشتريت هدية لمرتي بـ 150000', 150000, 'gifts', null],
  ['فاتورة المي ٨٥٠٠', 8500, 'bills', null],
  ['رحت على حلب وبـ ٧٥ الف بنزين', 75000, 'transport', 'حلب'],
  ['دفعت قسط المدرسة مليون ليرة', 1000000, 'education', null],
  ['٣٥٠٠ على الخبز والحليب', 3500, 'food', null],
  ['دفعت ٦٠ الف دوا للولد من الصيدلية', 60000, 'health', null],
  ['أكلت شاورما بـ٤٥٠٠', 4500, 'food', null],
  ['صرفت ٢٠٠٠٠٠ على المونة', 200000, 'food', null],
  ['دفعت اشتراك المولدة ٥٠ الف', 50000, 'bills', null],
  ['٥ الشهر دفعت ٣٠ الف كهربا', 30000, 'bills', null],
];

let pass = 0, fails = [];
for (const [text, expAmount, expCat, expPlace] of cases) {
  const p = parseUtterance(text, today);
  const amtOk = p && p.amountNew === expAmount;
  const catOk = p.category === expCat;
  const placeOk = !expPlace || (p.merchant && p.merchant.includes(expPlace));
  if (amtOk && catOk && placeOk) pass++;
  else fails.push({ text, got: p.amountNew, expAmount, cat: p.category, expCat, place: p.merchant, expPlace });
  console.log(`${amtOk && catOk && placeOk ? '✅' : '❌'}  ${text}\n     → ${formatMoney(p?.amountNew ?? 0,'ar')} (متوقع ${formatMoney(expAmount,'ar')}) | ${p?.category} (متوقع ${expCat}) | ${p?.merchant ?? '-'} | ${p?.date}`);
}
console.log(`\n===== ${pass}/${cases.length} نجحت =====`);
if (fails.length) { console.log('\nالأخطاء:'); fails.forEach(f => console.log(JSON.stringify(f))); }

console.log('\n--- تواريخ ---');
['اليوم','مبارح','اول مبارح','السبت','الجمعه','5 الشهر','12/3','الشهر الماضي'].forEach(t => {
  const d = detectDate(t, today); console.log(`  ${t.padEnd(14)} → ${d ? d.date + ' (' + d.label + ')' : 'null'}`);
});
console.log('\n--- عملة ---');
console.log('  100 قديمة =', legacyToNew(100), 'جديدة |', '1 جديدة =', newToLegacy(1), 'قديمة');
console.log('  أمس =', addDays(today, -1), '| اليوم =', today);
console.log('  صيغة عربية:', formatMoney(1234567,'ar'), '| إنكليزية:', formatMoney(1234567,'en'));
