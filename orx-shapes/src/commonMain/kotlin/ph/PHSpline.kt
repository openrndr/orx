package org.openrndr.extra.shapes.ph

import org.openrndr.extra.math.complex.Complex
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.ShapeContour

/**
 * Converts this polyline into a C2 spline of Pythagorean-hodograph quintics through all points, see [phQuinticSpline],
 * as a contour of cubic Béziers within [tolerance]. With a non-zero [offset] the offset curve at that distance is
 * returned; it is evaluated exactly, PH curves have rational offsets, and approximated within [tolerance] too.
 *
 * @param closed whether the polyline is closed, the last point should then not repeat the first
 * @param offset the signed distance of the returned offset curve along the normal (-y, x), 0.0 for the spline itself
 * @param tolerance the largest distance between the cubic approximation and the exact curve
 * @param polarity the polarity of the resulting contour
 */
fun List<Vector2>.phQuintics(
    closed: Boolean,
    offset: Double = 0.0,
    tolerance: Double = 0.01,
    polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): ShapeContour {
    val curves = phQuinticSpline(closed)
    if (curves.isEmpty()) {
        return ShapeContour.fromPoints(distinctPoints(closed), closed, polarity)
    }
    return ShapeContour.fromSegments(curves.flatMap { it.segments(offset, tolerance) }, closed, polarity)
}

/**
 * The C2 interpolating spline of PH quintics through this polyline, one per edge, after Albrecht and Farouki,
 * "Construction of C2 Pythagorean-hodograph interpolating splines by the homotopy method" (Advances in Computational
 * Mathematics, 1996).
 *
 * Every edge i gets the preimage w(t) with coefficients (z[i-1] + z[i]) / 2, z[i] and (z[i] + z[i+1]) / 2, which makes
 * w C1 and therefore the spline C2. Each edge ending at its point is a complex quadratic equation,
 * 3z[i-1]² + 27z[i]² + 3z[i+1]² + 13z[i-1]z[i] + 13z[i]z[i+1] + z[i-1]z[i+1] = 60(p[i] - p[i-1]),
 * solved by Newton's method from z[i] = √(p[i] - p[i-1]), with signs chosen so that neighbouring z point the same way.
 * Open splines take z[0] = z[1] and z[n+1] = z[n], which gives zero curvature at the ends. Closed splines take
 * z[n+1] = ±z[1]: w turns by half the turning of the tangent, so for curves that turn an odd number of times, every
 * simple closed curve among them, w has to change sign once around the loop.
 *
 * When Newton's method doesn't converge, which happens for erratic points, every edge is the [phQuinticHermite]
 * interpolant with Catmull-Rom derivatives instead, which still passes through all points but is only C1.
 *
 * Returns an empty list when there are fewer than two distinct points, or three for closed polylines.
 */
fun List<Vector2>.phQuinticSpline(closed: Boolean): List<PHQuintic> {
    val points = distinctPoints(closed)
    if (points.size < 2 || (closed && points.size < 3)) return emptyList()
    return solveC2(points, closed) ?: hermiteSpline(points, closed)
}

private fun List<Vector2>.distinctPoints(closed: Boolean) = filterIndexed { i, p ->
    val next = if (i + 1 < size) this[i + 1] else if (closed) this[0] else null
    next == null || p.squaredDistanceTo(next) > 1E-18
}

/** The largest residual, relative to 60 times the mean edge length, that counts as interpolating. */
private const val TOLERANCE = 1E-12

private fun solveC2(points: List<Vector2>, closed: Boolean): List<PHQuintic>? {
    val n = if (closed) points.size else points.size - 1
    val dp = List(n) { i -> (points[(i + 1) % points.size] - points[i]).toComplex() }
    val scale = 60.0 * dp.sumOf { it.magnitude() } / n

    // initial guess, the preimages of constant hodographs along the edges, aligned with their predecessor
    val z = Array(n) { dp[it].sqrt() }
    for (i in 1 until n) {
        if ((z[i] * z[i - 1].conjugate()).real < 0.0) z[i] = -z[i]
    }
    // for closed splines, whether w changes sign once around the loop
    val sign = if (closed && (z[0] * z[n - 1].conjugate()).real < 0.0) -1.0 else 1.0

    fun previous(z: Array<Complex>, i: Int) = when {
        i > 0 -> z[i - 1]
        closed -> z[n - 1] * sign
        else -> z[0]
    }

    fun next(z: Array<Complex>, i: Int) = when {
        i < n - 1 -> z[i + 1]
        closed -> z[0] * sign
        else -> z[n - 1]
    }

    fun residuals(z: Array<Complex>) = Array(n) { i ->
        val a = previous(z, i)
        val b = z[i]
        val c = next(z, i)
        a * a * 3.0 + b * b * 27.0 + c * c * 3.0 + a * b * 13.0 + b * c * 13.0 + a * c - dp[i] * 60.0
    }

    fun norm(r: Array<Complex>) = r.maxOf { it.magnitude() } / scale
    fun squares(r: Array<Complex>) = r.sumOf { it.sqrMagnitude() }

    var current = z
    var r = residuals(current)
    for (iteration in 0 until 100) {
        if (norm(r) < TOLERANCE) break
        // the Jacobian is tridiagonal; the ends either fold back onto the diagonal (open) or wrap around (closed)
        val sub = Array(n) { ZERO }
        val diagonal = Array(n) { ZERO }
        val sup = Array(n) { ZERO }
        for (i in 0 until n) {
            val a = previous(current, i)
            val b = current[i]
            val c = next(current, i)
            val da = a * 6.0 + b * 13.0 + c
            val db = b * 54.0 + a * 13.0 + c * 13.0
            val dc = c * 6.0 + b * 13.0 + a
            diagonal[i] = diagonal[i] + db
            when {
                i > 0 -> sub[i] = da
                closed -> sub[i] = da * sign
                else -> diagonal[i] = diagonal[i] + da
            }
            when {
                i < n - 1 -> sup[i] = dc
                closed -> sup[i] = dc * sign
                else -> diagonal[i] = diagonal[i] + dc
            }
        }
        val rhs = Array(n) { -r[it] }
        // closed splines have at least three edges, so the corner entries of the cyclic system are distinct
        val step = (if (closed) solveCyclicTridiagonal(sub, diagonal, sup, rhs) else solveTridiagonal(sub, diagonal, sup, rhs))
            ?: return null
        var factor = 1.0
        var improved = false
        while (factor > 1E-6) {
            val candidate = Array(n) { current[it] + step[it] * factor }
            val candidateResiduals = residuals(candidate)
            if (squares(candidateResiduals) < squares(r)) {
                current = candidate
                r = candidateResiduals
                improved = true
                break
            }
            factor /= 2.0
        }
        if (!improved) break
    }
    if (norm(r) >= TOLERANCE) return null

    return List(n) { i ->
        val a = previous(current, i)
        val b = current[i]
        val c = next(current, i)
        PHQuintic(points[i], (a + b) / 2.0, b, (b + c) / 2.0)
    }
}

/** The C1 fallback: [phQuinticHermite] per edge, with Catmull-Rom derivatives at the points. */
private fun hermiteSpline(points: List<Vector2>, closed: Boolean): List<PHQuintic> {
    val m = points.size
    val derivatives = List(m) { i ->
        when {
            closed -> (points[(i + 1) % m] - points[(i - 1 + m) % m]) / 2.0
            i == 0 -> points[1] - points[0]
            i == m - 1 -> points[m - 1] - points[m - 2]
            else -> (points[i + 1] - points[i - 1]) / 2.0
        }
    }
    val edges = if (closed) m else m - 1
    return List(edges) { i ->
        val j = (i + 1) % m
        phQuinticHermite(points[i], derivatives[i], points[j], derivatives[j])
    }
}

private val ZERO = Complex(0.0, 0.0)

/**
 * Solves the complex tridiagonal system with sub-diagonal [a] (a[0] unused), diagonal [b] and super-diagonal [c]
 * (c[n-1] unused) for [d] by Gaussian elimination with partial pivoting. Returns null when it is singular.
 */
private fun solveTridiagonal(a: Array<Complex>, b: Array<Complex>, c: Array<Complex>, d: Array<Complex>): Array<Complex>? {
    val n = b.size
    val dl = a.copyOf(); val dd = b.copyOf(); val du = c.copyOf(); val du2 = Array(n) { ZERO }; val x = d.copyOf()
    for (i in 0 until n - 1) {
        if (dd[i].magnitude() >= dl[i + 1].magnitude()) {
            if (dd[i].sqrMagnitude() == 0.0) return null
            val f = dl[i + 1] / dd[i]
            dd[i + 1] = dd[i + 1] - f * du[i]
            x[i + 1] = x[i + 1] - f * x[i]
        } else {
            val f = dd[i] / dl[i + 1]
            dd[i] = dl[i + 1]
            val temp = dd[i + 1]
            dd[i + 1] = du[i] - f * temp
            if (i < n - 2) {
                du2[i] = du[i + 1]
                du[i + 1] = -(f * du2[i])
            }
            du[i] = temp
            val tx = x[i]
            x[i] = x[i + 1]
            x[i + 1] = tx - f * x[i + 1]
        }
    }
    if (dd[n - 1].sqrMagnitude() == 0.0) return null
    x[n - 1] = x[n - 1] / dd[n - 1]
    if (n > 1) x[n - 2] = (x[n - 2] - du[n - 2] * x[n - 1]) / dd[n - 2]
    for (i in n - 3 downTo 0) {
        x[i] = (x[i] - du[i] * x[i + 1] - du2[i] * x[i + 2]) / dd[i]
    }
    return if (x.all { it.real.isFinite() && it.imaginary.isFinite() }) x else null
}

/**
 * Solves the cyclic tridiagonal system, in which a[0] couples the first row to the last unknown and c[n-1] the last
 * row to the first, with the Sherman-Morrison formula on top of [solveTridiagonal]. Needs n > 2.
 */
private fun solveCyclicTridiagonal(
    a: Array<Complex>, b: Array<Complex>, c: Array<Complex>, d: Array<Complex>
): Array<Complex>? {
    val n = b.size
    val gamma = if (b[0].sqrMagnitude() != 0.0) -b[0] else Complex(1.0, 0.0)
    val bb = b.copyOf()
    bb[0] = b[0] - gamma
    bb[n - 1] = b[n - 1] - a[0] * c[n - 1] / gamma
    val u = Array(n) { ZERO }.also { it[0] = gamma; it[n - 1] = c[n - 1] }
    val y = solveTridiagonal(a, bb, c, d) ?: return null
    val z = solveTridiagonal(a, bb, c, u) ?: return null
    val vy = y[0] + a[0] / gamma * y[n - 1]
    val vz = z[0] + a[0] / gamma * z[n - 1]
    val denominator = Complex(1.0, 0.0) + vz
    if (denominator.sqrMagnitude() == 0.0) return null
    val factor = vy / denominator
    return Array(n) { y[it] - factor * z[it] }
}
