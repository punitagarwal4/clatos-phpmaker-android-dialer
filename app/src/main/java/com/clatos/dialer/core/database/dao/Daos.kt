package com.clatos.dialer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.clatos.dialer.core.database.entity.CallLogEntity
import com.clatos.dialer.core.database.entity.ContactEntity
import com.clatos.dialer.core.database.entity.RecordingCapabilityEntity
import com.clatos.dialer.core.database.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: CallLogEntity): Long

    @Query("SELECT * FROM call_logs ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE syncStatus = :status ORDER BY startedAt ASC")
    suspend fun pending(status: SyncStatus = SyncStatus.PENDING): List<CallLogEntity>

    @Query("UPDATE call_logs SET syncStatus = :status, remoteId = :remoteId WHERE id = :id")
    suspend fun markSynced(id: Long, remoteId: Long?, status: SyncStatus = SyncStatus.SYNCED)

    @Query("UPDATE call_logs SET syncStatus = :status, attempts = attempts + 1 WHERE id = :id")
    suspend fun markAttempt(id: Long, status: SyncStatus = SyncStatus.FAILED)
}

@Dao
interface ContactDao {
    @Upsert
    suspend fun upsertAll(contacts: List<ContactEntity>)

    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE normalizedNumber = :normalized LIMIT 1")
    suspend fun byNormalizedNumber(normalized: String): ContactEntity?
}

@Dao
interface RecordingCapabilityDao {
    @Upsert
    suspend fun save(capability: RecordingCapabilityEntity)

    @Query("SELECT * FROM recording_capability WHERE id = 0 LIMIT 1")
    suspend fun get(): RecordingCapabilityEntity?
}
