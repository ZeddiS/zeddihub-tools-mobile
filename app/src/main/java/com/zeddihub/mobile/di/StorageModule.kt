package com.zeddihub.mobile.di

import android.content.Context
import androidx.room.Room
import com.zeddihub.mobile.data.alerts.AlertDao
import com.zeddihub.mobile.data.alerts.AlertDb
import com.zeddihub.mobile.data.local.AppPreferences
import com.zeddihub.mobile.data.local.CredentialStore
import com.zeddihub.mobile.data.telemetry.TelemetryDao
import com.zeddihub.mobile.data.telemetry.TelemetryDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideCredentialStore(@ApplicationContext context: Context): CredentialStore =
        CredentialStore(context)

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences =
        AppPreferences(context)

    @Provides
    @Singleton
    fun provideTelemetryDb(@ApplicationContext context: Context): TelemetryDb =
        Room.databaseBuilder(context, TelemetryDb::class.java, "telemetry.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTelemetryDao(db: TelemetryDb): TelemetryDao = db.telemetry()

    @Provides
    @Singleton
    fun provideAlertDb(@ApplicationContext context: Context): AlertDb =
        Room.databaseBuilder(context, AlertDb::class.java, "alerts.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideAlertDao(db: AlertDb): AlertDao = db.alerts()
}
