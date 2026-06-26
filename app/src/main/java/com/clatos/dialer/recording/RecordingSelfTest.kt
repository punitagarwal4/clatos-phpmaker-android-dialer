package com.clatos.dialer.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.clatos.dialer.core.database.dao.RecordingCapabilityDao
import com.clatos.dialer.core.database.entity.RecordingCapabilityEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Probes recording strategies in priority order on first run (and after OS
 * updates), measures whether each produces a non-trivial audio file, selects
 * the best working one, and persists the capability for the CRM telemetry.
 *
 * This scaffold validates the *mechanics* (a strategy can open the source and
 * write bytes). Confirming that the REMOTE party is actually captured requires
 * a live-call test on real hardware (see the device compatibility matrix).
 */
@Singleton
class RecordingSelfTest @Inject constructor(
    @ApplicationContext private val context: Context,
    private val capabilityDao: RecordingCapabilityDao,
) {

    /** Ordered candidate strategies (best → fallback). PRIVILEGED/ACCESSIBILITY are device-gated. */
    private fun candidates(): List<RecordingEngine> = listOf(
        MediaRecorderEngine(context, RecordingStrategy.VOICE_CALL, MediaRecorder.AudioSource.VOICE_CALL),
        MediaRecorderEngine(context, RecordingStrategy.VOICE_RECOGNITION, MediaRecorder.AudioSource.VOICE_RECOGNITION),
        MediaRecorderEngine(context, RecordingStrategy.MIC, MediaRecorder.AudioSource.MIC),
    )

    /** Returns the selected strategy, persisting the capability row. */
    suspend fun run(): RecordingStrategy {
        existingForThisOs()?.let { return RecordingStrategy.valueOf(it.selectedStrategy) }

        var selected = RecordingStrategy.NONE
        for (engine in candidates()) {
            if (!engine.isAvailable()) continue
            val probe = File.createTempFile("rectest", ".m4a", context.cacheDir)
            val ok = runCatching {
                engine.start(probe)
                delay(SAMPLE_MS)
                engine.stop().success
            }.getOrDefault(false)
            probe.delete()
            if (ok) {
                selected = engine.strategy
                break
            }
        }

        capabilityDao.save(
            RecordingCapabilityEntity(
                id = 0,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                selectedStrategy = selected.name,
                degraded = selected == RecordingStrategy.NONE || selected == RecordingStrategy.MIC,
                lastTestedAt = System.currentTimeMillis(),
            )
        )
        return selected
    }

    private suspend fun existingForThisOs(): RecordingCapabilityEntity? {
        val current = capabilityDao.get() ?: return null
        val osTag = "API ${Build.VERSION.SDK_INT}"
        return if (current.osVersion.contains(osTag)) current else null
    }

    private companion object {
        const val SAMPLE_MS = 1_200L
    }
}
