import { hashPassword, pbkdf2Js } from '/home/user/voice-expense-manager/web/js/auth.js';
const saltHex = '000102030405060708090a0b0c0d0e0f';
const web = await hashPassword('sawti2026', saltHex);
const saltBytes = Uint8Array.from(saltHex.match(/.{2}/g).map(b=>parseInt(b,16)));
const js = pbkdf2Js('sawti2026', saltBytes, web.iterations, 32);
const jsHex = Array.from(js).map(b=>b.toString(16).padStart(2,'0')).join('');
console.log('WebCrypto PBKDF2 :', web.digest);
console.log('JS fallback      :', jsHex);
console.log('متطابقان (نفس الخوارزمية) =', web.digest === jsHex);
