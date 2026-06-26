package com.clatos.dialer.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.clatos.dialer.core.database.dao.CallLogDao
import com.clatos.dialer.core.database.entity.CallLogEntity
import com.clatos.dialer.core.database.entity.RecordingState
import com.clatos.dialer.core.datastore.SessionStore
import com.clatos.dialer.core.network.CrmApi
import com.clatos.dialer.core.network.dto.CallLogMetadata
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant

/**
 * Uploads pending call logs + recordings to the CRM. Runs under WorkManager
 * with a network constraint and exponential backoff. Each row is committed
 * independently so a single failure doesn't block the rest of the batch.
 */
@HiltWorker
class CallSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val callLogDao: CallLogDao,
    private val crmApi: CrmApi,
    private val sessionStore: SessionStore,
    private val json: Json,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val agentId = sessionStore.agentId()
        if (agentId < 0) return Result.success() // not logged in; nothing to do

        val pending = callLogDao.pending()
        var hadFailure = false

        for (log in pending) {
            val ok = runCatching { upload(log, agentId) }.getOrDefault(false)
            if (ok) {
                pruneRecording(log)
            } else {
                hadFailure = true
                callLogDao.markAttempt(log.id)
            }
        }

        return if (hadFailure) Result.retry() else Result.success()
    }

    private suspend fun upload(log: CallLogEntity, agentId: Long): Boolean {
        val metadata = CallLogMetadata(
            agentId = agentId,
            direction = log.direction.name,
            number = log.number,
            contactId = log.contactCrmId,
            displayName = log.displayName,
            startedAt = Instant.ofEpochMilli(log.startedAt).toString(),
            durationSec = log.durationSec,
            status = log.status.name,
            recordingState = log.recordingState.name,
            recordingStrategy = log.recordingStrategy,
            clientCallId = log.clientCallId,
        )
        val metaPart = json.encodeToString(metadata)
            .toRequestBody("application/json".toMediaType())

        val recordingPart = log.recordingPath
            ?.takeIf { log.recordingState == RecordingState.OK }
            ?.let { File(it) }
            ?.takeIf { it.exists() }
            ?.let { file ->
                MultipartBody.Part.createFormData(
                    name = "recording",
                    filename = file.name,
                    body = file.asRequestBody("audio/mp4".toMediaType()),
                )
            }

        val response = crmApi.uploadCallLog(metaPart, recordingPart)
        callLogDao.markSynced(log.id, response.id)
        return true
    }

    private fun pruneRecording(log: CallLogEntity) {
        // Retention policy: prune local audio after successful upload (window TBD).
        log.recordingPath?.let { runCatching { File(it).delete() } }
    }

    companion object {
        const val NAME = "call_sync"
        const val PERIODIC_NAME = "call_sync_periodic"

        fun constraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
