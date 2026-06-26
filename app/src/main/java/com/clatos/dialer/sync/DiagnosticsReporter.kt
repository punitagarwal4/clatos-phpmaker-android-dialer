package com.clatos.dialer.sync

import com.clatos.dialer.BuildConfig
import com.clatos.dialer.core.database.dao.RecordingCapabilityDao
import com.clatos.dialer.core.datastore.SessionStore
import com.clatos.dialer.core.network.CrmApi
import com.clatos.dialer.core.network.dto.DeviceReportRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reports the device's recording capability to the CRM for admin visibility
 * (US-4.6 / US-9.3): which strategy was selected and whether recording is
 * degraded on this handset. Failures are swallowed — telemetry is best-effort.
 */
@Singleton
class DiagnosticsReporter @Inject constructor(
    private val crmApi: CrmApi,
    private val capabilityDao: RecordingCapabilityDao,
    private val sessionStore: SessionStore,
) {
    suspend fun reportCapability() {
        val agentId = sessionStore.agentId()
        if (agentId < 0) return
        val cap = capabilityDao.get() ?: return
        runCatching {
            crmApi.reportDevice(
                DeviceReportRequest(
                    agentId = agentId,
                    deviceModel = cap.deviceModel,
                    osVersion = cap.osVersion,
                    selectedStrategy = cap.selectedStrategy,
                    recordingDegraded = cap.degraded,
                    appVersion = BuildConfig.VERSION_NAME,
                ),
            )
        }
    }
}
