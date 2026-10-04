package com.abuomar.sawti.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * تشغيل التسجيلات المحفوظة في قسم «الوارد».
 *
 * يعرض حالة التشغيل والتقدّم (0..1) حتى تُرسم الموجة وتتحدّث الشارة.
 * يُحترم خيار الكتم: لا يُشغَّل أي تسجيل تلقائياً بعد الحفظ — التشغيل
 * يتم فقط عندما يضغط المستخدم زر الاستماع بنفسه.
 */
class AudioPlayback(context: Context) {

    private val appContext = context.applicationContext

    private val _playingId = MutableStateFlow<String?>(null)
    val playingId: StateFlow<String?> = _playingId.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private var player: MediaPlayer? = null
    private var ticker: Thread? = null

    val isPlaying: Boolean get() = player?.isPlaying == true

    /**
     * تشغيل ملف.
     * @param id معرّف المصروف (لتحديد العنصر النشط في القائمة)
     * @return true إذا بدأ التشغيل فعلاً
     */
    fun play(id: String, file: File, onFinished: (() -> Unit)? = null): Boolean {
        stop()
        if (!file.exists() || file.length() == 0L) return false
        return try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    cleanup()
                    onFinished?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    cleanup()
                    onFinished?.invoke()
                    true
                }
                prepare()
                start()
            }
            player = mp
            _playingId.value = id
            _progress.value = 0f
            startTicker(mp)
            true
        } catch (e: Throwable) {
            cleanup()
            false
        }
    }

    private fun startTicker(mp: MediaPlayer) {
        ticker?.interrupt()
        ticker = Thread {
            while (player === mp) {
                try {
                    if (mp.isPlaying) {
                        val dur = mp.duration.takeIf { it > 0 } ?: 1
                        _progress.value = (mp.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
                    }
                    Thread.sleep(90)
                } catch (e: Throwable) {
                    return@Thread
                }
            }
        }.apply { isDaemon = true; start() }
    }

    fun pause() {
        runCatching { player?.takeIf { it.isPlaying }?.pause() }
    }

    fun resume() {
        runCatching { player?.takeIf { !it.isPlaying }?.start() }
    }

    fun stop() {
        ticker?.interrupt()
        ticker = null
        runCatching { player?.takeIf { it.isPlaying }?.stop() }
        cleanup()
    }

    private fun cleanup() {
        runCatching { player?.release() }
        player = null
        _playingId.value = null
        _progress.value = 0f
    }

    fun release() {
        stop()
    }

    companion object {
        fun audioDir(context: Context): File =
            File(context.filesDir, "sawti/audio").apply { mkdirs() }
    }
}
