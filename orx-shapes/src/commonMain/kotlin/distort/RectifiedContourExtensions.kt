package org.openrndr.extra.shapes.distort

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Distorts the contour underlying this [RectifiedContour] using a distortion function that is
 * evaluated on the rectified (proportional to contour length) t parameter.
 *
 * This works like [ShapeContour.distort], but the [distort] function additionally receives the
 * rectified t value of the point it is displacing. That makes it possible to express distortions
 * in terms of arc length position, for example tapering an effect towards the ends of the contour
 * or applying a wave with a constant wavelength in curve-length units.
 *
 * The result is built from adaptively split cubic Beziers, one group per segment of the original
 * contour, such that corners of the original contour are preserved. Each candidate piece is fit
 * with its endpoints fixed to the true distorted positions at that piece's boundaries, so
 * neighbouring pieces always join up exactly.
 *
 * @param errorTolerance max allowed RMS fit error before a piece is split in two
 * @param maxDepth recursion limit; caps output at 2^maxDepth pieces per segment. Keep this modest (5-8)
 * @param samplesPerPiece interior samples used both for fitting and for measuring the resulting error
 * @param distort maps a rectified t value and the position at that t to a distorted position
 */
fun RectifiedContour.distort(
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    samplesPerPiece: Int = 24,
    distort: (t: Double, position: Vector2) -> Vector2
): ShapeContour {
    if (contour.empty) {
        return ShapeContour.EMPTY
    }

    fun displaced(t: Double): Vector2 = distort(t, position(t))

    fun b1(s: Double) = 3.0 * (1.0 - s) * (1.0 - s) * s
    fun b2(s: Double) = 3.0 * (1.0 - s) * s * s

    /**
     * Fits a cubic Bezier to displaced(t) for rectified t in [t0, t1] and returns the fitted
     * segment together with the RMS error against the sampled field.
     */
    fun fitRange(t0: Double, t1: Double): Pair<Segment2D, Double> {
        val q0 = displaced(t0)
        val q3 = displaced(t1)

        var sB1B1 = 0.0; var sB1B2 = 0.0; var sB2B2 = 0.0
        var rhs1 = Vector2.ZERO; var rhs2 = Vector2.ZERO
        val localSamples = ArrayList<Pair<Double, Vector2>>(samplesPerPiece - 1)

        for (i in 1 until samplesPerPiece) {
            val s = i.toDouble() / samplesPerPiece
            val t = t0 + (t1 - t0) * s
            val d = displaced(t)
            localSamples.add(s to d)

            val b1s = b1(s); val b2s = b2(s)
            val endpointContribution = q0 * (1.0 - s).pow(3) + q3 * s.pow(3)
            val residual = d - endpointContribution

            sB1B1 += b1s * b1s
            sB1B2 += b1s * b2s
            sB2B2 += b2s * b2s
            rhs1 += residual * b1s
            rhs2 += residual * b2s
        }

        val det = sB1B1 * sB2B2 - sB1B2 * sB1B2
        val (c0, c1) = if (abs(det) < 1e-9) {
            Pair(q0 + (q3 - q0) * (1.0 / 3.0), q0 + (q3 - q0) * (2.0 / 3.0))
        } else {
            val cc0 = (rhs1 * sB2B2 - rhs2 * sB1B2) * (1.0 / det)
            val cc1 = (rhs2 * sB1B1 - rhs1 * sB1B2) * (1.0 / det)
            Pair(cc0, cc1)
        }

        val fitted = Segment2D(q0, c0, c1, q3)

        var sqErr = 0.0
        for ((s, d) in localSamples) {
            val diff = d - fitted.position(s)
            sqErr += diff.length * diff.length
        }
        val rms = if (localSamples.isEmpty()) 0.0 else sqrt(sqErr / localSamples.size)

        return fitted to rms
    }

    fun recurse(t0: Double, t1: Double, depth: Int): List<Segment2D> {
        val (segment, rms) = fitRange(t0, t1)
        return if (rms <= errorTolerance || depth >= maxDepth) {
            listOf(segment)
        } else {
            val tm = (t0 + t1) / 2.0
            recurse(t0, tm, depth + 1) + recurse(tm, t1, depth + 1)
        }
    }

    val segmentCount = contour.segments.size
    val rectifiedBounds = (0..segmentCount).map { inverseRectify(it.toDouble() / segmentCount) }

    val segments = rectifiedBounds.zipWithNext().filter { (t0, t1) -> t1 > t0 }.flatMap { (t0, t1) ->
        recurse(t0, t1, 0)
    }

    return if (segments.isEmpty()) {
        ShapeContour.EMPTY
    } else {
        ShapeContour(segments, contour.closed)
    }
}

/**
 * How [distortUniform] treats stretches where the distorted contour runs backwards relative to the original one,
 * for example where an offset distance exceeds the local radius of curvature.
 */
enum class FoldMode {
    /** Keep the folded stretch; the result loops back over itself (a swallowtail) */
    FOLD,

    /** Cut the folded stretch out and join the remaining pieces where they cross */
    TRIM
}

/**
 * Distorts the contour underlying this [RectifiedContour] using a distortion function that is
 * evaluated on the rectified (proportional to contour length) t parameter.
 *
 * This works like [ShapeContour.distort], but the [distort] function additionally receives the
 * rectified t value of the point it is displacing. That makes it possible to express distortions
 * in terms of arc length position, for example tapering an effect towards the ends of the contour
 * or applying a wave with a constant wavelength in curve-length units.
 *
 * The result is built from adaptively split cubic Beziers, one group per segment of the original
 * contour, such that corners of the original contour are preserved. Each candidate piece is fit
 * with its endpoints fixed to the true distorted positions at that piece's boundaries, so
 * neighbouring pieces always join up exactly.
 *
 * The distorted contour folds over itself where it runs backwards relative to the original contour, that is where
 * `dot(q'(t), p'(t)) < 0` for distorted position `q` and original position `p`. For an offset along the true normal
 * this is where the offset distance exceeds the local radius of curvature. With [FoldMode.TRIM] every such fold is
 * cut out: the backwards stretch and the swallowtail around it are removed and the pieces on either side are joined
 * at the point where they cross. Folds whose sides do not cross are collapsed to a single point. For closed contours
 * the start point of the result can move when [FoldMode.TRIM] is used.
 *
 * @param errorTolerance max allowed RMS fit error before a piece is split in two
 * @param maxDepth recursion limit; caps output at 2^maxDepth pieces per segment. Keep this modest (5-8)
 * @param sampleDistance distance in contour length between samples used for fitting and measuring the error
 * @param foldMode how folds in the distorted contour are treated, [FoldMode.FOLD] keeps them
 * @param distort maps a rectified t value and the position at that t to a distorted position
 */
fun RectifiedContour.distortUniform(
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    sampleDistance: Double = 4.0,
    foldMode: FoldMode = FoldMode.FOLD,
    distort: (t: Double, position: Vector2) -> Vector2
): ShapeContour {
    if (contour.empty) {
        return ShapeContour.EMPTY
    }

    val closed = contour.closed

    // t values outside [0, 1] only occur when trimming a closed contour, which works on an unrolled domain
    fun wrap(t: Double): Double = if (closed && (t < 0.0 || t > 1.0)) t.mod(1.0) else t

    fun displaced(t: Double): Vector2 = wrap(t).let { distort(it, position(it)) }

    fun b1(s: Double) = 3.0 * (1.0 - s) * (1.0 - s) * s
    fun b2(s: Double) = 3.0 * (1.0 - s) * s * s

    /**
     * Fits a cubic Bezier to displaced(t) for rectified t in [t0, t1] and returns the fitted
     * segment together with the RMS error against the sampled field. [start] and [end] override the
     * endpoints of the fitted segment.
     */
    fun fitRange(t0: Double, t1: Double, start: Vector2? = null, end: Vector2? = null): Pair<Segment2D, Double> {
        val q0 = start ?: displaced(t0)
        val q3 = end ?: displaced(t1)

        // The control points are constrained to the tangent directions of the distorted contour at both ends, only
        // their distances along the tangents are fitted. Free control points can overshoot the ends of the piece, which
        // leaves tiny loops at junctions with neighbouring pieces, most notably at cut joints and short pieces.
        val e = (t1 - t0) * 1E-3
        val chord = (q3 - q0).length
        val tangent0 = (displaced(t0 + e) - displaced(t0)).let { if (it.length > 1E-12) it.normalized else (q3 - q0).normalized }
        val tangent1 = (displaced(t1) - displaced(t1 - e)).let { if (it.length > 1E-12) it.normalized else (q3 - q0).normalized }

        var m11 = 0.0; var m12 = 0.0; var m22 = 0.0
        var r1 = 0.0; var r2 = 0.0
        // the fit must be overdetermined, with too few samples it interpolates them exactly and its error says nothing
        val sampleCount = (((t1 - t0)*contour.length) / sampleDistance).toInt().coerceAtLeast(8)

        val localSamples = ArrayList<Pair<Double, Vector2>>(sampleCount - 1)

        for (i in 1 until sampleCount) {
            val s = i.toDouble() / sampleCount
            val t = t0 + (t1 - t0) * s
            val d = displaced(t)
            localSamples.add(s to d)

            val b1s = b1(s); val b2s = b2(s)
            val a1 = tangent0 * b1s
            val a2 = tangent1 * -b2s
            val residual = d - q0 * ((1.0 - s).pow(3) + b1s) - q3 * (b2s + s.pow(3))

            m11 += a1.dot(a1)
            m12 += a1.dot(a2)
            m22 += a2.dot(a2)
            r1 += a1.dot(residual)
            r2 += a2.dot(residual)
        }

        val det = m11 * m22 - m12 * m12
        var alpha = if (abs(det) < 1e-12) -1.0 else (r1 * m22 - r2 * m12) / det
        var beta = if (abs(det) < 1e-12) -1.0 else (m11 * r2 - m12 * r1) / det
        // negative distances put a control point behind its end, distances adding up to more than the chord let the
        // curve run past its end and double back
        if (alpha < 1E-6 * chord || beta < 1E-6 * chord || alpha + beta > chord) {
            alpha = chord / 3.0
            beta = chord / 3.0
        }
        val c0 = q0 + tangent0 * alpha
        val c1 = q3 - tangent1 * beta

        val fitted = Segment2D(q0, c0, c1, q3)

        var sqErr = 0.0
        for ((s, d) in localSamples) {
            val diff = d - fitted.position(s)
            sqErr += diff.length * diff.length
        }
        val rms = if (localSamples.isEmpty()) 0.0 else sqrt(sqErr / localSamples.size)

        return fitted to rms
    }

    fun recurse(t0: Double, t1: Double, depth: Int, start: Vector2? = null, end: Vector2? = null): List<Segment2D> {
        val (segment, rms) = fitRange(t0, t1, start, end)
        return if (rms <= errorTolerance || depth >= maxDepth) {
            listOf(segment)
        } else {
            val tm = (t0 + t1) / 2.0
            recurse(t0, tm, depth + 1, start, null) + recurse(tm, t1, depth + 1, null, end)
        }
    }

    val segmentCount = contour.segments.size
    val rectifiedBounds = (0..segmentCount).map { inverseRectify(it.toDouble() / segmentCount) }

    val trimmed = if (foldMode == FoldMode.TRIM) {
        findFoldCuts(sampleDistance, errorTolerance, rectifiedBounds, ::displaced)
    } else {
        null
    }

    val segments = if (trimmed == null) {
        rectifiedBounds.zipWithNext().filter { (t0, t1) -> t1 > t0 }.flatMap { (t0, t1) ->
            recurse(t0, t1, 0)
        }
    } else {
        val (domainStart, cuts) = trimmed
        val domainEnd = domainStart + 1.0
        // original segment boundaries in the unrolled domain, used to keep corners in place
        val bounds = if (closed) {
            (rectifiedBounds + rectifiedBounds.map { it + 1.0 }).filter { it > domainStart && it < domainEnd }.distinct().sorted()
        } else {
            rectifiedBounds.filter { it > domainStart && it < domainEnd }
        }
        val closingPoint = if (closed) displaced(domainStart) else null

        // kept ranges between the cuts, with the joint points at their ends
        val ranges = mutableListOf<KeptRange>()
        var t = domainStart
        var startJoint: Vector2? = null
        for (cut in cuts) {
            ranges.add(KeptRange(t, cut.s1, startJoint, cut.joint))
            t = cut.s2
            startJoint = cut.joint
        }
        ranges.add(KeptRange(t, domainEnd, startJoint, closingPoint))

        ranges.filter { it.t1 - it.t0 > 1E-12 }.flatMap { range ->
            val pieces = listOf(range.t0) + bounds.filter { it > range.t0 + 1E-12 && it < range.t1 - 1E-12 } + listOf(range.t1)
            pieces.zipWithNext().withIndex().flatMap { (index, piece) ->
                val start = if (index == 0) range.start else null
                val end = if (index == pieces.size - 2) range.end else null
                recurse(piece.first, piece.second, 0, start, end)
            }
        }
    }

    return if (segments.isEmpty()) {
        ShapeContour.EMPTY
    } else {
        ShapeContour(segments, contour.closed)
    }
}

/** The maximum number of neighbouring backwards intervals that are merged when looking for the crossing of a fold */
private const val MAX_MERGED_FOLDS = 4

/** A part of the (unrolled) rectified domain that is kept, with optional joint points overriding its ends */
private class KeptRange(val t0: Double, val t1: Double, val start: Vector2?, val end: Vector2?)

/**
 * A part of the (unrolled) rectified domain that is cut out. [a] and [b] bound the backwards running stretch, [s1] and
 * [s2] bound the full cut and [joint] is the point the pieces on either side are joined at (null when the cut reaches
 * the end of an open contour).
 */
private class FoldCut(val a: Double, val b: Double, val s1: Double, val s2: Double, val joint: Vector2?)

/**
 * Finds the folds of the distorted contour [q] and the cuts that remove them.
 * @return the start of the unrolled domain (which spans one unit) and the cuts in ascending order, or null when
 * there is nothing to trim
 */
private fun RectifiedContour.findFoldCuts(
    sampleDistance: Double,
    tolerance: Double,
    rectifiedBounds: List<Double>,
    q: (Double) -> Vector2
): Pair<Double, List<FoldCut>>? {
    val closed = contour.closed
    val length = contour.length
    if (length <= 0.0) {
        return null
    }
    val h = 1E-7

    fun qPrime(t: Double): Vector2 {
        val t0 = if (closed) t - h else (t - h).coerceAtLeast(0.0)
        val t1 = if (closed) t + h else (t + h).coerceAtMost(1.0)
        return (q(t1) - q(t0)) / (t1 - t0)
    }

    fun backwards(t: Double): Boolean {
        val wrapped = if (closed) t.mod(1.0) else t
        return qPrime(t).dot(direction(wrapped)) < 0.0
    }

    // sample the fold condition on [0, 1) (closed) or [0, 1] (open), including the original segment boundaries
    val step = min(sampleDistance, 0.25) / length
    val count = ceil(1.0 / step).toInt().coerceAtLeast(8)
    val grid = ((0 until count).map { it.toDouble() / count } + rectifiedBounds + if (closed) emptyList() else listOf(1.0))
        .filter { if (closed) it < 1.0 else it <= 1.0 }
        .distinct().sorted()
    val flags = grid.map { backwards(it) }
    if (flags.all { it }) {
        return null
    }

    // unroll the domain: a closed contour starts in the middle of its longest forward stretch, halfway between two
    // samples such that it never coincides with a corner of the original contour
    val domainStart: Double
    val samples: List<Pair<Double, Boolean>>
    if (closed) {
        var bestStart = 0
        var bestLength = grid.size
        if (flags.any { it }) {
            bestLength = -1
            for (i in grid.indices) {
                if (!flags[i] && flags[(i - 1).mod(grid.size)]) {
                    var n = 0
                    while (!flags[(i + n).mod(grid.size)]) {
                        n++
                    }
                    if (n > bestLength) {
                        bestLength = n; bestStart = i
                    }
                }
            }
        }
        val mid = (bestStart + (bestLength - 1) / 2).mod(grid.size)
        val next = if (mid + 1 < grid.size) grid[mid + 1] else grid[0] + 1.0
        domainStart = (grid[mid] + next) / 2.0
        val startBackwards = backwards(domainStart)
        samples = listOf(domainStart to startBackwards) +
                grid.indices.map { i -> (if (grid[i] < domainStart) grid[i] + 1.0 else grid[i]) to flags[i] }.sortedBy { it.first } +
                listOf((domainStart + 1.0) to startBackwards)
    } else {
        domainStart = 0.0
        samples = grid.zip(flags)
    }
    val domainEnd = domainStart + 1.0

    fun refine(lo: Double, hi: Double, loBackwards: Boolean): Double {
        var l = lo
        var r = hi
        repeat(48) {
            val m = (l + r) / 2.0
            if (backwards(m) == loBackwards) l = m else r = m
        }
        return (l + r) / 2.0
    }

    // backwards intervals [a, b]
    val intervals = mutableListOf<Pair<Double, Double>>()
    var a: Double? = if (samples.first().second) domainStart else null
    for ((s0, s1) in samples.zipWithNext()) {
        if (s0.second != s1.second) {
            val c = refine(s0.first, s1.first, s0.second)
            if (s1.second) {
                a = c
            } else {
                intervals.add(a!! to c)
                a = null
            }
        }
    }
    if (a != null) {
        intervals.add(a to domainEnd)
    }

    // At a sharp corner of the original contour the distorted contour can turn back on itself in a single point,
    // without a backwards running stretch. Such corners are added as zero length candidates, they are only cut when
    // the pieces on either side of them cross.
    val corners = (if (closed) rectifiedBounds.dropLast(1).map { if (it < domainStart) it + 1.0 else it } else rectifiedBounds)
        .filter { it > domainStart && it < domainEnd }
        .filter { c -> intervals.none { (a, b) -> c >= a && c <= b } }
        .filter { c ->
            // only corners where the distorted contour turns back by more than 90 degrees
            val incoming = q(c) - q(c - h)
            val outgoing = q(c + h) - q(c)
            incoming.dot(outgoing) < 0.0
        }
    intervals.addAll(corners.map { it to it })
    intervals.sortBy { it.first }
    if (intervals.isEmpty()) {
        return null
    }

    /** the cut through the crossing around the fold [a, b], null when the pieces around it don't cross */
    fun crossingCut(a: Double, b: Double, lo: Double, hi: Double): FoldCut? {
        if (!closed && a <= lo) {
            return FoldCut(a, b, lo, b, null)
        }
        if (!closed && b >= hi) {
            return FoldCut(a, b, a, hi, null)
        }
        val crossing = findCrossing(a, b, lo, hi, q, ::qPrime) ?: return null
        return FoldCut(a, b, crossing.first, crossing.second, (q(crossing.first) + q(crossing.second)) * 0.5)
    }

    /**
     * The cut for a fold [a, b] whose sides don't cross but run alongside each other within [tolerance], a spike
     * thinner than the fit can resolve. Both sides are followed away from the fold for as long as they stay within
     * [tolerance] of each other, null when they start further apart.
     */
    fun spikeCut(a: Double, b: Double, lo: Double, hi: Double): FoldCut? {
        if (!closed && (a <= lo || b >= hi)) {
            return null
        }
        if ((q(a) - q(b)).length > tolerance) {
            return null
        }
        val ds = tolerance / 4.0 / length
        var s1 = a
        var s2 = b
        while (s1 - ds > lo) {
            val p = q(s1 - ds)
            // follow the leaving side to the point closest to p
            var best = s2
            var bestDistance = (q(s2) - p).length
            while (best + ds < hi) {
                val distance = (q(best + ds) - p).length
                if (distance > bestDistance) break
                bestDistance = distance
                best += ds
            }
            if (bestDistance > tolerance) break
            s1 -= ds
            s2 = best
        }
        if (s1 == a && s2 == b) {
            return null
        }
        return FoldCut(a, b, s1, s2, (q(s1) + q(s2)) * 0.5)
    }

    /**
     * Turns backwards intervals into cuts. At a sharp corner of the original contour its direction jumps, which can
     * split a single fold into several backwards intervals with forward gaps between them, and the sides of the fold
     * may only cross well outside of them. When the pieces around an interval don't cross, the interval is merged with
     * the following ones (up to [MAX_MERGED_FOLDS] in total) and the crossing search is repeated. Folds that still
     * have no crossing are cut as a spike when their sides run alongside each other, otherwise they are collapsed to a
     * point.
     */
    fun buildCuts(list: List<Pair<Double, Double>>): List<FoldCut> {
        val result = mutableListOf<FoldCut>()
        var i = 0
        while (i < list.size) {
            val lo = max(result.lastOrNull()?.s2 ?: domainStart, if (i > 0) list[i - 1].second else domainStart)
            var j = i
            var cut: FoldCut? = null
            while (true) {
                val hi = if (j < list.size - 1) list[j + 1].first else domainEnd
                cut = crossingCut(list[i].first, list[j].second, lo, hi)
                if (cut != null || j + 1 >= list.size || j - i + 1 >= MAX_MERGED_FOLDS) break
                j++
            }
            if (cut == null) {
                val hi = if (i < list.size - 1) list[i + 1].first else domainEnd
                cut = spikeCut(list[i].first, list[i].second, lo, hi)
                j = i
            }
            if (cut != null) {
                result.add(cut)
                i = j + 1
            } else {
                val (a, b) = list[i]
                // a corner candidate without a crossing is not a fold
                if (b > a) {
                    result.add(FoldCut(a, b, a, b, (q(a) + q(b)) * 0.5))
                }
                i++
            }
        }
        return result
    }

    var current = intervals.toList()
    var cuts = buildCuts(current)

    // merge folds whose cuts overlap and redo their crossing search
    repeat(8) {
        val overlap = (1 until cuts.size).firstOrNull { cuts[it].s1 < cuts[it - 1].s2 } ?: return if (cuts.isEmpty()) null else domainStart to cuts
        val first = current.indexOfFirst { it.first >= cuts[overlap - 1].a }
        val last = current.indexOfLast { it.second <= cuts[overlap].b }
        current = current.take(first) + listOf(current[first].first to current[last].second) + current.drop(last + 1)
        cuts = buildCuts(current)
    }
    // give up refining, take the union of overlapping cuts
    val merged = mutableListOf<FoldCut>()
    for (cut in cuts) {
        val last = merged.lastOrNull()
        if (last != null && cut.s1 < last.s2) {
            merged[merged.size - 1] = FoldCut(last.a, cut.b, last.s1, max(last.s2, cut.s2), (q(last.s1) + q(max(last.s2, cut.s2))) * 0.5)
        } else {
            merged.add(cut)
        }
    }
    return domainStart to merged
}

/**
 * Finds where the forward running piece of [q] entering the fold [a, b] crosses the forward running piece leaving it.
 * @return the parameters (s1 <= a, s2 >= b) of the crossing closest to the fold, or null when the pieces don't cross
 * within [lo, hi]
 */
private fun findCrossing(
    a: Double,
    b: Double,
    lo: Double,
    hi: Double,
    q: (Double) -> Vector2,
    qPrime: (Double) -> Vector2
): Pair<Double, Double>? {
    val samplesPerBranch = 64
    var w = max(2.0 * (b - a), 1E-6)
    while (true) {
        val w0 = min(w, a - lo)
        val w1 = min(w, hi - b)
        val incoming = (0..samplesPerBranch).map { a - w0 * it / samplesPerBranch }
        val outgoing = (0..samplesPerBranch).map { b + w1 * it / samplesPerBranch }
        val qi = incoming.map(q)
        val qo = outgoing.map(q)

        var best: Pair<Double, Double>? = null
        var bestScore = Double.POSITIVE_INFINITY
        for (i in 0 until samplesPerBranch) {
            for (j in 0 until samplesPerBranch) {
                val hit = intersectSegments(qi[i], qi[i + 1], qo[j], qo[j + 1]) ?: continue
                // for a zero length fold (a corner) both pieces start in the same point, which is not a crossing
                if (i == 0 && j == 0 && hit.first < 1E-9 && hit.second < 1E-9) continue
                val s1 = incoming[i] + (incoming[i + 1] - incoming[i]) * hit.first
                val s2 = outgoing[j] + (outgoing[j + 1] - outgoing[j]) * hit.second
                val score = (a - s1) + (s2 - b)
                if (score < bestScore) {
                    bestScore = score
                    best = s1 to s2
                }
            }
        }
        if (best != null) {
            return refineCrossing(best.first, best.second, lo, a, b, hi, q, qPrime)
        }
        // stop once both branches reach their bounds, or half the contour
        if ((w0 >= a - lo && w1 >= hi - b) || w >= 0.5) {
            return null
        }
        w *= 2.0
    }
}

/** Newton refinement of q(s1) = q(s2), keeping the polyline estimate when it does not converge within bounds */
private fun refineCrossing(
    s1Start: Double,
    s2Start: Double,
    lo: Double,
    a: Double,
    b: Double,
    hi: Double,
    q: (Double) -> Vector2,
    qPrime: (Double) -> Vector2
): Pair<Double, Double> {
    var s1 = s1Start
    var s2 = s2Start
    repeat(20) {
        val f = q(s1) - q(s2)
        if (f.length < 1E-12) {
            return s1 to s2
        }
        val d1 = qPrime(s1)
        val d2 = qPrime(s2) * -1.0
        val det = d1.x * d2.y - d1.y * d2.x
        if (abs(det) < 1E-18) {
            return s1Start to s2Start
        }
        // solve [d1 d2] (x, y) = -f
        val x = (-f.x * d2.y + f.y * d2.x) / det
        val y = (-d1.x * f.y + d1.y * f.x) / det
        s1 += x
        s2 += y
        if (s1 < lo || s1 > a || s2 < b || s2 > hi) {
            return s1Start to s2Start
        }
    }
    return if ((q(s1) - q(s2)).length < (q(s1Start) - q(s2Start)).length) s1 to s2 else s1Start to s2Start
}

/** Intersection of segments p0-p1 and p2-p3 as parameters along both, or null */
private fun intersectSegments(p0: Vector2, p1: Vector2, p2: Vector2, p3: Vector2): Pair<Double, Double>? {
    val r = p1 - p0
    val s = p3 - p2
    val denominator = r.x * s.y - r.y * s.x
    if (abs(denominator) < 1E-18) {
        return null
    }
    val d = p2 - p0
    val u = (d.x * s.y - d.y * s.x) / denominator
    val v = (d.x * r.y - d.y * r.x) / denominator
    return if (u in 0.0..1.0 && v in 0.0..1.0) u to v else null
}
