package com.clatos.dialer.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clatos.dialer.sync.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Prefill the server field with the last-used tenant URL, if any. */
    fun lastServerUrl(): String = authRepository.currentBaseUrl().orEmpty()

    fun login(serverUrl: String, username: String, password: String) {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Enter server URL, username and password")
            return
        }
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            authRepository.login(serverUrl, username, password)
                .onSuccess { _uiState.value = LoginUiState.Success }
                .onFailure { _uiState.value = LoginUiState.Error(it.message ?: "Login failed") }
        }
    }
}
