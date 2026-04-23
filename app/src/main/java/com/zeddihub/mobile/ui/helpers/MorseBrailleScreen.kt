package com.zeddihub.mobile.ui.helpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * Two-tab converter: text ↔ Morse, text ↔ Braille.
 *
 * Morse tab also offers beep playback (AudioTrack, sine 600 Hz, standard
 * ITU dot=80 ms / dash=240 ms / intra-gap=80 ms / letter-gap=240 ms /
 * word-gap=560 ms) so users can listen to what their input sounds like.
 *
 * Braille uses Unicode braille block U+2800–U+283F; each character maps
 * to a 6-dot pattern. Grade-1 transliteration only; we don't contract.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorseBrailleScreen(padding: PaddingValues) {
    val colors = MaterialTheme.colorScheme
    var tabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
    ) {
        PrimaryTabRow(selectedTabIndex = tabIndex, containerColor = colors.background) {
            Tab(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                text = { Text(stringResource(R.string.morse_tab_morse)) }
            )
            Tab(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                text = { Text(stringResource(R.string.morse_tab_braille)) }
            )
        }
        when (tabIndex) {
            0 -> MorsePane()
            else -> BraillePane()
        }
    }
}

@Composable
private fun MorsePane() {
    val ctx = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var text by remember { mutableStateOf("SOS") }
    val scope = rememberCoroutineScope()
    var playJob by remember { mutableStateOf<Job?>(null) }

    // Encoded output updates live as user types.
    val morse = remember(text) { textToMorse(text) }
    // Optional decode preview — if the input already looks like morse
    // (only dots/dashes/spaces/slashes), decode it.
    val decoded = remember(text) {
        if (text.isNotBlank() && text.all { it == '.' || it == '-' || it == ' ' || it == '/' })
            morseToText(text)
        else null
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.morse_input)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutputBox(
            title = stringResource(R.string.morse_output_code),
            body = morse,
            onCopy = { copy(ctx, morse) }
        )

        if (decoded != null) {
            Spacer(Modifier.height(8.dp))
            OutputBox(
                title = stringResource(R.string.morse_output_text),
                body = decoded,
                onCopy = { copy(ctx, decoded) }
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                playJob?.cancel()
                playJob = scope.launch {
                    withContext(Dispatchers.Default) { playMorse(morse) }
                }
            }) { Text(stringResource(R.string.morse_play)) }
            OutlinedButton(onClick = {
                text = ""
                playJob?.cancel()
            }) { Text(stringResource(R.string.morse_clear)) }
        }
    }
    DisposableEffect(Unit) { onDispose { playJob?.cancel() } }
}

@Composable
private fun BraillePane() {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("Ahoj") }
    val braille = remember(text) { textToBraille(text) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.morse_input)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutputBox(
            title = stringResource(R.string.morse_output_code),
            body = braille,
            onCopy = { copy(ctx, braille) },
            mono = true,
            big = true
        )
    }
}

@Composable
private fun OutputBox(
    title: String,
    body: String,
    onCopy: () -> Unit,
    mono: Boolean = false,
    big: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onCopy) {
                    Text(stringResource(R.string.morse_copy))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = body.ifBlank { "—" },
                color = colors.onSurface,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                style = if (big) MaterialTheme.typography.headlineSmall
                else MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun copy(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("zh", text))
}

// ── Morse table (ITU) ────────────────────────────────────────────────────

private val MORSE: Map<Char, String> = mapOf(
    'A' to ".-",     'B' to "-...",   'C' to "-.-.",   'D' to "-..",
    'E' to ".",      'F' to "..-.",   'G' to "--.",    'H' to "....",
    'I' to "..",     'J' to ".---",   'K' to "-.-",    'L' to ".-..",
    'M' to "--",     'N' to "-.",     'O' to "---",    'P' to ".--.",
    'Q' to "--.-",   'R' to ".-.",    'S' to "...",    'T' to "-",
    'U' to "..-",    'V' to "...-",   'W' to ".--",    'X' to "-..-",
    'Y' to "-.--",   'Z' to "--..",
    '0' to "-----",  '1' to ".----",  '2' to "..---",  '3' to "...--",
    '4' to "....-",  '5' to ".....",  '6' to "-....",  '7' to "--...",
    '8' to "---..",  '9' to "----.",
    '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.",
    '!' to "-.-.--", '/' to "-..-.",  '(' to "-.--.",  ')' to "-.--.-",
    '&' to ".-...",  ':' to "---...", ';' to "-.-.-.", '=' to "-...-",
    '+' to ".-.-.",  '-' to "-....-", '_' to "..--.-", '"' to ".-..-.",
    '$' to "...-..-",'@' to ".--.-."
)

private fun textToMorse(input: String): String {
    val sb = StringBuilder()
    val norm = normalizeAscii(input).uppercase()
    val words = norm.split(' ').filter { it.isNotBlank() }
    words.forEachIndexed { i, word ->
        word.forEachIndexed { j, c ->
            MORSE[c]?.let {
                sb.append(it)
                if (j != word.length - 1) sb.append(' ')
            }
        }
        if (i != words.size - 1) sb.append(" / ")
    }
    return sb.toString()
}

private fun morseToText(input: String): String {
    val reverse = MORSE.entries.associate { (k, v) -> v to k }
    return input.trim().split('/').joinToString(" ") { w ->
        w.trim().split(Regex("\\s+")).mapNotNull { reverse[it]?.toString() }.joinToString("")
    }
}

// Simple ASCII-fold for Czech diacritics so "Ahoj" / "Žluťoučký" still
// produce something meaningful instead of dropping the whole character.
private fun normalizeAscii(s: String): String {
    val n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
    return n.replace(Regex("\\p{InCombiningDiacriticalMarks}"), "")
}

// ── Morse audio playback ────────────────────────────────────────────────
private const val SAMPLE_RATE = 44100
private const val TONE_HZ = 600
private const val DOT_MS = 80
private const val DASH_MS = DOT_MS * 3
private const val INTRA_GAP_MS = DOT_MS
private const val LETTER_GAP_MS = DOT_MS * 3
private const val WORD_GAP_MS = DOT_MS * 7

private fun playMorse(code: String) {
    val bufSize = AudioTrack.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(2048)

    val track = AudioTrack(
        AudioManager.STREAM_MUSIC,
        SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufSize,
        AudioTrack.MODE_STREAM
    )

    try {
        track.play()
        for (ch in code) {
            val tone: ShortArray? = when (ch) {
                '.' -> makeTone(DOT_MS)
                '-' -> makeTone(DASH_MS)
                else -> null
            }
            if (tone != null) {
                track.write(tone, 0, tone.size)
                track.write(silence(INTRA_GAP_MS), 0, INTRA_GAP_MS * SAMPLE_RATE / 1000)
            } else if (ch == ' ') {
                track.write(silence(LETTER_GAP_MS), 0, LETTER_GAP_MS * SAMPLE_RATE / 1000)
            } else if (ch == '/') {
                track.write(silence(WORD_GAP_MS), 0, WORD_GAP_MS * SAMPLE_RATE / 1000)
            }
        }
    } finally {
        runCatching { track.stop() }
        runCatching { track.release() }
    }
}

private fun makeTone(durationMs: Int): ShortArray {
    val samples = SAMPLE_RATE * durationMs / 1000
    val out = ShortArray(samples)
    val step = 2.0 * PI * TONE_HZ / SAMPLE_RATE
    // 20 ms attack+release envelope to remove clicks.
    val fade = (SAMPLE_RATE * 20 / 1000).coerceAtMost(samples / 4)
    for (i in 0 until samples) {
        val env = when {
            i < fade -> i.toDouble() / fade
            i > samples - fade -> (samples - i).toDouble() / fade
            else -> 1.0
        }
        out[i] = (sin(step * i) * env * Short.MAX_VALUE * 0.6).toInt().toShort()
    }
    return out
}

private fun silence(durationMs: Int): ShortArray {
    return ShortArray(SAMPLE_RATE * durationMs / 1000)
}

// ── Braille (grade-1, Unicode braille block) ────────────────────────────

private val BRAILLE: Map<Char, Char> = buildMap {
    val letters = mapOf(
        'A' to "1",    'B' to "12",   'C' to "14",   'D' to "145",
        'E' to "15",   'F' to "124",  'G' to "1245", 'H' to "125",
        'I' to "24",   'J' to "245",  'K' to "13",   'L' to "123",
        'M' to "134",  'N' to "1345", 'O' to "135",  'P' to "1234",
        'Q' to "12345",'R' to "1235", 'S' to "234",  'T' to "2345",
        'U' to "136",  'V' to "1236", 'W' to "2456", 'X' to "1346",
        'Y' to "13456",'Z' to "1356"
    )
    val digits = mapOf(
        '1' to "1", '2' to "12", '3' to "14", '4' to "145", '5' to "15",
        '6' to "124", '7' to "1245", '8' to "125", '9' to "24", '0' to "245"
    )
    val punct = mapOf(
        '.' to "256", ',' to "2", '?' to "236", '!' to "235",
        ';' to "23", ':' to "25", '-' to "36", ' ' to ""
    )
    for ((c, dots) in letters) put(c, dotsToBraille(dots))
    for ((c, dots) in digits) put(c, dotsToBraille(dots))
    for ((c, dots) in punct) put(c, if (dots.isEmpty()) ' ' else dotsToBraille(dots))
}

/** Convert "1245" → braille Unicode char from base 0x2800. */
private fun dotsToBraille(dots: String): Char {
    var bits = 0
    for (d in dots) {
        val n = d.digitToInt()
        bits = bits or (1 shl (n - 1))
    }
    return (0x2800 + bits).toChar()
}

private fun textToBraille(input: String): String {
    val norm = normalizeAscii(input).uppercase()
    val sb = StringBuilder()
    for (c in norm) {
        BRAILLE[c]?.let { sb.append(it) } ?: sb.append(c)
    }
    return sb.toString()
}
