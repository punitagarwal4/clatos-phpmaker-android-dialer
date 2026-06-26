package com.clatos.dialer.telephony

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.clatos.dialer.recording.CallRecorder
import com.clatos.dialer.recording.RecordingService
import com.clatos.dialer.sync.CallLogRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
            CallNotifications.showIncoming(this, callManager.state.value?.let {
                it.displayName ?: it.number
            }.orEmpty())
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
        RecordingService.stop(this)
        callManager.onCallRemoved()
        scope.launch {
            val recording = callRecorder.onCallEnded()
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
                RecordingService.start(this)
                scope.launch {
                    val ok = callRecorder.onCallConnected(active.clientCallId)
                    callManager.setRecording(ok)
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
