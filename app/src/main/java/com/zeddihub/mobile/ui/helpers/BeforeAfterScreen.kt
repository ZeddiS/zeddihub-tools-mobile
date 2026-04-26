package com.zeddihub.mobile.ui.helpers

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeddihub.mobile.R
import java.io.OutputStream

/**
 * Before / After photo composer.
 *
 * The user picks two photos. We render them in a single 16:9 canvas
 * with three modes:
 *   • SPLIT — fixed left half = Before, right half = After.
 *   • REVEAL — interactive vertical seam the user drags; left of the
 *     seam = Before, right = After. Lets the user wipe between the
 *     two states like a property-listing slider.
 *   • STACKED — top = Before, bottom = After (variant for portrait
 *     subjects where vertical comparison makes more sense).
 *
 * Each side gets its own jas / kontrast / saturace sliders applied via
 * an Android ColorMatrix at draw time. Cropping is fit-cover with a
 * pan offset that the user can drag per-side. Labels ("BEFORE" /
 * "AFTER") are drawn into the canvas so the export bakes them in.
 *
 * Export saves a JPEG to MediaStore Pictures/ZeddiHub. We don't gate
 * this behind the SOON badge — it's a finished tool from v0.8.0.
 */
@Composable
fun BeforeAfterScreen(padding: PaddingValues) {
    val ctx = LocalContext.current

    var beforeUri by remember { mutableStateOf<Uri?>(null) }
    var afterUri  by remember { mutableStateOf<Uri?>(null) }
    var beforeBmp by remember { mutableStateOf<Bitmap?>(null) }
    var afterBmp  by remember { mutableStateOf<Bitmap?>(null) }

    val pickBefore = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            beforeUri = uri
            beforeBmp = decodeScaled(ctx, uri, 1600)
        }
    }
    val pickAfter = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            afterUri = uri
            afterBmp = decodeScaled(ctx, uri, 1600)
        }
    }

    var mode by remember { mutableStateOf(BAMode.SPLIT) }
    var revealFraction by remember { mutableStateOf(0.5f) } // for REVEAL mode

    // Per-side colour adjustments (-1..+1 normalised)
    var bBrightness by remember { mutableStateOf(0f) }
    var bContrast   by remember { mutableStateOf(0f) }
    var bSaturation by remember { mutableStateOf(0f) }
    var aBrightness by remember { mutableStateOf(0f) }
    var aContrast   by remember { mutableStateOf(0f) }
    var aSaturation by remember { mutableStateOf(0f) }

    // Per-side pan offset (drag the photo around inside its half)
    var bPan by remember { mutableStateOf(Offset.Zero) }
    var aPan by remember { mutableStateOf(Offset.Zero) }

    var beforeLabel by remember { mutableStateOf("BEFORE") }
    var afterLabel  by remember { mutableStateOf("AFTER") }

    var saveStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.ba_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.ba_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pickBefore.launch(pickImagesOnly()) }) {
                Text(stringResource(R.string.ba_pick_before))
            }
            Button(onClick = { pickAfter.launch(pickImagesOnly()) }) {
                Text(stringResource(R.string.ba_pick_after))
            }
        }

        // Layout mode picker
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BAMode.values().forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(stringResource(m.labelRes)) }
                )
            }
        }

        // The composition canvas. Always 16:9; drag handlers live here.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.DarkGray, RoundedCornerShape(8.dp))
                .pointerInput(mode, beforeBmp, afterBmp) {
                    if (mode == BAMode.REVEAL) {
                        detectDragGestures { change, _ ->
                            val nw = (change.position.x / size.width).coerceIn(0f, 1f)
                            revealFraction = nw
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawBeforeAfter(
                    before = beforeBmp,
                    after = afterBmp,
                    mode = mode,
                    revealFraction = revealFraction,
                    bAdjust = ColorAdjust(bBrightness, bContrast, bSaturation),
                    aAdjust = ColorAdjust(aBrightness, aContrast, aSaturation),
                    bPan = bPan, aPan = aPan,
                    beforeLabel = beforeLabel,
                    afterLabel = afterLabel,
                )
            }
        }

        // Per-side adjustment sliders. Two columns side-by-side feels
        // intuitive: the slider on the left adjusts the left half of
        // the canvas, and vice versa.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(beforeLabel, fontWeight = FontWeight.SemiBold)
                LabeledSlider(stringResource(R.string.ba_brightness), bBrightness) { bBrightness = it }
                LabeledSlider(stringResource(R.string.ba_contrast),   bContrast)   { bContrast = it }
                LabeledSlider(stringResource(R.string.ba_saturation), bSaturation) { bSaturation = it }
                OutlinedButton(onClick = { bPan = Offset.Zero; bBrightness = 0f; bContrast = 0f; bSaturation = 0f }) {
                    Text(stringResource(R.string.ba_reset))
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(afterLabel, fontWeight = FontWeight.SemiBold)
                LabeledSlider(stringResource(R.string.ba_brightness), aBrightness) { aBrightness = it }
                LabeledSlider(stringResource(R.string.ba_contrast),   aContrast)   { aContrast = it }
                LabeledSlider(stringResource(R.string.ba_saturation), aSaturation) { aSaturation = it }
                OutlinedButton(onClick = { aPan = Offset.Zero; aBrightness = 0f; aContrast = 0f; aSaturation = 0f }) {
                    Text(stringResource(R.string.ba_reset))
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.ba_labels), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = beforeLabel, onValueChange = { beforeLabel = it.take(20) },
                        label = { Text("Before") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = afterLabel, onValueChange = { afterLabel = it.take(20) },
                        label = { Text("After") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Button(
            enabled = beforeBmp != null && afterBmp != null,
            onClick = {
                val out = renderToBitmap(
                    beforeBmp, afterBmp, mode, revealFraction,
                    ColorAdjust(bBrightness, bContrast, bSaturation),
                    ColorAdjust(aBrightness, aContrast, aSaturation),
                    bPan, aPan, beforeLabel, afterLabel,
                )
                if (out != null) {
                    val ok = saveBitmapToGallery(ctx, out)
                    saveStatus = if (ok) ctx.getString(R.string.ba_saved)
                    else ctx.getString(R.string.ba_save_failed)
                }
            }
        ) { Text(stringResource(R.string.ba_save_button)) }

        saveStatus?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun pickImagesOnly() = PickVisualMediaRequest(
    ActivityResultContracts.PickVisualMedia.ImageOnly
)

@Composable
private fun LabeledSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Text("$label  ${"%.2f".format(value)}",
            style = MaterialTheme.typography.labelSmall)
        Slider(
            value = value, onValueChange = onChange,
            valueRange = -1f..1f,
        )
    }
}

private enum class BAMode(val labelRes: Int) {
    SPLIT(R.string.ba_mode_split),
    REVEAL(R.string.ba_mode_reveal),
    STACKED(R.string.ba_mode_stacked),
}

private data class ColorAdjust(val brightness: Float, val contrast: Float, val saturation: Float)

/**
 * Build the ColorMatrix for one side. brightness in [-1..1] → translate
 * channels by ±255; contrast normalised to (0..2) scale; saturation 0
 * = greyscale, 1 = unchanged, 2 = oversaturated.
 */
private fun ColorAdjust.toMatrix(): ColorMatrix {
    val cm = ColorMatrix()
    // Saturation 0..2 (1 = identity)
    cm.setSaturation(1f + saturation.coerceIn(-1f, 1f))
    // Contrast: scale chans, recenter at 128
    val cf = 1f + contrast.coerceIn(-1f, 1f)
    val translate = (-0.5f * cf + 0.5f) * 255f
    val contrastM = ColorMatrix(floatArrayOf(
        cf, 0f, 0f, 0f, translate,
        0f, cf, 0f, 0f, translate,
        0f, 0f, cf, 0f, translate,
        0f, 0f, 0f, 1f, 0f,
    ))
    cm.postConcat(contrastM)
    // Brightness: simple translate
    val bf = brightness.coerceIn(-1f, 1f) * 255f
    val brightM = ColorMatrix(floatArrayOf(
        1f, 0f, 0f, 0f, bf,
        0f, 1f, 0f, 0f, bf,
        0f, 0f, 1f, 0f, bf,
        0f, 0f, 0f, 1f, 0f,
    ))
    cm.postConcat(brightM)
    return cm
}

/**
 * Compose canvas drawing the two photos according to mode. For SPLIT
 * we clipRect into halves; for REVEAL we draw the AFTER full-frame
 * then layer the BEFORE on top up to revealFraction. STACKED is a
 * vertical split.
 */
private fun DrawScope.drawBeforeAfter(
    before: Bitmap?, after: Bitmap?, mode: BAMode, revealFraction: Float,
    bAdjust: ColorAdjust, aAdjust: ColorAdjust,
    bPan: Offset, aPan: Offset,
    beforeLabel: String, afterLabel: String,
) {
    val w = size.width
    val h = size.height
    val nc = drawContext.canvas.nativeCanvas

    val bPaint = Paint().apply {
        isAntiAlias = true; isFilterBitmap = true
        colorFilter = ColorMatrixColorFilter(bAdjust.toMatrix())
    }
    val aPaint = Paint().apply {
        isAntiAlias = true; isFilterBitmap = true
        colorFilter = ColorMatrixColorFilter(aAdjust.toMatrix())
    }

    when (mode) {
        BAMode.SPLIT -> {
            clipRect(0f, 0f, w / 2, h) {
                if (before != null) drawCover(nc, before, bPaint, w, h, bPan)
            }
            clipRect(w / 2, 0f, w, h) {
                if (after != null) drawCover(nc, after, aPaint, w, h, aPan)
            }
            // Vertical seam line so the split is unambiguous.
            drawLine(Color.White, Offset(w / 2, 0f), Offset(w / 2, h), 3f)
        }
        BAMode.REVEAL -> {
            if (after != null) drawCover(nc, after, aPaint, w, h, aPan)
            val rx = (w * revealFraction).coerceIn(0f, w)
            clipRect(0f, 0f, rx, h) {
                if (before != null) drawCover(nc, before, bPaint, w, h, bPan)
            }
            drawLine(Color.White, Offset(rx, 0f), Offset(rx, h), 4f)
            // Handle dot in the centre
            drawCircle(Color.White, 18f, Offset(rx, h / 2))
            drawCircle(Color.Black, 14f, Offset(rx, h / 2))
        }
        BAMode.STACKED -> {
            clipRect(0f, 0f, w, h / 2) {
                if (before != null) drawCover(nc, before, bPaint, w, h, bPan)
            }
            clipRect(0f, h / 2, w, h) {
                if (after != null) drawCover(nc, after, aPaint, w, h, aPan)
            }
            drawLine(Color.White, Offset(0f, h / 2), Offset(w, h / 2), 3f)
        }
    }

    // Bake the BEFORE/AFTER labels into the canvas (so export keeps them).
    val labelPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = h * 0.05f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
    }
    val pad = h * 0.04f
    when (mode) {
        BAMode.SPLIT, BAMode.REVEAL -> {
            nc.drawText(beforeLabel, pad, h - pad, labelPaint)
            val aw = labelPaint.measureText(afterLabel)
            nc.drawText(afterLabel, w - aw - pad, h - pad, labelPaint)
        }
        BAMode.STACKED -> {
            nc.drawText(beforeLabel, pad, pad + labelPaint.textSize, labelPaint)
            nc.drawText(afterLabel, pad, h - pad, labelPaint)
        }
    }
}

/** Draw the bitmap fitting cover-style into (w,h) with a pan offset. */
private fun drawCover(nc: AndroidCanvas, bmp: Bitmap, paint: Paint, w: Float, h: Float, pan: Offset) {
    val bw = bmp.width.toFloat()
    val bh = bmp.height.toFloat()
    val canvasAr = w / h
    val bmpAr = bw / bh
    // Cover scale: stretch to fill the larger axis, the other gets cropped.
    val scale = if (bmpAr > canvasAr) h / bh else w / bw
    val outW = bw * scale
    val outH = bh * scale
    val dx = (w - outW) / 2 + pan.x
    val dy = (h - outH) / 2 + pan.y
    val src = Rect(0, 0, bmp.width, bmp.height)
    val dst = android.graphics.RectF(dx, dy, dx + outW, dy + outH)
    nc.drawBitmap(bmp, src, dst, paint)
}

/**
 * Bake the current state of the canvas into a fresh Bitmap for export.
 * 1080p output is plenty for social media; rendering at canvas size
 * would inherit screen DP and look fuzzy after upload.
 */
private fun renderToBitmap(
    before: Bitmap?, after: Bitmap?, mode: BAMode, revealFraction: Float,
    bAdjust: ColorAdjust, aAdjust: ColorAdjust,
    bPan: Offset, aPan: Offset,
    beforeLabel: String, afterLabel: String,
): Bitmap? {
    if (before == null || after == null) return null
    val w = 1920; val h = 1080
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val nc = AndroidCanvas(out)

    val bPaint = Paint().apply {
        isAntiAlias = true; isFilterBitmap = true
        colorFilter = ColorMatrixColorFilter(bAdjust.toMatrix())
    }
    val aPaint = Paint().apply {
        isAntiAlias = true; isFilterBitmap = true
        colorFilter = ColorMatrixColorFilter(aAdjust.toMatrix())
    }
    nc.drawColor(android.graphics.Color.BLACK)

    when (mode) {
        BAMode.SPLIT -> {
            val saveId = nc.save(); nc.clipRect(0, 0, w / 2, h)
            drawCover(nc, before, bPaint, w.toFloat(), h.toFloat(), bPan)
            nc.restoreToCount(saveId)
            val saveId2 = nc.save(); nc.clipRect(w / 2, 0, w, h)
            drawCover(nc, after, aPaint, w.toFloat(), h.toFloat(), aPan)
            nc.restoreToCount(saveId2)
        }
        BAMode.REVEAL -> {
            drawCover(nc, after, aPaint, w.toFloat(), h.toFloat(), aPan)
            val rx = (w * revealFraction).toInt().coerceIn(0, w)
            val saveId = nc.save(); nc.clipRect(0, 0, rx, h)
            drawCover(nc, before, bPaint, w.toFloat(), h.toFloat(), bPan)
            nc.restoreToCount(saveId)
        }
        BAMode.STACKED -> {
            val saveId = nc.save(); nc.clipRect(0, 0, w, h / 2)
            drawCover(nc, before, bPaint, w.toFloat(), h.toFloat(), bPan)
            nc.restoreToCount(saveId)
            val saveId2 = nc.save(); nc.clipRect(0, h / 2, w, h)
            drawCover(nc, after, aPaint, w.toFloat(), h.toFloat(), aPan)
            nc.restoreToCount(saveId2)
        }
    }

    val labelPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = h * 0.05f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLACK)
    }
    val pad = h * 0.04f
    when (mode) {
        BAMode.SPLIT, BAMode.REVEAL -> {
            nc.drawText(beforeLabel, pad, h - pad, labelPaint)
            val aw = labelPaint.measureText(afterLabel)
            nc.drawText(afterLabel, w - aw - pad, h - pad, labelPaint)
        }
        BAMode.STACKED -> {
            nc.drawText(beforeLabel, pad, pad + labelPaint.textSize, labelPaint)
            nc.drawText(afterLabel, pad, h - pad, labelPaint)
        }
    }

    return out
}

/** Save to MediaStore Pictures/ZeddiHub on API 29+, fall back to the
 *  external Pictures dir on legacy. JPEG quality 92 — visually
 *  lossless for a social-media post. */
private fun saveBitmapToGallery(
    ctx: android.content.Context, bmp: Bitmap,
): Boolean {
    val name = "ZeddiHub-BeforeAfter-${System.currentTimeMillis()}.jpg"
    return runCatching {
        if (Build.VERSION.SDK_INT >= 29) {
            val cv = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/ZeddiHub")
            }
            val resolver = ctx.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                ?: return@runCatching false
            val out: OutputStream = resolver.openOutputStream(uri)
                ?: return@runCatching false
            out.use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            true
        } else {
            // Legacy path. Requires WRITE_EXTERNAL_STORAGE which the
            // manifest already declares for ≤API 28.
            @Suppress("DEPRECATION")
            val dir = java.io.File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "ZeddiHub"
            )
            if (!dir.exists()) dir.mkdirs()
            val file = java.io.File(dir, name)
            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            true
        }
    }.getOrDefault(false)
}

/** Decode a content-uri photo with downsampling to keep memory sane. */
private fun decodeScaled(ctx: android.content.Context, uri: Uri, maxEdge: Int): Bitmap? {
    return runCatching {
        // First pass: bounds-only to read dimensions.
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
        val w = opts.outWidth; val h = opts.outHeight
        if (w <= 0 || h <= 0) return@runCatching null
        // Pick the largest power-of-2 inSampleSize that keeps both
        // edges ≤ maxEdge. Camera shots are commonly 4000×3000 — at
        // maxEdge=1600 we end up with sample 4 → ~1000×750, which is
        // fast to render and still sharp at 1080p export.
        var sample = 1
        while (w / sample > maxEdge || h / sample > maxEdge) sample *= 2
        val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
        ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts2)
        }
    }.getOrNull()
}
