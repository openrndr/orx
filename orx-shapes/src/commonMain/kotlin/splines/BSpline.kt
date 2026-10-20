package org.openrndr.extra.shapes.splines

import org.openrndr.extra.shapes.ph.cubicApproximation
import org.openrndr.math.LinearType
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.YPolarity
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Segment3D
import org.openrndr.shape.ShapeContour
import kotlin.jvm.JvmName
import kotlin.math.abs
import kotlin.math.pow

/**
 * A B-spline curve of [degree] p with [controlPoints] and a non-decreasing [knots] vector of length
 * controlPoints.size + p + 1, after Piegl and Tiller, "The NURBS Book" (Springer, 1997). The curve is defined on the
 * [domain] from knot p to knot controlPoints.size, is C^(p-k) where a knot has multiplicity k, and stays inside the
 * convex hull of every p + 1 consecutive control points.
 *
 * Create one with [BSpline.clamped], [BSpline.periodic] or by interpolation with [interpolatingBSpline].
 *
 * @param closed whether the curve is closed, as a [periodic] spline is; drawn as a closed contour or path
 */
class BSpline<T : LinearType<T>>(
    val controlPoints: List<T>,
    val degree: Int,
    val knots: List<Double>,
    val closed: Boolean = false
) {
    init {
        require(degree >= 1) { "degree should be at least 1, got $degree" }
        require(controlPoints.size >= degree + 1) {
            "a degree $degree B-spline needs at least ${degree + 1} control points, got ${controlPoints.size}"
        }
        require(knots.size == controlPoints.size + degree + 1) {
            "expected ${controlPoints.size + degree + 1} knots, got ${knots.size}"
        }
        require(knots.zipWithNext().all { (a, b) -> a <= b }) { "knots should be non-decreasing" }
        require(knots[degree] < knots[controlPoints.size]) { "the domain should not be empty" }
    }

    /** The parameter range on which the curve is defined. */
    val domain: ClosedFloatingPointRange<Double> get() = knots[degree]..knots[controlPoints.size]

    /** The knot span index k with knots[k] <= u < knots[k + 1], the last span for the end of the domain. */
    private fun span(u: Double): Int {
        val n = controlPoints.size
        if (u >= knots[n]) {
            var k = n - 1
            while (knots[k] >= knots[n]) k--
            return k
        }
        var k = degree
        while (k < n - 1 && knots[k + 1] <= u) k++
        return k
    }

    /** The position at parameter [u] in the [domain], by de Boor's algorithm. */
    fun position(u: Double): T {
        val t = u.coerceIn(domain)
        val k = span(t)
        val d = MutableList(degree + 1) { controlPoints[it + k - degree] }
        for (r in 1..degree) {
            for (j in degree downTo r) {
                val i = j + k - degree
                val alpha = (t - knots[i]) / (knots[i + degree - r + 1] - knots[i])
                d[j] = d[j - 1] * (1.0 - alpha) + d[j] * alpha
            }
        }
        return d[degree]
    }

    /**
     * The derivative B-spline of degree p - 1, with control points p·(Pᵢ₊₁ - Pᵢ) / (uᵢ₊ₚ₊₁ - uᵢ₊₁) and the inner
     * knots; null for degree 1, whose derivative is piecewise constant.
     */
    fun derivativeSpline(): BSpline<T>? {
        if (degree == 1) return null
        val points = List(controlPoints.size - 1) { i ->
            val span = knots[i + degree + 1] - knots[i + 1]
            if (span == 0.0) controlPoints[i + 1] - controlPoints[i + 1]
            else (controlPoints[i + 1] - controlPoints[i]) * (degree / span)
        }
        return BSpline(points, degree - 1, knots.subList(1, knots.size - 1), closed)
    }

    /** The derivative at parameter [u]. */
    fun derivative(u: Double): T {
        val derivative = derivativeSpline()
        if (derivative != null) return derivative.position(u)
        val k = span(u.coerceIn(domain))
        return (controlPoints[k] - controlPoints[k - 1]) / (knots[k + 1] - knots[k])
    }

    /**
     * The spline with knot [u] inserted once, by Boehm's algorithm (Piegl and Tiller, algorithm A5.1); it describes the
     * same curve. With k the last knot index at or before u and s the multiplicity of u, the control points k - p + 1
     * to k - s are replaced by points on the edges of the control polygon.
     */
    fun insertKnot(u: Double): BSpline<T> {
        require(u in domain) { "the knot $u should lie in the domain $domain" }
        val k = knots.indexOfLast { it <= u }
        require(k < knots.size - 1) { "no knot span starts at $u" }
        val multiplicity = knots.count { it == u }
        require(multiplicity < degree) { "the knot $u already has multiplicity $multiplicity" }
        val points = List(controlPoints.size + 1) { i ->
            when {
                i <= k - degree -> controlPoints[i]
                i >= k - multiplicity + 1 -> controlPoints[i - 1]
                else -> {
                    val alpha = (u - knots[i]) / (knots[i + degree] - knots[i])
                    controlPoints[i - 1] * (1.0 - alpha) + controlPoints[i] * alpha
                }
            }
        }
        val newKnots = knots.toMutableList().apply { add(k + 1, u) }
        return BSpline(points, degree, newKnots, closed)
    }

    /**
     * The control points of the Bézier pieces of the curve, one list of p + 1 points per non-empty knot span of the
     * [domain]: every knot in the domain, its ends included, is inserted until it has multiplicity p.
     */
    fun bezierPieces(): List<List<T>> {
        var spline = this
        val breaks = knots.filter { it in domain }.distinct()
        for (u in breaks) {
            while (spline.knots.count { it == u } < degree) spline = spline.insertKnot(u)
        }
        val result = mutableListOf<List<T>>()
        for ((a, b) in breaks.zipWithNext()) {
            val j = spline.knots.indexOfLast { it == a }.let { last ->
                // the span [a, b) starts at the last occurrence of a
                if (spline.knots[last + 1] == b) last else error("no span from $a to $b")
            }
            result.add(spline.controlPoints.subList(j - degree, j + 1).toList())
        }
        return result
    }

    companion object {
        /**
         * The clamped spline on uniform knots: the first and last knot have multiplicity p + 1, so the curve starts at
         * the first control point and ends at the last, in the directions of the first and last edge of the control
         * polygon.
         */
        fun <T : LinearType<T>> clamped(controlPoints: List<T>, degree: Int = 3): BSpline<T> {
            val n = controlPoints.size
            val spans = n - degree
            val knots = List(n + degree + 1) { i ->
                when {
                    i <= degree -> 0.0
                    i >= n -> 1.0
                    else -> (i - degree).toDouble() / spans
                }
            }
            return BSpline(controlPoints, degree, knots)
        }

        /**
         * The closed spline on uniform knots: the first p control points are repeated at the end, so that the curve
         * returns to where it started with continuous derivatives up to order p - 1. It doesn't pass through the
         * control points.
         */
        fun <T : LinearType<T>> periodic(controlPoints: List<T>, degree: Int = 3): BSpline<T> {
            require(controlPoints.size >= 2) { "a periodic B-spline needs at least 2 control points" }
            val wrapped = controlPoints + List(degree) { controlPoints[it % controlPoints.size] }
            val knots = List(wrapped.size + degree + 1) { it.toDouble() }
            return BSpline(wrapped, degree, knots, closed = true)
        }
    }
}

/**
 * The B-spline with this control polygon: [BSpline.clamped] for an open polygon, [BSpline.periodic] for a closed one.
 * The degree is reduced where there are too few control points for it.
 */
fun <T : LinearType<T>> List<T>.bSpline(degree: Int = 3, closed: Boolean = false): BSpline<T> {
    val p = if (closed) degree else minOf(degree, size - 1)
    return if (closed) BSpline.periodic(this, p) else BSpline.clamped(this, p)
}

/**
 * The B-spline of [degree] that passes through these points, after Piegl and Tiller, "The NURBS Book", algorithm A9.1.
 *
 * Open curves are clamped. Every point gets a parameter from the lengths of the edges between them raised to
 * [alpha]: 0 gives uniform, 0.5 centripetal and 1 chord-length parameters, as for Catmull-Rom splines; centripetal
 * parameters avoid loops and cusps best. The knots are averages of p consecutive parameters, which keeps the system
 * of equations for the control points well conditioned.
 *
 * Closed curves are periodic on uniform knots, with uniform parameters: at the knots for odd degrees and halfway
 * between them for even ones, where the cyclic system would otherwise be singular for an even number of points.
 *
 * The degree is reduced where there are too few points for it.
 */
fun <T : LinearType<T>> List<T>.interpolatingBSpline(
    degree: Int = 3,
    closed: Boolean = false,
    alpha: Double = 0.5,
    distance: (T, T) -> Double
): BSpline<T> {
    require(size >= 2) { "interpolation needs at least 2 points" }
    return if (closed) interpolatePeriodic(this, degree) else interpolateClamped(this, minOf(degree, size - 1), alpha, distance)
}

/** The B-spline of [degree] that passes through these 2D points, see [interpolatingBSpline]. */
@JvmName("interpolatingBSpline2D")
fun List<Vector2>.interpolatingBSpline(degree: Int = 3, closed: Boolean = false, alpha: Double = 0.5): BSpline<Vector2> =
    interpolatingBSpline(degree, closed, alpha) { a, b -> a.distanceTo(b) }

/** The B-spline of [degree] that passes through these 3D points, see [interpolatingBSpline]. */
@JvmName("interpolatingBSpline3D")
fun List<Vector3>.interpolatingBSpline(degree: Int = 3, closed: Boolean = false, alpha: Double = 0.5): BSpline<Vector3> =
    interpolatingBSpline(degree, closed, alpha) { a, b -> a.distanceTo(b) }

private fun <T : LinearType<T>> interpolateClamped(
    points: List<T>,
    degree: Int,
    alpha: Double,
    distance: (T, T) -> Double
): BSpline<T> {
    val n = points.size
    val steps = points.zipWithNext { a, b -> distance(a, b).pow(alpha) }
    val total = steps.sum()
    val parameters = DoubleArray(n)
    for (k in 1 until n) parameters[k] = if (total > 0.0) parameters[k - 1] + steps[k - 1] / total else k / (n - 1.0)
    parameters[n - 1] = 1.0
    val knots = MutableList(n + degree + 1) { 0.0 }
    for (i in n until n + degree + 1) knots[i] = 1.0
    for (j in 1..n - degree - 1) {
        knots[j + degree] = (j until j + degree).sumOf { parameters[it] } / degree
    }
    val matrix = Array(n) { row -> basisRow(knots, degree, n, parameters[row]) }
    val controlPoints = solveDense(matrix, points)
    return BSpline(controlPoints, degree, knots)
}

private fun <T : LinearType<T>> interpolatePeriodic(points: List<T>, degree: Int): BSpline<T> {
    val n = points.size
    // the basis functions of a periodic uniform spline at a parameter: those of the template below, shifted
    val template = BSpline.periodic(points, degree)
    val offset = if (degree % 2 == 1) 0.0 else 0.5
    val wrappedCount = n + degree
    val matrix = Array(n) { row ->
        val u = degree + row + offset
        val full = basisRow(template.knots, degree, wrappedCount, u)
        // fold the repeated control points back onto the first ones
        DoubleArray(n).also { folded -> for (i in 0 until wrappedCount) folded[i % n] += full[i] }
    }
    val controlPoints = solveDense(matrix, points)
    // the curve passes through point i at parameter degree + i + offset
    return BSpline.periodic(controlPoints, degree)
}

/** The values of all [count] basis functions of [degree] on [knots] at [u], by the Cox-de Boor recursion. */
private fun basisRow(knots: List<Double>, degree: Int, count: Int, u: Double): DoubleArray {
    // the last non-empty span holds the end of the domain
    val end = knots[count]
    val t = u.coerceIn(knots[degree], end)
    var k = degree
    while (k < count - 1 && knots[k + 1] <= t) k++
    val values = DoubleArray(degree + 1)
    values[0] = 1.0
    val left = DoubleArray(degree + 1)
    val right = DoubleArray(degree + 1)
    for (j in 1..degree) {
        left[j] = t - knots[k + 1 - j]
        right[j] = knots[k + j] - t
        var saved = 0.0
        for (r in 0 until j) {
            val temp = values[r] / (right[r + 1] + left[j - r])
            values[r] = saved + right[r + 1] * temp
            saved = left[j - r] * temp
        }
        values[j] = saved
    }
    val row = DoubleArray(count)
    for (j in 0..degree) row[k - degree + j] = values[j]
    return row
}

/** Solves [matrix]·x = [rhs] for points x by Gaussian elimination with partial pivoting. */
private fun <T : LinearType<T>> solveDense(matrix: Array<DoubleArray>, rhs: List<T>): List<T> {
    val n = rhs.size
    val a = Array(n) { matrix[it].copyOf() }
    val b = rhs.toMutableList()
    for (col in 0 until n) {
        val pivot = (col until n).maxBy { abs(a[it][col]) }
        require(abs(a[pivot][col]) > 1E-14) { "the interpolation system is singular" }
        if (pivot != col) {
            val t = a[pivot]; a[pivot] = a[col]; a[col] = t
            val tb = b[pivot]; b[pivot] = b[col]; b[col] = tb
        }
        for (row in col + 1 until n) {
            val factor = a[row][col] / a[col][col]
            if (factor == 0.0) continue
            for (c in col until n) a[row][c] -= factor * a[col][c]
            b[row] = b[row] - b[col] * factor
        }
    }
    val x = MutableList(n) { b[it] }
    for (row in n - 1 downTo 0) {
        var sum = b[row]
        for (c in row + 1 until n) if (a[row][c] != 0.0) sum = sum - x[c] * a[row][c]
        x[row] = sum / a[row][row]
    }
    return x
}

/**
 * This spline as a [ShapeContour]: exactly, one Bézier segment per knot span, for degree 1 to 3, and approximated by
 * cubic Béziers within [tolerance] for higher degrees.
 */
fun BSpline<Vector2>.toContour(tolerance: Double = 0.01, polarity: YPolarity = YPolarity.CW_NEGATIVE_Y): ShapeContour {
    val segments = bezierPieces().flatMap { piece ->
        when (degree) {
            1 -> listOf(Segment2D(piece[0], piece[1]))
            2 -> listOf(Segment2D(piece[0], piece[1], piece[2]))
            3 -> listOf(Segment2D(piece[0], piece[1], piece[2], piece[3]))
            else -> {
                val bezier = BSpline.clamped(piece, degree)
                cubicApproximation(bezier::position, bezier::derivative, tolerance)
            }
        }
    }
    return ShapeContour.fromSegments(segments, closed, polarity)
}

/** This spline as a [Path3D], exactly for degree 1 to 3, see [toContour]; higher degrees are not supported. */
fun BSpline<Vector3>.toPath3D(): Path3D {
    require(degree <= 3) { "only B-splines up to degree 3 convert to a Path3D, got degree $degree" }
    val segments = bezierPieces().map { piece -> Segment3D(piece.first(), piece.subList(1, piece.size - 1), piece.last()) }
    return Path3D.fromSegments(segments, closed)
}
