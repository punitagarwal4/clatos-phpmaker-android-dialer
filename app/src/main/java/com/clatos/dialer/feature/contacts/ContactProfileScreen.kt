package com.clatos.dialer.feature.contacts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Contact profile. For CRM-linked contacts this fetches a live profile from
 * GET /api/contacts/{id} (US-7.1) and shows recent call history (US-7.2).
 * Wiring the crmId argument + ContactRepository.profile() is the next step.
 */
@Composable
fun ContactProfileScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Contact profile")
        Text(
            "Loads live CRM profile + recent calls. No delete action (by policy).",
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
