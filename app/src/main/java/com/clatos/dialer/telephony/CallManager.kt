package com.clatos.dialer.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import androidx.core.content.ContextCompat
import com.clatos.dialer.core.common.PhoneNumberUtils
import com.clatos.dialer.core.database.dao.ContactDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Phase of the active call, mapped from telecom Call states (API-safe). */
enum class CallPhase { CONNECTING, RINGING, ACTIVE, HOLDING, DISCONNECTED }

data class CallUiState(
    val number: String,
    val displayName: String?,
    val phase: CallPhase,
    val isIncoming: Boolean,
    val isMuted: Boolean = false,
    val isSpeaker: Boolean = false,
    val isRecording: Boolean = false,
    /** Wall-clock millis when the call became active, for the duration timer. */
    val connectedAtMillis: Long? = null,
)

/**
 * Single source of truth for the active call. The InCallService feeds call
 * objects/state in; the in-call UI observes [state] and invokes the action
 * methods. Audio routing/mute go through the [AudioController] implemented by
 * the InCallService (only it can change them).
 */
@Singleton
class CallManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactDao: ContactDao,
) {
    interface AudioController {
        fun setMuted(muted: Boolean)
        fun setAudioRoute(route: Int)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var call: Call? = null
    private var audioController: AudioController? = null

    private val _state = MutableStateFlow<CallUiState?>(null)
    val state: StateFlow<CallUiState?> = _state.asStateFlow()

    fun attachAudioController(controller: AudioController) { audioController = controller }
    fun detachAudioController() { audioController = null }

    fun onCallAdded(call: Call, isIncoming: Boolean) {
        this.call = call
        val number = call.numberOrEmpty()
        _state.value = CallUiState(
            number = number,
            displayName = null,
            phase = phaseOf(call.stateCompat()),
            isIncoming = isIncoming,
        )
        resolveDisplayName(number)
    }

    fun onStateChanged(call: Call) {
        val current = _state.value ?: return
        val phase = phaseOf(call.stateCompat())
        _state.value = current.copy(
            phase = phase,
            connectedAtMillis = if (phase == CallPhase.ACTIVE && current.connectedAtMillis == null) {
                System.currentTimeMillis()
            } else {
                current.connectedAtMillis
            },
        )
    }

    fun onCallRemoved() {
        call = null
        _state.value = null
    }

    fun setRecording(recording: Boolean) {
        _state.value = _state.value?.copy(isRecording = recording)
    }

    fun onAudioStateChanged(muted: Boolean, route: Int) {
        val current = _state.value ?: return
        _state.value = current.copy(
            isMuted = muted,
            isSpeaker = route == CallAudioState.ROUTE_SPEAKER,
        )
    }

    // ---- Actions invoked by the in-call UI ----

    fun answer() = call?.answer(VideoProfile.STATE_AUDIO_ONLY)

    fun reject() = call?.reject(false, null)

    fun hangup() = call?.disconnect()

    fun toggleMute() {
        val muted = _state.value?.isMuted ?: false
        audioController?.setMuted(!muted)
    }

    fun toggleSpeaker() {
        val speaker = _state.value?.isSpeaker ?: false
        audioController?.setAudioRoute(
            if (speaker) CallAudioState.ROUTE_EARPIECE else CallAudioState.ROUTE_SPEAKER,
        )
    }

    fun playDtmf(digit: Char) {
        call?.playDtmfTone(digit)
        call?.stopDtmfTone()
    }

    // ---- Helpers ----

    private fun resolveDisplayName(number: String) {
        if (number.isBlank()) return
        scope.launch {
            val name = lookupCrm(number) ?: lookupDevice(number)
            if (name != null) {
                _state.value = _state.value?.copy(displayName = name)
            }
        }
    }

    private suspend fun lookupCrm(number: String): String? =
        contactDao.byNormalizedNumber(PhoneNumberUtils.normalize(number))?.name

    private fun lookupDevice(number: String): String? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number),
        )
        return context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun phaseOf(state: Int): CallPhase = when (state) {
        Call.STATE_CONNECTING, Call.STATE_DIALING, Call.STATE_NEW -> CallPhase.CONNECTING
        Call.STATE_RINGING -> CallPhase.RINGING
        Call.STATE_ACTIVE -> CallPhase.ACTIVE
        Call.STATE_HOLDING -> CallPhase.HOLDING
        Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> CallPhase.DISCONNECTED
        else -> CallPhase.CONNECTING
    }
}

@Suppress("DEPRECATION")
private fun Call.stateCompat(): Int = state

private fun Call.numberOrEmpty(): String =
    details?.handle?.schemeSpecificPart.orEmpty()
