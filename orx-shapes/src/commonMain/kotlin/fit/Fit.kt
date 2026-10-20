package org.openrndr.extra.shapes.fit

import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// --- small helpers -----------------------------------------------------

private fun bernstein(p0: Vector2, p1: Vector2, p2: Vector2, p3: Vector2, t: Double): Vector2 {
    val mt = 1.0 - t
    return p0 * (mt * mt * mt) +
            p1 * (3.0 * mt * mt * t) +
            p2 * (3.0 * mt * t * t) +
            p3 * (t * t * t)
}

private fun Segment2D.pointAt(t: Double): Vector2 =
    bernstein(start, control[0], control[1], end, t)

private fun dot(a: Vector2, b: Vector2) = a.x * b.x + a.y * b.y

/** Chord-length parameterization, normalized to [0, 1] over the given points. */
private fun chordLengthParams(points: List<Vector2>): DoubleArray {
    val t = DoubleArray(points.size)
    for (i in 1 until points.size) t[i] = t[i - 1] + (points[i] - points[i - 1]).length
    val total = t.last()
    if (total > 1e-9) for (i in points.indices) t[i] /= total
    else for (i in points.indices) t[i] = i.toDouble() / (points.size - 1).coerceAtLeast(1)
    return t
}

/**
 * One tangent direction per point, estimated from neighbors [window] steps
 * away on each side (falls back to a one-sided difference near the ends of
 * the whole point list, and a second-order one-sided difference at the ends
 * themselves). Computed once over the full list so that any two
 * pieces meeting at a shared point always use the exact same direction there
 * - that's what gives tangent (G1) continuity across splits.
 */
private fun estimateTangents(points: List<Vector2>, window: Int): List<Vector2> {
    val n = points.size
    return List(n) { i ->
        val d = when {
            // at the ends the second-order one-sided difference (-3p₀ + 4p₁ - p₂) / 2, more accurate than a chord
            i == 0 && 2 * window < n ->
                points[window] * 4.0 - points[0] * 3.0 - points[2 * window]
            i == n - 1 && 2 * window < n ->
                points[n - 1] * 3.0 - points[n - 1 - window] * 4.0 + points[n - 1 - 2 * window]
            else -> points[min(n - 1, i + window)] - points[max(0, i - window)]
        }
        if (d.length > 1e-9) d.normalized else Vector2(1.0, 0.0)
    }
}

/** Angle (radians) between the incoming and outgoing segment at points[i]; 0 = perfectly straight. */
private fun localTurnAngle(points: List<Vector2>, i: Int): Double {
    if (i <= 0 || i >= points.size - 1) return Double.MAX_VALUE
    val inDir = points[i] - points[i - 1]
    val outDir = points[i + 1] - points[i]
    val li = inDir.length; val lo = outDir.length
    if (li < 1e-9 || lo < 1e-9) return 0.0
    val cosA = (dot(inDir, outDir) / (li * lo)).coerceIn(-1.0, 1.0)
    return acos(cosA)
}

private fun b1(s: Double) = 3.0 * (1.0 - s) * (1.0 - s) * s
private fun b2(s: Double) = 3.0 * (1.0 - s) * s * s
private fun b0(s: Double) = (1.0 - s).pow(3)
private fun b3(s: Double) = s.pow(3)

/**
 * Fits [points] with a cubic Bezier whose tangent direction at the start is
 * fixed to [t0] and at the end to [t1] - only the handle *lengths* (alpha,
 * beta) are solved for, via 2x2 least squares. This is what makes adjacent
 * pieces join smoothly: as long as they're handed the same t0/t1 at a shared
 * point, their tangents there agree by construction.
 *
 * Falls back to a default handle length (chord/3) if the solve is degenerate
 * or would produce a negative (i.e. backwards-pointing) handle.
 */
private fun fitOnePieceConstrained(
    points: List<Vector2>,
    params: DoubleArray,
    t0: Vector2,
    t1: Vector2
): Pair<Segment2D, Pair<Int, Double>> {
    val q0 = points.first()
    val q3 = points.last()

    var c11 = 0.0; var c22 = 0.0; var c12 = 0.0
    var x1r = 0.0; var x2r = 0.0
    val t0t1 = dot(t0, t1)

    for (i in points.indices) {
        val s = params[i]
        val b1s = b1(s); val b2s = b2(s)
        val base = q0 * (b0(s) + b1s) + q3 * (b2s + b3(s))
        val r = points[i] - base

        c11 += b1s * b1s
        c22 += b2s * b2s
        c12 += -b1s * b2s * t0t1
        x1r += b1s * dot(t0, r)
        x2r += -b2s * dot(t1, r)
    }

    val det = c11 * c22 - c12 * c12
    val chord = (q3 - q0).length
    val fallback = chord / 3.0

    var alpha = 0.0
    var beta = 0.0
    if (abs(det) > 1e-9) {
        alpha = (c22 * x1r - c12 * x2r) / det
        beta = (c11 * x2r - c12 * x1r) / det
    }
    if (abs(det) <= 1e-9 || alpha < 1e-6 || beta < 1e-6) {
        alpha = fallback
        beta = fallback
    }

    val control1 = q0 + t0 * alpha
    val control2 = q3 - t1 * beta
    val fitted = Segment2D(q0, control1, control2, q3)

    var worstIndex = 0
    var worstDist = 0.0
    for (i in points.indices) {
        val d = (points[i] - fitted.pointAt(params[i])).length
        if (d > worstDist) { worstDist = d; worstIndex = i }
    }

    return fitted to (worstIndex to worstDist)
}

/** The derivative of the cubic Bézier [segment] at [u]. */
private fun derivative(segment: Segment2D, u: Double): Vector2 {
    val (p0, p1, p2, p3) = listOf(segment.start, segment.control[0], segment.control[1], segment.end)
    val v = 1.0 - u
    return ((p1 - p0) * (v * v) + (p2 - p1) * (2.0 * u * v) + (p3 - p2) * (u * u)) * 3.0
}

/** The second derivative of the cubic Bézier [segment] at [u]. */
private fun secondDerivative(segment: Segment2D, u: Double): Vector2 {
    val (p0, p1, p2, p3) = listOf(segment.start, segment.control[0], segment.control[1], segment.end)
    return ((p2 - p1 * 2.0 + p0) * (1.0 - u) + (p3 - p2 * 2.0 + p1) * u) * 6.0
}

/**
 * Moves every parameter one Newton step towards the parameter of the point on [segment] nearest to its point: the root
 * of f(u) = (Q(u) - P)·Q'(u), with f'(u) = Q'(u)·Q'(u) + (Q(u) - P)·Q''(u). After Schneider, "An algorithm for
 * automatically fitting digitized curves" (Graphics Gems, 1990).
 */
private fun reparameterize(segment: Segment2D, points: List<Vector2>, params: DoubleArray): DoubleArray =
    DoubleArray(params.size) { i ->
        val u = params[i]
        val d = segment.pointAt(u) - points[i]
        val d1 = derivative(segment, u)
        val d2 = secondDerivative(segment, u)
        val numerator = dot(d, d1)
        val denominator = dot(d1, d1) + dot(d, d2)
        if (abs(denominator) < 1e-12) u else (u - numerator / denominator).coerceIn(0.0, 1.0)
    }

/**
 * Fits [points] with one cubic Bézier leaving in [t0] and arriving in [t1], see [fitOnePieceConstrained], starting
 * from chord-length parameters. While the fit misses [errorTolerance], the parameters are moved towards the nearest
 * points on the fit by [reparameterize] and the fit is repeated, which usually lowers the error a lot, until it stops
 * improving. Schneider only reparameterizes within a few times the tolerance, which suits tolerances of a pixel but
 * skips it altogether for tighter ones. The error is the largest distance between a point and the fit at its
 * parameter, which is at least the distance to the nearest point on the fit. Returns the best fit found.
 */
private fun fitOnePiece(
    points: List<Vector2>,
    t0: Vector2,
    t1: Vector2,
    errorTolerance: Double
): Pair<Segment2D, Pair<Int, Double>> {
    var params = chordLengthParams(points)
    var best = fitOnePieceConstrained(points, params, t0, t1)
    if (best.second.second <= errorTolerance) return best
    var current = best
    repeat(MAX_REPARAMETERIZATIONS) {
        params = reparameterize(current.first, points, params)
        current = fitOnePieceConstrained(points, params, t0, t1)
        val improvement = best.second.second - current.second.second
        if (current.second.second < best.second.second) best = current
        if (best.second.second <= errorTolerance) return best
        // stop once the parameters have settled
        if (improvement < 1e-3 * best.second.second) return best
    }
    return best
}

private const val MAX_REPARAMETERIZATIONS = 20

/**
 * The turning angle in degrees at points[i] between the directions from [window] points before it and to [window]
 * points after it, with indices wrapping around for a [cyclic] list; 0 at the ends of an open list.
 */
private fun turnAngle(points: List<Vector2>, i: Int, window: Int, cyclic: Boolean): Double {
    val n = points.size
    val a = if (cyclic) points[(i - window).mod(n)] else points[max(0, i - window)]
    val b = if (cyclic) points[(i + window).mod(n)] else points[min(n - 1, i + window)]
    val inDir = points[i] - a
    val outDir = b - points[i]
    val li = inDir.length
    val lo = outDir.length
    if (li < 1e-9 || lo < 1e-9) return 0.0
    return acos((dot(inDir, outDir) / (li * lo)).coerceIn(-1.0, 1.0)) * 180.0 / PI
}

/**
 * The corners of [points]: points that turn by more than [cornerAngle] degrees, measured over [window] points on
 * either side, and turn the most among the points within [window] of them.
 */
private fun findCorners(points: List<Vector2>, cornerAngle: Double, window: Int, cyclic: Boolean): List<Int> {
    val n = points.size
    val range = if (cyclic) 0 until n else 1 until n - 1
    val angles = DoubleArray(n) { if (cyclic || it in range) turnAngle(points, it, window, cyclic) else 0.0 }
    return range.filter { i ->
        angles[i] > cornerAngle && (1..window).all { k ->
            val before = if (cyclic) (i - k).mod(n) else i - k
            val after = if (cyclic) (i + k).mod(n) else i + k
            (before !in 0 until n || angles[before] <= angles[i]) && (after !in 0 until n || angles[after] < angles[i])
        }
    }
}

/**
 * Fits a polyline of [points] with one or more cubic Beziers, splitting wherever a single piece can't stay within
 * [errorTolerance], after Schneider, "An algorithm for automatically fitting digitized curves" (Graphics Gems, 1990):
 *
 * - Tangent directions are estimated once, globally, and pieces meeting at a shared point always use the same
 *   direction there - no visible kinks at split boundaries.
 * - Every piece is fitted by least squares for the lengths of its handles, and while it misses the tolerance by not
 *   too much, the parameters of the points are moved towards their nearest points on the fit with Newton's method and
 *   the fit is repeated. That fits more points with fewer pieces.
 * - The split location is chosen near the worst-fitting point, but biased toward the flattest (lowest local turn
 *   angle) point within [curvatureSearchRadius] of it - splitting where the data is already nearly straight makes any
 *   residual join even less noticeable.
 * - With a [cornerAngle], points where the polyline turns more sharply than that stay corners: the curve is split
 *   there first, and the pieces on either side follow the polyline into the corner instead of rounding it.
 * - A [closed] polyline gives a closed loop of pieces, smooth at the first point unless that is a corner.
 *
 * @param points                  the points to fit, in order along the curve
 * @param errorTolerance          max allowed distance between any input point and the fitted curve
 * @param maxDepth                recursion limit; caps output at 2^maxDepth pieces
 * @param minPointsToSplit        don't split a piece with fewer points than this
 * @param tangentWindow           how many neighbors on each side to use when estimating a point's tangent, and when
 *                                measuring how sharply it turns for corner detection
 * @param preferLowCurvatureSplits if true, nudges the split point toward lower local curvature
 * @param curvatureSearchRadius   how far (in points) to search for a flatter split point
 * @param cornerAngle             the turning angle in degrees above which a point is a corner, or null for no corners
 * @param closed                  whether the polyline is closed; the last point should then not repeat the first
 */
fun fitCubicBeziers(
    points: List<Vector2>,
    errorTolerance: Double = 1.0,
    maxDepth: Int = 10,
    minPointsToSplit: Int = 5,
    tangentWindow: Int = 1,
    preferLowCurvatureSplits: Boolean = true,
    curvatureSearchRadius: Int = 3,
    cornerAngle: Double? = null,
    closed: Boolean = false
): List<Segment2D> {
    require(points.size >= 2) { "need at least 2 points to fit a curve" }
    val repeatsFirst = points.size > 2 && points.first().distanceTo(points.last()) <= 1e-9
    val distinct = if (closed && repeatsFirst) points.dropLast(1) else points
    if (closed) require(distinct.size >= 3) { "need at least 3 points to fit a closed curve" }

    fun fitSection(section: List<Vector2>, tangents: List<Vector2>): List<Segment2D> {
        fun recurse(lo: Int, hi: Int, depth: Int): List<Segment2D> {
            if (hi - lo == 1) {
                // two points: handles of a third of the chord along their tangents, which keeps the joins smooth
                val a = section[lo]; val b = section[hi]
                val handle = (b - a).length / 3.0
                return listOf(Segment2D(a, a + tangents[lo] * handle, b - tangents[hi] * handle, b))
            }

            val pts = section.subList(lo, hi + 1)
            val (segment, worst) = fitOnePiece(pts, tangents[lo], tangents[hi], errorTolerance)
            val (worstLocalIndex, worstDist) = worst

            val canSplit = pts.size >= minPointsToSplit && depth < maxDepth &&
                    worstLocalIndex > 0 && worstLocalIndex < pts.size - 1

            if (worstDist <= errorTolerance || !canSplit) return listOf(segment)

            var splitLocal = worstLocalIndex
            if (preferLowCurvatureSplits) {
                val searchLo = max(1, worstLocalIndex - curvatureSearchRadius)
                val searchHi = min(pts.size - 2, worstLocalIndex + curvatureSearchRadius)
                splitLocal = (searchLo..searchHi).minByOrNull { localTurnAngle(pts, it) } ?: worstLocalIndex
            }

            val splitGlobal = lo + splitLocal
            return recurse(lo, splitGlobal, depth + 1) + recurse(splitGlobal, hi, depth + 1)
        }
        return recurse(0, section.size - 1, 0)
    }

    val corners = cornerAngle?.let { findCorners(distinct, it, tangentWindow, closed) } ?: emptyList()

    if (closed && corners.isEmpty()) {
        // one smooth loop: tangents with wrapping neighbors, the first point repeated at the end
        val n = distinct.size
        val tangents = List(n) { i ->
            val d = distinct[(i + tangentWindow).mod(n)] - distinct[(i - tangentWindow).mod(n)]
            if (d.length > 1e-9) d.normalized else Vector2(1.0, 0.0)
        }
        return fitSection(distinct + distinct.first(), tangents + tangents.first())
    }

    // the sequence of points split at corners into sections, each fitted on its own with one-sided tangents at its ends
    val sequence: List<Vector2>
    val breaks: List<Int>
    if (closed) {
        val first = corners.first()
        sequence = distinct.drop(first) + distinct.take(first) + distinct[first]
        breaks = corners.map { (it - first).mod(distinct.size) }.sorted() + distinct.size
    } else {
        sequence = distinct
        breaks = listOf(0) + corners + (distinct.size - 1)
    }
    return breaks.zipWithNext().flatMap { (a, b) ->
        val section = sequence.subList(a, b + 1)
        fitSection(section, estimateTangents(section, tangentWindow))
    }
}

/** Stitch the fitted pieces into a single contour, e.g. for drawing; [closed] for the pieces of a closed fit. */
fun List<Segment2D>.toContour(closed: Boolean = false): ShapeContour = ShapeContour(this, closed = closed)

// --- bridging back to the distortion use case -------------------------------

/** Same idea as before: sample the curve, distort each sample, fit tangent-continuous beziers to the result. */
fun distortedSegment(
    curve: Segment2D,
    samples: Int = 40,
    errorTolerance: Double = 0.5,
    maxDepth: Int = 8,
    distort: (Vector2) -> Vector2
): List<Segment2D> {
    val points = (0..samples).map { i -> distort(curve.pointAt(i.toDouble() / samples)) }
    return fitCubicBeziers(points, errorTolerance, maxDepth)
}