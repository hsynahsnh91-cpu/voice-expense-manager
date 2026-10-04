/* =============================================================================
 *  Sawti — app.js : التطبيق الرئيسي (ويب)
 *  تسجيل دخول حقيقي → شاشة «ما ميزانيتك؟» → ٤ تبويبات سفلية
 *  صوت حقيقي: Web Speech API للتعرف + MediaRecorder للتسجيل + SpeechSynthesis للنطق
 *  لا بيانات تجريبية إطلاقاً — التطبيق يبدأ فارغاً تماماً.
 * ========================================================================== */

import {
  LOCALES, setLocale, getLocale, getDir, t, categoryName, onChange as onLocaleChange,
  PRIVACY_POLICY, TERMS,
} from './i18n.js';
import {
  CATEGORIES, parseUtterance, formatMoney, civilDate, categoryById,
  legacyToNew, newToLegacy, CURRENCY_NEW,
} from './parser.js';
import {
  getPrefs, savePrefs, getTransactions, addTransaction, updateTransaction,
  deleteTransaction, clearTransactions, putAudio, getAudio, audioStorageEstimate,
  exportData, importData, wipeEverything, subscribe, currentUser,
} from './store.js';
import { signUp, signIn, signOut, validateEmail } from './auth.js';
import {
  speechSupport, engineName, VoiceSession, speak, stopSpeaking, pickVoice, voiceInfo,
  playAudioBlob, stopAudio, ensureMicPermission, micPermissionState,
} from './speech.js';
import {
  mountDateRangeField, todayCivil, startOfCivilMonth, addCivilDays, addCivilMonths,
  formatCivilPretty, civilFromLocal, toCivil, fromCivil,
} from './calendar.js';

export const APP_VERSION = '1.0.0';
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));

/* ================================ الحالة ================================== */
const state = {
  user: null,
  prefs: getPrefs(),
  route: 'transactions',
  txs: [],
  filter: { from: startOfCivilMonth(todayCivil()), to: todayCivil(), category: 'all', q: '' },
  session: null,
  lastBlob: null,
  lastDuration: 0,
  parsed: null,
  playingId: null,
};

/* =============================== أدوات عامة ================================ */
function money(v) {
  const p = state.prefs;
  if (p.displayUnit === 'legacy') {
    const n = newToLegacy(v);
    return getLocale() === 'ar'
      ? `${n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '٬')} ل.س قديمة`
      : `${n.toLocaleString('en-US')} old SYP`;
  }
  return formatMoney(v, getLocale());
}
function moneyPlain(v) {
  return state.prefs.displayUnit === 'legacy' ? newToLegacy(v) : Math.round(v || 0);
}
function toast(msg, kind = '') {
  const host = $('#toasts');
  const el = document.createElement('div');
  el.className = 'toast ' + kind;
  el.textContent = msg;
  host.appendChild(el);
  setTimeout(() => el.remove(), 3400);
}
function setLoading(btn, on) {
  if (!btn) return;
  const sp = btn.querySelector('.spinner');
  const lb = btn.querySelector('.btn-label');
  btn.disabled = on;
  if (sp) sp.hidden = !on;
  if (lb) lb.style.opacity = on ? '.75' : '1';
}
function esc(s) { return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }

/* مودال عام */
let modalCleanup = null;
function openModal({ title, body, bodyIsText = false, actions = [], wide = false }) {
  const back = $('#modal');
  $('#modal-title').textContent = title;
  const b = $('#modal-body');
  b.className = 'modal-body' + (bodyIsText ? ' pre' : '');
  if (bodyIsText) b.textContent = body; else b.innerHTML = body;
  const foot = $('#modal-foot');
  foot.innerHTML = '';
  actions.forEach(a => {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'btn ripple ' + (a.kind || 'ghost');
    btn.textContent = a.label;
    btn.addEventListener('click', () => { a.onClick?.(btn); if (a.close !== false) closeModal(); });
    foot.appendChild(btn);
  });
  back.hidden = false;
  document.body.style.overflow = 'hidden';
  const onKey = e => { if (e.key === 'Escape') closeModal(); };
  document.addEventListener('keydown', onKey);
  modalCleanup = () => document.removeEventListener('keydown', onKey);
  setTimeout(() => $('#modal-close').focus(), 40);
  return b;
}
function closeModal() {
  $('#modal').hidden = true;
  document.body.style.overflow = '';
  modalCleanup?.(); modalCleanup = null;
}
function confirmDialog(title, text, onYes, yesLabel) {
  openModal({
    title,
    body: `<p style="margin:0;font-size:14.5px;line-height:1.8;color:var(--tx-2)">${esc(text)}</p>`,
    actions: [
      { label: t('common.cancel'), kind: 'ghost' },
      { label: yesLabel || t('common.confirm'), kind: 'danger', onClick: onYes },
    ],
  });
}

/* Ripple عند أي نقرة — تفاعل بسيط وحقيقي */
function initRipple() {
  document.addEventListener('pointerdown', e => {
    const host = e.target.closest('.ripple, .btn, .tab, .chip, .tx-item, .cal-day, .set-row');
    if (!host || host.disabled) return;
    const r = host.getBoundingClientRect();
    const size = Math.max(r.width, r.height);
    const s = document.createElement('span');
    s.className = 'rp';
    s.style.width = s.style.height = size + 'px';
    s.style.left = (e.clientX - r.left - size / 2) + 'px';
    s.style.top = (e.clientY - r.top - size / 2) + 'px';
    host.appendChild(s);
    setTimeout(() => s.remove(), 640);
  }, { passive: true });
}

/* ============================== الترجمة (i18n) ============================= */
function applyI18n() {
  const L = getLocale();
  document.documentElement.lang = L;
  document.documentElement.dir = getDir();
  document.body.dir = getDir();
  $$('[data-i18n]').forEach(el => { el.textContent = t(el.dataset.i18n); });
  $$('[data-i18n-placeholder]').forEach(el => { el.placeholder = t(el.dataset.i18nPlaceholder); });
  $$('[data-i18n-tpl]').forEach(el => { el.textContent = t(el.dataset.i18nTpl); });
  // أسماء الأزرار الممكن الوصول لها
  $('#toggle-signin-password')?.setAttribute('aria-label', L === 'ar' ? 'Show password' : 'Show password');
  $('#toggle-signup-password')?.setAttribute('aria-label', 'Show password');
  $('#tabbar')?.setAttribute('aria-label', L === 'ar' ? 'التنقل الرئيسي' : 'Main navigation');
  document.title = `${t('app.name')} — ${t('app.tagline')}`;
}

function applyTheme() {
  const th = state.prefs.theme;
  const dark = th === 'dark' || (th === 'system' && matchMedia('(prefers-color-scheme: dark)').matches);
  document.documentElement.dataset.theme = dark ? 'dark' : 'light';
  document.documentElement.classList.toggle('dark', dark);
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) meta.content = dark ? '#08120E' : '#0B7A5B';
}
matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => { if (state.prefs.theme === 'system') applyTheme(); });

/* ================================ التوجيه ================================= */
function showView(id) {
  ['view-auth', 'view-budget', 'view-app'].forEach(v => { $('#' + v).hidden = (v !== id); });
}

/* ============================ إخفاء شاشة الإقلاع ==========================
 *  تُخفى فور أول رسمّة حقيقية للواجهة (إطاران متتاليان) بدل تأخير ثابت،
 *  فلا ينتظر المستخدم ثوانٍ أمام شعار متحرك.                            */
function hideSplash() {
  const b = $('#boot');
  if (!b || b.dataset.hiding) return;
  b.dataset.hiding = '1';
  requestAnimationFrame(() => requestAnimationFrame(() => b.classList.add('gone')));
  setTimeout(() => b.remove(), 340);
}

/** يشغّل دالة عند خمول المتصفح إن أمكن، وإلا بعد فترة قصيرة */
function whenIdle(fn, timeout = 400) {
  if (typeof window.requestIdleCallback === 'function') window.requestIdleCallback(fn, { timeout });
  else setTimeout(fn, 60);
}

async function boot() {
  /* ١) كل شيء متزامن وسريع أولاً: التفضيلات واللغة والسمة */
  try {
    state.prefs = getPrefs();
    setLocale(state.prefs.locale || 'ar');
    applyI18n(); applyTheme(); initRipple();
    applyPrefsToUI();
    state.user = currentUser();
  } catch (e) {
    /* حتى لو فشل قراءة التخزين (سياق معزول مثلاً) نفتح على شاشة الدخول
       بدل أن تبقى شاشة الإقلاع معلّقة إلى الأبد */
    state.prefs = state.prefs || {};
    state.user = null;
  }

  /* ٢) نُظهر الواجهة المستهدفة فوراً */
  try {
    if (!state.user) { showView('view-auth'); setupAuth(); }
    else if (!state.prefs.onboarded || !state.prefs.monthlyBudget) { showView('view-budget'); setupBudget(); }
    else showView('view-app');
  } catch (e) { showView('view-auth'); }

  /* ٣) نخفي شاشة الإقلاع حالما تُرسم الواجهة — لا تأخير مصطنع */
  hideSplash();

  /* ٤) الشغل الأثقل (قوائم التطبيق وفحص الميكروفون) بعد أول رسمّة،
        حتى لا يُحجب الافتتاح ولا يشعر المستخدم بتعليق */
  if (state.user && state.prefs.onboarded && state.prefs.monthlyBudget) {
    setTimeout(() => { enterApp().catch(() => {}); }, 0);
  }
}

async function enterApp() {
  showView('view-app');
  setupApp();
  await navigate(location.hash.replace('#/', '') || 'transactions', true);
  renderAll();
  /* فحص بيئة الميكروفون مؤجَّل للخمول: لا يؤخر ظهور التطبيق أبداً */
  whenIdle(() => { try { checkMicEnvironment(); } catch { /* ignore */ } });
}

/* ============================== المصادقة ================================== */
function setupAuth() {
  const cardIn = $('#card-signin'), cardUp = $('#card-signup');
  const swap = (to) => {
    const out = to === 'signup' ? cardIn : cardUp;
    const inn = to === 'signup' ? cardUp : cardIn;
    out.style.animation = 'none'; out.hidden = true;
    inn.hidden = false;
    inn.style.animation = 'riseIn .42s var(--ease-out) both';
    setTimeout(() => inn.querySelector('input')?.focus(), 120);
  };
  $('#go-signup').onclick = () => swap('signup');
  $('#go-signin').onclick = () => swap('signin');

  // إظهار/إخفاء كلمة المرور (زر type=button باسم "Show password")
  [['#toggle-signin-password', '#signin-password'], ['#toggle-signup-password', '#signup-password']].forEach(([b, i]) => {
    const btn = $(b), inp = $(i);
    btn.onclick = () => {
      const show = inp.type === 'password';
      inp.type = show ? 'text' : 'password';
      btn.setAttribute('aria-pressed', String(show));
      btn.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
      inp.focus({ preventScroll: true });
    };
  });

  // أزرار الدخول الفيدرالي — غير مفعّلة (المستخدم سيربطها بنفسه)
  ['#btn-google', '#btn-apple', '#btn-google-2', '#btn-apple-2'].forEach(s => {
    const el = $(s); if (!el) return;
    el.onclick = () => toast(t('auth.federatedPending'), 'warn');
  });

  const showErr = (id, key) => {
    const el = $(id);
    el.hidden = false;
    el.textContent = t(key);
    el.style.animation = 'none'; void el.offsetWidth; el.style.animation = '';
  };

  $('#form-signin').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = $('#btn-signin');
    const errEl = $('#signin-error'); errEl.hidden = true;
    setLoading(btn, true);
    try {
      await new Promise(r => setTimeout(r, 120));   // إحساس حقيقي بالمعالجة
      const user = await signIn({ email: $('#signin-email').value, password: $('#signin-password').value });
      state.user = { email: user.email, name: user.name };
      state.prefs = getPrefs();
      $('#signin-password').value = '';
      if (!state.prefs.onboarded || !state.prefs.monthlyBudget) { showView('view-budget'); setupBudget(); }
      else await enterApp();
    } catch (err) {
      showErr('#signin-error', err.message.startsWith('auth.') ? err.message : 'auth.err.generic');
    } finally { setLoading(btn, false); }
  });

  $('#form-signup').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = $('#btn-signup');
    $('#signup-error').hidden = true;
    const pw = $('#signup-password').value, pw2 = $('#signup-password2').value;
    if (pw !== pw2) { showErr('#signup-error', 'auth.err.passwordMismatch'); return; }
    setLoading(btn, true);
    try {
      const user = await signUp({
        name: $('#signup-name').value,
        email: $('#signup-email').value,
        password: pw,
      });
      state.user = { email: user.email, name: user.name };
      showView('view-budget');
      setupBudget();
    } catch (err) {
      showErr('#signup-error', err.message.startsWith('auth.') ? err.message : 'auth.err.generic');
    } finally { setLoading(btn, false); }
  });
}

/* ========================== شاشة «ما ميزانيتك؟» =========================== */
function setupBudget() {
  const amt = $('#budget-amount'), cur = $('#budget-currency'), hint = $('#budget-hint');
  const chipsHost = $('#budget-chips');

  const renderHint = () => {
    const unit = cur.value;
    $('#budget-suffix').textContent = unit === 'LSN' ? CURRENCY_NEW.arSymbol : 'ل.س قديمة';
    if (unit === 'SYP-legacy') {
      const v = parseAmountInput(amt.value);
      hint.textContent = v > 0
        ? t('budget.convertedNote', { amount: formatMoney(legacyToNew(v), getLocale()) })
        : t('budget.hintLegacy');
    } else hint.textContent = t('budget.hintLegacy');
  };

  const presets = state.prefs.budgetCurrency === 'SYP-legacy'
    ? [100000, 500000, 1000000, 2500000, 5000000]
    : [500000, 1000000, 2000000, 5000000, 10000000];
  chipsHost.innerHTML = '';
  presets.forEach(p => {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = 'chip ripple';
    b.setAttribute('aria-pressed', 'false');
    b.textContent = p.toLocaleString(getLocale() === 'ar' ? 'ar-SY' : 'en-US');
    b.onclick = () => {
      amt.value = String(p);
      $$('.chip', chipsHost).forEach(c => c.setAttribute('aria-pressed', String(c === b)));
      renderHint();
    };
    chipsHost.appendChild(b);
  });

  cur.onchange = renderHint;
  amt.oninput = () => {
    // اقبل الأرقام فقط (لاتينية أو عربية)
    amt.value = amt.value.replace(/[٠-٩]/g, d => '٠١٢٣٤٥٦٧٨٩'.indexOf(d)).replace(/[^\d]/g, '');
    renderHint();
  };
  if (state.prefs.monthlyBudget) {
    amt.value = String(state.prefs.budgetCurrency === 'SYP-legacy'
      ? newToLegacy(state.prefs.monthlyBudget) : state.prefs.monthlyBudget);
    cur.value = state.prefs.budgetCurrency || 'LSN';
  }
  renderHint();

  $('#form-budget').onsubmit = async (e) => {
    e.preventDefault();
    const btn = $('#btn-enter-app');
    $('#budget-error').hidden = true;
    const raw = parseAmountInput(amt.value);
    if (!raw || raw <= 0) { showBudgetErr('budget.err.amount'); return; }
    if (raw > 999_000_000_000) { showBudgetErr('budget.err.range'); return; }
    setLoading(btn, true);
    const budgetNew = cur.value === 'SYP-legacy' ? legacyToNew(raw) : raw;
    savePrefs({ monthlyBudget: budgetNew, budgetCurrency: cur.value, onboarded: true, displayUnit: 'new' });
    state.prefs = getPrefs();
    await new Promise(r => setTimeout(r, 260));
    setLoading(btn, false);
    await enterApp();
    toast(getLocale() === 'ar' ? `تم ضبط ميزانيتك: ${money(budgetNew)}` : `Budget set: ${money(budgetNew)}`);
  };

  function showBudgetErr(k) {
    const el = $('#budget-error'); el.hidden = false; el.textContent = t(k);
    el.style.animation = 'none'; void el.offsetWidth; el.style.animation = '';
    amt.setAttribute('aria-invalid', 'true');
  }
}
function parseAmountInput(s) {
  const n = parseInt(String(s || '').replace(/[^\d]/g, ''), 10);
  return isNaN(n) ? 0 : n;
}

/* ============================== هيكل التطبيق =============================== */
function setupApp() {
  // شريط التنقل السفلي
  $$('#tabbar .tab').forEach(a => {
    a.addEventListener('click', (e) => {
      e.preventDefault();
      navigate(a.dataset.route);
      history.replaceState(null, '', '#/' + a.dataset.route);
    });
  });
  window.addEventListener('hashchange', () => {
    const r = location.hash.replace('#/', '') || 'transactions';
    if (r !== state.route) navigate(r);
  });

  $('#btn-add-tx').onclick = () => openTxEditor(null);
  $('#tx-search').oninput = (e) => { state.filter.q = e.target.value.trim(); renderTransactions(); };
  $('#tx-category-filter').onchange = (e) => { state.filter.category = e.target.value; renderTransactions(); };

  setupVoice();
  setupSettings();

  state.dateField = mountDateRangeField($('#tx-date-field'), {
    value: { from: state.filter.from, to: state.filter.to },
    label: t('tx.dateRange'),
    locale: getLocale(),
    align: 'start',
    onChange: (r) => {
      state.filter.from = r.from; state.filter.to = r.to;
      renderTransactions();
    },
  });

  $('#modal-close').onclick = closeModal;
  $('#modal').addEventListener('click', e => { if (e.target === $('#modal')) closeModal(); });

  subscribe(() => { state.txs = state.user ? getTransactions(state.user.email) : []; renderAll(); });
}

/* انتقال بين التبويبات: تلاشٍ + انزلاق خفيف (بدون push animation) */
async function navigate(route, immediate = false) {
  const routes = ['transactions', 'voice', 'inbox', 'settings'];
  if (!routes.includes(route)) route = 'transactions';
  const prev = $('#screen-' + state.route);
  const next = $('#screen-' + route);
  if (!next) return;

  $$('#tabbar .tab').forEach(a => {
    const active = a.dataset.route === route;
    if (active) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
  });

  if (immediate || prev === next || !prev || prev.hidden) {
    routes.forEach(r => { const s = $('#screen-' + r); if (s) s.hidden = (r !== route); });
    next.classList.remove('leaving');
    next.style.animation = 'none'; void next.offsetWidth; next.style.animation = '';
  } else {
    prev.classList.add('leaving');
    await new Promise(r => setTimeout(r, 150));
    routes.forEach(r => { const s = $('#screen-' + r); if (s) s.hidden = (r !== route); });
    prev.classList.remove('leaving');
    next.style.animation = 'none'; void next.offsetWidth; next.style.animation = '';
  }
  state.route = route;
  if (route === 'voice') { stopAudio(); }
  if (route !== 'voice') { cancelSession(); }
  if (route === 'transactions') renderTransactions();
  if (route === 'inbox') renderInbox();
  if (route === 'settings') renderSettings();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function renderAll() {
  state.txs = state.user ? getTransactions(state.user.email) : [];
  renderTransactions(); renderInbox(); renderBadge(); renderSettings();
}

/* ============================== شاشة المصاريف ============================== */
function filteredTx() {
  const f = state.filter;
  return state.txs.filter(x => {
    if (f.from && x.date < f.from) return false;
    if (f.to && x.date > f.to) return false;
    if (f.category !== 'all' && x.category !== f.category) return false;
    if (f.q) {
      const hay = `${x.note || ''} ${x.merchant || ''} ${categoryById(x.category).ar} ${categoryById(x.category).en} ${x.transcript || ''}`.toLowerCase();
      if (!hay.includes(f.q.toLowerCase())) return false;
    }
    return true;
  });
}

function renderTransactions() {
  const list = $('#tx-list'); if (!list) return;
  const rows = filteredTx();

  // الفئات في الفلتر
  const sel = $('#tx-category-filter');
  if (sel && sel.options.length !== CATEGORIES.length + 1) {
    sel.innerHTML = `<option value="all">${esc(t('tx.allCategories'))}</option>` +
      CATEGORIES.map(c => `<option value="${c.id}">${esc(getLocale() === 'ar' ? c.ar : c.en)}</option>`).join('');
    sel.value = state.filter.category;
  }

  // الملخص — الميزانية تُحسب على الشهر الحالي دائماً
  const monthPrefix = todayCivil().slice(0, 7);
  const monthTx = state.txs.filter(x => x.date.startsWith(monthPrefix));
  const spent = monthTx.filter(x => x.type !== 'income').reduce((s, x) => s + (x.amount || 0), 0);
  const income = monthTx.filter(x => x.type === 'income').reduce((s, x) => s + (x.amount || 0), 0);
  const budget = state.prefs.monthlyBudget || 0;
  const remaining = budget - spent;
  const pct = budget > 0 ? Math.min(100, Math.round((spent / budget) * 100)) : 0;

  $('#sum-spent').textContent = money(spent);
  $('#sum-income').textContent = money(income);
  $('#sum-balance').textContent = money(income - spent);
  $('#sum-remaining').textContent = money(Math.max(0, remaining));
  const fill = $('#budget-fill');
  fill.style.width = (budget > 0 ? Math.min(100, (spent / budget) * 100) : 0) + '%';
  fill.classList.toggle('over', budget > 0 && spent > budget);
  const pbar = $('#budget-progress');
  pbar.setAttribute('aria-valuenow', String(pct));
  pbar.setAttribute('aria-valuetext', `${pct}%`);
  $('#budget-progress-text').textContent = budget > 0
    ? (getLocale() === 'ar'
        ? `استهلكت ${pct}٪ من ميزانيتك${spent > budget ? ' — تجاوزت الميزانية!' : ''}`
        : `You used ${pct}% of your budget${spent > budget ? ' — over budget!' : ''}`)
    : (getLocale() === 'ar' ? 'ما ضبطت ميزانية بعد — اضبطها من الإعدادات.' : 'No budget set yet — set it in Settings.');
  $('#sum-remaining').style.color = (budget > 0 && spent > budget) ? '#FCA5A5' : '';

  // توزيع الفئات
  const byCat = {};
  rows.filter(x => x.type !== 'income').forEach(x => { byCat[x.category] = (byCat[x.category] || 0) + (x.amount || 0); });
  const catHost = $('#cat-breakdown');
  const entries = Object.entries(byCat).sort((a, b) => b[1] - a[1]).slice(0, 8);
  catHost.innerHTML = entries.map(([id, v]) => {
    const c = categoryById(id);
    return `<span class="cat-pill"><i style="background:${c.color}"></i>${esc(getLocale() === 'ar' ? c.ar : c.en)} <b>${esc(money(v))}</b></span>`;
  }).join('');
  catHost.hidden = !entries.length;

  // القائمة مجمّعة حسب اليوم
  $('#tx-empty').hidden = rows.length > 0;
  list.innerHTML = '';
  const groups = new Map();
  rows.forEach(x => { if (!groups.has(x.date)) groups.set(x.date, []); groups.get(x.date).push(x); });

  groups.forEach((items, date) => {
    const head = document.createElement('li');
    head.className = 'tx-day-head';
    const daySum = items.reduce((s, x) => s + (x.type === 'income' ? -(x.amount || 0) : (x.amount || 0)), 0);
    head.innerHTML = `<span>${esc(formatCivilPretty(date, getLocale()))}${date === todayCivil() ? ' · ' + t('date.today') : ''}</span><span>${esc(money(daySum))}</span>`;
    list.appendChild(head);
    items.forEach((x, i) => list.appendChild(txRow(x, i * 26)));
  });
}

function txRow(x, delay = 0) {
  const c = categoryById(x.category);
  const li = document.createElement('li');
  li.className = 'tx-item';
  li.style.animationDelay = delay + 'ms';
  const emoji = catEmoji(x.category);
  const sub = [x.merchant, x.dateLabel || (x.transcript ? '🎙 ' + truncate(x.transcript, 42) : '')].filter(Boolean).join(' · ');
  li.innerHTML = `
    <span class="tx-ic" style="background:${c.color}1F;color:${c.color}" aria-hidden="true">${emoji}</span>
    <span class="tx-main">
      <b>${esc(getLocale() === 'ar' ? c.ar : c.en)}</b>
      <i>${esc(sub || (x.type === 'income' ? t('tx.income') : t('tx.note')))}</i>
    </span>
    <span class="tx-amt ${x.type === 'income' ? 'income' : ''}">
      <b>${x.type === 'income' ? '+' : '−'}${esc(money(x.amount))}</b>
      <i>${esc(x.type === 'income' ? t('type.income') : t('type.expense'))}</i>
    </span>
    ${x.hasAudio ? `<button type="button" class="tx-play ripple" data-play="${x.id}" aria-label="${esc(t('tx.listen'))}" title="${esc(t('tx.listen'))}">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="currentColor" aria-hidden="true"><path d="M8 5.5v13l11-6.5Z"/></svg>
      </button>` : ''}`;
  li.addEventListener('click', (e) => {
    if (e.target.closest('[data-play]')) return;
    openTxEditor(x);
  });
  li.querySelector('[data-play]')?.addEventListener('click', async (e) => {
    e.stopPropagation();
    await listenTo(x.id, e.currentTarget);
  });
  return li;
}
function catEmoji(id) {
  return ({ food:'🍽️', transport:'🚕', bills:'💡', health:'💊', education:'📚', clothing:'👕',
            home:'🏠', family:'👨‍👩‍👧', gifts:'🎁', personal:'💆', business:'🧰', debt:'🧾', other:'📌' })[id] || '📌';
}
function truncate(s, n) { s = String(s || ''); return s.length > n ? s.slice(0, n - 1) + '…' : s; }

/* ------------------------- محرر المصروف (مودال) --------------------------- */
function openTxEditor(tx) {
  const isNew = !tx;
  const d = tx || { amount: 0, category: 'other', date: todayCivil(), type: 'expense', merchant: '', note: '' };
  const cats = CATEGORIES.map(c =>
    `<option value="${c.id}" ${c.id === d.category ? 'selected' : ''}>${esc(getLocale() === 'ar' ? c.ar : c.en)}</option>`).join('');
  const body = `
    <form id="tx-form" style="display:grid;gap:14px">
      <div class="field" style="margin:0">
        <label for="tx-amount">${esc(t('tx.amount'))} <small style="color:var(--tx-3);font-weight:500">(${esc(state.prefs.displayUnit === 'legacy' ? t('common.legacySyrianPound') : t('common.newSyrianPound'))})</small></label>
        <div class="amount-wrap">
          <input id="tx-amount" type="text" inputmode="numeric" value="${moneyPlain(d.amount) || ''}" placeholder="0">
          <span class="amount-suffix">${state.prefs.displayUnit === 'legacy' ? 'قديمة' : 'ل.س'}</span>
        </div>
      </div>
      <div class="field" style="margin:0">
        <label for="tx-type">${esc(t('voice.parsedType'))}</label>
        <div class="select-wrap"><select id="tx-type">
          <option value="expense" ${d.type === 'expense' ? 'selected' : ''}>${esc(t('type.expense'))}</option>
          <option value="income" ${d.type === 'income' ? 'selected' : ''}>${esc(t('type.income'))}</option>
          <option value="debt" ${d.type === 'debt' ? 'selected' : ''}>${esc(t('type.debt'))}</option>
        </select></div>
      </div>
      <div class="field" style="margin:0">
        <label for="tx-cat">${esc(t('tx.category'))}</label>
        <div class="select-wrap"><select id="tx-cat">${cats}</select></div>
      </div>
      <div class="field" style="margin:0">
        <label for="tx-date">${esc(t('tx.date'))}</label>
        <input id="tx-date" type="date" value="${esc(d.date)}">
      </div>
      <div class="field" style="margin:0">
        <label for="tx-merchant">${esc(t('tx.merchant'))}</label>
        <input id="tx-merchant" type="text" value="${esc(d.merchant || '')}" placeholder="دمشق / سوق الحميدية">
      </div>
      <div class="field" style="margin:0">
        <label for="tx-note">${esc(t('tx.note'))}</label>
        <textarea id="tx-note" rows="2" dir="auto">${esc(d.note || '')}</textarea>
      </div>
      ${tx?.transcript ? `<p class="inbox-transcript" style="margin:0">🎙 ${esc(tx.transcript)}</p>` : ''}
    </form>`;
  const actions = [
    { label: t('common.cancel'), kind: 'ghost' },
  ];
  if (!isNew) actions.push({ label: t('tx.delete'), kind: 'danger', onClick: async () => {
    await deleteTransaction(tx.id);
    toast(t('tx.deleted'));
    renderAll();
  }});
  actions.push({ label: t('common.save'), kind: 'primary', close: true, onClick: () => {
    const rawAmt = parseAmountInput($('#tx-amount').value);
    if (!rawAmt || rawAmt <= 0) { toast(t('budget.err.amount'), 'err'); return; }
    const amountNew = state.prefs.displayUnit === 'legacy' ? legacyToNew(rawAmt) : rawAmt;
    const payload = {
      amount: amountNew,
      type: $('#tx-type').value,
      category: $('#tx-cat').value,
      date: $('#tx-date').value || todayCivil(),
      merchant: $('#tx-merchant').value.trim() || null,
      note: $('#tx-note').value.trim(),
    };
    if (isNew) addTransaction(state.user.email, payload);
    else updateTransaction(tx.id, payload);
    toast(t('tx.saved'));
    renderAll();
  }});
  openModal({ title: isNew ? t('tx.add') : t('tx.edit'), body, actions });
}

/* ============================== شاشة الصوت ================================= */
function setupVoice() {
  const btn = $('#mic-btn'), stage = $('.mic-stage'), statusEl = $('#mic-status');
  const input = $('#voice-input');

  // حالة المحرك
  const pill = $('#engine-pill');
  if (!speechSupport.recognition) {
    pill.textContent = t('voice.engineNone');
    pill.classList.add('bad');
  } else {
    pill.textContent = t('voice.engineReal', { engine: engineName() });
    pill.classList.remove('bad');
  }

  // الأمثلة
  $('#examples-list').innerHTML = ['voice.ex1','voice.ex2','voice.ex3','voice.ex4','voice.ex5']
    .map(k => `<li data-ex="${k}">${esc(t(k))}</li>`).join('');
  $$('#examples-list li').forEach(li => li.onclick = () => {
    input.value = t(li.dataset.ex);
    input.dispatchEvent(new Event('input'));
    input.focus();
  });

  input.addEventListener('input', () => updateParsedPreview());

  btn.addEventListener('click', async () => {
    if (state.session && (state.session.state === 'listening' || state.session.state === 'processing')) {
      stopListening();
    } else {
      await startListening();
    }
  });

  $('#btn-voice-cancel').onclick = () => {
    cancelSession();
    input.value = '';
    $('#parsed-box').hidden = true;
    state.parsed = null; state.lastBlob = null;
    statusEl.textContent = t('voice.tap');
  };

  $('#btn-voice-send').onclick = () => saveFromVoice();

  $('#btn-toggle-unit').onclick = () => {
    if (!state.parsed?.amount) return;
    const a = state.parsed.amount;
    if (a.unit === 'legacy') { a.unit = 'new'; a.value = a.raw; }
    else { a.unit = 'legacy'; a.value = legacyToNew(a.raw); }
    state.parsed.amountNew = a.value;
    updateParsedPreview(true);
  };
}

async function startListening() {
  const stage = $('.mic-stage'), btn = $('#mic-btn'), statusEl = $('#mic-status'), input = $('#voice-input');

  if (!speechSupport.recognition) { toast(t('voice.notSupported'), 'err'); return; }

  try {
    await ensureMicPermission();
  } catch (e) {
    const map = { denied: 'voice.permissionDenied', 'no-device': 'voice.micInUse', busy: 'voice.micInUse', unsupported: 'voice.notSupported' };
    const msg = t(map[e.code] || 'voice.permissionNeeded');
    statusEl.textContent = msg;
    toast(msg, 'err');
    showMicWarning();
    return;
  }

  state.lastBlob = null; state.lastDuration = 0;
  input.classList.add('live');
  stage.classList.add('live');
  btn.setAttribute('aria-pressed', 'true');
  statusEl.textContent = t('voice.listening');

  const lang = state.prefs.speechLang || null;
  state.session = new VoiceSession({
    onStateChange: (s) => {
      if (s === 'listening') statusEl.textContent = t('voice.listening');
      if (s === 'processing') statusEl.textContent = t('voice.processing');
      if (s === 'error') { stage.classList.remove('live'); btn.setAttribute('aria-pressed', 'false'); input.classList.remove('live'); }
    },
    onLevel: (v) => { $('#mic-level').style.transform = `scale(${1 + v * 1.5})`; },
    onPartial: (text) => {
      input.value = text;                       // أي شي بتحكيه بينكتب فوراً بخانة الإرسال
      input.scrollTop = input.scrollHeight;
      updateParsedPreview();
    },
    onFinal: (chunk) => { input.value = (state.session.finalText + (state.session.partialText ? ' ' + state.session.partialText : '')).trim(); updateParsedPreview(); },
    onAudioReady: async (blob, dur) => {
      state.lastBlob = blob; state.lastDuration = dur || 0;
    },
    onError: (code) => {
      const map = {
        'not-allowed': 'voice.permissionDenied', 'service-not-allowed': 'voice.permissionDenied',
        'no-speech': null, 'audio-capture': 'voice.micInUse', 'network': 'voice.network',
        'aborted': null, 'language-not-supported': 'voice.notSupported',
      };
      if (map[code] === undefined) { statusEl.textContent = t('voice.noResult'); toast(t('voice.noResult'), 'warn'); }
      else if (map[code]) { statusEl.textContent = t(map[code]); toast(t(map[code]), 'err'); }
      cleanupMicUI();
    },
    onDone: (text) => {
      cleanupMicUI();
      if (text) { input.value = text; updateParsedPreview(); }
      else if (!input.value.trim()) statusEl.textContent = t('voice.noResult');
      else statusEl.textContent = t('voice.tap');
    },
  });
  await state.session.start({ lang });
}

function cleanupMicUI() {
  $('.mic-stage').classList.remove('live');
  $('#mic-btn').setAttribute('aria-pressed', 'false');
  $('#voice-input').classList.remove('live');
  $('#mic-level').style.transform = 'scale(1)';
}
function stopListening() {
  if (!state.session) return;
  const text = state.session.stop();
  if (text) $('#voice-input').value = text;
  cleanupMicUI();
  $('#mic-status').textContent = t('voice.tap');
  updateParsedPreview();
}
function cancelSession() {
  if (state.session) { try { state.session.cancel(); } catch {} state.session = null; }
  cleanupMicUI();
}

function updateParsedPreview(keepAmount) {
  const text = $('#voice-input').value.trim();
  const box = $('#parsed-box');
  if (!text) { box.hidden = true; state.parsed = null; return; }
  const prevUnit = state.parsed?.amount?.unit;
  const p = parseUtterance(text, todayCivil());
  state.parsed = p;
  if (keepAmount && prevUnit) { /* المستخدم قلب الوحدة يدوياً */ }
  box.hidden = false;

  const cells = [];
  if (p.amount) {
    cells.push(`<div class="parsed-cell amount"><b>${esc(t('voice.parsedAmount'))}</b><span>${esc(money(p.amountNew))}</span>
      ${p.amount.unit === 'legacy' ? `<small style="color:var(--tx-3)">${esc(t('budget.hintLegacy'))}</small>` : ''}</div>`);
  } else {
    cells.push(`<div class="parsed-cell amount"><b>${esc(t('voice.parsedAmount'))}</b><span style="color:var(--danger);font-size:14px">${esc(t('voice.noAmount'))}</span>
      <input id="manual-amount" type="text" inputmode="numeric" placeholder="0"></div>`);
  }
  cells.push(`<div class="parsed-cell"><b>${esc(t('voice.parsedCategory'))}</b>
    <select id="parsed-cat">${CATEGORIES.map(c => `<option value="${c.id}" ${c.id === p.category ? 'selected' : ''}>${esc(getLocale() === 'ar' ? c.ar : c.en)}</option>`).join('')}</select></div>`);
  cells.push(`<div class="parsed-cell"><b>${esc(t('voice.parsedDate'))}</b><input id="parsed-date" type="date" value="${esc(p.date)}"></div>`);
  cells.push(`<div class="parsed-cell"><b>${esc(t('voice.parsedPlace'))}</b><input id="parsed-place" type="text" value="${esc(p.merchant || '')}" placeholder="—"></div>`);
  cells.push(`<div class="parsed-cell"><b>${esc(t('voice.parsedType'))}</b>
    <select id="parsed-type">
      <option value="expense" ${p.type === 'expense' ? 'selected' : ''}>${esc(t('type.expense'))}</option>
      <option value="income" ${p.type === 'income' ? 'selected' : ''}>${esc(t('type.income'))}</option>
      <option value="debt" ${p.type === 'debt' ? 'selected' : ''}>${esc(t('type.debt'))}</option>
    </select></div>`);
  $('#parsed-grid').innerHTML = cells.join('');

  const unitBtn = $('#btn-toggle-unit');
  if (p.amount) {
    unitBtn.hidden = false;
    unitBtn.textContent = p.amount.unit === 'legacy'
      ? (getLocale() === 'ar' ? 'اعتبرها ليرة جديدة' : 'Treat as new pounds')
      : (getLocale() === 'ar' ? 'اعتبرها ليرة قديمة (÷١٠٠)' : 'Treat as old pounds (÷100)');
  } else unitBtn.hidden = true;
}

async function saveFromVoice() {
  const text = $('#voice-input').value.trim();
  if (!text) { toast(t('voice.noResult'), 'warn'); return; }
  const p = state.parsed || parseUtterance(text, todayCivil());

  let amountNew = p.amountNew;
  const manual = $('#manual-amount');
  if (manual) {
    const v = parseAmountInput(manual.value);
    if (!v) { toast(t('voice.noAmount'), 'err'); manual.focus(); return; }
    amountNew = state.prefs.displayUnit === 'legacy' ? legacyToNew(v) : v;
  }
  if (!amountNew || amountNew <= 0) { toast(t('voice.noAmount'), 'err'); return; }

  const rec = {
    amount: Math.round(amountNew),
    currency: 'LSN',
    category: $('#parsed-cat')?.value || p.category,
    type: $('#parsed-type')?.value || p.type,
    date: $('#parsed-date')?.value || p.date,
    merchant: ($('#parsed-place')?.value.trim() || p.merchant || null),
    note: '',
    transcript: text,
    dateLabel: p.dateLabel,
    hasAudio: !!state.lastBlob,
    audioDuration: state.lastDuration,
    listened: false,
    muted: !!state.prefs.mutePlaybackAfterRecord,
  };
  const saved = addTransaction(state.user.email, rec);

  if (state.lastBlob) {
    try { await putAudio(saved.id, state.lastBlob); } catch { /* ignore */ }
  }

  // التنظيف
  $('#voice-input').value = '';
  $('#parsed-box').hidden = true;
  state.parsed = null; state.lastBlob = null; state.lastDuration = 0;
  state.session = null;
  $('#mic-status').textContent = t('voice.tap');

  toast(t('voice.savedToast', { amount: money(saved.amount) }));
  renderAll();

  /* --- نطق الرد ---
     «كتم إعادة الصوت بعد التسجيل» = لا نُعيد كلام المستخدم ولا نُشغّل تسجيله.
     أما ردّ المساعد فيتحكم به خيار «نطق ردود المساعد» بشكل مستقل. */
  if (state.prefs.speakResponses) {
    const ar = getLocale() === 'ar';
    const line = ar
      ? `تمام، ضفت ${moneySpokenAr(saved.amount)} على ${categoryById(saved.category).ar}. ${remainingSpokenAr()}`
      : `Done — added ${money(saved.amount)} to ${categoryById(saved.category).en}. ${remainingSpokenEn()}`;
    speak(line, { rate: state.prefs.voiceRate, pitch: state.prefs.voicePitch });
  }
  if (!state.prefs.mutePlaybackAfterRecord && state.prefs.speakResponses === false) {
    // لا شيء: الكتم مغلول والنطق مغلق → صمت كامل
  }
}
function moneySpokenAr(v) {
  // نص منطوق طبيعي بالعربية العامية
  const n = Math.round(v);
  return `${n.toLocaleString('ar-SY')} ليرة سورية`;
}
function remainingSpokenAr() {
  const b = state.prefs.monthlyBudget || 0;
  if (!b) return '';
  const monthPrefix = todayCivil().slice(0, 7);
  const spent = state.txs.filter(x => x.date.startsWith(monthPrefix) && x.type !== 'income')
    .reduce((s, x) => s + (x.amount || 0), 0) + (state.parsed?.amountNew || 0);
  const left = b - spent;
  if (left < 0) return `انتبه، تجاوزت ميزانية الشهر بـ ${Math.abs(Math.round(left)).toLocaleString('ar-SY')} ليرة.`;
  return `ضل معك ${Math.round(left).toLocaleString('ar-SY')} ليرة من ميزانية الشهر.`;
}
function remainingSpokenEn() {
  const b = state.prefs.monthlyBudget || 0;
  if (!b) return '';
  const monthPrefix = todayCivil().slice(0, 7);
  const spent = state.txs.filter(x => x.date.startsWith(monthPrefix) && x.type !== 'income')
    .reduce((s, x) => s + (x.amount || 0), 0);
  const left = b - spent;
  return left < 0 ? `Careful, you are over budget.` : `${Math.round(left).toLocaleString('en-US')} pounds left this month.`;
}

/* =============================== شاشة الوارد =============================== */
function renderBadge() {
  const unheard = state.txs.filter(x => x.hasAudio && !x.listened).length;
  const badge = $('#inbox-badge');
  badge.hidden = unheard === 0;
  badge.textContent = unheard > 99 ? '99+' : String(unheard);
  const pill = $('#inbox-count-pill');
  if (pill) pill.textContent = String(state.txs.filter(x => x.hasAudio).length);
}

function renderInbox() {
  const list = $('#inbox-list'); if (!list) return;
  const items = state.txs.filter(x => x.hasAudio)
    .sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
  $('#inbox-empty').hidden = items.length > 0;
  renderBadge();
  list.innerHTML = '';
  items.forEach((x, i) => {
    const li = document.createElement('li');
    li.className = 'inbox-item' + (x.listened ? '' : ' unheard');
    li.style.animationDelay = (i * 30) + 'ms';
    const bars = Array.from({ length: 34 }, () => `<i style="height:${18 + Math.round(Math.random() * 80)}%"></i>`).join('');
    li.innerHTML = `
      <div class="inbox-top">
        <button type="button" class="inbox-play ripple" data-play="${x.id}" aria-label="${esc(t('inbox.play'))}">
          <svg viewBox="0 0 24 24" width="19" height="19" fill="currentColor" aria-hidden="true"><path d="M8 5.5v13l11-6.5Z"/></svg>
        </button>
        <span class="inbox-meta">
          <b>${esc(getLocale() === 'ar' ? categoryById(x.category).ar : categoryById(x.category).en)}</b>
          <i>${esc(formatCivilPretty(x.date, getLocale()))} · ${esc(t('inbox.duration', { s: Math.max(1, Math.round(x.audioDuration || 1)) }))}</i>
        </span>
        <span class="inbox-amt">${esc(money(x.amount))}</span>
      </div>
      <div class="wave" aria-hidden="true">${bars}</div>
      ${x.transcript ? `<p class="inbox-transcript">🎙 ${esc(x.transcript)}</p>` : ''}
      <div class="inbox-foot">
        <span class="tag ${x.listened ? 'ok' : 'warn'}">${esc(x.listened ? t('inbox.heard') : t('inbox.unheard'))}</span>
        ${x.muted ? `<span class="tag muted">🔇 ${esc(t('inbox.mutedBadge'))}</span>` : ''}
        <button type="button" class="btn small ghost ripple" data-del="${x.id}">${esc(t('inbox.delete'))}</button>
      </div>`;
    li.querySelector('[data-play]').addEventListener('click', (e) => listenTo(x.id, e.currentTarget, li));
    li.querySelector('[data-del]').addEventListener('click', () => {
      confirmDialog(t('inbox.delete'), t('inbox.delete') + '؟', async () => {
        await deleteTransaction(x.id);
        toast(t('inbox.deleted'));
        renderAll();
      });
    });
    list.appendChild(li);
  });
}

/** الاستماع للتسجيل — يُعلَّم كمستمع ويحدّث الشارة */
async function listenTo(id, btn, li) {
  if (state.playingId === id) { stopAudio(); state.playingId = null; resetPlayButtons(); return; }
  stopAudio();
  const blob = await getAudio(id);
  if (!blob) { toast(t('tx.noAudio'), 'warn'); return; }
  resetPlayButtons();
  state.playingId = id;
  btn?.classList.add('playing');
  li?.classList.add('playing');
  if (btn) btn.innerHTML = `<svg viewBox="0 0 24 24" width="17" height="17" fill="currentColor" aria-hidden="true"><rect x="7" y="5.5" width="3.6" height="13" rx="1.4"/><rect x="13.4" y="5.5" width="3.6" height="13" rx="1.4"/></svg>`;

  const bars = li ? $$('.wave i', li) : [];
  await playAudioBlob(blob, {
    onProgress: (cur, dur) => {
      if (!bars.length || !dur) return;
      const idx = Math.floor((cur / dur) * bars.length);
      bars.forEach((b, i) => { b.style.opacity = i <= idx ? '1' : '.42'; });
    },
    onEnd: async () => {
      resetPlayButtons();
      state.playingId = null;
      const x = state.txs.find(t2 => t2.id === id);
      if (x && !x.listened) { updateTransaction(id, { listened: true }); renderAll(); }
    },
  });
  const x = state.txs.find(t2 => t2.id === id);
  if (x && !x.listened) { updateTransaction(id, { listened: true }); renderAll(); }
}
function resetPlayButtons() {
  $$('.inbox-play.playing, .tx-play.playing').forEach(b => {
    b.classList.remove('playing');
    b.innerHTML = `<svg viewBox="0 0 24 24" width="17" height="17" fill="currentColor" aria-hidden="true"><path d="M8 5.5v13l11-6.5Z"/></svg>`;
  });
  $$('.inbox-item.playing').forEach(el => {
    el.classList.remove('playing');
    $$('.wave i', el).forEach(b => b.style.opacity = '1');
  });
}

/* ============================== الإعدادات ================================= */
function applyPrefsToUI() {
  const p = state.prefs;
  const setV = (sel, val) => { const el = $(sel); if (el) el.value = val; };
  const setChk = (sel, val) => { const el = $(sel); if (el) el.checked = !!val; };
  setV('#set-locale', p.locale);
  setChk('#set-mute', p.mutePlaybackAfterRecord);
  setChk('#set-speak', p.speakResponses);
  setV('#set-rate', p.voiceRate); $('#set-rate-val').textContent = Number(p.voiceRate).toFixed(2) + '×';
  setV('#set-pitch', p.voicePitch); $('#set-pitch-val').textContent = Number(p.voicePitch).toFixed(2);
  setV('#set-display-unit', p.displayUnit);
  $$('#theme-seg button').forEach(b => b.setAttribute('aria-checked', String(b.dataset.theme === p.theme)));
  const langSel = $('#set-speech-lang');
  if (langSel) {
    const opts = [...LOCALES.ar.speechTags, 'en-US', 'en-GB', 'tr-TR'].map(l =>
      `<option value="${l}" ${p.speechLang === l ? 'selected' : ''}>${l}</option>`);
    langSel.innerHTML = `<option value="" ${!p.speechLang ? 'selected' : ''}>${getLocale() === 'ar' ? 'تلقائي (حسب لغة التطبيق)' : 'Auto (follow app language)'}</option>` + opts.join('');
  }
}

function setupSettings() {
  $('#set-locale').onchange = (e) => {
    savePrefs({ locale: e.target.value });
    state.prefs = getPrefs();
    setLocale(e.target.value);
    applyI18n(); renderAll(); setupVoiceTexts();
    state.dateField?.setLocale(e.target.value);
    $('#tx-date-field').innerHTML = '';
    state.dateField = mountDateRangeField($('#tx-date-field'), {
      value: { from: state.filter.from, to: state.filter.to },
      label: t('tx.dateRange'), locale: e.target.value, align: 'start',
      onChange: (r) => { state.filter.from = r.from; state.filter.to = r.to; renderTransactions(); },
    });
    toast(getLocale() === 'ar' ? 'تم تغيير اللغة إلى العربية' : 'Language changed to English');
  };

  $('#set-mute').onchange = (e) => {
    savePrefs({ mutePlaybackAfterRecord: e.target.checked });
    state.prefs = getPrefs();
    toast(e.target.checked
      ? (getLocale() === 'ar' ? 'الكتم مفعّل — ما رح ينعاد صوتك بعد الإرسال' : 'Mute on — your voice will not replay')
      : (getLocale() === 'ar' ? 'الكتم مغلق' : 'Mute off'));
    if (e.target.checked) stopSpeaking();
  };

  $('#set-speak').onchange = (e) => { savePrefs({ speakResponses: e.target.checked }); state.prefs = getPrefs(); };
  $('#set-rate').oninput = (e) => {
    savePrefs({ voiceRate: parseFloat(e.target.value) }); state.prefs = getPrefs();
    $('#set-rate-val').textContent = Number(e.target.value).toFixed(2) + '×';
  };
  $('#set-pitch').oninput = (e) => {
    savePrefs({ voicePitch: parseFloat(e.target.value) }); state.prefs = getPrefs();
    $('#set-pitch-val').textContent = Number(e.target.value).toFixed(2);
  };
  $('#set-display-unit').onchange = (e) => {
    savePrefs({ displayUnit: e.target.value }); state.prefs = getPrefs(); renderAll(); renderSettings();
  };
  $('#set-speech-lang').onchange = (e) => {
    savePrefs({ speechLang: e.target.value || null }); state.prefs = getPrefs();
  };
  $$('#theme-seg button').forEach(b => b.onclick = () => {
    savePrefs({ theme: b.dataset.theme }); state.prefs = getPrefs(); applyTheme(); applyPrefsToUI();
  });
  $('#btn-test-voice').onclick = () => {
    speak(t('settings.testVoiceText'), { rate: state.prefs.voiceRate, pitch: state.prefs.voicePitch });
    renderSettings();
  };
  $('#btn-budget-edit').onclick = () => openBudgetModal();
  $('#btn-privacy').onclick = () => openModal({
    title: t('settings.privacy'), body: PRIVACY_POLICY[getLocale()] || PRIVACY_POLICY.ar, bodyIsText: true,
    actions: [{ label: t('common.close'), kind: 'primary' }],
  });
  $('#btn-terms').onclick = () => openModal({
    title: t('settings.terms'), body: TERMS[getLocale()] || TERMS.ar, bodyIsText: true,
    actions: [{ label: t('common.close'), kind: 'primary' }],
  });
  $('#btn-export').onclick = async () => {
    const data = await exportData(state.user.email);
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `sawti-backup-${todayCivil()}.json`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 4000);
    toast(t('settings.exported'));
  };
  $('#btn-import').onclick = () => $('#file-import').click();
  $('#file-import').onchange = async (e) => {
    const f = e.target.files?.[0]; if (!f) return;
    try {
      const data = JSON.parse(await f.text());
      const n = await importData(state.user.email, data);
      toast(t('settings.imported') + ` (${n})`);
      state.prefs = getPrefs(); applyPrefsToUI(); renderAll();
    } catch { toast(t('settings.importError'), 'err'); }
    e.target.value = '';
  };
  $('#btn-clear-tx').onclick = () => confirmDialog(t('settings.clearTx'), t('settings.clearTxConfirm'), async () => {
    await clearTransactions(state.user.email);
    toast(t('settings.deleted'));
    renderAll();
  });
  $('#btn-signout').onclick = () => confirmDialog(t('settings.signOut'), t('settings.signOutConfirm'), () => {
    signOut(); stopSpeaking(); stopAudio();
    location.hash = '';
    location.reload();
  });
  $('#btn-delete-account').onclick = () => confirmDialog(t('settings.deleteAccount'), t('settings.deleteConfirm'), async () => {
    await wipeEverything(state.user.email);
    toast(t('settings.deleted'));
    setTimeout(() => location.reload(), 700);
  }, t('settings.deleteAccount'));
}

function setupVoiceTexts() {
  const pill = $('#engine-pill');
  if (pill) {
    pill.textContent = speechSupport.recognition ? t('voice.engineReal', { engine: engineName() }) : t('voice.engineNone');
    pill.classList.toggle('bad', !speechSupport.recognition);
  }
  $('#examples-list').innerHTML = ['voice.ex1','voice.ex2','voice.ex3','voice.ex4','voice.ex5']
    .map(k => `<li data-ex="${k}">${esc(t(k))}</li>`).join('');
  $$('#examples-list li').forEach(li => li.onclick = () => {
    const inp = $('#voice-input'); inp.value = t(li.dataset.ex);
    inp.dispatchEvent(new Event('input')); inp.focus();
  });
}

function openBudgetModal() {
  const p = state.prefs;
  const body = `
    <form id="budget-modal-form" style="display:grid;gap:14px">
      <div class="field" style="margin:0">
        <label for="bm-amount">${esc(t('budget.title'))}</label>
        <div class="amount-wrap">
          <input id="bm-amount" type="text" inputmode="numeric" value="${p.monthlyBudget || ''}" placeholder="2000000">
          <span class="amount-suffix">ل.س</span>
        </div>
      </div>
      <div class="field" style="margin:0">
        <label for="bm-currency">${esc(t('budget.currencyLabel'))}</label>
        <div class="select-wrap"><select id="bm-currency">
          <option value="LSN">${esc(t('budget.currencyNew'))}</option>
          <option value="SYP-legacy">${esc(t('budget.currencyLegacy'))}</option>
        </select></div>
        <p class="hint">${esc(t('budget.hintLegacy'))}</p>
      </div>
    </form>`;
  openModal({
    title: t('budget.edit'), body,
    actions: [
      { label: t('common.cancel'), kind: 'ghost' },
      { label: t('common.save'), kind: 'primary', onClick: () => {
        const raw = parseAmountInput($('#bm-amount').value);
        if (!raw) { toast(t('budget.err.amount'), 'err'); return; }
        const cur = $('#bm-currency').value;
        const budgetNew = cur === 'SYP-legacy' ? legacyToNew(raw) : raw;
        savePrefs({ monthlyBudget: budgetNew, budgetCurrency: cur });
        state.prefs = getPrefs(); renderAll();
        toast(getLocale() === 'ar' ? `الميزانية الجديدة: ${money(budgetNew)}` : `New budget: ${money(budgetNew)}`);
      }},
    ],
  });
  $('#bm-currency').value = p.budgetCurrency || 'LSN';
}

function renderSettings() {
  if (!state.user) return;
  $('#set-name').textContent = state.user.name || state.user.email;
  $('#set-email').textContent = state.user.email;
  $('#set-avatar').textContent = (state.user.name || state.user.email || '?').trim().charAt(0).toUpperCase();
  $('#set-budget-value').textContent = money(state.prefs.monthlyBudget || 0);
  $('#set-version').textContent = APP_VERSION;
  const vi = voiceInfo();
  $('#set-engine-name').textContent = vi ? `${vi.name} (${vi.lang})` : (getLocale() === 'ar' ? 'لا يوجد صوت عربي مثبّت' : 'No Arabic voice installed');
  const st = $('#engine-status');
  if (st) {
    const parts = [];
    parts.push(speechSupport.recognition ? (getLocale() === 'ar' ? 'التعرّف الصوتي ✅' : 'Recognition ✅') : (getLocale() === 'ar' ? 'التعرّف الصوتي ❌' : 'Recognition ❌'));
    parts.push(speechSupport.recording ? (getLocale() === 'ar' ? 'التسجيل ✅' : 'Recording ✅') : (getLocale() === 'ar' ? 'التسجيل ❌' : 'Recording ❌'));
    parts.push(speechSupport.synthesis ? (getLocale() === 'ar' ? 'النطق ✅' : 'Speech ✅') : (getLocale() === 'ar' ? 'النطق ❌' : 'Speech ❌'));
    st.textContent = parts.join(' · ');
    st.style.color = (speechSupport.recognition && speechSupport.recording) ? 'var(--brand-500)' : 'var(--danger)';
  }
  audioStorageEstimate().then(({ usage }) => {
    const el = $('#set-storage');
    if (el) el.textContent = `${t('settings.storageUsed')}: ${(usage / 1048576).toFixed(2)} MB`;
  });
  renderBadge();
}

/* تحذير بيئة غير آمنة (الميكروفون يحتاج HTTPS) */
async function checkMicEnvironment() {
  const secure = window.isSecureContext;
  const warn = $('#mic-warning');
  if (!secure || !navigator.mediaDevices?.getUserMedia) {
    warn.hidden = false;
    $('#mic-warning-text').textContent = getLocale() === 'ar'
      ? 'الميكروفون يحتاج اتصالاً آمناً (HTTPS). افتح التطبيق من الرابط الخارجي حتى يشتغل الصوت.'
      : 'The microphone needs a secure context (HTTPS). Open the app from the external link for voice to work.';
    const openBtn = $('#mic-warning-open');
    openBtn.textContent = getLocale() === 'ar' ? 'افتح بنافذة كاملة' : 'Open full page';
    openBtn.onclick = () => window.open(location.href, '_blank', 'noopener');
  } else {
    const st = await micPermissionState();
    if (st === 'denied') {
      warn.hidden = false;
      $('#mic-warning-text').textContent = t('voice.permissionDenied');
      $('#mic-warning-open').hidden = true;
    } else warn.hidden = true;
  }
}
function showMicWarning() {
  const warn = $('#mic-warning');
  warn.hidden = false;
  $('#mic-warning-text').textContent = t('voice.permissionDenied');
  const b = $('#mic-warning-open');
  b.hidden = false;
  b.textContent = getLocale() === 'ar' ? 'افتح بنافذة كاملة' : 'Open full page';
  b.onclick = () => window.open(location.href, '_blank', 'noopener');
}

/* إعادة الترجمة عند تغيير اللغة من أي مكان */
onLocaleChange(() => { applyI18n(); });

/* Service worker (PWA) — يُسجَّل فقط بسياق آمن وليس داخل iframe مقيد */
if ('serviceWorker' in navigator && window.isSecureContext && window.self === window.top) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('sw.js').catch(() => { /* لا يؤثر على عمل التطبيق */ });
  });
}

/* منع فقدان النص عند إعادة التحميل */
window.addEventListener('beforeunload', () => { stopSpeaking(); stopAudio(); });

/* ================================ الإقلاع ================================= */
boot();
