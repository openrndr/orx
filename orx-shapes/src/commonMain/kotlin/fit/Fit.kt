package org.openrndr.extra.shapes.fit

import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import kotlin.math.abs
import kotlin.math.pow

import org.openrndr.shape.ShapeContour
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
 * the whole point list). Computed once over the full list so that any two
 * pieces meeting at a shared point always use the exact same direction there
 * - that's what gives tangent (G1) continuity across splits.
 */
private fun estimateTangents(points: List<Vector2>, window: Int): List<Vector2> {
    val n = points.size
    return List(n) { i ->
        val a = points[max(0, i - window)]
        val b = points[min(n - 1, i + window)]
        val d = b - a
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

/**
 * Fits a polyline of [points] with one or more tangent-continuous cubic
 * Beziers, splitting wherever a single piece can't stay within
 * [errorTolerance]. Compared to the plain least-squares version:
 *
 * - Tangent directions are estimated once, globally, and pieces meeting at
 *   a shared point always use the same direction there - no visible kinks
 *   at split boundaries.
 * - The split location is chosen near the worst-fitting point, but biased
 *   toward the flattest (lowest local turn angle) point within
 *   [curvatureSearchRadius] of it - splitting where the data is already
 *   nearly straight makes any residual join even less noticeable.
 *
 * @param points                  the points to fit, in order along the curve
 * @param errorTolerance          max allowed distance between any input point and the fitted curve
 * @param maxDepth                recursion limit; caps output at 2^maxDepth pieces
 * @param minPointsToSplit        don't split a piece with fewer points than this
 * @param tangentWindow           how many neighbors on each side to use when estimating a point's tangent
 * @param preferLowCurvatureSplits if true, nudges the split point toward lower local curvature
 * @param curvatureSearchRadius   how far (in points) to search for a flatter split point
 */
fun fitCubicBeziers(
    points: List<Vector2>,
    errorTolerance: Double = 1.0,
    maxDepth: Int = 10,
    minPointsToSplit: Int = 5,
    tangentWindow: Int = 1,
    preferLowCurvatureSplits: Boolean = true,
    curvatureSearchRadius: Int = 3
): List<Segment2D> {
    require(points.size >= 2) { "need at least 2 points to fit a curve" }
    val tangents = estimateTangents(points, tangentWindow)

    fun recurse(lo: Int, hi: Int, depth: Int): List<Segment2D> {
        if (hi - lo == 1) {
            val a = points[lo]; val b = points[hi]
            return listOf(Segment2D(a, a + (b - a) * (1.0 / 3.0), a + (b - a) * (2.0 / 3.0), b))
        }

        val pts = points.subList(lo, hi + 1)
        val params = chordLengthParams(pts)
        val (segment, worst) = fitOnePieceConstrained(pts, params, tangents[lo], tangents[hi])
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

    return recurse(0, points.size - 1, 0)
}

/** Stitch the fitted pieces into a single open contour, e.g. for drawing. */
fun List<Segment2D>.toContour(): ShapeContour = ShapeContour(this, closed = false)

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