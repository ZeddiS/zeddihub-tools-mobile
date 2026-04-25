package com.zeddihub.mobile.ui.helpers

/**
 * Rubik's cube solver using IDA* (iterative-deepening A*) with a
 * conservative misplaced-stickers heuristic.
 *
 * Why IDA* and not the textbook two-phase Kociemba? Kociemba needs
 * ~6–10 MB of pruning tables that have to be either generated at
 * runtime (slow first-launch) or shipped as a binary asset (bloats the
 * APK). IDA* with a weak heuristic is admissible and uses zero state,
 * so it ships in a single Kotlin file. The trade-off is that it's only
 * fast on states close to solved.
 *
 * To keep the in-app experience responsive we cap the search depth at
 * MAX_DEPTH (12 moves). Any cube further than that returns null and the
 * UI falls back to "play back the inverse of whatever you scrambled in
 * the app" — which always works for in-app scrambles, and a generic
 * full-state solver lands with the algorithm-bench polish in v1.0.0.
 *
 * Pruning we do apply:
 *   • Skip moves on the same face as the previous move (X X X' is junk).
 *   • Skip mirror-face moves whose order doesn't matter (R then L is
 *     identical to L then R, so we only allow R-before-L). This roughly
 *     halves branching for parallel face pairs.
 */
object RubikSolver {

    /** Cap depth so the worst case is sub-second on a phone. */
    const val MAX_DEPTH = 12

    /**
     * Try to solve [start]. Returns the solving sequence, or null if
     * we couldn't find one within MAX_DEPTH moves.
     */
    fun solve(start: RubikCube, maxDepth: Int = MAX_DEPTH): List<String>? {
        if (start.isSolved()) return emptyList()

        val moves = RubikCube.ALL_MOVES
        // Face index per move (U=0, D=1, L=2, R=3, F=4, B=5).
        val face = moves.map { faceIndex(it) }.toIntArray()

        // For each (lastFace, candidateFace) decide whether to allow it:
        //   - same face: never (collapsing turns)
        //   - opposite face after we just moved its partner: only one order
        //     allowed. We pick "even index first": U(0) before D(1),
        //     L(2) before R(3), F(4) before B(5). So if last was D and
        //     we want to do U, skip — should have done U first.
        fun allowed(lastFace: Int, f: Int): Boolean {
            if (lastFace < 0) return true
            if (lastFace == f) return false
            // Opposite-face pruning: faces are paired (0,1) (2,3) (4,5)
            if (lastFace xor f == 1 && lastFace > f) return false
            return true
        }

        val path = ArrayList<String>(maxDepth)
        for (depth in 1..maxDepth) {
            if (dfs(start, depth, -1, path, moves, face, ::allowed)) {
                return path.toList()
            }
        }
        return null
    }

    private fun dfs(
        c: RubikCube,
        remaining: Int,
        lastFace: Int,
        path: ArrayList<String>,
        moves: List<String>,
        face: IntArray,
        allowed: (Int, Int) -> Boolean,
    ): Boolean {
        if (c.isSolved()) return remaining == 0
        if (remaining == 0) return false
        // Heuristic prune
        if (c.heuristic() > remaining) return false

        for (i in moves.indices) {
            if (!allowed(lastFace, face[i])) continue
            val m = moves[i]
            path.add(m)
            val next = c.apply(m)
            if (dfs(next, remaining - 1, face[i], path, moves, face, allowed)) return true
            path.removeAt(path.size - 1)
        }
        return false
    }

    private fun faceIndex(m: String): Int = when (m[0]) {
        'U' -> 0; 'D' -> 1; 'L' -> 2; 'R' -> 3; 'F' -> 4; 'B' -> 5
        else -> -1
    }

    /** Generate a fresh scramble of the given length. */
    fun scramble(length: Int = 25, seed: Long = System.nanoTime()): List<String> {
        val moves = RubikCube.ALL_MOVES
        val rng = java.util.Random(seed)
        val out = ArrayList<String>(length)
        var lastFace = -1
        var prevFace = -1
        while (out.size < length) {
            val m = moves[rng.nextInt(moves.size)]
            val f = faceIndex(m)
            if (f == lastFace) continue
            // Avoid X Y X' patterns where Y is X's opposite, since
            // they collapse on physical cubes (looks "lazy"). Cheap
            // heuristic — not strictly needed for correctness.
            if (f == prevFace && lastFace xor f == 1) continue
            out.add(m)
            prevFace = lastFace
            lastFace = f
        }
        return out
    }
}
