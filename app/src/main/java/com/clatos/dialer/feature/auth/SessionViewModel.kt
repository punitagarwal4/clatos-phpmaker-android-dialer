package com.clatos.dialer.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clatos.dialer.core.datastore.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/** High-level app state used to pick the start destination. */
enum class SessionState { Loading, Unauthenticated, NeedsOnboarding, Ready }

@HiltViewModel
class SessionViewModel @Inject constructor(
    sessionStore: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        sessionStore.isAuthenticated
            .onEach { authed ->
                // Onboarding completion is tracked separately in a real build;
                // for the scaffold, authenticated users go through onboarding once.
                _state.value = if (authed) SessionState.NeedsOnboarding else SessionState.Unauthenticated
            }
            .launchIn(viewModelScope)
    }
}
