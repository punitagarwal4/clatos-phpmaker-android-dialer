package com.clatos.dialer.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clatos.dialer.core.datastore.SessionStore
import com.clatos.dialer.sync.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** High-level app state used to pick the start destination and gate features. */
enum class SessionState { Loading, Unauthenticated, NeedsOnboarding, Ready }

@HiltViewModel
class SessionViewModel @Inject constructor(
    sessionStore: SessionStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val state: StateFlow<SessionState> =
        combine(sessionStore.isAuthenticated, sessionStore.onboardingComplete) { authed, onboarded ->
            when {
                !authed -> SessionState.Unauthenticated
                !onboarded -> SessionState.NeedsOnboarding
                else -> SessionState.Ready
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    init {
        // Validate the stored token on launch; clears the session on a 401 so
        // the app gates back to login (offline keeps the existing session).
        viewModelScope.launch { authRepository.validateSession() }
    }
}
