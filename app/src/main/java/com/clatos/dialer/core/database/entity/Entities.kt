package com.clatos.dialer.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ContactSource { DEVICE, CRM }
enum class CallDirection { IN, OUT }
enum class CallStatus { INCOMING, OUTGOING, MISSED }
enum class RecordingState { OK, FAILED, NONE }
enum class SyncStatus { PENDING, SYNCED, FAILED }

/** Cached CRM contact (device contacts are read live from ContactsContract). */
@Entity(tableName = "contacts", indices = [Index("normalizedNumber")])
data class ContactEntity(
    @PrimaryKey val id: String,             // "crm:{id}" or "device:{lookupKey}"
    val source: ContactSource,
    val crmId: Long? = null,
    val name: String,
    val primaryNumber: String?,
    val normalizedNumber: String?,
    val company: String? = null,
    val updatedAt: Long = 0L,
)

/** A captured call, queued for upload to the CRM. */
@Entity(tableName = "call_logs", indices = [Index("syncStatus"), Index("clientCallId", unique = true)])
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientCallId: String,               // idempotency key for the CRM
    val direction: CallDirection,
    val number: String,
    val normalizedNumber: String,
    val displayName: String? = null,
    val contactCrmId: Long? = null,
    val startedAt: Long,
    val durationSec: Int,
    val status: CallStatus,
    val recordingPath: String? = null,
    val recordingStrategy: String = "NONE",
    val recordingState: RecordingState = RecordingState.NONE,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val attempts: Int = 0,
    val remoteId: Long? = null,
)

/** Result of the recording self-test for this device. */
@Entity(tableName = "recording_capability")
data class RecordingCapabilityEntity(
    @PrimaryKey val id: Int = 0,            // single row
    val deviceModel: String,
    val osVersion: String,
    val selectedStrategy: String,
    val degraded: Boolean,
    val lastTestedAt: Long,
)
