package com.zeddihub.mobile.ui.helpers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Interactive Rubik's cube simulator + solver.
 *
 * The screen offers three workflows:
 *
 *   1. **Manual entry** — tap any sticker to cycle through the 6 face
 *      colours, building the state of the cube the user holds in hand.
 *   2. **Scramble** — randomises the cube in-app and remembers the move
 *      sequence so we can always replay its inverse for a guaranteed
 *      solve animation.
 *   3. **Move-by-move tinkering** — the 18 standard turns are exposed
 *      as buttons; useful for learning notation or trying algorithms.
 *
 * "Solve" tries IDA* up to depth 12 (covers any state ≤12 moves from
 * solved, including most user-entered ones) and falls back to the
 * stored scramble inverse if IDA* gives up. If neither works the user
 * is told the state is too far from solved for the v0.8.0 algorithm —
 * a Kociemba two-phase solver is on the roadmap for v1.0.0.
 *
 * The "3D animation" is a 2D unfolded net (cross / T layout) where
 * stickers update one move at a time during playback. A real 3D
 * renderer would need a custom GL pipeline; the net view ends up being
 * just as readable when you're solving along on a physical cube.
 */
@Composable
fun RubikSolverScreen(padding: PaddingValues) {
    var cube by remember { mutableStateOf(RubikCube.solved()) }
    var scrambleSeq by remember { mutableStateOf<List<String>>(emptyList()) }
    var solution by remember { mutableStateOf<List<String>>(emptyList()) }
    var solutionIdx by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf<String?>(null) }
    var solving by remember { mutableStateOf(false) }
    var paintColor by remember { mutableStateOf(0) } // U=White by default
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.rubik_help_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(R.string.rubik_help_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── Cube net ──────────────────────────────────────────────────
        CubeNet(
            cube = cube,
            onStickerTap = { faceIdx, cell ->
                if (cell == 4) return@CubeNet // centre is fixed
                val s = cube.s.copyOf()
                s[faceIdx * 9 + cell] = paintColor
                cube = rebuild(s)
            }
        )

        // ── Colour picker for manual entry ────────────────────────────
        Text(
            stringResource(R.string.rubik_color_picker),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until 6) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(STICKER_COLOR[i])
                        .border(
                            width = if (paintColor == i) 3.dp else 1.dp,
                            color = if (paintColor == i) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { paintColor = i }
                )
            }
        }

        // ── Action buttons ────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !solving,
                onClick = {
                    val seq = RubikSolver.scramble(25)
                    scrambleSeq = seq
                    cube = RubikCube.solved().applyAll(seq)
                    solution = emptyList()
                    solutionIdx = 0
                    status = null
                }
            ) { Text(stringResource(R.string.rubik_scramble)) }

            OutlinedButton(
                enabled = !solving,
                onClick = {
                    cube = RubikCube.solved()
                    scrambleSeq = emptyList()
                    solution = emptyList()
                    solutionIdx = 0
                    status = null
                }
            ) { Text(stringResource(R.string.rubik_reset)) }

            Button(
                enabled = !solving,
                onClick = {
                    solving = true
                    status = null
                    scope.launch {
                        // IDA* is CPU-bound; off-main thread is mandatory.
                        val found = withContext(Dispatchers.Default) {
                            RubikSolver.solve(cube)
                        }
                        val seq = found ?: RubikCube.invertSequence(scrambleSeq).takeIf { it.isNotEmpty() }
                        if (seq != null) {
                            solution = seq
                            solutionIdx = 0
                            status = null
                        } else {
                            status = "TOO_DEEP"
                        }
                        solving = false
                    }
                }
            ) {
                Text(
                    if (solving) stringResource(R.string.rubik_solving)
                    else stringResource(R.string.rubik_solve)
                )
            }
        }

        if (cube.isSolved()) {
            Text(
                stringResource(R.string.rubik_status_solved),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (status == "TOO_DEEP") {
            Text(
                stringResource(R.string.rubik_too_deep),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // ── Solution playback ─────────────────────────────────────────
        if (solution.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.rubik_solution, solution.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(solution.size) { i ->
                    val played = i < solutionIdx
                    AssistChip(
                        onClick = {},
                        label = { Text(solution[i]) },
                        enabled = !played
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = solutionIdx < solution.size,
                    onClick = {
                        if (solutionIdx < solution.size) {
                            cube = cube.apply(solution[solutionIdx])
                            solutionIdx++
                        }
                    }
                ) { Text(stringResource(R.string.rubik_step)) }
                Button(
                    enabled = solutionIdx < solution.size,
                    onClick = {
                        scope.launch {
                            // Animate one move at a time so the user can
                            // follow on a physical cube. 600 ms feels
                            // about right — fast enough not to be tedious,
                            // slow enough to follow.
                            while (solutionIdx < solution.size) {
                                cube = cube.apply(solution[solutionIdx])
                                solutionIdx++
                                delay(600)
                            }
                        }
                    }
                ) { Text(stringResource(R.string.rubik_play_all)) }
            }
        }

        // ── Manual move buttons ───────────────────────────────────────
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.rubik_manual_moves),
            style = MaterialTheme.typography.labelLarge,
        )
        // Group as 6 face rows (M, M', M2)
        val groups = listOf("U", "D", "L", "R", "F", "B")
        for (face in groups) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(face, "$face'", "${face}2").forEach { m ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            cube = cube.apply(m)
                            // Manual moves invalidate the scramble inverse.
                            scrambleSeq = emptyList()
                            solution = emptyList()
                            solutionIdx = 0
                        },
                        label = { Text(m) }
                    )
                }
            }
        }
    }

}

// --- Cube net rendering -----------------------------------------------

/**
 * Draws the cube as an unfolded "T" net:
 *
 *           [U]
 *      [L] [F] [R] [B]
 *           [D]
 *
 * Each face is a 3×3 grid of 22-dp coloured squares with a 1-dp dark
 * border. The whole net comfortably fits the screen width on phones
 * (~12 squares horizontally including spacing).
 */
@Composable
private fun CubeNet(cube: RubikCube, onStickerTap: (faceIdx: Int, cell: Int) -> Unit) {
    val cell = 26.dp
    val gap = 2.dp
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top row: blank | U | blank | blank
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Spacer(Modifier.size(cell * 3 + gap * 2))
                Face(cube, RubikCube.U, cell, gap, onStickerTap)
                Spacer(Modifier.size(cell * 3 + gap * 2))
                Spacer(Modifier.size(cell * 3 + gap * 2))
            }
            // Middle row: L F R B
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Face(cube, RubikCube.L, cell, gap, onStickerTap)
                Face(cube, RubikCube.F, cell, gap, onStickerTap)
                Face(cube, RubikCube.R, cell, gap, onStickerTap)
                Face(cube, RubikCube.B, cell, gap, onStickerTap)
            }
            // Bottom row: blank | D | blank | blank
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Spacer(Modifier.size(cell * 3 + gap * 2))
                Face(cube, RubikCube.D, cell, gap, onStickerTap)
                Spacer(Modifier.size(cell * 3 + gap * 2))
                Spacer(Modifier.size(cell * 3 + gap * 2))
            }
        }
    }
}

@Composable
private fun Face(
    cube: RubikCube,
    faceIdx: Int,
    cell: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp,
    onTap: (Int, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (col in 0 until 3) {
                    val cellIdx = row * 3 + col
                    val color = STICKER_COLOR[cube.s[faceIdx * 9 + cellIdx]]
                    Box(
                        modifier = Modifier
                            .size(cell)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                            .border(1.dp, Color(0xFF111111), RoundedCornerShape(4.dp))
                            .clickable { onTap(faceIdx, cellIdx) }
                    )
                }
            }
        }
    }
}

private val STICKER_COLOR = arrayOf(
    Color(0xFFF8F8F8), // U white
    Color(0xFFD42121), // R red
    Color(0xFF1FA84A), // F green
    Color(0xFFFFD400), // D yellow
    Color(0xFFFF8800), // L orange
    Color(0xFF1F4FA8), // B blue
)

private fun rebuild(s: IntArray): RubikCube = RubikCube.fromStickers(s)
