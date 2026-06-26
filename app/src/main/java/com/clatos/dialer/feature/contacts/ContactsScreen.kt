package com.clatos.dialer.feature.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repository: ContactRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _contacts = MutableStateFlow<List<UnifiedContact>>(emptyList())
    val contacts: StateFlow<List<UnifiedContact>> = _contacts.asStateFlow()

    init { refresh() }

    fun onQueryChange(value: String) {
        _query.value = value
        reload()
    }

    /** Pulls the latest CRM contacts then rebuilds the unified list. */
    fun refresh() {
        viewModelScope.launch {
            repository.syncCrmContacts()
            reload()
        }
    }

    private fun reload() {
        viewModelScope.launch { _contacts.value = repository.loadUnified(_query.value) }
    }
}

@Composable
fun ContactsScreen(
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Contacts")
            Button(onClick = onCreate) { Text("New") }
        }
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        LazyColumn {
            items(contacts) { contact ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpen(contact.id) },
                    ) {
                        Text(contact.name)
                        Text("${contact.number ?: ""} • ${sourceLabel(contact)}")
                    }
                    contact.number?.takeIf { it.isNotBlank() }?.let { number ->
                        OutlinedButton(onClick = { CallPlacer.placeCall(context, number) }) {
                            Text("Call")
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

private fun sourceLabel(contact: UnifiedContact): String = when {
    contact.alsoInOtherSource -> "CRM + device"
    else -> contact.source.name.lowercase().replaceFirstChar { it.uppercase() }
}
