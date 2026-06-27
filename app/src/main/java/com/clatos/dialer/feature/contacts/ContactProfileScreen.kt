package com.clatos.dialer.feature.contacts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clatos.dialer.core.common.CallFormatting
import com.clatos.dialer.core.common.CallPlacer

/**
 * Contact profile. For CRM-linked contacts this shows a profile fetched live
 * from GET /api/contacts/{id} (US-7.1) plus recent local call history with the
 * number (US-7.2). No delete action exists, by policy (US-8.2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactProfileScreen(
    onBack: () -> Unit,
    viewModel: ContactProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.name.ifBlank { "Contact" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            state.error != null ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Couldn't load profile: ${state.error}")
                }
            else -> Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                if (state.isCrm) {
                    Text("CRM profile", modifier = Modifier.padding(bottom = 8.dp))
                }
                state.fields.forEach { (label, value) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("$label: ", fontWeight = FontWeight.Bold)
                        Text(value)
                    }
                }
                state.number?.takeIf { it.isNotBlank() }?.let { number ->
                    Button(
                        onClick = { CallPlacer.placeCall(context, number) },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Call")
                    }
                }

                if (state.recentCalls.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text("Recent calls", fontWeight = FontWeight.Bold)
                    state.recentCalls.forEach { call ->
                        Text(
                            "${CallFormatting.statusLabel(call.status)} · " +
                                "${CallFormatting.relativeTime(call.startedAt)} · " +
                                "${CallFormatting.duration(call.durationSec)} · " +
                                CallFormatting.recordingLabel(call.recordingState),
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
