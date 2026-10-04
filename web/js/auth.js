/* =============================================================================
 *  Sawti — auth.js : تسجيل دخول/إنشاء حساب حقيقي (لا حسابات تجريبية إطلاقاً)
 *  ---------------------------------------------------------------------------
 *  • كلمة المرور لا تُحفظ أبداً: نحسب PBKDF2-SHA256 بـ 150,000 دورة + ملح عشوائي
 *  • لا يمكن الدخول بأي بريد/كلمة مرور عشوائية — لازم إنشاء حساب أولاً
 *  • إن كان Web Crypto غير متاح (سياق غير آمن) نستخدم تنفيذ SHA-256/PBKDF2
 *    مكتوب يدوياً — نفس الخوارزمية، نفس النتيجة.
 * ========================================================================== */

import { getUsers, saveUsers, getSession, setSession, clearSession } from './store.js';

const ITERATIONS = 150000;
const KEY_BITS = 256;

/* ------------------------- SHA-256 احتياطي (JS صرف) ------------------------ */
const K256 = new Uint32Array([
  0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
  0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
  0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
  0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
  0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
  0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
  0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
  0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2,
]);
function rotr(x, n) { return (x >>> n) | (x << (32 - n)); }

function sha256Bytes(msg) {
  const H = new Uint32Array([0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19]);
  const l = msg.length;
  const withPad = new Uint8Array((((l + 9) >> 6) + 1) << 6);
  withPad.set(msg);
  withPad[l] = 0x80;
  const dv = new DataView(withPad.buffer);
  dv.setUint32(withPad.length - 4, (l * 8) >>> 0, false);
  dv.setUint32(withPad.length - 8, Math.floor((l * 8) / 4294967296), false);
  const w = new Uint32Array(64);
  for (let off = 0; off < withPad.length; off += 64) {
    for (let i = 0; i < 16; i++) w[i] = dv.getUint32(off + i * 4, false);
    for (let i = 16; i < 64; i++) {
      const s0 = rotr(w[i-15],7) ^ rotr(w[i-15],18) ^ (w[i-15] >>> 3);
      const s1 = rotr(w[i-2],17) ^ rotr(w[i-2],19) ^ (w[i-2] >>> 10);
      w[i] = (w[i-16] + s0 + w[i-7] + s1) >>> 0;
    }
    let [a,b,c,d,e,f,g,h] = H;
    for (let i = 0; i < 64; i++) {
      const S1 = rotr(e,6) ^ rotr(e,11) ^ rotr(e,25);
      const ch = (e & f) ^ (~e & g);
      const t1 = (h + S1 + ch + K256[i] + w[i]) >>> 0;
      const S0 = rotr(a,2) ^ rotr(a,13) ^ rotr(a,22);
      const maj = (a & b) ^ (a & c) ^ (b & c);
      const t2 = (S0 + maj) >>> 0;
      h=g; g=f; f=e; e=(d + t1) >>> 0; d=c; c=b; b=a; a=(t1 + t2) >>> 0;
    }
    H[0]=(H[0]+a)>>>0; H[1]=(H[1]+b)>>>0; H[2]=(H[2]+c)>>>0; H[3]=(H[3]+d)>>>0;
    H[4]=(H[4]+e)>>>0; H[5]=(H[5]+f)>>>0; H[6]=(H[6]+g)>>>0; H[7]=(H[7]+h)>>>0;
  }
  const out = new Uint8Array(32);
  const odv = new DataView(out.buffer);
  for (let i = 0; i < 8; i++) odv.setUint32(i * 4, H[i], false);
  return out;
}

/** HMAC-SHA256 (RFC 2104) — مطلوب لأن PBKDF2 مبني على HMAC وليس SHA-256 مباشرة */
function hmacSha256(key, msg) {
  let k = key.length > 64 ? sha256Bytes(key) : key;
  const kp = new Uint8Array(64); kp.set(k);
  const inner = new Uint8Array(64 + msg.length);
  for (let i = 0; i < 64; i++) inner[i] = kp[i] ^ 0x36;
  inner.set(msg, 64);
  const ih = sha256Bytes(inner);
  const outer = new Uint8Array(96);
  for (let i = 0; i < 64; i++) outer[i] = kp[i] ^ 0x5c;
  outer.set(ih, 64);
  return sha256Bytes(outer);
}

/**
 * PBKDF2-HMAC-SHA256 (RFC 8018) — تنفيذ احتياطي مطابق تماماً لـ WebCrypto،
 * يُستخدم فقط عندما لا يتوفر crypto.subtle (سياق غير آمن).
 */
export function pbkdf2Js(password, salt, iterations, keyLen) {
  const enc = new TextEncoder();
  const P = enc.encode(password);
  const block = new Uint8Array(salt.length + 4);
  block.set(salt, 0);
  const bdv = new DataView(block.buffer);
  const out = new Uint8Array(keyLen);
  let pos = 0, idx = 1;
  while (pos < keyLen) {
    bdv.setUint32(salt.length, idx, false);        // INT_32_BE(i)
    let U = hmacSha256(P, block);                  // U1 = PRF(P, S || INT(i))
    const T = Uint8Array.from(U);
    for (let j = 1; j < iterations; j++) {
      U = hmacSha256(P, U);                        // Uc = PRF(P, Uc-1)
      for (let k2 = 0; k2 < T.length; k2++) T[k2] ^= U[k2];
    }
    const take = Math.min(32, keyLen - pos);
    out.set(T.subarray(0, take), pos);
    pos += take; idx++;
  }
  return out;
}

/* ------------------------------- الواجهة ---------------------------------- */
const subtle = (globalThis.crypto && globalThis.crypto.subtle) || null;
export const cryptoAvailable = !!subtle;

const toHex = arr => Array.from(arr).map(b => b.toString(16).padStart(2, '0')).join('');
const fromHex = hex => new Uint8Array(hex.match(/.{2}/g).map(b => parseInt(b, 16)));

export async function hashPassword(password, saltHex) {
  const enc = new TextEncoder();
  const salt = saltHex ? fromHex(saltHex) : globalThis.crypto.getRandomValues(new Uint8Array(16));
  let digest;
  if (subtle) {
    const key = await subtle.importKey('raw', enc.encode(password), 'PBKDF2', false, ['deriveBits']);
    const bits = await subtle.deriveBits(
      { name: 'PBKDF2', salt, iterations: ITERATIONS, hash: 'SHA-256' }, key, KEY_BITS);
    digest = new Uint8Array(bits);
  } else {
    digest = pbkdf2Js(password, salt, ITERATIONS, KEY_BITS / 8);
  }
  return { salt: toHex(salt), digest: toHex(digest), iterations: ITERATIONS, alg: 'PBKDF2-SHA256' };
}

/* ------------------------------ التحقق من المدخلات ------------------------ */
export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[a-zA-Z\u0621-\u064A]{2,}$/;

export function validateEmail(v) {
  const s = String(v || '').trim().toLowerCase();
  return EMAIL_RE.test(s) ? { ok: true, value: s } : { ok: false, errorKey: 'auth.err.emailInvalid' };
}
export function validatePassword(v) {
  const s = String(v || '');
  if (s.length < 8) return { ok: false, errorKey: 'auth.err.passwordShort' };
  if (!/[A-Za-z\u0621-\u064A]/.test(s) || !/\d/.test(s)) return { ok: false, errorKey: 'auth.err.passwordWeak' };
  return { ok: true, value: s };
}
export function validateName(v) {
  const s = String(v || '').trim();
  return s.length >= 2 ? { ok: true, value: s } : { ok: false, errorKey: 'auth.err.nameRequired' };
}

/* -------------------------------- العمليات -------------------------------- */
export function findUser(email) {
  return getUsers().find(u => u.email === String(email).trim().toLowerCase()) || null;
}

/** إنشاء حساب جديد — يرفض أي بريد مسجّل مسبقاً */
export async function signUp({ name, email, password }) {
  const n = validateName(name); if (!n.ok) throw new Error(n.errorKey);
  const e = validateEmail(email); if (!e.ok) throw new Error(e.errorKey);
  const p = validatePassword(password); if (!p.ok) throw new Error(p.errorKey);
  if (findUser(e.value)) throw new Error('auth.err.emailTaken');

  const { salt, digest, iterations, alg } = await hashPassword(p.value, null);
  const user = {
    email: e.value,
    name: n.value,
    salt, digest, iterations, alg,
    createdAt: Date.now(),
    provider: 'password',
  };
  const users = getUsers();
  users.push(user);
  saveUsers(users);
  setSession({ email: user.email, name: user.name, since: Date.now() });
  return user;
}

/** تسجيل الدخول — يتحقق فعلياً من البصمة، لا يدخل بأي بريد وكلمة مرور */
export async function signIn({ email, password }) {
  const e = validateEmail(email); if (!e.ok) throw new Error(e.errorKey);
  const user = findUser(e.value);
  if (!user) throw new Error('auth.err.emailNotFound');
  if (!password) throw new Error('auth.err.passwordWrong');

  const { digest } = await hashPassword(password, user.salt);
  if (digest !== user.digest) throw new Error('auth.err.passwordWrong');

  setSession({ email: user.email, name: user.name, since: Date.now() });
  return user;
}

export function signOut() { clearSession(); }
export function currentUser() {
  const s = getSession();
  if (!s) return null;
  const u = findUser(s.email);
  return u ? { email: u.email, name: u.name } : null;
}
export async function changePassword(email, oldPassword, newPassword) {
  const user = findUser(email);
  if (!user) throw new Error('auth.err.emailNotFound');
  const { digest } = await hashPassword(oldPassword, user.salt);
  if (digest !== user.digest) throw new Error('auth.err.passwordWrong');
  const p = validatePassword(newPassword); if (!p.ok) throw new Error(p.errorKey);
  const h = await hashPassword(p.value, null);
  const users = getUsers();
  const i = users.findIndex(u => u.email === user.email);
  users[i] = { ...users[i], ...h };
  saveUsers(users);
  return true;
}
