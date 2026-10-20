package org.openrndr.extra.shapes.clothoid

import org.openrndr.extra.shapes.biarc.catmullRomTangents
import org.openrndr.extra.shapes.biarc.circleTangents
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.ShapeContour
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Converts this polyline into a clothoid spline: a contour of clothoid (Euler spiral) segments, along which the
 * curvature changes linearly with arc length, that passes through all points.
 *
 * When [tangents] is null the tangent direction at every point is solved for such that the curvature is continuous
 * where segments meet, which makes the contour G2: the tangent direction and the curvature never jump. The ends of an
 * open polyline get zero curvature, like the free ends of a bent elastic strip. The system is solved by Newton's
 * method, starting from Catmull-Rom tangents. Points on a circle give that circle and collinear points a straight line.
 *
 * When [tangents] are given every segment is the clothoid between its end points with those directions, which makes
 * the contour G1: curvature can jump at the points.
 *
 * Every segment is fitted after Bertolazzi and Frego, "G1 fitting with clothoids" (2015), and approximated with cubic
 * Béziers that turn by at most π/16 each.
 *
 * Some erratic point sets have no curvature-continuous solution that the solve can reach, such as some closed
 * figure-eights of four points; about 2 in 100 random point sets in the tests. The tangents with the smallest curvature
 * jumps found are then used, so the contour is still G1.
 *
 * @param closed whether the polyline is closed, the last point should then not repeat the first
 * @param tangents the tangent direction at every point, or null to solve for curvature continuity
 * @param polarity the polarity of the resulting contour
 */
fun List<Vector2>.clothoids(
    closed: Boolean,
    tangents: List<Vector2>? = null,
    polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): ShapeContour {
    require(tangents == null || tangents.size == size) {
        "expected $size tangents, got ${tangents?.size}"
    }
    val points = distinctPoints(closed)
    if (points.size < 2 || (closed && points.size < 3)) {
        return ShapeContour.fromPoints(points, closed, polarity)
    }
    val segments = clothoidFits(closed, tangents).mapIndexed { i, fit ->
        fit.segments(points[(i + 1) % points.size])
    }.flatten()
    return ShapeContour.fromSegments(segments, closed, polarity)
}

/** The indices of the points that don't repeat the next point. */
private fun List<Vector2>.distinctIndices(closed: Boolean) = indices.filter { i ->
    val next = if (i + 1 < size) i + 1 else if (closed) 0 else -1
    next == -1 || this[i].squaredDistanceTo(this[next]) > 1E-18
}

private fun List<Vector2>.distinctPoints(closed: Boolean) = distinctIndices(closed).map { this[it] }

/**
 * The clothoid segments of [clothoids], one per edge between the points that don't repeat the next point; empty when
 * there are too few of them.
 */
internal fun List<Vector2>.clothoidFits(closed: Boolean, tangents: List<Vector2>? = null): List<Clothoid> {
    val indices = distinctIndices(closed)
    val points = indices.map { this[it] }
    if (points.size < 2 || (closed && points.size < 3)) {
        return emptyList()
    }
    if (tangents == null) {
        return solveG2(points, closed)
    }
    val angles = indices.map { tangents[it].let { t -> atan2(t.y, t.x) } }
    val edges = if (closed) points.size else points.size - 1
    return List(edges) { i ->
        val j = (i + 1) % points.size
        fitClothoid(points[i], angles[i], points[j], angles[j])
    }
}

/**
 * The clothoid from [start] leaving in [startTangent] to [end] arriving in [endTangent], see [List.clothoids].
 *
 * @param polarity the polarity of the resulting contour
 */
fun clothoid(
    start: Vector2,
    startTangent: Vector2,
    end: Vector2,
    endTangent: Vector2,
    polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): ShapeContour {
    if (start.squaredDistanceTo(end) <= 1E-18) {
        return ShapeContour.fromPoints(listOf(start), false, polarity)
    }
    val segment = fitClothoid(start, atan2(startTangent.y, startTangent.x), end, atan2(endTangent.y, endTangent.x))
    return ShapeContour.fromSegments(segment.segments(end), false, polarity)
}

/**
 * The clothoids through [points] with tangent angles for which the curvature is continuous, with zero curvature at the
 * ends of an open polyline, solved in two phases.
 *
 * First the solve runs with segments that wind as little as possible, from the centripetal, uniform and chordal
 * Catmull-Rom tangents and the circle tangents in turn, until one converges. Such a segment changes abruptly where its
 * start or end direction points straight back along its chord, so a solution that needs a segment to wind further
 * can't be reached this way. The solve then stalls next to that point.
 *
 * Then it continues from where each first-phase attempt stalled, best first, with the angles of every segment relative
 * to its chord continuous instead of wrapped, so that segments can wind further. When neither phase converges the
 * first-phase result with the smallest curvature jumps is used.
 */
private fun solveG2(points: List<Vector2>, closed: Boolean): List<Clothoid> {
    val n = points.size
    val starts = listOf(
        catmullRomTangents(points, closed, 0.5), catmullRomTangents(points, closed, 0.0),
        catmullRomTangents(points, closed, 1.0), circleTangents(points, closed)
    )
    val stalled = mutableListOf<G2Solution>()
    for (start in starts) {
        val solution = solveG2From(points, closed, DoubleArray(n) { atan2(start[it].y, start[it].x) }, unwrapped = false)
        if (solution.converged) return solution.fits
        stalled.add(solution)
    }
    stalled.sortBy { it.norm }
    for (solution in stalled) {
        val unwrapped = solveG2From(points, closed, solution.angles, unwrapped = true)
        if (unwrapped.converged) return unwrapped.fits
    }
    return stalled.first().fits
}

/** The result of one Newton solve: the [fits], their tangent [angles] and the largest scaled curvature jump [norm]. */
private class G2Solution(val fits: List<Clothoid>, val angles: DoubleArray, val norm: Double) {
    val converged get() = norm < TOLERANCE
}

/** The largest curvature jump, times the mean chord length, that counts as continuous. */
private const val TOLERANCE = 1E-10

/**
 * One Newton solve for the tangent angles from [initial]. Every residual depends on the angles at a point and its
 * neighbours, so the Jacobian is tridiagonal, cyclic for closed polylines; its entries are found by central differences
 * per segment. Steps are halved until they lower the sum of squared residuals.
 *
 * When [unwrapped], the angles of every segment relative to its chord are wrapped to (-π, π] once, for [initial], and
 * are continuous in the tangent angles after that. Every fit then starts from the shape parameter of the previous fit
 * of that segment, which keeps it from jumping to a solution with a different winding.
 */
private fun solveG2From(points: List<Vector2>, closed: Boolean, initial: DoubleArray, unwrapped: Boolean): G2Solution {
    val n = points.size
    val edges = if (closed) n else n - 1
    val scale = (0 until edges).sumOf { points[it].distanceTo(points[(it + 1) % n]) } / edges
    val chordAngles = DoubleArray(edges) { i ->
        val d = points[(i + 1) % n] - points[i]
        atan2(d.y, d.x)
    }

    var angles = initial.copyOf()

    // multiples of 2π that wrap the initial relative angles, kept fixed from here on when unwrapped
    val startWinding = DoubleArray(edges) { i -> wrapOffset(angles[i] - chordAngles[i]) }
    val endWinding = DoubleArray(edges) { i -> wrapOffset(angles[(i + 1) % n] - chordAngles[i]) }

    fun fit(i: Int, startAngle: Double, endAngle: Double, previous: Clothoid?): Clothoid? {
        val j = (i + 1) % n
        if (!unwrapped) return fitClothoid(points[i], startAngle, points[j], endAngle)
        val guess = previous?.let { it.dkappa * it.length * it.length / 2.0 }
        return fitClothoidRelative(
            points[i], points[j],
            startAngle - chordAngles[i] + startWinding[i], endAngle - chordAngles[i] + endWinding[i],
            guess
        )
    }

    fun fits(angles: DoubleArray, previous: List<Clothoid>?): List<Clothoid>? {
        val result = ArrayList<Clothoid>(edges)
        for (i in 0 until edges) {
            result.add(fit(i, angles[i], angles[(i + 1) % n], previous?.get(i)) ?: return null)
        }
        return result
    }

    /** The curvature jumps at every point, in units of 1 / [scale]. */
    fun residuals(fits: List<Clothoid>) = DoubleArray(n) { j ->
        when {
            closed -> fits[(j - 1 + n) % n].endKappa - fits[j].kappa
            j == 0 -> fits[0].kappa
            j == n - 1 -> fits[n - 2].endKappa
            else -> fits[j - 1].endKappa - fits[j].kappa
        } * scale
    }

    fun norm(r: DoubleArray) = r.maxOf { abs(it) }
    fun squares(r: DoubleArray) = r.sumOf { it * it }

    // every fit with wrapped relative angles exists, so the initial fits do
    var current = fits(angles, null) ?: error("no clothoids for the initial tangents")
    var r = residuals(current)
    var best = current
    var bestAngles = angles
    var bestNorm = norm(r)

    for (iteration in 0 until 200) {
        if (bestNorm < TOLERANCE) break
        // derivatives of the start and end curvature of every segment to its start and end angle
        val h = 1E-6
        val dk0a = DoubleArray(edges); val dk0b = DoubleArray(edges)
        val dk1a = DoubleArray(edges); val dk1b = DoubleArray(edges)
        var differentiable = true
        for (i in 0 until edges) {
            val a = angles[i]
            val b = angles[(i + 1) % n]
            val base = current[i]
            val pa = fit(i, a + h, b, base); val ma = fit(i, a - h, b, base)
            val pb = fit(i, a, b + h, base); val mb = fit(i, a, b - h, base)
            if (pa == null || ma == null || pb == null || mb == null) {
                differentiable = false
                break
            }
            dk0a[i] = (pa.kappa - ma.kappa) / (2 * h) * scale
            dk1a[i] = (pa.endKappa - ma.endKappa) / (2 * h) * scale
            dk0b[i] = (pb.kappa - mb.kappa) / (2 * h) * scale
            dk1b[i] = (pb.endKappa - mb.endKappa) / (2 * h) * scale
        }
        if (!differentiable) break
        // row j: coefficients of the angles at j - 1 (sub), j (diagonal) and j + 1 (super)
        val sub = DoubleArray(n); val diagonal = DoubleArray(n); val sup = DoubleArray(n)
        for (j in 0 until n) {
            val incoming = if (closed) (j - 1 + n) % n else j - 1
            val outgoing = if (closed || j < n - 1) j else -1
            if (incoming >= 0) {
                sub[j] = dk1a[incoming]
                diagonal[j] += dk1b[incoming]
            }
            if (outgoing >= 0) {
                diagonal[j] -= dk0a[outgoing]
                sup[j] = -dk0b[outgoing]
            }
        }
        if (!closed) {
            // the end rows are κ0 of the first and κ1 of the last segment, not differences
            diagonal[0] = dk0a[0]; sup[0] = dk0b[0]
        }
        val rhs = DoubleArray(n) { -r[it] }
        val step = (if (closed) solveCyclicTridiagonal(sub, diagonal, sup, rhs) else solveTridiagonal(sub, diagonal, sup, rhs))
            ?: break
        var factor = 1.0
        var improved = false
        while (factor > 1E-6) {
            val candidate = DoubleArray(n) { angles[it] + factor * step[it] }
            val candidateFits = fits(candidate, current)
            if (candidateFits != null) {
                val candidateResiduals = residuals(candidateFits)
                if (squares(candidateResiduals) < squares(r)) {
                    angles = candidate
                    current = candidateFits
                    r = candidateResiduals
                    improved = true
                    break
                }
            }
            factor /= 2.0
        }
        if (!improved) break
        if (norm(r) < bestNorm) {
            best = current
            bestAngles = angles
            bestNorm = norm(r)
        }
    }
    return G2Solution(best, bestAngles, bestNorm)
}

/**
 * Solves the tridiagonal system with sub-diagonal [a] (a[0] unused), diagonal [b] and super-diagonal [c] (c[n-1]
 * unused) for [d] by Gaussian elimination with partial pivoting, as LAPACK's dgtsv. Returns null when it is singular.
 */
internal fun solveTridiagonal(a: DoubleArray, b: DoubleArray, c: DoubleArray, d: DoubleArray): DoubleArray? {
    val n = b.size
    val dl = a.copyOf(); val dd = b.copyOf(); val du = c.copyOf(); val du2 = DoubleArray(n); val x = d.copyOf()
    for (i in 0 until n - 1) {
        if (abs(dd[i]) >= abs(dl[i + 1])) {
            if (dd[i] == 0.0) return null
            val f = dl[i + 1] / dd[i]
            dd[i + 1] -= f * du[i]
            x[i + 1] -= f * x[i]
        } else {
            val f = dd[i] / dl[i + 1]
            dd[i] = dl[i + 1]
            val temp = dd[i + 1]
            dd[i + 1] = du[i] - f * temp
            if (i < n - 2) {
                du2[i] = du[i + 1]
                du[i + 1] = -f * du2[i]
            }
            du[i] = temp
            val tx = x[i]
            x[i] = x[i + 1]
            x[i + 1] = tx - f * x[i + 1]
        }
    }
    if (dd[n - 1] == 0.0) return null
    x[n - 1] /= dd[n - 1]
    if (n > 1) x[n - 2] = (x[n - 2] - du[n - 2] * x[n - 1]) / dd[n - 2]
    for (i in n - 3 downTo 0) {
        x[i] = (x[i] - du[i] * x[i + 1] - du2[i] * x[i + 2]) / dd[i]
    }
    return if (x.all { it.isFinite() }) x else null
}

/**
 * Solves the cyclic tridiagonal system, in which a[0] couples the first row to the last unknown and c[n-1] the last
 * row to the first, with the Sherman-Morrison formula on top of [solveTridiagonal].
 */
internal fun solveCyclicTridiagonal(a: DoubleArray, b: DoubleArray, c: DoubleArray, d: DoubleArray): DoubleArray? {
    val n = b.size
    val gamma = if (b[0] != 0.0) -b[0] else 1.0
    val bb = b.copyOf()
    bb[0] = b[0] - gamma
    bb[n - 1] = b[n - 1] - a[0] * c[n - 1] / gamma
    val u = DoubleArray(n).also { it[0] = gamma; it[n - 1] = c[n - 1] }
    val y = solveTridiagonal(a, bb, c, d) ?: return null
    val z = solveTridiagonal(a, bb, c, u) ?: return null
    val vy = y[0] + a[0] / gamma * y[n - 1]
    val vz = z[0] + a[0] / gamma * z[n - 1]
    if (1.0 + vz == 0.0) return null
    val factor = vy / (1.0 + vz)
    val x = DoubleArray(n) { y[it] - factor * z[it] }
    return if (x.all { it.isFinite() }) x else null
}
