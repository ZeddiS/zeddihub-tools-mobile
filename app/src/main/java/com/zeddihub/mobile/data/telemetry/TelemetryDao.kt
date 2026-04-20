package com.zeddihub.mobile.data.telemetry

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TelemetryDao {

    @Insert
    suspend fun insert(event: TelemetryEvent): Long

    @Query("SELECT * FROM telemetry_events ORDER BY id ASC LIMIT :limit")
    suspend fun oldest(limit: Int): List<TelemetryEvent>

    @Query("SELECT COUNT(*) FROM telemetry_events")
    suspend fun count(): Int

    @Delete
    suspend fun deleteAll(events: List<TelemetryEvent>)

    @Query("DELETE FROM telemetry_events")
    suspend fun wipe()
}
