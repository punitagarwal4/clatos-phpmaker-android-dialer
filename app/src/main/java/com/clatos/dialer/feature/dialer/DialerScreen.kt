package com.clatos.dialer.feature.dialer

import android.Manifest
import android.content.Context
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
import androidx.compose.material3.Card
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.clatos.dialer.core.common.PermissionUtils

/**
 * Dialpad + entry points to history, contacts, and settings. Placing the call
 * uses TelecomManager; the in-call experience is driven by ClatosInCallService.
 * Surfaces a warning if call/mic permissions were revoked (US-3.2).
 */
@Composable
fun DialerScreen(
    onOpenContacts: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    var number by remember { mutableStateOf("") }
    val keys = listOf("1","2","3","4","5","6","7","8","9","*","0","#")

    var refreshTick by remember { mutableStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refreshTick++
        onPauseOrDispose { }
    }
    val canCall = remember(refreshTick) { PermissionUtils.isGranted(context, Manifest.permission.CALL_PHONE) }
    val canRecord = remember(refreshTick) { PermissionUtils.isGranted(context, Manifest.permission.RECORD_AUDIO) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = onOpenHistory) { Text("History") }
            OutlinedButton(onClick = onOpenContacts) { Text("Contacts") }
            OutlinedButton(onClick = onOpenSettings) { Text("Settings") }
        }

        if (!canCall || !canRecord) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    buildString {
                        if (!canCall) append("Phone permission is off — calls are disabled. ")
                        if (!canRecord) append("Microphone is off — calls won't be recorded.")
                    }.trim(),
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        Text(text = number, modifier = Modifier.padding(vertical = 24.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f)) {
            items(keys) { key ->
                Button(onClick = { number += key }, modifier = Modifier.padding(8.dp)) { Text(key) }
            }
        }
        Button(
            onClick = { placeCall(context, number) },
            enabled = canCall && number.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Call") }
    }
}

private fun placeCall(context: Context, number: String) {
    if (number.isBlank()) return
    val telecom = context.getSystemService<TelecomManager>() ?: return
    runCatching {
        // Requires CALL_PHONE permission (checked before enabling the button).
        telecom.placeCall(Uri.fromParts("tel", number, null), null)
    }
}
