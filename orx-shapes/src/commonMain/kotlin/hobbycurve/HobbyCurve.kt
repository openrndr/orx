package org.openrndr.extra.shapes.hobbycurve
// Code adapted from http://weitz.de/hobby/

import org.openrndr.math.*
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.*
import kotlin.math.*

/**
 * Replaces this contour by a Hobby curve through its vertices, see [hobbyCurve].
 *
 * @param curl the curl at the ends of an open contour
 * @param tension the METAFONT tension at every point, at least 3/4; higher values give tighter curves
 * @param atLeast whether the tension is a minimum that keeps every segment inside the triangle of its chord and end
 * tangents, as METAFONT's `tension atleast`
 */
fun ShapeContour.hobbyCurve(curl: Double = 0.0, tension: Double = 1.0, atLeast: Boolean = false): ShapeContour {
    val vertices = if (closed)
        segments.map { it.start }
    else
        segments.map { it.start } + segments.last().end
    return hobbyCurve(vertices, closed, curl, tension, atLeast)
}

/** Replaces every contour of this shape by a Hobby curve through its vertices, see [ShapeContour.hobbyCurve]. */
fun Shape.hobbyCurve(curl: Double = 0.0, tension: Double = 1.0, atLeast: Boolean = false): Shape {
    return Shape(contours.map {
        it.hobbyCurve(curl, tension, atLeast)
    })
}

/**
 * The METAFONT tensions of a segment of a Hobby curve, see [hobbyCurve].
 *
 * @param start the tension where the segment leaves its start point, at least 3/4
 * @param end the tension where the segment arrives at its end point, at least 3/4
 * @param atLeast whether the tensions are minimums that keep the segment inside the triangle of its chord and end
 * tangents, as METAFONT's `tension atleast`
 */
data class HobbyTension(val start: Double = 1.0, val end: Double = 1.0, val atLeast: Boolean = false) {
    init {
        require(start >= 0.75 && end >= 0.75) { "tensions should be at least 3/4, got $start and $end" }
    }
}

private fun Vector2.atan22(other: Vector2): Double {
    val u = this.normalized
    val v = other.normalized
    val x = u.cross(v)
    val y = u.dot(v)
    return atan2(x, y)
}

private fun Vector3.atan22(other: Vector3): Double {
    val u = this.normalized
    val v = other.normalized
    val x = u.cross(v).length
    val y = u.dot(v)
    return atan2(x, y)
}


/**
 * Generates a smooth contour passing through a set of points based on Hobby's algorithm.
 *
 * @param points A list of 2D points through which the curve should pass.
 * @param closed A boolean value indicating whether the curve is closed (true)
 *               forming a loop, or open (false). The default is false.
 * @param curl A parameter that controls the curvature of the open-ended curve. Only
 *             applicable if the curve is not closed. The default is 0.0.
 * @param tensions A lambda function that accepts the chord index (an integer) and returns
 *                 a pair of factors (Double) for the lengths of the curve's control handles,
 *                 applied after the curve has been solved. Larger values give rounder segments.
 *                 These are not METAFONT tensions, which enter the solve itself and make the
 *                 curve tighter as they grow; see the overloads that take a tension for those.
 *                 The default lambda assigns both values as 1.0.
 * @return A ShapeContour object representing the smoothed curve based on Hobby's algorithm.
 */
fun hobbyCurve(
    points: List<Vector2>, closed: Boolean = false, curl: Double = 0.0,
    tensions: (chordIndex: Int, inAngleDegrees:Double, outAngleDegrees:Double) -> Pair<Double, Double> = { _, _, _ -> Pair(1.0, 1.0) },
): ShapeContour = hobbyCurve2D(points, closed, curl, null, tensions)

/**
 * Generates a Hobby curve through [points] with a METAFONT tension, after Hobby, "Smooth, easy to compute
 * interpolating splines" (Discrete & Computational Geometry, 1986) and Knuth, "METAFONT: The Program" (§276-§299).
 *
 * The tension enters the equations for the directions at the points, not only the lengths of the control handles:
 * with reciprocal tensions α where a segment leaves a point and β where it arrives, the mock curvature at an interior
 * point is continuous where
 * A·θᵢ₋₁ + (B + C)·θᵢ + D·θᵢ₊₁ = -B·ψᵢ - D·ψᵢ₊₁, with
 * A = αᵢ₋₁ / (βᵢ²·dᵢ₋₁), B = (3 - αᵢ₋₁) / (βᵢ²·dᵢ₋₁), C = (3 - βᵢ₊₁) / (αᵢ²·dᵢ) and D = βᵢ₊₁ / (αᵢ²·dᵢ),
 * for the directions θ relative to the chords, the turning angles ψ and the chord lengths d. The ends of an open curve
 * follow from the curl through METAFONT's curl ratio. The control handles have length ρ(θ, φ) / (3τ) times the chord.
 * Tension 1 is Hobby's original curve, higher tensions give tighter curves that approach the polyline, and tensions
 * below 1, down to 3/4, rounder ones.
 *
 * With [atLeast] the tension is a minimum, as METAFONT's `tension atleast`: where the tangents at both ends of a
 * segment lie on the same side of its chord, its handles are shortened so that the segment stays inside the triangle
 * of the chord and the two tangent lines.
 *
 * @param points the points through which the curve passes
 * @param closed whether the curve is closed
 * @param curl the curl at the ends of an open curve: 0 makes the end segments straighten out, 1 makes them nearly
 * circular arcs
 * @param tension the tension at every point, at least 3/4
 * @param atLeast whether the tension is a minimum that keeps every segment inside the triangle of its chord and end
 * tangents
 */
fun hobbyCurve(
    points: List<Vector2>,
    closed: Boolean = false,
    curl: Double = 0.0,
    tension: Double,
    atLeast: Boolean = false
): ShapeContour {
    val n = if (closed) points.size else points.size - 1
    return hobbyCurve(points, closed, curl, List(maxOf(n, 0)) { HobbyTension(tension, tension, atLeast) })
}

/**
 * Generates a Hobby curve through [points] with METAFONT tensions per segment, see the overload that takes a single
 * tension.
 *
 * @param segmentTensions the tensions of every segment, one per segment: as many as there are points for a closed
 * curve, one fewer for an open one
 */
fun hobbyCurve(
    points: List<Vector2>,
    closed: Boolean = false,
    curl: Double = 0.0,
    segmentTensions: List<HobbyTension>
): ShapeContour {
    val n = if (closed) points.size else points.size - 1
    require(points.size <= 1 || segmentTensions.size == n) { "expected $n segment tensions, got ${segmentTensions.size}" }
    return hobbyCurve2D(points, closed, curl, segmentTensions)
}

/** The 2D Hobby curve with METAFONT [segmentTensions], all 1 when null, and handle length [factors]. */
private fun hobbyCurve2D(
    points: List<Vector2>,
    closed: Boolean,
    curl: Double,
    segmentTensions: List<HobbyTension>?,
    factors: (chordIndex: Int, inAngleDegrees: Double, outAngleDegrees: Double) -> Pair<Double, Double> = { _, _, _ -> Pair(1.0, 1.0) },
): ShapeContour {
    if (points.size <= 1) return ShapeContour.EMPTY

    val m = points.size

    /** Chord count */
    val n = if (closed) m else m - 1

    /** Chords array stores vectors representing line segments between consecutive points
    Each chord is calculated as the vector difference between the next point and current point */
    val chords = Array(n) { points[(it + 1) % m] - points[it] }
    val distances = Array(n) { chords[it].length }

    require(distances.all { it > 0.0 })

    // two points: the only curve through them without a turn is the straight segment
    if (!closed && m == 2) return ShapeContour.fromPoints(points, false)

    /** Array storing turning angles (in radians) between adjacent chords at each point
    For each point i, gamma[i] represents the angle between chord[i-1] and chord[i] */
    val gamma = DoubleArray(m)
    for (i in (if (closed) 0 else 1) until n) {
        gamma[i] = chords[(i - 1).mod(m)].atan22(chords[(i).mod(m)])
    }
    if (!closed) gamma[n] = 0.0

    val tensions = segmentTensions ?: List(n) { HobbyTension() }
    val (alpha, beta) = solveAngles(gamma, distances, closed, curl, tensions)

    val c1s = ArrayList<Vector2>(n)
    val c2s = ArrayList<Vector2>(n)
    for (i in 0 until n) {
        val v1 = rotateAngle(chords[i], alpha[i]).normalized
        val v2 = rotateAngle(chords[i], -beta[i]).normalized
        val t = factors(i, gamma[i].asDegrees, gamma[(i + 1).mod(m)].asDegrees)
        val (startVelocity, endVelocity) = velocities(alpha[i], beta[i], tensions[i])
        c1s.add(points[i % m] + v1 * startVelocity * t.first * distances[i])
        c2s.add(points[(i + 1) % m] - v2 * endVelocity * t.second * distances[i])
    }
    return ShapeContour(List(n) { Segment2D(points[it], c1s[it], c2s[it], points[(it + 1) % m]) }, closed = closed)
}

/**
 * Uses Hobby's algorithm to construct a [Path3D] through a given list of points.
 * @param points The list of points through which the curve should go.
 * @param closed Whether to construct a closed or open curve.
 * @param curl The 'curl' at the endpoints of the curve; this is only applicable when [closed] is false. Best results for values in [-1, 1], where a higher value makes segments closer to circular arcs.
 * @param tensions A function that returns factors for the lengths of the in and out control handles given a chord
 * index, applied after the curve has been solved; these are not METAFONT tensions, see the overloads that take those.
 * @return A [Path3D] through [points].
 */
fun hobbyCurve(
    points: List<Vector3>,
    closed: Boolean = false,
    curl: Double = 0.0,
    tensions: (chordIndex: Int, inAngleDegrees:Double, outAngleDegrees:Double) -> Pair<Double, Double> = { _, _, _ -> Pair(1.0, 1.0) },
): Path3D = hobbyCurve3D(points, closed, curl, null, tensions)

/** A 3D Hobby curve through [points] with a METAFONT tension, see the 2D overload that takes a single tension. */
fun hobbyCurve(
    points: List<Vector3>,
    closed: Boolean = false,
    curl: Double = 0.0,
    tension: Double,
    atLeast: Boolean = false
): Path3D {
    val n = if (closed) points.size else points.size - 1
    return hobbyCurve(points, closed, curl, List(maxOf(n, 0)) { HobbyTension(tension, tension, atLeast) })
}

/** A 3D Hobby curve through [points] with METAFONT tensions per segment, see the 2D overload. */
fun hobbyCurve(
    points: List<Vector3>,
    closed: Boolean = false,
    curl: Double = 0.0,
    segmentTensions: List<HobbyTension>
): Path3D {
    val n = if (closed) points.size else points.size - 1
    require(points.size <= 1 || segmentTensions.size == n) { "expected $n segment tensions, got ${segmentTensions.size}" }
    return hobbyCurve3D(points, closed, curl, segmentTensions)
}

private fun hobbyCurve3D(
    points: List<Vector3>,
    closed: Boolean,
    curl: Double,
    segmentTensions: List<HobbyTension>?,
    factors: (chordIndex: Int, inAngleDegrees: Double, outAngleDegrees: Double) -> Pair<Double, Double> = { _, _, _ -> Pair(1.0, 1.0) },
): Path3D {
    if (points.size <= 1) return Path3D.EMPTY

    val m = points.size
    val n = if (closed) m else m - 1

    val chords = Array(n) { points[(it + 1) % m] - points[it] }
    val distances = Array(n) { chords[it].length }
    // the normal of the plane in which the curve turns, per point
    val normals = Array(m) { Vector3.ZERO }

    require(distances.all { it > 0.0 })

    if (!closed && m == 2) return Path3D.fromPoints(points, false)

    val gamma = DoubleArray(m)
    for (i in (if (closed) 0 else 1) until n) {
        val zc = chords[i].normalized
        val zp = chords[(i - 1).mod(m)].normalized
        val normal = zc.cross(-zp)
        gamma[i] = zp.atan22(zc) * sign(normal.z)
        normals[i] = normal * sign(normal.z)
    }
    if (!closed) {
        gamma[n] = 0.0
        // the ends of an open curve turn in the plane of their neighbouring point
        normals[0] = normals[1]
        normals[n] = normals[n - 1]
    }

    val tensions = segmentTensions ?: List(n) { HobbyTension() }
    val (alpha, beta) = solveAngles(gamma, distances, closed, curl, tensions)

    val c1s = ArrayList<Vector3>(n)
    val c2s = ArrayList<Vector3>(n)
    for (i in 0 until n) {
        val r1 = buildTransform { rotate(normals[i], alpha[i].asDegrees) }
        val r2 = buildTransform { rotate(normals[(i + 1) % m], -beta[i].asDegrees) }
        val v1 = (r1 * chords[i].xyz0).xyz.normalized
        val v2 = (r2 * chords[i].xyz0).xyz.normalized
        val t = factors(i, gamma[i].asDegrees, gamma[(i + 1).mod(m)].asDegrees)
        val (startVelocity, endVelocity) = velocities(alpha[i], beta[i], tensions[i])
        c1s.add(points[i % m] + v1 * startVelocity * distances[i] * t.first)
        c2s.add(points[(i + 1) % m] - v2 * endVelocity * distances[i] * t.second)
    }

    return Path3D(List(n) {
        Segment3D(points[it], c1s[it], c2s[it], points[(it + 1) % m])
    }, closed = closed)
}

/**
 * Solves for the directions of the curve relative to the chords: θ where every segment leaves its start point (alpha)
 * and φ where it arrives at its end point (beta), from the turning angles [gamma] and the chord lengths [distances].
 * See the 2D overload of [hobbyCurve] that takes a tension for the equations.
 */
private fun solveAngles(
    gamma: DoubleArray,
    distances: Array<Double>,
    closed: Boolean,
    curl: Double,
    tensions: List<HobbyTension>
): Pair<DoubleArray, DoubleArray> {
    val m = gamma.size
    val n = distances.size
    val a = DoubleArray(m) { 0.0 }
    val b = DoubleArray(m) { 0.0 }
    val c = DoubleArray(m) { 0.0 }
    val d = DoubleArray(m) { 0.0 }

    for (i in (if (closed) 0 else 1) until n) {
        val prev = (i - 1).mod(m)
        val next = (i + 1).mod(m)
        // reciprocal tensions: leaving the previous point, arriving here, leaving here, arriving at the next point
        val alphaPrevious = 1.0 / tensions[prev].start
        val betaHere = 1.0 / tensions[prev].end
        val alphaHere = 1.0 / tensions[i].start
        val betaNext = 1.0 / tensions[i].end
        val aa = alphaPrevious / (betaHere * betaHere * distances[prev])
        val bb = (3.0 - alphaPrevious) / (betaHere * betaHere * distances[prev])
        val cc = (3.0 - betaNext) / (alphaHere * alphaHere * distances[i])
        val dd = betaNext / (alphaHere * alphaHere * distances[i])
        a[i] = aa
        b[i] = bb + cc
        c[i] = dd
        d[i] = -bb * gamma[i] - dd * gamma[next]
    }

    if (!closed) {
        // the direction at an end is the curl ratio times the direction at the far end of its segment
        val startRatio = curlRatio(curl, tensions[0].start, tensions[0].end)
        a[0] = 0.0
        b[0] = 1.0
        c[0] = startRatio
        d[0] = -startRatio * gamma[1]

        val endRatio = curlRatio(curl, tensions[n - 1].end, tensions[n - 1].start)
        a[n] = endRatio
        b[n] = 1.0
        c[n] = 0.0
        d[n] = 0.0

        val alpha = thomas(a, b, c, d)
        val beta = DoubleArray(n) { 0.0 }
        for (i in 0 until n - 1) {
            beta[i] = -gamma[i + 1] - alpha[i + 1]
        }
        beta[n - 1] = -alpha[n]
        return Pair(alpha, beta)
    } else {
        val s = a[0]
        a[0] = 0.0
        val t = c[n - 1]
        c[n - 1] = 0.0
        val alpha = sherman(a, b, c, d, s, t)
        val beta = DoubleArray(n)
        for (i in 0 until n) {
            val j = (i + 1) % n
            beta[i] = -gamma[j] - alpha[j]
        }
        return Pair(alpha, beta)
    }
}

/**
 * METAFONT's curl ratio: the factor between the direction at an end of an open curve and at the far end of its
 * segment, for the [curl] and the tensions at the end ([endTension]) and the far end ([farTension]), at most 4.
 */
private fun curlRatio(curl: Double, endTension: Double, farTension: Double): Double {
    val alpha = 1.0 / endTension
    val beta = 1.0 / farTension
    val numerator = (3.0 - alpha) * alpha * alpha * curl + beta * beta * beta
    val denominator = alpha * alpha * alpha * curl + (3.0 - beta) * beta * beta
    return if (numerator >= 4.0 * denominator) 4.0 else numerator / denominator
}

/**
 * The lengths of the control handles at the start and end of a segment, relative to its chord, for the directions
 * [theta] and [phi]: ρ / (3τ) for the [tension]. With `atLeast`, where both directions lie on the same side of the
 * chord, they are limited to keep the control points inside the triangle of the chord and the two tangent lines,
 * whose sides are sin φ / sin(θ + φ) and sin θ / sin(θ + φ) by the law of sines.
 */
private fun velocities(theta: Double, phi: Double, tension: HobbyTension): Pair<Double, Double> {
    var start = rho(theta, phi) / (3.0 * tension.start)
    var end = rho(phi, theta) / (3.0 * tension.end)
    if (tension.atLeast) {
        val st = sin(theta)
        val sf = sin(phi)
        if ((st >= 0.0 && sf >= 0.0) || (st <= 0.0 && sf <= 0.0)) {
            val sine = abs(st) * cos(phi) + abs(sf) * cos(theta)
            if (sine > 0.0) {
                start = min(start, abs(sf) / sine)
                end = min(end, abs(st) / sine)
            }
        }
    }
    return Pair(start, end)
}

/**
 * Solves a tridiagonal system of equations using the Thomas algorithm.
 * [Wikipedia](https://en.wikipedia.org/wiki/Tridiagonal_matrix_algorithm)
 * @param a The subdiagonal elements of the tridiagonal matrix.
 * @param b The diagonal elements of the tridiagonal matrix.
 * @param c The superdiagonal elements of the tridiagonal matrix.
 * @param d The right-hand side vector of the system.
 * @return A double array representing the solution to the tridiagonal system.
 */
private fun thomas(a: DoubleArray, b: DoubleArray, c: DoubleArray, d: DoubleArray): DoubleArray {
    val n = a.size
    val cc = DoubleArray(n) { 0.0 }
    val dd = DoubleArray(n) { 0.0 }
    cc[0] = c[0] / b[0]
    dd[0] = d[0] / b[0]
    for (i in 1 until n) {
        val den = b[i] - cc[i - 1] * a[i]
        cc[i] = c[i] / den
        dd[i] = (d[i] - dd[i - 1] * a[i]) / den
    }
    val x = DoubleArray(n) { 0.0 }
    x[n - 1] = dd[n - 1]
    for (i in n - 2 downTo 0) {
        x[i] = dd[i] - cc[i] * x[i + 1]
    }
    return x
}

/**
 * Uses the Sherman-Morrison formula to solve a system of linear equations with a modified
 * tridiagonal matrix, based on the Thomas algorithm.
 *
 * @param a The lower diagonal coefficients of the tridiagonal matrix.
 * @param b The main diagonal coefficients of the tridiagonal matrix.
 * @param c The upper diagonal coefficients of the tridiagonal matrix.
 * @param d The right-hand side vector of the system of equations.
 * @param s The value for the last row modification in the matrix.
 * @param t The value for the first row modification in the matrix.
 * @return A solution vector of the system of equations considering the specified matrix modifications.
 */
private fun sherman(
    a: DoubleArray,
    b: DoubleArray,
    c: DoubleArray,
    d: DoubleArray,
    s: Double,
    t: Double
): DoubleArray {
    val n = a.size
    val u = DoubleArray(n) { if (it == 0 || it == n - 1) 1.0 else 0.0 }
    val v = DoubleArray(n) {
        when (it) {
            0 -> t; n - 1 -> s; else -> 0.0
        }
    }
    b[0] -= t
    b[n - 1] -= s
    val Td = thomas(a, b, c, d)
    val Tu = thomas(a, b, c, u)
    val factor = (t * Td[0] + s * Td[n - 1]) / (1 + t * Tu[0] + s * Tu[n - 1])
    return DoubleArray(n) {
        Td[it] - factor * Tu[it]
    }
}

/**
 * Calculates a ratio used in Hobby's curve construction based on the angles between curve segments.
 *
 * @param a The angle (in radians) at the current point on the curve.
 * @param b The angle (in radians) at the neighboring point on the curve.
 * @return A calculated ratio used to control the curve's shape.
 */
private fun rho(a: Double, b: Double): Double {
    val sa = sin(a)
    val sb = sin(b)
    val ca = cos(a)
    val cb = cos(b)
    val s5 = sqrt(5.0)
    val num = 4 + sqrt(8.0) * (sa - sb / 16) * (sb - sa / 16) * (ca - cb)
    val den = 2 + (s5 - 1) * ca + (3 - s5) * cb
    return num / den
}

private fun rotate(v: Vector2, s: Double, c: Double) = Vector2(v.x * c - v.y * s, v.x * s + v.y * c)
private fun rotateAngle(v: Vector2, alpha: Double) = rotate(v, sin(alpha), cos(alpha))