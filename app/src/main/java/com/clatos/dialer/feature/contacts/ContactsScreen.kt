package com.clatos.dialer.feature.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clatos.dialer.core.common.CallPlacer
import com.clatos.dialer.sync.ContactRepository
import com.clatos.dialer.sync.UnifiedContact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactsUiState(
    val query: String = "",
    val contacts: List<UnifiedContact> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repository: ContactRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ContactsUiState())
    val state: StateFlow<ContactsUiState> = _state.asStateFlow()

    init { refresh() }

    fun onQueryChange(value: String) {
        _state.value = _state.value.copy(query = value)
        reload()
    }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.syncCrmContacts()
                .onFailure { _state.value = _state.value.copy(error = "Couldn't sync CRM contacts") }
            reload()
        }
    }

    private fun reload() {
        viewModelScope.launch {
            val list = repository.loadUnified(_state.value.query)
            _state.value = _state.value.copy(contacts = list, loading = false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = "New contact")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            state.error?.let { Text(it, modifier = Modifier.padding(bottom = 8.dp)) }

            when {
                state.loading && state.contacts.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                state.contacts.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (state.query.isBlank()) "No contacts yet" else "No matches")
                    }
                else -> LazyColumn {
                    items(state.contacts, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            onOpen = { onOpen(contact.id) },
                            onCall = { contact.number?.let { CallPlacer.placeCall(context, it) } },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: UnifiedContact, onOpen: () -> Unit, onCall: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).clickable { onOpen() }) {
            Text(contact.name, fontWeight = FontWeight.Bold)
            Text("${contact.number ?: ""} · ${sourceLabel(contact)}")
        }
        if (!contact.number.isNullOrBlank()) {
            OutlinedButton(onClick = onCall) { Text("Call") }
        }
    }
}

private fun sourceLabel(contact: UnifiedContact): String = when {
    contact.alsoInOtherSource -> "CRM + device"
    else -> contact.source.name.lowercase().replaceFirstChar { it.uppercase() }
}
