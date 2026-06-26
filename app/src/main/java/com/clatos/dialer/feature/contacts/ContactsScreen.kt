package com.clatos.dialer.feature.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clatos.dialer.core.database.entity.ContactEntity
import com.clatos.dialer.sync.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repository: ContactRepository,
) : ViewModel() {
    // NOTE: device contacts (ContactsContract) are merged in a full build; the
    // scaffold shows cached CRM contacts. Deletion is intentionally unsupported.
    val contacts: StateFlow<List<ContactEntity>> =
        repository.observeCachedCrmContacts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { refresh() }

    fun refresh() = viewModelScope.launch { repository.syncCrmContacts() }
}

@Composable
fun ContactsScreen(
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onCreate, modifier = Modifier.padding(bottom = 8.dp)) { Text("New contact") }
        LazyColumn {
            items(contacts) { contact ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(contact.id) }
                        .padding(vertical = 8.dp),
                ) {
                    Text(contact.name)
                    Text("${contact.primaryNumber ?: ""} • ${contact.source}")
                }
                HorizontalDivider()
            }
        }
    }
}
