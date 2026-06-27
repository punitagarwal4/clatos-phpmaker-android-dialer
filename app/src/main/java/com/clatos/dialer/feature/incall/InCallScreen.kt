package com.clatos.dialer.feature.incall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
    var showKeypad by remember { mutableStateOf(false) }

    LaunchedEffect(state == null) { if (state == null) onCallEnded() }
    val call = state ?: return

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.2f))
        Text(
            text = call.displayName ?: call.number.ifBlank { "Unknown" },
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
        )
        if (call.displayName != null && call.number.isNotBlank()) {
            Text(call.number, modifier = Modifier.padding(top = 4.dp))
        }
        Text(statusLine(call), modifier = Modifier.padding(top = 8.dp))
        if (call.isRecording) {
            Text("● Recording", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.weight(0.5f))

        val ringingIncoming = call.phase == CallPhase.RINGING && call.isIncoming
        if (ringingIncoming) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = viewModel::reject,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Decline") }
                Button(onClick = viewModel::answer) { Text("Answer") }
            }
        } else {
            if (showKeypad) {
                DtmfKeypad(onDigit = viewModel::playDtmf)
                TextButton(onClick = { showKeypad = false }) { Text("Hide keypad") }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ToggleControl(
                    selected = call.isMuted,
                    onToggle = { viewModel.toggleMute() },
                    icon = { Icon(if (call.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic, contentDescription = "Mute") },
                    label = "Mute",
                )
                ToggleControl(
                    selected = call.isSpeaker,
                    onToggle = { viewModel.toggleSpeaker() },
                    icon = { Icon(Icons.Filled.VolumeUp, contentDescription = "Speaker") },
                    label = "Speaker",
                )
                ToggleControl(
                    selected = showKeypad,
                    onToggle = { showKeypad = !showKeypad },
                    icon = { Icon(Icons.Filled.Dialpad, contentDescription = "Keypad") },
                    label = "Keypad",
                )
            }
            Button(
                onClick = viewModel::hangup,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Filled.CallEnd, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Hang up")
            }
        }
        Spacer(Modifier.weight(0.2f))
    }
}

@Composable
private fun ToggleControl(
    selected: Boolean,
    onToggle: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconToggleButton(
            checked = selected,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(56.dp),
        ) { icon() }
        Text(label, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun DtmfKeypad(onDigit: (Char) -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        items(keys) { key ->
            TextButton(onClick = { onDigit(key.first()) }, modifier = Modifier.padding(4.dp)) {
                Text(key, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun statusLine(call: CallUiState): String = when (call.phase) {
    CallPhase.CONNECTING -> if (call.isIncoming) "Incoming…" else "Calling…"
    CallPhase.RINGING -> if (call.isIncoming) "Incoming call" else "Ringing…"
    CallPhase.ACTIVE -> rememberDuration(call.connectedAtMillis)
    CallPhase.HOLDING -> "On hold"
    CallPhase.DISCONNECTED -> "Call ended"
}

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
    return "%02d:%02d".format(elapsed / 60, elapsed % 60)
}
