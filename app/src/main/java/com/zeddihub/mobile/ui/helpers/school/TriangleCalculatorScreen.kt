package com.zeddihub.mobile.ui.helpers.school

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType.Companion.Decimal
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private data class Triangle(
    val a: Double, val b: Double, val c: Double,
    val alpha: Double, val beta: Double, val gamma: Double,
)

private data class TriResult(
    val solutions: List<Triangle>,
    val message: String,
)

private fun rad(deg: Double) = deg * PI / 180.0
private fun deg(r: Double) = r * 180.0 / PI

@Composable
fun TriangleCalculatorScreen(padding: PaddingValues) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }
    var al by remember { mutableStateOf("") }
    var be by remember { mutableStateOf("") }
    var ga by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Zadej alespoň 3 hodnoty (strany a úhly). Úhly ve stupních.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("a", a, { a = it }, Modifier.weight(1f))
            NumField("b", b, { b = it }, Modifier.weight(1f))
            NumField("c", c, { c = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("α °", al, { al = it }, Modifier.weight(1f))
            NumField("β °", be, { be = it }, Modifier.weight(1f))
            NumField("γ °", ga, { ga = it }, Modifier.weight(1f))
        }

        val av = a.replace(',', '.').toDoubleOrNull()
        val bv = b.replace(',', '.').toDoubleOrNull()
        val cv = c.replace(',', '.').toDoubleOrNull()
        val alv = al.replace(',', '.').toDoubleOrNull()
        val bev = be.replace(',', '.').toDoubleOrNull()
        val gav = ga.replace(',', '.').toDoubleOrNull()

        val result = solveTriangle(av, bv, cv, alv, bev, gav)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Výsledek", fontWeight = FontWeight.SemiBold)
                Text(result.message)
                result.solutions.forEachIndexed { idx, t ->
                    if (result.solutions.size > 1) Text("Řešení ${idx + 1}:", fontWeight = FontWeight.SemiBold)
                    Text(
                        "a = %.3f, b = %.3f, c = %.3f".format(t.a, t.b, t.c),
                    )
                    Text(
                        "α = %.2f°, β = %.2f°, γ = %.2f°".format(t.alpha, t.beta, t.gamma),
                    )
                    val s = (t.a + t.b + t.c) / 2.0
                    val area = sqrt(max(0.0, s * (s - t.a) * (s - t.b) * (s - t.c)))
                    Text("Obvod = %.3f,  Obsah (Heron) = %.3f".format(t.a + t.b + t.c, area))
                }
            }
        }

        result.solutions.firstOrNull()?.let { t ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Nákres", fontWeight = FontWeight.SemiBold)
                    TriangleCanvas(t, Modifier.fillMaxWidth().height(240.dp).padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit, mod: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = Decimal),
        modifier = mod,
        singleLine = true,
    )
}

/**
 * Solves a triangle given any 3 of 6 inputs. Handles SSS, SAS, ASA/AAS, SSA (2 solutions possible).
 */
private fun solveTriangle(
    a: Double?, b: Double?, c: Double?,
    alpha: Double?, beta: Double?, gamma: Double?,
): TriResult {
    val sides = listOf(a, b, c)
    val angles = listOf(alpha, beta, gamma)
    val knownSides = sides.count { it != null && it > 0 }
    val knownAngles = angles.count { it != null && it > 0 && it < 180 }

    if (knownSides + knownAngles < 3) {
        return TriResult(emptyList(), "Zadej alespoň 3 platné hodnoty.")
    }
    if (knownAngles == 3) {
        return TriResult(emptyList(), "Z tří úhlů trojúhelník jednoznačně neurčíš (potřebuješ alespoň jednu stranu).")
    }
    // Two angles given → compute the third.
    val angSum = (alpha ?: 0.0) + (beta ?: 0.0) + (gamma ?: 0.0)
    if (knownAngles >= 2 && angSum >= 180) {
        return TriResult(emptyList(), "Součet zadaných úhlů ≥ 180°.")
    }

    // SSS
    if (knownSides == 3) {
        val A = a!!; val B = b!!; val C = c!!
        if (A + B <= C || A + C <= B || B + C <= A) {
            return TriResult(emptyList(), "Trojúhelníková nerovnost není splněna.")
        }
        val al = deg(acos(((B * B + C * C - A * A) / (2 * B * C)).coerceIn(-1.0, 1.0)))
        val be = deg(acos(((A * A + C * C - B * B) / (2 * A * C)).coerceIn(-1.0, 1.0)))
        val ga = 180.0 - al - be
        return TriResult(listOf(Triangle(A, B, C, al, be, ga)), "Způsob řešení: SSS (tři strany).")
    }

    // SAS: two sides and the angle between them.
    fun sas(s1: Double, s2: Double, angleBetween: Double, whichAngle: Int): Triangle {
        val opp = sqrt(s1 * s1 + s2 * s2 - 2 * s1 * s2 * cos(rad(angleBetween)))
        // Fill triangle depending on which angle is between which two sides.
        // whichAngle=0 means alpha is between b,c; 1=beta between a,c; 2=gamma between a,b.
        return when (whichAngle) {
            0 -> {
                val A = opp; val B = s1; val C = s2
                val be = deg(asin((B * sin(rad(angleBetween)) / A).coerceIn(-1.0, 1.0)))
                val ga = 180.0 - angleBetween - be
                Triangle(A, B, C, angleBetween, be, ga)
            }
            1 -> {
                val B = opp; val A = s1; val C = s2
                val al = deg(asin((A * sin(rad(angleBetween)) / B).coerceIn(-1.0, 1.0)))
                val ga = 180.0 - angleBetween - al
                Triangle(A, B, C, al, angleBetween, ga)
            }
            else -> {
                val C = opp; val A = s1; val B = s2
                val al = deg(asin((A * sin(rad(angleBetween)) / C).coerceIn(-1.0, 1.0)))
                val be = 180.0 - angleBetween - al
                Triangle(A, B, C, al, be, angleBetween)
            }
        }
    }

    // ASA / AAS: two angles and a side.
    if (knownAngles >= 2 && knownSides >= 1) {
        val al = alpha ?: (180.0 - (beta ?: 0.0) - (gamma ?: 0.0))
        val be = beta ?: (180.0 - al - (gamma ?: 0.0))
        val ga = gamma ?: (180.0 - al - be)
        // Use sine rule from whatever side is known.
        val (ratio, label) = when {
            a != null && al > 0 -> (a / sin(rad(al))) to "ASA/AAS"
            b != null && be > 0 -> (b / sin(rad(be))) to "ASA/AAS"
            c != null && ga > 0 -> (c / sin(rad(ga))) to "ASA/AAS"
            else -> return TriResult(emptyList(), "Chybí platná strana.")
        }
        val A = a ?: ratio * sin(rad(al))
        val B = b ?: ratio * sin(rad(be))
        val C = c ?: ratio * sin(rad(ga))
        return TriResult(listOf(Triangle(A, B, C, al, be, ga)), "Způsob řešení: $label.")
    }

    // Exactly 2 sides + 1 angle → SAS or SSA
    if (knownSides == 2 && knownAngles == 1) {
        // Determine whether angle is between the two known sides (SAS) or not (SSA).
        val angleGiven = listOf(alpha, beta, gamma).indexOfFirst { it != null && it > 0 }
        val missingSideIdx = listOf(a, b, c).indexOfFirst { it == null || it <= 0 }
        // Angle at index k is opposite side k. SAS ⇔ missingSideIdx == angleGiven.
        if (missingSideIdx == angleGiven) {
            // SAS
            val ang = listOf(alpha, beta, gamma)[angleGiven]!!
            val sidesKnown = listOf(a, b, c).mapIndexedNotNull { i, v -> if (v != null && v > 0) v else null }
            val (s1, s2) = sidesKnown[0] to sidesKnown[1]
            val tri = sas(s1, s2, ang, angleGiven)
            return TriResult(listOf(tri), "Způsob řešení: SAS (dvě strany a úhel mezi nimi).")
        } else {
            // SSA — potenciálně dvě řešení.
            val ang = listOf(alpha, beta, gamma)[angleGiven]!!
            val sideOpp = listOf(a, b, c)[angleGiven]
                ?: return TriResult(emptyList(), "Pro SSA zadej stranu protilehlou k úhlu.")
            // The other known side:
            val otherSideIdx = (0..2).first { it != angleGiven && it != missingSideIdx }
            val otherSide = listOf(a, b, c)[otherSideIdx]!!
            // Law of sines: sin(otherAngle) = otherSide * sin(ang) / sideOpp
            val ratio = otherSide * sin(rad(ang)) / sideOpp
            if (ratio > 1.0 + 1e-9) return TriResult(emptyList(), "SSA: řešení neexistuje (sin > 1).")
            val clamp = ratio.coerceIn(-1.0, 1.0)
            val solutions = mutableListOf<Triangle>()
            val cand1 = deg(asin(clamp))
            val cand2 = 180.0 - cand1
            for (otherAng in listOf(cand1, cand2).distinct()) {
                val third = 180.0 - ang - otherAng
                if (third <= 0 || otherAng <= 0) continue
                // Assign angles to positions
                val angs = DoubleArray(3)
                angs[angleGiven] = ang
                angs[otherSideIdx] = otherAng
                angs[missingSideIdx] = third
                val sidesArr = DoubleArray(3)
                sidesArr[angleGiven] = sideOpp
                sidesArr[otherSideIdx] = otherSide
                sidesArr[missingSideIdx] = sideOpp * sin(rad(third)) / sin(rad(ang))
                solutions.add(
                    Triangle(
                        sidesArr[0], sidesArr[1], sidesArr[2],
                        angs[0], angs[1], angs[2],
                    ),
                )
            }
            if (solutions.isEmpty()) return TriResult(emptyList(), "SSA: žádné platné řešení.")
            val msg = if (solutions.size == 2)
                "Způsob řešení: SSA — dvojznačný případ, dvě řešení."
            else "Způsob řešení: SSA."
            return TriResult(solutions, msg)
        }
    }

    return TriResult(emptyList(), "Zadání nepodporováno.")
}

@Composable
private fun TriangleCanvas(t: Triangle, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        // Place triangle with side c on bottom: A at (0,0), B at (c,0).
        val Ax = 0f; val Ay = 0f
        val Bx = t.c.toFloat(); val By = 0f
        // C is at angle alpha from AB, distance b from A.
        val Cx = (t.b * cos(rad(t.alpha))).toFloat()
        val Cy = (t.b * sin(rad(t.alpha))).toFloat()

        val minX = minOf(Ax, Bx, Cx); val maxX = maxOf(Ax, Bx, Cx)
        val minY = minOf(Ay, By, Cy); val maxY = maxOf(Ay, By, Cy)
        val w = size.width; val h = size.height
        val pad = 24f
        val sx = (w - 2 * pad) / max(1e-3f, (maxX - minX))
        val sy = (h - 2 * pad) / max(1e-3f, (maxY - minY))
        val s = kotlin.math.min(sx, sy)
        fun tx(x: Float) = pad + (x - minX) * s
        // Flip Y so triangle sits on bottom.
        fun ty(y: Float) = h - pad - (y - minY) * s

        val path = Path().apply {
            moveTo(tx(Ax), ty(Ay))
            lineTo(tx(Bx), ty(By))
            lineTo(tx(Cx), ty(Cy))
            close()
        }
        drawPath(
            path = path,
            color = color.copy(alpha = 0.18f),
        )
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
        )
        // Vertex dots
        listOf(Ax to Ay, Bx to By, Cx to Cy).forEach { (x, y) ->
            drawCircle(color = color, radius = 5f, center = Offset(tx(x), ty(y)))
        }
    }
}

