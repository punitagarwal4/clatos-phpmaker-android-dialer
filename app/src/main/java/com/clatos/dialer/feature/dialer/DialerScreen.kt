package com.clatos.dialer.feature.dialer

import android.net.Uri
import android.telecom.TelecomManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService

/**
 * Dialpad + entry point to history and contacts. Placing the call uses
 * TelecomManager; the in-call experience is driven by ClatosInCallService.
 */
@Composable
fun DialerScreen(
    onOpenContacts: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val context = LocalContext.current
    var number by remember { mutableStateOf("") }
    val keys = listOf("1","2","3","4","5","6","7","8","9","*","0","#")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = onOpenHistory) { Text("History") }
            OutlinedButton(onClick = onOpenContacts) { Text("Contacts") }
        }
        Text(text = number, modifier = Modifier.padding(vertical = 24.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f)) {
            items(keys) { key ->
                Button(onClick = { number += key }, modifier = Modifier.padding(8.dp)) { Text(key) }
            }
        }
        Button(
            onClick = { placeCall(context, number) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Call") }
    }
}

private fun placeCall(context: android.content.Context, number: String) {
    if (number.isBlank()) return
    val telecom = context.getSystemService<TelecomManager>() ?: return
    runCatching {
        // Requires CALL_PHONE permission (requested in onboarding).
        telecom.placeCall(Uri.fromParts("tel", number, null), null)
    }
}
