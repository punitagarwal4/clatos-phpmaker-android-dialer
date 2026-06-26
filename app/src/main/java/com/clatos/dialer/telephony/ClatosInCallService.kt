package com.clatos.dialer.telephony

import android.Manifest
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.clatos.dialer.core.common.PermissionUtils
import com.clatos.dialer.recording.CallRecorder
import com.clatos.dialer.recording.RecordingService
import com.clatos.dialer.sync.CallLogRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * The app's in-call service. Because the app holds ROLE_DIALER, the system
 * routes calls here. It feeds call state into [CallManager] (which the in-call
 * UI observes), provides audio control, launches the in-call UI, and drives
 * recording + call-log capture.
 */
@AndroidEntryPoint
class ClatosInCallService : InCallService() {

    @Inject lateinit var callManager: CallManager
    @Inject lateinit var callRecorder: CallRecorder
    @Inject lateinit var callLogRepository: CallLogRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeCalls = mutableMapOf<Call, ActiveCall>()

    private val audioController = object : CallManager.AudioController {
        override fun setMuted(muted: Boolean) = this@ClatosInCallService.setMuted(muted)
        override fun setAudioRoute(route: Int) = this@ClatosInCallService.setAudioRoute(route)
    }

    override fun onCreate() {
        super.onCreate()
        callManager.attachAudioController(audioController)
    }

    override fun onDestroy() {
        callManager.detachAudioController()
        super.onDestroy()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        callManager.onAudioStateChanged(audioState.isMuted, audioState.route)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        @Suppress("DEPRECATION")
        val isIncoming = call.state == Call.STATE_RINGING
        activeCalls[call] = ActiveCall(UUID.randomUUID().toString(), System.currentTimeMillis(), isIncoming)

        callManager.onCallAdded(call, isIncoming)
        call.registerCallback(callback)

        // Surface the UI: full-screen notification for incoming, direct launch otherwise.
        if (isIncoming) {
            val initial = callManager.state.value
            CallNotifications.showIncoming(this, initial?.let { it.displayName ?: it.number }.orEmpty())
            // Caller ID resolves asynchronously; re-post the notification with the
            // name once it's known, as long as the call is still ringing.
            scope.launch {
                val resolved = callManager.state.first {
                    it == null || it.displayName != null || it.phase != CallPhase.RINGING
                }
                if (resolved?.phase == CallPhase.RINGING && resolved.displayName != null) {
                    CallNotifications.showIncoming(this@ClatosInCallService, resolved.displayName)
                }
            }
        } else {
            CallNotifications.launchInCall(this)
        }

        @Suppress("DEPRECATION")
        handleState(call, call.state)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callback)
        CallNotifications.cancel(this)
        val active = activeCalls.remove(call) ?: return
        callManager.onCallRemoved()
        scope.launch {
            // Finalize the recording BEFORE tearing down the mic foreground
            // service, otherwise the file can be truncated/corrupted.
            val recording = callRecorder.onCallEnded()
            RecordingService.stop(this@ClatosInCallService)
            callLogRepository.persistEndedCall(call, active, recording)
        }
    }

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = handleState(call, state)
    }

    private fun handleState(call: Call, state: Int) {
        callManager.onStateChanged(call)
        if (state == Call.STATE_ACTIVE) {
            CallNotifications.cancel(this)
            CallNotifications.launchInCall(this)
            val active = activeCalls[call] ?: return
            if (!active.recordingStarted) {
                active.recordingStarted = true
                // Only start the microphone foreground service when the mic is
                // granted — on Android 14+ starting a microphone FGS without
                // RECORD_AUDIO throws and would crash the call.
                if (PermissionUtils.isGranted(this, Manifest.permission.RECORD_AUDIO)) {
                    scope.launch {
                        RecordingService.start(this@ClatosInCallService)
                        val ok = callRecorder.onCallConnected(active.clientCallId)
                        callManager.setRecording(ok)
                        // No working strategy → don't keep the mic FGS running.
                        if (!ok) RecordingService.stop(this@ClatosInCallService)
                    }
                } else {
                    callManager.setRecording(false)
                }
            }
        }
    }

    /** In-memory state for a call in progress. */
    data class ActiveCall(
        val clientCallId: String,
        val startedAt: Long,
        val isIncoming: Boolean,
        var recordingStarted: Boolean = false,
    )
}
