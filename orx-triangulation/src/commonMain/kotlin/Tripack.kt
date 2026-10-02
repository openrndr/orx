package org.openrndr.extra.triangulation

import kotlin.math.abs

/**
 * A fidelity-first Kotlin port of the core of Robert Renka's **TRIPACK**
 * (ACM TOMS Algorithm 751, "A Constrained Two-Dimensional Delaunay Triangulation
 * Package"), see https://people.math.sc.edu/Burkardt/f_src/tripack/tripack.f90.
 *
 * Only the subset needed to (1) build an unconstrained Delaunay triangulation of a
 * fixed set of points and (2) force a set of edges into it by diagonal swapping is
 * ported. Node/arc deletion, nearest-neighbor queries, and TRIPACK's own
 * constraint-region bookkeeping (NCC/LCC, `ADDCST`, `CRTRI`, `INDXCC`, `TRLIST`'s
 * region classification) are not needed: [ConstrainedDelaunayTriangulation] instead
 * forces every contour edge directly via [forceEdge] and determines which of the
 * resulting triangles to keep using `Shape.contains`, OPENRNDR's own (already robust)
 * point-in-shape test.
 *
 * The triangulation is stored exactly as in TRIPACK: counterclockwise adjacency lists
 * [list]/[lptr] (1-based; index 0 is unused) with [lend] pointing to each node's last
 * neighbor (negated if the node is on the convex hull boundary), and [lnew] the next
 * free slot. This mirrors the Fortran source directly -- including its GOTO-driven
 * control flow, translated here as explicit label/state dispatch -- rather than
 * re-modelling it as idiomatic half-edges, so it can be diffed against the original
 * algorithm when something misbehaves.
 */
internal class Tripack(totalNodes: Int) {

    private val cap = 6 * totalNodes + 10
    private val x = DoubleArray(totalNodes + 1)
    private val y = DoubleArray(totalNodes + 1)
    private val list = IntArray(cap)
    private val lptr = IntArray(cap)
    private val lend = IntArray(totalNodes + 1)
    private var lnew = 1

    /** Number of nodes currently in the triangulation. */
    private var n = 0

    /** State for JRAND, seeded exactly as TRIPACK's SAVEd IX, IY, IZ. */
    private var ix = 1
    private var iy = 2
    private var iz = 3

    /** Sets the (fixed) coordinates of node [k] (1-based). Must be called before [build]. */
    fun setPoint(k: Int, px: Double, py: Double) {
        x[k] = px
        y[k] = py
    }

    // -------------------------------------------------------------------------------
    // Small geometric / list primitives (LEFT, LSTPTR, JRAND)
    // -------------------------------------------------------------------------------

    private fun left(x1: Double, y1: Double, x2: Double, y2: Double, x0: Double, y0: Double): Boolean {
        val dx1 = x2 - x1
        val dy1 = y2 - y1
        val dx2 = x0 - x1
        val dy2 = y0 - y1
        return dx1 * dy2 >= dx2 * dy1
    }

    private fun frwrd(xa: Double, ya: Double, xb: Double, yb: Double, xc: Double, yc: Double): Boolean =
        (xb - xa) * (xc - xa) + (yb - ya) * (yc - ya) >= 0.0

    private fun lstptr(lpl: Int, nb: Int): Int {
        var lp = lptr[lpl]
        while (true) {
            val nd = list[lp]
            if (nd == nb) break
            lp = lptr[lp]
            if (lp == lpl) break
        }
        return lp
    }

    private fun jrand(nn: Int): Int {
        ix = (171 * ix) % 30269
        iy = (172 * iy) % 30307
        iz = (170 * iz) % 30323
        val xf = ix / 30269.0 + iy / 30307.0 + iz / 30323.0
        val u = xf - xf.toInt()
        return (nn * u).toInt() + 1
    }

    // -------------------------------------------------------------------------------
    // Raw list-structure node insertion (INSERT, INTADD, BDYADD)
    // -------------------------------------------------------------------------------

    private fun insert(k: Int, lp: Int) {
        val lsav = lptr[lp]
        lptr[lp] = lnew
        list[lnew] = k
        lptr[lnew] = lsav
        lnew++
    }

    private fun intadd(kk: Int, i1: Int, i2: Int, i3: Int) {
        var lp = lstptr(lend[i1], i2)
        insert(kk, lp)
        lp = lstptr(lend[i2], i3)
        insert(kk, lp)
        lp = lstptr(lend[i3], i1)
        insert(kk, lp)
        list[lnew] = i1
        list[lnew + 1] = i2
        list[lnew + 2] = i3
        lptr[lnew] = lnew + 1
        lptr[lnew + 1] = lnew + 2
        lptr[lnew + 2] = lnew
        lend[kk] = lnew + 2
        lnew += 3
    }

    private fun bdyadd(kk: Int, i1: Int, i2: Int) {
        val n1 = i1
        val n2 = i2
        var lp = lend[n1]
        var lsav = lptr[lp]
        lptr[lp] = lnew
        list[lnew] = -kk
        lptr[lnew] = lsav
        lend[n1] = lnew
        lnew++
        var next = -list[lp]
        list[lp] = next
        val nsav = next

        while (true) {
            lp = lend[next]
            insert(kk, lp)
            if (next == n2) break
            next = -list[lp]
            list[lp] = next
        }

        lsav = lnew
        list[lnew] = n1
        lptr[lnew] = lnew + 1
        lnew++
        next = nsav

        while (true) {
            if (next == n2) break
            list[lnew] = next
            lptr[lnew] = lnew + 1
            lnew++
            lp = lend[next]
            next = list[lp]
        }

        list[lnew] = -n2
        lptr[lnew] = lsav
        lend[kk] = lnew
        lnew++
    }

    // -------------------------------------------------------------------------------
    // Diagonal flip and circumcircle test (SWAP, SWPTST)
    // -------------------------------------------------------------------------------

    /** Returns the LP21 pointer, or 0 if IN1 and IN2 were already adjacent (no-op). */
    private fun swap(in1: Int, in2: Int, io1: Int, io2: Int): Int {
        var lp = lstptr(lend[in1], in2)
        if (abs(list[lp]) == in2) {
            return 0
        }

        lp = lstptr(lend[io1], in2)
        var lph = lptr[lp]
        lptr[lp] = lptr[lph]
        if (lend[io1] == lph) lend[io1] = lp

        lp = lstptr(lend[in1], io1)
        var lpsav = lptr[lp]
        lptr[lp] = lph
        list[lph] = in2
        lptr[lph] = lpsav

        lp = lstptr(lend[io2], in1)
        lph = lptr[lp]
        lptr[lp] = lptr[lph]
        if (lend[io2] == lph) lend[io2] = lp

        lp = lstptr(lend[in2], io2)
        lpsav = lptr[lp]
        lptr[lp] = lph
        list[lph] = in1
        lptr[lph] = lpsav

        return lph
    }

    private fun swptst(in1: Int, in2: Int, io1: Int, io2: Int): Boolean {
        val dx11 = x[io1] - x[in1]
        val dx12 = x[io2] - x[in1]
        val dx22 = x[io2] - x[in2]
        val dx21 = x[io1] - x[in2]
        val dy11 = y[io1] - y[in1]
        val dy12 = y[io2] - y[in1]
        val dy22 = y[io2] - y[in2]
        val dy21 = y[io1] - y[in2]

        val cos1 = dx11 * dx12 + dy11 * dy12
        val cos2 = dx22 * dx21 + dy22 * dy21

        if (cos1 >= 0.0 && cos2 >= 0.0) return false
        if (cos1 < 0.0 && cos2 < 0.0) return true

        val sin1 = dx11 * dy12 - dx12 * dy11
        val sin2 = dx22 * dy21 - dx21 * dy22
        val sin12 = sin1 * cos2 + cos1 * sin2

        return sin12 < -SWTOL
    }

    // -------------------------------------------------------------------------------
    // OPTIM: repeated local-optimization sweep over a worklist of arcs
    // -------------------------------------------------------------------------------

    /** Returns an error code: 0 ok, 1 hit maxIt, 2 bad input, 3/4 invalid structure. */
    private fun optim(na: Int, col1: IntArray, col2: IntArray, maxIt: Int): Int {
        if (na < 0 || maxIt < 1) return 2
        if (na == 0) return 0

        var iter = 0
        while (true) {
            if (iter == maxIt) return 1
            iter++
            var swp = false

            for (i in 0 until na) {
                val io1 = col1[i]
                val io2 = col2[i]
                val lpl = lend[io1]
                var lpp = lpl
                var lp = lptr[lpp]
                var matched = false
                while (true) {
                    if (list[lp] == io2) {
                        matched = true
                        break
                    }
                    lpp = lp
                    lp = lptr[lpp]
                    if (lp == lpl) break
                }
                if (!matched) {
                    if (abs(list[lp]) != io2) return 3
                    if (list[lp] < 0) continue
                }
                val n2 = list[lpp]
                if (n2 < 0) continue
                lp = lptr[lp]
                val n1 = abs(list[lp])
                if (!swptst(n1, n2, io1, io2)) continue
                val lp21 = swap(n1, n2, io1, io2)
                if (lp21 == 0) return 4
                swp = true
                col1[i] = n1
                col2[i] = n2
            }

            if (!swp) return 0
        }
    }

    // -------------------------------------------------------------------------------
    // TRFIND: point location by edge-hopping, with randomized restart on cycling
    // -------------------------------------------------------------------------------

    /**
     * Locates [px],[py] relative to the triangulation, starting the search at node
     * [nst]. Returns `[i1, i2, i3]`: a counterclockwise-ordered containing triangle, or
     * `[i1, i2, 0]` (rightmost/leftmost visible boundary nodes) if the point is outside
     * the convex hull, or `[0, 0, 0]` if all nodes (and the point) are collinear.
     */
    private fun trfind(nst: Int, px: Double, py: Double): IntArray {
        var n0 = nst
        if (n0 < 1 || n < n0) n0 = jrand(n)

        var n1 = 0
        var n2 = 0
        var n3 = 0
        var n4 = 0
        var n1s = 0
        var n2s = 0
        var nf = 0
        var nl = 0
        var nb = 0
        var np = 0
        var npp = 0
        var i1 = 0
        var lp = 0

        var state = 1
        while (true) {
            when (state) {
                1 -> {
                    lp = lend[n0]
                    nl = list[lp]
                    lp = lptr[lp]
                    nf = list[lp]
                    n1 = nf
                    if (nl > 0) {
                        state = 2; continue
                    }
                    nl = -nl
                    if (!left(x[n0], y[n0], x[nf], y[nf], px, py)) {
                        nl = n0
                        state = 9; continue
                    }
                    if (!left(x[nl], y[nl], x[n0], y[n0], px, py)) {
                        nb = nf; nf = n0; np = nl; npp = n0
                        state = 11; continue
                    }
                    state = 3
                }
                2 -> {
                    if (left(x[n0], y[n0], x[n1], y[n1], px, py)) {
                        state = 3; continue
                    }
                    lp = lptr[lp]
                    n1 = list[lp]
                    if (n1 == nl) {
                        state = 6; continue
                    }
                    // repeat state 2
                }
                3 -> {
                    lp = lptr[lp]
                    n2 = abs(list[lp])
                    if (!left(x[n0], y[n0], x[n2], y[n2], px, py)) {
                        state = 7; continue
                    }
                    n1 = n2
                    if (n1 != nl) continue // repeat state 3
                    if (!left(x[n0], y[n0], x[nf], y[nf], px, py)) {
                        state = 6; continue
                    }
                    if (px == x[n0] && py == y[n0]) {
                        state = 5; continue
                    }
                    state = 4
                }
                4 -> {
                    if (!left(x[n1], y[n1], x[n0], y[n0], px, py)) {
                        state = 5; continue
                    }
                    lp = lptr[lp]
                    n1 = abs(list[lp])
                    if (n1 == nl) return intArrayOf(0, 0, 0)
                    // repeat state 4
                }
                5 -> {
                    n0 = n1
                    state = 1
                }
                6 -> {
                    n2 = nf
                    state = 7
                }
                7 -> {
                    n3 = n0
                    n1s = n1
                    n2s = n2
                    state = 8
                }
                8 -> {
                    if (left(x[n1], y[n1], x[n2], y[n2], px, py)) {
                        val b1 = (x[n3] - x[n2]) * (py - y[n2]) - (px - x[n2]) * (y[n3] - y[n2])
                        val b2 = (x[n1] - x[n3]) * (py - y[n3]) - (px - x[n3]) * (y[n1] - y[n3])
                        if (b1 + 1.0 >= 1.0 && b2 + 1.0 >= 1.0) {
                            state = 16; continue
                        }
                        n0 = jrand(n)
                        state = 1; continue
                    }
                    lp = lstptr(lend[n2], n1)
                    if (list[lp] < 0) {
                        nf = n2; nl = n1
                        state = 9; continue
                    }
                    lp = lptr[lp]
                    n4 = abs(list[lp])
                    if (left(x[n0], y[n0], x[n4], y[n4], px, py)) {
                        n3 = n1; n1 = n4; n2s = n2
                        if (n1 != n1s && n1 != n0) {
                            state = 8; continue
                        }
                    } else {
                        n3 = n2; n2 = n4; n1s = n1
                        if (n2 != n2s && n2 != n0) {
                            state = 8; continue
                        }
                    }
                    n0 = jrand(n)
                    state = 1
                }
                9 -> {
                    np = nl; npp = nf
                    state = 10
                }
                10 -> {
                    lp = lend[nf]
                    lp = lptr[lp]
                    nb = list[lp]
                    if (!left(x[nf], y[nf], x[nb], y[nb], px, py)) {
                        state = 12; continue
                    }
                    state = 11
                }
                11 -> {
                    if (frwrd(x[nf], y[nf], x[np], y[np], px, py) ||
                        frwrd(x[nf], y[nf], x[np], y[np], x[nb], y[nb])
                    ) {
                        i1 = nf
                        state = 13; continue
                    }
                    state = 12
                }
                12 -> {
                    np = nf; nf = nb
                    state = 10
                }
                13 -> {
                    lp = lend[nl]
                    nb = -list[lp]
                    if (!left(x[nb], y[nb], x[nl], y[nl], px, py)) {
                        state = 14; continue
                    }
                    if (frwrd(x[nl], y[nl], x[npp], y[npp], px, py) ||
                        frwrd(x[nl], y[nl], x[npp], y[npp], x[nb], y[nb])
                    ) {
                        state = 15; continue
                    }
                    state = 14
                }
                14 -> {
                    npp = nl; nl = nb
                    state = 13
                }
                15 -> return intArrayOf(i1, nl, 0)
                16 -> return intArrayOf(n1, n2, n3)
            }
        }
    }

    // -------------------------------------------------------------------------------
    // ADDNOD + TRMESH: incremental unconstrained Delaunay construction
    // -------------------------------------------------------------------------------

    /**
     * Appends node `n+1` (coordinates already set via [setPoint]) to the triangulation,
     * starting the [trfind] search at node [ist]. This is a simplified (append-only,
     * `NCC = 0`) port of TRIPACK's `ADDNOD` -- see the class doc for why the
     * constraint-region bookkeeping (`CRTRI`/`INDXCC`) isn't needed here.
     */
    private fun addNode(ist: Int) {
        val kk = n + 1
        val (i1, i2, i3) = trfind(ist, x[kk], y[kk]).let { Triple(it[0], it[1], it[2]) }

        if (i1 == 0) {
            error("Tripack: all nodes are collinear, cannot triangulate")
        }
        if (i3 != 0) {
            if (x[kk] == x[i1] && y[kk] == y[i1]) duplicatePoint(i1)
            if (x[kk] == x[i2] && y[kk] == y[i2]) duplicatePoint(i2)
            if (x[kk] == x[i3] && y[kk] == y[i3]) duplicatePoint(i3)
        }

        n = kk
        if (i3 == 0) bdyadd(kk, i1, i2) else intadd(kk, i1, i2, i3)

        var lp = lend[kk]
        val lpf = lptr[lp]
        var io2 = list[lpf]
        var lpo1 = lptr[lpf]
        var io1 = abs(list[lpo1])

        while (true) {
            lp = lstptr(lend[io1], io2)
            if (list[lp] >= 0) {
                lp = lptr[lp]
                val in1 = abs(list[lp])
                if (swptst(in1, kk, io1, io2)) {
                    val lp21 = swap(in1, kk, io1, io2)
                    if (lp21 == 0) error("Tripack: invalid triangulation geometry during node insertion")
                    lpo1 = lp21
                    io1 = in1
                    continue
                }
            }
            if (lpo1 == lpf || list[lpo1] < 0) break
            io2 = io1
            lpo1 = lptr[lpo1]
            io1 = abs(list[lpo1])
        }
    }

    private fun duplicatePoint(existing: Int): Nothing =
        error("Tripack: duplicate point at index ${existing - 1} (coinciding points are not allowed)")

    /**
     * Builds the initial unconstrained Delaunay triangulation of all `totalNodes`
     * points previously set via [setPoint]. A faithful but simplified port of
     * `TRMESH`: it omits TRIPACK's NEAR/NEXT/DIST nearest-node bookkeeping (an
     * optimization for very large point sets) and instead starts each [trfind] search
     * from the previously inserted node, which is adequate for the modest point counts
     * `ConstrainedDelaunayTriangulation` deals with.
     */
    fun build(totalNodes: Int) {
        require(totalNodes >= 3) { "Tripack requires at least 3 nodes" }

        val firstIsCcw = left(x[1], y[1], x[2], y[2], x[3], y[3])
        val secondIsCcw = left(x[2], y[2], x[1], y[1], x[3], y[3])

        if (!firstIsCcw) {
            // Initial triangle is (3,2,1) = (2,1,3) = (1,3,2).
            list[1] = 3; lptr[1] = 2; list[2] = -2; lptr[2] = 1; lend[1] = 2
            list[3] = 1; lptr[3] = 4; list[4] = -3; lptr[4] = 3; lend[2] = 4
            list[5] = 2; lptr[5] = 6; list[6] = -1; lptr[6] = 5; lend[3] = 6
        } else if (!secondIsCcw) {
            // Initial triangle is (1,2,3).
            list[1] = 2; lptr[1] = 2; list[2] = -3; lptr[2] = 1; lend[1] = 2
            list[3] = 3; lptr[3] = 4; list[4] = -1; lptr[4] = 3; lend[2] = 4
            list[5] = 1; lptr[5] = 6; list[6] = -2; lptr[6] = 5; lend[3] = 6
        } else {
            error("Tripack: the first three nodes are collinear")
        }

        lnew = 7
        n = 3
        if (totalNodes == 3) return

        for (k in 4..totalNodes) {
            addNode(k - 1)
        }
    }

    // -------------------------------------------------------------------------------
    // EDGE: forces an arc between two nodes via ordered diagonal swaps
    // -------------------------------------------------------------------------------

    /**
     * Forces nodes [in1] and [in2] to become adjacent, swapping out any arcs that cross
     * the segment between them (and re-legalizing everything else via [optim]). A
     * direct port of TRIPACK's `EDGE`, minus the `IWK`/`LWK` workspace-size reporting
     * (not needed here -- we always have room) and called per contour edge directly
     * instead of through `ADDCST`'s `NCC`/`LCC`-aware wrapper.
     */
    fun forceEdge(in1: Int, in2: Int) {
        require(in1 >= 1 && in2 >= 1 && in1 != in2) { "Tripack.forceEdge: invalid node indices $in1, $in2" }

        run {
            val lpl = lend[in1]
            var n0 = abs(list[lpl])
            var lp = lpl
            while (true) {
                if (n0 == in2) return // already adjacent
                lp = lptr[lp]
                n0 = list[lp]
                if (lp == lpl) break
            }
        }

        val iwkCap = n + 2
        val iwk1 = IntArray(iwkCap)
        val iwk2 = IntArray(iwkCap)
        var iwl = 0
        var iwend = 0
        var nit = 0

        var n1 = in1
        var n2 = in2
        var x1 = 0.0; var y1 = 0.0; var x2 = 0.0; var y2 = 0.0
        var nl = 0; var nr = 0; var n0 = 0; var x0 = 0.0; var y0 = 0.0
        var n1lst = 0; var n1frst = 0; var lp = 0; var lpl = 0; var next = 0
        var iwc = 0; var iwf = 0; var lft = 0

        var state = 2
        while (true) {
            when (state) {
                2 -> {
                    x1 = x[n1]; y1 = y[n1]
                    x2 = x[n2]; y2 = y[n2]
                    lpl = lend[n1]
                    n1lst = list[lpl]
                    lp = lptr[lpl]
                    n1frst = list[lp]
                    nl = n1frst
                    state = if (n1lst < 0) 4 else 3
                }
                3 -> {
                    if (left(x2, y2, x1, y1, x[nl], y[nl])) {
                        state = 4; continue
                    }
                    lp = lptr[lp]
                    nl = list[lp]
                    if (nl != n1frst) continue // repeat state 3
                    state = 5
                }
                4 -> {
                    nr = nl
                    lp = lptr[lp]
                    nl = abs(list[lp])
                    if (left(x1, y1, x2, y2, x[nl], y[nl])) {
                        val dx = x2 - x1
                        val dy = y2 - y1
                        val cond = (dx * (x[nl] - x1) + dy * (y[nl] - y1) >= 0.0 ||
                                dx * (x[nr] - x1) + dy * (y[nr] - y1) >= 0.0) &&
                                (dx * (x[nl] - x2) + dy * (y[nl] - y2) <= 0.0 ||
                                        dx * (x[nr] - x2) + dy * (y[nr] - y2) <= 0.0)
                        if (cond) {
                            state = 6; continue
                        }
                        if (!left(x2, y2, x1, y1, x[nl], y[nl])) {
                            state = 5; continue
                        }
                    }
                    if (nl != n1frst) continue // repeat state 4
                    state = 5
                }
                5 -> {
                    if (nit > 0) error(edgeInvalidMessage(in1, in2))
                    nit = 1
                    val tmp = n1; n1 = n2; n2 = tmp
                    state = 2
                }
                6 -> {
                    iwl++
                    iwk1[iwl] = nl; iwk2[iwl] = nr
                    lpl = lend[nl]
                    lp = lptr[lpl]
                    state = 7
                }
                7 -> {
                    if (list[lp] == nr) {
                        state = 8; continue
                    }
                    lp = lptr[lp]
                    if (lp != lpl) continue // repeat state 7
                    if (list[lp] != nr) error(edgeInvalidMessage(in1, in2))
                    state = 8
                }
                8 -> {
                    lp = lptr[lp]
                    next = abs(list[lp])
                    if (next == n2) {
                        state = 9; continue
                    }
                    if (left(x1, y1, x2, y2, x[next], y[next])) nl = next else nr = next
                    state = 6
                }
                9 -> {
                    iwend = iwl
                    iwf = 1
                    state = 10
                }
                10 -> {
                    lft = 0
                    n0 = n1
                    x0 = x1; y0 = y1
                    nl = iwk1[iwf]; nr = iwk2[iwf]
                    iwc = iwf
                    state = 11
                }
                11 -> {
                    if (iwc == iwl) {
                        state = 21; continue
                    }
                    val iwcp1 = iwc + 1
                    next = iwk1[iwcp1]
                    if (next != nl) {
                        state = 16; continue
                    }
                    next = iwk2[iwcp1]
                    if (!left(x0, y0, x[nr], y[nr], x[next], y[next])) {
                        state = 14; continue
                    }
                    if (lft >= 0) {
                        state = 12; continue
                    }
                    if (!left(x[nl], y[nl], x0, y0, x[next], y[next])) {
                        state = 14; continue
                    }
                    swap(next, n0, nl, nr)
                    iwk1[iwc] = n0; iwk2[iwc] = next
                    state = 15
                }
                12 -> {
                    swap(next, n0, nl, nr)
                    for (i in (iwc + 1)..iwl) {
                        iwk1[i - 1] = iwk1[i]; iwk2[i - 1] = iwk2[i]
                    }
                    iwk1[iwl] = n0; iwk2[iwl] = next
                    iwl--
                    nr = next
                    state = 11
                }
                14 -> {
                    n0 = nr; x0 = x[n0]; y0 = y[n0]; lft = 1
                    state = 15
                }
                15 -> {
                    nr = next
                    iwc++
                    state = 11
                }
                16 -> {
                    if (!left(x[nl], y[nl], x0, y0, x[next], y[next])) {
                        state = 19; continue
                    }
                    if (lft <= 0) {
                        state = 17; continue
                    }
                    if (!left(x0, y0, x[nr], y[nr], x[next], y[next])) {
                        state = 19; continue
                    }
                    swap(next, n0, nl, nr)
                    iwk1[iwc] = next; iwk2[iwc] = n0
                    state = 20
                }
                17 -> {
                    swap(next, n0, nl, nr)
                    for (i in (iwc - 1) downTo iwf) {
                        iwk1[i + 1] = iwk1[i]; iwk2[i + 1] = iwk2[i]
                    }
                    iwk1[iwf] = n0; iwk2[iwf] = next
                    iwf++
                    state = 20
                }
                19 -> {
                    n0 = nl; x0 = x[n0]; y0 = y[n0]; lft = -1
                    state = 20
                }
                20 -> {
                    nl = next
                    iwc++
                    state = 11
                }
                21 -> {
                    if (n0 == n1) {
                        state = 24; continue
                    }
                    if (lft < 0) {
                        state = 22; continue
                    }
                    if (!left(x0, y0, x[nr], y[nr], x2, y2)) {
                        state = 10; continue
                    }
                    swap(n2, n0, nl, nr)
                    iwk1[iwl] = n0; iwk2[iwl] = n2
                    iwl--
                    state = 10
                }
                22 -> {
                    if (!left(x[nl], y[nl], x0, y0, x2, y2)) {
                        state = 10; continue
                    }
                    swap(n2, n0, nl, nr)
                    var i = iwl
                    while (i > iwf) {
                        iwk1[i] = iwk1[i - 1]; iwk2[i] = iwk2[i - 1]
                        i--
                    }
                    iwk1[iwf] = n0; iwk2[iwf] = n2
                    iwf++
                    state = 10
                }
                24 -> {
                    swap(n2, n1, nl, nr)
                    iwk1[iwc] = 0; iwk2[iwc] = 0

                    if (iwc > 1) {
                        val cnt = iwc - 1
                        val col1 = IntArray(cnt) { iwk1[it + 1] }
                        val col2 = IntArray(cnt) { iwk2[it + 1] }
                        val ierr = optim(cnt, col1, col2, 3 * cnt)
                        if (ierr != 0) error("Tripack.forceEdge: optim failed (ier=$ierr) while forcing $in1-$in2")
                    }
                    if (iwc < iwend) {
                        val cnt = iwend - iwc
                        val col1 = IntArray(cnt) { iwk1[iwc + 1 + it] }
                        val col2 = IntArray(cnt) { iwk2[iwc + 1 + it] }
                        val ierr = optim(cnt, col1, col2, 3 * cnt)
                        if (ierr != 0) error("Tripack.forceEdge: optim failed (ier=$ierr) while forcing $in1-$in2")
                    }
                    return
                }
            }
        }
    }

    private fun edgeInvalidMessage(in1: Int, in2: Int) =
        "Tripack.forceEdge: cannot connect nodes $in1 and $in2 (invalid triangulation, or collinear nodes " +
                "on the convex hull boundary -- check for self-intersecting or degenerate contours)"

    // -------------------------------------------------------------------------------
    // Triangle enumeration (a simplified, non-constraint-aware TRLIST)
    // -------------------------------------------------------------------------------

    /** Returns every triangle in the triangulation as 1-based `[n1, n2, n3]` CCW triples. */
    fun enumerateTriangles(): List<IntArray> {
        val result = mutableListOf<IntArray>()
        for (n1 in 1..n) {
            val lpln1 = lend[n1]
            var lp2 = lpln1
            while (true) {
                lp2 = lptr[lp2]
                val n2 = list[lp2] // signed: negative only for the boundary wraparound sentinel
                val lp = lptr[lp2]
                val n3 = abs(list[lp])
                if (n2 >= n1 && n3 >= n1) {
                    result.add(intArrayOf(n1, n2, n3))
                }
                if (lp2 == lpln1) break
            }
        }
        return result
    }

    companion object {
        /** Tolerance for SWPTST: 20 * (double-precision machine epsilon), as computed by TRIPACK's TRMESH. */
        private const val SWTOL = 20.0 * 2.220446049250313e-16
    }
}
