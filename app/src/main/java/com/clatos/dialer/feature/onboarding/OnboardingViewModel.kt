package com.clatos.dialer.feature.onboarding

import androidx.lifecycle.ViewModel
import com.clatos.dialer.core.datastore.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val sessionStore: SessionStore,
) : ViewModel() {

    /** Marks onboarding complete; the session state flips to Ready and the app
     *  navigates to the dialer (handled by MainActivity's gating). */
    fun completeOnboarding() = sessionStore.setOnboardingComplete(true)
}
