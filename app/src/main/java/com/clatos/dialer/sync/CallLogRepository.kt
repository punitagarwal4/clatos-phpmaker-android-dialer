package com.clatos.dialer.sync

import android.content.Context
import android.telecom.Call
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.clatos.dialer.core.common.PhoneNumberUtils
import com.clatos.dialer.core.database.dao.CallLogDao
import com.clatos.dialer.core.database.entity.CallDirection
import com.clatos.dialer.core.database.entity.CallLogEntity
import com.clatos.dialer.core.database.entity.CallStatus
import com.clatos.dialer.core.database.entity.RecordingState
import com.clatos.dialer.recording.RecordingResult
import com.clatos.dialer.telephony.ClatosInCallService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists captured calls to Room (queued) and enqueues the upload worker.
 * Offline-first: rows are written locally and uploaded by [CallSyncWorker].
 */
@Singleton
class CallLogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callLogDao: CallLogDao,
) {
    fun observeHistory(): Flow<List<CallLogEntity>> = callLogDao.observeAll()

    suspend fun persistEndedCall(
        call: Call,
        active: ClatosInCallService.ActiveCall,
        recording: RecordingResult,
    ) {
        val details = call.details
        val rawNumber = details.handle?.schemeSpecificPart.orEmpty()
        val isIncoming = active.isIncoming
        val connectedAt = details.connectTimeMillis.takeIf { it > 0 } ?: active.startedAt
        val durationSec = ((System.currentTimeMillis() - connectedAt) / 1000).toInt().coerceAtLeast(0)
        val missed = isIncoming && details.connectTimeMillis <= 0

        val entity = CallLogEntity(
            clientCallId = active.clientCallId,
            direction = if (isIncoming) CallDirection.IN else CallDirection.OUT,
            number = rawNumber,
            normalizedNumber = PhoneNumberUtils.normalize(rawNumber),
            startedAt = active.startedAt,
            durationSec = durationSec,
            status = when {
                missed -> CallStatus.MISSED
                isIncoming -> CallStatus.INCOMING
                else -> CallStatus.OUTGOING
            },
            recordingPath = recording.file?.absolutePath,
            recordingStrategy = recording.strategy.name,
            recordingState = if (recording.success) RecordingState.OK else RecordingState.FAILED,
        )
        callLogDao.insert(entity)
        enqueueSync()
    }

    /** Kicks an immediate (network-gated) upload attempt with exponential backoff. */
    fun enqueueSync() {
        val request = OneTimeWorkRequestBuilder<CallSyncWorker>()
            .setConstraints(CallSyncWorker.constraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(CallSyncWorker.NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    /** Periodic safety-net sync so queued logs drain even with no new calls. */
    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<CallSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(CallSyncWorker.constraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                CallSyncWorker.PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }
}
