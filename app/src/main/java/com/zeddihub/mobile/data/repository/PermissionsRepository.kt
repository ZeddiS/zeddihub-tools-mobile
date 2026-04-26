package com.zeddihub.mobile.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.zeddihub.mobile.data.remote.ApiService
import com.zeddihub.mobile.data.remote.dto.PermissionsDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caches the user's feature-permission matrix.
 *
 * Lifecycle:
 *   1. App start → load() reads the last cached blob from DataStore
 *      so the UI has a sane state immediately (no permission flicker).
 *   2. After a successful auth or session resume, refresh() hits
 *      /api/permissions.php and overwrites the cache.
 *   3. UI components observe [permissions] (StateFlow) and react.
 *
 * Failure mode is "fail open": when the network fetch errors out we
 * keep the previous cached state. We never block the UI on a missing
 * permission response — features stay accessible from the last known
 * good state, which mirrors what most users would expect (the matrix
 * change rolls out on next app open).
 */
private val Context.permsDataStore by preferencesDataStore("perms")

@Singleton
class PermissionsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService,
    private val auth: AuthRepository,
    moshi: Moshi,
) {
    private val key = stringPreferencesKey("perms_json")
    private val adapter: JsonAdapter<PermissionsDto> =
        moshi.adapter(PermissionsDto::class.java)

    private val _permissions = MutableStateFlow(EMPTY)
    val permissions: StateFlow<PermissionsDto> = _permissions.asStateFlow()

    /** Load whatever is cached. Call once on app start. */
    suspend fun loadCached() {
        val raw = context.permsDataStore.data.map { it[key] }.first()
        if (raw.isNullOrBlank()) return
        val parsed = runCatching { adapter.fromJson(raw) }.getOrNull() ?: return
        _permissions.value = parsed
    }

    /**
     * Hit the network and replace the cache. Fails open: on any error
     * the previous state stays in place.
     */
    suspend fun refresh() {
        val token = auth.session.value?.token
        val bearer = if (token.isNullOrBlank()) null else "Bearer $token"
        val fresh = runCatching { api.fetchPermissions(bearer = bearer) }.getOrNull()
            ?: return
        _permissions.value = fresh
        runCatching {
            val json = adapter.toJson(fresh)
            context.permsDataStore.edit { it[key] = json }
        }
    }

    /** Three-way state for one feature; defaults to visible if unknown. */
    fun stateOf(featureKey: String): FeatureState = when (
        _permissions.value.states[featureKey]
    ) {
        "soon" -> FeatureState.SOON
        "hidden" -> FeatureState.HIDDEN
        else -> FeatureState.VISIBLE
    }

    companion object {
        val EMPTY = PermissionsDto(
            ok = true, role = "guest", adFree = false,
            states = emptyMap(), fetchedAt = 0
        )
    }
}

enum class FeatureState { VISIBLE, SOON, HIDDEN }
