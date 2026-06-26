package com.clatos.dialer.recording

import java.io.File

/** Audio-source/strategy used to capture a call, best → fallback. */
enum class RecordingStrategy {
    /** Device Owner / platform-signed / privileged grant on managed devices (best fidelity). */
    PRIVILEGED,

    /** MediaRecorder with AudioSource.VOICE_CALL — works on some OEMs/older versions. */
    VOICE_CALL,

    /** AudioSource.VOICE_RECOGNITION — usable audio on certain devices. */
    VOICE_RECOGNITION,

    /** Accessibility-service-assisted capture where the OEM permits it. */
    ACCESSIBILITY,

    /** AudioSource.MIC — local side reliable; remote side OEM/speakerphone dependent. */
    MIC,

    /** No viable strategy; recording disabled. */
    NONE,
}

data class RecordingResult(
    val file: File?,
    val strategy: RecordingStrategy,
    val success: Boolean,
    val error: String? = null,
)

/**
 * A single concrete way to record a call. Implementations are probed by the
 * [RecordingSelfTest] and selected in priority order.
 */
interface RecordingEngine {
    val strategy: RecordingStrategy

    /** Cheap capability probe (permissions, API level, source availability). */
    suspend fun isAvailable(): Boolean

    /** Begin recording the active call into [output]. */
    fun start(output: File)

    /** Stop and finalize; returns the result (file + measured quality). */
    fun stop(): RecordingResult
}
