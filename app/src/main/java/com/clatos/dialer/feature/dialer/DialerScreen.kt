package com.clatos.dialer.feature.dialer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clatos.dialer.core.common.CallPlacer

/**
 * Dialpad + entry points to history, contacts, and settings. Placing the call
 * uses TelecomManager; the in-call experience is driven by ClatosInCallService.
 */
@Composable
fun DialerScreen(
    onOpenContacts: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    var number by remember { mutableStateOf("") }
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = onOpenHistory) { Text("History") }
            OutlinedButton(onClick = onOpenContacts) { Text("Contacts") }
            OutlinedButton(onClick = onOpenSettings) { Text("Settings") }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = number.ifEmpty { "Enter a number" },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 28.sp,
            )
            if (number.isNotEmpty()) {
                BackspaceButton(
                    onClick = { number = number.dropLast(1) },
                    onClear = { number = "" },
                )
            }
        }

        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f)) {
            items(keys) { key ->
                DialKey(
                    label = key,
                    onClick = { number += key },
                    onLongClick = if (key == "0") {
                        { number += "+" }
                    } else {
                        null
                    },
                )
            }
        }

        OutlinedButton(
            onClick = { CallPlacer.placeCall(context, number) },
            enabled = number.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Call")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKey(label: String, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    Surface(
        modifier = Modifier
            .padding(6.dp)
            .height(60.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 24.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BackspaceButton(onClick: () -> Unit, onClear: () -> Unit) {
    Box(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onClear)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete last digit (long-press to clear)")
    }
}
