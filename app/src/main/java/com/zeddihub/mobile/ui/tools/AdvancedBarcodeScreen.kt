package com.zeddihub.mobile.ui.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AColor
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.camera.core.Camera
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.zeddihub.mobile.R
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Advanced barcode tool (Code 128, EAN-13, UPC-A, Code 39, ITF, PDF417, Data Matrix).
 *
 *  Tabs:
 *   - Generovat : picks format, validates input live, previews bitmap via ZXing's
 *                 [MultiFormatWriter], and offers save/share/copy actions.
 *   - Skenovat  : either pick an image (ZXing [MultiFormatReader]) or run a live
 *                 CameraX pipeline fed into ML Kit barcode scanner. The first
 *                 successful decode pops a bottom-sheet with quick actions.
 *   - Historie  : last 50 scans persisted in SharedPreferences, with CSV export
 *                 via SAF and individual quick-action re-opens.
 *
 * All state is local — no ViewModel needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedBarcodeScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }

    // History is shared across Scan + Historie tabs, so it lives at screen scope.
    var history by remember { mutableStateOf<List<BarcodeHistoryItem>>(emptyList()) }
    LaunchedEffect(Unit) { history = HistoryStore.load(ctx) }

    // Quick-action bottom sheet, can be triggered from Scan or Historie rows.
    var sheetItem by remember { mutableStateOf<BarcodeHistoryItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }) {
                Text(stringResource(R.string.bc_tab_generate), modifier = Modifier.padding(12.dp))
            }
            Tab(selected = tab == 1, onClick = { tab = 1 }) {
                Text(stringResource(R.string.bc_tab_scan), modifier = Modifier.padding(12.dp))
            }
            Tab(selected = tab == 2, onClick = { tab = 2 }) {
                Text(stringResource(R.string.bc_tab_history), modifier = Modifier.padding(12.dp))
            }
        }
        when (tab) {
            0 -> GenerateBarcodeTab()
            1 -> ScanBarcodeTab(
                onScanned = { format, payload ->
                    val item = BarcodeHistoryItem(System.currentTimeMillis(), format, payload)
                    val updated = HistoryStore.append(ctx, history, item)
                    history = updated
                    sheetItem = item
                }
            )
            2 -> HistoryTab(
                history = history,
                onRowClick = { sheetItem = it },
                onClear = {
                    HistoryStore.clear(ctx)
                    history = emptyList()
                },
                onExported = { /* toast handled inside */ }
            )
        }
    }

    sheetItem?.let { item ->
        QuickActionsSheet(
            item = item,
            onDismiss = { sheetItem = null }
        )
    }
}

// --- Generate tab -------------------------------------------------------

private enum class BcFormat(
    val label: String,
    val zxing: BarcodeFormat,
    val targetW: Int,
    val targetH: Int
) {
    CODE_128("Code 128", BarcodeFormat.CODE_128, 600, 220),
    EAN_13("EAN-13", BarcodeFormat.EAN_13, 600, 260),
    UPC_A("UPC-A", BarcodeFormat.UPC_A, 600, 260),
    CODE_39("Code 39", BarcodeFormat.CODE_39, 600, 220),
    ITF("ITF", BarcodeFormat.ITF, 600, 220),
    PDF_417("PDF417", BarcodeFormat.PDF_417, 600, 300),
    DATA_MATRIX("Data Matrix", BarcodeFormat.DATA_MATRIX, 400, 400);
}

@Composable
private fun GenerateBarcodeTab() {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current

    var format by remember { mutableStateOf(BcFormat.CODE_128) }
    var formatMenu by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var bitmapFor by remember { mutableStateOf<String?>(null) }
    var generateError by remember { mutableStateOf<String?>(null) }

    // Live validation — recomputed on every input or format change.
    val liveError: String? = remember(format, input) {
        if (input.isEmpty()) null else validateForFormat(format, input)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        // Format dropdown.
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { formatMenu = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.bc_format_label, format.label))
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(
                expanded = formatMenu,
                onDismissRequest = { formatMenu = false }
            ) {
                BcFormat.values().forEach { f ->
                    DropdownMenuItem(
                        text = { Text(f.label) },
                        onClick = {
                            format = f
                            formatMenu = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(stringResource(R.string.bc_data_label)) },
            isError = liveError != null,
            supportingText = {
                liveError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Button(
            enabled = input.isNotBlank() && liveError == null,
            onClick = {
                val normalized = normalizeForFormat(format, input)
                val result = runCatching { generateBarcode(format, normalized) }
                bitmap = result.getOrNull()
                bitmapFor = normalized
                generateError = result.exceptionOrNull()?.localizedMessage
            }
        ) {
            Text(stringResource(R.string.bc_generate))
        }

        generateError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        val bmp = bitmap
        val payload = bitmapFor
        if (bmp != null && payload != null) {
            // 1D barcodes are wide, 2D roughly square. Aspect ratio derived from bitmap.
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ElevatedButton(onClick = {
                    val file = saveBarcodeToPictures(ctx, bmp)
                    Toast.makeText(
                        ctx,
                        if (file != null) "Uloženo: ${file.name}" else "Uložení selhalo",
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.bc_save))
                }
                ElevatedButton(onClick = {
                    val file = saveBarcodeToCache(ctx, bmp)
                    if (file != null) {
                        val uri = FileProvider.getUriForFile(
                            ctx, "${ctx.packageName}.fileprovider", file
                        )
                        val intent = Intent(Intent.ACTION_SEND)
                            .setType("image/png")
                            .putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        ctx.startActivity(Intent.createChooser(intent, null))
                    }
                }) {
                    Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.bc_share))
                }
                ElevatedButton(onClick = {
                    clip.setText(AnnotatedString(payload))
                    Toast.makeText(ctx, "Zkopírováno", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.bc_copy))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                payload,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Validates [input] for [format] and returns an error message (Czech) or null if OK.
 *
 * Format-specific rules:
 *  - EAN-13: exactly 12 or 13 digits. If 12, the check digit will be auto-computed
 *            at generate time.
 *  - UPC-A : exactly 11 or 12 digits. Likewise auto-completes when 11.
 *  - Code 39: uppercase letters, digits, and `-. $/+%` only.
 *  - ITF   : digits only, even count (ITF pairs digits; odd count can't be encoded).
 *  - Others: free-form, any non-empty payload is acceptable.
 */
private fun validateForFormat(format: BcFormat, input: String): String? = when (format) {
    BcFormat.EAN_13 -> {
        if (!input.all { it.isDigit() }) "EAN-13: povoleny jsou pouze číslice"
        else if (input.length !in listOf(12, 13)) "EAN-13: očekávám 12 nebo 13 číslic"
        else if (input.length == 13 && !ean13CheckValid(input)) "EAN-13: neplatný kontrolní součet"
        else null
    }
    BcFormat.UPC_A -> {
        if (!input.all { it.isDigit() }) "UPC-A: povoleny jsou pouze číslice"
        else if (input.length !in listOf(11, 12)) "UPC-A: očekávám 11 nebo 12 číslic"
        else if (input.length == 12 && !upcaCheckValid(input)) "UPC-A: neplatný kontrolní součet"
        else null
    }
    BcFormat.CODE_39 -> {
        val allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-. $/+%"
        if (input.any { it !in allowed })
            "Code 39: povolené znaky A–Z, 0–9 a \"-. $/+%\""
        else null
    }
    BcFormat.ITF -> {
        if (!input.all { it.isDigit() }) "ITF: povoleny jsou pouze číslice"
        else if (input.length % 2 != 0) "ITF: počet číslic musí být sudý"
        else null
    }
    BcFormat.CODE_128, BcFormat.PDF_417, BcFormat.DATA_MATRIX -> null
}

/**
 * Normalizes [input] for encoding — in particular, appends auto-computed check
 * digits for EAN-13 (12→13) and UPC-A (11→12). Other formats pass through.
 */
private fun normalizeForFormat(format: BcFormat, input: String): String = when (format) {
    BcFormat.EAN_13 -> if (input.length == 12) input + ean13CheckDigit(input) else input
    BcFormat.UPC_A -> if (input.length == 11) input + upcaCheckDigit(input) else input
    else -> input
}

// --- EAN-13 / UPC-A check digit math -----------------------------------

private fun ean13CheckDigit(first12: String): Char {
    // Standard EAN-13 check: sum of digits at odd positions (1-indexed) + 3*even-pos.
    var sum = 0
    for (i in 0 until 12) {
        val d = first12[i].digitToInt()
        sum += if (i % 2 == 0) d else d * 3
    }
    val check = (10 - (sum % 10)) % 10
    return '0' + check
}

private fun ean13CheckValid(all13: String): Boolean =
    ean13CheckDigit(all13.substring(0, 12)) == all13[12]

private fun upcaCheckDigit(first11: String): Char {
    // UPC-A: 3*odd-position + even-position (1-indexed).
    var sum = 0
    for (i in 0 until 11) {
        val d = first11[i].digitToInt()
        sum += if (i % 2 == 0) d * 3 else d
    }
    val check = (10 - (sum % 10)) % 10
    return '0' + check
}

private fun upcaCheckValid(all12: String): Boolean =
    upcaCheckDigit(all12.substring(0, 11)) == all12[11]

/** Encodes [content] into a bitmap via ZXing's [MultiFormatWriter]. */
private fun generateBarcode(format: BcFormat, content: String): Bitmap {
    val matrix = MultiFormatWriter()
        .encode(content, format.zxing, format.targetW, format.targetH)
    val w = matrix.width
    val h = matrix.height
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    for (x in 0 until w) for (y in 0 until h) {
        bmp.setPixel(x, y, if (matrix[x, y]) AColor.BLACK else AColor.WHITE)
    }
    return bmp
}

private fun saveBarcodeToPictures(@Suppress("UNUSED_PARAMETER") ctx: Context, bmp: Bitmap): File? =
    runCatching {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "ZeddiHub"
        )
        dir.mkdirs()
        val file = File(dir, "barcode_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file
    }.getOrNull()

private fun saveBarcodeToCache(ctx: Context, bmp: Bitmap): File? = runCatching {
    val dir = File(ctx.cacheDir, "barcode_share")
    dir.mkdirs()
    val file = File(dir, "barcode.png")
    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    file
}.getOrNull()

// --- Scan tab -----------------------------------------------------------

private enum class ScanMode { CAMERA, GALLERY }

@Composable
private fun ScanBarcodeTab(onScanned: (format: String, payload: String) -> Unit) {
    val ctx = LocalContext.current
    var mode by remember { mutableStateOf(ScanMode.CAMERA) }
    var galleryError by remember { mutableStateOf<String?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val r = decodeBarcodeFromImage(ctx, uri)
            val payload = r.getOrNull()
            if (payload != null) {
                onScanned(payload.second, payload.first)
                galleryError = null
            } else {
                galleryError = r.exceptionOrNull()?.localizedMessage
                    ?: "Nepodařilo se načíst kód z obrázku"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Mode segmented buttons.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == ScanMode.CAMERA,
                onClick = { mode = ScanMode.CAMERA },
                label = { Text(stringResource(R.string.bc_camera)) }
            )
            FilterChip(
                selected = mode == ScanMode.GALLERY,
                onClick = { mode = ScanMode.GALLERY },
                label = { Text(stringResource(R.string.bc_gallery)) }
            )
        }

        Spacer(Modifier.height(12.dp))

        when (mode) {
            ScanMode.CAMERA -> LiveCameraScanner(onScanned = onScanned)
            ScanMode.GALLERY -> {
                Button(onClick = { pickLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Image, null); Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.bc_pick_image))
                }
                galleryError?.let {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * CameraX preview + ImageAnalysis with ML Kit barcode scanner.
 *
 * On first successful decode we fire [onScanned] once (guarded by a local flag)
 * and stop feeding frames. A DisposableEffect tears the camera down when this
 * composable leaves composition (tab switch, navigation, etc.).
 */
@Composable
private fun LiveCameraScanner(onScanned: (format: String, payload: String) -> Unit) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.bc_camera_perm_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                Text(stringResource(R.string.bc_grant_camera))
            }
        }
        return
    }

    // Permission granted — wire CameraX.
    var torchOn by remember { mutableStateOf(false) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    val alreadyScanned = remember { mutableStateOf(false) }

    // Executor used by both the analyzer and ProcessCameraProvider. We manually
    // tear it down on dispose to avoid leaking the thread.
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val scannerOptions = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    }
    val mlkitScanner = remember { BarcodeScanning.getClient(scannerOptions) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                val previewView = PreviewView(c).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val providerFuture = ProcessCameraProvider.getInstance(c)
                providerFuture.addListener({
                    val provider = providerFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { ia ->
                            ia.setAnalyzer(analysisExecutor) { imageProxy ->
                                processBarcodeFrame(
                                    imageProxy = imageProxy,
                                    scanner = mlkitScanner,
                                    alreadyScanned = alreadyScanned,
                                    onScanned = onScanned
                                )
                            }
                        }

                    val selector = CameraSelector.DEFAULT_BACK_CAMERA
                    runCatching {
                        provider.unbindAll()
                        cameraRef = provider.bindToLifecycle(
                            lifecycleOwner, selector, preview, analysis
                        )
                    }.onFailure { Log.e("AdvancedBarcode", "Bind failed", it) }
                }, ContextCompat.getMainExecutor(c))
                previewView
            }
        )

        // Torch toggle — overlaid top-right.
        IconButton(
            onClick = {
                val cam = cameraRef ?: return@IconButton
                if (cam.cameraInfo.hasFlashUnit()) {
                    torchOn = !torchOn
                    cam.cameraControl.enableTorch(torchOn)
                } else {
                    Toast.makeText(ctx, "Svítilna není dostupná", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    RoundedCornerShape(50)
                )
        ) {
            Icon(
                if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Torch"
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(ctx).get().unbindAll() }
            runCatching { mlkitScanner.close() }
            analysisExecutor.shutdown()
        }
    }
}

/**
 * Feeds a single [ImageProxy] frame into ML Kit. On the first successful decode
 * we flip [alreadyScanned] and emit the result on the main thread. Always closes
 * the proxy.
 */
private fun processBarcodeFrame(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    alreadyScanned: androidx.compose.runtime.MutableState<Boolean>,
    onScanned: (format: String, payload: String) -> Unit
) {
    if (alreadyScanned.value) {
        imageProxy.close()
        return
    }
    val media = imageProxy.image
    if (media == null) {
        imageProxy.close()
        return
    }
    val input = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { barcodes ->
            val first = barcodes.firstOrNull { it.rawValue != null }
            if (first != null && !alreadyScanned.value) {
                alreadyScanned.value = true
                val fmt = mlkitFormatLabel(first.format)
                onScanned(fmt, first.rawValue.orEmpty())
            }
        }
        .addOnFailureListener { Log.w("AdvancedBarcode", "ML Kit failed", it) }
        .addOnCompleteListener { imageProxy.close() }
}

private fun mlkitFormatLabel(format: Int): String = when (format) {
    Barcode.FORMAT_CODE_128 -> "Code 128"
    Barcode.FORMAT_CODE_39 -> "Code 39"
    Barcode.FORMAT_CODE_93 -> "Code 93"
    Barcode.FORMAT_CODABAR -> "Codabar"
    Barcode.FORMAT_EAN_13 -> "EAN-13"
    Barcode.FORMAT_EAN_8 -> "EAN-8"
    Barcode.FORMAT_ITF -> "ITF"
    Barcode.FORMAT_UPC_A -> "UPC-A"
    Barcode.FORMAT_UPC_E -> "UPC-E"
    Barcode.FORMAT_QR_CODE -> "QR"
    Barcode.FORMAT_PDF417 -> "PDF417"
    Barcode.FORMAT_AZTEC -> "Aztec"
    Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
    else -> "Unknown"
}

/** Decode a user-picked image via ZXing — returns (payload, formatLabel). */
private fun decodeBarcodeFromImage(ctx: Context, uri: Uri): Result<Pair<String, String>> =
    runCatching {
        val bmp = ctx.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
            ?: error("Obrázek se nepodařilo načíst")
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val source = RGBLuminanceSource(w, h, pixels)
        val bb = BinaryBitmap(HybridBinarizer(source))
        val result = MultiFormatReader().decode(bb)
        result.text to zxingFormatLabel(result.barcodeFormat)
    }

private fun zxingFormatLabel(format: BarcodeFormat): String = when (format) {
    BarcodeFormat.CODE_128 -> "Code 128"
    BarcodeFormat.CODE_39 -> "Code 39"
    BarcodeFormat.CODE_93 -> "Code 93"
    BarcodeFormat.CODABAR -> "Codabar"
    BarcodeFormat.EAN_13 -> "EAN-13"
    BarcodeFormat.EAN_8 -> "EAN-8"
    BarcodeFormat.ITF -> "ITF"
    BarcodeFormat.UPC_A -> "UPC-A"
    BarcodeFormat.UPC_E -> "UPC-E"
    BarcodeFormat.QR_CODE -> "QR"
    BarcodeFormat.PDF_417 -> "PDF417"
    BarcodeFormat.AZTEC -> "Aztec"
    BarcodeFormat.DATA_MATRIX -> "Data Matrix"
    else -> format.name
}

// --- Historie tab -------------------------------------------------------

@Composable
private fun HistoryTab(
    history: List<BarcodeHistoryItem>,
    onRowClick: (BarcodeHistoryItem) -> Unit,
    onClear: () -> Unit,
    onExported: () -> Unit
) {
    val ctx = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            val ok = exportHistoryToCsv(ctx, uri, history)
            Toast.makeText(
                ctx,
                if (ok) "CSV export hotový" else "CSV export selhal",
                Toast.LENGTH_SHORT
            ).show()
            if (ok) onExported()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.bc_history_count, history.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                enabled = history.isNotEmpty(),
                onClick = {
                    val suggested = "barcodes_${
                        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    }.csv"
                    exportLauncher.launch(suggested)
                }
            ) {
                Icon(Icons.Default.FileDownload, null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.bc_export_csv))
            }
            TextButton(
                enabled = history.isNotEmpty(),
                onClick = onClear
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.bc_clear_all))
            }
        }

        Spacer(Modifier.height(8.dp))

        if (history.isEmpty()) {
            Text(
                stringResource(R.string.bc_history_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(history, key = { it.timestamp }) { item ->
                    HistoryRow(item, onClick = { onRowClick(item) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: BarcodeHistoryItem, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = onClick, label = { Text(item.format) })
                Spacer(Modifier.weight(1f))
                Text(
                    formatTimestamp(item.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                item.payload.take(120) + if (item.payload.length > 120) "…" else "",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            )
        }
    }
}

private fun formatTimestamp(ms: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(ms))

/** RFC 4180 quote: wrap in double-quotes and double any internal quote. */
private fun csvQuote(s: String): String =
    "\"" + s.replace("\"", "\"\"") + "\""

private fun exportHistoryToCsv(
    ctx: Context,
    uri: Uri,
    history: List<BarcodeHistoryItem>
): Boolean = runCatching {
    val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    ctx.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { w ->
        w.append("timestamp_iso,format,payload\n")
        history.forEach { it ->
            w.append(csvQuote(iso.format(Date(it.timestamp))))
            w.append(',')
            w.append(csvQuote(it.format))
            w.append(',')
            w.append(csvQuote(it.payload))
            w.append('\n')
        }
    } ?: return false
    true
}.getOrElse {
    Log.e("AdvancedBarcode", "CSV export failed", it)
    false
}

// --- Quick actions sheet ------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionsSheet(
    item: BarcodeHistoryItem,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val payload = item.payload

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.bc_scanned_code), style = MaterialTheme.typography.titleMedium)
            Text(
                "${item.format} • ${formatTimestamp(item.timestamp)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    payload,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                )
            }

            // Contextual primary action.
            when {
                payload.startsWith("http://", ignoreCase = true) ||
                        payload.startsWith("https://", ignoreCase = true) -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            runCatching {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(payload)))
                            }
                        }
                    ) {
                        Icon(Icons.Default.OpenInBrowser, null); Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.bc_open_browser))
                    }
                }
                payload.startsWith("WIFI:", ignoreCase = true) -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.bc_wifi_hint),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                payload.startsWith("tel:", ignoreCase = true) -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            runCatching {
                                ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(payload)))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Call, null); Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.bc_call))
                    }
                }
                payload.startsWith("mailto:", ignoreCase = true) -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            runCatching {
                                ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(payload)))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Email, null); Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.bc_email))
                    }
                }
                else -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val search = "https://www.google.com/search?q=" + Uri.encode(payload)
                            runCatching {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(search)))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Search, null); Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.bc_search_google))
                    }
                }
            }

            // Always-available actions.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        clip.setText(AnnotatedString(payload))
                        Toast.makeText(ctx, "Zkopírováno", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.bc_copy))
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, payload)
                        ctx.startActivity(Intent.createChooser(intent, null))
                    }
                ) {
                    Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.bc_share))
                }
            }

            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { scope.launch { sheetState.hide(); onDismiss() } }) {
                Text(stringResource(R.string.bc_close))
            }
        }
    }
}

// --- Persistence (SharedPreferences + org.json) ------------------------

/**
 * Single scan entry. Persisted as JSON inside SharedPreferences — plain
 * org.json kept deliberately so we don't pull in Moshi code-gen/KSP just for
 * three fields.
 */
data class BarcodeHistoryItem(
    val timestamp: Long,
    val format: String,
    val payload: String
)

private object HistoryStore {
    private const val PREFS = "zeddihub_barcode_history"
    private const val KEY_ITEMS = "items_json"
    private const val MAX_ITEMS = 50

    fun load(ctx: Context): List<BarcodeHistoryItem> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val out = ArrayList<BarcodeHistoryItem>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    BarcodeHistoryItem(
                        timestamp = o.optLong("timestamp"),
                        format = o.optString("format"),
                        payload = o.optString("payload")
                    )
                )
            }
            out
        }.getOrElse { emptyList() }
    }

    fun save(ctx: Context, items: List<BarcodeHistoryItem>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("timestamp", it.timestamp)
                    .put("format", it.format)
                    .put("payload", it.payload)
            )
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, arr.toString())
            .apply()
    }

    /**
     * Appends [new] to the front of the list, trims to [MAX_ITEMS] (FIFO —
     * oldest entries drop off the tail), persists, and returns the new list.
     */
    fun append(
        ctx: Context,
        current: List<BarcodeHistoryItem>,
        new: BarcodeHistoryItem
    ): List<BarcodeHistoryItem> {
        val updated = (listOf(new) + current).take(MAX_ITEMS)
        save(ctx, updated)
        return updated
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ITEMS)
            .apply()
    }
}
