package com.zeddihub.mobile.data.alerts

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Insert
    suspend fun insert(alert: Alert): Long

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC LIMIT 200")
    fun observeRecent(): Flow<List<Alert>>

    @Query("SELECT COUNT(*) FROM alerts WHERE read = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("UPDATE alerts SET read = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE alerts SET read = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM alerts WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)

    @Query("DELETE FROM alerts")
    suspend fun wipe()
}
