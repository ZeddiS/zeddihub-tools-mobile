package com.zeddihub.mobile.ui.tools

import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.telemetry.TelemetryRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Správce úložiště — scoped-storage friendly storage overview.
 *
 *  - Uses MediaStore.Files to enumerate user-visible files so we don't need
 *    MANAGE_EXTERNAL_STORAGE (keeps the app on the Play Store happy path).
 *  - Categorises by MIME super-type, computes totals, surfaces the largest
 *    files and probable duplicate clusters (same size + same filename).
 *  - Actual delete goes through MediaStore.createDeleteRequest so the system
 *    dialog handles the scoped-storage grant — see StorageManagerScreen.
 */
@HiltViewModel
class StorageManagerViewModel @Inject constructor(
    application: Application,
    private val telemetry: TelemetryRecorder
) : AndroidViewModel(application) {

    enum class Category { IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, OTHER }

    data class FileItem(
        val id: Long,
        val uri: Uri,
        val displayName: String,
        val sizeBytes: Long,
        val category: Category,
        val mime: String?,
        val dateAdded: Long, // seconds since epoch
        val relativePath: String?
    )

    data class CategoryTotal(
        val category: Category,
        val count: Int,
        val totalBytes: Long
    )

    data class DuplicateCluster(
        val name: String,
        val sizeBytes: Long,
        val items: List<FileItem>
    )

    data class UiState(
        val loading: Boolean = false,
        val totalAllBytes: Long = 0,
        val deviceTotalBytes: Long = 0,
        val deviceFreeBytes: Long = 0,
        val totals: List<CategoryTotal> = emptyList(),
        val largest: List<FileItem> = emptyList(),
        val duplicates: List<DuplicateCluster> = emptyList(),
        val selected: Set<Long> = emptySet(),
        val lastScanAt: Long = 0L
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { scan() }

    fun scan() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val app = getApplication<Application>()
            val all = withContext(Dispatchers.IO) { queryMediaStore(app) }
            val totals = all.groupBy { it.category }
                .map { (c, list) ->
                    CategoryTotal(c, list.size, list.sumOf { it.sizeBytes })
                }
                .sortedByDescending { it.totalBytes }

            val largest = all.sortedByDescending { it.sizeBytes }.take(50)
            val duplicates = all
                .groupBy { it.displayName.lowercase() to it.sizeBytes }
                .filter { it.value.size > 1 && it.key.second > 0 }
                .map { (k, v) ->
                    DuplicateCluster(name = v.first().displayName, sizeBytes = k.second, items = v)
                }
                .sortedByDescending { it.sizeBytes * it.items.size }
                .take(30)

            val (devTotal, devFree) = readDeviceStorage()

            _state.value = UiState(
                loading = false,
                totalAllBytes = all.sumOf { it.sizeBytes },
                deviceTotalBytes = devTotal,
                deviceFreeBytes = devFree,
                totals = totals,
                largest = largest,
                duplicates = duplicates,
                selected = emptySet(),
                lastScanAt = System.currentTimeMillis()
            )
            telemetry.toolRun("storage_manager_scan", true)
        }
    }

    fun toggleSelect(id: Long) {
        val s = _state.value.selected
        _state.value = _state.value.copy(
            selected = if (id in s) s - id else s + id
        )
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selected = emptySet())
    }

    fun selectedUris(): List<Uri> {
        val s = _state.value
        val index = (s.largest + s.duplicates.flatMap { it.items }).associateBy { it.id }
        return s.selected.mapNotNull { index[it]?.uri }
    }

    /** Called after the system delete dialog resolves successfully. */
    fun onDeleteConfirmed() {
        // Re-scan to reflect the reality on disk.
        scan()
    }

    private fun queryMediaStore(ctx: Context): List<FileItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Files.getContentUri("external")
        }
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Files.FileColumns.RELATIVE_PATH
            else
                @Suppress("DEPRECATION") MediaStore.Files.FileColumns.DATA
        )
        val sort = "${MediaStore.Files.FileColumns.SIZE} DESC"
        val out = ArrayList<FileItem>(256)

        ctx.contentResolver.query(collection, projection, null, null, sort)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val mediaTypeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH)
            else
                c.getColumnIndexOrThrow(@Suppress("DEPRECATION") MediaStore.Files.FileColumns.DATA)

            while (c.moveToNext()) {
                val size = c.getLong(sizeCol)
                if (size <= 0) continue
                val id = c.getLong(idCol)
                val name = c.getString(nameCol) ?: "?"
                val mime = c.getString(mimeCol)
                val mediaType = c.getInt(mediaTypeCol)
                val cat = categoryFor(mime, mediaType, name)
                val uri = ContentUris.withAppendedId(collection, id)
                out.add(
                    FileItem(
                        id = id,
                        uri = uri,
                        displayName = name,
                        sizeBytes = size,
                        category = cat,
                        mime = mime,
                        dateAdded = c.getLong(dateCol),
                        relativePath = c.getString(pathCol)
                    )
                )
            }
        }
        return out
    }

    private fun categoryFor(mime: String?, mediaType: Int, name: String): Category {
        when (mediaType) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> return Category.IMAGE
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> return Category.VIDEO
            MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> return Category.AUDIO
            MediaStore.Files.FileColumns.MEDIA_TYPE_DOCUMENT -> return Category.DOCUMENT
        }
        val m = mime?.lowercase().orEmpty()
        return when {
            m.startsWith("image/") -> Category.IMAGE
            m.startsWith("video/") -> Category.VIDEO
            m.startsWith("audio/") -> Category.AUDIO
            m in setOf("application/pdf", "text/plain", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") -> Category.DOCUMENT
            m in setOf("application/zip", "application/x-rar-compressed", "application/x-7z-compressed",
                "application/x-tar", "application/gzip") -> Category.ARCHIVE
            name.endsWith(".zip", true) || name.endsWith(".rar", true) ||
                name.endsWith(".7z", true) || name.endsWith(".tar", true) ||
                name.endsWith(".gz", true) -> Category.ARCHIVE
            else -> Category.OTHER
        }
    }

    private fun readDeviceStorage(): Pair<Long, Long> = runCatching {
        val root = Environment.getExternalStorageDirectory()
        val stat = StatFs(root.path)
        val total = stat.blockCountLong * stat.blockSizeLong
        val free = stat.availableBlocksLong * stat.blockSizeLong
        total to free
    }.getOrDefault(0L to 0L)
}
