package org.openrndr.extra.shapes.biarc

import org.openrndr.extra.shapes.arcer.Arc
import org.openrndr.extra.shapes.arcer.arc
import org.openrndr.extra.shapes.arcer.circleTangent
import org.openrndr.extra.shapes.arcer.reflect
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.ShapeContour
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/** How [biarcs] derives the tangent at every point when no tangents are given. */
sealed interface BiarcTangents {
    /**
     * The direction of the circle through the point and its neighbours. Points on a circle give that circle, but at
     * sharp corners the direction can point back against the next edge, which gives large loops.
     */
    data object Circle : BiarcTangents

    /**
     * The direction of the Catmull-Rom spline through the points, with knot spacing |Δp|^[alpha]: 0.0 is uniform, 0.5
     * centripetal and 1.0 chordal. At the ends of an open polyline the tangent is that of the next point mirrored in
     * the end edge, so the end edges are single arcs. Usually loops less than [Circle] on scattered points.
     */
    data class CatmullRom(val alpha: Double = 0.5) : BiarcTangents
}

/**
 * Converts this polyline into a contour of biarcs: every pair of consecutive points is joined by two circular arcs that
 * leave the first point in its tangent direction, meet tangentially at a joint, and arrive at the second point in its
 * tangent direction. Unlike [org.openrndr.extra.shapes.arcer.arcer], every edge is fixed by its own end tangents, so
 * the contour is tangent-continuous everywhere, also where it closes, and an edge doesn't influence the edges after it.
 *
 * Of the one-parameter family of biarcs between two points with tangents, the one with equal tangent lengths at both
 * ends is used. When both tangents are equal the joint is the midpoint of the chord.
 *
 * When [tangents] is null the tangents are derived from the points by [tangentMethod], by default the centripetal
 * Catmull-Rom spline through the points, see [BiarcTangents]. Collinear
 * points then give a straight line and two points a straight segment.
 *
 * With a non-zero [offset] the parallel curve at that distance is returned, which is exact because the offset of an
 * arc is the concentric arc. Positive offsets lie on the side of the normal (-y, x) of the direction of travel, which
 * is to the right in a y-down frame.
 *
 * Arcs are approximated with one cubic Bézier per 90 degrees of turn.
 *
 * @param closed whether the polyline is closed, the last point should then not repeat the first
 * @param tangents the tangent direction at every point, or null to derive them with [tangentMethod]
 * @param tangentMethod how tangents are derived from the points when [tangents] is null
 * @param offset the signed distance of the returned parallel curve, 0.0 for the curve through the points
 * @param polarity the polarity of the resulting contour
 */
fun List<Vector2>.biarcs(
    closed: Boolean,
    tangents: List<Vector2>? = null,
    tangentMethod: BiarcTangents = BiarcTangents.CatmullRom(),
    offset: Double = 0.0,
    polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): ShapeContour {
    require(tangents == null || tangents.size == size) {
        "expected ${size} tangents, got ${tangents?.size}"
    }
    val indices = indices.filter { i ->
        val next = if (i + 1 < size) i + 1 else if (closed) 0 else -1
        next == -1 || this[i].squaredDistanceTo(this[next]) > 1E-18
    }
    val points = indices.map { this[it] }
    if (points.size < 2 || (closed && points.size < 3)) {
        return ShapeContour.fromPoints(points, closed, polarity)
    }
    val directions = tangents?.let { t -> indices.map { t[it].normalized } } ?: when (tangentMethod) {
        BiarcTangents.Circle -> circleTangents(points, closed)
        is BiarcTangents.CatmullRom -> catmullRomTangents(points, closed, tangentMethod.alpha)
    }
    val edges = if (closed) points.size else points.size - 1
    val arcs = (0 until edges).flatMap { i ->
        val j = (i + 1) % points.size
        biarc(points[i], directions[i], points[j], directions[j])
    }
    val segments = arcs.flatMap { if (offset == 0.0) it.segments else it.offset(offset) }
    return ShapeContour.fromSegments(segments, closed, polarity)
}

/**
 * The biarc from [start] leaving in [startTangent] to [end] arriving in [endTangent], see [List.biarcs].
 *
 * @param offset the signed distance of the returned parallel curve, 0.0 for the biarc itself
 * @param polarity the polarity of the resulting contour
 */
fun biarc(
    start: Vector2,
    startTangent: Vector2,
    end: Vector2,
    endTangent: Vector2,
    offset: Double = 0.0,
    polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): ShapeContour {
    if (start.squaredDistanceTo(end) <= 1E-18) {
        return ShapeContour.fromPoints(listOf(start), false, polarity)
    }
    val arcs = biarc(start, startTangent.normalized, end, endTangent.normalized)
    return ShapeContour.fromSegments(
        arcs.flatMap { if (offset == 0.0) it.segments else it.offset(offset) },
        false,
        polarity
    )
}

/** The tangent at every point from the circle through it and its neighbours. */
internal fun circleTangents(points: List<Vector2>, closed: Boolean): List<Vector2> {
    val n = points.size
    return List(n) { i ->
        when {
            closed -> circleTangent(points[(i - 1 + n) % n], points[i], points[(i + 1) % n])
            n == 2 -> (points[1] - points[0]).normalized
            // the circle through the first three points, traversed in the order of the points
            i == 0 -> circleTangent(points[2], points[0], points[1])
            i == n - 1 -> circleTangent(points[n - 2], points[n - 1], points[n - 3])
            else -> circleTangent(points[i - 1], points[i], points[i + 1])
        }
    }
}

/**
 * The tangent at every point of the Catmull-Rom spline through the points with knot spacing |Δp|^[alpha]: the
 * derivative of the Barry-Goldman pyramid at the knot. The open ends mirror the tangent of their neighbour in the end
 * edge.
 */
internal fun catmullRomTangents(points: List<Vector2>, closed: Boolean, alpha: Double): List<Vector2> {
    val n = points.size
    if (!closed && n == 2) {
        return List(2) { (points[1] - points[0]).normalized }
    }
    fun tangent(previous: Vector2, p: Vector2, next: Vector2): Vector2 {
        val h0 = p.distanceTo(previous).pow(alpha)
        val h1 = next.distanceTo(p).pow(alpha)
        val m = (p - previous) / h0 - (next - previous) / (h0 + h1) + (next - p) / h1
        // a reversal leaves no direction, continue along the next edge
        return if (m.length > 1E-12 * (next - previous).length) m.normalized else (next - p).normalized
    }
    val result = MutableList(n) { i ->
        if (closed || i in 1 until n - 1) tangent(points[(i - 1 + n) % n], points[i], points[(i + 1) % n])
        else Vector2.ZERO
    }
    if (!closed) {
        result[0] = reflect(result[1], points[1] - points[0])
        result[n - 1] = reflect(result[n - 2], points[n - 1] - points[n - 2])
    }
    return result
}

/**
 * The two arcs of the biarc with equal tangent lengths d at both ends. The control points `start + t0·d` and
 * `end - t1·d` are then 2d apart and the joint is their midpoint, which gives
 * `2(1 - t0·t1)·d² + 2(v·(t0 + t1))·d - v·v = 0` for the chord v.
 */
private fun biarc(start: Vector2, t0: Vector2, end: Vector2, t1: Vector2): List<Arc> {
    val v = end - start
    val t = t0 + t1
    val a = 2.0 * (1.0 - t0.dot(t1))
    val joint = if (abs(a) < 1E-12) {
        // equal tangents, the joint is the chord midpoint whatever d is
        (start + end) * 0.5
    } else {
        // the positive root of a·d² + 2b·d - v·v = 0, in the form without cancellation
        val b = v.dot(t)
        val root = sqrt(b * b + a * v.squaredLength)
        val d = if (b > 0.0) v.squaredLength / (b + root) else (root - b) / a
        ((start + t0 * d) + (end - t1 * d)) * 0.5
    }
    if (joint.squaredDistanceTo(start) <= 1E-18 || joint.squaredDistanceTo(end) <= 1E-18) {
        return listOf(arc(start, t0, end))
    }
    val first = arc(start, t0, joint)
    return listOf(first, arc(joint, first.exit, end))
}
