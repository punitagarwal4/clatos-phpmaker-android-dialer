package com.clatos.dialer.feature.contacts

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clatos.dialer.core.database.entity.CallLogEntity
import com.clatos.dialer.sync.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val loading: Boolean = true,
    val name: String = "",
    val number: String? = null,
    val fields: List<Pair<String, String>> = emptyList(),
    val recentCalls: List<CallLogEntity> = emptyList(),
    val isCrm: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ContactProfileViewModel @Inject constructor(
    private val repository: ContactRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val contactId: String =
        Uri.decode(savedStateHandle.get<String>("contactId").orEmpty())

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val crmId = contactId.takeIf { it.startsWith("crm:") }
            ?.removePrefix("crm:")
            ?.toLongOrNull()

        if (crmId == null) {
            // Device-only contact: CRM has no richer profile to fetch.
            _state.value = ProfileUiState(
                loading = false,
                name = "Device contact",
                isCrm = false,
                fields = listOf("Source" to "Device address book"),
            )
            return
        }

        _state.value = ProfileUiState(loading = true, isCrm = true)
        viewModelScope.launch {
            repository.profile(crmId)
                .onSuccess { dto ->
                    val number = dto.phone ?: dto.phones.firstOrNull()
                    val recent = repository.recentCalls(number)
                    _state.value = ProfileUiState(
                        loading = false,
                        isCrm = true,
                        name = dto.name,
                        number = number,
                        fields = buildList {
                            number?.let { add("Phone" to it) }
                            dto.email?.let { add("Email" to it) }
                            dto.company?.let { add("Company" to it) }
                            dto.updatedAt?.let { add("Updated" to it) }
                        },
                        recentCalls = recent,
                    )
                }
                .onFailure {
                    _state.value = ProfileUiState(
                        loading = false,
                        isCrm = true,
                        error = it.message ?: "Couldn't load profile",
                    )
                }
        }
    }
}
