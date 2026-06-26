package com.clatos.dialer.telephony

import android.telecom.Call
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
 * routes calls here. We observe call state to (a) drive the in-call UI and
 * (b) start/stop recording and persist a CallLog entry for CRM sync.
 */
@AndroidEntryPoint
class ClatosInCallService : InCallService() {

    @Inject lateinit var callRecorder: CallRecorder
    @Inject lateinit var callLogRepository: CallLogRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Tracks per-call metadata until the call ends and we can persist it.
    private val activeCalls = mutableMapOf<Call, ActiveCall>()

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val clientId = UUID.randomUUID().toString()
        activeCalls[call] = ActiveCall(clientCallId = clientId, startedAt = System.currentTimeMillis())
        InCallBus.publish(call) // expose to the Compose in-call UI
        call.registerCallback(callback)
        handleState(call, call.details.state)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callback)
        val active = activeCalls.remove(call) ?: return
        RecordingService.stop(this)
        scope.launch {
            val recording = callRecorder.onCallEnded()
            callLogRepository.persistEndedCall(call, active, recording)
        }
        InCallBus.clear(call)
    }

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = handleState(call, state)
    }

    private fun handleState(call: Call, state: Int) {
        InCallBus.publish(call)
        if (state == Call.STATE_ACTIVE) {
            val active = activeCalls[call] ?: return
            if (!active.recordingStarted) {
                active.recordingStarted = true
                RecordingService.start(this)
                scope.launch { callRecorder.onCallConnected(active.clientCallId) }
            }
        }
    }

    /** In-memory state for a call in progress. */
    data class ActiveCall(
        val clientCallId: String,
        val startedAt: Long,
        var recordingStarted: Boolean = false,
    )
}
