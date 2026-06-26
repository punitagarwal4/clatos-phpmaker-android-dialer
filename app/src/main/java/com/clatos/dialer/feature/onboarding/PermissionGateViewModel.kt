package com.clatos.dialer.feature.onboarding

import androidx.lifecycle.ViewModel
import com.clatos.dialer.core.common.di.ApplicationScope
import com.clatos.dialer.recording.RecordingSetup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionGateViewModel @Inject constructor(
    private val recordingSetup: RecordingSetup,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    /** Called once all permissions are granted; kicks the recording self-test
     *  (mic is now available) on the app scope so it survives recomposition. */
    fun onPermissionsReady() {
        appScope.launch { recordingSetup.ensure() }
    }
}
