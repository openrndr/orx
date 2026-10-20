package org.openrndr.extra.shapes.arcer

import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Converts this polyline into a contour of tangent-continuous circular arcs that pass through all of its points, after
 * [surface-detail.com/posts/arc-plotter](https://www.surface-detail.com/posts/arc-plotter).
 *
 * Every pair of consecutive points is joined by the unique circular arc that leaves the first point in the direction in
 * which the previous arc arrived, the first arc leaves in [startTangent]. The exit direction of an arc is its entry
 * direction mirrored in its chord, so the result is fully determined by the points and the start tangent. When the
 * entry direction is parallel to the chord the arc degenerates into a straight segment.
 *
 * When [startTangent] is null, open polylines start in the direction of the circle through the first three points.
 * Closed polylines with an odd number of points start in the direction for which the last arc arrives back in that
 * same direction, which gives a contour that is tangent-continuous everywhere. With an even number of points no such
 * direction exists in general, the start then follows the circle through the last, first and second point and the
 * contour has a kink at the first point.
 *
 * When an arc is longer than [maxArcLength], an extra point is inserted and the two arcs through it are tried instead,
 * recursively. The point lies on the bisector of the entry direction and the chord, at half the chord length from the
 * start. The first arc then arrives along the chord, turning half as far as the arc it replaces, and the second arc only
 * corrects for the offset of the point from the chord, turning at most about 60 degrees. Both are shorter than the arc
 * they replace, so long arcs straighten out towards their chords. Straight segments are not split. Splitting changes
 * the direction in which an edge arrives, so it also changes the arcs that follow.
 *
 * For closed polylines in which edges are split, the start direction for smooth closure is searched for numerically;
 * when none exists the direction with the smallest kink at the first point is used.
 *
 * With a non-zero [offset] the parallel curve at that distance is returned, which is exact because the offset of an
 * arc is the concentric arc. Positive offsets lie on the side of the normal (-y, x) of the direction of travel, which
 * is to the right in a y-down frame. Where an offset exceeds an arc's radius the offset arc passes through its center
 * and reverses direction, as parallel curves do, leaving cusps at its ends. Contours at a range of offsets form the
 * ribbons of the article.
 *
 * Arcs are approximated with one cubic Bézier per 90 degrees of turn.
 *
 * @param closed whether the polyline is closed, the last point should then not repeat the first
 * @param startTangent the direction in which the first arc leaves the first point, or null for a default direction
 * @param maxArcLength the length above which an arc is split in two, must be positive
 * @param offset the signed distance of the returned parallel curve, 0.0 for the curve through the points
 * @param polarity the polarity of the resulting contour
 */
fun List<Vector2>.arcer(
    closed: Boolean,
    startTangent: Vector2? = null,
    maxArcLength: Double = Double.POSITIVE_INFINITY,
    offset: Double = 0.0,
    polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): ShapeContour {
    require(maxArcLength > 0.0) { "maxArcLength should be positive, got $maxArcLength" }
    val points = filterIndexed { i, p ->
        val next = if (i + 1 < size) this[i + 1] else if (closed) this[0] else null
        next == null || p.squaredDistanceTo(next) > 1E-18
    }
    if (points.size < 2 || (closed && points.size < 3)) {
        return ShapeContour.fromPoints(points, closed, polarity)
    }
    val targets = if (closed) points.drop(1) + points[0] else points.drop(1)

    /** The arcs from [startTangent] along all edges, and whether any edge was split. */
    fun arcs(startTangent: Vector2): Pair<List<Arc>, Boolean> {
        val arcs = mutableListOf<Arc>()
        var split = false
        var start = points[0]
        var tangent = startTangent
        fun emit(end: Vector2, depth: Int) {
            val arc = arc(start, tangent, end)
            if (arc.center != null && arc.length > maxArcLength && depth < MAX_SPLIT_DEPTH) {
                split = true
                emit(splitPoint(start, tangent, end), depth + 1)
                emit(end, depth + 1)
                return
            }
            arcs.add(arc)
            start = end
            tangent = arc.exit
        }
        for (end in targets) {
            emit(end, 0)
        }
        return Pair(arcs, split)
    }

    val initial = startTangent?.normalized ?: if (closed) closedTangent(points) else
        if (points.size < 3) (points[1] - points[0]).normalized else
            circleTangent(points[2], points[0], points[1])
    val (initialArcs, split) = arcs(initial)
    val arcs = if (closed && startTangent == null && split) {
        arcs(searchClosedTangent(initial) { arcs(it).first.last().exit }).first
    } else initialArcs
    val segments = arcs.flatMap { if (offset == 0.0) it.segments else it.offset(offset) }
    return ShapeContour.fromSegments(segments, closed, polarity)
}

/** Limits the recursion depth of splitting. */
private const val MAX_SPLIT_DEPTH = 16

/**
 * The point inserted to split the arc from [start] leaving in [tangent] to [end]: on the bisector of [tangent] and the
 * chord, at half the chord length, so that the arc to it arrives along the chord.
 */
private fun splitPoint(start: Vector2, tangent: Vector2, end: Vector2): Vector2 {
    val chord = end - start
    val bisector = tangent + chord.normalized
    val direction = if (bisector.length > 1E-6) bisector.normalized else {
        // the chord points almost straight back, turn towards the side the arc curves to
        val normal = perpendicular(tangent)
        if (normal.dot(chord) >= 0.0) normal else -normal
    }
    return start + direction * (chord.length / 2.0)
}

/** The signed angle from [a] to [b] in (-π, π]. */
private fun signedAngle(a: Vector2, b: Vector2) = atan2(a.x * b.y - a.y * b.x, a.dot(b))

/**
 * Searches for a start direction for which [exit], the direction in which the closed contour arrives back at its first
 * point, equals that start direction. The mismatch angle is sampled around the circle and its sign changes are refined
 * by bisection; brackets in which the mismatch jumps, because the splits change or the angle wraps, are rejected by
 * the final check. Of the exact solutions the one closest to [preferred] is used, without one the sampled or refined
 * direction with the smallest mismatch.
 */
private fun searchClosedTangent(preferred: Vector2, exit: (Vector2) -> Vector2): Vector2 {
    fun direction(angle: Double) = Vector2(cos(angle), sin(angle))
    fun mismatch(angle: Double): Double = direction(angle).let { signedAngle(it, exit(it)) }

    val samples = 72
    val angles = List(samples) { it * 2 * PI / samples }
    val mismatches = angles.map { mismatch(it) }
    val candidates = mutableListOf(Pair(preferred, abs(signedAngle(preferred, exit(preferred)))))
    for (i in 0 until samples) {
        candidates.add(Pair(direction(angles[i]), abs(mismatches[i])))
        var a = angles[i]
        var b = a + 2 * PI / samples
        var ma = mismatches[i]
        val mb = mismatches[(i + 1) % samples]
        if (ma * mb > 0.0 || abs(ma) > PI / 2.0 || abs(mb) > PI / 2.0) continue
        repeat(50) {
            val m = (a + b) / 2.0
            val mm = mismatch(m)
            if (ma * mm <= 0.0) b = m else {
                a = m
                ma = mm
            }
        }
        val root = (a + b) / 2.0
        candidates.add(Pair(direction(root), abs(mismatch(root))))
    }
    val exact = candidates.filter { it.second < 1E-9 }
    return if (exact.isNotEmpty()) exact.maxBy { it.first.dot(preferred) }.first else candidates.minBy { it.second }.first
}

/**
 * The start direction for a closed polyline without splits. The arcs map the start direction to the final direction by
 * one reflection per edge. For an odd number of edges the composition is itself a reflection, of which the axis is the
 * start direction that closes smoothly.
 */
private fun closedTangent(points: List<Vector2>): Vector2 {
    val fallback = circleTangent(points.last(), points[0], points[1])
    if (points.size % 2 == 0) {
        return fallback
    }
    var u = Vector2.UNIT_X
    for (i in points.indices) {
        u = reflect(u, points[(i + 1) % points.size] - points[i])
    }
    val angle = atan2(u.y, u.x) / 2.0
    val axis = Vector2(cos(angle), sin(angle))
    val tangent = if (axis.dot(fallback) >= 0.0) axis else -axis
    // both senses of the axis close smoothly, but one may run a straight edge backwards, which leaves a cusp; the
    // tangents of the other sense are all negated, so it runs that edge forwards
    var direction = tangent
    for (i in points.indices) {
        val chord = (points[(i + 1) % points.size] - points[i]).normalized
        if (direction.dot(chord) < -1.0 + 1E-9) return -tangent
        direction = reflect(direction, chord)
    }
    return tangent
}
