package com.zeddihub.mobile.data.alerts

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val severity: String, // "info", "warn", "critical"
    val source: String,   // "desktop", "server", "system"
    val title: String,
    val body: String,
    val read: Boolean = false
)
