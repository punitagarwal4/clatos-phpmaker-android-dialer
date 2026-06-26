package com.clatos.dialer.feature.calllog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.clatos.dialer.core.common.CallPlacer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clatos.dialer.core.database.entity.CallLogEntity
import com.clatos.dialer.sync.CallLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class CallLogViewModel @Inject constructor(
    repository: CallLogRepository,
) : ViewModel() {
    val history: StateFlow<List<CallLogEntity>> =
        repository.observeHistory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun CallLogScreen(viewModel: CallLogViewModel = hiltViewModel()) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Call history", modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn {
            items(history) { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${entry.status} • ${entry.displayName ?: entry.number}")
                        Text("${entry.durationSec}s • rec:${entry.recordingState} • sync:${entry.syncStatus}")
                    }
                    if (entry.number.isNotBlank()) {
                        OutlinedButton(onClick = { CallPlacer.placeCall(context, entry.number) }) {
                            Text("Call")
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
