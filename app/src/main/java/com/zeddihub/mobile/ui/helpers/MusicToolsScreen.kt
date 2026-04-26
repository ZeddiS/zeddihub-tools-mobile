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
    var fileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // Persist write access — the SAF picker grants r/w for the
            // session, but we want to keep it after a screen transition
            // so the Save button still works.
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            fileUri = uri
            fileName = uri.lastPathSegment
            error = null
            saveStatus = null
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

            Button(onClick = { editing = true }) {
                Text(stringResource(R.string.music_edit))
            }
            saveStatus?.let {
                Text(it, color = MaterialTheme.colorScheme.primary,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (editing && meta != null && fileUri != null) {
        TagEditor(
            initial = meta!!,
            onCancel = { editing = false },
            onSave = { edited ->
                editing = false
                val ok = Id3v2Writer.writeTags(
                    ctx, fileUri!!,
                    mapOf(
                        "title" to (edited.title ?: ""),
                        "artist" to (edited.artist ?: ""),
                        "album" to (edited.album ?: ""),
                        "albumArtist" to (edited.albumArtist ?: ""),
                        "year" to (edited.year ?: ""),
                        "track" to (edited.track ?: ""),
                        "genre" to (edited.genre ?: ""),
                        "composer" to (edited.composer ?: ""),
                    )
                )
                saveStatus = if (ok) ctx.getString(R.string.music_save_ok)
                else ctx.getString(R.string.music_save_fail)
                if (ok) {
                    // Re-read so the displayed metadata reflects the
                    // newly-written tag instead of the stale view.
                    meta = runCatching { extractMeta(ctx, fileUri!!) }.getOrNull()
                }
            }
        )
    }
}

@Composable
private fun TagEditor(
    initial: AudioMeta,
    onCancel: () -> Unit,
    onSave: (AudioMeta) -> Unit,
) {
    var title by remember { mutableStateOf(initial.title ?: "") }
    var artist by remember { mutableStateOf(initial.artist ?: "") }
    var album by remember { mutableStateOf(initial.album ?: "") }
    var albumArtist by remember { mutableStateOf(initial.albumArtist ?: "") }
    var year by remember { mutableStateOf(initial.year ?: "") }
    var track by remember { mutableStateOf(initial.track ?: "") }
    var genre by remember { mutableStateOf(initial.genre ?: "") }
    var composer by remember { mutableStateOf(initial.composer ?: "") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.music_edit_title)) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                onSave(initial.copy(
                    title = title, artist = artist, album = album,
                    albumArtist = albumArtist, year = year, track = track,
                    genre = genre, composer = composer,
                ))
            }) { Text(stringResource(R.string.music_save)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onCancel) {
                Text(stringResource(R.string.music_cancel))
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tagField(R.string.music_field_title, title) { title = it }
                tagField(R.string.music_field_artist, artist) { artist = it }
                tagField(R.string.music_field_album, album) { album = it }
                tagField(R.string.music_field_albumartist, albumArtist) { albumArtist = it }
                tagField(R.string.music_field_year, year) { year = it }
                tagField(R.string.music_field_track, track) { track = it }
                tagField(R.string.music_field_genre, genre) { genre = it }
                tagField(R.string.music_field_composer, composer) { composer = it }
            }
        }
    )
}

@Composable
private fun tagField(labelRes: Int, value: String, onChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
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
