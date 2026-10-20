package org.openrndr.extra.shapes.dubins

import org.openrndr.extra.shapes.biarc.catmullRomTangents
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * The six candidate shapes of a Dubins path: L a turn towards the normal (-y, x) of the direction of travel, R a turn
 * the other way, both at the minimum radius, and S a straight segment. In a y-down frame, as on screen, L turns
 * clockwise.
 */
enum class DubinsWord(val turns: List<Int>) {
    LSL(listOf(1, 0, 1)),
    RSR(listOf(-1, 0, -1)),
    LSR(listOf(1, 0, -1)),
    RSL(listOf(-1, 0, 1)),
    RLR(listOf(-1, 1, -1)),
    LRL(listOf(1, -1, 1))
}

/**
 * A Dubins path: the shortest path from [start], leaving in [startDirection], to an end pose, for a vehicle that only
 * moves forwards and turns at most as sharply as a circle of [radius]. It consists of three pieces after its [word], of
 * lengths [lengths]; an arc of length l turns by l / [radius].
 */
class DubinsPath(
    val start: Vector2,
    val startDirection: Vector2,
    val radius: Double,
    val word: DubinsWord,
    val lengths: List<Double>
) {
    /** The total length. */
    val length: Double get() = lengths.sum()

    /** The position and unit direction at the start of each of the three pieces, and at the end. */
    private val poses: List<Pair<Vector2, Vector2>> = run {
        val result = mutableListOf(Pair(start, startDirection.normalized))
        for (i in 0 until 3) {
            val (p, d) = result.last()
            result.add(advance(p, d, word.turns[i], lengths[i]))
        }
        result
    }

    /** The end point. */
    val end: Vector2 get() = poses[3].first

    /** The unit direction at the end. */
    val endDirection: Vector2 get() = poses[3].second

    /** The pose after moving [distance] from [p] in direction [d] along a piece that turns by [turn]. */
    private fun advance(p: Vector2, d: Vector2, turn: Int, distance: Double): Pair<Vector2, Vector2> {
        if (turn == 0) return Pair(p + d * distance, d)
        val angle = turn * distance / radius
        val normal = Vector2(-d.y, d.x) * turn.toDouble()
        val center = p + normal * radius
        return Pair(center + rotate(p - center, angle), rotate(d, angle))
    }

    /** The piece that arc length [s] lies in and the arc length into it. */
    private fun locate(s: Double): Pair<Int, Double> {
        var remaining = s.coerceIn(0.0, length)
        for (i in 0 until 2) {
            if (remaining <= lengths[i]) return Pair(i, remaining)
            remaining -= lengths[i]
        }
        return Pair(2, remaining)
    }

    /** The position at arc length [s] from the start. */
    fun position(s: Double): Vector2 {
        val (i, into) = locate(s)
        val (p, d) = poses[i]
        return advance(p, d, word.turns[i], into).first
    }

    /** The unit direction at arc length [s] from the start. */
    fun direction(s: Double): Vector2 {
        val (i, into) = locate(s)
        val (p, d) = poses[i]
        return advance(p, d, word.turns[i], into).second
    }

    /** Cubic Bézier segments of the path, a line per straight piece and one Bézier per at most 90 degrees of arc. */
    fun segments(): List<Segment2D> {
        val result = mutableListOf<Segment2D>()
        for (i in 0 until 3) {
            if (lengths[i] <= 1E-12 * maxOf(1.0, radius)) continue
            val (p, d) = poses[i]
            val (q, _) = poses[i + 1]
            val turn = word.turns[i]
            if (turn == 0) {
                result.add(Segment2D(p, q))
                continue
            }
            val angle = lengths[i] / radius
            val pieces = ceil(angle / (PI / 2) - 1E-9).toInt().coerceAtLeast(1)
            val step = angle / pieces
            val handle = 4.0 / 3.0 * tan(step / 4.0) * radius
            var p0 = p
            var d0 = d
            for (k in 1..pieces) {
                val (p1, d1) = if (k == pieces) poses[i + 1] else advance(p, d, turn, step * k * radius)
                result.add(Segment2D(p0, p0 + d0 * handle, p1 - d1 * handle, p1))
                p0 = p1
                d0 = d1
            }
        }
        return result
    }

    /** The path as a contour, see [segments]. */
    fun contour(polarity: YPolarity = YPolarity.CW_NEGATIVE_Y): ShapeContour =
        if (length <= 0.0) ShapeContour.EMPTY else ShapeContour.fromSegments(segments(), false, polarity)
}

private fun rotate(v: Vector2, angle: Double): Vector2 {
    val c = cos(angle)
    val s = sin(angle)
    return Vector2(v.x * c - v.y * s, v.x * s + v.y * c)
}

private fun mod2pi(x: Double) = x - 2 * PI * floor(x / (2 * PI))

/**
 * All Dubins paths from the pose ([start], [startDirection]) to ([end], [endDirection]) with minimum turning [radius],
 * shortest first, one for every word that has a solution, after Shkel and Lumelsky, "Classification of the Dubins set"
 * (Robotics and Autonomous Systems, 2001). The poses are normalized so that the start lies at the origin, the end on
 * the positive x-axis at distance d in units of the radius, with the directions α and β relative to that axis; every
 * word then gives the three normalized piece lengths in closed form.
 */
fun dubinsPaths(
    start: Vector2,
    startDirection: Vector2,
    end: Vector2,
    endDirection: Vector2,
    radius: Double
): List<DubinsPath> {
    require(radius > 0.0) { "the radius should be positive, got $radius" }
    val delta = end - start
    val d = delta.length / radius
    val theta = if (d > 0.0) atan2(delta.y, delta.x) else 0.0
    val a = mod2pi(atan2(startDirection.y, startDirection.x) - theta)
    val b = mod2pi(atan2(endDirection.y, endDirection.x) - theta)
    val sa = sin(a)
    val sb = sin(b)
    val ca = cos(a)
    val cb = cos(b)
    val cab = cos(a - b)

    fun lsl(): List<Double>? {
        val p2 = 2 + d * d - 2 * cab + 2 * d * (sa - sb)
        if (p2 < 0) return null
        val tmp = atan2(cb - ca, d + sa - sb)
        return listOf(mod2pi(-a + tmp), sqrt(p2), mod2pi(b - tmp))
    }

    fun rsr(): List<Double>? {
        val p2 = 2 + d * d - 2 * cab + 2 * d * (sb - sa)
        if (p2 < 0) return null
        val tmp = atan2(ca - cb, d - sa + sb)
        return listOf(mod2pi(a - tmp), sqrt(p2), mod2pi(-b + tmp))
    }

    fun lsr(): List<Double>? {
        val p2 = -2 + d * d + 2 * cab + 2 * d * (sa + sb)
        if (p2 < 0) return null
        val p = sqrt(p2)
        val tmp = atan2(-ca - cb, d + sa + sb) - atan2(-2.0, p)
        return listOf(mod2pi(-a + tmp), p, mod2pi(-mod2pi(b) + tmp))
    }

    fun rsl(): List<Double>? {
        val p2 = -2 + d * d + 2 * cab - 2 * d * (sa + sb)
        if (p2 < 0) return null
        val p = sqrt(p2)
        val tmp = atan2(ca + cb, d - sa - sb) - atan2(2.0, p)
        return listOf(mod2pi(a - tmp), p, mod2pi(b - tmp))
    }

    fun rlr(): List<Double>? {
        val tmp = (6.0 - d * d + 2 * cab + 2 * d * (sa - sb)) / 8.0
        if (abs(tmp) > 1) return null
        val p = mod2pi(2 * PI - acos(tmp))
        val t = mod2pi(a - atan2(ca - cb, d - sa + sb) + p / 2.0)
        return listOf(t, p, mod2pi(a - b - t + p))
    }

    fun lrl(): List<Double>? {
        val tmp = (6.0 - d * d + 2 * cab + 2 * d * (sb - sa)) / 8.0
        if (abs(tmp) > 1) return null
        val p = mod2pi(2 * PI - acos(tmp))
        val t = mod2pi(-a - atan2(ca - cb, d + sa - sb) + p / 2.0)
        return listOf(t, p, mod2pi(b - a - t + p))
    }

    val candidates = mapOf(
        DubinsWord.LSL to lsl(), DubinsWord.RSR to rsr(), DubinsWord.LSR to lsr(),
        DubinsWord.RSL to rsl(), DubinsWord.RLR to rlr(), DubinsWord.LRL to lrl()
    )
    val direction = startDirection.normalized
    return candidates.mapNotNull { (word, normalized) ->
        normalized?.let { DubinsPath(start, direction, radius, word, it.map { l -> l * radius }) }
    }.sortedBy { it.length }
}

/**
 * The shortest Dubins path from the pose ([start], [startDirection]) to ([end], [endDirection]) for a vehicle that
 * only moves forwards and turns at most as sharply as a circle of [radius], see [dubinsPaths]. Equal poses give a path
 * of length zero.
 */
fun dubinsPath(
    start: Vector2,
    startDirection: Vector2,
    end: Vector2,
    endDirection: Vector2,
    radius: Double
): DubinsPath {
    if (start.squaredDistanceTo(end) <= 1E-24 &&
        startDirection.normalized.distanceTo(endDirection.normalized) <= 1E-12
    ) {
        return DubinsPath(start, startDirection.normalized, radius, DubinsWord.LSL, listOf(0.0, 0.0, 0.0))
    }
    return dubinsPaths(start, startDirection, end, endDirection, radius).first()
}

/**
 * The shortest Dubins paths between consecutive points of this polyline, joined into a contour: the path a vehicle
 * with minimum turning [radius] takes through the points. When [directions] is null, the direction at every point is
 * that of the centripetal Catmull-Rom spline through the points. The path passes through every point in its direction
 * and is tangent-continuous; between points it consists of arcs of [radius] and straight segments.
 *
 * @param closed whether the path returns to the first point
 * @param directions the direction at every point, or null for Catmull-Rom directions
 * @param polarity the polarity of the resulting contour
 */
fun List<Vector2>.dubinsPath(
    radius: Double,
    closed: Boolean = false,
    directions: List<Vector2>? = null,
    polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): ShapeContour {
    require(directions == null || directions.size == size) { "expected $size directions, got ${directions?.size}" }
    val points = this
    if (points.size < 2) return ShapeContour.fromPoints(points, closed, polarity)
    val headings = directions ?: catmullRomTangents(points, closed, 0.5)
    val edges = if (closed) points.size else points.size - 1
    val segments = (0 until edges).flatMap { i ->
        val j = (i + 1) % points.size
        dubinsPath(points[i], headings[i], points[j], headings[j], radius).segments()
    }
    return ShapeContour.fromSegments(segments, closed, polarity)
}
