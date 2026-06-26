package com.clatos.dialer.telephony

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Minimal bridge between the [ClatosInCallService] and the Compose in-call UI.
 * The service publishes the current primary call; the UI observes it to render
 * answer/hangup/mute/speaker controls.
 */
object InCallBus {
    private val _currentCall = MutableStateFlow<Call?>(null)
    val currentCall: StateFlow<Call?> = _currentCall.asStateFlow()

    fun publish(call: Call) { _currentCall.value = call }

    fun clear(call: Call) {
        if (_currentCall.value == call) _currentCall.value = null
    }
}
