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

    private val releasesUrl = "https://api.github.com/repos/ZeddiS/zeddihub-tools-mobile/releases/latest"

    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
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
    }

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
