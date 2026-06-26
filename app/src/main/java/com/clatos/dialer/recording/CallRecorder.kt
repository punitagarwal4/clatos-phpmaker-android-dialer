package com.clatos.dialer.recording

import android.content.Context
import android.media.MediaRecorder
import com.clatos.dialer.core.database.dao.RecordingCapabilityDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates recording of a single active call using the strategy chosen by
 * [RecordingSelfTest]. The telephony layer calls [onCallConnected] /
 * [onCallEnded]; the [RecordingService] keeps the process alive in foreground.
 */
@Singleton
class CallRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val capabilityDao: RecordingCapabilityDao,
) {
    private var engine: RecordingEngine? = null
    private var currentFile: File? = null

    suspend fun onCallConnected(callId: String): Boolean {
        val strategy = capabilityDao.get()?.selectedStrategy
            ?.let { runCatching { RecordingStrategy.valueOf(it) }.getOrNull() }
            ?: RecordingStrategy.NONE
        if (strategy == RecordingStrategy.NONE) return false

        val source = when (strategy) {
            RecordingStrategy.VOICE_CALL -> MediaRecorder.AudioSource.VOICE_CALL
            RecordingStrategy.VOICE_RECOGNITION -> MediaRecorder.AudioSource.VOICE_RECOGNITION
            else -> MediaRecorder.AudioSource.MIC
        }
        val recDir = File(context.filesDir, "recordings").apply { mkdirs() }
        val file = File(recDir, "$callId.m4a")
        return runCatching {
            MediaRecorderEngine(context, strategy, source).also {
                it.start(file)
                engine = it
                currentFile = file
            }
            true
        }.getOrDefault(false)
    }

    fun onCallEnded(): RecordingResult {
        val result = engine?.stop()
            ?: RecordingResult(file = null, strategy = RecordingStrategy.NONE, success = false)
        engine = null
        currentFile = null
        return result
    }
}
