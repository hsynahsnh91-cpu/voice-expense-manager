import { setLocale, t, getDir, LOCALES, PRIVACY_POLICY } from '/home/user/voice-expense-manager/web/js/i18n.js';
import { todayCivil, addCivilDays, addCivilMonths, startOfCivilMonth, formatCivilPretty, toCivil, weekdayLabels, isCivilBetween } from '/home/user/voice-expense-manager/web/js/calendar.js';
import { hashPassword, validateEmail, validatePassword } from '/home/user/voice-expense-manager/web/js/auth.js';

setLocale('ar');
console.log('AR dir:', getDir(), '| signIn =', t('auth.signIn'), '| budget =', t('budget.title'));
setLocale('en');
console.log('EN dir:', getDir(), '| signIn =', t('auth.signIn'), '| budget =', t('budget.title'), '| weekStart =', LOCALES.en.weekStart, LOCALES.ar.weekStart);

const today = todayCivil();
console.log('\nCalendar civil-date checks (no UTC bug):');
console.log('  today          =', today, '→', formatCivilPretty(today,'ar'), '/', formatCivilPretty(today,'en'));
console.log('  +1 day         =', addCivilDays(today,1));
console.log('  -1 day         =', addCivilDays(today,-1));
console.log('  month start    =', startOfCivilMonth(today));
console.log('  +1 month       =', addCivilMonths(today,1), '| -1 month =', addCivilMonths(today,-1));
console.log('  2026-03-31 +1m =', addCivilMonths('2026-03-31',1), '(يجب 2026-04-30)');
console.log('  weekdays ar    =', weekdayLabels('ar',6).join(' '));
console.log('  weekdays en    =', weekdayLabels('en',0).join(' '));
console.log('  between check  =', isCivilBetween('2026-10-05','2026-10-01','2026-10-10'));
console.log('  local vs UTC   =', new Date(2026,9,4).toString().slice(0,24), '| toISO(UTC) =', new Date(2026,9,4).toISOString().slice(0,10));

console.log('\nAuth:');
console.log('  email ok  =', JSON.stringify(validateEmail('abu.omar@example.com')));
console.log('  email bad =', JSON.stringify(validateEmail('abc@')));
console.log('  pw short  =', JSON.stringify(validatePassword('abc1')));
console.log('  pw weak   =', JSON.stringify(validatePassword('abcdefgh')));
console.log('  pw ok     =', JSON.stringify(validatePassword('sawti2026')));
const h1 = await hashPassword('sawti2026', null);
const h2 = await hashPassword('sawti2026', h1.salt);
const h3 = await hashPassword('wrongpass', h1.salt);
console.log('  PBKDF2 iters =', h1.iterations, h1.alg);
console.log('  same pw same digest =', h1.digest === h2.digest);
console.log('  wrong pw differs    =', h1.digest !== h3.digest);
console.log('  digest len          =', h1.digest.length, 'salt len =', h1.salt.length);
