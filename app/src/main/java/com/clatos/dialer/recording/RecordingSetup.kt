package com.clatos.dialer.recording

import android.Manifest
import android.content.Context
import com.clatos.dialer.core.common.PermissionUtils
import com.clatos.dialer.sync.DiagnosticsReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot recording readiness: runs the per-device self-test (idempotent for
 * the current OS version) and reports capability to the CRM. Safe to call on
 * every app start and after onboarding — it no-ops when the mic permission
 * isn't granted yet and skips the test when capability is already known.
 */
@Singleton
class RecordingSetup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val selfTest: RecordingSelfTest,
    private val diagnosticsReporter: DiagnosticsReporter,
) {
    suspend fun ensure() {
        if (!PermissionUtils.isGranted(context, Manifest.permission.RECORD_AUDIO)) return
        selfTest.run()
        diagnosticsReporter.reportCapability()
    }

    /** Forces a fresh probe regardless of any stored result (manual re-test). */
    suspend fun forceRetest() {
        if (!PermissionUtils.isGranted(context, Manifest.permission.RECORD_AUDIO)) return
        selfTest.run(force = true)
        diagnosticsReporter.reportCapability()
    }
}

