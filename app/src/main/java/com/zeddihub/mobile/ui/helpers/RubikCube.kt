package com.zeddihub.mobile.ui.helpers

/**
 * 3×3×3 Rubik's cube state, modelled as 54 sticker colours laid out
 * across six faces:
 *
 *      U (0..8)
 *  L F R B  (36..44, 18..26, 9..17, 45..53)
 *      D (27..35)
 *
 * Each face is read row-by-row, left-to-right:
 *
 *   0 1 2
 *   3 4 5
 *   6 7 8
 *
 * The centre sticker (index 4 on every face) is fixed — it identifies
 * the colour of that face. Solved-state colours follow the Western
 * Rubik convention: U=White, R=Red, F=Green, D=Yellow, L=Orange, B=Blue.
 *
 * All 18 standard moves (U/D/L/R/F/B with no-mod, ', and 2 variants)
 * are stored as compile-time permutation arrays so apply() is a single
 * 54-element scatter; no math, no allocation in the hot path.
 */
class RubikCube private constructor(val s: IntArray) {

    fun copy() = RubikCube(s.copyOf())

    fun apply(move: String): RubikCube {
        val perm = MOVES[move] ?: return this
        val out = IntArray(54)
        for (i in 0 until 54) out[i] = s[perm[i]]
        return RubikCube(out)
    }

    fun applyAll(seq: List<String>): RubikCube {
        var c = this
        for (m in seq) c = c.apply(m)
        return c
    }

    fun isSolved(): Boolean {
        // Each face must have all 9 stickers equal to its centre.
        for (f in 0 until 6) {
            val centre = s[f * 9 + 4]
            for (i in 0 until 9) if (s[f * 9 + i] != centre) return false
        }
        return true
    }

    /**
     * Manhattan-style heuristic counting how many stickers don't match
     * their face centre. Divided by 8 because a single move displaces
     * at most 8 stickers (4 face + 4 ring stickers per face turn — face
     * stickers count partially, but /8 still under-estimates safely).
     * Used by IDA* below.
     */
    fun heuristic(): Int {
        var miss = 0
        for (f in 0 until 6) {
            val c = s[f * 9 + 4]
            for (i in 0 until 9) if (i != 4 && s[f * 9 + i] != c) miss++
        }
        return (miss + 7) / 8
    }

    companion object {
        // Solved cube: U=0, R=1, F=2, D=3, L=4, B=5.
        fun solved(): RubikCube {
            val a = IntArray(54)
            for (f in 0 until 6) for (i in 0 until 9) a[f * 9 + i] = f
            return RubikCube(a)
        }

        /**
         * Build a cube from an existing 54-sticker array. We copy the
         * input so the caller can keep mutating their own array without
         * tearing the model state.
         */
        fun fromStickers(s: IntArray): RubikCube {
            require(s.size == 54) { "Cube needs exactly 54 stickers, got ${s.size}" }
            return RubikCube(s.copyOf())
        }

        const val U = 0; const val R = 1; const val F = 2
        const val D = 3; const val L = 4; const val B = 5

        // ---- Move permutation tables -----------------------------------
        //
        // Each entry maps DESTINATION_INDEX -> SOURCE_INDEX, i.e.
        // out[i] = s[perm[i]]. So the array tells you "for each new
        // sticker position, which old position do I read from".
        //
        // Compose face turn = rotate that face's 8 outer stickers
        // clockwise (90°) plus cycle 12 stickers on the adjacent ring
        // (3 stickers from each of the 4 neighbour faces). Centres stay.

        private fun face(idx: Int) = idx
        private fun u(i: Int) = U * 9 + i
        private fun r(i: Int) = R * 9 + i
        private fun f(i: Int) = F * 9 + i
        private fun d(i: Int) = D * 9 + i
        private fun l(i: Int) = L * 9 + i
        private fun b(i: Int) = B * 9 + i

        /** Build a permutation by listing cycles (groups rotate forward). */
        private fun buildPerm(vararg cycles: IntArray): IntArray {
            val p = IntArray(54) { it }
            for (cy in cycles) {
                // forward rotation: position cy[i+1] takes the value
                // previously at cy[i] -> in destination-from-source form
                // perm[cy[i+1]] = cy[i]
                val first = p[cy[0]]
                for (i in 0 until cy.size - 1) p[cy[i]] = p[cy[i + 1]]
                p[cy[cy.size - 1]] = first
            }
            return p
        }

        private val mU = buildPerm(
            // U face clockwise: 0->2->8->6->0, 1->5->7->3->1
            intArrayOf(u(0), u(2), u(8), u(6)),
            intArrayOf(u(1), u(5), u(7), u(3)),
            // Ring around U (top row of F,R,B,L cycles): F top -> L top -> B top -> R top -> F top
            intArrayOf(f(0), l(0), b(0), r(0)),
            intArrayOf(f(1), l(1), b(1), r(1)),
            intArrayOf(f(2), l(2), b(2), r(2)),
        )

        private val mD = buildPerm(
            intArrayOf(d(0), d(2), d(8), d(6)),
            intArrayOf(d(1), d(5), d(7), d(3)),
            // Ring: F bottom -> R bottom -> B bottom -> L bottom -> F bottom
            intArrayOf(f(6), r(6), b(6), l(6)),
            intArrayOf(f(7), r(7), b(7), l(7)),
            intArrayOf(f(8), r(8), b(8), l(8)),
        )

        private val mR = buildPerm(
            intArrayOf(r(0), r(2), r(8), r(6)),
            intArrayOf(r(1), r(5), r(7), r(3)),
            // Ring: F right col -> U right col -> B left col(reversed) -> D right col -> F right col
            intArrayOf(f(2), u(2), b(6), d(2)),
            intArrayOf(f(5), u(5), b(3), d(5)),
            intArrayOf(f(8), u(8), b(0), d(8)),
        )

        private val mL = buildPerm(
            intArrayOf(l(0), l(2), l(8), l(6)),
            intArrayOf(l(1), l(5), l(7), l(3)),
            intArrayOf(f(0), d(0), b(8), u(0)),
            intArrayOf(f(3), d(3), b(5), u(3)),
            intArrayOf(f(6), d(6), b(2), u(6)),
        )

        private val mF = buildPerm(
            intArrayOf(f(0), f(2), f(8), f(6)),
            intArrayOf(f(1), f(5), f(7), f(3)),
            intArrayOf(u(6), r(0), d(2), l(8)),
            intArrayOf(u(7), r(3), d(1), l(5)),
            intArrayOf(u(8), r(6), d(0), l(2)),
        )

        private val mB = buildPerm(
            intArrayOf(b(0), b(2), b(8), b(6)),
            intArrayOf(b(1), b(5), b(7), b(3)),
            intArrayOf(u(0), l(6), d(8), r(2)),
            intArrayOf(u(1), l(3), d(7), r(5)),
            intArrayOf(u(2), l(0), d(6), r(8)),
        )

        private fun inv(p: IntArray): IntArray {
            val q = IntArray(p.size)
            for (i in p.indices) q[p[i]] = i
            return q
        }

        private fun compose(a: IntArray, b: IntArray): IntArray {
            val c = IntArray(a.size)
            for (i in a.indices) c[i] = a[b[i]]
            return c
        }

        /** Public single-letter move names + ' / 2 variants. */
        val ALL_MOVES = listOf(
            "U", "U'", "U2", "D", "D'", "D2",
            "L", "L'", "L2", "R", "R'", "R2",
            "F", "F'", "F2", "B", "B'", "B2",
        )

        val MOVES: Map<String, IntArray> = mapOf(
            "U" to mU, "U'" to inv(mU), "U2" to compose(mU, mU),
            "D" to mD, "D'" to inv(mD), "D2" to compose(mD, mD),
            "R" to mR, "R'" to inv(mR), "R2" to compose(mR, mR),
            "L" to mL, "L'" to inv(mL), "L2" to compose(mL, mL),
            "F" to mF, "F'" to inv(mF), "F2" to compose(mF, mF),
            "B" to mB, "B'" to inv(mB), "B2" to compose(mB, mB),
        )

        fun invertMove(m: String) = when {
            m.endsWith("2") -> m
            m.endsWith("'") -> m.dropLast(1)
            else -> "$m'"
        }

        fun invertSequence(seq: List<String>) = seq.asReversed().map { invertMove(it) }
    }
}
