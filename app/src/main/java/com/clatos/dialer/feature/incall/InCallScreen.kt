package com.clatos.dialer.feature.incall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clatos.dialer.telephony.CallPhase
import com.clatos.dialer.telephony.CallUiState
import kotlinx.coroutines.delay

@Composable
fun InCallScreen(
    onCallEnded: () -> Unit,
    viewModel: InCallViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // When the call is gone, close the activity.
    LaunchedEffect(state == null) {
        if (state == null) onCallEnded()
    }

    val call = state ?: return

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.3f))
        Text(
            text = call.displayName ?: call.number.ifBlank { "Unknown" },
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
        )
        if (call.displayName != null && call.number.isNotBlank()) {
            Text(call.number, modifier = Modifier.padding(top = 4.dp))
        }
        Text(
            text = statusLine(call),
            modifier = Modifier.padding(top = 8.dp),
        )
        if (call.isRecording) {
            Text("● Recording", modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.weight(0.7f))

        val ringingIncoming = call.phase == CallPhase.RINGING && call.isIncoming
        if (ringingIncoming) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                OutlinedButton(onClick = viewModel::reject) { Text("Decline") }
                Button(onClick = viewModel::answer) { Text("Answer") }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                FilledTonalButton(onClick = viewModel::toggleMute) {
                    Text(if (call.isMuted) "Unmute" else "Mute")
                }
                FilledTonalButton(onClick = viewModel::toggleSpeaker) {
                    Text(if (call.isSpeaker) "Speaker off" else "Speaker")
                }
            }
            Button(
                onClick = viewModel::hangup,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Hang up") }
        }
    }
}

@Composable
private fun statusLine(call: CallUiState): String {
    return when (call.phase) {
        CallPhase.CONNECTING -> if (call.isIncoming) "Incoming…" else "Calling…"
        CallPhase.RINGING -> if (call.isIncoming) "Incoming call" else "Ringing…"
        CallPhase.ACTIVE -> rememberDuration(call.connectedAtMillis)
        CallPhase.HOLDING -> "On hold"
        CallPhase.DISCONNECTED -> "Call ended"
    }
}

/** Ticks a mm:ss timer once per second from [connectedAtMillis]. */
@Composable
private fun rememberDuration(connectedAtMillis: Long?): String {
    if (connectedAtMillis == null) return "Connected"
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(connectedAtMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val elapsed = ((now - connectedAtMillis) / 1000).coerceAtLeast(0)
    val mm = elapsed / 60
    val ss = elapsed % 60
    return "%02d:%02d".format(mm, ss)
}
