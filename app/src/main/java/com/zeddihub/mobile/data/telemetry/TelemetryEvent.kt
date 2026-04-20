package com.zeddihub.mobile.data.telemetry

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_events")
data class TelemetryEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String,        // "screen", "tool", "session", "crash", "device"
    val name: String,        // screen route, tool id, session action, etc.
    val durationMs: Long? = null,
    val success: Boolean? = null,
    val extra: String? = null
)
