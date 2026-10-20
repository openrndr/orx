package org.openrndr.extra.shapes.reedsshepp

import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * How a piece of a Reeds-Shepp path steers: [LEFT] turns towards the normal (-y, x) of the heading at the minimum
 * radius, [RIGHT] the other way, [STRAIGHT] not at all. In a y-down frame, as on screen, LEFT turns clockwise when
 * driving forwards.
 */
enum class ReedsSheppSteering { LEFT, RIGHT, STRAIGHT }

/** A piece of a Reeds-Shepp path: a [steering] over a signed [length]; negative lengths drive in reverse. */
data class ReedsSheppSegment(val steering: ReedsSheppSteering, val length: Double)

/**
 * A Reeds-Shepp path: a path from [start] with the car heading in [startDirection], for a car that drives forwards and
 * in reverse and turns at most as sharply as a circle of [radius]. It consists of up to five [segments]; where the
 * sign of their lengths changes, the car changes gear and the path has a cusp.
 */
class ReedsSheppPath(
    val start: Vector2,
    val startDirection: Vector2,
    val radius: Double,
    val segments: List<ReedsSheppSegment>
) {
    /** The total length driven, forwards and in reverse. */
    val length: Double get() = segments.sumOf { abs(it.length) }

    /** The number of gear changes. */
    val cusps: Int
        get() = segments.filter { abs(it.length) > 0.0 }.zipWithNext().count { (a, b) -> (a.length > 0) != (b.length > 0) }

    /** The position and heading at the start of every segment, and at the end. */
    private val poses: List<Pair<Vector2, Vector2>> = run {
        val result = mutableListOf(Pair(start, startDirection.normalized))
        for (segment in segments) {
            val (p, h) = result.last()
            result.add(advance(p, h, segment.steering, segment.length))
        }
        result
    }

    /** The end point. */
    val end: Vector2 get() = poses.last().first

    /** The heading of the car at the end. */
    val endDirection: Vector2 get() = poses.last().second

    /** The pose after driving the signed [distance] from [p] with heading [h] and [steering]. */
    private fun advance(p: Vector2, h: Vector2, steering: ReedsSheppSteering, distance: Double): Pair<Vector2, Vector2> {
        if (steering == ReedsSheppSteering.STRAIGHT) return Pair(p + h * distance, h)
        val side = if (steering == ReedsSheppSteering.LEFT) 1.0 else -1.0
        val angle = side * distance / radius
        val center = p + Vector2(-h.y, h.x) * (side * radius)
        return Pair(center + rotate(p - center, angle), rotate(h, angle))
    }

    private fun locate(s: Double): Pair<Int, Double> {
        var remaining = s.coerceIn(0.0, length)
        for (i in 0 until segments.size - 1) {
            val l = abs(segments[i].length)
            if (remaining <= l) return Pair(i, remaining)
            remaining -= l
        }
        return Pair(segments.size - 1, remaining)
    }

    /** The position after driving a distance [s] along the path, forwards or in reverse. */
    fun position(s: Double): Vector2 {
        val (i, into) = locate(s)
        val (p, h) = poses[i]
        return advance(p, h, segments[i].steering, into * sign(segments[i].length)).first
    }

    /** The heading of the car after driving a distance [s] along the path; in reverse it points against the motion. */
    fun direction(s: Double): Vector2 {
        val (i, into) = locate(s)
        val (p, h) = poses[i]
        return advance(p, h, segments[i].steering, into * sign(segments[i].length)).second
    }

    /** Whether the car drives forwards at distance [s] along the path. */
    fun isForward(s: Double): Boolean = segments[locate(s).first].length >= 0.0

    /** Bézier segments of the pieces in [range] of segment indices. */
    private fun bezierSegments(range: IntRange): List<Segment2D> {
        val result = mutableListOf<Segment2D>()
        for (i in range) {
            val segment = segments[i]
            if (abs(segment.length) <= 1E-12 * maxOf(1.0, radius)) continue
            val (p, h) = poses[i]
            val (q, _) = poses[i + 1]
            val forward = sign(segment.length)
            if (segment.steering == ReedsSheppSteering.STRAIGHT) {
                result.add(Segment2D(p, q))
                continue
            }
            // the direction of motion is the heading, reversed when driving in reverse
            val angle = abs(segment.length) / radius
            val pieces = ceil(angle / (PI / 2) - 1E-9).toInt().coerceAtLeast(1)
            val step = angle / pieces
            val handle = 4.0 / 3.0 * tan(step / 4.0) * radius
            var p0 = p
            var m0 = h * forward
            for (k in 1..pieces) {
                val (p1, h1) = if (k == pieces) poses[i + 1] else advance(p, h, segment.steering, forward * step * k * radius)
                val m1 = h1 * forward
                result.add(Segment2D(p0, p0 + m0 * handle, p1 - m1 * handle, p1))
                p0 = p1
                m0 = m1
            }
        }
        return result
    }

    /**
     * The path as a single contour of lines and cubic Béziers, one per at most 90 degrees of arc. It is continuous, but
     * reverses direction at every [cusps] gear change.
     */
    fun contour(polarity: YPolarity = YPolarity.CW_NEGATIVE_Y): ShapeContour {
        val beziers = bezierSegments(segments.indices)
        return if (beziers.isEmpty()) ShapeContour.EMPTY else ShapeContour.fromSegments(beziers, false, polarity)
    }

    /**
     * The path split at its gear changes, with for every part whether it is driven forwards. Every part is
     * tangent-continuous.
     */
    fun gearContours(polarity: YPolarity = YPolarity.CW_NEGATIVE_Y): List<Pair<ShapeContour, Boolean>> {
        val result = mutableListOf<Pair<ShapeContour, Boolean>>()
        var first = -1
        for (i in segments.indices) {
            if (abs(segments[i].length) == 0.0) continue
            if (first < 0) first = i
            val next = (i + 1 until segments.size).firstOrNull { abs(segments[it].length) > 0.0 }
            if (next == null || (segments[next].length > 0) != (segments[i].length > 0)) {
                val beziers = bezierSegments(first..i)
                if (beziers.isNotEmpty()) {
                    result.add(Pair(ShapeContour.fromSegments(beziers, false, polarity), segments[i].length > 0))
                }
                first = -1
            }
        }
        return result
    }
}

private fun sign(x: Double) = if (x < 0.0) -1.0 else 1.0

private fun rotate(v: Vector2, angle: Double): Vector2 {
    val c = cos(angle)
    val s = sin(angle)
    return Vector2(v.x * c - v.y * s, v.x * s + v.y * c)
}

/** [x] wrapped to [-π, π]. */
private fun mod2pi(x: Double): Double {
    var v = x % (2 * PI)
    if (v < -PI) v += 2 * PI else if (v > PI) v -= 2 * PI
    return v
}

private const val ZERO = 1E-10

private class Words(val t: Double, val u: Double, val v: Double)

private fun polar(x: Double, y: Double) = Pair(sqrt(x * x + y * y), atan2(y, x))

private fun tauOmega(u: Double, v: Double, xi: Double, eta: Double, phi: Double): Pair<Double, Double> {
    val delta = mod2pi(u - v)
    val a = sin(u) - sin(delta)
    val b = cos(u) - cos(delta) - 1.0
    val t1 = atan2(eta * a - xi * b, xi * a + eta * b)
    val t2 = 2.0 * (cos(delta) - cos(v) - cos(u)) + 3.0
    val tau = if (t2 < 0) mod2pi(t1 + PI) else mod2pi(t1)
    return Pair(tau, mod2pi(tau - u + v - phi))
}

// The base formulas of Reeds and Shepp, "Optimal paths for a car that goes both forwards and backwards" (Pacific
// Journal of Mathematics, 1990), section 8, in the form of OMPL's ReedsSheppStateSpace, which corrects the typos in
// formulas 8.3 and 8.11. The pose is normalized: start at the origin heading along x, radius 1, end at (x, y) with
// heading phi. L+ is a left turn forwards, R- a right turn in reverse, S a straight segment.

/** 8.1, L+S+L+. */
private fun lpSpLp(x: Double, y: Double, phi: Double): Words? {
    val (u, t) = polar(x - sin(phi), y - 1.0 + cos(phi))
    if (t >= -ZERO) {
        val v = mod2pi(phi - t)
        if (v >= -ZERO) return Words(t, u, v)
    }
    return null
}

/** 8.2, L+S+R+. */
private fun lpSpRp(x: Double, y: Double, phi: Double): Words? {
    val (r, t1) = polar(x + sin(phi), y - 1.0 - cos(phi))
    val u1 = r * r
    if (u1 >= 4.0) {
        val u = sqrt(u1 - 4.0)
        val theta = atan2(2.0, u)
        val t = mod2pi(t1 + theta)
        val v = mod2pi(t - phi)
        if (t >= -ZERO && v >= -ZERO) return Words(t, u, v)
    }
    return null
}

/** 8.3 and 8.4, L+R-L. */
private fun lpRmL(x: Double, y: Double, phi: Double): Words? {
    val xi = x - sin(phi)
    val eta = y - 1.0 + cos(phi)
    val (u1, theta) = polar(xi, eta)
    if (u1 <= 4.0) {
        val u = -2.0 * asin(0.25 * u1)
        val t = mod2pi(theta + 0.5 * u + PI)
        val v = mod2pi(phi - t + u)
        if (t >= -ZERO && u <= ZERO) return Words(t, u, v)
    }
    return null
}

/** 8.7, L+R+L-R-. */
private fun lpRupLumRm(x: Double, y: Double, phi: Double): Words? {
    val xi = x + sin(phi)
    val eta = y - 1.0 - cos(phi)
    val rho = 0.25 * (2.0 + sqrt(xi * xi + eta * eta))
    if (rho <= 1.0) {
        val u = acos(rho)
        val (t, v) = tauOmega(u, -u, xi, eta, phi)
        if (t >= -ZERO && v <= ZERO) return Words(t, u, v)
    }
    return null
}

/** 8.8, L+R-L-R+. */
private fun lpRumLumRp(x: Double, y: Double, phi: Double): Words? {
    val xi = x + sin(phi)
    val eta = y - 1.0 - cos(phi)
    val rho = (20.0 - xi * xi - eta * eta) / 16.0
    if (rho in 0.0..1.0) {
        val u = -acos(rho)
        if (u >= -0.5 * PI) {
            val (t, v) = tauOmega(u, u, xi, eta, phi)
            if (t >= -ZERO && v >= -ZERO) return Words(t, u, v)
        }
    }
    return null
}

/** 8.9, L+R-(π/2)S-L-. */
private fun lpRmSmLm(x: Double, y: Double, phi: Double): Words? {
    val xi = x - sin(phi)
    val eta = y - 1.0 + cos(phi)
    val (rho, theta) = polar(xi, eta)
    if (rho >= 2.0) {
        val r = sqrt(rho * rho - 4.0)
        val u = 2.0 - r
        val t = mod2pi(theta + atan2(r, -2.0))
        val v = mod2pi(phi - 0.5 * PI - t)
        if (t >= -ZERO && u <= ZERO && v <= ZERO) return Words(t, u, v)
    }
    return null
}

/** 8.10, L+R-(π/2)S-R-. */
private fun lpRmSmRm(x: Double, y: Double, phi: Double): Words? {
    val xi = x + sin(phi)
    val eta = y - 1.0 - cos(phi)
    val (rho, theta) = polar(-eta, xi)
    if (rho >= 2.0) {
        val t = theta
        val u = 2.0 - rho
        val v = mod2pi(t + 0.5 * PI - phi)
        if (t >= -ZERO && u <= ZERO && v <= ZERO) return Words(t, u, v)
    }
    return null
}

/** 8.11, L+R-(π/2)S-L-(π/2)R+. */
private fun lpRmSLmRp(x: Double, y: Double, phi: Double): Words? {
    val xi = x + sin(phi)
    val eta = y - 1.0 - cos(phi)
    val (rho, _) = polar(xi, eta)
    if (rho >= 2.0) {
        val u = 4.0 - sqrt(rho * rho - 4.0)
        if (u <= ZERO) {
            val t = mod2pi(atan2((4.0 - u) * xi - 2.0 * eta, -2.0 * xi + (u - 4.0) * eta))
            val v = mod2pi(t - phi)
            if (t >= -ZERO && v >= -ZERO) return Words(t, u, v)
        }
    }
    return null
}

private val L = ReedsSheppSteering.LEFT
private val R = ReedsSheppSteering.RIGHT
private val S = ReedsSheppSteering.STRAIGHT

/** Steerings with their normalized signed lengths. */
private fun path(steerings: List<ReedsSheppSteering>, lengths: List<Double>) =
    steerings.zip(lengths).map { (s, l) -> ReedsSheppSegment(s, l) }

/**
 * All candidate paths for the normalized end pose (x, y, phi): every base formula for the pose itself, time-flipped
 * (driven in reverse: x and phi negated, all lengths negated), reflected (left and right swapped: y and phi negated)
 * and both, and for the formulas that aren't symmetric in time also for the pose driven backwards from the end.
 */
private fun candidates(x: Double, y: Double, phi: Double): List<List<ReedsSheppSegment>> {
    val result = mutableListOf<List<ReedsSheppSegment>>()
    val h = 0.5 * PI

    /** The base formula under timeflip and reflection, with [build] the segments for its words and a sign. */
    fun symmetric(formula: (Double, Double, Double) -> Words?, x: Double, y: Double, phi: Double,
                  build: (Words, Double, Boolean) -> List<ReedsSheppSegment>) {
        formula(x, y, phi)?.let { result.add(build(it, 1.0, false)) }
        formula(-x, y, -phi)?.let { result.add(build(it, -1.0, false)) }
        formula(x, -y, -phi)?.let { result.add(build(it, 1.0, true)) }
        formula(-x, -y, phi)?.let { result.add(build(it, -1.0, true)) }
    }

    fun steer(steerings: List<ReedsSheppSteering>, reflect: Boolean) =
        if (!reflect) steerings else steerings.map { if (it == L) R else if (it == R) L else S }

    // the pose driven backwards from the end
    val xb = x * cos(phi) + y * sin(phi)
    val yb = x * sin(phi) - y * cos(phi)

    // CSC
    symmetric(::lpSpLp, x, y, phi) { w, s, r -> path(steer(listOf(L, S, L), r), listOf(w.t, w.u, w.v).map { it * s }) }
    symmetric(::lpSpRp, x, y, phi) { w, s, r -> path(steer(listOf(L, S, R), r), listOf(w.t, w.u, w.v).map { it * s }) }
    // CCC
    symmetric(::lpRmL, x, y, phi) { w, s, r -> path(steer(listOf(L, R, L), r), listOf(w.t, w.u, w.v).map { it * s }) }
    symmetric(::lpRmL, xb, yb, phi) { w, s, r -> path(steer(listOf(L, R, L), r), listOf(w.v, w.u, w.t).map { it * s }) }
    // CCCC
    symmetric(::lpRupLumRm, x, y, phi) { w, s, r ->
        path(steer(listOf(L, R, L, R), r), listOf(w.t, w.u, -w.u, w.v).map { it * s })
    }
    symmetric(::lpRumLumRp, x, y, phi) { w, s, r ->
        path(steer(listOf(L, R, L, R), r), listOf(w.t, w.u, w.u, w.v).map { it * s })
    }
    // CCSC
    symmetric(::lpRmSmLm, x, y, phi) { w, s, r -> path(steer(listOf(L, R, S, L), r), listOf(w.t, -h, w.u, w.v).map { it * s }) }
    symmetric(::lpRmSmRm, x, y, phi) { w, s, r -> path(steer(listOf(L, R, S, R), r), listOf(w.t, -h, w.u, w.v).map { it * s }) }
    symmetric(::lpRmSmLm, xb, yb, phi) { w, s, r -> path(steer(listOf(L, S, R, L), r), listOf(w.v, w.u, -h, w.t).map { it * s }) }
    symmetric(::lpRmSmRm, xb, yb, phi) { w, s, r -> path(steer(listOf(R, S, R, L), r), listOf(w.v, w.u, -h, w.t).map { it * s }) }
    // CCSCC
    symmetric(::lpRmSLmRp, x, y, phi) { w, s, r ->
        path(steer(listOf(L, R, S, L, R), r), listOf(w.t, -h, w.u, -h, w.v).map { it * s })
    }
    return result
}

/**
 * All Reeds-Shepp candidate paths from the pose ([start], [startDirection]) to ([end], [endDirection]) with minimum
 * turning [radius], shortest first. The shortest is the optimal path; the others are the optimal paths of their types,
 * after Reeds and Shepp, "Optimal paths for a car that goes both forwards and backwards" (1990).
 */
fun reedsSheppPaths(
    start: Vector2,
    startDirection: Vector2,
    end: Vector2,
    endDirection: Vector2,
    radius: Double
): List<ReedsSheppPath> {
    require(radius > 0.0) { "the radius should be positive, got $radius" }
    val heading = startDirection.normalized
    val delta = end - start
    // the end pose in the frame of the start, in units of the radius
    val x = (heading.x * delta.x + heading.y * delta.y) / radius
    val y = (-heading.y * delta.x + heading.x * delta.y) / radius
    val phi = mod2pi(atan2(endDirection.y, endDirection.x) - atan2(heading.y, heading.x))
    return candidates(x, y, phi).map { segments ->
        ReedsSheppPath(start, heading, radius, segments.map { it.copy(length = it.length * radius) })
    }.sortedBy { it.length }
}

/**
 * The shortest Reeds-Shepp path from the pose ([start], [startDirection]) to ([end], [endDirection]): the shortest path
 * for a car that drives forwards and in reverse and turns at most as sharply as a circle of [radius]. Equal poses give
 * a path of length zero.
 */
fun reedsSheppPath(
    start: Vector2,
    startDirection: Vector2,
    end: Vector2,
    endDirection: Vector2,
    radius: Double
): ReedsSheppPath {
    if (start.squaredDistanceTo(end) <= 1E-24 &&
        startDirection.normalized.distanceTo(endDirection.normalized) <= 1E-12
    ) {
        return ReedsSheppPath(start, startDirection.normalized, radius, listOf(ReedsSheppSegment(ReedsSheppSteering.STRAIGHT, 0.0)))
    }
    return reedsSheppPaths(start, startDirection, end, endDirection, radius).first()
}
