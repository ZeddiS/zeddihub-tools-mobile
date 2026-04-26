package com.zeddihub.mobile.ui.helpers

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * MP3 / audio metadata reader (read-only in v0.9.0).
 *
 * Pick an audio file via SAF, then read every tag MediaMetadataRetriever
 * exposes (title / artist / album / year / track / duration / bitrate /
 * genre / composer / mime type / sample rate / channel count + embedded
 * cover art).
 *
 * Tag *editing* would need a third-party library (jaudiotagger / mp3agic)
 * — those are 1-2 MB jars with their own ID3 round-trip semantics, and
 * v0.9.0 already adds enough APK weight from BT/USB tooling. The write
 * path lands in v1.0.0 alongside Phonecall Recorder where we're already
 * adding storage-write permission infrastructure.
 */
@Composable
fun MusicToolsScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    var meta by remember { mutableStateOf<AudioMeta?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            fileName = uri.lastPathSegment
            error = null
            meta = runCatching { extractMeta(ctx, uri) }
                .onFailure { error = it.message ?: "Read error" }
                .getOrNull()
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
            stringResource(R.string.music_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(stringResource(R.string.music_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Button(onClick = {
            picker.launch(arrayOf("audio/*"))
        }) { Text(stringResource(R.string.music_pick)) }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        meta?.let { m ->
            m.cover?.let { bmp ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
            }

            val rows = listOf(
                R.string.music_field_title to m.title,
                R.string.music_field_artist to m.artist,
                R.string.music_field_album to m.album,
                R.string.music_field_albumartist to m.albumArtist,
                R.string.music_field_genre to m.genre,
                R.string.music_field_year to m.year,
                R.string.music_field_track to m.track,
                R.string.music_field_composer to m.composer,
                R.string.music_field_duration to m.duration,
                R.string.music_field_bitrate to m.bitrate,
                R.string.music_field_samplerate to m.sampleRate,
                R.string.music_field_channels to m.channelCount,
                R.string.music_field_mime to m.mime,
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    fileName?.let {
                        Text(it, fontWeight = FontWeight.SemiBold)
                    }
                    for ((labelRes, value) in rows) {
                        if (value.isNullOrBlank()) continue
                        Text(
                            "${stringResource(labelRes)}: $value",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) { /* keep recompose contract */ }
}

private data class AudioMeta(
    val title: String?, val artist: String?, val album: String?,
    val albumArtist: String?, val genre: String?, val year: String?,
    val track: String?, val composer: String?, val duration: String?,
    val bitrate: String?, val sampleRate: String?, val channelCount: String?,
    val mime: String?, val cover: Bitmap?,
)

private fun extractMeta(ctx: android.content.Context, uri: Uri): AudioMeta {
    val r = MediaMetadataRetriever()
    try {
        r.setDataSource(ctx, uri)
        // Format some fields humanely so the user doesn't see raw ms /
        // raw Hz integers.
        val durMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        val durHuman = durMs?.let {
            val s = it / 1000
            "%d:%02d".format(s / 60, s % 60)
        }
        val br = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
        val brHuman = br?.let { "${it / 1000} kbps" }
        val sr = if (android.os.Build.VERSION.SDK_INT >= 31)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE) else null
        val cc = if (android.os.Build.VERSION.SDK_INT >= 31)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS) else null
        val cover = r.embeddedPicture?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
        return AudioMeta(
            title = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
            artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
            album = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            albumArtist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
            genre = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE),
            year = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR),
            track = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER),
            composer = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER),
            duration = durHuman,
            bitrate = brHuman,
            sampleRate = sr?.let { "$it Hz" },
            channelCount = cc,
            mime = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
            cover = cover,
        )
    } finally {
        r.release()
    }
}
