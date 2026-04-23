package com.zeddihub.mobile.ui.tools

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Video Downloader front-end.
 *
 * Flow:
 *  1. User pastes URL → we POST it to /api/tools/cobalt_proxy.php
 *     (server-side adds any cobalt API key and forwards to cobalt.tools).
 *  2. Proxy returns JSON with a short-lived {status, url, filename}.
 *  3. We enqueue DownloadManager to drop the file into Downloads/.
 */
@HiltViewModel
class VideoDownloaderViewModel @Inject constructor(
    @ApplicationContext private val appCtx: Context
) : ViewModel() {

    data class UiState(
        val url: String = "",
        val audioOnly: Boolean = false,
        val quality: String = "720",
        val fetching: Boolean = false,
        val lastStatus: Status = Status.Ready,
        val lastResultFile: String? = null,
        val lastError: String? = null
    )

    sealed interface Status {
        data object Ready : Status
        data object Fetching : Status
        data object Enqueued : Status
        data object Error : Status
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun setUrl(u: String) { _state.value = _state.value.copy(url = u) }
    fun setAudioOnly(v: Boolean) { _state.value = _state.value.copy(audioOnly = v) }
    fun setQuality(q: String) { _state.value = _state.value.copy(quality = q) }

    fun fetchAndEnqueue() {
        val s = _state.value
        if (s.url.isBlank() || s.fetching) return
        _state.value = s.copy(
            fetching = true,
            lastStatus = Status.Fetching,
            lastError = null,
            lastResultFile = null
        )

        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { callCobalt(s) } }
            val parsed = result.getOrNull()
            if (parsed == null) {
                _state.value = _state.value.copy(
                    fetching = false,
                    lastStatus = Status.Error,
                    lastError = result.exceptionOrNull()?.message ?: "network"
                )
                return@launch
            }
            if (parsed.downloadUrl == null) {
                _state.value = _state.value.copy(
                    fetching = false,
                    lastStatus = Status.Error,
                    lastError = parsed.errorCode ?: "upstream returned no url"
                )
                return@launch
            }
            val filename = parsed.filename ?: suggestFilename(s)
            enqueueDownload(parsed.downloadUrl, filename)
            _state.value = _state.value.copy(
                fetching = false,
                lastStatus = Status.Enqueued,
                lastResultFile = filename
            )
        }
    }

    private data class CobaltResult(
        val downloadUrl: String?,
        val filename: String?,
        val errorCode: String?
    )

    private fun callCobalt(s: UiState): CobaltResult {
        val body = JSONObject().apply {
            put("url", s.url.trim())
            put("audioOnly", s.audioOnly)
            put("videoQuality", s.quality)
        }.toString()

        val req = Request.Builder()
            .url(BuildConfig.API_BASE_URL.trimEnd('/') + "/tools/cobalt_proxy.php")
            .header("Accept", "application/json")
            .header("X-App-Secret", BuildConfig.APP_SECRET)
            .header("X-Client-Kind", BuildConfig.CLIENT_KIND)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(req).execute().use { resp ->
            val txt = resp.body?.string().orEmpty()
            val json = runCatching { JSONObject(txt) }.getOrNull()
                ?: return CobaltResult(null, null, "HTTP ${resp.code}")
            val status = json.optString("status", "")
            val url = json.optString("url", "").ifBlank { null }
            val filename = json.optString("filename", "").ifBlank { null }
            // "picker" (multiple media) → we take first item's url if present.
            if (status == "picker") {
                val arr = json.optJSONArray("picker")
                val first = arr?.optJSONObject(0)
                val pickerUrl = first?.optString("url", "")?.ifBlank { null }
                return CobaltResult(pickerUrl, filename, null)
            }
            if (status == "error") {
                val err = json.optJSONObject("error")?.optString("code", "unknown") ?: "unknown"
                return CobaltResult(null, null, err)
            }
            return CobaltResult(url, filename, null)
        }
    }

    private fun enqueueDownload(url: String, filename: String) {
        val dm = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(Uri.parse(url))
            .setTitle(filename)
            .setDescription("ZeddiHub Video Downloader")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        dm.enqueue(req)
    }

    private fun suggestFilename(s: UiState): String {
        val base = Uri.parse(s.url.trim()).host?.replace('.', '_') ?: "video"
        val ext = if (s.audioOnly) "mp3" else "mp4"
        val ts = System.currentTimeMillis()
        return "zh_${base}_$ts.$ext"
    }

    companion object {
        val QUALITIES = listOf("360", "480", "720", "1080", "1440", "2160", "max")
    }
}
