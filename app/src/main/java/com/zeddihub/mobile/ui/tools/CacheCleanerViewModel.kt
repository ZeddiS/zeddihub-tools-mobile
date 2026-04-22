package com.zeddihub.mobile.ui.tools

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.telemetry.TelemetryRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CacheCleanerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    private val telemetry: TelemetryRecorder
) : ViewModel() {

    data class UiState(
        val appCacheBytes: Long = 0,
        val tempBytes: Long = 0,
        val lastAction: String? = null,
        val safScanning: Boolean = false,
        val safScannedBytes: Long = 0,
        val safMatchCount: Int = 0,
        val safRootUri: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        val cache = dirSize(context.cacheDir)
        val temp = dirSize(context.externalCacheDir) + dirSize(File(context.filesDir, "tmp"))
        _state.value = _state.value.copy(appCacheBytes = cache, tempBytes = temp)
    }

    fun clearAll() = viewModelScope.launch(Dispatchers.IO) {
        val before = _state.value.appCacheBytes + _state.value.tempBytes
        wipeDir(context.cacheDir)
        context.externalCacheDir?.let { wipeDir(it) }
        wipeDir(File(context.filesDir, "tmp"))
        httpClient.cache?.evictAll()
        refresh()
        _state.value = _state.value.copy(lastAction = "Cleared ${before / 1024} KB")
        telemetry.toolRun("cache_clear_all", true)
    }

    fun flushDns() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            // OkHttp has no public DNS cache invalidation, but evicting connections
            // drops cached addresses held by active pooled connections.
            httpClient.connectionPool.evictAll()
        }
        _state.value = _state.value.copy(lastAction = "DNS/connection pool flushed")
        telemetry.toolRun("cache_flush_dns", true)
    }

    /**
     * SAF-based device-wide scan.  The user picks a folder (typically their
     * external storage root or a specific app data dir) with OpenDocumentTree
     * and we walk it recursively, flagging temp-like files: .tmp/.log/.bak
     * extensions, `thumbnails` directories, 0-byte files older than 7 days.
     */
    fun safScanAndClean(treeUri: Uri, confirmDelete: Boolean) = viewModelScope.launch {
        _state.value = _state.value.copy(
            safScanning = true,
            safScannedBytes = 0,
            safMatchCount = 0,
            safRootUri = treeUri.toString()
        )
        val (bytes, count, deleted) = withContext(Dispatchers.IO) {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext Triple(0L, 0, 0)
            var totalBytes = 0L
            var matches = 0
            var deletedCount = 0
            val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
            fun walk(node: DocumentFile) {
                val children = node.listFiles()
                for (f in children) {
                    if (f.isDirectory) {
                        if (f.name?.lowercase()?.endsWith("thumbnails") == true) {
                            // Cull thumbnail caches wholesale.
                            f.listFiles().forEach { child ->
                                totalBytes += child.length()
                                matches++
                                if (confirmDelete && child.delete()) deletedCount++
                            }
                        } else {
                            walk(f)
                        }
                    } else {
                        val n = f.name?.lowercase().orEmpty()
                        val isTemp = n.endsWith(".tmp") || n.endsWith(".log") ||
                            n.endsWith(".bak") || n.endsWith(".crdownload") ||
                            n.endsWith(".part") || n.endsWith(".cache")
                        val isStaleZero = f.length() == 0L && f.lastModified() in 1..sevenDaysAgo
                        if (isTemp || isStaleZero) {
                            totalBytes += f.length()
                            matches++
                            if (confirmDelete && f.delete()) deletedCount++
                        }
                    }
                }
            }
            walk(root)
            Triple(totalBytes, matches, deletedCount)
        }
        _state.value = _state.value.copy(
            safScanning = false,
            safScannedBytes = bytes,
            safMatchCount = count,
            lastAction = if (confirmDelete)
                "SAF clean: smazáno $deleted položek (~${bytes / 1024} KB)"
            else
                "SAF scan: $count kandidátů (~${bytes / 1024} KB)"
        )
        telemetry.toolRun(if (confirmDelete) "cache_saf_clean" else "cache_saf_scan", true)
    }

    private fun dirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun wipeDir(dir: File?) {
        if (dir == null || !dir.exists()) return
        dir.listFiles()?.forEach { it.deleteRecursively() }
    }
}
