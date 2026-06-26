package com.clatos.dialer.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clatos.dialer.core.datastore.SessionStore
import com.clatos.dialer.sync.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Top-level app state. Authentication gates the app; once authenticated, the
 * PermissionGate (not this state) decides whether the agent reaches the dialer,
 * by requiring all runtime permissions + the default-dialer role.
 */
enum class SessionState { Loading, Unauthenticated, Authenticated }

@HiltViewModel
class SessionViewModel @Inject constructor(
    sessionStore: SessionStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val state: StateFlow<SessionState> =
        sessionStore.isAuthenticated
            .map { authed -> if (authed) SessionState.Authenticated else SessionState.Unauthenticated }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    init {
        // Validate the stored token on launch; clears the session on a 401 so
        // the app gates back to login (offline keeps the existing session).
        viewModelScope.launch { authRepository.validateSession() }
    }
}
