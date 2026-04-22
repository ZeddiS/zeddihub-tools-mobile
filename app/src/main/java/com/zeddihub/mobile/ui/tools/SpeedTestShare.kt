package com.zeddihub.mobile.ui.tools

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders the speedtest result (ping/jitter/DL/UL + meta) to a shareable PNG
 * and launches a share chooser. The bitmap is drawn manually via the Android
 * 2D canvas so the output is the same regardless of screen density.
 */
object SpeedTestShare {

    /** Legacy alias — new code should call [shareResultAsImage] directly. */
    fun shareResult(context: Context, state: SpeedTestViewModel.UiState, shareTitle: String) {
        shareResultAsImage(context, state, shareTitle)
    }

    fun shareResultAsImage(
        context: Context,
        state: SpeedTestViewModel.UiState,
        shareTitle: String
    ) {
        val bitmap = renderBitmap(state)
        val dir = File(context.cacheDir, "speedtest").apply { mkdirs() }
        val file = File(dir, "speedtest-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val summary = buildSummaryText(state)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, summary)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, shareTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun shareResultAsText(
        context: Context,
        state: SpeedTestViewModel.UiState,
        shareTitle: String
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ZeddiHub SpeedTest")
            putExtra(Intent.EXTRA_TEXT, buildDetailedText(state))
        }
        context.startActivity(
            Intent.createChooser(intent, shareTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun shareResultAsPdf(
        context: Context,
        state: SpeedTestViewModel.UiState,
        shareTitle: String
    ) {
        val bitmap = renderBitmap(state)
        val dir = File(context.cacheDir, "speedtest").apply { mkdirs() }
        val file = File(dir, "speedtest-${System.currentTimeMillis()}.pdf")

        // A4 @ 72dpi ≈ 595x842pt. Our bitmap is 1080x1350; we center it on the page.
        val pageWidth = 595
        val pageHeight = 842
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // White PDF background
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        // Scale bitmap to fit width with 40pt margin
        val margin = 40f
        val targetW = pageWidth - 2 * margin
        val scale = targetW / bitmap.width
        val targetH = bitmap.height * scale
        val scaled = Bitmap.createScaledBitmap(bitmap, targetW.toInt(), targetH.toInt(), true)
        val top = (pageHeight - targetH) / 2f
        canvas.drawBitmap(scaled, margin, top, paint)

        doc.finishPage(page)
        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "ZeddiHub SpeedTest")
            putExtra(Intent.EXTRA_TEXT, buildSummaryText(state))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, shareTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun buildSummaryText(s: SpeedTestViewModel.UiState): String {
        val parts = mutableListOf<String>()
        parts += "ZeddiHub SpeedTest"
        s.pingMs?.let { parts += "ping %.0f ms".format(Locale.US, it) }
        s.jitterMs?.let { parts += "jitter %.1f ms".format(Locale.US, it) }
        s.downloadMbps?.let { parts += "↓ %.1f Mbps".format(Locale.US, it) }
        s.uploadMbps?.let { parts += "↑ %.1f Mbps".format(Locale.US, it) }
        s.isp?.let { parts += it }
        return parts.joinToString("  ·  ")
    }

    private fun buildDetailedText(s: SpeedTestViewModel.UiState): String = buildString {
        appendLine("ZeddiHub SpeedTest")
        appendLine("─".repeat(28))
        s.finishedAt?.let {
            appendLine("Čas: " + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(it)))
        }
        s.connectionType?.let {
            val conn = buildString {
                append(it)
                if (!s.ssid.isNullOrBlank()) append(" · ${s.ssid}")
                s.rssi?.let { r -> append(" · $r dBm") }
            }
            appendLine("Síť: $conn")
        }
        appendLine()
        s.pingMs?.let { appendLine("Ping:     %.0f ms".format(Locale.US, it)) }
        s.pingMin?.let { appendLine("  Min:    %.1f ms".format(Locale.US, it)) }
        s.pingAvg?.let { appendLine("  Prům.:  %.1f ms".format(Locale.US, it)) }
        s.pingMax?.let { appendLine("  Max:    %.1f ms".format(Locale.US, it)) }
        s.jitterMs?.let { appendLine("Jitter:   %.1f ms".format(Locale.US, it)) }
        s.lossPct?.let { appendLine("Ztráta:   %.1f %%".format(Locale.US, it)) }
        appendLine()
        s.downloadMbps?.let { appendLine("Download: %.1f Mbps".format(Locale.US, it)) }
        s.uploadMbps?.let { appendLine("Upload:   %.1f Mbps".format(Locale.US, it)) }
        appendLine()
        s.isp?.let { appendLine("ISP:      $it") }
        s.ip?.let { appendLine("IP:       $it") }
        s.server?.let { appendLine("Server:   $it") }
        s.city?.let { appendLine("Lokalita: $it") }
        appendLine()
        appendLine("https://zeddihub.eu/tools")
    }

    // ─────────────── Bitmap ───────────────

    private const val W = 1080
    private const val H = 1350
    private const val BG = 0xFF0A0A0F.toInt()
    private const val CARD_BG = 0xFF13131B.toInt()
    private const val BORDER = 0xFF1F1F2A.toInt()
    private const val TEXT = 0xFFF5F5F7.toInt()
    private const val TEXT_DIM = 0xFF7A7A8A.toInt()
    private const val PRIMARY = 0xFFF0A500.toInt()
    private const val PRIMARY_HI = 0xFFFFB91F.toInt()
    private const val GREEN = 0xFF22C55E.toInt()
    private const val BLUE = 0xFF3B82F6.toInt()
    private const val RED = 0xFFEF4444.toInt()

    private fun renderBitmap(s: SpeedTestViewModel.UiState): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background gradient
        p.shader = LinearGradient(
            0f, 0f, 0f, H.toFloat(),
            intArrayOf(0xFF14141F.toInt(), BG),
            null, Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        p.shader = null

        // Title
        p.color = TEXT
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = 68f
        c.drawText("ZeddiHub SpeedTest", 80f, 130f, p)

        // Timestamp / server
        p.color = TEXT_DIM
        p.typeface = Typeface.DEFAULT
        p.textSize = 34f
        val stamp = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            .format(Date(s.finishedAt ?: System.currentTimeMillis()))
        val metaLine = buildList {
            add(stamp)
            s.server?.let { add(it) }
            s.city?.let { add(it) }
        }.joinToString("   •   ")
        c.drawText(metaLine, 80f, 185f, p)

        // Gauge in center
        val gaugeTop = 230f
        val gaugeSize = 680f
        drawGauge(c, p, (W - gaugeSize) / 2f, gaugeTop, gaugeSize, s)

        // Four metrics row (Ping / Jitter / DL / UL)
        val rowY = gaugeTop + gaugeSize + 40f
        val cellW = (W - 160f) / 4f - 12f
        val metrics = listOf(
            Triple("PING", s.pingMs?.let { "%.0f".format(Locale.US, it) } ?: "—", GREEN) to "ms",
            Triple("JITTER", s.jitterMs?.let { "%.1f".format(Locale.US, it) } ?: "—", BLUE) to "ms",
            Triple("DOWNLOAD", s.downloadMbps?.let { "%.1f".format(Locale.US, it) } ?: "—", PRIMARY) to "Mbps",
            Triple("UPLOAD", s.uploadMbps?.let { "%.1f".format(Locale.US, it) } ?: "—", PRIMARY_HI) to "Mbps"
        )
        metrics.forEachIndexed { i, (m, unit) ->
            val x = 80f + i * (cellW + 16f)
            drawMetricCell(c, p, x, rowY, cellW, 190f, m.first, m.second, unit, m.third)
        }

        // Ping breakdown (min/avg/max)
        if (s.pingAttempts > 0) {
            p.color = TEXT_DIM
            p.typeface = Typeface.DEFAULT
            p.textSize = 30f
            val pingText = buildString {
                append("Ping ")
                s.pingMin?.let { append("min %.1f".format(Locale.US, it)) }
                s.pingAvg?.let {
                    if (length > 5) append("  ·  ")
                    append("prům. %.1f".format(Locale.US, it))
                }
                s.pingMax?.let {
                    if (length > 5) append("  ·  ")
                    append("max %.1f".format(Locale.US, it))
                }
                append(" ms")
            }
            c.drawText(pingText, 80f, rowY + 245f, p)

            val lossText = "Odezva: %d/%d odpovědí · ztráta %.1f%%".format(
                Locale.US, s.pingSuccess, s.pingAttempts, s.lossPct ?: 0.0
            )
            c.drawText(lossText, 80f, rowY + 285f, p)
        }

        // Connection conditions
        s.connectionType?.let {
            p.color = TEXT
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            p.textSize = 32f
            val connText = buildString {
                append(it)
                if (!s.ssid.isNullOrBlank()) append("  ·  ${s.ssid}")
                s.rssi?.let { r -> append("  ·  $r dBm") }
            }
            c.drawText(connText, 80f, rowY + 335f, p)
        }

        // ISP
        s.isp?.let {
            p.color = TEXT
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            p.textSize = 36f
            c.drawText(it, 80f, rowY + 385f, p)
        }
        s.ip?.let {
            p.color = TEXT_DIM
            p.typeface = Typeface.DEFAULT
            p.textSize = 30f
            c.drawText("IP: $it", 80f, rowY + 425f, p)
        }

        // Footer
        p.color = TEXT_DIM
        p.typeface = Typeface.DEFAULT
        p.textSize = 28f
        c.drawText("speed.cloudflare.com  •  zeddihub.eu", 80f, H - 60f, p)

        return bmp
    }

    private fun drawGauge(
        c: Canvas, p: Paint,
        left: Float, top: Float, size: Float, s: SpeedTestViewModel.UiState
    ) {
        val cx = left + size / 2f
        val cy = top + size / 2f + size * 0.02f
        val radius = size / 2f - 40f
        val stroke = 34f

        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        // Track
        p.style = Paint.Style.STROKE
        p.strokeWidth = stroke
        p.color = 0xFF1E1E28.toInt()
        c.drawArc(rect, 150f, 240f, false, p)

        // Filled portion (based on DL for final result)
        val value = s.downloadMbps ?: s.liveValue
        val gaugeMax = maxOf(100.0, (s.downloadMbps ?: s.gaugeMax) * 1.05)
        val fraction = (value / gaugeMax).coerceIn(0.0, 1.0).toFloat()
        p.color = PRIMARY
        c.drawArc(rect, 150f, 240f * fraction, false, p)

        // Tick marks
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f
        p.color = TEXT_DIM
        for (i in 0..10) {
            val t = i / 10.0
            val ang = Math.toRadians(150.0 + 240.0 * t)
            val rIn = radius - stroke - 12f
            val rOut = radius - stroke - 28f
            c.drawLine(
                cx + (cos(ang) * rIn).toFloat(),
                cy + (sin(ang) * rIn).toFloat(),
                cx + (cos(ang) * rOut).toFloat(),
                cy + (sin(ang) * rOut).toFloat(),
                p
            )
        }

        // Needle (drawn pointing right from center, then rotated)
        val rotDeg = 150f + 240f * fraction
        c.save()
        c.rotate(rotDeg, cx, cy)
        val needle = Path().apply {
            moveTo(cx, cy - 12f)
            lineTo(cx + radius - stroke - 8f, cy)
            lineTo(cx, cy + 12f)
            close()
        }
        p.style = Paint.Style.FILL
        p.color = PRIMARY
        c.drawPath(needle, p)
        c.restore()

        // Hub
        p.style = Paint.Style.FILL
        p.color = BG
        c.drawCircle(cx, cy, 22f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 7f
        p.color = PRIMARY
        c.drawCircle(cx, cy, 22f, p)

        // Big value
        p.style = Paint.Style.FILL
        p.color = TEXT
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = 130f
        val valTxt = if (value < 100) "%.2f".format(Locale.US, value) else "%.1f".format(Locale.US, value)
        val tw = p.measureText(valTxt)
        c.drawText(valTxt, cx - tw / 2f, cy + 35f, p)

        // Unit
        p.color = TEXT_DIM
        p.typeface = Typeface.DEFAULT
        p.textSize = 36f
        val unit = "Mbps"
        val uw = p.measureText(unit)
        c.drawText(unit, cx - uw / 2f, cy + 90f, p)
    }

    private fun drawMetricCell(
        c: Canvas, p: Paint,
        x: Float, y: Float, w: Float, h: Float,
        label: String, value: String, unit: String, accent: Int
    ) {
        val rect = RectF(x, y, x + w, y + h)
        p.style = Paint.Style.FILL
        p.color = CARD_BG
        c.drawRoundRect(rect, 28f, 28f, p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = BORDER
        c.drawRoundRect(rect, 28f, 28f, p)

        // Accent bar on top
        p.style = Paint.Style.FILL
        p.color = accent
        c.drawRect(x + 24f, y + 22f, x + 60f, y + 28f, p)

        p.color = TEXT_DIM
        p.typeface = Typeface.DEFAULT
        p.textSize = 26f
        c.drawText(label, x + 24f, y + 62f, p)

        p.color = TEXT
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = 54f
        c.drawText(value, x + 24f, y + 130f, p)

        p.color = TEXT_DIM
        p.typeface = Typeface.DEFAULT
        p.textSize = 28f
        c.drawText(unit, x + 24f, y + 168f, p)
    }

    @Suppress("unused")
    private fun clampMin(size: Float): Float = min(size, 1f)
}
