package com.clatos.dialer.core.common

import android.text.format.DateUtils
import com.clatos.dialer.core.database.entity.CallStatus
import com.clatos.dialer.core.database.entity.RecordingState
import com.clatos.dialer.core.database.entity.SyncStatus

/** Human-readable formatting for call data shown in the UI. */
object CallFormatting {

    /** Seconds → "m:ss" or "h:mm:ss". */
    fun duration(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
    }

    /** Epoch millis → relative time like "5 min ago", "Yesterday". */
    fun relativeTime(epochMillis: Long): String =
        DateUtils.getRelativeTimeSpanString(
            epochMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()

    fun statusLabel(status: CallStatus): String = when (status) {
        CallStatus.INCOMING -> "Incoming"
        CallStatus.OUTGOING -> "Outgoing"
        CallStatus.MISSED -> "Missed"
    }

    fun recordingLabel(state: RecordingState): String = when (state) {
        RecordingState.OK -> "Recorded"
        RecordingState.FAILED -> "Recording failed"
        RecordingState.NONE -> "Not recorded"
    }

    fun syncLabel(status: SyncStatus): String = when (status) {
        SyncStatus.PENDING -> "Pending upload"
        SyncStatus.SYNCED -> "Uploaded"
        SyncStatus.FAILED -> "Upload failed"
    }
}
