package com.clatos.dialer.feature.contacts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clatos.dialer.sync.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateUiState(
    val saving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ContactCreateViewModel @Inject constructor(
    private val repository: ContactRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateUiState())
    val state: StateFlow<CreateUiState> = _state.asStateFlow()

    fun create(name: String, phone: String, company: String, onSaved: () -> Unit) {
        if (name.isBlank() || phone.isBlank()) {
            _state.value = CreateUiState(error = "Name and phone are required")
            return
        }
        _state.value = CreateUiState(saving = true)
        viewModelScope.launch {
            repository.create(name, phone, company.ifBlank { null }, null)
                .onSuccess {
                    _state.value = CreateUiState()
                    onSaved()
                }
                .onFailure {
                    _state.value = CreateUiState(error = it.message ?: "Couldn't save contact")
                }
        }
    }
}

@Composable
fun ContactCreateScreen(
    onSaved: () -> Unit,
    viewModel: ContactCreateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("New contact", modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(name, { name = it }, label = { Text("Name") })
        OutlinedTextField(phone, { phone = it }, label = { Text("Phone") })
        OutlinedTextField(company, { company = it }, label = { Text("Company") })

        state.error?.let {
            Text(it, modifier = Modifier.padding(top = 8.dp))
        }

        Button(
            onClick = { viewModel.create(name, phone, company, onSaved) },
            enabled = !state.saving,
            modifier = Modifier.padding(top = 16.dp),
        ) { Text(if (state.saving) "Saving…" else "Save to CRM") }
    }
}
