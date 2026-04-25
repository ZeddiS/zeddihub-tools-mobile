package com.zeddihub.mobile.data.reminders

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderDataStore by preferencesDataStore("reminders")

/**
 * Persistent reminder store backed by DataStore Preferences. We
 * serialise the entire list to a single JSON string — reminders are
 * tens of items at most, so the simplicity of "rewrite the whole list
 * on every edit" beats the overhead of a Room database.
 *
 * Save/load go through ReminderJson which already validates each
 * entry; malformed entries are silently dropped on read so a corrupted
 * file never crashes the screen.
 */
@Singleton
class ReminderStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = stringPreferencesKey("rules_json")

    val flow: Flow<List<Reminder>> = context.reminderDataStore.data.map { prefs ->
        ReminderJson.fromJson(prefs[key] ?: "[]")
    }

    suspend fun load(): List<Reminder> = flow.first()

    suspend fun save(list: List<Reminder>) {
        val json = ReminderJson.toJson(list)
        context.reminderDataStore.edit { it[key] = json }
    }

    suspend fun upsert(r: Reminder) {
        val all = load().toMutableList()
        val idx = all.indexOfFirst { it.id == r.id }
        if (idx >= 0) all[idx] = r else all.add(r)
        save(all)
    }

    suspend fun remove(id: String) {
        save(load().filterNot { it.id == id })
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        val all = load().toMutableList()
        val idx = all.indexOfFirst { it.id == id }
        if (idx >= 0) {
            all[idx] = all[idx].copy(enabled = enabled)
            save(all)
        }
    }
}
