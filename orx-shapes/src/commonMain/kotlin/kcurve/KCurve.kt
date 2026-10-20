package org.openrndr.extra.shapes.kcurve

import org.openrndr.extra.shapes.clothoid.solveCyclicTridiagonal
import org.openrndr.extra.shapes.clothoid.solveTridiagonal
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Converts this polyline into a κ-curve, after Yan, Schiller, Wilensky, Carr and Schaefer, "κ-Curves: Interpolation at
 * Local Maximum Curvature" (SIGGRAPH 2017): a curve of quadratic Béziers, one per point, on which every point is where
 * the curvature of its quadratic peaks. Corners of the polyline therefore become the sharpest spots of the curve, and
 * the curve never bends more sharply between the points than at them.
 *
 * Neighbouring quadratics share their end point, which lies on the line between their middle control points, so the
 * curve is G1 everywhere. Where it divides that line in the ratio of the square roots of the areas of the triangles of
 * the two control polygons, the curvature is continuous too, which makes the curve G2 everywhere except at
 * inflections. Every quadratic passes through its point at the parameter of maximum curvature, where the derivative is
 * perpendicular to the constant second derivative; for given end points that parameter is the only root in [0, 1] of a
 * cubic. The curve is found by the iteration of the paper: update the end points from the areas, the parameters from
 * the cubics, then solve the linear, tridiagonal system that makes every quadratic pass through its point at its
 * parameter, until the control points no longer move.
 *
 * The first and last point of an open polyline are the end points of the curve rather than curvature maxima.
 *
 * Not every polyline has a κ-curve: for some erratic points, typically with a point very close to where two
 * quadratics meet, no curve has its curvature maxima at all points, and the iteration doesn't converge (about 1 in 10
 * random point sets in the tests). The curve then still passes through all points and is G1, but the maxima and the
 * continuity of curvature are only approximate.
 *
 * @param closed whether the polyline is closed, the last point should then not repeat the first
 * @param polarity the polarity of the resulting contour
 */
fun List<Vector2>.kCurve(
    closed: Boolean,
    polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): ShapeContour {
    val points = filterIndexed { i, p ->
        val next = if (i + 1 < size) this[i + 1] else if (closed) this[0] else null
        next == null || p.squaredDistanceTo(next) > 1E-18
    }
    if (points.size < 3) {
        return ShapeContour.fromPoints(points, closed, polarity)
    }
    return ShapeContour.fromSegments(KCurve(points, closed).solve(), closed, polarity)
}

/** The iteration of [kCurve] over the middle control points, end point ratios and maximum curvature parameters. */
private class KCurve(val points: List<Vector2>, val closed: Boolean) {
    /** The points that are curvature maxima, one per quadratic. */
    val targets = if (closed) points else points.subList(1, points.size - 1)
    val m = targets.size
    val scale = points.zipWithNext { a, b -> a.distanceTo(b) }.maxOrNull() ?: 1.0

    /** The middle control point of every quadratic. */
    var middles = targets.toMutableList()

    /** λ[i] places the start of quadratic i between the middles of quadratics i - 1 and i; unused for an open start. */
    val lambdas = DoubleArray(m) { 0.5 }
    val parameters = DoubleArray(m) { 0.5 }

    fun start(i: Int): Vector2 = when {
        !closed && i == 0 -> points.first()
        else -> middles[(i - 1 + m) % m] * (1.0 - lambdas[i]) + middles[i] * lambdas[i]
    }

    fun end(i: Int): Vector2 = if (!closed && i == m - 1) points.last() else start((i + 1) % m)

    /** One step of the iteration of the paper: the middles that follow from the ratios and parameters of [current]. */
    fun step(current: List<Vector2>): List<Vector2>? {
        middles = current.toMutableList()
        updateLambdas()
        for (i in 0 until m) parameters[i] = maximumCurvatureParameter(start(i), end(i), targets[i])
        return solveMiddles()
    }

    /**
     * Iterates [step] to its fixed point with Anderson acceleration: every next iterate combines the last few steps
     * with the weights that minimize the combined residual step(x) - x. Plain iteration can settle into a cycle where a
     * point lies close to the end of its quadratic; the acceleration breaks those cycles.
     *
     * For some erratic points there is no fixed point, no curve on which every point is a curvature maximum; neither the
     * acceleration nor Newton's method on the residual finds one. The result is then the step from the iterate with the
     * smallest residual. The quadratics keep the ratios and parameters that step used, so every quadratic still passes
     * through its point and the curve is still G1; only the maximum and the continuity of curvature are approximate.
     */
    fun solve(): List<Segment2D> {
        val depth = 6
        val xs = ArrayDeque<DoubleArray>()
        val gs = ArrayDeque<DoubleArray>()
        var x = flatten(middles)
        var best = x
        var bestNorm = Double.POSITIVE_INFINITY
        for (iteration in 0 until MAX_ITERATIONS) {
            val g = step(unflatten(x))?.let { flatten(it) } ?: break
            val norm = x.indices.maxOf { abs(g[it] - x[it]) }
            if (norm < bestNorm) {
                best = x
                bestNorm = norm
            }
            if (norm < 1E-12 * scale) break
            xs.addLast(x); gs.addLast(g)
            if (xs.size > depth) { xs.removeFirst(); gs.removeFirst() }
            x = andersonMix(xs, gs) ?: g
        }
        // the ratios and parameters of the best iterate, with the middles that pass through the points for them
        val solved = step(unflatten(best))
        if (solved != null) middles = solved.toMutableList()
        return List(m) { Segment2D(start(it), middles[it], end(it)) }
    }

    private fun flatten(points: List<Vector2>) = DoubleArray(2 * points.size) { if (it % 2 == 0) points[it / 2].x else points[it / 2].y }
    private fun unflatten(x: DoubleArray) = List(x.size / 2) { Vector2(x[2 * it], x[2 * it + 1]) }

    /**
     * The ratios that make the curvature continuous where quadratics i - 1 and i meet: the square root of the area of
     * the triangle of the control polygon of i - 1 over the sum of that of both. Both areas vanish for collinear
     * control points, which then meet halfway.
     */
    fun updateLambdas() {
        val next = DoubleArray(m) { i ->
            if (!closed && i == 0) return@DoubleArray lambdas[0]
            val previous = (i - 1 + m) % m
            val a1 = sqrt(triangleArea(start(previous), middles[previous], middles[i]))
            val a2 = sqrt(triangleArea(middles[previous], middles[i], end(i)))
            if (a1 + a2 > 1E-12 * scale) a1 / (a1 + a2) else 0.5
        }
        next.copyInto(lambdas)
    }

    /**
     * The middle control points for which every quadratic passes through its target at its parameter t:
     * (1-t)²·start + 2t(1-t)·middle + t²·end = target, with start and end on the lines between neighbouring middles.
     */
    fun solveMiddles(): List<Vector2>? {
        val sub = DoubleArray(m)
        val diagonal = DoubleArray(m)
        val sup = DoubleArray(m)
        val rhsX = DoubleArray(m)
        val rhsY = DoubleArray(m)
        for (i in 0 until m) {
            val t = parameters[i]
            val u = 1.0 - t
            var rhs = targets[i]
            diagonal[i] = 2.0 * t * u
            if (!closed && i == 0) {
                rhs -= points.first() * (u * u)
            } else {
                sub[i] = u * u * (1.0 - lambdas[i])
                diagonal[i] += u * u * lambdas[i]
            }
            if (!closed && i == m - 1) {
                rhs -= points.last() * (t * t)
            } else {
                val nextLambda = lambdas[(i + 1) % m]
                diagonal[i] += t * t * (1.0 - nextLambda)
                sup[i] = t * t * nextLambda
            }
            rhsX[i] = rhs.x
            rhsY[i] = rhs.y
        }
        val (x, y) = if (closed) {
            Pair(solveCyclicTridiagonal(sub, diagonal, sup, rhsX), solveCyclicTridiagonal(sub, diagonal, sup, rhsY))
        } else {
            Pair(solveTridiagonal(sub, diagonal, sup, rhsX), solveTridiagonal(sub, diagonal, sup, rhsY))
        }
        if (x == null || y == null) return null
        return List(m) { Vector2(x[it], y[it]) }
    }
}

private const val MAX_ITERATIONS = 500

/**
 * The Anderson-accelerated next iterate from the last iterates [xs] and their images [gs]: Σ αₖ·gₖ with Σ αₖ = 1 and α
 * minimizing |Σ αₖ·(gₖ - xₖ)|, found from the normal equations of the differences of the residuals. Null when they are
 * singular.
 */
private fun andersonMix(xs: List<DoubleArray>, gs: List<DoubleArray>): DoubleArray? {
    val k = xs.size
    if (k == 1) return gs[0]
    val n = xs[0].size
    val f = List(k) { j -> DoubleArray(n) { gs[j][it] - xs[j][it] } }
    // minimize |f[k-1] - Σ γⱼ (f[j+1] - f[j])| over γ
    val df = List(k - 1) { j -> DoubleArray(n) { f[j + 1][it] - f[j][it] } }
    val dg = List(k - 1) { j -> DoubleArray(n) { gs[j + 1][it] - gs[j][it] } }
    val size = k - 1
    val normal = Array(size) { a -> DoubleArray(size) { b -> dot(df[a], df[b]) } }
    val rhs = DoubleArray(size) { dot(df[it], f[k - 1]) }
    val trace = (0 until size).sumOf { normal[it][it] }
    for (j in 0 until size) normal[j][j] += 1E-12 * trace + 1E-300
    val gamma = solveDense(normal, rhs) ?: return null
    val result = gs[k - 1].copyOf()
    for (j in 0 until size) for (i in 0 until n) result[i] -= gamma[j] * dg[j][i]
    return if (result.all { it.isFinite() }) result else null
}

private fun dot(a: DoubleArray, b: DoubleArray): Double {
    var sum = 0.0
    for (i in a.indices) sum += a[i] * b[i]
    return sum
}

/** Gaussian elimination with partial pivoting; null when singular. */
private fun solveDense(m: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
    val n = b.size
    val a = Array(n) { m[it].copyOf() }
    val x = b.copyOf()
    for (col in 0 until n) {
        val pivot = (col until n).maxBy { abs(a[it][col]) }
        if (abs(a[pivot][col]) == 0.0) return null
        if (pivot != col) {
            val t = a[pivot]; a[pivot] = a[col]; a[col] = t
            val tx = x[pivot]; x[pivot] = x[col]; x[col] = tx
        }
        for (row in col + 1 until n) {
            val factor = a[row][col] / a[col][col]
            for (c in col until n) a[row][c] -= factor * a[col][c]
            x[row] -= factor * x[col]
        }
    }
    for (row in n - 1 downTo 0) {
        var sum = x[row]
        for (c in row + 1 until n) sum -= a[row][c] * x[c]
        x[row] = sum / a[row][row]
    }
    return x
}

private fun triangleArea(a: Vector2, b: Vector2, c: Vector2): Double {
    val u = b - a
    val v = c - a
    return abs(u.x * v.y - u.y * v.x) / 2.0
}

/**
 * The parameter t at which the quadratic from [start] to [end] that passes through [target] at t has its maximum
 * curvature. With a = start - target, b = end - target and u = 1 - t, the middle control point is fixed by passing
 * through the target, and the derivative is perpendicular to the constant second derivative where
 * |b|²·t³ + (a·b)·t²u - (a·b)·tu² - |a|²·u³ = 0.
 * That cubic is -|a|² at 0 and |b|² at 1 and has a single root in between, found by bisection. It is kept away from 0
 * and 1, where the middle control point would be undefined.
 */
internal fun maximumCurvatureParameter(start: Vector2, end: Vector2, target: Vector2): Double {
    val a = start - target
    val b = end - target
    val aa = a.squaredLength
    val bb = b.squaredLength
    val ab = a.dot(b)
    fun f(t: Double): Double {
        val u = 1.0 - t
        return bb * t * t * t + ab * t * t * u - ab * t * u * u - aa * u * u * u
    }
    var lo = 0.0
    var hi = 1.0
    repeat(64) {
        val mid = (lo + hi) / 2.0
        if (f(mid) < 0.0) lo = mid else hi = mid
    }
    return ((lo + hi) / 2.0).coerceIn(PARAMETER_MARGIN, 1.0 - PARAMETER_MARGIN)
}

private const val PARAMETER_MARGIN = 1E-6
