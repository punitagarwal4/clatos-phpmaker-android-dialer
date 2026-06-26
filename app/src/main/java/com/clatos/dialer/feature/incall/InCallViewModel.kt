package com.clatos.dialer.feature.incall

import androidx.lifecycle.ViewModel
import com.clatos.dialer.telephony.CallManager
import com.clatos.dialer.telephony.CallUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class InCallViewModel @Inject constructor(
    private val callManager: CallManager,
) : ViewModel() {

    val state: StateFlow<CallUiState?> = callManager.state

    fun answer() = callManager.answer()
    fun reject() = callManager.reject()
    fun hangup() = callManager.hangup()
    fun toggleMute() = callManager.toggleMute()
    fun toggleSpeaker() = callManager.toggleSpeaker()
    fun playDtmf(digit: Char) = callManager.playDtmf(digit)
}
