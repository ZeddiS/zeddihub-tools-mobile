package com.zeddihub.mobile.ui.helpers.school

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private data class Formula(
    val name: String,
    val formula: AnnotatedString,
    val when_: String,
    val example: String,
)

private fun plain(s: String) = AnnotatedString(s)

/**
 * Helper: builds a string with ^(exponent) tokens rendered as superscript.
 * Use the token "^{..}" to wrap multi-char exponents, or "^x" for single.
 */
private fun rich(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '^' && i + 1 < text.length) {
            val next = text[i + 1]
            if (next == '{') {
                val end = text.indexOf('}', i + 2)
                if (end > 0) {
                    withStyle(SpanStyle(baselineShift = BaselineShift.Superscript)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 1
                    continue
                }
            } else {
                withStyle(SpanStyle(baselineShift = BaselineShift.Superscript)) {
                    append(next.toString())
                }
                i += 2
                continue
            }
        }
        append(c.toString())
        i++
    }
}

private val algebra = listOf(
    Formula(
        "Kvadratická rovnice",
        rich("ax^2 + bx + c = 0  →  x = (−b ± √D) / (2a)"),
        "Kdy použít: řešení rovnice druhého stupně.",
        "Příklad: x^2 − 5x + 6 = 0 → x = 2 nebo x = 3.",
    ),
    Formula(
        "Diskriminant",
        rich("D = b^2 − 4ac"),
        "D > 0 dva reálné kořeny, D = 0 jeden (dvojnásobný), D < 0 žádný reálný.",
        "b=−5, a=1, c=6 → D = 25 − 24 = 1.",
    ),
    Formula(
        "Vietovy vzorce",
        rich("x₁ + x₂ = −b/a,   x₁·x₂ = c/a"),
        "Rychlé ověření kořenů kvadratické rovnice.",
        "x^2 − 5x + 6: součet 5, součin 6 → 2, 3.",
    ),
    Formula(
        "Druhá mocnina součtu",
        rich("(a + b)^2 = a^2 + 2ab + b^2"),
        "Roznásobení závorek.",
        "(x+3)^2 = x^2 + 6x + 9.",
    ),
    Formula(
        "Druhá mocnina rozdílu",
        rich("(a − b)^2 = a^2 − 2ab + b^2"),
        "Stejně jako součet, ale s opačným znaménkem u 2ab.",
        "(x−2)^2 = x^2 − 4x + 4.",
    ),
    Formula(
        "Rozdíl druhých mocnin",
        rich("a^2 − b^2 = (a − b)(a + b)"),
        "Rozklad výrazu na součin.",
        "x^2 − 9 = (x−3)(x+3).",
    ),
    Formula(
        "Binomická věta",
        rich("(a+b)^n = Σ C(n,k) · a^{n−k} · b^k"),
        "Rozvoj (a+b) na n-tou.",
        "(a+b)^3 = a^3 + 3a^2 b + 3ab^2 + b^3.",
    ),
    Formula(
        "Aritmetická posloupnost",
        rich("a_n = a_1 + (n−1)·d,   S_n = n(a_1+a_n)/2"),
        "Rozdíl mezi sousedními členy je konstantní.",
        "1, 3, 5, 7 … a_10 = 19.",
    ),
    Formula(
        "Geometrická posloupnost",
        rich("a_n = a_1 · q^{n−1},   S_n = a_1(q^n − 1)/(q − 1)"),
        "Podíl sousedních členů je konstantní.",
        "2, 6, 18 … a_5 = 162.",
    ),
    Formula(
        "Logaritmy — identity",
        rich("log(xy) = log x + log y,   log(x^n) = n·log x,   log_a(x) = ln x / ln a"),
        "Zjednodušení výrazů s logaritmy.",
        "log(100·10) = log 100 + log 10 = 3.",
    ),
)

private val geometry = listOf(
    Formula(
        "Pythagorova věta",
        rich("c^2 = a^2 + b^2"),
        "Pravoúhlý trojúhelník — přepona a odvěsny.",
        "a=3, b=4 → c = 5.",
    ),
    Formula(
        "Obvod/obsah čtverce",
        rich("o = 4a,   S = a^2"),
        "Čtverec se stranou a.",
        "a=5: o=20, S=25.",
    ),
    Formula(
        "Obvod/obsah obdélníku",
        rich("o = 2(a+b),   S = a·b"),
        "Obdélník se stranami a, b.",
        "a=5, b=3: o=16, S=15.",
    ),
    Formula(
        "Obvod/obsah trojúhelníku",
        rich("o = a+b+c,   S = (a·v_a)/2"),
        "Obecný trojúhelník s výškou v_a.",
        "a=6, v_a=4 → S = 12.",
    ),
    Formula(
        "Heronův vzorec",
        rich("S = √(s(s−a)(s−b)(s−c)),  s = (a+b+c)/2"),
        "Obsah trojúhelníku jen ze stran.",
        "a=5, b=6, c=7 → s=9, S ≈ 14,70.",
    ),
    Formula(
        "Kruh",
        rich("o = 2πr,   S = π·r^2"),
        "Kružnice o poloměru r.",
        "r=3: o≈18,85; S≈28,27.",
    ),
    Formula(
        "Lichoběžník",
        rich("S = (a + c)·v / 2"),
        "a, c jsou rovnoběžné strany, v výška.",
        "a=8, c=4, v=3 → S = 18.",
    ),
    Formula(
        "Objem krychle / kvádru",
        rich("V_krychle = a^3,   V_kvádru = a·b·c"),
        "Tělesa s pravoúhlými stěnami.",
        "a=2, b=3, c=4 → V = 24.",
    ),
    Formula(
        "Koule",
        rich("V = (4/3)·π·r^3,   S = 4·π·r^2"),
        "Objem a povrch koule.",
        "r=3 → V ≈ 113,1.",
    ),
    Formula(
        "Válec",
        rich("V = π·r^2·v,   S = 2πr(r+v)"),
        "Rotační válec s výškou v.",
        "r=2, v=5 → V ≈ 62,83.",
    ),
    Formula(
        "Kužel",
        rich("V = (1/3)·π·r^2·v,   S = πr(r+s)"),
        "s je strana kužele = √(r^2+v^2).",
        "r=3, v=4 → s=5; V ≈ 37,7.",
    ),
    Formula(
        "Jehlan",
        rich("V = (1/3)·S_podstavy·v"),
        "Jehlan s libovolnou podstavou.",
        "S=12, v=5 → V = 20.",
    ),
)

private val trig = listOf(
    Formula(
        "Základní identita",
        rich("sin^2 α + cos^2 α = 1"),
        "Pythagorejská identita.",
        "Ze sin α = 0,6 → cos α = 0,8.",
    ),
    Formula(
        "Tangens, cotangens",
        rich("tg α = sin α / cos α,   cotg α = cos α / sin α"),
        "Definice zbylých funkcí.",
        "tg 45° = 1.",
    ),
    Formula(
        "Součtové vzorce",
        rich("sin(α±β) = sin α·cos β ± cos α·sin β\ncos(α±β) = cos α·cos β ∓ sin α·sin β"),
        "Rozklad funkcí součtu/rozdílu úhlů.",
        "cos 75° = cos(45°+30°).",
    ),
    Formula(
        "Dvojnásobný úhel",
        rich("sin 2α = 2 sin α cos α\ncos 2α = cos^2 α − sin^2 α"),
        "Ze součtových vzorců pro β=α.",
        "sin 60° = 2·sin 30°·cos 30°.",
    ),
    Formula(
        "Sinová věta",
        rich("a/sin α = b/sin β = c/sin γ = 2R"),
        "Libovolný trojúhelník, R poloměr opsané kružnice.",
        "Strana proti 30° je polovinou strany proti 90° (při R stejném).",
    ),
    Formula(
        "Kosinová věta",
        rich("c^2 = a^2 + b^2 − 2ab·cos γ"),
        "Zobecněná Pythagorova věta.",
        "γ=60°, a=b=1 → c=1.",
    ),
    Formula(
        "Obsah obecného trojúhelníku",
        rich("S = (1/2)·a·b·sin γ"),
        "Dvě strany a úhel mezi nimi.",
        "a=5, b=6, γ=30° → S = 7,5.",
    ),
)

private val calculus = listOf(
    Formula(
        "Derivace mocniny",
        rich("(x^n)' = n·x^{n−1}"),
        "Základ derivování polynomů.",
        "(x^3)' = 3x^2.",
    ),
    Formula(
        "Derivace exponenciály",
        rich("(e^x)' = e^x,   (a^x)' = a^x · ln a"),
        "Exponenciální růst.",
        "(2^x)' = 2^x · ln 2.",
    ),
    Formula(
        "Derivace logaritmu",
        rich("(ln x)' = 1/x,   (log_a x)' = 1/(x·ln a)"),
        "x > 0.",
        "(ln 3x)' = 1/x (konstanta se ztratí).",
    ),
    Formula(
        "Derivace sin, cos",
        rich("(sin x)' = cos x,   (cos x)' = −sin x"),
        "Periodické funkce.",
        "(sin 2x)' = 2 cos 2x (řetízkové pr.).",
    ),
    Formula(
        "Pravidlo součtu, rozdílu",
        rich("(f ± g)' = f' ± g'"),
        "Lineární operace.",
        "(x^2 + sin x)' = 2x + cos x.",
    ),
    Formula(
        "Pravidlo součinu",
        rich("(f·g)' = f'·g + f·g'"),
        "Derivace součinu dvou funkcí.",
        "(x·sin x)' = sin x + x cos x.",
    ),
    Formula(
        "Pravidlo podílu",
        rich("(f/g)' = (f'g − fg') / g^2"),
        "g ≠ 0.",
        "(sin x / x)' = (x cos x − sin x)/x^2.",
    ),
    Formula(
        "Řetízkové pravidlo",
        rich("(f(g(x)))' = f'(g(x)) · g'(x)"),
        "Derivace složené funkce.",
        "(sin(x^2))' = 2x · cos(x^2).",
    ),
    Formula(
        "Integrál mocniny",
        rich("∫ x^n dx = x^{n+1} / (n+1) + C  (n ≠ −1)"),
        "Základ integrování polynomů.",
        "∫ x^2 dx = x^3/3 + C.",
    ),
    Formula(
        "Základní integrály",
        rich("∫ e^x dx = e^x + C\n∫ 1/x dx = ln|x| + C\n∫ sin x dx = −cos x + C"),
        "Na zapamatování.",
        "∫ cos x dx = sin x + C.",
    ),
)

private data class Section(val title: String, val items: List<Formula>)

private val sections = listOf(
    Section("Algebra", algebra),
    Section("Geometrie", geometry),
    Section("Goniometrie", trig),
    Section("Analýza", calculus),
)

@Composable
fun MathFormulasScreen(padding: PaddingValues) {
    var tab by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        ScrollableTabRow(selectedTabIndex = tab) {
            sections.forEachIndexed { i, s ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(s.title) })
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(sections[tab].items) { f ->
                FormulaCard(f)
            }
        }
    }
}

@Composable
private fun FormulaCard(f: Formula) {
    var expanded by rememberSaveable(f.name) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    f.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(f.formula, style = MaterialTheme.typography.bodyLarge)
                    Text(f.when_, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        f.example,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
