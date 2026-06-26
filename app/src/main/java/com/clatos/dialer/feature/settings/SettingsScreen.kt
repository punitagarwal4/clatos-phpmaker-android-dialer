package com.clatos.dialer.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Minimal settings: account info, pending sync count, and logout (US-1.4).
 * Logout warns when there are unsynced call logs.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Settings", fontWeight = FontWeight.Bold)
        Text("Signed in as: ${state.agentName.ifBlank { "—" }}")
        Text("Pending uploads: ${state.pendingSync}")
        Text("Recording: ${state.recordingStrategy}" + if (state.recordingDegraded) " (limited on this device)" else "")
        if (state.deviceInfo.isNotBlank()) {
            Text(state.deviceInfo)
        }

        OutlinedButton(
            onClick = viewModel::retestRecording,
            enabled = !state.testing,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text(if (state.testing) "Testing…" else "Re-run recording test") }

        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) { Text("Log out") }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out?") },
            text = {
                Text(
                    if (state.pendingSync > 0) {
                        "${state.pendingSync} call log(s) haven't synced yet. Logging out keeps " +
                            "them queued but they won't upload until you sign back in. Continue?"
                    } else {
                        "You'll need to sign in again to use the dialer."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                }) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            },
        )
    }
}
