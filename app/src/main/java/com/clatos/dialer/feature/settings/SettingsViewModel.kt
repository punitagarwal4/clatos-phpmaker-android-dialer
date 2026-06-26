package com.clatos.dialer.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clatos.dialer.core.common.di.ApplicationScope
import com.clatos.dialer.core.database.dao.RecordingCapabilityDao
import com.clatos.dialer.recording.RecordingSetup
import com.clatos.dialer.sync.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val agentName: String = "",
    val pendingSync: Int = 0,
    val recordingStrategy: String = "Not tested",
    val recordingDegraded: Boolean = false,
    val deviceInfo: String = "",
    val testing: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val capabilityDao: RecordingCapabilityDao,
    private val recordingSetup: RecordingSetup,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(agentName = authRepository.agentName()))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init { refresh() }

    private fun refresh() {
        viewModelScope.launch {
            val cap = capabilityDao.get()
            _state.value = _state.value.copy(
                pendingSync = authRepository.pendingSyncCount(),
                recordingStrategy = cap?.selectedStrategy ?: "Not tested",
                recordingDegraded = cap?.degraded ?: false,
                deviceInfo = cap?.let { "${it.deviceModel} • ${it.osVersion}" }.orEmpty(),
            )
        }
    }

    /** Re-probes recording capability and re-reports it to the CRM. */
    fun retestRecording() {
        _state.value = _state.value.copy(testing = true)
        appScope.launch {
            recordingSetup.forceRetest()
            val cap = capabilityDao.get()
            _state.value = _state.value.copy(
                testing = false,
                recordingStrategy = cap?.selectedStrategy ?: "Not tested",
                recordingDegraded = cap?.degraded ?: false,
                deviceInfo = cap?.let { "${it.deviceModel} • ${it.osVersion}" }.orEmpty(),
            )
        }
    }

    /** Logs out; the session state flips and gating returns to login. */
    fun logout() = authRepository.logout()
}
