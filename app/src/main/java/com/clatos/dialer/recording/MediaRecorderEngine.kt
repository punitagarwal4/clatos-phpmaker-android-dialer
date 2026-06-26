package com.clatos.dialer.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File

/**
 * MediaRecorder-based engine, parameterized by audio source. Used for the
 * VOICE_CALL, VOICE_RECOGNITION, and MIC strategies. Output is AAC in an .m4a.
 *
 * NOTE: On Android 9/10+ the VOICE_CALL source is restricted for non-system
 * apps. The [RecordingSelfTest] determines whether a given source actually
 * produces usable audio on this device; this class only does the mechanics.
 */
class MediaRecorderEngine(
    @ApplicationContext private val context: Context,
    override val strategy: RecordingStrategy,
    private val audioSource: Int,
) : RecordingEngine {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    override suspend fun isAvailable(): Boolean = when (strategy) {
        RecordingStrategy.MIC -> true
        RecordingStrategy.VOICE_RECOGNITION -> true
        // VOICE_CALL is frequently blocked; the self-test confirms real capture.
        RecordingStrategy.VOICE_CALL -> true
        else -> false
    }

    override fun start(output: File) {
        outputFile = output
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        rec.apply {
            setAudioSource(audioSource)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(output.absolutePath)
            prepare()
            start()
        }
        recorder = rec
    }

    override fun stop(): RecordingResult = try {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        val file = outputFile
        val ok = file != null && file.exists() && file.length() > MIN_VALID_BYTES
        RecordingResult(file = file, strategy = strategy, success = ok)
    } catch (e: Exception) {
        recorder?.runCatching { release() }
        recorder = null
        RecordingResult(file = outputFile, strategy = strategy, success = false, error = e.message)
    }

    private companion object {
        const val MIN_VALID_BYTES = 1_024L
    }
}
