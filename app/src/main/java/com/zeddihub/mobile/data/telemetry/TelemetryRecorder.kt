package com.zeddihub.mobile.data.telemetry

import android.content.Context
import android.os.Build
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.zeddihub.mobile.BuildConfig
import com.zeddihub.mobile.data.local.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records usage events to a local Room queue and best-effort uploads them
 * to /api/telemetry. Silent on any network/backend failure — events stay
 * queued for later retry.
 */
@Singleton
class TelemetryRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: TelemetryDao,
    private val appPreferences: AppPreferences
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val flushLock = Mutex()
    private val moshi = Moshi.Builder().build()
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun screenView(route: String, dwellMs: Long) = record("screen", route, durationMs = dwellMs)
    fun toolRun(id: String, success: Boolean) = record("tool", id, success = success)
    fun sessionStart() = record("session", "start", extra = deviceExtra())
    fun sessionEnd(totalMs: Long) = record("session", "end", durationMs = totalMs)
    fun crash(message: String) = record("crash", "uncaught", extra = message.take(2000))

    private fun record(
        type: String,
        name: String,
        durationMs: Long? = null,
        success: Boolean? = null,
        extra: String? = null
    ) {
        scope.launch {
            dao.insert(
                TelemetryEvent(
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    name = name,
                    durationMs = durationMs,
                    success = success,
                    extra = extra
                )
            )
            flush()
        }
    }

    suspend fun flush() {
        if (!flushLock.tryLock()) return
        try {
            while (true) {
                val batch = dao.oldest(50)
                if (batch.isEmpty()) return
                if (!upload(batch)) return
                dao.deleteAll(batch)
            }
        } finally {
            flushLock.unlock()
        }
    }

    private fun upload(batch: List<TelemetryEvent>): Boolean {
        val payload = Payload(
            app = BuildConfig.APPLICATION_ID,
            version = BuildConfig.VERSION_NAME,
            events = batch.map {
                EventDto(
                    ts = it.timestamp,
                    type = it.type,
                    name = it.name,
                    durationMs = it.durationMs,
                    success = it.success,
                    extra = it.extra
                )
            }
        )
        val adapter = moshi.adapter(Payload::class.java)
        val body = adapter.toJson(payload)
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url(BuildConfig.API_BASE_URL.trimEnd('/') + "/telemetry")
            .post(body)
            .build()
        return runCatching {
            client.newCall(req).execute().use { resp -> resp.isSuccessful }
        }.getOrDefault(false)
    }

    private fun deviceExtra(): String =
        "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} · ${appPreferences.language.value.tag}"

    @JsonClass(generateAdapter = true)
    data class Payload(
        val app: String,
        val version: String,
        val events: List<EventDto>
    )

    @JsonClass(generateAdapter = true)
    data class EventDto(
        val ts: Long,
        val type: String,
        val name: String,
        val durationMs: Long?,
        val success: Boolean?,
        val extra: String?
    )
}
