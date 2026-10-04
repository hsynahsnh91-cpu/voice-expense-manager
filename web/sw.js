/* Sawti — service worker : تخزين مؤقت للواجهة فقط (بدون أي شبكة إضافية) */
const CACHE = 'sawti-cache-1.0.0-r2';
const ASSETS = [
  './', './index.html', './styles.css', './manifest.json', './icon.svg',
  './js/app.js', './js/auth.js', './js/calendar.js', './js/i18n.js',
  './js/parser.js', './js/speech.js', './js/store.js',
  './icons/icon-192.png', './icons/icon-512.png',
];

self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ASSETS)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;   // لا نتدخل بأي طلب خارجي (محرّك الصوت)
  e.respondWith(
    caches.match(req).then(hit => hit || fetch(req).then(res => {
      const copy = res.clone();
      caches.open(CACHE).then(c => c.put(req, copy)).catch(() => {});
      return res;
    }).catch(() => caches.match('./index.html')))
  );
});
