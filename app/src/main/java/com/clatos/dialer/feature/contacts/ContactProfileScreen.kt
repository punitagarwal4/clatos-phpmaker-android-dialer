package com.clatos.dialer.feature.contacts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Contact profile. For CRM-linked contacts this shows a profile fetched live
 * from GET /api/contacts/{id} (US-7.1) plus recent local call history with the
 * number (US-7.2). No delete action exists, by policy (US-8.2).
 */
@Composable
fun ContactProfileScreen(viewModel: ContactProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (state.loading) {
            CircularProgressIndicator()
            return@Column
        }
        state.error?.let {
            Text("Couldn't load profile: $it")
            return@Column
        }

        Text(state.name, fontWeight = FontWeight.Bold)
        if (state.isCrm) {
            Text("CRM profile", modifier = Modifier.padding(bottom = 8.dp))
        }

        state.fields.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("$label: ", fontWeight = FontWeight.Bold)
                Text(value)
            }
        }

        if (state.recentCalls.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text("Recent calls", fontWeight = FontWeight.Bold)
            state.recentCalls.forEach { call ->
                Text(
                    "${call.status} • ${call.durationSec}s • rec:${call.recordingState}",
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}
