package com.clatos.dialer.core.database.di

import android.content.Context
import androidx.room.Room
import com.clatos.dialer.core.database.ClatosDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ClatosDatabase =
        // Only destroy on downgrade (rare). On upgrade, add real Migrations so
        // un-uploaded call logs in the offline queue are never silently wiped.
        Room.databaseBuilder(context, ClatosDatabase::class.java, "clatos.db")
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun callLogDao(db: ClatosDatabase) = db.callLogDao()
    @Provides fun contactDao(db: ClatosDatabase) = db.contactDao()
    @Provides fun recordingCapabilityDao(db: ClatosDatabase) = db.recordingCapabilityDao()
}
