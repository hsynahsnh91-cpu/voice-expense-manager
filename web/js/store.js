/* =============================================================================
 *  Sawti — store.js : التخزين المحلي الحقيقي (بدون أي بيانات وهمية/تجريبية)
 *  ---------------------------------------------------------------------------
 *  • IndexedDB  → التسجيلات الصوتية (Blobs) لأنها كبيرة
 *  • localStorage → الحسابات (بصمة PBKDF2)، المصاريف، التفضيلات
 *  لا يوجد سيرفر ولا seed data: أول ما تفتح التطبيق يكون فاضياً تماماً.
 * ========================================================================== */

const DB_NAME = 'sawti-db';
const DB_VERSION = 1;
const STORE_AUDIO = 'audio';
const LS = {
  users: 'sawti.users.v1',
  session: 'sawti.session.v1',
  tx: 'sawti.transactions.v1',
  prefs: 'sawti.prefs.v1',
};

/* ------------------------------- IndexedDB -------------------------------- */
let dbPromise = null;
function db() {
  if (dbPromise) return dbPromise;
  /* إذا ما في IndexedDB (سياق معزول) نرفض فوراً بدل ما نعلّق للأبد */
  if (typeof indexedDB === 'undefined') {
    dbPromise = Promise.reject(new Error('indexeddb-unavailable'));
    return dbPromise;
  }
  dbPromise = new Promise((resolve, reject) => {
    /* مهلة ٣ ثوانٍ: فتح معلّق ما لازم يحبس التطبيق */
    const timer = setTimeout(() => reject(new Error('indexeddb-timeout')), 3000);
    const done = (fn, val) => { clearTimeout(timer); fn(val); };
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const d = req.result;
      if (!d.objectStoreNames.contains(STORE_AUDIO)) {
        const s = d.createObjectStore(STORE_AUDIO);      // key = transactionId
      }
    };
    req.onsuccess = () => done(resolve, req.result);
    req.onerror = () => done(reject, req.error);
    req.onblocked = () => done(reject, new Error('indexeddb-blocked'));
  });
  /* الفشل يُخزَّن حتى لا نعيد المحاولة بلا نهاية */
  dbPromise.catch(() => {});
  return dbPromise;
}

export async function putAudio(id, blob) {
  const d = await db();
  return new Promise((res, rej) => {
    const tx = d.transaction(STORE_AUDIO, 'readwrite');
    tx.objectStore(STORE_AUDIO).put(blob, id);
    tx.oncomplete = () => res(true);
    tx.onerror = () => rej(tx.error);
  });
}
export async function getAudio(id) {
  const d = await db();
  return new Promise((res, rej) => {
    const tx = d.transaction(STORE_AUDIO, 'readonly');
    const r = tx.objectStore(STORE_AUDIO).get(id);
    r.onsuccess = () => res(r.result || null);
    r.onerror = () => rej(r.error);
  });
}
export async function delAudio(id) {
  const d = await db();
  return new Promise((res, rej) => {
    const tx = d.transaction(STORE_AUDIO, 'readwrite');
    tx.objectStore(STORE_AUDIO).delete(id);
    tx.oncomplete = () => res(true);
    tx.onerror = () => rej(tx.error);
  });
}
export async function clearAllAudio() {
  const d = await db();
  return new Promise((res, rej) => {
    const tx = d.transaction(STORE_AUDIO, 'readwrite');
    tx.objectStore(STORE_AUDIO).clear();
    tx.oncomplete = () => res(true);
    tx.onerror = () => rej(tx.error);
  });
}
export async function audioStorageEstimate() {
  if (navigator.storage?.estimate) {
    try {
      const e = await navigator.storage.estimate();
      return { usage: e.usage || 0, quota: e.quota || 0 };
    } catch { /* ignore */ }
  }
  return { usage: 0, quota: 0 };
}

/* ------------------------------ localStorage ------------------------------ */
function readJSON(key, fallback) {
  try {
    const raw = localStorage.getItem(key) ?? memFallback.get(key) ?? null;
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    try {
      const m = memFallback.get(key);
      return m ? JSON.parse(m) : fallback;
    } catch { return fallback; }
  }
}
/* ذاكرة احتياطية داخل الجلسة: إذا كان localStorage محجوباً (iframe معزول
   أو وضع خاص) يبقى التطبيق شغالاً بدل أن ينهار عند أول حفظ. */
const memFallback = new Map();

function writeJSON(key, value) {
  const json = JSON.stringify(value);
  try {
    localStorage.setItem(key, json);
  } catch {
    memFallback.set(key, json);
  }
  notify(key);
}

const subs = new Set();
function notify() { subs.forEach(fn => fn()); }
export function subscribe(fn) { subs.add(fn); return () => subs.delete(fn); }

/* -------------------------------- الحسابات -------------------------------- */
export function getUsers() { return readJSON(LS.users, []); }
export function saveUsers(u) { writeJSON(LS.users, u); }
export function getSession() { return readJSON(LS.session, null); }
export function setSession(s) { writeJSON(LS.session, s); }
export function clearSession() { localStorage.removeItem(LS.session); notify(); }

/* ------------------------------- المصاريف --------------------------------- */
/** @returns {Array} كل المصاريف لكل المستخدمين: {ownerEmail, ...tx} */
export function getAllTransactions() { return readJSON(LS.tx, []); }
export function saveAllTransactions(list) { writeJSON(LS.tx, list); }

export function getTransactions(email) {
  return getAllTransactions()
    .filter(t => t.ownerEmail === email)
    .sort((a, b) => (b.date === a.date ? b.createdAt - a.createdAt : (a.date < b.date ? 1 : -1)));
}

export function addTransaction(email, tx) {
  const all = getAllTransactions();
  const record = {
    id: 'tx_' + Date.now().toString(36) + Math.random().toString(36).slice(2, 7),
    ownerEmail: email,
    createdAt: Date.now(),
    amount: 0,              // بالليرة السورية الجديدة دائماً
    currency: 'LSN',
    type: 'expense',        // expense | income | debt
    category: 'other',
    date: new Date().toISOString().slice(0, 10),
    merchant: null,
    note: '',
    transcript: null,
    hasAudio: false,
    audioDuration: 0,
    listened: false,
    muted: false,
    ...tx,
  };
  all.push(record);
  saveAllTransactions(all);
  return record;
}

export function updateTransaction(id, patch) {
  const all = getAllTransactions();
  const i = all.findIndex(t => t.id === id);
  if (i === -1) return null;
  all[i] = { ...all[i], ...patch };
  saveAllTransactions(all);
  return all[i];
}

export async function deleteTransaction(id) {
  const all = getAllTransactions();
  const i = all.findIndex(t => t.id === id);
  if (i === -1) return false;
  all.splice(i, 1);
  saveAllTransactions(all);
  try { await delAudio(id); } catch { /* ignore */ }
  return true;
}

export async function clearTransactions(email) {
  const all = getAllTransactions().filter(t => t.ownerEmail === email);
  for (const t of all) { try { await delAudio(t.id); } catch { /* ignore */ } }
  saveAllTransactions(getAllTransactions().filter(t => t.ownerEmail !== email));
}

/* ------------------------------- التفضيلات -------------------------------- */
export const DEFAULT_PREFS = {
  locale: 'ar',
  theme: 'system',            // light | dark | system
  monthlyBudget: 0,           // بالليرة السورية الجديدة
  budgetCurrency: 'LSN',      // LSN (جديدة) | SYP-legacy (قديمة)
  displayUnit: 'new',         // new | legacy
  mutePlaybackAfterRecord: true,   // كتم إعادة الصوت بعد التسجيل
  speakResponses: true,            // نطق ردود المساعد
  voiceRate: 1.0,
  voicePitch: 1.0,
  speechLang: null,           // null = حسب لغة الواجهة
  onboarded: false,
};

export function getPrefs() { return { ...DEFAULT_PREFS, ...readJSON(LS.prefs, {}) }; }
export function savePrefs(patch) {
  const next = { ...getPrefs(), ...patch };
  writeJSON(LS.prefs, next);
  return next;
}

/* ------------------------------ تصدير/استيراد ----------------------------- */
export async function exportData(email) {
  const users = getUsers().filter(u => u.email === email).map(u => ({ ...u, salt: undefined, digest: undefined }));
  const txs = getTransactions(email);
  const payload = {
    app: 'sawti',
    version: '1.0.0',
    exportedAt: new Date().toISOString(),
    currency: 'LSN (New Syrian Pound)',
    prefs: getPrefs(),
    profile: users[0] || null,
    transactions: txs,
    audioClips: txs.filter(t => t.hasAudio).length,
  };
  // نضيف الصوت كـ base64 حتى تكون النسخة الاحتياطية كاملة
  payload.audio = [];
  for (const t of txs.filter(x => x.hasAudio)) {
    const blob = await getAudio(t.id);
    if (blob) payload.audio.push({ id: t.id, type: blob.type, base64: await blobToBase64(blob) });
  }
  return payload;
}

export async function importData(email, payload) {
  if (!payload || payload.app !== 'sawti' || !Array.isArray(payload.transactions)) {
    throw new Error('invalid-file');
  }
  const all = getAllTransactions().filter(t => t.ownerEmail !== email);
  for (const t of payload.transactions) {
    const { ownerEmail, ...rest } = t;
    all.push({ ...rest, ownerEmail: email });
  }
  saveAllTransactions(all);
  if (payload.prefs) savePrefs(payload.prefs);
  if (Array.isArray(payload.audio)) {
    for (const a of payload.audio) {
      try { await putAudio(a.id, base64ToBlob(a.base64, a.type)); } catch { /* ignore */ }
    }
  }
  return payload.transactions.length;
}

export function blobToBase64(blob) {
  return new Promise((res, rej) => {
    const r = new FileReader();
    r.onload = () => res(String(r.result).split(',')[1]);
    r.onerror = () => rej(r.error);
    r.readAsDataURL(blob);
  });
}
export function base64ToBlob(b64, type = 'audio/webm') {
  const bin = atob(b64);
  const arr = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i);
  return new Blob([arr], { type });
}

/* ------------------------------ حذف كل شي --------------------------------- */
export async function wipeEverything(email) {
  await clearTransactions(email);
  const users = getUsers().filter(u => u.email !== email);
  saveUsers(users);
  clearSession();
  localStorage.removeItem(LS.prefs);
  notify();
}
