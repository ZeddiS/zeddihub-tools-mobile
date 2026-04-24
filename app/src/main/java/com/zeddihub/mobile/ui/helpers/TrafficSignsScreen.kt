package com.zeddihub.mobile.ui.helpers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.data.local.LanguageCode

/**
 * Czech traffic signs reference catalog (vyhláška č. 294/2015 Sb.).
 *
 * Purely read-only educational tool — no network, no permissions. All
 * signs are drawn geometrically in a Compose [Canvas] using the legal
 * standard colors so the catalog is self-contained (no bitmap assets).
 * Tapping a tile opens a bottom sheet with the full czech/english name,
 * category, and a short description.
 */

// ────────────────────────────────────────────────────────────────────────
// Standard colors (TP 65 / vyhláška — legal signaling colors).
// ────────────────────────────────────────────────────────────────────────
private val SignRed = Color(0xFFC8102E)
private val SignBlue = Color(0xFF003DA5)
private val SignGreen = Color(0xFF008E3A)
private val SignWhite = Color.White
private val SignBlack = Color.Black

// ────────────────────────────────────────────────────────────────────────
// Data
// ────────────────────────────────────────────────────────────────────────

private enum class SignCategory(
    val code: String,
    val nameCs: String,
    val nameEn: String
) {
    WARNING("A", "Výstražné", "Warning"),
    PROHIBITION("B", "Zákazové", "Prohibition"),
    COMMAND("C", "Příkazové", "Mandatory"),
    INFO_OP("IP", "Informativní provozní", "Info (operational)"),
    INFO_DIR("IS", "Informativní směrové", "Info (directional)"),
    ZONE("IZ", "Zóny", "Zones"),
    ADDITIONAL("E", "Dodatkové", "Supplementary")
}

private data class CzechTrafficSign(
    val code: String,
    val nameCs: String,
    val nameEn: String,
    val descriptionCs: String,
    val descriptionEn: String,
    val category: SignCategory
)

// ────────────────────────────────────────────────────────────────────────
// Public API
// ────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficSignsScreen(
    padding: PaddingValues,
    language: LanguageCode = LanguageCode.CS
) {
    val colors = MaterialTheme.colorScheme
    val cs = language == LanguageCode.CS

    var selected by remember { mutableStateOf<CzechTrafficSign?>(null) }
    var query by remember { mutableStateOf("") }
    // 0 = All, then one tab per category in declared order
    var tabIndex by remember { mutableStateOf(0) }

    val categories = SignCategory.values().toList()
    val tabTitles = remember(language) {
        buildList {
            add(if (cs) "Všechny" else "All")
            categories.forEach { add(if (cs) it.nameCs else it.nameEn) }
        }
    }

    val filtered = remember(query, tabIndex, language) {
        val q = query.trim().lowercase()
        TrafficSigns.asSequence()
            .filter { tabIndex == 0 || it.category == categories[tabIndex - 1] }
            .filter { s ->
                if (q.isEmpty()) true
                else s.code.lowercase().contains(q) ||
                    s.nameCs.lowercase().contains(q) ||
                    s.nameEn.lowercase().contains(q) ||
                    s.descriptionCs.lowercase().contains(q)
            }
            .toList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (cs)
                "Katalog českých dopravních značek (vyhl. 294/2015 Sb.). Klepnutím zobrazíš detail."
            else
                "Czech traffic signs catalog (Decree 294/2015 Coll.). Tap a tile for details.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = {
                Text(
                    if (cs) "Hledat (kód, název, popis)" else "Search (code, name, description)"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))

        ScrollableTabRow(
            selectedTabIndex = tabIndex,
            edgePadding = 12.dp
        ) {
            tabTitles.forEachIndexed { i, title ->
                Tab(
                    selected = tabIndex == i,
                    onClick = { tabIndex = i },
                    text = { Text(title, maxLines = 1) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (cs) "Žádné značky neodpovídají hledání."
                    else "No signs match your search.",
                    color = colors.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered) { sign ->
                    SignTile(sign = sign, language = language) { selected = sign }
                }
            }
        }
    }

    selected?.let { sign ->
        SignDetailSheet(
            sign = sign,
            language = language,
            onDismiss = { selected = null }
        )
    }
}

// ────────────────────────────────────────────────────────────────────────
// Tile
// ────────────────────────────────────────────────────────────────────────

@Composable
private fun SignTile(
    sign: CzechTrafficSign,
    language: LanguageCode,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val title = if (language == LanguageCode.CS) sign.nameCs else sign.nameEn
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                SignShape(sign = sign, sizeDp = 72)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = sign.code,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// Sign shape — draws a category-appropriate geometric shape with the
// sign's code text centered inside. No bitmaps, pure Compose Canvas.
// ────────────────────────────────────────────────────────────────────────

@Composable
private fun SignShape(sign: CzechTrafficSign, sizeDp: Int) {
    Canvas(modifier = Modifier.size(sizeDp.dp)) {
        when (sign.category) {
            SignCategory.WARNING -> drawWarningTriangle(sign.code)
            SignCategory.PROHIBITION -> drawProhibitionCircle(sign.code)
            SignCategory.COMMAND -> drawCommandCircle(sign.code)
            SignCategory.INFO_OP -> drawInfoOpSquare(sign.code)
            SignCategory.INFO_DIR -> drawInfoDirRect(sign.code)
            SignCategory.ZONE -> drawZoneSquare(sign.code)
            SignCategory.ADDITIONAL -> drawAdditionalRect(sign.code)
        }
    }
}

/** Shared helper — draws centered black text (uses native canvas). */
private fun DrawScope.drawCenteredText(
    text: String,
    cx: Float,
    cy: Float,
    fontPx: Float,
    color: Color = SignBlack,
    bold: Boolean = true
) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = fontPx
            this.color = color.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = if (bold)
                android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            else
                android.graphics.Typeface.DEFAULT
        }
        val fm = paint.fontMetrics
        val baseline = cy - (fm.ascent + fm.descent) / 2f
        drawText(text, cx, baseline, paint)
    }
}

/** Warning (A): red-bordered white triangle, point up. */
private fun DrawScope.drawWarningTriangle(code: String) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w / 2f, h * 0.06f)
        lineTo(w * 0.96f, h * 0.92f)
        lineTo(w * 0.04f, h * 0.92f)
        close()
    }
    drawPath(path, SignWhite)
    drawPath(path, SignRed, style = Stroke(width = w * 0.09f))
    // Center of inscribed region — triangle centroid is roughly at h*0.63.
    drawCenteredText(code, w / 2f, h * 0.66f, fontPx = w * 0.20f)
}

/** Prohibition (B): red ring, white interior. */
private fun DrawScope.drawProhibitionCircle(code: String) {
    val w = size.width
    val r = w * 0.46f
    val c = Offset(w / 2f, w / 2f)
    drawCircle(SignWhite, r, c)
    drawCircle(SignRed, r, c, style = Stroke(width = w * 0.10f))
    drawCenteredText(code, c.x, c.y, fontPx = w * 0.22f)
}

/** Command (C): solid blue circle, white text. */
private fun DrawScope.drawCommandCircle(code: String) {
    val w = size.width
    val r = w * 0.46f
    val c = Offset(w / 2f, w / 2f)
    drawCircle(SignBlue, r, c)
    drawCenteredText(code, c.x, c.y, fontPx = w * 0.22f, color = SignWhite)
}

/** Info operational (IP): rounded blue square, white text. */
private fun DrawScope.drawInfoOpSquare(code: String) {
    val w = size.width
    val h = size.height
    val pad = w * 0.05f
    val rect = androidx.compose.ui.geometry.Rect(
        left = pad, top = pad, right = w - pad, bottom = h - pad
    )
    val cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f)
    val path = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(rect, cornerRadius))
    }
    drawPath(path, SignBlue)
    drawCenteredText(code, w / 2f, h / 2f, fontPx = w * 0.22f, color = SignWhite)
}

/** Info directional (IS): green rectangle, white text. */
private fun DrawScope.drawInfoDirRect(code: String) {
    val w = size.width
    val h = size.height
    val top = h * 0.18f
    val bottom = h * 0.82f
    drawRect(
        color = SignGreen,
        topLeft = Offset(w * 0.02f, top),
        size = androidx.compose.ui.geometry.Size(w * 0.96f, bottom - top)
    )
    drawCenteredText(code, w / 2f, (top + bottom) / 2f, fontPx = w * 0.20f, color = SignWhite)
}

/** Zone (IZ): blue square with code text (zone marker). */
private fun DrawScope.drawZoneSquare(code: String) {
    val w = size.width
    val h = size.height
    val pad = w * 0.05f
    drawRect(
        color = SignBlue,
        topLeft = Offset(pad, pad),
        size = androidx.compose.ui.geometry.Size(w - 2 * pad, h - 2 * pad)
    )
    drawRect(
        color = SignWhite,
        topLeft = Offset(pad, pad),
        size = androidx.compose.ui.geometry.Size(w - 2 * pad, h - 2 * pad),
        style = Stroke(width = w * 0.03f)
    )
    drawCenteredText(code, w / 2f, h / 2f, fontPx = w * 0.22f, color = SignWhite)
}

/** Additional plate (E): white rectangle with black border. */
private fun DrawScope.drawAdditionalRect(code: String) {
    val w = size.width
    val h = size.height
    val top = h * 0.28f
    val bottom = h * 0.72f
    drawRect(
        color = SignWhite,
        topLeft = Offset(w * 0.05f, top),
        size = androidx.compose.ui.geometry.Size(w * 0.90f, bottom - top)
    )
    drawRect(
        color = SignBlack,
        topLeft = Offset(w * 0.05f, top),
        size = androidx.compose.ui.geometry.Size(w * 0.90f, bottom - top),
        style = Stroke(width = w * 0.025f)
    )
    drawCenteredText(code, w / 2f, (top + bottom) / 2f, fontPx = w * 0.20f)
}

// ────────────────────────────────────────────────────────────────────────
// Detail sheet
// ────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignDetailSheet(
    sign: CzechTrafficSign,
    language: LanguageCode,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme
    val cs = language == LanguageCode.CS
    val title = if (cs) sign.nameCs else sign.nameEn
    val description = if (cs) sign.descriptionCs else sign.descriptionEn
    val categoryLabel = if (cs) sign.category.nameCs else sign.category.nameEn

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SignShape(sign = sign, sizeDp = 112)
                }
                Spacer(Modifier.size(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sign.code,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (cs) "Popis" else "Description",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// Catalog — Czech traffic signs per vyhláška č. 294/2015 Sb.
// (Descriptions are condensed 1–2-sentence summaries for quick reference;
// they are not a substitute for the legal text.)
// ────────────────────────────────────────────────────────────────────────

private val TrafficSigns: List<CzechTrafficSign> = listOf(
    // ───────── Výstražné (A) ─────────
    CzechTrafficSign(
        "A1a", "Zatáčka vpravo", "Curve to the right",
        "Upozorňuje na nebezpečnou zatáčku vpravo.",
        "Warns of a dangerous right-hand curve.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A1b", "Zatáčka vlevo", "Curve to the left",
        "Upozorňuje na nebezpečnou zatáčku vlevo.",
        "Warns of a dangerous left-hand curve.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A2a", "Dvojitá zatáčka, první vpravo", "Double curve, first to the right",
        "Dvě za sebou následující nebezpečné zatáčky, první vpravo.",
        "Two successive dangerous curves, first to the right.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A2b", "Dvojitá zatáčka, první vlevo", "Double curve, first to the left",
        "Dvě za sebou následující nebezpečné zatáčky, první vlevo.",
        "Two successive dangerous curves, first to the left.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A3", "Křižovatka", "Crossroads",
        "Upozorňuje na křižovatku, kde není přednost určena svislou značkou.",
        "Warns of a crossroads where priority is not set by a sign.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A4", "Pozor, kruhový objezd", "Roundabout ahead",
        "Upozorňuje na blížící se kruhový objezd.",
        "Warns of an approaching roundabout.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A5a", "Nebezpečné klesání", "Dangerous descent",
        "Upozorňuje na úsek s výrazným klesáním (sklon je uveden v %).",
        "Warns of a section with steep descent (grade shown in %).",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A5b", "Nebezpečné stoupání", "Dangerous ascent",
        "Upozorňuje na úsek s výrazným stoupáním.",
        "Warns of a section with steep ascent.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A6a", "Zúžená vozovka (z obou stran)", "Road narrows (both sides)",
        "Vozovka se zužuje z obou stran.",
        "The road narrows from both sides.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A6b", "Zúžená vozovka (z jedné strany)", "Road narrows (one side)",
        "Vozovka se zužuje pouze z jedné strany.",
        "The road narrows from one side only.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A7a", "Pozor, zpomalovací práh", "Road hump",
        "Upozorňuje na umělou nerovnost (zpomalovací práh) na vozovce.",
        "Warns of a speed hump in the roadway.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A7b", "Nerovnost vozovky", "Uneven road",
        "Upozorňuje na nerovnost vozovky — výmoly, vlny apod.",
        "Warns of uneven surface — potholes, ripples, etc.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A8", "Nebezpečí smyku", "Slippery road",
        "Úsek, na kterém hrozí smyk (mokro, bláto, listí, náledí).",
        "Section where skidding is likely (wet, mud, leaves, ice).",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A9", "Provoz v obou směrech", "Two-way traffic",
        "Konec jednosměrného úseku — následuje obousměrný provoz.",
        "End of one-way section — two-way traffic ahead.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A10", "Světelné signály", "Traffic signals ahead",
        "Upozorňuje na světelnou signalizaci na křižovatce nebo přechodu.",
        "Warns of upcoming traffic signals.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A11", "Pozor, přechod pro chodce", "Pedestrian crossing ahead",
        "Upozorňuje na blížící se přechod pro chodce.",
        "Warns of an approaching pedestrian crossing.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A12", "Děti", "Children",
        "Úsek u školy nebo hřiště — zvýšený pohyb dětí.",
        "Area near a school or playground — increased child activity.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A13", "Zvěř", "Wild animals",
        "Úsek, kde je pravděpodobný pohyb zvěře přes vozovku.",
        "Section where wildlife may cross the road.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A14", "Domácí zvířata", "Domestic animals",
        "Úsek, kde je pravděpodobný pohyb domácích zvířat.",
        "Section where domestic animals may be on the road.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A15", "Práce na silnici", "Road works",
        "Upozorňuje na probíhající silniční práce.",
        "Warns of ongoing road works.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A16", "Boční vítr", "Side wind",
        "Úsek, kde může foukat silný boční vítr.",
        "Section where strong crosswinds may occur.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A17", "Odlétávající štěrk", "Loose chippings",
        "Na vozovce je volný štěrk, který může odlétávat.",
        "Loose gravel on the road that may fly up.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A18", "Padající kamení", "Falling rocks",
        "Nebezpečí padajícího kamení ze svahu u vozovky.",
        "Danger of rocks falling from roadside slope.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A19", "Cyklisté", "Cyclists",
        "Úsek s výskytem cyklistů nebo křížení s cyklistickou stezkou.",
        "Section with cyclists or a cycle-route crossing.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A20", "Letadla", "Low-flying aircraft",
        "Úsek, kde přelétávají nízko letící letadla.",
        "Section with low-flying aircraft.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A21", "Tramvaj", "Tram",
        "Upozorňuje na křížení s tramvajovou tratí mimo křižovatku.",
        "Warns of a tram-track crossing outside an intersection.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A22", "Jiné nebezpečí", "Other danger",
        "Obecné upozornění na nebezpečí, které nelze vyjádřit jinou značkou. Blíže určeno dodatkovou tabulkou.",
        "General warning of a hazard not covered by other signs; details on a supplementary plate.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A23", "Kolona", "Traffic queue",
        "Upozorňuje na tvořící se nebo pravděpodobnou kolonu vozidel.",
        "Warns of a traffic jam ahead.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A24", "Náledí", "Ice / icy road",
        "Úsek, kde se často tvoří náledí.",
        "Section prone to ice formation.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A25", "Tunel", "Tunnel",
        "Upozorňuje na blížící se tunel; často doplněn o délku.",
        "Warns of an approaching tunnel; often shows length.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A26", "Mlha", "Fog",
        "Úsek s častým výskytem mlhy.",
        "Section where fog often occurs.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A27", "Nehoda", "Accident",
        "Informuje o dopravní nehodě před vozidlem.",
        "Warns of an accident ahead.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A29", "Železniční přejezd se závorami", "Level crossing with barriers",
        "Blížící se železniční přejezd vybavený závorami.",
        "Approaching level crossing with barriers.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A30", "Železniční přejezd bez závor", "Level crossing without barriers",
        "Blížící se železniční přejezd bez závor.",
        "Approaching level crossing without barriers.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A31a", "Návěstní deska (240 m)", "Countdown marker (240 m)",
        "Tři pruhy — 240 m před železničním přejezdem.",
        "Three bars — 240 m before a level crossing.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A31b", "Návěstní deska (160 m)", "Countdown marker (160 m)",
        "Dva pruhy — 160 m před železničním přejezdem.",
        "Two bars — 160 m before a level crossing.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A31c", "Návěstní deska (80 m)", "Countdown marker (80 m)",
        "Jeden pruh — 80 m před železničním přejezdem.",
        "One bar — 80 m before a level crossing.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A32a", "Výstražný kříž jednokolejný", "St Andrew's cross (single track)",
        "Označuje místo železničního přejezdu s jednou kolejí.",
        "Marks a single-track level crossing location.",
        SignCategory.WARNING
    ),
    CzechTrafficSign(
        "A32b", "Výstražný kříž vícekolejný", "St Andrew's cross (multi-track)",
        "Označuje místo železničního přejezdu s více kolejemi.",
        "Marks a multi-track level crossing location.",
        SignCategory.WARNING
    ),

    // ───────── Zákazové (B) ─────────
    CzechTrafficSign(
        "B1", "Zákaz vjezdu všech vozidel (v obou směrech)", "No vehicles (both directions)",
        "Žádnému vozidlu není dovoleno vjet do označeného úseku.",
        "No vehicle may enter the marked section.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B2", "Zákaz vjezdu všech vozidel", "No entry",
        "Zakazuje vjezd vozidel z této strany; pro protisměr jednosměrky.",
        "Prohibits entry from this side; used for one-way streets.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B3", "Zákaz vjezdu motorových vozidel", "No motor vehicles",
        "Zakazuje vjezd všech motorových vozidel kromě jednostopých bez postranního vozíku.",
        "Prohibits all motor vehicles except single-track without sidecar.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B4", "Zákaz vjezdu nákladních automobilů", "No trucks",
        "Zakazuje vjezd nákladním automobilům (nad uvedenou hmotnost).",
        "Prohibits entry of lorries (above stated mass).",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B5", "Zákaz vjezdu autobusů", "No buses",
        "Zakazuje vjezd autobusům.",
        "Prohibits entry of buses.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B6", "Zákaz vjezdu traktorů", "No tractors",
        "Zakazuje vjezd traktorům a jiným zvláštním vozidlům.",
        "Prohibits entry of tractors and special vehicles.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B7", "Zákaz vjezdu motocyklů", "No motorcycles",
        "Zakazuje vjezd motocyklů.",
        "Prohibits entry of motorcycles.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B8", "Zákaz vjezdu jízdních kol", "No bicycles",
        "Zakazuje vjezd jízdních kol.",
        "Prohibits entry of bicycles.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B9", "Zákaz vjezdu ručních vozíků", "No handcarts",
        "Zakazuje vjezd ručních vozíků o šířce nad 0,6 m.",
        "Prohibits handcarts wider than 0.6 m.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B10", "Zákaz vstupu chodců", "No pedestrians",
        "Zakazuje vstup chodců na vozovku nebo do daného úseku.",
        "Prohibits pedestrians from entering the section.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B11", "Zákaz vjezdu všech motorových vozidel kromě motocyklů", "No motor vehicles except motorcycles",
        "Zakazuje vjezd motorových vozidel s výjimkou motocyklů.",
        "Prohibits motor vehicles except motorcycles.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B12", "Zákaz vjezdu vyznačených vozidel", "No designated vehicles",
        "Zakazuje vjezd vozidlům zobrazeným na značce (kombinace symbolů).",
        "Prohibits entry of the vehicle types depicted.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B13", "Zákaz vjezdu vozidel nad okamžitou hmotnost", "Weight limit",
        "Zakazuje vjezd vozidel, jejichž hmotnost překračuje uvedenou hodnotu (v tunách).",
        "Prohibits vehicles exceeding the indicated mass (tonnes).",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B14", "Zákaz vjezdu vozidel na nápravu", "Axle weight limit",
        "Zákaz vjezdu vozidel, jejichž hmotnost na nápravu překračuje hodnotu.",
        "Prohibits vehicles whose axle load exceeds the value.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B15", "Zákaz vjezdu vozidel, jejichž šířka přesahuje", "Width limit",
        "Zakazuje vjezd vozidel, jejichž šířka (včetně nákladu) přesahuje uvedenou hodnotu.",
        "Prohibits vehicles wider than the indicated value (incl. load).",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B16", "Zákaz vjezdu vozidel, jejichž výška přesahuje", "Height limit",
        "Zakazuje vjezd vozidel, jejichž výška přesahuje uvedenou hodnotu.",
        "Prohibits vehicles taller than the indicated value.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B17", "Zákaz vjezdu vozidel, jejichž délka přesahuje", "Length limit",
        "Zakazuje vjezd vozidel, jejichž délka přesahuje uvedenou hodnotu.",
        "Prohibits vehicles longer than the indicated value.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B18", "Zákaz vjezdu vozidel přepravujících nebezpečný náklad", "No dangerous goods",
        "Zakazuje vjezd vozidel s nebezpečným nákladem podléhajícím ADR.",
        "Prohibits vehicles carrying ADR dangerous goods.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B19", "Zákaz vjezdu vozidel přepravujících náklad, který může ohrozit vodu",
        "No vehicles with water-polluting load",
        "Zakazuje vjezd vozidel s nákladem ohrožujícím vodní zdroje.",
        "Prohibits vehicles whose load may pollute water sources.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B20a", "Nejvyšší dovolená rychlost", "Maximum speed limit",
        "Nejvyšší dovolená rychlost uvedená číslicí v km/h.",
        "Maximum permitted speed shown in km/h.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B20b", "Konec nejvyšší dovolené rychlosti", "End of speed limit",
        "Konec úseku, kde platila snížená nejvyšší dovolená rychlost.",
        "End of the reduced-speed section.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B21a", "Zákaz předjíždění", "No overtaking",
        "Zákaz předjíždět motorová vozidla zleva; nelze předjet jedoucí vozidlo.",
        "Prohibits overtaking moving motor vehicles on the left.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B21b", "Konec zákazu předjíždění", "End of no overtaking",
        "Konec úseku se zákazem předjíždění.",
        "End of the no-overtaking section.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B22a", "Zákaz předjíždění pro nákladní automobily", "No overtaking for trucks",
        "Řidičům nákladních automobilů zakazuje předjíždět.",
        "Prohibits overtaking by trucks.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B22b", "Konec zákazu předjíždění pro nákladní automobily", "End of no overtaking for trucks",
        "Konec úseku se zákazem předjíždění pro nákladní automobily.",
        "End of the truck-overtaking prohibition.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B23a", "Zákaz zvukových výstražných znamení", "No audible warning",
        "Zákaz užívat zvukové výstražné znamení (klakson).",
        "Prohibits use of audible warning devices (horn).",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B23b", "Konec zákazu zvukových výstražných znamení", "End of no audible warning",
        "Konec úseku se zákazem zvukového znamení.",
        "End of the horn-prohibition section.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B24a", "Zákaz odbočování vpravo", "No right turn",
        "Zakazuje odbočit vpravo na nejbližší křižovatce.",
        "Prohibits right turn at the next intersection.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B24b", "Zákaz odbočování vlevo", "No left turn",
        "Zakazuje odbočit vlevo na nejbližší křižovatce.",
        "Prohibits left turn at the next intersection.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B25", "Zákaz otáčení", "No U-turn",
        "Zakazuje otáčení ve vyznačeném úseku.",
        "Prohibits U-turns in the marked section.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B26", "Konec všech zákazů", "End of all restrictions",
        "Ukončuje platnost všech dříve vyznačených zákazů.",
        "Ends all previously posted prohibitions.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B28", "Zákaz zastavení", "No stopping",
        "Zakazuje zastavení a stání na vyznačené straně vozovky.",
        "Prohibits stopping or parking on the marked side.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B29", "Zákaz stání", "No parking",
        "Zakazuje stání, zastavit lze pouze krátce pro naložení/vyložení.",
        "Prohibits parking; brief stops for loading are allowed.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B30", "Zákaz vstupu chodcům", "No pedestrian entry",
        "Zakazuje vstup chodcům do vyznačeného prostoru.",
        "Prohibits pedestrians from entering the area.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B32", "Jiný zákaz", "Other prohibition",
        "Jiný zákaz určený textem na značce (např. Průjezd zakázán).",
        "Other prohibition specified by text on the sign.",
        SignCategory.PROHIBITION
    ),
    CzechTrafficSign(
        "B34", "Nejmenší vzdálenost mezi vozidly", "Minimum distance",
        "Stanovuje nejmenší vzdálenost, kterou musí řidič udržovat za vozidlem.",
        "Specifies the minimum distance to keep from the vehicle ahead.",
        SignCategory.PROHIBITION
    ),

    // ───────── Příkazové (C) ─────────
    CzechTrafficSign(
        "C1", "Kruhový objezd", "Roundabout",
        "Přikazuje jízdu po kruhovém objezdu ve směru šipek.",
        "Mandatory direction of travel on a roundabout.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C2a", "Přikázaný směr jízdy přímo", "Go straight",
        "Přikazuje pokračovat v jízdě přímo.",
        "Mandatory straight-ahead direction.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C2b", "Přikázaný směr jízdy vpravo", "Turn right",
        "Přikazuje odbočit vpravo.",
        "Mandatory right turn.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C2c", "Přikázaný směr jízdy vlevo", "Turn left",
        "Přikazuje odbočit vlevo.",
        "Mandatory left turn.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C2d", "Přikázaný směr jízdy přímo a vpravo", "Straight or right",
        "Povoluje jízdu přímo nebo vpravo.",
        "Straight ahead or right turn allowed.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C2e", "Přikázaný směr jízdy přímo a vlevo", "Straight or left",
        "Povoluje jízdu přímo nebo vlevo.",
        "Straight ahead or left turn allowed.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C2f", "Přikázaný směr jízdy vpravo a vlevo", "Right or left",
        "Povoluje odbočit vpravo nebo vlevo.",
        "Right or left turn allowed.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C3a", "Přikázaný směr objíždění vpravo", "Pass on right",
        "Přikazuje objíždět překážku po pravé straně.",
        "Pass the obstacle on the right side.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C3b", "Přikázaný směr objíždění vlevo", "Pass on left",
        "Přikazuje objíždět překážku po levé straně.",
        "Pass the obstacle on the left side.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C4a", "Stezka pro cyklisty", "Cycle path",
        "Vyhrazená stezka pro cyklisty; jiná vozidla zákaz.",
        "Cycle-only path; other vehicles prohibited.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C4b", "Konec stezky pro cyklisty", "End of cycle path",
        "Ukončuje stezku vyhrazenou pro cyklisty.",
        "End of the cycle-only path.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C5a", "Nejnižší dovolená rychlost", "Minimum speed",
        "Stanovuje nejnižší dovolenou rychlost v km/h.",
        "Sets the minimum permitted speed in km/h.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C5b", "Konec nejnižší dovolené rychlosti", "End of minimum speed",
        "Konec úseku s nejnižší dovolenou rychlostí.",
        "End of the minimum-speed section.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C6a", "Sněhové řetězy", "Snow chains required",
        "Přikazuje použití sněhových řetězů na hnaných kolech.",
        "Requires snow chains on the driven wheels.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C6b", "Konec přikázaného užití sněhových řetězů", "End of snow chains",
        "Konec úseku s povinnými sněhovými řetězy.",
        "End of the snow-chains-required section.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C7a", "Stezka pro chodce", "Pedestrian path",
        "Stezka vyhrazená chodcům.",
        "Path reserved for pedestrians.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C7b", "Konec stezky pro chodce", "End of pedestrian path",
        "Ukončuje stezku pro chodce.",
        "End of the pedestrian path.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C8a", "Stezka pro chodce a cyklisty (společná)", "Shared ped/cycle path",
        "Společná stezka pro chodce a cyklisty; dbají vzájemného ohledu.",
        "Shared path for pedestrians and cyclists; mutual consideration.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C9a", "Stezka pro chodce a cyklisty (dělená)", "Segregated ped/cycle path",
        "Dělená stezka — oddělené pruhy pro chodce a cyklisty.",
        "Segregated path — separate lanes for peds and cyclists.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C10a", "Stezka pro jezdce na koni", "Bridle path",
        "Vyhrazená stezka pro jezdce na koni.",
        "Path reserved for horse riders.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C13a", "Rozsviť světla", "Headlights on",
        "Přikazuje rozsvítit obrysová a potkávací světla (např. tunel).",
        "Requires headlights on (e.g. entering a tunnel).",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C13b", "Rozsvícená světla — konec", "End of headlights-on",
        "Konec úseku s povinností svítit.",
        "End of the headlights-required section.",
        SignCategory.COMMAND
    ),
    CzechTrafficSign(
        "C14a", "Jiný příkaz", "Other command",
        "Jiný příkaz určený textem na značce.",
        "Other command specified by text on the sign.",
        SignCategory.COMMAND
    ),

    // ───────── Informativní provozní (IP) ─────────
    CzechTrafficSign(
        "IP1a", "Vyhrazený jízdní pruh", "Reserved lane",
        "Jízdní pruh vyhrazený pro vozidla veřejné dopravy.",
        "Lane reserved for public transport vehicles.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP2", "Zpomalovací práh", "Road hump (info)",
        "Informuje o zpomalovacím prahu na vozovce.",
        "Informs of a speed hump in the road.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP3", "Podchod nebo nadchod", "Underpass / overpass",
        "Upozorňuje na podchod nebo nadchod pro chodce.",
        "Indicates a pedestrian underpass or overpass.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP4a", "Jednosměrný provoz", "One-way traffic",
        "Začátek úseku s jednosměrným provozem.",
        "Start of a one-way section.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP4b", "Jednosměrný provoz (zprava/zleva)", "One-way (directional)",
        "Jednosměrný provoz s uvedením směru (šipka vpravo nebo vlevo).",
        "One-way traffic with indicated direction (arrow).",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP5", "Doporučená rychlost", "Recommended speed",
        "Doporučená rychlost pro daný úsek v km/h.",
        "Recommended speed for the section in km/h.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP6", "Přechod pro chodce", "Pedestrian crossing",
        "Místo přechodu pro chodce.",
        "Pedestrian crossing location.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP7", "Přejezd pro cyklisty", "Cycle crossing",
        "Místo přejezdu pro cyklisty.",
        "Cycle crossing location.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP10a", "Slepá pozemní komunikace", "Dead-end road",
        "Označuje silnici bez průjezdu.",
        "Marks a no-through road.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP10b", "Návěst před slepou komunikací", "Dead-end advance",
        "Návěstní informace o slepé komunikaci před odbočkou.",
        "Advance information about an upcoming dead-end.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP11a", "Parkoviště", "Parking",
        "Označuje plochu určenou k parkování.",
        "Designates a parking area.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP11b", "Parkoviště s parkovacím kotoučem", "Parking with disc",
        "Parkování povoleno s použitím parkovacího kotouče (časově omezené).",
        "Parking allowed with parking disc (time-limited).",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP11c", "Parkoviště s parkovacím automatem", "Paid parking",
        "Parkování povoleno po zaplacení v parkovacím automatu.",
        "Parking allowed upon payment at a pay-and-display machine.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP12", "Vyhrazené parkoviště", "Reserved parking",
        "Parkoviště vyhrazené určitému uživateli nebo druhu vozidla (např. invalidé).",
        "Parking reserved for a specific user or vehicle type (e.g. disabled).",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP13c", "Parkoviště P+R", "Park and Ride",
        "Parkoviště umožňující přestup na veřejnou dopravu.",
        "Park-and-ride lot for transfer to public transport.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP14a", "Dálnice", "Motorway",
        "Začátek dálnice — platí zvláštní pravidla.",
        "Start of motorway — special rules apply.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP14b", "Konec dálnice", "End of motorway",
        "Ukončuje dálnici.",
        "End of motorway.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP15a", "Silnice pro motorová vozidla", "Expressway",
        "Začátek silnice pro motorová vozidla.",
        "Start of expressway (motor-vehicle-only road).",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP15b", "Konec silnice pro motorová vozidla", "End of expressway",
        "Konec silnice pro motorová vozidla.",
        "End of expressway.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP16", "Uspořádání jízdních pruhů", "Lane arrangement",
        "Informace o uspořádání pruhů před křižovatkou — kde kdo kam jede.",
        "Information on lane arrangement before an intersection.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP18a", "Zvýšení počtu jízdních pruhů", "Lane added",
        "Upozornění, že se počet jízdních pruhů zvyšuje.",
        "Warns that the number of lanes is increasing.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP18b", "Snížení počtu jízdních pruhů", "Lane reduction",
        "Upozornění, že se počet jízdních pruhů snižuje.",
        "Warns that the number of lanes is decreasing.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP19", "Řadicí pruhy", "Sorting lanes",
        "Informuje o řadicích pruzích před křižovatkou.",
        "Indicates sorting lanes before the intersection.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP22", "Změna místní úpravy", "Traffic layout change",
        "Oznámení o dočasné nebo trvalé změně místní úpravy provozu.",
        "Notice of a temporary or permanent traffic-layout change.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP25a", "Zóna s omezenou rychlostí", "Speed-limit zone",
        "Začátek zóny s omezenou nejvyšší dovolenou rychlostí.",
        "Start of a reduced-speed zone.",
        SignCategory.INFO_OP
    ),
    CzechTrafficSign(
        "IP25b", "Konec zóny s omezenou rychlostí", "End of speed-limit zone",
        "Konec zóny s omezenou nejvyšší dovolenou rychlostí.",
        "End of the reduced-speed zone.",
        SignCategory.INFO_OP
    ),

    // ───────── Informativní směrové (IS) ─────────
    CzechTrafficSign(
        "IS1a", "Směrová tabule pro příjezd k dálnici", "Direction to motorway",
        "Ukazuje směr k nájezdu na dálnici (zelená tabule).",
        "Indicates the direction to a motorway entry (green sign).",
        SignCategory.INFO_DIR
    ),
    CzechTrafficSign(
        "IS2", "Směrová tabule (s cílem)", "Direction sign (destination)",
        "Směrová tabule udávající cíl a vzdálenost.",
        "Direction sign giving destination and distance.",
        SignCategory.INFO_DIR
    ),
    CzechTrafficSign(
        "IS3a", "Směrová tabule silnice I. třídy", "Direction sign (class I)",
        "Směrová tabule na silnici první třídy.",
        "Direction sign on a class-I road.",
        SignCategory.INFO_DIR
    ),
    CzechTrafficSign(
        "IS3b", "Směrová tabule silnice II. třídy", "Direction sign (class II)",
        "Směrová tabule na silnici druhé třídy.",
        "Direction sign on a class-II road.",
        SignCategory.INFO_DIR
    ),
    CzechTrafficSign(
        "IS4", "Směrová tabule pro místní cíl", "Local destination sign",
        "Směrová tabule pro místní cíle (ulice, městské části).",
        "Direction sign for local destinations.",
        SignCategory.INFO_DIR
    ),
    CzechTrafficSign(
        "IS5", "Směrová tabule pro cyklisty", "Cycle direction sign",
        "Směrová tabule cyklistické trasy.",
        "Direction sign for a cycle route.",
        SignCategory.INFO_DIR
    ),
    CzechTrafficSign(
        "IS9a", "Návěst před křižovatkou", "Advance direction sign",
        "Návěstní tabule informující o křižovatce a směrech.",
        "Advance sign for an intersection and directions.",
        SignCategory.INFO_DIR
    ),
    CzechTrafficSign(
        "IS9b", "Tabule s uspořádáním křižovatky", "Intersection diagram",
        "Diagram uspořádání křižovatky se směry.",
        "Diagram of the intersection layout with directions.",
        SignCategory.INFO_DIR
    ),
    CzechTrafficSign(
        "IS11a", "Dálková navigace", "Route direction",
        "Tabule pro dálkovou navigaci — hlavní tahy.",
        "Direction sign for long-distance routes.",
        SignCategory.INFO_DIR
    ),
    CzechTrafficSign(
        "IS12a", "Obec", "Locality (begin)",
        "Název obce — začátek obce (v obci platí 50 km/h).",
        "Locality name — start of built-up area (50 km/h limit).",
        SignCategory.INFO_DIR
    ),
    CzechTrafficSign(
        "IS12b", "Konec obce", "Locality (end)",
        "Konec obce — obecný rychlostní limit mimo obec.",
        "End of built-up area — general out-of-town speed applies.",
        SignCategory.INFO_DIR
    ),
    CzechTrafficSign(
        "IS13", "Označení ulice", "Street name",
        "Pojmenování ulice.",
        "Street-name plate.",
        SignCategory.INFO_DIR
    ),
    CzechTrafficSign(
        "IS15a", "Označení turistického cíle", "Tourist destination",
        "Hnědá tabule označující turistický cíl.",
        "Brown sign marking a tourist attraction.",
        SignCategory.INFO_DIR
    ),
    CzechTrafficSign(
        "IS16a", "Hraniční přechod", "Border crossing",
        "Označuje hraniční přechod.",
        "Marks a border crossing.",
        SignCategory.INFO_DIR
    ),
    CzechTrafficSign(
        "IS18", "Kilometrovník", "Kilometre post",
        "Označuje kilometrovou polohu na silnici.",
        "Marks the kilometre position on the road.",
        SignCategory.INFO_DIR
    ),

    // ───────── Zóny (IZ) ─────────
    CzechTrafficSign(
        "IZ1a", "Obec", "Locality (zone begin)",
        "Začátek obce jako dopravní zóna.",
        "Start of built-up area as a traffic zone.",
        SignCategory.ZONE
    ),
    CzechTrafficSign(
        "IZ1b", "Konec obce", "Locality (zone end)",
        "Konec obce jako dopravní zóny.",
        "End of built-up area as a traffic zone.",
        SignCategory.ZONE
    ),
    CzechTrafficSign(
        "IZ2a", "Obytná zóna", "Residential zone (begin)",
        "Začátek obytné zóny — chodci smí užívat celou šířku, max. 20 km/h.",
        "Start of residential zone — peds may use the full width, max 20 km/h.",
        SignCategory.ZONE
    ),
    CzechTrafficSign(
        "IZ2b", "Konec obytné zóny", "Residential zone (end)",
        "Konec obytné zóny.",
        "End of residential zone.",
        SignCategory.ZONE
    ),
    CzechTrafficSign(
        "IZ3a", "Pěší zóna", "Pedestrian zone (begin)",
        "Začátek pěší zóny — povolen pouze chůze a vozidla určená na značce.",
        "Start of pedestrian zone — only walking and permitted vehicles.",
        SignCategory.ZONE
    ),
    CzechTrafficSign(
        "IZ3b", "Konec pěší zóny", "Pedestrian zone (end)",
        "Konec pěší zóny.",
        "End of pedestrian zone.",
        SignCategory.ZONE
    ),
    CzechTrafficSign(
        "IZ4a", "Zóna s dopravním omezením", "Restricted zone (begin)",
        "Začátek zóny s vyznačenými omezeními (např. zákaz stání).",
        "Start of zone with displayed restrictions (e.g. no parking).",
        SignCategory.ZONE
    ),
    CzechTrafficSign(
        "IZ4b", "Konec zóny s dopravním omezením", "Restricted zone (end)",
        "Konec zóny s dopravním omezením.",
        "End of the restricted zone.",
        SignCategory.ZONE
    ),
    CzechTrafficSign(
        "IZ8a", "Zóna s placeným stáním", "Paid-parking zone (begin)",
        "Začátek zóny s placeným stáním.",
        "Start of a paid-parking zone.",
        SignCategory.ZONE
    ),
    CzechTrafficSign(
        "IZ8b", "Konec zóny s placeným stáním", "Paid-parking zone (end)",
        "Konec zóny s placeným stáním.",
        "End of a paid-parking zone.",
        SignCategory.ZONE
    ),

    // ───────── Dodatkové (E) ─────────
    CzechTrafficSign(
        "E1", "Počet", "Count",
        "Upřesňuje počet opakování nebo množství (např. železničních kolejí).",
        "Specifies a number or count (e.g. number of tracks).",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E2a", "Tvar křižovatky", "Intersection shape",
        "Znázorňuje tvar křižovatky a hlavní silnici.",
        "Depicts the intersection shape and main road.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E2b", "Průběh hlavní silnice", "Main road course",
        "Zobrazuje průběh hlavní silnice křižovatkou.",
        "Shows how the main road runs through the intersection.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E3a", "Vzdálenost", "Distance",
        "Vzdálenost ke značce, které se dodatková tabulka týká.",
        "Distance to the sign the plate refers to.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E3b", "Vzdálenost od značky", "Distance from sign",
        "Vzdálenost od této značky ke konci nebezpečného úseku.",
        "Distance from this sign to the end of the hazardous section.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E4", "Délka úseku", "Section length",
        "Délka úseku, po kterou značka platí.",
        "Length of the section the sign applies to.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E5", "Celková hmotnost", "Total mass",
        "Omezuje platnost značky na vozidla nad uvedenou hmotnost.",
        "Limits the sign's validity to vehicles above the stated mass.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E6", "Za deště", "In rain",
        "Omezuje platnost značky na období deště / mokré vozovky.",
        "Limits the sign's validity to rain / wet road conditions.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E7a", "Vjezd/výjezd vpravo", "Entry/exit right",
        "Označuje místo vjezdu/výjezdu vpravo.",
        "Marks an entry/exit on the right.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E7b", "Vjezd/výjezd vlevo", "Entry/exit left",
        "Označuje místo vjezdu/výjezdu vlevo.",
        "Marks an entry/exit on the left.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E8a", "Začátek úseku", "Start of section",
        "Označuje začátek úseku, ve kterém platí předcházející značka.",
        "Marks the start of the section where the preceding sign applies.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E8b", "Průběh úseku", "Continuation of section",
        "Informuje, že úsek trvá.",
        "Indicates the section continues.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E8c", "Konec úseku", "End of section",
        "Označuje konec úseku s předchozí značkou.",
        "Marks the end of the section with the preceding sign.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E9", "Druh vozidla", "Vehicle type",
        "Omezuje platnost na určitý druh vozidla (např. nákladní).",
        "Limits validity to a particular vehicle type (e.g. trucks).",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E10", "Tvar tramvajového pásu", "Tram-lane layout",
        "Znázorňuje tvar tramvajového pásu v místě.",
        "Depicts the tram-lane layout at the location.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E11", "Bez poplatku / Mimo vozidla zásobování",
        "Except loading / No charge",
        "Výjimka z platnosti značky — např. zásobování, MHD.",
        "Exception from the sign — e.g. loading vehicles or public transport.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E12", "Text", "Text",
        "Doplňující textová informace k dopravní značce.",
        "Additional textual information for the traffic sign.",
        SignCategory.ADDITIONAL
    ),
    CzechTrafficSign(
        "E13", "Jiný údaj", "Other data",
        "Obecná dodatková tabulka s jiným údajem (obvykle piktogramem).",
        "Generic supplementary plate with other data (usually a pictogram).",
        SignCategory.ADDITIONAL
    )
)
