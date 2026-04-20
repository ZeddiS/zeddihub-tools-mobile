package com.zeddihub.mobile.data.alerts

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Alert::class], version = 1, exportSchema = false)
abstract class AlertDb : RoomDatabase() {
    abstract fun alerts(): AlertDao
}
