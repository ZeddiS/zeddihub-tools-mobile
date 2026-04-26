package com.zeddihub.mobile.ui.helpers

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * Minimal ID3v2.3 tag writer — writes only the text frames that the
 * Music Tools editor exposes. Out of scope: APIC (cover art) editing,
 * extended header, footer, unsynchronisation, compression. We can read
 * everything via MediaMetadataRetriever; we only ever WRITE the simple
 * stuff people actually edit (title / artist / album / year / track /
 * genre / album artist / composer).
 *
 * Why hand-roll instead of pulling jaudiotagger / mp3agic: those libs
 * weigh 1–2 MB after R8 stripping — almost as much as the rest of the
 * "Hardware & media" section combined. ID3v2.3 text-frame writing is
 * ~150 lines once you know the spec. We pay that cost ourselves.
 *
 * Format reminder (ID3v2.3 spec):
 *   • File starts with "ID3" + version (3,0) + flags(1) + size(4 syncsafe)
 *   • Then frames: ID(4) + size(4 normal big-endian) + flags(2) + content
 *   • Text frames begin with one encoding byte: 01 = UTF-16 with BOM
 *   • Tag size doesn't include the 10-byte header itself, only frames + padding
 */
object Id3v2Writer {

    /**
     * Rewrite the MP3 file at [uri] with the given text tag values.
     * Existing audio data is preserved verbatim — only the tag region
     * at the head of the file is replaced.
     *
     * Returns true on success, false on any I/O or format error. Errors
     * are intentionally swallowed (and logged via runCatching) because
     * the UI just needs a success/failure flag — no recovery path makes
     * sense for "the file format isn't ID3v2.3 mp3".
     */
    fun writeTags(ctx: Context, uri: Uri, tags: Map<String, String>): Boolean {
        return runCatching {
            val resolver = ctx.contentResolver
            // Step 1 — read the audio body (everything after the existing
            // tag). We copy through a ByteArrayOutputStream because the
            // SAF stream is read-once: the moment we open it for writing
            // we lose the ability to re-read.
            val audioBody = resolver.openInputStream(uri)!!.use { skipExistingTagAndReadRest(it) }

            // Step 2 — build the new tag bytes.
            val newTag = buildTagBytes(tags)

            // Step 3 — write tag + audio body back, truncating the file.
            // Mode "wt" = write + truncate. Without 't' we'd leave trailing
            // garbage from the previous (potentially larger) tag.
            resolver.openOutputStream(uri, "wt")!!.use { out ->
                out.write(newTag)
                out.write(audioBody)
            }
            true
        }.getOrDefault(false)
    }

    // ── Reading existing tag to locate audio offset ─────────────────

    /**
     * Parse the ID3v2 header (if present) to skip past the existing tag,
     * then return everything after it. If no tag is present we return
     * the whole stream — the writer just prepends a fresh tag to it.
     */
    private fun skipExistingTagAndReadRest(input: InputStream): ByteArray {
        val head = ByteArray(10)
        val read = input.read(head)
        val out = ByteArrayOutputStream()
        if (read < 10 || head[0] != 'I'.code.toByte() ||
            head[1] != 'D'.code.toByte() || head[2] != '3'.code.toByte()) {
            // No tag — emit head bytes we already consumed, then the rest.
            if (read > 0) out.write(head, 0, read)
            input.copyTo(out)
            return out.toByteArray()
        }
        val tagSize = decodeSyncsafe(head, 6)
        // Discard the rest of the existing tag body.
        var skipped = 0L
        while (skipped < tagSize) {
            val n = input.skip(tagSize - skipped)
            if (n <= 0) break
            skipped += n
        }
        input.copyTo(out)
        return out.toByteArray()
    }

    private fun decodeSyncsafe(buf: ByteArray, off: Int): Int {
        // Syncsafe: each byte uses only the lower 7 bits to avoid
        // accidentally producing the MPEG sync pattern (FF Fx) inside
        // the size field. Top bit must be 0.
        return (buf[off].toInt() and 0x7F) shl 21 or
            ((buf[off + 1].toInt() and 0x7F) shl 14) or
            ((buf[off + 2].toInt() and 0x7F) shl 7) or
            (buf[off + 3].toInt() and 0x7F)
    }

    // ── Building the new tag ────────────────────────────────────────

    private fun buildTagBytes(tags: Map<String, String>): ByteArray {
        val frames = ByteArrayOutputStream()
        for ((id, value) in tags) {
            if (value.isBlank()) continue
            writeTextFrame(frames, id, value)
        }
        // 1 KB padding so a future small tag edit doesn't have to rewrite
        // the whole audio body. Common practice in ID3v2 implementations.
        val paddingSize = 1024
        val tagBodySize = frames.size() + paddingSize

        val out = ByteArrayOutputStream(tagBodySize + 10)
        out.write("ID3".toByteArray(Charsets.US_ASCII))
        out.write(3); out.write(0)              // version 2.3.0
        out.write(0)                             // flags: no extended header
        // Tag size is syncsafe — see decodeSyncsafe above.
        encodeSyncsafe(tagBodySize, out)
        out.write(frames.toByteArray())
        out.write(ByteArray(paddingSize))        // zero padding
        return out.toByteArray()
    }

    private fun writeTextFrame(out: OutputStream, id: String, value: String) {
        // Encoding 01 = UTF-16 with BOM. We use UTF-16 (not 03 = UTF-8)
        // because UTF-8 is ID3v2.4-only and we're writing v2.3 for max
        // compatibility with old players (Winamp, car stereos, etc.).
        val payload = ByteArrayOutputStream()
        payload.write(0x01)                       // encoding marker
        payload.write(0xFF); payload.write(0xFE)  // UTF-16LE BOM
        payload.write(value.toByteArray(Charset.forName("UTF-16LE")))
        // ID3v2.3 spec mandates a null terminator for text fields. Two
        // bytes because we're in UTF-16.
        payload.write(0); payload.write(0)
        val data = payload.toByteArray()

        out.write(id.toByteArray(Charsets.US_ASCII))
        // Frame size in plain big-endian (NOT syncsafe — that's only
        // for the outer tag header).
        out.write((data.size ushr 24) and 0xFF)
        out.write((data.size ushr 16) and 0xFF)
        out.write((data.size ushr 8) and 0xFF)
        out.write(data.size and 0xFF)
        out.write(0); out.write(0)                // frame flags
        out.write(data)
    }

    private fun encodeSyncsafe(value: Int, out: OutputStream) {
        out.write((value ushr 21) and 0x7F)
        out.write((value ushr 14) and 0x7F)
        out.write((value ushr 7) and 0x7F)
        out.write(value and 0x7F)
    }

    /** Map of friendly name -> ID3v2.3 frame ID. Editor maps user input
     *  through this when calling [writeTags]. */
    val FRAMES = linkedMapOf(
        "title" to "TIT2",
        "artist" to "TPE1",
        "album" to "TALB",
        "albumArtist" to "TPE2",
        "year" to "TYER",
        "track" to "TRCK",
        "genre" to "TCON",
        "composer" to "TCOM",
    )
}
