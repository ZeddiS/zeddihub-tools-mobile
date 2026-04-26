package com.zeddihub.mobile.ui.helpers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zeddihub.mobile.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phonecall Recorder — best-effort audio capture during a call.
 *
 * **Hard truth**: since Android 10 the OS prevents any non-privileged
 * app from sampling the call uplink/downlink. The official AudioSource
 * constants (VOICE_CALL / VOICE_DOWNLINK / VOICE_UPLINK) need either
 * MODIFY_AUDIO_SETTINGS plus carrier-privileged signing, or pre-Android-10
 * builds. On modern devices the only recordable source during a call is
 * MediaRecorder.AudioSource.VOICE_RECOGNITION over the speakerphone — so
 * we rely on the user putting the call on speakerphone and we record
 * the room audio. Imperfect, but it's what's actually available without
 * rooting.
 *
 * **Legal**: in CZ / EU recording a call without informing the other
 * party is illegal under GDPR + telecommunications privacy. We surface
 * a one-time disclaimer as the first thing on screen and require an
 * explicit "I confirm both parties consent" tap before the first
 * recording. The button states reset on re-launch — we don't persist
 * a "user said yes once" flag because consent has to be per-call, not
 * once forever.
 *
 * Files land in `Download/ZeddiHub-Calls/` so they're easy for the user
 * to find and share. We don't write anywhere private — these recordings
 * belong to the user and the user should be able to remove them with a
 * file manager.
 */
@Composable
fun CallRecorderScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    val granted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted.value = it
    }
    var consented by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var lastFile by remember { mutableStateOf<String?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.runCatching { stop(); release() }
            recorder = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.cr_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Long red disclaimer first — important enough that the user
        // sees it before they think about pressing Record.
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    stringResource(R.string.cr_legal_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    stringResource(R.string.cr_legal_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        Text(
            stringResource(R.string.cr_technical_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!granted.value) {
            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text(stringResource(R.string.cr_grant_mic))
            }
            return@Column
        }

        if (!consented) {
            Button(onClick = { consented = true }) {
                Text(stringResource(R.string.cr_consent))
            }
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !recording,
                onClick = {
                    recorder = startRecording(ctx).also { mr ->
                        if (mr != null) {
                            recording = true
                        }
                    }
                }
            ) { Text(stringResource(R.string.cr_start)) }
            OutlinedButton(
                enabled = recording,
                onClick = {
                    val mr = recorder ?: return@OutlinedButton
                    val path = mr.runCatching { stop(); release() }
                    lastFile = currentFile
                    recorder = null
                    recording = false
                }
            ) { Text(stringResource(R.string.cr_stop)) }
        }

        lastFile?.let {
            Text(stringResource(R.string.cr_saved) + "\n$it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary)
        }

        if (Build.VERSION.SDK_INT >= 29) {
            // Direct user to enable speakerphone in their dialer; we
            // can't toggle it programmatically without MODIFY_PHONE_STATE.
            OutlinedButton(onClick = {
                runCatching {
                    ctx.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }) { Text(stringResource(R.string.cr_open_sound_settings)) }
        }
    }
}

private var currentFile: String? = null

private fun startRecording(ctx: Context): MediaRecorder? {
    val dir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "ZeddiHub-Calls"
    )
    if (!dir.exists()) dir.mkdirs()
    val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
    val out = File(dir, "call-$ts.m4a")
    currentFile = out.absolutePath

    val mr = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
    return runCatching {
        // VOICE_RECOGNITION over the speakerphone is the only call-side
        // source modern Android lets a non-system app open. See KDoc.
        mr.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mr.setAudioEncodingBitRate(96_000)
        mr.setAudioSamplingRate(44_100)
        mr.setOutputFile(out.absolutePath)
        mr.prepare()
        mr.start()
        mr
    }.onFailure {
        runCatching { mr.release() }
    }.getOrNull()
}
