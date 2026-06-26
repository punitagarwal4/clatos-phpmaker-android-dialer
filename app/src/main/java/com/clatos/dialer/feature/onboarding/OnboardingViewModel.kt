package com.clatos.dialer.feature.onboarding

import androidx.lifecycle.ViewModel
import com.clatos.dialer.core.common.di.ApplicationScope
import com.clatos.dialer.core.datastore.SessionStore
import com.clatos.dialer.recording.RecordingSetup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val recordingSetup: RecordingSetup,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    /** Marks onboarding complete; the session state flips to Ready and the app
     *  navigates to the dialer (handled by MainActivity's gating). Also kicks
     *  off the recording self-test now that the mic permission is granted. */
    fun completeOnboarding() {
        sessionStore.setOnboardingComplete(true)
        // Use the app scope so the test survives this screen being torn down.
        appScope.launch { recordingSetup.ensure() }
    }
}
