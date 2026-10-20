package org.openrndr.extra.shapes.splines

import org.openrndr.extra.shapes.ph.cubicApproximation
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Converts this polyline into a circle spline through all points, after Séquin, Lee and Yen, "Fair, G2- and
 * C2-continuous circle splines for the interpolation of sparse data points" (Computer-Aided Design, 2005).
 *
 * Every segment from Pᵢ to Pᵢ₊₁ blends two circular arcs between those points: the arc of the circle through Pᵢ₋₁, Pᵢ
 * and Pᵢ₊₁ and the arc of the circle through Pᵢ, Pᵢ₊₁ and Pᵢ₊₂. An arc from A to B is given by the angle θ between its
 * tangent at A and the chord, and its point at fraction t of the way is A + rotate(chord direction, θ(1 - t)) times
 * |AB|·sin(θt) / sin(θ). The segment takes θ from the first arc to the second with the weight sin²(πt / 2), in the
 * angle space of the paper. Because the weight has zero derivative at both ends, every segment starts on the first
 * circle and ends on the second with the same tangent and curvature: the spline is G2, its curvature at Pᵢ is that of
 * the circle through Pᵢ and its neighbours, points on a circle give that circle and collinear points a straight line.
 * The end segments of an open polyline are arcs of their one circle.
 *
 * The blended arcs are drawn as cubic Béziers within [tolerance].
 *
 * @param closed whether the polyline is closed, the last point should then not repeat the first
 * @param tolerance the largest distance between the cubic approximation and the spline
 * @param polarity the polarity of the resulting contour
 */
fun List<Vector2>.circleSpline(
    closed: Boolean,
    tolerance: Double = 0.01,
    polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): ShapeContour {
    val segments = circleSplineSegments(closed)
    if (segments.isEmpty()) return ShapeContour.fromPoints(this, closed, polarity)
    val beziers = segments.flatMap { segment -> cubicApproximation(segment::position, segment::derivative, tolerance) }
    return ShapeContour.fromSegments(beziers, closed, polarity)
}

/**
 * A segment of a circle spline from [start] to [end], blending the arcs that leave [start] at angle [startAngle] and
 * at angle [endAngle] to the chord, see [circleSpline].
 */
internal class CircleSplineSegment(
    val start: Vector2,
    val end: Vector2,
    val startAngle: Double,
    val endAngle: Double
) {
    private val chord = end - start
    private val length = chord.length
    private val direction = chord / length

    /** The blended angle at [t]: from [startAngle] to [endAngle] with the weight sin²(πt / 2). */
    fun angle(t: Double): Double {
        val s = sin(PI * t / 2.0)
        val w = s * s
        return startAngle * (1.0 - w) + endAngle * w
    }

    /**
     * The point at fraction [t] of the arc from [start] to [end] whose tangent at [start] is the chord direction rotated
     * by [theta]; positive θ bulges to the side of positive rotation, so the arc turns the other way.
     */
    private fun arcPoint(theta: Double, t: Double): Vector2 {
        if (abs(theta) < 1E-12) return start + chord * t
        val along = rotate(direction, theta * (1.0 - t))
        return start + along * (length * sin(theta * t) / sin(theta))
    }

    fun position(t: Double): Vector2 = when (t) {
        0.0 -> start
        1.0 -> end
        else -> arcPoint(angle(t), t)
    }

    /** The derivative to t, by central differences of [position]. */
    fun derivative(t: Double): Vector2 {
        val h = 1E-6
        val a = (t - h).coerceAtLeast(0.0)
        val b = (t + h).coerceAtMost(1.0)
        return (arcPoint(angle(b), b) - arcPoint(angle(a), a)) / (b - a)
    }
}

private fun rotate(v: Vector2, angle: Double): Vector2 {
    val c = cos(angle)
    val s = sin(angle)
    return Vector2(v.x * c - v.y * s, v.x * s + v.y * c)
}

private fun cross(a: Vector2, b: Vector2) = a.x * b.y - a.y * b.x

/** The signed angle from [a] to [b] in (-π, π]. */
private fun signedAngle(a: Vector2, b: Vector2) = atan2(cross(a, b), a.dot(b))

/** Arcs of more than nearly a full circle are kept just short of it, where sin θ vanishes. */
private const val MAX_ANGLE = PI - 1E-6

/**
 * The angle between the chord from [a] to [b] and the tangent at [a] of the circle through [x], [a] and [b], traversed
 * from x via a to b; 0 when they are collinear. By the inscribed angle theorem that is the angle at x subtended by the
 * chord, measured towards the side the circle turns to.
 */
private fun angleBefore(x: Vector2, a: Vector2, b: Vector2): Double {
    // the angle between the tangent at a and the chord ab equals the angle in the triangle at x
    val turn = cross(a - x, b - a)
    if (abs(turn) <= 1E-12 * (a - x).length * (b - a).length) return 0.0
    val inscribed = abs(signedAngle(a - x, b - x))
    // a circle that turns left (positive turn) leaves a with its tangent turned clockwise from the chord: θ < 0
    return (if (turn > 0) -inscribed else inscribed).coerceIn(-MAX_ANGLE, MAX_ANGLE)
}

/**
 * The angle between the chord from [a] to [b] and the tangent at [a] of the circle through [a], [b] and [y], traversed
 * from a via b to y; 0 when they are collinear.
 */
private fun angleAfter(a: Vector2, b: Vector2, y: Vector2): Double {
    val turn = cross(b - a, y - b)
    if (abs(turn) <= 1E-12 * (b - a).length * (y - b).length) return 0.0
    // the tangent at a makes the same angle with ab as the chord ab makes with the tangent at b, which equals the
    // inscribed angle at y on the far side
    val inscribed = abs(signedAngle(a - y, b - y))
    // a circle that turns left (positive turn) leaves a with its tangent turned clockwise from the chord: θ < 0
    return (if (turn > 0) -inscribed else inscribed).coerceIn(-MAX_ANGLE, MAX_ANGLE)
}

/**
 * The segments of [circleSpline], one per edge between the points that don't repeat the next point; empty when there
 * are too few of them.
 */
internal fun List<Vector2>.circleSplineSegments(closed: Boolean): List<CircleSplineSegment> {
    val points = filterIndexed { i, p ->
        val next = if (i + 1 < size) this[i + 1] else if (closed) this[0] else null
        next == null || p.squaredDistanceTo(next) > 1E-18
    }
    val n = points.size
    if (n < 2 || (closed && n < 3)) return emptyList()
    val edges = if (closed) n else n - 1
    return List(edges) { i ->
        val a = points[i]
        val b = points[(i + 1) % n]
        val before = if (closed || i > 0) angleBefore(points[(i - 1 + n) % n], a, b) else null
        val after = if (closed || i + 2 < n) angleAfter(a, b, points[(i + 2) % n]) else null
        // the end segments of an open polyline are arcs of their one circle
        val first = before ?: after ?: 0.0
        val second = after ?: before ?: 0.0
        CircleSplineSegment(a, b, first, second)
    }
}
