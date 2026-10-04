package com.abuomar.sawti.speech

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * تسجيل المقطع الصوتي الحقيقي من الميكروفون (MediaRecorder / AAC داخل MP4).
 *
 * يُحفظ الملف في المساحة الخاصة بالتطبيق:  filesDir/sawti/audio/<id>.m4a
 * ولا نطلب أي إذن تخزين خارجي.
 *
 * ملاحظة هندسية مهمة:
 * على بعض الأجهزة يحتكر محرّك التعرّف الصوتي الميكروفون، فيخرج التسجيل صامتاً.
 * لذلك نقيس أعلى سعة صوتية (getMaxAmplitude) أثناء الجلسة؛ إذا بقيت شبه صفرية
 * نعتبر أن الالتقاط المتوازي غير مدعوم على هذا الجهاز، نحذف الملف الصامت،
 * ونحفظ العلم في التفضيلات حتى لا نعيد المحاولة في كل مرة.
 */
class AudioRecorder(private val context: Context) {

    enum class RecorderState { IDLE, RECORDING, STOPPED, FAILED }

    var state: RecorderState = RecorderState.IDLE
        private set

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt = 0L
    private var peakAmplitude = 0
    private var amplitudePoller: Thread? = null

    /** أعلى سعة وصلت أثناء التسجيل — تُستخدم لكشف التسجيل الصامت */
    val peak: Int get() = peakAmplitude

    fun durationSeconds(): Int =
        if (startedAt == 0L) 0 else ((System.currentTimeMillis() - startedAt) / 1000L).toInt().coerceAtLeast(1)

    /**
     * بدء التسجيل.
     * @return الملف المؤقت أو null عند الفشل
     */
    fun start(tempName: String = "rec_${System.currentTimeMillis()}"): File? {
        if (state == RecorderState.RECORDING) return outputFile
        return try {
            val dir = File(context.filesDir, "sawti/audio").apply { mkdirs() }
            val file = File(dir, "$tempName.m4a")
            if (file.exists()) file.delete()

            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(44_100)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioChannels(1)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()

            recorder = r
            outputFile = file
            startedAt = System.currentTimeMillis()
            peakAmplitude = 0
            state = RecorderState.RECORDING
            startAmplitudePolling()
            file
        } catch (e: Throwable) {
            state = RecorderState.FAILED
            cleanupRecorder()
            null
        }
    }

    /** قراءة أعلى سعة كل ١٢٠ مللي ثانية */
    private fun startAmplitudePolling() {
        amplitudePoller?.interrupt()
        amplitudePoller = Thread {
            while (state == RecorderState.RECORDING) {
                try {
                    val amp = recorder?.maxAmplitude ?: 0
                    if (amp > peakAmplitude) peakAmplitude = amp
                    Thread.sleep(120)
                } catch (e: Throwable) {
                    return@Thread
                }
            }
        }.apply { isDaemon = true; start() }
    }

    /**
     * إيقاف التسجيل.
     * @param minPeak أقل سعة تُعتبر دليلاً على أن التسجيل ليس صامتاً
     * @return الملف إن كان صالحاً، وإلا null (ويُحذف الملف الصامت)
     */
    fun stop(minPeak: Int = 320): File? {
        if (state != RecorderState.RECORDING) return null
        amplitudePoller?.interrupt()
        amplitudePoller = null
        var file = outputFile
        try {
            recorder?.stop()
        } catch (e: Throwable) {
            // التوقف فشل (جلسة أقصر من الحد الأدنى) → الملف غير صالح
            file = null
        } finally {
            cleanupRecorder()
        }
        state = RecorderState.STOPPED

        val valid = file?.let { it.exists() && it.length() > 1_024L } ?: false
        val notSilent = peakAmplitude >= minPeak
        return if (valid && notSilent) {
            file
        } else {
            file?.delete()
            outputFile = null
            null
        }
    }

    /** إلغاء التسجيل وحذف الملف */
    fun cancel() {
        amplitudePoller?.interrupt()
        amplitudePoller = null
        try { recorder?.stop() } catch (_: Throwable) {}
        cleanupRecorder()
        outputFile?.delete()
        outputFile = null
        state = RecorderState.IDLE
    }

    private fun cleanupRecorder() {
        try { recorder?.reset() } catch (_: Throwable) {}
        try { recorder?.release() } catch (_: Throwable) {}
        recorder = null
    }

    /** هل التسجيل الصامت يعني أن الالتقاط المتوازي غير مدعوم؟ */
    fun wasSilentCapture(): Boolean = state == RecorderState.STOPPED && peakAmplitude < 320
}
