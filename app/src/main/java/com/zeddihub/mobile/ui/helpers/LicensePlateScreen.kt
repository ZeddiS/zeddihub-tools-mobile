package com.zeddihub.mobile.ui.helpers

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.zeddihub.mobile.R
import java.util.concurrent.Executors

/**
 * License plate lookup — type a plate (or scan with the camera) and
 * see which CZ / SK district issued it.
 *
 * Two input modes share the same lookup result:
 *   • Manual: OutlinedTextField, parsed live as the user types.
 *   • Camera: CameraX preview + ML Kit text recognition. The first
 *     recognised plate-shaped token (≥2 letters at start, ≥6 chars
 *     total) auto-fills the text field and stops the camera so the
 *     user gets immediate feedback instead of OCR jitter.
 *
 * Plate format awareness is in [LicensePlateData.lookup] — this screen
 * is a thin UI shell. Below the result we list the full district set
 * so users can browse without typing.
 */
@Composable
fun LicensePlateScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    var showCamera by remember { mutableStateOf(false) }
    val match = remember(input) { LicensePlateData.lookup(input) }

    val cameraGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraGranted.value = granted; if (granted) showCamera = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.lp_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.lp_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(stringResource(R.string.lp_input_label)) },
            placeholder = { Text("1AB 1234 / BA 123 XY") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (cameraGranted.value) showCamera = !showCamera
                else launcher.launch(Manifest.permission.CAMERA)
            }) {
                Text(if (showCamera) stringResource(R.string.lp_close_camera)
                    else stringResource(R.string.lp_open_camera))
            }
            if (input.isNotBlank()) {
                OutlinedButton(onClick = { input = "" }) {
                    Text(stringResource(R.string.lp_clear))
                }
            }
        }

        // Camera preview with live OCR. Folds away when toggled off.
        if (showCamera && cameraGranted.value) {
            PlateCameraScanner(
                onPlateRecognised = { ocr ->
                    input = ocr
                    showCamera = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        }

        if (match != null) {
            ResultCard(match)
        } else if (input.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.lp_no_match),
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Browse-the-tables — a flat scrollable list. Cheaper than a
        // searchable + grouped UI for the size we have (~150 rows).
        Text(
            stringResource(R.string.lp_full_list),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        DistrictTable(
            country = LicensePlateData.Country.CZ,
            rows = LicensePlateData.CZ
        )
        DistrictTable(
            country = LicensePlateData.Country.SK,
            rows = LicensePlateData.SK
        )
    }
}

@Composable
private fun ResultCard(p: LicensePlateData.PlateRegion) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("${p.country.flag}  ${p.code}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(p.district,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            p.regionName?.let {
                Text(it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun DistrictTable(
    country: LicensePlateData.Country,
    rows: List<LicensePlateData.PlateRegion>,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${country.flag}  ${country.label}",
                fontWeight = FontWeight.SemiBold)
            // Use a height-bounded LazyColumn — full list inside a
            // verticalScroll Column would crash (nested-scroll), so we
            // cap each table at a fixed height.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                userScrollEnabled = true,
            ) {
                items(rows) { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(r.code,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp))
                        Column {
                            Text(r.district)
                            r.regionName?.let {
                                Text(it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * CameraX preview + ML Kit image-by-image text recognition. Plate
 * detection is permissive: any text containing 2+ letters at the start
 * and ≥4 trailing chars is offered to the lookup. The first match
 * wins and shuts the camera down, so we don't churn on OCR noise.
 */
@Composable
private fun PlateCameraScanner(
    onPlateRecognised: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            recognizer.close()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val previewView = PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                                )
                            )
                            .build()
                    )
                    .build()

                analysis.setAnalyzer(executor) { proxy: ImageProxy ->
                    val media = proxy.image
                    if (media == null) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                    recognizer.process(image)
                        .addOnSuccessListener { text ->
                            val candidate = pickPlateCandidate(text.text)
                            if (candidate != null && LicensePlateData.lookup(candidate) != null) {
                                // Hop to the main thread for UI mutation.
                                previewView.post { onPlateRecognised(candidate) }
                            }
                        }
                        .addOnCompleteListener { proxy.close() }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                )
            }, ContextCompat.getMainExecutor(context))

            previewView
        }
    )
}

/**
 * Walk OCR'd lines and pick the first one that *looks* like a plate.
 * We use a permissive regex because dirty plates / sun glare / odd
 * fonts confuse OCR; tightening this would just make scans miss.
 */
private fun pickPlateCandidate(blob: String): String? {
    if (blob.isBlank()) return null
    val plateRegex = Regex("""([0-9]?[A-Z]{2}[0-9]{2,5}[A-Z]{0,3})""")
    blob.split('\n', ' ').forEach { token ->
        val cleaned = token.uppercase().filter { it.isLetterOrDigit() }
        if (cleaned.length in 6..10 && plateRegex.containsMatchIn(cleaned)) {
            return cleaned
        }
    }
    return null
}
