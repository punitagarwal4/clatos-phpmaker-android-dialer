package com.clatos.dialer.feature.calllog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clatos.dialer.core.common.CallFormatting
import com.clatos.dialer.core.common.CallPlacer
import com.clatos.dialer.core.database.entity.CallLogEntity
import com.clatos.dialer.sync.CallLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallLogViewModel @Inject constructor(
    private val repository: CallLogRepository,
) : ViewModel() {
    val history: StateFlow<List<CallLogEntity>> =
        repository.observeHistory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun syncNow() = repository.enqueueSync()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogScreen(
    onBack: () -> Unit,
    viewModel: CallLogViewModel = hiltViewModel(),
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call history") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.syncNow()
                        scope.launch { snackbarHostState.showSnackbar("Sync queued") }
                    }) { Text("Sync now") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text("No calls yet") }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(history, key = { it.id }) { entry ->
                CallLogRow(entry, onCall = { CallPlacer.placeCall(context, entry.number) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun CallLogRow(entry: CallLogEntity, onCall: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.displayName ?: entry.number, fontWeight = FontWeight.Bold)
            Text(
                "${CallFormatting.statusLabel(entry.status)} · " +
                    "${CallFormatting.relativeTime(entry.startedAt)} · ${CallFormatting.duration(entry.durationSec)}",
            )
            Text("${CallFormatting.recordingLabel(entry.recordingState)} · ${CallFormatting.syncLabel(entry.syncStatus)}")
        }
        if (entry.number.isNotBlank()) {
            OutlinedButton(onClick = onCall) { Text("Call") }
        }
    }
}
