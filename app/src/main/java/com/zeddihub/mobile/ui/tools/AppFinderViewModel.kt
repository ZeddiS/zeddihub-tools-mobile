package com.zeddihub.mobile.ui.tools

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AppFinderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telemetry: TelemetryRecorder
) : ViewModel() {

    enum class SortBy { SIZE_DESC, SIZE_ASC, NAME_ASC }

    data class AppEntry(
        val packageName: String,
        val label: String,
        val apkSize: Long,
        val dataSize: Long,
        val totalSize: Long,
        val isSystem: Boolean,
        val icon: Drawable?,
        val installedAt: Long,
        val updatedAt: Long
    )

    data class UiState(
        val loading: Boolean = true,
        val apps: List<AppEntry> = emptyList(),
        val filter: String = "",
        val sort: SortBy = SortBy.SIZE_DESC,
        val includeSystem: Boolean = false,
        val totalBytes: Long = 0
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { reload() }

    fun reload() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true)
        val apps = withContext(Dispatchers.IO) { loadApps() }
        _state.value = _state.value.copy(
            loading = false,
            apps = apps,
            totalBytes = apps.sumOf { it.totalSize }
        )
        telemetry.toolRun("app_finder_scan", true)
    }

    fun setFilter(q: String) { _state.value = _state.value.copy(filter = q) }
    fun setSort(s: SortBy) { _state.value = _state.value.copy(sort = s) }
    fun setIncludeSystem(v: Boolean) { _state.value = _state.value.copy(includeSystem = v) }

    fun openApp(pkg: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    fun openAppSettings(pkg: String) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.parse("package:$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun loadApps(): List<AppEntry> {
        // Launcher-only: QUERY_ALL_PACKAGES not required.
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolved = pm.queryIntentActivities(launcher, 0)
        val seen = HashSet<String>()
        return resolved.mapNotNull { ri ->
            val pkg = ri.activityInfo.packageName
            if (!seen.add(pkg)) return@mapNotNull null
            runCatching {
                val info = pm.getApplicationInfo(pkg, 0)
                val pkgInfo = pm.getPackageInfo(pkg, 0)
                val apkSize = File(info.sourceDir).length()
                val dataSize = dirSize(info.dataDir?.let(::File))
                AppEntry(
                    packageName = pkg,
                    label = info.loadLabel(pm).toString(),
                    apkSize = apkSize,
                    dataSize = dataSize,
                    totalSize = apkSize + dataSize,
                    isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    icon = runCatching { info.loadIcon(pm) }.getOrNull(),
                    installedAt = pkgInfo.firstInstallTime,
                    updatedAt = pkgInfo.lastUpdateTime
                )
            }.getOrNull()
        }
    }

    private fun dirSize(dir: File?): Long {
        if (dir == null || !dir.exists() || !dir.canRead()) return 0L
        return runCatching {
            dir.walkTopDown().filter { it.isFile && it.canRead() }.sumOf { it.length() }
        }.getOrDefault(0L)
    }
}
