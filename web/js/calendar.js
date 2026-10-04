/* =============================================================================
 *  Sawti — calendar.js : منتقي فترة (Date Range Picker)
 *  ---------------------------------------------------------------------------
 *  نمط shadcn/ui: Popover + Calendar (mode="range")
 *  • كل التواريخ نصوص مدنية yyyy-mm-dd — أبداً لا Date-parsed-as-UTC
 *    (هذا هو إصلاح خطأ اليوم الناقص الشهير)
 *  • الطرفان يأخذان range_start / range_end، والأيام بينهما range_middle
 *  • بداية الأسبوع حسب اللغة (السبت للسورية، الأحد للإنكليزية)
 *  • نموذج لوحة المفاتيح كامل: الأسهم يوم/أسبوع، PageUp/Down شهر،
 *    Home/End طرف الأسبوع، Enter/Space تحديد، Esc إغلاق
 * ========================================================================== */

import { LOCALES, getLocale, t } from './i18n.js';

/* ------------------------ أدوات التاريخ المدني --------------------------- */
export function toCivil(y, m, d) {
  return `${String(y).padStart(4, '0')}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
}
export function fromCivil(c) {
  const parts = String(c).split('-');
  return { y: Number(parts[0]), m: Number(parts[1]), d: Number(parts[2]) };
}
export function civilFromLocal(date) {
  return toCivil(date.getFullYear(), date.getMonth() + 1, date.getDate());
}
export function civilToLocal(c) {
  const { y, m, d } = fromCivil(c);
  return new Date(y, m - 1, d);          // محلي بالكامل، لا UTC
}
export function todayCivil() { return civilFromLocal(new Date()); }
export function addCivilDays(c, n) {
  const dt = civilToLocal(c);
  dt.setDate(dt.getDate() + n);
  return civilFromLocal(dt);
}
export function addCivilMonths(c, n) {
  const { y, m, d } = fromCivil(c);
  const dt = new Date(y, m - 1 + n, 1);
  const last = new Date(dt.getFullYear(), dt.getMonth() + 1, 0).getDate();
  return toCivil(dt.getFullYear(), dt.getMonth() + 1, Math.min(d, last));
}
export function startOfCivilMonth(c) { const { y, m } = fromCivil(c); return toCivil(y, m, 1); }
export function daysInCivilMonth(c) {
  const { y, m } = fromCivil(c);
  return new Date(y, m, 0).getDate();
}
export function weekdayOf(c) { return civilToLocal(c).getDay(); }
export function diffCivilDays(a, b) { return Math.round((civilToLocal(b) - civilToLocal(a)) / 86400000); }
export function isCivilBetween(c, from, to) {
  if (!from || !to) return false;
  const [a, b] = from <= to ? [from, to] : [to, from];
  return c >= a && c <= b;
}
export function monthLabel(c, locale) {
  return civilToLocal(c).toLocaleDateString(locale === 'ar' ? 'ar-SY' : 'en-US',
    { month: 'long', year: 'numeric' });
}
export function weekdayLabels(locale, weekStart) {
  const base = civilToLocal('2024-01-07');      // يوم أحد معروف (7 كانون الثاني 2024)
  const out = [];
  for (let i = 0; i < 7; i++) {
    const d = new Date(base);
    d.setDate(base.getDate() + ((i + weekStart) % 7));
    out.push(d.toLocaleDateString(locale === 'ar' ? 'ar-SY' : 'en-US', { weekday: 'short' }));
  }
  return out;
}
export function formatCivilPretty(c, locale) {
  if (!c) return '';
  return civilToLocal(c).toLocaleDateString(locale === 'ar' ? 'ar-SY' : 'en-US',
    { day: 'numeric', month: 'short', year: 'numeric' });
}

/* ============================ مكوّن التقويم =============================== */
export class RangeCalendar {
  /**
   * @param {HTMLElement} root
   * @param {Object} opts { selected:{from,to}, onSelect(range), mode, locale, weekStart, numberOfMonths, min, max }
   */
  constructor(root, opts = {}) {
    this.root = root;
    this.locale = opts.locale || getLocale();
    this.weekStart = opts.weekStart ?? LOCALES[this.locale].weekStart;
    this.mode = opts.mode || 'range';
    this.numberOfMonths = opts.numberOfMonths ?? (window.innerWidth < 700 ? 1 : 2);
    this.selected = opts.selected || { from: null, to: null };
    this.onSelect = opts.onSelect || (() => {});
    this.viewMonth = opts.viewMonth || startOfCivilMonth(this.selected?.from || todayCivil());
    this.cursor = this.selected?.from || todayCivil();
    this.hover = null;
    this.picking = false;         // هل اختار المستخدم الطرف الأول وينتظر الثاني؟
    this.root.classList.add('cal-root');
    this.render();
  }

  setLocale(l) { this.locale = l; this.weekStart = LOCALES[l]?.weekStart ?? 6; this.render(); }

  setSelected(range, { keepView = false } = {}) {
    this.selected = range ? { from: range.from || null, to: range.to || null } : { from: null, to: null };
    if (!keepView && this.selected.from) this.viewMonth = startOfCivilMonth(this.selected.from);
    this.picking = !!(this.selected.from && !this.selected.to);
    this.render();
  }

  getRange() { return { ...this.selected }; }

  /* ------------------------------ البناء ------------------------------- */
  render() {
    const dir = LOCALES[this.locale]?.dir || 'rtl';
    this.root.innerHTML = '';
    this.root.setAttribute('dir', dir);

    const wrap = document.createElement('div');
    wrap.className = 'cal';

    const months = [];
    for (let i = 0; i < this.numberOfMonths; i++) months.push(addCivilMonths(this.viewMonth, i));

    /* رأس التنقل */
    const head = document.createElement('div');
    head.className = 'cal-head';
    const mk = (cls, aria, txt, fn) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'cal-nav ' + cls;
      b.setAttribute('aria-label', aria);
      b.title = aria;
      b.textContent = txt;
      b.addEventListener('click', (e) => { e.stopPropagation(); fn(); this._focusCursor(); });
      return b;
    };
    head.appendChild(mk('prev-year', t('date.prevYear'), '«', () => { this.viewMonth = addCivilMonths(this.viewMonth, -12); this.render(); }));
    head.appendChild(mk('prev-month', t('date.prevMonth'), '‹', () => { this.viewMonth = addCivilMonths(this.viewMonth, -1); this.render(); }));
    const live = document.createElement('div');
    live.className = 'cal-month';
    live.id = 'cal-live-' + Math.random().toString(36).slice(2, 7);
    live.textContent = months.length > 1
      ? `${monthLabel(months[0], this.locale)} — ${monthLabel(months[months.length - 1], this.locale)}`
      : monthLabel(months[0], this.locale);
    head.appendChild(live);
    head.appendChild(mk('next-month', t('date.nextMonth'), '›', () => { this.viewMonth = addCivilMonths(this.viewMonth, 1); this.render(); }));
    head.appendChild(mk('next-year', t('date.nextYear'), '»', () => { this.viewMonth = addCivilMonths(this.viewMonth, 12); this.render(); }));
    wrap.appendChild(head);

    /* الشهور */
    const grid = document.createElement('div');
    grid.className = 'cal-months';
    grid.setAttribute('aria-labelledby', live.id);
    months.forEach(m => grid.appendChild(this._month(m)));
    wrap.appendChild(grid);

    const hint = document.createElement('p');
    hint.className = 'cal-hint';
    hint.textContent = t('date.rangeHint');
    wrap.appendChild(hint);

    this.root.appendChild(wrap);
    this._focusCursor();
  }

  _month(monthCivil) {
    const box = document.createElement('div');
    box.className = 'cal-monthbox';

    const cap = document.createElement('div');
    cap.className = 'cal-monthname';
    cap.setAttribute('aria-hidden', 'true');
    cap.textContent = monthLabel(monthCivil, this.locale);
    box.appendChild(cap);

    const table = document.createElement('table');
    table.className = 'cal-table';
    table.setAttribute('role', 'grid');
    table.tabIndex = -1;

    const thead = document.createElement('thead');
    const hrow = document.createElement('tr');
    weekdayLabels(this.locale, this.weekStart).forEach((w, i) => {
      const th = document.createElement('th');
      th.scope = 'col';
      th.setAttribute('aria-label', w);
      th.innerHTML = `<abbr title="${w}"><span aria-hidden="true">${w}</span></abbr>`;
      th.dataset.wd = String((i + this.weekStart) % 7);
      hrow.appendChild(th);
    });
    thead.appendChild(hrow);
    table.appendChild(thead);

    const tbody = document.createElement('tbody');
    const total = daysInCivilMonth(monthCivil);
    const { y, m } = fromCivil(monthCivil);
    const lead = ((weekdayOf(toCivil(y, m, 1)) - this.weekStart) + 7) % 7;
    const today = todayCivil();

    let cellIdx = 0;
    let row = document.createElement('tr');
    for (let i = 0; i < lead; i++) { row.appendChild(this._emptyCell()); cellIdx++; }

    for (let d = 1; d <= total; d++) {
      if (cellIdx % 7 === 0) { row = document.createElement('tr'); tbody.appendChild(row); }
      const c = toCivil(y, m, d);
      row.appendChild(this._dayCell(c, today));
      cellIdx++;
    }
    while (cellIdx % 7 !== 0) { row.appendChild(this._emptyCell()); cellIdx++; }
    if (!tbody.children.length) tbody.appendChild(row);
    table.appendChild(tbody);
    box.appendChild(table);
    return box;
  }

  _emptyCell() {
    const td = document.createElement('td');
    td.className = 'cal-cell empty';
    td.setAttribute('aria-hidden', 'true');
    return td;
  }

  _dayCell(c, today) {
    const td = document.createElement('td');
    td.className = 'cal-cell';
    td.dataset.date = c;
    td.setAttribute('role', 'gridcell');

    const { from, to } = this.selected;
    const preview = this.picking && this.hover ? this.hover : null;
    const effFrom = from, effTo = to || preview;

    const isStart = !!from && c === from;
    const isEnd = !!to && c === to;
    const isMiddle = !isStart && !isEnd && effFrom && effTo && effFrom !== effTo && isCivilBetween(c, effFrom, effTo);
    const isToday = c === today;
    const isCursor = c === this.cursor;
    const isOutside = false;

    td.classList.toggle('range_start', isStart);
    td.classList.toggle('range_end', isEnd);
    td.classList.toggle('range_middle', !!isMiddle);
    td.classList.toggle('today', isToday);
    td.classList.toggle('outside', isOutside);
    td.dataset.modifiers = [isStart && 'range_start', isEnd && 'range_end', isMiddle && 'range_middle', isToday && 'today']
      .filter(Boolean).join(' ');

    const b = document.createElement('button');
    b.type = 'button';
    b.className = 'cal-day';
    b.dataset.date = c;
    b.textContent = String(fromCivil(c).d);
    b.tabIndex = isCursor ? 0 : -1;          // roving tabindex (نموذج الشبكة)
    b.setAttribute('aria-label', formatCivilPretty(c, this.locale));
    b.setAttribute('aria-selected', String(isStart || isEnd || !!isMiddle));
    if (isToday) b.setAttribute('aria-current', 'date');

    b.addEventListener('click', (e) => { e.stopPropagation(); this._pick(c); });
    b.addEventListener('mouseenter', () => {
      if (this.picking && this.hover !== c) { this.hover = c; this._repaint(); }
    });
    td.appendChild(b);
    return td;
  }

  _repaint() {
    const cells = this.root.querySelectorAll('.cal-cell[data-date]');
    const { from, to } = this.selected;
    const effTo = to || (this.picking ? this.hover : null);
    cells.forEach(td => {
      const c = td.dataset.date;
      const btn = td.querySelector('.cal-day');
      const isStart = !!from && c === from;
      const isEnd = !!to && c === to;
      const isMiddle = !isStart && !isEnd && from && effTo && from !== effTo && isCivilBetween(c, from, effTo);
      td.classList.toggle('range_start', isStart);
      td.classList.toggle('range_end', isEnd);
      td.classList.toggle('range_middle', !!isMiddle);
      btn?.setAttribute('aria-selected', String(isStart || isEnd || !!isMiddle));
    });
  }

  _pick(c) {
    this.cursor = c;
    if (this.mode === 'single') {
      this.selected = { from: c, to: c };
      this.picking = false;
      this.render();
      this.onSelect(this.getRange());
      return;
    }
    if (!this.picking) {
      // بداية اختيار جديد
      this.selected = { from: c, to: null };
      this.picking = true;
      this.hover = null;
      this.render();
      this.onSelect({ ...this.getRange(), pending: true });
      return;
    }
    // الطرف الثاني
    let { from } = this.selected;
    let to = c;
    if (to < from) { const tmp = from; from = to; to = tmp; }
    this.selected = { from, to };
    this.picking = false;
    this.hover = null;
    this.render();
    this.onSelect(this.getRange());
  }

  /* --------------------------- لوحة المفاتيح --------------------------- */
  attachKeyboard(hostEl) {
    const onKey = (e) => {
      const map = {
        ArrowLeft: this.locale === 'ar' ? 1 : -1,     // في RTL السهم الأيسر = اليوم التالي
        ArrowRight: this.locale === 'ar' ? -1 : 1,
        ArrowUp: -7,
        ArrowDown: 7,
      };
      if (map[e.key] !== undefined) {
        e.preventDefault();
        this._moveCursor(map[e.key]);
      } else if (e.key === 'PageUp') {
        e.preventDefault();
        this._shiftMonth(e.shiftKey ? -12 : -1);
      } else if (e.key === 'PageDown') {
        e.preventDefault();
        this._shiftMonth(e.shiftKey ? 12 : 1);
      } else if (e.key === 'Home') {
        e.preventDefault();
        this._moveCursorToWeekEdge('start');
      } else if (e.key === 'End') {
        e.preventDefault();
        this._moveCursorToWeekEdge('end');
      } else if (e.key === 'Enter' || e.key === ' ' || e.key === 'Spacebar') {
        e.preventDefault();
        this._pick(this.cursor);
      }
    };
    (hostEl || this.root).addEventListener('keydown', onKey);
    return () => (hostEl || this.root).removeEventListener('keydown', onKey);
  }

  _moveCursor(days) {
    this.cursor = addCivilDays(this.cursor, days);
    this._ensureVisible();
  }
  _shiftMonth(n) {
    this.cursor = addCivilMonths(this.cursor, n);
    this._ensureVisible();
  }
  _moveCursorToWeekEdge(edge) {
    const wd = weekdayOf(this.cursor);
    const offsetFromStart = ((wd - this.weekStart) + 7) % 7;
    this.cursor = edge === 'start'
      ? addCivilDays(this.cursor, -offsetFromStart)
      : addCivilDays(this.cursor, 6 - offsetFromStart);
    this._ensureVisible();
  }
  _ensureVisible() {
    const cm = this.cursor.slice(0, 7);
    const vm = this.viewMonth.slice(0, 7);
    if (cm !== vm) {
      const diffMonths = (Number(cm.slice(0, 4)) - Number(vm.slice(0, 4))) * 12 +
                         (Number(cm.slice(5, 7)) - Number(vm.slice(5, 7)));
      const shift = this.numberOfMonths > 1
        ? (diffMonths < 0 ? diffMonths : Math.max(0, diffMonths - (this.numberOfMonths - 1)))
        : diffMonths;
      this.viewMonth = addCivilMonths(this.viewMonth, shift);
    }
    this.render();
  }
  _focusCursor() {
    const btn = this.root.querySelector(`.cal-day[data-date="${this.cursor}"]`);
    btn?.focus({ preventScroll: true });
  }
}

/* ========================== Popover + حقل التاريخ ========================= */
/**
 * حقل تاريخ يفتح Popover فيه RangeCalendar.
 * يعرض النص "من → إلى" ويحفظ القيم كـ yyyy-mm-dd.
 */
export function mountDateRangeField(container, { value, onChange, label, locale, align = 'end' } = {}) {
  const L = locale || getLocale();
  container.innerHTML = '';
  container.classList.add('datefield');

  const btn = document.createElement('button');
  btn.type = 'button';
  btn.className = 'datefield-trigger ripple';
  btn.setAttribute('aria-haspopup', 'dialog');
  btn.setAttribute('aria-expanded', 'false');
  btn.innerHTML = `<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><rect x="3" y="5" width="18" height="16" rx="3"/><path d="M3 10h18M8 3v4M16 3v4"/></svg><span class="datefield-text"></span>`;
  const textEl = btn.querySelector('.datefield-text');

  const pop = document.createElement('div');
  pop.className = 'popover';
  pop.setAttribute('role', 'dialog');
  pop.setAttribute('aria-modal', 'false');
  pop.setAttribute('aria-label', label || t('date.title'));
  pop.hidden = true;
  pop.dataset.align = align;

  const calHost = document.createElement('div');
  calHost.className = 'popover-body';
  pop.appendChild(calHost);

  const foot = document.createElement('div');
  foot.className = 'popover-foot';
  const btnClear = document.createElement('button');
  btnClear.type = 'button'; btnClear.className = 'btn ghost ripple'; btnClear.textContent = t('date.clear');
  const btnToday = document.createElement('button');
  btnToday.type = 'button'; btnToday.className = 'btn ghost ripple'; btnToday.textContent = t('date.today');
  const btnApply = document.createElement('button');
  btnApply.type = 'button'; btnApply.className = 'btn primary ripple'; btnApply.textContent = t('date.apply');
  foot.append(btnClear, btnToday, btnApply);
  pop.appendChild(foot);

  container.append(btn, pop);

  let range = value ? { from: value.from || null, to: value.to || null } : { from: null, to: null };
  let draft = { ...range };

  const cal = new RangeCalendar(calHost, {
    locale: L,
    selected: range,
    mode: 'range',
    onSelect: (r) => {
      draft = { from: r.from, to: r.to };
      if (r.from && r.to) renderText(r);
    },
  });
  cal.attachKeyboard(calHost);

  function renderText(r = range) {
    if (!r.from) { textEl.textContent = label || t('tx.dateRange'); btn.classList.remove('has-value'); return; }
    btn.classList.add('has-value');
    textEl.textContent = r.to && r.to !== r.from
      ? `${formatCivilPretty(r.from, L)} → ${formatCivilPretty(r.to, L)}`
      : formatCivilPretty(r.from, L);
  }
  renderText();

  function open() {
    draft = { ...range };
    cal.setSelected(range);
    pop.hidden = false;
    btn.setAttribute('aria-expanded', 'true');
    requestAnimationFrame(() => pop.classList.add('open'));
    document.addEventListener('mousedown', outside, true);
    document.addEventListener('keydown', esc, true);
    setTimeout(() => calHost.querySelector('.cal-day[tabindex="0"]')?.focus(), 30);
  }
  function close() {
    pop.classList.remove('open');
    btn.setAttribute('aria-expanded', 'false');
    setTimeout(() => { pop.hidden = true; }, 160);
    document.removeEventListener('mousedown', outside, true);
    document.removeEventListener('keydown', esc, true);
  }
  function outside(e) { if (!container.contains(e.target)) close(); }
  function esc(e) { if (e.key === 'Escape') { close(); btn.focus(); } }

  btn.addEventListener('click', () => (pop.hidden ? open() : close()));
  btnToday.addEventListener('click', () => {
    const td = todayCivil();
    range = { from: td, to: td }; draft = { ...range };
    cal.setSelected(range); renderText(); close(); onChange?.(range);
  });
  btnClear.addEventListener('click', () => {
    range = { from: null, to: null }; draft = { ...range };
    cal.setSelected(range); renderText(); onChange?.(range);
  });
  btnApply.addEventListener('click', () => {
    range = draft.from ? { from: draft.from, to: draft.to || draft.from } : { from: null, to: null };
    cal.setSelected(range); renderText(); close(); onChange?.(range);
  });

  return {
    setLocale(l) { cal.setLocale(l); renderText(); },
    setValue(v) { range = { from: v?.from || null, to: v?.to || null }; cal.setSelected(range); renderText(); },
    getValue() { return { ...range }; },
    close,
  };
}
