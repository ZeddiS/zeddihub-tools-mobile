package com.zeddihub.mobile.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lightweight Markdown renderer for changelog-style content.
 *
 * Supported block elements:
 *  - `#`..`######` headings
 *  - `-` / `*` / `+` bullet lists
 *  - Blank lines as paragraph separators
 *
 * Supported inline markers:
 *  - `**bold**` / `__bold__`
 *  - `*italic*` / `_italic_`
 *  - `` `code` ``
 *  - `~~strike~~`
 *  - `[text](url)` renders just the text (no navigation)
 *
 * No external dependency — good enough for release notes, not a general-purpose CommonMark parser.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(text) { parseBlocks(text) }
    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MdBlock.Heading -> {
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    Text(
                        text = renderInline(block.text),
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                }

                is MdBlock.Bullet -> {
                    Row(modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)) {
                        Text(
                            "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = renderInline(block.text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is MdBlock.Paragraph -> {
                    if (index > 0) Spacer(Modifier.height(6.dp))
                    Text(
                        text = renderInline(block.text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Block parser
// ---------------------------------------------------------------------------

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Bullet(val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
}

private fun parseBlocks(raw: String): List<MdBlock> {
    if (raw.isBlank()) return emptyList()
    val lines = raw.replace("\r\n", "\n").replace('\r', '\n').split('\n')
    val out = mutableListOf<MdBlock>()
    val paragraph = StringBuilder()

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            out.add(MdBlock.Paragraph(paragraph.toString().trim()))
            paragraph.clear()
        }
    }

    for (line in lines) {
        val trimmed = line.trimEnd()
        when {
            trimmed.isBlank() -> {
                flushParagraph()
            }
            HEADING_REGEX.containsMatchIn(trimmed) -> {
                flushParagraph()
                val match = HEADING_REGEX.find(trimmed)!!
                val level = match.groupValues[1].length.coerceIn(1, 6)
                out.add(MdBlock.Heading(level, match.groupValues[2].trim()))
            }
            BULLET_REGEX.containsMatchIn(trimmed) -> {
                flushParagraph()
                val match = BULLET_REGEX.find(trimmed)!!
                out.add(MdBlock.Bullet(match.groupValues[1].trim()))
            }
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(trimmed.trim())
            }
        }
    }
    flushParagraph()
    return out
}

private val HEADING_REGEX = Regex("^(#{1,6})\\s+(.+)$")
private val BULLET_REGEX = Regex("^\\s*[-*+]\\s+(.+)$")

// ---------------------------------------------------------------------------
// Inline parser
// ---------------------------------------------------------------------------

private fun renderInline(raw: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    val s = raw
    while (i < s.length) {
        val c = s[i]
        // `code`
        if (c == '`') {
            val end = s.indexOf('`', i + 1)
            if (end > i) {
                pushStyle(
                    SpanStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )
                append(s.substring(i + 1, end))
                pop()
                i = end + 1
                continue
            }
        }
        // **bold** or __bold__
        if ((c == '*' || c == '_') && i + 1 < s.length && s[i + 1] == c) {
            val marker = "$c$c"
            val end = s.indexOf(marker, i + 2)
            if (end > i + 1) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(renderInline(s.substring(i + 2, end)))
                pop()
                i = end + 2
                continue
            }
        }
        // *italic* / _italic_
        if (c == '*' || c == '_') {
            val end = s.indexOf(c, i + 1)
            if (end > i && !(end + 1 < s.length && s[end + 1] == c)) {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(renderInline(s.substring(i + 1, end)))
                pop()
                i = end + 1
                continue
            }
        }
        // ~~strike~~
        if (c == '~' && i + 1 < s.length && s[i + 1] == '~') {
            val end = s.indexOf("~~", i + 2)
            if (end > i + 1) {
                pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                append(renderInline(s.substring(i + 2, end)))
                pop()
                i = end + 2
                continue
            }
        }
        // [label](url) -> just the label
        if (c == '[') {
            val close = s.indexOf(']', i + 1)
            if (close > i && close + 1 < s.length && s[close + 1] == '(') {
                val urlEnd = s.indexOf(')', close + 2)
                if (urlEnd > close) {
                    append(s.substring(i + 1, close))
                    i = urlEnd + 1
                    continue
                }
            }
        }
        append(c)
        i++
    }
}

