package com.zeddihub.mobile.data.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.zeddihub.mobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class ReleaseInfo(
    val tag: String,
    val name: String,
    val body: String,
    val apkUrl: String,
    val apkSize: Long
)

@Singleton
class UpdateChecker @Inject constructor() {

    // Primary: release-gating endpoint. Single source of truth, gated by
    // admin "Publikovat" — admin must explicitly publish a `pending`
    // build before users see it. See tools/admin/app_releases.php.
    private val gatingUrl = "${BuildConfig.API_BASE_URL}app-version.php?platform=mobile"
    // Legacy fallback: static manifest auto-generated from the gating
    // system after each publish (tools/admin/app_releases.php →
    // ar_sync_legacy_manifest()). Kept around because installs on
    // ≤ v0.7.2 still read this file directly.
    private val manifestUrl = "https://zeddihub.eu/tools/data/version_android.json"
    // Last-resort fallback: GitHub Releases API.
    private val releasesUrl = "https://api.github.com/repos/ZeddiS/zeddihub-tools-mobile/releases/latest"

    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        fetchGating() ?: fetchManifest() ?: fetchGitHubRelease()
    }

    /**
     * /api/app-version.php returns the *currently published* mobile
     * build only (rows in zh_app_releases with status='published'). If
     * no release is published, the endpoint returns ok=false and we
     * fall through to the legacy manifest. This guarantees we never
     * advertise a `pending` build to users by accident.
     */
    private fun fetchGating(): ReleaseInfo? = runCatching {
        val url = URL(gatingUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "ZeddiHub-Mobile/${BuildConfig.VERSION_NAME}")
        conn.requestMethod = "GET"
        if (conn.responseCode !in 200..299) return@runCatching null
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        if (!json.optBoolean("ok", false)) return@runCatching null
        val versionName = json.optString("version_name")
        if (versionName.isBlank()) return@runCatching null
        ReleaseInfo(
            tag = versionName,
            name = versionName,
            body = json.optString("release_notes_cs"),
            apkUrl = json.optString("apk_url"),
            apkSize = 0L
        )
    }.getOrNull()

    private fun fetchManifest(): ReleaseInfo? = runCatching {
        val url = URL(manifestUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "ZeddiHub-Mobile/${BuildConfig.VERSION_NAME}")
        conn.requestMethod = "GET"
        if (conn.responseCode !in 200..299) return@runCatching null
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val version = json.optString("version")
        if (version.isBlank()) return@runCatching null
        val downloadUrl = json.optString("download_url")
        val changelog = json.optString("changelog")
        val mandatory = json.optBoolean("mandatory", false)
        ReleaseInfo(
            tag = version,
            name = if (mandatory) "$version (mandatory)" else version,
            body = changelog,
            apkUrl = downloadUrl,
            apkSize = 0L
        )
    }.getOrNull()

    private fun fetchGitHubRelease(): ReleaseInfo? = runCatching {
        val url = URL(releasesUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "ZeddiHub-Mobile/${BuildConfig.VERSION_NAME}")
        conn.requestMethod = "GET"
        if (conn.responseCode !in 200..299) return@runCatching null
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val tag = json.optString("tag_name").ifEmpty { json.optString("name") }
        val assets: JSONArray = json.optJSONArray("assets") ?: JSONArray()
        var apkUrl = ""
        var apkSize = 0L
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.optString("name")
            if (name.endsWith(".apk", ignoreCase = true)) {
                apkUrl = a.optString("browser_download_url")
                apkSize = a.optLong("size")
                break
            }
        }
        ReleaseInfo(
            tag = tag,
            name = json.optString("name", tag),
            body = json.optString("body"),
            apkUrl = apkUrl,
            apkSize = apkSize
        )
    }.getOrNull()

    fun isNewer(info: ReleaseInfo): Boolean {
        val current = BuildConfig.VERSION_NAME
        val latest = info.tag.trimStart('v', 'V')
        return compareVersions(latest, current) > 0
    }

    suspend fun downloadApk(context: Context, url: String): File? = withContext(Dispatchers.IO) {
        runCatching {
            val out = File(context.getExternalFilesDir(null), "update.apk")
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 20000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "ZeddiHub-Mobile/${BuildConfig.VERSION_NAME}")
            conn.inputStream.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            out
        }.getOrNull()
    }

    fun installApk(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun compareVersions(a: String, b: String): Int {
        val pa = a.split('.', '-').mapNotNull { it.toIntOrNull() }
        val pb = b.split('.', '-').mapNotNull { it.toIntOrNull() }
        val n = maxOf(pa.size, pb.size)
        for (i in 0 until n) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }
}
