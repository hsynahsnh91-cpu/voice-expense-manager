/* =============================================================================
 *  Sawti — speech.js : محرك صوت حقيقي (ليس محاكاة ولا شكل فقط)
 *  ---------------------------------------------------------------------------
 *  ١) التعرّف على الكلام : Web Speech API (SpeechRecognition) — المحرك الحقيقي
 *     الموجود في النظام/المتصفح (في Chrome/Edge هو محرّك Google للكلام).
 *     على أندرويد نفس المنطق يستخدم SpeechRecognizer الحقيقي.
 *  ٢) تسجيل الصوت : MediaRecorder على نفس الميكروفون — يُحفظ ملف فعلي
 *     في IndexedDB حتى يرجع المستخدم يسمعه من قسم «الوارد».
 *  ٣) نطق الردود : SpeechSynthesis مع اختيار أفضل صوت عربي متاح (صوت واقعي).
 *  لا يوجد أي نص وهمي ولا نتائج مُصنّعة: كل ما يظهر هو ما قيل فعلاً.
 * ========================================================================== */

import { LOCALES, getLocale } from './i18n.js';

const SR = globalThis.SpeechRecognition || globalThis.webkitSpeechRecognition || null;

export const speechSupport = {
  recognition: !!SR,
  recording: !!(globalThis.MediaRecorder && navigator.mediaDevices?.getUserMedia),
  synthesis: !!globalThis.speechSynthesis,
};

export function engineName() {
  if (!SR) return null;
  return (SR === globalThis.webkitSpeechRecognition) ? 'Web Speech (Google)' : 'Web Speech';
}

/* ---------------------------- إذن الميكروفون ------------------------------ */
export async function ensureMicPermission() {
  if (!navigator.mediaDevices?.getUserMedia) {
    const err = new Error('unsupported'); err.code = 'unsupported'; throw err;
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
    });
    stream.getTracks().forEach(t => t.stop());     // نفتح القفل فقط ثم نحرر
    return true;
  } catch (e) {
    const err = new Error(e.name === 'NotAllowedError' || e.name === 'SecurityError' ? 'denied'
      : e.name === 'NotFoundError' ? 'no-device'
      : e.name === 'NotReadableError' ? 'busy' : 'denied');
    err.code = err.message; err.raw = e.name;
    throw err;
  }
}

/** يقرأ حالة الإذن الحالية (إن كانت مدعومة) */
export async function micPermissionState() {
  try {
    if (navigator.permissions?.query) {
      /* مهلة ١.٢ ثانية: بعض السياقات المعزولة تعلّق الاستعلام */
      const s = await Promise.race([
        navigator.permissions.query({ name: 'microphone' }),
        new Promise((_, rej) => setTimeout(() => rej(new Error('permission-query-timeout')), 1200)),
      ]);
      return s.state;               // granted | denied | prompt
    }
  } catch { /* غير مدعوم أو معلق */ }
  return 'unknown';
}

/* --------------------------- لغة التعرّف المطلوبة -------------------------- */
export function speechLangPref() {
  const L = LOCALES[getLocale()] || LOCALES.ar;
  return L.speechTags;              // ['ar-SY','ar-LB','ar-JO','ar'] أو ['en-US',...]
}

export function availableRecognitionLangs() {
  // المتصفح لا يعرض قائمة اللغات؛ نُرجع المفضّلة المتاحة لدينا
  return speechLangPref();
}

/* ============================== جلسة التعرّف ============================== */
/**
 * يبدأ التعرّف على الكلام + تسجيل الصوت في نفس الوقت.
 * @param {Object} opts
 *  - onPartial(text)      : النص المؤقت لحظة بلحظة (يُكتب في خانة الإرسال فوراً)
 *  - onFinal(text)        : نص نهائي مؤكَّد
 *  - onLevel(0..1)        : مستوى الصوت لرسم الموجات
 *  - onStateChange(state) : idle|requesting|listening|processing|error
 *  - onError(code)        : رمز الخطأ
 *  - onAudioReady(blob)   : ملف التسجيل الكامل
 *  - lang                 : لغة اختيارية تتجاوز لغة الواجهة
 */
export class VoiceSession {
  constructor(opts = {}) {
    this.opts = opts;
    this.state = 'idle';
    this.recognition = null;
    this.recorder = null;
    this.stream = null;
    this.chunks = [];
    this.analyser = null;
    this.rafId = null;
    this.finalText = '';
    this.partialText = '';
    this.startedAt = 0;
    this.aborted = false;
    this.autoStopTimer = null;
  }

  setState(s) { this.state = s; this.opts.onStateChange?.(s); }

  async start({ lang, maxSeconds = 60, recordAudio = true } = {}) {
    this.aborted = false;
    this.finalText = '';
    this.partialText = '';
    this.chunks = [];
    this.setState('requesting');

    const tags = lang ? [lang, ...speechLangPref()] : speechLangPref();

    /* --- ١) فتح الميكروفون فعلياً (يمنح الإذن ويعطينا stream للتسجيل) --- */
    if (recordAudio && speechSupport.recording) {
      try {
        this.stream = await navigator.mediaDevices.getUserMedia({
          audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
        });
        this._startRecorder();
        this._startLevelMeter();
      } catch (e) {
        const code = e.name === 'NotAllowedError' || e.name === 'SecurityError' ? 'denied'
          : e.name === 'NotReadableError' ? 'busy'
          : e.name === 'NotFoundError' ? 'no-device' : 'mic-error';
        this.setState('error');
        this.opts.onError?.(code, e);
        // نتابع للتعرف الصوتي وحده (قد ينجح بدون MediaRecorder)
      }
    }

    /* --- ٢) محرك التعرّف الحقيقي --- */
    if (!SR) {
      this.setState('error');
      this.opts.onError?.('unsupported');
      this.stopRecordingOnly();
      return;
    }

    const rec = new SR();
    this.recognition = rec;
    rec.continuous = true;
    rec.interimResults = true;
    rec.maxAlternatives = 3;

    // نجرب اللغات بالترتيب: ar-SY ثم ar-LB ثم ar (العامية الشامية مفهومة فيها)
    let langIdx = 0;
    const applyLang = () => { rec.lang = tags[Math.min(langIdx, tags.length - 1)]; };
    applyLang();

    rec.onstart = () => {
      this.startedAt = Date.now();
      this.setState('listening');
      this.opts.onEngine?.(rec.lang);
      // إيقاف أمان: لا نترك الميكروفون مفتوحاً للأبد
      this.autoStopTimer = setTimeout(() => this.stop(), maxSeconds * 1000);
    };

    rec.onaudiostart = () => { if (this.state !== 'listening') this.setState('listening'); };

    rec.onresult = (ev) => {
      let interim = '';
      for (let i = ev.resultIndex; i < ev.results.length; i++) {
        const r = ev.results[i];
        const txt = (r[0]?.transcript || '').trim();
        if (!txt) continue;
        if (r.isFinal) {
          this.finalText += (this.finalText ? ' ' : '') + txt;
          this.opts.onFinal?.(txt, this.finalText);
        } else {
          interim += (interim ? ' ' : '') + txt;
        }
      }
      this.partialText = interim;
      const combined = (this.finalText + (interim ? ' ' + interim : '')).trim();
      this.opts.onPartial?.(combined, { final: this.finalText, interim });
    };

    rec.onerror = (ev) => {
      const code = ev.error || 'unknown';
      // no-speech: نتابع (قد يتكلم بعدها) — لا نُفشل الجلسة
      if (code === 'no-speech' || code === 'aborted') {
        if (code === 'aborted' && this.aborted) return;
        this.opts.onError?.(code, ev);
        if (code === 'no-speech') return;
      }
      // لغة غير مدعومة → جرّب اللغة التالية في القائمة
      if (code === 'language-not-supported' || code === 'not-allowed') {
        if (code === 'language-not-supported' && langIdx < tags.length - 1) {
          langIdx++; applyLang();
          try { rec.start(); } catch { /* ignore */ }
          return;
        }
      }
      this.setState('error');
      this.opts.onError?.(code, ev);
      this.stopRecordingOnly();
    };

    rec.onend = () => {
      if (this.aborted) { this.stopRecordingOnly(); return; }
      // أحياناً يقطع المحرك من حاله — نُعيد التشغيل إذا ما وصلنا نص وللسه الجلسة شغّالة
      const elapsed = Date.now() - this.startedAt;
      if (!this.finalText && elapsed < 1500 && langIdx < tags.length - 1) {
        langIdx++; applyLang();
        try { rec.start(); } catch { this.finish(); }
        return;
      }
      this.finish();
    };

    try {
      rec.start();
    } catch (e) {
      this.setState('error');
      this.opts.onError?.('start-failed', e);
      this.stopRecordingOnly();
    }
  }

  /* ---------------------------- تسجيل الصوت ---------------------------- */
  _startRecorder() {
    if (!this.stream) return;
    const mime = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4', 'audio/ogg;codecs=opus', 'audio/mpeg']
      .find(m => globalThis.MediaRecorder.isTypeSupported?.(m));
    try {
      this.recorder = new MediaRecorder(this.stream, mime ? { mimeType: mime, audioBitsPerSecond: 64000 } : undefined);
    } catch {
      this.recorder = new MediaRecorder(this.stream);
    }
    this.recorder.ondataavailable = (e) => { if (e.data && e.data.size) this.chunks.push(e.data); };
    this.recorder.onstop = () => {
      const blob = new Blob(this.chunks, { type: this.recorder.mimeType || mime || 'audio/webm' });
      this.stream?.getTracks().forEach(t => t.stop());
      this.stream = null;
      if (blob.size > 0) {
        this.opts.onAudioReady?.(blob, (Date.now() - this.startedAt) / 1000);
      }
    };
    this.recorder.start(250);
  }

  _startLevelMeter() {
    if (!this.stream || !globalThis.AudioContext) return;
    try {
      const ctx = new AudioContext();
      this._ctx = ctx;
      const src = ctx.createMediaStreamSource(this.stream);
      const analyser = ctx.createAnalyser();
      analyser.fftSize = 512;
      src.connect(analyser);
      this.analyser = analyser;
      const buf = new Uint8Array(analyser.frequencyBinCount);
      const tick = () => {
        if (!this.analyser) return;
        analyser.getByteTimeDomainData(buf);
        let sum = 0;
        for (let i = 0; i < buf.length; i++) { const v = (buf[i] - 128) / 128; sum += v * v; }
        this.opts.onLevel?.(Math.min(1, Math.sqrt(sum / buf.length) * 3.2));
        this.rafId = requestAnimationFrame(tick);
      };
      tick();
    } catch { /* غير مدعوم — لا يؤثر على التسجيل */ }
  }

  stopRecordingOnly() {
    if (this.autoStopTimer) { clearTimeout(this.autoStopTimer); this.autoStopTimer = null; }
    if (this.rafId) cancelAnimationFrame(this.rafId), this.rafId = null;
    this.analyser = null;
    try { this._ctx?.close(); } catch { /* ignore */ }
    this._ctx = null;
    if (this.recorder && this.recorder.state !== 'inactive') {
      try { this.recorder.stop(); } catch { /* ignore */ }
    } else if (this.stream) {
      this.stream.getTracks().forEach(t => t.stop());
      this.stream = null;
    }
  }

  /** إيقاف طبيعي: يُنهي التعرّف ويُغلق التسجيل ويرجع النص */
  stop() {
    this.setState('processing');
    try { this.recognition?.stop(); } catch { /* ignore */ }
    this.stopRecordingOnly();
    return this.getTranscript();
  }

  /** إلغاء: يتجاهل كل شي */
  cancel() {
    this.aborted = true;
    try { this.recognition?.abort(); } catch { /* ignore */ }
    this.chunks = [];
    this.stopRecordingOnly();
    this.setState('idle');
  }

  finish() {
    this.stopRecordingOnly();
    const text = this.getTranscript();
    this.setState('idle');
    this.opts.onDone?.(text);
  }

  getTranscript() {
    return (this.finalText + (this.partialText ? ' ' + this.partialText : '')).trim();
  }
}

/* ================================ النطق (TTS) ============================= */
let cachedVoices = [];
function loadVoices() {
  if (!speechSupport.synthesis) return [];
  const v = speechSynthesis.getVoices();
  if (v?.length) cachedVoices = v;
  return cachedVoices;
}
if (speechSupport.synthesis) {
  loadVoices();
  speechSynthesis.onvoiceschanged = () => { loadVoices(); };
}

/**
 * يختار أفضل صوت عربي متاح — الأولوية:
 * ar-SY > ar-LB/ar-JO/ar-SA/ar-EG > أي ar، ويُفضّل أصوات Google (أنظف وأقرب للواقع)
 */
export function pickVoice(langOverride) {
  const voices = loadVoices();
  if (!voices.length) return null;
  const wantAr = !langOverride || langOverride.startsWith('ar') || getLocale() === 'ar';
  const pool = wantAr ? voices.filter(v => /^ar/i.test(v.lang)) : voices.filter(v => /^en/i.test(v.lang));
  const list = pool.length ? pool : voices;
  const score = v => {
    let s = 0;
    const l = (v.lang || '').toLowerCase();
    if (l === 'ar-sy') s += 100;
    else if (l.startsWith('ar-lb') || l.startsWith('ar-jo')) s += 80;
    else if (l.startsWith('ar-sa') || l.startsWith('ar-eg')) s += 60;
    else if (l.startsWith('ar')) s += 40;
    if (/google/i.test(v.name)) s += 30;              // أصوات Google أوضح
    if (/natural|neural|enhanced|premium/i.test(v.name)) s += 25;
    if (v.localService === false) s += 8;             // أصوات الشبكة أعلى جودة عادةً
    if (v.default) s += 5;
    return s;
  };
  return [...list].sort((a, b) => score(b) - score(a))[0] || null;
}

export function voiceInfo() {
  const v = pickVoice();
  return v ? { name: v.name, lang: v.lang, local: v.localService } : null;
}

/**
 * ينطق جملة بصوت واقعي. يُحترم إعداد «كتم إعادة الصوت بعد التسجيل»:
 * إذا كان mutePlayback مفعلاً فلا يُنطق كلام المستخدم أبداً.
 */
export function speak(text, { rate = 1, pitch = 1, lang, allow = true } = {}) {
  if (!speechSupport.synthesis || !allow || !text) return Promise.resolve(false);
  return new Promise((resolve) => {
    try {
      speechSynthesis.cancel();                       // لا نراكِم الجمل
      const u = new SpeechSynthesisUtterance(String(text));
      const v = pickVoice(lang);
      if (v) u.voice = v;
      u.lang = v?.lang || lang || (getLocale() === 'ar' ? 'ar-SY' : 'en-US');
      // نطق أبطأ قليلاً وطبقة طبيعية = أقرب للكلام الحقيقي
      u.rate = Math.max(0.5, Math.min(2, Number(rate) * 0.97));
      u.pitch = Math.max(0.5, Math.min(1.6, Number(pitch)));
      u.volume = 1;
      let done = false;
      const fin = () => { if (!done) { done = true; resolve(true); } };
      u.onend = fin; u.onerror = fin;
      setTimeout(fin, 20000);
      speechSynthesis.speak(u);
    } catch { resolve(false); }
  });
}

export function stopSpeaking() {
  try { speechSynthesis?.cancel(); } catch { /* ignore */ }
}

/* ========================= تشغيل تسجيل محفوظ (الوارد) ===================== */
let currentAudio = null;
export async function playAudioBlob(blob, { onEnd, onProgress } = {}) {
  stopAudio();
  const url = URL.createObjectURL(blob);
  const a = new Audio(url);
  currentAudio = { el: a, url };
  a.onended = () => { onEnd?.(); cleanup(); };
  a.onerror = () => { onEnd?.(true); cleanup(); };
  if (onProgress) {
    a.ontimeupdate = () => onProgress(a.currentTime, a.duration || 0);
  }
  try {
    await a.play();
    return true;
  } catch (e) {
    cleanup();
    onEnd?.(true);
    return false;
  }
  function cleanup() {
    if (currentAudio?.url === url) currentAudio = null;
    setTimeout(() => URL.revokeObjectURL(url), 1500);
  }
}
export function stopAudio() {
  if (currentAudio) {
    try { currentAudio.el.pause(); } catch { /* ignore */ }
    try { URL.revokeObjectURL(currentAudio.url); } catch { /* ignore */ }
    currentAudio = null;
  }
}
export function isPlaying() { return !!currentAudio && !currentAudio.el.paused; }
