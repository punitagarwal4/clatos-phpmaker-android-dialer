package com.clatos.dialer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.clatos.dialer.core.database.dao.CallLogDao
import com.clatos.dialer.core.database.dao.ContactDao
import com.clatos.dialer.core.database.dao.RecordingCapabilityDao
import com.clatos.dialer.core.database.entity.CallDirection
import com.clatos.dialer.core.database.entity.CallLogEntity
import com.clatos.dialer.core.database.entity.CallStatus
import com.clatos.dialer.core.database.entity.ContactEntity
import com.clatos.dialer.core.database.entity.ContactSource
import com.clatos.dialer.core.database.entity.RecordingCapabilityEntity
import com.clatos.dialer.core.database.entity.RecordingState
import com.clatos.dialer.core.database.entity.SyncStatus

class Converters {
    @TypeConverter fun contactSource(v: ContactSource) = v.name
    @TypeConverter fun toContactSource(v: String) = ContactSource.valueOf(v)
    @TypeConverter fun callDirection(v: CallDirection) = v.name
    @TypeConverter fun toCallDirection(v: String) = CallDirection.valueOf(v)
    @TypeConverter fun callStatus(v: CallStatus) = v.name
    @TypeConverter fun toCallStatus(v: String) = CallStatus.valueOf(v)
    @TypeConverter fun recState(v: RecordingState) = v.name
    @TypeConverter fun toRecState(v: String) = RecordingState.valueOf(v)
    @TypeConverter fun syncStatus(v: SyncStatus) = v.name
    @TypeConverter fun toSyncStatus(v: String) = SyncStatus.valueOf(v)
}

@Database(
    entities = [
        ContactEntity::class,
        CallLogEntity::class,
        RecordingCapabilityEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ClatosDatabase : RoomDatabase() {
    abstract fun callLogDao(): CallLogDao
    abstract fun contactDao(): ContactDao
    abstract fun recordingCapabilityDao(): RecordingCapabilityDao
}
