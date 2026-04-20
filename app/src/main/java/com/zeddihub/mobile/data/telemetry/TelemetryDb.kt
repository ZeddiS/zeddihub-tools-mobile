package com.zeddihub.mobile.data.telemetry

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TelemetryEvent::class],
    version = 1,
    exportSchema = false
)
abstract class TelemetryDb : RoomDatabase() {
    abstract fun telemetry(): TelemetryDao
}
