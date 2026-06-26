package com.clatos.dialer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.clatos.dialer.core.common.di.ApplicationScope
import com.clatos.dialer.recording.RecordingSetup
import com.clatos.dialer.sync.CallLogRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point. Hosts the Hilt graph and provides a Hilt-aware
 * [HiltWorkerFactory] so WorkManager workers can use constructor injection.
 */
@HiltAndroidApp
class ClatosApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var recordingSetup: RecordingSetup
    @Inject lateinit var callLogRepository: CallLogRepository

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Ensure recording capability is probed/reported once mic is granted
        // (re-runs after an OS update; no-ops before onboarding grants the mic).
        appScope.launch { recordingSetup.ensure() }
        // Drain any queued call logs even if no new calls happen.
        callLogRepository.schedulePeriodicSync()
    }
}
